/*
 * dex2jar - Tools to work with android .dex and java .class files
 * Copyright (c) 2009-2012 Panxiaobo
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.googlecode.dex2jar.tools;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import com.googlecode.d2j.dex.ClassVisitorFactory;
import com.googlecode.d2j.dex.ExDex2Asm;
import com.googlecode.d2j.node.DexClassNode;
import com.googlecode.d2j.node.DexFileNode;
import com.googlecode.d2j.reader.DexFileReader;
import com.googlecode.d2j.util.zip.ZipEntry;
import com.googlecode.d2j.util.zip.ZipFile;

@BaseCmd.Syntax(cmd = "d2j-ex-dex2jar", syntax = "[options] <file0> [file1 ... fileN]", desc = "convert dex to jar")
public class Dex2jarExCmd extends BaseCmd {
    public static void main(String... args) {
        new Dex2jarExCmd().doMain(args);
    }

    @Opt(opt = "mt", longOpt = "multi-thread", description = "concurrent process, default is 4 thread")
    private int multiThread = 4;

    @Opt(opt = "fl", longOpt = "file-list", description = "a file contains a list of dex to process")
    private Path fileList;

    @Opt(opt = "o", longOpt = "output", description = "output .jar file, default is $current_dir/[file-name]-dex2jar.jar", argName = "out-jar-file")
    private Path output;

    @Opt(opt = "oc", longOpt = "only-clinit", hasArg = false, description = "only output class skeleton")
    private boolean onlyClinit = false;

    private int readerConfig;

    @Override
    protected void doCommandLine() throws Exception {
        long start = System.currentTimeMillis();
        List<String> f = new ArrayList<>();
        f.addAll(Arrays.asList(remainingArgs));
        if (onlyClinit) {
            readerConfig |= DexFileReader.KEEP_CLINIT | DexFileReader.SKIP_CODE;
        }
        if (fileList != null) {
            f.addAll(Files.readAllLines(fileList, StandardCharsets.UTF_8));
        }
        if (f.size() < 1) {
            throw new HelpException();
        }

        final ExecutorService executorService = Executors.newFixedThreadPool(multiThread);

        final Iterator<String> fileIt = f.iterator();
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                if (fileIt.hasNext()) {
                    String fileName = fileIt.next();
                    try {
                        run0(fileName, executorService);
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        executorService.submit(this); // run this job again
                    }
                } else {
                    executorService.shutdown();
                }
            }
        });
        executorService.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
        System.out.println("done " + (System.currentTimeMillis() - start) + "ms");
    }

    private void run0(String fileName, final ExecutorService executorService) throws IOException {
        File input = new File(fileName).getAbsoluteFile();
        String baseName = getBaseName(input.toPath());
        Path currentDir = input.getParentFile().toPath();
        Path file = output == null ? currentDir.resolve(baseName + "-dex2jar.jar") : output;
        final Path errorFile = currentDir.resolve(baseName + "-error.zip");
        System.out.println("dex2jar " + fileName + " -> " + file);
        final BaksmaliBaseDexExceptionHandler exceptionHandler = new BaksmaliBaseDexExceptionHandler();
        DexFileNode fileNode = new DexFileNode();
        List<byte[]> dexData = readMultipleDex(new File(fileName).toPath());
        for (byte[] dexBytes : dexData) {
            DexFileReader reader = new DexFileReader(dexBytes);
            try {
                reader.accept(fileNode, readerConfig | DexFileReader.SKIP_DEBUG | DexFileReader.IGNORE_READ_EXCEPTION);
            } catch (Exception ex) {
                exceptionHandler.handleFileException(ex);
                throw ex;
            }
        }
        file.toFile().createNewFile();
        final FileSystem fs = createZip(file);
        final Path dist = fs.getPath("/");
        ClassVisitorFactory cvf = new ClassVisitorFactory() {
            @Override
            public ClassVisitor create(final String name) {
                return new ClassVisitor(Opcodes.ASM4, new ClassWriter(ClassWriter.COMPUTE_MAXS)) {
                    @Override
                    public void visitEnd() {
                        super.visitEnd();
                        ClassWriter cw = (ClassWriter) super.cv;

                        byte[] data;
                        try {
                            data = cw.toByteArray();
                        } catch (Exception ex) {
                            System.err.println(String.format("ASM fail to generate .class file: %s", name));
                            exceptionHandler.handleFileException(ex);
                            return;
                        }
                        try {
                            Path dist1 = dist.resolve(name + ".class");
                            BaseCmd.createParentDirectories(dist1);
                            Files.write(dist1, data);
                        } catch (IOException e) {
                            exceptionHandler.handleFileException(e);
                        }
                    }
                };
            }
        };

        new ExDex2Asm(exceptionHandler) {

            @Override
            public void convertDex(DexFileNode fileNode, final ClassVisitorFactory cvf) {
                if (fileNode.clzs != null) {
                    final Map<String, Clz> classes = collectClzInfo(fileNode);
                    final List<Future<?>> results = new ArrayList<>(fileNode.clzs.size());
                    for (final DexClassNode classNode : fileNode.clzs) {
                        results.add(executorService.submit(new Runnable() {
                            @Override
                            public void run() {
                                convertClass(classNode, cvf, classes);
                            }
                        }));
                    }
                    executorService.submit(new Runnable() {
                        @Override
                        public void run() {
                            for (Future<?> result : results) {
                                try {
                                    result.get();
                                } catch (InterruptedException | ExecutionException e) {
                                    e.printStackTrace();
                                }
                            }
                            BaksmaliBaseDexExceptionHandler exceptionHandler1 = (BaksmaliBaseDexExceptionHandler) exceptionHandler;
                            if (exceptionHandler1.hasException()) {
                                exceptionHandler1.dump(errorFile, new String[0]);
                            }
                            try {
                                fs.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }
        }.convertDex(fileNode, cvf);
    }

    public static List<byte[]> readMultipleDex(Path file) throws IOException {
        List<byte[]> dexBytes = new ArrayList<>(2);
        byte[] allBytes = Files.readAllBytes(file);

        if (allBytes.length < 3) {
            throw new IOException("File too small to be a dex/zip");
        }
        String fileType = new String(allBytes, 0, 3);
        if ("dex".equals(fileType)) { // dex
            dexBytes.add(allBytes);
            return dexBytes;
        } else if (fileType.startsWith("PK")) { // ZIP
            try (ZipFile zipFile = new ZipFile(allBytes)) {
                for (ZipEntry entry : zipFile.entries()) {
                    if (entry.getName().startsWith("classes")
                            && entry.getName().endsWith(".dex")) {
                        InputStream is = zipFile.getInputStream(entry);
                        byte[] data = new byte[is.available()];
                        int remain = data.length;
                        int read = 0;
                        int readSize;
                        while (remain > 0 && (readSize = is.read(data, read, remain)) != -1) {
                            remain -= readSize;
                            read += readSize;
                        }
                        dexBytes.add(data);
                    }
                }
                if (dexBytes.isEmpty()) {
                    throw new IOException(
                            "Can not find classes.dex in zip file");
                }
                return dexBytes;
            }
        }
        throw new IOException("the src file not a .dex or zip file " + fileType);
    }
}

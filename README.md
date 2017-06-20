This is a fork of dex2jar. Additional code has been added to determine the size breakdown per class.

#dex2jar [![Build Status](https://travis-ci.org/pxb1988/dex2jar.svg?branch=2.x)](https://travis-ci.org/pxb1988/dex2jar)
Tools to work with android .dex and java .class files

1. dex-reader/writer:
    Read/write the Dalvik Executable (.dex) file. It has a [light weight API similar with ASM](https://sourceforge.net/p/dex2jar/wiki/Faq#markdown-header-want-to-read-dex-file-using-dex2jar).
2. d2j-dex2jar:
    Convert .dex file to .class files (zipped as jar)
3. smali/baksmali:
    disassemble dex to smali files and assemble dex from smali files. different implementation to [smali/baksmali](http://code.google.com/p/smali), same syntax, but we support escape in type desc "Lcom/dex2jar\t\u1234;"
4. other tools:
    [d2j-decrypt-string](https://sourceforge.net/p/dex2jar/wiki/DecryptStrings)

## Usage

> sh d2j-dex2jar.sh -f ~/path/to/apk_to_decompile.apk > output.txt

The jar file will be `apk_to_decompile-dex2jar.jar`.
And the analytics about class size will be inside `output.txt`.

## Build Process 

```
cd dex2jar-2.x
gradle clean distZip
# if build successfull, the zip file is under dex-tools/build/distributions/
# unzip by calling the following
open dex-tools/build/distributions/dex-tools-2.1-SNAPSHOT.zip
```

/*
 * dex2jar - Tools to work with android .dex and java .class files
 * Copyright (c) 2009-2013 Panxiaobo
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
package com.googlecode.d2j.dex;

import java.util.HashMap;

import com.googlecode.d2j.DexConstants;
import com.googlecode.d2j.Field;
import com.googlecode.d2j.node.DexClassNode;
import com.googlecode.d2j.node.DexFieldNode;
import com.googlecode.d2j.node.DexFileNode;
import com.googlecode.d2j.node.DexMethodNode;
import com.googlecode.d2j.reader.Op;
import com.googlecode.d2j.visitors.DexCodeVisitor;

/**
 * 1. Dex omit the value of static-final field if it is the default value.
 *
 * 2. static-final field init by zero, but assigned in clinit
 *
 * this method is try to fix the problems.
 */
public class DexFix {

    public static void fixStaticFieldValue(final DexFileNode dex) {
        if (dex.clzs != null) {
            for (DexClassNode classNode : dex.clzs) {
                fixStaticFieldValue(classNode);
            }
        }
    }

    /**
     * init value to default if the field is static and final, and the field is not init in clinit method
     *
     * erase the default value if the field is init in clinit method
     * 
     * @param classNode
     */
    public static void fixStaticFieldValue(final DexClassNode classNode) {
        if (classNode.fields == null) {
            return;
        }

        final HashMap<Field, DexFieldNode> staticPrimitiveFields = new HashMap<>();
        for (DexFieldNode fn : classNode.fields) {
            if ((fn.access & DexConstants.ACC_STATIC) == DexConstants.ACC_STATIC) {
                char t = fn.field.getType().charAt(0);
                if (t == 'L' || t == '[') { // Ignore Object
                    continue;
                }
                staticPrimitiveFields.put(fn.field, fn);
            }
        }
        if (staticPrimitiveFields.isEmpty()) {
            return;
        }
        DexMethodNode node = null;
        if (classNode.methods != null) {
            for (DexMethodNode mn : classNode.methods) {
                if (mn.method.getName().equals("<clinit>")) {
                    node = mn;
                    break;
                }
            }
        }
        if (node != null && node.codeNode != null) {
            node.codeNode.accept(new DexCodeVisitor() {
                @Override
                public void visitFieldStmt(Op op, int a, int b, Field field) {
                    switch (op) {
                        case SPUT:
                        case SPUT_BOOLEAN:
                        case SPUT_BYTE:
                        case SPUT_CHAR:
                        case SPUT_OBJECT:
                        case SPUT_SHORT:
                        case SPUT_WIDE:
                            if (field.getOwner().equals(classNode.className)) {
                                DexFieldNode fn = staticPrimitiveFields.remove(field);
                                if (fn != null) {
                                    fn.cst = null;
                                }
                            }
                            break;
                        default:
                            // ignored
                            break;
                    }
                }
            });
        }

        for (DexFieldNode fn : staticPrimitiveFields.values()) {
            if (fn.cst == null) {
                fn.cst = getDefaultValueOfPrimitive(fn.field.getType().charAt(0));
            }
        }

    }

    private static Object getDefaultValueOfPrimitive(char t) {
        switch (t) {
            case 'B':
                return (byte) 0;
            case 'Z':
                return Boolean.FALSE;
            case 'S':
                return (short) 0;
            case 'C':
                return (char) 0;
            case 'I':
                return 0;
            case 'F':
                return 0.0f;
            case 'J':
                return 0L;
            case 'D':
                return 0.0;
            default:
                return null;
        }
    }
}

/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.ide.eclipse.gltrace.format;

import static org.junit.Assert.*;

import com.android.ide.eclipse.gltrace.GLProtoBuf.GLMessage.DataType.Type;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class GLAPISpecTest {
    @Test
    public void testParser() {
        String returnType = "void";
        String funcName = "glDiscardFramebufferEXT";
        List<String> args = Arrays.asList(
                "GLenum target",
                "GLsizei numAttachments",
                "const GLenum* attachments");

        GLAPISpec spec = GLAPISpec.parseLine(createSpec(returnType, funcName, args));

        assertEquals(Type.VOID, spec.getReturnValue().getDataType());
        assertEquals(returnType, spec.getReturnValue().getCType());
        assertEquals(funcName, spec.getFunction());

        List<GLDataTypeSpec> argSpecs = spec.getArgs();
        assertEquals(argSpecs.size(), args.size());

        GLDataTypeSpec argSpec = argSpecs.get(0);
        assertEquals(argSpec.getArgName(), "target");
        assertEquals(argSpec.getDataType(), Type.ENUM);
        assertEquals(argSpec.isPointer(), false);

        argSpec = argSpecs.get(2);
        assertEquals(argSpec.getArgName(), "attachments");
        assertEquals(argSpec.getDataType(), Type.ENUM);
        assertEquals(argSpec.isPointer(), true);
    }

    private String createSpec(String returnType, String funcName, List<String> args) {
        StringBuffer sb = new StringBuffer();
        sb.append(String.format("%s, %s", returnType, funcName));

        if (args != null) {
            sb.append(", ");
            for (int i = 0; i < args.size(); i++) {
                sb.append(args.get(i));
                if (i != args.size() - 1) {
                    sb.append(", ");
                }
            }
        }

        return sb.toString();
    }
}

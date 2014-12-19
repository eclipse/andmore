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

import com.android.ide.eclipse.gltrace.GLEnum;
import com.android.ide.eclipse.gltrace.GLProtoBuf.GLMessage;
import com.android.ide.eclipse.gltrace.GLProtoBuf.GLMessage.Builder;
import com.android.ide.eclipse.gltrace.GLProtoBuf.GLMessage.DataType;
import com.android.ide.eclipse.gltrace.GLProtoBuf.GLMessage.DataType.Type;
import com.android.ide.eclipse.gltrace.GLProtoBuf.GLMessage.Function;
import com.google.protobuf.ByteString;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GLMessageFormatterTest {
    private static final List<String> API_SPECS = Arrays.asList(
            "void, glBindBuffer, GLenum target, GLuint buffer",
            "const GLchar*, glGetString, GLenum name",
            "void, glMultMatrixf, const GLfloat* m",
            "GLenum, eglBindAPI, GLEnum arg",
            "void, glGetActiveAttrib, GLenum* type",
            "void, glTexImage2D, GLint level, GLsizei width, const GLvoid* pixels");
    private static GLMessageFormatter sGLMessageFormatter;

    static {
        Map<String, GLAPISpec> specs = new HashMap<String, GLAPISpec>(API_SPECS.size());

        for (String specString: API_SPECS) {
            GLAPISpec spec = GLAPISpec.parseLine(specString);
            specs.put(spec.getFunction(), spec);
        }

        sGLMessageFormatter = new GLMessageFormatter(specs);
    }

    @Test
    public void testBindBuffer() {
        GLEnum arg1 = GLEnum.GL_ELEMENT_ARRAY_BUFFER;
        int arg2 = 10;

        GLMessage msg = constructGLMessage(null, Function.glBindBuffer,
                createEnumDataType((int)arg1.value),
                createIntegerDataType(arg2));

        String expected = String.format("glBindBuffer(target = %s, buffer = %s)",
                arg1.toString(),
                Integer.toString(arg2));
        String actual = sGLMessageFormatter.formatGLMessage(msg);

        assertEquals(expected, actual);
    }

    @Test
    public void testGlGetString() {
        String retValue = "testString";
        GLEnum arg1 = GLEnum.GL_RENDERER;

        GLMessage msg = constructGLMessage(
                createStringDataType(retValue),
                Function.glGetString,
                createEnumDataType((int)arg1.value));
        String expected = String.format("%s(name = %s) = (const GLchar*) %s", Function.glGetString,
                arg1.toString(), retValue);
        String actual = sGLMessageFormatter.formatGLMessage(msg);

        assertEquals(expected, actual);
    }

    @Test
    public void testGLEnum0() {
        // an enum of value 0 should equal GL_POINTS if it is an argument,
        // and GL_NO_ERROR if it is a return value
        GLMessage msg = constructGLMessage(
                createEnumDataType(0),
                Function.eglBindAPI,
                createEnumDataType(0));
        String expected = "eglBindAPI(arg = GL_POINTS) = (GLenum) GL_NO_ERROR";
        String actual = sGLMessageFormatter.formatGLMessage(msg);

        assertEquals(expected, actual);
    }

    @Test
    public void testMessageWithPointer() {
        GLMessage msg = constructGLMessage(null,
                Function.glTexImage2D,
                createIntegerDataType(1),
                createIntegerDataType(2),
                createIntegerPointerDataType(0xbadc0ffe));
        String expected = "glTexImage2D(level = 1, width = 2, pixels = 0xbadc0ffe)";
        String actual = sGLMessageFormatter.formatGLMessage(msg);

        assertEquals(expected, actual);
    }

    @Test
    public void testMessageWithMismatchedPointer() {
        // "void, glMultMatrixf, const GLfloat* m",
        GLMessage msg = constructGLMessage(null,
                Function.glMultMatrixf,
                createIntegerDataType(0xbadc0ffe));

        String expected = "glMultMatrixf(m = 0xbadc0ffe)";
        String actual = sGLMessageFormatter.formatGLMessage(msg);

        assertEquals(expected, actual);
    }

    @Test
    public void testMessageWithEnumPointer() {
        //void, glGetActiveAttrib, GLenum* type
        GLMessage msg = constructGLMessage(null,
                Function.glGetActiveAttrib,
                createIntegerPointerDataType((int)GLEnum.GL_FLOAT_MAT4.value));

        String expected = "glGetActiveAttrib(type = [GL_FLOAT_MAT4])";
        String actual = sGLMessageFormatter.formatGLMessage(msg);

        assertEquals(expected, actual);
    }

    private DataType createStringDataType(String retValue) {
        return DataType.newBuilder()
                .addCharValue(ByteString.copyFromUtf8(retValue))
                .setIsArray(true)
                .setType(Type.CHAR)
                .build();
    }

    private DataType createIntegerDataType(int val) {
        return DataType.newBuilder()
                .addIntValue(val)
                .setIsArray(false)
                .setType(Type.INT)
                .build();
    }

    private DataType createIntegerPointerDataType(int val) {
        return DataType.newBuilder()
                .addIntValue(val)
                .setIsArray(true)
                .setType(Type.INT)
                .build();
    }

    private DataType createEnumDataType(int val) {
        return DataType.newBuilder()
                .addIntValue(val)
                .setIsArray(false)
                .setType(Type.ENUM)
                .build();
    }

    private GLMessage constructGLMessage(DataType retValue, Function func, DataType...args) {
        Builder builder = GLMessage.newBuilder();
        builder.setFunction(func);

        // set required fields we don't care about in these tests
        builder.setContextId(0);
        builder.setStartTime(0);
        builder.setDuration(0);

        // set return value if any
        if (retValue != null) {
            builder.setReturnValue(retValue);
        }

        for (DataType arg: args) {
            builder.addArgs(arg);
        }

        return builder.build();
    }
}

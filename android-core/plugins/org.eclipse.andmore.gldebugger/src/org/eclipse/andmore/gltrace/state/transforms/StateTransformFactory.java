/*
 * Copyright (C) 2012 The Android Open Source Project
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

package org.eclipse.andmore.gltrace.state.transforms;

import com.google.common.io.Files;
import com.google.protobuf.ByteString;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import org.eclipse.andmore.gltrace.FileUtils;
import org.eclipse.andmore.gltrace.GLEnum;
import org.eclipse.andmore.gltrace.GLProtoBuf.GLMessage;
import org.eclipse.andmore.gltrace.state.GLState;
import org.eclipse.andmore.gltrace.state.GLStateType;
import org.eclipse.andmore.gltrace.state.IGLProperty;

public class StateTransformFactory {
	private static final String TEXTURE_DATA_FILE_PREFIX = "tex"; //$NON-NLS-1$
	private static final String TEXTURE_DATA_FILE_SUFFIX = ".dat"; //$NON-NLS-1$
	private static EnumSet<GLEnum> sTexParameterPnameValues;

	/**
	 * Construct a list of transformations to be applied for the provided OpenGL
	 * call.
	 */
	public static List<IStateTransform> getTransformsFor(GLMessage msg) {
		switch (msg.getFunction()) {
		case eglCreateContext:
			return transformsForEglCreateContext(msg);
		case glBindFramebuffer:
			return transformsForGlBindFramebuffer(msg);

			// vertex data
		case glVertexAttribPointer:
			return transformsForGlVertexAttribPointer(msg);
		case glVertexAttrib1f:
		case glVertexAttrib2f:
		case glVertexAttrib3f:
		case glVertexAttrib4f:
			return transformsForGlVertexAttribxf(msg);
		case glVertexAttrib1fv:
		case glVertexAttrib2fv:
		case glVertexAttrib3fv:
		case glVertexAttrib4fv:
			return transformsForGlVertexAttribxfv(msg);
		case glEnableVertexAttribArray:
			return transformsForGlEnableVertexAttribArray(msg);
		case glDisableVertexAttribArray:
			return transformsForGlDisableVertexAttribArray(msg);

			// VBO's
		case glBindBuffer:
			return transformsForGlBindBuffer(msg);
		case glGenBuffers:
			return transformsForGlGenBuffers(msg);
		case glDeleteBuffers:
			return transformsForGlDeleteBuffers(msg);
		case glBufferData:
			return transformsForGlBufferData(msg);
		case glBufferSubData:
			return transformsForGlBufferSubData(msg);

			// transformation state
		case glViewport:
			return transformsForGlViewport(msg);
		case glDepthRangef:
			return transformsForGlDepthRangef(msg);

			// rasterization
		case glLineWidth:
			return transformsForGlLineWidth(msg);
		case glCullFace:
			return transformsForGlCullFace(msg);
		case glFrontFace:
			return transformsForGlFrontFace(msg);
		case glPolygonOffset:
			return transformsForGlPolygonOffset(msg);

			// pixel operations
		case glScissor:
			return transformsForGlScissor(msg);
		case glStencilFunc:
			return transformsForGlStencilFunc(msg);
		case glStencilFuncSeparate:
			return transformsForGlStencilFuncSeparate(msg);
		case glStencilOp:
			return transformsForGlStencilOp(msg);
		case glStencilOpSeparate:
			return transformsForGlStencilOpSeparate(msg);
		case glDepthFunc:
			return transformsForGlDepthFunc(msg);
		case glBlendEquation:
			return transformsForGlBlendEquation(msg);
		case glBlendEquationSeparate:
			return transformsForGlBlendEquationSeparate(msg);
		case glBlendFunc:
			return transformsForGlBlendFunc(msg);
		case glBlendFuncSeparate:
			return transformsForGlBlendFuncSeparate(msg);
		case glPixelStorei:
			return transformsForGlPixelStorei(msg);

			// Texture State Transformations
		case glGenTextures:
			return transformsForGlGenTextures(msg);
		case glDeleteTextures:
			return transformsForGlDeleteTextures(msg);
		case glActiveTexture:
			return transformsForGlActiveTexture(msg);
		case glBindTexture:
			return transformsForGlBindTexture(msg);
		case glTexImage2D:
			return transformsForGlTexImage2D(msg);
		case glTexSubImage2D:
			return transformsForGlTexSubImage2D(msg);
		case glTexParameteri:
			return transformsForGlTexParameter(msg);

			// Program State Transformations
		case glCreateProgram:
			return transformsForGlCreateProgram(msg);
		case glUseProgram:
			return transformsForGlUseProgram(msg);
		case glAttachShader:
			return transformsForGlAttachShader(msg);
		case glDetachShader:
			return transformsForGlDetachShader(msg);
		case glGetActiveAttrib:
			return transformsForGlGetActiveAttrib(msg);
		case glGetActiveUniform:
			return transformsForGlGetActiveUniform(msg);
		case glUniform1i:
		case glUniform2i:
		case glUniform3i:
		case glUniform4i:
			return transformsForGlUniform(msg, false);
		case glUniform1f:
		case glUniform2f:
		case glUniform3f:
		case glUniform4f:
			return transformsForGlUniform(msg, true);
		case glUniform1iv:
		case glUniform2iv:
		case glUniform3iv:
		case glUniform4iv:
			return transformsForGlUniformv(msg, false);
		case glUniform1fv:
		case glUniform2fv:
		case glUniform3fv:
		case glUniform4fv:
			return transformsForGlUniformv(msg, true);
		case glUniformMatrix2fv:
		case glUniformMatrix3fv:
		case glUniformMatrix4fv:
			return transformsForGlUniformMatrix(msg);

			// Shader State Transformations
		case glCreateShader:
			return transformsForGlCreateShader(msg);
		case glDeleteShader:
			return transformsForGlDeleteShader(msg);
		case glShaderSource:
			return transformsForGlShaderSource(msg);
		default:
			return Collections.emptyList();
		}
	}

	private static List<IStateTransform> transformsForGlVertexAttribPointer(GLMessage msg) {
		int index = msg.getArgs(0).getIntValue(0);

		int size = msg.getArgs(1).getIntValue(0);
		int type = msg.getArgs(2).getIntValue(0);
		boolean normalized = msg.getArgs(3).getBoolValue(0);
		int stride = msg.getArgs(4).getIntValue(0);

		long pointer;
		if (msg.getArgs(5).getIntValueCount() > 0) {
			pointer = msg.getArgs(5).getIntValue(0);
		} else {
			pointer = msg.getArgs(5).getInt64Value(0);
		}

		List<IStateTransform> transforms = new ArrayList<IStateTransform>();
		transforms.add(new PropertyChangeTransform(GLPropertyAccessor.makeAccessor(msg.getContextId(),
				GLStateType.VERTEX_ARRAY_DATA, GLStateType.VERTEX_ATTRIB_ARRAY, Integer.valueOf(index),
				GLStateType.VERTEX_ATTRIB_ARRAY_SIZE), Integer.valueOf(size)));
		transforms.add(new PropertyChangeTransform(GLPropertyAccessor.makeAccessor(msg.getContextId(),
				GLStateType.VERTEX_ARRAY_DATA, GLStateType.VERTEX_ATTRIB_ARRAY, Integer.valueOf(index),
				GLStateType.VERTEX_ATTRIB_ARRAY_TYPE), GLEnum.valueOf(type)));
		transforms.add(new PropertyChangeTransform(GLPropertyAccessor.makeAccessor(msg.getContextId(),
				GLStateType.VERTEX_ARRAY_DATA, GLStateType.VERTEX_ATTRIB_ARRAY, Integer.valueOf(index),
				GLStateType.VERTEX_ATTRIB_ARRAY_NORMALIZED), Boolean.valueOf(normalized)));
		transforms.add(new PropertyChangeTransform(GLPropertyAccessor.makeAccessor(msg.getContextId(),
				GLStateType.VERTEX_ARRAY_DATA, GLStateType.VERTEX_ATTRIB_ARRAY, Integer.valueOf(index),
				GLStateType.VERTEX_ATTRIB_ARRAY_STRIDE), Integer.valueOf(stride)));
		transforms.add(new PropertyChangeTransform(GLPropertyAccessor.makeAccessor(msg.getContextId(),
				GLStateType.VERTEX_ARRAY_DATA, GLStateType.VERTEX_ATTRIB_ARRAY, Integer.valueOf(index),
				GLStateType.VERTEX_ATTRIB_ARRAY_POINTER), Long.valueOf(pointer)));
		return transforms;
	}

	private static List<IStateTransform> transformsForGlVertexAttrib(int context, int index, float v0, float v1,
			float v2, float v3) {
		List<IStateTransform> transforms = new ArrayList<IStateTransform>(4);
		transforms.add(new PropertyChangeTransform(GLPropertyAccessor.makeAccessor(context,
				GLStateType.VERTEX_ARRAY_DATA, GLStateType.GENERIC_VERTEX_ATTRIBUTES, Integer.valueOf(index),
				GLStateType.GENERIC_VERTEX_ATTRIB_V0), Float.valueOf(v0)));
		transforms.add(new PropertyChangeTransform(GLPropertyAccessor.makeAccessor(context,
				GLStateType.VERTEX_ARRAY_DATA, GLStateType.GENERIC_VERTEX_ATTRIBUTES, Integer.valueOf(index),
				GLStateType.GENERIC_VERTEX_ATTRIB_V1), Float.valueOf(v1)));
		transforms.add(new PropertyChangeTransform(GLPropertyAccessor.makeAccessor(context,
				GLStateType.VERTEX_ARRAY_DATA, GLStateType.GENERIC_VERTEX_ATTRIBUTES, Integer.valueOf(index),
				GLStateType.GENERIC_VERTEX_ATTRIB_V2), Float.valueOf(v2)));
		transforms.add(new PropertyChangeTransform(GLPropertyAccessor.makeAccessor(context,
				GLStateType.VERTEX_ARRAY_DATA, GLStateType.GENERIC_VERTEX_ATTRIBUTES, Integer.valueOf(index),
				GLStateType.GENERIC_VERTEX_ATTRIB_V3), Float.valueOf(v3)));
		return transforms;
	}

	private static List<IStateTransform> transformsForGlVertexAttribxf(GLMessage msg) {
		// void glVertexAttrib1f(GLuint index, GLfloat v0);
		// void glVertexAttrib2f(GLuint index, GLfloat v0, GLfloat v1);
		// void glVertexAttrib3f(GLuint index, GLfloat v0, GLfloat v1, GLfloat
		// v2);
		// void glVertexAttrib4f(GLuint index, GLfloat v0, GLfloat v1, GLfloat
		// v2, GLfloat v3);

		int index = msg.getArgs(0).getIntValue(0);
		float v0 = msg.getArgs(1).getFloatValue(0);
		float v1 = msg.getArgsCount() > 2 ? msg.getArgs(2).getFloatValue(0) : 0;
		float v2 = msg.getArgsCount() > 3 ? msg.getArgs(3).getFloatValue(0) : 0;
		float v3 = msg.getArgsCount() > 4 ? msg.getArgs(4).getFloatValue(0) : 0;

		return transformsForGlVertexAttrib(msg.getContextId(), index, v0, v1, v2, v3);
	}

	private static List<IStateTransform> transformsForGlVertexAttribxfv(GLMessage msg) {
		// void glVertexAttrib1fv(GLuint index, const GLfloat *v);
		// void glVertexAttrib2fv(GLuint index, const GLfloat *v);
		// void glVertexAttrib3fv(GLuint index, const GLfloat *v);
		// void glVertexAttrib4fv(GLuint index, const GLfloat *v);

		int index = msg.getArgs(0).getIntValue(0);
		float v[] = new float[4];

		for (int i = 0; i < msg.getArgs(1).getFloatValueList().size(); i++) {
			v[i] = msg.getArgs(1).getFloatValue(i);
		}

		return transformsForGlVertexAttrib(msg.getContextId(), index, v[0], v[1], v[2], v[3]);
	}

	private static List<IStateTransform> transformsForGlEnableVertexAttribArray(GLMessage msg) {
		// void glEnableVertexAttribArray(GLuint index);
		// void glDisableVertexAttribArray(GLuint index);

		int index = msg.getArgs(0).getIntValue(0);
		IStateTransform transform = new PropertyChangeTransform(GLPropertyAccessor.makeAccessor(msg.getContextId(),
				GLStateType.VERTEX_ARRAY_DATA, GLStateType.VERTEX_ATTRIB_ARRAY, Integer.valueOf(index),
				GLStateType.VERTEX_ATTRIB_ARRAY_ENABLED), Boolean.TRUE);
		return Collections.singletonList(transform);
	}

	private static List<IStateTransform> transformsForGlDisableVertexAttribArray(GLMessage msg) {
		// void glEnableVertexAttribArray(GLuint index);
		// void glDisableVertexAttribArray(GLuint index);

		int index = msg.getArgs(0).getIntValue(0);
		IStateTransform transform = new PropertyChangeTransform(GLPropertyAccessor.makeAccessor(msg.getContextId(),
				GLStateType.VERTEX_ARRAY_DATA, GLStateType.VERTEX_ATTRIB_ARRAY, Integer.valueOf(index),
				GLStateType.VERTEX_ATTRIB_ARRAY_ENABLED), Boolean.FALSE);
		return Collections.singletonList(transform);
	}

	private static List<IStateTransform> transformsForGlBindBuffer(GLMessage msg) {
		// void glBindBuffer(GLenum target, GLuint buffer);
		// target is one of GL_ARRAY_BUFFER or GL_ELEMENT_ARRAY_BUFFER.

		GLEnum target = GLEnum.valueOf(msg.getArgs(0).getIntValue(0));
		int buffer = msg.getArgs(1).getIntValue(0);
		GLStateType bufferType;

		if (target == GLEnum.GL_ARRAY_BUFFER) {
			bufferType = GLStateType.ARRAY_BUFFER_BINDING;
		} else {
			bufferType = GLStateType.ELEMENT_ARRAY_BUFFER_BINDING;
		}

		IStateTransform transform = new PropertyChangeTransform(GLPropertyAccessor.makeAccessor(msg.getContextId(),
				GLStateType.VERTEX_ARRAY_DATA, GLStateType.BUFFER_BINDINGS, bufferType), Integer.valueOf(buffer));
		return Collections.singletonList(transform);
	}

	private static List<IStateTransform> transformsForGlGenBuffers(GLMessage msg) {
		// void glGenBuffers(GLsizei n, GLuint * buffers);
		int n = msg.getArgs(0).getIntValue(0);
		List<IStateTransform> transforms = new ArrayList<IStateTransform>();

		for (int i = 0; i < n; i++) {
			transforms.add(new SparseArrayElementAddTransform(GLPropertyAccessor.makeAccessor(msg.getContextId(),
					GLStateType.VERTEX_ARRAY_DATA, GLStateType.VBO), msg.getArgs(1).getIntValue(i)));
		}

		return transforms;
	}

	private static List<IStateTransform> transformsForGlDeleteBuffers(GLMessage msg) {
		// void glDeleteBuffers(GLsizei n, const GLuint * buffers);
		int n = msg.getArgs(0).getIntValue(0);
		List<IStateTransform> transforms = new ArrayList<IStateTransform>();

		for (int i = 0; i < n; i++) {
			transforms.add(new SparseArrayElementRemoveTransform(GLPropertyAccessor.makeAccessor(msg.getContextId(),
					GLStateType.VERTEX_ARRAY_DATA, GLStateType.VBO), msg.getArgs(1).getIntValue(i)));
		}

		return transforms;
	}

	private static List<IStateTransform> transformsForGlBufferData(GLMessage msg) {
		// void glBufferData(GLenum target, GLsizeiptr size, const GLvoid *
		// data, GLenum usage);
		GLEnum target = GLEnum.valueOf(msg.getArgs(0).getIntValue(0));
		int size = msg.getArgs(1).getIntValue(0);
		byte[] data = null;
		GLEnum usage = GLEnum.valueOf(msg.getArgs(3).getIntValue(0));

		if (msg.getArgs(2).getRawBytesList().size() > 0) {
			data = msg.getArgs(2).getRawBytesList().get(0).toByteArray();
		} else {
			data = new byte[size];
		}

		List<IStateTransform> transforms = new ArrayList<IStateTransform>();

		transforms.add(new PropertyChangeTransform(new CurrentVboPropertyAccessor(msg.getContextId(), target,
				GLStateType.BUFFER_SIZE), Integer.valueOf(size)));
		transforms.add(new PropertyChangeTransform(new CurrentVboPropertyAccessor(msg.getContextId(), target,
				GLStateType.BUFFER_DATA), data));
		transforms.add(new PropertyChangeTransform(new CurrentVboPropertyAccessor(msg.getContextId(), target,
				GLStateType.BUFFER_USAGE), usage));
		transforms.add(new PropertyChangeTransform(new CurrentVboPropertyAccessor(msg.getContextId(), target,
				GLStateType.BUFFER_TYPE), target));
		return transforms;
	}

	private static List<IStateTransform> transformsForGlBufferSubData(GLMessage msg) {
		// void glBufferSubData(GLenum target, GLintptr offset, GLsizeiptr size,
		// const GLvoid * data);
		GLEnum target = GLEnum.valueOf(msg.getArgs(0).getIntValue(0));
		int offset = msg.getArgs(1).getIntValue(0);
		byte[] data = msg.getArgs(3).getRawBytesList().get(0).toByteArray();

		IStateTransform transform = new BufferSubDataTransform(new CurrentVboPropertyAccessor(msg.getContextId(),
				target, GLStateType.BUFFER_DATA), offset, data);

		return Collections.singletonList(transform);
	}

	private static List<IStateTransform> transformsForGlBindFramebuffer(GLMessage msg) {
		// void glBindFramebuffer(GLenum target, GLuint framebuffer);
		int fb = msg.getArgs(1).getIntValue(0);
		IStateTransform transform = new PropertyChangeTransform(GLPropertyAccessor.makeAccessor(msg.getContextId(),
				GLStateType.FRAMEBUFFER_STATE, GLStateType.FRAMEBUFFER_BINDING), fb);
		return Collections.singletonList(transform);
	}

	private static List<IStateTransform> transformsForGlLineWidth(GLMessage msg) {
		// void glLineWidth(GLfloat width);
		float width = msg.getArgs(0).getFloatValue(0);
		IStateTransform transform = new PropertyChangeTransform(GLPropertyAccessor.makeAccessor(msg.getContextId(),
				GLStateType.RASTERIZATION_STATE, GLStateType.LINE_WIDTH), width);
		return Collections.singletonList(transform);
	}

	private static List<IStateTransform> transformsForGlCullFace(GLMessage msg) {
		// void glCullFace(GLenum mode);
		int mode = msg.getArgs(0).getIntValue(0);
		IStateTransform transform = new PropertyChangeTransform(GLPropertyAccessor.makeAccessor(msg.getContextId(),
				GLStateType.RASTERIZATION_STATE, GLStateType.CULL_FACE_MODE), GLEnum.valueOf(mode));
		return Collections.singletonList(transform);
	}

	private static List<IStateTransform> transformsForGlFrontFace(GLMessage msg) {
		// void glFrontFace(GLenum mode);
		int mode = msg.getArgs(0).getIntValue(0);
		IStateTransform transform = new PropertyChangeTransform(GLPropertyAccessor.makeAccessor(msg.getContextId(),
				GLStateType.RASTERIZATION_STATE, GLStateType.FRONT_FACE), GLEnum.valueOf(mode));
		return Collections.singletonList(transform);
	}

	private static List<IStateTransform> transformsForGlPolygonOffset(GLMessage msg) {
		// void glPolygonOffset(GLfloat factor, GLfloat units)
		float factor = msg.getArgs(0).getFloatValue(0);
		float units = msg.getArgs(1).getFloatValue(0);

		List<IStateTransform> transforms = new ArrayList<IStateTransform>();
		transforms.add(new PropertyChangeTransform(GLPropertyAccessor.makeAccessor(msg.getContextId(),
				GLStateType.RASTERIZATION_STATE, GLStateType.POLYGON_OFFSET_FACTOR), Float.valueOf(factor)));
		transforms.add(new PropertyChangeTransform(GLPropertyAccessor.makeAccessor(msg.getContextId(),
				GLStateType.RASTERIZATION_STATE, GLStateType.POLYGON_OFFSET_UNITS), Float.valueOf(units)));
		return transforms;
	}

	private static List<IStateTransform> transformsForGlScissor(GLMessage msg) {
		// void glScissor(GLint x, GLint y, GLsizei width, GLsizei height);
		int x = msg.getArgs(0).getIntValue(0);
		int y = msg.getArgs(1).getIntValue(0);
		int w = msg.getArgs(2).getIntValue(0);
		int h = msg.getArgs(3).getIntValue(0);

		List<IStateTransform> transforms = new ArrayList<IStateTransform>();
		transforms.add(new PropertyChangeTransform(GLPropertyAccessor.makeAccessor(msg.getContextId(),
				GLStateType.PIXEL_OPERATIONS, GLStateType.SCISSOR_BOX, GLStateType.SCISSOR_BOX_X), Integer.valueOf(x)));
		transforms.add(new PropertyChangeTransform(GLPropertyAccessor.makeAccessor(msg.getContextId(),
				GLStateType.PIXEL_OPERATIONS, GLStateType.SCISSOR_BOX, GLStateType.SCISSOR_BOX_Y), Integer.valueOf(y)));
		transforms.add(new PropertyChangeTransform(GLPropertyAccessor.makeAccessor(msg.getContextId(),
				GLStateType.PIXEL_OPERATIONS, GLStateType.SCISSOR_BOX, GLStateType.SCISSOR_BOX_WIDTH), Integer
				.valueOf(w)));
		transforms.add(new PropertyChangeTransform(GLPropertyAccessor.makeAccessor(msg.getContextId(),
				GLStateType.PIXEL_OPERATIONS, GLStateType.SCISSOR_BOX, GLStateType.SCISSOR_BOX_HEIGHT), Integer
				.valueOf(h)));
		return transforms;
	}

	private static List<IStateTransform> transformsForGlStencilFuncFront(int contextId, GLEnum func, int ref, int mask) {
		List<IStateTransform> transforms = new ArrayList<IStateTransform>();
		transforms.add(new PropertyChangeTransform(GLPropertyAccessor.makeAccessor(contextId,
				GLStateType.PIXEL_OPERATIONS, GLStateType.STENCIL, GLStateType.STENCIL_FUNC), func));
		transforms.add(new PropertyChangeTransform(GLPropertyAccessor.makeAccessor(contextId,
				GLStateType.PIXEL_OPERATIONS, GLStateType.STENCIL, GLStateType.STENCIL_REF), Integer.valueOf(ref)));
		transforms.add(new PropertyChangeTransform(GLPropertyAccessor.makeAccessor(contextId,
				GLStateType.PIXEL_OPERATIONS, GLStateType.STENCIL, GLStateType.STENCIL_VALUE_MASK), Integer
				.valueOf(mask)));
		return transforms;
	}

	private static List<IStateTransform> transformsForGlStencilFuncBack(int contextId, GLEnum func, int ref, int mask) {
		List<IStateTransform> transforms = new ArrayList<IStateTransform>();
		transforms.add(new PropertyChangeTransform(GLPropertyAccessor.makeAccessor(contextId,
				GLStateType.PIXEL_OPERATIONS, GLStateType.STENCIL, GLStateType.STENCIL_BACK_FUNC), func));
		transforms
				.add(new PropertyChangeTransform(GLPropertyAccessor.makeAccessor(contextId,
						GLStateType.PIXEL_OPERATIONS, GLStateType.STENCIL, GLStateType.STENCIL_BACK_REF), Integer
						.valueOf(ref)));
		transforms.add(new PropertyChangeTransform(GLPropertyAccessor.makeAccessor(contextId,
				GLStateType.PIXEL_OPERATIONS, GLStateType.STENCIL, GLStateType.STENCIL_BACK_VALUE_MASK), Integer
				.valueOf(mask)));
		return transforms;
	}

	private static List<IStateTransform> transformsForGlStencilFunc(GLMessage msg) {
		// void glStencilFunc(GLenum func, GLint ref, GLuint mask);
		GLEnum func = GLEnum.valueOf(msg.getArgs(0).getIntValue(0));
		int ref = msg.getArgs(1).getIntValue(0);
		int mask = msg.getArgs(2).getIntValue(0);

		List<IStateTransform> transforms = new ArrayList<IStateTransform>();
		transforms.addAll(transformsForGlStencilFuncFront(msg.getContextId(), func, ref, mask));
		transforms.addAll(transformsForGlStencilFuncBack(msg.getContextId(), func, ref, mask));
		return transforms;
	}

	private static List<IStateTransform> transformsForGlStencilFuncSeparate(GLMessage msg) {
		// void glStencilFuncSeparate(GLenum face, GLenum func, GLint ref,
		// GLuint mask);
		GLEnum face = GLEnum.valueOf(msg.getArgs(0).getIntValue(0));
		GLEnum func = GLEnum.valueOf(msg.getArgs(1).getIntValue(0));
		int ref = msg.getArgs(2).getIntValue(0);
		int mask = msg.getArgs(3).getIntValue(0);

		List<IStateTransform> transforms = new ArrayList<IStateTransform>();
		if (face == GLEnum.GL_FRONT || face == GLEnum.GL_FRONT_AND_BACK) {
			transforms.addAll(transformsForGlStencilFuncFront(msg.getContextId(), func, ref, mask));
		}
		if (face == GLEnum.GL_BACK || face == GLEnum.GL_FRONT_AND_BACK) {
			transforms.addAll(transformsForGlStencilFuncBack(msg.getContextId(), func, ref, mask));
		}

		return transforms;
	}

	private static List<IStateTransform> transformsForGlStencilOpFront(int contextId, GLEnum sfail, GLEnum dpfail,
			GLEnum dppass) {
		List<IStateTransform> transforms = new ArrayList<IStateTransform>();
		transforms.add(new PropertyChangeTransform(GLPropertyAccessor.makeAccessor(contextId,
				GLStateType.PIXEL_OPERATIONS, GLStateType.STENCIL, GLStateType.STENCIL_FAIL), sfail));
		transforms.add(new PropertyChangeTransform(GLPropertyAccessor.makeAccessor(contextId,
				GLStateType.PIXEL_OPERATIONS, GLStateType.STENCIL, GLStateType.STENCIL_PASS_DEPTH_FAIL), dpfail));
		transforms.add(new PropertyChangeTransform(GLPropertyAccessor.makeAccessor(contextId,
				GLStateType.PIXEL_OPERATIONS, GLStateType.STENCIL, GLStateType.STENCIL_PASS_DEPTH_PASS), dppass));
		return transforms;
	}

	private static List<IStateTransform> transformsForGlStencilOpBack(int contextId, GLEnum sfail, GLEnum dpfail,
			GLEnum dppass) {
		List<IStateTransform> transforms = new ArrayList<IStateTransform>();
		transforms.add(new PropertyChangeTransform(GLPropertyAccessor.makeAccessor(contextId,
				GLStateType.PIXEL_OPERATIONS, GLStateType.STENCIL, GLStateType.STENCIL_BACK_FAIL), sfail));
		transforms.add(new PropertyChangeTransform(GLPropertyAccessor.makeAccessor(contextId,
				GLStateType.PIXEL_OPERATIONS, GLStateType.STENCIL, GLStateType.STENCIL_BACK_PASS_DEPTH_FAIL), dpfail));
		transforms.add(new PropertyChangeTransform(GLPropertyAccessor.makeAccessor(contextId,
				GLStateType.PIXEL_OPERATIONS, GLStateType.STENCIL, GLStateType.STENCIL_BACK_PASS_DEPTH_PASS), dppass));
		return transforms;
	}

	private static List<IStateTransform> transformsForGlStencilOp(GLMessage msg) {
		// void glStencilOp(GLenum sfail, GLenum dpfail, GLenum dppass);
		GLEnum sfail = GLEnum.valueOf(msg.getArgs(0).getIntValue(0));
		GLEnum dpfail = GLEnum.valueOf(msg.getArgs(1).getIntValue(0));
		GLEnum dppass = GLEnum.valueOf(msg.getArgs(2).getIntValue(0));
		List<IStateTransform> transforms = new ArrayList<IStateTransform>();
		transforms.addAll(transformsForGlStencilOpFront(msg.getContextId(), sfail, dpfail, dppass));
		transforms.addAll(transformsForGlStencilOpBack(msg.getContextId(), sfail, dpfail, dppass));
		return transforms;
	}

	private static List<IStateTransform> transformsForGlStencilOpSeparate(GLMessage msg) {
		// void glStencilOp(GLenum face, GLenum sfail, GLenum dpfail, GLenum
		// dppass);
		GLEnum face = GLEnum.valueOf(msg.getArgs(0).getIntValue(0));
		GLEnum sfail = GLEnum.valueOf(msg.getArgs(1).getIntValue(0));
		GLEnum dpfail = GLEnum.valueOf(msg.getArgs(2).getIntValue(0));
		GLEnum dppass = GLEnum.valueOf(msg.getArgs(3).getIntValue(0));
		List<IStateTransform> transforms = new ArrayList<IStateTransform>();

		if (face == GLEnum.GL_FRONT || face == GLEnum.GL_FRONT_AND_BACK) {
			transforms.addAll(transformsForGlStencilOpFront(msg.getContextId(), sfail, dpfail, dppass));
		}

		if (face == GLEnum.GL_BACK || face == GLEnum.GL_FRONT_AND_BACK) {
			transforms.addAll(transformsForGlStencilOpBack(msg.getContextId(), sfail, dpfail, dppass));
		}

		return transforms;
	}

	private static List<IStateTransform> transformsForGlDepthFunc(GLMessage msg) {
		// void glDepthFunc(GLenum func);
		GLEnum func = GLEnum.valueOf(msg.getArgs(0).getIntValue(0));

		IStateTransform transform = new PropertyChangeTransform(GLPropertyAccessor.makeAccessor(msg.getContextId(),
				GLStateType.PIXEL_OPERATIONS, GLStateType.DEPTH_FUNC), func);
		return Collections.singletonList(transform);
	}

	private static IStateTransform transformForGlEquationRGB(int contextId, GLEnum mode) {
		return new PropertyChangeTransform(GLPropertyAccessor.makeAccessor(contextId, GLStateType.PIXEL_OPERATIONS,
				GLStateType.BLEND, GLStateType.BLEND_EQUATION_RGB), mode);
	}

	private static IStateTransform transformForGlEquationAlpha(int contextId, GLEnum mode) {
		return new PropertyChangeTransform(GLPropertyAccessor.makeAccessor(contextId, GLStateType.PIXEL_OPERATIONS,
				GLStateType.BLEND, GLStateType.BLEND_EQUATION_ALPHA), mode);
	}

	private static List<IStateTransform> transformsForGlBlendEquationSeparate(GLMessage msg) {
		// void glBlendEquationSeparate(GLenum modeRGB, GLenum modeAlpha);
		GLEnum rgb = GLEnum.valueOf(msg.getArgs(0).getIntValue(0));
		GLEnum alpha = GLEnum.valueOf(msg.getArgs(1).getIntValue(0));

		List<IStateTransform> transforms = new ArrayList<IStateTransform>();
		transforms.add(transformForGlEquationRGB(msg.getContextId(), rgb));
		transforms.add(transformForGlEquationAlpha(msg.getContextId(), alpha));
		return transforms;
	}

	private static List<IStateTransform> transformsForGlBlendEquation(GLMessage msg) {
		// void glBlendEquation(GLenum mode);
		GLEnum mode = GLEnum.valueOf(msg.getArgs(0).getIntValue(0));

		List<IStateTransform> transforms = new ArrayList<IStateTransform>();
		transforms.add(transformForGlEquationRGB(msg.getContextId(), mode));
		transforms.add(transformForGlEquationAlpha(msg.getContextId(), mode));
		return transforms;
	}

	private static List<IStateTransform> transformsForGlBlendFuncSrcDst(boolean src, int contextId, GLEnum rgb,
			GLEnum alpha) {
		List<IStateTransform> transforms = new ArrayList<IStateTransform>();

		GLStateType rgbAccessor = GLStateType.BLEND_DST_RGB;
		GLStateType alphaAccessor = GLStateType.BLEND_DST_ALPHA;
		if (src) {
			rgbAccessor = GLStateType.BLEND_SRC_RGB;
			alphaAccessor = GLStateType.BLEND_SRC_ALPHA;
		}

		transforms.add(new PropertyChangeTransform(GLPropertyAccessor.makeAccessor(contextId,
				GLStateType.PIXEL_OPERATIONS, GLStateType.BLEND, rgbAccessor), rgb));
		transforms.add(new PropertyChangeTransform(GLPropertyAccessor.makeAccessor(contextId,
				GLStateType.PIXEL_OPERATIONS, GLStateType.BLEND, alphaAccessor), alpha));
		return transforms;
	}

	private static List<IStateTransform> transformsForGlBlendFuncSeparate(GLMessage msg) {
		// void glBlendFuncSeparate(GLenum srcRGB, GLenum dstRGB, GLenum
		// srcAlpha, GLenum dstAlpha)
		GLEnum srcRgb = GLEnum.valueOf(msg.getArgs(0).getIntValue(0));
		GLEnum dstRgb = GLEnum.valueOf(msg.getArgs(1).getIntValue(0));
		GLEnum srcAlpha = GLEnum.valueOf(msg.getArgs(2).getIntValue(0));
		GLEnum dstAlpha = GLEnum.valueOf(msg.getArgs(3).getIntValue(0));

		List<IStateTransform> transforms = new ArrayList<IStateTransform>();
		transforms.addAll(transformsForGlBlendFuncSrcDst(true, msg.getContextId(), srcRgb, srcAlpha));
		transforms.addAll(transformsForGlBlendFuncSrcDst(false, msg.getContextId(), dstRgb, dstAlpha));
		return transforms;
	}

	private static List<IStateTransform> transformsForGlBlendFunc(GLMessage msg) {
		// void glBlendFunc(GLenum sfactor, GLenum dfactor);
		GLEnum sfactor = GLEnum.valueOf(msg.getArgs(0).getIntValue(0));
		GLEnum dfactor = GLEnum.valueOf(msg.getArgs(1).getIntValue(0));

		List<IStateTransform> transforms = new ArrayList<IStateTransform>();
		transforms.addAll(transformsForGlBlendFuncSrcDst(true, msg.getContextId(), sfactor, sfactor));
		transforms.addAll(transformsForGlBlendFuncSrcDst(false, msg.getContextId(), dfactor, dfactor));
		return transforms;
	}

	private static List<IStateTransform> transformsForGlPixelStorei(GLMessage msg) {
		// void glPixelStorei(GLenum pname, GLint param);
		GLEnum pname = GLEnum.valueOf(msg.getArgs(0).getIntValue(0));
		Integer param = Integer.valueOf(msg.getArgs(1).getIntValue(0));

		IStateTransform transform;
		if (pname == GLEnum.GL_PACK_ALIGNMENT) {
			transform = new PropertyChangeTransform(GLPropertyAccessor.makeAccessor(msg.getContextId(),
					GLStateType.PIXEL_PACKING, GLStateType.PACK_ALIGNMENT), param);
		} else {
			transform = new PropertyChangeTransform(GLPropertyAccessor.makeAccessor(msg.getContextId(),
					GLStateType.PIXEL_PACKING, GLStateType.UNPACK_ALIGNMENT), param);
		}

		return Collections.singletonList(transform);
	}

	private static List<IStateTransform> transformsForGlViewport(GLMessage msg) {
		// void glViewport( GLint x, GLint y, GLsizei width, GLsizei height);
		int x = msg.getArgs(0).getIntValue(0);
		int y = msg.getArgs(1).getIntValue(0);
		int w = msg.getArgs(2).getIntValue(0);
		int h = msg.getArgs(3).getIntValue(0);

		List<IStateTransform> transforms = new ArrayList<IStateTransform>();
		transforms.add(new PropertyChangeTransform(GLPropertyAccessor.makeAccessor(msg.getContextId(),
				GLStateType.TRANSFORMATION_STATE, GLStateType.VIEWPORT, GLStateType.VIEWPORT_X), Integer.valueOf(x)));
		transforms.add(new PropertyChangeTransform(GLPropertyAccessor.makeAccessor(msg.getContextId(),
				GLStateType.TRANSFORMATION_STATE, GLStateType.VIEWPORT, GLStateType.VIEWPORT_Y), Integer.valueOf(y)));
		transforms
				.add(new PropertyChangeTransform(GLPropertyAccessor.makeAccessor(msg.getContextId(),
						GLStateType.TRANSFORMATION_STATE, GLStateType.VIEWPORT, GLStateType.VIEWPORT_WIDTH), Integer
						.valueOf(w)));
		transforms.add(new PropertyChangeTransform(GLPropertyAccessor.makeAccessor(msg.getContextId(),
				GLStateType.TRANSFORMATION_STATE, GLStateType.VIEWPORT, GLStateType.VIEWPORT_HEIGHT), Integer
				.valueOf(h)));
		return transforms;
	}

	private static List<IStateTransform> transformsForGlDepthRangef(GLMessage msg) {
		// void glDepthRangef(GLclampf nearVal, GLclampf farVal);
		float near = msg.getArgs(0).getFloatValue(0);
		float far = msg.getArgs(1).getFloatValue(0);

		List<IStateTransform> transforms = new ArrayList<IStateTransform>();
		transforms.add(new PropertyChangeTransform(GLPropertyAccessor.makeAccessor(msg.getContextId(),
				GLStateType.TRANSFORMATION_STATE, GLStateType.DEPTH_RANGE, GLStateType.DEPTH_RANGE_NEAR), Float
				.valueOf(near)));
		transforms.add(new PropertyChangeTransform(GLPropertyAccessor.makeAccessor(msg.getContextId(),
				GLStateType.TRANSFORMATION_STATE, GLStateType.DEPTH_RANGE, GLStateType.DEPTH_RANGE_FAR), Float
				.valueOf(far)));
		return transforms;
	}

	private static List<IStateTransform> transformsForGlGenTextures(GLMessage msg) {
		// void glGenTextures(GLsizei n, GLuint *textures);
		int n = msg.getArgs(0).getIntValue(0);

		List<IStateTransform> transforms = new ArrayList<IStateTransform>();
		for (int i = 0; i < n; i++) {
			int texture = msg.getArgs(1).getIntValue(i);
			transforms.add(new SparseArrayElementAddTransform(GLPropertyAccessor.makeAccessor(msg.getContextId(),
					GLStateType.TEXTURE_STATE, GLStateType.TEXTURES), texture));
		}

		return transforms;
	}

	/**
	 * Obtain a list of transforms that will reset any existing texture units
	 * that are bound to provided texture.
	 * 
	 * @param contextId
	 *            context to operate on
	 * @param texture
	 *            texture that should be unbound
	 */
	private static List<IStateTransform> transformsToResetBoundTextureUnits(int contextId, int texture) {
		List<IStateTransform> transforms = new ArrayList<IStateTransform>(GLState.TEXTURE_UNIT_COUNT);

		for (int i = 0; i < GLState.TEXTURE_UNIT_COUNT; i++) {
			transforms.add(new PropertyChangeTransform(GLPropertyAccessor.makeAccessor(contextId,
					GLStateType.TEXTURE_STATE, GLStateType.TEXTURE_UNITS, Integer.valueOf(i),
					GLStateType.TEXTURE_BINDING_2D), Integer.valueOf(0), /*
																		 * reset
																		 * binding
																		 * to
																		 * texture
																		 * 0
																		 */
			Predicates.matchesInteger(texture) /*
												 * only if currently bound to
												 * @texture
												 */));
		}
		return transforms;
	}

	private static List<IStateTransform> transformsForGlDeleteTextures(GLMessage msg) {
		// void glDeleteTextures(GLsizei n, const GLuint * textures);
		int n = msg.getArgs(0).getIntValue(0);

		List<IStateTransform> transforms = new ArrayList<IStateTransform>(n);
		for (int i = 0; i < n; i++) {
			int texture = msg.getArgs(1).getIntValue(i);
			transforms.add(new SparseArrayElementRemoveTransform(GLPropertyAccessor.makeAccessor(msg.getContextId(),
					GLStateType.TEXTURE_STATE, GLStateType.TEXTURES), texture));
			transforms.addAll(transformsToResetBoundTextureUnits(msg.getContextId(), texture));
		}

		return transforms;
	}

	private static List<IStateTransform> transformsForGlActiveTexture(GLMessage msg) {
		// void glActiveTexture(GLenum texture);
		GLEnum texture = GLEnum.valueOf(msg.getArgs(0).getIntValue(0));
		Integer textureIndex = Integer.valueOf((int) (texture.value - GLEnum.GL_TEXTURE0.value));
		IStateTransform transform = new PropertyChangeTransform(GLPropertyAccessor.makeAccessor(msg.getContextId(),
				GLStateType.TEXTURE_STATE, GLStateType.ACTIVE_TEXTURE_UNIT), textureIndex);
		return Collections.singletonList(transform);
	}

	private static GLStateType getTextureUnitTargetName(GLEnum target) {
		if (target == GLEnum.GL_TEXTURE_CUBE_MAP) {
			return GLStateType.TEXTURE_BINDING_CUBE_MAP;
		} else if (target == GLEnum.GL_TEXTURE_EXTERNAL) {
			// added by OES_EGL_image_external
			return GLStateType.TEXTURE_BINDING_EXTERNAL;
		} else {
			return GLStateType.TEXTURE_BINDING_2D;
		}
	}

	private static GLStateType getTextureTargetName(GLEnum pname) {
		switch (pname) {
		case GL_TEXTURE_MIN_FILTER:
			return GLStateType.TEXTURE_MIN_FILTER;
		case GL_TEXTURE_MAG_FILTER:
			return GLStateType.TEXTURE_MAG_FILTER;
		case GL_TEXTURE_WRAP_S:
			return GLStateType.TEXTURE_WRAP_S;
		case GL_TEXTURE_WRAP_T:
			return GLStateType.TEXTURE_WRAP_T;
		}

		assert false : "glTexParameter's pname argument does not support provided value.";
		return GLStateType.TEXTURE_MIN_FILTER;
	}

	private static List<IStateTransform> transformsForGlBindTexture(GLMessage msg) {
		// void glBindTexture(GLenum target, GLuint texture);
		GLEnum target = GLEnum.valueOf(msg.getArgs(0).getIntValue(0));
		Integer texture = Integer.valueOf(msg.getArgs(1).getIntValue(0));

		IStateTransform transform = new PropertyChangeTransform(new TextureUnitPropertyAccessor(msg.getContextId(),
				getTextureUnitTargetName(target)), texture);
		return Collections.singletonList(transform);
	}

	/**
	 * Utility function used by both
	 * {@link #transformsForGlTexImage2D(GLMessage) and {
	 * @link #transformsForGlTexSubImage2D(GLMessage)}.
	 */
	private static List<IStateTransform> transformsForGlTexImage(GLMessage msg, int widthArgIndex, int heightArgIndex,
			int xOffsetIndex, int yOffsetIndex) {
		GLEnum target = GLEnum.valueOf(msg.getArgs(0).getIntValue(0));
		int level = msg.getArgs(1).getIntValue(0);
		Integer width = Integer.valueOf(msg.getArgs(widthArgIndex).getIntValue(0));
		Integer height = Integer.valueOf(msg.getArgs(heightArgIndex).getIntValue(0));
		GLEnum format = GLEnum.valueOf(msg.getArgs(6).getIntValue(0));
		GLEnum type = GLEnum.valueOf(msg.getArgs(7).getIntValue(0));

		List<IStateTransform> transforms = new ArrayList<IStateTransform>();
		transforms.add(new PropertyChangeTransform(new TexturePropertyAccessor(msg.getContextId(),
				getTextureUnitTargetName(target), level, GLStateType.TEXTURE_WIDTH), width));
		transforms.add(new PropertyChangeTransform(new TexturePropertyAccessor(msg.getContextId(),
				getTextureUnitTargetName(target), level, GLStateType.TEXTURE_HEIGHT), height));
		transforms.add(new PropertyChangeTransform(new TexturePropertyAccessor(msg.getContextId(),
				getTextureUnitTargetName(target), level, GLStateType.TEXTURE_FORMAT), format));
		transforms.add(new PropertyChangeTransform(new TexturePropertyAccessor(msg.getContextId(),
				getTextureUnitTargetName(target), level, GLStateType.TEXTURE_IMAGE_TYPE), type));

		// if texture data is available, extract and store it in the cache
		// folder
		File f = null;
		if (msg.getArgs(8).getIsArray()) {
			ByteString data = msg.getArgs(8).getRawBytes(0);
			f = FileUtils.createTempFile(TEXTURE_DATA_FILE_PREFIX, TEXTURE_DATA_FILE_SUFFIX);
			try {
				Files.write(data.toByteArray(), f);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		int xOffset = 0;
		int yOffset = 0;

		if (xOffsetIndex >= 0) {
			xOffset = msg.getArgs(xOffsetIndex).getIntValue(0);
		}

		if (yOffsetIndex >= 0) {
			yOffset = msg.getArgs(yOffsetIndex).getIntValue(0);
		}

		transforms.add(new TexImageTransform(new TexturePropertyAccessor(msg.getContextId(),
				getTextureUnitTargetName(target), level, GLStateType.TEXTURE_IMAGE), f, format, type, xOffset, yOffset,
				width, height));

		return transforms;
	}

	private static List<IStateTransform> transformsForGlTexImage2D(GLMessage msg) {
		// void glTexImage2D(GLenum target, GLint level, GLint internalformat,
		// GLsizei width,
		// GLsizei height, GLint border, GLenum format, GLenum type, const
		// GLvoid *data);
		return transformsForGlTexImage(msg, 3, 4, -1, -1);
	}

	private static List<IStateTransform> transformsForGlTexSubImage2D(GLMessage msg) {
		// void glTexSubImage2D(GLenum target, GLint level, GLint xoffset, GLint
		// yoffset,
		// GLsizei width, GLsizei height, GLenum format, GLenum type, const
		// GLvoid *data);
		return transformsForGlTexImage(msg, 4, 5, 2, 3);
	}

	private static List<IStateTransform> transformsForGlTexParameter(GLMessage msg) {
		// void glTexParameteri(GLenum target, GLenum pname, GLint param);
		GLEnum target = GLEnum.valueOf(msg.getArgs(0).getIntValue(0));
		GLEnum pname = GLEnum.valueOf(msg.getArgs(1).getIntValue(0));
		GLEnum pvalue = GLEnum.valueOf(msg.getArgs(2).getIntValue(0));

		if (sTexParameterPnameValues == null) {
			GLEnum[] pnameValues = new GLEnum[] { GLEnum.GL_TEXTURE_BASE_LEVEL, GLEnum.GL_TEXTURE_COMPARE_FUNC,
					GLEnum.GL_TEXTURE_COMPARE_MODE, GLEnum.GL_TEXTURE_MIN_FILTER, GLEnum.GL_TEXTURE_MAG_FILTER,
					GLEnum.GL_TEXTURE_MIN_LOD, GLEnum.GL_TEXTURE_MAX_LOD, GLEnum.GL_TEXTURE_MAX_LEVEL,
					GLEnum.GL_TEXTURE_SWIZZLE_R, GLEnum.GL_TEXTURE_SWIZZLE_G, GLEnum.GL_TEXTURE_SWIZZLE_B,
					GLEnum.GL_TEXTURE_SWIZZLE_A, GLEnum.GL_TEXTURE_WRAP_S, GLEnum.GL_TEXTURE_WRAP_T,
					GLEnum.GL_TEXTURE_WRAP_R };
			sTexParameterPnameValues = EnumSet.copyOf(Arrays.asList(pnameValues));
		}

		if (!sTexParameterPnameValues.contains(pname)) {
			throw new IllegalArgumentException(String.format("Unsupported parameter (%s) for glTexParameter()", pname));
		}

		IStateTransform transform = new PropertyChangeTransform(new TexturePropertyAccessor(msg.getContextId(),
				getTextureUnitTargetName(target), getTextureTargetName(pname)), pvalue);
		return Collections.singletonList(transform);
	}

	private static List<IStateTransform> transformsForGlCreateProgram(GLMessage msg) {
		// GLuint glCreateProgram(void);
		int program = msg.getReturnValue().getIntValue(0);

		IStateTransform transform = new SparseArrayElementAddTransform(GLPropertyAccessor.makeAccessor(
				msg.getContextId(), GLStateType.PROGRAM_STATE, GLStateType.PROGRAMS), program);
		return Collections.singletonList(transform);
	}

	private static List<IStateTransform> transformsForGlUseProgram(GLMessage msg) {
		// void glUseProgram(GLuint program);
		Integer program = Integer.valueOf(msg.getArgs(0).getIntValue(0));

		IStateTransform transform = new PropertyChangeTransform(GLPropertyAccessor.makeAccessor(msg.getContextId(),
				GLStateType.PROGRAM_STATE, GLStateType.CURRENT_PROGRAM), program);
		return Collections.singletonList(transform);
	}

	private static List<IStateTransform> transformsForGlAttachShader(GLMessage msg) {
		// void glAttachShader(GLuint program, GLuint shader);
		int program = msg.getArgs(0).getIntValue(0);
		int shader = msg.getArgs(1).getIntValue(0);

		IStateTransform transform = new SparseArrayElementAddTransform(GLPropertyAccessor.makeAccessor(
				msg.getContextId(), GLStateType.PROGRAM_STATE, GLStateType.PROGRAMS, Integer.valueOf(program),
				GLStateType.ATTACHED_SHADERS), Integer.valueOf(shader));
		return Collections.singletonList(transform);
	}

	private static List<IStateTransform> transformsForGlDetachShader(GLMessage msg) {
		// void glDetachShader(GLuint program, GLuint shader);
		int program = msg.getArgs(0).getIntValue(0);
		int shader = msg.getArgs(1).getIntValue(0);

		IStateTransform transform = new SparseArrayElementRemoveTransform(GLPropertyAccessor.makeAccessor(
				msg.getContextId(), GLStateType.PROGRAM_STATE, GLStateType.PROGRAMS, Integer.valueOf(program),
				GLStateType.ATTACHED_SHADERS), Integer.valueOf(shader));
		return Collections.singletonList(transform);
	}

	private static List<IStateTransform> transformsForGlGetActiveAttribOrUniform(GLMessage msg, boolean isAttrib) {
		// void glGetActive[Attrib|Uniform](GLuint program, GLuint index,
		// GLsizei bufsize,
		// GLsizei* length, GLint* size, GLenum* type, GLchar* name);
		int program = msg.getArgs(0).getIntValue(0);
		int size = msg.getArgs(4).getIntValue(0);
		GLEnum type = GLEnum.valueOf(msg.getArgs(5).getIntValue(0));
		String name = msg.getArgs(6).getCharValue(0).toStringUtf8();

		// The 2nd argument (index) does not give the correct location of the
		// attribute/uniform in device. The actual location is obtained from
		// the getAttribLocation or getUniformLocation calls. The trace library
		// appends this value as an additional last argument to this call.
		int location = msg.getArgs(7).getIntValue(0);

		GLStateType activeInput;
		GLStateType inputName;
		GLStateType inputType;
		GLStateType inputSize;

		if (isAttrib) {
			activeInput = GLStateType.ACTIVE_ATTRIBUTES;
			inputName = GLStateType.ATTRIBUTE_NAME;
			inputType = GLStateType.ATTRIBUTE_TYPE;
			inputSize = GLStateType.ATTRIBUTE_SIZE;
		} else {
			activeInput = GLStateType.ACTIVE_UNIFORMS;
			inputName = GLStateType.UNIFORM_NAME;
			inputType = GLStateType.UNIFORM_TYPE;
			inputSize = GLStateType.UNIFORM_SIZE;
		}

		IStateTransform addAttribute = new SparseArrayElementAddTransform(GLPropertyAccessor.makeAccessor(
				msg.getContextId(), GLStateType.PROGRAM_STATE, GLStateType.PROGRAMS, Integer.valueOf(program),
				activeInput), Integer.valueOf(location));
		IStateTransform setAttributeName = new PropertyChangeTransform(GLPropertyAccessor.makeAccessor(
				msg.getContextId(), GLStateType.PROGRAM_STATE, GLStateType.PROGRAMS, Integer.valueOf(program),
				activeInput, Integer.valueOf(location), inputName), name);
		IStateTransform setAttributeType = new PropertyChangeTransform(GLPropertyAccessor.makeAccessor(
				msg.getContextId(), GLStateType.PROGRAM_STATE, GLStateType.PROGRAMS, Integer.valueOf(program),
				activeInput, Integer.valueOf(location), inputType), type);
		IStateTransform setAttributeSize = new PropertyChangeTransform(GLPropertyAccessor.makeAccessor(
				msg.getContextId(), GLStateType.PROGRAM_STATE, GLStateType.PROGRAMS, Integer.valueOf(program),
				activeInput, Integer.valueOf(location), inputSize), Integer.valueOf(size));
		return Arrays.asList(addAttribute, setAttributeName, setAttributeType, setAttributeSize);
	}

	private static List<IStateTransform> transformsForGlGetActiveAttrib(GLMessage msg) {
		return transformsForGlGetActiveAttribOrUniform(msg, true);
	}

	private static List<IStateTransform> transformsForGlGetActiveUniform(GLMessage msg) {
		return transformsForGlGetActiveAttribOrUniform(msg, false);
	}

	private static List<IStateTransform> transformsForGlUniformMatrix(GLMessage msg) {
		// void glUniformMatrix[2|3|4]fv(GLint location, GLsizei count,
		// GLboolean transpose,
		// const GLfloat *value);
		int location = msg.getArgs(0).getIntValue(0);
		List<Float> uniforms = msg.getArgs(3).getFloatValueList();

		IStateTransform setValues = new PropertyChangeTransform(new CurrentProgramPropertyAccessor(msg.getContextId(),
				GLStateType.ACTIVE_UNIFORMS, location, GLStateType.UNIFORM_VALUE), uniforms);

		return Collections.singletonList(setValues);
	}

	private static List<IStateTransform> transformsForGlUniformv(GLMessage msg, boolean isFloats) {
		// void glUniform1fv(GLint location, GLsizei count, const GLfloat
		// *value);
		int location = msg.getArgs(0).getIntValue(0);
		List<?> uniforms;
		if (isFloats) {
			uniforms = msg.getArgs(2).getFloatValueList();
		} else {
			uniforms = msg.getArgs(2).getIntValueList();
		}

		IStateTransform setValues = new PropertyChangeTransform(new CurrentProgramPropertyAccessor(msg.getContextId(),
				GLStateType.ACTIVE_UNIFORMS, location, GLStateType.UNIFORM_VALUE), uniforms);

		return Collections.singletonList(setValues);
	}

	private static List<IStateTransform> transformsForGlUniform(GLMessage msg, boolean isFloats) {
		// void glUniform1f(GLint location, GLfloat v0);
		// void glUniform2f(GLint location, GLfloat v0, GLfloat v1);
		// .. 3f
		// .. 4f
		// void glUniform1i(GLint location, GLfloat v0);
		// void glUniform2i(GLint location, GLfloat v0, GLfloat v1);
		// .. 3i
		// .. 4i

		int location = msg.getArgs(0).getIntValue(0);
		if (location < 0) {
			throw new IllegalArgumentException("Argument location cannot be less than 0.");
		}
		List<?> uniforms;
		if (isFloats) {
			List<Float> args = new ArrayList<Float>(msg.getArgsCount() - 1);
			for (int i = 1; i < msg.getArgsCount(); i++) {
				args.add(Float.valueOf(msg.getArgs(1).getFloatValue(0)));
			}
			uniforms = args;
		} else {
			List<Integer> args = new ArrayList<Integer>(msg.getArgsCount() - 1);
			for (int i = 1; i < msg.getArgsCount(); i++) {
				args.add(Integer.valueOf(msg.getArgs(1).getIntValue(0)));
			}
			uniforms = args;
		}

		IStateTransform setValues = new PropertyChangeTransform(new CurrentProgramPropertyAccessor(msg.getContextId(),
				GLStateType.ACTIVE_UNIFORMS, location, GLStateType.UNIFORM_VALUE), uniforms);

		return Collections.singletonList(setValues);
	}

	private static List<IStateTransform> transformsForGlCreateShader(GLMessage msg) {
		// GLuint glCreateShader(GLenum shaderType);
		GLEnum shaderType = GLEnum.valueOf(msg.getArgs(0).getIntValue(0));
		int shader = msg.getReturnValue().getIntValue(0);

		IStateTransform addShader = new SparseArrayElementAddTransform(GLPropertyAccessor.makeAccessor(
				msg.getContextId(), GLStateType.SHADERS), shader);
		IStateTransform setShaderType = new PropertyChangeTransform(GLPropertyAccessor.makeAccessor(msg.getContextId(),
				GLStateType.SHADERS, Integer.valueOf(shader), GLStateType.SHADER_TYPE), shaderType);
		return Arrays.asList(addShader, setShaderType);
	}

	private static List<IStateTransform> transformsForGlDeleteShader(GLMessage msg) {
		// void glDeleteShader(GLuint shader);
		int shader = msg.getArgs(0).getIntValue(0);

		IStateTransform transform = new SparseArrayElementRemoveTransform(GLPropertyAccessor.makeAccessor(
				msg.getContextId(), GLStateType.SHADERS), shader);
		return Collections.singletonList(transform);
	}

	private static List<IStateTransform> transformsForGlShaderSource(GLMessage msg) {
		// void glShaderSource(GLuint shader, GLsizei count, const GLchar
		// **string,
		// const GLint *length);
		// This message is patched up on the device to return a single string as
		// opposed to a
		// list of strings
		int shader = msg.getArgs(0).getIntValue(0);
		String src = msg.getArgs(2).getCharValue(0).toStringUtf8();

		IStateTransform transform = new PropertyChangeTransform(GLPropertyAccessor.makeAccessor(msg.getContextId(),
				GLStateType.SHADERS, Integer.valueOf(shader), GLStateType.SHADER_SOURCE), src);
		return Collections.singletonList(transform);
	}

	private static List<IStateTransform> transformsForEglCreateContext(GLMessage msg) {
		// void eglCreateContext(int version, int context);
		int version = msg.getArgs(0).getIntValue(0);
		IGLProperty glState = null;
		if (version == 0) {
			glState = GLState.createDefaultES1State();
		} else {
			glState = GLState.createDefaultES2State();
		}
		IStateTransform transform = new ListElementAddTransform(null, glState);
		return Collections.singletonList(transform);
	}
}

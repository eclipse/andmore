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

package org.eclipse.andmore.gltrace.state;

import java.util.Collections;

import org.eclipse.andmore.gltrace.GLEnum;
import org.eclipse.andmore.gltrace.state.DisplayRadix;

public class GLState {
	/** # of texture units modeled in the GL State. */
	public static final int TEXTURE_UNIT_COUNT = 32;

	/** # of vertex attributes */
	private static final int MAX_VERTEX_ATTRIBS = 16;

	private static GLState sGLState = new GLState();

	private IGLProperty createBufferBindings() {
		IGLProperty array, eArray, vArray;

		array = new GLIntegerProperty(GLStateType.ARRAY_BUFFER_BINDING, 0);
		eArray = new GLIntegerProperty(GLStateType.ELEMENT_ARRAY_BUFFER_BINDING, 0);

		vArray = new GLIntegerProperty(GLStateType.VERTEX_ATTRIB_ARRAY_BUFFER_BINDING_PER_INDEX, 0);
		IGLProperty vArray8 = new GLListProperty(GLStateType.VERTEX_ATTRIB_ARRAY_BUFFER_BINDINGS, vArray,
				MAX_VERTEX_ATTRIBS);

		return new GLCompositeProperty(GLStateType.BUFFER_BINDINGS, array, eArray, vArray8);
	}

	private IGLProperty createVertexAttribArrays() {
		IGLProperty enabled, size, stride, type, normalized, pointer;

		enabled = new GLBooleanProperty(GLStateType.VERTEX_ATTRIB_ARRAY_ENABLED, false);
		size = new GLIntegerProperty(GLStateType.VERTEX_ATTRIB_ARRAY_SIZE, 4);
		stride = new GLIntegerProperty(GLStateType.VERTEX_ATTRIB_ARRAY_STRIDE, 0);
		type = new GLEnumProperty(GLStateType.VERTEX_ATTRIB_ARRAY_TYPE, GLEnum.GL_FLOAT);
		normalized = new GLBooleanProperty(GLStateType.VERTEX_ATTRIB_ARRAY_NORMALIZED, false);
		pointer = new GLLongProperty(GLStateType.VERTEX_ATTRIB_ARRAY_POINTER, 0L, DisplayRadix.HEX);

		IGLProperty perVertexAttribArrayState = new GLCompositeProperty(GLStateType.VERTEX_ATTRIB_ARRAY_COMPOSITE,
				enabled, size, stride, type, normalized, pointer);

		return new GLListProperty(GLStateType.VERTEX_ATTRIB_ARRAY, perVertexAttribArrayState, MAX_VERTEX_ATTRIBS);
	}

	private IGLProperty createGenericVertexAttributeState() {
		IGLProperty v0 = new GLFloatProperty(GLStateType.GENERIC_VERTEX_ATTRIB_V0, Float.valueOf(0));
		IGLProperty v1 = new GLFloatProperty(GLStateType.GENERIC_VERTEX_ATTRIB_V1, Float.valueOf(0));
		IGLProperty v2 = new GLFloatProperty(GLStateType.GENERIC_VERTEX_ATTRIB_V2, Float.valueOf(0));
		IGLProperty v3 = new GLFloatProperty(GLStateType.GENERIC_VERTEX_ATTRIB_V3, Float.valueOf(0));

		IGLProperty perGenericVertexAttribState = new GLCompositeProperty(
				GLStateType.GENERIC_VERTEX_ATTRIBUTE_DATA_COMPOSITE, v0, v1, v2, v3);

		return new GLListProperty(GLStateType.GENERIC_VERTEX_ATTRIBUTES, perGenericVertexAttribState,
				MAX_VERTEX_ATTRIBS);
	}

	private IGLProperty createVboState() {
		IGLProperty size = new GLIntegerProperty(GLStateType.BUFFER_SIZE, Integer.valueOf(0));
		IGLProperty usage = new GLEnumProperty(GLStateType.BUFFER_USAGE, GLEnum.GL_STATIC_DRAW);
		IGLProperty data = new GLObjectProperty(GLStateType.BUFFER_DATA, new byte[0]);
		IGLProperty type = new GLEnumProperty(GLStateType.BUFFER_TYPE, GLEnum.GL_ARRAY_BUFFER);

		IGLProperty perVboState = new GLCompositeProperty(GLStateType.VBO_COMPOSITE, size, usage, data, type);

		return new GLSparseArrayProperty(GLStateType.VBO, perVboState);
	}

	private IGLProperty createVertexArrayData() {
		IGLProperty vertexAttribArrays = createVertexAttribArrays();
		IGLProperty bufferBindings = createBufferBindings();
		IGLProperty genericAttribs = createGenericVertexAttributeState();
		IGLProperty vboState = createVboState();

		return new GLCompositeProperty(GLStateType.VERTEX_ARRAY_DATA, genericAttribs, vertexAttribArrays,
				bufferBindings, vboState);
	}

	private IGLProperty createTransformationState() {
		IGLProperty viewPortX = new GLIntegerProperty(GLStateType.VIEWPORT_X, 0);
		IGLProperty viewPortY = new GLIntegerProperty(GLStateType.VIEWPORT_Y, 0);
		IGLProperty viewPortW = new GLIntegerProperty(GLStateType.VIEWPORT_WIDTH, 0);
		IGLProperty viewPortH = new GLIntegerProperty(GLStateType.VIEWPORT_HEIGHT, 0);
		IGLProperty viewPort = new GLCompositeProperty(GLStateType.VIEWPORT, viewPortX, viewPortY, viewPortW, viewPortH);

		IGLProperty clampNear = new GLFloatProperty(GLStateType.DEPTH_RANGE_NEAR, Float.valueOf(0.0f));
		IGLProperty clampFar = new GLFloatProperty(GLStateType.DEPTH_RANGE_FAR, Float.valueOf(1.0f));
		IGLProperty depthRange = new GLCompositeProperty(GLStateType.DEPTH_RANGE, clampNear, clampFar);

		IGLProperty transformationState = new GLCompositeProperty(GLStateType.TRANSFORMATION_STATE, viewPort,
				depthRange);
		return transformationState;
	}

	private IGLProperty createRasterizationState() {
		IGLProperty lineWidth = new GLFloatProperty(GLStateType.LINE_WIDTH, Float.valueOf(1.0f));
		IGLProperty cullFace = new GLBooleanProperty(GLStateType.CULL_FACE, Boolean.FALSE);
		IGLProperty cullFaceMode = new GLEnumProperty(GLStateType.CULL_FACE_MODE, GLEnum.GL_BACK);
		IGLProperty frontFace = new GLEnumProperty(GLStateType.FRONT_FACE, GLEnum.GL_CCW);
		IGLProperty polyOffsetFactor = new GLFloatProperty(GLStateType.POLYGON_OFFSET_FACTOR, Float.valueOf(0f));
		IGLProperty polyOffsetUnits = new GLFloatProperty(GLStateType.POLYGON_OFFSET_UNITS, Float.valueOf(0f));
		IGLProperty polyOffsetFill = new GLBooleanProperty(GLStateType.POLYGON_OFFSET_FILL, Boolean.FALSE);

		return new GLCompositeProperty(GLStateType.RASTERIZATION_STATE, lineWidth, cullFace, cullFaceMode, frontFace,
				polyOffsetFactor, polyOffsetUnits, polyOffsetFill);
	}

	private IGLProperty createPixelOperationsState() {
		IGLProperty scissorTest = new GLBooleanProperty(GLStateType.SCISSOR_TEST, Boolean.FALSE);
		IGLProperty scissorBoxX = new GLIntegerProperty(GLStateType.SCISSOR_BOX_X, 0);
		IGLProperty scissorBoxY = new GLIntegerProperty(GLStateType.SCISSOR_BOX_Y, 0);
		IGLProperty scissorBoxW = new GLIntegerProperty(GLStateType.SCISSOR_BOX_WIDTH, 0);
		IGLProperty scissorBoxH = new GLIntegerProperty(GLStateType.SCISSOR_BOX_HEIGHT, 0);
		IGLProperty scissorBox = new GLCompositeProperty(GLStateType.SCISSOR_BOX, scissorBoxX, scissorBoxY,
				scissorBoxW, scissorBoxH);

		IGLProperty stencilTest = new GLBooleanProperty(GLStateType.STENCIL_TEST, Boolean.FALSE);
		IGLProperty stencilFunc = new GLEnumProperty(GLStateType.STENCIL_FUNC, GLEnum.GL_ALWAYS);
		IGLProperty stencilMask = new GLIntegerProperty(GLStateType.STENCIL_VALUE_MASK, Integer.valueOf(0xffffffff),
				DisplayRadix.HEX);
		IGLProperty stencilRef = new GLIntegerProperty(GLStateType.STENCIL_REF, Integer.valueOf(0));
		IGLProperty stencilFail = new GLEnumProperty(GLStateType.STENCIL_FAIL, GLEnum.GL_KEEP);
		IGLProperty stencilPassDepthFail = new GLEnumProperty(GLStateType.STENCIL_PASS_DEPTH_FAIL, GLEnum.GL_KEEP);
		IGLProperty stencilPassDepthPass = new GLEnumProperty(GLStateType.STENCIL_PASS_DEPTH_PASS, GLEnum.GL_KEEP);
		IGLProperty stencilBackFunc = new GLEnumProperty(GLStateType.STENCIL_BACK_FUNC, GLEnum.GL_ALWAYS);
		IGLProperty stencilBackValueMask = new GLIntegerProperty(GLStateType.STENCIL_BACK_VALUE_MASK,
				Integer.valueOf(0xffffffff), DisplayRadix.HEX);
		IGLProperty stencilBackRef = new GLIntegerProperty(GLStateType.STENCIL_BACK_REF, 0);
		IGLProperty stencilBackFail = new GLEnumProperty(GLStateType.STENCIL_BACK_FAIL, GLEnum.GL_KEEP);
		IGLProperty stencilBackPassDepthFail = new GLEnumProperty(GLStateType.STENCIL_BACK_PASS_DEPTH_FAIL,
				GLEnum.GL_KEEP);
		IGLProperty stencilBackPassDepthPass = new GLEnumProperty(GLStateType.STENCIL_BACK_PASS_DEPTH_PASS,
				GLEnum.GL_KEEP);
		IGLProperty stencil = new GLCompositeProperty(GLStateType.STENCIL, stencilTest, stencilFunc, stencilMask,
				stencilRef, stencilFail, stencilPassDepthFail, stencilPassDepthPass, stencilBackFunc,
				stencilBackValueMask, stencilBackRef, stencilBackFail, stencilBackPassDepthFail,
				stencilBackPassDepthPass);

		IGLProperty depthTest = new GLBooleanProperty(GLStateType.DEPTH_TEST, Boolean.FALSE);
		IGLProperty depthFunc = new GLEnumProperty(GLStateType.DEPTH_FUNC, GLEnum.GL_LESS);

		IGLProperty blendEnabled = new GLBooleanProperty(GLStateType.BLEND_ENABLED, Boolean.FALSE);
		// FIXME: BLEND_SRC_RGB should be set to GL_ONE, but GL_LINES is already
		// 0x1.
		IGLProperty blendSrcRgb = new GLEnumProperty(GLStateType.BLEND_SRC_RGB, GLEnum.GL_LINES);
		IGLProperty blendSrcAlpha = new GLEnumProperty(GLStateType.BLEND_SRC_ALPHA, GLEnum.GL_LINES);
		IGLProperty blendDstRgb = new GLEnumProperty(GLStateType.BLEND_DST_RGB, GLEnum.GL_NONE);
		IGLProperty blendDstAlpha = new GLEnumProperty(GLStateType.BLEND_DST_ALPHA, GLEnum.GL_NONE);
		IGLProperty blendEquationRgb = new GLEnumProperty(GLStateType.BLEND_EQUATION_RGB, GLEnum.GL_FUNC_ADD);
		IGLProperty blendEquationAlpha = new GLEnumProperty(GLStateType.BLEND_EQUATION_ALPHA, GLEnum.GL_FUNC_ADD);
		IGLProperty blend = new GLCompositeProperty(GLStateType.BLEND, blendEnabled, blendSrcRgb, blendSrcAlpha,
				blendDstRgb, blendDstAlpha, blendEquationRgb, blendEquationAlpha);

		IGLProperty dither = new GLBooleanProperty(GLStateType.DITHER, Boolean.TRUE);

		return new GLCompositeProperty(GLStateType.PIXEL_OPERATIONS, scissorTest, scissorBox, stencil, depthTest,
				depthFunc, blend, dither);
	}

	private IGLProperty createPixelPackState() {
		IGLProperty packAlignment = new GLIntegerProperty(GLStateType.PACK_ALIGNMENT, Integer.valueOf(4));
		IGLProperty unpackAlignment = new GLIntegerProperty(GLStateType.UNPACK_ALIGNMENT, Integer.valueOf(4));
		IGLProperty pixelPack = new GLCompositeProperty(GLStateType.PIXEL_PACKING, packAlignment, unpackAlignment);
		return pixelPack;
	}

	private IGLProperty createFramebufferState() {
		IGLProperty binding = new GLIntegerProperty(GLStateType.FRAMEBUFFER_BINDING, 0);
		GLCompositeProperty framebufferState = new GLCompositeProperty(GLStateType.FRAMEBUFFER_STATE, binding);
		return framebufferState;
	}

	private IGLProperty createTextureState() {
		IGLProperty activeTexture = new GLIntegerProperty(GLStateType.ACTIVE_TEXTURE_UNIT, Integer.valueOf(0));

		IGLProperty binding2D = new GLIntegerProperty(GLStateType.TEXTURE_BINDING_2D, Integer.valueOf(0));
		IGLProperty bindingCubeMap = new GLIntegerProperty(GLStateType.TEXTURE_BINDING_CUBE_MAP, Integer.valueOf(0));
		IGLProperty bindingExternal = new GLIntegerProperty(GLStateType.TEXTURE_BINDING_EXTERNAL, Integer.valueOf(0));
		IGLProperty perTextureUnitState = new GLCompositeProperty(GLStateType.PER_TEXTURE_UNIT_STATE, binding2D,
				bindingCubeMap, bindingExternal);
		IGLProperty textureUnitState = new GLListProperty(GLStateType.TEXTURE_UNITS, perTextureUnitState,
				TEXTURE_UNIT_COUNT);

		IGLProperty swizzleR = new GLEnumProperty(GLStateType.TEXTURE_SWIZZLE_R, GLEnum.GL_RED);
		IGLProperty swizzleG = new GLEnumProperty(GLStateType.TEXTURE_SWIZZLE_G, GLEnum.GL_GREEN);
		IGLProperty swizzleB = new GLEnumProperty(GLStateType.TEXTURE_SWIZZLE_B, GLEnum.GL_BLUE);
		IGLProperty swizzleA = new GLEnumProperty(GLStateType.TEXTURE_SWIZZLE_A, GLEnum.GL_ALPHA);
		IGLProperty minFilter = new GLEnumProperty(GLStateType.TEXTURE_MIN_FILTER, GLEnum.GL_NEAREST);
		IGLProperty magFilter = new GLEnumProperty(GLStateType.TEXTURE_MAG_FILTER, GLEnum.GL_NEAREST);
		IGLProperty wrapS = new GLEnumProperty(GLStateType.TEXTURE_WRAP_S, GLEnum.GL_REPEAT);
		IGLProperty wrapT = new GLEnumProperty(GLStateType.TEXTURE_WRAP_T, GLEnum.GL_REPEAT);
		IGLProperty wrapR = new GLEnumProperty(GLStateType.TEXTURE_WRAP_R, GLEnum.GL_REPEAT);
		IGLProperty minLod = new GLFloatProperty(GLStateType.TEXTURE_MIN_LOD, Float.valueOf(-1000));
		IGLProperty maxLod = new GLFloatProperty(GLStateType.TEXTURE_MAX_LOD, Float.valueOf(1000));
		IGLProperty baseLevel = new GLIntegerProperty(GLStateType.TEXTURE_BASE_LEVEL, 0);
		IGLProperty maxLevel = new GLIntegerProperty(GLStateType.TEXTURE_MAX_LEVEL, 1000);
		IGLProperty cmpMode = new GLEnumProperty(GLStateType.TEXTURE_COMPARE_MODE, GLEnum.GL_NONE);
		IGLProperty cmpFunc = new GLEnumProperty(GLStateType.TEXTURE_COMPARE_FUNC, GLEnum.GL_LEQUAL);
		IGLProperty immutableFormat = new GLBooleanProperty(GLStateType.TEXTURE_IMMUTABLE_FORMAT, Boolean.FALSE);
		IGLProperty immutableLevels = new GLIntegerProperty(GLStateType.TEXTURE_IMMUTABLE_LEVELS, 0);

		IGLProperty width = new GLIntegerProperty(GLStateType.TEXTURE_WIDTH, Integer.valueOf(-1));
		IGLProperty height = new GLIntegerProperty(GLStateType.TEXTURE_HEIGHT, Integer.valueOf(-1));
		IGLProperty format = new GLEnumProperty(GLStateType.TEXTURE_FORMAT, GLEnum.GL_INVALID_VALUE);
		IGLProperty imageType = new GLEnumProperty(GLStateType.TEXTURE_IMAGE_TYPE, GLEnum.GL_UNSIGNED_BYTE);
		IGLProperty image = new GLStringProperty(GLStateType.TEXTURE_IMAGE, null);

		IGLProperty perTextureLevelState = new GLCompositeProperty(GLStateType.PER_TEXTURE_LEVEL_STATE, width, height,
				format, imageType, image);
		IGLProperty mipmapState = new GLSparseArrayProperty(GLStateType.TEXTURE_MIPMAPS, perTextureLevelState, true);

		IGLProperty textureDefaultState = new GLCompositeProperty(GLStateType.PER_TEXTURE_STATE, swizzleR, swizzleG,
				swizzleB, swizzleA, minFilter, magFilter, wrapS, wrapT, wrapR, minLod, maxLod, baseLevel, maxLevel,
				cmpMode, cmpFunc, immutableFormat, immutableLevels, mipmapState);
		GLSparseArrayProperty textures = new GLSparseArrayProperty(GLStateType.TEXTURES, textureDefaultState);
		textures.add(0);

		return new GLCompositeProperty(GLStateType.TEXTURE_STATE, activeTexture, textureUnitState, textures);
	}

	private IGLProperty createProgramState() {
		IGLProperty currentProgram = new GLIntegerProperty(GLStateType.CURRENT_PROGRAM, Integer.valueOf(0));

		IGLProperty attachedShaderId = new GLIntegerProperty(GLStateType.ATTACHED_SHADER_ID, Integer.valueOf(0));
		IGLProperty attachedShaders = new GLSparseArrayProperty(GLStateType.ATTACHED_SHADERS, attachedShaderId);

		IGLProperty attributeName = new GLStringProperty(GLStateType.ATTRIBUTE_NAME, "");
		IGLProperty attributeType = new GLEnumProperty(GLStateType.ATTRIBUTE_TYPE, GLEnum.GL_FLOAT_MAT4);
		IGLProperty attributeSize = new GLIntegerProperty(GLStateType.ATTRIBUTE_SIZE, Integer.valueOf(1));
		IGLProperty attributeValue = new GLObjectProperty(GLStateType.ATTRIBUTE_VALUE, Collections.emptyList());
		IGLProperty perAttributeProperty = new GLCompositeProperty(GLStateType.PER_ATTRIBUTE_STATE, attributeName,
				attributeType, attributeSize, attributeValue);
		IGLProperty attributes = new GLSparseArrayProperty(GLStateType.ACTIVE_ATTRIBUTES, perAttributeProperty);

		IGLProperty uniformName = new GLStringProperty(GLStateType.UNIFORM_NAME, "");
		IGLProperty uniformType = new GLEnumProperty(GLStateType.UNIFORM_TYPE, GLEnum.GL_FLOAT_MAT4);
		IGLProperty uniformSize = new GLIntegerProperty(GLStateType.UNIFORM_SIZE, Integer.valueOf(1));
		IGLProperty uniformValue = new GLObjectProperty(GLStateType.UNIFORM_VALUE, Collections.emptyList());
		IGLProperty perUniformProperty = new GLCompositeProperty(GLStateType.PER_UNIFORM_STATE, uniformName,
				uniformType, uniformSize, uniformValue);
		IGLProperty uniforms = new GLSparseArrayProperty(GLStateType.ACTIVE_UNIFORMS, perUniformProperty);

		IGLProperty perProgramState = new GLCompositeProperty(GLStateType.PER_PROGRAM_STATE, attachedShaders,
				attributes, uniforms);

		IGLProperty programs = new GLSparseArrayProperty(GLStateType.PROGRAMS, perProgramState);

		return new GLCompositeProperty(GLStateType.PROGRAM_STATE, currentProgram, programs);
	}

	private IGLProperty createShaderState() {
		IGLProperty shaderType = new GLEnumProperty(GLStateType.SHADER_TYPE, GLEnum.GL_VERTEX_SHADER);
		IGLProperty shaderSource = new GLStringProperty(GLStateType.SHADER_SOURCE, ""); //$NON-NLS-1$
		IGLProperty perShaderState = new GLCompositeProperty(GLStateType.PER_SHADER_STATE, shaderType, shaderSource);
		return new GLSparseArrayProperty(GLStateType.SHADERS, perShaderState);
	}

	public static IGLProperty createDefaultES2State() {
		GLCompositeProperty glState = new GLCompositeProperty(GLStateType.GL_STATE_ES2,
				sGLState.createVertexArrayData(), sGLState.createFramebufferState(),
				sGLState.createTransformationState(), sGLState.createRasterizationState(),
				sGLState.createPixelOperationsState(), sGLState.createPixelPackState(), sGLState.createTextureState(),
				sGLState.createProgramState(), sGLState.createShaderState());
		return glState;
	}

	public static IGLProperty createDefaultES1State() {
		GLCompositeProperty glState = new GLCompositeProperty(GLStateType.GL_STATE_ES1,
				sGLState.createVertexArrayData(), sGLState.createFramebufferState(),
				sGLState.createTransformationState(), sGLState.createRasterizationState(),
				sGLState.createPixelOperationsState(), sGLState.createPixelPackState(), sGLState.createTextureState());
		return glState;
	}

	public static IGLProperty createDefaultState() {
		return new GLListProperty(GLStateType.GL_STATE, null, 0);
	}
}

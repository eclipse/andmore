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

/** The type for each OpenGL State Property {@link IGLProperty}. */
public enum GLStateType {
	// Note: the indentation reflects the state hierarchy.

	GL_STATE("OpenGL State Variables"), GL_STATE_ES1("OpenGL ES 1.1 State"), GL_STATE_ES2("OpenGL ES 2.0 State"),

	VERTEX_ARRAY_DATA("Vertex Array Data"), GENERIC_VERTEX_ATTRIBUTES("Generic Vertex Attributes"), GENERIC_VERTEX_ATTRIBUTE_DATA_COMPOSITE(
			"Generic Vertex Attribute Data"), GENERIC_VERTEX_ATTRIB_V0("x"), GENERIC_VERTEX_ATTRIB_V1("y"), GENERIC_VERTEX_ATTRIB_V2(
			"z"), GENERIC_VERTEX_ATTRIB_V3("w"),

	VERTEX_ATTRIB_ARRAY("Vertex Attrib Array Properties"), VERTEX_ATTRIB_ARRAY_COMPOSITE(
			"Vertex Attrib Array #n Properties"), VERTEX_ATTRIB_ARRAY_ENABLED("Vertex Attrib Array Enable"), VERTEX_ATTRIB_ARRAY_SIZE(
			"Vertex Attrib Array Size"), VERTEX_ATTRIB_ARRAY_STRIDE("Vertex Attrib Array Stride"), VERTEX_ATTRIB_ARRAY_TYPE(
			"Vertex Attrib Array Type"), VERTEX_ATTRIB_ARRAY_NORMALIZED("Vertex Attrib Array Normalized"), VERTEX_ATTRIB_ARRAY_POINTER(
			"Vertex Attrib Array Pointer"),

	BUFFER_BINDINGS("Buffer Bindings"), ARRAY_BUFFER_BINDING("Current Buffer Binding"), ELEMENT_ARRAY_BUFFER_BINDING(
			"Element Array Buffer Binding"), VERTEX_ATTRIB_ARRAY_BUFFER_BINDINGS("Attribute Array Buffer Bindings"), VERTEX_ATTRIB_ARRAY_BUFFER_BINDING_PER_INDEX(
			"Attribute Array Buffer Binding"),

	VBO("Vertex Buffer Objects"), VBO_COMPOSITE("Per VBO State"), BUFFER_SIZE("Size"), BUFFER_USAGE("Usage"), BUFFER_DATA(
			"Data"), BUFFER_TYPE("Type"),

	TRANSFORMATION_STATE("Transformation State"), VIEWPORT("Viewport"), VIEWPORT_X("Lower Left X"), VIEWPORT_Y(
			"Lower Left Y"), VIEWPORT_WIDTH("Width"), VIEWPORT_HEIGHT("Height"), DEPTH_RANGE("Depth Range"), DEPTH_RANGE_NEAR(
			"Near Clipping Plane"), DEPTH_RANGE_FAR("Far Clipping Plane"),

	RASTERIZATION_STATE("Rasterization State"), LINE_WIDTH("Line Width"), CULL_FACE("Polygon Culling Enabled"), CULL_FACE_MODE(
			"Cull front/back facing polygons"), FRONT_FACE("Polygon frontface CW/CCW indicator"), POLYGON_OFFSET_FACTOR(
			"Polygon Offset Factor"), POLYGON_OFFSET_UNITS("Polygon Offset Units"), POLYGON_OFFSET_FILL(
			"Polygon Offset Enable"),

	PIXEL_OPERATIONS("Pixel Operations"), SCISSOR_TEST("Scissoring enabled"), SCISSOR_BOX("Scissor Box"), SCISSOR_BOX_X(
			"Lower Left X"), SCISSOR_BOX_Y("Lower Left Y"), SCISSOR_BOX_WIDTH("Width"), SCISSOR_BOX_HEIGHT("Height"), STENCIL(
			"Stencil"), STENCIL_TEST("Stenciling enabled"), STENCIL_FUNC("Front Stencil Function"), STENCIL_VALUE_MASK(
			"Front Stencil Mask"), STENCIL_REF("Front Stencil Reference Value"), STENCIL_FAIL(
			"Front Stencil Fail Action"), STENCIL_PASS_DEPTH_FAIL("Front stencil depth buffer fail action"), STENCIL_PASS_DEPTH_PASS(
			"Front stencil depth buffer pass action"), STENCIL_BACK_FUNC("Back stencil function"), STENCIL_BACK_VALUE_MASK(
			"Back stencil mask"), STENCIL_BACK_REF("Back stencil reference value"), STENCIL_BACK_FAIL(
			"Back stencil fail action"), STENCIL_BACK_PASS_DEPTH_FAIL("Back stencil depth buffer fail action"), STENCIL_BACK_PASS_DEPTH_PASS(
			"Back stencil depth buffer pass action"), DEPTH_TEST("Depth buffer enabled"), DEPTH_FUNC(
			"Depth buffer test function"), BLEND("Blending"), BLEND_ENABLED("Enabled"), BLEND_SRC_RGB(
			"Source RGB function"), BLEND_SRC_ALPHA("Source A function"), BLEND_DST_RGB("Dest. RGB function"), BLEND_DST_ALPHA(
			"Dest. A function"), BLEND_EQUATION_RGB("RGB Equation"), BLEND_EQUATION_ALPHA("Alpha Equation"), DITHER(
			"Dithering enabled"),

	PIXEL_PACKING("Pixel Packing"), PACK_ALIGNMENT("Pack Alignment"), UNPACK_ALIGNMENT("Unpack Alignment"),

	TEXTURE_STATE("Texture State"), ACTIVE_TEXTURE_UNIT("Active Texture Unit"), TEXTURE_UNITS("Texture Units"), PER_TEXTURE_UNIT_STATE(
			"Texture Unit Properties"), TEXTURE_BINDING_2D("TEXTURE_2D Binding"), TEXTURE_BINDING_CUBE_MAP(
			"TEXTURE_CUBE_MAP Binding"), TEXTURE_BINDING_EXTERNAL("TEXTURE_EXTERNAL Binding"), TEXTURES("Textures"), PER_TEXTURE_STATE(
			"Per Texture State"), TEXTURE_SWIZZLE_R("Red Component Swizzle"), TEXTURE_SWIZZLE_G(
			"Green Component Swizzle"), TEXTURE_SWIZZLE_B("Blue Component Swizzle"), TEXTURE_SWIZZLE_A(
			"Alpha Component Swizzle"), TEXTURE_MIN_FILTER("Minification Function"), TEXTURE_MAG_FILTER(
			"Magnification Function"), TEXTURE_WRAP_S("Texcoord s Wrap Mode"), TEXTURE_WRAP_T("Texcoord t Wrap Mode"), TEXTURE_WRAP_R(
			"Texcoord r Wrap Mode"), TEXTURE_MIN_LOD("Min Level of Detail"), TEXTURE_MAX_LOD("Max Level of Detail"), TEXTURE_BASE_LEVEL(
			"Base Texture Array"), TEXTURE_MAX_LEVEL("Max Texture Array Level"), TEXTURE_COMPARE_MODE("Comparison Mode"), TEXTURE_COMPARE_FUNC(
			"Comparison Function"), TEXTURE_IMMUTABLE_FORMAT("Size and format immutable?"), TEXTURE_IMMUTABLE_LEVELS(
			"# of levels in immutable textures"), TEXTURE_MIPMAPS("Texture Mipmap State"), PER_TEXTURE_LEVEL_STATE(
			"Per Texture Level State"), TEXTURE_FORMAT("Format"), TEXTURE_WIDTH("Width"), TEXTURE_HEIGHT("Height"), TEXTURE_IMAGE_TYPE(
			"Image Type"), TEXTURE_IMAGE("Image"),

	PROGRAM_STATE("Program Object State"), CURRENT_PROGRAM("Current Program"), PROGRAMS("Programs"), PER_PROGRAM_STATE(
			"Per Program State"), ATTACHED_SHADERS("Attached Shaders"), ATTACHED_SHADER_ID("Attached Shader ID"), ACTIVE_ATTRIBUTES(
			"Attributes"), PER_ATTRIBUTE_STATE("Per Attribute State"), ATTRIBUTE_NAME("Name"), ATTRIBUTE_TYPE("Type"), ATTRIBUTE_SIZE(
			"Size"), ATTRIBUTE_VALUE("Value"), ACTIVE_UNIFORMS("Uniforms"), PER_UNIFORM_STATE("Per Uniform State"), UNIFORM_NAME(
			"Name"), UNIFORM_TYPE("Type"), UNIFORM_SIZE("Size"), UNIFORM_VALUE("Value"),

	SHADERS("Shader Objects"), PER_SHADER_STATE("Per Shader State"), SHADER_TYPE("Shader Type"), SHADER_SOURCE("Source"),

	FRAMEBUFFER_STATE("Framebuffer State"), FRAMEBUFFER_BINDING("Framebuffer Binding"), FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE(
			"Framebuffer object type"), FRAMEBUFFER_ATTACHMENT_OBJECT_NAME("Framebuffer object name"), FRAMEBUFFER_ATTACHMENT_TEXTURE_LEVEL(
			"Framebuffer texture level"), FRAMEBUFFER_ATTACHEMENT_TEXTURE_CUBE_MAP_FACE(
			"Framebuffer texture cubemap face");

	private final String mDescription;

	GLStateType(String description) {
		mDescription = description;
	}

	public String getDescription() {
		return mDescription;
	}
}

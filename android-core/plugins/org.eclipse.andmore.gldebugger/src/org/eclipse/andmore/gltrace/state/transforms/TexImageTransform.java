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
import com.google.common.primitives.UnsignedBytes;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import javax.imageio.ImageIO;

import org.eclipse.andmore.gltrace.FileUtils;
import org.eclipse.andmore.gltrace.GLEnum;
import org.eclipse.andmore.gltrace.state.GLStringProperty;
import org.eclipse.andmore.gltrace.state.IGLProperty;

/**
 * {@link TexImageTransform} transforms the state to reflect the effect of a
 * glTexImage2D or glTexSubImage2D GL call.
 */
public class TexImageTransform implements IStateTransform {
	private static final String PNG_IMAGE_FORMAT = "PNG";
	private static final String TEXTURE_FILE_PREFIX = "tex";
	private static final String TEXTURE_FILE_SUFFIX = ".png";

	private final IGLPropertyAccessor mAccessor;
	private final File mTextureDataFile;

	private final int mxOffset;
	private final int myOffset;
	private final int mWidth;
	private final int mHeight;

	private String mOldValue;
	private String mNewValue;
	private GLEnum mFormat;
	private GLEnum mType;

	/**
	 * Construct a texture image transformation.
	 * 
	 * @param accessor
	 *            accessor to obtain the GL state variable to modify
	 * @param textureData
	 *            texture data passed in by the call. Could be null.
	 * @param format
	 *            format of the source texture data
	 * @param xOffset
	 *            x offset for the source data (used only in glTexSubImage2D)
	 * @param yOffset
	 *            y offset for the source data (used only in glTexSubImage2D)
	 * @param width
	 *            width of the texture
	 * @param height
	 *            height of the texture
	 */
	public TexImageTransform(IGLPropertyAccessor accessor, File textureData, GLEnum format, GLEnum type, int xOffset,
			int yOffset, int width, int height) {
		mAccessor = accessor;
		mTextureDataFile = textureData;
		mFormat = format;
		mType = type;

		mxOffset = xOffset;
		myOffset = yOffset;
		mWidth = width;
		mHeight = height;
	}

	@Override
	public void apply(IGLProperty currentState) {
		assert mOldValue == null : "Transform cannot be applied multiple times"; //$NON-NLS-1$

		IGLProperty property = mAccessor.getProperty(currentState);
		if (!(property instanceof GLStringProperty)) {
			return;
		}

		GLStringProperty prop = (GLStringProperty) property;
		mOldValue = prop.getStringValue();

		// Applying texture transformations is a heavy weight process. So we
		// perform
		// it only once and save the result in a temporary file. The property is
		// actually
		// the path to the file.
		if (mNewValue == null) {
			try {
				if (mOldValue == null) {
					mNewValue = createTexture(mTextureDataFile, mWidth, mHeight);
				} else {
					mNewValue = updateTextureData(mOldValue, mTextureDataFile, mxOffset, myOffset, mWidth, mHeight);
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			} catch (RuntimeException e) {
				throw e;
			}
		}

		prop.setValue(mNewValue);
	}

	@Override
	public void revert(IGLProperty state) {
		if (mOldValue != null) {
			IGLProperty property = mAccessor.getProperty(state);
			property.setValue(mOldValue);
			mOldValue = null;
		}
	}

	@Override
	public IGLProperty getChangedProperty(IGLProperty state) {
		return mAccessor.getProperty(state);
	}

	/**
	 * Creates a texture of provided width and height. If the texture data file
	 * is provided, then the texture is initialized with the contents of that
	 * file, otherwise an empty image is created.
	 * 
	 * @param textureDataFile
	 *            path to texture data, could be null.
	 * @param width
	 *            width of texture
	 * @param height
	 *            height of texture
	 * @return path to cached texture
	 */
	private String createTexture(File textureDataFile, int width, int height) throws IOException {
		File f = FileUtils.createTempFile(TEXTURE_FILE_PREFIX, TEXTURE_FILE_SUFFIX);

		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);

		if (textureDataFile != null) {
			byte[] initialData = Files.toByteArray(textureDataFile);
			img.getRaster().setDataElements(0, 0, width, height, formatSourceData(initialData, width, height));
		}

		ImageIO.write(img, PNG_IMAGE_FORMAT, f);

		return f.getAbsolutePath();
	}

	/**
	 * Update part of an existing texture.
	 * 
	 * @param currentImagePath
	 *            current texture image.
	 * @param textureDataFile
	 *            new data to update the current texture with
	 * @param xOffset
	 *            x offset for the update region
	 * @param yOffset
	 *            y offset for the update region
	 * @param width
	 *            width of the update region
	 * @param height
	 *            height of the update region
	 * @return path to the updated texture
	 */
	private String updateTextureData(String currentImagePath, File textureDataFile, int xOffset, int yOffset,
			int width, int height) throws IOException {
		assert currentImagePath != null : "Attempt to update a null texture";

		if (textureDataFile == null) {
			// Do not perform any updates if we don't have the actual data.
			return currentImagePath;
		}

		File f = FileUtils.createTempFile(TEXTURE_FILE_PREFIX, TEXTURE_FILE_SUFFIX);
		BufferedImage image = null;
		image = ImageIO.read(new File(currentImagePath));

		byte[] subImageData = Files.toByteArray(textureDataFile);
		image.getRaster().setDataElements(xOffset, yOffset, width, height,
				formatSourceData(subImageData, width, height));
		ImageIO.write(image, PNG_IMAGE_FORMAT, f);

		return f.getAbsolutePath();
	}

	private byte[] formatSourceData(byte[] subImageData, int width, int height) {
		if (mType != GLEnum.GL_UNSIGNED_BYTE) {
			subImageData = unpackData(subImageData, mType);
		}

		switch (mFormat) {
		case GL_RGBA:
			// no conversions necessary
			return subImageData;
		case GL_RGB:
			return addAlphaChannel(subImageData, width, height);
		case GL_RED:
		case GL_GREEN:
		case GL_BLUE:
			// GL_RED, GL_GREEN and GL_BLUE are all supposed to fill those
			// respective
			// channels, but we assume that the programmers intent was to use
			// GL_ALPHA in order
			// to overcome the issue that GL_ALPHA cannot be used with float
			// data.
			if (mType != GLEnum.GL_FLOAT) {
				throw new RuntimeException();
			} else {
				// fall through - assume that it is GL_ALPHA
			}
			//$FALL-THROUGH$
		case GL_ALPHA:
			return addRGBChannels(subImageData, width, height);
		case GL_LUMINANCE:
			return createRGBAFromLuminance(subImageData, width, height);
		case GL_LUMINANCE_ALPHA:
			return createRGBAFromLuminanceAlpha(subImageData, width, height);
		default:
			throw new RuntimeException();
		}
	}

	private byte[] unpackData(byte[] data, GLEnum type) {
		switch (type) {
		case GL_UNSIGNED_BYTE:
			return data;
		case GL_UNSIGNED_SHORT_4_4_4_4:
			return convertShortToUnsigned(data, 0xf000, 12, 0x0f00, 8, 0x00f0, 4, 0x000f, 0, true);
		case GL_UNSIGNED_SHORT_5_6_5:
			return convertShortToUnsigned(data, 0xf800, 11, 0x07e0, 5, 0x001f, 0, 0, 0, false);
		case GL_UNSIGNED_SHORT_5_5_5_1:
			return convertShortToUnsigned(data, 0xf800, 11, 0x07c0, 6, 0x003e, 1, 0x1, 0, true);
		case GL_FLOAT:
			return convertFloatToUnsigned(data);
		default:
			return data;
		}
	}

	private byte[] convertFloatToUnsigned(byte[] data) {
		byte[] unsignedData = new byte[data.length];
		ByteBuffer floatBuffer = ByteBuffer.wrap(data);
		for (int i = 0; i < data.length / 4; i++) {
			float v = floatBuffer.getFloat(i);
			byte alpha = (byte) (v * 255);
			unsignedData[i * 4 + 3] = alpha;
		}
		return unsignedData;
	}

	private byte[] convertShortToUnsigned(byte[] shortData, int rmask, int rshift, int gmask, int gshift, int bmask,
			int bshift, int amask, int ashift, boolean includeAlpha) {
		int numChannels = includeAlpha ? 4 : 3;
		byte[] unsignedData = new byte[(shortData.length / 2) * numChannels];

		for (int i = 0; i < (shortData.length / 2); i++) {
			int hi = UnsignedBytes.toInt(shortData[i * 2 + 0]);
			int lo = UnsignedBytes.toInt(shortData[i * 2 + 1]);

			int x = hi << 8 | lo;

			int r = (x & rmask) >>> rshift;
			int g = (x & gmask) >>> gshift;
			int b = (x & bmask) >>> bshift;
			int a = (x & amask) >>> ashift;

			unsignedData[i * numChannels + 0] = UnsignedBytes.checkedCast(r);
			unsignedData[i * numChannels + 1] = UnsignedBytes.checkedCast(g);
			unsignedData[i * numChannels + 2] = UnsignedBytes.checkedCast(b);

			if (includeAlpha) {
				unsignedData[i * numChannels + 3] = UnsignedBytes.checkedCast(a);
			}
		}

		return unsignedData;
	}

	private byte[] addAlphaChannel(byte[] sourceData, int width, int height) {
		assert sourceData.length == 3 * width * height; // should have R, G & B
														// channels

		byte[] data = new byte[4 * width * height];

		for (int src = 0, dst = 0; src < sourceData.length; src += 3, dst += 4) {
			data[dst + 0] = sourceData[src + 0]; // copy R byte
			data[dst + 1] = sourceData[src + 1]; // copy G byte
			data[dst + 2] = sourceData[src + 2]; // copy B byte
			data[dst + 3] = 1; // add alpha = 1
		}

		return data;
	}

	private byte[] addRGBChannels(byte[] sourceData, int width, int height) {
		assert sourceData.length == width * height; // should have a single
													// alpha channel

		byte[] data = new byte[4 * width * height];

		for (int src = 0, dst = 0; src < sourceData.length; src++, dst += 4) {
			data[dst + 0] = data[dst + 1] = data[dst + 2] = 0; // set R = G = B
																// = 0
			data[dst + 3] = sourceData[src]; // copy over alpha
		}

		return data;
	}

	private byte[] createRGBAFromLuminance(byte[] sourceData, int width, int height) {
		assert sourceData.length == width * height; // should have a single
													// luminance channel

		byte[] data = new byte[4 * width * height];

		for (int src = 0, dst = 0; src < sourceData.length; src++, dst += 4) {
			int l = sourceData[src] * 3;
			if (l > 255) { // clamp to 255
				l = 255;
			}

			data[dst + 0] = data[dst + 1] = data[dst + 2] = (byte) l; // set R =
																		// G = B
																		// = L *
																		// 3
			data[dst + 3] = 1; // set alpha = 1
		}

		return data;
	}

	private byte[] createRGBAFromLuminanceAlpha(byte[] sourceData, int width, int height) {
		assert sourceData.length == 2 * width * height; // should have luminance
														// & alpha channels

		byte[] data = new byte[4 * width * height];

		for (int src = 0, dst = 0; src < sourceData.length; src += 2, dst += 4) {
			int l = sourceData[src] * 3;
			if (l > 255) { // clamp to 255
				l = 255;
			}

			data[dst + 0] = data[dst + 1] = data[dst + 2] = (byte) l; // set R =
																		// G = B
																		// = L *
																		// 3
			data[dst + 3] = sourceData[src + 1]; // copy over alpha
		}

		return data;
	}
}

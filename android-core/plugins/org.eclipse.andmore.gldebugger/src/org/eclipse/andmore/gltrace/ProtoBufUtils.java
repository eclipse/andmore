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

package org.eclipse.andmore.gltrace;

import org.eclipse.andmore.gltrace.GLProtoBuf.GLMessage;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.widgets.Display;
import org.liblzf.CLZF;

/** Utilities to deal with protobuf encoded {@link GLMessage}. */
public class ProtoBufUtils {
	private static ImageData getImageData(GLMessage glMsg) {
		int width = glMsg.getFb().getWidth();
		int height = glMsg.getFb().getHeight();

		if (width * height == 0) {
			return null;
		}

		byte[] compressed = glMsg.getFb().getContents(0).toByteArray();
		byte[] uncompressed = new byte[width * height * 4];

		int size = CLZF.lzf_decompress(compressed, compressed.length, uncompressed, uncompressed.length);
		assert size == width * height * 4 : "Unexpected image size after decompression.";

		int redMask = 0xff000000;
		int greenMask = 0x00ff0000;
		int blueMask = 0x0000ff00;
		PaletteData palette = new PaletteData(redMask, greenMask, blueMask);
		ImageData imageData = new ImageData(width, height, 32, // depth
				palette, 1, // scan line padding
				uncompressed);
		byte[] alpha = new byte[width * height];
		for (int i = 0; i < width * height; i++) {
			alpha[i] = uncompressed[i * 4 + 3];
		}
		imageData.alphaData = alpha;

		imageData = imageData.scaledTo(imageData.width, -imageData.height);
		return imageData;
	}

	/** Obtains the image stored in provided protocol buffer message. */
	public static Image getImage(Display display, GLMessage glMsg) {
		if (!glMsg.hasFb()) {
			return null;
		}

		ImageData imageData = null;
		try {
			imageData = getImageData(glMsg);
		} catch (Exception e) {
			GlTracePlugin.getDefault().logMessage("Unexpected error while retrieving framebuffer image: " + e);
			return null;
		}

		if (imageData == null) {
			return null;
		}

		return new Image(display, imageData);
	}
}

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

package org.eclipse.andmore.gltrace;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

public class GLUtils {
	public static String formatData(byte[] data, GLEnum format) {
		switch (format) {
		case GL_BYTE:
			return formatBytes(data, false);
		case GL_UNSIGNED_BYTE:
			return formatBytes(data, true);
		case GL_SHORT:
			return formatShorts(data, false);
		case GL_UNSIGNED_SHORT:
			return formatShorts(data, true);
		case GL_FIXED:
			return formatInts(data);
		case GL_FLOAT:
			return formatFloats(data);
		default:
			return ""; //$NON-NLS-1$
		}
	}

	private static String formatFloats(byte[] data) {
		FloatBuffer bb = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer();

		StringBuilder sb = new StringBuilder(bb.capacity() * 3);

		while (bb.remaining() > 0) {
			sb.append(String.format("%.4f", bb.get()));
			sb.append(',');
			sb.append('\n');
		}

		return sb.toString();
	}

	private static String formatInts(byte[] data) {
		IntBuffer bb = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();

		StringBuilder sb = new StringBuilder(bb.capacity() * 3);

		while (bb.remaining() > 0) {
			sb.append(bb.get());
			sb.append(',');
			sb.append('\n');
		}

		return sb.toString();
	}

	private static String formatShorts(byte[] data, boolean unsigned) {
		ShortBuffer bb = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();

		StringBuilder sb = new StringBuilder(bb.capacity() * 3);

		while (bb.remaining() > 0) {
			if (unsigned) {
				sb.append(bb.get() & 0xffff);
			} else {
				sb.append(bb.get());
			}
			sb.append(',');
			sb.append('\n');
		}

		return sb.toString();
	}

	private static String formatBytes(byte[] data, boolean unsigned) {
		ByteBuffer bb = ByteBuffer.wrap(data);

		StringBuilder sb = new StringBuilder(bb.capacity() * 3);

		while (bb.remaining() > 0) {
			if (unsigned) {
				sb.append(bb.get() & 0xff);
			} else {
				sb.append(bb.get());
			}

			sb.append(',');
			sb.append('\n');
		}

		return sb.toString();
	}
}

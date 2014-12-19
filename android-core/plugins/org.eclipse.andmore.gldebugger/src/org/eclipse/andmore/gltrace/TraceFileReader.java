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

import com.google.protobuf.InvalidProtocolBufferException;

import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.eclipse.andmore.gltrace.GLProtoBuf.GLMessage;

public class TraceFileReader {
	/**
	 * Maximum size for a protocol buffer message. The message size is dominated
	 * by the size of the compressed framebuffer. Currently, we assume that the
	 * maximum is for a 1080p display. Since the buffers compress well, we
	 * should probably never get close to this.
	 */
	private static final int MAX_PROTOBUF_SIZE = 1920 * 1080 * 100;

	/**
	 * Obtain the next protobuf message in this file.
	 * 
	 * @param file
	 *            file to read from
	 * @param offset
	 *            offset to start reading from
	 * @return protobuf message at given offset
	 * @throws IOException
	 *             in case of file I/O errors
	 * @throws InvalidProtocolBufferException
	 *             if protobuf is not well formed
	 */
	public GLMessage getMessageAtOffset(RandomAccessFile file, long offset) throws IOException {
		int len;
		byte[] b;
		try {
			if (offset != -1) {
				file.seek(offset);
			}

			len = file.readInt();
			if (len > MAX_PROTOBUF_SIZE) {
				String msg = String.format("Unexpectedly large (%d bytes) protocol buffer message encountered.", len);
				throw new InvalidProtocolBufferException(msg);
			}

			b = new byte[len];
			file.readFully(b);
		} catch (EOFException e) {
			return null;
		}

		return GLMessage.parseFrom(b);
	}
}

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

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Write trace control options to the trace backend. Currently, the number of
 * options is limited, so all the options are packed into a single integer. Any
 * changes to this protocol have to be updated on the device as well.
 */
public class TraceCommandWriter {
	private static final int CMD_SIZE = 4;

	private static final int READ_FB_ON_EGLSWAP_BIT = 0;
	private static final int READ_FB_ON_GLDRAW_BIT = 1;
	private static final int READ_TEXTURE_DATA_ON_GLTEXIMAGE_BIT = 2;

	private final DataOutputStream mStream;;

	public TraceCommandWriter(DataOutputStream traceCommandStream) {
		mStream = traceCommandStream;
	}

	public void setTraceOptions(boolean readFbOnEglSwap, boolean readFbOnGlDraw, boolean readTextureOnGlTexImage)
			throws IOException {
		int eglSwap = readFbOnEglSwap ? (1 << READ_FB_ON_EGLSWAP_BIT) : 0;
		int glDraw = readFbOnGlDraw ? (1 << READ_FB_ON_GLDRAW_BIT) : 0;
		int tex = readTextureOnGlTexImage ? (1 << READ_TEXTURE_DATA_ON_GLTEXIMAGE_BIT) : 0;

		int cmd = eglSwap | glDraw | tex;

		mStream.writeInt(CMD_SIZE);
		mStream.writeInt(cmd);
		mStream.flush();
	}

	public void close() {
		try {
			mStream.close();
		} catch (IOException e) {
			// ignore exception while closing stream
		}
	}
}

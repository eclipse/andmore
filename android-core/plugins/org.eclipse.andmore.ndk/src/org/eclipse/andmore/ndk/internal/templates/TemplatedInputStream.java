/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.eclipse.andmore.ndk.internal.templates;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Reads from a template substituting marked values from the supplied Map.
 */
public class TemplatedInputStream extends InputStream {

	private final InputStream mIn;
	private final Map<String, String> mMap;
	private char[] mSub;
	private int mPos;
	private int mMark;

	public TemplatedInputStream(InputStream in, Map<String, String> map) {
		this.mIn = in;
		this.mMap = map;
	}

	@Override
	public int read() throws IOException {
		// if from a mark, return the char
		if (mMark != 0) {
			int c = mMark;
			mMark = 0;
			return c;
		}

		// return char from sub layer if available
		if (mSub != null) {
			char c = mSub[mPos++];
			if (mPos >= mSub.length)
				mSub = null;
			return c;
		}

		int c = mIn.read();
		if (c == '%') {
			// check if it's a sub
			c = mIn.read();
			if (c == '{') {
				// it's a sub
				StringBuffer buff = new StringBuffer();
				for (c = mIn.read(); c != '}' && c >= 0; c = mIn.read())
					buff.append((char) c);
				String str = mMap.get(buff.toString());
				if (str != null) {
					mSub = str.toCharArray();
					mPos = 0;
				}
				return read(); // recurse to get the real char
			} else {
				// not a sub
				mMark = c;
				return '%';
			}
		}

		return c;
	}

	@Override
	public void close() throws IOException {
		super.close();
		mIn.close();
	}

}

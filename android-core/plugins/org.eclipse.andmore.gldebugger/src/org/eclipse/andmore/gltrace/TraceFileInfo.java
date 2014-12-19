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

public class TraceFileInfo {
	private final String mPath;
	private final long mSize;
	private final long mLastModified;

	public TraceFileInfo(String path, long size, long lastModified) {
		mPath = path;
		mSize = size;
		mLastModified = lastModified;
	}

	public String getPath() {
		return mPath;
	}

	public long getSize() {
		return mSize;
	}

	public long getLastModificationTime() {
		return mLastModified;
	}
}

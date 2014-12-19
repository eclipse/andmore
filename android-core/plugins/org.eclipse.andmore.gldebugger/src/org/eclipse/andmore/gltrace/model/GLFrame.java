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

package org.eclipse.andmore.gltrace.model;

/**
 * A GLFrame is used to keep track of the start and end {@link GLCall} indices
 * for each OpenGL frame.
 */
public class GLFrame {
	private final int mIndex;
	private final int mStartCallIndex;
	private final int mEndCallIndex;

	/**
	 * Construct a {@link GLFrame} given the range of {@link GLCall}s spanning
	 * this frame.
	 * 
	 * @param frameIndex
	 *            index of this frame in the trace.
	 * @param startCallIndex
	 *            index of the first call in this frame (inclusive).
	 * @param endCallIndex
	 *            index of the last call in this frame (exclusive).
	 */
	public GLFrame(int frameIndex, int startCallIndex, int endCallIndex) {
		mIndex = frameIndex;
		mStartCallIndex = startCallIndex;
		mEndCallIndex = endCallIndex;
	}

	public int getIndex() {
		return mIndex;
	}

	public int getStartIndex() {
		return mStartCallIndex;
	}

	public int getEndIndex() {
		return mEndCallIndex;
	}
}

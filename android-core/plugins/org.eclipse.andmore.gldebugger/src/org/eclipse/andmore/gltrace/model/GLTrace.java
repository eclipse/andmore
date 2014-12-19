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

import org.eclipse.andmore.gltrace.ProtoBufUtils;
import org.eclipse.andmore.gltrace.TraceFileInfo;
import org.eclipse.andmore.gltrace.TraceFileReader;
import org.eclipse.andmore.gltrace.GLProtoBuf.GLMessage;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Collections;
import java.util.List;

/** GLTrace is the in memory model of a OpenGL trace file. */
public class GLTrace {
	private static final TraceFileReader sTraceFileReader = new TraceFileReader();

	/** Information regarding the trace file. */
	private final TraceFileInfo mTraceFileInfo;

	/** List of frames in the trace. */
	private final List<GLFrame> mGLFrames;

	/** List of GL Calls comprising the trace. */
	private final List<GLCall> mGLCalls;

	/** List of context ids used by the application. */
	private List<Integer> mContextIds;

	public GLTrace(TraceFileInfo traceFileInfo, List<GLFrame> glFrames, List<GLCall> glCalls, List<Integer> contextIds) {
		mTraceFileInfo = traceFileInfo;
		mGLFrames = glFrames;
		mGLCalls = glCalls;
		mContextIds = contextIds;
	}

	public List<GLFrame> getFrames() {
		return mGLFrames;
	}

	public GLFrame getFrame(int i) {
		return mGLFrames.get(i);
	}

	public List<GLCall> getGLCalls() {
		return mGLCalls;
	}

	public List<GLCall> getGLCallsForFrame(int frameIndex) {
		if (frameIndex >= mGLFrames.size()) {
			return Collections.emptyList();
		}

		GLFrame frame = mGLFrames.get(frameIndex);
		return mGLCalls.subList(frame.getStartIndex(), frame.getEndIndex());
	}

	public Image getImage(GLCall c) {
		if (!c.hasFb()) {
			return null;
		}

		if (isTraceFileModified()) {
			return null;
		}

		RandomAccessFile file;
		try {
			file = new RandomAccessFile(mTraceFileInfo.getPath(), "r"); //$NON-NLS-1$
		} catch (FileNotFoundException e1) {
			return null;
		}

		GLMessage m = null;
		try {
			m = sTraceFileReader.getMessageAtOffset(file, c.getOffsetInTraceFile());
		} catch (Exception e) {
			return null;
		} finally {
			try {
				file.close();
			} catch (IOException e) {
				// ignore exception while closing file
			}
		}

		return ProtoBufUtils.getImage(Display.getCurrent(), m);
	}

	private boolean isTraceFileModified() {
		File f = new File(mTraceFileInfo.getPath());
		return f.length() != mTraceFileInfo.getSize() || f.lastModified() != mTraceFileInfo.getLastModificationTime();
	}

	public List<Integer> getContexts() {
		return mContextIds;
	}
}

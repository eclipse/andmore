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
import org.eclipse.andmore.gltrace.GLProtoBuf.GLMessage.Function;
import org.eclipse.andmore.gltrace.format.GLAPISpec;
import org.eclipse.andmore.gltrace.format.GLMessageFormatter;
import org.eclipse.andmore.gltrace.model.GLCall;
import org.eclipse.andmore.gltrace.model.GLFrame;
import org.eclipse.andmore.gltrace.model.GLTrace;
import org.eclipse.andmore.gltrace.state.transforms.StateTransformFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class TraceFileParserTask implements IRunnableWithProgress {
	private static final TraceFileReader sReader = new TraceFileReader();

	private static final GLMessageFormatter sGLMessageFormatter = new GLMessageFormatter(GLAPISpec.getSpecs());

	private String mTraceFilePath;
	private RandomAccessFile mFile;

	private List<GLCall> mGLCalls;
	private Set<Integer> mGLContextIds;

	private GLTrace mTrace;

	/**
	 * Construct a GL Trace file parser.
	 * 
	 * @param path
	 *            path to trace file
	 */
	public TraceFileParserTask(String path) {
		try {
			mFile = new RandomAccessFile(path, "r"); //$NON-NLS-1$
		} catch (FileNotFoundException e) {
			throw new IllegalArgumentException(e);
		}

		mTraceFilePath = path;
		mGLCalls = new ArrayList<GLCall>();
		mGLContextIds = new TreeSet<Integer>();
	}

	private void addMessage(int index, long traceFileOffset, GLMessage msg, long startTime) {
		String formattedMsg;
		try {
			formattedMsg = sGLMessageFormatter.formatGLMessage(msg);
		} catch (Exception e) {
			formattedMsg = String.format("%s()", msg.getFunction().toString()); //$NON-NLS-1$
		}

		GLCall c = new GLCall(index, startTime, traceFileOffset, formattedMsg, msg.getFunction(), msg.hasFb(),
				msg.getContextId(), msg.getDuration(), msg.getThreadtime());

		addProperties(c, msg);

		try {
			c.setStateTransformations(StateTransformFactory.getTransformsFor(msg));
		} catch (Exception e) {
			c.setStateTransformationCreationError(e.getMessage());
			GlTracePlugin.getDefault().logMessage("Error while creating transformations for " + c.toString() + ":");
			GlTracePlugin.getDefault().logMessage(e.getMessage());
		}

		mGLCalls.add(c);
		mGLContextIds.add(Integer.valueOf(c.getContextId()));
	}

	/**
	 * Save important values from the {@link GLMessage} in the {@link GLCall} as
	 * properties.
	 */
	private void addProperties(GLCall c, GLMessage msg) {
		switch (msg.getFunction()) {
		case glPushGroupMarkerEXT:
			// void PushGroupMarkerEXT(sizei length, const char *marker);
			// save the marker name
			c.addProperty(GLCall.PROPERTY_MARKERNAME, msg.getArgs(1).getCharValue(0).toStringUtf8());
			break;
		case glVertexAttribPointerData:
			// void glVertexAttribPointerData(GLuint indx, GLint size, GLenum
			// type,
			// GLboolean normalized, GLsizei stride, const GLvoid* ptr,
			// int minIndex, int maxIndex)
			c.addProperty(GLCall.PROPERTY_VERTEX_ATTRIB_POINTER_SIZE, Integer.valueOf(msg.getArgs(1).getIntValue(0)));
			c.addProperty(GLCall.PROPERTY_VERTEX_ATTRIB_POINTER_TYPE, GLEnum.valueOf(msg.getArgs(2).getIntValue(0)));
			c.addProperty(GLCall.PROPERTY_VERTEX_ATTRIB_POINTER_DATA, msg.getArgs(5).getRawBytes(0).toByteArray());
			break;
		default:
			break;
		}
	}

	/**
	 * Parse the entire file and create a {@link GLTrace} object that can be
	 * retrieved using {@link #getTrace()}.
	 */
	@Override
	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		long fileLength;
		try {
			fileLength = mFile.length();
		} catch (IOException e1) {
			fileLength = 0;
		}

		monitor.beginTask("Parsing OpenGL Trace File", fileLength > 0 ? 100 : IProgressMonitor.UNKNOWN);

		List<GLFrame> glFrames = null;

		try {
			GLMessage msg = null;
			int msgCount = 0;
			long filePointer = mFile.getFilePointer();
			int percentParsed = 0;

			// counters that maintain some statistics about the trace messages
			long minTraceStartTime = Long.MAX_VALUE;

			while ((msg = sReader.getMessageAtOffset(mFile, -1)) != null) {
				if (minTraceStartTime > msg.getStartTime()) {
					minTraceStartTime = msg.getStartTime();
				}

				addMessage(msgCount, filePointer, msg, msg.getStartTime() - minTraceStartTime);

				filePointer = mFile.getFilePointer();
				msgCount++;

				if (monitor.isCanceled()) {
					throw new InterruptedException();
				}

				if (fileLength > 0) {
					int percentParsedNow = (int) ((filePointer * 100) / fileLength);
					monitor.worked(percentParsedNow - percentParsed);
					percentParsed = percentParsedNow;
				}
			}

			if (mGLContextIds.size() > 1) {
				// if there are multiple contexts, then the calls may arrive at
				// the
				// host out of order. So we perform a sort based on the
				// invocation time.
				Collections.sort(mGLCalls, new Comparator<GLCall>() {
					@Override
					public int compare(GLCall c1, GLCall c2) {
						long diff = (c1.getStartTime() - c2.getStartTime());

						// We could return diff casted to an int. But in Java,
						// casting
						// from a long to an int truncates the bits and will not
						// preserve
						// the sign. So we resort to comparing the diff to 0 and
						// returning
						// the sign.
						if (diff == 0) {
							return 0;
						} else if (diff > 0) {
							return 1;
						} else {
							return -1;
						}
					}
				});

				// reassign indices after sorting
				for (int i = 0; i < mGLCalls.size(); i++) {
					mGLCalls.get(i).setIndex(i);
				}
			}

			glFrames = createFrames(mGLCalls);
		} catch (Exception e) {
			throw new InvocationTargetException(e);
		} finally {
			try {
				mFile.close();
			} catch (IOException e) {
				// ignore exception while closing file
			}
			monitor.done();
		}

		File f = new File(mTraceFilePath);
		TraceFileInfo fileInfo = new TraceFileInfo(mTraceFilePath, f.length(), f.lastModified());
		mTrace = new GLTrace(fileInfo, glFrames, mGLCalls, new ArrayList<Integer>(mGLContextIds));
	}

	/** Assign GL calls to GL Frames. */
	private List<GLFrame> createFrames(List<GLCall> calls) {
		List<GLFrame> glFrames = new ArrayList<GLFrame>();
		int startCallIndex = 0;
		int frameIndex = 0;

		for (int i = 0; i < calls.size(); i++) {
			GLCall c = calls.get(i);
			if (c.getFunction() == Function.eglSwapBuffers) {
				glFrames.add(new GLFrame(frameIndex, startCallIndex, i + 1));
				startCallIndex = i + 1;
				frameIndex++;
			}
		}

		// assign left over calls at the end to the last frame
		if (startCallIndex != mGLCalls.size()) {
			glFrames.add(new GLFrame(frameIndex, startCallIndex, mGLCalls.size()));
		}

		return glFrames;
	}

	/**
	 * Retrieve the trace object constructed from messages in the trace file.
	 */
	public GLTrace getTrace() {
		return mTrace;
	}
}

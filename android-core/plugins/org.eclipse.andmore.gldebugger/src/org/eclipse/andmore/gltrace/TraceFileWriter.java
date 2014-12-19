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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.eclipse.andmore.gltrace.GLProtoBuf.GLMessage;
import org.eclipse.andmore.gltrace.GLProtoBuf.GLMessage.Function;

/** A class that streams data received from a socket into the trace file. */
public class TraceFileWriter {
	private DataInputStream mInputStream;
	private DataOutputStream mOutputStream;
	private Thread mReceiverThread;

	private int mFileSize = 0;
	private int mFrameCount = 0;

	/**
	 * Construct a trace file writer.
	 * 
	 * @param fos
	 *            output stream to write trace data to
	 * @param is
	 *            input stream from which trace data is read
	 */
	public TraceFileWriter(FileOutputStream fos, DataInputStream is) {
		mOutputStream = new DataOutputStream(fos);
		mInputStream = is;
	}

	public void start() {
		// launch thread
		mReceiverThread = new Thread(new GLTraceReceiverTask());
		mReceiverThread.setName("GL Trace Receiver");
		mReceiverThread.start();
	}

	public void stopTracing() {
		// close socket to stop the receiver thread
		try {
			mInputStream.close();
		} catch (IOException e) {
			// ignore exception while closing socket
		}

		// wait for receiver to complete
		try {
			mReceiverThread.join();
		} catch (InterruptedException e1) {
			// ignore, this cannot be interrupted
		}

		// close stream
		try {
			mOutputStream.close();
		} catch (IOException e) {
			// ignore error while closing stream
		}
	}

	/**
	 * The GLTraceReceiverTask collects trace data from the device and writes it
	 * into a file while collecting some stats on the way.
	 */
	private class GLTraceReceiverTask implements Runnable {
		@Override
		public void run() {
			while (true) {
				byte[] buffer = readTraceData(mInputStream);
				if (buffer == null) {
					break;
				}

				try {
					writeTraceData(buffer, mOutputStream);
				} catch (IOException e) {
					break;
				}

				updateTraceStats(buffer);
			}
		}
	}

	private byte[] readTraceData(DataInputStream dis) {
		int len;
		try {
			len = dis.readInt();
		} catch (IOException e1) {
			return null;
		}
		len = Integer.reverseBytes(len); // readInt is big endian, we want
											// little endian

		byte[] buffer = new byte[len];
		int readLen = 0;
		while (readLen < len) {
			try {
				int read = dis.read(buffer, readLen, len - readLen);
				if (read < 0) {
					return null;
				} else {
					readLen += read;
				}
			} catch (IOException e) {
				return null;
			}
		}

		return buffer;
	}

	private void writeTraceData(byte[] buffer, DataOutputStream stream) throws IOException {
		stream.writeInt(buffer.length);
		stream.write(buffer);
	}

	private void updateTraceStats(byte[] buffer) {
		GLMessage msg = null;
		try {
			msg = GLMessage.parseFrom(buffer);
		} catch (InvalidProtocolBufferException e) {
			return;
		}

		mFileSize += buffer.length;

		if (msg.getFunction() == Function.eglSwapBuffers) {
			mFrameCount++;
		}
	}

	public int getCurrentFileSize() {
		return mFileSize;
	}

	public int getCurrentFrameCount() {
		return mFrameCount;
	}
}

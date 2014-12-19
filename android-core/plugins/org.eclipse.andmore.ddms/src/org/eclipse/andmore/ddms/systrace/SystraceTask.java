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

package org.eclipse.andmore.ddms.systrace;

import com.android.ddmlib.IDevice;
import com.android.ddmlib.IShellOutputReceiver;
import com.google.common.primitives.Bytes;

public class SystraceTask implements Runnable {
	private final IDevice mDevice;
	private final String mOptions;

	private volatile boolean mCancel;

	private final Object mLock = new Object();
	private String errorMessage;
	private boolean mTraceComplete;
	private byte[] mBuffer = new byte[1024];
	private int mDataLength = 0;

	public SystraceTask(IDevice device, String options) {
		mDevice = device;
		mOptions = options;
	}

	@Override
	public void run() {
		try {
			mDevice.executeShellCommand("atrace " + mOptions, new Receiver(), 0);
		} catch (Exception e) {
			synchronized (mLock) {
				errorMessage = "Unexpected error while running atrace on device: " + e;
			}
		}
	}

	public void cancel() {
		mCancel = true;
	}

	public String getError() {
		synchronized (mLock) {
			return errorMessage;
		}
	}

	public byte[] getAtraceOutput() {
		synchronized (mLock) {
			return mTraceComplete ? mBuffer : null;
		}
	}

	private class Receiver implements IShellOutputReceiver {
		@Override
		public void addOutput(byte[] data, int offset, int length) {
			synchronized (mLock) {
				if (mDataLength + length > mBuffer.length) {
					mBuffer = Bytes.ensureCapacity(mBuffer, mDataLength + length + 1, 1024);
				}

				for (int i = 0; i < length; i++) {
					mBuffer[mDataLength + i] = data[offset + i];
				}
				mDataLength += length;
			}
		}

		@Override
		public void flush() {
			synchronized (mLock) {
				// trim mBuffer to its final size
				byte[] copy = new byte[mDataLength];
				for (int i = 0; i < mDataLength; i++) {
					copy[i] = mBuffer[i];
				}
				mBuffer = copy;

				mTraceComplete = true;
			}
		}

		@Override
		public boolean isCancelled() {
			return mCancel;
		}
	}
}

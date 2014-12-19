/*
 * Copyright (C) 2012 The Android Open Source Project
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

package org.eclipse.andmore.ndk.internal.launch;

import com.android.ddmlib.IDevice;
import com.android.ddmlib.IShellOutputReceiver;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The {@link GdbServerTask} launches gdbserver on the given device and attaches
 * it to provided pid.
 */
public class GdbServerTask implements Runnable {
	private IDevice mDevice;
	private String mRunAs;
	private String mSocket;
	private int mPid;
	private CountDownLatch mAttachLatch;

	private GdbServerOutputReceiver mOutputReceiver;
	private Exception mLaunchException;

	private AtomicBoolean mCancelled = new AtomicBoolean(false);
	private AtomicBoolean mHasCompleted = new AtomicBoolean(false);

	/**
	 * Construct a gdbserver task.
	 * 
	 * @param device
	 *            device to run gdbserver on
	 * @param runAsPackage
	 *            name of the package in which gdbserver resides
	 * @param socketName
	 *            name of the local socket on which the server will listen
	 * @param pid
	 *            pid of task to attach to
	 * @param attachLatch
	 *            latch to notify when gdbserver gets attached to the task
	 */
	public GdbServerTask(IDevice device, String runAsPackage, String socketName, int pid, CountDownLatch attachLatch) {
		mDevice = device;
		mRunAs = runAsPackage;
		mSocket = socketName;
		mPid = pid;
		mAttachLatch = attachLatch;

		mOutputReceiver = new GdbServerOutputReceiver();
	}

	/**
	 * Runs gdbserver on the device and connects to the given task. If gdbserver
	 * manages to successfully attach itself to the process, then it counts down
	 * on its attach latch.
	 */
	@Override
	public void run() {
		// Launch gdbserver on the device.
		String command = String.format("run-as %s lib/gdbserver +%s --attach %d", mRunAs, mSocket, mPid);
		try {
			mDevice.executeShellCommand(command, mOutputReceiver, 0);
		} catch (Exception e) {
			mLaunchException = e;
		}
	}

	/**
	 * Returns any exceptions that might have occurred while launching
	 * gdbserver.
	 */
	public Exception getLaunchException() {
		return mLaunchException;
	}

	/** Cancel gdbserver if it is running. */
	public void setCancelled() {
		mCancelled.set(true);
	}

	public String getShellOutput() {
		return mOutputReceiver.getOutput();
	}

	private class GdbServerOutputReceiver implements IShellOutputReceiver {
		private StringBuffer mOutput = new StringBuffer(100);

		@Override
		public synchronized void addOutput(byte[] data, int offset, int length) {
			mOutput.append(new String(data, offset, length));

			// notify other threads that gdbserver has attached to the task
			if (mOutput.toString().contains("Attached")) {
				mAttachLatch.countDown();
			}
		}

		@Override
		public void flush() {
			mHasCompleted.set(true);
		}

		@Override
		public boolean isCancelled() {
			return mCancelled.get();
		}

		public synchronized String getOutput() {
			return mOutput.toString();
		}
	}
}

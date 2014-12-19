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
package org.eclipse.andmore.android.emulator.logic;

import static org.eclipse.andmore.android.common.log.StudioLogger.error;
import static org.eclipse.andmore.android.common.log.StudioLogger.info;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.andmore.android.DDMSFacade;
import org.eclipse.andmore.android.ISerialNumbered;
import org.eclipse.andmore.android.common.exception.AndroidException;
import org.eclipse.andmore.android.emulator.core.exception.InstanceStartException;
import org.eclipse.andmore.android.emulator.core.exception.StartCancelledException;
import org.eclipse.andmore.android.emulator.core.exception.StartTimeoutException;
import org.eclipse.andmore.android.emulator.i18n.EmulatorNLS;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;

/**
 * This class is used as an utilities class for operations related to Android
 * Devices
 * 
 */
public class AndroidLogicUtils {
	private static final int INITIAL_VNC_PORT_VALUE = 5900;

	public static final String ORIENTATION_BASE_COMMAND = "sendevent /dev/input/event0 ";

	/**
	 * Execute the VM process
	 * 
	 * @param cmd
	 *            the command to be executed
	 * @return the VM process
	 * 
	 * @throws AndroidException
	 *             if the command failed to execute
	 */
	public static Process executeProcess(final String cmd) throws AndroidException {
		try {
			info("Executing command " + cmd);
			Process vmProcess = Runtime.getRuntime().exec(cmd);
			return vmProcess;
		} catch (IOException e) {
			error("Falied to execute the command: " + cmd);
			throw new AndroidException(NLS.bind(EmulatorNLS.EXC_AndroidLogicUtils_CannotStartProcess, cmd));
		}
	}

	/**
	 * Execute the VM process
	 * 
	 * @param cmd
	 *            the command to be executed, described as an array
	 * @return the VM process
	 * 
	 * @throws AndroidException
	 *             if the command failed to execute
	 */
	public static Process executeProcess(final String[] cmd) throws AndroidException {
		String cmdString = "";
		for (int i = 0; i < cmd.length; i++) {
			cmdString += cmd[i] + " ";

		}

		return executeProcess(cmd, cmdString);
	}

	/**
	 * Execute the VM process
	 * 
	 * @param cmd
	 *            The command to be executed, described as an array
	 * @param cmdToLog
	 *            The command to be logged.
	 * 
	 * @return the VM process
	 * 
	 * @throws AndroidException
	 *             if the command failed to execute
	 */
	public static Process executeProcess(final String[] cmd, final String cmdToLog) throws AndroidException {
		try {
			info("Executing command " + cmdToLog);
			Process vmProcess = Runtime.getRuntime().exec(cmd);
			return vmProcess;
		} catch (IOException e) {
			error("Falied to execute the command: " + cmd);
			throw new AndroidException(NLS.bind(EmulatorNLS.EXC_AndroidLogicUtils_CannotStartProcess, cmd));
		}
	}

	/**
	 * }
	 * 
	 * /** Checks if the user has canceled the VM startup
	 *
	 * @param monitor
	 *            A progress monitor that will give the user feedback about this
	 *            long running operation
	 * @param instanceHost
	 *            The IP address of the started emulator instance
	 *
	 * @return True if the operation can proceed, false otherwise
	 * 
	 * @throws StartCancelledException
	 *             If the user has canceled the start process
	 */
	public static void testCanceled(IProgressMonitor monitor) throws StartCancelledException {
		if (monitor.isCanceled()) {
			info("Operation canceled by the user");
			monitor.subTask(EmulatorNLS.MON_AndroidEmulatorStarter_Canceling);

			throw new StartCancelledException(EmulatorNLS.EXC_AndroidEmulatorStarter_EmulatorStartCanceled);
		}
	}

	/**
	 * Checks if the timeout limit has reached
	 * 
	 * @param timeoutLimit
	 *            The system time limit that cannot be overtaken, in
	 *            milliseconds
	 * 
	 * @throws StartTimeoutException
	 *             When the system time limit is overtaken
	 */
	public static void testTimeout(long timeoutLimit, String timeoutErrorMessage) throws StartTimeoutException {
		if (System.currentTimeMillis() > timeoutLimit) {
			error("The emulator was not up within the set timeout");
			throw new StartTimeoutException(timeoutErrorMessage);
		}
	}

	/**
	 * Get the relative timeout limit, which is the the current time plus the
	 * timeout value
	 * 
	 * @param timeout
	 *            timeout value (in milliseconds)
	 * @return Relative timeout limit
	 */
	public static long getTimeoutLimit(int timeout) {
		return System.currentTimeMillis() + timeout;
	}

	/**
	 * Check if the given process is still up and running
	 * 
	 * @param p
	 *            process
	 * @throws InstanceStartException
	 */
	public static void testProcessStatus(Process p) throws InstanceStartException {

		boolean isRunning;
		int exitCode;

		try {
			exitCode = p.exitValue();
			isRunning = false;
		} catch (Exception e) {
			// emulator process is still running... so everything looks fine...
			isRunning = true;
			exitCode = 0;
		}

		if (!isRunning) {
			error("Emulator process is not running! Exit code:" + exitCode);
			StringBuffer outBuf = null;
			InputStream inStream = null;

			int ch;

			// Getting error output stream
			String processAnswer = "";
			inStream = p.getErrorStream();
			outBuf = new StringBuffer();
			try {
				while ((ch = inStream.read()) != -1) {
					outBuf.append((char) ch + "");
				}
			} catch (IOException e) {
				error("Cannot read error output  stream from Emulator proccess");
			}

			processAnswer = outBuf.toString();

			if (processAnswer.length() == 0) {
				// if no error came from process, get standard output stream
				inStream = p.getInputStream();
				outBuf = new StringBuffer();
				try {
					while ((ch = inStream.read()) != -1) {
						outBuf.append((char) ch + "");
					}
				} catch (IOException e) {
					error("Cannot read standard output stream from Emulator proccess");
				}

				processAnswer = outBuf.toString();

			}
			String msg = EmulatorNLS.EXC_AndroidEmulatorStarter_ProcessTerminated;
			msg += processAnswer;
			throw new InstanceStartException(msg);
		}

	}

	/**
	 * Kill the communication channel
	 * 
	 * @param instance
	 *            Android instance
	 */
	public static void kill(IAndroidLogicInstance instance) {
		if (instance instanceof ISerialNumbered) {
			String serialNumber = ((ISerialNumbered) instance).getSerialNumber();
			DDMSFacade.kill(serialNumber);
			Process process = instance.getProcess();
			if (process != null) {
				int tries = 0;
				Integer exitValue = null;
				while ((process != null) && (tries < 10) && (exitValue == null)) {
					try {
						exitValue = process.exitValue();
					} catch (Throwable t) {
						tries++;
						try {
							Thread.sleep(250);
						} catch (InterruptedException e) {
							// do nothing
						}
					}
				}
				process.destroy();
				instance.setProcess(null);
			}
		}
	}

	/**
	 * Get the VNC port forward
	 * 
	 * @param serial
	 *            port number
	 * @return VNC port
	 */
	public static int getVncServerPortFoward(String serial) {
		if (serial == null) {
			return 0;
		}

		int stringSize = serial.length();
		String lastTwoNumbers = serial.substring(stringSize - 2, stringSize);

		int port = INITIAL_VNC_PORT_VALUE;

		try {
			port += Integer.valueOf(lastTwoNumbers);
		} catch (NumberFormatException e) {
			// do nothing (this should not happen)
		}

		return port;

	}

	public static int getEmulatorPort(String serial) {
		if (serial == null) {
			return 0;
		}

		int stringSize = serial.length();
		String lastFourNumbers = serial.substring(stringSize - 4, stringSize);

		int port = 0;

		try {
			port = Integer.valueOf(lastFourNumbers);
		} catch (NumberFormatException e) {
			// do nothing (this should not happen)
		}
		return port;
	}

	/**
	 * Checks if the Device is still online... If the device is not online it is
	 * not possible to communicate with it. Notice it is a verification of the
	 * status of the Device wich may be different than the status of the Tml
	 * Instance...
	 *
	 * @param serialNumber
	 *            serial number of the device
	 * 
	 * @throws AndroidException
	 *             If the device is not started
	 */
	public static void testDeviceStatus(String serialNumber) throws AndroidException {
		if (!DDMSFacade.isDeviceOnline(serialNumber)) {
			info("Device is offline: " + serialNumber);

			throw new AndroidException(EmulatorNLS.EXC_AndroidLogicUtils_DeviceIsOffline);
		}
	}
}

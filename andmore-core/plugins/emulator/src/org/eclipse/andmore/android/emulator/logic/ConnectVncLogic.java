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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.andmore.android.emulator.core.exception.InstanceStartException;
import org.eclipse.andmore.android.emulator.core.exception.StartCancelledException;
import org.eclipse.andmore.android.emulator.core.exception.StartTimeoutException;
import org.eclipse.andmore.android.emulator.core.model.IAndroidEmulatorInstance;
import org.eclipse.andmore.android.emulator.i18n.EmulatorNLS;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.osgi.util.NLS;
import org.eclipse.sequoyah.vnc.protocol.PluginProtocolActionDelegate;
import org.eclipse.sequoyah.vnc.protocol.lib.IProtocolExceptionHandler;
import org.eclipse.sequoyah.vnc.protocol.lib.ProtocolHandle;

/**
 * This class contains the logic to stablish VNC connections
 * 
 */
public class ConnectVncLogic implements IAndroidLogic {
	/**
	 * The port that is used to start the communication with the instance. It
	 * corresponds to the VNC display 1 port
	 */
	private static final String LOCALHOST_IP_ADDRESS = "127.0.0.1";

	public IJobChangeEvent vncServerDoneEvent = null;

	/**
	 * Initialize by connecting to VNC
	 * 
	 * @see org.eclipse.andmore.android.emulator.logic.IAndroidLogic#execute(IAndroidLogicInstance,
	 *      int, IProgressMonitor)
	 */
	@Override
	public void execute(final IAndroidLogicInstance instance, final int timeout, IProgressMonitor monitor)
			throws StartTimeoutException, StartCancelledException, InstanceStartException {
		connectVnc(instance, timeout, monitor);
	}

	/**
	 * Connect to VNC
	 * 
	 * @param instance
	 *            instance to connect
	 * @param timeout
	 *            timeout for the operation
	 * @param monitor
	 *            monitor for this operation
	 * 
	 * @throws InstanceStartException
	 */
	private void connectVnc(IAndroidLogicInstance instance, int timeout, IProgressMonitor monitor)
			throws StartTimeoutException, StartCancelledException, InstanceStartException {
		info("Trying to estabilish vnc connection with " + instance.getName());
		long timeoutLimit = System.currentTimeMillis() + timeout;
		try {
			startProtocol(instance, timeoutLimit,
					AndroidLogicUtils.getVncServerPortFoward(instance.getInstanceIdentifier()), monitor);
		} catch (StartTimeoutException ise) {
			info("The protocol or the emulator services could not be launched. Stopping the instance.");
			throw ise;
		}
		info("VNC Protocol is running for " + instance.getName());
	}

	/**
	 * Starts protocol connection
	 * 
	 * @param instance
	 *            The Android device instance
	 * @param timeoutLimit
	 *            The timestamp of the time when timeout happens
	 * @param instanceHost
	 *            The IP address of the started emulator instance
	 * @param monitor
	 *            A progress monitor that will give the user feedback about this
	 *            long running operation
	 * 
	 * @throws InstanceStartException
	 *             If some fatal error occurs during the start process, that may
	 *             require status update at the clients
	 * @throws StartCancelledException
	 *             If the user presses the "Cancel" button at the progress
	 *             monitor
	 * @throws InstanceStartException
	 */
	private void startProtocol(IAndroidEmulatorInstance instance, long timeoutLimit, int port, IProgressMonitor monitor)
			throws StartTimeoutException, StartCancelledException, InstanceStartException {
		try {
			monitor.beginTask(EmulatorNLS.MON_AndroidEmulatorStarter_ConnectingToEmulator, 100);
			monitor.setTaskName(EmulatorNLS.MON_AndroidEmulatorStarter_ConnectingToEmulator);

			testVncServer(instance);
			AndroidLogicUtils.testCanceled(monitor);
			requestStartProtocol(instance, port);
			ProtocolHandle handle = instance.getProtocolHandle();

			while (!PluginProtocolActionDelegate.isProtocolRunning(handle)) {
				AndroidLogicUtils.testCanceled(monitor);

				try {
					Thread.sleep(500);
				} catch (InterruptedException e1) {
					// Do nothing.
				}

				AndroidLogicUtils.testTimeout(timeoutLimit,
						EmulatorNLS.EXC_AndroidEmulatorStarter_TimeoutWhileRunningProtocol);

			}
			monitor.worked(100);
		} finally {
			monitor.done();
		}
	}

	/**
	 * Set if the job has been completed
	 * 
	 * @param jobEvent
	 *            event job
	 */
	public void setVncServerDoneEvent(IJobChangeEvent jobEvent) {
		this.vncServerDoneEvent = jobEvent;
	}

	/**
	 * Test if the VNC server is up and running
	 * 
	 * @param instance
	 *            emulator instance
	 * 
	 * @throws InstanceStartException
	 */
	private void testVncServer(final IAndroidEmulatorInstance instance) throws InstanceStartException {
		if (vncServerDoneEvent != null) {
			IStatus jobResult = vncServerDoneEvent.getResult();
			String reason = "";
			if (IStatus.ERROR == jobResult.getSeverity()) {
				reason = jobResult.getMessage();
			} else if (Status.CANCEL_STATUS.equals(jobResult)) {
				reason = EmulatorNLS.INFO_ConnectVncLogic_UserCancelledVncServerStart;
			}

			String message = NLS.bind(EmulatorNLS.EXC_VncServerNotRunning, new String[] { instance.getName(), reason });
			throw new InstanceStartException(message);
		}
	}

	/**
	 * Starts the protocol execution, connecting to the server accessible
	 * through the provided Android Emulator instance
	 * 
	 * @param androidInstance
	 *            The Android device instance
	 */
	private void requestStartProtocol(IAndroidEmulatorInstance androidInstance, int port) throws InstanceStartException {
		if (androidInstance != null) {
			ProtocolHandle handle = null;

			try {
				// Start protocol and screen update
				info("Requesting protocol start");
				Map<String, Object> parameters = new HashMap<String, Object>();
				parameters.put("password", "");
				parameters.put("bypassProxy", new Boolean(true));
				IProtocolExceptionHandler excHandler = new AndroidExceptionHandler();
				handle = PluginProtocolActionDelegate.requestStartProtocolAsClient("vncProtocol38", excHandler,
						LOCALHOST_IP_ADDRESS, port, parameters);

				androidInstance.setProtocolHandle(handle);
			} catch (Exception e) {
				error("There is an error at the protocol specification.");
				throw new InstanceStartException(EmulatorNLS.EXC_CouldNotStartProtocol);
			}
		} else {
			error("Could not start the protocol, because the provided instance is null");
			throw new InstanceStartException(EmulatorNLS.EXC_CouldNotStartProtocol);
		}
	}

}

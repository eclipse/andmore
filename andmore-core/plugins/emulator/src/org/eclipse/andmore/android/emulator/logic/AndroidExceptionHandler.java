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

import static org.eclipse.andmore.android.common.log.StudioLogger.debug;
import static org.eclipse.andmore.android.common.log.StudioLogger.error;
import static org.eclipse.andmore.android.common.log.StudioLogger.info;
import static org.eclipse.andmore.android.common.log.StudioLogger.warn;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.eclipse.andmore.android.ISerialNumbered;
import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.andmore.android.emulator.core.exception.InstanceNotFoundException;
import org.eclipse.andmore.android.emulator.core.exception.InstanceStopException;
import org.eclipse.andmore.android.emulator.core.model.IAndroidEmulatorInstance;
import org.eclipse.andmore.android.emulator.core.utils.EmulatorCoreUtils;
import org.eclipse.andmore.android.emulator.i18n.EmulatorNLS;
import org.eclipse.andmore.android.emulator.logic.AbstractStartAndroidEmulatorLogic.LogicMode;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.sequoyah.vnc.protocol.PluginProtocolActionDelegate;
import org.eclipse.sequoyah.vnc.protocol.lib.IProtocolExceptionHandler;
import org.eclipse.sequoyah.vnc.protocol.lib.ProtocolHandle;
import org.eclipse.sequoyah.vnc.protocol.lib.exceptions.InvalidDefinitionException;
import org.eclipse.sequoyah.vnc.protocol.lib.exceptions.InvalidInputStreamDataException;
import org.eclipse.sequoyah.vnc.protocol.lib.exceptions.InvalidMessageException;
import org.eclipse.sequoyah.vnc.protocol.lib.exceptions.MessageHandleException;
import org.eclipse.sequoyah.vnc.protocol.lib.exceptions.ProtocolHandshakeException;
import org.eclipse.sequoyah.vnc.protocol.lib.exceptions.ProtocolRawHandlingException;

/**
 * DESCRIPTION: Class that defines how to handle internal protocol exceptions
 * 
 * RESPONSABILITY: Handle internal protocol exceptions
 * 
 * COLABORATORS: None.
 * 
 * USAGE: This class shall be used by Eclipse only
 */
public class AndroidExceptionHandler implements IProtocolExceptionHandler {
	private int handlingLevel = 1;

	private boolean checkThreadRunning = false;

	private Lock lock = new ReentrantReadWriteLock().writeLock();

	private static Collection<String> stoppedWithFailure = new HashSet<String>();

	static {

		IJobManager manager = Job.getJobManager();
		manager.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				Job job = event.getJob();
				if (job.belongsTo(StartVncServerLogic.VNC_SERVER_JOB_FAMILY)) {
					IStatus result = event.getResult();
					if (!result.isOK() && !(result.getSeverity() == IStatus.CANCEL)) {
						stoppedWithFailure.add(job.getName());
					}
				}
			}

			@Override
			public void scheduled(IJobChangeEvent event) {
				Job job = event.getJob();
				if (job.belongsTo(StartVncServerLogic.VNC_SERVER_JOB_FAMILY)) {
					stoppedWithFailure.remove(job.getName());
				}
			}
		});
	}

	/**
	 * Handles internal IOExceptions caught by the protocol plugin during its
	 * execution.
	 * 
	 * @see IProtocolExceptionHandler#handleIOException(ProtocolHandle,
	 *      IOException)
	 */
	@Override
	public void handleIOException(ProtocolHandle handle, IOException e) {
		error("A socket was broken while communicating to server. Cause: " + e.getMessage());
		handleException(handle);
	}

	/**
	 * Handles exceptions thrown by the protocol plugin when it detects an
	 * invalid message definition provided by the plugin which is extending it
	 * (in this case, the core plugin).
	 * 
	 * @see IProtocolExceptionHandler#handleInvalidDefinitionException(ProtocolHandle,
	 *      InvalidDefinitionException)
	 */
	@Override
	public void handleInvalidDefinitionException(ProtocolHandle handle, InvalidDefinitionException e) {
		// This exception should not happen, because the message definitions are
		// provided
		// by the development team.
		warn("An invalid message definition was detected. Cause: " + e.getMessage());
		handleException(handle);
	}

	/**
	 * Handles exceptions thrown by the protocol plugin when it detects that the
	 * data retrieved from the connection does not match the format defined in
	 * the message definition.
	 * 
	 * @see IProtocolExceptionHandler#handleInvalidInputStreamDataException(ProtocolHandle,
	 *      InvalidInputStreamDataException)
	 */
	@Override
	public void handleInvalidInputStreamDataException(ProtocolHandle handle, InvalidInputStreamDataException e) {
		// If the data retrieved from the connection is not as expected
		// (considering
		// the message definition provided), there is a high chance of errors to
		// happen.
		// It is likely that the data from stream is no longer synchronized, so
		// the
		// exception handling for this case is to restart connection.

		error("Some received data is not compatible with the expected definition. Restarting the protocol for synchronization.");
		handleException(handle);
	}

	/**
	 * Handles exceptions thrown by the protocol plugin when it detects that the
	 * message provided for sending does not have enough or valid information,
	 * given a corresponding message definition
	 * 
	 * @see IProtocolExceptionHandler#handleInvalidMessageException(ProtocolHandle,
	 *      InvalidMessageException)
	 */
	@Override
	public void handleInvalidMessageException(ProtocolHandle handle, InvalidMessageException e) {
		// This exception should not happen, because the message object data is
		// provided
		// by the development team. Log only.
		warn("A message was not constructed according to its definition. Cause: " + e.getMessage());
		handleException(handle);
	}

	/**
	 * Handles exceptions thrown by any message handler when they discovers that
	 * it is not possible to handle the message and it is a fatal error for the
	 * protocol.
	 * 
	 * @see IProtocolExceptionHandler#handleMessageHandleException(ProtocolHandle,
	 *      MessageHandleException)
	 */
	@Override
	public void handleMessageHandleException(ProtocolHandle handle, MessageHandleException e) {
		// If a message handler throws a MessageHandleException, that means that
		// it cannot
		// continue. Restart the protocol to guarantee the synchronization.
		error("A message handler has ended in error and has thrown an exception meaning the protocol cannot continue.");
		handleException(handle);
	}

	/**
	 * Handles exceptions thrown by the protocol plugin when it is not possible
	 * to init the protocol, for example because the handshaking procedure has
	 * failed.
	 * 
	 * @see IProtocolExceptionHandler#handleProtocolHandshakeException(ProtocolHandle,
	 *      ProtocolHandshakeException)
	 */
	@Override
	public void handleProtocolHandshakeException(ProtocolHandle handle, ProtocolHandshakeException e) {
		error("Could not initialize the protocol.");
		handleException(handle);
	}

	/**
	 * Handles exceptions thrown by any raw field handler when they discovers
	 * that it is not possible to handle the field and it is a fatal error for
	 * the protocol.
	 * 
	 * @see IProtocolExceptionHandler#handleProtocolRawHandlingException(ProtocolHandle,
	 *      ProtocolRawHandlingException)
	 */
	@Override
	public void handleProtocolRawHandlingException(ProtocolHandle handle, ProtocolRawHandlingException e) {
		// This message should be thrown by raw field handlers when they cannot
		// handle the
		// raw field and need to abort the protocol execution. Restart the
		// protocol to
		// guarantee the synchronization.
		error("A raw field handler has ended in error and has thrown an exception meaning the protocol cannot continue.");
		handleException(handle);
	}

	/**
	 * This method will be called whenever an exception happens. It is important
	 * to find out if the failure happened during an start or restart procedure,
	 * so that we can do appropriate handling to each situation.
	 * 
	 * @param handle
	 *            The object that identifies the protocol instance
	 */
	private void handleException(ProtocolHandle handle) {
		IAndroidEmulatorInstance instance = null;
		if (lock.tryLock()) {
			try {
				instance = EmulatorCoreUtils.getAndroidInstanceByHandle(handle);

				try {
					debug("Check if device is online: " + instance);
					if (instance instanceof ISerialNumbered) {
						String serialNumber = ((ISerialNumbered) instance).getSerialNumber();
						AndroidLogicUtils.testDeviceStatus(serialNumber);
					}
				} catch (Exception e) {
					error("Device is not online. Abort VNC session...");
					abort(instance);
				}

				if ((handlingLevel == 3) && canRestartServer(instance)) {
					handlingLevel--;
				}

				// Firstly, try to restart only the VNC client. If restarting
				// the VNC client
				// is not possible, try to restart the VNC server at the device
				// and to connect
				// to it again. If the start logic cannot be retrieved, delegate
				// the decision
				// to the user.

				if (handlingLevel == 1) {
					restartClientOnly(handle);
					handlingLevel++;
				} else if (handlingLevel == 2) {
					restartServerAndClient(instance, handle);
					handlingLevel++;
				} else {
					if (delegateDecisionToUser(handle)) {
						abort(instance);
					}
				}
			} catch (InstanceNotFoundException e) {
				// If the instance is not found, it means that the instance is
				// stopped.
				// In this case, a restart is not applicable.
			} finally {
				lock.unlock();
			}
		}
	}

	private boolean canRestartServer(IAndroidEmulatorInstance instance) {
		String name = StartVncServerLogic.VNC_SERVER_JOB_PREFIX + instance.getName();
		return !stoppedWithFailure.contains(name);
	}

	private void restartClientOnly(final ProtocolHandle handle) {
		PluginProtocolActionDelegate.requestRestartProtocol(handle);

		if (!checkThreadRunning) {
			Runnable r = new Runnable() {
				@Override
				public void run() {
					checkThreadRunning = true;
					while (checkThreadRunning) {
						if (PluginProtocolActionDelegate.isProtocolRunning(handle)) {
							handlingLevel = 1;
							checkThreadRunning = false;
						}

						try {
							Thread.sleep(500);
						} catch (InterruptedException e) {
							// Do nothing.
						}
					}
				}
			};
			(new Thread(r)).start();
		}
	}

	private void restartServerAndClient(IAndroidEmulatorInstance instance, ProtocolHandle handle) {
		try {
			if (instance instanceof IAndroidLogicInstance) {
				IAndroidLogicInstance logicInstance = (IAndroidLogicInstance) instance;
				AbstractStartAndroidEmulatorLogic logic = logicInstance.getStartLogic();
				logic.execute(logicInstance, LogicMode.TRANSFER_AND_CONNECT_VNC, logicInstance.getTimeout(),
						new NullProgressMonitor());
				try {
					Thread.sleep(1500);
				} catch (InterruptedException e) {
					// Do nothing.
				}
				PluginProtocolActionDelegate.requestRestartProtocol(handle);
			} else {
				handlingLevel = 3;
			}
		} catch (Exception e1) {
			handlingLevel = 3;
		}
	}

	/**
	 * In this method, the user is asked whether to retry or not. While the user
	 * does not give up, the instance retries to connect to the emulator. When
	 * the user gives up, the instance is stopped.
	 * 
	 * @param handle
	 *            The object that identifies the protocol instance
	 */
	private boolean delegateDecisionToUser(ProtocolHandle handle) {
		boolean abort = true;
		error("Cannot reconnect to VM. Asking to the user if he/she wants to retry.");
		if (EclipseUtils.showQuestionDialog(EmulatorNLS.GEN_Question,
				EmulatorNLS.QUESTION_AndroidExceptionHandler_ImpossibleToReconnect)) {
			info("User chose to retry to reconnect to emulator VNC server.");
			PluginProtocolActionDelegate.requestRestartProtocol(handle);
			handlingLevel = 2;
			abort = false;
		}
		return abort;
	}

	/**
     *
     */
	private void abort(IAndroidEmulatorInstance instance) {
		info("User chose to stop the instance.");
		try {
			checkThreadRunning = false;
			instance.stop(true);
		} catch (InstanceStopException e1) {
			error("Error while running service for stopping virtual machine");
			EclipseUtils.showErrorDialog(EmulatorNLS.GEN_Error,
					EmulatorNLS.EXC_AndroidExceptionHandler_CannotRunStopService);
		}
	}

}

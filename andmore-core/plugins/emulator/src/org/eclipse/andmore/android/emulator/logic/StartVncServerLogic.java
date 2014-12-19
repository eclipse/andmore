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
import java.util.Collection;
import java.util.LinkedList;

import org.eclipse.andmore.android.DDMSFacade;
import org.eclipse.andmore.android.ISerialNumbered;
import org.eclipse.andmore.android.common.exception.AndroidException;
import org.eclipse.andmore.android.emulator.EmulatorPlugin;
import org.eclipse.andmore.android.emulator.core.exception.InstanceStartException;
import org.eclipse.andmore.android.emulator.core.exception.StartCancelledException;
import org.eclipse.andmore.android.emulator.core.exception.StartTimeoutException;
import org.eclipse.andmore.android.emulator.core.model.IAndroidEmulatorInstance;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;

/**
 * This class contains the logic to start the VNC server on the given Emulator.
 */
public final class StartVncServerLogic implements IAndroidLogic {
	public static final String VNC_SERVER_JOB_PREFIX = "VNC Server - ";

	public static final Object VNC_SERVER_JOB_FAMILY = new Object();

	/**
	 * Sequence of commands that must be executed on the emulator to start the
	 * VNC server
	 */
	private final Collection<String> remoteCommands = new LinkedList<String>();

	/**
	 * Collection of listeners for the job executing the VNC server.
	 */
	private final Collection<IJobChangeListener> listeners = new LinkedList<IJobChangeListener>();

	/**
	 * Executes the logic to start the vnc server.
	 */
	@Override
	public void execute(final IAndroidLogicInstance instance, int timeout, final IProgressMonitor monitor)
			throws InstanceStartException, StartTimeoutException, StartCancelledException, IOException {
		cancelCurrentVncServerJobs(instance);

		// Creates and starts a job that will keep running as long as the VNC
		// server is up on that Emulator instance.
		// add listeners that will receive notifications about the Job
		// life-cycle.
		VncServerJob vncServerJob = new VncServerJob(instance, getRemoteCommands());
		for (IJobChangeListener vncServerListener : listeners) {
			vncServerJob.addJobChangeListener(vncServerListener);
		}
		vncServerJob.schedule();

	}

	/**
	 * Cancel any VncServerJob that is currently running the VNC server on the
	 * given emulator instance.
	 * 
	 * @param instance
	 *            , the emulator instances where VNC server execution must be
	 *            canceled.
	 **/
	public static void cancelCurrentVncServerJobs(IAndroidEmulatorInstance instance) {
		// stop the previous VNC Server job for this instance if any...
		IJobManager manager = Job.getJobManager();
		Job[] allVncJobs = manager.find(StartVncServerLogic.VNC_SERVER_JOB_FAMILY);
		if (allVncJobs.length > 0) {
			for (Job job : allVncJobs) {
				if (job.getName().equals(StartVncServerLogic.VNC_SERVER_JOB_PREFIX + instance.getName())) {
					info("Cancel execution of the VNC Server on " + instance);
					job.cancel();
				}
			}
		}
	}

	/**
	 * Add job listener to receive state-change notifications from the job that
	 * runs the VNC Server.
	 * 
	 * @param vncServerListener
	 *            job listener that willl receive state change notifications
	 *            from the VNC Serever job.
	 */
	public void addVncServerJobListener(IJobChangeListener vncServerListener) {
		listeners.add(vncServerListener);
	}

	/**
	 * Add a command to be executed in the process of starting the VNC Server on
	 * the Emulator.
	 * 
	 * @param remoteCommand
	 */
	public void addRemoteCommand(String remoteCommand) {
		remoteCommands.add(remoteCommand);
	}

	/**
	 * Get the list of commands to be executed on the Emulator in order to start
	 * the VNC Server.
	 * 
	 * @return the sequence of commands that must be executed on the Emulator to
	 *         start the VNC Server.
	 */
	public Collection<String> getRemoteCommands() {
		return remoteCommands;
	}

}

/**
 * Job that executes the VNC Server. It will keep running as long as the VNC
 * Server process is running on the Emulator.
 */
class VncServerJob extends Job implements ISchedulingRule {
	private String serialNumber;

	/**
	 * Sequence of commands that must be executed on the emulator to start the
	 * VNC server
	 */
	private final Collection<String> remoteCommands;

	/**
	 * Creates a new job to execute the VNC server on the given emulator
	 * instance.
	 * 
	 * @param instance
	 *            , emulator instance where the VNC server will be started.
	 * @param remoteCommands
	 *            , sequence of commands that must be executed on the given
	 *            emulator instance to start the VNC Server.
	 * @throws InstanceStartException
	 */
	public VncServerJob(IAndroidLogicInstance instance, Collection<String> remoteCommands)
			throws InstanceStartException {
		super(StartVncServerLogic.VNC_SERVER_JOB_PREFIX + instance.getName());

		this.serialNumber = ((ISerialNumbered) instance).getSerialNumber();

		try {
			AndroidLogicUtils.testDeviceStatus(serialNumber);
		} catch (AndroidException e) {
			throw new InstanceStartException(e.getMessage());
		}

		this.remoteCommands = remoteCommands;
		setSystem(true);
		setRule(this);
	}

	/**
	 * @see org.eclipse.core.runtime.jobs.Job#run(IProgressMonitor)
	 */
	@Override
	public IStatus run(IProgressMonitor monitor) {
		IStatus status = Status.OK_STATUS;
		try {
			info("Executing VNC Server on " + serialNumber);
			AndroidLogicUtils.testDeviceStatus(serialNumber);
			DDMSFacade.execRemoteApp(serialNumber, remoteCommands, monitor);

			if (monitor.isCanceled()) {
				status = Status.CANCEL_STATUS;
			}
		} catch (Exception e) {
			String errorMessage = "Error while trying to run the VNC server on " + serialNumber;
			error(errorMessage + " " + e.getMessage());
			status = new Status(IStatus.CANCEL, EmulatorPlugin.PLUGIN_ID, errorMessage, e);
		}

		info("Finished the execution of the VNC Server on " + serialNumber + " with status " + status);

		return status;
	}

	/**
	 * @see org.eclipse.core.runtime.jobs.Job#belongsTo(Object)
	 */
	@Override
	public boolean belongsTo(Object family) {
		return StartVncServerLogic.VNC_SERVER_JOB_FAMILY.equals(family);
	}

	@Override
	public boolean contains(ISchedulingRule rule) {
		boolean contains = false;
		if (rule instanceof VncServerJob) {
			VncServerJob otherVncServerJob = (VncServerJob) rule;
			contains = otherVncServerJob.serialNumber.equals(serialNumber);
		}

		return contains;
	}

	@Override
	public boolean isConflicting(ISchedulingRule rule) {
		return contains(rule);
	}
}

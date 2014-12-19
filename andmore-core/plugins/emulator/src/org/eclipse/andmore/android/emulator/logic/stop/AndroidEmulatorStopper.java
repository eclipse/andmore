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
package org.eclipse.andmore.android.emulator.logic.stop;

import static org.eclipse.andmore.android.common.log.StudioLogger.error;
import static org.eclipse.andmore.android.common.log.StudioLogger.info;
import static org.eclipse.andmore.android.common.log.StudioLogger.warn;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.andmore.android.emulator.i18n.EmulatorNLS;
import org.eclipse.andmore.android.emulator.logic.AndroidLogicUtils;
import org.eclipse.andmore.android.emulator.logic.IAndroidLogicInstance;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.sequoyah.vnc.protocol.PluginProtocolActionDelegate;
import org.eclipse.sequoyah.vnc.protocol.lib.ProtocolHandle;
import org.eclipse.sequoyah.vnc.vncviewer.registry.VNCProtocolRegistry;

/**
 * DESCRIPTION: This class contains the business layer of the Android Emulator
 * stop procedure
 *
 * RESPONSIBILITY: Stop any Android Emulator
 *
 * COLABORATORS: None.
 *
 * USAGE: Use the public method to stop a Android Emulator
 */
public class AndroidEmulatorStopper {
	private static Lock lock = new ReentrantReadWriteLock().writeLock();

	private static final int MAX_LOCK_ATTEMPTS = 20;

	/**
	 * Stops this instance, removing its viewer from Android Emulator View
	 *
	 * @param instance
	 *            The device instance
	 * @param force
	 *            If true do not ask the user if he/she wants to proceed
	 * @param monitor
	 *            A progress monitor that will show the disposal action progress
	 *            at UI
	 * 
	 * @return True if the instance was stopped; false if the user chose not to
	 *         stop it
	 */
	public static boolean stopInstance(final IAndroidLogicInstance instance, boolean force, boolean kill,
			IProgressMonitor monitor) {

		if (instance == null) {
			error("Could not stop the protocol because the provided instance is null");
			return false;
		}

		boolean canProceed = true;

		// decision whether to actually stop or not comes from user
		if (!force) {

			canProceed = EclipseUtils.showQuestionDialog(EmulatorNLS.GEN_Question,
					NLS.bind(EmulatorNLS.QUESTION_AndroidEmulatorStopper_StopEmulatorQuestion, instance.getName()));

		}

		if (canProceed) {
			int attempts = 0;
			boolean locked = lock.tryLock();
			while (!locked && (attempts < MAX_LOCK_ATTEMPTS)) {
				try {
					Thread.sleep(125);
				} catch (InterruptedException e) {
					// Do nothing!
				}
				locked = lock.tryLock();
				attempts++;
			}

			if (locked) {
				if (monitor == null) {
					monitor = new NullProgressMonitor();
				}

				try {
					info("Stopping the Android Emulator instance");

					monitor.beginTask(EmulatorNLS.MON_AndroidEmulatorStopper_DisposingInstance, 200);
					monitor.setTaskName(EmulatorNLS.MON_AndroidEmulatorStopper_DisposingInstance);

					// Try to stop the protocol.
					//
					// This is not critical to the stop instance procedure, but
					// it is
					// desirable because the TmL's methods may do some cleanup
					// before returning. The loop below tries to stop for some
					// time (and
					// give enough time for cleanup as well), but if TmL does
					// not finish
					// in an acceptable time, the stop instance procedure
					// continues

					try {
						info("Trying to stop the protocol");
						// Try to implement a TmL independent code
						ProtocolHandle handle = instance.getProtocolHandle();
						if (handle != null) {
							PluginProtocolActionDelegate.requestStopProtocol(handle);

							while (PluginProtocolActionDelegate.isProtocolRunning(handle)) {
								Thread.sleep(250);
							}
							VNCProtocolRegistry.getInstance().unregister(handle);
							info("Protocol stopped");
						}
					} catch (Exception e) {
						error("There was an error while trying to stop the protocol");
					}

					// Try to implement a TmL independent code
					instance.setProtocolHandle(null);

					monitor.worked(100);
					monitor.setTaskName(EmulatorNLS.MON_AndroidEmulatorStopper_StopVm);

					if (kill) {
						AndroidLogicUtils.kill(instance);
					}

					info("Stopped the Android Emulator instance");
				} finally {
					monitor.done();
					try {
						lock.unlock();
					} catch (Exception e) {
						warn("The thread that is releasing the lock is not the one which has it.");
					}
				}
			} else {
				canProceed = false;
			}
		}

		return canProceed;
	}
}

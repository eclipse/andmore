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
package org.eclipse.andmore.android.emulator10;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;

import org.eclipse.andmore.android.SdkUtils;
import org.eclipse.andmore.android.common.utilities.PluginUtils;
import org.eclipse.andmore.android.emulator.EmulatorPlugin;
import org.eclipse.andmore.android.emulator.logic.AbstractStartAndroidEmulatorLogic;
import org.eclipse.andmore.android.emulator.logic.ConnectVncLogic;
import org.eclipse.andmore.android.emulator.logic.ForwardVncPortLogic;
import org.eclipse.andmore.android.emulator.logic.IAndroidLogic;
import org.eclipse.andmore.android.emulator.logic.IAndroidLogicInstance;
import org.eclipse.andmore.android.emulator.logic.StartEmulatorProcessLogic;
import org.eclipse.andmore.android.emulator.logic.StartVncServerLogic;
import org.eclipse.andmore.android.emulator.logic.TransferFilesLogic;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;

public class StartAndroidEmulatorLogic extends AbstractStartAndroidEmulatorLogic {
	@SuppressWarnings("incomplete-switch")
	@Override
	public Collection<IAndroidLogic> getLogicCollection(IAndroidLogicInstance instance, LogicMode mode) {
		// When starting, all steps must be done. When restarting, only VNC
		// server launch
		// step will be performed.
		Collection<IAndroidLogic> logicCollection = new LinkedList<IAndroidLogic>();

		switch (mode) {
		case START_MODE:
			logicCollection.add(new StartEmulatorProcessLogic());
			break;
		case TRANSFER_AND_CONNECT_VNC:
			logicCollection.add(createTransferFilesLogic());
			logicCollection.add(new ForwardVncPortLogic());
			StartVncServerLogic startVncServerLogic = createStartVncServerLogic();
			logicCollection.add(startVncServerLogic);
			logicCollection.add(getConnectVncClientLogic(startVncServerLogic));
			break;
		case RESTART_VNC_SERVER:
			logicCollection.add(createTransferFilesLogic());
			logicCollection.add(new ForwardVncPortLogic());
			logicCollection.add(createStartVncServerLogic());
			break;
		}

		return logicCollection;
	}

	private String getResourceDir() {
		String resDir = "res";
		if (SdkUtils.isOphoneSDK()) {
			resDir = "res_OPhone";
		}

		return resDir;
	}

	protected IAndroidLogic createTransferFilesLogic() {
		File localDirParent = PluginUtils.getPluginInstallationPath(EmulatorPlugin.getDefault());
		File localDir = new File(localDirParent, getResourceDir());

		TransferFilesLogic transferLogic = new TransferFilesLogic();
		transferLogic.setLocalDir(localDir.getAbsolutePath());
		transferLogic.setRemoteDir("/data/local");
		transferLogic.addFilename("fbvncserver");
		return transferLogic;
	}

	protected StartVncServerLogic createStartVncServerLogic() {
		StartVncServerLogic logic = new StartVncServerLogic();
		logic.addRemoteCommand("chmod 700 /data/local/fbvncserver");
		logic.addRemoteCommand("/data/local/fbvncserver");
		return logic;
	}

	protected IAndroidLogic getConnectVncClientLogic(StartVncServerLogic startVncServerLogic) {
		final ConnectVncLogic startVncClientLogic = new ConnectVncLogic();

		startVncServerLogic.addVncServerJobListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent ijobchangeevent) {
				startVncClientLogic.setVncServerDoneEvent(ijobchangeevent);
			}
		});

		return startVncClientLogic;
	}

}

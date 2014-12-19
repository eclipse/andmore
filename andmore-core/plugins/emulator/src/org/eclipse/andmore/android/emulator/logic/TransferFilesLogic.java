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

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;

import org.eclipse.andmore.android.DDMSFacade;
import org.eclipse.andmore.android.emulator.core.exception.InstanceStartException;
import org.eclipse.andmore.android.emulator.core.exception.StartCancelledException;
import org.eclipse.andmore.android.emulator.core.exception.StartTimeoutException;
import org.eclipse.andmore.android.emulator.i18n.EmulatorNLS;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

public class TransferFilesLogic implements IAndroidLogic {
	private String localDir = "";

	private String remoteDir = "";

	private final Collection<String> filenames = new LinkedList<String>();

	@Override
	public void execute(IAndroidLogicInstance instance, int timeout, IProgressMonitor monitor)
			throws InstanceStartException, StartCancelledException, StartTimeoutException, IOException {
		if ((instance != null) && (timeout > 0) && (localDir != null) && (!"".equals(localDir)) && (remoteDir != null)
				&& ("".equals(remoteDir))) {
			error("Cannot transfer files because the parameters provided are not as expected.");
			throw new InstanceStartException(EmulatorNLS.ERR_TransferFilesLogic_NotEnoughInformation);
		}

		String serialNumber = DDMSFacade.getSerialNumberByName(instance.getName());
		IStatus status = DDMSFacade.pushFiles(serialNumber, getLocalDir(), getFilenames(), getRemoteDir(), timeout,
				monitor, null);
		if (status.getSeverity() == IStatus.CANCEL) {
			throw new StartCancelledException();
		} else if (status.getSeverity() == IStatus.ERROR) {
			throw new InstanceStartException(status.getMessage());
		}
	}

	public void setLocalDir(String localDir) {
		this.localDir = localDir;
	}

	public String getLocalDir() {
		return localDir;
	}

	public void addFilename(String filename) {
		filenames.add(filename);
	}

	public Collection<String> getFilenames() {
		return filenames;
	}

	public void setRemoteDir(String remoteDir) {
		this.remoteDir = remoteDir;
	}

	public String getRemoteDir() {
		return remoteDir;
	}

}

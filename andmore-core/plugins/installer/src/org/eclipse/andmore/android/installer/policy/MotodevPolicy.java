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
package org.eclipse.andmore.android.installer.policy;

import java.io.File;

import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.andmore.android.common.utilities.FileUtil;
import org.eclipse.andmore.android.installer.i18n.InstallerNLS;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.internal.p2.ui.sdk.SDKPolicy;
import org.eclipse.equinox.p2.operations.ProfileChangeOperation;
import org.eclipse.swt.widgets.Shell;

/**
 * This class replaces the P2 Policy on MOTODEV Studio. This is responsible to
 * verify if writing on the current install directory is writable. A error
 * message is displayed if this occurs and the operation is aborted.
 */
@SuppressWarnings("restriction")
public class MotodevPolicy extends SDKPolicy {

	@Override
	public boolean continueWorkingWithOperation(ProfileChangeOperation operation, Shell shell) {
		boolean canContinue = super.continueWorkingWithOperation(operation, shell);

		if (canContinue) {
			// Check if it's possible to write on the current workbench location
			// If not... display a message and return false.

			String installLocation = Platform.getInstallLocation().getURL().getFile();
			File tmpFile = new File(installLocation + File.separator + "erase.me"); //$NON-NLS-1$
			boolean canWrite = false;
			canWrite = FileUtil.canWrite(tmpFile);

			if (!canWrite) {
				EclipseUtils.showErrorDialog(InstallerNLS.MotodevPolicy_Insufficient_Permissions_Title,
						InstallerNLS.MotodevPolicy_Insufficient_Permissions_Message);
				canContinue = false;
			}
		}

		return canContinue;
	}
}

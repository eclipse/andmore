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
package org.eclipse.andmore.android.devices;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.andmore.android.DDMSFacade;
import org.eclipse.andmore.android.DDMSUtils;
import org.eclipse.andmore.android.InstallPackageBean;
import org.eclipse.andmore.android.i18n.AndroidNLS;
import org.eclipse.andmore.android.wizards.installapp.DeployWizard.INSTALL_TYPE;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.sequoyah.device.framework.model.IDeviceTypeDropSupport;
import org.eclipse.sequoyah.device.framework.model.IInstance;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.ui.PlatformUI;

public abstract class AbstractDeviceDropSupportHandler implements IDeviceTypeDropSupport {

	private DropTargetEvent lastEvent = null;

	private List<File> availableAPKs = null;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.sequoyah.device.framework.model.IDeviceTypeDropSupport#canDrop
	 * (org.eclipse.sequoyah.device.framework.model.IInstance,
	 * org.eclipse.swt.dnd.TransferData, org.eclipse.swt.dnd.DropTargetEvent)
	 */
	@Override
	public boolean canDrop(IInstance instance, TransferData data, DropTargetEvent event) {
		return (((getFiles(event) != null) && (getFiles(event).size() > 0)) || !Platform.OS_WIN32.equals(Platform
				.getOS()));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.sequoyah.device.framework.model.IDeviceTypeDropSupport#drop
	 * (org.eclipse.sequoyah.device.framework.model.IInstance,
	 * org.eclipse.swt.dnd.TransferData, org.eclipse.swt.dnd.DropTargetEvent)
	 */
	@Override
	public void drop(final IInstance instance, TransferData data, DropTargetEvent event) {
		final List<File> files = getFiles(event);
		Job installApksJob = new Job(AndroidNLS.AbstractDeviceDropSupportHandler_InstallingApksJobName) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				final List<File> failedFiles = new ArrayList<File>();
				availableAPKs = null;
				if (files != null) {
					for (File apk : files) {
						IStatus installationStatus = installAPK(apk, instance);
						if (!installationStatus.isOK()) {
							failedFiles.add(apk);
						}
					}
				}

				if (failedFiles.size() > 0) {
					PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
						@Override
						public void run() {
							StringBuilder builder = new StringBuilder();

							builder.append(AndroidNLS.AbstractDeviceDropSupportHandler_ApplicationsFailed);
							builder.append("\n"); //$NON-NLS-1$
							for (File f : failedFiles) {
								builder.append("\n"); //$NON-NLS-1$
								builder.append(f.getName());
							}
							MessageDialog.openInformation(PlatformUI.getWorkbench().getActiveWorkbenchWindow()
									.getShell(), AndroidNLS.UI_InstallApp_InstallApp, builder.toString());

						}
					});
				}

				return Status.OK_STATUS;
			}
		};

		installApksJob.schedule();
		lastEvent = null;
	}

	private synchronized IStatus installAPK(File apk, final IInstance instance) {
		final InstallPackageBean installBean = new InstallPackageBean();
		installBean.setCanOverwrite(INSTALL_TYPE.UNINSTALL);
		installBean.setPackagePath(apk.getAbsolutePath());

		return DDMSUtils.installPackage(DDMSFacade.getSerialNumberByName(instance.getName()), installBean);

	}

	protected List<File> getFiles(DropTargetEvent event) {

		if ((lastEvent == null) || (lastEvent != event)) {
			lastEvent = event;
			availableAPKs = new ArrayList<File>();

			if (FileTransfer.getInstance().isSupportedType(event.currentDataType)) {
				String[] files = (String[]) FileTransfer.getInstance().nativeToJava(event.currentDataType);

				// On MacOSX the files are not available until drop
				if (files == null) {
					try {
						files = (String[]) event.data;
					} catch (Exception e) {
						// do nothing
						files = null;
					}
				}

				if (files != null) {

					for (int i = 0; (i < files.length); i++) {
						String filePath = files[i];
						if (filePath.toLowerCase().endsWith(".apk")) //$NON-NLS-1$
						{
							File f = new File(filePath);
							if (f.exists() && f.isFile() && f.canRead()) {
								availableAPKs.add(f);
							}
						}
					}
				}

			}

		}

		return availableAPKs;
	}
}

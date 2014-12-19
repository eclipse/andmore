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
package org.eclipse.andmore.android.installer.jobs;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.andmore.android.installer.InstallerException;
import org.eclipse.andmore.android.installer.InstallerPlugin;
import org.eclipse.andmore.android.installer.i18n.InstallerNLS;
import org.eclipse.andmore.android.installer.ui.dialogs.AcceptLicensesDialog;
import org.eclipse.andmore.android.installer.utilities.InstallManager;
import org.eclipse.andmore.android.installer.utilities.InstallableItem;
import org.eclipse.andmore.android.installer.utilities.IInstallManager.BACKEND;
import org.eclipse.andmore.android.installer.utilities.IInstallManager.CATEGORY;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.PlatformUI;

/**
 * This {@link Job} execute studio update
 */
public class UpdateStudioJob extends Job {
	private static final String STUDIO_UPDATE_SITE = "https://studio-android.motodevupdate.com/android/4.0/";

	private static Job updateJob;

	/**
	 * Constructor which receives data in order to install components.
	 * 
	 * @param name
	 *            Name of the {@link Job}.
	 * @param stageSites
	 *            Selected Stage Sites.
	 * @param itemsToInstall
	 *            Items to Install.
	 */
	private UpdateStudioJob(String name) {
		super(name);
	}

	/**
	 * Get the instance of the {@link UpdateStudioJob}. In case it has never
	 * been created by the method
	 * {@link UpdateStudioJob#createJob(String, List, Collection, MultiStatus, Map)}
	 * , <code>null</code> will be returned.
	 * 
	 * @return Returns the instance of {@link UpdateStudioJob}.
	 */
	public static Job getInstance() {
		return updateJob;
	}

	/**
	 * Create a {@link UpdateStudioJob} job instance for installing components
	 * from the "Download Components".
	 * 
	 * @param name
	 *            Name of the {@link Job}.
	 * @param stageSites
	 *            Selected Stage Sites.
	 * @param itemsToInstall
	 *            Items to Install.
	 * @param finishStatus
	 *            Status to be merged with this operation.
	 * @param pages
	 *            Pages which hold the tasks that origins the installation.
	 * @param configurationDialog
	 *            The Page which started this job.
	 */
	public static Job createJob(String name) {
		updateJob = new UpdateStudioJob(name);

		return updateJob;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.
	 * IProgressMonitor)
	 */
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		InstallManager installManager = (InstallManager) InstallManager.getInstance();
		IStatus status = null;

		SubMonitor submonitor = SubMonitor.convert(monitor);
		submonitor.beginTask(InstallerNLS.UpdateStudio_LoadingRepositories, 100);

		Collection<InstallableItem> itemsToInstall = new ArrayList<InstallableItem>();
		List<URI> updateSites = new ArrayList<URI>();
		updateSites.add(URI.create(STUDIO_UPDATE_SITE));

		try {
			status = installManager.listAllAvailableUpdates(itemsToInstall, updateSites, CATEGORY.UPDATE_STUDIO,
					BACKEND.P2, submonitor.newChild(40));

			if (itemsToInstall.size() == 0) {
				StudioLogger.info(this.getClass(), "listAvailable updates returned an empty list"); //$NON-NLS-1$
			}

			if (!status.isOK()) {
				StudioLogger.info(this.getClass(), "Error listing available updates " + status.getMessage()); //$NON-NLS-1$
				if (status.getSeverity() == IStatus.INFO) {
					EclipseUtils.showInformationDialog(InstallerNLS.UpdateStudio_AlreadyUpdatedInformationDialogTitle,
							InstallerNLS.UpdateStudio_AlreadyUpdatedInformationDialogText);
				} else if (status.getSeverity() != IStatus.CANCEL) {
					EclipseUtils.showErrorDialog(InstallerNLS.UpdateStudio_UpdateErrorTitle,
							InstallerNLS.UpdateStudioJob_UpdateErrorMessage, status);
					status = Status.CANCEL_STATUS;
				}
			} else {

				final InstallableItem[] itemsToInstallArray = new InstallableItem[itemsToInstall.size()];
				int index = 0;
				for (InstallableItem item : itemsToInstall) {
					itemsToInstallArray[index] = item;
					index++;
				}

				final int[] result = new int[1];
				if (!monitor.isCanceled()) {
					PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
						@Override
						public void run() {
							AcceptLicensesDialog licenceDialog = new AcceptLicensesDialog(PlatformUI.getWorkbench()
									.getDisplay().getActiveShell(), itemsToInstallArray, true, true);

							result[0] = licenceDialog.open();
						}
					});
				}

				if (!monitor.isCanceled()) {
					if (result[0] == Window.OK) {

						if (status.isOK() && (itemsToInstallArray.length > 0)) {
							submonitor.setTaskName(InstallerNLS.UpdateStudio_UpdatingStudioJobDescription);
							status = installManager.updateStudio(updateSites, BACKEND.P2, submonitor.newChild(60));

							if (updateSites.isEmpty()) {
								StudioLogger.info(this.getClass(),
										"Tryed to update from Studio but updateSites[] is empty. Status message = " //$NON-NLS-1$
												+ status.getMessage());
							} else {
								StudioLogger.info(this.getClass(), "Tryed to update from [" + updateSites.toString() //$NON-NLS-1$
										+ "]. Status message = " + status.getMessage()); //$NON-NLS-1$
							}

						}

						if (status.isOK() && !monitor.isCanceled()) {
							if (itemsToInstallArray.length > 0) {
								boolean restart = EclipseUtils.showQuestionDialog(
										InstallerNLS.UpdateStudio_MSG_RESTART_TITLE,
										InstallerNLS.UpdateStudio_MSG_RESTART_MESSAGE);
								if (restart) {
									PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
										@Override
										public void run() {
											PlatformUI.getWorkbench().restart();
										}
									});

								}
							}

						} else if (monitor.isCanceled()) {
							StudioLogger
									.info(this.getClass(), "Setting status to CANCEL since monitor was canceled 1."); //$NON-NLS-1$
							status = Status.CANCEL_STATUS;
						} else if (status.getSeverity() == IStatus.INFO) {
							EclipseUtils.showInformationDialog(
									InstallerNLS.UpdateStudio_AlreadyUpdatedInformationDialogTitle,
									InstallerNLS.UpdateStudio_AlreadyUpdatedInformationDialogText);
						} else {
							EclipseUtils.showErrorDialog(InstallerNLS.UpdateStudio_UpdateErrorTitle,
									InstallerNLS.UpdateStudioJob_UpdateErrorMessage, status);
							status = Status.CANCEL_STATUS;
							StudioLogger.info(this.getClass(), "Setting status to CANCEL 2."); //$NON-NLS-1$
						}
					}
				}

				if (monitor.isCanceled()) {
					StudioLogger.info(this.getClass(), "Setting status to CANCEL since monitor was canceled 3."); //$NON-NLS-1$
					status = Status.CANCEL_STATUS;
				}
			}
		} catch (InstallerException e) {
			StudioLogger.error(this.getClass(), "Error when retrieving installable units for update", e); //$NON-NLS-1$
			status = new Status(IStatus.ERROR, InstallerPlugin.PLUGIN_ID, 0, InstallerNLS.UpdateStudio_UpdateErrorText,
					null);
		}

		if (!status.isOK()) {
			StudioLogger.info(this.getClass(), "Update Studio job exiting with status different from ok: " //$NON-NLS-1$
					+ status.getMessage());
		}

		return status;
	}
}

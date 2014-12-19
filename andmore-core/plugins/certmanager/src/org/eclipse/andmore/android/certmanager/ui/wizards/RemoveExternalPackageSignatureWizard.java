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
package org.eclipse.andmore.android.certmanager.ui.wizards;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

import org.eclipse.andmore.android.certmanager.CertificateManagerActivator;
import org.eclipse.andmore.android.certmanager.i18n.CertificateManagerNLS;
import org.eclipse.andmore.android.certmanager.packaging.PackageFile;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * This Wizard removes a signature of a package. based on a root dir, It shows a
 * list of packages to remove signature
 */
public class RemoveExternalPackageSignatureWizard extends Wizard {
	private RemoveExternalPackageSignaturePage page = null;

	public RemoveExternalPackageSignatureWizard(IStructuredSelection selection) {
		setWindowTitle(CertificateManagerNLS.UNSIGN_EXTERNAL_PKG_WIZARD_WINDOW_TITLE);
		setNeedsProgressMonitor(true);
		this.page = new RemoveExternalPackageSignaturePage("removeSigPage", selection);
		setDefaultPageImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(CertificateManagerActivator.PLUGIN_ID,
				CertificateManagerActivator.REMOVE_SIGNATURE_WIZ_BAN));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	@Override
	public void addPages() {
		addPage(this.page);
	}

	/**
	 * Finishes this wizard removing packages signatures
	 */
	@Override
	public boolean performFinish() {
		final List<String> defectivePackages = new ArrayList<String>();
		IRunnableWithProgress finishAction = new IRunnableWithProgress() {

			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				List<String> selectedFiles = RemoveExternalPackageSignatureWizard.this.page.getSelectedPackages();
				monitor.beginTask(CertificateManagerNLS.UNSIGN_EXTERNAL_PKG_WIZARD_WINDOW_TITLE, selectedFiles.size());
				for (String selected : selectedFiles) {
					File file = new File(selected);
					monitor.setTaskName(CertificateManagerNLS.UNSIGN_EXTERNAL_PKG_WIZARD_OPERATION + " "
							+ file.getName());
					if ((file != null) && file.exists() && file.isFile() && file.canWrite()) {
						OutputStream fileToWrite = null;
						JarFile jar = null;
						PackageFile pack = null;
						try {
							// Open package and remove signature
							jar = new JarFile(file);
							pack = new PackageFile(jar);
							try {
								pack.removeMetaEntryFiles();
							} catch (IOException e) {
								StudioLogger.error(RemoveExternalPackageSignatureWizard.class.toString(),
										"Impossible to delete temporary files");
								throw e;
							}

							// Write the new package file
							fileToWrite = new FileOutputStream(file);
							pack.write(fileToWrite);
							PackageFile.zipAlign(file);
						} catch (IOException e) {
							defectivePackages.add(selected);
							StudioLogger.error(RemoveExternalPackageSignatureWizard.class.toString(),
									"Impossible write to package: " + selected + " " + e.getMessage());
						} catch (SecurityException e) {
							defectivePackages.add(selected);
							StudioLogger.error(RemoveExternalPackageSignatureWizard.class.toString(),
									"Impossible write to package: " + selected + " " + e.getMessage());
						} finally {

							System.gc(); // Force garbage collector to avoid
							// errors when deleting temp files

							try {
								if (jar != null) {
									jar.close();
								}

								if (pack != null) {
									pack.removeTemporaryEntryFiles();
								}

								if (fileToWrite != null) {
									fileToWrite.close();
								}
							} catch (IOException e) {
								// Silent exception. Only log the deletion
								// exception.
								StudioLogger.error(CertificateManagerActivator.PLUGIN_ID, "Deleting temporary files");
							}
						}
					} else {
						defectivePackages.add(selected);
					}
					monitor.worked(1);
				}
				monitor.done();
			}

		};

		try {
			PlatformUI.getWorkbench().getProgressService()
					.runInUI(new ProgressMonitorDialog(getShell()), finishAction, null);
		} catch (InvocationTargetException e1) {
			StudioLogger.error(RemoveExternalPackageSignatureWizard.class.toString(), "Error running finish actions");
		} catch (InterruptedException e1) {
			StudioLogger.error(RemoveExternalPackageSignatureWizard.class.toString(), "Error running finish actions");
		}

		if (ResourcesPlugin.getWorkspace().getRoot().getLocation().isPrefixOf(this.page.getSourcePath())) {
			org.eclipse.ui.actions.WorkspaceModifyOperation op = new org.eclipse.ui.actions.WorkspaceModifyOperation() {

				@Override
				protected void execute(IProgressMonitor monitor) throws CoreException, InvocationTargetException,
						InterruptedException {
					for (IContainer container : ResourcesPlugin.getWorkspace().getRoot()
							.findContainersForLocation(RemoveExternalPackageSignatureWizard.this.page.getSourcePath())) {

						container.refreshLocal(IResource.DEPTH_INFINITE, monitor);
					}

				}

			};
			try {
				PlatformUI.getWorkbench().getProgressService().run(false, false, op);
			} catch (InvocationTargetException e) {
				StudioLogger.error(RemoveExternalPackageSignatureWizard.class.toString(), "Error refreshing workspace");
			} catch (InterruptedException e) {
				StudioLogger.error(RemoveExternalPackageSignatureWizard.class.toString(), "Error refreshing workspace");
			}
		}

		if (!defectivePackages.isEmpty()) {
			MultiStatus errors = new MultiStatus(CertificateManagerActivator.PLUGIN_ID, IStatus.ERROR,
					CertificateManagerNLS.UNSIGN_EXTERNAL_PKG_WIZARD_ERROR_REASON, null);
			for (String defect : defectivePackages) {
				errors.add(new Status(IStatus.ERROR, CertificateManagerActivator.PLUGIN_ID, defect));
			}

			ErrorDialog errorBox = new ErrorDialog(getShell(),
					CertificateManagerNLS.UNSIGN_EXTERNAL_PKG_WIZARD_WINDOW_TITLE,
					CertificateManagerNLS.UNSIGN_EXTERNAL_PKG_WIZARD_ERROR, errors, IStatus.ERROR);
			errorBox.open();
		}

		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.wizard.Wizard#createPageControls(org.eclipse.swt.widgets
	 * .Composite)
	 */
	@Override
	public void createPageControls(Composite pageContainer) {
		super.createPageControls(pageContainer);
		PlatformUI.getWorkbench().getHelpSystem()
				.setHelp(getShell(), CertificateManagerActivator.UNSIGN_EXTERNAL_PKG_WIZARD_CONTEXT_HELP_ID);
	}
}

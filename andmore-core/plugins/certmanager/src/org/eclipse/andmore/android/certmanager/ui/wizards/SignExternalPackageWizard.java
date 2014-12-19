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
import java.security.UnrecoverableKeyException;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

import org.eclipse.andmore.android.certmanager.CertificateManagerActivator;
import org.eclipse.andmore.android.certmanager.i18n.CertificateManagerNLS;
import org.eclipse.andmore.android.certmanager.packaging.PackageFile;
import org.eclipse.andmore.android.certmanager.packaging.sign.PackageFileSigner;
import org.eclipse.andmore.android.certmanager.packaging.sign.SignException;
import org.eclipse.andmore.android.certmanager.ui.model.IKeyStore;
import org.eclipse.andmore.android.certmanager.ui.model.IKeyStoreEntry;
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
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * This Wizard signs a package. based on a root dir, It shows a list o packages
 * to sign and let user choose a certificate to use
 */
public class SignExternalPackageWizard extends Wizard {
	private SignExternalPackagePage page = null;

	public SignExternalPackageWizard(IStructuredSelection selection, IKeyStore selectedKeyStore) {
		this(selection, selectedKeyStore, null);
	}

	public SignExternalPackageWizard(IStructuredSelection selection, IKeyStore selectedKeyStore,
			IKeyStoreEntry selectedEntry) {
		setWindowTitle(CertificateManagerNLS.SIGN_EXTERNAL_PKG_WIZARD_WINDOW_TITLE);
		setNeedsProgressMonitor(true);
		setHelpAvailable(false);
		this.page = new SignExternalPackagePage("signPage", selection, selectedKeyStore, selectedEntry);
		setDefaultPageImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(CertificateManagerActivator.PLUGIN_ID,
				CertificateManagerActivator.SIGNATURE_WIZ_BAN));
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
	 * Finishes this wizard, signing the selected packages
	 */
	@Override
	public boolean performFinish() {
		final List<String> defectivePackages = new ArrayList<String>();
		IRunnableWithProgress finishAction = new IRunnableWithProgress() {

			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				List<String> selectedFiles = SignExternalPackageWizard.this.page.getSelectedPackages();
				monitor.beginTask(CertificateManagerNLS.SIGN_EXTERNAL_PKG_WIZARD_WINDOW_TITLE, selectedFiles.size() * 2);
				for (String selected : selectedFiles) {
					File file = new File(selected);
					monitor.setTaskName(CertificateManagerNLS.SIGN_EXTERNAL_PKG_WIZARD_OPERATION + " " + file.getName());
					if ((file != null) && file.exists() && file.isFile() && file.canWrite()) {
						OutputStream fileToWrite = null;
						PackageFile pack = null;
						JarFile jar = null;
						try {

							// Update monitor
							monitor.worked(1);
							monitor.setTaskName(CertificateManagerNLS.SIGN_EXTERNAL_PKG_WIZARD_OPERATION + " "
									+ file.getName());

							String keyStorePassword = SignExternalPackageWizard.this.page.getKeystorePassword();
							if (SignExternalPackageWizard.this.page.getSavePasswordSelection()) {
								SignExternalPackageWizard.this.page.getPasswordProvider().saveKeyStorePassword(
										keyStorePassword);
							}
							String keyEntryPassword = SignExternalPackageWizard.this.page.getKeyEntryPassword();
							boolean keepTrying;
							if (keyEntryPassword != null) {
								keepTrying = true;
							} else {
								keepTrying = false;
								throw new Exception();
							}
							while (keepTrying) {
								try {
									// Open package and remove signature
									jar = new JarFile(file);
									pack = new PackageFile(jar);
									pack.removeMetaEntryFiles();

									// Sign the new package
									PackageFileSigner.signPackage(pack,
											SignExternalPackageWizard.this.page.getSelectedKeyEntry(),
											keyEntryPassword, PackageFileSigner.MOTODEV_STUDIO);
									keepTrying = false;
								} catch (UnrecoverableKeyException sE) {
									keyEntryPassword = SignExternalPackageWizard.this.page.getPasswordProvider()
											.getPassword(
													SignExternalPackageWizard.this.page.getSelectedKeyEntry()
															.getAlias(), true, false);
									if (keyEntryPassword == null) {
										keepTrying = false;
									} else {
										keepTrying = true;
									}
								}
							}

							// Write the new package file
							fileToWrite = new FileOutputStream(file);
							pack.write(fileToWrite);
							PackageFile.zipAlign(file);
						} catch (IOException e) {
							defectivePackages.add(selected);
							StudioLogger.error(SignExternalPackageWizard.class.toString(),
									"Impossible write to package: " + selected + " " + e.getMessage());
						} catch (SignException e) {
							defectivePackages.add(selected);
							StudioLogger.error(SignExternalPackageWizard.class.toString(),
									"Impossible sign the package: " + selected + " " + e.getMessage());
						} catch (SecurityException e) {
							defectivePackages.add(selected);
							StudioLogger.error(SignExternalPackageWizard.class.toString(),
									"Impossible sign the package: " + selected + " " + e.getMessage());
						} catch (Exception e) {
							defectivePackages.add(selected);
							StudioLogger.error(SignExternalPackageWizard.class.toString(),
									"Impossible sign the package: " + selected + " " + e.getMessage());
						} finally {
							System.gc(); // Force garbage collector to avoid
							// errors when deleting temp files

							try {
								if (pack != null) {
									pack.removeTemporaryEntryFiles();
								}

								if (fileToWrite != null) {
									fileToWrite.close();
								}

								if (jar != null) {
									jar.close();
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
			StudioLogger.error(SignExternalPackageWizard.class.toString(), "Error running finish actions");
		} catch (InterruptedException e1) {
			StudioLogger.error(SignExternalPackageWizard.class.toString(), "Error running finish actions");
		}

		if (ResourcesPlugin.getWorkspace().getRoot().getLocation().isPrefixOf(this.page.getSourcePath())) {
			WorkspaceModifyOperation op = new WorkspaceModifyOperation() {

				@Override
				protected void execute(IProgressMonitor monitor) throws CoreException, InvocationTargetException,
						InterruptedException {
					for (IContainer container : ResourcesPlugin.getWorkspace().getRoot()
							.findContainersForLocation(SignExternalPackageWizard.this.page.getSourcePath())) {

						container.refreshLocal(IResource.DEPTH_INFINITE, monitor);
					}

				}

			};
			try {
				PlatformUI.getWorkbench().getProgressService().run(false, false, op);
			} catch (InvocationTargetException e) {
				StudioLogger.error(SignExternalPackageWizard.class.toString(), "Error refreshing workspace");
			} catch (InterruptedException e) {
				StudioLogger.error(SignExternalPackageWizard.class.toString(), "Error refreshing workspace");
			}
		}
		if (!defectivePackages.isEmpty()) {
			MultiStatus errors = new MultiStatus(CertificateManagerActivator.PLUGIN_ID, IStatus.ERROR,
					CertificateManagerNLS.UNSIGN_EXTERNAL_PKG_WIZARD_ERROR_REASON, null);
			for (String defect : defectivePackages) {
				errors.add(new Status(IStatus.ERROR, CertificateManagerActivator.PLUGIN_ID, defect));
			}

			ErrorDialog errorBox = new ErrorDialog(getShell(),
					CertificateManagerNLS.SIGN_EXTERNAL_PKG_WIZARD_WINDOW_TITLE,
					CertificateManagerNLS.SIGN_EXTERNAL_PKG_WIZARD_ERROR, errors, IStatus.ERROR);
			errorBox.open();
		}
		return true;

	}

	@Override
	public void createPageControls(Composite pageContainer) {
		super.createPageControls(pageContainer);
		PlatformUI.getWorkbench().getHelpSystem()
				.setHelp(getShell(), CertificateManagerActivator.SIGN_EXTERNAL_PKG_WIZARD_CONTEXT_HELP_ID);
	}
}

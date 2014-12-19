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
package org.eclipse.andmore.android.certmanager.ui.dialogs;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.andmore.android.certmanager.CertificateManagerActivator;
import org.eclipse.andmore.android.certmanager.command.BackupHandler;
import org.eclipse.andmore.android.certmanager.i18n.CertificateManagerNLS;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class RestoreBackupDialog extends TitleAreaDialog {

	private static final String ZIP_EXT = "*.zip"; //$NON-NLS-1$

	private static final String WIZARD_BANNER = "icons/wizban/restore_keystore_wiz.png"; //$NON-NLS-1$

	public static final String RESTORE_KEYSTORE_HELP_ID = CertificateManagerActivator.PLUGIN_ID + ".restore_keystore"; //$NON-NLS-1$

	private final IContentProvider contentProvider;

	private final IBaseLabelProvider labelProvider;

	private CheckboxTableViewer tableViewer;

	private final String title;

	private File archiveFile;

	private List<String> selectedKeyStores;

	private File destinationFile;

	private Button selectAllButton;

	private String destinationPath = ""; //$NON-NLS-1$

	public RestoreBackupDialog(Shell parentShell) {
		super(parentShell);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		this.contentProvider = new BackupContentProvider();
		this.labelProvider = new BackupLabelProvider();
		this.title = CertificateManagerNLS.RestoreBackupDialog_Dialog_Title;
		selectedKeyStores = new ArrayList<String>();
		setTitleImage(AbstractUIPlugin.imageDescriptorFromPlugin(CertificateManagerActivator.PLUGIN_ID, WIZARD_BANNER)
				.createImage());
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		getShell().setText(title);
		setTitle(CertificateManagerNLS.RestoreBackupDialog_TitleArea_Message);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(getShell(), RestoreBackupDialog.RESTORE_KEYSTORE_HELP_ID);

		Composite dialogArea = new Composite(parent, SWT.FILL);
		dialogArea.setLayout(new GridLayout());
		dialogArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Group pathGroup = new Group(dialogArea, SWT.SHADOW_NONE);
		pathGroup.setText(CertificateManagerNLS.RestoreBackupDialog_Path_Group);
		pathGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		pathGroup.setLayout(new GridLayout(3, false));

		Label pathLabel = new Label(pathGroup, SWT.NONE);
		pathLabel.setText(CertificateManagerNLS.RestoreBackupDialog_BackUp_File);
		pathLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

		final Text pathText = new Text(pathGroup, SWT.BORDER);
		pathText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		pathText.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent e) {
				archiveFile = new File(pathText.getText());
				selectAllButton.setSelection(false);
				validate();
				loadArchiveEntries();
			}
		});

		Button browseButton = new Button(pathGroup, SWT.PUSH);
		browseButton.setText(CertificateManagerNLS.RestoreBackupDialog_Browse_Button);
		browseButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		browseButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog fileDialog = new FileDialog(getShell());
				fileDialog.setFilterExtensions(new String[] { ZIP_EXT });
				fileDialog.setOverwrite(false);
				String choosenPath = fileDialog.open();
				pathText.setText(choosenPath);
				super.widgetSelected(e);
			}
		});

		Label destinPath = new Label(pathGroup, SWT.NONE);
		destinPath.setText(CertificateManagerNLS.RestoreBackupDialog_Destination);
		destinPath.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

		final Text destinText = new Text(pathGroup, SWT.BORDER);
		destinText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		destinText.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent e) {
				destinationPath = destinText.getText();
				destinationFile = new File(destinationPath);
				validate();
			}
		});

		Button destinBrowseButton = new Button(pathGroup, SWT.PUSH);
		destinBrowseButton.setText(CertificateManagerNLS.RestoreBackupDialog_Browse_Button);
		destinBrowseButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		destinBrowseButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog directoryDialog = new DirectoryDialog(getShell());
				String choosenPath = directoryDialog.open();
				destinText.setText(choosenPath);
				super.widgetSelected(e);
			}
		});

		Group keystoresGroup = new Group(dialogArea, SWT.SHADOW_NONE);
		keystoresGroup.setText(CertificateManagerNLS.RestoreBackupDialog_KeyStores);
		keystoresGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		keystoresGroup.setLayout(new GridLayout(1, false));

		tableViewer = CheckboxTableViewer.newCheckList(keystoresGroup, SWT.H_SCROLL | SWT.V_SCROLL | SWT.CHECK
				| SWT.BORDER);
		tableViewer.setContentProvider(contentProvider);
		tableViewer.setLabelProvider(labelProvider);
		GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
		layoutData.widthHint = 400;
		layoutData.heightHint = 200;
		tableViewer.getControl().setLayoutData(layoutData);
		tableViewer.addCheckStateListener(new ICheckStateListener() {
			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				String keyStore = null;
				Object element = event.getElement();
				if (element instanceof String) {
					keyStore = (String) element;
				}

				if (keyStore != null) {
					if (event.getChecked()) {
						selectedKeyStores.add(keyStore);
					} else {
						selectedKeyStores.remove(keyStore);
					}
				}

				if (tableViewer.getCheckedElements().length == tableViewer.getTable().getItems().length) {
					selectAllButton.setSelection(true);
				} else {
					selectAllButton.setSelection(false);
				}

				validate();
			}
		});

		Composite selectButtonArea = new Composite(keystoresGroup, SWT.NONE);
		selectButtonArea.setLayout(new GridLayout(1, true));
		selectButtonArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		selectAllButton = new Button(selectButtonArea, SWT.CHECK);
		selectAllButton.setText(CertificateManagerNLS.RestoreBackupDialog_Select_All);
		selectAllButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
		selectAllButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				tableViewer.setAllChecked(selectAllButton.getSelection());
				selectedKeyStores.clear();
				for (Object element : tableViewer.getCheckedElements()) {
					String keyStoreEl = (String) element;
					selectedKeyStores.add(keyStoreEl);
				}
				validate();
				super.widgetSelected(e);
			}
		});

		setMessage(CertificateManagerNLS.RestoreBackupDialog_Default_Message);
		return dialogArea;
	}

	protected void loadArchiveEntries() {
		Runnable loadRunnable = new Runnable() {
			@Override
			public void run() {
				if (archiveFile.exists()) {
					@SuppressWarnings("rawtypes")
					final List[] holderArray = new List[1];

					ZipFile zipFile = null;
					try {
						zipFile = new ZipFile(archiveFile, ZipFile.OPEN_READ);
						ArrayList<String> keyStores = new ArrayList<String>(zipFile.size());
						holderArray[0] = keyStores;
						Enumeration<? extends ZipEntry> entries = zipFile.entries();
						while (entries.hasMoreElements()) {
							ZipEntry zipEntry = entries.nextElement();
							if (!zipEntry.getName().equalsIgnoreCase(BackupHandler.KS_TYPES_FILENAME)) {
								keyStores.add(zipEntry.getName());
							}
						}
						Display.getDefault().syncExec(new Runnable() {
							@Override
							public void run() {
								tableViewer.setInput(holderArray[0]);
							}
						});
					} catch (Exception e) {
						Display.getDefault().asyncExec(new Runnable() {
							@Override
							public void run() {
								clearKeystoresTableViewer();
								setErrorMessage(NLS.bind(
										CertificateManagerNLS.RestoreBackupDialog_Error_Loading_Entries, archiveFile));
								getButton(OK).setEnabled(false);
							}
						});
					} finally {
						if (zipFile != null) {
							try {
								zipFile.close();
							} catch (IOException e) {
								StudioLogger.error("Could not close stream while restoring backup. " + e.getMessage());
							}
						}
					}
				}
			}
		};

		Thread thread = new Thread(loadRunnable);
		thread.start();
	}

	@Override
	protected Control createButtonBar(Composite parent) {
		Control buttonBar = super.createButtonBar(parent);
		getButton(OK).setEnabled(false);
		return buttonBar;
	}

	private void validate() {

		boolean isValid = true;

		if (!archiveFile.exists()) {
			clearKeystoresTableViewer();
			setErrorMessage(CertificateManagerNLS.RestoreBackupDialog_BackUpFile_Not_Exist);
			isValid = false;
		} else {
			setErrorMessage(null);
			isValid = true;
		}

		if (isValid) {
			if (destinationPath.isEmpty()) {
				setErrorMessage(CertificateManagerNLS.RestoreBackupDialog_Invalid_Dest_Path);
				isValid = false;
			} else {
				if ((destinationFile != null) && destinationFile.isFile()) {
					setErrorMessage(CertificateManagerNLS.RestoreBackupDialog_Invalid_Dest_Path);
					isValid = false;
				} else {
					setErrorMessage(null);
					isValid = true;
				}
			}
		}

		if (isValid) {
			if (selectedKeyStores.isEmpty()) {
				setErrorMessage(CertificateManagerNLS.RestoreBackupDialog_Select_KeyStore);
				isValid = false;
			} else {
				setErrorMessage(null);
				isValid = true;
			}
		}

		getButton(OK).setEnabled(isValid);
	}

	/**
	 * Remove all entries from keystores table viewer.
	 * */
	private void clearKeystoresTableViewer() {
		tableViewer.setInput(null);
	}

	/**
	 * @return The back archive file
	 */
	public File getArchiveFile() {
		return archiveFile;
	}

	/**
	 * @return The keystores to be restored
	 */
	public List<String> getSelectedKeyStores() {
		return selectedKeyStores;
	}

	/**
	 * @return The destination directory
	 */
	public File getDestinationDir() {
		return destinationFile;
	}

}
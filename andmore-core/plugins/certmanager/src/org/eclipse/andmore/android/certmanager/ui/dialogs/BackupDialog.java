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
import java.util.List;

import org.eclipse.andmore.android.certmanager.CertificateManagerActivator;
import org.eclipse.andmore.android.certmanager.i18n.CertificateManagerNLS;
import org.eclipse.andmore.android.certmanager.ui.model.IKeyStore;
import org.eclipse.andmore.android.certmanager.ui.model.ITreeNode;
import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.andmore.android.common.utilities.FileUtil;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IMessageProvider;
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
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class BackupDialog extends TitleAreaDialog {

	private static final String ZIP_EXT = ".zip"; //$NON-NLS-1$

	private static final String WIZARD_BANNER = "icons/wizban/backup_keystore_wiz.png"; //$NON-NLS-1$

	private static final String BACKUP_KEYSTORE_HELP_ID = CertificateManagerActivator.PLUGIN_ID + ".backup_keystore"; //$NON-NLS-1$;

	private final IContentProvider contentProvider;

	private final IBaseLabelProvider labelProvider;

	private CheckboxTableViewer tableViewer;

	private Object input;

	private final String title;

	private File archiveFile;

	private List<String> selectedKeyStores;

	private String archivePath;

	private boolean verifyOverwrite;

	private Button selectAllButton;

	public BackupDialog(Shell parentShell) {
		super(parentShell);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		this.contentProvider = new BackupContentProvider();
		this.labelProvider = new BackupLabelProvider();
		this.title = CertificateManagerNLS.BackupDialog_Diag_Title;
		this.archivePath = ""; //$NON-NLS-1$
		selectedKeyStores = new ArrayList<String>();
		setTitleImage(AbstractUIPlugin.imageDescriptorFromPlugin(CertificateManagerActivator.PLUGIN_ID, WIZARD_BANNER)
				.createImage());
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		getShell().setText(title);
		setTitle(CertificateManagerNLS.BackupDialog_DialogTitle);
		// the shell has the same help as its page
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getShell(), BackupDialog.BACKUP_KEYSTORE_HELP_ID);

		Composite dialogArea = new Composite(parent, SWT.FILL);
		dialogArea.setLayout(new GridLayout());
		dialogArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Group pathGroup = new Group(dialogArea, SWT.SHADOW_NONE);
		pathGroup.setText(CertificateManagerNLS.BackupDialog_Backup_File);
		pathGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		pathGroup.setLayout(new GridLayout(3, false));

		Label pathLabel = new Label(pathGroup, SWT.NONE);
		pathLabel.setText(CertificateManagerNLS.BackupDialog_Path);
		pathLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

		final Text pathText = new Text(pathGroup, SWT.BORDER);
		pathText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		pathText.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent e) {
				archivePath = pathText.getText();
				archiveFile = new File(archivePath);
				verifyOverwrite = true;
				validate();
			}
		});

		Button browseButton = new Button(pathGroup, SWT.PUSH);
		browseButton.setText(CertificateManagerNLS.BackupDialog_Browse);
		browseButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		browseButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog fileDialog = new FileDialog(getShell(), SWT.SAVE);
				fileDialog.setFilterExtensions(new String[] { "*" + ZIP_EXT //$NON-NLS-1$
				});
				fileDialog.setOverwrite(true);
				String choosenPath = fileDialog.open();
				pathText.setText(choosenPath);
				verifyOverwrite = false;
				super.widgetSelected(e);
			}
		});

		Group keystoresGroup = new Group(dialogArea, SWT.SHADOW_NONE);
		keystoresGroup.setText(CertificateManagerNLS.BackupDialog_KeyStores);
		keystoresGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		keystoresGroup.setLayout(new GridLayout(1, false));

		tableViewer = CheckboxTableViewer.newCheckList(keystoresGroup, SWT.H_SCROLL | SWT.V_SCROLL | SWT.CHECK
				| SWT.BORDER);
		tableViewer.setContentProvider(contentProvider);
		tableViewer.setLabelProvider(labelProvider);
		tableViewer.setInput(input);
		selectKeystores();
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

				updateSelectAllState();

				validate();
			}
		});
		Composite selectButtonArea = new Composite(keystoresGroup, SWT.NONE);
		selectButtonArea.setLayout(new GridLayout(1, true));
		selectButtonArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		selectAllButton = new Button(selectButtonArea, SWT.CHECK);
		selectAllButton.setText(CertificateManagerNLS.BackupDialog_Select_All);
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

		updateSelectAllState();

		setMessage(CertificateManagerNLS.BackupDialog_Default_Message);
		return dialogArea;
	}

	private void updateSelectAllState() {
		if (tableViewer.getCheckedElements().length == tableViewer.getTable().getItems().length) {
			selectAllButton.setSelection(true);
		} else {
			selectAllButton.setSelection(false);
		}
	}

	@Override
	protected Control createButtonBar(Composite parent) {
		Control buttonBar = super.createButtonBar(parent);
		getButton(OK).setEnabled(false);
		return buttonBar;
	}

	@Override
	protected void okPressed() {
		boolean canContinue = true;
		if (verifyOverwrite && archiveFile.exists()) {
			boolean canOvewrite = EclipseUtils.showQuestionDialog(
					CertificateManagerNLS.BackupDialog_Archive_Exists_Title,
					NLS.bind(CertificateManagerNLS.BackupDialog_Archive_Exists_Message, archiveFile));

			if (!canOvewrite) {
				canContinue = false;
			} else {
				archiveFile.delete();
			}
		}

		if (canContinue) {
			if (!FileUtil.canWrite(archiveFile)) {
				EclipseUtils.showErrorDialog(CertificateManagerNLS.BackupDialog_Fail_Writing_Archive_Title,
						NLS.bind(CertificateManagerNLS.BackupDialog_Fail_Writing_Archive_Message, archiveFile));

			} else {
				setErrorMessage(null);
				super.okPressed();
			}
		}
	}

	private void validate() {

		boolean isValid = true;
		boolean hasWarn = false;
		Path path = new Path(archivePath);
		try {
			if (archivePath.isEmpty() || !path.isValidPath(archiveFile.getCanonicalPath())) {
				setErrorMessage(CertificateManagerNLS.BackupDialog_Invalid_Destination_Title);
				isValid = false;
			}
		} catch (IOException e) {
			setErrorMessage(CertificateManagerNLS.BackupDialog_Invalid_Destination_Title);
			isValid = false;
		}

		if (isValid) {
			if (archiveFile.exists() && (archiveFile.isDirectory())) {
				setErrorMessage(CertificateManagerNLS.BackupDialog_Invalid_Destination_Message);
				isValid = false;
			}
		}

		if (isValid) {
			if (!archiveFile.isAbsolute()) {
				setMessage(
						NLS.bind(CertificateManagerNLS.BackupDialog_Non_Absolute_Path, archiveFile.getAbsolutePath()),
						IMessageProvider.WARNING);
				hasWarn = true;
			}
		}

		if (isValid) {
			if (selectedKeyStores.isEmpty()) {
				setErrorMessage(CertificateManagerNLS.BackupDialog_Select_KeyStore);
				isValid = false;
			} else {
				setErrorMessage(null);
				if (!hasWarn) {
					setMessage(CertificateManagerNLS.BackupDialog_Default_Message);
				}
				isValid = true;
			}
		}

		getButton(OK).setEnabled(isValid);
	}

	public void setInput(Object input) {
		if (tableViewer != null) {
			tableViewer.setInput(input);
		}
		this.input = input;
	}

	/**
	 * @return the Archive file to be created
	 */
	public File getArchiveFile() {
		return archiveFile.getName().toLowerCase().endsWith(ZIP_EXT) ? archiveFile : new File(archivePath + ZIP_EXT);
	}

	/**
	 * @return the Keystores file paths to be archived
	 */
	public List<String> getSelectedKeyStores() {
		return selectedKeyStores;
	}

	public void selectKeyStores(List<ITreeNode> keystores) {
		for (ITreeNode treeNode : keystores) {
			if (treeNode instanceof IKeyStore) {
				IKeyStore keystore = (IKeyStore) treeNode;
				selectedKeyStores.add(keystore.getFile().getAbsolutePath());
			}
		}
	}

	private void selectKeystores() {
		for (TableItem item : tableViewer.getTable().getItems()) {
			item.setChecked(selectedKeyStores.contains(item.getText()));
		}
	}

}
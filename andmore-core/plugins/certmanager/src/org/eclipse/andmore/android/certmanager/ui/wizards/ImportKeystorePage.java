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
import java.util.List;

import org.eclipse.andmore.android.certmanager.CertificateManagerActivator;
import org.eclipse.andmore.android.certmanager.core.KeyStoreManager;
import org.eclipse.andmore.android.certmanager.core.PasswordProvider;
import org.eclipse.andmore.android.certmanager.exception.KeyStoreManagerException;
import org.eclipse.andmore.android.certmanager.i18n.CertificateManagerNLS;
import org.eclipse.andmore.android.certmanager.ui.model.IKeyStore;
import org.eclipse.andmore.android.certmanager.ui.model.KeyStoreNode;
import org.eclipse.andmore.android.certmanager.ui.model.SigningAndKeysModelManager;
import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.andmore.android.common.utilities.FileUtil;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

public class ImportKeystorePage extends WizardPage {

	private Text keystoreFilename;

	private ComboViewer keystoreType;

	public static final String IMPORT_KEYSTORE_HELP_ID = CertificateManagerActivator.PLUGIN_ID + ".import_keystore"; //$NON-NLS-1$

	private SelectionListener selectionListener = new SelectionAdapter() {

		@Override
		public void widgetSelected(SelectionEvent e) {
			validatePage();
		}
	};

	private boolean userChangedFilename = false;

	private Composite mainComposite;

	private File keystoreFile;

	private String keystoreTypeString;

	protected boolean keystoreAlreadyMapped = false;

	private IKeyStore keyStoreNode;

	/**
	 * @param pageName
	 */
	protected ImportKeystorePage(String pageName) {
		super(pageName);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets
	 * .Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		mainComposite = new Composite(parent, SWT.FILL);
		mainComposite.setLayout(new GridLayout(3, false));
		mainComposite.setLayoutData(new GridData(GridData.FILL_BOTH));

		setTitle(CertificateManagerNLS.ImportKeystorePage_Title);
		setMessage(CertificateManagerNLS.ImportKeystorePage_Description);

		createFilenameSection(mainComposite);
		createKeystoreTypeSection(mainComposite);

		validatePage();

		setControl(mainComposite);

		// set help id for this page
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IMPORT_KEYSTORE_HELP_ID);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(mainComposite, IMPORT_KEYSTORE_HELP_ID);
	}

	private void createFilenameSection(Composite mainComposite) {
		Label keystoreFilenameLabel = new Label(mainComposite, SWT.NONE);
		keystoreFilenameLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
		keystoreFilenameLabel.setText(CertificateManagerNLS.CreateKeystorePage_KeystoreFilenameLabel);

		keystoreFilename = new Text(mainComposite, SWT.SINGLE | SWT.BORDER);
		keystoreFilename.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		keystoreFilename.addSelectionListener(selectionListener);
		keystoreFilename.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent e) {
				userChangedFilename = true;
				validatePage();
			}
		});

		Button chooseLocation = new Button(mainComposite, SWT.PUSH);
		chooseLocation.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1));
		chooseLocation.setText(CertificateManagerNLS.CreateKeystorePage_KeystoreFilenameBrowse);
		chooseLocation.addSelectionListener(new SelectionAdapter() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse
			 * .swt.events.SelectionEvent)
			 */
			@Override
			public void widgetSelected(SelectionEvent e) {
				Shell shell = Display.getCurrent().getActiveShell();

				FileDialog dialog = new FileDialog(shell, SWT.OPEN);

				String keystoreFilenameStr = dialog.open();

				if (keystoreFilenameStr != null) {
					keystoreFilename.setText(keystoreFilenameStr);
					keystoreType.getCombo().setFocus();
				}
			}
		});
	}

	private void createKeystoreTypeSection(Composite parent) {
		Label keystoreTypeLabel = new Label(parent, SWT.NONE);
		keystoreTypeLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
		keystoreTypeLabel.setText(CertificateManagerNLS.CreateKeystorePage_KeystoreType);

		keystoreType = new ComboViewer(parent, SWT.READ_ONLY);
		keystoreType.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false, 1, 1));
		keystoreType.setContentProvider(new IStructuredContentProvider() {

			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
				// do nothing
			}

			@Override
			public void dispose() {
				// do nothing
			}

			@SuppressWarnings("unchecked")
			@Override
			public Object[] getElements(Object inputElement) {
				return ((List<String>) inputElement).toArray();
			}
		});
		keystoreType.setLabelProvider(new ILabelProvider() {

			@Override
			public void removeListener(ILabelProviderListener listener) {
				// do nothing
			}

			@Override
			public boolean isLabelProperty(Object element, String property) {
				return false;
			}

			@Override
			public void dispose() {
				// do nothing
			}

			@Override
			public void addListener(ILabelProviderListener listener) {
				// do nothing
			}

			@Override
			public String getText(Object element) {
				return (String) element;
			}

			@Override
			public Image getImage(Object element) {
				return null;
			}
		});

		keystoreType.setInput(KeyStoreManager.getInstance().getAvailableTypes());

		for (int i = 0; i < keystoreType.getCombo().getItemCount(); i++) {
			if (keystoreType.getCombo().getItem(i).compareToIgnoreCase(KeyStoreManager.getInstance().getDefaultType()) == 0) {
				keystoreType.getCombo().select(i);
			}
		}

		keystoreType.getCombo().addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent e) {
				validatePage();
			}
		});

		// fill the third column with a blank label
		Label separator2 = new Label(parent, SWT.NONE);
		separator2.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
	}

	protected boolean validatePage() {
		String errorMessage = null;
		boolean pageComplete = true;
		keystoreAlreadyMapped = false;

		keystoreFile = new File(keystoreFilename.getText());
		keystoreTypeString = keystoreType.getCombo().getText();

		if (keystoreType.getCombo().getText().isEmpty()) {
			errorMessage = CertificateManagerNLS.ImportKeystorePage_KeystoreTypeCannotBeEmpty;
			pageComplete = false;
		}

		if (userChangedFilename && !keystoreFile.exists()) {
			errorMessage = NLS.bind(CertificateManagerNLS.ImportKeystorePage_FileDoesNotExist,
					keystoreFilename.getText());
			pageComplete = false;
		}

		if (keystoreFile.exists()) {
			if (keystoreFile.isFile()) {
				int fileSize = -1;
				try {
					fileSize = FileUtil.getFileSize(keystoreFile);
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (fileSize <= 0) {
					errorMessage = NLS.bind(CertificateManagerNLS.ImportKeystorePage_FileEmpty,
							keystoreFilename.getText());
					pageComplete = false;
				}
			} else if (keystoreFile.isDirectory()) {
				errorMessage = NLS.bind(CertificateManagerNLS.ImportKeystorePage_DirectoryNotAllowedErrorMsg,
						keystoreFilename.getText());
				pageComplete = false;
			}
		}

		if (keystoreFilename.getText().isEmpty()) {
			errorMessage = CertificateManagerNLS.ImportKeystorePage_FilenameCannotBeEmpty;
			pageComplete = false;
		}

		if (KeyStoreManager.getInstance().isKeystoreMapped(keystoreFile)) {
			errorMessage = NLS.bind(CertificateManagerNLS.ImportKeystorePage_KeystoreAlreadyMapped,
					keystoreFilename.getText());
			keystoreAlreadyMapped = true;
			pageComplete = false;
		}

		setErrorMessage(errorMessage);
		setPageComplete(pageComplete);

		return pageComplete;
	}

	/**
	 * Import the keystore using the information provided by the user.
	 * 
	 * @return True if the keystore was imported, false otherwise.
	 * */
	protected boolean importKeystore(String password, boolean savePassword) {
		boolean successfullyImported = true;

		validatePage();

		if (isPageComplete()) {

			KeyStoreNode keyStoreNode = new KeyStoreNode(keystoreFile, keystoreType.getCombo().getText());

			try {
				SigningAndKeysModelManager.getInstance().mapKeyStore(keyStoreNode);

				if (savePassword) {
					PasswordProvider passwordProvider = new PasswordProvider(keystoreFile);
					passwordProvider.saveKeyStorePassword(password);
				}
			} catch (KeyStoreManagerException e) {
				// keystore already mapped
				EclipseUtils.showErrorDialog(CertificateManagerNLS.ImportKeystorePage_CouldNotImportKeystore,
						e.getMessage());
				successfullyImported = false;
			}
		} else {
			successfullyImported = false;
		}

		return successfullyImported;
	}

	/**
	 * Import the keystore using the information provided by the user.
	 * 
	 * @return True if the keystore was imported, false otherwise.
	 * */
	public boolean importKeystore() {
		return importKeystore(null, false);
	}

	/**
	 * 
	 * @return
	 */
	protected IKeyStore getSelectedKeystore() {
		if ((keyStoreNode == null)
				|| !keyStoreNode.getFile().equals(keystoreFile)
				|| ((keyStoreNode.getFile().equals(keystoreFile)) && keyStoreNode.getType().equalsIgnoreCase(
						keystoreTypeString))) {
			keyStoreNode = new KeyStoreNode(keystoreFile, keystoreTypeString);
		}
		return keyStoreNode;
	}

	/**
	 * @return the mainComposite
	 */
	protected Composite getMainComposite() {
		return mainComposite;
	}

}

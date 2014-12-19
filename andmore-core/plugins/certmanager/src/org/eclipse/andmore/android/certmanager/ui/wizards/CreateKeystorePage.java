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
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
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
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
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

public class CreateKeystorePage extends WizardPage {

	private static final String CREATE_KEYSTORE_HELP_ID = CertificateManagerActivator.PLUGIN_ID + ".new_keystore"; //$NON-NLS-1$

	private Text keystoreFilenameText;

	private ComboViewer keystoreTypeComboViewer;

	private Text keystorePasswordText;

	private Text keystoreConfirmPasswordText;

	private String keystorePassword;

	private boolean initialValidation = true;

	private boolean userChangedPasswordConfirmation = false;

	private boolean userChangedPassword = false;

	SelectionListener selectionListener = new SelectionListener() {

		@Override
		public void widgetSelected(SelectionEvent e) {
			validatePage();
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			// nothing to do...
		}
	};

	private Button savePassword;

	private Button useTypeAsExtensionCheckBox;

	protected boolean useTypeAsExtensionCheckBoxPreviousState = true;

	/**
	 * @param pageName
	 */
	protected CreateKeystorePage(String pageName) {
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
		Composite mainComposite = new Composite(parent, SWT.FILL);
		mainComposite.setLayout(new GridLayout(3, false));
		mainComposite.setLayoutData(new GridData(GridData.FILL_BOTH));

		setTitle(CertificateManagerNLS.CreateKeystorePage_CreateKeystore);
		setMessage(CertificateManagerNLS.CreateKeystorePage_WizardDefaultMessage);

		createFilenameSection(mainComposite);
		createKeystoreTypeSection(mainComposite);
		createFilenameExtensionSection(mainComposite);

		setKeystoreFilenameExtension();

		// LINE TO SEPARATE PASSWORD SECTION FROM KEYSTORE DETAILS SECTION
		Label separator1 = new Label(mainComposite, SWT.SEPARATOR | SWT.HORIZONTAL);
		separator1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));

		createKeystorePasswordSection(mainComposite);
		createConfirmPasswordSection(mainComposite);
		createSavePasswordSection(mainComposite);

		validatePage();

		setControl(mainComposite);

		// set help id for this page
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, CREATE_KEYSTORE_HELP_ID);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(mainComposite, CREATE_KEYSTORE_HELP_ID);
	}

	/**
	 * @param mainComposite
	 */
	private void createKeystoreTypeSection(Composite parent) {
		Label keystoreTypeLabel = new Label(parent, SWT.NONE);
		keystoreTypeLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
		keystoreTypeLabel.setText(CertificateManagerNLS.CreateKeystorePage_KeystoreType);

		keystoreTypeComboViewer = new ComboViewer(parent, SWT.READ_ONLY);
		keystoreTypeComboViewer.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false, 1, 1));
		keystoreTypeComboViewer.setContentProvider(new IStructuredContentProvider() {

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
		keystoreTypeComboViewer.setLabelProvider(new ILabelProvider() {

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

		keystoreTypeComboViewer.setInput(KeyStoreManager.getInstance().getAvailableTypes());

		keystoreTypeComboViewer.getCombo().addSelectionListener(new SelectionAdapter() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse
			 * .swt.events.SelectionEvent)
			 */
			@Override
			public void widgetSelected(SelectionEvent e) {
				useTypeAsExtensionCheckBox.setEnabled(true);
				useTypeAsExtensionCheckBox.setSelection(useTypeAsExtensionCheckBoxPreviousState);

				if (useTypeAsExtensionCheckBox.getSelection()) {
					setKeystoreFilenameExtension();
				}
			}
		});

		for (int i = 0; i < keystoreTypeComboViewer.getCombo().getItemCount(); i++) {
			if (keystoreTypeComboViewer.getCombo().getItem(i)
					.compareToIgnoreCase(KeyStoreManager.getInstance().getDefaultType()) == 0) {
				keystoreTypeComboViewer.getCombo().select(i);
			}
		}

		keystoreTypeComboViewer.getCombo().addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (useTypeAsExtensionCheckBox != null) {
					useTypeAsExtensionCheckBox.setEnabled(false);
					useTypeAsExtensionCheckBox.setSelection(false);
				}
			}
		});

		// fill the third column with a blank label
		Label separator2 = new Label(parent, SWT.NONE);
		separator2.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
	}

	/**
	 * Set the extension of the keystore based on selected keystore type and the
	 * user's choice to use it or not as the extension. If the user typed a
	 * custom keystore type, then the filename extension is set to ".keystore".
	 * */
	protected void setKeystoreFilenameExtension() {
		String keystoreFilename = keystoreFilenameText.getText();
		String keystoreType = keystoreTypeComboViewer.getCombo().getText();

		List<String> availableTypes = KeyStoreManager.getInstance().getAvailableTypes();
		availableTypes.add(CertificateManagerNLS.CreateKeystorePage_DefaultKeystoreFilenameExtension);

		for (String availableType : availableTypes) {
			String availableTypeExtension = "." + availableType.toLowerCase(); //$NON-NLS-1$
			if (keystoreFilename.endsWith(availableTypeExtension)) {
				keystoreFilename = keystoreFilename.substring(0,
						keystoreFilename.length() - availableTypeExtension.length());
				break;
			}
		}

		keystoreFilenameText.setText(keystoreFilename + "." + keystoreType.toLowerCase()); //$NON-NLS-1$
	}

	private void createFilenameExtensionSection(Composite mainComposite) {
		useTypeAsExtensionCheckBox = new Button(mainComposite, SWT.CHECK);
		useTypeAsExtensionCheckBox.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
		useTypeAsExtensionCheckBox.setText(CertificateManagerNLS.CreateKeystorePage_UseKeystoreTypeAsExtension);
		useTypeAsExtensionCheckBox.setSelection(true);

		useTypeAsExtensionCheckBox.addSelectionListener(new SelectionAdapter() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse
			 * .swt.events.SelectionEvent)
			 */
			@Override
			public void widgetSelected(SelectionEvent e) {
				useTypeAsExtensionCheckBoxPreviousState = useTypeAsExtensionCheckBox.getSelection();
				if (useTypeAsExtensionCheckBox.getSelection()) {
					setKeystoreFilenameExtension();
				}
			}
		});
	}

	/**
	 * @param mainComposite
	 */
	private void createSavePasswordSection(Composite mainComposite) {
		savePassword = new Button(mainComposite, SWT.CHECK);
		savePassword.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
		savePassword.setText(CertificateManagerNLS.CreateKeystorePage_SaveThisPassword);
		savePassword.setSelection(false);
	}

	/**
	 * @param mainComposite
	 */
	private void createConfirmPasswordSection(Composite mainComposite) {
		Label keystoreConfirmPasswordLabel = new Label(mainComposite, SWT.NONE);
		keystoreConfirmPasswordLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
		keystoreConfirmPasswordLabel.setText(CertificateManagerNLS.CreateKeystorePage_KeystoreConfirmPasswordLabel);

		keystoreConfirmPasswordText = new Text(mainComposite, SWT.SINGLE | SWT.BORDER | SWT.PASSWORD);
		keystoreConfirmPasswordText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		keystoreConfirmPasswordText.addSelectionListener(selectionListener);
		keystoreConfirmPasswordText.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent e) {
				userChangedPasswordConfirmation = true;
				validatePage();
			}
		});

		// fill the third column with a blank label
		Label separator2 = new Label(mainComposite, SWT.NONE);
		separator2.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
	}

	/**
	 * @param mainComposite
	 */
	private void createKeystorePasswordSection(Composite mainComposite) {
		Label keystorePasswordLabel = new Label(mainComposite, SWT.NONE);
		keystorePasswordLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
		keystorePasswordLabel.setText(CertificateManagerNLS.CreateKeystorePage_KeystorePasswordLabel);

		keystorePasswordText = new Text(mainComposite, SWT.SINGLE | SWT.BORDER | SWT.PASSWORD);
		keystorePasswordText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		keystorePasswordText.addSelectionListener(selectionListener);
		keystorePasswordText.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent e) {
				keystorePassword = keystorePasswordText.getText();
				userChangedPassword = true;
				validatePage();
			}
		});

		// fill the third column with a blank label
		@SuppressWarnings("unused")
		Label separator = new Label(mainComposite, SWT.NONE);
		keystorePasswordLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
	}

	/**
	 * @param mainComposite
	 */
	private void createFilenameSection(Composite mainComposite) {
		Label keystoreFilenameLabel = new Label(mainComposite, SWT.NONE);
		keystoreFilenameLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
		keystoreFilenameLabel.setText(CertificateManagerNLS.CreateKeystorePage_KeystoreFilenameLabel);

		keystoreFilenameText = new Text(mainComposite, SWT.SINGLE | SWT.BORDER);
		keystoreFilenameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		keystoreFilenameText.setText(generateKeyStoreFilename());
		keystoreFilenameText.addSelectionListener(selectionListener);
		keystoreFilenameText.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent e) {
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

				FileDialog dialog = new FileDialog(shell, SWT.SAVE);

				String keystoreFilenameStr = dialog.open();

				if (keystoreFilenameStr != null) {
					keystoreFilenameText.setText(keystoreFilenameStr);
				}
			}
		});
	}

	private void validatePage() {
		boolean pageComplete = true;

		String errorMessage = null;

		String message = CertificateManagerNLS.CreateKeystorePage_WizardDefaultMessage;

		int messageType = IMessageProvider.NONE;

		if (initialValidation == true) {
			// when the wizard opens, does not show any errors
			pageComplete = false;
			initialValidation = false;
		} else {
			// password text and confirmation password text must match
			if (!keystorePasswordText.getText().equals(keystoreConfirmPasswordText.getText())) {
				// if the user hasn't started typing the confirmation password,
				// then just show an info, instead of an error
				if (userChangedPasswordConfirmation) {
					errorMessage = CertificateManagerNLS.CreateKeystorePage_PasswordDoesNotMatch;
					pageComplete = false;
				} else {
					message = CertificateManagerNLS.CreateKeystorePage_ConfirmPasswordInfoMsg;
					messageType = IMessageProvider.INFORMATION;
					pageComplete = false;
				}
			}
			// check password size according to keytool specification
			if (keystorePasswordText.getText().length() < IKeyStore.KEYSTORE_PASSWORD_MIN_SIZE) {
				if (userChangedPassword) {
					errorMessage = NLS.bind(CertificateManagerNLS.CreateKeystorePage_PasswordMinSizeMessage,
							IKeyStore.KEYSTORE_PASSWORD_MIN_SIZE);
					pageComplete = false;
				} else {
					message = CertificateManagerNLS.CreateKeystorePage_SetPasswordInfoMsg;
					messageType = IMessageProvider.INFORMATION;
					pageComplete = false;
				}
			}

			// check if store type is filled
			if (keystoreTypeComboViewer.getCombo().getText().isEmpty()) {
				errorMessage = CertificateManagerNLS.CreateKeystorePage_SetKeystoreType;
				pageComplete = false;
			}

			// check if filename is valid
			try {
				File keystoreFile = new File(keystoreFilenameText.getText().trim());
				Path keystorePath = new Path(keystoreFilenameText.getText().trim());
				if (!keystorePath.isValidPath(keystoreFile.getCanonicalPath())) {
					// throw the same exception as getCanonicalPath() in order
					// to do not duplicate code
					throw new IOException();
				}
			} catch (IOException e) {
				errorMessage = CertificateManagerNLS.CreateKeystorePage_FilenameSyntaxError;
				pageComplete = false;
			}
			if (keystoreFilenameText.getText().trim().isEmpty()) {
				errorMessage = CertificateManagerNLS.ImportKeystorePage_FilenameCannotBeEmpty;
				pageComplete = false;
			}
		}

		setMessage(message, messageType);
		setErrorMessage(errorMessage);
		setPageComplete(pageComplete);
	}

	/**
	 * Generate a valid filename for a new keystore. The file must not exist, so
	 * a serial number is added to it as necessary.
	 * 
	 * @return An standard keystore filename.
	 * */
	private String generateKeyStoreFilename() {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd-HH_mm_ss_SSS"); //$NON-NLS-1$
		String timestamp = dateFormat.format(Calendar.getInstance().getTime());

		// initial keystore filename with timestamp
		String keystoreFilenameStr = System.getProperty("user.home") + System.getProperty("file.separator") //$NON-NLS-1$ //$NON-NLS-2$
				+ NLS.bind(CertificateManagerNLS.CreateKeystorePage_DefaultKeystoreFilename, timestamp);

		File keystoreFile = new File(keystoreFilenameStr);

		// while file already exists, generate a new one using a new timestamp
		while (keystoreFile.exists()) {
			timestamp = dateFormat.format(Calendar.getInstance().getTime());
			keystoreFilenameStr = System.getProperty("user.home") + System.getProperty("file.separator") //$NON-NLS-1$ //$NON-NLS-2$
					+ NLS.bind(CertificateManagerNLS.CreateKeystorePage_DefaultKeystoreFilename, timestamp);
			keystoreFile = new File(keystoreFilenameStr);
		}

		return keystoreFilenameStr;
	}

	/**
	 * As this page works independently of other pages, it has its own version
	 * of performFinish(). Wizards that use this page must call this method to
	 * effectively create the new keystore.
	 * 
	 * @return {@code true} if the keystore were successfully created,
	 *         {@code false} otherwise.
	 * */
	public KeyStoreNode createKeyStore() {
		boolean successfullyCreated = true;
		File keystoreFile = null;
		KeyStoreNode keystoreNode = null;

		try {
			keystoreFile = new File(keystoreFilenameText.getText().trim());
			if (validateKeyStoreFile(keystoreFile)) {
				keystoreNode = (KeyStoreNode) KeyStoreManager.createKeyStore(keystoreFile, keystoreTypeComboViewer
						.getCombo().getText(), keystorePasswordText.getText().toCharArray());

				SigningAndKeysModelManager.getInstance().mapKeyStore(keystoreNode);
			} else {
				// file already exist and will not be overwritten
				successfullyCreated = false;
			}
		} catch (KeyStoreManagerException e) {
			// in case of error, the keystore wasn't properly created and the
			// file should not be left on file system
			if (keystoreFile != null) {
				keystoreFile.delete();
			}

			EclipseUtils.showErrorDialog(
					CertificateManagerNLS.CreateKeystorePage_ErrorCreatingKeystore,
					NLS.bind(CertificateManagerNLS.CreateKeystorePage_ErrorOnKeyStoreFileCreation,
							keystoreFilenameText.getText()));
			successfullyCreated = false;
		}

		if (successfullyCreated && savePassword.getSelection()) {
			savePassword(keystoreFile);
		}

		return successfullyCreated ? keystoreNode : null;
	}

	/**
	 * @param keystoreFile
	 */
	private void savePassword(File keystoreFile) {
		try {
			PasswordProvider passwordProvider = new PasswordProvider(keystoreFile);
			passwordProvider.saveKeyStorePassword(keystorePasswordText.getText());
		} catch (KeyStoreManagerException e) {
			EclipseUtils.showWarningDialog(CertificateManagerNLS.CreateKeystorePage_CouldNotSavePassword,
					e.getLocalizedMessage());
		}
	}

	/*
	 * If file exists and the user chooses to overwrite it, the key store file
	 * is valid and return value is true. If file exists and the user do not
	 * want to overwrite the file, then the keystore file is considered invalid
	 * and the return value is false. If file does not exist, then the file is
	 * valid and the return value is true.
	 */
	private boolean validateKeyStoreFile(File keystoreFile) {
		boolean result = true;

		if (keystoreFile.exists()) {
			Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();

			result = MessageDialog.openQuestion(
					shell,
					CertificateManagerNLS.CreateKeystorePage_ConfirmFileOverwrite,
					NLS.bind(CertificateManagerNLS.CreateKeystorePage_ConfirmReplaceFile,
							keystoreFile.getAbsolutePath()));
			if (result) {
				// file will be recreated
				keystoreFile.delete();
			}
		}
		return result;
	}

	public String getPassword() {
		return keystorePassword;
	}

}

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
package org.eclipse.andmore.android.certmanager.ui.dialogs.importks;

import java.io.File;
import java.security.KeyStore.Entry;
import java.security.KeyStore.PasswordProtection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.andmore.android.certmanager.CertificateManagerActivator;
import org.eclipse.andmore.android.certmanager.core.KeyStoreManager;
import org.eclipse.andmore.android.certmanager.exception.InvalidPasswordException;
import org.eclipse.andmore.android.certmanager.exception.KeyStoreManagerException;
import org.eclipse.andmore.android.certmanager.i18n.CertificateManagerNLS;
import org.eclipse.andmore.android.certmanager.ui.model.IKeyStore;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class ConvertKeyStoreTypeDialog extends TitleAreaDialog {
	public class EntryModel {

		private final String alias;

		private String passwd;

		private boolean verified;

		public EntryModel(String alias) {
			this.alias = alias;
			try {
				String savedPass = keyStore.getPasswordProvider().getPassword(alias, false);
				setPasswd(savedPass != null ? savedPass : ""); //$NON-NLS-1$
			} catch (KeyStoreManagerException e) {
				setPasswd(""); //$NON-NLS-1$
			}
		}

		@Override
		public String toString() {
			return alias;
		}

		public String getPasswd() {
			return passwd;
		}

		public void setPasswd(String passwd) {
			this.passwd = passwd;
			try {
				Entry entry = keyStore.getKeyStore().getEntry(alias, new PasswordProtection(passwd.toCharArray()));
				setVerified(entry != null);
			} catch (Exception e) {
				setVerified(false);
			}
			aliaseMap.put(alias, passwd);
		}

		public String getAlias() {
			return alias;
		}

		public boolean isVerified() {
			return verified;
		}

		private void setVerified(boolean verified) {
			this.verified = verified;
			validateUi();
		}
	}

	public class EntriesContentProvider implements IStructuredContentProvider {

		@Override
		public void dispose() {
			// do nothing
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			// do nothing
		}

		@Override
		public Object[] getElements(Object inputElement) {
			List<EntryModel> modelList = null;
			if (inputElement instanceof List<?>) {
				List<?> inputList = (List<?>) inputElement;
				modelList = new ArrayList<EntryModel>(inputList.size());
				Iterator<?> it = inputList.iterator();
				while (it.hasNext()) {
					Object element = it.next();
					if (element instanceof String) // received an alias
					{
						String alias = (String) element;
						EntryModel entryModel = new EntryModel(alias);
						modelList.add(entryModel);
					}
				}

			}
			return modelList.toArray();
		}

	}

	private final class PasswordEditingSupport extends EditingSupport {
		private PasswordEditingSupport(ColumnViewer viewer) {
			super(viewer);
		}

		@Override
		protected boolean canEdit(Object element) {
			return true;
		}

		@Override
		protected CellEditor getCellEditor(Object element) {
			return new TextCellEditor(entriesTable, SWT.PASSWORD);
		}

		@Override
		protected Object getValue(Object element) {
			return ((EntryModel) element).getPasswd();
		}

		@Override
		protected void setValue(Object element, Object value) {
			EntryModel model = (EntryModel) element;
			model.setPasswd((String) value);
			getViewer().update(element, null);
		}
	}

	private static final String HELP_ID = CertificateManagerActivator.PLUGIN_ID + ".convert_keystore_type"; //$NON-NLS-1$

	private static final String WIZARD_BANNER = "icons/wizban/change_keystore_type_wiz.png"; //$NON-NLS-1$

	private IKeyStore keyStore;

	private String newType = ""; //$NON-NLS-1$

	private Text passwdText;

	private String password = ""; //$NON-NLS-1$

	private Table entriesTable;

	private TableViewer entriesTableViewer;

	private final Map<String, String> aliaseMap = new HashMap<String, String>();

	/**
	 * Create the dialog.
	 * 
	 * @param parentShell
	 */
	public ConvertKeyStoreTypeDialog(Shell parentShell, IKeyStore keyStore) {
		super(parentShell);
		setShellStyle(SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);

		this.keyStore = keyStore;
		setTitleImage(AbstractUIPlugin.imageDescriptorFromPlugin(CertificateManagerActivator.PLUGIN_ID, WIZARD_BANNER)
				.createImage());
	}

	/**
	 * Create contents of the dialog.
	 * 
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		parent.getShell().setText(CertificateManagerNLS.ConvertKeyStoreTypeDialog_DialogTitle);
		setMessage(CertificateManagerNLS.ConvertKeyStoreTypeDialog_DefaultMessage);
		setTitle(CertificateManagerNLS.ConvertKeyStoreTypeDialog_DialogTitle);
		Composite area = (Composite) super.createDialogArea(parent);
		Composite container = new Composite(area, SWT.NONE);
		container.setLayout(new GridLayout(1, false));
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		Composite convertTopComposite = new Composite(container, SWT.NONE);
		convertTopComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
		convertTopComposite.setLayout(new GridLayout(1, false));

		Composite keyStoreComposite = new Composite(convertTopComposite, SWT.NONE);
		keyStoreComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		keyStoreComposite.setLayout(new GridLayout(3, false));

		Label keyStoreLabel = new Label(keyStoreComposite, SWT.NONE);
		keyStoreLabel.setText(CertificateManagerNLS.ConvertKeyStoreTypeDialog_KeyStoreLabel);

		final Combo keyStoreCombo = new Combo(keyStoreComposite, SWT.READ_ONLY);
		keyStoreCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		final KeyStoreManager keyStoreManager = KeyStoreManager.getInstance();
		try {
			List<IKeyStore> keyStores = keyStoreManager.getKeyStores();
			for (IKeyStore keyStore : keyStores) {
				File ksFile = keyStore.getFile();
				String comboItem = ksFile.getName() + " - " + ksFile.getAbsolutePath(); //$NON-NLS-1$
				keyStoreCombo.add(comboItem);
				keyStoreCombo.setData(comboItem, keyStore);
				if (keyStore.equals(this.keyStore)) {
					keyStoreCombo.select(keyStoreCombo.indexOf(comboItem));
				}
			}
		} catch (KeyStoreManagerException e1) {
			setErrorMessage(CertificateManagerNLS.ConvertKeyStoreTypeDialog_CouldNotLoad_Keystores_Error);
		}

		Label passwdLabel = new Label(keyStoreComposite, SWT.NONE);
		passwdLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		passwdLabel.setText(CertificateManagerNLS.ConvertKeyStoreTypeDialog_Password_Label);

		passwdText = new Text(keyStoreComposite, SWT.BORDER | SWT.PASSWORD);
		passwdText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				password = passwdText.getText();
			}
		});
		passwdText.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				super.widgetDefaultSelected(e);
				loadEntries();
			}
		});
		passwdText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Button loadButton = new Button(keyStoreComposite, SWT.NONE);
		GridData gd_loadButton = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
		gd_loadButton.widthHint = 80;
		loadButton.setLayoutData(gd_loadButton);
		loadButton.setText(CertificateManagerNLS.ConvertKeyStoreTypeDialog_Load_Button);
		loadButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				loadEntries();
			}
		});

		Composite composite_1 = new Composite(convertTopComposite, SWT.NONE);
		GridData gd_composite_1 = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_composite_1.heightHint = 39;
		composite_1.setLayoutData(gd_composite_1);
		composite_1.setLayout(new GridLayout(4, false));

		Label typeLabel = new Label(composite_1, SWT.NONE);
		typeLabel.setText(CertificateManagerNLS.ConvertKeyStoreTypeDialog_Original_Type_Label);

		final Label currentTypeLabel = new Label(composite_1, SWT.NONE);
		GridData gd_currentTypeLabel = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_currentTypeLabel.widthHint = 90;
		gd_currentTypeLabel.minimumWidth = 90;
		currentTypeLabel.setLayoutData(gd_currentTypeLabel);
		currentTypeLabel.setText(keyStore.getType());

		Label newTypeLabel = new Label(composite_1, SWT.NONE);
		newTypeLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		newTypeLabel.setText(CertificateManagerNLS.ConvertKeyStoreTypeDialog_NewType_Label);

		final Combo typeCombo = new Combo(composite_1, SWT.READ_ONLY);
		typeCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		loadTypeCombo(keyStoreManager, typeCombo);
		new Label(composite_1, SWT.NONE);
		new Label(composite_1, SWT.NONE);
		new Label(composite_1, SWT.NONE);
		new Label(composite_1, SWT.NONE);

		typeCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				super.widgetSelected(e);
				newType = typeCombo.getText();
				validateUi();
			}
		});

		keyStoreCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				super.widgetSelected(e);
				updateUi(keyStoreCombo, keyStoreManager, currentTypeLabel, typeCombo);
				validateUi();
			}
		});

		Group entriesGroup = new Group(container, SWT.NONE);
		entriesGroup.setLayout(new GridLayout(1, true));
		GridData gd_entriesGroup = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
		gd_entriesGroup.heightHint = 230;
		gd_entriesGroup.widthHint = 433;
		entriesGroup.setLayoutData(gd_entriesGroup);
		entriesGroup.setText(CertificateManagerNLS.ConvertKeyStoreTypeDialog_Entries_Group);

		entriesTableViewer = new TableViewer(entriesGroup, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		entriesTableViewer.setContentProvider(new EntriesContentProvider());
		entriesTable = entriesTableViewer.getTable();
		entriesTable.setHeaderVisible(true);
		entriesTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		TableViewerColumn aliasViewerColumn = new TableViewerColumn(entriesTableViewer, SWT.NONE);
		TableColumn tblclmnAlias = aliasViewerColumn.getColumn();
		tblclmnAlias.setWidth(100);
		tblclmnAlias.setText(CertificateManagerNLS.ConvertKeyStoreTypeDialog_Alias_Column);
		aliasViewerColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return ((EntryModel) element).getAlias();
			}
		});

		TableViewerColumn passwordViewerColumn_1 = new TableViewerColumn(entriesTableViewer, SWT.NONE);
		passwordViewerColumn_1.setEditingSupport(new PasswordEditingSupport(entriesTableViewer));
		TableColumn tblclmnPassword = passwordViewerColumn_1.getColumn();
		tblclmnPassword.setWidth(100);
		tblclmnPassword.setText(CertificateManagerNLS.ConvertKeyStoreTypeDialog_Password_Column);
		passwordViewerColumn_1.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return ((EntryModel) element).getPasswd().replaceAll(".", "*"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		});

		TableViewerColumn verifiedViewerColumn_2 = new TableViewerColumn(entriesTableViewer, SWT.NONE);
		TableColumn tblclmnVerified = verifiedViewerColumn_2.getColumn();
		tblclmnVerified.setWidth(130);
		tblclmnVerified.setText(CertificateManagerNLS.ConvertKeyStoreTypeDialog_Verified_Column);
		verifiedViewerColumn_2.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return ((EntryModel) element).isVerified() ? CertificateManagerNLS.ConvertKeyStoreTypeDialog_Verified_Pass_Yes
						: CertificateManagerNLS.ConvertKeyStoreTypeDialog_Verified_Pass_Wrong;
			}
		});

		updateUi(keyStoreCombo, keyStoreManager, currentTypeLabel, typeCombo);

		return area;
	}

	@Override
	protected Control createHelpControl(Composite parent) {
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent.getShell(), HELP_ID);
		return super.createHelpControl(parent);
	}

	private void updateUi(final Combo keyStoreCombo, final KeyStoreManager keyStoreManager,
			final Label currentTypeLabel, final Combo typeCombo) {
		keyStore = (IKeyStore) keyStoreCombo.getData(keyStoreCombo.getText());
		try {
			password = keyStore.getPasswordProvider().getKeyStorePassword(false);
		} catch (KeyStoreManagerException e) {
			StudioLogger.error("Error while accessing keystore manager. " + e.getMessage());
		}

		if (password == null) {
			password = ""; //$NON-NLS-1$
		}
		passwdText.setText(password);
		if (!password.isEmpty()) {
			IKeyStore keyStore = (IKeyStore) keyStoreCombo.getData(keyStoreCombo.getText());
			currentTypeLabel.setText(keyStore.getType());
			loadTypeCombo(keyStoreManager, typeCombo);
			aliaseMap.clear();
			try {
				if (keyStore.isPasswordValid(password)) {
					List<String> aliases;
					aliases = keyStore.getAliases(password);
					entriesTableViewer.setInput(aliases);
				} else {
					validateUi();
				}
			} catch (KeyStoreManagerException e) {
				StudioLogger.error("Error while accessing keystore manager. " + e.getMessage());
			} catch (InvalidPasswordException e) {
				validateUi();
			}
		}
	}

	private void loadEntries() {
		try {
			aliaseMap.clear();
			List<String> aliases = keyStore.getAliases(password);
			entriesTableViewer.setInput(aliases);
		} catch (KeyStoreManagerException e1) {
			setErrorMessage(CertificateManagerNLS.ConvertKeyStoreTypeDialog_Error_Loading_Keystore);
			entriesTableViewer.setInput(new ArrayList<String>());
		} catch (InvalidPasswordException e1) {
			setErrorMessage(CertificateManagerNLS.ConvertKeyStoreTypeDialog_Invalid_Keystore_Pass);
			entriesTableViewer.setInput(new ArrayList<String>());
		}
		validateUi();
	}

	private void loadTypeCombo(KeyStoreManager keyStoreManager, final Combo typeCombo) {
		typeCombo.setItems(new String[0]);
		List<String> availableTypes = keyStoreManager.getAvailableTypes();
		for (String type : availableTypes) {
			if (!type.equals(keyStore.getType())) {
				typeCombo.add(type);
			}
		}
		typeCombo.clearSelection();
	}

	@Override
	protected Control createButtonBar(Composite parent) {
		Control bar = super.createButtonBar(parent);
		getButton(OK).setEnabled(false);
		return bar;
	}

	public void validateUi() {
		boolean isValid = true;
		setErrorMessage(null);
		if (isValid && (keyStore == null)) {
			isValid = false;
			setMessage(CertificateManagerNLS.ConvertKeyStoreTypeDialog_Choose_KeyStore_Msg);
		}
		if (isValid) {
			boolean passwordValid;
			try {
				passwordValid = keyStore.isPasswordValid(password);
			} catch (KeyStoreManagerException e) {
				passwordValid = false;
			} catch (InvalidPasswordException e) {
				passwordValid = false;
			}
			if (!passwordValid) {
				isValid = false;
				setErrorMessage(CertificateManagerNLS.ConvertKeyStoreTypeDialog_Invalid_Keystore_Pass);
			}
		}
		if (isValid && newType.isEmpty()) {
			isValid = false;
			setMessage(CertificateManagerNLS.ConvertKeyStoreTypeDialog_Choose_New_Type_Msg);
		}
		if (isValid) {
			Object input = entriesTableViewer.getInput();
			if (input != null) {
				int itemCount = ((List<?>) input).size();
				for (int i = 0; i < itemCount; i++) {
					EntryModel entryModel = (EntryModel) entriesTableViewer.getElementAt(i);
					if ((entryModel != null) && !entryModel.isVerified()) {
						isValid = false;
						setMessage(CertificateManagerNLS.ConvertKeyStoreTypeDialog_Incorrect_Entry_Pass,
								IMessageProvider.WARNING);
						break;
					}
				}
			}
		}

		Button okButton = getButton(OK);
		if (okButton != null) {
			if (!isValid) {
				okButton.setEnabled(false);
			} else {
				getButton(OK).setEnabled(true);
				setErrorMessage(null);
				setMessage(CertificateManagerNLS.ConvertKeyStoreTypeDialog_DefaultMessage);
			}
		}
	}

	public IKeyStore getKeyStore() {
		return keyStore;
	}

	public String getNewType() {
		return newType;
	}

	public Map<String, String> getAliases() {
		return aliaseMap;
	}

	public String getKeystorePassword() {
		return this.password;
	}
}

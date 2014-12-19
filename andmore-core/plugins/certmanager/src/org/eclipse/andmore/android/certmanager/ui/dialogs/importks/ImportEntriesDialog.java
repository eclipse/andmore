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
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.IStructuredContentProvider;
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
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class ImportEntriesDialog extends TitleAreaDialog {
	public class EntryModel {
		private final String alias;

		private String passwd;

		private boolean verified;

		public EntryModel(String alias) {
			this.alias = alias;
			try {
				String savedPass = sourceKeyStore.getPasswordProvider().getPassword(alias, false);
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
				Entry entry = sourceKeyStore.getKeyStore()
						.getEntry(alias, new PasswordProtection(passwd.toCharArray()));
				setVerified(entry != null);
			} catch (Exception e) {
				setVerified(false);
			}
			aliasMap.put(alias, passwd);
		}

		public String getAlias() {
			return alias;
		}

		public boolean isVerified() {
			return verified;
		}

		private void setVerified(boolean verified) {
			this.verified = verified;
			// entriesTableViewer.update(this, null);
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
			validateUi();
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

	private static final String WIZARD_BANNER = "icons/wizban/import_entries_wiz.png"; //$NON-NLS-1$

	private static final String HELP_ID = CertificateManagerActivator.PLUGIN_ID + ".import_entries_dialog"; //$NON-NLS-1$

	private IKeyStore sourceKeyStore;

	private IKeyStore targetKeyStore;

	private Text passwdText;

	protected String sourcePassword = ""; //$NON-NLS-1$

	private Table entriesTable;

	private CheckboxTableViewer entriesTableViewer;

	private final Map<String, String> aliasMap = new HashMap<String, String>();

	private Combo keyStoreCombo;

	private Combo targetKsCombo;

	protected List<String> selectedAlias = new ArrayList<String>();

	/**
	 * Create the dialog.
	 * 
	 * @param parentShell
	 */
	public ImportEntriesDialog(Shell parentShell, IKeyStore keyStore) {
		super(parentShell);
		setShellStyle(SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);

		this.targetKeyStore = keyStore;
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
		parent.getShell().setText(CertificateManagerNLS.ImportKeyStoreDialog_Dialog_Title);
		setMessage(CertificateManagerNLS.ImportKeyStoreDialog_Default_Message);
		setTitle(CertificateManagerNLS.ImportKeyStoreDialog_Dialog_Title);
		Composite area = (Composite) super.createDialogArea(parent);
		Composite container = new Composite(area, SWT.NONE);
		container.setLayout(new GridLayout(1, false));
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		Group SourceGroup = new Group(container, SWT.NONE);
		SourceGroup.setText(CertificateManagerNLS.ImportKeyStoreDialog_Source_Group);
		SourceGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		SourceGroup.setLayout(new GridLayout(1, false));

		Composite keyStoreComposite = new Composite(SourceGroup, SWT.NONE);
		keyStoreComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		keyStoreComposite.setLayout(new GridLayout(3, false));

		Label keyStoreLabel = new Label(keyStoreComposite, SWT.NONE);
		keyStoreLabel.setText(CertificateManagerNLS.ImportKeyStoreDialog_KeyStore_Label);

		keyStoreCombo = new Combo(keyStoreComposite, SWT.READ_ONLY);
		keyStoreCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		Label passwdLabel = new Label(keyStoreComposite, SWT.NONE);
		passwdLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		passwdLabel.setText(CertificateManagerNLS.ImportKeyStoreDialog_Password_Label);

		passwdText = new Text(keyStoreComposite, SWT.BORDER | SWT.PASSWORD);
		passwdText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				sourcePassword = passwdText.getText();
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
		loadButton.setText(CertificateManagerNLS.ImportKeyStoreDialog_Load_Button);

		Composite entriesComposite = new Composite(SourceGroup, SWT.NONE);
		GridData gd_entriesComposite = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
		gd_entriesComposite.heightHint = 200;
		entriesComposite.setLayoutData(gd_entriesComposite);
		entriesComposite.setLayout(new GridLayout(1, true));

		entriesTableViewer = CheckboxTableViewer.newCheckList(entriesComposite, SWT.BORDER | SWT.CHECK
				| SWT.FULL_SELECTION);
		entriesTableViewer.setContentProvider(new EntriesContentProvider());
		entriesTable = entriesTableViewer.getTable();
		entriesTable.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (e.detail == SWT.CHECK) {
					validateUi();
					TableItem item = (TableItem) e.item;
					if (item.getChecked()) {
						selectedAlias.add(item.getText(0));
					} else {
						selectedAlias.remove(item.getText(0));
					}
				}
			}
		});
		entriesTable.setHeaderVisible(true);
		GridData gd_entriesTable = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
		gd_entriesTable.heightHint = 250;
		entriesTable.setLayoutData(gd_entriesTable);
		TableViewerColumn aliasViewerColumn = new TableViewerColumn(entriesTableViewer, SWT.NONE);
		TableColumn tblclmnAlias = aliasViewerColumn.getColumn();
		tblclmnAlias.setWidth(100);
		tblclmnAlias.setText(CertificateManagerNLS.ImportKeyStoreDialog_Alias_Column);
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
		tblclmnPassword.setText(CertificateManagerNLS.ImportKeyStoreDialog_Passwd_Column);
		passwordViewerColumn_1.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return ((EntryModel) element).getPasswd().replaceAll(".", "*"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		});

		TableViewerColumn verifiedViewerColumn_2 = new TableViewerColumn(entriesTableViewer, SWT.NONE);
		TableColumn tblclmnVerified = verifiedViewerColumn_2.getColumn();
		tblclmnVerified.setWidth(130);
		tblclmnVerified.setText(CertificateManagerNLS.ImportKeyStoreDialog_Verified_Column);
		verifiedViewerColumn_2.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return ((EntryModel) element).isVerified() ? CertificateManagerNLS.ImportKeyStoreDialog_Verified_Pass_Yes
						: CertificateManagerNLS.ImportKeyStoreDialog_Verified_Pass_Wrong;
			}
		});
		loadButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				loadEntries();
			}
		});

		keyStoreCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				super.widgetSelected(e);
				sourceKeyStore = (IKeyStore) keyStoreCombo.getData(keyStoreCombo.getText());
				IKeyStore keyStore = (IKeyStore) keyStoreCombo.getData(keyStoreCombo.getText());
				try {
					sourcePassword = keyStore.getPasswordProvider().getKeyStorePassword(false);
				} catch (KeyStoreManagerException e1) {
					StudioLogger.error("Error while accessing keystore manager. " + e1.getMessage());
				}

				if (sourcePassword == null) {
					sourcePassword = ""; //$NON-NLS-1$
				}
				passwdText.setText(sourcePassword);
				loadEntries();
				updateTargetCombo();
				validateUi();
			}
		});

		Group targetGroup = new Group(container, SWT.NONE);
		targetGroup.setLayout(new GridLayout(2, false));
		targetGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		targetGroup.setText(CertificateManagerNLS.ImportKeyStoreDialog_Target_Group);

		Label targetKsLabel = new Label(targetGroup, SWT.NONE);
		targetKsLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		targetKsLabel.setText(CertificateManagerNLS.ImportKeyStoreDialog_KeyStore_Label);

		targetKsCombo = new Combo(targetGroup, SWT.READ_ONLY);
		targetKsCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		targetKsCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				super.widgetSelected(e);
				String selectedItem = targetKsCombo.getText();
				targetKeyStore = (IKeyStore) targetKsCombo.getData(selectedItem);
				validateUi();
			}
		});

		final KeyStoreManager keyStoreManager = KeyStoreManager.getInstance();
		try {
			List<IKeyStore> keyStores = keyStoreManager.getKeyStores();
			for (IKeyStore keyStore : keyStores) {
				File ksFile = keyStore.getFile();
				String comboItem = ksFile.getName() + " - " + ksFile.getAbsolutePath(); //$NON-NLS-1$
				keyStoreCombo.add(comboItem);
				keyStoreCombo.setData(comboItem, keyStore);
				if (keyStore.equals(this.sourceKeyStore)) {
					keyStoreCombo.select(keyStoreCombo.indexOf(comboItem));
				} else {
					targetKsCombo.add(comboItem);
					targetKsCombo.setData(comboItem, keyStore);
					if (keyStore.equals(this.targetKeyStore)) {
						targetKsCombo.select(targetKsCombo.indexOf(comboItem));
					}
				}
			}
		} catch (KeyStoreManagerException e1) {
			setErrorMessage(CertificateManagerNLS.ImportKeyStoreDialog_Error_Loading_Keystores);
		}

		return area;
	}

	@Override
	protected Control createHelpControl(Composite parent) {
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent.getShell(), HELP_ID);
		return super.createHelpControl(parent);
	}

	private void updateTargetCombo() {
		final KeyStoreManager keyStoreManager = KeyStoreManager.getInstance();
		try {
			targetKsCombo.clearSelection();
			targetKsCombo.setItems(new String[0]);
			List<IKeyStore> keyStores = keyStoreManager.getKeyStores();
			for (IKeyStore keyStore : keyStores) {
				if (keyStore != this.sourceKeyStore) {
					File ksFile = keyStore.getFile();
					String comboItem = ksFile.getName() + " - " + ksFile.getAbsolutePath(); //$NON-NLS-1$
					targetKsCombo.add(comboItem);
					targetKsCombo.setData(comboItem, keyStore);
					if (keyStore.equals(targetKeyStore)) {
						targetKsCombo.select(targetKsCombo.indexOf(comboItem));
					}
				}
			}
			if (targetKsCombo.getSelectionIndex() == -1) // nothing is selected.
			{
				targetKeyStore = null;
			}
		} catch (KeyStoreManagerException e1) {
			setErrorMessage(CertificateManagerNLS.ImportKeyStoreDialog_Error_Loading_Keystores);
		}

	}

	private void loadEntries() {
		try {
			aliasMap.clear();
			if (!sourcePassword.isEmpty()) {
				List<String> aliases = sourceKeyStore.getAliases(sourcePassword);
				entriesTableViewer.setInput(aliases);
			} else {
				entriesTableViewer.setInput(new ArrayList<String>());
			}
		} catch (KeyStoreManagerException e1) {
			setErrorMessage(CertificateManagerNLS.ImportKeyStoreDialog_Error_Loading_Entries);
			entriesTableViewer.setInput(new ArrayList<String>());
		} catch (InvalidPasswordException e1) {
			setErrorMessage(CertificateManagerNLS.ImportKeyStoreDialog_Invalid_Keystore_Passwd);
			entriesTableViewer.setInput(new ArrayList<String>());
		}
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
		if (isValid && (sourceKeyStore == null)) {
			isValid = false;
			setMessage(CertificateManagerNLS.ImportKeyStoreDialog_Select_Source_Ks);
		}
		if (isValid && ((sourcePassword == null) || sourcePassword.isEmpty())) {
			isValid = false;
			setMessage(CertificateManagerNLS.ImportKeyStoreDialog_Type_SourceKs_Passwd);
		}
		if (isValid) {
			try {
				if (!sourceKeyStore.isPasswordValid(sourcePassword)) {
					isValid = false;
					setErrorMessage("Wrong source keystore password.");
				}
			} catch (KeyStoreManagerException e) {
				isValid = false;
				setErrorMessage("Unable to access source keystore.\n" + e.getMessage());
			} catch (InvalidPasswordException e) {
				isValid = false;
				setErrorMessage("Wrong source keystore password.");
			}
		}
		if (isValid) {
			List<?> input = (List<?>) entriesTableViewer.getInput();
			if (input != null) {
				int itemCount = input.size();
				if (itemCount == 0) {
					isValid = false;
					setMessage(CertificateManagerNLS.ImportKeyStoreDialog_No_Entries_To_Import,
							IMessageProvider.WARNING);
				}
				if (entriesTableViewer.getCheckedElements().length == 0) {
					isValid = false;
					setMessage(CertificateManagerNLS.ImportKeyStoreDialog_No_Entries_To_Import,
							IMessageProvider.WARNING);
				} else {
					for (int i = 0; i < itemCount; i++) {
						EntryModel entryModel = (EntryModel) entriesTableViewer.getElementAt(i);
						if (entriesTableViewer.getChecked(entryModel) && !entryModel.isVerified()) {
							isValid = false;
							setMessage(CertificateManagerNLS.ImportKeyStoreDialog_Wrong_Entries_Passwd,
									IMessageProvider.WARNING);
							break;
						}
					}
				}
			} else {
				isValid = false;
				setMessage(CertificateManagerNLS.ImportKeyStoreDialog_No_Entries_To_Import, IMessageProvider.WARNING);
			}
		}
		if (isValid && (targetKeyStore == null)) {
			isValid = false;
			setMessage(CertificateManagerNLS.ImportKeyStoreDialog_Select_Target_Kesytore);
		}

		if (!isValid) {
			getButton(OK).setEnabled(false);
		} else {
			getButton(OK).setEnabled(true);
			setErrorMessage(null);
			setMessage(CertificateManagerNLS.ImportKeyStoreDialog_Default_Message);
		}
	}

	public IKeyStore getKeyStore() {
		return sourceKeyStore;
	}

	public Map<String, String> getAliases() {
		Map<String, String> selectedAliasMap = new HashMap<String, String>(selectedAlias.size());
		for (String alias : selectedAlias) {
			selectedAliasMap.put(alias, aliasMap.get(alias));
		}
		return selectedAliasMap;
	}

	public IKeyStore getTargetKeyStore() {
		return targetKeyStore;
	}

	public String getPassword() {
		return sourcePassword;
	}
}

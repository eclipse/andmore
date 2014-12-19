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

package org.eclipse.andmore.android.packaging.ui.export;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.UnrecoverableKeyException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;

import org.eclipse.andmore.android.AdtUtils;
import org.eclipse.andmore.android.AndroidPlugin;
import org.eclipse.andmore.android.SdkUtils;
import org.eclipse.andmore.android.certmanager.CertificateManagerActivator;
import org.eclipse.andmore.android.certmanager.core.KeyStoreManager;
import org.eclipse.andmore.android.certmanager.exception.InvalidPasswordException;
import org.eclipse.andmore.android.certmanager.exception.KeyStoreManagerException;
import org.eclipse.andmore.android.certmanager.job.CreateKeyJob;
import org.eclipse.andmore.android.certmanager.packaging.PackageFile;
import org.eclipse.andmore.android.certmanager.packaging.sign.PackageFileSigner;
import org.eclipse.andmore.android.certmanager.packaging.sign.SignException;
import org.eclipse.andmore.android.certmanager.ui.model.IKeyStore;
import org.eclipse.andmore.android.certmanager.ui.model.ITreeNode;
import org.eclipse.andmore.android.certmanager.ui.wizards.CreateKeyWizard;
import org.eclipse.andmore.android.certmanager.ui.wizards.CreateKeystoreWizard;
import org.eclipse.andmore.android.certmanager.ui.wizards.SelectExistentKeystoreWizard;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.common.log.UsageDataConstants;
import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.andmore.android.packaging.ui.PackagingUIPlugin;
import org.eclipse.andmore.android.packaging.ui.i18n.Messages;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jdt.internal.core.JavaProject;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * 
 * This Class is an area implementation for Package Export Wizards It contains
 * all UI and finish logics
 * 
 */
@SuppressWarnings("restriction")
public class PackageExportWizardArea {
	/**
	 * It holds the selection
	 */
	private final IStructuredSelection selection;

	/**
	 * The destination Folder selected by the user
	 */
	private Text destinationText;

	private Button selectAllButton;

	private Button deselectAllButton;

	private Button packageDestinationBrowseButton;

	private Button defaultDestination;

	private Button signCheckBox;

	private final Composite parentComposite;

	private final boolean signingEnabled;

	private final HashMap<IProject, Integer> projectSeverity;

	/**
	 * The tree which contains all descriptor files
	 */
	private Tree tree;

	private String message;

	private int severity;

	private boolean treeSelectionChanged;

	private final Image icon_ok, icon_nok;

	private Combo keystores;

	private Combo keysCombo;

	private Group signingGroup;

	private Button buttonAddKey;

	// Used in parallel with keystore combo. Use it with the selection index.
	private static ArrayList<IKeyStore> keystoreList = new ArrayList<IKeyStore>();

	// maps keystore->password
	private final Map<IKeyStore, String> keystorePasswords = new HashMap<IKeyStore, String>();

	private IKeyStore previousSelectedKeystore;

	private String previousSelectedKey;

	private Button buttonExisting;

	private Button buttonAddNew;

	/**
	 * Creates a new Export Area.
	 * 
	 * @param selection
	 *            The current Selection
	 */
	public PackageExportWizardArea(IStructuredSelection selection, Composite parent, boolean signingEnabled) {
		this.selection = normalizeSelection(selection);
		this.parentComposite = parent;
		this.signingEnabled = signingEnabled;
		this.projectSeverity = new HashMap<IProject, Integer>();
		validateProjects();
		ImageDescriptor adtProjectImageDescriptor = AbstractUIPlugin.imageDescriptorFromPlugin("org.eclipe.andmore", //$NON-NLS-1$
				"icons/android_project.png"); //$NON-NLS-1$
		ImageDescriptor errorImageDescriptor = JavaPluginImages.DESC_OVR_ERROR;
		ImageDescriptor projectImage = PlatformUI.getWorkbench().getSharedImages()
				.getImageDescriptor(org.eclipse.ui.ide.IDE.SharedImages.IMG_OBJ_PROJECT);
		// (IDecoration.TOP_LEFT, IDecoration.TOP_RIGHT,
		// IDecoration.BOTTOM_LEFT, IDecoration.BOTTOM_RIGHT and
		// IDecoration.UNDERLAY).
		ImageDescriptor[] overlays = new ImageDescriptor[5];
		overlays[1] = adtProjectImageDescriptor;
		icon_ok = new DecorationOverlayIcon(projectImage.createImage(), overlays).createImage();
		overlays[2] = errorImageDescriptor;
		icon_nok = new DecorationOverlayIcon(projectImage.createImage(), overlays).createImage();
		createControl(parent);
		this.treeSelectionChanged = false;
	}

	/**
	 * It opens the Directory Selection Dialog that allows the user enter the
	 * destination folder
	 * 
	 * @param originalPath
	 *            The Folder to show first
	 * 
	 * @return The entire path of the user choice
	 */
	private String directoryDialog(String originalPath) {
		DirectoryDialog directoryDialog;

		directoryDialog = new DirectoryDialog(parentComposite.getShell());

		File directory = new File(originalPath);

		if (directory.exists()) {
			directoryDialog.setFilterPath(directory.getPath());
		}

		String returnedPath = directoryDialog.open();

		return returnedPath;
	}

	/**
	 * This method normalize the selection only to contain projects and
	 * descriptors
	 * 
	 * @return
	 * @throws CoreException
	 */
	@SuppressWarnings("unchecked")
	private IStructuredSelection normalizeSelection(IStructuredSelection selection) {
		ArrayList<Object> normalized = new ArrayList<Object>();
		Iterator<Object> iterator = selection.iterator();
		while (iterator.hasNext()) {
			Object item = iterator.next();
			IResource resource = null;
			if (item instanceof IResource) {
				resource = (IResource) item;
			} else if (item instanceof IAdaptable) {
				try {
					resource = (IResource) ((IAdaptable) item).getAdapter(IResource.class);
				} catch (Exception e) {
					StudioLogger.warn("Error retrieving projects.");
				}
			}
			if (resource != null) {
				IProject project = resource.getProject();
				if (!normalized.contains(project)) {
					normalized.add(project);
				}
			}

		}
		return new StructuredSelection(normalized);
	}

	/**
	 * This method is responsible to add all the existent descriptor files of
	 * the current workspace in the tree shown in the wizard.
	 */
	private void populateTree() {
		tree.removeAll();
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		try {
			for (IProject project : projects) {
				if (project.isOpen() && (project.getNature(AndroidPlugin.Android_Nature) != null)
						&& !SdkUtils.isLibraryProject(project)) {
					TreeItem item = new TreeItem(tree, SWT.NONE);
					item.setData(project);
					item.setText(project.getName());
					item.setImage(projectSeverity.get(project) == IMessageProvider.ERROR ? icon_nok : icon_ok);
					if (selection.toList().contains(project)) {
						item.setChecked(true);
					}
				}
			}
		} catch (CoreException e) {
			StudioLogger.error(PackageExportWizardArea.class, "Error populating tree: " + e.getMessage()); //$NON-NLS-1$
		}

	}

	/**
	 * Create the destination selection group
	 * 
	 * @param mainComposite
	 *            : the parent composite
	 */
	private void createDestinationGroup(Composite mainComposite) {

		// create destination group
		Group destinationGroup = new Group(mainComposite, SWT.SHADOW_ETCHED_OUT);
		GridLayout layout = new GridLayout(3, false);
		destinationGroup.setLayout(layout);
		GridData defaultDestGridData = new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1);
		destinationGroup.setLayoutData(defaultDestGridData);
		destinationGroup.setText(Messages.PACKAGE_EXPORT_WIZARD_AREA_DESTINATION_LABEL);

		// Default Destination
		this.defaultDestination = new Button(destinationGroup, SWT.CHECK);
		defaultDestGridData = new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1);
		this.defaultDestination.setLayoutData(defaultDestGridData);
		this.defaultDestination.addListener(SWT.Selection, new DefaultDestinationListener());
		this.defaultDestination.setText(Messages.PACKAGE_EXPORT_WIZARD_USE_DEFAULT_DESTINATION);
		this.defaultDestination.setSelection(true);

		// Package Destination Label
		Label packageDestinationLabel = new Label(destinationGroup, SWT.NONE);
		packageDestinationLabel.setText(Messages.PACKAGE_EXPORT_WIZARD_PACKAGE_DESTINATION_LABEL);
		GridData folderGridData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		packageDestinationLabel.setLayoutData(folderGridData);

		// Package Destination
		this.destinationText = new Text(destinationGroup, SWT.SINGLE | SWT.BORDER);
		folderGridData = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		this.destinationText.setLayoutData(folderGridData);
		this.destinationText.setEnabled(!this.defaultDestination.getSelection());
		this.destinationText.addListener(SWT.Modify, new DestinationTextListener());

		// Browse Button
		this.packageDestinationBrowseButton = new Button(destinationGroup, SWT.PUSH);
		folderGridData = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
		this.packageDestinationBrowseButton.setLayoutData(folderGridData);
		this.packageDestinationBrowseButton.setText(Messages.PACKAGE_EXPORT_WIZARD_AREA_BROWSE_BUTTON_LABEL);
		this.packageDestinationBrowseButton.setEnabled(!this.defaultDestination.getSelection());
		this.packageDestinationBrowseButton.addListener(SWT.Selection, new PackageDestinationButtonListener());

	}

	public final String[] getKeys(IKeyStore iKeyStore) throws KeyStoreManagerException, InvalidPasswordException {
		List<String> aliases = new ArrayList<String>();

		aliases = iKeyStore.getAliases(getKeyStorePassword(iKeyStore));

		return aliases.toArray(new String[0]);
	}

	public String openNewKeyWizard(IKeyStore keyStore, IJobChangeListener createKeyJobListener) {

		CreateKeyWizard wizard = new CreateKeyWizard(keyStore, getKeyStorePassword(getSelectedKeyStore()),
				createKeyJobListener);

		WizardDialog dialog = new WizardDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), wizard);
		dialog.open();

		return wizard.getAlias();
	}

	private void selectKeystore(IKeyStore newKeystore) {
		if (newKeystore != null) {
			int index = keystoreList.indexOf(newKeystore);

			if (keystores.getItemCount() > index) {
				keystores.select(index);

				loadKeys(newKeystore);

				setEnablement(true);
			}
		} else {
			keystores.deselectAll();
		}

		previousSelectedKeystore = getSelectedKeyStore();
	}

	private void selectKeystoreWithoutLoadingKeys(IKeyStore newKeystore) {
		if (newKeystore != null) {
			int index = keystoreList.indexOf(newKeystore);
			keysCombo.removeAll();

			if (keystores.getItemCount() > index) {
				keystores.select(index);

				setEnablement(true);
			}
		} else {
			keystores.deselectAll();
		}

		previousSelectedKeystore = getSelectedKeyStore();
	}

	private boolean loadKeys(IKeyStore newKeystore) {
		boolean successfullyLoaded = true;
		keysCombo.removeAll();

		try {
			String[] keys = getKeys(newKeystore);
			if (keys != null) {
				keysCombo.setItems(keys);
			}
		} catch (Exception e) {
			successfullyLoaded = false;
			StudioLogger.info(PackageExportWizardArea.class,
					NLS.bind("Could not load keys for keystore: {0}", newKeystore.getFile() //$NON-NLS-1$
							.getAbsolutePath()));
		}

		selectKey(0);

		return successfullyLoaded;
	}

	private void selectKey(String key) {

		String[] keys = keysCombo.getItems();

		int index = -1;
		int i = 0;

		for (String k : keys) {

			if (k.equals(key)) {
				index = i;
				break;
			}
			i++;
		}

		selectKey(index);
	}

	private void selectKey(int index) {
		if ((index >= 0) && (index < keysCombo.getItemCount())) {
			keysCombo.select(index);
			setEnablement(true);
		} else {
			keysCombo.deselectAll();
		}

	}

	private void restorePreviousSelections() {
		selectKeystore(previousSelectedKeystore);
		selectKey(previousSelectedKey);
	}

	private String getSelectedKey() {
		String result = null;
		if (keysCombo.getSelectionIndex() >= 0) {
			result = keysCombo.getText();
		}
		return result;
	}

	protected IKeyStore getSelectedKeyStore() {
		IKeyStore result = null;
		if (keystores.getSelectionIndex() >= 0) {
			result = keystoreList.get(keystores.getSelectionIndex());
		}
		return result;
	}

	/**
	 * Create the sign selection group
	 * 
	 * @param mainComposite
	 *            : the parent composite
	 */
	private void createSignGroup(Composite mainComposite) {
		// Create signing group
		signingGroup = new Group(mainComposite, SWT.SHADOW_ETCHED_OUT);
		GridLayout layout = new GridLayout(4, false);
		GridData layoutData = new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1);
		signingGroup.setLayout(layout);
		signingGroup.setLayoutData(layoutData);
		signingGroup.setText(Messages.PACKAGE_EXPORT_WIZARD_AREA_SIGNING_TAB_TEXT);

		// Sign button/Check box
		this.signCheckBox = new Button(signingGroup, SWT.CHECK);
		layoutData = new GridData(SWT.LEFT, SWT.CENTER, true, false, 4, 1);
		this.signCheckBox.setLayoutData(layoutData);
		this.signCheckBox.setText(Messages.PACKAGE_EXPORT_WIZARD_AREA_SIGN_CHECK_LABEL);
		this.signCheckBox.addListener(SWT.Selection, new SignButtonListener());
		this.signCheckBox.setSelection(true);

		// --------------

		// Keystore label
		Label keystoreLabel = new Label(signingGroup, SWT.NONE);
		keystoreLabel.setText(Messages.PACKAGE_EXPORT_WIZARD_AREA_SIGN_KEYSTORE_LABEL);
		GridData gridData = new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 1, 1);
		keystoreLabel.setLayoutData(gridData);

		// Keystore combo
		this.keystores = new Combo(signingGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
		gridData = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gridData.widthHint = 250;
		this.keystores.setLayoutData(gridData);

		// populate mapped keystores from view
		populateKeystoresFromView();

		keystores.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IKeyStore selectedKeystore = keystoreList.get(keystores.getSelectionIndex());
				boolean keysLoaded = loadKeys(selectedKeystore);

				if (!keysLoaded) {
					ITreeNode keystoreNode = (ITreeNode) selectedKeystore;

					if (keystoreNode.getNodeStatus().getCode() == IKeyStore.WRONG_KEYSTORE_TYPE_ERROR_CODE) {
						EclipseUtils.showInformationDialog(
								Messages.PackageExportWizardArea_WrongKeystoreTypeDialogTitle,
								NLS.bind(Messages.PackageExportWizardArea_WrongKeystoreTypeDialogMessage,
										keystoreNode.getName()));
					}

					restorePreviousSelections();
				} else {
					previousSelectedKeystore = getSelectedKeyStore();
					previousSelectedKey = getSelectedKey();
				}
				setEnablement(true);
			}
		});

		if (keystores.getItemCount() <= 0) {
			signCheckBox.setSelection(false);
		}

		// Add keystore buttons
		buttonExisting = new Button(signingGroup, SWT.NONE);
		buttonAddNew = new Button(signingGroup, SWT.NONE);

		gridData = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
		buttonExisting.setLayoutData(gridData);
		gridData = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
		buttonAddNew.setLayoutData(gridData);

		buttonExisting.setText(Messages.PackageExportWizardArea_MenuItem_UseExistent);
		buttonExisting.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				openSelectKeystoreWizard();
			}
		});

		buttonAddNew.setText(Messages.PackageExportWizardArea_MenuItem_AddNew);
		buttonAddNew.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				CreateKeystoreWizard createKeystoreWizard = new CreateKeystoreWizard(new CreateKeystoreJobListener());

				WizardDialog dialog = new WizardDialog(parentComposite.getShell(), createKeystoreWizard);

				// open the wizard to create keystores
				dialog.create();
				if (dialog.open() == Window.OK) {
					// user really created keystore
					String keystorePassword = createKeystoreWizard.getCreatedKeystorePassword();
					addKeystore(createKeystoreWizard.getCreatedKeystoreNode(), true, keystorePassword);
					// required for case when just keystore is created, but no
					// key is created
					// DO NOT call selectKeystore here because it has loadKeys,
					// that may conflict with createKey (if a key is created),
					// for this case the CreateKeystoreJobListener will solve
					// the problem.
					selectKeystoreWithoutLoadingKeys(createKeystoreWizard.getCreatedKeystoreNode());
				}
			}
		});

		// Key label
		Label keyLabel = new Label(signingGroup, SWT.NONE);
		keyLabel.setText(Messages.PACKAGE_EXPORT_WIZARD_AREA_SIGN_KEY_LABEL);
		gridData = new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 1, 1);
		keyLabel.setLayoutData(gridData);

		// Key Combo
		this.keysCombo = new Combo(signingGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
		gridData = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
		this.keysCombo.setLayoutData(gridData);

		if (keysCombo.getItemCount() <= 0) {
			signCheckBox.setSelection(false);
		}

		keysCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				super.widgetSelected(e);

				previousSelectedKey = getSelectedKey();

				parentComposite.notifyListeners(SWT.Modify, new Event());
			}
		});

		// Add keystore button
		buttonAddKey = new Button(signingGroup, SWT.PUSH);
		buttonAddKey.setText(Messages.PackageExportWizardArea_AddKeyButton_Text);
		gridData = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
		buttonAddKey.setLayoutData(gridData);

		buttonAddKey.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				IKeyStore keyStore = null;
				if (keystores.getSelectionIndex() >= 0) {
					keyStore = PackageExportWizardArea.keystoreList.get(keystores.getSelectionIndex());
				}

				if (keyStore != null) {
					openNewKeyWizard(keyStore, new CreateKeyJobListener());
				}

				setEnablement(true);

			}
		});

		setEnablement(false);

	}

	/**
	 * This class is required because when creating a new key during export, the
	 * threads are not synchronized (createKey and loadKeys). Otherwise, wizard
	 * page could not finish accordingly.
	 */
	private class CreateKeyJobListener extends JobChangeAdapter {

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.core.runtime.jobs.JobChangeAdapter#done(org.eclipse.core
		 * .runtime.jobs.IJobChangeEvent)
		 */
		@Override
		public void done(final IJobChangeEvent event) {
			PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
				@Override
				public void run() {

					IKeyStore keyStore = ((CreateKeyJob) event.getJob()).getKeyStore();
					String key = ((CreateKeyJob) event.getJob()).getCreatedKeyAlias();

					loadKeys(keyStore);
					selectKey(key);
				}
			});
		}
	}

	/**
	 * This class is required because when creating a new key and new keystore
	 * during export, the threads are not synchronized (createKey and loadKeys).
	 * Otherwise, wizard page could not finish accordingly.
	 */
	private class CreateKeystoreJobListener extends JobChangeAdapter {

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.core.runtime.jobs.JobChangeAdapter#done(org.eclipse.core
		 * .runtime.jobs.IJobChangeEvent)
		 */
		@Override
		public void done(final IJobChangeEvent event) {
			PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
				@Override
				public void run() {
					IKeyStore keyStore = ((CreateKeyJob) event.getJob()).getKeyStore();
					String key = ((CreateKeyJob) event.getJob()).getCreatedKeyAlias();

					loadKeys(keyStore);
					selectKeystore(keyStore);
					selectKey(key);
				}
			});
		}
	}

	/**
	 * Inserts keystores mapped into keystore combo
	 */
	protected void populateKeystoresFromView() {
		try {
			// when reopening wizard, we need to clear the list
			if (!keystoreList.isEmpty()) {
				keystoreList.clear();
				keystores.removeAll();
			}
			if ((KeyStoreManager.getInstance() != null) && (KeyStoreManager.getInstance().getKeyStores() != null)) {
				List<IKeyStore> keyStores = KeyStoreManager.getInstance().getKeyStores();
				if (keyStores != null) {
					for (IKeyStore keyStore : keyStores) {
						insertKeystoreIntoCombo(keyStore);
					}
				}
			}
		} catch (KeyStoreManagerException e) {
			StudioLogger.error(PackageExportWizardArea.class, "Error retrieving keystore list", //$NON-NLS-1$
					e);
		}
	}

	protected void insertKeystoreIntoCombo(IKeyStore iKeyStore) {
		File ksFile = iKeyStore.getFile();
		keystores.add(ksFile.getName() + " - ( " + ksFile.getPath() + " )"); //$NON-NLS-1$ //$NON-NLS-2$
		keystoreList.add(iKeyStore);
	}

	/**
	 * Adds keystore to keystores combo box and model from GUI
	 * 
	 * @param iKeyStore
	 * @param canSavePassword
	 *            true if create/select and need to import into view, false if
	 *            selecting and does NOT need to import into the view
	 * @param password
	 *            to retrieve keys
	 */
	protected void addKeystore(IKeyStore iKeyStore, boolean canSavePassword, String password) {
		keystorePasswords.put(iKeyStore, password);
		insertKeystoreIntoCombo(iKeyStore);
	}

	/**
	 * Update the Default Destination The behavior is: if only one project is
	 * selected, then default destination text is set to the folder of this
	 * project if have more than one project selected or none than default
	 * destination text is set to workspace root
	 */
	private void updateDefaultDestination() {
		ArrayList<IProject> selectedProjects = getSelectedProjects();
		if (this.defaultDestination.getSelection()) {
			if ((selectedProjects != null) && (selectedProjects.size() == 1)) {
				this.destinationText.setText(selectedProjects.get(0).getLocation().toOSString());
			} else {
				this.destinationText.setText(ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString());
			}
		}
	}

	/**
	 * Get selected projects from the tree
	 * 
	 * @return
	 */
	private ArrayList<IProject> getSelectedProjects() {
		ArrayList<IProject> projects = new ArrayList<IProject>();
		for (TreeItem item : tree.getItems()) {
			if (item.getChecked()) {
				projects.add((IProject) item.getData());
			}
		}
		return projects;
	}

	/**
	 * Get selected items from the tree
	 * 
	 * @return
	 */
	private ArrayList<TreeItem> getSelectedItems() {
		ArrayList<TreeItem> items = new ArrayList<TreeItem>();
		for (TreeItem item : tree.getItems()) {
			if (item.getChecked()) {
				items.add(item);
			}
		}
		return items;
	}

	/**
	 * This method creates the entire structure of the wizard page. Also, it
	 * stores the user input.
	 * 
	 * @param parent
	 *            Composite for all the elements.
	 */
	public void createControl(Composite parent) {
		Composite mainComposite = new Composite(parent, SWT.NONE);
		GridLayout gridLayout = new GridLayout(3, false);
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1);
		mainComposite.setLayout(gridLayout);
		mainComposite.setLayoutData(gridData);

		// Tree structure
		this.tree = new Tree(mainComposite, SWT.CHECK | SWT.MULTI | SWT.V_SCROLL | SWT.BORDER);
		gridData = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 2);
		gridData.heightHint = 150;
		this.tree.setLayoutData(gridData);
		this.tree.addListener(SWT.Selection, new TreeListener());

		// Add all the descriptor files to the tree
		populateTree();

		// Select All Button
		this.selectAllButton = new Button(mainComposite, SWT.PUSH);
		gridData = new GridData(SWT.FILL, SWT.UP, false, false, 1, 1);
		this.selectAllButton.setLayoutData(gridData);
		this.selectAllButton.setText(Messages.PACKAGE_EXPORT_WIZARD_AREA_SELECT_ALL_BUTTON);
		this.selectAllButton.addListener(SWT.Selection, new TreeSelectionButtonListener(true));

		// Deselect All button
		this.deselectAllButton = new Button(mainComposite, SWT.PUSH);
		gridData = new GridData(SWT.FILL, SWT.UP, false, false, 1, 1);
		this.deselectAllButton.setLayoutData(gridData);
		this.deselectAllButton.setText(Messages.PACKAGE_EXPORT_WIZARD_AREA_DESELECT_ALL_BUTTON);
		this.deselectAllButton.addListener(SWT.Selection, new TreeSelectionButtonListener(false));

		createDestinationGroup(mainComposite);

		if (this.signingEnabled) {
			createSignGroup(mainComposite);
		}

		String path = ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString();
		Object prj = this.selection.getFirstElement();
		if ((prj != null) && (this.selection.size() == 1)) {
			if (prj instanceof IProject) {
				String realPath = ((IProject) prj).getLocation().toOSString();
				this.destinationText.setText(realPath);
			} else if (prj instanceof IResource) {
				String realPath = path + ((IResource) prj).getProject().getFullPath().toOSString();
				this.destinationText.setText(realPath);
			}
		} else {
			this.destinationText.setText(path);
		}
		/**
		 * Force the focus to parent. This action make help ok
		 */
		if (!parent.isFocusControl()) {
			parent.forceFocus();
		}
	}

	/**
	 * Get all projects severities to avoid user selects erroneous projects
	 */
	private void validateProjects() {
		try {
			for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
				if (project.isOpen()) {
					int sev = project.findMaxProblemSeverity(null, true, IResource.DEPTH_INFINITE);
					int projectSev;
					switch (sev) {
					case IMarker.SEVERITY_ERROR:
						projectSev = IMessageProvider.ERROR;
						break;
					case IMarker.SEVERITY_INFO:
						projectSev = IMessageProvider.INFORMATION;
						break;
					case IMarker.SEVERITY_WARNING:
						projectSev = IMessageProvider.WARNING;
						break;
					default:
						projectSev = IMessageProvider.NONE;
						break;
					}
					projectSeverity.put(project, new Integer(projectSev));
				}
			}
		} catch (CoreException e) {
			StudioLogger.error(PackageExportWizardArea.class, "Impossible to get project severity"); //$NON-NLS-1$
		}
	}

	/**
	 * Can finish used in {@link IWizardPage} This method validate the page and
	 * change the severity/message.
	 * 
	 * @return true if can finish this wizard, false otherwise
	 */
	public boolean canFinish() {
		String messageAux = null;
		int severity_aux = IMessageProvider.NONE;

		/*
		 * Check is has selected items
		 */
		if (!hasItemChecked()) {
			messageAux = Messages.SELECTOR_MESSAGE_NO_SELECTION;
			if (treeSelectionChanged) {
				severity_aux = IMessageProvider.ERROR;
			} else {
				severity_aux = IMessageProvider.INFORMATION;
			}
		}

		// validate if some selected project has errors
		if (messageAux == null) {
			Iterator<IProject> iterator = getSelectedProjects().iterator();
			while (iterator.hasNext() && (severity_aux != IMessageProvider.ERROR)) {
				severity_aux = projectSeverity.get(iterator.next());
			}
			if (severity_aux == IMessageProvider.ERROR) {
				messageAux = Messages.PACKAGE_EXPORT_WIZARD_AREA_PROJECTS_WITH_ERRORS_SELECTED;

			}
		}

		/*
		 * Check if the selected location is valid, even if non existent.
		 */
		IPath path = new Path(this.destinationText.getText());

		if (!this.defaultDestination.getSelection() && (messageAux == null)) {
			// Test if path is blank, to warn user instead of show an error
			// message
			if (this.destinationText.getText().equals("")) //$NON-NLS-1$
			{
				messageAux = Messages.SELECTOR_MESSAGE_LOCATION_ERROR_INVALID;
				severity_aux = IMessageProvider.INFORMATION;
			}

			/*
			 * Do Win32 Validation
			 */
			if ((messageAux == null) && Platform.getOS().equalsIgnoreCase(Platform.OS_WIN32)) {
				// test path size
				if (path.toString().length() > 255) {
					messageAux = Messages.SELECTOR_MESSAGE_LOCATION_ERROR_PATH_TOO_LONG;
					severity_aux = IMessageProvider.ERROR;
				}
				String device = path.getDevice();
				File deviceFile = null;
				if (device != null) {
					deviceFile = new File(path.getDevice());
				}

				if ((device != null) && !deviceFile.exists()) {
					messageAux = Messages.SELECTOR_MESSAGE_LOCATION_ERROR_INVALID_DEVICE + " [" + device //$NON-NLS-1$
							+ "]"; //$NON-NLS-1$
					severity_aux = IMessageProvider.ERROR;
				}

			}
			// test if path is absolute
			if (messageAux == null) {
				if (!path.isAbsolute()) {
					messageAux = Messages.SELECTOR_MESSAGE_LOCATION_ERROR_INVALID;
					severity_aux = IMessageProvider.ERROR;
				}
			}

			if (messageAux == null) {
				for (String folderName : path.segments()) {
					if (!ResourcesPlugin.getWorkspace().validateName(folderName, IResource.FOLDER).isOK()) {
						messageAux = Messages.SELECTOR_MESSAGE_LOCATION_ERROR_INVALID;
						severity_aux = IMessageProvider.ERROR;

					}
				}
			}

			if ((messageAux == null) && path.toFile().exists() && !path.toFile().isDirectory()) {
				messageAux = Messages.SELECTOR_MESSAGE_LOCATION_ERROR_NOT_DIRECTORY;
				severity_aux = IMessageProvider.ERROR;
			}
		}

		/*
		 * Check if there are available certificates and if selection isn't null
		 */
		if (messageAux == null) {

			if (this.signingEnabled && (this.signCheckBox != null) && this.signCheckBox.getSelection()
					&& !((this.keystores != null) && (this.keystores.getItemCount() > 0))) {
				messageAux = Messages.PACKAGE_EXPORT_WIZARD_AREA_SIGN_NO_KEYSTORE_AVAILABLE;
				severity_aux = IMessageProvider.ERROR;
			}

			else if (this.signCheckBox.getSelection()
					&& !((this.keysCombo != null) && (this.keysCombo.getItemCount() > 0)
							&& (this.keysCombo.getSelectionIndex() >= 0)
							&& (this.keysCombo.getItem(this.keysCombo.getSelectionIndex()) != null) && !this.keysCombo
							.getItem(this.keysCombo.getSelectionIndex()).equals(""))) //$NON-NLS-1$ 
			{
				messageAux = Messages.PACKAGE_EXPORT_WIZARD_AREA_SIGN_NO_KEYSTORE_OR_KEY_SELECTED;
				severity_aux = IMessageProvider.ERROR;
			}

		}

		if (messageAux == null) {
			if (!this.signCheckBox.getSelection()) {
				messageAux = Messages.PACKAGE_EXPORT_WIZARD_AREA_UNSIGNEDPACKAGE_WARNING;
				severity_aux = IMessageProvider.WARNING;
			}
		}

		/*
		 * Setting message
		 */
		if (messageAux == null) {
			messageAux = Messages.PACKAGE_EXPORT_WIZARD_AREA_DESCRIPTION;
			severity_aux = IMessageProvider.NONE;
		}
		this.message = messageAux;
		this.severity = severity_aux;

		boolean result;
		switch (severity_aux) {
		case IMessageProvider.ERROR:
			// ERROR. can't finish wizard
			result = false;
			break;

		case IMessageProvider.WARNING:
			// WARNING. ok to finish the wizard
			result = true;
			break;

		case IMessageProvider.INFORMATION:
			// INFORMATION. Path is empty, so it's NOT OK to finish the wizard
			result = false;
			break;

		default:
			// by default, canFinish returns true
			result = true;
			break;

		}

		return result;
	}

	/**
	 * Check if we have at least one item checked
	 * 
	 * @param items
	 * @return true if some of tree item is checked, false otherwise
	 */
	private boolean hasItemChecked() {
		boolean checked = false;
		TreeItem[] items = tree.getItems();
		int i = 0;
		while (!checked && (i < items.length)) {
			if (items[i].getChecked()) {
				checked = true;
			}
			i++;
		}
		return checked;
	}

	/**
	 * Check if the destination folder is valid during finish action If the
	 * package don't exist, ask to create
	 * 
	 * @return true if the destination is valid
	 * @throws CoreException
	 *             when errors with directory creation occurs
	 */
	private boolean checkDestination() throws CoreException {
		boolean destinationOK = true;
		if (!defaultDestination.getSelection()) {
			File destination = new File(destinationText.getText());
			if (!destination.exists()) {
				destinationOK = createDestinationFolderDialog();
				if (destinationOK) {
					if (!destination.mkdirs()) {
						throw new CoreException(new Status(IStatus.ERROR, PackagingUIPlugin.PLUGIN_ID,
								Messages.PACKAGE_EXPORT_WIZARD_AREA_ERROR_DESTINATION_CHECK));
					}
				}
			}
		} else {
			for (TreeItem item : getSelectedItems()) {
				IProject project = (IProject) item.getData();
				IPath dist = project.getLocation().append(CertificateManagerActivator.PACKAGE_PROJECT_DESTINATION);
				File file = dist.toFile();
				if (file.exists() && !file.isDirectory()) {
					if (!file.delete()) {
						throw new CoreException(new Status(IStatus.ERROR, PackagingUIPlugin.PLUGIN_ID,
								Messages.PACKAGE_EXPORT_WIZARD_AREA_ERROR_DESTINATION_CHECK));
					}
				}
				if (!file.exists()) {
					if (!file.mkdir()) {
						throw new CoreException(new Status(IStatus.ERROR, PackagingUIPlugin.PLUGIN_ID,
								Messages.PACKAGE_EXPORT_WIZARD_AREA_ERROR_DESTINATION_CHECK));
					}
				}
				project.refreshLocal(IResource.DEPTH_ONE, new NullProgressMonitor());
			}
		}
		return destinationOK;
	}

	/**
	 * Create a destination folder, asking user's permission
	 * 
	 * @return
	 */
	private boolean createDestinationFolderDialog() {
		MessageBox box = new MessageBox(parentComposite.getShell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO);
		box.setMessage(Messages.PACKAGE_EXPORT_WIZARD_AREA_CREATE_DIRECTORIES_BOX_MESSAGE);
		box.setText(Messages.PACKAGE_EXPORT_WIZARD_AREA_CREATE_DIRECTORIES_BOX_TITLE);
		return box.open() == SWT.YES;
	}

	/**
	 * do the finish: Create the package for each selected descriptor
	 */
	public boolean performFinish() {
		final boolean[] finished = { false };
		boolean destOK = false;
		final MultiStatus status = new MultiStatus(PackagingUIPlugin.PLUGIN_ID, IStatus.OK, "", null); //$NON-NLS-1$
		ProgressMonitorDialog monitorDialog = null;
		String DESCRIPTION_TO_LOG = UsageDataConstants.DESCRIPTION_DEFAULT;

		try {
			destOK = checkDestination();
		} catch (CoreException e) {
			status.add(e.getStatus());
		}

		if (destOK) {
			monitorDialog = new ProgressMonitorDialog(parentComposite.getShell());
			try {
				monitorDialog.run(false, false, new IRunnableWithProgress() {

					@Override
					public void run(IProgressMonitor aMonitor) throws InvocationTargetException, InterruptedException {
						int finishSize = getSelectedItems().size() * PackagingUIPlugin.PROGRESS_MONITOR_MULTIPLIER;
						SubMonitor monitor = SubMonitor.convert(aMonitor);
						monitor.beginTask(Messages.PACKAGE_EXPORT_WIZARD_AREA_FINISH_ACTION_LABEL, finishSize);
						IPath exportDestinationFolder = new Path(destinationText.getText());
						IPath exportDestinationFile = null;

						for (TreeItem item : getSelectedItems()) {
							// get the eclipse project as a java project to get
							// the project
							// destination
							IProject eclipseProject = (IProject) item.getData();
							try {
								monitor.worked(PackagingUIPlugin.PROGRESS_MONITOR_MULTIPLIER / 4);

								JavaProject javaProject = new JavaProject();
								javaProject.setProject(eclipseProject);

								// find all packages built by Android builder
								Map<String, String> apkConfigurations = SdkUtils
										.getAPKConfigurationsForProject(eclipseProject);

								Set<String> apkConfNames = new HashSet<String>();
								if (apkConfigurations != null) {
									apkConfNames.addAll(apkConfigurations.keySet());
								}
								apkConfNames.add(""); // the default package //$NON-NLS-1$

								SubMonitor submonitor = monitor
										.newChild(PackagingUIPlugin.PROGRESS_MONITOR_MULTIPLIER / 4);

								submonitor.beginTask(Messages.PACKAGE_EXPORT_WIZARD_AREA_EXPORTING_ACTION_LABEL, 3
										* PackagingUIPlugin.PROGRESS_MONITOR_MULTIPLIER * apkConfNames.size());

								for (String apkConfName : apkConfNames) {

									String apkName = eclipseProject.getName()
											+ (apkConfName.isEmpty() ? apkConfName : "-" //$NON-NLS-1$ 
													+ apkConfName);
									if (defaultDestination.getSelection()) {
										exportDestinationFolder = eclipseProject.getLocation().append(
												CertificateManagerActivator.PACKAGE_PROJECT_DESTINATION);
									}
									exportDestinationFile = exportDestinationFolder.append(apkName).addFileExtension(
											CertificateManagerActivator.PACKAGE_EXTENSION);
									File file = exportDestinationFile.toFile();
									submonitor.worked(PackagingUIPlugin.PROGRESS_MONITOR_MULTIPLIER);

									// always export unsigned package
									AdtUtils.exportUnsignedReleaseApk(javaProject.getProject(), file, submonitor);

									submonitor.worked(PackagingUIPlugin.PROGRESS_MONITOR_MULTIPLIER);

									if (signCheckBox.getSelection()) {
										// sign the package if required
										IStatus signStatus = signPackage(eclipseProject, file);
										status.add(signStatus);
									}

									// zipalign the file and we are done
									// exporting the package
									PackageFile.zipAlign(file);

									submonitor.worked(PackagingUIPlugin.PROGRESS_MONITOR_MULTIPLIER);
								}
								submonitor.done();
							} catch (CoreException e) {
								StudioLogger.error(PackageExportWizardArea.class,
										"Error while building project or getting project output folder" //$NON-NLS-1$
												+ eclipseProject.getName(), e);
								status.add(new Status(IStatus.ERROR, PackagingUIPlugin.PLUGIN_ID,
										Messages.PACKAGE_EXPORT_WIZARD_AREA_ERROR_PROJECT_BUILD
												+ " " + eclipseProject.getName())); //$NON-NLS-1$
							} finally {
								try {
									eclipseProject.refreshLocal(IResource.DEPTH_INFINITE,
											monitor.newChild(PackagingUIPlugin.PROGRESS_MONITOR_MULTIPLIER / 4));
								} catch (CoreException e) {
									// do nothing
								}
							}
							monitor.worked(PackagingUIPlugin.PROGRESS_MONITOR_MULTIPLIER / 4);
						}
						finished[0] = true;

					}
				});
			} catch (Exception e) {
				StudioLogger.warn("Error finishing package export.");
			}
		}

		if (!status.isOK()) {
			status.getMessage();
			DESCRIPTION_TO_LOG = Messages.PACKAGE_EXPORT_WIZARD_AREA_READONLY_TITLE;
			ErrorDialog.openError(parentComposite.getShell(), Messages.PACKAGE_EXPORT_WIZARD_AREA_READONLY_TITLE,
					Messages.PACKAGE_EXPORT_WIZARD_AREA_READONLY_MESSAGE, status);
		}

		// Saving usage data
		try {
			StudioLogger.collectUsageData(UsageDataConstants.WHAT_APP_MANAGEMENT_PACKAGE,
					UsageDataConstants.KIND_APP_MANAGEMENT, DESCRIPTION_TO_LOG, PackagingUIPlugin.PLUGIN_ID,
					PackagingUIPlugin.getDefault().getBundle().getVersion().toString());
		} catch (Throwable e) {
			// Do nothing, but error on the log should never prevent app from
			// working
		}

		return finished[0];
	}

	/**
	 * 
	 * @return key entry password
	 */
	public String getKeyEntryPassword() {
		String keyEntryPassword = new String();
		try {
			keyEntryPassword = getSelectedKeyStore().getPasswordProvider().getPassword(
					this.keysCombo.getItem(this.keysCombo.getSelectionIndex()), true);
		} catch (KeyStoreManagerException e) {
			StudioLogger.error(this.getClass(), "Error retrieving keys entry password", e); //$NON-NLS-1$
		}
		return keyEntryPassword;
	}

	/**
	 * @param eclipseProject
	 *            The project being exported.
	 * @param exportedPackage
	 *            The package to be signed
	 * @throws InvalidPasswordException
	 * @throws KeyStoreManagerException
	 */
	private IStatus signPackage(IProject eclipseProject, File exportedPackage) {
		IStatus status = Status.OK_STATUS;

		String keyAlias = keysCombo.getItem(keysCombo.getSelectionIndex());
		String keystorePassword = getKeyStorePassword(getSelectedKeyStore());
		String keyPassword = getKeyEntryPassword();

		JarFile jar = null;
		try {
			PackageFile pack = null;
			boolean keepTrying;
			if (keyPassword != null) {
				keepTrying = true;
			} else {
				keepTrying = false;

			}
			while (keepTrying) {
				try {
					// Open package and remove signature
					jar = new JarFile(exportedPackage);
					pack = new PackageFile(jar);
					pack.removeMetaEntryFiles();

					// Sign the new package
					PackageFileSigner.signPackage(pack, getSelectedKeyStore().getEntry(keyAlias, keystorePassword),
							keyPassword, PackageFileSigner.MOTODEV_STUDIO);
					keepTrying = false;
				} catch (UnrecoverableKeyException sE) {
					try {
						keyPassword = getSelectedKeyStore().getPasswordProvider().getPassword(keyAlias, true, false);
					} catch (KeyStoreManagerException e) {
						status = new Status(IStatus.ERROR, CertificateManagerActivator.PLUGIN_ID, e.getMessage());
						StudioLogger.error(PackageExportWizardArea.this.getClass(),
								"Could not retrieve key password on export: " + e.getMessage()); //$NON-NLS-1$
					}
					if (keyPassword == null) {
						keepTrying = false;
						status = Status.CANCEL_STATUS;
					} else {
						keepTrying = true;
					}
				} catch (InvalidPasswordException e) {
					// Should never happen as the entry alias is only available
					// if the keystore password
					// was typed correctly.
					// Unless the user changed the keystore password outside the
					// tool while exporting the package.
					status = new Status(IStatus.ERROR, CertificateManagerActivator.PLUGIN_ID, e.getMessage());
				} catch (KeyStoreManagerException e) {
					// Should never happen as the entry alias is only available
					// if the keystore password
					// was typed correctly.
					// Unless the user changed the keystore password outside the
					// tool while exporting the package.
					status = new Status(IStatus.ERROR, CertificateManagerActivator.PLUGIN_ID, e.getMessage());
				}
			}

			if (status.isOK()) {
				FileOutputStream fileToWrite = null;
				try {
					// Write the new package file
					fileToWrite = new FileOutputStream(exportedPackage);
					pack.write(fileToWrite);
				} finally {

					fileToWrite.close();
				}
			} else {
				EclipseUtils.showErrorDialog("Package Signing", "Could not sign the package.");
			}
		} catch (IOException e) {
			StudioLogger
					.error(PackageExportWizardArea.this.getClass(), "Could not sign the package: " + e.getMessage()); //$NON-NLS-1$

			status = new Status(IStatus.ERROR, PackagingUIPlugin.PLUGIN_ID,
					Messages.PackageExportWizardArea_ErrorWritingSignedPackageFile + " " //$NON-NLS-1$
							+ eclipseProject.getName());
		} catch (SignException e) {
			StudioLogger
					.error(PackageExportWizardArea.this.getClass(), "Could not sign the package: " + e.getMessage()); //$NON-NLS-1$

			status = new Status(IStatus.ERROR, PackagingUIPlugin.PLUGIN_ID,
					Messages.PackageExportWizardArea_ErrorSigningPackage + " " + eclipseProject.getName()); //$NON-NLS-1$
		} finally {
			try {
				jar.close();
			} catch (IOException e) {
				StudioLogger.error(PackageExportWizardArea.this.getClass(),
						"Could not sign the package: " + e.getMessage()); //$NON-NLS-1$
			}
		}
		return status;
	}

	/**
	 * Get the area message
	 * 
	 * @return the message for the wizard
	 */
	public String getMessage() {
		return this.message;
	}

	/**
	 * Get the area error severity
	 * 
	 * @return
	 */
	public int getSeverity() {
		return this.severity;
	}

	/**
	 * "Browser..." button for package destination.
	 * 
	 */
	private class PackageDestinationButtonListener implements Listener {

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets
		 * .Event)
		 */
		@Override
		public void handleEvent(Event event) {
			String path = destinationText.getText();

			if (path.equals("")) //$NON-NLS-1$
			{
				path = ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString().subSequence(0, 2).toString();
			}

			String executablePath = directoryDialog(path);

			if ((executablePath != null) && !executablePath.equals("")) //$NON-NLS-1$
			{
				destinationText.setText(executablePath);
			}
			parentComposite.notifyListeners(SWT.Modify, new Event());
		}
	}

	/**
	 * Update the default destination status
	 * 
	 */
	private class DestinationTextListener implements Listener {

		@Override
		public void handleEvent(Event event) {
			if (!defaultDestination.getSelection()) {
				parentComposite.notifyListeners(SWT.Modify, new Event());
			}
		}
	}

	/**
	 * A Listener for the Tree.
	 * 
	 */
	private class TreeListener implements Listener {
		@Override
		public void handleEvent(Event event) {
			updateDefaultDestination();
			parentComposite.notifyListeners(SWT.Modify, new Event());
			treeSelectionChanged = true;
		}
	}

	/**
	 * Enable Destination Text.
	 * 
	 */
	private class DefaultDestinationListener implements Listener {

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets
		 * .Event)
		 */
		@Override
		public void handleEvent(Event event) {
			Button defaultDestination = (Button) event.widget;
			destinationText.setEnabled(!defaultDestination.getSelection());
			packageDestinationBrowseButton.setEnabled(!defaultDestination.getSelection());
			if (defaultDestination.getSelection()) {
				updateDefaultDestination();
			} else {
				destinationText.setText(""); //$NON-NLS-1$
			}
			destinationText.notifyListeners(SWT.Modify, new Event());
			parentComposite.notifyListeners(SWT.Modify, new Event());

		}

	}

	/**
	 * 
	 * @param notify
	 *            Notifies listener that validates if Finish can be enabled
	 */
	private void setEnablement(boolean notify) {

		boolean signEnabled = signCheckBox.getSelection();

		keysCombo.setEnabled(signEnabled);
		keystores.setEnabled(signEnabled);
		buttonAddKey.setEnabled(signEnabled);
		// toolbar.setEnabled(signEnabled);
		buttonExisting.setEnabled(signEnabled);
		buttonAddNew.setEnabled(signEnabled);

		if (signEnabled) {

			if (keystores.getSelectionIndex() < 0) {
				// no keystore selected, clear keys combo
				buttonAddKey.setEnabled(false);
				keysCombo.setEnabled(false);
				keysCombo.removeAll();
			}

			if (keystores.getItemCount() <= 0) {
				keysCombo.setEnabled(false);
				keystores.setEnabled(false);
				buttonAddKey.setEnabled(false);
				// toolbar.setEnabled(true);
				buttonExisting.setEnabled(signEnabled);
				buttonAddNew.setEnabled(signEnabled);
			}
		}

		if (notify) {
			parentComposite.notifyListeners(SWT.Modify, new Event());
		}

	}

	protected void openSelectKeystoreWizard() {
		SelectExistentKeystoreWizard selectExistentKeystoreWizard = new SelectExistentKeystoreWizard();

		WizardDialog dialog = new WizardDialog(parentComposite.getShell(), selectExistentKeystoreWizard);

		// open the wizard to select keystores
		dialog.create();
		if (dialog.open() == Window.OK) {
			// keystore was really selected : adding keystore to the list
			IKeyStore iKeyStore = selectExistentKeystoreWizard.getSelectedKeystore();
			boolean canSavePassword = selectExistentKeystoreWizard.canSavePassword();
			String password = selectExistentKeystoreWizard.getPassword();
			addKeystore(iKeyStore, canSavePassword, password);
			selectKeystore(iKeyStore);
		}
	}

	/**
	 * This method must be used to retrieve the passwod of any keystore in the
	 * context of this wizard. It is purpose is to cache the passwords of the
	 * keystores, mainly of the ones that do not have its password saved, so the
	 * password will be asked only once for each keystore, during the lifetime
	 * of this wizard.
	 * */
	protected String getKeyStorePassword(IKeyStore keystore) {
		String password = keystorePasswords.get(keystore);

		// check if password is already cached
		if (password == null) {
			password = keystore.getKeyStorePassword(true);
		}
		if (password != null) {
			keystorePasswords.put(keystore, password);
		}

		return password;
	}

	/**
	 * Sign button listener to enable/disable combos, buttons and finish/next
	 * 
	 */
	private class SignButtonListener implements Listener {
		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets
		 * .Event)
		 */
		@Override
		public void handleEvent(Event event) {
			if (event.widget == signCheckBox) {
				setEnablement(true);
			}

		}

	}

	/**
	 * This class will handle the (Des)Select All buttons
	 * 
	 */
	private class TreeSelectionButtonListener implements Listener {
		private final boolean checked;

		/**
		 * Create a new instance of the listener with the desired check state
		 * 
		 * @param selectItems
		 */
		public TreeSelectionButtonListener(boolean selectItems) {
			this.checked = selectItems;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets
		 * .Event)
		 */
		@Override
		public void handleEvent(Event event) {
			for (TreeItem item : tree.getItems()) {
				item.setChecked(checked);
			}
			updateDefaultDestination();
			parentComposite.notifyListeners(SWT.Modify, new Event());
			treeSelectionChanged = true;
		}

	}
}
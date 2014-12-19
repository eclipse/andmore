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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.andmore.android.certmanager.CertificateManagerActivator;
import org.eclipse.andmore.android.certmanager.i18n.CertificateManagerNLS;
import org.eclipse.core.internal.resources.Folder;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.navigator.ResourceComparator;

@SuppressWarnings("restriction")
public class RemoveExternalPackageSignaturePage extends WizardPage {
	private Text sourceDirText = null;

	private Button browseDirButton = null;

	private Button workspaceDirButton = null;

	private Tree packagesTree = null;

	private Button selectAllButton = null;

	private Button deselectAllButton = null;

	private WizardSelection selection = null;

	protected Composite mainComposite = null;

	/**
	 * Create a new wizard page based on selection
	 * 
	 * @param pageName
	 *            the page name
	 * @param selection
	 *            the selection
	 */
	public RemoveExternalPackageSignaturePage(String pageName, IStructuredSelection selection) {
		super(pageName);
		setDescription(CertificateManagerNLS.UNSIGN_EXTERNAL_PKG_WIZARD_DESCRIPTION);
		setTitle(CertificateManagerNLS.UNSIGN_EXTERNAL_PKG_WIZARD_WINDOW_TITLE);
		this.selection = new WizardSelection(selection);
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
		this.mainComposite = new Composite(parent, SWT.NULL);
		// create new layout with 3 columns of different sizes
		GridLayout layout = new GridLayout(4, false);
		this.mainComposite.setLayout(layout);

		Label sourceDirLabel = new Label(this.mainComposite, SWT.NONE);
		sourceDirLabel.setText(CertificateManagerNLS.SIGN_EXTERNAL_PKG_WIZARD_SOURCE_DIR_LABEL);

		GridData layoutData = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
		sourceDirLabel.setLayoutData(layoutData);

		this.sourceDirText = new Text(this.mainComposite, SWT.BORDER);
		layoutData = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		this.sourceDirText.setLayoutData(layoutData);
		this.sourceDirText.addListener(SWT.Modify, new SourceDirectoryTextListener());

		this.browseDirButton = new Button(this.mainComposite, SWT.PUSH);
		this.browseDirButton.setText(CertificateManagerNLS.SIGN_EXTERNAL_PKG_WIZARD_FILESYSTEM);
		layoutData = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
		this.browseDirButton.setLayoutData(layoutData);
		this.browseDirButton.addListener(SWT.Selection, new BrowseButtonListener());

		this.workspaceDirButton = new Button(this.mainComposite, SWT.PUSH);
		this.workspaceDirButton.setText(CertificateManagerNLS.SIGN_EXTERNAL_PKG_WIZARD_WORKSPACE);
		layoutData = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
		this.workspaceDirButton.setLayoutData(layoutData);
		this.workspaceDirButton.addListener(SWT.Selection, new WorkspaceButtonListener());

		createExtendedArea(this.mainComposite);

		createPackageTreeLabel();

		this.packagesTree = new Tree(this.mainComposite, SWT.BORDER | SWT.CHECK | SWT.V_SCROLL);
		layoutData = new GridData(SWT.FILL, SWT.FILL, true, true, 3, 2);
		layoutData.heightHint = 150;
		this.packagesTree.setLayoutData(layoutData);
		this.packagesTree.addListener(SWT.Selection, new TreeSelectionListener());

		Composite selectionButtons = new Composite(this.mainComposite, SWT.FILL);
		layoutData = new GridData(SWT.FILL, SWT.TOP, false, true, 1, 2);
		selectionButtons.setLayoutData(layoutData);
		FillLayout row = new FillLayout(SWT.VERTICAL);
		row.spacing = 3;
		selectionButtons.setLayout(row);

		this.selectAllButton = new Button(selectionButtons, SWT.PUSH);
		this.selectAllButton.setText(CertificateManagerNLS.PACKAGE_EXPORT_WIZARD_AREA_SELECT_ALL_BUTTON);
		SelectionButtonsListener selectionButtonsListener = new SelectionButtonsListener();
		this.selectAllButton.addListener(SWT.Selection, selectionButtonsListener);

		this.deselectAllButton = new Button(selectionButtons, SWT.PUSH);
		this.deselectAllButton.setText(CertificateManagerNLS.PACKAGE_EXPORT_WIZARD_AREA_DESELECT_ALL_BUTTON);
		this.deselectAllButton.addListener(SWT.Selection, selectionButtonsListener);

		this.sourceDirText.setText(this.selection.getSelectedDirectory());
		populateTree(this.selection.getSelectedPackages());
		updatePageComplete();
		setControl(this.mainComposite);
	}

	protected void createPackageTreeLabel() {
		GridData layoutData;
		Label packagesLabel = new Label(this.mainComposite, SWT.NONE);
		packagesLabel.setText(CertificateManagerNLS.RemoveExternalPackageSignaturePage_Package_Tree_Label);
		layoutData = new GridData(SWT.FILL, SWT.CENTER, false, false, 4, 1);
		packagesLabel.setLayoutData(layoutData);
	}

	/**
	 * Create a composite area after the basic composite Subclasses that need
	 * more than the basic package selection screen should override this method
	 * 
	 * @param mainComposite
	 */
	protected void createExtendedArea(Composite parent) {
		PlatformUI.getWorkbench().getHelpSystem()
				.setHelp(parent, CertificateManagerActivator.UNSIGN_EXTERNAL_PKG_WIZARD_CONTEXT_HELP_ID);
	}

	/**
	 * Populates the tree with the packages of base dir Requires a valid folder
	 * set as source dir
	 */
	private void populateTree(List<String> selection) {
		File sourceDir = getSourcePath().toFile();
		Color gray = new Color(null, 130, 130, 130);
		this.packagesTree.removeAll();
		if (sourceDir.isDirectory() && sourceDir.canWrite()) {
			File[] list = sourceDir.listFiles();
			for (File file : list) {
				if (file.canRead() && file.isFile() && file.getName().endsWith("apk")) //$NON-NLS-1$
				{
					TreeItem fileItem = new TreeItem(this.packagesTree, SWT.NONE);
					String text = file.getName();
					if (!file.canWrite()) {
						text += " [" + CertificateManagerNLS.READ_ONLY_TEXT //$NON-NLS-1$
								+ "]"; //$NON-NLS-1$
						fileItem.setForeground(gray);
					}

					fileItem.setText(text);
					fileItem.setData(file);
					if ((selection != null) && selection.contains(file.getName()) && file.canWrite()) {
						fileItem.setChecked(true);
					}
				}
			}
		}
	}

	/**
	 * Validates if the source directory is valid one
	 * 
	 * @return true if the source dir text is valid, false otherwise
	 */
	private boolean isSourceDirValid() {

		String messageAux = null;
		int severity = IMessageProvider.NONE;

		/*
		 * Check if the selected location is valid, even if non existent.
		 */
		IPath path = new Path(this.sourceDirText.getText());

		// Test if path is blank, to warn user instead of show an error message
		if (this.sourceDirText.getText().equals("")) //$NON-NLS-1$
		{
			messageAux =

			CertificateManagerNLS.SIGN_EXTERNAL_PKG_WIZARD_SOURCE_DIR_EMPTY;
			severity = IMessageProvider.INFORMATION;
		}

		/*
		 * Do Win32 Validation
		 */
		if ((messageAux == null) && Platform.getOS().equalsIgnoreCase(Platform.OS_WIN32)) {
			// test path size
			if (path.toString().length() > 255) {
				messageAux =

				CertificateManagerNLS.SELECTOR_MESSAGE_LOCATION_ERROR_PATH_TOO_LONG;
				severity = IMessageProvider.WARNING;
			}
			String device = path.getDevice();
			File deviceFile = null;
			if (device != null) {
				deviceFile = new File(path.getDevice());
			}

			if ((device != null) && !deviceFile.exists()) {
				messageAux =

				CertificateManagerNLS.SELECTOR_MESSAGE_LOCATION_ERROR_INVALID_DEVICE + " [" + device + "]";
				severity = IMessageProvider.ERROR;
			}

		}
		// test if path is absolute
		if (messageAux == null) {
			if (!path.isAbsolute() || !path.toFile().exists()) {
				messageAux =

				CertificateManagerNLS.SIGN_EXTERNAL_PKG_WIZARD_SOURCE_DIR_INVALID;
				severity = IMessageProvider.ERROR;
			}
		}

		if (messageAux == null) {
			for (String folderName : path.segments()) {
				if (!ResourcesPlugin.getWorkspace().validateName(folderName, IResource.FOLDER).isOK()) {
					messageAux =

					CertificateManagerNLS.SIGN_EXTERNAL_PKG_WIZARD_SOURCE_DIR_INVALID;
					severity = IMessageProvider.ERROR;

				}
			}
		}

		if ((messageAux == null) && ((path.toFile().exists() && !path.toFile().isDirectory()))) {
			messageAux =

			CertificateManagerNLS.SIGN_EXTERNAL_PKG_WIZARD_SOURCE_DIR_NOT_DIRECTORY;
			severity = IMessageProvider.ERROR;

		}

		/*
		 * Setting message
		 */
		if (messageAux == null) {
			messageAux = CertificateManagerNLS.SIGN_EXTERNAL_PKG_WIZARD_DESCRIPTION;
			severity = IMessageProvider.NONE;
		}
		setMessage(messageAux, severity);
		return severity == IMessageProvider.NONE;
	}

	/**
	 * @return the path of base dir where packages are located
	 */
	public IPath getSourcePath() {
		return new Path(this.sourceDirText.getText());
	}

	/**
	 * 
	 * @return the list with selected packages
	 */
	public List<String> getSelectedPackages() {
		ArrayList<String> selected = new ArrayList<String>();
		for (TreeItem item : this.packagesTree.getItems()) {
			if (item.getChecked()) {
				selected.add(item.getData().toString());
			}
		}

		return selected;
	}

	/**
	 * Update the page status, validating each field of this page Subclasses
	 * that overrides createExtendedArea method should override this method too
	 * to validate the new fields
	 */
	public void updatePageComplete() {

		String messageAux = null;
		int severity = IMessageProvider.NONE;

		/*
		 * Check if there are available certificates and if selection isn't null
		 */
		if (isSourceDirValid()) {
			if (this.packagesTree.getItemCount() == 0) {
				messageAux =

				CertificateManagerNLS.SIGN_EXTERNAL_PKG_WIZARD_NO_AVAILABLE_PACKAGES;
				severity = IMessageProvider.ERROR;
			}
		} else {
			messageAux = getMessage();
			severity = getMessageType();
		}

		if ((messageAux == null) && (getSelectedPackages().size() == 0)) {
			messageAux =

			CertificateManagerNLS.UNSIGN_EXTERNAL_PKG_WIZARD_NO_PACKAGES_SELECTED;
			severity = IMessageProvider.INFORMATION;
		}

		if (messageAux == null) {
			messageAux =

			CertificateManagerNLS.UNSIGN_EXTERNAL_PKG_WIZARD_DESCRIPTION;
			severity = IMessageProvider.NONE;
		}

		setMessage(messageAux, severity);
		setPageComplete(severity == IMessageProvider.NONE);

	}

	/**
	 * This class implements the listener of filesystem button, opening the
	 * browse window and updating the dir text
	 */
	class BrowseButtonListener implements Listener {
		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets
		 * .Event)
		 */
		@Override
		public void handleEvent(Event event) {
			DirectoryDialog dialog = new DirectoryDialog(
					RemoveExternalPackageSignaturePage.this.mainComposite.getShell());
			dialog.setFilterPath(!RemoveExternalPackageSignaturePage.this.sourceDirText.getText().trim().equals("") ? RemoveExternalPackageSignaturePage.this.sourceDirText
					.getText() : null);
			String path = dialog.open();
			if (path != null) {
				RemoveExternalPackageSignaturePage.this.sourceDirText.setText(path);
				populateTree(null);
				updatePageComplete();
			}
		}

	}

	/**
	 * This class implements the listener of workspace button, opening the
	 * browse window and updating the dir text
	 */
	class WorkspaceButtonListener implements Listener {
		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets
		 * .Event)
		 */
		@Override
		public void handleEvent(Event event) {
			ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(getShell(),
					new WorkbenchLabelProvider(), new WorkbenchContentProvider());
			dialog.setTitle(CertificateManagerNLS.SIGN_EXTERNAL_PKG_WIZARD_WORKSPACE_SIMPLE);
			dialog.setMessage(CertificateManagerNLS.SIGN_EXTERNAL_PKG_WIZARD_CHOOSE);

			// set the workspace as the limit
			dialog.setInput(ResourcesPlugin.getWorkspace().getRoot());
			dialog.setComparator(new ResourceComparator(ResourceComparator.NAME));

			// don't display files
			dialog.addFilter(new ViewerFilter() {

				@Override
				public boolean select(Viewer viewer, Object parentElement, Object element) {
					boolean filtered = false;

					if (element instanceof IFile) {
						filtered = true;
					}
					return !filtered;
				}
			});

			// user can select only one folder
			dialog.setValidator(new ISelectionStatusValidator() {

				@Override
				public IStatus validate(Object[] selection) {
					IStatus valid = new Status(IStatus.ERROR, CertificateManagerActivator.PLUGIN_ID, ""); //$NON-NLS-1$
					if (selection.length == 1) {
						if (selection[0] instanceof Folder) {
							valid = new Status(IStatus.OK, CertificateManagerActivator.PLUGIN_ID, ""); //$NON-NLS-1$
						}
					}
					return valid;
				}
			});

			String path = null;
			if (dialog.open() == IDialogConstants.OK_ID) {
				Folder resource = (Folder) dialog.getFirstResult();
				path = resource.getLocation().toString();
			}

			if (path != null) {
				RemoveExternalPackageSignaturePage.this.sourceDirText.setText(path);
				populateTree(null);
				updatePageComplete();
			}
		}
	}

	/**
	 * Listener to validate any SourceDirectory text Change
	 */
	class SourceDirectoryTextListener implements Listener {
		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets
		 * .Event)
		 */
		@Override
		public void handleEvent(Event event) {
			RemoveExternalPackageSignaturePage.this.packagesTree.removeAll();
			if (isSourceDirValid()) {
				populateTree(null);
			}
			updatePageComplete();
		}

	}

	/**
	 * This class handles clicks on select all and deselect all buttons
	 */
	class SelectionButtonsListener implements Listener {

		/**
		 * Check/Uncheck all items
		 * 
		 * @param check
		 *            : true for check, false for unckeck
		 */
		private void setCheckedAll(boolean check) {
			for (TreeItem item : RemoveExternalPackageSignaturePage.this.packagesTree.getItems()) {
				item.setChecked(check);
			}
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
			if (event.widget == RemoveExternalPackageSignaturePage.this.selectAllButton) {
				setCheckedAll(true);
			} else if (event.widget == RemoveExternalPackageSignaturePage.this.deselectAllButton) {
				setCheckedAll(false);
			}
			updatePageComplete();
		}

	}

	/**
	 * Listener to update wizard status according tree selection
	 */
	class TreeSelectionListener implements Listener {
		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets
		 * .Event)
		 */
		@Override
		public void handleEvent(Event event) {
			updatePageComplete();
		}

	}

	/**
	 * This class gets the workspace selection and makes a suitable selection to
	 * the wizard
	 */
	class WizardSelection {
		private ArrayList<String> packages = null;

		private IPath directory = null;

		public WizardSelection(IStructuredSelection selection) {
			this.packages = new ArrayList<String>();
			Iterator<?> iterator = selection.iterator();
			while (iterator.hasNext()) {
				Object obj = iterator.next();
				if (obj instanceof IFile) {
					IFile file = (IFile) obj;

					if (file.getLocation().getFileExtension().equals("apk")) //$NON-NLS-1$
					{
						if (this.directory == null) {
							this.directory = file.getLocation().removeLastSegments(1);
							this.packages.add(file.getName());
						} else {
							if (file.getLocation().matchingFirstSegments(this.directory) == this.directory
									.segmentCount()) {
								this.packages.add(file.getName());
							}
						}
					}
				} else if (obj instanceof IFolder) {
					if (this.directory == null) {
						this.directory = ((IFolder) obj).getLocation();
					}
				}
			}
			if (this.directory == null) {
				this.directory = new Path(""); //$NON-NLS-1$
			}
		}

		/**
		 * 
		 * @return the selected directory
		 */
		public String getSelectedDirectory() {
			return this.directory.toOSString();
		}

		/**
		 * 
		 * @return the selected packages
		 */
		public List<String> getSelectedPackages() {
			return this.packages;
		}

	}
}

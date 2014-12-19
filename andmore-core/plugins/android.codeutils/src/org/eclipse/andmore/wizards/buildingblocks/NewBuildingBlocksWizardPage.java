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
package org.eclipse.andmore.wizards.buildingblocks;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.andmore.android.codeutils.CodeUtilsActivator;
import org.eclipse.andmore.android.codeutils.i18n.CodeUtilsNLS;
import org.eclipse.andmore.android.common.IAndroidConstants;
import org.eclipse.andmore.android.common.exception.AndroidException;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.common.utilities.AndroidUtils;
import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.andmore.android.manifest.AndroidProjectManifestFile;
import org.eclipse.andmore.android.model.BuildingBlockModel;
import org.eclipse.andmore.android.model.manifest.AndroidManifestFile;
import org.eclipse.andmore.android.wizards.elements.AddRemoveButtons;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaElementComparator;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.StandardJavaElementContentProvider;
import org.eclipse.jdt.ui.wizards.NewContainerWizardPage;
import org.eclipse.jdt.ui.wizards.NewTypeWizardPage;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;

/**
 * Abstract class used to create the building block wizard main pages.
 */
public abstract class NewBuildingBlocksWizardPage extends NewTypeWizardPage {
	private static final String JAVA_EXTENSION = ".java"; //$NON-NLS-1$

	private static final int MAX_PATH_SIZE = 255;

	protected static String LABEL = TYPENAME + ".LABEL"; //$NON-NLS-1$

	protected IWorkspaceRoot fWorkspaceRoot = ResourcesPlugin.getWorkspace().getRoot();

	private BuildingBlockModel buildBlock;

	private Text labelText;

	private Button defaultLabelButton;

	private AddRemoveButtons addRemovePermissionsButtons;

	private List activityPermissions;

	private final Set<String> intentFilterPermissions = new HashSet<String>();

	private MethodCreationControl methodCreationControl;

	/**
	 * Listener to check if the wizard can be opened.
	 */
	private class WizardShellListener implements ShellListener {
		private boolean wasChecked = false;

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.swt.events.ShellListener#shellActivated(org.eclipse.swt
		 * .events.ShellEvent)
		 */
		@Override
		public void shellActivated(ShellEvent e) {
			if (!wasChecked) {
				wasChecked = true;

				if (!canOpen()) {
					((Shell) e.widget).close();
				}
			}

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.swt.events.ShellListener#shellClosed(org.eclipse.swt.
		 * events.ShellEvent)
		 */
		@Override
		public void shellClosed(ShellEvent e) {
			// Do nothing
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.swt.events.ShellListener#shellDeactivated(org.eclipse
		 * .swt.events.ShellEvent)
		 */
		@Override
		public void shellDeactivated(ShellEvent e) {
			// Do nothing
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.swt.events.ShellListener#shellDeiconified(org.eclipse
		 * .swt.events.ShellEvent)
		 */
		@Override
		public void shellDeiconified(ShellEvent e) {
			// Do nothing
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.swt.events.ShellListener#shellIconified(org.eclipse.swt
		 * .events.ShellEvent)
		 */
		@Override
		public void shellIconified(ShellEvent e) {
			// Do nothing
		}

	}

	/*
	 * Each building block is represent by a class (e.g., an Activity or
	 * Service). Each of these classes contain some methods that must be
	 * overridden by subclasses in order to be called by android framework
	 * (e.g., onCreate(...) methods). This class is responsible to create check
	 * boxes that let users choose which of these methods should be
	 * automatically created by the wizard.
	 */
	private class MethodCreationControl {
		private Label stubMessage;

		private Button[] stubButtonArray;

		MethodCreationControl(Composite parent, Method[] methods) {
			if (methods != null) {
				if (methods.length > 0) {
					stubMessage = new Label(parent, SWT.NONE);
					stubMessage.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false, 4, 1));
					stubMessage.setText(CodeUtilsNLS.UI_NewBuildingBlocksWizardPage_QuestionWhichMethodCreate);
				}

				createStubsComponent(parent, methods);
			}
		}

		/*
		 * Creates a single method declaration to the wizard page
		 * 
		 * @param parent The wizard page composite
		 * 
		 * @param method The method to add
		 */
		private void createStubsComponent(Composite parent, Method[] methods) {
			stubButtonArray = new Button[methods.length];
			int i = 0;
			for (final Method method : methods) {
				new Label(parent, SWT.NONE);
				final Button stubsButton = new Button(parent, SWT.CHECK);
				stubsButton.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false, 3, 1));
				stubsButton.setText(method.getMessage());
				stubsButton.addListener(SWT.Selection, new Listener() {
					@Override
					public void handleEvent(Event event) {
						method.handle(stubsButton.getSelection());
					}
				});
				stubButtonArray[i++] = stubsButton;
			}
		}

		public void setMethodCreationControlEnabled(boolean enabled) {
			stubMessage.setEnabled(enabled);
			for (Button stubButton : stubButtonArray) {
				stubButton.setEnabled(enabled);
			}
		}
	}

	/**
	 * Default constructor.
	 * 
	 * @param buildBlock
	 *            The building block model that the wizard will create.
	 * @param pageName
	 *            The page name.
	 */
	protected NewBuildingBlocksWizardPage(BuildingBlockModel buildBlock, String pageName) {
		super(true, pageName);
		this.buildBlock = buildBlock;
		setTitle(getWizardTitle());
		setDescription(getDefaultMessage());
		setPageComplete(false);
	}

	/**
	 * Gets the help ID to be used for attaching context sensitive help.
	 * 
	 * Classes that extends this class and want to set their own help should
	 * override this method.
	 */
	protected abstract String getHelpId();

	/**
	 * Returns the wizard title.
	 * 
	 * @return the wizard title.
	 */
	public abstract String getWizardTitle();

	/**
	 * Returns the wizard default status message.
	 * 
	 * @return the wizard default status message.
	 */
	public abstract String getDefaultMessage();

	/**
	 * Returns all methods that the building block can override.
	 * 
	 * @return all methods that the building block can override.
	 */
	protected abstract Method[] getMethods();

	/**
	 * @param enabled
	 *            If true, all available methods in the building block will be
	 *            checked for automatic creation.
	 * */
	public void setMethodCreationControlEnabled(boolean enabled) {
		methodCreationControl.setMethodCreationControlEnabled(enabled);
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
		initializeDialogUnits(parent);

		// main control
		Composite mainComposite = new Composite(parent, SWT.FILL);
		mainComposite.setLayout(new FillLayout(SWT.FILL));
		final ScrolledComposite scroll = new ScrolledComposite(mainComposite, SWT.H_SCROLL | SWT.V_SCROLL);

		final Composite composite = new Composite(scroll, SWT.NONE);
		composite.setFont(parent.getFont());

		int nColumns = 4;

		createSampleControls(composite, nColumns);

		GridLayout layout = new GridLayout(nColumns, false);
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));

		if (hasHeader()) {
			setPackageFragmentRoot(getBuildBlock().getPackageFragmentRoot(), true);
			createContainerControls(composite, nColumns);
			setPackageFragment(getBuildBlock().getPackageFragment(), true);
			createPackageControls(composite, nColumns);

			createSeparator(composite, nColumns);

			createTypeNameControls(composite, nColumns);

			createLabelControls(composite);

			setSuperClass(getBuildBlock().getSuperClass(), getBuildBlock().useExtendedClass());
			createSuperClassControls(composite, nColumns);

			createPermissionControls(composite);

			createIntermediateControls(composite);

			createMethodCreationControl(composite, getMethods());
		}
		createExtendedControls(composite);

		// set up scroll
		scroll.setContent(composite);

		scroll.setExpandHorizontal(true);
		scroll.setExpandVertical(true);

		scroll.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				scroll.setMinSize(composite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			}
		});

		setControl(mainComposite);
		Dialog.applyDialogFont(mainComposite);

		mainComposite.getShell().addShellListener(new WizardShellListener());
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, getHelpId());
		PlatformUI.getWorkbench().getHelpSystem().setHelp(mainComposite, getHelpId());
	}

	/**
	 * Override this class to create the label controls.
	 * 
	 * @param parent
	 *            The wizard page composite
	 */
	protected void createLabelControls(Composite parent) {
		Label label = new Label(parent, SWT.NONE);
		label.setText(CodeUtilsNLS.UI_NewBuildingBlocksWizardPage_TextLabel);

		labelText = new Text(parent, SWT.BORDER);
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 2;
		labelText.setLayoutData(gridData);
		labelText.setEnabled(false);

		defaultLabelButton = new Button(parent, SWT.CHECK);
		defaultLabelButton.setText(CodeUtilsNLS.UI_NewBuildingBlocksWizardPage_ButtonNameDefault);
		defaultLabelButton.setSelection(true);

		Listener listener = new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (!defaultLabelButton.getSelection() || !event.widget.equals(labelText)) {
					handleFieldChanged(LABEL);
				}
				if (event.widget.equals(defaultLabelButton) && !defaultLabelButton.getSelection()
						&& labelText.isEnabled()) {
					labelText.forceFocus();
					labelText.selectAll();
				}
			}
		};

		labelText.addListener(SWT.Modify, listener);
		defaultLabelButton.addListener(SWT.Selection, listener);
	}

	/**
	 * Override this class to add samples control.
	 * 
	 * @param composite
	 *            The wizard page composite
	 */
	protected void createSampleControls(Composite composite, int nColumns) {
		// default implementation does nothing
	}

	/**
	 * Override this class to add components after superclass control.
	 * 
	 * @param composite
	 *            The wizard page composite
	 */
	protected void createIntermediateControls(Composite composite) {
		// default implementation does nothing
	}

	/**
	 * Return all Filter Permissions as an Array.
	 * 
	 * @return
	 */
	public String[] getIntentFilterPermissionsAsArray() {
		return intentFilterPermissions.toArray(new String[intentFilterPermissions.size()]);
	}

	/**
	 * Creates the "Permissions" section on the wizard.
	 * 
	 * @param composite
	 *            the wizard composite
	 */
	protected void createPermissionControls(Composite composite) {
		GridData gridData;
		Label activityPermissionsLabel = new Label(composite, SWT.NONE);
		activityPermissionsLabel.setText(CodeUtilsNLS.NewBuildingBlocksWizardPage_PermissionLabel);
		gridData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		gridData.verticalAlignment = GridData.BEGINNING;
		activityPermissionsLabel.setLayoutData(gridData);

		activityPermissions = new List(composite, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		activityPermissions.setItems(getBuildBlock().getIntentFilterPermissionsAsArray());
		gridData = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
		gridData.heightHint = convertHeightInCharsToPixels(3);
		activityPermissions.setLayoutData(gridData);

		addRemovePermissionsButtons = new AddRemoveButtons(composite);
		setButtonLayoutData(addRemovePermissionsButtons.getAddButton());
		setButtonLayoutData(addRemovePermissionsButtons.getRemoveButton());
		addRemovePermissionsButtons.getAddButton().addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				Set<String> permissionSet = new HashSet<String>(Arrays.asList(AndroidUtils
						.getIntentFilterPermissions(getBuildBlock().getProject())));

				permissionSet.removeAll(getBuildBlock().getIntentFilterPermissions());

				FilteredActionsSelectionDialog dialog = new FilteredActionsSelectionDialog(getShell(), permissionSet);
				dialog.setInitialPattern("**"); //$NON-NLS-1$
				dialog.setTitle("Select an action permission"); //$NON-NLS-1$
				dialog.setMessage(CodeUtilsNLS.UI_NewLauncherWizardPage_CategorySelectionDialogMessage);

				if (Window.OK == dialog.open()) {
					for (Object result : dialog.getResult()) {
						getBuildBlock().addIntentFilterPermissions((String) result);
					}
					activityPermissions.setItems(getBuildBlock().getIntentFilterPermissionsAsArray());
					addRemovePermissionsButtons.getRemoveButton().setEnabled(
							activityPermissions.getSelectionCount() > 0);
					updateStatus(getBuildBlock().getStatus());
				}
			}
		});
		addRemovePermissionsButtons.getRemoveButton().addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				for (int selection : activityPermissions.getSelectionIndices()) {
					getBuildBlock().removeIntentFilterPermissions(activityPermissions.getItem(selection));
				}
				activityPermissions.setItems(getBuildBlock().getIntentFilterPermissionsAsArray());
				addRemovePermissionsButtons.getRemoveButton().setEnabled(activityPermissions.getSelectionCount() > 0);
				updateStatus(getBuildBlock().getStatus());
			}
		});
		addRemovePermissionsButtons.getRemoveButton().setEnabled(activityPermissions.getSelectionCount() > 0);
		activityPermissions.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				addRemovePermissionsButtons.getRemoveButton().setEnabled(activityPermissions.getSelectionCount() > 0);
			}
		});
	}

	/**
	 * Override this class to add components at the end of the Page.
	 * 
	 * @param parent
	 *            The wizard page composite
	 */
	protected void createExtendedControls(Composite parent) {
		// default implementation does nothing
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jdt.ui.wizards.NewElementWizardPage#setVisible(boolean)
	 */
	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			setFocus();
		}
	}

	/**
	 * Returns true if page has header false otherwise.
	 * 
	 * @return true if page has header false otherwise.
	 */
	public boolean hasHeader() {
		return true;
	}

	/**
	 * Adds the methods that a building block can override to the wizard page.
	 * 
	 * @param parent
	 *            The wizard page composite.
	 * @param methods
	 *            The methods to add to the wizard.
	 */
	protected void createMethodCreationControl(Composite parent, Method[] methods) {
		methodCreationControl = new MethodCreationControl(parent, methods);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jdt.ui.wizards.NewContainerWizardPage#chooseContainer()
	 */
	@Override
	protected IPackageFragmentRoot chooseContainer() {
		IJavaElement initElement = getPackageFragmentRoot();

		ISelectionStatusValidator validator = new ElementTreeValidator();
		ViewerFilter filter = new ElementTreeViewFilter();
		StandardJavaElementContentProvider provider = new ElementTreeContentProvider();

		ILabelProvider labelProvider = new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT);
		ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(getShell(), labelProvider, provider);
		dialog.setComparator(new JavaElementComparator());
		dialog.setValidator(validator);
		dialog.setTitle(CodeUtilsNLS.UI_NewBuildingBlocksWizardPage_WizardTitle);
		dialog.setMessage(CodeUtilsNLS.UI_NewBuildingBlocksWizardPage_MessageChooseFolder);
		dialog.setInput(JavaCore.create(fWorkspaceRoot));
		dialog.setInitialSelection(initElement);
		dialog.addFilter(filter);
		dialog.setHelpAvailable(false);

		IPackageFragmentRoot rootSelection = null;
		if (dialog.open() == Window.OK) {
			Object element = dialog.getFirstResult();
			if (element instanceof IJavaProject) {
				IJavaProject jproject = (IJavaProject) element;
				rootSelection = jproject.getPackageFragmentRoot(jproject.getProject());
			} else if (element instanceof IPackageFragmentRoot) {
				rootSelection = (IPackageFragmentRoot) element;
			}
		}
		return rootSelection;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jdt.ui.wizards.NewTypeWizardPage#handleFieldChanged(java.
	 * lang.String)
	 */
	@Override
	protected void handleFieldChanged(String fieldName) {
		if (NewTypeWizardPage.TYPENAME.equals(fieldName)) {
			getBuildBlock().setName(getTypeName());
			getBuildBlock().setNameStatus(typeNameChanged());
			getBuildBlock().setPackageStatus(packageChanged());
		} else if (NewContainerWizardPage.CONTAINER.equals(fieldName)) {
			// source folder
			getBuildBlock().setPackageFragmentRoot(getPackageFragmentRoot());
			getBuildBlock().setPackageFragmentRootStatus(containerChanged());
			getBuildBlock().setPackageStatus(packageChanged());
			getBuildBlock().setNameStatus(typeNameChanged());

			updatePackage(getPackageFragmentRoot());
		} else if (NewTypeWizardPage.PACKAGE.equals(fieldName)) {
			if (getPackageFragmentRoot() != null) {
				getBuildBlock().setPackageFragment(getPackageFragmentRoot().getPackageFragment(getPackageText()));
			}
			getBuildBlock().setPackageStatus(packageChanged());
			getBuildBlock().setNameStatus(typeNameChanged());
		} else if (LABEL.equals(fieldName)) {
			getBuildBlock().setLabelStatus(labelChanged());
		}
		updateStatus(getBuildBlock().getStatus());
	}

	private void updatePackage(IPackageFragmentRoot packageFragmentRoot) {
		if (packageFragmentRoot != null) {
			IJavaProject project = null;
			IPackageFragment pack = null;

			project = packageFragmentRoot.getJavaProject();
			try {
				pack = EclipseUtils.getDefaultPackageFragment(project);
				getBuildBlock().setPackageFragment(pack);
			} catch (JavaModelException e) {
				StudioLogger.error(NewBuildingBlocksWizardPage.class, "Error getting default package fragment.", e); //$NON-NLS-1$
				// do nothing
			}
			setPackageFragment(pack, true);
			handleFieldChanged(NewTypeWizardPage.PACKAGE);
		}
	}

	/**
	 * @return A status indicating if the building block label property has been
	 *         change.
	 */
	protected IStatus labelChanged() {
		IStatus status = new Status(IStatus.OK, CodeUtilsActivator.PLUGIN_ID, null);
		if ((defaultLabelButton != null) && (labelText != null)) {
			if (defaultLabelButton.getSelection()) {
				labelText.setText(""); //$NON-NLS-1$

			}
			labelText.setEnabled(!defaultLabelButton.getSelection());
			getBuildBlock().setLabel(getLabel());
		}
		return status;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jdt.ui.wizards.NewTypeWizardPage#typeNameChanged()
	 */
	@Override
	protected IStatus typeNameChanged() {
		IStatus superStatus = super.typeNameChanged();
		IStatus status = new Status(superStatus.getSeverity(), CodeUtilsActivator.PLUGIN_ID, superStatus.getMessage());

		Pattern pattern = Pattern.compile("([A-Za-z0-9_]+)"); //$NON-NLS-1$

		if (superStatus.getSeverity() != IStatus.ERROR) {
			Matcher matcher = pattern.matcher(getTypeName());

			if (!matcher.matches() || !matcher.group().equals(getTypeName())) {
				String errMsg = NLS.bind(CodeUtilsNLS.ERR_NewBuildingBlocksWizardPage_InvalidTypeName, getTypeName());

				status = new Status(IStatus.ERROR, CodeUtilsActivator.PLUGIN_ID, errMsg);
			} else if (packageAndClassExist()) {
				status = new Status(IStatus.ERROR, CodeUtilsActivator.PLUGIN_ID,
						CodeUtilsNLS.ERR_NewBuildingBlocksWizardPage_PackageAndClassAlreadyExist);
			} else if (isTooLongOnFileSystem()) {
				status = new Status(IStatus.ERROR, CodeUtilsActivator.PLUGIN_ID,
						CodeUtilsNLS.ERR_NewBuildingBlocksWizardPage_FileNameTooLong);
			}
		}

		labelChanged();
		return status;
	}

	/**
	 * Returns the building block label property value.
	 * 
	 * @return the building block label property value.
	 */
	protected String getLabel() {
		String label;
		if (defaultLabelButton.getSelection()) {
			label = ""; //$NON-NLS-1$
		} else {
			label = labelText.getText();
		}
		return label;
	}

	/**
	 * Returns the building block model
	 * 
	 * @return the building block model
	 */
	public BuildingBlockModel getBuildBlock() {
		return buildBlock;
	}

	public void setBuildBlock(BuildingBlockModel buildBlock) {
		this.buildBlock = buildBlock;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jdt.ui.wizards.NewTypeWizardPage#packageChanged()
	 */
	@Override
	protected IStatus packageChanged() {
		IStatus superStatus = super.packageChanged();
		IStatus status = new Status(superStatus.getSeverity(), CodeUtilsActivator.PLUGIN_ID, superStatus.getMessage());

		// The package name is being get by getPackageText because the method
		// getPackageFragment
		// (from super class) is not returning the right value in some cases
		String packageName = getPackageText();

		if (status.getCode() != IStatus.ERROR) {
			if (packageName != null) {
				Pattern pattern = Pattern.compile("[A-Za-z0-9_\\.]+"); //$NON-NLS-1$
				Matcher matcher = pattern.matcher(packageName);

				if (packageName.indexOf('.') == -1) {
					status = new Status(IStatus.ERROR, CodeUtilsActivator.PLUGIN_ID,
							CodeUtilsNLS.ERR_NewBuildingBlocksWizardPage_PackageMustHaveAtLeastTwoIdentifiers);
				} else if (!matcher.matches()) {
					String errMsg = NLS.bind(CodeUtilsNLS.ERR_NewBuildingBlocksWizardPage_InvalidPackageName,
							packageName);
					status = new Status(IStatus.ERROR, CodeUtilsActivator.PLUGIN_ID, errMsg);
				} else if (packageAndClassExist()) {
					status = new Status(IStatus.ERROR, CodeUtilsActivator.PLUGIN_ID,
							CodeUtilsNLS.ERR_NewBuildingBlocksWizardPage_PackageAndClassAlreadyExist);
				} else if (isTooLongOnFileSystem()) {
					status = new Status(IStatus.ERROR, CodeUtilsActivator.PLUGIN_ID,
							CodeUtilsNLS.ERR_NewBuildingBlocksWizardPage_FileNameTooLong);
				}
			}

		}

		return status;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jdt.ui.wizards.NewTypeWizardPage#containerChanged()
	 */
	@Override
	protected IStatus containerChanged() {
		IStatus superStatus = super.containerChanged();
		IStatus status = new Status(superStatus.getSeverity(), CodeUtilsActivator.PLUGIN_ID, superStatus.getMessage());

		boolean hasNature = false;

		if (status.getCode() != IStatus.ERROR) {
			try {
				if ((getPackageFragmentRoot() != null) && (getPackageFragmentRoot().getJavaProject() != null)) {
					hasNature = getPackageFragmentRoot().getJavaProject().getProject()
							.hasNature(IAndroidConstants.ANDROID_NATURE);
				}
			} catch (CoreException ce) {
				StudioLogger.error(NewBuildingBlocksWizardPage.class, "Error getting the project nature.", ce); //$NON-NLS-1$
				hasNature = false;
			}

			if ((getPackageFragmentRoot() != null) && !hasNature) {
				status = new Status(IStatus.ERROR, CodeUtilsActivator.PLUGIN_ID,
						CodeUtilsNLS.ERR_NewBuildingBlocksWizardPage_SelectAnAndroidProject);
			} else if ((getPackageFragmentRoot() == null)
					|| (getPackageFragmentRoot().getResource().getType() == IResource.PROJECT)
					|| ((getPackageFragmentRoot().getElementType() & IPackageFragmentRoot.K_SOURCE) != IPackageFragmentRoot.K_SOURCE)) {
				status = new Status(IStatus.ERROR, CodeUtilsActivator.PLUGIN_ID,
						CodeUtilsNLS.ERR_NewBuildingBlocksWizardPage_SelectAValidSourceFolder);
			} else if (getPackageFragmentRoot().getElementName().equals(IAndroidConstants.GEN_SRC_FOLDER)
					&& (getPackageFragmentRoot().getParent() instanceof IJavaProject)) {
				status = new Status(IStatus.ERROR, CodeUtilsActivator.PLUGIN_ID,
						CodeUtilsNLS.ERR_NewBuildingBlocksWizardPage_CannotUseTheGenFolderAsSourceFolder);
			} else if (isTooLongOnFileSystem()) {
				status = new Status(IStatus.ERROR, CodeUtilsActivator.PLUGIN_ID,
						CodeUtilsNLS.ERR_NewBuildingBlocksWizardPage_FileNameTooLong);
			}
		}

		return status;
	}

	/**
	 * Checks for cross package/class collision among source folders
	 * 
	 * @return true if there is any collision or false otherwise
	 */
	private boolean packageAndClassExist() {
		boolean exists = false;

		try {
			if ((getJavaProject() != null) && getJavaProject().isOpen()) {
				IPackageFragmentRoot[] roots = getJavaProject().getPackageFragmentRoots();

				if (roots != null) {
					for (IPackageFragmentRoot root : roots) {
						if ((root.getKind() & IPackageFragmentRoot.K_SOURCE) == IPackageFragmentRoot.K_SOURCE) {
							IPackageFragment pack = root.getPackageFragment(getPackageText());

							if ((pack != null) && pack.exists()) {
								IJavaElement classes[] = pack.getChildren();

								if (classes != null) {
									for (IJavaElement clazz : classes) {
										if (clazz.getElementName().equals(getTypeName() + JAVA_EXTENSION)) {
											exists = true;
											break;
										}
									}
								}
							}
						}

						if (exists) {
							break;
						}
					}
				}
			}
		} catch (JavaModelException e) {
			// Do nothing
			StudioLogger.error(NewBuildingBlocksWizardPage.class, e.getLocalizedMessage(), e);
		}

		return exists;
	}

	/**
	 * Checks if the current building block that is being created can be written
	 * to the file system without throw a "file name too long" error
	 * 
	 * @return true if the building block can be written or false otherwise
	 */
	private boolean isTooLongOnFileSystem() {
		boolean isTooLong = false;

		if (getPackageFragment() != null) {
			String javaFileName = getPackageFragment().getCompilationUnit(getTypeName() + JAVA_EXTENSION).getResource()
					.getLocation().toFile().getPath();

			isTooLong = javaFileName.length() > MAX_PATH_SIZE;
		}

		return isTooLong;
	}

	/**
	 * Checks if the wizard can be opened. If the wizard cannot be opened, an
	 * error message is displayed.
	 * 
	 * @return true if the wizard can be opened or false otherwise.
	 */
	private boolean canOpen() {
		boolean canOpen = true;

		if (getBuildBlock().getProject() != null) {
			IStatus status = null;

			try {
				AndroidManifestFile manifestFile = AndroidProjectManifestFile.getFromProject(getBuildBlock()
						.getProject());

				if (manifestFile.hasErrors()) {
					status = new MultiStatus(CodeUtilsActivator.PLUGIN_ID, IStatus.ERROR, manifestFile.getErrors(),
							CodeUtilsNLS.ERR_NewBuildingBlocksWizardPage_OneOrMoreErrorsWhenParsingManifest, null);
				}
			} catch (AndroidException e) {
				status = new Status(IStatus.ERROR, CodeUtilsActivator.PLUGIN_ID, e.getLocalizedMessage());
			} catch (CoreException e) {
				status = e.getStatus();
			}

			if (status != null) {
				canOpen = false;

				EclipseUtils.showErrorDialog(CodeUtilsNLS.UI_GenericErrorDialogTitle,
						CodeUtilsNLS.ERR_NewBuildingBlocksWizardPage_CannotProceedWithTheBuildingBlockCreation, status);
			}
		}

		return canOpen;
	}
}

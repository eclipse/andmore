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
package org.eclipse.andmore.android.codeutils.wizards;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.andmore.android.codeutils.CodeUtilsActivator;
import org.eclipse.andmore.android.codeutils.db.utils.DatabaseUtils;
import org.eclipse.andmore.android.codeutils.i18n.CodeUtilsNLS;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.andmore.android.manifest.AndroidProjectManifestFile;
import org.eclipse.andmore.android.model.manifest.AndroidManifestFile;
import org.eclipse.andmore.android.model.manifest.dom.IAndroidManifestProperties;
import org.eclipse.andmore.android.model.manifest.dom.ManifestNode;
import org.eclipse.andmore.android.wizards.elements.FileChooser;
import org.eclipse.andmore.android.wizards.elements.ProjectChooser;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.wizards.NewTypeWizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.PlatformUI;

/**
 * 
 * Wizard page to create classes which aids the database management.
 *
 */
public class DatabaseManagementClassesCreationMainPage extends NewTypeWizardPage {
	private static final String CONTEXT_HELP_ID = CodeUtilsActivator.PLUGIN_ID + ".create_db_classes";

	/**
	 * <p>
	 * This listener is called when the Project�s name is modified. Here is
	 * checked whether the project�s name represents any available project in
	 * the current workspace. In case it does, the project of the wizard is
	 * updated and the status is also checked. Otherwise, the status is checked
	 * and an error message regarding this validation is displayed.
	 * </p>
	 * <p>
	 * This listener also enables/disables components depending on whether the
	 * chosen project is valid. Moreover, it populates the fields accordingly if
	 * the selected project is valid.
	 * </p>
	 */
	private final class ProjectChooserModifyListener implements ModifyListener {
		@Override
		public void modifyText(ModifyEvent e) {
			boolean isValidationOK = getProjectStatus(projectChooser.getText()) == null;
			// in case there is no status, the project�s name is OK therefore
			// update the project
			try {
				if (isValidationOK) {
					// update fileChooser container
					fileChooser.setContainer(projectChooser.getProject());
					// update project
					updateProject(projectChooser.getProject());
				} else {
					fileChooser.setContainer(ResourcesPlugin.getWorkspace().getRoot());
					// null the project
					updateProject(null);
				}
			} catch (JavaModelException jme) {
				StudioLogger.error(this.getClass(), CodeUtilsNLS.Db_GenerateManagementClassesError, jme);
				IStatus status = new Status(IStatus.ERROR, PLUGIN_ID, jme.getLocalizedMessage());
				EclipseUtils.showErrorDialog(CodeUtilsNLS.Db_GenerateManagementClassesError,
						CodeUtilsNLS.Db_GenerateManagementClassesError, status);
			}

			// enable/disable this wizard�s group
			// setCompositeChildremEnabled(databaseFileGroup, isValidationOK);
			// ckbGenerateSQLOpenHelper.setEnabled(isValidationOK);
			// setCompositeChildremEnabled(sqlOpenHelperGroup, isValidationOK);
			// ckbCreateContentProviders.setEnabled(isValidationOK);
			// setCompositeChildremEnabled(contentProviderGroup,
			// isValidationOK);

			// update status and page completion
			doStatusAndPageCompletionUpdate();
		}
	}

	/**
	 * <p>
	 * This class represents a listener which is called every time the text
	 * field in the file chooser is modified. This listener intends to validate
	 * whether the entered path is a valid path or not. To do so, the
	 * {@link DatabaseManagementClassesCreationMainPage#doStatusAndPageCompletionUpdate()}
	 * is called.
	 * </p>
	 * <p>
	 * This listener also enable/disables components depending on whether the
	 * database file is validated correctly.
	 * </p>
	 */
	private final class DBFileChooserModifyListener implements ModifyListener {

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.swt.events.ModifyListener#modifyText(org.eclipse.swt.
		 * events.ModifyEvent)
		 */
		@Override
		public void modifyText(ModifyEvent e) {
			// validation flag
			boolean isValidationOK = ((getFileStatus(fileChooser.getText()) == null) || (!getFileStatus(
					fileChooser.getText()).equals(IStatus.ERROR)));

			if (isValidationOK) {
				// set the database file
				selectedDatabasePath = getDatabaseFilePath();
			}

			// enable/disable this wizard�s group
			setCompositeChildremEnabled(sqlOpenHelperGroup, isValidationOK);
			ckbCreateContentProviders.setEnabled(isValidationOK);
			setCompositeChildremEnabled(contentProviderGroup, isValidationOK);

			// update status and page completion
			doStatusAndPageCompletionUpdate();
		}
	}

	/**
	 * This listener is dispatched, from the inner part of Source/Package
	 * component - {@link SourcePackageChooserPartWizard}. when a message inside
	 * it is thrown. This listener calls the
	 * {@link DatabaseManagementClassesCreationMainPage#doStatusAndPageCompletionUpdate()}
	 * which gets the error message from the Source?Package element and displays
	 * it.
	 */
	private final class ContentProviderPackageElementMessageActionListener implements ActionListener {
		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent
		 * )
		 */
		@Override
		public void actionPerformed(ActionEvent e) {
			// update status and page completion
			doStatusAndPageCompletionUpdate();
		}
	}

	/**
	 * Listener which handles when a check box button is pressed. Basically, it
	 * enables or disables all components stored in the constructor parameter
	 * {@link Group}. It also starts the event for validating this screen.
	 */
	private final class CkbPanelEnablementListener implements Listener {
		private final Group panelEnablementGroup;

		/*
		 * Constructor
		 * 
		 * @param panelEnablementGroup SQL Helper group
		 */
		private CkbPanelEnablementListener(Group panelEnablementGroup) {
			this.panelEnablementGroup = panelEnablementGroup;
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
			// get the check box which dispatched the event
			Button checkBox = event.widget != null ? (Button) event.widget : null;
			// proceed in case there is a check box
			if (checkBox != null) {
				// flag indicating whether to enable/disable the controls
				boolean enabled = checkBox.getSelection();
				// enable/disable the children of panelEnablementGroup field
				setCompositeChildremEnabled(panelEnablementGroup, enabled);
			}
			// update status and page completion
			doStatusAndPageCompletionUpdate();
		}
	}

	/* Constants */

	// max size of the database file in bytes
	private static final long MAX_FILE_SIZE = 1048576;

	private static final String MAX_FILE_CHAR_SIZE = "1MB"; //$NON-NLS-1$

	private static final String ASSESTS_FOLDER = "assets";

	private final String PLUGIN_ID = "org.eclipse.andmore.android.db"; //$NON-NLS-1$

	/* Fields */

	private IProject selectedProject;

	private Button ckbCreateContentProviders;

	private Button ckbOverrideContentProviders;

	private SourcePackageChooserPartWizard contentProviderPackageComposite;

	private Group sqlOpenHelperGroup;

	private Group contentProviderGroup;

	private Group databaseFileGroup;

	private FileChooser fileChooser;

	private Group projectGroup;

	private ProjectChooser projectChooser;

	private ScrolledComposite scroll;

	private Composite innerSchollComposite;

	IPath selectedDatabasePath;

	/**
	 * Get the SQL Open Helper class name.
	 * 
	 * @return SQl Open Helper class name
	 */
	public String getSQLOpenHelperClassName() {
		return getTypeName();
	}

	/**
	 * Get the destination package of the Content Providers
	 * 
	 * @return Destination package of the Content Providers
	 */
	public String getContentProviderPackage() {
		return contentProviderPackageComposite != null ? contentProviderPackageComposite.getPackageText() : ""; //$NON-NLS-1$
	}

	/**
	 * Get the chosen project.
	 * 
	 * @return Selected {@link IProject}.
	 */
	public IProject getSelectedProject() {
		return selectedProject;
	}

	/**
	 * @return The {@link IPath} representing the selected Database file.
	 */
	public IPath getDatabaseFilePath() {
		// file to be returned
		IPath dbFile = null;
		// text representing the file
		String fileText = fileChooser.getText();
		// get the database file in case there is a selected project
		if (selectedProject != null) {
			dbFile = (fileText != null) && (fileText.length() > 0) ? new Path(fileText) : null;
		}
		return dbFile;
	}

	/**
	 * Get the Database Open Helper Package.
	 * 
	 * @return Database Open Helper Package.
	 */
	public String getDatabaseOpenHelperPackage() {
		return getPackageText();
	}

	/**
	 * Returns <code>true</code> in case is is necessary to create the Content
	 * Provider classes, <code>false</code> otherwise.
	 * 
	 * @return <code>true</code> in order to create Content Provider classes,
	 *         <code>false</code> otherwise.
	 */
	public boolean isCreateContentProviders() {
		return ckbCreateContentProviders != null ? ckbCreateContentProviders.getSelection() : false;
	}

	/**
	 * Returns <code>true</code> in case it is necessary to create the Open SQL
	 * Helper classes, <code>false</code> otherwise.
	 * 
	 * @return <code>true</code> in case it is necessary to create SQL Open
	 *         Helper class, <code>false</code> otherwise.
	 */
	public boolean isCreateSQLOpenHelperClass() {
		return true;
	}

	/**
	 * Returns <code>true</code> in case one wishes to override existing Content
	 * Provider classes, <code>false</code> otherwise.
	 * 
	 * @return <code>true</code> in case one wishes to override existing Content
	 *         Provider classes, <code>false</code> otherwise.
	 */
	public boolean isOverrideExistingContentProviderClasses() {
		return ckbOverrideContentProviders != null ? ckbOverrideContentProviders.getSelection() : false;
	}

	/**
	 * Create a new wizard page based on selection
	 * 
	 * @param pageName
	 *            the page name
	 * @param isDeployWaizard
	 *            show deployment fields
	 */
	public DatabaseManagementClassesCreationMainPage(String pageName, IProject project, IResource database) {
		// call super
		super(true, pageName);
		// set description and title
		setDescription(CodeUtilsNLS.UI_PersistenceWizardPageDescriptionDeploy);
		setTitle(CodeUtilsNLS.UI_PersistenceWizardPageTitleDeploy);

		// update project
		try {
			updateProject(project);
		} catch (JavaModelException e) {
			StudioLogger.error(this.getClass(), CodeUtilsNLS.Db_GenerateManagementClassesError, e);
			IStatus status = new Status(IStatus.ERROR, PLUGIN_ID, e.getLocalizedMessage());
			EclipseUtils.showErrorDialog(CodeUtilsNLS.Db_GenerateManagementClassesError,
					CodeUtilsNLS.Db_GenerateManagementClassesError, status);
		}
		// update database
		if (database != null) {
			selectedDatabasePath = database.getLocation();
		}
		// update status and page completion
		doStatusAndPageCompletionUpdate();
	}

	/*
	 * @see NewContainerWizardPage#handleFieldChanged
	 */
	@Override
	protected void handleFieldChanged(String fieldName) {
		super.handleFieldChanged(fieldName);
		// update status and page completion
		doStatusAndPageCompletionUpdate();
	}

	/**
	 * Creates page content.
	 * 
	 * @param parent
	 *            wizard composite.
	 */
	@Override
	public void createControl(Composite parent) {
		try {
			// main control
			Composite mainComposite = new Composite(parent, SWT.FILL);
			PlatformUI.getWorkbench().getHelpSystem().setHelp(mainComposite, CONTEXT_HELP_ID);
			mainComposite.setLayout(new FillLayout(SWT.FILL));

			scroll = new ScrolledComposite(mainComposite, SWT.H_SCROLL | SWT.V_SCROLL);
			innerSchollComposite = new Composite(scroll, SWT.NONE);

			// set a grid layout with 1 column
			GridLayout layout = new GridLayout();
			layout.numColumns = 1;
			innerSchollComposite.setLayout(layout);
			innerSchollComposite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));

			// create Project selection stuff
			createProjectSelectionSession(innerSchollComposite);

			// create Database File stuff
			createDatabaseFileSession(innerSchollComposite);

			// retrieve package from manifest file
			String manifestPackage = null;
			try {
				AndroidManifestFile androidManifestFile = AndroidProjectManifestFile
						.getFromProject(getSelectedProject());
				ManifestNode manifestNode = androidManifestFile.getManifestNode();
				manifestPackage = manifestNode.getNodeProperties().get(IAndroidManifestProperties.PROP_PACKAGE);
			} catch (Exception e) {
				// do nothing; default package value will be passed as null
			}

			// add SQL helper stuff
			createOpenHelperSession(innerSchollComposite, manifestPackage);

			// add content provider stuff
			createContentProviderSession(innerSchollComposite, manifestPackage);

			// add listeners
			addListeners();

			// set up scroll
			scroll.setContent(innerSchollComposite);

			scroll.setExpandHorizontal(true);
			scroll.setExpandVertical(true);

			scroll.addControlListener(new ControlAdapter() {
				@Override
				public void controlResized(ControlEvent e) {
					scroll.setMinSize(innerSchollComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
				}
			});

			// set control
			setControl(mainComposite);

			// update project text field
			if (selectedProject != null) {
				projectChooser.setText(selectedProject.getName());
			}

			// update database text field
			if (selectedDatabasePath != null) {
				fileChooser.setText(selectedDatabasePath.toOSString());
			}

			// set focus on the name text field
			setFocus();
		} catch (Exception e) {
			StudioLogger.error(this.getClass(), CodeUtilsNLS.Db_GenerateManagementClassesError, e);
			IStatus status = new Status(IStatus.ERROR, PLUGIN_ID, e.getLocalizedMessage());
			EclipseUtils.showErrorDialog(CodeUtilsNLS.Db_GenerateManagementClassesError,
					CodeUtilsNLS.Db_GenerateManagementClassesError, status);
		}
	}

	/**
	 * Add Project Selection Section.
	 * 
	 * @param mainComposite
	 *            Main Composite.
	 */
	private void createProjectSelectionSession(Composite mainComposite) {
		// create layout for Content Provider class creation
		projectGroup = new Group(mainComposite, SWT.NONE);
		projectGroup.setText(CodeUtilsNLS.UI_PersistenceWizardPageSelectProjectTitle);

		projectGroup.setLayout(new GridLayout(5, false));
		projectGroup.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));

		// add project chooser
		projectChooser = new ProjectChooser(projectGroup, SWT.FILL);
	}

	/**
	 * Create the Database File Session
	 * 
	 * @param mainComposite
	 *            Main composite
	 */
	private void createDatabaseFileSession(Composite mainComposite) {
		// create group for Database file
		databaseFileGroup = new Group(mainComposite, SWT.NONE);
		databaseFileGroup.setText(CodeUtilsNLS.UI_PersistenceWizardPageDatabaseFileGroupTitle);
		databaseFileGroup.setLayout(new GridLayout(3, false));
		databaseFileGroup.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));

		// add file chooser
		fileChooser = new FileChooser(selectedProject, databaseFileGroup, SWT.NONE, null);
		fileChooser.setFilterExtensions(new String[] {

		});
		fileChooser.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
	}

	/**
	 * Create the "Generate SQLOpenHelper" session
	 * 
	 * @param mainComposite
	 *            The Composite used as the main screen of the page.
	 * @param defaultPackageName
	 *            The name of the default package to use on the package field.
	 */
	private void createOpenHelperSession(Composite mainComposite, String defaultPackageName) {
		// check box for generating SQL Open Helper
		sqlOpenHelperGroup = new Group(mainComposite, SWT.NONE);
		sqlOpenHelperGroup.setText(CodeUtilsNLS.UI_PersistenceWizardPageSQLOpenHelperGroupTitle);
		int numColumns = 5;
		sqlOpenHelperGroup.setLayout(new GridLayout(numColumns, false));
		sqlOpenHelperGroup.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));

		/* Class selection for SQLOpenHelper */

		createTypeNameControls(sqlOpenHelperGroup, numColumns);

		/* Package selection for SQLOpenHelper */
		setPackageFragmentRoot(getPackageFragmentRoot(), true);
		createContainerControls(sqlOpenHelperGroup, numColumns);
		boolean defaultPackageUsed = false;
		if (defaultPackageName != null) {
			// try to use the manifest package, but if this fails, use the
			// default getPackageFragment() logic
			try {
				setPackageFragment(getPackageFragmentRoot().getPackageFragment(defaultPackageName), true);
				defaultPackageUsed = true;
			} catch (Exception e) {
				// do nothing
			}
		}
		if (!defaultPackageUsed) {
			setPackageFragment(getPackageFragment(), true);
		}
		// create the controls for the package
		createPackageControls(sqlOpenHelperGroup, numColumns);
	}

	/**
	 * Create the "Content Provider" session
	 * 
	 * @param mainComposite
	 *            The Composite used as the main screen of the page.
	 * @param defaultPackageName
	 *            The name of the default package to use on the package field.
	 */
	private void createContentProviderSession(Composite mainComposite, String defaultPackageName) {
		// create the check for generating content providers
		ckbCreateContentProviders = new Button(mainComposite, SWT.CHECK);
		ckbCreateContentProviders.setText(CodeUtilsNLS.UI_PersistenceWizardGenerateContentProvidersForEachTable);
		ckbCreateContentProviders.setSelection(true);

		contentProviderGroup = new Group(mainComposite, SWT.NONE);
		contentProviderGroup.setText(CodeUtilsNLS.UI_PersistenceWizardPageContentProviderGroupTitle);
		int numColumns = 5;
		contentProviderGroup.setLayout(new GridLayout(numColumns, false));
		contentProviderGroup.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));

		// add the Source/Package part
		contentProviderPackageComposite = new SourcePackageChooserPartWizard(getName(), selectedProject,
				defaultPackageName, contentProviderGroup, numColumns);

		// create the check for overriding content providers
		ckbOverrideContentProviders = new Button(contentProviderGroup, SWT.CHECK);
		ckbOverrideContentProviders.setText(CodeUtilsNLS.UI_PersistenceWizardOverrideContentProvidersIfAlreadyExists);
		ckbOverrideContentProviders.setSelection(true);
		ckbOverrideContentProviders
				.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, numColumns, 1));
	}

	/**
	 * Add listeners to the components
	 */
	private void addListeners() {
		// add message listener
		contentProviderPackageComposite
				.addMessageNotificationActionListener(new ContentProviderPackageElementMessageActionListener());

		// add listener for content provider panel enablement
		ckbCreateContentProviders.addListener(SWT.Selection, new CkbPanelEnablementListener(contentProviderGroup));

		// add listener for DB file chooser
		fileChooser.addModifyListener(new DBFileChooserModifyListener());

		// add listener for the Project chooser
		projectChooser.addModifyListener(new ProjectChooserModifyListener());
	}

	/**
	 * Update the status, related to this page�s components, which interferes
	 * with this wizard. Moreover, validate the page and launch the
	 * {@link NewTypeWizardPage}{@link #setPageComplete(boolean)} method.
	 */
	private void doStatusAndPageCompletionUpdate() {
		// variable which indicates whether there is any status with an error
		// level
		boolean isAnyStatusWithError = false;
		// variable which indicates whether the wizard validation is OK
		boolean isPageValidated = false;
		// list to hold all status to validate
		List<IStatus> statusList = new ArrayList<IStatus>();
		// array list to be validated
		IStatus[] statusArray = new IStatus[0];

		// get the status from the project chooser
		if (projectChooser != null) {
			// get the project Name
			String projectName = projectChooser.getText();

			// validate project�s name
			IStatus status = getProjectStatus(projectName);
			// add status to the list and check for error status, in case there
			// is any
			if (status != null) {
				// add status to the list
				statusList.add(status);
				// verify status error
				isAnyStatusWithError = status.getSeverity() == IStatus.ERROR;
			}
		}

		// get the status from the database file chooser path, in case there is
		// no error status
		if (!isAnyStatusWithError && (fileChooser != null)) {
			// get path
			String path = fileChooser.getText();

			// get the status
			IStatus status = getFileStatus(path);
			// in case it is not null, add it to the list and check error status
			if (status != null) {
				statusList.add(status);
				// check error status
				isAnyStatusWithError = status.getSeverity() == IStatus.ERROR;
			}
		}

		// get the status from Database helper name and package selection, in
		// case it is required and there is no error status
		if (!isAnyStatusWithError) {
			for (IStatus status : getThisPageStatusList()) {
				// add the status to its list
				statusList.add(status);
				// verify error status
				isAnyStatusWithError = status.getSeverity() == IStatus.ERROR;
				// in case there is error status, quit the loop
				if (isAnyStatusWithError) {
					break;
				}
			}
			// in case there are still no errors, check for the package name
			// status
			if (!isAnyStatusWithError) {
				IStatus status = getDatabasePackageStatus();
				if (status != null) {
					// add status to its list
					statusList.add(status);
					// verify whether there is no error status
					isAnyStatusWithError = status.getSeverity() == IStatus.ERROR;
				}
			}
		}

		// get the status of the content provider part, in case it is required
		// and there is no error status
		if (!isAnyStatusWithError && (ckbCreateContentProviders != null) && ckbCreateContentProviders.getSelection()) {
			if (this.contentProviderPackageComposite != null) {
				IStatus status = this.contentProviderPackageComposite.getMostSevereStatus();
				if (status != null) {
					// add status to its list
					statusList.add(status);
					// verify whether there is no error status
					isAnyStatusWithError = status.getSeverity() == IStatus.ERROR;
				}
			}
			// in case there are still no errors, check for the package name
			// status
			if (!isAnyStatusWithError) {
				IStatus status = getContentProvidersPackageStatus();
				if (status != null) {
					// add status to its list
					statusList.add(status);
					// verify whether there is no error status
					isAnyStatusWithError = status.getSeverity() == IStatus.ERROR;
				}
			}
		}

		// get the status for minimum requirements of classes generation, if
		// there is no error status
		if (!isAnyStatusWithError) {
			IStatus status = getMinimumClassesGenerationRequirementsStatus();
			if (status != null) {
				// add status to its list
				statusList.add(status);
				// verify whether there is no error status
				isAnyStatusWithError = status.getSeverity() == IStatus.ERROR;
			}
		}

		// convert to an array
		if (statusList.size() > 0) {
			statusArray = new IStatus[statusList.size()];
			for (int index = 0; index < statusList.size(); index++) {
				statusArray[index] = statusList.get(index);
			}
		} else {
			// in case there is no error, status, add an OK status with the
			// default message
			statusArray = new IStatus[] { new Status(IStatus.OK, PLUGIN_ID,
					CodeUtilsNLS.UI_PersistenceWizardPageDescriptionDeploy) };
		}

		// the most severe status will be displayed and the OK button
		// enabled/disabled.
		updateStatus(statusArray);

		/*
		 * The page is validated if: 1 - There are no status errors 2 - The SQL
		 * Helper creation and Content Provider creation check boxes exists
		 */
		isPageValidated = (!isAnyStatusWithError) && ((ckbCreateContentProviders != null));

		// set page completion
		setPageComplete(isPageValidated);
	}

	/**
	 * Get the SQL Open Helper package name status. Basically, it points an
	 * error in case there is no package.
	 * 
	 * @return SQL Open Helper package name status
	 */
	private IStatus getDatabasePackageStatus() {
		IStatus status = null;

		// get the package name
		String packageName = getPackageText();
		// it must have a value
		if ((packageName == null) || (packageName.length() == 0)) {
			status = new Status(IStatus.ERROR, PLUGIN_ID,
					CodeUtilsNLS.DatabaseManagementClassesCreationMainPage_UI_OpenHelperPackageNameMustNotBeEmpty);
		} else {
			status = new Status(IStatus.OK, PLUGIN_ID, ""); //$NON-NLS-1$
		}

		return status;
	}

	/**
	 * Get the Content Providers package name status. Basically, it points an
	 * error in case there is no package.
	 * 
	 * @return SQL Open Helper package name status
	 */
	private IStatus getContentProvidersPackageStatus() {
		IStatus status = null;

		// get the package name
		String packageName = contentProviderPackageComposite != null ? contentProviderPackageComposite.getPackageText()
				: ""; //$NON-NLS-1$
		// it must have a value
		if ((packageName == null) || (packageName.length() == 0)) {
			status = new Status(IStatus.ERROR, PLUGIN_ID,
					CodeUtilsNLS.DatabaseManagementClassesCreationMainPage_UI_ContentProvidersPackageNameMustNotBeEmpty);
		} else {
			status = new Status(IStatus.OK, PLUGIN_ID, ""); //$NON-NLS-1$
		}

		return status;
	}

	/**
	 * Return the status regarding the mininum requirements for the generation
	 * of classes.
	 * 
	 * These requirements are simple: there must be either the creation of SQL
	 * Open Helper classes.
	 * 
	 * @return Status regarding minimum requirements for the classes generation.
	 */
	private IStatus getMinimumClassesGenerationRequirementsStatus() {
		IStatus status = null;
		status = new Status(IStatus.OK, PLUGIN_ID, ""); //$NON-NLS-1$        
		return status;
	}

	/**
	 * Get this page Status list considering only the {@link NewTypeWizardPage}
	 * inheritance. It means that this method return status only related to
	 * elements from {@link NewTypeWizardPage}. Other things associated, for
	 * instance, with {@link FileChooser} or {@link ProjectChooser} are treated
	 * some place else.
	 * 
	 * @return {@link NewTypeWizardPage} related status list
	 */
	private IStatus[] getThisPageStatusList() {
		return new IStatus[] { fContainerStatus, isEnclosingTypeSelected() ? fEnclosingTypeStatus : fPackageStatus,
				fTypeNameStatus, };
	}

	/**
	 * <p>
	 * Check whether a Project Name refers to a Project. In case it does,
	 * <code>null</code> is returned, otherwise, an {@link IStatus} is returned
	 * indicating the error.
	 * </p>
	 * <p>
	 * Note the the Project must belong to the current workspace.
	 * </p>
	 * 
	 * @param projectName
	 *            Project Name to be validated.
	 * @return
	 */
	private IStatus getProjectStatus(String projectName) {
		IStatus status = null;

		if ((projectName == null) || (projectName.length() == 0)) {
			// there must be a selected project
			status = new Status(IStatus.ERROR, PLUGIN_ID,
					CodeUtilsNLS.UI_PersistenceWizardPageThereMustBeASelectedProject);
		} else {

			// get root workspace
			IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();

			IProject[] projects = workspaceRoot.getProjects();

			// iterate through the projects
			if ((projects != null) && (projects.length > 0)) {
				// flag indicating whether the project was found
				boolean isProjectFound = false;

				for (IProject project : projects) {
					if (project.getName().equals(projectName)) {
						// the validation is OK, set the flag and quit the loop
						isProjectFound = true;
						break;
					}
				}
				// in case the project was not found, set the flag
				if (!isProjectFound) {
					status = new Status(IStatus.ERROR, PLUGIN_ID,
							CodeUtilsNLS.UI_PersistenceWizardPageTheEnteredProjectIsInvalid);
				}
			} else {
				// there must be a selected project
				status = new Status(IStatus.ERROR, PLUGIN_ID,
						CodeUtilsNLS.UI_PersistenceWizardPageThereMustBeASelectedProject);
			}
		}

		return status;
	}

	/**
	 * Get a file validation status. In case the file is OK, <code>null</code>
	 * is returned otherwise an error status is returned.
	 * 
	 * @param filePath
	 *            The file path (including the file name) to be validated
	 * 
	 * @return <code>null</code> in case the validation is OK, an error status
	 *         otherwise.
	 */
	private IStatus getFileStatus(String filePath) {
		// get status
		IStatus status = null;
		// there must be a file path and it must not be empty
		if ((filePath == null) || filePath.equals("")) //$NON-NLS-1$
		{
			status = new Status(IStatus.ERROR, PLUGIN_ID,
					CodeUtilsNLS.UI_PersistenceWizardPageThereMustBeASelectedDatabaseFile);
		} else {
			// validation result
			boolean isFileOK = false;
			// get path object
			Path path = new Path(filePath);

			// testing if the entered path is a folder
			isFileOK = path.toFile().isFile();
			if (!isFileOK) {
				status = new Status(IStatus.ERROR, PLUGIN_ID,
						CodeUtilsNLS.UI_PersistenceWizardPageTheEnteredPathIsInvalid);
			} else {
				isFileOK = path.isValidPath(path.toString());

				if (!isFileOK) {
					status = new Status(IStatus.ERROR, PLUGIN_ID,
							CodeUtilsNLS.UI_PersistenceWizardPageTheEnteredPathIsInvalid);
				} else {
					// Test if file exists
					isFileOK = path.toFile().exists();
					if (!isFileOK) {

						status = new Status(IStatus.ERROR, PLUGIN_ID,
								CodeUtilsNLS.UI_PersistenceWizardPageFileDoesNotExist);
					} else {
						try {
							if (!DatabaseUtils.isValidSQLiteDatabase(path.toFile())) {
								status = new Status(IStatus.ERROR, PLUGIN_ID,
										CodeUtilsNLS.UI_PersistenceWizardPageFileNotValid);

							}
						} catch (IOException e) {
							status = new Status(IStatus.WARNING, PLUGIN_ID,
									CodeUtilsNLS.UI_PersistenceWizardPageFileNotEvaluated);
						}

						if (path.toFile().length() > MAX_FILE_SIZE) {
							status = new Status(IStatus.WARNING, PLUGIN_ID, NLS.bind(
									CodeUtilsNLS.UI_PersistenceWizardPageFileTooLarge, MAX_FILE_CHAR_SIZE));
						}
					}

				}
			}
		}

		// in case there is no status, the validation went OK, or if the status
		// is not an ERROR
		// verify whether the file belongs to the project assets
		if ((status == null) || (status.getSeverity() != IStatus.ERROR)) {
			// verify whether the file is within asset�s folder
			if (!isFileWithinProjectAssets(new Path(filePath))) {
				// set the status
				status = new Status(IStatus.WARNING, PLUGIN_ID,
						CodeUtilsNLS.UI_PersistenceWizardPageTheDatabaseFileWillBeCopiedToProjectsAssetsFolder);
			}
		}

		return status;
	}

	/**
	 * Verifies whether a given file path belongs to the Project�s assets
	 * folder. <code>true</code> is returned in case it does, <code>false</code>
	 * otherwise.
	 * 
	 * @param path
	 *            File path which will be verified whether it is in the
	 *            Project�s assets directory.
	 * 
	 * @return <code>true</code> in case the file path belongs to the Project�s
	 *         assets, <code>false</code> otherwise.
	 */
	private boolean isFileWithinProjectAssets(IPath path) {
		boolean isPathWithin = false;

		// proceed in case there is a project and a path
		if ((selectedProject != null) && (path != null)) {
			// make the assets path = project path + assets + file name
			IPath databaseInAssetsPath = selectedProject.getLocation().addTrailingSeparator().append(ASSESTS_FOLDER)
					.addTrailingSeparator().append(path.toFile().getName());

			// the database path and the entered path must match
			isPathWithin = databaseInAssetsPath.toOSString().equals(path.toOSString());
		}

		return isPathWithin;
	}

	/**
	 * Enable/disable children of the entered {@link Composite}.
	 * 
	 * @param composite
	 *            Composite to have its children enabled/disabled
	 * @param enabled
	 *            <code>true</code> for enabling the elements,
	 *            <code>false</code> for disabling the elements.
	 */
	private void setCompositeChildremEnabled(Composite composite, boolean enabled) {
		Control[] controls = composite.getChildren();
		if ((controls != null) && (controls.length > 0)) {
			for (Control control : controls) {
				control.setEnabled(enabled);
			}
		}
	}

	/**
	 * Update project information within this wizard. Besides the basic project
	 * field input update, the package fragment root is determined and correctly
	 * updated in this wizard. Therefore, use this method preferably to update
	 * the project so everything in the wizard is updated accordingly.
	 * 
	 * @param project
	 *            {@link IProject} information to update
	 * @throws JavaModelException
	 *             Exception thrown when there are problems retrieving the
	 *             project�s fragment root.
	 */
	private void updateProject(IProject project) throws JavaModelException {
		if (project != null) {
			// set selected project
			this.selectedProject = project;

			// update project text in case it is not already set correctly
			if ((projectChooser != null) && !projectChooser.getText().equals(project.getName())) {
				projectChooser.setText(project.getName());
			}

			// get the java project
			IJavaProject javaProject = JavaCore.create(project);
			IPackageFragmentRoot[] possibleRoots = null;
			// continue in case it does exist
			if (javaProject != null) {
				// get all possible roots
				possibleRoots = javaProject.getPackageFragmentRoots();
				// select the first one, in case it does exist
				if ((possibleRoots != null) && (possibleRoots.length > 0)) {
					// set the first one
					setPackageFragmentRoot(possibleRoots[0], true);
					if (contentProviderPackageComposite != null) {
						contentProviderPackageComposite.setPackageFragmentRoot(possibleRoots[0], true);
					}
				}
			}
		} else {
			// update null information
			selectedProject = null;
			setPackageFragmentRoot(null, true);
			if (contentProviderPackageComposite != null) {
				contentProviderPackageComposite.setPackageFragmentRoot(null, true);
			}
		}
	}
}

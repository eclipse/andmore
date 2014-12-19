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

package org.eclipse.andmore.android.codeutils.codegeneration;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.andmore.android.codeutils.CodeUtilsActivator;
import org.eclipse.andmore.android.codeutils.db.utils.DatabaseUtils;
import org.eclipse.andmore.android.codeutils.i18n.CodeUtilsNLS;
import org.eclipse.andmore.android.common.exception.AndroidException;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.common.utilities.AndroidUtils;
import org.eclipse.andmore.android.model.ActivityBasedOnTemplate;
import org.eclipse.andmore.wizards.buildingblocks.Method;
import org.eclipse.andmore.wizards.buildingblocks.NewLauncherWizardPage;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.datatools.connectivity.ConnectionProfileException;
import org.eclipse.datatools.modelbase.sql.schema.Database;
import org.eclipse.datatools.modelbase.sql.tables.Table;
import org.eclipse.jface.dialogs.IPageChangeProvider;
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeNode;
import org.eclipse.jface.viewers.TreeNodeContentProvider;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

/**
 * Wizard page to create an activity based on database samples.
 * */
public class CreateSampleDatabaseActivityPage extends NewLauncherWizardPage {

	private TreeViewer treeViewer;

	// Tree viewer input
	private TreeNode[] treeNodeArray;

	// Page help ID
	public static final String PAGE_HELP_ID = CodeUtilsActivator.PLUGIN_ID + ".selecttablepage";

	// ANDROID_METADATA Table name
	private static final String ANDROID_METADATA_TABLE_NAME = "ANDROID_METADATA";

	// Can flip to next page flag
	private boolean canFlip = false;

	/**
	 * Default constructor. </br></br> Creates a new instance using {@link
	 * org.eclipse.andmore.wizards.buildingblocks.NewLauncherWizardPage#
	 * NewLauncherWizardPage
	 * ("org.eclipse.andmore.android.model.BuildingBlockModel,java.lang.String)
	 * NewLauncherWizardPage(BuildingBlockModel, String)} and passing
	 * {@code null} and
	 * {@link CodeUtilsNLS#UI_CreateSampleDatabaseActivityPageName Page name} as
	 * arguments.
	 */
	public CreateSampleDatabaseActivityPage() {
		super(null, CodeUtilsNLS.UI_CreateSampleDatabaseActivityPageName);
	}

	/**
	 * Creates a new instance using {@link
	 * org.eclipse.andmore.wizards.buildingblocks.NewLauncherWizardPage#
	 * NewLauncherWizardPage
	 * ("org.eclipse.andmore.android.model.BuildingBlockModel,java.lang.String)
	 * NewLauncherWizardPage(BuildingBlockModel, String)} and passing
	 * {@code activity} and
	 * {@link CodeUtilsNLS#UI_CreateSampleDatabaseActivityPageName Page name} as
	 * arguments.
	 * 
	 * @param activity
	 *            an
	 *            {@code com.motorola.studio.android.model.ActivityBasedOnTemplate}
	 *            to be used as the building block model.
	 */
	public CreateSampleDatabaseActivityPage(ActivityBasedOnTemplate activity) {
		super(activity, CodeUtilsNLS.UI_CreateSampleDatabaseActivityPageName);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.wizards.buildingblocks.
	 * NewBuildingBlocksWizardPage
	 * #createIntermediateControls(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createExtendedControls(Composite parent) {
		Composite mainComposite = new Composite(parent, SWT.NONE);
		mainComposite.setLayout(new GridLayout());
		mainComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
		mainComposite.setLayout(new GridLayout());

		Composite composite = new Composite(mainComposite, SWT.NONE);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));

		// Databases and Tables Tree Viewer
		treeViewer = new TreeViewer(composite, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL);
		treeViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// Set content and label provider
		treeViewer.setLabelProvider(new SampleDatabaseActivityPageLabelProvider());
		treeViewer.setContentProvider(new TreeNodeContentProvider());

		// Create an array of type TreeNode[] to serve as the input for the tree
		if ((treeNodeArray == null) || (treeNodeArray.length < 1)) {
			treeNodeArray = generateTreeViewerInput();
		}

		treeViewer.setInput(treeNodeArray);
		treeViewer.addSelectionChangedListener(new DatabaseTreeListener());

		// Check if there were any databases found and update status if not.
		if (treeNodeArray.length < 1) {
			// Create a warning status
			IStatus status = new Status(IStatus.WARNING, CodeUtilsActivator.PLUGIN_ID,
					CodeUtilsNLS.UI_CreateSampleDatabaseActivityPage_No_Database_Found_Information);
			updateStatus(status);
		}

		// Expand all elements
		treeViewer.expandAll();

		// Add a listener to the wizard to listen for page changes
		if (getContainer() instanceof IPageChangeProvider) {
			((IPageChangeProvider) getContainer()).addPageChangedListener(new PageChangeListener());
		}

		setControl(mainComposite);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.motorola.studio.android.wizards.buildingblocks.NewLauncherWizardPage
	 * #getIntentFiltersActions()
	 */
	@Override
	protected String[] getIntentFiltersActions() {
		String[] intentFiltersActions = new String[0];
		try {
			intentFiltersActions = AndroidUtils.getActivityActions(getBuildBlock().getProject());
		} catch (AndroidException e) {
			setErrorMessage(e.getMessage());
		}
		return intentFiltersActions;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.wizards.buildingblocks.
	 * NewBuildingBlocksWizardPage#getDefaultMessage()
	 */
	@Override
	public String getDefaultMessage() {
		return CodeUtilsNLS.UI_CreateSampleDatabaseActivityPage_Default_Message;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.wizards.buildingblocks.
	 * NewBuildingBlocksWizardPage#getHelpId()
	 */
	@Override
	protected String getHelpId() {
		return PAGE_HELP_ID;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.wizards.buildingblocks.
	 * NewBuildingBlocksWizardPage#getWizardTitle()
	 */
	@Override
	public String getWizardTitle() {
		return CodeUtilsNLS.UI_ActivityWizard_Title;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.wizards.buildingblocks.
	 * NewBuildingBlocksWizardPage#getMethods()
	 */
	@Override
	protected Method[] getMethods() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 */
	@Override
	public boolean canFlipToNextPage() {

		return canFlip;
	}

	/*
	 * Method to retrieve the databases and tables from the target project and
	 * make a TreeNodeArray
	 */
	private TreeNode[] generateTreeViewerInput() {
		// Collection of TreeNodes
		HashSet<TreeNode> treeNodeColletion = new HashSet<TreeNode>();

		// The selected project for which the activity will be created
		IProject project = getBuildBlock().getProject();

		if (project != null) {
			// Get a collection of existing .db files inside the project
			Set<IFile> dbFilesSet = DatabaseUtils.getDbFilesFromProject(project);

			// Retrieve the database instances
			for (IFile dbFile : dbFilesSet) {
				try {
					// For each database retrieved, construct a TreeNode object
					// containing itself and it's tables
					Database database = DatabaseUtils.getDatabase(project.getName(), dbFile.getName());
					TreeNode treeNodeDatabase = new TreeNode(database);

					// Collection to store the table treeNodes from this
					// database. Will be used later to set the children nodes of
					// the database.
					HashSet<TreeNode> databaseChildren = new HashSet<TreeNode>();

					// Construct another TreeTable object for each table and set
					// the database tree node as the parent.
					// Tables don't have children
					for (Table table : DatabaseUtils.getTables(database)) {
						// Do not add ANDROID_METADATA table
						if (!table.getName().equalsIgnoreCase(ANDROID_METADATA_TABLE_NAME)) {
							TreeNode treeNodeTable = new TreeNode(table);
							treeNodeTable.setParent(treeNodeDatabase);

							// Add this node as a children of the database tree
							// node.
							databaseChildren.add(treeNodeTable);
						}

					}

					// Add the table nodes as the children of the database node
					treeNodeDatabase.setChildren(databaseChildren.toArray(new TreeNode[0]));

					// Add the database tree node to the resulting TreeNode
					// collection that will serve as input
					treeNodeColletion.add(treeNodeDatabase);
				} catch (ConnectionProfileException e) {
					// Log error
					StudioLogger.error(DatabaseUtils.class, "A error ocurred while retrieving the connection profile.",
							e);
				} catch (IOException e) {
					// Log error
					StudioLogger.error(DatabaseUtils.class, "An I/O error ocurred.", e);
				}
			}
		}

		// Return a TreeNode array
		return treeNodeColletion.toArray(new TreeNode[0]);

	}

	/**
	 * @return Returns true if page has header. Otherwise, returns false.
	 */
	@Override
	public boolean hasHeader() {
		return false;
	}

	/**
	 * Selection listener for the tree viewer.
	 */
	class DatabaseTreeListener implements ISelectionChangedListener {

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged
		 * (org.eclipse.jface.viewers.SelectionChangedEvent)
		 */
		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			ActivityBasedOnTemplate activity = ((ActivityBasedOnTemplate) getBuildBlock());

			if (event.getSelection() instanceof ITreeSelection) {
				ITreeSelection treeSelection = (ITreeSelection) event.getSelection();

				if (treeSelection.getFirstElement() instanceof TreeNode) {
					TreeNode selectedNode = (TreeNode) treeSelection.getFirstElement();
					// Check if it's a database or table that was selected and
					// set verification flags
					if (selectedNode.getValue() instanceof Table) {
						canFlip = true;
						// Set the collector table
						activity.setCollectorTable((Table) selectedNode.getValue());
						// Set the collector database
						TreeNode parentNode = selectedNode.getParent();
						activity.setCollectorDatabaseName(((Database) parentNode.getValue()).getName());
						activity.setDatabaseTableSelected(true);

					} else {
						canFlip = false;
						// Set the collector info to null
						activity.setCollectorTable(null);
						activity.setCollectorDatabaseName(null);
						activity.setDatabaseTableSelected(false);

					}

					getWizard().getContainer().updateButtons();
				}
			}

		}
	}

	/**
	 * Listener to verify when this page is visible
	 */
	private class PageChangeListener implements IPageChangedListener {
		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.jface.dialogs.IPageChangedListener#pageChanged(org.eclipse
		 * .jface.dialogs.PageChangedEvent)
		 */
		@Override
		public void pageChanged(PageChangedEvent event) {
			if ((event.getSelectedPage() == CreateSampleDatabaseActivityPage.this)) {
				ActivityBasedOnTemplate activity = (ActivityBasedOnTemplate) getBuildBlock();

				treeNodeArray = generateTreeViewerInput();
				treeViewer.setInput(treeNodeArray);

				// Check if there were any databases found and update status if
				// not.
				if (treeNodeArray.length < 1) {
					// Create a warning status
					IStatus status = new Status(IStatus.WARNING, CodeUtilsActivator.PLUGIN_ID,
							CodeUtilsNLS.UI_CreateSampleDatabaseActivityPage_No_Database_Found_Information);
					updateStatus(status);
				} else {
					updateStatus(new Status(IStatus.OK, CodeUtilsActivator.PLUGIN_ID, null));
				}

				if ((activity.getCollectorTable() == null) && (!treeViewer.getSelection().isEmpty())) {
					treeViewer.setSelection(TreeSelection.EMPTY);
				}

				if (treeViewer.getSelection().isEmpty()) {
					canFlip = false;
				}

				// update buttons
				getWizard().getContainer().updateButtons();

				// Expand all elements
				treeViewer.expandAll();
			}

		}
	}
}

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
package org.eclipse.andmore.android.wizards.elements;

import org.eclipse.andmore.android.common.CommonPlugin;
import org.eclipse.andmore.android.common.utilities.i18n.UtilitiesNLS;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.navigator.ResourceComparator;

/**
 * This widget displays a Project Chooser element.
 */
public class ProjectChooser extends Composite {

	private Label lblProject;

	private Text txtProject;

	private Button btnBrowseProject;

	IProject project;

	/**
	 * Get the selected Project.
	 * 
	 * @return The selected Project
	 */
	public IProject getProject() {
		// get root workspace
		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		// get projects
		IProject[] projects = workspaceRoot.getProjects();

		// iterate through the projects
		if ((projects != null) && (projects.length > 0)) {
			for (IProject innerProject : projects) {
				if (innerProject.getName().equals(txtProject.getText())) {
					// the project was found, set it
					this.project = innerProject;
					break;
				}
			}
		}
		// return the project
		return project;
	}

	/**
	 * Get the text filled in the Project�s text field.
	 * 
	 * @return The text filled in the Project�s text field
	 */
	public String getText() {
		return txtProject != null ? txtProject.getText() : ""; //$NON-NLS-1$
	}

	/**
	 * Set the text within this {@link ProjectChooser}.
	 * 
	 * @param text
	 *            The text to set
	 */
	public void setText(String text) {
		if (txtProject != null) {
			this.txtProject.setText(text);
		}
	}

	/**
	 * Add a modify listener to the Project�s text field. This listener is
	 * called every time the text is modified.
	 * 
	 * @param modifyListener
	 *            Listener to be added
	 * 
	 * @see Text#addModifyListener(ModifyListener)
	 */
	public void addModifyListener(ModifyListener modifyListener) {
		// add listener
		txtProject.addModifyListener(modifyListener);
	}

	/**
	 * Enables or disables the text field.
	 * 
	 * @param enabled
	 *            <code>true</code> in case the Text Field is to be enabled,
	 *            <code>false</code> otherwise.
	 */
	public void setTextFieldEnabled(boolean enabled) {
		// enables/disables the text field
		txtProject.setEnabled(enabled);
	}

	/**
	 * Constructor holding the parent composite and the stile.
	 * 
	 * @param parent
	 *            Parent Composite
	 * @param style
	 *            Style
	 */
	public ProjectChooser(Composite parent, int style) {
		// call super
		super(parent, style);
		// set up layout
		setupLayout();
		// add components
		addComponents();
	}

	/**
	 * Set up this widget layout
	 */
	private void setupLayout() {
		GridLayout layout = new GridLayout(5, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		this.setLayout(layout);
		this.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
	}

	/**
	 * Add components to this widget
	 */
	private void addComponents() {
		// add project label
		lblProject = new Label(this, SWT.NONE);
		lblProject.setText(UtilitiesNLS.UI_General_ProjectLabel);
		lblProject.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, false, false, 1, 1));

		// add text field
		txtProject = new Text(this, SWT.BORDER);
		txtProject.setText(""); //$NON-NLS-1$
		txtProject.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false, 3, 1));

		// add browner button
		btnBrowseProject = new Button(this, SWT.PUSH);
		btnBrowseProject.setText(UtilitiesNLS.UI_General_BrowseButtonLabel);
		btnBrowseProject.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, false, false, 1, 1));

		btnBrowseProject.addListener(SWT.Selection, new Listener() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.
			 * widgets.Event)
			 */

			@Override
			public void handleEvent(Event event) {
				// get the selected project
				project = openProjectChooser();
				// write the project in case there is one
				if (project != null) {
					txtProject.setText(project.getName());
				}
			}
		});
	}

	/**
	 * Opens dialog to choose a project
	 * 
	 * @return Selected project
	 */
	private IProject openProjectChooser() {
		IProject selectedProject = null;

		// get shell
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		// crate package dialog
		final ElementTreeSelectionDialog packageDialog = new ElementTreeSelectionDialog(shell,
				new WorkbenchLabelProvider(), new WorkbenchContentProvider());

		// set title and message
		packageDialog.setTitle(UtilitiesNLS.ProjectChooser_UI_Selection);
		packageDialog.setMessage(UtilitiesNLS.ProjectChooser_UI_ChooseAProject);

		// set workspace as root
		packageDialog.setInput(ResourcesPlugin.getWorkspace().getRoot());
		// set comparator
		packageDialog.setComparator(new ResourceComparator(ResourceComparator.NAME));

		// filter extensions
		packageDialog.addFilter(new ViewerFilter() {

			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * org.eclipse.jface.viewers.ViewerFilter#select(org.eclipse.jface
			 * .viewers.Viewer, java.lang.Object, java.lang.Object)
			 */
			@Override
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				// the element must be a project
				return element instanceof IProject;
			}
		});
		// user can select only one PROJECT
		packageDialog.setValidator(new ISelectionStatusValidator() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * org.eclipse.ui.dialogs.ISelectionStatusValidator#validate(java
			 * .lang.Object[])
			 */

			@Override
			public IStatus validate(Object[] selection) {
				// by default the status is an error
				IStatus valid = new Status(IStatus.ERROR, CommonPlugin.PLUGIN_ID, ""); //$NON-NLS-1$
				// one element must be selected
				if (selection.length == 1) {
					// this element must be a project
					if (selection[0] instanceof IProject) {
						// set status to valid because it is one element and a
						// project
						valid = new Status(IStatus.OK, CommonPlugin.PLUGIN_ID, ""); //$NON-NLS-1$
					}
				}
				// return validation
				return valid;
			}
		});
		// open dialog
		if (packageDialog.open() == IDialogConstants.OK_ID) {
			// get the first result
			IResource resource = (IResource) packageDialog.getFirstResult();
			// the resource must be a project
			if (resource instanceof IProject) {
				// get the selected project
				selectedProject = (IProject) resource;
			}
		}
		// return the selected project
		return selectedProject;
	}
}

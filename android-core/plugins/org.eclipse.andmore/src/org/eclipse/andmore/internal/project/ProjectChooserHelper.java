/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.eclipse.andmore.internal.project;

import org.eclipse.andmore.internal.project.BaseProjectHelper.IProjectFilter;
import org.eclipse.andmore.internal.sdk.ProjectState;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

/**
 * Helper class to deal with displaying a project choosing dialog that lists only the
 * projects with the Android nature.
 */
public class ProjectChooserHelper {

    private final Shell mParentShell;
    private final IProjectChooserFilter mFilter;

    /**
     * List of current android projects. Since the dialog is modal, we'll just get
     * the list once on-demand.
     */
    private IJavaProject[] mAndroidProjects;

    /**
     * Interface to filter out some project displayed by {@link ProjectChooserHelper}.
     *
     * @see IProjectFilter
     */
    public interface IProjectChooserFilter extends IProjectFilter {
        /**
         * Whether the Project Chooser can compute the project list once and cache the result.
         * </p>If false the project list is recomputed every time the dialog is opened.
         */
        boolean useCache();
    }

    /**
     * An implementation of {@link IProjectChooserFilter} that only displays non-library projects.
     */
    public final static class NonLibraryProjectOnlyFilter implements IProjectChooserFilter {
        @Override
        public boolean accept(IProject project) {
            ProjectState state = Sdk.getProjectState(project);
            if (state != null) {
                return state.isLibrary() == false;
            }

            return false;
        }

        @Override
        public boolean useCache() {
            return true;
        }
    }

    /**
     * An implementation of {@link IProjectChooserFilter} that only displays library projects.
     */
    public final static class LibraryProjectOnlyFilter implements IProjectChooserFilter {
        @Override
        public boolean accept(IProject project) {
            ProjectState state = Sdk.getProjectState(project);
            if (state != null ) {
                return state.isLibrary();
            }

            return false;
        }

        @Override
        public boolean useCache() {
            return true;
        }
    }

    /**
     * Creates a new project chooser.
     * @param parentShell the parent {@link Shell} for the dialog.
     * @param filter a filter to only accept certain projects. Can be null.
     */
    public ProjectChooserHelper(Shell parentShell, IProjectChooserFilter filter) {
        mParentShell = parentShell;
        mFilter = filter;
    }

    /**
     * Displays a project chooser dialog which lists all available projects with the Android nature.
     * <p/>
     * The list of project is built from Android flagged projects currently opened in the workspace.
     *
     * @param projectName If non null and not empty, represents the name of an Android project
     *                    that will be selected by default.
     * @param message Message for the dialog box. Can be null in which case a default message
     *                is displayed.
     * @return the project chosen by the user in the dialog, or null if the dialog was canceled.
     */
    public IJavaProject chooseJavaProject(String projectName, String message) {
        ILabelProvider labelProvider = new JavaElementLabelProvider(
                JavaElementLabelProvider.SHOW_DEFAULT);
        ElementListSelectionDialog dialog = new ElementListSelectionDialog(
                mParentShell, labelProvider);
        dialog.setTitle("Project Selection");
        if (message == null) {
            message = "Please select a project";
        }
        dialog.setMessage(message);

        IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        IJavaModel javaModel = JavaCore.create(workspaceRoot);

        // set the elements in the dialog. These are opened android projects.
        dialog.setElements(getAndroidProjects(javaModel));

        // look for the project matching the given project name
        IJavaProject javaProject = null;
        if (projectName != null && projectName.length() > 0) {
            javaProject = javaModel.getJavaProject(projectName);
        }

        // if we found it, we set the initial selection in the dialog to this one.
        if (javaProject != null) {
            dialog.setInitialSelections(new Object[] { javaProject });
        }

        // open the dialog and return the object selected if OK was clicked, or null otherwise
        if (dialog.open() == Window.OK) {
            return (IJavaProject) dialog.getFirstResult();
        }
        return null;
    }

    /**
     * Returns the list of Android projects.
     * <p/>
     * Because this list can be time consuming, this class caches the list of project.
     * It is recommended to call this method instead of
     * {@link BaseProjectHelper#getAndroidProjects()}.
     *
     * @param javaModel the java model. Can be null.
     */
    public IJavaProject[] getAndroidProjects(IJavaModel javaModel) {
        // recompute only if we don't have the projects already or the filter is dynamic
        // and prevent usage of a cache.
        if (mAndroidProjects == null || (mFilter != null && mFilter.useCache() == false)) {
            if (javaModel == null) {
                mAndroidProjects = BaseProjectHelper.getAndroidProjects(mFilter);
            } else {
                mAndroidProjects = BaseProjectHelper.getAndroidProjects(javaModel, mFilter);
            }
        }

        return mAndroidProjects;
    }

    /**
     * Helper method to get the Android project with the given name
     *
     * @param projectName the name of the project to find
     * @return the {@link IProject} for the Android project. <code>null</code> if not found.
     */
    public IProject getAndroidProject(String projectName) {
        IProject iproject = null;
        IJavaProject[] javaProjects = getAndroidProjects(null);
        if (javaProjects != null) {
            for (IJavaProject javaProject : javaProjects) {
                if (javaProject.getElementName().equals(projectName)) {
                    iproject = javaProject.getProject();
                    break;
                }
            }
        }
        return iproject;
    }

    /**
     * A selector combo for showing the currently selected project and for
     * changing the selection
     */
    public static class ProjectCombo extends Combo implements SelectionListener {
        /** Currently chosen project, or null when no project has been initialized or selected */
        private IProject mProject;
        private IJavaProject[] mAvailableProjects;

        /**
         * Creates a new project selector combo
         *
         * @param helper associated {@link ProjectChooserHelper} for looking up
         *            projects
         * @param parent parent composite to add the combo to
         * @param initialProject the initial project to select, or null (which
         *            will show a "Please Choose Project..." label instead.)
         */
        public ProjectCombo(ProjectChooserHelper helper, Composite parent,
                IProject initialProject) {
            super(parent, SWT.BORDER | SWT.FLAT | SWT.READ_ONLY);
            mProject = initialProject;

            mAvailableProjects = helper.getAndroidProjects(null);
            String[] items = new String[mAvailableProjects.length + 1];
            items[0] = "--- Choose Project ---";

            ILabelProvider labelProvider = new JavaElementLabelProvider(
                    JavaElementLabelProvider.SHOW_DEFAULT);
            int selectionIndex = 0;
            for (int i = 0, n = mAvailableProjects.length; i < n; i++) {
                IProject project = mAvailableProjects[i].getProject();
                items[i + 1] = labelProvider.getText(project);
                if (project == initialProject) {
                    selectionIndex = i + 1;
                }
            }
            setItems(items);
            select(selectionIndex);

            addSelectionListener(this);
        }

        /**
         * Returns the project selected by this chooser (or the initial project
         * passed to the constructor if the user did not change it)
         *
         * @return the selected project
         */
        public IProject getSelectedProject() {
            return mProject;
        }

        /**
         * Sets the project selected by this chooser
         *
         * @param project the selected project
         */
        public void setSelectedProject(IProject project) {
            mProject = project;

            int selectionIndex = 0;
            for (int i = 0, n = mAvailableProjects.length; i < n; i++) {
                if (project == mAvailableProjects[i].getProject()) {
                    selectionIndex = i + 1; // +1: Slot 0 is reserved for "Choose Project"
                    select(selectionIndex);
                    break;
                }
            }
        }

        /**
         * Click handler for the button: Open the {@link ProjectChooserHelper}
         * dialog for selecting a new project.
         */
        @Override
        public void widgetSelected(SelectionEvent e) {
            int selectionIndex = getSelectionIndex();
            if (selectionIndex > 0 && mAvailableProjects != null
                    && selectionIndex <= mAvailableProjects.length) {
                // selection index 0 is "Choose Project", all other projects are offset
                // by 1 from the selection index
                mProject = mAvailableProjects[selectionIndex - 1].getProject();
            } else {
                mProject = null;
            }
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent e) {
        }

        @Override
        protected void checkSubclass() {
            // Disable the check that prevents subclassing of SWT components
        }
    }
}

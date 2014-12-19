/*
 * Copyright (C) 2013 The Android Open Source Project
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

package org.eclipse.andmore.internal.wizards.exportgradle;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.ibm.icu.text.MessageFormat;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays a wizard page that lets the user choose the projects for which to create Gradle build
 * files.
 * <p>
 * Based on {@link org.eclipse.ant.internal.ui.datatransfer.AntBuildfileExportPage}
 */
public class ProjectSelectionPage extends WizardPage {

    private final ProjectSetupBuilder mBuilder;
    private CheckboxTableViewer mTableViewer;
    private List<IJavaProject> mSelectedJavaProjects = Lists.newArrayList();

    public ProjectSelectionPage(ProjectSetupBuilder builder) {
        super("GradleExportPage"); //$NON-NLS-1$
        mBuilder = builder;
        setPageComplete(false);
        setTitle(ExportMessages.PageTitle);
        setDescription(ExportMessages.PageDescription);
    }

    @Override
    public void createControl(Composite parent) {
        initializeDialogUnits(parent);

        Composite workArea = new Composite(parent, SWT.NONE);
        setControl(workArea);

        workArea.setLayout(new GridLayout());
        workArea.setLayoutData(new GridData(GridData.FILL_BOTH
                | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));

        Label title = new Label(workArea, SWT.NONE);
        title.setText(ExportMessages.SelectProjects);

        Composite listComposite = new Composite(workArea, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        layout.marginWidth = 0;
        layout.makeColumnsEqualWidth = false;
        listComposite.setLayout(layout);

        listComposite.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL
                | GridData.GRAB_VERTICAL | GridData.FILL_BOTH));

        Table table = new Table(listComposite,
                SWT.CHECK | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        mTableViewer = new CheckboxTableViewer(table);
        table.setLayout(new TableLayout());
        GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
        data.heightHint = 300;
        table.setLayoutData(data);
        mTableViewer.setContentProvider(new WorkbenchContentProvider() {
            @Override
            public Object[] getElements(Object element) {
                if (element instanceof IJavaProject[]) {
                    return (IJavaProject[]) element;
                }
                return null;
            }
        });
        mTableViewer.setLabelProvider(new WorkbenchLabelProvider());
        mTableViewer.addCheckStateListener(new ICheckStateListener() {
            @Override
            public void checkStateChanged(CheckStateChangedEvent event) {
                if (event.getChecked()) {
                    mSelectedJavaProjects.add((IJavaProject) event.getElement());
                } else {
                    mSelectedJavaProjects.remove(event.getElement());
                }
                updateEnablement();
            }
        });

        initializeProjects();
        createSelectionButtons(listComposite);
        setControl(workArea);
        updateEnablement();
        Dialog.applyDialogFont(parent);
    }

    /**
     * Creates select all/deselect all buttons.
     */
    private void createSelectionButtons(Composite composite) {
        Composite buttonsComposite = new Composite(composite, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        buttonsComposite.setLayout(layout);

        buttonsComposite.setLayoutData(new GridData(
                GridData.VERTICAL_ALIGN_BEGINNING));

        Button selectAll = new Button(buttonsComposite, SWT.PUSH);
        selectAll.setText(ExportMessages.SelectAll);
        selectAll.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                for (int i = 0; i < mTableViewer.getTable().getItemCount(); i++) {
                    mSelectedJavaProjects.add((IJavaProject) mTableViewer.getElementAt(i));
                }
                mTableViewer.setAllChecked(true);
                updateEnablement();
            }
        });
        setButtonLayoutData(selectAll);

        Button deselectAll = new Button(buttonsComposite, SWT.PUSH);
        deselectAll.setText(ExportMessages.DeselectAll);
        deselectAll.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                mSelectedJavaProjects.clear();
                mTableViewer.setAllChecked(false);
                updateEnablement();
            }
        });
        setButtonLayoutData(deselectAll);
    }

    /**
     * Populates the list with all the eligible projects in the workspace.
     */
    private void initializeProjects() {
        IWorkspaceRoot rootWorkspace = ResourcesPlugin.getWorkspace().getRoot();
        IJavaModel javaModel = JavaCore.create(rootWorkspace);
        IJavaProject[] javaProjects;
        try {
            javaProjects = javaModel.getJavaProjects();
        } catch (JavaModelException e) {
            javaProjects = new IJavaProject[0];
        }
        mTableViewer.setInput(javaProjects);
        // Check any necessary projects
        if (mSelectedJavaProjects != null) {
            mTableViewer.setCheckedElements(mSelectedJavaProjects.toArray(
                    new IJavaProject[mSelectedJavaProjects.size()]));
        }
    }

    /**
     * Enables/disables the finish button on the wizard and displays error messages as needed.
     */
    private void updateEnablement() {
        String error = null;
        try {
            if (mSelectedJavaProjects.size() == 0) {
                error = ExportMessages.NoProjectsError;
                return;
            }

            List<String> cyclicProjects;
            try {
                cyclicProjects = getCyclicProjects(mSelectedJavaProjects);
                if (cyclicProjects.size() > 0) {
                    error = MessageFormat.format(ExportMessages.CyclicProjectsError,
                            new Object[] { Joiner.on(", ").join(cyclicProjects) }); //$NON-NLS-1$
                    return;
                }

                error = mBuilder.setProject(mSelectedJavaProjects);
                if (error != null) {
                    return;
                }

            } catch (CoreException ignored) {
                // TODO: do something?
            }
        } finally {
            setErrorMessage(error);
            setPageComplete(error == null);
            getContainer().updateButtons();
        }
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            mTableViewer.getTable().setFocus();
            mBuilder.setCanFinish(false);
            mBuilder.setCanGenerate(false);
        }
    }

    /**
     * Returns given projects that have cyclic dependencies.
     *
     * @param javaProjects list of IJavaProject objects
     * @return set of project names
     */
    private List<String> getCyclicProjects(List<IJavaProject> projects) throws CoreException {

        List<String> cyclicProjects = new ArrayList<String>();
        for (IJavaProject javaProject : projects) {
            if (hasCyclicDependency(javaProject)) {
                cyclicProjects.add(javaProject.getProject().getName());
            }
        }
        return cyclicProjects;
    }

    /**
     * Check if given project has a cyclic dependency.
     * <p>
     * See {@link org.eclipse.jdt.core.tests.model.ClasspathTests.numberOfCycleMarkers}
     */
    private static boolean hasCyclicDependency(IJavaProject javaProject)
            throws CoreException {
        IMarker[] markers = javaProject.getProject().findMarkers(
                IJavaModelMarker.BUILDPATH_PROBLEM_MARKER, false,
                IResource.DEPTH_ONE);
        for (IMarker marker : markers) {
            String cycleAttr = (String) marker
                    .getAttribute(IJavaModelMarker.CYCLE_DETECTED);
            if (cycleAttr != null && cycleAttr.equals("true")) { //$NON-NLS-1$
                return true;
            }
        }
        return false;
    }
}
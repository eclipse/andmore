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

import com.google.common.collect.Lists;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import java.io.File;
import java.util.Collection;
import java.util.List;

/**
 * Confirmation page to review the actual project export
 * list and see warning about existing files.
 *
 */
public class ConfirmationPage extends WizardPage {

    private final ProjectSetupBuilder mBuilder;
    private TableViewer mTableViewer;
    private Label mModuleDescription1;
    private Label mModuleDescription2;
    private Label mModuleDescription3;
    private Label mProjectRootLabel;
    private Label mProjectRootWarning;
    private List<IJavaProject> mOverrideProjects;
    private boolean mOverrideWarning;
    private Button mForceOverride;

    public ConfirmationPage(ProjectSetupBuilder builder) {
        super("ConfirmationPage"); //$NON-NLS-1$
        mBuilder = builder;
        setPageComplete(false);
        setTitle(ExportMessages.PageTitle);
        setDescription(ExportMessages.PageDescription);
    }

    @Override
    public void createControl(Composite parent) {
        initializeDialogUnits(parent);
        GridData data;

        Composite workArea = new Composite(parent, SWT.NONE);
        setControl(workArea);

        workArea.setLayout(new GridLayout());
        workArea.setLayoutData(new GridData(GridData.FILL_BOTH
                | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));

        Label title = new Label(workArea, SWT.NONE);
        title.setText("Please review the export options.");

        Group group = new Group(workArea, SWT.NONE);
        group.setText("Project root");
        group.setLayout(new GridLayout());
        group.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));

        mProjectRootLabel = new Label(group, SWT.NONE);
        mProjectRootLabel.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));

        mProjectRootWarning = new Label(group, SWT.NONE);
        mProjectRootWarning.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));

        Group group2 = new Group(workArea, SWT.NONE);
        group2.setText("Exported Modules");
        group2.setLayout(new GridLayout());
        group2.setLayoutData(data = new GridData(SWT.FILL, SWT.FILL, true, true));
        data.heightHint = 300;

        Table table = new Table(group2, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        mTableViewer = new TableViewer(table);
        table.setLayout(new TableLayout());
        table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        mTableViewer.setContentProvider(new IStructuredContentProvider() {
            @Override
            public Object[] getElements(Object inputElement) {
                if (inputElement instanceof ProjectSetupBuilder) {
                    ProjectSetupBuilder builder = (ProjectSetupBuilder) inputElement;
                    Collection<GradleModule> modules = builder.getModules();
                    Object[] array = new Object[modules.size()];
                    int i = 0;
                    for (GradleModule module : modules) {
                        array[i++] = module.getJavaProject();
                    }

                    return array;
                }

                return null;
            }

            @Override
            public void dispose() {
            }

            @Override
            public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            }

        });
        mTableViewer.setLabelProvider(new WorkbenchLabelProvider() {
            @Override
            protected String decorateText(String input, Object element) {
                if (element instanceof IJavaProject) {
                    IJavaProject javaProject = (IJavaProject) element;
                    StringBuilder sb = new StringBuilder(input);
                    if (!mBuilder.isOriginalProject(javaProject)) {
                        sb.append('*');
                    }
                    // TODO: decorate icon instead?
                    if (mOverrideProjects.contains(javaProject)) {
                        sb.append(" (1 warning)");
                    }

                    return sb.toString();
                }

                return input;
            }
        });
        mTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                IStructuredSelection selection = (IStructuredSelection) event.getSelection();
                Object firstElement = selection.getFirstElement();
                if (firstElement instanceof IJavaProject) {
                    GradleModule module = mBuilder.getModule((IJavaProject) firstElement);
                    if (mBuilder.getOriginalModules().contains(module)) {
                        mModuleDescription1.setText("Exported because selected in previous page.");
                    } else {
                        List<GradleModule> list = mBuilder.getShortestDependencyTo(module);
                        StringBuilder sb = new StringBuilder();
                        for (GradleModule m : list) {
                            if (sb.length() > 0) {
                                sb.append(" > ");
                            }
                            sb.append(m.getJavaProject().getProject().getName());
                        }
                        mModuleDescription1.setText("Dependency chain: " + sb);
                    }
                    mModuleDescription2.setText("Path: " + module.getPath());

                    if (mOverrideProjects.contains(module.getJavaProject())) {
                        mModuleDescription3.setText(
                                "WARNING: build.gradle already exists for this project");
                    } else {
                        mModuleDescription3.setText("");
                    }
                } else {
                    mModuleDescription1.setText("");
                    mModuleDescription2.setText("");
                    mModuleDescription3.setText("");
                }
            }
        });

        mModuleDescription1 = new Label(group2, SWT.NONE);
        mModuleDescription1.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
        mModuleDescription2 = new Label(group2, SWT.NONE);
        mModuleDescription2.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
        mModuleDescription3 = new Label(group2, SWT.NONE);
        mModuleDescription3.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));

        mForceOverride = new Button(workArea, SWT.CHECK);
        mForceOverride.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
        mForceOverride.setText("Force overriding of existing files");
        mForceOverride.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateEnablement();
            }
        });

        setControl(workArea);
        Dialog.applyDialogFont(parent);
    }

    /**
     * Get list of projects which have already a buildfile.
     *
     * @param javaProjects list of IJavaProject objects
     * @return set of project names
     */
    private void computeOverride(String commonRoot) {
        mOverrideProjects = Lists.newArrayList();
        for (GradleModule module : mBuilder.getModules()) {
            if (new File(module.getProject().getLocation().toFile(),
                    BuildFileCreator.BUILD_FILE).exists()) {
                mOverrideProjects.add(module.getJavaProject());
            }
        }

        // also check on the root settings.gradle/build.gradle
        boolean settingsFile = new File(commonRoot, BuildFileCreator.SETTINGS_FILE).exists();
        boolean buildFile = new File(commonRoot, BuildFileCreator.BUILD_FILE).exists();
        if (settingsFile && buildFile) {
             mProjectRootWarning.setText(
                     "WARNING: build.gradle/settings.gradle already exists at this location.");
        } else if (settingsFile) {
            mProjectRootWarning.setText(
                    "WARNING: settings.gradle already exists at this location.");
        } else if (buildFile) {
            mProjectRootWarning.setText("WARNING: build.gradle already exists at this location.");
        }

        mOverrideWarning = mOverrideProjects.size() > 0 || settingsFile || buildFile;
    }

    /**
     * Enables/disables the finish button on the wizard and displays error messages as needed.
     */
    private void updateEnablement() {
        if (mOverrideWarning && !mForceOverride.getSelection()) {
            setErrorMessage("Enable overriding of existing files before clicking Finish");
            mBuilder.setCanGenerate(false);
        } else {
            setErrorMessage(null);
            mBuilder.setCanGenerate(true);
        }
        setPageComplete(false);
        getContainer().updateButtons();
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            mProjectRootWarning.setText("");

            String commonRoot = mBuilder.getCommonRoot().toOSString();
            computeOverride(commonRoot);
            mProjectRootLabel.setText(commonRoot);
            mTableViewer.setInput(mBuilder);
            mTableViewer.getTable().setFocus();
            mBuilder.setCanFinish(false);
            mBuilder.setCanGenerate(true);
            updateEnablement();
        }
    }
}
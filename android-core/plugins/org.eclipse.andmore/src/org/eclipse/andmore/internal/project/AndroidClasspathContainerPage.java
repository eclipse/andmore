/*
 * Copyright (C) 2010 The Android Open Source Project
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
 *
 *******************************************************************************/

package org.eclipse.andmore.internal.project;

import org.eclipse.andmore.AdtConstants;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPage;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPageExtension;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import java.util.Arrays;

public class AndroidClasspathContainerPage extends WizardPage implements IClasspathContainerPage,
        IClasspathContainerPageExtension {

    private IProject mOwnerProject;

    private String mLibsProjectName;

    private Combo mProjectsCombo;

    private IStatus mCurrStatus;

    private boolean mPageVisible;

    public AndroidClasspathContainerPage() {
        super("AndroidClasspathContainerPage"); //$NON-NLS-1$
        mPageVisible = false;
        mCurrStatus = new StatusInfo();
        setTitle("Android Libraries");
        setDescription("This container manages classpath entries for Android container");
    }

    @Override
    public IClasspathEntry getSelection() {
        IPath path = new Path(AdtConstants.CONTAINER_FRAMEWORK);

        final int index = this.mProjectsCombo.getSelectionIndex();
        if (index != -1) {
            final String selectedProjectName = this.mProjectsCombo.getItem(index);

            if (this.mOwnerProject == null
                    || !selectedProjectName.equals(this.mOwnerProject.getName())) {
                path = path.append(selectedProjectName);
            }
        }

        return JavaCore.newContainerEntry(path);
    }

    @Override
    public void setSelection(final IClasspathEntry cpentry) {
        final IPath path = cpentry == null ? null : cpentry.getPath();

        if (path == null || path.segmentCount() == 1) {
            if (this.mOwnerProject != null) {
                this.mLibsProjectName = this.mOwnerProject.getName();
            }
        } else {
            this.mLibsProjectName = path.segment(1);
        }
    }

    @Override
    public void createControl(final Composite parent) {
        final Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(2, false));

        final Label label = new Label(composite, SWT.NONE);
        label.setText("Project:");

        final String[] androidProjects = getAndroidProjects();

        this.mProjectsCombo = new Combo(composite, SWT.READ_ONLY);
        this.mProjectsCombo.setItems(androidProjects);

        final int index;

        if (this.mOwnerProject != null) {
            index = indexOf(androidProjects, this.mLibsProjectName);
        } else {
            if (this.mProjectsCombo.getItemCount() > 0) {
                index = 0;
            } else {
                index = -1;
            }
        }

        if (index != -1) {
            this.mProjectsCombo.select(index);
        }

        final GridData gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.minimumWidth = 100;

        this.mProjectsCombo.setLayoutData(gd);

        setControl(composite);
    }

    @Override
    public boolean finish() {
        return true;
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        mPageVisible = visible;
        // policy: wizards are not allowed to come up with an error message
        if (visible && mCurrStatus.matches(IStatus.ERROR)) {
            StatusInfo status = new StatusInfo();
            status.setError(""); //$NON-NLS-1$
            mCurrStatus = status;
        }
        updateStatus(mCurrStatus);
    }

    /**
     * Updates the status line and the OK button according to the given status
     *
     * @param status status to apply
     */
    protected void updateStatus(IStatus status) {
        mCurrStatus = status;
        setPageComplete(!status.matches(IStatus.ERROR));
        if (mPageVisible) {
            StatusUtil.applyToStatusLine(this, status);
        }
    }

    /**
     * Updates the status line and the OK button according to the status
     * evaluate from an array of status. The most severe error is taken. In case
     * that two status with the same severity exists, the status with lower
     * index is taken.
     *
     * @param status the array of status
     */
    protected void updateStatus(IStatus[] status) {
        updateStatus(StatusUtil.getMostSevere(status));
    }

    @Override
    public void initialize(final IJavaProject project, final IClasspathEntry[] currentEntries) {
        this.mOwnerProject = (project == null ? null : project.getProject());
    }

    private static String[] getAndroidProjects() {
        IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
        final String[] names = new String[projects.length];
        for (int i = 0; i < projects.length; i++) {
            names[i] = projects[i].getName();
        }
        Arrays.sort(names);
        return names;
    }

    private static int indexOf(final String[] array, final String str) {
        for (int i = 0; i < array.length; i++) {
            if (array[i].equals(str)) {
                return i;
            }
        }
        return -1;
    }

}

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

package org.eclipse.andmore.internal.properties;

import com.android.SdkConstants;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.internal.project.ProjectProperties;
import com.android.sdklib.internal.project.ProjectPropertiesWorkingCopy;
import com.android.sdkuilib.internal.widgets.SdkTargetSelector;

import java.util.Collection;

import org.eclipse.andmore.AndmoreAndroidPlugin;
import org.eclipse.andmore.internal.sdk.ProjectState;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.dialogs.PropertyPage;

/**
 * Property page for "Android" project.
 * This is accessible from the Package Explorer when right clicking a project and choosing
 * "Properties".
 *
 */
public class AndroidPropertyPage extends PropertyPage {

    private IProject mProject;
    private SdkTargetSelector mSelector;
    private Button mIsLibrary;
    // APK-SPLIT: This is not yet supported, so we hide the UI
//    private Button mSplitByDensity;
    private LibraryProperties mLibraryDependencies;
    private ProjectPropertiesWorkingCopy mPropertiesWorkingCopy;

    public AndroidPropertyPage() {
        // pass
    }

    @Override
    protected Control createContents(Composite parent) {
        // get the element (this is not yet valid in the constructor).
        mProject = (IProject)getElement();

        // get the targets from the sdk
        IAndroidTarget[] targets = null;
        if (Sdk.getCurrent() != null) {
            targets = Sdk.getCurrent().getTargets().toArray(new IAndroidTarget[0]);
        }

        // build the UI.
        Composite top = new Composite(parent, SWT.NONE);
        top.setLayoutData(new GridData(GridData.FILL_BOTH));
        top.setLayout(new GridLayout(1, false));

        Group targetGroup = new Group(top, SWT.NONE);
        targetGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
        targetGroup.setLayout(new GridLayout(1, false));
        targetGroup.setText("Project Build Target");

        mSelector = new SdkTargetSelector(targetGroup, targets);

        Group libraryGroup = new Group(top, SWT.NONE);
        libraryGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
        libraryGroup.setLayout(new GridLayout(1, false));
        libraryGroup.setText("Library");

        mIsLibrary = new Button(libraryGroup, SWT.CHECK);
        mIsLibrary.setText("Is Library");

        mLibraryDependencies = new LibraryProperties(libraryGroup);

        // fill the ui
        fillUi();

        // add callbacks
        mSelector.setSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateValidity();
            }
        });

        if (mProject.isOpen() == false) {
            // disable the ui.
        }

        return top;
    }

    @Override
    public boolean performOk() {
        Sdk currentSdk = Sdk.getCurrent();
        if (currentSdk != null && mProject.isOpen()) {
            ProjectState state = Sdk.getProjectState(mProject);

            // simply update the properties copy. Eclipse will be notified of the file change
            // and will reload it smartly (detecting differences) and updating the ProjectState.
            // See Sdk.mFileListener
            boolean mustSaveProp = false;

            IAndroidTarget newTarget = mSelector.getSelected();
            if (state == null || newTarget != state.getTarget()) {
                mPropertiesWorkingCopy.setProperty(ProjectProperties.PROPERTY_TARGET,
                        newTarget.hashString());
                mustSaveProp = true;
            }

            if (state == null || mIsLibrary.getSelection() != state.isLibrary()) {
                mPropertiesWorkingCopy.setProperty(ProjectProperties.PROPERTY_LIBRARY,
                        Boolean.toString(mIsLibrary.getSelection()));
                mustSaveProp = true;
            }

            if (mLibraryDependencies.save()) {
                mustSaveProp = true;
            }

            if (mustSaveProp) {
                try {
                    mPropertiesWorkingCopy.save();

                    IResource projectProp = mProject.findMember(SdkConstants.FN_PROJECT_PROPERTIES);
                    if (projectProp != null) {
                        projectProp.refreshLocal(IResource.DEPTH_ZERO, new NullProgressMonitor());
                    }
                } catch (Exception e) {
                    String msg = String.format(
                            "Failed to save %1$s for project %2$s",
                            SdkConstants.FN_PROJECT_PROPERTIES, mProject.getName());
                    AndmoreAndroidPlugin.log(e, msg);
                }
            }
        }

        return true;
    }

    @Override
    protected void performDefaults() {
        fillUi();
        updateValidity();
    }

    private void fillUi() {
        if (Sdk.getCurrent() != null && mProject.isOpen()) {
            ProjectState state = Sdk.getProjectState(mProject);

            // make a working copy of the properties
            mPropertiesWorkingCopy = state.getProperties().makeWorkingCopy();

            // get the target
            IAndroidTarget target = state.getTarget();
            if (target != null) {
                mSelector.setSelection(target);
            }

            mIsLibrary.setSelection(state.isLibrary());
            mLibraryDependencies.setContent(state, mPropertiesWorkingCopy);
        }

    }

    private void updateValidity() {
        // look for the selection and validate the page if there is a selection
        IAndroidTarget target = mSelector.getSelected();
        setValid(target != null);
    }
}

/*
 * Copyright (C) 2012 The Android Open Source Project
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
package org.eclipse.andmore.internal.wizards.newproject;

import static com.android.SdkConstants.FN_PROJECT_PROGUARD_FILE;
import static com.android.SdkConstants.OS_SDK_TOOLS_LIB_FOLDER;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.AdtUtils;
import org.eclipse.andmore.internal.wizards.newproject.NewProjectWizardState.Mode;
import org.eclipse.jdt.ui.actions.OpenJavaPerspectiveAction;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

import java.io.File;


/**
 * An "Import Android Project" wizard.
 */
public class ImportProjectWizard extends Wizard implements INewWizard {
    private static final String PROJECT_LOGO_LARGE = "icons/android-64.png"; //$NON-NLS-1$

    private NewProjectWizardState mValues;
    private ImportPage mImportPage;
    private IStructuredSelection mSelection;

    /** Constructs a new wizard default project wizard */
    public ImportProjectWizard() {
    }

    @Override
    public void addPages() {
        mValues = new NewProjectWizardState(Mode.ANY);
        mImportPage = new ImportPage(mValues);
        if (mSelection != null) {
            mImportPage.init(mSelection, AdtUtils.getActivePart());
        }
        addPage(mImportPage);
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        mSelection = selection;

        setHelpAvailable(false); // TODO have help
        ImageDescriptor desc = AdtPlugin.getImageDescriptor(PROJECT_LOGO_LARGE);
        setDefaultPageImageDescriptor(desc);

        // Trigger a check to see if the SDK needs to be reloaded (which will
        // invoke onSdkLoaded asynchronously as needed).
        AdtPlugin.getDefault().refreshSdk();
    }

    @Override
    public boolean performFinish() {
        File file = new File(AdtPlugin.getOsSdkFolder(), OS_SDK_TOOLS_LIB_FOLDER + File.separator
                + FN_PROJECT_PROGUARD_FILE);
        if (!file.exists()) {
            AdtPlugin.displayError("Tools Out of Date?",
            String.format("It looks like you do not have the latest version of the "
                    + "SDK Tools installed. Make sure you update via the SDK Manager "
                    + "first. (Could not find %1$s)", file.getPath()));
            return false;
        }

        NewProjectCreator creator = new NewProjectCreator(mValues, getContainer());
        if (!(creator.createAndroidProjects())) {
            return false;
        }

        // Open the default Java Perspective
        OpenJavaPerspectiveAction action = new OpenJavaPerspectiveAction();
        action.run();
        return true;
    }
}

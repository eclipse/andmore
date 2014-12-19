/*
 * Copyright (C) 2011 The Android Open Source Project
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
package org.eclipse.andmore.internal.assetstudio;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.ui.actions.AbstractOpenWizardAction;
import org.eclipse.ui.INewWizard;

import java.util.List;

/** An action for opening the Create Icon Set wizard */
public class OpenCreateAssetSetWizardAction extends AbstractOpenWizardAction {
    private IProject mProject;
    private CreateAssetSetWizard mWizard;

    /**
     * Creates a new {@link #OpenCreateAssetSetWizardAction} instance
     *
     * @param project the initial project to associate with the wizard
     */
    public OpenCreateAssetSetWizardAction(IProject project) {
        mProject = project;
    }


    @Override
    protected INewWizard createWizard() throws CoreException {
        mWizard = new CreateAssetSetWizard();
        mWizard.setProject(mProject);
        return mWizard;
    }

    /**
     * Returns the list of files created by the wizard. Must only be called
     * after this action's {@link #run()} method has been called. May return
     * null if the user cancels out of the wizard.
     *
     * @return a list of files created by the wizard, or null
     */
    public List<IResource> getCreatedFiles() {
        return mWizard.getCreatedFiles();
    }
}

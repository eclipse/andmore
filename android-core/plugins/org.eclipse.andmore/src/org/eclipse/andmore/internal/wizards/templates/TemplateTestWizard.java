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
package org.eclipse.andmore.internal.wizards.templates;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.ui.IWorkbench;

import java.io.File;

/**
 * Template wizard which creates parameterized templates
 */
public class TemplateTestWizard extends NewTemplateWizard {
    private TemplateTestPage mSelectionPage;
    private IProject mProject;

    /** Creates a new wizard for testing template definitions in a local directory */
    public TemplateTestWizard() {
        super("");
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        super.init(workbench, selection);
        if (mValues != null) {
            mProject = mValues.project;
        }

        mMainPage = null;
        mValues = null;

        mSelectionPage = new TemplateTestPage();
    }

    @Override
    public void addPages() {
        addPage(mSelectionPage);
    }

    @Override
    public IWizardPage getNextPage(IWizardPage page) {
        if (page == mSelectionPage) {
            File file = mSelectionPage.getLocation();
            if (file != null && file.exists()) {
                if (mValues == null) {
                    mValues = new NewTemplateWizardState();
                    mValues.setTemplateLocation(file);
                    mValues.project = mProject;
                    hideBuiltinParameters();

                    mMainPage = new NewTemplatePage(mValues, true);
                    addPage(mMainPage);
                } else {
                    mValues.setTemplateLocation(file);
                }

                return mMainPage;
            }
        }

        return super.getNextPage(page);
    }
}

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

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

public class GradleExportWizard extends Wizard implements IExportWizard {

    private ProjectSetupBuilder mBuilder = new ProjectSetupBuilder();

    private ProjectSelectionPage mFirstPage;
    private ConfirmationPage mSecondPage;
    private FinalPage mFinalPage;

    /**
     * Creates buildfile.
     */
    @Override
    public boolean performFinish() {
        if (mBuilder.canGenerate()) {
            generateBuildfiles(mSecondPage);
            getContainer().showPage(mFinalPage);
            return false;
        }

        return true;
    }

    @Override
    public void addPages() {
        addPage(new ImportInsteadPage());
        mFirstPage = new ProjectSelectionPage(mBuilder);
        addPage(mFirstPage);
        mSecondPage = new ConfirmationPage(mBuilder);
        addPage(mSecondPage);
        mFinalPage = new FinalPage(mBuilder);
        addPage(mFinalPage);
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        setWindowTitle(ExportMessages.WindowTitle);
        setNeedsProgressMonitor(true);
    }

    @Override
    public boolean canFinish() {
        return mBuilder.canFinish() || mBuilder.canGenerate();
    }

    /**
     * Converts Eclipse Java projects to Gradle build files. Displays error dialogs.
     */
    public boolean generateBuildfiles(final WizardPage page) {
        IRunnableWithProgress runnable = new IRunnableWithProgress() {
            @Override
            public void run(IProgressMonitor pm) throws InterruptedException {
                Collection<GradleModule> modules = mBuilder.getModules();
                final int count = modules.size();

                SubMonitor localmonitor = SubMonitor.convert(pm, ExportMessages.StatusMessage,
                        count);
                BuildFileCreator.createBuildFiles(
                        mBuilder,
                        page.getShell(),
                        localmonitor.newChild(count));
            }
        };

        try {
            getContainer().run(false, false, runnable);
        } catch (InvocationTargetException e) {
            AdtPlugin.log(e, null);
            return false;
        } catch (InterruptedException e) {
            AdtPlugin.log(e, null);
            return false;
        }
        if (page.getErrorMessage() != null) {
            return false;
        }
        return true;
    }

}

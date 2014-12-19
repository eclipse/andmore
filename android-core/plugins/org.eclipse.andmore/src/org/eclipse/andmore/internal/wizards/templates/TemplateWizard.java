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

import static org.eclipse.core.resources.IResource.DEPTH_INFINITE;

import com.android.annotations.NonNull;
import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.assetstudio.ConfigureAssetSetPage;
import org.eclipse.andmore.internal.assetstudio.CreateAssetSetWizardState;
import org.eclipse.andmore.internal.editors.IconFactory;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

abstract class TemplateWizard extends Wizard implements INewWizard {
    private static final String PROJECT_LOGO_LARGE = "android-64"; //$NON-NLS-1$
    protected IWorkbench mWorkbench;
    private UpdateToolsPage mUpdatePage;
    private InstallDependencyPage mDependencyPage;
    private TemplatePreviewPage mPreviewPage;
    protected ConfigureAssetSetPage mIconPage;

    protected TemplateWizard() {
    }

    /** Should this wizard add an icon page? */
    protected boolean shouldAddIconPage() {
        return false;
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        mWorkbench = workbench;

        setHelpAvailable(false);
        ImageDescriptor desc = IconFactory.getInstance().getImageDescriptor(PROJECT_LOGO_LARGE);
        setDefaultPageImageDescriptor(desc);

        if (!UpdateToolsPage.isUpToDate()) {
            mUpdatePage = new UpdateToolsPage();
        }

        setNeedsProgressMonitor(true);

        // Trigger a check to see if the SDK needs to be reloaded (which will
        // invoke onSdkLoaded asynchronously as needed).
        AdtPlugin.getDefault().refreshSdk();
    }

    @Override
    public void addPages() {
        super.addPages();
        if (mUpdatePage != null) {
            addPage(mUpdatePage);
        }
    }

    @Override
    public IWizardPage getStartingPage() {
        if (mUpdatePage != null && mUpdatePage.isPageComplete()) {
            return getNextPage(mUpdatePage);
        }

        return super.getStartingPage();
    }

    protected WizardPage getPreviewPage(NewTemplateWizardState values) {
        if (mPreviewPage == null) {
            mPreviewPage = new TemplatePreviewPage(values);
            addPage(mPreviewPage);
        }

        return mPreviewPage;
    }

    protected WizardPage getIconPage(CreateAssetSetWizardState iconState) {
        if (mIconPage == null) {
            mIconPage = new ConfigureAssetSetPage(iconState);
            mIconPage.setTitle("Configure Icon");
            addPage(mIconPage);
        }

        return mIconPage;
    }

    protected WizardPage getDependencyPage(TemplateMetadata template, boolean create) {
        if (!create) {
            return mDependencyPage;
        }

        if (mDependencyPage == null) {
            mDependencyPage = new InstallDependencyPage();
            addPage(mDependencyPage);
        }
        mDependencyPage.setTemplate(template);
        return mDependencyPage;
    }

    /**
     * Returns the project where the template is being inserted
     *
     * @return the project to insert the template into
     */
    @NonNull
    protected abstract IProject getProject();

    /**
     * Returns the list of files to open, which might be empty. This method will
     * only be called <b>after</b> {@link #computeChanges()} has been called.
     *
     * @return a list of files to open
     */
    @NonNull
    protected abstract List<String> getFilesToOpen();

    /**
     * Returns the list of files to open, which might be empty. This method will
     * only be called <b>after</b> {@link #computeChanges()} has been called.
     *
     * @return a list of files to open
     */
    @NonNull
    protected abstract List<Runnable> getFinalizingActions();

    /**
     * Computes the changes to the {@link #getProject()} this template should
     * perform
     *
     * @return the changes to perform
     */
    protected abstract List<Change> computeChanges();

    protected boolean performFinish(IProgressMonitor monitor) throws InvocationTargetException {
        List<Change> changes = computeChanges();
        if (!changes.isEmpty()) {
            monitor.beginTask("Creating template...", changes.size());
            try {
                CompositeChange composite = new CompositeChange("",
                        changes.toArray(new Change[changes.size()]));
                composite.perform(monitor);
            } catch (CoreException e) {
                AdtPlugin.log(e, null);
                throw new InvocationTargetException(e);
            } finally {
                monitor.done();
            }
        }

        // TBD: Is this necessary now that we're using IFile objects?
        try {
            getProject().refreshLocal(DEPTH_INFINITE, new NullProgressMonitor());
        } catch (CoreException e) {
            AdtPlugin.log(e, null);
        }
        return true;
    }

    @Override
    public boolean performFinish() {
        final AtomicBoolean success = new AtomicBoolean();
        try {
            getContainer().run(true, false, new IRunnableWithProgress() {
                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException,
                InterruptedException {
                    boolean ok = performFinish(monitor);
                    success.set(ok);
                }
            });

        } catch (InvocationTargetException e) {
            AdtPlugin.log(e, null);
            return false;
        } catch (InterruptedException e) {
            AdtPlugin.log(e, null);
            return false;
        }

        if (success.get()) {
            // Open the primary file/files
            NewTemplateWizard.openFiles(getProject(), getFilesToOpen(), mWorkbench);
            return true;
        } else {
            return false;
        }
    }
}

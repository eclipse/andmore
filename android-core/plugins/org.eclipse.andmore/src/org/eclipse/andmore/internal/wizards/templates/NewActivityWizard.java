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

import static org.eclipse.andmore.internal.wizards.templates.NewProjectWizard.ATTR_BUILD_API;
import static org.eclipse.andmore.internal.wizards.templates.NewProjectWizard.ATTR_MIN_API;
import static org.eclipse.andmore.internal.wizards.templates.NewProjectWizard.ATTR_MIN_API_LEVEL;
import static org.eclipse.andmore.internal.wizards.templates.NewProjectWizard.ATTR_PACKAGE_NAME;
import static org.eclipse.andmore.internal.wizards.templates.NewProjectWizard.ATTR_TARGET_API;
import com.android.annotations.NonNull;

import org.eclipse.andmore.AdtUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ui.IWorkbench;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Set;

/**
 * Wizard for creating new activities. This is a hybrid between a New Project
 * Wizard and a New Template Wizard: it has the "Activity selector" page from
 * the New Project Wizard, which is used to dynamically select a wizard for the
 * second page, but beyond that it runs the normal template wizard when it comes
 * time to create the template.
 */
public class NewActivityWizard extends TemplateWizard {
    private NewTemplatePage mTemplatePage;
    private ActivityPage mActivityPage;
    private NewProjectWizardState mValues;
    private NewTemplateWizardState mActivityValues;
    protected boolean mOnlyActivities;

    /** Creates a new {@link NewActivityWizard} */
    public NewActivityWizard() {
        mOnlyActivities = true;
    }

    @Override
    protected boolean shouldAddIconPage() {
        return mActivityValues.getIconState() != null;
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        super.init(workbench, selection);

        setWindowTitle(mOnlyActivities ? "New Activity" : "New Android Object");

        mValues = new NewProjectWizardState();
        mActivityPage = new ActivityPage(mValues, mOnlyActivities, false);

        mActivityValues = mValues.activityValues;
        List<IProject> projects = AdtUtils.getSelectedProjects(selection);
        if (projects.size() == 1) {
            mActivityValues.project = projects.get(0);
        }
    }

    @Override
    public void addPages() {
        super.addPages();
        addPage(mActivityPage);
    }

    @Override
    public IWizardPage getNextPage(IWizardPage page) {
        if (page == mActivityPage) {
            if (mTemplatePage == null) {
                Set<String> hidden = mActivityValues.hidden;
                hidden.add(ATTR_PACKAGE_NAME);
                hidden.add(ATTR_MIN_API);
                hidden.add(ATTR_MIN_API_LEVEL);
                hidden.add(ATTR_TARGET_API);
                hidden.add(ATTR_BUILD_API);

                mTemplatePage = new NewTemplatePage(mActivityValues, true);
                addPage(mTemplatePage);
            }
            return mTemplatePage;
        } else if (page == mTemplatePage && shouldAddIconPage()) {
            WizardPage iconPage = getIconPage(mActivityValues.getIconState());
            mActivityValues.updateIconState(mTemplatePage.getEvaluator());
            return iconPage;
        } else if (page == mTemplatePage
                || shouldAddIconPage() && page == getIconPage(mActivityValues.getIconState())) {
            TemplateMetadata template = mActivityValues.getTemplateHandler().getTemplate();
            if (template != null) {
                if (InstallDependencyPage.isInstalled(template.getDependencies())) {
                    return getPreviewPage(mActivityValues);
                } else {
                    return getDependencyPage(template, true);
                }
            }
        } else {
            TemplateMetadata template = mActivityValues.getTemplateHandler().getTemplate();
            if (template != null && page == getDependencyPage(template, false)) {
                return getPreviewPage(mActivityValues);
            }
        }

        return super.getNextPage(page);
    }

    @Override
    public boolean canFinish() {
        // Deal with lazy creation of some pages: these may not be in the page-list yet
        // since they are constructed lazily, so consider that option here.
        if (mTemplatePage == null || !mTemplatePage.isPageComplete()) {
            return false;
        }

        return super.canFinish();
    }

    @Override
    public boolean performFinish(IProgressMonitor monitor) throws InvocationTargetException {
        boolean success = super.performFinish(monitor);

        if (success) {
            List<Runnable> finalizingTasks = getFinalizingActions();
            for (Runnable r : finalizingTasks) {
                r.run();
            }
            return true;
        }
        return false;
    }

    @Override
    @NonNull
    protected IProject getProject() {
        return mActivityValues.project;
    }

    @Override
    @NonNull
    protected List<String> getFilesToOpen() {
        TemplateHandler activityTemplate = mActivityValues.getTemplateHandler();
        return activityTemplate.getFilesToOpen();
    }

    @Override
    @NonNull
    protected List<Runnable> getFinalizingActions() {
        TemplateHandler activityTemplate = mActivityValues.getTemplateHandler();
        return activityTemplate.getFinalizingActions();
    }

    @Override
    protected List<Change> computeChanges() {
        return mActivityValues.computeChanges();
    }

    /** Wizard for creating other Android components */
    public static class OtherWizard extends NewActivityWizard {
        /** Create new {@link OtherWizard} */
        public OtherWizard() {
            mOnlyActivities = false;
        }
    }
}

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

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.AdtUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

import java.io.File;
import java.util.List;
import java.util.Set;

/**
 * Template wizard which creates parameterized templates
 */
public class NewTemplateWizard extends TemplateWizard {
    /** Template name and location under $sdk/templates for the default activity */
    static final String BLANK_ACTIVITY = "activities/BlankActivity";           //$NON-NLS-1$
    /** Template name and location under $sdk/templates for the custom view template */
    static final String CUSTOM_VIEW = "other/CustomView";                      //$NON-NLS-1$

    protected NewTemplatePage mMainPage;
    protected NewTemplateWizardState mValues;
    private final String mTemplateName;

    NewTemplateWizard(String templateName) {
        mTemplateName = templateName;
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        super.init(workbench, selection);

        mValues = new NewTemplateWizardState();

        File template = TemplateManager.getTemplateLocation(mTemplateName);
        if (template != null) {
            mValues.setTemplateLocation(template);
        }
        hideBuiltinParameters();

        List<IProject> projects = AdtUtils.getSelectedProjects(selection);
        if (projects.size() == 1) {
            mValues.project = projects.get(0);
        }

        mMainPage = new NewTemplatePage(mValues, true);
    }

    @Override
    protected boolean shouldAddIconPage() {
        return mValues.getIconState() != null;
    }

    /**
     * Hide those parameters that the template requires but that we don't want
     * to ask the users about, since we can derive it from the target project
     * the template is written into.
     */
    protected void hideBuiltinParameters() {
        Set<String> hidden = mValues.hidden;
        hidden.add(ATTR_PACKAGE_NAME);
        hidden.add(ATTR_MIN_API);
        hidden.add(ATTR_MIN_API_LEVEL);
        hidden.add(ATTR_TARGET_API);
        hidden.add(ATTR_BUILD_API);
    }

    @Override
    public void addPages() {
        super.addPages();
        addPage(mMainPage);
    }

    @Override
    public IWizardPage getNextPage(IWizardPage page) {
        TemplateMetadata template = mValues.getTemplateHandler().getTemplate();

        if (page == mMainPage && shouldAddIconPage()) {
            WizardPage iconPage = getIconPage(mValues.getIconState());
            mValues.updateIconState(mMainPage.getEvaluator());
            return iconPage;
        } else if (page == mMainPage
                || shouldAddIconPage() && page == getIconPage(mValues.getIconState())) {
            if (template != null) {
                if (InstallDependencyPage.isInstalled(template.getDependencies())) {
                    return getPreviewPage(mValues);
                } else {
                    return getDependencyPage(template, true);
                }
            }
        } else if (page == getDependencyPage(template, false)) {
            return getPreviewPage(mValues);
        }

        return super.getNextPage(page);
    }

    @Override
    @NonNull
    protected IProject getProject() {
        return mValues.project;
    }

    @Override
    @NonNull
    protected List<String> getFilesToOpen() {
        TemplateHandler activityTemplate = mValues.getTemplateHandler();
        return activityTemplate.getFilesToOpen();
    }

    @Override
    @NonNull
    protected List<Runnable> getFinalizingActions() {
        TemplateHandler activityTemplate = mValues.getTemplateHandler();
        return activityTemplate.getFinalizingActions();
    }

    @Override
    protected List<Change> computeChanges() {
        return mValues.computeChanges();
    }

    /**
     * Opens the given set of files (as relative paths within a given project
     *
     * @param project the project containing the paths
     * @param relativePaths the paths to files to open
     * @param mWorkbench the workbench to open the files in
     */
    public static void openFiles(
            @NonNull final IProject project,
            @NonNull final List<String> relativePaths,
            @NonNull final IWorkbench mWorkbench) {
        if (!relativePaths.isEmpty()) {
            // This has to be delayed in order for focus handling to work correctly
            AdtPlugin.getDisplay().asyncExec(new Runnable() {
                @Override
                public void run() {
                    for (String path : relativePaths) {
                        IResource resource = project.findMember(path);
                        if (resource != null) {
                            if (resource instanceof IFile) {
                                try {
                                    AdtPlugin.openFile((IFile) resource, null, false);
                                } catch (PartInitException e) {
                                    AdtPlugin.log(e, "Failed to open %1$s", //$NON-NLS-1$
                                            resource.getFullPath().toString());
                                }
                            }
                            boolean isLast = relativePaths.size() == 1 ||
                                    path.equals(relativePaths.get(relativePaths.size() - 1));
                            if (isLast) {
                                BasicNewResourceWizard.selectAndReveal(resource,
                                        mWorkbench.getActiveWorkbenchWindow());
                            }
                        }
                    }
                }
            });
        }
    }

    /**
     * Specific New Custom View wizard
     */
    public static class NewCustomViewWizard extends NewTemplateWizard {
        /** Creates a new {@link NewCustomViewWizard} */
        public NewCustomViewWizard() {
            super(CUSTOM_VIEW);
        }

        @Override
        public void init(IWorkbench workbench, IStructuredSelection selection) {
            super.init(workbench, selection);
            setWindowTitle("New Custom View");
            super.mMainPage.setTitle("New Custom View");
            super.mMainPage.setDescription("Creates a new custom view");
        }
    }
}

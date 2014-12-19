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

package org.eclipse.andmore.internal.lint;

import static com.android.SdkConstants.DOT_XML;

import com.android.tools.lint.detector.api.LintUtils;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.AdtUtils;
import org.eclipse.andmore.internal.editors.IconFactory;
import org.eclipse.andmore.internal.preferences.AdtPrefs;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowPulldownDelegate;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

import java.util.ArrayList;
import java.util.List;

/**
 * Action which runs Lint on the currently projects (and also provides a
 * pulldown menu in the toolbar for selecting specifically which projects to
 * check)
 */
public class RunLintAction implements IObjectActionDelegate, IMenuCreator,
        IWorkbenchWindowPulldownDelegate {

    private ISelection mSelection;
    private Menu mMenu;

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        mSelection = selection;
    }

    @Override
    public void run(IAction action) {
        List<IProject> projects = getProjects(mSelection, true /* warn */);

        if (!projects.isEmpty()) {
            EclipseLintRunner.startLint(projects, null, null, false /*fatalOnly*/, true /*show*/);
        }
    }

    /** Returns the Android project(s) to apply a lint run to. */
    static List<IProject> getProjects(ISelection selection, boolean warn) {
        List<IProject> projects = AdtUtils.getSelectedProjects(selection);

        if (projects.isEmpty() && warn) {
            MessageDialog.openWarning(AdtPlugin.getShell(), "Lint",
                    "Could not run Lint: Select an Android project first.");
        }

        return projects;
    }

    @Override
    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
    }

    @Override
    public void dispose() {
        if (mMenu != null) {
            mMenu.dispose();
        }
    }

    @Override
    public void init(IWorkbenchWindow window) {
    }

    // ---- IMenuCreator ----

    @Override
    public Menu getMenu(Control parent) {
        mMenu = new Menu(parent);

        IconFactory iconFactory = IconFactory.getInstance();
        ImageDescriptor allIcon = iconFactory.getImageDescriptor("lintrun"); //$NON-NLS-1$
        LintMenuAction allAction = new LintMenuAction("Check All Projects", allIcon,
                ACTION_RUN, null);

        addAction(allAction);
        addSeparator();
        IJavaProject[] projects = AdtUtils.getOpenAndroidProjects();
        ILabelProvider provider = new JavaElementLabelProvider(
                JavaElementLabelProvider.SHOW_DEFAULT);
        for (IJavaProject project : projects) {
            IProject p = project.getProject();
            ImageDescriptor icon = ImageDescriptor.createFromImage(provider.getImage(p));
            String label = String.format("Check %1$s", p.getName());
            LintMenuAction projectAction = new LintMenuAction(label, icon, ACTION_RUN, p);
            addAction(projectAction);
        }

        ITextEditor textEditor = AdtUtils.getActiveTextEditor();
        if (textEditor != null) {
            IFile file = AdtUtils.getActiveFile();
            // Currently only supported for XML files
            if (file != null && LintUtils.endsWith(file.getName(), DOT_XML)) {
                ImageDescriptor icon = ImageDescriptor.createFromImage(provider.getImage(file));
                IAction fileAction = new LintMenuAction("Check Current File", icon, ACTION_RUN,
                        file);

                addSeparator();
                addAction(fileAction);
            }
        }

        ISharedImages images = PlatformUI.getWorkbench().getSharedImages();
        ImageDescriptor clear = images.getImageDescriptor(ISharedImages.IMG_ELCL_REMOVEALL);
        LintMenuAction clearAction = new LintMenuAction("Clear Lint Warnings", clear, ACTION_CLEAR,
                null);
        addSeparator();
        addAction(clearAction);

        LintMenuAction excludeAction = new LintMenuAction("Skip Library Project Dependencies",
                allIcon, ACTION_TOGGLE_EXCLUDE, null);
        addSeparator();
        addAction(excludeAction);
        excludeAction.setChecked(AdtPrefs.getPrefs().getSkipLibrariesFromLint());

        return mMenu;
    }

    private void addAction(IAction action) {
        ActionContributionItem item = new ActionContributionItem(action);
        item.fill(mMenu, -1);
    }

    private void addSeparator() {
        new Separator().fill(mMenu, -1);
    }

    @Override
    public Menu getMenu(Menu parent) {
        return null;
    }

    private static final int ACTION_RUN = 1;
    private static final int ACTION_CLEAR = 2;
    private static final int ACTION_TOGGLE_EXCLUDE = 3;

    /**
     * Actions in the pulldown context menu: run lint or clear lint markers on
     * the given resource
     */
    private static class LintMenuAction extends Action {
        private final IResource mResource;
        private final int mAction;

        /**
         * Creates a new context menu action
         *
         * @param text the label
         * @param descriptor the icon
         * @param action the action to run: run lint, clear, or toggle exclude libraries
         * @param resource the resource to check or clear markers for, where
         *            null means all projects
         */
        private LintMenuAction(String text, ImageDescriptor descriptor, int action,
                IResource resource) {
            super(text, action == ACTION_TOGGLE_EXCLUDE ? AS_CHECK_BOX : AS_PUSH_BUTTON);
            if (descriptor != null) {
                setImageDescriptor(descriptor);
            }
            mAction = action;
            mResource = resource;
        }

        @Override
        public void run() {
            if (mAction == ACTION_TOGGLE_EXCLUDE) {
                AdtPrefs prefs = AdtPrefs.getPrefs();
                prefs.setSkipLibrariesFromLint(!prefs.getSkipLibrariesFromLint());
                return;
            }
            List<IResource> resources = new ArrayList<IResource>();
            if (mResource == null) {
                // All projects
                IJavaProject[] open = AdtUtils.getOpenAndroidProjects();
                for (IJavaProject project : open) {
                    resources.add(project.getProject());
                }
            } else {
                resources.add(mResource);
            }
            EclipseLintRunner.cancelCurrentJobs(false);
            if (mAction == ACTION_CLEAR) {
                EclipseLintClient.clearMarkers(resources);
            } else {
                assert mAction == ACTION_RUN;
                EclipseLintRunner.startLint(resources, null, null, false /*fatalOnly*/,
                        true /*show*/);
            }
        }
    }
}

/*
 * Copyright (C) 2007 The Android Open Source Project
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

package org.eclipse.andmore.internal.wizards.actions;

import org.eclipse.andmore.internal.lint.EclipseLintRunner;
import org.eclipse.andmore.internal.project.ExportHelper;
import org.eclipse.andmore.internal.sdk.ProjectState;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

public class ExportAction implements IObjectActionDelegate {

    private ISelection mSelection;
    private Shell mShell;

    /**
     * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
     */
    @Override
    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
        mShell = targetPart.getSite().getShell();
    }

    @Override
    public void run(IAction action) {
        if (mSelection instanceof IStructuredSelection) {
            IStructuredSelection selection = (IStructuredSelection)mSelection;
            // get the unique selected item.
            if (selection.size() == 1) {
                Object element = selection.getFirstElement();

                // get the project object from it.
                IProject project = null;
                if (element instanceof IProject) {
                    project = (IProject) element;
                } else if (element instanceof IAdaptable) {
                    project = (IProject) ((IAdaptable) element).getAdapter(IProject.class);
                }

                // and finally do the action
                if (project != null) {
                    if (!EclipseLintRunner.runLintOnExport(mShell, project)) {
                        return;
                    }

                    ProjectState state = Sdk.getProjectState(project);
                    if (state.isLibrary()) {
                        MessageDialog.openError(mShell, "Android Export",
                                "Android library projects cannot be exported.");
                    } else {
                        ExportHelper.exportUnsignedReleaseApk(project);
                    }
                }
            }
        }
    }

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        mSelection = selection;
    }
}

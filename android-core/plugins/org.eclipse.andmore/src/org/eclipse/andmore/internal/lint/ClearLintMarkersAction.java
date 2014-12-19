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

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IActionDelegate;

import java.util.List;

/** Action which clear lint markers from the current project */
public class ClearLintMarkersAction implements IActionDelegate {

    private ISelection mSelection;

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        mSelection = selection;
    }

    @Override
    public void run(IAction action) {
        List<IProject> projects = RunLintAction.getProjects(mSelection, false /*warn*/);
        if (projects != null) {
            EclipseLintRunner.cancelCurrentJobs(false);
            EclipseLintClient.clearMarkers(projects);
        }
    }
}

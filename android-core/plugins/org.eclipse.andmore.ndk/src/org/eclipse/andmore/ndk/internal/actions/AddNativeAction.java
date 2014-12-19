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

package org.eclipse.andmore.ndk.internal.actions;

import org.eclipse.andmore.ndk.internal.wizards.AddNativeWizard;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

public class AddNativeAction implements IObjectActionDelegate {

	private IWorkbenchPart mPart;
	private ISelection mSelection;

	@Override
	public void run(IAction action) {
		IProject project = null;
		if (mSelection instanceof IStructuredSelection) {
			IStructuredSelection ss = (IStructuredSelection) mSelection;
			if (ss.size() == 1) {
				Object obj = ss.getFirstElement();
				if (obj instanceof IProject) {
					project = (IProject) obj;
				} else if (obj instanceof PlatformObject) {
					project = (IProject) ((PlatformObject) obj).getAdapter(IProject.class);
				}
			}
		}

		if (project != null) {
			AddNativeWizard wizard = new AddNativeWizard(project, mPart.getSite().getWorkbenchWindow());
			WizardDialog dialog = new WizardDialog(mPart.getSite().getShell(), wizard);
			dialog.open();
		}

	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		mSelection = selection;
	}

	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		mPart = targetPart;
	}

}

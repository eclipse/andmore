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

package org.eclipse.andmore.ndk.internal.wizards;

import org.eclipse.andmore.ndk.internal.Activator;
import org.eclipse.andmore.ndk.internal.NdkManager;
import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.make.core.MakeCorePlugin;
import org.eclipse.cdt.ui.CUIPlugin;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.WorkbenchException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class AddNativeWizard extends Wizard {

	private final IProject mProject;
	private final IWorkbenchWindow mWindow;

	private AddNativeWizardPage mAddNativeWizardPage;
	private Map<String, String> mTemplateArgs = new HashMap<String, String>();

	public AddNativeWizard(IProject project, IWorkbenchWindow window) {
		mProject = project;
		mWindow = window;
		mTemplateArgs.put(NdkManager.LIBRARY_NAME, project.getName());
	}

	@Override
	public void addPages() {
		mAddNativeWizardPage = new AddNativeWizardPage(mTemplateArgs);
		addPage(mAddNativeWizardPage);
	}

	@Override
	public boolean performFinish() {
		// Switch to C/C++ Perspective
		try {
			mWindow.getWorkbench().showPerspective(CUIPlugin.ID_CPERSPECTIVE, mWindow);
		} catch (WorkbenchException e1) {
			Activator.log(e1);
		}

		mAddNativeWizardPage.updateArgs(mTemplateArgs);

		IRunnableWithProgress op = new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				IWorkspaceRunnable op1 = new IWorkspaceRunnable() {
					@Override
					public void run(IProgressMonitor monitor1) throws CoreException {
						// Convert to CDT project
						CCorePlugin.getDefault().convertProjectToCC(mProject, monitor1, MakeCorePlugin.MAKE_PROJECT_ID);
						// Set up build information
						new NdkWizardHandler().convertProject(mProject, monitor1);

						// When using CDT 8.1.x, disable the language settings
						// provider mechanism
						// for scanner discovery. Use the classloader to load
						// the class since it
						// will not be available pre 8.1.
						try {
							@SuppressWarnings("rawtypes")
							Class c = getClass().getClassLoader().loadClass(
									"org.eclipse.cdt.core.language.settings.providers.ScannerDiscoveryLegacySupport"); //$NON-NLS-1$

							@SuppressWarnings("unchecked")
							Method m = c.getMethod("setLanguageSettingsProvidersFunctionalityEnabled", //$NON-NLS-1$
									IProject.class, boolean.class);

							m.invoke(null, mProject, false);
						} catch (Exception e) {
							// ignore all exceptions: On pre 8.1.x CDT, this
							// class will not be
							// found, but this options only needs to be set in
							// 8.1.x
						}

						// Run the template
						NdkManager.addNativeSupport(mProject, mTemplateArgs, monitor1);
					}
				};
				// TODO run from a job
				IWorkspace workspace = ResourcesPlugin.getWorkspace();
				try {
					workspace.run(op1, workspace.getRoot(), 0, new NullProgressMonitor());
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				}
			}
		};
		try {
			getContainer().run(false, true, op);
			return true;
		} catch (InterruptedException e) {
			Activator.log(e);
			return false;
		} catch (InvocationTargetException e) {
			Activator.log(e);
			return false;
		}
	}

}

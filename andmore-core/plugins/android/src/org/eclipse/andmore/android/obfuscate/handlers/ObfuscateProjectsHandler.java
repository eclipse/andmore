/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eclipse.andmore.android.obfuscate.handlers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.andmore.android.model.AndroidProject;
import org.eclipse.andmore.android.obfuscate.ObfuscatorManager;
import org.eclipse.andmore.android.obfuscate.ui.ObfuscateDialog;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.PlatformUI;

/**
 * 
 * This class is responsible for handling the command that marks/unmarks Android
 * project to be obfuscated.
 * 
 * Its proposal at first is to be called from the motodev menu
 */
public class ObfuscateProjectsHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				ObfuscateDialog dialog = new ObfuscateDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow()
						.getShell());
				if (dialog.open() == Window.OK) {
					List<IProject> obfuscatedProjects = new ArrayList<IProject>();
					ArrayList<IProject> selectedProjects = dialog.getSelectedProjects();

					List<IProject> toDesobfuscate = new ArrayList<IProject>();
					IProject[] allProjects = ResourcesPlugin.getWorkspace().getRoot().getProjects();

					for (int i = 0; i < allProjects.length; i++) {
						IProject projectN = allProjects[i];

						try {
							// it is an android project and is opened
							if ((projectN.getNature(AndroidProject.ANDROID_NATURE) != null) && projectN.isOpen()) {
								// it is obfuscated
								if (ObfuscatorManager.isProguardSet(projectN)) {
									obfuscatedProjects.add(projectN);
									// it was not selected -> desobfuscate it
									if (!selectedProjects.contains(projectN)) {
										toDesobfuscate.add(projectN);
									}
								}
							}
						} catch (CoreException e) {
							// do nothing
						}
					}

					// It only makes sense to perform some action if there is a
					// project to obfuscate/desobfuscate
					if (!(obfuscatedProjects.containsAll(selectedProjects) && toDesobfuscate.isEmpty())) {
						toggleObfuscateMode(selectedProjects, toDesobfuscate);
					}
				}
			}
		});

		return null;
	}

	/**
	 * Receives a list of project and toogles obfscation mode of these projects:
	 * if it is set to be obfuscated, than unset. (and vice-versa)
	 * 
	 * @param selection
	 */
	private void toggleObfuscateMode(List<IProject> _obfuscatedProjects, List<IProject> _notObfuscatedProjects) {

		for (Iterator<IProject> iterator = _notObfuscatedProjects.iterator(); iterator.hasNext();) {
			IProject iProject = iterator.next();
			ObfuscatorManager.unobfuscate(iProject);
		}

		for (Iterator<IProject> iterator = _obfuscatedProjects.iterator(); iterator.hasNext();) {
			IProject iProject = iterator.next();
			ObfuscatorManager.obfuscate(iProject, new NullProgressMonitor());
		}

	}
}

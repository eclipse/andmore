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

package org.eclipse.andmore.internal.actions;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.andmore.AndmoreAndroidConstants;
import org.eclipse.andmore.internal.project.AndroidNature;
import org.eclipse.andmore.internal.project.ProjectHelper;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.android.annotations.NonNull;

/**
 * Action to fix the project properties:
 * <ul>
 * <li>Make sure the framework archive is present with the link to the java
 * doc</li>
 * </ul>
 */
public class FixProjectAction implements IObjectActionDelegate {

    private ISelection mSelection;

    /**
     * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
     */
    @Override
    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
    }

    @Override
    public void run(IAction action) {
        if (mSelection instanceof IStructuredSelection) {

            for (Iterator<?> it = ((IStructuredSelection) mSelection).iterator();
                    it.hasNext();) {
                Object element = it.next();
                IProject project = null;
                if (element instanceof IProject) {
                    project = (IProject) element;
                } else if (element instanceof IAdaptable) {
                    project = (IProject) ((IAdaptable) element)
                            .getAdapter(IProject.class);
                }
                if (project != null) {
                    fixProject(project);
                }
            }
        }
    }

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        mSelection = selection;
    }

    private void fixProject(final IProject project) {
        createFixProjectJob(project).schedule();
    }

    /**
     * Creates a job to fix the project
     *
     * @param project the project to fix
     * @return a job to perform the fix (not yet scheduled)
     */
    @NonNull
    public static Job createFixProjectJob(@NonNull final IProject project) {
        return new Job("Fix Project Properties") {

            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    if (monitor != null) {
                        monitor.beginTask("Fix Project Properties", 6);
                    }
//                    fixAdtEntries(project, monitor);

                    ProjectHelper.fixProject(project);
                    if (monitor != null) {
                        monitor.worked(1);
                    }

                    // fix the nature order to have the proper project icon
                    ProjectHelper.fixProjectNatureOrder(project);
                    if (monitor != null) {
                        monitor.worked(1);
                    }

                    // now we fix the builders
                    AndroidNature.configureResourceManagerBuilder(project);
                    if (monitor != null) {
                        monitor.worked(1);
                    }

                    AndroidNature.configurePreBuilder(project);
                    if (monitor != null) {
                        monitor.worked(1);
                    }

                    AndroidNature.configureApkBuilder(project);
                    if (monitor != null) {
                        monitor.worked(1);
                    }

                    return Status.OK_STATUS;
                } catch (JavaModelException e) {
                    return e.getJavaModelStatus();
                } catch (CoreException e) {
                    return e.getStatus();
                } finally {
                    if (monitor != null) {
                        monitor.done();
                    }
                }
            }
        };
    }

    /**
     * @see IWorkbenchWindowActionDelegate#init
     */
    public void init(IWorkbenchWindow window) {
        // pass
    }

	private static void fixAdtEntries(IProject libraryProject, IProgressMonitor monitor) {

		// Update the NatureIds
		try {
			IProjectDescription desc = libraryProject.getDescription();
			String[] natureIds = desc.getNatureIds();
			for (int i = 0; i < natureIds.length; i++) {
				natureIds[i] = natureIds[i].replace("com.android.ide.eclipse.adt", "org.eclipse.andmore");
			}
			desc.setNatureIds(natureIds);
			ICommand[] commands = desc.getBuildSpec();
			for (int i = 0; i < commands.length; i++) {
				commands[i].setBuilderName(
						commands[i].getBuilderName().replace("com.android.ide.eclipse.adt", "org.eclipse.andmore"));
			}
			desc.setBuildSpec(commands);
			libraryProject.setDescription(desc, monitor);
			updateClasspathEntries(JavaCore.create(libraryProject));
		} catch (CoreException e) {
		}
	}

	@SuppressWarnings("restriction")
	private static void updateClasspathEntries(IJavaProject androidProject) throws JavaModelException {
		ArrayList<IClasspathEntry> newclasspathEntries = new ArrayList<IClasspathEntry>();

		IClasspathEntry[] classpathEntries = androidProject.getRawClasspath();
		for (IClasspathEntry classpathEntry : classpathEntries) {
			if (classpathEntry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
				String classpathId = classpathEntry.getPath().toString();
				if (classpathId.equals(AndmoreAndroidConstants.ADT_CONTAINER_DEPENDENCIES)) {
					newclasspathEntries.add(createNewAndmoreContainer(AndmoreAndroidConstants.CONTAINER_DEPENDENCIES));
				} else if (classpathId.equals(AndmoreAndroidConstants.ADT_CONTAINER_FRAMEWORK)) {
					newclasspathEntries.add(createNewAndmoreContainer(AndmoreAndroidConstants.CONTAINER_FRAMEWORK));
				} else if (classpathId.equals(AndmoreAndroidConstants.ADT_CONTAINER_PRIVATE_LIBRARIES)) {
					newclasspathEntries
							.add(createNewAndmoreContainer(AndmoreAndroidConstants.CONTAINER_PRIVATE_LIBRARIES));
				} else {
					newclasspathEntries.add(classpathEntry);
				}

			} else {
				newclasspathEntries.add(classpathEntry);
			}
		}

		IClasspathEntry[] andmoreClasspathEntries = new IClasspathEntry[newclasspathEntries.size()];
		newclasspathEntries.toArray(andmoreClasspathEntries);
		androidProject.setRawClasspath(andmoreClasspathEntries, true, new NullProgressMonitor());
	}

	private static IClasspathEntry createNewAndmoreContainer(String id) {
		IClasspathEntry andmoreClasspathEntry = JavaCore.newContainerEntry(new Path(id));
		return andmoreClasspathEntry;
	}

}

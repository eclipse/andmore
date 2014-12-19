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
package org.eclipse.andmore.android;

import org.eclipse.andmore.internal.project.AndroidNature;
import org.eclipse.andmore.internal.project.ProjectHelper;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaModelException;

/**
 * Class that contains useful methods to handle with Android Projects
 */
public class ProjectUtils {
	/**
	 * Fixes an Android project (a project with errors)
	 * 
	 * @param project
	 *            the project to be fixed
	 * 
	 * @throws JavaModelException
	 */
	public static void fixProject(IProject project) throws JavaModelException {
		ProjectHelper.fixProject(project);
	}

	/**
	 * Sets the Android Project natures to a project
	 * 
	 * @param project
	 *            The project
	 * @param monitor
	 *            The progress monitor
	 * 
	 * @throws CoreException
	 */
	public static void setupAndroidNatures(IProject project, IProgressMonitor monitor) throws CoreException {
		// Add the Java and android nature to the project
		AndroidNature.setupProjectNatures(project, monitor, true);
	}

	/**
	 * Adds a new entry to a set of classpath entries
	 * 
	 * @param entries
	 *            the classpath entries
	 * @param newSourceEntry
	 *            the new entry to add
	 * 
	 * @return the new set of classpath
	 */
	public static IClasspathEntry[] addEntryToClasspath(IClasspathEntry[] entries, IClasspathEntry newSourceEntry) {
		return ProjectHelper.addEntryToClasspath(entries, newSourceEntry);
	}

}

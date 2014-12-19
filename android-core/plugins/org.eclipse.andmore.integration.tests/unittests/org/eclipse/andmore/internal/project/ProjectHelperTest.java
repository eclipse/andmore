/*
 * Copyright (C) 2008 The Android Open Source Project
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

package org.eclipse.andmore.internal.project;

import org.eclipse.andmore.mock.Mocks;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;

import junit.framework.TestCase;

public class ProjectHelperTest extends TestCase {

    /** The old container id */
    private final static String OLD_CONTAINER_ID =
        "org.eclipse.andmore.project.AndroidClasspathContainerInitializer"; //$NON-NLS-1$

    /** The container id for the android framework jar file */
    private final static String CONTAINER_ID =
        "org.eclipse.andmore.ANDROID_FRAMEWORK"; //$NON-NLS-1$

    @Override
    public void setUp() throws Exception {
        // pass for now
    }

    @Override
    public void tearDown() throws Exception {
        // pass for now
    }

    public final void testFixProjectClasspathEntriesFromOldContainer() throws Exception {
        // create a project with a path to an android .zip
        IJavaProject javaProject = Mocks.createProject(
                new IClasspathEntry[] {
                        Mocks.createClasspathEntry(new Path("Project/src"), //$NON-NLS-1$
                                IClasspathEntry.CPE_SOURCE),
                        Mocks.createClasspathEntry(new Path(OLD_CONTAINER_ID),
                                IClasspathEntry.CPE_CONTAINER),
                },
                new Path("Project/bin"));

        ProjectHelper.fixProjectClasspathEntries(javaProject);

        IClasspathEntry[] fixedEntries = javaProject.getRawClasspath();
        assertEquals(3, fixedEntries.length);
        assertEquals("Project/src", fixedEntries[0].getPath().toString());
        assertEquals(OLD_CONTAINER_ID, fixedEntries[1].getPath().toString());
        assertEquals(CONTAINER_ID, fixedEntries[2].getPath().toString());
    }
}

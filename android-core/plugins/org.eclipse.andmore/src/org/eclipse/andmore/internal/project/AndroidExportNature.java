/*
 * Copyright (C) 2010 The Android Open Source Project
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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;

/**
 * Project nature for the Android Export Projects.
 */
public class AndroidExportNature implements IProjectNature {

    /** the project this nature object is associated with */
    private IProject mProject;

    /**
     * Configures this nature for its project. This is called by the workspace
     * when natures are added to the project using
     * <code>IProject.setDescription</code> and should not be called directly
     * by clients. The nature extension id is added to the list of natures
     * before this method is called, and need not be added here.
     *
     * Exceptions thrown by this method will be propagated back to the caller of
     * <code>IProject.setDescription</code>, but the nature will remain in
     * the project description.
     *
     * @see org.eclipse.core.resources.IProjectNature#configure()
     * @throws CoreException if configuration fails.
     */
    @Override
    public void configure() throws CoreException {
        // nothing to do.
    }

    /**
     * De-configures this nature for its project. This is called by the
     * workspace when natures are removed from the project using
     * <code>IProject.setDescription</code> and should not be called directly
     * by clients. The nature extension id is removed from the list of natures
     * before this method is called, and need not be removed here.
     *
     * Exceptions thrown by this method will be propagated back to the caller of
     * <code>IProject.setDescription</code>, but the nature will still be
     * removed from the project description.
     *
     * The Android nature removes the custom pre builder and APK builder.
     *
     * @see org.eclipse.core.resources.IProjectNature#deconfigure()
     * @throws CoreException if configuration fails.
     */
    @Override
    public void deconfigure() throws CoreException {
        // nothing to do
    }

    /**
     * Returns the project to which this project nature applies.
     *
     * @return the project handle
     * @see org.eclipse.core.resources.IProjectNature#getProject()
     */
    @Override
    public IProject getProject() {
        return mProject;
    }

    /**
     * Sets the project to which this nature applies. Used when instantiating
     * this project nature runtime. This is called by
     * <code>IProject.create()</code> or
     * <code>IProject.setDescription()</code> and should not be called
     * directly by clients.
     *
     * @param project the project to which this nature applies
     * @see org.eclipse.core.resources.IProjectNature#setProject(org.eclipse.core.resources.IProject)
     */
    @Override
    public void setProject(IProject project) {
        mProject = project;
    }
}

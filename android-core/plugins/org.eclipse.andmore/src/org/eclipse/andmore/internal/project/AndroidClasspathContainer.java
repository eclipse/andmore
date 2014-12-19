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

package org.eclipse.andmore.internal.project;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;

/**
 * Classpath container for the Android projects.
 * This supports both the System classpath and the library dependencies.
 */
class AndroidClasspathContainer implements IClasspathContainer {

    private final IClasspathEntry[] mClasspathEntry;
    private final IPath mContainerPath;
    private final String mName;
    private final int mKind;

    /**
     * Constructs the container with the {@link IClasspathEntry} representing the android
     * framework jar file and the container id
     * @param entries the entries representing the android framework and optional libraries.
     * @param path the path containing the classpath container id.
     * @param name the name of the container to display.
     * @param the container kind. Can be {@link IClasspathContainer#K_DEFAULT_SYSTEM} or
     *      {@link IClasspathContainer#K_APPLICATION}
     */
    AndroidClasspathContainer(IClasspathEntry[] entries, IPath path, String name, int kind) {
        mClasspathEntry = entries;
        mContainerPath = path;
        mName = name;
        mKind = kind;
    }

    @Override
    public IClasspathEntry[] getClasspathEntries() {
        return mClasspathEntry;
    }

    @Override
    public String getDescription() {
        return mName;
    }

    @Override
    public int getKind() {
        return mKind;
    }

    @Override
    public IPath getPath() {
        return mContainerPath;
    }
}

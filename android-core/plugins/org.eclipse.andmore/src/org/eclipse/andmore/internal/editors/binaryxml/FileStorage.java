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

package org.eclipse.andmore.internal.editors.binaryxml;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Implementation of storage for a local file
 * (<code>java.io.File</code>).
 *
 * @see org.eclipse.core.resources.IStorage
 */

public class FileStorage implements IStorage {

    /**
     * The file this storage refers to.
     */
    private File mFile = null;

    /**
     * Constructs and returns storage for the given file.
     *
     * @param file a local file
     */
    public FileStorage(File file) {
        mFile = file;
    }

    /* (non-Javadoc)
     * @see org.eclipse.core.resources.IStorage#getContents()
     */
    @Override
    public InputStream getContents() throws CoreException {
        InputStream stream = null;
        try {
            stream = new FileInputStream(mFile);
        } catch (Exception e) {
            throw new CoreException(new Status(IStatus.ERROR, AdtPlugin.getDefault().getBundle()
                    .getSymbolicName(), IStatus.ERROR, mFile.getAbsolutePath(), e));
        }
        return stream;
    }

    /* (non-Javadoc)
     * @see org.eclipse.core.resources.IStorage#getFullPath()
     */
    @Override
    public IPath getFullPath() {
        return new Path(mFile.getAbsolutePath());
    }

    /* (non-Javadoc)
     * @see org.eclipse.core.resources.IStorage#getName()
     */
    @Override
    public String getName() {
        return mFile.getName();
    }

    /* (non-Javadoc)
     * @see org.eclipse.core.resources.IStorage#isReadOnly()
     */
    @Override
    public boolean isReadOnly() {
        return true;
    }

    /* (non-Javadoc)
     * @see org.eclipse.core.runtime.IAdaptable#getAdapter(Class)
     */
    @Override
    public Object getAdapter(Class adapter) {
        return null;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FileStorage) {
            return mFile.equals(((FileStorage) obj).mFile);
        }
        return super.equals(obj);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return mFile.hashCode();
    }
}

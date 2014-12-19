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

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IStorageEditorInput;

/**
 * An editor input for a local file.
 */
public class XmlStorageEditorInput implements IStorageEditorInput {

    /**
     * Storage associated with this editor input
     */
    IStorage mStorage = null;

    /**
     * Constructs an editor input on the given storage
     *
     * @param storage
     */
    public XmlStorageEditorInput(IStorage storage) {
        mStorage = storage;
    }

    /* (non-Javadoc)
     * @see IStorageEditorInput#getStorage()
     */
    @Override
    public IStorage getStorage() throws CoreException {
        return mStorage;
    }

    /* (non-Javadoc)
     * @see IInput#getStorage()
     */
    @Override
    public boolean exists() {
        return mStorage != null;
    }

    /* (non-Javadoc)
     * @see IEditorInput#getImageDescriptor()
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return null;
    }

    /* (non-Javadoc)
     * @see IEditorInput#getName()
     */
    @Override
    public String getName() {
        return mStorage.getName();
    }

    /* (non-Javadoc)
     * @see IEditorInput#getPersistable()
     */
    @Override
    public IPersistableElement getPersistable() {
        return null;
    }

    /* (non-Javadoc)
     * @see IEditorInput#getToolTipText()
     */
    @Override
    public String getToolTipText() {
        return mStorage.getFullPath() != null ? mStorage.getFullPath().toString() : mStorage
                .getName();
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
        if (obj instanceof XmlStorageEditorInput) {
            return mStorage.equals(((XmlStorageEditorInput) obj).mStorage);
        }
        return super.equals(obj);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return mStorage.hashCode();
    }
}

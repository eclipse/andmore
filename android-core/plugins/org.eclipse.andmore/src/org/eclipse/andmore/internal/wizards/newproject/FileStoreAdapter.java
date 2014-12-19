/*
 * Copyright (C) 2012 The Android Open Source Project
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

package org.eclipse.andmore.internal.wizards.newproject;

import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.IFileSystem;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

/**
 * IFileStore implementation that delegates to the give {@link IFileStore}.
 * This makes it easier to just override a single method from a store.
 */
class FileStoreAdapter implements IFileStore {

    private final IFileStore mStore;

    public FileStoreAdapter(IFileStore store) {
        mStore = store;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Object getAdapter(Class adapter) {
        return mStore.getAdapter(adapter);
    }

    @Override
    public IFileInfo[] childInfos(int options, IProgressMonitor monitor) throws CoreException {
        return mStore.childInfos(options, monitor);
    }

    @Override
    public String[] childNames(int options, IProgressMonitor monitor)
            throws CoreException {
        return mStore.childNames(options, monitor);
    }

    @Override
    public IFileStore[] childStores(int options, IProgressMonitor monitor) throws CoreException {
        return mStore.childStores(options, monitor);
    }

    @Override
    public void copy(IFileStore destination, int options, IProgressMonitor monitor)
            throws CoreException {
        mStore.copy(destination, options, monitor);
    }

    @Override
    public void delete(int options, IProgressMonitor monitor) throws CoreException {
        mStore.delete(options, monitor);
    }

    @Override
    public IFileInfo fetchInfo() {
        return mStore.fetchInfo();
    }

    @Override
    public IFileInfo fetchInfo(int options, IProgressMonitor monitor) throws CoreException {
        return mStore.fetchInfo(options, monitor);
    }

    @Deprecated
    @Override
    public IFileStore getChild(IPath path) {
        return mStore.getChild(path);
    }

    @Override
    public IFileStore getFileStore(IPath path) {
        return mStore.getFileStore(path);
    }

    @Override
    public IFileStore getChild(String name) {
        return mStore.getChild(name);
    }

    @Override
    public IFileSystem getFileSystem() {
        return mStore.getFileSystem();
    }

    @Override
    public String getName() {
        return mStore.getName();
    }

    @Override
    public IFileStore getParent() {
        return mStore.getParent();
    }

    @Override
    public boolean isParentOf(IFileStore other) {
        return mStore.isParentOf(other);
    }

    @Override
    public IFileStore mkdir(int options, IProgressMonitor monitor) throws CoreException {
        return mStore.mkdir(options, monitor);
    }

    @Override
    public void move(IFileStore destination, int options, IProgressMonitor monitor)
            throws CoreException {
        mStore.move(destination, options, monitor);
    }

    @Override
    public InputStream openInputStream(int options, IProgressMonitor monitor)
            throws CoreException {
        return mStore.openInputStream(options, monitor);
    }

    @Override
    public OutputStream openOutputStream(int options, IProgressMonitor monitor)
            throws CoreException {
        return mStore.openOutputStream(options, monitor);
    }

    @Override
    public void putInfo(IFileInfo info, int options, IProgressMonitor monitor)
            throws CoreException {
        mStore.putInfo(info, options, monitor);
    }

    @Override
    public File toLocalFile(int options, IProgressMonitor monitor) throws CoreException {
        return mStore.toLocalFile(options, monitor);
    }

    @Override
    public URI toURI() {
        return mStore.toURI();
    }
}

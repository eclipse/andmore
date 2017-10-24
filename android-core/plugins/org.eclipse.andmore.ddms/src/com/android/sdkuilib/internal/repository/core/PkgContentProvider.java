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

package com.android.sdkuilib.internal.repository.core;

import com.android.sdklib.internal.repository.archives.Archive;
import com.android.sdklib.internal.repository.packages.Package;
import com.android.sdklib.internal.repository.sources.SdkSource;
import com.android.sdklib.internal.repository.updater.PkgItem;
import com.android.sdklib.repository.IDescription;
import com.android.sdkuilib.internal.repository.ui.PackagesPage;

import org.eclipse.jface.viewers.IInputProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import java.util.ArrayList;
import java.util.List;

/**
 * Content provider for the main tree view in {@link PackagesPage}.
 */
public class PkgContentProvider implements ITreeContentProvider {

    private final IInputProvider mViewer;
    private boolean mDisplayArchives;

    public PkgContentProvider(IInputProvider viewer) {
        mViewer = viewer;
    }

    public void setDisplayArchives(boolean displayArchives) {
        mDisplayArchives = displayArchives;
    }

    @Override
    public Object[] getChildren(Object parentElement) {
        if (parentElement instanceof ArrayList<?>) {
            return ((ArrayList<?>) parentElement).toArray();

        } else if (parentElement instanceof PkgCategorySource) {
            return getSourceChildren((PkgCategorySource) parentElement);

        } else if (parentElement instanceof PkgCategory) {
            return ((PkgCategory) parentElement).getItems().toArray();

        } else if (parentElement instanceof PkgItem) {
            if (mDisplayArchives) {

                Package pkg = ((PkgItem) parentElement).getUpdatePkg();

                // Display update packages as sub-items if the details mode is activated.
                if (pkg != null) {
                    return new Object[] { pkg };
                }

                return ((PkgItem) parentElement).getArchives();
            }

        } else if (parentElement instanceof Package) {
            if (mDisplayArchives) {
                return ((Package) parentElement).getArchives();
            }

        }

        return new Object[0];
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object getParent(Object element) {
        // This operation is expensive, so we do the minimum
        // and don't try to cover all cases.

        if (element instanceof PkgItem) {
            Object input = mViewer.getInput();
            if (input != null) {
                for (PkgCategory cat : (List<PkgCategory>) input) {
                    if (cat.getItems().contains(element)) {
                        return cat;
                    }
                }
            }
        }

        return null;
    }

    @Override
    public boolean hasChildren(Object parentElement) {
        if (parentElement instanceof ArrayList<?>) {
            return true;

        } else if (parentElement instanceof PkgCategory) {
            return true;

        } else if (parentElement instanceof PkgItem) {
            if (mDisplayArchives) {
                Package pkg = ((PkgItem) parentElement).getUpdatePkg();

                // Display update packages as sub-items if the details mode is activated.
                if (pkg != null) {
                    return true;
                }

                Archive[] archives = ((PkgItem) parentElement).getArchives();
                return archives.length > 0;
            }
        } else if (parentElement instanceof Package) {
            if (mDisplayArchives) {
                return ((Package) parentElement).getArchives().length > 0;
            }
        }

        return false;
    }

    @Override
    public Object[] getElements(Object inputElement) {
        return getChildren(inputElement);
    }

    @Override
    public void dispose() {
        // unused

    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        // unused
    }


    private Object[] getSourceChildren(PkgCategorySource parentElement) {
        List<?> children = parentElement.getItems();

        SdkSource source = parentElement.getSource();
        IDescription error = null;
        IDescription empty = null;

        String errStr = source.getFetchError();
        if (errStr != null) {
            error = new RepoSourceError(source);
        }
        if (!source.isEnabled() || children.isEmpty()) {
            empty = new RepoSourceNotification(source);
        }

        if (error != null || empty != null) {
            ArrayList<Object> children2 = new ArrayList<Object>();
            if (error != null) {
                children2.add(error);
            }
            if (empty != null) {
                children2.add(empty);
            }
            children2.addAll(children);
            children = children2;
        }

        return children.toArray();
    }


    /**
     * A dummy entry returned for sources which had load errors.
     * It displays a summary of the error as its short description or
     * it displays the source's long description.
     */
    public static class RepoSourceError implements IDescription {

        private final SdkSource mSource;

        public RepoSourceError(SdkSource source) {
            mSource = source;
        }

        @Override
        public String getLongDescription() {
            return mSource.getLongDescription();
        }

        @Override
        public String getShortDescription() {
            return mSource.getFetchError();
        }
    }

    /**
     * A dummy entry returned for sources with no packages.
     * We need that to force the SWT tree to display an open/close triangle
     * even for empty sources.
     */
    public static class RepoSourceNotification implements IDescription {

        private final SdkSource mSource;

        public RepoSourceNotification(SdkSource source) {
            mSource = source;
        }

        @Override
        public String getLongDescription() {
            if (mSource.isEnabled()) {
                return mSource.getLongDescription();
            } else {
                return "Loading from this site has been disabled. " +
                       "To enable it, use Tools > Manage Add-ons Sites.";
            }
        }

        @Override
        public String getShortDescription() {
            if (mSource.isEnabled()) {
                return "No packages found.";
            } else {
                return "This site is disabled. ";
            }
        }
    }

}

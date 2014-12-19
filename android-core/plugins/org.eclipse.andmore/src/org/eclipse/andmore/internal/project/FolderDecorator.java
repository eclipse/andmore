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

import com.android.SdkConstants;

import org.eclipse.andmore.AdtConstants;
import org.eclipse.andmore.AdtPlugin;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;

/**
 * A {@link ILabelDecorator} associated with an org.eclipse.ui.decorators extension.
 * This is used to add android icons in some special folders in the package explorer.
 */
public class FolderDecorator implements ILightweightLabelDecorator {

    private ImageDescriptor mDescriptor;

    public FolderDecorator() {
        mDescriptor = AdtPlugin.getImageDescriptor("/icons/android_project.png"); //$NON-NLS-1$
    }

    @Override
    public void decorate(Object element, IDecoration decoration) {
        if (element instanceof IFolder) {
            IFolder folder = (IFolder)element;

            // get the project and make sure this is an android project
            IProject project = folder.getProject();
            if (project == null || !project.exists() || !folder.exists()) {
                return;
            }

            try {
                if (project.hasNature(AdtConstants.NATURE_DEFAULT)) {
                    // check the folder is directly under the project.
                    if (folder.getParent().getType() == IResource.PROJECT) {
                        String name = folder.getName();
                        if (name.equals(SdkConstants.FD_ASSETS)) {
                            doDecoration(decoration, null);
                        } else if (name.equals(SdkConstants.FD_RESOURCES)) {
                            doDecoration(decoration, null);
                        } else if (name.equals(SdkConstants.FD_GEN_SOURCES)) {
                            doDecoration(decoration, " [Generated Java Files]");
                        } else if (name.equals(SdkConstants.FD_NATIVE_LIBS)) {
                            doDecoration(decoration, null);
                        } else if (name.equals(SdkConstants.FD_OUTPUT)) {
                            doDecoration(decoration, null);
                        }
                    }
                }
            } catch (CoreException e) {
                // log the error
                AdtPlugin.log(e, "Unable to get nature of project '%s'.", project.getName());
            }
        }
    }

    public void doDecoration(IDecoration decoration, String suffix) {
        decoration.addOverlay(mDescriptor, IDecoration.TOP_LEFT);

        if (suffix != null) {
            decoration.addSuffix(suffix);
        }
    }

    @Override
    public boolean isLabelProperty(Object element, String property) {
        // Property change do not affect the label
        return false;
    }

    @Override
    public void addListener(ILabelProviderListener listener) {
        // No state change will affect the rendering.
    }

    @Override
    public void removeListener(ILabelProviderListener listener) {
        // No state change will affect the rendering.
    }

    @Override
    public void dispose() {
        // nothing to dispose
    }
}

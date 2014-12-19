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

package org.eclipse.andmore.internal.ui;

import com.android.ide.common.resources.ResourceFile;
import com.android.ide.common.resources.ResourceItem;
import com.android.ide.common.resources.ResourceRepository;
import com.android.resources.ResourceType;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Content provider for the Resource Explorer TreeView.
 * Each level of the tree is represented by a different class.
 * <ul>
 * <li>{@link ResourceType}. This represents the list of existing Resource Type present
 * in the resources. This can be matched to the subclasses inside the class <code>R</code>
 * </li>
 * <ul>
 * <li>{@link ResourceItem}. This represents one resource (which can existing in various alternate
 * versions). This is similar to the resource Ids defined as <code>R.sometype.id</code>.
 * </li>
 * <ul>
 * <li>{@link ResourceFile}. (optional) This represents a particular version of the
 * {@link ResourceItem}. It is displayed as a list of resource qualifier.
 * </li>
 * </ul>
 * </ul>
 * </ul>
 *
 * @see ResourceLabelProvider
 */
public class ResourceContentProvider implements ITreeContentProvider {

    /**
     * The current ProjectResources being displayed.
     */
    private ResourceRepository mResources;

    private boolean mFullLevels;

   /**
     * Constructs a new content providers for resource display.
     * @param fullLevels if <code>true</code> the content provider will suppport all 3 levels. If
     * <code>false</code>, only two levels are provided.
     */
    public ResourceContentProvider(boolean fullLevels) {
        mFullLevels = fullLevels;
    }

    @Override
    public Object[] getChildren(Object parentElement) {
        if (parentElement instanceof ResourceType) {
            Object[] array = mResources.getResourceItemsOfType(
                    (ResourceType)parentElement).toArray();
            Arrays.sort(array);
            return array;
        } else if (mFullLevels && parentElement instanceof ResourceItem) {
            return ((ResourceItem)parentElement).getSourceFileArray();
        }
        return null;
    }

    @Override
    public Object getParent(Object element) {
        // pass
        return null;
    }

    @Override
    public boolean hasChildren(Object element) {
        if (element instanceof ResourceType) {
            return mResources.hasResourcesOfType((ResourceType)element);
        } else if (mFullLevels && element instanceof ResourceItem) {
            return ((ResourceItem)element).hasAlternates();
        }
        return false;
    }

    @Override
    public Object[] getElements(Object inputElement) {
        if (inputElement instanceof ResourceRepository) {
            if ((ResourceRepository)inputElement == mResources) {
                // get the top level resources.
                List<ResourceType> types = mResources.getAvailableResourceTypes();
                Collections.sort(types);
                return types.toArray();
            }
        }

        return new Object[0];
    }

    @Override
    public void dispose() {
        // pass
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        if (newInput instanceof ResourceRepository) {
             mResources = (ResourceRepository)newInput;
        }
    }
}

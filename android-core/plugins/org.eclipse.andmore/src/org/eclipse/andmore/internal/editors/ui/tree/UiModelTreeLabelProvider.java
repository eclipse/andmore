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

package org.eclipse.andmore.internal.editors.ui.tree;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.editors.IconFactory;
import org.eclipse.andmore.internal.editors.descriptors.ElementDescriptor;
import org.eclipse.andmore.internal.editors.uimodel.UiElementNode;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;

/**
 * UiModelTreeLabelProvider is a trivial implementation of {@link ILabelProvider}
 * where elements are expected to derive from {@link UiElementNode} or
 * from {@link ElementDescriptor}.
 *
 * It is used by both the master tree viewer and by the list in the Add... selection dialog.
 */
public class UiModelTreeLabelProvider implements ILabelProvider {

    public UiModelTreeLabelProvider() {
    }

    /**
     * Returns the element's logo with a fallback on the android logo.
     */
    @Override
    public Image getImage(Object element) {
        ElementDescriptor desc = null;
        UiElementNode node = null;

        if (element instanceof ElementDescriptor) {
            desc = (ElementDescriptor) element;
        } else if (element instanceof UiElementNode) {
            node = (UiElementNode) element;
            desc = node.getDescriptor();
        }

        if (desc != null) {
            Image img = desc.getCustomizedIcon();
            if (img != null) {
                if (node != null && node.hasError()) {
                    return IconFactory.getInstance().addErrorIcon(img);
                } else {
                    return img;
                }
            }
        }

        return AdtPlugin.getAndroidLogo();
    }

    /**
     * Uses UiElementNode.shortDescription for the label for this tree item.
     */
    @Override
    public String getText(Object element) {
        if (element instanceof ElementDescriptor) {
            ElementDescriptor desc = (ElementDescriptor) element;
            return desc.getUiName();
        } else if (element instanceof UiElementNode) {
            UiElementNode node = (UiElementNode) element;
            return node.getShortDescription();
        }
        return element.toString();
    }

    @Override
    public void addListener(ILabelProviderListener listener) {
        // pass
    }

    @Override
    public void dispose() {
        // pass
    }

    @Override
    public boolean isLabelProperty(Object element, String property) {
        // pass
        return false;
    }

    @Override
    public void removeListener(ILabelProviderListener listener) {
        // pass
    }
}



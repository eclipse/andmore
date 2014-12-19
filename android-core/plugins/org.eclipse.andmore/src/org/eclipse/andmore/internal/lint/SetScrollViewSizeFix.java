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
package org.eclipse.andmore.internal.lint;

import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_LAYOUT_HEIGHT;
import static com.android.SdkConstants.ATTR_LAYOUT_WIDTH;
import static com.android.SdkConstants.HORIZONTAL_SCROLL_VIEW;
import static com.android.SdkConstants.VALUE_WRAP_CONTENT;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

@SuppressWarnings("restriction") // DOM model
final class SetScrollViewSizeFix extends DocumentFix {
    private SetScrollViewSizeFix(String id, IMarker marker) {
        super(id, marker);
    }

    @Override
    public boolean needsFocus() {
        return false;
    }

    @Override
    public boolean isCancelable() {
        return false;
    }

    @Override
    protected void apply(IDocument document, IStructuredModel model, Node node, int start,
            int end) {
        if (node instanceof Element && node.getParentNode() instanceof Element) {
            Element element = (Element) node;
            Element parent = (Element) node.getParentNode();

            boolean isHorizontal = HORIZONTAL_SCROLL_VIEW.equals(parent.getTagName());
            String attributeName = isHorizontal ? ATTR_LAYOUT_WIDTH : ATTR_LAYOUT_HEIGHT;
            element.setAttributeNS(ANDROID_URI, attributeName, VALUE_WRAP_CONTENT);
        }
    }

    @Override
    public String getDisplayString() {
        return "Replace size attribute with wrap_content";
    }

    @Override
    public Image getImage() {
        ISharedImages sharedImages = PlatformUI.getWorkbench().getSharedImages();
        // TODO: Need a better icon here
        return sharedImages.getImage(ISharedImages.IMG_OBJ_ELEMENT);
    }
}

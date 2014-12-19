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

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

@SuppressWarnings("restriction") // DOM model
final class ObsoleteLayoutParamsFix extends DocumentFix {
    private ObsoleteLayoutParamsFix(String id, IMarker marker) {
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
    public boolean isBulkCapable() {
        return false;
    }

    @Override
    protected void apply(IDocument document, IStructuredModel model, Node node, int start,
            int end) {
        if (node instanceof Element) {
            Element element = (Element) node;
            NamedNodeMap attributes = element.getAttributes();
            for (int i = 0, n = attributes.getLength(); i < n; i++) {
                Attr attribute = (Attr) attributes.item(i);
                if (attribute instanceof IndexedRegion) {
                    IndexedRegion region = (IndexedRegion) attribute;
                    if (region.getStartOffset() == start) {
                        element.removeAttribute(attribute.getName());
                        return;
                    }
                }
            }
        }
    }

    @Override
    public String getDisplayString() {
        return "Remove attribute";
    }

    @Override
    public Image getImage() {
        ISharedImages sharedImages = PlatformUI.getWorkbench().getSharedImages();
        return sharedImages.getImage(ISharedImages.IMG_ETOOL_DELETE);
    }
}
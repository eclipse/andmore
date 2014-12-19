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

import com.android.tools.lint.checks.TypographyDetector;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.List;

@SuppressWarnings("restriction") // DOM model
final class TypographyFix extends DocumentFix {
    private TypographyFix(String id, IMarker marker) {
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
            // Find the text node which contains the character in question
            NodeList childNodes = element.getChildNodes();
            for (int i = 0, n = childNodes.getLength(); i < n; i++) {
                Node child = childNodes.item(i);
                if (child.getNodeType() == Node.TEXT_NODE) {
                    IndexedRegion region = (IndexedRegion) child;
                    String message = mMarker.getAttribute(IMarker.MESSAGE, "");
                    List<TypographyDetector.ReplaceEdit> edits =
                            TypographyDetector.getEdits(mId, message, child);
                    for (TypographyDetector.ReplaceEdit edit : edits) {
                        try {
                            document.replace(edit.offset + region.getStartOffset(),
                                    edit.length, edit.replaceWith);
                        } catch (BadLocationException e) {
                            AdtPlugin.log(e, null);
                        }
                    }
                }
            }
        }
    }

    @Override
    public String getDisplayString() {
        return "Replace with suggested characters";
    }

    @Override
    public Image getImage() {
        ISharedImages sharedImages = PlatformUI.getWorkbench().getSharedImages();
        // TODO: Need a better icon here
        return sharedImages.getImage(ISharedImages.IMG_OBJ_ELEMENT);
    }
}
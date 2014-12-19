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

import com.android.utils.XmlUtils;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.AdtUtils;
import org.eclipse.andmore.internal.editors.AndroidXmlEditor;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Region;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

@SuppressWarnings("restriction") // DOM model
abstract class SetPropertyFix extends DocumentFix {
    private Region mSelect;

    protected SetPropertyFix(String id, IMarker marker) {
        super(id, marker);
    }

    /** Attribute to be added */
    protected abstract String getAttribute();

    /** Whether it's in the android: namespace */
    protected abstract boolean isAndroidAttribute();

    protected String getProposal(Element element) {
        return invokeCodeCompletion() ? "" : "TODO"; //$NON-NLS-1$
    }

    protected boolean invokeCodeCompletion() {
        return false;
    }

    @Override
    public boolean isCancelable() {
        return false;
    }

    @Override
    protected void apply(IDocument document, IStructuredModel model, Node node, int start,
            int end) {
        mSelect = null;

        if (node instanceof Element) {
            Element element = (Element) node;
            String proposal = getProposal(element);
            String localAttribute = getAttribute();
            String prefix = null;
            if (isAndroidAttribute()) {
                prefix = XmlUtils.lookupNamespacePrefix(node, ANDROID_URI);
            }
            String attribute = prefix != null ? prefix + ':' + localAttribute : localAttribute;

            // This does not work even though it should: it does not include the prefix
            //element.setAttributeNS(ANDROID_URI, localAttribute, proposal);
            // So workaround instead:
            element.setAttribute(attribute, proposal);

            Attr attr = null;
            if (isAndroidAttribute()) {
                attr = element.getAttributeNodeNS(ANDROID_URI, localAttribute);
            } else {
                attr = element.getAttributeNode(localAttribute);
            }
            if (attr instanceof IndexedRegion) {
                IndexedRegion region = (IndexedRegion) attr;
                int offset = region.getStartOffset();
                // We only want to select the value part inside the quotes,
                // so skip the attribute and =" parts added by WST:
                offset += attribute.length() + 2;
                if (selectValue()) {
                    mSelect = new Region(offset, proposal.length());
                }
            }
        }
    }

    protected boolean selectValue() {
        return true;
    }

    @Override
    public void apply(IDocument document) {
        try {
            IFile file = (IFile) mMarker.getResource();
            super.apply(document);
            AdtPlugin.openFile(file, mSelect, true);
        } catch (PartInitException e) {
            AdtPlugin.log(e, null);
        }

        // Invoke code assist
        if (invokeCodeCompletion()) {
            IEditorPart editor = AdtUtils.getActiveEditor();
            if (editor instanceof AndroidXmlEditor) {
                ((AndroidXmlEditor) editor).invokeContentAssist(-1);
            }
        }
    }

    @Override
    public boolean needsFocus() {
        // Because we need to show the editor with text selected
        return true;
    }

    @Override
    public Image getImage() {
        ISharedImages sharedImages = PlatformUI.getWorkbench().getSharedImages();
        return sharedImages.getImage(ISharedImages.IMG_OBJ_ADD);
    }
}

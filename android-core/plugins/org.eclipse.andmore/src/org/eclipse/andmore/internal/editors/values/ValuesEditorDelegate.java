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

package org.eclipse.andmore.internal.editors.values;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.resources.ResourceFolderType;
import com.android.xml.AndroidXPathFactory;

import org.eclipse.andmore.AdtConstants;
import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.editors.common.CommonXmlDelegate;
import org.eclipse.andmore.internal.editors.common.CommonXmlEditor;
import org.eclipse.andmore.internal.editors.descriptors.ElementDescriptor;
import org.eclipse.andmore.internal.editors.values.descriptors.ValuesDescriptors;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.ui.PartInitException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

/**
 * Multi-page form editor for /res/values XML files.
 */
public class ValuesEditorDelegate extends CommonXmlDelegate {

    public static class Creator implements IDelegateCreator {
        @Override
        @SuppressWarnings("unchecked")
        public ValuesEditorDelegate createForFile(
                @NonNull CommonXmlEditor delegator,
                @Nullable ResourceFolderType type) {
            if (ResourceFolderType.VALUES == type) {
                return new ValuesEditorDelegate(delegator);
            }

            return null;
        }
    }

    /**
     * Old standalone-editor ID.
     * Use {@link CommonXmlEditor#ID} instead.
     */
    public static final String LEGACY_EDITOR_ID =
        AdtConstants.EDITORS_NAMESPACE + ".resources.ResourcesEditor"; //$NON-NLS-1$


    /**
     * Creates the form editor for resources XML files.
     */
    private ValuesEditorDelegate(CommonXmlEditor editor) {
        super(editor, new ValuesContentAssist());
        editor.addDefaultTargetListener();
    }

    // ---- Base Class Overrides ----

    /**
     * Create the various form pages.
     */
    @Override
    public void delegateCreateFormPages() {
        try {
            getEditor().addPage(new ValuesTreePage(getEditor()));
        } catch (PartInitException e) {
            AdtPlugin.log(IStatus.ERROR, "Error creating nested page"); //$NON-NLS-1$
            AdtPlugin.getDefault().getLog().log(e.getStatus());
        }
     }

    /**
     * Processes the new XML Model, which XML root node is given.
     *
     * @param xml_doc The XML document, if available, or null if none exists.
     */
    @Override
    public void delegateXmlModelChanged(Document xml_doc) {
        // init the ui root on demand
        delegateInitUiRootNode(false /*force*/);

        getUiRootNode().setXmlDocument(xml_doc);
        if (xml_doc != null) {
            ElementDescriptor resources_desc =
                    ValuesDescriptors.getInstance().getElementDescriptor();
            try {
                XPath xpath = AndroidXPathFactory.newXPath();
                Node node = (Node) xpath.evaluate("/" + resources_desc.getXmlName(),  //$NON-NLS-1$
                        xml_doc,
                        XPathConstants.NODE);
                // Node can be null _or_ it must be the element we searched for.
                assert node == null || node.getNodeName().equals(resources_desc.getXmlName());

                // Refresh the manifest UI node and all its descendants
                getUiRootNode().loadFromXmlNode(node);
            } catch (XPathExpressionException e) {
                AdtPlugin.log(e, "XPath error when trying to find '%s' element in XML.", //$NON-NLS-1$
                        resources_desc.getXmlName());
            }
        }
    }

    /**
     * Creates the initial UI Root Node, including the known mandatory elements.
     * @param force if true, a new UiRootNode is recreated even if it already exists.
     */
    @Override
    public void delegateInitUiRootNode(boolean force) {
        // The manifest UI node is always created, even if there's no corresponding XML node.
        if (getUiRootNode() == null || force) {
            ElementDescriptor resources_desc =
                    ValuesDescriptors.getInstance().getElementDescriptor();
            setUiRootNode(resources_desc.createUiNode());
            getUiRootNode().setEditor(getEditor());

            onDescriptorsChanged();
        }
    }

    // ---- Local methods ----

    private void onDescriptorsChanged() {
        // nothing to be done, as the descriptor are static for now.
        // FIXME Update when the descriptors are not static
    }
}


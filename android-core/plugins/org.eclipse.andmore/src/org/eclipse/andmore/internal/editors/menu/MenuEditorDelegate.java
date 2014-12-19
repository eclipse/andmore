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

package org.eclipse.andmore.internal.editors.menu;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.resources.ResourceFolderType;
import com.android.xml.AndroidXPathFactory;

import org.eclipse.andmore.AdtConstants;
import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.editors.common.CommonXmlDelegate;
import org.eclipse.andmore.internal.editors.common.CommonXmlEditor;
import org.eclipse.andmore.internal.editors.descriptors.ElementDescriptor;
import org.eclipse.andmore.internal.editors.descriptors.ElementDescriptor.Mandatory;
import org.eclipse.andmore.internal.sdk.AndroidTargetData;
import org.eclipse.ui.PartInitException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

/**
 * Multi-page form editor for /res/menu XML files.
 */
public class MenuEditorDelegate extends CommonXmlDelegate {

    public static class Creator implements IDelegateCreator {
        @Override
        @SuppressWarnings("unchecked")
        public MenuEditorDelegate createForFile(
                @NonNull CommonXmlEditor delegator,
                @Nullable ResourceFolderType type) {
            if (ResourceFolderType.MENU == type) {
                return new MenuEditorDelegate(delegator);
            }

            return null;
        }
    }

    /**
     * Old standalone-editor ID.
     * Use {@link CommonXmlEditor#ID} instead.
     */
    public static final String LEGACY_EDITOR_ID =
        AdtConstants.EDITORS_NAMESPACE + ".menu.MenuEditor"; //$NON-NLS-1$

    /**
     * Creates the form editor for resources XML files.
     */
    private MenuEditorDelegate(CommonXmlEditor editor) {
        super(editor, new MenuContentAssist());
        editor.addDefaultTargetListener();
    }

    /**
     * Create the various form pages.
     */
    @Override
    public void delegateCreateFormPages() {
        try {
            getEditor().addPage(new MenuTreePage(getEditor()));
        } catch (PartInitException e) {
            AdtPlugin.log(e, "Error creating nested page"); //$NON-NLS-1$
        }

     }

    private boolean mUpdatingModel;

    /**
     * Processes the new XML Model, which XML root node is given.
     *
     * @param xml_doc The XML document, if available, or null if none exists.
     */
    @Override
    public void delegateXmlModelChanged(Document xml_doc) {
        if (mUpdatingModel) {
            return;
        }

        try {
            mUpdatingModel = true;

            // init the ui root on demand
            delegateInitUiRootNode(false /*force*/);

            getUiRootNode().setXmlDocument(xml_doc);
            if (xml_doc != null) {
                ElementDescriptor root_desc = getUiRootNode().getDescriptor();
                try {
                    XPath xpath = AndroidXPathFactory.newXPath();
                    Node node = (Node) xpath.evaluate("/" + root_desc.getXmlName(),  //$NON-NLS-1$
                            xml_doc,
                            XPathConstants.NODE);
                    if (node == null && root_desc.getMandatory() != Mandatory.NOT_MANDATORY) {
                        // Create the root element if it doesn't exist yet (for empty new documents)
                        node = getUiRootNode().createXmlNode();
                    }

                    // Refresh the manifest UI node and all its descendants
                    getUiRootNode().loadFromXmlNode(node);

                    // TODO ? startMonitoringMarkers();
                } catch (XPathExpressionException e) {
                    AdtPlugin.log(e, "XPath error when trying to find '%s' element in XML.", //$NON-NLS-1$
                            root_desc.getXmlName());
                }
            }

        } finally {
            mUpdatingModel = false;
        }
    }

    /**
     * Creates the initial UI Root Node, including the known mandatory elements.
     * @param force if true, a new UiRootNode is recreated even if it already exists.
     */
    @Override
    public void delegateInitUiRootNode(boolean force) {
        // The root UI node is always created, even if there's no corresponding XML node.
        if (getUiRootNode() == null || force) {
            Document doc = null;
            if (getUiRootNode() != null) {
                doc = getUiRootNode().getXmlDocument();
            }

            // get the target data from the opened file (and its project)
            AndroidTargetData data = getEditor().getTargetData();

            ElementDescriptor desc;
            if (data == null) {
                desc = new ElementDescriptor("temp", null /*children*/);
            } else {
                desc = data.getMenuDescriptors().getDescriptor();
            }

            setUiRootNode(desc.createUiNode());
            getUiRootNode().setEditor(getEditor());

            onDescriptorsChanged(doc);
        }
    }

    // ---- Local Methods ----

    /**
     * Reloads the UI manifest node from the XML, and calls the pages to update.
     */
    private void onDescriptorsChanged(Document document) {
        if (document != null) {
            getUiRootNode().loadFromXmlNode(document);
        } else {
            getUiRootNode().reloadFromXmlNode(getUiRootNode().getXmlNode());
        }
    }
}

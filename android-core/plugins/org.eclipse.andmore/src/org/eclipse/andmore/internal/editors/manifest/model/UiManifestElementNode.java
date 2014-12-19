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

package org.eclipse.andmore.internal.editors.manifest.model;

import com.android.SdkConstants;

import org.eclipse.andmore.internal.editors.descriptors.ElementDescriptor;
import org.eclipse.andmore.internal.editors.manifest.descriptors.AndroidManifestDescriptors;
import org.eclipse.andmore.internal.editors.manifest.descriptors.ManifestElementDescriptor;
import org.eclipse.andmore.internal.editors.uimodel.UiAttributeNode;
import org.eclipse.andmore.internal.editors.uimodel.UiElementNode;
import org.eclipse.andmore.internal.sdk.AndroidTargetData;
import org.w3c.dom.Element;

/**
 * Represents an XML node that can be modified by the user interface in the XML editor.
 * <p/>
 * Each tree viewer used in the application page's parts needs to keep a model representing
 * each underlying node in the tree. This interface represents the base type for such a node.
 * <p/>
 * Each node acts as an intermediary model between the actual XML model (the real data support)
 * and the tree viewers or the corresponding page parts.
 * <p/>
 * Element nodes don't contain data per se. Their data is contained in their attributes
 * as well as their children's attributes, see {@link UiAttributeNode}.
 * <p/>
 * The structure of a given {@link UiElementNode} is declared by a corresponding
 * {@link ElementDescriptor}.
 */
public final class UiManifestElementNode extends UiElementNode {

    /**
     * Creates a new {@link UiElementNode} described by a given {@link ElementDescriptor}.
     *
     * @param elementDescriptor The {@link ElementDescriptor} for the XML node. Cannot be null.
     */
    public UiManifestElementNode(ManifestElementDescriptor elementDescriptor) {
        super(elementDescriptor);
    }

    /**
     * Computes a short string describing the UI node suitable for tree views.
     * Uses the element's attribute "android:name" if present, or the "android:label" one
     * followed by the element's name if not repeated.
     *
     * @return A short string describing the UI node suitable for tree views.
     */
    @Override
    public String getShortDescription() {
        AndroidTargetData target = getAndroidTarget();
        AndroidManifestDescriptors manifestDescriptors = null;
        if (target != null) {
            manifestDescriptors = target.getManifestDescriptors();
        }

        String name = getDescriptor().getUiName();

        if (manifestDescriptors != null &&
                getXmlNode() != null &&
                getXmlNode() instanceof Element &&
                getXmlNode().hasAttributes()) {

            // Application and Manifest nodes have a special treatment: they are unique nodes
            // so we don't bother trying to differentiate their strings and we fall back to
            // just using the UI name below.
            ElementDescriptor desc = getDescriptor();
            if (desc != manifestDescriptors.getManifestElement() &&
                    desc != manifestDescriptors.getApplicationElement()) {
                Element elem = (Element) getXmlNode();
                String attr = _Element_getAttributeNS(elem,
                                    SdkConstants.NS_RESOURCES,
                                    AndroidManifestDescriptors.ANDROID_NAME_ATTR);
                if (attr == null || attr.length() == 0) {
                    attr = _Element_getAttributeNS(elem,
                                    SdkConstants.NS_RESOURCES,
                                    AndroidManifestDescriptors.ANDROID_LABEL_ATTR);
                }
                if (attr != null && attr.length() > 0) {
                    // If the ui name is repeated in the attribute value, don't use it.
                    // Typical case is to avoid ".pkg.MyActivity (Activity)".
                    if (attr.contains(name)) {
                        return attr;
                    } else {
                        return String.format("%1$s (%2$s)", attr, name);
                    }
                }
            }
        }

        return String.format("%1$s", name);
    }

    /**
     * Retrieves an attribute value by local name and namespace URI.
     * <br>Per [<a href='http://www.w3.org/TR/1999/REC-xml-names-19990114/'>XML Namespaces</a>]
     * , applications must use the value <code>null</code> as the
     * <code>namespaceURI</code> parameter for methods if they wish to have
     * no namespace.
     * <p/>
     * Note: This is a wrapper around {@link Element#getAttributeNS(String, String)}.
     * In some versions of webtools, the getAttributeNS implementation crashes with an NPE.
     * This wrapper will return null instead.
     *
     * @see Element#getAttributeNS(String, String)
     * @see <a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=318108">https://bugs.eclipse.org/bugs/show_bug.cgi?id=318108</a>
     * @return The result from {@link Element#getAttributeNS(String, String)} or or an empty string.
     */
    private String _Element_getAttributeNS(Element element,
            String namespaceURI,
            String localName) {
        try {
            return element.getAttributeNS(namespaceURI, localName);
        } catch (Exception ignore) {
            return "";
        }
    }
}


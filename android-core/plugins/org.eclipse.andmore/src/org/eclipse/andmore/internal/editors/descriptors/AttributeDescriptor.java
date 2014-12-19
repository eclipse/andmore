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

package org.eclipse.andmore.internal.editors.descriptors;

import com.android.SdkConstants;
import com.android.ide.common.api.IAttributeInfo;

import org.eclipse.andmore.internal.editors.IconFactory;
import org.eclipse.andmore.internal.editors.uimodel.UiAttributeNode;
import org.eclipse.andmore.internal.editors.uimodel.UiElementNode;
import org.eclipse.swt.graphics.Image;

/**
 * {@link AttributeDescriptor} describes an XML attribute with its XML attribute name.
 * <p/>
 * An attribute descriptor also knows which UI node should be instantiated to represent
 * this particular attribute (e.g. text field, icon chooser, class selector, etc.)
 * Some attributes may be hidden and have no user interface at all.
 * <p/>
 * This is an abstract class. Derived classes must implement data description and return
 * the correct UiAttributeNode-derived class.
 */
public abstract class AttributeDescriptor implements Comparable<AttributeDescriptor> {
    public static final String ATTRIBUTE_ICON_FILENAME = "attribute"; //$NON-NLS-1$

    private final String mXmlLocalName;
    private final String mNsUri;
    private final IAttributeInfo mAttrInfo;
    private ElementDescriptor mParent;

    /**
     * Creates a new {@link AttributeDescriptor}
     *
     * @param xmlLocalName The XML name of the attribute (case sensitive)
     * @param nsUri The URI of the attribute. Can be null if attribute has no namespace.
     *              See {@link SdkConstants#NS_RESOURCES} for a common value.
     * @param attrInfo The {@link IAttributeInfo} of this attribute. Can't be null for a "real"
     *              attribute representing a View element's attribute. Can be null for some
     *              specialized internal attribute descriptors (e.g. hidden descriptors, XMLNS,
     *              or attribute separator, all of which do not represent any real attribute.)
     */
    public AttributeDescriptor(String xmlLocalName, String nsUri, IAttributeInfo attrInfo) {
        assert xmlLocalName != null;
        mXmlLocalName = xmlLocalName;
        mNsUri = nsUri;
        mAttrInfo = attrInfo;
    }

    /** Returns the XML local name of the attribute (case sensitive). */
    public final String getXmlLocalName() {
        return mXmlLocalName;
    }

    /** Returns the namespace URI of this attribute. */
    public final String getNamespaceUri() {
        return mNsUri;
    }

    /** Sets the element descriptor to which this attribute is attached. */
    final void setParent(ElementDescriptor parent) {
        mParent = parent;
    }

    /** Returns the element descriptor to which this attribute is attached. */
    public final ElementDescriptor getParent() {
        return mParent;
    }

    /** Returns whether this attribute is deprecated (based on its attrs.xml javadoc.) */
    public boolean isDeprecated() {
        return mAttrInfo == null ? false : mAttrInfo.getDeprecatedDoc() != null;
    }

    /**
     * Returns the {@link IAttributeInfo} of this attribute.
     * Can't be null for real attributes.
     * Can be null for specialized internal attribute descriptors that do not correspond to
     * any real XML attribute.
     */
    public IAttributeInfo getAttributeInfo() {
        return mAttrInfo;
    }

    /**
     * Returns an optional icon for the attribute.
     * This icon is generic, that is all attribute descriptors have the same icon
     * no matter what they represent.
     *
     * @return An icon for this element or null.
     */
    public Image getGenericIcon() {
        return IconFactory.getInstance().getIcon(ATTRIBUTE_ICON_FILENAME);
    }

    /**
     * @param uiParent The {@link UiElementNode} parent of this UI attribute.
     * @return A new {@link UiAttributeNode} linked to this descriptor or null if this
     *         attribute has no user interface.
     */
    public abstract UiAttributeNode createUiNode(UiElementNode uiParent);

    // Implements Comparable<AttributeDescriptor>
    @Override
    public int compareTo(AttributeDescriptor other) {
        return mXmlLocalName.compareTo(other.mXmlLocalName);
    }
}

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

package org.eclipse.andmore.internal.editors.descriptors;

import com.android.SdkConstants;
import com.android.ide.common.api.IAttributeInfo;
import com.android.ide.common.api.IAttributeInfo.Format;
import com.android.resources.ResourceType;

import org.eclipse.andmore.common.resources.platform.AttributeInfo;
import org.eclipse.andmore.internal.editors.ui.ResourceValueCellEditor;
import org.eclipse.andmore.internal.editors.uimodel.UiAttributeNode;
import org.eclipse.andmore.internal.editors.uimodel.UiElementNode;
import org.eclipse.andmore.internal.editors.uimodel.UiResourceAttributeNode;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.widgets.Composite;

/**
 * Describes an XML attribute displayed containing a value or a reference to a resource.
 * It is displayed by a {@link UiResourceAttributeNode}.
 */
public final class ReferenceAttributeDescriptor extends TextAttributeDescriptor {

    /**
     * The {@link ResourceType} that this reference attribute can accept. It can be null,
     * in which case any reference type can be used.
     */
    private ResourceType mResourceType;

    /**
     * Used by {@link DescriptorsUtils} to create instances of this descriptor.
     */
    public static final ITextAttributeCreator CREATOR = new ITextAttributeCreator() {
        @Override
        public TextAttributeDescriptor create(String xmlLocalName,
                String nsUri, IAttributeInfo attrInfo) {
            return new ReferenceAttributeDescriptor(
                    ResourceType.DRAWABLE,
                    xmlLocalName, nsUri,
                    new AttributeInfo(xmlLocalName, Format.REFERENCE_SET));
        }
    };

    /**
     * Creates a reference attributes that can contain any type of resources.
     * @param xmlLocalName The XML name of the attribute (case sensitive)
     * @param nsUri The URI of the attribute. Can be null if attribute has no namespace.
     *              See {@link SdkConstants#NS_RESOURCES} for a common value.
     * @param attrInfo The {@link IAttributeInfo} of this attribute. Can't be null.
     */
    public ReferenceAttributeDescriptor(String xmlLocalName, String nsUri,
            IAttributeInfo attrInfo) {
        super(xmlLocalName, nsUri, attrInfo);
    }

    /**
     * Creates a reference attributes that can contain a reference to a specific
     * {@link ResourceType}.
     * @param resourceType The specific {@link ResourceType} that this reference attribute supports.
     * It can be <code>null</code>, in which case, all resource types are supported.
     * @param xmlLocalName The XML name of the attribute (case sensitive)
     * @param nsUri The URI of the attribute. Can be null if attribute has no namespace.
     *              See {@link SdkConstants#NS_RESOURCES} for a common value.
     * @param attrInfo The {@link IAttributeInfo} of this attribute. Can't be null.
     */
    public ReferenceAttributeDescriptor(ResourceType resourceType,
            String xmlLocalName, String nsUri, IAttributeInfo attrInfo) {
        super(xmlLocalName, nsUri, attrInfo);
        mResourceType = resourceType;
    }


    /** Returns the {@link ResourceType} that this reference attribute can accept.
     * It can be null, in which case any reference type can be used. */
    public ResourceType getResourceType() {
        return mResourceType;
    }

    /**
     * @return A new {@link UiResourceAttributeNode} linked to this reference descriptor.
     */
    @Override
    public UiAttributeNode createUiNode(UiElementNode uiParent) {
        return new UiResourceAttributeNode(mResourceType, this, uiParent);
    }

    // ------- IPropertyDescriptor Methods

    @Override
    public CellEditor createPropertyEditor(Composite parent) {
        return new ResourceValueCellEditor(parent);
    }

}

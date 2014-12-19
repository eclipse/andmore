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

import com.android.ide.common.api.IAttributeInfo;

import org.eclipse.andmore.internal.editors.ui.ListValueCellEditor;
import org.eclipse.andmore.internal.editors.uimodel.UiAttributeNode;
import org.eclipse.andmore.internal.editors.uimodel.UiElementNode;
import org.eclipse.andmore.internal.editors.uimodel.UiListAttributeNode;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.widgets.Composite;

/**
 * Describes a text attribute that can contains some predefined values.
 * It is displayed by a {@link UiListAttributeNode}.
 */
public class ListAttributeDescriptor extends TextAttributeDescriptor {

    private String[] mValues = null;

    /**
     * Used by {@link DescriptorsUtils} to create instances of this descriptor.
     */
    public static final ITextAttributeCreator CREATOR = new ITextAttributeCreator() {
        @Override
        public TextAttributeDescriptor create(String xmlLocalName,
                String nsUri, IAttributeInfo attrInfo) {
            return new ListAttributeDescriptor(xmlLocalName, nsUri, attrInfo);
        }
    };

    /**
     * Creates a new {@link ListAttributeDescriptor}.
     * <p/>
     * If <code>attrInfo</code> is not null and has non-null enum values, these will be
     * used for the list.
     * Otherwise values are automatically extracted from the FrameworkResourceManager.
     */
    public ListAttributeDescriptor(String xmlLocalName, String nsUri, IAttributeInfo attrInfo) {
        super(xmlLocalName, nsUri, attrInfo);
        if (attrInfo != null) {
            mValues = attrInfo.getEnumValues();
        }
    }

     /**
     * Creates a new {@link ListAttributeDescriptor} which uses the provided values
     * and does not lookup the content of <code>attrInfo</code>.
     */
    public ListAttributeDescriptor(String xmlLocalName, String nsUri, IAttributeInfo attrInfo,
            String[] values) {
        super(xmlLocalName, nsUri, attrInfo);
        mValues = values;
    }

    public String[] getValues() {
        return mValues;
    }

    /**
     * @return A new {@link UiListAttributeNode} linked to this descriptor.
     */
    @Override
    public UiAttributeNode createUiNode(UiElementNode uiParent) {
        return new UiListAttributeNode(this, uiParent);
    }

    // ------- IPropertyDescriptor Methods

    @Override
    public CellEditor createPropertyEditor(Composite parent) {
        return new ListValueCellEditor(parent);
    }
}

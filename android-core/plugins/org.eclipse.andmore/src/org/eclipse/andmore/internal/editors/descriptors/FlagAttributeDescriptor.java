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

import com.android.ide.common.api.IAttributeInfo;

import org.eclipse.andmore.internal.editors.ui.FlagValueCellEditor;
import org.eclipse.andmore.internal.editors.uimodel.UiAttributeNode;
import org.eclipse.andmore.internal.editors.uimodel.UiElementNode;
import org.eclipse.andmore.internal.editors.uimodel.UiFlagAttributeNode;
import org.eclipse.andmore.internal.editors.uimodel.UiListAttributeNode;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.widgets.Composite;

/**
 * Describes a text attribute that can only contains some predefined values.
 * It is displayed by a {@link UiListAttributeNode}.
 *
 * Note: in Android resources, a "flag" is a list of fixed values where one or
 * more values can be selected using an "or", e.g. "align='left|top'".
 * By contrast, an "enum" is a list of fixed values of which only one can be
 * selected at a given time, e.g. "gravity='right'".
 * <p/>
 * This class handles the "flag" case.
 * The "enum" case is done using {@link ListAttributeDescriptor}.
 */
public class FlagAttributeDescriptor extends TextAttributeDescriptor {

    private String[] mNames;

    /**
     * Creates a new {@link FlagAttributeDescriptor}.
     * <p/>
     * If <code>attrInfo</code> is not null and has non-null flag values, these will be
     * used for the list.
     * Otherwise values are automatically extracted from the FrameworkResourceManager.
     */
    public FlagAttributeDescriptor(String xmlLocalName, String nsUri, IAttributeInfo attrInfo) {
        super(xmlLocalName, nsUri, attrInfo);
        if (attrInfo != null) {
            mNames = attrInfo.getFlagValues();
        }
    }

    /**
    * Creates a new {@link FlagAttributeDescriptor} which uses the provided values
    * and does not lookup the content of <code>attrInfo</code>.
    */
    public FlagAttributeDescriptor(String xmlLocalName, String uiName, String nsUri,
            String tooltip, IAttributeInfo attrInfo, String[] names) {
       super(xmlLocalName, nsUri, attrInfo);
       mNames = names;
    }

    /**
     * @return The initial names of the flags. Can be null, in which case the Framework
     *         resource parser should be checked.
     */
    public String[] getNames() {
        return mNames;
    }

    /**
     * @return A new {@link UiListAttributeNode} linked to this descriptor.
     */
    @Override
    public UiAttributeNode createUiNode(UiElementNode uiParent) {
        return new UiFlagAttributeNode(this, uiParent);
    }

    // ------- IPropertyDescriptor Methods

    @Override
    public CellEditor createPropertyEditor(Composite parent) {
        return new FlagValueCellEditor(parent);
    }

}

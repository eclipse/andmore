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

package org.eclipse.andmore.internal.editors.layout.properties;

import org.eclipse.andmore.internal.editors.descriptors.AttributeDescriptor;
import org.eclipse.andmore.internal.editors.descriptors.ListAttributeDescriptor;
import org.eclipse.wb.core.controls.CCombo3;
import org.eclipse.wb.internal.core.model.property.Property;
import org.eclipse.wb.internal.core.model.property.editor.AbstractComboPropertyEditor;
import org.eclipse.wb.internal.core.model.property.editor.ITextValuePropertyEditor;

class EnumXmlPropertyEditor extends AbstractComboPropertyEditor implements
        ITextValuePropertyEditor {
    public static final EnumXmlPropertyEditor INSTANCE = new EnumXmlPropertyEditor();

    private EnumXmlPropertyEditor() {
    }

    @Override
    protected String getText(Property property) throws Exception {
        Object value = property.getValue();
        if (value == null) {
            return "";
        } else if (value instanceof String) {
            return (String) value;
        } else if (value == Property.UNKNOWN_VALUE) {
            return "<varies>";
        } else {
            return "";
        }
    }

    private String[] getItems(Property property) {
        XmlProperty xmlProperty = (XmlProperty) property;
        AttributeDescriptor descriptor = xmlProperty.getDescriptor();
        assert descriptor instanceof ListAttributeDescriptor;
        ListAttributeDescriptor list = (ListAttributeDescriptor) descriptor;
        return list.getValues();
    }

    @Override
    protected void addItems(Property property, CCombo3 combo) throws Exception {
        for (String item : getItems(property)) {
            combo.add(item);
        }
    }

    @Override
    protected void selectItem(Property property, CCombo3 combo) throws Exception {
        combo.setText(getText(property));
    }

    @Override
    protected void toPropertyEx(Property property, CCombo3 combo, int index) throws Exception {
        property.setValue(getItems(property)[index]);
    }

    @Override
    public void setText(Property property, String text) throws Exception {
        property.setValue(text);
    }
}

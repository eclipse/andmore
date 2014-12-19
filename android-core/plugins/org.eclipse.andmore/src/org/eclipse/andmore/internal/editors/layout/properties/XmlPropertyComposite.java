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

import com.android.annotations.NonNull;
import com.google.common.base.Objects;

import org.eclipse.wb.internal.core.model.property.Property;

import java.util.Arrays;

/**
 * Property holding multiple instances of the same {@link XmlProperty} (but
 * bound to difference objects. This is used when multiple objects are selected
 * in the layout editor and the common properties are shown; editing a value
 * will (via {@link #setValue(Object)}) set it on all selected objects.
 * <p>
 * Similar to
 * org.eclipse.wb.internal.core.model.property.GenericPropertyComposite
 */
class XmlPropertyComposite extends XmlProperty {
    private static final Object NO_VALUE = new Object();

    private final XmlProperty[] mProperties;

    public XmlPropertyComposite(XmlProperty primary, XmlProperty[] properties) {
        super(
            primary.getEditor(),
            primary.getFactory(),
            primary.getNode(),
            primary.getDescriptor());
        mProperties = properties;
    }

    @Override
    @NonNull
    public String getTitle() {
        return mProperties[0].getTitle();
    }

    @Override
    public int hashCode() {
        return mProperties.length;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj instanceof XmlPropertyComposite) {
            XmlPropertyComposite property = (XmlPropertyComposite) obj;
            return Arrays.equals(mProperties, property.mProperties);
        }

        return false;
    }

    @Override
    public boolean isModified() throws Exception {
        for (Property property : mProperties) {
            if (property.isModified()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Object getValue() throws Exception {
        Object value = NO_VALUE;
        for (Property property : mProperties) {
            Object propertyValue = property.getValue();
            if (value == NO_VALUE) {
                value = propertyValue;
            } else if (!Objects.equal(value, propertyValue)) {
                return UNKNOWN_VALUE;
            }
        }

        return value;
    }

    @Override
    public void setValue(final Object value) throws Exception {
        // TBD: Wrap in ExecutionUtils.run?
        for (Property property : mProperties) {
            property.setValue(value);
        }
    }

    @NonNull
    public static XmlPropertyComposite create(Property... properties) {
        // Cast from Property into XmlProperty
        XmlProperty[] xmlProperties = new XmlProperty[properties.length];
        for (int i = 0; i < properties.length; i++) {
            Property property = properties[i];
            xmlProperties[i] = (XmlProperty) property;
        }

        XmlPropertyComposite composite = new XmlPropertyComposite(xmlProperties[0], xmlProperties);
        composite.setCategory(xmlProperties[0].getCategory());
        return composite;
    }
}

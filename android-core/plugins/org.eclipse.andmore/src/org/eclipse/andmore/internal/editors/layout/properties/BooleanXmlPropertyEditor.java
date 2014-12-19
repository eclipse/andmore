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

import static com.android.SdkConstants.VALUE_FALSE;
import static com.android.SdkConstants.VALUE_TRUE;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.wb.internal.core.DesignerPlugin;
import org.eclipse.wb.internal.core.model.property.Property;
import org.eclipse.wb.internal.core.model.property.table.PropertyTable;
import org.eclipse.wb.internal.core.utils.ui.DrawUtils;

/**
 * Handle an XML property which represents booleans.
 *
 * Similar to the WindowBuilder PropertyEditor, but operates on Strings rather
 * than Booleans (which means it is a tri-state boolean: true, false, not set)
 */
public class BooleanXmlPropertyEditor extends XmlPropertyEditor {
    public static final BooleanXmlPropertyEditor INSTANCE = new BooleanXmlPropertyEditor();

    private static final Image mTrueImage = DesignerPlugin.getImage("properties/true.png");
    private static final Image mFalseImage = DesignerPlugin.getImage("properties/false.png");
    private static final Image mNullImage =
            DesignerPlugin.getImage("properties/BooleanNull.png");
    private static final Image mUnknownImage =
            DesignerPlugin.getImage("properties/BooleanUnknown.png");

    private BooleanXmlPropertyEditor() {
    }

    @Override
    public void paint(Property property, GC gc, int x, int y, int width, int height)
            throws Exception {
        Object value = property.getValue();
        assert value == null || value instanceof String;
        if (value == null || value instanceof String) {
            String text = (String) value;
            Image image;
            if (VALUE_TRUE.equals(text)) {
                image = mTrueImage;
            } else if (VALUE_FALSE.equals(text)) {
                image = mFalseImage;
            } else if (text == null) {
                image = mNullImage;
            } else {
                // Probably something like a reference, e.g. @boolean/foo
                image = mUnknownImage;
            }

            // draw image
            DrawUtils.drawImageCV(gc, image, x, y, height);

            // prepare new position/width
            int imageWidth = image.getBounds().width + 2;
            width -= imageWidth;

            // draw text
            if (text != null) {
                x += imageWidth;
                DrawUtils.drawStringCV(gc, text, x, y, width, height);
            }
        }
    }

    @Override
    public boolean activate(PropertyTable propertyTable, Property property, Point location)
            throws Exception {
        // check that user clicked on image
        if (location == null || location.x < mTrueImage.getBounds().width + 2) {
            cycleValue(property);
        }
        // don't activate
        return false;
    }

    @Override
    public void doubleClick(Property property, Point location) throws Exception {
        cycleValue(property);
    }

    /**
     * Cycles through the values
     */
    private void cycleValue(Property property) throws Exception {
        Object value = property.getValue();
        if (value == null || value instanceof String) {
            // Cycle null => true => false => null
            String text = (String) value;
            if (VALUE_TRUE.equals(text)) {
                property.setValue(VALUE_FALSE);
            } else if (VALUE_FALSE.equals(text)) {
                property.setValue(null);
            } else {
                property.setValue(VALUE_TRUE);
            }
        } else {
            assert false;
        }
    }
}

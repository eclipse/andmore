/*
 * Copyright (C) 2011 The Android Open Source Project
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
package org.eclipse.andmore.internal.editors.color;

import static com.android.SdkConstants.ANDROID_NS_NAME;
import static com.android.SdkConstants.ANDROID_URI;

import com.android.ide.common.api.IAttributeInfo.Format;
import com.android.resources.ResourceType;

import java.util.Map;

import org.eclipse.andmore.common.resources.platform.AttributeInfo;
import org.eclipse.andmore.common.resources.platform.DeclareStyleableInfo;
import org.eclipse.andmore.internal.editors.animator.AnimatorDescriptors;
import org.eclipse.andmore.internal.editors.descriptors.AttributeDescriptor;
import org.eclipse.andmore.internal.editors.descriptors.ElementDescriptor;
import org.eclipse.andmore.internal.editors.descriptors.IDescriptorProvider;
import org.eclipse.andmore.internal.editors.descriptors.ReferenceAttributeDescriptor;
import org.eclipse.andmore.internal.editors.descriptors.XmlnsAttributeDescriptor;

/** Descriptors for /res/color XML files */
public class ColorDescriptors implements IDescriptorProvider {
    private static final String SDK_URL =
        "http://d.android.com/guide/topics/resources/color-list-resource.html"; //$NON-NLS-1$

    public static final String SELECTOR_TAG = "selector";               //$NON-NLS-1$
    public static final String ATTR_COLOR = "color";                    //$NON-NLS-1$

    /** The root element descriptor */
    private ElementDescriptor mDescriptor = new ElementDescriptor(
            SELECTOR_TAG, "Selector",
            "Required. This must be the root element. Contains one or more <item> elements.",
            SDK_URL,
            new AttributeDescriptor[] {
                    new XmlnsAttributeDescriptor(ANDROID_NS_NAME, ANDROID_URI) },
            null /*children: added later*/, true /*mandatory*/);

    /** @return the root descriptor. */
    @Override
    public ElementDescriptor getDescriptor() {
        if (mDescriptor == null) {
            mDescriptor = new ElementDescriptor("", getRootElementDescriptors()); //$NON-NLS-1$
        }

        return mDescriptor;
    }

    @Override
    public ElementDescriptor[] getRootElementDescriptors() {
        return new ElementDescriptor[] { mDescriptor };
    }

    public synchronized void updateDescriptors(Map<String, DeclareStyleableInfo> styleMap) {
        if (styleMap == null) {
            return;
        }

        // Selector children
        ElementDescriptor selectorItem = AnimatorDescriptors.addElement(null, styleMap,
            "item", "Item", "DrawableStates", null, //$NON-NLS-1$ //$NON-NLS-3$
            "Defines a drawable to use during certain states, as described by "
                 + "its attributes. Must be a child of a <selector> element.",
            SDK_URL,
            new ReferenceAttributeDescriptor(
                    ResourceType.COLOR, ATTR_COLOR,
                    ANDROID_URI,
                    new AttributeInfo(ATTR_COLOR, Format.COLOR_SET)).setTooltip(
                "Hexadeximal color. Required. The color is specified with an RGB value and "
                    + "optional alpha channel.\n"
                    + "The value always begins with a pound (#) character and then "
                    + "followed by the Alpha-Red-Green-Blue information in one of "
                    + "the following formats:\n"
                    + "* RGB\n"
                    + "* ARGB\n"
                    + "* RRGGBB\n"
                    + "* AARRGGBB"),
            null, /* This is wrong -- we can now embed any above drawable
                        (but without xmlns as extra) */
            false /*mandatory*/);

        if (selectorItem != null) {
            mDescriptor.setChildren(new ElementDescriptor[] { selectorItem });
        }
    }
}

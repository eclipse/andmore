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
package org.eclipse.andmore.internal.editors.animator;

import static com.android.SdkConstants.ANDROID_NS_NAME;
import static com.android.SdkConstants.ANDROID_URI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.andmore.common.resources.platform.DeclareStyleableInfo;
import org.eclipse.andmore.internal.editors.descriptors.AttributeDescriptor;
import org.eclipse.andmore.internal.editors.descriptors.DescriptorsUtils;
import org.eclipse.andmore.internal.editors.descriptors.ElementDescriptor;
import org.eclipse.andmore.internal.editors.descriptors.IDescriptorProvider;
import org.eclipse.andmore.internal.editors.descriptors.XmlnsAttributeDescriptor;

/**
 * Descriptors for /res/animator XML files.
 */
public class AnimatorDescriptors implements IDescriptorProvider {
    /** The root element descriptor */
    private ElementDescriptor mDescriptor;
    /** The root element descriptors */
    private ElementDescriptor[] mRootDescriptors;
    private Map<String, ElementDescriptor> nameToDescriptor;

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
        return mRootDescriptors;
    }

    ElementDescriptor getElementDescriptor(String rootTag) {
        if (nameToDescriptor == null) {
            nameToDescriptor = new HashMap<String, ElementDescriptor>();
            for (ElementDescriptor descriptor : getRootElementDescriptors()) {
                nameToDescriptor.put(descriptor.getXmlName(), descriptor);
            }
        }

        ElementDescriptor descriptor = nameToDescriptor.get(rootTag);
        if (descriptor == null) {
            descriptor = getDescriptor();
        }
        return descriptor;
    }

    public synchronized void updateDescriptors(Map<String, DeclareStyleableInfo> styleMap) {
        if (styleMap == null) {
            return;
        }

        XmlnsAttributeDescriptor xmlns = new XmlnsAttributeDescriptor(ANDROID_NS_NAME,
                ANDROID_URI);

        List<ElementDescriptor> descriptors = new ArrayList<ElementDescriptor>();

        String sdkUrl =
            "http://developer.android.com/guide/topics/graphics/animation.html"; //$NON-NLS-1$

        ElementDescriptor set = addElement(descriptors, styleMap,
                "set", "Animator Set", "AnimatorSet", null, //$NON-NLS-1$ //$NON-NLS-3$
                null /* tooltip */, sdkUrl,
                xmlns, null, true /*mandatory*/);

        ElementDescriptor objectAnimator = addElement(descriptors, styleMap,
                "objectAnimator", "Object Animator", //$NON-NLS-1$
                "PropertyAnimator", "Animator", //$NON-NLS-1$ //$NON-NLS-2$
                null /* tooltip */, sdkUrl,
                xmlns, null, true /*mandatory*/);

        ElementDescriptor animator = addElement(descriptors, styleMap,
                "animator", "Animator", "Animator", null, //$NON-NLS-1$ //$NON-NLS-3$
                null /* tooltip */, sdkUrl,
                xmlns, null, true /*mandatory*/);

        mRootDescriptors = descriptors.toArray(new ElementDescriptor[descriptors.size()]);

        // Allow arbitrary nesting: the children of all of these element can include
        // any of the others
        if (objectAnimator != null) {
            objectAnimator.setChildren(mRootDescriptors);
        }
        if (animator != null) {
            animator.setChildren(mRootDescriptors);
        }
        if (set != null) {
            set.setChildren(mRootDescriptors);
        }
    }

    /**
     * Looks up the given style, and if found creates a new {@link ElementDescriptor}
     * corresponding to the style. It can optionally take an extra style to merge in
     * additional attributes from, and an extra attribute to add in as well. The new
     * element, if it exists, can also be optionally added into a list.
     *
     * @param descriptors an optional list to add the element into, or null
     * @param styleMap The map style => attributes from the attrs.xml file
     * @param xmlName the XML tag name to use for the element
     * @param uiName the UI name to display the element as
     * @param styleName the name of the style which must exist for this style
     * @param extraStyle an optional extra style to merge in attributes from, or null
     * @param tooltip the tooltip or documentation for this element, or null
     * @param sdkUrl an optional SDK url to display for the element, or null
     * @param extraAttribute an extra attribute to add to the attributes list, or null
     * @param childrenElements an array of children allowed by this element, or null
     * @param mandatory if true, this element is mandatory
     * @return a newly created element, or null if the style does not exist
     */
    public static ElementDescriptor addElement(
            List<ElementDescriptor> descriptors,
            Map<String, DeclareStyleableInfo> styleMap,
            String xmlName, String uiName, String styleName, String extraStyle,
            String tooltip, String sdkUrl,
            AttributeDescriptor extraAttribute,
            ElementDescriptor[] childrenElements,
            boolean mandatory) {
        DeclareStyleableInfo style = styleMap.get(styleName);
        if (style == null) {
            return null;
        }
        ElementDescriptor element = new ElementDescriptor(xmlName, uiName, tooltip, sdkUrl,
                null, childrenElements, mandatory);

        ArrayList<AttributeDescriptor> descs = new ArrayList<AttributeDescriptor>();

        DescriptorsUtils.appendAttributes(descs,
                null,   // elementName
                ANDROID_URI,
                style.getAttributes(),
                null,   // requiredAttributes
                null);  // overrides
        element.setTooltip(style.getJavaDoc());

        if (extraStyle != null) {
            style = styleMap.get(extraStyle);
            if (style != null) {
                DescriptorsUtils.appendAttributes(descs,
                        null,   // elementName
                        ANDROID_URI,
                        style.getAttributes(),
                        null,   // requiredAttributes
                        null);  // overrides
            }
        }

        if (extraAttribute != null) {
            descs.add(extraAttribute);
        }

        element.setAttributes(descs.toArray(new AttributeDescriptor[descs.size()]));
        if (descriptors != null) {
            descriptors.add(element);
        }

        return element;
    }
}

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

import com.android.SdkConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.andmore.common.resources.platform.DeclareStyleableInfo;
import org.eclipse.andmore.internal.editors.descriptors.ElementDescriptor;
import org.eclipse.andmore.internal.editors.descriptors.IDescriptorProvider;
import org.eclipse.andmore.internal.editors.descriptors.XmlnsAttributeDescriptor;

/** Descriptors for the res/anim resources */
public class AnimDescriptors implements IDescriptorProvider {
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

    public ElementDescriptor getElementDescriptor(String mRootTag) {
        if (nameToDescriptor == null) {
            nameToDescriptor = new HashMap<String, ElementDescriptor>();
            for (ElementDescriptor descriptor : getRootElementDescriptors()) {
                nameToDescriptor.put(descriptor.getXmlName(), descriptor);
            }
        }

        ElementDescriptor descriptor = nameToDescriptor.get(mRootTag);
        if (descriptor == null) {
            descriptor = getDescriptor();
        }
        return descriptor;
    }

    public synchronized void updateDescriptors(Map<String, DeclareStyleableInfo> styleMap) {
        if (styleMap == null) {
            return;
        }

        XmlnsAttributeDescriptor xmlns = new XmlnsAttributeDescriptor(SdkConstants.ANDROID_NS_NAME,
                SdkConstants.ANDROID_URI);

        List<ElementDescriptor> descriptors = new ArrayList<ElementDescriptor>();

        String sdkUrl =
            "http://developer.android.com/guide/topics/graphics/view-animation.html"; //$NON-NLS-1$
        ElementDescriptor set = AnimatorDescriptors.addElement(descriptors, styleMap,
                "set", "Set", "AnimationSet", "Animation", //$NON-NLS-1$ //$NON-NLS-3$ //$NON-NLS-4$
                "A container that holds other animation elements (<alpha>, <scale>, "
                    + "<translate>, <rotate>) or other <set> elements. ",
                sdkUrl,
                xmlns, null, true /*mandatory*/);

        AnimatorDescriptors.addElement(descriptors, styleMap,
                "alpha", "Alpha", "AlphaAnimation", "Animation", //$NON-NLS-1$ //$NON-NLS-3$ //$NON-NLS-4$
                "A fade-in or fade-out animation.",
                sdkUrl,
                xmlns, null, true /*mandatory*/);

        AnimatorDescriptors.addElement(descriptors, styleMap,
                "scale", "Scale", "ScaleAnimation", "Animation", //$NON-NLS-1$ //$NON-NLS-3$ //$NON-NLS-4$
                "A resizing animation. You can specify the center point of the image from "
                    + "which it grows outward (or inward) by specifying pivotX and pivotY. "
                    + "For example, if these values are 0, 0 (top-left corner), all growth "
                    + "will be down and to the right.",
                sdkUrl,
                xmlns, null, true /*mandatory*/);

        AnimatorDescriptors.addElement(descriptors, styleMap,
                "rotate", "Rotate", "RotateAnimation", "Animation", //$NON-NLS-1$ //$NON-NLS-3$ //$NON-NLS-4$
                "A rotation animation.",
                sdkUrl,
                xmlns, null, true /*mandatory*/);

        AnimatorDescriptors.addElement(descriptors, styleMap,
                "translate", "Translate", "TranslateAnimation", "Animation", //$NON-NLS-1$ //$NON-NLS-3$ //$NON-NLS-4$
                "A vertical and/or horizontal motion. Supports the following attributes in "
                    + "any of the following three formats: values from -100 to 100 ending "
                    + "with \"%\", indicating a percentage relative to itself; values from "
                    + "-100 to 100 ending in \"%p\", indicating a percentage relative to its "
                    + "parent; a float value with no suffix, indicating an absolute value.",
                sdkUrl,
                xmlns, null, true /*mandatory*/);

        mRootDescriptors = descriptors.toArray(new ElementDescriptor[descriptors.size()]);

        // Allow <set> to nest the others (and other sets)
        if (set != null) {
            set.setChildren(mRootDescriptors);
        }
    }
}

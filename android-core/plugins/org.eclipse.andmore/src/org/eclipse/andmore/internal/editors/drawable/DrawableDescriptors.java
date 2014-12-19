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
package org.eclipse.andmore.internal.editors.drawable;

import static com.android.SdkConstants.ANDROID_NS_NAME;
import static com.android.SdkConstants.ANDROID_URI;

import com.android.ide.common.api.IAttributeInfo.Format;
import com.android.resources.ResourceType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.andmore.common.resources.platform.AttributeInfo;
import org.eclipse.andmore.common.resources.platform.DeclareStyleableInfo;
import org.eclipse.andmore.internal.editors.animator.AnimatorDescriptors;
import org.eclipse.andmore.internal.editors.descriptors.ElementDescriptor;
import org.eclipse.andmore.internal.editors.descriptors.IDescriptorProvider;
import org.eclipse.andmore.internal.editors.descriptors.ReferenceAttributeDescriptor;
import org.eclipse.andmore.internal.editors.descriptors.XmlnsAttributeDescriptor;

/**
 * Descriptors for /res/drawable files
 */
public class DrawableDescriptors implements IDescriptorProvider {
    private static final String SDK_URL_BASE =
        "http://d.android.com/guide/topics/resources/"; //$NON-NLS-1$

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

    /**
     * Returns a descriptor for the given root tag name
     *
     * @param tag the tag name to look up a descriptor for
     * @return a descriptor with the given tag name
     */
    public ElementDescriptor getElementDescriptor(String tag) {
        if (nameToDescriptor == null) {
            nameToDescriptor = new HashMap<String, ElementDescriptor>();
            for (ElementDescriptor descriptor : getRootElementDescriptors()) {
                nameToDescriptor.put(descriptor.getXmlName(), descriptor);
            }
        }

        ElementDescriptor descriptor = nameToDescriptor.get(tag);
        if (descriptor == null) {
            descriptor = getDescriptor();
        }
        return descriptor;
    }

    public synchronized void updateDescriptors(Map<String, DeclareStyleableInfo> styleMap) {
        XmlnsAttributeDescriptor xmlns = new XmlnsAttributeDescriptor(ANDROID_NS_NAME,
                ANDROID_URI);
        List<ElementDescriptor> descriptors = new ArrayList<ElementDescriptor>();

        AnimatorDescriptors.addElement(descriptors, styleMap,
            "animation-list", "Animation List", "AnimationDrawable", null, //$NON-NLS-1$ //$NON-NLS-3$
            "An animation defined in XML that shows a sequence of images in "
                + "order (like a film)",
            SDK_URL_BASE + "animation-resource.html#Frame",
            xmlns, null, true /*mandatory*/);

        /* For some reason, android.graphics.drawable.AnimatedRotateDrawable is marked with @hide
         * so we should not register it and its attributes here. See issue #19248 for details.
        AnimatorDescriptors.addElement(descriptors, styleMap,
            "animated-rotate", "Animated Rotate", "AnimatedRotateDrawable", null, //$NON-NLS-1$ //$NON-NLS-3$
            // Need docs
            null, // tooltip
            null, // sdk_url
            xmlns, null, true);
        */

        AnimatorDescriptors.addElement(descriptors, styleMap,
            "bitmap", "BitMap", "BitmapDrawable", null, //$NON-NLS-1$ //$NON-NLS-3$
            "An XML bitmap is a resource defined in XML that points to a bitmap file. "
                    + "The effect is an alias for a raw bitmap file. The XML can "
                    + "specify additional properties for the bitmap such as "
                    + "dithering and tiling.",
            SDK_URL_BASE + "drawable-resource.html#Bitmap", //$NON-NLS-1$
            xmlns, null, true /* mandatory */);

        AnimatorDescriptors.addElement(descriptors, styleMap,
            "clip", "Clip", "ClipDrawable", null, //$NON-NLS-1$ //$NON-NLS-3$
            "An XML file that defines a drawable that clips another Drawable based on "
                + "this Drawable's current level value.",
            SDK_URL_BASE + "drawable-resource.html#Clip", //$NON-NLS-1$
            xmlns, null, true /*mandatory*/);

        AnimatorDescriptors.addElement(descriptors, styleMap,
            "color", "Color", "ColorDrawable", null, //$NON-NLS-1$ //$NON-NLS-3$
            "XML resource that carries a color value (a hexadecimal color)",
            SDK_URL_BASE + "more-resources.html#Color",
            xmlns, null, true /*mandatory*/);

        AnimatorDescriptors.addElement(descriptors, styleMap,
            "inset", "Inset", "InsetDrawable", null, //$NON-NLS-1$ //$NON-NLS-3$
            "An XML file that defines a drawable that insets another drawable by a "
                + "specified distance. This is useful when a View needs a background "
                + "drawble that is smaller than the View's actual bounds.",
            SDK_URL_BASE + "drawable-resource.html#Inset", //$NON-NLS-1$
            xmlns, null, true /*mandatory*/);

        // Layer list

        // An <item> in a <selector> or <
        ElementDescriptor layerItem = AnimatorDescriptors.addElement(null, styleMap,
                "item", "Item", "LayerDrawableItem", null, //$NON-NLS-1$ //$NON-NLS-3$
                "Defines a drawable to place in the layer drawable, in a position "
                    + "defined by its attributes. Must be a child of a <selector> "
                    + "element. Accepts child <bitmap> elements.",
                SDK_URL_BASE + "drawable-resource.html#LayerList", //$NON-NLS-1$
                null, /* extra attribute */
                null, /* This is wrong -- we can now embed any above drawable
                            (but without xmlns as extra) */
                false /*mandatory*/);
        ElementDescriptor[] layerChildren = layerItem != null
            ? new ElementDescriptor[] { layerItem } : null;

        AnimatorDescriptors.addElement(descriptors, styleMap,
            "layer-list", "Layer List", "LayerDrawable", null, //$NON-NLS-1$ //$NON-NLS-3$
            "A Drawable that manages an array of other Drawables. These are drawn in "
                    + "array order, so the element with the largest index is be drawn on top.",
            SDK_URL_BASE + "drawable-resource.html#LayerList", //$NON-NLS-1$
            xmlns,
            layerChildren,
            true /*mandatory*/);

        // Level list children
        ElementDescriptor levelListItem = AnimatorDescriptors.addElement(null, styleMap,
            "item", "Item", "LevelListDrawableItem", null, //$NON-NLS-1$ //$NON-NLS-3$
            "Defines a drawable to use at a certain level.",
            SDK_URL_BASE + "drawable-resource.html#LevelList", //$NON-NLS-1$
            null, /* extra attribute */
            null, /* no further children */
            // TODO: The inflation code seems to show that all drawables can be nested here!
            false /*mandatory*/);
        AnimatorDescriptors.addElement(descriptors, styleMap,
            "level-list", "Level List", "LevelListDrawable", null, //$NON-NLS-1$ //$NON-NLS-3$
            "An XML file that defines a drawable that manages a number of alternate "
                + "Drawables, each assigned a maximum numerical value",
            SDK_URL_BASE + "drawable-resource.html#LevelList", //$NON-NLS-1$
            xmlns,
            levelListItem != null ? new ElementDescriptor[] { levelListItem } : null,
            true /*mandatory*/);

        // Not yet supported
        //addElement(descriptors, styleMap, "mipmap", "Mipmap", "MipmapDrawable", null,
        //        null /* tooltip */,
        //        null /* sdk_url */,
        //        xmlns, null, true /*mandatory*/);

        AnimatorDescriptors.addElement(descriptors, styleMap,
            "nine-patch", "Nine Patch", "NinePatchDrawable", null, //$NON-NLS-1$ //$NON-NLS-3$
            "A PNG file with stretchable regions to allow image resizing "
                    + "based on content (.9.png).",
            SDK_URL_BASE + "drawable-resource.html#NinePatch", //$NON-NLS-1$
            xmlns, null, true /*mandatory*/);

        AnimatorDescriptors.addElement(descriptors, styleMap,
            "rotate", "Rotate", "RotateDrawable", null, //$NON-NLS-1$ //$NON-NLS-3$
            // Need docs
            null /* tooltip */,
            null /* sdk_url */,
            xmlns, null, true /*mandatory*/);

        AnimatorDescriptors.addElement(descriptors, styleMap,
            "scale", "Shape", "ScaleDrawable", null, //$NON-NLS-1$ //$NON-NLS-3$
            "An XML file that defines a drawable that changes the size of another Drawable "
                + "based on its current level value.",
            SDK_URL_BASE + "drawable-resource.html#Scale", //$NON-NLS-1$
            xmlns, null, true /*mandatory*/);

        // Selector children
        ElementDescriptor selectorItem = AnimatorDescriptors.addElement(null, styleMap,
            "item", "Item", "DrawableStates", null, //$NON-NLS-1$ //$NON-NLS-3$
            "Defines a drawable to use during certain states, as described by "
                 + "its attributes. Must be a child of a <selector> element.",
            SDK_URL_BASE + "drawable-resource.html#StateList", //$NON-NLS-1$
            new ReferenceAttributeDescriptor(
                    ResourceType.DRAWABLE, "drawable", ANDROID_URI, //$NON-NLS-1$
                    new AttributeInfo("drawable", Format.REFERENCE_SET))
                    .setTooltip("Reference to a drawable resource."),
            null, /* This is wrong -- we can now embed any above drawable
                        (but without xmlns as extra) */
            false /*mandatory*/);

        AnimatorDescriptors.addElement(descriptors, styleMap,
            "selector", "Selector", "StateListDrawable", null, //$NON-NLS-1$ //$NON-NLS-3$
            "An XML file that references different bitmap graphics for different states "
                + "(for example, to use a different image when a button is pressed).",
            SDK_URL_BASE + "drawable-resource.html#StateList", //$NON-NLS-1$
            xmlns,
            selectorItem != null ? new ElementDescriptor[] { selectorItem } : null,
            true /*mandatory*/);

        // Shape
        // Shape children
        List<ElementDescriptor> shapeChildren = new ArrayList<ElementDescriptor>();
        // Selector children
        AnimatorDescriptors.addElement(shapeChildren, styleMap,
            "size", "Size", "GradientDrawableSize", null, //$NON-NLS-1$ //$NON-NLS-3$
            null /* tooltip */, null /* sdk_url */, null /* extra attribute */,
            null /* children */, false /* mandatory */);
        AnimatorDescriptors.addElement(shapeChildren, styleMap,
            "gradient", "Gradient", "GradientDrawableGradient", null, //$NON-NLS-1$ //$NON-NLS-3$
            null /* tooltip */, null /* sdk_url */, null /* extra attribute */,
            null /* children */, false /* mandatory */);
        AnimatorDescriptors.addElement(shapeChildren, styleMap,
            "solid", "Solid", "GradientDrawableSolid", null, //$NON-NLS-1$ //$NON-NLS-3$
            null /* tooltip */, null /* sdk_url */, null /* extra attribute */,
            null /* children */, false /* mandatory */);
        AnimatorDescriptors.addElement(shapeChildren, styleMap,
            "stroke", "Stroke", "GradientDrawableStroke", null, //$NON-NLS-1$ //$NON-NLS-3$
            null /* tooltip */, null /* sdk_url */, null /* extra attribute */,
            null /* children */, false /* mandatory */);
        AnimatorDescriptors.addElement(shapeChildren, styleMap,
            "corners", "Corners", "DrawableCorners", null, //$NON-NLS-1$ //$NON-NLS-3$
            null /* tooltip */, null /* sdk_url */, null /* extra attribute */,
            null /* children */, false /* mandatory */);
        AnimatorDescriptors.addElement(shapeChildren, styleMap,
            "padding", "Padding", "GradientDrawablePadding", null, //$NON-NLS-1$ //$NON-NLS-3$
            null /* tooltip */, null /* sdk_url */, null /* extra attribute */,
            null /* children */, false /* mandatory */);

        AnimatorDescriptors.addElement(descriptors, styleMap,
            "shape", "Shape", //$NON-NLS-1$

            // The documentation says that a <shape> element creates a ShapeDrawable,
            // but ShapeDrawable isn't finished and the code currently creates
            // a GradientDrawable.
            //"ShapeDrawable", //$NON-NLS-1$
            "GradientDrawable", //$NON-NLS-1$

            null,
            "An XML file that defines a geometric shape, including colors and gradients.",
            SDK_URL_BASE + "drawable-resource.html#Shape", //$NON-NLS-1$
            xmlns,

            // These are the GradientDrawable children, not the ShapeDrawable children
            shapeChildren.toArray(new ElementDescriptor[shapeChildren.size()]),
            true /*mandatory*/);

        AnimatorDescriptors.addElement(descriptors, styleMap,
            "transition", "Transition", "TransitionDrawable", null, //$NON-NLS-1$ //$NON-NLS-3$
            "An XML file that defines a drawable that can cross-fade between two "
                + "drawable resources. Each drawable is represented by an <item> "
                + "element inside a single <transition> element. No more than two "
                + "items are supported. To transition forward, call startTransition(). "
                + "To transition backward, call reverseTransition().",
            SDK_URL_BASE + "drawable-resource.html#Transition", //$NON-NLS-1$
            xmlns,
            layerChildren, // children: a TransitionDrawable is a LayerDrawable
            true /*mandatory*/);

        mRootDescriptors = descriptors.toArray(new ElementDescriptor[descriptors.size()]);

        // A <selector><item> can contain any of the top level drawables
        if (selectorItem != null) {
            selectorItem.setChildren(mRootDescriptors);
        }
        // Docs says it can accept <bitmap> but code comment suggests any is possible;
        // test and either use this or just { bitmap }
        if (layerItem != null) {
            layerItem.setChildren(mRootDescriptors);
        }
    }
}

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

package org.eclipse.andmore.internal.editors.values.descriptors;

import static com.android.SdkConstants.ATTR_NAME;
import static com.android.SdkConstants.ATTR_TYPE;
import static com.android.SdkConstants.TAG_COLOR;
import static com.android.SdkConstants.TAG_DIMEN;
import static com.android.SdkConstants.TAG_DRAWABLE;
import static com.android.SdkConstants.TAG_INTEGER_ARRAY;
import static com.android.SdkConstants.TAG_ITEM;
import static com.android.SdkConstants.TAG_PLURALS;
import static com.android.SdkConstants.TAG_RESOURCES;
import static com.android.SdkConstants.TAG_STRING;
import static com.android.SdkConstants.TAG_STRING_ARRAY;
import static com.android.SdkConstants.TAG_STYLE;

import com.android.ide.common.api.IAttributeInfo.Format;
import com.android.resources.ResourceType;

import java.util.EnumSet;

import org.eclipse.andmore.common.resources.platform.AttributeInfo;
import org.eclipse.andmore.internal.editors.descriptors.AttributeDescriptor;
import org.eclipse.andmore.internal.editors.descriptors.ElementDescriptor;
import org.eclipse.andmore.internal.editors.descriptors.EnumAttributeDescriptor;
import org.eclipse.andmore.internal.editors.descriptors.FlagAttributeDescriptor;
import org.eclipse.andmore.internal.editors.descriptors.IDescriptorProvider;
import org.eclipse.andmore.internal.editors.descriptors.ListAttributeDescriptor;
import org.eclipse.andmore.internal.editors.descriptors.TextAttributeDescriptor;
import org.eclipse.andmore.internal.editors.descriptors.TextValueDescriptor;


/**
 * Complete description of the structure for resources XML files (under res/values/)
 */
public final class ValuesDescriptors implements IDescriptorProvider {
    private static final ValuesDescriptors sThis = new ValuesDescriptors();

    /** The {@link ElementDescriptor} for the root Resources element. */
    public final ElementDescriptor mResourcesElement;

    public static ValuesDescriptors getInstance() {
        return sThis;
    }

    @Override
    public ElementDescriptor[] getRootElementDescriptors() {
        return new ElementDescriptor[] { mResourcesElement };
    }

    @Override
    public ElementDescriptor getDescriptor() {
        return mResourcesElement;
    }

    public ElementDescriptor getElementDescriptor() {
        return mResourcesElement;
    }

    private ValuesDescriptors() {

        // Common attributes used in many placed

        // Elements

        AttributeInfo nameAttrInfo = new AttributeInfo(ATTR_NAME, Format.STRING_SET);

        ElementDescriptor color_element = new ElementDescriptor(
                TAG_COLOR,
                "Color",
                "A @color@ value specifies an RGB value with an alpha channel, which can be used in various places such as specifying a solid color for a Drawable or the color to use for text.  It always begins with a # character and then is followed by the alpha-red-green-blue information in one of the following formats: #RGB, #ARGB, #RRGGBB or #AARRGGBB.",
                "http://code.google.com/android/reference/available-resources.html#colorvals",  //$NON-NLS-1$
                new AttributeDescriptor[] {
                        new TextAttributeDescriptor(ATTR_NAME,
                                null /* nsUri */,
                                nameAttrInfo),
                        new ColorValueDescriptor(
                                "Value*",
                                "A mandatory color value.")
                        .setTooltip("The mandatory name used in referring to this color.")
                },
                null,  // no child nodes
                false /* not mandatory */);

        ElementDescriptor string_element = new ElementDescriptor(
                TAG_STRING,
                "String",
                "@Strings@, with optional simple formatting, can be stored and retrieved as resources. You can add formatting to your string by using three standard HTML tags: b, i, and u. If you use an apostrophe or a quote in your string, you must either escape it or enclose the whole string in the other kind of enclosing quotes.",
                "http://code.google.com/android/reference/available-resources.html#stringresources",  //$NON-NLS-1$
                new AttributeDescriptor[] {
                        new TextAttributeDescriptor(ATTR_NAME,
                                null /* nsUri */,
                                nameAttrInfo)
                        .setTooltip("The mandatory name used in referring to this string."),
                        new TextValueDescriptor(
                                "Value*",
                                "A mandatory string value.")
                },
                null,  // no child nodes
                false /* not mandatory */);

        ElementDescriptor item_element = new ItemElementDescriptor(
                 TAG_ITEM,
                 "Item",
                 null,  // TODO find javadoc
                 null,  // TODO find link to javadoc
                 new AttributeDescriptor[] {
                         new TextAttributeDescriptor(ATTR_NAME,
                                 null /* nsUri */,
                                 nameAttrInfo)
                         .setTooltip("The mandatory name used in referring to this resource."),
                         new ListAttributeDescriptor(ATTR_TYPE,
                                 null /* nsUri */,
                                 new AttributeInfo(ATTR_TYPE,
                                         EnumSet.of(Format.STRING, Format.ENUM)
                                 ).setEnumValues(ResourceType.getNames())
                         ).setTooltip("The mandatory type of this resource."),
                         new FlagAttributeDescriptor("format",      //$NON-NLS-1$
                                 null /* nsUri */,
                                 new AttributeInfo("format",
                                         EnumSet.of(Format.STRING, Format.FLAG)
                                 ).setFlagValues(
                                     new String[] {
                                         "boolean",     //$NON-NLS-1$
                                         TAG_COLOR,
                                         "dimension",   //$NON-NLS-1$
                                         "float",       //$NON-NLS-1$
                                         "fraction",    //$NON-NLS-1$
                                         "integer",     //$NON-NLS-1$
                                         "reference",   //$NON-NLS-1$
                                         "string"       //$NON-NLS-1$
                                     } )
                         ).setTooltip("The optional format of this resource."),
                         new TextValueDescriptor(
                                 "Value",
                                 "A standard string, hex color value, or reference to any other resource type.")
                 },
                 null,  // no child nodes
                 false /* not mandatory */);

        ElementDescriptor drawable_element = new ElementDescriptor(
                TAG_DRAWABLE,
                "Drawable",
                "A @drawable@ defines a rectangle of color. Android accepts color values written in various web-style formats -- a hexadecimal constant in any of the following forms: #RGB, #ARGB, #RRGGBB, #AARRGGBB. Zero in the alpha channel means transparent. The default value is opaque.",
                "http://code.google.com/android/reference/available-resources.html#colordrawableresources",  //$NON-NLS-1$
                new AttributeDescriptor[] {
                        new TextAttributeDescriptor(ATTR_NAME,
                                null /* nsUri */,
                                nameAttrInfo)
                        .setTooltip("The mandatory name used in referring to this drawable."),
                        new TextValueDescriptor(
                                "Value*",
                                "A mandatory color value in the form #RGB, #ARGB, #RRGGBB or #AARRGGBB.")
                },
                null,  // no child nodes
                false /* not mandatory */);

        ElementDescriptor dimen_element = new ElementDescriptor(
                TAG_DIMEN,
                "Dimension",
                "You can create common dimensions to use for various screen elements by defining @dimension@ values in XML. A dimension resource is a number followed by a unit of measurement. Supported units are px (pixels), in (inches), mm (millimeters), pt (points at 72 DPI), dp (density-independent pixels) and sp (scale-independent pixels)",
                "http://code.google.com/android/reference/available-resources.html#dimension",  //$NON-NLS-1$
                new AttributeDescriptor[] {
                        new TextAttributeDescriptor(ATTR_NAME,
                                null /* nsUri */,
                                nameAttrInfo)
                        .setTooltip("The mandatory name used in referring to this dimension."),
                        new TextValueDescriptor(
                                "Value*",
                                "A mandatory dimension value is a number followed by a unit of measurement. For example: 10px, 2in, 5sp.")
                },
                null,  // no child nodes
                false /* not mandatory */);

         ElementDescriptor style_element = new ElementDescriptor(
                TAG_STYLE,
                "Style/Theme",
                "Both @styles and themes@ are defined in a style block containing one or more string or numerical values (typically color values), or references to other resources (drawables and so on).",
                "http://code.google.com/android/reference/available-resources.html#stylesandthemes",  //$NON-NLS-1$
                new AttributeDescriptor[] {
                        new TextAttributeDescriptor(ATTR_NAME,
                                null /* nsUri */,
                                nameAttrInfo)
                        .setTooltip("The mandatory name used in referring to this theme."),
                        new TextAttributeDescriptor("parent", //$NON-NLS-1$
                                null /* nsUri */,
                                new AttributeInfo("parent",  //$NON-NLS-1$
                                        Format.STRING_SET))
                        .setTooltip("An optional parent theme. All values from the specified theme will be inherited into this theme. Any values with identical names that you specify will override inherited values."),
                },
                new ElementDescriptor[] {
                    new ElementDescriptor(
                        TAG_ITEM,
                        "Item",
                        "A value to use in this @theme@. It can be a standard string, a hex color value, or a reference to any other resource type.",
                        "http://code.google.com/android/reference/available-resources.html#stylesandthemes",  //$NON-NLS-1$
                        new AttributeDescriptor[] {
                            new TextAttributeDescriptor(ATTR_NAME,
                                null /* nsUri */,
                                nameAttrInfo)
                            .setTooltip("The mandatory name used in referring to this item."),
                            new TextValueDescriptor(
                                "Value*",
                                "A mandatory standard string, hex color value, or reference to any other resource type.")
                        },
                        null,  // no child nodes
                        false /* not mandatory */)
                },
                false /* not mandatory */);

         ElementDescriptor string_array_element = new ElementDescriptor(
                 TAG_STRING_ARRAY,
                 "String Array",
                 "An array of strings. Strings are added as underlying item elements to the array.",
                 null, // tooltips
                 new AttributeDescriptor[] {
                         new TextAttributeDescriptor(ATTR_NAME,
                                 null /* nsUri */,
                                 nameAttrInfo)
                         .setTooltip("The mandatory name used in referring to this string array."),
                 },
                 new ElementDescriptor[] {
                     new ElementDescriptor(
                         TAG_ITEM,
                         "Item",
                         "A string value to use in this string array.",
                         null, // tooltip
                         new AttributeDescriptor[] {
                             new TextValueDescriptor(
                                 "Value*",
                                 "A mandatory string.")
                         },
                         null,  // no child nodes
                         false /* not mandatory */)
                 },
                 false /* not mandatory */);

         ElementDescriptor plurals_element = new ElementDescriptor(
                 TAG_PLURALS,
                 "Quantity Strings (Plurals)",
                 "A quantity string",
                 null, // tooltips
                 new AttributeDescriptor[] {
                         new TextAttributeDescriptor(ATTR_NAME,
                                 null /* nsUri */,
                                 nameAttrInfo)
                         .setTooltip("A name for the pair of strings. This name will be used as the resource ID."),
                 },
                 new ElementDescriptor[] {
                     new ElementDescriptor(
                         TAG_ITEM,
                         "Item",
                         "A plural or singular string",
                         null, // tooltip
                         new AttributeDescriptor[] {
                             new EnumAttributeDescriptor(
                                 "quantity", "Quantity", null,
                                 "A keyword value indicating when this string should be used",
                                 new AttributeInfo("quantity", Format.ENUM_SET)
                                 .setEnumValues(new String[] {
                                         "zero", //$NON-NLS-1$
                                         "one",  //$NON-NLS-1$
                                         "two",  //$NON-NLS-1$
                                         "few",  //$NON-NLS-1$
                                         "many", //$NON-NLS-1$
                                         "other" //$NON-NLS-1$
                                 }))
                         },
                         null,  // no child nodes
                         false /* not mandatory */)
                 },
                 false /* not mandatory */);

         ElementDescriptor integer_array_element = new ElementDescriptor(
                 TAG_INTEGER_ARRAY,
                 "Integer Array",
                 "An array of integers. Integers are added as underlying item elements to the array.",
                 null, // tooltips
                 new AttributeDescriptor[] {
                         new TextAttributeDescriptor(ATTR_NAME,
                                 null /* nsUri */,
                                 nameAttrInfo)
                         .setTooltip("The mandatory name used in referring to this integer array.")
                 },
                 new ElementDescriptor[] {
                     new ElementDescriptor(
                         TAG_ITEM,
                         "Item",
                         "An integer value to use in this integer array.",
                         null, // tooltip
                         new AttributeDescriptor[] {
                             new TextValueDescriptor(
                                 "Value*",
                                 "A mandatory integer.")
                         },
                         null,  // no child nodes
                         false /* not mandatory */)
                 },
                 false /* not mandatory */);

         mResourcesElement = new ElementDescriptor(
                        TAG_RESOURCES,
                        "Resources",
                        null,
                        "http://code.google.com/android/reference/available-resources.html",  //$NON-NLS-1$
                        null,  // no attributes
                        new ElementDescriptor[] {
                                string_element,
                                color_element,
                                dimen_element,
                                drawable_element,
                                style_element,
                                item_element,
                                string_array_element,
                                integer_array_element,
                                plurals_element,
                        },
                        true /* mandatory */);
    }
}

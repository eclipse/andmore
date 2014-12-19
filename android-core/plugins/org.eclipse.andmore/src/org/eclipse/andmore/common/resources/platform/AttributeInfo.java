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

package org.eclipse.andmore.common.resources.platform;

import static com.android.SdkConstants.ANDROID_PREFIX;
import static com.android.SdkConstants.ANDROID_THEME_PREFIX;
import static com.android.SdkConstants.ID_PREFIX;
import static com.android.SdkConstants.NEW_ID_PREFIX;
import static com.android.SdkConstants.PREFIX_THEME_REF;
import static com.android.SdkConstants.VALUE_FALSE;
import static com.android.SdkConstants.VALUE_TRUE;
import static com.android.ide.common.api.IAttributeInfo.Format.BOOLEAN;
import static com.android.ide.common.api.IAttributeInfo.Format.COLOR;
import static com.android.ide.common.api.IAttributeInfo.Format.DIMENSION;
import static com.android.ide.common.api.IAttributeInfo.Format.ENUM;
import static com.android.ide.common.api.IAttributeInfo.Format.FLAG;
import static com.android.ide.common.api.IAttributeInfo.Format.FLOAT;
import static com.android.ide.common.api.IAttributeInfo.Format.FRACTION;
import static com.android.ide.common.api.IAttributeInfo.Format.INTEGER;
import static com.android.ide.common.api.IAttributeInfo.Format.STRING;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.api.IAttributeInfo;
import com.android.ide.common.resources.ResourceRepository;
import com.android.resources.ResourceType;
import com.google.common.base.Splitter;

import java.util.EnumSet;
import java.util.regex.Pattern;


/**
 * Information about an attribute as gathered from the attrs.xml file where
 * the attribute was declared. This must include a format (string, reference, float, etc.),
 * possible flag or enum values, whether it's deprecated and its javadoc.
 */
public class AttributeInfo implements IAttributeInfo {
    /** XML Name of the attribute */
    private String mName;

    /** Formats of the attribute. Cannot be null. Should have at least one format. */
    private EnumSet<Format> mFormats;
    /** Values for enum. null for other types. */
    private String[] mEnumValues;
    /** Values for flag. null for other types. */
    private String[] mFlagValues;
    /** Short javadoc (i.e. the first sentence). */
    private String mJavaDoc;
    /** Documentation for deprecated attributes. Null if not deprecated. */
    private String mDeprecatedDoc;
    /** The source class defining this attribute */
    private String mDefinedBy;

    /**
     * @param name The XML Name of the attribute
     * @param formats The formats of the attribute. Cannot be null.
     *                Should have at least one format.
     */
    public AttributeInfo(@NonNull String name, @NonNull EnumSet<Format> formats) {
        mName = name;
        mFormats = formats;
    }

    /**
     * @param name The XML Name of the attribute
     * @param formats The formats of the attribute. Cannot be null.
     *                Should have at least one format.
     * @param javadoc Short javadoc (i.e. the first sentence).
     */
    public AttributeInfo(@NonNull String name, @NonNull EnumSet<Format> formats, String javadoc) {
        mName = name;
        mFormats = formats;
        mJavaDoc = javadoc;
    }

    public AttributeInfo(AttributeInfo info) {
        mName = info.mName;
        mFormats = info.mFormats;
        mEnumValues = info.mEnumValues;
        mFlagValues = info.mFlagValues;
        mJavaDoc = info.mJavaDoc;
        mDeprecatedDoc = info.mDeprecatedDoc;
    }

    /**
     * Sets the XML Name of the attribute
     *
     * @param name the new name to assign
     */
    public void setName(String name) {
        mName = name;
    }

    /** Returns the XML Name of the attribute */
    @Override
    public @NonNull String getName() {
        return mName;
    }
    /** Returns the formats of the attribute. Cannot be null.
     *  Should have at least one format. */
    @Override
    public @NonNull EnumSet<Format> getFormats() {
        return mFormats;
    }
    /** Returns the values for enums. null for other types. */
    @Override
    public String[] getEnumValues() {
        return mEnumValues;
    }
    /** Returns the values for flags. null for other types. */
    @Override
    public String[] getFlagValues() {
        return mFlagValues;
    }
    /** Returns a short javadoc, .i.e. the first sentence. */
    @Override
    public @NonNull String getJavaDoc() {
        return mJavaDoc;
    }
    /** Returns the documentation for deprecated attributes. Null if not deprecated. */
    @Override
    public String getDeprecatedDoc() {
        return mDeprecatedDoc;
    }

    /** Sets the values for enums. null for other types. */
    public AttributeInfo setEnumValues(String[] values) {
        mEnumValues = values;
        return this;
    }

    /** Sets the values for flags. null for other types. */
    public AttributeInfo setFlagValues(String[] values) {
        mFlagValues = values;
        return this;
    }

    /** Sets a short javadoc, .i.e. the first sentence. */
    public void setJavaDoc(String javaDoc) {
        mJavaDoc = javaDoc;
    }

    /** Sets the documentation for deprecated attributes. Null if not deprecated. */
    public void setDeprecatedDoc(String deprecatedDoc) {
        mDeprecatedDoc = deprecatedDoc;
    }

    /**
     * Sets the name of the class (fully qualified class name) which defined
     * this attribute
     *
     * @param definedBy the name of the class (fully qualified class name) which
     *            defined this attribute
     */
    public void setDefinedBy(String definedBy) {
        mDefinedBy = definedBy;
    }

    /**
     * Returns the name of the class (fully qualified class name) which defined
     * this attribute
     *
     * @return the name of the class (fully qualified class name) which defined
     *         this attribute
     */
    @Override
    public @NonNull String getDefinedBy() {
        return mDefinedBy;
    }

    private final static Pattern INTEGER_PATTERN = Pattern.compile("-?[0-9]+"); //$NON-NLS-1$
    private final static Pattern FLOAT_PATTERN =
            Pattern.compile("-?[0-9]?(\\.[0-9]+)?"); //$NON-NLS-1$
    private final static Pattern DIMENSION_PATTERN =
            Pattern.compile("-?[0-9]+(\\.[0-9]+)?(dp|dip|sp|px|pt|in|mm)"); //$NON-NLS-1$

    /**
     * Checks the given value and returns true only if it is a valid XML value
     * for this attribute.
     *
     * @param value the XML value to check
     * @param projectResources project resources to validate resource URLs with,
     *            if any
     * @param frameworkResources framework resources to validate resource URLs
     *            with, if any
     * @return true if the value is valid, false otherwise
     */
    public boolean isValid(
            @NonNull String value,
            @Nullable ResourceRepository projectResources,
            @Nullable ResourceRepository frameworkResources) {

        if (mFormats.contains(STRING) || mFormats.isEmpty()) {
            // Anything is allowed
            return true;
        }

        // All other formats require a nonempty string
        if (value.isEmpty()) {
            // Except for flags
            if (mFormats.contains(FLAG)) {
                return true;
            }

            return false;
        }
        char first = value.charAt(0);

        // There are many attributes which are incorrectly marked in the attrs.xml
        // file, such as "duration", "minHeight", etc. These are marked as only
        // accepting "integer", but also appear to accept "reference". Therefore,
        // in these cases, be more lenient. (This happens for theme references too,
        // such as ?android:attr/listPreferredItemHeight)
        if ((first == '@' || first == '?') /* && mFormats.contains(REFERENCE)*/) {
            if (value.equals("@null")) {
                return true;
            }

            if (value.startsWith(NEW_ID_PREFIX) || value.startsWith(ID_PREFIX)) {
                // These are handled in the IdGeneratingResourceFile; we shouldn't
                // complain about not finding ids in the repository yet since they may
                // not yet have been defined (@+id's can be defined in the same layout,
                // later on.)
                return true;
            }

            if (value.startsWith(ANDROID_PREFIX) || value.startsWith(ANDROID_THEME_PREFIX)) {
                if (frameworkResources != null) {
                    return frameworkResources.hasResourceItem(value);
                }
            } else if (projectResources != null) {
                return projectResources.hasResourceItem(value);
            }

            // Validate resource string
            String url = value;
            int typeEnd = url.indexOf('/', 1);
            if (typeEnd != -1) {
                int typeBegin = url.startsWith("@+") ? 2 : 1; //$NON-NLS-1$
                int colon = url.lastIndexOf(':', typeEnd);
                if (colon != -1) {
                    typeBegin = colon + 1;
                }
                String typeName = url.substring(typeBegin, typeEnd);
                ResourceType type = ResourceType.getEnum(typeName);
                if (type != null) {
                    // TODO: Validate that the name portion conforms to the rules
                    // (is an identifier but not a keyword, etc.)
                    // Also validate that the prefix before the colon is either
                    // not there or is "android"

                    //int nameBegin = typeEnd + 1;
                    //String name = url.substring(nameBegin);
                    return true;
                }
            } else if (value.startsWith(PREFIX_THEME_REF)) {
                if (projectResources != null) {
                    return projectResources.hasResourceItem(ResourceType.ATTR,
                            value.substring(PREFIX_THEME_REF.length()));
                } else {
                    // Until proven otherwise
                    return true;
                }
            }
        }

        if (mFormats.contains(ENUM) && mEnumValues != null) {
            for (String e : mEnumValues) {
                if (value.equals(e)) {
                    return true;
                }
            }
        }

        if (mFormats.contains(FLAG) && mFlagValues != null) {
            for (String v : Splitter.on('|').split(value)) {
                for (String e : mFlagValues) {
                    if (v.equals(e)) {
                        return true;
                    }
                }
            }
        }

        if (mFormats.contains(DIMENSION)) {
            if (DIMENSION_PATTERN.matcher(value).matches()) {
                return true;
            }
        }

        if (mFormats.contains(BOOLEAN)) {
            if (value.equalsIgnoreCase(VALUE_TRUE) || value.equalsIgnoreCase(VALUE_FALSE)) {
                return true;
            }
        }

        if (mFormats.contains(FLOAT)) {
            if (Character.isDigit(first) || first == '-' || first == '.') {
                if (FLOAT_PATTERN.matcher(value).matches()) {
                    return true;
                }
                // AAPT accepts more general floats, such as ".1",
                try {
                    Float.parseFloat(value);
                    return true;
                } catch (NumberFormatException nufe) {
                    // Not a float
                }
            }
        }

        if (mFormats.contains(INTEGER)) {
            if (Character.isDigit(first) || first == '-') {
                if (INTEGER_PATTERN.matcher(value).matches()) {
                    return true;
                }
            }
        }

        if (mFormats.contains(COLOR)) {
            if (first == '#' && value.length() <= 9) { // Only allowed 32 bit ARGB
                try {
                    // Use Long.parseLong rather than Integer.parseInt to not overflow on
                    // 32 big hex values like "ff191919"
                    Long.parseLong(value.substring(1), 16);
                    return true;
                } catch (NumberFormatException nufe) {
                    // Not a valid color number
                }
            }
        }

        if (mFormats.contains(FRACTION)) {
            // should end with % or %p
            return true;
        }

        return false;
    }
}

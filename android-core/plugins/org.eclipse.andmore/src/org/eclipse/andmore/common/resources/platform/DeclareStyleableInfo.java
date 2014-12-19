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


/**
 * Information needed to represent a View or ViewGroup (aka Layout) item
 * in the layout hierarchy, as extracted from the main android.jar and the
 * associated attrs.xml.
 */
public class DeclareStyleableInfo {
    /** The style name, never null. */
    private final String mStyleName;
    /** Attributes for this view or view group. Can be empty but never null. */
    private final AttributeInfo[] mAttributes;
    /** Short javadoc. Can be null. */
    private String mJavaDoc;
    /** Optional name of the parents styleable. Can be null. */
    private String[] mParents;

    /**
     * Creates a new {@link DeclareStyleableInfo}.
     *
     * @param styleName The name of the style. Should not be empty nor null.
     * @param attributes The initial list of attributes. Can be null.
     */
    public DeclareStyleableInfo(String styleName, AttributeInfo[] attributes) {
        mStyleName = styleName;
        mAttributes = attributes == null ? new AttributeInfo[0] : attributes;
    }

    /**
     * Creates a new {@link DeclareStyleableInfo} that has the same attributes
     * as an existing one and only differs by name.
     *
     * @param styleName The name of the style. Should not be empty nor null.
     * @param existing The existing {@link DeclareStyleableInfo} to mirror.
     */
    public DeclareStyleableInfo(String styleName, DeclareStyleableInfo existing) {
        mStyleName = styleName;

        mJavaDoc = existing.getJavaDoc();

        String[] parents = existing.getParents();
        if (parents != null) {
            mParents = new String[parents.length];
            System.arraycopy(parents, 0, mParents, 0, parents.length);
        }

        AttributeInfo[] attrs = existing.getAttributes();
        if (attrs == null || attrs.length == 0) {
            mAttributes = new AttributeInfo[0];
        } else {
            mAttributes = new AttributeInfo[attrs.length];
            System.arraycopy(attrs, 0, mAttributes, 0, attrs.length);
        }
    }

    /** Returns style name */
    public String getStyleName() {
        return mStyleName;
    }

    /** Returns the attributes for this view or view group. Maybe empty but not null. */
    public AttributeInfo[] getAttributes() {
        return mAttributes;
    }

    /** Returns a short javadoc */
    public String getJavaDoc() {
        return mJavaDoc;
    }

    /** Sets the javadoc. */
    public void setJavaDoc(String javaDoc) {
        mJavaDoc = javaDoc;
    }

    /** Sets the name of the parents styleable. Can be null. */
    public void setParents(String[] parents) {
        mParents = parents;
    }

    /** Returns the name of the parents styleable. Can be null. */
    public String[] getParents() {
        return mParents;
    }
}

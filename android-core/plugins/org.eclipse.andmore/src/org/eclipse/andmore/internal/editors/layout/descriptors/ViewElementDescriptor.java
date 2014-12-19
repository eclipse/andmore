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

package org.eclipse.andmore.internal.editors.layout.descriptors;

import static com.android.SdkConstants.ANDROID_VIEW_PKG;
import static com.android.SdkConstants.ANDROID_WEBKIT_PKG;
import static com.android.SdkConstants.ANDROID_WIDGET_PREFIX;
import static com.android.SdkConstants.VIEW;
import static com.android.SdkConstants.VIEW_TAG;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.common.resources.platform.AttributeInfo;
import org.eclipse.andmore.internal.editors.IconFactory;
import org.eclipse.andmore.internal.editors.descriptors.AttributeDescriptor;
import org.eclipse.andmore.internal.editors.descriptors.ElementDescriptor;
import org.eclipse.andmore.internal.editors.layout.uimodel.UiViewElementNode;
import org.eclipse.andmore.internal.editors.uimodel.UiElementNode;
import org.eclipse.swt.graphics.Image;

import java.util.Collections;
import java.util.List;

/**
 * {@link ViewElementDescriptor} describes the properties expected for a given XML element node
 * representing a class in an XML Layout file.
 * <p/>
 * These descriptors describe Android views XML elements.
 * <p/>
 * The base class {@link ElementDescriptor} has a notion of "children", that is an XML element
 * can produce another set of XML elements. Because of the flat nature of Android's layout
 * XML files all possible views are children of the document and of themselves (that is any
 * view group can contain any other view). This is an implied contract of this class that is
 * enforces at construction by {@link LayoutDescriptors}. Note that by construction any code
 * that deals with the children hierarchy must also deal with potential infinite loops since views
 * <em>will</em> reference themselves (e.g. a ViewGroup can contain a ViewGroup).
 * <p/>
 * Since Views are also Java classes, they derive from each other. Here this is represented
 * as the "super class", which denotes the fact that a given View java class derives from
 * another class. These properties are also set at construction by {@link LayoutDescriptors}.
 * The super class hierarchy is very different from the descriptor's children hierarchy: the
 * later represents Java inheritance, the former represents an XML nesting capability.
 *
 * @see ElementDescriptor
 */
public class ViewElementDescriptor extends ElementDescriptor {

    /** The full class name (FQCN) of this view. */
    private final String mFullClassName;

    /** The list of layout attributes. Can be empty but not null. */
    private AttributeDescriptor[] mLayoutAttributes;

    /** The super-class descriptor. Can be null. */
    private ViewElementDescriptor mSuperClassDesc;

    /** List of attribute sources, classes that contribute attributes to {@link #mAttributes} */
    private List<String> mAttributeSources;

    /**
     * Constructs a new {@link ViewElementDescriptor} based on its XML name, UI name,
     * the canonical name of the class it represents, its tooltip, its SDK url, its attributes list,
     * its children list and its mandatory flag.
     *
     * @param xml_name The XML element node name. Case sensitive.
     * @param ui_name The XML element name for the user interface, typically capitalized.
     * @param fullClassName The fully qualified class name the {@link ViewElementDescriptor} is
     *          representing.
     * @param tooltip An optional tooltip. Can be null or empty.
     * @param sdk_url An optional SKD URL. Can be null or empty.
     * @param attributes The list of allowed attributes. Can be null or empty.
     * @param layoutAttributes The list of layout attributes. Can be null or empty.
     * @param children The list of allowed children. Can be null or empty.
     * @param mandatory Whether this node must always exist (even for empty models). A mandatory
     *  UI node is never deleted and it may lack an actual XML node attached. A non-mandatory
     *  UI node MUST have an XML node attached and it will cease to exist when the XML node
     *  ceases to exist.
     */
    public ViewElementDescriptor(String xml_name, String ui_name,
            String fullClassName,
            String tooltip, String sdk_url,
            AttributeDescriptor[] attributes, AttributeDescriptor[] layoutAttributes,
            ElementDescriptor[] children, boolean mandatory) {
        super(xml_name, ui_name, tooltip, sdk_url, attributes, children, mandatory);
        mFullClassName = fullClassName;
        mLayoutAttributes = layoutAttributes != null ? layoutAttributes : new AttributeDescriptor[0];
    }

    /**
     * Constructs a new {@link ElementDescriptor} based on its XML name and on the canonical
     * name of the class it represents.
     * The UI name is build by capitalizing the XML name.
     * The UI nodes will be non-mandatory.
     *
     * @param xml_name The XML element node name. Case sensitive.
     * @param fullClassName The fully qualified class name the {@link ViewElementDescriptor} is
     * representing.
     */
    public ViewElementDescriptor(String xml_name, String fullClassName) {
        super(xml_name);
        mFullClassName = fullClassName;
        mLayoutAttributes = null;
    }

    /**
     * Returns the fully qualified name of the View class represented by this element descriptor
     * e.g. "android.view.View".
     *
     * @return the fully qualified class name, never null
     */
    public String getFullClassName() {
        return mFullClassName;
    }

    /** Returns the list of layout attributes. Can be empty but not null.
     *
     * @return the list of layout attributes, never null
     */
    public AttributeDescriptor[] getLayoutAttributes() {
        return mLayoutAttributes;
    }

    /**
     * Sets the list of layout attribute attributes.
     *
     * @param attributes the new layout attributes, not null
     */
    public void setLayoutAttributes(AttributeDescriptor[] attributes) {
        assert attributes != null;
        mLayoutAttributes = attributes;
    }

    /**
     * Returns a new {@link UiViewElementNode} linked to this descriptor.
     */
    @Override
    public UiElementNode createUiNode() {
        return new UiViewElementNode(this);
    }

    /**
     * Returns the {@link ViewElementDescriptor} of the super-class of this View descriptor
     * that matches the java View hierarchy. Can be null.
     *
     * @return the super class' descriptor or null
     */
    public ViewElementDescriptor getSuperClassDesc() {
        return mSuperClassDesc;
    }

    /**
     * Sets the {@link ViewElementDescriptor} of the super-class of this View descriptor
     * that matches the java View hierarchy. Can be null.
     *
     * @param superClassDesc the descriptor for the super class, or null
     */
    public void setSuperClass(ViewElementDescriptor superClassDesc) {
        mSuperClassDesc = superClassDesc;
    }

    /**
     * Returns an optional icon for the element.
     * <p/>
     * By default this tries to return an icon based on the XML name of the element.
     * If this fails, it tries to return the default element icon as defined in the
     * plugin. If all fails, it returns null.
     *
     * @return An icon for this element or null.
     */
    @Override
    public Image getGenericIcon() {
        IconFactory factory = IconFactory.getInstance();
        String name = mXmlName;
        if (name.indexOf('.') != -1) {
            // If the user uses a fully qualified name, such as
            // "android.gesture.GestureOverlayView" in their XML, we need to look up
            // only by basename
            name = name.substring(name.lastIndexOf('.') + 1);
        } else if (VIEW_TAG.equals(name)) {
            // Can't have both view.png and View.png; issues on case sensitive vs
            // case insensitive file systems
            name = VIEW;
        }

        Image icon = factory.getIcon(name);
        if (icon == null) {
            icon = AdtPlugin.getAndroidLogo();
        }

        return icon;
    }

    /**
     * Returns the list of attribute sources for the attributes provided by this
     * descriptor. An attribute source is the fully qualified class name of the
     * defining class for some of the properties. The specific attribute source
     * of a given {@link AttributeInfo} can be found by calling
     * {@link AttributeInfo#getDefinedBy()}.
     * <p>
     * The attribute sources are ordered from class to super class.
     * <p>
     * The list may <b>not</b> be modified by clients.
     *
     * @return a non null list of attribute sources for this view
     */
    public List<String> getAttributeSources() {
        return mAttributeSources != null ? mAttributeSources : Collections.<String>emptyList();
    }

    /**
     * Sets the attribute sources for this view. See {@link #getAttributes()}
     * for details.
     *
     * @param attributeSources a non null list of attribute sources for this
     *            view descriptor
     * @see #getAttributeSources()
     */
    public void setAttributeSources(List<String> attributeSources) {
        mAttributeSources = attributeSources;
    }

    /**
     * Returns true if views with the given fully qualified class name need to include
     * their package in the layout XML tag
     *
     * @param fqcn the fully qualified class name, such as android.widget.Button
     * @return true if the full package path should be included in the layout XML element
     *         tag
     */
    public static boolean viewNeedsPackage(String fqcn) {
        return !(fqcn.startsWith(ANDROID_WIDGET_PREFIX)
              || fqcn.startsWith(ANDROID_VIEW_PKG)
              || fqcn.startsWith(ANDROID_WEBKIT_PKG));
    }
}

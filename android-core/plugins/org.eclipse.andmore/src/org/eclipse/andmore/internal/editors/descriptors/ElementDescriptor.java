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

package org.eclipse.andmore.internal.editors.descriptors;

import static com.android.SdkConstants.ANDROID_NS_NAME_PREFIX;
import static com.android.SdkConstants.ANDROID_URI;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.editors.IconFactory;
import org.eclipse.andmore.internal.editors.uimodel.UiElementNode;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * {@link ElementDescriptor} describes the properties expected for a given XML element node.
 *
 * {@link ElementDescriptor} have an XML name, UI name, a tooltip, an SDK url,
 * an attributes list and a children list.
 *
 * An UI node can be "mandatory", meaning the UI node is never deleted and it may lack
 * an actual XML node attached. A non-mandatory UI node MUST have an XML node attached
 * and it will cease to exist when the XML node ceases to exist.
 */
public class ElementDescriptor implements Comparable<ElementDescriptor> {
    private static final String ELEMENT_ICON_FILENAME = "element"; //$NON-NLS-1$

    /** The XML element node name. Case sensitive. */
    protected final String mXmlName;
    /** The XML element name for the user interface, typically capitalized. */
    private final String mUiName;
    /** The list of allowed attributes. */
    private AttributeDescriptor[] mAttributes;
    /** The list of allowed children */
    private ElementDescriptor[] mChildren;
    /* An optional tooltip. Can be empty. */
    private String mTooltip;
    /** An optional SKD URL. Can be empty. */
    private String mSdkUrl;
    /** Whether this UI node must always exist (even for empty models). */
    private final Mandatory mMandatory;

    public enum Mandatory {
        NOT_MANDATORY,
        MANDATORY,
        MANDATORY_LAST
    }

    /**
     * Constructs a new {@link ElementDescriptor} based on its XML name, UI name,
     * tooltip, SDK url, attributes list, children list and mandatory.
     *
     * @param xml_name The XML element node name. Case sensitive.
     * @param ui_name The XML element name for the user interface, typically capitalized.
     * @param tooltip An optional tooltip. Can be null or empty.
     * @param sdk_url An optional SKD URL. Can be null or empty.
     * @param attributes The list of allowed attributes. Can be null or empty.
     * @param children The list of allowed children. Can be null or empty.
     * @param mandatory Whether this node must always exist (even for empty models). A mandatory
     *  UI node is never deleted and it may lack an actual XML node attached. A non-mandatory
     *  UI node MUST have an XML node attached and it will cease to exist when the XML node
     *  ceases to exist.
     */
    public ElementDescriptor(String xml_name, String ui_name, String tooltip, String sdk_url,
            AttributeDescriptor[] attributes,
            ElementDescriptor[] children,
            Mandatory mandatory) {
        mMandatory = mandatory;
        mXmlName = xml_name;
        mUiName = ui_name;
        mTooltip = (tooltip != null && tooltip.length() > 0) ? tooltip : null;
        mSdkUrl = (sdk_url != null && sdk_url.length() > 0) ? sdk_url : null;
        setAttributes(attributes != null ? attributes : new AttributeDescriptor[]{});
        mChildren = children != null ? children : new ElementDescriptor[]{};
    }

    /**
     * Constructs a new {@link ElementDescriptor} based on its XML name, UI name,
     * tooltip, SDK url, attributes list, children list and mandatory.
     *
     * @param xml_name The XML element node name. Case sensitive.
     * @param ui_name The XML element name for the user interface, typically capitalized.
     * @param tooltip An optional tooltip. Can be null or empty.
     * @param sdk_url An optional SKD URL. Can be null or empty.
     * @param attributes The list of allowed attributes. Can be null or empty.
     * @param children The list of allowed children. Can be null or empty.
     * @param mandatory Whether this node must always exist (even for empty models). A mandatory
     *  UI node is never deleted and it may lack an actual XML node attached. A non-mandatory
     *  UI node MUST have an XML node attached and it will cease to exist when the XML node
     *  ceases to exist.
     */
    public ElementDescriptor(String xml_name, String ui_name, String tooltip, String sdk_url,
            AttributeDescriptor[] attributes,
            ElementDescriptor[] children,
            boolean mandatory) {
        mMandatory = mandatory ? Mandatory.MANDATORY : Mandatory.NOT_MANDATORY;
        mXmlName = xml_name;
        mUiName = ui_name;
        mTooltip = (tooltip != null && tooltip.length() > 0) ? tooltip : null;
        mSdkUrl = (sdk_url != null && sdk_url.length() > 0) ? sdk_url : null;
        setAttributes(attributes != null ? attributes : new AttributeDescriptor[]{});
        mChildren = children != null ? children : new ElementDescriptor[]{};
    }

    /**
     * Constructs a new {@link ElementDescriptor} based on its XML name and children list.
     * The UI name is build by capitalizing the XML name.
     * The UI nodes will be non-mandatory.
     *
     * @param xml_name The XML element node name. Case sensitive.
     * @param children The list of allowed children. Can be null or empty.
     * @param mandatory Whether this node must always exist (even for empty models). A mandatory
     *  UI node is never deleted and it may lack an actual XML node attached. A non-mandatory
     *  UI node MUST have an XML node attached and it will cease to exist when the XML node
     *  ceases to exist.
     */
    public ElementDescriptor(String xml_name, ElementDescriptor[] children, Mandatory mandatory) {
        this(xml_name, prettyName(xml_name), null, null, null, children, mandatory);
    }

    /**
     * Constructs a new {@link ElementDescriptor} based on its XML name and children list.
     * The UI name is build by capitalizing the XML name.
     * The UI nodes will be non-mandatory.
     *
     * @param xml_name The XML element node name. Case sensitive.
     * @param children The list of allowed children. Can be null or empty.
     */
    public ElementDescriptor(String xml_name, ElementDescriptor[] children) {
        this(xml_name, prettyName(xml_name), null, null, null, children, false);
    }

    /**
     * Constructs a new {@link ElementDescriptor} based on its XML name.
     * The UI name is build by capitalizing the XML name.
     * The UI nodes will be non-mandatory.
     *
     * @param xml_name The XML element node name. Case sensitive.
     */
    public ElementDescriptor(String xml_name) {
        this(xml_name, prettyName(xml_name), null, null, null, null, false);
    }

    /** Returns whether this node must always exist (even for empty models) */
    public Mandatory getMandatory() {
        return mMandatory;
    }

    @Override
    public String toString() {
        return String.format("%s [%s, attr %d, children %d%s]",    //$NON-NLS-1$
                this.getClass().getSimpleName(),
                mXmlName,
                mAttributes != null ? mAttributes.length : 0,
                mChildren != null ? mChildren.length : 0,
                mMandatory != Mandatory.NOT_MANDATORY ? ", " + mMandatory.toString() : "" //$NON-NLS-1$ //$NON-NLS-2$
                );
    }

    /**
     * Returns the XML element node local name (case sensitive)
     */
    public final String getXmlLocalName() {
        int pos = mXmlName.indexOf(':');
        if (pos != -1) {
            return mXmlName.substring(pos+1);
        }
        return mXmlName;
    }

    /**
     * Returns the XML element node name, including the prefix.
     * Case sensitive.
     * <p/>
     * In Android resources, the element node name for Android resources typically does not
     * have a prefix and is typically the simple Java class name (e.g. "View"), whereas for
     * custom views it is generally the fully qualified class name of the view (e.g.
     * "com.mycompany.myapp.MyView").
     * <p/>
     * Most of the time you'll probably want to use {@link #getXmlLocalName()} to get a local
     * name guaranteed without a prefix.
     * <p/>
     * Note that the prefix that <em>may</em> be available in this descriptor has nothing to
     * do with the actual prefix the node might have (or needs to have) in the actual XML file
     * since descriptors are fixed and do not depend on any current namespace defined in the
     * target XML.
     */
    public String getXmlName() {
        return mXmlName;
    }

    /**
     * Returns the namespace of the attribute.
     */
    public final String getNamespace() {
        // For now we hard-code the prefix as being "android"
        if (mXmlName.startsWith(ANDROID_NS_NAME_PREFIX)) {
            return ANDROID_URI;
        }

        return ""; //$NON-NLs-1$
    }


    /** Returns the XML element name for the user interface, typically capitalized. */
    public String getUiName() {
        return mUiName;
    }

    /**
     * Returns an icon for the element.
     * This icon is generic, that is all element descriptors have the same icon
     * no matter what they represent.
     *
     * @return An icon for this element or null.
     * @see #getCustomizedIcon()
     */
    public Image getGenericIcon() {
        return IconFactory.getInstance().getIcon(ELEMENT_ICON_FILENAME);
    }

    /**
     * Returns an optional icon for the element, typically to be used in XML form trees.
     * <p/>
     * This icon is customized to the given descriptor, that is different elements
     * will have different icons.
     * <p/>
     * By default this tries to return an icon based on the XML name of the element.
     * If this fails, it tries to return the default Android logo as defined in the
     * plugin. If all fails, it returns null.
     *
     * @return An icon for this element. This is never null.
     */
    public Image getCustomizedIcon() {
        IconFactory factory = IconFactory.getInstance();
        int color = hasChildren() ? IconFactory.COLOR_BLUE
                : IconFactory.COLOR_GREEN;
        int shape = hasChildren() ? IconFactory.SHAPE_RECT
                : IconFactory.SHAPE_CIRCLE;
        String name = mXmlName;

        int pos = name.lastIndexOf('.');
        if (pos != -1) {
            // If the user uses a fully qualified name, such as
            // "android.gesture.GestureOverlayView" in their XML, we need to
            // look up only by basename
            name = name.substring(pos + 1);
        }
        Image icon = factory.getIcon(name, color, shape);
        if (icon == null) {
            icon = getGenericIcon();
        }
        if (icon == null) {
            icon = AdtPlugin.getAndroidLogo();
        }
        return icon;
    }

    /**
     * Returns an optional ImageDescriptor for the element.
     * <p/>
     * By default this tries to return an image based on the XML name of the element.
     * If this fails, it tries to return the default Android logo as defined in the
     * plugin. If all fails, it returns null.
     *
     * @return An ImageDescriptor for this element or null.
     */
    public ImageDescriptor getImageDescriptor() {
        IconFactory factory = IconFactory.getInstance();
        int color = hasChildren() ? IconFactory.COLOR_BLUE : IconFactory.COLOR_GREEN;
        int shape = hasChildren() ? IconFactory.SHAPE_RECT : IconFactory.SHAPE_CIRCLE;
        ImageDescriptor id = factory.getImageDescriptor(mXmlName, color, shape);
        return id != null ? id : AdtPlugin.getAndroidLogoDesc();
    }

    /* Returns the list of allowed attributes. */
    public AttributeDescriptor[] getAttributes() {
        return mAttributes;
    }

    /** Sets the list of allowed attributes. */
    public void setAttributes(AttributeDescriptor[] attributes) {
        mAttributes = attributes;
        for (AttributeDescriptor attribute : attributes) {
            attribute.setParent(this);
        }
    }

    /** Returns the list of allowed children */
    public ElementDescriptor[] getChildren() {
        return mChildren;
    }

    /** @return True if this descriptor has children available */
    public boolean hasChildren() {
        return mChildren.length > 0;
    }

    /**
     * Checks whether this descriptor can accept the given descriptor type
     * as a direct child.
     *
     * @return True if this descriptor can accept children of the given descriptor type.
     *   False if not accepted, no children allowed, or target is null.
     */
    public boolean acceptChild(ElementDescriptor target) {
        if (target != null && mChildren.length > 0) {
            String targetXmlName = target.getXmlName();
            for (ElementDescriptor child : mChildren) {
                if (child.getXmlName().equals(targetXmlName)) {
                    return true;
                }
            }
        }

        return false;
    }

    /** Sets the list of allowed children. */
    public void setChildren(ElementDescriptor[] newChildren) {
        mChildren = newChildren;
    }

    /**
     * Sets the list of allowed children.
     * <p/>
     * This is just a convenience method that converts a Collection into an array and
     * calls {@link #setChildren(ElementDescriptor[])}.
     * <p/>
     * This means a <em>copy</em> of the collection is made. The collection is not
     * stored by the recipient and can thus be altered by the caller.
     */
    public void setChildren(Collection<ElementDescriptor> newChildren) {
        setChildren(newChildren.toArray(new ElementDescriptor[newChildren.size()]));
    }

    /**
     * Returns an optional tooltip. Will be null if not present.
     * <p/>
     * The tooltip is based on the Javadoc of the element and already processed via
     * {@link DescriptorsUtils#formatTooltip(String)} to be displayed right away as
     * a UI tooltip.
     */
    public String getTooltip() {
        return mTooltip;
    }

    /** Returns an optional SKD URL. Will be null if not present. */
    public String getSdkUrl() {
        return mSdkUrl;
    }

    /** Sets the optional tooltip. Can be null or empty. */
    public void setTooltip(String tooltip) {
        mTooltip = tooltip;
    }

    /** Sets the optional SDK URL. Can be null or empty. */
    public void setSdkUrl(String sdkUrl) {
        mSdkUrl = sdkUrl;
    }

    /**
     * @return A new {@link UiElementNode} linked to this descriptor.
     */
    public UiElementNode createUiNode() {
        return new UiElementNode(this);
    }

    /**
     * Returns the first children of this descriptor that describes the given XML element name.
     * <p/>
     * In recursive mode, searches the direct children first before descending in the hierarchy.
     *
     * @return The ElementDescriptor matching the requested XML node element name or null.
     */
    public ElementDescriptor findChildrenDescriptor(String element_name, boolean recursive) {
        return findChildrenDescriptorInternal(element_name, recursive, null);
    }

    private ElementDescriptor findChildrenDescriptorInternal(String element_name,
            boolean recursive,
            Set<ElementDescriptor> visited) {
        if (recursive && visited == null) {
            visited = new HashSet<ElementDescriptor>();
        }

        for (ElementDescriptor e : getChildren()) {
            if (e.getXmlName().equals(element_name)) {
                return e;
            }
        }

        if (visited != null) {
            visited.add(this);
        }

        if (recursive) {
            for (ElementDescriptor e : getChildren()) {
                if (visited != null) {
                    if (!visited.add(e)) {  // Set.add() returns false if element is already present
                        continue;
                    }
                }
                ElementDescriptor f = e.findChildrenDescriptorInternal(element_name,
                        recursive, visited);
                if (f != null) {
                    return f;
                }
            }
        }

        return null;
    }

    /**
     * Utility helper than pretty-formats an XML Name for the UI.
     * This is used by the simplified constructor that takes only an XML element name.
     *
     * @param xml_name The XML name to convert.
     * @return The XML name with dashes replaced by spaces and capitalized.
     */
    private static String prettyName(String xml_name) {
        char c[] = xml_name.toCharArray();
        if (c.length > 0) {
            c[0] = Character.toUpperCase(c[0]);
        }
        return new String(c).replace("-", " ");  //$NON-NLS-1$  //$NON-NLS-2$
    }

    /**
     * Returns true if this node defines the given attribute
     *
     * @param namespaceUri the namespace URI of the target attribute
     * @param attributeName the attribute name
     * @return true if this element defines an attribute of the given name and namespace
     */
    public boolean definesAttribute(String namespaceUri, String attributeName) {
        for (AttributeDescriptor desc : mAttributes) {
            if (desc.getXmlLocalName().equals(attributeName) &&
                    desc.getNamespaceUri().equals(namespaceUri)) {
                return true;
            }
        }

        return false;
    }

    // Implements Comparable<ElementDescriptor>:
    @Override
    public int compareTo(ElementDescriptor o) {
        return mUiName.compareToIgnoreCase(o.mUiName);
    }

    /**
     * Ensures that this view descriptor's attribute list is up to date. This is
     * always the case for all the builtin descriptors, but for example for a
     * custom view, it could be changing dynamically so caches may have to be
     * recomputed. This method will return true if nothing changed, and false if
     * it recomputed its info.
     *
     * @return true if the attributes are already up to date and nothing changed
     */
    public boolean syncAttributes() {
        return true;
    }
}

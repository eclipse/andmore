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

package org.eclipse.andmore.internal.editors.uimodel;

import static com.android.SdkConstants.ANDROID_PKG_PREFIX;
import static com.android.SdkConstants.ANDROID_SUPPORT_PKG_PREFIX;
import static com.android.SdkConstants.ATTR_CLASS;
import static com.android.SdkConstants.ID_PREFIX;
import static com.android.SdkConstants.NEW_ID_PREFIX;

import com.android.SdkConstants;
import com.android.annotations.VisibleForTesting;
import com.android.ide.common.api.IAttributeInfo.Format;
import com.android.ide.common.xml.XmlAttributeSortOrder;
import com.android.utils.SdkUtils;
import com.android.utils.XmlUtils;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.common.resources.platform.AttributeInfo;
import org.eclipse.andmore.internal.editors.AndroidXmlEditor;
import org.eclipse.andmore.internal.editors.descriptors.AttributeDescriptor;
import org.eclipse.andmore.internal.editors.descriptors.ElementDescriptor;
import org.eclipse.andmore.internal.editors.descriptors.IUnknownDescriptorProvider;
import org.eclipse.andmore.internal.editors.descriptors.SeparatorAttributeDescriptor;
import org.eclipse.andmore.internal.editors.descriptors.TextAttributeDescriptor;
import org.eclipse.andmore.internal.editors.descriptors.XmlnsAttributeDescriptor;
import org.eclipse.andmore.internal.editors.descriptors.ElementDescriptor.Mandatory;
import org.eclipse.andmore.internal.editors.layout.descriptors.CustomViewDescriptorService;
import org.eclipse.andmore.internal.editors.manifest.descriptors.AndroidManifestDescriptors;
import org.eclipse.andmore.internal.editors.otherxml.descriptors.OtherXmlDescriptors;
import org.eclipse.andmore.internal.editors.uimodel.IUiUpdateListener.UiUpdateState;
import org.eclipse.andmore.internal.preferences.AdtPrefs;
import org.eclipse.andmore.internal.sdk.AndroidTargetData;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.xml.core.internal.document.ElementImpl;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Represents an XML node that can be modified by the user interface in the XML editor.
 * <p/>
 * Each tree viewer used in the application page's parts needs to keep a model representing
 * each underlying node in the tree. This interface represents the base type for such a node.
 * <p/>
 * Each node acts as an intermediary model between the actual XML model (the real data support)
 * and the tree viewers or the corresponding page parts.
 * <p/>
 * Element nodes don't contain data per se. Their data is contained in their attributes
 * as well as their children's attributes, see {@link UiAttributeNode}.
 * <p/>
 * The structure of a given {@link UiElementNode} is declared by a corresponding
 * {@link ElementDescriptor}.
 * <p/>
 * The class implements {@link IPropertySource}, in order to fill the Eclipse property tab when
 * an element is selected. The {@link AttributeDescriptor} are used property descriptors.
 */
@SuppressWarnings("restriction") // XML model
public class UiElementNode implements IPropertySource {

    /** List of prefixes removed from android:id strings when creating short descriptions. */
    private static String[] ID_PREFIXES = {
        "@android:id/", //$NON-NLS-1$
        NEW_ID_PREFIX, ID_PREFIX, "@+", "@" }; //$NON-NLS-1$ //$NON-NLS-2$

    /** The element descriptor for the node. Always present, never null. */
    private ElementDescriptor mDescriptor;
    /** The parent element node in the UI model. It is null for a root element or until
     *  the node is attached to its parent. */
    private UiElementNode mUiParent;
    /** The {@link AndroidXmlEditor} handling the UI hierarchy. This is defined only for the
     *  root node. All children have the value set to null and query their parent. */
    private AndroidXmlEditor mEditor;
    /** The XML {@link Document} model that is being mirror by the UI model. This is defined
     *  only for the root node. All children have the value set to null and query their parent. */
    private Document mXmlDocument;
    /** The XML {@link Node} mirror by this UI node. This can be null for mandatory UI node which
     *  have no corresponding XML node or for new UI nodes before their XML node is set. */
    private Node mXmlNode;
    /** The list of all UI children nodes. Can be empty but never null. There's one UI children
     *  node per existing XML children node. */
    private ArrayList<UiElementNode> mUiChildren;
    /** The list of <em>all</em> UI attributes, as declared in the {@link ElementDescriptor}.
     *  The list is always defined and never null. Unlike the UiElementNode children list, this
     *  is always defined, even for attributes that do not exist in the XML model - that's because
     *  "missing" attributes in the XML model simply mean a default value is used. Also note that
     *  the underlying collection is a map, so order is not respected. To get the desired attribute
     *  order, iterate through the {@link ElementDescriptor}'s attribute list. */
    private HashMap<AttributeDescriptor, UiAttributeNode> mUiAttributes;
    private HashSet<UiAttributeNode> mUnknownUiAttributes;
    /** A read-only view of the UI children node collection. */
    private List<UiElementNode> mReadOnlyUiChildren;
    /** A read-only view of the UI attributes collection. */
    private Collection<UiAttributeNode> mCachedAllUiAttributes;
    /** A map of hidden attribute descriptors. Key is the XML name. */
    private Map<String, AttributeDescriptor> mCachedHiddenAttributes;
    /** An optional list of {@link IUiUpdateListener}. Most element nodes will not have any
     *  listeners attached, so the list is only created on demand and can be null. */
    private List<IUiUpdateListener> mUiUpdateListeners;
    /** A provider that knows how to create {@link ElementDescriptor} from unmapped XML names.
     *  The default is to have one that creates new {@link ElementDescriptor}. */
    private IUnknownDescriptorProvider mUnknownDescProvider;
    /** Error Flag */
    private boolean mHasError;

    /**
     * Creates a new {@link UiElementNode} described by a given {@link ElementDescriptor}.
     *
     * @param elementDescriptor The {@link ElementDescriptor} for the XML node. Cannot be null.
     */
    public UiElementNode(ElementDescriptor elementDescriptor) {
        mDescriptor = elementDescriptor;
        clearContent();
    }

    @Override
    public String toString() {
      return String.format("%s [desc: %s, parent: %s, children: %d]",         //$NON-NLS-1$
              this.getClass().getSimpleName(),
              mDescriptor,
              mUiParent != null ? mUiParent.toString() : "none",              //$NON-NLS-1$
                      mUiChildren != null ? mUiChildren.size() : 0
      );
    }

    /**
     * Clears the {@link UiElementNode} by resetting the children list and
     * the {@link UiAttributeNode}s list.
     * Also resets the attached XML node, document, editor if any.
     * <p/>
     * The parent {@link UiElementNode} node is not reset so that it's position
     * in the hierarchy be left intact, if any.
     */
    /* package */ void clearContent() {
        mXmlNode = null;
        mXmlDocument = null;
        mEditor = null;
        clearAttributes();
        mReadOnlyUiChildren = null;
        if (mUiChildren == null) {
            mUiChildren = new ArrayList<UiElementNode>();
        } else {
            // We can't remove mandatory nodes, we just clear them.
            for (int i = mUiChildren.size() - 1; i >= 0; --i) {
                removeUiChildAtIndex(i);
            }
        }
    }

    /**
     * Clears the internal list of attributes, the read-only cached version of it
     * and the read-only cached hidden attribute list.
     */
    private void clearAttributes() {
        mUiAttributes = null;
        mCachedAllUiAttributes = null;
        mCachedHiddenAttributes = null;
        mUnknownUiAttributes = new HashSet<UiAttributeNode>();
    }

    /**
     * Gets or creates the internal UiAttributes list.
     * <p/>
     * When the descriptor derives from ViewElementDescriptor, this list depends on the
     * current UiParent node.
     *
     * @return A new set of {@link UiAttributeNode} that matches the expected
     *         attributes for this node.
     */
    private HashMap<AttributeDescriptor, UiAttributeNode> getInternalUiAttributes() {
        if (mUiAttributes == null) {
            AttributeDescriptor[] attrList = getAttributeDescriptors();
            mUiAttributes = new HashMap<AttributeDescriptor, UiAttributeNode>(attrList.length);
            for (AttributeDescriptor desc : attrList) {
                UiAttributeNode uiNode = desc.createUiNode(this);
                if (uiNode != null) {  // Some AttributeDescriptors do not have UI associated
                    mUiAttributes.put(desc, uiNode);
                }
            }
        }
        return mUiAttributes;
    }

    /**
     * Computes a short string describing the UI node suitable for tree views.
     * Uses the element's attribute "android:name" if present, or the "android:label" one
     * followed by the element's name if not repeated.
     *
     * @return A short string describing the UI node suitable for tree views.
     */
    public String getShortDescription() {
        String name = mDescriptor.getUiName();
        String attr = getDescAttribute();
        if (attr != null) {
            // If the ui name is repeated in the attribute value, don't use it.
            // Typical case is to avoid ".pkg.MyActivity (Activity)".
            if (attr.contains(name)) {
                return attr;
            } else {
                return String.format("%1$s (%2$s)", attr, name);
            }
        }

        return name;
    }

    /** Returns the key attribute that can be used to describe this node, or null */
    private String getDescAttribute() {
        if (mXmlNode != null && mXmlNode instanceof Element && mXmlNode.hasAttributes()) {
            // Application and Manifest nodes have a special treatment: they are unique nodes
            // so we don't bother trying to differentiate their strings and we fall back to
            // just using the UI name below.
            Element elem = (Element) mXmlNode;

            String attr = _Element_getAttributeNS(elem,
                                SdkConstants.NS_RESOURCES,
                                AndroidManifestDescriptors.ANDROID_NAME_ATTR);
            if (attr == null || attr.length() == 0) {
                attr = _Element_getAttributeNS(elem,
                                SdkConstants.NS_RESOURCES,
                                AndroidManifestDescriptors.ANDROID_LABEL_ATTR);
            } else if (mXmlNode.getNodeName().equals(SdkConstants.VIEW_FRAGMENT)) {
                attr = attr.substring(attr.lastIndexOf('.') + 1);
            }
            if (attr == null || attr.length() == 0) {
                attr = _Element_getAttributeNS(elem,
                                SdkConstants.NS_RESOURCES,
                                OtherXmlDescriptors.PREF_KEY_ATTR);
            }
            if (attr == null || attr.length() == 0) {
                attr = _Element_getAttributeNS(elem,
                                null, // no namespace
                                SdkConstants.ATTR_NAME);
            }
            if (attr == null || attr.length() == 0) {
                attr = _Element_getAttributeNS(elem,
                                SdkConstants.NS_RESOURCES,
                                SdkConstants.ATTR_ID);

                if (attr != null && attr.length() > 0) {
                    for (String prefix : ID_PREFIXES) {
                        if (attr.startsWith(prefix)) {
                            attr = attr.substring(prefix.length());
                            break;
                        }
                    }
                }
            }
            if (attr != null && attr.length() > 0) {
                return attr;
            }
        }

        return null;
    }

    /**
     * Computes a styled string describing the UI node suitable for tree views.
     * Similar to {@link #getShortDescription()} but styles the Strings.
     *
     * @return A styled string describing the UI node suitable for tree views.
     */
    public StyledString getStyledDescription() {
        String uiName = mDescriptor.getUiName();

        // Special case: for <view>, show the class attribute value instead.
        // This is done here rather than in the descriptor since this depends on
        // node instance data.
        if (SdkConstants.VIEW_TAG.equals(uiName) && mXmlNode instanceof Element) {
            Element element = (Element) mXmlNode;
            String cls = element.getAttribute(ATTR_CLASS);
            if (cls != null) {
                uiName = cls.substring(cls.lastIndexOf('.') + 1);
            }
        }

        StyledString styledString = new StyledString();
        String attr = getDescAttribute();
        if (attr != null) {
            // Don't append the two when it's a repeat, e.g. Button01 (Button),
            // only when the ui name is not part of the attribute
            if (attr.toLowerCase(Locale.US).indexOf(uiName.toLowerCase(Locale.US)) == -1) {
                styledString.append(attr);
                styledString.append(String.format(" (%1$s)", uiName),
                        StyledString.DECORATIONS_STYLER);
            } else {
                styledString.append(attr);
            }
        }

        if (styledString.length() == 0) {
            styledString.append(uiName);
        }

        return styledString;
    }

    /**
     * Retrieves an attribute value by local name and namespace URI.
     * <br>Per [<a href='http://www.w3.org/TR/1999/REC-xml-names-19990114/'>XML Namespaces</a>]
     * , applications must use the value <code>null</code> as the
     * <code>namespaceURI</code> parameter for methods if they wish to have
     * no namespace.
     * <p/>
     * Note: This is a wrapper around {@link Element#getAttributeNS(String, String)}.
     * In some versions of webtools, the getAttributeNS implementation crashes with an NPE.
     * This wrapper will return an empty string instead.
     *
     * @see Element#getAttributeNS(String, String)
     * @see <a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=318108">https://bugs.eclipse.org/bugs/show_bug.cgi?id=318108</a>
     * @return The result from {@link Element#getAttributeNS(String, String)} or an empty string.
     */
    private String _Element_getAttributeNS(Element element,
            String namespaceURI,
            String localName) {
        try {
            return element.getAttributeNS(namespaceURI, localName);
        } catch (Exception ignore) {
            return "";
        }
    }

    /**
     * Computes a "breadcrumb trail" description for this node.
     * It will look something like "Manifest > Application > .myactivity (Activity) > Intent-Filter"
     *
     * @param includeRoot Whether to include the root (e.g. "Manifest") or not. Has no effect
     *                     when called on the root node itself.
     * @return The "breadcrumb trail" description for this node.
     */
    public String getBreadcrumbTrailDescription(boolean includeRoot) {
        StringBuilder sb = new StringBuilder(getShortDescription());

        for (UiElementNode uiNode = getUiParent();
                uiNode != null;
                uiNode = uiNode.getUiParent()) {
            if (!includeRoot && uiNode.getUiParent() == null) {
                break;
            }
            sb.insert(0, String.format("%1$s > ", uiNode.getShortDescription())); //$NON-NLS-1$
        }

        return sb.toString();
    }

    /**
     * Sets the XML {@link Document}.
     * <p/>
     * The XML {@link Document} is initially null. The XML {@link Document} must be set only on the
     * UI root element node (this method takes care of that.)
     * @param xmlDoc The new XML document to associate this node with.
     */
    public void setXmlDocument(Document xmlDoc) {
        if (mUiParent == null) {
            mXmlDocument = xmlDoc;
        } else {
            mUiParent.setXmlDocument(xmlDoc);
        }
    }

    /**
     * Returns the XML {@link Document}.
     * <p/>
     * The value is initially null until the UI node is attached to its UI parent -- the value
     * of the document is then propagated.
     *
     * @return the XML {@link Document} or the parent's XML {@link Document} or null.
     */
    public Document getXmlDocument() {
        if (mXmlDocument != null) {
            return mXmlDocument;
        } else if (mUiParent != null) {
            return mUiParent.getXmlDocument();
        }
        return null;
    }

    /**
     * Returns the XML node associated with this UI node.
     * <p/>
     * Some {@link ElementDescriptor} are declared as being "mandatory". This means the
     * corresponding UI node will exist even if there is no corresponding XML node. Such structure
     * is created and enforced by the parent of the tree, not the element themselves. However
     * such nodes will likely not have an XML node associated, so getXmlNode() can return null.
     *
     * @return The associated XML node. Can be null for mandatory nodes.
     */
    public Node getXmlNode() {
        return mXmlNode;
    }

    /**
     * Returns the {@link ElementDescriptor} for this node. This is never null.
     * <p/>
     * Do not use this to call getDescriptor().getAttributes(), instead call
     * getAttributeDescriptors() which can be overridden by derived classes.
     * @return The {@link ElementDescriptor} for this node. This is never null.
     */
    public ElementDescriptor getDescriptor() {
        return mDescriptor;
    }

    /**
     * Returns the {@link AttributeDescriptor} array for the descriptor of this node.
     * <p/>
     * Use this instead of getDescriptor().getAttributes() -- derived classes can override
     * this to manipulate the attribute descriptor list depending on the current UI node.
     * @return The {@link AttributeDescriptor} array for the descriptor of this node.
     */
    public AttributeDescriptor[] getAttributeDescriptors() {
        return mDescriptor.getAttributes();
    }

    /**
     * Returns the hidden {@link AttributeDescriptor} array for the descriptor of this node.
     * This is a subset of the getAttributeDescriptors() list.
     * <p/>
     * Use this instead of getDescriptor().getHiddenAttributes() -- potentially derived classes
     * could override this to manipulate the attribute descriptor list depending on the current
     * UI node. There's no need for it right now so keep it private.
     */
    private Map<String, AttributeDescriptor> getHiddenAttributeDescriptors() {
        if (mCachedHiddenAttributes == null) {
            mCachedHiddenAttributes = new HashMap<String, AttributeDescriptor>();
            for (AttributeDescriptor attrDesc : getAttributeDescriptors()) {
                if (attrDesc instanceof XmlnsAttributeDescriptor) {
                    mCachedHiddenAttributes.put(
                            ((XmlnsAttributeDescriptor) attrDesc).getXmlNsName(),
                            attrDesc);
                }
            }
        }
        return mCachedHiddenAttributes;
    }

    /**
     * Sets the parent of this UiElementNode.
     * <p/>
     * The root node has no parent.
     */
    protected void setUiParent(UiElementNode parent) {
        mUiParent = parent;
        // Invalidate the internal UiAttributes list, as it may depend on the actual UiParent.
        clearAttributes();
    }

    /**
     * @return The parent {@link UiElementNode} or null if this is the root node.
     */
    public UiElementNode getUiParent() {
        return mUiParent;
    }

    /**
     * Returns the root {@link UiElementNode}.
     *
     * @return The root {@link UiElementNode}.
     */
    public UiElementNode getUiRoot() {
        UiElementNode root = this;
        while (root.mUiParent != null) {
            root = root.mUiParent;
        }

        return root;
    }

    /**
     * Returns the index of this sibling (where the first child has index 0, the second child
     * has index 1, and so on.)
     *
     * @return The sibling index of this node
     */
    public int getUiSiblingIndex() {
        if (mUiParent != null) {
            int index = 0;
            for (UiElementNode node : mUiParent.getUiChildren()) {
                if (node == this) {
                    break;
                }
                index++;
            }
            return index;
        }

        return 0;
    }

    /**
     * Returns the previous UI sibling of this UI node. If the node does not have a previous
     * sibling, returns null.
     *
     * @return The previous UI sibling of this UI node, or null if not applicable.
     */
    public UiElementNode getUiPreviousSibling() {
        if (mUiParent != null) {
            List<UiElementNode> childlist = mUiParent.getUiChildren();
            if (childlist != null && childlist.size() > 1 && childlist.get(0) != this) {
                int index = childlist.indexOf(this);
                return index > 0 ? childlist.get(index - 1) : null;
            }
        }
        return null;
    }

    /**
     * Returns the next UI sibling of this UI node.
     * If the node does not have a next sibling, returns null.
     *
     * @return The next UI sibling of this UI node, or null.
     */
    public UiElementNode getUiNextSibling() {
        if (mUiParent != null) {
            List<UiElementNode> childlist = mUiParent.getUiChildren();
            if (childlist != null) {
                int size = childlist.size();
                if (size > 1 && childlist.get(size - 1) != this) {
                    int index = childlist.indexOf(this);
                    return index >= 0 && index < size - 1 ? childlist.get(index + 1) : null;
                }
            }
        }
        return null;
    }

    /**
     * Sets the {@link AndroidXmlEditor} handling this {@link UiElementNode} hierarchy.
     * <p/>
     * The editor must always be set on the root node. This method takes care of that.
     *
     * @param editor The editor to associate this node with.
     */
    public void setEditor(AndroidXmlEditor editor) {
        if (mUiParent == null) {
            mEditor = editor;
        } else {
            mUiParent.setEditor(editor);
        }
    }

    /**
     * Returns the {@link AndroidXmlEditor} that embeds this {@link UiElementNode}.
     * <p/>
     * The value is initially null until the node is attached to its parent -- the value
     * of the root node is then propagated.
     *
     * @return The embedding {@link AndroidXmlEditor} or null.
     */
    public AndroidXmlEditor getEditor() {
        return mUiParent == null ? mEditor : mUiParent.getEditor();
    }

    /**
     * Returns the Android target data for the file being edited.
     *
     * @return The Android target data for the file being edited.
     */
    public AndroidTargetData getAndroidTarget() {
        return getEditor().getTargetData();
    }

    /**
     * @return A read-only version of the children collection.
     */
    public List<UiElementNode> getUiChildren() {
        if (mReadOnlyUiChildren == null) {
            mReadOnlyUiChildren = Collections.unmodifiableList(mUiChildren);
        }
        return mReadOnlyUiChildren;
    }

    /**
     * Returns a collection containing all the known attributes as well as
     * all the unknown ui attributes.
     *
     * @return A read-only version of the attributes collection.
     */
    public Collection<UiAttributeNode> getAllUiAttributes() {
        if (mCachedAllUiAttributes == null) {

            List<UiAttributeNode> allValues =
                new ArrayList<UiAttributeNode>(getInternalUiAttributes().values());
            allValues.addAll(mUnknownUiAttributes);

            mCachedAllUiAttributes = Collections.unmodifiableCollection(allValues);
        }
        return mCachedAllUiAttributes;
    }

    /**
     * Returns all the unknown ui attributes, that is those we found defined in the
     * actual XML but that we don't have descriptors for.
     *
     * @return A read-only version of the unknown attributes collection.
     */
    public Collection<UiAttributeNode> getUnknownUiAttributes() {
        return Collections.unmodifiableCollection(mUnknownUiAttributes);
    }

    /**
     * Sets the error flag value.
     *
     * @param errorFlag the error flag
     */
    public final void setHasError(boolean errorFlag) {
        mHasError = errorFlag;
    }

    /**
     * Returns whether this node, its attributes, or one of the children nodes (and attributes)
     * has errors.
     *
     * @return True if this node, its attributes, or one of the children nodes (and attributes)
     * has errors.
     */
    public final boolean hasError() {
        if (mHasError) {
            return true;
        }

        // get the error value from the attributes.
        for (UiAttributeNode attribute : getAllUiAttributes()) {
            if (attribute.hasError()) {
                return true;
            }
        }

        // and now from the children.
        for (UiElementNode child : mUiChildren) {
            if (child.hasError()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns the provider that knows how to create {@link ElementDescriptor} from unmapped
     * XML names.
     * <p/>
     * The default is to have one that creates new {@link ElementDescriptor}.
     * <p/>
     * There is only one such provider in any UI model tree, attached to the root node.
     *
     * @return An instance of {@link IUnknownDescriptorProvider}. Can never be null.
     */
    public IUnknownDescriptorProvider getUnknownDescriptorProvider() {
        if (mUiParent != null) {
            return mUiParent.getUnknownDescriptorProvider();
        }
        if (mUnknownDescProvider == null) {
            // Create the default one on demand.
            mUnknownDescProvider = new IUnknownDescriptorProvider() {

                private final HashMap<String, ElementDescriptor> mMap =
                    new HashMap<String, ElementDescriptor>();

                /**
                 * The default is to create a new ElementDescriptor wrapping
                 * the unknown XML local name and reuse previously created descriptors.
                 */
                @Override
                public ElementDescriptor getDescriptor(String xmlLocalName) {

                    ElementDescriptor desc = mMap.get(xmlLocalName);

                    if (desc == null) {
                        desc = new ElementDescriptor(xmlLocalName);
                        mMap.put(xmlLocalName, desc);
                    }

                    return desc;
                }
            };
        }
        return mUnknownDescProvider;
    }

    /**
     * Sets the provider that knows how to create {@link ElementDescriptor} from unmapped
     * XML names.
     * <p/>
     * The default is to have one that creates new {@link ElementDescriptor}.
     * <p/>
     * There is only one such provider in any UI model tree, attached to the root node.
     *
     * @param unknownDescProvider The new provider to use. Must not be null.
     */
    public void setUnknownDescriptorProvider(IUnknownDescriptorProvider unknownDescProvider) {
        if (mUiParent == null) {
            mUnknownDescProvider = unknownDescProvider;
        } else {
            mUiParent.setUnknownDescriptorProvider(unknownDescProvider);
        }
    }

    /**
     * Adds a new {@link IUiUpdateListener} to the internal update listener list.
     *
     * @param listener The listener to add.
     */
    public void addUpdateListener(IUiUpdateListener listener) {
       if (mUiUpdateListeners == null) {
           mUiUpdateListeners = new ArrayList<IUiUpdateListener>();
       }
       if (!mUiUpdateListeners.contains(listener)) {
           mUiUpdateListeners.add(listener);
       }
    }

    /**
     * Removes an existing {@link IUiUpdateListener} from the internal update listener list.
     * Does nothing if the list is empty or the listener is not registered.
     *
     * @param listener The listener to remove.
     */
    public void removeUpdateListener(IUiUpdateListener listener) {
       if (mUiUpdateListeners != null) {
           mUiUpdateListeners.remove(listener);
       }
    }

    /**
     * Finds a child node relative to this node using a path-like expression.
     * F.ex. "node1/node2" would find a child "node1" that contains a child "node2" and
     * returns the latter. If there are multiple nodes with the same name at the same
     * level, always uses the first one found.
     *
     * @param path The path like expression to select a child node.
     * @return The ui node found or null.
     */
    public UiElementNode findUiChildNode(String path) {
        String[] items = path.split("/");  //$NON-NLS-1$
        UiElementNode uiNode = this;
        for (String item : items) {
            boolean nextSegment = false;
            for (UiElementNode c : uiNode.mUiChildren) {
                if (c.getDescriptor().getXmlName().equals(item)) {
                    uiNode = c;
                    nextSegment = true;
                    break;
                }
            }
            if (!nextSegment) {
                return null;
            }
        }
        return uiNode;
    }

    /**
     * Finds an {@link UiElementNode} which contains the give XML {@link Node}.
     * Looks recursively in all children UI nodes.
     *
     * @param xmlNode The XML node to look for.
     * @return The {@link UiElementNode} that contains xmlNode or null if not found,
     */
    public UiElementNode findXmlNode(Node xmlNode) {
        if (xmlNode == null) {
            return null;
        }
        if (getXmlNode() == xmlNode) {
            return this;
        }

        for (UiElementNode uiChild : mUiChildren) {
            UiElementNode found = uiChild.findXmlNode(xmlNode);
            if (found != null) {
                return found;
            }
        }

        return null;
    }

    /**
     * Returns the {@link UiAttributeNode} matching this attribute descriptor or
     * null if not found.
     *
     * @param attrDesc The {@link AttributeDescriptor} to match.
     * @return the {@link UiAttributeNode} matching this attribute descriptor or null
     *         if not found.
     */
    public UiAttributeNode findUiAttribute(AttributeDescriptor attrDesc) {
        return getInternalUiAttributes().get(attrDesc);
    }

    /**
     * Populate this element node with all values from the given XML node.
     *
     * This fails if the given XML node has a different element name -- it won't change the
     * type of this ui node.
     *
     * This method can be both used for populating values the first time and updating values
     * after the XML model changed.
     *
     * @param xmlNode The XML node to mirror
     * @return Returns true if the XML structure has changed (nodes added, removed or replaced)
     */
    public boolean loadFromXmlNode(Node xmlNode) {
        boolean structureChanged = (mXmlNode != xmlNode);
        mXmlNode = xmlNode;
        if (xmlNode != null) {
            updateAttributeList(xmlNode);
            structureChanged |= updateElementList(xmlNode);
            invokeUiUpdateListeners(structureChanged ? UiUpdateState.CHILDREN_CHANGED
                                                      : UiUpdateState.ATTR_UPDATED);
        }
        return structureChanged;
    }

    /**
     * Clears the UI node and reload it from the given XML node.
     * <p/>
     * This works by clearing all references to any previous XML or UI nodes and
     * then reloads the XML document from scratch. The editor reference is kept.
     * <p/>
     * This is used in the special case where the ElementDescriptor structure has changed.
     * Rather than try to diff inflated UI nodes (as loadFromXmlNode does), we don't bother
     * and reload everything. This is not subtle and should be used very rarely.
     *
     * @param xmlNode The XML node or document to reload. Can be null.
     */
    public void reloadFromXmlNode(Node xmlNode) {
        // The editor needs to be preserved, it is not affected by an XML change.
        AndroidXmlEditor editor = getEditor();
        clearContent();
        setEditor(editor);
        if (xmlNode != null) {
            setXmlDocument(xmlNode.getOwnerDocument());
        }
        // This will reload all the XML and recreate the UI structure from scratch.
        loadFromXmlNode(xmlNode);
    }

    /**
     * Called by attributes when they want to commit their value
     * to an XML node.
     * <p/>
     * For mandatory nodes, this makes sure the underlying XML element node
     * exists in the model. If not, it is created and assigned as the underlying
     * XML node.
     * </br>
     * For non-mandatory nodes, simply return the underlying XML node, which
     * must always exists.
     *
     * @return The XML node matching this {@link UiElementNode} or null.
     */
    public Node prepareCommit() {
        if (getDescriptor().getMandatory() != Mandatory.NOT_MANDATORY) {
            createXmlNode();
            // The new XML node has been created.
            // We don't need to refresh using loadFromXmlNode() since there are
            // no attributes or elements that need to be loading into this node.
        }
        return getXmlNode();
    }

    /**
     * Commits the attributes (all internal, inherited from UI parent & unknown attributes).
     * This is called by the UI when the embedding part needs to be committed.
     */
    public void commit() {
        for (UiAttributeNode uiAttr : getAllUiAttributes()) {
            uiAttr.commit();
        }
    }

    /**
     * Returns true if the part has been modified with respect to the data
     * loaded from the model.
     * @return True if the part has been modified with respect to the data
     * loaded from the model.
     */
    public boolean isDirty() {
        for (UiAttributeNode uiAttr : getAllUiAttributes()) {
            if (uiAttr.isDirty()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Creates the underlying XML element node for this UI node if it doesn't already
     * exists.
     *
     * @return The new value of getXmlNode() (can be null if creation failed)
     */
    public Node createXmlNode() {
        if (mXmlNode != null) {
            return null;
        }
        Node parentXmlNode = null;
        if (mUiParent != null) {
            parentXmlNode = mUiParent.prepareCommit();
            if (parentXmlNode == null) {
                // The parent failed to create its own backing XML node. Abort.
                // No need to throw an exception, the parent will most likely
                // have done so itself.
                return null;
            }
        }

        String elementName = getDescriptor().getXmlName();
        Document doc = getXmlDocument();

        // We *must* have a root node. If not, we need to abort.
        if (doc == null) {
            throw new RuntimeException(
                    String.format("Missing XML document for %1$s XML node.", elementName));
        }

        // If we get here and parentXmlNode is null, the node is to be created
        // as the root node of the document (which can't be null, cf. check above).
        if (parentXmlNode == null) {
            parentXmlNode = doc;
        }

        mXmlNode = doc.createElement(elementName);

        // If this element does not have children, mark it as an empty tag
        // such that the XML looks like <tag/> instead of <tag></tag>
        if (!mDescriptor.hasChildren()) {
            if (mXmlNode instanceof ElementImpl) {
                ElementImpl element = (ElementImpl) mXmlNode;
                element.setEmptyTag(true);
            }
        }

        Node xmlNextSibling = null;

        UiElementNode uiNextSibling = getUiNextSibling();
        if (uiNextSibling != null) {
            xmlNextSibling = uiNextSibling.getXmlNode();
        }

        Node previousTextNode = null;
        if (xmlNextSibling != null) {
            Node previousNode = xmlNextSibling.getPreviousSibling();
            if (previousNode != null && previousNode.getNodeType() == Node.TEXT_NODE) {
                previousTextNode = previousNode;
            }
        } else {
            Node lastChild = parentXmlNode.getLastChild();
            if (lastChild != null && lastChild.getNodeType() == Node.TEXT_NODE) {
                previousTextNode = lastChild;
            }
        }

        String insertAfter = null;

        // Try to figure out the indentation node to insert. Even in auto-formatting
        // we need to do this, because it turns out the XML editor's formatter does
        // not do a very good job with completely botched up XML; it does a much better
        // job if the new XML is already mostly well formatted. Thus, the main purpose
        // of applying the real XML formatter after our own indentation attempts here is
        // to make it apply its own tab-versus-spaces indentation properties, have it
        // insert line breaks before attributes (if the user has configured that), etc.

        // First figure out the indentation level of the newly inserted element;
        // this is either the same as the previous sibling, or if there is no sibling,
        // it's the indentation of the parent plus one indentation level.
        boolean isFirstChild = getUiPreviousSibling() == null
                || parentXmlNode.getFirstChild() == null;
        AndroidXmlEditor editor = getEditor();
        String indent;
        String parentIndent = ""; //$NON-NLS-1$
        if (isFirstChild) {
            indent = parentIndent = editor.getIndent(parentXmlNode);
            // We need to add one level of indentation. Are we using tabs?
            // Can't get to formatting settings so let's just look at the
            // parent indentation and see if we can guess
            if (indent.length() > 0 && indent.charAt(indent.length()-1) == '\t') {
                indent = indent + '\t';
            } else {
                // Not using tabs, or we can't figure it out (because parent had no
                // indentation). In that case, indent with 4 spaces, as seems to
                // be the Android default.
                indent = indent + "    "; //$NON-NLS-1$
            }
        } else {
            // Find out the indent of the previous sibling
            indent = editor.getIndent(getUiPreviousSibling().getXmlNode());
        }

        // We want to insert the new element BEFORE the text node which precedes
        // the next element, since that text node is the next element's indentation!
        if (previousTextNode != null) {
            xmlNextSibling = previousTextNode;
        } else {
            // If there's no previous text node, we are probably inside an
            // empty element (<LinearLayout>|</LinearLayout>) and in that case we need
            // to not only insert a newline and indentation before the new element, but
            // after it as well.
            insertAfter = parentIndent;
        }

        // Insert indent text node before the new element
        IStructuredDocument document = editor.getStructuredDocument();
        String newLine;
        if (document != null) {
            newLine = TextUtilities.getDefaultLineDelimiter(document);
        } else {
            newLine = SdkUtils.getLineSeparator();
        }
        Text indentNode = doc.createTextNode(newLine + indent);
        parentXmlNode.insertBefore(indentNode, xmlNextSibling);

        // Insert the element itself
        parentXmlNode.insertBefore(mXmlNode, xmlNextSibling);

        // Insert a separator after the tag. We only do this when we've inserted
        // a tag into an area where there was no whitespace before
        // (e.g. a new child of <LinearLayout></LinearLayout>).
        if (insertAfter != null) {
            Text sep = doc.createTextNode(newLine + insertAfter);
            parentXmlNode.insertBefore(sep, xmlNextSibling);
        }

        // Set all initial attributes in the XML node if they are not empty.
        // Iterate on the descriptor list to get the desired order and then use the
        // internal values, if any.
        List<UiAttributeNode> addAttributes = new ArrayList<UiAttributeNode>();

        for (AttributeDescriptor attrDesc : getAttributeDescriptors()) {
            if (attrDesc instanceof XmlnsAttributeDescriptor) {
                XmlnsAttributeDescriptor desc = (XmlnsAttributeDescriptor) attrDesc;
                Attr attr = doc.createAttributeNS(SdkConstants.XMLNS_URI,
                        desc.getXmlNsName());
                attr.setValue(desc.getValue());
                attr.setPrefix(desc.getXmlNsPrefix());
                mXmlNode.getAttributes().setNamedItemNS(attr);
            } else {
                UiAttributeNode uiAttr = getInternalUiAttributes().get(attrDesc);

                // Don't apply the attribute immediately, instead record this attribute
                // such that we can gather all attributes and sort them first.
                // This is necessary because the XML model will *append* all attributes
                // so we want to add them in a particular order.
                // (Note that we only have to worry about UiAttributeNodes with non null
                // values, since this is a new node and we therefore don't need to attempt
                // to remove existing attributes)
                String value = uiAttr.getCurrentValue();
                if (value != null && value.length() > 0) {
                    addAttributes.add(uiAttr);
                }
            }
        }

        // Sort and apply the attributes in order, because the Eclipse XML model will always
        // append the XML attributes, so by inserting them in our desired order they will
        // appear that way in the XML
        Collections.sort(addAttributes);

        for (UiAttributeNode node : addAttributes) {
            commitAttributeToXml(node, node.getCurrentValue());
            node.setDirty(false);
        }

        getEditor().scheduleNodeReformat(this, false);

        // Notify per-node listeners
        invokeUiUpdateListeners(UiUpdateState.CREATED);
        // Notify global listeners
        fireNodeCreated(this, getUiSiblingIndex());

        return mXmlNode;
    }

    /**
     * Removes the XML node corresponding to this UI node if it exists
     * and also removes all mirrored information in this UI node (i.e. children, attributes)
     *
     * @return The removed node or null if it didn't exist in the first place.
     */
    public Node deleteXmlNode() {
        if (mXmlNode == null) {
            return null;
        }

        int previousIndex = getUiSiblingIndex();

        // First clear the internals of the node and *then* actually deletes the XML
        // node (because doing so will generate an update even and this node may be
        // revisited via loadFromXmlNode).
        Node oldXmlNode = mXmlNode;
        clearContent();

        Node xmlParent = oldXmlNode.getParentNode();
        if (xmlParent == null) {
            xmlParent = getXmlDocument();
        }
        Node previousSibling = oldXmlNode.getPreviousSibling();
        oldXmlNode = xmlParent.removeChild(oldXmlNode);

        // We need to remove the text node BEFORE the removed element, since THAT's the
        // indentation node for the removed element.
        if (previousSibling != null && previousSibling.getNodeType() == Node.TEXT_NODE
                && previousSibling.getNodeValue().trim().length() == 0) {
            xmlParent.removeChild(previousSibling);
        }

        invokeUiUpdateListeners(UiUpdateState.DELETED);
        fireNodeDeleted(this, previousIndex);

        return oldXmlNode;
    }

    /**
     * Updates the element list for this UiElementNode.
     * At the end, the list of children UiElementNode here will match the one from the
     * provided XML {@link Node}:
     * <ul>
     * <li> Walk both the current ui children list and the xml children list at the same time.
     * <li> If we have a new xml child but already reached the end of the ui child list, add the
     *      new xml node.
     * <li> Otherwise, check if the xml node is referenced later in the ui child list and if so,
     *      move it here. It means the XML child list has been reordered.
     * <li> Otherwise, this is a new XML node that we add in the middle of the ui child list.
     * <li> At the end, we may have finished walking the xml child list but still have remaining
     *      ui children, simply delete them as they matching trailing xml nodes that have been
     *      removed unless they are mandatory ui nodes.
     * </ul>
     * Note that only the first case is used when populating the ui list the first time.
     *
     * @param xmlNode The XML node to mirror
     * @return True when the XML structure has changed.
     */
    protected boolean updateElementList(Node xmlNode) {
        boolean structureChanged = false;
        boolean hasMandatoryLast = false;
        int uiIndex = 0;
        Node xmlChild = xmlNode.getFirstChild();
        while (xmlChild != null) {
            if (xmlChild.getNodeType() == Node.ELEMENT_NODE) {
                String elementName = xmlChild.getNodeName();
                UiElementNode uiNode = null;
                CustomViewDescriptorService service = CustomViewDescriptorService.getInstance();
                if (mUiChildren.size() <= uiIndex) {
                    // A new node is being added at the end of the list
                    ElementDescriptor desc = mDescriptor.findChildrenDescriptor(elementName,
                            false /* recursive */);
                    if (desc == null && elementName.indexOf('.') != -1 &&
                            (!elementName.startsWith(ANDROID_PKG_PREFIX)
                                    || elementName.startsWith(ANDROID_SUPPORT_PKG_PREFIX))) {
                        AndroidXmlEditor editor = getEditor();
                        if (editor != null && editor.getProject() != null) {
                            desc = service.getDescriptor(editor.getProject(), elementName);
                        }
                    }
                    if (desc == null) {
                        // Unknown node. Create a temporary descriptor for it.
                        // We'll add unknown attributes to it later.
                        IUnknownDescriptorProvider p = getUnknownDescriptorProvider();
                        desc = p.getDescriptor(elementName);
                    }
                    structureChanged = true;
                    uiNode = appendNewUiChild(desc);
                    uiIndex++;
                } else {
                    // A new node is being inserted or moved.
                    // Note: mandatory nodes can be created without an XML node in which case
                    // getXmlNode() is null.
                    UiElementNode uiChild;
                    int n = mUiChildren.size();
                    for (int j = uiIndex; j < n; j++) {
                        uiChild = mUiChildren.get(j);
                        if (uiChild.getXmlNode() != null && uiChild.getXmlNode() == xmlChild) {
                            if (j > uiIndex) {
                                // Found the same XML node at some later index, now move it here.
                                mUiChildren.remove(j);
                                mUiChildren.add(uiIndex, uiChild);
                                structureChanged = true;
                            }
                            uiNode = uiChild;
                            uiIndex++;
                            break;
                        }
                    }

                    if (uiNode == null) {
                        // Look for an unused mandatory node with no XML node attached
                        // referencing the same XML element name
                        for (int j = uiIndex; j < n; j++) {
                            uiChild = mUiChildren.get(j);
                            if (uiChild.getXmlNode() == null &&
                                    uiChild.getDescriptor().getMandatory() !=
                                                                Mandatory.NOT_MANDATORY &&
                                    uiChild.getDescriptor().getXmlName().equals(elementName)) {

                                if (j > uiIndex) {
                                    // Found it, now move it here
                                    mUiChildren.remove(j);
                                    mUiChildren.add(uiIndex, uiChild);
                                }
                                // Assign the XML node to this empty mandatory element.
                                uiChild.mXmlNode = xmlChild;
                                structureChanged = true;
                                uiNode = uiChild;
                                uiIndex++;
                            }
                        }
                    }

                    if (uiNode == null) {
                        // Inserting new node
                        ElementDescriptor desc = mDescriptor.findChildrenDescriptor(elementName,
                                false /* recursive */);
                        if (desc == null && elementName.indexOf('.') != -1 &&
                                (!elementName.startsWith(ANDROID_PKG_PREFIX)
                                        || elementName.startsWith(ANDROID_SUPPORT_PKG_PREFIX))) {
                            AndroidXmlEditor editor = getEditor();
                            if (editor != null && editor.getProject() != null) {
                                desc = service.getDescriptor(editor.getProject(), elementName);
                            }
                        }
                        if (desc == null) {
                            // Unknown node. Create a temporary descriptor for it.
                            // We'll add unknown attributes to it later.
                            IUnknownDescriptorProvider p = getUnknownDescriptorProvider();
                            desc = p.getDescriptor(elementName);
                        } else {
                            structureChanged = true;
                            uiNode = insertNewUiChild(uiIndex, desc);
                            uiIndex++;
                        }
                    }
                }
                if (uiNode != null) {
                    // If we touched an UI Node, even an existing one, refresh its content.
                    // For new nodes, this will populate them recursively.
                    structureChanged |= uiNode.loadFromXmlNode(xmlChild);

                    // Remember if there are any mandatory-last nodes to reorder.
                    hasMandatoryLast |=
                        uiNode.getDescriptor().getMandatory() == Mandatory.MANDATORY_LAST;
                }
            }
            xmlChild = xmlChild.getNextSibling();
        }

        // There might be extra UI nodes at the end if the XML node list got shorter.
        for (int index = mUiChildren.size() - 1; index >= uiIndex; --index) {
             structureChanged |= removeUiChildAtIndex(index);
        }

        if (hasMandatoryLast) {
            // At least one mandatory-last uiNode was moved. Let's see if we can
            // move them back to the last position. That's possible if the only
            // thing between these and the end are other mandatory empty uiNodes
            // (mandatory uiNodes with no XML attached are pure "virtual" reserved
            // slots and it's ok to reorganize them but other can't.)
            int n = mUiChildren.size() - 1;
            for (int index = n; index >= 0; index--) {
                UiElementNode uiChild = mUiChildren.get(index);
                Mandatory mand = uiChild.getDescriptor().getMandatory();
                if (mand == Mandatory.MANDATORY_LAST && index < n) {
                    // Remove it from index and move it back at the end of the list.
                    mUiChildren.remove(index);
                    mUiChildren.add(uiChild);
                } else if (mand == Mandatory.NOT_MANDATORY || uiChild.getXmlNode() != null) {
                    // We found at least one non-mandatory or a mandatory node with an actual
                    // XML attached, so there's nothing we can reorganize past this point.
                    break;
                }
            }
        }

        return structureChanged;
    }

    /**
     * Internal helper to remove an UI child node given by its index in the
     * internal child list.
     *
     * Also invokes the update listener on the node to be deleted *after* the node has
     * been removed.
     *
     * @param uiIndex The index of the UI child to remove, range 0 .. mUiChildren.size()-1
     * @return True if the structure has changed
     * @throws IndexOutOfBoundsException if index is out of mUiChildren's bounds. Of course you
     *         know that could never happen unless the computer is on fire or something.
     */
    private boolean removeUiChildAtIndex(int uiIndex) {
        UiElementNode uiNode = mUiChildren.get(uiIndex);
        ElementDescriptor desc = uiNode.getDescriptor();

        try {
            if (uiNode.getDescriptor().getMandatory() != Mandatory.NOT_MANDATORY) {
                // This is a mandatory node. Such a node must exist in the UiNode hierarchy
                // even if there's no XML counterpart. However we only need to keep one.

                // Check if the parent (e.g. this node) has another similar ui child node.
                boolean keepNode = true;
                for (UiElementNode child : mUiChildren) {
                    if (child != uiNode && child.getDescriptor() == desc) {
                        // We found another child with the same descriptor that is not
                        // the node we want to remove. This means we have one mandatory
                        // node so we can safely remove uiNode.
                        keepNode = false;
                        break;
                    }
                }

                if (keepNode) {
                    // We can't remove a mandatory node as we need to keep at least one
                    // mandatory node in the parent. Instead we just clear its content
                    // (including its XML Node reference).

                    // A mandatory node with no XML means it doesn't really exist, so it can't be
                    // deleted. So the structure will change only if the ui node is actually
                    // associated to an XML node.
                    boolean xmlExists = (uiNode.getXmlNode() != null);

                    uiNode.clearContent();
                    return xmlExists;
                }
            }

            mUiChildren.remove(uiIndex);

            return true;
        } finally {
            // Tell listeners that a node has been removed.
            // The model has already been modified.
            invokeUiUpdateListeners(UiUpdateState.DELETED);
        }
    }

    /**
     * Creates a new {@link UiElementNode} from the given {@link ElementDescriptor}
     * and appends it to the end of the element children list.
     *
     * @param descriptor The {@link ElementDescriptor} that knows how to create the UI node.
     * @return The new UI node that has been appended
     */
    public UiElementNode appendNewUiChild(ElementDescriptor descriptor) {
        UiElementNode uiNode;
        uiNode = descriptor.createUiNode();
        mUiChildren.add(uiNode);
        uiNode.setUiParent(this);
        uiNode.invokeUiUpdateListeners(UiUpdateState.CREATED);
        return uiNode;
    }

    /**
     * Creates a new {@link UiElementNode} from the given {@link ElementDescriptor}
     * and inserts it in the element children list at the specified position.
     *
     * @param index The position where to insert in the element children list.
     *              Shifts the element currently at that position (if any) and any
     *              subsequent elements to the right (adds one to their indices).
     *              Index must >= 0 and <= getUiChildren.size().
     *              Using size() means to append to the end of the list.
     * @param descriptor The {@link ElementDescriptor} that knows how to create the UI node.
     * @return The new UI node.
     */
    public UiElementNode insertNewUiChild(int index, ElementDescriptor descriptor) {
        UiElementNode uiNode;
        uiNode = descriptor.createUiNode();
        mUiChildren.add(index, uiNode);
        uiNode.setUiParent(this);
        uiNode.invokeUiUpdateListeners(UiUpdateState.CREATED);
        return uiNode;
    }

    /**
     * Updates the {@link UiAttributeNode} list for this {@link UiElementNode}
     * using the values from the XML element.
     * <p/>
     * For a given {@link UiElementNode}, the attribute list always exists in
     * full and is totally independent of whether the XML model actually
     * has the corresponding attributes.
     * <p/>
     * For each attribute declared in this {@link UiElementNode}, get
     * the corresponding XML attribute. It may not exist, in which case the
     * value will be null. We don't really know if a value has changed, so
     * the updateValue() is called on the UI attribute in all cases.
     *
     * @param xmlNode The XML node to mirror
     */
    protected void updateAttributeList(Node xmlNode) {
        NamedNodeMap xmlAttrMap = xmlNode.getAttributes();
        HashSet<Node> visited = new HashSet<Node>();

        // For all known (i.e. expected) UI attributes, find an existing XML attribute of
        // same (uri, local name) and update the internal Ui attribute value.
        for (UiAttributeNode uiAttr : getInternalUiAttributes().values()) {
            AttributeDescriptor desc = uiAttr.getDescriptor();
            if (!(desc instanceof SeparatorAttributeDescriptor)) {
                Node xmlAttr = xmlAttrMap == null ? null :
                    xmlAttrMap.getNamedItemNS(desc.getNamespaceUri(), desc.getXmlLocalName());
                uiAttr.updateValue(xmlAttr);
                visited.add(xmlAttr);
            }
        }

        // Clone the current list of unknown attributes. We'll then remove from this list when
        // we find attributes which are still unknown. What will be left are the old unknown
        // attributes that have been deleted in the current XML attribute list.
        @SuppressWarnings("unchecked")
        HashSet<UiAttributeNode> deleted = (HashSet<UiAttributeNode>) mUnknownUiAttributes.clone();

        // We need to ignore hidden attributes.
        Map<String, AttributeDescriptor> hiddenAttrDesc = getHiddenAttributeDescriptors();

        // Traverse the actual XML attribute list to find unknown attributes
        if (xmlAttrMap != null) {
            for (int i = 0; i < xmlAttrMap.getLength(); i++) {
                Node xmlAttr = xmlAttrMap.item(i);
                // Ignore attributes which have actual descriptors
                if (visited.contains(xmlAttr)) {
                    continue;
                }

                String xmlFullName = xmlAttr.getNodeName();

                // Ignore attributes which are hidden (based on the prefix:localName key)
                if (hiddenAttrDesc.containsKey(xmlFullName)) {
                    continue;
                }

                String xmlAttrLocalName = xmlAttr.getLocalName();
                String xmlNsUri = xmlAttr.getNamespaceURI();

                UiAttributeNode uiAttr = null;
                for (UiAttributeNode a : mUnknownUiAttributes) {
                    String aLocalName = a.getDescriptor().getXmlLocalName();
                    String aNsUri = a.getDescriptor().getNamespaceUri();
                    if (aLocalName.equals(xmlAttrLocalName) &&
                            (aNsUri == xmlNsUri || (aNsUri != null && aNsUri.equals(xmlNsUri)))) {
                        // This attribute is still present in the unknown list
                        uiAttr = a;
                        // It has not been deleted
                        deleted.remove(a);
                        break;
                    }
                }
                if (uiAttr == null) {
                    uiAttr = addUnknownAttribute(xmlFullName, xmlAttrLocalName, xmlNsUri);
                }

                uiAttr.updateValue(xmlAttr);
            }

            // Remove from the internal list unknown attributes that have been deleted from the xml
            for (UiAttributeNode a : deleted) {
                mUnknownUiAttributes.remove(a);
                mCachedAllUiAttributes = null;
            }
        }
    }

    /**
     * Create a new temporary text attribute descriptor for the unknown attribute
     * and returns a new {@link UiAttributeNode} associated to this descriptor.
     * <p/>
     * The attribute is not marked as dirty, doing so is up to the caller.
     */
    private UiAttributeNode addUnknownAttribute(String xmlFullName,
            String xmlAttrLocalName, String xmlNsUri) {
        // Create a new unknown attribute of format string
        TextAttributeDescriptor desc = new TextAttributeDescriptor(
                xmlAttrLocalName,           // xml name
                xmlNsUri,                // ui name
                new AttributeInfo(xmlAttrLocalName, Format.STRING_SET)
                );
        UiAttributeNode uiAttr = desc.createUiNode(this);
        mUnknownUiAttributes.add(uiAttr);
        mCachedAllUiAttributes = null;
        return uiAttr;
    }

    /**
     * Invoke all registered {@link IUiUpdateListener} listening on this UI update for this node.
     */
    protected void invokeUiUpdateListeners(UiUpdateState state) {
        if (mUiUpdateListeners != null) {
            for (IUiUpdateListener listener : mUiUpdateListeners) {
                try {
                    listener.uiElementNodeUpdated(this, state);
                } catch (Exception e) {
                    // prevent a crashing listener from crashing the whole invocation chain
                    AdtPlugin.log(e, "UIElement Listener failed: %s, state=%s",  //$NON-NLS-1$
                            getBreadcrumbTrailDescription(true),
                            state.toString());
                }
            }
        }
    }

    // --- for derived implementations only ---

    @VisibleForTesting
    public void setXmlNode(Node xmlNode) {
        mXmlNode = xmlNode;
    }

    public void refreshUi() {
        invokeUiUpdateListeners(UiUpdateState.ATTR_UPDATED);
    }


    // ------------- Helpers

    /**
     * Helper method to commit a single attribute value to XML.
     * <p/>
     * This method updates the XML regardless of the current XML value.
     * Callers should check first if an update is needed.
     * If the new value is empty, the XML attribute will be actually removed.
     * <p/>
     * Note that the caller MUST ensure that modifying the underlying XML model is
     * safe and must take care of marking the model as dirty if necessary.
     *
     * @see AndroidXmlEditor#wrapEditXmlModel(Runnable)
     *
     * @param uiAttr The attribute node to commit. Must be a child of this UiElementNode.
     * @param newValue The new value to set.
     * @return True if the XML attribute was modified or removed, false if nothing changed.
     */
    public boolean commitAttributeToXml(UiAttributeNode uiAttr, String newValue) {
        // Get (or create) the underlying XML element node that contains the attributes.
        Node element = prepareCommit();
        if (element != null && uiAttr != null) {
            String attrLocalName = uiAttr.getDescriptor().getXmlLocalName();
            String attrNsUri = uiAttr.getDescriptor().getNamespaceUri();

            NamedNodeMap attrMap = element.getAttributes();
            if (newValue == null || newValue.length() == 0) {
                // Remove attribute if it's empty
                if (attrMap.getNamedItemNS(attrNsUri, attrLocalName) != null) {
                    attrMap.removeNamedItemNS(attrNsUri, attrLocalName);
                    return true;
                }
            } else {
                // Add or replace an attribute
                Document doc = element.getOwnerDocument();
                if (doc != null) {
                    Attr attr;
                    if (attrNsUri != null && attrNsUri.length() > 0) {
                        attr = (Attr) attrMap.getNamedItemNS(attrNsUri, attrLocalName);
                        if (attr == null) {
                            attr = doc.createAttributeNS(attrNsUri, attrLocalName);
                            attr.setPrefix(XmlUtils.lookupNamespacePrefix(element, attrNsUri));
                            attrMap.setNamedItemNS(attr);
                        }
                    } else {
                        attr = (Attr) attrMap.getNamedItem(attrLocalName);
                        if (attr == null) {
                            attr = doc.createAttribute(attrLocalName);
                            attrMap.setNamedItem(attr);
                        }
                    }
                    attr.setValue(newValue);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Helper method to commit all dirty attributes values to XML.
     * <p/>
     * This method is useful if {@link #setAttributeValue(String, String, String, boolean)} has
     * been called more than once and all the attributes marked as dirty must be committed to
     * the XML. It calls {@link #commitAttributeToXml(UiAttributeNode, String)} on each dirty
     * attribute.
     * <p/>
     * Note that the caller MUST ensure that modifying the underlying XML model is
     * safe and must take care of marking the model as dirty if necessary.
     *
     * @see AndroidXmlEditor#wrapEditXmlModel(Runnable)
     *
     * @return True if one or more values were actually modified or removed,
     *         false if nothing changed.
     */
    @SuppressWarnings("null") // Eclipse is confused by the logic and gets it wrong
    public boolean commitDirtyAttributesToXml() {
        boolean result = false;
        List<UiAttributeNode> dirtyAttributes = new ArrayList<UiAttributeNode>();
        for (UiAttributeNode uiAttr : getAllUiAttributes()) {
            if (uiAttr.isDirty()) {
                String value = uiAttr.getCurrentValue();
                if (value != null && value.length() > 0) {
                    // Defer the new attributes: set these last and in order
                    dirtyAttributes.add(uiAttr);
                } else {
                    result |= commitAttributeToXml(uiAttr, value);
                    uiAttr.setDirty(false);
                }
            }
        }
        if (dirtyAttributes.size() > 0) {
            result = true;

            Collections.sort(dirtyAttributes);

            // The Eclipse XML model will *always* append new attributes.
            // Therefore, if any of the dirty attributes are new, they will appear
            // after any existing, clean attributes on the element. To fix this,
            // we need to first remove any of these attributes, then insert them
            // back in the right order.
            Node element = prepareCommit();
            if (element == null) {
                return result;
            }

            if (AdtPrefs.getPrefs().getFormatGuiXml() && getEditor().supportsFormatOnGuiEdit()) {
                // If auto formatting, don't bother with attribute sorting here since the
                // order will be corrected as soon as the edit is committed anyway
                for (UiAttributeNode uiAttribute : dirtyAttributes) {
                    commitAttributeToXml(uiAttribute, uiAttribute.getCurrentValue());
                    uiAttribute.setDirty(false);
                }

                return result;
            }

            AttributeDescriptor descriptor = dirtyAttributes.get(0).getDescriptor();
            String firstName = descriptor.getXmlLocalName();
            String firstNamePrefix = null;
            String namespaceUri = descriptor.getNamespaceUri();
            if (namespaceUri != null) {
                firstNamePrefix = XmlUtils.lookupNamespacePrefix(element, namespaceUri);
            }
            NamedNodeMap attributes = ((Element) element).getAttributes();
            List<Attr> move = new ArrayList<Attr>();
            for (int i = 0, n = attributes.getLength(); i < n; i++) {
                Attr attribute = (Attr) attributes.item(i);
                if (XmlAttributeSortOrder.compareAttributes(
                        attribute.getPrefix(), attribute.getLocalName(),
                        firstNamePrefix, firstName) > 0) {
                    move.add(attribute);
                }
            }

            for (Attr attribute : move) {
                if (attribute.getNamespaceURI() != null) {
                    attributes.removeNamedItemNS(attribute.getNamespaceURI(),
                            attribute.getLocalName());
                } else {
                    attributes.removeNamedItem(attribute.getName());
                }
            }

            // Merge back the removed DOM attribute nodes and the new UI attribute nodes.
            // In cases where the attribute DOM name and the UI attribute names equal,
            // skip the DOM nodes and just apply the UI attributes.
            int domAttributeIndex = 0;
            int domAttributeIndexMax = move.size();
            int uiAttributeIndex = 0;
            int uiAttributeIndexMax = dirtyAttributes.size();

            while (true) {
                Attr domAttribute;
                UiAttributeNode uiAttribute;

                int compare;
                if (uiAttributeIndex < uiAttributeIndexMax) {
                    if (domAttributeIndex < domAttributeIndexMax) {
                        domAttribute = move.get(domAttributeIndex);
                        uiAttribute = dirtyAttributes.get(uiAttributeIndex);

                        String domAttributeName = domAttribute.getLocalName();
                        String uiAttributeName = uiAttribute.getDescriptor().getXmlLocalName();
                        compare = XmlAttributeSortOrder.compareAttributes(domAttributeName,
                                uiAttributeName);
                    } else {
                        compare = 1;
                        uiAttribute = dirtyAttributes.get(uiAttributeIndex);
                        domAttribute = null;
                    }
                } else if (domAttributeIndex < domAttributeIndexMax) {
                    compare = -1;
                    domAttribute = move.get(domAttributeIndex);
                    uiAttribute = null;
                } else {
                    break;
                }

                if (compare < 0) {
                    if (domAttribute.getNamespaceURI() != null) {
                        attributes.setNamedItemNS(domAttribute);
                    } else {
                        attributes.setNamedItem(domAttribute);
                    }
                    domAttributeIndex++;
                } else {
                    assert compare >= 0;
                    if (compare == 0) {
                        domAttributeIndex++;
                    }
                    commitAttributeToXml(uiAttribute, uiAttribute.getCurrentValue());
                    uiAttribute.setDirty(false);
                    uiAttributeIndex++;
                }
            }
        }

        return result;
    }

    /**
     * Utility method to internally set the value of a text attribute for the current
     * UiElementNode.
     * <p/>
     * This method is a helper. It silently ignores the errors such as the requested
     * attribute not being present in the element or attribute not being settable.
     * It accepts inherited attributes (such as layout).
     * <p/>
     * This does not commit to the XML model. It does mark the attribute node as dirty.
     * This is up to the caller.
     *
     * @see #commitAttributeToXml(UiAttributeNode, String)
     * @see #commitDirtyAttributesToXml()
     *
     * @param attrXmlName The XML <em>local</em> name of the attribute to modify
     * @param attrNsUri The namespace URI of the attribute.
     *                  Can be null if the attribute uses the global namespace.
     * @param value The new value for the attribute. If set to null, the attribute is removed.
     * @param override True if the value must be set even if one already exists.
     * @return The {@link UiAttributeNode} that has been modified or null.
     */
    public UiAttributeNode setAttributeValue(
            String attrXmlName,
            String attrNsUri,
            String value,
            boolean override) {
        if (value == null) {
            value = ""; //$NON-NLS-1$ -- this removes an attribute
        }

        getEditor().scheduleNodeReformat(this, true);

        // Try with all internal attributes
        UiAttributeNode uiAttr = setInternalAttrValue(
                getAllUiAttributes(), attrXmlName, attrNsUri, value, override);
        if (uiAttr != null) {
            return uiAttr;
        }

        if (uiAttr == null) {
            // Failed to find the attribute. For non-android attributes that is mostly expected,
            // in which case we just create a new custom one. As a side effect, we'll find the
            // attribute descriptor via getAllUiAttributes().
            addUnknownAttribute(attrXmlName, attrXmlName, attrNsUri);

            // We've created the attribute, but not actually set the value on it, so let's do it.
            // Try with the updated internal attributes.
            // Implementation detail: we could just do a setCurrentValue + setDirty on the
            // uiAttr returned by addUnknownAttribute(); however going through setInternalAttrValue
            // means we won't duplicate the logic, at the expense of doing one more lookup.
            uiAttr = setInternalAttrValue(
                    getAllUiAttributes(), attrXmlName, attrNsUri, value, override);
        }

        return uiAttr;
    }

    private UiAttributeNode setInternalAttrValue(
            Collection<UiAttributeNode> attributes,
            String attrXmlName,
            String attrNsUri,
            String value,
            boolean override) {

        // For namespace less attributes (like the "layout" attribute of an <include> tag
        // we may be passed "" as the namespace (during an attribute copy), and it
        // should really be null instead.
        if (attrNsUri != null && attrNsUri.length() == 0) {
            attrNsUri = null;
        }

        for (UiAttributeNode uiAttr : attributes) {
            AttributeDescriptor uiDesc = uiAttr.getDescriptor();

            if (uiDesc.getXmlLocalName().equals(attrXmlName)) {
                // Both NS URI must be either null or equal.
                if ((attrNsUri == null && uiDesc.getNamespaceUri() == null) ||
                        (attrNsUri != null && attrNsUri.equals(uiDesc.getNamespaceUri()))) {

                    // Not all attributes are editable, ignore those which are not.
                    if (uiAttr instanceof IUiSettableAttributeNode) {
                        String current = uiAttr.getCurrentValue();
                        // Only update (and mark as dirty) if the attribute did not have any
                        // value or if the value was different.
                        if (override || current == null || !current.equals(value)) {
                            ((IUiSettableAttributeNode) uiAttr).setCurrentValue(value);
                            // mark the attribute as dirty since their internal content
                            // as been modified, but not the underlying XML model
                            uiAttr.setDirty(true);
                            return uiAttr;
                        }
                    }

                    // We found the attribute but it's not settable. Since attributes are
                    // not duplicated, just abandon here.
                    break;
                }
            }
        }

        return null;
    }

    /**
     * Utility method to retrieve the internal value of an attribute.
     * <p/>
     * Note that this retrieves the *field* value if the attribute has some UI, and
     * not the actual XML value. They may differ if the attribute is dirty.
     *
     * @param attrXmlName The XML name of the attribute to modify
     * @return The current internal value for the attribute or null in case of error.
     */
    public String getAttributeValue(String attrXmlName) {
        HashMap<AttributeDescriptor, UiAttributeNode> attributeMap = getInternalUiAttributes();

        for (Entry<AttributeDescriptor, UiAttributeNode> entry : attributeMap.entrySet()) {
            AttributeDescriptor uiDesc = entry.getKey();
            if (uiDesc.getXmlLocalName().equals(attrXmlName)) {
                UiAttributeNode uiAttr = entry.getValue();
                return uiAttr.getCurrentValue();
            }
        }
        return null;
    }

    // ------ IPropertySource methods

    @Override
    public Object getEditableValue() {
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.views.properties.IPropertySource#getPropertyDescriptors()
     *
     * Returns the property descriptor for this node. Since the descriptors are not linked to the
     * data, the AttributeDescriptor are used directly.
     */
    @Override
    public IPropertyDescriptor[] getPropertyDescriptors() {
        List<IPropertyDescriptor> propDescs = new ArrayList<IPropertyDescriptor>();

        // get the standard descriptors
        HashMap<AttributeDescriptor, UiAttributeNode> attributeMap = getInternalUiAttributes();
        Set<AttributeDescriptor> keys = attributeMap.keySet();


        // we only want the descriptor that do implement the IPropertyDescriptor interface.
        for (AttributeDescriptor key : keys) {
            if (key instanceof IPropertyDescriptor) {
                propDescs.add((IPropertyDescriptor)key);
            }
        }

        // now get the descriptor from the unknown attributes
        for (UiAttributeNode unknownNode : mUnknownUiAttributes) {
            if (unknownNode.getDescriptor() instanceof IPropertyDescriptor) {
                propDescs.add((IPropertyDescriptor)unknownNode.getDescriptor());
            }
        }

        // TODO cache this maybe, as it's not going to change (except for unknown descriptors)
        return propDescs.toArray(new IPropertyDescriptor[propDescs.size()]);
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.views.properties.IPropertySource#getPropertyValue(java.lang.Object)
     *
     * Returns the value of a given property. The id is the result of IPropertyDescriptor.getId(),
     * which return the AttributeDescriptor itself.
     */
    @Override
    public Object getPropertyValue(Object id) {
        HashMap<AttributeDescriptor, UiAttributeNode> attributeMap = getInternalUiAttributes();

        UiAttributeNode attribute = attributeMap.get(id);

        if (attribute == null) {
            // look for the id in the unknown attributes.
            for (UiAttributeNode unknownAttr : mUnknownUiAttributes) {
                if (id == unknownAttr.getDescriptor()) {
                    return unknownAttr;
                }
            }
        }

        return attribute;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.views.properties.IPropertySource#isPropertySet(java.lang.Object)
     *
     * Returns whether the property is set. In our case this is if the string is non empty.
     */
    @Override
    public boolean isPropertySet(Object id) {
        HashMap<AttributeDescriptor, UiAttributeNode> attributeMap = getInternalUiAttributes();

        UiAttributeNode attribute = attributeMap.get(id);

        if (attribute != null) {
            return attribute.getCurrentValue().length() > 0;
        }

        // look for the id in the unknown attributes.
        for (UiAttributeNode unknownAttr : mUnknownUiAttributes) {
            if (id == unknownAttr.getDescriptor()) {
                return unknownAttr.getCurrentValue().length() > 0;
            }
        }

        return false;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.views.properties.IPropertySource#resetPropertyValue(java.lang.Object)
     *
     * Reset the property to its default value. For now we simply empty it.
     */
    @Override
    public void resetPropertyValue(Object id) {
        HashMap<AttributeDescriptor, UiAttributeNode> attributeMap = getInternalUiAttributes();

        UiAttributeNode attribute = attributeMap.get(id);
        if (attribute != null) {
            // TODO: reset the value of the attribute

            return;
        }

        // look for the id in the unknown attributes.
        for (UiAttributeNode unknownAttr : mUnknownUiAttributes) {
            if (id == unknownAttr.getDescriptor()) {
                // TODO: reset the value of the attribute

                return;
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.views.properties.IPropertySource#setPropertyValue(java.lang.Object, java.lang.Object)
     *
     * Set the property value. id is the result of IPropertyDescriptor.getId(), which is the
     * AttributeDescriptor itself. Value should be a String.
     */
    @Override
    public void setPropertyValue(Object id, Object value) {
        HashMap<AttributeDescriptor, UiAttributeNode> attributeMap = getInternalUiAttributes();

        UiAttributeNode attribute = attributeMap.get(id);

        if (attribute == null) {
            // look for the id in the unknown attributes.
            for (UiAttributeNode unknownAttr : mUnknownUiAttributes) {
                if (id == unknownAttr.getDescriptor()) {
                    attribute = unknownAttr;
                    break;
                }
            }
        }

        if (attribute != null) {

            // get the current value and compare it to the new value
            String oldValue = attribute.getCurrentValue();
            final String newValue = (String)value;

            if (oldValue.equals(newValue)) {
                return;
            }

            final UiAttributeNode fAttribute = attribute;
            AndroidXmlEditor editor = getEditor();
            editor.wrapEditXmlModel(new Runnable() {
                @Override
                public void run() {
                    commitAttributeToXml(fAttribute, newValue);
                }
            });
        }
    }

    /**
     * Returns true if this node is an ancestor (parent, grandparent, and so on)
     * of the given node. Note that a node is not considered an ancestor of
     * itself.
     *
     * @param node the node to test
     * @return true if this node is an ancestor of the given node
     */
    public boolean isAncestorOf(UiElementNode node) {
        node = node.getUiParent();
        while (node != null) {
            if (node == this) {
                return true;
            }
            node = node.getUiParent();
        }
        return false;
    }

    /**
     * Finds the nearest common parent of the two given nodes (which could be one of the
     * two nodes as well)
     *
     * @param node1 the first node to test
     * @param node2 the second node to test
     * @return the nearest common parent of the two given nodes
     */
    public static UiElementNode getCommonAncestor(UiElementNode node1, UiElementNode node2) {
        while (node2 != null) {
            UiElementNode current = node1;
            while (current != null && current != node2) {
                current = current.getUiParent();
            }
            if (current == node2) {
                return current;
            }
            node2 = node2.getUiParent();
        }

        return null;
    }

    // ---- Global node create/delete Listeners ----

    /** List of listeners to be notified of newly created nodes, or null */
    private static List<NodeCreationListener> sListeners;

    /** Notify listeners that a new node has been created */
    private void fireNodeCreated(UiElementNode newChild, int index) {
        // Nothing to do if there aren't any listeners. We don't need to worry about
        // the case where one thread is firing node changes while another is adding a listener
        // (in that case it's still okay for this node firing not to be heard) so perform
        // the check outside of synchronization.
        if (sListeners == null) {
            return;
        }
        synchronized (UiElementNode.class) {
            if (sListeners != null) {
                UiElementNode parent = newChild.getUiParent();
                for (NodeCreationListener listener : sListeners) {
                    listener.nodeCreated(parent, newChild, index);
                }
            }
        }
    }

    /** Notify listeners that a new node has been deleted */
    private void fireNodeDeleted(UiElementNode oldChild, int index) {
        if (sListeners == null) {
            return;
        }
        synchronized (UiElementNode.class) {
            if (sListeners != null) {
                UiElementNode parent = oldChild.getUiParent();
                for (NodeCreationListener listener : sListeners) {
                    listener.nodeDeleted(parent, oldChild, index);
                }
            }
        }
    }

    /**
     * Adds a {@link NodeCreationListener} to be notified when new nodes are created
     *
     * @param listener the listener to be notified
     */
    public static void addNodeCreationListener(NodeCreationListener listener) {
        synchronized (UiElementNode.class) {
            if (sListeners == null) {
                sListeners = new ArrayList<NodeCreationListener>(1);
            }
            sListeners.add(listener);
        }
    }

    /**
     * Removes a {@link NodeCreationListener} from the set of listeners such that it is
     * no longer notified when nodes are created.
     *
     * @param listener the listener to be removed from the notification list
     */
    public static void removeNodeCreationListener(NodeCreationListener listener) {
        synchronized (UiElementNode.class) {
            sListeners.remove(listener);
            if (sListeners.size() == 0) {
                sListeners = null;
            }
        }
    }

    /** Interface implemented by listeners to be notified of newly created nodes */
    public interface NodeCreationListener {
        /**
         * Called when a new child node is created and added to the given parent
         *
         * @param parent the parent of the created node
         * @param child the newly node
         * @param index the index among the siblings of the child <b>after</b>
         *            insertion
         */
        void nodeCreated(UiElementNode parent, UiElementNode child, int index);

        /**
         * Called when a child node is removed from the given parent
         *
         * @param parent the parent of the removed node
         * @param child the removed node
         * @param previousIndex the index among the siblings of the child
         *            <b>before</b> removal
         */
        void nodeDeleted(UiElementNode parent, UiElementNode child, int previousIndex);
    }
}

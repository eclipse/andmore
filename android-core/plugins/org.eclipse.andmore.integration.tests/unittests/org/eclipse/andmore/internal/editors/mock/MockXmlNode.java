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

package org.eclipse.andmore.internal.editors.mock;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.UserDataHandler;

import java.util.HashMap;


/**
 * A mock XML node with only a minimal set of information.
 */
public class MockXmlNode implements Node {

    MockNodeList mNodeList;
    private String mLocalName;
    private String mNamespace;
    private short mNodeType;
    private MockXmlNode mParent;
    private MockXmlNode mPreviousSibling;
    private MockXmlNode mNextSibling;
    private String mAttrValue;
    private MockNamedNodeMap mAttributes;

    // namespace stuff only set in the root node
    /** map from namespace to prefix. */
    private HashMap<String, String> mNsMap = null;

    /**
     * Constructs a node from a given children list.
     *
     * @param namespace The namespace of the node or null if none
     * @param localName The XML local node name.
     * @param node_type One of Node.xxx_NODE constants, e.g. Node.ELEMENT_NODE
     * @param children The children list. Can be null.
     */
    public MockXmlNode(String namespace, String localName, short node_type,
            MockXmlNode[] children) {
        mLocalName = localName;
        mNamespace = namespace;
        mNodeType = node_type;
        mNodeList = new MockNodeList(children);
        fixNavigation();
    }

    /**
     * Constructs an attribute node
     *
     * @param namespace The namespace of the node or null if none
     * @param localName The XML local node name.
     * @param value the value of the attribute
     */
    public MockXmlNode(String namespace, String localName, String value) {
        mLocalName = localName;
        mNamespace = namespace;
        mAttrValue = value;
        mNodeType = Node.ATTRIBUTE_NODE;
        mNodeList = new MockNodeList(new MockXmlNode[0]);
        fixNavigation();
    }

    private void fixNavigation() {
        MockXmlNode prev = null;
        for (MockXmlNode n : mNodeList.getArrayList()) {
            n.mParent = this;
            n.mPreviousSibling = prev;
            if (prev != null) {
                prev.mNextSibling = n;
            }
            n.fixNavigation();
            prev = n;
        }
    }

    public void addAttributes(String namespaceURI, String localName, String value) {
        if (mAttributes == null) {
            mAttributes = new MockNamedNodeMap();
        }

        MockXmlNode node = mAttributes.addAttribute(namespaceURI, localName, value);
        node.mParent = this;
    }

    public void setPrefix(String namespace, String prefix) {
        if (mNsMap == null) {
            mNsMap = new HashMap<String, String>();
        }

        mNsMap.put(namespace, prefix);
    }

    public String getPrefix(String namespace) {
        if (mNsMap != null) {
            return mNsMap.get(namespace);
        }

        return mParent.getPrefix(namespace);
    }


    // ----------- Node methods

    @Override
    public Node appendChild(Node newChild) throws DOMException {
        mNodeList.getArrayList().add((MockXmlNode) newChild);
        return newChild;
    }

    @Override
    public NamedNodeMap getAttributes() {
        return mAttributes;
    }

    @Override
    public NodeList getChildNodes() {
        return mNodeList;
    }

    @Override
    public Node getFirstChild() {
        if (mNodeList.getLength() > 0) {
            return mNodeList.item(0);
        }
        return null;
    }

    @Override
    public Node getLastChild() {
        if (mNodeList.getLength() > 0) {
            return mNodeList.item(mNodeList.getLength() - 1);
        }
        return null;
    }

    @Override
    public Node getNextSibling() {
        return mNextSibling;
    }

    @Override
    public String getNodeName() {
        return mLocalName;
    }

    @Override
    public String getLocalName() {
        return mLocalName;
    }

    @Override
    public short getNodeType() {
        return mNodeType;
    }

    @Override
    public Node getParentNode() {
        return mParent;
    }

    @Override
    public Node getPreviousSibling() {
        return mPreviousSibling;
    }

    @Override
    public boolean hasChildNodes() {
        return mNodeList.getLength() > 0;
    }

    @Override
    public boolean hasAttributes() {
        throw new UnsupportedOperationException("Operation not implemented.");  //$NON-NLS-1$
    }

    @Override
    public boolean isSameNode(Node other) {
        return this == other;
    }

    @Override
    public String getNodeValue() throws DOMException {
        return mAttrValue;
    }

    @Override
    public String getPrefix() {
        return getPrefix(getNamespaceURI());
    }

    @Override
    public String getNamespaceURI() {
        return mNamespace;
    }


    // --- methods not implemented ---

    @Override
    public Node cloneNode(boolean deep) {
        throw new UnsupportedOperationException("Operation not implemented.");  //$NON-NLS-1$
    }

    @Override
    public short compareDocumentPosition(Node other) throws DOMException {
        throw new UnsupportedOperationException("Operation not implemented.");  //$NON-NLS-1$
    }

    @Override
    public String getBaseURI() {
        throw new UnsupportedOperationException("Operation not implemented.");  //$NON-NLS-1$
    }

    @Override
    public Object getFeature(String feature, String version) {
        throw new UnsupportedOperationException("Operation not implemented.");  //$NON-NLS-1$
    }

    @Override
    public Document getOwnerDocument() {
        throw new UnsupportedOperationException("Operation not implemented.");  //$NON-NLS-1$
    }

    @Override
    public String getTextContent() throws DOMException {
        throw new UnsupportedOperationException("Operation not implemented.");  //$NON-NLS-1$
    }

    @Override
    public Object getUserData(String key) {
        throw new UnsupportedOperationException("Operation not implemented.");  //$NON-NLS-1$
    }

    @Override
    public Node insertBefore(Node newChild, Node refChild)
            throws DOMException {
        throw new UnsupportedOperationException("Operation not implemented.");  //$NON-NLS-1$
    }

    @Override
    public boolean isDefaultNamespace(String namespaceURI) {
        throw new UnsupportedOperationException("Operation not implemented.");  //$NON-NLS-1$
    }

    @Override
    public boolean isEqualNode(Node arg) {
        throw new UnsupportedOperationException("Operation not implemented.");  //$NON-NLS-1$
    }

    @Override
    public boolean isSupported(String feature, String version) {
        throw new UnsupportedOperationException("Operation not implemented.");  //$NON-NLS-1$
    }

    @Override
    public String lookupNamespaceURI(String prefix) {
        throw new UnsupportedOperationException("Operation not implemented.");  //$NON-NLS-1$
    }

    @Override
    public String lookupPrefix(String namespaceURI) {
        throw new UnsupportedOperationException("Operation not implemented.");  //$NON-NLS-1$
    }

    @Override
    public void normalize() {
        throw new UnsupportedOperationException("Operation not implemented.");  //$NON-NLS-1$
    }

    @Override
    public Node removeChild(Node oldChild) throws DOMException {
        throw new UnsupportedOperationException("Operation not implemented.");  //$NON-NLS-1$
    }

    @Override
    public Node replaceChild(Node newChild, Node oldChild)
            throws DOMException {
        throw new UnsupportedOperationException("Operation not implemented.");  //$NON-NLS-1$
    }

    @Override
    public void setNodeValue(String nodeValue) throws DOMException {
        throw new UnsupportedOperationException("Operation not implemented.");  //$NON-NLS-1$
    }

    @Override
    public void setPrefix(String prefix) throws DOMException {
        throw new UnsupportedOperationException("Operation not implemented.");  //$NON-NLS-1$
    }

    @Override
    public void setTextContent(String textContent) throws DOMException {
        throw new UnsupportedOperationException("Operation not implemented.");  //$NON-NLS-1$
    }

    @Override
    public Object setUserData(String key, Object data,
            UserDataHandler handler) {
        throw new UnsupportedOperationException("Operation not implemented.");  //$NON-NLS-1$
    }
}

/*
 * Copyright (C) 2010 The Android Open Source Project
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
package org.eclipse.andmore.common.layout;

import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ANDROID_WIDGET_PREFIX;
import static com.android.SdkConstants.ATTR_ID;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.api.IAttributeInfo;
import com.android.ide.common.api.INode;
import com.android.ide.common.api.INodeHandler;
import com.android.ide.common.api.Margins;
import com.android.ide.common.api.Rect;
import com.android.ide.common.xml.XmlFormatStyle;
import com.android.ide.common.xml.XmlPrettyPrinter;
import com.google.common.base.Splitter;

import org.eclipse.andmore.internal.editors.formatting.EclipseXmlFormatPreferences;
import org.eclipse.andmore.internal.editors.formatting.EclipseXmlPrettyPrinter;
import org.eclipse.andmore.internal.editors.layout.gle2.DomUtilities;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Test/mock implementation of {@link INode} */
@SuppressWarnings("javadoc")
public class TestNode implements INode {
    private TestNode mParent;

    private final List<TestNode> mChildren = new ArrayList<TestNode>();

    private final String mFqcn;

    private Rect mBounds = new Rect(); // Invalid bounds initially

    private Map<String, IAttribute> mAttributes = new HashMap<String, IAttribute>();

    private Map<String, IAttributeInfo> mAttributeInfos = new HashMap<String, IAttributeInfo>();

    private List<String> mAttributeSources;

    public TestNode(String fqcn) {
        this.mFqcn = fqcn;
    }

    public TestNode bounds(Rect bounds) {
        this.mBounds = bounds;

        return this;
    }

    public TestNode id(String id) {
        return set(ANDROID_URI, ATTR_ID, id);
    }

    public TestNode set(String uri, String name, String value) {
        setAttribute(uri, name, value);

        return this;
    }

    public TestNode add(TestNode child) {
        mChildren.add(child);
        child.mParent = this;

        return this;
    }

    public TestNode add(TestNode... children) {
        for (TestNode child : children) {
            mChildren.add(child);
            child.mParent = this;
        }

        return this;
    }

    public static TestNode create(String fcqn) {
        return new TestNode(fcqn);
    }

    public void removeChild(int index) {
        TestNode removed = mChildren.remove(index);
        removed.mParent = null;
    }

    // ==== INODE ====

    @Override
    public @NonNull INode appendChild(@NonNull String viewFqcn) {
        return insertChildAt(viewFqcn, mChildren.size());
    }

    @Override
    public void editXml(@NonNull String undoName, @NonNull INodeHandler callback) {
        callback.handle(this);
    }

    public void putAttributeInfo(String uri, String attrName, IAttributeInfo info) {
        mAttributeInfos.put(uri + attrName, info);
    }

    @Override
    public IAttributeInfo getAttributeInfo(@Nullable String uri, @NonNull String attrName) {
        return mAttributeInfos.get(uri + attrName);
    }

    @Override
    public @NonNull Rect getBounds() {
        return mBounds;
    }

    @Override
    public @NonNull INode[] getChildren() {
        return mChildren.toArray(new INode[mChildren.size()]);
    }

    @Override
    public @NonNull IAttributeInfo[] getDeclaredAttributes() {
        return mAttributeInfos.values().toArray(new IAttributeInfo[mAttributeInfos.size()]);
    }

    @Override
    public @NonNull String getFqcn() {
        return mFqcn;
    }

    @Override
    public @NonNull IAttribute[] getLiveAttributes() {
        return mAttributes.values().toArray(new IAttribute[mAttributes.size()]);
    }

    @Override
    public INode getParent() {
        return mParent;
    }

    @Override
    public INode getRoot() {
        TestNode curr = this;
        while (curr.mParent != null) {
            curr = curr.mParent;
        }

        return curr;
    }

    @Override
    public String getStringAttr(@Nullable String uri, @NonNull String attrName) {
        IAttribute attr = mAttributes.get(uri + attrName);
        if (attr == null) {
            return null;
        }

        return attr.getValue();
    }

    @Override
    public @NonNull INode insertChildAt(@NonNull String viewFqcn, int index) {
        TestNode child = new TestNode(viewFqcn);
        if (index == -1) {
            mChildren.add(child);
        } else {
            mChildren.add(index, child);
        }
        child.mParent = this;
        return child;
    }

    @Override
    public void removeChild(@NonNull INode node) {
        int index = mChildren.indexOf(node);
        if (index != -1) {
            removeChild(index);
        }
    }

    @Override
    public boolean setAttribute(@Nullable String uri, @NonNull String localName,
            @Nullable String value) {
        mAttributes.put(uri + localName, new TestAttribute(uri, localName, value));
        return true;
    }

    @Override
    public String toString() {
        String id = getStringAttr(ANDROID_URI, ATTR_ID);
        return "TestNode [id=" + (id != null ? id : "?") + ", fqn=" + mFqcn + ", infos="
                + mAttributeInfos + ", attributes=" + mAttributes + ", bounds=" + mBounds + "]";
    }

    @Override
    public int getBaseline() {
        return -1;
    }

    @Override
    public @NonNull Margins getMargins() {
        return null;
    }

    @Override
    public @NonNull List<String> getAttributeSources() {
        return mAttributeSources != null ? mAttributeSources : Collections.<String>emptyList();
    }

    public void setAttributeSources(List<String> attributeSources) {
        mAttributeSources = attributeSources;
    }

    /** Create a test node from the given XML */
    public static TestNode createFromXml(String xml) {
        Document document = DomUtilities.parseDocument(xml, false);
        assertNotNull(document);
        assertNotNull(document.getDocumentElement());

        return createFromNode(document.getDocumentElement());
    }

    public static String toXml(TestNode node) {
        assertTrue("This method only works with nodes constructed from XML",
                node instanceof TestXmlNode);
        Document document = ((TestXmlNode) node).mElement.getOwnerDocument();
        // Insert new whitespace nodes etc
        String xml = dumpDocument(document);
        document = DomUtilities.parseDocument(xml, false);

        XmlPrettyPrinter printer = new EclipseXmlPrettyPrinter(EclipseXmlFormatPreferences.create(),
                XmlFormatStyle.LAYOUT, "\n");
        StringBuilder sb = new StringBuilder(1000);
        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        printer.prettyPrint(-1, document, null, null, sb, false);
        return sb.toString();
    }

    @SuppressWarnings("deprecation")
    private static String dumpDocument(Document document) {
        // Diagnostics: print out the XML that we're about to render
        org.apache.xml.serialize.OutputFormat outputFormat =
                new org.apache.xml.serialize.OutputFormat(
                        "XML", "ISO-8859-1", true); //$NON-NLS-1$ //$NON-NLS-2$
        outputFormat.setIndent(2);
        outputFormat.setLineWidth(100);
        outputFormat.setIndenting(true);
        outputFormat.setOmitXMLDeclaration(true);
        outputFormat.setOmitDocumentType(true);
        StringWriter stringWriter = new StringWriter();
        // Using FQN here to avoid having an import above, which will result
        // in a deprecation warning, and there isn't a way to annotate a single
        // import element with a SuppressWarnings.
        org.apache.xml.serialize.XMLSerializer serializer =
                new org.apache.xml.serialize.XMLSerializer(stringWriter, outputFormat);
        serializer.setNamespaces(true);
        try {
            serializer.serialize(document.getDocumentElement());
            return stringWriter.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static TestNode createFromNode(Element element) {
        String fqcn = ANDROID_WIDGET_PREFIX + element.getTagName();
        TestNode node = new TestXmlNode(fqcn, element);

        for (Element child : DomUtilities.getChildren(element)) {
            node.add(createFromNode(child));
        }

        return node;
    }

    @Nullable
    public static TestNode findById(TestNode node, String id) {
        id = BaseViewRule.stripIdPrefix(id);
        return node.findById(id);
    }

    private TestNode findById(String targetId) {
        String id = getStringAttr(ANDROID_URI, ATTR_ID);
        if (id != null && targetId.equals(BaseViewRule.stripIdPrefix(id))) {
            return this;
        }

        for (TestNode child : mChildren) {
            TestNode result = child.findById(targetId);
            if (result != null) {
                return result;
            }
        }

        return null;
    }

    private static String getTagName(String fqcn) {
        return fqcn.substring(fqcn.lastIndexOf('.') + 1);
    }

    private static class TestXmlNode extends TestNode {
        private final Element mElement;

        public TestXmlNode(String fqcn, Element element) {
            super(fqcn);
            mElement = element;
        }

        @Override
        public @NonNull IAttribute[] getLiveAttributes() {
            List<IAttribute> result = new ArrayList<IAttribute>();

            NamedNodeMap attributes = mElement.getAttributes();
            for (int i = 0, n = attributes.getLength(); i < n; i++) {
                Attr attribute = (Attr) attributes.item(i);
                result.add(new TestXmlAttribute(attribute));
            }
            return result.toArray(new IAttribute[result.size()]);
        }

        @Override
        public boolean setAttribute(String uri, String localName, String value) {
            if (value == null) {
                mElement.removeAttributeNS(uri, localName);
            } else {
                mElement.setAttributeNS(uri, localName, value);
            }
            return super.setAttribute(uri, localName, value);
        }

        @Override
        public INode appendChild(String viewFqcn) {
            Element child = mElement.getOwnerDocument().createElement(getTagName(viewFqcn));
            mElement.appendChild(child);
            return new TestXmlNode(viewFqcn, child);
        }

        @Override
        public INode insertChildAt(String viewFqcn, int index) {
            if (index == -1) {
                return appendChild(viewFqcn);
            }
            Element child = mElement.getOwnerDocument().createElement(getTagName(viewFqcn));
            List<Element> children = DomUtilities.getChildren(mElement);
            if (children.size() >= index) {
                Element before = children.get(index);
                mElement.insertBefore(child, before);
            } else {
                fail("Unexpected index");
                mElement.appendChild(child);
            }
            return new TestXmlNode(viewFqcn, child);
        }

        @Override
        public String getStringAttr(String uri, String name) {
            String value;
            if (uri == null) {
                value = mElement.getAttribute(name);
            } else {
                value = mElement.getAttributeNS(uri, name);
            }
            if (value.isEmpty()) {
                value = null;
            }

            return value;
        }

        @Override
        public void removeChild(INode node) {
            assert node instanceof TestXmlNode;
            mElement.removeChild(((TestXmlNode) node).mElement);
        }

        @Override
        public void removeChild(int index) {
            List<Element> children = DomUtilities.getChildren(mElement);
            assertTrue(index < children.size());
            Element oldChild = children.get(index);
            mElement.removeChild(oldChild);
        }
    }

    public static class TestXmlAttribute implements IAttribute {
        private Attr mAttribute;

        public TestXmlAttribute(Attr attribute) {
            this.mAttribute = attribute;
        }

        @Override
        public String getUri() {
            return mAttribute.getNamespaceURI();
        }

        @Override
        public String getName() {
            String name = mAttribute.getLocalName();
            if (name == null) {
                name = mAttribute.getName();
            }
            return name;
        }

        @Override
        public String getValue() {
            return mAttribute.getValue();
        }
    }

    // Recursively initialize this node with the bounds specified in the given hierarchy
    // dump (from ViewHierarchy's DUMP_INFO flag
    public void assignBounds(String bounds) {
        Iterable<String> split = Splitter.on('\n').trimResults().split(bounds);
        assignBounds(split.iterator());
    }

    private void assignBounds(Iterator<String> iterator) {
        assertTrue(iterator.hasNext());
        String desc = iterator.next();

        Pattern pattern = Pattern.compile("^\\s*(.+)\\s+\\[(.+)\\]\\s*(<.+>)?\\s*(\\S+)?\\s*$");
        Matcher matcher = pattern.matcher(desc);
        assertTrue(matcher.matches());
        String fqn = matcher.group(1);
        assertEquals(getFqcn(), fqn);
        String boundsString = matcher.group(2);
        String[] bounds = boundsString.split(",");
        assertEquals(boundsString, 4, bounds.length);
        try {
            int left = Integer.parseInt(bounds[0]);
            int top = Integer.parseInt(bounds[1]);
            int right = Integer.parseInt(bounds[2]);
            int bottom = Integer.parseInt(bounds[3]);
            mBounds = new Rect(left, top, right - left, bottom - top);
        } catch (NumberFormatException nufe) {
            fail(nufe.getLocalizedMessage());
        }
        String tag = matcher.group(3);

        for (INode child : getChildren()) {
            assertTrue(iterator.hasNext());
            ((TestNode) child).assignBounds(iterator);
        }
    }
}

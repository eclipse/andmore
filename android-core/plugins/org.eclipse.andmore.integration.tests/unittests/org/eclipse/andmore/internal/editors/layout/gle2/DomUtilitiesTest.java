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
package org.eclipse.andmore.internal.editors.layout.gle2;

import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.TOOLS_URI;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.Arrays;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.TestCase;

@SuppressWarnings("javadoc")
public class DomUtilitiesTest extends TestCase {

    public void testIsEquivalent() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setValidating(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document1 = builder.newDocument();
        Document document2 = builder.newDocument();
        document1.appendChild(document1.createElement("root"));
        document2.appendChild(document2.createElement("root"));

        assertFalse(DomUtilities.isEquivalent(null, null));
        Element root1 = document1.getDocumentElement();
        assertFalse(DomUtilities.isEquivalent(null, root1));
        Element root2 = document2.getDocumentElement();
        assertFalse(DomUtilities.isEquivalent(root2, null));
        assertTrue(DomUtilities.isEquivalent(root1, root2));

        root1.appendChild(document1.createTextNode("    "));
        // Differences in text are NOT significant!
        assertTrue(DomUtilities.isEquivalent(root1, root2));
        root2.appendChild(document2.createTextNode("    "));
        assertTrue(DomUtilities.isEquivalent(root1, root2));

        Element foo1 = document1.createElement("foo");
        Element foo2 = document2.createElement("foo");
        root1.appendChild(foo1);
        assertFalse(DomUtilities.isEquivalent(root1, root2));
        root2.appendChild(foo2);
        assertTrue(DomUtilities.isEquivalent(root1, root2));

        root1.appendChild(document1.createElement("bar"));
        assertFalse(DomUtilities.isEquivalent(root1, root2));
        root2.appendChild(document2.createElement("bar"));
        assertTrue(DomUtilities.isEquivalent(root1, root2));

        // Add attributes in opposite order
        foo1.setAttribute("attribute1", "value1");
        foo1.setAttribute("attribute2", "value2");
        assertFalse(DomUtilities.isEquivalent(root1, root2));
        foo2.setAttribute("attribute2", "value2");
        foo2.setAttribute("attribute1", "valueWrong");
        assertFalse(DomUtilities.isEquivalent(root1, root2));
        foo2.setAttribute("attribute1", "value1");
        assertTrue(DomUtilities.isEquivalent(root1, root2));
        foo2.setAttributeNS(TOOLS_URI, "foo", "bar");
        assertTrue(DomUtilities.isEquivalent(root1, root2));
        foo2.setAttributeNS(ANDROID_URI, "foo", "bar");
        assertFalse(DomUtilities.isEquivalent(root1, root2));

        // TODO - test different tag names
        // TODO - test different name spaces!
    }

    public void testIsContiguous() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setValidating(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.newDocument();
        document.appendChild(document.createElement("root"));
        Element root = document.getDocumentElement();
        root.appendChild(document.createTextNode("    "));
        Element foo = document.createElement("foo");
        root.appendChild(foo);
        root.appendChild(document.createTextNode("    "));
        Element bar = document.createElement("bar");
        root.appendChild(bar);
        Element baz = document.createElement("baz");
        root.appendChild(baz);

        assertTrue(DomUtilities.isContiguous(Arrays.asList(foo)));
        assertTrue(DomUtilities.isContiguous(Arrays.asList(foo, bar)));
        assertTrue(DomUtilities.isContiguous(Arrays.asList(foo, bar, baz)));
        assertTrue(DomUtilities.isContiguous(Arrays.asList(foo, bar, baz)));
        assertTrue(DomUtilities.isContiguous(Arrays.asList(bar, baz, foo)));
        assertTrue(DomUtilities.isContiguous(Arrays.asList(baz, bar, foo)));
        assertTrue(DomUtilities.isContiguous(Arrays.asList(baz, foo, bar)));

        assertFalse(DomUtilities.isContiguous(Arrays.asList(foo, baz)));
        assertFalse(DomUtilities.isContiguous(Arrays.asList(root, baz)));
    }

    public void testGetCommonAncestor() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setValidating(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.newDocument();

        //            A
        //          /   \
        //         B     C
        //              / \
        //             D   E

        document.appendChild(document.createElement("A"));
        Element a = document.getDocumentElement();
        assertSame(a, DomUtilities.getCommonAncestor(a, a));

        Element b = document.createElement("B");
        a.appendChild(b);
        Element c = document.createElement("C");
        a.appendChild(c);
        Element d = document.createElement("D");
        c.appendChild(d);
        Element e = document.createElement("E");
        c.appendChild(e);

        assertSame(a, DomUtilities.getCommonAncestor(a, b));
        assertSame(a, DomUtilities.getCommonAncestor(b, a));
        assertSame(a, DomUtilities.getCommonAncestor(b, c));
        assertSame(a, DomUtilities.getCommonAncestor(b, d));
        assertSame(a, DomUtilities.getCommonAncestor(b, e));
        assertSame(a, DomUtilities.getCommonAncestor(a, e));

        assertSame(c, DomUtilities.getCommonAncestor(d, e));
        assertSame(c, DomUtilities.getCommonAncestor(c, e));
        assertSame(c, DomUtilities.getCommonAncestor(d, c));
        assertSame(c, DomUtilities.getCommonAncestor(c, c));
    }

    public void testGetDepth() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setValidating(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.newDocument();

        //            A
        //          /   \
        //         B     C
        //              / \
        //             D   E

        document.appendChild(document.createElement("A"));
        Element a = document.getDocumentElement();
        assertSame(a, DomUtilities.getCommonAncestor(a, a));
        Element b = document.createElement("B");
        a.appendChild(b);
        Element c = document.createElement("C");
        a.appendChild(c);
        Element d = document.createElement("D");
        c.appendChild(d);
        Element e = document.createElement("E");
        c.appendChild(e);

        assertEquals(0, DomUtilities.getDepth(document));

        assertEquals(1, DomUtilities.getDepth(a));
        assertEquals(2, DomUtilities.getDepth(b));
        assertEquals(2, DomUtilities.getDepth(c));
        assertEquals(3, DomUtilities.getDepth(d));
        assertEquals(3, DomUtilities.getDepth(e));
    }

    public void testHasChildren() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setValidating(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.newDocument();
        assertFalse(DomUtilities.hasElementChildren(document));
        document.appendChild(document.createElement("A"));
        Element a = document.getDocumentElement();
        assertFalse(DomUtilities.hasElementChildren(a));
        a.appendChild(document.createTextNode("foo"));
        assertFalse(DomUtilities.hasElementChildren(a));
        Element b = document.createElement("B");
        a.appendChild(b);
        assertTrue(DomUtilities.hasElementChildren(a));
        assertFalse(DomUtilities.hasElementChildren(b));
    }
}

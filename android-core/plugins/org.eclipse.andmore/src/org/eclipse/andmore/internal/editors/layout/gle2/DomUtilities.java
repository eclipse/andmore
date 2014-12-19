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
import static com.android.SdkConstants.ATTR_ID;
import static com.android.SdkConstants.ID_PREFIX;
import static com.android.SdkConstants.NEW_ID_PREFIX;
import static com.android.SdkConstants.TOOLS_URI;
import static org.eclipse.wst.xml.core.internal.provisional.contenttype.ContentTypeIdForXML.ContentTypeID_XML;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.utils.Pair;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.editors.AndroidXmlEditor;
import org.eclipse.andmore.internal.editors.descriptors.DescriptorsUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.IDocument;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.eclipse.wst.xml.core.internal.regions.DOMRegionContext;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Various utility methods for manipulating DOM nodes.
 */
@SuppressWarnings("restriction") // No replacement for restricted XML model yet
public class DomUtilities {
    /**
     * Finds the nearest common parent of the two given nodes (which could be one of the
     * two nodes as well)
     *
     * @param node1 the first node to test
     * @param node2 the second node to test
     * @return the nearest common parent of the two given nodes
     */
    @Nullable
    public static Node getCommonAncestor(@NonNull Node node1, @NonNull Node node2) {
        while (node2 != null) {
            Node current = node1;
            while (current != null && current != node2) {
                current = current.getParentNode();
            }
            if (current == node2) {
                return current;
            }
            node2 = node2.getParentNode();
        }

        return null;
    }

    /**
     * Returns all elements below the given node (which can be a document,
     * element, etc). This will include the node itself, if it is an element.
     *
     * @param node the node to search from
     * @return all elements in the subtree formed by the node parameter
     */
    @NonNull
    public static List<Element> getAllElements(@NonNull Node node) {
        List<Element> elements = new ArrayList<Element>(64);
        addElements(node, elements);
        return elements;
    }

    private static void addElements(@NonNull Node node, @NonNull List<Element> elements) {
        if (node instanceof Element) {
            elements.add((Element) node);
        }

        NodeList childNodes = node.getChildNodes();
        for (int i = 0, n = childNodes.getLength(); i < n; i++) {
            addElements(childNodes.item(i), elements);
        }
    }

    /**
     * Returns the depth of the given node (with the document node having depth 0,
     * and the document element having depth 1)
     *
     * @param node the node to test
     * @return the depth in the document
     */
    public static int getDepth(@NonNull Node node) {
        int depth = -1;
        while (node != null) {
            depth++;
            node = node.getParentNode();
        }

        return depth;
    }

    /**
     * Returns true if the given node has one or more element children
     *
     * @param node the node to test for element children
     * @return true if the node has one or more element children
     */
    public static boolean hasElementChildren(@NonNull Node node) {
        NodeList children = node.getChildNodes();
        for (int i = 0, n = children.getLength(); i < n; i++) {
            if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns the DOM document for the given file
     *
     * @param file the XML file
     * @return the document, or null if not found or not parsed properly (no
     *         errors are generated/thrown)
     */
    @Nullable
    public static Document getDocument(@NonNull IFile file) {
        IModelManager modelManager = StructuredModelManager.getModelManager();
        if (modelManager == null) {
            return null;
        }
        try {
            IStructuredModel model = modelManager.getExistingModelForRead(file);
            if (model == null) {
                model = modelManager.getModelForRead(file);
            }
            if (model != null) {
                if (model instanceof IDOMModel) {
                    IDOMModel domModel = (IDOMModel) model;
                    return domModel.getDocument();
                }
                try {
                } finally {
                    model.releaseFromRead();
                }
            }
        } catch (Exception e) {
            // Ignore exceptions.
        }

        return null;
    }

    /**
     * Returns the DOM document for the given editor
     *
     * @param editor the XML editor
     * @return the document, or null if not found or not parsed properly (no
     *         errors are generated/thrown)
     */
    @Nullable
    public static Document getDocument(@NonNull AndroidXmlEditor editor) {
        IStructuredModel model = editor.getModelForRead();
        try {
            if (model instanceof IDOMModel) {
                IDOMModel domModel = (IDOMModel) model;
                return domModel.getDocument();
            }
        } finally {
            if (model != null) {
                model.releaseFromRead();
            }
        }

        return null;
    }


    /**
     * Returns the XML DOM node corresponding to the given offset of the given
     * document.
     *
     * @param document The document to look in
     * @param offset The offset to look up the node for
     * @return The node containing the offset, or null
     */
    @Nullable
    public static Node getNode(@NonNull IDocument document, int offset) {
        Node node = null;
        IModelManager modelManager = StructuredModelManager.getModelManager();
        if (modelManager == null) {
            return null;
        }
        try {
            IStructuredModel model = modelManager.getExistingModelForRead(document);
            if (model != null) {
                try {
                    for (; offset >= 0 && node == null; --offset) {
                        node = (Node) model.getIndexedRegion(offset);
                    }
                } finally {
                    model.releaseFromRead();
                }
            }
        } catch (Exception e) {
            // Ignore exceptions.
        }

        return node;
    }

    /**
     * Returns the editing context at the given offset, as a pair of parent node and child
     * node. This is not the same as just calling {@link DomUtilities#getNode} and taking
     * its parent node, because special care has to be taken to return content element
     * positions.
     * <p>
     * For example, for the XML {@code <foo>^</foo>}, if the caret ^ is inside the foo
     * element, between the opening and closing tags, then the foo element is the parent,
     * and the child is null which represents a potential text node.
     * <p>
     * If the node is inside an element tag definition (between the opening and closing
     * bracket) then the child node will be the element and whatever parent (element or
     * document) will be its parent.
     * <p>
     * If the node is in a text node, then the text node will be the child and its parent
     * element or document node its parent.
     * <p>
     * Finally, if the caret is on a boundary of a text node, then the text node will be
     * considered the child, regardless of whether it is on the left or right of the
     * caret. For example, in the XML {@code <foo>^ </foo>} and in the XML
     * {@code <foo> ^</foo>}, in both cases the text node is preferred over the element.
     *
     * @param document the document to search in
     * @param offset the offset to look up
     * @return a pair of parent and child elements, where either the parent or the child
     *         but not both can be null, and if non null the child.getParentNode() should
     *         return the parent. Note that the method can also return null if no
     *         document or model could be obtained or if the offset is invalid.
     */
    @Nullable
    public static Pair<Node, Node> getNodeContext(@NonNull IDocument document, int offset) {
        Node node = null;
        IModelManager modelManager = StructuredModelManager.getModelManager();
        if (modelManager == null) {
            return null;
        }
        try {
            IStructuredModel model = modelManager.getExistingModelForRead(document);
            if (model != null) {
                try {
                    for (; offset >= 0 && node == null; --offset) {
                        IndexedRegion indexedRegion = model.getIndexedRegion(offset);
                        if (indexedRegion != null) {
                            node = (Node) indexedRegion;

                            if (node.getNodeType() == Node.TEXT_NODE) {
                                return Pair.of(node.getParentNode(), node);
                            }

                            // Look at the structured document to see if
                            // we have the special case where the caret is pointing at
                            // a -potential- text node, e.g. <foo>^</foo>
                            IStructuredDocument doc = model.getStructuredDocument();
                            IStructuredDocumentRegion region =
                                doc.getRegionAtCharacterOffset(offset);

                            ITextRegion subRegion = region.getRegionAtCharacterOffset(offset);
                            String type = subRegion.getType();
                            if (DOMRegionContext.XML_END_TAG_OPEN.equals(type)) {
                                // Try to return the text node if it's on the left
                                // of this element node, such that replace strings etc
                                // can be computed.
                                Node lastChild = node.getLastChild();
                                if (lastChild != null) {
                                    IndexedRegion previousRegion = (IndexedRegion) lastChild;
                                    if (previousRegion.getEndOffset() == offset) {
                                        return Pair.of(node, lastChild);
                                    }
                                }
                                return Pair.of(node, null);
                            }

                            return Pair.of(node.getParentNode(), node);
                        }
                    }
                } finally {
                    model.releaseFromRead();
                }
            }
        } catch (Exception e) {
            // Ignore exceptions.
        }

        return null;
    }

    /**
     * Like {@link #getNode(IDocument, int)}, but has a bias parameter which lets you
     * indicate whether you want the search to look forwards or backwards.
     * This is vital when trying to compute a node range. Consider the following
     * XML fragment:
     * {@code
     *    <a/><b/>[<c/><d/><e/>]<f/><g/>
     * }
     * Suppose we want to locate the nodes in the range indicated by the brackets above.
     * If we want to search for the node corresponding to the start position, should
     * we pick the node on its left or the node on its right? Similarly for the end
     * position. Clearly, we'll need to bias the search towards the right when looking
     * for the start position, and towards the left when looking for the end position.
     * The following method lets us do just that. When passed an offset which sits
     * on the edge of the computed node, it will pick the neighbor based on whether
     * "forward" is true or false, where forward means searching towards the right
     * and not forward is obviously towards the left.
     * @param document the document to search in
     * @param offset the offset to search for
     * @param forward if true, search forwards, otherwise search backwards when on node boundaries
     * @return the node which surrounds the given offset, or the node adjacent to the offset
     *    where the side depends on the forward parameter
     */
    @Nullable
    public static Node getNode(@NonNull IDocument document, int offset, boolean forward) {
        Node node = getNode(document, offset);

        if (node instanceof IndexedRegion) {
            IndexedRegion region = (IndexedRegion) node;

            if (!forward && offset <= region.getStartOffset()) {
                Node left = node.getPreviousSibling();
                if (left == null) {
                    left = node.getParentNode();
                }

                node = left;
            } else if (forward && offset >= region.getEndOffset()) {
                Node right = node.getNextSibling();
                if (right == null) {
                    right = node.getParentNode();
                }
                node = right;
            }
        }

        return node;
    }

    /**
     * Returns a range of elements for the given caret range. Note that the two elements
     * may not be at the same level so callers may want to perform additional input
     * filtering.
     *
     * @param document the document to search in
     * @param beginOffset the beginning offset of the range
     * @param endOffset the ending offset of the range
     * @return a pair of begin+end elements, or null
     */
    @Nullable
    public static Pair<Element, Element> getElementRange(@NonNull IDocument document,
            int beginOffset, int endOffset) {
        Element beginElement = null;
        Element endElement = null;
        Node beginNode = getNode(document, beginOffset, true);
        Node endNode = beginNode;
        if (endOffset > beginOffset) {
            endNode = getNode(document, endOffset, false);
        }

        if (beginNode == null || endNode == null) {
            return null;
        }

        // Adjust offsets if you're pointing at text
        if (beginNode.getNodeType() != Node.ELEMENT_NODE) {
            // <foo> <bar1/> | <bar2/> </foo> => should pick <bar2/>
            beginElement = getNextElement(beginNode);
            if (beginElement == null) {
                // Might be inside the end of a parent, e.g.
                // <foo> <bar/> | </foo> => should pick <bar/>
                beginElement = getPreviousElement(beginNode);
                if (beginElement == null) {
                    // We must be inside an empty element,
                    // <foo> | </foo>
                    // In that case just pick the parent.
                    beginElement = getParentElement(beginNode);
                }
            }
        } else {
            beginElement = (Element) beginNode;
        }

        if (endNode.getNodeType() != Node.ELEMENT_NODE) {
            // In the following, | marks the caret position:
            // <foo> <bar1/> | <bar2/> </foo> => should pick <bar1/>
            endElement = getPreviousElement(endNode);
            if (endElement == null) {
                // Might be inside the beginning of a parent, e.g.
                // <foo> | <bar/></foo> => should pick <bar/>
                endElement = getNextElement(endNode);
                if (endElement == null) {
                    // We must be inside an empty element,
                    // <foo> | </foo>
                    // In that case just pick the parent.
                    endElement = getParentElement(endNode);
                }
            }
        } else {
            endElement = (Element) endNode;
        }

        if (beginElement != null && endElement != null) {
            return Pair.of(beginElement, endElement);
        }

        return null;
    }

    /**
     * Returns the next sibling element of the node, or null if there is no such element
     *
     * @param node the starting node
     * @return the next sibling element, or null
     */
    @Nullable
    public static Element getNextElement(@NonNull Node node) {
        while (node != null && node.getNodeType() != Node.ELEMENT_NODE) {
            node = node.getNextSibling();
        }

        return (Element) node; // may be null as well
    }

    /**
     * Returns the previous sibling element of the node, or null if there is no such element
     *
     * @param node the starting node
     * @return the previous sibling element, or null
     */
    @Nullable
    public static Element getPreviousElement(@NonNull Node node) {
        while (node != null && node.getNodeType() != Node.ELEMENT_NODE) {
            node = node.getPreviousSibling();
        }

        return (Element) node; // may be null as well
    }

    /**
     * Returns the closest ancestor element, or null if none
     *
     * @param node the starting node
     * @return the closest parent element, or null
     */
    @Nullable
    public static Element getParentElement(@NonNull Node node) {
        while (node != null && node.getNodeType() != Node.ELEMENT_NODE) {
            node = node.getParentNode();
        }

        return (Element) node; // may be null as well
    }

    /** Utility used by {@link #getFreeWidgetId(Element)} */
    private static void addLowercaseIds(@NonNull Element root, @NonNull Set<String> seen) {
        if (root.hasAttributeNS(ANDROID_URI, ATTR_ID)) {
            String id = root.getAttributeNS(ANDROID_URI, ATTR_ID);
            if (id.startsWith(NEW_ID_PREFIX)) {
                // See getFreeWidgetId for details on locale
                seen.add(id.substring(NEW_ID_PREFIX.length()).toLowerCase(Locale.US));
            } else if (id.startsWith(ID_PREFIX)) {
                seen.add(id.substring(ID_PREFIX.length()).toLowerCase(Locale.US));
            } else {
                seen.add(id.toLowerCase(Locale.US));
            }
        }
    }

    /**
     * Returns a suitable new widget id (not including the {@code @id/} prefix) for the
     * given element, which is guaranteed to be unique in this document
     *
     * @param element the element to compute a new widget id for
     * @param reserved an optional set of extra, "reserved" set of ids that should be
     *            considered taken
     * @param prefix an optional prefix to use for the generated name, or null to get a
     *            default (which is currently the tag name)
     * @return a unique id, never null, which does not include the {@code @id/} prefix
     * @see DescriptorsUtils#getFreeWidgetId
     */
    public static String getFreeWidgetId(
            @NonNull Element element,
            @Nullable Set<String> reserved,
            @Nullable String prefix) {
        Set<String> ids = new HashSet<String>();
        if (reserved != null) {
            for (String id : reserved) {
                // Note that we perform locale-independent lowercase checks; in "Image" we
                // want the lowercase version to be "image", not "?mage" where ? is
                // the char LATIN SMALL LETTER DOTLESS I.

                ids.add(id.toLowerCase(Locale.US));
            }
        }
        addLowercaseIds(element.getOwnerDocument().getDocumentElement(), ids);

        if (prefix == null) {
            prefix = DescriptorsUtils.getBasename(element.getTagName());
        }
        String generated;
        int num = 1;
        do {
            generated = String.format("%1$s%2$d", prefix, num++);   //$NON-NLS-1$
        } while (ids.contains(generated.toLowerCase(Locale.US)));

        return generated;
    }

    /**
     * Returns the element children of the given element
     *
     * @param element the parent element
     * @return a list of child elements, possibly empty but never null
     */
    @NonNull
    public static List<Element> getChildren(@NonNull Element element) {
        // Convenience to avoid lots of ugly DOM access casting
        NodeList children = element.getChildNodes();
        // An iterator would have been more natural (to directly drive the child list
        // iteration) but iterators can't be used in enhanced for loops...
        List<Element> result = new ArrayList<Element>(children.getLength());
        for (int i = 0, n = children.getLength(); i < n; i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element child = (Element) node;
                result.add(child);
            }
        }

        return result;
    }

    /**
     * Returns true iff the given elements are contiguous siblings
     *
     * @param elements the elements to be tested
     * @return true if the elements are contiguous siblings with no gaps
     */
    public static boolean isContiguous(@NonNull List<Element> elements) {
        if (elements.size() > 1) {
            // All elements must be siblings (e.g. same parent)
            Node parent = elements.get(0).getParentNode();
            if (!(parent instanceof Element)) {
                return false;
            }
            for (Element node : elements) {
                if (parent != node.getParentNode()) {
                    return false;
                }
            }

            // Ensure that the siblings are contiguous; no gaps.
            // If we've selected all the children of the parent then we don't need
            // to look.
            List<Element> siblings = DomUtilities.getChildren((Element) parent);
            if (siblings.size() != elements.size()) {
                Set<Element> nodeSet = new HashSet<Element>(elements);
                boolean inRange = false;
                int remaining = elements.size();
                for (Element node : siblings) {
                    boolean in = nodeSet.contains(node);
                    if (in) {
                        remaining--;
                        if (remaining == 0) {
                            break;
                        }
                        inRange = true;
                    } else if (inRange) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * Determines whether two element trees are equivalent. Two element trees are
     * equivalent if they represent the same DOM structure (elements, attributes, and
     * children in order). This is almost the same as simply checking whether the String
     * representations of the two nodes are identical, but this allows for minor
     * variations that are not semantically significant, such as variations in formatting
     * or ordering of the element attribute declarations, and the text children are
     * ignored (this is such that in for example layout where content is only used for
     * indentation the indentation differences are ignored). Null trees are never equal.
     *
     * @param element1 the first element to compare
     * @param element2 the second element to compare
     * @return true if the two element hierarchies are logically equal
     */
    public static boolean isEquivalent(@Nullable Element element1, @Nullable Element element2) {
        if (element1 == null || element2 == null) {
            return false;
        }

        if (!element1.getTagName().equals(element2.getTagName())) {
            return false;
        }

        // Check attribute map
        NamedNodeMap attributes1 = element1.getAttributes();
        NamedNodeMap attributes2 = element2.getAttributes();

        List<Attr> attributeNodes1 = new ArrayList<Attr>();
        for (int i = 0, n = attributes1.getLength(); i < n; i++) {
            Attr attribute = (Attr) attributes1.item(i);
            // Ignore tools uri namespace attributes for equivalency test
            if (TOOLS_URI.equals(attribute.getNamespaceURI())) {
                continue;
            }
            attributeNodes1.add(attribute);
        }
        List<Attr> attributeNodes2 = new ArrayList<Attr>();
        for (int i = 0, n = attributes2.getLength(); i < n; i++) {
            Attr attribute = (Attr) attributes2.item(i);
            // Ignore tools uri namespace attributes for equivalency test
            if (TOOLS_URI.equals(attribute.getNamespaceURI())) {
                continue;
            }
            attributeNodes2.add(attribute);
        }

        if (attributeNodes1.size() != attributeNodes2.size()) {
            return false;
        }

        if (attributes1.getLength() > 0) {
            Collections.sort(attributeNodes1, ATTRIBUTE_COMPARATOR);
            Collections.sort(attributeNodes2, ATTRIBUTE_COMPARATOR);
            for (int i = 0; i < attributeNodes1.size(); i++) {
                Attr attr1 = attributeNodes1.get(i);
                Attr attr2 = attributeNodes2.get(i);
                if (attr1.getLocalName() == null || attr2.getLocalName() == null) {
                    if (!attr1.getName().equals(attr2.getName())) {
                        return false;
                    }
                } else if (!attr1.getLocalName().equals(attr2.getLocalName())) {
                    return false;
                }
                if (!attr1.getValue().equals(attr2.getValue())) {
                    return false;
                }
                if (attr1.getNamespaceURI() == null) {
                    if (attr2.getNamespaceURI() != null) {
                        return false;
                    }
                } else if (attr2.getNamespaceURI() == null) {
                    return false;
                } else if (!attr1.getNamespaceURI().equals(attr2.getNamespaceURI())) {
                    return false;
                }
            }
        }

        NodeList children1 = element1.getChildNodes();
        NodeList children2 = element2.getChildNodes();
        int nextIndex1 = 0;
        int nextIndex2 = 0;
        while (true) {
            while (nextIndex1 < children1.getLength() &&
                    children1.item(nextIndex1).getNodeType() != Node.ELEMENT_NODE) {
                nextIndex1++;
            }

            while (nextIndex2 < children2.getLength() &&
                    children2.item(nextIndex2).getNodeType() != Node.ELEMENT_NODE) {
                nextIndex2++;
            }

            Element nextElement1 = (Element) (nextIndex1 < children1.getLength()
                    ? children1.item(nextIndex1) : null);
            Element nextElement2 = (Element) (nextIndex2 < children2.getLength()
                    ? children2.item(nextIndex2) : null);
            if (nextElement1 == null) {
                return nextElement2 == null;
            } else if (nextElement2 == null) {
                return false;
            } else if (!isEquivalent(nextElement1, nextElement2)) {
                return false;
            }
            nextIndex1++;
            nextIndex2++;
        }
    }

    /**
     * Finds the corresponding element in a document to a given element in another
     * document. Note that this does <b>not</b> do any kind of equivalence check
     * (see {@link #isEquivalent(Element, Element)}), and currently the search
     * is only by id; there is no structural search.
     *
     * @param element the element to find an equivalent for
     * @param document the document to search for an equivalent element in
     * @return an equivalent element, or null
     */
    @Nullable
    public static Element findCorresponding(@NonNull Element element, @NonNull Document document) {
        // Make sure the method is called correctly -- the element is for a different
        // document than the one we are searching
        assert element.getOwnerDocument() != document;

        // First search by id. This allows us to find the corresponding
        String id = element.getAttributeNS(ANDROID_URI, ATTR_ID);
        if (id != null && id.length() > 0) {
            if (id.startsWith(ID_PREFIX)) {
                id = NEW_ID_PREFIX + id.substring(ID_PREFIX.length());
            }

            return findCorresponding(document.getDocumentElement(), id);
        }

        // TODO: Search by structure - look in the document and
        // find a corresponding element in the same location in the structure,
        // e.g. 4th child of root, 3rd child, 6th child, then pick node with tag "foo".

        return null;
    }

    /** Helper method for {@link #findCorresponding(Element, Document)} */
    @Nullable
    private static Element findCorresponding(@NonNull Element element, @NonNull String targetId) {
        String id = element.getAttributeNS(ANDROID_URI, ATTR_ID);
        if (id != null) { // Work around DOM bug
            if (id.equals(targetId)) {
                return element;
            } else if (id.startsWith(ID_PREFIX)) {
                id = NEW_ID_PREFIX + id.substring(ID_PREFIX.length());
                if (id.equals(targetId)) {
                    return element;
                }
            }
        }

        NodeList children = element.getChildNodes();
        for (int i = 0, n = children.getLength(); i < n; i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element child = (Element) node;
                Element match = findCorresponding(child, targetId);
                if (match != null) {
                    return match;
                }
            }
        }

        return null;
    }

    /**
     * Parses the given XML string as a DOM document, using Eclipse's structured
     * XML model (which for example allows us to distinguish empty elements
     * (<foo/>) from elements with no children (<foo></foo>).
     *
     * @param xml the XML content to be parsed (must be well formed)
     * @return the DOM document, or null
     */
    @Nullable
    public static Document parseStructuredDocument(@NonNull String xml) {
        IStructuredModel model = createStructuredModel(xml);
        if (model instanceof IDOMModel) {
            IDOMModel domModel = (IDOMModel) model;
            return domModel.getDocument();
        }

        return null;
    }

    /**
     * Parses the given XML string and builds an Eclipse structured model for it.
     *
     * @param xml the XML content to be parsed (must be well formed)
     * @return the structured model
     */
    @Nullable
    public static IStructuredModel createStructuredModel(@NonNull String xml) {
        IStructuredModel model = createEmptyModel();
        IStructuredDocument document = model.getStructuredDocument();
        model.aboutToChangeModel();
        document.set(xml);
        model.changedModel();

        return model;
    }

    /**
     * Creates an empty Eclipse XML model
     *
     * @return a new Eclipse XML model
     */
    @NonNull
    public static IStructuredModel createEmptyModel() {
        IModelManager modelManager = StructuredModelManager.getModelManager();
        return modelManager.createUnManagedStructuredModelFor(ContentTypeID_XML);
    }

    /**
     * Creates an empty Eclipse XML document
     *
     * @return an empty Eclipse XML document
     */
    @Nullable
    public static Document createEmptyDocument() {
        IStructuredModel model = createEmptyModel();
        if (model instanceof IDOMModel) {
            IDOMModel domModel = (IDOMModel) model;
            return domModel.getDocument();
        }

        return null;
    }

    /**
     * Creates an empty non-Eclipse XML document.
     * This is used when you need to use XML operations not supported by
     * the Eclipse XML model (such as serialization).
     * <p>
     * The new document will not validate, will ignore comments, and will
     * support namespace.
     *
     * @return the new document
     */
    @Nullable
    public static Document createEmptyPlainDocument() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setValidating(false);
        factory.setIgnoringComments(true);
        DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
            return builder.newDocument();
        } catch (ParserConfigurationException e) {
            AdtPlugin.log(e, null);
        }

        return null;
    }

    /**
     * Parses the given XML string as a DOM document, using the JDK parser.
     * The parser does not validate, and is namespace aware.
     *
     * @param xml the XML content to be parsed (must be well formed)
     * @param logParserErrors if true, log parser errors to the log, otherwise
     *            silently return null
     * @return the DOM document, or null
     */
    @Nullable
    public static Document parseDocument(@NonNull String xml, boolean logParserErrors) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        InputSource is = new InputSource(new StringReader(xml));
        factory.setNamespaceAware(true);
        factory.setValidating(false);
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(is);
        } catch (Exception e) {
            if (logParserErrors) {
                AdtPlugin.log(e, null);
            }
        }

        return null;
    }

    /** Can be used to sort attributes by name */
    private static final Comparator<Attr> ATTRIBUTE_COMPARATOR = new Comparator<Attr>() {
        @Override
        public int compare(Attr a1, Attr a2) {
            return a1.getName().compareTo(a2.getName());
        }
    };
}

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

import static com.android.SdkConstants.DOT_LAYOUT_PARAMS;
import static org.eclipse.andmore.AdtConstants.DOC_HIDE;

import com.android.ide.common.api.IAttributeInfo.Format;
import com.android.utils.ILogger;
import com.google.common.collect.Maps;

import org.eclipse.andmore.AdtUtils;
import org.eclipse.andmore.common.resources.platform.ViewClassInfo.LayoutParamsInfo;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Parser for attributes description files.
 */
public final class AttrsXmlParser {

    public static final String ANDROID_MANIFEST_STYLEABLE = "AndroidManifest";  //$NON-NLS-1$

    private Document mDocument;
    private String mOsAttrsXmlPath;

    // all attributes that have the same name are supposed to have the same
    // parameters so we'll keep a cache of them to avoid processing them twice.
    private Map<String, AttributeInfo> mAttributeMap;

    /** Map of all attribute names for a given element */
    private final Map<String, DeclareStyleableInfo> mStyleMap =
        new HashMap<String, DeclareStyleableInfo>();

    /** Map from format name (lower case) to the uppercase version */
    private Map<String, Format> mFormatNames = new HashMap<String, Format>(10);

    /**
     * Map of all (constant, value) pairs for attributes of format enum or flag.
     * E.g. for attribute name=gravity, this tells us there's an enum/flag called "center"
     * with value 0x11.
     */
    private Map<String, Map<String, Integer>> mEnumFlagValues;

    /**
     * A logger object. Must not be null.
     */
    private final ILogger mLog;

    /**
     * Creates a new {@link AttrsXmlParser}, set to load things from the given
     * XML file. Nothing has been parsed yet. Callers should call {@link #preload()}
     * next.
     *
     * @param osAttrsXmlPath The path of the <code>attrs.xml</code> file to parse.
     *              Must not be null. Should point to an existing valid XML document.
     * @param log A logger object. Must not be null.
     * @param expectedAttributeCount expected number of attributes in the file
     */
    public AttrsXmlParser(String osAttrsXmlPath, ILogger log, int expectedAttributeCount) {
        this(osAttrsXmlPath, null /* inheritableAttributes */, log, expectedAttributeCount);
    }

    /**
     * Returns the parsed map of attribute infos
     *
     * @return a map from string name to {@link AttributeInfo}
     */
    public Map<String, AttributeInfo> getAttributeMap() {
        return mAttributeMap;
    }

    /**
     * Creates a new {@link AttrsXmlParser} set to load things from the given
     * XML file.
     * <p/>
     * If inheritableAttributes is non-null, it must point to a preloaded
     * {@link AttrsXmlParser} which attributes will be used for this one. Since
     * already defined attributes are not modifiable, they are thus "inherited".
     *
     * @param osAttrsXmlPath The path of the <code>attrs.xml</code> file to parse.
     *              Must not be null. Should point to an existing valid XML document.
     * @param inheritableAttributes An optional parser with attributes to inherit. Can be null.
     *              If not null, the parser must have had its {@link #preload()} method
     *              invoked prior to being used here.
     * @param log A logger object. Must not be null.
     * @param expectedAttributeCount expected number of attributes in the file
     */
    public AttrsXmlParser(
            String osAttrsXmlPath,
            AttrsXmlParser inheritableAttributes,
            ILogger log,
            int expectedAttributeCount) {
        mOsAttrsXmlPath = osAttrsXmlPath;
        mLog = log;

        assert osAttrsXmlPath != null;
        assert log != null;

        mAttributeMap = Maps.newHashMapWithExpectedSize(expectedAttributeCount);
        if (inheritableAttributes == null) {
            mEnumFlagValues = new HashMap<String, Map<String,Integer>>();
        } else {
            mAttributeMap.putAll(inheritableAttributes.mAttributeMap);
            mEnumFlagValues = new HashMap<String, Map<String,Integer>>(
                                                         inheritableAttributes.mEnumFlagValues);
        }

        // Pre-compute the set of format names such that we don't have to compute the uppercase
        // version of the same format string names again and again
        for (Format f : Format.values()) {
            mFormatNames.put(f.name().toLowerCase(Locale.US), f);
        }
    }

    /**
     * Returns the OS path of the attrs.xml file parsed.
     */
    public String getOsAttrsXmlPath() {
        return mOsAttrsXmlPath;
    }

    /**
     * Preloads the document, parsing all attributes and declared styles.
     *
     * @return Self, for command chaining.
     */
    public AttrsXmlParser preload() {
        Document doc = getDocument();

        if (doc == null) {
            mLog.warning("Failed to find %1$s", //$NON-NLS-1$
                    mOsAttrsXmlPath);
            return this;
        }

        Node res = doc.getFirstChild();
        while (res != null &&
                res.getNodeType() != Node.ELEMENT_NODE &&
                !res.getNodeName().equals("resources")) { //$NON-NLS-1$
            res = res.getNextSibling();
        }

        if (res == null) {
            mLog.warning("Failed to find a <resources> node in %1$s", //$NON-NLS-1$
                    mOsAttrsXmlPath);
            return this;
        }

        parseResources(res);
        return this;
    }

    /**
     * Loads all attributes & javadoc for the view class info based on the class name.
     */
    public void loadViewAttributes(ViewClassInfo info) {
        if (getDocument() != null) {
            String xmlName = info.getShortClassName();
            DeclareStyleableInfo style = mStyleMap.get(xmlName);
            if (style != null) {
                String definedBy = info.getFullClassName();
                AttributeInfo[] attributes = style.getAttributes();
                for (AttributeInfo attribute : attributes) {
                    if (attribute.getDefinedBy() == null) {
                        attribute.setDefinedBy(definedBy);
                    }
                }
                info.setAttributes(attributes);
                info.setJavaDoc(style.getJavaDoc());
            }
        }
    }

    /**
     * Loads all attributes for the layout data info based on the class name.
     */
    public void loadLayoutParamsAttributes(LayoutParamsInfo info) {
        if (getDocument() != null) {
            // Transforms "LinearLayout" and "LayoutParams" into "LinearLayout_Layout".
            ViewClassInfo viewLayoutClass = info.getViewLayoutClass();
            String xmlName = String.format("%1$s_%2$s", //$NON-NLS-1$
                    viewLayoutClass.getShortClassName(),
                    info.getShortClassName());
            xmlName = AdtUtils.stripSuffix(xmlName, "Params"); //$NON-NLS-1$

            DeclareStyleableInfo style = mStyleMap.get(xmlName);
            if (style != null) {
                // For defined by, use the actual class name, e.g.
                //   android.widget.LinearLayout.LayoutParams
                String definedBy = viewLayoutClass.getFullClassName() + DOT_LAYOUT_PARAMS;
                AttributeInfo[] attributes = style.getAttributes();
                for (AttributeInfo attribute : attributes) {
                    if (attribute.getDefinedBy() == null) {
                        attribute.setDefinedBy(definedBy);
                    }
                }
                info.setAttributes(attributes);
            }
        }
    }

    /**
     * Returns a list of all <code>declare-styleable</code> found in the XML file.
     */
    public Map<String, DeclareStyleableInfo> getDeclareStyleableList() {
        return Collections.unmodifiableMap(mStyleMap);
    }

    /**
     * Returns a map of all enum and flag constants sorted by parent attribute name.
     * The map is attribute_name => (constant_name => integer_value).
     */
    public Map<String, Map<String, Integer>> getEnumFlagValues() {
        return mEnumFlagValues;
    }

    //-------------------------

    /**
     * Creates an XML document from the attrs.xml OS path.
     * May return null if the file doesn't exist or cannot be parsed.
     */
    private Document getDocument() {
        if (mDocument == null) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setIgnoringComments(false);
            try {
                DocumentBuilder builder = factory.newDocumentBuilder();
                mDocument = builder.parse(new File(mOsAttrsXmlPath));
            } catch (ParserConfigurationException e) {
                mLog.error(e, "Failed to create XML document builder for %1$s", //$NON-NLS-1$
                        mOsAttrsXmlPath);
            } catch (SAXException e) {
                mLog.error(e, "Failed to parse XML document %1$s", //$NON-NLS-1$
                        mOsAttrsXmlPath);
            } catch (IOException e) {
                mLog.error(e, "Failed to read XML document %1$s", //$NON-NLS-1$
                        mOsAttrsXmlPath);
            }
        }
        return mDocument;
    }

    /**
     * Finds all the &lt;declare-styleable&gt; and &lt;attr&gt; nodes
     * in the top &lt;resources&gt; node.
     */
    private void parseResources(Node res) {

        Map<String, String> unknownParents = new HashMap<String, String>();

        Node lastComment = null;
        for (Node node = res.getFirstChild(); node != null; node = node.getNextSibling()) {
            switch (node.getNodeType()) {
            case Node.COMMENT_NODE:
                lastComment = node;
                break;
            case Node.ELEMENT_NODE:
                if (node.getNodeName().equals("declare-styleable")) {          //$NON-NLS-1$
                    Node nameNode = node.getAttributes().getNamedItem("name"); //$NON-NLS-1$
                    if (nameNode != null) {
                        String name = nameNode.getNodeValue();

                        Node parentNode = node.getAttributes().getNamedItem("parent"); //$NON-NLS-1$
                        String parents = parentNode == null ? null : parentNode.getNodeValue();

                        if (name != null && !mStyleMap.containsKey(name)) {
                            DeclareStyleableInfo style = parseDeclaredStyleable(name, node);
                            if (parents != null) {
                                String[] parentsArray =
                                    parseStyleableParents(parents, mStyleMap, unknownParents);
                                style.setParents(parentsArray);
                            }
                            mStyleMap.put(name, style);
                            unknownParents.remove(name);
                            if (lastComment != null) {
                                String nodeValue = lastComment.getNodeValue();
                                if (nodeValue.contains(DOC_HIDE)) {
                                    mStyleMap.remove(name);
                                } else {
                                    style.setJavaDoc(parseJavadoc(nodeValue));
                                }
                            }
                        }
                    }
                } else if (node.getNodeName().equals("attr")) {                //$NON-NLS-1$
                    parseAttr(node, lastComment);
                }
                lastComment = null;
                break;
            }
        }

        // If we have any unknown parent, re-create synthetic styleable for them.
        for (Entry<String, String> entry : unknownParents.entrySet()) {
            String name = entry.getKey();
            String parent = entry.getValue();

            DeclareStyleableInfo style = new DeclareStyleableInfo(name, (AttributeInfo[])null);
            if (parent != null) {
                style.setParents(new String[] { parent });
            }
            mStyleMap.put(name, style);

            // Simplify parents names. See SDK Bug 3125910.
            // Implementation detail: that since we want to delete and add to the map,
            // we can't just use an iterator.
            for (String key : new ArrayList<String>(mStyleMap.keySet())) {
                if (key.startsWith(name) && !key.equals(name)) {
                    // We found a child which name starts with the full name of the
                    // parent. Simplify the children name.
                    String newName = ANDROID_MANIFEST_STYLEABLE + key.substring(name.length());

                    DeclareStyleableInfo newStyle =
                        new DeclareStyleableInfo(newName, mStyleMap.get(key));
                    mStyleMap.remove(key);
                    mStyleMap.put(newName, newStyle);
                }
            }
        }
    }

    /**
     * Parses the "parents" attribute from a &lt;declare-styleable&gt;.
     * <p/>
     * The syntax is the following:
     * <pre>
     *   parent[.parent]* [[space|,] parent[.parent]* ]
     * </pre>
     * <p/>
     * In English: </br>
     * - There can be one or more parents, separated by whitespace or commas. </br>
     * - Whitespace is ignored and trimmed. </br>
     * - A parent name is actually composed of one or more identifiers joined by a dot.
     * <p/>
     * Styleables do not usually need to declare their parent chain (e.g. the grand-parents
     * of a parent.) Parent names are unique, so in most cases a styleable will only declare
     * its immediate parent.
     * <p/>
     * However it is possible for a styleable's parent to not exist, e.g. if you have a
     * styleable "A" that is the root and then styleable "C" declares its parent to be "A.B".
     * In this case we record "B" as the parent, even though it is unknown and will never be
     * known. Any parent that is currently not in the knownParent map is thus added to the
     * unknownParent set. The caller will remove the name from the unknownParent set when it
     * sees a declaration for it.
     *
     * @param parents The parents string to parse. Must not be null or empty.
     * @param knownParents The map of all declared styles known so far.
     * @param unknownParents A map of all unknown parents collected here.
     * @return The array of terminal parent names parsed from the parents string.
     */
    private String[] parseStyleableParents(String parents,
            Map<String, DeclareStyleableInfo> knownParents,
            Map<String, String> unknownParents) {

        ArrayList<String> result = new ArrayList<String>();

        for (String parent : parents.split("[ \t\n\r\f,|]")) {          //$NON-NLS-1$
            parent = parent.trim();
            if (parent.length() == 0) {
                continue;
            }
            if (parent.indexOf('.') >= 0) {
                // This is a grand-parent/parent chain. Make sure we know about the
                // parents and only record the terminal one.
                String last = null;
                for (String name : parent.split("\\.")) {          //$NON-NLS-1$
                    if (name.length() > 0) {
                        if (!knownParents.containsKey(name)) {
                            // Record this unknown parent and its grand parent.
                            unknownParents.put(name, last);
                        }
                        last = name;
                    }
                }
                parent = last;
            }

            result.add(parent);
        }

        return result.toArray(new String[result.size()]);
    }

    /**
     * Parses an &lt;attr&gt; node and convert it into an {@link AttributeInfo} if it is valid.
     */
    private AttributeInfo parseAttr(Node attrNode, Node lastComment) {
        AttributeInfo info = null;
        Node nameNode = attrNode.getAttributes().getNamedItem("name"); //$NON-NLS-1$
        if (nameNode != null) {
            String name = nameNode.getNodeValue();
            if (name != null) {
                info = mAttributeMap.get(name);
                // If the attribute is unknown yet, parse it.
                // If the attribute is know but its format is unknown, parse it too.
                if (info == null || info.getFormats().size() == 0) {
                    info = parseAttributeTypes(attrNode, name);
                    if (info != null) {
                        mAttributeMap.put(name, info);
                    }
                } else if (lastComment != null) {
                    info = new AttributeInfo(info);
                }
                if (info != null) {
                    if (lastComment != null) {
                        String nodeValue = lastComment.getNodeValue();
                        if (nodeValue.contains(DOC_HIDE)) {
                            return null;
                        }
                        info.setJavaDoc(parseJavadoc(nodeValue));
                        info.setDeprecatedDoc(parseDeprecatedDoc(nodeValue));
                    }
                }
            }
        }
        return info;
    }

    /**
     * Finds all the attributes for a particular style node,
     * e.g. a declare-styleable of name "TextView" or "LinearLayout_Layout".
     *
     * @param styleName The name of the declare-styleable node
     * @param declareStyleableNode The declare-styleable node itself
     */
    private DeclareStyleableInfo parseDeclaredStyleable(String styleName,
            Node declareStyleableNode) {
        ArrayList<AttributeInfo> attrs = new ArrayList<AttributeInfo>();
        Node lastComment = null;
        for (Node node = declareStyleableNode.getFirstChild();
             node != null;
             node = node.getNextSibling()) {

            switch (node.getNodeType()) {
            case Node.COMMENT_NODE:
                lastComment = node;
                break;
            case Node.ELEMENT_NODE:
                if (node.getNodeName().equals("attr")) {                       //$NON-NLS-1$
                    AttributeInfo info = parseAttr(node, lastComment);
                    if (info != null) {
                        attrs.add(info);
                    }
                }
                lastComment = null;
                break;
            }

        }

        return new DeclareStyleableInfo(styleName, attrs.toArray(new AttributeInfo[attrs.size()]));
    }

    /**
     * Returns the {@link AttributeInfo} for a specific <attr> XML node.
     * This gets the javadoc, the type, the name and the enum/flag values if any.
     * <p/>
     * The XML node is expected to have the following attributes:
     * <ul>
     * <li>"name", which is mandatory. The node is skipped if this is missing.</li>
     * <li>"format".</li>
     * </ul>
     * The format may be one type or two types (e.g. "reference|color").
     * An extra format can be implied: "enum" or "flag" are not specified in the "format" attribute,
     * they are implicitly stated by the presence of sub-nodes <enum> or <flag>.
     * <p/>
     * By design, attr nodes of the same name MUST have the same type.
     * Attribute nodes are thus cached by name and reused as much as possible.
     * When reusing a node, it is duplicated and its javadoc reassigned.
     */
    private AttributeInfo parseAttributeTypes(Node attrNode, String name) {
        EnumSet<Format> formats = null;
        String[] enumValues = null;
        String[] flagValues = null;

        Node attrFormat = attrNode.getAttributes().getNamedItem("format"); //$NON-NLS-1$
        if (attrFormat != null) {
            for (String f : attrFormat.getNodeValue().split("\\|")) { //$NON-NLS-1$
                Format format = mFormatNames.get(f);
                if (format == null) {
                    mLog.info(
                        "Unknown format name '%s' in <attr name=\"%s\">, file '%s'.", //$NON-NLS-1$
                        f, name, getOsAttrsXmlPath());
                } else if (format != AttributeInfo.Format.ENUM &&
                        format != AttributeInfo.Format.FLAG) {
                    if (formats == null) {
                        formats = format.asSet();
                    } else {
                        if (formats.size() == 1) {
                            formats = EnumSet.copyOf(formats);
                        }
                        formats.add(format);
                    }
                }
            }
        }

        // does this <attr> have <enum> children?
        enumValues = parseEnumFlagValues(attrNode, "enum", name); //$NON-NLS-1$
        if (enumValues != null) {
            if (formats == null) {
                formats = Format.ENUM_SET;
            } else {
                if (formats.size() == 1) {
                    formats = EnumSet.copyOf(formats);
                }
                formats.add(Format.ENUM);
            }
        }

        // does this <attr> have <flag> children?
        flagValues = parseEnumFlagValues(attrNode, "flag", name); //$NON-NLS-1$
        if (flagValues != null) {
            if (formats == null) {
                formats = Format.FLAG_SET;
            } else {
                if (formats.size() == 1) {
                    formats = EnumSet.copyOf(formats);
                }
                formats.add(Format.FLAG);
            }
        }

        if (formats == null) {
            formats = Format.NONE;
        }

        AttributeInfo info = new AttributeInfo(name, formats);
        info.setEnumValues(enumValues);
        info.setFlagValues(flagValues);
        return info;
    }

    /**
     * Given an XML node that represents an <attr> node, this method searches
     * if the node has any children nodes named "target" (e.g. "enum" or "flag").
     * Such nodes must have a "name" attribute.
     * <p/>
     * If "attrNode" is null, look for any <attr> that has the given attrNode
     * and the requested children nodes.
     * <p/>
     * This method collects all the possible names of these children nodes and
     * return them.
     *
     * @param attrNode The <attr> XML node
     * @param filter The child node to look for, either "enum" or "flag".
     * @param attrName The value of the name attribute of <attr>
     *
     * @return Null if there are no such children nodes, otherwise an array of length >= 1
     *         of all the names of these children nodes.
     */
    private String[] parseEnumFlagValues(Node attrNode, String filter, String attrName) {
        ArrayList<String> names = null;
        for (Node child = attrNode.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equals(filter)) {
                Node nameNode = child.getAttributes().getNamedItem("name");  //$NON-NLS-1$
                if (nameNode == null) {
                    mLog.warning(
                            "Missing name attribute in <attr name=\"%s\"><%s></attr>", //$NON-NLS-1$
                            attrName, filter);
                } else {
                    if (names == null) {
                        names = new ArrayList<String>();
                    }
                    String name = nameNode.getNodeValue();
                    names.add(name);

                    Node valueNode = child.getAttributes().getNamedItem("value");  //$NON-NLS-1$
                    if (valueNode == null) {
                        mLog.warning(
                            "Missing value attribute in <attr name=\"%s\"><%s name=\"%s\"></attr>", //$NON-NLS-1$
                            attrName, filter, name);
                    } else {
                        String value = valueNode.getNodeValue();
                        try {
                            // Integer.decode cannot handle "ffffffff", see JDK issue 6624867
                            int i = (int) (long) Long.decode(value);

                            Map<String, Integer> map = mEnumFlagValues.get(attrName);
                            if (map == null) {
                                map = new HashMap<String, Integer>();
                                mEnumFlagValues.put(attrName, map);
                            }
                            map.put(name, Integer.valueOf(i));

                        } catch(NumberFormatException e) {
                            mLog.error(e,
                                    "Value in <attr name=\"%s\"><%s name=\"%s\" value=\"%s\"></attr> is not a valid decimal or hexadecimal", //$NON-NLS-1$
                                    attrName, filter, name, value);
                        }
                    }
                }
            }
        }
        return names == null ? null : names.toArray(new String[names.size()]);
    }

    /**
     * Parses the javadoc comment.
     * Only keeps the first sentence.
     * <p/>
     * This does not remove nor simplify links and references.
     */
    private String parseJavadoc(String comment) {
        if (comment == null) {
            return null;
        }

        // sanitize & collapse whitespace
        comment = comment.replaceAll("\\s+", " "); //$NON-NLS-1$ //$NON-NLS-2$

        // Explicitly remove any @deprecated tags since they are handled separately.
        comment = comment.replaceAll("(?:\\{@deprecated[^}]*\\}|@deprecated[^@}]*)", "");

        // take everything up to the first dot that is followed by a space or the end of the line.
        // I love regexps :-). For the curious, the regexp is:
        // - start of line
        // - ignore whitespace
        // - group:
        //   - everything, not greedy
        //   - non-capturing group (?: )
        //      - end of string
        //      or
        //      - not preceded by a letter, a dot and another letter (for "i.e" and "e.g" )
        //                            (<! non-capturing zero-width negative look-behind)
        //      - a dot
        //      - followed by a space (?= non-capturing zero-width positive look-ahead)
        // - anything else is ignored
        comment = comment.replaceFirst("^\\s*(.*?(?:$|(?<![a-zA-Z]\\.[a-zA-Z])\\.(?=\\s))).*", "$1"); //$NON-NLS-1$ //$NON-NLS-2$

        return comment;
    }


    /**
     * Parses the javadoc and extract the first @deprecated tag, if any.
     * Returns null if there's no @deprecated tag.
     * The deprecated tag can be of two forms:
     * - {+@deprecated ...text till the next bracket }
     *   Note: there should be no space or + between { and @. I need one in this comment otherwise
     *   this method will be tagged as deprecated ;-)
     * - @deprecated ...text till the next @tag or end of the comment.
     * In both cases the comment can be multi-line.
     */
    private String parseDeprecatedDoc(String comment) {
        // Skip if we can't even find the tag in the comment.
        if (comment == null) {
            return null;
        }

        // sanitize & collapse whitespace
        comment = comment.replaceAll("\\s+", " "); //$NON-NLS-1$ //$NON-NLS-2$

        int pos = comment.indexOf("{@deprecated");
        if (pos >= 0) {
            comment = comment.substring(pos + 12 /* len of {@deprecated */);
            comment = comment.replaceFirst("^([^}]*).*", "$1");
        } else if ((pos = comment.indexOf("@deprecated")) >= 0) {
            comment = comment.substring(pos + 11 /* len of @deprecated */);
            comment = comment.replaceFirst("^(.*?)(?:@.*|$)", "$1");
        } else {
            return null;
        }

        return comment.trim();
    }
}

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

package org.eclipse.andmore.internal.editors.descriptors;

import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_ID;
import static com.android.SdkConstants.ATTR_LAYOUT_BELOW;
import static com.android.SdkConstants.ATTR_LAYOUT_HEIGHT;
import static com.android.SdkConstants.ATTR_LAYOUT_WIDTH;
import static com.android.SdkConstants.ATTR_TEXT;
import static com.android.SdkConstants.EDIT_TEXT;
import static com.android.SdkConstants.EXPANDABLE_LIST_VIEW;
import static com.android.SdkConstants.FQCN_ADAPTER_VIEW;
import static com.android.SdkConstants.GALLERY;
import static com.android.SdkConstants.GRID_LAYOUT;
import static com.android.SdkConstants.GRID_VIEW;
import static com.android.SdkConstants.GT_ENTITY;
import static com.android.SdkConstants.ID_PREFIX;
import static com.android.SdkConstants.LIST_VIEW;
import static com.android.SdkConstants.LT_ENTITY;
import static com.android.SdkConstants.NEW_ID_PREFIX;
import static com.android.SdkConstants.RELATIVE_LAYOUT;
import static com.android.SdkConstants.REQUEST_FOCUS;
import static com.android.SdkConstants.SPACE;
import static com.android.SdkConstants.VALUE_FILL_PARENT;
import static com.android.SdkConstants.VALUE_WRAP_CONTENT;
import static com.android.SdkConstants.VIEW_INCLUDE;
import static com.android.SdkConstants.VIEW_MERGE;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.ide.common.api.IAttributeInfo.Format;
import com.android.resources.ResourceType;

import org.eclipse.andmore.AdtConstants;
import org.eclipse.andmore.common.resources.platform.AttributeInfo;
import org.eclipse.andmore.internal.editors.uimodel.UiDocumentNode;
import org.eclipse.andmore.internal.editors.uimodel.UiElementNode;
import org.eclipse.swt.graphics.Image;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Utility methods related to descriptors handling.
 */
public final class DescriptorsUtils {
    private static final String DEFAULT_WIDGET_PREFIX = "widget";

    private static final int JAVADOC_BREAK_LENGTH = 60;

    /**
     * The path in the online documentation for the manifest description.
     * <p/>
     * This is NOT a complete URL. To be used, it needs to be appended
     * to {@link AdtConstants#CODESITE_BASE_URL} or to the local SDK
     * documentation.
     */
    public static final String MANIFEST_SDK_URL = "/reference/android/R.styleable.html#";  //$NON-NLS-1$

    public static final String IMAGE_KEY = "image"; //$NON-NLS-1$

    private static final String CODE  = "$code";  //$NON-NLS-1$
    private static final String LINK  = "$link";  //$NON-NLS-1$
    private static final String ELEM  = "$elem";  //$NON-NLS-1$
    private static final String BREAK = "$break"; //$NON-NLS-1$

    /**
     * Add all {@link AttributeInfo} to the the array of {@link AttributeDescriptor}.
     *
     * @param attributes The list of {@link AttributeDescriptor} to append to
     * @param elementXmlName Optional XML local name of the element to which attributes are
     *              being added. When not null, this is used to filter overrides.
     * @param nsUri The URI of the attribute. Can be null if attribute has no namespace.
     *              See {@link SdkConstants#NS_RESOURCES} for a common value.
     * @param infos The array of {@link AttributeInfo} to read and append to attributes
     * @param requiredAttributes An optional set of attributes to mark as "required" (i.e. append
     *        a "*" to their UI name as a hint for the user.) If not null, must contains
     *        entries in the form "elem-name/attr-name". Elem-name can be "*".
     * @param overrides A map [attribute name => ITextAttributeCreator creator].
     */
    public static void appendAttributes(List<AttributeDescriptor> attributes,
            String elementXmlName,
            String nsUri, AttributeInfo[] infos,
            Set<String> requiredAttributes,
            Map<String, ITextAttributeCreator> overrides) {
        for (AttributeInfo info : infos) {
            boolean required = false;
            if (requiredAttributes != null) {
                String attr_name = info.getName();
                if (requiredAttributes.contains("*/" + attr_name) ||
                        requiredAttributes.contains(elementXmlName + "/" + attr_name)) {
                    required = true;
                }
            }
            appendAttribute(attributes, elementXmlName, nsUri, info, required, overrides);
        }
    }

    /**
     * Add an {@link AttributeInfo} to the the array of {@link AttributeDescriptor}.
     *
     * @param attributes The list of {@link AttributeDescriptor} to append to
     * @param elementXmlName Optional XML local name of the element to which attributes are
     *              being added. When not null, this is used to filter overrides.
     * @param info The {@link AttributeInfo} to append to attributes
     * @param nsUri The URI of the attribute. Can be null if attribute has no namespace.
     *              See {@link SdkConstants#NS_RESOURCES} for a common value.
     * @param required True if the attribute is to be marked as "required" (i.e. append
     *        a "*" to its UI name as a hint for the user.)
     * @param overrides A map [attribute name => ITextAttributeCreator creator].
     */
    public static void appendAttribute(List<AttributeDescriptor> attributes,
            String elementXmlName,
            String nsUri,
            AttributeInfo info, boolean required,
            Map<String, ITextAttributeCreator> overrides) {
        TextAttributeDescriptor attr = null;

        String xmlLocalName = info.getName();

        // Add the known types to the tooltip
        EnumSet<Format> formats_set = info.getFormats();
        int flen = formats_set.size();
        if (flen > 0) {
            // Create a specialized attribute if we can
            if (overrides != null) {
                for (Entry<String, ITextAttributeCreator> entry: overrides.entrySet()) {
                    // The override key can have the following formats:
                    //   */xmlLocalName
                    //   element/xmlLocalName
                    //   element1,element2,...,elementN/xmlLocalName
                    String key = entry.getKey();
                    String elements[] = key.split("/");          //$NON-NLS-1$
                    String overrideAttrLocalName = null;
                    if (elements.length < 1) {
                        continue;
                    } else if (elements.length == 1) {
                        overrideAttrLocalName = elements[0];
                        elements = null;
                    } else {
                        overrideAttrLocalName = elements[elements.length - 1];
                        elements = elements[0].split(",");       //$NON-NLS-1$
                    }

                    if (overrideAttrLocalName == null ||
                            !overrideAttrLocalName.equals(xmlLocalName)) {
                        continue;
                    }

                    boolean ok_element = elements != null && elements.length < 1;
                    if (!ok_element && elements != null) {
                        for (String element : elements) {
                            if (element.equals("*")              //$NON-NLS-1$
                                    || element.equals(elementXmlName)) {
                                ok_element = true;
                                break;
                            }
                        }
                    }

                    if (!ok_element) {
                        continue;
                    }

                    ITextAttributeCreator override = entry.getValue();
                    if (override != null) {
                        attr = override.create(xmlLocalName, nsUri, info);
                    }
                }
            } // if overrides

            // Create a specialized descriptor if we can, based on type
            if (attr == null) {
                if (formats_set.contains(Format.REFERENCE)) {
                    // This is either a multi-type reference or a generic reference.
                    attr = new ReferenceAttributeDescriptor(
                            xmlLocalName, nsUri, info);
                } else if (formats_set.contains(Format.ENUM)) {
                    attr = new ListAttributeDescriptor(
                            xmlLocalName, nsUri, info);
                } else if (formats_set.contains(Format.FLAG)) {
                    attr = new FlagAttributeDescriptor(
                            xmlLocalName, nsUri, info);
                } else if (formats_set.contains(Format.BOOLEAN)) {
                    attr = new BooleanAttributeDescriptor(
                            xmlLocalName, nsUri, info);
                } else if (formats_set.contains(Format.STRING)) {
                    attr = new ReferenceAttributeDescriptor(
                            ResourceType.STRING, xmlLocalName, nsUri, info);
                }
            }
        }

        // By default a simple text field is used
        if (attr == null) {
            attr = new TextAttributeDescriptor(xmlLocalName, nsUri, info);
        }

        if (required) {
            attr.setRequired(true);
        }

        attributes.add(attr);
    }

    /**
     * Indicates the the given {@link AttributeInfo} already exists in the ArrayList of
     * {@link AttributeDescriptor}. This test for the presence of a descriptor with the same
     * XML name.
     *
     * @param attributes The list of {@link AttributeDescriptor} to compare to.
     * @param nsUri The URI of the attribute. Can be null if attribute has no namespace.
     *              See {@link SdkConstants#NS_RESOURCES} for a common value.
     * @param info The {@link AttributeInfo} to know whether it is included in the above list.
     * @return True if this {@link AttributeInfo} is already present in
     *         the {@link AttributeDescriptor} list.
     */
    public static boolean containsAttribute(ArrayList<AttributeDescriptor> attributes,
            String nsUri,
            AttributeInfo info) {
        String xmlLocalName = info.getName();
        for (AttributeDescriptor desc : attributes) {
            if (desc.getXmlLocalName().equals(xmlLocalName)) {
                if (nsUri == desc.getNamespaceUri() ||
                        (nsUri != null && nsUri.equals(desc.getNamespaceUri()))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Create a pretty attribute UI name from an XML name.
     * <p/>
     * The original xml name starts with a lower case and is camel-case,
     * e.g. "maxWidthForView". The pretty name starts with an upper case
     * and has space separators, e.g. "Max width for view".
     */
    public static String prettyAttributeUiName(String name) {
        if (name.length() < 1) {
            return name;
        }
        StringBuilder buf = new StringBuilder(2 * name.length());

        char c = name.charAt(0);
        // Use upper case initial letter
        buf.append(Character.toUpperCase(c));
        int len = name.length();
        for (int i = 1; i < len; i++) {
            c = name.charAt(i);
            if (Character.isUpperCase(c)) {
                // Break camel case into separate words
                buf.append(' ');
                // Use a lower case initial letter for the next word, except if the
                // word is solely X, Y or Z.
                if (c >= 'X' && c <= 'Z' &&
                        (i == len-1 ||
                            (i < len-1 && Character.isUpperCase(name.charAt(i+1))))) {
                    buf.append(c);
                } else {
                    buf.append(Character.toLowerCase(c));
                }
            } else if (c == '_') {
                buf.append(' ');
            } else {
                buf.append(c);
            }
        }

        name = buf.toString();

        name = replaceAcronyms(name);

        return name;
    }

    /**
     * Similar to {@link #prettyAttributeUiName(String)}, but it will capitalize
     * all words, not just the first one.
     * <p/>
     * The original xml name starts with a lower case and is camel-case, e.g.
     * "maxWidthForView". The corresponding return value is
     * "Max Width For View".
     *
     * @param name the attribute name, which should be a camel case name, e.g.
     *            "maxWidth"
     * @return the corresponding display name, e.g. "Max Width"
     */
    @NonNull
    public static String capitalize(@NonNull String name) {
        if (name.isEmpty()) {
            return name;
        }
        StringBuilder buf = new StringBuilder(2 * name.length());

        char c = name.charAt(0);
        // Use upper case initial letter
        buf.append(Character.toUpperCase(c));
        int len = name.length();
        for (int i = 1; i < len; i++) {
            c = name.charAt(i);
            if (Character.isUpperCase(c)) {
                // Break camel case into separate words
                buf.append(' ');
                // Use a lower case initial letter for the next word, except if the
                // word is solely X, Y or Z.
                buf.append(c);
            } else if (c == '_') {
                buf.append(' ');
                if (i < len -1 && Character.isLowerCase(name.charAt(i + 1))) {
                    buf.append(Character.toUpperCase(name.charAt(i + 1)));
                    i++;
                }
            } else {
                buf.append(c);
            }
        }

        name = buf.toString();

        name = replaceAcronyms(name);

        return name;
    }

    private static String replaceAcronyms(String name) {
        // Replace these acronyms by upper-case versions
        // - (?<=^| ) means "if preceded by a space or beginning of string"
        // - (?=$| )  means "if followed by a space or end of string"
        if (name.contains("sdk") || name.contains("Sdk")) {
            name = name.replaceAll("(?<=^| )[sS]dk(?=$| )", "SDK");
        }
        if (name.contains("uri") || name.contains("Uri")) {
            name = name.replaceAll("(?<=^| )[uU]ri(?=$| )", "URI");
        }
        if (name.contains("ime") || name.contains("Ime")) {
            name = name.replaceAll("(?<=^| )[iI]me(?=$| )", "IME");
        }
        if (name.contains("vm") || name.contains("Vm")) {
            name = name.replaceAll("(?<=^| )[vV]m(?=$| )", "VM");
        }
        if (name.contains("ui") || name.contains("Ui")) {
            name = name.replaceAll("(?<=^| )[uU]i(?=$| )", "UI");
        }
        return name;
    }

    /**
     * Formats the javadoc tooltip to be usable in a tooltip.
     */
    public static String formatTooltip(String javadoc) {
        ArrayList<String> spans = scanJavadoc(javadoc);

        StringBuilder sb = new StringBuilder();
        boolean needBreak = false;

        for (int n = spans.size(), i = 0; i < n; ++i) {
            String s = spans.get(i);
            if (CODE.equals(s)) {
                s = spans.get(++i);
                if (s != null) {
                    sb.append('"').append(s).append('"');
                }
            } else if (LINK.equals(s)) {
                String base   = spans.get(++i);
                String anchor = spans.get(++i);
                String text   = spans.get(++i);

                if (base != null) {
                    base = base.trim();
                }
                if (anchor != null) {
                    anchor = anchor.trim();
                }
                if (text != null) {
                    text = text.trim();
                }

                // If there's no text, use the anchor if there's one
                if (text == null || text.length() == 0) {
                    text = anchor;
                }

                if (base != null && base.length() > 0) {
                    if (text == null || text.length() == 0) {
                        // If we still have no text, use the base as text
                        text = base;
                    }
                }

                if (text != null) {
                    sb.append(text);
                }

            } else if (ELEM.equals(s)) {
                s = spans.get(++i);
                if (s != null) {
                    sb.append(s);
                }
            } else if (BREAK.equals(s)) {
                needBreak = true;
            } else if (s != null) {
                if (needBreak && s.trim().length() > 0) {
                    sb.append('\n');
                }
                sb.append(s);
                needBreak = false;
            }
        }

        return sb.toString();
    }

    /**
     * Formats the javadoc tooltip to be usable in a FormText.
     * <p/>
     * If the descriptor can provide an icon, the caller should provide
     * elementsDescriptor.getIcon() as "image" to FormText, e.g.:
     * <code>formText.setImage(IMAGE_KEY, elementsDescriptor.getIcon());</code>
     *
     * @param javadoc The javadoc to format. Cannot be null.
     * @param elementDescriptor The element descriptor parent of the javadoc. Cannot be null.
     * @param androidDocBaseUrl The base URL for the documentation. Cannot be null. Should be
     *   <code>FrameworkResourceManager.getInstance().getDocumentationBaseUrl()</code>
     */
    public static String formatFormText(String javadoc,
            ElementDescriptor elementDescriptor,
            String androidDocBaseUrl) {
        ArrayList<String> spans = scanJavadoc(javadoc);

        String fullSdkUrl = androidDocBaseUrl + MANIFEST_SDK_URL;
        String sdkUrl = elementDescriptor.getSdkUrl();
        if (sdkUrl != null && sdkUrl.startsWith(MANIFEST_SDK_URL)) {
            fullSdkUrl = androidDocBaseUrl + sdkUrl;
        }

        StringBuilder sb = new StringBuilder();

        Image icon = elementDescriptor.getCustomizedIcon();
        if (icon != null) {
            sb.append("<form><li style=\"image\" value=\"" +        //$NON-NLS-1$
                    IMAGE_KEY + "\">");                             //$NON-NLS-1$
        } else {
            sb.append("<form><p>");                                 //$NON-NLS-1$
        }

        for (int n = spans.size(), i = 0; i < n; ++i) {
            String s = spans.get(i);
            if (CODE.equals(s)) {
                s = spans.get(++i);
                if (elementDescriptor.getXmlName().equals(s) && fullSdkUrl != null) {
                    sb.append("<a href=\"");                        //$NON-NLS-1$
                    sb.append(fullSdkUrl);
                    sb.append("\">");                               //$NON-NLS-1$
                    sb.append(s);
                    sb.append("</a>");                              //$NON-NLS-1$
                } else if (s != null) {
                    sb.append('"').append(s).append('"');
                }
            } else if (LINK.equals(s)) {
                String base   = spans.get(++i);
                String anchor = spans.get(++i);
                String text   = spans.get(++i);

                if (base != null) {
                    base = base.trim();
                }
                if (anchor != null) {
                    anchor = anchor.trim();
                }
                if (text != null) {
                    text = text.trim();
                }

                // If there's no text, use the anchor if there's one
                if (text == null || text.length() == 0) {
                    text = anchor;
                }

                // TODO specialize with a base URL for views, menus & other resources
                // Base is empty for a local page anchor, in which case we'll replace it
                // by the element SDK URL if it exists.
                if ((base == null || base.length() == 0) && fullSdkUrl != null) {
                    base = fullSdkUrl;
                }

                String url = null;
                if (base != null && base.length() > 0) {
                    if (base.startsWith("http")) {                  //$NON-NLS-1$
                        // If base looks an URL, use it, with the optional anchor
                        url = base;
                        if (anchor != null && anchor.length() > 0) {
                            // If the base URL already has an anchor, it needs to be
                            // removed first. If there's no anchor, we need to add "#"
                            int pos = url.lastIndexOf('#');
                            if (pos < 0) {
                                url += "#";                         //$NON-NLS-1$
                            } else if (pos < url.length() - 1) {
                                url = url.substring(0, pos + 1);
                            }

                            url += anchor;
                        }
                    } else if (text == null || text.length() == 0) {
                        // If we still have no text, use the base as text
                        text = base;
                    }
                }

                if (url != null && text != null) {
                    sb.append("<a href=\"");                        //$NON-NLS-1$
                    sb.append(url);
                    sb.append("\">");                               //$NON-NLS-1$
                    sb.append(text);
                    sb.append("</a>");                              //$NON-NLS-1$
                } else if (text != null) {
                    sb.append("<b>").append(text).append("</b>");   //$NON-NLS-1$ //$NON-NLS-2$
                }

            } else if (ELEM.equals(s)) {
                s = spans.get(++i);
                if (sdkUrl != null && s != null) {
                    sb.append("<a href=\"");                        //$NON-NLS-1$
                    sb.append(sdkUrl);
                    sb.append("\">");                               //$NON-NLS-1$
                    sb.append(s);
                    sb.append("</a>");                              //$NON-NLS-1$
                } else if (s != null) {
                    sb.append("<b>").append(s).append("</b>");      //$NON-NLS-1$ //$NON-NLS-2$
                }
            } else if (BREAK.equals(s)) {
                // ignore line breaks in pseudo-HTML rendering
            } else if (s != null) {
                sb.append(s);
            }
        }

        if (icon != null) {
            sb.append("</li></form>");                              //$NON-NLS-1$
        } else {
            sb.append("</p></form>");                               //$NON-NLS-1$
        }
        return sb.toString();
    }

    private static ArrayList<String> scanJavadoc(String javadoc) {
        ArrayList<String> spans = new ArrayList<String>();

        // Standardize all whitespace in the javadoc to single spaces.
        if (javadoc != null) {
            javadoc = javadoc.replaceAll("[ \t\f\r\n]+", " "); //$NON-NLS-1$ //$NON-NLS-2$
        }

        // Detects {@link <base>#<name> <text>} where all 3 are optional
        Pattern p_link = Pattern.compile("\\{@link\\s+([^#\\}\\s]*)(?:#([^\\s\\}]*))?(?:\\s*([^\\}]*))?\\}(.*)"); //$NON-NLS-1$
        // Detects <code>blah</code>
        Pattern p_code = Pattern.compile("<code>(.+?)</code>(.*)");                 //$NON-NLS-1$
        // Detects @blah@, used in hard-coded tooltip descriptors
        Pattern p_elem = Pattern.compile("@([\\w -]+)@(.*)");                       //$NON-NLS-1$
        // Detects a buffer that starts by @@ (request for a break)
        Pattern p_break = Pattern.compile("@@(.*)");                                //$NON-NLS-1$
        // Detects a buffer that starts by @ < or { (one that was not matched above)
        Pattern p_open = Pattern.compile("([@<\\{])(.*)");                          //$NON-NLS-1$
        // Detects everything till the next potential separator, i.e. @ < or {
        Pattern p_text = Pattern.compile("([^@<\\{]+)(.*)");                        //$NON-NLS-1$

        int currentLength = 0;
        String text = null;

        while(javadoc != null && javadoc.length() > 0) {
            Matcher m;
            String s = null;
            if ((m = p_code.matcher(javadoc)).matches()) {
                spans.add(CODE);
                spans.add(text = cleanupJavadocHtml(m.group(1))); // <code> text
                javadoc = m.group(2);
                if (text != null) {
                    currentLength += text.length();
                }
            } else if ((m = p_link.matcher(javadoc)).matches()) {
                spans.add(LINK);
                spans.add(m.group(1)); // @link base
                spans.add(m.group(2)); // @link anchor
                spans.add(text = cleanupJavadocHtml(m.group(3))); // @link text
                javadoc = m.group(4);
                if (text != null) {
                    currentLength += text.length();
                }
            } else if ((m = p_elem.matcher(javadoc)).matches()) {
                spans.add(ELEM);
                spans.add(text = cleanupJavadocHtml(m.group(1))); // @text@
                javadoc = m.group(2);
                if (text != null) {
                    currentLength += text.length() - 2;
                }
            } else if ((m = p_break.matcher(javadoc)).matches()) {
                spans.add(BREAK);
                currentLength = 0;
                javadoc = m.group(1);
            } else if ((m = p_open.matcher(javadoc)).matches()) {
                s = m.group(1);
                javadoc = m.group(2);
            } else if ((m = p_text.matcher(javadoc)).matches()) {
                s = m.group(1);
                javadoc = m.group(2);
            } else {
                // This is not supposed to happen. In case of, just use everything.
                s = javadoc;
                javadoc = null;
            }
            if (s != null && s.length() > 0) {
                s = cleanupJavadocHtml(s);

                if (currentLength >= JAVADOC_BREAK_LENGTH) {
                    spans.add(BREAK);
                    currentLength = 0;
                }
                while (currentLength + s.length() > JAVADOC_BREAK_LENGTH) {
                    int pos = s.indexOf(' ', JAVADOC_BREAK_LENGTH - currentLength);
                    if (pos <= 0) {
                        break;
                    }
                    spans.add(s.substring(0, pos + 1));
                    spans.add(BREAK);
                    currentLength = 0;
                    s = s.substring(pos + 1);
                }

                spans.add(s);
                currentLength += s.length();
            }
        }

        return spans;
    }

    /**
     * Remove anything that looks like HTML from a javadoc snippet, as it is supported
     * neither by FormText nor a standard text tooltip.
     */
    private static String cleanupJavadocHtml(String s) {
        if (s != null) {
            s = s.replaceAll(LT_ENTITY, "\"");     //$NON-NLS-1$ $NON-NLS-2$
            s = s.replaceAll(GT_ENTITY, "\"");     //$NON-NLS-1$ $NON-NLS-2$
            s = s.replaceAll("<[^>]+>", "");    //$NON-NLS-1$ $NON-NLS-2$
        }
        return s;
    }

    /**
     * Returns the basename for the given fully qualified class name. It is okay to pass
     * a basename to this method which will just be returned back.
     *
     * @param fqcn The fully qualified class name to convert
     * @return the basename of the class name
     */
    public static String getBasename(String fqcn) {
        String name = fqcn;
        int lastDot = name.lastIndexOf('.');
        if (lastDot != -1) {
            name = name.substring(lastDot + 1);
        }

        return name;
    }

    /**
     * Sets the default layout attributes for the a new UiElementNode.
     * <p/>
     * Note that ideally the node should already be part of a hierarchy so that its
     * parent layout and previous sibling can be determined, if any.
     * <p/>
     * This does not override attributes which are not empty.
     */
    public static void setDefaultLayoutAttributes(UiElementNode node, boolean updateLayout) {
        // if this ui_node is a layout and we're adding it to a document, use match_parent for
        // both W/H. Otherwise default to wrap_layout.
        ElementDescriptor descriptor = node.getDescriptor();

        String name = descriptor.getXmlLocalName();
        if (name.equals(REQUEST_FOCUS)) {
            // Don't add ids, widths and heights etc to <requestFocus>
            return;
        }

        // Width and height are mandatory in all layouts except GridLayout
        boolean setSize = !node.getUiParent().getDescriptor().getXmlName().equals(GRID_LAYOUT);
        if (setSize) {
            boolean fill = descriptor.hasChildren() &&
                           node.getUiParent() instanceof UiDocumentNode;
            node.setAttributeValue(
                    ATTR_LAYOUT_WIDTH,
                    ANDROID_URI,
                    fill ? VALUE_FILL_PARENT : VALUE_WRAP_CONTENT,
                    false /* override */);
            node.setAttributeValue(
                    ATTR_LAYOUT_HEIGHT,
                    ANDROID_URI,
                    fill ? VALUE_FILL_PARENT : VALUE_WRAP_CONTENT,
                    false /* override */);
        }

        if (needsDefaultId(node.getDescriptor())) {
            String freeId = getFreeWidgetId(node);
            if (freeId != null) {
                node.setAttributeValue(
                        ATTR_ID,
                        ANDROID_URI,
                        freeId,
                        false /* override */);
            }
        }

        // Set a text attribute on textual widgets -- but only on those that define a text
        // attribute
        if (descriptor.definesAttribute(ANDROID_URI, ATTR_TEXT)
                // Don't set default text value into edit texts - they typically start out blank
                && !descriptor.getXmlLocalName().equals(EDIT_TEXT)) {
            String type = getBasename(descriptor.getUiName());
            node.setAttributeValue(
                ATTR_TEXT,
                ANDROID_URI,
                type,
                false /*override*/);
        }

        if (updateLayout) {
            UiElementNode parent = node.getUiParent();
            if (parent != null &&
                    parent.getDescriptor().getXmlLocalName().equals(
                            RELATIVE_LAYOUT)) {
                UiElementNode previous = node.getUiPreviousSibling();
                if (previous != null) {
                    String id = previous.getAttributeValue(ATTR_ID);
                    if (id != null && id.length() > 0) {
                        id = id.replace("@+", "@");                     //$NON-NLS-1$ //$NON-NLS-2$
                        node.setAttributeValue(
                                ATTR_LAYOUT_BELOW,
                                ANDROID_URI,
                                id,
                                false /* override */);
                    }
                }
            }
        }
    }

    /**
     * Determines whether new views of the given type should be assigned a
     * default id.
     *
     * @param descriptor a descriptor describing the view to look up
     * @return true if new views of the given type should be assigned a default
     *         id
     */
    public static boolean needsDefaultId(ElementDescriptor descriptor) {
        // By default, layouts do not need ids.
        String tag = descriptor.getXmlLocalName();
        if (tag.endsWith("Layout")  //$NON-NLS-1$
                || tag.equals(VIEW_INCLUDE)
                || tag.equals(VIEW_MERGE)
                || tag.equals(SPACE)
                || tag.endsWith(SPACE) && tag.length() > SPACE.length() &&
                    tag.charAt(tag.length() - SPACE.length()) == '.') {
            return false;
        }

        return true;
    }

    /**
     * Given a UI node, returns the first available id that matches the
     * pattern "prefix%d".
     * <p/>TabWidget is a special case and the method will always return "@android:id/tabs".
     *
     * @param uiNode The UI node that gives the prefix to match.
     * @return A suitable generated id in the attribute form needed by the XML id tag
     * (e.g. "@+id/something")
     */
    public static String getFreeWidgetId(UiElementNode uiNode) {
        String name = getBasename(uiNode.getDescriptor().getXmlLocalName());
        return getFreeWidgetId(uiNode.getUiRoot(), name);
    }

    /**
     * Given a UI root node and a potential XML node name, returns the first available
     * id that matches the pattern "prefix%d".
     * <p/>TabWidget is a special case and the method will always return "@android:id/tabs".
     *
     * @param uiRoot The root UI node to search for name conflicts from
     * @param name The XML node prefix name to look for
     * @return A suitable generated id in the attribute form needed by the XML id tag
     * (e.g. "@+id/something")
     */
    public static String getFreeWidgetId(UiElementNode uiRoot, String name) {
        if ("TabWidget".equals(name)) {                        //$NON-NLS-1$
            return "@android:id/tabs";                         //$NON-NLS-1$
        }

        return NEW_ID_PREFIX + getFreeWidgetId(uiRoot,
                new Object[] { name, null, null, null });
    }

    /**
     * Given a UI root node, returns the first available id that matches the
     * pattern "prefix%d".
     *
     * For recursion purposes, a "context" is given. Since Java doesn't have in-out parameters
     * in methods and we're not going to do a dedicated type, we just use an object array which
     * must contain one initial item and several are built on the fly just for internal storage:
     * <ul>
     * <li> prefix(String): The prefix of the generated id, i.e. "widget". Cannot be null.
     * <li> index(Integer): The minimum index of the generated id. Must start with null.
     * <li> generated(String): The generated widget currently being searched. Must start with null.
     * <li> map(Set<String>): A set of the ids collected so far when walking through the widget
     *                        hierarchy. Must start with null.
     * </ul>
     *
     * @param uiRoot The Ui root node where to start searching recursively. For the initial call
     *               you want to pass the document root.
     * @param params An in-out context of parameters used during recursion, as explained above.
     * @return A suitable generated id
     */
    @SuppressWarnings("unchecked")
    private static String getFreeWidgetId(UiElementNode uiRoot,
            Object[] params) {

        Set<String> map = (Set<String>)params[3];
        if (map == null) {
            params[3] = map = new HashSet<String>();
        }

        int num = params[1] == null ? 0 : ((Integer)params[1]).intValue();

        String generated = (String) params[2];
        String prefix = (String) params[0];
        if (generated == null) {
            int pos = prefix.indexOf('.');
            if (pos >= 0) {
                prefix = prefix.substring(pos + 1);
            }
            pos = prefix.indexOf('$');
            if (pos >= 0) {
                prefix = prefix.substring(pos + 1);
            }
            prefix = prefix.replaceAll("[^a-zA-Z]", "");                //$NON-NLS-1$ $NON-NLS-2$
            if (prefix.length() == 0) {
                prefix = DEFAULT_WIDGET_PREFIX;
            } else {
                // Lowercase initial character
                prefix = Character.toLowerCase(prefix.charAt(0)) + prefix.substring(1);
            }

            // Note that we perform locale-independent lowercase checks; in "Image" we
            // want the lowercase version to be "image", not "?mage" where ? is
            // the char LATIN SMALL LETTER DOTLESS I.
            do {
                num++;
                generated = String.format("%1$s%2$d", prefix, num);   //$NON-NLS-1$
            } while (map.contains(generated.toLowerCase(Locale.US)));

            params[0] = prefix;
            params[1] = num;
            params[2] = generated;
        }

        String id = uiRoot.getAttributeValue(ATTR_ID);
        if (id != null) {
            id = id.replace(NEW_ID_PREFIX, "");                            //$NON-NLS-1$
            id = id.replace(ID_PREFIX, "");                                //$NON-NLS-1$
            if (map.add(id.toLowerCase(Locale.US))
                    && map.contains(generated.toLowerCase(Locale.US))) {

                do {
                    num++;
                    generated = String.format("%1$s%2$d", prefix, num);   //$NON-NLS-1$
                } while (map.contains(generated.toLowerCase(Locale.US)));

                params[1] = num;
                params[2] = generated;
            }
        }

        for (UiElementNode uiChild : uiRoot.getUiChildren()) {
            getFreeWidgetId(uiChild, params);
        }

        // Note: return params[2] (not "generated") since it could have changed during recursion.
        return (String) params[2];
    }

    /**
     * Returns true if the given descriptor represents a view that not only can have
     * children but which allows us to <b>insert</b> children. Some views, such as
     * ListView (and in general all AdapterViews), disallow children to be inserted except
     * through the dedicated AdapterView interface to do it.
     *
     * @param descriptor the descriptor for the view in question
     * @param viewObject an actual instance of the view, or null if not available
     * @return true if the descriptor describes a view which allows insertion of child
     *         views
     */
    public static boolean canInsertChildren(ElementDescriptor descriptor, Object viewObject) {
        if (descriptor.hasChildren()) {
            if (viewObject != null) {
                // We have a view object; see if it derives from an AdapterView
                Class<?> clz = viewObject.getClass();
                while (clz != null) {
                    if (clz.getName().equals(FQCN_ADAPTER_VIEW)) {
                        return false;
                    }
                    clz = clz.getSuperclass();
                }
            } else {
                // No view object, so we can't easily look up the class and determine
                // whether it's an AdapterView; instead, look at the fixed list of builtin
                // concrete subclasses of AdapterView
                String viewName = descriptor.getXmlLocalName();
                if (viewName.equals(LIST_VIEW) || viewName.equals(EXPANDABLE_LIST_VIEW)
                        || viewName.equals(GALLERY) || viewName.equals(GRID_VIEW)) {

                    // We should really also enforce that
                    // XmlUtils.ANDROID_URI.equals(descriptor.getNameSpace())
                    // here and if not, return true, but it turns out the getNameSpace()
                    // for elements are often "".

                    return false;
                }
            }

            return true;
        }

        return false;
    }
}

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
package org.eclipse.andmore.internal.editors.formatting;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.xml.XmlFormatPreferences;
import com.android.ide.common.xml.XmlFormatStyle;
import com.android.ide.common.xml.XmlPrettyPrinter;
import com.android.resources.ResourceFolderType;
import com.android.resources.ResourceType;
import com.android.utils.SdkUtils;
import com.android.utils.XmlUtils;

import org.eclipse.andmore.internal.editors.layout.gle2.DomUtilities;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMElement;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Eclipse customization of the {@link EclipseXmlPrettyPrinter} which takes advantage of the
 * Eclipse DOM Api to track additional information, such as whether an element with no children
 * was of the open form ({@code <foo></foo>}) or the closed form ({@code <foo/>}), the ability to
 * look up the original source (for proper entity handling), the ability to preserve attribute
 * source order, etc.
 */
@SuppressWarnings("restriction") // WST XML API
public class EclipseXmlPrettyPrinter extends XmlPrettyPrinter {

    /**
     * Creates a new {@link com.android.ide.common.xml.XmlPrettyPrinter}
     *
     * @param prefs         the preferences to format with
     * @param style         the style to format with
     * @param lineSeparator the line separator to use, such as "\n" (can be null, in which case the
     *                      system default is looked up via the line.separator property)
     */
    public EclipseXmlPrettyPrinter(
            XmlFormatPreferences prefs,
            XmlFormatStyle style,
            String lineSeparator) {
        super(prefs, style, lineSeparator == null ? getDefaultLineSeparator() : lineSeparator);
    }

    /**
     * Pretty-prints the given XML document, which must be well-formed. If it is not,
     * the original unformatted XML document is returned
     *
     * @param xml the XML content to format
     * @param prefs the preferences to format with
     * @param style the style to format with
     * @param lineSeparator the line separator to use, such as "\n" (can be null, in which
     *     case the system default is looked up via the line.separator property)
     * @return the formatted document (or if a parsing error occurred, returns the
     *     unformatted document)
     */
    @NonNull
    public static String prettyPrint(
            @NonNull String xml,
            @NonNull XmlFormatPreferences prefs,
            @NonNull XmlFormatStyle style,
            @Nullable String lineSeparator) {
        Document document = DomUtilities.parseStructuredDocument(xml);
        if (document != null) {
            EclipseXmlPrettyPrinter printer = new EclipseXmlPrettyPrinter(prefs, style,
                    lineSeparator);
            if (xml.endsWith("\n")) { //$NON-NLS-1$
                printer.setEndWithNewline(true);
            }

            StringBuilder sb = new StringBuilder(3 * xml.length() / 2);
            printer.prettyPrint(-1, document, null, null, sb, false /*openTagOnly*/);
            return sb.toString();
        } else {
            // Parser error: just return the unformatted content
            return xml;
        }
    }

    @NonNull
    public static String prettyPrint(@NonNull Node node, boolean endWithNewline) {
        return prettyPrint(node, EclipseXmlFormatPreferences.create(), XmlFormatStyle.get(node),
                null, endWithNewline);
    }

    private static String getDefaultLineSeparator() {
        org.eclipse.jface.text.Document blank = new org.eclipse.jface.text.Document();
        String lineSeparator = TextUtilities.getDefaultLineDelimiter(blank);
        if (lineSeparator == null) {
            lineSeparator = SdkUtils.getLineSeparator();
        }

        return lineSeparator;
    }

    /**
     * Pretty prints the given node
     *
     * @param node the node, usually a document, to be printed
     * @param prefs the formatting preferences
     * @param style the formatting style to use
     * @param lineSeparator the line separator to use, or null to use the
     *            default
     * @return a formatted string
     */
    @NonNull
    public static String prettyPrint(
            @NonNull Node node,
            @NonNull XmlFormatPreferences prefs,
            @NonNull XmlFormatStyle style,
            @Nullable String lineSeparator,
            boolean endWithNewline) {
        XmlPrettyPrinter printer = new EclipseXmlPrettyPrinter(prefs, style, lineSeparator);
        printer.setEndWithNewline(endWithNewline);
        StringBuilder sb = new StringBuilder(1000);
        printer.prettyPrint(-1, node, null, null, sb, false /*openTagOnly*/);
        String xml = sb.toString();
        if (node.getNodeType() == Node.DOCUMENT_NODE && !xml.startsWith("<?")) { //$NON-NLS-1$
            xml = XmlUtils.XML_PROLOG + xml;
        }
        return xml;
    }

    @Nullable
    @Override
    protected String getSource(@NonNull Node node) {
        // In Eclipse, org.w3c.dom.DocumentType.getTextContent() returns null
        if (node instanceof IDOMNode) {
            // Get the original source string. This will contain the actual entities
            // such as "&gt;" instead of ">" which it gets turned into for the DOM nodes.
            // By operating on source we can preserve the user's entities rather than
            // having &gt; for example always turned into >.
            IDOMNode textImpl = (IDOMNode) node;
            return textImpl.getSource();
        }

        return super.getSource(node);
    }

    @Override
    protected boolean isEmptyTag(Element element) {
        if (element instanceof IDOMElement) {
            IDOMElement elementImpl = (IDOMElement) element;
            if (elementImpl.isEmptyTag()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns the {@link XmlFormatStyle} to use for a resource of the given type
     *
     * @param resourceType the type of resource to be formatted
     * @return the suitable format style to use
     */
    public static XmlFormatStyle get(ResourceType resourceType) {
        switch (resourceType) {
            case ARRAY:
            case ATTR:
            case BOOL:
            case DECLARE_STYLEABLE:
            case DIMEN:
            case FRACTION:
            case ID:
            case INTEGER:
            case STRING:
            case PLURALS:
            case STYLE:
            case STYLEABLE:
            case COLOR:
                return XmlFormatStyle.RESOURCE;

            case LAYOUT:
                return XmlFormatStyle.LAYOUT;

            case DRAWABLE:
            case MENU:
            case ANIM:
            case ANIMATOR:
            case INTERPOLATOR:
            default:
                return XmlFormatStyle.FILE;
        }
    }

    /**
     * Returns the {@link XmlFormatStyle} to use for resource files in the given resource
     * folder
     *
     * @param folderType the type of folder containing the resource file
     * @return the suitable format style to use
     */
    public static XmlFormatStyle getForFolderType(ResourceFolderType folderType) {
        switch (folderType) {
            case LAYOUT:
                return XmlFormatStyle.LAYOUT;
            case COLOR:
            case VALUES:
                return XmlFormatStyle.RESOURCE;
            case ANIM:
            case ANIMATOR:
            case DRAWABLE:
            case INTERPOLATOR:
            case MENU:
            default:
                return XmlFormatStyle.FILE;
        }
    }

    /**
     * Returns the {@link XmlFormatStyle} to use for resource files of the given path.
     *
     * @param path the path to the resource file
     * @return the suitable format style to use
     */
    public static XmlFormatStyle getForFile(IPath path) {
        if (SdkConstants.FN_ANDROID_MANIFEST_XML.equals(path.lastSegment())) {
            return XmlFormatStyle.MANIFEST;
        }

        if (path.segmentCount() > 2) {
            String parentName = path.segment(path.segmentCount() - 2);
            ResourceFolderType folderType = ResourceFolderType.getFolderType(parentName);
            return getForFolderType(folderType);
        }

        return XmlFormatStyle.FILE;
    }
}

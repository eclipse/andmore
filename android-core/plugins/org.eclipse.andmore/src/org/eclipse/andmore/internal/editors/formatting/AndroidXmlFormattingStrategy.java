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

import static com.android.SdkConstants.ANDROID_MANIFEST_XML;
import static org.eclipse.andmore.internal.editors.AndroidXmlAutoEditStrategy.findLineStart;
import static org.eclipse.andmore.internal.editors.AndroidXmlAutoEditStrategy.findTextStart;
import static org.eclipse.andmore.internal.editors.color.ColorDescriptors.SELECTOR_TAG;
import static org.eclipse.jface.text.formatter.FormattingContextProperties.CONTEXT_MEDIUM;
import static org.eclipse.jface.text.formatter.FormattingContextProperties.CONTEXT_PARTITION;
import static org.eclipse.jface.text.formatter.FormattingContextProperties.CONTEXT_REGION;
import static org.eclipse.wst.xml.core.internal.regions.DOMRegionContext.XML_EMPTY_TAG_CLOSE;
import static org.eclipse.wst.xml.core.internal.regions.DOMRegionContext.XML_END_TAG_OPEN;
import static org.eclipse.wst.xml.core.internal.regions.DOMRegionContext.XML_TAG_CLOSE;
import static org.eclipse.wst.xml.core.internal.regions.DOMRegionContext.XML_TAG_OPEN;

import com.android.SdkConstants;
import com.android.annotations.VisibleForTesting;
import com.android.ide.common.xml.XmlFormatPreferences;
import com.android.ide.common.xml.XmlFormatStyle;
import com.android.ide.common.xml.XmlPrettyPrinter;
import com.android.resources.ResourceType;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.AdtUtils;
import org.eclipse.andmore.internal.editors.layout.gle2.DomUtilities;
import org.eclipse.andmore.internal.preferences.AdtPrefs;
import org.eclipse.andmore.internal.project.BaseProjectHelper;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.TypedPosition;
import org.eclipse.jface.text.formatter.ContextBasedFormattingStrategy;
import org.eclipse.jface.text.formatter.IFormattingContext;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegionList;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;
import org.eclipse.wst.xml.ui.internal.XMLFormattingStrategy;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * Formatter which formats XML content according to the established Android coding
 * conventions. It performs the format by computing the smallest set of DOM nodes
 * overlapping the formatted region, then it pretty-prints that XML region
 * using the {@link EclipseXmlPrettyPrinter}, and then it replaces the affected region
 * by the pretty-printed region.
 * <p>
 * This strategy is also used for delegation. If the user has chosen to use the
 * standard Eclipse XML formatter, this strategy simply delegates to the
 * default XML formatting strategy in WTP.
 */
@SuppressWarnings("restriction")
public class AndroidXmlFormattingStrategy extends ContextBasedFormattingStrategy {
    private IRegion mRegion;
    private final Queue<IDocument> mDocuments = new LinkedList<IDocument>();
    private final LinkedList<TypedPosition> mPartitions = new LinkedList<TypedPosition>();
    private ContextBasedFormattingStrategy mDelegate = null;
    /** False if document is known not to be in an Android project, null until initialized */
    private Boolean mIsAndroid;

    /**
     * Creates a new {@link AndroidXmlFormattingStrategy}
     */
    public AndroidXmlFormattingStrategy() {
    }

    private ContextBasedFormattingStrategy getDelegate() {
        if (!AdtPrefs.getPrefs().getUseCustomXmlFormatter()
                || mIsAndroid != null && !mIsAndroid.booleanValue()) {
            if (mDelegate == null) {
                mDelegate = new XMLFormattingStrategy();
            }

            return mDelegate;
        }

        return null;
    }

    @Override
    public void format() {
        // Use Eclipse XML formatter instead?
        ContextBasedFormattingStrategy delegate = getDelegate();
        if (delegate != null) {
            delegate.format();
            return;
        }

        super.format();

        IDocument document = mDocuments.poll();
        TypedPosition partition = mPartitions.poll();

        if (document != null && partition != null && mRegion != null) {
            try {
                if (document instanceof IStructuredDocument) {
                    IStructuredDocument structuredDocument = (IStructuredDocument) document;
                    IModelManager modelManager = StructuredModelManager.getModelManager();
                    IStructuredModel model = modelManager.getModelForEdit(structuredDocument);
                    if (model != null) {
                        try {
                            TextEdit edit = format(model, mRegion.getOffset(),
                                    mRegion.getLength());
                            if (edit != null) {
                                try {
                                    model.aboutToChangeModel();
                                    edit.apply(document);
                                }
                                finally {
                                    model.changedModel();
                                }
                            }
                        }
                        finally {
                            model.releaseFromEdit();
                        }
                    }
                }
            }
            catch (BadLocationException e) {
                AdtPlugin.log(e, "Formatting error");
            }
        }
    }

    /**
     * Creates a {@link TextEdit} for formatting the given model's XML in the text range
     * starting at offset start with the given length. Note that the exact formatting
     * offsets may be adjusted to format a complete element.
     *
     * @param model the model to be formatted
     * @param start the starting offset
     * @param length the length of the text range to be formatted
     * @return a {@link TextEdit} which edits the model into a formatted document
     */
    private static TextEdit format(IStructuredModel model, int start, int length) {
        int end = start + length;

        TextEdit edit = new MultiTextEdit();
        IStructuredDocument document = model.getStructuredDocument();

        Node startNode = null;
        Node endNode = null;
        Document domDocument = null;

        if (model instanceof IDOMModel) {
            IDOMModel domModel = (IDOMModel) model;
            domDocument = domModel.getDocument();
        } else {
            // This should not happen
            return edit;
        }

        IStructuredDocumentRegion startRegion = document.getRegionAtCharacterOffset(start);
        if (startRegion != null) {
            int startOffset = startRegion.getStartOffset();
            IndexedRegion currentIndexedRegion = model.getIndexedRegion(startOffset);
            if (currentIndexedRegion instanceof IDOMNode) {
                IDOMNode currentDOMNode = (IDOMNode) currentIndexedRegion;
                startNode = currentDOMNode;
            }
        }

        boolean isOpenTagOnly = false;
        int openTagEnd = -1;

        IStructuredDocumentRegion endRegion = document.getRegionAtCharacterOffset(end);
        if (endRegion != null) {
            int endOffset = Math.max(endRegion.getStartOffset(),
                    endRegion.getEndOffset() - 1);
            IndexedRegion currentIndexedRegion = model.getIndexedRegion(endOffset);

            // If you place the caret right on the right edge of an element, such as this:
            //     <foo name="value">|
            // then the DOM model will consider the region containing the caret to be
            // whatever nodes FOLLOWS the element, usually a text node.
            // Detect this case, and look into the previous range.
            if (currentIndexedRegion instanceof Text
                    && currentIndexedRegion.getStartOffset() == end && end > 0) {
                end--;
                currentIndexedRegion = model.getIndexedRegion(end);
                endRegion = document.getRegionAtCharacterOffset(
                        currentIndexedRegion.getStartOffset());
            }

            if (currentIndexedRegion instanceof IDOMNode) {
                IDOMNode currentDOMNode = (IDOMNode) currentIndexedRegion;
                endNode = currentDOMNode;

                // See if this range is fully within the opening tag
                if (endNode == startNode && endRegion == startRegion) {
                    ITextRegion subRegion = endRegion.getRegionAtCharacterOffset(end);
                    ITextRegionList regions = endRegion.getRegions();
                    int index = regions.indexOf(subRegion);
                    if (index != -1) {
                        // Skip past initial occurrence of close tag if we place the caret
                        // right on a >
                        subRegion = regions.get(index);
                        String type = subRegion.getType();
                        if (type == XML_TAG_CLOSE || type == XML_EMPTY_TAG_CLOSE) {
                            index--;
                        }
                    }
                    for (; index >= 0; index--) {
                        subRegion = regions.get(index);
                        String type = subRegion.getType();
                        if (type == XML_TAG_OPEN) {
                            isOpenTagOnly = true;
                        } else if (type == XML_EMPTY_TAG_CLOSE || type == XML_TAG_CLOSE
                                || type == XML_END_TAG_OPEN) {
                            break;
                        }
                    }

                    int max = regions.size();
                    for (index = Math.max(0, index); index < max; index++) {
                        subRegion = regions.get(index);
                        String type = subRegion.getType();
                        if (type == XML_EMPTY_TAG_CLOSE || type == XML_TAG_CLOSE) {
                            openTagEnd = subRegion.getEnd() + endRegion.getStartOffset();
                        }
                    }

                    if (openTagEnd == -1) {
                        isOpenTagOnly = false;
                    }
                }
            }
        }

        String[] indentationLevels = null;
        Node root = null;
        int initialDepth = 0;
        int replaceStart;
        int replaceEnd;
        boolean endWithNewline = false;
        if (startNode == null || endNode == null) {
            // Process the entire document
            root = domDocument;
            // both document and documentElement should be <= 0
            initialDepth = -1;
            startNode = root;
            endNode = root;
            replaceStart = 0;
            replaceEnd = document.getLength();
            try {
                endWithNewline = replaceEnd > 0 && document.getChar(replaceEnd - 1) == '\n';
            } catch (BadLocationException e) {
                // Can't happen
            }
        } else {
            root = DomUtilities.getCommonAncestor(startNode, endNode);
            initialDepth = root != null ? DomUtilities.getDepth(root) - 1 : 0;

            // Regions must be non-null since the DOM nodes are non null, but Eclipse null
            // analysis doesn't realize it:
            assert startRegion != null && endRegion != null;

            replaceStart = ((IndexedRegion) startNode).getStartOffset();
            if (isOpenTagOnly) {
                replaceEnd = openTagEnd;
            } else {
                replaceEnd = ((IndexedRegion) endNode).getEndOffset();
            }

            // Look up the indentation level of the start node, if it is an element
            // and it starts on its own line
            if (startNode.getNodeType() == Node.ELEMENT_NODE) {
                // Measure the indentation of the start node such that we can indent
                // the reformatted version of the node exactly in place and it should blend
                // in if the surrounding content does not use the same indentation size etc.
                // However, it's possible for the start node to have deeper depth than other
                // content we're formatting, as in the following scenario for example:
                //      <foo>
                //         <bar/>
                //      </foo>
                //   <baz/>
                // If you select this text range, we want <foo> to be formatted at whatever
                // level it is, and we also need to know the indentation level to use
                // for </baz>. We don't measure the depth of <bar/>, a child of the start node,
                // since from the initial indentation level and on down we want to normalize
                // the output.
                IndentationMeasurer m = new IndentationMeasurer(startNode, endNode, document);
                indentationLevels = m.measure(initialDepth, root);

                // Wipe out any levels deeper than the start node's level
                // (which may not be the smallest level, e.g. where you select a child
                // and the end of its parent etc).
                // (Since we're ONLY measuring the node and its parents, you might wonder
                // why this is doing a full subtree traversal instead of just walking up
                // the parent chain and looking up the indentation for each. The reason for
                // this is that some of theses nodes, which have not yet been formatted,
                // may be sharing lines with other nodes, and we disregard indentation for
                // any nodes that don't start a line since the indentation may only be correct
                // for the first element, so therefore we look for other nodes at the same
                // level that do have indentation info at the front of the line.
                int depth = DomUtilities.getDepth(startNode) - 1;
                for (int i = depth + 1; i < indentationLevels.length; i++) {
                    indentationLevels[i] = null;
                }
            }
        }

        XmlFormatStyle style = guessStyle(model, domDocument);
        XmlFormatPreferences prefs = EclipseXmlFormatPreferences.create();
        String delimiter = TextUtilities.getDefaultLineDelimiter(document);
        XmlPrettyPrinter printer = new EclipseXmlPrettyPrinter(prefs, style, delimiter);
        printer.setEndWithNewline(endWithNewline);

        if (indentationLevels != null) {
            printer.setIndentationLevels(indentationLevels);
        }

        StringBuilder sb = new StringBuilder(length);
        printer.prettyPrint(initialDepth, root, startNode, endNode, sb, isOpenTagOnly);

        String formatted = sb.toString();
        ReplaceEdit replaceEdit = createReplaceEdit(document, replaceStart, replaceEnd, formatted,
                prefs);
        if (replaceEdit != null) {
            edit.addChild(replaceEdit);
        }

        // Attempt to fix the selection range since otherwise, with the document shifting
        // under it, you end up selecting a "random" portion of text now shifted into the
        // old positions of the formatted text:
        if (replaceEdit != null && replaceStart != 0 && replaceEnd != document.getLength()) {
            ITextEditor editor = AdtUtils.getActiveTextEditor();
            if (editor != null) {
                editor.setHighlightRange(replaceEdit.getOffset(), replaceEdit.getText().length(),
                        false /*moveCursor*/);
            }
        }

        return edit;
    }

    /**
     * Create a {@link ReplaceEdit} which replaces the text in the given document with the
     * given new formatted content. The replaceStart and replaceEnd parameters point to
     * the equivalent unformatted text in the document, but the actual edit range may be
     * adjusted (for example to make the edit smaller if the beginning and/or end is
     * identical, and so on)
     */
    @VisibleForTesting
    static ReplaceEdit createReplaceEdit(IDocument document, int replaceStart,
            int replaceEnd, String formatted, XmlFormatPreferences prefs) {
        // If replacing a node somewhere in the middle, start the replacement at the
        // beginning of the current line
        int index = replaceStart;
        try {
            while (index > 0) {
                char c = document.getChar(index - 1);
                if (c == '\n') {
                    if (index < replaceStart) {
                        replaceStart = index;
                    }
                    break;
                } else if (!Character.isWhitespace(c)) {
                    // The replaced node does not start on its own line; in that case,
                    // remove the initial indentation in the reformatted element
                    for (int i = 0; i < formatted.length(); i++) {
                        if (!Character.isWhitespace(formatted.charAt(i))) {
                            formatted = formatted.substring(i);
                            break;
                        }
                    }
                    break;
                }
                index--;
            }
        } catch (BadLocationException e) {
            AdtPlugin.log(e, null);
        }

        // If there are multiple blank lines before the insert position, collapse them down
        // to one
        int prevNewlineIndex = -1;
        boolean beginsWithNewline = false;
        for (int i = 0, n = formatted.length(); i < n; i++) {
            char c = formatted.charAt(i);
            if (c == '\n') {
                beginsWithNewline = true;
                break;
            } else if (!Character.isWhitespace(c)) { // \r is whitespace so is handled correctly
                break;
            }
        }
        try {
            for (index = replaceStart - 1; index > 0; index--) {
                char c = document.getChar(index);
                if (c == '\n') {
                    if (prevNewlineIndex != -1) {
                        replaceStart = prevNewlineIndex;
                    }
                    prevNewlineIndex = index;
                    if (index > 0 && document.getChar(index - 1) == '\r') {
                        prevNewlineIndex--;
                    }
                } else if (!Character.isWhitespace(c)) {
                    break;
                }
            }
        } catch (BadLocationException e) {
            AdtPlugin.log(e, null);
        }
        if (prefs.removeEmptyLines && prevNewlineIndex != -1 && beginsWithNewline) {
            replaceStart = prevNewlineIndex + 1;
        }

        // Search forwards too
        int nextNewlineIndex = -1;
        try {
            int max = document.getLength();
            for (index = replaceEnd; index < max; index++) {
                char c = document.getChar(index);
                if (c == '\n') {
                    if (nextNewlineIndex != -1) {
                        replaceEnd = nextNewlineIndex + 1;
                    }
                    nextNewlineIndex = index;
                } else if (!Character.isWhitespace(c)) {
                    break;
                }
            }
        } catch (BadLocationException e) {
            AdtPlugin.log(e, null);
        }
        boolean endsWithNewline = false;
        for (int i = formatted.length() - 1; i >= 0; i--) {
            char c = formatted.charAt(i);
            if (c == '\n') {
                endsWithNewline = true;
                break;
            } else if (!Character.isWhitespace(c)) {
                break;
            }
        }

        if (prefs.removeEmptyLines && nextNewlineIndex != -1 && endsWithNewline) {
            replaceEnd = nextNewlineIndex + 1;
        }

        // Figure out how much of the before and after strings are identical and narrow
        // the replacement scope
        boolean foundDifference = false;
        int firstDifference = 0;
        int lastDifference = formatted.length();
        try {
            for (int i = 0, j = replaceStart; i < formatted.length() && j < replaceEnd; i++, j++) {
                if (formatted.charAt(i) != document.getChar(j)) {
                    firstDifference = i;
                    foundDifference = true;
                    break;
                }
            }

            if (!foundDifference) {
                // No differences - the document is already formatted, nothing to do
                return null;
            }

            lastDifference = firstDifference + 1;
            for (int i = formatted.length() - 1, j = replaceEnd - 1;
                    i > firstDifference && j > replaceStart;
                    i--, j--) {
                if (formatted.charAt(i) != document.getChar(j)) {
                    lastDifference = i + 1;
                    break;
                }
            }
        } catch (BadLocationException e) {
            AdtPlugin.log(e, null);
        }

        replaceStart += firstDifference;
        replaceEnd -= (formatted.length() - lastDifference);
        replaceEnd = Math.max(replaceStart, replaceEnd);
        formatted = formatted.substring(firstDifference, lastDifference);

        ReplaceEdit replaceEdit = new ReplaceEdit(replaceStart, replaceEnd - replaceStart,
                formatted);
        return replaceEdit;
    }

    /**
     * Guess what style to use to edit the given document - layout, resource, manifest, ... ? */
    public static XmlFormatStyle guessStyle(IStructuredModel model, Document domDocument) {
        // The "layout" style is used for most XML resource file types:
        // layouts, color-lists and state-lists, animations, drawables, menus, etc
        XmlFormatStyle style = XmlFormatStyle.get(domDocument);
        if (style == XmlFormatStyle.FILE) {
            style = XmlFormatStyle.LAYOUT;
        }

        // The "resource" style is used for most value-based XML files:
        // strings, dimensions, booleans, colors, integers, plurals,
        // integer-arrays, string-arrays, and typed-arrays
        Element rootElement = domDocument.getDocumentElement();
        if (rootElement != null
                && SdkConstants.TAG_RESOURCES.equals(rootElement.getTagName())) {
            style = XmlFormatStyle.RESOURCE;
        }

        // Selectors are also used similar to resources
        if (rootElement != null && SELECTOR_TAG.equals(rootElement.getTagName())) {
            return XmlFormatStyle.RESOURCE;
        }

        // The "manifest" style is used for manifest files
        String baseLocation = model.getBaseLocation();
        if (baseLocation != null) {
            if (baseLocation.endsWith(SdkConstants.FN_ANDROID_MANIFEST_XML)) {
                style = XmlFormatStyle.MANIFEST;
            } else {
                int lastSlash = baseLocation.lastIndexOf('/');
                if (lastSlash != -1) {
                    int end = baseLocation.lastIndexOf('/', lastSlash - 1); // -1 is okay
                    String resourceFolder = baseLocation.substring(end + 1, lastSlash);
                    String[] segments = resourceFolder.split("-"); //$NON-NLS-1$
                    ResourceType type = ResourceType.getEnum(segments[0]);
                    if (type != null) {
                        // <resources> files found in res/xml/ should be formatted as
                        // resource files!
                        if (type == ResourceType.XML && style == XmlFormatStyle.RESOURCE) {
                            return style;
                        }
                        style = EclipseXmlPrettyPrinter.get(type);
                    }
                }
            }
        }

        return style;
    }

    private Boolean isAndroid(IDocument document) {
        if (mIsAndroid == null) {
            // Look up the corresponding IResource for this document. This isn't
            // readily available, so take advantage of the structured model's base location
            // string and convert it to an IResource to look up its project.
            if (document instanceof IStructuredDocument) {
                IStructuredDocument structuredDocument = (IStructuredDocument) document;
                IModelManager modelManager = StructuredModelManager.getModelManager();

                IStructuredModel model = modelManager.getModelForRead(structuredDocument);
                if (model != null) {
                    String location = model.getBaseLocation();
                    model.releaseFromRead();
                    if (location != null) {
                        if (!location.endsWith(ANDROID_MANIFEST_XML)
                                && !location.contains("/res/")) { //$NON-NLS-1$
                            // See if it looks like a foreign document
                            IWorkspace workspace = ResourcesPlugin.getWorkspace();
                            IWorkspaceRoot root = workspace.getRoot();
                            IResource member = root.findMember(location);
                            if (member.exists()) {
                                IProject project = member.getProject();
                                if (project.isAccessible() &&
                                        !BaseProjectHelper.isAndroidProject(project)) {
                                    mIsAndroid = false;
                                    return false;
                                }
                            }
                        }
                        // Ignore Maven POM files even in Android projects
                        if (location.endsWith("/pom.xml")) { //$NON-NLS-1$
                            mIsAndroid = false;
                            return false;
                        }
                    }
                }
            }

            mIsAndroid = true;
        }

        return mIsAndroid.booleanValue();
    }

    @Override
    public void formatterStarts(final IFormattingContext context) {
        // Use Eclipse XML formatter instead?
        ContextBasedFormattingStrategy delegate = getDelegate();
        if (delegate != null) {
            delegate.formatterStarts(context);

            // We also need the super implementation because it stores items into the
            // map, and we can't override the getPreferences method, so we need for
            // this delegating strategy to supply the correct values when it is called
            // instead of the delegate
            super.formatterStarts(context);

            return;
        }

        super.formatterStarts(context);
        mRegion = (IRegion) context.getProperty(CONTEXT_REGION);
        TypedPosition partition = (TypedPosition) context.getProperty(CONTEXT_PARTITION);
        IDocument document = (IDocument) context.getProperty(CONTEXT_MEDIUM);
        mPartitions.offer(partition);
        mDocuments.offer(document);

        if (!isAndroid(document)) {
            // It's some foreign type of project: use default
            // formatter
            delegate = getDelegate();
            if (delegate != null) {
                delegate.formatterStarts(context);
            }
        }
    }

    @Override
    public void formatterStops() {
        // Use Eclipse XML formatter instead?
        ContextBasedFormattingStrategy delegate = getDelegate();
        if (delegate != null) {
            delegate.formatterStops();
            // See formatterStarts for an explanation
            super.formatterStops();

            return;
        }

        super.formatterStops();
        mRegion = null;
        mDocuments.clear();
        mPartitions.clear();
    }

    /**
     * Utility class which can measure the indentation strings for various node levels in
     * a given node range
     */
    static class IndentationMeasurer {
        private final Map<Integer, String> mDepth = new HashMap<Integer, String>();
        private final Node mStartNode;
        private final Node mEndNode;
        private final IStructuredDocument mDocument;
        private boolean mDone = false;
        private boolean mInRange = false;
        private int mMaxDepth;

        public IndentationMeasurer(Node mStartNode, Node mEndNode, IStructuredDocument document) {
            super();
            this.mStartNode = mStartNode;
            this.mEndNode = mEndNode;
            mDocument = document;
        }

        /**
         * Measure the various depths found in the range (defined in the constructor)
         * under the given node which should be a common ancestor of the start and end
         * nodes. The result is a string array where each index corresponds to a depth,
         * and the string is either empty, or the complete indentation string to be used
         * to indent to the given depth (note that these strings are not cumulative)
         *
         * @param initialDepth the initial depth to use when visiting
         * @param root the root node to look for depths under
         * @return a string array containing nulls or indentation strings
         */
        public String[] measure(int initialDepth, Node root) {
            visit(initialDepth, root);
            String[] indentationLevels = new String[mMaxDepth + 1];
            for (Map.Entry<Integer, String> entry : mDepth.entrySet()) {
                int depth = entry.getKey();
                String indentation = entry.getValue();
                indentationLevels[depth] = indentation;
            }

            return indentationLevels;
        }

        private void visit(int depth, Node node) {
            // Look up indentation for this level
            if (node.getNodeType() == Node.ELEMENT_NODE && mDepth.get(depth) == null) {
                // Look up the depth
                try {
                    IndexedRegion region = (IndexedRegion) node;
                    int lineStart = findLineStart(mDocument, region.getStartOffset());
                    int textStart = findTextStart(mDocument, lineStart, region.getEndOffset());

                    // Ensure that the text which begins the line is this element, otherwise
                    // we could be measuring the indentation of a parent element which begins
                    // the line
                    if (textStart == region.getStartOffset()) {
                        String indent = mDocument.get(lineStart,
                                Math.max(0, textStart - lineStart));
                        mDepth.put(depth, indent);

                        if (depth > mMaxDepth) {
                            mMaxDepth = depth;
                        }
                    }
                } catch (BadLocationException e) {
                    AdtPlugin.log(e, null);
                }
            }

            NodeList children = node.getChildNodes();
            for (int i = 0, n = children.getLength(); i < n; i++) {
                Node child = children.item(i);
                visit(depth + 1, child);
                if (mDone) {
                    return;
                }
            }

            if (node == mEndNode) {
                mDone = true;
            }
        }
    }
}
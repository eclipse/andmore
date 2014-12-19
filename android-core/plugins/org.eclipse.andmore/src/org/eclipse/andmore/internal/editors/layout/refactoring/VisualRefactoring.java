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
package org.eclipse.andmore.internal.editors.layout.refactoring;

import static com.android.SdkConstants.ANDROID_NS_NAME;
import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ANDROID_WIDGET_PREFIX;
import static com.android.SdkConstants.ATTR_ID;
import static com.android.SdkConstants.ATTR_LAYOUT_HEIGHT;
import static com.android.SdkConstants.ATTR_LAYOUT_RESOURCE_PREFIX;
import static com.android.SdkConstants.ATTR_LAYOUT_WIDTH;
import static com.android.SdkConstants.ID_PREFIX;
import static com.android.SdkConstants.NEW_ID_PREFIX;
import static com.android.SdkConstants.XMLNS;
import static com.android.SdkConstants.XMLNS_PREFIX;

import com.android.annotations.NonNull;
import com.android.annotations.VisibleForTesting;
import com.android.ide.common.xml.XmlFormatStyle;
import com.android.utils.Pair;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.editors.AndroidXmlEditor;
import org.eclipse.andmore.internal.editors.formatting.EclipseXmlFormatPreferences;
import org.eclipse.andmore.internal.editors.formatting.EclipseXmlPrettyPrinter;
import org.eclipse.andmore.internal.editors.layout.LayoutEditorDelegate;
import org.eclipse.andmore.internal.editors.layout.configuration.ConfigurationDescription;
import org.eclipse.andmore.internal.editors.layout.descriptors.ViewElementDescriptor;
import org.eclipse.andmore.internal.editors.layout.gle2.CanvasViewInfo;
import org.eclipse.andmore.internal.editors.layout.gle2.DomUtilities;
import org.eclipse.andmore.internal.editors.layout.gle2.GraphicalEditorPart;
import org.eclipse.andmore.internal.editors.layout.uimodel.UiViewElementNode;
import org.eclipse.andmore.internal.editors.uimodel.UiElementNode;
import org.eclipse.andmore.internal.preferences.AdtPrefs;
import org.eclipse.andmore.internal.sdk.AndroidTargetData;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.ChangeDescriptor;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringChangeDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegionList;
import org.eclipse.wst.xml.core.internal.regions.DOMRegionContext;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Parent class for the various visual refactoring operations; contains shared
 * implementations needed by most of them
 */
@SuppressWarnings("restriction") // XML model
public abstract class VisualRefactoring extends Refactoring {
    private static final String KEY_FILE = "file";                      //$NON-NLS-1$
    private static final String KEY_PROJECT = "proj";                   //$NON-NLS-1$
    private static final String KEY_SEL_START = "sel-start";            //$NON-NLS-1$
    private static final String KEY_SEL_END = "sel-end";                //$NON-NLS-1$

    protected final IFile mFile;
    protected final LayoutEditorDelegate mDelegate;
    protected final IProject mProject;
    protected int mSelectionStart = -1;
    protected int mSelectionEnd = -1;
    protected final List<Element> mElements;
    protected final ITreeSelection mTreeSelection;
    protected final ITextSelection mSelection;
    /** Same as {@link #mSelectionStart} but not adjusted to element edges */
    protected int mOriginalSelectionStart = -1;
    /** Same as {@link #mSelectionEnd} but not adjusted to element edges */
    protected int mOriginalSelectionEnd = -1;

    protected final Map<Element, String> mGeneratedIdMap = new HashMap<Element, String>();
    protected final Set<String> mGeneratedIds = new HashSet<String>();

    protected List<Change> mChanges;
    private String mAndroidNamespacePrefix;

    /**
     * This constructor is solely used by {@link VisualRefactoringDescriptor},
     * to replay a previous refactoring.
     * @param arguments argument map created by #createArgumentMap.
     */
    VisualRefactoring(Map<String, String> arguments) {
        IPath path = Path.fromPortableString(arguments.get(KEY_PROJECT));
        mProject = (IProject) ResourcesPlugin.getWorkspace().getRoot().findMember(path);
        path = Path.fromPortableString(arguments.get(KEY_FILE));
        mFile = (IFile) ResourcesPlugin.getWorkspace().getRoot().findMember(path);
        mSelectionStart = Integer.parseInt(arguments.get(KEY_SEL_START));
        mSelectionEnd = Integer.parseInt(arguments.get(KEY_SEL_END));
        mOriginalSelectionStart = mSelectionStart;
        mOriginalSelectionEnd = mSelectionEnd;
        mDelegate = null;
        mElements = null;
        mSelection = null;
        mTreeSelection = null;
    }

    @VisibleForTesting
    VisualRefactoring(List<Element> elements, LayoutEditorDelegate delegate) {
        mElements = elements;
        mDelegate = delegate;

        mFile = delegate != null ? delegate.getEditor().getInputFile() : null;
        mProject = delegate != null ? delegate.getEditor().getProject() : null;
        mSelectionStart = 0;
        mSelectionEnd = 0;
        mOriginalSelectionStart = 0;
        mOriginalSelectionEnd = 0;
        mSelection = null;
        mTreeSelection = null;

        int end = Integer.MIN_VALUE;
        int start = Integer.MAX_VALUE;
        for (Element element : elements) {
            if (element instanceof IndexedRegion) {
                IndexedRegion region = (IndexedRegion) element;
                start = Math.min(start, region.getStartOffset());
                end = Math.max(end, region.getEndOffset());
            }
        }
        if (start >= 0) {
            mSelectionStart = start;
            mSelectionEnd = end;
            mOriginalSelectionStart = start;
            mOriginalSelectionEnd = end;
        }
    }

    public VisualRefactoring(IFile file, LayoutEditorDelegate editor, ITextSelection selection,
            ITreeSelection treeSelection) {
        mFile = file;
        mDelegate = editor;
        mProject = file.getProject();
        mSelection = selection;
        mTreeSelection = treeSelection;

        // Initialize mSelectionStart and mSelectionEnd based on the selection context, which
        // is either a treeSelection (when invoked from the layout editor or the outline), or
        // a selection (when invoked from an XML editor)
        if (treeSelection != null) {
            int end = Integer.MIN_VALUE;
            int start = Integer.MAX_VALUE;
            for (TreePath path : treeSelection.getPaths()) {
                Object lastSegment = path.getLastSegment();
                if (lastSegment instanceof CanvasViewInfo) {
                    CanvasViewInfo viewInfo = (CanvasViewInfo) lastSegment;
                    UiViewElementNode uiNode = viewInfo.getUiViewNode();
                    if (uiNode == null) {
                        continue;
                    }
                    Node xmlNode = uiNode.getXmlNode();
                    if (xmlNode instanceof IndexedRegion) {
                        IndexedRegion region = (IndexedRegion) xmlNode;

                        start = Math.min(start, region.getStartOffset());
                        end = Math.max(end, region.getEndOffset());
                    }
                }
            }
            if (start >= 0) {
                mSelectionStart = start;
                mSelectionEnd = end;
                mOriginalSelectionStart = mSelectionStart;
                mOriginalSelectionEnd = mSelectionEnd;
            }
            if (selection != null) {
                mOriginalSelectionStart = selection.getOffset();
                mOriginalSelectionEnd = mOriginalSelectionStart + selection.getLength();
            }
        } else if (selection != null) {
            // TODO: update selection to boundaries!
            mSelectionStart = selection.getOffset();
            mSelectionEnd = mSelectionStart + selection.getLength();
            mOriginalSelectionStart = mSelectionStart;
            mOriginalSelectionEnd = mSelectionEnd;
        }

        mElements = initElements();
    }

    @NonNull
    protected abstract List<Change> computeChanges(IProgressMonitor monitor);

    @Override
    public RefactoringStatus checkFinalConditions(IProgressMonitor monitor) throws CoreException,
            OperationCanceledException {
        RefactoringStatus status = new RefactoringStatus();
        mChanges = new ArrayList<Change>();
        try {
            monitor.beginTask("Checking post-conditions...", 5);

            // Reset state for each computeChanges call, in case the user goes back
            // and forth in the refactoring wizard
            mGeneratedIdMap.clear();
            mGeneratedIds.clear();
            List<Change> changes = computeChanges(monitor);
            mChanges.addAll(changes);

            monitor.worked(1);
        } finally {
            monitor.done();
        }

        return status;
    }

    @Override
    public Change createChange(IProgressMonitor monitor) throws CoreException,
            OperationCanceledException {
        try {
            monitor.beginTask("Applying changes...", 1);

            CompositeChange change = new CompositeChange(
                    getName(),
                    mChanges.toArray(new Change[mChanges.size()])) {
                @Override
                public ChangeDescriptor getDescriptor() {
                    VisualRefactoringDescriptor desc = createDescriptor();
                    return new RefactoringChangeDescriptor(desc);
                }
            };

            monitor.worked(1);
            return change;

        } finally {
            monitor.done();
        }
    }

    protected abstract VisualRefactoringDescriptor createDescriptor();

    protected Map<String, String> createArgumentMap() {
        HashMap<String, String> args = new HashMap<String, String>();
        args.put(KEY_PROJECT, mProject.getFullPath().toPortableString());
        args.put(KEY_FILE, mFile.getFullPath().toPortableString());
        args.put(KEY_SEL_START, Integer.toString(mSelectionStart));
        args.put(KEY_SEL_END, Integer.toString(mSelectionEnd));

        return args;
    }

    IFile getFile() {
        return mFile;
    }

    // ---- Shared functionality ----


    protected void openFile(IFile file) {
        GraphicalEditorPart graphicalEditor = mDelegate.getGraphicalEditor();
        IFile leavingFile = graphicalEditor.getEditedFile();

        try {
            // Duplicate the current state into the newly created file
            String state = ConfigurationDescription.getDescription(leavingFile);

            // TODO: Look for a ".NoTitleBar.Fullscreen" theme version of the current
            // theme to show.

            file.setSessionProperty(GraphicalEditorPart.NAME_INITIAL_STATE, state);
        } catch (CoreException e) {
            // pass
        }

        /* TBD: "Show Included In" if supported.
         * Not sure if this is a good idea.
        if (graphicalEditor.renderingSupports(Capability.EMBEDDED_LAYOUT)) {
            try {
                Reference include = Reference.create(graphicalEditor.getEditedFile());
                file.setSessionProperty(GraphicalEditorPart.NAME_INCLUDE, include);
            } catch (CoreException e) {
                // pass - worst that can happen is that we don't start with inclusion
            }
        }
        */

        try {
            IEditorPart part =
                IDE.openEditor(mDelegate.getEditor().getEditorSite().getPage(), file);
            if (part instanceof AndroidXmlEditor && AdtPrefs.getPrefs().getFormatGuiXml()) {
                AndroidXmlEditor newEditor = (AndroidXmlEditor) part;
                newEditor.reformatDocument();
            }
        } catch (PartInitException e) {
            AdtPlugin.log(e, "Can't open new included layout");
        }
    }


    /** Produce a list of edits to replace references to the given id with the given new id */
    protected static List<TextEdit> replaceIds(String androidNamePrefix,
            IStructuredDocument doc, int skipStart, int skipEnd,
            String rootId, String referenceId) {
        if (rootId == null) {
            return Collections.emptyList();
        }

        // We need to search for either @+id/ or @id/
        String match1 = rootId;
        String match2;
        if (match1.startsWith(ID_PREFIX)) {
            match2 = '"' + NEW_ID_PREFIX + match1.substring(ID_PREFIX.length()) + '"';
            match1 = '"' + match1 + '"';
        } else if (match1.startsWith(NEW_ID_PREFIX)) {
            match2 = '"' + ID_PREFIX + match1.substring(NEW_ID_PREFIX.length()) + '"';
            match1 = '"' + match1 + '"';
        } else {
            return Collections.emptyList();
        }

        String namePrefix = androidNamePrefix + ':' + ATTR_LAYOUT_RESOURCE_PREFIX;
        List<TextEdit> edits = new ArrayList<TextEdit>();

        IStructuredDocumentRegion region = doc.getFirstStructuredDocumentRegion();
        for (; region != null; region = region.getNext()) {
            ITextRegionList list = region.getRegions();
            int regionStart = region.getStart();

            // Look at all attribute values and look for an id reference match
            String attributeName = ""; //$NON-NLS-1$
            for (int j = 0; j < region.getNumberOfRegions(); j++) {
                ITextRegion subRegion = list.get(j);
                String type = subRegion.getType();
                if (DOMRegionContext.XML_TAG_ATTRIBUTE_NAME.equals(type)) {
                    attributeName = region.getText(subRegion);
                } else if (DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE.equals(type)) {
                    // Only replace references in layout attributes
                    if (!attributeName.startsWith(namePrefix)) {
                        continue;
                    }
                    // Skip occurrences in the given skip range
                    int subRegionStart = regionStart + subRegion.getStart();
                    if (subRegionStart >= skipStart && subRegionStart <= skipEnd) {
                        continue;
                    }

                    String attributeValue = region.getText(subRegion);
                    if (attributeValue.equals(match1) || attributeValue.equals(match2)) {
                        int start = subRegionStart + 1; // skip quote
                        int end = start + rootId.length();

                        edits.add(new ReplaceEdit(start, end - start, referenceId));
                    }
                }
            }
        }

        return edits;
    }

    /** Get the id of the root selected element, if any */
    protected String getRootId() {
        Element primary = getPrimaryElement();
        if (primary != null) {
            String oldId = primary.getAttributeNS(ANDROID_URI, ATTR_ID);
            // id null check for https://bugs.eclipse.org/bugs/show_bug.cgi?id=272378
            if (oldId != null && oldId.length() > 0) {
                return oldId;
            }
        }

        return null;
    }

    protected String getAndroidNamespacePrefix() {
        if (mAndroidNamespacePrefix == null) {
            List<Attr> attributeNodes = findNamespaceAttributes();
            for (Node attributeNode : attributeNodes) {
                String prefix = attributeNode.getPrefix();
                if (XMLNS.equals(prefix)) {
                    String name = attributeNode.getNodeName();
                    String value = attributeNode.getNodeValue();
                    if (value.equals(ANDROID_URI)) {
                        mAndroidNamespacePrefix = name;
                        if (mAndroidNamespacePrefix.startsWith(XMLNS_PREFIX)) {
                            mAndroidNamespacePrefix =
                                mAndroidNamespacePrefix.substring(XMLNS_PREFIX.length());
                        }
                    }
                }
            }

            if (mAndroidNamespacePrefix == null) {
                mAndroidNamespacePrefix = ANDROID_NS_NAME;
            }
        }

        return mAndroidNamespacePrefix;
    }

    protected static String getAndroidNamespacePrefix(Document document) {
        String nsPrefix = null;
        List<Attr> attributeNodes = findNamespaceAttributes(document);
        for (Node attributeNode : attributeNodes) {
            String prefix = attributeNode.getPrefix();
            if (XMLNS.equals(prefix)) {
                String name = attributeNode.getNodeName();
                String value = attributeNode.getNodeValue();
                if (value.equals(ANDROID_URI)) {
                    nsPrefix = name;
                    if (nsPrefix.startsWith(XMLNS_PREFIX)) {
                        nsPrefix =
                            nsPrefix.substring(XMLNS_PREFIX.length());
                    }
                }
            }
        }

        if (nsPrefix == null) {
            nsPrefix = ANDROID_NS_NAME;
        }

        return nsPrefix;
    }

    protected List<Attr> findNamespaceAttributes() {
        Document document = getDomDocument();
        return findNamespaceAttributes(document);
    }

    protected static List<Attr> findNamespaceAttributes(Document document) {
        if (document != null) {
            Element root = document.getDocumentElement();
            return findNamespaceAttributes(root);
        }

        return Collections.emptyList();
    }

    protected static List<Attr> findNamespaceAttributes(Node root) {
        List<Attr> result = new ArrayList<Attr>();
        NamedNodeMap attributes = root.getAttributes();
        for (int i = 0, n = attributes.getLength(); i < n; i++) {
            Node attributeNode = attributes.item(i);

            String prefix = attributeNode.getPrefix();
            if (XMLNS.equals(prefix)) {
                result.add((Attr) attributeNode);
            }
        }

        return result;
    }

    protected List<Attr> findLayoutAttributes(Node root) {
        List<Attr> result = new ArrayList<Attr>();
        NamedNodeMap attributes = root.getAttributes();
        for (int i = 0, n = attributes.getLength(); i < n; i++) {
            Node attributeNode = attributes.item(i);

            String name = attributeNode.getLocalName();
            if (name.startsWith(ATTR_LAYOUT_RESOURCE_PREFIX)
                    && ANDROID_URI.equals(attributeNode.getNamespaceURI())) {
                result.add((Attr) attributeNode);
            }
        }

        return result;
    }

    protected String insertNamespace(String xmlText, String namespaceDeclarations) {
        // Insert namespace declarations into the extracted XML fragment
        int firstSpace = xmlText.indexOf(' ');
        int elementEnd = xmlText.indexOf('>');
        int insertAt;
        if (firstSpace != -1 && firstSpace < elementEnd) {
            insertAt = firstSpace;
        } else {
            insertAt = elementEnd;
        }
        xmlText = xmlText.substring(0, insertAt) + namespaceDeclarations
                + xmlText.substring(insertAt);

        return xmlText;
    }

    /** Remove sections of the document that correspond to top level layout attributes;
     * these are placed on the include element instead */
    protected String stripTopLayoutAttributes(Element primary, int start, String xml) {
        if (primary != null) {
            // List of attributes to remove
            List<IndexedRegion> skip = new ArrayList<IndexedRegion>();
            NamedNodeMap attributes = primary.getAttributes();
            for (int i = 0, n = attributes.getLength(); i < n; i++) {
                Node attr = attributes.item(i);
                String name = attr.getLocalName();
                if (name.startsWith(ATTR_LAYOUT_RESOURCE_PREFIX)
                        && ANDROID_URI.equals(attr.getNamespaceURI())) {
                    if (name.equals(ATTR_LAYOUT_WIDTH) || name.equals(ATTR_LAYOUT_HEIGHT)) {
                        // These are special and are left in
                        continue;
                    }

                    if (attr instanceof IndexedRegion) {
                        skip.add((IndexedRegion) attr);
                    }
                }
            }
            if (skip.size() > 0) {
                Collections.sort(skip, new Comparator<IndexedRegion>() {
                    // Sort in start order
                    @Override
                    public int compare(IndexedRegion r1, IndexedRegion r2) {
                        return r1.getStartOffset() - r2.getStartOffset();
                    }
                });

                // Successively cut out the various layout attributes
                // TODO remove adjacent whitespace too (but not newlines, unless they
                // are newly adjacent)
                StringBuilder sb = new StringBuilder(xml.length());
                int nextStart = 0;

                // Copy out all the sections except the skip sections
                for (IndexedRegion r : skip) {
                    int regionStart = r.getStartOffset();
                    // Adjust to string offsets since we've copied the string out of
                    // the document
                    regionStart -= start;

                    sb.append(xml.substring(nextStart, regionStart));

                    nextStart = regionStart + r.getLength();
                }
                if (nextStart < xml.length()) {
                    sb.append(xml.substring(nextStart));
                }

                return sb.toString();
            }
        }

        return xml;
    }

    protected static String getIndent(String line, int max) {
        int i = 0;
        int n = Math.min(max, line.length());
        for (; i < n; i++) {
            char c = line.charAt(i);
            if (!Character.isWhitespace(c)) {
                return line.substring(0, i);
            }
        }

        if (n < line.length()) {
            return line.substring(0, n);
        } else {
            return line;
        }
    }

    protected static String dedent(String xml) {
        String[] lines = xml.split("\n"); //$NON-NLS-1$
        if (lines.length < 2) {
            // The first line never has any indentation since we copy it out from the
            // element start index
            return xml;
        }

        String indentPrefix = getIndent(lines[1], lines[1].length());
        for (int i = 2, n = lines.length; i < n; i++) {
            String line = lines[i];

            // Ignore blank lines
            if (line.trim().length() == 0) {
                continue;
            }

            indentPrefix = getIndent(line, indentPrefix.length());

            if (indentPrefix.length() == 0) {
                return xml;
            }
        }

        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            if (line.startsWith(indentPrefix)) {
                sb.append(line.substring(indentPrefix.length()));
            } else {
                sb.append(line);
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    protected String getText(int start, int end) {
        try {
            IStructuredDocument document = mDelegate.getEditor().getStructuredDocument();
            return document.get(start, end - start);
        } catch (BadLocationException e) {
            // the region offset was invalid. ignore.
            return null;
        }
    }

    protected List<Element> getElements() {
        return mElements;
    }

    protected List<Element> initElements() {
        List<Element> nodes = new ArrayList<Element>();

        assert mTreeSelection == null || mSelection == null :
            "treeSel= " + mTreeSelection + ", sel=" + mSelection;

        // Initialize mSelectionStart and mSelectionEnd based on the selection context, which
        // is either a treeSelection (when invoked from the layout editor or the outline), or
        // a selection (when invoked from an XML editor)
        if (mTreeSelection != null) {
            int end = Integer.MIN_VALUE;
            int start = Integer.MAX_VALUE;
            for (TreePath path : mTreeSelection.getPaths()) {
                Object lastSegment = path.getLastSegment();
                if (lastSegment instanceof CanvasViewInfo) {
                    CanvasViewInfo viewInfo = (CanvasViewInfo) lastSegment;
                    UiViewElementNode uiNode = viewInfo.getUiViewNode();
                    if (uiNode == null) {
                        continue;
                    }
                    Node xmlNode = uiNode.getXmlNode();
                    if (xmlNode instanceof Element) {
                        Element element = (Element) xmlNode;
                        nodes.add(element);
                        IndexedRegion region = getRegion(element);
                        start = Math.min(start, region.getStartOffset());
                        end = Math.max(end, region.getEndOffset());
                    }
                }
            }
            if (start >= 0) {
                mSelectionStart = start;
                mSelectionEnd = end;
            }
        } else if (mSelection != null) {
            mSelectionStart = mSelection.getOffset();
            mSelectionEnd = mSelectionStart + mSelection.getLength();
            mOriginalSelectionStart = mSelectionStart;
            mOriginalSelectionEnd = mSelectionEnd;

            // Figure out the range of selected nodes from the document offsets
            IStructuredDocument doc = mDelegate.getEditor().getStructuredDocument();
            Pair<Element, Element> range = DomUtilities.getElementRange(doc,
                    mSelectionStart, mSelectionEnd);
            if (range != null) {
                Element first = range.getFirst();
                Element last = range.getSecond();

                // Adjust offsets to get rid of surrounding text nodes (if you happened
                // to select a text range and included whitespace on either end etc)
                mSelectionStart = getRegion(first).getStartOffset();
                mSelectionEnd = getRegion(last).getEndOffset();

                if (mSelectionStart > mSelectionEnd) {
                    int tmp = mSelectionStart;
                    mSelectionStart = mSelectionEnd;
                    mSelectionEnd = tmp;
                }

                if (first == last) {
                    nodes.add(first);
                } else if (first.getParentNode() == last.getParentNode()) {
                    // Add the range
                    Node node = first;
                    while (node != null) {
                        if (node instanceof Element) {
                            nodes.add((Element) node);
                        }
                        if (node == last) {
                            break;
                        }
                        node = node.getNextSibling();
                    }
                } else {
                    // Different parents: this means we have an uneven selection, selecting
                    // elements from different levels. We can't extract ranges like that.
                }
            }
        } else {
            assert false;
        }

        // Make sure that the list of elements is unique
        //Set<Element> seen = new HashSet<Element>();
        //for (Element element : nodes) {
        //   assert !seen.contains(element) : element;
        //   seen.add(element);
        //}

        return nodes;
    }

    protected Element getPrimaryElement() {
        List<Element> elements = getElements();
        if (elements != null && elements.size() == 1) {
            return elements.get(0);
        }

        return null;
    }

    protected Document getDomDocument() {
        if (mDelegate.getUiRootNode() != null) {
            return mDelegate.getUiRootNode().getXmlDocument();
        } else {
            return getElements().get(0).getOwnerDocument();
        }
    }

    protected List<CanvasViewInfo> getSelectedViewInfos() {
        List<CanvasViewInfo> infos = new ArrayList<CanvasViewInfo>();
        if (mTreeSelection != null) {
            for (TreePath path : mTreeSelection.getPaths()) {
                Object lastSegment = path.getLastSegment();
                if (lastSegment instanceof CanvasViewInfo) {
                    infos.add((CanvasViewInfo) lastSegment);
                }
            }
        }
        return infos;
    }

    protected boolean validateNotEmpty(List<CanvasViewInfo> infos, RefactoringStatus status) {
        if (infos.size() == 0) {
            status.addFatalError("No selection to extract");
            return false;
        }

        return true;
    }

    protected boolean validateNotRoot(List<CanvasViewInfo> infos, RefactoringStatus status) {
        for (CanvasViewInfo info : infos) {
            if (info.isRoot()) {
                status.addFatalError("Cannot refactor the root");
                return false;
            }
        }

        return true;
    }

    protected boolean validateContiguous(List<CanvasViewInfo> infos, RefactoringStatus status) {
        if (infos.size() > 1) {
            // All elements must be siblings (e.g. same parent)
            List<UiViewElementNode> nodes = new ArrayList<UiViewElementNode>(infos
                    .size());
            for (CanvasViewInfo info : infos) {
                UiViewElementNode node = info.getUiViewNode();
                if (node != null) {
                    nodes.add(node);
                }
            }
            if (nodes.size() == 0) {
                status.addFatalError("No selected views");
                return false;
            }

            UiElementNode parent = nodes.get(0).getUiParent();
            for (UiViewElementNode node : nodes) {
                if (parent != node.getUiParent()) {
                    status.addFatalError("The selected elements must be adjacent");
                    return false;
                }
            }
            // Ensure that the siblings are contiguous; no gaps.
            // If we've selected all the children of the parent then we don't need
            // to look.
            List<UiElementNode> siblings = parent.getUiChildren();
            if (siblings.size() != nodes.size()) {
                Set<UiViewElementNode> nodeSet = new HashSet<UiViewElementNode>(nodes);
                boolean inRange = false;
                int remaining = nodes.size();
                for (UiElementNode node : siblings) {
                    boolean in = nodeSet.contains(node);
                    if (in) {
                        remaining--;
                        if (remaining == 0) {
                            break;
                        }
                        inRange = true;
                    } else if (inRange) {
                        status.addFatalError("The selected elements must be adjacent");
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * Updates the given element with a new name if the current id reflects the old
     * element type. If the name was changed, it will return the new name.
     */
    protected String ensureIdMatchesType(Element element, String newType, MultiTextEdit rootEdit) {
        String oldType = element.getTagName();
        if (oldType.indexOf('.') == -1) {
            oldType = ANDROID_WIDGET_PREFIX + oldType;
        }
        String oldTypeBase = oldType.substring(oldType.lastIndexOf('.') + 1);
        String id = getId(element);
        if (id == null || id.length() == 0
                || id.toLowerCase(Locale.US).contains(oldTypeBase.toLowerCase(Locale.US))) {
            String newTypeBase = newType.substring(newType.lastIndexOf('.') + 1);
            return ensureHasId(rootEdit, element, newTypeBase);
        }

        return null;
    }

    /**
     * Returns the {@link IndexedRegion} for the given node
     *
     * @param node the node to look up the region for
     * @return the corresponding region, or null
     */
    public static IndexedRegion getRegion(Node node) {
        if (node instanceof IndexedRegion) {
            return (IndexedRegion) node;
        }

        return null;
    }

    protected String ensureHasId(MultiTextEdit rootEdit, Element element, String prefix) {
        return ensureHasId(rootEdit, element, prefix, true);
    }

    protected String ensureHasId(MultiTextEdit rootEdit, Element element, String prefix,
            boolean apply) {
        String id = mGeneratedIdMap.get(element);
        if (id != null) {
            return NEW_ID_PREFIX + id;
        }

        if (!element.hasAttributeNS(ANDROID_URI, ATTR_ID)
                || (prefix != null && !getId(element).startsWith(prefix))) {
            id = DomUtilities.getFreeWidgetId(element, mGeneratedIds, prefix);
            // Make sure we don't use this one again
            mGeneratedIds.add(id);
            mGeneratedIdMap.put(element, id);
            id = NEW_ID_PREFIX + id;
            if (apply) {
                setAttribute(rootEdit, element,
                        ANDROID_URI, getAndroidNamespacePrefix(), ATTR_ID, id);
            }
            return id;
        }

        return getId(element);
    }

    protected int getFirstAttributeOffset(Element element) {
        IndexedRegion region = getRegion(element);
        if (region != null) {
            int startOffset = region.getStartOffset();
            int endOffset = region.getEndOffset();
            String text = getText(startOffset, endOffset);
            String name = element.getLocalName();
            int nameOffset = text.indexOf(name);
            if (nameOffset != -1) {
                return startOffset + nameOffset + name.length();
            }
        }

        return -1;
    }

    /**
     * Returns the id of the given element
     *
     * @param element the element to look up the id for
     * @return the corresponding id, or an empty string (should not be null
     *         according to the DOM API, but has been observed to be null on
     *         some versions of Eclipse)
     */
    public static String getId(Element element) {
        return element.getAttributeNS(ANDROID_URI, ATTR_ID);
    }

    protected String ensureNewId(String id) {
        if (id != null && id.length() > 0) {
            if (id.startsWith(ID_PREFIX)) {
                id = NEW_ID_PREFIX + id.substring(ID_PREFIX.length());
            } else if (!id.startsWith(NEW_ID_PREFIX)) {
                id = NEW_ID_PREFIX + id;
            }
        } else {
            id = null;
        }

        return id;
    }

    protected String getViewClass(String fqcn) {
        // Don't include android.widget. as a package prefix in layout files
        if (fqcn.startsWith(ANDROID_WIDGET_PREFIX)) {
            fqcn = fqcn.substring(ANDROID_WIDGET_PREFIX.length());
        }

        return fqcn;
    }

    protected void setAttribute(MultiTextEdit rootEdit, Element element,
            String attributeUri,
            String attributePrefix, String attributeName, String attributeValue) {
        int offset = getFirstAttributeOffset(element);
        if (offset != -1) {
            if (element.hasAttributeNS(attributeUri, attributeName)) {
                replaceAttributeDeclaration(rootEdit, offset, element, attributePrefix,
                        attributeUri, attributeName, attributeValue);
            } else {
                addAttributeDeclaration(rootEdit, offset, attributePrefix, attributeName,
                        attributeValue);
            }
        }
    }

    private void addAttributeDeclaration(MultiTextEdit rootEdit, int offset,
            String attributePrefix, String attributeName, String attributeValue) {
        StringBuilder sb = new StringBuilder();
        sb.append(' ');

        if (attributePrefix != null) {
            sb.append(attributePrefix).append(':');
        }
        sb.append(attributeName).append('=').append('"');
        sb.append(attributeValue).append('"');

        InsertEdit setAttribute = new InsertEdit(offset, sb.toString());
        rootEdit.addChild(setAttribute);
    }

    /** Replaces the value declaration of the given attribute */
    private void replaceAttributeDeclaration(MultiTextEdit rootEdit, int offset,
            Element element, String attributePrefix, String attributeUri,
            String attributeName, String attributeValue) {
        // Find attribute value and replace it
        IStructuredModel model = mDelegate.getEditor().getModelForRead();
        try {
            IStructuredDocument doc = model.getStructuredDocument();

            IStructuredDocumentRegion region = doc.getRegionAtCharacterOffset(offset);
            ITextRegionList list = region.getRegions();
            int regionStart = region.getStart();

            int valueStart = -1;
            boolean useNextValue = false;
            String targetName = attributePrefix != null
                ? attributePrefix + ':' + attributeName : attributeName;

            // Look at all attribute values and look for an id reference match
            for (int j = 0; j < region.getNumberOfRegions(); j++) {
                ITextRegion subRegion = list.get(j);
                String type = subRegion.getType();
                if (DOMRegionContext.XML_TAG_ATTRIBUTE_NAME.equals(type)) {
                    // What about prefix?
                    if (targetName.equals(region.getText(subRegion))) {
                        useNextValue = true;
                    }
                } else if (DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE.equals(type)) {
                    if (useNextValue) {
                        valueStart = regionStart + subRegion.getStart();
                        break;
                    }
                }
            }

            if (valueStart != -1) {
                String oldValue = element.getAttributeNS(attributeUri, attributeName);
                int start = valueStart + 1; // Skip opening "
                ReplaceEdit setAttribute = new ReplaceEdit(start, oldValue.length(),
                        attributeValue);
                try {
                    rootEdit.addChild(setAttribute);
                } catch (MalformedTreeException mte) {
                    AdtPlugin.log(mte, "Could not replace attribute %1$s with %2$s",
                            attributeName, attributeValue);
                    throw mte;
                }
            }
        } finally {
            model.releaseFromRead();
        }
    }

    /** Strips out the given attribute, if defined */
    protected void removeAttribute(MultiTextEdit rootEdit, Element element, String uri,
            String attributeName) {
        if (element.hasAttributeNS(uri, attributeName)) {
            Attr attribute = element.getAttributeNodeNS(uri, attributeName);
            removeAttribute(rootEdit, attribute);
        }
    }

    /** Strips out the given attribute, if defined */
    protected void removeAttribute(MultiTextEdit rootEdit, Attr attribute) {
        IndexedRegion region = getRegion(attribute);
        if (region != null) {
            int startOffset = region.getStartOffset();
            int endOffset = region.getEndOffset();
            DeleteEdit deletion = new DeleteEdit(startOffset, endOffset - startOffset);
            rootEdit.addChild(deletion);
        }
    }


    /**
     * Removes the given element's opening and closing tags (including all of its
     * attributes) but leaves any children alone
     *
     * @param rootEdit the multi edit to add the removal operation to
     * @param element the element to delete the open and closing tags for
     * @param skip a list of elements that should not be modified (for example because they
     *    are targeted for deletion)
     *
     * TODO: Rename this to "unwrap" ? And allow for handling nested deletions.
     */
    protected void removeElementTags(MultiTextEdit rootEdit, Element element, List<Element> skip,
            boolean changeIndentation) {
        IndexedRegion elementRegion = getRegion(element);
        if (elementRegion == null) {
            return;
        }

        // Look for the opening tag
        IStructuredModel model = mDelegate.getEditor().getModelForRead();
        try {
            int startLineInclusive = -1;
            int endLineInclusive = -1;
            IStructuredDocument doc = model.getStructuredDocument();
            if (doc != null) {
                int start = elementRegion.getStartOffset();
                IStructuredDocumentRegion region = doc.getRegionAtCharacterOffset(start);
                ITextRegionList list = region.getRegions();
                int regionStart = region.getStart();
                int startOffset = regionStart;
                for (int j = 0; j < region.getNumberOfRegions(); j++) {
                    ITextRegion subRegion = list.get(j);
                    String type = subRegion.getType();
                    if (DOMRegionContext.XML_TAG_OPEN.equals(type)) {
                        startOffset = regionStart + subRegion.getStart();
                    } else if (DOMRegionContext.XML_TAG_CLOSE.equals(type)) {
                        int endOffset = regionStart + subRegion.getStart() + subRegion.getLength();

                        DeleteEdit deletion = createDeletion(doc, startOffset, endOffset);
                        rootEdit.addChild(deletion);
                        startLineInclusive = doc.getLineOfOffset(endOffset) + 1;
                        break;
                    }
                }

                // Find the close tag
                // Look at all attribute values and look for an id reference match
                region = doc.getRegionAtCharacterOffset(elementRegion.getEndOffset()
                        - element.getTagName().length() - 1);
                list = region.getRegions();
                regionStart = region.getStartOffset();
                startOffset = -1;
                for (int j = 0; j < region.getNumberOfRegions(); j++) {
                    ITextRegion subRegion = list.get(j);
                    String type = subRegion.getType();
                    if (DOMRegionContext.XML_END_TAG_OPEN.equals(type)) {
                        startOffset = regionStart + subRegion.getStart();
                    } else if (DOMRegionContext.XML_TAG_CLOSE.equals(type)) {
                        int endOffset = regionStart + subRegion.getStart() + subRegion.getLength();
                        if (startOffset != -1) {
                            DeleteEdit deletion = createDeletion(doc, startOffset, endOffset);
                            rootEdit.addChild(deletion);
                            endLineInclusive = doc.getLineOfOffset(startOffset) - 1;
                        }
                        break;
                    }
                }
            }

            // Dedent the contents
            if (changeIndentation && startLineInclusive != -1 && endLineInclusive != -1) {
                String indent = AndroidXmlEditor.getIndentAtOffset(doc, getRegion(element)
                        .getStartOffset());
                setIndentation(rootEdit, indent, doc, startLineInclusive, endLineInclusive,
                        element, skip);
            }
        } finally {
            model.releaseFromRead();
        }
    }

    protected void removeIndentation(MultiTextEdit rootEdit, String removeIndent,
            IStructuredDocument doc, int startLineInclusive, int endLineInclusive,
            Element element, List<Element> skip) {
        if (startLineInclusive > endLineInclusive) {
            return;
        }
        int indentLength = removeIndent.length();
        if (indentLength == 0) {
            return;
        }

        try {
            for (int line = startLineInclusive; line <= endLineInclusive; line++) {
                IRegion info = doc.getLineInformation(line);
                int lineStart = info.getOffset();
                int lineLength = info.getLength();
                int lineEnd = lineStart + lineLength;
                if (overlaps(lineStart, lineEnd, element, skip)) {
                    continue;
                }
                String lineText = getText(lineStart,
                        lineStart + Math.min(lineLength, indentLength));
                if (lineText.startsWith(removeIndent)) {
                    rootEdit.addChild(new DeleteEdit(lineStart, indentLength));
                }
            }
        } catch (BadLocationException e) {
            AdtPlugin.log(e, null);
        }
    }

    protected void setIndentation(MultiTextEdit rootEdit, String indent,
            IStructuredDocument doc, int startLineInclusive, int endLineInclusive,
            Element element, List<Element> skip) {
        if (startLineInclusive > endLineInclusive) {
            return;
        }
        int indentLength = indent.length();
        if (indentLength == 0) {
            return;
        }

        try {
            for (int line = startLineInclusive; line <= endLineInclusive; line++) {
                IRegion info = doc.getLineInformation(line);
                int lineStart = info.getOffset();
                int lineLength = info.getLength();
                int lineEnd = lineStart + lineLength;
                if (overlaps(lineStart, lineEnd, element, skip)) {
                    continue;
                }
                String lineText = getText(lineStart, lineStart + lineLength);
                int indentEnd = getFirstNonSpace(lineText);
                rootEdit.addChild(new ReplaceEdit(lineStart, indentEnd, indent));
            }
        } catch (BadLocationException e) {
            AdtPlugin.log(e, null);
        }
    }

    private int getFirstNonSpace(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isWhitespace(s.charAt(i))) {
                return i;
            }
        }

        return s.length();
    }

    /** Returns true if the given line overlaps any of the given elements */
    private static boolean overlaps(int startOffset, int endOffset,
            Element element, List<Element> overlaps) {
        for (Element e : overlaps) {
            if (e == element) {
                continue;
            }

            IndexedRegion region = getRegion(e);
            if (region.getEndOffset() >= startOffset && region.getStartOffset() <= endOffset) {
                return true;
            }
        }
        return false;
    }

    protected DeleteEdit createDeletion(IStructuredDocument doc, int startOffset, int endOffset) {
        // Expand to delete the whole line?
        try {
            IRegion info = doc.getLineInformationOfOffset(startOffset);
            int lineBegin = info.getOffset();
            // Is the text on the line leading up to the deletion region,
            // and the text following it, all whitespace?
            boolean deleteLine = true;
            if (lineBegin < startOffset) {
                String prefix = getText(lineBegin, startOffset);
                if (prefix.trim().length() > 0) {
                    deleteLine = false;
                }
            }
            info = doc.getLineInformationOfOffset(endOffset);
            int lineEnd = info.getOffset() + info.getLength();
            if (lineEnd > endOffset) {
                String suffix = getText(endOffset, lineEnd);
                if (suffix.trim().length() > 0) {
                    deleteLine = false;
                }
            }
            if (deleteLine) {
                startOffset = lineBegin;
                endOffset = Math.min(doc.getLength(), lineEnd + 1);
            }
        } catch (BadLocationException e) {
            AdtPlugin.log(e, null);
        }


        return new DeleteEdit(startOffset, endOffset - startOffset);
    }

    /**
     * Rewrite the edits in the given {@link MultiTextEdit} such that same edits are
     * applied, but the resulting range is also formatted
     */
    protected MultiTextEdit reformat(MultiTextEdit edit, XmlFormatStyle style) {
        String xml = mDelegate.getEditor().getStructuredDocument().get();
        return reformat(xml, edit, style);
    }

    /**
     * Rewrite the edits in the given {@link MultiTextEdit} such that same edits are
     * applied, but the resulting range is also formatted
     *
     * @param oldContents the original contents that should be edited by a
     *            {@link MultiTextEdit}
     * @param edit the {@link MultiTextEdit} to be applied to some string
     * @param style the formatting style to use
     * @return a new {@link MultiTextEdit} which performs the same edits as the input edit
     *         but also reformats the text
     */
    public static MultiTextEdit reformat(String oldContents, MultiTextEdit edit,
            XmlFormatStyle style) {
        IDocument document = new org.eclipse.jface.text.Document();
        document.set(oldContents);

        try {
            edit.apply(document);
        } catch (MalformedTreeException e) {
            AdtPlugin.log(e, null);
            return null; // Abort formatting
        } catch (BadLocationException e) {
            AdtPlugin.log(e, null);
            return null; // Abort formatting
        }

        String actual = document.get();

        // TODO: Try to format only the affected portion of the document.
        // To do that we need to find out what the affected offsets are; we know
        // the MultiTextEdit's affected range, but that is referring to offsets
        // in the old document. Use that to compute offsets in the new document.
        //int distanceFromEnd = actual.length() - edit.getExclusiveEnd();
        //IStructuredModel model = DomUtilities.createStructuredModel(actual);
        //int start = edit.getOffset();
        //int end = actual.length() - distanceFromEnd;
        //int length = end - start;
        //TextEdit format = AndroidXmlFormattingStrategy.format(model, start, length);
        EclipseXmlFormatPreferences formatPrefs = EclipseXmlFormatPreferences.create();
        String formatted = EclipseXmlPrettyPrinter.prettyPrint(actual, formatPrefs, style,
                null /*lineSeparator*/);


        // Figure out how much of the before and after strings are identical and narrow
        // the replacement scope
        boolean foundDifference = false;
        int firstDifference = 0;
        int lastDifference = formatted.length();
        int start = 0;
        int end = oldContents.length();

        for (int i = 0, j = start; i < formatted.length() && j < end; i++, j++) {
            if (formatted.charAt(i) != oldContents.charAt(j)) {
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
        for (int i = formatted.length() - 1, j = end - 1;
                i > firstDifference && j > start;
                i--, j--) {
            if (formatted.charAt(i) != oldContents.charAt(j)) {
                lastDifference = i + 1;
                break;
            }
        }

        start += firstDifference;
        end -= (formatted.length() - lastDifference);
        end = Math.max(start, end);
        formatted = formatted.substring(firstDifference, lastDifference);

        ReplaceEdit format = new ReplaceEdit(start, end - start,
                formatted);

        MultiTextEdit newEdit = new MultiTextEdit();
        newEdit.addChild(format);

        return newEdit;
    }

    protected ViewElementDescriptor getElementDescriptor(String fqcn) {
        AndroidTargetData data = mDelegate.getEditor().getTargetData();
        if (data != null) {
            return data.getLayoutDescriptors().findDescriptorByClass(fqcn);
        }

        return null;
    }

    /** Create a wizard for this refactoring */
    abstract VisualRefactoringWizard createWizard();

    public abstract static class VisualRefactoringDescriptor extends RefactoringDescriptor {
        private final Map<String, String> mArguments;

        public VisualRefactoringDescriptor(
                String id, String project, String description, String comment,
                Map<String, String> arguments) {
            super(id, project, description, comment, STRUCTURAL_CHANGE | MULTI_CHANGE);
            mArguments = arguments;
        }

        public Map<String, String> getArguments() {
            return mArguments;
        }

        protected abstract Refactoring createRefactoring(Map<String, String> args);

        @Override
        public Refactoring createRefactoring(RefactoringStatus status) throws CoreException {
            try {
                return createRefactoring(mArguments);
            } catch (NullPointerException e) {
                status.addFatalError("Failed to recreate refactoring from descriptor");
                return null;
            }
        }
    }
}

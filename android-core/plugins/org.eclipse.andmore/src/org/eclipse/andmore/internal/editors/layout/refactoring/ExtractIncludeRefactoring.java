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
import static com.android.SdkConstants.ATTR_ID;
import static com.android.SdkConstants.ATTR_LAYOUT_HEIGHT;
import static com.android.SdkConstants.ATTR_LAYOUT_RESOURCE_PREFIX;
import static com.android.SdkConstants.ATTR_LAYOUT_WIDTH;
import static com.android.SdkConstants.DOT_XML;
import static com.android.SdkConstants.EXT_XML;
import static com.android.SdkConstants.FD_RES;
import static com.android.SdkConstants.FD_RESOURCES;
import static com.android.SdkConstants.FD_RES_LAYOUT;
import static com.android.SdkConstants.ID_PREFIX;
import static com.android.SdkConstants.NEW_ID_PREFIX;
import static com.android.SdkConstants.VALUE_WRAP_CONTENT;
import static com.android.SdkConstants.VIEW_INCLUDE;
import static com.android.SdkConstants.XMLNS;
import static com.android.SdkConstants.XMLNS_PREFIX;
import static com.android.resources.ResourceType.LAYOUT;
import static org.eclipse.andmore.AdtConstants.WS_SEP;

import com.android.annotations.NonNull;
import com.android.annotations.VisibleForTesting;
import com.android.ide.common.xml.XmlFormatStyle;
import com.android.utils.XmlUtils;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.editors.formatting.EclipseXmlFormatPreferences;
import org.eclipse.andmore.internal.editors.formatting.EclipseXmlPrettyPrinter;
import org.eclipse.andmore.internal.editors.layout.LayoutEditorDelegate;
import org.eclipse.andmore.internal.editors.layout.gle2.CanvasViewInfo;
import org.eclipse.andmore.internal.editors.layout.gle2.DomUtilities;
import org.eclipse.andmore.internal.editors.layout.uimodel.UiViewElementNode;
import org.eclipse.andmore.internal.preferences.AdtPrefs;
import org.eclipse.andmore.internal.resources.ResourceNameValidator;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.NullChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.swt.widgets.Display;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMDocument;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Extracts the selection and writes it out as a separate layout file, then adds an
 * include to that new layout file. Interactively asks the user for a new name for the
 * layout.
 */
@SuppressWarnings("restriction") // XML model
public class ExtractIncludeRefactoring extends VisualRefactoring {
    private static final String KEY_NAME = "name";                      //$NON-NLS-1$
    private static final String KEY_OCCURRENCES = "all-occurrences";    //$NON-NLS-1$
    private String mLayoutName;
    private boolean mReplaceOccurrences;

    /**
     * This constructor is solely used by {@link Descriptor},
     * to replay a previous refactoring.
     * @param arguments argument map created by #createArgumentMap.
     */
    ExtractIncludeRefactoring(Map<String, String> arguments) {
        super(arguments);
        mLayoutName = arguments.get(KEY_NAME);
        mReplaceOccurrences  = Boolean.parseBoolean(arguments.get(KEY_OCCURRENCES));
    }

    public ExtractIncludeRefactoring(
            IFile file,
            LayoutEditorDelegate delegate,
            ITextSelection selection,
            ITreeSelection treeSelection) {
        super(file, delegate, selection, treeSelection);
    }

    @VisibleForTesting
	public
    ExtractIncludeRefactoring(List<Element> selectedElements, LayoutEditorDelegate editor) {
        super(selectedElements, editor);
    }

    @Override
    public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException,
            OperationCanceledException {
        RefactoringStatus status = new RefactoringStatus();

        try {
            pm.beginTask("Checking preconditions...", 6);

            if (mSelectionStart == -1 || mSelectionEnd == -1) {
                status.addFatalError("No selection to extract");
                return status;
            }

            // Make sure the selection is contiguous
            if (mTreeSelection != null) {
                // TODO - don't do this if we based the selection on text. In this case,
                // make sure we're -balanced-.
                List<CanvasViewInfo> infos = getSelectedViewInfos();
                if (!validateNotEmpty(infos, status)) {
                    return status;
                }

                if (!validateNotRoot(infos, status)) {
                    return status;
                }

                // Disable if you've selected a single include tag
                if (infos.size() == 1) {
                    UiViewElementNode uiNode = infos.get(0).getUiViewNode();
                    if (uiNode != null) {
                        Node xmlNode = uiNode.getXmlNode();
                        if (xmlNode.getLocalName().equals(VIEW_INCLUDE)) {
                            status.addWarning("No point in refactoring a single include tag");
                        }
                    }
                }

                // Enforce that the selection is -contiguous-
                if (!validateContiguous(infos, status)) {
                    return status;
                }
            }

            // This also ensures that we have a valid DOM model:
            if (mElements.size() == 0) {
                status.addFatalError("Nothing to extract");
                return status;
            }

            pm.worked(1);
            return status;

        } finally {
            pm.done();
        }
    }

    @Override
    protected VisualRefactoringDescriptor createDescriptor() {
        String comment = getName();
        return new Descriptor(
                mProject.getName(), //project
                comment, //description
                comment, //comment
                createArgumentMap());
    }

    @Override
    protected Map<String, String> createArgumentMap() {
        Map<String, String> args = super.createArgumentMap();
        args.put(KEY_NAME, mLayoutName);
        args.put(KEY_OCCURRENCES, Boolean.toString(mReplaceOccurrences));

        return args;
    }

    @Override
    public String getName() {
        return "Extract as Include";
    }

    public void setLayoutName(String layoutName) {
        mLayoutName = layoutName;
    }

    public void setReplaceOccurrences(boolean selection) {
        mReplaceOccurrences = selection;
    }

    // ---- Actual implementation of Extract as Include modification computation ----

    @Override
    @NonNull
	public List<Change> computeChanges(IProgressMonitor monitor) {
        String extractedText = getExtractedText();

        String namespaceDeclarations = computeNamespaceDeclarations();

        // Insert namespace:
        extractedText = insertNamespace(extractedText, namespaceDeclarations);

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"); //$NON-NLS-1$
        sb.append(extractedText);
        sb.append('\n');

        List<Change> changes = new ArrayList<Change>();

        String newFileName = mLayoutName + DOT_XML;
        IProject project = mDelegate.getEditor().getProject();
        IFile sourceFile = mDelegate.getEditor().getInputFile();
        if (sourceFile == null) {
            return changes;
        }

        // Replace extracted elements by <include> tag
        handleIncludingFile(changes, sourceFile, mSelectionStart, mSelectionEnd,
                getDomDocument(), getPrimaryElement());

        // Also extract in other variations of the same file (landscape/portrait, etc)
        boolean haveVariations = false;
        if (mReplaceOccurrences) {
            List<IFile> layouts = getOtherLayouts(sourceFile);
            for (IFile file : layouts) {
                IModelManager modelManager = StructuredModelManager.getModelManager();
                IStructuredModel model = null;
                // We could enhance this with a SubMonitor to make the progress bar move as
                // well.
                monitor.subTask(String.format("Looking for duplicates in %1$s",
                        file.getProjectRelativePath()));
                if (monitor.isCanceled()) {
                    throw new OperationCanceledException();
                }

                try {
                    model = modelManager.getModelForRead(file);
                    if (model instanceof IDOMModel) {
                        IDOMModel domModel = (IDOMModel) model;
                        IDOMDocument otherDocument = domModel.getDocument();
                        List<Element> otherElements = new ArrayList<Element>();
                        Element otherPrimary = null;

                        for (Element element : getElements()) {
                            Element other = DomUtilities.findCorresponding(element,
                                    otherDocument);
                            if (other != null) {
                                // See if the structure is similar to what we have in this
                                // document
                                if (DomUtilities.isEquivalent(element, other)) {
                                    otherElements.add(other);
                                    if (element == getPrimaryElement()) {
                                        otherPrimary = other;
                                    }
                                }
                            }
                        }

                        // Only perform extract in the other file if we find a match for
                        // ALL of elements being extracted, and if they too are contiguous
                        if (otherElements.size() == getElements().size() &&
                                DomUtilities.isContiguous(otherElements)) {
                            // Find the range
                            int begin = Integer.MAX_VALUE;
                            int end = Integer.MIN_VALUE;
                            for (Element element : otherElements) {
                                // Yes!! Extract this one as well!
                                IndexedRegion region = getRegion(element);
                                end = Math.max(end, region.getEndOffset());
                                begin = Math.min(begin, region.getStartOffset());
                            }
                            handleIncludingFile(changes, file, begin,
                                    end, otherDocument, otherPrimary);
                            haveVariations = true;
                        }
                    }
                } catch (IOException e) {
                    AdtPlugin.log(e, null);
                } catch (CoreException e) {
                    AdtPlugin.log(e, null);
                } finally {
                    if (model != null) {
                        model.releaseFromRead();
                    }
                }
            }
        }

        // Add change to create the new file
        IContainer parent = sourceFile.getParent();
        if (haveVariations) {
            // If we're extracting from multiple configuration folders, then we need to
            // place the extracted include in the base layout folder (if not it goes next to
            // the including file)
            parent = mProject.getFolder(FD_RES).getFolder(FD_RES_LAYOUT);
        }
        IPath parentPath = parent.getProjectRelativePath();
        final IFile file = project.getFile(new Path(parentPath + WS_SEP + newFileName));
        TextFileChange addFile = new TextFileChange("Create new separate layout", file);
        addFile.setTextType(EXT_XML);
        changes.add(addFile);

        String newFile = sb.toString();
        if (AdtPrefs.getPrefs().getFormatGuiXml()) {
            newFile = EclipseXmlPrettyPrinter.prettyPrint(newFile,
                    EclipseXmlFormatPreferences.create(), XmlFormatStyle.LAYOUT,
                    null /*lineSeparator*/);
        }
        addFile.setEdit(new InsertEdit(0, newFile));

        Change finishHook = createFinishHook(file);
        changes.add(finishHook);

        return changes;
    }

    private void handleIncludingFile(List<Change> changes,
            IFile sourceFile, int begin, int end, Document document, Element primary) {
        TextFileChange change = new TextFileChange(sourceFile.getName(), sourceFile);
        MultiTextEdit rootEdit = new MultiTextEdit();
        change.setTextType(EXT_XML);
        changes.add(change);

        String referenceId = getReferenceId();
        // Replace existing elements in the source file and insert <include>
        String androidNsPrefix = getAndroidNamespacePrefix(document);
        String include = computeIncludeString(primary, mLayoutName, androidNsPrefix, referenceId);
        int length = end - begin;
        ReplaceEdit replace = new ReplaceEdit(begin, length, include);
        rootEdit.addChild(replace);

        // Update any layout references to the old id with the new id
        if (referenceId != null && primary != null) {
            String rootId = getId(primary);
            IStructuredModel model = null;
            try {
                model = StructuredModelManager.getModelManager().getModelForRead(sourceFile);
                IStructuredDocument doc = model.getStructuredDocument();
                if (doc != null && rootId != null) {
                    List<TextEdit> replaceIds = replaceIds(androidNsPrefix, doc, begin,
                            end, rootId, referenceId);
                    for (TextEdit edit : replaceIds) {
                        rootEdit.addChild(edit);
                    }

                    if (AdtPrefs.getPrefs().getFormatGuiXml()) {
                        MultiTextEdit formatted = reformat(doc.get(), rootEdit,
                                XmlFormatStyle.LAYOUT);
                        if (formatted != null) {
                            rootEdit = formatted;
                        }
                    }
                }
            } catch (IOException e) {
                AdtPlugin.log(e, null);
            } catch (CoreException e) {
                AdtPlugin.log(e, null);
            } finally {
                if (model != null) {
                    model.releaseFromRead();
                }
            }
        }

        change.setEdit(rootEdit);
    }

    /**
     * Returns a list of all the other layouts (in all configurations) in the project other
     * than the given source layout where the refactoring was initiated. Never null.
     */
    private List<IFile> getOtherLayouts(IFile sourceFile) {
        List<IFile> layouts = new ArrayList<IFile>(100);
        IPath sourcePath = sourceFile.getProjectRelativePath();
        IFolder resources = mProject.getFolder(FD_RESOURCES);
        try {
            for (IResource folder : resources.members()) {
                if (folder.getName().startsWith(FD_RES_LAYOUT) &&
                        folder instanceof IFolder) {
                    IFolder layoutFolder = (IFolder) folder;
                    for (IResource file : layoutFolder.members()) {
                        if (file.getName().endsWith(EXT_XML)
                                && file instanceof IFile) {
                            if (!file.getProjectRelativePath().equals(sourcePath)) {
                                layouts.add((IFile) file);
                            }
                        }
                    }
                }
            }
        } catch (CoreException e) {
            AdtPlugin.log(e, null);
        }

        return layouts;
    }

    String getInitialName() {
        String defaultName = ""; //$NON-NLS-1$
        Element primary = getPrimaryElement();
        if (primary != null) {
            String id = primary.getAttributeNS(ANDROID_URI, ATTR_ID);
            // id null check for https://bugs.eclipse.org/bugs/show_bug.cgi?id=272378
            if (id != null && (id.startsWith(ID_PREFIX) || id.startsWith(NEW_ID_PREFIX))) {
                // Use everything following the id/, and make it lowercase since that is
                // the convention for layouts (and use Locale.US to ensure that "Image" becomes
                // "image" etc)
                defaultName = id.substring(id.indexOf('/') + 1).toLowerCase(Locale.US);

                IInputValidator validator = ResourceNameValidator.create(true, mProject, LAYOUT);

                if (validator.isValid(defaultName) != null) { // Already exists?
                    defaultName = ""; //$NON-NLS-1$
                }
            }
        }

        return defaultName;
    }

    IFile getSourceFile() {
        return mFile;
    }

    private Change createFinishHook(final IFile file) {
        return new NullChange("Open extracted layout and refresh resources") {
            @Override
            public Change perform(IProgressMonitor pm) throws CoreException {
                Display display = AdtPlugin.getDisplay();
                display.asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        openFile(file);
                        mDelegate.getGraphicalEditor().refreshProjectResources();
                        // Save file to trigger include finder scanning (as well as making
                        // the
                        // actual show-include feature work since it relies on reading
                        // files from
                        // disk, not a live buffer)
                        IWorkbenchPage page = mDelegate.getEditor().getEditorSite().getPage();
                        page.saveEditor(mDelegate.getEditor(), false);
                    }
                });

                // Not undoable: just return null instead of an undo-change.
                return null;
            }
        };
    }

    private String computeNamespaceDeclarations() {
        String androidNsPrefix = null;
        String namespaceDeclarations = null;

        StringBuilder sb = new StringBuilder();
        List<Attr> attributeNodes = findNamespaceAttributes();
        for (Node attributeNode : attributeNodes) {
            String prefix = attributeNode.getPrefix();
            if (XMLNS.equals(prefix)) {
                sb.append(' ');
                String name = attributeNode.getNodeName();
                sb.append(name);
                sb.append('=').append('"');

                String value = attributeNode.getNodeValue();
                if (value.equals(ANDROID_URI)) {
                    androidNsPrefix = name;
                    if (androidNsPrefix.startsWith(XMLNS_PREFIX)) {
                        androidNsPrefix = androidNsPrefix.substring(XMLNS_PREFIX.length());
                    }
                }
                sb.append(XmlUtils.toXmlAttributeValue(value));
                sb.append('"');
            }
        }
        namespaceDeclarations = sb.toString();

        if (androidNsPrefix == null) {
            androidNsPrefix = ANDROID_NS_NAME;
        }

        if (namespaceDeclarations.length() == 0) {
            sb.setLength(0);
            sb.append(' ');
            sb.append(XMLNS_PREFIX);
            sb.append(androidNsPrefix);
            sb.append('=').append('"');
            sb.append(ANDROID_URI);
            sb.append('"');
            namespaceDeclarations = sb.toString();
        }

        return namespaceDeclarations;
    }

    /** Returns the id to be used for the include tag itself (may be null) */
    private String getReferenceId() {
        String rootId = getRootId();
        if (rootId != null) {
            return rootId + "_ref";
        }

        return null;
    }

    /**
     * Compute the actual {@code <include>} string to be inserted in place of the old
     * selection
     */
    private static String computeIncludeString(Element primaryNode, String newName,
            String androidNsPrefix, String referenceId) {
        StringBuilder sb = new StringBuilder();
        sb.append("<include layout=\"@layout/"); //$NON-NLS-1$
        sb.append(newName);
        sb.append('"');
        sb.append(' ');

        // Create new id for the include itself
        if (referenceId != null) {
            sb.append(androidNsPrefix);
            sb.append(':');
            sb.append(ATTR_ID);
            sb.append('=').append('"');
            sb.append(referenceId);
            sb.append('"').append(' ');
        }

        // Add id string, unless it's a <merge>, since we may need to adjust any layout
        // references to apply to the <include> tag instead

        // I should move all the layout_ attributes as well
        // I also need to duplicate and modify the id and then replace
        // everything else in the file with this new id...

        // HACK: see issue 13494: We must duplicate the width/height attributes on the
        // <include> statement for designtime rendering only
        String width = null;
        String height = null;
        if (primaryNode == null) {
            // Multiple selection - in that case we will be creating an outer <merge>
            // so we need to set our own width/height on it
            width = height = VALUE_WRAP_CONTENT;
        } else {
            if (!primaryNode.hasAttributeNS(ANDROID_URI, ATTR_LAYOUT_WIDTH)) {
                width = VALUE_WRAP_CONTENT;
            } else {
                width = primaryNode.getAttributeNS(ANDROID_URI, ATTR_LAYOUT_WIDTH);
            }
            if (!primaryNode.hasAttributeNS(ANDROID_URI, ATTR_LAYOUT_HEIGHT)) {
                height = VALUE_WRAP_CONTENT;
            } else {
                height = primaryNode.getAttributeNS(ANDROID_URI, ATTR_LAYOUT_HEIGHT);
            }
        }
        if (width != null) {
            sb.append(' ');
            sb.append(androidNsPrefix);
            sb.append(':');
            sb.append(ATTR_LAYOUT_WIDTH);
            sb.append('=').append('"');
            sb.append(XmlUtils.toXmlAttributeValue(width));
            sb.append('"');
        }
        if (height != null) {
            sb.append(' ');
            sb.append(androidNsPrefix);
            sb.append(':');
            sb.append(ATTR_LAYOUT_HEIGHT);
            sb.append('=').append('"');
            sb.append(XmlUtils.toXmlAttributeValue(height));
            sb.append('"');
        }

        // Duplicate all the other layout attributes as well
        if (primaryNode != null) {
            NamedNodeMap attributes = primaryNode.getAttributes();
            for (int i = 0, n = attributes.getLength(); i < n; i++) {
                Node attr = attributes.item(i);
                String name = attr.getLocalName();
                if (name.startsWith(ATTR_LAYOUT_RESOURCE_PREFIX)
                        && ANDROID_URI.equals(attr.getNamespaceURI())) {
                    if (name.equals(ATTR_LAYOUT_WIDTH) || name.equals(ATTR_LAYOUT_HEIGHT)) {
                        // Already handled
                        continue;
                    }

                    sb.append(' ');
                    sb.append(androidNsPrefix);
                    sb.append(':');
                    sb.append(name);
                    sb.append('=').append('"');
                    sb.append(XmlUtils.toXmlAttributeValue(attr.getNodeValue()));
                    sb.append('"');
                }
            }
        }

        sb.append("/>");
        return sb.toString();
    }

    /** Return the text in the document in the range start to end */
    private String getExtractedText() {
        String xml = getText(mSelectionStart, mSelectionEnd);
        Element primaryNode = getPrimaryElement();
        xml = stripTopLayoutAttributes(primaryNode, mSelectionStart, xml);
        xml = dedent(xml);

        // Wrap siblings in <merge>?
        if (primaryNode == null) {
            StringBuilder sb = new StringBuilder();
            sb.append("<merge>\n"); //$NON-NLS-1$
            // indent an extra level
            for (String line : xml.split("\n")) { //$NON-NLS-1$
                sb.append("    "); //$NON-NLS-1$
                sb.append(line).append('\n');
            }
            sb.append("</merge>\n"); //$NON-NLS-1$
            xml = sb.toString();
        }

        return xml;
    }

    @Override
    VisualRefactoringWizard createWizard() {
        return new ExtractIncludeWizard(this, mDelegate);
    }

    public static class Descriptor extends VisualRefactoringDescriptor {
        public Descriptor(String project, String description, String comment,
                Map<String, String> arguments) {
            super("org.eclipse.andmore.refactoring.extract.include", //$NON-NLS-1$
                    project, description, comment, arguments);
        }

        @Override
        protected Refactoring createRefactoring(Map<String, String> args) {
            return new ExtractIncludeRefactoring(args);
        }
    }
}

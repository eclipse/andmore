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
import static com.android.SdkConstants.ANDROID_NS_NAME_PREFIX;
import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_HINT;
import static com.android.SdkConstants.ATTR_ID;
import static com.android.SdkConstants.ATTR_LAYOUT_MARGIN;
import static com.android.SdkConstants.ATTR_LAYOUT_RESOURCE_PREFIX;
import static com.android.SdkConstants.ATTR_NAME;
import static com.android.SdkConstants.ATTR_ON_CLICK;
import static com.android.SdkConstants.ATTR_PARENT;
import static com.android.SdkConstants.ATTR_SRC;
import static com.android.SdkConstants.ATTR_STYLE;
import static com.android.SdkConstants.ATTR_TEXT;
import static com.android.SdkConstants.EXT_XML;
import static com.android.SdkConstants.FD_RESOURCES;
import static com.android.SdkConstants.FD_RES_VALUES;
import static com.android.SdkConstants.PREFIX_ANDROID;
import static com.android.SdkConstants.PREFIX_RESOURCE_REF;
import static com.android.SdkConstants.REFERENCE_STYLE;
import static com.android.SdkConstants.TAG_ITEM;
import static com.android.SdkConstants.TAG_RESOURCES;
import static com.android.SdkConstants.XMLNS_PREFIX;
import static org.eclipse.andmore.AdtConstants.WS_SEP;

import com.android.annotations.NonNull;
import com.android.annotations.VisibleForTesting;
import com.android.ide.common.rendering.api.ResourceValue;
import com.android.ide.common.resources.ResourceResolver;
import com.android.ide.common.xml.XmlFormatStyle;
import com.android.utils.Pair;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.editors.AndroidXmlEditor;
import org.eclipse.andmore.internal.editors.descriptors.DescriptorsUtils;
import org.eclipse.andmore.internal.editors.layout.LayoutEditorDelegate;
import org.eclipse.andmore.internal.preferences.AdtPrefs;
import org.eclipse.andmore.internal.wizards.newxmlfile.NewXmlFileWizard;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMDocument;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Extracts the selection and writes it out as a separate layout file, then adds an
 * include to that new layout file. Interactively asks the user for a new name for the
 * layout.
 * <p>
 * Remaining work to do / Possible enhancements:
 * <ul>
 * <li>Optionally look in other files in the project and attempt to set style attributes
 * in other cases where the style attributes match?
 * <li>If the elements we are extracting from already contain a style attribute, set that
 * style as the parent style of the current style?
 * <li>Add a parent-style picker to the wizard (initialized with the above if applicable)
 * <li>Pick up indentation settings from the XML module
 * <li>Integrate with themes somehow -- make an option to have the extracted style go into
 *    the theme instead
 * </ul>
 */
@SuppressWarnings("restriction") // XML model
public class ExtractStyleRefactoring extends VisualRefactoring {
    private static final String KEY_NAME = "name";                        //$NON-NLS-1$
    private static final String KEY_REMOVE_EXTRACTED = "removeextracted"; //$NON-NLS-1$
    private static final String KEY_REMOVE_ALL = "removeall";             //$NON-NLS-1$
    private static final String KEY_APPLY_STYLE = "applystyle";           //$NON-NLS-1$
    private static final String KEY_PARENT = "parent";           //$NON-NLS-1$
    private String mStyleName;
    /** The name of the file in res/values/ that the style will be added to. Normally
     * res/values/styles.xml - but unit tests pick other names */
    private String mStyleFileName = "styles.xml";
    /** Set a style reference on the extracted elements? */
    private boolean mApplyStyle;
    /** Remove the attributes that were extracted? */
    private boolean mRemoveExtracted;
    /** List of attributes chosen by the user to be extracted */
    private List<Attr> mChosenAttributes = new ArrayList<Attr>();
    /** Remove all attributes that match the extracted attributes names, regardless of value */
    private boolean mRemoveAll;
    /** The parent style to extend */
    private String mParent;
    /** The full list of available attributes in the refactoring */
    private Map<String, List<Attr>> mAvailableAttributes;

    /**
     * This constructor is solely used by {@link Descriptor},
     * to replay a previous refactoring.
     * @param arguments argument map created by #createArgumentMap.
     */
    ExtractStyleRefactoring(Map<String, String> arguments) {
        super(arguments);
        mStyleName = arguments.get(KEY_NAME);
        mRemoveExtracted = Boolean.parseBoolean(arguments.get(KEY_REMOVE_EXTRACTED));
        mRemoveAll = Boolean.parseBoolean(arguments.get(KEY_REMOVE_ALL));
        mApplyStyle = Boolean.parseBoolean(arguments.get(KEY_APPLY_STYLE));
        mParent = arguments.get(KEY_PARENT);
        if (mParent != null && mParent.length() == 0) {
            mParent = null;
        }
    }

    public ExtractStyleRefactoring(
            IFile file,
            LayoutEditorDelegate delegate,
            ITextSelection selection,
            ITreeSelection treeSelection) {
        super(file, delegate, selection, treeSelection);
    }

    @VisibleForTesting
    ExtractStyleRefactoring(List<Element> selectedElements, LayoutEditorDelegate editor) {
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
                comment,            //description
                comment,            //comment
                createArgumentMap());
    }

    @Override
    protected Map<String, String> createArgumentMap() {
        Map<String, String> args = super.createArgumentMap();
        args.put(KEY_NAME, mStyleName);
        args.put(KEY_REMOVE_EXTRACTED, Boolean.toString(mRemoveExtracted));
        args.put(KEY_REMOVE_ALL, Boolean.toString(mRemoveAll));
        args.put(KEY_APPLY_STYLE, Boolean.toString(mApplyStyle));
        args.put(KEY_PARENT, mParent != null ? mParent : "");

        return args;
    }

    @Override
    public String getName() {
        return "Extract Style";
    }

    void setStyleName(String styleName) {
        mStyleName = styleName;
    }

    void setStyleFileName(String styleFileName) {
        mStyleFileName = styleFileName;
    }

    void setChosenAttributes(List<Attr> attributes) {
        mChosenAttributes = attributes;
    }

    void setRemoveExtracted(boolean removeExtracted) {
        mRemoveExtracted = removeExtracted;
    }

    void setApplyStyle(boolean applyStyle) {
        mApplyStyle = applyStyle;
    }

    void setRemoveAll(boolean removeAll) {
        mRemoveAll = removeAll;
    }

    void setParent(String parent) {
        mParent = parent;
    }

    // ---- Actual implementation of Extract Style modification computation ----

    /**
     * Returns two items: a map from attribute name to a list of attribute nodes of that
     * name, and a subset of these attributes that fall within the text selection
     * (used to drive initial selection in the wizard)
     */
    Pair<Map<String, List<Attr>>, Set<Attr>> getAvailableAttributes() {
        mAvailableAttributes = new TreeMap<String, List<Attr>>();
        Set<Attr> withinSelection = new HashSet<Attr>();
        for (Element element : getElements()) {
            IndexedRegion elementRegion = getRegion(element);
            boolean allIncluded =
                (mOriginalSelectionStart <= elementRegion.getStartOffset() &&
                 mOriginalSelectionEnd >= elementRegion.getEndOffset());

            NamedNodeMap attributeMap = element.getAttributes();
            for (int i = 0, n = attributeMap.getLength(); i < n; i++) {
                Attr attribute = (Attr) attributeMap.item(i);

                String name = attribute.getLocalName();
                if (!isStylableAttribute(name)) {
                    // Don't offer to extract attributes that don't make sense in
                    // styles (like "id" or "style"), or attributes that the user
                    // probably does not want to define in styles (like layout
                    // attributes such as layout_width, or the label of a button etc).
                    // This makes the options offered listed in the wizard simpler.
                    // In special cases where the user *does* want to set one of these
                    // attributes, they can always do it manually so optimize for
                    // the common case here.
                    continue;
                }

                // Skip attributes that are in a namespace other than the Android one
                String namespace = attribute.getNamespaceURI();
                if (namespace != null && !ANDROID_URI.equals(namespace)) {
                    continue;
                }

                if (!allIncluded) {
                    IndexedRegion region = getRegion(attribute);
                    boolean attributeIncluded = mOriginalSelectionStart < region.getEndOffset() &&
                        mOriginalSelectionEnd >= region.getStartOffset();
                    if (attributeIncluded) {
                        withinSelection.add(attribute);
                    }
                } else {
                    withinSelection.add(attribute);
                }

                List<Attr> list = mAvailableAttributes.get(name);
                if (list == null) {
                    list = new ArrayList<Attr>();
                    mAvailableAttributes.put(name, list);
                }
                list.add(attribute);
            }
        }

        return Pair.of(mAvailableAttributes, withinSelection);
    }

    /**
     * Returns whether the given local attribute name is one the style wizard
     * should present as a selectable attribute to be extracted.
     *
     * @param name the attribute name, not including a namespace prefix
     * @return true if the name is one that the user can extract
     */
    public static boolean isStylableAttribute(String name) {
        return !(name == null
                || name.equals(ATTR_ID)
                || name.startsWith(ATTR_STYLE)
                || (name.startsWith(ATTR_LAYOUT_RESOURCE_PREFIX) &&
                        !name.startsWith(ATTR_LAYOUT_MARGIN))
                || name.equals(ATTR_TEXT)
                || name.equals(ATTR_HINT)
                || name.equals(ATTR_SRC)
                || name.equals(ATTR_ON_CLICK));
    }

    IFile getStyleFile(IProject project) {
        return project.getFile(new Path(FD_RESOURCES + WS_SEP + FD_RES_VALUES + WS_SEP
                + mStyleFileName));
    }

    @Override
    protected @NonNull List<Change> computeChanges(IProgressMonitor monitor) {
        List<Change> changes = new ArrayList<Change>();
        if (mChosenAttributes.size() == 0) {
            return changes;
        }

        IFile file = getStyleFile(mDelegate.getEditor().getProject());
        boolean createFile = !file.exists();
        int insertAtIndex;
        String initialIndent = null;
        if (!createFile) {
            Pair<Integer, String> context = computeInsertContext(file);
            insertAtIndex = context.getFirst();
            initialIndent = context.getSecond();
        } else {
            insertAtIndex = 0;
        }

        TextFileChange addFile = new TextFileChange("Create new separate style declaration", file);
        addFile.setTextType(EXT_XML);
        changes.add(addFile);
        String styleString = computeStyleDeclaration(createFile, initialIndent);
        addFile.setEdit(new InsertEdit(insertAtIndex, styleString));

        // Remove extracted attributes?
        MultiTextEdit rootEdit = new MultiTextEdit();
        if (mRemoveExtracted || mRemoveAll) {
            for (Attr attribute : mChosenAttributes) {
                List<Attr> list = mAvailableAttributes.get(attribute.getLocalName());
                for (Attr attr : list) {
                    if (mRemoveAll || attr.getValue().equals(attribute.getValue())) {
                        removeAttribute(rootEdit, attr);
                    }
                }
            }
        }

        // Set the style attribute?
        if (mApplyStyle) {
            for (Element element : getElements()) {
                String value = PREFIX_RESOURCE_REF + REFERENCE_STYLE + mStyleName;
                setAttribute(rootEdit, element, null, null, ATTR_STYLE, value);
            }
        }

        if (rootEdit.hasChildren()) {
            IFile sourceFile = mDelegate.getEditor().getInputFile();
            if (sourceFile == null) {
                return changes;
            }
            TextFileChange change = new TextFileChange(sourceFile.getName(), sourceFile);
            change.setTextType(EXT_XML);
            changes.add(change);

            if (AdtPrefs.getPrefs().getFormatGuiXml()) {
                MultiTextEdit formatted = reformat(rootEdit, XmlFormatStyle.LAYOUT);
                if (formatted != null) {
                    rootEdit = formatted;
                }
            }

            change.setEdit(rootEdit);
        }

        return changes;
    }

    private String computeStyleDeclaration(boolean createFile, String initialIndent) {
        StringBuilder sb = new StringBuilder();
        if (createFile) {
            sb.append(NewXmlFileWizard.XML_HEADER_LINE);
            sb.append('<').append(TAG_RESOURCES).append(' ');
            sb.append(XMLNS_PREFIX).append(ANDROID_NS_NAME).append('=').append('"');
            sb.append(ANDROID_URI);
            sb.append('"').append('>').append('\n');
        }

        // Indent. Use the existing indent found for previous <style> elements in
        // the resource file - but if that indent was 0 (e.g. <style> elements are
        // at the left margin) only use it to indent the style elements and use a real
        // nonzero indent for its children.
        String indent = "    "; //$NON-NLS-1$
        if (initialIndent == null) {
            initialIndent = indent;
        } else if (initialIndent.length() > 0) {
            indent = initialIndent;
        }
        sb.append(initialIndent);
        String styleTag = "style"; //$NON-NLS-1$ // TODO - use constant in parallel changeset
        sb.append('<').append(styleTag).append(' ').append(ATTR_NAME).append('=').append('"');
        sb.append(mStyleName);
        sb.append('"');
        if (mParent != null) {
            sb.append(' ').append(ATTR_PARENT).append('=').append('"');
            sb.append(mParent);
            sb.append('"');
        }
        sb.append('>').append('\n');

        for (Attr attribute : mChosenAttributes) {
            sb.append(initialIndent).append(indent);
            sb.append('<').append(TAG_ITEM).append(' ').append(ATTR_NAME).append('=').append('"');
            // We've already enforced that regardless of prefix, only attributes with
            // an Android namespace can be in the set of chosen attributes. Rewrite the
            // prefix to android here.
            if (attribute.getPrefix() != null) {
                sb.append(ANDROID_NS_NAME_PREFIX);
            }
            sb.append(attribute.getLocalName());
            sb.append('"').append('>');
            sb.append(attribute.getValue());
            sb.append('<').append('/').append(TAG_ITEM).append('>').append('\n');
        }
        sb.append(initialIndent).append('<').append('/').append(styleTag).append('>').append('\n');

        if (createFile) {
            sb.append('<').append('/').append(TAG_RESOURCES).append('>').append('\n');
        }
        String styleString = sb.toString();
        return styleString;
    }

    /** Computes the location in the file to insert the new style element at, as well as
     * the exact indent string to use to indent the {@code <style>} element.
     * @param file the styles.xml file to insert into
     * @return a pair of an insert offset and an indent string
     */
    private Pair<Integer, String> computeInsertContext(final IFile file) {
        int insertAtIndex = -1;
        // Find the insert of the final </resources> item where we will insert
        // the new style elements.
        String indent = null;
        IModelManager modelManager = StructuredModelManager.getModelManager();
        IStructuredModel model = null;
        try {
            model = modelManager.getModelForRead(file);
            if (model instanceof IDOMModel) {
                IDOMModel domModel = (IDOMModel) model;
                IDOMDocument otherDocument = domModel.getDocument();
                Element root = otherDocument.getDocumentElement();
                Node lastChild = root.getLastChild();
                if (lastChild != null) {
                    if (lastChild instanceof IndexedRegion) {
                        IndexedRegion region = (IndexedRegion) lastChild;
                        insertAtIndex = region.getStartOffset() + region.getLength();
                    }

                    // Compute indent
                    while (lastChild != null) {
                        if (lastChild.getNodeType() == Node.ELEMENT_NODE) {
                            IStructuredDocument document = model.getStructuredDocument();
                            indent = AndroidXmlEditor.getIndent(document, lastChild);
                            break;
                        }
                        lastChild = lastChild.getPreviousSibling();
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

        if (insertAtIndex == -1) {
            String contents = AdtPlugin.readFile(file);
            insertAtIndex = contents.indexOf("</" + TAG_RESOURCES + ">"); //$NON-NLS-1$
            if (insertAtIndex == -1) {
                insertAtIndex = contents.length();
            }
        }

        return Pair.of(insertAtIndex, indent);
    }

    @Override
    VisualRefactoringWizard createWizard() {
        return new ExtractStyleWizard(this, mDelegate);
    }

    public static class Descriptor extends VisualRefactoringDescriptor {
        public Descriptor(String project, String description, String comment,
                Map<String, String> arguments) {
            super("org.eclipse.andmore.refactoring.extract.style", //$NON-NLS-1$
                    project, description, comment, arguments);
        }

        @Override
        protected Refactoring createRefactoring(Map<String, String> args) {
            return new ExtractStyleRefactoring(args);
        }
    }

    /**
     * Determines the parent style to be used for this refactoring
     *
     * @return the parent style to be used for this refactoring
     */
    public String getParentStyle() {
        Set<String> styles = new HashSet<String>();
        for (Element element : getElements()) {
            // Includes "" for elements not setting the style
            styles.add(element.getAttribute(ATTR_STYLE));
        }

        if (styles.size() > 1) {
            // The elements differ in what style attributes they are set to
            return null;
        }

        String style = styles.iterator().next();
        if (style != null && style.length() > 0) {
            return style;
        }

        // None of the elements set the style -- see if they have the same widget types
        // and if so offer to extend the theme style for that widget type

        Set<String> types = new HashSet<String>();
        for (Element element : getElements()) {
            types.add(element.getTagName());
        }

        if (types.size() == 1) {
            String view = DescriptorsUtils.getBasename(types.iterator().next());

            ResourceResolver resolver = mDelegate.getGraphicalEditor().getResourceResolver();
            // Look up the theme item name, which for a Button would be "buttonStyle", and so on.
            String n = Character.toLowerCase(view.charAt(0)) + view.substring(1)
                + "Style"; //$NON-NLS-1$
            ResourceValue value = resolver.findItemInTheme(n);
            if (value != null) {
                ResourceValue resolvedValue = resolver.resolveResValue(value);
                String name = resolvedValue.getName();
                if (name != null) {
                    if (resolvedValue.isFramework()) {
                        return PREFIX_ANDROID + name;
                    } else {
                        return name;
                    }
                }
            }
        }

        return null;
    }
}

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

import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ANDROID_WIDGET_PREFIX;
import static com.android.SdkConstants.ATTR_LAYOUT_RESOURCE_PREFIX;
import static com.android.SdkConstants.ATTR_TEXT;
import static com.android.SdkConstants.EXT_XML;
import static com.android.SdkConstants.VIEW_FRAGMENT;
import static com.android.SdkConstants.VIEW_INCLUDE;

import com.android.annotations.NonNull;
import com.android.annotations.VisibleForTesting;

import org.eclipse.andmore.internal.editors.descriptors.AttributeDescriptor;
import org.eclipse.andmore.internal.editors.layout.LayoutEditorDelegate;
import org.eclipse.andmore.internal.editors.layout.descriptors.ViewElementDescriptor;
import org.eclipse.andmore.internal.editors.layout.gle2.CanvasViewInfo;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.xml.core.internal.document.ElementImpl;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Changes the type of the given widgets to the given target type
 * and updates the attributes if necessary
 */
@SuppressWarnings("restriction") // XML model
public class ChangeViewRefactoring extends VisualRefactoring {
    private static final String KEY_TYPE = "type"; //$NON-NLS-1$
    private String mTypeFqcn;

    /**
     * This constructor is solely used by {@link Descriptor},
     * to replay a previous refactoring.
     * @param arguments argument map created by #createArgumentMap.
     */
    ChangeViewRefactoring(Map<String, String> arguments) {
        super(arguments);
        mTypeFqcn = arguments.get(KEY_TYPE);
    }

    public ChangeViewRefactoring(
            IFile file,
            LayoutEditorDelegate delegate,
            ITextSelection selection,
            ITreeSelection treeSelection) {
        super(file, delegate, selection, treeSelection);
    }

    @VisibleForTesting
	public
    ChangeViewRefactoring(List<Element> selectedElements, LayoutEditorDelegate editor) {
        super(selectedElements, editor);
    }

    @Override
    public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException,
            OperationCanceledException {
        RefactoringStatus status = new RefactoringStatus();

        try {
            pm.beginTask("Checking preconditions...", 6);

            if (mSelectionStart == -1 || mSelectionEnd == -1) {
                status.addFatalError("No selection to convert");
                return status;
            }

            // Make sure the selection is contiguous
            if (mTreeSelection != null) {
                List<CanvasViewInfo> infos = getSelectedViewInfos();
                if (!validateNotEmpty(infos, status)) {
                    return status;
                }
            }

            // Ensures that we have a valid DOM model:
            if (mElements.size() == 0) {
                status.addFatalError("Nothing to convert");
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
        args.put(KEY_TYPE, mTypeFqcn);

        return args;
    }

    @Override
    public String getName() {
        return "Change Widget Type";
    }

    public void setType(String typeFqcn) {
        mTypeFqcn = typeFqcn;
    }

    @Override
    @NonNull
	public List<Change> computeChanges(IProgressMonitor monitor) {
        String name = getViewClass(mTypeFqcn);

        IFile file = mDelegate.getEditor().getInputFile();
        List<Change> changes = new ArrayList<Change>();
        if (file == null) {
            return changes;
        }
        TextFileChange change = new TextFileChange(file.getName(), file);
        MultiTextEdit rootEdit = new MultiTextEdit();
        change.setEdit(rootEdit);
        change.setTextType(EXT_XML);
        changes.add(change);

        for (Element element : getElements()) {
            IndexedRegion region = getRegion(element);
            String text = getText(region.getStartOffset(), region.getEndOffset());
            String oldName = element.getNodeName();
            int open = text.indexOf(oldName);
            int close = text.lastIndexOf(oldName);
            if (element instanceof ElementImpl && ((ElementImpl) element).isEmptyTag()) {
                close = -1;
            }

            if (open != -1) {
                int oldLength = oldName.length();
                rootEdit.addChild(new ReplaceEdit(region.getStartOffset() + open,
                        oldLength, name));
            }
            if (close != -1 && close != open) {
                int oldLength = oldName.length();
                rootEdit.addChild(new ReplaceEdit(region.getStartOffset() + close, oldLength,
                        name));
            }

            // Change tag type
            String oldId = getId(element);
            String newId = ensureIdMatchesType(element, mTypeFqcn, rootEdit);
            // Update any layout references to the old id with the new id
            if (oldId != null && newId != null) {
                IStructuredModel model = mDelegate.getEditor().getModelForRead();
                try {
                    IStructuredDocument doc = model.getStructuredDocument();
                    if (doc != null) {
                        IndexedRegion range = getRegion(element);
                        int skipStart = range.getStartOffset();
                        int skipEnd = range.getEndOffset();
                        List<TextEdit> replaceIds = replaceIds(getAndroidNamespacePrefix(), doc,
                                skipStart, skipEnd,
                                oldId, newId);
                        for (TextEdit edit : replaceIds) {
                            rootEdit.addChild(edit);
                        }
                    }
                } finally {
                    model.releaseFromRead();
                }
            }

            // Strip out attributes that no longer make sense
            removeUndefinedAttrs(rootEdit, element);
        }

        return changes;
    }

    /** Removes all the unused attributes after a conversion */
    private void removeUndefinedAttrs(MultiTextEdit rootEdit, Element element) {
        ViewElementDescriptor descriptor = getElementDescriptor(mTypeFqcn);
        if (descriptor == null) {
            return;
        }

        Set<String> defined = new HashSet<String>();
        AttributeDescriptor[] layoutAttributes = descriptor.getAttributes();
        for (AttributeDescriptor attribute : layoutAttributes) {
            defined.add(attribute.getXmlLocalName());
        }

        List<Attr> attributes = findAttributes(element);
        for (Attr attribute : attributes) {
            String name = attribute.getLocalName();
            if (!defined.contains(name)) {
                // Remove it
                removeAttribute(rootEdit, element, attribute.getNamespaceURI(), name);
            }
        }

        // Set text attribute if it's defined
        if (defined.contains(ATTR_TEXT) && !element.hasAttributeNS(ANDROID_URI, ATTR_TEXT)) {
            setAttribute(rootEdit, element, ANDROID_URI, getAndroidNamespacePrefix(),
                    ATTR_TEXT, descriptor.getUiName());
        }
    }

    protected List<Attr> findAttributes(Node root) {
        List<Attr> result = new ArrayList<Attr>();
        NamedNodeMap attributes = root.getAttributes();
        for (int i = 0, n = attributes.getLength(); i < n; i++) {
            Node attributeNode = attributes.item(i);

            String name = attributeNode.getLocalName();
            if (!name.startsWith(ATTR_LAYOUT_RESOURCE_PREFIX)
                    && ANDROID_URI.equals(attributeNode.getNamespaceURI())) {
                result.add((Attr) attributeNode);
            }
        }

        return result;
    }

    List<String> getOldTypes() {
        List<String> types = new ArrayList<String>();
        for (Element primary : getElements()) {
            String oldType = primary.getTagName();
            if (oldType.indexOf('.') == -1
                    && !oldType.equals(VIEW_INCLUDE) && !oldType.equals(VIEW_FRAGMENT)) {
                oldType = ANDROID_WIDGET_PREFIX + oldType;
            }
            types.add(oldType);
        }

        return types;
    }

    @Override
    VisualRefactoringWizard createWizard() {
        return new ChangeViewWizard(this, mDelegate);
    }

    public static class Descriptor extends VisualRefactoringDescriptor {
        public Descriptor(String project, String description, String comment,
                Map<String, String> arguments) {
            super("org.eclipse.andmore.refactoring.changeview", //$NON-NLS-1$
                    project, description, comment, arguments);
        }

        @Override
        protected Refactoring createRefactoring(Map<String, String> args) {
            return new ChangeViewRefactoring(args);
        }
    }
}

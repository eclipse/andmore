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
import static com.android.SdkConstants.ATTR_LAYOUT_HEIGHT;
import static com.android.SdkConstants.ATTR_LAYOUT_WIDTH;
import static com.android.SdkConstants.EXT_XML;

import com.android.annotations.NonNull;
import com.android.annotations.VisibleForTesting;
import com.android.ide.common.xml.XmlFormatStyle;

import org.eclipse.andmore.internal.editors.layout.LayoutEditorDelegate;
import org.eclipse.andmore.internal.editors.layout.gle2.DomUtilities;
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
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Removes the layout surrounding the current selection (or if the current selection has
 * children, removes the current layout), and migrates namespace and layout attributes.
 */
@SuppressWarnings("restriction") // XML model
public class UnwrapRefactoring extends VisualRefactoring {
    private Element mContainer;

    /**
     * This constructor is solely used by {@link Descriptor},
     * to replay a previous refactoring.
     * @param arguments argument map created by #createArgumentMap.
     */
    UnwrapRefactoring(Map<String, String> arguments) {
        super(arguments);
    }

    public UnwrapRefactoring(
            IFile file,
            LayoutEditorDelegate delegate,
            ITextSelection selection,
            ITreeSelection treeSelection) {
        super(file, delegate, selection, treeSelection);
    }

    @VisibleForTesting
    UnwrapRefactoring(List<Element> selectedElements, LayoutEditorDelegate editor) {
        super(selectedElements, editor);
    }

    @Override
    public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException,
            OperationCanceledException {
        RefactoringStatus status = new RefactoringStatus();

        try {
            pm.beginTask("Checking preconditions...", 6);

            if (mSelectionStart == -1 || mSelectionEnd == -1) {
                status.addFatalError("No selection to wrap");
                return status;
            }

            // Make sure that the selection all has the same parent?
            if (mElements.size() == 0) {
                status.addFatalError("Nothing to unwrap");
                return status;
            }

            Element first = mElements.get(0);

            // Determine the element of the container to be removed.
            // If you've selected a non-container, or you've selected multiple
            // elements, then it's the parent which should be removed. Otherwise,
            // it's the selection itself which represents the container.
            boolean useParent = mElements.size() > 1;
            if (!useParent) {
                if (DomUtilities.getChildren(first).size() == 0) {
                    useParent = true;
                }
            }
            Node parent = first.getParentNode();
            if (parent instanceof Document) {
                mContainer = first;
                List<Element> elements = DomUtilities.getChildren(mContainer);
                if (elements.size() == 0) {
                    status.addFatalError(
                            "Cannot remove container when it has no children");
                        return status;
                }
            } else if (useParent && (parent instanceof Element)) {
                mContainer = (Element) parent;
            } else {
                mContainer = first;
            }

            for (Element element : mElements) {
                if (element.getParentNode() != parent) {
                    status.addFatalError(
                            "All unwrapped elements must share the same parent element");
                    return status;
                }
            }

            // Ensure that if we are removing the root, that it has only one child
            // such that there is a new single root
            if (mContainer.getParentNode() instanceof Document) {
                if (DomUtilities.getChildren(mContainer).size() > 1) {
                    status.addFatalError(
                        "Cannot remove root: it has more than one child "
                            + "which would result in multiple new roots");
                    return status;
                }
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
    public String getName() {
        return "Remove Container";
    }

    @Override
    protected @NonNull List<Change> computeChanges(IProgressMonitor monitor) {
        // (1) If the removed parent is the root container, transfer its
        //   namespace declarations
        // (2) Remove the root element completely
        // (3) Transfer layout attributes?
        // (4) Check for Java R.file usages?

        IFile file = mDelegate.getEditor().getInputFile();
        List<Change> changes = new ArrayList<Change>();
        if (file == null) {
            return changes;
        }
        MultiTextEdit rootEdit = new MultiTextEdit();

        // Transfer namespace elements?
        if (mContainer.getParentNode() instanceof Document) {
            List<Element> elements = DomUtilities.getChildren(mContainer);
            assert elements.size() == 1;
            Element newRoot = elements.get(0);

            List<Attr> declarations = findNamespaceAttributes(mContainer);
            for (Attr attribute : declarations) {
                if (attribute instanceof IndexedRegion) {
                    setAttribute(rootEdit, newRoot, attribute.getNamespaceURI(),
                            attribute.getPrefix(), attribute.getLocalName(), attribute.getValue());
                }
            }
        }

        // Transfer layout_ attributes (other than width and height)
         List<Element> children = DomUtilities.getChildren(mContainer);
         if (children.size() == 1) {
            List<Attr> layoutAttributes = findLayoutAttributes(mContainer);
            for (Attr attribute : layoutAttributes) {
                String name = attribute.getLocalName();
                if ((name.equals(ATTR_LAYOUT_WIDTH) || name.equals(ATTR_LAYOUT_HEIGHT))
                        && ANDROID_URI.equals(attribute.getNamespaceURI())) {
                    // Already handled specially
                    continue;
                }
            }
        }

         // Remove the root
         removeElementTags(rootEdit, mContainer, Collections.<Element>emptyList() /* skip */,
                 false /*changeIndentation*/);

         MultiTextEdit formatted = reformat(rootEdit, XmlFormatStyle.LAYOUT);
         if (formatted != null) {
             rootEdit = formatted;
         }

         TextFileChange change = new TextFileChange(file.getName(), file);
         change.setEdit(rootEdit);
         change.setTextType(EXT_XML);
         changes.add(change);
         return changes;
    }

    @Override
    public VisualRefactoringWizard createWizard() {
        return new UnwrapWizard(this, mDelegate);
    }

    public static class Descriptor extends VisualRefactoringDescriptor {
        public Descriptor(String project, String description, String comment,
                Map<String, String> arguments) {
            super("org.eclipse.andmore.refactoring.unwrap", //$NON-NLS-1$
                    project, description, comment, arguments);
        }

        @Override
        protected Refactoring createRefactoring(Map<String, String> args) {
            return new UnwrapRefactoring(args);
        }
    }
}

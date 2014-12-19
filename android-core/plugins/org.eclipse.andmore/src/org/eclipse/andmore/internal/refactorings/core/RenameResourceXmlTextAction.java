/*
 * Copyright (C) 2012 The Android Open Source Project
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

package org.eclipse.andmore.internal.refactorings.core;

import static com.android.SdkConstants.ANDROID_MANIFEST_XML;
import static com.android.SdkConstants.ANDROID_PREFIX;
import static com.android.SdkConstants.ANDROID_THEME_PREFIX;
import static com.android.SdkConstants.ATTR_NAME;
import static com.android.SdkConstants.ATTR_TYPE;
import static com.android.SdkConstants.TAG_ITEM;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.resources.ResourceUrl;
import com.android.resources.ResourceType;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.editors.layout.gle2.DomUtilities;
import org.eclipse.andmore.internal.editors.manifest.ManifestInfo;
import org.eclipse.andmore.internal.project.BaseProjectHelper;
import org.eclipse.andmore.internal.sdk.ProjectState;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameTypeProcessor;
import org.eclipse.jdt.internal.ui.refactoring.reorg.RenameTypeWizard;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorExtension;
import org.eclipse.ui.texteditor.ITextEditorExtension2;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.List;

/**
 * Text action for XML files to invoke renaming
 * <p>
 * TODO: Handle other types of renaming: invoking class renaming when editing
 * class names in layout files and manifest files, renaming attribute names when
 * editing a styleable attribute, etc.
 */
@SuppressWarnings("restriction") // Java rename refactoring
public final class RenameResourceXmlTextAction extends Action {
    private final ITextEditor mEditor;

    /**
     * Creates a new {@linkplain RenameResourceXmlTextAction}
     *
     * @param editor the associated editor
     */
    public RenameResourceXmlTextAction(@NonNull ITextEditor editor) {
        super("Rename");
        mEditor = editor;
    }

    @Override
    public void run() {
        if (!validateEditorInputState()) {
            return;
        }
        IFile file = getFile();
        if (file == null) {
            return;
        }
        IProject project = file.getProject();
        if (project == null) {
            return;
        }
        IDocument document = getDocument();
        if (document == null) {
            return;
        }
        ITextSelection selection = getSelection();
        if (selection == null) {
            return;
        }

        ResourceUrl resource = findResource(document, selection.getOffset());

        if (resource == null) {
            resource = findItemDefinition(document, selection.getOffset());
        }

        if (resource != null) {
            ResourceType type = resource.type;
            String name = resource.name;
            Shell shell = mEditor.getSite().getShell();
            boolean canClear = false;

            RenameResourceWizard.renameResource(shell, project, type, name, null, canClear);
            return;
        }

        String className = findClassName(document, file, selection.getOffset());
        if (className != null) {
            assert className.equals(className.trim());
            IType type = findType(className, project);
            if (type != null) {
                RenameTypeProcessor processor = new RenameTypeProcessor(type);
                //processor.setNewElementName(className);
                processor.setUpdateQualifiedNames(true);
                processor.setUpdateSimilarDeclarations(false);
                //processor.setMatchStrategy(?);
                //processor.setFilePatterns(patterns);
                processor.setUpdateReferences(true);

                RenameRefactoring refactoring = new RenameRefactoring(processor);
                RenameTypeWizard wizard = new RenameTypeWizard(refactoring);
                RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation(wizard);
                try {
                    IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                    op.run(window.getShell(), wizard.getDefaultPageTitle());
                } catch (InterruptedException e) {
                }
            }

            return;
        }

        // Fallback: tell user the cursor isn't in the right place
        MessageDialog.openInformation(mEditor.getSite().getShell(),
                "Rename",
                "Operation unavailable on the current selection.\n"
                        + "Select an Android resource name or class.");
    }

    private boolean validateEditorInputState() {
        if (mEditor instanceof ITextEditorExtension2)
            return ((ITextEditorExtension2) mEditor).validateEditorInputState();
        else if (mEditor instanceof ITextEditorExtension)
            return !((ITextEditorExtension) mEditor).isEditorInputReadOnly();
        else if (mEditor != null)
            return mEditor.isEditable();
        else
            return false;
    }

    /**
     * Searches for a resource URL around the caret, such as {@code @string/foo}
     *
     * @param document the document to search in
     * @param offset the offset to search at
     * @return a resource pair, or null if not found
     */
    @Nullable
    public static ResourceUrl findResource(@NonNull IDocument document, int offset) {
        try {
            int max = document.getLength();
            if (offset >= max) {
                offset = max - 1;
            } else if (offset < 0) {
                offset = 0;
            } else if (offset > 0) {
                // If the caret is right after a resource name (meaning getChar(offset) points
                // to the following character), back up
                char c = document.getChar(offset);
                if (!isValidResourceNameChar(c)) {
                    offset--;
                }
            }

            int start = offset;
            boolean valid = true;
            for (; start >= 0; start--) {
                char c = document.getChar(start);
                if (c == '@' || c == '?') {
                    break;
                } else if (!isValidResourceNameChar(c)) {
                    valid = false;
                    break;
                }
            }
            if (valid) {
                // Search forwards for the end
                int end = start + 1;
                for (; end < max; end++) {
                    char c = document.getChar(end);
                    if (!isValidResourceNameChar(c)) {
                        break;
                    }
                }
                if (end > start + 1) {
                    String url = document.get(start, end - start);

                    // Don't allow renaming framework resources -- @android:string/ok etc
                    if (url.startsWith(ANDROID_PREFIX) || url.startsWith(ANDROID_THEME_PREFIX)) {
                        return null;
                    }

                    return ResourceUrl.parse(url);
                }
            }
        } catch (BadLocationException e) {
            AdtPlugin.log(e, null);
        }

        return null;
    }

    private static boolean isValidResourceNameChar(char c) {
        return c == '@' || c == '?' || c == '/' || c == '+' || Character.isJavaIdentifierPart(c);
    }

    /**
     * Searches for an item definition around the caret, such as
     * {@code   <string name="foo">My String</string>}
     */
    private ResourceUrl findItemDefinition(IDocument document, int offset) {
        Node node = DomUtilities.getNode(document, offset);
        if (node == null) {
            return null;
        }
        if (node.getNodeType() == Node.TEXT_NODE) {
            node = node.getParentNode();
        }
        if (node == null || node.getNodeType() != Node.ELEMENT_NODE) {
            return null;
        }

        Element element = (Element) node;
        String name = element.getAttribute(ATTR_NAME);
        if (name == null || name.isEmpty()) {
            return null;
        }
        String typeString = element.getTagName();
        if (TAG_ITEM.equals(typeString)) {
            typeString = element.getAttribute(ATTR_TYPE);
            if (typeString == null || typeString.isEmpty()) {
                return null;
            }
        }
        ResourceType type = ResourceType.getEnum(typeString);
        if (type != null) {
            return ResourceUrl.create(type, name, false, false);
        }

        return null;
    }

    /**
     * Searches for a fully qualified class name around the caret, such as {@code foo.bar.MyClass}
     *
     * @param document the document to search in
     * @param file the file, if known
     * @param offset the offset to search at
     * @return a resource pair, or null if not found
     */
    @Nullable
    public static String findClassName(
            @NonNull IDocument document,
            @Nullable IFile file,
            int offset) {
        try {
            int max = document.getLength();
            if (offset >= max) {
                offset = max - 1;
            } else if (offset < 0) {
                offset = 0;
            } else if (offset > 0) {
                // If the caret is right after a resource name (meaning getChar(offset) points
                // to the following character), back up
                char c = document.getChar(offset);
                if (Character.isJavaIdentifierPart(c)) {
                    offset--;
                }
            }

            int start = offset;
            for (; start >= 0; start--) {
                char c = document.getChar(start);
                if (c == '"' || c == '<' || c == '/') {
                    start++;
                    break;
                } else if (c != '.' && !Character.isJavaIdentifierPart(c)) {
                    return null;
                }
            }
            // Search forwards for the end
            int end = start + 1;
            for (; end < max; end++) {
                char c = document.getChar(end);
                if (c != '.' && !Character.isJavaIdentifierPart(c)) {
                    if (c != '"' && c != '>' && !Character.isWhitespace(c)) {
                        return null;
                    }
                    break;
                }
            }
            if (end > start + 1) {
                String fqcn = document.get(start, end - start);
                int dot = fqcn.indexOf('.');
                if (dot == -1) { // Only support fully qualified names
                    return null;
                }
                if (dot == 0) { // Special case for manifests: prepend package
                    if (file != null && file.getName().equals(ANDROID_MANIFEST_XML)) {
                        ManifestInfo info = ManifestInfo.get(file.getProject());
                        return info.getPackage() + fqcn;
                    }
                    return null;
                }

                return fqcn;
            }
        } catch (BadLocationException e) {
            AdtPlugin.log(e, null);
        }

        return null;
    }

    @Nullable
    private IType findType(@NonNull String className, @NonNull IProject project) {
        IType type = null;
        try {
            IJavaProject javaProject = BaseProjectHelper.getJavaProject(project);
            type = javaProject.findType(className);
            if (type == null || !type.exists()) {
                return null;
            }
            if (!type.isBinary()) {
                return type;
            }
            // See if this class is coming through a library project jar file and
            // if so locate the real class
            ProjectState projectState = Sdk.getProjectState(project);
            if (projectState != null) {
                List<IProject> libraries = projectState.getFullLibraryProjects();
                for (IProject library : libraries) {
                    javaProject = BaseProjectHelper.getJavaProject(library);
                    type = javaProject.findType(className);
                    if (type != null && type.exists() && !type.isBinary()) {
                        return type;
                    }
                }
            }
        } catch (CoreException e) {
            AdtPlugin.log(e, null);
        }

        return null;
    }

    private ITextSelection getSelection() {
        ISelectionProvider selectionProvider = mEditor.getSelectionProvider();
        if (selectionProvider == null) {
            return null;
        }
        ISelection selection = selectionProvider.getSelection();
        if (!(selection instanceof ITextSelection)) {
            return null;
        }
        return (ITextSelection) selection;
    }

    private IDocument getDocument() {
        IDocumentProvider documentProvider = mEditor.getDocumentProvider();
        if (documentProvider == null) {
            return null;
        }
        IDocument document = documentProvider.getDocument(mEditor.getEditorInput());
        if (document == null) {
            return null;
        }
        return document;
    }

    @Nullable
    private IFile getFile() {
        IEditorInput input = mEditor.getEditorInput();
        if (input instanceof IFileEditorInput) {
            IFileEditorInput fileInput = (IFileEditorInput) input;
            return fileInput.getFile();
        }

        return null;
    }
}

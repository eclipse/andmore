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

package org.eclipse.andmore.internal.lint;

import static com.android.SdkConstants.FQCN_SUPPRESS_LINT;
import static com.android.SdkConstants.FQCN_TARGET_API;
import static com.android.SdkConstants.SUPPRESS_LINT;
import static com.android.SdkConstants.TARGET_API;
import static org.eclipse.jdt.core.dom.ArrayInitializer.EXPRESSIONS_PROPERTY;
import static org.eclipse.jdt.core.dom.SingleMemberAnnotation.VALUE_PROPERTY;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.sdklib.SdkVersionInfo;
import com.android.tools.lint.checks.AnnotationDetector;
import com.android.tools.lint.checks.ApiDetector;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.Scope;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.AdtUtils;
import org.eclipse.andmore.internal.editors.IconFactory;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.ui.IWorkingCopyManager;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.SharedASTProvider;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.graphics.Image;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolution2;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Marker resolution for adding {@code @SuppressLint} annotations in Java files.
 * It can also add {@code @TargetApi} annotations.
 */
class AddSuppressAnnotation implements IMarkerResolution2 {
    private final IMarker mMarker;
    private final String mId;
    private final BodyDeclaration mNode;
    private final String mDescription;
    /**
     * Should it create a {@code @TargetApi} annotation instead of
     * {@code SuppressLint} ? If so pass a non null API level
     */
    private final String mTargetApi;

    private AddSuppressAnnotation(
            @NonNull String id,
            @NonNull IMarker marker,
            @NonNull BodyDeclaration node,
            @NonNull String description,
            @Nullable String targetApi) {
        mId = id;
        mMarker = marker;
        mNode = node;
        mDescription = description;
        mTargetApi = targetApi;
    }

    @Override
    public String getLabel() {
        return mDescription;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public Image getImage() {
        return IconFactory.getInstance().getIcon("newannotation"); //$NON-NLS-1$
    }

    @Override
    public void run(IMarker marker) {
        ITextEditor textEditor = AdtUtils.getActiveTextEditor();
        IDocumentProvider provider = textEditor.getDocumentProvider();
        IEditorInput editorInput = textEditor.getEditorInput();
        IDocument document = provider.getDocument(editorInput);
        if (document == null) {
            return;
        }
        IWorkingCopyManager manager = JavaUI.getWorkingCopyManager();
        ICompilationUnit compilationUnit = manager.getWorkingCopy(editorInput);
        try {
            MultiTextEdit edit;
            if (mTargetApi == null) {
                edit = addSuppressAnnotation(document, compilationUnit, mNode);
            } else {
                edit = addTargetApiAnnotation(document, compilationUnit, mNode);
            }
            if (edit != null) {
                edit.apply(document);

                // Remove the marker now that the suppress annotation has been added
                // (so the user doesn't have to re-run lint just to see it disappear,
                // and besides we don't want to keep offering marker resolutions on this
                // marker which could lead to duplicate annotations since the above code
                // assumes that the current id isn't in the list of values, since otherwise
                // lint shouldn't have complained here.
                mMarker.delete();
            }
        } catch (Exception ex) {
            AdtPlugin.log(ex, "Could not add suppress annotation");
        }
    }

    @SuppressWarnings({"rawtypes"}) // Java AST API has raw types
    private MultiTextEdit addSuppressAnnotation(
            IDocument document,
            ICompilationUnit compilationUnit,
            BodyDeclaration declaration) throws CoreException {
        List modifiers = declaration.modifiers();
        SingleMemberAnnotation existing = null;
        for (Object o : modifiers) {
            if (o instanceof SingleMemberAnnotation) {
                SingleMemberAnnotation annotation = (SingleMemberAnnotation) o;
                String type = annotation.getTypeName().getFullyQualifiedName();
                if (type.equals(FQCN_SUPPRESS_LINT) || type.endsWith(SUPPRESS_LINT)) {
                    existing = annotation;
                    break;
                }
            }
        }

        ImportRewrite importRewrite = ImportRewrite.create(compilationUnit, true);
        String local = importRewrite.addImport(FQCN_SUPPRESS_LINT);
        AST ast = declaration.getAST();
        ASTRewrite rewriter = ASTRewrite.create(ast);
        if (existing == null) {
            SingleMemberAnnotation newAnnotation = ast.newSingleMemberAnnotation();
            newAnnotation.setTypeName(ast.newSimpleName(local));
            StringLiteral value = ast.newStringLiteral();
            value.setLiteralValue(mId);
            newAnnotation.setValue(value);
            ListRewrite listRewrite = rewriter.getListRewrite(declaration,
                    declaration.getModifiersProperty());
            listRewrite.insertFirst(newAnnotation, null);
        } else {
            Expression existingValue = existing.getValue();
            if (existingValue instanceof StringLiteral) {
                StringLiteral stringLiteral = (StringLiteral) existingValue;
                if (mId.equals(stringLiteral.getLiteralValue())) {
                    // Already contains the id
                    return null;
                }
                // Create a new array initializer holding the old string plus the new id
                ArrayInitializer array = ast.newArrayInitializer();
                StringLiteral old = ast.newStringLiteral();
                old.setLiteralValue(stringLiteral.getLiteralValue());
                array.expressions().add(old);
                StringLiteral value = ast.newStringLiteral();
                value.setLiteralValue(mId);
                array.expressions().add(value);
                rewriter.set(existing, VALUE_PROPERTY, array, null);
            } else if (existingValue instanceof ArrayInitializer) {
                // Existing array: just append the new string
                ArrayInitializer array = (ArrayInitializer) existingValue;
                List expressions = array.expressions();
                if (expressions != null) {
                    for (Object o : expressions) {
                        if (o instanceof StringLiteral) {
                            if (mId.equals(((StringLiteral)o).getLiteralValue())) {
                                // Already contains the id
                                return null;
                            }
                        }
                    }
                }
                StringLiteral value = ast.newStringLiteral();
                value.setLiteralValue(mId);
                ListRewrite listRewrite = rewriter.getListRewrite(array, EXPRESSIONS_PROPERTY);
                listRewrite.insertLast(value, null);
            } else {
                assert false : existingValue;
                return null;
            }
        }

        TextEdit importEdits = importRewrite.rewriteImports(new NullProgressMonitor());
        TextEdit annotationEdits = rewriter.rewriteAST(document, null);

        // Apply to the document
        MultiTextEdit edit = new MultiTextEdit();
        // Create the edit to change the imports, only if
        // anything changed
        if (importEdits.hasChildren()) {
            edit.addChild(importEdits);
        }
        edit.addChild(annotationEdits);

        return edit;
    }

    @SuppressWarnings({"rawtypes"}) // Java AST API has raw types
    private MultiTextEdit addTargetApiAnnotation(
            IDocument document,
            ICompilationUnit compilationUnit,
            BodyDeclaration declaration) throws CoreException {
        List modifiers = declaration.modifiers();
        SingleMemberAnnotation existing = null;
        for (Object o : modifiers) {
            if (o instanceof SingleMemberAnnotation) {
                SingleMemberAnnotation annotation = (SingleMemberAnnotation) o;
                String type = annotation.getTypeName().getFullyQualifiedName();
                if (type.equals(FQCN_TARGET_API) || type.endsWith(TARGET_API)) {
                    existing = annotation;
                    break;
                }
            }
        }

        ImportRewrite importRewrite = ImportRewrite.create(compilationUnit, true);
        importRewrite.addImport("android.os.Build"); //$NON-NLS-1$
        String local = importRewrite.addImport(FQCN_TARGET_API);
        AST ast = declaration.getAST();
        ASTRewrite rewriter = ASTRewrite.create(ast);
        if (existing == null) {
            SingleMemberAnnotation newAnnotation = ast.newSingleMemberAnnotation();
            newAnnotation.setTypeName(ast.newSimpleName(local));
            Expression value = createLiteral(ast);
            newAnnotation.setValue(value);
            ListRewrite listRewrite = rewriter.getListRewrite(declaration,
                    declaration.getModifiersProperty());
            listRewrite.insertFirst(newAnnotation, null);
        } else {
            Expression value = createLiteral(ast);
            rewriter.set(existing, VALUE_PROPERTY, value, null);
        }

        TextEdit importEdits = importRewrite.rewriteImports(new NullProgressMonitor());
        TextEdit annotationEdits = rewriter.rewriteAST(document, null);
        MultiTextEdit edit = new MultiTextEdit();
        if (importEdits.hasChildren()) {
            edit.addChild(importEdits);
        }
        edit.addChild(annotationEdits);

        return edit;
    }

    private Expression createLiteral(AST ast) {
        Expression value;
        if (!isCodeName()) {
            value = ast.newQualifiedName(
                    ast.newQualifiedName(ast.newSimpleName("Build"), //$NON-NLS-1$
                                ast.newSimpleName("VERSION_CODES")), //$NON-NLS-1$
                    ast.newSimpleName(mTargetApi));
        } else {
            value = ast.newNumberLiteral(mTargetApi);
        }
        return value;
    }

    private boolean isCodeName() {
        return Character.isDigit(mTargetApi.charAt(0));
    }

    /**
     * Adds any applicable suppress lint fix resolutions into the given list
     *
     * @param marker the marker to create fixes for
     * @param id the issue id
     * @param resolutions a list to add the created resolutions into, if any
     */
    public static void createFixes(IMarker marker, String id,
            List<IMarkerResolution> resolutions) {
        ITextEditor textEditor = AdtUtils.getActiveTextEditor();
        IDocumentProvider provider = textEditor.getDocumentProvider();
        IEditorInput editorInput = textEditor.getEditorInput();
        IDocument document = provider.getDocument(editorInput);
        if (document == null) {
            return;
        }

        IWorkingCopyManager manager = JavaUI.getWorkingCopyManager();
        ICompilationUnit compilationUnit = manager.getWorkingCopy(editorInput);
        int offset = 0;
        int length = 0;
        int start = marker.getAttribute(IMarker.CHAR_START, -1);
        int end = marker.getAttribute(IMarker.CHAR_END, -1);
        offset = start;
        length = end - start;
        CompilationUnit root = SharedASTProvider.getAST(compilationUnit,
                SharedASTProvider.WAIT_YES, null);
        if (root == null) {
            return;
        }

        int api = -1;
        if (id.equals(ApiDetector.UNSUPPORTED.getId()) ||
                id.equals(ApiDetector.INLINED.getId())) {
            String message = marker.getAttribute(IMarker.MESSAGE, null);
            if (message != null) {
                Pattern pattern = Pattern.compile("\\s(\\d+)\\s"); //$NON-NLS-1$
                Matcher matcher = pattern.matcher(message);
                if (matcher.find()) {
                    api = Integer.parseInt(matcher.group(1));
                }
            }
        }

        Issue issue = EclipseLintClient.getRegistry().getIssue(id);
        boolean isClassDetector = issue != null && issue.getImplementation().getScope().contains(
                Scope.CLASS_FILE);

        // Don't offer to suppress (with an annotation) the annotation checks
        if (issue == AnnotationDetector.ISSUE) {
            return;
        }

        NodeFinder nodeFinder = new NodeFinder(root, offset, length);
        ASTNode coveringNode;
        if (offset <= 0) {
            // Error added on the first line of a Java class: typically from a class-based
            // detector which lacks line information. Map this to the top level class
            // in the file instead.
            coveringNode = root;
            if (root.types() != null && root.types().size() > 0) {
                Object type = root.types().get(0);
                if (type instanceof ASTNode) {
                    coveringNode = (ASTNode) type;
                }
            }
        } else {
            coveringNode = nodeFinder.getCoveringNode();
        }
        for (ASTNode body = coveringNode; body != null; body = body.getParent()) {
            if (body instanceof BodyDeclaration) {
                BodyDeclaration declaration = (BodyDeclaration) body;

                String target = null;
                if (body instanceof MethodDeclaration) {
                    target = ((MethodDeclaration) body).getName().toString() + "()"; //$NON-NLS-1$
                } else if (body instanceof FieldDeclaration) {
                    target = "field";
                    FieldDeclaration field = (FieldDeclaration) body;
                    if (field.fragments() != null && field.fragments().size() > 0) {
                        ASTNode first = (ASTNode) field.fragments().get(0);
                        if (first instanceof VariableDeclarationFragment) {
                            VariableDeclarationFragment decl = (VariableDeclarationFragment) first;
                            target = decl.getName().toString();
                        }
                    }
                } else if (body instanceof AnonymousClassDeclaration) {
                    target = "anonymous class";
                } else if (body instanceof TypeDeclaration) {
                    target = ((TypeDeclaration) body).getName().toString();
                } else {
                    target = body.getClass().getSimpleName();
                }

                // In class files, detectors can only find annotations on methods
                // and on classes, not on variable declarations
                if (isClassDetector && !(body instanceof MethodDeclaration
                            || body instanceof TypeDeclaration
                            || body instanceof AnonymousClassDeclaration
                            || body instanceof FieldDeclaration)) {
                    continue;
                }

                String desc = String.format("Add @SuppressLint '%1$s\' to '%2$s'", id, target);
                resolutions.add(new AddSuppressAnnotation(id, marker, declaration, desc, null));

                if (api != -1
                        // @TargetApi is only valid on methods and classes, not fields etc
                        && (body instanceof MethodDeclaration
                                || body instanceof TypeDeclaration)) {
                    String apiString = SdkVersionInfo.getBuildCode(api);
                    if (apiString == null) {
                        apiString = Integer.toString(api);
                    }
                    desc = String.format("Add @TargetApi(%1$s) to '%2$s'", apiString, target);
                    resolutions.add(new AddSuppressAnnotation(id, marker, declaration, desc,
                            apiString));
                }
            }
        }
    }
}

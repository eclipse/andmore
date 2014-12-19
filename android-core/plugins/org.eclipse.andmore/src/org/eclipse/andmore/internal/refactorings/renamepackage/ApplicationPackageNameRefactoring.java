/*
 * Copyright (C) 2010 The Android Open Source Project
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

package org.eclipse.andmore.internal.refactorings.renamepackage;

import static com.android.SdkConstants.FN_BUILD_CONFIG_BASE;
import static com.android.SdkConstants.FN_MANIFEST_BASE;
import static com.android.SdkConstants.FN_RESOURCE_BASE;

import com.android.SdkConstants;
import com.android.xml.AndroidManifest;

import org.eclipse.andmore.AdtConstants;
import org.eclipse.andmore.AdtPlugin;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextEditChangeGroup;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IModelManager;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegionList;
import org.eclipse.wst.xml.core.internal.regions.DOMRegionContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 *  Wrapper class defining the stages of the refactoring process
 */
@SuppressWarnings("restriction")
class ApplicationPackageNameRefactoring extends Refactoring {
    private final IProject mProject;
    private final Name mOldPackageName;
    private final Name mNewPackageName;

    List<String> MAIN_COMPONENT_TYPES_LIST = Arrays.asList(MAIN_COMPONENT_TYPES);

    ApplicationPackageNameRefactoring(
            IProject project,
            Name oldPackageName,
            Name newPackageName) {
        mProject = project;
        mOldPackageName = oldPackageName;
        mNewPackageName = newPackageName;
    }

    @Override
    public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
            throws CoreException, OperationCanceledException {

        // Accurate refactoring of the "shorthand" names in
        // AndroidManifest.xml depends on not having compilation errors.
        if (mProject.findMaxProblemSeverity(
                IMarker.PROBLEM,
                true,
                IResource.DEPTH_INFINITE) == IMarker.SEVERITY_ERROR) {
            return
                RefactoringStatus.createFatalErrorStatus("Fix the errors in your project, first.");
        }

        return new RefactoringStatus();
    }

    @Override
    public RefactoringStatus checkFinalConditions(IProgressMonitor pm)
            throws OperationCanceledException {

        return new RefactoringStatus();
    }

    @Override
    public Change createChange(IProgressMonitor pm) throws CoreException,
    OperationCanceledException {

        // Traverse all files in the project, building up a list of changes
        JavaFileVisitor fileVisitor = new JavaFileVisitor();
        mProject.accept(fileVisitor);
        return fileVisitor.getChange();
    }

    @Override
    public String getName() {
        return "AndroidPackageNameRefactoring"; //$NON-NLS-1$
    }

    public final static String[] MAIN_COMPONENT_TYPES = {
        AndroidManifest.NODE_ACTIVITY, AndroidManifest.NODE_SERVICE,
        AndroidManifest.NODE_RECEIVER, AndroidManifest.NODE_PROVIDER,
        AndroidManifest.NODE_APPLICATION
    };


    TextEdit updateJavaFileImports(CompilationUnit cu) {

        ImportVisitor importVisitor = new ImportVisitor(cu.getAST());
        cu.accept(importVisitor);
        TextEdit rewrittenImports = importVisitor.getTextEdit();

        // If the import of R was potentially implicit, insert an import statement
        if (rewrittenImports != null && cu.getPackage().getName().getFullyQualifiedName()
                .equals(mOldPackageName.getFullyQualifiedName())) {

            UsageVisitor usageVisitor = new UsageVisitor();
            cu.accept(usageVisitor);

            if (usageVisitor.seenAny()) {
                ImportRewrite irw = ImportRewrite.create(cu, true);
                if (usageVisitor.hasSeenR()) {
                    irw.addImport(mNewPackageName.getFullyQualifiedName() + '.'
                            + FN_RESOURCE_BASE);
                }
                if (usageVisitor.hasSeenBuildConfig()) {
                    irw.addImport(mNewPackageName.getFullyQualifiedName() + '.'
                            + FN_BUILD_CONFIG_BASE);
                }
                if (usageVisitor.hasSeenManifest()) {
                    irw.addImport(mNewPackageName.getFullyQualifiedName() + '.'
                            + FN_MANIFEST_BASE);
                }

                try {
                    rewrittenImports.addChild( irw.rewriteImports(null) );
                } catch (MalformedTreeException e) {
                    Status s = new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID, e.getMessage(), e);
                    AdtPlugin.getDefault().getLog().log(s);
                } catch (CoreException e) {
                    Status s = new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID, e.getMessage(), e);
                    AdtPlugin.getDefault().getLog().log(s);
                }
            }
        }

        return rewrittenImports;
    }

    // XML utility functions
    private String stripQuotes(String text) {
        int len = text.length();
        if (len >= 2 && text.charAt(0) == '"' && text.charAt(len - 1) == '"') {
            return text.substring(1, len - 1);
        } else if (len >= 2 && text.charAt(0) == '\'' && text.charAt(len - 1) == '\'') {
            return text.substring(1, len - 1);
        }
        return text;
    }

    private String addQuotes(String text) {
        return '"' + text + '"';
    }

    /*
     * Make the appropriate package name changes to a resource file,
     * e.g. .xml files in res/layout. This entails updating the namespace
     * declarations for custom styleable attributes.  The namespace prefix
     * is user-defined and may be declared in any element where or parent
     * element of where the prefix is used.
     */
    TextFileChange editXmlResourceFile(IFile file) {

        IModelManager modelManager = StructuredModelManager.getModelManager();
        IStructuredDocument sdoc = null;
        try {
            sdoc = modelManager.createStructuredDocumentFor(file);
        } catch (IOException e) {
            Status s = new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID, e.getMessage(), e);
            AdtPlugin.getDefault().getLog().log(s);
        } catch (CoreException e) {
            Status s = new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID, e.getMessage(), e);
            AdtPlugin.getDefault().getLog().log(s);
        }

        if (sdoc == null) {
            return null;
        }

        TextFileChange xmlChange = new TextFileChange("XML resource file edit", file);
        xmlChange.setTextType(SdkConstants.EXT_XML);

        MultiTextEdit multiEdit = new MultiTextEdit();
        ArrayList<TextEditGroup> editGroups = new ArrayList<TextEditGroup>();

        final String oldAppNamespaceString = String.format(AdtConstants.NS_CUSTOM_RESOURCES,
                mOldPackageName.getFullyQualifiedName());
        final String newAppNamespaceString = String.format(AdtConstants.NS_CUSTOM_RESOURCES,
                mNewPackageName.getFullyQualifiedName());

        // Prepare the change set
        for (IStructuredDocumentRegion region : sdoc.getStructuredDocumentRegions()) {

            if (!DOMRegionContext.XML_TAG_NAME.equals(region.getType())) {
                continue;
            }

            int nb = region.getNumberOfRegions();
            ITextRegionList list = region.getRegions();
            String lastAttrName = null;

            for (int i = 0; i < nb; i++) {
                ITextRegion subRegion = list.get(i);
                String type = subRegion.getType();

                if (DOMRegionContext.XML_TAG_ATTRIBUTE_NAME.equals(type)) {
                    // Memorize the last attribute name seen
                    lastAttrName = region.getText(subRegion);

                } else if (DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE.equals(type)) {
                    // Check this is the attribute and the original string

                    if (lastAttrName != null &&
                            lastAttrName.startsWith(SdkConstants.XMLNS_PREFIX)) {

                        String lastAttrValue = region.getText(subRegion);
                        if (oldAppNamespaceString.equals(stripQuotes(lastAttrValue))) {

                            // Found an occurrence. Create a change for it.
                            TextEdit edit = new ReplaceEdit(
                                    region.getStartOffset() + subRegion.getStart(),
                                    subRegion.getTextLength(),
                                    addQuotes(newAppNamespaceString));
                            TextEditGroup editGroup = new TextEditGroup(
                                    "Replace package name in custom namespace prefix", edit);

                            multiEdit.addChild(edit);
                            editGroups.add(editGroup);
                        }
                    }
                }
            }
        }

        if (multiEdit.hasChildren()) {
            xmlChange.setEdit(multiEdit);
            for (TextEditGroup group : editGroups) {
                xmlChange.addTextEditChangeGroup(new TextEditChangeGroup(xmlChange, group));
            }

            return xmlChange;
        }
        return null;
    }

    /*
     * Replace all instances of the package name in AndroidManifest.xml.
     * This includes expanding shorthand paths for each Component (Activity,
     * Service, etc.) and of course updating the application package name.
     * The namespace prefix might not be "android", so we resolve it
     * dynamically.
     */
    TextFileChange editAndroidManifest(IFile file) {

        IModelManager modelManager = StructuredModelManager.getModelManager();
        IStructuredDocument sdoc = null;
        try {
            sdoc = modelManager.createStructuredDocumentFor(file);
        } catch (IOException e) {
            Status s = new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID, e.getMessage(), e);
            AdtPlugin.getDefault().getLog().log(s);
        } catch (CoreException e) {
            Status s = new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID, e.getMessage(), e);
            AdtPlugin.getDefault().getLog().log(s);
        }

        if (sdoc == null) {
            return null;
        }

        TextFileChange xmlChange = new TextFileChange("Make Manifest edits", file);
        xmlChange.setTextType(SdkConstants.EXT_XML);

        MultiTextEdit multiEdit = new MultiTextEdit();
        ArrayList<TextEditGroup> editGroups = new ArrayList<TextEditGroup>();

        // The namespace prefix is guaranteed to be resolved before
        // the first use of this attribute
        String android_name_attribute = null;

        // Prepare the change set
        for (IStructuredDocumentRegion region : sdoc.getStructuredDocumentRegions()) {

            // Only look at XML "top regions"
            if (!DOMRegionContext.XML_TAG_NAME.equals(region.getType())) {
                continue;
            }

            int nb = region.getNumberOfRegions();
            ITextRegionList list = region.getRegions();
            String lastTagName = null, lastAttrName = null;

            for (int i = 0; i < nb; i++) {
                ITextRegion subRegion = list.get(i);
                String type = subRegion.getType();

                if (DOMRegionContext.XML_TAG_NAME.equals(type)) {
                    // Memorize the last tag name seen
                    lastTagName = region.getText(subRegion);

                } else if (DOMRegionContext.XML_TAG_ATTRIBUTE_NAME.equals(type)) {
                    // Memorize the last attribute name seen
                    lastAttrName = region.getText(subRegion);

                } else if (DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE.equals(type)) {

                    String lastAttrValue = region.getText(subRegion);
                    if (lastAttrName != null &&
                            lastAttrName.startsWith(SdkConstants.XMLNS_PREFIX)) {

                        // Resolves the android namespace prefix for this file
                        if (SdkConstants.ANDROID_URI.equals(stripQuotes(lastAttrValue))) {
                            String android_namespace_prefix = lastAttrName
                                .substring(SdkConstants.XMLNS_PREFIX.length());
                            android_name_attribute = android_namespace_prefix + ':'
                                + AndroidManifest.ATTRIBUTE_NAME;
                        }
                    } else if (AndroidManifest.NODE_MANIFEST.equals(lastTagName)
                            && AndroidManifest.ATTRIBUTE_PACKAGE.equals(lastAttrName)) {

                        // Found an occurrence. Create a change for it.
                        TextEdit edit = new ReplaceEdit(region.getStartOffset()
                                + subRegion.getStart(), subRegion.getTextLength(),
                                addQuotes(mNewPackageName.getFullyQualifiedName()));

                        multiEdit.addChild(edit);
                        editGroups.add(new TextEditGroup("Change Android package name", edit));

                    } else if (MAIN_COMPONENT_TYPES_LIST.contains(lastTagName)
                            && lastAttrName != null
                            && lastAttrName.equals(android_name_attribute)) {

                        String package_path = stripQuotes(lastAttrValue);
                        String old_package_name_string = mOldPackageName.getFullyQualifiedName();

                        String absolute_path = AndroidManifest.combinePackageAndClassName(
                                old_package_name_string, package_path);

                        TextEdit edit = new ReplaceEdit(region.getStartOffset()
                                + subRegion.getStart(), subRegion.getTextLength(),
                                addQuotes(absolute_path));

                        multiEdit.addChild(edit);

                        editGroups.add(new TextEditGroup("Update component path", edit));
                    }
                }
            }
        }

        if (multiEdit.hasChildren()) {
            xmlChange.setEdit(multiEdit);
            for (TextEditGroup group : editGroups) {
                xmlChange.addTextEditChangeGroup(new TextEditChangeGroup(xmlChange, group));
            }

            return xmlChange;
        }
        return null;
    }


    /*
     * Iterates through all project files, taking distinct actions based on
     * whether the file is:
     * 1) a .java file (replaces or inserts the "import" statements)
     * 2) a .xml layout file (updates namespace declarations)
     * 3) the AndroidManifest.xml
     */
    class JavaFileVisitor implements IResourceVisitor {

        final List<TextFileChange> mChanges = new ArrayList<TextFileChange>();

        final ASTParser mParser = ASTParser.newParser(AST.JLS3);

        public CompositeChange getChange() {

            Collections.reverse(mChanges);
            CompositeChange change = new CompositeChange("Refactoring Application package name",
                    mChanges.toArray(new Change[mChanges.size()]));
            change.markAsSynthetic();
            return change;
        }

        @Override
        public boolean visit(IResource resource) throws CoreException {
            if (resource instanceof IFile) {
                IFile file = (IFile) resource;
                if (SdkConstants.EXT_JAVA.equals(file.getFileExtension())) {

                    ICompilationUnit icu = JavaCore.createCompilationUnitFrom(file);

                    mParser.setSource(icu);
                    CompilationUnit cu = (CompilationUnit) mParser.createAST(null);

                    TextEdit textEdit = updateJavaFileImports(cu);
                    if (textEdit != null && textEdit.hasChildren()) {
                        MultiTextEdit edit = new MultiTextEdit();
                        edit.addChild(textEdit);

                        TextFileChange text_file_change = new TextFileChange(file.getName(), file);
                        text_file_change.setTextType(SdkConstants.EXT_JAVA);
                        text_file_change.setEdit(edit);
                        mChanges.add(text_file_change);
                    }

                    // XXX Partially taken from ExtractStringRefactoring.java
                    // Check this a Layout XML file and get the selection and
                    // its context.
                } else if (SdkConstants.EXT_XML.equals(file.getFileExtension())) {

                    if (SdkConstants.FN_ANDROID_MANIFEST_XML.equals(file.getName())) {
                        // Ensure that this is the root manifest, not some other copy
                        // (such as the one in bin/)
                        IPath path = file.getFullPath();
                        if (path.segmentCount() == 2) {
                            TextFileChange manifest_change = editAndroidManifest(file);
                            mChanges.add(manifest_change);
                        }
                    } else {

                        // Currently we only support Android resource XML files,
                        // so they must have a path similar to
                        //   project/res/<type>[-<configuration>]/*.xml
                        // There is no support for sub folders, so the segment count must be 4.
                        // We don't need to check the type folder name because
                        // a/ we only accept an AndroidXmlEditor source and
                        // b/ aapt generates a compilation error for unknown folders.
                        IPath path = file.getFullPath();
                        // check if we are inside the project/res/* folder.
                        if (path.segmentCount() == 4) {
                            if (path.segment(1).equalsIgnoreCase(SdkConstants.FD_RESOURCES)) {


                                TextFileChange xmlChange = editXmlResourceFile(file);
                                if (xmlChange != null) {
                                    mChanges.add(xmlChange);
                                }
                            }
                        }
                    }
                }

                return false;

            } else if (resource instanceof IFolder) {
                return !SdkConstants.FD_GEN_SOURCES.equals(resource.getName());
            }

            return true;
        }
    }

    private static class UsageVisitor extends ASTVisitor {
        private boolean mSeenManifest;
        private boolean mSeenR;
        private boolean mSeenBuildConfig;

        @Override
        public boolean visit(QualifiedName node) {
            Name qualifier = node.getQualifier();
            if (qualifier.isSimpleName()) {
                String name = qualifier.toString();
                if (name.equals(FN_RESOURCE_BASE)) {
                    mSeenR = true;
                } else if (name.equals(FN_BUILD_CONFIG_BASE)) {
                    mSeenBuildConfig = true;
                } else if (name.equals(FN_MANIFEST_BASE)) {
                    mSeenManifest = true;
                }
            }
            return super.visit(node);
        };

        public boolean seenAny() {
            return mSeenR || mSeenBuildConfig || mSeenManifest;
        }

        public boolean hasSeenBuildConfig() {
            return mSeenBuildConfig;
        }
        public boolean hasSeenManifest() {
            return mSeenManifest;
        }
        public boolean hasSeenR() {
            return mSeenR;
        }
    }

    private class ImportVisitor extends ASTVisitor {

        final AST mAst;
        final ASTRewrite mRewriter;

        ImportVisitor(AST ast) {
            mAst = ast;
            mRewriter = ASTRewrite.create(ast);
        }

        public TextEdit getTextEdit() {
            try {
                return this.mRewriter.rewriteAST();
            } catch (JavaModelException e) {
                Status s = new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID, e.getMessage(), e);
                AdtPlugin.getDefault().getLog().log(s);
            } catch (IllegalArgumentException e) {
                Status s = new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID, e.getMessage(), e);
                AdtPlugin.getDefault().getLog().log(s);
            }
            return null;
        }

        @Override
        public boolean visit(ImportDeclaration id) {

            Name importName = id.getName();
            if (importName.isQualifiedName()) {
                QualifiedName qualifiedImportName = (QualifiedName) importName;

                String identifier = qualifiedImportName.getName().getIdentifier();
                if (identifier.equals(FN_RESOURCE_BASE)) {
                    mRewriter.replace(qualifiedImportName.getQualifier(), mNewPackageName,
                            null);
                } else if (identifier.equals(FN_BUILD_CONFIG_BASE)
                        && mOldPackageName.toString().equals(
                                qualifiedImportName.getQualifier().toString())) {
                    mRewriter.replace(qualifiedImportName.getQualifier(), mNewPackageName,
                            null);

                } else if (identifier.equals(FN_MANIFEST_BASE)
                        && mOldPackageName.toString().equals(
                                qualifiedImportName.getQualifier().toString())) {
                    mRewriter.replace(qualifiedImportName.getQualifier(), mNewPackageName,
                            null);
                }
            }

            return true;
        }
    }
}

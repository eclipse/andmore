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

package org.eclipse.andmore.internal.refactorings.core;

import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_CLASS;
import static com.android.SdkConstants.ATTR_CONTEXT;
import static com.android.SdkConstants.ATTR_NAME;
import static com.android.SdkConstants.ATTR_PACKAGE;
import static com.android.SdkConstants.DOT_XML;
import static com.android.SdkConstants.EXT_XML;
import static com.android.SdkConstants.TOOLS_URI;
import static com.android.SdkConstants.VIEW_FRAGMENT;
import static com.android.SdkConstants.VIEW_TAG;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.ide.common.xml.ManifestData;
import com.android.resources.ResourceFolderType;
import com.android.utils.SdkUtils;

import org.eclipse.andmore.AdtConstants;
import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.editors.layout.gle2.DomUtilities;
import org.eclipse.andmore.internal.project.AndroidManifestHelper;
import org.eclipse.andmore.internal.sdk.ProjectState;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.corext.refactoring.changes.RenamePackageChange;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameCompilationUnitProcessor;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameTypeProcessor;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.FileStatusContext;
import org.eclipse.ltk.core.refactoring.NullChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.ltk.core.refactoring.participants.RenameParticipant;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A participant to participate in refactorings that rename a package in an Android project.
 * The class updates android manifest and the layout file
 * The user can suppress refactoring by disabling the "Update references" checkbox
 * <p>
 * Rename participants are registered via the extension point <code>
 * org.eclipse.ltk.core.refactoring.renameParticipants</code>.
 * Extensions to this extension point must therefore extend
 * <code>org.eclipse.ltk.core.refactoring.participants.RenameParticipant</code>.
 * </p>
 */
@SuppressWarnings("restriction")
public class AndroidPackageRenameParticipant extends RenameParticipant {

    private IProject mProject;
    private IFile mManifestFile;
    private IPackageFragment mPackageFragment;
    private String mOldPackage;
    private String mNewPackage;
    private String mAppPackage;
    private boolean mRefactoringAppPackage;

    @Override
    public String getName() {
        return "Android Package Rename";
    }

    @Override
    public RefactoringStatus checkConditions(IProgressMonitor pm, CheckConditionsContext context)
            throws OperationCanceledException {
        if (mAppPackage.equals(mOldPackage) && !mRefactoringAppPackage) {
            IRegion region = null;
            Document document = DomUtilities.getDocument(mManifestFile);
            if (document != null && document.getDocumentElement() != null) {
                Attr attribute = document.getDocumentElement().getAttributeNode(ATTR_PACKAGE);
                if (attribute instanceof IndexedRegion) {
                    IndexedRegion ir = (IndexedRegion) attribute;
                    int start = ir.getStartOffset();
                    region = new Region(start, ir.getEndOffset() - start);
                }
            }
            if (region == null) {
                region = new Region(0, 0);
            }
            // There's no line wrapping in the error dialog, so split up the message into
            // individually digestible pieces of information
            RefactoringStatusContext ctx = new FileStatusContext(mManifestFile, region);
            RefactoringStatus status = RefactoringStatus.createInfoStatus(
                    "You are refactoring the same package as your application's " +
                    "package (specified in the manifest).\n", ctx);
            status.addInfo(
                    "Note that this refactoring does NOT also update your " +
                    "application package.", ctx);
            status.addInfo("The application package defines your application's identity.", ctx);
            status.addInfo(
                    "If you change it, then it is considered to be a different application.", ctx);
            status.addInfo("(Users of the previous version cannot update to the new version.)",
                    ctx);
            status.addInfo(
                    "The application package, and the package containing the code, can differ.",
                    ctx);
            status.addInfo(
                    "To really change application package, " +
                    "choose \"Android Tools\" > \"Rename  Application Package.\" " +
                    "from the project context menu.", ctx);
            return status;
        }

        return new RefactoringStatus();
    }

    @Override
    protected boolean initialize(final Object element) {
        mRefactoringAppPackage = false;
        try {
            // Only propose this refactoring if the "Update References" checkbox is set.
            if (!getArguments().getUpdateReferences()) {
                return false;
            }

            if (element instanceof IPackageFragment) {
                mPackageFragment = (IPackageFragment) element;
                if (!mPackageFragment.containsJavaResources()) {
                    return false;
                }
                IJavaProject javaProject = (IJavaProject) mPackageFragment
                        .getAncestor(IJavaElement.JAVA_PROJECT);
                mProject = javaProject.getProject();
                IResource manifestResource = mProject.findMember(AdtConstants.WS_SEP
                        + SdkConstants.FN_ANDROID_MANIFEST_XML);

                if (manifestResource == null || !manifestResource.exists()
                        || !(manifestResource instanceof IFile)) {
                    RefactoringUtil.logInfo("Invalid or missing the "
                            + SdkConstants.FN_ANDROID_MANIFEST_XML + " in the "
                            + mProject.getName() + " project.");
                    return false;
                }
                mManifestFile = (IFile) manifestResource;
                String packageName = mPackageFragment.getElementName();
                ManifestData manifestData;
                manifestData = AndroidManifestHelper.parseForData(mManifestFile);
                if (manifestData == null) {
                    return false;
                }
                mAppPackage = manifestData.getPackage();
                mOldPackage = packageName;
                mNewPackage = getArguments().getNewName();
                if (mOldPackage == null || mNewPackage == null) {
                    return false;
                }

                if (RefactoringUtil.isRefactorAppPackage()
                        && mAppPackage != null
                        && mAppPackage.equals(packageName)) {
                    mRefactoringAppPackage = true;
                }

                return true;
            }
        } catch (JavaModelException ignore) {
        }
        return false;
    }


    @Override
    public Change createChange(IProgressMonitor pm) throws CoreException,
            OperationCanceledException {
        if (pm.isCanceled()) {
            return null;
        }
        if (!getArguments().getUpdateReferences()) {
            return null;
        }

        RefactoringProcessor p = getProcessor();
        if (p instanceof RenameCompilationUnitProcessor) {
            RenameTypeProcessor rtp =
                    ((RenameCompilationUnitProcessor) p).getRenameTypeProcessor();
            if (rtp != null) {
                String pattern = rtp.getFilePatterns();
                boolean updQualf = rtp.getUpdateQualifiedNames();
                if (updQualf && pattern != null && pattern.contains("xml")) { //$NON-NLS-1$
                    // Do not propose this refactoring if the
                    // "Update fully qualified names in non-Java files" option is
                    // checked and the file patterns mention XML. [c.f. SDK bug 21589]
                    return null;
                }
            }
        }

        IPath pkgPath = mPackageFragment.getPath();
        IPath genPath = mProject.getFullPath().append(SdkConstants.FD_GEN_SOURCES);
        if (genPath.isPrefixOf(pkgPath)) {
            RefactoringUtil.logInfo(getName() + ": Cannot rename generated package.");
            return null;
        }
        CompositeChange result = new CompositeChange(getName());
        result.markAsSynthetic();

        addManifestFileChanges(result);

        // Update layout files; we don't just need to react to custom view
        // changes, we need to update fragment references and even tool:context activity
        // references
        addLayoutFileChanges(mProject, result);

        // Also update in dependent projects
        ProjectState projectState = Sdk.getProjectState(mProject);
        if (projectState != null) {
            Collection<ProjectState> parentProjects = projectState.getFullParentProjects();
            for (ProjectState parentProject : parentProjects) {
                IProject project = parentProject.getProject();
                addLayoutFileChanges(project, result);
            }
        }

        if (mRefactoringAppPackage) {
            Change genChange = getGenPackageChange(pm);
            if (genChange != null) {
                result.add(genChange);
            }

            return new NullChange("Update Imports") {
                @Override
                public Change perform(IProgressMonitor monitor) throws CoreException {
                    FixImportsJob job = new FixImportsJob("Fix Rename Package",
                            mManifestFile, mNewPackage);
                    job.schedule(500);

                    // Not undoable: just return null instead of an undo-change.
                    return null;
                }
            };
        }

        return (result.getChildren().length == 0) ? null : result;
    }

    /**
     * Returns Android gen package text change
     *
     * @param pm the progress monitor
     *
     * @return Android gen package text change
     * @throws CoreException if an error happens
     * @throws OperationCanceledException if the operation is canceled
     */
    public Change getGenPackageChange(IProgressMonitor pm) throws CoreException,
            OperationCanceledException {
        if (mRefactoringAppPackage) {
            IPackageFragment genJavaPackageFragment = getGenPackageFragment();
            if (genJavaPackageFragment != null && genJavaPackageFragment.exists()) {
                return new RenamePackageChange(genJavaPackageFragment, mNewPackage, true);
            }
        }
        return null;
    }

    /**
     * Return the gen package fragment
     */
    private IPackageFragment getGenPackageFragment() throws JavaModelException {
        IJavaProject javaProject = (IJavaProject) mPackageFragment
                .getAncestor(IJavaElement.JAVA_PROJECT);
        if (javaProject != null && javaProject.isOpen()) {
            IProject project = javaProject.getProject();
            IFolder genFolder = project.getFolder(SdkConstants.FD_GEN_SOURCES);
            if (genFolder.exists()) {
                String javaPackagePath = mAppPackage.replace('.', '/');
                IPath genJavaPackagePath = genFolder.getFullPath().append(javaPackagePath);
                IPackageFragment genPackageFragment = javaProject
                        .findPackageFragment(genJavaPackagePath);
                return genPackageFragment;
            }
        }
        return null;
    }

    /**
     * Returns the new class name
     *
     * @param fqcn the fully qualified class name in the renamed package
     * @return the new class name
     */
    private String getNewClassName(String fqcn) {
        assert isInRenamedPackage(fqcn) : fqcn;
        int lastDot = fqcn.lastIndexOf('.');
        if (lastDot < 0) {
            return mNewPackage;
        }
        String name = fqcn.substring(lastDot, fqcn.length());
        String newClassName = mNewPackage + name;
        return newClassName;
    }

    private void addManifestFileChanges(CompositeChange result) {
        addXmlFileChanges(mManifestFile, result, true);
    }

    private void addLayoutFileChanges(IProject project, CompositeChange result) {
        try {
            // Update references in XML resource files
            IFolder resFolder = project.getFolder(SdkConstants.FD_RESOURCES);

            IResource[] folders = resFolder.members();
            for (IResource folder : folders) {
                String folderName = folder.getName();
                ResourceFolderType folderType = ResourceFolderType.getFolderType(folderName);
                if (folderType != ResourceFolderType.LAYOUT) {
                    continue;
                }
                if (!(folder instanceof IFolder)) {
                    continue;
                }
                IResource[] files = ((IFolder) folder).members();
                for (int i = 0; i < files.length; i++) {
                    IResource member = files[i];
                    if ((member instanceof IFile) && member.exists()) {
                        IFile file = (IFile) member;
                        String fileName = member.getName();

                        if (SdkUtils.endsWith(fileName, DOT_XML)) {
                            addXmlFileChanges(file, result, false);
                        }
                    }
                }
            }
        } catch (CoreException e) {
            RefactoringUtil.log(e);
        }
    }

    private boolean addXmlFileChanges(IFile file, CompositeChange changes, boolean isManifest) {
        IModelManager modelManager = StructuredModelManager.getModelManager();
        IStructuredModel model = null;
        try {
            model = modelManager.getExistingModelForRead(file);
            if (model == null) {
                model = modelManager.getModelForRead(file);
            }
            if (model != null) {
                IStructuredDocument document = model.getStructuredDocument();
                if (model instanceof IDOMModel) {
                    IDOMModel domModel = (IDOMModel) model;
                    Element root = domModel.getDocument().getDocumentElement();
                    if (root != null) {
                        List<TextEdit> edits = new ArrayList<TextEdit>();
                        if (isManifest) {
                            addManifestReplacements(edits, root, document);
                        } else {
                            addLayoutReplacements(edits, root, document);
                        }
                        if (!edits.isEmpty()) {
                            MultiTextEdit rootEdit = new MultiTextEdit();
                            rootEdit.addChildren(edits.toArray(new TextEdit[edits.size()]));
                            TextFileChange change = new TextFileChange(file.getName(), file);
                            change.setTextType(EXT_XML);
                            change.setEdit(rootEdit);
                            changes.add(change);
                        }
                    }
                } else {
                    return false;
                }
            }

            return true;
        } catch (IOException e) {
            AdtPlugin.log(e, null);
        } catch (CoreException e) {
            AdtPlugin.log(e, null);
        } finally {
            if (model != null) {
                model.releaseFromRead();
            }
        }

        return false;
    }

    private boolean isInRenamedPackage(String fqcn) {
        return fqcn.startsWith(mOldPackage)
                && fqcn.length() > mOldPackage.length()
                && fqcn.indexOf('.', mOldPackage.length() + 1) == -1;
    }

    private void addLayoutReplacements(
            @NonNull List<TextEdit> edits,
            @NonNull Element element,
            @NonNull IStructuredDocument document) {
        String tag = element.getTagName();
        if (isInRenamedPackage(tag)) {
            int start = RefactoringUtil.getTagNameRangeStart(element, document);
            if (start != -1) {
                int end = start + tag.length();
                edits.add(new ReplaceEdit(start, end - start, getNewClassName(tag)));
            }
        } else {
            Attr classNode = null;
            if (tag.equals(VIEW_TAG)) {
                classNode = element.getAttributeNode(ATTR_CLASS);
            } else if (tag.equals(VIEW_FRAGMENT)) {
                classNode = element.getAttributeNode(ATTR_CLASS);
                if (classNode == null) {
                    classNode = element.getAttributeNodeNS(ANDROID_URI, ATTR_NAME);
                }
            } else if (element.hasAttributeNS(TOOLS_URI, ATTR_CONTEXT)) {
                classNode = element.getAttributeNodeNS(TOOLS_URI, ATTR_CONTEXT);
                if (classNode != null && classNode.getValue().startsWith(".")) { //$NON-NLS-1$
                    classNode = null;
                }
            }
            if (classNode != null) {
                String fqcn = classNode.getValue();
                if (isInRenamedPackage(fqcn)) {
                    int start = RefactoringUtil.getAttributeValueRangeStart(classNode, document);
                    if (start != -1) {
                        int end = start + fqcn.length();
                        edits.add(new ReplaceEdit(start, end - start, getNewClassName(fqcn)));
                    }
                }
            }
        }

        NodeList children = element.getChildNodes();
        for (int i = 0, n = children.getLength(); i < n; i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                addLayoutReplacements(edits, (Element) child, document);
            }
        }
    }

    private void addManifestReplacements(
            @NonNull List<TextEdit> edits,
            @NonNull Element element,
            @NonNull IStructuredDocument document) {
        if (mRefactoringAppPackage &&
                element == element.getOwnerDocument().getDocumentElement()) {
            // Update the app package declaration
            Attr pkg = element.getAttributeNode(ATTR_PACKAGE);
            if (pkg != null && pkg.getValue().equals(mOldPackage)) {
                int start = RefactoringUtil.getAttributeValueRangeStart(pkg, document);
                if (start != -1) {
                    int end = start + mOldPackage.length();
                    edits.add(new ReplaceEdit(start, end - start, mNewPackage));
                }
            }
        }

        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0, n = attributes.getLength(); i < n; i++) {
            Attr attr = (Attr) attributes.item(i);
            if (!RefactoringUtil.isManifestClassAttribute(attr)) {
                continue;
            }

            String value = attr.getValue();
            if (isInRenamedPackage(value)) {
                int start = RefactoringUtil.getAttributeValueRangeStart(attr, document);
                if (start != -1) {
                    int end = start + value.length();
                    edits.add(new ReplaceEdit(start, end - start, getNewClassName(value)));
                }
            } else if (value.startsWith(".")) {
                // If we're renaming the app package
                String fqcn = mAppPackage + value;
                if (isInRenamedPackage(fqcn)) {
                    int start = RefactoringUtil.getAttributeValueRangeStart(attr, document);
                    if (start != -1) {
                        int end = start + value.length();
                        String newClassName = getNewClassName(fqcn);
                        if (mRefactoringAppPackage) {
                            newClassName = newClassName.substring(mNewPackage.length());
                        } else if (newClassName.startsWith(mOldPackage)
                                && newClassName.charAt(mOldPackage.length()) == '.') {
                            newClassName = newClassName.substring(mOldPackage.length());
                        }

                        if (!newClassName.equals(value)) {
                            edits.add(new ReplaceEdit(start, end - start, newClassName));
                        }
                    }
                }
            }
        }

        NodeList children = element.getChildNodes();
        for (int i = 0, n = children.getLength(); i < n; i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                addManifestReplacements(edits, (Element) child, document);
            }
        }
    }
}

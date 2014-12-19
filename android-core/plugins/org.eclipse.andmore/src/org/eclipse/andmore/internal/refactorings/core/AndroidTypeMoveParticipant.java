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
import org.eclipse.andmore.internal.project.AndroidManifestHelper;
import org.eclipse.andmore.internal.sdk.ProjectState;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.MoveParticipant;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A participant to participate in refactorings that move a type in an Android project.
 * The class updates android manifest and the layout file
 * The user can suppress refactoring by disabling the "Update references" checkbox
 * <p>
 * Rename participants are registered via the extension point <code>
 * org.eclipse.ltk.core.refactoring.moveParticipants</code>.
 * Extensions to this extension point must therefore extend <code>org.eclipse.ltk.core.refactoring.participants.MoveParticipant</code>.
 * </p>
 */
@SuppressWarnings("restriction")
public class AndroidTypeMoveParticipant extends MoveParticipant {

    private IProject mProject;
    protected IFile mManifestFile;
    protected String mOldFqcn;
    protected String mNewFqcn;
    protected String mAppPackage;

    @Override
    public String getName() {
        return "Android Type Move";
    }

    @Override
    public RefactoringStatus checkConditions(IProgressMonitor pm, CheckConditionsContext context)
            throws OperationCanceledException {
        return new RefactoringStatus();
    }

    @Override
    protected boolean initialize(Object element) {
        if (element instanceof IType) {
            IType type = (IType) element;
            IJavaProject javaProject = (IJavaProject) type.getAncestor(IJavaElement.JAVA_PROJECT);
            mProject = javaProject.getProject();
            IResource manifestResource = mProject.findMember(AdtConstants.WS_SEP
                    + SdkConstants.FN_ANDROID_MANIFEST_XML);

            if (manifestResource == null || !manifestResource.exists()
                    || !(manifestResource instanceof IFile)) {
                RefactoringUtil.logInfo("Invalid or missing the "
                        + SdkConstants.FN_ANDROID_MANIFEST_XML + " in the " + mProject.getName()
                        + " project.");
                return false;
            }
            mManifestFile = (IFile) manifestResource;
            ManifestData manifestData;
            manifestData = AndroidManifestHelper.parseForData(mManifestFile);
            if (manifestData == null) {
                return false;
            }
            mAppPackage = manifestData.getPackage();
            mOldFqcn = type.getFullyQualifiedName();
            Object destination = getArguments().getDestination();
            if (destination instanceof IPackageFragment) {
                IPackageFragment packageFragment = (IPackageFragment) destination;
                mNewFqcn = packageFragment.getElementName() + "." + type.getElementName();
            } else if (destination instanceof IResource) {
                try {
                    IPackageFragment[] fragments = javaProject.getPackageFragments();
                    for (IPackageFragment fragment : fragments) {
                        IResource resource = fragment.getResource();
                        if (resource.equals(destination)) {
                            mNewFqcn = fragment.getElementName() + '.' + type.getElementName();
                            break;
                        }
                    }
                } catch (JavaModelException e) {
                    // pass
                }
            }
            return mOldFqcn != null && mNewFqcn != null;
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

        return (result.getChildren().length == 0) ? null : result;
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

    private void addLayoutReplacements(
            @NonNull List<TextEdit> edits,
            @NonNull Element element,
            @NonNull IStructuredDocument document) {
        String tag = element.getTagName();
        if (tag.equals(mOldFqcn)) {
            int start = RefactoringUtil.getTagNameRangeStart(element, document);
            if (start != -1) {
                int end = start + mOldFqcn.length();
                edits.add(new ReplaceEdit(start, end - start, mNewFqcn));
            }
        } else if (tag.equals(VIEW_TAG)) {
            Attr classNode = element.getAttributeNode(ATTR_CLASS);
            if (classNode != null && classNode.getValue().equals(mOldFqcn)) {
                int start = RefactoringUtil.getAttributeValueRangeStart(classNode, document);
                if (start != -1) {
                    int end = start + mOldFqcn.length();
                    edits.add(new ReplaceEdit(start, end - start, mNewFqcn));
                }
            }
        } else if (tag.equals(VIEW_FRAGMENT)) {
            Attr classNode = element.getAttributeNode(ATTR_CLASS);
            if (classNode == null) {
                classNode = element.getAttributeNodeNS(ANDROID_URI, ATTR_NAME);
            }
            if (classNode != null && classNode.getValue().equals(mOldFqcn)) {
                int start = RefactoringUtil.getAttributeValueRangeStart(classNode, document);
                if (start != -1) {
                    int end = start + mOldFqcn.length();
                    edits.add(new ReplaceEdit(start, end - start, mNewFqcn));
                }
            }
        } else if (element.hasAttributeNS(TOOLS_URI, ATTR_CONTEXT)) {
            Attr classNode = element.getAttributeNodeNS(TOOLS_URI, ATTR_CONTEXT);
            if (classNode != null && classNode.getValue().equals(mOldFqcn)) {
                int start = RefactoringUtil.getAttributeValueRangeStart(classNode, document);
                if (start != -1) {
                    int end = start + mOldFqcn.length();
                    edits.add(new ReplaceEdit(start, end - start, mNewFqcn));
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
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0, n = attributes.getLength(); i < n; i++) {
            Attr attr = (Attr) attributes.item(i);
            if (!RefactoringUtil.isManifestClassAttribute(attr)) {
                continue;
            }

            String value = attr.getValue();
            if (value.equals(mOldFqcn)) {
                int start = RefactoringUtil.getAttributeValueRangeStart(attr, document);
                if (start != -1) {
                    int end = start + mOldFqcn.length();
                    edits.add(new ReplaceEdit(start, end - start, mNewFqcn));
                }
            } else if (value.startsWith(".")) { //$NON-NLS-1$
                String fqcn = mAppPackage + value;
                if (fqcn.equals(mOldFqcn)) {
                    int start = RefactoringUtil.getAttributeValueRangeStart(attr, document);
                    if (start != -1) {
                        int end = start + value.length();
                        edits.add(new ReplaceEdit(start, end - start, mNewFqcn));
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

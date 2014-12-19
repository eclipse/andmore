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

import static com.android.SdkConstants.ANDROID_PREFIX;
import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_ID;
import static com.android.SdkConstants.ATTR_NAME;
import static com.android.SdkConstants.ATTR_TYPE;
import static com.android.SdkConstants.DOT_XML;
import static com.android.SdkConstants.EXT_XML;
import static com.android.SdkConstants.FD_RES;
import static com.android.SdkConstants.FN_RESOURCE_CLASS;
import static com.android.SdkConstants.NEW_ID_PREFIX;
import static com.android.SdkConstants.PREFIX_RESOURCE_REF;
import static com.android.SdkConstants.PREFIX_THEME_REF;
import static com.android.SdkConstants.R_CLASS;
import static com.android.SdkConstants.TAG_ITEM;
import static com.android.SdkConstants.TOOLS_URI;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.resources.ResourceFolderType;
import com.android.resources.ResourceType;
import com.android.utils.SdkUtils;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.AdtUtils;
import org.eclipse.andmore.internal.editors.manifest.ManifestInfo;
import org.eclipse.andmore.internal.project.BaseProjectHelper;
import org.eclipse.andmore.internal.resources.ResourceNameValidator;
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
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameFieldProcessor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RenameParticipant;
import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;
import org.eclipse.ltk.core.refactoring.resource.RenameResourceChange;
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
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A rename participant handling renames of resources (such as R.id.foo and R.layout.bar).
 * This reacts to refactorings of fields in the R inner classes (such as R.id), and updates
 * the XML files as appropriate; renaming .xml files, updating XML attributes, resource
 * references in style declarations, and so on.
 */
@SuppressWarnings("restriction") // WTP API
public class RenameResourceParticipant extends RenameParticipant {
    /** The project we're refactoring in */
    private @NonNull IProject mProject;

    /** The type of the resource we're refactoring, such as {@link ResourceType#ID} */
    private @NonNull ResourceType mType;
    /**
     * The type of the resource folder we're refactoring in, such as
     * {@link ResourceFolderType#VALUES}. When refactoring non value files, we need to
     * rename the files as well.
     */
    private @NonNull ResourceFolderType mFolderType;

    /** The previous name of the resource */
    private @NonNull String mOldName;

    /** The new name of the resource */
    private @NonNull String mNewName;

    /** Whether references to the resource should be updated */
    private boolean mUpdateReferences;

    /** A match pattern to look for in XML, such as {@code @attr/foo} */
    private @NonNull String mXmlMatch1;

    /** A match pattern to look for in XML, such as {@code ?attr/foo} */
    private @Nullable String mXmlMatch2;

    /** A match pattern to look for in XML, such as {@code ?foo} */
    private @Nullable String mXmlMatch3;

    /** The value to replace a reference to {@link #mXmlMatch1} with, such as {@code @attr/bar} */
    private @NonNull String mXmlNewValue1;

    /** The value to replace a reference to {@link #mXmlMatch2} with, such as {@code ?attr/bar} */
    private @Nullable String mXmlNewValue2;

    /** The value to replace a reference to {@link #mXmlMatch3} with, such as {@code ?bar} */
    private @Nullable String mXmlNewValue3;

    /**
     * If non null, this refactoring was initiated as a file rename of an XML file (and if
     * null, we are just reacting to a Java field rename)
     */
    private IFile mRenamedFile;

    /**
     * If renaming a field, we need to create an embedded field refactoring to update the
     * Java sources referring to the corresponding R class field. This is stored as an
     * instance such that we can have it participate in both the condition check methods
     * as well as the {@link #createChange(IProgressMonitor)} refactoring operation.
     */
    private RenameRefactoring mFieldRefactoring;

    /**
     * Set while we are creating an embedded Java refactoring. This could cause a recursive
     * invocation of the XML renaming refactoring to react to the field, so this is flag
     * during the call to the Java processor, and is used to ignore requests for adding in
     * field reactions during that time.
     */
    private static boolean sIgnore;

    /**
     * Creates a new {@linkplain RenameResourceParticipant}
     */
    public RenameResourceParticipant() {
    }

    @Override
    public String getName() {
        return "Android Rename Field Participant";
    }

    @Override
    protected boolean initialize(Object element) {
        if (sIgnore) {
            return false;
        }

        if (element instanceof IField) {
            IField field = (IField) element;
            IType declaringType = field.getDeclaringType();
            if (declaringType != null) {
                if (R_CLASS.equals(declaringType.getParent().getElementName())) {
                    String typeName = declaringType.getElementName();
                    mType = ResourceType.getEnum(typeName);
                    if (mType != null) {
                        mUpdateReferences = getArguments().getUpdateReferences();
                        mFolderType = AdtUtils.getFolderTypeFor(mType);
                        IJavaProject javaProject = (IJavaProject) field.getAncestor(
                                IJavaElement.JAVA_PROJECT);
                        mProject = javaProject.getProject();
                        mOldName = field.getElementName();
                        mNewName = getArguments().getNewName();
                        mFieldRefactoring = null;
                        mRenamedFile = null;
                        createXmlSearchPatterns();
                        return true;
                    }
                }
            }

            return false;
        } else if (element instanceof IFile) {
            IFile file = (IFile) element;
            mProject = file.getProject();
            if (BaseProjectHelper.isAndroidProject(mProject)) {
                IPath path = file.getFullPath();
                int segments = path.segmentCount();
                if (segments == 4 && path.segment(1).equals(FD_RES)) {
                    String parentName = file.getParent().getName();
                    mFolderType = ResourceFolderType.getFolderType(parentName);
                    if (mFolderType != null && mFolderType != ResourceFolderType.VALUES) {
                        mType = AdtUtils.getResourceTypeFor(mFolderType);
                        if (mType != null) {
                            mUpdateReferences = getArguments().getUpdateReferences();
                            mProject = file.getProject();
                            mOldName = AdtUtils.stripAllExtensions(file.getName());
                            mNewName = AdtUtils.stripAllExtensions(getArguments().getNewName());
                            mRenamedFile = file;
                            createXmlSearchPatterns();

                            mFieldRefactoring = null;
                            IField field = getResourceField(mProject, mType, mOldName);
                            if (field != null) {
                                mFieldRefactoring = createFieldRefactoring(field);
                            } else {
                                // no corresponding field; aapt has not run yet. Perhaps user has
                                // turned off auto build.
                                mFieldRefactoring = null;
                            }

                            return true;
                        }
                    }
                }
            }
        } else if (element instanceof String) {
            String uri = (String) element;
            if (uri.startsWith(PREFIX_RESOURCE_REF) && !uri.startsWith(ANDROID_PREFIX)) {
                RenameResourceProcessor processor = (RenameResourceProcessor) getProcessor();
                mProject = processor.getProject();
                mType = processor.getType();
                mFolderType = AdtUtils.getFolderTypeFor(mType);
                mOldName = processor.getCurrentName();
                mNewName = processor.getNewName();
                assert uri.endsWith(mOldName) && uri.contains(mType.getName()) : uri;
                mUpdateReferences = getArguments().getUpdateReferences();
                if (mNewName.isEmpty()) {
                    mUpdateReferences = false;
                }
                mRenamedFile = null;
                createXmlSearchPatterns();
                mFieldRefactoring = null;
                if (!mNewName.isEmpty()) {
                    IField field = getResourceField(mProject, mType, mOldName);
                    if (field != null) {
                        mFieldRefactoring = createFieldRefactoring(field);
                    }
                }

                return true;
            }
        }

        return false;
    }

    /** Create nested Java refactoring which updates the R field references, if applicable */
    private RenameRefactoring createFieldRefactoring(IField field) {
        return createFieldRefactoring(field, mNewName, mUpdateReferences);
    }

    /**
     * Create nested Java refactoring which updates the R field references, if
     * applicable
     *
     * @param field the field to be refactored
     * @param newName the new name
     * @param updateReferences whether references should be updated
     * @return a new rename refactoring
     */
    public static RenameRefactoring createFieldRefactoring(
            @NonNull IField field,
            @NonNull String newName,
            boolean updateReferences) {
        RenameFieldProcessor processor = new RenameFieldProcessor(field);
        processor.setRenameGetter(false);
        processor.setRenameSetter(false);
        RenameRefactoring refactoring = new RenameRefactoring(processor);
        processor.setUpdateReferences(updateReferences);
        processor.setUpdateTextualMatches(false);
        processor.setNewElementName(newName);
        try {
            if (refactoring.isApplicable()) {
                return refactoring;
            }
        } catch (CoreException e) {
            AdtPlugin.log(e, null);
        }

        return null;
    }

    private void createXmlSearchPatterns() {
        // Set up search strings for the attribute iterator. This will
        // identify string matches for mXmlMatch1, 2 and 3, and when matched,
        // will add a replacement edit for mXmlNewValue1, 2, or 3.
        mXmlMatch2 = null;
        mXmlNewValue2 = null;
        mXmlMatch3 = null;
        mXmlNewValue3 = null;

        String typeName = mType.getName();
        if (mUpdateReferences) {
            mXmlMatch1 = PREFIX_RESOURCE_REF + typeName + '/' + mOldName;
            mXmlNewValue1 = PREFIX_RESOURCE_REF + typeName + '/' + mNewName;
            if (mType == ResourceType.ID) {
                mXmlMatch2 = NEW_ID_PREFIX + mOldName;
                mXmlNewValue2 = NEW_ID_PREFIX + mNewName;
            } else if (mType == ResourceType.ATTR) {
                // When renaming @attr/foo, also edit ?attr/foo
                mXmlMatch2 = PREFIX_THEME_REF + typeName + '/' + mOldName;
                mXmlNewValue2 = PREFIX_THEME_REF + typeName + '/' + mNewName;
                // as well as ?foo
                mXmlMatch3 = PREFIX_THEME_REF + mOldName;
                mXmlNewValue3 = PREFIX_THEME_REF + mNewName;
            }
        } else if (mType == ResourceType.ID) {
            mXmlMatch1 = NEW_ID_PREFIX + mOldName;
            mXmlNewValue1 = NEW_ID_PREFIX + mNewName;
        }
    }

    @Override
    public RefactoringStatus checkConditions(IProgressMonitor pm, CheckConditionsContext context)
            throws OperationCanceledException {
        if (mRenamedFile != null && getArguments().getNewName().indexOf('.') == -1
                && mRenamedFile.getName().indexOf('.') != -1) {
            return RefactoringStatus.createErrorStatus(
                    String.format("You must include the file extension (%1$s?)",
                           mRenamedFile.getName().substring(mRenamedFile.getName().indexOf('.'))));
        }

        // Ensure that the new name is valid
        if (mNewName != null && !mNewName.isEmpty()) {
            ResourceNameValidator validator = ResourceNameValidator.create(false, mProject, mType);
            String error = validator.isValid(mNewName);
            if (error != null) {
                return RefactoringStatus.createErrorStatus(error);
            }
        }

        if (mFieldRefactoring != null) {
            try {
                sIgnore = true;
                return mFieldRefactoring.checkAllConditions(pm);
            } catch (CoreException e) {
                AdtPlugin.log(e, null);
            } finally {
                sIgnore = false;
            }
        }

        return new RefactoringStatus();
    }

    @Override
    public Change createChange(IProgressMonitor monitor) throws CoreException,
            OperationCanceledException {
        if (monitor.isCanceled()) {
            return null;
        }

        CompositeChange result = new CompositeChange("Update resource references");

        // Only show the children in the refactoring preview dialog
        result.markAsSynthetic();

        addResourceFileChanges(result, mProject, monitor);

        // If renaming resources in a library project, also offer to rename references
        // in including projects
        if (mUpdateReferences) {
            ProjectState projectState = Sdk.getProjectState(mProject);
            if (projectState != null && projectState.isLibrary()) {
                List<ProjectState> parentProjects = projectState.getParentProjects();
                for (ProjectState state : parentProjects) {
                    IProject project = state.getProject();
                    CompositeChange nested = new CompositeChange(
                            String.format("Update references in %1$s", project.getName()));
                    addResourceFileChanges(nested, project, monitor);
                    if (nested.getChildren().length > 0) {
                        result.add(nested);
                    }
                }
            }
        }

        if (mFieldRefactoring != null) {
            // We have to add in Java field refactoring
            try {
                sIgnore = true;
                addJavaChanges(result, monitor);
            } finally {
                sIgnore = false;
            }
        } else {
            // Disable field refactoring added by the default Java field rename handler
            disableExistingResourceFileChange();
        }

        return (result.getChildren().length == 0) ? null : result;
    }

    /**
     * Adds all changes to resource files (typically XML but also renaming drawable files
     *
     * @param project the Android project
     * @param className the layout classes
     */
    private void addResourceFileChanges(
            CompositeChange change,
            IProject project,
            IProgressMonitor monitor)
            throws OperationCanceledException {
        if (monitor.isCanceled()) {
            return;
        }

        try {
            // Update resource references in the manifest
            IFile manifest = project.getFile(SdkConstants.ANDROID_MANIFEST_XML);
            if (manifest != null) {
                addResourceXmlChanges(manifest, change, null);
            }

            // Update references in XML resource files
            IFolder resFolder = project.getFolder(SdkConstants.FD_RESOURCES);

            IResource[] folders = resFolder.members();
            for (IResource folder : folders) {
                if (!(folder instanceof IFolder)) {
                    continue;
                }
                String folderName = folder.getName();
                ResourceFolderType folderType = ResourceFolderType.getFolderType(folderName);
                IResource[] files = ((IFolder) folder).members();
                for (int i = 0; i < files.length; i++) {
                    IResource member = files[i];
                    if ((member instanceof IFile) && member.exists()) {
                        IFile file = (IFile) member;
                        String fileName = member.getName();

                        if (SdkUtils.endsWith(fileName, DOT_XML)) {
                            addResourceXmlChanges(file, change, folderType);
                        }

                        if ((mRenamedFile == null || !mRenamedFile.equals(file))
                                && fileName.startsWith(mOldName)
                                && fileName.length() > mOldName.length()
                                && fileName.charAt(mOldName.length()) == '.'
                                && mFolderType != ResourceFolderType.VALUES
                                && mFolderType == folderType) {
                            // Rename this file
                            String newFile = mNewName + fileName.substring(mOldName.length());
                            IPath path = file.getFullPath();
                            change.add(new RenameResourceChange(path, newFile));
                        }
                    }
                }
            }
        } catch (CoreException e) {
            RefactoringUtil.log(e);
        }
    }

    private void addJavaChanges(CompositeChange result, IProgressMonitor monitor)
            throws CoreException, OperationCanceledException {
        if (monitor.isCanceled()) {
            return;
        }

        RefactoringStatus status = mFieldRefactoring.checkAllConditions(monitor);
        if (status != null && !status.hasError()) {
            Change fieldChanges = mFieldRefactoring.createChange(monitor);
            if (fieldChanges != null) {
                result.add(fieldChanges);

                // Look for the field change on the R.java class; it's a derived file
                // and will generate file modified manually warnings. Disable it.
                disableRClassChanges(fieldChanges);
            }
        }
    }

    private boolean addResourceXmlChanges(
            IFile file,
            CompositeChange changes,
            ResourceFolderType folderType) {
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
                        addReplacements(edits, root, document, folderType);
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

    private void addReplacements(
            @NonNull List<TextEdit> edits,
            @NonNull Element element,
            @NonNull IStructuredDocument document,
            @Nullable ResourceFolderType folderType) {
        String tag = element.getTagName();
        if (folderType == ResourceFolderType.VALUES) {
            // Look for
            //   <item name="main_layout" type="layout">...</item>
            //   <item name="myid" type="id"/>
            //   <string name="mystring">...</string>
            // etc
            if (tag.equals(mType.getName())
                    || (tag.equals(TAG_ITEM)
                            && (mType == ResourceType.ID
                                || mType.getName().equals(element.getAttribute(ATTR_TYPE))))) {
                Attr nameNode = element.getAttributeNode(ATTR_NAME);
                if (nameNode != null && nameNode.getValue().equals(mOldName)) {
                    int start = RefactoringUtil.getAttributeValueRangeStart(nameNode, document);
                    if (start != -1) {
                        int end = start + mOldName.length();
                        edits.add(new ReplaceEdit(start, end - start, mNewName));
                    }
                }
            }
        }

        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0, n = attributes.getLength(); i < n; i++) {
            Attr attr = (Attr) attributes.item(i);
            String value = attr.getValue();

            // If not updating references, only update XML matches that define the id
            if (!mUpdateReferences && (!ATTR_ID.equals(attr.getLocalName()) ||
                    !ANDROID_URI.equals(attr.getNamespaceURI()))) {

                if (TOOLS_URI.equals(attr.getNamespaceURI()) && value.equals(mXmlMatch1)) {
                    int start = RefactoringUtil.getAttributeValueRangeStart(attr, document);
                    if (start != -1) {
                        int end = start + mXmlMatch1.length();
                        edits.add(new ReplaceEdit(start, end - start, mXmlNewValue1));
                    }
                }

                continue;
            }

            // Replace XML attribute reference, such as
            //   android:id="@+id/oldName"   =>   android:id="+id/newName"

            String match = null;
            String matchedValue = null;

            if (value.equals(mXmlMatch1)) {
                match = mXmlMatch1;
                matchedValue = mXmlNewValue1;
            } else if (value.equals(mXmlMatch2)) {
                match = mXmlMatch2;
                matchedValue = mXmlNewValue2;
            } else if (value.equals(mXmlMatch3)) {
                match = mXmlMatch3;
                matchedValue = mXmlNewValue3;
            } else {
                continue;
            }

            if (match != null) {
                if (mNewName.isEmpty() && ATTR_ID.equals(attr.getLocalName()) &&
                        ANDROID_URI.equals(attr.getNamespaceURI())) {
                    // Delete attribute
                    IndexedRegion region = (IndexedRegion) attr;
                    int start = region.getStartOffset();
                    int end = region.getEndOffset();
                    edits.add(new ReplaceEdit(start, end - start, ""));
                } else {
                    int start = RefactoringUtil.getAttributeValueRangeStart(attr, document);
                    if (start != -1) {
                        int end = start + match.length();
                        edits.add(new ReplaceEdit(start, end - start, matchedValue));
                    }
                }
            }
        }

        NodeList children = element.getChildNodes();
        for (int i = 0, n = children.getLength(); i < n; i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                addReplacements(edits, (Element) child, document, folderType);
            } else if (child.getNodeType() == Node.TEXT_NODE && mUpdateReferences) {
                // Replace XML text, such as @color/custom_theme_color in
                //    <item name="android:windowBackground">@color/custom_theme_color</item>
                //
                String text = child.getNodeValue();
                int index = getFirstNonBlankIndex(text);
                if (index != -1) {
                    String match = null;
                    String matchedValue = null;
                    if (mXmlMatch1 != null
                            && text.startsWith(mXmlMatch1) && text.trim().equals(mXmlMatch1)) {
                        match = mXmlMatch1;
                        matchedValue = mXmlNewValue1;
                    } else if (mXmlMatch2 != null
                            && text.startsWith(mXmlMatch2) && text.trim().equals(mXmlMatch2)) {
                        match = mXmlMatch2;
                        matchedValue = mXmlNewValue2;
                    } else if (mXmlMatch3 != null
                            && text.startsWith(mXmlMatch3) && text.trim().equals(mXmlMatch3)) {
                        match = mXmlMatch3;
                        matchedValue = mXmlNewValue3;
                    }
                    if (match != null) {
                        IndexedRegion region = (IndexedRegion) child;
                        int start = region.getStartOffset() + index;
                        int end = start + match.length();
                        edits.add(new ReplaceEdit(start, end - start, matchedValue));
                    }
                }
            }
        }
    }

    /**
     * Returns the index of the first non-space character in the string, or -1
     * if the string is empty or has only whitespace
     *
     * @param s the string to check
     * @return the index of the first non whitespace character
     */
    private int getFirstNonBlankIndex(String s) {
        for (int i = 0, n = s.length(); i < n; i++) {
            if (!Character.isWhitespace(s.charAt(i))) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Initiates a renaming of a resource item
     *
     * @param project the project containing the resource references
     * @param type the type of resource
     * @param name the name of the resource
     * @return false if initiating the rename failed
     */
    @Nullable
    private static IField getResourceField(
            @NonNull IProject project,
            @NonNull ResourceType type,
            @NonNull String name) {
        try {
            IJavaProject javaProject = BaseProjectHelper.getJavaProject(project);
            if (javaProject == null) {
                return null;
            }

            String pkg = ManifestInfo.get(project).getPackage();
            // TODO: Rename in all libraries too?
            IType t = javaProject.findType(pkg + '.' + R_CLASS + '.' + type.getName());
            if (t == null) {
                return null;
            }

            return t.getField(name);
        } catch (CoreException e) {
            AdtPlugin.log(e, null);
        }

        return null;
    }

    /**
     * Searches for existing changes in the refactoring which modifies the R
     * field to rename it. it's derived so performing this change will generate
     * a "generated code was modified manually" warning
     */
    private void disableExistingResourceFileChange() {
        IFolder genFolder = mProject.getFolder(SdkConstants.FD_GEN_SOURCES);
        if (genFolder != null && genFolder.exists()) {
            ManifestInfo manifestInfo = ManifestInfo.get(mProject);
            String pkg = manifestInfo.getPackage();
            if (pkg != null) {
                IFile rFile = genFolder.getFile(pkg.replace('.', '/') + '/' + FN_RESOURCE_CLASS);
                TextChange change = getTextChange(rFile);
                if (change != null) {
                    change.setEnabled(false);
                }
            }
        }
    }

    /**
     * Searches for existing changes in the refactoring which modifies the R
     * field to rename it. it's derived so performing this change will generate
     * a "generated code was modified manually" warning
     *
     * @param change the change to disable R file changes in
     */
    public static void disableRClassChanges(Change change) {
        if (change.getName().equals(FN_RESOURCE_CLASS)) {
            change.setEnabled(false);
        }
        // Look for the field change on the R.java class; it's a derived file
        // and will generate file modified manually warnings. Disable it.
        if (change instanceof CompositeChange) {
            for (Change outer : ((CompositeChange) change).getChildren()) {
                disableRClassChanges(outer);
            }
        }
    }
}

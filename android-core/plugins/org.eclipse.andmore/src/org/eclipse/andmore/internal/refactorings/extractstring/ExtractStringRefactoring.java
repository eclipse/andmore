/*
 * Copyright (C) 2009 The Android Open Source Project
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

package org.eclipse.andmore.internal.refactorings.extractstring;

import static com.android.SdkConstants.QUOT_ENTITY;
import static com.android.SdkConstants.STRING_PREFIX;

import com.android.SdkConstants;
import com.android.ide.common.res2.ValueXmlHelper;
import com.android.ide.common.xml.ManifestData;
import com.android.resources.ResourceFolderType;
import com.android.resources.ResourceType;

import org.eclipse.andmore.AdtConstants;
import org.eclipse.andmore.internal.editors.AndroidXmlEditor;
import org.eclipse.andmore.internal.editors.descriptors.AttributeDescriptor;
import org.eclipse.andmore.internal.editors.descriptors.ReferenceAttributeDescriptor;
import org.eclipse.andmore.internal.editors.uimodel.UiAttributeNode;
import org.eclipse.andmore.internal.editors.uimodel.UiElementNode;
import org.eclipse.andmore.internal.project.AndroidManifestHelper;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.ChangeDescriptor;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringChangeDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextEditChangeGroup;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;
import org.eclipse.ui.IEditorPart;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegionList;
import org.eclipse.wst.xml.core.internal.regions.DOMRegionContext;
import org.w3c.dom.Node;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * This refactoring extracts a string from a file and replaces it by an Android resource ID
 * such as R.string.foo.
 * <p/>
 * There are a number of scenarios, which are not all supported yet. The workflow works as
 * such:
 * <ul>
 * <li> User selects a string in a Java and invokes the {@link ExtractStringAction}.
 * <li> The action finds the {@link ICompilationUnit} being edited as well as the current
 *      {@link ITextSelection}. The action creates a new instance of this refactoring as
 *      well as an {@link ExtractStringWizard} and runs the operation.
 * <li> Step 1 of the refactoring is to check the preliminary conditions. Right now we check
 *      that the java source is not read-only and is in sync. We also try to find a string under
 *      the selection. If this fails, the refactoring is aborted.
 * <li> On success, the wizard is shown, which lets the user input the new ID to use.
 * <li> The wizard sets the user input values into this refactoring instance, e.g. the new string
 *      ID, the XML file to update, etc. The wizard does use the utility method
 *      {@link XmlStringFileHelper#valueOfStringId(IProject, String, String)} to check whether
 *      the new ID is already defined in the target XML file.
 * <li> Once Preview or Finish is selected in the wizard, the
 *      {@link #checkFinalConditions(IProgressMonitor)} is called to double-check the user input
 *      and compute the actual changes.
 * <li> When all changes are computed, {@link #createChange(IProgressMonitor)} is invoked.
 * </ul>
 *
 * The list of changes are:
 * <ul>
 * <li> If the target XML does not exist, create it with the new string ID.
 * <li> If the target XML exists, find the <resources> node and add the new string ID right after.
 *      If the node is <resources/>, it needs to be opened.
 * <li> Create an AST rewriter to edit the source Java file and replace all occurrences by the
 *      new computed R.string.foo. Also need to rewrite imports to import R as needed.
 *      If there's already a conflicting R included, we need to insert the FQCN instead.
 * <li> TODO: Have a pref in the wizard: [x] Change other XML Files
 * <li> TODO: Have a pref in the wizard: [x] Change other Java Files
 * </ul>
 */
@SuppressWarnings("restriction")
public class ExtractStringRefactoring extends Refactoring {

    public enum Mode {
        /**
         * the Extract String refactoring is called on an <em>existing</em> source file.
         * Its purpose is then to get the selected string of the source and propose to
         * change it by an XML id. The XML id may be a new one or an existing one.
         */
        EDIT_SOURCE,
        /**
         * The Extract String refactoring is called without any source file.
         * Its purpose is then to create a new XML string ID or select/modify an existing one.
         */
        SELECT_ID,
        /**
         * The Extract String refactoring is called without any source file.
         * Its purpose is then to create a new XML string ID. The ID must not already exist.
         */
        SELECT_NEW_ID
    }

    /** The {@link Mode} of operation of the refactoring. */
    private final Mode mMode;
    /** Non-null when editing an Android Resource XML file: identifies the attribute name
     * of the value being edited. When null, the source is an Android Java file. */
    private String mXmlAttributeName;
    /** The file model being manipulated.
     * Value is null when not on {@link Mode#EDIT_SOURCE} mode. */
    private final IFile mFile;
    /** The editor. Non-null when invoked from {@link ExtractStringAction}. Null otherwise. */
    private final IEditorPart mEditor;
    /** The project that contains {@link #mFile} and that contains the target XML file to modify. */
    private final IProject mProject;
    /** The start of the selection in {@link #mFile}.
     * Value is -1 when not on {@link Mode#EDIT_SOURCE} mode. */
    private final int mSelectionStart;
    /** The end of the selection in {@link #mFile}.
     * Value is -1 when not on {@link Mode#EDIT_SOURCE} mode. */
    private final int mSelectionEnd;

    /** The compilation unit, only defined if {@link #mFile} points to a usable Java source file. */
    private ICompilationUnit mUnit;
    /** The actual string selected, after UTF characters have been escaped, good for display.
     * Value is null when not on {@link Mode#EDIT_SOURCE} mode. */
    private String mTokenString;

    /** The XML string ID selected by the user in the wizard. */
    private String mXmlStringId;
    /** The XML string value. Might be different than the initial selected string. */
    private String mXmlStringValue;
    /** The path of the XML file that will define {@link #mXmlStringId}, selected by the user
     *  in the wizard. This is relative to the project, e.g. "/res/values/string.xml" */
    private String mTargetXmlFileWsPath;
    /** True if we should find & replace in all Java files. */
    private boolean mReplaceAllJava;
    /** True if we should find & replace in all XML files of the same name in other res configs
     * (other than the main {@link #mTargetXmlFileWsPath}.) */
    private boolean mReplaceAllXml;

    /** The list of changes computed by {@link #checkFinalConditions(IProgressMonitor)} and
     *  used by {@link #createChange(IProgressMonitor)}. */
    private ArrayList<Change> mChanges;

    private XmlStringFileHelper mXmlHelper = new XmlStringFileHelper();

    private static final String KEY_MODE = "mode";                      //$NON-NLS-1$
    private static final String KEY_FILE = "file";                      //$NON-NLS-1$
    private static final String KEY_PROJECT = "proj";                   //$NON-NLS-1$
    private static final String KEY_SEL_START = "sel-start";            //$NON-NLS-1$
    private static final String KEY_SEL_END = "sel-end";                //$NON-NLS-1$
    private static final String KEY_TOK_ESC = "tok-esc";                //$NON-NLS-1$
    private static final String KEY_XML_ATTR_NAME = "xml-attr-name";    //$NON-NLS-1$
    private static final String KEY_RPLC_ALL_JAVA = "rplc-all-java";    //$NON-NLS-1$
    private static final String KEY_RPLC_ALL_XML  = "rplc-all-xml";     //$NON-NLS-1$

    /**
     * This constructor is solely used by {@link ExtractStringDescriptor},
     * to replay a previous refactoring.
     * <p/>
     * To create a refactoring from code, please use one of the two other constructors.
     *
     * @param arguments A map previously created using {@link #createArgumentMap()}.
     * @throws NullPointerException
     */
    public ExtractStringRefactoring(Map<String, String> arguments) throws NullPointerException {

        mReplaceAllJava = Boolean.parseBoolean(arguments.get(KEY_RPLC_ALL_JAVA));
        mReplaceAllXml  = Boolean.parseBoolean(arguments.get(KEY_RPLC_ALL_XML));
        mMode = Mode.valueOf(arguments.get(KEY_MODE));

        IPath path = Path.fromPortableString(arguments.get(KEY_PROJECT));
        mProject = (IProject) ResourcesPlugin.getWorkspace().getRoot().findMember(path);

        if (mMode == Mode.EDIT_SOURCE) {
            path = Path.fromPortableString(arguments.get(KEY_FILE));
            mFile = (IFile) ResourcesPlugin.getWorkspace().getRoot().findMember(path);

            mSelectionStart   = Integer.parseInt(arguments.get(KEY_SEL_START));
            mSelectionEnd     = Integer.parseInt(arguments.get(KEY_SEL_END));
            mTokenString      = arguments.get(KEY_TOK_ESC);
            mXmlAttributeName = arguments.get(KEY_XML_ATTR_NAME);
        } else {
            mFile = null;
            mSelectionStart = mSelectionEnd = -1;
            mTokenString = null;
            mXmlAttributeName = null;
        }

        mEditor = null;
    }

    private Map<String, String> createArgumentMap() {
        HashMap<String, String> args = new HashMap<String, String>();
        args.put(KEY_RPLC_ALL_JAVA, Boolean.toString(mReplaceAllJava));
        args.put(KEY_RPLC_ALL_XML,  Boolean.toString(mReplaceAllXml));
        args.put(KEY_MODE,      mMode.name());
        args.put(KEY_PROJECT,   mProject.getFullPath().toPortableString());
        if (mMode == Mode.EDIT_SOURCE) {
            args.put(KEY_FILE,      mFile.getFullPath().toPortableString());
            args.put(KEY_SEL_START, Integer.toString(mSelectionStart));
            args.put(KEY_SEL_END,   Integer.toString(mSelectionEnd));
            args.put(KEY_TOK_ESC,   mTokenString);
            args.put(KEY_XML_ATTR_NAME, mXmlAttributeName);
        }
        return args;
    }

    /**
     * Constructor to use when the Extract String refactoring is called on an
     * *existing* source file. Its purpose is then to get the selected string of
     * the source and propose to change it by an XML id. The XML id may be a new one
     * or an existing one.
     *
     * @param file The source file to process. Cannot be null. File must exist in workspace.
     * @param editor The editor.
     * @param selection The selection in the source file. Cannot be null or empty.
     */
    public ExtractStringRefactoring(IFile file, IEditorPart editor, ITextSelection selection) {
        mMode = Mode.EDIT_SOURCE;
        mFile = file;
        mEditor = editor;
        mProject = file.getProject();
        mSelectionStart = selection.getOffset();
        mSelectionEnd = mSelectionStart + Math.max(0, selection.getLength() - 1);
    }

    /**
     * Constructor to use when the Extract String refactoring is called without
     * any source file. Its purpose is then to create a new XML string ID.
     * <p/>
     * For example this is currently invoked by the ResourceChooser when
     * the user wants to create a new string rather than select an existing one.
     *
     * @param project The project where the target XML file to modify is located. Cannot be null.
     * @param enforceNew If true the XML ID must be a new one.
     *                   If false, an existing ID can be used.
     */
    public ExtractStringRefactoring(IProject project, boolean enforceNew) {
        mMode = enforceNew ? Mode.SELECT_NEW_ID : Mode.SELECT_ID;
        mFile = null;
        mEditor = null;
        mProject = project;
        mSelectionStart = mSelectionEnd = -1;
    }

    /**
     * Sets the replacement string ID. Used by the wizard to set the user input.
     */
    public void setNewStringId(String newStringId) {
        mXmlStringId = newStringId;
    }

    /**
     * Sets the replacement string ID. Used by the wizard to set the user input.
     */
    public void setNewStringValue(String newStringValue) {
        mXmlStringValue = newStringValue;
    }

    /**
     * Sets the target file. This is a project path, e.g. "/res/values/strings.xml".
     * Used by the wizard to set the user input.
     */
    public void setTargetFile(String targetXmlFileWsPath) {
        mTargetXmlFileWsPath = targetXmlFileWsPath;
    }

    public void setReplaceAllJava(boolean replaceAllJava) {
        mReplaceAllJava = replaceAllJava;
    }

    public void setReplaceAllXml(boolean replaceAllXml) {
        mReplaceAllXml = replaceAllXml;
    }

    /**
     * @see org.eclipse.ltk.core.refactoring.Refactoring#getName()
     */
    @Override
    public String getName() {
        if (mMode == Mode.SELECT_ID) {
            return "Create or Use Android String";
        } else if (mMode == Mode.SELECT_NEW_ID) {
            return "Create New Android String";
        }

        return "Extract Android String";
    }

    public Mode getMode() {
        return mMode;
    }

    /**
     * Gets the actual string selected, after UTF characters have been escaped,
     * good for display. Value can be null.
     */
    public String getTokenString() {
        return mTokenString;
    }

    /** Returns the XML string ID selected by the user in the wizard. */
    public String getXmlStringId() {
        return mXmlStringId;
    }

    /**
     * Step 1 of 3 of the refactoring:
     * Checks that the current selection meets the initial condition before the ExtractString
     * wizard is shown. The check is supposed to be lightweight and quick. Note that at that
     * point the wizard has not been created yet.
     * <p/>
     * Here we scan the source buffer to find the token matching the selection.
     * The check is successful is a Java string literal is selected, the source is in sync
     * and is not read-only.
     * <p/>
     * This is also used to extract the string to be modified, so that we can display it in
     * the refactoring wizard.
     *
     * @see org.eclipse.ltk.core.refactoring.Refactoring#checkInitialConditions(org.eclipse.core.runtime.IProgressMonitor)
     *
     * @throws CoreException
     */
    @Override
    public RefactoringStatus checkInitialConditions(IProgressMonitor monitor)
            throws CoreException, OperationCanceledException {

        mUnit = null;
        mTokenString = null;

        RefactoringStatus status = new RefactoringStatus();

        try {
            monitor.beginTask("Checking preconditions...", 6);

            if (mMode != Mode.EDIT_SOURCE) {
                monitor.worked(6);
                return status;
            }

            if (!checkSourceFile(mFile, status, monitor)) {
                return status;
            }

            // Try to get a compilation unit from this file. If it fails, mUnit is null.
            try {
                mUnit = JavaCore.createCompilationUnitFrom(mFile);

                // Make sure the unit is not read-only, e.g. it's not a class file or inside a Jar
                if (mUnit.isReadOnly()) {
                    status.addFatalError("The file is read-only, please make it writeable first.");
                    return status;
                }

                // This is a Java file. Check if it contains the selection we want.
                if (!findSelectionInJavaUnit(mUnit, status, monitor)) {
                    return status;
                }

            } catch (Exception e) {
                // That was not a Java file. Ignore.
            }

            if (mUnit != null) {
                monitor.worked(1);
                return status;
            }

            // Check this a Layout XML file and get the selection and its context.
            if (mFile != null && SdkConstants.EXT_XML.equals(mFile.getFileExtension())) {

                // Currently we only support Android resource XML files, so they must have a path
                // similar to
                //    project/res/<type>[-<configuration>]/*.xml
                //    project/AndroidManifest.xml
                // There is no support for sub folders, so the segment count must be 4 or 2.
                // We don't need to check the type folder name because a/ we only accept
                // an AndroidXmlEditor source and b/ aapt generates a compilation error for
                // unknown folders.

                IPath path = mFile.getFullPath();
                if ((path.segmentCount() == 4 &&
                     path.segment(1).equalsIgnoreCase(SdkConstants.FD_RESOURCES)) ||
                    (path.segmentCount() == 2 &&
                     path.segment(1).equalsIgnoreCase(SdkConstants.FN_ANDROID_MANIFEST_XML))) {
                    if (!findSelectionInXmlFile(mFile, status, monitor)) {
                        return status;
                    }
                }
            }

            if (!status.isOK()) {
                status.addFatalError(
                        "Selection must be inside a Java source or an Android Layout XML file.");
            }

        } finally {
            monitor.done();
        }

        return status;
    }

    /**
     * Try to find the selected Java element in the compilation unit.
     *
     * If selection matches a string literal, capture it, otherwise add a fatal error
     * to the status.
     *
     * On success, advance the monitor by 3.
     * Returns status.isOK().
     */
    private boolean findSelectionInJavaUnit(ICompilationUnit unit,
            RefactoringStatus status, IProgressMonitor monitor) {
        try {
            IBuffer buffer = unit.getBuffer();

            IScanner scanner = ToolFactory.createScanner(
                    false, //tokenizeComments
                    false, //tokenizeWhiteSpace
                    false, //assertMode
                    false  //recordLineSeparator
                    );
            scanner.setSource(buffer.getCharacters());
            monitor.worked(1);

            for(int token = scanner.getNextToken();
                    token != ITerminalSymbols.TokenNameEOF;
                    token = scanner.getNextToken()) {
                if (scanner.getCurrentTokenStartPosition() <= mSelectionStart &&
                        scanner.getCurrentTokenEndPosition() >= mSelectionEnd) {
                    // found the token, but only keep if the right type
                    if (token == ITerminalSymbols.TokenNameStringLiteral) {
                        mTokenString = new String(scanner.getCurrentTokenSource());
                    }
                    break;
                } else if (scanner.getCurrentTokenStartPosition() > mSelectionEnd) {
                    // scanner is past the selection, abort.
                    break;
                }
            }
        } catch (JavaModelException e1) {
            // Error in unit.getBuffer. Ignore.
        } catch (InvalidInputException e2) {
            // Error in scanner.getNextToken. Ignore.
        } finally {
            monitor.worked(1);
        }

        if (mTokenString != null) {
            // As a literal string, the token should have surrounding quotes. Remove them.
            // Note: unquoteAttrValue technically removes either " or ' paired quotes, whereas
            // the Java token should only have " quotes. Since we know the type to be a string
            // literal, there should be no confusion here.
            mTokenString = unquoteAttrValue(mTokenString);

            // We need a non-empty string literal
            if (mTokenString.length() == 0) {
                mTokenString = null;
            }
        }

        if (mTokenString == null) {
            status.addFatalError("Please select a Java string literal.");
        }

        monitor.worked(1);
        return status.isOK();
    }

    /**
     * Try to find the selected XML element. This implementation replies on the refactoring
     * originating from an Android Layout Editor. We rely on some internal properties of the
     * Structured XML editor to retrieve file content to avoid parsing it again. We also rely
     * on our specific Android XML model to get element & attribute descriptor properties.
     *
     * If selection matches a string literal, capture it, otherwise add a fatal error
     * to the status.
     *
     * On success, advance the monitor by 1.
     * Returns status.isOK().
     */
    private boolean findSelectionInXmlFile(IFile file,
            RefactoringStatus status,
            IProgressMonitor monitor) {

        try {
            if (!(mEditor instanceof AndroidXmlEditor)) {
                status.addFatalError("Only the Android XML Editor is currently supported.");
                return status.isOK();
            }

            AndroidXmlEditor editor = (AndroidXmlEditor) mEditor;
            IStructuredModel smodel = null;
            Node node = null;
            String currAttrName = null;

            try {
                // See the portability note in AndroidXmlEditor#getModelForRead() javadoc.
                smodel = editor.getModelForRead();
                if (smodel != null) {
                    // The structured model gives the us the actual XML Node element where the
                    // offset is. By using this Node, we can find the exact UiElementNode of our
                    // model and thus we'll be able to get the properties of the attribute -- to
                    // check if it accepts a string reference. This does not however tell us if
                    // the selection is actually in an attribute value, nor which attribute is
                    // being edited.
                    for(int offset = mSelectionStart; offset >= 0 && node == null; --offset) {
                        node = (Node) smodel.getIndexedRegion(offset);
                    }

                    if (node == null) {
                        status.addFatalError(
                                "The selection does not match any element in the XML document.");
                        return status.isOK();
                    }

                    if (node.getNodeType() != Node.ELEMENT_NODE) {
                        status.addFatalError("The selection is not inside an actual XML element.");
                        return status.isOK();
                    }

                    IStructuredDocument sdoc = smodel.getStructuredDocument();
                    if (sdoc != null) {
                        // Portability note: all the structured document implementation is
                        // under wst.sse.core.internal.provisional so we can expect it to change in
                        // a distant future if they start cleaning their codebase, however unlikely
                        // that is.

                        int selStart = mSelectionStart;
                        IStructuredDocumentRegion region =
                            sdoc.getRegionAtCharacterOffset(selStart);
                        if (region != null &&
                                DOMRegionContext.XML_TAG_NAME.equals(region.getType())) {
                            // Find if any sub-region representing an attribute contains the
                            // selection. If it does, returns the name of the attribute in
                            // currAttrName and returns the value in the field mTokenString.
                            currAttrName = findSelectionInRegion(region, selStart);

                            if (mTokenString == null) {
                                status.addFatalError(
                                    "The selection is not inside an actual XML attribute value.");
                            }
                        }
                    }

                    if (mTokenString != null && node != null && currAttrName != null) {

                        // Validate that the attribute accepts a string reference.
                        // This sets mTokenString to null by side-effect when it fails and
                        // adds a fatal error to the status as needed.
                        validateSelectedAttribute(editor, node, currAttrName, status);

                    } else {
                        // We shouldn't get here: we're missing one of the token string, the node
                        // or the attribute name. All of them have been checked earlier so don't
                        // set any specific error.
                        mTokenString = null;
                    }
                }
            } catch (Throwable t) {
                // Since we use some internal APIs, use a broad catch-all to report any
                // unexpected issue rather than crash the whole refactoring.
                status.addFatalError(
                        String.format("XML parsing error: %1$s", t.getMessage()));
            } finally {
                if (smodel != null) {
                    smodel.releaseFromRead();
                }
            }

        } finally {
            monitor.worked(1);
        }

        return status.isOK();
    }

    /**
     * The region gives us the textual representation of the XML element
     * where the selection starts, split using sub-regions. We now just
     * need to iterate through the sub-regions to find which one
     * contains the actual selection. We're interested in an attribute
     * value however when we find one we want to memorize the attribute
     * name that was defined just before.
     *
     * @return When the cursor is on a valid attribute name or value, returns the string of
     * attribute name. As a side-effect, returns the value of the attribute in {@link #mTokenString}
     */
    private String findSelectionInRegion(IStructuredDocumentRegion region, int selStart) {

        String currAttrName = null;

        int startInRegion = selStart - region.getStartOffset();

        int nb = region.getNumberOfRegions();
        ITextRegionList list = region.getRegions();
        String currAttrValue = null;

        for (int i = 0; i < nb; i++) {
            ITextRegion subRegion = list.get(i);
            String type = subRegion.getType();

            if (DOMRegionContext.XML_TAG_ATTRIBUTE_NAME.equals(type)) {
                currAttrName = region.getText(subRegion);

                // I like to select the attribute definition and invoke
                // the extract string wizard. So if the selection is on
                // the attribute name part, find the value that is just
                // after and use it as if it were the selection.

                if (subRegion.getStart() <= startInRegion &&
                        startInRegion < subRegion.getTextEnd()) {
                    // A well-formed attribute is composed of a name,
                    // an equal sign and the value. There can't be any space
                    // in between, which makes the parsing a lot easier.
                    if (i <= nb - 3 &&
                            DOMRegionContext.XML_TAG_ATTRIBUTE_EQUALS.equals(
                                                   list.get(i + 1).getType())) {
                        subRegion = list.get(i + 2);
                        type = subRegion.getType();
                        if (DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE.equals(
                                type)) {
                            currAttrValue = region.getText(subRegion);
                        }
                    }
                }

            } else if (subRegion.getStart() <= startInRegion &&
                    startInRegion < subRegion.getTextEnd() &&
                    DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE.equals(type)) {
                currAttrValue = region.getText(subRegion);
            }

            if (currAttrValue != null) {
                // We found the value. Only accept it if not empty
                // and if we found an attribute name before.
                String text = currAttrValue;

                // The attribute value contains XML quotes. Remove them.
                text = unquoteAttrValue(text);
                if (text.length() > 0 && currAttrName != null) {
                    // Setting mTokenString to non-null marks the fact we
                    // accept this attribute.
                    mTokenString = text;
                }

                break;
            }
        }

        return currAttrName;
    }

    /**
     * Attribute values found as text for {@link DOMRegionContext#XML_TAG_ATTRIBUTE_VALUE}
     * contain XML quotes. This removes the quotes (either single or double quotes).
     *
     * @param attrValue The attribute value, as extracted by
     *                  {@link IStructuredDocumentRegion#getText(ITextRegion)}.
     *                  Must not be null.
     * @return The attribute value, without quotes. Whitespace is not trimmed, if any.
     *         String may be empty, but not null.
     */
    static String unquoteAttrValue(String attrValue) {
        int len = attrValue.length();
        int len1 = len - 1;
        if (len >= 2 &&
                attrValue.charAt(0) == '"' &&
                attrValue.charAt(len1) == '"') {
            attrValue = attrValue.substring(1, len1);
        } else if (len >= 2 &&
                attrValue.charAt(0) == '\'' &&
                attrValue.charAt(len1) == '\'') {
            attrValue = attrValue.substring(1, len1);
        }

        return attrValue;
    }

    /**
     * Validates that the attribute accepts a string reference.
     * This sets mTokenString to null by side-effect when it fails and
     * adds a fatal error to the status as needed.
     */
    private void validateSelectedAttribute(AndroidXmlEditor editor, Node node,
            String attrName, RefactoringStatus status) {
        UiElementNode rootUiNode = editor.getUiRootNode();
        UiElementNode currentUiNode =
            rootUiNode == null ? null : rootUiNode.findXmlNode(node);
        ReferenceAttributeDescriptor attrDesc = null;

        if (currentUiNode != null) {
            // remove any namespace prefix from the attribute name
            String name = attrName;
            int pos = name.indexOf(':');
            if (pos > 0 && pos < name.length() - 1) {
                name = name.substring(pos + 1);
            }

            for (UiAttributeNode attrNode : currentUiNode.getAllUiAttributes()) {
                if (attrNode.getDescriptor().getXmlLocalName().equals(name)) {
                    AttributeDescriptor desc = attrNode.getDescriptor();
                    if (desc instanceof ReferenceAttributeDescriptor) {
                        attrDesc = (ReferenceAttributeDescriptor) desc;
                    }
                    break;
                }
            }
        }

        // The attribute descriptor is a resource reference. It must either accept
        // of any resource type or specifically accept string types.
        if (attrDesc != null &&
                (attrDesc.getResourceType() == null ||
                 attrDesc.getResourceType() == ResourceType.STRING)) {
            // We have one more check to do: is the current string value already
            // an Android XML string reference? If so, we can't edit it.
            if (mTokenString != null && mTokenString.startsWith("@")) {                             //$NON-NLS-1$
                int pos1 = 0;
                if (mTokenString.length() > 1 && mTokenString.charAt(1) == '+') {
                    pos1++;
                }
                int pos2 = mTokenString.indexOf('/');
                if (pos2 > pos1) {
                    String kind = mTokenString.substring(pos1 + 1, pos2);
                    if (ResourceType.STRING.getName().equals(kind)) {
                        mTokenString = null;
                        status.addFatalError(String.format(
                                "The attribute %1$s already contains a %2$s reference.",
                                attrName,
                                kind));
                    }
                }
            }

            if (mTokenString != null) {
                // We're done with all our checks. mTokenString contains the
                // current attribute value. We don't memorize the region nor the
                // attribute, however we memorize the textual attribute name so
                // that we can offer replacement for all its occurrences.
                mXmlAttributeName = attrName;
            }

        } else {
            mTokenString = null;
            status.addFatalError(String.format(
                    "The attribute %1$s does not accept a string reference.",
                    attrName));
        }
    }

    /**
     * Tests from org.eclipse.jdt.internal.corext.refactoringChecks#validateEdit()
     * Might not be useful.
     *
     * On success, advance the monitor by 2.
     *
     * @return False if caller should abort, true if caller should continue.
     */
    private boolean checkSourceFile(IFile file,
            RefactoringStatus status,
            IProgressMonitor monitor) {
        // check whether the source file is in sync
        if (!file.isSynchronized(IResource.DEPTH_ZERO)) {
            status.addFatalError("The file is not synchronized. Please save it first.");
            return false;
        }
        monitor.worked(1);

        // make sure we can write to it.
        ResourceAttributes resAttr = file.getResourceAttributes();
        if (resAttr == null || resAttr.isReadOnly()) {
            status.addFatalError("The file is read-only, please make it writeable first.");
            return false;
        }
        monitor.worked(1);

        return true;
    }

    /**
     * Step 2 of 3 of the refactoring:
     * Check the conditions once the user filled values in the refactoring wizard,
     * then prepare the changes to be applied.
     * <p/>
     * In this case, most of the sanity checks are done by the wizard so essentially this
     * should only be called if the wizard positively validated the user input.
     *
     * Here we do check that the target resource XML file either does not exists or
     * is not read-only.
     *
     * @see org.eclipse.ltk.core.refactoring.Refactoring#checkFinalConditions(IProgressMonitor)
     *
     * @throws CoreException
     */
    @Override
    public RefactoringStatus checkFinalConditions(IProgressMonitor monitor)
            throws CoreException, OperationCanceledException {
        RefactoringStatus status = new RefactoringStatus();

        try {
            monitor.beginTask("Checking post-conditions...", 5);

            if (mXmlStringId == null || mXmlStringId.length() <= 0) {
                // this is not supposed to happen
                status.addFatalError("Missing replacement string ID");
            } else if (mTargetXmlFileWsPath == null || mTargetXmlFileWsPath.length() <= 0) {
                // this is not supposed to happen
                status.addFatalError("Missing target xml file path");
            }
            monitor.worked(1);

            // Either that resource must not exist or it must be a writable file.
            IResource targetXml = getTargetXmlResource(mTargetXmlFileWsPath);
            if (targetXml != null) {
                if (targetXml.getType() != IResource.FILE) {
                    status.addFatalError(
                            String.format("XML file '%1$s' is not a file.", mTargetXmlFileWsPath));
                } else {
                    ResourceAttributes attr = targetXml.getResourceAttributes();
                    if (attr != null && attr.isReadOnly()) {
                        status.addFatalError(
                                String.format("XML file '%1$s' is read-only.",
                                        mTargetXmlFileWsPath));
                    }
                }
            }
            monitor.worked(1);

            if (status.hasError()) {
                return status;
            }

            mChanges = new ArrayList<Change>();


            // Prepare the change to create/edit the String ID in the res/values XML file.
            if (!mXmlStringValue.equals(
                    mXmlHelper.valueOfStringId(mProject, mTargetXmlFileWsPath, mXmlStringId))) {
                // We actually change it only if the ID doesn't exist yet or has a different value
                Change change = createXmlChanges((IFile) targetXml, mXmlStringId, mXmlStringValue,
                        status, SubMonitor.convert(monitor, 1));
                if (change != null) {
                    mChanges.add(change);
                }
            }

            if (status.hasError()) {
                return status;
            }

            if (mMode == Mode.EDIT_SOURCE) {
                List<Change> changes = null;
                if (mXmlAttributeName != null) {
                    // Prepare the change to the Android resource XML file
                    changes = computeXmlSourceChanges(mFile,
                            mXmlStringId,
                            mTokenString,
                            mXmlAttributeName,
                            true, // allConfigurations
                            status,
                            monitor);

                } else if (mUnit != null) {
                    // Prepare the change to the Java compilation unit
                    changes = computeJavaChanges(mUnit, mXmlStringId, mTokenString,
                            status, SubMonitor.convert(monitor, 1));
                }
                if (changes != null) {
                    mChanges.addAll(changes);
                }
            }

            if (mReplaceAllJava) {
                String currentIdentifier = mUnit != null ? mUnit.getHandleIdentifier() : ""; //$NON-NLS-1$

                SubMonitor submon = SubMonitor.convert(monitor, 1);
                for (ICompilationUnit unit : findAllJavaUnits()) {
                    // Only process Java compilation units that exist, are not derived
                    // and are not read-only.
                    if (unit == null || !unit.exists()) {
                        continue;
                    }
                    IResource resource = unit.getResource();
                    if (resource == null || resource.isDerived()) {
                        continue;
                    }

                    // Ensure that we don't process the current compilation unit (processed
                    // as mUnit above) twice
                    if (currentIdentifier.equals(unit.getHandleIdentifier())) {
                        continue;
                    }

                    ResourceAttributes attrs = resource.getResourceAttributes();
                    if (attrs != null && attrs.isReadOnly()) {
                        continue;
                    }

                    List<Change> changes = computeJavaChanges(
                            unit, mXmlStringId, mTokenString,
                            status, SubMonitor.convert(submon, 1));
                    if (changes != null) {
                        mChanges.addAll(changes);
                    }
                }
            }

            if (mReplaceAllXml) {
                SubMonitor submon = SubMonitor.convert(monitor, 1);
                for (IFile xmlFile : findAllResXmlFiles()) {
                    if (xmlFile != null) {
                        List<Change> changes = computeXmlSourceChanges(xmlFile,
                                mXmlStringId,
                                mTokenString,
                                mXmlAttributeName,
                                false, // allConfigurations
                                status,
                                SubMonitor.convert(submon, 1));
                        if (changes != null) {
                            mChanges.addAll(changes);
                        }
                    }
                }
            }

            monitor.worked(1);
        } finally {
            monitor.done();
        }

        return status;
    }

    // --- XML changes ---

    /**
     * Returns a foreach-compatible iterator over all XML files in the project's
     * /res folder, excluding the target XML file (the one where we'll write/edit
     * the string id).
     */
    private Iterable<IFile> findAllResXmlFiles() {
        return new Iterable<IFile>() {
            @Override
            public Iterator<IFile> iterator() {
                return new Iterator<IFile>() {
                    final Queue<IFile> mFiles = new LinkedList<IFile>();
                    final Queue<IResource> mFolders = new LinkedList<IResource>();
                    IPath mFilterPath1 = null;
                    IPath mFilterPath2 = null;
                    {
                        // Filter out the XML file where we'll be writing the XML string id.
                        IResource filterRes = mProject.findMember(mTargetXmlFileWsPath);
                        if (filterRes != null) {
                            mFilterPath1 = filterRes.getFullPath();
                        }
                        // Filter out the XML source file, if any (e.g. typically a layout)
                        if (mFile != null) {
                            mFilterPath2 = mFile.getFullPath();
                        }

                        // We want to process the manifest
                        IResource man = mProject.findMember("AndroidManifest.xml"); // TODO find a constant
                        if (man.exists() && man instanceof IFile && !man.equals(mFile)) {
                            mFiles.add((IFile) man);
                        }

                        // Add all /res folders (technically we don't need to process /res/values
                        // XML files that contain resources/string elements, but it's easier to
                        // not filter them out.)
                        IFolder f = mProject.getFolder(AdtConstants.WS_RESOURCES);
                        if (f.exists()) {
                            try {
                                mFolders.addAll(
                                        Arrays.asList(f.members(IContainer.EXCLUDE_DERIVED)));
                            } catch (CoreException e) {
                                // pass
                            }
                        }
                    }

                    @Override
                    public boolean hasNext() {
                        if (!mFiles.isEmpty()) {
                            return true;
                        }

                        while (!mFolders.isEmpty()) {
                            IResource res = mFolders.poll();
                            if (res.exists() && res instanceof IFolder) {
                                IFolder f = (IFolder) res;
                                try {
                                    getFileList(f);
                                    if (!mFiles.isEmpty()) {
                                        return true;
                                    }
                                } catch (CoreException e) {
                                    // pass
                                }
                            }
                        }
                        return false;
                    }

                    private void getFileList(IFolder folder) throws CoreException {
                        for (IResource res : folder.members(IContainer.EXCLUDE_DERIVED)) {
                            // Only accept file resources which are not derived and actually exist
                            if (res.exists() && !res.isDerived() && res instanceof IFile) {
                                IFile file = (IFile) res;
                                // Must have an XML extension
                                if (SdkConstants.EXT_XML.equals(file.getFileExtension())) {
                                    IPath p = file.getFullPath();
                                    // And not be either paths we want to filter out
                                    if ((mFilterPath1 != null && mFilterPath1.equals(p)) ||
                                            (mFilterPath2 != null && mFilterPath2.equals(p))) {
                                        continue;
                                    }
                                    mFiles.add(file);
                                }
                            }
                        }
                    }

                    @Override
                    public IFile next() {
                        IFile file = mFiles.poll();
                        hasNext();
                        return file;
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException(
                            "This iterator does not support removal");  //$NON-NLS-1$
                    }
                };
            }
        };
    }

    /**
     * Internal helper that actually prepares the {@link Change} that adds the given
     * ID to the given XML File.
     * <p/>
     * This does not actually modify the file.
     *
     * @param targetXml The file resource to modify.
     * @param xmlStringId The new ID to insert.
     * @param tokenString The old string, which will be the value in the XML string.
     * @return A new {@link TextEdit} that describes how to change the file.
     */
    private Change createXmlChanges(IFile targetXml,
            String xmlStringId,
            String tokenString,
            RefactoringStatus status,
            SubMonitor monitor) {

        TextFileChange xmlChange = new TextFileChange(getName(), targetXml);
        xmlChange.setTextType(SdkConstants.EXT_XML);

        String error = "";                  //$NON-NLS-1$
        TextEdit edit = null;
        TextEditGroup editGroup = null;

        try {
            if (!targetXml.exists()) {
                // Kludge: use targetXml==null as a signal this is a new file being created
                targetXml = null;
            }

            edit = createXmlReplaceEdit(targetXml, xmlStringId, tokenString, status,
                    SubMonitor.convert(monitor, 1));
        } catch (IOException e) {
            error = e.toString();
        } catch (CoreException e) {
            // Failed to read file. Ignore. Will handle error below.
            error = e.toString();
        }

        if (edit == null) {
            status.addFatalError(String.format("Failed to modify file %1$s%2$s",
                    targetXml == null ? "" : targetXml.getFullPath(),   //$NON-NLS-1$
                    error == null ? "" : ": " + error));                //$NON-NLS-1$
            return null;
        }

        editGroup = new TextEditGroup(targetXml == null ? "Create <string> in new XML file"
                                                        : "Insert <string> in XML file",
                                      edit);

        xmlChange.setEdit(edit);
        // The TextEditChangeGroup let the user toggle this change on and off later.
        xmlChange.addTextEditChangeGroup(new TextEditChangeGroup(xmlChange, editGroup));

        monitor.worked(1);
        return xmlChange;
    }

    /**
     * Scan the XML file to find the best place where to insert the new string element.
     * <p/>
     * This handles a variety of cases, including replacing existing ids in place,
     * adding the top resources element if missing and the XML PI if not present.
     * It tries to preserve indentation when adding new elements at the end of an existing XML.
     *
     * @param file The XML file to modify, that must be present in the workspace.
     *             Pass null to create a change for a new file that doesn't exist yet.
     * @param xmlStringId The new ID to insert.
     * @param tokenString The old string, which will be the value in the XML string.
     * @param status The in-out refactoring status. Used to log a more detailed error if the
     *          XML has a top element that is not a resources element.
     * @param monitor A monitor to track progress.
     * @return A new {@link TextEdit} for either a replace or an insert operation, or null in case
     *          of error.
     * @throws CoreException - if the file's contents or description can not be read.
     * @throws IOException   - if the file's contents can not be read or its detected encoding does
     *                         not support its contents.
     */
    private TextEdit createXmlReplaceEdit(IFile file,
            String xmlStringId,
            String tokenString,
            RefactoringStatus status,
            SubMonitor monitor)
                throws IOException, CoreException {

        IModelManager modelMan = StructuredModelManager.getModelManager();

        final String NODE_RESOURCES = SdkConstants.TAG_RESOURCES;
        final String NODE_STRING = SdkConstants.TAG_STRING;
        final String ATTR_NAME = SdkConstants.ATTR_NAME;


        // Scan the source to find the best insertion point.

        // 1- The most common case we need to handle is the one of inserting at the end
        //    of a valid XML document, respecting the whitespace last used.
        //
        // Ideally we have this structure:
        // <xml ...>
        // <resource>
        // ...ws1...<string>blah</string>...ws2...
        // </resource>
        //
        // where ws1 and ws2 are the whitespace respectively before and after the last element
        // just before the closing </resource>.
        // In this case we want to generate the new string just before ws2...</resource> with
        // the same whitespace as ws1.
        //
        // 2- Another expected case is there's already an existing string which "name" attribute
        //    equals to xmlStringId and we just want to replace its value.
        //
        // Other cases we need to handle:
        // 3- There is no element at all -> create a full new <resource>+<string> content.
        // 4- There is <resource/>, that is the tag is not opened. This can be handled as the
        //    previous case, generating full content but also replacing <resource/>.
        // 5- There is a top element that is not <resource>. That's a fatal error and we abort.

        IStructuredModel smodel = null;

        // Single and double quotes must be escaped in the <string>value</string> declaration
        tokenString = ValueXmlHelper.escapeResourceString(tokenString);

        try {
            IStructuredDocument sdoc = null;
            boolean checkTopElement = true;
            boolean replaceStringContent = false;
            boolean hasPiXml = false;
            int newResStart = 0;
            int newResLength = 0;
            String lineSep = "\n";                  //$NON-NLS-1$

            if (file != null) {
                smodel = modelMan.getExistingModelForRead(file);
                if (smodel != null) {
                    sdoc = smodel.getStructuredDocument();
                } else if (smodel == null) {
                    // The model is not currently open.
                    if (file.exists()) {
                        sdoc = modelMan.createStructuredDocumentFor(file);
                    } else {
                        sdoc = modelMan.createNewStructuredDocumentFor(file);
                    }
                }
            }

            if (sdoc == null && file != null) {
                // Get a document matching the actual saved file
                sdoc = modelMan.createStructuredDocumentFor(file);
            }

            if (sdoc != null) {
                String wsBefore = "";   //$NON-NLS-1$
                String lastWs = null;

                lineSep = sdoc.getLineDelimiter();
                if (lineSep == null || lineSep.length() == 0) {
                    // That wasn't too useful, let's go back to a reasonable default
                    lineSep = "\n"; //$NON-NLS-1$
                }

                for (IStructuredDocumentRegion regions : sdoc.getStructuredDocumentRegions()) {
                    String type = regions.getType();

                    if (DOMRegionContext.XML_CONTENT.equals(type)) {

                        if (replaceStringContent) {
                            // Generate a replacement for a <string> value matching the string ID.
                            return new ReplaceEdit(
                                    regions.getStartOffset(), regions.getLength(), tokenString);
                        }

                        // Otherwise capture what should be whitespace content
                        lastWs = regions.getFullText();
                        continue;

                    } else if (DOMRegionContext.XML_PI_OPEN.equals(type) && !hasPiXml) {

                        int nb = regions.getNumberOfRegions();
                        ITextRegionList list = regions.getRegions();
                        for (int i = 0; i < nb; i++) {
                            ITextRegion region = list.get(i);
                            type = region.getType();
                            if (DOMRegionContext.XML_TAG_NAME.equals(type)) {
                                String name = regions.getText(region);
                                if ("xml".equals(name)) {   //$NON-NLS-1$
                                    hasPiXml = true;
                                    break;
                                }
                            }
                        }
                        continue;

                    } else if (!DOMRegionContext.XML_TAG_NAME.equals(type)) {
                        // ignore things which are not a tag nor text content (such as comments)
                        continue;
                    }

                    int nb = regions.getNumberOfRegions();
                    ITextRegionList list = regions.getRegions();

                    String name = null;
                    String attrName = null;
                    String attrValue = null;
                    boolean isEmptyTag = false;
                    boolean isCloseTag = false;

                    for (int i = 0; i < nb; i++) {
                        ITextRegion region = list.get(i);
                        type = region.getType();

                        if (DOMRegionContext.XML_END_TAG_OPEN.equals(type)) {
                            isCloseTag = true;
                        } else if (DOMRegionContext.XML_EMPTY_TAG_CLOSE.equals(type)) {
                            isEmptyTag = true;
                        } else if (DOMRegionContext.XML_TAG_NAME.equals(type)) {
                            name = regions.getText(region);
                        } else if (DOMRegionContext.XML_TAG_ATTRIBUTE_NAME.equals(type) &&
                                NODE_STRING.equals(name)) {
                            // Record the attribute names into a <string> element.
                            attrName = regions.getText(region);
                        } else if (DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE.equals(type) &&
                                ATTR_NAME.equals(attrName)) {
                            // Record the value of a <string name=...> attribute
                            attrValue = regions.getText(region);

                            if (attrValue != null &&
                                    unquoteAttrValue(attrValue).equals(xmlStringId)) {
                                // We found a <string name=> matching the string ID to replace.
                                // We'll generate a replacement when we process the string value
                                // (that is the next XML_CONTENT region.)
                                replaceStringContent = true;
                            }
                        }
                    }

                    if (checkTopElement) {
                        // Check the top element has a resource name
                        checkTopElement = false;
                        if (!NODE_RESOURCES.equals(name)) {
                            status.addFatalError(
                                    String.format("XML file lacks a <resource> tag: %1$s",
                                            mTargetXmlFileWsPath));
                            return null;

                        }

                        if (isEmptyTag) {
                            // The top element is an empty "<resource/>" tag. We need to do
                            // a full new resource+string replacement.
                            newResStart = regions.getStartOffset();
                            newResLength = regions.getLength();
                        }
                    }

                    if (NODE_RESOURCES.equals(name)) {
                        if (isCloseTag) {
                            // We found the </resource> tag and we want
                            // to insert just before this one.

                            StringBuilder content = new StringBuilder();
                            content.append(wsBefore)
                                   .append("<string name=\"")                   //$NON-NLS-1$
                                   .append(xmlStringId)
                                   .append("\">")                               //$NON-NLS-1$
                                   .append(tokenString)
                                   .append("</string>");                        //$NON-NLS-1$

                            // Backup to insert before the whitespace preceding </resource>
                            IStructuredDocumentRegion insertBeforeReg = regions;
                            while (true) {
                                IStructuredDocumentRegion previous = insertBeforeReg.getPrevious();
                                if (previous != null &&
                                        DOMRegionContext.XML_CONTENT.equals(previous.getType()) &&
                                        previous.getText().trim().length() == 0) {
                                    insertBeforeReg = previous;
                                } else {
                                    break;
                                }
                            }
                            if (insertBeforeReg == regions) {
                                // If we have not found any whitespace before </resources>,
                                // at least add a line separator.
                                content.append(lineSep);
                            }

                            return new InsertEdit(insertBeforeReg.getStartOffset(),
                                                  content.toString());
                        }
                    } else {
                        // For any other tag than <resource>, capture whitespace before and after.
                        if (!isCloseTag) {
                            wsBefore = lastWs;
                        }
                    }
                }
            }

            // We reach here either because there's no XML content at all or because
            // there's an empty <resource/>.
            // Provide a full new resource+string replacement.
            StringBuilder content = new StringBuilder();
            if (!hasPiXml) {
                content.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>"); //$NON-NLS-1$
                content.append(lineSep);
            } else if (newResLength == 0 && sdoc != null) {
                // If inserting at the end, check if the last region is some whitespace.
                // If there's no newline, insert one ourselves.
                IStructuredDocumentRegion lastReg = sdoc.getLastStructuredDocumentRegion();
                if (lastReg != null && lastReg.getText().indexOf('\n') == -1) {
                    content.append('\n');
                }
            }

            // FIXME how to access formatting preferences to generate the proper indentation?
            content.append("<resources>").append(lineSep);                  //$NON-NLS-1$
            content.append("    <string name=\"")                           //$NON-NLS-1$
                   .append(xmlStringId)
                   .append("\">")                                           //$NON-NLS-1$
                   .append(tokenString)
                   .append("</string>")                                     //$NON-NLS-1$
                   .append(lineSep);
            content.append("</resources>").append(lineSep);                 //$NON-NLS-1$

            if (newResLength > 0) {
                // Replace existing piece
                return new ReplaceEdit(newResStart, newResLength, content.toString());
            } else {
                // Insert at the end.
                int offset = sdoc == null ? 0 : sdoc.getLength();
                return new InsertEdit(offset, content.toString());
            }
        } catch (IOException e) {
            // This is expected to happen and is properly reported to the UI.
            throw e;
        } catch (CoreException e) {
            // This is expected to happen and is properly reported to the UI.
            throw e;
        } catch (Throwable t) {
            // Since we use some internal APIs, use a broad catch-all to report any
            // unexpected issue rather than crash the whole refactoring.
            status.addFatalError(
                    String.format("XML replace error: %1$s", t.getMessage()));
        } finally {
            if (smodel != null) {
                smodel.releaseFromRead();
            }
        }

        return null;
    }

    /**
     * Computes the changes to be made to the source Android XML file and
     * returns a list of {@link Change}.
     * <p/>
     * This function scans an XML file, looking for an attribute value equals to
     * <code>tokenString</code>. If non null, <code>xmlAttrName</code> limit the search
     * to only attributes that have that name.
     * If found, a change is made to replace each occurrence of <code>tokenString</code>
     * by a new "@string/..." using the new <code>xmlStringId</code>.
     *
     * @param sourceFile The file to process.
     *          A status error will be generated if it does not exists.
     *          Must not be null.
     * @param tokenString The string to find. Must not be null or empty.
     * @param xmlAttrName Optional attribute name to limit the search. Can be null.
     * @param allConfigurations True if this function should can all XML files with the same
     *          name and the same resource type folder but with different configurations.
     * @param status Status used to report fatal errors.
     * @param monitor Used to log progress.
     */
    private List<Change> computeXmlSourceChanges(IFile sourceFile,
            String xmlStringId,
            String tokenString,
            String xmlAttrName,
            boolean allConfigurations,
            RefactoringStatus status,
            IProgressMonitor monitor) {

        if (!sourceFile.exists()) {
            status.addFatalError(String.format("XML file '%1$s' does not exist.",
                    sourceFile.getFullPath().toOSString()));
            return null;
        }

        // We shouldn't be trying to replace a null or empty string.
        assert tokenString != null && tokenString.length() > 0;
        if (tokenString == null || tokenString.length() == 0) {
            return null;
        }

        // Note: initially this method was only processing files using a pattern
        //   /project/res/<type>-<configuration>/<filename.xml>
        // However the last version made that more generic to be able to process any XML
        // files. We should probably revisit and simplify this later.
        HashSet<IFile> files = new HashSet<IFile>();
        files.add(sourceFile);

        if (allConfigurations && SdkConstants.EXT_XML.equals(sourceFile.getFileExtension())) {
            IPath path = sourceFile.getFullPath();
            if (path.segmentCount() == 4 && path.segment(1).equals(SdkConstants.FD_RESOURCES)) {
                IProject project = sourceFile.getProject();
                String filename = path.segment(3);
                String initialTypeName = path.segment(2);
                ResourceFolderType type = ResourceFolderType.getFolderType(initialTypeName);

                IContainer res = sourceFile.getParent().getParent();
                if (type != null && res != null && res.getType() == IResource.FOLDER) {
                    try {
                        for (IResource r : res.members()) {
                            if (r != null && r.getType() == IResource.FOLDER) {
                                String name = r.getName();
                                // Skip the initial folder name, it's already in the list.
                                if (!name.equals(initialTypeName)) {
                                    // Only accept the same folder type (e.g. layout-*)
                                    ResourceFolderType t =
                                        ResourceFolderType.getFolderType(name);
                                    if (type.equals(t)) {
                                        // recompute the path
                                        IPath p = res.getProjectRelativePath().append(name).
                                                                               append(filename);
                                        IResource f = project.findMember(p);
                                        if (f != null && f instanceof IFile) {
                                            files.add((IFile) f);
                                        }
                                    }
                                }
                            }
                        }
                    } catch (CoreException e) {
                        // Ignore.
                    }
                }
            }
        }

        SubMonitor subMonitor = SubMonitor.convert(monitor, Math.min(1, files.size()));

        ArrayList<Change> changes = new ArrayList<Change>();

        // Portability note: getModelManager is part of wst.sse.core however the
        // interface returned is part of wst.sse.core.internal.provisional so we can
        // expect it to change in a distant future if they start cleaning their codebase,
        // however unlikely that is.
        IModelManager modelManager = StructuredModelManager.getModelManager();

        for (IFile file : files) {

            IStructuredModel smodel = null;
            MultiTextEdit multiEdit = null;
            TextFileChange xmlChange = null;
            ArrayList<TextEditGroup> editGroups = null;

            try {
                IStructuredDocument sdoc = null;

                smodel = modelManager.getExistingModelForRead(file);
                if (smodel != null) {
                    sdoc = smodel.getStructuredDocument();
                } else if (smodel == null) {
                    // The model is not currently open.
                    if (file.exists()) {
                        sdoc = modelManager.createStructuredDocumentFor(file);
                    } else {
                        sdoc = modelManager.createNewStructuredDocumentFor(file);
                    }
                }

                if (sdoc == null) {
                    status.addFatalError("XML structured document not found");     //$NON-NLS-1$
                    continue;
                }

                multiEdit = new MultiTextEdit();
                editGroups = new ArrayList<TextEditGroup>();
                xmlChange = new TextFileChange(getName(), file);
                xmlChange.setTextType("xml");   //$NON-NLS-1$

                String quotedReplacement = quotedAttrValue(STRING_PREFIX + xmlStringId);

                // Prepare the change set
                for (IStructuredDocumentRegion regions : sdoc.getStructuredDocumentRegions()) {
                    // Only look at XML "top regions"
                    if (!DOMRegionContext.XML_TAG_NAME.equals(regions.getType())) {
                        continue;
                    }

                    int nb = regions.getNumberOfRegions();
                    ITextRegionList list = regions.getRegions();
                    String lastAttrName = null;

                    for (int i = 0; i < nb; i++) {
                        ITextRegion subRegion = list.get(i);
                        String type = subRegion.getType();

                        if (DOMRegionContext.XML_TAG_ATTRIBUTE_NAME.equals(type)) {
                            // Memorize the last attribute name seen
                            lastAttrName = regions.getText(subRegion);

                        } else if (DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE.equals(type)) {
                            // Check this is the attribute and the original string
                            String text = regions.getText(subRegion);

                            // Remove " or ' quoting present in the attribute value
                            text = unquoteAttrValue(text);

                            if (tokenString.equals(text) &&
                                    (xmlAttrName == null || xmlAttrName.equals(lastAttrName))) {

                                // Found an occurrence. Create a change for it.
                                TextEdit edit = new ReplaceEdit(
                                        regions.getStartOffset() + subRegion.getStart(),
                                        subRegion.getTextLength(),
                                        quotedReplacement);
                                TextEditGroup editGroup = new TextEditGroup(
                                        "Replace attribute string by ID",
                                        edit);

                                multiEdit.addChild(edit);
                                editGroups.add(editGroup);
                            }
                        }
                    }
                }
            } catch (Throwable t) {
                // Since we use some internal APIs, use a broad catch-all to report any
                // unexpected issue rather than crash the whole refactoring.
                status.addFatalError(
                        String.format("XML refactoring error: %1$s", t.getMessage()));
            } finally {
                if (smodel != null) {
                    smodel.releaseFromRead();
                }

                if (multiEdit != null &&
                        xmlChange != null &&
                        editGroups != null &&
                        multiEdit.hasChildren()) {
                    xmlChange.setEdit(multiEdit);
                    for (TextEditGroup group : editGroups) {
                        xmlChange.addTextEditChangeGroup(
                                new TextEditChangeGroup(xmlChange, group));
                    }
                    changes.add(xmlChange);
                }
                subMonitor.worked(1);
            }
        } // for files

        if (changes.size() > 0) {
            return changes;
        }
        return null;
    }

    /**
     * Returns a quoted attribute value suitable to be placed after an attributeName=
     * statement in an XML stream.
     *
     * According to http://www.w3.org/TR/2008/REC-xml-20081126/#NT-AttValue
     * the attribute value can be either quoted using ' or " and the corresponding
     * entities &apos; or &quot; must be used inside.
     */
    private String quotedAttrValue(String attrValue) {
        if (attrValue.indexOf('"') == -1) {
            // no double-quotes inside, use double-quotes around.
            return '"' + attrValue + '"';
        }
        if (attrValue.indexOf('\'') == -1) {
            // no single-quotes inside, use single-quotes around.
            return '\'' + attrValue + '\'';
        }
        // If we get here, there's a mix. Opt for double-quote around and replace
        // inner double-quotes.
        attrValue = attrValue.replace("\"", QUOT_ENTITY);  //$NON-NLS-1$
        return '"' + attrValue + '"';
    }

    // --- Java changes ---

    /**
     * Returns a foreach compatible iterator over all ICompilationUnit in the project.
     */
    private Iterable<ICompilationUnit> findAllJavaUnits() {
        final IJavaProject javaProject = JavaCore.create(mProject);

        return new Iterable<ICompilationUnit>() {
            @Override
            public Iterator<ICompilationUnit> iterator() {
                return new Iterator<ICompilationUnit>() {
                    final Queue<ICompilationUnit> mUnits = new LinkedList<ICompilationUnit>();
                    final Queue<IPackageFragment> mFragments = new LinkedList<IPackageFragment>();
                    {
                        try {
                            IPackageFragment[] tmpFrags = javaProject.getPackageFragments();
                            if (tmpFrags != null && tmpFrags.length > 0) {
                                mFragments.addAll(Arrays.asList(tmpFrags));
                            }
                        } catch (JavaModelException e) {
                            // pass
                        }
                    }

                    @Override
                    public boolean hasNext() {
                        if (!mUnits.isEmpty()) {
                            return true;
                        }

                        while (!mFragments.isEmpty()) {
                            try {
                                IPackageFragment fragment = mFragments.poll();
                                if (fragment.getKind() == IPackageFragmentRoot.K_SOURCE) {
                                    ICompilationUnit[] tmpUnits = fragment.getCompilationUnits();
                                    if (tmpUnits != null && tmpUnits.length > 0) {
                                        mUnits.addAll(Arrays.asList(tmpUnits));
                                        return true;
                                    }
                                }
                            } catch (JavaModelException e) {
                                // pass
                            }
                        }
                        return false;
                    }

                    @Override
                    public ICompilationUnit next() {
                        ICompilationUnit unit = mUnits.poll();
                        hasNext();
                        return unit;
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException(
                                "This iterator does not support removal");  //$NON-NLS-1$
                    }
                };
            }
        };
    }

    /**
     * Computes the changes to be made to Java file(s) and returns a list of {@link Change}.
     * <p/>
     * This function scans a Java compilation unit using {@link ReplaceStringsVisitor}, looking
     * for a string literal equals to <code>tokenString</code>.
     * If found, a change is made to replace each occurrence of <code>tokenString</code> by
     * a piece of Java code that somehow accesses R.string.<code>xmlStringId</code>.
     *
     * @param unit The compilated unit to process. Must not be null.
     * @param tokenString The string to find. Must not be null or empty.
     * @param status Status used to report fatal errors.
     * @param monitor Used to log progress.
     */
    private List<Change> computeJavaChanges(ICompilationUnit unit,
            String xmlStringId,
            String tokenString,
            RefactoringStatus status,
            SubMonitor monitor) {

        // We shouldn't be trying to replace a null or empty string.
        assert tokenString != null && tokenString.length() > 0;
        if (tokenString == null || tokenString.length() == 0) {
            return null;
        }

        // Get the Android package name from the Android Manifest. We need it to create
        // the FQCN of the R class.
        String packageName = null;
        String error = null;
        IResource manifestFile = mProject.findMember(SdkConstants.FN_ANDROID_MANIFEST_XML);
        if (manifestFile == null || manifestFile.getType() != IResource.FILE) {
            error = "File not found";
        } else {
            ManifestData manifestData = AndroidManifestHelper.parseForData((IFile) manifestFile);
            if (manifestData == null) {
                error = "Invalid content";
            } else {
                packageName = manifestData.getPackage();
                if (packageName == null) {
                    error = "Missing package definition";
                }
            }
        }

        if (error != null) {
            status.addFatalError(
                    String.format("Failed to parse file %1$s: %2$s.",
                            manifestFile == null ? "" : manifestFile.getFullPath(),  //$NON-NLS-1$
                            error));
            return null;
        }

        // Right now the changes array will contain one TextFileChange at most.
        ArrayList<Change> changes = new ArrayList<Change>();

        // This is the unit that will be modified.
        TextFileChange change = new TextFileChange(getName(), (IFile) unit.getResource());
        change.setTextType("java"); //$NON-NLS-1$

        // Create an AST for this compilation unit
        ASTParser parser = ASTParser.newParser(AST.JLS3);
        parser.setProject(unit.getJavaProject());
        parser.setSource(unit);
        parser.setResolveBindings(true);
        ASTNode node = parser.createAST(monitor.newChild(1));

        // The ASTNode must be a CompilationUnit, by design
        if (!(node instanceof CompilationUnit)) {
            status.addFatalError(String.format("Internal error: ASTNode class %s",  //$NON-NLS-1$
                    node.getClass()));
            return null;
        }

        // ImportRewrite will allow us to add the new type to the imports and will resolve
        // what the Java source must reference, e.g. the FQCN or just the simple name.
        ImportRewrite importRewrite = ImportRewrite.create((CompilationUnit) node, true);
        String Rqualifier = packageName + ".R"; //$NON-NLS-1$
        Rqualifier = importRewrite.addImport(Rqualifier);

        // Rewrite the AST itself via an ASTVisitor
        AST ast = node.getAST();
        ASTRewrite astRewrite = ASTRewrite.create(ast);
        ArrayList<TextEditGroup> astEditGroups = new ArrayList<TextEditGroup>();
        ReplaceStringsVisitor visitor = new ReplaceStringsVisitor(
                ast, astRewrite, astEditGroups,
                tokenString, Rqualifier, xmlStringId);
        node.accept(visitor);

        // Finally prepare the change set
        try {
            MultiTextEdit edit = new MultiTextEdit();

            // Create the edit to change the imports, only if anything changed
            TextEdit subEdit = importRewrite.rewriteImports(monitor.newChild(1));
            if (subEdit.hasChildren()) {
                edit.addChild(subEdit);
            }

            // Create the edit to change the Java source, only if anything changed
            subEdit = astRewrite.rewriteAST();
            if (subEdit.hasChildren()) {
                edit.addChild(subEdit);
            }

            // Only create a change set if any edit was collected
            if (edit.hasChildren()) {
                change.setEdit(edit);

                // Create TextEditChangeGroups which let the user turn changes on or off
                // individually. This must be done after the change.setEdit() call above.
                for (TextEditGroup editGroup : astEditGroups) {
                    TextEditChangeGroup group = new TextEditChangeGroup(change, editGroup);
                    if (editGroup instanceof EnabledTextEditGroup) {
                        group.setEnabled(((EnabledTextEditGroup) editGroup).isEnabled());
                    }
                    change.addTextEditChangeGroup(group);
                }

                changes.add(change);
            }

            monitor.worked(1);

            if (changes.size() > 0) {
                return changes;
            }

        } catch (CoreException e) {
            // ImportRewrite.rewriteImports failed.
            status.addFatalError(e.getMessage());
        }
        return null;
    }

    // ----

    /**
     * Step 3 of 3 of the refactoring: returns the {@link Change} that will be able to do the
     * work and creates a descriptor that can be used to replay that refactoring later.
     *
     * @see org.eclipse.ltk.core.refactoring.Refactoring#createChange(org.eclipse.core.runtime.IProgressMonitor)
     *
     * @throws CoreException
     */
    @Override
    public Change createChange(IProgressMonitor monitor)
            throws CoreException, OperationCanceledException {

        try {
            monitor.beginTask("Applying changes...", 1);

            CompositeChange change = new CompositeChange(
                    getName(),
                    mChanges.toArray(new Change[mChanges.size()])) {
                @Override
                public ChangeDescriptor getDescriptor() {

                    String comment = String.format(
                            "Extracts string '%1$s' into R.string.%2$s",
                            mTokenString,
                            mXmlStringId);

                    ExtractStringDescriptor desc = new ExtractStringDescriptor(
                            mProject.getName(), //project
                            comment, //description
                            comment, //comment
                            createArgumentMap());

                    return new RefactoringChangeDescriptor(desc);
                }
            };

            monitor.worked(1);

            return change;

        } finally {
            monitor.done();
        }

    }

    /**
     * Given a file project path, returns its resource in the same project than the
     * compilation unit. The resource may not exist.
     */
    private IResource getTargetXmlResource(String xmlFileWsPath) {
        IResource resource = mProject.getFile(xmlFileWsPath);
        return resource;
    }
}

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

package org.eclipse.andmore;

import static com.android.SdkConstants.TOOLS_PREFIX;
import static com.android.SdkConstants.TOOLS_URI;
import static org.eclipse.ui.IWorkbenchPage.MATCH_INPUT;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.sdklib.SdkVersionInfo;
import com.android.resources.ResourceFolderType;
import com.android.resources.ResourceType;
import com.android.sdklib.AndroidVersion;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.repository.PkgProps;
import com.android.utils.XmlUtils;
import com.google.common.io.ByteStreams;
import com.google.common.io.Closeables;

import org.eclipse.andmore.internal.editors.AndroidXmlEditor;
import org.eclipse.andmore.internal.editors.layout.gle2.GraphicalEditorPart;
import org.eclipse.andmore.internal.editors.uimodel.UiElementNode;
import org.eclipse.andmore.internal.project.BaseProjectHelper;
import org.eclipse.andmore.internal.project.BaseProjectHelper.IProjectFilter;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.File;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;


/** Utility methods for ADT */
@SuppressWarnings("restriction") // WST API
public class AdtUtils {
    /**
     * Creates a Java class name out of the given string, if possible. For
     * example, "My Project" becomes "MyProject", "hello" becomes "Hello",
     * "Java's" becomes "Java", and so on.
     *
     * @param string the string to be massaged into a Java class
     * @return the string as a Java class, or null if a class name could not be
     *         extracted
     */
    @Nullable
    public static String extractClassName(@NonNull String string) {
        StringBuilder sb = new StringBuilder(string.length());
        int n = string.length();

        int i = 0;
        for (; i < n; i++) {
            char c = Character.toUpperCase(string.charAt(i));
            if (Character.isJavaIdentifierStart(c)) {
                sb.append(c);
                i++;
                break;
            }
        }
        if (sb.length() > 0) {
            for (; i < n; i++) {
                char c = string.charAt(i);
                if (Character.isJavaIdentifierPart(c)) {
                    sb.append(c);
                }
            }

            return sb.toString();
        }

        return null;
    }

    /**
     * Strips off the last file extension from the given filename, e.g.
     * "foo.backup.diff" will be turned into "foo.backup".
     * <p>
     * Note that dot files (e.g. ".profile") will be left alone.
     *
     * @param filename the filename to be stripped
     * @return the filename without the last file extension.
     */
    public static String stripLastExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0) { // > 0 instead of != -1: Treat dot files (e.g. .profile) differently
            return filename.substring(0, dotIndex);
        } else {
            return filename;
        }
    }

    /**
     * Strips off all extensions from the given filename, e.g. "foo.9.png" will
     * be turned into "foo".
     * <p>
     * Note that dot files (e.g. ".profile") will be left alone.
     *
     * @param filename the filename to be stripped
     * @return the filename without any file extensions
     */
    public static String stripAllExtensions(String filename) {
        int dotIndex = filename.indexOf('.');
        if (dotIndex > 0) { // > 0 instead of != -1: Treat dot files (e.g. .profile) differently
            return filename.substring(0, dotIndex);
        } else {
            return filename;
        }
    }

    /**
     * Strips the given suffix from the given string, provided that the string ends with
     * the suffix.
     *
     * @param string the full string to strip from
     * @param suffix the suffix to strip out
     * @return the string without the suffix at the end
     */
    public static String stripSuffix(@NonNull String string, @NonNull String suffix) {
        if (string.endsWith(suffix)) {
            return string.substring(0, string.length() - suffix.length());
        }

        return string;
    }

    /**
     * Capitalizes the string, i.e. transforms the initial [a-z] into [A-Z].
     * Returns the string unmodified if the first character is not [a-z].
     *
     * @param str The string to capitalize.
     * @return The capitalized string
     */
    public static String capitalize(String str) {
        if (str == null || str.length() < 1 || Character.isUpperCase(str.charAt(0))) {
            return str;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(Character.toUpperCase(str.charAt(0)));
        sb.append(str.substring(1));
        return sb.toString();
    }

    /**
     * Converts a CamelCase word into an underlined_word
     *
     * @param string the CamelCase version of the word
     * @return the underlined version of the word
     */
    public static String camelCaseToUnderlines(String string) {
        if (string.isEmpty()) {
            return string;
        }

        StringBuilder sb = new StringBuilder(2 * string.length());
        int n = string.length();
        boolean lastWasUpperCase = Character.isUpperCase(string.charAt(0));
        for (int i = 0; i < n; i++) {
            char c = string.charAt(i);
            boolean isUpperCase = Character.isUpperCase(c);
            if (isUpperCase && !lastWasUpperCase) {
                sb.append('_');
            }
            lastWasUpperCase = isUpperCase;
            c = Character.toLowerCase(c);
            sb.append(c);
        }

        return sb.toString();
    }

    /**
     * Converts an underlined_word into a CamelCase word
     *
     * @param string the underlined word to convert
     * @return the CamelCase version of the word
     */
    public static String underlinesToCamelCase(String string) {
        StringBuilder sb = new StringBuilder(string.length());
        int n = string.length();

        int i = 0;
        boolean upcaseNext = true;
        for (; i < n; i++) {
            char c = string.charAt(i);
            if (c == '_') {
                upcaseNext = true;
            } else {
                if (upcaseNext) {
                    c = Character.toUpperCase(c);
                }
                upcaseNext = false;
                sb.append(c);
            }
        }

        return sb.toString();
    }

    /**
     * Returns the current editor (the currently visible and active editor), or null if
     * not found
     *
     * @return the current editor, or null
     */
    public static IEditorPart getActiveEditor() {
        IWorkbenchWindow window = getActiveWorkbenchWindow();
        if (window != null) {
            IWorkbenchPage page = window.getActivePage();
            if (page != null) {
                return page.getActiveEditor();
            }
        }

        return null;
    }

    /**
     * Returns the current active workbench, or null if not found
     *
     * @return the current window, or null
     */
    @Nullable
    public static IWorkbenchWindow getActiveWorkbenchWindow() {
        IWorkbench workbench = PlatformUI.getWorkbench();
        IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
        if (window == null) {
            IWorkbenchWindow[] windows = workbench.getWorkbenchWindows();
            if (windows.length > 0) {
                window = windows[0];
            }
        }

        return window;
    }

    /**
     * Returns the current active workbench page, or null if not found
     *
     * @return the current page, or null
     */
    @Nullable
    public static IWorkbenchPage getActiveWorkbenchPage() {
        IWorkbenchWindow window = getActiveWorkbenchWindow();
        if (window != null) {
            IWorkbenchPage page = window.getActivePage();
            if (page == null) {
                IWorkbenchPage[] pages = window.getPages();
                if (pages.length > 0) {
                    page = pages[0];
                }
            }

            return page;
        }

        return null;
    }

    /**
     * Returns the current active workbench part, or null if not found
     *
     * @return the current active workbench part, or null
     */
    @Nullable
    public static IWorkbenchPart getActivePart() {
        IWorkbenchWindow window = getActiveWorkbenchWindow();
        if (window != null) {
            IWorkbenchPage activePage = window.getActivePage();
            if (activePage != null) {
                return activePage.getActivePart();
            }
        }
        return null;
    }

    /**
     * Returns the current text editor (the currently visible and active editor), or null
     * if not found.
     *
     * @return the current text editor, or null
     */
    public static ITextEditor getActiveTextEditor() {
        IEditorPart editor = getActiveEditor();
        if (editor != null) {
            if (editor instanceof ITextEditor) {
                return (ITextEditor) editor;
            } else {
                return (ITextEditor) editor.getAdapter(ITextEditor.class);
            }
        }

        return null;
    }

    /**
     * Looks through the open editors and returns the editors that have the
     * given file as input.
     *
     * @param file the file to search for
     * @param restore whether editors should be restored (if they have an open
     *            tab, but the editor hasn't been restored since the most recent
     *            IDE start yet
     * @return a collection of editors
     */
    @NonNull
    public static Collection<IEditorPart> findEditorsFor(@NonNull IFile file, boolean restore) {
        FileEditorInput input = new FileEditorInput(file);
        List<IEditorPart> result = null;
        IWorkbench workbench = PlatformUI.getWorkbench();
        IWorkbenchWindow[] windows = workbench.getWorkbenchWindows();
        for (IWorkbenchWindow window : windows) {
            IWorkbenchPage[] pages = window.getPages();
            for (IWorkbenchPage page : pages) {
                IEditorReference[] editors = page.findEditors(input, null,  MATCH_INPUT);
                if (editors != null) {
                    for (IEditorReference reference : editors) {
                        IEditorPart editor = reference.getEditor(restore);
                        if (editor != null) {
                            if (result == null) {
                                result = new ArrayList<IEditorPart>();
                            }
                            result.add(editor);
                        }
                    }
                }
            }
        }

        if (result == null) {
            return Collections.emptyList();
        }

        return result;
    }

    /**
     * Attempts to convert the given {@link URL} into a {@link File}.
     *
     * @param url the {@link URL} to be converted
     * @return the corresponding {@link File}, which may not exist
     */
    @NonNull
    public static File getFile(@NonNull URL url) {
        try {
            // First try URL.toURI(): this will work for URLs that contain %20 for spaces etc.
            // Unfortunately, it *doesn't* work for "broken" URLs where the URL contains
            // spaces, which is often the case.
            return new File(url.toURI());
        } catch (URISyntaxException e) {
            // ...so as a fallback, go to the old url.getPath() method, which handles space paths.
            return new File(url.getPath());
        }
    }

    /**
     * Returns the file for the current editor, if any.
     *
     * @return the file for the current editor, or null if none
     */
    public static IFile getActiveFile() {
        IEditorPart editor = getActiveEditor();
        if (editor != null) {
            IEditorInput input = editor.getEditorInput();
            if (input instanceof IFileEditorInput) {
                IFileEditorInput fileInput = (IFileEditorInput) input;
                return fileInput.getFile();
            }
        }

        return null;
    }

    /**
     * Returns an absolute path to the given resource
     *
     * @param resource the resource to look up a path for
     * @return an absolute file system path to the resource
     */
    @NonNull
    public static IPath getAbsolutePath(@NonNull IResource resource) {
        IPath location = resource.getRawLocation();
        if (location != null) {
            return location.makeAbsolute();
        } else {
            IWorkspace workspace = ResourcesPlugin.getWorkspace();
            IWorkspaceRoot root = workspace.getRoot();
            IPath workspacePath = root.getLocation();
            return workspacePath.append(resource.getFullPath());
        }
    }

    /**
     * Converts a workspace-relative path to an absolute file path
     *
     * @param path the workspace-relative path to convert
     * @return the corresponding absolute file in the file system
     */
    @NonNull
    public static File workspacePathToFile(@NonNull IPath path) {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IResource res = root.findMember(path);
        if (res != null) {
            IPath location = res.getLocation();
            if (location != null) {
                return location.toFile();
            }
            return root.getLocation().append(path).toFile();
        }

        return path.toFile();
    }

    /**
     * Converts a {@link File} to an {@link IFile}, if possible.
     *
     * @param file a file to be converted
     * @return the corresponding {@link IFile}, or null
     */
    public static IFile fileToIFile(File file) {
        if (!file.isAbsolute()) {
            file = file.getAbsoluteFile();
        }

        IWorkspaceRoot workspace = ResourcesPlugin.getWorkspace().getRoot();
        IFile[] files = workspace.findFilesForLocationURI(file.toURI());
        if (files.length > 0) {
            return files[0];
        }

        IPath filePath = new Path(file.getPath());
        return pathToIFile(filePath);
    }

    /**
     * Converts a {@link File} to an {@link IResource}, if possible.
     *
     * @param file a file to be converted
     * @return the corresponding {@link IResource}, or null
     */
    public static IResource fileToResource(File file) {
        if (!file.isAbsolute()) {
            file = file.getAbsoluteFile();
        }

        IWorkspaceRoot workspace = ResourcesPlugin.getWorkspace().getRoot();
        IFile[] files = workspace.findFilesForLocationURI(file.toURI());
        if (files.length > 0) {
            return files[0];
        }

        IPath filePath = new Path(file.getPath());
        return pathToResource(filePath);
    }

    /**
     * Converts a {@link IPath} to an {@link IFile}, if possible.
     *
     * @param path a path to be converted
     * @return the corresponding {@link IFile}, or null
     */
    public static IFile pathToIFile(IPath path) {
        IWorkspaceRoot workspace = ResourcesPlugin.getWorkspace().getRoot();

        IFile[] files = workspace.findFilesForLocationURI(URIUtil.toURI(path.makeAbsolute()));
        if (files.length > 0) {
            return files[0];
        }

        IPath workspacePath = workspace.getLocation();
        if (workspacePath.isPrefixOf(path)) {
            IPath relativePath = path.makeRelativeTo(workspacePath);
            IResource member = workspace.findMember(relativePath);
            if (member instanceof IFile) {
                return (IFile) member;
            }
        } else if (path.isAbsolute()) {
            return workspace.getFileForLocation(path);
        }

        return null;
    }

    /**
     * Converts a {@link IPath} to an {@link IResource}, if possible.
     *
     * @param path a path to be converted
     * @return the corresponding {@link IResource}, or null
     */
    public static IResource pathToResource(IPath path) {
        IWorkspaceRoot workspace = ResourcesPlugin.getWorkspace().getRoot();

        IFile[] files = workspace.findFilesForLocationURI(URIUtil.toURI(path.makeAbsolute()));
        if (files.length > 0) {
            return files[0];
        }

        IPath workspacePath = workspace.getLocation();
        if (workspacePath.isPrefixOf(path)) {
            IPath relativePath = path.makeRelativeTo(workspacePath);
            return workspace.findMember(relativePath);
        } else if (path.isAbsolute()) {
            return workspace.getFileForLocation(path);
        }

        return null;
    }

    /**
     * Returns all markers in a file/document that fit on the same line as the given offset
     *
     * @param markerType the marker type
     * @param file the file containing the markers
     * @param document the document showing the markers
     * @param offset the offset to be checked
     * @return a list (possibly empty but never null) of matching markers
     */
    @NonNull
    public static List<IMarker> findMarkersOnLine(
            @NonNull String markerType,
            @NonNull IResource file,
            @NonNull IDocument document,
            int offset) {
        List<IMarker> matchingMarkers = new ArrayList<IMarker>(2);
        try {
            IMarker[] markers = file.findMarkers(markerType, true, IResource.DEPTH_ZERO);

            // Look for a match on the same line as the caret.
            IRegion lineInfo = document.getLineInformationOfOffset(offset);
            int lineStart = lineInfo.getOffset();
            int lineEnd = lineStart + lineInfo.getLength();
            int offsetLine = document.getLineOfOffset(offset);


            for (IMarker marker : markers) {
                int start = marker.getAttribute(IMarker.CHAR_START, -1);
                int end = marker.getAttribute(IMarker.CHAR_END, -1);
                if (start >= lineStart && start <= lineEnd && end > start) {
                    matchingMarkers.add(marker);
                } else if (start == -1 && end == -1) {
                    // Some markers don't set character range, they only set the line
                    int line = marker.getAttribute(IMarker.LINE_NUMBER, -1);
                    if (line == offsetLine + 1) {
                        matchingMarkers.add(marker);
                    }
                }
            }
        } catch (CoreException e) {
            AndmoreAndroidPlugin.log(e, null);
        } catch (BadLocationException e) {
            AndmoreAndroidPlugin.log(e, null);
        }

        return matchingMarkers;
    }

    /**
     * Returns the available and open Android projects
     *
     * @return the available and open Android projects, never null
     */
    @NonNull
    public static IJavaProject[] getOpenAndroidProjects() {
        return BaseProjectHelper.getAndroidProjects(new IProjectFilter() {
            @Override
            public boolean accept(IProject project) {
                return project.isAccessible();
            }
        });
    }

    /**
     * Returns a unique project name, based on the given {@code base} file name
     * possibly with a {@code conjunction} and a new number behind it to ensure
     * that the project name is unique. For example,
     * {@code getUniqueProjectName("project", "_")} will return
     * {@code "project"} if that name does not already exist, and if it does, it
     * will return {@code "project_2"}.
     *
     * @param base the base name to use, such as "foo"
     * @param conjunction a string to insert between the base name and the
     *            number.
     * @return a unique project name based on the given base and conjunction
     */
    public static String getUniqueProjectName(String base, String conjunction) {
        // We're using all workspace projects here rather than just open Android project
        // via getOpenAndroidProjects because the name cannot conflict with non-Android
        // or closed projects either
        IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        IProject[] projects = workspaceRoot.getProjects();

        for (int i = 1; i < 1000; i++) {
            String name = i == 1 ? base : base + conjunction + Integer.toString(i);
            boolean found = false;
            for (IProject project : projects) {
                // Need to make case insensitive comparison, since otherwise we can hit
                // org.eclipse.core.internal.resources.ResourceException:
                // A resource exists with a different case: '/test'.
                if (project.getName().equalsIgnoreCase(name)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return name;
            }
        }

        return base;
    }

    /**
     * Returns the name of the parent folder for the given editor input
     *
     * @param editorInput the editor input to check
     * @return the parent folder, which is never null but may be ""
     */
    @NonNull
    public static String getParentFolderName(@Nullable IEditorInput editorInput) {
        if (editorInput instanceof IFileEditorInput) {
             IFile file = ((IFileEditorInput) editorInput).getFile();
             return file.getParent().getName();
        }

        if (editorInput instanceof IURIEditorInput) {
            IURIEditorInput urlEditorInput = (IURIEditorInput) editorInput;
            String path = urlEditorInput.getURI().toString();
            int lastIndex = path.lastIndexOf('/');
            if (lastIndex != -1) {
                int lastLastIndex = path.lastIndexOf('/', lastIndex - 1);
                if (lastLastIndex != -1) {
                    return path.substring(lastLastIndex + 1, lastIndex);
                }
            }
        }

        return "";
    }

    /**
     * Returns the XML editor for the given editor part
     *
     * @param part the editor part to look up the editor for
     * @return the editor or null if this part is not an XML editor
     */
    @Nullable
    public static AndroidXmlEditor getXmlEditor(@NonNull IEditorPart part) {
        if (part instanceof AndroidXmlEditor) {
            return (AndroidXmlEditor) part;
        } else if (part instanceof GraphicalEditorPart) {
            ((GraphicalEditorPart) part).getEditorDelegate().getEditor();
        }

        return null;
    }

    /**
     * Sets the given tools: attribute in the given XML editor document, adding
     * the tools name space declaration if necessary, formatting the affected
     * document region, and optionally comma-appending to an existing value and
     * optionally opening and revealing the attribute.
     *
     * @param editor the associated editor
     * @param element the associated element
     * @param description the description of the attribute (shown in the undo
     *            event)
     * @param name the name of the attribute
     * @param value the attribute value
     * @param reveal if true, open the editor and select the given attribute
     *            node
     * @param appendValue if true, add this value as a comma separated value to
     *            the existing attribute value, if any
     */
    public static void setToolsAttribute(
            @NonNull final AndroidXmlEditor editor,
            @NonNull final Element element,
            @NonNull final String description,
            @NonNull final String name,
            @Nullable final String value,
            final boolean reveal,
            final boolean appendValue) {
        editor.wrapUndoEditXmlModel(description, new Runnable() {
            @Override
            public void run() {
                String prefix = XmlUtils.lookupNamespacePrefix(element, TOOLS_URI, null, true);
                if (prefix == null) {
                    // Add in new prefix...
                    prefix = XmlUtils.lookupNamespacePrefix(element,
                            TOOLS_URI, TOOLS_PREFIX, true /*create*/);
                    if (value != null) {
                        // ...and ensure that the header is formatted such that
                        // the XML namespace declaration is placed in the right
                        // position and wrapping is applied etc.
                        editor.scheduleNodeReformat(editor.getUiRootNode(),
                                true /*attributesOnly*/);
                    }
                }

                String v = value;
                if (appendValue && v != null) {
                    String prev = element.getAttributeNS(TOOLS_URI, name);
                    if (prev.length() > 0) {
                        v = prev + ',' + value;
                    }
                }

                // Use the non-namespace form of set attribute since we can't
                // reference the namespace until the model has been reloaded
                if (v != null) {
                    element.setAttribute(prefix + ':' + name, v);
                } else {
                    element.removeAttribute(prefix + ':' + name);
                }

                UiElementNode rootUiNode = editor.getUiRootNode();
                if (rootUiNode != null && v != null) {
                    final UiElementNode uiNode = rootUiNode.findXmlNode(element);
                    if (uiNode != null) {
                        editor.scheduleNodeReformat(uiNode, true /*attributesOnly*/);

                        if (reveal) {
                            // Update editor selection after format
                            Display display = AndmoreAndroidPlugin.getDisplay();
                            if (display != null) {
                                display.asyncExec(new Runnable() {
                                    @Override
                                    public void run() {
                                        Node xmlNode = uiNode.getXmlNode();
                                        Attr attribute = ((Element) xmlNode).getAttributeNodeNS(
                                                TOOLS_URI, name);
                                        if (attribute instanceof IndexedRegion) {
                                            IndexedRegion region = (IndexedRegion) attribute;
                                            editor.getStructuredTextEditor().selectAndReveal(
                                                    region.getStartOffset(), region.getLength());
                                        }
                                    }
                                });
                            }
                        }
                    }
                }
            }
        });
    }

    /**
     * Returns a string label for the given target, of the form
     * "API 16: Android 4.1 (Jelly Bean)".
     *
     * @param target the target to generate a string from
     * @return a suitable display string
     */
    @NonNull
    public static String getTargetLabel(@NonNull IAndroidTarget target) {
        if (target.isPlatform()) {
            AndroidVersion version = target.getVersion();
            String codename = target.getVersion().getCodename();
            String release = target.getProperty("ro.build.version.release"); //$NON-NLS-1$
            if (codename != null) {
                return String.format("API %1$d: Android %2$s (%3$s)",
                        version.getApiLevel(),
                        release,
                        codename);
            }
            return String.format("API %1$d: Android %2$s", version.getApiLevel(),
                    release);
        }

        return String.format("%1$s (API %2$s)", target.getFullName(),
                target.getVersion().getApiString());
    }

    /**
     * Sets the given tools: attribute in the given XML editor document, adding
     * the tools name space declaration if necessary, and optionally
     * comma-appending to an existing value.
     *
     * @param file the file associated with the element
     * @param element the associated element
     * @param description the description of the attribute (shown in the undo
     *            event)
     * @param name the name of the attribute
     * @param value the attribute value
     * @param appendValue if true, add this value as a comma separated value to
     *            the existing attribute value, if any
     */
    public static void setToolsAttribute(
            @NonNull final IFile file,
            @NonNull final Element element,
            @NonNull final String description,
            @NonNull final String name,
            @Nullable final String value,
            final boolean appendValue) {
        IModelManager modelManager = StructuredModelManager.getModelManager();
        if (modelManager == null) {
            return;
        }

        try {
            IStructuredModel model = null;
            if (model == null) {
                model = modelManager.getModelForEdit(file);
            }
            if (model != null) {
                try {
                    model.aboutToChangeModel();
                    if (model instanceof IDOMModel) {
                        IDOMModel domModel = (IDOMModel) model;
                        Document doc = domModel.getDocument();
                        if (doc != null && element.getOwnerDocument() == doc) {
                            String prefix = XmlUtils.lookupNamespacePrefix(element, TOOLS_URI,
                                    null, true);
                            if (prefix == null) {
                                // Add in new prefix...
                                prefix = XmlUtils.lookupNamespacePrefix(element,
                                        TOOLS_URI, TOOLS_PREFIX, true);
                            }

                            String v = value;
                            if (appendValue && v != null) {
                                String prev = element.getAttributeNS(TOOLS_URI, name);
                                if (prev.length() > 0) {
                                    v = prev + ',' + value;
                                }
                            }

                            // Use the non-namespace form of set attribute since we can't
                            // reference the namespace until the model has been reloaded
                            if (v != null) {
                                element.setAttribute(prefix + ':' + name, v);
                            } else {
                                element.removeAttribute(prefix + ':' + name);
                            }
                        }
                    }
                } finally {
                    model.changedModel();
                    String updated = model.getStructuredDocument().get();
                    model.releaseFromEdit();
                    model.save(file);

                    // Must also force a save on disk since the above model.save(file) often
                    // (always?) has no effect.
                    ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
                    NullProgressMonitor monitor = new NullProgressMonitor();
                    IPath path = file.getFullPath();
                    manager.connect(path, LocationKind.IFILE, monitor);
                    try {
                        ITextFileBuffer buffer = manager.getTextFileBuffer(path,
                                LocationKind.IFILE);
                        IDocument currentDocument = buffer.getDocument();
                        currentDocument.set(updated);
                        buffer.commit(monitor, true);
                    } finally {
                        manager.disconnect(path, LocationKind.IFILE,  monitor);
                    }
                }
            }
        } catch (Exception e) {
            AndmoreAndroidPlugin.log(e, null);
        }
    }

    /**
     * Returns the Android version and code name of the given API level
     *
     * @param api the api level
     * @return a suitable version display name
     */
    public static String getAndroidName(int api) {
        if (api <= SdkVersionInfo.HIGHEST_KNOWN_API) {
            return SdkVersionInfo.getAndroidName(api);
        }

        // Consult SDK manager to see if we know any more (later) names,
        // installed by user
        Sdk sdk = Sdk.getCurrent();
        if (sdk != null) {
            for (IAndroidTarget target : sdk.getTargets()) {
                if (target.isPlatform()) {
                    AndroidVersion version = target.getVersion();
                    if (version.getApiLevel() == api) {
                        return getTargetLabel(target);
                    }
                }
            }
        }

        return "API " + api;
    }

    /**
     * Returns the highest known API level to this version of ADT. The
     * {@link #getAndroidName(int)} method will return real names up to and
     * including this number.
     *
     * @return the highest known API number
     */
    public static int getHighestKnownApiLevel() {
        return SdkVersionInfo.HIGHEST_KNOWN_API;
    }

    /**
     * Returns a list of known API names
     *
     * @return a list of string API names, starting from 1 and up through the
     *         maximum known versions (with no gaps)
     */
    public static String[] getKnownVersions() {
        int max = getHighestKnownApiLevel();
        Sdk sdk = Sdk.getCurrent();
        if (sdk != null) {
            for (IAndroidTarget target : sdk.getTargets()) {
                if (target.isPlatform()) {
                    AndroidVersion version = target.getVersion();
                    if (!version.isPreview()) {
                        max = Math.max(max, version.getApiLevel());
                    }
                }
            }
        }

        String[] versions = new String[max];
        for (int api = 1; api <= max; api++) {
            versions[api-1] = getAndroidName(api);
        }

        return versions;
    }

    /**
     * Returns the Android project(s) that are selected or active, if any. This
     * considers the selection, the active editor, etc.
     *
     * @param selection the current selection
     * @return a list of projects, possibly empty (but never null)
     */
    @NonNull
    public static List<IProject> getSelectedProjects(@Nullable ISelection selection) {
        List<IProject> projects = new ArrayList<IProject>();

        if (selection instanceof IStructuredSelection) {
            IStructuredSelection structuredSelection = (IStructuredSelection) selection;
            // get the unique selected item.
            Iterator<?> iterator = structuredSelection.iterator();
            while (iterator.hasNext()) {
                Object element = iterator.next();

                // First look up the resource (since some adaptables
                // provide an IResource but not an IProject, and we can
                // always go from IResource to IProject)
                IResource resource = null;
                if (element instanceof IResource) { // may include IProject
                   resource = (IResource) element;
                } else if (element instanceof IAdaptable) {
                    IAdaptable adaptable = (IAdaptable)element;
                    Object adapter = adaptable.getAdapter(IResource.class);
                    resource = (IResource) adapter;
                }

                // get the project object from it.
                IProject project = null;
                if (resource != null) {
                    project = resource.getProject();
                } else if (element instanceof IAdaptable) {
                    project = (IProject) ((IAdaptable) element).getAdapter(IProject.class);
                }

                if (project != null && !projects.contains(project)) {
                    projects.add(project);
                }
            }
        }

        if (projects.isEmpty()) {
            // Try to look at the active editor instead
            IFile file = AdtUtils.getActiveFile();
            if (file != null) {
                projects.add(file.getProject());
            }
        }

        if (projects.isEmpty()) {
            // If we didn't find a default project based on the selection, check how many
            // open Android projects we can find in the current workspace. If there's only
            // one, we'll just select it by default.
            IJavaProject[] open = AdtUtils.getOpenAndroidProjects();
            for (IJavaProject project : open) {
                projects.add(project.getProject());
            }
            return projects;
        } else {
            // Make sure all the projects are Android projects
            List<IProject> androidProjects = new ArrayList<IProject>(projects.size());
            for (IProject project : projects) {
                if (BaseProjectHelper.isAndroidProject(project)) {
                    androidProjects.add(project);
                }
            }
            return androidProjects;
        }
    }

    private static Boolean sEclipse4;

    /**
     * Returns true if the running Eclipse is version 4.x or later
     *
     * @return true if the current Eclipse version is 4.x or later, false
     *         otherwise
     */
    public static boolean isEclipse4() {
        if (sEclipse4 == null) {
            sEclipse4 = Platform.getBundle("org.eclipse.e4.ui.model.workbench") != null; //$NON-NLS-1$
        }

        return sEclipse4;
    }

    /**
     * Reads the contents of an {@link IFile} and return it as a byte array
     *
     * @param file the file to be read
     * @return the String read from the file, or null if there was an error
     */
    @SuppressWarnings("resource") // Eclipse doesn't understand Closeables.closeQuietly yet
    @Nullable
    public static byte[] readData(@NonNull IFile file) {
        InputStream contents = null;
        try {
            contents = file.getContents();
            return ByteStreams.toByteArray(contents);
        } catch (Exception e) {
            // Pass -- just return null
        } finally {
            Closeables.closeQuietly(contents);
        }

        return null;
    }

    /**
     * Ensure that a given folder (and all its parents) are created. This implements
     * the equivalent of {@link File#mkdirs()} for {@link IContainer} folders.
     *
     * @param container the container to ensure exists
     * @throws CoreException if an error occurs
     */
    public static void ensureExists(@Nullable IContainer container) throws CoreException {
        if (container == null || container.exists()) {
            return;
        }
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IFolder folder = root.getFolder(container.getFullPath());
        ensureExists(folder);
    }

    private static void ensureExists(IFolder folder) throws CoreException {
        if (folder != null && !folder.exists()) {
            IContainer parent = folder.getParent();
            if (parent instanceof IFolder) {
                ensureExists((IFolder) parent);
            }
            folder.create(false, false, null);
        }
    }

    /**
     * Format the given floating value into an XML string, omitting decimals if
     * 0
     *
     * @param value the value to be formatted
     * @return the corresponding XML string for the value
     */
    public static String formatFloatAttribute(float value) {
        if (value != (int) value) {
            // Run String.format without a locale, because we don't want locale-specific
            // conversions here like separating the decimal part with a comma instead of a dot!
            return String.format((Locale) null, "%.2f", value); //$NON-NLS-1$
        } else {
            return Integer.toString((int) value);
        }
    }

    /**
     * Creates all the directories required for the given path.
     *
     * @param wsPath the path to create all the parent directories for
     * @return true if all the parent directories were created
     */
    public static boolean createWsParentDirectory(IContainer wsPath) {
        if (wsPath.getType() == IResource.FOLDER) {
            if (wsPath.exists()) {
                return true;
            }

            IFolder folder = (IFolder) wsPath;
            try {
                if (createWsParentDirectory(wsPath.getParent())) {
                    folder.create(true /* force */, true /* local */, null /* monitor */);
                    return true;
                }
            } catch (CoreException e) {
                e.printStackTrace();
            }
        }

        return false;
    }

    /**
     * Lists the files of the given directory and returns them as an array which
     * is never null. This simplifies processing file listings from for each
     * loops since {@link File#listFiles} can return null. This method simply
     * wraps it and makes sure it returns an empty array instead if necessary.
     *
     * @param dir the directory to list
     * @return the children, or empty if it has no children, is not a directory,
     *         etc.
     */
    @NonNull
    public static File[] listFiles(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            return files;
        } else {
            return new File[0];
        }
    }

    /**
     * Closes all open editors that are showing a file for the given project. This method
     * should be called when a project is closed or deleted.
     * <p>
     * This method can be called from any thread, but if it is not called on the GUI thread
     * the editor will be closed asynchronously.
     *
     * @param project the project to close all editors for
     * @param save whether unsaved editors should be saved first
     */
    public static void closeEditors(@NonNull final IProject project, final boolean save) {
        final Display display = AndmoreAndroidPlugin.getDisplay();
        if (display == null || display.isDisposed()) {
            return;
        }
        if (display.getThread() != Thread.currentThread()) {
            display.asyncExec(new Runnable() {
                @Override
                public void run() {
                    closeEditors(project, save);
                }
            });
            return;
        }

        // Close editors for removed files
        IWorkbench workbench = PlatformUI.getWorkbench();
        for (IWorkbenchWindow window : workbench.getWorkbenchWindows()) {
            for (IWorkbenchPage page : window.getPages()) {
                List<IEditorReference> matching = null;
                for (IEditorReference ref : page.getEditorReferences()) {
                    boolean close = false;
                    try {
                        IEditorInput input = ref.getEditorInput();
                        if (input instanceof IFileEditorInput) {
                            IFileEditorInput fileInput = (IFileEditorInput) input;
                            if (project.equals(fileInput.getFile().getProject())) {
                                close = true;
                            }
                        }
                    } catch (PartInitException ex) {
                        close = true;
                    }
                    if (close) {
                        if (matching == null) {
                            matching = new ArrayList<IEditorReference>(2);
                        }
                        matching.add(ref);
                    }
                }
                if (matching != null) {
                    IEditorReference[] refs = new IEditorReference[matching.size()];
                    page.closeEditors(matching.toArray(refs), save);
                }
            }
        }
    }

    /**
     * Closes all open editors for the given file. Note that a file can be open in
     * more than one editor, for example by using Open With on the file to choose different
     * editors.
     * <p>
     * This method can be called from any thread, but if it is not called on the GUI thread
     * the editor will be closed asynchronously.
     *
     * @param file the file whose editors should be closed.
     * @param save whether unsaved editors should be saved first
     */
    public static void closeEditors(@NonNull final IFile file, final boolean save) {
        final Display display = AndmoreAndroidPlugin.getDisplay();
        if (display == null || display.isDisposed()) {
            return;
        }
        if (display.getThread() != Thread.currentThread()) {
            display.asyncExec(new Runnable() {
                @Override
                public void run() {
                    closeEditors(file, save);
                }
            });
            return;
        }

        // Close editors for removed files
        IWorkbench workbench = PlatformUI.getWorkbench();
        for (IWorkbenchWindow window : workbench.getWorkbenchWindows()) {
            for (IWorkbenchPage page : window.getPages()) {
                List<IEditorReference> matching = null;
                for (IEditorReference ref : page.getEditorReferences()) {
                    boolean close = false;
                    try {
                        IEditorInput input = ref.getEditorInput();
                        if (input instanceof IFileEditorInput) {
                            IFileEditorInput fileInput = (IFileEditorInput) input;
                            if (file.equals(fileInput.getFile())) {
                                close = true;
                            }
                        }
                    } catch (PartInitException ex) {
                        close = true;
                    }
                    if (close) {
                        // Found
                        if (matching == null) {
                            matching = new ArrayList<IEditorReference>(2);
                        }
                        matching.add(ref);
                        // We don't break here in case the file is
                        // opened multiple times with different editors.
                    }
                }
                if (matching != null) {
                    IEditorReference[] refs = new IEditorReference[matching.size()];
                    page.closeEditors(matching.toArray(refs), save);
                }
            }
        }
    }

    /**
     * Returns the offset region of the given 0-based line number in the given
     * file
     *
     * @param file the file to look up the line number in
     * @param line the line number (0-based, meaning that the first line is line
     *            0)
     * @return the corresponding offset range, or null
     */
    @Nullable
    public static IRegion getRegionOfLine(@NonNull IFile file, int line) {
        IDocumentProvider provider = new TextFileDocumentProvider();
        try {
            provider.connect(file);
            IDocument document = provider.getDocument(file);
            if (document != null) {
                return document.getLineInformation(line);
            }
        } catch (Exception e) {
            AndmoreAndroidPlugin.log(e, "Can't find range information for %1$s", file.getName());
        } finally {
            provider.disconnect(file);
        }

        return null;
    }

    /**
     * Returns all resource variations for the given file
     *
     * @param file resource file, which should be an XML file in one of the
     *            various resource folders, e.g. res/layout, res/values-xlarge, etc.
     * @param includeSelf if true, include the file itself in the list,
     *            otherwise exclude it
     * @return a list of all the resource variations
     */
    public static List<IFile> getResourceVariations(@Nullable IFile file, boolean includeSelf) {
        if (file == null) {
            return Collections.emptyList();
        }

        // Compute the set of layout files defining this layout resource
        List<IFile> variations = new ArrayList<IFile>();
        String name = file.getName();
        IContainer parent = file.getParent();
        if (parent != null) {
            IContainer resFolder = parent.getParent();
            if (resFolder != null) {
                String parentName = parent.getName();
                String prefix = parentName;
                int qualifiers = prefix.indexOf('-');

                if (qualifiers != -1) {
                    parentName = prefix.substring(0, qualifiers);
                    prefix = prefix.substring(0, qualifiers + 1);
                } else {
                    prefix = prefix + '-';
                }
                try {
                    for (IResource resource : resFolder.members()) {
                        String n = resource.getName();
                        if ((n.startsWith(prefix) || n.equals(parentName))
                                && resource instanceof IContainer) {
                            IContainer layoutFolder = (IContainer) resource;
                            IResource r = layoutFolder.findMember(name);
                            if (r instanceof IFile) {
                                IFile variation = (IFile) r;
                                if (!includeSelf && file.equals(variation)) {
                                    continue;
                                }
                                variations.add(variation);
                            }
                        }
                    }
                } catch (CoreException e) {
                    AndmoreAndroidPlugin.log(e, null);
                }
            }
        }

        return variations;
    }

    /**
     * Returns whether the current thread is the UI thread
     *
     * @return true if the current thread is the UI thread
     */
    public static boolean isUiThread() {
        return AndmoreAndroidPlugin.getDisplay() != null
                && AndmoreAndroidPlugin.getDisplay().getThread() == Thread.currentThread();
    }

    /**
     * Replaces any {@code \\uNNNN} references in the given string with the corresponding
     * unicode characters.
     *
     * @param s the string to perform replacements in
     * @return the string with unicode escapes replaced with actual characters
     */
    @NonNull
    public static String replaceUnicodeEscapes(@NonNull String s) {
        // Handle unicode escapes
        if (s.indexOf("\\u") != -1) { //$NON-NLS-1$
            StringBuilder sb = new StringBuilder(s.length());
            for (int i = 0, n = s.length(); i < n; i++) {
                char c = s.charAt(i);
                if (c == '\\' && i < n - 1) {
                    char next = s.charAt(i + 1);
                    if (next == 'u' && i < n - 5) { // case sensitive
                        String hex = s.substring(i + 2, i + 6);
                        try {
                            int unicodeValue = Integer.parseInt(hex, 16);
                            sb.append((char) unicodeValue);
                            i += 5;
                            continue;
                        } catch (NumberFormatException nufe) {
                            // Invalid escape: Just proceed to literally transcribe it
                            sb.append(c);
                        }
                    } else {
                        sb.append(c);
                        sb.append(next);
                        i++;
                        continue;
                    }
                } else {
                    sb.append(c);
                }
            }
            s = sb.toString();
        }

        return s;
    }

    /**
     * Looks up the {@link ResourceFolderType} corresponding to a given
     * {@link ResourceType}: the folder where those resources can be found.
     * <p>
     * Note that {@link ResourceType#ID} is a special case: it can not just
     * be defined in {@link ResourceFolderType#VALUES}, but it can also be
     * defined inline via {@code @+id} in {@link ResourceFolderType#LAYOUT} and
     * {@link ResourceFolderType#MENU} folders.
     *
     * @param type the resource type
     * @return the corresponding resource folder type
     */
    @NonNull
    public static ResourceFolderType getFolderTypeFor(@NonNull ResourceType type) {
        switch (type) {
            case ANIM:
                return ResourceFolderType.ANIM;
            case ANIMATOR:
                return ResourceFolderType.ANIMATOR;
            case ARRAY:
                return ResourceFolderType.VALUES;
            case COLOR:
                return ResourceFolderType.COLOR;
            case DRAWABLE:
                return ResourceFolderType.DRAWABLE;
            case INTERPOLATOR:
                return ResourceFolderType.INTERPOLATOR;
            case LAYOUT:
                return ResourceFolderType.LAYOUT;
            case MENU:
                return ResourceFolderType.MENU;
            case MIPMAP:
                return ResourceFolderType.MIPMAP;
            case RAW:
                return ResourceFolderType.RAW;
            case XML:
                return ResourceFolderType.XML;
            case ATTR:
            case BOOL:
            case DECLARE_STYLEABLE:
            case DIMEN:
            case FRACTION:
            case ID:
            case INTEGER:
            case PLURALS:
            case PUBLIC:
            case STRING:
            case STYLE:
            case STYLEABLE:
                return ResourceFolderType.VALUES;
            default:
                assert false : type;
            return ResourceFolderType.VALUES;

        }
    }

    /**
     * Looks up the {@link ResourceType} defined in a given {@link ResourceFolderType}.
     * <p>
     * Note that for {@link ResourceFolderType#VALUES} there are many, many
     * different types of resources that can be defined, so this method returns
     * {@code null} for that scenario.
     * <p>
     * Note also that {@link ResourceType#ID} is a special case: it can not just
     * be defined in {@link ResourceFolderType#VALUES}, but it can also be
     * defined inline via {@code @+id} in {@link ResourceFolderType#LAYOUT} and
     * {@link ResourceFolderType#MENU} folders.
     *
     * @param folderType the resource folder type
     * @return the corresponding resource type, or null if {@code folderType} is
     *         {@link ResourceFolderType#VALUES}
     */
    @Nullable
    public static ResourceType getResourceTypeFor(@NonNull ResourceFolderType folderType) {
        switch (folderType) {
            case ANIM:
                return ResourceType.ANIM;
            case ANIMATOR:
                return ResourceType.ANIMATOR;
            case COLOR:
                return ResourceType.COLOR;
            case DRAWABLE:
                return ResourceType.DRAWABLE;
            case INTERPOLATOR:
                return ResourceType.INTERPOLATOR;
            case LAYOUT:
                return ResourceType.LAYOUT;
            case MENU:
                return ResourceType.MENU;
            case MIPMAP:
                return ResourceType.MIPMAP;
            case RAW:
                return ResourceType.RAW;
            case XML:
                return ResourceType.XML;
            case VALUES:
                return null;
            default:
                assert false : folderType;
                return null;
        }
    }
}

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
package org.eclipse.andmore.internal.lint;

import static com.android.SdkConstants.DOT_JAR;
import static com.android.SdkConstants.DOT_XML;
import static com.android.SdkConstants.FD_NATIVE_LIBS;
import static org.eclipse.andmore.AndmoreAndroidConstants.MARKER_LINT;
import static org.eclipse.andmore.AdtUtils.workspacePathToFile;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.sdklib.IAndroidTarget;
import com.android.tools.lint.checks.BuiltinIssueRegistry;
import com.android.tools.lint.client.api.Configuration;
import com.android.tools.lint.client.api.IssueRegistry;
import com.android.tools.lint.client.api.JavaParser;
import com.android.tools.lint.client.api.LintClient;
import com.android.tools.lint.client.api.XmlParser;
import com.android.tools.lint.detector.api.ClassContext;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.DefaultPosition;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.LintUtils;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Location.Handle;
import com.android.tools.lint.detector.api.Position;
import com.android.tools.lint.detector.api.Project;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.TextFormat;
import com.android.tools.lint.detector.api.XmlContext;
import com.android.utils.Pair;
import com.android.utils.SdkUtils;
import com.google.common.collect.Maps;

import org.eclipse.andmore.AndmoreAndroidPlugin;
import org.eclipse.andmore.AdtUtils;
import org.eclipse.andmore.internal.editors.layout.LayoutEditorDelegate;
import org.eclipse.andmore.internal.editors.layout.uimodel.UiViewElementNode;
import org.eclipse.andmore.internal.preferences.AdtPrefs;
import org.eclipse.andmore.internal.project.BaseProjectHelper;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.DefaultErrorHandlingPolicies;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.batch.CompilationUnit;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.parser.Parser;
import org.eclipse.jdt.internal.compiler.problem.AbortCompilation;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;
import org.eclipse.jdt.internal.compiler.problem.ProblemReporter;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import lombok.ast.ecj.EcjTreeConverter;
import lombok.ast.grammar.ParseProblem;
import lombok.ast.grammar.Source;

/**
 * Eclipse implementation for running lint on workspace files and projects.
 */
@SuppressWarnings("restriction") // DOM model
public class EclipseLintClient extends LintClient {
    static final String MARKER_CHECKID_PROPERTY = "checkid";    //$NON-NLS-1$
    private static final String MODEL_PROPERTY = "model";       //$NON-NLS-1$
    private final List<? extends IResource> mResources;
    private final IDocument mDocument;
    private boolean mWasFatal;
    private boolean mFatalOnly;
    private EclipseJavaParser mJavaParser;
    private boolean mCollectNodes;
    private Map<Node, IMarker> mNodeMap;

    /**
     * Creates a new {@link EclipseLintClient}.
     *
     * @param registry the associated detector registry
     * @param resources the associated resources (project, file or null)
     * @param document the associated document, or null if the {@code resource}
     *            param is not a file
     * @param fatalOnly whether only fatal issues should be reported (and therefore checked)
     */
    public EclipseLintClient(IssueRegistry registry, List<? extends IResource> resources,
            IDocument document, boolean fatalOnly) {
        mResources = resources;
        mDocument = document;
        mFatalOnly = fatalOnly;
    }

    /**
     * Returns true if lint should only check fatal issues
     *
     * @return true if lint should only check fatal issues
     */
    public boolean isFatalOnly() {
        return mFatalOnly;
    }

    /**
     * Sets whether the lint client should store associated XML nodes for each
     * reported issue
     *
     * @param collectNodes if true, collect node positions for errors in XML
     *            files, retrievable via the {@link #getIssueForNode} method
     */
    public void setCollectNodes(boolean collectNodes) {
        mCollectNodes = collectNodes;
    }

    /**
     * Returns one of the issues for the given node (there could be more than one)
     *
     * @param node the node to look up lint issues for
     * @return the marker for one of the issues found for the given node
     */
    @Nullable
    public IMarker getIssueForNode(@NonNull UiViewElementNode node) {
        if (mNodeMap != null) {
            return mNodeMap.get(node.getXmlNode());
        }

        return null;
    }

    /**
     * Returns a collection of nodes that have one or more lint warnings
     * associated with them (retrievable via
     * {@link #getIssueForNode(UiViewElementNode)})
     *
     * @return a collection of nodes, which should <b>not</b> be modified by the
     *         caller
     */
    @Nullable
    public Collection<Node> getIssueNodes() {
        if (mNodeMap != null) {
            return mNodeMap.keySet();
        }

        return null;
    }

    // ----- Extends LintClient -----

    @Override
    public void log(@NonNull Severity severity, @Nullable Throwable exception,
            @Nullable String format, @Nullable Object... args) {
        if (exception == null) {
            AndmoreAndroidPlugin.log(IStatus.WARNING, format, args);
        } else {
            AndmoreAndroidPlugin.log(exception, format, args);
        }
    }

    @Override
    public XmlParser getXmlParser() {
        return new XmlParser() {
            @Override
            public Document parseXml(@NonNull XmlContext context) {
                // Map File to IFile
                IFile file = AdtUtils.fileToIFile(context.file);
                if (file == null || !file.exists()) {
                    String path = context.file.getPath();
                    AndmoreAndroidPlugin.log(IStatus.ERROR, "Can't find file %1$s in workspace", path);
                    return null;
                }

                IStructuredModel model = null;
                try {
                    IModelManager modelManager = StructuredModelManager.getModelManager();
                    if (modelManager == null) {
                        // This can happen if incremental lint is running right as Eclipse is
                        // shutting down
                        return null;
                    }
                    model = modelManager.getModelForRead(file);
                    if (model instanceof IDOMModel) {
                        context.setProperty(MODEL_PROPERTY, model);
                        IDOMModel domModel = (IDOMModel) model;
                        return domModel.getDocument();
                    }
                } catch (IOException e) {
                    AndmoreAndroidPlugin.log(e, "Cannot read XML file");
                } catch (CoreException e) {
                    AndmoreAndroidPlugin.log(e, null);
                }

                return null;
            }

            @Override
            public @NonNull Location getLocation(@NonNull XmlContext context, @NonNull Node node) {
                IStructuredModel model = (IStructuredModel) context.getProperty(MODEL_PROPERTY);
                return new LazyLocation(context.file, model.getStructuredDocument(),
                        (IndexedRegion) node);
            }

            @Override
            public @NonNull Location getLocation(@NonNull XmlContext context, @NonNull Node node,
                    int start, int end) {
                IndexedRegion region = (IndexedRegion) node;
                int nodeStart = region.getStartOffset();

                IStructuredModel model = (IStructuredModel) context.getProperty(MODEL_PROPERTY);
                // Get line number
                LazyLocation location = new LazyLocation(context.file,
                        model.getStructuredDocument(), region);
                int line = location.getStart().getLine();

                Position startPos = new DefaultPosition(line, -1, nodeStart + start);
                Position endPos = new DefaultPosition(line, -1, nodeStart + end);
                return Location.create(context.file, startPos, endPos);
            }

            @Override
            public int getNodeStartOffset(@NonNull XmlContext context, @NonNull Node node) {
                IndexedRegion region = (IndexedRegion) node;
                return region.getStartOffset();
            }

            @Override
            public int getNodeEndOffset(@NonNull XmlContext context, @NonNull Node node) {
                IndexedRegion region = (IndexedRegion) node;
                return region.getEndOffset();
            }

            @Override
            public @NonNull Handle createLocationHandle(final @NonNull XmlContext context,
                    final @NonNull Node node) {
                IStructuredModel model = (IStructuredModel) context.getProperty(MODEL_PROPERTY);
                return new LazyLocation(context.file, model.getStructuredDocument(),
                        (IndexedRegion) node);
            }

            @Override
            public void dispose(@NonNull XmlContext context, @NonNull Document document) {
                IStructuredModel model = (IStructuredModel) context.getProperty(MODEL_PROPERTY);
                assert model != null : context.file;
                if (model != null) {
                    model.releaseFromRead();
                }
            }

            @Override
            public Location getNameLocation(@NonNull XmlContext context, @NonNull Node node) {
                IndexedRegion region = (IndexedRegion) node;

                IStructuredModel model = (IStructuredModel) context.getProperty(MODEL_PROPERTY);
                // Get line number
                LazyLocation location = new LazyLocation(context.file,
                        model.getStructuredDocument(), region);
                int line = location.getStart().getLine();

                Position startPos = new DefaultPosition(line, -1, region.getStartOffset());
                Position endPos = new DefaultPosition(line, -1, region.getEndOffset());

                return Location.create(context.file, startPos, endPos);
            }

            @Override
            public Location getValueLocation(@NonNull XmlContext context, @NonNull Attr attribute) {
                IndexedRegion region = (IndexedRegion) attribute;

                IStructuredModel model = (IStructuredModel) context.getProperty(MODEL_PROPERTY);
                // Get line number
                LazyLocation location = new LazyLocation(context.file,
                        model.getStructuredDocument(), region);
                int line = location.getStart().getLine();

                Position startPos = new DefaultPosition(line, -1, region.getStartOffset());
                Position endPos = new DefaultPosition(line, -1, region.getEndOffset());

                return Location.create(context.file, startPos, endPos);
            }
        };
    }

    @Override
    public JavaParser getJavaParser(@Nullable Project project) {
        if (mJavaParser == null) {
            mJavaParser = new EclipseJavaParser();
        }

        return mJavaParser;
    }

    // Cache for {@link getProject}
    private IProject mLastEclipseProject;
    private Project mLastLintProject;

    private IProject getProject(Project project) {
        if (project == mLastLintProject) {
            return mLastEclipseProject;
        }

        mLastLintProject = project;
        mLastEclipseProject = null;

        if (mResources != null) {
            if (mResources.size() == 1) {
                IProject p = mResources.get(0).getProject();
                mLastEclipseProject = p;
                return p;
            }

            IProject last = null;
            for (IResource resource : mResources) {
                IProject p = resource.getProject();
                if (p != last) {
                    if (project.getDir().equals(AdtUtils.getAbsolutePath(p).toFile())) {
                        mLastEclipseProject = p;
                        return p;
                    }
                    last = p;
                }
            }
        }

        return null;
    }

    @Override
    @NonNull
    public String getProjectName(@NonNull Project project) {
        // Initialize the lint project's name to the name of the Eclipse project,
        // which might differ from the directory name
        IProject eclipseProject = getProject(project);
        if (eclipseProject != null) {
            return eclipseProject.getName();
        }

        return super.getProjectName(project);
    }

    /**
     * Same as {@link #getConfiguration(Project)}, but {@code project} can be
     * null in which case the global configuration is returned.
     *
     * @param project the project to look up
     * @return a corresponding configuration
     */
    @NonNull
    public Configuration getConfigurationFor(@Nullable Project project) {
        if (project != null) {
            IProject eclipseProject = getProject(project);
            if (eclipseProject != null) {
                return ProjectLintConfiguration.get(this, eclipseProject, mFatalOnly);
            }
        }

        return GlobalLintConfiguration.get();
    }

    @Override
    public void report(@NonNull Context context, @NonNull Issue issue, @NonNull Severity s,
            @Nullable Location location,
            @NonNull String message, TextFormat arg5) {
        int severity = getMarkerSeverity(s);
        IMarker marker = null;
        if (location != null) {
            Position startPosition = location.getStart();
            if (startPosition == null) {
                if (location.getFile() != null) {
                    IResource resource = AdtUtils.fileToResource(location.getFile());
                    if (resource != null && resource.isAccessible()) {
                        marker = BaseProjectHelper.markResource(resource, MARKER_LINT,
                                message, 0, severity);
                    }
                }
            } else {
                Position endPosition = location.getEnd();
                int line = startPosition.getLine() + 1; // Marker API is 1-based
                IFile file = AdtUtils.fileToIFile(location.getFile());
                if (file != null && file.isAccessible()) {
                    Pair<Integer, Integer> r = getRange(file, mDocument,
                            startPosition, endPosition);
                    int startOffset = r.getFirst();
                    int endOffset = r.getSecond();
                    marker = BaseProjectHelper.markResource(file, MARKER_LINT,
                            message, line, startOffset, endOffset, severity);
                }
            }
        }

        if (marker == null) {
            marker = BaseProjectHelper.markResource(mResources.get(0), MARKER_LINT,
                        message, 0, severity);
        }

        if (marker != null) {
            // Store marker id such that we can recognize it from the suppress quickfix
            try {
                marker.setAttribute(MARKER_CHECKID_PROPERTY, issue.getId());
            } catch (CoreException e) {
                AndmoreAndroidPlugin.log(e, null);
            }
        }

        if (s == Severity.FATAL) {
            mWasFatal = true;
        }

        if (mCollectNodes && location != null && marker != null) {
            if (location instanceof LazyLocation) {
                LazyLocation l = (LazyLocation) location;
                IndexedRegion region = l.mRegion;
                if (region instanceof Node) {
                    Node node = (Node) region;
                    if (node instanceof Attr) {
                        node = ((Attr) node).getOwnerElement();
                    }
                    if (mNodeMap == null) {
                        mNodeMap = new WeakHashMap<Node, IMarker>();
                    }
                    IMarker prev = mNodeMap.get(node);
                    if (prev != null) {
                        // Only replace the node if this node has higher priority
                        int prevSeverity = prev.getAttribute(IMarker.SEVERITY, 0);
                        if (prevSeverity < severity) {
                            mNodeMap.put(node, marker);
                        }
                    } else {
                        mNodeMap.put(node, marker);
                    }
                }
            }
        }
    }

    @Override
    @Nullable
    public File findResource(@NonNull String relativePath) {
        // Look within the $ANDROID_SDK
        String sdkFolder = AdtPrefs.getPrefs().getOsSdkFolder();
        if (sdkFolder != null) {
            File file = new File(sdkFolder, relativePath);
            if (file.exists()) {
                return file;
            }
        }

        return null;
    }

    /**
     * Clears any lint markers from the given resource (project, folder or file)
     *
     * @param resource the resource to remove markers from
     */
    public static void clearMarkers(@NonNull IResource resource) {
        clearMarkers(Collections.singletonList(resource));
    }

    /** Clears any lint markers from the given list of resource (project, folder or file) */
    static void clearMarkers(List<? extends IResource> resources) {
        for (IResource resource : resources) {
            try {
                if (resource.isAccessible()) {
                    resource.deleteMarkers(MARKER_LINT, false, IResource.DEPTH_INFINITE);
                }
            } catch (CoreException e) {
                AndmoreAndroidPlugin.log(e, null);
            }
        }

        IEditorPart activeEditor = AdtUtils.getActiveEditor();
        LayoutEditorDelegate delegate = LayoutEditorDelegate.fromEditor(activeEditor);
        if (delegate != null) {
            delegate.getGraphicalEditor().getLayoutActionBar().updateErrorIndicator();
        }
    }

    /**
     * Removes all markers of the given id from the given resource.
     *
     * @param resource the resource to remove markers from (file or project, or
     *            null for all open projects)
     * @param id the id for the issue whose markers should be deleted
     */
    public static void removeMarkers(IResource resource, String id) {
        if (resource == null) {
            IJavaProject[] androidProjects = BaseProjectHelper.getAndroidProjects(null);
            for (IJavaProject project : androidProjects) {
                IProject p = project.getProject();
                if (p != null) {
                    // Recurse, but with a different parameter so it will not continue recursing
                    removeMarkers(p, id);
                }
            }
            return;
        }
        IMarker[] markers = getMarkers(resource);
        for (IMarker marker : markers) {
            if (id.equals(getId(marker))) {
                try {
                    marker.delete();
                } catch (CoreException e) {
                    AndmoreAndroidPlugin.log(e, null);
                }
            }
        }
    }

    /**
     * Returns the lint marker for the given resource (which may be a project, folder or file)
     *
     * @param resource the resource to be checked, typically a source file
     * @return an array of markers, possibly empty but never null
     */
    public static IMarker[] getMarkers(IResource resource) {
        try {
            if (resource.isAccessible()) {
                return resource.findMarkers(MARKER_LINT, false, IResource.DEPTH_INFINITE);
            }
        } catch (CoreException e) {
            AndmoreAndroidPlugin.log(e, null);
        }

        return new IMarker[0];
    }

    private static int getMarkerSeverity(Severity severity) {
        switch (severity) {
            case INFORMATIONAL:
                return IMarker.SEVERITY_INFO;
            case WARNING:
                return IMarker.SEVERITY_WARNING;
            case FATAL:
            case ERROR:
            default:
                return IMarker.SEVERITY_ERROR;
        }
    }

    private static Pair<Integer, Integer> getRange(IFile file, IDocument doc,
            Position startPosition, Position endPosition) {
        int startOffset = startPosition.getOffset();
        int endOffset = endPosition != null ? endPosition.getOffset() : -1;
        if (endOffset != -1) {
            // Attribute ranges often include trailing whitespace; trim this up
            if (doc == null) {
                IDocumentProvider provider = new TextFileDocumentProvider();
                try {
                    provider.connect(file);
                    doc = provider.getDocument(file);
                    if (doc != null) {
                        return adjustOffsets(doc, startOffset, endOffset);
                    }
                } catch (Exception e) {
                    AndmoreAndroidPlugin.log(e, "Can't find range information for %1$s", file.getName());
                } finally {
                    provider.disconnect(file);
                }
            } else {
                return adjustOffsets(doc, startOffset, endOffset);
            }
        }

        return Pair.of(startOffset, startOffset);
    }

    /**
     * Trim off any trailing space on the given offset range in the given
     * document, and don't span multiple lines on ranges since it makes (for
     * example) the XML editor just glow with yellow underlines for all the
     * attributes etc. Highlighting just the element beginning gets the point
     * across. It also makes it more obvious where there are warnings on both
     * the overall element and on individual attributes since without this the
     * warnings on attributes would just overlap with the whole-element
     * highlighting.
     */
    private static Pair<Integer, Integer> adjustOffsets(IDocument doc, int startOffset,
            int endOffset) {
        int originalStart = startOffset;
        int originalEnd = endOffset;

        if (doc != null) {
            while (endOffset > startOffset && endOffset < doc.getLength()) {
                try {
                    if (!Character.isWhitespace(doc.getChar(endOffset - 1))) {
                        break;
                    } else {
                        endOffset--;
                    }
                } catch (BadLocationException e) {
                    // Pass - we've already validated offset range above
                    break;
                }
            }

            // Also don't span lines
            int lineEnd = startOffset;
            while (lineEnd < endOffset) {
                try {
                    char c = doc.getChar(lineEnd);
                    if (c == '\n' || c == '\r') {
                        endOffset = lineEnd;
                        if (endOffset > 0 && doc.getChar(endOffset - 1) == '\r') {
                            endOffset--;
                        }
                        break;
                    }
                } catch (BadLocationException e) {
                    // Pass - we've already validated offset range above
                    break;
                }
                lineEnd++;
            }
        }

        if (startOffset >= endOffset) {
            // Selecting nothing (for example, for the mangled CRLF delimiter issue selecting
            // just the newline)
            // In that case, use the real range
            return Pair.of(originalStart, originalEnd);
        }

        return Pair.of(startOffset, endOffset);
    }

    /**
     * Returns true if a fatal error was encountered
     *
     * @return true if a fatal error was encountered
     */
    public boolean hasFatalErrors() {
        return mWasFatal;
    }

    /**
     * Describe the issue for the given marker
     *
     * @param marker the marker to look up
     * @return a full description of the corresponding issue, never null
     */
    public static String describe(IMarker marker) {
        IssueRegistry registry = getRegistry();
        String markerId = getId(marker);
        Issue issue = registry.getIssue(markerId);
        if (issue == null) {
            return "";
        }

        String summary = issue.getBriefDescription(TextFormat.TEXT);
        String explanation = issue.getExplanation(TextFormat.TEXT);

        StringBuilder sb = new StringBuilder(summary.length() + explanation.length() + 20);
        try {
            sb.append((String) marker.getAttribute(IMarker.MESSAGE));
            sb.append('\n').append('\n');
        } catch (CoreException e) {
        }
        sb.append("Issue: ");
        sb.append(summary);
        sb.append('\n');
        sb.append("Id: ");
        sb.append(issue.getId());
        sb.append('\n').append('\n');
        sb.append(explanation);

        if (issue.getMoreInfo() != null) {
            sb.append('\n').append('\n');
            sb.append(issue.getMoreInfo());
        }

        return sb.toString();
    }

    /**
     * Returns the id for the given marker
     *
     * @param marker the marker to look up
     * @return the corresponding issue id, or null
     */
    public static String getId(IMarker marker) {
        try {
            return (String) marker.getAttribute(MARKER_CHECKID_PROPERTY);
        } catch (CoreException e) {
            return null;
        }
    }

    /**
     * Shows the given marker in the editor
     *
     * @param marker the marker to be shown
     */
    public static void showMarker(IMarker marker) {
        IRegion region = null;
        try {
            int start = marker.getAttribute(IMarker.CHAR_START, -1);
            int end = marker.getAttribute(IMarker.CHAR_END, -1);
            if (start >= 0 && end >= 0) {
                region = new org.eclipse.jface.text.Region(start, end - start);
            }

            IResource resource = marker.getResource();
            if (resource instanceof IFile) {
                IEditorPart editor =
                        AndmoreAndroidPlugin.openFile((IFile) resource, region, true /* showEditorTab */);
                if (editor != null) {
                    IDE.gotoMarker(editor, marker);
                }
            }
        } catch (PartInitException ex) {
            AndmoreAndroidPlugin.log(ex, null);
        }
    }

    /**
     * Show a dialog with errors for the given file
     *
     * @param shell the parent shell to attach the dialog to
     * @param file the file to show the errors for
     * @param editor the editor for the file, if known
     */
    public static void showErrors(
            @NonNull Shell shell,
            @NonNull IFile file,
            @Nullable IEditorPart editor) {
        LintListDialog dialog = new LintListDialog(shell, file, editor);
        dialog.open();
    }

    @Override
    public @NonNull String readFile(@NonNull File f) {
        // Map File to IFile
        IFile file = AdtUtils.fileToIFile(f);
        if (file == null || !file.exists()) {
            String path = f.getPath();
            AndmoreAndroidPlugin.log(IStatus.ERROR, "Can't find file %1$s in workspace", path);
            return readPlainFile(f);
        }

        if (SdkUtils.endsWithIgnoreCase(file.getName(), DOT_XML)) {
            IStructuredModel model = null;
            try {
                IModelManager modelManager = StructuredModelManager.getModelManager();
                model = modelManager.getModelForRead(file);
                return model.getStructuredDocument().get();
            } catch (IOException e) {
                AndmoreAndroidPlugin.log(e, "Cannot read XML file");
            } catch (CoreException e) {
                AndmoreAndroidPlugin.log(e, null);
            } finally {
                if (model != null) {
                    // TODO: This may be too early...
                    model.releaseFromRead();
                }
            }
        }

        return readPlainFile(f);
    }

    private String readPlainFile(File file) {
        try {
            return LintUtils.getEncodedString(this, file);
        } catch (IOException e) {
            return ""; //$NON-NLS-1$
        }
    }

    private Map<Project, ClassPathInfo> mProjectInfo;

    @Override
    @NonNull
    protected ClassPathInfo getClassPath(@NonNull Project project) {
        ClassPathInfo info;
        if (mProjectInfo == null) {
            mProjectInfo = Maps.newHashMap();
            info = null;
        } else {
            info = mProjectInfo.get(project);
        }

        if (info == null) {
            List<File> sources = null;
            List<File> classes = null;
            List<File> libraries = null;

            IProject p = getProject(project);
            if (p != null) {
                try {
                    IJavaProject javaProject = BaseProjectHelper.getJavaProject(p);

                    // Output path
                    File file = workspacePathToFile(javaProject.getOutputLocation());
                    classes = Collections.singletonList(file);

                    // Source path
                    IClasspathEntry[] entries = javaProject.getRawClasspath();
                    sources = new ArrayList<File>(entries.length);
                    libraries = new ArrayList<File>(entries.length);
                    for (int i = 0; i < entries.length; i++) {
                        IClasspathEntry entry = entries[i];
                        int kind = entry.getEntryKind();

                        if (kind == IClasspathEntry.CPE_VARIABLE) {
                            entry = JavaCore.getResolvedClasspathEntry(entry);
                            if (entry == null) {
                                // It's possible that the variable is no longer valid; ignore
                                continue;
                            }
                            kind = entry.getEntryKind();
                        }

                        if (kind == IClasspathEntry.CPE_SOURCE) {
                            sources.add(workspacePathToFile(entry.getPath()));
                        } else if (kind == IClasspathEntry.CPE_LIBRARY) {
                            libraries.add(entry.getPath().toFile());
                        }
                        // Note that we ignore IClasspathEntry.CPE_CONTAINER:
                        // Normal Android Eclipse projects supply both
                        //   AdtConstants.CONTAINER_FRAMEWORK
                        // and
                        //   AdtConstants.CONTAINER_LIBRARIES
                        // here. We ignore the framework classes for obvious reasons,
                        // but we also ignore the library container because lint will
                        // process the libraries differently. When Eclipse builds a
                        // project, it gets the .jar output of the library projects
                        // from this container, which means it doesn't have to process
                        // the library sources. Lint on the other hand wants to process
                        // the source code, so instead it actually looks at the
                        // project.properties file to find the libraries, and then it
                        // iterates over all the library projects in turn and analyzes
                        // those separately (but passing the main project for context,
                        // such that the including project's manifest declarations
                        // are used for data like minSdkVersion level).
                        //
                        // Note that this container will also contain *other*
                        // libraries (Java libraries, not library projects) that we
                        // *should* include. However, we can't distinguish these
                        // class path entries from the library project jars,
                        // so instead of looking at these, we simply listFiles() in
                        // the libs/ folder after processing the classpath info
                    }

                    // Add in libraries
                    File libs = new File(project.getDir(), FD_NATIVE_LIBS);
                    if (libs.isDirectory()) {
                        File[] jars = libs.listFiles();
                        if (jars != null) {
                            for (File jar : jars) {
                                if (SdkUtils.endsWith(jar.getPath(), DOT_JAR)) {
                                    libraries.add(jar);
                                }
                            }
                        }
                    }
                } catch (CoreException e) {
                    AndmoreAndroidPlugin.log(e, null);
                }
            }

            if (sources == null) {
                sources = super.getClassPath(project).getSourceFolders();
            }
            if (classes == null) {
                classes = super.getClassPath(project).getClassFolders();
            }
            if (libraries == null) {
                libraries = super.getClassPath(project).getLibraries(true);
            }

            info = new ClassPathInfo(sources, classes, libraries, libraries, null);
            mProjectInfo.put(project, info);
        }

        return info;
    }

    /**
     * Returns the registry of issues to check from within Eclipse.
     *
     * @return the issue registry to use to access detectors and issues
     */
    public static IssueRegistry getRegistry() {
        return new BuiltinIssueRegistry();
    }

    @Override
    public @NonNull Class<? extends Detector> replaceDetector(
            @NonNull Class<? extends Detector> detectorClass) {
        return detectorClass;
    }

    @Override
    @NonNull
    public IAndroidTarget[] getTargets() {
        Sdk sdk = Sdk.getCurrent();
        if (sdk != null) {
            return sdk.getTargets().toArray(new IAndroidTarget[0]);
        } else {
            return new IAndroidTarget[0];
        }
    }

    private boolean mSearchForSuperClasses;

    /**
     * Sets whether this client should search for super types on its own. This
     * is typically not needed when doing a full lint run (because lint will
     * look at all classes and libraries), but is useful during incremental
     * analysis when lint is only looking at a subset of classes. In that case,
     * we want to use Eclipse's data structures for super classes.
     *
     * @param search whether to use a custom Eclipse search for super class
     *            names
     */
    public void setSearchForSuperClasses(boolean search) {
        mSearchForSuperClasses = search;
    }

    /**
     * Whether this lint client is searching for super types. See
     * {@link #setSearchForSuperClasses(boolean)} for details.
     *
     * @return whether the client will search for super types
     */
    public boolean getSearchForSuperClasses() {
        return mSearchForSuperClasses;
    }

    @Override
    @Nullable
    public String getSuperClass(@NonNull Project project, @NonNull String name) {
        if (!mSearchForSuperClasses) {
            // Super type search using the Eclipse index is potentially slow, so
            // only do this when necessary
            return null;
        }

        IProject eclipseProject = getProject(project);
        if (eclipseProject == null) {
            return null;
        }

        try {
            IJavaProject javaProject = BaseProjectHelper.getJavaProject(eclipseProject);
            if (javaProject == null) {
                return null;
            }

            String typeFqcn = ClassContext.getFqcn(name);
            IType type = javaProject.findType(typeFqcn);
            if (type != null) {
                ITypeHierarchy hierarchy = type.newSupertypeHierarchy(new NullProgressMonitor());
                IType superType = hierarchy.getSuperclass(type);
                if (superType != null) {
                    String key = superType.getKey();
                    if (!key.isEmpty()
                            && key.charAt(0) == 'L'
                            && key.charAt(key.length() - 1) == ';') {
                        return key.substring(1, key.length() - 1);
                    } else {
                        String fqcn = superType.getFullyQualifiedName();
                        return ClassContext.getInternalName(fqcn);
                    }
                }
            }
        } catch (JavaModelException e) {
            log(Severity.INFORMATIONAL, e, null);
        } catch (CoreException e) {
            log(Severity.INFORMATIONAL, e, null);
        }

        return null;
    }

    @Override
    @Nullable
    public Boolean isSubclassOf(
            @NonNull Project project,
            @NonNull String name, @NonNull
            String superClassName) {
        if (!mSearchForSuperClasses) {
            // Super type search using the Eclipse index is potentially slow, so
            // only do this when necessary
            return null;
        }

        IProject eclipseProject = getProject(project);
        if (eclipseProject == null) {
            return null;
        }

        try {
            IJavaProject javaProject = BaseProjectHelper.getJavaProject(eclipseProject);
            if (javaProject == null) {
                return null;
            }

            String typeFqcn = ClassContext.getFqcn(name);
            IType type = javaProject.findType(typeFqcn);
            if (type != null) {
                ITypeHierarchy hierarchy = type.newSupertypeHierarchy(new NullProgressMonitor());
                IType[] allSupertypes = hierarchy.getAllSuperclasses(type);
                if (allSupertypes != null) {
                    String target = 'L' + superClassName + ';';
                    for (IType superType : allSupertypes) {
                        if (target.equals(superType.getKey())) {
                            return Boolean.TRUE;
                        }
                    }
                    return Boolean.FALSE;
                }
            }
        } catch (JavaModelException e) {
            log(Severity.INFORMATIONAL, e, null);
        } catch (CoreException e) {
            log(Severity.INFORMATIONAL, e, null);
        }

        return null;
    }

    private static class LazyLocation extends Location implements Location.Handle {
        private final IStructuredDocument mDocument;
        private final IndexedRegion mRegion;
        private Position mStart;
        private Position mEnd;

        public LazyLocation(File file, IStructuredDocument document, IndexedRegion region) {
            super(file, null /*start*/, null /*end*/);
            mDocument = document;
            mRegion = region;
        }

        @Override
        public Position getStart() {
            if (mStart == null) {
                int line = -1;
                int column = -1;
                int offset = mRegion.getStartOffset();

                if (mRegion instanceof org.w3c.dom.Text && mDocument != null) {
                    // For text nodes, skip whitespace prefix, if any
                    for (int i = offset;
                            i < mRegion.getEndOffset() && i < mDocument.getLength(); i++) {
                        try {
                            char c = mDocument.getChar(i);
                            if (!Character.isWhitespace(c)) {
                                offset = i;
                                break;
                            }
                        } catch (BadLocationException e) {
                            break;
                        }
                    }
                }

                if (mDocument != null && offset < mDocument.getLength()) {
                    line = mDocument.getLineOfOffset(offset);
                    column = -1;
                    try {
                        int lineOffset = mDocument.getLineOffset(line);
                        column = offset - lineOffset;
                    } catch (BadLocationException e) {
                        AndmoreAndroidPlugin.log(e, null);
                    }
                }

                mStart = new DefaultPosition(line, column, offset);
            }

            return mStart;
        }

        @Override
        public Position getEnd() {
            if (mEnd == null) {
                mEnd = new DefaultPosition(-1, -1, mRegion.getEndOffset());
            }

            return mEnd;
        }

        @Override
        public @NonNull Location resolve() {
            return this;
        }
    }

    private static class EclipseJavaParser extends JavaParser {
        private static final boolean USE_ECLIPSE_PARSER = true;
        private final Parser mParser;

        EclipseJavaParser() {
            if (USE_ECLIPSE_PARSER) {
                CompilerOptions options = new CompilerOptions();
                // Always using JDK 7 rather than basing it on project metadata since we
                // don't do compilation error validation in lint (we leave that to the IDE's
                // error parser or the command line build's compilation step); we want an
                // AST that is as tolerant as possible.
                options.complianceLevel = ClassFileConstants.JDK1_7;
                options.sourceLevel = ClassFileConstants.JDK1_7;
                options.targetJDK = ClassFileConstants.JDK1_7;
                options.parseLiteralExpressionsAsConstants = true;
                ProblemReporter problemReporter = new ProblemReporter(
                        DefaultErrorHandlingPolicies.exitOnFirstError(),
                        options,
                        new DefaultProblemFactory());
                mParser = new Parser(problemReporter, options.parseLiteralExpressionsAsConstants);
                mParser.javadocParser.checkDocComment = false;
            } else {
                mParser = null;
            }
        }

        @Override
        public void prepareJavaParse(@NonNull List<JavaContext> contexts) {
            // TODO: Use batch compiler from lint-cli.jar
        }

        @Override
        public lombok.ast.Node parseJava(@NonNull JavaContext context) {
            if (USE_ECLIPSE_PARSER) {
                // Use Eclipse's compiler
                EcjTreeConverter converter = new EcjTreeConverter();
                String code = context.getContents();

                CompilationUnit sourceUnit = new CompilationUnit(code.toCharArray(),
                        context.file.getName(), "UTF-8"); //$NON-NLS-1$
                CompilationResult compilationResult = new CompilationResult(sourceUnit, 0, 0, 0);
                CompilationUnitDeclaration unit = null;
                try {
                    unit = mParser.parse(sourceUnit, compilationResult);
                } catch (AbortCompilation e) {
                    // No need to report Java parsing errors while running in Eclipse.
                    // Eclipse itself will already provide problem markers for these files,
                    // so all this achieves is creating "multiple annotations on this line"
                    // tooltips instead.
                    return null;
                }
                if (unit == null) {
                    return null;
                }

                try {
                    converter.visit(code, unit);
                    List<? extends lombok.ast.Node> nodes = converter.getAll();

                    // There could be more than one node when there are errors; pick out the
                    // compilation unit node
                    for (lombok.ast.Node node : nodes) {
                        if (node instanceof lombok.ast.CompilationUnit) {
                            return node;
                        }
                    }

                    return null;
                } catch (Throwable t) {
                    AndmoreAndroidPlugin.log(t, "Failed converting ECJ parse tree to Lombok for file %1$s",
                            context.file.getPath());
                    return null;
                }
            } else {
                // Use Lombok for now
                Source source = new Source(context.getContents(), context.file.getName());
                List<lombok.ast.Node> nodes = source.getNodes();

                // Don't analyze files containing errors
                List<ParseProblem> problems = source.getProblems();
                if (problems != null && problems.size() > 0) {
                    /* Silently ignore the errors. There are still some bugs in Lombok/Parboiled
                     * (triggered if you run lint on the AOSP framework directory for example),
                     * and having these show up as fatal errors when it's really a tool bug
                     * is bad. To make matters worse, the error messages aren't clear:
                     * http://code.google.com/p/projectlombok/issues/detail?id=313
                    for (ParseProblem problem : problems) {
                        lombok.ast.Position position = problem.getPosition();
                        Location location = Location.create(context.file,
                                context.getContents(), position.getStart(), position.getEnd());
                        String message = problem.getMessage();
                        context.report(
                                IssueRegistry.PARSER_ERROR, location,
                                message,
                                null);

                    }
                    */
                    return null;
                }

                // There could be more than one node when there are errors; pick out the
                // compilation unit node
                for (lombok.ast.Node node : nodes) {
                    if (node instanceof lombok.ast.CompilationUnit) {
                        return node;
                    }
                }
                return null;
            }
        }

        @Override
        public @NonNull Location getLocation(@NonNull JavaContext context,
                @NonNull lombok.ast.Node node) {
            lombok.ast.Position position = node.getPosition();
            return Location.create(context.file, context.getContents(),
                    position.getStart(), position.getEnd());
        }

        @Override
        public @NonNull Handle createLocationHandle(@NonNull JavaContext context,
                @NonNull lombok.ast.Node node) {
            return new LocationHandle(context.file, node);
        }

        @Override
        public void dispose(@NonNull JavaContext context,
                @NonNull lombok.ast.Node compilationUnit) {
        }

        @Override
        @Nullable
        public ResolvedNode resolve(@NonNull JavaContext context,
                @NonNull lombok.ast.Node node) {
            return null;
        }

        @Override
        @Nullable
        public TypeDescriptor getType(@NonNull JavaContext context,
                @NonNull lombok.ast.Node node) {
            return null;
        }

        @Override
        public Location getRangeLocation(JavaContext context, lombok.ast.Node from, int fromDelta, lombok.ast.Node to,
                int toDelta) {
            String contents = context.getContents();
            int start = Math.max(0,  from.getPosition().getStart() + fromDelta);
            int end = Math.max(contents == null ? Integer.MAX_VALUE : contents.length(), to.getPosition().getEnd() + toDelta);
            return Location.create(context.file, contents, start, end);
        }

        /* Handle for creating positions cheaply and returning full fledged locations later */
        private class LocationHandle implements Handle {
            private File mFile;
            private lombok.ast.Node mNode;
            private Object mClientData;

            public LocationHandle(File file, lombok.ast.Node node) {
                mFile = file;
                mNode = node;
            }

            @Override
            public @NonNull Location resolve() {
                lombok.ast.Position pos = mNode.getPosition();
                return Location.create(mFile, null /*contents*/, pos.getStart(), pos.getEnd());
            }

            @Override
            public void setClientData(@Nullable Object clientData) {
                mClientData = clientData;
            }

            @Override
            @Nullable
            public Object getClientData() {
                return mClientData;
            }
        }
    }
}


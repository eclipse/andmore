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

package org.eclipse.andmore.internal.editors;

import static com.android.SdkConstants.ANDROID_PKG;
import static com.android.SdkConstants.ANDROID_PREFIX;
import static com.android.SdkConstants.ANDROID_STYLE_RESOURCE_PREFIX;
import static com.android.SdkConstants.ANDROID_THEME_PREFIX;
import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_CLASS;
import static com.android.SdkConstants.ATTR_CONTEXT;
import static com.android.SdkConstants.ATTR_ID;
import static com.android.SdkConstants.ATTR_NAME;
import static com.android.SdkConstants.ATTR_ON_CLICK;
import static com.android.SdkConstants.CLASS_ACTIVITY;
import static com.android.SdkConstants.EXT_XML;
import static com.android.SdkConstants.FD_DOCS;
import static com.android.SdkConstants.FD_DOCS_REFERENCE;
import static com.android.SdkConstants.FN_RESOURCE_BASE;
import static com.android.SdkConstants.FN_RESOURCE_CLASS;
import static com.android.SdkConstants.NEW_ID_PREFIX;
import static com.android.SdkConstants.PREFIX_RESOURCE_REF;
import static com.android.SdkConstants.PREFIX_THEME_REF;
import static com.android.SdkConstants.STYLE_RESOURCE_PREFIX;
import static com.android.SdkConstants.TAG_RESOURCES;
import static com.android.SdkConstants.TAG_STYLE;
import static com.android.SdkConstants.TOOLS_URI;
import static com.android.SdkConstants.VIEW;
import static com.android.SdkConstants.VIEW_FRAGMENT;
import static com.android.xml.AndroidManifest.ATTRIBUTE_NAME;
import static com.android.xml.AndroidManifest.ATTRIBUTE_PACKAGE;
import static com.android.xml.AndroidManifest.NODE_ACTIVITY;
import static com.android.xml.AndroidManifest.NODE_SERVICE;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.annotations.VisibleForTesting;
import com.android.ide.common.resources.ResourceFile;
import com.android.ide.common.resources.ResourceFolder;
import com.android.ide.common.resources.ResourceRepository;
import com.android.ide.common.resources.ResourceUrl;
import com.android.ide.common.resources.configuration.FolderConfiguration;
import com.android.io.FileWrapper;
import com.android.io.IAbstractFile;
import com.android.io.IAbstractFolder;
import com.android.resources.ResourceFolderType;
import com.android.resources.ResourceType;
import com.android.sdklib.IAndroidTarget;
import com.android.utils.Pair;

import org.apache.xerces.parsers.DOMParser;
import org.apache.xerces.xni.Augmentations;
import org.apache.xerces.xni.NamespaceContext;
import org.apache.xerces.xni.QName;
import org.apache.xerces.xni.XMLAttributes;
import org.apache.xerces.xni.XMLLocator;
import org.apache.xerces.xni.XNIException;
import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.AdtUtils;
import org.eclipse.andmore.internal.editors.layout.LayoutEditorDelegate;
import org.eclipse.andmore.internal.editors.layout.gle2.GraphicalEditorPart;
import org.eclipse.andmore.internal.editors.manifest.ManifestEditor;
import org.eclipse.andmore.internal.editors.manifest.ManifestInfo;
import org.eclipse.andmore.internal.project.BaseProjectHelper;
import org.eclipse.andmore.internal.resources.ResourceHelper;
import org.eclipse.andmore.internal.resources.manager.ProjectResources;
import org.eclipse.andmore.internal.resources.manager.ResourceManager;
import org.eclipse.andmore.internal.sdk.AndroidTargetData;
import org.eclipse.andmore.internal.sdk.ProjectState;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.andmore.io.IFileWrapper;
import org.eclipse.andmore.io.IFolderWrapper;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICodeAssist;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.text.JavaWordFinder;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.actions.SelectionDispatchAction;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.eclipse.wst.sse.ui.StructuredTextEditor;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.eclipse.wst.xml.core.internal.regions.DOMRegionContext;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Class containing hyperlink resolvers for XML and Java files to jump to associated
 * resources -- Java Activity and Service classes, XML layout and string declarations,
 * image drawables, etc.
 */
@SuppressWarnings("restriction")
public class Hyperlinks {
    private static final String CATEGORY = "category";                            //$NON-NLS-1$
    private static final String ACTION = "action";                                //$NON-NLS-1$
    private static final String PERMISSION = "permission";                        //$NON-NLS-1$
    private static final String USES_PERMISSION = "uses-permission";              //$NON-NLS-1$
    private static final String CATEGORY_PKG_PREFIX = "android.intent.category."; //$NON-NLS-1$
    private static final String ACTION_PKG_PREFIX = "android.intent.action.";     //$NON-NLS-1$
    private static final String PERMISSION_PKG_PREFIX = "android.permission.";    //$NON-NLS-1$

    private Hyperlinks() {
        // Not instantiatable. This is a container class containing shared code
        // for the various inner classes that are actual hyperlink resolvers.
    }

    /**
     * Returns whether a string represents a valid fully qualified name for a view class.
     * Does not check for existence.
     */
    @VisibleForTesting
	public
    static boolean isViewClassName(String name) {
        int length = name.length();
        if (length < 2 || name.indexOf('.') == -1) {
            return false;
        }

        boolean lastWasDot = true;
        for (int i = 0; i < length; i++) {
            char c = name.charAt(i);
            if (lastWasDot) {
                if (!Character.isJavaIdentifierStart(c)) {
                    return false;
                }
                lastWasDot = false;
            } else {
                if (c == '.') {
                    lastWasDot = true;
                } else if (!Character.isJavaIdentifierPart(c)) {
                    return false;
                }
            }
        }

        return !lastWasDot;
    }

    /** Determines whether the given attribute <b>name</b> is linkable */
    private static boolean isAttributeNameLink(XmlContext context) {
        // We could potentially allow you to link to builtin Android properties:
        //   ANDROID_URI.equals(attribute.getNamespaceURI())
        // and then jump into the res/values/attrs.xml document that is available
        // in the SDK data directory (path found via
        // IAndroidTarget.getPath(IAndroidTarget.ATTRIBUTES)).
        //
        // For now, we're not doing that.
        //
        // We could also allow to jump into custom attributes in custom view
        // classes. Not yet implemented.

        return false;
    }

    /** Determines whether the given attribute <b>value</b> is linkable */
    private static boolean isAttributeValueLink(XmlContext context) {
        // Everything else here is attribute based
        Attr attribute = context.getAttribute();
        if (attribute == null) {
            return false;
        }

        if (isClassAttribute(context) || isOnClickAttribute(context)
                || isManifestName(context) || isStyleAttribute(context)) {
            return true;
        }

        String value = attribute.getValue();
        if (value.startsWith(NEW_ID_PREFIX)) {
            // It's a value -declaration-, nowhere else to jump
            // (though we could consider jumping to the R-file; would that
            // be helpful?)
            return !ATTR_ID.equals(attribute.getLocalName());
        }

        ResourceUrl resource = ResourceUrl.parse(value);
        if (resource != null) {
            return true;
        }

        return false;
    }

    /** Determines whether the given element <b>name</b> is linkable */
    private static boolean isElementNameLink(XmlContext context) {
        if (isClassElement(context)) {
            return true;
        }

        return false;
    }

    /**
     * Returns true if this node/attribute pair corresponds to a manifest reference to
     * an activity.
     */
    private static boolean isActivity(XmlContext context) {
        // Is this an <activity> or <service> in an AndroidManifest.xml file? If so, jump
        // to it
        Attr attribute = context.getAttribute();
        String tagName = context.getElement().getTagName();
        if (NODE_ACTIVITY.equals(tagName) && ATTRIBUTE_NAME.equals(attribute.getLocalName())
                && ANDROID_URI.equals(attribute.getNamespaceURI())) {
            return true;
        }

        return false;
    }

    /**
     * Returns true if this node/attribute pair corresponds to a manifest android:name reference
     */
    private static boolean isManifestName(XmlContext context) {
        Attr attribute = context.getAttribute();
        if (attribute != null && ATTRIBUTE_NAME.equals(attribute.getLocalName())
                && ANDROID_URI.equals(attribute.getNamespaceURI())) {
            if (getEditor() instanceof ManifestEditor) {
                return true;
            }
        }

        return false;
    }

    /**
     * Opens the declaration corresponding to an android:name reference in the
     * AndroidManifest.xml file
     */
    private static boolean openManifestName(IProject project, XmlContext context) {
        if (isActivity(context)) {
            String fqcn = getActivityClassFqcn(context);
            return AdtPlugin.openJavaClass(project, fqcn);
        } else if (isService(context)) {
            String fqcn = getServiceClassFqcn(context);
            return AdtPlugin.openJavaClass(project, fqcn);
        } else if (isBuiltinPermission(context)) {
            String permission = context.getAttribute().getValue();
            // Mutate something like android.permission.ACCESS_CHECKIN_PROPERTIES
            // into relative doc url android/Manifest.permission.html#ACCESS_CHECKIN_PROPERTIES
            assert permission.startsWith(PERMISSION_PKG_PREFIX);
            String relative = "android/Manifest.permission.html#" //$NON-NLS-1$
                    + permission.substring(PERMISSION_PKG_PREFIX.length());

            URL url = getDocUrl(relative);
            if (url != null) {
                AdtPlugin.openUrl(url);
                return true;
            } else {
                return false;
            }
        } else if (isBuiltinIntent(context)) {
            String intent = context.getAttribute().getValue();
            // Mutate something like android.intent.action.MAIN into
            // into relative doc url android/content/Intent.html#ACTION_MAIN
            String relative;
            if (intent.startsWith(ACTION_PKG_PREFIX)) {
                relative = "android/content/Intent.html#ACTION_" //$NON-NLS-1$
                        + intent.substring(ACTION_PKG_PREFIX.length());
            } else if (intent.startsWith(CATEGORY_PKG_PREFIX)) {
                relative = "android/content/Intent.html#CATEGORY_" //$NON-NLS-1$
                        + intent.substring(CATEGORY_PKG_PREFIX.length());
            } else {
                return false;
            }
            URL url = getDocUrl(relative);
            if (url != null) {
                AdtPlugin.openUrl(url);
                return true;
            } else {
                return false;
            }
        }

        return false;
    }

    /** Returns true if this represents a style attribute */
    private static boolean isStyleAttribute(XmlContext context) {
        String tag = context.getElement().getTagName();
        return TAG_STYLE.equals(tag);
    }

    /**
     * Returns true if this represents a {@code <view class="foo.bar.Baz">} class
     * attribute, or a {@code <fragment android:name="foo.bar.Baz">} class attribute
     */
    private static boolean isClassAttribute(XmlContext context) {
        Attr attribute = context.getAttribute();
        if (attribute == null) {
            return false;
        }
        String tag = context.getElement().getTagName();
        String attributeName = attribute.getLocalName();
        return ATTR_CLASS.equals(attributeName) && (VIEW.equals(tag) || VIEW_FRAGMENT.equals(tag))
                || ATTR_NAME.equals(attributeName) && VIEW_FRAGMENT.equals(tag)
                || (ATTR_CONTEXT.equals(attributeName)
                        && TOOLS_URI.equals(attribute.getNamespaceURI()));
    }

    /** Returns true if this represents an onClick attribute specifying a method handler */
    private static boolean isOnClickAttribute(XmlContext context) {
        Attr attribute = context.getAttribute();
        if (attribute == null) {
            return false;
        }
        return ATTR_ON_CLICK.equals(attribute.getLocalName()) && attribute.getValue().length() > 0;
    }

    /** Returns true if this represents a {@code <foo.bar.Baz>} custom view class element */
    private static boolean isClassElement(XmlContext context) {
        if (context.getAttribute() != null) {
            // Don't match the outer element if the user is hovering over a specific attribute
            return false;
        }
        // If the element looks like a fully qualified class name (e.g. it's a custom view
        // element) offer it as a link
        String tag = context.getElement().getTagName();
        return isViewClassName(tag);
    }

    /** Returns the FQCN for a class declaration at the given context */
    private static String getClassFqcn(XmlContext context) {
        if (isClassAttribute(context)) {
            String value = context.getAttribute().getValue();
            if (!value.isEmpty() && value.charAt(0) == '.') {
                IProject project = getProject();
                if (project != null) {
                    ManifestInfo info = ManifestInfo.get(project);
                    String pkg = info.getPackage();
                    if (pkg != null) {
                        value = pkg + value;
                    }
                }
            }
            return value;
        } else if (isClassElement(context)) {
            return context.getElement().getTagName();
        }

        return null;
    }

    /**
     * Returns true if this node/attribute pair corresponds to a manifest reference to
     * an service.
     */
    private static boolean isService(XmlContext context) {
        Attr attribute = context.getAttribute();
        Element node = context.getElement();

        // Is this an <activity> or <service> in an AndroidManifest.xml file? If so, jump to it
        String nodeName = node.getNodeName();
        if (NODE_SERVICE.equals(nodeName) && ATTRIBUTE_NAME.equals(attribute.getLocalName())
                && ANDROID_URI.equals(attribute.getNamespaceURI())) {
            return true;
        }

        return false;
    }

    /**
     * Returns a URL pointing to the Android reference documentation, either installed
     * locally or the one on android.com
     *
     * @param relative a relative url to append to the root url
     * @return a URL pointing to the documentation
     */
    private static URL getDocUrl(String relative) {
        // First try to find locally installed documentation
        File sdkLocation = new File(Sdk.getCurrent().getSdkOsLocation());
        File docs = new File(sdkLocation, FD_DOCS + File.separator + FD_DOCS_REFERENCE);
        try {
            if (docs.exists()) {
                String s = docs.toURI().toURL().toExternalForm();
                if (!s.endsWith("/")) { //$NON-NLS-1$
                    s += "/";           //$NON-NLS-1$
                }
                return new URL(s + relative);
            }
            // If not, fallback to the online documentation
            return new URL("http://developer.android.com/reference/" + relative); //$NON-NLS-1$
        } catch (MalformedURLException e) {
            AdtPlugin.log(e, "Can't create URL for %1$s", docs);
            return null;
        }
    }

    /** Returns true if the context is pointing to a permission name reference */
    private static boolean isBuiltinPermission(XmlContext context) {
        Attr attribute = context.getAttribute();
        Element node = context.getElement();

        // Is this an <activity> or <service> in an AndroidManifest.xml file? If so, jump to it
        String nodeName = node.getNodeName();
        if ((USES_PERMISSION.equals(nodeName) || PERMISSION.equals(nodeName))
                && ATTRIBUTE_NAME.equals(attribute.getLocalName())
                && ANDROID_URI.equals(attribute.getNamespaceURI())) {
            String value = attribute.getValue();
            if (value.startsWith(PERMISSION_PKG_PREFIX)) {
                return true;
            }
        }

        return false;
    }

    /** Returns true if the context is pointing to an intent reference */
    private static boolean isBuiltinIntent(XmlContext context) {
        Attr attribute = context.getAttribute();
        Element node = context.getElement();

        // Is this an <activity> or <service> in an AndroidManifest.xml file? If so, jump to it
        String nodeName = node.getNodeName();
        if ((ACTION.equals(nodeName) || CATEGORY.equals(nodeName))
                && ATTRIBUTE_NAME.equals(attribute.getLocalName())
                && ANDROID_URI.equals(attribute.getNamespaceURI())) {
            String value = attribute.getValue();
            if (value.startsWith(ACTION_PKG_PREFIX) || value.startsWith(CATEGORY_PKG_PREFIX)) {
                return true;
            }
        }

        return false;
    }


    /**
     * Returns the fully qualified class name of an activity referenced by the given
     * AndroidManifest.xml node
     */
    private static String getActivityClassFqcn(XmlContext context) {
        Attr attribute = context.getAttribute();
        Element node = context.getElement();
        StringBuilder sb = new StringBuilder();
        Element root = node.getOwnerDocument().getDocumentElement();
        String pkg = root.getAttribute(ATTRIBUTE_PACKAGE);
        String className = attribute.getValue();
        if (className.startsWith(".")) { //$NON-NLS-1$
            sb.append(pkg);
        } else if (className.indexOf('.') == -1) {
            // According to the <activity> manifest element documentation, this is not
            // valid ( http://developer.android.com/guide/topics/manifest/activity-element.html )
            // but it appears in manifest files and appears to be supported by the runtime
            // so handle this in code as well:
            sb.append(pkg);
            sb.append('.');
        } // else: the class name is already a fully qualified class name
        sb.append(className);
        return sb.toString();
    }

    /**
     * Returns the fully qualified class name of a service referenced by the given
     * AndroidManifest.xml node
     */
    private static String getServiceClassFqcn(XmlContext context) {
        // Same logic
        return getActivityClassFqcn(context);
    }

    /**
     * Returns the XML tag containing an element description for value items of the given
     * resource type
     *
     * @param type the resource type to query the XML tag name for
     * @return the tag name used for value declarations in XML of resources of the given
     *         type
     */
    public static String getTagName(ResourceType type) {
        if (type == ResourceType.ID) {
            // Ids are recorded in <item> tags instead of <id> tags
            return SdkConstants.TAG_ITEM;
        }

        return type.getName();
    }

    /**
     * Computes the actual exact location to jump to for a given XML context.
     *
     * @param context the XML context to be opened
     * @return true if the request was handled successfully
     */
    private static boolean open(XmlContext context) {
        IProject project = getProject();
        if (project == null) {
            return false;
        }

        if (isManifestName(context)) {
            return openManifestName(project, context);
        } else if (isClassElement(context) || isClassAttribute(context)) {
            return AdtPlugin.openJavaClass(project, getClassFqcn(context));
        } else if (isOnClickAttribute(context)) {
            return openOnClickMethod(project, context.getAttribute().getValue());
        } else {
            return false;
        }
    }

    /** Opens a path (which may not be in the workspace) */
    private static void openPath(IPath filePath, IRegion region, int offset) {
        IEditorPart sourceEditor = getEditor();
        IWorkbenchPage page = sourceEditor.getEditorSite().getPage();

        IFile file = AdtUtils.pathToIFile(filePath);
        if (file != null && file.exists()) {
            try {
                AdtPlugin.openFile(file, region);
                return;
            } catch (PartInitException ex) {
                AdtPlugin.log(ex, "Can't open %$1s", filePath); //$NON-NLS-1$
            }
        } else {
            // It's not a path in the workspace; look externally
            // (this is probably an @android: path)
            if (filePath.isAbsolute()) {
                IFileStore fileStore = EFS.getLocalFileSystem().getStore(filePath);
                if (!fileStore.fetchInfo().isDirectory() && fileStore.fetchInfo().exists()) {
                    try {
                        IEditorPart target = IDE.openEditorOnFileStore(page, fileStore);
                        if (target instanceof MultiPageEditorPart) {
                            MultiPageEditorPart part = (MultiPageEditorPart) target;
                            IEditorPart[] editors = part.findEditors(target.getEditorInput());
                            if (editors != null) {
                                for (IEditorPart editor : editors) {
                                    if (editor instanceof StructuredTextEditor) {
                                        StructuredTextEditor ste = (StructuredTextEditor) editor;
                                        part.setActiveEditor(editor);
                                        ste.selectAndReveal(offset, 0);
                                        break;
                                    }
                                }
                            }
                        }

                        return;
                    } catch (PartInitException ex) {
                        AdtPlugin.log(ex, "Can't open %$1s", filePath); //$NON-NLS-1$
                    }
                }
            }
        }

        // Failed: display message to the user
        displayError(String.format("Could not find resource %1$s", filePath));
    }

    private static void displayError(String message) {
        // Failed: display message to the user
        IEditorSite editorSite = getEditor().getEditorSite();
        IStatusLineManager status = editorSite.getActionBars().getStatusLineManager();
        status.setErrorMessage(message);
    }

    /**
     * Opens a Java method referenced by the given on click attribute method name
     *
     * @param project the project containing the click handler
     * @param method the method name of the on click handler
     * @return true if the method was opened, false otherwise
     */
    public static boolean openOnClickMethod(IProject project, String method) {
        // Search for the method in the Java index, filtering by the required click handler
        // method signature (public and has a single View parameter), and narrowing the scope
        // first to Activity classes, then to the whole workspace.
        final AtomicBoolean success = new AtomicBoolean(false);
        SearchRequestor requestor = new SearchRequestor() {
            @Override
            public void acceptSearchMatch(SearchMatch match) throws CoreException {
                Object element = match.getElement();
                if (element instanceof IMethod) {
                    IMethod methodElement = (IMethod) element;
                    String[] parameterTypes = methodElement.getParameterTypes();
                    if (parameterTypes != null
                            && parameterTypes.length == 1
                            && ("Qandroid.view.View;".equals(parameterTypes[0]) //$NON-NLS-1$
                                    || "QView;".equals(parameterTypes[0]))) {   //$NON-NLS-1$
                        // Check that it's public
                        if (Flags.isPublic(methodElement.getFlags())) {
                            JavaUI.openInEditor(methodElement);
                            success.getAndSet(true);
                        }
                    }
                }
            }
        };
        try {
            IJavaSearchScope scope = null;
            IType activityType = null;
            IJavaProject javaProject = BaseProjectHelper.getJavaProject(project);
            if (javaProject != null) {
                activityType = javaProject.findType(CLASS_ACTIVITY);
                if (activityType != null) {
                    scope = SearchEngine.createHierarchyScope(activityType);
                }
            }
            if (scope == null) {
                scope = SearchEngine.createWorkspaceScope();
            }

            SearchParticipant[] participants = new SearchParticipant[] {
                SearchEngine.getDefaultSearchParticipant()
            };
            int matchRule = SearchPattern.R_PATTERN_MATCH | SearchPattern.R_CASE_SENSITIVE;
            SearchPattern pattern = SearchPattern.createPattern("*." + method,
                    IJavaSearchConstants.METHOD, IJavaSearchConstants.DECLARATIONS, matchRule);
            SearchEngine engine = new SearchEngine();
            engine.search(pattern, participants, scope, requestor, new NullProgressMonitor());

            boolean ok = success.get();
            if (!ok && activityType != null) {
                // TODO: Create a project+dependencies scope and search only that scope

                // Try searching again with a complete workspace scope this time
                scope = SearchEngine.createWorkspaceScope();
                engine.search(pattern, participants, scope, requestor, new NullProgressMonitor());

                // TODO: There could be more than one match; add code to consider them all
                // and pick the most likely candidate and open only that one.

                ok = success.get();
            }
            return ok;
        } catch (CoreException e) {
            AdtPlugin.log(e, null);
        }
        return false;
    }

    /**
     * Returns the current configuration, if the associated UI editor has been initialized
     * and has an associated configuration
     *
     * @return the configuration for this file, or null
     */
    private static FolderConfiguration getConfiguration() {
        IEditorPart editor = getEditor();
        if (editor != null) {
            LayoutEditorDelegate delegate = LayoutEditorDelegate.fromEditor(editor);
            GraphicalEditorPart graphicalEditor =
                delegate == null ? null : delegate.getGraphicalEditor();

            if (graphicalEditor != null) {
                return graphicalEditor.getConfiguration();
            } else {
                // TODO: Could try a few more things to get the configuration:
                // (1) try to look at the file.getPersistentProperty(NAME_CONFIG_STATE)
                //    which will return previously saved state. This isn't necessary today
                //    since no editors seem to be lazily initialized.
                // (2) attempt to use the configuration from any of the other open
                //    files, especially files in the same directory as this one.
            }

            // Create a configuration from the current file
            IProject project = null;
            IEditorInput editorInput = editor.getEditorInput();
            if (editorInput instanceof FileEditorInput) {
                IFile file = ((FileEditorInput) editorInput).getFile();
                project = file.getProject();
                ProjectResources pr = ResourceManager.getInstance().getProjectResources(project);
                IContainer parent = file.getParent();
                if (parent instanceof IFolder) {
                    ResourceFolder resFolder = pr.getResourceFolder((IFolder) parent);
                    if (resFolder != null) {
                        return resFolder.getConfiguration();
                    }
                }
            }

            // Might be editing a Java file, where there is no configuration context.
            // Instead look at surrounding files in the workspace and obtain one valid
            // configuration.
            for (IEditorReference reference : editor.getSite().getPage().getEditorReferences()) {
                IEditorPart part = reference.getEditor(false /*restore*/);

                LayoutEditorDelegate refDelegate = LayoutEditorDelegate.fromEditor(part);
                if (refDelegate != null) {
                    IProject refProject = refDelegate.getEditor().getProject();
                    if (project == null || project == refProject) {
                        GraphicalEditorPart refGraphicalEditor = refDelegate.getGraphicalEditor();
                        if (refGraphicalEditor != null) {
                            return refGraphicalEditor.getConfiguration();
                        }
                    }
                }
            }
        }

        return null;
    }

    /** Returns the {@link IAndroidTarget} to be used for looking up system resources */
    private static IAndroidTarget getTarget(IProject project) {
        IEditorPart editor = getEditor();
        LayoutEditorDelegate delegate = LayoutEditorDelegate.fromEditor(editor);
        if (delegate != null) {
            GraphicalEditorPart graphicalEditor = delegate.getGraphicalEditor();
            if (graphicalEditor != null) {
                return graphicalEditor.getRenderingTarget();
            }
        }

        Sdk currentSdk = Sdk.getCurrent();
        if (currentSdk == null) {
            return null;
        }

        return currentSdk.getTarget(project);
    }

    /** Return either the project resources or the framework resources (or null) */
    private static ResourceRepository getResources(IProject project, boolean framework) {
        if (framework) {
            IAndroidTarget target = getTarget(project);

            if (target == null && project == null && framework) {
                // No current project: probably jumped into some of the framework XML resource
                // files and attempting to jump around. Attempt to figure out which target
                // we're dealing with and continue looking within the same framework.
                IEditorPart editor = getEditor();
                Sdk sdk = Sdk.getCurrent();
                if (sdk != null && editor instanceof AndroidXmlEditor) {
                    AndroidTargetData data = ((AndroidXmlEditor) editor).getTargetData();
                    if (data != null) {
                        return data.getFrameworkResources();
                    }
                }
            }

            if (target == null) {
                return null;
            }
            AndroidTargetData data = Sdk.getCurrent().getTargetData(target);
            if (data == null) {
                return null;
            }
            return data.getFrameworkResources();
        } else {
            return ResourceManager.getInstance().getProjectResources(project);
        }
    }

    /**
     * Finds a definition of an id attribute in layouts. (Ids can also be defined as
     * resources; use {@link #findValueInXml} or {@link #findValueInDocument} to locate it there.)
     */
    private static Pair<IFile, IRegion> findIdDefinition(IProject project, String id) {
        // FIRST look in the same file as the originating request, that's where you usually
        // want to jump
        IFile self = AdtUtils.getActiveFile();
        if (self != null && EXT_XML.equals(self.getFileExtension())) {
            Pair<IFile, IRegion> target = findIdInXml(id, self);
            if (target != null) {
                return target;
            }
        }

        // Look in the configuration folder: Search compatible configurations
        ResourceRepository resources = getResources(project, false /* isFramework */);
        FolderConfiguration configuration = getConfiguration();
        if (configuration != null) { // Not the case when searching from Java files for example
            List<ResourceFolder> folders = resources.getFolders(ResourceFolderType.LAYOUT);
            if (folders != null) {
                for (ResourceFolder folder : folders) {
                    if (folder.getConfiguration().isMatchFor(configuration)) {
                        IAbstractFolder wrapper = folder.getFolder();
                        if (wrapper instanceof IFolderWrapper) {
                            IFolder iFolder = ((IFolderWrapper) wrapper).getIFolder();
                            Pair<IFile, IRegion> target = findIdInFolder(iFolder, id);
                            if (target != null) {
                                return target;
                            }
                        }
                    }
                }
                return null;
            }
        }

        // Ugh. Search ALL layout files in the project!
        List<ResourceFolder> folders = resources.getFolders(ResourceFolderType.LAYOUT);
        if (folders != null) {
            for (ResourceFolder folder : folders) {
                IAbstractFolder wrapper = folder.getFolder();
                if (wrapper instanceof IFolderWrapper) {
                    IFolder iFolder = ((IFolderWrapper) wrapper).getIFolder();
                    Pair<IFile, IRegion> target = findIdInFolder(iFolder, id);
                    if (target != null) {
                        return target;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Finds a definition of an id attribute in a particular layout folder.
     */
    private static Pair<IFile, IRegion> findIdInFolder(IContainer f, String id) {
        try {
            // Check XML files in values/
            for (IResource resource : f.members()) {
                if (resource.exists() && !resource.isDerived() && resource instanceof IFile) {
                    IFile file = (IFile) resource;
                    // Must have an XML extension
                    if (EXT_XML.equals(file.getFileExtension())) {
                        Pair<IFile, IRegion> target = findIdInXml(id, file);
                        if (target != null) {
                            return target;
                        }
                    }
                }
            }
        } catch (CoreException e) {
            AdtPlugin.log(e, ""); //$NON-NLS-1$
        }

        return null;
    }

    /** Parses the given file and locates a definition of the given resource */
    private static Pair<IFile, IRegion> findValueInXml(
            ResourceType type, String name, IFile file) {
        IStructuredModel model = null;
        try {
            model = StructuredModelManager.getModelManager().getExistingModelForRead(file);
            if (model == null) {
                // There is no open or cached model for the file; see if the file looks
                // like it's interesting (content contains the String name we are looking for)
                if (AdtPlugin.fileContains(file, name)) {
                    // Yes, so parse content
                    model = StructuredModelManager.getModelManager().getModelForRead(file);
                }
            }
            if (model instanceof IDOMModel) {
                IDOMModel domModel = (IDOMModel) model;
                Document document = domModel.getDocument();
                return findValueInDocument(type, name, file, document);
            }
        } catch (IOException e) {
            AdtPlugin.log(e, "Can't parse %1$s", file); //$NON-NLS-1$
        } catch (CoreException e) {
            AdtPlugin.log(e, "Can't parse %1$s", file); //$NON-NLS-1$
        } finally {
            if (model != null) {
                model.releaseFromRead();
            }
        }

        return null;
    }

    /** Looks within an XML DOM document for the given resource name and returns it */
    private static Pair<IFile, IRegion> findValueInDocument(
            ResourceType type, String name, IFile file, Document document) {
        String targetTag = getTagName(type);
        Element root = document.getDocumentElement();
        if (root.getTagName().equals(TAG_RESOURCES)) {
            NodeList topLevel = root.getChildNodes();
            Pair<IFile, IRegion> value = findValueInChildren(name, file, targetTag, topLevel);
            if (value == null && type == ResourceType.ATTR) {
                for (int i = 0, n = topLevel.getLength(); i < n; i++) {
                    Node child = topLevel.item(i);
                    if (child.getNodeType() == Node.ELEMENT_NODE) {
                        Element element = (Element)child;
                        String tagName = element.getTagName();
                        if (tagName.equals("declare-styleable")) {
                            NodeList children = element.getChildNodes();
                            value = findValueInChildren(name, file, targetTag, children);
                            if (value != null) {
                                return value;
                            }
                        }
                    }
                }
            }

            return value;
        }

        return null;
    }

    private static Pair<IFile, IRegion> findValueInChildren(String name, IFile file,
            String targetTag, NodeList children) {
        for (int i = 0, n = children.getLength(); i < n; i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element)child;
                String tagName = element.getTagName();
                if (tagName.equals(targetTag)) {
                    String elementName = element.getAttribute(ATTR_NAME);
                    if (elementName.equals(name)) {
                        IRegion region = null;
                        if (element instanceof IndexedRegion) {
                            IndexedRegion r = (IndexedRegion) element;
                            // IndexedRegion.getLength() returns bogus values
                            int length = r.getEndOffset() - r.getStartOffset();
                            region = new Region(r.getStartOffset(), length);
                        }

                        return Pair.of(file, region);
                    }
                }
            }
        }

        return null;
    }

    /** Parses the given file and locates a definition of the given resource */
    private static Pair<IFile, IRegion> findIdInXml(String id, IFile file) {
        IStructuredModel model = null;
        try {
            model = StructuredModelManager.getModelManager().getExistingModelForRead(file);
            if (model == null) {
                // There is no open or cached model for the file; see if the file looks
                // like it's interesting (content contains the String name we are looking for)
                if (AdtPlugin.fileContains(file, id)) {
                    // Yes, so parse content
                    model = StructuredModelManager.getModelManager().getModelForRead(file);
                }
            }
            if (model instanceof IDOMModel) {
                IDOMModel domModel = (IDOMModel) model;
                Document document = domModel.getDocument();
                return findIdInDocument(id, file, document);
            }
        } catch (IOException e) {
            AdtPlugin.log(e, "Can't parse %1$s", file); //$NON-NLS-1$
        } catch (CoreException e) {
            AdtPlugin.log(e, "Can't parse %1$s", file); //$NON-NLS-1$
        } finally {
            if (model != null) {
                model.releaseFromRead();
            }
        }

        return null;
    }

    /** Looks within an XML DOM document for the given resource name and returns it */
    private static Pair<IFile, IRegion> findIdInDocument(String id, IFile file,
            Document document) {
        String targetAttribute = NEW_ID_PREFIX + id;
        Element root = document.getDocumentElement();
        Pair<IFile, IRegion> result = findIdInElement(root, file, targetAttribute,
                true /*requireId*/);
        if (result == null) {
            result = findIdInElement(root, file, targetAttribute, false /*requireId*/);
        }
        return result;
    }

    private static Pair<IFile, IRegion> findIdInElement(
            Element root, IFile file, String targetAttribute, boolean requireIdAttribute) {
        NamedNodeMap attributes = root.getAttributes();
        for (int i = 0, n = attributes.getLength(); i < n; i++) {
            Node item = attributes.item(i);
            if (item instanceof Attr) {
                Attr attribute = (Attr) item;
                if (requireIdAttribute && !ATTR_ID.equals(attribute.getLocalName())) {
                    continue;
                }
                String value = attribute.getValue();
                if (value.equals(targetAttribute)) {
                    // Select the element -containing- the id rather than the attribute itself
                    IRegion region = null;
                    Node element = attribute.getOwnerElement();
                    //if (attribute instanceof IndexedRegion) {
                    if (element instanceof IndexedRegion) {
                        IndexedRegion r = (IndexedRegion) element;
                        int length = r.getEndOffset() - r.getStartOffset();
                        region = new Region(r.getStartOffset(), length);
                    }

                    return Pair.of(file, region);
                }
            }
        }

        NodeList children = root.getChildNodes();
        for (int i = 0, n = children.getLength(); i < n; i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element)child;
                Pair<IFile, IRegion> result = findIdInElement(element, file, targetAttribute,
                        requireIdAttribute);
                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }

    /** Parses the given file and locates a definition of the given resource */
    private static Pair<File, Integer> findValueInXml(ResourceType type, String name, File file) {
        // We can't use the StructureModelManager on files outside projects
        // There is no open or cached model for the file; see if the file looks
        // like it's interesting (content contains the String name we are looking for)
        if (AdtPlugin.fileContains(file, name)) {
            try {
                InputSource is = new InputSource(new FileInputStream(file));
                OffsetTrackingParser parser = new OffsetTrackingParser();
                parser.parse(is);
                Document document = parser.getDocument();

                return findValueInDocument(type, name, file, parser, document);
            } catch (SAXException e) {
                // pass -- ignore files we can't parse
            } catch (IOException e) {
                // pass -- ignore files we can't parse
            }
        }

        return null;
    }

    /** Looks within an XML DOM document for the given resource name and returns it */
    private static Pair<File, Integer> findValueInDocument(ResourceType type, String name,
            File file, OffsetTrackingParser parser, Document document) {
        String targetTag = type.getName();
        if (type == ResourceType.ID) {
            // Ids are recorded in <item> tags instead of <id> tags
            targetTag = "item"; //$NON-NLS-1$
        }

        Pair<File, Integer> result = findTag(name, file, parser, document, targetTag);
        if (result == null && type == ResourceType.ATTR) {
            // Attributes seem to be defined in <public> tags
            targetTag = "public"; //$NON-NLS-1$
            result = findTag(name, file, parser, document, targetTag);
        }
        return result;
    }

    private static Pair<File, Integer> findTag(String name, File file, OffsetTrackingParser parser,
            Document document, String targetTag) {
        NodeList children = document.getElementsByTagName(targetTag);
        for (int i = 0, n = children.getLength(); i < n; i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) child;
                if (element.getTagName().equals(targetTag)) {
                    String elementName = element.getAttribute(ATTR_NAME);
                    if (elementName.equals(name)) {
                        return Pair.of(file, parser.getOffset(element));
                    }
                }
            }
        }

        return null;
    }

    private static IHyperlink[] getStyleLinks(XmlContext context, IRegion range, String url) {
        Attr attribute = context.getAttribute();
        if (attribute != null) {
            // Split up theme resource urls to the nearest dot forwards, such that you
            // can point to "Theme.Light" by placing the caret anywhere after the dot,
            // and point to just "Theme" by pointing before it.
            int caret = context.getInnerRegionCaretOffset();
            String value = attribute.getValue();
            int index = value.indexOf('.', caret);
            if (index != -1) {
                url = url.substring(0, index);
                range = new Region(range.getOffset(),
                        range.getLength() - (value.length() - index));
            }
        }

        ResourceUrl resource = ResourceUrl.parse(url);
        if (resource == null) {
            String androidStyle = ANDROID_STYLE_RESOURCE_PREFIX;
            if (url.startsWith(ANDROID_PREFIX)) {
                url = androidStyle + url.substring(ANDROID_PREFIX.length());
            } else if (url.startsWith(ANDROID_THEME_PREFIX)) {
                url = androidStyle + url.substring(ANDROID_THEME_PREFIX.length());
            } else if (url.startsWith(ANDROID_PKG + ':')) {
                url = androidStyle + url.substring(ANDROID_PKG.length() + 1);
            } else {
                url = STYLE_RESOURCE_PREFIX + url;
            }
        }
        return getResourceLinks(range, url);
    }

    private static IHyperlink[] getResourceLinks(@Nullable IRegion range, @NonNull String url) {
        IProject project = Hyperlinks.getProject();
        FolderConfiguration configuration = getConfiguration();
        return getResourceLinks(range, url, project, configuration);
    }

    /**
     * Computes hyperlinks to resource definitions for resource urls (e.g.
     * {@code @android:string/ok} or {@code @layout/foo}. May create multiple links.
     * @param range TBD
     * @param url the resource url
     * @param project the relevant project
     * @param configuration the applicable configuration
     * @return an array of hyperlinks, or null
     */
    @Nullable
    public static IHyperlink[] getResourceLinks(@Nullable IRegion range, @NonNull String url,
            @Nullable IProject project,  @Nullable FolderConfiguration configuration) {
        List<IHyperlink> links = new ArrayList<IHyperlink>();

        ResourceUrl resource = ResourceUrl.parse(url);
        if (resource == null) {
            return null;
        }
        ResourceType type = resource.type;
        String name = resource.name;
        boolean isFramework = resource.framework;
        if (project == null) {
            // Local reference *within* a framework
            isFramework = true;
        }

        ResourceRepository resources = getResources(project, isFramework);
        if (resources == null) {
            return null;
        }
        List<ResourceFile> sourceFiles = resources.getSourceFiles(type, name,
                null /*configuration*/);
        if (sourceFiles == null) {
            ProjectState projectState = Sdk.getProjectState(project);
            if (projectState != null) {
                List<IProject> libraries = projectState.getFullLibraryProjects();
                if (libraries != null && !libraries.isEmpty()) {
                    for (IProject library : libraries) {
                        resources = ResourceManager.getInstance().getProjectResources(library);
                        sourceFiles = resources.getSourceFiles(type, name, null /*configuration*/);
                        if (sourceFiles != null && !sourceFiles.isEmpty()) {
                            break;
                        }
                    }
                }
            }
        }

        ResourceFile best = null;
        if (configuration != null && sourceFiles != null && sourceFiles.size() > 0) {
            List<ResourceFile> bestFiles = resources.getSourceFiles(type, name, configuration);
            if (bestFiles != null && bestFiles.size() > 0) {
                best = bestFiles.get(0);
            }
        }
        if (sourceFiles != null) {
            List<ResourceFile> matches = new ArrayList<ResourceFile>();
            for (ResourceFile resourceFile : sourceFiles) {
                matches.add(resourceFile);
            }

            if (matches.size() > 0) {
                final ResourceFile fBest = best;
                Collections.sort(matches, new Comparator<ResourceFile>() {
                    @Override
                    public int compare(ResourceFile rf1, ResourceFile rf2) {
                        // Sort best item to the front
                        if (rf1 == fBest) {
                            return -1;
                        } else if (rf2 == fBest) {
                            return 1;
                        } else {
                            return getFileName(rf1).compareTo(getFileName(rf2));
                        }
                    }
                });

                // Is this something found in a values/ folder?
                boolean valueResource = ResourceHelper.isValueBasedResourceType(type);

                for (ResourceFile file : matches) {
                    String folderName = file.getFolder().getFolder().getName();
                    String label = String.format("Open Declaration in %1$s/%2$s",
                            folderName, getFileName(file));

                    // Only search for resource type within the file if it's an
                    // XML file and it is a value resource
                    ResourceLink link = new ResourceLink(label, range, file,
                            valueResource ? type : null, name);
                    links.add(link);
                }
            }
        }

        // Id's are handled specially because they are typically defined
        // inline (though they -can- be defined in the values folder above as
        // well, in which case we will prefer that definition)
        if (!isFramework && type == ResourceType.ID && links.size() == 0) {
            // Must compute these lazily...
            links.add(new ResourceLink("Open XML Declaration", range, null, type, name));
        }

        if (links.size() > 0) {
            return links.toArray(new IHyperlink[links.size()]);
        } else {
            return null;
        }
    }

    private static String getFileName(ResourceFile file) {
        return file.getFile().getName();
    }

    /** Detector for finding Android references in XML files */
   public static class XmlResolver extends AbstractHyperlinkDetector {

        @Override
        public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region,
                boolean canShowMultipleHyperlinks) {

            if (region == null || textViewer == null) {
                return null;
            }

            IDocument document = textViewer.getDocument();

            XmlContext context = XmlContext.find(document, region.getOffset());
            if (context == null) {
                return null;
            }

            IRegion range = context.getInnerRange(document);
            boolean isLinkable = false;
            String type = context.getInnerRegion().getType();
            if (type == DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE) {
                if (isAttributeValueLink(context)) {
                    isLinkable = true;
                    // Strip out quotes
                    range = new Region(range.getOffset() + 1, range.getLength() - 2);

                    Attr attribute = context.getAttribute();
                    if (isStyleAttribute(context)) {
                        return getStyleLinks(context, range, attribute.getValue());
                    }
                    if (attribute != null
                            && (attribute.getValue().startsWith(PREFIX_RESOURCE_REF)
                                    || attribute.getValue().startsWith(PREFIX_THEME_REF))) {
                        // Instantly create links for resources since we can use the existing
                        // resolved maps for this and offer multiple choices for the user
                        String url = attribute.getValue();
                        return getResourceLinks(range, url);
                    }
                }
            } else if (type == DOMRegionContext.XML_TAG_ATTRIBUTE_NAME) {
                if (isAttributeNameLink(context)) {
                    isLinkable = true;
                }
            } else if (type == DOMRegionContext.XML_TAG_NAME) {
                if (isElementNameLink(context)) {
                    isLinkable = true;
                }
            } else if (type == DOMRegionContext.XML_CONTENT) {
                Node parentNode = context.getNode().getParentNode();
                if (parentNode != null && parentNode.getNodeType() == Node.ELEMENT_NODE) {
                    // Try to complete resources defined inline as text, such as
                    // style definitions
                    ITextRegion outer = context.getElementRegion();
                    ITextRegion inner = context.getInnerRegion();
                    int innerOffset = outer.getStart() + inner.getStart();
                    int caretOffset = innerOffset + context.getInnerRegionCaretOffset();
                    try {
                        IRegion lineInfo = document.getLineInformationOfOffset(caretOffset);
                        int lineStart = lineInfo.getOffset();
                        int lineEnd = Math.min(lineStart + lineInfo.getLength(),
                                innerOffset + inner.getLength());

                        // Compute the resource URL
                        int urlStart = -1;
                        int offset = caretOffset;
                        while (offset > lineStart) {
                            char c = document.getChar(offset);
                            if (c == '@' || c == '?') {
                                urlStart = offset;
                                break;
                            } else if (!isValidResourceUrlChar(c)) {
                                break;
                            }
                            offset--;
                        }

                        if (urlStart != -1) {
                            offset = caretOffset;
                            while (offset < lineEnd) {
                                if (!isValidResourceUrlChar(document.getChar(offset))) {
                                    break;
                                }
                                offset++;
                            }

                            int length = offset - urlStart;
                            String url = document.get(urlStart, length);
                            range = new Region(urlStart, length);
                            return getResourceLinks(range, url);
                        }
                    } catch (BadLocationException e) {
                        AdtPlugin.log(e, null);
                    }
                }
            }

            if (isLinkable) {
                IHyperlink hyperlink = new DeferredResolutionLink(context, range);
                if (hyperlink != null) {
                    return new IHyperlink[] {
                        hyperlink
                    };
                }
            }

            return null;
        }
    }

    private static boolean isValidResourceUrlChar(char c) {
        return Character.isJavaIdentifierPart(c) || c == ':' || c == '/' || c == '.' || c == '+';

    }

    /** Detector for finding Android references in Java files */
    public static class JavaResolver extends AbstractHyperlinkDetector {

        @Override
        public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region,
                boolean canShowMultipleHyperlinks) {
            // Most of this is identical to the builtin JavaElementHyperlinkDetector --
            // everything down to the Android R filtering below

            ITextEditor textEditor = (ITextEditor) getAdapter(ITextEditor.class);
            if (region == null || !(textEditor instanceof JavaEditor))
                return null;

            IAction openAction = textEditor.getAction("OpenEditor"); //$NON-NLS-1$
            if (!(openAction instanceof SelectionDispatchAction))
                return null;

            int offset = region.getOffset();

            IJavaElement input = EditorUtility.getEditorInputJavaElement(textEditor, false);
            if (input == null)
                return null;

            try {
                IDocument document = textEditor.getDocumentProvider().getDocument(
                        textEditor.getEditorInput());
                IRegion wordRegion = JavaWordFinder.findWord(document, offset);
                if (wordRegion == null || wordRegion.getLength() == 0)
                    return null;

                IJavaElement[] elements = null;
                elements = ((ICodeAssist) input).codeSelect(wordRegion.getOffset(), wordRegion
                        .getLength());

                // Specific Android R class filtering:
                if (elements.length > 0) {
                    IJavaElement element = elements[0];
                    if (element.getElementType() == IJavaElement.FIELD) {
                        IJavaElement unit = element.getAncestor(IJavaElement.COMPILATION_UNIT);
                        if (unit == null) {
                            // Probably in a binary; see if this is an android.R resource
                            IJavaElement type = element.getAncestor(IJavaElement.TYPE);
                            if (type != null && type.getParent() != null) {
                                IJavaElement parentType = type.getParent();
                                if (parentType.getElementType() == IJavaElement.CLASS_FILE) {
                                    String pn = parentType.getElementName();
                                    String prefix = FN_RESOURCE_BASE + "$"; //$NON-NLS-1$
                                    if (pn.startsWith(prefix)) {
                                        return createTypeLink(element, type, wordRegion, true);
                                    }
                                }
                            }
                        } else if (FN_RESOURCE_CLASS.equals(unit.getElementName())) {
                            // Yes, we're referencing the project R class.
                            // Offer hyperlink navigation to XML resource files for
                            // the various definitions
                            IJavaElement type = element.getAncestor(IJavaElement.TYPE);
                            if (type != null) {
                                return createTypeLink(element, type, wordRegion, false);
                            }
                        }
                    }

                }
                return null;
            } catch (JavaModelException e) {
                return null;
            }
        }

        private IHyperlink[] createTypeLink(IJavaElement element, IJavaElement type,
                IRegion wordRegion, boolean isFrameworkResource) {
            String typeName = type.getElementName();
            // typeName will be "id", "layout", "string", etc
            if (isFrameworkResource) {
                typeName = ANDROID_PKG + ':' + typeName;
            }
            String elementName = element.getElementName();
            String url = '@' + typeName + '/' + elementName;
            return getResourceLinks(wordRegion, url);
        }
    }

    /** Returns the editor applicable to this hyperlink detection */
    private static IEditorPart getEditor() {
        // I would like to be able to find this via getAdapter(TextEditor.class) but
        // couldn't find a way to initialize the editor context from
        // AndroidSourceViewerConfig#getHyperlinkDetectorTargets (which only has
        // a TextViewer, not a TextEditor, instance).
        //
        // Therefore, for now, use a hack. This hack is reasonable because hyperlink
        // resolvers are only run for the front-most visible window in the active
        // workbench.
        return AdtUtils.getActiveEditor();
    }

    /** Returns the project applicable to this hyperlink detection */
    @Nullable
    private static IProject getProject() {
        IFile file = AdtUtils.getActiveFile();
        if (file != null) {
            return file.getProject();
        }

        return null;
    }

    /**
     * Hyperlink implementation which delays computing the actual file and offset target
     * until it is asked to open the hyperlink
     */
    private static class DeferredResolutionLink implements IHyperlink {
        private XmlContext mXmlContext;
        private IRegion mRegion;

        public DeferredResolutionLink(XmlContext xmlContext, IRegion mRegion) {
            super();
            this.mXmlContext = xmlContext;
            this.mRegion = mRegion;
        }

        @Override
        public IRegion getHyperlinkRegion() {
            return mRegion;
        }

        @Override
        public String getHyperlinkText() {
            return "Open XML Declaration";
        }

        @Override
        public String getTypeLabel() {
            return null;
        }

        @Override
        public void open() {
            // Lazily compute the location to open
            if (mXmlContext != null && !Hyperlinks.open(mXmlContext)) {
                // Failed: display message to the user
                displayError("Could not open link");
            }
        }
    }

    /**
     * Hyperlink implementation which provides a link for a resource; the actual file name
     * is known, but the value location within XML files is deferred until the link is
     * actually opened.
     */
    public static class ResourceLink implements IHyperlink {
        private final String mLinkText;
        private final IRegion mLinkRegion;
        private final ResourceType mType;
        private final String mName;
        private final ResourceFile mFile;

        /**
         * Constructs a new {@link ResourceLink}.
         *
         * @param linkText the description of the link to be shown in a popup when there
         *            is more than one match
         * @param linkRegion the region corresponding to the link source highlight
         * @param file the target resource file containing the link definition
         * @param type the type of resource being linked to
         * @param name the name of the resource being linked to
         */
        public ResourceLink(String linkText, IRegion linkRegion, ResourceFile file,
                ResourceType type, String name) {
            super();
            mLinkText = linkText;
            mLinkRegion = linkRegion;
            mType = type;
            mName = name;
            mFile = file;
        }

        @Override
        public IRegion getHyperlinkRegion() {
            return mLinkRegion;
        }

        @Override
        public String getHyperlinkText() {
            // return "Open XML Declaration";
            return mLinkText;
        }

        @Override
        public String getTypeLabel() {
            return null;
        }

        @Override
        public void open() {
            // We have to defer computation of ids until the link is clicked since we
            // don't have a fast map lookup for these
            if (mFile == null && mType == ResourceType.ID) {
                // Id's are handled specially because they are typically defined
                // inline (though they -can- be defined in the values folder above as well,
                // in which case we will prefer that definition)
                IProject project = getProject();
                Pair<IFile,IRegion> def = findIdDefinition(project, mName);
                if (def != null) {
                    try {
                        AdtPlugin.openFile(def.getFirst(), def.getSecond());
                    } catch (PartInitException e) {
                        AdtPlugin.log(e, null);
                    }
                    return;
                }

                displayError(String.format("Could not find id %1$s", mName));
                return;
            }

            IAbstractFile wrappedFile = mFile != null ? mFile.getFile() : null;
            if (wrappedFile instanceof IFileWrapper) {
                IFile file = ((IFileWrapper) wrappedFile).getIFile();
                try {
                    // Lazily search for the target?
                    IRegion region = null;
                    String extension = file.getFileExtension();
                    if (mType != null && mName != null && EXT_XML.equals(extension)) {
                        Pair<IFile, IRegion> target;
                        if (mType == ResourceType.ID) {
                            target = findIdInXml(mName, file);
                        } else {
                            target = findValueInXml(mType, mName, file);
                        }
                        if (target != null) {
                            region = target.getSecond();
                        }
                    }
                    AdtPlugin.openFile(file, region);
                } catch (PartInitException e) {
                    AdtPlugin.log(e, null);
                }
            } else if (wrappedFile instanceof FileWrapper) {
                File file = ((FileWrapper) wrappedFile);
                IPath path = new Path(file.getAbsolutePath());
                int offset = 0;
                // Lazily search for the target?
                if (mType != null && mName != null && EXT_XML.equals(path.getFileExtension())) {
                    if (file.exists()) {
                        Pair<File, Integer> target = findValueInXml(mType, mName, file);
                        if (target != null && target.getSecond() != null) {
                            offset = target.getSecond();
                        }
                    }
                }
                openPath(path, null, offset);
            } else {
                throw new IllegalArgumentException("Invalid link parameters");
            }
        }

        public ResourceFile getFile() {
            return mFile;
        }
    }

    /**
     * XML context containing node, potentially attribute, and text regions surrounding a
     * particular caret offset
     */
    private static class XmlContext {
        private final Node mNode;
        private final Element mElement;
        private final Attr mAttribute;
        private final IStructuredDocumentRegion mOuterRegion;
        private final ITextRegion mInnerRegion;
        private final int mInnerRegionOffset;

        public XmlContext(Node node, Element element, Attr attribute,
                IStructuredDocumentRegion outerRegion,
                ITextRegion innerRegion, int innerRegionOffset) {
            super();
            mNode = node;
            mElement = element;
            mAttribute = attribute;
            mOuterRegion = outerRegion;
            mInnerRegion = innerRegion;
            mInnerRegionOffset = innerRegionOffset;
        }

        /**
         * Gets the current node, never null
         *
         * @return the surrounding node
         */
        public Node getNode() {
            return mNode;
        }


        /**
         * Gets the current node, may be null
         *
         * @return the surrounding node
         */
        public Element getElement() {
            return mElement;
        }

        /**
         * Returns the current attribute, or null if we are not over an attribute
         *
         * @return the attribute, or null
         */
        public Attr getAttribute() {
            return mAttribute;
        }

        /**
         * Gets the region of the element
         *
         * @return the region of the surrounding element, never null
         */
        public ITextRegion getElementRegion() {
            return mOuterRegion;
        }

        /**
         * Gets the inner region, which can be the tag name, an attribute name, an
         * attribute value, or some other portion of an XML element
         * @return the inner region, never null
         */
        public ITextRegion getInnerRegion() {
            return mInnerRegion;
        }

        /**
         * Gets the caret offset relative to the inner region
         *
         * @return the offset relative to the inner region
         */
        public int getInnerRegionCaretOffset() {
            return mInnerRegionOffset;
        }

        /**
         * Returns a range with suffix whitespace stripped out
         *
         * @param document the document containing the regions
         * @return the range of the inner region, minus any whitespace at the end
         */
        public IRegion getInnerRange(IDocument document) {
            int start = mOuterRegion.getStart() + mInnerRegion.getStart();
            int length = mInnerRegion.getLength();
            try {
                String s = document.get(start, length);
                for (int i = s.length() - 1; i >= 0; i--) {
                    if (Character.isWhitespace(s.charAt(i))) {
                        length--;
                    }
                }
            } catch (BadLocationException e) {
                AdtPlugin.log(e, ""); //$NON-NLS-1$
            }
            return new Region(start, length);
        }

        /**
         * Returns the node the cursor is currently on in the document. null if no node is
         * selected
         */
        private static XmlContext find(IDocument document, int offset) {
            // Loosely based on getCurrentNode and getCurrentAttr in the WST's
            // XMLHyperlinkDetector.
            IndexedRegion inode = null;
            IStructuredModel model = null;
            try {
                model = StructuredModelManager.getModelManager().getExistingModelForRead(document);
                if (model != null) {
                    inode = model.getIndexedRegion(offset);
                    if (inode == null) {
                        inode = model.getIndexedRegion(offset - 1);
                    }

                    if (inode instanceof Element) {
                        Element element = (Element) inode;
                        Attr attribute = null;
                        if (element.hasAttributes()) {
                            NamedNodeMap attrs = element.getAttributes();
                            // go through each attribute in node and if attribute contains
                            // offset, return that attribute
                            for (int i = 0; i < attrs.getLength(); ++i) {
                                // assumption that if parent node is of type IndexedRegion,
                                // then its attributes will also be of type IndexedRegion
                                IndexedRegion attRegion = (IndexedRegion) attrs.item(i);
                                if (attRegion.contains(offset)) {
                                    attribute = (Attr) attrs.item(i);
                                    break;
                                }
                            }
                        }

                        IStructuredDocument doc = model.getStructuredDocument();
                        IStructuredDocumentRegion region = doc.getRegionAtCharacterOffset(offset);
                        if (region != null
                                && DOMRegionContext.XML_TAG_NAME.equals(region.getType())) {
                            ITextRegion subRegion = region.getRegionAtCharacterOffset(offset);
                            if (subRegion == null) {
                                return null;
                            }
                            int regionStart = region.getStartOffset();
                            int subregionStart = subRegion.getStart();
                            int relativeOffset = offset - (regionStart + subregionStart);
                            return new XmlContext(element, element, attribute, region, subRegion,
                                    relativeOffset);
                        }
                    } else if (inode instanceof Node) {
                        IStructuredDocument doc = model.getStructuredDocument();
                        IStructuredDocumentRegion region = doc.getRegionAtCharacterOffset(offset);
                        if (region != null
                                && DOMRegionContext.XML_CONTENT.equals(region.getType())) {
                            ITextRegion subRegion = region.getRegionAtCharacterOffset(offset);
                            int regionStart = region.getStartOffset();
                            int subregionStart = subRegion.getStart();
                            int relativeOffset = offset - (regionStart + subregionStart);
                            return new XmlContext((Node) inode, null, null, region, subRegion,
                                    relativeOffset);
                        }

                    }
                }
            } finally {
                if (model != null) {
                    model.releaseFromRead();
                }
            }

            return null;
        }
    }

    /**
     * DOM parser which records offsets in the element nodes such that it can return
     * offsets for elements later
     */
    private static final class OffsetTrackingParser extends DOMParser {

        private static final String KEY_OFFSET = "offset"; //$NON-NLS-1$

        private static final String KEY_NODE =
            "http://apache.org/xml/properties/dom/current-element-node"; //$NON-NLS-1$

        private XMLLocator mLocator;

        public OffsetTrackingParser() throws SAXException {
            this.setFeature("http://apache.org/xml/features/dom/defer-node-expansion",//$NON-NLS-1$
                    false);
        }

        public int getOffset(Node node) {
            Integer offset = (Integer) node.getUserData(KEY_OFFSET);
            if (offset != null) {
                return offset;
            }

            return -1;
        }

        @Override
        public void startElement(QName elementQName, XMLAttributes attrList, Augmentations augs)
                throws XNIException {
            int offset = mLocator.getCharacterOffset();
            super.startElement(elementQName, attrList, augs);

            try {
                Node node = (Node) this.getProperty(KEY_NODE);
                if (node != null) {
                    node.setUserData(KEY_OFFSET, offset, null);
                }
            } catch (org.xml.sax.SAXException ex) {
                AdtPlugin.log(ex, ""); //$NON-NLS-1$
            }
        }

        @Override
        public void startDocument(XMLLocator locator, String encoding,
                NamespaceContext namespaceContext, Augmentations augs) throws XNIException {
            super.startDocument(locator, encoding, namespaceContext, augs);
            mLocator = locator;
        }
    }
}

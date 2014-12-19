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

package org.eclipse.andmore.internal.editors.layout.gle2;

import static com.android.SdkConstants.ATTR_LAYOUT;
import static com.android.SdkConstants.EXT_XML;
import static com.android.SdkConstants.FD_RESOURCES;
import static com.android.SdkConstants.FD_RES_LAYOUT;
import static com.android.SdkConstants.TOOLS_URI;
import static com.android.SdkConstants.VIEW_FRAGMENT;
import static com.android.SdkConstants.VIEW_INCLUDE;
import static com.android.resources.ResourceType.LAYOUT;
import static org.eclipse.andmore.AdtConstants.WS_LAYOUTS;
import static org.eclipse.andmore.AdtConstants.WS_SEP;
import static org.eclipse.core.resources.IResourceDelta.ADDED;
import static org.eclipse.core.resources.IResourceDelta.CHANGED;
import static org.eclipse.core.resources.IResourceDelta.CONTENT;
import static org.eclipse.core.resources.IResourceDelta.REMOVED;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.annotations.VisibleForTesting;
import com.android.ide.common.resources.ResourceFile;
import com.android.ide.common.resources.ResourceFolder;
import com.android.ide.common.resources.ResourceItem;
import com.android.io.IAbstractFile;
import com.android.resources.ResourceType;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.project.BaseProjectHelper;
import org.eclipse.andmore.internal.resources.manager.ProjectResources;
import org.eclipse.andmore.internal.resources.manager.ResourceManager;
import org.eclipse.andmore.internal.resources.manager.ResourceManager.IResourceListener;
import org.eclipse.andmore.io.IFileWrapper;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.swt.widgets.Display;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The include finder finds other XML files that are including a given XML file, and does
 * so efficiently (caching results across IDE sessions etc).
 */
@SuppressWarnings("restriction") // XML model
public class IncludeFinder {
    /** Qualified name for the per-project persistent property include-map */
    private final static QualifiedName CONFIG_INCLUDES = new QualifiedName(AdtPlugin.PLUGIN_ID,
            "includes");//$NON-NLS-1$

    /**
     * Qualified name for the per-project non-persistent property storing the
     * {@link IncludeFinder} for this project
     */
    private final static QualifiedName INCLUDE_FINDER = new QualifiedName(AdtPlugin.PLUGIN_ID,
            "includefinder"); //$NON-NLS-1$

    /** Project that the include finder locates includes for */
    private final IProject mProject;

    /** Map from a layout resource name to a set of layouts included by the given resource */
    private Map<String, List<String>> mIncludes = null;

    /**
     * Reverse map of {@link #mIncludes}; points to other layouts that are including a
     * given layouts
     */
    private Map<String, List<String>> mIncludedBy = null;

    /** Flag set during a refresh; ignore updates when this is true */
    private static boolean sRefreshing;

    /** Global (cross-project) resource listener */
    private static ResourceListener sListener;

    /**
     * Constructs an {@link IncludeFinder} for the given project. Don't use this method;
     * use the {@link #get} factory method instead.
     *
     * @param project project to create an {@link IncludeFinder} for
     */
    private IncludeFinder(IProject project) {
        mProject = project;
    }

    /**
     * Returns the {@link IncludeFinder} for the given project
     *
     * @param project the project the finder is associated with
     * @return an {@link IncludeFinder} for the given project, never null
     */
    @NonNull
    public static IncludeFinder get(IProject project) {
        IncludeFinder finder = null;
        try {
            finder = (IncludeFinder) project.getSessionProperty(INCLUDE_FINDER);
        } catch (CoreException e) {
            // Not a problem; we will just create a new one
        }

        if (finder == null) {
            finder = new IncludeFinder(project);
            try {
                project.setSessionProperty(INCLUDE_FINDER, finder);
            } catch (CoreException e) {
                AdtPlugin.log(e, "Can't store IncludeFinder");
            }
        }

        return finder;
    }

    /**
     * Returns a list of resource names that are included by the given resource
     *
     * @param includer the resource name to return included layouts for
     * @return the layouts included by the given resource
     */
    private List<String> getIncludesFrom(String includer) {
        ensureInitialized();

        return mIncludes.get(includer);
    }

    /**
     * Gets the list of all other layouts that are including the given layout.
     *
     * @param included the file that is included
     * @return the files that are including the given file, or null or empty
     */
    @Nullable
    public List<Reference> getIncludedBy(IResource included) {
        ensureInitialized();
        String mapKey = getMapKey(included);
        List<String> result = mIncludedBy.get(mapKey);
        if (result == null) {
            String name = getResourceName(included);
            if (!name.equals(mapKey)) {
                result = mIncludedBy.get(name);
            }
        }

        if (result != null && result.size() > 0) {
            List<Reference> references = new ArrayList<Reference>(result.size());
            for (String s : result) {
                references.add(new Reference(mProject, s));
            }
            return references;
        } else {
            return null;
        }
    }

    /**
     * Returns true if the given resource is included from some other layout in the
     * project
     *
     * @param included the resource to check
     * @return true if the file is included by some other layout
     */
    public boolean isIncluded(IResource included) {
        ensureInitialized();
        String mapKey = getMapKey(included);
        List<String> result = mIncludedBy.get(mapKey);
        if (result == null) {
            String name = getResourceName(included);
            if (!name.equals(mapKey)) {
                result = mIncludedBy.get(name);
            }
        }

        return result != null && result.size() > 0;
    }

    @VisibleForTesting
    /* package */ List<String> getIncludedBy(String included) {
        ensureInitialized();
        return mIncludedBy.get(included);
    }

    /** Initialize the inclusion data structures, if not already done */
    private void ensureInitialized() {
        if (mIncludes == null) {
            // Initialize
            if (!readSettings()) {
                // Couldn't read settings: probably the first time this code is running
                // so there is no known data about includes.

                // Yes, these should be multimaps! If we start using Guava replace
                // these with multimaps.
                mIncludes = new HashMap<String, List<String>>();
                mIncludedBy = new HashMap<String, List<String>>();

                scanProject();
                saveSettings();
            }
        }
    }

    // ----- Persistence -----

    /**
     * Create a String serialization of the includes map. The map attempts to be compact;
     * it strips out the @layout/ prefix, and eliminates the values for empty string
     * values. The map can be restored by calling {@link #decodeMap}. The encoded String
     * will have sorted keys.
     *
     * @param map the map to be serialized
     * @return a serialization (never null) of the given map
     */
    @VisibleForTesting
    public static String encodeMap(Map<String, List<String>> map) {
        StringBuilder sb = new StringBuilder();

        if (map != null) {
            // Process the keys in sorted order rather than just
            // iterating over the entry set to ensure stable output
            List<String> keys = new ArrayList<String>(map.keySet());
            Collections.sort(keys);
            for (String key : keys) {
                List<String> values = map.get(key);

                if (sb.length() > 0) {
                    sb.append(',');
                }
                sb.append(key);
                if (values.size() > 0) {
                    sb.append('=').append('>');
                    sb.append('{');
                    boolean first = true;
                    for (String value : values) {
                        if (first) {
                            first = false;
                        } else {
                            sb.append(',');
                        }
                        sb.append(value);
                    }
                    sb.append('}');
                }
            }
        }

        return sb.toString();
    }

    /**
     * Decodes the encoding (produced by {@link #encodeMap}) back into the original map,
     * modulo any key sorting differences.
     *
     * @param encoded an encoding of a map created by {@link #encodeMap}
     * @return a map corresponding to the encoded values, never null
     */
    @VisibleForTesting
    public static Map<String, List<String>> decodeMap(String encoded) {
        HashMap<String, List<String>> map = new HashMap<String, List<String>>();

        if (encoded.length() > 0) {
            int i = 0;
            int end = encoded.length();

            while (i < end) {

                // Find key range
                int keyBegin = i;
                int keyEnd = i;
                while (i < end) {
                    char c = encoded.charAt(i);
                    if (c == ',') {
                        break;
                    } else if (c == '=') {
                        i += 2; // Skip =>
                        break;
                    }
                    i++;
                    keyEnd = i;
                }

                List<String> values = new ArrayList<String>();
                // Find values
                if (i < end && encoded.charAt(i) == '{') {
                    i++;
                    while (i < end) {
                        int valueBegin = i;
                        int valueEnd = i;
                        char c = 0;
                        while (i < end) {
                            c = encoded.charAt(i);
                            if (c == ',' || c == '}') {
                                valueEnd = i;
                                break;
                            }
                            i++;
                        }
                        if (valueEnd > valueBegin) {
                            values.add(encoded.substring(valueBegin, valueEnd));
                        }

                        if (c == '}') {
                            if (i < end-1 && encoded.charAt(i+1) == ',') {
                                i++;
                            }
                            break;
                        }
                        assert c == ',';
                        i++;
                    }
                }

                String key = encoded.substring(keyBegin, keyEnd);
                map.put(key, values);
                i++;
            }
        }

        return map;
    }

    /**
     * Stores the settings in the persistent project storage.
     */
    private void saveSettings() {
        // Serialize the mIncludes map into a compact String. The mIncludedBy map can be
        // inferred from it.
        String encoded = encodeMap(mIncludes);

        try {
            if (encoded.length() >= 2048) {
                // The maximum length of a setting key is 2KB, according to the javadoc
                // for the project class. It's unlikely that we'll
                // hit this -- even with an average layout root name of 20 characters
                // we can still store over a hundred names. But JUST IN CASE we run
                // into this, we'll clear out the key in this name which means that the
                // information will need to be recomputed in the next IDE session.
                mProject.setPersistentProperty(CONFIG_INCLUDES, null);
            } else {
                String existing = mProject.getPersistentProperty(CONFIG_INCLUDES);
                if (!encoded.equals(existing)) {
                    mProject.setPersistentProperty(CONFIG_INCLUDES, encoded);
                }
            }
        } catch (CoreException e) {
            AdtPlugin.log(e, "Can't store include settings");
        }
    }

    /**
     * Reads previously stored settings from the persistent project storage
     *
     * @return true iff settings were restored from the project
     */
    private boolean readSettings() {
        try {
            String encoded = mProject.getPersistentProperty(CONFIG_INCLUDES);
            if (encoded != null) {
                mIncludes = decodeMap(encoded);

                // Set up a reverse map, pointing from included files to the files that
                // included them
                mIncludedBy = new HashMap<String, List<String>>(2 * mIncludes.size());
                for (Map.Entry<String, List<String>> entry : mIncludes.entrySet()) {
                    // File containing the <include>
                    String includer = entry.getKey();
                    // Files being <include>'ed by the above file
                    List<String> included = entry.getValue();
                    setIncludedBy(includer, included);
                }

                return true;
            }
        } catch (CoreException e) {
            AdtPlugin.log(e, "Can't read include settings");
        }

        return false;
    }

    // ----- File scanning -----

    /**
     * Scan the whole project for XML layout resources that are performing includes.
     */
    private void scanProject() {
        ProjectResources resources = ResourceManager.getInstance().getProjectResources(mProject);
        if (resources != null) {
            Collection<ResourceItem> layouts = resources.getResourceItemsOfType(LAYOUT);
            for (ResourceItem layout : layouts) {
                List<ResourceFile> sources = layout.getSourceFileList();
                for (ResourceFile source : sources) {
                    updateFileIncludes(source, false);
                }
            }

            return;
        }
    }

    /**
     * Scans the given {@link ResourceFile} and if it is a layout resource, updates the
     * includes in it.
     *
     * @param resourceFile the {@link ResourceFile} to be scanned for includes (doesn't
     *            have to be only layout XML files; this method will filter the type)
     * @param singleUpdate true if this is a single file being updated, false otherwise
     *            (e.g. during initial project scanning)
     * @return true if we updated the includes for the resource file
     */
    private boolean updateFileIncludes(ResourceFile resourceFile, boolean singleUpdate) {
        Collection<ResourceType> resourceTypes = resourceFile.getResourceTypes();
        for (ResourceType type : resourceTypes) {
            if (type == ResourceType.LAYOUT) {
                ensureInitialized();

                List<String> includes = Collections.emptyList();
                if (resourceFile.getFile() instanceof IFileWrapper) {
                    IFile file = ((IFileWrapper) resourceFile.getFile()).getIFile();

                    // See if we have an existing XML model for this file; if so, we can
                    // just look directly at the parse tree
                    boolean hadXmlModel = false;
                    IStructuredModel model = null;
                    try {
                        IModelManager modelManager = StructuredModelManager.getModelManager();
                        model = modelManager.getExistingModelForRead(file);
                        if (model instanceof IDOMModel) {
                            IDOMModel domModel = (IDOMModel) model;
                            Document document = domModel.getDocument();
                            includes = findIncludesInDocument(document);
                            hadXmlModel = true;
                        }
                    } finally {
                        if (model != null) {
                            model.releaseFromRead();
                        }
                    }

                    // If no XML model we have to read the XML contents and (possibly) parse it.
                    // The actual file may not exist anymore (e.g. when deleting a layout file
                    // or when the workspace is out of sync.)
                    if (!hadXmlModel) {
                        String xml = AdtPlugin.readFile(file);
                        if (xml != null) {
                            includes = findIncludes(xml);
                        }
                    }
                } else {
                    String xml = AdtPlugin.readFile(resourceFile);
                    if (xml != null) {
                        includes = findIncludes(xml);
                    }
                }

                String key = getMapKey(resourceFile);
                if (includes.equals(getIncludesFrom(key))) {
                    // Common case -- so avoid doing settings flush etc
                    return false;
                }

                boolean detectCycles = singleUpdate;
                setIncluded(key, includes, detectCycles);

                if (singleUpdate) {
                    saveSettings();
                }

                return true;
            }
        }

        return false;
    }

    /**
     * Finds the list of includes in the given XML content. It attempts quickly return
     * empty if the file does not include any include tags; it does this by only parsing
     * if it detects the string &lt;include in the file.
     */
    @VisibleForTesting
    @NonNull
    static List<String> findIncludes(@NonNull String xml) {
        int index = xml.indexOf(ATTR_LAYOUT);
        if (index != -1) {
            return findIncludesInXml(xml);
        }

        return Collections.emptyList();
    }

    /**
     * Parses the given XML content and extracts all the included URLs and returns them
     *
     * @param xml layout XML content to be parsed for includes
     * @return a list of included urls, or null
     */
    @VisibleForTesting
    @NonNull
    static List<String> findIncludesInXml(@NonNull String xml) {
        Document document = DomUtilities.parseDocument(xml, false /*logParserErrors*/);
        if (document != null) {
            return findIncludesInDocument(document);
        }

        return Collections.emptyList();
    }

    /** Searches the given DOM document and returns the list of includes, if any */
    @NonNull
    private static List<String> findIncludesInDocument(@NonNull Document document) {
        List<String> includes = findIncludesInDocument(document, null);
        if (includes == null) {
            includes = Collections.emptyList();
        }
        return includes;
    }

    @Nullable
    private static List<String> findIncludesInDocument(@NonNull Node node,
            @Nullable List<String> urls) {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            String tag = node.getNodeName();
            boolean isInclude = tag.equals(VIEW_INCLUDE);
            boolean isFragment = tag.equals(VIEW_FRAGMENT);
            if (isInclude || isFragment) {
                Element element = (Element) node;
                String url;
                if (isInclude) {
                    url = element.getAttribute(ATTR_LAYOUT);
                } else {
                    url = element.getAttributeNS(TOOLS_URI, ATTR_LAYOUT);
                }
                if (url.length() > 0) {
                    String resourceName = urlToLocalResource(url);
                    if (resourceName != null) {
                        if (urls == null) {
                            urls = new ArrayList<String>();
                        }
                        urls.add(resourceName);
                    }
                }

            }
        }

        NodeList children = node.getChildNodes();
        for (int i = 0, n = children.getLength(); i < n; i++) {
            urls = findIncludesInDocument(children.item(i), urls);
        }

        return urls;
    }


    /**
     * Returns the layout URL to a local resource name (provided the URL is a local
     * resource, not something in @android etc.) Returns null otherwise.
     */
    private static String urlToLocalResource(String url) {
        if (!url.startsWith("@")) { //$NON-NLS-1$
            return null;
        }
        int typeEnd = url.indexOf('/', 1);
        if (typeEnd == -1) {
            return null;
        }
        int nameBegin = typeEnd + 1;
        int typeBegin = 1;
        int colon = url.lastIndexOf(':', typeEnd);
        if (colon != -1) {
            String packageName = url.substring(typeBegin, colon);
            if ("android".equals(packageName)) { //$NON-NLS-1$
                // Don't want to point to non-local resources
                return null;
            }

            typeBegin = colon + 1;
            assert "layout".equals(url.substring(typeBegin, typeEnd)); //$NON-NLS-1$
        }

        return url.substring(nameBegin);
    }

    /**
     * Record the list of included layouts from the given layout
     *
     * @param includer the layout including other layouts
     * @param included the layouts that were included by the including layout
     * @param detectCycles if true, check for cycles and report them as project errors
     */
    @VisibleForTesting
    /* package */ void setIncluded(String includer, List<String> included, boolean detectCycles) {
        // Remove previously linked inverse mappings
        List<String> oldIncludes = mIncludes.get(includer);
        if (oldIncludes != null && oldIncludes.size() > 0) {
            for (String includee : oldIncludes) {
                List<String> includers = mIncludedBy.get(includee);
                if (includers != null) {
                    includers.remove(includer);
                }
            }
        }

        mIncludes.put(includer, included);
        // Reverse mapping: for included items, point back to including file
        setIncludedBy(includer, included);

        if (detectCycles) {
            detectCycles(includer);
        }
    }

    /** Record the list of included layouts from the given layout */
    private void setIncludedBy(String includer, List<String> included) {
        for (String target : included) {
            List<String> list = mIncludedBy.get(target);
            if (list == null) {
                list = new ArrayList<String>(2); // We don't expect many includes
                mIncludedBy.put(target, list);
            }
            if (!list.contains(includer)) {
                list.add(includer);
            }
        }
    }

    /** Start listening on project resources */
    public static void start() {
        assert sListener == null;
        sListener = new ResourceListener();
        ResourceManager.getInstance().addListener(sListener);
    }

    /** Stop listening on project resources */
    public static void stop() {
        assert sListener != null;
        ResourceManager.getInstance().addListener(sListener);
    }

    private static String getMapKey(ResourceFile resourceFile) {
        IAbstractFile file = resourceFile.getFile();
        String name = file.getName();
        String folderName = file.getParentFolder().getName();
        return getMapKey(folderName, name);
    }

    private static String getMapKey(IResource resourceFile) {
        String folderName = resourceFile.getParent().getName();
        String name = resourceFile.getName();
        return getMapKey(folderName, name);
    }

    private static String getResourceName(IResource resourceFile) {
        String name = resourceFile.getName();
        int baseEnd = name.length() - EXT_XML.length() - 1; // -1: the dot
        if (baseEnd > 0) {
            name = name.substring(0, baseEnd);
        }

        return name;
    }

    private static String getMapKey(String folderName, String name) {
        int baseEnd = name.length() - EXT_XML.length() - 1; // -1: the dot
        if (baseEnd > 0) {
            name = name.substring(0, baseEnd);
        }

        // Create a map key for the given resource file
        // This will map
        //     /res/layout/foo.xml => "foo"
        //     /res/layout-land/foo.xml => "-land/foo"

        if (FD_RES_LAYOUT.equals(folderName)) {
            // Normal case -- keep just the basename
            return name;
        } else {
            // Store the relative path from res/ on down, so
            // /res/layout-land/foo.xml becomes "layout-land/foo"
            //if (folderName.startsWith(FD_LAYOUT)) {
            //    folderName = folderName.substring(FD_LAYOUT.length());
            //}

            return folderName + WS_SEP + name;
        }
    }

    /** Listener of resource file saves, used to update layout inclusion data structures */
    private static class ResourceListener implements IResourceListener {
        @Override
        public void fileChanged(IProject project, ResourceFile file, int eventType) {
            if (sRefreshing) {
                return;
            }

            if ((eventType & (CHANGED | ADDED | REMOVED | CONTENT)) == 0) {
                return;
            }

            IncludeFinder finder = get(project);
            if (finder != null) {
                if (finder.updateFileIncludes(file, true)) {
                    finder.saveSettings();
                }
            }
        }

        @Override
        public void folderChanged(IProject project, ResourceFolder folder, int eventType) {
            // We only care about layout resource files
        }
    }

    // ----- Cycle detection -----

    private void detectCycles(String from) {
        // Perform DFS on the include graph and look for a cycle; if we find one, produce
        // a chain of includes on the way back to show to the user
        if (mIncludes.size() > 0) {
            Set<String> visiting = new HashSet<String>(mIncludes.size());
            String chain = dfs(from, visiting);
            if (chain != null) {
                addError(from, chain);
            } else {
                // Is there an existing error for us to clean up?
                removeErrors(from);
            }
        }
    }

    /** Format to chain include cycles in: a=>b=>c=>d etc */
    private final String CHAIN_FORMAT = "%1$s=>%2$s"; //$NON-NLS-1$

    private String dfs(String from, Set<String> visiting) {
        visiting.add(from);

        List<String> includes = mIncludes.get(from);
        if (includes != null && includes.size() > 0) {
            for (String include : includes) {
                if (visiting.contains(include)) {
                    return String.format(CHAIN_FORMAT, from, include);
                }
                String chain = dfs(include, visiting);
                if (chain != null) {
                    return String.format(CHAIN_FORMAT, from, chain);
                }
            }
        }

        visiting.remove(from);

        return null;
    }

    private void removeErrors(String from) {
        final IResource resource = findResource(from);
        if (resource != null) {
            try {
                final String markerId = IMarker.PROBLEM;

                IMarker[] markers = resource.findMarkers(markerId, true, IResource.DEPTH_ZERO);

                for (final IMarker marker : markers) {
                    String tmpMsg = marker.getAttribute(IMarker.MESSAGE, null);
                    if (tmpMsg == null || tmpMsg.startsWith(MESSAGE)) {
                        // Remove
                        runLater(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    sRefreshing = true;
                                    marker.delete();
                                } catch (CoreException e) {
                                    AdtPlugin.log(e, "Can't delete problem marker");
                                } finally {
                                    sRefreshing = false;
                                }
                            }
                        });
                    }
                }
            } catch (CoreException e) {
                // if we couldn't get the markers, then we just mark the file again
                // (since markerAlreadyExists is initialized to false, we do nothing)
            }
        }
    }

    /** Error message for cycles */
    private static final String MESSAGE = "Found cyclical <include> chain";

    private void addError(String from, String chain) {
        final IResource resource = findResource(from);
        if (resource != null) {
            final String markerId = IMarker.PROBLEM;
            final String message = String.format("%1$s: %2$s", MESSAGE, chain);
            final int lineNumber = 1;
            final int severity = IMarker.SEVERITY_ERROR;

            // check if there's a similar marker already, since aapt is launched twice
            boolean markerAlreadyExists = false;
            try {
                IMarker[] markers = resource.findMarkers(markerId, true, IResource.DEPTH_ZERO);

                for (IMarker marker : markers) {
                    int tmpLine = marker.getAttribute(IMarker.LINE_NUMBER, -1);
                    if (tmpLine != lineNumber) {
                        break;
                    }

                    int tmpSeverity = marker.getAttribute(IMarker.SEVERITY, -1);
                    if (tmpSeverity != severity) {
                        break;
                    }

                    String tmpMsg = marker.getAttribute(IMarker.MESSAGE, null);
                    if (tmpMsg == null || tmpMsg.equals(message) == false) {
                        break;
                    }

                    // if we're here, all the marker attributes are equals, we found it
                    // and exit
                    markerAlreadyExists = true;
                    break;
                }

            } catch (CoreException e) {
                // if we couldn't get the markers, then we just mark the file again
                // (since markerAlreadyExists is initialized to false, we do nothing)
            }

            if (!markerAlreadyExists) {
                runLater(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            sRefreshing = true;

                            // Adding a resource will force a refresh on the file;
                            // ignore these updates
                            BaseProjectHelper.markResource(resource, markerId, message, lineNumber,
                                    severity);
                        } finally {
                            sRefreshing = false;
                        }
                    }
                });
            }
        }
    }

    // FIXME: Find more standard Eclipse way to do this.
    // We need to run marker registration/deletion "later", because when the include
    // scanning is running it's in the middle of resource notification, so the IDE
    // throws an exception
    private static void runLater(Runnable runnable) {
        Display display = Display.findDisplay(Thread.currentThread());
        if (display != null) {
            display.asyncExec(runnable);
        } else {
            AdtPlugin.log(IStatus.WARNING, "Could not find display");
        }
    }

    /**
     * Finds the project resource for the given layout path
     *
     * @param from the resource name
     * @return the {@link IResource}, or null if not found
     */
    private IResource findResource(String from) {
        final IResource resource = mProject.findMember(WS_LAYOUTS + WS_SEP + from + '.' + EXT_XML);
        return resource;
    }

    /**
     * Creates a blank, project-less {@link IncludeFinder} <b>for use by unit tests
     * only</b>
     */
    @VisibleForTesting
    /* package */ static IncludeFinder create() {
        IncludeFinder finder = new IncludeFinder(null);
        finder.mIncludes = new HashMap<String, List<String>>();
        finder.mIncludedBy = new HashMap<String, List<String>>();
        return finder;
    }

    /** A reference to a particular file in the project */
    public static class Reference {
        /** The unique id referencing the file, such as (for res/layout-land/main.xml)
         * "layout-land/main") */
        private final String mId;

        /** The project containing the file */
        private final IProject mProject;

        /** The resource name of the file, such as (for res/layout/main.xml) "main" */
        private String mName;

        /** Creates a new include reference */
        private Reference(IProject project, String id) {
            super();
            mProject = project;
            mId = id;
        }

        /**
         * Returns the id identifying the given file within the project
         *
         * @return the id identifying the given file within the project
         */
        public String getId() {
            return mId;
        }

        /**
         * Returns the {@link IFile} in the project for the given file. May return null if
         * there is an error in locating the file or if the file no longer exists.
         *
         * @return the project file, or null
         */
        public IFile getFile() {
            String reference = mId;
            if (!reference.contains(WS_SEP)) {
                reference = FD_RES_LAYOUT + WS_SEP + reference;
            }

            String projectPath = FD_RESOURCES + WS_SEP + reference + '.' + EXT_XML;
            IResource member = mProject.findMember(projectPath);
            if (member instanceof IFile) {
                return (IFile) member;
            }

            return null;
        }

        /**
         * Returns a description of this reference, suitable to be shown to the user
         *
         * @return a display name for the reference
         */
        public String getDisplayName() {
            // The ID is deliberately kept in a pretty user-readable format but we could
            // consider prepending layout/ on ids that don't have it (to make the display
            // more uniform) or ripping out all layout[-constraint] prefixes out and
            // instead prepending @ etc.
            return mId;
        }

        /**
         * Returns the name of the reference, suitable for resource lookup. For example,
         * for "res/layout/main.xml", as well as for "res/layout-land/main.xml", this
         * would be "main".
         *
         * @return the resource name of the reference
         */
        public String getName() {
            if (mName == null) {
                mName = mId;
                int index = mName.lastIndexOf(WS_SEP);
                if (index != -1) {
                    mName = mName.substring(index + 1);
                }
            }

            return mName;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((mId == null) ? 0 : mId.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Reference other = (Reference) obj;
            if (mId == null) {
                if (other.mId != null)
                    return false;
            } else if (!mId.equals(other.mId))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "Reference [getId()=" + getId() //$NON-NLS-1$
                    + ", getDisplayName()=" + getDisplayName() //$NON-NLS-1$
                    + ", getName()=" + getName() //$NON-NLS-1$
                    + ", getFile()=" + getFile() + "]"; //$NON-NLS-1$
        }

        /**
         * Creates a reference to the given file
         *
         * @param file the file to create a reference for
         * @return a reference to the given file
         */
        public static Reference create(IFile file) {
            return new Reference(file.getProject(), getMapKey(file));
        }

        /**
         * Returns the resource name of this layout, such as {@code @layout/foo}.
         *
         * @return the resource name
         */
        public String getResourceName() {
            return '@' + FD_RES_LAYOUT + '/' + getName();
        }
    }

    /**
     * Returns a collection of layouts (expressed as resource names, such as
     * {@code @layout/foo} which would be invalid includes in the given layout
     * (because it would introduce a cycle)
     *
     * @param layout the layout file to check for cyclic dependencies from
     * @return a collection of layout resources which cannot be included from
     *         the given layout, never null
     */
    public Collection<String> getInvalidIncludes(IFile layout) {
        IProject project = layout.getProject();
        Reference self = Reference.create(layout);

        // Add anyone who transitively can reach this file via includes.
        LinkedList<Reference> queue = new LinkedList<Reference>();
        List<Reference> invalid = new ArrayList<Reference>();
        queue.add(self);
        invalid.add(self);
        Set<String> seen = new HashSet<String>();
        seen.add(self.getId());
        while (!queue.isEmpty()) {
            Reference reference = queue.removeFirst();
            String refId = reference.getId();

            // Look up both configuration specific includes as well as includes in the
            // base versions
            List<String> included = getIncludedBy(refId);
            if (refId.indexOf('/') != -1) {
                List<String> baseIncluded = getIncludedBy(reference.getName());
                if (included == null) {
                    included = baseIncluded;
                } else if (baseIncluded != null) {
                    included = new ArrayList<String>(included);
                    included.addAll(baseIncluded);
                }
            }

            if (included != null && included.size() > 0) {
                for (String id : included) {
                    if (!seen.contains(id)) {
                        seen.add(id);
                        Reference ref = new Reference(project, id);
                        invalid.add(ref);
                        queue.addLast(ref);
                    }
                }
            }
        }

        List<String> result = new ArrayList<String>();
        for (Reference reference : invalid) {
            result.add(reference.getResourceName());
        }

        return result;
    }
}

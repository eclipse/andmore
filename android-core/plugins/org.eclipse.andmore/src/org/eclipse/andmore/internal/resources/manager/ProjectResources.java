/*
 * Copyright (C) 2007 The Android Open Source Project
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

package org.eclipse.andmore.internal.resources.manager;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.ide.common.rendering.api.ResourceValue;
import com.android.ide.common.resources.IntArrayWrapper;
import com.android.ide.common.resources.ResourceFolder;
import com.android.ide.common.resources.ResourceItem;
import com.android.ide.common.resources.ResourceRepository;
import com.android.ide.common.resources.configuration.FolderConfiguration;
import com.android.io.IAbstractFolder;
import com.android.resources.ResourceType;
import com.android.util.Pair;

import org.eclipse.andmore.internal.sdk.ProjectState;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.andmore.io.IFolderWrapper;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Represents the resources of a project.
 * On top of the regular {@link ResourceRepository} features it provides:
 *<ul>
 *<li>configured resources contain the resources coming from the libraries.</li>
 *<li>resolution to and from resource integer (compiled value in R.java).</li>
 *<li>handles resource integer for non existing values of type ID. This is used when rendering.</li>
 *<li>layouts that have no been saved yet. This is handled by generating dynamic IDs
 *       on the fly.</li>
 *</ul>
 */
@SuppressWarnings("deprecation")
public class ProjectResources extends ResourceRepository {
    // project resources are defined as 0x7FXX#### where XX is the resource type (layout, drawable,
    // etc...). Using FF as the type allows for 255 resource types before we get a collision
    // which should be fine.
    private final static int DYNAMIC_ID_SEED_START = 0x7fff0000;

    /** Map of (name, id) for resources of type {@link ResourceType#ID} coming from R.java */
    private Map<ResourceType, Map<String, Integer>> mResourceValueMap;
    /** Map of (id, [name, resType]) for all resources coming from R.java */
    private Map<Integer, Pair<ResourceType, String>> mResIdValueToNameMap;
    /** Map of (int[], name) for styleable resources coming from R.java */
    private Map<IntArrayWrapper, String> mStyleableValueToNameMap;

    private final DynamicIdMap mDynamicIdMap = new DynamicIdMap(DYNAMIC_ID_SEED_START);
    private final IntArrayWrapper mWrapper = new IntArrayWrapper(null);
    private final IProject mProject;

    public static ProjectResources create(IProject project) {
        IFolder resFolder = project.getFolder(SdkConstants.FD_RESOURCES);

        return new ProjectResources(project, new IFolderWrapper(resFolder));
    }

    /**
     * Makes a ProjectResources for a given <var>project</var>.
     * @param project the project.
     */
    private ProjectResources(IProject project, IAbstractFolder resFolder) {
        super(resFolder, false /*isFrameworkRepository*/);
        mProject = project;
    }

    /**
     * Returns the resources values matching a given {@link FolderConfiguration}, this will
     * include library dependency.
     *
     * @param referenceConfig the configuration that each value must match.
     * @return a map with guaranteed to contain an entry for each {@link ResourceType}
     */
    @Override
    @NonNull
    public Map<ResourceType, Map<String, ResourceValue>> getConfiguredResources(
            @NonNull FolderConfiguration referenceConfig) {
        ensureInitialized();

        Map<ResourceType, Map<String, ResourceValue>> resultMap =
            new EnumMap<ResourceType, Map<String, ResourceValue>>(ResourceType.class);

        // if the project contains libraries, we need to add the libraries resources here
        // so that they are accessible to the layout rendering.
        if (mProject != null) {
            ProjectState state = Sdk.getProjectState(mProject);
            if (state != null) {
                List<IProject> libraries = state.getFullLibraryProjects();

                ResourceManager resMgr = ResourceManager.getInstance();

                // because aapt put all the library in their order in this array, the first
                // one will have priority over the 2nd one. So it's better to loop in the inverse
                // order and fill the map with resources that will be overwritten by higher
                // priority resources
                for (int i = libraries.size() - 1 ; i >= 0 ; i--) {
                    IProject library = libraries.get(i);

                    ProjectResources libRes = resMgr.getProjectResources(library);
                    if (libRes != null) {
                        // get the library resources, and only the library, not the dependencies
                        // so call doGetConfiguredResources() directly.
                        Map<ResourceType, Map<String, ResourceValue>> libMap =
                                libRes.doGetConfiguredResources(referenceConfig);

                        // we don't want to simply replace the whole map, but instead merge the
                        // content of any sub-map
                        for (Entry<ResourceType, Map<String, ResourceValue>> libEntry :
                                libMap.entrySet()) {

                            // get the map currently in the result map for this resource type
                            Map<String, ResourceValue> tempMap = resultMap.get(libEntry.getKey());
                            if (tempMap == null) {
                                // since there's no current map for this type, just add the map
                                // directly coming from the library resources
                                resultMap.put(libEntry.getKey(), libEntry.getValue());
                            } else {
                                // already a map for this type. add the resources from the
                                // library, this will override existing value, which is why
                                // we loop in a specific library order.
                                tempMap.putAll(libEntry.getValue());
                            }
                        }
                    }
                }
            }
        }

        // now the project resources themselves.
        Map<ResourceType, Map<String, ResourceValue>> thisProjectMap =
                doGetConfiguredResources(referenceConfig);

        // now merge the maps.
        for (Entry<ResourceType, Map<String, ResourceValue>> entry : thisProjectMap.entrySet()) {
            ResourceType type = entry.getKey();
            Map<String, ResourceValue> typeMap = resultMap.get(type);
            if (typeMap == null) {
                resultMap.put(type, entry.getValue());
            } else {
                typeMap.putAll(entry.getValue());
            }
        }

        return resultMap;
    }

    /**
     * Returns the {@link ResourceFolder} associated with a {@link IFolder}.
     * @param folder The {@link IFolder} object.
     * @return the {@link ResourceFolder} or null if it was not found.
     *
     * @see ResourceRepository#getResourceFolder(com.android.io.IAbstractFolder)
     */
    public ResourceFolder getResourceFolder(IFolder folder) {
        return getResourceFolder(new IFolderWrapper(folder));
    }

    /**
     * Resolves a compiled resource id into the resource name and type
     * @param id the resource integer id.
     * @return a {@link Pair} of 2 strings { name, type } or null if the id could not be resolved
     */
    public Pair<ResourceType, String> resolveResourceId(int id) {
        Pair<ResourceType, String> result = null;
        if (mResIdValueToNameMap != null) {
            result = mResIdValueToNameMap.get(id);
        }

        if (result == null) {
            synchronized (mDynamicIdMap) {
                result = mDynamicIdMap.resolveId(id);
            }
        }

        return result;
    }

    /**
     * Resolves a compiled styleable id of type int[] into the styleable name.
     */
    public String resolveStyleable(int[] id) {
        if (mStyleableValueToNameMap != null) {
            mWrapper.set(id);
            return mStyleableValueToNameMap.get(mWrapper);
        }

        return null;
    }

    /**
     * Returns the integer id of a resource given its type and name.
     * <p/>If the resource is of type {@link ResourceType#ID} and does not exist in the
     * internal map, then new id values are dynamically generated (and stored so that queries
     * with the same names will return the same value).
     */
    public Integer getResourceId(ResourceType type, String name) {
        Integer result = null;
        if (mResourceValueMap != null) {
            Map<String, Integer> map = mResourceValueMap.get(type);
            if (map != null) {
                result = map.get(name);
            }
        }

        if (result == null) {
            synchronized (mDynamicIdMap) {
                result = mDynamicIdMap.getId(type, name);
            }
        }

        return result;
    }

    /**
     * Resets the list of dynamic Ids. This list is used by
     * {@link #getResourceId(String, String)} when the resource query is an ID that doesn't
     * exist (for example for ID automatically generated in layout files that are not saved yet.)
     * <p/>This method resets those dynamic ID and must be called whenever the actual list of IDs
     * change.
     */
    public void resetDynamicIds() {
        synchronized (mDynamicIdMap) {
            mDynamicIdMap.reset(DYNAMIC_ID_SEED_START);
        }
    }

    @Override
    @NonNull
    protected ResourceItem createResourceItem(@NonNull String name) {
        return new ResourceItem(name);
    }

    /**
     * Sets compiled resource information.
     *
     * @param resIdValueToNameMap a map of compiled resource id to resource name.
     *    The map is acquired by the {@link ProjectResources} object.
     * @param styleableValueMap a map of (int[], name) for the styleable information. The map is
     *    acquired by the {@link ProjectResources} object.
     * @param resourceValueMap a map of (name, id) for resources of type {@link ResourceType#ID}.
     *    The list is acquired by the {@link ProjectResources} object.
     */
    void setCompiledResources(Map<Integer, Pair<ResourceType, String>> resIdValueToNameMap,
            Map<IntArrayWrapper, String> styleableValueMap,
            Map<ResourceType, Map<String, Integer>> resourceValueMap) {
        mResourceValueMap = resourceValueMap;
        mResIdValueToNameMap = resIdValueToNameMap;
        mStyleableValueToNameMap = styleableValueMap;

        resetDynamicIds();
    }
}

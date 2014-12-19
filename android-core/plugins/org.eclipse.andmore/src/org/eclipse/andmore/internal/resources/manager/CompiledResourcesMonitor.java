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
import com.android.annotations.Nullable;
import com.android.ide.common.resources.IntArrayWrapper;
import com.android.ide.common.xml.ManifestData;
import com.android.resources.ResourceType;
import com.android.util.Pair;

import org.eclipse.andmore.AdtConstants;
import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.project.AndroidManifestHelper;
import org.eclipse.andmore.internal.project.ProjectHelper;
import org.eclipse.andmore.internal.resources.manager.GlobalProjectMonitor.IFileListener;
import org.eclipse.andmore.internal.resources.manager.GlobalProjectMonitor.IProjectListener;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * A monitor for the compiled resources. This only monitors changes in the resources of type
 *  {@link ResourceType#ID}.
 */
public final class CompiledResourcesMonitor implements IFileListener, IProjectListener {

    private final static CompiledResourcesMonitor sThis = new CompiledResourcesMonitor();

    /**
     * Sets up the monitoring system.
     * @param monitor The main Resource Monitor.
     */
    public static void setupMonitor(GlobalProjectMonitor monitor) {
        monitor.addFileListener(sThis, IResourceDelta.ADDED | IResourceDelta.CHANGED);
        monitor.addProjectListener(sThis);
    }

    /**
     * private constructor to prevent construction.
     */
    private CompiledResourcesMonitor() {
    }


    /* (non-Javadoc)
     * Sent when a file changed : if the file is the R class, then it is parsed again to update
     * the internal data.
     *
     * @param file The file that changed.
     * @param markerDeltas The marker deltas for the file.
     * @param kind The change kind. This is equivalent to
     * {@link IResourceDelta#accept(IResourceDeltaVisitor)}
     *
     * @see IFileListener#fileChanged
     */
    @Override
    public void fileChanged(@NonNull IFile file, @NonNull IMarkerDelta[] markerDeltas,
            int kind, @Nullable String extension, int flags, boolean isAndroidProject) {
        if (!isAndroidProject || flags == IResourceDelta.MARKERS) {
            // Not Android or only the markers changed: not relevant
            return;
        }

        IProject project = file.getProject();

        if (file.getName().equals(SdkConstants.FN_COMPILED_RESOURCE_CLASS)) {
            // create the classname
            String className = getRClassName(project);
            if (className == null) {
                // We need to abort.
                AdtPlugin.log(IStatus.ERROR,
                        "fileChanged: failed to find manifest package for project %1$s", //$NON-NLS-1$
                        project.getName());
                return;
            }
            // path will begin with /projectName/bin/classes so we'll ignore that
            IPath relativeClassPath = file.getFullPath().removeFirstSegments(3);
            if (packagePathMatches(relativeClassPath.toString(), className)) {
                loadAndParseRClass(project, className);
            }
        }
    }

    /**
     * Check to see if the package section of the given path matches the packageName.
     * For example, /project/bin/classes/com/foo/app/R.class should match com.foo.app.R
     * @param path the pathname of the file to look at
     * @param packageName the package qualified name of the class
     * @return true if the package section of the path matches the package qualified name
     */
    private boolean packagePathMatches(String path, String packageName) {
        // First strip the ".class" off the end of the path
        String pathWithoutExtension = path.substring(0, path.indexOf(SdkConstants.DOT_CLASS));

        // then split the components of each path by their separators
        String [] pathArray = pathWithoutExtension.split(Pattern.quote(File.separator));
        String [] packageArray = packageName.split(AdtConstants.RE_DOT);


        int pathIndex = 0;
        int packageIndex = 0;

        while (pathIndex < pathArray.length && packageIndex < packageArray.length) {
            if (pathArray[pathIndex].equals(packageArray[packageIndex]) == false) {
                return false;
            }
            pathIndex++;
            packageIndex++;
        }
        // We may have matched all the way up to this point, but we're not sure it's a match
        // unless BOTH paths done
        return (pathIndex == pathArray.length && packageIndex == packageArray.length);
    }

    /**
     * Processes project close event.
     */
    @Override
    public void projectClosed(IProject project) {
        // the ProjectResources object will be removed by the ResourceManager.
    }

    /**
     * Processes project delete event.
     */
    @Override
    public void projectDeleted(IProject project) {
        // the ProjectResources object will be removed by the ResourceManager.
    }

    /**
     * Processes project open event.
     */
    @Override
    public void projectOpened(IProject project) {
        // when the project is opened, we get an ADDED event for each file, so we don't
        // need to do anything here.
    }

    @Override
    public void projectRenamed(IProject project, IPath from) {
        // renamed projects also trigger delete/open event,
        // so nothing to be done here.
    }

    /**
     * Processes existing project at init time.
     */
    @Override
    public void projectOpenedWithWorkspace(IProject project) {
        try {
            // check this is an android project
            if (project.hasNature(AdtConstants.NATURE_DEFAULT)) {
                String className = getRClassName(project);
                // Find the classname
                if (className == null) {
                    // We need to abort.
                    AdtPlugin.log(IStatus.ERROR,
                            "projectOpenedWithWorkspace: failed to find manifest package for project %1$s", //$NON-NLS-1$
                            project.getName());
                    return;
                }
                loadAndParseRClass(project, className);
            }
        } catch (CoreException e) {
            // pass
        }
    }

    @Override
    public void allProjectsOpenedWithWorkspace() {
        // nothing to do.
    }


    private void loadAndParseRClass(IProject project, String className) {
        try {
            // first check there's a ProjectResources to store the content
            ProjectResources projectResources = ResourceManager.getInstance().getProjectResources(
                    project);

            if (projectResources != null) {
                // create a temporary class loader to load the class
                ProjectClassLoader loader = new ProjectClassLoader(null /* parentClassLoader */,
                        project);

                try {
                    Class<?> clazz = loader.loadClass(className);

                    if (clazz != null) {
                        // create the maps to store the result of the parsing
                        Map<ResourceType, Map<String, Integer>> resourceValueMap =
                            new EnumMap<ResourceType, Map<String, Integer>>(ResourceType.class);
                        Map<Integer, Pair<ResourceType, String>> genericValueToNameMap =
                            new HashMap<Integer, Pair<ResourceType, String>>();
                        Map<IntArrayWrapper, String> styleableValueToNameMap =
                            new HashMap<IntArrayWrapper, String>();

                        // parse the class
                        if (parseClass(clazz, genericValueToNameMap, styleableValueToNameMap,
                                resourceValueMap)) {
                            // now we associate the maps to the project.
                            projectResources.setCompiledResources(genericValueToNameMap,
                                    styleableValueToNameMap, resourceValueMap);
                        }
                    }
                } catch (Error e) {
                    // Log this error with the class name we're trying to load and abort.
                    AdtPlugin.log(e, "loadAndParseRClass failed to find class %1$s", className); //$NON-NLS-1$
                }
            }
        } catch (ClassNotFoundException e) {
            // pass
        }
    }

    /**
     * Parses a R class, and fills maps.
     * @param rClass the class to parse
     * @param genericValueToNameMap
     * @param styleableValueToNameMap
     * @param resourceValueMap
     * @return True if we managed to parse the R class.
     */
    private boolean parseClass(Class<?> rClass,
            Map<Integer, Pair<ResourceType, String>> genericValueToNameMap,
            Map<IntArrayWrapper, String> styleableValueToNameMap, Map<ResourceType,
            Map<String, Integer>> resourceValueMap) {
        try {
            for (Class<?> inner : rClass.getDeclaredClasses()) {
                String resTypeName = inner.getSimpleName();
                ResourceType resType = ResourceType.getEnum(resTypeName);

                if (resType != null) {
                    Map<String, Integer> fullMap = new HashMap<String, Integer>();
                    resourceValueMap.put(resType, fullMap);

                    for (Field f : inner.getDeclaredFields()) {
                        // only process static final fields.
                        int modifiers = f.getModifiers();
                        if (Modifier.isStatic(modifiers)) {
                            Class<?> type = f.getType();
                            if (type.isArray() && type.getComponentType() == int.class) {
                                // if the object is an int[] we put it in the styleable map
                                styleableValueToNameMap.put(
                                        new IntArrayWrapper((int[]) f.get(null)),
                                        f.getName());
                            } else if (type == int.class) {
                                Integer value = (Integer) f.get(null);
                                genericValueToNameMap.put(value, Pair.of(resType, f.getName()));
                                fullMap.put(f.getName(), value);
                            } else {
                                assert false;
                            }
                        }
                    }
                }
            }

            return true;
        } catch (IllegalArgumentException e) {
        } catch (IllegalAccessException e) {
        }
        return false;
    }

    /**
     * Returns the class name of the R class, based on the project's manifest's package.
     *
     * @return A class name (e.g. "my.app.R") or null if there's no valid package in the manifest.
     */
    private String getRClassName(IProject project) {
        IFile manifestFile = ProjectHelper.getManifest(project);
        if (manifestFile != null && manifestFile.isSynchronized(IResource.DEPTH_ZERO)) {
            ManifestData data = AndroidManifestHelper.parseForData(manifestFile);
            if (data != null) {
                String javaPackage = data.getPackage();
                return javaPackage + ".R"; //$NON-NLS-1$
            }
        }
        return null;
    }

}

/*
 * Copyright (C) 2008 The Android Open Source Project
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

package org.eclipse.andmore.internal.editors.layout.descriptors;

import static com.android.SdkConstants.ANDROID_NS_NAME_PREFIX;
import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.AUTO_URI;
import static com.android.SdkConstants.CLASS_VIEWGROUP;
import static com.android.SdkConstants.URI_PREFIX;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.resources.ResourceFile;
import com.android.ide.common.resources.ResourceItem;
import com.android.resources.ResourceType;
import com.android.sdklib.IAndroidTarget;
import com.google.common.collect.Maps;
import com.google.common.collect.ObjectArrays;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.AdtUtils;
import org.eclipse.andmore.common.resources.platform.AttributeInfo;
import org.eclipse.andmore.common.resources.platform.AttrsXmlParser;
import org.eclipse.andmore.common.resources.platform.ViewClassInfo;
import org.eclipse.andmore.common.resources.platform.ViewClassInfo.LayoutParamsInfo;
import org.eclipse.andmore.internal.editors.IconFactory;
import org.eclipse.andmore.internal.editors.descriptors.AttributeDescriptor;
import org.eclipse.andmore.internal.editors.descriptors.DescriptorsUtils;
import org.eclipse.andmore.internal.editors.descriptors.ElementDescriptor;
import org.eclipse.andmore.internal.editors.manifest.ManifestInfo;
import org.eclipse.andmore.internal.resources.manager.ProjectResources;
import org.eclipse.andmore.internal.resources.manager.ResourceManager;
import org.eclipse.andmore.internal.sdk.AndroidTargetData;
import org.eclipse.andmore.internal.sdk.ProjectState;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.swt.graphics.Image;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service responsible for creating/managing {@link ViewElementDescriptor} objects for custom
 * View classes per project.
 * <p/>
 * The service provides an on-demand monitoring of custom classes to check for changes. Monitoring
 * starts once a request for an {@link ViewElementDescriptor} object has been done for a specific
 * class.
 * <p/>
 * The monitoring will notify a listener of any changes in the class triggering a change in its
 * associated {@link ViewElementDescriptor} object.
 * <p/>
 * If the custom class does not exist, no monitoring is put in place to avoid having to listen
 * to all class changes in the projects.
 */
public final class CustomViewDescriptorService {

    private static CustomViewDescriptorService sThis = new CustomViewDescriptorService();

    /**
     * Map where keys are the project, and values are another map containing all the known
     * custom View class for this project. The custom View class are stored in a map
     * where the keys are the fully qualified class name, and the values are their associated
     * {@link ViewElementDescriptor}.
     */
    private HashMap<IProject, HashMap<String, ViewElementDescriptor>> mCustomDescriptorMap =
        new HashMap<IProject, HashMap<String, ViewElementDescriptor>>();

    /**
     * TODO will be used to update the ViewElementDescriptor of the custom view when it
     * is modified (either the class itself or its attributes.xml)
     */
    @SuppressWarnings("unused")
    private ICustomViewDescriptorListener mListener;

    /**
     * Classes which implements this interface provide a method that deal with modifications
     * in custom View class triggering a change in its associated {@link ViewClassInfo} object.
     */
    public interface ICustomViewDescriptorListener {
        /**
         * Sent when a custom View class has changed and
         * its {@link ViewElementDescriptor} was modified.
         *
         * @param project the project containing the class.
         * @param className the fully qualified class name.
         * @param descriptor the updated ElementDescriptor.
         */
        public void updatedClassInfo(IProject project,
                                     String className,
                                     ViewElementDescriptor descriptor);
    }

    /**
     * Returns the singleton instance of {@link CustomViewDescriptorService}.
     */
    public static CustomViewDescriptorService getInstance() {
        return sThis;
    }

    /**
     * Sets the listener receiving custom View class modification notifications.
     * @param listener the listener to receive the notifications.
     *
     * TODO will be used to update the ViewElementDescriptor of the custom view when it
     * is modified (either the class itself or its attributes.xml)
     */
    public void setListener(ICustomViewDescriptorListener listener) {
        mListener = listener;
    }

    /**
     * Returns the {@link ViewElementDescriptor} for a particular project/class when the
     * fully qualified class name actually matches a class from the given project.
     * <p/>
     * Custom descriptors are created as needed.
     * <p/>
     * If it is the first time the {@link ViewElementDescriptor} is requested, the method
     * will check that the specified class is in fact a custom View class. Once this is
     * established, a monitoring for that particular class is initiated. Any change will
     * trigger a notification to the {@link ICustomViewDescriptorListener}.
     *
     * @param project the project containing the class.
     * @param fqcn the fully qualified name of the class.
     * @return a {@link ViewElementDescriptor} or <code>null</code> if the class was not
     *         a custom View class.
     */
    public ViewElementDescriptor getDescriptor(IProject project, String fqcn) {
        // look in the map first
        synchronized (mCustomDescriptorMap) {
            HashMap<String, ViewElementDescriptor> map = mCustomDescriptorMap.get(project);

            if (map != null) {
                ViewElementDescriptor descriptor = map.get(fqcn);
                if (descriptor != null) {
                    return descriptor;
                }
            }

            // if we step here, it looks like we haven't created it yet.
            // First lets check this is in fact a valid type in the project

            try {
                // We expect the project to be both opened and of java type (since it's an android
                // project), so we can create a IJavaProject object from our IProject.
                IJavaProject javaProject = JavaCore.create(project);

                // replace $ by . in the class name
                String javaClassName = fqcn.replaceAll("\\$", "\\."); //$NON-NLS-1$ //$NON-NLS-2$

                // look for the IType object for this class
                IType type = javaProject.findType(javaClassName);
                if (type != null && type.exists()) {
                    // the type exists. Let's get the parent class and its ViewClassInfo.

                    // get the type hierarchy
                    ITypeHierarchy hierarchy = type.newSupertypeHierarchy(
                            new NullProgressMonitor());

                    ViewElementDescriptor parentDescriptor = createViewDescriptor(
                            hierarchy.getSuperclass(type), project, hierarchy);

                    if (parentDescriptor != null) {
                        // we have a valid parent, lets create a new ViewElementDescriptor.
                        List<AttributeDescriptor> attrList = new ArrayList<AttributeDescriptor>();
                        List<AttributeDescriptor> paramList = new ArrayList<AttributeDescriptor>();
                        Map<ResourceFile, Long> files = findCustomDescriptors(project, type,
                                attrList, paramList);

                        AttributeDescriptor[] attributes =
                                getAttributeDescriptor(type, parentDescriptor);
                        if (!attrList.isEmpty()) {
                            attributes = join(attrList, attributes);
                        }
                        AttributeDescriptor[] layoutAttributes =
                                getLayoutAttributeDescriptors(type, parentDescriptor);
                        if (!paramList.isEmpty()) {
                            layoutAttributes = join(paramList, layoutAttributes);
                        }
                        String name = DescriptorsUtils.getBasename(fqcn);
                        ViewElementDescriptor descriptor = new CustomViewDescriptor(name, fqcn,
                                attributes,
                                layoutAttributes,
                                parentDescriptor.getChildren(),
                                project, files);
                        descriptor.setSuperClass(parentDescriptor);

                        synchronized (mCustomDescriptorMap) {
                            map = mCustomDescriptorMap.get(project);
                            if (map == null) {
                                map = new HashMap<String, ViewElementDescriptor>();
                                mCustomDescriptorMap.put(project, map);
                            }

                            map.put(fqcn, descriptor);
                        }

                        //TODO setup listener on this resource change.

                        return descriptor;
                    }
                }
            } catch (JavaModelException e) {
                // there was an error accessing any of the IType, we'll just return null;
            }
        }

        return null;
    }

    private static AttributeDescriptor[] join(
            @NonNull List<AttributeDescriptor> attributeList,
            @NonNull AttributeDescriptor[] attributes) {
        if (!attributeList.isEmpty()) {
            return ObjectArrays.concat(
                    attributeList.toArray(new AttributeDescriptor[attributeList.size()]),
                    attributes,
                    AttributeDescriptor.class);
        } else {
            return attributes;
        }

    }

    /** Cache used by {@link #getParser(ResourceFile)} */
    private Map<ResourceFile, AttrsXmlParser> mParserCache;

    private AttrsXmlParser getParser(ResourceFile file) {
        if (mParserCache == null) {
            mParserCache = new HashMap<ResourceFile, AttrsXmlParser>();
        }

        AttrsXmlParser parser = mParserCache.get(file);
        if (parser == null) {
            parser = new AttrsXmlParser(
                    file.getFile().getOsLocation(),
                    AdtPlugin.getDefault(), 20);
            parser.preload();
            mParserCache.put(file, parser);
        }

        return parser;
    }

    /** Compute/find the styleable resources for the given type, if possible */
    private Map<ResourceFile, Long> findCustomDescriptors(
            IProject project,
            IType type,
            List<AttributeDescriptor> customAttributes,
            List<AttributeDescriptor> customLayoutAttributes) {
        // Look up the project where the type is declared (could be a library project;
        // we cannot use type.getJavaProject().getProject())
        IProject library = getProjectDeclaringType(type);
        if (library == null) {
            library = project;
        }

        String className = type.getElementName();
        Set<ResourceFile> resourceFiles = findAttrsFiles(library, className);
        if (resourceFiles != null && resourceFiles.size() > 0) {
            String appUri = getAppResUri(project);
            Map<ResourceFile, Long> timestamps =
                    Maps.newHashMapWithExpectedSize(resourceFiles.size());
            for (ResourceFile file : resourceFiles) {
                AttrsXmlParser attrsXmlParser = getParser(file);
                String fqcn = type.getFullyQualifiedName();

                // Attributes
                ViewClassInfo classInfo = new ViewClassInfo(true, fqcn, className);
                attrsXmlParser.loadViewAttributes(classInfo);
                appendAttributes(customAttributes, classInfo.getAttributes(), appUri);

                // Layout params
                LayoutParamsInfo layoutInfo = new ViewClassInfo.LayoutParamsInfo(
                        classInfo, "Layout", null /*superClassInfo*/); //$NON-NLS-1$
                attrsXmlParser.loadLayoutParamsAttributes(layoutInfo);
                appendAttributes(customLayoutAttributes, layoutInfo.getAttributes(), appUri);

                timestamps.put(file, file.getFile().getModificationStamp());
            }

            return timestamps;
        }

        return null;
    }

    /**
     * Finds the set of XML files (if any) in the given library declaring
     * attributes for the given class name
     */
    @Nullable
    private static Set<ResourceFile> findAttrsFiles(IProject library, String className) {
        Set<ResourceFile> resourceFiles = null;
        ResourceManager manager = ResourceManager.getInstance();
        ProjectResources resources = manager.getProjectResources(library);
        if (resources != null) {
            Collection<ResourceItem> items =
                resources.getResourceItemsOfType(ResourceType.DECLARE_STYLEABLE);
            for (ResourceItem item : items) {
                String viewName = item.getName();
                if (viewName.equals(className)
                        || (viewName.startsWith(className)
                            && viewName.equals(className + "_Layout"))) { //$NON-NLS-1$
                    if (resourceFiles == null) {
                        resourceFiles = new HashSet<ResourceFile>();
                    }
                    resourceFiles.addAll(item.getSourceFileList());
                }
            }
        }
        return resourceFiles;
    }

    /**
     * Find the project containing this type declaration. We cannot use
     * {@link IType#getJavaProject()} since that will return the including
     * project and we're after the library project such that we can find the
     * attrs.xml file in the same project.
     */
    @Nullable
    private static IProject getProjectDeclaringType(IType type) {
        IClassFile classFile = type.getClassFile();
        if (classFile != null) {
            IPath path = classFile.getPath();
            IWorkspaceRoot workspace = ResourcesPlugin.getWorkspace().getRoot();
            IResource resource;
            if (path.isAbsolute()) {
                resource = AdtUtils.fileToResource(path.toFile());
            } else {
                resource = workspace.findMember(path);
            }
            if (resource != null && resource.getProject() != null) {
                return resource.getProject();
            }
        }

        return null;
    }

    /** Returns the name space to use for application attributes */
    private static String getAppResUri(IProject project) {
        String appResource;
        ProjectState projectState = Sdk.getProjectState(project);
        if (projectState != null && projectState.isLibrary()) {
            appResource = AUTO_URI;
        } else {
            ManifestInfo manifestInfo = ManifestInfo.get(project);
            appResource = URI_PREFIX + manifestInfo.getPackage();
        }
        return appResource;
    }


    /** Append the {@link AttributeInfo} objects converted {@link AttributeDescriptor}
     * objects into the given attribute list.
     * <p>
     * This is nearly identical to
     *  {@link DescriptorsUtils#appendAttribute(List, String, String, AttributeInfo, boolean, Map)}
     * but it handles namespace declarations in the attrs.xml file where the android:
     * namespace is included in the names.
     */
    private static void appendAttributes(List<AttributeDescriptor> attributes,
            AttributeInfo[] attributeInfos, String appResource) {
        // Custom attributes
        for (AttributeInfo info : attributeInfos) {
            String nsUri;
            if (info.getName().startsWith(ANDROID_NS_NAME_PREFIX)) {
                info.setName(info.getName().substring(ANDROID_NS_NAME_PREFIX.length()));
                nsUri = ANDROID_URI;
            } else {
                nsUri = appResource;
            }

            DescriptorsUtils.appendAttribute(attributes,
                    null /*elementXmlName*/, nsUri, info, false /*required*/,
                    null /*overrides*/);
        }
    }

    /**
     * Computes (if needed) and returns the {@link ViewElementDescriptor} for the specified type.
     *
     * @return A {@link ViewElementDescriptor} or null if type or typeHierarchy is null.
     */
    private ViewElementDescriptor createViewDescriptor(IType type, IProject project,
            ITypeHierarchy typeHierarchy) {
        // check if the type is a built-in View class.
        List<ViewElementDescriptor> builtInList = null;

        // give up if there's no type
        if (type == null) {
            return null;
        }

        String fqcn = type.getFullyQualifiedName();

        Sdk currentSdk = Sdk.getCurrent();
        if (currentSdk != null) {
            IAndroidTarget target = currentSdk.getTarget(project);
            if (target != null) {
                AndroidTargetData data = currentSdk.getTargetData(target);
                if (data != null) {
                    LayoutDescriptors descriptors = data.getLayoutDescriptors();
                    ViewElementDescriptor d = descriptors.findDescriptorByClass(fqcn);
                    if (d != null) {
                        return d;
                    }
                    builtInList = descriptors.getViewDescriptors();
                }
            }
        }

        // it's not a built-in class? Lets look if the superclass is built-in
        // give up if there's no type
        if (typeHierarchy == null) {
            return null;
        }

        IType parentType = typeHierarchy.getSuperclass(type);
        if (parentType != null) {
            ViewElementDescriptor parentDescriptor = createViewDescriptor(parentType, project,
                    typeHierarchy);

            if (parentDescriptor != null) {
                // parent class is a valid View class with a descriptor, so we create one
                // for this class.
                String name = DescriptorsUtils.getBasename(fqcn);
                // A custom view accepts children if its parent descriptor also does.
                // The only exception to this is ViewGroup, which accepts children even though
                // its parent does not.
                boolean isViewGroup = fqcn.equals(CLASS_VIEWGROUP);
                boolean hasChildren = isViewGroup || parentDescriptor.hasChildren();
                ViewElementDescriptor[] children = null;
                if (hasChildren && builtInList != null) {
                    // We can't figure out what the allowable children are by just
                    // looking at the class, so assume any View is valid
                    children = builtInList.toArray(new ViewElementDescriptor[builtInList.size()]);
                }
                ViewElementDescriptor descriptor = new CustomViewDescriptor(name, fqcn,
                        getAttributeDescriptor(type, parentDescriptor),
                        getLayoutAttributeDescriptors(type, parentDescriptor),
                        children, project, null);
                descriptor.setSuperClass(parentDescriptor);

                // add it to the map
                synchronized (mCustomDescriptorMap) {
                    HashMap<String, ViewElementDescriptor> map = mCustomDescriptorMap.get(project);

                    if (map == null) {
                        map = new HashMap<String, ViewElementDescriptor>();
                        mCustomDescriptorMap.put(project, map);
                    }

                    map.put(fqcn, descriptor);

                }

                //TODO setup listener on this resource change.

                return descriptor;
            }
        }

        // class is neither a built-in view class, nor extend one. return null.
        return null;
    }

    /**
     * Returns the array of {@link AttributeDescriptor} for the specified {@link IType}.
     * <p/>
     * The array should contain the descriptor for this type and all its supertypes.
     *
     * @param type the type for which the {@link AttributeDescriptor} are returned.
     * @param parentDescriptor the {@link ViewElementDescriptor} of the direct superclass.
     */
    private static AttributeDescriptor[] getAttributeDescriptor(IType type,
            ViewElementDescriptor parentDescriptor) {
        // TODO add the class attribute descriptors to the parent descriptors.
        return parentDescriptor.getAttributes();
    }

    private static AttributeDescriptor[] getLayoutAttributeDescriptors(IType type,
            ViewElementDescriptor parentDescriptor) {
        return parentDescriptor.getLayoutAttributes();
    }

    private class CustomViewDescriptor extends ViewElementDescriptor {
        private Map<ResourceFile, Long> mTimeStamps;
        private IProject mProject;

        public CustomViewDescriptor(String name, String fqcn, AttributeDescriptor[] attributes,
                AttributeDescriptor[] layoutAttributes,
                ElementDescriptor[] children, IProject project,
                Map<ResourceFile, Long> timestamps) {
            super(
                    fqcn, // xml name
                    name, // ui name
                    fqcn, // full class name
                    fqcn, // tooltip
                    null, // sdk_url
                    attributes,
                    layoutAttributes,
                    children,
                    false // mandatory
            );
            mTimeStamps = timestamps;
            mProject = project;
        }

        @Override
        public Image getGenericIcon() {
            IconFactory iconFactory = IconFactory.getInstance();

            int index = mXmlName.lastIndexOf('.');
            if (index != -1) {
                return iconFactory.getIcon(mXmlName.substring(index + 1),
                        "customView"); //$NON-NLS-1$
            }

            return iconFactory.getIcon("customView"); //$NON-NLS-1$
        }

        @Override
        public boolean syncAttributes() {
            // Check if any of the descriptors
            if (mTimeStamps != null) {
                // Prevent checking actual file timestamps too frequently on rapid burst calls
                long now = System.currentTimeMillis();
                if (now - sLastCheck < 1000) {
                    return true;
                }
                sLastCheck = now;

                // Check whether the resource files (typically just one) which defined
                // custom attributes for this custom view have changed, and if so,
                // refresh the attribute descriptors.
                // This doesn't work the cases where you add descriptors for a custom
                // view after using it, or add attributes in a separate file, but those
                // scenarios aren't quite as common (and would require a bit more expensive
                // analysis.)
                for (Map.Entry<ResourceFile, Long> entry : mTimeStamps.entrySet()) {
                    ResourceFile file = entry.getKey();
                    Long timestamp = entry.getValue();
                    boolean recompute = false;
                    if (file.getFile().getModificationStamp() > timestamp.longValue()) {
                        // One or more attributes changed: recompute
                        recompute = true;
                        mParserCache.remove(file);
                    }

                    if (recompute) {
                        IJavaProject javaProject = JavaCore.create(mProject);
                        String fqcn = getFullClassName();
                        IType type = null;
                        try {
                            type = javaProject.findType(fqcn);
                        } catch (CoreException e) {
                            AdtPlugin.log(e, null);
                        }
                        if (type == null || !type.exists()) {
                            return true;
                        }

                        List<AttributeDescriptor> attrList = new ArrayList<AttributeDescriptor>();
                        List<AttributeDescriptor> paramList = new ArrayList<AttributeDescriptor>();

                        mTimeStamps = findCustomDescriptors(mProject, type, attrList, paramList);

                        ViewElementDescriptor parentDescriptor = getSuperClassDesc();
                        AttributeDescriptor[] attributes =
                                getAttributeDescriptor(type, parentDescriptor);
                        if (!attrList.isEmpty()) {
                            attributes = join(attrList, attributes);
                        }
                        attributes = attrList.toArray(new AttributeDescriptor[attrList.size()]);
                        setAttributes(attributes);

                        return false;
                    }
                }
            }

            return true;
        }
    }

    /** Timestamp of the most recent {@link CustomViewDescriptor#syncAttributes} check */
    private static long sLastCheck;
}

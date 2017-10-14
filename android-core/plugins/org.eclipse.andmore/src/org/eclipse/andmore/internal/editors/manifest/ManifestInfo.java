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

package org.eclipse.andmore.internal.editors.manifest;

import static com.android.SdkConstants.ANDROID_STYLE_RESOURCE_PREFIX;
import static com.android.SdkConstants.CLASS_ACTIVITY;
import static com.android.SdkConstants.NS_RESOURCES;
import static com.android.xml.AndroidManifest.ATTRIBUTE_ICON;
import static com.android.xml.AndroidManifest.ATTRIBUTE_LABEL;
import static com.android.xml.AndroidManifest.ATTRIBUTE_MIN_SDK_VERSION;
import static com.android.xml.AndroidManifest.ATTRIBUTE_NAME;
import static com.android.xml.AndroidManifest.ATTRIBUTE_PACKAGE;
import static com.android.xml.AndroidManifest.ATTRIBUTE_PARENT_ACTIVITY_NAME;
import static com.android.xml.AndroidManifest.ATTRIBUTE_SUPPORTS_RTL;
import static com.android.xml.AndroidManifest.ATTRIBUTE_TARGET_SDK_VERSION;
import static com.android.xml.AndroidManifest.ATTRIBUTE_THEME;
import static com.android.xml.AndroidManifest.ATTRIBUTE_UI_OPTIONS;
import static com.android.xml.AndroidManifest.ATTRIBUTE_VALUE;
import static com.android.xml.AndroidManifest.NODE_ACTIVITY;
import static com.android.xml.AndroidManifest.NODE_METADATA;
import static com.android.xml.AndroidManifest.NODE_USES_SDK;
import static com.android.xml.AndroidManifest.VALUE_PARENT_ACTIVITY;
import static org.eclipse.jdt.core.search.IJavaSearchConstants.REFERENCES;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.io.IAbstractFile;
import com.android.io.StreamException;
import com.android.resources.ScreenSize;
import com.android.sdklib.IAndroidTarget;
import com.android.utils.Pair;
import com.android.xml.AndroidManifest;

import org.eclipse.andmore.AndmoreAndroidPlugin;
import org.eclipse.andmore.internal.project.BaseProjectHelper;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.andmore.io.IFolderWrapper;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.jdt.internal.core.BinaryType;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathExpressionException;

/**
 * Retrieves and caches manifest information such as the themes to be used for
 * a given activity.
 *
 * @see AndroidManifest
 */
public class ManifestInfo {

    public static class ActivityAttributes {
        @Nullable
        private final String mIcon;
        @Nullable
        private final String mLabel;
        @NonNull
        private final String mName;
        @Nullable
        private final String mParentActivity;
        @Nullable
        private final String mTheme;
        @Nullable
        private final String mUiOptions;

        public ActivityAttributes(Element activity, String packageName) {

            // Get activity name.
            String name = activity.getAttributeNS(NS_RESOURCES, ATTRIBUTE_NAME);
            if (name == null || name.length() == 0) {
                throw new RuntimeException("Activity name cannot be empty");
            }
            int index = name.indexOf('.');
            if (index <= 0 && packageName != null && !packageName.isEmpty()) {
              name =  packageName + (index == -1 ? "." : "") + name;
            }
            mName = name;

            // Get activity icon.
            String value = activity.getAttributeNS(NS_RESOURCES, ATTRIBUTE_ICON);
            if (value != null && value.length() > 0) {
                mIcon = value;
            } else {
                mIcon = null;
            }

            // Get activity label.
            value = activity.getAttributeNS(NS_RESOURCES, ATTRIBUTE_LABEL);
            if (value != null && value.length() > 0) {
                mLabel = value;
            } else {
                mLabel = null;
            }

            // Get activity parent. Also search the meta-data for parent info.
            value = activity.getAttributeNS(NS_RESOURCES, ATTRIBUTE_PARENT_ACTIVITY_NAME);
            if (value == null || value.length() == 0) {
                // TODO: Not sure if meta data can be used for API Level > 16
                NodeList metaData = activity.getElementsByTagName(NODE_METADATA);
                for (int j = 0, m = metaData.getLength(); j < m; j++) {
                    Element data = (Element) metaData.item(j);
                    String metadataName = data.getAttributeNS(NS_RESOURCES, ATTRIBUTE_NAME);
                    if (VALUE_PARENT_ACTIVITY.equals(metadataName)) {
                        value = data.getAttributeNS(NS_RESOURCES, ATTRIBUTE_VALUE);
                        if (value != null) {
                            index = value.indexOf('.');
                            if (index <= 0 && packageName != null && !packageName.isEmpty()) {
                                value = packageName + (index == -1 ? "." : "") + value;
                                break;
                            }
                        }
                    }
                }
            }
            if (value != null && value.length() > 0) {
                mParentActivity = value;
            } else {
                mParentActivity = null;
            }

            // Get activity theme.
            value = activity.getAttributeNS(NS_RESOURCES, ATTRIBUTE_THEME);
            if (value != null && value.length() > 0) {
                mTheme = value;
            } else {
                mTheme = null;
            }

            // Get UI options.
            value = activity.getAttributeNS(NS_RESOURCES, ATTRIBUTE_UI_OPTIONS);
            if (value != null && value.length() > 0) {
                mUiOptions = value;
            } else {
                mUiOptions = null;
            }
        }

        @Nullable
        public String getIcon() {
            return mIcon;
        }

        @Nullable
        public String getLabel() {
            return mLabel;
        }

        public String getName() {
            return mName;
        }

        @Nullable
        public String getParentActivity() {
            return mParentActivity;
        }

        @Nullable
        public String getTheme() {
            return mTheme;
        }

        @Nullable
        public String getUiOptions() {
            return mUiOptions;
        }
    }

    /**
     * The maximum number of milliseconds to search for an activity in the codebase when
     * attempting to associate layouts with activities in
     * {@link #guessActivity(IFile, String)}
     */
    private static final int SEARCH_TIMEOUT_MS = 3000;

    private final IProject mProject;
    private String mPackage;
    private String mManifestTheme;
    private Map<String, ActivityAttributes> mActivityAttributes;
    private IAbstractFile mManifestFile;
    private long mLastModified;
    private long mLastChecked;
    private String mMinSdkName;
    private int mMinSdk;
    private int mTargetSdk;
    private String mApplicationIcon;
    private String mApplicationLabel;
    private boolean mApplicationSupportsRtl;

    /**
     * Qualified name for the per-project non-persistent property storing the
     * {@link ManifestInfo} for this project
     */
    final static QualifiedName MANIFEST_FINDER = new QualifiedName(AndmoreAndroidPlugin.PLUGIN_ID,
            "manifest"); //$NON-NLS-1$

    /**
     * Constructs an {@link ManifestInfo} for the given project. Don't use this method;
     * use the {@link #get} factory method instead.
     *
     * @param project project to create an {@link ManifestInfo} for
     */
    private ManifestInfo(IProject project) {
        mProject = project;
    }

    /**
     * Clears the cached manifest information. The next get call on one of the
     * properties will cause the information to be refreshed.
     */
    public void clear() {
        mLastChecked = 0;
    }

    /**
     * Returns the {@link ManifestInfo} for the given project
     *
     * @param project the project the finder is associated with
     * @return a {@ManifestInfo} for the given project, never null
     */
    @NonNull
    public static ManifestInfo get(IProject project) {
        ManifestInfo finder = null;
        try {
            finder = (ManifestInfo) project.getSessionProperty(MANIFEST_FINDER);
        } catch (CoreException e) {
            // Not a problem; we will just create a new one
        }

        if (finder == null) {
            finder = new ManifestInfo(project);
            try {
                project.setSessionProperty(MANIFEST_FINDER, finder);
            } catch (CoreException e) {
                AndmoreAndroidPlugin.log(e, "Can't store ManifestInfo");
            }
        }

        return finder;
    }

    /**
     * Ensure that the package, theme and activity maps are initialized and up to date
     * with respect to the manifest file
     */
    private void sync() {
        // Since each of the accessors call sync(), allow a bunch of immediate
        // accessors to all bypass the file stat() below
        long now = System.currentTimeMillis();
        if (now - mLastChecked < 50 && mManifestFile != null) {
            return;
        }
        mLastChecked = now;

        if (mManifestFile == null) {
            IFolderWrapper projectFolder = new IFolderWrapper(mProject);
            mManifestFile = AndroidManifest.getManifest(projectFolder);
            if (mManifestFile == null) {
                return;
            }
        }

        // Check to see if our data is up to date
        long fileModified = mManifestFile.getModificationStamp();
        if (fileModified == mLastModified) {
            // Already have up to date data
            return;
        }
        mLastModified = fileModified;

        mActivityAttributes = new HashMap<String, ActivityAttributes>();
        mManifestTheme = null;
        mTargetSdk = 1; // Default when not specified
        mMinSdk = 1; // Default when not specified
        mMinSdkName = "1"; // Default when not specified
        mPackage = ""; //$NON-NLS-1$
        mApplicationIcon = null;
        mApplicationLabel = null;
        mApplicationSupportsRtl = false;

        Document document = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            InputSource is = new InputSource(mManifestFile.getContents());

            factory.setNamespaceAware(true);
            factory.setValidating(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.parse(is);

            Element root = document.getDocumentElement();
            mPackage = root.getAttribute(ATTRIBUTE_PACKAGE);
            NodeList activities = document.getElementsByTagName(NODE_ACTIVITY);
            for (int i = 0, n = activities.getLength(); i < n; i++) {
                Element activity = (Element) activities.item(i);
                ActivityAttributes info = new ActivityAttributes(activity, mPackage);
                mActivityAttributes.put(info.getName(), info);
            }

            NodeList applications = root.getElementsByTagName(AndroidManifest.NODE_APPLICATION);
            if (applications.getLength() > 0) {
                assert applications.getLength() == 1;
                Element application = (Element) applications.item(0);
                if (application.hasAttributeNS(NS_RESOURCES, ATTRIBUTE_ICON)) {
                    mApplicationIcon = application.getAttributeNS(NS_RESOURCES, ATTRIBUTE_ICON);
                }
                if (application.hasAttributeNS(NS_RESOURCES, ATTRIBUTE_LABEL)) {
                    mApplicationLabel = application.getAttributeNS(NS_RESOURCES, ATTRIBUTE_LABEL);
                }
                if (SdkConstants.VALUE_TRUE.equals(application.getAttributeNS(NS_RESOURCES,
                        ATTRIBUTE_SUPPORTS_RTL))) {
                    mApplicationSupportsRtl = true;
                }

                String defaultTheme = application.getAttributeNS(NS_RESOURCES, ATTRIBUTE_THEME);
                if (defaultTheme != null && !defaultTheme.isEmpty()) {
                    // From manifest theme documentation:
                    // "If that attribute is also not set, the default system theme is used."
                    mManifestTheme = defaultTheme;
                }
            }

            // Look up target SDK
            NodeList usesSdks = root.getElementsByTagName(NODE_USES_SDK);
            if (usesSdks.getLength() > 0) {
                Element usesSdk = (Element) usesSdks.item(0);
                mMinSdk = getApiVersion(usesSdk, ATTRIBUTE_MIN_SDK_VERSION, 1);
                mTargetSdk = getApiVersion(usesSdk, ATTRIBUTE_TARGET_SDK_VERSION, mMinSdk);
            }

        } catch (SAXException e) {
            AndmoreAndroidPlugin.log(e, "Malformed manifest");
        } catch (Exception e) {
            AndmoreAndroidPlugin.log(e, "Could not read Manifest data");
        }
    }

    private int getApiVersion(Element usesSdk, String attribute, int defaultApiLevel) {
        String valueString = null;
        if (usesSdk.hasAttributeNS(NS_RESOURCES, attribute)) {
            valueString = usesSdk.getAttributeNS(NS_RESOURCES, attribute);
            if (attribute.equals(ATTRIBUTE_MIN_SDK_VERSION)) {
                mMinSdkName = valueString;
            }
        }

        if (valueString != null) {
            int apiLevel = -1;
            try {
                apiLevel = Integer.valueOf(valueString);
            } catch (NumberFormatException e) {
                // Handle codename
                if (Sdk.getCurrent() != null) {
                    IAndroidTarget target = Sdk.getCurrent().getTargetFromHashString(
                            "android-" + valueString); //$NON-NLS-1$
                    if (target != null) {
                        // codename future API level is current api + 1
                        apiLevel = target.getVersion().getApiLevel() + 1;
                    }
                }
            }

            return apiLevel;
        }

        return defaultApiLevel;
    }

    /**
     * Returns the default package registered in the Android manifest
     *
     * @return the default package registered in the manifest
     */
    @NonNull
    public String getPackage() {
        sync();
        return mPackage;
    }

    /**
     * Returns a map from activity full class names to the corresponding {@link ActivityAttributes}.
     *
     * @return a map from activity fqcn to ActivityAttributes
     */
    @NonNull
    public Map<String, ActivityAttributes> getActivityAttributesMap() {
        sync();
        return mActivityAttributes;
    }

    /**
     * Returns the attributes of an activity given its full class name.
     */
    @Nullable
    public ActivityAttributes getActivityAttributes(String activity) {
        return getActivityAttributesMap().get(activity);
    }

    /**
     * Returns the manifest theme registered on the application, if any
     *
     * @return a manifest theme, or null if none was registered
     */
    @Nullable
    public String getManifestTheme() {
        sync();
        return mManifestTheme;
    }

    /**
     * Returns the default theme for this project, by looking at the manifest default
     * theme registration, target SDK, rendering target, etc.
     *
     * @param renderingTarget the rendering target use to render the theme, or null
     * @param screenSize the screen size to obtain a default theme for, or null if unknown
     * @return the theme to use for this project, never null
     */
    @NonNull
    public String getDefaultTheme(IAndroidTarget renderingTarget, ScreenSize screenSize) {
        sync();

        if (mManifestTheme != null) {
            return mManifestTheme;
        }

        int renderingTargetSdk = mTargetSdk;
        if (renderingTarget != null) {
            renderingTargetSdk = renderingTarget.getVersion().getApiLevel();
        }

        int apiLevel = Math.min(mTargetSdk, renderingTargetSdk);
        // For now this theme works only on XLARGE screens. When it works for all sizes,
        // add that new apiLevel to this check.
        if (apiLevel >= 11 && screenSize == ScreenSize.XLARGE || apiLevel >= 14) {
            return ANDROID_STYLE_RESOURCE_PREFIX + "Theme.Holo"; //$NON-NLS-1$
        } else {
            return ANDROID_STYLE_RESOURCE_PREFIX + "Theme"; //$NON-NLS-1$
        }
    }

    /**
     * Returns the application icon, or null
     *
     * @return the application icon, or null
     */
    @Nullable
    public String getApplicationIcon() {
        sync();
        return mApplicationIcon;
    }

    /**
     * Returns the application label, or null
     *
     * @return the application label, or null
     */
    @Nullable
    public String getApplicationLabel() {
        sync();
        return mApplicationLabel;
    }

    /**
     * Returns true if the application has RTL support.
     *
     * @return true if the application has RTL support.
     */
    public boolean isRtlSupported() {
        sync();
        return mApplicationSupportsRtl;
    }

    /**
     * Returns the target SDK version
     *
     * @return the target SDK version
     */
    public int getTargetSdkVersion() {
        sync();
        return mTargetSdk;
    }

    /**
     * Returns the minimum SDK version
     *
     * @return the minimum SDK version
     */
    public int getMinSdkVersion() {
        sync();
        return mMinSdk;
    }

    /**
     * Returns the minimum SDK version name (which may not be a numeric string, e.g.
     * it could be a codename). It will never be null or empty; if no min sdk version
     * was specified in the manifest, the return value will be "1". Use
     * {@link #getMinSdkCodeName()} instead if you want to look up whether there is a code name.
     *
     * @return the minimum SDK version
     */
    @NonNull
    public String getMinSdkName() {
        sync();
        if (mMinSdkName == null || mMinSdkName.isEmpty()) {
            mMinSdkName = "1"; //$NON-NLS-1$
        }

        return mMinSdkName;
    }

    /**
     * Returns the code name used for the minimum SDK version, if any.
     *
     * @return the minSdkVersion codename or null
     */
    @Nullable
    public String getMinSdkCodeName() {
        String minSdkName = getMinSdkName();
        if (!Character.isDigit(minSdkName.charAt(0))) {
            return minSdkName;
        }

        return null;
    }

    /**
     * Returns the {@link IPackageFragment} for the package registered in the manifest
     *
     * @return the {@link IPackageFragment} for the package registered in the manifest
     */
    @Nullable
    public IPackageFragment getPackageFragment() {
        sync();
        try {
            IJavaProject javaProject = BaseProjectHelper.getJavaProject(mProject);
            if (javaProject != null) {
                IPackageFragmentRoot root = ManifestInfo.getSourcePackageRoot(javaProject);
                if (root != null) {
                    return root.getPackageFragment(mPackage);
                }
            }
        } catch (CoreException e) {
            AndmoreAndroidPlugin.log(e, null);
        }

        return null;
    }

    /**
     * Returns the activity associated with the given layout file. Makes an educated guess
     * by peeking at the usages of the R.layout.name field corresponding to the layout and
     * if it finds a usage.
     *
     * @param project the project containing the layout
     * @param layoutName the layout whose activity we want to look up
     * @param pkg the package containing activities
     * @return the activity name
     */
    @Nullable
    public static String guessActivity(IProject project, String layoutName, String pkg) {
        List<String> activities = guessActivities(project, layoutName, pkg);
        if (activities.size() > 0) {
            return activities.get(0);
        } else {
            return null;
        }
    }

    /**
     * Returns the activities associated with the given layout file. Makes an educated guess
     * by peeking at the usages of the R.layout.name field corresponding to the layout and
     * if it finds a usage.
     *
     * @param project the project containing the layout
     * @param layoutName the layout whose activity we want to look up
     * @param pkg the package containing activities
     * @return the activity name
     */
    @NonNull
    public static List<String> guessActivities(IProject project, String layoutName, String pkg) {
        final LinkedList<String> activities = new LinkedList<String>();
        SearchRequestor requestor = new SearchRequestor() {
            @Override
            public void acceptSearchMatch(SearchMatch match) throws CoreException {
                Object element = match.getElement();
                if (element instanceof IMethod) {
                    IMethod method = (IMethod) element;
                    IType declaringType = method.getDeclaringType();
                    String fqcn = declaringType.getFullyQualifiedName();

                    if ((declaringType.getSuperclassName() != null &&
                            declaringType.getSuperclassName().endsWith("Activity")) //$NON-NLS-1$
                        || method.getElementName().equals("onCreate")) { //$NON-NLS-1$
                        activities.addFirst(fqcn);
                    } else {
                        activities.addLast(fqcn);
                    }
                }
            }
        };
        try {
            IJavaProject javaProject = BaseProjectHelper.getJavaProject(project);
            if (javaProject == null) {
                return Collections.emptyList();
            }
            // TODO - look around a bit more and see if we can figure out whether the
            // call if from within a setContentView call!

            // Search for which java classes call setContentView(R.layout.layoutname);
            String typeFqcn = "R.layout"; //$NON-NLS-1$
            if (pkg != null) {
                typeFqcn = pkg + '.' + typeFqcn;
            }

            IType type = javaProject.findType(typeFqcn);
            if (type != null) {
                IField field = type.getField(layoutName);
                if (field.exists()) {
                    SearchPattern pattern = SearchPattern.createPattern(field, REFERENCES);
                    try {
                        search(requestor, javaProject, pattern);
                    } catch (OperationCanceledException canceled) {
                        // pass
                    }
                }
            }
        } catch (CoreException e) {
            AndmoreAndroidPlugin.log(e, null);
        }

        return activities;
    }

    /**
     * Returns all activities found in the given project (including those in libraries,
     * except for android.jar itself)
     *
     * @param project the project
     * @return a list of activity classes as fully qualified class names
     */
    @SuppressWarnings("restriction") // BinaryType
    @NonNull
    public static List<String> getProjectActivities(IProject project) {
        final List<String> activities = new ArrayList<String>();
        try {
            final IJavaProject javaProject = BaseProjectHelper.getJavaProject(project);
            if (javaProject != null) {
                IType[] activityTypes = new IType[0];
                IType activityType = javaProject.findType(CLASS_ACTIVITY);
                if (activityType != null) {
                    ITypeHierarchy hierarchy =
                        activityType.newTypeHierarchy(javaProject, new NullProgressMonitor());
                    activityTypes = hierarchy.getAllSubtypes(activityType);
                    for (IType type : activityTypes) {
                        if (type instanceof BinaryType && (type.getClassFile() == null
                                    || type.getClassFile().getResource() == null)) {
                            continue;
                        }
                        activities.add(type.getFullyQualifiedName());
                    }
                }
            }
        } catch (CoreException e) {
            AndmoreAndroidPlugin.log(e, null);
        }

        return activities;
    }


    /**
     * Returns the activity associated with the given layout file.
     * <p>
     * This is an alternative to {@link #guessActivity(IFile, String)}. Whereas
     * guessActivity simply looks for references to "R.layout.foo", this method searches
     * for all usages of Activity#setContentView(int), and for each match it looks up the
     * corresponding call text (such as "setContentView(R.layout.foo)"). From this it uses
     * a regexp to pull out "foo" from this, and stores the association that layout "foo"
     * is associated with the activity class that contained the setContentView call.
     * <p>
     * This has two potential advantages:
     * <ol>
     * <li>It can be faster. We do the reference search -once-, and we've built a map of
     * all the layout-to-activity mappings which we can then immediately look up other
     * layouts for, which is particularly useful at startup when we have to compute the
     * layout activity associations to populate the theme choosers.
     * <li>It can be more accurate. Just because an activity references an "R.layout.foo"
     * field doesn't mean it's setting it as a content view.
     * </ol>
     * However, this second advantage is also its chief problem. There are some common
     * code constructs which means that the associated layout is not explicitly referenced
     * in a direct setContentView call; on a couple of sample projects I tested I found
     * patterns like for example "setContentView(v)" where "v" had been computed earlier.
     * Therefore, for now we're going to stick with the more general approach of just
     * looking up each field when needed. We're keeping the code around, though statically
     * compiled out with the "if (false)" construct below in case we revisit this.
     *
     * @param layoutFile the layout whose activity we want to look up
     * @return the activity name
     */
    @SuppressWarnings("all")
    @Nullable
    public String guessActivityBySetContentView(String layoutName) {
        if (false) {
            // These should be fields
            final Pattern LAYOUT_FIELD_PATTERN =
                Pattern.compile("R\\.layout\\.([a-z0-9_]+)"); //$NON-NLS-1$
            Map<String, String> mUsages = null;

            sync();
            if (mUsages == null) {
                final Map<String, String> usages = new HashMap<String, String>();
                mUsages = usages;
                SearchRequestor requestor = new SearchRequestor() {
                    @Override
                    public void acceptSearchMatch(SearchMatch match) throws CoreException {
                        Object element = match.getElement();
                        if (element instanceof IMethod) {
                            IMethod method = (IMethod) element;
                            IType declaringType = method.getDeclaringType();
                            String fqcn = declaringType.getFullyQualifiedName();
                            IDocumentProvider provider = new TextFileDocumentProvider();
                            IResource resource = match.getResource();
                            try {
                                provider.connect(resource);
                                IDocument document = provider.getDocument(resource);
                                if (document != null) {
                                    String matchText = document.get(match.getOffset(),
                                            match.getLength());
                                    Matcher matcher = LAYOUT_FIELD_PATTERN.matcher(matchText);
                                    if (matcher.find()) {
                                        usages.put(matcher.group(1), fqcn);
                                    }
                                }
                            } catch (Exception e) {
                                AndmoreAndroidPlugin.log(e, "Can't find range information for %1$s",
                                        resource.getName());
                            } finally {
                                provider.disconnect(resource);
                            }
                        }
                    }
                };
                try {
                    IJavaProject javaProject = BaseProjectHelper.getJavaProject(mProject);
                    if (javaProject == null) {
                        return null;
                    }

                    // Search for which java classes call setContentView(R.layout.layoutname);
                    String typeFqcn = "R.layout"; //$NON-NLS-1$
                    if (mPackage != null) {
                        typeFqcn = mPackage + '.' + typeFqcn;
                    }

                    IType activityType = javaProject.findType(CLASS_ACTIVITY);
                    if (activityType != null) {
                        IMethod method = activityType.getMethod(
                                "setContentView", new String[] {"I"}); //$NON-NLS-1$ //$NON-NLS-2$
                        if (method.exists()) {
                            SearchPattern pattern = SearchPattern.createPattern(method,
                                    REFERENCES);
                            search(requestor, javaProject, pattern);
                        }
                    }
                } catch (CoreException e) {
                    AndmoreAndroidPlugin.log(e, null);
                }
            }

            return mUsages.get(layoutName);
        }

        return null;
    }

    /**
     * Performs a search using the given pattern, scope and handler. The search will abort
     * if it takes longer than {@link #SEARCH_TIMEOUT_MS} milliseconds.
     */
    private static void search(SearchRequestor requestor, IJavaProject javaProject,
            SearchPattern pattern) throws CoreException {
        // Find the package fragment specified in the manifest; the activities should
        // live there.
        IJavaSearchScope scope = createPackageScope(javaProject);

        SearchParticipant[] participants = new SearchParticipant[] {
            SearchEngine.getDefaultSearchParticipant()
        };
        SearchEngine engine = new SearchEngine();

        final long searchStart = System.currentTimeMillis();
        NullProgressMonitor monitor = new NullProgressMonitor() {
            private boolean mCancelled;
            @Override
            public void internalWorked(double work) {
                long searchEnd = System.currentTimeMillis();
                if (searchEnd - searchStart > SEARCH_TIMEOUT_MS) {
                    mCancelled = true;
                }
            }

            @Override
            public boolean isCanceled() {
                return mCancelled;
            }
        };
        engine.search(pattern, participants, scope, requestor, monitor);
    }

    /** Creates a package search scope for the first package root in the given java project */
    private static IJavaSearchScope createPackageScope(IJavaProject javaProject) {
        IPackageFragmentRoot packageRoot = getSourcePackageRoot(javaProject);

        IJavaSearchScope scope;
        if (packageRoot != null) {
            IJavaElement[] scopeElements = new IJavaElement[] { packageRoot };
            scope = SearchEngine.createJavaSearchScope(scopeElements);
        } else {
            scope = SearchEngine.createWorkspaceScope();
        }
        return scope;
    }

    /**
     * Returns the first package root for the given java project
     *
     * @param javaProject the project to search in
     * @return the first package root, or null
     */
    @Nullable
    public static IPackageFragmentRoot getSourcePackageRoot(IJavaProject javaProject) {
        IPackageFragmentRoot packageRoot = null;
        List<IPath> sources = BaseProjectHelper.getSourceClasspaths(javaProject);

        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        for (IPath path : sources) {
            IResource firstSource = workspace.getRoot().findMember(path);
            if (firstSource != null) {
                packageRoot = javaProject.getPackageFragmentRoot(firstSource);
                if (packageRoot != null) {
                    break;
                }
            }
        }
        return packageRoot;
    }

    /**
     * Computes the minimum SDK and target SDK versions for the project
     *
     * @param project the project to look up the versions for
     * @return a pair of (minimum SDK, target SDK) versions, never null
     */
    @NonNull
    public static Pair<Integer, Integer> computeSdkVersions(IProject project) {
        int mMinSdkVersion = 1;
        int mTargetSdkVersion = 1;

        IAbstractFile manifestFile = AndroidManifest.getManifest(new IFolderWrapper(project));
        if (manifestFile != null) {
            try {
                Object value = AndroidManifest.getMinSdkVersion(manifestFile);
                mMinSdkVersion = 1; // Default case if missing
                if (value instanceof Integer) {
                    mMinSdkVersion = ((Integer) value).intValue();
                } else if (value instanceof String) {
                    // handle codename, only if we can resolve it.
                    if (Sdk.getCurrent() != null) {
                        IAndroidTarget target = Sdk.getCurrent().getTargetFromHashString(
                                "android-" + value); //$NON-NLS-1$
                        if (target != null) {
                            // codename future API level is current api + 1
                            mMinSdkVersion = target.getVersion().getApiLevel() + 1;
                        }
                    }
                }

                value = AndroidManifest.getTargetSdkVersion(manifestFile);
                if (value == null) {
                    mTargetSdkVersion = mMinSdkVersion;
                } else if (value instanceof String) {
                    // handle codename, only if we can resolve it.
                    if (Sdk.getCurrent() != null) {
                        IAndroidTarget target = Sdk.getCurrent().getTargetFromHashString(
                                "android-" + value); //$NON-NLS-1$
                        if (target != null) {
                            // codename future API level is current api + 1
                        	mTargetSdkVersion = target.getVersion().getApiLevel() + 1;
                        }
                    }
                }
            } catch (StreamException e) {
                // do nothing we'll use 1 below.
            }
        }

        return Pair.of(mMinSdkVersion, mTargetSdkVersion);
    }
}

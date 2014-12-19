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

package org.eclipse.andmore.internal.sdk;

import com.android.SdkConstants;
import com.android.ide.common.rendering.LayoutLibrary;
import com.android.ide.common.resources.ResourceRepository;
import com.android.sdklib.IAndroidTarget;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.common.resources.platform.AttrsXmlParser;
import org.eclipse.andmore.common.resources.platform.DeclareStyleableInfo;
import org.eclipse.andmore.common.resources.platform.ViewClassInfo;
import org.eclipse.andmore.internal.editors.animator.AnimDescriptors;
import org.eclipse.andmore.internal.editors.animator.AnimatorDescriptors;
import org.eclipse.andmore.internal.editors.color.ColorDescriptors;
import org.eclipse.andmore.internal.editors.drawable.DrawableDescriptors;
import org.eclipse.andmore.internal.editors.layout.descriptors.LayoutDescriptors;
import org.eclipse.andmore.internal.editors.manifest.descriptors.AndroidManifestDescriptors;
import org.eclipse.andmore.internal.editors.menu.descriptors.MenuDescriptors;
import org.eclipse.andmore.internal.editors.otherxml.descriptors.OtherXmlDescriptors;
import org.eclipse.andmore.internal.resources.manager.ResourceManager;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.InvalidAttributeValueException;

/**
 * Parser for the platform data in an SDK.
 * <p/>
 * This gather the following information:
 * <ul>
 * <li>Resource ID from <code>android.R</code></li>
 * <li>The list of permissions values from <code>android.Manifest$permission</code></li>
 * <li></li>
 * </ul>
 */
public final class AndroidTargetParser {

    private static final String TAG = "Framework Resource Parser";
    private final IAndroidTarget mAndroidTarget;

    /**
     * Creates a platform data parser.
     */
    public AndroidTargetParser(IAndroidTarget platformTarget) {
        mAndroidTarget = platformTarget;
    }

    /**
     * Parses the framework, collects all interesting information and stores them in the
     * {@link IAndroidTarget} given to the constructor.
     *
     * @param monitor A progress monitor. Can be null. Caller is responsible for calling done.
     * @return True if the SDK path was valid and parsing has been attempted.
     */
    public IStatus run(IProgressMonitor monitor) {
        try {
            SubMonitor progress = SubMonitor.convert(monitor,
                    String.format("Parsing SDK %1$s", mAndroidTarget.getName()),
                    16);

            AndroidTargetData targetData = new AndroidTargetData(mAndroidTarget);

            // parse the rest of the data.

            AndroidJarLoader classLoader =
                new AndroidJarLoader(mAndroidTarget.getPath(IAndroidTarget.ANDROID_JAR));

            preload(classLoader, progress.newChild(40, SubMonitor.SUPPRESS_NONE));

            if (progress.isCanceled()) {
                return Status.CANCEL_STATUS;
            }

            // get the permissions
            progress.subTask("Permissions");
            String[] permissionValues = collectPermissions(classLoader);
            progress.worked(1);

            if (progress.isCanceled()) {
                return Status.CANCEL_STATUS;
            }

            // get the action and category values for the Intents.
            progress.subTask("Intents");
            ArrayList<String> activity_actions = new ArrayList<String>();
            ArrayList<String> broadcast_actions = new ArrayList<String>();
            ArrayList<String> service_actions = new ArrayList<String>();
            ArrayList<String> categories = new ArrayList<String>();
            collectIntentFilterActionsAndCategories(activity_actions, broadcast_actions,
                    service_actions, categories);
            progress.worked(1);

            if (progress.isCanceled()) {
                return Status.CANCEL_STATUS;
            }

            // gather the attribute definition
            progress.subTask("Attributes definitions");
            AttrsXmlParser attrsXmlParser = new AttrsXmlParser(
                    mAndroidTarget.getPath(IAndroidTarget.ATTRIBUTES),
                    AdtPlugin.getDefault(),
                    1000);
            attrsXmlParser.preload();

            progress.worked(1);

            progress.subTask("Manifest definitions");
            AttrsXmlParser attrsManifestXmlParser = new AttrsXmlParser(
                    mAndroidTarget.getPath(IAndroidTarget.MANIFEST_ATTRIBUTES),
                    attrsXmlParser,
                    AdtPlugin.getDefault(), 1100);
            attrsManifestXmlParser.preload();
            progress.worked(1);

            Collection<ViewClassInfo> mainList = new ArrayList<ViewClassInfo>();
            Collection<ViewClassInfo> groupList = new ArrayList<ViewClassInfo>();

            // collect the layout/widgets classes
            progress.subTask("Widgets and layouts");
            collectLayoutClasses(classLoader, attrsXmlParser, mainList, groupList,
                    progress.newChild(1));

            if (progress.isCanceled()) {
                return Status.CANCEL_STATUS;
            }

            ViewClassInfo[] layoutViewsInfo = mainList.toArray(
                    new ViewClassInfo[mainList.size()]);
            ViewClassInfo[] layoutGroupsInfo = groupList.toArray(
                    new ViewClassInfo[groupList.size()]);
            mainList.clear();
            groupList.clear();

            // collect the preferences classes.
            collectPreferenceClasses(classLoader, attrsXmlParser, mainList, groupList,
                    progress.newChild(1));

            if (progress.isCanceled()) {
                return Status.CANCEL_STATUS;
            }

            ViewClassInfo[] preferencesInfo = mainList.toArray(new ViewClassInfo[mainList.size()]);
            ViewClassInfo[] preferenceGroupsInfo = groupList.toArray(
                    new ViewClassInfo[groupList.size()]);

            Map<String, DeclareStyleableInfo> xmlMenuMap = collectMenuDefinitions(attrsXmlParser);
            Map<String, DeclareStyleableInfo> xmlSearchableMap = collectSearchableDefinitions(
                    attrsXmlParser);
            Map<String, DeclareStyleableInfo> manifestMap = collectManifestDefinitions(
                                                                            attrsManifestXmlParser);
            Map<String, Map<String, Integer>> enumValueMap = attrsXmlParser.getEnumFlagValues();

            Map<String, DeclareStyleableInfo> xmlAppWidgetMap = null;
            if (mAndroidTarget.getVersion().getApiLevel() >= 3) {
                xmlAppWidgetMap = collectAppWidgetDefinitions(attrsXmlParser);
            }

            if (progress.isCanceled()) {
                return Status.CANCEL_STATUS;
            }

            // From the information that was collected, create the pieces that will be put in
            // the PlatformData object.
            AndroidManifestDescriptors manifestDescriptors = new AndroidManifestDescriptors();
            manifestDescriptors.updateDescriptors(manifestMap);
            progress.worked(1);

            if (progress.isCanceled()) {
                return Status.CANCEL_STATUS;
            }

            LayoutDescriptors layoutDescriptors = new LayoutDescriptors();
            layoutDescriptors.updateDescriptors(layoutViewsInfo, layoutGroupsInfo,
                    attrsXmlParser.getDeclareStyleableList(), mAndroidTarget);
            progress.worked(1);

            if (progress.isCanceled()) {
                return Status.CANCEL_STATUS;
            }

            MenuDescriptors menuDescriptors = new MenuDescriptors();
            menuDescriptors.updateDescriptors(xmlMenuMap);
            progress.worked(1);

            if (progress.isCanceled()) {
                return Status.CANCEL_STATUS;
            }

            OtherXmlDescriptors otherXmlDescriptors = new OtherXmlDescriptors();
            otherXmlDescriptors.updateDescriptors(
                    xmlSearchableMap,
                    xmlAppWidgetMap,
                    preferencesInfo,
                    preferenceGroupsInfo);
            progress.worked(1);

            if (progress.isCanceled()) {
                return Status.CANCEL_STATUS;
            }

            DrawableDescriptors drawableDescriptors = new DrawableDescriptors();
            Map<String, DeclareStyleableInfo> map = attrsXmlParser.getDeclareStyleableList();
            drawableDescriptors.updateDescriptors(map);
            progress.worked(1);

            if (progress.isCanceled()) {
                return Status.CANCEL_STATUS;
            }

            AnimatorDescriptors animatorDescriptors = new AnimatorDescriptors();
            animatorDescriptors.updateDescriptors(map);
            progress.worked(1);

            if (progress.isCanceled()) {
                return Status.CANCEL_STATUS;
            }

            AnimDescriptors animDescriptors = new AnimDescriptors();
            animDescriptors.updateDescriptors(map);
            progress.worked(1);

            if (progress.isCanceled()) {
                return Status.CANCEL_STATUS;
            }

            ColorDescriptors colorDescriptors = new ColorDescriptors();
            colorDescriptors.updateDescriptors(map);
            progress.worked(1);

            // load the framework resources.
            ResourceRepository frameworkResources =
                    ResourceManager.getInstance().loadFrameworkResources(mAndroidTarget);
            progress.worked(1);

            // now load the layout lib bridge
            LayoutLibrary layoutBridge =  LayoutLibrary.load(
                    mAndroidTarget.getPath(IAndroidTarget.LAYOUT_LIB),
                    AdtPlugin.getDefault(),
                    "ADT plug-in");

            progress.worked(1);

            // and finally create the PlatformData with all that we loaded.
            targetData.setExtraData(
                    manifestDescriptors,
                    layoutDescriptors,
                    menuDescriptors,
                    otherXmlDescriptors,
                    drawableDescriptors,
                    animatorDescriptors,
                    animDescriptors,
                    colorDescriptors,
                    enumValueMap,
                    permissionValues,
                    activity_actions.toArray(new String[activity_actions.size()]),
                    broadcast_actions.toArray(new String[broadcast_actions.size()]),
                    service_actions.toArray(new String[service_actions.size()]),
                    categories.toArray(new String[categories.size()]),
                    mAndroidTarget.getPlatformLibraries(),
                    mAndroidTarget.getOptionalLibraries(),
                    frameworkResources,
                    layoutBridge);

            targetData.setAttributeMap(attrsXmlParser.getAttributeMap());

            Sdk.getCurrent().setTargetData(mAndroidTarget, targetData);

            return Status.OK_STATUS;
        } catch (Exception e) {
            AdtPlugin.logAndPrintError(e, TAG, "SDK parser failed"); //$NON-NLS-1$
            AdtPlugin.printToConsole("SDK parser failed", e.getMessage());
            return new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID, "SDK parser failed", e);
        }
    }

    /**
     * Preloads all "interesting" classes from the framework SDK jar.
     * <p/>
     * Currently this preloads all classes from the framework jar
     *
     * @param classLoader The framework SDK jar classloader
     * @param monitor A progress monitor. Can be null. Caller is responsible for calling done.
     */
    private void preload(AndroidJarLoader classLoader, IProgressMonitor monitor) {
        try {
            classLoader.preLoadClasses("" /* all classes */,        //$NON-NLS-1$
                    mAndroidTarget.getName(),                       // monitor task label
                    monitor);
        } catch (InvalidAttributeValueException e) {
            AdtPlugin.log(e, "Problem preloading classes"); //$NON-NLS-1$
        } catch (IOException e) {
            AdtPlugin.log(e, "Problem preloading classes"); //$NON-NLS-1$
        }
    }

    /**
     * Loads, collects and returns the list of default permissions from the framework.
     *
     * @param classLoader The framework SDK jar classloader
     * @return a non null (but possibly empty) array containing the permission values.
     */
    private String[] collectPermissions(AndroidJarLoader classLoader) {
        try {
            Class<?> permissionClass =
                classLoader.loadClass(SdkConstants.CLASS_MANIFEST_PERMISSION);

            if (permissionClass != null) {
                ArrayList<String> list = new ArrayList<String>();

                Field[] fields = permissionClass.getFields();

                for (Field f : fields) {
                    int modifiers = f.getModifiers();
                    if (Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers) &&
                            Modifier.isPublic(modifiers)) {
                        try {
                            Object value = f.get(null);
                            if (value instanceof String) {
                                list.add((String)value);
                            }
                        } catch (IllegalArgumentException e) {
                            // since we provide null this should not happen
                        } catch (IllegalAccessException e) {
                            // if the field is inaccessible we ignore it.
                        } catch (NullPointerException npe) {
                            // looks like this is not a static field. we can ignore.
                        } catch (ExceptionInInitializerError  eiie) {
                            // lets just ignore the field again
                        }
                    }
                }

                return list.toArray(new String[list.size()]);
            }
        } catch (ClassNotFoundException e) {
            AdtPlugin.logAndPrintError(e, TAG,
                    "Collect permissions failed, class %1$s not found in %2$s", //$NON-NLS-1$
                    SdkConstants.CLASS_MANIFEST_PERMISSION,
                    mAndroidTarget.getPath(IAndroidTarget.ANDROID_JAR));
        }

        return new String[0];
    }

    /**
     * Loads and collects the action and category default values from the framework.
     * The values are added to the <code>actions</code> and <code>categories</code> lists.
     *
     * @param activityActions the list which will receive the activity action values.
     * @param broadcastActions the list which will receive the broadcast action values.
     * @param serviceActions the list which will receive the service action values.
     * @param categories the list which will receive the category values.
     */
    private void collectIntentFilterActionsAndCategories(ArrayList<String> activityActions,
            ArrayList<String> broadcastActions,
            ArrayList<String> serviceActions, ArrayList<String> categories)  {
        collectValues(mAndroidTarget.getPath(IAndroidTarget.ACTIONS_ACTIVITY),
                activityActions);
        collectValues(mAndroidTarget.getPath(IAndroidTarget.ACTIONS_BROADCAST),
                broadcastActions);
        collectValues(mAndroidTarget.getPath(IAndroidTarget.ACTIONS_SERVICE),
                serviceActions);
        collectValues(mAndroidTarget.getPath(IAndroidTarget.CATEGORIES),
                categories);
    }

    /**
     * Collects values from a text file located in the SDK
     * @param osFilePath The path to the text file.
     * @param values the {@link ArrayList} to fill with the values.
     */
    private void collectValues(String osFilePath, ArrayList<String> values) {
        FileReader fr = null;
        BufferedReader reader = null;
        try {
            fr = new FileReader(osFilePath);
            reader = new BufferedReader(fr);

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.length() > 0 && line.startsWith("#") == false) { //$NON-NLS-1$
                    values.add(line);
                }
            }
        } catch (IOException e) {
            AdtPlugin.log(e, "Failed to read SDK values"); //$NON-NLS-1$
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                AdtPlugin.log(e, "Failed to read SDK values"); //$NON-NLS-1$
            }

            try {
                if (fr != null) {
                    fr.close();
                }
            } catch (IOException e) {
                AdtPlugin.log(e, "Failed to read SDK values"); //$NON-NLS-1$
            }
        }
    }

    /**
     * Collects all layout classes information from the class loader and the
     * attrs.xml and sets the corresponding structures in the resource manager.
     *
     * @param classLoader The framework SDK jar classloader in case we cannot get the widget from
     * the platform directly
     * @param attrsXmlParser The parser of the attrs.xml file
     * @param mainList the Collection to receive the main list of {@link ViewClassInfo}.
     * @param groupList the Collection to receive the group list of {@link ViewClassInfo}.
     * @param monitor A progress monitor. Can be null. Caller is responsible for calling done.
     */
    private void collectLayoutClasses(AndroidJarLoader classLoader,
            AttrsXmlParser attrsXmlParser,
            Collection<ViewClassInfo> mainList,
            Collection<ViewClassInfo> groupList,
            IProgressMonitor monitor) {
        LayoutParamsParser ldp = null;
        try {
            WidgetClassLoader loader = new WidgetClassLoader(
                    mAndroidTarget.getPath(IAndroidTarget.WIDGETS));
            if (loader.parseWidgetList(monitor)) {
                ldp = new LayoutParamsParser(loader, attrsXmlParser);
            }
            // if the parsing failed, we'll use the old loader below.
        } catch (FileNotFoundException e) {
            AdtPlugin.log(e, "Android Framework Parser"); //$NON-NLS-1$
            // the file does not exist, we'll use the old loader below.
        }

        if (ldp == null) {
            ldp = new LayoutParamsParser(classLoader, attrsXmlParser);
        }
        ldp.parseLayoutClasses(monitor);

        List<ViewClassInfo> views = ldp.getViews();
        List<ViewClassInfo> groups = ldp.getGroups();

        if (views != null && groups != null) {
            mainList.addAll(views);
            groupList.addAll(groups);
        }
    }

    /**
     * Collects all preferences definition information from the attrs.xml and
     * sets the corresponding structures in the resource manager.
     *
     * @param classLoader The framework SDK jar classloader
     * @param attrsXmlParser The parser of the attrs.xml file
     * @param mainList the Collection to receive the main list of {@link ViewClassInfo}.
     * @param groupList the Collection to receive the group list of {@link ViewClassInfo}.
     * @param monitor A progress monitor. Can be null. Caller is responsible for calling done.
     */
    private void collectPreferenceClasses(AndroidJarLoader classLoader,
            AttrsXmlParser attrsXmlParser, Collection<ViewClassInfo> mainList,
            Collection<ViewClassInfo> groupList, IProgressMonitor monitor) {
        LayoutParamsParser ldp = new LayoutParamsParser(classLoader, attrsXmlParser);

        try {
            ldp.parsePreferencesClasses(monitor);

            List<ViewClassInfo> prefs = ldp.getViews();
            List<ViewClassInfo> groups = ldp.getGroups();

            if (prefs != null && groups != null) {
                mainList.addAll(prefs);
                groupList.addAll(groups);
            }
        } catch (NoClassDefFoundError e) {
            AdtPlugin.logAndPrintError(e, TAG,
                    "Collect preferences failed, class %1$s not found in %2$s",
                    e.getMessage(),
                    classLoader.getSource());
        } catch (Throwable e) {
            AdtPlugin.log(e, "Android Framework Parser: failed to collect preference classes"); //$NON-NLS-1$
            AdtPlugin.printErrorToConsole("Android Framework Parser",
                    "failed to collect preference classes");
        }
    }

    /**
     * Collects all menu definition information from the attrs.xml and returns it.
     *
     * @param attrsXmlParser The parser of the attrs.xml file
     */
    private Map<String, DeclareStyleableInfo> collectMenuDefinitions(
            AttrsXmlParser attrsXmlParser) {
        Map<String, DeclareStyleableInfo> map = attrsXmlParser.getDeclareStyleableList();
        Map<String, DeclareStyleableInfo> map2 = new HashMap<String, DeclareStyleableInfo>();
        for (String key : new String[] { "Menu",        //$NON-NLS-1$
                                         "MenuItem",        //$NON-NLS-1$
                                         "MenuGroup" }) {   //$NON-NLS-1$
            if (map.containsKey(key)) {
                map2.put(key, map.get(key));
            } else {
                AdtPlugin.log(IStatus.WARNING,
                        "Menu declare-styleable %1$s not found in file %2$s", //$NON-NLS-1$
                        key, attrsXmlParser.getOsAttrsXmlPath());
                AdtPlugin.printErrorToConsole("Android Framework Parser",
                        String.format("Menu declare-styleable %1$s not found in file %2$s", //$NON-NLS-1$
                        key, attrsXmlParser.getOsAttrsXmlPath()));
            }
        }

        return Collections.unmodifiableMap(map2);
    }

    /**
     * Collects all searchable definition information from the attrs.xml and returns it.
     *
     * @param attrsXmlParser The parser of the attrs.xml file
     */
    private Map<String, DeclareStyleableInfo> collectSearchableDefinitions(
            AttrsXmlParser attrsXmlParser) {
        Map<String, DeclareStyleableInfo> map = attrsXmlParser.getDeclareStyleableList();
        Map<String, DeclareStyleableInfo> map2 = new HashMap<String, DeclareStyleableInfo>();
        for (String key : new String[] { "Searchable",              //$NON-NLS-1$
                                         "SearchableActionKey" }) { //$NON-NLS-1$
            if (map.containsKey(key)) {
                map2.put(key, map.get(key));
            } else {
                AdtPlugin.log(IStatus.WARNING,
                        "Searchable declare-styleable %1$s not found in file %2$s", //$NON-NLS-1$
                        key, attrsXmlParser.getOsAttrsXmlPath());
                AdtPlugin.printErrorToConsole("Android Framework Parser",
                        String.format("Searchable declare-styleable %1$s not found in file %2$s", //$NON-NLS-1$
                        key, attrsXmlParser.getOsAttrsXmlPath()));
            }
        }

        return Collections.unmodifiableMap(map2);
    }

    /**
     * Collects all appWidgetProviderInfo definition information from the attrs.xml and returns it.
     *
     * @param attrsXmlParser The parser of the attrs.xml file
     */
    private Map<String, DeclareStyleableInfo> collectAppWidgetDefinitions(
            AttrsXmlParser attrsXmlParser) {
        Map<String, DeclareStyleableInfo> map = attrsXmlParser.getDeclareStyleableList();
        Map<String, DeclareStyleableInfo> map2 = new HashMap<String, DeclareStyleableInfo>();
        for (String key : new String[] { "AppWidgetProviderInfo" }) {  //$NON-NLS-1$
            if (map.containsKey(key)) {
                map2.put(key, map.get(key));
            } else {
                AdtPlugin.log(IStatus.WARNING,
                        "AppWidget declare-styleable %1$s not found in file %2$s", //$NON-NLS-1$
                        key, attrsXmlParser.getOsAttrsXmlPath());
                AdtPlugin.printErrorToConsole("Android Framework Parser",
                        String.format("AppWidget declare-styleable %1$s not found in file %2$s", //$NON-NLS-1$
                        key, attrsXmlParser.getOsAttrsXmlPath()));
            }
        }

        return Collections.unmodifiableMap(map2);
    }

    /**
     * Collects all manifest definition information from the attrs_manifest.xml and returns it.
     */
    private Map<String, DeclareStyleableInfo> collectManifestDefinitions(
            AttrsXmlParser attrsXmlParser) {

        return attrsXmlParser.getDeclareStyleableList();
    }

}

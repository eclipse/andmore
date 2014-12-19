/*
 * Copyright (C) 2012 The Android Open Source Project
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

package org.eclipse.andmore.internal.editors.layout.configuration;

import static com.android.SdkConstants.ANDROID_STYLE_RESOURCE_PREFIX;
import static com.android.SdkConstants.ATTR_NAME;
import static com.android.SdkConstants.ATTR_THEME;
import static com.android.SdkConstants.PREFIX_RESOURCE_REF;
import static com.android.SdkConstants.STYLE_RESOURCE_PREFIX;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.resources.ResourceRepository;
import com.android.ide.common.resources.configuration.DeviceConfigHelper;
import com.android.ide.common.resources.configuration.FolderConfiguration;
import com.android.ide.common.resources.configuration.LanguageQualifier;
import com.android.ide.common.resources.configuration.RegionQualifier;
import com.android.ide.common.resources.configuration.ScreenSizeQualifier;
import com.android.resources.NightMode;
import com.android.resources.ResourceFolderType;
import com.android.resources.ScreenSize;
import com.android.resources.UiMode;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.devices.Device;
import com.android.sdklib.devices.State;
import com.google.common.base.Splitter;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.editors.manifest.ManifestInfo;
import org.eclipse.andmore.internal.editors.manifest.ManifestInfo.ActivityAttributes;
import org.eclipse.andmore.internal.sdk.AndroidTargetData;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.QualifiedName;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.Collection;
import java.util.List;

/** A description of a configuration, used for persistence */
public class ConfigurationDescription {
    private static final String TAG_PREVIEWS = "previews";    //$NON-NLS-1$
    private static final String TAG_PREVIEW = "preview";      //$NON-NLS-1$
    private static final String ATTR_TARGET = "target";       //$NON-NLS-1$
    private static final String ATTR_CONFIG = "config";       //$NON-NLS-1$
    private static final String ATTR_LOCALE = "locale";       //$NON-NLS-1$
    private static final String ATTR_ACTIVITY = "activity";   //$NON-NLS-1$
    private static final String ATTR_DEVICE = "device";       //$NON-NLS-1$
    private static final String ATTR_STATE = "devicestate";   //$NON-NLS-1$
    private static final String ATTR_UIMODE = "ui";           //$NON-NLS-1$
    private static final String ATTR_NIGHTMODE = "night";     //$NON-NLS-1$
    private final static String SEP_LOCALE = "-";             //$NON-NLS-1$

    /**
     * Settings name for file-specific configuration preferences, such as which theme or
     * device to render the current layout with
     */
    public final static QualifiedName NAME_CONFIG_STATE =
        new QualifiedName(AdtPlugin.PLUGIN_ID, "state");//$NON-NLS-1$

    /** The project corresponding to this configuration's description */
    public final IProject project;

    /** The display name */
    public String displayName;

    /** The theme */
    public String theme;

    /** The target */
    public IAndroidTarget target;

    /** The display name */
    public FolderConfiguration folder;

    /** The locale */
    public Locale locale = Locale.ANY;

    /** The device */
    public Device device;

    /** The device state */
    public State state;

    /** The activity */
    public String activity;

    /** UI mode */
    @NonNull
    public UiMode uiMode = UiMode.NORMAL;

    /** Night mode */
    @NonNull
    public NightMode nightMode = NightMode.NOTNIGHT;

    private ConfigurationDescription(@Nullable IProject project) {
        this.project = project;
    }

    /**
     * Returns the persistent configuration description from the given file
     *
     * @param file the file to look up a description from
     * @return the description or null if never written
     */
    @Nullable
    public static String getDescription(@NonNull IFile file) {
        return AdtPlugin.getFileProperty(file, NAME_CONFIG_STATE);
    }

    /**
     * Sets the persistent configuration description data for the given file
     *
     * @param file the file to associate the description with
     * @param description the description
     */
    public static void setDescription(@NonNull IFile file, @NonNull String description) {
        AdtPlugin.setFileProperty(file, NAME_CONFIG_STATE, description);
    }

    /**
     * Creates a description from a given configuration
     *
     * @param project the project for this configuration's description
     * @param configuration the configuration to describe
     * @return a new configuration
     */
    public static ConfigurationDescription fromConfiguration(
            @Nullable IProject project,
            @NonNull Configuration configuration) {
        ConfigurationDescription description = new ConfigurationDescription(project);
        description.displayName = configuration.getDisplayName();
        description.theme = configuration.getTheme();
        description.target = configuration.getTarget();
        description.folder = new FolderConfiguration();
        description.folder.set(configuration.getFullConfig());
        description.locale = configuration.getLocale();
        description.device = configuration.getDevice();
        description.state = configuration.getDeviceState();
        description.activity = configuration.getActivity();
        return description;
    }

    /**
     * Initializes a string previously created with
     * {@link #toXml(Document)}
     *
     * @param project the project for this configuration's description
     * @param element the element to read back from
     * @param deviceList list of available devices
     * @return true if the configuration was initialized
     */
    @Nullable
    public static ConfigurationDescription fromXml(
            @Nullable IProject project,
            @NonNull Element element,
            @NonNull Collection<Device> deviceList) {
        ConfigurationDescription description = new ConfigurationDescription(project);

        if (!TAG_PREVIEW.equals(element.getTagName())) {
            return null;
        }

        String displayName = element.getAttribute(ATTR_NAME);
        if (!displayName.isEmpty()) {
            description.displayName = displayName;
        }

        String config = element.getAttribute(ATTR_CONFIG);
        Iterable<String> segments = Splitter.on('-').split(config);
        description.folder = FolderConfiguration.getConfig(segments);

        String theme = element.getAttribute(ATTR_THEME);
        if (!theme.isEmpty()) {
            description.theme = theme;
        }

        String targetId = element.getAttribute(ATTR_TARGET);
        if (!targetId.isEmpty()) {
            IAndroidTarget target = Configuration.stringToTarget(targetId);
            description.target = target;
        }

        String localeString = element.getAttribute(ATTR_LOCALE);
        if (!localeString.isEmpty()) {
            // Load locale. Note that this can get overwritten by the
            // project-wide settings read below.
            LanguageQualifier language = Locale.ANY_LANGUAGE;
            RegionQualifier region = Locale.ANY_REGION;
            String locales[] = localeString.split(SEP_LOCALE);
            if (locales[0].length() > 0) {
                language = new LanguageQualifier(locales[0]);
            }
            if (locales.length > 1 && locales[1].length() > 0) {
                region = new RegionQualifier(locales[1]);
            }
            description.locale = Locale.create(language, region);
        }

        String activity = element.getAttribute(ATTR_ACTIVITY);
        if (activity.isEmpty()) {
            activity = null;
        }

        String deviceString = element.getAttribute(ATTR_DEVICE);
        if (!deviceString.isEmpty()) {
            for (Device d : deviceList) {
                if (d.getName().equals(deviceString)) {
                    description.device = d;
                    String stateName = element.getAttribute(ATTR_STATE);
                    if (stateName.isEmpty() || stateName.equals("null")) {
                        description.state = Configuration.getState(d, stateName);
                    } else if (d.getAllStates().size() > 0) {
                        description.state = d.getAllStates().get(0);
                    }
                    break;
                }
            }
        }

        String uiModeString = element.getAttribute(ATTR_UIMODE);
        if (!uiModeString.isEmpty()) {
            description.uiMode = UiMode.getEnum(uiModeString);
            if (description.uiMode == null) {
                description.uiMode = UiMode.NORMAL;
            }
        }

        String nightModeString = element.getAttribute(ATTR_NIGHTMODE);
        if (!nightModeString.isEmpty()) {
            description.nightMode = NightMode.getEnum(nightModeString);
            if (description.nightMode == null) {
                description.nightMode = NightMode.NOTNIGHT;
            }
        }


        // Should I really be storing the FULL configuration? Might be trouble if
        // you bring a different device

        return description;
    }

    /**
     * Write this description into the given document as a new element.
     *
     * @param document the document to add the description to
     * @return the newly inserted element
     */
    @NonNull
    public Element toXml(Document document) {
        Element element = document.createElement(TAG_PREVIEW);

        element.setAttribute(ATTR_NAME, displayName);
        FolderConfiguration fullConfig = folder;
        String folderName = fullConfig.getFolderName(ResourceFolderType.LAYOUT);
        element.setAttribute(ATTR_CONFIG, folderName);
        if (theme != null) {
            element.setAttribute(ATTR_THEME, theme);
        }
        if (target != null) {
            element.setAttribute(ATTR_TARGET, Configuration.targetToString(target));
        }

        if (locale != null && (locale.hasLanguage() || locale.hasRegion())) {
            String value;
            if (locale.hasRegion()) {
                value = locale.language.getValue() + SEP_LOCALE + locale.region.getValue();
            } else {
                value = locale.language.getValue();
            }
            element.setAttribute(ATTR_LOCALE, value);
        }

        if (device != null) {
            element.setAttribute(ATTR_DEVICE, device.getName());
            if (state != null) {
                element.setAttribute(ATTR_STATE, state.getName());
            }
        }

        if (activity != null) {
            element.setAttribute(ATTR_ACTIVITY, activity);
        }

        if (uiMode != null && uiMode != UiMode.NORMAL) {
            element.setAttribute(ATTR_UIMODE, uiMode.getResourceValue());
        }

        if (nightMode != null && nightMode != NightMode.NOTNIGHT) {
            element.setAttribute(ATTR_NIGHTMODE, nightMode.getResourceValue());
        }

        Element parent = document.getDocumentElement();
        if (parent == null) {
            parent = document.createElement(TAG_PREVIEWS);
            document.appendChild(parent);
        }
        parent.appendChild(element);

        return element;
    }

    /** Returns the preferred theme, or null */
    @Nullable
    String computePreferredTheme() {
        if (project == null) {
            return "Theme";
        }
        ManifestInfo manifest = ManifestInfo.get(project);

        // Look up the screen size for the current state
        ScreenSize screenSize = null;
        if (device != null) {
            List<State> states = device.getAllStates();
            for (State s : states) {
                FolderConfiguration folderConfig = DeviceConfigHelper.getFolderConfig(s);
                if (folderConfig != null) {
                    ScreenSizeQualifier qualifier = folderConfig.getScreenSizeQualifier();
                    screenSize = qualifier.getValue();
                    break;
                }
            }
        }

        // Look up the default/fallback theme to use for this project (which
        // depends on the screen size when no particular theme is specified
        // in the manifest)
        String defaultTheme = manifest.getDefaultTheme(target, screenSize);

        String preferred = defaultTheme;
        if (theme == null) {
            // If we are rendering a layout in included context, pick the theme
            // from the outer layout instead

            if (activity != null) {
                ActivityAttributes attributes = manifest.getActivityAttributes(activity);
                if (attributes != null) {
                    preferred = attributes.getTheme();
                }
            }
            if (preferred == null) {
                preferred = defaultTheme;
            }
            theme = preferred;
        }

        return preferred;
    }

    private void checkThemePrefix() {
        if (theme != null && !theme.startsWith(PREFIX_RESOURCE_REF)) {
            if (theme.isEmpty()) {
                computePreferredTheme();
                return;
            }

            if (target != null) {
                Sdk sdk = Sdk.getCurrent();
                if (sdk != null) {
                    AndroidTargetData data = sdk.getTargetData(target);

                    if (data != null) {
                        ResourceRepository resources = data.getFrameworkResources();
                        if (resources != null
                            && resources.hasResourceItem(ANDROID_STYLE_RESOURCE_PREFIX + theme)) {
                            theme = ANDROID_STYLE_RESOURCE_PREFIX + theme;
                            return;
                        }
                    }
                }
            }

            theme = STYLE_RESOURCE_PREFIX + theme;
        }
    }
}

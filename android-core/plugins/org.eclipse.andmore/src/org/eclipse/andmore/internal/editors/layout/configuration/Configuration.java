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
import static com.android.SdkConstants.PREFIX_RESOURCE_REF;
import static com.android.SdkConstants.STYLE_RESOURCE_PREFIX;

import java.util.List;

import org.eclipse.andmore.AndmoreAndroidPlugin;
import org.eclipse.andmore.internal.editors.layout.gle2.RenderService;
import org.eclipse.andmore.internal.editors.manifest.ManifestInfo;
import org.eclipse.andmore.internal.editors.manifest.ManifestInfo.ActivityAttributes;
import org.eclipse.andmore.internal.preferences.AdtPrefs;
import org.eclipse.andmore.internal.resources.ResourceHelper;
import org.eclipse.andmore.internal.resources.manager.ProjectResources;
import org.eclipse.andmore.internal.resources.manager.ResourceManager;
import org.eclipse.andmore.internal.sdk.AndroidTargetData;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.rendering.LayoutLibrary;
import com.android.ide.common.rendering.api.Capability;
import com.android.ide.common.resources.ResourceFolder;
import com.android.ide.common.resources.ResourceRepository;
import com.android.ide.common.resources.configuration.DensityQualifier;
import com.android.ide.common.resources.configuration.DeviceConfigHelper;
import com.android.ide.common.resources.configuration.FolderConfiguration;
import com.android.ide.common.resources.configuration.LayoutDirectionQualifier;
import com.android.ide.common.resources.configuration.LocaleQualifier;
import com.android.ide.common.resources.configuration.NightModeQualifier;
import com.android.ide.common.resources.configuration.ScreenSizeQualifier;
import com.android.ide.common.resources.configuration.UiModeQualifier;
import com.android.ide.common.resources.configuration.VersionQualifier;
import com.android.resources.Density;
import com.android.resources.LayoutDirection;
import com.android.resources.NightMode;
import com.android.resources.ScreenSize;
import com.android.resources.UiMode;
import com.android.sdklib.AndroidVersion;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.devices.Device;
import com.android.sdklib.devices.State;
import com.android.utils.Pair;
import com.google.common.base.Objects;

/**
 * A {@linkplain Configuration} is a selection of device, orientation, theme,
 * etc for use when rendering a layout.
 */
public class Configuration {
    /** The {@link FolderConfiguration} in change flags or override flags */
    public static final int CFG_FOLDER       = 1 << 0;
    /** The {@link Device} in change flags or override flags */
    public static final int CFG_DEVICE       = 1 << 1;
    /** The {@link State} in change flags or override flags */
    public static final int CFG_DEVICE_STATE = 1 << 2;
    /** The theme in change flags or override flags */
    public static final int CFG_THEME        = 1 << 3;
    /** The locale in change flags or override flags */
    public static final int CFG_LOCALE       = 1 << 4;
    /** The rendering {@link IAndroidTarget} in change flags or override flags */
    public static final int CFG_TARGET       = 1 << 5;
    /** The {@link NightMode} in change flags or override flags */
    public static final int CFG_NIGHT_MODE   = 1 << 6;
    /** The {@link UiMode} in change flags or override flags */
    public static final int CFG_UI_MODE      = 1 << 7;
    /** The {@link UiMode} in change flags or override flags */
    public static final int CFG_ACTIVITY     = 1 << 8;

    /** References all attributes */
    public static final int MASK_ALL = 0xFFFF;

    /** Attributes which affect which best-layout-file selection */
    public static final int MASK_FILE_ATTRS =
            CFG_DEVICE|CFG_DEVICE_STATE|CFG_LOCALE|CFG_TARGET|CFG_NIGHT_MODE|CFG_UI_MODE;

    /** Attributes which affect rendering appearance */
    public static final int MASK_RENDERING = MASK_FILE_ATTRS|CFG_THEME;

    /**
     * Setting name for project-wide setting controlling rendering target and locale which
     * is shared for all files
     */
    public final static QualifiedName NAME_RENDER_STATE =
        new QualifiedName(AndmoreAndroidPlugin.PLUGIN_ID, "render");          //$NON-NLS-1$

    private final static String MARKER_FRAMEWORK = "-";            //$NON-NLS-1$
    private final static String MARKER_PROJECT = "+";              //$NON-NLS-1$
    private final static String SEP = ":";                         //$NON-NLS-1$
    private final static String SEP_LOCALE = "-";                  //$NON-NLS-1$

    @NonNull
    protected ConfigurationChooser mConfigChooser;

    /** The {@link FolderConfiguration} representing the state of the UI controls */
    @NonNull
    protected final FolderConfiguration mFullConfig = new FolderConfiguration();

    /** The {@link FolderConfiguration} being edited. */
    @Nullable
    protected FolderConfiguration mEditedConfig;

    /** The target of the project of the file being edited. */
    @Nullable
    private IAndroidTarget mTarget;

    /** The theme style to render with */
    @Nullable
    private String mTheme;

    /** The device to render with */
    @Nullable
    private Device mDevice;

    /** The device state */
    @Nullable
    private State mState;

    /**
     * The activity associated with the layout. This is just a cached value of
     * the true value stored on the layout.
     */
    @Nullable
    private String mActivity;

    /** The locale to use for this configuration */
    @NonNull
    private Locale mLocale = Locale.ANY;

    /** UI mode */
    @NonNull
    private UiMode mUiMode = UiMode.NORMAL;

    /** Night mode */
    @NonNull
    private NightMode mNightMode = NightMode.NOTNIGHT;

    /** The display name */
    private String mDisplayName;

    /**
     * Creates a new {@linkplain Configuration}
     *
     * @param chooser the associated chooser
     */
    protected Configuration(@NonNull ConfigurationChooser chooser) {
        mConfigChooser = chooser;
    }

    /**
     * Sets the associated configuration chooser
     *
     * @param chooser the chooser
     */
    void setChooser(@NonNull ConfigurationChooser chooser) {
        // TODO: We should get rid of the binding between configurations
        // and configuration choosers. This is currently needed because
        // the choosers contain vital data such as the set of available
        // rendering targets, the set of available locales etc, which
        // also doesn't belong inside the configuration but is needed by it.
        mConfigChooser = chooser;
    }

    /**
     * Gets the associated configuration chooser
     *
     * @return the chooser
     */
    @NonNull
    ConfigurationChooser getChooser() {
        return mConfigChooser;
    }

    /**
     * Creates a new {@linkplain Configuration}
     *
     * @param chooser the associated chooser
     * @return a new configuration
     */
    @NonNull
    public static Configuration create(@NonNull ConfigurationChooser chooser) {
        return new Configuration(chooser);
    }

    /**
     * Creates a configuration suitable for the given file
     *
     * @param base the base configuration to base the file configuration off of
     * @param file the file to look up a configuration for
     * @return a suitable configuration
     */
    @NonNull
    public static Configuration create(
            @NonNull Configuration base,
            @NonNull IFile file) {
        Configuration configuration = copy(base);
        ConfigurationChooser chooser = base.getChooser();
        ProjectResources resources = chooser.getResources();
        ConfigurationMatcher matcher = new ConfigurationMatcher(chooser, configuration, file,
                resources, false);

        ResourceFolder resFolder = ResourceManager.getInstance().getResourceFolder(file);
        configuration.mEditedConfig = new FolderConfiguration();
        configuration.mEditedConfig.set(resFolder.getConfiguration());

        matcher.adaptConfigSelection(true /*needBestMatch*/);
        configuration.syncFolderConfig();

        return configuration;
    }

    /**
     * Creates a new {@linkplain Configuration} that is a copy from a different configuration
     *
     * @param original the original to copy from
     * @return a new configuration copied from the original
     */
    @NonNull
    public static Configuration copy(@NonNull Configuration original) {
        Configuration copy = create(original.mConfigChooser);
        copy.mFullConfig.set(original.mFullConfig);
        if (original.mEditedConfig != null) {
            copy.mEditedConfig = new FolderConfiguration();
            copy.mEditedConfig.set(original.mEditedConfig);
        }
        copy.mTarget = original.getTarget();
        copy.mTheme = original.getTheme();
        copy.mDevice = original.getDevice();
        copy.mState = original.getDeviceState();
        copy.mActivity = original.getActivity();
        copy.mLocale = original.getLocale();
        copy.mUiMode = original.getUiMode();
        copy.mNightMode = original.getNightMode();
        copy.mDisplayName = original.getDisplayName();

        return copy;
    }

    /**
     * Returns the associated activity
     *
     * @return the activity
     */
    @Nullable
    public String getActivity() {
        return mActivity;
    }

    /**
     * Returns the chosen device.
     *
     * @return the chosen device
     */
    @Nullable
    public Device getDevice() {
        return mDevice;
    }

    /**
     * Returns the chosen device state
     *
     * @return the device state
     */
    @Nullable
    public State getDeviceState() {
        return mState;
    }

    /**
     * Returns the chosen locale
     *
     * @return the locale
     */
    @NonNull
    public Locale getLocale() {
        return mLocale;
    }

    /**
     * Returns the UI mode
     *
     * @return the UI mode
     */
    @NonNull
    public UiMode getUiMode() {
        return mUiMode;
    }

    /**
     * Returns the day/night mode
     *
     * @return the night mode
     */
    @NonNull
    public NightMode getNightMode() {
        return mNightMode;
    }

    /**
     * Returns the current theme style
     *
     * @return the theme style
     */
    @Nullable
    public String getTheme() {
        return mTheme;
    }

    /**
     * Returns the rendering target
     *
     * @return the target
     */
    @Nullable
    public IAndroidTarget getTarget() {
        return mTarget;
    }

    /**
     * Returns the display name to show for this configuration
     *
     * @return the display name, or null if none has been assigned
     */
    @Nullable
    public String getDisplayName() {
        return mDisplayName;
    }

    /**
     * Returns whether the configuration's theme is a project theme.
     * <p/>
     * The returned value is meaningless if {@link #getTheme()} returns
     * <code>null</code>.
     *
     * @return true for project a theme, false for a framework theme
     */
    public boolean isProjectTheme() {
        String theme = getTheme();
        if (theme != null) {
            assert theme.startsWith(STYLE_RESOURCE_PREFIX)
                || theme.startsWith(ANDROID_STYLE_RESOURCE_PREFIX);

            return ResourceHelper.isProjectStyle(theme);
        }

        return false;
    }

    /**
     * Returns true if the current layout is locale-specific
     *
     * @return if this configuration represents a locale-specific layout
     */
    public boolean isLocaleSpecificLayout() {
        return mEditedConfig == null || mEditedConfig.getLocaleQualifier() != null;
    }

    /**
     * Returns the full, complete {@link FolderConfiguration}
     *
     * @return the full configuration
     */
    @NonNull
    public FolderConfiguration getFullConfig() {
        return mFullConfig;
    }

    /**
     * Copies the full, complete {@link FolderConfiguration} into the given
     * folder config instance.
     *
     * @param dest the {@link FolderConfiguration} instance to copy into
     */
    public void copyFullConfig(FolderConfiguration dest) {
        dest.set(mFullConfig);
    }

    /**
     * Returns the edited {@link FolderConfiguration} (this is not a full
     * configuration, so you can think of it as the "constraints" used by the
     * {@link ConfigurationMatcher} to produce a full configuration.
     *
     * @return the constraints configuration
     */
    @NonNull
    public FolderConfiguration getEditedConfig() {
        return mEditedConfig;
    }

    /**
     * Sets the edited {@link FolderConfiguration} (this is not a full
     * configuration, so you can think of it as the "constraints" used by the
     * {@link ConfigurationMatcher} to produce a full configuration.
     *
     * @param editedConfig the constraints configuration
     */
    public void setEditedConfig(@NonNull FolderConfiguration editedConfig) {
        mEditedConfig = editedConfig;
    }

    /**
     * Sets the associated activity
     *
     * @param activity the activity
     */
    public void setActivity(String activity) {
        mActivity = activity;
    }

    /**
     * Sets the device
     *
     * @param device the device
     * @param skipSync if true, don't sync folder configuration (typically because
     *   you are going to set other configuration parameters and you'll call
     *   {@link #syncFolderConfig()} once at the end)
     */
    public void setDevice(Device device, boolean skipSync) {
        mDevice = device;

        if (!skipSync) {
            syncFolderConfig();
        }
    }

    /**
     * Sets the device state
     *
     * @param state the device state
     * @param skipSync if true, don't sync folder configuration (typically because
     *   you are going to set other configuration parameters and you'll call
     *   {@link #syncFolderConfig()} once at the end)
     */
    public void setDeviceState(State state, boolean skipSync) {
        mState = state;

        if (!skipSync) {
            syncFolderConfig();
        }
    }

    /**
     * Sets the locale
     *
     * @param locale the locale
     * @param skipSync if true, don't sync folder configuration (typically because
     *   you are going to set other configuration parameters and you'll call
     *   {@link #syncFolderConfig()} once at the end)
     */
    public void setLocale(@NonNull Locale locale, boolean skipSync) {
        mLocale = locale;

        if (!skipSync) {
            syncFolderConfig();
        }
    }

    /**
     * Sets the rendering target
     *
     * @param target rendering target
     * @param skipSync if true, don't sync folder configuration (typically because
     *   you are going to set other configuration parameters and you'll call
     *   {@link #syncFolderConfig()} once at the end)
     */
    public void setTarget(IAndroidTarget target, boolean skipSync) {
        mTarget = target;

        if (!skipSync) {
            syncFolderConfig();
        }
    }

    /**
     * Sets the display name to be shown for this configuration.
     *
     * @param displayName the new display name
     */
    public void setDisplayName(@Nullable String displayName) {
        mDisplayName = displayName;
    }

    /**
     * Sets the night mode
     *
     * @param night the night mode
     * @param skipSync if true, don't sync folder configuration (typically because
     *   you are going to set other configuration parameters and you'll call
     *   {@link #syncFolderConfig()} once at the end)
     */
    public void setNightMode(@NonNull NightMode night, boolean skipSync) {
        mNightMode = night;

        if (!skipSync) {
            syncFolderConfig();
        }
    }

    /**
     * Sets the UI mode
     *
     * @param uiMode the UI mode
     * @param skipSync if true, don't sync folder configuration (typically because
     *   you are going to set other configuration parameters and you'll call
     *   {@link #syncFolderConfig()} once at the end)
     */
    public void setUiMode(@NonNull UiMode uiMode, boolean skipSync) {
        mUiMode = uiMode;

        if (!skipSync) {
            syncFolderConfig();
        }
    }

    /**
     * Sets the theme style
     *
     * @param theme the theme
     */
    public void setTheme(String theme) {
        mTheme = theme;
        checkThemePrefix();
    }

    /**
     * Updates the folder configuration such that it reflects changes in
     * configuration state such as the device orientation, the UI mode, the
     * rendering target, etc.
     */
    public void syncFolderConfig() {
        Device device = getDevice();
        if (device == null) {
            return;
        }

        // get the device config from the device/state combos.
        FolderConfiguration config = DeviceConfigHelper.getFolderConfig(getDeviceState());

        // replace the config with the one from the device
        mFullConfig.set(config);

        // sync the selected locale
        Locale locale = getLocale();
        mFullConfig.setLocaleQualifier(locale.locale);

        if (!locale.hasLanguage()) {
            // Avoid getting the layout library if the locale doesn't have any language.
            mFullConfig.setLayoutDirectionQualifier(
                    new LayoutDirectionQualifier(LayoutDirection.LTR));
        } else {
            Sdk currentSdk = Sdk.getCurrent();
            if (currentSdk != null) {
                AndroidTargetData targetData = currentSdk.getTargetData(getTarget());
                if (targetData != null) {
                    LayoutLibrary layoutLib = targetData.getLayoutLibrary();
                    if (layoutLib != null) {
                        if (layoutLib.isRtl(locale.toLocaleId())) {
                            mFullConfig.setLayoutDirectionQualifier(
                                    new LayoutDirectionQualifier(LayoutDirection.RTL));
                        } else {
                            mFullConfig.setLayoutDirectionQualifier(
                                    new LayoutDirectionQualifier(LayoutDirection.LTR));
                        }
                    }
                }
            }
        }

        // Replace the UiMode with the selected one, if one is selected
        UiMode uiMode = getUiMode();
        if (uiMode != null) {
            mFullConfig.setUiModeQualifier(new UiModeQualifier(uiMode));
        }

        // Replace the NightMode with the selected one, if one is selected
        NightMode nightMode = getNightMode();
        if (nightMode != null) {
            mFullConfig.setNightModeQualifier(new NightModeQualifier(nightMode));
        }

        // replace the API level by the selection of the combo
        IAndroidTarget target = getTarget();
        if (target == null && mConfigChooser != null) {
            target = mConfigChooser.getProjectTarget();
        }
        if (target != null) {
            int apiLevel = target.getVersion().getApiLevel();
            mFullConfig.setVersionQualifier(new VersionQualifier(apiLevel));
        }
    }

    /**
     * Creates a string suitable for persistence, which can be initialized back
     * to a configuration via {@link #initialize(String)}
     *
     * @return a persistent string
     */
    @NonNull
    public String toPersistentString() {
        StringBuilder sb = new StringBuilder(32);
        Device device = getDevice();
        if (device != null) {
            sb.append(device.getId());
            sb.append(SEP);
            State state = getDeviceState();
            if (state != null) {
                sb.append(state.getName());
            }
            sb.append(SEP);
            Locale locale = getLocale();
            if (isLocaleSpecificLayout() && locale != null) {
                // locale[0]/[1] can be null sometimes when starting Eclipse
            	sb.append(locale.locale.getValue());
            }
            sb.append(SEP);
            // Need to escape the theme: if we write the full theme style, then
            // we can end up with ":"'s in the string (as in @android:style/Theme) which
            // can be mistaken for {@link #SEP}. Instead use {@link #MARKER_FRAMEWORK}.
            String theme = getTheme();
            if (theme != null) {
                String themeName = ResourceHelper.styleToTheme(theme);
                if (theme.startsWith(STYLE_RESOURCE_PREFIX)) {
                    sb.append(MARKER_PROJECT);
                } else if (theme.startsWith(ANDROID_STYLE_RESOURCE_PREFIX)) {
                    sb.append(MARKER_FRAMEWORK);
                }
                sb.append(themeName);
            }
            sb.append(SEP);
            UiMode uiMode = getUiMode();
            if (uiMode != null) {
                sb.append(uiMode.getResourceValue());
            }
            sb.append(SEP);
            NightMode nightMode = getNightMode();
            if (nightMode != null) {
                sb.append(nightMode.getResourceValue());
            }
            sb.append(SEP);

            // We used to store the render target here in R9. Leave a marker
            // to ensure that we don't reuse this slot; add new extra fields after it.
            sb.append(SEP);
            String activity = getActivity();
            if (activity != null) {
                sb.append(activity);
            }
        }

        return sb.toString();
    }

    /** Returns the preferred theme, or null */
    @Nullable
    String computePreferredTheme() {
        IProject project = mConfigChooser.getProject();
        ManifestInfo manifest = ManifestInfo.get(project);

        // Look up the screen size for the current state
        ScreenSize screenSize = null;
        Device device = getDevice();
        if (device != null) {
            List<State> states = device.getAllStates();
            for (State state : states) {
                FolderConfiguration folderConfig = DeviceConfigHelper.getFolderConfig(state);
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
        String defaultTheme = manifest.getDefaultTheme(getTarget(), screenSize);

        String preferred = defaultTheme;
        if (getTheme() == null) {
            // If we are rendering a layout in included context, pick the theme
            // from the outer layout instead

            String activity = getActivity();
            if (activity != null) {
                ActivityAttributes attributes = manifest.getActivityAttributes(activity);
                if (attributes != null) {
                    preferred = attributes.getTheme();
                }
            }
            if (preferred == null) {
                preferred = defaultTheme;
            }
            setTheme(preferred);
        }

        return preferred;
    }

    private void checkThemePrefix() {
        if (mTheme != null && !mTheme.startsWith(PREFIX_RESOURCE_REF)) {
            if (mTheme.isEmpty()) {
                computePreferredTheme();
                return;
            }
            ResourceRepository frameworkRes = mConfigChooser.getClient().getFrameworkResources();
            if (frameworkRes != null
                    && frameworkRes.hasResourceItem(ANDROID_STYLE_RESOURCE_PREFIX + mTheme)) {
                mTheme = ANDROID_STYLE_RESOURCE_PREFIX + mTheme;
            } else {
                mTheme = STYLE_RESOURCE_PREFIX + mTheme;
            }
        }
    }

    /**
     * Initializes a string previously created with
     * {@link #toPersistentString()}
     *
     * @param data the string to initialize back from
     * @return true if the configuration was initialized
     */
    boolean initialize(String data) {
        String[] values = data.split(SEP);
        if (values.length >= 6 && values.length <= 8) {
            for (Device d : mConfigChooser.getDevices()) {
                if (d.getId().equals(values[0]) || d.getDisplayName().equals(values[0])) {
                    mDevice = d;
                    String stateName = null;
                    FolderConfiguration config = null;
                    if (!values[1].isEmpty() && !values[1].equals("null")) { //$NON-NLS-1$
                        stateName = values[1];
                        config = DeviceConfigHelper.getFolderConfig(mDevice, stateName);
                    } else if (mDevice.getAllStates().size() > 0) {
                        State first = mDevice.getAllStates().get(0);
                        stateName = first.getName();
                        config = DeviceConfigHelper.getFolderConfig(first);
                    }
                    mState = getState(mDevice, stateName);
                    if (config != null) {
                        // Load locale. Note that this can get overwritten by the
                        // project-wide settings read below.
                    	String locales[] = values[2].split(SEP_LOCALE);
                    	if (locales[0].length() > 0) {
                    		String language = locales[0];
                    		String region = null;
                    		if (locales.length > 1 && locales[1].length() > 0) {
                    			region = locales[1];
                    		}
                    		mLocale = Locale.create(new LocaleQualifier(null, language, region, null));
                    	}
                    	else {
                    		mLocale = Locale.ANY;
                    	}

                        // Decode the theme name: See {@link #getData}
                        mTheme = values[3];
                        if (mTheme.startsWith(MARKER_FRAMEWORK)) {
                            mTheme = ANDROID_STYLE_RESOURCE_PREFIX
                                    + mTheme.substring(MARKER_FRAMEWORK.length());
                        } else if (mTheme.startsWith(MARKER_PROJECT)) {
                            mTheme = STYLE_RESOURCE_PREFIX
                                    + mTheme.substring(MARKER_PROJECT.length());
                        } else {
                            checkThemePrefix();
                        }

                        mUiMode = UiMode.getEnum(values[4]);
                        if (mUiMode == null) {
                            mUiMode = UiMode.NORMAL;
                        }
                        mNightMode = NightMode.getEnum(values[5]);
                        if (mNightMode == null) {
                            mNightMode = NightMode.NOTNIGHT;
                        }

                        // element 7/values[6]: used to store render target in R9.
                        // No longer stored here. If adding more data, make
                        // sure you leave 7 alone.

                        Pair<Locale, IAndroidTarget> pair = loadRenderState(mConfigChooser);
                        if (pair != null) {
                            // We only use the "global" setting
                            if (!isLocaleSpecificLayout()) {
                                mLocale = pair.getFirst();
                            }
                            mTarget = pair.getSecond();
                        }

                        if (values.length == 8) {
                            mActivity = values[7];
                        }

                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Loads the render state (the locale and the render target, which are shared among
     * all the layouts meaning that changing it in one will change it in all) and returns
     * the current project-wide locale and render target to be used.
     *
     * @param chooser the {@link ConfigurationChooser} providing information about
     *     loaded targets
     * @return a pair of a locale and a render target
     */
    @Nullable
    static Pair<Locale, IAndroidTarget> loadRenderState(ConfigurationChooser chooser) {
        IProject project = chooser.getProject();
        if (project == null || !project.isAccessible()) {
            return null;
        }

        try {
            String data = project.getPersistentProperty(NAME_RENDER_STATE);
            if (data != null) {
                Locale locale = Locale.ANY;
                IAndroidTarget target = null;

                String[] values = data.split(SEP);
                if (values.length == 2) {
                	String locales[] = values[0].split(SEP_LOCALE);
                    if (locales[0].length() != 0) {
                    	String language = locales[0];
                    	String region = null;
                    	if (locales.length > 1 && locales[1].length() != 0) {
                    		region = locales[1];
                    	}
                    	locale = Locale.create(new LocaleQualifier(null, language, region, null));
                    }
                	if (values[0].length() > 0) {
                		locale = Locale.create(new LocaleQualifier(values[0]));
                	}

                    if (AdtPrefs.getPrefs().isAutoPickRenderTarget()) {
                        target = ConfigurationMatcher.findDefaultRenderTarget(chooser);
                    } else {
                        String targetString = values[1];
                        target = stringToTarget(chooser, targetString);
                        // See if we should "correct" the rendering target to a
                        // better version. If you're using a pre-release version
                        // of the render target, and a final release is
                        // available and installed, we should switch to that
                        // one instead.
                        if (target != null) {
                            AndroidVersion version = target.getVersion();
                            List<IAndroidTarget> targetList = chooser.getTargetList();
                            if (version.getCodename() != null && targetList != null) {
                                int targetApiLevel = version.getApiLevel() + 1;
                                for (IAndroidTarget t : targetList) {
                                    if (t.getVersion().getApiLevel() == targetApiLevel
                                            && t.isPlatform()) {
                                        target = t;
                                        break;
                                    }
                                }
                            }
                        } else {
                            target = ConfigurationMatcher.findDefaultRenderTarget(chooser);
                        }
                    }
                }

                return Pair.of(locale, target);
            }

            return Pair.of(Locale.ANY, ConfigurationMatcher.findDefaultRenderTarget(chooser));
        } catch (CoreException e) {
            AndmoreAndroidPlugin.log(e, null);
        }

        return null;
    }

    /**
     * Saves the render state (the current locale and render target settings) into the
     * project wide settings storage
     */
    void saveRenderState() {
        IProject project = mConfigChooser.getProject();
        if (project == null) {
            return;
        }
        try {
            // Generate a persistent string from locale+target
            StringBuilder sb = new StringBuilder(32);
            Locale locale = getLocale();
            if (locale != null) {
                // locale[0]/[1] can be null sometimes when starting Eclipse
            	sb.append(locale.locale.getValue());
            }
            sb.append(SEP);
            IAndroidTarget target = getTarget();
            if (target != null) {
                sb.append(targetToString(target));
                sb.append(SEP);
            }

            project.setPersistentProperty(NAME_RENDER_STATE, sb.toString());
        } catch (CoreException e) {
            AndmoreAndroidPlugin.log(e, null);
        }
    }

    /**
     * Returns a String id to represent an {@link IAndroidTarget} which can be translated
     * back to an {@link IAndroidTarget} by the matching {@link #stringToTarget}. The id
     * will never contain the {@link #SEP} character.
     *
     * @param target the target to return an id for
     * @return an id for the given target; never null
     */
    @NonNull
    public static String targetToString(@NonNull IAndroidTarget target) {
        return target.getFullName().replace(SEP, "");  //$NON-NLS-1$
    }

    /**
     * Returns an {@link IAndroidTarget} that corresponds to the given id that was
     * originally returned by {@link #targetToString}. May be null, if the platform is no
     * longer available, or if the platform list has not yet been initialized.
     *
     * @param chooser the {@link ConfigurationChooser} providing information about
     *     loaded targets
     * @param id the id that corresponds to the desired platform
     * @return an {@link IAndroidTarget} that matches the given id, or null
     */
    @Nullable
    public static IAndroidTarget stringToTarget(
            @NonNull ConfigurationChooser chooser,
            @NonNull String id) {
        List<IAndroidTarget> targetList = chooser.getTargetList();
        if (targetList != null && targetList.size() > 0) {
            for (IAndroidTarget target : targetList) {
                if (id.equals(targetToString(target))) {
                    return target;
                }
            }
        }

        return null;
    }

    /**
     * Returns an {@link IAndroidTarget} that corresponds to the given id that was
     * originally returned by {@link #targetToString}. May be null, if the platform is no
     * longer available, or if the platform list has not yet been initialized.
     *
     * @param id the id that corresponds to the desired platform
     * @return an {@link IAndroidTarget} that matches the given id, or null
     */
    @Nullable
    public static IAndroidTarget stringToTarget(
            @NonNull String id) {
        Sdk currentSdk = Sdk.getCurrent();
        if (currentSdk != null) {
            for (IAndroidTarget target : currentSdk.getTargets()) {
                if (id.equals(targetToString(target))) {
                    return target;
                }
            }
        }

        return null;
    }

    /**
     * Returns the {@link State} by the given name for the given {@link Device}
     *
     * @param device the device
     * @param name the name of the state
     */
    @Nullable
    static State getState(@Nullable Device device, @Nullable String name) {
        if (device == null) {
            return null;
        } else if (name != null) {
            State state = device.getState(name);
            if (state != null) {
                return state;
            }
        }

        return device.getDefaultState();
    }

    /**
     * Returns the currently selected {@link Density}. This is guaranteed to be non null.
     *
     * @return the density
     */
    @NonNull
    public Density getDensity() {
        if (mFullConfig != null) {
            DensityQualifier qual = mFullConfig.getDensityQualifier();
            if (qual != null) {
                // just a sanity check
                Density d = qual.getValue();
                if (d != Density.NODPI) {
                    return d;
                }
            }
        }

        // no config? return medium as the default density.
        return Density.MEDIUM;
    }

    /**
     * Get the next cyclical state after the given state
     *
     * @param from the state to start with
     * @return the following state following
     */
    @Nullable
    public State getNextDeviceState(@Nullable State from) {
        Device device = getDevice();
        if (device == null) {
            return null;
        }
        List<State> states = device.getAllStates();
        for (int i = 0; i < states.size(); i++) {
            if (states.get(i) == from) {
                return states.get((i + 1) % states.size());
            }
        }

        return null;
    }

    /**
     * Returns true if this configuration supports the given rendering
     * capability
     *
     * @param capability the capability to check
     * @return true if the capability is supported
     */
    public boolean supports(Capability capability) {
        IAndroidTarget target = getTarget();
        if (target != null) {
            return RenderService.supports(target, capability);
        }

        return false;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this.getClass())
                .add("display", getDisplayName())                 //$NON-NLS-1$
                .add("persistent", toPersistentString())          //$NON-NLS-1$
                .toString();
    }
}

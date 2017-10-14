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

import static com.android.SdkConstants.ANDROID_NS_NAME_PREFIX;
import static com.android.SdkConstants.ANDROID_STYLE_RESOURCE_PREFIX;
import static com.android.SdkConstants.ATTR_CONTEXT;
import static com.android.SdkConstants.PREFIX_RESOURCE_REF;
import static com.android.SdkConstants.RES_QUALIFIER_SEP;
import static com.android.SdkConstants.STYLE_RESOURCE_PREFIX;
import static com.android.SdkConstants.TOOLS_URI;
import static com.google.common.base.Objects.equal;
import static org.eclipse.andmore.AdtUtils.isUiThread;
import static org.eclipse.andmore.internal.editors.layout.configuration.Configuration.CFG_DEVICE;
import static org.eclipse.andmore.internal.editors.layout.configuration.Configuration.CFG_DEVICE_STATE;
import static org.eclipse.andmore.internal.editors.layout.configuration.Configuration.CFG_FOLDER;
import static org.eclipse.andmore.internal.editors.layout.configuration.Configuration.CFG_LOCALE;
import static org.eclipse.andmore.internal.editors.layout.configuration.Configuration.CFG_TARGET;
import static org.eclipse.andmore.internal.editors.layout.configuration.Configuration.CFG_THEME;
import static org.eclipse.andmore.internal.editors.layout.configuration.Configuration.MASK_ALL;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import org.eclipse.andmore.AdtUtils;
import org.eclipse.andmore.AndmoreAndroidPlugin;
import org.eclipse.andmore.internal.editors.IconFactory;
import org.eclipse.andmore.internal.editors.common.CommonXmlDelegate;
import org.eclipse.andmore.internal.editors.common.CommonXmlEditor;
import org.eclipse.andmore.internal.editors.layout.LayoutEditorDelegate;
import org.eclipse.andmore.internal.editors.layout.gle2.DomUtilities;
import org.eclipse.andmore.internal.editors.layout.gle2.GraphicalEditorPart;
import org.eclipse.andmore.internal.editors.layout.gle2.IncludeFinder.Reference;
import org.eclipse.andmore.internal.editors.layout.gle2.LayoutCanvas;
import org.eclipse.andmore.internal.editors.manifest.ManifestInfo;
import org.eclipse.andmore.internal.editors.manifest.ManifestInfo.ActivityAttributes;
import org.eclipse.andmore.internal.resources.ResourceHelper;
import org.eclipse.andmore.internal.resources.manager.ProjectResources;
import org.eclipse.andmore.internal.resources.manager.ResourceManager;
import org.eclipse.andmore.internal.sdk.AndroidTargetData;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IEditorPart;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.rendering.api.ResourceValue;
import com.android.ide.common.rendering.api.StyleResourceValue;
import com.android.ide.common.resources.LocaleManager;
import com.android.ide.common.resources.ResourceFile;
import com.android.ide.common.resources.ResourceFolder;
import com.android.ide.common.resources.ResourceRepository;
import com.android.ide.common.resources.configuration.DeviceConfigHelper;
import com.android.ide.common.resources.configuration.FolderConfiguration;
import com.android.ide.common.resources.configuration.LocaleQualifier;
import com.android.ide.common.resources.configuration.ResourceQualifier;
import com.android.ide.common.sdk.LoadStatus;
import com.android.resources.ResourceType;
import com.android.resources.ScreenOrientation;
import com.android.sdklib.AndroidVersion;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.devices.Device;
import com.android.sdklib.devices.DeviceManager;
import com.android.sdklib.devices.DeviceManager.DevicesChangedListener;
import com.android.sdklib.devices.State;
import com.android.utils.Pair;
import com.google.common.base.Objects;
import com.google.common.base.Strings;

/**
 * The {@linkplain ConfigurationChooser} allows the user to pick a
 * {@link Configuration} by configuring various constraints.
 */
public class ConfigurationChooser extends Composite
        implements DevicesChangedListener, DisposeListener {
    private static final String ICON_SQUARE = "square";           //$NON-NLS-1$
    private static final String ICON_LANDSCAPE = "landscape";     //$NON-NLS-1$
    private static final String ICON_PORTRAIT = "portrait";       //$NON-NLS-1$
    private static final String ICON_LANDSCAPE_FLIP = "flip_landscape";//$NON-NLS-1$
    private static final String ICON_PORTRAIT_FLIP = "flip_portrait";//$NON-NLS-1$
    private static final String ICON_DISPLAY = "display";         //$NON-NLS-1$
    private static final String ICON_THEMES = "themes";           //$NON-NLS-1$
    private static final String ICON_ACTIVITY = "activity";       //$NON-NLS-1$

    /** The configuration state associated with this editor */
    private @NonNull Configuration mConfiguration = Configuration.create(this);

    /** Serialized state to use when initializing the configuration after the SDK is loaded */
    private String mInitialState;

    /** The client of the configuration editor */
    private final ConfigurationClient mClient;

    /** Counter for programmatic UI changes: if greater than 0, we're within a call */
    private int mDisableUpdates = 0;

    /** List of available devices */
    private Collection<Device> mDevices = Collections.emptyList();

    /** List of available targets */
    private final List<IAndroidTarget> mTargetList = new ArrayList<IAndroidTarget>();

    /** List of available themes */
    private final List<String> mThemeList = new ArrayList<String>();

    /** List of available locales */
    private final List<Locale > mLocaleList = new ArrayList<Locale>();

    /** The file being edited */
    private IFile mEditedFile;

    /** The {@link ProjectResources} for the edited file's project */
    private ProjectResources mResources;

    /** The target of the project of the file being edited. */
    private IAndroidTarget mProjectTarget;

    /** Dropdown for configurations */
    private ToolItem mConfigCombo;

    /** Dropdown for devices */
    private ToolItem mDeviceCombo;

    /** Dropdown for device states */
    private ToolItem mOrientationCombo;

    /** Dropdown for themes */
    private ToolItem mThemeCombo;

    /** Dropdown for locales */
    private ToolItem mLocaleCombo;

    /** Dropdown for activities */
    private ToolItem mActivityCombo;

    /** Dropdown for rendering targets */
    private ToolItem mTargetCombo;

    /** Whether the SDK has changed since the last model reload; if so we must reload targets */
    private boolean mSdkChanged = true;

    /**
     * Creates a new {@linkplain ConfigurationChooser} and adds it to the
     * parent. The method also receives custom buttons to set into the
     * configuration composite. The list is organized as an array of arrays.
     * Each array represents a group of buttons thematically grouped together.
     *
     * @param client the client embedding this configuration chooser
     * @param parent The parent composite.
     * @param initialState The initial state (serialized form) to use for the
     *            configuration
     */
    public ConfigurationChooser(
            @NonNull ConfigurationClient client,
            Composite parent,
            @Nullable String initialState) {
        super(parent, SWT.NONE);
        mClient = client;

        setVisible(false); // Delayed until the targets are loaded

        mInitialState = initialState;
        setLayout(new GridLayout(1, false));

        IconFactory icons = IconFactory.getInstance();

        // TODO: Consider switching to a CoolBar instead
        ToolBar toolBar = new ToolBar(this, SWT.WRAP | SWT.FLAT | SWT.RIGHT | SWT.HORIZONTAL);
        toolBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        mConfigCombo = new ToolItem(toolBar, SWT.DROP_DOWN );
        mConfigCombo.setImage(icons.getIcon("android_file")); //$NON-NLS-1$
        mConfigCombo.setToolTipText("Configuration to render this layout with in Eclipse");

        @SuppressWarnings("unused")
        ToolItem separator2 = new ToolItem(toolBar, SWT.SEPARATOR);

        mDeviceCombo = new ToolItem(toolBar, SWT.DROP_DOWN);
        mDeviceCombo.setImage(icons.getIcon(ICON_DISPLAY));

        @SuppressWarnings("unused")
        ToolItem separator3 = new ToolItem(toolBar, SWT.SEPARATOR);

        mOrientationCombo = new ToolItem(toolBar, SWT.DROP_DOWN);
        mOrientationCombo.setImage(icons.getIcon(ICON_PORTRAIT));
        mOrientationCombo.setToolTipText("Go to next state");

        @SuppressWarnings("unused")
        ToolItem separator4 = new ToolItem(toolBar, SWT.SEPARATOR);

        mThemeCombo = new ToolItem(toolBar, SWT.DROP_DOWN);
        mThemeCombo.setImage(icons.getIcon(ICON_THEMES));

        @SuppressWarnings("unused")
        ToolItem separator5 = new ToolItem(toolBar, SWT.SEPARATOR);

        mActivityCombo = new ToolItem(toolBar, SWT.DROP_DOWN);
        mActivityCombo.setToolTipText("Associated activity or fragment providing context");
        // The JDT class icon is lopsided, presumably because they've left room in the
        // bottom right corner for badges (for static, final etc). Unfortunately, this
        // means that the icon looks out of place when sitting close to the language globe
        // icon, the theme icon, etc so that it looks vertically misaligned:
        //mActivityCombo.setImage(JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_CLASS));
        // ...so use one that is centered instead:
        mActivityCombo.setImage(icons.getIcon(ICON_ACTIVITY));

        @SuppressWarnings("unused")
        ToolItem separator6 = new ToolItem(toolBar, SWT.SEPARATOR);

        //ToolBar rightToolBar = new ToolBar(this, SWT.WRAP | SWT.FLAT | SWT.RIGHT | SWT.HORIZONTAL);
        //rightToolBar.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));
        ToolBar rightToolBar = toolBar;

        mLocaleCombo = new ToolItem(rightToolBar, SWT.DROP_DOWN);
        mLocaleCombo.setImage(FlagManager.getGlobeIcon());
        mLocaleCombo.setToolTipText("Locale to use when rendering layouts in Eclipse");

        @SuppressWarnings("unused")
        ToolItem separator7 = new ToolItem(rightToolBar, SWT.SEPARATOR);

        mTargetCombo = new ToolItem(rightToolBar, SWT.DROP_DOWN);
        mTargetCombo.setImage(AndmoreAndroidPlugin.getAndroidLogo());
        mTargetCombo.setToolTipText("Android version to use when rendering layouts in Eclipse");

        SelectionListener listener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                Object source = e.getSource();

                if (source == mConfigCombo) {
                    ConfigurationMenuListener.show(ConfigurationChooser.this, mConfigCombo);
                } else if (source == mActivityCombo) {
                    ActivityMenuListener.show(ConfigurationChooser.this, mActivityCombo);
                } else if (source == mLocaleCombo) {
                    LocaleMenuListener.show(ConfigurationChooser.this, mLocaleCombo);
                } else if (source == mDeviceCombo) {
                    DeviceMenuListener.show(ConfigurationChooser.this, mDeviceCombo);
                } else if (source == mTargetCombo) {
                    TargetMenuListener.show(ConfigurationChooser.this, mTargetCombo);
                } else if (source == mThemeCombo) {
                    ThemeMenuAction.showThemeMenu(ConfigurationChooser.this, mThemeCombo,
                            mThemeList);
                } else if (source == mOrientationCombo) {
                    if (e.detail == SWT.ARROW) {
                        OrientationMenuAction.showMenu(ConfigurationChooser.this,
                                mOrientationCombo);
                    } else {
                        gotoNextState();
                    }
                }
            }
        };
        mConfigCombo.addSelectionListener(listener);
        mActivityCombo.addSelectionListener(listener);
        mLocaleCombo.addSelectionListener(listener);
        mDeviceCombo.addSelectionListener(listener);
        mTargetCombo.addSelectionListener(listener);
        mThemeCombo.addSelectionListener(listener);
        mOrientationCombo.addSelectionListener(listener);

        addDisposeListener(this);

        initDevices();
        initTargets();
    }

    /**
     * Returns the edited file
     *
     * @return the file
     */
    @Nullable
    public IFile getEditedFile() {
        return mEditedFile;
    }

    /**
     * Returns the project of the edited file
     *
     * @return the project
     */
    @Nullable
    public IProject getProject() {
        if (mEditedFile != null) {
            return mEditedFile.getProject();
        } else {
            return null;
        }
    }

    ConfigurationClient getClient() {
        return mClient;
    }

    /**
     * Returns the project resources for the project being configured by this
     * chooser
     *
     * @return the project resources
     */
    @Nullable
    public ProjectResources getResources() {
        return mResources;
    }

    /**
     * Returns the full, complete {@link FolderConfiguration}
     *
     * @return the full configuration
     */
    public FolderConfiguration getFullConfiguration() {
        return mConfiguration.getFullConfig();
    }

    /**
     * Returns the project target
     *
     * @return the project target
     */
    public IAndroidTarget getProjectTarget() {
        return mProjectTarget;
    }

    /**
     * Returns the configuration being edited by this {@linkplain ConfigurationChooser}
     *
     * @return the configuration
     */
    public Configuration getConfiguration() {
        return mConfiguration;
    }

    /**
     * Returns the list of locales
     * @return a list of {@link ResourceQualifier} pairs
     */
    @NonNull
    public List<Locale> getLocaleList() {
        return mLocaleList;
    }

    /**
     * Returns the list of available devices
     *
     * @return a list of {@link Device} objects
     */
    @NonNull
    public Collection<Device> getDevices() {
        return mDevices;
    }

    /**
     * Returns the list of available render targets
     *
     * @return a list of {@link IAndroidTarget} objects
     */
    @NonNull
    public List<IAndroidTarget> getTargetList() {
        return mTargetList;
    }

    // ---- Configuration State Lookup ----

    /**
     * Returns the rendering target to be used
     *
     * @return the target
     */
    @NonNull
    public IAndroidTarget getTarget() {
        IAndroidTarget target = mConfiguration.getTarget();
        if (target == null) {
            target = mProjectTarget;
        }

        return target;
    }

    /**
     * Returns the current device string, or null if no device is selected
     *
     * @return the device name, or null
     */
    @Nullable
    public String getDeviceName() {
        Device device = mConfiguration.getDevice();
        if (device != null) {
            return device.getName();
        }

        return null;
    }

    /**
     * Returns the current theme, or null if none has been selected
     *
     * @return the theme name, or null
     */
    @Nullable
    public String getThemeName() {
        String theme = mConfiguration.getTheme();
        if (theme != null) {
            theme = ResourceHelper.styleToTheme(theme);
        }

        return theme;
    }

    /** Move to the next device state, changing the icon if it changes orientation */
    private void gotoNextState() {
        State state = mConfiguration.getDeviceState();
        State flipped = mConfiguration.getNextDeviceState(state);
        if (flipped != state) {
            selectDeviceState(flipped);
            onDeviceConfigChange();
        }
    }

    // ---- Implements DisposeListener ----

    @Override
    public void widgetDisposed(DisposeEvent e) {
        dispose();
    }

    @Override
    public void dispose() {
        if (!isDisposed()) {
            super.dispose();

            final Sdk sdk = Sdk.getCurrent();
            if (sdk != null) {
                DeviceManager manager = sdk.getDeviceManager();
                manager.unregisterListener(this);
            }
        }
    }

    // ---- Init and reset/reload methods ----

    /**
     * Sets the reference to the file being edited.
     * <p/>The UI is initialized in {@link #onXmlModelLoaded()} which is called as the XML model is
     * loaded (or reloaded as the SDK/target changes).
     *
     * @param file the file being opened
     *
     * @see #onXmlModelLoaded()
     * @see #replaceFile(IFile)
     * @see #changeFileOnNewConfig(IFile)
     */
    public void setFile(IFile file) {
        mEditedFile = file;
        ensureInitialized();
    }

    /**
     * Replaces the UI with a given file configuration. This is meant to answer the user
     * explicitly opening a different version of the same layout from the Package Explorer.
     * <p/>This attempts to keep the current config, but may change it if it's not compatible or
     * not the best match
     * @param file the file being opened.
     */
    public void replaceFile(IFile file) {
        // if there is no previous selection, revert to default mode.
        if (mConfiguration.getDevice() == null) {
            setFile(file); // onTargetChanged will be called later.
            return;
        }

        setFile(file);
        IProject project = mEditedFile.getProject();
        mResources = ResourceManager.getInstance().getProjectResources(project);

        ResourceFolder resFolder = ResourceManager.getInstance().getResourceFolder(file);
        mConfiguration.setEditedConfig(resFolder.getConfiguration());

        mDisableUpdates++; // we do not want to trigger onXXXChange when setting
                           // new values in the widgets.

        try {
            // only attempt to do anything if the SDK and targets are loaded.
            LoadStatus sdkStatus = AndmoreAndroidPlugin.getDefault().getSdkLoadStatus();

            if (sdkStatus == LoadStatus.LOADED) {
                setVisible(true);

                LoadStatus targetStatus = Sdk.getCurrent().checkAndLoadTargetData(mProjectTarget,
                        null /*project*/);

                if (targetStatus == LoadStatus.LOADED) {

                    // update the current config selection to make sure it's
                    // compatible with the new file
                    ConfigurationMatcher matcher = new ConfigurationMatcher(this);
                    matcher.adaptConfigSelection(true /*needBestMatch*/);
                    mConfiguration.syncFolderConfig();

                    // update the string showing the config value
                    selectConfiguration(mConfiguration.getEditedConfig());
                    updateActivity();
                }
            } else if (sdkStatus == LoadStatus.FAILED) {
                setVisible(true);
            }
        } finally {
            mDisableUpdates--;
        }
    }

    /**
     * Updates the UI with a new file that was opened in response to a config change.
     * @param file the file being opened.
     *
     * @see #replaceFile(IFile)
     */
    public void changeFileOnNewConfig(IFile file) {
        setFile(file);
        IProject project = mEditedFile.getProject();
        mResources = ResourceManager.getInstance().getProjectResources(project);

        ResourceFolder resFolder = ResourceManager.getInstance().getResourceFolder(file);
        FolderConfiguration config = resFolder.getConfiguration();
        mConfiguration.setEditedConfig(config);

        // All that's needed is to update the string showing the config value
        // (since the config combo settings chosen by the user).
        selectConfiguration(config);
    }

    /**
     * Resets the configuration chooser to reflect the given file configuration. This is
     * intended to be used by the "Show Included In" functionality where the user has
     * picked a non-default configuration (such as a particular landscape layout) and the
     * configuration chooser must be switched to a landscape layout. This method will
     * trigger a model change.
     * <p>
     * This will NOT trigger a redraw event!
     * <p>
     * FIXME: We are currently setting the configuration file to be the configuration for
     * the "outer" (the including) file, rather than the inner file, which is the file the
     * user is actually editing. We need to refine this, possibly with a way for the user
     * to choose which configuration they are editing. And in particular, we should be
     * filtering the configuration chooser to only show options in the outer configuration
     * that are compatible with the inner included file.
     *
     * @param file the file to be configured
     */
    public void resetConfigFor(IFile file) {
        setFile(file);

        IFolder parent = (IFolder) mEditedFile.getParent();
        ResourceFolder resFolder = mResources.getResourceFolder(parent);
        if (resFolder != null) {
            mConfiguration.setEditedConfig(resFolder.getConfiguration());
        } else {
            FolderConfiguration config = FolderConfiguration.getConfig(
                    parent.getName().split(RES_QUALIFIER_SEP));
            if (config != null) {
                mConfiguration.setEditedConfig(config);
            } else {
                mConfiguration.setEditedConfig(new FolderConfiguration());
            }
        }

        onXmlModelLoaded();
    }


    /**
     * Sets the current configuration to match the given folder configuration,
     * the given theme name, the given device and device state.
     *
     * @param configuration new folder configuration to use
     */
    public void setConfiguration(@NonNull Configuration configuration) {
        if (mClient != null) {
            mClient.aboutToChange(MASK_ALL);
        }

        Configuration oldConfiguration = mConfiguration;
        mConfiguration = configuration;
        mConfiguration.setChooser(this);

        selectTheme(configuration.getTheme());
        selectLocale(configuration.getLocale());
        selectDevice(configuration.getDevice());
        selectDeviceState(configuration.getDeviceState());
        selectTarget(configuration.getTarget());
        selectActivity(configuration.getActivity());

        // This may be a second refresh after triggered by theme above
        if (mClient != null) {
            LayoutCanvas canvas = mClient.getCanvas();
            if (canvas != null) {
                assert mConfiguration != oldConfiguration;
                canvas.getPreviewManager().updateChooserConfig(oldConfiguration, mConfiguration);
            }

            boolean accepted = mClient.changed(MASK_ALL);
            if (!accepted) {
                configuration = oldConfiguration;
                selectTheme(configuration.getTheme());
                selectLocale(configuration.getLocale());
                selectDevice(configuration.getDevice());
                selectDeviceState(configuration.getDeviceState());
                selectTarget(configuration.getTarget());
                selectActivity(configuration.getActivity());
                if (canvas != null && mConfiguration != oldConfiguration) {
                    canvas.getPreviewManager().updateChooserConfig(mConfiguration,
                            oldConfiguration);
                }
                return;
            } else {
                int changed = 0;
                if (!equal(oldConfiguration.getTheme(), mConfiguration.getTheme())) {
                    changed |= CFG_THEME;
                }
                if (!equal(oldConfiguration.getDevice(), mConfiguration.getDevice())) {
                    changed |= CFG_DEVICE | CFG_DEVICE_STATE;
                }
                if (changed != 0) {
                    syncToVariations(changed, mEditedFile, mConfiguration, false, true);
                }
            }
        }

        saveConstraints();
    }

    /**
     * Responds to the event that the basic SDK information finished loading.
     * @param target the possibly new target object associated with the file being edited (in case
     * the SDK path was changed).
     */
    public void onSdkLoaded(IAndroidTarget target) {
        // a change to the SDK means that we need to check for new/removed devices.
        mSdkChanged = true;

        // store the new target.
        mProjectTarget = target;

        mDisableUpdates++; // we do not want to trigger onXXXChange when setting
                           // new values in the widgets.
        try {
            updateDevices();
            updateTargets();
            ensureInitialized();
        } finally {
            mDisableUpdates--;
        }
    }

    /**
     * Responds to the XML model being loaded, either the first time or when the
     * Target/SDK changes.
     * <p>
     * This initializes the UI, either with the first compatible configuration
     * found, or it will attempt to restore a configuration if one is found to
     * have been saved in the file persistent storage.
     * <p>
     * If the SDK or target are not loaded, nothing will happen (but the method
     * must be called back when they are.)
     * <p>
     * The method automatically handles being called the first time after editor
     * creation, or being called after during SDK/Target changes (as long as
     * {@link #onSdkLoaded(IAndroidTarget)} is properly called).
     *
     * @return the target data for the rendering target used to render the
     *         layout
     *
     * @see #saveConstraints()
     * @see #onSdkLoaded(IAndroidTarget)
     */
    public AndroidTargetData onXmlModelLoaded() {
        AndroidTargetData targetData = null;

        // only attempt to do anything if the SDK and targets are loaded.
        LoadStatus sdkStatus = AndmoreAndroidPlugin.getDefault().getSdkLoadStatus();
        if (sdkStatus == LoadStatus.LOADED) {
            mDisableUpdates++; // we do not want to trigger onXXXChange when setting

            try {
                // init the devices if needed (new SDK or first time going through here)
                if (mSdkChanged) {
                    updateDevices();
                    updateTargets();
                    ensureInitialized();
                    mSdkChanged = false;
                }

                IProject project = mEditedFile.getProject();

                Sdk currentSdk = Sdk.getCurrent();
                if (currentSdk != null) {
                    mProjectTarget = currentSdk.getTarget(project);
                }

                LoadStatus targetStatus = LoadStatus.FAILED;
                if (mProjectTarget != null) {
                    targetStatus = Sdk.getCurrent().checkAndLoadTargetData(mProjectTarget, null);
                    updateTargets();
                    ensureInitialized();
                }

                if (targetStatus == LoadStatus.LOADED) {
                    setVisible(true);
                    if (mResources == null) {
                        mResources = ResourceManager.getInstance().getProjectResources(project);
                    }
                    if (mConfiguration.getEditedConfig() == null) {
                        IFolder parent = (IFolder) mEditedFile.getParent();
                        ResourceFolder resFolder = mResources.getResourceFolder(parent);
                        if (resFolder != null) {
                            mConfiguration.setEditedConfig(resFolder.getConfiguration());
                        } else {
                            FolderConfiguration config = FolderConfiguration.getConfig(
                                    parent.getName().split(RES_QUALIFIER_SEP));
                            if (config != null) {
                                mConfiguration.setEditedConfig(config);
                            } else {
                                mConfiguration.setEditedConfig(new FolderConfiguration());
                            }
                        }
                    }

                    targetData = Sdk.getCurrent().getTargetData(mProjectTarget);

                    // get the file stored state
                    ensureInitialized();
                    boolean loadedConfigData = mConfiguration.getDevice() != null &&
                            mConfiguration.getDeviceState() != null;

                    // Load locale list. This must be run after we initialize the
                    // configuration above, since it attempts to sync the UI with
                    // the value loaded into the configuration.
                    updateLocales();

                    // If the current state was loaded from the persistent storage, we update the
                    // UI with it and then try to adapt it (which will handle incompatible
                    // configuration).
                    // Otherwise, just look for the first compatible configuration.
                    ConfigurationMatcher matcher = new ConfigurationMatcher(this);
                    if (loadedConfigData) {
                        // first make sure we have the config to adapt
                        selectDevice(mConfiguration.getDevice());
                        selectDeviceState(mConfiguration.getDeviceState());
                        mConfiguration.syncFolderConfig();

                        matcher.adaptConfigSelection(false);

                        IAndroidTarget target = mConfiguration.getTarget();
                        selectTarget(target);
                        targetData = Sdk.getCurrent().getTargetData(target);
                    } else {
                        matcher.findAndSetCompatibleConfig(false);

                        // Default to modern layout lib
                        IAndroidTarget target = ConfigurationMatcher.findDefaultRenderTarget(this);
                        if (target != null) {
                            targetData = Sdk.getCurrent().getTargetData(target);
                            selectTarget(target);
                            mConfiguration.setTarget(target, true);
                        }
                    }

                    // Update activity: This is done before updateThemes() since
                    // the themes selection can depend on the currently selected activity
                    // (e.g. when there are manifest registrations for the theme to use
                    // for a given activity)
                    updateActivity();

                    // Update themes. This is done after updating the devices above,
                    // since we want to look at the chosen device size to decide
                    // what the default theme (for example, with Honeycomb we choose
                    // Holo as the default theme but only if the screen size is XLARGE
                    // (and of course only if the manifest does not specify another
                    // default theme).
                    updateThemes();

                    // update the string showing the config value
                    selectConfiguration(mConfiguration.getEditedConfig());

                    // compute the final current config
                    mConfiguration.syncFolderConfig();
                } else if (targetStatus == LoadStatus.FAILED) {
                    setVisible(true);
                }
            } finally {
                mDisableUpdates--;
            }
        }

        return targetData;
    }

    /**
     * This is a temporary workaround for a infrequently happening bug; apparently
     * there are cases where the configuration chooser isn't shown
     */
    public void ensureVisible() {
        if (!isVisible()) {
            LoadStatus sdkStatus = AndmoreAndroidPlugin.getDefault().getSdkLoadStatus();
            if (sdkStatus == LoadStatus.LOADED) {
                onXmlModelLoaded();
            }
        }
    }

    /**
     * An alternate layout for this layout has been created. This means that the
     * current layout may no longer be a best fit. However, since we support multiple
     * layouts being open at the same time, we need to adjust the current configuration
     * back to something where this layout <b>is</b> a best match.
     */
    public void onAlternateLayoutCreated() {
        IFile best = ConfigurationMatcher.getBestFileMatch(this);
        if (best != null && !best.equals(mEditedFile)) {
            ConfigurationMatcher matcher = new ConfigurationMatcher(this);
            matcher.adaptConfigSelection(true /*needBestMatch*/);
            mConfiguration.syncFolderConfig();
            if (mClient != null) {
                mClient.changed(MASK_ALL);
            }
        }
    }

    /**
     * Loads the list of {@link Device}s and inits the UI with it.
     */
    private void initDevices() {
        final Sdk sdk = Sdk.getCurrent();
        if (sdk != null) {
            DeviceManager manager = sdk.getDeviceManager();
            // This method can be called more than once, so avoid duplicate entries
            manager.unregisterListener(this);
            manager.registerListener(this);
            mDevices = manager.getDevices(DeviceManager.ALL_DEVICES);
        } else {
            mDevices = new ArrayList<Device>();
        }
    }

    /**
     * Loads the list of {@link IAndroidTarget} and inits the UI with it.
     */
    private boolean initTargets() {
        mTargetList.clear();

        Sdk currentSdk = Sdk.getCurrent();
        if (currentSdk != null) {
            for (IAndroidTarget target : currentSdk.getTargets()) {
                if (target.hasRenderingLibrary()) {
                    mTargetList.add(target);
                }
            }

            return true;
        }

        return false;
    }

    /** Ensures that the configuration has been initialized */
    public void ensureInitialized() {
        if (mConfiguration.getDevice() == null && mEditedFile != null) {
            String data = ConfigurationDescription.getDescription(mEditedFile);
            if (mInitialState != null) {
                data = mInitialState;
                mInitialState = null;
            }
            if (data != null) {
                mConfiguration.initialize(data);
                mConfiguration.syncFolderConfig();
            }
        }
    }

    private void updateDevices() {
        if (mDevices.size() == 0) {
            initDevices();
        }
    }

    private void updateTargets() {
        if (mTargetList.size() == 0) {
            if (!initTargets()) {
                return;
            }
        }

        IAndroidTarget renderingTarget = mConfiguration.getTarget();

        IAndroidTarget match = null;
        for (IAndroidTarget target : mTargetList) {
            if (renderingTarget != null) {
                // use equals because the rendering could be from a previous SDK, so
                // it may not be the same instance.
                if (renderingTarget.equals(target)) {
                    match = target;
                }
            } else if (mProjectTarget == target) {
                match = target;
            }

        }

        if (match == null) {
            // the rendering target is the same as the project.
            renderingTarget = mProjectTarget;
        } else {
            // set the rendering target to the new object.
            renderingTarget = match;
        }

        mConfiguration.setTarget(renderingTarget, true);
        selectTarget(renderingTarget);
    }

    /** Update the toolbar whenever a label has changed, to not only
     * cause the layout in the current toolbar to update, but to possibly
     * wrap the toolbars and update the layout of the surrounding area.
     */
    private void resizeToolBar() {
        Point size = getSize();
        Point newSize = computeSize(size.x, SWT.DEFAULT, true);
        setSize(newSize);
        Composite parent = getParent();
        parent.layout();
        parent.redraw();
    }


    Image getOrientationIcon(ScreenOrientation orientation, boolean flip) {
        IconFactory icons = IconFactory.getInstance();
        switch (orientation) {
            case LANDSCAPE:
                return icons.getIcon(flip ? ICON_LANDSCAPE_FLIP : ICON_LANDSCAPE);
            case SQUARE:
                return icons.getIcon(ICON_SQUARE);
            case PORTRAIT:
            default:
                return icons.getIcon(flip ? ICON_PORTRAIT_FLIP : ICON_PORTRAIT);
        }
    }

    ImageDescriptor getOrientationImage(ScreenOrientation orientation, boolean flip) {
        IconFactory icons = IconFactory.getInstance();
        switch (orientation) {
            case LANDSCAPE:
                return icons.getImageDescriptor(flip ? ICON_LANDSCAPE_FLIP : ICON_LANDSCAPE);
            case SQUARE:
                return icons.getImageDescriptor(ICON_SQUARE);
            case PORTRAIT:
            default:
                return icons.getImageDescriptor(flip ? ICON_PORTRAIT_FLIP : ICON_PORTRAIT);
        }
    }

    @NonNull
    ScreenOrientation getOrientation(State state) {
        FolderConfiguration config = DeviceConfigHelper.getFolderConfig(state);
        ScreenOrientation orientation = null;
        if (config != null && config.getScreenOrientationQualifier() != null) {
            orientation = config.getScreenOrientationQualifier().getValue();
        }

        if (orientation == null) {
            orientation = ScreenOrientation.PORTRAIT;
        }

        return orientation;
    }

    /**
     * Stores the current config selection into the edited file such that we can
     * bring it back the next time this layout is opened.
     */
    public void saveConstraints() {
        String description = mConfiguration.toPersistentString();
        if (description != null && !description.isEmpty()) {
            ConfigurationDescription.setDescription(mEditedFile, description);
        }
    }

    // ---- Setting the current UI state ----

    void selectDeviceState(@Nullable State state) {
        assert isUiThread();
        try {
            mDisableUpdates++;
            mOrientationCombo.setData(state);

            State nextState = mConfiguration.getNextDeviceState(state);
            mOrientationCombo.setImage(getOrientationIcon(getOrientation(state),
                    nextState != state));
        } finally {
            mDisableUpdates--;
        }
    }

    void selectTarget(IAndroidTarget target) {
        assert isUiThread();
        try {
            mDisableUpdates++;
            mTargetCombo.setData(target);
            String label = getRenderingTargetLabel(target, true);
            mTargetCombo.setText(label);
            resizeToolBar();
        } finally {
            mDisableUpdates--;
        }
    }

    /**
     * Selects a given {@link Device} in the device combo, if it is found.
     * @param device the device to select
     * @return true if the device was found.
     */
    boolean selectDevice(@Nullable Device device) {
        assert isUiThread();
        try {
            mDisableUpdates++;
            mDeviceCombo.setData(device);
            if (device != null) {
                mDeviceCombo.setText(getDeviceLabel(device, true));
            } else {
                mDeviceCombo.setText("Device");
            }
            resizeToolBar();
        } finally {
            mDisableUpdates--;
        }

        return false;
    }

    void selectActivity(@Nullable String fqcn) {
        assert isUiThread();
        try {
            mDisableUpdates++;
            if (fqcn != null) {
                mActivityCombo.setData(fqcn);
                String label = getActivityLabel(fqcn, true);
                mActivityCombo.setText(label);
            } else {
                mActivityCombo.setText("(Select)");
            }
            resizeToolBar();
        } finally {
            mDisableUpdates--;
        }
    }

    void selectTheme(@Nullable String theme) {
        assert isUiThread();
        try {
            mDisableUpdates++;
            assert theme == null ||  theme.startsWith(STYLE_RESOURCE_PREFIX)
                    || theme.startsWith(ANDROID_STYLE_RESOURCE_PREFIX) : theme;
            mThemeCombo.setData(theme);
            if (theme != null) {
                mThemeCombo.setText(getThemeLabel(theme, true));
            } else {
                // FIXME eclipse claims this is dead code.
                mThemeCombo.setText("(Set Theme)");
            }
            resizeToolBar();
        } finally {
            mDisableUpdates--;
        }
    }

    void selectLocale(@Nullable Locale locale) {
        assert isUiThread();
        try {
            mDisableUpdates++;
            mLocaleCombo.setData(locale);
            String label = Strings.nullToEmpty(getLocaleLabel(this, locale, true));
            mLocaleCombo.setText(label);

            Image image = getFlagImage(locale);
            mLocaleCombo.setImage(image);

            resizeToolBar();
        } finally {
            mDisableUpdates--;
        }
    }

    @NonNull
    Image getFlagImage(@Nullable Locale locale) {
        if (locale != null) {
            return locale.getFlagImage();
        }

        return FlagManager.getGlobeIcon();
    }

    private void selectConfiguration(FolderConfiguration fileConfig) {
        /* For now, don't show any text in the configuration combo, use just an
           icon. This has the advantage that the configuration contents don't
           shift around, so you can for example click back and forth between
           portrait and landscape without the icon moving under the mouse.
           If this works well, remove this whole method post ADT 21.
        assert isUiThread();
        try {
            String current = mEditedFile.getParent().getName();
            if (current.equals(FD_RES_LAYOUT)) {
                current = "default";
            }

            // Pretty things up a bit
            //if (current == null || current.equals("default")) {
            //    current = "Default Configuration";
            //}
            mConfigCombo.setText(current);
            resizeToolBar();
        } finally {
            mDisableUpdates--;
        }
         */
    }

    /**
     * Finds a locale matching the config from a file.
     *
     * @param language the language qualifier or null if none is set.
     * @param region the region qualifier or null if none is set.
     * @return true if there was a change in the combobox as a result of
     *         applying the locale
     */
    private boolean setLocale(@Nullable Locale locale) {
        boolean changed = !Objects.equal(mConfiguration.getLocale(), locale);
        selectLocale(locale);

        return changed;
    }

    // ---- Creating UI labels ----

    /**
     * Returns a suitable label to use to display the given activity
     *
     * @param fqcn the activity class to look up a label for
     * @param brief if true, generate a brief label (suitable for a toolbar
     *            button), otherwise a fuller name (suitable for a menu item)
     * @return the label
     */
    public static String getActivityLabel(String fqcn, boolean brief) {
        if (brief) {
            String label = fqcn;
            int packageIndex = label.lastIndexOf('.');
            if (packageIndex != -1) {
                label = label.substring(packageIndex + 1);
            }
            int innerClass = label.lastIndexOf('$');
            if (innerClass != -1) {
                label = label.substring(innerClass + 1);
            }

            // Also strip out the "Activity" or "Fragment" common suffix
            // if this is a long name
            if (label.endsWith("Activity") && label.length() > 8 + 12) { // 12 chars + 8 in suffix
                label = label.substring(0, label.length() - 8);
            } else if (label.endsWith("Fragment") && label.length() > 8 + 12) {
                label = label.substring(0, label.length() - 8);
            }

            return label;
        }

        return fqcn;
    }

    /**
     * Returns a suitable label to use to display the given theme
     *
     * @param theme the theme to produce a label for
     * @param brief if true, generate a brief label (suitable for a toolbar
     *            button), otherwise a fuller name (suitable for a menu item)
     * @return the label
     */
    public static String getThemeLabel(String theme, boolean brief) {
        theme = ResourceHelper.styleToTheme(theme);

        if (brief) {
            int index = theme.lastIndexOf('.');
            if (index < theme.length() - 1) {
                return theme.substring(index + 1);
            }
        }
        return theme;
    }

    /**
     * Returns a suitable label to use to display the given rendering target
     *
     * @param target the target to produce a label for
     * @param brief if true, generate a brief label (suitable for a toolbar
     *            button), otherwise a fuller name (suitable for a menu item)
     * @return the label
     */
    public static String getRenderingTargetLabel(IAndroidTarget target, boolean brief) {
        if (target == null) {
            return "<null>";
        }

        AndroidVersion version = target.getVersion();

        if (brief) {
            if (target.isPlatform()) {
                return Integer.toString(version.getApiLevel());
            } else {
                return target.getName() + ':' + Integer.toString(version.getApiLevel());
            }
        }

        String label = String.format("API %1$d: %2$s",
                version.getApiLevel(),
                target.getShortClasspathName());

        return label;
    }

    /**
     * Returns a suitable label to use to display the given device
     *
     * @param device the device to produce a label for
     * @param brief if true, generate a brief label (suitable for a toolbar
     *            button), otherwise a fuller name (suitable for a menu item)
     * @return the label
     */
    public static String getDeviceLabel(@Nullable Device device, boolean brief) {
        if (device == null) {
            return "";
        }
        String name = device.getName();

        if (brief) {
            // Produce a really brief summary of the device name, suitable for
            // use in the narrow space available in the toolbar for example
            int nexus = name.indexOf("Nexus"); //$NON-NLS-1$
            if (nexus != -1) {
                int begin = name.indexOf('(');
                if (begin != -1) {
                    begin++;
                    int end = name.indexOf(')', begin);
                    if (end != -1) {
                        return name.substring(begin, end).trim();
                    }
                }
            }
        }

        return name;
    }

    /**
     * Returns a suitable label to use to display the given locale
     *
     * @param chooser the chooser, if known
     * @param locale the locale to look up a label for
     * @param brief if true, generate a brief label (suitable for a toolbar
     *            button), otherwise a fuller name (suitable for a menu item)
     * @return the label
     */
    @Nullable
    public static String getLocaleLabel(
            @Nullable ConfigurationChooser chooser,
            @Nullable Locale locale,
            boolean brief) {
        if (locale == null) {
            return null;
        }

        if (!locale.hasLanguage()) {
            if (brief) {
                // Just use the icon
                return "";
            }

            boolean hasLocale = false;
            ResourceRepository projectRes = chooser != null ? chooser.mClient.getProjectResources()
                    : null;
            if (projectRes != null) {
                hasLocale = projectRes.getLanguages().size() > 0;
            }

            if (hasLocale) {
                return "Other";
            } else {
                return "Any";
            }
        }

        String languageCode = locale.locale.getLanguage();
        String languageName = LocaleManager.getLanguageName(languageCode);

        if (!locale.hasRegion()) {
            // TODO: Make the region string use "Other" instead of "Any" if
            // there is more than one region for a given language
            //if (regions.size() > 0) {
            //    return String.format("%1$s / Other", language);
            //} else {
            //    return String.format("%1$s / Any", language);
            //}
            if (!brief && languageName != null) {
                return String.format("%1$s (%2$s)", languageName, languageCode);
            } else {
                return languageCode;
            }
        } else {
            String regionCode = locale.locale.getRegion();
            if (!brief && languageName != null) {
                String regionName = LocaleManager.getRegionName(regionCode);
                if (regionName != null) {
                    return String.format("%1$s (%2$s) in %3$s (%4$s)", languageName, languageCode,
                            regionName, regionCode);
                }
                return String.format("%1$s (%2$s) in %3$s", languageName, languageCode,
                        regionCode);
            }
            return String.format("%1$s / %2$s", languageCode, regionCode);
        }
    }

    // ---- Implements DevicesChangedListener ----

    @Override
    public void onDevicesChanged() {
        final Sdk sdk = Sdk.getCurrent();
        if (sdk != null) {
            mDevices = sdk.getDeviceManager().getDevices(DeviceManager.ALL_DEVICES);
        } else {
            mDevices = new ArrayList<Device>();
        }
    }

    // ---- Reacting to UI changes ----

    /**
     * Called when the selection of the device combo changes.
     */
    void onDeviceChange() {
        // because changing the content of a combo triggers a change event, respect the
        // mDisableUpdates flag
        if (mDisableUpdates > 0) {
            return;
        }

        // Attempt to preserve the device state
        String stateName = null;
        Device prevDevice = mConfiguration.getDevice();
        State prevState = mConfiguration.getDeviceState();
        Device device = (Device) mDeviceCombo.getData();
        if (prevDevice != null && prevState != null && device != null) {
            // get the previous config, so that we can look for a close match
            FolderConfiguration oldConfig = DeviceConfigHelper.getFolderConfig(prevState);
            if (oldConfig != null) {
                stateName = ConfigurationMatcher.getClosestMatch(oldConfig, device.getAllStates());
            }
        }
        mConfiguration.setDevice(device, true);
        State newState = Configuration.getState(device, stateName);
        mConfiguration.setDeviceState(newState, true);
        selectDeviceState(newState);
        mConfiguration.syncFolderConfig();

        // Notify
        IFile file = mEditedFile;
        boolean accepted = mClient.changed(CFG_DEVICE | CFG_DEVICE_STATE);
        if (!accepted) {
            mConfiguration.setDevice(prevDevice, true);
            mConfiguration.setDeviceState(prevState, true);
            mConfiguration.syncFolderConfig();
            selectDevice(prevDevice);
            selectDeviceState(prevState);
            return;
        } else {
            syncToVariations(CFG_DEVICE | CFG_DEVICE_STATE, file, mConfiguration, false, true);
        }

        saveConstraints();
    }

    /**
     * Synchronizes changes to the given attributes (indicated by the mask
     * referencing the {@code CFG_} configuration attribute bit flags in
     * {@link Configuration} to the layout variations of the given updated file.
     *
     * @param flags the attributes which were updated
     * @param updatedFile the file which was updated
     * @param base the base configuration to base the chooser off of
     * @param includeSelf whether the updated file itself should be updated
     * @param async whether the updates should be performed asynchronously
     */
    public void syncToVariations(
            final int flags,
            final @NonNull IFile updatedFile,
            final @NonNull Configuration base,
            final boolean includeSelf,
            boolean async) {
        if (async) {
            getDisplay().asyncExec(new Runnable() {
                @Override
                public void run() {
                    doSyncToVariations(flags, updatedFile, includeSelf, base);
                }
            });
        } else {
            doSyncToVariations(flags, updatedFile, includeSelf, base);
        }
    }

    private void doSyncToVariations(int flags, IFile updatedFile, boolean includeSelf,
            Configuration base) {
        // Synchronize the given changes to other configurations as well
        List<IFile> files = AdtUtils.getResourceVariations(updatedFile, includeSelf);
        for (IFile file : files) {
            Configuration configuration = Configuration.create(base, file);
            configuration.setTheme(base.getTheme());
            configuration.setActivity(base.getActivity());
            Collection<IEditorPart> editors = AdtUtils.findEditorsFor(file, false);
            boolean found = false;
            for (IEditorPart editor : editors) {
                if (editor instanceof CommonXmlEditor) {
                    CommonXmlDelegate delegate = ((CommonXmlEditor) editor).getDelegate();
                    if (delegate instanceof LayoutEditorDelegate) {
                        editor = ((LayoutEditorDelegate) delegate).getGraphicalEditor();
                    }
                }
                if (editor instanceof GraphicalEditorPart) {
                    ConfigurationChooser chooser =
                        ((GraphicalEditorPart) editor).getConfigurationChooser();
                    chooser.setConfiguration(configuration);
                    found = true;
                }
            }
            if (!found) {
                // Just update the file persistence
                String description = configuration.toPersistentString();
                ConfigurationDescription.setDescription(file, description);
            }
        }
    }

    /**
     * Called when the device config selection changes.
     */
    void onDeviceConfigChange() {
        // because changing the content of a combo triggers a change event, respect the
        // mDisableUpdates flag
        if (mDisableUpdates > 0) {
            return;
        }

        State prev = mConfiguration.getDeviceState();
        State state = (State) mOrientationCombo.getData();
        mConfiguration.setDeviceState(state, false);

        if (mClient != null) {
            boolean accepted = mClient.changed(CFG_DEVICE | CFG_DEVICE_STATE);
            if (!accepted) {
                mConfiguration.setDeviceState(prev, false);
                selectDeviceState(prev);
                return;
            }
        }

        saveConstraints();
    }

    /**
     * Call back for language combo selection
     */
    void onLocaleChange() {
        // because mLocaleList triggers onLocaleChange at each modification, the filling
        // of the combo with data will trigger notifications, and we don't want that.
        if (mDisableUpdates > 0) {
            return;
        }

        Locale prev = mConfiguration.getLocale();
        Locale locale = (Locale) mLocaleCombo.getData();
        if (locale == null) {
            locale = Locale.ANY;
        }
        mConfiguration.setLocale(locale, false);

        if (mClient != null) {
            boolean accepted = mClient.changed(CFG_LOCALE);
            if (!accepted) {
                mConfiguration.setLocale(prev, false);
                selectLocale(prev);
            }
        }

        // Store locale project-wide setting
        mConfiguration.saveRenderState();
    }


    void onThemeChange() {
        if (mDisableUpdates > 0) {
            return;
        }

        String prev = mConfiguration.getTheme();
        mConfiguration.setTheme((String) mThemeCombo.getData());

        if (mClient != null) {
            boolean accepted = mClient.changed(CFG_THEME);
            if (!accepted) {
                mConfiguration.setTheme(prev);
                selectTheme(prev);
                return;
            } else {
                syncToVariations(CFG_DEVICE|CFG_DEVICE_STATE, mEditedFile, mConfiguration,
                        false, true);
            }
        }

        saveConstraints();
    }

    void notifyFolderConfigChanged() {
        if (mDisableUpdates > 0 || mClient == null) {
            return;
        }

        if (mClient.changed(CFG_FOLDER)) {
            saveConstraints();
        }
    }

    void onSelectActivity() {
        if (mDisableUpdates > 0) {
            return;
        }

        String activity = (String) mActivityCombo.getData();
        mConfiguration.setActivity(activity);

        if (activity == null) {
            return;
        }

        // See if there is a default theme assigned to this activity, and if so, use it
        ManifestInfo manifest = ManifestInfo.get(mEditedFile.getProject());
        String preferred = null;
        ActivityAttributes attributes = manifest.getActivityAttributes(activity);
        if (attributes != null) {
            preferred = attributes.getTheme();
        }
        if (preferred != null && !Objects.equal(preferred, mConfiguration.getTheme())) {
            // Yes, switch to it
            selectTheme(preferred);
            onThemeChange();
        }

        // Persist in XML
        if (mClient != null) {
            mClient.setActivity(activity);
        }

        saveConstraints();
    }

    /**
     * Call back for api level combo selection
     */
    void onRenderingTargetChange() {
        // because mApiCombo triggers onApiLevelChange at each modification, the filling
        // of the combo with data will trigger notifications, and we don't want that.
        if (mDisableUpdates > 0) {
            return;
        }

        IAndroidTarget prevTarget = mConfiguration.getTarget();
        String prevTheme = mConfiguration.getTheme();

        int changeFlags = 0;

        // tell the listener a new rendering target is being set. Need to do this before updating
        // mRenderingTarget.
        if (prevTarget != null) {
            changeFlags |= CFG_TARGET;
            mClient.aboutToChange(changeFlags);
        }

        IAndroidTarget target = (IAndroidTarget) mTargetCombo.getData();
        mConfiguration.setTarget(target, true);

        // force a theme update to reflect the new rendering target.
        // This must be done after computeCurrentConfig since it'll depend on the currentConfig
        // to figure out the theme list.
        String oldTheme = mConfiguration.getTheme();
        updateThemes();
        // updateThemes may change the theme (based on theme availability in the new rendering
        // target) so mark theme change if necessary
        if (!Objects.equal(oldTheme, mConfiguration.getTheme())) {
            changeFlags |= CFG_THEME;
        }

        if (target != null) {
            changeFlags |= CFG_TARGET;
            changeFlags |= CFG_FOLDER; // In case we added a -vNN qualifier
        }

        // Store project-wide render-target setting
        mConfiguration.saveRenderState();

        mConfiguration.syncFolderConfig();

        if (mClient != null) {
            boolean accepted = mClient.changed(changeFlags);
            if (!accepted) {
                mConfiguration.setTarget(prevTarget, true);
                mConfiguration.setTheme(prevTheme);
                mConfiguration.syncFolderConfig();
                selectTheme(prevTheme);
                selectTarget(prevTarget);
            }
        }
    }

    /**
     * Syncs this configuration to the project wide locale and render target settings. The
     * locale may ignore the project-wide setting if it is a locale-specific
     * configuration.
     *
     * @return true if one or both of the toggles were changed, false if there were no
     *         changes
     */
    public boolean syncRenderState() {
        if (mConfiguration.getEditedConfig() == null) {
            // Startup; ignore
            return false;
        }

        boolean renderTargetChanged = false;

        // When a page is re-activated, force the toggles to reflect the current project
        // state

        Pair<Locale, IAndroidTarget> pair = Configuration.loadRenderState(this);

        int changeFlags = 0;
        // Only sync the locale if this layout is not already a locale-specific layout!
        if (pair != null && !mConfiguration.isLocaleSpecificLayout()) {
            Locale locale = pair.getFirst();
            if (locale != null) {
                boolean localeChanged = setLocale(locale);
                if (localeChanged) {
                    changeFlags |= CFG_LOCALE;
                }
            } else {
                locale = Locale.ANY;
            }
            mConfiguration.setLocale(locale, true);
        }

        // Sync render target
        IAndroidTarget configurationTarget = mConfiguration.getTarget();
        IAndroidTarget target = pair != null ? pair.getSecond() : configurationTarget;
        if (target != null && configurationTarget != target) {
            if (mClient != null && configurationTarget != null) {
                changeFlags |= CFG_TARGET;
                mClient.aboutToChange(changeFlags);
            }

            mConfiguration.setTarget(target, true);
            selectTarget(target);
            renderTargetChanged = true;
        }

        // Neither locale nor render target changed: nothing to do
        if (changeFlags == 0) {
            return false;
        }

        // Update the locale and/or the render target. This code contains a logical
        // merge of the onRenderingTargetChange() and onLocaleChange() methods, combined
        // such that we don't duplicate work.

        // Compute the new configuration; we want to do this both for locale changes
        // and for render targets.
        mConfiguration.syncFolderConfig();
        changeFlags |= CFG_FOLDER; // in case we added/remove a -v<NN> qualifier

        if (renderTargetChanged) {
            // force a theme update to reflect the new rendering target.
            // This must be done after computeCurrentConfig since it'll depend on the currentConfig
            // to figure out the theme list.
            updateThemes();
        }

        if (mClient != null) {
            mClient.changed(changeFlags);
        }

        return true;
    }

    // ---- Populate data structures with themes, locales, etc ----

    /**
     * Updates the internal list of themes.
     */
    private void updateThemes() {
        if (mClient == null) {
            return; // can't do anything without it.
        }

        ResourceRepository frameworkRes = mClient.getFrameworkResources(
                mConfiguration.getTarget());

        mDisableUpdates++;

        try {
            if (mEditedFile != null) {
                String theme = mConfiguration.getTheme();
                if (theme == null || theme.isEmpty() || mClient.getIncludedWithin() != null) {
                    mConfiguration.setTheme(null);
                    mConfiguration.computePreferredTheme();
                }
                assert mConfiguration.getTheme() != null;
            }

            mThemeList.clear();

            ArrayList<String> themes = new ArrayList<String>();
            ResourceRepository projectRes = mClient.getProjectResources();
            // in cases where the opened file is not linked to a project, this could be null.
            if (projectRes != null) {
                // get the configured resources for the project
                Map<ResourceType, Map<String, ResourceValue>> configuredProjectRes =
                    mClient.getConfiguredProjectResources();

                if (configuredProjectRes != null) {
                    // get the styles.
                    Map<String, ResourceValue> styleMap = configuredProjectRes.get(
                            ResourceType.STYLE);

                    if (styleMap != null) {
                        // collect the themes out of all the styles, ie styles that extend,
                        // directly or indirectly a platform theme.
                        for (ResourceValue value : styleMap.values()) {
                            if (isTheme(value, styleMap, null)) {
                                String theme = value.getName();
                                themes.add(theme);
                            }
                        }

                        Collections.sort(themes);

                        for (String theme : themes) {
                            if (!theme.startsWith(PREFIX_RESOURCE_REF)) {
                                theme = STYLE_RESOURCE_PREFIX + theme;
                            }
                            mThemeList.add(theme);
                        }
                    }
                }
                themes.clear();
            }

            // get the themes, and languages from the Framework.
            if (frameworkRes != null) {
                // get the configured resources for the framework
                Map<ResourceType, Map<String, ResourceValue>> frameworResources =
                    frameworkRes.getConfiguredResources(mConfiguration.getFullConfig());

                if (frameworResources != null) {
                    // get the styles.
                    Map<String, ResourceValue> styles = frameworResources.get(ResourceType.STYLE);

                    // collect the themes out of all the styles.
                    for (ResourceValue value : styles.values()) {
                        String name = value.getName();
                        if (name.startsWith("Theme.") || name.equals("Theme")) { //$NON-NLS-1$ //$NON-NLS-2$
                            themes.add(value.getName());
                        }
                    }

                    // sort them and add them to the combo
                    Collections.sort(themes);

                    for (String theme : themes) {
                        if (!theme.startsWith(PREFIX_RESOURCE_REF)) {
                            theme = ANDROID_STYLE_RESOURCE_PREFIX + theme;
                        }
                        mThemeList.add(theme);
                    }

                    themes.clear();
                }
            }

            // Migration: In the past we didn't store the style prefix in the settings;
            // this meant we might lose track of whether the theme is a project style
            // or a framework style. For now we need to migrate. Search through the
            // theme list until we have a match
            String theme = mConfiguration.getTheme();
            if (theme != null && !theme.startsWith(PREFIX_RESOURCE_REF)) {
                String projectStyle = STYLE_RESOURCE_PREFIX + theme;
                String frameworkStyle = ANDROID_STYLE_RESOURCE_PREFIX + theme;
                for (String t : mThemeList) {
                    if (t.equals(projectStyle)) {
                        mConfiguration.setTheme(projectStyle);
                        break;
                    } else if (t.equals(frameworkStyle)) {
                        mConfiguration.setTheme(frameworkStyle);
                        break;
                    }
                }
                if (!theme.startsWith(PREFIX_RESOURCE_REF)) {
                    // Arbitrary guess
                    if (theme.startsWith("Theme.")) {
                        theme = ANDROID_STYLE_RESOURCE_PREFIX + theme;
                    } else {
                        theme = STYLE_RESOURCE_PREFIX + theme;
                    }
                }
            }

            // TODO: Handle the case where you have a theme persisted that isn't available??
            // We could look up mConfiguration.theme and make sure it appears in the list! And if
            // not, picking one.
            selectTheme(mConfiguration.getTheme());
        } finally {
            mDisableUpdates--;
        }
    }

    private void updateActivity() {
        if (mEditedFile != null) {
            String preferred = getPreferredActivity(mEditedFile);
            selectActivity(preferred);
        }
    }

    /**
     * Updates the locale combo.
     * This must be called from the UI thread.
     */
    public void updateLocales() {
        if (mClient == null) {
            return; // can't do anything w/o it.
        }

        mDisableUpdates++;

        try {
            mLocaleList.clear();

            SortedSet<String> languages = null;

            // get the languages from the project.
            ResourceRepository projectRes = mClient.getProjectResources();

            // in cases where the opened file is not linked to a project, this could be null.
            if (projectRes != null) {
                // now get the languages from the project.
                languages = projectRes.getLanguages();

                for (String language : languages) {
                    // find the matching regions and add them
                    SortedSet<String> regions = projectRes.getRegions(language);
                    for (String region : regions) {
                        mLocaleList.add(Locale.create(new LocaleQualifier(null, language, region, null)));
                    }

                    // now the entry for the other regions the language alone
                    // create a region qualifier that will never be matched by qualified resources.
                    mLocaleList.add(Locale.create(new LocaleQualifier(null, language, null, null)));
                }
            }

            // create language/region qualifier that will never be matched by qualified resources.
            mLocaleList.add(Locale.ANY);

            Locale locale = mConfiguration.getLocale();
            setLocale(locale);
        } finally {
            mDisableUpdates--;
        }
    }

    @Nullable
    private String getPreferredActivity(@NonNull IFile file) {
        // Store/restore the activity context in the config state to help with
        // performance if for some reason we can't write it into the XML file and to
        // avoid having to open the model below
        if (mConfiguration.getActivity() != null) {
            return mConfiguration.getActivity();
        }

        IProject project = file.getProject();

        // Look up from XML file
        Document document = DomUtilities.getDocument(file);
        if (document != null) {
            Element element = document.getDocumentElement();
            if (element != null) {
                String activity = element.getAttributeNS(TOOLS_URI, ATTR_CONTEXT);
                if (activity != null && !activity.isEmpty()) {
                    if (activity.startsWith(".") || activity.indexOf('.') == -1) { //$NON-NLS-1$
                        ManifestInfo manifest = ManifestInfo.get(project);
                        String pkg = manifest.getPackage();
                        if (!pkg.isEmpty()) {
                            if (activity.startsWith(".")) { //$NON-NLS-1$
                                activity = pkg + activity;
                            } else {
                                activity = activity + '.' + pkg;
                            }
                        }
                    }

                    mConfiguration.setActivity(activity);
                    saveConstraints();
                    return activity;
                }
            }
        }

        // No, not available there: try to infer it from the code index
        String includedIn = null;
        Reference includedWithin = mClient.getIncludedWithin();
        if (mClient != null && includedWithin != null) {
            includedIn = includedWithin.getName();
        }

        ManifestInfo manifest = ManifestInfo.get(project);
        String pkg = manifest.getPackage();
        String layoutName = ResourceHelper.getLayoutName(mEditedFile);

        // If we are rendering a layout in included context, pick the theme
        // from the outer layout instead
        if (includedIn != null) {
            layoutName = includedIn;
        }

        String activity = ManifestInfo.guessActivity(project, layoutName, pkg);

        if (activity == null) {
            List<String> activities = ManifestInfo.getProjectActivities(project);
            if (activities.size() == 1) {
                activity = activities.get(0);
            }
        }

        if (activity != null) {
            mConfiguration.setActivity(activity);
            saveConstraints();
            return activity;
        }

        // TODO: Do anything else, such as pick the first activity found?
        // Or just leave some default label instead?
        // Also, figure out what to store in the mState so I don't keep trying

        return null;
    }

    /**
     * Returns whether the given <var>style</var> is a theme.
     * This is done by making sure the parent is a theme.
     * @param value the style to check
     * @param styleMap the map of styles for the current project. Key is the style name.
     * @param seen the map of styles we have already processed (or null if not yet
     *          initialized). Only the keys are significant (since there is no IdentityHashSet).
     * @return True if the given <var>style</var> is a theme.
     */
    private static boolean isTheme(ResourceValue value, Map<String, ResourceValue> styleMap,
            IdentityHashMap<ResourceValue, Boolean> seen) {
        if (value instanceof StyleResourceValue) {
            StyleResourceValue style = (StyleResourceValue)value;

            boolean frameworkStyle = false;
            String parentStyle = style.getParentStyle();
            if (parentStyle == null) {
                // if there is no specified parent style we look an implied one.
                // For instance 'Theme.light' is implied child style of 'Theme',
                // and 'Theme.light.fullscreen' is implied child style of 'Theme.light'
                String name = style.getName();
                int index = name.lastIndexOf('.');
                if (index != -1) {
                    parentStyle = name.substring(0, index);
                }
            } else {
                // remove the useless @ if it's there
                if (parentStyle.startsWith("@")) {
                    parentStyle = parentStyle.substring(1);
                }

                // check for framework identifier.
                if (parentStyle.startsWith(ANDROID_NS_NAME_PREFIX)) {
                    frameworkStyle = true;
                    parentStyle = parentStyle.substring(ANDROID_NS_NAME_PREFIX.length());
                }

                // at this point we could have the format style/<name>. we want only the name
                if (parentStyle.startsWith("style/")) {
                    parentStyle = parentStyle.substring("style/".length());
                }
            }

            if (parentStyle != null) {
                if (frameworkStyle) {
                    // if the parent is a framework style, it has to be 'Theme' or 'Theme.*'
                    return parentStyle.equals("Theme") || parentStyle.startsWith("Theme.");
                } else {
                    // if it's a project style, we check this is a theme.
                    ResourceValue parentValue = styleMap.get(parentStyle);

                    // also prevent stack overflow in case the dev mistakenly declared
                    // the parent of the style as the style itself.
                    if (parentValue != null && !parentValue.equals(value)) {
                        if (seen == null) {
                            seen = new IdentityHashMap<ResourceValue, Boolean>();
                            seen.put(value, Boolean.TRUE);
                        } else if (seen.containsKey(parentValue)) {
                            return false;
                        }
                        seen.put(parentValue, Boolean.TRUE);
                        return isTheme(parentValue, styleMap, seen);
                    }
                }
            }
        }

        return false;
    }

    /**
     * Returns true if this configuration chooser represents the best match for
     * the given file
     *
     * @param file the file to test
     * @param config the config to test
     * @return true if the given config is the best match for the given file
     */
    public boolean isBestMatchFor(IFile file, FolderConfiguration config) {
        ResourceFile match = mResources.getMatchingFile(mEditedFile.getName(),
                ResourceType.LAYOUT, config);
        if (match != null) {
            return match.getFile().equals(mEditedFile);
        }

        return false;
    }
}

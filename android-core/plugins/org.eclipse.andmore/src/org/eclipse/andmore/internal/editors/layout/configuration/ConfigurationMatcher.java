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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.andmore.AdtUtils;
import org.eclipse.andmore.AndmoreAndroidPlugin;
import org.eclipse.andmore.internal.editors.layout.LayoutEditorDelegate;
import org.eclipse.andmore.internal.resources.manager.ProjectResources;
import org.eclipse.andmore.internal.resources.manager.ResourceManager;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.andmore.io.IFileWrapper;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.ui.IEditorPart;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.resources.ResourceFile;
import com.android.ide.common.resources.configuration.DensityQualifier;
import com.android.ide.common.resources.configuration.DeviceConfigHelper;
import com.android.ide.common.resources.configuration.FolderConfiguration;
import com.android.ide.common.resources.configuration.LocaleQualifier;
import com.android.ide.common.resources.configuration.NightModeQualifier;
import com.android.ide.common.resources.configuration.ResourceQualifier;
import com.android.ide.common.resources.configuration.ScreenOrientationQualifier;
import com.android.ide.common.resources.configuration.ScreenSizeQualifier;
import com.android.ide.common.resources.configuration.UiModeQualifier;
import com.android.ide.common.resources.configuration.VersionQualifier;
import com.android.resources.Density;
import com.android.resources.NightMode;
import com.android.resources.ResourceType;
import com.android.resources.ScreenOrientation;
import com.android.resources.ScreenSize;
import com.android.resources.UiMode;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.devices.Device;
import com.android.sdklib.devices.State;
import com.android.sdklib.repository.PkgProps;
import com.android.utils.Pair;
import com.android.utils.SparseIntArray;

/**
 * Produces matches for configurations
 * <p>
 * See algorithm described here:
 * http://developer.android.com/guide/topics/resources/providing-resources.html
 */
public class ConfigurationMatcher {
    private static final boolean PREFER_RECENT_RENDER_TARGETS = true;

    private final ConfigurationChooser mConfigChooser;
    private final Configuration mConfiguration;
    private final IFile mEditedFile;
    private final ProjectResources mResources;
    private final boolean mUpdateUi;

    ConfigurationMatcher(ConfigurationChooser chooser) {
        this(chooser, chooser.getConfiguration(), chooser.getEditedFile(),
                chooser.getResources(), true);
    }

    ConfigurationMatcher(
            @NonNull ConfigurationChooser chooser,
            @NonNull Configuration configuration,
            @Nullable IFile editedFile,
            @Nullable ProjectResources resources,
            boolean updateUi) {
        mConfigChooser = chooser;
        mConfiguration = configuration;
        mEditedFile = editedFile;
        mResources = resources;
        mUpdateUi = updateUi;
    }

    // ---- Finding matching configurations ----

    private static class ConfigBundle {
        private final FolderConfiguration config;
        private int localeIndex;
        private int dockModeIndex;
        private int nightModeIndex;

        private ConfigBundle() {
            config = new FolderConfiguration();
        }

        private ConfigBundle(ConfigBundle bundle) {
            config = new FolderConfiguration();
            config.set(bundle.config);
            localeIndex = bundle.localeIndex;
            dockModeIndex = bundle.dockModeIndex;
            nightModeIndex = bundle.nightModeIndex;
        }
    }

    private static class ConfigMatch {
        final FolderConfiguration testConfig;
        final Device device;
        final State state;
        final ConfigBundle bundle;

        public ConfigMatch(@NonNull FolderConfiguration testConfig, @NonNull Device device,
                @NonNull State state, @NonNull ConfigBundle bundle) {
            this.testConfig = testConfig;
            this.device = device;
            this.state = state;
            this.bundle = bundle;
        }

        @Override
        public String toString() {
            return device.getName() + " - " + state.getName();
        }
    }

    /**
     * Checks whether the current edited file is the best match for a given config.
     * <p>
     * This tests against other versions of the same layout in the project.
     * <p>
     * The given config must be compatible with the current edited file.
     * @param config the config to test.
     * @return true if the current edited file is the best match in the project for the
     * given config.
     */
    public boolean isCurrentFileBestMatchFor(FolderConfiguration config) {
        ResourceFile match = mResources.getMatchingFile(mEditedFile.getName(),
                ResourceType.LAYOUT, config);

        if (match != null) {
            return match.getFile().equals(mEditedFile);
        } else {
            // if we stop here that means the current file is not even a match!
            AndmoreAndroidPlugin.log(IStatus.ERROR, "Current file is not a match for the given config.");
        }

        return false;
    }

    /**
     * Adapts the current device/config selection so that it's compatible with
     * the configuration.
     * <p>
     * If the current selection is compatible, nothing is changed.
     * <p>
     * If it's not compatible, configs from the current devices are tested.
     * <p>
     * If none are compatible, it reverts to
     * {@link #findAndSetCompatibleConfig(boolean)}
     */
    void adaptConfigSelection(boolean needBestMatch) {
        // check the device config (ie sans locale)
        boolean needConfigChange = true; // if still true, we need to find another config.
        boolean currentConfigIsCompatible = false;
        State selectedState = mConfiguration.getDeviceState();
        FolderConfiguration editedConfig = mConfiguration.getEditedConfig();
        if (selectedState != null) {
            FolderConfiguration currentConfig = DeviceConfigHelper.getFolderConfig(selectedState);
            if (currentConfig != null && editedConfig.isMatchFor(currentConfig)) {
                currentConfigIsCompatible = true; // current config is compatible
                if (!needBestMatch || isCurrentFileBestMatchFor(currentConfig)) {
                    needConfigChange = false;
                }
            }
        }

        if (needConfigChange) {
            List<Locale> localeList = mConfigChooser.getLocaleList();

            // if the current state/locale isn't a correct match, then
            // look for another state/locale in the same device.
            FolderConfiguration testConfig = new FolderConfiguration();

            // first look in the current device.
            State matchState = null;
            int localeIndex = -1;
            Device device = mConfiguration.getDevice();
            if (device != null) {
                mainloop: for (State state : device.getAllStates()) {
                    testConfig.set(DeviceConfigHelper.getFolderConfig(state));

                    // loop on the locales.
                    for (int i = 0 ; i < localeList.size() ; i++) {
                        Locale locale = localeList.get(i);

                        // update the test config with the locale qualifiers
                        testConfig.setLocaleQualifier(locale.locale);

                        if (editedConfig.isMatchFor(testConfig) &&
                                isCurrentFileBestMatchFor(testConfig)) {
                            matchState = state;
                            localeIndex = i;
                            break mainloop;
                        }
                    }
                }
            }

            if (matchState != null) {
                mConfiguration.setDeviceState(matchState, true);
                Locale locale = localeList.get(localeIndex);
                mConfiguration.setLocale(locale, true);
                if (mUpdateUi) {
                    mConfigChooser.selectDeviceState(matchState);
                    mConfigChooser.selectLocale(locale);
                }
                mConfiguration.syncFolderConfig();
            } else {
                // no match in current device with any state/locale
                // attempt to find another device that can display this
                // particular state.
                findAndSetCompatibleConfig(currentConfigIsCompatible);
            }
        }
    }

    /**
     * Finds a device/config that can display a configuration.
     * <p>
     * Once found the device and config combos are set to the config.
     * <p>
     * If there is no compatible configuration, a custom one is created.
     *
     * @param favorCurrentConfig if true, and no best match is found, don't
     *            change the current config. This must only be true if the
     *            current config is compatible.
     */
    void findAndSetCompatibleConfig(boolean favorCurrentConfig) {
        List<Locale> localeList = mConfigChooser.getLocaleList();
        Collection<Device> devices = mConfigChooser.getDevices();
        FolderConfiguration editedConfig = mConfiguration.getEditedConfig();
        FolderConfiguration currentConfig = mConfiguration.getFullConfig();

        // list of compatible device/state/locale
        List<ConfigMatch> anyMatches = new ArrayList<ConfigMatch>();

        // list of actual best match (ie the file is a best match for the
        // device/state)
        List<ConfigMatch> bestMatches = new ArrayList<ConfigMatch>();

        // get a locale that match the host locale roughly (may not be exact match on the region.)
        int localeHostMatch = getLocaleMatch();

        // build a list of combinations of non standard qualifiers to add to each device's
        // qualifier set when testing for a match.
        // These qualifiers are: locale, night-mode, car dock.
        List<ConfigBundle> configBundles = new ArrayList<ConfigBundle>(200);

        // If the edited file has locales, then we have to select a matching locale from
        // the list.
        // However, if it doesn't, we don't randomly take the first locale, we take one
        // matching the current host locale (making sure it actually exist in the project)
        int start, max;
        if (editedConfig.getLocaleQualifier() != null || localeHostMatch == -1) {
            // add all the locales
            start = 0;
            max = localeList.size();
        } else {
            // only add the locale host match
            start = localeHostMatch;
            max = localeHostMatch + 1; // test is <
        }

        for (int i = start ; i < max ; i++) {
            Locale l = localeList.get(i);

            ConfigBundle bundle = new ConfigBundle();
            bundle.config.setLocaleQualifier(l.locale);

            bundle.localeIndex = i;
            configBundles.add(bundle);
        }

        // add the dock mode to the bundle combinations.
        addDockModeToBundles(configBundles);

        // add the night mode to the bundle combinations.
        addNightModeToBundles(configBundles);

        addRenderTargetToBundles(configBundles);

        for (Device device : devices) {
            for (State state : device.getAllStates()) {

                // loop on the list of config bundles to create full
                // configurations.
                FolderConfiguration stateConfig = DeviceConfigHelper.getFolderConfig(state);
                for (ConfigBundle bundle : configBundles) {
                    // create a new config with device config
                    FolderConfiguration testConfig = new FolderConfiguration();
                    testConfig.set(stateConfig);

                    // add on top of it, the extra qualifiers from the bundle
                    testConfig.add(bundle.config);

                    if (editedConfig.isMatchFor(testConfig)) {
                        // this is a basic match. record it in case we don't
                        // find a match
                        // where the edited file is a best config.
                        anyMatches.add(new ConfigMatch(testConfig, device, state, bundle));

                        if (isCurrentFileBestMatchFor(testConfig)) {
                            // this is what we want.
                            bestMatches.add(new ConfigMatch(testConfig, device, state, bundle));
                        }
                    }
                }
            }
        }

        if (bestMatches.size() == 0) {
            if (favorCurrentConfig) {
                // quick check
                if (!editedConfig.isMatchFor(currentConfig)) {
                    AndmoreAndroidPlugin.log(IStatus.ERROR,
                        "favorCurrentConfig can only be true if the current config is compatible");
                }

                // just display the warning
                AndmoreAndroidPlugin.printErrorToConsole(mEditedFile.getProject(),
                        String.format(
                                "'%1$s' is not a best match for any device/locale combination.",
                                editedConfig.toDisplayString()),
                        String.format(
                                "Displaying it with '%1$s'",
                                currentConfig.toDisplayString()));
            } else if (anyMatches.size() > 0) {
                // select the best device anyway.
                ConfigMatch match = selectConfigMatch(anyMatches);
                mConfiguration.setDevice(match.device, true);
                mConfiguration.setDeviceState(match.state, true);
                mConfiguration.setLocale(localeList.get(match.bundle.localeIndex), true);
                mConfiguration.setUiMode(UiMode.getByIndex(match.bundle.dockModeIndex), true);
                mConfiguration.setNightMode(NightMode.getByIndex(match.bundle.nightModeIndex),
                        true);

                if (mUpdateUi) {
                    mConfigChooser.selectDevice(mConfiguration.getDevice());
                    mConfigChooser.selectDeviceState(mConfiguration.getDeviceState());
                    mConfigChooser.selectLocale(mConfiguration.getLocale());
                }

                mConfiguration.syncFolderConfig();

                // TODO: display a better warning!
                AndmoreAndroidPlugin.printErrorToConsole(mEditedFile.getProject(),
                        String.format(
                                "'%1$s' is not a best match for any device/locale combination.",
                                editedConfig.toDisplayString()),
                        String.format(
                                "Displaying it with '%1$s' which is compatible, but will " +
                                "actually be displayed with another more specific version of " +
                                "the layout.",
                                currentConfig.toDisplayString()));

            } else {
                // TODO: there is no device/config able to display the layout, create one.
                // For the base config values, we'll take the first device and state,
                // and replace whatever qualifier required by the layout file.
            }
        } else {
            ConfigMatch match = selectConfigMatch(bestMatches);
            mConfiguration.setDevice(match.device, true);
            mConfiguration.setDeviceState(match.state, true);
            mConfiguration.setLocale(localeList.get(match.bundle.localeIndex), true);
            mConfiguration.setUiMode(UiMode.getByIndex(match.bundle.dockModeIndex), true);
            mConfiguration.setNightMode(NightMode.getByIndex(match.bundle.nightModeIndex), true);

            mConfiguration.syncFolderConfig();

            if (mUpdateUi) {
                mConfigChooser.selectDevice(mConfiguration.getDevice());
                mConfigChooser.selectDeviceState(mConfiguration.getDeviceState());
                mConfigChooser.selectLocale(mConfiguration.getLocale());
            }
        }
    }

    private void addRenderTargetToBundles(List<ConfigBundle> configBundles) {
        Pair<Locale, IAndroidTarget> state = Configuration.loadRenderState(mConfigChooser);
        if (state != null) {
            IAndroidTarget target = state.getSecond();
            if (target != null) {
                int apiLevel = target.getVersion().getApiLevel();
                for (ConfigBundle bundle : configBundles) {
                    bundle.config.setVersionQualifier(
                            new VersionQualifier(apiLevel));
                }
            }
        }
    }

    private void addDockModeToBundles(List<ConfigBundle> addConfig) {
        ArrayList<ConfigBundle> list = new ArrayList<ConfigBundle>();

        // loop on each item and for each, add all variations of the dock modes
        for (ConfigBundle bundle : addConfig) {
            int index = 0;
            for (UiMode mode : UiMode.values()) {
                ConfigBundle b = new ConfigBundle(bundle);
                b.config.setUiModeQualifier(new UiModeQualifier(mode));
                b.dockModeIndex = index++;
                list.add(b);
            }
        }

        addConfig.clear();
        addConfig.addAll(list);
    }

    private void addNightModeToBundles(List<ConfigBundle> addConfig) {
        ArrayList<ConfigBundle> list = new ArrayList<ConfigBundle>();

        // loop on each item and for each, add all variations of the night modes
        for (ConfigBundle bundle : addConfig) {
            int index = 0;
            for (NightMode mode : NightMode.values()) {
                ConfigBundle b = new ConfigBundle(bundle);
                b.config.setNightModeQualifier(new NightModeQualifier(mode));
                b.nightModeIndex = index++;
                list.add(b);
            }
        }

        addConfig.clear();
        addConfig.addAll(list);
    }

    private int getLocaleMatch() {
        java.util.Locale defaultLocale = java.util.Locale.getDefault();
        if (defaultLocale != null) {
            String currentLanguage = defaultLocale.getLanguage();
            String currentRegion = defaultLocale.getCountry();

            List<Locale> localeList = mConfigChooser.getLocaleList();
            final int count = localeList.size();
            for (int l = 0; l < count; l++) {
                Locale locale = localeList.get(l);
                LocaleQualifier localeQ = locale.locale;

                // there's always a ##/Other or ##/Any (which is the same, the region
                // contains FAKE_REGION_VALUE). If we don't find a perfect region match
                // we take the fake region. Since it's last in the list, this makes the
                // test easy.
                if (localeQ.getLanguage().equals(currentLanguage) && (localeQ.getRegion().equals(currentRegion) || localeQ.hasRegion())) { 
                    return l;
                }
            }

            // if no locale match the current local locale, it's likely that it is
            // the default one which is the last one.
            return count - 1;
        }

        return -1;
    }

    private ConfigMatch selectConfigMatch(List<ConfigMatch> matches) {
        // API 11-13: look for a x-large device
        Comparator<ConfigMatch> comparator = null;
        Sdk sdk = Sdk.getCurrent();
        if (sdk != null) {
            IAndroidTarget projectTarget = sdk.getTarget(mEditedFile.getProject());
            if (projectTarget != null) {
                int apiLevel = projectTarget.getVersion().getApiLevel();
                if (apiLevel >= 11 && apiLevel < 14) {
                    // TODO: Maybe check the compatible-screen tag in the manifest to figure out
                    // what kind of device should be used for display.
                    comparator = new TabletConfigComparator();
                }
            }
        }
        if (comparator == null) {
            // lets look for a high density device
            comparator = new PhoneConfigComparator();
        }
        Collections.sort(matches, comparator);

        // Look at the currently active editor to see if it's a layout editor, and if so,
        // look up its configuration and if the configuration is in our match list,
        // use it. This means we "preserve" the current configuration when you open
        // new layouts.
        IEditorPart activeEditor = AdtUtils.getActiveEditor();
        LayoutEditorDelegate delegate = LayoutEditorDelegate.fromEditor(activeEditor);
        if (delegate != null
                // (Only do this when the two files are in the same project)
                && delegate.getEditor().getProject() == mEditedFile.getProject()) {
            FolderConfiguration configuration = delegate.getGraphicalEditor().getConfiguration();
            if (configuration != null) {
                for (ConfigMatch match : matches) {
                    if (configuration.equals(match.testConfig)) {
                        return match;
                    }
                }
            }
        }

        // the list has been sorted so that the first item is the best config
        return matches.get(0);
    }

    /** Return the default render target to use, or null if no strong preference */
    @Nullable
    static IAndroidTarget findDefaultRenderTarget(ConfigurationChooser chooser) {
        if (PREFER_RECENT_RENDER_TARGETS) {
            // Use the most recent target
            List<IAndroidTarget> targetList = chooser.getTargetList();
            if (!targetList.isEmpty()) {
                return targetList.get(targetList.size() - 1);
            }
        }

        IProject project = chooser.getProject();
        // Default to layoutlib version 5
        Sdk current = Sdk.getCurrent();
        if (current != null) {
            IAndroidTarget projectTarget = current.getTarget(project);
            int minProjectApi = Integer.MAX_VALUE;
            if (projectTarget != null) {
                if (!projectTarget.isPlatform() && projectTarget.hasRenderingLibrary()) {
                    // Renderable non-platform targets are all going to be adequate (they
                    // will have at least version 5 of layoutlib) so use the project
                    // target as the render target.
                    return projectTarget;
                }

                if (projectTarget.getVersion().isPreview()
                        && projectTarget.hasRenderingLibrary()) {
                    // If the project target is a preview version, then just use it
                    return projectTarget;
                }

                minProjectApi = projectTarget.getVersion().getApiLevel();
            }

            // We want to pick a render target that contains at least version 5 (and
            // preferably version 6) of the layout library. To do this, we go through the
            // targets and pick the -smallest- API level that is both simultaneously at
            // least as big as the project API level, and supports layoutlib level 5+.
            IAndroidTarget best = null;
            int bestApiLevel = Integer.MAX_VALUE;

            for (IAndroidTarget target : current.getTargets()) {
                // Non-platform targets are not chosen as the default render target
                if (!target.isPlatform()) {
                    continue;
                }

                int apiLevel = target.getVersion().getApiLevel();

                // Ignore targets that have a lower API level than the minimum project
                // API level:
                if (apiLevel < minProjectApi) {
                    continue;
                }

                // Look up the layout lib API level. This property is new so it will only
                // be defined for version 6 or higher, which means non-null is adequate
                // to see if this target is eligible:
                String property = target.getProperty(PkgProps.LAYOUTLIB_API);
                // In addition, Android 3.0 with API level 11 had version 5.0 which is adequate:
                if (property != null || apiLevel >= 11) {
                    if (apiLevel < bestApiLevel) {
                        bestApiLevel = apiLevel;
                        best = target;
                    }
                }
            }

            return best;
        }

        return null;
    }

    /**
     * Attempts to find a close state among a list
     *
     * @param oldConfig the reference config.
     * @param states the list of states to search through
     * @return the name of the closest state match, or possibly null if no states are compatible
     * (this can only happen if the states don't have a single qualifier that is the same).
     */
    @Nullable
    static String getClosestMatch(@NonNull FolderConfiguration oldConfig,
            @NonNull List<State> states) {

        // create 2 lists as we're going to go through one and put the
        // candidates in the other.
        List<State> list1 = new ArrayList<State>(states.size());
        List<State> list2 = new ArrayList<State>(states.size());

        list1.addAll(states);

        final int count = FolderConfiguration.getQualifierCount();
        for (int i = 0 ; i < count ; i++) {
            // compute the new candidate list by only taking states that have
            // the same i-th qualifier as the old state
            for (State s : list1) {
                ResourceQualifier oldQualifier = oldConfig.getQualifier(i);

                FolderConfiguration folderConfig = DeviceConfigHelper.getFolderConfig(s);
                ResourceQualifier newQualifier =
                        folderConfig != null ? folderConfig.getQualifier(i) : null;

                if (oldQualifier == null) {
                    if (newQualifier == null) {
                        list2.add(s);
                    }
                } else if (oldQualifier.equals(newQualifier)) {
                    list2.add(s);
                }
            }

            // at any moment if the new candidate list contains only one match, its name
            // is returned.
            if (list2.size() == 1) {
                return list2.get(0).getName();
            }

            // if the list is empty, then all the new states failed. It is considered ok, and
            // we move to the next qualifier anyway. This way, if a qualifier is different for
            // all new states it is simply ignored.
            if (list2.size() != 0) {
                // move the candidates back into list1.
                list1.clear();
                list1.addAll(list2);
                list2.clear();
            }
        }

        // the only way to reach this point is if there's an exact match.
        // (if there are more than one, then there's a duplicate state and it doesn't matter,
        // we take the first one).
        if (list1.size() > 0) {
            return list1.get(0).getName();
        }

        return null;
    }

    /**
     * Returns the layout {@link IFile} which best matches the configuration
     * selected in the given configuration chooser.
     *
     * @param chooser the associated configuration chooser holding project state
     * @return the file which best matches the settings
     */
    @Nullable
    public static IFile getBestFileMatch(ConfigurationChooser chooser) {
        // get the resources of the file's project.
        ResourceManager manager = ResourceManager.getInstance();
        ProjectResources resources = manager.getProjectResources(chooser.getProject());
        if (resources == null) {
            return null;
        }

        // From the resources, look for a matching file
        IFile editedFile = chooser.getEditedFile();
        if (editedFile == null) {
            return null;
        }
        String name = editedFile.getName();
        FolderConfiguration config = chooser.getConfiguration().getFullConfig();
        ResourceFile match = resources.getMatchingFile(name, ResourceType.LAYOUT, config);

        if (match != null) {
            // In Eclipse, the match's file is always an instance of IFileWrapper
            return ((IFileWrapper) match.getFile()).getIFile();
        }

        return null;
    }

    /**
     * Note: this comparator imposes orderings that are inconsistent with equals.
     */
    private static class TabletConfigComparator implements Comparator<ConfigMatch> {
        @Override
        public int compare(ConfigMatch o1, ConfigMatch o2) {
            FolderConfiguration config1 = o1 != null ? o1.testConfig : null;
            FolderConfiguration config2 = o2 != null ? o2.testConfig : null;
            if (config1 == null) {
                if (config2 == null) {
                    return 0;
                } else {
                    return -1;
                }
            } else if (config2 == null) {
                return 1;
            }

            ScreenSizeQualifier size1 = config1.getScreenSizeQualifier();
            ScreenSizeQualifier size2 = config2.getScreenSizeQualifier();
            ScreenSize ss1 = size1 != null ? size1.getValue() : ScreenSize.NORMAL;
            ScreenSize ss2 = size2 != null ? size2.getValue() : ScreenSize.NORMAL;

            // X-LARGE is better than all others (which are considered identical)
            // if both X-LARGE, then LANDSCAPE is better than all others (which are identical)

            if (ss1 == ScreenSize.XLARGE) {
                if (ss2 == ScreenSize.XLARGE) {
                    ScreenOrientationQualifier orientation1 =
                            config1.getScreenOrientationQualifier();
                    ScreenOrientation so1 = orientation1.getValue();
                    if (so1 == null) {
                        so1 = ScreenOrientation.PORTRAIT;
                    }
                    ScreenOrientationQualifier orientation2 =
                            config2.getScreenOrientationQualifier();
                    ScreenOrientation so2 = orientation2.getValue();
                    if (so2 == null) {
                        so2 = ScreenOrientation.PORTRAIT;
                    }

                    if (so1 == ScreenOrientation.LANDSCAPE) {
                        if (so2 == ScreenOrientation.LANDSCAPE) {
                            return 0;
                        } else {
                            return -1;
                        }
                    } else if (so2 == ScreenOrientation.LANDSCAPE) {
                        return 1;
                    } else {
                        return 0;
                    }
                } else {
                    return -1;
                }
            } else if (ss2 == ScreenSize.XLARGE) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    /**
     * Note: this comparator imposes orderings that are inconsistent with equals.
     */
    private static class PhoneConfigComparator implements Comparator<ConfigMatch> {

        private SparseIntArray mDensitySort = new SparseIntArray(4);

        public PhoneConfigComparator() {
            // put the sort order for the density.
            mDensitySort.put(Density.HIGH.getDpiValue(),   1);
            mDensitySort.put(Density.MEDIUM.getDpiValue(), 2);
            mDensitySort.put(Density.XHIGH.getDpiValue(),  3);
            mDensitySort.put(Density.LOW.getDpiValue(),    4);
        }

        @Override
        public int compare(ConfigMatch o1, ConfigMatch o2) {
            FolderConfiguration config1 = o1 != null ? o1.testConfig : null;
            FolderConfiguration config2 = o2 != null ? o2.testConfig : null;
            if (config1 == null) {
                if (config2 == null) {
                    return 0;
                } else {
                    return -1;
                }
            } else if (config2 == null) {
                return 1;
            }

            int dpi1 = Density.DEFAULT_DENSITY;
            int dpi2 = Density.DEFAULT_DENSITY;

            DensityQualifier dpiQualifier1 = config1.getDensityQualifier();
            if (dpiQualifier1 != null) {
                Density value = dpiQualifier1.getValue();
                dpi1 = value != null ? value.getDpiValue() : Density.DEFAULT_DENSITY;
            }
            dpi1 = mDensitySort.get(dpi1, 100 /* valueIfKeyNotFound*/);

            DensityQualifier dpiQualifier2 = config2.getDensityQualifier();
            if (dpiQualifier2 != null) {
                Density value = dpiQualifier2.getValue();
                dpi2 = value != null ? value.getDpiValue() : Density.DEFAULT_DENSITY;
            }
            dpi2 = mDensitySort.get(dpi2, 100 /* valueIfKeyNotFound*/);

            if (dpi1 == dpi2) {
                // portrait is better
                ScreenOrientation so1 = ScreenOrientation.PORTRAIT;
                ScreenOrientationQualifier orientationQualifier1 =
                        config1.getScreenOrientationQualifier();
                if (orientationQualifier1 != null) {
                    so1 = orientationQualifier1.getValue();
                    if (so1 == null) {
                        so1 = ScreenOrientation.PORTRAIT;
                    }
                }
                ScreenOrientation so2 = ScreenOrientation.PORTRAIT;
                ScreenOrientationQualifier orientationQualifier2 =
                        config2.getScreenOrientationQualifier();
                if (orientationQualifier2 != null) {
                    so2 = orientationQualifier2.getValue();
                    if (so2 == null) {
                        so2 = ScreenOrientation.PORTRAIT;
                    }
                }

                if (so1 == ScreenOrientation.PORTRAIT) {
                    if (so2 == ScreenOrientation.PORTRAIT) {
                        return 0;
                    } else {
                        return -1;
                    }
                } else if (so2 == ScreenOrientation.PORTRAIT) {
                    return 1;
                } else {
                    return 0;
                }
            }

            return dpi1 - dpi2;
        }
    }
}

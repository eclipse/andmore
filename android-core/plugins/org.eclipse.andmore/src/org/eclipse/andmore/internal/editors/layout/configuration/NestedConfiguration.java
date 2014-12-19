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

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.resources.configuration.FolderConfiguration;
import com.android.resources.NightMode;
import com.android.resources.UiMode;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.devices.Device;
import com.android.sdklib.devices.State;
import com.google.common.base.Objects;

/**
 * An {@linkplain NestedConfiguration} is a {@link Configuration} which inherits
 * all of its values from a different configuration, except for one or more
 * attributes where it overrides a custom value.
 * <p>
 * Unlike a {@link VaryingConfiguration}, a {@linkplain NestedConfiguration}
 * will always return the same overridden value, regardless of the inherited
 * value.
 * <p>
 * For example, an {@linkplain NestedConfiguration} may fix the locale to always
 * be "en", but otherwise inherit everything else.
 */
public class NestedConfiguration extends Configuration {
    /** The configuration we are inheriting non-overridden values from */
    protected Configuration mParent;

    /** Bitmask of attributes to be overridden in this configuration */
    private int mOverride;

    /**
     * Constructs a new {@linkplain NestedConfiguration}.
     * Construct via
     *
     * @param chooser the associated chooser
     * @param configuration the configuration to inherit from
     */
    protected NestedConfiguration(
            @NonNull ConfigurationChooser chooser,
            @NonNull Configuration configuration) {
        super(chooser);
        mParent = configuration;

        mFullConfig.set(mParent.mFullConfig);
        if (mParent.getEditedConfig() != null) {
            mEditedConfig = new FolderConfiguration();
            mEditedConfig.set(mParent.mEditedConfig);
        }
    }

    /**
     * Returns the override flags for this configuration. Corresponds to
     * the {@code CFG_} flags in {@link ConfigurationClient}.
     *
     * @return the bitmask
     */
    public int getOverrideFlags() {
        return mOverride;
    }

    /**
     * Creates a new {@linkplain NestedConfiguration} that has the same overriding
     * attributes as the given other {@linkplain NestedConfiguration}, and gets
     * its values from the given {@linkplain Configuration}.
     *
     * @param other the configuration to copy overrides from
     * @param values the configuration to copy values from
     * @param parent the parent to tie the configuration to for inheriting values
     * @return a new configuration
     */
    @NonNull
    public static NestedConfiguration create(
            @NonNull NestedConfiguration other,
            @NonNull Configuration values,
            @NonNull Configuration parent) {
        NestedConfiguration configuration =
                new NestedConfiguration(other.mConfigChooser, parent);
        initFrom(configuration, other, values, true /*sync*/);
        return configuration;
    }

    /**
     * Initializes a new {@linkplain NestedConfiguration} with the overriding
     * attributes as the given other {@linkplain NestedConfiguration}, and gets
     * its values from the given {@linkplain Configuration}.
     *
     * @param configuration the configuration to initialize
     * @param other the configuration to copy overrides from
     * @param values the configuration to copy values from
     * @param sync if true, sync the folder configuration from
     */
    protected static void initFrom(NestedConfiguration configuration,
            NestedConfiguration other, Configuration values, boolean sync) {
        configuration.mOverride = other.mOverride;
        configuration.setDisplayName(values.getDisplayName());
        configuration.setActivity(values.getActivity());

        if (configuration.isOverridingLocale()) {
            configuration.setLocale(values.getLocale(), true);
        }
        if (configuration.isOverridingTarget()) {
            configuration.setTarget(values.getTarget(), true);
        }
        if (configuration.isOverridingDevice()) {
            configuration.setDevice(values.getDevice(), true);
        }
        if (configuration.isOverridingDeviceState()) {
            configuration.setDeviceState(values.getDeviceState(), true);
        }
        if (configuration.isOverridingNightMode()) {
            configuration.setNightMode(values.getNightMode(), true);
        }
        if (configuration.isOverridingUiMode()) {
            configuration.setUiMode(values.getUiMode(), true);
        }
        if (sync) {
            configuration.syncFolderConfig();
        }
    }

    /**
     * Sets the parent configuration that this configuration is inheriting from.
     *
     * @param parent the parent configuration
     */
    public void setParent(@NonNull Configuration parent) {
        mParent = parent;
    }

    /**
     * Creates a new {@linkplain Configuration} which inherits values from the
     * given parent {@linkplain Configuration}, possibly overriding some as
     * well.
     *
     * @param chooser the associated chooser
     * @param parent the configuration to inherit values from
     * @return a new configuration
     */
    @NonNull
    public static NestedConfiguration create(@NonNull ConfigurationChooser chooser,
            @NonNull Configuration parent) {
        return new NestedConfiguration(chooser, parent);
    }

    @Override
    @Nullable
    public String getTheme() {
        // Never overridden: this is a static attribute of a layout, not something which
        // varies by configuration or at runtime
        return mParent.getTheme();
    }

    @Override
    public void setTheme(String theme) {
        // Never overridden
        mParent.setTheme(theme);
    }

    /**
     * Sets whether the locale should be overridden by this configuration
     *
     * @param override if true, override the inherited value
     */
    public void setOverrideLocale(boolean override) {
        mOverride |= CFG_LOCALE;
    }

    /**
     * Returns true if the locale is overridden
     *
     * @return true if the locale is overridden
     */
    public final boolean isOverridingLocale() {
        return (mOverride & CFG_LOCALE) != 0;
    }

    @Override
    @NonNull
    public Locale getLocale() {
        if (isOverridingLocale()) {
            return super.getLocale();
        } else {
            return mParent.getLocale();
        }
    }

    @Override
    public void setLocale(@NonNull Locale locale, boolean skipSync) {
        if (isOverridingLocale()) {
            super.setLocale(locale, skipSync);
        } else {
            mParent.setLocale(locale, skipSync);
        }
    }

    /**
     * Sets whether the rendering target should be overridden by this configuration
     *
     * @param override if true, override the inherited value
     */
    public void setOverrideTarget(boolean override) {
        mOverride |= CFG_TARGET;
    }

    /**
     * Returns true if the target is overridden
     *
     * @return true if the target is overridden
     */
    public final boolean isOverridingTarget() {
        return (mOverride & CFG_TARGET) != 0;
    }

    @Override
    @Nullable
    public IAndroidTarget getTarget() {
        if (isOverridingTarget()) {
            return super.getTarget();
        } else {
            return mParent.getTarget();
        }
    }

    @Override
    public void setTarget(IAndroidTarget target, boolean skipSync) {
        if (isOverridingTarget()) {
            super.setTarget(target, skipSync);
        } else {
            mParent.setTarget(target, skipSync);
        }
    }

    /**
     * Sets whether the device should be overridden by this configuration
     *
     * @param override if true, override the inherited value
     */
    public void setOverrideDevice(boolean override) {
        mOverride |= CFG_DEVICE;
    }

    /**
     * Returns true if the device is overridden
     *
     * @return true if the device is overridden
     */
    public final boolean isOverridingDevice() {
        return (mOverride & CFG_DEVICE) != 0;
    }

    @Override
    @Nullable
    public Device getDevice() {
        if (isOverridingDevice()) {
            return super.getDevice();
        } else {
            return mParent.getDevice();
        }
    }

    @Override
    public void setDevice(Device device, boolean skipSync) {
        if (isOverridingDevice()) {
            super.setDevice(device, skipSync);
        } else {
            mParent.setDevice(device, skipSync);
        }
    }

    /**
     * Sets whether the device state should be overridden by this configuration
     *
     * @param override if true, override the inherited value
     */
    public void setOverrideDeviceState(boolean override) {
        mOverride |= CFG_DEVICE_STATE;
    }

    /**
     * Returns true if the device state is overridden
     *
     * @return true if the device state is overridden
     */
    public final boolean isOverridingDeviceState() {
        return (mOverride & CFG_DEVICE_STATE) != 0;
    }

    @Override
    @Nullable
    public State getDeviceState() {
        if (isOverridingDeviceState()) {
            return super.getDeviceState();
        } else {
            State state = mParent.getDeviceState();
            if (isOverridingDevice()) {
                // If the device differs, I need to look up a suitable equivalent state
                // on our device
                if (state != null) {
                    Device device = super.getDevice();
                    if (device != null) {
                        return device.getState(state.getName());
                    }
                }
            }

            return state;
        }
    }

    @Override
    public void setDeviceState(State state, boolean skipSync) {
        if (isOverridingDeviceState()) {
            super.setDeviceState(state, skipSync);
        } else {
            if (isOverridingDevice()) {
                Device device = super.getDevice();
                if (device != null) {
                    State equivalentState = device.getState(state.getName());
                    if (equivalentState != null) {
                        state = equivalentState;
                    }
                }
            }
            mParent.setDeviceState(state, skipSync);
        }
    }

    /**
     * Sets whether the night mode should be overridden by this configuration
     *
     * @param override if true, override the inherited value
     */
    public void setOverrideNightMode(boolean override) {
        mOverride |= CFG_NIGHT_MODE;
    }

    /**
     * Returns true if the night mode is overridden
     *
     * @return true if the night mode is overridden
     */
    public final boolean isOverridingNightMode() {
        return (mOverride & CFG_NIGHT_MODE) != 0;
    }

    @Override
    @NonNull
    public NightMode getNightMode() {
        if (isOverridingNightMode()) {
            return super.getNightMode();
        } else {
            return mParent.getNightMode();
        }
    }

    @Override
    public void setNightMode(@NonNull NightMode night, boolean skipSync) {
        if (isOverridingNightMode()) {
            super.setNightMode(night, skipSync);
        } else {
            mParent.setNightMode(night, skipSync);
        }
    }

    /**
     * Sets whether the UI mode should be overridden by this configuration
     *
     * @param override if true, override the inherited value
     */
    public void setOverrideUiMode(boolean override) {
        mOverride |= CFG_UI_MODE;
    }

    /**
     * Returns true if the UI mode is overridden
     *
     * @return true if the UI mode is overridden
     */
    public final boolean isOverridingUiMode() {
        return (mOverride & CFG_UI_MODE) != 0;
    }

    @Override
    @NonNull
    public UiMode getUiMode() {
        if (isOverridingUiMode()) {
            return super.getUiMode();
        } else {
            return mParent.getUiMode();
        }
    }

    @Override
    public void setUiMode(@NonNull UiMode uiMode, boolean skipSync) {
        if (isOverridingUiMode()) {
            super.setUiMode(uiMode, skipSync);
        } else {
            mParent.setUiMode(uiMode, skipSync);
        }
    }

    /**
     * Returns the configuration this {@linkplain NestedConfiguration} is
     * inheriting from
     *
     * @return the configuration this configuration is inheriting from
     */
    @NonNull
    public Configuration getParent() {
        return mParent;
    }

    @Override
    @Nullable
    public String getActivity() {
        return mParent.getActivity();
    }

    @Override
    public void setActivity(String activity) {
        super.setActivity(activity);
    }

    /**
     * Returns a computed display name (ignoring the value stored by
     * {@link #setDisplayName(String)}) by looking at the override flags
     * and picking a suitable name.
     *
     * @return a suitable display name
     */
    @Nullable
    public String computeDisplayName() {
        return computeDisplayName(mOverride, this);
    }

    /**
     * Computes a display name for the given configuration, using the given
     * override flags (which correspond to the {@code CFG_} constants in
     * {@link ConfigurationClient}
     *
     * @param flags the override bitmask
     * @param configuration the configuration to fetch values from
     * @return a suitable display name
     */
    @Nullable
    public static String computeDisplayName(int flags, @NonNull Configuration configuration) {
        if ((flags & CFG_LOCALE) != 0) {
            return ConfigurationChooser.getLocaleLabel(configuration.mConfigChooser,
                    configuration.getLocale(), false);
        }

        if ((flags & CFG_TARGET) != 0) {
            return ConfigurationChooser.getRenderingTargetLabel(configuration.getTarget(), false);
        }

        if ((flags & CFG_DEVICE) != 0) {
            return ConfigurationChooser.getDeviceLabel(configuration.getDevice(), true);
        }

        if ((flags & CFG_DEVICE_STATE) != 0) {
            State deviceState = configuration.getDeviceState();
            if (deviceState != null) {
                return deviceState.getName();
            }
        }

        if ((flags & CFG_NIGHT_MODE) != 0) {
            return configuration.getNightMode().getLongDisplayValue();
        }

        if ((flags & CFG_UI_MODE) != 0) {
            configuration.getUiMode().getLongDisplayValue();
        }

        return null;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this.getClass())
                .add("parent", mParent.getDisplayName())          //$NON-NLS-1$
                .add("display", getDisplayName())                 //$NON-NLS-1$
                .add("overrideLocale", isOverridingLocale())           //$NON-NLS-1$
                .add("overrideTarget", isOverridingTarget())           //$NON-NLS-1$
                .add("overrideDevice", isOverridingDevice())           //$NON-NLS-1$
                .add("overrideDeviceState", isOverridingDeviceState()) //$NON-NLS-1$
                .add("persistent", toPersistentString())          //$NON-NLS-1$
                .toString();
    }
}

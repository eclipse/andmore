/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.sdkuilib.internal.widgets;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.prefs.AndroidLocation.AndroidLocationException;
import com.android.resources.Density;
import com.android.resources.ScreenSize;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.ISystemImage;
import com.android.sdklib.SystemImage;
import com.android.sdklib.devices.Camera;
import com.android.sdklib.devices.CameraLocation;
import com.android.sdklib.devices.Device;
import com.android.sdklib.devices.DeviceManager;
import com.android.sdklib.devices.Hardware;
import com.android.sdklib.devices.Screen;
import com.android.sdklib.devices.Software;
import com.android.sdklib.devices.Storage;
import com.android.sdklib.internal.avd.AvdInfo;
import com.android.sdklib.internal.avd.AvdManager;
import com.android.sdklib.internal.avd.AvdManager.AvdConflict;
import com.android.sdklib.internal.avd.HardwareProperties;
import com.android.sdklib.repository.descriptors.IdDisplay;
import com.android.sdklib.repository.local.LocalSdk;
import com.android.utils.ILogger;
import com.android.utils.Pair;
import com.google.common.base.Joiner;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implements all the logic of the {@link AvdCreationDialog}.
 * <p/>
 * Implementation detail: the dialog is split using a simple MVP pattern. <br/>
 * This code, the presenter, handles all the logic including event handling and controls
 * what is displayed in the dialog. It does not directly manipulate the UI and in fact the
 * code has no swt import. Instead the constructor is given a {@link IWidgetAdapter}
 * implementation. This makes it possible to test the <em>logic</em> of the dialog as-is
 * in a unit test by simply passing a different {@link IWidgetAdapter} implementation to the
 * constructor.
 */
class AvdCreationPresenter {

    @NonNull
    private IWidgetAdapter mWidgets;
    private AvdManager mAvdManager;
    private ILogger mSdkLog;
    private AvdInfo mAvdInfo;

    private final TreeMap<String, IAndroidTarget> mCurrentTargets =
            new TreeMap<String, IAndroidTarget>();

    private static final AvdSkinChoice SKIN_DYNAMIC =
        new AvdSkinChoice(SkinType.DYNAMIC, "Skin with dynamic hardware controls");
    private static final AvdSkinChoice SKIN_NONE =
        new AvdSkinChoice(SkinType.NONE, "No skin");

    private final List<Device> mComboDevices = new ArrayList<Device>();
    private final List<AvdSkinChoice> mComboSkins = new ArrayList<AvdSkinChoice>();
    private final List<ISystemImage> mComboSystemImages = new ArrayList<ISystemImage>();
    private final List<IAndroidTarget> mComboTargets = new ArrayList<IAndroidTarget>();

    private Device mInitWithDevice;
    private AvdInfo mCreatedAvd;

    public enum Ctrl {
        BUTTON_OK,
        BUTTON_BROWSE_SDCARD,

        COMBO_DEVICE,
        COMBO_TARGET,
        COMBO_TAG_ABI,
        COMBO_SKIN,
        COMBO_FRONT_CAM,
        COMBO_BACK_CAM,
        COMBO_DATA_PART_SIZE,
        COMBO_SDCARD_SIZE,

        CHECK_FORCE_CREATION,
        CHECK_KEYBOARD,
        CHECK_SNAPSHOT,
        CHECK_GPU_EMUL,
        RADIO_SDCARD_SIZE,
        RADIO_SDCARD_FILE,

        TEXT_AVD_NAME,
        TEXT_RAM,
        TEXT_VM_HEAP,
        TEXT_DATA_PART,
        TEXT_SDCARD_SIZE,
        TEXT_SDCARD_FILE,

        ICON_STATUS,
        TEXT_STATUS,
    }

    /** Interface exposed by the view. Presenter calls this to update UI. */
    public interface IWidgetAdapter {
        void    setTitle(@NonNull String title);
        int     getComboIndex(@NonNull Ctrl ctrl);
        int     getComboSize (@NonNull Ctrl ctrl);
        void    selectComboIndex(@NonNull Ctrl ctrl, int index);
        String  getComboItem (@NonNull Ctrl ctrl, int index);
        void    addComboItem (@NonNull Ctrl ctrl, String label);
        /** Set combo labels or clear combo if labels is null. */
        void    setComboItems(@NonNull Ctrl ctrl, @Nullable String[] labels);
        boolean isEnabled(@NonNull Ctrl ctrl);
        void    setEnabled(@NonNull Ctrl ctrl, boolean enabled);
        boolean isChecked(@NonNull Ctrl ctrl);
        void    setChecked(@NonNull Ctrl ctrl, boolean checked);
        String  getText(@NonNull Ctrl ctrl);
        void    setText(@NonNull Ctrl ctrl, String text);
        void    setImage(@NonNull Ctrl ctrl, @Nullable String imageName);
        String  openFileDialog(@NonNull String string);
        void    repack();

        /**
         * Creates a MessageBoxLog, a special MessageBox that returns a logger instance
         * to capture logs. The caller then performs actions and outputs to the logger
         * and at the end calls MessageBoxLog.displayResult() to create a message box
         * with the result of the logged output.
         */
        IMessageBoxLogger newDelayedMessageBoxLog(String title, boolean logErrorsOnly);

    }

    public AvdCreationPresenter(@NonNull  AvdManager avdManager,
                                @NonNull  ILogger    log,
                                @Nullable AvdInfo    editAvdInfo) {
        mAvdManager = avdManager;
        mSdkLog     = log;
        mAvdInfo    = editAvdInfo;
    }

    /** Returns the AVD Created, if successful. */
    public AvdInfo getCreatedAvd() {
        return mCreatedAvd;
    }

    /** Called by the view constructor to set the view updater. */
    public void setWidgetAdapter(@NonNull IWidgetAdapter widgetAdapter) {
        mWidgets = widgetAdapter;
    }

    /**
     * Can be called after the constructor to set the default device for this AVD.
     * Useful especially for new AVDs.
     */
    public void selectInitialDevice(@NonNull Device device) {
        mInitWithDevice = device;
    }

    public void onViewInit() {
        mWidgets.setTitle(
                mAvdInfo == null ? "Create new Android Virtual Device (AVD)"
                                 : "Edit Android Virtual Device (AVD)");
        initializeDevices();

        // setup the 2 default choices (no skin, dynamic skin); do not select any right now.
        mComboSkins.add(SKIN_DYNAMIC);
        mComboSkins.add(SKIN_NONE);
        Collections.sort(mComboSkins);
        mWidgets.addComboItem(Ctrl.COMBO_SKIN, mComboSkins.get(0).getLabel());
        mWidgets.addComboItem(Ctrl.COMBO_SKIN, mComboSkins.get(1).getLabel());

        // Preload target combo *after* ABI/Tag and Skin combos have been setup as
        // they will be setup depending on the selected target.
        preloadTargetCombo();

        toggleCameras();

        enableSdCardWidgets(true);


        if (mAvdInfo != null) {
            fillExistingAvdInfo(mAvdInfo);
        } else if (mInitWithDevice != null) {
            fillInitialDeviceInfo(mInitWithDevice);
        }

        validatePage();
    }

    //-------



    private void initializeDevices() {
        LocalSdk localSdk = mAvdManager.getLocalSdk();
        File location = localSdk.getLocation();
        if (location != null) {
            DeviceManager deviceManager = DeviceManager.createInstance(location, mSdkLog);
            Collection<Device> deviceList = deviceManager.getDevices(DeviceManager.ALL_DEVICES);

            // Sort
            List<Device> nexus = new ArrayList<Device>(deviceList.size());
            List<Device> other = new ArrayList<Device>(deviceList.size());
            for (Device device : deviceList) {
                if (isNexus(device) && !isGeneric(device)) {
                    nexus.add(device);
                } else {
                    other.add(device);
                }
            }
            Collections.reverse(other);
            Collections.sort(nexus, new Comparator<Device>() {
                @Override
                public int compare(Device device1, Device device2) {
                    // Descending order of age
                    return nexusRank(device2) - nexusRank(device1);
                }
            });

            mComboDevices.clear();
            mComboDevices.addAll(nexus);
            mComboDevices.addAll(other);

            String[] labels = new String[mComboDevices.size()];
            for (int i = 0, n = mComboDevices.size(); i < n; i++) {
                Device device = mComboDevices.get(i);
                if (isNexus(device) && !isGeneric(device)) {
                    labels[i] = getNexusLabel(device);
                } else {
                    labels[i] = getGenericLabel(device);
                }
            }

            mWidgets.setComboItems(Ctrl.COMBO_DEVICE, labels);
        }
    }

    @Nullable
    private Device getSelectedDevice() {
        int index = mWidgets.getComboIndex(Ctrl.COMBO_DEVICE);
        if (index != -1 && index < mComboDevices.size()) {
            return mComboDevices.get(index);
        }

        return null;
    }

    /** Called by fillExisting/InitialDeviceInfo to select the device in the combo list. */
    @SuppressWarnings("deprecation")
    private void selectDevice(String manufacturer, String name) {
        for (int i = 0, n = mComboDevices.size(); i < n; i++) {
            Device device = mComboDevices.get(i);
            if (device.getManufacturer().equals(manufacturer)
                    && device.getName().equals(name)) {
                mWidgets.selectComboIndex(Ctrl.COMBO_DEVICE, i);
                break;
            }
        }
    }

    /** Called by fillExisting/InitialDeviceInfo to select the device in the combo list. */
    private void selectDevice(Device device) {
        for (int i = 0, n = mComboDevices.size(); i < n; i++) {
            if (mComboDevices.get(i).equals(device)) {
                mWidgets.selectComboIndex(Ctrl.COMBO_DEVICE, i);
                break;
            }
        }
    }

    void onDeviceComboChanged() {
        Device currentDevice = getSelectedDevice();
        if (currentDevice != null) {
            fillDeviceProperties(currentDevice);
        }

        toggleCameras();
        validatePage();
    }

    void onAvdNameModified() {
        String name = mWidgets.getText(Ctrl.TEXT_AVD_NAME).trim();
        if (mAvdInfo == null || !name.equals(mAvdInfo.getName())) {
            // Case where we're creating a new AVD or editing an existing one
            // and the AVD name has been changed... check for name uniqueness.

            Pair<AvdConflict, String> conflict = mAvdManager.isAvdNameConflicting(name);
            if (conflict.getFirst() != AvdManager.AvdConflict.NO_CONFLICT) {
                // If we're changing the state from disabled to enabled, make sure
                // to uncheck the button, to force the user to voluntarily re-enforce it.
                // This happens when editing an existing AVD and changing the name from
                // the existing AVD to another different existing AVD.
                if (!mWidgets.isEnabled(Ctrl.CHECK_FORCE_CREATION)) {
                    mWidgets.setEnabled(Ctrl.CHECK_FORCE_CREATION, true);
                    mWidgets.setChecked(Ctrl.CHECK_FORCE_CREATION, false);
                }
            } else {
                mWidgets.setEnabled(Ctrl.CHECK_FORCE_CREATION, false);
                mWidgets.setChecked(Ctrl.CHECK_FORCE_CREATION, false);
            }
        } else {
            // Case where we're editing an existing AVD with the name unchanged.
            mWidgets.setEnabled(Ctrl.CHECK_FORCE_CREATION, false);
            mWidgets.setChecked(Ctrl.CHECK_FORCE_CREATION, false);
        }
        validatePage();

    }



    private void fillDeviceProperties(Device device) {
        Hardware hw = device.getDefaultHardware();
        Long ram = hw.getRam().getSizeAsUnit(Storage.Unit.MiB);
        mWidgets.setText(Ctrl.TEXT_RAM, Long.toString(ram));

        // Set the default VM heap size. This is based on the Android CDD minimums for each
        // screen size and density.
        Screen s = hw.getScreen();
        ScreenSize size = s.getSize();
        Density density = s.getPixelDensity();
        int vmHeapSize = 32;
        if (size.equals(ScreenSize.XLARGE)) {
            switch (density) {
                case LOW:
                case MEDIUM:
                    vmHeapSize = 32;
                    break;
                case TV:
                case HIGH:
                    vmHeapSize = 64;
                    break;
                case XHIGH:
                case XXHIGH:
                case XXXHIGH:
                    vmHeapSize = 128;
                break;
                case NODPI:
                    break;
            }
        } else {
            switch (density) {
                case LOW:
                case MEDIUM:
                    vmHeapSize = 16;
                    break;
                case TV:
                case HIGH:
                    vmHeapSize = 32;
                    break;
                case XHIGH:
                case XXHIGH:
                case XXXHIGH:
                    vmHeapSize = 64;
                break;
                case NODPI:
                    break;
            }
        }
        mWidgets.setText(Ctrl.TEXT_VM_HEAP, Integer.toString(vmHeapSize));

        boolean reloadTabAbiCombo = false;

        List<Software> allSoftware = device.getAllSoftware();
        if (allSoftware != null && !allSoftware.isEmpty()) {
            Software first = allSoftware.get(0);
            int min = first.getMinSdkLevel();;
            int max = first.getMaxSdkLevel();;
            for (int i = 1; i < allSoftware.size(); i++) {
                min = Math.min(min, first.getMinSdkLevel());
                max = Math.max(max, first.getMaxSdkLevel());
            }
            if (mCurrentTargets != null) {
                int bestApiLevel = Integer.MAX_VALUE;
                IAndroidTarget bestTarget = null;
                for (IAndroidTarget target : mCurrentTargets.values()) {
                    if (!target.isPlatform()) {
                        continue;
                    }
                    int apiLevel = target.getVersion().getApiLevel();
                    if (apiLevel >= min && apiLevel <= max) {
                        if (bestTarget == null || apiLevel < bestApiLevel) {
                            bestTarget = target;
                            bestApiLevel = apiLevel;
                        }
                    }
                }

                if (bestTarget != null) {
                    selectTarget(bestTarget);
                    reloadTabAbiCombo = true;
                }
            }
        }

        if (!reloadTabAbiCombo) {
            String deviceTagId = device.getTagId();
            Pair<IdDisplay, String> currTagAbi = getSelectedTagAbi();
            if (deviceTagId != null &&
                    (currTagAbi == null || !deviceTagId.equals(currTagAbi.getFirst().getId()))) {
                reloadTabAbiCombo = true;
            }
        }

        if (reloadTabAbiCombo) {
            reloadTagAbiCombo();
        }
    }

    private void toggleCameras() {
        mWidgets.setEnabled(Ctrl.COMBO_FRONT_CAM, false);
        mWidgets.setEnabled(Ctrl.COMBO_BACK_CAM, false);
        Device d = getSelectedDevice();
        if (d != null) {
            for (Camera c : d.getDefaultHardware().getCameras()) {
                if (CameraLocation.FRONT.equals(c.getLocation())) {
                    mWidgets.setEnabled(Ctrl.COMBO_FRONT_CAM, true);
                }
                if (CameraLocation.BACK.equals(c.getLocation())) {
                    mWidgets.setEnabled(Ctrl.COMBO_BACK_CAM, true);
                }
            }
        }
    }

    private void preloadTargetCombo() {
        String selected = null;
        int index = mWidgets.getComboIndex(Ctrl.COMBO_TARGET);
        if (index >= 0) {
            selected = mWidgets.getComboItem(Ctrl.COMBO_TARGET, index);
        }

        mCurrentTargets.clear();
        mWidgets.setComboItems(Ctrl.COMBO_TARGET, null);

        boolean found = false;
        index = -1;

        mComboTargets.clear();
        LocalSdk localSdk = mAvdManager.getLocalSdk();
        if (localSdk != null) {
            for (IAndroidTarget target : localSdk.getTargets()) {
                String name;
                if (target.isPlatform()) {
                    name = String.format("%s - API Level %s",
                            target.getName(),
                            target.getVersion().getApiString());
                } else {
                    name = String.format("%s (%s) - API Level %s",
                            target.getName(),
                            target.getVendor(),
                            target.getVersion().getApiString());
                }
                mCurrentTargets.put(name, target);
                mWidgets.addComboItem(Ctrl.COMBO_TARGET, name);
                mComboTargets.add(target);
                if (!found) {
                    index++;
                    found = name.equals(selected);
                }
            }
        }

        mWidgets.setEnabled(Ctrl.COMBO_TARGET, mCurrentTargets.size() > 0);

        if (found) {
            mWidgets.selectComboIndex(Ctrl.COMBO_TARGET, index);
        }

        reloadTagAbiCombo();
    }

    private void selectTarget(IAndroidTarget target) {
        for (int i = 0, n = mComboTargets.size(); i < n; i++) {
            if (target == mComboTargets.get(i)) {
                mWidgets.selectComboIndex(Ctrl.COMBO_TARGET, i);
                break;
            }
        }
    }

    private IAndroidTarget getSelectedTarget() {
        int index = mWidgets.getComboIndex(Ctrl.COMBO_TARGET);
        if (index != -1 && index < mComboTargets.size()) {
            return mComboTargets.get(index);
        }

        return null;
    }

    /**
     * Reload all the abi types in the selection list.
     * Also adds/remove the skin choices embedded in a tag/abi, if any.
     */
    void reloadTagAbiCombo() {

        int index = mWidgets.getComboIndex(Ctrl.COMBO_TARGET);
        if (index >= 0) {
            String targetName = mWidgets.getComboItem(Ctrl.COMBO_TARGET, index);
            IAndroidTarget target = mCurrentTargets.get(targetName);

            ISystemImage[] systemImages = getSystemImages(target);

            // If user explicitly selected an ABI before, preserve that option
            // If user did not explicitly select before (only one option before)
            // force them to select
            String selected = null;
            index = mWidgets.getComboIndex(Ctrl.COMBO_TAG_ABI);
            if (index >= 0 && mWidgets.getComboSize(Ctrl.COMBO_TAG_ABI) > 1) {
                selected = mWidgets.getComboItem(Ctrl.COMBO_TAG_ABI, index);
            }

            // if there's a selected device that requires a specific non-default tag-id,
            // filter the list to only match this tag.
            Device currDevice = getSelectedDevice();
            String deviceTagId = currDevice == null ? null : currDevice.getTagId();
            if (deviceTagId != null &&
                    (deviceTagId.isEmpty() || SystemImage.DEFAULT_TAG.equals(deviceTagId))) {
                deviceTagId = null;
            }

            // filter and create the list
            mWidgets.setComboItems(Ctrl.COMBO_TAG_ABI, null);
            mComboSystemImages.clear();

            int i;
            boolean found = false;
            for (i = 0; i < systemImages.length; i++) {
                ISystemImage systemImage = systemImages[i];
                if (deviceTagId != null && !deviceTagId.equals(systemImage.getTag().getId())) {
                    continue;
                }
                mComboSystemImages.add(systemImage);
                String prettyAbiType = AvdInfo.getPrettyAbiType(systemImage);
                mWidgets.addComboItem(Ctrl.COMBO_TAG_ABI, prettyAbiType);
                if (!found) {
                    found = prettyAbiType.equals(selected);
                    if (found) {
                        mWidgets.selectComboIndex(Ctrl.COMBO_TAG_ABI, i);
                    }
                }
            }

            mWidgets.setEnabled(Ctrl.COMBO_TAG_ABI, !mComboSystemImages.isEmpty());

            if (mComboSystemImages.isEmpty()) {
                mWidgets.addComboItem(Ctrl.COMBO_TAG_ABI, "No system images installed for this target.");
                mWidgets.selectComboIndex(Ctrl.COMBO_TAG_ABI, 0);
            } else if (mComboSystemImages.size() == 1) {
                mWidgets.selectComboIndex(Ctrl.COMBO_TAG_ABI, 0);
            }
        }

        reloadSkinCombo();
    }

    void reloadSkinCombo() {
        AvdSkinChoice selected = getSelectedSkinChoice();

        // Remove existing target & tag skins
        for (Iterator<AvdSkinChoice> it = mComboSkins.iterator(); it.hasNext(); ) {
            AvdSkinChoice choice = it.next();
            if (choice.hasPath()) {
                it.remove();
            }
        }

        IAndroidTarget target = getSelectedTarget();
        if (target != null) {
            ISystemImage   sysImg = getSelectedSysImg();
            Set<File> sysImgSkins = new HashSet<File>();
            if (sysImg != null) {
                sysImgSkins.addAll(Arrays.asList(sysImg.getSkins()));
            }

            // path of sdk/system-images
            String sdkSysImgPath = new File(mAvdManager.getLocalSdk().getLocation(),
                                            SdkConstants.FD_SYSTEM_IMAGES).getAbsolutePath();

            for (File skin : target.getSkins()) {
                String label = skin.getName();
                String skinPath = skin.getAbsolutePath();
                if (skinPath.startsWith(sdkSysImgPath)) {
                    if (sysImg == null) {
                        // Reject a sys-img based skin if no sys img is selected
                        continue;
                    }
                    if (!sysImgSkins.contains(skin)) {
                        // If a skin comes from a tagged system-image, only display
                        // those matching the current system image.
                        continue;
                    }
                    if (!SystemImage.DEFAULT_TAG.equals(sysImg.getTag().getId())) {
                        // Append the tag name if it's not the similar to the label.
                        String display = sysImg.getTag().getDisplay();
                        String azDisplay = display.toLowerCase(Locale.US).replaceAll("[^a-z]", "");
                        String azLabel   = label  .toLowerCase(Locale.US).replaceAll("[^a-z]", "");
                        if (!azLabel.contains(azDisplay)) {
                            label = String.format("%s (%s)", label, display);
                        }
                    }
                }
                AvdSkinChoice sc = new AvdSkinChoice(SkinType.FROM_TARGET, label, skin);
                mComboSkins.add(sc);
            }
        }

        Collections.sort(mComboSkins);

        mWidgets.setComboItems(Ctrl.COMBO_SKIN, null);
        for (int i = 0; i < mComboSkins.size(); i++) {
            AvdSkinChoice choice = mComboSkins.get(i);
            mWidgets.addComboItem(Ctrl.COMBO_SKIN, choice.getLabel());
            if (choice == selected) {
                mWidgets.selectComboIndex(Ctrl.COMBO_SKIN, i);
            }
        }

    }

    /**
     * Enable or disable the sd card widgets.
     *
     * @param sizeMode if true the size-based widgets are to be enabled, and the
     *            file-based ones disabled.
     */
    void enableSdCardWidgets(boolean sizeMode) {
        mWidgets.setEnabled(Ctrl.TEXT_SDCARD_SIZE, sizeMode);
        mWidgets.setEnabled(Ctrl.COMBO_SDCARD_SIZE, sizeMode);

        mWidgets.setEnabled(Ctrl.TEXT_SDCARD_FILE, !sizeMode);
        mWidgets.setEnabled(Ctrl.BUTTON_BROWSE_SDCARD, !sizeMode);
    }

    void onBrowseSdCard() {
        String fileName = mWidgets. openFileDialog("Choose SD Card image file.");
        if (fileName != null) {
            mWidgets.setText(Ctrl.TEXT_SDCARD_FILE, fileName);
        }
        validatePage();
    }

    void validatePage() {
        String error = null;
        ArrayList<String> warnings = new ArrayList<String>();
        boolean valid = true;

        String avdName = mWidgets.getText(Ctrl.TEXT_AVD_NAME);

        if (avdName.isEmpty()) {
            error = "AVD Name cannot be empty";
            setPageValid(false, error, null);
            return;
        }

        if (!AvdManager.RE_AVD_NAME.matcher(avdName).matches()) {
            error = String.format(
                    "AVD name '%1$s' contains invalid characters.\nAllowed characters are: %2$s",
                    avdName, AvdManager.CHARS_AVD_NAME);
            setPageValid(false, error, null);
            return;
        }

        if (mWidgets.getComboIndex(Ctrl.COMBO_DEVICE) < 0) {
            error = "No device selected";
            setPageValid(false, error, null);
            return;
        }

        if (mWidgets.getComboIndex(Ctrl.COMBO_TARGET) < 0) {
            error = "No target selected";
            setPageValid(false, error, null);
            return;
        }

        if (mComboSystemImages.isEmpty()) {
            error = "No CPU/ABI system image available for this target";
            setPageValid(false, error, null);
            return;
        } else if (getSelectedTagAbi() == null) {
            error = "No CPU/ABI system image selected";
            setPageValid(false, error, null);
            return;
        }

        // If the target is an addon, check its base platform requirement is satisfied.
        String targetName = mWidgets.getComboItem(Ctrl.COMBO_TARGET, mWidgets.getComboIndex(Ctrl.COMBO_TARGET));
        IAndroidTarget target = mCurrentTargets.get(targetName);
        if (target != null && !target.isPlatform()) {

            ISystemImage[] sis = target.getSystemImages();
            if (sis != null && sis.length > 0) {
                // Note: if an addon has no system-images of its own, it depends on its parent
                // platform and it wouldn't have been loaded properly if the platform were
                // missing so we don't need to double-check that part here.

                Pair<IdDisplay, String> tagAbi = getSelectedTagAbi();
                IdDisplay tag = tagAbi.getFirst();
                String abiType = tagAbi.getSecond();
                if (abiType != null &&
                        !abiType.isEmpty() &&
                        target.getParent().getSystemImage(tag, abiType) == null) {
                    // We have a system-image requirement but there is no such system image
                    // loaded in the parent platform. This AVD won't run properly.
                    warnings.add(
                            String.format(
                                "This AVD may not work unless you install the %1$s system image " +
                                "for %2$s (%3$s) first.",
                                AvdInfo.getPrettyAbiType(tag, abiType),
                                target.getParent().getName(),
                                target.getParent().getVersion().toString()));
                }
            }
        }

        AvdSkinChoice skinChoice = getSelectedSkinChoice();
        if (skinChoice == null) {
            error = "No skin selected";
            setPageValid(false, error, null);
            return;
        }

        if (mWidgets.getText(Ctrl.TEXT_RAM).isEmpty()) {
            error = "Mising RAM value";
            setPageValid(false, error, null);
            return;
        }

        if (mWidgets.getText(Ctrl.TEXT_VM_HEAP).isEmpty()) {
            error = "Mising VM Heap value";
            setPageValid(false, error, null);
            return;
        }

        if (mWidgets.getText(Ctrl.TEXT_DATA_PART).isEmpty() || mWidgets.getComboIndex(Ctrl.COMBO_DATA_PART_SIZE) < 0) {
            error = "Invalid Data partition size.";
            setPageValid(false, error, null);
            return;
        }

        // validate sdcard size or file
        if (mWidgets.isChecked(Ctrl.RADIO_SDCARD_SIZE)) {
            if (!mWidgets.getText(Ctrl.TEXT_SDCARD_SIZE).isEmpty() && mWidgets.getComboIndex(Ctrl.COMBO_SDCARD_SIZE) >= 0) {
                try {
                    long sdSize = Long.parseLong(mWidgets.getText(Ctrl.TEXT_SDCARD_SIZE));

                    int sizeIndex = mWidgets.getComboIndex(Ctrl.COMBO_SDCARD_SIZE);
                    if (sizeIndex >= 0) {
                        // index 0 shifts by 10 (1024=K), index 1 by 20, etc.
                        sdSize <<= 10 * (1 + sizeIndex);
                    }

                    if (sdSize < AvdManager.SDCARD_MIN_BYTE_SIZE ||
                            sdSize > AvdManager.SDCARD_MAX_BYTE_SIZE) {
                        valid = false;
                        error = "SD Card size is invalid. Range is 9 MiB..1023 GiB.";
                    }
                } catch (NumberFormatException e) {
                    valid = false;
                    error = " SD Card size must be a valid integer between 9 MiB and 1023 GiB";
                }
            }
        } else {
            if (mWidgets.getText(Ctrl.TEXT_SDCARD_FILE).isEmpty() ||
                    !new File(mWidgets.getText(Ctrl.TEXT_SDCARD_FILE)).isFile()) {
                valid = false;
                error = "SD Card path isn't valid.";
            }
        }
        if (!valid) {
            setPageValid(valid, error, null);
            return;
        }

        if (mWidgets.isEnabled(Ctrl.CHECK_FORCE_CREATION) && !mWidgets.isChecked(Ctrl.CHECK_FORCE_CREATION)) {
            valid = false;
            error = String.format(
                    "The AVD name '%s' is already used.\n" +
                            "Check \"Override the existing AVD\" to delete the existing one.",
                            mWidgets.getText(Ctrl.TEXT_AVD_NAME));
        }

        if (mAvdInfo != null && !mAvdInfo.getName().equals(mWidgets.getText(Ctrl.TEXT_AVD_NAME))) {
            warnings.add(
                    String.format("The AVD '%1$s' will be duplicated into '%2$s'.",
                        mAvdInfo.getName(),
                        mWidgets.getText(Ctrl.TEXT_AVD_NAME)));
        }

        // On Windows, display a warning if attempting to create AVD's with RAM > 512 MB.
        // This restriction should go away when we switch to using a 64 bit emulator.
        if (SdkConstants.CURRENT_PLATFORM == SdkConstants.PLATFORM_WINDOWS) {
            long ramSize = 0;
            try {
                ramSize = Long.parseLong(mWidgets.getText(Ctrl.TEXT_RAM));
            } catch (NumberFormatException e) {
                // ignore
            }

            if (ramSize > 768) {
                warnings.add(
                    "On Windows, emulating RAM greater than 768M may fail depending on the"
                    + " system load. Try progressively smaller values of RAM if the emulator"
                    + " fails to launch.");
            }
        }

        if (mWidgets.isChecked(Ctrl.CHECK_GPU_EMUL) && mWidgets.isChecked(Ctrl.CHECK_SNAPSHOT)) {
            valid = false;
            error = "GPU Emulation and Snapshot cannot be used simultaneously";
        }

        String warning = Joiner.on('\n').join(warnings);
        setPageValid(valid, error, warning);
        return;
    }

    private void setPageValid(boolean valid, String error, String warning) {
        mWidgets.setEnabled(Ctrl.BUTTON_OK, valid);
        if (error != null && !error.isEmpty()) {
            mWidgets.setImage(Ctrl.ICON_STATUS, "reject_icon16.png");  //$NON-NLS-1$
            mWidgets.setText(Ctrl.TEXT_STATUS, error);
        } else if (warning != null && !warning.isEmpty()) {
            mWidgets.setImage(Ctrl.ICON_STATUS, "warning_icon16.png"); //$NON-NLS-1$
            mWidgets.setText(Ctrl.TEXT_STATUS, warning);
        } else {
            mWidgets.setImage(Ctrl.ICON_STATUS, null);
            mWidgets.setText(Ctrl.TEXT_STATUS, " \n "); //$NON-NLS-1$
        }

        mWidgets.repack();
    }

    boolean createAvd() {
        String avdName = mWidgets.getText(Ctrl.TEXT_AVD_NAME);
        if (avdName == null || avdName.isEmpty()) {
            return false;
        }

        String targetName = mWidgets.getComboItem(Ctrl.COMBO_TARGET, mWidgets.getComboIndex(Ctrl.COMBO_TARGET));
        IAndroidTarget target = mCurrentTargets.get(targetName);
        if (target == null) {
            return false;
        }

        // get the tag & abi type
        Pair<IdDisplay, String> tagAbi = getSelectedTagAbi();
        if (tagAbi == null) {
            return false;
        }
        IdDisplay tag = tagAbi.getFirst();
        String abiType = tagAbi.getSecond();

        // get the SD card data from the UI.
        String sdName = null;
        if (mWidgets.isChecked(Ctrl.RADIO_SDCARD_SIZE)) {
            // size mode
            String value = mWidgets.getText(Ctrl.TEXT_SDCARD_SIZE).trim();
            if (value.length() > 0) {
                sdName = value;
                // add the unit
                switch (mWidgets.getComboIndex(Ctrl.COMBO_SDCARD_SIZE)) {
                    case 0:
                        sdName += "K"; //$NON-NLS-1$
                        break;
                    case 1:
                        sdName += "M"; //$NON-NLS-1$
                        break;
                    case 2:
                        sdName += "G"; //$NON-NLS-1$
                        break;
                    default:
                        // shouldn't be here
                        assert false;
                }
            }
        } else {
            // file mode.
            sdName = mWidgets.getText(Ctrl.TEXT_SDCARD_FILE).trim();
        }

        // Get the device
        Device device = getSelectedDevice();
        if (device == null) {
            return false;
        }

        File skinFolder = null;
        String skinName = null;
        AvdSkinChoice skinChoice = getSelectedSkinChoice();
        if (skinChoice == null) {
            return false;
        }
        if (skinChoice.hasPath()) {
            skinFolder = skinChoice.getPath();
        } else {
            Screen s = device.getDefaultHardware().getScreen();
            skinName = s.getXDimension() + "x" + s.getYDimension();
        }

        ILogger log = mSdkLog;
        if (log == null || log instanceof MessageBoxLog) {
            // If the current logger is a message box, we use our own (to make sure
            // to display errors right away and customize the title).
            log = mWidgets.newDelayedMessageBoxLog(
                    String.format("Result of creating AVD '%s':", avdName),
                    false /* logErrorsOnly */);
        }

        Map<String, String> hwProps = DeviceManager.getHardwareProperties(device);
        if (mWidgets.isChecked(Ctrl.CHECK_GPU_EMUL)) {
            hwProps.put(AvdManager.AVD_INI_GPU_EMULATION, HardwareProperties.BOOLEAN_YES);
        }

        File avdFolder = null;
        try {
            avdFolder = AvdInfo.getDefaultAvdFolder(mAvdManager, avdName);
        } catch (AndroidLocationException e) {
            return false;
        }

        // Although the device has this information, some devices have more RAM than we'd want to
        // allocate to an emulator.
        hwProps.put(AvdManager.AVD_INI_RAM_SIZE, mWidgets.getText(Ctrl.TEXT_RAM));
        hwProps.put(AvdManager.AVD_INI_VM_HEAP_SIZE, mWidgets.getText(Ctrl.TEXT_VM_HEAP));

        String suffix;
        switch (mWidgets.getComboIndex(Ctrl.COMBO_DATA_PART_SIZE)) {
            case 0:
                suffix = "M";
                break;
            case 1:
                suffix = "G";
                break;
            default:
                suffix = "K";
        }
        hwProps.put(AvdManager.AVD_INI_DATA_PARTITION_SIZE,
                mWidgets.getText(Ctrl.TEXT_DATA_PART) + suffix);

        hwProps.put(HardwareProperties.HW_KEYBOARD,
                mWidgets.isChecked(Ctrl.CHECK_KEYBOARD) ?
                        HardwareProperties.BOOLEAN_YES : HardwareProperties.BOOLEAN_NO);

        hwProps.put(AvdManager.AVD_INI_SKIN_DYNAMIC,
                skinChoice.getType() == SkinType.DYNAMIC ?
                        HardwareProperties.BOOLEAN_YES : HardwareProperties.BOOLEAN_NO);

        if (mWidgets.isEnabled(Ctrl.COMBO_FRONT_CAM)) {
            hwProps.put(AvdManager.AVD_INI_CAMERA_FRONT,
                    mWidgets.getText(Ctrl.COMBO_FRONT_CAM).toLowerCase());
        }

        if (mWidgets.isEnabled(Ctrl.COMBO_BACK_CAM)) {
            hwProps.put(AvdManager.AVD_INI_CAMERA_BACK,
                    mWidgets.getText(Ctrl.COMBO_BACK_CAM).toLowerCase());
        }

        if (sdName != null) {
            hwProps.put(HardwareProperties.HW_SDCARD, HardwareProperties.BOOLEAN_YES);
        }

        AvdInfo avdInfo = mAvdManager.createAvd(avdFolder,
                avdName,
                target,
                tag,
                abiType,
                skinFolder,
                skinName,
                sdName,
                hwProps,
                device.getBootProps(),
                mWidgets.isChecked(Ctrl.CHECK_SNAPSHOT),
                mWidgets.isChecked(Ctrl.CHECK_FORCE_CREATION),
                mAvdInfo != null, // edit existing
                log);

        mCreatedAvd = avdInfo;
        boolean success = avdInfo != null;

        if (log instanceof IMessageBoxLogger) {
            ((IMessageBoxLogger) log).displayResult(success);
        }
        return success;
    }

    @Nullable
    private AvdSkinChoice getSelectedSkinChoice() {
        int choiceIndex = mWidgets.getComboIndex(Ctrl.COMBO_SKIN);
        if (choiceIndex >= 0 && choiceIndex < mComboSkins.size()) {
            return mComboSkins.get(choiceIndex);
        }
        return null;
    }

    @Nullable
    private Pair<IdDisplay, String> getSelectedTagAbi() {
        ISystemImage selected = getSelectedSysImg();
        if (selected != null) {
            return Pair.of(selected.getTag(), selected.getAbiType());
        }
        return null;
    }

    @Nullable
    private ISystemImage getSelectedSysImg() {
        if (!mComboSystemImages.isEmpty()) {
            int abiIndex = mWidgets.getComboIndex(Ctrl.COMBO_TAG_ABI);
            if (abiIndex >= 0 && abiIndex < mComboSystemImages.size()) {
                return mComboSystemImages.get(abiIndex);
            }
        }
        return null;
    }

    private void fillExistingAvdInfo(AvdInfo avd) {
        mWidgets.setText(Ctrl.TEXT_AVD_NAME, avd.getName());
        selectDevice(avd.getDeviceManufacturer(), avd.getDeviceName());
        toggleCameras();

        IAndroidTarget target = avd.getTarget();

        if (target != null && !mCurrentTargets.isEmpty()) {
            // Try to select the target in the target combo.
            // This will fail if the AVD needs to be repaired.
            //
            // This is a linear search but the list is always
            // small enough and we only do this once.
            int n = mWidgets.getComboSize(Ctrl.COMBO_TARGET);
            for (int i = 0; i < n; i++) {
                if (target.equals(mCurrentTargets.get(mWidgets.getComboItem(Ctrl.COMBO_TARGET, i)))) {
                    // Note: combo.select does not trigger the combo's widgetSelected callback.
                    mWidgets.selectComboIndex(Ctrl.COMBO_TARGET, i);
                    reloadTagAbiCombo();
                    break;
                }
            }
        }

        ISystemImage[] systemImages = getSystemImages(target);
        if (target != null && systemImages.length > 0) {
            mWidgets.setEnabled(Ctrl.COMBO_TAG_ABI, systemImages.length > 1);
            String abiType = AvdInfo.getPrettyAbiType(avd.getTag(), avd.getAbiType());
            int n = mWidgets.getComboSize(Ctrl.COMBO_TAG_ABI);
            for (int i = 0; i < n; i++) {
                if (abiType.equals(mWidgets.getComboItem(Ctrl.COMBO_TAG_ABI, i))) {
                    mWidgets.selectComboIndex(Ctrl.COMBO_TAG_ABI, i);
                    reloadSkinCombo();
                    break;
                }
            }
        }

        Map<String, String> props = avd.getProperties();

        if (props != null) {
            String snapshots = props.get(AvdManager.AVD_INI_SNAPSHOT_PRESENT);
            if (snapshots != null && snapshots.length() > 0) {
                mWidgets.setChecked(Ctrl.CHECK_SNAPSHOT, snapshots.equals("true"));
            }

            String gpuEmulation = props.get(AvdManager.AVD_INI_GPU_EMULATION);
            mWidgets.setChecked(Ctrl.CHECK_GPU_EMUL, gpuEmulation != null &&
                    gpuEmulation.equals(HardwareProperties.BOOLEAN_YES));

            String sdcard = props.get(AvdManager.AVD_INI_SDCARD_PATH);
            if (sdcard != null && sdcard.length() > 0) {
                enableSdCardWidgets(false);
                mWidgets.setChecked(Ctrl.RADIO_SDCARD_SIZE, false);
                mWidgets.setChecked(Ctrl.RADIO_SDCARD_FILE, true);
                mWidgets.setText(Ctrl.TEXT_SDCARD_FILE, sdcard);
            }

            String ramSize = props.get(AvdManager.AVD_INI_RAM_SIZE);
            if (ramSize != null) {
                mWidgets.setText(Ctrl.TEXT_RAM, ramSize);
            }

            String vmHeapSize = props.get(AvdManager.AVD_INI_VM_HEAP_SIZE);
            if (vmHeapSize != null) {
                mWidgets.setText(Ctrl.TEXT_VM_HEAP, vmHeapSize);
            }

            String dataPartitionSize = props.get(AvdManager.AVD_INI_DATA_PARTITION_SIZE);
            if (dataPartitionSize != null) {
                mWidgets.setText(Ctrl.TEXT_DATA_PART,
                    dataPartitionSize.substring(0, dataPartitionSize.length() - 1));
                switch (dataPartitionSize.charAt(dataPartitionSize.length() - 1)) {
                    case 'M':
                        mWidgets.selectComboIndex(Ctrl.COMBO_DATA_PART_SIZE, 0);
                        break;
                    case 'G':
                        mWidgets.selectComboIndex(Ctrl.COMBO_DATA_PART_SIZE, 1);
                        break;
                    default:
                        mWidgets.selectComboIndex(Ctrl.COMBO_DATA_PART_SIZE, -1);
                }
            }

            mWidgets.setChecked(Ctrl.CHECK_KEYBOARD,
                    HardwareProperties.BOOLEAN_YES.equalsIgnoreCase(
                            props.get(HardwareProperties.HW_KEYBOARD)));

            SkinType defaultSkinType = SkinType.NONE;
            // the AVD .ini skin path is relative to the SDK folder *or* is a numeric size.
            String skinIniPath = props.get(AvdManager.AVD_INI_SKIN_PATH);
            if (skinIniPath != null) {
                File skinFolder = new File(mAvdManager.getLocalSdk().getLocation(), skinIniPath);

                for (int i = 0; i < mComboSkins.size(); i++) {
                    if (mComboSkins.get(i).hasPath() &&
                            skinFolder.equals(mComboSkins.get(i).getPath())) {
                        mWidgets.selectComboIndex(Ctrl.COMBO_SKIN, i);
                        defaultSkinType = null;
                        break;
                    }
                }
            }

            if (defaultSkinType != null) {
                if (HardwareProperties.BOOLEAN_YES.equalsIgnoreCase(
                        props.get(AvdManager.AVD_INI_SKIN_DYNAMIC))) {
                    defaultSkinType = SkinType.DYNAMIC;
                }

                for (int i = 0; i < mComboSkins.size(); i++) {
                    if (mComboSkins.get(i).getType() == defaultSkinType) {
                        mWidgets.selectComboIndex(Ctrl.COMBO_SKIN, i);
                        break;
                    }
                }

            }

            String cameraFront = props.get(AvdManager.AVD_INI_CAMERA_FRONT);
            if (cameraFront != null) {
                for (int i = 0, n = mWidgets.getComboSize(Ctrl.COMBO_FRONT_CAM); i < n; i++) {
                    String item = mWidgets.getComboItem(Ctrl.COMBO_FRONT_CAM, i);
                    if (item.toLowerCase().equals(cameraFront)) {
                        mWidgets.selectComboIndex(Ctrl.COMBO_FRONT_CAM, i);
                        break;
                    }
                }
            }

            String cameraBack = props.get(AvdManager.AVD_INI_CAMERA_BACK);
            if (cameraBack != null) {
                for (int i = 0, n = mWidgets.getComboSize(Ctrl.COMBO_BACK_CAM); i < n; i++) {
                    String item = mWidgets.getComboItem(Ctrl.COMBO_BACK_CAM, i);
                    if (item.toLowerCase().equals(cameraBack)) {
                        mWidgets.selectComboIndex(Ctrl.COMBO_BACK_CAM, i);
                        break;
                    }
                }
            }

            sdcard = props.get(AvdManager.AVD_INI_SDCARD_SIZE);
            if (sdcard != null && sdcard.length() > 0) {
                String[] values = new String[2];
                long sdcardSize = AvdManager.parseSdcardSize(sdcard, values);

                if (sdcardSize != AvdManager.SDCARD_NOT_SIZE_PATTERN) {
                    enableSdCardWidgets(true);
                    mWidgets.setChecked(Ctrl.RADIO_SDCARD_FILE, false);
                    mWidgets.setChecked(Ctrl.RADIO_SDCARD_SIZE, true);

                    mWidgets.setText(Ctrl.TEXT_SDCARD_SIZE, values[0]);

                    String suffix = values[1];
                    int n = mWidgets.getComboSize(Ctrl.COMBO_SDCARD_SIZE);
                    for (int i = 0; i < n; i++) {
                        if (mWidgets.getComboItem(Ctrl.COMBO_SDCARD_SIZE, i).startsWith(suffix)) {
                            mWidgets.selectComboIndex(Ctrl.COMBO_SDCARD_SIZE, i);
                        }
                    }
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void fillInitialDeviceInfo(Device device) {
        String name = device.getManufacturer();
        if (!name.equals("Generic") &&      // TODO define & use constants
                !name.equals("User") &&
                device.getName().indexOf(name) == -1) {
            name = " by " + name;
        } else {
            name = "";
        }
        name = "AVD for " + device.getName() + name;
        // sanitize the name
        name = name.replaceAll("[^0-9a-zA-Z_-]+", " ").trim().replaceAll("[ _]+", "_");
        mWidgets.setText(Ctrl.TEXT_AVD_NAME, name);

        // Select the device
        selectDevice(device);
        toggleCameras();

        // If there's only one target, select it by default.
        // TODO: if there are more than 1 target, select the higher platform target as
        // a likely default.
        if (mWidgets.getComboSize(Ctrl.COMBO_TARGET) == 1) {
            mWidgets.selectComboIndex(Ctrl.COMBO_TARGET, 0);
            reloadTagAbiCombo();
        }

        fillDeviceProperties(device);
    }

    /**
     * Returns the list of system images of a target.
     * <p/>
     * If target is null, returns an empty list. If target is an add-on with no
     * system images, return the list from its parent platform.
     *
     * @param target An IAndroidTarget. Can be null.
     * @return A non-null ISystemImage array. Can be empty.
     */
    @NonNull
    private ISystemImage[] getSystemImages(IAndroidTarget target) {
        if (target != null) {
            ISystemImage[] images = target.getSystemImages();

            if ((images == null || images.length == 0) && !target.isPlatform()) {
                // This is an add-on and it does not provide any system image.

                // Before LMP / Tools 23.0.4, the behavior was to provide the
                // parent (platform) system-image using this code:
                //
                // images = target.getParent().getSystemImages();
                //
                // After tools 23.0.4, the behavior is to NOT provide the
                // platform system-image for the add-on.
            }

            if (images != null) {
                return images;
            }
        }

        return new ISystemImage[0];
    }

    // Code copied from DeviceMenuListener in ADT; unify post release

    private static final String NEXUS = "Nexus";       //$NON-NLS-1$
    private static final String GENERIC = "Generic";   //$NON-NLS-1$
    private static Pattern PATTERN = Pattern.compile(
            "(\\d+\\.?\\d*)in (.+?)( \\(.*Nexus.*\\))?"); //$NON-NLS-1$

    private static int nexusRank(Device device) {
        @SuppressWarnings("deprecation")
        String name = device.getName();
        if (name.endsWith(" One")) {     //$NON-NLS-1$
            return 1;
        }
        if (name.endsWith(" S")) {       //$NON-NLS-1$
            return 2;
        }
        if (name.startsWith("Galaxy")) { //$NON-NLS-1$
            return 3;
        }
        if (name.endsWith(" 7")) {       //$NON-NLS-1$
            return 4;
        }
        if (name.endsWith(" 10")) {       //$NON-NLS-1$
            return 5;
        }
        if (name.endsWith(" 4")) {       //$NON-NLS-1$
            return 6;
        }

        return 7;
    }

    private static boolean isGeneric(Device device) {
        return device.getManufacturer().equals(GENERIC);
    }

    @SuppressWarnings("deprecation")
    private static boolean isNexus(Device device) {
        return device.getName().contains(NEXUS);
    }

    private static String getGenericLabel(Device d) {
        // * Replace "'in'" with '"' (e.g. 2.7" QVGA instead of 2.7in QVGA)
        // * Use the same precision for all devices (all but one specify decimals)
        // * Add some leading space such that the dot ends up roughly in the
        //   same space
        // * Add in screen resolution and density
        @SuppressWarnings("deprecation")
        String name = d.getName();
        if (name.equals("3.7 FWVGA slider")) {                        //$NON-NLS-1$
            // Fix metadata: this one entry doesn't have "in" like the rest of them
            name = "3.7in FWVGA slider";                              //$NON-NLS-1$
        }

        Matcher matcher = PATTERN.matcher(name);
        if (matcher.matches()) {
            String size = matcher.group(1);
            String n = matcher.group(2);
            int dot = size.indexOf('.');
            if (dot == -1) {
                size = size + ".0";
                dot = size.length() - 2;
            }
            for (int i = 0; i < 2 - dot; i++) {
                size = ' ' + size;
            }
            name = size + "\" " + n;
        }

        return String.format(java.util.Locale.US, "%1$s (%2$s)", name,
                getResolutionString(d));
    }

    private static String getNexusLabel(Device d) {
        @SuppressWarnings("deprecation")
        String name = d.getName();
        Screen screen = d.getDefaultHardware().getScreen();
        float length = (float) screen.getDiagonalLength();
        return String.format(java.util.Locale.US, "%1$s (%3$s\", %2$s)",
                name, getResolutionString(d), Float.toString(length));
    }

    @Nullable
    private static String getResolutionString(Device device) {
        Screen screen = device.getDefaultHardware().getScreen();
        return String.format(java.util.Locale.US,
                "%1$d \u00D7 %2$d: %3$s", // U+00D7: Unicode multiplication sign
                screen.getXDimension(),
                screen.getYDimension(),
                screen.getPixelDensity().getResourceValue());
    }

    //-------


    /**
     * AVD skin type. Order defines the order of the skin combo list.
     */
    private enum SkinType {
        DYNAMIC,
        NONE,
        FROM_TARGET,
    }

    /*
     * Choice of AVD skin: dynamic, no skin, or one from the target.
     * The 2 "internals" skins (dynamic and no skin) have no path.
     * The target-based skins have a path.
     */
    private static class AvdSkinChoice implements Comparable<AvdSkinChoice> {

        private final SkinType mType;
        private final String mLabel;
        private final File mPath;

        AvdSkinChoice(@NonNull SkinType type, @NonNull String label) {
            this(type, label, null);
        }

        AvdSkinChoice(@NonNull SkinType type, @NonNull String label, @NonNull File path) {
            mType = type;
            mLabel = label;
            mPath = path;
        }

        @NonNull
        public SkinType getType() {
            return mType;
        }

        @NonNull
        public String getLabel() {
            return mLabel;
        }

        @Nullable
        public File getPath() {
            return mPath;
        }

        public boolean hasPath() {
            return mType == SkinType.FROM_TARGET;
        }

        @Override
        public int compareTo(AvdSkinChoice o) {
            int t = mType.compareTo(o.mType);
            if (t == 0) {
                t = mLabel.compareTo(o.mLabel);
            }
            if (t == 0 && mPath != null && o.mPath != null) {
                t = mPath.compareTo(o.mPath);
            }
            return t;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((mType == null) ? 0 : mType.hashCode());
            result = prime * result + ((mLabel == null) ? 0 : mLabel.hashCode());
            result = prime * result + ((mPath == null) ? 0 : mPath.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof AvdSkinChoice)) {
                return false;
            }
            AvdSkinChoice other = (AvdSkinChoice) obj;
            if (mType != other.mType) {
                return false;
            }
            if (mLabel == null) {
                if (other.mLabel != null) {
                    return false;
                }
            } else if (!mLabel.equals(other.mLabel)) {
                return false;
            }
            if (mPath == null) {
                if (other.mPath != null) {
                    return false;
                }
            } else if (!mPath.equals(other.mPath)) {
                return false;
            }
            return true;
        }


    }

    public void onTargetComboChanged() {
        reloadTagAbiCombo();
        validatePage();
    }

    public void onTagComboChanged() {
        reloadSkinCombo();
        validatePage();
    }

    public void onRadioSdCardSizeChanged() {
        boolean sizeMode = mWidgets.isChecked(Ctrl.RADIO_SDCARD_SIZE);
        enableSdCardWidgets(sizeMode);
        validatePage();
    }


}

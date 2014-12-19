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

package org.eclipse.andmore.internal.launch;

import com.android.ddmuilib.ImageLoader;
import com.android.prefs.AndroidLocation.AndroidLocationException;
import com.android.sdklib.AndroidVersion;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.internal.avd.AvdInfo;
import com.android.sdklib.internal.avd.AvdManager;
import com.android.sdkuilib.internal.widgets.AvdSelector;
import com.android.sdkuilib.internal.widgets.AvdSelector.DisplayMode;
import com.android.sdkuilib.internal.widgets.AvdSelector.IAvdFilter;
import com.android.utils.NullLogger;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.editors.manifest.ManifestInfo;
import org.eclipse.andmore.internal.launch.AndroidLaunchConfiguration.TargetMode;
import org.eclipse.andmore.internal.launch.AvdCompatibility.Compatibility;
import org.eclipse.andmore.internal.preferences.AdtPrefs;
import org.eclipse.andmore.internal.project.BaseProjectHelper;
import org.eclipse.andmore.internal.sdk.AdtConsoleSdkLog;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * Launch configuration tab to control the parameters of the Emulator
 */
public class EmulatorConfigTab extends AbstractLaunchConfigurationTab {

    private final static String[][] NETWORK_SPEEDS = new String[][] {
        { "Full", "full" }, //$NON-NLS-2$
        { "GSM", "gsm" }, //$NON-NLS-2$
        { "HSCSD", "hscsd" }, //$NON-NLS-2$
        { "GPRS", "gprs" }, //$NON-NLS-2$
        { "EDGE", "edge" }, //$NON-NLS-2$
        { "UMTS", "umts" }, //$NON-NLS-2$
        { "HSPDA", "hsdpa" }, //$NON-NLS-2$
    };

    private final static String[][] NETWORK_LATENCIES = new String[][] {
        { "None", "none" }, //$NON-NLS-2$
        { "GPRS", "gprs" }, //$NON-NLS-2$
        { "EDGE", "edge" }, //$NON-NLS-2$
        { "UMTS", "umts" }, //$NON-NLS-2$
    };

    private Button mAutoTargetButton;
    private Button mManualTargetButton;
    private AvdSelector mPreferredAvdSelector;
    private Combo mSpeedCombo;
    private Combo mDelayCombo;
    private Group mEmulatorOptionsGroup;
    private Text mEmulatorCLOptions;
    private Button mWipeDataButton;
    private Button mNoBootAnimButton;
    private Label mPreferredAvdLabel;
    private IAndroidTarget mProjectTarget;
    private AndroidVersion mProjectMinApiVersion;
    private Button mFutureLaunchesOnSameDevice;
    private boolean mSupportMultiDeviceLaunch;
    private Button mAllDevicesTargetButton;
    private Combo mDeviceTypeCombo;

    private static final String DEVICES_AND_EMULATORS = "Active devices and AVD's";
    private static final String EMULATORS_ONLY = "Active AVD's";
    private static final String DEVICES_ONLY = "Active devices";

    /**
     * Returns the emulator ready speed option value.
     * @param value The index of the combo selection.
     */
    public static String getSpeed(int value) {
        try {
            return NETWORK_SPEEDS[value][1];
        } catch (ArrayIndexOutOfBoundsException e) {
            return NETWORK_SPEEDS[LaunchConfigDelegate.DEFAULT_SPEED][1];
        }
    }

    /**
     * Returns the emulator ready network latency value.
     * @param value The index of the combo selection.
     */
    public static String getDelay(int value) {
        try {
            return NETWORK_LATENCIES[value][1];
        } catch (ArrayIndexOutOfBoundsException e) {
            return NETWORK_LATENCIES[LaunchConfigDelegate.DEFAULT_DELAY][1];
        }
    }

    /**
     *
     */
    public EmulatorConfigTab(boolean supportMultiDeviceLaunch) {
        mSupportMultiDeviceLaunch = supportMultiDeviceLaunch;
    }

    /**
     * @wbp.parser.entryPoint
     */
    @Override
    public void createControl(Composite parent) {
        Font font = parent.getFont();

        // Reload the AVDs to make sure we are up to date
        try {
            // SDK can be null if the user opens the dialog before ADT finished
            // initializing the SDK itself. In this case just don't reload anything
            // so there's nothing obsolete yet.
            Sdk sdk = Sdk.getCurrent();
            if (sdk != null) {
                AvdManager avdMan = sdk.getAvdManager();
                assert avdMan != null;
                avdMan.reloadAvds(NullLogger.getLogger());
            }
        } catch (AndroidLocationException e1) {
            // this happens if the AVD Manager failed to find the folder in which the AVDs are
            // stored. There isn't much we can do at this point.
        }

        Composite topComp = new Composite(parent, SWT.NONE);
        setControl(topComp);
        GridLayout topLayout = new GridLayout();
        topLayout.numColumns = 1;
        topLayout.verticalSpacing = 0;
        topComp.setLayout(topLayout);
        topComp.setFont(font);

        GridData gd;
        GridLayout layout;

        // radio button for the target mode
        Group targetModeGroup = new Group(topComp, SWT.NONE);
        targetModeGroup.setText("Deployment Target Selection Mode");
        gd = new GridData(GridData.FILL_HORIZONTAL);
        targetModeGroup.setLayoutData(gd);
        layout = new GridLayout();
        layout.numColumns = 1;
        targetModeGroup.setLayout(layout);
        targetModeGroup.setFont(font);

        mManualTargetButton = new Button(targetModeGroup, SWT.RADIO);
        mManualTargetButton.setText("Always prompt to pick device");

        mAllDevicesTargetButton = new Button(targetModeGroup, SWT.RADIO);
        mAllDevicesTargetButton.setText("Launch on all compatible devices/AVD's");
        mAllDevicesTargetButton.setEnabled(mSupportMultiDeviceLaunch);

        Composite deviceTypeOffsetComp = new Composite(targetModeGroup, SWT.NONE);
        deviceTypeOffsetComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        layout = new GridLayout(1, false);
        layout.marginRight = layout.marginHeight = 0;
        layout.marginLeft = 30;
        deviceTypeOffsetComp.setLayout(layout);

        mDeviceTypeCombo = new Combo(deviceTypeOffsetComp, SWT.READ_ONLY);
        mDeviceTypeCombo.setItems(new String[] {
                DEVICES_AND_EMULATORS,
                EMULATORS_ONLY,
                DEVICES_ONLY,
        });
        mDeviceTypeCombo.select(0);
        mDeviceTypeCombo.setEnabled(false);

        // add the radio button
        mAutoTargetButton = new Button(targetModeGroup, SWT.RADIO);
        mAutoTargetButton.setText("Automatically pick compatible device: "
                + "Always uses preferred AVD if set below, "
                + "launches on compatible device/AVD otherwise.");
        mAutoTargetButton.setSelection(true);

        SelectionListener targetModeChangeListener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                targetModeChanged();
            }
        };

        mAutoTargetButton.addSelectionListener(targetModeChangeListener);
        mAllDevicesTargetButton.addSelectionListener(targetModeChangeListener);
        mManualTargetButton.addSelectionListener(targetModeChangeListener);

        Composite avdOffsetComp = new Composite(targetModeGroup, SWT.NONE);
        avdOffsetComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        layout = new GridLayout(1, false);
        layout.marginRight = layout.marginHeight = 0;
        layout.marginLeft = 30;
        avdOffsetComp.setLayout(layout);

        mPreferredAvdLabel = new Label(avdOffsetComp, SWT.NONE);
        mPreferredAvdLabel.setText("Select a preferred Android Virtual Device for deployment:");

        // create the selector with no manager, we'll reset the manager every time this is
        // displayed to ensure we have the latest one (dialog is reused but SDK could have
        // been changed in between.
        mPreferredAvdSelector = new AvdSelector(avdOffsetComp,
                Sdk.getCurrent().getSdkOsLocation(),
                null /* avd manager */,
                DisplayMode.SIMPLE_CHECK,
                new AdtConsoleSdkLog());
        mPreferredAvdSelector.setTableHeightHint(100);
        SelectionListener listener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateLaunchConfigurationDialog();
            }
        };
        mPreferredAvdSelector.setSelectionListener(listener);
        mDeviceTypeCombo.addSelectionListener(listener);

        mFutureLaunchesOnSameDevice = new Button(targetModeGroup, SWT.CHECK);
        mFutureLaunchesOnSameDevice.setText("Use same device for future launches");
        mFutureLaunchesOnSameDevice.addSelectionListener(listener);

        // emulator size
        mEmulatorOptionsGroup = new Group(topComp, SWT.NONE);
        mEmulatorOptionsGroup.setText("Emulator launch parameters:");
        mEmulatorOptionsGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        layout = new GridLayout();
        layout.numColumns = 2;
        mEmulatorOptionsGroup.setLayout(layout);
        mEmulatorOptionsGroup.setFont(font);

        // Explanation
        Label l = new Label(mEmulatorOptionsGroup, SWT.NONE);
        l.setText("If no compatible and active devices or AVD's are found, then an AVD "
                 + "might be launched. Provide options for the AVD launch below.");
        gd = new GridData();
        gd.horizontalSpan = 2;
        l.setLayoutData(gd);

        // network options
        new Label(mEmulatorOptionsGroup, SWT.NONE).setText("Network Speed:");

        mSpeedCombo = new Combo(mEmulatorOptionsGroup, SWT.READ_ONLY);
        for (String[] speed : NETWORK_SPEEDS) {
            mSpeedCombo.add(speed[0]);
        }
        mSpeedCombo.addSelectionListener(listener);
        mSpeedCombo.pack();

        new Label(mEmulatorOptionsGroup, SWT.NONE).setText("Network Latency:");

        mDelayCombo = new Combo(mEmulatorOptionsGroup, SWT.READ_ONLY);

        for (String[] delay : NETWORK_LATENCIES) {
            mDelayCombo.add(delay[0]);
        }
        mDelayCombo.addSelectionListener(listener);
        mDelayCombo.pack();

        // wipe data option
        mWipeDataButton = new Button(mEmulatorOptionsGroup, SWT.CHECK);
        mWipeDataButton.setText("Wipe User Data");
        mWipeDataButton.setToolTipText("Check this if you want to wipe your user data each time you start the emulator. You will be prompted for confirmation when the emulator starts.");
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        mWipeDataButton.setLayoutData(gd);
        mWipeDataButton.addSelectionListener(listener);

        // no boot anim option
        mNoBootAnimButton = new Button(mEmulatorOptionsGroup, SWT.CHECK);
        mNoBootAnimButton.setText("Disable Boot Animation");
        mNoBootAnimButton.setToolTipText("Check this if you want to disable the boot animation. This can help the emulator start faster on slow machines.");
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        mNoBootAnimButton.setLayoutData(gd);
        mNoBootAnimButton.addSelectionListener(listener);

        // custom command line option for emulator
        l = new Label(mEmulatorOptionsGroup, SWT.NONE);
        l.setText("Additional Emulator Command Line Options");
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        l.setLayoutData(gd);

        mEmulatorCLOptions = new Text(mEmulatorOptionsGroup, SWT.BORDER);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        mEmulatorCLOptions.setLayoutData(gd);
        mEmulatorCLOptions.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                updateLaunchConfigurationDialog();
            }
        });
    }

    private void targetModeChanged() {
        updateLaunchConfigurationDialog();

        boolean auto = mAutoTargetButton.getSelection();
        mPreferredAvdSelector.setEnabled(auto);
        mPreferredAvdLabel.setEnabled(auto);

        boolean all = mAllDevicesTargetButton.getSelection();
        mDeviceTypeCombo.setEnabled(all);
    }

    /* (non-Javadoc)
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
     */
    @Override
    public String getName() {
        return "Target";
    }

    @Override
    public Image getImage() {
        return ImageLoader.getDdmUiLibLoader().loadImage("emulator.png", null); //$NON-NLS-1$
    }

    private void updateAvdList(AvdManager avdManager) {
        if (avdManager == null) {
            avdManager = Sdk.getCurrent().getAvdManager();
        }

        mPreferredAvdSelector.setManager(avdManager);
        mPreferredAvdSelector.refresh(false);

        mPreferredAvdSelector.setFilter(new IAvdFilter() {
            @Override
            public void prepare() {
            }

            @Override
            public void cleanup() {
            }

            @Override
            public boolean accept(AvdInfo avd) {
                AvdCompatibility.Compatibility c =
                        AvdCompatibility.canRun(avd, mProjectTarget, mProjectMinApiVersion);
                return (c == Compatibility.NO) ? false : true;
            }
        });
    }

    /* (non-Javadoc)
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
     */
    @Override
    public void initializeFrom(ILaunchConfiguration configuration) {
        AvdManager avdManager = Sdk.getCurrent().getAvdManager();

        TargetMode mode = AndroidLaunchConfiguration.parseTargetMode(configuration,
                LaunchConfigDelegate.DEFAULT_TARGET_MODE);

        boolean multipleDevices = mode.isMultiDevice();
        if (multipleDevices && !mSupportMultiDeviceLaunch) {
            // The launch config says to run on multiple devices, but this launch type does not
            // suppport multiple devices. In such a case, switch back to default mode.
            // This could happen if a launch config used for Run is then used for Debug.
            multipleDevices = false;
            mode = LaunchConfigDelegate.DEFAULT_TARGET_MODE;
        }

        mAutoTargetButton.setSelection(mode == TargetMode.AUTO);
        mManualTargetButton.setSelection(mode == TargetMode.MANUAL);
        mAllDevicesTargetButton.setSelection(multipleDevices);

        targetModeChanged();

        boolean reuseLastUsedDevice;
        try {
            reuseLastUsedDevice = configuration.getAttribute(
                    LaunchConfigDelegate.ATTR_REUSE_LAST_USED_DEVICE, false);
        } catch (CoreException ex) {
            reuseLastUsedDevice = false;
        }
        mFutureLaunchesOnSameDevice.setSelection(reuseLastUsedDevice);

        mDeviceTypeCombo.setEnabled(multipleDevices);
        if (multipleDevices) {
            int index = 0;
            if (mode == TargetMode.ALL_EMULATORS) {
                index = 1;
            } else if (mode == TargetMode.ALL_DEVICES) {
                index = 2;
            }
            mDeviceTypeCombo.select(index);
        }

        // look for the project name to get its target.
        String stringValue = "";
        try {
            stringValue = configuration.getAttribute(
                    IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, stringValue);
        } catch (CoreException ce) {
            // let's not do anything here, we'll use the default value
        }

        IProject project = null;

        // get the list of existing Android projects from the workspace.
        IJavaProject[] projects = BaseProjectHelper.getAndroidProjects(null /*filter*/);
        if (projects != null) {
            // look for the project whose name we read from the configuration.
            for (IJavaProject p : projects) {
                if (p.getElementName().equals(stringValue)) {
                    project = p.getProject();
                    break;
                }
            }
        }

        // update the AVD list
        if (project != null) {
            mProjectTarget = Sdk.getCurrent().getTarget(project);

            ManifestInfo mi = ManifestInfo.get(project);
            final int minApiLevel = mi.getMinSdkVersion();
            final String minApiCodeName = mi.getMinSdkCodeName();
            mProjectMinApiVersion = new AndroidVersion(minApiLevel, minApiCodeName);
        }

        updateAvdList(avdManager);

        stringValue = "";
        try {
            stringValue = configuration.getAttribute(LaunchConfigDelegate.ATTR_AVD_NAME,
                    stringValue);
        } catch (CoreException e) {
            // let's not do anything here, we'll use the default value
        }

        if (stringValue != null && stringValue.length() > 0 && avdManager != null) {
            AvdInfo targetAvd = avdManager.getAvd(stringValue, true /*validAvdOnly*/);
            mPreferredAvdSelector.setSelection(targetAvd);
        } else {
            mPreferredAvdSelector.setSelection(null);
        }

        boolean value = LaunchConfigDelegate.DEFAULT_WIPE_DATA;
        try {
            value = configuration.getAttribute(LaunchConfigDelegate.ATTR_WIPE_DATA, value);
        } catch (CoreException e) {
            // let's not do anything here, we'll use the default value
        }
        mWipeDataButton.setSelection(value);

        value = LaunchConfigDelegate.DEFAULT_NO_BOOT_ANIM;
        try {
            value = configuration.getAttribute(LaunchConfigDelegate.ATTR_NO_BOOT_ANIM, value);
        } catch (CoreException e) {
            // let's not do anything here, we'll use the default value
        }
        mNoBootAnimButton.setSelection(value);

        int index = -1;

        index = LaunchConfigDelegate.DEFAULT_SPEED;
        try {
            index = configuration.getAttribute(LaunchConfigDelegate.ATTR_SPEED,
                    index);
        } catch (CoreException e) {
            // let's not do anything here, we'll use the default value
        }
        if (index == -1) {
            mSpeedCombo.clearSelection();
        } else {
            mSpeedCombo.select(index);
        }

        index = LaunchConfigDelegate.DEFAULT_DELAY;
        try {
            index = configuration.getAttribute(LaunchConfigDelegate.ATTR_DELAY,
                    index);
        } catch (CoreException e) {
            // let's not do anything here, we'll put a proper value in
            // performApply anyway
        }
        if (index == -1) {
            mDelayCombo.clearSelection();
        } else {
            mDelayCombo.select(index);
        }

        String commandLine = null;
        try {
            commandLine = configuration.getAttribute(
                    LaunchConfigDelegate.ATTR_COMMANDLINE, ""); //$NON-NLS-1$
        } catch (CoreException e) {
            // let's not do anything here, we'll use the default value
        }
        if (commandLine != null) {
            mEmulatorCLOptions.setText(commandLine);
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
     */
    @Override
    public void performApply(ILaunchConfigurationWorkingCopy configuration) {
        configuration.setAttribute(LaunchConfigDelegate.ATTR_TARGET_MODE,
                getCurrentTargetMode().toString());
        configuration.setAttribute(LaunchConfigDelegate.ATTR_REUSE_LAST_USED_DEVICE,
                mFutureLaunchesOnSameDevice.getSelection());
        AvdInfo avd = mPreferredAvdSelector.getSelected();
        if (avd != null) {
            configuration.setAttribute(LaunchConfigDelegate.ATTR_AVD_NAME, avd.getName());
        } else {
            configuration.setAttribute(LaunchConfigDelegate.ATTR_AVD_NAME, (String)null);
        }
        configuration.setAttribute(LaunchConfigDelegate.ATTR_SPEED,
                mSpeedCombo.getSelectionIndex());
        configuration.setAttribute(LaunchConfigDelegate.ATTR_DELAY,
                mDelayCombo.getSelectionIndex());
        configuration.setAttribute(LaunchConfigDelegate.ATTR_COMMANDLINE,
                mEmulatorCLOptions.getText());
        configuration.setAttribute(LaunchConfigDelegate.ATTR_WIPE_DATA,
                mWipeDataButton.getSelection());
        configuration.setAttribute(LaunchConfigDelegate.ATTR_NO_BOOT_ANIM,
                mNoBootAnimButton.getSelection());
   }

    private TargetMode getCurrentTargetMode() {
        if (mAutoTargetButton.getSelection()) {
            return TargetMode.AUTO;
        } else if (mManualTargetButton.getSelection()) {
            return TargetMode.MANUAL;
        } else {
            String selection = mDeviceTypeCombo.getText();
            if (DEVICES_AND_EMULATORS.equals(selection)) {
                return TargetMode.ALL_DEVICES_AND_EMULATORS;
            } else if (DEVICES_ONLY.equals(selection)) {
                return TargetMode.ALL_DEVICES;
            } else if (EMULATORS_ONLY.equals(selection)) {
                return TargetMode.ALL_EMULATORS;
            }
        }

        return TargetMode.AUTO;
    }

    /* (non-Javadoc)
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
     */
    @Override
    public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
        configuration.setAttribute(LaunchConfigDelegate.ATTR_TARGET_MODE,
                LaunchConfigDelegate.DEFAULT_TARGET_MODE.toString());
        configuration.setAttribute(LaunchConfigDelegate.ATTR_SPEED,
                LaunchConfigDelegate.DEFAULT_SPEED);
        configuration.setAttribute(LaunchConfigDelegate.ATTR_DELAY,
                LaunchConfigDelegate.DEFAULT_DELAY);
        configuration.setAttribute(LaunchConfigDelegate.ATTR_WIPE_DATA,
                LaunchConfigDelegate.DEFAULT_WIPE_DATA);
        configuration.setAttribute(LaunchConfigDelegate.ATTR_NO_BOOT_ANIM,
                LaunchConfigDelegate.DEFAULT_NO_BOOT_ANIM);

        IPreferenceStore store = AdtPlugin.getDefault().getPreferenceStore();
        String emuOptions = store.getString(AdtPrefs.PREFS_EMU_OPTIONS);
        configuration.setAttribute(LaunchConfigDelegate.ATTR_COMMANDLINE, emuOptions);
   }
}

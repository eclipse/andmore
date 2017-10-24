/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.sdkuilib.internal.widgets;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.sdkuilib.internal.repository.icons.ImageFactory;
import com.android.sdkuilib.internal.widgets.AvdCreationPresenter.Ctrl;
import com.android.sdkuilib.internal.widgets.AvdCreationPresenter.IWidgetAdapter;
import com.android.sdkuilib.ui.GridDialog;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import java.util.HashMap;
import java.util.Map;

/**
 * Creates the SWT shell and controls for the {@link AvdCreationDialog}.
 * All the logic is handled by the {@link AvdCreationPresenter}.
 *
 * @see AvdCreationPresenter
 */
class AvdCreationSwtView extends GridDialog {

    private final ImageFactory mImageFactory;
    private final AvdCreationPresenter mPresenter;

    private Button mBtnOK;

    private Text mTextAvdName;

    private Combo mComboDevice;

    private Combo mComboTarget;
    private Combo mComboTagAbi;

    private Button mCheckKeyboard;
    private Combo mComboSkinCombo;

    private Combo mComboFrontCamera;
    private Combo mComboBackCamera;

    private Button mCheckSnapshot;
    private Button mCheckGpuEmulation;

    private Text mTextRam;
    private Text mTextVmHeap;

    private Text mTextDataPartition;
    private Combo mComboDataPartitionSize;

    private Button mRadioSdCardSize;
    private Text mTextSdCardSize;
    private Combo mComboSdCardSize;
    private Button mRadioSdCardFile;
    private Text mTextSdCardFile;
    private Button mBtnBrowseSdCard;

    private Button mCheckForceCreation;
    private Composite mCompositeStatus;

    private Label mIconStatus;
    private Label mTextStatus;

    private final Map<Ctrl, Control> mControlMap = new HashMap<AvdCreationPresenter.Ctrl, Control>();

    /**
     * {@link VerifyListener} for {@link Text} widgets that should only contains
     * numbers.
     */
    private final VerifyListener mDigitVerifier = new VerifyListener() {
        @Override
        public void verifyText(VerifyEvent event) {
            int count = event.text.length();
            for (int i = 0; i < count; i++) {
                char c = event.text.charAt(i);
                if (c < '0' || c > '9') {
                    event.doit = false;
                    return;
                }
            }
        }
    };

    public AvdCreationSwtView(Shell shell,
            @NonNull ImageFactory imageFactory,
            @NonNull AvdCreationPresenter presenter) {
        super(shell, 2, false);
        mImageFactory = imageFactory;
        mPresenter = presenter;
        setShellStyle(getShellStyle() | SWT.RESIZE);

        mPresenter.setWidgetAdapter(new IWidgetAdapter() {
            @Override
            public void setTitle(@NonNull String title) {
                getShell().setText(title);
            }

            @Override
            public int getComboIndex(@NonNull Ctrl ctrl) {
                Control c = mControlMap.get(ctrl);
                if (c instanceof Combo) {
                    return ((Combo) c).getSelectionIndex();
                }
                return -1;
            }

            @Override
            public int getComboSize(@NonNull Ctrl ctrl) {
                Control c = mControlMap.get(ctrl);
                if (c instanceof Combo) {
                    return ((Combo) c).getItemCount();
                }
                return 0;
            }

            @Nullable
            @Override
            public String getComboItem(@NonNull Ctrl ctrl, int index) {
                Control c = mControlMap.get(ctrl);
                if (c instanceof Combo) {
                    return ((Combo) c).getItem(index);
                }
                return null;
            }

            @Override
            public void selectComboIndex(@NonNull Ctrl ctrl, int index) {
                Control c = mControlMap.get(ctrl);
                if (c instanceof Combo) {
                    ((Combo) c).select(index);
                }
            }

            @Override
            public void addComboItem(@NonNull Ctrl ctrl, String label) {
                Control c = mControlMap.get(ctrl);
                if (c instanceof Combo) {
                    ((Combo) c).add(label);
                }
            }

            @Override
            public void setComboItems(@NonNull Ctrl ctrl, String[] labels) {
                Control c = mControlMap.get(ctrl);
                if (c instanceof Combo) {
                    Combo combo = ((Combo) c);
                    combo.removeAll();
                    if (labels != null) {
                        combo.setItems(labels);
                    }
                }
            }

            @Override
            public boolean isEnabled(@NonNull Ctrl ctrl) {
                Control c = mControlMap.get(ctrl);
                if (c != null) {
                    return c.isEnabled();
                }
                return false;
            }

            @Override
            public void setEnabled(@NonNull Ctrl ctrl, boolean enabled) {
                Control c = mControlMap.get(ctrl);
                if (c != null) {
                    c.setEnabled(enabled);
                }
            }

            @Override
            public boolean isChecked(@NonNull Ctrl ctrl) {
                Control c = mControlMap.get(ctrl);
                if (c instanceof Button) {
                    return ((Button) c).getSelection();
                }
                return false;
            }

            @Override
            public void setChecked(@NonNull Ctrl ctrl, boolean checked) {
                Control c = mControlMap.get(ctrl);
                if (c instanceof Button) {
                    ((Button) c).setSelection(checked);
                }
            }

            @Override
            public String getText(@NonNull Ctrl ctrl) {
                Control c = mControlMap.get(ctrl);
                if (c instanceof Text) {
                    return ((Text) c).getText();
                } else if (c instanceof Combo) {
                    return ((Combo) c).getText();
                } else if (c instanceof Label) {
                    return ((Label) c).getText();
                } else if (c instanceof Button) {
                    return ((Button) c).getText();
                }
                return null;
            }

            @Override
            public void setText(@NonNull Ctrl ctrl, @NonNull String text) {
                Control c = mControlMap.get(ctrl);
                if (c instanceof Text) {
                    ((Text) c).setText(text);
                } else if (c instanceof Combo) {
                    ((Combo) c).setText(text);
                } else if (c instanceof Label) {
                    ((Label) c).setText(text);
                } else if (c instanceof Button) {
                    ((Button) c).setText(text);
                }
            }

            @Override
            public void setImage(@NonNull Ctrl ctrl, @Nullable String imageName) {
                Control c = mControlMap.get(ctrl);
                if (c instanceof Label) {
                    ((Label) c).setImage(
                            imageName == null ? null : mImageFactory.getImageByName(imageName));
                }
            }

            @Nullable
            @Override
            public String openFileDialog(@NonNull String title) {
                FileDialog dlg = new FileDialog(getContents().getShell(), SWT.OPEN);
                dlg.setText(title);
                return dlg.open();
            }

            @Override
            public void repack() {
                mCompositeStatus.pack(true);
                getShell().layout(true, true);
            }

            @Override
            public IMessageBoxLogger newDelayedMessageBoxLog(String title, boolean logErrorsOnly) {
                return new MessageBoxLog(title, getContents().getDisplay(), logErrorsOnly);
            }
        });
    }

    @NonNull
    public AvdCreationPresenter getPresenter() {
        return mPresenter;
    }

    @Override
    protected Control createContents(Composite parent) {
        // super.createContents will call createDialogContent()
        // below and then continue here.
        Control control = super.createContents(parent);
        getShell().setMinimumSize(new Point(350, 600));

        mBtnOK = getButton(IDialogConstants.OK_ID);

        registerControlMap();
        mPresenter.onViewInit();

        return control;
    }

    @Override
    public void createDialogContent(Composite parent) {
        Label label;
        String tooltip;
        ValidateListener validateListener = new ValidateListener();

        // --- avd name
        label = new Label(parent, SWT.NONE);
        label.setText("AVD Name:");
        tooltip = "The name of the Android Virtual Device";
        label.setToolTipText(tooltip);
        mTextAvdName = new Text(parent, SWT.BORDER);
        mTextAvdName.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mTextAvdName.addModifyListener(new CreateNameModifyListener());

        // --- device selection
        label = new Label(parent, SWT.NONE);
        label.setText("Device:");
        tooltip = "The device this AVD will be based on";
        mComboDevice = new Combo(parent, SWT.READ_ONLY | SWT.DROP_DOWN);
        mComboDevice.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mComboDevice.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                mPresenter.onDeviceComboChanged();
            }
        });

        // --- api target
        label = new Label(parent, SWT.NONE);
        label.setText("Target:");
        tooltip = "The target API of the AVD";
        label.setToolTipText(tooltip);
        mComboTarget = new Combo(parent, SWT.READ_ONLY | SWT.DROP_DOWN);
        mComboTarget.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mComboTarget.setToolTipText(tooltip);
        mComboTarget.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                mPresenter.onTargetComboChanged();
            }
        });

        // --- avd ABIs
        label = new Label(parent, SWT.NONE);
        label.setText("CPU/ABI:");
        tooltip = "The CPU/ABI of the virtual device";
        label.setToolTipText(tooltip);
        mComboTagAbi = new Combo(parent, SWT.READ_ONLY | SWT.DROP_DOWN);
        mComboTagAbi.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mComboTagAbi.setToolTipText(tooltip);
        mComboTagAbi.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                mPresenter.onTagComboChanged();
            }
        });

        label = new Label(parent, SWT.NONE);
        label.setText("Keyboard:");
        mCheckKeyboard = new Button(parent, SWT.CHECK);
        mCheckKeyboard.setSelection(true); // default to having a keyboard irrespective of device
        mCheckKeyboard.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mCheckKeyboard.setText("Hardware keyboard present");

        // --- skins
        label = new Label(parent, SWT.NONE);
        label.setText("Skin:");
        mComboSkinCombo = new Combo(parent, SWT.READ_ONLY | SWT.DROP_DOWN);
        mComboSkinCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mComboSkinCombo.addSelectionListener(validateListener);

        // --- camera
        label = new Label(parent, SWT.NONE);
        label.setText("Front Camera:");
        tooltip = "";
        label.setToolTipText(tooltip);
        mComboFrontCamera = new Combo(parent, SWT.READ_ONLY | SWT.DROP_DOWN);
        mComboFrontCamera.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mComboFrontCamera.add("None");
        mComboFrontCamera.add("Emulated");
        mComboFrontCamera.add("Webcam0");
        mComboFrontCamera.select(0);

        label = new Label(parent, SWT.NONE);
        label.setText("Back Camera:");
        tooltip = "";
        label.setToolTipText(tooltip);
        mComboBackCamera = new Combo(parent, SWT.READ_ONLY | SWT.DROP_DOWN);
        mComboBackCamera.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mComboBackCamera.add("None");
        mComboBackCamera.add("Emulated");
        mComboBackCamera.add("Webcam0");
        mComboBackCamera.select(0);

        // --- memory options group
        label = new Label(parent, SWT.NONE);
        label.setText("Memory Options:");

        Group memoryGroup = new Group(parent, SWT.NONE);
        memoryGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        memoryGroup.setLayout(new GridLayout(4, false));

        label = new Label(memoryGroup, SWT.NONE);
        label.setText("RAM:");
        tooltip = "The amount of RAM the emulated device should have in MiB";
        label.setToolTipText(tooltip);
        mTextRam = new Text(memoryGroup, SWT.BORDER);
        mTextRam.addVerifyListener(mDigitVerifier);
        mTextRam.addModifyListener(validateListener);
        mTextRam.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        label = new Label(memoryGroup, SWT.NONE);
        label.setText("VM Heap:");
        tooltip = "The amount of memory, in MiB, available to typical Android applications";
        label.setToolTipText(tooltip);
        mTextVmHeap = new Text(memoryGroup, SWT.BORDER);
        mTextVmHeap.addVerifyListener(mDigitVerifier);
        mTextVmHeap.addModifyListener(validateListener);
        mTextVmHeap.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mTextVmHeap.setToolTipText(tooltip);

        // --- Data partition group
        label = new Label(parent, SWT.NONE);
        label.setText("Internal Storage:");
        tooltip = "The size of the data partition on the device.";
        Group storageGroup = new Group(parent, SWT.NONE);
        storageGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        storageGroup.setLayout(new GridLayout(2, false));
        mTextDataPartition = new Text(storageGroup, SWT.BORDER);
        mTextDataPartition.setText("200");
        mTextDataPartition.addVerifyListener(mDigitVerifier);
        mTextDataPartition.addModifyListener(validateListener);
        mTextDataPartition.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mComboDataPartitionSize = new Combo(storageGroup, SWT.READ_ONLY | SWT.DROP_DOWN);
        mComboDataPartitionSize.add("MiB");
        mComboDataPartitionSize.add("GiB");
        mComboDataPartitionSize.select(0);
        mComboDataPartitionSize.addModifyListener(validateListener);

        // --- sd card group
        label = new Label(parent, SWT.NONE);
        label.setText("SD Card:");
        label.setLayoutData(new GridData(GridData.BEGINNING, GridData.BEGINNING,
                false, false));

        final Group sdCardGroup = new Group(parent, SWT.NONE);
        sdCardGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        sdCardGroup.setLayout(new GridLayout(3, false));

        mRadioSdCardSize = new Button(sdCardGroup, SWT.RADIO);
        mRadioSdCardSize.setText("Size:");
        mRadioSdCardSize.setToolTipText("Create a new SD Card file");
        mRadioSdCardSize.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                mPresenter.onRadioSdCardSizeChanged();
            }
        });

        mTextSdCardSize = new Text(sdCardGroup, SWT.BORDER);
        mTextSdCardSize.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mTextSdCardSize.addVerifyListener(mDigitVerifier);
        mTextSdCardSize.addModifyListener(validateListener);
        mTextSdCardSize.setToolTipText("Size of the new SD Card file (must be at least 9 MiB)");

        mComboSdCardSize = new Combo(sdCardGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
        mComboSdCardSize.add("KiB");
        mComboSdCardSize.add("MiB");
        mComboSdCardSize.add("GiB");
        mComboSdCardSize.select(1);
        mComboSdCardSize.addSelectionListener(validateListener);

        mRadioSdCardFile = new Button(sdCardGroup, SWT.RADIO);
        mRadioSdCardFile.setText("File:");
        mRadioSdCardFile.setToolTipText("Use an existing file for the SD Card");

        mTextSdCardFile = new Text(sdCardGroup, SWT.BORDER);
        mTextSdCardFile.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mTextSdCardFile.addModifyListener(validateListener);
        mTextSdCardFile.setToolTipText("File to use for the SD Card");

        mBtnBrowseSdCard = new Button(sdCardGroup, SWT.PUSH);
        mBtnBrowseSdCard.setText("Browse...");
        mBtnBrowseSdCard.setToolTipText("Select the file to use for the SD Card");
        mBtnBrowseSdCard.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                mPresenter.onBrowseSdCard();
            }
        });

        mRadioSdCardSize.setSelection(true);

        // --- avd options group
        label = new Label(parent, SWT.NONE);
        label.setText("Emulation Options:");
        Group optionsGroup = new Group(parent, SWT.NONE);
        optionsGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        optionsGroup.setLayout(new GridLayout(2, true));
        mCheckSnapshot = new Button(optionsGroup, SWT.CHECK);
        mCheckSnapshot.setText("Snapshot");
        mCheckSnapshot.setToolTipText("Emulator's state will be persisted between emulator executions");
        mCheckSnapshot.addSelectionListener(validateListener);
        mCheckGpuEmulation = new Button(optionsGroup, SWT.CHECK);
        mCheckGpuEmulation.setText("Use Host GPU");
        mCheckGpuEmulation.setToolTipText("Enable hardware OpenGLES emulation");
        mCheckGpuEmulation.addSelectionListener(validateListener);

        // --- force creation group
        mCheckForceCreation = new Button(parent, SWT.CHECK);
        mCheckForceCreation.setText("Override the existing AVD with the same name");
        mCheckForceCreation
                .setToolTipText("There's already an AVD with the same name. Check this to delete it and replace it by the new AVD.");
        mCheckForceCreation.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER,
                true, false, 2, 1));
        mCheckForceCreation.setEnabled(false);
        mCheckForceCreation.addSelectionListener(validateListener);

        // add a separator to separate from the ok/cancel button
        label = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
        label.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false, 3, 1));

        // add stuff for the error display
        mCompositeStatus = new Composite(parent, SWT.NONE);
        mCompositeStatus.setLayoutData(new GridData(GridData.FILL, GridData.CENTER,
                true, false, 3, 1));
        GridLayout gl;
        mCompositeStatus.setLayout(gl = new GridLayout(2, false));
        gl.marginHeight = gl.marginWidth = 0;

        mIconStatus = new Label(mCompositeStatus, SWT.NONE);
        mIconStatus.setLayoutData(new GridData(GridData.BEGINNING, GridData.BEGINNING,
                false, false));
        mTextStatus = new Label(mCompositeStatus, SWT.WRAP);
        GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
        // allow for approx 3 lines of text corresponding to the number of lines in the longest
        // error or warning
        gridData.heightHint = 50;
        mTextStatus.setLayoutData(gridData);
        mTextStatus.setText(""); //$NON-NLS-1$
    }

    private void registerControlMap() {
        mControlMap.put(Ctrl.BUTTON_OK, mBtnOK);
        mControlMap.put(Ctrl.BUTTON_BROWSE_SDCARD, mBtnBrowseSdCard);

        mControlMap.put(Ctrl.COMBO_DEVICE, mComboDevice);
        mControlMap.put(Ctrl.COMBO_TARGET, mComboTarget);
        mControlMap.put(Ctrl.COMBO_TAG_ABI, mComboTagAbi);
        mControlMap.put(Ctrl.COMBO_SKIN, mComboSkinCombo);
        mControlMap.put(Ctrl.COMBO_FRONT_CAM, mComboFrontCamera);
        mControlMap.put(Ctrl.COMBO_BACK_CAM, mComboBackCamera);
        mControlMap.put(Ctrl.COMBO_DATA_PART_SIZE, mComboDataPartitionSize);
        mControlMap.put(Ctrl.COMBO_SDCARD_SIZE, mComboSdCardSize);

        mControlMap.put(Ctrl.CHECK_FORCE_CREATION, mCheckForceCreation);
        mControlMap.put(Ctrl.CHECK_KEYBOARD, mCheckKeyboard);
        mControlMap.put(Ctrl.CHECK_SNAPSHOT, mCheckSnapshot);
        mControlMap.put(Ctrl.CHECK_GPU_EMUL, mCheckGpuEmulation);
        mControlMap.put(Ctrl.RADIO_SDCARD_SIZE, mRadioSdCardSize);
        mControlMap.put(Ctrl.RADIO_SDCARD_FILE, mRadioSdCardFile);

        mControlMap.put(Ctrl.TEXT_AVD_NAME, mTextAvdName);
        mControlMap.put(Ctrl.TEXT_RAM, mTextRam);
        mControlMap.put(Ctrl.TEXT_VM_HEAP, mTextVmHeap);
        mControlMap.put(Ctrl.TEXT_DATA_PART, mTextDataPartition);
        mControlMap.put(Ctrl.TEXT_SDCARD_SIZE, mTextSdCardSize);
        mControlMap.put(Ctrl.TEXT_SDCARD_FILE, mTextSdCardFile);

        mControlMap.put(Ctrl.ICON_STATUS, mIconStatus);
        mControlMap.put(Ctrl.TEXT_STATUS, mTextStatus);
    }

    /**
     * {@link ModifyListener} used for live-validation of the fields content.
     */
    private class ValidateListener extends SelectionAdapter implements ModifyListener {
        @Override
        public void modifyText(ModifyEvent e) {
            mPresenter.validatePage();
        }

        @Override
        public void widgetSelected(SelectionEvent e) {
            super.widgetSelected(e);
            mPresenter.validatePage();
        }
    }

    /**
     * Callback when the AVD name is changed. When creating a new AVD, enables
     * the force checkbox if the name is a duplicate. When editing an existing
     * AVD, it's OK for the name to match the existing AVD.
     */
    private class CreateNameModifyListener implements ModifyListener {
        @Override
        public void modifyText(ModifyEvent e) {
            mPresenter.onAvdNameModified();
        }
    }

    @Override
    public void okPressed() {
        if (mPresenter.createAvd()) {
            super.okPressed();
        }
    }
}

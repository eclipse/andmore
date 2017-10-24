/*
 * Copyright (C) 2009 The Android Open Source Project
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

import com.android.SdkConstants;
import com.android.io.FileWrapper;
import com.android.prefs.AndroidLocation.AndroidLocationException;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.ISystemImage;
import com.android.sdklib.SystemImage;
import com.android.sdklib.internal.avd.AvdInfo;
import com.android.sdklib.internal.avd.AvdManager;
import com.android.sdklib.internal.avd.AvdManager.AvdConflict;
import com.android.sdklib.internal.avd.HardwareProperties;
import com.android.sdklib.internal.avd.HardwareProperties.HardwareProperty;
import com.android.sdklib.internal.project.ProjectProperties;
import com.android.sdklib.repository.descriptors.IdDisplay;
import com.android.sdklib.repository.local.LocalSdk;
import com.android.sdkuilib.internal.repository.icons.ImageFactory;
import com.android.sdkuilib.ui.GridDialog;
import com.android.utils.ILogger;
import com.android.utils.Pair;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.window.Window;
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
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;

/**
 * AVD creation or edit dialog.
 *
 * @deprecated Replaced by {@link AvdCreationDialog}
 */
final class LegacyAvdEditDialog extends GridDialog {

    private static final String ABI_SYS_IMG_DATA_KEY = "systemImagesData";  //$NON-NLS-1$

    private final AvdManager mAvdManager;
    private final TreeMap<String, IAndroidTarget> mCurrentTargets =
        new TreeMap<String, IAndroidTarget>();

    private final Map<String, HardwareProperty> mHardwareMap;
    private final Map<String, String> mProperties = new HashMap<String, String>();
    // a list of user-edited properties.
    private final ArrayList<String> mEditedProperties = new ArrayList<String>();
    private final ImageFactory mImageFactory;
    private final ILogger mSdkLog;
    /**
     * The original AvdInfo if we're editing an existing AVD.
     * Null when we're creating a new AVD.
     */
    private final AvdInfo mEditAvdInfo;

    private Text mAvdName;
    private Combo mTargetCombo;

    private Combo mTagAbiCombo;
    private String mAbiType;

    private Button mSdCardSizeRadio;
    private Text mSdCardSize;
    private Combo mSdCardSizeCombo;

    private Text mSdCardFile;
    private Button mBrowseSdCard;
    private Button mSdCardFileRadio;

    private Button mSnapshotCheck;

    private Button mSkinListRadio;
    private Combo mSkinCombo;

    private Button mSkinSizeRadio;
    private Text mSkinSizeWidth;
    private Text mSkinSizeHeight;

    private TableViewer mHardwareViewer;
    private Button mDeleteHardwareProp;

    private Button mForceCreation;
    private Button mOkButton;
    private Label mStatusIcon;
    private Label mStatusLabel;
    private Composite mStatusComposite;

    /**
     * {@link VerifyListener} for {@link Text} widgets that should only contains numbers.
     */
    private final VerifyListener mDigitVerifier = new VerifyListener() {
        @Override
        public void verifyText(VerifyEvent event) {
            int count = event.text.length();
            for (int i = 0 ; i < count ; i++) {
                char c = event.text.charAt(i);
                if (c < '0' || c > '9') {
                    event.doit = false;
                    return;
                }
            }
        }
    };

    /**
     * Callback when the AVD name is changed.
     * When creating a new AVD, enables the force checkbox if the name is a duplicate.
     * When editing an existing AVD, it's OK for the name to match the existing AVD.
     */
    private class CreateNameModifyListener implements ModifyListener {
        @Override
        public void modifyText(ModifyEvent e) {
            String name = mAvdName.getText().trim();
            if (mEditAvdInfo == null || !name.equals(mEditAvdInfo.getName())) {
                // Case where we're creating a new AVD or editing an existing one
                // and the AVD name has been changed... check for name uniqueness.

                Pair<AvdConflict, String> conflict = mAvdManager.isAvdNameConflicting(name);
                if (conflict.getFirst() != AvdManager.AvdConflict.NO_CONFLICT) {
                    // If we're changing the state from disabled to enabled, make sure
                    // to uncheck the button, to force the user to voluntarily re-enforce it.
                    // This happens when editing an existing AVD and changing the name from
                    // the existing AVD to another different existing AVD.
                    if (!mForceCreation.isEnabled()) {
                        mForceCreation.setEnabled(true);
                        mForceCreation.setSelection(false);
                    }
                } else {
                    mForceCreation.setEnabled(false);
                    mForceCreation.setSelection(false);
                }
            } else {
                // Case where we're editing an existing AVD with the name unchanged.

                mForceCreation.setEnabled(false);
                mForceCreation.setSelection(false);
            }
            validatePage();
        }
    }

    /**
     * {@link ModifyListener} used for live-validation of the fields content.
     */
    private class ValidateListener extends SelectionAdapter implements ModifyListener {
        @Override
        public void modifyText(ModifyEvent e) {
            validatePage();
        }

        @Override
        public void widgetSelected(SelectionEvent e) {
            super.widgetSelected(e);
            validatePage();
        }
    }

    /**
     * Creates the dialog. Caller should then use {@link Window#open()} and
     * refresh if the status is {@link Window#OK}.
     *
     * @param parentShell The parent shell.
     * @param avdManager The existing {@link AvdManager} to use. Must not be null.
     * @param imageFactory An existing {@link ImageFactory} to use. Must not be null.
     * @param log An existing {@link ILogger} where output will go. Must not be null.
     * @param editAvdInfo An optional {@link AvdInfo}. When null, the dialog is used
     *   to create a new AVD. When non-null, the dialog is used to <em>edit</em> this AVD.
     */
    protected LegacyAvdEditDialog(Shell parentShell,
            AvdManager avdManager,
            ImageFactory imageFactory,
            ILogger log,
            AvdInfo editAvdInfo) {
        super(parentShell, 2, false);
        mAvdManager = avdManager;
        mImageFactory = imageFactory;
        mSdkLog = log;
        mEditAvdInfo = editAvdInfo;

        File hardwareDefs = null;

        LocalSdk localSdk = avdManager.getLocalSdk();
        if (localSdk != null) {
            File sdkPath = localSdk.getLocation();
            if (sdkPath != null) {
                hardwareDefs = new File(new File(sdkPath, SdkConstants.OS_SDK_TOOLS_LIB_FOLDER),
                                                          SdkConstants.FN_HARDWARE_INI);
            }
        }

        if (hardwareDefs == null) {
            log.error(null, "Failed to load file %s from SDK", SdkConstants.FN_HARDWARE_INI);
            mHardwareMap = new HashMap<String, HardwareProperty>();
        } else {
            mHardwareMap = HardwareProperties.parseHardwareDefinitions(
                hardwareDefs, null /*sdkLog*/);
        }
    }

    @Override
    public void create() {
        super.create();

        Point p = getShell().getSize();
        if (p.x < 400) {
            p.x = 400;
        }
        getShell().setSize(p);
    }

    @Override
    protected Control createContents(Composite parent) {
        Control control = super.createContents(parent);
        getShell().setText(mEditAvdInfo == null ? "Create new Android Virtual Device (AVD)"
                                                : "Edit Android Virtual Device (AVD)");

        mOkButton = getButton(IDialogConstants.OK_ID);

        fillExistingAvdInfo();
        validatePage();

        return control;
    }

    @Override
    public void createDialogContent(final Composite parent) {
        GridData gd;
        GridLayout gl;

        Label label = new Label(parent, SWT.NONE);
        label.setText("Name:");
        String tooltip = "Name of the new Android Virtual Device";
        label.setToolTipText(tooltip);

        mAvdName = new Text(parent, SWT.BORDER);
        mAvdName.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mAvdName.addModifyListener(new CreateNameModifyListener());
        mAvdName.setToolTipText(tooltip);

        label = new Label(parent, SWT.NONE);
        label.setText("Target:");
        tooltip = "The version of Android to use in the virtual device";
        label.setToolTipText(tooltip);

        mTargetCombo = new Combo(parent, SWT.READ_ONLY | SWT.DROP_DOWN);
        mTargetCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mTargetCombo.setToolTipText(tooltip);
        mTargetCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                super.widgetSelected(e);
                reloadSkinCombo();
                reloadTagAbiCombo();
                validatePage();
            }
        });

        //ABI group
        label = new Label(parent, SWT.NONE);
        label.setText("CPU/ABI:");
        tooltip = "The CPU/ABI to use in the virtual device";
        label.setToolTipText(tooltip);

         mTagAbiCombo = new Combo(parent, SWT.READ_ONLY | SWT.DROP_DOWN);
         mTagAbiCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
         mTagAbiCombo.setToolTipText(tooltip);
         mTagAbiCombo.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
                     super.widgetSelected(e);
                     validatePage();
                 }
         });
         mTagAbiCombo.setEnabled(false);

        // --- sd card group
        label = new Label(parent, SWT.NONE);
        label.setText("SD Card:");
        label.setLayoutData(new GridData(GridData.BEGINNING, GridData.BEGINNING,
                false, false));

        final Group sdCardGroup = new Group(parent, SWT.NONE);
        sdCardGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        sdCardGroup.setLayout(new GridLayout(3, false));

        mSdCardSizeRadio = new Button(sdCardGroup, SWT.RADIO);
        mSdCardSizeRadio.setText("Size:");
        mSdCardSizeRadio.setToolTipText("Create a new SD Card file");
        mSdCardSizeRadio.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                boolean sizeMode = mSdCardSizeRadio.getSelection();
                enableSdCardWidgets(sizeMode);
                validatePage();
            }
        });

        ValidateListener validateListener = new ValidateListener();

        mSdCardSize = new Text(sdCardGroup, SWT.BORDER);
        mSdCardSize.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mSdCardSize.addVerifyListener(mDigitVerifier);
        mSdCardSize.addModifyListener(validateListener);
        mSdCardSize.setToolTipText("Size of the new SD Card file (must be at least 9 MiB)");

        mSdCardSizeCombo = new Combo(sdCardGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
        mSdCardSizeCombo.add("KiB");
        mSdCardSizeCombo.add("MiB");
        mSdCardSizeCombo.add("GiB");
        mSdCardSizeCombo.select(1);
        mSdCardSizeCombo.addSelectionListener(validateListener);

        mSdCardFileRadio = new Button(sdCardGroup, SWT.RADIO);
        mSdCardFileRadio.setText("File:");
        mSdCardFileRadio.setToolTipText("Use an existing file for the SD Card");

        mSdCardFile = new Text(sdCardGroup, SWT.BORDER);
        mSdCardFile.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mSdCardFile.addModifyListener(validateListener);
        mSdCardFile.setToolTipText("File to use for the SD Card");

        mBrowseSdCard = new Button(sdCardGroup, SWT.PUSH);
        mBrowseSdCard.setText("Browse...");
        mBrowseSdCard.setToolTipText("Select the file to use for the SD Card");
        mBrowseSdCard.addSelectionListener(new SelectionAdapter() {
           @Override
            public void widgetSelected(SelectionEvent arg0) {
               onBrowseSdCard();
               validatePage();
            }
        });

        mSdCardSizeRadio.setSelection(true);
        enableSdCardWidgets(true);

        // --- snapshot group

        label = new Label(parent, SWT.NONE);
        label.setText("Snapshot:");
        label.setLayoutData(new GridData(GridData.BEGINNING, GridData.BEGINNING,
            false, false));

        final Group snapshotGroup = new Group(parent, SWT.NONE);
        snapshotGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        snapshotGroup.setLayout(new GridLayout(3, false));

        mSnapshotCheck = new Button(snapshotGroup, SWT.CHECK);
        mSnapshotCheck.setText("Enabled");
        mSnapshotCheck.setToolTipText(
                "Emulator's state will be persisted between emulator executions");

        // --- skin group
        label = new Label(parent, SWT.NONE);
        label.setText("Skin:");
        label.setLayoutData(new GridData(GridData.BEGINNING, GridData.BEGINNING,
                false, false));

        final Group skinGroup = new Group(parent, SWT.NONE);
        skinGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        skinGroup.setLayout(new GridLayout(4, false));

        mSkinListRadio = new Button(skinGroup, SWT.RADIO);
        mSkinListRadio.setText("Built-in:");
        mSkinListRadio.setToolTipText("Select an emulated screen size provided by the current Android target");
        mSkinListRadio.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                boolean listMode = mSkinListRadio.getSelection();
                enableSkinWidgets(listMode);
                validatePage();
            }
        });

        mSkinCombo = new Combo(skinGroup, SWT.READ_ONLY | SWT.DROP_DOWN);
        mSkinCombo.setLayoutData(gd = new GridData(GridData.FILL_HORIZONTAL));
        gd.horizontalSpan = 3;
        mSkinCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                // get the skin info
                loadSkin();
            }
        });

        mSkinSizeRadio = new Button(skinGroup, SWT.RADIO);
        mSkinSizeRadio.setText("Resolution:");
        mSkinSizeRadio.setToolTipText("Select a custom emulated screen size");

        mSkinSizeWidth = new Text(skinGroup, SWT.BORDER);
        mSkinSizeWidth.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mSkinSizeWidth.addVerifyListener(mDigitVerifier);
        mSkinSizeWidth.addModifyListener(validateListener);
        mSkinSizeWidth.setToolTipText("Width in pixels of the emulated screen size");

        new Label(skinGroup, SWT.NONE).setText("x");

        mSkinSizeHeight = new Text(skinGroup, SWT.BORDER);
        mSkinSizeHeight.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mSkinSizeHeight.addVerifyListener(mDigitVerifier);
        mSkinSizeHeight.addModifyListener(validateListener);
        mSkinSizeHeight.setToolTipText("Height in pixels of the emulated screen size");

        mSkinListRadio.setSelection(true);
        enableSkinWidgets(true);

        // --- hardware group
        label = new Label(parent, SWT.NONE);
        label.setText("Hardware:");
        label.setLayoutData(new GridData(GridData.BEGINNING, GridData.BEGINNING,
                false, false));

        final Group hwGroup = new Group(parent, SWT.NONE);
        hwGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        hwGroup.setLayout(new GridLayout(2, false));

        createHardwareTable(hwGroup);

        // composite for the side buttons
        Composite hwButtons = new Composite(hwGroup, SWT.NONE);
        hwButtons.setLayoutData(new GridData(GridData.FILL_VERTICAL));
        hwButtons.setLayout(gl = new GridLayout(1, false));
        gl.marginHeight = gl.marginWidth = 0;

        Button b = new Button(hwButtons, SWT.PUSH | SWT.FLAT);
        b.setText("New...");
        b.setToolTipText("Add a new hardware property");
        b.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        b.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                HardwarePropertyChooser dialog = new HardwarePropertyChooser(parent.getShell(),
                        mHardwareMap, mProperties.keySet());
                if (dialog.open() == Window.OK) {
                    HardwareProperty choice = dialog.getProperty();
                    if (choice != null) {
                        mProperties.put(choice.getName(), choice.getDefault());
                        mHardwareViewer.refresh();
                    }
                }
            }
        });
        mDeleteHardwareProp = new Button(hwButtons, SWT.PUSH | SWT.FLAT);
        mDeleteHardwareProp.setText("Delete");
        mDeleteHardwareProp.setToolTipText("Delete the selected hardware property");
        mDeleteHardwareProp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mDeleteHardwareProp.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                ISelection selection = mHardwareViewer.getSelection();
                if (selection instanceof IStructuredSelection) {
                    String hwName = (String)((IStructuredSelection)selection).getFirstElement();
                    mProperties.remove(hwName);
                    mHardwareViewer.refresh();
                }
            }
        });
        mDeleteHardwareProp.setEnabled(false);

        // --- end hardware group

        mForceCreation = new Button(parent, SWT.CHECK);
        mForceCreation.setText("Override the existing AVD with the same name");
        mForceCreation.setToolTipText("There's already an AVD with the same name. Check this to delete it and replace it by the new AVD.");
        mForceCreation.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER,
                true, false, 2, 1));
        mForceCreation.setEnabled(false);
        mForceCreation.addSelectionListener(validateListener);

        // add a separator to separate from the ok/cancel button
        label = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
        label.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false, 3, 1));

        // add stuff for the error display
        mStatusComposite = new Composite(parent, SWT.NONE);
        mStatusComposite.setLayoutData(new GridData(GridData.FILL, GridData.CENTER,
                true, false, 3, 1));
        mStatusComposite.setLayout(gl = new GridLayout(2, false));
        gl.marginHeight = gl.marginWidth = 0;

        mStatusIcon = new Label(mStatusComposite, SWT.NONE);
        mStatusIcon.setLayoutData(new GridData(GridData.BEGINNING, GridData.BEGINNING,
                false, false));
        mStatusLabel = new Label(mStatusComposite, SWT.NONE);
        mStatusLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mStatusLabel.setText(" \n "); //$NON-NLS-1$

        reloadTargetCombo();
    }

    /**
     * Creates the UI for the hardware properties table.
     * This creates the {@link Table}, and several viewers ({@link TableViewer},
     * {@link TableViewerColumn}) and adds edit support for the 2nd column
     */
    private void createHardwareTable(Composite parent) {
        final Table hardwareTable = new Table(parent, SWT.SINGLE | SWT.FULL_SELECTION);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL);
        gd.widthHint = 200;
        gd.heightHint = 100;
        hardwareTable.setLayoutData(gd);
        hardwareTable.setHeaderVisible(true);
        hardwareTable.setLinesVisible(true);

        // -- Table viewer
        mHardwareViewer = new TableViewer(hardwareTable);
        mHardwareViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                // it's a single selection mode, we can just access the selection index
                // from the table directly.
                mDeleteHardwareProp.setEnabled(hardwareTable.getSelectionIndex() != -1);
            }
        });

        // only a content provider. Use viewers per column below (for editing support)
        mHardwareViewer.setContentProvider(new IStructuredContentProvider() {
            @Override
            public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
                // we can just ignore this. we just use mProperties directly.
            }

            @Override
            public Object[] getElements(Object arg0) {
                return mProperties.keySet().toArray();
            }

            @Override
            public void dispose() {
                // pass
            }
        });

        // -- column 1: prop abstract name
        TableColumn col1 = new TableColumn(hardwareTable, SWT.LEFT);
        col1.setText("Property");
        col1.setWidth(150);
        TableViewerColumn tvc1 = new TableViewerColumn(mHardwareViewer, col1);
        tvc1.setLabelProvider(new CellLabelProvider() {
            @Override
            public void update(ViewerCell cell) {
                String name = cell.getElement().toString();
                HardwareProperty prop = mHardwareMap.get(name);
                if (prop != null) {
                    cell.setText(prop.getAbstract());
                } else {
                    cell.setText(String.format("%1$s (Unknown)", name));
                }
            }
        });

        // -- column 2: prop value
        TableColumn col2 = new TableColumn(hardwareTable, SWT.LEFT);
        col2.setText("Value");
        col2.setWidth(50);
        TableViewerColumn tvc2 = new TableViewerColumn(mHardwareViewer, col2);
        tvc2.setLabelProvider(new CellLabelProvider() {
            @Override
            public void update(ViewerCell cell) {
                String value = mProperties.get(cell.getElement());
                cell.setText(value != null ? value : "");
            }
        });

        // add editing support to the 2nd column
        tvc2.setEditingSupport(new EditingSupport(mHardwareViewer) {
            @Override
            protected void setValue(Object element, Object value) {
                String hardwareName = (String)element;
                HardwareProperty property = mHardwareMap.get(hardwareName);
                int index;
                switch (property.getType()) {
                    case INTEGER:
                        mProperties.put((String)element, (String)value);
                        break;
                    case DISKSIZE:
                        if (HardwareProperties.DISKSIZE_PATTERN.matcher((String)value).matches()) {
                            mProperties.put((String)element, (String)value);
                        }
                        break;
                    case BOOLEAN:
                        index = (Integer)value;
                        mProperties.put((String)element, HardwareProperties.getBooleanValue(index));
                        break;
                    case STRING_ENUM:
                    case INTEGER_ENUM:
                        // For a combo, value is the index of the enum to use.
                        index = (Integer)value;
                        String[] values = property.getEnum();
                        if (values != null && values.length > index) {
                            mProperties.put((String)element, values[index]);
                        }
                        break;
                }
                mHardwareViewer.refresh(element);
            }

            @Override
            protected Object getValue(Object element) {
                String hardwareName = (String)element;
                HardwareProperty property = mHardwareMap.get(hardwareName);
                String value = mProperties.get(hardwareName);
                switch (property.getType()) {
                    case INTEGER:
                        // intended fall-through.
                    case DISKSIZE:
                        return value;
                    case BOOLEAN:
                        return HardwareProperties.getBooleanValueIndex(value);
                    case STRING_ENUM:
                    case INTEGER_ENUM:
                        // For a combo, we need to return the index of the value in the enum
                        String[] values = property.getEnum();
                        if (values != null) {
                            for (int i = 0; i < values.length; i++) {
                                if (values[i].equals(value)) {
                                    return i;
                                }
                            }
                        }
                }

                return null;
            }

            @Override
            protected CellEditor getCellEditor(Object element) {
                String hardwareName = (String)element;
                HardwareProperty property = mHardwareMap.get(hardwareName);
                switch (property.getType()) {
                    // TODO: custom TextCellEditor that restrict input.
                    case INTEGER:
                        // intended fall-through.
                    case DISKSIZE:
                        return new TextCellEditor(hardwareTable);
                    case BOOLEAN:
                        return new ComboBoxCellEditor(hardwareTable,
                                new String[] {
                                    HardwareProperties.getBooleanValue(0),
                                    HardwareProperties.getBooleanValue(1),
                                },
                                SWT.READ_ONLY | SWT.DROP_DOWN);
                    case STRING_ENUM:
                    case INTEGER_ENUM:
                        String[] values = property.getEnum();
                        if (values != null && values.length > 0) {
                            return new ComboBoxCellEditor(hardwareTable,
                                    values,
                                    SWT.READ_ONLY | SWT.DROP_DOWN);
                        }
                }
                return null;
            }

            @Override
            protected boolean canEdit(Object element) {
                String hardwareName = (String)element;
                HardwareProperty property = mHardwareMap.get(hardwareName);
                return property != null;
            }
        });


        mHardwareViewer.setInput(mProperties);
    }

    // -- Start of internal part ----------
    // Hide everything down-below from SWT designer
    //$hide>>$

    /**
     * When editing an existing AVD info, fill the UI that has just been created with
     * the values from the AVD.
     */
    public void fillExistingAvdInfo() {
        if (mEditAvdInfo == null) {
            return;
        }

        mAvdName.setText(mEditAvdInfo.getName());

        Map<String, String> props = mEditAvdInfo.getProperties();

        IAndroidTarget target = mEditAvdInfo.getTarget();
        if (target != null && !mCurrentTargets.isEmpty()) {
            // Try to select the target in the target combo.
            // This will fail if the AVD needs to be repaired.
            //
            // This is a linear search but the list is always
            // small enough and we only do this once.
            int n = mTargetCombo.getItemCount();
            for (int i = 0;i < n; i++) {
                if (target.equals(mCurrentTargets.get(mTargetCombo.getItem(i)))) {
                    mTargetCombo.select(i);
                    reloadTagAbiCombo();
                    reloadSkinCombo();
                    break;
                }
            }
        }

        // select the abi type
        ISystemImage[] systemImages = getSystemImages(target);
        if (target != null && systemImages.length > 0) {
            mTagAbiCombo.setEnabled(systemImages.length > 1);
            String abiType = AvdInfo.getPrettyAbiType(mEditAvdInfo);
            int n = mTagAbiCombo.getItemCount();
            for (int i = 0; i < n; i++) {
                if (abiType.equals(mTagAbiCombo.getItem(i))) {
                    mTagAbiCombo.select(i);
                    reloadSkinCombo();
                    break;
                }
            }
        }

        if (props != null) {

            // First try the skin name and if it doesn't work fallback on the skin path
            nextSkin: for (int s = 0; s < 2; s++) {
                String skin = props.get(s == 0 ? AvdManager.AVD_INI_SKIN_NAME
                                               : AvdManager.AVD_INI_SKIN_PATH);
                if (skin != null && skin.length() > 0) {
                    Matcher m = AvdManager.NUMERIC_SKIN_SIZE.matcher(skin);
                    if (m.matches() && m.groupCount() == 2) {
                        enableSkinWidgets(false);
                        mSkinListRadio.setSelection(false);
                        mSkinSizeRadio.setSelection(true);
                        mSkinSizeWidth.setText(m.group(1));
                        mSkinSizeHeight.setText(m.group(2));
                        break nextSkin;
                    } else {
                        enableSkinWidgets(true);
                        mSkinSizeRadio.setSelection(false);
                        mSkinListRadio.setSelection(true);

                        int n = mSkinCombo.getItemCount();
                        for (int i = 0; i < n; i++) {
                            if (skin.equals(mSkinCombo.getItem(i))) {
                                mSkinCombo.select(i);
                                break nextSkin;
                            }
                        }
                    }
                }
            }

            String sdcard = props.get(AvdManager.AVD_INI_SDCARD_PATH);
            if (sdcard != null && sdcard.length() > 0) {
                enableSdCardWidgets(false);
                mSdCardSizeRadio.setSelection(false);
                mSdCardFileRadio.setSelection(true);
                mSdCardFile.setText(sdcard);
            }

            sdcard = props.get(AvdManager.AVD_INI_SDCARD_SIZE);
            if (sdcard != null && sdcard.length() > 0) {
                String[] values = new String[2];
                long sdcardSize = AvdManager.parseSdcardSize(sdcard, values);

                if (sdcardSize != AvdManager.SDCARD_NOT_SIZE_PATTERN) {
                    enableSdCardWidgets(true);
                    mSdCardFileRadio.setSelection(false);
                    mSdCardSizeRadio.setSelection(true);

                    mSdCardSize.setText(values[0]);

                    String suffix = values[1];
                    int n = mSdCardSizeCombo.getItemCount();
                    for (int i = 0; i < n; i++) {
                        if (mSdCardSizeCombo.getItem(i).startsWith(suffix)) {
                            mSdCardSizeCombo.select(i);
                        }
                    }
                }
            }

            String snapshots = props.get(AvdManager.AVD_INI_SNAPSHOT_PRESENT);
            if (snapshots != null && snapshots.length() > 0) {
                mSnapshotCheck.setSelection(snapshots.equals("true"));
            }
        }

        mProperties.clear();

        if (props != null) {
            for (Entry<String, String> entry : props.entrySet()) {
                HardwareProperty prop = mHardwareMap.get(entry.getKey());
                if (prop != null && prop.isValidForUi()) {
                    mProperties.put(entry.getKey(), entry.getValue());
                }
            }
        }

        // Cleanup known non-hardware properties
        mProperties.remove(AvdManager.AVD_INI_ABI_TYPE);
        mProperties.remove(AvdManager.AVD_INI_CPU_ARCH);
        mProperties.remove(AvdManager.AVD_INI_SKIN_PATH);
        mProperties.remove(AvdManager.AVD_INI_SKIN_NAME);
        mProperties.remove(AvdManager.AVD_INI_SDCARD_SIZE);
        mProperties.remove(AvdManager.AVD_INI_SDCARD_PATH);
        mProperties.remove(AvdManager.AVD_INI_SNAPSHOT_PRESENT);
        mProperties.remove(AvdManager.AVD_INI_IMAGES_1);
        mProperties.remove(AvdManager.AVD_INI_IMAGES_2);

        mHardwareViewer.refresh();
    }

    @Override
    protected void okPressed() {
        if (createAvd()) {
            super.okPressed();
        }
    }

    /**
     * Enable or disable the sd card widgets.
     * @param sizeMode if true the size-based widgets are to be enabled, and the file-based ones
     * disabled.
     */
    private void enableSdCardWidgets(boolean sizeMode) {
        mSdCardSize.setEnabled(sizeMode);
        mSdCardSizeCombo.setEnabled(sizeMode);

        mSdCardFile.setEnabled(!sizeMode);
        mBrowseSdCard.setEnabled(!sizeMode);
    }

    /**
     * Enable or disable the skin widgets.
     * @param listMode if true the list-based widgets are to be enabled, and the size-based ones
     * disabled.
     */
    private void enableSkinWidgets(boolean listMode) {
        mSkinCombo.setEnabled(listMode);

        mSkinSizeWidth.setEnabled(!listMode);
        mSkinSizeHeight.setEnabled(!listMode);
    }


    private void onBrowseSdCard() {
        FileDialog dlg = new FileDialog(getContents().getShell(), SWT.OPEN);
        dlg.setText("Choose SD Card image file.");

        String fileName = dlg.open();
        if (fileName != null) {
            mSdCardFile.setText(fileName);
        }
    }



    private void reloadTargetCombo() {
        String selected = null;
        int index = mTargetCombo.getSelectionIndex();
        if (index >= 0) {
            selected = mTargetCombo.getItem(index);
        }

        mCurrentTargets.clear();
        mTargetCombo.removeAll();

        boolean found = false;
        index = -1;

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
                mTargetCombo.add(name);
                if (!found) {
                    index++;
                    found = name.equals(selected);
                }
            }
        }

        mTargetCombo.setEnabled(mCurrentTargets.size() > 0);

        if (found) {
            mTargetCombo.select(index);
        }

        reloadSkinCombo();
    }

    private void reloadSkinCombo() {
        String selected = null;
        int index = mSkinCombo.getSelectionIndex();
        if (index >= 0) {
            selected = mSkinCombo.getItem(index);
        }

        mSkinCombo.removeAll();
        mSkinCombo.setEnabled(false);

        index = mTargetCombo.getSelectionIndex();
        if (index >= 0) {

            String targetName = mTargetCombo.getItem(index);

            boolean found = false;
            IAndroidTarget target = mCurrentTargets.get(targetName);
            if (target != null) {
                mSkinCombo.add(String.format("Default (%s)", target.getDefaultSkin()));

                index = -1;
                for (File skin : target.getSkins()) {
                    mSkinCombo.add(skin.getName());
                    if (!found) {
                        index++;
                        found = skin.equals(selected);
                    }
                }

                mSkinCombo.setEnabled(true);

                if (found) {
                    mSkinCombo.select(index);
                } else {
                    mSkinCombo.select(0);  // default
                    loadSkin();
                }
            }
        }
    }

    /**
    * Reload all the abi types in the selection list
    */
    private void reloadTagAbiCombo() {
       String selected = null;
       boolean found = false;

       int index = mTargetCombo.getSelectionIndex();
       if (index >= 0) {
           String targetName = mTargetCombo.getItem(index);
           IAndroidTarget target = mCurrentTargets.get(targetName);

           ISystemImage[] systemImages = getSystemImages(target);

           // keep a reference to the array into the combo app data field
           // so that we can lookup the tag/abi later in getSelectedAbiType()
           mTagAbiCombo.setData(ABI_SYS_IMG_DATA_KEY, systemImages);

           mTagAbiCombo.setEnabled(systemImages.length > 1);

           // If user explicitly selected an ABI before, preserve that option
           // If user did not explicitly select before (only one option before)
           // force them to select
           index = mTagAbiCombo.getSelectionIndex();
           if (index >= 0 && mTagAbiCombo.getItemCount() > 1) {
               selected = mTagAbiCombo.getItem(index);
           }

           mTagAbiCombo.removeAll();

           int i;
           for ( i = 0; i < systemImages.length ; i++ ) {
               String prettyAbiType = AvdInfo.getPrettyAbiType(systemImages[i]);
               mTagAbiCombo.add(prettyAbiType);
               if (!found) {
                   found = prettyAbiType.equals(selected);
                   if (found) {
                       mTagAbiCombo.select(i);
                   }
               }
           }

           if (systemImages.length == 1) {
               mTagAbiCombo.select(0);
           }
       }
    }

    /**
     * Validates the fields, displays errors and warnings.
     * Enables the finish button if there are no errors.
     */
    private void validatePage() {
        String error = null;
        String warning = null;

        // Validate AVD name
        String avdName = mAvdName.getText().trim();
        boolean hasAvdName = avdName.length() > 0;
        boolean isCreate = mEditAvdInfo == null || !avdName.equals(mEditAvdInfo.getName());

        if (hasAvdName && !AvdManager.RE_AVD_NAME.matcher(avdName).matches()) {
            error = String.format(
                "AVD name '%1$s' contains invalid characters.\nAllowed characters are: %2$s",
                avdName, AvdManager.CHARS_AVD_NAME);
        }

        // Validate target
        if (hasAvdName && error == null && mTargetCombo.getSelectionIndex() < 0) {
            error = "A target must be selected in order to create an AVD.";
        }

        // validate abi type if the selected target supports multi archs.
        if (hasAvdName && error == null && mTargetCombo.getSelectionIndex() > 0) {
            int index = mTargetCombo.getSelectionIndex();
            String targetName = mTargetCombo.getItem(index);
            IAndroidTarget target = mCurrentTargets.get(targetName);
            ISystemImage[] systemImages = getSystemImages(target);
            if (systemImages.length > 1 && mTagAbiCombo.getSelectionIndex() < 0) {
               error = "An ABI type must be selected in order to create an AVD.";
            }
        }

        // Validate SDCard path or value
        if (error == null) {
            // get the mode. We only need to check the file since the
            // verifier on the size Text will prevent invalid input
            boolean sdcardFileMode = mSdCardFileRadio.getSelection();
            if (sdcardFileMode) {
                String sdName = mSdCardFile.getText().trim();
                if (sdName.length() > 0 && !new File(sdName).isFile()) {
                    error = "SD Card path isn't valid.";
                }
            } else {
                String valueString = mSdCardSize.getText();
                if (valueString.length() > 0) {
                    long value = 0;
                    try {
                        value = Long.parseLong(valueString);

                        int sizeIndex = mSdCardSizeCombo.getSelectionIndex();
                        if (sizeIndex >= 0) {
                            // index 0 shifts by 10 (1024=K), index 1 by 20, etc.
                            value <<= 10*(1 + sizeIndex);
                        }

                        if (value < AvdManager.SDCARD_MIN_BYTE_SIZE ||
                                value > AvdManager.SDCARD_MAX_BYTE_SIZE) {
                            value = 0;
                        }
                    } catch (Exception e) {
                        // ignore, we'll test value below.
                    }
                    if (value <= 0) {
                        error = "SD Card size is invalid. Range is 9 MiB..1023 GiB.";
                    } else if (mEditAvdInfo != null) {
                        // When editing an existing AVD, compare with the existing
                        // sdcard size, if any. It only matters if there was an sdcard setting
                        // before.
                        Map<String, String> props = mEditAvdInfo.getProperties();
                        if (props != null) {
                            String original =
                                mEditAvdInfo.getProperties().get(AvdManager.AVD_INI_SDCARD_SIZE);
                            if (original != null && original.length() > 0) {
                                long originalSize =
                                    AvdManager.parseSdcardSize(original, null/*parsedStrings*/);
                                if (originalSize > 0 && value != originalSize) {
                                    warning = "A new SD Card file will be created.\nThe current SD Card file will be lost.";
                                }
                            }
                        }
                    }
                }
            }
        }

        // validate the skin
        if (error == null) {
            // get the mode, we should only check if it's in size mode since
            // the built-in list mode is always valid.
            if (mSkinSizeRadio.getSelection()) {
                // need both with and heigh to be non null
                String width = mSkinSizeWidth.getText();   // no need for trim, since the verifier
                String height = mSkinSizeHeight.getText(); // rejects non digit.

                if (width.length() == 0 || height.length() == 0) {
                    error = "Skin size is incorrect.\nBoth dimensions must be > 0.";
                }
            }
        }

        // Check for duplicate AVD name
        if (isCreate && hasAvdName && error == null && !mForceCreation.getSelection()) {
            Pair<AvdConflict, String> conflict = mAvdManager.isAvdNameConflicting(avdName);
            assert conflict != null;
            switch(conflict.getFirst()) {
            case NO_CONFLICT:
                break;
            case CONFLICT_EXISTING_AVD:
            case CONFLICT_INVALID_AVD:
                error = String.format(
                        "The AVD name '%s' is already used.\n" +
                        "Check \"Override the existing AVD\" to delete the existing one.",
                        avdName);
                break;
            case CONFLICT_EXISTING_PATH:
                error = String.format(
                        "Conflict with %s\n" +
                        "Check \"Override the existing AVD\" to delete the existing one.",
                        conflict.getSecond());
                break;
            default:
                // Hmm not supposed to happen... probably someone expanded the
                // enum without adding something here. In this case just do an
                // assert and use a generic error message.
                error = String.format(
                        "Conflict %s with %s.\n" +
                        "Check \"Override the existing AVD\" to delete the existing one.",
                        conflict.getFirst().toString(),
                        conflict.getSecond());
                assert false;
                break;
            }
        }

        if (error == null && mEditAvdInfo != null && isCreate) {
            warning = String.format("The AVD '%1$s' will be duplicated into '%2$s'.",
                    mEditAvdInfo.getName(),
                    avdName);
        }

        // Validate the create button
        boolean can_create = hasAvdName && error == null;
        if (can_create) {
            can_create &= mTargetCombo.getSelectionIndex() >= 0;
        }
        mOkButton.setEnabled(can_create);

        // Adjust the create button label as needed
        if (isCreate) {
            mOkButton.setText("Create AVD");
        } else {
            mOkButton.setText("Edit AVD");
        }

        // -- update UI
        if (error != null) {
            mStatusIcon.setImage(mImageFactory.getImageByName("reject_icon16.png"));  //$NON-NLS-1$
            mStatusLabel.setText(error);
        } else if (warning != null) {
            mStatusIcon.setImage(mImageFactory.getImageByName("warning_icon16.png"));  //$NON-NLS-1$
            mStatusLabel.setText(warning);
        } else {
            mStatusIcon.setImage(null);
            mStatusLabel.setText(" \n "); //$NON-NLS-1$
        }

        mStatusComposite.pack(true);
    }

    private void loadSkin() {
        int targetIndex = mTargetCombo.getSelectionIndex();
        if (targetIndex < 0) {
            return;
        }

        // resolve the target.
        String targetName = mTargetCombo.getItem(targetIndex);
        IAndroidTarget target = mCurrentTargets.get(targetName);
        if (target == null) {
            return;
        }

        // get the skin name
        String skinName = null;
        int skinIndex = mSkinCombo.getSelectionIndex();
        if (skinIndex < 0) {
            return;
        } else if (skinIndex == 0) { // default skin for the target
            File skin = target.getDefaultSkin();
            if (skin != null) {
                skinName = skin.getName();
            }
        } else {
            skinName = mSkinCombo.getItem(skinIndex);
        }

        // load the skin properties
        String path = target.getPath(IAndroidTarget.SKINS);
        File skin = new File(path, skinName);
        if (skin.isDirectory() == false && target.isPlatform() == false) {
            // it's possible the skin is in the parent target
            path = target.getParent().getPath(IAndroidTarget.SKINS);
            skin = new File(path, skinName);
        }

        if (skin.isDirectory() == false) {
            return;
        }

        // now get the hardware.ini from the add-on (if applicable) and from the skin
        // (if applicable)
        HashMap<String, String> hardwareValues = new HashMap<String, String>();
        if (target.isPlatform() == false) {
            FileWrapper targetHardwareFile = new FileWrapper(target.getLocation(),
                    AvdManager.HARDWARE_INI);
            if (targetHardwareFile.isFile()) {
                Map<String, String> targetHardwareConfig = ProjectProperties.parsePropertyFile(
                        targetHardwareFile, null /*log*/);

                if (targetHardwareConfig != null) {
                    hardwareValues.putAll(targetHardwareConfig);
                }
            }
        }

        // from the skin
        FileWrapper skinHardwareFile = new FileWrapper(skin, AvdManager.HARDWARE_INI);
        if (skinHardwareFile.isFile()) {
            Map<String, String> skinHardwareConfig = ProjectProperties.parsePropertyFile(
                    skinHardwareFile, null /*log*/);

            if (skinHardwareConfig != null) {
                hardwareValues.putAll(skinHardwareConfig);
            }
        }

        // now set those values in the list of properties for the AVD.
        // We just check that none of those properties have been edited by the user yet.
        for (Entry<String, String> entry : hardwareValues.entrySet()) {
            if (mEditedProperties.contains(entry.getKey()) == false) {
                mProperties.put(entry.getKey(), entry.getValue());
            }
        }

        mHardwareViewer.refresh();
    }

    /**
     * Creates an AVD from the values in the UI. Called when the user presses the OK button.
     */
    private boolean createAvd() {
        String avdName = mAvdName.getText().trim();
        int index = mTargetCombo.getSelectionIndex();

        // quick check on the name and the target selection
        if (avdName.length() == 0 || index < 0) {
            return false;
        }

        // resolve the target.
        String targetName = mTargetCombo.getItem(index);
        IAndroidTarget target = mCurrentTargets.get(targetName);
        if (target == null) {
            return false;
        }

        // get the tag & abi type
        IdDisplay tag = SystemImage.DEFAULT_TAG;
        mAbiType = SdkConstants.ABI_ARMEABI;
        Object appData = mTagAbiCombo.getData(ABI_SYS_IMG_DATA_KEY);
        if (appData instanceof ISystemImage[]) {
            int abiIndex = mTagAbiCombo.getSelectionIndex();
            ISystemImage[] systemImages = (ISystemImage[]) appData;
            if (abiIndex >= 0 && abiIndex < systemImages.length) {
                ISystemImage systemImage = systemImages[abiIndex];
                tag = systemImage.getTag();
                mAbiType = systemImage.getAbiType();
            }
        }

        // get the SD card data from the UI.
        String sdName = null;
        if (mSdCardSizeRadio.getSelection()) {
            // size mode
            String value = mSdCardSize.getText().trim();
            if (value.length() > 0) {
                sdName = value;
                // add the unit
                switch (mSdCardSizeCombo.getSelectionIndex()) {
                    case 0:
                        sdName += "K";  //$NON-NLS-1$
                        break;
                    case 1:
                        sdName += "M";  //$NON-NLS-1$
                        break;
                    case 2:
                        sdName += "G";  //$NON-NLS-1$
                        break;
                    default:
                        // shouldn't be here
                        assert false;
                }
            }
        } else {
            // file mode.
            sdName = mSdCardFile.getText().trim();
        }

        // get the Skin data from the UI
        String skinName = null;
        if (mSkinListRadio.getSelection()) {
            // built-in list provides the skin
            int skinIndex = mSkinCombo.getSelectionIndex();
            if (skinIndex > 0) {
                // index 0 is the default, we don't use it
                skinName = mSkinCombo.getItem(skinIndex);
            }
        } else {
            // size mode. get both size and writes it as a skin name
            // thanks to validatePage() we know the content of the fields is correct
            skinName = mSkinSizeWidth.getText() + "x" + mSkinSizeHeight.getText(); //$NON-NLS-1$
        }

        ILogger log = mSdkLog;
        if (log == null || log instanceof MessageBoxLog) {
            // If the current logger is a message box, we use our own (to make sure
            // to display errors right away and customize the title).
            log = new MessageBoxLog(
                    String.format("Result of creating AVD '%s':", avdName),
                    getContents().getDisplay(),
                    false /*logErrorsOnly*/);
        }

        File avdFolder = null;
        try {
            avdFolder = AvdInfo.getDefaultAvdFolder(mAvdManager, avdName);
        } catch (AndroidLocationException e) {
            return false;
        }

        boolean force = mForceCreation.getSelection();
        boolean snapshot = mSnapshotCheck.getSelection();

        boolean success = false;
        AvdInfo avdInfo = mAvdManager.createAvd(
                avdFolder,
                avdName,
                target,
                tag,
                mAbiType,
                null, //skinFolder
                skinName,
                sdName,
                mProperties,
                null, // bootProps
                snapshot,
                force,
                mEditAvdInfo != null, //edit existing
                log);

        success = avdInfo != null;

        if (log instanceof MessageBoxLog) {
            ((MessageBoxLog) log).displayResult(success);
        }
        return success;
    }

    /**
     * Returns the list of system images of a target.
     * <p/>
     * If target is null, returns an empty list.
     * If target is an add-on with no system images, return the list from its parent platform.
     *
     * @param target An IAndroidTarget. Can be null.
     * @return A non-null ISystemImage array. Can be empty.
     */
    private ISystemImage[] getSystemImages(IAndroidTarget target) {
        if (target != null) {
            ISystemImage[] images = target.getSystemImages();

            if ((images == null || images.length == 0) && !target.isPlatform()) {
                // If an add-on does not provide any system images, use the ones from the parent.
                images = target.getParent().getSystemImages();
            }

            if (images != null) {
                return images;
            }
        }

        return new ISystemImage[0];
    }

    // End of hiding from SWT Designer
    //$hide<<$
}

/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.sdkuilib.internal.repository.ui;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.sdklib.SystemImage;
import com.android.sdklib.devices.Device;
import com.android.sdklib.devices.DeviceManager;
import com.android.sdklib.devices.DeviceManager.DeviceFilter;
import com.android.sdklib.devices.DeviceManager.DevicesChangedListener;
import com.android.sdklib.devices.Hardware;
import com.android.sdklib.devices.Screen;
import com.android.sdklib.devices.Storage;
import com.android.sdklib.devices.Storage.Unit;
import com.android.sdklib.internal.avd.AvdInfo;
import com.android.sdklib.repository.ISdkChangeListener;
import com.android.sdkuilib.internal.repository.SwtUpdaterData;
import com.android.sdkuilib.internal.repository.icons.ImageFactory;
import com.android.sdkuilib.internal.repository.icons.ImageFactory.Filter;
import com.android.sdkuilib.internal.widgets.AvdCreationDialog;
import com.android.sdkuilib.internal.widgets.AvdSelector;
import com.android.sdkuilib.internal.widgets.DeviceCreationDialog;
import com.android.sdkuilib.ui.GridDataBuilder;
import com.android.sdkuilib.ui.GridLayoutBuilder;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Resource;
import org.eclipse.swt.graphics.TextLayout;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A page displaying Device Manager entries.
 * <p/>
 * This is displayed as a second tab in the AVD Manager window.
 * The layout purposely matches the one from {@link AvdManagerPage} and {@link AvdSelector}
 * so that there's a good consistency when switching tabs.
 * The table displays a few properties of each device as well as actions to edit/add/delete
 * devices and a button to create an AVD from a given device.
 *
 * Non-goals: this tries to keep it simple for a first iteration. Possible enhancements:
 * - a way to sort the device list by name, manufacturer or screen size.
 * - possibly a tree organized by manufacturer.
 * - a filter box to do a string search on any part of the display.
 */
public class DeviceManagerPage extends Composite
    implements ISdkChangeListener, DevicesChangedListener, DisposeListener {

    public interface IAvdCreatedListener {
        public void onAvdCreated(AvdInfo createdAvdInfo);
    }

    private final SwtUpdaterData mSwtUpdaterData;
    private final DeviceManager mDeviceManager;
    private Table mTable;
    private Button mNewButton;
    private Button mEditButton;
    private Button mDeleteButton;
    private Button mNewAvdButton;
    private Button mRefreshButton;
    private ImageFactory mImageFactory;
    private Image mUserImage;
    private Image mDeviceImage;
    private int mImageWidth;
    private boolean mDisableRefresh;
    private IAvdCreatedListener mAvdCreatedListener;
    private final Filter mUserColorFilter = new Filter() {
        @Override
        public Image filter(Image source) {
            ImageData srcData = source.getImageData();

            // swap green and blue
            PaletteData p = srcData.palette;
            int b = p.blueMask;
            p.blueMask = p.greenMask;
            p.greenMask = b;

            b = p.blueShift;
            p.blueShift = p.greenShift;
            p.greenShift = b;

            return new Image(source.getDevice(), srcData);
        }
    };

    /**
     * Create the composite.
     * @param parent The parent of the composite.
     * @param swtUpdaterData An instance of {@link SwtUpdaterData}.
     */
    public DeviceManagerPage(Composite parent,
            int swtStyle,
            SwtUpdaterData swtUpdaterData,
            DeviceManager deviceManager) {
        super(parent, swtStyle);

        mSwtUpdaterData = swtUpdaterData;
        mSwtUpdaterData.addListeners(this);

        mDeviceManager = deviceManager;
        mDeviceManager.registerListener(this);

        createContents(this);
        postCreate();  //$hide$
    }

    public void setAvdCreatedListener(IAvdCreatedListener avdCreatedListener) {
        mAvdCreatedListener = avdCreatedListener;
    }

    private void createContents(Composite parent) {

        // get some bitmaps.
        mImageFactory = new ImageFactory(parent.getDisplay());
        mUserImage = getTagImage(null /*tag*/, true /*isUser*/);
        mDeviceImage = getTagImage(null /*tag*/, false /*isUser*/);
        mImageWidth = Math.max(mDeviceImage.getImageData().width,
                        Math.max(mUserImage.getImageData().width,
                                mDeviceImage.getImageData().width));

        // Layout has 2 columns
        GridLayoutBuilder.create(parent).columns(2);

        // Insert a top label explanation. This matches the design in AvdManagerPage so
        // that the table starts at the same height on both tabs.
        Label label = new Label(parent, SWT.NONE);
        label.setText("List of known device definitions. This can later be used to create Android Virtual Devices.");
        GridDataBuilder.create(label).hSpan(2);

        // Device table.
        mTable = new Table(parent, SWT.FULL_SELECTION | SWT.SINGLE | SWT.BORDER);
        mTable.setHeaderVisible(true);
        mTable.setLinesVisible(true);
        mTable.setFont(parent.getFont());
        setTableHeightHint(30);

        // Buttons on the side.
        Composite buttons = new Composite(parent, SWT.NONE);
        GridLayoutBuilder.create(buttons).columns(1).noMargins();
        GridDataBuilder.create(buttons).vFill();
        buttons.setFont(parent.getFont());

        mNewAvdButton = new Button(buttons, SWT.PUSH | SWT.FLAT);
        mNewAvdButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mNewAvdButton.setText("Create AVD...");
        mNewAvdButton.setToolTipText("Creates a new AVD based on this device definition.");
        mNewAvdButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                onCreateAvd();
            }
        });

        @SuppressWarnings("unused")
        Label spacing = new Label(buttons, SWT.NONE);

        mNewButton = new Button(buttons, SWT.PUSH | SWT.FLAT);
        mNewButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mNewButton.setText("Create Device...");
        mNewButton.setToolTipText("Creates a new user device definition.");
        mNewButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                onNewDevice();
            }
        });

        mEditButton = new Button(buttons, SWT.PUSH | SWT.FLAT);
        mEditButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mEditButton.setText("Edit...");
        mEditButton.setToolTipText("Edit an existing device definition.");
        mEditButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                onEditDevice();
            }
        });

        mDeleteButton = new Button(buttons, SWT.PUSH | SWT.FLAT);
        mDeleteButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mDeleteButton.setText("Delete...");
        mDeleteButton.setToolTipText("Deletes the selected AVD.");
        mDeleteButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                onDeleteDevice();
            }
        });

        Composite padding = new Composite(buttons, SWT.NONE);
        padding.setLayoutData(new GridData(GridData.FILL_VERTICAL));

        mRefreshButton = new Button(buttons, SWT.PUSH | SWT.FLAT);
        mRefreshButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mRefreshButton.setText("Refresh");
        mRefreshButton.setToolTipText("Reloads the list of devices.");
        mRefreshButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                onRefresh();
            }
        });

        // Legend at the bottom.
        // This matches the one on AvdSelector so that the table height in the tab be similar.
        Composite legend = new Composite(parent, SWT.NONE);
        GridLayoutBuilder.create(legend).columns(4).noMargins();
        GridDataBuilder.create(legend).hFill().vTop().hGrab().hSpan(2);
        legend.setFont(parent.getFont());

        new Label(legend, SWT.NONE).setImage(mUserImage);
        new Label(legend, SWT.NONE).setText("A user-created device definition.");
        new Label(legend, SWT.NONE).setImage(mDeviceImage);
        new Label(legend, SWT.NONE).setText("A generic device definition.");

        // create the table columns
        final TableColumn column0 = new TableColumn(mTable, SWT.NONE);
        column0.setText("Device");

        adjustColumnsWidth(mTable, column0);
        setupSelectionListener(mTable);
        fillTable(mTable);
        updateButtonStates();
        setEnabled(true);
    }

    private void adjustColumnsWidth(final Table table, final TableColumn column0) {
        // Add a listener to resize the column to the full width of the table
        table.addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {
                Rectangle r = table.getClientArea();
                column0.setWidth(r.width * 100 / 100 - 1); // 100%
            }
        });
    }

    private void setupSelectionListener(Table table) {
        // TODO Auto-generated method stub

    }

    /**
     * Sets the table grid layout data.
     *
     * @param heightHint If > 0, the height hint is set to the requested value.
     */
    public void setTableHeightHint(int heightHint) {
        GridData data = new GridData();
        if (heightHint > 0) {
            data.heightHint = heightHint;
        }
        data.grabExcessVerticalSpace = true;
        data.grabExcessHorizontalSpace = true;
        data.horizontalAlignment = GridData.FILL;
        data.verticalAlignment = GridData.FILL;
        mTable.setLayoutData(data);
    }

    @Override
    public void widgetDisposed(DisposeEvent e) {
        dispose();
    }

    @Override
    public void dispose() {
        mSwtUpdaterData.removeListener(this);
        mDeviceManager.unregisterListener(this);
        super.dispose();
    }

    @Override
    protected void checkSubclass() {
        // Disable the check that prevents subclassing of SWT components
    }

    // -- Start of internal part ----------
    // Hide everything down-below from SWT designer
    //$hide>>$

    /**
     * Called by the constructor right after {@link #createContents(Composite)}.
     */
    private void postCreate() {
        // nothing to be done for now.
    }


    // -------

    private static class CellInfo {
        final boolean mIsUser;
        final Device  mDevice;
        final TextLayout mWidget;
        Rectangle mBounds;

        CellInfo(boolean isUser, Device device, TextLayout widget) {
            mIsUser = isUser;
            mDevice = device;
            mWidget = widget;
        }
    }

    private void fillTable(final Table table) {

        table.removeAll();
        disposeTableResources(table.getData("disposeResources"));

        final List<Resource> disposables = new ArrayList<Resource>();

        Font boldFont = getBoldFont(table);
        if (boldFont != null) {
            disposables.add(boldFont);
        } else {
            boldFont = table.getFont();
        }

        try {
            mDisableRefresh = true;
            disposables.addAll(fillDevices(table, boldFont, true,
                    mDeviceManager.getDevices(DeviceFilter.USER)));
            disposables.addAll(fillDevices(table, boldFont, false,
                    mDeviceManager.getDevices(EnumSet.of(DeviceFilter.DEFAULT,
                                                         DeviceFilter.VENDOR,
                                                         DeviceFilter.SYSTEM_IMAGES))));
        } finally {
            mDisableRefresh = false;
        }

        table.setData("disposeResources", disposables);

        if (!Boolean.TRUE.equals(table.getData("createdTableListeners"))) {
            table.addListener(SWT.PaintItem, new Listener() {
                @Override
                public void handleEvent(Event event) {
                    if (event.item != null) {
                        Object info = event.item.getData();
                        if (info instanceof CellInfo) {
                            ((CellInfo) info).mWidget.draw(event.gc, event.x, event.y + 1);
                        }
                    }
                }
            });

            table.addListener(SWT.MeasureItem, new Listener() {
                @Override
                public void handleEvent(Event event) {
                    if (event.item != null) {
                        Object info = event.item.getData();
                        if (info instanceof CellInfo) {
                            CellInfo ci = (CellInfo) info;
                            Rectangle bounds = ci.mBounds;
                            if (bounds == null) {
                                // TextLayout.getBounds() seems expensive, so let's cache it.
                                ci.mBounds = bounds = ci.mWidget.getBounds();
                            }
                            event.width = bounds.width + 2;
                            event.height = bounds.height + 4;
                        }
                    }
                }
            });

            table.addDisposeListener(new DisposeListener() {
                @Override
                public void widgetDisposed(DisposeEvent event) {
                    disposeTableResources(table.getData("disposeResources"));
                }
            });

            table.addSelectionListener(new SelectionListener() {
                /** Handles single clicks on a row. */
                @Override
                public void widgetSelected(SelectionEvent event) {
                    updateButtonStates();
                }

                /** Handles double click on a row. */
                @Override
                public void widgetDefaultSelected(SelectionEvent event) {
                    // FIXME: should double-click be to edit a device or create a new AVD?
                    onEditDevice();
                }
            });
        }

        if (table.getItemCount() == 0) {
            table.setEnabled(true);
            TableItem item = new TableItem(table, SWT.NONE);
            item.setData(null);
            item.setText(0, "No devices available");
            return;
        }

        table.setData("createdTableListeners", Boolean.TRUE);
    }

    private void disposeTableResources(Object disposablesList) {
        if (disposablesList instanceof List<?>) {
            for (Object obj : (List<?>) disposablesList) {
                if (obj instanceof Resource) {
                    ((Resource) obj).dispose();
                }
            }
        }
    }

    private Font getBoldFont(Table table) {
        Display display = table.getDisplay();
        FontData[] fds = table.getFont().getFontData();
        if (fds != null && fds.length > 0) {
            fds[0].setStyle(SWT.BOLD);
            return new Font(display, fds[0]);
        }
        return null;
    }

    private List<Resource> fillDevices(
            Table table,
            Font boldFont,
            boolean isUser,
            Collection<Device> devicesCollection) {
        List<Resource> disposables = new ArrayList<Resource>();
        Display display = table.getDisplay();

        TextStyle boldStyle = new TextStyle();
        boldStyle.font = boldFont;

        // We need the list to be be modifiable so that we can sort it.
        ArrayList<Device> devices = new ArrayList<Device>(devicesCollection);
        Collections.sort(devices, Device.getDisplayComparator());

        // Generate a list of the AVD names using these devices
        Map<Device, List<String>> device2avdMap = new HashMap<Device, List<String>>();
        for (AvdInfo avd : mSwtUpdaterData.getAvdManager().getAllAvds()) {
            String n = avd.getDeviceName();
            String m = avd.getDeviceManufacturer();
            if (n == null || m == null || n.isEmpty() || m.isEmpty()) {
                continue;
            }
            for (Device device : devices) {
                if (m.equals(device.getManufacturer()) && n.equals(device.getName())) {
                    List<String> list = device2avdMap.get(device);
                    if (list == null) {
                        list = new LinkedList<String>();
                        device2avdMap.put(device, list);
                    }
                    list.add(avd.getName());
                }
            }
        }

        final String prefix = "\n    ";

        for (Device device : devices) {
            TableItem item = new TableItem(table, SWT.NONE);
            TextLayout widget = new TextLayout(display);
            CellInfo ci = new CellInfo(isUser, device, widget);
            item.setData(ci);

            widget.setIndent(mImageWidth * 2);
            widget.setFont(table.getFont());

            StringBuilder sb = new StringBuilder();
            String name = getPrettyName(device, false /*leadZeroes*/);
            sb.append(name);
            int pos1 = sb.length();

            String manufacturer = device.getManufacturer();
            if (!manufacturer.contains(NEXUS)) {
                sb.append("  by ").append(manufacturer);
            }

            Image img = getTagImage(device.getTagId(), isUser);
            item.setImage(img != null ? img : mDeviceImage);

            Hardware hw = device.getDefaultHardware();
            Screen screen = hw.getScreen();
            sb.append(prefix);
            sb.append(String.format(java.util.Locale.US,
                        "Screen:   %1$.1f\", %2$d \u00D7 %3$d, %4$s %5$s", // U+00D7: Unicode multiplication sign
                        screen.getDiagonalLength(),
                        screen.getXDimension(),
                        screen.getYDimension(),
                        screen.getSize().getShortDisplayValue(),
                        screen.getPixelDensity().getResourceValue()
                        ));

            Storage sto = hw.getRam();
            Unit unit = sto.getSizeAsUnit(Unit.GiB) > 1 ? Unit.GiB : Unit.MiB;
            sb.append(prefix);
            sb.append(String.format(java.util.Locale.US,
                    "RAM:       %1$d %2$s",
                    sto.getSizeAsUnit(unit),
                    unit));

            List<String> avdList = device2avdMap.get(device);
            if (avdList != null && !avdList.isEmpty()) {
                sb.append(prefix);
                sb.append("Used by: ");
                boolean first = true;
                for (String avd : avdList) {
                    if (!first) {
                        sb.append(", ");
                    }
                    sb.append(avd);
                    first = false;
                }
            }

            widget.setText(sb.toString());
            widget.setStyle(boldStyle, 0, pos1);
        }

        return disposables;
    }

    @Nullable
    private Image getTagImage(@NonNull String tagId, boolean isUser) {
        if (tagId == null) {
            tagId = SystemImage.DEFAULT_TAG.getId();
        }

        String fname = String.format("tag_%s_32.png", tagId);
        String kname = (isUser ? "user_" : "dev_") + fname;
        return mImageFactory.getImageByName(fname, kname, isUser ? mUserColorFilter : null);
    }

    // Constants extracted from DeviceMenuListerner -- TODO refactor somewhere else.
    private static final String NEXUS   = "Nexus";     //$NON-NLS-1$
    private static Pattern PATTERN = Pattern.compile(
            "(\\d+\\.?\\d*)(?:in|\") (.+?)( \\(.*Nexus.*\\))?"); //$NON-NLS-1$
    /**
     * Returns a pretty name for the device.
     *
     * Extracted from DeviceMenuListener.
     * Modified to remove the leading space insertion as it doesn't render
     * neatly in the avd manager. Instead added the option to add leading
     * zeroes to make the string names sort properly.
     *
     * Replace "'in'" with '"' (e.g. 2.7" QVGA instead of 2.7in QVGA)
     * Use the same precision for all devices (all but one specify decimals)
     * Add in screen resolution and density
     */
    private static String getPrettyName(Device d, boolean leadZeroes) {
        if (d == null) {
            return "";
        }
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
            if (leadZeroes && dot < 3) {
                // Pad to have at least 3 digits before the dot, for sorting purposes.
                // We can revisit this once we get devices that are more than 999 inches wide.
                size = "000".substring(dot) + size;
            }
            name = size + "\" " + n;
        }

        return name;
    }

    /**
     * Returns the currently selected cell info in the table or null
     */
    private CellInfo getTableSelection() {
        if (mTable.isDisposed()) {
            return null;
        }
        int selIndex = mTable.getSelectionIndex();
        if (selIndex >= 0) {
            return (CellInfo) mTable.getItem(selIndex).getData();
        }

        return null;
    }

    private void updateButtonStates() {
        CellInfo ci = getTableSelection();

        mNewButton.setEnabled(true);
        mEditButton.setEnabled(ci != null);
        mEditButton.setText((ci != null && !ci.mIsUser) ? "Clone..." : "Edit...");
        mDeleteButton.setEnabled(ci != null && ci.mIsUser);
        mNewAvdButton.setEnabled(ci != null);
        mRefreshButton.setEnabled(true);
    }

    private void onNewDevice() {
        DeviceCreationDialog dlg = new DeviceCreationDialog(
                getShell(),
                mDeviceManager,
                mSwtUpdaterData.getImageFactory(),
                null /*device*/);
        if (dlg.open() == Window.OK) {
            onRefresh();

            // Select the new device, if any.
            selectCellByDevice(dlg.getCreatedDevice());
            updateButtonStates();
        }
    }

    private void onEditDevice() {
        CellInfo ci = getTableSelection();
        if (ci == null || ci.mDevice == null) {
            return;
        }

        DeviceCreationDialog dlg = new DeviceCreationDialog(
                getShell(),
                mDeviceManager,
                mSwtUpdaterData.getImageFactory(),
                ci.mDevice);
        if (dlg.open() == Window.OK) {
            onRefresh();

            // Select the new device, if any.
            selectCellByDevice(dlg.getCreatedDevice());
            updateButtonStates();
        }
    }

    private void onDeleteDevice() {
        CellInfo ci = getTableSelection();
        if (ci == null || ci.mDevice == null || !ci.mIsUser) {
            return;
        }

        final String name = getPrettyName(ci.mDevice, false /*leadZeroes*/);
        final AtomicBoolean result = new AtomicBoolean(false);
        getDisplay().syncExec(new Runnable() {
            @Override
            public void run() {
                Shell shell = getDisplay().getActiveShell();
                boolean ok = MessageDialog.openQuestion(shell,
                        "Delete Device Definition",
                        String.format(
                                "Please confirm that you want to delete the device definition named '%s'. This operation cannot be reverted.",
                                name));
                result.set(ok);
            }
        });

        if (result.get()) {
            mDeviceManager.removeUserDevice(ci.mDevice);
            mDeviceManager.saveUserDevices();
            onRefresh();
        }
    }

    private void onCreateAvd() {
        CellInfo ci = getTableSelection();
        if (ci == null || ci.mDevice == null) {
            return;
        }

        final AvdCreationDialog dlg = new AvdCreationDialog(mTable.getShell(),
                mSwtUpdaterData.getAvdManager(),
                mImageFactory,
                mSwtUpdaterData.getSdkLog(),
                null);
        dlg.selectInitialDevice(ci.mDevice);

        if (dlg.open() == Window.OK) {
            onRefresh();

            if (mAvdCreatedListener != null) {
                mAvdCreatedListener.onAvdCreated(dlg.getCreatedAvd());
            }
        }
    }

    private void onRefresh() {
        if (mDisableRefresh || mTable.isDisposed()) {
            return;
        }
        int selIndex = mTable.getSelectionIndex();
        CellInfo selected = getTableSelection();

        fillTable(mTable);

        if (selected != null) {
            if (selectCellByName(selected)) {
                updateButtonStates();
                return;
            }
        }
        // If not found by name, use the position if available.
        if (selIndex >= 0 && selIndex < mTable.getItemCount()) {
            mTable.select(selIndex);
        }
    }

    private boolean selectCellByName(CellInfo selected) {
        if (mTable.isDisposed() || selected == null || selected.mDevice == null) {
            return false;
        }
        String name = selected.mDevice.getName();
        for (int n = mTable.getItemCount() - 1; n >= 0; n--) {
            TableItem item = mTable.getItem(n);
            Object data = item.getData();
            if (data instanceof CellInfo) {
                CellInfo ci = (CellInfo) data;
                if (ci != null && ci.mDevice != null && name.equals(ci.mDevice.getName())) {
                    // Same cell object. Select it.
                    mTable.select(n);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean selectCellByDevice(Device selected) {
        if (mTable.isDisposed() || selected == null) {
            return false;
        }
        for (int n = mTable.getItemCount() - 1; n >= 0; n--) {
            TableItem item = mTable.getItem(n);
            Object data = item.getData();
            if (data instanceof CellInfo) {
                CellInfo ci = (CellInfo) data;
                if (ci != null && ci.mDevice == selected) {
                    // Same device object. Select it.
                    mTable.select(n);
                    return true;
                }
            }
        }
        return false;
    }

    // -------


    // --- Implementation of ISdkChangeListener ---

    @Override
    public void onSdkLoaded() {
        onSdkReload();
    }

    @Override
    public void onSdkReload() {
        onRefresh();
    }

    @Override
    public void preInstallHook() {
        // nothing to be done for now.
    }

    @Override
    public void postInstallHook() {
        // nothing to be done for now.
    }

    // --- Implementation of DevicesChangeListener

    @Override
    public void onDevicesChanged() {
        onRefresh();
    }


    // End of hiding from SWT Designer
    //$hide<<$
}

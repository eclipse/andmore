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

import static com.android.ide.common.rendering.HardwareConfigHelper.MANUFACTURER_GENERIC;
import static com.android.ide.common.rendering.HardwareConfigHelper.getGenericLabel;
import static com.android.ide.common.rendering.HardwareConfigHelper.getNexusLabel;
import static com.android.ide.common.rendering.HardwareConfigHelper.isGeneric;
import static com.android.ide.common.rendering.HardwareConfigHelper.isNexus;
import static com.android.ide.common.rendering.HardwareConfigHelper.sortNexusList;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.sdklib.devices.Device;
import com.android.sdklib.internal.avd.AvdInfo;
import com.android.sdklib.internal.avd.AvdManager;

import org.eclipse.andmore.internal.editors.layout.gle2.RenderPreviewMode;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolItem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The {@linkplain DeviceMenuListener} class is responsible for generating the device
 * menu in the {@link ConfigurationChooser}.
 */
class DeviceMenuListener extends SelectionAdapter {
    private final ConfigurationChooser mConfigChooser;
    private final Device mDevice;

    DeviceMenuListener(
            @NonNull ConfigurationChooser configChooser,
            @Nullable Device device) {
        mConfigChooser = configChooser;
        mDevice = device;
    }

    @Override
    public void widgetSelected(SelectionEvent e) {
        mConfigChooser.selectDevice(mDevice);
        mConfigChooser.onDeviceChange();
    }

    static void show(final ConfigurationChooser chooser, ToolItem combo) {
        Configuration configuration = chooser.getConfiguration();
        Device current = configuration.getDevice();
        Menu menu = new Menu(chooser.getShell(), SWT.POP_UP);

        Collection<Device> deviceCollection = chooser.getDevices();
        Sdk sdk = Sdk.getCurrent();
        if (sdk != null) {
            AvdManager avdManager = sdk.getAvdManager();
            if (avdManager != null) {
                boolean separatorNeeded = false;
                AvdInfo[] avds = avdManager.getValidAvds();
                for (AvdInfo avd : avds) {
                    for (Device device : deviceCollection) {
                        if (device.getManufacturer().equals(avd.getDeviceManufacturer())
                                && device.getName().equals(avd.getDeviceName())) {
                            separatorNeeded = true;
                            MenuItem item = new MenuItem(menu, SWT.CHECK);
                            item.setText(avd.getName());
                            item.setSelection(current == device);

                            item.addSelectionListener(new DeviceMenuListener(chooser, device));
                        }
                    }
                }

                if (separatorNeeded) {
                    @SuppressWarnings("unused")
                    MenuItem separator = new MenuItem(menu, SWT.SEPARATOR);
                }
            }
        }

        // Group the devices by manufacturer, then put them in the menu.
        // If we don't have anything but Nexus devices, group them together rather than
        // make many manufacturer submenus.
        boolean haveNexus = false;
        boolean haveNonNexus = false;
        if (!deviceCollection.isEmpty()) {
            Map<String, List<Device>> manufacturers = new TreeMap<String, List<Device>>();
            for (Device device : deviceCollection) {
                List<Device> devices;
                if (isNexus(device)) {
                    haveNexus = true;
                } else if (!isGeneric(device)) {
                    haveNonNexus = true;
                }
                if (manufacturers.containsKey(device.getManufacturer())) {
                    devices = manufacturers.get(device.getManufacturer());
                } else {
                    devices = new ArrayList<Device>();
                    manufacturers.put(device.getManufacturer(), devices);
                }
                devices.add(device);
            }
            if (haveNonNexus) {
                for (List<Device> devices : manufacturers.values()) {
                    Menu manufacturerMenu = menu;
                    if (manufacturers.size() > 1) {
                        MenuItem item = new MenuItem(menu, SWT.CASCADE);
                        item.setText(devices.get(0).getManufacturer());
                        manufacturerMenu = new Menu(menu);
                        item.setMenu(manufacturerMenu);
                    }
                    for (final Device device : devices) {
                        MenuItem deviceItem = new MenuItem(manufacturerMenu, SWT.CHECK);
                        deviceItem.setText(getGenericLabel(device));
                        deviceItem.setSelection(current == device);
                        deviceItem.addSelectionListener(new DeviceMenuListener(chooser, device));
                    }
                }
            } else {
                List<Device> nexus = new ArrayList<Device>();
                List<Device> generic = new ArrayList<Device>();
                if (haveNexus) {
                    // Nexus
                    for (List<Device> devices : manufacturers.values()) {
                        for (Device device : devices) {
                            if (isNexus(device)) {
                                if (device.getManufacturer().equals(MANUFACTURER_GENERIC)) {
                                    generic.add(device);
                                } else {
                                    nexus.add(device);
                                }
                            } else {
                                generic.add(device);
                            }
                        }
                    }
                }

                if (!nexus.isEmpty()) {
                    sortNexusList(nexus);
                    for (final Device device : nexus) {
                        MenuItem item = new MenuItem(menu, SWT.CHECK);
                        item.setText(getNexusLabel(device));
                        item.setSelection(current == device);
                        item.addSelectionListener(new DeviceMenuListener(chooser, device));
                    }

                    @SuppressWarnings("unused")
                    MenuItem separator = new MenuItem(menu, SWT.SEPARATOR);
                }

                // Generate the generic menu.
                Collections.reverse(generic);
                for (final Device device : generic) {
                    MenuItem item = new MenuItem(menu, SWT.CHECK);
                    item.setText(getGenericLabel(device));
                    item.setSelection(current == device);
                    item.addSelectionListener(new DeviceMenuListener(chooser, device));
                }
            }
        }

        @SuppressWarnings("unused")
        MenuItem separator = new MenuItem(menu, SWT.SEPARATOR);

        ConfigurationMenuListener.addTogglePreviewModeAction(menu,
                "Preview All Screens", chooser, RenderPreviewMode.SCREENS);


        Rectangle bounds = combo.getBounds();
        Point location = new Point(bounds.x, bounds.y + bounds.height);
        location = combo.getParent().toDisplay(location);
        menu.setLocation(location.x, location.y);
        menu.setVisible(true);
    }
}

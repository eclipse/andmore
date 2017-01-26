/*
 * Copyright (C) 2011 The Android Open Source Project
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
package com.android.ddmuilib.logcat;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.AndroidDebugBridge.IClientChangeListener;
import com.android.ddmlib.AndroidDebugBridge.IDeviceChangeListener;
import com.android.ddmlib.Client;
import com.android.ddmlib.ClientData;
import com.android.ddmlib.IDevice;

import java.util.HashMap;
import java.util.Map;

/**
 * This class maintains a mapping between the PID and the application name for all
 * running apps on a device. It does this by implementing callbacks to two events:
 * {@link AndroidDebugBridge.IDeviceChangeListener} and
 * {@link AndroidDebugBridge.IClientChangeListener}.
 */
public class LogCatPidToNameMapper {
    /** Default name used when the actual name cannot be determined. */
    public static final String UNKNOWN_APP = "";

    private IClientChangeListener mClientChangeListener;
    private IDeviceChangeListener mDeviceChangeListener;
    private IDevice mDevice;
    private Map<String, String> mPidToName;

    public LogCatPidToNameMapper(IDevice device) {
        mDevice = device;
        mClientChangeListener = constructClientChangeListener();
        AndroidDebugBridge.addClientChangeListener(mClientChangeListener);

        mDeviceChangeListener = constructDeviceChangeListener();
        AndroidDebugBridge.addDeviceChangeListener(mDeviceChangeListener);

        mPidToName = new HashMap<String, String>();

        updateClientList(device);
    }

    private IClientChangeListener constructClientChangeListener() {
        return new IClientChangeListener() {
            @Override
            public void clientChanged(Client client, int changeMask) {
                if ((changeMask & Client.CHANGE_NAME) == Client.CHANGE_NAME) {
                    ClientData cd = client.getClientData();
                    updateClientName(cd);
                }
            }
        };
    }

    private void updateClientName(ClientData cd) {
        String name = cd.getClientDescription();
        if (name != null) {
            int pid = cd.getPid();
            if (mPidToName != null) {
                mPidToName.put(Integer.toString(pid), name);
            }
        }
    }

    private IDeviceChangeListener constructDeviceChangeListener() {
        return new IDeviceChangeListener() {
            @Override
            public void deviceDisconnected(IDevice device) {
            }

            @Override
            public void deviceConnected(IDevice device) {
            }

            @Override
            public void deviceChanged(IDevice device, int changeMask) {
                if (changeMask == IDevice.CHANGE_CLIENT_LIST) {
                    updateClientList(device);
                }
            }
        };
    }

    private void updateClientList(IDevice device) {
        if (mDevice == null) {
            return;
        }

        if (!mDevice.equals(device)) {
            return;
        }

        mPidToName = new HashMap<String, String>();
        for (Client c : device.getClients()) {
            ClientData cd = c.getClientData();
            String name = cd.getClientDescription();
            int pid = cd.getPid();

            /* The name will be null for apps that have just been created.
             * In such a case, we fill in the default name, and wait for the
             * clientChangeListener to do the update with the correct name.
             */
            if (name == null) {
                name = UNKNOWN_APP;
            }

            mPidToName.put(Integer.toString(pid), name);
        }
    }

    /**
     * Get the application name corresponding to given pid.
     * @param pid application's pid
     * @return application name if available, else {@link LogCatPidToNameMapper#UNKNOWN_APP}.
     */
    public String getName(String pid) {
        String name = mPidToName.get(pid);
        return name != null ? name : UNKNOWN_APP;
    }
}

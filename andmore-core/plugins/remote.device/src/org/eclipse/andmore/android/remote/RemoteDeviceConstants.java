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
package org.eclipse.andmore.android.remote;

/**
 * Constants used by Android Remote Device Plug-in.
 */
public class RemoteDeviceConstants {

	/**
	 * The ID of the device declared by this plug-in
	 */
	public static final String DEVICE_ID = RemoteDevicePlugin.PLUGIN_ID + ".androidRemoteDevice";

	public static final String SERVICE_INIT_ID = RemoteDevicePlugin.PLUGIN_ID + ".initRemoteService";

	public static final String SERVICE_CONNECT_ID = RemoteDevicePlugin.PLUGIN_ID + ".connectRemoteService";

	public static final String SERVICE_DISCONNECT_ID = RemoteDevicePlugin.PLUGIN_ID + ".disconnectRemoteService";

	public static final String HELP_ID = RemoteDevicePlugin.PLUGIN_ID + ".remoteDeviceProperties";

	public static final String WIRELESS_HELP_ID = RemoteDevicePlugin.PLUGIN_ID + ".wirelessRemoteDeviceProperties";

	public static final String DUMMY_TRANSITION = "dummy";

	public static final int DEFAULT_TIMEOUT = 30;

	public static final int DEFAULT_PORT = 10000;

	public static final String DEFAULT_WIRELESS_SUFIX = "_wireless";

	/**
	 * Preference key of the Question Dialog about disconnection all remote
	 * devices in shutdown
	 */
	public static final String DISCONNECT_ALL_REMOTE_DEVICES_IN_SHUTDOWN_KEY_PREFERENCE = "disconnect.all.remote.devices.in.shutdown";

	public static final String CONNECT_PARAMETER = "connect";

	/**
	 * The minimum port number allowed to create a remote device.
	 */
	public static final int MINIMUM_PORT_NUMBER = 1024;

	/**
	 * The maximum port number allowed for TCP/IP addresses.
	 */
	public static final int MAXIMUM_PORT_NUMBER = 65535;
}

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
package org.eclipse.andmore.android.emulator.device.instance.options;

import java.util.HashMap;
import java.util.Map;

/**
 * This interface contains constants used for the Startup Options Management
 * 
 */
@SuppressWarnings("serial")
public interface IStartupOptionsConstants {

	/*
	 * XML Path
	 */
	public final String STARTUP_OPTIONS_XML_PATH = "resource/startup_options.xml";

	/*
	 * XML tags
	 */
	public final String ROOT_TAG = "startupOptions";

	public final String GROUP_TAG = "group";

	public final String GROUP_TAG_ID = "id";

	public final String STARTUP_OPT_TAG = "startupOption";

	public final String STARTUP_OPT_TAG_NAME = "name";

	public final String STARTUP_OPT_TAG_FRIENDLY_NAME = "fName";

	public final String STARTUP_OPT_TAG_TYPE = "type";

	public final String STARTUP_OPT_TAG_TYPE_DETAILS = "typeDetails";

	public final String STARTUP_OPT_TAG_DESCRIPTION = "description";

	public final String PREDEFINED_VALUES_TAG = "values";

	public final String PREDEFINED_VALUE_TAG = "value";

	/*
	 * Startup option value type
	 */
	public final int TYPE_NONE = 0;

	public final int TYPE_TEXT = 1;

	public final int TYPE_PATH = 2;

	public final int TYPE_NUMBER = 3;

	public final String TYPE_PATH_DIR = "dir";

	public final Map<String, Integer> TYPE_MAP = new HashMap<String, Integer>() {
		{
			put("none", TYPE_NONE);
			put("text", TYPE_TEXT);
			put("path", TYPE_PATH);
			put("int", TYPE_NUMBER);
		}

	};

	/*
	 * Disk images options
	 */
	public final String DISKIMAGES_GROUP = "Disk Images";

	public final String DISKIMAGES_CACHE = "-cache";

	public final String DISKIMAGES_DATA = "-data";

	public final String DISKIMAGES_IMAGE = "-image";

	public final String DISKIMAGES_INITDATA = "-initdata";

	public final String DISKIMAGES_KERNEL = "-kernel";

	public final String DISKIMAGES_NOCACHE = "-nocache";

	public final String DISKIMAGES_RAMDISK = "-ramdisk";

	public final String DISKIMAGES_SDCARD = "-sdcard";

	public final String DISKIMAGES_SYSTEM = "-system";

	public final String DISKIMAGES_WIPEDATA = "-wipe-data";

	/*
	 * Network options
	 */
	public final String NETWORK_GROUP = "Network";

	public final String NETWORK_DNS_SERVER = "-dns-server";

	public final String NETWORK_HTTP_PROXY = "-http-proxy";

	public final String NETWORK_NETDELAY = "-netdelay";

	public final String NETWORK_NETFAST = "-netfast";

	public final String NETWORK_NETSPEED = "-netspeed";

	public final String NETWORK_PORT = "-port";

	/*
	 * System options
	 */
	public final String SYSTEM_GROUP = "System";

	public final String SYSTEM_CPU_DELAY = "-cpu-delay";

	public final String SYSTEM_GPS = "-gps";

	public final String SYSTEM_NO_JNI = "-nojni";

	/*
	 * UI options
	 */
	public final String UI_GROUP = "UI";

	public final String UI_DPI_DEVICE = "-dpi-device";

	public final String SCALE = "-scale";

	public final String NO_BOOT_AIM = "-no-boot-anim";

	public final String NO_SKIN = "-no-skin";

	/*
	 * Other options
	 */
	public final String OTHERS_GROUP = "Others";

	public final String OTHERS_OTHER = "other";
}

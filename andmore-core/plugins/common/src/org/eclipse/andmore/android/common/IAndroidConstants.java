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
package org.eclipse.andmore.android.common;

import org.eclipse.core.runtime.IPath;

/**
 * Interface that holds common constants to be used in the Android Plug-in
 */
public interface IAndroidConstants {
	String RES_DIR = "res" + IPath.SEPARATOR;

	String VALUES_DIR = "values" + IPath.SEPARATOR;

	String STRINGS_FILE = "strings.xml"; //$NON-NLS-1$    

	String MENU_FILE_EXTENSION = "xml";

	String FN_ANDROID_MANIFEST = "AndroidManifest.xml";

	String GEN_SRC_FOLDER = "gen";

	String OPHONE_JAR = "oms.jar";

	String JIL_JAR = "internal.jar";

	String WS_ROOT = "/";

	String FD_SOURCES = "src";

	String RE_DOT = "\\.";

	String DOT_JAVA = ".java";

	String ANDROID_NATURE = "org.eclipe.andmore.AndroidNature";

	String CLASS_ACTIVITY = "android.app.Activity";

	String CLASS_CONTENTPROVIDER = "android.content.ContentProvider";

	String CLASS_SERVICE = "android.app.Service";

	String CLASS_BROADCASTRECEIVER = "android.content.BroadcastReceiver";

	String FD_RES = "res";

	String FD_XML = "xml";

	String FD_TOOLS = "tools";

	String FD_PLATFORM_TOOLS = "platform-tools";

	String FD_VALUES = "values";

	String FD_ANIM = "anim";

	String FD_DRAWABLE = "drawable";

	String FD_LAYOUT = "layout";

	String FD_MENU = "menu";

	String FD_GEN_SOURCES = "gen";

	String FD_ASSETS = "assets";

	String FD_RESOURCES = "res";

	String FD_OUTPUT = "bin";

	static final int API_LEVEL_FOR_PLATFORM_VERSION_3_0_0 = 11;

	String ACTION_MAIN = "android.intent.action.MAIN";

	String CATEGORY_LAUNCHER = "android.intent.category.LAUNCHER";
}

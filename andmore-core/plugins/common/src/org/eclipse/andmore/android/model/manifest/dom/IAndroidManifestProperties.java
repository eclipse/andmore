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
package org.eclipse.andmore.android.model.manifest.dom;

/**
 * Interface that contains the node properties from AndroidManifest.xml file
 */
public interface IAndroidManifestProperties {
	String ANDROID_QUALIFIER = "android:";

	String PROP_ALLOWCLEARUSERDATA = ANDROID_QUALIFIER + "allowClearUserData";

	String PROP_ALLOWTASKREPARENTING = ANDROID_QUALIFIER + "allowTaskReparenting";

	String PROP_ALWAYSRETAINTASKSTATE = ANDROID_QUALIFIER + "alwaysRetainTaskState";

	String PROP_AUTHORITIES = ANDROID_QUALIFIER + "authorities";

	String PROP_CLEARTASKONLAUNCH = ANDROID_QUALIFIER + "clearTaskOnLaunch";

	String PROP_CONFIGCHANGES = ANDROID_QUALIFIER + "configChanges";

	String PROP_DEBUGGABLE = ANDROID_QUALIFIER + "debuggable";

	String PROP_DESCRIPTION = ANDROID_QUALIFIER + "description";

	String PROP_ENABLED = ANDROID_QUALIFIER + "enabled";

	String PROP_EXCLUDEFROMRECENTS = ANDROID_QUALIFIER + "excludeFromRecents";

	String PROP_EXPORTED = ANDROID_QUALIFIER + "exported";

	String PROP_FINISHONTASKLAUNCH = ANDROID_QUALIFIER + "finishOnTaskLaunch";

	String PROP_FUNCTIONALTEST = ANDROID_QUALIFIER + "functionalTest";

	String PROP_GRANTURIPERMISSIONS = ANDROID_QUALIFIER + "grantUriPermissions";

	String PROP_HANDLEPROFILING = ANDROID_QUALIFIER + "handleProfiling";

	String PROP_HASCODE = ANDROID_QUALIFIER + "hasCode";

	String PROP_HOST = ANDROID_QUALIFIER + "host";

	String PROP_ICON = ANDROID_QUALIFIER + "icon";

	String PROP_INITORDER = ANDROID_QUALIFIER + "initOrder";

	String PROP_LABEL = ANDROID_QUALIFIER + "label";

	String PROP_LAUNCHMODE = ANDROID_QUALIFIER + "launchMode";

	String PROP_MANAGESPACEACTIVITY = ANDROID_QUALIFIER + "manageSpaceActivity";

	String PROP_MIMETYPE = ANDROID_QUALIFIER + "mimeType";

	String PROP_MINSDKVERSION = ANDROID_QUALIFIER + "minSdkVersion";

	String PROP_MAXSDKVERSION = ANDROID_QUALIFIER + "maxSdkVersion";

	String PROP_MULTIPROCESS = ANDROID_QUALIFIER + "multiprocess";

	String PROP_NAME = ANDROID_QUALIFIER + "name";

	// Without the android qualifier
	String PROP_PACKAGE = "package";

	String PROP_PATH = ANDROID_QUALIFIER + "path";

	String PROP_PATHPATTERN = ANDROID_QUALIFIER + "pathPattern";

	String PROP_PATHPREFIX = ANDROID_QUALIFIER + "pathPrefix";

	String PROP_PERMISSION = ANDROID_QUALIFIER + "permission";

	String PROP_PERMISSIONGROUP = ANDROID_QUALIFIER + "permissionGroup";

	String PROP_PERSISTENT = ANDROID_QUALIFIER + "persistent";

	String PROP_PORT = ANDROID_QUALIFIER + "port";

	String PROP_PRIORITY = ANDROID_QUALIFIER + "priority";

	String PROP_PROCESS = ANDROID_QUALIFIER + "process";

	String PROP_PROTECTIONLEVEL = ANDROID_QUALIFIER + "protectionLevel";

	String PROP_READPERMISSION = ANDROID_QUALIFIER + "readPermission";

	String PROP_REQUIRED = ANDROID_QUALIFIER + "required";

	String PROP_RESOURCE = ANDROID_QUALIFIER + "resource";

	String PROP_SCHEME = ANDROID_QUALIFIER + "scheme";

	String PROP_SCREENORIENTATION = ANDROID_QUALIFIER + "screenOrientation";

	String PROP_SHAREDUSERID = ANDROID_QUALIFIER + "sharedUserId";

	String PROP_STATENOTNEEDED = ANDROID_QUALIFIER + "stateNotNeeded";

	String PROP_SYNCABLE = ANDROID_QUALIFIER + "syncable";

	String PROP_TARGETACTIVITY = ANDROID_QUALIFIER + "targetActivity";

	String PROP_TARGETPACKAGE = ANDROID_QUALIFIER + "targetPackage";

	String PROP_TARGETSDKVERSION = ANDROID_QUALIFIER + "targetSdkVersion";

	String PROP_TASKAFFINITY = ANDROID_QUALIFIER + "taskAffinity";

	String PROP_THEME = ANDROID_QUALIFIER + "theme";

	String PROP_VALUE = ANDROID_QUALIFIER + "value";

	String PROP_VERSIONCODE = ANDROID_QUALIFIER + "versionCode";

	String PROP_VERSIONNAME = ANDROID_QUALIFIER + "versionName";

	String PROP_XMLNS = "xmlns:android";

	String PROP_WRITEPERMISSION = ANDROID_QUALIFIER + "writePermission";

}

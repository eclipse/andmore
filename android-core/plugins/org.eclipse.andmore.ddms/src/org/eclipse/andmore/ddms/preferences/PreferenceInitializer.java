/*
 * Copyright (C) 2007 The Android Open Source Project
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

package org.eclipse.andmore.ddms.preferences;

import com.android.ddmlib.DdmPreferences;
import com.android.ddmlib.Log.LogLevel;
import com.android.ddmuilib.DdmUiPreferences;

import org.eclipse.andmore.ddms.DdmsPlugin;
import org.eclipse.andmore.ddms.LogCatMonitor;
import org.eclipse.andmore.ddms.views.LogCatView;
import org.eclipse.andmore.ddms.views.DeviceView.HProfHandler;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontData;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

	public final static String ATTR_LOG_LEVEL = DdmsPlugin.PLUGIN_ID + ".logLevel"; //$NON-NLS-1$

	public final static String ATTR_DEBUG_PORT_BASE = DdmsPlugin.PLUGIN_ID + ".adbDebugBasePort"; //$NON-NLS-1$

	public final static String ATTR_SELECTED_DEBUG_PORT = DdmsPlugin.PLUGIN_ID + ".debugSelectedPort"; //$NON-NLS-1$

	public final static String ATTR_DEFAULT_THREAD_UPDATE = DdmsPlugin.PLUGIN_ID + ".defaultThreadUpdateEnabled"; //$NON-NLS-1$

	public final static String ATTR_DEFAULT_HEAP_UPDATE = DdmsPlugin.PLUGIN_ID + ".defaultHeapUpdateEnabled"; //$NON-NLS-1$

	public final static String ATTR_THREAD_INTERVAL = DdmsPlugin.PLUGIN_ID + ".threadStatusInterval"; //$NON-NLS-1$

	public final static String ATTR_IMAGE_SAVE_DIR = DdmsPlugin.PLUGIN_ID + ".imageSaveDir"; //$NON-NLS-1$

	public final static String ATTR_LAST_IMAGE_SAVE_DIR = DdmsPlugin.PLUGIN_ID + ".lastImageSaveDir"; //$NON-NLS-1$

	public final static String ATTR_LOGCAT_FONT = DdmsPlugin.PLUGIN_ID + ".logcatFont"; //$NON-NLS-1$

	public final static String ATTR_HPROF_ACTION = DdmsPlugin.PLUGIN_ID + ".hprofAction"; //$NON-NLS-1$

	public final static String ATTR_TIME_OUT = DdmsPlugin.PLUGIN_ID + ".timeOut"; //$NON-NLS-1$

	public final static String ATTR_USE_ADBHOST = DdmsPlugin.PLUGIN_ID + ".useAdbHost"; //$NON-NLS-1$

	public final static String ATTR_ADBHOST_VALUE = DdmsPlugin.PLUGIN_ID + ".adbHostValue"; //$NON-NLS-1$

	public final static String ATTR_SWITCH_PERSPECTIVE = DdmsPlugin.PLUGIN_ID + ".switchPerspective"; //$NON-NLS-1$

	public final static String ATTR_PERSPECTIVE_ID = DdmsPlugin.PLUGIN_ID + ".perspectiveId"; //$NON-NLS-1$

	public static final String ATTR_PROFILER_BUFSIZE_MB = DdmsPlugin.PLUGIN_ID + ".profilerBufferSizeMb"; //$NON-NLS-1$

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer
	 * #initializeDefaultPreferences()
	 */
	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = DdmsPlugin.getDefault().getPreferenceStore();

		store.setDefault(ATTR_DEBUG_PORT_BASE, DdmPreferences.DEFAULT_DEBUG_PORT_BASE);

		store.setDefault(ATTR_SELECTED_DEBUG_PORT, DdmPreferences.DEFAULT_SELECTED_DEBUG_PORT);

		store.setDefault(ATTR_DEFAULT_THREAD_UPDATE, DdmPreferences.DEFAULT_INITIAL_THREAD_UPDATE);
		store.setDefault(ATTR_DEFAULT_HEAP_UPDATE, DdmPreferences.DEFAULT_INITIAL_HEAP_UPDATE);

		store.setDefault(ATTR_PROFILER_BUFSIZE_MB, DdmPreferences.DEFAULT_PROFILER_BUFFER_SIZE_MB);

		store.setDefault(ATTR_THREAD_INTERVAL, DdmUiPreferences.DEFAULT_THREAD_REFRESH_INTERVAL);

		String homeDir = System.getProperty("user.home"); //$NON-NLS-1$
		store.setDefault(ATTR_IMAGE_SAVE_DIR, homeDir);

		store.setDefault(ATTR_LOG_LEVEL, DdmPreferences.DEFAULT_LOG_LEVEL.getStringValue());

		store.setDefault(ATTR_LOGCAT_FONT, new FontData("Courier", 10, SWT.NORMAL).toString()); //$NON-NLS-1$

		// When obtaining hprof files from the device, default to opening the
		// file
		// only if there is a registered content type for the hprof extension.
		store.setDefault(ATTR_HPROF_ACTION, HProfHandler.ACTION_SAVE);
		for (IContentType contentType : Platform.getContentTypeManager().getAllContentTypes()) {
			if (contentType.isAssociatedWith(HProfHandler.DOT_HPROF)) {
				store.setDefault(ATTR_HPROF_ACTION, HProfHandler.ACTION_OPEN);
				break;
			}
		}

		store.setDefault(ATTR_TIME_OUT, DdmPreferences.DEFAULT_TIMEOUT);

		store.setDefault(ATTR_USE_ADBHOST, DdmPreferences.DEFAULT_USE_ADBHOST);
		store.setDefault(ATTR_ADBHOST_VALUE, DdmPreferences.DEFAULT_ADBHOST_VALUE);
		store.setDefault(ATTR_SWITCH_PERSPECTIVE, LogCatView.DEFAULT_SWITCH_PERSPECTIVE);
		store.setDefault(ATTR_PERSPECTIVE_ID, LogCatView.DEFAULT_PERSPECTIVE_ID);

		store.setDefault(LogCatMonitor.AUTO_MONITOR_PREFKEY, true);
		store.setDefault(LogCatMonitor.AUTO_MONITOR_LOGLEVEL, LogLevel.VERBOSE.getStringValue());
	}

	/**
	 * Initializes the preferences of ddmlib and ddmuilib with values from the
	 * eclipse store.
	 */
	public synchronized static void setupPreferences() {
		IPreferenceStore store = DdmsPlugin.getDefault().getPreferenceStore();

		DdmPreferences.setDebugPortBase(store.getInt(ATTR_DEBUG_PORT_BASE));
		DdmPreferences.setSelectedDebugPort(store.getInt(ATTR_SELECTED_DEBUG_PORT));
		DdmPreferences.setLogLevel(store.getString(ATTR_LOG_LEVEL));
		DdmPreferences.setInitialThreadUpdate(store.getBoolean(ATTR_DEFAULT_THREAD_UPDATE));
		DdmPreferences.setInitialHeapUpdate(store.getBoolean(ATTR_DEFAULT_HEAP_UPDATE));
		DdmPreferences.setProfilerBufferSizeMb(store.getInt(ATTR_PROFILER_BUFSIZE_MB));
		DdmUiPreferences.setThreadRefreshInterval(store.getInt(ATTR_THREAD_INTERVAL));
		DdmPreferences.setTimeOut(store.getInt(ATTR_TIME_OUT));
		DdmPreferences.setUseAdbHost(store.getBoolean(ATTR_USE_ADBHOST));
		DdmPreferences.setAdbHostValue(store.getString(ATTR_ADBHOST_VALUE));
	}
}

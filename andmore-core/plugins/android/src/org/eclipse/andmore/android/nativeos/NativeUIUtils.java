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
package org.eclipse.andmore.android.nativeos;

import org.eclipse.andmore.android.SdkUtils;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;

public class NativeUIUtils {
	private static INativeUI instance;

	private NativeUIUtils() {

	}

	public static INativeUI getInstance() {
		if (instance == null) {
			instance = (INativeUI) getClass("org.eclipse.andmore.android.nativeos.NativeUI");
		}
		return instance;
	}

	public static Object getClass(String nameClass) {
		try {
			return (Class.forName(nameClass)).newInstance();
		} catch (Exception ex) {
			StudioLogger
					.error(NativeUIUtils.class, "Error resolving OS dependent class for native windows feature", ex);
			return null;
		}
	}

	public static String getDefaultCommandLine() {
		return getInstance().getDefaultCommandLine();
	}

	public static String getDefaultUseVnc() {
		return getInstance().getDefaultUseVnc();
	}

	public static long getWindowHandle(String avdName, int instancePort) {
		String windowName = SdkUtils.getEmulatorWindowName(avdName, instancePort);
		return getInstance().getWindowHandle(windowName);
	}

	public static long getWindowProperties(long windowHandle) {
		return getInstance().getWindowProperties(windowHandle);
	}

	public static Point getWindowSize(long originalParentHandle, long windowHandle) {
		return getInstance().getWindowSize(originalParentHandle, windowHandle);
	}

	public static void hideWindow(long windowHandle) {
		getInstance().hideWindow(windowHandle);
	}

	public static boolean isWindowEnabled(long windowHandle) {
		return getInstance().isWindowEnabled(windowHandle);
	}

	public static void restoreWindow(long windowHandle) {
		getInstance().restoreWindow(windowHandle);
	}

	public static void sendNextLayoutCommand(long originalParent, long windowHandle) {
		getInstance().sendNextLayoutCommand(originalParent, windowHandle);
	}

	public static void setWindowFocus(long windowHandle) {
		getInstance().setWindowFocus(windowHandle);
	}

	public static long embedWindow(long windowHandle, Composite composite) {
		return getInstance().embedWindow(windowHandle, composite);
	}

	public static void unembedWindow(long windowHandle, long originalParent) {
		getInstance().unembedWindow(windowHandle, originalParent);
	}

	public static void setWindowProperties(long windowHandle, long originalProperties) {
		getInstance().setWindowProperties(windowHandle, originalProperties);
	}

	public static void setWindowStyle(long windowHandle) {
		getInstance().setWindowStyle(windowHandle);
	}

	public static void showWindow(long windowHandle) {
		getInstance().showWindow(windowHandle);
	}

}

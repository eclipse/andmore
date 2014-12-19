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

import org.eclipse.andmore.android.nativeos.INativeUI;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;

/***
 * This class is responsible for provide MAC OS specific constants values since
 * the option of use native emulator window to show emulator within the MOTODEV
 * Studio is not available on MAC OS. This is why the methods are not filled.
 */
public class NativeUI implements INativeUI {
	String DEFAULT_COMMANDLINE = "-no-window";

	String DEFAULT_USEVNC = "true";

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.motorola.studio.android.nativeos.INativeUI#getDefaultCommandLine()
	 */
	public String getDefaultCommandLine() {
		return DEFAULT_COMMANDLINE;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.nativeos.INativeUI#getDefaultUseVnc()
	 */
	public String getDefaultUseVnc() {
		return DEFAULT_USEVNC;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.motorola.studio.android.nativeos.INativeUI#getWindowHandle(java.lang
	 * .String)
	 */
	public long getWindowHandle(String windowName) {
		// Not needed
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.motorola.studio.android.nativeos.INativeUI#getWindowProperties(long)
	 */
	public long getWindowProperties(long windowHandle) {
		// Not needed
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.nativeos.INativeUI#getWindowSize(long,
	 * long)
	 */
	public Point getWindowSize(long originalHandle, long windowHandle) {
		// Not needed
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.nativeos.INativeUI#hideWindow(long)
	 */
	public void hideWindow(long windowHandle) {
		// Not needed
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.nativeos.INativeUI#isWindowEnabled(long)
	 */
	public boolean isWindowEnabled(long windowHandle) {
		// Not needed
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.nativeos.INativeUI#restoreWindow(long)
	 */
	public void restoreWindow(long windowHandle) {
		// Not needed
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.motorola.studio.android.nativeos.INativeUI#sendNextLayoutCommand(
	 * long, long)
	 */
	public void sendNextLayoutCommand(long originalHandle, long windowHandle) {
		// Not needed
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.nativeos.INativeUI#setWindowFocus(long)
	 */
	public void setWindowFocus(long windowHandle) {
		// Not needed
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.nativeos.INativeUI#embedWindow(long,
	 * org.eclipse.swt.widgets.Composite)
	 */
	public long embedWindow(long windowHandle, Composite composite) {
		// Not needed
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.nativeos.INativeUI#unembedWindow(long,
	 * long)
	 */
	public void unembedWindow(long windowHandle, long originalParent) {
		// Not needed
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.motorola.studio.android.nativeos.INativeUI#setWindowProperties(long,
	 * long)
	 */
	public void setWindowProperties(long windowHandle, long originalProperties) {
		// Not needed
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.nativeos.INativeUI#setWindowStyle(long)
	 */
	public void setWindowStyle(long windowHandle) {
		// Not needed
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.nativeos.INativeUI#showWindow(long)
	 */
	public void showWindow(long windowHandle) {
		// Not needed
	}

}

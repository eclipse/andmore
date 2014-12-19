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

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;

public interface INativeUI {
	public String getDefaultUseVnc();

	public String getDefaultCommandLine();

	public long getWindowHandle(String windowName);

	public long getWindowProperties(long windowHandle);

	public void setWindowProperties(long windowHandle, long originalProperties);

	public long embedWindow(long windowHandle, Composite composite);

	public void unembedWindow(long windowHandle, long originalParent);

	public Point getWindowSize(long originalParentHandle, long windowHandle);

	public void setWindowStyle(long windowHandle);

	public void hideWindow(long windowHandle);

	public void showWindow(long windowHandle);

	public void restoreWindow(long windowHandle);

	public void sendNextLayoutCommand(long originalParent, long windowHandle);

	public boolean isWindowEnabled(long windowHandle);

	public void setWindowFocus(long windowHandle);
}
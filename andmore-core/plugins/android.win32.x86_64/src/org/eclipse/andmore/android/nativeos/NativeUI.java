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

import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.nativeos.INativeUI;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.internal.win32.OS;
import org.eclipse.swt.internal.win32.RECT;
import org.eclipse.swt.internal.win32.TCHAR;
import org.eclipse.swt.widgets.Composite;

/***
 * This class is responsible for provide WIN32 X86_64 specific constants values 
 * and implementation of INativeUI interface
 */
@SuppressWarnings("restriction")
public class NativeUI implements INativeUI
{
    private static final int SWP_SHOWWINDOW = 0x0040;

    String DEFAULT_COMMANDLINE = "";

    String DEFAULT_USEVNC = "false";

    /*
     *     (non-Javadoc)
     * @see com.motorola.studio.android.nativeos.INativeUI#getDefaultCommandLine()
     */
    public String getDefaultCommandLine()
    {
        return DEFAULT_COMMANDLINE;
    }

    /*
     * (non-Javadoc)
     * @see com.motorola.studio.android.nativeos.INativeUI#getDefaultUseVnc()
     */
    public String getDefaultUseVnc()
    {
        return DEFAULT_USEVNC;
    }

    /*
     * (non-Javadoc)
     * @see com.motorola.studio.android.nativeos.INativeUI#getWindowHandle(java.lang.String)
     */
    public long getWindowHandle(String windowName)
    {
        StudioLogger.debug(this, "Get native window handler for: " + windowName);
        long windowHandle = 0;
        try
        {
            TCHAR className = null;
            TCHAR tChrTitle = new TCHAR(0, windowName, true);
            windowHandle = OS.FindWindow(className, tChrTitle);
        }
        catch (Throwable t)
        {
            StudioLogger.error(this.getClass(), "Failed to retrieve window handler for window "
                    + windowName, t);
        }

        return windowHandle;
    }

    /*
     * (non-Javadoc)
     * @see com.motorola.studio.android.nativeos.INativeUI#getWindowProperties(long)
     */
    public long getWindowProperties(long windowHandle)
    {
        long windowLong = OS.GetWindowLong(windowHandle, OS.GWL_STYLE);
        return windowLong;
    }

    /*
     * (non-Javadoc)
     * @see com.motorola.studio.android.nativeos.INativeUI#setWindowProperties(long, long)
     */
    public void setWindowProperties(long windowHandle, long originalProperties)
    {
        OS.SetWindowLong(windowHandle, OS.GWL_STYLE, (int) originalProperties);
    }

    /*
     * (non-Javadoc)
     * @see com.motorola.studio.android.nativeos.INativeUI#embedWindow(long, org.eclipse.swt.widgets.Composite)
     */
    public long embedWindow(long windowHandle, Composite composite)
    {
        // Set the position to fix an odd behavior in the Windows Classic Theme - the window goes off-screen and Emulator View stops working
        OS.SetWindowPos(windowHandle, OS.HWND_TOP, 0, 0, 0, 0, SWP_SHOWWINDOW);

        long originalParent = OS.SetParent(windowHandle, composite.handle);
        return originalParent;
    }

    /*
     * (non-Javadoc)
     * @see com.motorola.studio.android.nativeos.INativeUI#unembedWindow(long, long)
     */
    public void unembedWindow(long windowHandle, long originalParent)
    {
        OS.SetParent(windowHandle, originalParent);
    }

    /*
     * (non-Javadoc)
     * @see com.motorola.studio.android.nativeos.INativeUI#getWindowSize(long, long)
     */
    public Point getWindowSize(long originalParentHandle, long windowHandle)
    {
        RECT rect = new RECT();
        OS.GetClientRect(windowHandle, rect);
        return new Point(rect.right, rect.bottom);
    }

    /*
     * (non-Javadoc)
     * @see com.motorola.studio.android.nativeos.INativeUI#setWindowStyle(long)
     */
    public void setWindowStyle(long windowHandle)
    {
        OS.SetWindowLong(windowHandle, OS.GWL_STYLE, OS.WS_VISIBLE | OS.WS_CLIPCHILDREN
                | OS.WS_CLIPSIBLINGS);
    }

    /*
     * (non-Javadoc)
     * @see com.motorola.studio.android.nativeos.INativeUI#hideWindow(long)
     */
    public void hideWindow(long windowHandle)
    {
        OS.ShowWindow(windowHandle, OS.SW_HIDE);
    }

    /*
     * (non-Javadoc)
     * @see com.motorola.studio.android.nativeos.INativeUI#showWindow(long)
     */
    public void showWindow(long windowHandle)
    {
        OS.ShowWindow(windowHandle, OS.SW_SHOW);
    }

    /*
     * (non-Javadoc)
     * @see com.motorola.studio.android.nativeos.INativeUI#restoreWindow(long)
     */
    public void restoreWindow(long windowHandle)
    {
        OS.ShowWindow(windowHandle, OS.SW_SHOWMINIMIZED);
        OS.ShowWindow(windowHandle, OS.SW_RESTORE);
    }

    /*
     * (non-Javadoc)
     * @see com.motorola.studio.android.nativeos.INativeUI#sendNextLayoutCommand(long, long)
     */
    public void sendNextLayoutCommand(long originalParent, long windowHandle)
    {
        OS.SendMessage(windowHandle, OS.WM_KEYDOWN, OS.VK_CONTROL, 0);
        OS.SendMessage(windowHandle, OS.WM_KEYDOWN, OS.VK_F12, 0);
        OS.SendMessage(windowHandle, OS.WM_KEYUP, OS.VK_CONTROL, 0);
        OS.SendMessage(windowHandle, OS.WM_KEYUP, OS.VK_F12, 0);
    }

    /*
     * (non-Javadoc)
     * @see com.motorola.studio.android.nativeos.INativeUI#isWindowEnabled(long)
     */
    public boolean isWindowEnabled(long windowHandle)
    {
        long getFocus = OS.GetForegroundWindow();
        return windowHandle == getFocus;
    }

    /*
     * (non-Javadoc)
     * @see com.motorola.studio.android.nativeos.INativeUI#setWindowFocus(long)
     */
    public void setWindowFocus(long windowHandle)
    {
        OS.SetForegroundWindow(windowHandle);
    }
}
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
import org.eclipse.andmore.android.nativeos.linux.gtk.GtkBridge;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.internal.gtk.OS;
import org.eclipse.swt.widgets.Composite;

/***
 * This class is responsible for provide LINUX X86 specific constants values 
 * and implementation of INativeUI interface
 */
@SuppressWarnings("restriction")
public class NativeUI implements INativeUI
{
    String DEFAULT_COMMANDLINE = "";

    String DEFAULT_USEVNC = "false";

    /*
     * (non-Javadoc)
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
        return GtkBridge.getWindowHandle(windowName);
    }

    /*
     * (non-Javadoc)
     * @see com.motorola.studio.android.nativeos.INativeUI#getWindowProperties(long)
     */
    public long getWindowProperties(long windowHandle)
    {
        return 0;
    }

    /*
     *     (non-Javadoc)
     * @see com.motorola.studio.android.nativeos.INativeUI#setWindowProperties(long, long)
     */
    public void setWindowProperties(long windowHandle, long originalProperties)
    {
        //Do nothing
    }

    /*
     * (non-Javadoc)
     * @see com.motorola.studio.android.nativeos.INativeUI#embedWindow(long, org.eclipse.swt.widgets.Composite)
     */
    public long embedWindow(long windowHandle, Composite composite)
    {
        long hnd = 0;
        hnd = composite.embeddedHandle;
        return GtkBridge.embedNativeWindow(hnd, windowHandle);
    }

    /*
     * (non-Javadoc)
     * @see com.motorola.studio.android.nativeos.INativeUI#unembedWindow(long, long)
     */
    public void unembedWindow(long windowHandle, long originalParent)
    {
        GtkBridge.unembedNativeWindow(windowHandle, originalParent);
    }

    /*
     * (non-Javadoc)
     * @see com.motorola.studio.android.nativeos.INativeUI#getWindowSize(long, long)
     */
    public Point getWindowSize(long originalWindowHandle, long windowHandle)
    {
        return GtkBridge.getWindowSize(originalWindowHandle);
    }

    /*
     * (non-Javadoc)
     * @see com.motorola.studio.android.nativeos.INativeUI#setWindowStyle(long)
     */
    public void setWindowStyle(long windowHandle)
    {
        //Not needed on Linux
    }

    /*
     * (non-Javadoc)
     * @see com.motorola.studio.android.nativeos.INativeUI#hideWindow(long)
     */
    public void hideWindow(long windowHandle)
    {
        GtkBridge.hideWindow(windowHandle);
    }

    /*
     *     (non-Javadoc)
     * @see com.motorola.studio.android.nativeos.INativeUI#showWindow(long)
     */
    public void showWindow(long windowHandle)
    {
        GtkBridge.showWindow(windowHandle);
    }

    /*
     * (non-Javadoc)
     * @see com.motorola.studio.android.nativeos.INativeUI#restoreWindow(long)
     */
    public void restoreWindow(long windowHandle)
    {
        GtkBridge.restoreWindow(windowHandle);
    }

    /*
     * (non-Javadoc)
     * @see com.motorola.studio.android.nativeos.INativeUI#sendNextLayoutCommand(long, long)
     */
    public void sendNextLayoutCommand(long originalParent, long windowHandle)
    {
        GtkBridge.pressKey(originalParent, OS.GDK_Control_L);
        GtkBridge.pressKey(originalParent, OS.GDK_F11);
        GtkBridge.releaseKey(originalParent, OS.GDK_F11);
        GtkBridge.releaseKey(originalParent, OS.GDK_Control_L);
    }

    /*
     * (non-Javadoc)
     * @see com.motorola.studio.android.nativeos.INativeUI#isWindowEnabled(long)
     */
    public boolean isWindowEnabled(long windowHandle)
    {
        // Current not needed on Linux
        return false;
    }

    /*
     * (non-Javadoc)
     * @see com.motorola.studio.android.nativeos.INativeUI#setWindowFocus(long)
     */
    public void setWindowFocus(long windowHandle)
    {
        // Current not needed on Linux
    }
}
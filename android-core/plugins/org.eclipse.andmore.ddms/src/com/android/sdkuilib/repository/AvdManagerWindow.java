/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.sdkuilib.repository;

import com.android.sdkuilib.internal.repository.ui.AvdManagerWindowImpl1;
import com.android.sdkuilib.internal.widgets.AvdSelector;
import com.android.utils.ILogger;

import org.eclipse.swt.widgets.Shell;

/**
 * Opens an AVD Manager Window.
 *
 * This is the public entry point for using the window.
 */
public class AvdManagerWindow {

    /** The actual window implementation to which this class delegates. */
    private AvdManagerWindowImpl1 mWindow;

    /**
     * Enum giving some indication of what is invoking this window.
     * The behavior and UI will change slightly depending on the context.
     * <p/>
     * Note: if you add Android support to your specific IDE, you might want
     * to specialize this context enum.
     */
    public enum AvdInvocationContext {
        /**
         * The AVD Manager is invoked from the stand-alone 'android' tool.
         * In this mode, we present an about box, a settings page.
         * For SdkMan2, we also have a menu bar and link to the SDK Manager 2.
         */
        STANDALONE,

        /**
         * The AVD Manager is embedded as a dialog in the SDK Manager
         * or in the {@link AvdSelector}.
         * This is similar to the {@link #STANDALONE} mode except we don't need
         * to display a menu bar at all since we don't want a menu item linking
         * back to the SDK Manager and we don't need to redisplay the options
         * and about which are already on the root window.
         */
        DIALOG,

        /**
         * The AVD Manager is invoked from an IDE.
         * In this mode, we do not modify the menu bar.
         * There is no about box and no settings.
         */
        IDE,
    }


    /**
     * Creates a new window. Caller must call open(), which will block.
     *
     * @param parentShell Parent shell.
     * @param sdkLog Logger. Cannot be null.
     * @param osSdkRoot The OS path to the SDK root.
     * @param context The {@link AvdInvocationContext} to change the behavior depending on who's
     *  opening the SDK Manager.
     */
    public AvdManagerWindow(
            Shell parentShell,
            ILogger sdkLog,
            String osSdkRoot,
            AvdInvocationContext context) {
        mWindow = new AvdManagerWindowImpl1(
                parentShell,
                sdkLog,
                osSdkRoot,
                context);
    }

    /**
     * Opens the window.
     */
    public void open() {
        mWindow.open();
    }
}

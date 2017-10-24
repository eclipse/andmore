/*
 * Copyright (C) 2009 The Android Open Source Project
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

import com.android.sdklib.repository.ISdkChangeListener;
import com.android.sdkuilib.internal.repository.ISdkUpdaterWindow;
import com.android.sdkuilib.internal.repository.ui.SdkUpdaterWindowImpl2;
import com.android.utils.ILogger;

import org.eclipse.swt.widgets.Shell;

/**
 * Opens an SDK Manager Window.
 *
 * This is the public entry point for using the window.
 */
public class SdkUpdaterWindow {

    /** The actual window implementation to which this class delegates. */
    private ISdkUpdaterWindow mWindow;

    /**
     * Enum giving some indication of what is invoking this window.
     * The behavior and UI will change slightly depending on the context.
     * <p/>
     * Note: if you add Android support to your specific IDE, you might want
     * to specialize this context enum.
     */
    public enum SdkInvocationContext {
        /**
         * The SDK Manager is invoked from the stand-alone 'android' tool.
         * In this mode, we present an about box, a settings page.
         * For SdkMan2, we also have a menu bar and link to the AVD manager.
         */
        STANDALONE,

        /**
         * The SDK Manager is invoked from the standalone AVD Manager.
         * This is similar to the standalone mode except that in this case we
         * don't display a menu item linking to the AVD Manager.
         */
        AVD_MANAGER,

        /**
         * The SDK Manager is invoked from an IDE.
         * In this mode, we do not modify the menu bar. There is no about box
         * and no settings (e.g. HTTP proxy settings are inherited from Eclipse.)
         */
        IDE,

        /**
         * The SDK Manager is invoked from the AVD Selector.
         * For SdkMan1, this means the AVD page will be displayed first.
         * For SdkMan2, we won't be using this.
         */
        AVD_SELECTOR
    }

    /**
     * Creates a new window. Caller must call open(), which will block.
     *
     * @param parentShell Parent shell.
     * @param sdkLog Logger. Cannot be null.
     * @param osSdkRoot The OS path to the SDK root.
     * @param context The {@link SdkInvocationContext} to change the behavior depending on who's
     *  opening the SDK Manager.
     */
    public SdkUpdaterWindow(
            Shell parentShell,
            ILogger sdkLog,
            String osSdkRoot,
            SdkInvocationContext context) {

        mWindow = new SdkUpdaterWindowImpl2(parentShell, sdkLog, osSdkRoot, context);
    }

    /**
     * Adds a new listener to be notified when a change is made to the content of the SDK.
     * This should be called before {@link #open()}.
     */
    public void addListener(ISdkChangeListener listener) {
        mWindow.addListener(listener);
    }

    /**
     * Removes a new listener to be notified anymore when a change is made to the content of
     * the SDK.
     */
    public void removeListener(ISdkChangeListener listener) {
        mWindow.removeListener(listener);
    }

    /**
     * Opens the window.
     */
    public void open() {
        mWindow.open();
    }
}

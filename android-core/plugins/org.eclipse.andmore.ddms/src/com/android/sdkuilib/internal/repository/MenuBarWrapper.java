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

package com.android.sdkuilib.internal.repository;


import com.android.menubar.IMenuBarCallback;
import com.android.menubar.MenuBarEnhancer;
import com.android.sdkuilib.internal.repository.ui.SdkUpdaterWindowImpl2;

import org.eclipse.swt.widgets.Menu;

/**
 * A simple wrapper/delegate around the {@link MenuBarEnhancer}.
 *
 * The {@link MenuBarEnhancer} and {@link IMenuBarCallback} classes are only
 * available when the SwtMenuBar library is available too. This wrapper helps
 * {@link SdkUpdaterWindowImpl2} make the call conditional, otherwise the updater
 * window class would fail to load when the SwtMenuBar library isn't found.
 */
public abstract class MenuBarWrapper {

    public MenuBarWrapper(String appName, Menu menu) {
        MenuBarEnhancer.setupMenu(appName, menu, new IMenuBarCallback() {
            @Override
            public void onPreferencesMenuSelected() {
                MenuBarWrapper.this.onPreferencesMenuSelected();
            }

            @Override
            public void onAboutMenuSelected() {
                MenuBarWrapper.this.onAboutMenuSelected();
            }

            @Override
            public void printError(String format, Object... args) {
                MenuBarWrapper.this.printError(format, args);
            }
        });
    }

    abstract public void onPreferencesMenuSelected();

    abstract public void onAboutMenuSelected();

    abstract public void printError(String format, Object... args);
}

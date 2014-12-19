/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.eclipse.andmore.internal.assetstudio;

/**
 * The type of asset to create: launcher icon, menu icon, etc.
 */
public enum AssetType {
    /** Launcher icon to be shown in the application list */
    LAUNCHER("Launcher Icons", "ic_launcher"),                             //$NON-NLS-2$

    /** Icons shown in the action bar */
    ACTIONBAR("Action Bar and Tab Icons (Android 3.0+)", "ic_action_%s"),  //$NON-NLS-2$

    /** Icons shown in a notification message */
    NOTIFICATION("Notification Icons", "ic_stat_%s"),                      //$NON-NLS-2$

    /** Icons shown as part of tabs */
    TAB("Pre-Android 3.0 Tab Icons", "ic_tab_%s"),                         //$NON-NLS-2$

    /** Icons shown in menus */
    MENU("Pre-Android 3.0 Menu Icons", "ic_menu_%s");                      //$NON-NLS-2$

    /** Display name to show to the user in the asset type selection list */
    private final String mDisplayName;

    /** Default asset name format */
    private String mDefaultNameFormat;

    AssetType(String displayName, String defaultNameFormat) {
        mDisplayName = displayName;
        mDefaultNameFormat = defaultNameFormat;
    }

    /**
     * Returns the display name of this asset type to show to the user in the
     * asset wizard selection page etc
     */
    String getDisplayName() {
        return mDisplayName;
    }

    /**
     * Returns the default format to use to suggest a name for the asset
     */
    String getDefaultNameFormat() {
        return mDefaultNameFormat;
    }

    /** Whether this asset type configures foreground scaling */
    boolean needsForegroundScaling() {
        return this == LAUNCHER;
    }

    /** Whether this asset type needs a shape parameter */
    boolean needsShape() {
        return this == LAUNCHER;
    }

    /** Whether this asset type needs foreground and background color parameters */
    boolean needsColors() {
        return this == LAUNCHER;
    }

    /** Whether this asset type needs an effects parameter */
    boolean needsEffects() {
        return this == LAUNCHER;
    }

    /** Whether this asset type needs a theme parameter */
    boolean needsTheme() {
        return this == ACTIONBAR;
    }
}

/*
 * Copyright (C) 2012 The Android Open Source Project
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

package org.eclipse.andmore.internal.editors.layout.configuration;

import static com.android.SdkConstants.ANDROID_STYLE_RESOURCE_PREFIX;
import static com.android.SdkConstants.STYLE_RESOURCE_PREFIX;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;

/**
 * Action which brings up the "Create new XML File" wizard, pre-selected with the
 * animation category
 */
class SelectThemeAction extends Action {
    private final ConfigurationChooser mConfiguration;
    private final String mTheme;

    public SelectThemeAction(ConfigurationChooser configuration, String title, String theme,
            boolean selected) {
        super(title, IAction.AS_RADIO_BUTTON);
        assert theme.startsWith(STYLE_RESOURCE_PREFIX)
            || theme.startsWith(ANDROID_STYLE_RESOURCE_PREFIX) : theme;
        mConfiguration = configuration;
        mTheme = theme;
        if (selected) {
            setChecked(selected);
        }
    }

    @Override
    public void run() {
        mConfiguration.selectTheme(mTheme);
        mConfiguration.onThemeChange();
    }
}

/*
 * Copyright (C) 2014 The Android Open Source Project
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

package org.eclipse.andmore.internal.editors.layout;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.andmore.internal.editors.layout.gle2.GraphicalEditorPart;
import org.eclipse.andmore.internal.editors.manifest.ManifestInfo;
import org.eclipse.andmore.internal.editors.manifest.ManifestInfo.ActivityAttributes;
import org.w3c.dom.Element;

import com.android.ide.common.rendering.api.ActionBarCallback;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;

import static com.android.SdkConstants.TOOLS_URI;
import static com.android.SdkConstants.VALUE_SPLIT_ACTION_BAR_WHEN_NARROW;

public class ActionBarHandler extends ActionBarCallback {

    private final GraphicalEditorPart mEditor;

    ActionBarHandler(GraphicalEditorPart editor) {
        mEditor = editor;
    }

    @Override
    public List<String> getMenuIdNames() {
        String commaSeparatedMenus = getXmlAttribute(ATTR_MENU);
        List<String> menus = new ArrayList<String>();
        Iterables.addAll(menus, Splitter.on(',').trimResults().omitEmptyStrings()
                .split(commaSeparatedMenus));
        return menus;
    }

    @Override
    public boolean getSplitActionBarWhenNarrow() {
        ActivityAttributes attributes = getActivityAttributes();
        if (attributes != null) {
          return VALUE_SPLIT_ACTION_BAR_WHEN_NARROW.equals(attributes.getUiOptions());
        }
        return false;
    }

    @Override
    public int getNavigationMode() {
        String navMode = getXmlAttribute(ATTR_NAV_MODE);
        if (navMode.equalsIgnoreCase(VALUE_NAV_MODE_TABS)) {
          return NAVIGATION_MODE_TABS;
        }
        if (navMode.equalsIgnoreCase(VALUE_NAV_MODE_LIST)) {
          return NAVIGATION_MODE_LIST;
        }
        return NAVIGATION_MODE_STANDARD;
    }

    @Override
    public HomeButtonStyle getHomeButtonStyle() {
        ActivityAttributes attributes = getActivityAttributes();
        if (attributes != null && attributes.getParentActivity() != null) {
          return HomeButtonStyle.SHOW_HOME_AS_UP;
        }
        return HomeButtonStyle.NONE;
    }

    private ActivityAttributes getActivityAttributes() {
        ManifestInfo manifest = ManifestInfo.get(mEditor.getProject());
        String activity = mEditor.getConfigurationChooser().getConfiguration().getActivity();
        return manifest.getActivityAttributes(activity);
    }

    private String getXmlAttribute(String name) {
        Element element = mEditor.getModel().getUiRoot().getXmlDocument().getDocumentElement();
        String value = element.getAttributeNS(TOOLS_URI, name);
        if (value == null) {
            return "";
        }
        return value.trim();
    }
}

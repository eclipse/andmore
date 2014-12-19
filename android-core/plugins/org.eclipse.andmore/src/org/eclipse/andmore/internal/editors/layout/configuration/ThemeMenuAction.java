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

import com.android.sdklib.IAndroidTarget;

import org.eclipse.andmore.internal.editors.Hyperlinks;
import org.eclipse.andmore.internal.editors.layout.gle2.SubmenuAction;
import org.eclipse.andmore.internal.editors.manifest.ManifestInfo;
import org.eclipse.andmore.internal.editors.manifest.ManifestInfo.ActivityAttributes;
import org.eclipse.andmore.internal.resources.ResourceHelper;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Action which creates a submenu displaying available themes
 */
class ThemeMenuAction extends SubmenuAction {
    private static final String DEVICE_LIGHT_PREFIX =
            ANDROID_STYLE_RESOURCE_PREFIX + "Theme.DeviceDefault.Light";  //$NON-NLS-1$
    private static final String HOLO_LIGHT_PREFIX =
            ANDROID_STYLE_RESOURCE_PREFIX + "Theme.Holo.Light";           //$NON-NLS-1$
    private static final String DEVICE_PREFIX =
            ANDROID_STYLE_RESOURCE_PREFIX + "Theme.DeviceDefault";        //$NON-NLS-1$
    private static final String HOLO_PREFIX =
            ANDROID_STYLE_RESOURCE_PREFIX + "Theme.Holo";                 //$NON-NLS-1$
    private static final String LIGHT_PREFIX =
            ANDROID_STYLE_RESOURCE_PREFIX +"Theme.Light";                 //$NON-NLS-1$
    private static final String THEME_PREFIX =
            ANDROID_STYLE_RESOURCE_PREFIX +"Theme";                       //$NON-NLS-1$

    // Constants used to indicate what type of menu is being shown, such that
    // the submenus can lazily construct their contents
    private static final int MENU_MANIFEST = 1;
    private static final int MENU_PROJECT = 2;
    private static final int MENU_THEME = 3;
    private static final int MENU_THEME_LIGHT = 4;
    private static final int MENU_HOLO = 5;
    private static final int MENU_HOLO_LIGHT = 6;
    private static final int MENU_DEVICE = 7;
    private static final int MENU_DEVICE_LIGHT = 8;
    private static final int MENU_ALL = 9;

    private final ConfigurationChooser mConfigChooser;
    private final List<String> mThemeList;
    /** Type of menu; one of the constants {@link #MENU_ALL} etc */
    private final int mType;

    ThemeMenuAction(int type, String title, ConfigurationChooser configuration,
            List<String> themeList) {
        super(title);
        mType = type;
        mConfigChooser = configuration;
        mThemeList = themeList;
    }

    static void showThemeMenu(ConfigurationChooser configChooser, ToolItem combo,
            List<String> themeList) {
        MenuManager manager = new MenuManager();

        // First show the currently selected theme (grayed out since you can't
        // reselect it)
        Configuration configuration = configChooser.getConfiguration();
        String currentTheme = configuration.getTheme();
        String currentName = null;
        if (currentTheme != null) {
            currentName = ResourceHelper.styleToTheme(currentTheme);
            SelectThemeAction action = new SelectThemeAction(configChooser,
                    currentName,
                    currentTheme,
                    true /* selected */);
            action.setEnabled(false);
            manager.add(action);
            manager.add(new Separator());
        }

        String preferred = configuration.computePreferredTheme();
        if (preferred != null && !preferred.equals(currentTheme)) {
            manager.add(new SelectThemeAction(configChooser,
                    ResourceHelper.styleToTheme(preferred),
                    preferred, false /* selected */));
            manager.add(new Separator());
        }

        IAndroidTarget target = configuration.getTarget();
        int apiLevel = target != null ? target.getVersion().getApiLevel() : 1;
        boolean hasHolo = apiLevel >= 11;   // Honeycomb
        boolean hasDeviceDefault = apiLevel >= 14; // ICS

        // TODO: Add variations of the current theme here, e.g.
        // if you're using Theme.Holo, add Theme.Holo.Dialog, Theme.Holo.Panel,
        // Theme.Holo.Wallpaper etc

        manager.add(new ThemeMenuAction(MENU_PROJECT, "Project Themes",
                configChooser, themeList));
        manager.add(new ThemeMenuAction(MENU_MANIFEST, "Manifest Themes",
                configChooser, themeList));

        manager.add(new Separator());

        if (hasHolo) {
            manager.add(new ThemeMenuAction(MENU_HOLO, "Holo",
                    configChooser, themeList));
            manager.add(new ThemeMenuAction(MENU_HOLO_LIGHT, "Holo.Light",
                    configChooser, themeList));
        }
        if (hasDeviceDefault) {
            manager.add(new ThemeMenuAction(MENU_DEVICE, "DeviceDefault",
                    configChooser, themeList));
            manager.add(new ThemeMenuAction(MENU_DEVICE_LIGHT, "DeviceDefault.Light",
                    configChooser, themeList));
        }
        manager.add(new ThemeMenuAction(MENU_THEME, "Theme",
                configChooser, themeList));
        manager.add(new ThemeMenuAction(MENU_THEME_LIGHT, "Theme.Light",
                configChooser, themeList));

        // TODO: Add generic types like Wallpaper, Dialog, Alert, etc here, with
        // submenus for picking it within each theme category?

        manager.add(new Separator());
        manager.add(new ThemeMenuAction(MENU_ALL, "All",
                configChooser, themeList));

        if (currentTheme != null) {
            assert currentName != null;
            manager.add(new Separator());
            String title = String.format("Open %1$s Declaration...", currentName);
            manager.add(new OpenThemeAction(title, configChooser.getEditedFile(), currentTheme));
        }

        Menu menu = manager.createContextMenu(configChooser.getShell());

        Rectangle bounds = combo.getBounds();
        Point location = new Point(bounds.x, bounds.y + bounds.height);
        location = combo.getParent().toDisplay(location);
        menu.setLocation(location.x, location.y);
        menu.setVisible(true);
    }

    @Override
    protected void addMenuItems(Menu menu) {
        switch (mType) {
            case MENU_ALL:
                addMenuItems(menu, mThemeList);
                break;

            case MENU_MANIFEST: {
                IProject project = mConfigChooser.getEditedFile().getProject();
                ManifestInfo manifest = ManifestInfo.get(project);
                Configuration configuration = mConfigChooser.getConfiguration();
                String activity = configuration.getActivity();
                if (activity != null) {
                    ActivityAttributes attributes = manifest.getActivityAttributes(activity);
                    if (attributes != null) {
                        String theme = attributes.getTheme();
                        if (theme != null) {
                            addMenuItem(menu, theme, isSelectedTheme(theme));
                        }
                    }
                }

                String manifestTheme = manifest.getManifestTheme();
                boolean found = false;
                Set<String> allThemes = new HashSet<String>();
                if (manifestTheme != null) {
                    found = true;
                    allThemes.add(manifestTheme);
                }
                for (ActivityAttributes info : manifest.getActivityAttributesMap().values()) {
                    if (info.getTheme() != null) {
                        found = true;
                        allThemes.add(info.getTheme());
                    }
                }
                List<String> sorted = new ArrayList<String>(allThemes);
                Collections.sort(sorted);
                String current = configuration.getTheme();
                for (String theme : sorted) {
                    boolean selected = theme.equals(current);
                    addMenuItem(menu, theme, selected);
                }
                if (!found) {
                    addDisabledMessageItem("No themes are registered in the manifest");
                }
                break;
            }
            case MENU_PROJECT: {
                int size = mThemeList.size();
                List<String> themes = new ArrayList<String>(size);
                for (int i = 0; i < size; i++) {
                    String theme = mThemeList.get(i);
                    if (ResourceHelper.isProjectStyle(theme)) {
                        themes.add(theme);
                    }
                }
                if (themes.isEmpty()) {
                    addDisabledMessageItem("There are no local theme styles in the project");
                } else {
                    addMenuItems(menu, themes);
                }
                break;
            }
            case MENU_THEME: {
                // Can't just use the usual filterThemes() call here because we need
                // to exclude on multiple prefixes: Holo, DeviceDefault, Light, ...
                List<String> themes = new ArrayList<String>(mThemeList.size());
                for (String theme : mThemeList) {
                    if (theme.startsWith(THEME_PREFIX)
                            && !theme.startsWith(LIGHT_PREFIX)
                            && !theme.startsWith(HOLO_PREFIX)
                            && !theme.startsWith(DEVICE_PREFIX)) {
                        themes.add(theme);
                    }
                }

                addMenuItems(menu, themes);
                break;
            }
            case MENU_THEME_LIGHT:
                addMenuItems(menu, filterThemes(LIGHT_PREFIX, null));
                break;
            case MENU_HOLO:
                addMenuItems(menu, filterThemes(HOLO_PREFIX, HOLO_LIGHT_PREFIX));
                break;
            case MENU_HOLO_LIGHT:
                addMenuItems(menu, filterThemes(HOLO_LIGHT_PREFIX, null));
                break;
            case MENU_DEVICE:
                addMenuItems(menu, filterThemes(DEVICE_PREFIX, DEVICE_LIGHT_PREFIX));
                break;
            case MENU_DEVICE_LIGHT:
                addMenuItems(menu, filterThemes(DEVICE_LIGHT_PREFIX, null));
                break;
        }
    }

    private List<String> filterThemes(String include, String exclude) {
        List<String> themes = new ArrayList<String>(mThemeList.size());
        for (String theme : mThemeList) {
            if (theme.startsWith(include) && (exclude == null || !theme.startsWith(exclude))) {
                themes.add(theme);
            }
        }

        return themes;
    }

    private void addMenuItems(Menu menu, List<String> themes) {
        String current = mConfigChooser.getConfiguration().getTheme();
        for (String theme : themes) {
            addMenuItem(menu, theme, theme.equals(current));
        }
    }

    private boolean isSelectedTheme(String theme) {
        return theme.equals(mConfigChooser.getConfiguration().getTheme());
    }

    private void addMenuItem(Menu menu, String theme, boolean selected) {
        String title = ResourceHelper.styleToTheme(theme);
        SelectThemeAction action = new SelectThemeAction(mConfigChooser, title, theme, selected);
        new ActionContributionItem(action).fill(menu, -1);
    }

    private static class OpenThemeAction extends Action {
        private final String mTheme;
        private final IFile mFile;

        private OpenThemeAction(String title, IFile file, String theme) {
            super(title, IAction.AS_PUSH_BUTTON);
            mFile = file;
            mTheme = theme;
        }

        @Override
        public void run() {
            IProject project = mFile.getProject();
            IHyperlink[] links = Hyperlinks.getResourceLinks(null, mTheme, project, null);
            if (links != null && links.length > 0) {
                IHyperlink link = links[0];
                link.open();
            }
        }
    }
}

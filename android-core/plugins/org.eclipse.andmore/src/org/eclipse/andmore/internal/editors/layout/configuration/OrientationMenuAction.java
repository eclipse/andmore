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

import com.android.resources.NightMode;
import com.android.resources.ScreenOrientation;
import com.android.resources.UiMode;
import com.android.sdklib.devices.Device;
import com.android.sdklib.devices.State;

import org.eclipse.andmore.internal.editors.layout.gle2.SubmenuAction;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolItem;

import java.util.List;

/**
 * Action which creates a submenu that shows the available orientations as well
 * as some related options for night mode and dock mode
 */
class OrientationMenuAction extends SubmenuAction {
    // Constants used to indicate what type of menu is being shown, such that
    // the submenus can lazily construct their contents
    private static final int MENU_NIGHTMODE = 1;
    private static final int MENU_UIMODE = 2;

    private final ConfigurationChooser mConfigChooser;
    /** Type of menu; one of the constants {@link #MENU_NIGHTMODE} etc */
    private final int mType;

    OrientationMenuAction(int type, String title, ConfigurationChooser configuration) {
        super(title);
        mType = type;
        mConfigChooser = configuration;
    }

    static void showMenu(ConfigurationChooser configChooser, ToolItem combo) {
        MenuManager manager = new MenuManager();

        // Show toggles for all the available states

        Configuration configuration = configChooser.getConfiguration();
        Device device = configuration.getDevice();
        State current = configuration.getDeviceState();
        if (device != null) {
            List<State> states = device.getAllStates();

            if (states.size() > 1 && current != null) {
                State flip = configuration.getNextDeviceState(current);
                String flipName = flip != null ? flip.getName() : current.getName();
                manager.add(new DeviceConfigAction(configChooser,
                        String.format("Switch to %1$s", flipName), flip, false, true));
                manager.add(new Separator());
            }

            for (State config : states) {
                manager.add(new DeviceConfigAction(configChooser, config.getName(),
                        config, config == current, false));
            }
            manager.add(new Separator());
        }
        manager.add(new OrientationMenuAction(MENU_UIMODE, "UI Mode", configChooser));
        manager.add(new Separator());
        manager.add(new OrientationMenuAction(MENU_NIGHTMODE, "Night Mode", configChooser));

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
            case MENU_NIGHTMODE: {
                NightMode selected = mConfigChooser.getConfiguration().getNightMode();
                for (NightMode mode : NightMode.values()) {
                    boolean checked = mode == selected;
                    SelectNightModeAction action = new SelectNightModeAction(mode, checked);
                    new ActionContributionItem(action).fill(menu, -1);

                }
                break;
            }
            case MENU_UIMODE: {
                UiMode selected = mConfigChooser.getConfiguration().getUiMode();
                for (UiMode mode : UiMode.values()) {
                    boolean checked = mode == selected;
                    SelectUiModeAction action = new SelectUiModeAction(mode, checked);
                    new ActionContributionItem(action).fill(menu, -1);
                }
                break;
            }
        }
    }


    private class SelectNightModeAction extends Action {
        private final NightMode mMode;

        private SelectNightModeAction(NightMode mode, boolean checked) {
            super(mode.getLongDisplayValue(), IAction.AS_RADIO_BUTTON);
            mMode = mode;
            if (checked) {
                setChecked(true);
            }
        }

        @Override
        public void run() {
            Configuration configuration = mConfigChooser.getConfiguration();
            configuration.setNightMode(mMode, false);
            mConfigChooser.notifyFolderConfigChanged();
        }
    }

    private class SelectUiModeAction extends Action {
        private final UiMode mMode;

        private SelectUiModeAction(UiMode mode, boolean checked) {
            super(mode.getLongDisplayValue(), IAction.AS_RADIO_BUTTON);
            mMode = mode;
            if (checked) {
                setChecked(true);
            }
        }

        @Override
        public void run() {
            Configuration configuration = mConfigChooser.getConfiguration();
            configuration.setUiMode(mMode, false);
        }
    }

    private static class DeviceConfigAction extends Action {
        private final ConfigurationChooser mConfiguration;
        private final State mState;

        private DeviceConfigAction(ConfigurationChooser configuration, String title,
                State state, boolean checked, boolean flip) {
            super(title, IAction.AS_RADIO_BUTTON);
            mConfiguration = configuration;
            mState = state;
            if (checked) {
                setChecked(true);
            }
            ScreenOrientation orientation = configuration.getOrientation(state);
            setImageDescriptor(configuration.getOrientationImage(orientation, flip));
        }

        @Override
        public void run() {
            mConfiguration.selectDeviceState(mState);
            mConfiguration.onDeviceConfigChange();
        }
    }
}

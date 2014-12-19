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

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.editors.manifest.ManifestInfo;
import org.eclipse.andmore.internal.resources.ResourceHelper;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolItem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The {@linkplain ActivityMenuListener} class is responsible for
 * generating the activity menu in the {@link ConfigurationChooser}.
 */
class ActivityMenuListener extends SelectionAdapter {
    private static final int ACTION_OPEN_ACTIVITY = 1;
    private static final int ACTION_SELECT_ACTIVITY = 2;

    private final ConfigurationChooser mConfigChooser;
    private final int mAction;
    private final String mFqcn;

    ActivityMenuListener(
            @NonNull ConfigurationChooser configChooser,
            int action,
            @Nullable String fqcn) {
        mConfigChooser = configChooser;
        mAction = action;
        mFqcn = fqcn;
    }

    @Override
    public void widgetSelected(SelectionEvent e) {
        switch (mAction) {
            case ACTION_OPEN_ACTIVITY: {
                Configuration configuration = mConfigChooser.getConfiguration();
                String fqcn = configuration.getActivity();
                AdtPlugin.openJavaClass(mConfigChooser.getProject(), fqcn);
                break;
            }
            case ACTION_SELECT_ACTIVITY: {
                mConfigChooser.selectActivity(mFqcn);
                mConfigChooser.onSelectActivity();
                break;
            }
            default: assert false : mAction;
        }
    }

    static void show(ConfigurationChooser chooser, ToolItem combo) {
        // TODO: Allow using fragments here as well?
        Menu menu = new Menu(chooser.getShell(), SWT.POP_UP);
        ISharedImages sharedImages = JavaUI.getSharedImages();
        Configuration configuration = chooser.getConfiguration();
        String current = configuration.getActivity();

        if (current != null) {
            MenuItem item = new MenuItem(menu, SWT.PUSH);
            String label = ConfigurationChooser.getActivityLabel(current, true);
            item.setText( String.format("Open %1$s...", label));
            Image image = sharedImages.getImage(ISharedImages.IMG_OBJS_CUNIT);
            item.setImage(image);
            item.addSelectionListener(
                    new ActivityMenuListener(chooser, ACTION_OPEN_ACTIVITY, null));

            @SuppressWarnings("unused")
            MenuItem separator = new MenuItem(menu, SWT.SEPARATOR);
        }

        IProject project = chooser.getProject();
        Image image = sharedImages.getImage(ISharedImages.IMG_OBJS_CLASS);

        // Add activities found to be relevant to this layout
        String layoutName = ResourceHelper.getLayoutName(chooser.getEditedFile());
        String pkg = ManifestInfo.get(project).getPackage();
        List<String> preferred = ManifestInfo.guessActivities(project, layoutName, pkg);
        current = addActivities(chooser, menu, current, image, preferred);

        // Add all activities
        List<String> activities = ManifestInfo.getProjectActivities(project);
        if (preferred.size() > 0) {
            // Filter out the activities we've already listed above
            List<String> filtered = new ArrayList<String>(activities.size());
            Set<String> remove = new HashSet<String>(preferred);
            for (String fqcn : activities) {
                if (!remove.contains(fqcn)) {
                    filtered.add(fqcn);
                }
            }
            activities = filtered;
        }

        if (activities.size() > 0) {
            if (preferred.size() > 0) {
                @SuppressWarnings("unused")
                MenuItem separator = new MenuItem(menu, SWT.SEPARATOR);
            }

            addActivities(chooser, menu, current, image, activities);
        }

        Rectangle bounds = combo.getBounds();
        Point location = new Point(bounds.x, bounds.y + bounds.height);
        location = combo.getParent().toDisplay(location);
        menu.setLocation(location.x, location.y);
        menu.setVisible(true);
    }

    private static String addActivities(ConfigurationChooser chooser, Menu menu, String current,
            Image image, List<String> activities) {
        for (final String fqcn : activities) {
            String title = ConfigurationChooser.getActivityLabel(fqcn, false);
            MenuItem item = new MenuItem(menu, SWT.CHECK);
            item.setText(title);
            item.setImage(image);

            boolean selected = title.equals(current);
            if (selected) {
                item.setSelection(true);
                current = null; // Only show the first occurrence as selected
                // such that we don't show it selected again in the full activity list
            }

            item.addSelectionListener(new ActivityMenuListener(chooser,
                    ACTION_SELECT_ACTIVITY, fqcn));
        }

        return current;
    }
}

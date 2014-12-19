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
import com.android.sdklib.AndroidVersion;
import com.android.sdklib.IAndroidTarget;

import org.eclipse.andmore.internal.preferences.AdtPrefs;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolItem;

import java.util.List;
import java.util.RandomAccess;

/**
 * The {@linkplain TargetMenuListener} class is responsible for
 * generating the rendering target menu in the {@link ConfigurationChooser}.
 */
class TargetMenuListener extends SelectionAdapter {
    private final ConfigurationChooser mConfigChooser;
    private final IAndroidTarget mTarget;
    private final boolean mPickBest;

    TargetMenuListener(
            @NonNull ConfigurationChooser configChooser,
            @Nullable IAndroidTarget target,
            boolean pickBest) {
        mConfigChooser = configChooser;
        mTarget = target;
        mPickBest = pickBest;
    }

    @Override
    public void widgetSelected(SelectionEvent e) {
        IAndroidTarget target = mTarget;
        AdtPrefs prefs = AdtPrefs.getPrefs();
        if (mPickBest) {
            boolean autoPick = prefs.isAutoPickRenderTarget();
            autoPick = !autoPick;
            prefs.setAutoPickRenderTarget(autoPick);
            if (autoPick) {
                target = ConfigurationMatcher.findDefaultRenderTarget(mConfigChooser);
            } else {
                // Turn it off, but keep current target until another one is chosen
                return;
            }
        } else {
            // Manually picked some other target: turn off auto-pick
            prefs.setAutoPickRenderTarget(false);
        }
        mConfigChooser.selectTarget(target);
        mConfigChooser.onRenderingTargetChange();
    }

    static void show(ConfigurationChooser chooser, ToolItem combo) {
        Menu menu = new Menu(chooser.getShell(), SWT.POP_UP);
        Configuration configuration = chooser.getConfiguration();
        IAndroidTarget current = configuration.getTarget();
        List<IAndroidTarget> targets = chooser.getTargetList();
        boolean haveRecent = false;

        MenuItem menuItem = new MenuItem(menu, SWT.CHECK);
        menuItem.setText("Automatically Pick Best");
        menuItem.addSelectionListener(new TargetMenuListener(chooser, null, true));
        if (AdtPrefs.getPrefs().isAutoPickRenderTarget()) {
            menuItem.setSelection(true);
        }

        @SuppressWarnings("unused")
        MenuItem separator = new MenuItem(menu, SWT.SEPARATOR);

        // Process in reverse order: most important targets first
        assert targets instanceof RandomAccess;
        for (int i = targets.size() - 1; i >= 0; i--) {
            IAndroidTarget target = targets.get(i);

            AndroidVersion version = target.getVersion();
            if (version.getApiLevel() >= 7) {
                haveRecent = true;
            } else if (haveRecent) {
                // Don't show ancient rendering targets; they're pretty broken
                // (unless of course all you have are ancient targets)
                break;
            }

            String title = ConfigurationChooser.getRenderingTargetLabel(target, false);
            MenuItem item = new MenuItem(menu, SWT.CHECK);
            item.setText(title);

            boolean selected = current == target;
            if (selected) {
                item.setSelection(true);
            }

            item.addSelectionListener(new TargetMenuListener(chooser, target, false));
        }

        Rectangle bounds = combo.getBounds();
        Point location = new Point(bounds.x, bounds.y + bounds.height);
        location = combo.getParent().toDisplay(location);
        menu.setLocation(location.x, location.y);
        menu.setVisible(true);
    }
}

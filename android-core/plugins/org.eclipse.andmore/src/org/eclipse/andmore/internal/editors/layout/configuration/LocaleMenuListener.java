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

import org.eclipse.andmore.internal.editors.layout.gle2.RenderPreviewMode;
import org.eclipse.andmore.internal.wizards.newxmlfile.AddTranslationDialog;
import org.eclipse.core.resources.IProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolItem;

import java.util.List;

/**
 * The {@linkplain LocaleMenuListener} class is responsible for generating the locale
 * menu in the {@link ConfigurationChooser}.
 */
class LocaleMenuListener extends SelectionAdapter {
    private static final int ACTION_SET_LOCALE = 1;
    private static final int ACTION_ADD_TRANSLATION = 2;

    private final ConfigurationChooser mConfigChooser;
    private final int mAction;
    private final Locale mLocale;

    LocaleMenuListener(
            @NonNull ConfigurationChooser configChooser,
            int action,
            @Nullable Locale locale) {
        mConfigChooser = configChooser;
        mAction = action;
        mLocale = locale;
    }

    @Override
    public void widgetSelected(SelectionEvent e) {
        switch (mAction) {
            case ACTION_SET_LOCALE: {
                mConfigChooser.selectLocale(mLocale);
                mConfigChooser.onLocaleChange();
                break;
            }
            case ACTION_ADD_TRANSLATION: {
                IProject project = mConfigChooser.getProject();
                Shell shell = mConfigChooser.getShell();
                AddTranslationDialog dialog = new AddTranslationDialog(shell, project);
                dialog.open();
                break;
            }
            default: assert false : mAction;
        }
    }

    static void show(final ConfigurationChooser chooser, ToolItem combo) {
        Menu menu = new Menu(chooser.getShell(), SWT.POP_UP);
        Configuration configuration = chooser.getConfiguration();
        List<Locale> locales = chooser.getLocaleList();
        Locale current = configuration.getLocale();

        for (Locale locale : locales) {
            String title = ConfigurationChooser.getLocaleLabel(chooser, locale, false);
            MenuItem item = new MenuItem(menu, SWT.CHECK);
            item.setText(title);
            Image image = locale.getFlagImage();
            item.setImage(image);

            boolean selected = current == locale;
            if (selected) {
                item.setSelection(true);
            }

            LocaleMenuListener listener = new LocaleMenuListener(chooser, ACTION_SET_LOCALE,
                    locale);
            item.addSelectionListener(listener);
        }

        if (locales.size() > 1) {
            @SuppressWarnings("unused")
            MenuItem separator = new MenuItem(menu, SWT.SEPARATOR);

            ConfigurationMenuListener.addTogglePreviewModeAction(menu,
                    "Preview All Locales", chooser, RenderPreviewMode.LOCALES);
        }

        @SuppressWarnings("unused")
        MenuItem separator = new MenuItem(menu, SWT.SEPARATOR);

        MenuItem item = new MenuItem(menu, SWT.PUSH);
        item.setText("Add New Translation...");
        LocaleMenuListener listener = new LocaleMenuListener(chooser,
                ACTION_ADD_TRANSLATION, null);
        item.addSelectionListener(listener);

        Rectangle bounds = combo.getBounds();
        Point location = new Point(bounds.x, bounds.y + bounds.height);
        location = combo.getParent().toDisplay(location);
        menu.setLocation(location.x, location.y);
        menu.setVisible(true);
    }
}

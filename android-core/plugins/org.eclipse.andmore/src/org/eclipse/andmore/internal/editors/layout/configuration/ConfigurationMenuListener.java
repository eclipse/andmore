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

import static org.eclipse.andmore.internal.editors.layout.gle2.RenderPreviewMode.CUSTOM;
import static org.eclipse.andmore.internal.editors.layout.gle2.RenderPreviewMode.DEFAULT;
import static org.eclipse.andmore.internal.editors.layout.gle2.RenderPreviewMode.INCLUDES;
import static org.eclipse.andmore.internal.editors.layout.gle2.RenderPreviewMode.LOCALES;
import static org.eclipse.andmore.internal.editors.layout.gle2.RenderPreviewMode.NONE;
import static org.eclipse.andmore.internal.editors.layout.gle2.RenderPreviewMode.SCREENS;
import static org.eclipse.andmore.internal.editors.layout.gle2.RenderPreviewMode.VARIATIONS;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.resources.ResourceFolder;
import com.android.ide.common.resources.configuration.FolderConfiguration;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.AdtUtils;
import org.eclipse.andmore.internal.editors.IconFactory;
import org.eclipse.andmore.internal.editors.layout.LayoutEditorDelegate;
import org.eclipse.andmore.internal.editors.layout.gle2.IncludeFinder;
import org.eclipse.andmore.internal.editors.layout.gle2.LayoutCanvas;
import org.eclipse.andmore.internal.editors.layout.gle2.RenderPreviewManager;
import org.eclipse.andmore.internal.editors.layout.gle2.RenderPreviewMode;
import org.eclipse.andmore.internal.editors.layout.gle2.IncludeFinder.Reference;
import org.eclipse.andmore.internal.preferences.AdtPrefs;
import org.eclipse.andmore.internal.resources.manager.ResourceManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;

import java.util.List;

/**
 * The {@linkplain ConfigurationMenuListener} class is responsible for
 * generating the configuration menu in the {@link ConfigurationChooser}.
 */
class ConfigurationMenuListener extends SelectionAdapter {
    private static final String ICON_NEW_CONFIG = "newConfig";    //$NON-NLS-1$
    private static final int ACTION_SELECT_CONFIG = 1;
    private static final int ACTION_CREATE_CONFIG_FILE = 2;
    private static final int ACTION_ADD = 3;
    private static final int ACTION_DELETE_ALL = 4;
    private static final int ACTION_PREVIEW_MODE = 5;

    private final ConfigurationChooser mConfigChooser;
    private final int mAction;
    private final IFile mResource;
    private final RenderPreviewMode mMode;

    ConfigurationMenuListener(
            @NonNull ConfigurationChooser configChooser,
            int action,
            @Nullable IFile resource,
            @Nullable RenderPreviewMode mode) {
        mConfigChooser = configChooser;
        mAction = action;
        mResource = resource;
        mMode = mode;
    }

    @Override
    public void widgetSelected(SelectionEvent e) {
        switch (mAction) {
            case ACTION_SELECT_CONFIG: {
                try {
                    AdtPlugin.openFile(mResource, null, false);
                } catch (PartInitException ex) {
                    AdtPlugin.log(ex, null);
                }
                return;
            }
            case ACTION_CREATE_CONFIG_FILE: {
                ConfigurationClient client = mConfigChooser.getClient();
                if (client != null) {
                    client.createConfigFile();
                }
                return;
            }
        }

        IEditorPart activeEditor = AdtUtils.getActiveEditor();
        LayoutEditorDelegate delegate = LayoutEditorDelegate.fromEditor(activeEditor);
        IFile editedFile = mConfigChooser.getEditedFile();

        if (delegate == null || editedFile == null) {
            return;
        }
        // (Only do this when the two files are in the same project)
        IProject project = delegate.getEditor().getProject();
        if (project == null ||
                !project.equals(editedFile.getProject())) {
            return;
        }
        LayoutCanvas canvas = delegate.getGraphicalEditor().getCanvasControl();
        RenderPreviewManager previewManager = canvas.getPreviewManager();

        switch (mAction) {
            case ACTION_ADD: {
                previewManager.addAsThumbnail();
                break;
            }
            case ACTION_PREVIEW_MODE: {
                previewManager.selectMode(mMode);
                break;
            }
            case ACTION_DELETE_ALL: {
                previewManager.deleteManualPreviews();
                break;
            }
            default: assert false : mAction;
        }
        canvas.setFitScale(true /*onlyZoomOut*/, false /*allowZoomIn*/);
        canvas.redraw();
    }

    static void show(ConfigurationChooser chooser, ToolItem combo) {
        Menu menu = new Menu(chooser.getShell(), SWT.POP_UP);
        RenderPreviewMode mode = AdtPrefs.getPrefs().getRenderPreviewMode();

        // Configuration Previews
        create(menu, "Add As Thumbnail...",
                new ConfigurationMenuListener(chooser, ACTION_ADD, null, null),
                SWT.PUSH, false);
        if (mode == RenderPreviewMode.CUSTOM) {
            MenuItem item = create(menu, "Delete All Thumbnails",
                new ConfigurationMenuListener(chooser, ACTION_DELETE_ALL, null, null),
                SWT.PUSH, false);
            IEditorPart activeEditor = AdtUtils.getActiveEditor();
            LayoutEditorDelegate delegate = LayoutEditorDelegate.fromEditor(activeEditor);
            if (delegate != null) {
                LayoutCanvas canvas = delegate.getGraphicalEditor().getCanvasControl();
                RenderPreviewManager previewManager = canvas.getPreviewManager();
                if (!previewManager.hasManualPreviews()) {
                    item.setEnabled(false);
                }
            }
        }

        @SuppressWarnings("unused")
        MenuItem configSeparator = new MenuItem(menu, SWT.SEPARATOR);

        create(menu, "Preview Representative Sample",
                new ConfigurationMenuListener(chooser, ACTION_PREVIEW_MODE, null,
                        DEFAULT), SWT.RADIO, mode == DEFAULT);
        create(menu, "Preview All Screen Sizes",
                new ConfigurationMenuListener(chooser, ACTION_PREVIEW_MODE, null,
                        SCREENS), SWT.RADIO, mode == SCREENS);

        MenuItem localeItem = create(menu, "Preview All Locales",
                new ConfigurationMenuListener(chooser, ACTION_PREVIEW_MODE, null,
                        LOCALES), SWT.RADIO, mode == LOCALES);
        if (chooser.getLocaleList().size() <= 1) {
            localeItem.setEnabled(false);
        }

        boolean canPreviewIncluded = false;
        IProject project = chooser.getProject();
        if (project != null) {
            IncludeFinder finder = IncludeFinder.get(project);
            final List<Reference> includedBy = finder.getIncludedBy(chooser.getEditedFile());
            canPreviewIncluded = includedBy != null && !includedBy.isEmpty();
        }
        //if (!graphicalEditor.renderingSupports(Capability.EMBEDDED_LAYOUT)) {
        //    canPreviewIncluded = false;
        //}
        MenuItem includedItem = create(menu, "Preview Included",
                new ConfigurationMenuListener(chooser, ACTION_PREVIEW_MODE, null,
                        INCLUDES), SWT.RADIO, mode == INCLUDES);
        if (!canPreviewIncluded) {
            includedItem.setEnabled(false);
        }

        IFile file = chooser.getEditedFile();
        List<IFile> variations = AdtUtils.getResourceVariations(file, true);
        MenuItem variationsItem = create(menu, "Preview Layout Versions",
                new ConfigurationMenuListener(chooser, ACTION_PREVIEW_MODE, null,
                        VARIATIONS), SWT.RADIO, mode == VARIATIONS);
        if (variations.size() <= 1) {
            variationsItem.setEnabled(false);
        }

        create(menu, "Manual Previews",
                new ConfigurationMenuListener(chooser, ACTION_PREVIEW_MODE, null,
                        CUSTOM), SWT.RADIO, mode == CUSTOM);
        create(menu, "None",
                new ConfigurationMenuListener(chooser, ACTION_PREVIEW_MODE, null,
                        NONE), SWT.RADIO, mode == NONE);

        if (variations.size() > 1) {
            @SuppressWarnings("unused")
            MenuItem separator = new MenuItem(menu, SWT.SEPARATOR);

            ResourceManager manager = ResourceManager.getInstance();
            for (final IFile resource : variations) {
                IFolder parent = (IFolder) resource.getParent();
                ResourceFolder parentResource = manager.getResourceFolder(parent);
                FolderConfiguration configuration = parentResource.getConfiguration();
                String title = configuration.toDisplayString();

                MenuItem item = create(menu, title,
                        new ConfigurationMenuListener(chooser, ACTION_SELECT_CONFIG,
                                resource, null),
                        SWT.CHECK, false);

                if (file != null) {
                    boolean selected = file.equals(resource);
                    if (selected) {
                        item.setSelection(true);
                        item.setEnabled(false);
                    }
                }
            }
        }

        Configuration configuration = chooser.getConfiguration();
        if (configuration.getEditedConfig() != null &&
                !configuration.getEditedConfig().equals(configuration.getFullConfig())) {
            if (variations.size() > 0) {
                @SuppressWarnings("unused")
                MenuItem separator = new MenuItem(menu, SWT.SEPARATOR);
            }

            // Add action for creating a new configuration
            MenuItem item = create(menu, "Create New...",
                    new ConfigurationMenuListener(chooser, ACTION_CREATE_CONFIG_FILE,
                            null, null),
                    SWT.PUSH, false);
            item.setImage(IconFactory.getInstance().getIcon(ICON_NEW_CONFIG));
        }

        Rectangle bounds = combo.getBounds();
        Point location = new Point(bounds.x, bounds.y + bounds.height);
        location = combo.getParent().toDisplay(location);
        menu.setLocation(location.x, location.y);
        menu.setVisible(true);
    }

    @NonNull
    public static MenuItem create(@NonNull Menu menu, String title,
            ConfigurationMenuListener listener, int style, boolean selected) {
        MenuItem item = new MenuItem(menu, style);
        item.setText(title);
        item.addSelectionListener(listener);
        if (selected) {
            item.setSelection(true);
        }
        return item;
    }

    @NonNull
    static MenuItem addTogglePreviewModeAction(
            @NonNull Menu menu,
            @NonNull String title,
            @NonNull ConfigurationChooser chooser,
            @NonNull RenderPreviewMode mode) {
        boolean selected = AdtPrefs.getPrefs().getRenderPreviewMode() == mode;
        if (selected) {
            mode = RenderPreviewMode.NONE;
        }
        return create(menu, title,
                new ConfigurationMenuListener(chooser, ACTION_PREVIEW_MODE, null, mode),
                SWT.CHECK, selected);
    }
}

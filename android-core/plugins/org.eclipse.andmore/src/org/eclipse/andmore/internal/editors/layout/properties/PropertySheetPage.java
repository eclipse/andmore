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
package org.eclipse.andmore.internal.editors.layout.properties;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.editors.IconFactory;
import org.eclipse.andmore.internal.editors.layout.gle2.CanvasViewInfo;
import org.eclipse.andmore.internal.editors.layout.gle2.GraphicalEditorPart;
import org.eclipse.andmore.internal.editors.layout.properties.PropertyFactory.SortingMode;
import org.eclipse.andmore.internal.editors.layout.uimodel.UiViewElementNode;
import org.eclipse.andmore.internal.editors.uimodel.IUiUpdateListener;
import org.eclipse.andmore.internal.editors.uimodel.UiElementNode;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.Page;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.wb.internal.core.editor.structure.IPage;
import org.eclipse.wb.internal.core.model.property.Property;
import org.eclipse.wb.internal.core.model.property.table.IPropertyExceptionHandler;
import org.eclipse.wb.internal.core.model.property.table.PropertyTable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Property sheet page used when the graphical layout editor is chosen
 */
public class PropertySheetPage extends Page
        implements IPropertySheetPage, IUiUpdateListener, IPage {
    private PropertyTable mPropertyTable;
    private final GraphicalEditorPart mEditor;
    private Property mActiveProperty;
    private Action mDefaultValueAction;
    private Action mShowAdvancedPropertiesAction;
    private Action mSortAlphaAction;
    private Action mCollapseAll;
    private Action mExpandAll;
    private List<CanvasViewInfo> mSelection;

    private static final String EXPAND_DISABLED_ICON = "expandall-disabled";          //$NON-NLS-1$
    private static final String EXPAND_ICON = "expandall";                            //$NON-NLS-1$
    private static final String DEFAULT_ICON = "properties_default";                  //$NON-NLS-1$
    private static final String ADVANCED_ICON = "filter_advanced_properties";         //$NON-NLS-1$
    private static final String ALPHA_ICON = "sort_alpha";                            //$NON-NLS-1$
    // TODO: goto-definition.png

    /**
     * Constructs a new {@link PropertySheetPage} associated with the given
     * editor
     *
     * @param editor the editor associated with this property sheet page
     */
    public PropertySheetPage(GraphicalEditorPart editor) {
        mEditor = editor;
    }

    private PropertyFactory getPropertyFactory() {
        return mEditor.getPropertyFactory();
    }

    @Override
    public void createControl(Composite parent) {
        assert parent != null;
        mPropertyTable = new PropertyTable(parent, SWT.NONE);
        mPropertyTable.setExceptionHandler(new IPropertyExceptionHandler() {
            @Override
            public void handle(Throwable e) {
                AdtPlugin.log(e, null);
            }
        });
        mPropertyTable.setDefaultCollapsedNames(Arrays.asList(
                "Deprecated",
                "Layout Parameters",
                "Layout Parameters|Margins"));

        createActions();
        setPropertyTableContextMenu();
    }

    @Override
    public void selectionChanged(IWorkbenchPart part, ISelection selection) {
        if (selection instanceof TreeSelection
                && mPropertyTable != null && !mPropertyTable.isDisposed()) {
            TreeSelection treeSelection = (TreeSelection) selection;

            // We get a lot of repeated selection requests for the same selection
            // as before, so try to eliminate these
            if (mSelection != null) {
                if (mSelection.isEmpty()) {
                    if (treeSelection.isEmpty()) {
                        return;
                    }
                } else {
                    int selectionCount = treeSelection.size();
                    if (selectionCount == mSelection.size()) {
                        boolean same = true;
                        Iterator<?> iterator = treeSelection.iterator();
                        for (int i = 0, n = selectionCount; i < n && iterator.hasNext(); i++) {
                            Object next = iterator.next();
                            if (next instanceof CanvasViewInfo) {
                                CanvasViewInfo info = (CanvasViewInfo) next;
                                if (info != mSelection.get(i)) {
                                    same = false;
                                    break;
                                }
                            } else {
                                same = false;
                                break;
                            }
                        }
                        if (same) {
                            return;
                        }
                    }
                }
            }

            stopTrackingSelection();

            if (treeSelection.isEmpty()) {
                mSelection = Collections.emptyList();
            } else {
                int selectionCount = treeSelection.size();
                List<CanvasViewInfo> newSelection = new ArrayList<CanvasViewInfo>(selectionCount);
                Iterator<?> iterator = treeSelection.iterator();
                while (iterator.hasNext()) {
                    Object next = iterator.next();
                    if (next instanceof CanvasViewInfo) {
                        CanvasViewInfo info = (CanvasViewInfo) next;
                        newSelection.add(info);
                    }
                }
                mSelection = newSelection;
            }

            startTrackingSelection();

            refreshProperties();
        }
    }

    @Override
    public void dispose() {
        stopTrackingSelection();
        super.dispose();
    }

    private void startTrackingSelection() {
        if (mSelection != null && !mSelection.isEmpty()) {
            for (CanvasViewInfo item : mSelection) {
                UiViewElementNode node = item.getUiViewNode();
                if (node != null) {
                    node.addUpdateListener(this);
                }
            }
        }
    }

    private void stopTrackingSelection() {
        if (mSelection != null && !mSelection.isEmpty()) {
            for (CanvasViewInfo item : mSelection) {
                UiViewElementNode node = item.getUiViewNode();
                if (node != null) {
                    node.removeUpdateListener(this);
                }
            }
        }
        mSelection = null;
    }

    // Implements IUiUpdateListener
    @Override
    public void uiElementNodeUpdated(UiElementNode node, UiUpdateState state) {
        refreshProperties();
    }

    @Override
    public Control getControl() {
        return mPropertyTable;
    }

    @Override
    public void setFocus() {
        mPropertyTable.setFocus();
    }

    @Override
    public void makeContributions(IMenuManager menuManager,
            IToolBarManager toolBarManager, IStatusLineManager statusLineManager) {
        toolBarManager.add(mShowAdvancedPropertiesAction);
        toolBarManager.add(new Separator());
        toolBarManager.add(mSortAlphaAction);
        toolBarManager.add(new Separator());
        toolBarManager.add(mDefaultValueAction);
        toolBarManager.add(new Separator());
        toolBarManager.add(mExpandAll);
        toolBarManager.add(mCollapseAll);
        toolBarManager.add(new Separator());
    }

    private void createActions() {
        ISharedImages sharedImages = PlatformUI.getWorkbench().getSharedImages();
        IconFactory iconFactory = IconFactory.getInstance();

        mExpandAll = new PropertySheetAction(
                IAction.AS_PUSH_BUTTON,
                "Expand All",
                ACTION_EXPAND,
                iconFactory.getImageDescriptor(EXPAND_ICON),
                iconFactory.getImageDescriptor(EXPAND_DISABLED_ICON));

        mCollapseAll = new PropertySheetAction(
                IAction.AS_PUSH_BUTTON,
                "Collapse All",
                ACTION_COLLAPSE,
                sharedImages.getImageDescriptor(ISharedImages.IMG_ELCL_COLLAPSEALL),
                sharedImages.getImageDescriptor(ISharedImages.IMG_ELCL_COLLAPSEALL_DISABLED));

        mShowAdvancedPropertiesAction = new PropertySheetAction(
                IAction.AS_CHECK_BOX,
                "Show Advanced Properties",
                ACTION_SHOW_ADVANCED,
                iconFactory.getImageDescriptor(ADVANCED_ICON),
                null);

        mSortAlphaAction = new PropertySheetAction(
                IAction.AS_CHECK_BOX,
                "Sort Alphabetically",
                ACTION_SORT_ALPHA,
                iconFactory.getImageDescriptor(ALPHA_ICON),
                null);

        mDefaultValueAction = new PropertySheetAction(
                IAction.AS_PUSH_BUTTON,
                "Restore Default Value",
                ACTION_DEFAULT_VALUE,
                iconFactory.getImageDescriptor(DEFAULT_ICON),
                null);

        // Listen on the selection in the property sheet so we can update the
        // Restore Default Value action
        ISelectionChangedListener listener = new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                StructuredSelection selection = (StructuredSelection) event.getSelection();
                mActiveProperty = (Property) selection.getFirstElement();
                updateDefaultValueAction();
            }
        };
        mPropertyTable.addSelectionChangedListener(listener);
    }

    /**
     * Updates the state of {@link #mDefaultValueAction}.
     */
    private void updateDefaultValueAction() {
        if (mActiveProperty != null) {
            try {
                mDefaultValueAction.setEnabled(mActiveProperty.isModified());
            } catch (Exception e) {
                AdtPlugin.log(e, null);
            }
        } else {
            mDefaultValueAction.setEnabled(false);
        }
    }

    /**
     * Sets the context menu for {@link #mPropertyTable}.
     */
    private void setPropertyTableContextMenu() {
        final MenuManager manager = new MenuManager();
        manager.setRemoveAllWhenShown(true);
        manager.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(IMenuManager m) {
                // dispose items to avoid caching
                for (MenuItem item : manager.getMenu().getItems()) {
                    item.dispose();
                }
                // apply new items
                fillContextMenu();
            }

            private void fillContextMenu() {
                manager.add(mDefaultValueAction);
                manager.add(mSortAlphaAction);
                manager.add(mShowAdvancedPropertiesAction);
            }
        });

        mPropertyTable.setMenu(manager.createContextMenu(mPropertyTable));
    }

    /**
     * Shows {@link Property}'s of current objects.
     */
    private void refreshProperties() {
        PropertyFactory factory = getPropertyFactory();
        mPropertyTable.setInput(factory.getProperties(mSelection));
        updateDefaultValueAction();
    }

    // ---- Actions ----

    private static final int ACTION_DEFAULT_VALUE = 1;
    private static final int ACTION_SHOW_ADVANCED = 2;
    private static final int ACTION_COLLAPSE = 3;
    private static final int ACTION_EXPAND = 4;
    private static final int ACTION_SORT_ALPHA = 5;

    private class PropertySheetAction extends Action {
        private final int mAction;

        private PropertySheetAction(int style, String label, int action,
                ImageDescriptor imageDesc, ImageDescriptor disabledImageDesc) {
            super(label, style);
            mAction = action;
            setImageDescriptor(imageDesc);
            if (disabledImageDesc != null) {
                setDisabledImageDescriptor(disabledImageDesc);
            }
            setToolTipText(label);
        }

        @Override
        public void run() {
            switch (mAction) {
                case ACTION_COLLAPSE: {
                    mPropertyTable.collapseAll();
                    break;
                }
                case ACTION_EXPAND: {
                    mPropertyTable.expandAll();
                    break;
                }
                case ACTION_SHOW_ADVANCED: {
                    boolean show = mShowAdvancedPropertiesAction.isChecked();
                    mPropertyTable.setShowAdvancedProperties(show);
                    break;
                }
                case ACTION_SORT_ALPHA: {
                    boolean isAlphabetical = mSortAlphaAction.isChecked();
                    getPropertyFactory().setSortingMode(
                        isAlphabetical ? SortingMode.ALPHABETICAL : PropertyFactory.DEFAULT_MODE);
                    refreshProperties();
                    break;
                }
                case ACTION_DEFAULT_VALUE:
                    try {
                        mActiveProperty.setValue(Property.UNKNOWN_VALUE);
                    } catch (Exception e) {
                        // Ignore warnings from setters
                    }
                    break;
                default:
                    assert false : mAction;
            }
        }
    }

    @Override
    public void setToolBar(IToolBarManager toolBarManager) {
        makeContributions(null, toolBarManager, null);
        toolBarManager.update(false);
    }
}

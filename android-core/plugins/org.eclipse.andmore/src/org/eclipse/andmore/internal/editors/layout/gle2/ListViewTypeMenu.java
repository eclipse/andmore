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
package org.eclipse.andmore.internal.editors.layout.gle2;

import static com.android.SdkConstants.ANDROID_LAYOUT_RESOURCE_PREFIX;
import static org.eclipse.andmore.internal.editors.layout.gle2.LayoutMetadata.KEY_LV_FOOTER;
import static org.eclipse.andmore.internal.editors.layout.gle2.LayoutMetadata.KEY_LV_HEADER;
import static org.eclipse.andmore.internal.editors.layout.gle2.LayoutMetadata.KEY_LV_ITEM;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.rendering.api.Capability;
import com.android.resources.ResourceType;

import org.eclipse.andmore.internal.editors.layout.LayoutEditorDelegate;
import org.eclipse.andmore.internal.editors.layout.uimodel.UiViewElementNode;
import org.eclipse.andmore.internal.resources.CyclicDependencyValidator;
import org.eclipse.andmore.internal.ui.ResourceChooser;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Menu;
import org.w3c.dom.Node;

/**
 * "Preview List Content" context menu which lists available data types and layouts
 * the user can choose to view the ListView as.
 */
public class ListViewTypeMenu extends SubmenuAction {
    /** Associated canvas */
    private final LayoutCanvas mCanvas;
    /** When true, this menu is for a grid rather than a simple list */
    private boolean mGrid;
    /** When true, this menu is for a spinner rather than a simple list */
    private boolean mSpinner;

    /**
     * Creates a "Preview List Content" menu
     *
     * @param canvas associated canvas
     * @param isGrid whether the menu is for a grid rather than a list
     * @param isSpinner whether the menu is for a spinner rather than a list
     */
    public ListViewTypeMenu(LayoutCanvas canvas, boolean isGrid, boolean isSpinner) {
        super(isGrid ? "Preview Grid Content" : isSpinner ? "Preview Spinner Layout"
                : "Preview List Content");
        mCanvas = canvas;
        mGrid = isGrid;
        mSpinner = isSpinner;
    }

    @Override
    protected void addMenuItems(Menu menu) {
        GraphicalEditorPart graphicalEditor = mCanvas.getEditorDelegate().getGraphicalEditor();
        if (graphicalEditor.renderingSupports(Capability.ADAPTER_BINDING)) {
            IAction action = new PickLayoutAction("Choose Layout...", KEY_LV_ITEM);
            new ActionContributionItem(action).fill(menu, -1);
            new Separator().fill(menu, -1);

            String selected = getSelectedLayout();
            if (selected != null) {
                if (selected.startsWith(ANDROID_LAYOUT_RESOURCE_PREFIX)) {
                    selected = selected.substring(ANDROID_LAYOUT_RESOURCE_PREFIX.length());
                }
            }

            if (mSpinner) {
                action = new SetListTypeAction("Spinner Item",
                        "simple_spinner_item", selected); //$NON-NLS-1$
                new ActionContributionItem(action).fill(menu, -1);
                action = new SetListTypeAction("Spinner Dropdown Item",
                        "simple_spinner_dropdown_item", selected); //$NON-NLS-1$
                new ActionContributionItem(action).fill(menu, -1);
                return;
            }

            action = new SetListTypeAction("Simple List Item",
                    "simple_list_item_1", selected); //$NON-NLS-1$
            new ActionContributionItem(action).fill(menu, -1);
            action = new SetListTypeAction("Simple 2-Line List Item",
                    "simple_list_item_2", //$NON-NLS-1$
                    selected);
            new ActionContributionItem(action).fill(menu, -1);
            action = new SetListTypeAction("Checked List Item",
                    "simple_list_item_checked", //$NON-NLS-1$
                    selected);
            new ActionContributionItem(action).fill(menu, -1);
            action = new SetListTypeAction("Single Choice List Item",
                    "simple_list_item_single_choice", //$NON-NLS-1$
                    selected);
            new ActionContributionItem(action).fill(menu, -1);
            action = new SetListTypeAction("Multiple Choice List Item",
                    "simple_list_item_multiple_choice", //$NON-NLS-1$
                    selected);
            if (!mGrid) {
                new Separator().fill(menu, -1);
                action = new SetListTypeAction("Simple Expandable List Item",
                        "simple_expandable_list_item_1", selected); //$NON-NLS-1$
                new ActionContributionItem(action).fill(menu, -1);
                action = new SetListTypeAction("Simple 2-Line Expandable List Item",
                        "simple_expandable_list_item_2", //$NON-NLS-1$
                        selected);
                new ActionContributionItem(action).fill(menu, -1);

                new Separator().fill(menu, -1);
                action = new PickLayoutAction("Choose Header...", KEY_LV_HEADER);
                new ActionContributionItem(action).fill(menu, -1);
                action = new PickLayoutAction("Choose Footer...", KEY_LV_FOOTER);
                new ActionContributionItem(action).fill(menu, -1);
            }
        } else {
            // Should we just hide the menu item instead?
            addDisabledMessageItem(
                    "Not supported for this SDK version; try changing the Render Target");
        }
    }

    private class SetListTypeAction extends Action {
        private final String mLayout;

        public SetListTypeAction(String title, String layout, String selected) {
            super(title, IAction.AS_RADIO_BUTTON);
            mLayout = layout;

            if (layout.equals(selected)) {
                setChecked(true);
            }
        }

        @Override
        public void run() {
            if (isChecked()) {
                setNewType(KEY_LV_ITEM, ANDROID_LAYOUT_RESOURCE_PREFIX + mLayout);
            }
        }
    }

    /**
     * Action which brings up a resource chooser to choose an arbitrary layout as the
     * layout to be previewed in the list.
     */
    private class PickLayoutAction extends Action {
        private final String mType;

        public PickLayoutAction(String title, String type) {
            super(title, IAction.AS_PUSH_BUTTON);
            mType = type;
        }

        @Override
        public void run() {
            LayoutEditorDelegate delegate = mCanvas.getEditorDelegate();
            IFile file = delegate.getEditor().getInputFile();
            GraphicalEditorPart editor = delegate.getGraphicalEditor();
            ResourceChooser dlg = ResourceChooser.create(editor, ResourceType.LAYOUT)
                .setInputValidator(CyclicDependencyValidator.create(file))
                .setInitialSize(85, 10)
                .setCurrentResource(getSelectedLayout());
            int result = dlg.open();
            if (result == ResourceChooser.CLEAR_RETURN_CODE) {
                setNewType(mType, null);
            } else if (result == Window.OK) {
                String newType = dlg.getCurrentResource();
                setNewType(mType, newType);
            }
        }
    }

    @Nullable
    private String getSelectedLayout() {
        String layout = null;
        SelectionManager selectionManager = mCanvas.getSelectionManager();
        for (SelectionItem item : selectionManager.getSelections()) {
            UiViewElementNode node = item.getViewInfo().getUiViewNode();
            if (node != null) {
                Node xmlNode = node.getXmlNode();
                layout = LayoutMetadata.getProperty(xmlNode, KEY_LV_ITEM);
                if (layout != null) {
                    return layout;
                }
            }
        }

        return null;
    }

    private void setNewType(@NonNull String type, @Nullable String layout) {
        LayoutEditorDelegate delegate = mCanvas.getEditorDelegate();
        GraphicalEditorPart graphicalEditor = delegate.getGraphicalEditor();
        SelectionManager selectionManager = mCanvas.getSelectionManager();

        for (SelectionItem item : selectionManager.getSnapshot()) {
            UiViewElementNode node = item.getViewInfo().getUiViewNode();
            if (node != null) {
                Node xmlNode = node.getXmlNode();
                LayoutMetadata.setProperty(delegate.getEditor(), xmlNode, type, layout);
            }
        }

        // Refresh
        graphicalEditor.recomputeLayout();
        mCanvas.redraw();
    }
}

/*
 * Copyright (C) 2010 The Android Open Source Project
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

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import java.util.ArrayList;

/** Drag listener for the outline page */
/* package */ class OutlineDragListener implements DragSourceListener {
    private TreeViewer mTreeViewer;
    private OutlinePage mOutlinePage;
    private final ArrayList<SelectionItem> mDragSelection = new ArrayList<SelectionItem>();
    private SimpleElement[] mDragElements;

    public OutlineDragListener(OutlinePage outlinePage, TreeViewer treeViewer) {
        super();
        mOutlinePage = outlinePage;
        mTreeViewer = treeViewer;
    }

    @Override
    public void dragStart(DragSourceEvent e) {
        Tree tree = mTreeViewer.getTree();

        TreeItem overTreeItem = tree.getItem(new Point(e.x, e.y));
        if (overTreeItem == null) {
            // Not dragging over a tree item
            e.doit = false;
            return;
        }
        CanvasViewInfo over = getViewInfo(overTreeItem);
        if (over == null) {
            e.doit = false;
            return;
        }

        // The selection logic for the outline is much simpler than in the canvas,
        // because for one thing, the tree selection is updated synchronously on mouse
        // down, so it's not possible to start dragging a non-selected item.
        // We also don't deliberately disallow root-element dragging since you can
        // drag it into another form.
        final LayoutCanvas canvas = mOutlinePage.getEditor().getCanvasControl();
        SelectionManager selectionManager = canvas.getSelectionManager();
        TreeItem[] treeSelection = tree.getSelection();
        mDragSelection.clear();
        for (TreeItem item : treeSelection) {
            CanvasViewInfo viewInfo = getViewInfo(item);
            if (viewInfo != null) {
                mDragSelection.add(selectionManager.createSelection(viewInfo));
            }
        }
        SelectionManager.sanitize(mDragSelection);

        e.doit = !mDragSelection.isEmpty();
        int imageCount = mDragSelection.size();
        if (e.doit) {
            mDragElements = SelectionItem.getAsElements(mDragSelection);
            GlobalCanvasDragInfo.getInstance().startDrag(mDragElements,
                    mDragSelection.toArray(new SelectionItem[imageCount]),
                    canvas, new Runnable() {
                        @Override
                        public void run() {
                            canvas.getClipboardSupport().deleteSelection("Remove",
                                    mDragSelection);
                        }
                    });
            return;
        }

        e.detail = DND.DROP_NONE;
    }

    @Override
    public void dragSetData(DragSourceEvent e) {
        if (TextTransfer.getInstance().isSupportedType(e.dataType)) {
            LayoutCanvas canvas = mOutlinePage.getEditor().getCanvasControl();
            e.data = SelectionItem.getAsText(canvas, mDragSelection);
            return;
        }

        if (SimpleXmlTransfer.getInstance().isSupportedType(e.dataType)) {
            e.data = mDragElements;
            return;
        }

        // otherwise we failed
        e.detail = DND.DROP_NONE;
        e.doit = false;
    }

    @Override
    public void dragFinished(DragSourceEvent e) {
        // Unregister the dragged data.
        // Clear the selection
        mDragSelection.clear();
        mDragElements = null;
        GlobalCanvasDragInfo.getInstance().stopDrag();
    }

    private CanvasViewInfo getViewInfo(TreeItem item) {
        Object data = item.getData();
        if (data != null) {
            return OutlinePage.getViewInfo(data);
        }

        return null;
    }
}
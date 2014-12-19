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

import com.android.ide.common.api.INode;
import com.android.ide.common.api.InsertType;

import org.eclipse.andmore.common.layout.BaseLayoutRule;
import org.eclipse.andmore.internal.editors.descriptors.DescriptorsUtils;
import org.eclipse.andmore.internal.editors.layout.gre.NodeProxy;
import org.eclipse.andmore.internal.editors.layout.uimodel.UiViewElementNode;
import org.eclipse.andmore.internal.editors.uimodel.UiElementNode;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TransferData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Drop listener for the outline page */
/*package*/ class OutlineDropListener extends ViewerDropAdapter {
    private final OutlinePage mOutlinePage;

    public OutlineDropListener(OutlinePage outlinePage, TreeViewer treeViewer) {
        super(treeViewer);
        mOutlinePage = outlinePage;
    }

    @Override
    public void dragEnter(DropTargetEvent event) {
        if (event.detail == DND.DROP_NONE && GlobalCanvasDragInfo.getInstance().isDragging()) {
            // For some inexplicable reason, we get DND.DROP_NONE from the palette
            // even though in its drag start we set DND.DROP_COPY, so correct that here...
            int operation = DND.DROP_COPY;
            event.detail = operation;
        }
        super.dragEnter(event);
    }

    @Override
    public boolean performDrop(Object data) {
        final DropTargetEvent event = getCurrentEvent();
        if (event == null) {
            return false;
        }
        int location = determineLocation(event);
        if (location == LOCATION_NONE) {
            return false;
        }

        final SimpleElement[] elements;
        SimpleXmlTransfer sxt = SimpleXmlTransfer.getInstance();
        if (sxt.isSupportedType(event.currentDataType)) {
            if (data instanceof SimpleElement[]) {
                elements = (SimpleElement[]) data;
            } else {
                return false;
            }
        } else {
            return false;
        }
        if (elements.length == 0) {
            return false;
        }

        // Determine target:
        CanvasViewInfo parent = OutlinePage.getViewInfo(event.item.getData());
        if (parent == null) {
            return false;
        }

        int index = -1;
        UiViewElementNode parentNode = parent.getUiViewNode();
        if (location == LOCATION_BEFORE || location == LOCATION_AFTER) {
            UiViewElementNode node = parentNode;
            parent = parent.getParent();
            if (parent == null) {
                return false;
            }
            parentNode = parent.getUiViewNode();

            // Determine index
            index = 0;
            for (UiElementNode child : parentNode.getUiChildren()) {
                if (child == node) {
                    break;
                }
                index++;
            }
            if (location == LOCATION_AFTER) {
                index++;
            }
        }

        // Copy into new position.
        final LayoutCanvas canvas = mOutlinePage.getEditor().getCanvasControl();
        final NodeProxy targetNode = canvas.getNodeFactory().create(parentNode);

        // Record children of the target right before the drop (such that we can
        // find out after the drop which exact children were inserted)
        Set<INode> children = new HashSet<INode>();
        for (INode node : targetNode.getChildren()) {
            children.add(node);
        }

        String label = MoveGesture.computeUndoLabel(targetNode, elements, event.detail);
        final int indexFinal = index;
        canvas.getEditorDelegate().getEditor().wrapUndoEditXmlModel(label, new Runnable() {
            @Override
            public void run() {
                InsertType insertType = MoveGesture.getInsertType(event, targetNode);
                canvas.getRulesEngine().setInsertType(insertType);

                Object sourceCanvas = GlobalCanvasDragInfo.getInstance().getSourceCanvas();
                boolean createNew = event.detail == DND.DROP_COPY || sourceCanvas != canvas;
                BaseLayoutRule.insertAt(targetNode, elements, createNew, indexFinal);
                targetNode.applyPendingChanges();

                // Clean up drag if applicable
                if (event.detail == DND.DROP_MOVE) {
                    GlobalCanvasDragInfo.getInstance().removeSource();
                }
            }
        });

        // Now find out which nodes were added, and look up their corresponding
        // CanvasViewInfos
        final List<INode> added = new ArrayList<INode>();
        for (INode node : targetNode.getChildren()) {
            if (!children.contains(node)) {
                added.add(node);
            }
        }
        // Select the newly dropped nodes
        final SelectionManager selectionManager = canvas.getSelectionManager();
        selectionManager.setOutlineSelection(added);

        canvas.redraw();

        return true;
    }

    @Override
    public boolean validateDrop(Object target, int operation,
            TransferData transferType) {
        DropTargetEvent event = getCurrentEvent();
        if (event == null) {
            return false;
        }
        int location = determineLocation(event);
        if (location == LOCATION_NONE) {
            return false;
        }

        SimpleXmlTransfer sxt = SimpleXmlTransfer.getInstance();
        if (!sxt.isSupportedType(transferType)) {
            return false;
        }

        CanvasViewInfo parent = OutlinePage.getViewInfo(event.item.getData());
        if (parent == null) {
            return false;
        }

        UiViewElementNode parentNode = parent.getUiViewNode();

        if (location == LOCATION_ON) {
            // Targeting the middle of an item means to add it as a new child
            // of the given element. This is only allowed on some types of nodes.
            if (!DescriptorsUtils.canInsertChildren(parentNode.getDescriptor(),
                    parent.getViewObject())) {
                return false;
            }
        }

        // Check that the drop target position is not a child or identical to
        // one of the dragged items
        SelectionItem[] sel = GlobalCanvasDragInfo.getInstance().getCurrentSelection();
        if (sel != null) {
            for (SelectionItem item : sel) {
                if (isAncestor(item.getViewInfo().getUiViewNode(), parentNode)) {
                    return false;
                }
            }
        }

        return true;
    }

    /** Returns true if the given parent node is an ancestor of the given child node  */
    private boolean isAncestor(UiElementNode parent, UiElementNode child) {
        while (child != null) {
            if (child == parent) {
                return true;
            }
            child = child.getUiParent();
        }
        return false;
    }
}

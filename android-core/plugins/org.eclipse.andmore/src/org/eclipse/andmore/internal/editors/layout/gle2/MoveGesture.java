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

import com.android.ide.common.api.DropFeedback;
import com.android.ide.common.api.INode;
import com.android.ide.common.api.InsertType;
import com.android.ide.common.api.Point;
import com.android.ide.common.api.Rect;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.editors.layout.gre.NodeFactory;
import org.eclipse.andmore.internal.editors.layout.gre.NodeProxy;
import org.eclipse.andmore.internal.editors.layout.gre.RulesEngine;
import org.eclipse.andmore.internal.editors.layout.uimodel.UiViewElementNode;
import org.eclipse.andmore.internal.editors.uimodel.UiElementNode;
import org.eclipse.andmore.internal.editors.uimodel.UiElementNode.NodeCreationListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Display;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The Move gesture provides the operation for moving widgets around in the canvas.
 */
public class MoveGesture extends DropGesture {
    /** The associated {@link LayoutCanvas}. */
    private LayoutCanvas mCanvas;

    /** Overlay which paints the drag &amp; drop feedback. */
    private MoveOverlay mOverlay;

    private static final boolean DEBUG = false;

    /**
     * The top view right under the drag'n'drop cursor.
     * This can only be null during a drag'n'drop when there is no view under the cursor
     * or after the state was all cleared.
     */
    private CanvasViewInfo mCurrentView;

    /**
     * The elements currently being dragged. This will always be non-null for a valid
     * drag'n'drop that happens within the same instance of Eclipse.
     * <p/>
     * In the event that the drag and drop happens between different instances of Eclipse
     * this will remain null.
     */
    private SimpleElement[] mCurrentDragElements;

    /**
     * The first view under the cursor that responded to onDropEnter is called the "target view".
     * It can differ from mCurrentView, typically because a terminal View doesn't
     * accept drag'n'drop so its parent layout became the target drag'n'drop receiver.
     * <p/>
     * The target node is the proxy node associated with the target view.
     * This can be null if no view under the cursor accepted the drag'n'drop or if the node
     * factory couldn't create a proxy for it.
     */
    private NodeProxy mTargetNode;

    /**
     * The latest drop feedback returned by IViewRule.onDropEnter/Move.
     */
    private DropFeedback mFeedback;

    /**
     * {@link #dragLeave(DropTargetEvent)} is unfortunately called right before data is
     * about to be dropped (between the last {@link #dragOver(DropTargetEvent)} and the
     * next {@link #dropAccept(DropTargetEvent)}). That means we can't just
     * trash the current DropFeedback from the current view rule in dragLeave().
     * Instead we preserve it in mLeaveTargetNode and mLeaveFeedback in case a dropAccept
     * happens next.
     */
    private NodeProxy mLeaveTargetNode;

    /**
     * @see #mLeaveTargetNode
     */
    private DropFeedback mLeaveFeedback;

    /**
     * @see #mLeaveTargetNode
     */
    private CanvasViewInfo mLeaveView;

    /** Singleton used to keep track of drag selection in the same Eclipse instance. */
    private final GlobalCanvasDragInfo mGlobalDragInfo;

    /**
     * Constructs a new {@link MoveGesture}, tied to the given canvas.
     *
     * @param canvas The canvas to associate the {@link MoveGesture} with.
     */
    public MoveGesture(LayoutCanvas canvas) {
        mCanvas = canvas;
        mGlobalDragInfo = GlobalCanvasDragInfo.getInstance();
    }

    @Override
    public List<Overlay> createOverlays() {
        mOverlay = new MoveOverlay();
        return Collections.<Overlay> singletonList(mOverlay);
    }

    @Override
    public void begin(ControlPoint pos, int startMask) {
        super.begin(pos, startMask);

        // Hide selection overlays during a move drag
        mCanvas.getSelectionOverlay().setHidden(true);
    }

    @Override
    public void end(ControlPoint pos, boolean canceled) {
        super.end(pos, canceled);

        mCanvas.getSelectionOverlay().setHidden(false);

        // Ensure that the outline is back to showing the current selection, since during
        // a drag gesture we temporarily set it to show the current target node instead.
        mCanvas.getSelectionManager().syncOutlineSelection();
    }

    /* TODO: Pass modifier mask to drag rules as well! This doesn't work yet since
       the drag &amp; drop code seems to steal keyboard events.
    @Override
    public boolean keyPressed(KeyEvent event) {
        update(mCanvas.getGestureManager().getCurrentControlPoint());
        mCanvas.redraw();
        return true;
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        update(mCanvas.getGestureManager().getCurrentControlPoint());
        mCanvas.redraw();
        return true;
    }
    */

    /*
     * The cursor has entered the drop target boundaries.
     * {@inheritDoc}
     */
    @Override
    public void dragEnter(DropTargetEvent event) {
        if (DEBUG) AdtPlugin.printErrorToConsole("DEBUG", "drag enter", event);

        // Make sure we don't have any residual data from an earlier operation.
        clearDropInfo();
        mLeaveTargetNode = null;
        mLeaveFeedback = null;
        mLeaveView = null;

        // Get the dragged elements.
        //
        // The current transfered type can be extracted from the event.
        // As described in dragOver(), this works basically works on Windows but
        // not on Linux or Mac, in which case we can't get the type until we
        // receive dropAccept/drop().
        // For consistency we try to use the GlobalCanvasDragInfo instance first,
        // and if it fails we use the event transfer type as a backup (but as said
        // before it will most likely work only on Windows.)
        // In any case this can be null even for a valid transfer.

        mCurrentDragElements = mGlobalDragInfo.getCurrentElements();

        if (mCurrentDragElements == null) {
            SimpleXmlTransfer sxt = SimpleXmlTransfer.getInstance();
            if (sxt.isSupportedType(event.currentDataType)) {
                mCurrentDragElements = (SimpleElement[]) sxt.nativeToJava(event.currentDataType);
            }
        }

        // if there is no data to transfer, invalidate the drag'n'drop.
        // The assumption is that the transfer should have at least one element with a
        // a non-null non-empty FQCN. Everything else is optional.
        if (mCurrentDragElements == null ||
                mCurrentDragElements.length == 0 ||
                mCurrentDragElements[0] == null ||
                mCurrentDragElements[0].getFqcn() == null ||
                mCurrentDragElements[0].getFqcn().length() == 0) {
            event.detail = DND.DROP_NONE;
        }

        dragOperationChanged(event);
    }

    /*
     * The operation being performed has changed (e.g. modifier key).
     * {@inheritDoc}
     */
    @Override
    public void dragOperationChanged(DropTargetEvent event) {
        if (DEBUG) AdtPlugin.printErrorToConsole("DEBUG", "drag changed", event);

        checkDataType(event);
        recomputeDragType(event);
    }

    private void recomputeDragType(DropTargetEvent event) {
        if (event.detail == DND.DROP_DEFAULT) {
            // Default means we can now choose the default operation, either copy or move.
            // If the drag comes from the same canvas we default to move, otherwise we
            // default to copy.

            if (mGlobalDragInfo.getSourceCanvas() == mCanvas &&
                    (event.operations & DND.DROP_MOVE) != 0) {
                event.detail = DND.DROP_MOVE;
            } else if ((event.operations & DND.DROP_COPY) != 0) {
                event.detail = DND.DROP_COPY;
            }
        }

        // We don't support other types than copy and move
        if (event.detail != DND.DROP_COPY && event.detail != DND.DROP_MOVE) {
            event.detail = DND.DROP_NONE;
        }
    }

    /*
     * The cursor has left the drop target boundaries OR data is about to be dropped.
     * {@inheritDoc}
     */
    @Override
    public void dragLeave(DropTargetEvent event) {
        if (DEBUG) AdtPlugin.printErrorToConsole("DEBUG", "drag leave");

        // dragLeave is unfortunately called right before data is about to be dropped
        // (between the last dropMove and the next dropAccept). That means we can't just
        // trash the current DropFeedback from the current view rule, we need to preserve
        // it in case a dropAccept happens next.
        // See the corresponding kludge in dropAccept().
        mLeaveTargetNode = mTargetNode;
        mLeaveFeedback = mFeedback;
        mLeaveView = mCurrentView;

        clearDropInfo();
    }

    /*
     * The cursor is moving over the drop target.
     * {@inheritDoc}
     */
    @Override
    public void dragOver(DropTargetEvent event) {
        processDropEvent(event);
    }

    /*
     * The drop is about to be performed.
     * The drop target is given a last chance to change the nature of the drop.
     * {@inheritDoc}
     */
    @Override
    public void dropAccept(DropTargetEvent event) {
        if (DEBUG) AdtPlugin.printErrorToConsole("DEBUG", "drop accept");

        checkDataType(event);

        // If we have a valid target node and it matches the one we saved in
        // dragLeave then we restore the DropFeedback that we saved in dragLeave.
        if (mLeaveTargetNode != null) {
            mTargetNode = mLeaveTargetNode;
            mFeedback = mLeaveFeedback;
            mCurrentView = mLeaveView;
        }

        if (mFeedback != null && mFeedback.invalidTarget) {
            // The script said we can't drop here.
            event.detail = DND.DROP_NONE;
        }

        if (mLeaveTargetNode == null || event.detail == DND.DROP_NONE) {
            clearDropInfo();
        }

        mLeaveTargetNode = null;
        mLeaveFeedback = null;
        mLeaveView = null;
    }

    /*
     * The data is being dropped.
     * {@inheritDoc}
     */
    @Override
    public void drop(final DropTargetEvent event) {
        if (DEBUG) AdtPlugin.printErrorToConsole("DEBUG", "dropped");

        SimpleElement[] elements = null;

        SimpleXmlTransfer sxt = SimpleXmlTransfer.getInstance();

        if (sxt.isSupportedType(event.currentDataType)) {
            if (event.data instanceof SimpleElement[]) {
                elements = (SimpleElement[]) event.data;
            }
        }

        if (elements == null || elements.length < 1) {
            if (DEBUG) AdtPlugin.printErrorToConsole("DEBUG", "drop missing drop data");
            return;
        }

        if (mCurrentDragElements != null && Arrays.equals(elements, mCurrentDragElements)) {
            elements = mCurrentDragElements;
        }

        if (mTargetNode == null) {
            ViewHierarchy viewHierarchy = mCanvas.getViewHierarchy();
            if (viewHierarchy.isValid() && viewHierarchy.isEmpty()) {
                // There is no target node because the drop happens on an empty document.
                // Attempt to create a root node accordingly.
                createDocumentRoot(elements);
            } else {
                if (DEBUG) AdtPlugin.printErrorToConsole("DEBUG", "dropped on null targetNode");
            }
            return;
        }

        updateDropFeedback(mFeedback, event);

        final SimpleElement[] elementsFinal = elements;
        final LayoutPoint canvasPoint = getDropLocation(event).toLayout();
        String label = computeUndoLabel(mTargetNode, elements, event.detail);

        // Create node listener which (during the drop) listens for node additions
        // and stores the list of added node such that they can be selected afterwards.
        final List<UiElementNode> added = new ArrayList<UiElementNode>();
        // List of "index within parent" for each node
        final List<Integer> indices = new ArrayList<Integer>();
        NodeCreationListener listener = new NodeCreationListener() {
            @Override
            public void nodeCreated(UiElementNode parent, UiElementNode child, int index) {
                if (parent == mTargetNode.getNode()) {
                    added.add(child);

                    // Adjust existing indices
                    for (int i = 0, n = indices.size(); i < n; i++) {
                        int idx = indices.get(i);
                        if (idx >= index) {
                            indices.set(i, idx + 1);
                        }
                    }

                    indices.add(index);
                }
            }

            @Override
            public void nodeDeleted(UiElementNode parent, UiElementNode child, int previousIndex) {
                if (parent == mTargetNode.getNode()) {
                    // Adjust existing indices
                    for (int i = 0, n = indices.size(); i < n; i++) {
                        int idx = indices.get(i);
                        if (idx >= previousIndex) {
                            indices.set(i, idx - 1);
                        }
                    }

                    // Make sure we aren't removing the same nodes that are being added
                    // No, that can happen when canceling out of a drop handler such as
                    // when dropping an included layout, then canceling out of the
                    // resource chooser.
                    //assert !added.contains(child);
                }
            }
        };

        try {
            UiElementNode.addNodeCreationListener(listener);
            mCanvas.getEditorDelegate().getEditor().wrapUndoEditXmlModel(label, new Runnable() {
                @Override
                public void run() {
                    InsertType insertType = getInsertType(event, mTargetNode);
                    mCanvas.getRulesEngine().callOnDropped(mTargetNode,
                            elementsFinal,
                            mFeedback,
                            new Point(canvasPoint.x, canvasPoint.y),
                            insertType);
                    mTargetNode.applyPendingChanges();
                    // Clean up drag if applicable
                    if (event.detail == DND.DROP_MOVE) {
                        GlobalCanvasDragInfo.getInstance().removeSource();
                    }
                    mTargetNode.applyPendingChanges();
                }
            });
        } finally {
            UiElementNode.removeNodeCreationListener(listener);
        }

        final List<INode> nodes = new ArrayList<INode>();
        NodeFactory nodeFactory = mCanvas.getNodeFactory();
        for (UiElementNode uiNode : added) {
            if (uiNode instanceof UiViewElementNode) {
                NodeProxy node = nodeFactory.create((UiViewElementNode) uiNode);
                if (node != null) {
                    nodes.add(node);
                }
            }
        }

        // Select the newly dropped nodes:
        // Find out which nodes were added, and look up their corresponding
        // CanvasViewInfos.
        final SelectionManager selectionManager = mCanvas.getSelectionManager();
        // Don't use the indices to search for corresponding nodes yet, since a
        // render may not have happened yet and we'd rather use an up to date
        // view hierarchy than indices to look up the right view infos.
        if (!selectionManager.selectDropped(nodes, null /* indices */)) {
            // In some scenarios we can't find the actual view infos yet; this
            // seems to happen when you drag from one canvas to another (see the
            // related comment next to the setFocus() call below). In that case
            // defer selection briefly until the view hierarchy etc is up to
            // date.
            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    selectionManager.selectDropped(nodes, indices);
                }
            });
        }

        clearDropInfo();
        mCanvas.redraw();
        // Request focus: This is *necessary* when you are dragging from one canvas editor
        // to another, because without it, the redraw does not seem to be processed (the change
        // is invisible until you click on the target canvas to give it focus).
        mCanvas.setFocus();
    }

    /**
     * Returns the right {@link InsertType} to use for the given drop target event and the
     * given target node
     *
     * @param event the drop target event
     * @param mTargetNode the node targeted by the drop
     * @return the {link InsertType} to use for the drop
     */
    public static InsertType getInsertType(DropTargetEvent event, NodeProxy mTargetNode) {
        GlobalCanvasDragInfo dragInfo = GlobalCanvasDragInfo.getInstance();
        if (event.detail == DND.DROP_MOVE) {
            SelectionItem[] selection = dragInfo.getCurrentSelection();
            if (selection != null) {
                for (SelectionItem item : selection) {
                    if (item.getNode() != null
                            && item.getNode().getParent() == mTargetNode) {
                        return InsertType.MOVE_WITHIN;
                    }
                }
            }

            return InsertType.MOVE_INTO;
        } else if (dragInfo.getSourceCanvas() != null) {
            return InsertType.PASTE;
        } else {
            return InsertType.CREATE;
        }
    }

    /**
     * Computes a suitable Undo label to use for a drop operation, such as
     * "Drop Button in LinearLayout" and "Move Widgets in RelativeLayout".
     *
     * @param targetNode The target of the drop
     * @param elements The dragged widgets
     * @param detail The DnD mode, as used in {@link DropTargetEvent#detail}.
     * @return A string suitable as an undo-label for the drop event
     */
    public static String computeUndoLabel(NodeProxy targetNode,
            SimpleElement[] elements, int detail) {
        // Decide whether it's a move or a copy; we'll label moves specifically
        // as a move and consider everything else a "Drop"
        String verb = (detail == DND.DROP_MOVE) ? "Move" : "Drop";

        // Get the type of widget being dropped/moved, IF there is only one. If
        // there is more than one, just reference it as "Widgets".
        String object;
        if (elements != null && elements.length == 1) {
            object = getSimpleName(elements[0].getFqcn());
        } else {
            object = "Widgets";
        }

        String where = getSimpleName(targetNode.getFqcn());

        // When we localize this: $1 is the verb (Move or Drop), $2 is the
        // object (such as "Button"), and $3 is the place we are doing it (such
        // as "LinearLayout").
        return String.format("%1$s %2$s in %3$s", verb, object, where);
    }

    /**
     * Returns simple name (basename, following last dot) of a fully qualified
     * class name.
     *
     * @param fqcn The fqcn to reduce
     * @return The base name of the fqcn
     */
    public static String getSimpleName(String fqcn) {
        // Note that the following works even when there is no dot, since
        // lastIndexOf will return -1 so we get fcqn.substring(-1+1) =
        // fcqn.substring(0) = fqcn
        return fqcn.substring(fqcn.lastIndexOf('.') + 1);
    }

    /**
     * Updates the {@link DropFeedback#isCopy} and {@link DropFeedback#sameCanvas} fields
     * of the given {@link DropFeedback}. This is generally called right before invoking
     * one of the callOnXyz methods of GRE to refresh the fields.
     *
     * @param df The current {@link DropFeedback}.
     * @param event An optional event to determine if the current operation is copy or move.
     */
    private void updateDropFeedback(DropFeedback df, DropTargetEvent event) {
        if (event != null) {
            df.isCopy = event.detail == DND.DROP_COPY;
        }
        df.sameCanvas = mCanvas == mGlobalDragInfo.getSourceCanvas();
        df.invalidTarget = false;
        df.dipScale = mCanvas.getEditorDelegate().getGraphicalEditor().getDipScale();
        df.modifierMask = mCanvas.getGestureManager().getRuleModifierMask();

        // Set the drag bounds, after converting it from control coordinates to
        // layout coordinates
        GlobalCanvasDragInfo dragInfo = GlobalCanvasDragInfo.getInstance();
        Rect dragBounds = null;
        Rect controlDragBounds = dragInfo.getDragBounds();
        if (controlDragBounds != null) {
            CanvasTransform ht = mCanvas.getHorizontalTransform();
            CanvasTransform vt = mCanvas.getVerticalTransform();
            double horizScale = ht.getScale();
            double verticalScale = vt.getScale();
            int x = (int) (controlDragBounds.x / horizScale);
            int y = (int) (controlDragBounds.y / verticalScale);
            int w = (int) (controlDragBounds.w / horizScale);
            int h = (int) (controlDragBounds.h / verticalScale);
            dragBounds = new Rect(x, y, w, h);
        }
        int baseline = dragInfo.getDragBaseline();
        if (baseline != -1) {
            df.dragBaseline = baseline;
        }
        df.dragBounds = dragBounds;
    }

    /**
     * Verifies that event.currentDataType is of type {@link SimpleXmlTransfer}.
     * If not, try to find a valid data type.
     * Otherwise set the drop to {@link DND#DROP_NONE} to cancel it.
     *
     * @return True if the data type is accepted.
     */
    private static boolean checkDataType(DropTargetEvent event) {

        SimpleXmlTransfer sxt = SimpleXmlTransfer.getInstance();

        TransferData current = event.currentDataType;

        if (sxt.isSupportedType(current)) {
            return true;
        }

        // We only support SimpleXmlTransfer and the current data type is not right.
        // Let's see if we can find another one.

        for (TransferData td : event.dataTypes) {
            if (td != current && sxt.isSupportedType(td)) {
                // We like this type better.
                event.currentDataType = td;
                return true;
            }
        }

        // We failed to find any good transfer type.
        event.detail = DND.DROP_NONE;
        return false;
    }

    /**
     * Returns the mouse location of the drop target event.
     *
     * @param event the drop target event
     * @return a {@link ControlPoint} location corresponding to the top left corner
     */
    private ControlPoint getDropLocation(DropTargetEvent event) {
        return ControlPoint.create(mCanvas, event);
    }

    /**
     * Called on both dragEnter and dragMove.
     * Generates the onDropEnter/Move/Leave events depending on the currently
     * selected target node.
     */
    private void processDropEvent(DropTargetEvent event) {
        if (!mCanvas.getViewHierarchy().isValid()) {
            // We don't allow drop on an invalid layout, even if we have some obsolete
            // layout info for it.
            event.detail = DND.DROP_NONE;
            clearDropInfo();
            return;
        }

        LayoutPoint p = getDropLocation(event).toLayout();

        // Is the mouse currently captured by a DropFeedback.captureArea?
        boolean isCaptured = false;
        if (mFeedback != null) {
            Rect r = mFeedback.captureArea;
            isCaptured = r != null && r.contains(p.x, p.y);
        }

        // We can't switch views/nodes when the mouse is captured
        CanvasViewInfo vi;
        if (isCaptured) {
            vi = mCurrentView;
        } else {
            vi = mCanvas.getViewHierarchy().findViewInfoAt(p);

            // When dragging into the canvas, if you are not over any other view, target
            // the root element (since it may not "fill" the screen, e.g. if you have a linear
            // layout but have layout_height wrap_content, then the layout will only extend
            // to cover the children in the layout, not the whole visible screen area, which
            // may be surprising
            if (vi == null) {
                vi = mCanvas.getViewHierarchy().getRoot();
            }
        }

        boolean isMove = true;
        boolean needRedraw = false;

        if (vi != mCurrentView) {
            // Current view has changed. Does that also change the target node?
            // Note that either mCurrentView or vi can be null.

            if (vi == null) {
                // vi is null but mCurrentView is not, no view is a target anymore
                // We don't need onDropMove in this case
                isMove = false;
                needRedraw = true;
                event.detail = DND.DROP_NONE;
                clearDropInfo(); // this will call callDropLeave.

            } else {
                // vi is a new current view.
                // Query GRE for onDropEnter on the ViewInfo hierarchy, starting from the child
                // towards its parent, till we find one that returns a non-null drop feedback.

                DropFeedback df = null;
                NodeProxy targetNode = null;

                for (CanvasViewInfo targetVi = vi;
                     targetVi != null && df == null;
                     targetVi = targetVi.getParent()) {
                    targetNode = mCanvas.getNodeFactory().create(targetVi);
                    df = mCanvas.getRulesEngine().callOnDropEnter(targetNode,
                            targetVi.getViewObject(), mCurrentDragElements);

                    if (df != null) {
                        // We should also dispatch an onDropMove() call to the initial enter
                        // position, such that the view is notified of the position where
                        // we are within the node immediately (before we for example attempt
                        // to draw feedback). This is necessary since most views perform the
                        // guideline computations in onDropMove (since only onDropMove is handed
                        // the -position- of the mouse), and we want this computation to happen
                        // before we ask the view to draw its feedback.
                        updateDropFeedback(df, event);
                        df = mCanvas.getRulesEngine().callOnDropMove(targetNode,
                                mCurrentDragElements, df, new Point(p.x, p.y));
                    }

                    if (df != null &&
                            event.detail == DND.DROP_MOVE &&
                            mCanvas == mGlobalDragInfo.getSourceCanvas()) {
                        // You can't move an object into itself in the same canvas.
                        // E.g. case of moving a layout and the node under the mouse is the
                        // layout itself: a copy would be ok but not a move operation of the
                        // layout into himself.

                        SelectionItem[] selection = mGlobalDragInfo.getCurrentSelection();
                        if (selection != null) {
                            for (SelectionItem cs : selection) {
                                if (cs.getViewInfo() == targetVi) {
                                    // The node that responded is one of the selection roots.
                                    // Simply invalidate the drop feedback and move on the
                                    // parent in the ViewInfo chain.

                                    updateDropFeedback(df, event);
                                    mCanvas.getRulesEngine().callOnDropLeave(
                                            targetNode, mCurrentDragElements, df);
                                    df = null;
                                    targetNode = null;
                                }
                            }
                        }
                    }
                }

                if (df == null) {
                    // Provide visual feedback that we are refusing the drop
                    event.detail = DND.DROP_NONE;
                    clearDropInfo();

                } else if (targetNode != mTargetNode) {
                    // We found a new target node for the drag'n'drop.
                    // Release the previous one, if any.
                    callDropLeave();

                    // And assign the new one
                    mTargetNode = targetNode;
                    mFeedback = df;

                    // We don't need onDropMove in this case
                    isMove = false;
                }
            }

            mCurrentView = vi;
        }

        if (isMove && mTargetNode != null && mFeedback != null) {
            // this is a move inside the same view
            com.android.ide.common.api.Point p2 =
                new com.android.ide.common.api.Point(p.x, p.y);
            updateDropFeedback(mFeedback, event);
            DropFeedback df = mCanvas.getRulesEngine().callOnDropMove(
                    mTargetNode, mCurrentDragElements, mFeedback, p2);
            mCanvas.getGestureManager().updateMessage(mFeedback);

            if (df == null) {
                // The target is no longer interested in the drop move.
                event.detail = DND.DROP_NONE;
                callDropLeave();

            } else if (df != mFeedback) {
                mFeedback = df;
            }
        }

        if (mFeedback != null) {
            if (event.detail == DND.DROP_NONE && !mFeedback.invalidTarget) {
                // If we previously provided visual feedback that we were refusing
                // the drop, we now need to change it to mean we're accepting it.
                event.detail = DND.DROP_DEFAULT;
                recomputeDragType(event);

            } else if (mFeedback.invalidTarget) {
                // Provide visual feedback that we are refusing the drop
                event.detail = DND.DROP_NONE;
            }
        }

        if (needRedraw || (mFeedback != null && mFeedback.requestPaint)) {
            mCanvas.redraw();
        }

        // Update outline to show the target node there
        OutlinePage outline = mCanvas.getOutlinePage();
        TreeSelection newSelection = TreeSelection.EMPTY;
        if (mCurrentView != null && mTargetNode != null) {
            // Find the view corresponding to the target node. The current view can be a leaf
            // view whereas the target node is always a parent layout.
            if (mCurrentView.getUiViewNode() != mTargetNode.getNode()) {
                mCurrentView = mCurrentView.getParent();
            }
            if (mCurrentView != null && mCurrentView.getUiViewNode() == mTargetNode.getNode()) {
                TreePath treePath = SelectionManager.getTreePath(mCurrentView);
                newSelection = new TreeSelection(treePath);
            }
        }

        ISelection currentSelection = outline.getSelection();
        if (currentSelection == null || !currentSelection.equals(newSelection)) {
            outline.setSelection(newSelection);
        }
    }

    /**
     * Calls onDropLeave on mTargetNode with the current mFeedback. <br/>
     * Then clears mTargetNode and mFeedback.
     */
    private void callDropLeave() {
        if (mTargetNode != null && mFeedback != null) {
            updateDropFeedback(mFeedback, null);
            mCanvas.getRulesEngine().callOnDropLeave(mTargetNode, mCurrentDragElements, mFeedback);
        }

        mTargetNode = null;
        mFeedback = null;
    }

    private void clearDropInfo() {
        callDropLeave();
        mCurrentView = null;
        mCanvas.redraw();
    }

    /**
     * Creates a root element in an empty document.
     * Only the first element's FQCN of the dragged elements is used.
     * <p/>
     * Actual XML handling is done by {@link LayoutCanvas#createDocumentRoot(String)}.
     */
    private void createDocumentRoot(SimpleElement[] elements) {
        if (elements == null || elements.length < 1 || elements[0] == null) {
            return;
        }

        mCanvas.createDocumentRoot(elements[0]);
    }

    /**
     * An {@link Overlay} to paint the move feedback. This just delegates to the
     * layout rules.
     */
    private class MoveOverlay extends Overlay {
        @Override
        public void paint(GC gc) {
            if (mTargetNode != null && mFeedback != null) {
                RulesEngine rulesEngine = mCanvas.getRulesEngine();
                rulesEngine.callDropFeedbackPaint(mCanvas.getGcWrapper(), mTargetNode, mFeedback);
                mFeedback.requestPaint = false;
            }
        }
    }
}

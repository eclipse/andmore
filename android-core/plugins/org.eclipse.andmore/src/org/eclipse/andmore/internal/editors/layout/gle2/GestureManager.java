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

import com.android.SdkConstants;
import com.android.ide.common.api.DropFeedback;
import com.android.ide.common.api.IViewRule;
import com.android.ide.common.api.Rect;
import com.android.ide.common.api.SegmentType;
import com.android.utils.Pair;

import org.eclipse.andmore.internal.editors.layout.gre.NodeProxy;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.TypedEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorSite;

import java.util.ArrayList;
import java.util.List;

/**
 * The {@link GestureManager} is is the central manager of gestures; it is responsible
 * for recognizing when particular gestures should begin and terminate. It
 * listens to the drag, mouse and keyboard systems to find out when to start
 * gestures and in order to update the gestures along the way.
 */
public class GestureManager {
    /** The canvas which owns this GestureManager. */
    private final LayoutCanvas mCanvas;

    /** The currently executing gesture, or null. */
    private Gesture mCurrentGesture;

    /** A listener for drop target events. */
    private final DropTargetListener mDropListener = new CanvasDropListener();

    /** A listener for drag source events. */
    private final DragSourceListener mDragSourceListener = new CanvasDragSourceListener();

    /** Tooltip shown during the gesture, or null */
    private GestureToolTip mTooltip;

    /**
     * The list of overlays associated with {@link #mCurrentGesture}. Will be
     * null before it has been initialized lazily by the paint routine (the
     * initialized value can never be null, but it can be an empty collection).
     */
    private List<Overlay> mOverlays;

    /**
     * Most recently seen mouse position (x coordinate). We keep a copy of this
     * value since we sometimes need to know it when we aren't told about the
     * mouse position (such as when a keystroke is received, such as an arrow
     * key in order to tweak the current drop position)
     */
    protected int mLastMouseX;

    /**
     * Most recently seen mouse position (y coordinate). We keep a copy of this
     * value since we sometimes need to know it when we aren't told about the
     * mouse position (such as when a keystroke is received, such as an arrow
     * key in order to tweak the current drop position)
     */
    protected int mLastMouseY;

    /**
     * Most recently seen mouse mask. We keep a copy of this since in some
     * scenarios (such as on a drag gesture) we don't get access to it.
     */
    protected int mLastStateMask;

    /**
     * Listener for mouse motion, click and keyboard events.
     */
    private Listener mListener;

    /**
     * When we the drag leaves, we don't know if that's the last we'll see of
     * this drag or if it's just temporarily outside the canvas and it will
     * return. We want to restore it if it comes back. This is also necessary
     * because even on a drop we'll receive a
     * {@link DropTargetListener#dragLeave} right before the drop, and we need
     * to restore it in the drop. Therefore, when we lose a {@link DropGesture}
     * to a {@link DropTargetListener#dragLeave}, we store a reference to the
     * current gesture as a {@link #mZombieGesture}, since the gesture is dead
     * but might be brought back to life if we see a subsequent
     * {@link DropTargetListener#dragEnter} before another gesture begins.
     */
    private DropGesture mZombieGesture;

    /**
     * Flag tracking whether we've set a message or error message on the global status
     * line (since we only want to clear that message if we have set it ourselves).
     * This is the actual message rather than a boolean such that (if we can get our
     * hands on the global message) we can check to see if the current message is the
     * one we set and only in that case clear it when it is no longer applicable.
     */
    private String mDisplayingMessage;

    /**
     * Constructs a new {@link GestureManager} for the given
     * {@link LayoutCanvas}.
     *
     * @param canvas The canvas which controls this {@link GestureManager}
     */
    public GestureManager(LayoutCanvas canvas) {
        mCanvas = canvas;
    }

    /**
     * Returns the canvas associated with this GestureManager.
     *
     * @return The {@link LayoutCanvas} associated with this GestureManager.
     *         Never null.
     */
    public LayoutCanvas getCanvas() {
        return mCanvas;
    }

    /**
     * Returns the current gesture, if one is in progress, and otherwise returns
     * null.
     *
     * @return The current gesture or null.
     */
    public Gesture getCurrentGesture() {
        return mCurrentGesture;
    }

    /**
     * Paints the overlays associated with the current gesture, if any.
     *
     * @param gc The graphics object to paint into.
     */
    public void paint(GC gc) {
        if (mCurrentGesture == null) {
            return;
        }

        if (mOverlays == null) {
            mOverlays = mCurrentGesture.createOverlays();
            Device device = gc.getDevice();
            for (Overlay overlay : mOverlays) {
                overlay.create(device);
            }
        }
        for (Overlay overlay : mOverlays) {
            overlay.paint(gc);
        }
    }

    /**
     * Registers all the listeners needed by the {@link GestureManager}.
     *
     * @param dragSource The drag source in the {@link LayoutCanvas} to listen
     *            to.
     * @param dropTarget The drop target in the {@link LayoutCanvas} to listen
     *            to.
     */
    public void registerListeners(DragSource dragSource, DropTarget dropTarget) {
        assert mListener == null;
        mListener = new Listener();
        mCanvas.addMouseMoveListener(mListener);
        mCanvas.addMouseListener(mListener);
        mCanvas.addKeyListener(mListener);

        if (dragSource != null) {
            dragSource.addDragListener(mDragSourceListener);
        }
        if (dropTarget != null) {
            dropTarget.addDropListener(mDropListener);
        }
    }

    /**
     * Unregisters all the listeners previously registered by
     * {@link #registerListeners}.
     *
     * @param dragSource The drag source in the {@link LayoutCanvas} to stop
     *            listening to.
     * @param dropTarget The drop target in the {@link LayoutCanvas} to stop
     *            listening to.
     */
    public void unregisterListeners(DragSource dragSource, DropTarget dropTarget) {
        if (mCanvas.isDisposed()) {
            // If the LayoutCanvas is already disposed, we shouldn't try to unregister
            // the listeners; they are already not active and an attempt to remove the
            // listener will throw a widget-is-disposed exception.
            mListener = null;
            return;
        }

        if (mListener != null) {
            mCanvas.removeMouseMoveListener(mListener);
            mCanvas.removeMouseListener(mListener);
            mCanvas.removeKeyListener(mListener);
            mListener = null;
        }

        if (dragSource != null) {
            dragSource.removeDragListener(mDragSourceListener);
        }
        if (dropTarget != null) {
            dropTarget.removeDropListener(mDropListener);
        }
    }

    /**
     * Starts the given gesture.
     *
     * @param mousePos The most recent mouse coordinate applicable to the new
     *            gesture, in control coordinates.
     * @param gesture The gesture to initiate
     */
    private void startGesture(ControlPoint mousePos, Gesture gesture, int mask) {
        if (mCurrentGesture != null) {
            finishGesture(mousePos, true);
            assert mCurrentGesture == null;
        }

        if (gesture != null) {
            mCurrentGesture = gesture;
            mCurrentGesture.begin(mousePos, mask);
        }
    }

    /**
     * Updates the current gesture, if any, for the given event.
     *
     * @param mousePos The most recent mouse coordinate applicable to the new
     *            gesture, in control coordinates.
     * @param event The event corresponding to this update. May be null. Don't
     *            make any assumptions about the type of this event - for
     *            example, it may not always be a MouseEvent, it could be a
     *            DragSourceEvent, etc.
     */
    private void updateMouse(ControlPoint mousePos, TypedEvent event) {
        if (mCurrentGesture != null) {
            mCurrentGesture.update(mousePos);
        }
    }

    /**
     * Finish the given gesture, either from successful completion or from
     * cancellation.
     *
     * @param mousePos The most recent mouse coordinate applicable to the new
     *            gesture, in control coordinates.
     * @param canceled True if and only if the gesture was canceled.
     */
    private void finishGesture(ControlPoint mousePos, boolean canceled) {
        if (mCurrentGesture != null) {
            mCurrentGesture.end(mousePos, canceled);
            if (mOverlays != null) {
                for (Overlay overlay : mOverlays) {
                    overlay.dispose();
                }
                mOverlays = null;
            }
            mCurrentGesture = null;
            mZombieGesture = null;
            mLastStateMask = 0;
            updateMessage(null);
            updateCursor(mousePos);
            mCanvas.redraw();
        }
    }

    /**
     * Update the cursor to show the type of operation we expect on a mouse press:
     * <ul>
     * <li>Over a selection handle, show a directional cursor depending on the position of
     * the selection handle
     * <li>Over a widget, show a move (hand) cursor
     * <li>Otherwise, show the default arrow cursor
     * </ul>
     */
    void updateCursor(ControlPoint controlPoint) {
        // We don't hover on the root since it's not a widget per see and it is always there.
        SelectionManager selectionManager = mCanvas.getSelectionManager();

        if (!selectionManager.isEmpty()) {
            Display display = mCanvas.getDisplay();
            Pair<SelectionItem, SelectionHandle> handlePair =
                selectionManager.findHandle(controlPoint);
            if (handlePair != null) {
                SelectionHandle handle = handlePair.getSecond();
                int cursorType = handle.getSwtCursorType();
                Cursor cursor = display.getSystemCursor(cursorType);
                if (cursor != mCanvas.getCursor()) {
                    mCanvas.setCursor(cursor);
                }
                return;
            }

            // See if it's over a selected view
            LayoutPoint layoutPoint = controlPoint.toLayout();
            for (SelectionItem item : selectionManager.getSelections()) {
                if (item.getRect().contains(layoutPoint.x, layoutPoint.y)
                        && !item.isRoot()) {
                    Cursor cursor = display.getSystemCursor(SWT.CURSOR_HAND);
                    if (cursor != mCanvas.getCursor()) {
                        mCanvas.setCursor(cursor);
                    }
                    return;
                }
            }
        }

        if (mCanvas.getCursor() != null) {
            mCanvas.setCursor(null);
        }
    }

    /**
     * Update the Eclipse status message with any feedback messages from the given
     * {@link DropFeedback} object, or clean up if there is no more feedback to process
     * @param feedback the feedback whose message we want to display, or null to clear the
     *            message if previously set
     */
    void updateMessage(DropFeedback feedback) {
        IEditorSite editorSite = mCanvas.getEditorDelegate().getEditor().getEditorSite();
        IStatusLineManager status = editorSite.getActionBars().getStatusLineManager();
        if (feedback == null) {
            if (mDisplayingMessage != null) {
                status.setMessage(null);
                status.setErrorMessage(null);
                mDisplayingMessage = null;
            }
        } else if (feedback.errorMessage != null) {
            if (!feedback.errorMessage.equals(mDisplayingMessage)) {
                mDisplayingMessage = feedback.errorMessage;
                status.setErrorMessage(mDisplayingMessage);
            }
        } else if (feedback.message != null) {
            if (!feedback.message.equals(mDisplayingMessage)) {
                mDisplayingMessage = feedback.message;
                status.setMessage(mDisplayingMessage);
            }
        } else if (mDisplayingMessage != null) {
            // TODO: Can we check the existing message and only clear it if it's the
            // same as the one we set?
            mDisplayingMessage = null;
            status.setMessage(null);
            status.setErrorMessage(null);
        }

        // Tooltip
        if (feedback != null && feedback.tooltip != null) {
            Pair<Boolean,Boolean> position = mCurrentGesture.getTooltipPosition();
            boolean below = position.getFirst();
            if (feedback.tooltipY != null) {
                below = feedback.tooltipY == SegmentType.BOTTOM;
            }
            boolean toRightOf = position.getSecond();
            if (feedback.tooltipX != null) {
                toRightOf = feedback.tooltipX == SegmentType.RIGHT;
            }
            if (mTooltip == null) {
                mTooltip = new GestureToolTip(mCanvas, below, toRightOf);
            }
            mTooltip.update(feedback.tooltip, below, toRightOf);
        } else if (mTooltip != null) {
            mTooltip.dispose();
            mTooltip = null;
        }
    }

    /**
     * Returns the current mouse position as a {@link ControlPoint}
     *
     * @return the current mouse position as a {@link ControlPoint}
     */
    public ControlPoint getCurrentControlPoint() {
        return ControlPoint.create(mCanvas, mLastMouseX, mLastMouseY);
    }

    /**
     * Returns the current SWT modifier key mask as an {@link IViewRule} modifier mask
     *
     * @return the current SWT modifier key mask as an {@link IViewRule} modifier mask
     */
    public int getRuleModifierMask() {
        int swtMask = mLastStateMask;
        int modifierMask = 0;
        if ((swtMask & SWT.MOD1) != 0) {
            modifierMask |= DropFeedback.MODIFIER1;
        }
        if ((swtMask & SWT.MOD2) != 0) {
            modifierMask |= DropFeedback.MODIFIER2;
        }
        if ((swtMask & SWT.MOD3) != 0) {
            modifierMask |= DropFeedback.MODIFIER3;
        }
        return modifierMask;
    }

    /**
     * Helper class which implements the {@link MouseMoveListener},
     * {@link MouseListener} and {@link KeyListener} interfaces.
     */
    private class Listener implements MouseMoveListener, MouseListener, MouseTrackListener,
            KeyListener {

        // --- MouseMoveListener ---

        @Override
        public void mouseMove(MouseEvent e) {
            mLastMouseX = e.x;
            mLastMouseY = e.y;
            mLastStateMask = e.stateMask;

            ControlPoint controlPoint = ControlPoint.create(mCanvas, e);
            if ((e.stateMask & SWT.BUTTON_MASK) != 0) {
                if (mCurrentGesture != null) {
                    updateMouse(controlPoint, e);
                    mCanvas.redraw();
                }
            } else {
                updateCursor(controlPoint);
                mCanvas.hover(e);
                mCanvas.getPreviewManager().moved(controlPoint);
            }
        }

        // --- MouseListener ---

        @Override
        public void mouseUp(MouseEvent e) {
            ControlPoint mousePos = ControlPoint.create(mCanvas, e);

            if (mCurrentGesture == null) {
                // If clicking on a configuration preview, just process it there
                if (mCanvas.getPreviewManager().click(mousePos)) {
                    return;
                }

                // Just a click, select
                Pair<SelectionItem, SelectionHandle> handlePair =
                    mCanvas.getSelectionManager().findHandle(mousePos);
                if (handlePair == null) {
                    mCanvas.getSelectionManager().select(e);
                }
            }
            if (mCurrentGesture == null) {
                updateCursor(mousePos);
            } else if (mCurrentGesture instanceof DropGesture) {
                // Mouse Up shouldn't be delivered in the middle of a drag & drop -
                // but this can happen on some versions of Linux
                // (see http://code.google.com/p/android/issues/detail?id=19057 )
                // and if we process the mouseUp it will abort the remainder of
                // the drag & drop operation, so ignore this event!
            } else {
                finishGesture(mousePos, false);
            }
            mCanvas.redraw();
        }

        @Override
        public void mouseDown(MouseEvent e) {
            mLastMouseX = e.x;
            mLastMouseY = e.y;
            mLastStateMask = e.stateMask;

            // Not yet used. Should be, for Mac and Linux.
        }

        @Override
        public void mouseDoubleClick(MouseEvent e) {
            // SWT delivers a double click event even if you click two different buttons
            // in rapid succession. In any case, we only want to let you double click the
            // first button to warp to XML:
            if (e.button == 1) {
                // Warp to the text editor and show the corresponding XML for the
                // double-clicked widget
                LayoutPoint p = ControlPoint.create(mCanvas, e).toLayout();
                CanvasViewInfo vi = mCanvas.getViewHierarchy().findViewInfoAt(p);
                if (vi != null) {
                    mCanvas.show(vi);
                }
            }
        }

        // --- MouseTrackListener ---

        @Override
        public void mouseEnter(MouseEvent e) {
            ControlPoint mousePos = ControlPoint.create(mCanvas, e);
            mCanvas.getPreviewManager().enter(mousePos);
        }

        @Override
        public void mouseExit(MouseEvent e) {
            ControlPoint mousePos = ControlPoint.create(mCanvas, e);
            mCanvas.getPreviewManager().exit(mousePos);
        }

        @Override
        public void mouseHover(MouseEvent e) {
        }

        // --- KeyListener ---

        @Override
        public void keyPressed(KeyEvent e) {
            mLastStateMask = e.stateMask;
            // Workaround for the fact that in keyPressed the current state
            // mask is not yet updated
            if (e.keyCode == SWT.SHIFT) {
                mLastStateMask |= SWT.MOD2;
            }
            if (SdkConstants.CURRENT_PLATFORM == SdkConstants.PLATFORM_DARWIN) {
                if (e.keyCode == SWT.COMMAND) {
                    mLastStateMask |= SWT.MOD1;
                }
            } else {
                if (e.keyCode == SWT.CTRL) {
                    mLastStateMask |= SWT.MOD1;
                }
            }

            // Give gestures a first chance to see and consume the key press
            if (mCurrentGesture != null) {
                // unless it's "Escape", which cancels the gesture
                if (e.keyCode == SWT.ESC) {
                    ControlPoint controlPoint = ControlPoint.create(mCanvas,
                            mLastMouseX, mLastMouseY);
                    finishGesture(controlPoint, true);
                    return;
                }

                if (mCurrentGesture.keyPressed(e)) {
                    return;
                }
            }

            // Fall back to canvas actions for the key press
            mCanvas.handleKeyPressed(e);
        }

        @Override
        public void keyReleased(KeyEvent e) {
            mLastStateMask = e.stateMask;
            // Workaround for the fact that in keyPressed the current state
            // mask is not yet updated
            if (e.keyCode == SWT.SHIFT) {
                mLastStateMask &= ~SWT.MOD2;
            }
            if (SdkConstants.CURRENT_PLATFORM == SdkConstants.PLATFORM_DARWIN) {
                if (e.keyCode == SWT.COMMAND) {
                    mLastStateMask &= ~SWT.MOD1;
                }
            } else {
                if (e.keyCode == SWT.CTRL) {
                    mLastStateMask &= ~SWT.MOD1;
                }
            }

            if (mCurrentGesture != null) {
                mCurrentGesture.keyReleased(e);
            }
        }
    }

    /** Listener for Drag &amp; Drop events. */
    private class CanvasDropListener implements DropTargetListener {
        public CanvasDropListener() {
        }

        /**
         * The cursor has entered the drop target boundaries. {@inheritDoc}
         */
        @Override
        public void dragEnter(DropTargetEvent event) {
            mCanvas.showInvisibleViews(true);
            mCanvas.getEditorDelegate().getGraphicalEditor().dismissHoverPalette();

            if (mCurrentGesture == null) {
                Gesture newGesture = mZombieGesture;
                if (newGesture == null) {
                    newGesture = new MoveGesture(mCanvas);
                } else {
                    mZombieGesture = null;
                }
                startGesture(ControlPoint.create(mCanvas, event),
                        newGesture, 0);
            }

            if (mCurrentGesture instanceof DropGesture) {
                ((DropGesture) mCurrentGesture).dragEnter(event);
            }
        }

        /**
         * The cursor is moving over the drop target. {@inheritDoc}
         */
        @Override
        public void dragOver(DropTargetEvent event) {
            if (mCurrentGesture instanceof DropGesture) {
                ((DropGesture) mCurrentGesture).dragOver(event);
            }
        }

        /**
         * The cursor has left the drop target boundaries OR data is about to be
         * dropped. {@inheritDoc}
         */
        @Override
        public void dragLeave(DropTargetEvent event) {
            if (mCurrentGesture instanceof DropGesture) {
                DropGesture dropGesture = (DropGesture) mCurrentGesture;
                dropGesture.dragLeave(event);
                finishGesture(ControlPoint.create(mCanvas, event), true);
                mZombieGesture = dropGesture;
            }

            mCanvas.showInvisibleViews(false);
        }

        /**
         * The drop is about to be performed. The drop target is given a last
         * chance to change the nature of the drop. {@inheritDoc}
         */
        @Override
        public void dropAccept(DropTargetEvent event) {
            Gesture gesture = mCurrentGesture != null ? mCurrentGesture : mZombieGesture;
            if (gesture instanceof DropGesture) {
                ((DropGesture) gesture).dropAccept(event);
            }
        }

        /**
         * The data is being dropped. {@inheritDoc}
         */
        @Override
        public void drop(final DropTargetEvent event) {
            // See if we had a gesture just prior to the drop (we receive a dragLeave
            // right before the drop which we don't know whether means the cursor has
            // left the canvas for good or just before a drop)
            Gesture gesture = mCurrentGesture != null ? mCurrentGesture : mZombieGesture;
            mZombieGesture = null;

            if (gesture instanceof DropGesture) {
                ((DropGesture) gesture).drop(event);

                finishGesture(ControlPoint.create(mCanvas, event), true);
            }
        }

        /**
         * The operation being performed has changed (e.g. modifier key).
         * {@inheritDoc}
         */
        @Override
        public void dragOperationChanged(DropTargetEvent event) {
            if (mCurrentGesture instanceof DropGesture) {
                ((DropGesture) mCurrentGesture).dragOperationChanged(event);
            }
        }
    }

    /**
     * Our canvas {@link DragSourceListener}. Handles drag being started and
     * finished and generating the drag data.
     */
    private class CanvasDragSourceListener implements DragSourceListener {

        /**
         * The current selection being dragged. This may be a subset of the
         * canvas selection due to the "sanitize" pass. Can be empty but never
         * null.
         */
        private final ArrayList<SelectionItem> mDragSelection = new ArrayList<SelectionItem>();

        private SimpleElement[] mDragElements;

        /**
         * The user has begun the actions required to drag the widget.
         * <p/>
         * Initiate a drag only if there is one or more item selected. If
         * there's none, try to auto-select the one under the cursor.
         * {@inheritDoc}
         */
        @Override
        public void dragStart(DragSourceEvent e) {
            LayoutPoint p = LayoutPoint.create(mCanvas, e);
            ControlPoint controlPoint = ControlPoint.create(mCanvas, e);
            SelectionManager selectionManager = mCanvas.getSelectionManager();

            // See if the mouse is over a selection handle; if so, start a resizing
            // gesture.
            Pair<SelectionItem, SelectionHandle> handle =
                selectionManager.findHandle(controlPoint);
            if (handle != null) {
                startGesture(controlPoint, new ResizeGesture(mCanvas, handle.getFirst(),
                        handle.getSecond()), mLastStateMask);
                e.detail = DND.DROP_NONE;
                e.doit = false;
                mCanvas.redraw();
                return;
            }

            // We need a selection (simple or multiple) to do any transfer.
            // If there's a selection *and* the cursor is over this selection,
            // use all the currently selected elements.
            // If there is no selection or the cursor is not over a selected
            // element, *change* the selection to match the element under the
            // cursor and use that. If nothing can be selected, abort the drag
            // operation.
            List<SelectionItem> selections = selectionManager.getSelections();
            mDragSelection.clear();
            SelectionItem primary = null;

            if (!selections.isEmpty()) {
                // Is the cursor on top of a selected element?
                boolean insideSelection = false;

                for (SelectionItem cs : selections) {
                    if (!cs.isRoot() && cs.getRect().contains(p.x, p.y)) {
                        primary = cs;
                        insideSelection = true;
                        break;
                    }
                }

                if (!insideSelection) {
                    CanvasViewInfo vi = mCanvas.getViewHierarchy().findViewInfoAt(p);
                    if (vi != null && !vi.isRoot() && !vi.isHidden()) {
                        primary = selectionManager.selectSingle(vi);
                        insideSelection = true;
                    }
                }

                if (insideSelection) {
                    // We should now have a proper selection that matches the
                    // cursor. Let's use this one. We make a copy of it since
                    // the "sanitize" pass below might remove some of the
                    // selected objects.
                    if (selections.size() == 1) {
                        // You are dragging just one element - this might or
                        // might not be the root, but if it's the root that is
                        // fine since we will let you drag the root if it is the
                        // only thing you are dragging.
                        mDragSelection.addAll(selections);
                    } else {
                        // Only drag non-root items.
                        for (SelectionItem cs : selections) {
                            if (!cs.isRoot() && !cs.isHidden()) {
                                mDragSelection.add(cs);
                            } else if (cs == primary) {
                                primary = null;
                            }
                        }
                    }
                }
            }

            // If you are dragging a non-selected item, select it
            if (mDragSelection.isEmpty()) {
                CanvasViewInfo vi = mCanvas.getViewHierarchy().findViewInfoAt(p);
                if (vi != null && !vi.isRoot() && !vi.isHidden()) {
                    primary = selectionManager.selectSingle(vi);
                    mDragSelection.addAll(selections);
                }
            }

            SelectionManager.sanitize(mDragSelection);

            e.doit = !mDragSelection.isEmpty();
            int imageCount = mDragSelection.size();
            if (e.doit) {
                mDragElements = SelectionItem.getAsElements(mDragSelection, primary);
                GlobalCanvasDragInfo.getInstance().startDrag(mDragElements,
                        mDragSelection.toArray(new SelectionItem[imageCount]),
                        mCanvas, new Runnable() {
                            @Override
                            public void run() {
                                mCanvas.getClipboardSupport().deleteSelection("Remove",
                                        mDragSelection);
                            }
                        });
            }

            // If you drag on the -background-, we make that into a marquee
            // selection
            if (!e.doit || (imageCount == 1
                    && (mDragSelection.get(0).isRoot() || mDragSelection.get(0).isHidden()))) {
                boolean toggle = (mLastStateMask & (SWT.CTRL | SWT.SHIFT | SWT.COMMAND)) != 0;
                startGesture(controlPoint,
                        new MarqueeGesture(mCanvas, toggle), mLastStateMask);
                e.detail = DND.DROP_NONE;
                e.doit = false;
            } else {
                // Otherwise, the drag means you are moving something
                mCanvas.showInvisibleViews(true);
                startGesture(controlPoint, new MoveGesture(mCanvas), 0);

                // Render drag-images: Copy portions of the full screen render.
                Image image = mCanvas.getImageOverlay().getImage();
                if (image != null) {
                    /**
                     * Transparency of the dragged image ([0-255]). We're using 30%
                     * translucency to make the image faint and not obscure the drag
                     * feedback below it.
                     */
                    final byte DRAG_TRANSPARENCY = (byte) (0.3 * 255);

                    List<Rectangle> rectangles = new ArrayList<Rectangle>(imageCount);
                    if (imageCount > 0) {
                        ImageData data = image.getImageData();
                        Rectangle imageRectangle = new Rectangle(0, 0, data.width, data.height);
                        for (SelectionItem item : mDragSelection) {
                            Rectangle bounds = item.getRect();
                            // Some bounds can be outside the rendered rectangle (for
                            // example, in an absolute layout, you can have negative
                            // coordinates), so create the intersection of these bounds.
                            Rectangle clippedBounds = imageRectangle.intersection(bounds);
                            rectangles.add(clippedBounds);
                        }
                        Rectangle boundingBox = ImageUtils.getBoundingRectangle(rectangles);
                        double scale = mCanvas.getHorizontalTransform().getScale();
                        e.image = SwtUtils.drawRectangles(image, rectangles, boundingBox, scale,
                                DRAG_TRANSPARENCY);

                        // Set the image offset such that we preserve the relative
                        // distance between the mouse pointer and the top left corner of
                        // the dragged view
                        int deltaX = (int) (scale * (boundingBox.x - p.x));
                        int deltaY = (int) (scale * (boundingBox.y - p.y));
                        e.offsetX = -deltaX;
                        e.offsetY = -deltaY;

                        // View rules may need to know it as well
                        GlobalCanvasDragInfo dragInfo = GlobalCanvasDragInfo.getInstance();
                        Rect dragBounds = null;
                        int width = (int) (scale * boundingBox.width);
                        int height = (int) (scale * boundingBox.height);
                        dragBounds = new Rect(deltaX, deltaY, width, height);
                        dragInfo.setDragBounds(dragBounds);

                        // Record the baseline such that we can perform baseline alignment
                        // on the node as it's dragged around
                        NodeProxy firstNode =
                            mCanvas.getNodeFactory().create(mDragSelection.get(0).getViewInfo());
                        dragInfo.setDragBaseline(firstNode.getBaseline());
                    }
                }
            }

            // No hover during drag (since no mouse over events are delivered
            // during a drag to keep the hovers up to date anyway)
            mCanvas.clearHover();

            mCanvas.redraw();
        }

        /**
         * Callback invoked when data is needed for the event, typically right
         * before drop. The drop side decides what type of transfer to use and
         * this side must now provide the adequate data. {@inheritDoc}
         */
        @Override
        public void dragSetData(DragSourceEvent e) {
            if (TextTransfer.getInstance().isSupportedType(e.dataType)) {
                e.data = SelectionItem.getAsText(mCanvas, mDragSelection);
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

        /**
         * Callback invoked when the drop has been finished either way. On a
         * successful move, remove the originating elements.
         */
        @Override
        public void dragFinished(DragSourceEvent e) {
            // Clear the selection
            mDragSelection.clear();
            mDragElements = null;
            GlobalCanvasDragInfo.getInstance().stopDrag();

            finishGesture(ControlPoint.create(mCanvas, e), e.detail == DND.DROP_NONE);
            mCanvas.showInvisibleViews(false);
            mCanvas.redraw();
        }
    }
}

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

import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_ID;
import static com.android.SdkConstants.FQCN_SPACE;
import static com.android.SdkConstants.FQCN_SPACE_V7;
import static com.android.SdkConstants.NEW_ID_PREFIX;
import static org.eclipse.andmore.internal.editors.layout.gle2.SelectionHandle.PIXEL_MARGIN;
import static org.eclipse.andmore.internal.editors.layout.gle2.SelectionHandle.PIXEL_RADIUS;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.api.INode;
import com.android.ide.common.api.RuleAction;
import com.android.resources.ResourceType;
import com.android.utils.Pair;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.common.layout.BaseViewRule;
import org.eclipse.andmore.common.layout.GridLayoutRule;
import org.eclipse.andmore.internal.editors.descriptors.ElementDescriptor;
import org.eclipse.andmore.internal.editors.layout.LayoutEditorDelegate;
import org.eclipse.andmore.internal.editors.layout.gre.NodeFactory;
import org.eclipse.andmore.internal.editors.layout.gre.NodeProxy;
import org.eclipse.andmore.internal.editors.layout.gre.RulesEngine;
import org.eclipse.andmore.internal.editors.layout.uimodel.UiViewElementNode;
import org.eclipse.andmore.internal.editors.uimodel.UiElementNode;
import org.eclipse.andmore.internal.refactorings.core.RenameResourceWizard;
import org.eclipse.andmore.internal.refactorings.core.RenameResult;
import org.eclipse.andmore.internal.resources.ResourceNameValidator;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IWorkbenchPartSite;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

/**
 * The {@link SelectionManager} manages the selection in the canvas editor.
 * It holds (and can be asked about) the set of selected items, and it also has
 * operations for manipulating the selection - such as toggling items, copying
 * the selection to the clipboard, etc.
 * <p/>
 * This class implements {@link ISelectionProvider} so that it can delegate
 * the selection provider from the {@link LayoutCanvasViewer}.
 * <p/>
 * Note that {@link LayoutCanvasViewer} sets a selection change listener on this
 * manager so that it can invoke its own fireSelectionChanged when the canvas'
 * selection changes.
 */
public class SelectionManager implements ISelectionProvider {

    private LayoutCanvas mCanvas;

    /** The current selection list. The list is never null, however it can be empty. */
    private final LinkedList<SelectionItem> mSelections = new LinkedList<SelectionItem>();

    /** An unmodifiable view of {@link #mSelections}. */
    private final List<SelectionItem> mUnmodifiableSelection =
        Collections.unmodifiableList(mSelections);

    /** Barrier set when updating the selection to prevent from recursively
     * invoking ourselves. */
    private boolean mInsideUpdateSelection;

    /**
     * The <em>current</em> alternate selection, if any, which changes when the Alt key is
     * used during a selection. Can be null.
     */
    private CanvasAlternateSelection mAltSelection;

    /** List of clients listening to selection changes. */
    private final ListenerList mSelectionListeners = new ListenerList();

    /**
     * Constructs a new {@link SelectionManager} associated with the given layout canvas.
     *
     * @param layoutCanvas The layout canvas to create a {@link SelectionManager} for.
     */
    public SelectionManager(LayoutCanvas layoutCanvas) {
        mCanvas = layoutCanvas;
    }

    @Override
    public void addSelectionChangedListener(ISelectionChangedListener listener) {
        mSelectionListeners.add(listener);
    }

    @Override
    public void removeSelectionChangedListener(ISelectionChangedListener listener) {
        mSelectionListeners.remove(listener);
    }

    /**
     * Returns the native {@link SelectionItem} list.
     *
     * @return An immutable list of {@link SelectionItem}. Can be empty but not null.
     */
    @NonNull
    List<SelectionItem> getSelections() {
        return mUnmodifiableSelection;
    }

    /**
     * Return a snapshot/copy of the selection. Useful for clipboards etc where we
     * don't want the returned copy to be affected by future edits to the selection.
     *
     * @return A copy of the current selection. Never null.
     */
    @NonNull
    public List<SelectionItem> getSnapshot() {
        if (mSelectionListeners.isEmpty()) {
            return Collections.emptyList();
        }

        return new ArrayList<SelectionItem>(mSelections);
    }

    /**
     * Returns a {@link TreeSelection} where each {@link TreePath} item is
     * actually a {@link CanvasViewInfo}.
     */
    @Override
    public ISelection getSelection() {
        if (mSelections.isEmpty()) {
            return TreeSelection.EMPTY;
        }

        ArrayList<TreePath> paths = new ArrayList<TreePath>();

        for (SelectionItem cs : mSelections) {
            CanvasViewInfo vi = cs.getViewInfo();
            if (vi != null) {
                paths.add(getTreePath(vi));
            }
        }

        return new TreeSelection(paths.toArray(new TreePath[paths.size()]));
    }

    /**
     * Create a {@link TreePath} from the given view info
     *
     * @param viewInfo the view info to look up a tree path for
     * @return a {@link TreePath} for the given view info
     */
    public static TreePath getTreePath(CanvasViewInfo viewInfo) {
        ArrayList<Object> segments = new ArrayList<Object>();
        while (viewInfo != null) {
            segments.add(0, viewInfo);
            viewInfo = viewInfo.getParent();
        }

        return new TreePath(segments.toArray());
    }

    /**
     * Sets the selection. It must be an {@link ITreeSelection} where each segment
     * of the tree path is a {@link CanvasViewInfo}. A null selection is considered
     * as an empty selection.
     * <p/>
     * This method is invoked by {@link LayoutCanvasViewer#setSelection(ISelection)}
     * in response to an <em>outside</em> selection (compatible with ours) that has
     * changed. Typically it means the outline selection has changed and we're
     * synchronizing ours to match.
     */
    @Override
    public void setSelection(ISelection selection) {
        if (mInsideUpdateSelection) {
            return;
        }

        boolean changed = false;
        try {
            mInsideUpdateSelection = true;

            if (selection == null) {
                selection = TreeSelection.EMPTY;
            }

            if (selection instanceof ITreeSelection) {
                ITreeSelection treeSel = (ITreeSelection) selection;

                if (treeSel.isEmpty()) {
                    // Clear existing selection, if any
                    if (!mSelections.isEmpty()) {
                        mSelections.clear();
                        mAltSelection = null;
                        updateActionsFromSelection();
                        redraw();
                    }
                    return;
                }

                boolean redoLayout = false;

                // Create a list of all currently selected view infos
                Set<CanvasViewInfo> oldSelected = new HashSet<CanvasViewInfo>();
                for (SelectionItem cs : mSelections) {
                    oldSelected.add(cs.getViewInfo());
                }

                // Go thru new selection and take care of selecting new items
                // or marking those which are the same as in the current selection
                for (TreePath path : treeSel.getPaths()) {
                    Object seg = path.getLastSegment();
                    if (seg instanceof CanvasViewInfo) {
                        CanvasViewInfo newVi = (CanvasViewInfo) seg;
                        if (oldSelected.contains(newVi)) {
                            // This view info is already selected. Remove it from the
                            // oldSelected list so that we don't deselect it later.
                            oldSelected.remove(newVi);
                        } else {
                            // This view info is not already selected. Select it now.

                            // reset alternate selection if any
                            mAltSelection = null;
                            // otherwise add it.
                            mSelections.add(createSelection(newVi));
                            changed = true;
                        }
                        if (newVi.isInvisible()) {
                            redoLayout = true;
                        }
                    } else {
                        // Unrelated selection (e.g. user clicked in the Project Explorer
                        // or something) -- just ignore these
                        return;
                    }
                }

                // Deselect old selected items that are not in the new one
                for (CanvasViewInfo vi : oldSelected) {
                    if (vi.isExploded()) {
                        redoLayout = true;
                    }
                    deselect(vi);
                    changed = true;
                }

                if (redoLayout) {
                    mCanvas.getEditorDelegate().recomputeLayout();
                }
            }
        } finally {
            mInsideUpdateSelection = false;
        }

        if (changed) {
            redraw();
            fireSelectionChanged();
            updateActionsFromSelection();
        }
    }

    /**
     * The menu has been activated; ensure that the menu click is over the existing
     * selection, and if not, update the selection.
     *
     * @param e the {@link MenuDetectEvent} which triggered the menu
     */
    public void menuClick(MenuDetectEvent e) {
        LayoutPoint p = ControlPoint.create(mCanvas, e).toLayout();

        // Right click button is used to display a context menu.
        // If there's an existing selection and the click is anywhere in this selection
        // and there are no modifiers being used, we don't want to change the selection.
        // Otherwise we select the item under the cursor.

        for (SelectionItem cs : mSelections) {
            if (cs.isRoot()) {
                continue;
            }
            if (cs.getRect().contains(p.x, p.y)) {
                // The cursor is inside the selection. Don't change anything.
                return;
            }
        }

        CanvasViewInfo vi = mCanvas.getViewHierarchy().findViewInfoAt(p);
        selectSingle(vi);
    }

    /**
     * Performs selection for a mouse event.
     * <p/>
     * Shift key (or Command on the Mac) is used to toggle in multi-selection.
     * Alt key is used to cycle selection through objects at the same level than
     * the one pointed at (i.e. click on an object then alt-click to cycle).
     *
     * @param e The mouse event which triggered the selection. Cannot be null.
     *            The modifier key mask will be used to determine whether this
     *            is a plain select or a toggle, etc.
     */
    public void select(MouseEvent e) {
        boolean isMultiClick = (e.stateMask & SWT.SHIFT) != 0 ||
            // On Mac, the Command key is the normal toggle accelerator
            ((SdkConstants.CURRENT_PLATFORM == SdkConstants.PLATFORM_DARWIN) &&
                    (e.stateMask & SWT.COMMAND) != 0);
        boolean isCycleClick   = (e.stateMask & SWT.ALT)   != 0;

        LayoutPoint p = ControlPoint.create(mCanvas, e).toLayout();

        if (e.button == 3) {
            // Right click button is used to display a context menu.
            // If there's an existing selection and the click is anywhere in this selection
            // and there are no modifiers being used, we don't want to change the selection.
            // Otherwise we select the item under the cursor.

            if (!isCycleClick && !isMultiClick) {
                for (SelectionItem cs : mSelections) {
                    if (cs.getRect().contains(p.x, p.y)) {
                        // The cursor is inside the selection. Don't change anything.
                        return;
                    }
                }
            }

        } else if (e.button != 1) {
            // Click was done with something else than the left button for normal selection
            // or the right button for context menu.
            // We don't use mouse button 2 yet (middle mouse, or scroll wheel?) for
            // anything, so let's not change the selection.
            return;
        }

        CanvasViewInfo vi = mCanvas.getViewHierarchy().findViewInfoAt(p);

        if (vi != null && vi.isHidden()) {
            vi = vi.getParent();
        }

        if (isMultiClick && !isCycleClick) {
            // Case where shift is pressed: pointed object is toggled.

            // reset alternate selection if any
            mAltSelection = null;

            // If nothing has been found at the cursor, assume it might be a user error
            // and avoid clearing the existing selection.

            if (vi != null) {
                // toggle this selection on-off: remove it if already selected
                if (deselect(vi)) {
                    if (vi.isExploded()) {
                        mCanvas.getEditorDelegate().recomputeLayout();
                    }

                    redraw();
                    return;
                }

                // otherwise add it.
                mSelections.add(createSelection(vi));
                fireSelectionChanged();
                redraw();
            }

        } else if (isCycleClick) {
            // Case where alt is pressed: select or cycle the object pointed at.

            // Note: if shift and alt are pressed, shift is ignored. The alternate selection
            // mechanism does not reset the current multiple selection unless they intersect.

            // We need to remember the "origin" of the alternate selection, to be
            // able to continue cycling through it later. If there's no alternate selection,
            // create one. If there's one but not for the same origin object, create a new
            // one too.
            if (mAltSelection == null || mAltSelection.getOriginatingView() != vi) {
                mAltSelection = new CanvasAlternateSelection(
                        vi, mCanvas.getViewHierarchy().findAltViewInfoAt(p));

                // deselect them all, in case they were partially selected
                deselectAll(mAltSelection.getAltViews());

                // select the current one
                CanvasViewInfo vi2 = mAltSelection.getCurrent();
                if (vi2 != null) {
                    mSelections.addFirst(createSelection(vi2));
                    fireSelectionChanged();
                }
            } else {
                // We're trying to cycle through the current alternate selection.
                // First remove the current object.
                CanvasViewInfo vi2 = mAltSelection.getCurrent();
                deselect(vi2);

                // Now select the next one.
                vi2 = mAltSelection.getNext();
                if (vi2 != null) {
                    mSelections.addFirst(createSelection(vi2));
                    fireSelectionChanged();
                }
            }
            redraw();

        } else {
            // Case where no modifier is pressed: either select or reset the selection.
            selectSingle(vi);
        }
    }

    /**
     * Removes all the currently selected item and only select the given item.
     * Issues a redraw() if the selection changes.
     *
     * @param vi The new selected item if non-null. Selection becomes empty if null.
     * @return the item selected, or null if the selection was cleared (e.g. vi was null)
     */
    @Nullable
    SelectionItem selectSingle(CanvasViewInfo vi) {
        SelectionItem item = null;

        // reset alternate selection if any
        mAltSelection = null;

        if (vi == null) {
            // The user clicked outside the bounds of the root element; in that case, just
            // select the root element.
            vi = mCanvas.getViewHierarchy().getRoot();
        }

        boolean redoLayout = hasExplodedItems();

        // reset (multi)selection if any
        if (!mSelections.isEmpty()) {
            if (mSelections.size() == 1 && mSelections.getFirst().getViewInfo() == vi) {
                // CanvasSelection remains the same, don't touch it.
                return mSelections.getFirst();
            }
            mSelections.clear();
        }

        if (vi != null) {
            item = createSelection(vi);
            mSelections.add(item);
            if (vi.isInvisible()) {
                redoLayout = true;
            }
        }
        fireSelectionChanged();

        if (redoLayout) {
            mCanvas.getEditorDelegate().recomputeLayout();
        }

        redraw();

        return item;
    }

    /** Returns true if the view hierarchy is showing exploded items. */
    private boolean hasExplodedItems() {
        for (SelectionItem item : mSelections) {
            if (item.getViewInfo().isExploded()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Selects the given set of {@link CanvasViewInfo}s. This is similar to
     * {@link #selectSingle} but allows you to make a multi-selection. Issues a
     * {@link #redraw()}.
     *
     * @param viewInfos A collection of {@link CanvasViewInfo} objects to be
     *            selected, or null or empty to clear the selection.
     */
    /* package */ void selectMultiple(Collection<CanvasViewInfo> viewInfos) {
        // reset alternate selection if any
        mAltSelection = null;

        boolean redoLayout = hasExplodedItems();

        mSelections.clear();
        if (viewInfos != null) {
            for (CanvasViewInfo viewInfo : viewInfos) {
                mSelections.add(createSelection(viewInfo));
                if (viewInfo.isInvisible()) {
                    redoLayout = true;
                }
            }
        }

        fireSelectionChanged();

        if (redoLayout) {
            mCanvas.getEditorDelegate().recomputeLayout();
        }

        redraw();
    }

    public void select(Collection<INode> nodes) {
        List<CanvasViewInfo> infos = new ArrayList<CanvasViewInfo>(nodes.size());
        for (INode node : nodes) {
            CanvasViewInfo info = mCanvas.getViewHierarchy().findViewInfoFor(node);
            if (info != null) {
                infos.add(info);
            }
        }
        selectMultiple(infos);
    }

    /**
     * Selects the visual element corresponding to the given XML node
     * @param xmlNode The Node whose element we want to select.
     */
    /* package */ void select(Node xmlNode) {
        if (xmlNode == null) {
            return;
        } else if (xmlNode.getNodeType() == Node.TEXT_NODE) {
            xmlNode = xmlNode.getParentNode();
        }

        CanvasViewInfo vi = mCanvas.getViewHierarchy().findViewInfoFor(xmlNode);
        if (vi != null && !vi.isRoot()) {
            selectSingle(vi);
        }
    }

    /**
     * Selects any views that overlap the given selection rectangle.
     *
     * @param topLeft The top left corner defining the selection rectangle.
     * @param bottomRight The bottom right corner defining the selection
     *            rectangle.
     * @param toggled A set of {@link CanvasViewInfo}s that should be toggled
     *            rather than just added.
     */
    public void selectWithin(LayoutPoint topLeft, LayoutPoint bottomRight,
            Collection<CanvasViewInfo> toggled) {
        // reset alternate selection if any
        mAltSelection = null;

        ViewHierarchy viewHierarchy = mCanvas.getViewHierarchy();
        Collection<CanvasViewInfo> viewInfos = viewHierarchy.findWithin(topLeft, bottomRight);

        if (toggled.size() > 0) {
            // Copy; we're not allowed to touch the passed in collection
            Set<CanvasViewInfo> result = new HashSet<CanvasViewInfo>(toggled);
            for (CanvasViewInfo viewInfo : viewInfos) {
                if (toggled.contains(viewInfo)) {
                    result.remove(viewInfo);
                } else {
                    result.add(viewInfo);
                }
            }
            viewInfos = result;
        }

        mSelections.clear();
        for (CanvasViewInfo viewInfo : viewInfos) {
            if (viewInfo.isHidden()) {
                continue;
            }
            mSelections.add(createSelection(viewInfo));
        }

        fireSelectionChanged();
        redraw();
    }

    /**
     * Clears the selection and then selects everything (all views and all their
     * children).
     */
    public void selectAll() {
        // First clear the current selection, if any.
        mSelections.clear();
        mAltSelection = null;

        // Now select everything if there's a valid layout
        for (CanvasViewInfo vi : mCanvas.getViewHierarchy().findAllViewInfos(false)) {
            mSelections.add(createSelection(vi));
        }

        fireSelectionChanged();
        redraw();
    }

    /** Clears the selection */
    public void selectNone() {
        mSelections.clear();
        mAltSelection = null;
        fireSelectionChanged();
        redraw();
    }

    /** Selects the parent of the current selection */
    public void selectParent() {
        if (mSelections.size() == 1) {
            CanvasViewInfo parent = mSelections.get(0).getViewInfo().getParent();
            if (parent != null) {
                selectSingle(parent);
            }
        }
    }

    /** Finds all widgets in the layout that have the same type as the primary */
    public void selectSameType() {
        // Find all
        if (mSelections.size() == 1) {
            CanvasViewInfo viewInfo = mSelections.get(0).getViewInfo();
            ElementDescriptor descriptor = viewInfo.getUiViewNode().getDescriptor();
            mSelections.clear();
            mAltSelection = null;
            addSameType(mCanvas.getViewHierarchy().getRoot(), descriptor);
            fireSelectionChanged();
            redraw();
        }
    }

    /** Helper for {@link #selectSameType} */
    private void addSameType(CanvasViewInfo root, ElementDescriptor descriptor) {
        if (root.getUiViewNode().getDescriptor() == descriptor) {
            mSelections.add(createSelection(root));
        }

        for (CanvasViewInfo child : root.getChildren()) {
            addSameType(child, descriptor);
        }
    }

    /** Selects the siblings of the primary */
    public void selectSiblings() {
        // Find all
        if (mSelections.size() == 1) {
            CanvasViewInfo vi = mSelections.get(0).getViewInfo();
            mSelections.clear();
            mAltSelection = null;
            CanvasViewInfo parent = vi.getParent();
            if (parent == null) {
                selectNone();
            } else {
                for (CanvasViewInfo child : parent.getChildren()) {
                    mSelections.add(createSelection(child));
                }
                fireSelectionChanged();
                redraw();
            }
        }
    }

    /**
     * Returns true if and only if there is currently more than one selected
     * item.
     *
     * @return True if more than one item is selected
     */
    public boolean hasMultiSelection() {
        return mSelections.size() > 1;
    }

    /**
     * Deselects a view info. Returns true if the object was actually selected.
     * Callers are responsible for calling redraw() and updateOulineSelection()
     * after.
     * @param canvasViewInfo The item to deselect.
     * @return  True if the object was successfully removed from the selection.
     */
    public boolean deselect(CanvasViewInfo canvasViewInfo) {
        if (canvasViewInfo == null) {
            return false;
        }

        for (ListIterator<SelectionItem> it = mSelections.listIterator(); it.hasNext(); ) {
            SelectionItem s = it.next();
            if (canvasViewInfo == s.getViewInfo()) {
                it.remove();
                return true;
            }
        }

        return false;
    }

    /**
     * Deselects multiple view infos.
     * Callers are responsible for calling redraw() and updateOulineSelection() after.
     */
    private void deselectAll(List<CanvasViewInfo> canvasViewInfos) {
        for (ListIterator<SelectionItem> it = mSelections.listIterator(); it.hasNext(); ) {
            SelectionItem s = it.next();
            if (canvasViewInfos.contains(s.getViewInfo())) {
                it.remove();
            }
        }
    }

    /** Sync the selection with an updated view info tree */
    void sync() {
        // Check if the selection is still the same (based on the object keys)
        // and eventually recompute their bounds.
        for (ListIterator<SelectionItem> it = mSelections.listIterator(); it.hasNext(); ) {
            SelectionItem s = it.next();

            // Check if the selected object still exists
            ViewHierarchy viewHierarchy = mCanvas.getViewHierarchy();
            UiViewElementNode key = s.getViewInfo().getUiViewNode();
            CanvasViewInfo vi = viewHierarchy.findViewInfoFor(key);

            // Remove the previous selection -- if the selected object still exists
            // we need to recompute its bounds in case it moved so we'll insert a new one
            // at the same place.
            it.remove();
            if (vi == null) {
                vi = findCorresponding(s.getViewInfo(), viewHierarchy.getRoot());
            }
            if (vi != null) {
                it.add(createSelection(vi));
            }
        }
        fireSelectionChanged();

        // remove the current alternate selection views
        mAltSelection = null;
    }

    /** Finds the corresponding {@link CanvasViewInfo} in the new hierarchy */
    private CanvasViewInfo findCorresponding(CanvasViewInfo old, CanvasViewInfo newRoot) {
        CanvasViewInfo oldParent = old.getParent();
        if (oldParent != null) {
            CanvasViewInfo newParent = findCorresponding(oldParent, newRoot);
            if (newParent == null) {
                return null;
            }

            List<CanvasViewInfo> oldSiblings = oldParent.getChildren();
            List<CanvasViewInfo> newSiblings = newParent.getChildren();
            Iterator<CanvasViewInfo> oldIterator = oldSiblings.iterator();
            Iterator<CanvasViewInfo> newIterator = newSiblings.iterator();
            while (oldIterator.hasNext() && newIterator.hasNext()) {
                CanvasViewInfo oldSibling = oldIterator.next();
                CanvasViewInfo newSibling = newIterator.next();

                if (oldSibling.getName().equals(newSibling.getName())) {
                    // Structure has changed: can't do a proper search
                    return null;
                }

                if (oldSibling == old) {
                    return newSibling;
                }
            }
        } else {
            return newRoot;
        }

        return null;
    }

    /**
     * Notifies listeners that the selection has changed.
     */
    private void fireSelectionChanged() {
        if (mInsideUpdateSelection) {
            return;
        }
        try {
            mInsideUpdateSelection = true;

            final SelectionChangedEvent event = new SelectionChangedEvent(this, getSelection());

            SafeRunnable.run(new SafeRunnable() {
                @Override
                public void run() {
                    for (Object listener : mSelectionListeners.getListeners()) {
                        ((ISelectionChangedListener) listener).selectionChanged(event);
                    }
                }
            });

            updateActionsFromSelection();
        } finally {
            mInsideUpdateSelection = false;
        }
    }

    /**
     * Updates menu actions and the layout action bar after a selection change - these are
     * actions that depend on the selection
     */
    private void updateActionsFromSelection() {
        LayoutEditorDelegate editor = mCanvas.getEditorDelegate();
        if (editor != null) {
            // Update menu actions that depend on the selection
            mCanvas.updateMenuActionState();

            // Update the layout actions bar
            LayoutActionBar layoutActionBar = editor.getGraphicalEditor().getLayoutActionBar();
            layoutActionBar.updateSelection();
        }
    }

    /**
     * Sanitizes the selection for a copy/cut or drag operation.
     * <p/>
     * Sanitizes the list to make sure all elements have a valid XML attached to it,
     * that is remove element that have no XML to avoid having to make repeated such
     * checks in various places after.
     * <p/>
     * In case of multiple selection, we also need to remove all children when their
     * parent is already selected since parents will always be added with all their
     * children.
     * <p/>
     *
     * @param selection The selection list to be sanitized <b>in-place</b>.
     *      The <code>selection</code> argument should not be {@link #mSelections} -- the
     *      given list is going to be altered and we should never alter the user-made selection.
     *      Instead the caller should provide its own copy.
     */
    /* package */ static void sanitize(List<SelectionItem> selection) {
        if (selection.isEmpty()) {
            return;
        }

        for (Iterator<SelectionItem> it = selection.iterator(); it.hasNext(); ) {
            SelectionItem cs = it.next();
            CanvasViewInfo vi = cs.getViewInfo();
            UiViewElementNode key = vi == null ? null : vi.getUiViewNode();
            Node node = key == null ? null : key.getXmlNode();
            if (node == null) {
                // Missing ViewInfo or view key or XML, discard this.
                it.remove();
                continue;
            }

            if (vi != null) {
                for (Iterator<SelectionItem> it2 = selection.iterator();
                     it2.hasNext(); ) {
                    SelectionItem cs2 = it2.next();
                    if (cs != cs2) {
                        CanvasViewInfo vi2 = cs2.getViewInfo();
                        if (vi.isParent(vi2)) {
                            // vi2 is a parent for vi. Remove vi.
                            it.remove();
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * Selects the given list of nodes in the canvas, and returns true iff the
     * attempt to select was successful.
     *
     * @param nodes The collection of nodes to be selected
     * @param indices A list of indices within the parent for each node, or null
     * @return True if and only if all nodes were successfully selected
     */
    public boolean selectDropped(List<INode> nodes, List<Integer> indices) {
        assert indices == null || nodes.size() == indices.size();

        ViewHierarchy viewHierarchy = mCanvas.getViewHierarchy();

        // Look up a list of view infos which correspond to the nodes.
        final Collection<CanvasViewInfo> newChildren = new ArrayList<CanvasViewInfo>();
        for (int i = 0, n = nodes.size(); i < n; i++) {
            INode node = nodes.get(i);

            CanvasViewInfo viewInfo = viewHierarchy.findViewInfoFor(node);

            // There are two scenarios where looking up a view info fails.
            // The first one is that the node was just added and the render has not yet
            // happened, so the ViewHierarchy has no record of the node. In this case
            // there is nothing we can do, and the method will return false (which the
            // caller will use to schedule a second attempt later).
            // The second scenario is where the nodes *change identity*. This isn't
            // common, but when a drop handler makes a lot of changes to its children,
            // for example when dropping into a GridLayout where attributes are adjusted
            // on nearly all the other children to update row or column attributes
            // etc, then in some cases Eclipse's DOM model changes the identities of
            // the nodes when applying all the edits, so the new Node we created (as
            // well as possibly other nodes) are no longer the children we observe
            // after the edit, and there are new copies there instead. In this case
            // the UiViewModel also fails to map the nodes. To work around this,
            // we track the *indices* (within the parent) during a drop, such that we
            // know which children (according to their positions) the given nodes
            // are supposed to map to, and then we use these view infos instead.
            if (viewInfo == null && node instanceof NodeProxy && indices != null) {
                INode parent = node.getParent();
                CanvasViewInfo parentViewInfo = viewHierarchy.findViewInfoFor(parent);
                if (parentViewInfo != null) {
                    UiViewElementNode parentUiNode = parentViewInfo.getUiViewNode();
                    if (parentUiNode != null) {
                        List<UiElementNode> children = parentUiNode.getUiChildren();
                        int index = indices.get(i);
                        if (index >= 0 && index < children.size()) {
                            UiElementNode replacedNode = children.get(index);
                            viewInfo = viewHierarchy.findViewInfoFor(replacedNode);
                        }
                    }
                }
            }

            if (viewInfo != null) {
                if (nodes.size() > 1 && viewInfo.isHidden()) {
                    // Skip spacers - unless you're dropping just one
                    continue;
                }
                if (GridLayoutRule.sDebugGridLayout && (viewInfo.getName().equals(FQCN_SPACE)
                        || viewInfo.getName().equals(FQCN_SPACE_V7))) {
                    // In debug mode they might not be marked as hidden but we never never
                    // want to select these guys
                    continue;
                }
                newChildren.add(viewInfo);
            }
        }
        boolean found = nodes.size() == newChildren.size();

        if (found || newChildren.size() > 0) {
            mCanvas.getSelectionManager().selectMultiple(newChildren);
        }

        return found;
    }

    /**
     * Update the outline selection to select the given nodes, asynchronously.
     * @param nodes The nodes to be selected
     */
    public void setOutlineSelection(final List<INode> nodes) {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                selectDropped(nodes, null /* indices */);
                syncOutlineSelection();
            }
        });
    }

    /**
     * Syncs the current selection to the outline, synchronously.
     */
    public void syncOutlineSelection() {
        OutlinePage outlinePage = mCanvas.getOutlinePage();
        IWorkbenchPartSite site = outlinePage.getEditor().getSite();
        ISelectionProvider selectionProvider = site.getSelectionProvider();
        ISelection selection = selectionProvider.getSelection();
        if (selection != null) {
            outlinePage.setSelection(selection);
        }
    }

    private void redraw() {
        mCanvas.redraw();
    }

    SelectionItem createSelection(CanvasViewInfo vi) {
        return new SelectionItem(mCanvas, vi);
    }

    /**
     * Returns true if there is nothing selected
     *
     * @return true if there is nothing selected
     */
    public boolean isEmpty() {
        return mSelections.size() == 0;
    }

    /**
     * "Select" context menu which lists various menu options related to selection:
     * <ul>
     * <li> Select All
     * <li> Select Parent
     * <li> Select None
     * <li> Select Siblings
     * <li> Select Same Type
     * </ul>
     * etc.
     */
    public static class SelectionMenu extends SubmenuAction {
        private final GraphicalEditorPart mEditor;

        public SelectionMenu(GraphicalEditorPart editor) {
            super("Select");
            mEditor = editor;
        }

        @Override
        public String getId() {
            return "-selectionmenu"; //$NON-NLS-1$
        }

        @Override
        protected void addMenuItems(Menu menu) {
            LayoutCanvas canvas = mEditor.getCanvasControl();
            SelectionManager selectionManager = canvas.getSelectionManager();
            List<SelectionItem> selections = selectionManager.getSelections();
            boolean selectedOne = selections.size() == 1;
            boolean notRoot = selectedOne && !selections.get(0).isRoot();
            boolean haveSelection = selections.size() > 0;

            Action a;
            a = selectionManager.new SelectAction("Select Parent\tEsc", SELECT_PARENT);
            new ActionContributionItem(a).fill(menu, -1);
            a.setEnabled(notRoot);
            a.setAccelerator(SWT.ESC);

            a = selectionManager.new SelectAction("Select Siblings", SELECT_SIBLINGS);
            new ActionContributionItem(a).fill(menu, -1);
            a.setEnabled(notRoot);

            a = selectionManager.new SelectAction("Select Same Type", SELECT_SAME_TYPE);
            new ActionContributionItem(a).fill(menu, -1);
            a.setEnabled(selectedOne);

            new Separator().fill(menu, -1);

            // Special case for Select All: Use global action
            a = canvas.getSelectAllAction();
            new ActionContributionItem(a).fill(menu, -1);
            a.setEnabled(true);

            a = selectionManager.new SelectAction("Deselect All", SELECT_NONE);
            new ActionContributionItem(a).fill(menu, -1);
            a.setEnabled(haveSelection);
        }
    }

    private static final int SELECT_PARENT = 1;
    private static final int SELECT_SIBLINGS = 2;
    private static final int SELECT_SAME_TYPE = 3;
    private static final int SELECT_NONE = 4; // SELECT_ALL is handled separately

    private class SelectAction extends Action {
        private final int mType;

        public SelectAction(String title, int type) {
            super(title, IAction.AS_PUSH_BUTTON);
            mType = type;
        }

        @Override
        public void run() {
            switch (mType) {
                case SELECT_NONE:
                    selectNone();
                    break;
                case SELECT_PARENT:
                    selectParent();
                    break;
                case SELECT_SAME_TYPE:
                    selectSameType();
                    break;
                case SELECT_SIBLINGS:
                    selectSiblings();
                    break;
            }

            List<INode> nodes = new ArrayList<INode>();
            for (SelectionItem item : getSelections()) {
                nodes.add(item.getNode());
            }
            setOutlineSelection(nodes);
        }
    }

    public Pair<SelectionItem, SelectionHandle> findHandle(ControlPoint controlPoint) {
        if (!isEmpty()) {
            LayoutPoint layoutPoint = controlPoint.toLayout();
            int distance = (int) ((PIXEL_MARGIN + PIXEL_RADIUS) / mCanvas.getScale());

            for (SelectionItem item : getSelections()) {
                SelectionHandles handles = item.getSelectionHandles();
                // See if it's over the selection handles
                SelectionHandle handle = handles.findHandle(layoutPoint, distance);
                if (handle != null) {
                    return Pair.of(item, handle);
                }
            }

        }
        return null;
    }

    /** Performs the default action provided by the currently selected view */
    public void performDefaultAction() {
        final List<SelectionItem> selections = getSelections();
        if (selections.size() > 0) {
            NodeProxy primary = selections.get(0).getNode();
            if (primary != null) {
                RulesEngine rulesEngine = mCanvas.getRulesEngine();
                final String id = rulesEngine.callGetDefaultActionId(primary);
                if (id == null) {
                    return;
                }
                final List<RuleAction> actions = rulesEngine.callGetContextMenu(primary);
                if (actions == null) {
                    return;
                }
                RuleAction matching = null;
                for (RuleAction a : actions) {
                    if (id.equals(a.getId())) {
                        matching = a;
                        break;
                    }
                }
                if (matching == null) {
                    return;
                }
                final List<INode> selectedNodes = new ArrayList<INode>();
                for (SelectionItem item : selections) {
                    NodeProxy n = item.getNode();
                    if (n != null) {
                        selectedNodes.add(n);
                    }
                }
                final RuleAction action = matching;
                mCanvas.getEditorDelegate().getEditor().wrapUndoEditXmlModel(action.getTitle(),
                    new Runnable() {
                        @Override
                        public void run() {
                            action.getCallback().action(action, selectedNodes,
                                    action.getId(), null);
                            LayoutCanvas canvas = mCanvas;
                            CanvasViewInfo root = canvas.getViewHierarchy().getRoot();
                            if (root != null) {
                                UiViewElementNode uiViewNode = root.getUiViewNode();
                                NodeFactory nodeFactory = canvas.getNodeFactory();
                                NodeProxy rootNode = nodeFactory.create(uiViewNode);
                                if (rootNode != null) {
                                    rootNode.applyPendingChanges();
                                }
                            }
                        }
                });
            }
        }
    }

    /** Performs renaming the selected views */
    public void performRename() {
        final List<SelectionItem> selections = getSelections();
        if (selections.size() > 0) {
            NodeProxy primary = selections.get(0).getNode();
            if (primary != null) {
                performRename(primary, selections);
            }
        }
    }

    /**
     * Performs renaming the given node.
     *
     * @param primary the node to be renamed, or the primary node (to get the
     *            current value from if more than one node should be renamed)
     * @param selections if not null, a list of nodes to apply the setting to
     *            (which should include the primary)
     * @return the result of the renaming operation
     */
    @NonNull
    public RenameResult performRename(
            final @NonNull INode primary,
            final @Nullable List<SelectionItem> selections) {
        String id = primary.getStringAttr(ANDROID_URI, ATTR_ID);
        if (id != null && !id.isEmpty()) {
            RenameResult result = RenameResourceWizard.renameResource(
                    mCanvas.getShell(),
                    mCanvas.getEditorDelegate().getGraphicalEditor().getProject(),
                    ResourceType.ID, BaseViewRule.stripIdPrefix(id), null, true /*canClear*/);
            if (result.isCanceled()) {
                return result;
            } else if (!result.isUnavailable()) {
                return result;
            }
        }
        String currentId = primary.getStringAttr(ANDROID_URI, ATTR_ID);
        currentId = BaseViewRule.stripIdPrefix(currentId);
        InputDialog d = new InputDialog(
                    AdtPlugin.getDisplay().getActiveShell(),
                    "Set ID",
                    "New ID:",
                    currentId,
                    ResourceNameValidator.create(false, (IProject) null, ResourceType.ID));
        if (d.open() == Window.OK) {
            final String s = d.getValue();
            mCanvas.getEditorDelegate().getEditor().wrapUndoEditXmlModel("Set ID",
                    new Runnable() {
                @Override
                public void run() {
                    String newId = s;
                    newId = NEW_ID_PREFIX + BaseViewRule.stripIdPrefix(s);
                    if (selections != null) {
                        for (SelectionItem item : selections) {
                            NodeProxy node = item.getNode();
                            if (node != null) {
                                node.setAttribute(ANDROID_URI, ATTR_ID, newId);
                            }
                        }
                    } else {
                        primary.setAttribute(ANDROID_URI, ATTR_ID, newId);
                    }

                    LayoutCanvas canvas = mCanvas;
                    CanvasViewInfo root = canvas.getViewHierarchy().getRoot();
                    if (root != null) {
                        UiViewElementNode uiViewNode = root.getUiViewNode();
                        NodeFactory nodeFactory = canvas.getNodeFactory();
                        NodeProxy rootNode = nodeFactory.create(uiViewNode);
                        if (rootNode != null) {
                            rootNode.applyPendingChanges();
                        }
                    }
                }
            });
            return RenameResult.name(BaseViewRule.stripIdPrefix(s));
        } else {
            return RenameResult.canceled();
        }
    }
}

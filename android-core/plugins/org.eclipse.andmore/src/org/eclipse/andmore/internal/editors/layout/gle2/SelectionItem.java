/*
 * Copyright (C) 2009 The Android Open Source Project
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

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.api.ResizePolicy;

import org.eclipse.andmore.internal.editors.layout.LayoutEditorDelegate;
import org.eclipse.andmore.internal.editors.layout.gre.NodeProxy;
import org.eclipse.andmore.internal.editors.layout.gre.ViewMetadataRepository;
import org.eclipse.andmore.internal.editors.layout.uimodel.UiViewElementNode;
import org.eclipse.swt.graphics.Rectangle;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents one selection in {@link LayoutCanvas}.
 */
class SelectionItem {

    /** The associated {@link LayoutCanvas} */
    private LayoutCanvas mCanvas;

    /** Current selected view info. Can be null. */
    private final CanvasViewInfo mCanvasViewInfo;

    /** Current selection border rectangle. Null when mCanvasViewInfo is null . */
    private final Rectangle mRect;

    /** The node proxy for drawing the selection. Null when mCanvasViewInfo is null. */
    private final NodeProxy mNodeProxy;

    /** The resize policy for this selection item */
    private ResizePolicy mResizePolicy;

    /** The selection handles for this item */
    private SelectionHandles mHandles;

    /**
     * Creates a new {@link SelectionItem} object.
     * @param canvas the associated canvas
     * @param canvasViewInfo The view info being selected. Must not be null.
     */
    public SelectionItem(LayoutCanvas canvas, CanvasViewInfo canvasViewInfo) {
        assert canvasViewInfo != null;

        mCanvas = canvas;
        mCanvasViewInfo = canvasViewInfo;

        if (canvasViewInfo == null) {
            mRect = null;
            mNodeProxy = null;
        } else {
            Rectangle r = canvasViewInfo.getSelectionRect();
            mRect = new Rectangle(r.x, r.y, r.width, r.height);
            mNodeProxy = mCanvas.getNodeFactory().create(canvasViewInfo);
        }
    }

    /**
     * Returns true when this selection item represents the root, the top level
     * layout element in the editor.
     *
     * @return True if and only if this element is at the root of the hierarchy
     */
    public boolean isRoot() {
        return mCanvasViewInfo.isRoot();
    }

    /**
     * Returns true if this item represents a widget that should not be manipulated by the
     * user.
     *
     * @return True if this widget should not be manipulated directly by the user
     */
    public boolean isHidden() {
        return mCanvasViewInfo.isHidden();
    }

    /**
     * Returns the selected view info. Cannot be null.
     *
     * @return the selected view info. Cannot be null.
     */
    @NonNull
    public CanvasViewInfo getViewInfo() {
        return mCanvasViewInfo;
    }

    /**
     * Returns the selected node.
     *
     * @return the selected node, or null
     */
    @Nullable
    public UiViewElementNode getUiNode() {
        return mCanvasViewInfo.getUiViewNode();
    }

    /**
     * Returns the selection border rectangle. Cannot be null.
     *
     * @return the selection border rectangle, never null
     */
    public Rectangle getRect() {
        return mRect;
    }

    /** Returns the node associated with this selection (may be null) */
    @Nullable
    NodeProxy getNode() {
        return mNodeProxy;
    }

    /** Returns the canvas associated with this selection (never null) */
    @NonNull
    LayoutCanvas getCanvas() {
        return mCanvas;
    }

    //----

    /**
     * Gets the XML text from the given selection for a text transfer.
     * The returned string can be empty but not null.
     */
    @NonNull
    static String getAsText(LayoutCanvas canvas, List<SelectionItem> selection) {
        StringBuilder sb = new StringBuilder();

        LayoutEditorDelegate layoutEditorDelegate = canvas.getEditorDelegate();
        for (SelectionItem cs : selection) {
            CanvasViewInfo vi = cs.getViewInfo();
            UiViewElementNode key = vi.getUiViewNode();
            Node node = key.getXmlNode();
            String t = layoutEditorDelegate.getEditor().getXmlText(node);
            if (t != null) {
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append(t);
            }
        }

        return sb.toString();
    }

    /**
     * Returns elements representing the given selection of canvas items.
     *
     * @param items Items to wrap in elements
     * @return An array of wrapper elements. Never null.
     */
    @NonNull
    static SimpleElement[] getAsElements(@NonNull List<SelectionItem> items) {
        return getAsElements(items, null);
    }

    /**
     * Returns elements representing the given selection of canvas items.
     *
     * @param items Items to wrap in elements
     * @param primary The primary selected item which should be listed first
     * @return An array of wrapper elements. Never null.
     */
    @NonNull
    static SimpleElement[] getAsElements(
            @NonNull List<SelectionItem> items,
            @Nullable SelectionItem primary) {
        List<SimpleElement> elements = new ArrayList<SimpleElement>();

        if (primary != null) {
            CanvasViewInfo vi = primary.getViewInfo();
            SimpleElement e = vi.toSimpleElement();
            e.setSelectionItem(primary);
            elements.add(e);
        }

        for (SelectionItem cs : items) {
            if (cs == primary) {
                // Already handled
                continue;
            }

            CanvasViewInfo vi = cs.getViewInfo();
            SimpleElement e = vi.toSimpleElement();
            e.setSelectionItem(cs);
            elements.add(e);
        }

        return elements.toArray(new SimpleElement[elements.size()]);
    }

    /**
     * Returns true if this selection item is a layout
     *
     * @return true if this selection item is a layout
     */
    public boolean isLayout() {
        UiViewElementNode node = mCanvasViewInfo.getUiViewNode();
        if (node != null) {
            return node.getDescriptor().hasChildren();
        } else {
            return false;
        }
    }

    /**
     * Returns the {@link SelectionHandles} for this {@link SelectionItem}. Never null.
     *
     * @return the {@link SelectionHandles} for this {@link SelectionItem}, never null
     */
    @NonNull
    public SelectionHandles getSelectionHandles() {
        if (mHandles == null) {
            mHandles = new SelectionHandles(this);
        }

        return mHandles;
    }

    /**
     * Returns the {@link ResizePolicy} for this item
     *
     * @return the {@link ResizePolicy} for this item, never null
     */
    @NonNull
    public ResizePolicy getResizePolicy() {
        if (mResizePolicy == null && mNodeProxy != null) {
            mResizePolicy = ViewMetadataRepository.get().getResizePolicy(mNodeProxy.getFqcn());
        }

        return mResizePolicy;
    }
}

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

import com.android.ide.common.api.Margins;
import com.android.ide.common.api.Rect;
import com.android.ide.common.api.ResizePolicy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.andmore.internal.editors.layout.gle2.SelectionHandle.Position;
import org.eclipse.andmore.internal.editors.layout.gre.NodeProxy;

/**
 * The {@link SelectionHandles} of a {@link SelectionItem} are the set of
 * {@link SelectionHandle} objects (possibly empty, for non-resizable objects) the user
 * can manipulate to resize a widget.
 */
public class SelectionHandles implements Iterable<SelectionHandle> {
    private final SelectionItem mItem;
    private List<SelectionHandle> mHandles;

    /**
     * Constructs a new {@link SelectionHandles} object for the given {link
     * {@link SelectionItem}
     * @param item the item to create {@link SelectionHandles} for
     */
    public SelectionHandles(SelectionItem item) {
        mItem = item;

        createHandles(item.getCanvas());
    }

    /**
     * Find a specific {@link SelectionHandle} from this set of {@link SelectionHandles},
     * which is within the given distance (in layout coordinates) from the center of the
     * {@link SelectionHandle}.
     *
     * @param point the mouse position (in layout coordinates) to test
     * @param distance the maximum distance from the handle center to accept
     * @return a {@link SelectionHandle} under the point, or null if not found
     */
    public SelectionHandle findHandle(LayoutPoint point, int distance) {
        for (SelectionHandle handle : mHandles) {
            if (handle.contains(point, distance)) {
                return handle;
            }
        }

        return null;
    }

    /**
     * Create the {@link SelectionHandle} objects for the selection item, according to its
     * {@link ResizePolicy}.
     */
    private void createHandles(LayoutCanvas canvas) {
        NodeProxy selectedNode = mItem.getNode();
        Rect r = selectedNode.getBounds();
        if (!r.isValid()) {
            mHandles = Collections.emptyList();
            return;
        }

        ResizePolicy resizability = mItem.getResizePolicy();
        if (resizability.isResizable()) {
            mHandles = new ArrayList<SelectionHandle>(8);
            boolean left = resizability.leftAllowed();
            boolean right = resizability.rightAllowed();
            boolean top = resizability.topAllowed();
            boolean bottom = resizability.bottomAllowed();
            int x1 = r.x;
            int y1 = r.y;
            int w = r.w;
            int h = r.h;
            int x2 = x1 + w;
            int y2 = y1 + h;

            Margins insets = canvas.getInsets(mItem.getNode().getFqcn());
            if (insets != null) {
                x1 += insets.left;
                x2 -= insets.right;
                y1 += insets.top;
                y2 -= insets.bottom;
            }

            int mx = (x1 + x2) / 2;
            int my = (y1 + y2) / 2;

            if (left) {
                mHandles.add(new SelectionHandle(x1, my, Position.LEFT_MIDDLE));
                if (top) {
                    mHandles.add(new SelectionHandle(x1, y1, Position.TOP_LEFT));
                }
                if (bottom) {
                    mHandles.add(new SelectionHandle(x1, y2, Position.BOTTOM_LEFT));
                }
            }
            if (right) {
                mHandles.add(new SelectionHandle(x2, my, Position.RIGHT_MIDDLE));
                if (top) {
                    mHandles.add(new SelectionHandle(x2, y1, Position.TOP_RIGHT));
                }
                if (bottom) {
                    mHandles.add(new SelectionHandle(x2, y2, Position.BOTTOM_RIGHT));
                }
            }
            if (top) {
                mHandles.add(new SelectionHandle(mx, y1, Position.TOP_MIDDLE));
            }
            if (bottom) {
                mHandles.add(new SelectionHandle(mx, y2, Position.BOTTOM_MIDDLE));
            }
        } else {
            mHandles = Collections.emptyList();
        }
    }

    // Implements Iterable<SelectionHandle>
    @Override
    public Iterator<SelectionHandle> iterator() {
        return mHandles.iterator();
    }
}

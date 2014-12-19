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
package org.eclipse.andmore.internal.editors.layout.gle2;

import static org.eclipse.andmore.internal.editors.layout.gle2.LintOverlay.ICON_SIZE;

import com.android.annotations.Nullable;

import org.eclipse.andmore.internal.editors.layout.LayoutEditorDelegate;
import org.eclipse.andmore.internal.editors.layout.uimodel.UiViewElementNode;
import org.eclipse.andmore.internal.preferences.AdtPrefs;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Tooltip in the layout editor showing lint errors under the cursor */
class LintTooltipManager implements Listener {
    private final LayoutCanvas mCanvas;
    private Shell mTip = null;
    private List<UiViewElementNode> mShowingNodes;

    /**
     * Sets up a custom tooltip when hovering over tree items. It currently displays the error
     * message for the lint warning associated with each node, if any (and only if the hover
     * is over the icon portion).
     */
    LintTooltipManager(LayoutCanvas canvas) {
        mCanvas = canvas;
    }

    void register() {
        mCanvas.addListener(SWT.Dispose, this);
        mCanvas.addListener(SWT.KeyDown, this);
        mCanvas.addListener(SWT.MouseMove, this);
        mCanvas.addListener(SWT.MouseHover, this);
    }

    void unregister() {
        if (!mCanvas.isDisposed()) {
            mCanvas.removeListener(SWT.Dispose, this);
            mCanvas.removeListener(SWT.KeyDown, this);
            mCanvas.removeListener(SWT.MouseMove, this);
            mCanvas.removeListener(SWT.MouseHover, this);
        }
    }

    @Override
    public void handleEvent(Event event) {
        switch(event.type) {
        case SWT.MouseMove:
            // See if we're still overlapping this or *other* errors; if so, keep the
            // tip up (or update it).
            if (mShowingNodes != null) {
                List<UiViewElementNode> nodes = computeNodes(event);
                if (nodes != null && !nodes.isEmpty()) {
                    if (nodes.equals(mShowingNodes)) {
                        return;
                    } else {
                        show(nodes);
                    }
                    break;
                }
            }

            // If not, fall through and hide the tooltip

            //$FALL-THROUGH$
        case SWT.Dispose:
        case SWT.FocusOut:
        case SWT.KeyDown:
        case SWT.MouseExit:
        case SWT.MouseDown:
            hide();
            break;
        case SWT.MouseHover:
            hide();
            show(event);
            break;
        }
    }

    void hide() {
        if (mTip != null) {
            mTip.dispose();
            mTip = null;
        }
        mShowingNodes = null;
    }

    private void show(Event event) {
        List<UiViewElementNode> nodes = computeNodes(event);
        if (nodes != null && !nodes.isEmpty()) {
            show(nodes);
        }
    }

    /** Show a tooltip listing the lint errors for the given nodes */
    private void show(List<UiViewElementNode> nodes) {
        hide();

        if (!AdtPrefs.getPrefs().isLintOnSave()) {
            return;
        }

        mTip = new LintTooltip(mCanvas, nodes);
        Rectangle rect = mCanvas.getBounds();
        Point size = mTip.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        Point pos = mCanvas.toDisplay(rect.x, rect.y + rect.height);
        if (size.x > rect.width) {
            size = mTip.computeSize(rect.width, SWT.DEFAULT);
        }
        mTip.setBounds(pos.x, pos.y, size.x, size.y);

        mShowingNodes = nodes;
        mTip.setVisible(true);
    }

    /**
     * Compute the list of nodes which have lint warnings near the given mouse
     * coordinates
     *
     * @param event the mouse cursor event
     * @return a list of nodes, possibly empty
     */
    @Nullable
    private List<UiViewElementNode> computeNodes(Event event) {
        LayoutPoint p = ControlPoint.create(mCanvas, event.x, event.y).toLayout();
        LayoutEditorDelegate delegate = mCanvas.getEditorDelegate();
        ViewHierarchy viewHierarchy = mCanvas.getViewHierarchy();
        CanvasTransform mHScale = mCanvas.getHorizontalTransform();
        CanvasTransform mVScale = mCanvas.getVerticalTransform();

        int layoutIconSize = mHScale.inverseScale(ICON_SIZE);
        int slop = mVScale.inverseScale(10); // extra space around icon where tip triggers

        Collection<Node> xmlNodes = delegate.getLintNodes();
        if (xmlNodes == null) {
            return null;
        }
        List<UiViewElementNode> nodes = new ArrayList<UiViewElementNode>();
        for (Node xmlNode : xmlNodes) {
            CanvasViewInfo v = viewHierarchy.findViewInfoFor(xmlNode);
            if (v != null) {
                Rectangle b = v.getAbsRect();
                int x2 = b.x + b.width;
                int y2 = b.y + b.height;
                if (p.x < x2 - layoutIconSize - slop
                        || p.x > x2 + slop
                        || p.y < y2 - layoutIconSize - slop
                        || p.y > y2 + slop) {
                    continue;
                }

                nodes.add(v.getUiViewNode());
            }
        }

        return nodes;
    }
}

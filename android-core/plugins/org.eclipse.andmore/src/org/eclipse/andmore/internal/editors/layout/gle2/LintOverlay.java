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

import com.google.common.collect.Lists;

import org.eclipse.andmore.internal.editors.IconFactory;
import org.eclipse.andmore.internal.editors.layout.LayoutEditorDelegate;
import org.eclipse.andmore.internal.preferences.AdtPrefs;
import org.eclipse.core.resources.IMarker;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;
import org.w3c.dom.Node;

import java.util.Collection;

/**
 * The {@link LintOverlay} paints an icon over each view that contains at least one
 * lint error (unless the view is smaller than the icon)
 */
public class LintOverlay extends Overlay {
    /** Approximate size of lint overlay icons */
    static final int ICON_SIZE = 8;
    /** Alpha to draw lint overlay icons with */
    private static final int ALPHA = 192;

    private final LayoutCanvas mCanvas;
    private Image mWarningImage;
    private Image mErrorImage;

    /**
     * Constructs a new {@link LintOverlay}
     *
     * @param canvas the associated canvas
     */
    public LintOverlay(LayoutCanvas canvas) {
        mCanvas = canvas;
    }

    @Override
    public boolean isHiding() {
        return super.isHiding() || !AdtPrefs.getPrefs().isLintOnSave();
    }

    @Override
    public void paint(GC gc) {
        LayoutEditorDelegate editor = mCanvas.getEditorDelegate();
        Collection<Node> nodes = editor.getLintNodes();
        if (nodes != null && !nodes.isEmpty()) {
            // Copy list before iterating through it to avoid a concurrent list modification
            // in case lint runs in the background while painting and updates this list
            nodes = Lists.newArrayList(nodes);
            ViewHierarchy hierarchy = mCanvas.getViewHierarchy();
            Image icon = getWarningIcon();
            ImageData imageData = icon.getImageData();
            int iconWidth = imageData.width;
            int iconHeight = imageData.height;
            CanvasTransform mHScale = mCanvas.getHorizontalTransform();
            CanvasTransform mVScale = mCanvas.getVerticalTransform();

            // Right/bottom edges of the canvas image; don't paint overlays outside of
            // that. (With for example RelativeLayouts with margins rendered on smaller
            // screens than they are intended for this can happen.)
            int maxX = mHScale.translate(0) + mHScale.getScaledImgSize();
            int maxY = mVScale.translate(0) + mVScale.getScaledImgSize();

            int oldAlpha = gc.getAlpha();
            try {
                gc.setAlpha(ALPHA);
                for (Node node : nodes) {
                    CanvasViewInfo vi = hierarchy.findViewInfoFor(node);
                    if (vi != null) {
                        Rectangle bounds = vi.getAbsRect();
                        int x = mHScale.translate(bounds.x);
                        int y = mVScale.translate(bounds.y);
                        int w = mHScale.scale(bounds.width);
                        int h = mVScale.scale(bounds.height);
                        if (w < iconWidth || h < iconHeight) {
                            // Don't draw badges on tiny widgets (including those
                            // that aren't tiny but are zoomed out too far)
                            continue;
                        }

                        x += w - iconWidth;
                        y += h - iconHeight;

                        if (x > maxX || y > maxY) {
                            continue;
                        }

                        boolean isError = false;
                        IMarker marker = editor.getIssueForNode(vi.getUiViewNode());
                        if (marker != null) {
                            int severity = marker.getAttribute(IMarker.SEVERITY, 0);
                            isError = severity == IMarker.SEVERITY_ERROR;
                        }

                        icon = isError ? getErrorIcon() : getWarningIcon();

                        gc.drawImage(icon, x, y);
                    }
                }
            } finally {
                gc.setAlpha(oldAlpha);
            }
        }
    }

    private Image getWarningIcon() {
        if (mWarningImage == null) {
            mWarningImage = IconFactory.getInstance().getIcon("warning-badge"); //$NON-NLS-1$
        }

        return mWarningImage;
    }

    private Image getErrorIcon() {
        if (mErrorImage == null) {
            mErrorImage = IconFactory.getInstance().getIcon("error-badge");     //$NON-NLS-1$
        }

        return mErrorImage;
    }
}

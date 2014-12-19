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

package org.eclipse.andmore.common.layout;

import com.android.ide.common.api.INode;
import com.android.ide.common.api.Point;
import com.android.ide.common.api.Rect;

/** Test the {@link FrameLayoutRule} */
public class FrameLayoutRuleTest extends LayoutTestBase {
    // Utility for other tests
    protected void dragInto(Rect dragBounds, Point dragPoint, int insertIndex, int currentIndex,
            String... graphicsFragments) {
        INode layout = TestNode.create("android.widget.FrameLayout").id("@+id/FrameLayout01")
                .bounds(new Rect(0, 0, 240, 480)).add(
                        TestNode.create("android.widget.Button").id("@+id/Button01").bounds(
                                new Rect(0, 0, 100, 80)),
                        TestNode.create("android.widget.Button").id("@+id/Button02").bounds(
                                new Rect(0, 100, 100, 80)),
                        TestNode.create("android.widget.Button").id("@+id/Button03").bounds(
                                new Rect(0, 200, 100, 80)),
                        TestNode.create("android.widget.Button").id("@+id/Button04").bounds(
                                new Rect(0, 300, 100, 80)));

        super.dragInto(new FrameLayoutRule(), layout, dragBounds, dragPoint, null,
                insertIndex, currentIndex, graphicsFragments);
    }

    public void testDragMiddle() {
        dragInto(
        // Bounds of the dragged item
                new Rect(0, 0, 105, 80),
                // Drag point
                new Point(30, -10),
                // Expected insert location: We just append in absolute layout
                4,
                // Not dragging one of the existing children
                -1,
                // Bounds rectangle
                "useStyle(DROP_RECIPIENT), drawRect(Rect[0,0,240,480])",

                // Drop Preview
                "useStyle(DROP_PREVIEW), drawRect(0,0,105,80)");
        // Without drag bounds we should just draw guide lines instead
        dragInto(new Rect(0, 0, 0, 0), new Point(30, -10), 4, -1,
                "useStyle(DROP_RECIPIENT), drawRect(Rect[0,0,240,480])",
                "useStyle(DROP_PREVIEW), drawLine(1,0,1,480), drawLine(0,1,240,1)");
    }
}

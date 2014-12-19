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

import com.android.ide.common.api.DropFeedback;
import com.android.ide.common.api.IDragElement;
import com.android.ide.common.api.INode;
import com.android.ide.common.api.Rect;

/** Test the {@link ZoomControlsRule} */
public class ZoomControlsRuleTest extends LayoutTestBase {
    public void testDoNothing() {
        String draggedButtonId = "@+id/DraggedButton";

        IDragElement[] elements = TestDragElement.create(TestDragElement.create(
                "android.widget.Button").id(draggedButtonId));

        INode layout = TestNode.create("android.widget.ZoomControls").id("@+id/ZoomControls01")
        .bounds(new Rect(0, 0, 240, 480)).add(
                TestNode.create("android.widget.Button").id("@+id/Button01").bounds(
                        new Rect(0, 0, 100, 80)),
                TestNode.create("android.widget.Button").id("@+id/Button02").bounds(
                        new Rect(0, 100, 100, 80)),
                TestNode.create("android.widget.Button").id("@+id/Button03").bounds(
                        new Rect(0, 200, 100, 80)),
                TestNode.create("android.widget.Button").id("@+id/Button04").bounds(
                        new Rect(0, 300, 100, 80)));

        ZoomControlsRule rule = new ZoomControlsRule();

        // Enter target
        DropFeedback feedback = rule.onDropEnter(layout, null/*targetView*/, elements);
        // Zoom controls don't respond to drags
        assertNull(feedback);
    }
}

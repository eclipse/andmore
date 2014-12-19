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

import com.android.ide.common.rendering.api.ViewInfo;

import org.eclipse.andmore.internal.editors.layout.uimodel.UiViewElementNode;
import org.eclipse.swt.widgets.Shell;

import java.util.Arrays;

import junit.framework.TestCase;

public class SelectionManagerTest extends TestCase {
    private SelectionManager createManager() {
        LayoutCanvas canvas = new LayoutCanvas(null, null, new Shell(), 0);
        return new SelectionManager(canvas);
    }

    public void testEmpty() {
        SelectionManager manager = createManager();

        assertNotNull(manager.getSelections());
        assertEquals(0, manager.getSelections().size());
        assertFalse(manager.hasMultiSelection());
        assertTrue(manager.isEmpty());
    }

    public void testBasic() {
        SelectionManager manager = createManager();
        assertTrue(manager.isEmpty());

        UiViewElementNode rootNode = CanvasViewInfoTest.createNode("android.widget.LinearLayout",
                true);
        ViewInfo root = new ViewInfo("LinearLayout", rootNode, 10, 10, 100, 100);
        UiViewElementNode child1Node = CanvasViewInfoTest.createNode(rootNode,
                "android.widget.Button", false);
        ViewInfo child1 = new ViewInfo("Button1", child1Node, 0, 0, 50, 20);
        UiViewElementNode child2Node = CanvasViewInfoTest.createNode(rootNode,
                "android.widget.Button", false);
        ViewInfo child2 = new ViewInfo("Button2", child2Node, 0, 20, 70, 25);
        root.setChildren(Arrays.asList(child1, child2));
        CanvasViewInfo rootView = CanvasViewInfo.create(root, true /* layoutlib5 */).getFirst();
        assertNotNull(rootView);

        manager.selectMultiple(Arrays.asList(rootView, rootView.getChildren().get(0), rootView
                .getChildren().get(1)));
        assertEquals(3, manager.getSelections().size());
        assertFalse(manager.isEmpty());
        assertTrue(manager.hasMultiSelection());

        // Expect read-only result; ensure that's the case
        try {
            manager.getSelections().remove(0);
            fail("Result should be read only collection");
        } catch (Exception e) {
            ; //ok, what we expected
        }

        manager.selectNone();
        assertEquals(0, manager.getSelections().size());
        assertTrue(manager.isEmpty());

        manager.selectSingle(rootView);
        assertEquals(1, manager.getSelections().size());
        assertFalse(manager.isEmpty());
        assertSame(rootView, manager.getSelections().get(0).getViewInfo());

        manager.selectMultiple(Arrays.asList(rootView, rootView.getChildren().get(0), rootView
                .getChildren().get(1)));
        assertEquals(3, manager.getSelections().size());

        manager.deselect(rootView.getChildren().get(0));
        assertEquals(2, manager.getSelections().size());
        manager.deselect(rootView);
        assertEquals(1, manager.getSelections().size());
        assertSame(rootView.getChildren().get(1), manager.getSelections().get(0).getViewInfo());
    }

    public void testSelectParent() {
        SelectionManager manager = createManager();
        assertTrue(manager.isEmpty());

        UiViewElementNode rootNode = CanvasViewInfoTest.createNode("android.widget.LinearLayout",
                true);
        ViewInfo root = new ViewInfo("LinearLayout", rootNode, 10, 10, 100, 100);
        UiViewElementNode child1Node = CanvasViewInfoTest.createNode(rootNode,
                "android.widget.Button", false);
        ViewInfo child1 = new ViewInfo("Button1", child1Node, 0, 0, 50, 20);
        UiViewElementNode child2Node = CanvasViewInfoTest.createNode(rootNode,
                "android.widget.Button", false);
        ViewInfo child2 = new ViewInfo("Button2", child2Node, 0, 20, 70, 25);
        root.setChildren(Arrays.asList(child1, child2));
        CanvasViewInfo rootView = CanvasViewInfo.create(root, true /* layoutlib5 */).getFirst();
        assertNotNull(rootView);

        manager.selectMultiple(Arrays.asList(rootView.getChildren().get(0)));
        assertEquals(1, manager.getSelections().size());
        assertFalse(manager.isEmpty());
        assertSame(rootView.getChildren().get(0), manager.getSelections().get(0).getViewInfo());

        manager.selectParent();
        assertEquals(1, manager.getSelections().size());
        assertFalse(manager.isEmpty());
        assertSame(rootView, manager.getSelections().get(0).getViewInfo());
    }
}

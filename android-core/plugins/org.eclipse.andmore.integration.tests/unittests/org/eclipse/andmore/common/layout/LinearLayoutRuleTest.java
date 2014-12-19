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

import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_ID;
import static com.android.SdkConstants.ATTR_LAYOUT_HEIGHT;
import static com.android.SdkConstants.ATTR_LAYOUT_WIDTH;
import static com.android.SdkConstants.ATTR_ORIENTATION;
import static com.android.SdkConstants.VALUE_HORIZONTAL;
import static com.android.SdkConstants.VALUE_VERTICAL;

import com.android.ide.common.api.DropFeedback;
import com.android.ide.common.api.IAttributeInfo.Format;
import com.android.ide.common.api.IDragElement;
import com.android.ide.common.api.IMenuCallback;
import com.android.ide.common.api.INode;
import com.android.ide.common.api.IViewRule;
import com.android.ide.common.api.Point;
import com.android.ide.common.api.Rect;
import com.android.ide.common.api.RuleAction;
import com.android.ide.common.api.RuleAction.NestedAction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Test the {@link LinearLayoutRule} */
public class LinearLayoutRuleTest extends LayoutTestBase {
    // Utility for other tests
    protected void dragIntoEmpty(Rect dragBounds) {
        boolean haveBounds = dragBounds.isValid();

        IViewRule rule = new LinearLayoutRule();

        INode targetNode = TestNode.create("android.widget.LinearLayout").id(
        "@+id/LinearLayout01").bounds(new Rect(0, 0, 240, 480));
        Point dropPoint = new Point(10, 5);

        IDragElement[] elements = TestDragElement.create(TestDragElement.create(
                "android.widget.Button", dragBounds).id("@+id/Button01"));

        // Enter target
        DropFeedback feedback = rule.onDropEnter(targetNode, null/*targetView*/, elements);
        assertNotNull(feedback);
        assertFalse(feedback.invalidTarget);
        assertNotNull(feedback.painter);

        feedback = rule.onDropMove(targetNode, elements, feedback, dropPoint);
        assertNotNull(feedback);
        assertFalse(feedback.invalidTarget);

        // Paint feedback and make sure it's what we expect
        TestGraphics graphics = new TestGraphics();
        assertNotNull(feedback.painter);
        feedback.painter.paint(graphics, targetNode, feedback);
        assertEquals(
                // Expect to see a recipient rectangle around the bounds of the
                // LinearLayout,
                // as well as a single vertical line as a drop preview located
                // along the left
                // edge (for this horizontal linear layout) showing insert
                // position at index 0,
                // and finally a rectangle for the bounds of the inserted button
                // centered over
                // the middle
                "[useStyle(DROP_RECIPIENT), "
                        +
                        // Bounds rectangle
                        "drawRect(Rect[0,0,240,480]), "
                        + "useStyle(DROP_ZONE), drawLine(1,0,1,480), "
                        + "useStyle(DROP_ZONE_ACTIVE), " + "useStyle(DROP_PREVIEW), " +
                        // Insert position line
                        "drawLine(1,0,1,480)" + (haveBounds ?
                        // Outline of dragged node centered over position line
                        ", useStyle(DROP_PREVIEW), " + "drawRect(1,0,101,80)"
                                // Nothing when we don't have bounds
                                : "") + "]", graphics.getDrawn().toString());

        // Attempt a drop
        assertEquals(0, targetNode.getChildren().length);
        rule.onDropped(targetNode, elements, feedback, dropPoint);
        assertEquals(1, targetNode.getChildren().length);
        assertEquals("@+id/Button01", targetNode.getChildren()[0].getStringAttr(
                ANDROID_URI, ATTR_ID));
    }

    // Utility for other tests
    protected INode dragInto(boolean vertical, Rect dragBounds, Point dragPoint,
            int insertIndex, int currentIndex,
            String... graphicsFragments) {
        INode linearLayout = TestNode.create("android.widget.LinearLayout").id(
                "@+id/LinearLayout01").bounds(new Rect(0, 0, 240, 480)).set(ANDROID_URI,
                ATTR_ORIENTATION,
                vertical ? VALUE_VERTICAL : VALUE_HORIZONTAL)
                .add(
                        TestNode.create("android.widget.Button").id("@+id/Button01").bounds(
                                new Rect(0, 0, 100, 80)),
                        TestNode.create("android.widget.Button").id("@+id/Button02").bounds(
                                new Rect(0, 100, 100, 80)),
                        TestNode.create("android.widget.Button").id("@+id/Button03").bounds(
                                new Rect(0, 200, 100, 80)),
                        TestNode.create("android.widget.Button").id("@+id/Button04").bounds(
                                new Rect(0, 300, 100, 80)));

        return super.dragInto(new LinearLayoutRule(), linearLayout, dragBounds, dragPoint, null,
                insertIndex, currentIndex, graphicsFragments);
    }

    // Check that the context menu registers the expected menu items
    public void testContextMenu() {
        LinearLayoutRule rule = new LinearLayoutRule();
        initialize(rule, "android.widget.LinearLayout");
        INode node = TestNode.create("android.widget.Button").id("@+id/Button012");

        List<RuleAction> contextMenu = new ArrayList<RuleAction>();
        rule.addContextMenuActions(contextMenu, node);
        assertEquals(6, contextMenu.size());
        assertEquals("Edit ID...", contextMenu.get(0).getTitle());
        assertTrue(contextMenu.get(1) instanceof RuleAction.Separator);
        assertEquals("Layout Width", contextMenu.get(2).getTitle());
        assertEquals("Layout Height", contextMenu.get(3).getTitle());
        assertTrue(contextMenu.get(4) instanceof RuleAction.Separator);
        assertEquals("Other Properties", contextMenu.get(5).getTitle());

        RuleAction propertiesMenu = contextMenu.get(5);
        assertTrue(propertiesMenu.getClass().getName(),
                propertiesMenu instanceof NestedAction);
    }

    public void testContextMenuCustom() {
        LinearLayoutRule rule = new LinearLayoutRule();
        initialize(rule, "android.widget.LinearLayout");
        INode node = TestNode.create("android.widget.LinearLayout").id("@+id/LinearLayout")
            .set(ANDROID_URI, ATTR_LAYOUT_WIDTH, "42dip")
            .set(ANDROID_URI, ATTR_LAYOUT_HEIGHT, "50sp");

        List<RuleAction> contextMenu = new ArrayList<RuleAction>();
        rule.addContextMenuActions(contextMenu, node);
        assertEquals(6, contextMenu.size());
        assertEquals("Layout Width", contextMenu.get(2).getTitle());
        RuleAction menuAction = contextMenu.get(2);
        assertTrue(menuAction instanceof RuleAction.Choices);
        RuleAction.Choices choices = (RuleAction.Choices) menuAction;
        List<String> titles = choices.getTitles();
        List<String> ids = choices.getIds();
        assertEquals("Wrap Content", titles.get(0));
        assertEquals("wrap_content", ids.get(0));
        assertEquals("Match Parent", titles.get(1));
        assertEquals("match_parent", ids.get(1));
        assertEquals("42dip", titles.get(2));
        assertEquals("42dip", ids.get(2));
        assertEquals("42dip", choices.getCurrent());
    }

    // Check that the context menu manipulates the orientation attribute
    public void testOrientation() {
        LinearLayoutRule rule = new LinearLayoutRule();
        initialize(rule, "android.widget.LinearLayout");
        TestNode node = TestNode.create("android.widget.LinearLayout").id("@+id/LinearLayout012");
        node.putAttributeInfo(ANDROID_URI, "orientation",
                new TestAttributeInfo(ATTR_ORIENTATION, Format.ENUM_SET,
                        "android.widget.LinearLayout",
                        new String[] {"horizontal", "vertical"}, null, null));

        assertNull(node.getStringAttr(ANDROID_URI, ATTR_ORIENTATION));

        List<RuleAction> contextMenu = new ArrayList<RuleAction>();
        rule.addContextMenuActions(contextMenu, node);
        assertEquals(7, contextMenu.size());
        RuleAction orientationAction = contextMenu.get(1);
        assertEquals("Orientation", orientationAction.getTitle());

        assertTrue(orientationAction.getClass().getName(),
                orientationAction instanceof RuleAction.Choices);

        RuleAction.Choices choices = (RuleAction.Choices) orientationAction;
        IMenuCallback callback = choices.getCallback();
        callback.action(orientationAction, Collections.singletonList(node), VALUE_VERTICAL, true);

        String orientation = node.getStringAttr(ANDROID_URI,
                ATTR_ORIENTATION);
        assertEquals(VALUE_VERTICAL, orientation);
        callback.action(orientationAction, Collections.singletonList(node), VALUE_HORIZONTAL,
                true);
        orientation = node.getStringAttr(ANDROID_URI, ATTR_ORIENTATION);
        assertEquals(VALUE_HORIZONTAL, orientation);
    }

    // Check that the context menu manipulates the orientation attribute
    public void testProperties() {
        LinearLayoutRule rule = new LinearLayoutRule();
        initialize(rule, "android.widget.LinearLayout");
        TestNode node = TestNode.create("android.widget.LinearLayout").id("@+id/LinearLayout012");
        node.putAttributeInfo(ANDROID_URI, "orientation",
                new TestAttributeInfo(ATTR_ORIENTATION, Format.ENUM_SET,
                        "android.widget.LinearLayout",
                        new String[] {"horizontal", "vertical"}, null, null));
        node.setAttributeSources(Arrays.asList("android.widget.LinearLayout",
                "android.view.ViewGroup", "android.view.View"));
        node.putAttributeInfo(ANDROID_URI, "gravity",
                new TestAttributeInfo("gravity", Format.INTEGER_SET,
                        "android.widget.LinearLayout", null, null, null));


        assertNull(node.getStringAttr(ANDROID_URI, ATTR_ORIENTATION));

        List<RuleAction> contextMenu = new ArrayList<RuleAction>();
        rule.addContextMenuActions(contextMenu, node);
        assertEquals(8, contextMenu.size());

        assertEquals("Orientation", contextMenu.get(1).getTitle());
        assertEquals("Edit Gravity...", contextMenu.get(2).getTitle());

        assertEquals("Other Properties", contextMenu.get(7).getTitle());

        RuleAction propertiesMenu = contextMenu.get(7);
        assertTrue(propertiesMenu.getClass().getName(),
                propertiesMenu instanceof NestedAction);
        NestedAction nested = (NestedAction) propertiesMenu;
        List<RuleAction> nestedActions = nested.getNestedActions(node);
        assertEquals(9, nestedActions.size());
        assertEquals("Recent", nestedActions.get(0).getTitle());
        assertTrue(nestedActions.get(1) instanceof RuleAction.Separator);
        assertEquals("Defined by LinearLayout", nestedActions.get(2).getTitle());
        assertEquals("Inherited from ViewGroup", nestedActions.get(3).getTitle());
        assertEquals("Inherited from View", nestedActions.get(4).getTitle());
        assertTrue(nestedActions.get(5) instanceof RuleAction.Separator);
        assertEquals("Layout Parameters", nestedActions.get(6).getTitle());
        assertTrue(nestedActions.get(7) instanceof RuleAction.Separator);
        assertEquals("All By Name", nestedActions.get(8).getTitle());

        BaseViewRule.editedProperty(ATTR_ORIENTATION);

        RuleAction recentAction = nestedActions.get(0);
        assertTrue(recentAction instanceof NestedAction);
        NestedAction recentChoices = (NestedAction) recentAction;
        List<RuleAction> recentItems = recentChoices.getNestedActions(node);

        assertEquals(1, recentItems.size());
        assertEquals("Orientation", recentItems.get(0).getTitle());

        BaseViewRule.editedProperty("gravity");
        recentItems = recentChoices.getNestedActions(node);
        assertEquals(2, recentItems.size());
        assertEquals("Gravity...", recentItems.get(0).getTitle());
        assertEquals("Orientation", recentItems.get(1).getTitle());

        BaseViewRule.editedProperty(ATTR_ORIENTATION);
        recentItems = recentChoices.getNestedActions(node);
        assertEquals(2, recentItems.size());
        assertEquals("Orientation", recentItems.get(0).getTitle());
        assertEquals("Gravity...", recentItems.get(1).getTitle());

        // Lots of other properties -- flushes out properties that apply to this view
        for (int i = 0; i < 30; i++) {
            BaseViewRule.editedProperty("dummy_" + i);
        }
        recentItems = recentChoices.getNestedActions(node);
        assertEquals(0, recentItems.size());

        BaseViewRule.editedProperty("gravity");
        recentItems = recentChoices.getNestedActions(node);
        assertEquals(1, recentItems.size());
        assertEquals("Gravity...", recentItems.get(0).getTitle());
    }

    public void testDragInEmptyWithBounds() {
        dragIntoEmpty(new Rect(0, 0, 100, 80));
    }

    public void testDragInEmptyWithoutBounds() {
        dragIntoEmpty(new Rect(0, 0, 0, 0));
    }

    public void testDragInVerticalTop() {
        dragInto(true,
                // Bounds of the dragged item
                new Rect(0, 0, 105, 80),
                // Drag point
                new Point(30, -10),
                // Expected insert location
                0,
                // Not dragging one of the existing children
                -1,
                // Bounds rectangle
                "useStyle(DROP_RECIPIENT), drawRect(Rect[0,0,240,480])",

                // Drop zones
                "useStyle(DROP_ZONE), drawLine(0,0,240,0), drawLine(0,90,240,90), "
                        + "drawLine(0,190,240,190), drawLine(0,290,240,290), "
                        + "drawLine(0,381,240,381)",

                // Active nearest line
                "useStyle(DROP_ZONE_ACTIVE), useStyle(DROP_PREVIEW), drawLine(0,0,240,0)",

                // Preview of the dropped rectangle
                "useStyle(DROP_PREVIEW), drawRect(0,-40,105,40)");

        // Without drag bounds it should be identical except no preview
        // rectangle
        dragInto(true,
                new Rect(0, 0, 0, 0), // Invalid
                new Point(30, -10), 0, -1,
                "useStyle(DROP_ZONE_ACTIVE), useStyle(DROP_PREVIEW), drawLine(0,0,240,0)");
    }

    public void testDragInVerticalBottom() {
        dragInto(true,
                // Bounds of the dragged item
                new Rect(0, 0, 105, 80),
                // Drag point
                new Point(30, 500),
                // Expected insert location
                4,
                // Not dragging one of the existing children
                -1,
                // Bounds rectangle
                "useStyle(DROP_RECIPIENT), drawRect(Rect[0,0,240,480])",

                // Drop zones
                "useStyle(DROP_ZONE), drawLine(0,0,240,0), drawLine(0,90,240,90), "
                        + "drawLine(0,190,240,190), drawLine(0,290,240,290), drawLine(0,381,240,381), ",

                // Active nearest line
                "useStyle(DROP_ZONE_ACTIVE), useStyle(DROP_PREVIEW), drawLine(0,381,240,381)",

                // Preview of the dropped rectangle
                "useStyle(DROP_PREVIEW), drawRect(0,381,105,461)");

        // Check without bounds too
        dragInto(true, new Rect(0, 0, 105, 80), new Point(30, 500), 4, -1,
                "useStyle(DROP_PREVIEW), drawRect(0,381,105,461)");
    }

    public void testDragInVerticalMiddle() {
        dragInto(true,
                // Bounds of the dragged item
                new Rect(0, 0, 105, 80),
                // Drag point
                new Point(0, 170),
                // Expected insert location
                2,
                // Not dragging one of the existing children
                -1,
                // Bounds rectangle
                "useStyle(DROP_RECIPIENT), drawRect(Rect[0,0,240,480])",

                // Drop zones
                "useStyle(DROP_ZONE), drawLine(0,0,240,0), drawLine(0,90,240,90), "
                        + "drawLine(0,190,240,190), drawLine(0,290,240,290)",

                // Active nearest line
                "useStyle(DROP_ZONE_ACTIVE), useStyle(DROP_PREVIEW), drawLine(0,190,240,190)",

                // Preview of the dropped rectangle
                "useStyle(DROP_PREVIEW), drawRect(0,150,105,230)");

        // Check without bounds too
        dragInto(true, new Rect(0, 0, 105, 80), new Point(0, 170), 2, -1,
                "useStyle(DROP_PREVIEW), drawRect(0,150,105,230)");
    }

    public void testDragInVerticalMiddleSelfPos() {
        // Drag the 2nd button, down to the position between 3rd and 4th
        dragInto(true,
                // Bounds of the dragged item
                new Rect(0, 100, 100, 80),
                // Drag point
                new Point(0, 250),
                // Expected insert location
                2,
                // Dragging 1st item
                1,
                // Bounds rectangle

                "useStyle(DROP_RECIPIENT), drawRect(Rect[0,0,240,480])",

                // Drop zones - these are different because we exclude drop
                // zones around the
                // dragged item itself (it doesn't make sense to insert directly
                // before or after
                // myself
                "useStyle(DROP_ZONE), drawLine(0,0,240,0), drawLine(0,290,240,290), "
                        + "drawLine(0,381,240,381)",

                // Preview line along insert axis
                "useStyle(DROP_ZONE_ACTIVE), useStyle(DROP_PREVIEW), drawLine(0,290,240,290)",

                // Preview of dropped rectangle
                "useStyle(DROP_PREVIEW), drawRect(0,250,100,330)");

        // Test dropping on self (no position change):
        dragInto(true,
                // Bounds of the dragged item
                new Rect(0, 100, 100, 80),
                // Drag point
                new Point(0, 210),
                // Expected insert location
                1,
                // Dragging from same pos
                1,
                // Bounds rectangle
                "useStyle(DROP_RECIPIENT), drawRect(Rect[0,0,240,480])",

                // Drop zones - these are different because we exclude drop
                // zones around the
                // dragged item itself (it doesn't make sense to insert directly
                // before or after
                // myself
                "useStyle(DROP_ZONE), drawLine(0,0,240,0), drawLine(0,290,240,290), "
                        + "drawLine(0,381,240,381)",

                // No active nearest line when you're over the self pos!

                // Preview of the dropped rectangle
                "useStyle(DROP_ZONE_ACTIVE), useStyle(DROP_PREVIEW), drawRect(0,100,100,180)");
    }

    public void testDragToLastPosition() {
        // Drag a button to the last position -- and confirm that the preview rectangle
        // is now shown midway between the second to last and last positions, but fully
        // below the drop zone line:
        dragInto(true,
                // Bounds of the dragged item
                new Rect(0, 100, 100, 80),
                // Drag point
                new Point(0, 400),
                // Expected insert location
                3,
                // Dragging 1st item
                1,

                // Bounds rectangle
                "useStyle(DROP_RECIPIENT), drawRect(Rect[0,0,240,480])",

                // Drop Zones
                "useStyle(DROP_ZONE), drawLine(0,0,240,0), drawLine(0,290,240,290), " +
                "drawLine(0,381,240,381), ",

                // Active Drop Zone
                "useStyle(DROP_ZONE_ACTIVE), useStyle(DROP_PREVIEW), drawLine(0,381,240,381)",

                // Drop Preview
                "useStyle(DROP_PREVIEW), drawRect(0,381,100,461)");
    }

    // Left to test:
    // Check inserting at last pos with multiple children
    // Check inserting with no bounds rectangle for dragged element
}

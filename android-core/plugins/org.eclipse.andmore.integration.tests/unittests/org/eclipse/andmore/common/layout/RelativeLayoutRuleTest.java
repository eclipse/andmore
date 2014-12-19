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

import com.android.ide.common.api.INode;
import com.android.ide.common.api.Point;
import com.android.ide.common.api.Rect;

/** Test the {@link RelativeLayoutRule} */
public class RelativeLayoutRuleTest extends LayoutTestBase {
    // Utility for other tests
    protected INode dragInto(Rect dragBounds, Point dragPoint, Point secondDragPoint,
            int insertIndex, int currentIndex, String... graphicsFragments) {
        INode layout = TestNode.create("android.widget.RelativeLayout").id("@+id/RelativeLayout01")
                .bounds(new Rect(0, 0, 240, 480)).add(
                // Add centered button as the anchor
                        TestNode.create("android.widget.Button").id("@+id/Centered").bounds(
                                new Rect(70, 200, 100, 80)).set(ANDROID_URI,
                                "layout_centerInParent", "true"),
                        // Add a second button anchored to it
                        TestNode.create("android.widget.Button").id("@+id/Below").bounds(
                                new Rect(70, 280, 100, 80)).set(ANDROID_URI, "layout_below",
                                "@+id/Centered").set(ANDROID_URI, "layout_alignLeft",
                                "@+id/Centered"));

        return super.dragInto(new RelativeLayoutRule(), layout, dragBounds, dragPoint,
                secondDragPoint, insertIndex, currentIndex, graphicsFragments);
    }

    protected INode dragInto(Rect dragBounds, Point dragPoint, Point secondDragPoint,
            int insertIndex, int currentIndex, String[] extraFragments,
            String... graphicsFragments) {

        // When we switch to JDK6, use Arrays#copyOf instead
        String[] combined = new String[extraFragments.length + graphicsFragments.length];
        System.arraycopy(graphicsFragments, 0, combined, 0, graphicsFragments.length);
        System.arraycopy(extraFragments, 0, combined, graphicsFragments.length,
                extraFragments.length);

        return dragInto(dragBounds, dragPoint, secondDragPoint, insertIndex,
                currentIndex, combined);
    }

    /* This needs to be updated for the new interaction
    public void testDropTopEdge() {
        // If we drag right into the button itself, not a valid drop position
        INode inserted = dragInto(
                new Rect(0, 0, 105, 80), new Point(30, -10), null, 2, -1,
                // Bounds rectangle
                "useStyle(DROP_RECIPIENT), drawRect(Rect[0,0,240,480])",

                // Preview line + drop zone rectangle along the top
                "useStyle(DROP_ZONE), drawRect(Rect[0,-10,240,20])",
                "useStyle(DROP_ZONE_ACTIVE), fillRect(Rect[0,-10,240,20])",
                "useStyle(DROP_PREVIEW), drawLine(0,0,240,0)",

                // Tip
                "useStyle(HELP), drawBoxedStrings(5,15,[alignParentTop])",

                // Drop preview
                "useStyle(DROP_PREVIEW), drawRect(Rect[0,0,105,80])");

        assertEquals("true", inserted.getStringAttr(ANDROID_URI,
                "layout_alignParentTop"));
    }

    public void testDropZones() {
        List<Pair<Point,String[]>> zones = new ArrayList<Pair<Point,String[]>>();

        zones.add(Pair.of(new Point(51+10, 181+10),
                new String[] {"above=@+id/Centered", "toLeftOf=@+id/Centered"}));
        zones.add(Pair.of(new Point(71+10, 181+10),
                new String[] {"above=@+id/Centered", "alignLeft=@+id/Centered"}));
        zones.add(Pair.of(new Point(104+10, 181+10),
                new String[] {"above=@+id/Centered", "alignRight=@+id/Centered"}));
        zones.add(Pair.of(new Point(137+10, 181+10),
                new String[] {"above=@+id/Centered", "alignRight=@+id/Centered"}));
        zones.add(Pair.of(new Point(170+10, 181+10),
                new String[] {"above=@+id/Centered", "toRightOf=@+id/Centered"}));
        zones.add(Pair.of(new Point(51+10, 279+10),
                new String[] {"below=@+id/Centered", "toLeftOf=@+id/Centered"}));
        zones.add(Pair.of(new Point(71+10, 279+10),
                new String[] {"below=@+id/Centered", "alignLeft=@+id/Centered"}));
        zones.add(Pair.of(new Point(104+10, 279+10),
                new String[] {"below=@+id/Centered", "alignLeft=@+id/Centered"}));
        zones.add(Pair.of(new Point(137+10, 279+10),
                new String[] {"below=@+id/Centered", "alignRight=@+id/Centered"}));
        zones.add(Pair.of(new Point(170+10, 279+10),
                new String[] {"below=@+id/Centered", "toRightOf=@+id/Centered"}));
        zones.add(Pair.of(new Point(51+10, 201+10),
                new String[] {"toLeftOf=@+id/Centered", "alignTop=@+id/Centered"}));
        zones.add(Pair.of(new Point(51+10, 227+10),
                new String[] {"toLeftOf=@+id/Centered", "alignTop=@+id/Centered"}));
        zones.add(Pair.of(new Point(170+10, 201+10),
                new String[] {"toRightOf=@+id/Centered", "alignTop=@+id/Centered"}));
        zones.add(Pair.of(new Point(51+10, 253+10),
                new String[] {"toLeftOf=@+id/Centered", "alignBottom=@+id/Centered"}));
        zones.add(Pair.of(new Point(170+10, 227+10),
                new String[] {"toRightOf=@+id/Centered", "alignTop=@+id/Centered",
            "alignBottom=@+id/Centered"}));
        zones.add(Pair.of(new Point(170+10, 253+10),
                new String[] {"toRightOf=@+id/Centered", "alignBottom=@+id/Centered"}));

        for (Pair<Point,String[]> zonePair : zones) {
            Point dropPoint = zonePair.getFirst();
            String[] attachments = zonePair.getSecond();
            // If we drag right into the button itself, not a valid drop position

            INode inserted = dragInto(
                    new Rect(0, 0, 105, 80), new Point(120, 240), dropPoint, 1, -1,
                    attachments,

                    // Bounds rectangle
                    "useStyle(DROP_RECIPIENT), drawRect(Rect[0,0,240,480])",

                    // Drop zones
                    "useStyle(DROP_ZONE), "
                            + "drawRect(Rect[51,181,20,20]), drawRect(Rect[71,181,33,20]), "
                            + "drawRect(Rect[104,181,33,20]), drawRect(Rect[137,181,33,20]), "
                            + "drawRect(Rect[170,181,20,20]), drawRect(Rect[51,279,20,20]), "
                            + "drawRect(Rect[71,279,33,20]), drawRect(Rect[104,279,33,20]), "
                            + "drawRect(Rect[137,279,33,20]), drawRect(Rect[170,279,20,20]), "
                            + "drawRect(Rect[51,201,20,26]), drawRect(Rect[51,227,20,26]), "
                            + "drawRect(Rect[51,253,20,26]), drawRect(Rect[170,201,20,26]), "
                            + "drawRect(Rect[170,227,20,26]), drawRect(Rect[170,253,20,26])");

            for (String attachment : attachments) {
                String[] elements = attachment.split("=");
                String name = "layout_" + elements[0];
                String value = elements[1];
                assertEquals(value, inserted.getStringAttr(ANDROID_URI, name));
            }
        }
    }


    public void testDragInvalid() {
        // If we drag right into the button itself, not a valid drop position
        dragInto(new Rect(70, 200, 100, 80), new Point(120, 240), new Point(120, 240), -1, 0,
        // Bounds rectangle
                "useStyle(DROP_RECIPIENT), drawRect(Rect[0,0,240,480])",

                // Invalid marker
                "useStyle(INVALID), fillRect(Rect[70,200,100,80]), drawLine(70,200,170,280), "
                        + "drawLine(70,280,170,200)");
    }

    // TODO: Test error (dragging on ancestor)
     */
}

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

import org.eclipse.swt.events.MouseEvent;

public class ControlPointTest extends PointTestCases {
    public void testCreateFromMouseEvent() throws Exception {
        MouseEvent mouseEvent = canvasMouseEvent(10, 20, 0);

        ControlPoint point = ControlPoint.create(mCanvas, mouseEvent);
        assertEquals(10, point.x);
        assertEquals(20, point.y);
    }

    public void testCreateFromCoordinates() throws Exception {
        ControlPoint point = ControlPoint.create(mCanvas, 10, 20);
        assertEquals(10, point.x);
        assertEquals(20, point.y);
    }

    public void testConvertToLayout() throws Exception {
        ControlPoint point = ControlPoint.create(new TestLayoutCanvas(), 10, 20);
        assertEquals(10, point.x);
        assertEquals(20, point.y);

        LayoutPoint layoutPoint = point.toLayout();
        assertNotNull(layoutPoint);
        assertEquals(40, layoutPoint.x);
        assertEquals(60, layoutPoint.y);

        // For sanity let's also convert back and verify
        ControlPoint controlPoint = layoutPoint.toControl();
        assertNotNull(controlPoint);
        assertNotSame(controlPoint, point);
        assertEquals(point, controlPoint);
        assertEquals(10, controlPoint.x);
        assertEquals(20, controlPoint.y);
    }

    public void testEquals() throws Exception {
        ControlPoint point1 = ControlPoint.create(mCanvas, 1, 1);
        ControlPoint point2 = ControlPoint.create(mCanvas, 1, 2);
        ControlPoint point3 = ControlPoint.create(mCanvas, 2, 1);
        ControlPoint point2b = ControlPoint.create(mCanvas, 1, 2);

        assertFalse(point2.equals(null));

        assertEquals(point2, point2);
        assertEquals(point2, point2b);
        assertEquals(point2.hashCode(), point2b.hashCode());
        assertNotSame(point2, point2b);

        assertFalse(point1.equals(point2));
        assertFalse(point1.equals(point3));
        assertFalse(point2.equals(point3));
        assertFalse(point1.equals(point2));
    }
}

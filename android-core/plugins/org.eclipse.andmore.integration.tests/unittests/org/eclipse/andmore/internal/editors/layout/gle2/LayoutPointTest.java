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

public class LayoutPointTest extends PointTestCases {
    public void testCreateFromCoordinates() throws Exception {
        LayoutPoint point = LayoutPoint.create(mCanvas, 10, 20);
        assertEquals(10, point.x);
        assertEquals(20, point.y);
    }

    public void testEquals() throws Exception {
        LayoutPoint point1 = LayoutPoint.create(mCanvas, 1, 1);
        LayoutPoint point2 = LayoutPoint.create(mCanvas, 1, 2);
        LayoutPoint point3 = LayoutPoint.create(mCanvas, 2, 1);
        LayoutPoint point2b = LayoutPoint.create(mCanvas, 1, 2);

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

    public void testConvertToControl() throws Exception {
        LayoutPoint point = LayoutPoint.create(new TestLayoutCanvas(), 10, 20);
        assertEquals(10, point.x);
        assertEquals(20, point.y);

        ControlPoint controlPoint = point.toControl();
        assertNotNull(controlPoint);
        assertEquals(-5, controlPoint.x);
        assertEquals(0, controlPoint.y);

        // For sanity let's also convert back and verify
        LayoutPoint layoutPoint = controlPoint.toLayout();
        assertNotNull(layoutPoint);
        assertNotSame(layoutPoint, point);
        assertEquals(point, layoutPoint);
        assertEquals(10, layoutPoint.x);
        assertEquals(20, layoutPoint.y);
    }

}

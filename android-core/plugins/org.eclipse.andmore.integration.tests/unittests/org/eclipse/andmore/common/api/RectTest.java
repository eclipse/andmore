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

package org.eclipse.andmore.common.api;

import com.android.ide.common.api.Point;
import com.android.ide.common.api.Rect;

import junit.framework.TestCase;

public class RectTest extends TestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public final void testRect() {
        Rect r = new Rect();
        assertEquals(0, r.x);
        assertEquals(0, r.y);
        assertEquals(0, r.w);
        assertEquals(0, r.h);
    }

    public final void testRectIntIntIntInt() {
        Rect r = new Rect(1, 2, 3, 4);
        assertEquals(1, r.x);
        assertEquals(2, r.y);
        assertEquals(3, r.w);
        assertEquals(4, r.h);
    }

    public final void testRectRect() {
        Rect r = new Rect(1, 2, 3, 4);
        Rect r2 = new Rect(r);

        assertNotSame(r2, r);
        assertEquals(r2, r);
    }

    public final void testSetIntIntIntInt() {
        Rect r = new Rect(1, 2, 3, 4);
        Rect r2 = r.set(3, 4, 20, 30);

        assertSame(r2, r);
        assertEquals(3, r2.x);
        assertEquals(4, r2.y);
        assertEquals(20, r2.w);
        assertEquals(30, r2.h);
    }

    public final void testSetRect() {
        Rect r = new Rect(1, 2, 3, 4);
        Rect r2 = new Rect(3, 4, 20, 30);
        Rect r3 = r.set(r2);

        assertSame(r3, r);
        assertNotSame(r3, r2);
        assertEquals(3, r.x);
        assertEquals(4, r.y);
        assertEquals(20, r.w);
        assertEquals(30, r.h);
    }

    public final void testCopy() {
        Rect r = new Rect(1, 2, 3, 4);
        Rect r2 = r.copy();

        assertNotSame(r2, r);
        assertEquals(r2, r);
    }

    public final void testIsValid() {
        Rect r = new Rect();
        assertFalse(r.isValid());

        r = new Rect(1, 2, 3, 4);
        assertTrue(r.isValid());

        // Rectangles must have a width > 0 to be valid
        r = new Rect(1, 2, 0, 4);
        assertFalse(r.isValid());
        r = new Rect(1, 2, -5, 4);
        assertFalse(r.isValid());

        // Rectangles must have a height > 0 to be valid
        r = new Rect(1, 2, 3, 0);
        assertFalse(r.isValid());
        r = new Rect(1, 2, 3, -5);
        assertFalse(r.isValid());

        r = new Rect(1, 2, 0, 0);
        assertFalse(r.isValid());
        r = new Rect(1, 2, -20, -5);
        assertFalse(r.isValid());
    }

    public final void testContainsIntInt() {
        Rect r = new Rect(3, 4, 20, 30);

        assertTrue(r.contains(3,    4));
        assertTrue(r.contains(3+19, 4));
        assertTrue(r.contains(3+19, 4+29));
        assertTrue(r.contains(3,    4+29));

        assertFalse(r.contains(3-1, 4));
        assertFalse(r.contains(3,   4-1));
        assertFalse(r.contains(3-1, 4-1));

        assertFalse(r.contains(3+20, 4));
        assertFalse(r.contains(3+20, 4+30));
        assertFalse(r.contains(3,    4+30));
    }

    public final void testContainsIntInt_Invalid() {
        // Invalid rects always return false
        Rect r = new Rect(3, 4, -20, -30);
        assertFalse(r.contains(3,    4));
    }

    public final void testContainsPoint_Null() {
        // contains(null) returns false rather than an NPE
        Rect r = new Rect(3, 4, -20, -30);
        assertFalse(r.contains((Point) null));
    }

    public final void testContainsRect_Null() {
        // contains(null) returns false rather than an NPE
        Rect r = new Rect(3, 4, -20, -30);
        assertFalse(r.contains((Rect) null));
    }

    public final void testContainsPoint() {
        Rect r = new Rect(3, 4, 20, 30);

        assertTrue(r.contains(new Point(3,    4)));
        assertTrue(r.contains(new Point(3+19, 4)));
        assertTrue(r.contains(new Point(3+19, 4+29)));
        assertTrue(r.contains(new Point(3,    4+29)));

        assertFalse(r.contains(new Point(3-1, 4)));
        assertFalse(r.contains(new Point(3,   4-1)));
        assertFalse(r.contains(new Point(3-1, 4-1)));

        assertFalse(r.contains(new Point(3+20, 4)));
        assertFalse(r.contains(new Point(3+20, 4+30)));
        assertFalse(r.contains(new Point(3,    4+30)));
    }

    public final void testContainsRect() {
        Rect r = new Rect(3, 4, 20, 30);

        assertTrue(r.contains(new Rect(3, 4, 5, 10)));
        assertFalse(r.contains(new Rect(3 - 1, 4, 5, 10)));
    }

    public final void testIntersects() {
        Rect r1 = new Rect(0, 0, 10, 10);
        Rect r2 = new Rect(1, 1, 5, 5);
        Rect r3 = new Rect(10, 0, 1, 1);
        Rect r4 = new Rect(5, 5, 10, 10);
        Rect r5 = new Rect(-1, 0, 1, 1);
        Rect r6 = new Rect(0, 10, 1, 1);

        assertTrue(r1.intersects(r2));
        assertTrue(r2.intersects(r1));
        assertTrue(r1.intersects(r4));
        assertFalse(r1.intersects(r3));
        assertFalse(r1.intersects(r5));
        assertFalse(r1.intersects(r6));
    }

    public final void testMoveTo() {
        Rect r = new Rect(3, 4, 20, 30);
        Rect r2 = r.moveTo(100, 200);

        assertSame(r2, r);
        assertEquals(100, r.x);
        assertEquals(200, r.y);
        assertEquals(20, r.w);
        assertEquals(30, r.h);
    }

    public final void testOffsetBy() {
        Rect r = new Rect(3, 4, 20, 30);
        Rect r2 = r.offsetBy(100, 200);

        assertSame(r2, r);
        assertEquals(103, r.x);
        assertEquals(204, r.y);
        assertEquals(20, r.w);
        assertEquals(30, r.h);
    }

    public final void testGetCenter() {
        Rect r = new Rect(3, 4, 20, 30);
        Point p = r.getCenter();

        assertEquals(3+20/2, p.x);
        assertEquals(4+30/2, p.y);
    }

    public final void testGetTopLeft() {
        Rect r = new Rect(3, 4, 20, 30);
        Point p = r.getTopLeft();

        assertEquals(3, p.x);
        assertEquals(4, p.y);
    }

    public final void testGetBottomLeft() {
        Rect r = new Rect(3, 4, 20, 30);
        Point p = r.getBottomLeft();

        assertEquals(3, p.x);
        assertEquals(4+30, p.y);
    }

    public final void testGetTopRight() {
        Rect r = new Rect(3, 4, 20, 30);
        Point p = r.getTopRight();

        assertEquals(3+20, p.x);
        assertEquals(4, p.y);
    }

    public final void testGetBottomRight() {
        Rect r = new Rect(3, 4, 20, 30);
        Point p = r.getBottomRight();

        assertEquals(3+20, p.x);
        assertEquals(4+30, p.y);
    }

    public final void testToString() {
        Rect r = new Rect(3, 4, 20, 30);

        assertEquals("Rect [(3,4)-(23,34): 20x30]", r.toString());
    }

    public final void testEqualsObject() {
        Rect r = new Rect(3, 4, 20, 30);

        assertFalse(r.equals(null));
        assertFalse(r.equals(new Object()));
        assertTrue(r.equals(new Rect(3, 4, 20, 30)));
    }

    public final void testEqualsObject_Invalid() {
        Rect r = new Rect(3, 4, 20, 30);
        assertTrue(r.isValid());

        Rect i1 = new Rect(3, 4, 0, 0);
        assertFalse(i1.isValid());
        Rect i2 = new Rect(10, 20, 0, 0);
        assertFalse(i2.isValid());

        // valid rects can't be equal to invalid rects
        assertFalse(r.equals(i1));
        assertFalse(r.equals(i2));

        // invalid rects are equal to each other whatever their content is
        assertEquals(i2, i1);
    }

    public final void testHashCode() {
        Rect r = new Rect(1, 2, 3, 4);
        Rect r1 = new Rect(3, 4, 20, 30);
        Rect r2 = new Rect(3, 4, 20, 30);

        assertFalse(r1.hashCode() == r.hashCode());
        assertEquals(r2.hashCode(), r1.hashCode());
    }


    public final void testCenter() {
        Rect r = new Rect(10, 20, 30, 40);
        Point center = r.center();
        assertEquals(25, center.x);
        assertEquals(40, center.y);
        assertEquals(25, r.centerX());
        assertEquals(40, r.centerY());
    }

    public final void testX2Y2() {
        Rect r = new Rect(1, 2, 3, 4);
        assertEquals(4, r.x2());
        assertEquals(6, r.y2());
    }
}

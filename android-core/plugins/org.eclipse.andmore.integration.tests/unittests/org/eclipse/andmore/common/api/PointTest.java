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

import junit.framework.TestCase;

public class PointTest extends TestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public final void testPointIntInt() {
        Point p = new Point(1, 2);
        assertEquals(1, p.x);
        assertEquals(2, p.y);

        p = new Point(-3, -4);
        assertEquals(-3, p.x);
        assertEquals(-4, p.y);
    }

    public final void testSet() {
        Point p = new Point(1, 2);
        assertEquals(1, p.x);
        assertEquals(2, p.y);

        p.set(-3, -4);
        assertEquals(-3, p.x);
        assertEquals(-4, p.y);
    }

    public final void testPointPoint() {
        Point p = new Point(1, 2);
        Point p2 = new Point(p);

        assertNotSame(p, p2);
        assertEquals(p, p2);
    }

    public final void testPointPoint_Null() {
        // Constructing a point with null throws an NPE
        try {
            new Point(null);
        } catch (NullPointerException ignore) {
            return; // success
        }

        fail("new Point(null) failed to throew NullPointerException");
    }

    public final void testCopy() {
        Point p = new Point(1, 2);
        Point p2 = p.copy();

        assertNotSame(p, p2);
        assertEquals(p, p2);
    }

    public final void testOffsetBy() {
        Point p = new Point(1, 2);
        Point p2 = p.offsetBy(3, 4);

        assertSame(p, p2);
        assertEquals(1+3, p.x);
        assertEquals(2+4, p.y);
    }

    public final void testEquals_Null() {
        Point p = new Point(1, 2);
        assertFalse(p.equals(null));
    }

    public final void testEquals_UnknownObject() {
        Point p = new Point(1, 2);
        assertFalse(p.equals(new Object()));
    }

    public final void testEquals_Point() {
        Point p = new Point(1, 2);
        Point p1 = new Point(1, 2);
        Point p2 = new Point(-3, -4);

        assertNotSame(p1, p);
        assertTrue(p.equals(p1));

        assertFalse(p.equals(p2));
    }

    public final void testHashCode() {
        Point p = new Point(1, 2);
        Point p1 = new Point(1, 2);
        Point p2 = new Point(-3, -4);

        assertNotSame(p1, p);
        assertEquals(p1.hashCode(), p.hashCode());

        assertFalse(p2.hashCode() == p.hashCode());
    }

    public final void testToString() {
        Point p = new Point(1, 2);
        assertEquals("Point [1x2]", p.toString());
    }

}

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
package org.eclipse.andmore.common.layout;

import static org.eclipse.andmore.common.layout.GravityHelper.GRAVITY_BOTTOM;
import static org.eclipse.andmore.common.layout.GravityHelper.GRAVITY_CENTER_HORIZ;
import static org.eclipse.andmore.common.layout.GravityHelper.GRAVITY_CENTER_VERT;
import static org.eclipse.andmore.common.layout.GravityHelper.GRAVITY_END;
import static org.eclipse.andmore.common.layout.GravityHelper.GRAVITY_FILL_HORIZ;
import static org.eclipse.andmore.common.layout.GravityHelper.GRAVITY_FILL_VERT;
import static org.eclipse.andmore.common.layout.GravityHelper.GRAVITY_LEFT;
import static org.eclipse.andmore.common.layout.GravityHelper.GRAVITY_RIGHT;
import static org.eclipse.andmore.common.layout.GravityHelper.GRAVITY_START;
import static org.eclipse.andmore.common.layout.GravityHelper.GRAVITY_TOP;
import static org.eclipse.andmore.common.layout.GravityHelper.getGravity;

import com.android.utils.XmlUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import junit.framework.TestCase;

@SuppressWarnings("javadoc")
public class GravityHelperTest extends TestCase {
    public void testGravity() throws Exception {
        assertEquals(GRAVITY_BOTTOM, GravityHelper.getGravity("bottom", 0));
        assertEquals(GRAVITY_BOTTOM | GRAVITY_LEFT, GravityHelper.getGravity("bottom|left", 0));
        assertEquals(GRAVITY_BOTTOM | GRAVITY_LEFT | GRAVITY_START,
                GravityHelper.getGravity("bottom|left|start", 0));
        assertEquals(GRAVITY_BOTTOM | GRAVITY_LEFT | GRAVITY_START,
                GravityHelper.getGravity("bottom|start|left", 0));
        assertEquals(GRAVITY_BOTTOM | GRAVITY_RIGHT | GRAVITY_END,
                GravityHelper.getGravity("bottom|right|end", 0));
        assertEquals(GRAVITY_BOTTOM | GRAVITY_RIGHT | GRAVITY_END,
                GravityHelper.getGravity("bottom|end|right", 0));
        assertEquals(GRAVITY_CENTER_HORIZ | GRAVITY_CENTER_VERT,
                GravityHelper.getGravity("center", 0));
    }

    public void testGravityString() throws Exception {
        assertEquals("left", getGravity(GRAVITY_LEFT));
        assertEquals("start", getGravity(GRAVITY_START));
        assertEquals("right", getGravity(GRAVITY_RIGHT));
        assertEquals("end", getGravity(GRAVITY_END));
        assertEquals("top", getGravity(GRAVITY_TOP));
        assertEquals("bottom", getGravity(GRAVITY_BOTTOM));
        assertEquals("center_horizontal", getGravity(GRAVITY_CENTER_HORIZ));
        assertEquals("center_vertical", getGravity(GRAVITY_CENTER_VERT));
        assertEquals("fill_horizontal", getGravity(GRAVITY_FILL_HORIZ));
        assertEquals("fill_vertical", getGravity(GRAVITY_FILL_VERT));

        assertEquals("center", getGravity(GRAVITY_CENTER_HORIZ|GRAVITY_CENTER_VERT));

        assertEquals("left|bottom", getGravity(GRAVITY_LEFT|GRAVITY_BOTTOM));
        assertEquals("left|start|bottom", getGravity(GRAVITY_LEFT|GRAVITY_START|GRAVITY_BOTTOM));
        assertEquals("center_horizontal|top", getGravity(GRAVITY_CENTER_HORIZ|GRAVITY_TOP));
    }

    public void testConstrained() throws Exception {
        assertTrue(GravityHelper.isConstrainedHorizontally(GRAVITY_LEFT));
        assertTrue(GravityHelper.isConstrainedHorizontally(GRAVITY_START));
        assertTrue(GravityHelper.isConstrainedHorizontally(GRAVITY_RIGHT));
        assertTrue(GravityHelper.isConstrainedHorizontally(GRAVITY_END));
        assertTrue(GravityHelper.isConstrainedHorizontally(GRAVITY_CENTER_HORIZ));
        assertTrue(GravityHelper.isConstrainedHorizontally(GRAVITY_FILL_HORIZ));

        assertFalse(GravityHelper.isConstrainedVertically(GRAVITY_LEFT));
        assertFalse(GravityHelper.isConstrainedVertically(GRAVITY_START));
        assertFalse(GravityHelper.isConstrainedVertically(GRAVITY_RIGHT));
        assertFalse(GravityHelper.isConstrainedVertically(GRAVITY_END));
        assertFalse(GravityHelper.isConstrainedVertically(GRAVITY_CENTER_HORIZ));
        assertFalse(GravityHelper.isConstrainedVertically(GRAVITY_FILL_HORIZ));

        assertTrue(GravityHelper.isConstrainedVertically(GRAVITY_TOP));
        assertTrue(GravityHelper.isConstrainedVertically(GRAVITY_BOTTOM));
        assertTrue(GravityHelper.isConstrainedVertically(GRAVITY_CENTER_VERT));
        assertTrue(GravityHelper.isConstrainedVertically(GRAVITY_FILL_VERT));

        assertFalse(GravityHelper.isConstrainedHorizontally(GRAVITY_TOP));
        assertFalse(GravityHelper.isConstrainedHorizontally(GRAVITY_BOTTOM));
        assertFalse(GravityHelper.isConstrainedHorizontally(GRAVITY_CENTER_VERT));
        assertFalse(GravityHelper.isConstrainedHorizontally(GRAVITY_FILL_VERT));
    }

    public void testAligned() throws Exception {
        assertTrue(GravityHelper.isLeftAligned(GRAVITY_LEFT|GRAVITY_TOP));
        assertTrue(GravityHelper.isLeftAligned(GRAVITY_START|GRAVITY_TOP));
        assertTrue(GravityHelper.isLeftAligned(GRAVITY_LEFT));
        assertTrue(GravityHelper.isLeftAligned(GRAVITY_START));
        assertFalse(GravityHelper.isLeftAligned(GRAVITY_RIGHT));
        assertFalse(GravityHelper.isLeftAligned(GRAVITY_END));

        assertTrue(GravityHelper.isTopAligned(GRAVITY_LEFT|GRAVITY_TOP));
        assertTrue(GravityHelper.isTopAligned(GRAVITY_START|GRAVITY_TOP));
        assertTrue(GravityHelper.isTopAligned(GRAVITY_TOP));
        assertFalse(GravityHelper.isTopAligned(GRAVITY_BOTTOM));
    }

    public void testElement() throws Exception {
        Document document = XmlUtils.parseDocumentSilently(""
                + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                + "    android:layout_width=\"match_parent\"\n"
                + "    android:layout_height=\"wrap_content\"\n"
                + "    android:layout_gravity=\"start|center_vertical|left\""
                + "    android:orientation=\"vertical\" />\n", true);
        assertNotNull(document);
        Element element = document.getDocumentElement();
        assertNotNull(element);
        int gravity = GravityHelper.getGravity(element);
        assertEquals("left|start|center_vertical", GravityHelper.getGravity(gravity));
    }
}
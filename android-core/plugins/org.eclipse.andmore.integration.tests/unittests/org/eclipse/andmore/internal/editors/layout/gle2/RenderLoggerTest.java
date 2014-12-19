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

import junit.framework.TestCase;

public class RenderLoggerTest extends TestCase {
    public void testLogger1() throws Exception {
        RenderLogger l = new RenderLogger("foo", null);
        assertFalse(l.hasProblems());
    }

    public void testLogger2() throws Exception {
        RenderLogger l = new RenderLogger("foo", null);
        assertFalse(l.hasProblems());
        l.fidelityWarning(null, "No perspective Transforms", null, null);
        l.fidelityWarning(null, "No GPS", null, null);
        assertTrue(l.hasProblems());
        assertEquals("The graphics preview in the layout editor may not be accurate:\n"
                + "* No perspective Transforms\n" + "* No GPS\n", l.getProblems(true));
        assertFalse(l.seenTag("foo"));
        assertFalse(l.seenTag(null));
    }

    public void testLogger3() throws Exception {
        RenderLogger l = new RenderLogger("foo", null);
        assertFalse(l.hasProblems());
        l.error("timeout", "Sample Error", new RuntimeException(), null);
        l.warning("slow", "Sample warning", null);
        assertTrue(l.hasProblems());
        assertEquals("Sample Error\n" + "Sample warning\n"
                + "Exception details are logged in Window > Show View > Error Log",
                l.getProblems(true));
        assertFalse(l.seenTag("foo"));
        assertTrue(l.seenTag("timeout"));
        assertTrue(l.seenTag("slow"));
        assertFalse(l.seenTagPrefix("foo"));
        assertTrue(l.seenTagPrefix("timeout"));
        assertTrue(l.seenTagPrefix("slow"));
        assertTrue(l.seenTagPrefix("time"));
        assertFalse(l.seenTagPrefix("timeouts"));
    }

    public void testLoggerSuppressWarnings() throws Exception {
        RenderLogger l = new RenderLogger("foo", null);
        assertFalse(l.hasProblems());
        RenderLogger.ignoreFidelityWarning("No perspective Transforms");
        l.fidelityWarning(null, "No perspective Transforms", null, null);
        l.fidelityWarning(null, "No GPS", null, null);
        assertTrue(l.hasProblems());
        assertEquals("The graphics preview in the layout editor may not be accurate:\n"
                + "* No GPS\n", l.getProblems(true));
        assertEquals("", l.getProblems(false));
        assertFalse(l.seenTag("foo"));
        assertFalse(l.seenTag(null));

        l = new RenderLogger("foo", null);
        assertFalse(l.hasProblems());
        RenderLogger.ignoreFidelityWarning("No perspective Transforms");
        RenderLogger.ignoreFidelityWarning("No GPS");
        l.fidelityWarning(null, "No perspective Transforms", null, null);
        l.fidelityWarning(null, "No GPS", null, null);
        assertFalse(l.hasProblems());
        assertEquals("", l.getProblems(true));
        assertFalse(l.seenTag("foo"));
        assertFalse(l.seenTag(null));
    }
}

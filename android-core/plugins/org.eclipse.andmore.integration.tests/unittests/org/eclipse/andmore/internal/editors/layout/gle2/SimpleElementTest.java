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

import com.android.ide.common.api.Rect;

import java.util.Arrays;

import junit.framework.TestCase;

public class SimpleElementTest extends TestCase {

    private SimpleElement e;

    /**
     * Helper method to compare arrays' *content* is equal (instead of object identity).
     * Also produces a suitable output to understand mismatch, if any.
     * <p/>
     * Pre-requisite: The arrays' elements must properly implement {@link Object#equals(Object)}
     * and a sensible {@link Object#toString()}.
     */
    private static void assertArrayEquals(Object[] expected, Object[] actual) {
        if (!Arrays.equals(expected, actual)) {
            // In case of failure, transform the arguments into strings and let
            // assertEquals(string) handle it as it can produce a nice diff of the string.
            String strExpected = expected == null ? "(null)" : Arrays.toString(expected);
            String strActual = actual == null ? "(null)" : Arrays.toString(actual);

            if (strExpected.equals(strActual)) {
                fail(String.format("Array not equal:\n Expected[%d]=%s\n Actual[%d]=%s",
                        expected == null ? 0 : expected.length,
                        strExpected,
                        actual == null ? 0 : actual.length,
                        strActual));
            } else {
                assertEquals(strExpected, strActual);
            }
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        e = new SimpleElement("android.view.LinearLayout", // fqcn
                "android.view.FrameLayout", // parentFqcn
                new Rect(10, 5, 60, 40),    // bounds
                new Rect(0, 0, 320, 480));  // parentBounds
    }

    public final void testGetFqcn() {
        assertEquals("android.view.LinearLayout", e.getFqcn());
    }

    public final void testGetParentFqcn() {
        assertEquals("android.view.FrameLayout", e.getParentFqcn());
    }

    public final void testGetBounds() {
        assertEquals(new Rect(10, 5, 60, 40), e.getBounds());
    }

    public final void testGetParentBounds() {
        assertEquals(new Rect(0, 0, 320, 480), e.getParentBounds());
    }

    public final void testToString() {
        assertEquals("{V=3,N=android.view.LinearLayout,P=android.view.FrameLayout,R=10 5 60 40,Q=0 0 320 480\n" +
                     "}\n",
                e.toString());

        e.addAttribute(new SimpleAttribute("uri", "name", "value"));
        e.addAttribute(new SimpleAttribute("my-uri", "second-name", "my = value "));

        assertEquals("{V=3,N=android.view.LinearLayout,P=android.view.FrameLayout,R=10 5 60 40,Q=0 0 320 480\n" +
                "@name:uri=value\n" +
                "@second-name:my-uri=my = value \n" +
                "}\n",
           e.toString());

        SimpleElement e2 = new SimpleElement("android.view.Button",
                                             "android.view.LinearLayout",
                                             new Rect(10, 20, 30, 40),
                                             new Rect(0, 0, 320, 480));
        e2.addAttribute(new SimpleAttribute("uri1", "name1", "value1"));
        SimpleElement e3 = new SimpleElement("android.view.CheckBox",
                                             "android.view.LinearLayout",
                                             new Rect(-1, -2, -3, -4), // invalid rect is ignored
                                             new Rect(-1, -2, -3, -4)); // invalid rectis ignored
        e3.addAttribute(new SimpleAttribute("uri2", "name2", "value2"));
        e3.addAttribute(new SimpleAttribute("uri3", "name3", "value3"));
        e.addInnerElement(e2);
        e.addInnerElement(e3);

        assertEquals("{V=3,N=android.view.LinearLayout,P=android.view.FrameLayout,R=10 5 60 40,Q=0 0 320 480\n" +
                "@name:uri=value\n" +
                "@second-name:my-uri=my = value \n" +
                "{V=3,N=android.view.Button,P=android.view.LinearLayout,R=10 20 30 40,Q=0 0 320 480\n" +
                "@name1:uri1=value1\n" +
                "}\n" +
                "{V=3,N=android.view.CheckBox,P=android.view.LinearLayout\n" +
                "@name2:uri2=value2\n" +
                "@name3:uri3=value3\n" +
                "}\n" +
                "}\n",
           e.toString());
    }

    public final void testParseString() {
        assertArrayEquals(
            new SimpleElement[] { new SimpleElement("android.view.LinearLayout",
                                                    null, null, null) },
            SimpleElement.parseString(
                "{V=3,N=android.view.LinearLayout\n" +
                "}\n"));

        assertArrayEquals(
                new SimpleElement[] { new SimpleElement("android.view.LinearLayout",
                                                        "android.view.FrameLayout",
                                                        null, null) },
                SimpleElement.parseString(
                    "{V=3,N=android.view.LinearLayout,P=android.view.FrameLayout\n" +
                    "}\n"));

        assertArrayEquals(
                new SimpleElement[] { new SimpleElement("android.view.LinearLayout",
                                                        null,
                                                        new Rect(10, 5, 60, 40),
                                                        new Rect(0, 0, 320, 480)) },
                SimpleElement.parseString(
                    "{V=3,N=android.view.LinearLayout,R=10 5 60 40,Q=0 0 320 480\n" +
                    "}\n"));


        assertArrayEquals(
            new SimpleElement[] { e },
            SimpleElement.parseString(
                "{V=3,N=android.view.LinearLayout,P=android.view.FrameLayout,R=10 5 60 40,Q=0 0 320 480\n" +
                "}\n"));


        e.addAttribute(new SimpleAttribute("uri", "name", "value"));
        e.addAttribute(new SimpleAttribute("my-uri", "second-name", "my = value "));

        assertArrayEquals(
                new SimpleElement[] { e },
                SimpleElement.parseString(
                        "{V=3,N=android.view.LinearLayout,P=android.view.FrameLayout,R=10 5 60 40,Q=0 0 320 480\n" +
                        "@name:uri=value\n" +
                        "@second-name:my-uri=my = value \n" +
                        "}\n"));


        SimpleElement e2 = new SimpleElement("android.view.Button",
                                             "android.view.LinearLayout",
                                             new Rect(10, 20, 30, 40),
                                             new Rect(0, 0, 320, 480));
        e2.addAttribute(new SimpleAttribute("uri1", "name1", "value1"));
        SimpleElement e3 = new SimpleElement("android.view.CheckBox",
                                             "android.view.LinearLayout",
                                             new Rect(-1, -2, -3, -4),
                                             new Rect(-1, -2, -3, -4));
        e3.addAttribute(new SimpleAttribute("uri2", "name2", "value2"));
        e3.addAttribute(new SimpleAttribute("uri3", "name3", "value3"));
        e.addInnerElement(e2);
        e.addInnerElement(e3);

        assertArrayEquals(
                new SimpleElement[] { e },
                SimpleElement.parseString(
                        "{V=3,N=android.view.LinearLayout,P=android.view.FrameLayout,R=10 5 60 40,Q=0 0 320 480\n" +
                        "@name:uri=value\n" +
                        "@second-name:my-uri=my = value \n" +
                        "{V=3,N=android.view.Button,P=android.view.LinearLayout,R=10 20 30 40,Q=0 0 320 480\n" +
                        "@name1:uri1=value1\n" +
                        "}\n" +
                        "{V=3,N=android.view.CheckBox,P=android.view.LinearLayout,R=-1 -2 -3 -4,Q=-1 -2 -3 -4\n" +
                        "@name2:uri2=value2\n" +
                        "@name3:uri3=value3\n" +
                        "}\n" +
                        "}\n"));

        // Parse string can also parse an array of elements
        assertArrayEquals(
                new SimpleElement[] { e, e2, e3 },
                SimpleElement.parseString(
                        "{V=3,N=android.view.LinearLayout,P=android.view.FrameLayout,R=10 5 60 40,Q=0 0 320 480\n" +
                        "@name:uri=value\n" +
                        "@second-name:my-uri=my = value \n" +
                        "{V=3,N=android.view.Button,P=android.view.LinearLayout,R=10 20 30 40,Q=0 0 320 480\n" +
                        "@name1:uri1=value1\n" +
                        "}\n" +
                        "{V=3,N=android.view.CheckBox,P=android.view.LinearLayout,R=-1 -2 -3 -4\n" +
                        "@name2:uri2=value2\n" +
                        "@name3:uri3=value3\n" +
                        "}\n" +
                        "}\n" +
                        "{V=3,N=android.view.Button,P=android.view.LinearLayout,R=10 20 30 40,Q=0 0 320 480\n" +
                        "@name1:uri1=value1\n" +
                        "}\n" +
                        "{V=3,N=android.view.CheckBox,P=android.view.LinearLayout,R=-1 -2 -3 -4,Q=-1 -2 -3 -4\n" +
                        "@name2:uri2=value2\n" +
                        "@name3:uri3=value3\n" +
                        "}\n"));

    }

    public final void testAddGetAttribute() {
        assertNotNull(e.getAttributes());
        assertArrayEquals(
                new SimpleAttribute[] {},
                e.getAttributes());

        e.addAttribute(new SimpleAttribute("uri", "name", "value"));
        assertArrayEquals(
                new SimpleAttribute[] { new SimpleAttribute("uri", "name", "value") },
                e.getAttributes());

        e.addAttribute(new SimpleAttribute("my-uri", "second-name", "value"));
        assertArrayEquals(
                new SimpleAttribute[] { new SimpleAttribute("uri", "name", "value"),
                                        new SimpleAttribute("my-uri", "second-name", "value") },
                e.getAttributes());

        assertNull(e.getAttribute("unknown uri", "name"));
        assertNull(e.getAttribute("uri", "unknown name"));
        assertEquals(new SimpleAttribute("uri", "name", "value"),
                     e.getAttribute("uri", "name"));
        assertEquals(new SimpleAttribute("my-uri", "second-name", "value"),
                     e.getAttribute("my-uri", "second-name"));
    }

    public final void testAddGetInnerElements() {
        assertNotNull(e.getInnerElements());
        assertArrayEquals(
                new SimpleElement[] {},
                e.getInnerElements());

        e.addInnerElement(new SimpleElement("android.view.Button", null, null, null));
        assertArrayEquals(
                new SimpleElement[] { new SimpleElement("android.view.Button", null, null, null) },
                e.getInnerElements());

        e.addInnerElement(new SimpleElement("android.view.CheckBox", null, null, null));
        assertArrayEquals(
               new SimpleElement[] { new SimpleElement("android.view.Button", null, null, null),
                                     new SimpleElement("android.view.CheckBox", null, null, null) },
               e.getInnerElements());
    }

    public final void testEqualsObject() {
        assertFalse(e.equals(null));
        assertFalse(e.equals(new Object()));

        assertNotSame(new SimpleElement("android.view.LinearLayout",
                                        "android.view.FrameLayout",
                                        new Rect(10, 5, 60, 40),
                                        new Rect(0, 0, 320, 480)),
                      e);
        assertEquals(new SimpleElement("android.view.LinearLayout",
                                       "android.view.FrameLayout",
                                       new Rect(10, 5, 60, 40),
                                       new Rect(0, 0, 320, 480)),
                      e);
        assertTrue(e.equals(new SimpleElement("android.view.LinearLayout",
                                              "android.view.FrameLayout",
                                              new Rect(10, 5, 60, 40),
                                              new Rect(0, 0, 320, 480))));

        // not the same FQCN
        assertFalse(e.equals(new SimpleElement("android.view.Button",
                                               "android.view.FrameLayout",
                                               new Rect(10, 5, 60, 40),
                                               new Rect(0, 0, 320, 480))));

        // not the same parent
        assertFalse(e.equals(new SimpleElement("android.view.LinearLayout",
                                               "android.view.LinearLayout",
                                               new Rect(10, 5, 60, 40),
                                               new Rect(0, 0, 320, 480))));

        // not the same bounds
        assertFalse(e.equals(new SimpleElement("android.view.LinearLayout",
                                               "android.view.FrameLayout",
                                               new Rect(10, 25, 30, 40),
                                               new Rect(0, 0, 320, 480))));

        // not the same parent bounds
        assertFalse(e.equals(new SimpleElement("android.view.LinearLayout",
                                               "android.view.FrameLayout",
                                               new Rect(10, 5, 60, 40),
                                               new Rect(10, 100, 160, 240))));
    }

    public final void testHashCode() {
        int he = e.hashCode();

        assertEquals(he, new SimpleElement("android.view.LinearLayout",
                                           "android.view.FrameLayout",
                                           new Rect(10, 5, 60, 40),
                                           new Rect(0, 0, 320, 480)).hashCode());


        // not the same FQCN
        assertFalse(he == new SimpleElement("android.view.Button",
                                            "android.view.FrameLayout",
                                            new Rect(10, 5, 60, 40),
                                            new Rect(0, 0, 320, 480)).hashCode());

        // not the same parent
        assertFalse(he == new SimpleElement("android.view.LinearLayout",
                                            "android.view.Button",
                                            new Rect(10, 5, 60, 40),
                                            new Rect(0, 0, 320, 480)).hashCode());

        // not the same bounds
        assertFalse(he == new SimpleElement("android.view.LinearLayout",
                                            "android.view.FrameLayout",
                                            new Rect(10, 25, 30, 40),
                                            new Rect(0, 0, 320, 480)).hashCode());

        // not the same parent bounds
        assertFalse(he == new SimpleElement("android.view.LinearLayout",
                                            "android.view.FrameLayout",
                                            new Rect(10, 25, 30, 40),
                                            new Rect(10, 100, 160, 240)).hashCode());
    }

}

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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

@SuppressWarnings("javadoc")
public class IncludeFinderTest extends TestCase {
    public void testEncodeDecode1() throws Exception {
        // Test ending with just a key
        String s = "bar,baz,foo";
        assertEquals(s, IncludeFinder.encodeMap(IncludeFinder.decodeMap(s)));
    }

    public void testDecode1() throws Exception {
        // Test ending with just a key
        String s = "foo";
        assertTrue(IncludeFinder.decodeMap(s).containsKey("foo"));
        assertEquals(0, IncludeFinder.decodeMap(s).get("foo").size());
    }

    public void testDecode2() throws Exception {
        // Test ending with just a key
        String s = "foo=>{bar,baz}";
        assertTrue(IncludeFinder.decodeMap(s).containsKey("foo"));
        assertEquals("[bar, baz]",
                IncludeFinder.decodeMap(s).get("foo").toString());
    }

    public void testNoBlanks() throws Exception {
        // Make sure we skip the },
        String s = "foo=>{bar,baz},bar";
        assertNull(IncludeFinder.decodeMap(s).get(""));
    }

    public void testEncodeDecode2() throws Exception {
        // Test ending with just a key
        String s = "bar,key1=>{value1,value2},key2=>{value3,value4}";
        assertEquals(s, IncludeFinder.encodeMap(IncludeFinder.decodeMap(s)));
    }

    public void testUpdates() throws Exception {
        IncludeFinder finder = IncludeFinder.create();
        assertEquals(null, finder.getIncludedBy("foo"));

        finder.setIncluded("bar", Arrays.<String>asList("foo", "baz"), false);
        finder.setIncluded("baz", Arrays.<String>asList("foo"), false);
        assertEquals(Arrays.asList("bar", "baz"), finder.getIncludedBy("foo"));
        finder.setIncluded("bar", Collections.<String>emptyList(), false);
        assertEquals(Arrays.asList("baz"), finder.getIncludedBy("foo"));
        finder.setIncluded("baz", Collections.<String>emptyList(), false);
        assertEquals(Collections.emptyList(), finder.getIncludedBy("foo"));
    }

    public void testFindIncludes() throws Exception {
        String xml =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
            "    android:layout_width=\"match_parent\"\n" +
            "    android:layout_height=\"match_parent\"\n" +
            "    android:orientation=\"vertical\" >\n" +
            "\n" +
            "    <RadioButton\n" +
            "        android:id=\"@+id/radioButton1\"\n" +
            "        android:layout_width=\"wrap_content\"\n" +
            "        android:layout_height=\"wrap_content\"\n" +
            "        android:text=\"RadioButton\" />\n" +
            "\n" +
            "    <include\n" +
            "        android:layout_width=\"wrap_content\"\n" +
            "        android:layout_height=\"wrap_content\"\n" +
            "        layout=\"@layout/layout3\" />\n" +
            "\n" +
            "    <include\n" +
            "        android:layout_width=\"wrap_content\"\n" +
            "        android:layout_height=\"wrap_content\"\n" +
            "        layout=\"@layout/layout4\" />\n" +
            "\n" +
            "</LinearLayout>";
        List<String> includes = IncludeFinder.findIncludes(xml);
        Collections.sort(includes);
        assertEquals(Arrays.asList("layout3", "layout4"), includes);
    }

    public void testFindFragments() throws Exception {
        String xml =
            "<RelativeLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
            "    xmlns:tools=\"http://schemas.android.com/tools\"\n" +
            "    android:layout_width=\"match_parent\"\n" +
            "    android:layout_height=\"match_parent\"\n" +
            "    tools:context=\".MainActivity\" >\n" +
            "\n" +
            "    <fragment\n" +
            "        android:id=\"@+id/fragment1\"\n" +
            "        android:name=\"android.app.ListFragment\"\n" +
            "        android:layout_width=\"wrap_content\"\n" +
            "        android:layout_height=\"wrap_content\"\n" +
            "        android:layout_alignParentLeft=\"true\"\n" +
            "        android:layout_alignParentTop=\"true\"\n" +
            "        android:layout_marginLeft=\"58dp\"\n" +
            "        android:layout_marginTop=\"74dp\"\n" +
            "        tools:layout=\"@layout/myfragment\" />\n" +
            "\n" +
            "</RelativeLayout>";
        List<String> includes = IncludeFinder.findIncludes(xml);
        Collections.sort(includes);
        assertEquals(Arrays.asList("myfragment"), includes);
    }


}

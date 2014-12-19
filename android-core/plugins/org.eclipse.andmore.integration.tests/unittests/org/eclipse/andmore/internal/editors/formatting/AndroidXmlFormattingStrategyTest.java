/*
 * Copyright (C) 2012 The Android Open Source Project
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
package org.eclipse.andmore.internal.editors.formatting;

import com.android.ide.common.xml.XmlFormatPreferences;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.ReplaceEdit;

import junit.framework.TestCase;

@SuppressWarnings("javadoc")
public class AndroidXmlFormattingStrategyTest extends TestCase {
    // In the given before document, replace in the range replaceStart to replaceEnd
    // the formatted string, and assert that it's identical to the given after string
    private void check(String before, int replaceStart, int replaceEnd, String formatted,
            String expected, XmlFormatPreferences prefs)
                    throws MalformedTreeException, BadLocationException {
        Document document = new Document();
        document.set(before);
        ReplaceEdit edit = AndroidXmlFormattingStrategy.createReplaceEdit(document, replaceStart,
                replaceEnd, formatted, prefs);
        assertNotNull(edit);
        edit.apply(document);
        String contents = document.get();
        // Ensure that we don't have any mangled CRLFs
        char prev =  0;
        boolean haveCrlf = false;
        for (int i = 0, n = contents.length(); i < n; i++) {
            char c = contents.charAt(i);
            if (c == '\r') {
                haveCrlf = true;
            }
            if (!(c != '\r' || prev != '\r')) {
                fail("Mangled document: Found adjacent \\r's starting at " + i
                        + ": " + contents.substring(i - 1, Math.min(contents.length(), i + 10))
                                + "...");
            }
            if (haveCrlf && c == '\n' && prev != '\r') {
                fail("Mangled document: In a CRLF document, found \\n without preceeding \\r");
            }

            prev = c;
        }

        assertEquals(expected, contents);
    }

    // In the given before document, replace the range indicated by [ and ] with the given
    // formatted string, and assert that it's identical to the given after string
    private void check(
            String before, String insert, String expected,
            XmlFormatPreferences prefs)
            throws MalformedTreeException, BadLocationException {
        int replaceStart = before.indexOf('[');
        assertTrue(replaceStart != -1);
        before = before.substring(0, replaceStart) + before.substring(replaceStart + 1);

        int replaceEnd = before.indexOf(']');
        assertTrue(replaceEnd != -1);
        before = before.substring(0, replaceEnd) + before.substring(replaceEnd + 1);

        check(before, replaceStart, replaceEnd, insert, expected, prefs);
    }

    public void test1() throws Exception {
        check(
            // Before
            "<root>\n" +
            "[     <element/>\n" +
            "   <second/>\n" +
            "]\n" +
            "</root>\n",

            // Insert
            "    <element/>\n" +
            "    <second/>\n",

            // After
            "<root>\n" +
            "    <element/>\n" +
            "    <second/>\n" +
            "\n" +
            "</root>\n",

            XmlFormatPreferences.defaults());
    }

    public void test2() throws Exception {
        XmlFormatPreferences prefs = XmlFormatPreferences.defaults();
        prefs.removeEmptyLines = true;

        check(
                // Before
                "<root>\n" +
                "\n" +
                "\n" +
                "[     <element/>\n" +
                "   <second/>\n" +
                "]\n" +
                "\n" +
                "\n" +
                "</root>\n",

                // Insert
                "    <element/>\n" +
                "    <second/>\n",

                // After
                "<root>\n" +
                "    <element/>\n" +
                "    <second/>\n" +
                "</root>\n",

                prefs);
    }

    public void test3() throws Exception {
        XmlFormatPreferences prefs = XmlFormatPreferences.defaults();
        prefs.removeEmptyLines = true;

        check(
                // Before
                "<root>\n" +
                "\n" +
                "\n" +
                "     [<element/>\n" +
                "   <second/>]\n" +
                "\n" +
                "\n" +
                "\n" +
                "</root>\n",

                // Insert
                "    <element/>\n" +
                "    <second/>",

                // After
                "<root>\n" +
                "    <element/>\n" +
                "    <second/>\n" +
                "</root>\n",

                prefs);
    }

    public void test4() throws Exception {
        check(
            "<RelativeLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
            "    xmlns:tools=\"http://schemas.android.com/tools\"\n" +
            "    android:layout_width=\"match_parent\"\n" +
            "    android:layout_height=\"match_parent\" >\n" +
            "\n" +
            "    [<TextView\n" +
            "        android:layout_width=\"wrap_content\"\n" +
            "        android:layout_height=\"wrap_content\"\n" +
            "          android:layout_centerHorizontal=\"true\"\n" +
            "        android:layout_centerVertical=\"true\"\n" +
            "        android:text=\"foo\"\n" +
            "        tools:context=\".MainActivity\" />]\n" +
            "\n" +
            "</RelativeLayout>\n",

            // Insert
            "\n" +
            "    <TextView\n" +
            "        android:layout_width=\"wrap_content\"\n" +
            "        android:layout_height=\"wrap_content\"\n" +
            "        android:layout_centerHorizontal=\"true\"\n" +
            "        android:layout_centerVertical=\"true\"\n" +
            "        android:text=\"foo\"\n" +
            "        tools:context=\".MainActivity\" />\n",

            // After
            "<RelativeLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
            "    xmlns:tools=\"http://schemas.android.com/tools\"\n" +
            "    android:layout_width=\"match_parent\"\n" +
            "    android:layout_height=\"match_parent\" >\n" +
            "\n" +
            "    <TextView\n" +
            "        android:layout_width=\"wrap_content\"\n" +
            "        android:layout_height=\"wrap_content\"\n" +
            "        android:layout_centerHorizontal=\"true\"\n" +
            "        android:layout_centerVertical=\"true\"\n" +
            "        android:text=\"foo\"\n" +
            "        tools:context=\".MainActivity\" />\n" +
            "\n" +
            "</RelativeLayout>\n",

            XmlFormatPreferences.defaults());
    }

    public void testCrLf1() throws Exception {
        check(
            // Before
            "<root>\r\n" +
            "[     <element/>\r\n" +
            "   <second/>\r\n" +
            "]\r\n" +
            "</root>\r\n",

            // Insert
            "    <element/>\r\n" +
            "    <second/>\r\n",

            // After
            "<root>\r\n" +
            "    <element/>\r\n" +
            "    <second/>\r\n" +
            "\r\n" +
            "</root>\r\n",

            XmlFormatPreferences.defaults());
    }

    public void testCrLf2() throws Exception {
        XmlFormatPreferences prefs = XmlFormatPreferences.defaults();
        prefs.removeEmptyLines = true;

        check(
                // Before
                "<root>\r\n" +
                "\r\n" +
                "\r\n" +
                "[     <element/>\r\n" +
                "   <second/>\r\n" +
                "]\r\n" +
                "\r\n" +
                "\r\n" +
                "</root>\r\n",

                // Insert
                "    <element/>\r\n" +
                "    <second/>\r\n",

                // After
                "<root>\r\n" +
                "    <element/>\r\n" +
                "    <second/>\r\n" +
                "</root>\r\n",

                prefs);
    }

    public void testCrLf3() throws Exception {
        XmlFormatPreferences prefs = XmlFormatPreferences.defaults();
        prefs.removeEmptyLines = true;

        check(
                // Before
                "<root>\r\n" +
                "\r\n" +
                "\r\n" +
                "     [<element/>\r\n" +
                "   <second/>]\r\n" +
                "\r\n" +
                "\r\n" +
                "\r\n" +
                "</root>\r\n",

                // Insert
                "    <element/>\r\n" +
                "    <second/>",

                // After
                "<root>\r\n" +
                "    <element/>\r\n" +
                "    <second/>\r\n" +
                "</root>\r\n",

                prefs);
    }


    public void testCrlf4() throws Exception {
        check(
            "<RelativeLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\r\n" +
            "    xmlns:tools=\"http://schemas.android.com/tools\"\r\n" +
            "    android:layout_width=\"match_parent\"\r\n" +
            "    android:layout_height=\"match_parent\" >\r\n" +
            "\r\n" +
            "    [<TextView\r\n" +
            "        android:layout_width=\"wrap_content\"\r\n" +
            "        android:layout_height=\"wrap_content\"\r\n" +
            "          android:layout_centerHorizontal=\"true\"\r\n" +
            "        android:layout_centerVertical=\"true\"\r\n" +
            "        android:text=\"foo\"\r\n" +
            "        tools:context=\".MainActivity\" />]\r\n" +
            "\r\n" +
            "</RelativeLayout>\r\n",

            // Insert
            "\r\n" +
            "    <TextView\r\n" +
            "        android:layout_width=\"wrap_content\"\r\n" +
            "        android:layout_height=\"wrap_content\"\r\n" +
            "        android:layout_centerHorizontal=\"true\"\r\n" +
            "        android:layout_centerVertical=\"true\"\r\n" +
            "        android:text=\"foo\"\r\n" +
            "        tools:context=\".MainActivity\" />\r\n",

            // After
            "<RelativeLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\r\n" +
            "    xmlns:tools=\"http://schemas.android.com/tools\"\r\n" +
            "    android:layout_width=\"match_parent\"\r\n" +
            "    android:layout_height=\"match_parent\" >\r\n" +
            "\r\n" +
            "    <TextView\r\n" +
            "        android:layout_width=\"wrap_content\"\r\n" +
            "        android:layout_height=\"wrap_content\"\r\n" +
            "        android:layout_centerHorizontal=\"true\"\r\n" +
            "        android:layout_centerVertical=\"true\"\r\n" +
            "        android:text=\"foo\"\r\n" +
            "        tools:context=\".MainActivity\" />\r\n" +
            "\r\n" +
            "</RelativeLayout>\r\n",

            XmlFormatPreferences.defaults());
    }
}

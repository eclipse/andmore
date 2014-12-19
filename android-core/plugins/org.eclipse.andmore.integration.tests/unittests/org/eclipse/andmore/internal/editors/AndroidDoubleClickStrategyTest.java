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
package org.eclipse.andmore.internal.editors;

import org.eclipse.swt.graphics.Point;

import junit.framework.TestCase;

@SuppressWarnings("javadoc")
public class AndroidDoubleClickStrategyTest extends TestCase {
    public void test_getWord_plain() throws Exception {
        checkWord("^foo", "[foo]");
        checkWord("'fo^o'", "'[foo]'");
        checkWord("\"foo^\"", "\"[foo]\"");
    }

    public void test_getWord_resources() throws Exception {
        checkWord("'@and^roid:string/ok'", "'[@android:string/ok]'");
        checkWord("'@android^:string/ok'", "'[@android:string/ok]'");
        checkWord("'^@android:string/ok'", "'[@android:string/ok]'");
        checkWord("'@android:^string/ok'", "'[@android:string/ok]'");
        checkWord("'@android:string^/ok'", "'[@android:string/ok]'");
        checkWord("'@android:string/^ok'", "'@android:string/[ok]'");
        checkWord("'@android:string/o^k'", "'@android:string/[ok]'");
        checkWord("'@android:string/ok^'", "'@android:string/[ok]'");
        checkWord("'@string/ok^'", "'@string/[ok]'");
        checkWord("'@str^ing/ok'", "'[@string/ok]'");
    }

    public void test_getWord_classnames() throws Exception {
        checkWord("\"co^m.example.templatetest1\"", "\"[com.example.templatetest1]\"");
        checkWord("\"com.exam^ple.templatetest1\"", "\"[com.example.templatetest1]\"");
        checkWord("\"com.example^.templatetest1\"", "\"[com.example.templatetest1]\"");
        checkWord("\"com.example.templat^etest1\"", "\"com.example.[templatetest1]\"");
        checkWord("\"com.example.^templatetest1\"", "\"com.example.[templatetest1]\"");
        checkWord("\"com.example.templatetest1^\"", "\"com.example.[templatetest1]\"");
        checkWord("\"...^\"", "\"[...]\"");
        checkWord("\"..^.\"", "\"[...]\"");
    }

    private void checkWord(String before, String expected) throws Exception {
        AndroidDoubleClickStrategy strategy = new AndroidDoubleClickStrategy();
        int cursor = before.indexOf('^');
        assertTrue("Must set cursor position with ^ in " + before, cursor != -1);
        before = before.substring(0, cursor) + before.substring(cursor + 1);
        assertEquals(-1, before.indexOf('^'));
        assertEquals(-1, before.indexOf('['));
        assertEquals(-1, before.indexOf(']'));

        Point positions = strategy.getWord(before, cursor);
        assertNotNull(positions);
        assertTrue(positions.y >= positions.x);
        String after = before.substring(0, positions.x) + '[' +
                before.substring(positions.x, positions.y) + ']' +
                before.substring(positions.y);
        assertEquals(expected, after);
    }
}

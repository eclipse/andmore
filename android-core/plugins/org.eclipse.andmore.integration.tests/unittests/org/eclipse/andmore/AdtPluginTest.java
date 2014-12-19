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
package org.eclipse.andmore;

import java.io.File;
import java.io.StringReader;

import junit.framework.TestCase;

public class AdtPluginTest extends TestCase {
    public void testReaderContains() throws Exception {
        String input = "this is a test";
        assertFalse(AdtPlugin.streamContains(new StringReader(input), "hello"));
        assertTrue(AdtPlugin.streamContains(new StringReader(input), "this"));
        assertFalse(AdtPlugin.streamContains(new StringReader(input), "thiss"));
        assertTrue(AdtPlugin.streamContains(new StringReader(input), "is a"));
        assertTrue(AdtPlugin.streamContains(new StringReader("ABC ABCDAB ABCDABCDABDE"),
                "ABCDABD"));
        assertFalse(AdtPlugin.streamContains(new StringReader("ABC ABCDAB ABCDABCDABDE"),
                "ABCEABD"));
    }

    public void testReadStream() throws Exception {
        String input = "this is a test";
        String contents = AdtPlugin.readFile(new StringReader(input));
        assertEquals(input, contents);
    }

    public void testReadWriteFile() throws Exception {
        File temp = File.createTempFile("test", ".txt");
        String myContent = "this is\na test";
        AdtPlugin.writeFile(temp, myContent);
        String readBack = AdtPlugin.readFile(temp);
        assertEquals(myContent, readBack);
    }
}

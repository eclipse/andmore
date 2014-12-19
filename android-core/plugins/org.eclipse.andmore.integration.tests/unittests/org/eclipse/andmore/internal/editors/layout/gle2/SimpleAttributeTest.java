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

import com.android.SdkConstants;

import junit.framework.TestCase;

public class SimpleAttributeTest extends TestCase {

    private SimpleAttribute a;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        a = new SimpleAttribute(SdkConstants.NS_RESOURCES, "name", "value = with space ");
    }

    public final void testSimpleAttribute() {
        assertEquals(SdkConstants.NS_RESOURCES, a.getUri());
        assertEquals("name", a.getName());
        assertEquals("value = with space ", a.getValue());
    }

    public final void testGetUri() {
        assertEquals(SdkConstants.NS_RESOURCES, a.getUri());
    }

    public final void testGetName() {
        assertEquals("name", a.getName());
    }

    public final void testGetValue() {
        assertEquals("value = with space ", a.getValue());
    }

    public final void testToString() {
        assertEquals(
                "@name:" + SdkConstants.NS_RESOURCES + "=value = with space \n",
                a.toString());
    }

    public final void testParseString() {
        String s = "@name:" + SdkConstants.NS_RESOURCES + "=value = with space \n";
        SimpleAttribute b = SimpleAttribute.parseString(s);
        assertEquals(a, b);
    }

    public final void testEqualsObject() {
        assertFalse(a.equals(null));
        assertFalse(a.equals(new Object()));

        // wrong name
        assertFalse(a.equals(new SimpleAttribute(SdkConstants.NS_RESOURCES,
                                                 "wrong name",
                                                 "value = with space ")));
        // wrong value
        assertFalse(a.equals(new SimpleAttribute(SdkConstants.NS_RESOURCES,
                                                 "name",
                                                 "value")));
        // wrong uri
        assertFalse(a.equals(new SimpleAttribute("uri", "name", "value = with space ")));
        // all fields wrong
        assertFalse(a.equals(new SimpleAttribute("uri", "wrong name", "value")));

        assertTrue(a.equals(new SimpleAttribute(SdkConstants.NS_RESOURCES,
                                                "name",
                                                "value = with space ")));
    }

    public final void testHashCode() {

        int ah = a.hashCode();

        assertFalse(ah == new Object().hashCode());

        // wrong name
        assertFalse(ah == new SimpleAttribute(SdkConstants.NS_RESOURCES,
                                              "wrong name",
                                              "value = with space ").hashCode());
        // wrong value
        assertFalse(ah == new SimpleAttribute(SdkConstants.NS_RESOURCES,
                                              "name",
                                              "value").hashCode());
        // wrong uri
        assertFalse(ah == new SimpleAttribute("uri", "name", "value = with space ").hashCode());
        // all fields wrong
        assertFalse(ah == new SimpleAttribute("uri", "wrong name", "value").hashCode());

        assertEquals(ah, new SimpleAttribute(SdkConstants.NS_RESOURCES,
                                             "name",
                                              "value = with space ").hashCode());
    }

}

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

package org.eclipse.andmore.common.layout;

import java.util.Arrays;
import java.util.Collections;

import junit.framework.TestCase;

public class BaseViewRuleTest extends TestCase {

    public final void testGetAttributeDisplayName() {
        assertEquals(null, BaseViewRule.getAttributeDisplayName(null));
        assertEquals("", BaseViewRule.getAttributeDisplayName(""));
        assertEquals("Foo", BaseViewRule.getAttributeDisplayName("foo"));
        assertEquals("FooBar", BaseViewRule.getAttributeDisplayName("fooBar"));
        assertEquals("Foo Bar", BaseViewRule.getAttributeDisplayName("foo_bar"));
        // TBD: Should we also handle CamelCase properties?
        // assertEquals("Foo Bar", BaseViewRule.getAttributeDisplayName("fooBar"));
    }

    public final void testJoin() {
        assertEquals("foo", BaseViewRule.join('|', Arrays.asList("foo")));
        assertEquals("", BaseViewRule.join('|', Collections.<String>emptyList()));
        assertEquals("foo,bar", BaseViewRule.join(',', Arrays.asList("foo", "bar")));
        assertEquals("foo|bar", BaseViewRule.join('|', Arrays.asList("foo", "bar")));
    }
}

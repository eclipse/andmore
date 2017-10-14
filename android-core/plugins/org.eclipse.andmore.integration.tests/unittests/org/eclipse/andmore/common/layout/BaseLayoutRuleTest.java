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

import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_ID;
import static org.junit.Assert.*;

import com.android.ide.common.api.IDragElement;
import com.android.ide.common.api.INode;
import com.android.ide.common.api.Rect;
import com.android.utils.Pair;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.andmore.common.layout.BaseLayoutRule.AttributeFilter;
import org.junit.Test;

// TODO: Check assertions
// TODO: Check equals() but not == strings by using new String("") to prevent interning
// TODO: Rename BaseLayout to BaseLayoutRule, and tests too of course

public class BaseLayoutRuleTest extends LayoutTestBase {

    /** Provides test data used by other test cases */
    private IDragElement[] createSampleElements() {
        IDragElement[] elements = TestDragElement.create(TestDragElement.create(
                "android.widget.Button", new Rect(0, 0, 100, 80)).id("@+id/Button01"),
                TestDragElement.create("android.widget.LinearLayout", new Rect(0, 80, 100, 280))
                        .id("@+id/LinearLayout01").add(
                                TestDragElement.create("android.widget.Button",
                                        new Rect(0, 80, 100, 80)).id("@+id/Button011"),
                                TestDragElement.create("android.widget.Button",
                                        new Rect(0, 180, 100, 80)).id("@+id/Button012")),
                TestDragElement.create("android.widget.Button", new Rect(100, 0, 100, 80)).id(
                        "@+id/Button02"));
        return elements;
    }

    /** Test {@link BaseLayoutRule#collectIds}: Check that basic lookup of id works */
    @Test
    public void testCollectIds1() {
        IDragElement[] elements = TestDragElement.create(TestDragElement.create(
                "android.widget.Button", new Rect(0, 0, 100, 80)).id("@+id/Button01"));
        Map<String, Pair<String, String>> idMap = new LinkedHashMap<String, Pair<String, String>>();
        new BaseLayoutRule();
		Map<String, Pair<String, String>> ids = BaseLayoutRule.collectIds(idMap, elements);
        assertEquals(1, ids.size());
        assertEquals("@+id/Button01", ids.keySet().iterator().next());
    }

    /**
     * Test {@link BaseLayoutRule#collectIds}: Check that with the wrong URI we
     * don't pick up the ID
     */
    @Test
    public void testCollectIds2() {
        IDragElement[] elements = TestDragElement.create(TestDragElement.create(
                "android.widget.Button", new Rect(0, 0, 100, 80)).set("myuri", ATTR_ID,
                "@+id/Button01"));

        Map<String, Pair<String, String>> idMap = new LinkedHashMap<String, Pair<String, String>>();
        new BaseLayoutRule();
		Map<String, Pair<String, String>> ids = BaseLayoutRule.collectIds(idMap, elements);
        assertEquals(0, ids.size());
    }

    /**
     * Test {@link BaseLayoutRule#normalizeId(String)}
     */
    @Test
    public void testNormalizeId() {
        new BaseLayoutRule();
		assertEquals("foo", BaseLayoutRule.normalizeId("foo"));
        new BaseLayoutRule();
		assertEquals("@+id/name", BaseLayoutRule.normalizeId("@id/name"));
        new BaseLayoutRule();
		assertEquals("@+id/name", BaseLayoutRule.normalizeId("@+id/name"));
    }

    /**
     * Test {@link BaseLayoutRule#collectExistingIds}
     */
    @Test
    public void testCollectExistingIds1() {
        Set<String> existing = new HashSet<String>();
        INode node = TestNode.create("android.widget.Button").id("@+id/Button012").add(
                TestNode.create("android.widget.Button").id("@+id/Button2"));

        new BaseLayoutRule();
		BaseLayoutRule.collectExistingIds(node, existing);

        assertEquals(2, existing.size());
        assertContainsSame(Arrays.asList("@+id/Button2", "@+id/Button012"), existing);
    }

    /**
     * Test {@link BaseLayoutRule#collectIds}: Check that with multiple elements and
     * some children we still pick up all the right id's
     */
    @Test
    public void testCollectIds3() {
        Map<String, Pair<String, String>> idMap = new LinkedHashMap<String, Pair<String, String>>();

        IDragElement[] elements = createSampleElements();
        new BaseLayoutRule();
		Map<String, Pair<String, String>> ids = BaseLayoutRule.collectIds(idMap, elements);
        assertEquals(5, ids.size());
        assertContainsSame(Arrays.asList("@+id/Button01", "@+id/Button02", "@+id/Button011",
                "@+id/Button012", "@+id/LinearLayout01"), ids.keySet());

        // Make sure the Pair has the right stuff too;
        // (having the id again in the pair seems redundant; see if I really
        // need it in the implementation)
        assertEquals(Pair.of("@+id/LinearLayout01", "android.widget.LinearLayout"), ids
                .get("@+id/LinearLayout01"));
    }

    /**
     * Test {@link BaseLayoutRule#remapIds}: Ensure that it identifies a conflict
     */
    @Test
    public void testRemapIds1() {
        Map<String, Pair<String, String>> idMap = new LinkedHashMap<String, Pair<String, String>>();
        BaseLayoutRule baseLayout = new BaseLayoutRule();
        IDragElement[] elements = createSampleElements();
        BaseLayoutRule.collectIds(idMap, elements);
        INode node = TestNode.create("android.widget.Button").id("@+id/Button012").add(
                TestNode.create("android.widget.Button").id("@+id/Button2"));

        assertEquals(5, idMap.size());
        Map<String, Pair<String, String>> remapped = BaseLayoutRule.remapIds(node, idMap);
        // 4 original from the sample elements, plus overlap with one
        // (Button012) - one new
        // button added in
        assertEquals(6, remapped.size());

        // TODO: I'm a little confused about what exactly this method should do;
        // check with Raphael.
    }


    /**
     * Test {@link BaseLayoutRule#getDropIdMap}
     */
    @Test
    public void testGetDropIdMap() {
        BaseLayoutRule baseLayout = new BaseLayoutRule();
        IDragElement[] elements = createSampleElements();
        INode node = TestNode.create("android.widget.Button").id("@+id/Button012").add(
                TestNode.create("android.widget.Button").id("@+id/Button2"));

        Map<String, Pair<String, String>> idMap = BaseLayoutRule.getDropIdMap(node, elements, true);
        assertContainsSame(Arrays.asList("@+id/Button01", "@+id/Button012", "@+id/Button011",
                "@id/Button012", "@+id/Button02", "@+id/LinearLayout01"), idMap
                .keySet());

        // TODO: I'm a little confused about what exactly this method should do;
        // check with Raphael.
    }

    @Test
    public void testAddAttributes1() {
        BaseLayoutRule layout = new BaseLayoutRule();

        // First try with no filter
        IDragElement oldElement = TestDragElement.create("a.w.B").id("@+id/foo");
        INode newNode = TestNode.create("a.w.B").id("@+id/foo").set("u", "key", "value").set("u",
                "nothidden", "nothiddenvalue");
        ;
        AttributeFilter filter = null;
        // No references in this test case
        Map<String, Pair<String, String>> idMap = null;

        BaseLayoutRule.addAttributes(newNode, oldElement, idMap, filter);
        assertEquals("value", newNode.getStringAttr("u", "key"));
        assertEquals("nothiddenvalue", newNode.getStringAttr("u", "nothidden"));
    }

    @Test
    public void testAddAttributes2() {
        // Test filtering
        BaseLayoutRule layout = new BaseLayoutRule();

        // First try with no filter
        IDragElement oldElement = TestDragElement.create("a.w.B").id("@+id/foo");
        INode newNode = TestNode.create("a.w.B").id("@+id/foo").set("u", "key", "value").set("u",
                "hidden", "hiddenvalue");
        AttributeFilter filter = new AttributeFilter() {

            @Override
            public String replace(String attributeUri, String attributeName,
                    String attributeValue) {
                if (attributeName.equals("hidden")) {
                    return null;
                }

                return attributeValue;
            }
        };
        // No references in this test case
        Map<String, Pair<String, String>> idMap = null;

        BaseLayoutRule.addAttributes(newNode, oldElement, idMap, filter);
        assertEquals("value", newNode.getStringAttr("u", "key"));
    }

    @Test
    public void testFindNewId() {
        BaseLayoutRule baseLayout = new BaseLayoutRule();
        Set<String> existing = new HashSet<String>();
        assertEquals("@+id/Widget01", BaseLayoutRule.findNewId("a.w.Widget", existing));

        existing.add("@+id/Widget01");
        assertEquals("@+id/Widget02", BaseLayoutRule.findNewId("a.w.Widget", existing));

        existing.add("@+id/Widget02");
        assertEquals("@+id/Widget03", BaseLayoutRule.findNewId("a.w.Widget", existing));

        existing.remove("@+id/Widget02");
        assertEquals("@+id/Widget02", BaseLayoutRule.findNewId("a.w.Widget", existing));
    }

    @Test
    public void testDefaultAttributeFilter() {
        assertEquals("true", BaseLayoutRule.DEFAULT_ATTR_FILTER.replace("myuri", "layout_alignRight",
                "true"));
        assertEquals(null, BaseLayoutRule.DEFAULT_ATTR_FILTER.replace(ANDROID_URI,
                "layout_alignRight", "true"));
        assertEquals("true", BaseLayoutRule.DEFAULT_ATTR_FILTER.replace(ANDROID_URI,
                "myproperty", "true"));
    }

    @Test
    public void testAddInnerElements() {
        IDragElement oldElement = TestDragElement.create("root").add(
                TestDragElement.create("a.w.B").id("@+id/child1")
                        .set("uri", "childprop1", "value1"),
                TestDragElement.create("a.w.B").id("@+id/child2").set("uri", "childprop2a",
                        "value2a").set("uri", "childprop2b", "value2b"));
        INode newNode = TestNode.create("a.w.B").id("@+id/foo");
        Map<String, Pair<String, String>> idMap = new LinkedHashMap<String, Pair<String, String>>();
        BaseLayoutRule layout = new BaseLayoutRule();
        BaseLayoutRule.addInnerElements(newNode, oldElement, idMap);
        assertEquals(2, newNode.getChildren().length);

        assertEquals("value2b", newNode.getChildren()[1].getStringAttr("uri", "childprop2b"));
    }
}

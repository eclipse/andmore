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

import com.android.ide.common.rendering.api.Capability;
import com.android.ide.common.rendering.api.DataBindingItem;
import com.android.ide.common.rendering.api.MergeCookie;
import com.android.ide.common.rendering.api.ViewInfo;
import com.android.utils.Pair;

import org.eclipse.andmore.internal.editors.descriptors.AttributeDescriptor;
import org.eclipse.andmore.internal.editors.descriptors.ElementDescriptor;
import org.eclipse.andmore.internal.editors.layout.descriptors.ViewElementDescriptor;
import org.eclipse.andmore.internal.editors.layout.uimodel.UiViewElementNode;
import org.eclipse.swt.graphics.Rectangle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

@SuppressWarnings("javadoc")
public class CanvasViewInfoTest extends TestCase {

    public static ViewElementDescriptor createDesc(String name, String fqn, boolean hasChildren) {
        if (hasChildren) {
            return new ViewElementDescriptor(name, name, fqn, "", "", new AttributeDescriptor[0],
                    new AttributeDescriptor[0], new ElementDescriptor[1], false);
        } else {
            return new ViewElementDescriptor(name, fqn);
        }
    }

    public static UiViewElementNode createNode(UiViewElementNode parent, String fqn,
            boolean hasChildren) {
        String name = fqn.substring(fqn.lastIndexOf('.') + 1);
        ViewElementDescriptor descriptor = createDesc(name, fqn, hasChildren);
        if (parent == null) {
            // All node hierarchies should be wrapped inside a document node at the root
            parent = new UiViewElementNode(createDesc("doc", "doc", true));
        }
        return (UiViewElementNode) parent.appendNewUiChild(descriptor);
    }

    public static UiViewElementNode createNode(String fqn, boolean hasChildren) {
        return createNode(null, fqn, hasChildren);
    }

    public void testNormalCreate() throws Exception {
        normal(true);
    }

    public void testNormalCreateLayoutLib5() throws Exception {
        normal(false);
    }

    private void normal(boolean layoutlib5) {

        // Normal view hierarchy, no null keys anywhere

        UiViewElementNode rootNode = createNode("android.widget.LinearLayout", true);
        ViewInfo root = new ViewInfo("LinearLayout", rootNode, 10, 10, 100, 100);
        UiViewElementNode child1Node = createNode(rootNode, "android.widget.Button", false);
        ViewInfo child1 = new ViewInfo("Button", child1Node, 0, 0, 50, 20);
        UiViewElementNode child2Node = createNode(rootNode, "android.widget.Button", false);
        ViewInfo child2 = new ViewInfo("Button", child2Node, 0, 20, 70, 25);
        root.setChildren(Arrays.asList(child1, child2));

        CanvasViewInfo rootView = CanvasViewInfo.create(root, layoutlib5).getFirst();
        assertNotNull(rootView);
        assertEquals("LinearLayout", rootView.getName());
        assertEquals(new Rectangle(10, 10, 89, 89), rootView.getAbsRect());
        assertEquals(new Rectangle(10, 10, 89, 89), rootView.getSelectionRect());
        assertNull(rootView.getParent());
        assertSame(rootView.getUiViewNode(), rootNode);
        assertEquals(2, rootView.getChildren().size());
        CanvasViewInfo childView1 = rootView.getChildren().get(0);
        CanvasViewInfo childView2 = rootView.getChildren().get(1);

        assertEquals("Button", childView1.getName());
        assertSame(rootView, childView1.getParent());
        assertEquals(new Rectangle(10, 10, 49, 19), childView1.getAbsRect());
        assertEquals(new Rectangle(10, 10, 49, 19), childView1.getSelectionRect());
        assertSame(childView1.getUiViewNode(), child1Node);

        assertEquals("Button", childView2.getName());
        assertSame(rootView, childView2.getParent());
        assertEquals(new Rectangle(10, 30, 69, 4), childView2.getAbsRect());
        assertEquals(new Rectangle(10, 30, 69, 5), childView2.getSelectionRect());
        assertSame(childView2.getUiViewNode(), child2Node);
    }

    public void testShowIn() throws Exception {
        showIn(false);
    }

    public void testShowInLayoutLib5() throws Exception {
        showIn(true);
    }

    public void showIn(boolean layoutlib5) throws Exception {

        // Test rendering of "Show Included In" (included content rendered
        // within an outer content that has null keys)

        ViewInfo root = new ViewInfo("LinearLayout", null, 10, 10, 100, 100);
        ViewInfo child1 = new ViewInfo("CheckBox", null, 0, 0, 50, 20);
        UiViewElementNode child2Node = createNode("android.widget.RelativeLayout", true);
        ViewInfo child2 = new ViewInfo("RelativeLayout", child2Node, 0, 20, 70, 25);
        root.setChildren(Arrays.asList(child1, child2));
        UiViewElementNode child21Node = createNode("android.widget.Button", false);
        ViewInfo child21 = new ViewInfo("RadioButton", child21Node, 0, 20, 70, 25);
        child2.setChildren(Arrays.asList(child21));

        CanvasViewInfo rootView = CanvasViewInfo.create(root, layoutlib5).getFirst();
        assertNotNull(rootView);
        assertEquals("LinearLayout", rootView.getName());
        assertEquals(new Rectangle(10, 10, 89, 89), rootView.getAbsRect());
        assertEquals(new Rectangle(10, 10, 89, 89), rootView.getSelectionRect());
        assertNull(rootView.getParent());
        assertNull(rootView.getUiViewNode());
        assertEquals(1, rootView.getChildren().size());
        CanvasViewInfo includedView = rootView.getChildren().get(0);

        assertEquals("RelativeLayout", includedView.getName());
        assertSame(rootView, includedView.getParent());
        assertEquals(new Rectangle(10, 30, 69, 4), includedView.getAbsRect());
        assertEquals(new Rectangle(10, 30, 69, 5), includedView.getSelectionRect());
        assertSame(includedView.getUiViewNode(), child2Node);

        CanvasViewInfo grandChild = includedView.getChildren().get(0);
        assertNotNull(grandChild);
        assertEquals("RadioButton", grandChild.getName());
        assertSame(child21Node, grandChild.getUiViewNode());
        assertEquals(new Rectangle(10, 50, 69, 4), grandChild.getAbsRect());
        assertEquals(new Rectangle(10, 50, 69, 5), grandChild.getSelectionRect());
    }

    public void testIncludeTag() throws Exception {
        boolean layoutlib5 = true;

        // Test rendering of included views on layoutlib 5+ (e.g. has <include> tag)

        UiViewElementNode rootNode = createNode("android.widget.LinearLayout", true);
        ViewInfo root = new ViewInfo("LinearLayout", rootNode, 10, 10, 100, 100);
        UiViewElementNode child1Node = createNode(rootNode, "android.widget.Button", false);
        ViewInfo child1 = new ViewInfo("CheckBox", child1Node, 0, 0, 50, 20);
        UiViewElementNode child2Node = createNode(rootNode, "include", true);
        ViewInfo child2 = new ViewInfo("RelativeLayout", child2Node, 0, 20, 70, 25);
        root.setChildren(Arrays.asList(child1, child2));
        ViewInfo child21 = new ViewInfo("RadioButton", null, 0, 20, 70, 25);
        child2.setChildren(Arrays.asList(child21));

        CanvasViewInfo rootView = CanvasViewInfo.create(root, layoutlib5).getFirst();
        assertNotNull(rootView);
        assertEquals("LinearLayout", rootView.getName());
        assertEquals(new Rectangle(10, 10, 89, 89), rootView.getAbsRect());
        assertEquals(new Rectangle(10, 10, 89, 89), rootView.getSelectionRect());
        assertNull(rootView.getParent());
        assertSame(rootNode, rootView.getUiViewNode());
        assertEquals(2, rootView.getChildren().size());

        CanvasViewInfo childView1 = rootView.getChildren().get(0);
        CanvasViewInfo includedView = rootView.getChildren().get(1);

        assertEquals("CheckBox", childView1.getName());
        assertSame(rootView, childView1.getParent());
        assertEquals(new Rectangle(10, 10, 49, 19), childView1.getAbsRect());
        assertEquals(new Rectangle(10, 10, 49, 19), childView1.getSelectionRect());
        assertSame(childView1.getUiViewNode(), child1Node);

        assertEquals("RelativeLayout", includedView.getName());
        assertSame(rootView, includedView.getParent());
        assertEquals(new Rectangle(10, 30, 69, 4), includedView.getAbsRect());
        assertEquals(new Rectangle(10, 30, 69, 5), includedView.getSelectionRect());
        assertSame(includedView.getUiViewNode(), child2Node);
        assertEquals(0, includedView.getChildren().size());
    }

    public void testNoIncludeTag() throws Exception {
        boolean layoutlib5 = false;

        // Test rendering of included views on layoutlib 4- (e.g. no <include> tag cookie
        // in view info)

        UiViewElementNode rootNode = createNode("android.widget.LinearLayout", true);
        ViewInfo root = new ViewInfo("LinearLayout", rootNode, 10, 10, 100, 100);
        UiViewElementNode child1Node = createNode(rootNode, "android.widget.Button", false);
        ViewInfo child1 = new ViewInfo("CheckBox", child1Node, 0, 0, 50, 20);
        UiViewElementNode child2Node = createNode(rootNode, "include", true);
        ViewInfo child2 = new ViewInfo("RelativeLayout", null /* layoutlib 4 */, 0, 20, 70, 25);
        root.setChildren(Arrays.asList(child1, child2));
        ViewInfo child21 = new ViewInfo("RadioButton", null, 0, 20, 70, 25);
        child2.setChildren(Arrays.asList(child21));

        CanvasViewInfo rootView = CanvasViewInfo.create(root, layoutlib5).getFirst();
        assertNotNull(rootView);
        assertEquals("LinearLayout", rootView.getName());
        assertEquals(new Rectangle(10, 10, 89, 89), rootView.getAbsRect());
        assertEquals(new Rectangle(10, 10, 89, 89), rootView.getSelectionRect());
        assertNull(rootView.getParent());
        assertSame(rootNode, rootView.getUiViewNode());
        assertEquals(2, rootView.getChildren().size());

        CanvasViewInfo childView1 = rootView.getChildren().get(0);
        CanvasViewInfo includedView = rootView.getChildren().get(1);

        assertEquals("CheckBox", childView1.getName());
        assertSame(rootView, childView1.getParent());
        assertEquals(new Rectangle(10, 10, 49, 19), childView1.getAbsRect());
        assertEquals(new Rectangle(10, 10, 49, 19), childView1.getSelectionRect());
        assertSame(childView1.getUiViewNode(), child1Node);

        assertEquals("RelativeLayout", includedView.getName());
        assertSame(rootView, includedView.getParent());
        assertEquals(new Rectangle(10, 30, 69, 4), includedView.getAbsRect());
        assertEquals(new Rectangle(10, 30, 69, 5), includedView.getSelectionRect());
        assertSame(includedView.getUiViewNode(), child2Node);
        assertEquals(0, includedView.getChildren().size());
    }

    public void testMergeMatching() throws Exception {
        boolean layoutlib5 = false;

        // Test rendering of MULTIPLE included views or when there is no simple match
        // between view info and ui element node children

        UiViewElementNode rootNode = createNode("android.widget.LinearLayout", true);
        ViewInfo root = new ViewInfo("LinearLayout", rootNode, 10, 10, 100, 100);
        UiViewElementNode child1Node = createNode(rootNode, "android.widget.Button", false);
        ViewInfo child1 = new ViewInfo("CheckBox", child1Node, 0, 0, 50, 20);
        UiViewElementNode multiChildNode1 = createNode(rootNode, "foo", true);
        UiViewElementNode multiChildNode2 = createNode(rootNode, "bar", true);
        ViewInfo child2 = new ViewInfo("RelativeLayout", null, 0, 20, 70, 25);
        ViewInfo child3 = new ViewInfo("AbsoluteLayout", null, 10, 40, 50, 15);
        root.setChildren(Arrays.asList(child1, child2, child3));
        ViewInfo child21 = new ViewInfo("RadioButton", null, 0, 20, 70, 25);
        child2.setChildren(Arrays.asList(child21));

        CanvasViewInfo rootView = CanvasViewInfo.create(root, layoutlib5).getFirst();
        assertNotNull(rootView);
        assertEquals("LinearLayout", rootView.getName());
        assertEquals(new Rectangle(10, 10, 89, 89), rootView.getAbsRect());
        assertEquals(new Rectangle(10, 10, 89, 89), rootView.getSelectionRect());
        assertNull(rootView.getParent());
        assertTrue(rootView.isRoot());
        assertSame(rootNode, rootView.getUiViewNode());
        assertEquals(3, rootView.getChildren().size());

        CanvasViewInfo childView1 = rootView.getChildren().get(0);
        assertFalse(childView1.isRoot());
        CanvasViewInfo includedView1 = rootView.getChildren().get(1);
        assertFalse(includedView1.isRoot());
        CanvasViewInfo includedView2 = rootView.getChildren().get(2);
        assertFalse(includedView1.isRoot());

        assertEquals("CheckBox", childView1.getName());
        assertSame(rootView, childView1.getParent());
        assertEquals(new Rectangle(10, 10, 49, 19), childView1.getAbsRect());
        assertEquals(new Rectangle(10, 10, 49, 19), childView1.getSelectionRect());
        assertSame(childView1.getUiViewNode(), child1Node);

        assertEquals("RelativeLayout", includedView1.getName());
        assertSame(multiChildNode1, includedView1.getUiViewNode());
        assertEquals("foo", includedView1.getUiViewNode().getDescriptor().getXmlName());
        assertSame(multiChildNode2, includedView2.getUiViewNode());
        assertEquals("AbsoluteLayout", includedView2.getName());
        assertEquals("bar", includedView2.getUiViewNode().getDescriptor().getXmlName());
        assertSame(rootView, includedView1.getParent());
        assertSame(rootView, includedView2.getParent());
        assertEquals(new Rectangle(10, 30, 69, 4), includedView1.getAbsRect());
        assertEquals(new Rectangle(10, 30, 69, 5), includedView1.getSelectionRect());
        assertEquals(new Rectangle(20, 50, 39, -26), includedView2.getAbsRect());
        assertEquals(new Rectangle(20, 35, 39, 5), includedView2.getSelectionRect());
        assertEquals(0, includedView1.getChildren().size());
        assertEquals(0, includedView2.getChildren().size());
    }

    public void testMerge() throws Exception {
        boolean layoutlib5 = false;

        // Test rendering of MULTIPLE included views or when there is no simple match
        // between view info and ui element node children

        UiViewElementNode rootNode = createNode("android.widget.LinearLayout", true);
        ViewInfo root = new ViewInfo("LinearLayout", rootNode, 10, 10, 100, 100);
        UiViewElementNode child1Node = createNode(rootNode, "android.widget.Button", false);
        ViewInfo child1 = new ViewInfo("CheckBox", child1Node, 0, 0, 50, 20);
        UiViewElementNode multiChildNode = createNode(rootNode, "foo", true);
        ViewInfo child2 = new ViewInfo("RelativeLayout", null, 0, 20, 70, 25);
        ViewInfo child3 = new ViewInfo("AbsoluteLayout", null, 10, 40, 50, 15);
        root.setChildren(Arrays.asList(child1, child2, child3));
        ViewInfo child21 = new ViewInfo("RadioButton", null, 0, 20, 70, 25);
        child2.setChildren(Arrays.asList(child21));

        CanvasViewInfo rootView = CanvasViewInfo.create(root, layoutlib5).getFirst();
        assertNotNull(rootView);
        assertEquals("LinearLayout", rootView.getName());
        assertEquals(new Rectangle(10, 10, 89, 89), rootView.getAbsRect());
        assertEquals(new Rectangle(10, 10, 89, 89), rootView.getSelectionRect());
        assertNull(rootView.getParent());
        assertSame(rootNode, rootView.getUiViewNode());
        assertEquals(2, rootView.getChildren().size());

        CanvasViewInfo childView1 = rootView.getChildren().get(0);
        CanvasViewInfo includedView = rootView.getChildren().get(1);

        assertEquals("CheckBox", childView1.getName());
        assertSame(rootView, childView1.getParent());
        assertEquals(new Rectangle(10, 10, 49, 19), childView1.getAbsRect());
        assertEquals(new Rectangle(10, 10, 49, 19), childView1.getSelectionRect());
        assertSame(childView1.getUiViewNode(), child1Node);

        assertEquals("RelativeLayout", includedView.getName());
        assertSame(rootView, includedView.getParent());
        assertEquals(new Rectangle(10, 30, 69, 4), includedView.getAbsRect());
        assertEquals(new Rectangle(10, 30, 69, 5), includedView.getSelectionRect());
        assertEquals(0, includedView.getChildren().size());
        assertSame(multiChildNode, includedView.getUiViewNode());
    }

    public void testInsertMerge() throws Exception {
        boolean layoutlib5 = false;

        // Test rendering of MULTIPLE included views or when there is no simple match
        // between view info and ui element node children

        UiViewElementNode mergeNode = createNode("merge", true);
        UiViewElementNode rootNode = createNode(mergeNode, "android.widget.Button", false);
        ViewInfo root = new ViewInfo("Button", rootNode, 10, 10, 100, 100);

        CanvasViewInfo rootView = CanvasViewInfo.create(root, layoutlib5).getFirst();
        assertNotNull(rootView);
        assertEquals("merge", rootView.getName());
        assertSame(rootView.getUiViewNode(), mergeNode);
        assertEquals(new Rectangle(10, 10, 89, 89), rootView.getAbsRect());
        assertEquals(new Rectangle(10, 10, 89, 89), rootView.getSelectionRect());
        assertNull(rootView.getParent());
        assertSame(mergeNode, rootView.getUiViewNode());
        assertEquals(1, rootView.getChildren().size());

        CanvasViewInfo childView1 = rootView.getChildren().get(0);

        assertEquals("Button", childView1.getName());
        assertSame(rootView, childView1.getParent());
        assertEquals(new Rectangle(10, 10, 89, 89), childView1.getAbsRect());
        assertEquals(new Rectangle(10, 10, 89, 89), childView1.getSelectionRect());
        assertSame(childView1.getUiViewNode(), rootNode);
    }

    public void testUnmatchedMissing() throws Exception {
        boolean layoutlib5 = false;

        UiViewElementNode rootNode = createNode("android.widget.LinearLayout", true);
        ViewInfo root = new ViewInfo("LinearLayout", rootNode, 0, 0, 100, 100);
        List<ViewInfo> children = new ArrayList<ViewInfo>();
        // Should be matched up with corresponding node:
        Set<Integer> missingKeys = new HashSet<Integer>();
        // Should not be matched with any views, but should get view created:
        Set<Integer> extraKeys = new HashSet<Integer>();
        // Should not be matched with any nodes
        Set<Integer> extraViews = new HashSet<Integer>();
        int numViews = 30;
        missingKeys.add(0);
        missingKeys.add(4);
        missingKeys.add(14);
        missingKeys.add(29);
        extraKeys.add(9);
        extraKeys.add(20);
        extraKeys.add(22);
        extraViews.add(18);
        extraViews.add(24);

        List<String> expectedViewNames = new ArrayList<String>();
        List<String> expectedNodeNames = new ArrayList<String>();

        for (int i = 0; i < numViews; i++) {
            UiViewElementNode childNode = null;
            if (!extraViews.contains(i)) {
                childNode = createNode(rootNode, "childNode" + i, false);
            }
            Object cookie = missingKeys.contains(i) || extraViews.contains(i) ? null : childNode;
            ViewInfo childView = new ViewInfo("childView" + i, cookie,
                    0, i * 20, 50, (i + 1) * 20);
            children.add(childView);

            if (!extraViews.contains(i)) {
                expectedViewNames.add("childView" + i);
                expectedNodeNames.add("childNode" + i);
            }

            if (extraKeys.contains(i)) {
                createNode(rootNode, "extraNodeAt" + i, false);

                expectedViewNames.add("extraNodeAt" + i);
                expectedNodeNames.add("extraNodeAt" + i);
            }
        }
        root.setChildren(children);

        CanvasViewInfo rootView = CanvasViewInfo.create(root, layoutlib5).getFirst();
        assertNotNull(rootView);

        // dump(root, 0);
        // dump(rootView, 0);

        assertEquals("LinearLayout", rootView.getName());
        assertNull(rootView.getParent());
        assertSame(rootNode, rootView.getUiViewNode());
        assertEquals(numViews + extraKeys.size() - extraViews.size(), rootNode.getUiChildren()
                .size());
        assertEquals(numViews + extraKeys.size() - extraViews.size(),
                rootView.getChildren().size());
        assertEquals(expectedViewNames.size(), rootView.getChildren().size());
        for (int i = 0, n = rootView.getChildren().size(); i < n; i++) {
            CanvasViewInfo childView = rootView.getChildren().get(i);
            String expectedViewName = expectedViewNames.get(i);
            String expectedNodeName = expectedNodeNames.get(i);
            assertEquals(expectedViewName, childView.getName());
            assertNotNull(childView.getUiViewNode());
            assertEquals(expectedNodeName, childView.getUiViewNode().getDescriptor().getXmlName());
        }
    }

    public void testMergeCookies() throws Exception {
        boolean layoutlib5 = true;

        UiViewElementNode rootNode = createNode("android.widget.LinearLayout", true);
        ViewInfo root = new ViewInfo("LinearLayout", rootNode, 0, 0, 100, 100);

        // Create the merge cookies in the opposite order to ensure that we don't
        // apply our own logic when matching up views with nodes
        LinkedList<MergeCookie> cookies = new LinkedList<MergeCookie>();
        for (int i = 0; i < 10; i++) {
            UiViewElementNode node = createNode(rootNode, "childNode" + i, false);
            cookies.addFirst(new MergeCookie(node));
        }
        Iterator<MergeCookie> it = cookies.iterator();
        ArrayList<ViewInfo> children = new ArrayList<ViewInfo>();
        for (int i = 0; i < 10; i++) {
            ViewInfo childView = new ViewInfo("childView" + i, it.next(), 0, i * 20, 50,
                    (i + 1) * 20);
            children.add(childView);
        }
        root.setChildren(children);

        CanvasViewInfo rootView = CanvasViewInfo.create(root, layoutlib5).getFirst();
        assertNotNull(rootView);

        assertEquals("LinearLayout", rootView.getName());
        assertNull(rootView.getParent());
        assertSame(rootNode, rootView.getUiViewNode());
        for (int i = 0, n = rootView.getChildren().size(); i < n; i++) {
            CanvasViewInfo childView = rootView.getChildren().get(i);
            assertEquals("childView" + i, childView.getName());
            assertEquals("childNode" + (9 - i), childView.getUiViewNode().getDescriptor()
                    .getXmlName());
        }
    }

    public void testMergeCookies2() throws Exception {
        boolean layoutlib5 = true;

        UiViewElementNode rootNode = createNode("android.widget.LinearLayout", true);
        ViewInfo root = new ViewInfo("LinearLayout", rootNode, 0, 0, 100, 100);

        UiViewElementNode node1 = createNode(rootNode, "childNode1", false);
        UiViewElementNode node2 = createNode(rootNode, "childNode2", false);
        MergeCookie cookie1 = new MergeCookie(node1);
        MergeCookie cookie2 = new MergeCookie(node2);

        // Sets alternating merge cookies and checks whether the node sibling lists are
        // okay and merged correctly

        ArrayList<ViewInfo> children = new ArrayList<ViewInfo>();
        for (int i = 0; i < 10; i++) {
            Object cookie = (i % 2) == 0 ? cookie1 : cookie2;
            ViewInfo childView = new ViewInfo("childView" + i, cookie, 0, i * 20, 50,
                    (i + 1) * 20);
            children.add(childView);
        }
        root.setChildren(children);

        Pair<CanvasViewInfo, List<Rectangle>> result = CanvasViewInfo.create(root, layoutlib5);
        CanvasViewInfo rootView = result.getFirst();
        List<Rectangle> bounds = result.getSecond();
        assertNull(bounds);
        assertNotNull(rootView);

        assertEquals("LinearLayout", rootView.getName());
        assertNull(rootView.getParent());
        assertSame(rootNode, rootView.getUiViewNode());
        assertEquals(10, rootView.getChildren().size());
        assertEquals(2, rootView.getUniqueChildren().size());
        for (int i = 0, n = rootView.getChildren().size(); i < n; i++) {
            CanvasViewInfo childView = rootView.getChildren().get(i);
            assertEquals("childView" + i, childView.getName());
            Object cookie = (i % 2) == 0 ? node1 : node2;
            assertSame(cookie, childView.getUiViewNode());
            List<CanvasViewInfo> nodeSiblings = childView.getNodeSiblings();
            assertEquals(5, nodeSiblings.size());
        }
        List<CanvasViewInfo> nodeSiblings = rootView.getChildren().get(0).getNodeSiblings();
        for (int j = 0; j < 5; j++) {
            assertEquals("childView" + (j * 2), nodeSiblings.get(j).getName());
        }
        nodeSiblings = rootView.getChildren().get(1).getNodeSiblings();
        for (int j = 0; j < 5; j++) {
            assertEquals("childView" + (j * 2 + 1), nodeSiblings.get(j).getName());
        }
    }

    public void testIncludeBounds() throws Exception {
        boolean layoutlib5 = true;

        UiViewElementNode rootNode = createNode("android.widget.LinearLayout", true);
        ViewInfo root = new ViewInfo("included", null, 0, 0, 100, 100);

        UiViewElementNode node1 = createNode(rootNode, "childNode1", false);
        UiViewElementNode node2 = createNode(rootNode, "childNode2", false);
        MergeCookie cookie1 = new MergeCookie(node1);
        MergeCookie cookie2 = new MergeCookie(node2);

        // Sets alternating merge cookies and checks whether the node sibling lists are
        // okay and merged correctly

        ArrayList<ViewInfo> children = new ArrayList<ViewInfo>();
        for (int i = 0; i < 10; i++) {
            Object cookie = (i % 2) == 0 ? cookie1 : cookie2;
            ViewInfo childView = new ViewInfo("childView" + i, cookie, 0, i * 20, 50,
                    (i + 1) * 20);
            children.add(childView);
        }
        root.setChildren(children);

        Pair<CanvasViewInfo, List<Rectangle>> result = CanvasViewInfo.create(root, layoutlib5);
        CanvasViewInfo rootView = result.getFirst();
        List<Rectangle> bounds = result.getSecond();
        assertNotNull(rootView);

        assertEquals("included", rootView.getName());
        assertNull(rootView.getParent());
        assertNull(rootView.getUiViewNode());
        assertEquals(10, rootView.getChildren().size());
        assertEquals(2, rootView.getUniqueChildren().size());
        for (int i = 0, n = rootView.getChildren().size(); i < n; i++) {
            CanvasViewInfo childView = rootView.getChildren().get(i);
            assertEquals("childView" + i, childView.getName());
            Object cookie = (i % 2) == 0 ? node1 : node2;
            assertSame(cookie, childView.getUiViewNode());
            List<CanvasViewInfo> nodeSiblings = childView.getNodeSiblings();
            assertEquals(5, nodeSiblings.size());
        }
        List<CanvasViewInfo> nodeSiblings = rootView.getChildren().get(0).getNodeSiblings();
        for (int j = 0; j < 5; j++) {
            assertEquals("childView" + (j * 2), nodeSiblings.get(j).getName());
        }
        nodeSiblings = rootView.getChildren().get(1).getNodeSiblings();
        for (int j = 0; j < 5; j++) {
            assertEquals("childView" + (j * 2 + 1), nodeSiblings.get(j).getName());
        }

        // Only show the primary bounds as included
        assertEquals(2, bounds.size());
        assertEquals(new Rectangle(0, 0, 49, 19), bounds.get(0));
        assertEquals(new Rectangle(0, 20, 49, 19), bounds.get(1));
    }

    public void testIncludeBounds2() throws Exception {
        includeBounds2(false);
    }

    public void testIncludeBounds2LayoutLib5() throws Exception {
        includeBounds2(true);
    }

    public void includeBounds2(boolean layoutlib5) throws Exception {

        UiViewElementNode rootNode = createNode("android.widget.LinearLayout", true);
        ViewInfo root = new ViewInfo("included", null, 0, 0, 100, 100);

        UiViewElementNode node1 = createNode(rootNode, "childNode1", false);
        UiViewElementNode node2 = createNode(rootNode, "childNode2", false);

        ViewInfo childView1 = new ViewInfo("childView1", node1, 0, 20, 50, 40);
        ViewInfo childView2 = new ViewInfo("childView2", node2, 0, 40, 50, 60);

        root.setChildren(Arrays.asList(childView1, childView2));

        Pair<CanvasViewInfo, List<Rectangle>> result = CanvasViewInfo.create(root, layoutlib5);
        CanvasViewInfo rootView = result.getFirst();
        List<Rectangle> bounds = result.getSecond();
        assertNotNull(rootView);

        assertEquals("included", rootView.getName());
        assertNull(rootView.getParent());
        assertNull(rootView.getUiViewNode());
        assertEquals(2, rootView.getChildren().size());
        assertEquals(2, rootView.getUniqueChildren().size());

        Rectangle bounds1 = bounds.get(0);
        Rectangle bounds2 = bounds.get(1);
        assertEquals(new Rectangle(0, 20, 49, 19), bounds1);
        assertEquals(new Rectangle(0, 40, 49, 19), bounds2);
    }

    public void testCookieWorkaround() throws Exception {
        UiViewElementNode rootNode = createNode("android.widget.LinearLayout", true);
        ViewInfo root = new ViewInfo("included", null, 0, 0, 100, 100);

        UiViewElementNode node2 = createNode(rootNode, "childNode2", false);
        MergeCookie mergeCookie = new MergeCookie(root);

        ViewInfo childView1 = new ViewInfo("childView1", mergeCookie, 0, 20, 50, 40);
        ViewInfo childView2 = new ViewInfo("childView2", node2, 0, 40, 50, 60);

        root.setChildren(Arrays.asList(childView1, childView2));

        Pair<CanvasViewInfo, List<Rectangle>> result = CanvasViewInfo.create(root, true);
        CanvasViewInfo rootView = result.getFirst();
        List<Rectangle> bounds = result.getSecond();
        assertNotNull(rootView);

        assertEquals("included", rootView.getName());
        assertNull(rootView.getParent());
        assertNull(rootView.getUiViewNode());
        // childView1 should have been removed since it has the wrong merge cookie
        assertEquals(1, rootView.getChildren().size());
        assertEquals(1, rootView.getUniqueChildren().size());

        Rectangle bounds1 = bounds.get(0);
        assertEquals(new Rectangle(0, 40, 49, 19), bounds1);
    }

    public void testGestureOverlayView() throws Exception {
        boolean layoutlib5 = true;

        // Test rendering of included views on layoutlib 5+ (e.g. has <include> tag)

        UiViewElementNode rootNode = createNode("android.gesture.GestureOverlayView", true);
        UiViewElementNode childNode = createNode(rootNode, "android.widget.LinearLayout", false);
        UiViewElementNode grandChildNode = createNode(childNode, "android.widget.Button", false);
        ViewInfo root = new ViewInfo("GestureOverlayView", rootNode, 10, 10, 100, 100);
        ViewInfo child = new ViewInfo("LinearLayout", childNode, 0, 0, 50, 20);
        root.setChildren(Collections.singletonList(child));
        ViewInfo grandChild = new ViewInfo("Button", grandChildNode, 0, 20, 70, 25);
        child.setChildren(Collections.singletonList(grandChild));
        CanvasViewInfo rootView = CanvasViewInfo.create(root, layoutlib5).getFirst();
        assertNotNull(rootView);
        assertEquals("GestureOverlayView", rootView.getName());

        assertTrue(rootView.isRoot());
        assertNull(rootView.getParent());
        assertSame(rootNode, rootView.getUiViewNode());
        assertEquals(1, rootView.getChildren().size());

        CanvasViewInfo childView = rootView.getChildren().get(0);
        assertEquals("LinearLayout", childView.getName());

        // This should also be a root for the special case that the root is
        assertTrue(childView.isRoot());

        assertEquals(1, childView.getChildren().size());
        CanvasViewInfo grandChildView = childView.getChildren().get(0);
        assertEquals("Button", grandChildView.getName());
        assertFalse(grandChildView.isRoot());
    }

    public void testListView() throws Exception {
        // For ListViews we get AdapterItemReferences as cookies. Ensure that this
        // works properly.
        //
        // android.widget.FrameLayout [0,50,320,480] <FrameLayout>
        //     android.widget.ListView [0,0,320,430] <ListView>
        //        android.widget.LinearLayout [0,0,320,17] SessionParams$AdapterItemReference
        //            android.widget.TextView [0,0,73,17]
        //        android.widget.LinearLayout [0,18,320,35] SessionParams$AdapterItemReference
        //            android.widget.TextView [0,0,73,17]
        //        android.widget.LinearLayout [0,36,320,53] SessionParams$AdapterItemReference
        //            android.widget.TextView [0,0,73,17]
        //        ...

        UiViewElementNode rootNode = createNode("FrameLayout", true);
        UiViewElementNode childNode = createNode(rootNode, "ListView", false);
        /*UiViewElementNode grandChildNode =*/ createNode(childNode, "LinearLayout", false);
        /*UiViewElementNode greatGrandChildNode =*/ createNode(childNode, "TextView", false);
        DataBindingItem dataBindingItem = new DataBindingItem("foo");

        ViewInfo root = new ViewInfo("FrameLayout", rootNode, 0, 50, 320, 480);
        ViewInfo child = new ViewInfo("ListView", childNode, 0, 0, 320, 430);
        root.setChildren(Collections.singletonList(child));
        ViewInfo grandChild = new ViewInfo("LinearLayout", dataBindingItem, 0, 0, 320, 17);
        child.setChildren(Collections.singletonList(grandChild));
        ViewInfo greatGrandChild = new ViewInfo("Button", null, 0, 0, 73, 17);
        grandChild.setChildren(Collections.singletonList(greatGrandChild));
        CanvasViewInfo rootView = CanvasViewInfo.create(root, true /*layoutlib5*/).getFirst();
        assertNotNull(rootView);

        assertEquals("FrameLayout", rootView.getName());
        assertEquals(1, rootView.getChildren().size());
        assertSame(rootNode, rootView.getUiViewNode());

        CanvasViewInfo childView = rootView.getChildren().get(0);
        assertEquals("ListView", childView.getName());
        assertEquals(0, childView.getChildren().size());
        assertSame(childNode, childView.getUiViewNode());
    }

    /**
     * Dumps out the given {@link ViewInfo} hierarchy to standard out.
     * Useful during development.
     *
     * @param graphicalEditor the editor associated with this hierarchy
     * @param root the root of the {@link ViewInfo} hierarchy
     */
    public static void dump(GraphicalEditorPart graphicalEditor, ViewInfo root) {
        System.out.println("\n\nRendering:");
        boolean supportsEmbedding = graphicalEditor.renderingSupports(Capability.EMBEDDED_LAYOUT);
        System.out.println("Supports Embedded Layout=" + supportsEmbedding);
        System.out.println("Rendering context=" + graphicalEditor.getIncludedWithin());
        dump(root, 0);
    }

    /** Helper for {@link #dump(GraphicalEditorPart, ViewInfo)} */
    public static void dump(ViewInfo info, int depth) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            sb.append("    ");
        }
        sb.append(info.getClassName());
        sb.append(" [");
        sb.append(info.getLeft());
        sb.append(",");
        sb.append(info.getTop());
        sb.append(",");
        sb.append(info.getRight());
        sb.append(",");
        sb.append(info.getBottom());
        sb.append("] ");
        Object cookie = info.getCookie();
        if (cookie instanceof UiViewElementNode) {
            sb.append(" ");
            UiViewElementNode node = (UiViewElementNode) cookie;
            sb.append("<");
            sb.append(node.getDescriptor().getXmlName());
            sb.append("> ");
        } else if (cookie != null) {
            sb.append(" cookie=" + cookie);
        }

        System.out.println(sb.toString());

        for (ViewInfo child : info.getChildren()) {
            dump(child, depth + 1);
        }
    }

    /** Helper for {@link #dump(GraphicalEditorPart, ViewInfo)} */
    public static void dump(CanvasViewInfo info, int depth) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            sb.append("    ");
        }
        sb.append(info.getName());
        sb.append(" [");
        sb.append(info.getAbsRect());
        sb.append("], node=");
        sb.append(info.getUiViewNode());

        System.out.println(sb.toString());

        for (CanvasViewInfo child : info.getChildren()) {
            dump(child, depth + 1);
        }
    }

}

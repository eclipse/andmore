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

package org.eclipse.andmore.internal.editors.layout.gre;

import com.android.ide.common.api.INode;
import com.android.ide.common.api.Rect;
import com.android.ide.common.rendering.api.ViewInfo;

import java.util.Arrays;

import org.eclipse.andmore.internal.editors.layout.descriptors.ViewElementDescriptor;
import org.eclipse.andmore.internal.editors.layout.gle2.CanvasViewInfo;
import org.eclipse.andmore.internal.editors.layout.uimodel.UiViewElementNode;

import junit.framework.TestCase;

public class NodeFactoryTest extends TestCase {

    private NodeFactory m;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        m = new NodeFactory(null);

    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        m = null;
    }

    public final void testCreateCanvasViewInfo() {
        ViewElementDescriptor ved = new ViewElementDescriptor("xml", "com.example.MyJavaClass");
        UiViewElementNode uiv = new UiViewElementNode(ved);
        ViewInfo lvi = new ViewInfo("name", uiv, 10, 12, 110, 120);
        CanvasViewInfo cvi = CanvasViewInfo.create(lvi, true /* layoutlib5 */).getFirst();

        // Create a NodeProxy.
        NodeProxy proxy = m.create(cvi);

        // getNode() is our only internal implementation method.
        assertNotNull(proxy);
        assertSame(uiv, proxy.getNode());

        // Groovy scripts only see the INode interface so we want to primarily test that.
        INode inode = proxy;
        assertEquals(new Rect(10, 12, 110-10-1, 120-12-1), inode.getBounds());
        assertTrue(Arrays.equals(new INode[0], inode.getChildren()));
        assertEquals("com.example.MyJavaClass", inode.getFqcn());
        assertNull(inode.getParent());
        assertSame(inode, inode.getRoot());

    }

    public final void testCreateUiViewElementNode() {
        ViewElementDescriptor ved = new ViewElementDescriptor("xml", "com.example.MyJavaClass");
        UiViewElementNode uiv = new UiViewElementNode(ved);

        // Create a NodeProxy.
        NodeProxy proxy = m.create(uiv);

        // getNode() is our only internal implementation method.
        assertNotNull(proxy);
        assertSame(uiv, proxy.getNode());

        // Groovy scripts only see the INode interface so we want to primarily test that.
        INode inode = proxy;
        // Nodes constructed using this create() method do not have valid bounds.
        // There should be one invalid bound rectangle.
        assertNotNull(inode.getBounds());
        assertFalse(inode.getBounds().isValid());
        // All the other properties should be set correctly.
        assertTrue(Arrays.equals(new INode[0], inode.getChildren()));
        assertEquals("com.example.MyJavaClass", inode.getFqcn());
        assertNull(inode.getParent());
        assertSame(inode, inode.getRoot());
    }

    public final void testCreateDup() {
        ViewElementDescriptor ved = new ViewElementDescriptor("xml", "com.example.MyJavaClass");
        UiViewElementNode uiv = new UiViewElementNode(ved);
        ViewInfo lvi = new ViewInfo("name", uiv, 10, 12, 110, 120);
        CanvasViewInfo cvi = CanvasViewInfo.create(lvi, true /* layoutlib5 */).getFirst();

        // NodeProxies are cached. Creating the same one twice returns the same proxy.
        NodeProxy proxy1 = m.create(cvi);
        NodeProxy proxy2 = m.create(cvi);
        assertSame(proxy2, proxy1);
    }

    public final void testClear() {
        ViewElementDescriptor ved = new ViewElementDescriptor("xml", "com.example.MyJavaClass");
        UiViewElementNode uiv = new UiViewElementNode(ved);
        ViewInfo lvi = new ViewInfo("name", uiv, 10, 12, 110, 120);
        CanvasViewInfo cvi = CanvasViewInfo.create(lvi, true /* layoutlib5 */).getFirst();

        // NodeProxies are cached. Creating the same one twice returns the same proxy.
        NodeProxy proxy1 = m.create(cvi);
        NodeProxy proxy2 = m.create(cvi);
        assertSame(proxy2, proxy1);

        // Clearing the cache will force it create a new proxy.
        m.clear();
        NodeProxy proxy3 = m.create(cvi);
        assertNotSame(proxy1, proxy3);
    }

}

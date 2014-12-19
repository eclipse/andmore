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

import org.eclipse.andmore.internal.editors.layout.descriptors.ViewElementDescriptor;
import org.eclipse.andmore.internal.editors.layout.uimodel.UiViewElementNode;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.ui.internal.registry.ViewDescriptor;

/**
 * Mocks a {@link NodeProxy}, creating it using an XML local name and generating
 * a made-up {@link UiViewElementNode} and a {@link ViewDescriptor} on the fly.
 */
public class MockNodeProxy extends NodeProxy {

    /**
     * Generates a {@link NodeProxy} using an FQCN (e.g. android.view.View)
     * and making the last segment of the FQCN the XML name of the view (e.g. "View")
     * and wraps it as a {@link NodeProxy}.
     *
     * @param fqcn The fully qualified name of the class to wrap, e.g. "android.view.Button".
     * @param bounds The bounds of a the view in the canvas. Must be either: <br/>
     *   - a valid rect for a view that is actually in the canvas <br/>
     *   - <b>*or*</b> null (or an invalid rect) for a view that has just been added dynamically
     *   to the model. We never store a null bounds rectangle in the node, a null rectangle
     *   will be converted to an invalid rectangle.
     * @param factory A {@link NodeFactory} to create unique children nodes.
     */
    public MockNodeProxy(String fqcn, Rectangle bounds, NodeFactory factory) {
        super(makeUiViewNode(fqcn), bounds, factory);
    }

    /**
     * Generates a {@link ViewElementDescriptor} using an FQCN (e.g. android.view.View)
     * and making the last segment of the FQCN the XML name of the view (e.g. "View").
     *
     * @param fqcn The fully qualified name of the class to wrap, e.g. "android.view.Button"
     * @return A new view element node with a new descriptor for the FQCN and an XML name
     *  matching the last FQCN segment (e.g. "Button")
     */
    private static UiViewElementNode makeUiViewNode(String fqcn) {
        String xmlName = fqcn;
        int pos = xmlName.lastIndexOf('.');
        if (pos > 0) {
            xmlName = xmlName.substring(pos + 1);
        }

        ViewElementDescriptor desc = new ViewElementDescriptor(xmlName, fqcn);
        UiViewElementNode uiNode = new UiViewElementNode(desc);
        return uiNode;
    }

}

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

package org.eclipse.andmore.internal.editors.layout;

import com.android.SdkConstants;

import org.eclipse.andmore.internal.editors.mock.MockXmlNode;
import org.w3c.dom.Node;

import java.util.HashSet;

import junit.framework.TestCase;

public class ExplodeRenderingHelperTest extends TestCase {

    private final HashSet<String> mLayoutNames = new HashSet<String>();

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mLayoutNames.add("LinearLayout");
        mLayoutNames.add("RelativeLayout");
    }

    public void testSingleHorizontalLinearLayout() {
        // Single layout, horizontal, 2 buttons.
        MockXmlNode layout = createLinearLayout(true /*horizontal*/,
                new MockXmlNode[] { createButton(), createButton()} );

        ExplodedRenderingHelper helper = new ExplodedRenderingHelper(layout, mLayoutNames);
        assertEquals(1, helper.getHeightPadding());
        assertEquals(1, helper.getWidthPadding());
    }

    public void testSingleVerticalLinearLayout() {
        // Single layout, horizontal, with 2 buttons.
        // LinearLayout(H:[Button Button])
        MockXmlNode layout = createLinearLayout(false /*horizontal*/,
                new MockXmlNode[] { createButton(), createButton()} );

        ExplodedRenderingHelper helper = new ExplodedRenderingHelper(layout, mLayoutNames);
        assertEquals(1, helper.getWidthPadding());
        assertEquals(1, helper.getHeightPadding());
    }

    public void testEmbeddedLinearLayouts() {
        /*
         *  LinearLayout(vertical):
         *    LinearLayout(H:[Button Button])
         *    LinearLayout(H:[Button Button Button])
         *
         * Result should be 2 in x, 3 in y
         */
        MockXmlNode layout = createLinearLayout(false /*horizontal*/,
                new MockXmlNode[] {
                    createLinearLayout(true /*horizontal*/,
                            new MockXmlNode[] { createButton(), createButton()}),
                    createLinearLayout(true /*horizontal*/,
                            new MockXmlNode[] { createButton(), createButton(), createButton()}),
                } );

        ExplodedRenderingHelper helper = new ExplodedRenderingHelper(layout, mLayoutNames);
        assertEquals(2, helper.getWidthPadding());
        assertEquals(3, helper.getHeightPadding());
    }

    public void testSimpleRelativeLayoutWithOneLinearLayouts() {
        /*
         *  RelativeLayout:
         *    LinearLayout(H:[Button Button])
         *
         * Result should be 2 in x, 2 in y
         */
        MockXmlNode layout = createRelativeLayout(
                new MockXmlNode[] {
                    createLinearLayout(true /*horizontal*/,
                            new MockXmlNode[] { createButton(), createButton()}),
                } );

        ExplodedRenderingHelper helper = new ExplodedRenderingHelper(layout, mLayoutNames);
        assertEquals(2, helper.getWidthPadding());
        assertEquals(2, helper.getHeightPadding());
    }

    public void /*test*/RelativeLayoutWithVerticalLinearLayouts() {
        //FIXME: Reenable once the relative layout are properly supported.
        /*
         * Children of the relative layouts, one below the other.
         * Each with only buttons in them.
         *  RelativeLayout:
         *    LinearLayout(H:[Button Button])
         *          ^
         *    LinearLayout(H:[Button Button])
         *
         * Result should be 2 in x, 3 in y
         */

        // create the linearlayouts.
        MockXmlNode linear1 = createLinearLayout(true /*horizontal*/,
                new MockXmlNode[] { createButton(), createButton()});
        linear1.addAttributes(SdkConstants.NS_RESOURCES, "id", "@+id/linear1");

        MockXmlNode linear2 = createLinearLayout(true /*horizontal*/,
                        new MockXmlNode[] { createButton(), createButton()});
        linear2.addAttributes(SdkConstants.NS_RESOURCES, "id", "@+id/linear2");

        // position linear2 below linear1
        linear2.addAttributes(SdkConstants.NS_RESOURCES, "layout_below", "@+id/linear1");


        MockXmlNode layout = createRelativeLayout(new MockXmlNode[] { linear1, linear2 } );

        ExplodedRenderingHelper helper = new ExplodedRenderingHelper(layout, mLayoutNames);
        assertEquals(2, helper.getWidthPadding());
        assertEquals(3, helper.getHeightPadding());
    }

    public void /*test*/RelativeLayoutWithVerticalLinearLayouts2() {
        //FIXME: Reenable once the relative layout are properly supported.
        /*
         * Children of the relative layouts, one above the other.
         * Each with only buttons in them.
         *  RelativeLayout:
         *    LinearLayout(H:[Button Button])
         *          v
         *    LinearLayout(H:[Button Button])
         *
         * Result should be 2 in x, 3 in y
         */

        // create the linearlayouts.
        MockXmlNode linear1 = createLinearLayout(true /*horizontal*/,
                new MockXmlNode[] { createButton(), createButton() } );
        linear1.addAttributes(SdkConstants.NS_RESOURCES, "id", "@+id/linear1");

        MockXmlNode linear2 = createLinearLayout(true /*horizontal*/,
                        new MockXmlNode[] { createButton(), createButton() } );
        linear2.addAttributes(SdkConstants.NS_RESOURCES, "id", "@+id/linear2");

        // position linear2 below linear1
        linear2.addAttributes(SdkConstants.NS_RESOURCES, "layout_above", "@+id/linear1");


        MockXmlNode layout = createRelativeLayout(new MockXmlNode[] { linear1, linear2 } );

        ExplodedRenderingHelper helper = new ExplodedRenderingHelper(layout, mLayoutNames);
        assertEquals(2, helper.getWidthPadding());
        assertEquals(3, helper.getHeightPadding());
    }

    public void /*test*/ComplexRelativeLayout() {
        //FIXME: Reenable once the relative layout are properly supported.
        /*
         *  RelativeLayout:
         *
         *                                       < LinearLayout1(V: [button]) > LinearLayout2(V: [button])
         *                                            v
         *  Button1 > LinearLayout3(V: [button]) < Button2
         *                  v
         *          < LinearLayout4(V: [button])
         *                                     ^
         *                                     <LinearLayout5(V: [button])
         *
         * Result should be 4 in x, 5 in y
         */

        // create the elements
        MockXmlNode button1 = createButton();
        button1.addAttributes(SdkConstants.NS_RESOURCES, "id", "@+id/button1");

        MockXmlNode button2 = createButton();
        button2.addAttributes(SdkConstants.NS_RESOURCES, "id", "@+id/button2");

        MockXmlNode linear1 = createLinearLayout(true /*horizontal*/,
                new MockXmlNode[] { createButton() } );
        linear1.addAttributes(SdkConstants.NS_RESOURCES, "id", "@+id/linear1");

        MockXmlNode linear2 = createLinearLayout(true /*horizontal*/,
                new MockXmlNode[] { createButton() } );
        linear2.addAttributes(SdkConstants.NS_RESOURCES, "id", "@+id/linear2");

        MockXmlNode linear3 = createLinearLayout(true /*horizontal*/,
                new MockXmlNode[] { createButton() } );
        linear3.addAttributes(SdkConstants.NS_RESOURCES, "id", "@+id/linear3");

        MockXmlNode linear4 = createLinearLayout(true /*horizontal*/,
                new MockXmlNode[] { createButton() } );
        linear4.addAttributes(SdkConstants.NS_RESOURCES, "id", "@+id/linear4");

        MockXmlNode linear5 = createLinearLayout(true /*horizontal*/,
                new MockXmlNode[] { createButton() } );
        linear5.addAttributes(SdkConstants.NS_RESOURCES, "id", "@+id/linear5");


        // link them
        button1.addAttributes(SdkConstants.NS_RESOURCES, "layout_toLeftOf",  "@+id/linear3");

        button2.addAttributes(SdkConstants.NS_RESOURCES, "layout_toRightOf", "@+id/linear3");

        linear1.addAttributes(SdkConstants.NS_RESOURCES, "layout_toRightOf", "@+id/linear3");
        linear1.addAttributes(SdkConstants.NS_RESOURCES, "layout_toLeftOf",  "@+id/linear2");
        linear1.addAttributes(SdkConstants.NS_RESOURCES, "layout_above",     "@+id/button2");

        linear3.addAttributes(SdkConstants.NS_RESOURCES, "layout_above",     "@+id/linear4");

        linear4.addAttributes(SdkConstants.NS_RESOURCES, "layout_toRightOf", "@+id/button1");

        linear5.addAttributes(SdkConstants.NS_RESOURCES, "layout_toRightOf", "@+id/linear4");
        linear5.addAttributes(SdkConstants.NS_RESOURCES, "layout_below",     "@+id/linear4");

        MockXmlNode layout = createRelativeLayout(
                new MockXmlNode[] {
                        button1, button2, linear1, linear2, linear3, linear4, linear5 } );

        ExplodedRenderingHelper helper = new ExplodedRenderingHelper(layout, mLayoutNames);
        assertEquals(4, helper.getWidthPadding());
        assertEquals(5, helper.getHeightPadding());
    }


    // ----- helper to deal with mocks

    private MockXmlNode createButton() {
        return new MockXmlNode(null, "Button", Node.ELEMENT_NODE, null);
    }

    private MockXmlNode createLinearLayout(boolean horizontal, MockXmlNode[] children) {
        MockXmlNode layout = new MockXmlNode(null, "LinearLayout", Node.ELEMENT_NODE, children);

        layout.addAttributes(SdkConstants.NS_RESOURCES, "orientation",
                horizontal ? "horizontal" : "vertical");

        return layout;
    }

    private MockXmlNode createRelativeLayout(MockXmlNode[] children) {
        MockXmlNode layout = new MockXmlNode(null, "RelativeLayout", Node.ELEMENT_NODE, children);

        return layout;
    }
}

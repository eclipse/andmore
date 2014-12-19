/*
 * Copyright (C) 2007 The Android Open Source Project
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

package org.eclipse.andmore.internal.editors.manifest.model;

import com.android.SdkConstants;
import com.android.utils.XmlUtils;

import org.eclipse.andmore.internal.editors.descriptors.ElementDescriptor;
import org.eclipse.andmore.internal.editors.descriptors.ElementDescriptor.Mandatory;
import org.eclipse.andmore.internal.editors.mock.MockXmlNode;
import org.eclipse.andmore.internal.editors.uimodel.UiElementNode;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.Iterator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.TestCase;

@SuppressWarnings("javadoc")
public class UiElementNodeTest extends TestCase {

    private UiElementNode ui;
    private ElementDescriptor mManifestDesc;
    private ElementDescriptor mAppDesc;
    private ElementDescriptor mUsesSdkDesc;

    @Override
    protected void setUp() throws Exception {
        mAppDesc = new ElementDescriptor("application", new ElementDescriptor[] {
                new ElementDescriptor("provider"),
                new ElementDescriptor("activity", new ElementDescriptor[] {
                    new ElementDescriptor("intent-filter")
                }),
            }, Mandatory.MANDATORY_LAST);

        mUsesSdkDesc = new ElementDescriptor("uses-sdk", new ElementDescriptor[] {},
                Mandatory.MANDATORY);

        mManifestDesc = new ElementDescriptor("manifest", new ElementDescriptor[] {
                mAppDesc,
                mUsesSdkDesc,
                new ElementDescriptor("permission")
            }, Mandatory.MANDATORY);

        ui = new UiElementNode(mManifestDesc);

        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        // pass
    }

    /**
     * Check initialization values for ui node
     */
    public void testInit() {
        assertSame(mManifestDesc, ui.getDescriptor());
        assertNull(ui.getUiParent());
        assertEquals(0, ui.getUiChildren().size());
        assertEquals(0, ui.getAllUiAttributes().size());
    }

    /**
     * We declared the descriptors as having a "mandatory last" application element
     * and a mandatory non-last uses-sdk element. This means if we create an empty
     * UiModel, we should get these two created, with the application element after
     * the uses-sdk.
     */
    public void testMandatoryOrder() {
        // Add the mandatory nodes with no XML backing, do it explicitly in the wrong order.
        assertEquals(0, ui.getUiChildren().size());
        ui.appendNewUiChild(mAppDesc);
        ui.appendNewUiChild(mUsesSdkDesc);

        assertEquals(2, ui.getUiChildren().size());
        assertSame(mAppDesc,     ui.getUiChildren().get(0).getDescriptor());
        assertSame(mUsesSdkDesc, ui.getUiChildren().get(1).getDescriptor());

        // Parse an XML with just a manifest.
        MockXmlNode root = new MockXmlNode(null /* namespace */, "manifest", Node.ELEMENT_NODE,
            new MockXmlNode[] {
                new MockXmlNode(null /* namespace */, "application", Node.ELEMENT_NODE, null)
            });

        ui.loadFromXmlNode(root);

        // We should get 2 children, the 2 mandatory nodes but this time with uses-sdk
        // before application since it's a mandatory-last so it "moves" to the end if possible.
        assertEquals(2, ui.getUiChildren().size());
        assertSame(mUsesSdkDesc, ui.getUiChildren().get(0).getDescriptor());
        assertNull(ui.getUiChildren().get(0).getXmlNode());
        assertSame(mAppDesc, ui.getUiChildren().get(1).getDescriptor());
        assertNotNull(ui.getUiChildren().get(1).getXmlNode());
    }

    /**
     * loadFrom() does nothing if the root node doesn't match what's expected
     */
    public void testLoadFrom_InvalidRoot() {
        assertEquals(0, ui.getUiChildren().size());
        MockXmlNode root = new MockXmlNode(null /* namespace */, "blah", Node.ELEMENT_NODE, null);
        ui.loadFromXmlNode(root);
        assertEquals(0, ui.getUiChildren().size());
    }

    /**
     * UiElementNode.loadFrom should be used to populate an empty ui node from an
     * existing XML node tree.
     */
    public void testLoadFrom_NewTree_1_Node() {
        MockXmlNode root = new MockXmlNode(null /* namespace */, "manifest", Node.ELEMENT_NODE,
            new MockXmlNode[] {
                new MockXmlNode(null /* namespace */, "application", Node.ELEMENT_NODE, null)
            });

        // get /manifest
        ui.loadFromXmlNode(root);
        assertEquals("manifest", ui.getDescriptor().getXmlName());
        assertEquals(1, ui.getUiChildren().size());
        assertEquals(0, ui.getAllUiAttributes().size());

        // get /manifest/application
        Iterator<UiElementNode> ui_child_it = ui.getUiChildren().iterator();
        UiElementNode application = ui_child_it.next();
        assertEquals("application", application.getDescriptor().getXmlName());
        assertEquals(0, application.getUiChildren().size());
        assertEquals(0, application.getAllUiAttributes().size());
    }


    public void testLoadFrom_NewTree_2_Nodes() {
        MockXmlNode root = new MockXmlNode(null /* namespace */, "manifest", Node.ELEMENT_NODE,
            new MockXmlNode[] {
                new MockXmlNode(null /* namespace */, "application", Node.ELEMENT_NODE, null),
                new MockXmlNode(null /* namespace */, "permission", Node.ELEMENT_NODE, null),
            });

        // get /manifest
        ui.loadFromXmlNode(root);
        assertEquals("manifest", ui.getDescriptor().getXmlName());
        assertEquals(2, ui.getUiChildren().size());
        assertEquals(0, ui.getAllUiAttributes().size());

        // get /manifest/application
        Iterator<UiElementNode> ui_child_it = ui.getUiChildren().iterator();
        UiElementNode application = ui_child_it.next();
        assertEquals("application", application.getDescriptor().getXmlName());
        assertEquals(0, application.getUiChildren().size());
        assertEquals(0, application.getAllUiAttributes().size());
        assertEquals(0, application.getUiSiblingIndex());

        // get /manifest/permission
        UiElementNode first_permission = ui_child_it.next();
        assertEquals("permission", first_permission.getDescriptor().getXmlName());
        assertEquals(0, first_permission.getUiChildren().size());
        assertEquals(0, first_permission.getAllUiAttributes().size());
        assertEquals(1, first_permission.getUiSiblingIndex());
    }

    public void testLoadFrom_NewTree_N_Nodes() {
        MockXmlNode root = new MockXmlNode(null /* namespace */, "manifest", Node.ELEMENT_NODE,
            new MockXmlNode[] {
                new MockXmlNode(null /* namespace */, "application", Node.ELEMENT_NODE,
                    new MockXmlNode[] {
                        new MockXmlNode(null /* namespace */, "activity", Node.ELEMENT_NODE,
                            null),
                        new MockXmlNode(null /* namespace */, "activity", Node.ELEMENT_NODE,
                            new MockXmlNode[] {
                                new MockXmlNode(null /* namespace */, "intent-filter",
                                        Node.ELEMENT_NODE, null),
                            }),
                        new MockXmlNode(null /* namespace */, "provider", Node.ELEMENT_NODE,
                                null),
                        new MockXmlNode(null /* namespace */, "provider", Node.ELEMENT_NODE,
                                null),
                    }),
                new MockXmlNode(null /* namespace */, "permission", Node.ELEMENT_NODE,
                        null),
                new MockXmlNode(null /* namespace */, "permission", Node.ELEMENT_NODE,
                        null),
            });

        // get /manifest
        ui.loadFromXmlNode(root);
        assertEquals("manifest", ui.getDescriptor().getXmlName());
        assertEquals(3, ui.getUiChildren().size());
        assertEquals(0, ui.getAllUiAttributes().size());

        // get /manifest/application
        Iterator<UiElementNode> ui_child_it = ui.getUiChildren().iterator();
        UiElementNode application = ui_child_it.next();
        assertEquals("application", application.getDescriptor().getXmlName());
        assertEquals(4, application.getUiChildren().size());
        assertEquals(0, application.getAllUiAttributes().size());

        // get /manifest/application/activity #1
        Iterator<UiElementNode> app_child_it = application.getUiChildren().iterator();
        UiElementNode first_activity = app_child_it.next();
        assertEquals("activity", first_activity.getDescriptor().getXmlName());
        assertEquals(0, first_activity.getUiChildren().size());
        assertEquals(0, first_activity.getAllUiAttributes().size());

        // get /manifest/application/activity #2
        UiElementNode second_activity = app_child_it.next();
        assertEquals("activity", second_activity.getDescriptor().getXmlName());
        assertEquals(1, second_activity.getUiChildren().size());
        assertEquals(0, second_activity.getAllUiAttributes().size());

        // get /manifest/application/activity #2/intent-filter #1
        Iterator<UiElementNode> activity_child_it = second_activity.getUiChildren().iterator();
        UiElementNode intent_filter = activity_child_it.next();
        assertEquals("intent-filter", intent_filter.getDescriptor().getXmlName());
        assertEquals(0, intent_filter.getUiChildren().size());
        assertEquals(0, intent_filter.getAllUiAttributes().size());

        // get /manifest/application/provider #1
        UiElementNode first_provider = app_child_it.next();
        assertEquals("provider", first_provider.getDescriptor().getXmlName());
        assertEquals(0, first_provider.getUiChildren().size());
        assertEquals(0, first_provider.getAllUiAttributes().size());

        // get /manifest/application/provider #2
        UiElementNode second_provider = app_child_it.next();
        assertEquals("provider", second_provider.getDescriptor().getXmlName());
        assertEquals(0, second_provider.getUiChildren().size());
        assertEquals(0, second_provider.getAllUiAttributes().size());

        // get /manifest/permission #1
        UiElementNode first_permission = ui_child_it.next();
        assertEquals("permission", first_permission.getDescriptor().getXmlName());
        assertEquals(0, first_permission.getUiChildren().size());
        assertEquals(0, first_permission.getAllUiAttributes().size());

        // get /manifest/permission #1
        UiElementNode second_permission = ui_child_it.next();
        assertEquals("permission", second_permission.getDescriptor().getXmlName());
        assertEquals(0, second_permission.getUiChildren().size());
        assertEquals(0, second_permission.getAllUiAttributes().size());
    }

    public void testCreateNameSpace() throws Exception {
        // Setup
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setValidating(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.newDocument();
        Element rootElement = document.createElement("root");
        document.appendChild(rootElement);
        Element root = document.getDocumentElement();
        root.appendChild(document.createTextNode("    "));
        Element foo = document.createElement("foo");
        root.appendChild(foo);
        root.appendChild(document.createTextNode("    "));
        Element bar = document.createElement("bar");
        root.appendChild(bar);
        Element baz = document.createElement("baz");
        root.appendChild(baz);

        String prefix = XmlUtils.lookupNamespacePrefix(baz, SdkConstants.ANDROID_URI);
        assertEquals("android", prefix);
    }
}

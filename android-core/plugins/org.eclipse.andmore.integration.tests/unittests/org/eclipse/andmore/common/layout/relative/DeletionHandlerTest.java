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
package org.eclipse.andmore.common.layout.relative;

import static com.android.SdkConstants.ATTR_ID;
import static com.android.SdkConstants.ANDROID_URI;

import com.android.ide.common.api.INode;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.andmore.common.layout.BaseViewRule;
import org.eclipse.andmore.common.layout.TestNode;

import junit.framework.TestCase;

@SuppressWarnings("javadoc")
public class DeletionHandlerTest extends TestCase {
    public void testSimple() {
        String xml = "" +
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<RelativeLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
            "    xmlns:tools=\"http://schemas.android.com/tools\"\n" +
            "    android:layout_width=\"match_parent\"\n" +
            "    android:layout_height=\"match_parent\"\n" +
            "    tools:ignore=\"HardcodedText\" >\n" +
            "\n" +
            "    <Button\n" +
            "        android:id=\"@+id/button1\"\n" +
            "        android:layout_width=\"wrap_content\"\n" +
            "        android:layout_height=\"wrap_content\"\n" +
            "        android:layout_alignParentLeft=\"true\"\n" +
            "        android:text=\"A\" />\n" +
            "\n" +
            "    <Button\n" +
            "        android:id=\"@+id/button2\"\n" +
            "        android:layout_width=\"wrap_content\"\n" +
            "        android:layout_height=\"wrap_content\"\n" +
            "        android:layout_alignBaseline=\"@+id/button1\"\n" +
            "        android:layout_alignBottom=\"@+id/button1\"\n" +
            "        android:layout_toRightOf=\"@+id/button1\"\n" +
            "        android:text=\"B\" />\n" +
            "\n" +
            "    <Button\n" +
            "        android:id=\"@+id/button3\"\n" +
            "        android:layout_width=\"wrap_content\"\n" +
            "        android:layout_height=\"wrap_content\"\n" +
            "        android:layout_alignBottom=\"@+id/button2\"\n" +
            "        android:layout_toRightOf=\"@+id/button2\"\n" +
            "        android:text=\"C\" />\n" +
            "\n" +
            "</RelativeLayout>";
        TestNode targetNode = TestNode.createFromXml(xml);
        assertNotNull(targetNode);

        TestNode button2 = TestNode.findById(targetNode, "@+id/button2");

        INode layout = button2.getParent();
        List<INode> deletedNodes = Collections.<INode>singletonList(button2);
        List<INode> movedNodes = Collections.<INode>emptyList();
        assertSame(layout, targetNode);
        layout.removeChild(button2);

        DeletionHandler handler = new DeletionHandler(deletedNodes, movedNodes, layout);
        handler.updateConstraints();

        String updated = TestNode.toXml(targetNode);
        assertEquals(
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<RelativeLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                "    xmlns:tools=\"http://schemas.android.com/tools\"\n" +
                "    android:layout_width=\"match_parent\"\n" +
                "    android:layout_height=\"match_parent\"\n" +
                "    tools:ignore=\"HardcodedText\">\n" +
                "\n" +
                "    <Button\n" +
                "        android:id=\"@+id/button1\"\n" +
                "        android:layout_width=\"wrap_content\"\n" +
                "        android:layout_height=\"wrap_content\"\n" +
                "        android:layout_alignParentLeft=\"true\"\n" +
                "        android:text=\"A\">\n" +
                "    </Button>\n" +
                "\n" +
                "    <Button\n" +
                "        android:id=\"@+id/button3\"\n" +
                "        android:layout_width=\"wrap_content\"\n" +
                "        android:layout_height=\"wrap_content\"\n" +
                "        android:layout_alignBaseline=\"@+id/button1\"\n" +
                "        android:layout_alignBottom=\"@+id/button1\"\n" +
                "        android:layout_toRightOf=\"@+id/button1\"\n" +
                "        android:text=\"C\">\n" +
                "    </Button>\n" +
                "\n" +
                "</RelativeLayout>",
                updated);
        assertFalse(updated.contains(BaseViewRule.stripIdPrefix(button2.getStringAttr(ANDROID_URI,
                ATTR_ID))));
    }

    public void testTransitive() {
        String xml = "" +
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<RelativeLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
            "    xmlns:tools=\"http://schemas.android.com/tools\"\n" +
            "    android:layout_width=\"match_parent\"\n" +
            "    android:layout_height=\"match_parent\"\n" +
            "    tools:ignore=\"HardcodedText\" >\n" +
            "\n" +
            "    <Button\n" +
            "        android:id=\"@+id/button1\"\n" +
            "        android:layout_width=\"wrap_content\"\n" +
            "        android:layout_height=\"wrap_content\"\n" +
            "        android:layout_alignParentLeft=\"true\"\n" +
            "        android:layout_alignParentTop=\"true\"\n" +
            "        android:text=\"Above\" />\n" +
            "\n" +
            "    <Button\n" +
            "        android:id=\"@+id/button2\"\n" +
            "        android:layout_width=\"wrap_content\"\n" +
            "        android:layout_height=\"wrap_content\"\n" +
            "        android:layout_alignParentLeft=\"true\"\n" +
            "        android:layout_below=\"@+id/button1\"\n" +
            "        android:text=\"A\" />\n" +
            "\n" +
            "    <Button\n" +
            "        android:id=\"@+id/button3\"\n" +
            "        android:layout_width=\"wrap_content\"\n" +
            "        android:layout_height=\"wrap_content\"\n" +
            "        android:layout_alignBaseline=\"@+id/button2\"\n" +
            "        android:layout_alignBottom=\"@+id/button2\"\n" +
            "        android:layout_toRightOf=\"@+id/button2\"\n" +
            "        android:text=\"B\" />\n" +
            "\n" +
            "    <Button\n" +
            "        android:id=\"@+id/button4\"\n" +
            "        android:layout_width=\"wrap_content\"\n" +
            "        android:layout_height=\"wrap_content\"\n" +
            "        android:layout_alignBottom=\"@+id/button3\"\n" +
            "        android:layout_toRightOf=\"@+id/button3\"\n" +
            "        android:text=\"C\" />\n" +
            "\n" +
            "    <Button\n" +
            "        android:id=\"@+id/button5\"\n" +
            "        android:layout_width=\"wrap_content\"\n" +
            "        android:layout_height=\"wrap_content\"\n" +
            "        android:layout_alignBaseline=\"@+id/button4\"\n" +
            "        android:layout_alignBottom=\"@+id/button4\"\n" +
            "        android:layout_toRightOf=\"@+id/button4\"\n" +
            "        android:text=\"D\" />\n" +
            "\n" +
            "    <Button\n" +
            "        android:id=\"@+id/button6\"\n" +
            "        android:layout_width=\"wrap_content\"\n" +
            "        android:layout_height=\"wrap_content\"\n" +
            "        android:layout_alignBottom=\"@+id/button5\"\n" +
            "        android:layout_toRightOf=\"@+id/button5\"\n" +
            "        android:text=\"E\" />\n" +
            "\n" +
            "    <Button\n" +
            "        android:id=\"@+id/button7\"\n" +
            "        android:layout_width=\"wrap_content\"\n" +
            "        android:layout_height=\"wrap_content\"\n" +
            "        android:layout_alignLeft=\"@+id/button3\"\n" +
            "        android:layout_below=\"@+id/button3\"\n" +
            "        android:text=\"Button\" />\n" +
            "\n" +
            "    <CheckBox\n" +
            "        android:id=\"@+id/checkBox1\"\n" +
            "        android:layout_width=\"wrap_content\"\n" +
            "        android:layout_height=\"wrap_content\"\n" +
            "        android:layout_alignBaseline=\"@+id/button7\"\n" +
            "        android:layout_alignBottom=\"@+id/button7\"\n" +
            "        android:layout_toRightOf=\"@+id/button7\"\n" +
            "        android:text=\"CheckBox\" />\n" +
            "\n" +
            "    <Button\n" +
            "        android:id=\"@+id/button8\"\n" +
            "        android:layout_width=\"wrap_content\"\n" +
            "        android:layout_height=\"wrap_content\"\n" +
            "        android:layout_below=\"@+id/checkBox1\"\n" +
            "        android:layout_toRightOf=\"@+id/checkBox1\"\n" +
            "        android:text=\"Button\" />\n" +
            "\n" +
            "</RelativeLayout>";
        TestNode targetNode = TestNode.createFromXml(xml);
        assertNotNull(targetNode);
        TestNode button7 = TestNode.findById(targetNode, "@+id/button7");
        TestNode checkBox = TestNode.findById(targetNode, "@+id/checkBox1");

        INode layout = button7.getParent();
        List<INode> deletedNodes = Arrays.<INode>asList(button7, checkBox);
        List<INode> movedNodes = Collections.<INode>emptyList();
        assertSame(layout, targetNode);
        layout.removeChild(button7);
        layout.removeChild(checkBox);

        DeletionHandler handler = new DeletionHandler(deletedNodes, movedNodes, layout);
        handler.updateConstraints();

        String updated = TestNode.toXml(targetNode);
        assertEquals(
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<RelativeLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                "    xmlns:tools=\"http://schemas.android.com/tools\"\n" +
                "    android:layout_width=\"match_parent\"\n" +
                "    android:layout_height=\"match_parent\"\n" +
                "    tools:ignore=\"HardcodedText\">\n" +
                "\n" +
                "    <Button\n" +
                "        android:id=\"@+id/button1\"\n" +
                "        android:layout_width=\"wrap_content\"\n" +
                "        android:layout_height=\"wrap_content\"\n" +
                "        android:layout_alignParentLeft=\"true\"\n" +
                "        android:layout_alignParentTop=\"true\"\n" +
                "        android:text=\"Above\">\n" +
                "    </Button>\n" +
                "\n" +
                "    <Button\n" +
                "        android:id=\"@+id/button2\"\n" +
                "        android:layout_width=\"wrap_content\"\n" +
                "        android:layout_height=\"wrap_content\"\n" +
                "        android:layout_alignParentLeft=\"true\"\n" +
                "        android:layout_below=\"@+id/button1\"\n" +
                "        android:text=\"A\">\n" +
                "    </Button>\n" +
                "\n" +
                "    <Button\n" +
                "        android:id=\"@+id/button3\"\n" +
                "        android:layout_width=\"wrap_content\"\n" +
                "        android:layout_height=\"wrap_content\"\n" +
                "        android:layout_alignBaseline=\"@+id/button2\"\n" +
                "        android:layout_alignBottom=\"@+id/button2\"\n" +
                "        android:layout_toRightOf=\"@+id/button2\"\n" +
                "        android:text=\"B\">\n" +
                "    </Button>\n" +
                "\n" +
                "    <Button\n" +
                "        android:id=\"@+id/button4\"\n" +
                "        android:layout_width=\"wrap_content\"\n" +
                "        android:layout_height=\"wrap_content\"\n" +
                "        android:layout_alignBottom=\"@+id/button3\"\n" +
                "        android:layout_toRightOf=\"@+id/button3\"\n" +
                "        android:text=\"C\">\n" +
                "    </Button>\n" +
                "\n" +
                "    <Button\n" +
                "        android:id=\"@+id/button5\"\n" +
                "        android:layout_width=\"wrap_content\"\n" +
                "        android:layout_height=\"wrap_content\"\n" +
                "        android:layout_alignBaseline=\"@+id/button4\"\n" +
                "        android:layout_alignBottom=\"@+id/button4\"\n" +
                "        android:layout_toRightOf=\"@+id/button4\"\n" +
                "        android:text=\"D\">\n" +
                "    </Button>\n" +
                "\n" +
                "    <Button\n" +
                "        android:id=\"@+id/button6\"\n" +
                "        android:layout_width=\"wrap_content\"\n" +
                "        android:layout_height=\"wrap_content\"\n" +
                "        android:layout_alignBottom=\"@+id/button5\"\n" +
                "        android:layout_toRightOf=\"@+id/button5\"\n" +
                "        android:text=\"E\">\n" +
                "    </Button>\n" +
                "\n" +
                "    <Button\n" +
                "        android:id=\"@+id/button8\"\n" +
                "        android:layout_width=\"wrap_content\"\n" +
                "        android:layout_height=\"wrap_content\"\n" +
                "        android:layout_alignLeft=\"@+id/button3\"\n" +
                "        android:layout_below=\"@+id/button3\"\n" +
                "        android:text=\"Button\">\n" +
                "    </Button>\n" +
                "\n" +
                "</RelativeLayout>",
                updated);
        assertFalse(updated.contains(BaseViewRule.stripIdPrefix(button7.getStringAttr(ANDROID_URI,
                ATTR_ID))));
    }

    public void testCenter() {
        String xml =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<RelativeLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
            "    xmlns:tools=\"http://schemas.android.com/tools\"\n" +
            "    android:layout_width=\"match_parent\"\n" +
            "    android:layout_height=\"match_parent\"\n" +
            "    tools:ignore=\"HardcodedText\" >\n" +
            "\n" +
            "    <Button\n" +
            "        android:id=\"@+id/button1\"\n" +
            "        android:layout_width=\"wrap_content\"\n" +
            "        android:layout_height=\"wrap_content\"\n" +
            "        android:layout_centerInParent=\"true\"\n" +
            "        android:text=\"Button\" />\n" +
            "\n" +
            "    <CheckBox\n" +
            "        android:id=\"@+id/checkBox1\"\n" +
            "        android:layout_width=\"wrap_content\"\n" +
            "        android:layout_height=\"wrap_content\"\n" +
            "        android:layout_below=\"@+id/button1\"\n" +
            "        android:layout_toRightOf=\"@+id/button1\"\n" +
            "        android:text=\"CheckBox\" />\n" +
            "\n" +
            "</RelativeLayout>";

        TestNode targetNode = TestNode.createFromXml(xml);
        assertNotNull(targetNode);
        TestNode button1 = TestNode.findById(targetNode, "@+id/button1");

        INode layout = button1.getParent();
        List<INode> deletedNodes = Collections.<INode>singletonList(button1);
        List<INode> movedNodes = Collections.<INode>emptyList();
        assertSame(layout, targetNode);
        layout.removeChild(button1);

        DeletionHandler handler = new DeletionHandler(deletedNodes, movedNodes, layout);
        handler.updateConstraints();

        String updated = TestNode.toXml(targetNode);
        assertEquals(
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<RelativeLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                "    xmlns:tools=\"http://schemas.android.com/tools\"\n" +
                "    android:layout_width=\"match_parent\"\n" +
                "    android:layout_height=\"match_parent\"\n" +
                "    tools:ignore=\"HardcodedText\">\n" +
                "\n" +
                "    <CheckBox\n" +
                "        android:id=\"@+id/checkBox1\"\n" +
                "        android:layout_width=\"wrap_content\"\n" +
                "        android:layout_height=\"wrap_content\"\n" +
                "        android:layout_centerInParent=\"true\"\n" +
                "        android:text=\"CheckBox\">\n" +
                "    </CheckBox>\n" +
                "\n" +
                "</RelativeLayout>",
                updated);
        assertFalse(updated.contains(BaseViewRule.stripIdPrefix(button1.getStringAttr(ANDROID_URI,
                ATTR_ID))));

    }

    public void testMove() {
        String xml = "" +
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<RelativeLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
            "    xmlns:tools=\"http://schemas.android.com/tools\"\n" +
            "    android:layout_width=\"match_parent\"\n" +
            "    android:layout_height=\"match_parent\"\n" +
            "    tools:ignore=\"HardcodedText\" >\n" +
            "\n" +
            "    <Button\n" +
            "        android:id=\"@+id/button1\"\n" +
            "        android:layout_width=\"wrap_content\"\n" +
            "        android:layout_height=\"wrap_content\"\n" +
            "        android:layout_alignParentLeft=\"true\"\n" +
            "        android:text=\"A\" />\n" +
            "\n" +
            "    <Button\n" +
            "        android:id=\"@+id/button2\"\n" +
            "        android:layout_width=\"wrap_content\"\n" +
            "        android:layout_height=\"wrap_content\"\n" +
            "        android:layout_alignBaseline=\"@+id/button1\"\n" +
            "        android:layout_alignBottom=\"@+id/button1\"\n" +
            "        android:layout_toRightOf=\"@+id/button1\"\n" +
            "        android:text=\"B\" />\n" +
            "\n" +
            "    <Button\n" +
            "        android:id=\"@+id/button3\"\n" +
            "        android:layout_width=\"wrap_content\"\n" +
            "        android:layout_height=\"wrap_content\"\n" +
            "        android:layout_alignBottom=\"@+id/button2\"\n" +
            "        android:layout_toRightOf=\"@+id/button2\"\n" +
            "        android:text=\"C\" />\n" +
            "\n" +
            "</RelativeLayout>";
        TestNode targetNode = TestNode.createFromXml(xml);
        assertNotNull(targetNode);

        TestNode button2 = TestNode.findById(targetNode, "@+id/button2");

        INode layout = button2.getParent();
        List<INode> deletedNodes = Collections.<INode>singletonList(button2);
        List<INode> movedNodes = Collections.<INode>singletonList(button2);
        assertSame(layout, targetNode);

        DeletionHandler handler = new DeletionHandler(deletedNodes, movedNodes, layout);
        handler.updateConstraints();

        String updated = TestNode.toXml(targetNode);
        assertEquals(
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<RelativeLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                "    xmlns:tools=\"http://schemas.android.com/tools\"\n" +
                "    android:layout_width=\"match_parent\"\n" +
                "    android:layout_height=\"match_parent\"\n" +
                "    tools:ignore=\"HardcodedText\">\n" +
                "\n" +
                "    <Button\n" +
                "        android:id=\"@+id/button1\"\n" +
                "        android:layout_width=\"wrap_content\"\n" +
                "        android:layout_height=\"wrap_content\"\n" +
                "        android:layout_alignParentLeft=\"true\"\n" +
                "        android:text=\"A\">\n" +
                "    </Button>\n" +
                "\n" +
                "    <Button\n" +
                "        android:id=\"@+id/button2\"\n" +
                "        android:layout_width=\"wrap_content\"\n" +
                "        android:layout_height=\"wrap_content\"\n" +
                "        android:layout_alignBaseline=\"@+id/button1\"\n" +
                "        android:layout_alignBottom=\"@+id/button1\"\n" +
                "        android:layout_toRightOf=\"@+id/button1\"\n" +
                "        android:text=\"B\">\n" +
                "    </Button>\n" +
                "\n" +
                "    <Button\n" +
                "        android:id=\"@+id/button3\"\n" +
                "        android:layout_width=\"wrap_content\"\n" +
                "        android:layout_height=\"wrap_content\"\n" +
                "        android:layout_alignBottom=\"@+id/button2\"\n" +
                "        android:layout_toRightOf=\"@+id/button2\"\n" +
                "        android:text=\"C\">\n" +
                "    </Button>\n" +
                "\n" +
                "</RelativeLayout>",
                updated);
        assertTrue(updated.contains(BaseViewRule.stripIdPrefix(button2.getStringAttr(ANDROID_URI,
                ATTR_ID))));
    }
}

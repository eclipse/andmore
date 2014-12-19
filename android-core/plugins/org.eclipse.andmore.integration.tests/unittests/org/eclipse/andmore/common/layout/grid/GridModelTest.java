/*
 * Copyright (C) 2011 The Android Open Source Project
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
package org.eclipse.andmore.common.layout.grid;

import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_COLUMN_COUNT;
import static com.android.SdkConstants.ATTR_LAYOUT_COLUMN;
import static com.android.SdkConstants.ATTR_LAYOUT_COLUMN_SPAN;
import static com.android.SdkConstants.ATTR_LAYOUT_ROW;
import static com.android.SdkConstants.FQCN_BUTTON;

import com.android.ide.common.api.INode;
import com.android.ide.common.api.Rect;

import java.util.Arrays;
import java.util.Collections;

import org.eclipse.andmore.common.layout.LayoutTestBase;
import org.eclipse.andmore.common.layout.TestNode;
import org.eclipse.andmore.common.layout.grid.GridModel.ViewData;


@SuppressWarnings("javadoc")
public class GridModelTest extends LayoutTestBase {
    public void testRemoveFlag() {
        assertEquals("left", GridModel.removeFlag("top", "top|left"));
        assertEquals("left", GridModel.removeFlag("top", "top | left"));
        assertEquals("top", GridModel.removeFlag("left", "top|left"));
        assertEquals("top", GridModel.removeFlag("left", "top | left"));
        assertEquals("left | center", GridModel.removeFlag("top", "top | left | center"));
        assertEquals(null, GridModel.removeFlag("top", "top"));
    }

    public void testReadModel1() {
        TestNode targetNode = TestNode.create("android.widget.GridLayout").id("@+id/GridLayout1")
                .bounds(new Rect(0, 0, 240, 480)).set(ANDROID_URI, ATTR_COLUMN_COUNT, "3");

        GridModel model = GridModel.get(null, targetNode, null);
        assertEquals(3, model.declaredColumnCount);
        assertEquals(1, model.actualColumnCount);
        assertEquals(1, model.actualRowCount);

        targetNode.add(TestNode.create(FQCN_BUTTON).id("@+id/Button1"));
        targetNode.add(TestNode.create(FQCN_BUTTON).id("@+id/Button2"));
        targetNode.add(TestNode.create(FQCN_BUTTON).id("@+id/Button3"));
        targetNode.add(TestNode.create(FQCN_BUTTON).id("@+id/Button4"));

        model = GridModel.get(null, targetNode, null);
        assertEquals(3, model.declaredColumnCount);
        assertEquals(3, model.actualColumnCount);
        assertEquals(2, model.actualRowCount);
    }

    public void testSplitColumn() {
        TestNode targetNode = TestNode.create("android.widget.GridLayout").id("@+id/GridLayout1")
                .bounds(new Rect(0, 0, 240, 480)).set(ANDROID_URI, ATTR_COLUMN_COUNT, "3");
        TestNode b1 = TestNode.create(FQCN_BUTTON).id("@+id/Button1");
        TestNode b2 = TestNode.create(FQCN_BUTTON).id("@+id/Button2");
        TestNode b3 = TestNode.create(FQCN_BUTTON).id("@+id/Button3");
        TestNode b4 = TestNode.create(FQCN_BUTTON).id("@+id/Button4");
        targetNode.add(b1);
        targetNode.add(b2);
        targetNode.add(b3);
        targetNode.add(b4);
        b4.setAttribute(ANDROID_URI, ATTR_LAYOUT_COLUMN_SPAN, "2");

        GridModel model = GridModel.get(new LayoutTestBase.TestRulesEngine(targetNode.getFqcn()),
                targetNode, null);
        assertEquals(3, model.declaredColumnCount);
        assertEquals(3, model.actualColumnCount);
        assertEquals(2, model.actualRowCount);

        model.applyPositionAttributes();
        assertEquals("0", b1.getStringAttr(ANDROID_URI, ATTR_LAYOUT_COLUMN));
        assertEquals("0", b1.getStringAttr(ANDROID_URI, ATTR_LAYOUT_ROW));

        assertEquals("1", b2.getStringAttr(ANDROID_URI, ATTR_LAYOUT_COLUMN));
        assertEquals("0", b2.getStringAttr(ANDROID_URI, ATTR_LAYOUT_ROW));

        assertEquals("2", b3.getStringAttr(ANDROID_URI, ATTR_LAYOUT_COLUMN));
        assertEquals("0", b3.getStringAttr(ANDROID_URI, ATTR_LAYOUT_ROW));

        assertEquals("0", b4.getStringAttr(ANDROID_URI, ATTR_LAYOUT_COLUMN));
        assertEquals("1", b4.getStringAttr(ANDROID_URI, ATTR_LAYOUT_ROW));
        assertEquals("2", b4.getStringAttr(ANDROID_URI, ATTR_LAYOUT_COLUMN_SPAN));

        model.splitColumn(1, false /*insertMarginColumn*/, 100 /*columnWidthDp*/, 300 /* x */);
        model.applyPositionAttributes();

        assertEquals(4, model.declaredColumnCount);
        assertEquals(4, model.actualColumnCount);
        assertEquals(2, model.actualRowCount);

        assertEquals("0", b1.getStringAttr(ANDROID_URI, ATTR_LAYOUT_COLUMN));
        assertEquals("0", b1.getStringAttr(ANDROID_URI, ATTR_LAYOUT_ROW));

        assertEquals("1", b2.getStringAttr(ANDROID_URI, ATTR_LAYOUT_COLUMN));
        assertEquals("2", b2.getStringAttr(ANDROID_URI, ATTR_LAYOUT_COLUMN_SPAN));
        assertEquals("0", b2.getStringAttr(ANDROID_URI, ATTR_LAYOUT_ROW));

        assertEquals("3", b3.getStringAttr(ANDROID_URI, ATTR_LAYOUT_COLUMN));
        assertEquals("0", b3.getStringAttr(ANDROID_URI, ATTR_LAYOUT_ROW));

        assertEquals("0", b4.getStringAttr(ANDROID_URI, ATTR_LAYOUT_COLUMN));
        assertEquals("1", b4.getStringAttr(ANDROID_URI, ATTR_LAYOUT_ROW));
        assertEquals("3", b4.getStringAttr(ANDROID_URI, ATTR_LAYOUT_COLUMN_SPAN));
    }

    public void testDeletion1() {
        String xml =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<GridLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
            "    android:layout_width=\"match_parent\"\n" +
            "    android:layout_height=\"match_parent\"\n" +
            "    android:columnCount=\"4\" >\n" +
            "\n" +
            "    <Button\n" +
            "        android:id=\"@+id/button1\"\n" +
            "        android:layout_column=\"1\"\n" +
            "        android:layout_gravity=\"left|top\"\n" +
            "        android:layout_row=\"1\"\n" +
            "        android:text=\"Button\" />\n" +
            "\n" +
            "    <TextView\n" +
            "        android:id=\"@+id/TextView1\"\n" +
            "        android:layout_column=\"3\"\n" +
            "        android:layout_gravity=\"left|top\"\n" +
            "        android:layout_row=\"1\"\n" +
            "        android:text=\"Text\" />\n" +
            "\n" +
            "    <Space\n" +
            "        android:id=\"@+id/wspace1\"\n" +
            "        android:layout_width=\"21dp\"\n" +
            "        android:layout_height=\"1dp\"\n" +
            "        android:layout_column=\"0\"\n" +
            "        android:layout_row=\"0\" />\n" +
            "\n" +
            "    <Space\n" +
            "        android:id=\"@+id/hspace1\"\n" +
            "        android:layout_width=\"1dp\"\n" +
            "        android:layout_height=\"55dp\"\n" +
            "        android:layout_column=\"0\"\n" +
            "        android:layout_row=\"0\" />\n" +
            "\n" +
            "    <Space\n" +
            "        android:id=\"@+id/wspace2\"\n" +
            "        android:layout_width=\"10dp\"\n" +
            "        android:layout_height=\"1dp\"\n" +
            "        android:layout_column=\"2\"\n" +
            "        android:layout_row=\"0\" />\n" +
            "\n" +
            "</GridLayout>";

        TestNode targetNode = TestNode.createFromXml(xml);
        assertNotNull(targetNode);

        TestNode button1 = TestNode.findById(targetNode, "@+id/button1");
        TestNode textView1 = TestNode.findById(targetNode, "@+id/TextView1");
        TestNode wspace1 = TestNode.findById(targetNode, "@+id/wspace1");
        TestNode wspace2 = TestNode.findById(targetNode, "@+id/wspace2");
        TestNode hspace1 = TestNode.findById(targetNode, "@+id/hspace1");
        assertNotNull(wspace1);
        assertNotNull(hspace1);
        assertNotNull(wspace2);
        assertNotNull(button1);
        assertNotNull(textView1);

        // Assign some bounds such that the model makes sense when merging spacer sizes
        // TODO: MAke test utility method to automatically assign half divisions!!
        button1.bounds(new Rect(90, 10, 100, 40));
        textView1.bounds(new Rect(200, 10, 100, 40));
        wspace1.bounds(new Rect(0, 0, 90, 1));
        wspace1.bounds(new Rect(190, 0, 10, 1));
        hspace1.bounds(new Rect(0, 0, 1, 10));

        GridModel model = GridModel.get(new LayoutTestBase.TestRulesEngine(targetNode.getFqcn()),
                targetNode, null);
        assertEquals(4, model.declaredColumnCount);
        assertEquals(4, model.actualColumnCount);
        assertEquals(2, model.actualRowCount);

        ViewData textViewData = model.getView(textView1);
        assertEquals(3, textViewData.column);

        // Delete button1
        button1.getParent().removeChild(button1);
        model.onDeleted(Arrays.<INode>asList(button1));
        model.applyPositionAttributes();

        assertEquals(2, model.declaredColumnCount);
        assertEquals(2, model.actualColumnCount);
        assertEquals(2, model.actualRowCount);
        assertNotNull(model.getView(textView1));
        assertNull(model.getView(button1));

        assertEquals(
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<GridLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                "    android:layout_width=\"match_parent\"\n" +
                "    android:layout_height=\"match_parent\"\n" +
                "    android:columnCount=\"2\">\n" +
                "\n" +
                "    <TextView\n" +
                "        android:id=\"@+id/TextView1\"\n" +
                "        android:layout_column=\"1\"\n" +
                "        android:layout_gravity=\"left|top\"\n" +
                "        android:layout_row=\"1\"\n" +
                "        android:text=\"Text\">\n" +
                "    </TextView>\n" +
                "\n" +
                "    <Space\n" +
                "        android:id=\"@+id/wspace1\"\n" +
                "        android:layout_width=\"66dp\"\n" +
                "        android:layout_height=\"1dp\"\n" +
                "        android:layout_column=\"0\"\n" +
                "        android:layout_row=\"0\">\n" +
                "    </Space>\n" +
                "\n" +
                "    <Space\n" +
                "        android:id=\"@+id/hspace1\"\n" +
                "        android:layout_width=\"1dp\"\n" +
                "        android:layout_height=\"55dp\"\n" +
                "        android:layout_column=\"0\"\n" +
                "        android:layout_row=\"0\">\n" +
                "    </Space>\n" +
                "\n" +
                "</GridLayout>", TestNode.toXml(targetNode));

        // Delete textView1

        textView1.getParent().removeChild(textView1);
        model.onDeleted(Arrays.<INode>asList(textView1));
        model.applyPositionAttributes();

        assertEquals(2, model.declaredColumnCount);
        assertEquals(0, model.actualColumnCount);
        assertEquals(0, model.actualRowCount);
        assertNull(model.getView(textView1));
        assertNull(model.getView(button1));

        assertEquals(
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<GridLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                "    android:layout_width=\"match_parent\"\n" +
                "    android:layout_height=\"match_parent\"\n" +
                "    android:columnCount=\"0\">\n" +
                "\n" +
                "</GridLayout>", TestNode.toXml(targetNode));

    }

    public void testDelete2() throws Exception {
        String xml =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<GridLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
            "    android:layout_width=\"match_parent\"\n" +
            "    android:layout_height=\"match_parent\"\n" +
            "    android:columnCount=\"4\"\n" +
            "    android:orientation=\"vertical\" >\n" +
            "\n" +
            "    <Button\n" +
            "        android:id=\"@+id/button1\"\n" +
            "        android:layout_column=\"0\"\n" +
            "        android:layout_columnSpan=\"2\"\n" +
            "        android:layout_gravity=\"left|top\"\n" +
            "        android:layout_row=\"0\"\n" +
            "        android:text=\"Button1\" />\n" +
            "\n" +
            "    <Button\n" +
            "        android:id=\"@+id/button3\"\n" +
            "        android:layout_column=\"1\"\n" +
            "        android:layout_columnSpan=\"2\"\n" +
            "        android:layout_gravity=\"left|top\"\n" +
            "        android:layout_row=\"1\"\n" +
            "        android:text=\"Button2\" />\n" +
            "\n" +
            "    <Button\n" +
            "        android:id=\"@+id/button2\"\n" +
            "        android:layout_column=\"2\"\n" +
            "        android:layout_gravity=\"left|top\"\n" +
            "        android:layout_row=\"0\"\n" +
            "        android:text=\"Button3\" />\n" +
            "\n" +
            "    <Space\n" +
            "        android:id=\"@+id/spacer_177\"\n" +
            "        android:layout_width=\"46dp\"\n" +
            "        android:layout_height=\"1dp\"\n" +
            "        android:layout_column=\"0\"\n" +
            "        android:layout_row=\"0\" />\n" +
            "\n" +
            "</GridLayout>";

        TestNode targetNode = TestNode.createFromXml(xml);
        assertNotNull(targetNode);

        TestNode button1 = TestNode.findById(targetNode, "@+id/button1");
        TestNode button2 = TestNode.findById(targetNode, "@+id/button2");
        TestNode button3 = TestNode.findById(targetNode, "@+id/button3");
        TestNode hspacer = TestNode.findById(targetNode, "@+id/spacer_177");
        assertNotNull(button1);
        assertNotNull(button2);
        assertNotNull(button3);
        assertNotNull(hspacer);

        // Assign some bounds such that the model makes sense when merging spacer sizes
        // TODO: MAke test utility method to automatically assign half divisions!!
        button1.bounds(new Rect(0, 0, 100, 40));
        button2.bounds(new Rect(100, 0, 100, 40));
        button3.bounds(new Rect(50, 40, 100, 40));
        hspacer.bounds(new Rect(0, 0, 50, 1));

        GridModel model = GridModel.get(new LayoutTestBase.TestRulesEngine(targetNode.getFqcn()),
                targetNode, null);
        assertEquals(4, model.declaredColumnCount);
        assertEquals(3, model.actualColumnCount);
        assertEquals(2, model.actualRowCount);

        ViewData buttonData = model.getView(button1);
        assertEquals(0, buttonData.column);

        // Delete button1
        button1.getParent().removeChild(button1);
        model.onDeleted(Arrays.<INode>asList(button1));
        model.applyPositionAttributes();

        assertEquals(3, model.declaredColumnCount);
        assertEquals(3, model.actualColumnCount);
        assertEquals(2, model.actualRowCount);
        assertNull(model.getView(button1));

        assertEquals(
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<GridLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                "    android:layout_width=\"match_parent\"\n" +
                "    android:layout_height=\"match_parent\"\n" +
                "    android:columnCount=\"3\"\n" +
                "    android:orientation=\"vertical\">\n" +
                "\n" +
                "    <Button\n" +
                "        android:id=\"@+id/button3\"\n" +
                "        android:layout_column=\"1\"\n" +
                "        android:layout_columnSpan=\"2\"\n" +
                "        android:layout_gravity=\"left|top\"\n" +
                "        android:layout_row=\"1\"\n" +
                "        android:text=\"Button2\">\n" +
                "    </Button>\n" +
                "\n" +
                "    <Button\n" +
                "        android:id=\"@+id/button2\"\n" +
                "        android:layout_column=\"2\"\n" +
                "        android:layout_gravity=\"left|top\"\n" +
                "        android:layout_row=\"0\"\n" +
                "        android:text=\"Button3\">\n" +
                "    </Button>\n" +
                "\n" +
                "    <Space\n" +
                "        android:id=\"@+id/spacer_177\"\n" +
                "        android:layout_width=\"46dp\"\n" +
                "        android:layout_height=\"1dp\"\n" +
                "        android:layout_column=\"0\"\n" +
                "        android:layout_row=\"0\">\n" +
                "    </Space>\n" +
                "\n" +
                "</GridLayout>", TestNode.toXml(targetNode));
    }

    public void testDelete3_INCOMPLETE() throws Exception {
        String xml =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<GridLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
            "  android:layout_width=\"match_parent\" android:layout_height=\"match_parent\"\n" +
            "  android:columnCount=\"6\">\n" +
            "  <Button android:id=\"@+id/button1\" android:layout_column=\"1\"\n" +
            "    android:layout_columnSpan=\"2\" android:layout_gravity=\"left|top\"\n" +
            "    android:layout_row=\"1\" android:layout_rowSpan=\"2\" android:text=\"Button\" />\n" +
            "  <TextView android:id=\"@+id/TextView1\" android:layout_column=\"4\"\n" +
            "    android:layout_gravity=\"left|top\" android:layout_row=\"1\"\n" +
            "    android:text=\"Text\" />\n" +
            "  <Button android:id=\"@+id/button3\" android:layout_column=\"5\"\n" +
            "    android:layout_gravity=\"left|top\" android:layout_row=\"2\"\n" +
            "    android:layout_rowSpan=\"2\" android:text=\"Button\" />\n" +
            "  <Button android:id=\"@+id/button2\" android:layout_column=\"2\"\n" +
            "    android:layout_columnSpan=\"3\" android:layout_gravity=\"left|top\"\n" +
            "    android:layout_row=\"4\" android:text=\"Button\" />\n" +
            "  <Space android:id=\"@+id/wspace1\" android:layout_width=\"21dp\"\n" +
            "    android:layout_height=\"1dp\" android:layout_column=\"0\"\n" +
            "    android:layout_row=\"0\" />\n" +
            "  <Space android:id=\"@+id/spacer_630\" android:layout_width=\"1dp\"\n" +
            "    android:layout_height=\"55dp\" android:layout_column=\"0\"\n" +
            "    android:layout_row=\"0\" />\n" +
            "  <Space android:id=\"@+id/wspace2\" android:layout_width=\"10dp\"\n" +
            "    android:layout_height=\"1dp\" android:layout_column=\"3\"\n" +
            "    android:layout_row=\"0\" />\n" +
            "  <Space android:id=\"@+id/spacer_619\" android:layout_width=\"59dp\"\n" +
            "    android:layout_height=\"1dp\" android:layout_column=\"1\"\n" +
            "    android:layout_row=\"0\" />\n" +
            "  <Space android:id=\"@+id/spacer_102\" android:layout_width=\"1dp\"\n" +
            "    android:layout_height=\"30dp\" android:layout_column=\"0\"\n" +
            "    android:layout_row=\"1\" />\n" +
            "  <Space android:id=\"@+id/spacer_109\" android:layout_width=\"1dp\"\n" +
            "    android:layout_height=\"28dp\" android:layout_column=\"0\"\n" +
            "    android:layout_row=\"2\" />\n" +
            "  <Space android:id=\"@+id/spacer_146\" android:layout_width=\"1dp\"\n" +
            "    android:layout_height=\"70dp\" android:layout_column=\"0\"\n" +
            "    android:layout_row=\"3\" />\n" +
            "</GridLayout>";
        TestNode targetNode = TestNode.createFromXml(xml);
        assertNotNull(targetNode);
        targetNode.assignBounds(
            "android.widget.GridLayout [0,109,480,800] <GridLayout>\n" +
            "    android.widget.Button [32,83,148,155] <Button> @+id/button1\n" +
            "    android.widget.TextView [163,83,205,109] <TextView> @+id/TextView1\n" +
            "    android.widget.Button [237,128,353,200] <Button> @+id/button3\n" +
            "    android.widget.Button [121,275,237,347] <Button> @+id/button2\n" +
            "    android.widget.Space [0,0,32,2] <Space> @+id/wspace1\n" +
            "    android.widget.Space [0,0,2,83] <Space> @+id/spacer_630\n" +
            "    android.widget.Space [148,0,163,2] <Space> @+id/wspace2\n" +
            "    android.widget.Space [32,0,121,2] <Space> @+id/spacer_619\n" +
            "    android.widget.Space [0,83,2,128] <Space> @+id/spacer_102\n" +
            "    android.widget.Space [0,128,2,170] <Space> @+id/spacer_109\n" +
            "    android.widget.Space [0,170,2,275] <Space> @+id/spacer_146\n");
        TestNode layout = TestNode.findById(targetNode, "@+id/GridLayout1");
        //TestNode button1 = TestNode.findById(targetNode, "@+id/button1");
        TestNode button2 = TestNode.findById(targetNode, "@+id/button2");
        TestNode button3 = TestNode.findById(targetNode, "@+id/button3");

        GridModel model = GridModel.get(new LayoutTestBase.TestRulesEngine(targetNode.getFqcn()),
                targetNode, null);
        assertEquals(6, model.declaredColumnCount);
        assertEquals(6, model.actualColumnCount);
        assertEquals(5, model.actualRowCount);

        // TODO: Delete button2 or button3: bad stuff happens visually
        fail("Finish test");
    }

    public void testDelete4_INCOMPLETE() {
        String xml = "" +
            "<GridLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
            "  xmlns:tools=\"http://schemas.android.com/tools\" " +
            "  android:layout_width=\"match_parent\"\n" +
            "  android:layout_height=\"match_parent\" android:columnCount=\"3\"\n" +
            "  android:gravity=\"center\" android:text=\"@string/hello_world\"\n" +
            "  tools:context=\".MainActivity\">\n" +
            "  <Button android:id=\"@+id/button2\" android:layout_column=\"1\"\n" +
            "    android:layout_columnSpan=\"2\" android:layout_gravity=\"left|top\"\n" +
            "    android:layout_row=\"1\" android:text=\"Button\" />\n" +
            "  <Button android:id=\"@+id/button1\" android:layout_column=\"1\"\n" +
            "    android:layout_columnSpan=\"2\" android:layout_gravity=\"left|top\"\n" +
            "    android:layout_row=\"3\" android:text=\"Button\" />\n" +
            "  <Space android:id=\"@+id/spacer_167\" android:layout_width=\"74dp\"\n" +
            "    android:layout_height=\"1dp\" android:layout_column=\"0\"\n" +
            "    android:layout_row=\"0\" />\n" +
            "  <Space android:id=\"@+id/spacer_133\" android:layout_width=\"1dp\"\n" +
            "    android:layout_height=\"21dp\" android:layout_column=\"0\"\n" +
            "    android:layout_row=\"0\" />\n" +
            "  <Space android:id=\"@+id/spacer_142\" android:layout_width=\"1dp\"\n" +
            "    android:layout_height=\"26dp\" android:layout_column=\"0\"\n" +
            "    android:layout_row=\"2\" />\n" +
            "  <Space android:id=\"@+id/spacer_673\" android:layout_width=\"43dp\"\n" +
            "    android:layout_height=\"1dp\" android:layout_column=\"1\"\n" +
            "    android:layout_row=\"0\" />\n" +
            "  <Space android:id=\"@+id/spacer_110\" android:layout_width=\"202dp\"\n" +
            "    android:layout_height=\"15dp\" android:layout_column=\"2\" />\n" +
            "</GridLayout>";
        TestNode targetNode = TestNode.createFromXml(xml);
        assertNotNull(targetNode);
        targetNode.assignBounds(
                "android.widget.GridLayout [0,109,480,800] <GridLayout>\n" +
                "    android.widget.Button [111,32,227,104] <Button> @+id/button2\n" +
                "    android.widget.Button [111,143,227,215] <Button> @+id/button1\n" +
                "    android.widget.Space [0,0,111,2] <Space> @+id/spacer_167\n" +
                "    android.widget.Space [0,0,2,32] <Space> @+id/spacer_133\n" +
                "    android.widget.Space [0,104,2,143] <Space> @+id/spacer_142\n" +
                "    android.widget.Space [111,0,176,2] <Space> @+id/spacer_673\n" +
                "    android.widget.Space [176,668,479,691] <Space> @+id/spacer_110");


        // Remove button2; button1 shifts to the right!

        //TestNode layout = TestNode.findById(targetNode, "@+id/GridLayout1");
        //TestNode button1 = TestNode.findById(targetNode, "@+id/button1");
        TestNode button2 = TestNode.findById(targetNode, "@+id/button2");
        TestNode button3 = TestNode.findById(targetNode, "@+id/button3");
        assertEquals(new Rect(111, 32, 227 - 111, 104 - 32), button2.getBounds());

        GridModel model = GridModel.get(new LayoutTestBase.TestRulesEngine(targetNode.getFqcn()),
                targetNode, null);
        assertEquals(3, model.declaredColumnCount);
        assertEquals(3, model.actualColumnCount);
        assertEquals(4, model.actualRowCount);
        fail("Finish test");
    }

    public void testDelete5_INCOMPLETE() {
        String xml =
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<GridLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                "  android:id=\"@+id/GridLayout1\" android:layout_width=\"match_parent\"\n" +
                "  android:layout_height=\"match_parent\" android:columnCount=\"4\"\n" +
                "  android:orientation=\"vertical\">\n" +
                "  <Button android:id=\"@+id/button1\" android:layout_column=\"0\"\n" +
                "    android:layout_gravity=\"center_horizontal|bottom\"\n" +
                "    android:layout_row=\"0\" android:text=\"Button\" />\n" +
                "  <Space android:layout_width=\"66dp\" android:layout_height=\"1dp\"\n" +
                "    android:layout_column=\"0\" android:layout_row=\"0\" />\n" +
                "  <Button android:id=\"@+id/button3\" android:layout_column=\"2\"\n" +
                "    android:layout_gravity=\"left|bottom\" android:layout_row=\"0\"\n" +
                "    android:text=\"Button\" />\n" +
                "  <Button android:id=\"@+id/button2\" android:layout_column=\"3\"\n" +
                "    android:layout_columnSpan=\"2\" android:layout_gravity=\"left|bottom\"\n" +
                "    android:layout_row=\"0\" android:text=\"Button\" />\n" +
                "  <Space android:id=\"@+id/spacer_109\" android:layout_width=\"51dp\"\n" +
                "    android:layout_height=\"1dp\" android:layout_column=\"1\"\n" +
                "    android:layout_row=\"0\" />\n" +
                "  <Space android:layout_width=\"129dp\" android:layout_height=\"1dp\"\n" +
                "    android:layout_column=\"2\" android:layout_row=\"0\" />\n" +
                "  <Space android:layout_width=\"62dp\" android:layout_height=\"1dp\"\n" +
                "    android:layout_column=\"3\" android:layout_row=\"0\" />\n" +
                "  <Space android:id=\"@+id/spacer_397\" android:layout_width=\"1dp\"\n" +
                "    android:layout_height=\"103dp\" android:layout_column=\"0\"\n" +
                "    android:layout_row=\"0\" />\n" +
                "  <Space android:layout_width=\"1dp\" android:layout_height=\"356dp\"\n" +
                "    android:layout_column=\"0\" android:layout_row=\"1\" />\n" +
                "</GridLayout>";
        TestNode targetNode = TestNode.createFromXml(xml);
        assertNotNull(targetNode);

        targetNode.assignBounds(
                "android.widget.GridLayout [0,109,480,800] <GridLayout> @+id/GridLayout1\n" +
                "    android.widget.Button [0,83,116,155] <Button> @+id/button1\n" +
                "    android.widget.Space [0,0,99,2] <Space>\n" +
                "    android.widget.Button [193,83,309,155] <Button> @+id/button3\n" +
                "    android.widget.Button [387,83,503,155] <Button> @+id/button2\n" +
                "    android.widget.Space [116,0,193,2] <Space> @+id/spacer_109\n" +
                "    android.widget.Space [193,0,387,2] <Space>\n" +
                "    android.widget.Space [387,0,480,2] <Space>\n" +
                "    android.widget.Space [0,0,2,155] <Space> @+id/spacer_397\n" +
                "    android.widget.Space [0,155,2,689] <Space>");

        // Delete button3. This causes an array out of bounds exception currently.

        //TestNode layout = TestNode.findById(targetNode, "@+id/GridLayout1");
        //TestNode button1 = TestNode.findById(targetNode, "@+id/button1");
        TestNode button2 = TestNode.findById(targetNode, "@+id/button2");
        TestNode button3 = TestNode.findById(targetNode, "@+id/button3");
        assertEquals(new Rect(387, 83, 503 - 387, 155- 83), button2.getBounds());

        GridModel model = GridModel.get(new LayoutTestBase.TestRulesEngine(targetNode.getFqcn()),
                targetNode, null);
        assertEquals(4, model.declaredColumnCount);
        assertEquals(4, model.actualColumnCount);
        assertEquals(2, model.actualRowCount);

        model.onDeleted(Collections.<INode>singletonList(button3));
        // Exception fixed; todo: Test that the model updates are correct.

        assertEquals(3, model.declaredColumnCount);
        assertEquals(3, model.actualColumnCount);
        assertEquals(2, model.actualRowCount);

        fail("Finish test");
    }

    public void testInsert1() throws Exception {
        String xml =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<GridLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
            "    android:id=\"@+id/GridLayout1\"\n" +
            "    android:layout_width=\"match_parent\"\n" +
            "    android:layout_height=\"match_parent\"\n" +
            "    android:columnCount=\"4\"\n" +
            "    android:orientation=\"vertical\" >\n" +
            "\n" +
            "    <Button\n" +
            "        android:id=\"@+id/button1\"\n" +
            "        android:layout_column=\"0\"\n" +
            "        android:layout_columnSpan=\"4\"\n" +
            "        android:layout_gravity=\"center_horizontal|bottom\"\n" +
            "        android:layout_row=\"0\"\n" +
            "        android:text=\"Button\" />\n" +
            "\n" +
            "    <Button\n" +
            "        android:id=\"@+id/button2\"\n" +
            "        android:layout_column=\"2\"\n" +
            "        android:layout_gravity=\"left|top\"\n" +
            "        android:layout_row=\"1\"\n" +
            "        android:text=\"Button\" />\n" +
            "\n" +
            "    <Button\n" +
            "        android:id=\"@+id/button3\"\n" +
            "        android:layout_column=\"3\"\n" +
            "        android:layout_gravity=\"left|top\"\n" +
            "        android:layout_row=\"1\"\n" +
            "        android:text=\"Button\" />\n" +
            "\n" +
            "    <Space\n" +
            "        android:id=\"@+id/spacer_393\"\n" +
            "        android:layout_width=\"81dp\"\n" +
            "        android:layout_height=\"1dp\"\n" +
            "        android:layout_column=\"1\"\n" +
            "        android:layout_row=\"0\" />\n" +
            "\n" +
            "    <Space\n" +
            "        android:id=\"@+id/spacer_397\"\n" +
            "        android:layout_width=\"1dp\"\n" +
            "        android:layout_height=\"103dp\"\n" +
            "        android:layout_column=\"0\"\n" +
            "        android:layout_row=\"0\" />\n" +
            "\n" +
            "</GridLayout>";

        TestNode targetNode = TestNode.createFromXml(xml);
        assertNotNull(targetNode);

        TestNode layout = TestNode.findById(targetNode, "@+id/GridLayout1");
        TestNode button1 = TestNode.findById(targetNode, "@+id/button1");
        TestNode button2 = TestNode.findById(targetNode, "@+id/button2");
        TestNode button3 = TestNode.findById(targetNode, "@+id/button3");
        TestNode hspacer = TestNode.findById(targetNode, "@+id/spacer_393");
        TestNode vspacer = TestNode.findById(targetNode, "@+id/spacer_397");
        assertNotNull(layout);
        assertNotNull(button1);
        assertNotNull(button2);
        assertNotNull(button3);
        assertNotNull(hspacer);

        // Obtained by setting ViewHierarchy.DUMP_INFO=true:
        layout.bounds(new Rect(0, 109, 480, 800-109));
        button1.bounds(new Rect(182, 83, 298-182, 155-83));
        button2.bounds(new Rect(124, 155, 240-124, 227-155));
        button3.bounds(new Rect(240, 155, 356-240, 227-155));
        hspacer.bounds(new Rect(2, 0, 124-2, 2));
        vspacer.bounds(new Rect(0, 0, 2, 155));

        GridModel model = GridModel.get(new LayoutTestBase.TestRulesEngine(targetNode.getFqcn()),
                targetNode, null);
        assertEquals(4, model.declaredColumnCount);
        assertEquals(4, model.actualColumnCount);
        assertEquals(2, model.actualRowCount);


        model.splitColumn(1, false, 21, 32);
        int index = model.getInsertIndex(2, 1);
        GridModel.ViewData next = model.getView(index);
        INode newChild = targetNode.insertChildAt(FQCN_BUTTON, index);
        next.applyPositionAttributes();
        model.setGridAttribute(newChild, ATTR_LAYOUT_COLUMN, 1);
        model.setGridAttribute(newChild, ATTR_LAYOUT_COLUMN_SPAN, 3);
    }

    public void testInsert2() throws Exception {
        // Drop into a view where there is a centered view: when dropping to the right of
        // it (on a row further down), ensure that the row span is increased for the
        // non-left-justified centered view which does not horizontally overlap the view
        String xml =
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<GridLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                "    android:id=\"@+id/GridLayout1\"\n" +
                "    android:layout_width=\"match_parent\"\n" +
                "    android:layout_height=\"match_parent\"\n" +
                "    android:columnCount=\"3\"\n" +
                "    android:orientation=\"vertical\" >\n" +
                "\n" +
                "    <Button\n" +
                "        android:id=\"@+id/button1\"\n" +
                "        android:layout_column=\"0\"\n" +
                "        android:layout_columnSpan=\"3\"\n" +
                "        android:layout_gravity=\"center_horizontal|bottom\"\n" +
                "        android:layout_row=\"0\"\n" +
                "        android:text=\"Button\" />\n" +
                "\n" +
                "    <Button\n" +
                "        android:id=\"@+id/button2\"\n" +
                "        android:layout_column=\"1\"\n" +
                "        android:layout_gravity=\"left|top\"\n" +
                "        android:layout_row=\"1\"\n" +
                "        android:text=\"Button\" />\n" +
                "\n" +
                "    <Button\n" +
                "        android:id=\"@+id/button3\"\n" +
                "        android:layout_column=\"2\"\n" +
                "        android:layout_gravity=\"left|top\"\n" +
                "        android:layout_row=\"1\"\n" +
                "        android:text=\"Button\" />\n" +
                "\n" +
                "    <Space\n" +
                "        android:id=\"@+id/spacer_393\"\n" +
                "        android:layout_width=\"81dp\"\n" +
                "        android:layout_height=\"1dp\"\n" +
                "        android:layout_column=\"0\"\n" +
                "        android:layout_row=\"0\" />\n" +
                "\n" +
                "    \n" +
                "    <Space\n" +
                "        android:id=\"@+id/spacer_397\"\n" +
                "        android:layout_width=\"1dp\"\n" +
                "        android:layout_height=\"103dp\"\n" +
                "        android:layout_column=\"0\"\n" +
                "        android:layout_row=\"0\" />\n" +
                "\n" +
                "    \n" +
                "</GridLayout>";

        TestNode targetNode = TestNode.createFromXml(xml);
        assertNotNull(targetNode);

        TestNode layout = TestNode.findById(targetNode, "@+id/GridLayout1");
        TestNode button1 = TestNode.findById(targetNode, "@+id/button1");
        TestNode button2 = TestNode.findById(targetNode, "@+id/button2");
        TestNode button3 = TestNode.findById(targetNode, "@+id/button3");
        TestNode hspacer = TestNode.findById(targetNode, "@+id/spacer_393");
        TestNode vspacer = TestNode.findById(targetNode, "@+id/spacer_397");
        assertNotNull(layout);
        assertNotNull(button1);
        assertNotNull(button2);
        assertNotNull(button3);
        assertNotNull(hspacer);

        // Obtained by setting ViewHierarchy.DUMP_INFO=true:
        layout.bounds(new Rect(0, 109, 480, 800-109));
        button1.bounds(new Rect(182, 83, 298-182, 155-83));
        button2.bounds(new Rect(122, 155, 238-122, 227-155));
        button3.bounds(new Rect(238, 155, 354-238, 227-155));
        hspacer.bounds(new Rect(0, 0, 122, 2));
        vspacer.bounds(new Rect(0, 0, 2, 155));

        GridModel model = GridModel.get(new LayoutTestBase.TestRulesEngine(targetNode.getFqcn()),
                targetNode, null);
        assertEquals(3, model.declaredColumnCount);
        assertEquals(3, model.actualColumnCount);
        assertEquals(2, model.actualRowCount);

        ViewData view = model.getView(button1);
        assertNotNull(view);
        assertEquals(0, view.column);
        assertEquals(3, view.columnSpan);
        assertEquals("3", view.node.getStringAttr(ANDROID_URI, ATTR_LAYOUT_COLUMN_SPAN));

        model.splitColumn(3, false, 53, 318);
        assertEquals(0, view.column);
        assertEquals(4, view.columnSpan);
        assertEquals("4", view.node.getStringAttr(ANDROID_URI, ATTR_LAYOUT_COLUMN_SPAN));
    }

    public void testInsert3_BROKEN() throws Exception {
        // Check that when we insert a new gap column near an existing column, the
        // view in that new column does not get moved
        String xml =
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<GridLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                "    android:id=\"@+id/GridLayout1\"\n" +
                "    android:layout_width=\"match_parent\"\n" +
                "    android:layout_height=\"match_parent\"\n" +
                "    android:columnCount=\"3\"\n" +
                "    android:orientation=\"vertical\" >\n" +
                "\n" +
                "    <Button\n" +
                "        android:id=\"@+id/button1\"\n" +
                "        android:layout_column=\"0\"\n" +
                "        android:layout_columnSpan=\"3\"\n" +
                "        android:layout_gravity=\"center_horizontal|bottom\"\n" +
                "        android:layout_row=\"0\"\n" +
                "        android:text=\"Button\" />\n" +
                "\n" +
                "    <Button\n" +
                "        android:id=\"@+id/button2\"\n" +
                "        android:layout_column=\"1\"\n" +
                "        android:layout_gravity=\"left|top\"\n" +
                "        android:layout_row=\"1\"\n" +
                "        android:text=\"Button\" />\n" +
                "\n" +
                "    <Button\n" +
                "        android:id=\"@+id/button3\"\n" +
                "        android:layout_column=\"2\"\n" +
                "        android:layout_gravity=\"left|top\"\n" +
                "        android:layout_row=\"1\"\n" +
                "        android:text=\"Button\" />\n" +
                "\n" +
                "    <Space\n" +
                "        android:id=\"@+id/spacer_393\"\n" +
                "        android:layout_width=\"81dp\"\n" +
                "        android:layout_height=\"1dp\"\n" +
                "        android:layout_column=\"0\"\n" +
                "        android:layout_row=\"0\" />\n" +
                "\n" +
                "    \n" +
                "    <Space\n" +
                "        android:id=\"@+id/spacer_397\"\n" +
                "        android:layout_width=\"1dp\"\n" +
                "        android:layout_height=\"103dp\"\n" +
                "        android:layout_column=\"0\"\n" +
                "        android:layout_row=\"0\" />\n" +
                "\n" +
                "    \n" +
                "</GridLayout>";

        TestNode targetNode = TestNode.createFromXml(xml);
        assertNotNull(targetNode);

        TestNode layout = TestNode.findById(targetNode, "@+id/GridLayout1");
        TestNode button1 = TestNode.findById(targetNode, "@+id/button1");
        TestNode button2 = TestNode.findById(targetNode, "@+id/button2");
        TestNode button3 = TestNode.findById(targetNode, "@+id/button3");
        TestNode hspacer = TestNode.findById(targetNode, "@+id/spacer_393");
        TestNode vspacer = TestNode.findById(targetNode, "@+id/spacer_397");
        assertNotNull(layout);
        assertNotNull(button1);
        assertNotNull(button2);
        assertNotNull(button3);
        assertNotNull(hspacer);

        // Obtained by setting ViewHierarchy.DUMP_INFO=true:
        layout.bounds(new Rect(0, 109, 480, 800-109));
        button1.bounds(new Rect(182, 83, 298-182, 155-83));
        button2.bounds(new Rect(122, 155, 238-122, 227-155));
        button3.bounds(new Rect(238, 155, 354-238, 227-155));
        hspacer.bounds(new Rect(0, 0, 122, 2));
        vspacer.bounds(new Rect(0, 0, 2, 155));

        GridModel model = GridModel.get(new LayoutTestBase.TestRulesEngine(targetNode.getFqcn()),
                targetNode, null);
        assertEquals(3, model.declaredColumnCount);
        assertEquals(3, model.actualColumnCount);
        assertEquals(2, model.actualRowCount);

        ViewData view = model.getView(button3);
        assertNotNull(view);
        assertEquals(2, view.column);
        assertEquals(1, view.columnSpan);
        assertNull("1", view.node.getStringAttr(ANDROID_URI, ATTR_LAYOUT_COLUMN_SPAN));

        model.splitColumn(2, true, 10, 253);
        // TODO: Finish this test: Assert that the cells are in the right place
        //assertEquals(4, view.column);
        //assertEquals(1, view.columnSpan);
        //assertEquals("4", view.node.getStringAttr(ANDROID_URI, ATTR_LAYOUT_COLUMN_SPAN));
        fail("Finish test");
    }
}

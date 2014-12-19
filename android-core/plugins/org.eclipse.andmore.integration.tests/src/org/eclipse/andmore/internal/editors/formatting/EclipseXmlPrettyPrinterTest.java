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
package org.eclipse.andmore.internal.editors.formatting;

import com.android.ide.common.xml.XmlFormatStyle;

import org.eclipse.andmore.internal.editors.formatting.EclipseXmlFormatPreferences;
import org.eclipse.andmore.internal.editors.formatting.EclipseXmlPrettyPrinter;
import org.eclipse.andmore.internal.editors.layout.gle2.DomUtilities;
import org.eclipse.andmore.internal.preferences.AdtPrefs;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import junit.framework.TestCase;

@SuppressWarnings({ "javadoc", "restriction" })
public class EclipseXmlPrettyPrinterTest extends TestCase {
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		PreferenceStore store = new PreferenceStore();
		AdtPrefs.init(store);
		AdtPrefs prefs = AdtPrefs.getPrefs();
		prefs.initializeStoreWithDefaults(store);
		prefs.loadValues(null);
		EclipseXmlFormatPreferences formatPrefs = EclipseXmlFormatPreferences.create();
		assertTrue(formatPrefs.oneAttributeOnFirstLine);
	}

	private void checkFormat(EclipseXmlFormatPreferences prefs, String baseLocation, String xml, String expected,
			String delimiter, String startNodeName, boolean openTagOnly, String endNodeName) throws Exception {

		IStructuredModel model = DomUtilities.createStructuredModel(xml);
		assertNotNull(model);
		model.setBaseLocation(baseLocation);
		Document document = null;
		if (model instanceof IDOMModel) {
			IDOMModel domModel = (IDOMModel) model;
			document = domModel.getDocument();
		} else {
			fail("Can't get DOM model");
			return;
		}
		XmlFormatStyle style = AndroidXmlFormattingStrategy.guessStyle(model, document);

		EclipseXmlPrettyPrinter printer = new EclipseXmlPrettyPrinter(prefs, style, delimiter);
		printer.setEndWithNewline(xml.endsWith("\n"));

		StringBuilder sb = new StringBuilder(1000);
		Node startNode = document;
		Node endNode = document;
		if (startNodeName != null) {
			startNode = findNode(document.getDocumentElement(), startNodeName);
		}
		if (endNodeName != null) {
			endNode = findNode(document.getDocumentElement(), endNodeName);
		}

		printer.prettyPrint(-1, document, startNode, endNode, sb, false/* openTagOnly */);
		String formatted = sb.toString();
		if (!expected.equals(formatted)) {
			System.out.println(formatted);
		}
		assertEquals(expected, formatted);
	}

	private Node findNode(Node node, String nodeName) {
		if (node.getNodeName().equals(nodeName)) {
			return node;
		}

		NodeList children = node.getChildNodes();
		for (int i = 0, n = children.getLength(); i < n; i++) {
			Node child = children.item(i);
			Node result = findNode(child, nodeName);
			if (result != null) {
				return result;
			}
		}

		return null;
	}

	protected int getCaretOffset(String fileContent, String caretLocation) {
		int caretDelta = caretLocation.indexOf("^"); //$NON-NLS-1$
		assertTrue(caretLocation, caretDelta != -1);

		String caretContext = caretLocation.substring(0, caretDelta) + caretLocation.substring(caretDelta + 1); // +1:
																												// skip
																												// "^"
		int caretContextIndex = fileContent.indexOf(caretContext);
		assertTrue("Caret content " + caretContext + " not found in file", caretContextIndex != -1);
		return caretContextIndex + caretDelta;
	}

	private void checkFormat(EclipseXmlFormatPreferences prefs, String baseLocation, String xml, String expected,
			String delimiter) throws Exception {
		checkFormat(prefs, baseLocation, xml, expected, delimiter, null, false, null);
	}

	private void checkFormat(EclipseXmlFormatPreferences prefs, String baseLocation, String xml, String expected)
			throws Exception {
		checkFormat(prefs, baseLocation, xml, expected, "\n"); //$NON-NLS-1$
	}

	private void checkFormat(String baseLocation, String xml, String expected) throws Exception {
		EclipseXmlFormatPreferences prefs = EclipseXmlFormatPreferences.create();
		checkFormat(prefs, baseLocation, xml, expected);
	}

	public void testLayout1() throws Exception {
		checkFormat("res/layout-port/layout1.xml", "<LinearLayout><Button></Button></LinearLayout>",

		"<LinearLayout>\n" + "\n" + "    <Button>\n" + "    </Button>\n" + "\n" + "</LinearLayout>");
	}

	public void testLayout2() throws Exception {
		checkFormat("res/layout-port/layout2.xml", "<LinearLayout><Button foo=\"bar\"></Button></LinearLayout>",

		"<LinearLayout>\n" + "\n" + "    <Button foo=\"bar\" >\n" + "    </Button>\n" + "\n" + "</LinearLayout>");
	}

	public void testLayout3() throws Exception {
		EclipseXmlFormatPreferences prefs = EclipseXmlFormatPreferences.create();
		prefs.oneAttributeOnFirstLine = true;
		checkFormat(prefs, "res/layout-land/layout3.xml", "<LinearLayout><Button foo=\"bar\"></Button></LinearLayout>",

		"<LinearLayout>\n" + "\n" + "    <Button foo=\"bar\" >\n" + "    </Button>\n" + "\n" + "</LinearLayout>");
	}

	public void testClosedElements() throws Exception {
		checkFormat("res/values/strings.xml", "<resources>\n" + "<item   name=\"title_container\"  type=\"id\"   />\n"
				+ "<item name=\"title_logo\" type=\"id\"/>\n" + "</resources>\n",

		"<resources>\n" + "\n" + "    <item name=\"title_container\" type=\"id\"/>\n"
				+ "    <item name=\"title_logo\" type=\"id\"/>\n" + "\n" + "</resources>\n");
	}

	public void testResources() throws Exception {
		checkFormat("res/values-us/strings.xml", "<resources><item name=\"foo\">Text value here </item></resources>",
				"<resources>\n\n" + "    <item name=\"foo\">Text value here </item>\n" + "\n</resources>");
	}

	public void testNodeTypes() throws Exception {
		// Ensures that a document with all kinds of node types is serialized
		// correctly
		checkFormat("res/layout-xlarge/layout.xml", "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" + "<!--\n" + "/**\n"
				+ " * Licensed under the Apache License, Version 2.0 (the \"License\");\n" + " */\n" + "-->\n"
				+ "<!DOCTYPE metadata [\n" + "<!ELEMENT metadata (category)*>\n" + "<!ENTITY % ISOLat2\n"
				+ "         SYSTEM \"http://www.xml.com/iso/isolat2-xml.entities\" >\n" + "]>\n" + "<LinearLayout\n"
				+ "    xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
				+ "    android:orientation=\"vertical\">\n" + "<![CDATA[\n" + "This is character data!\n"
				+ "<!-- This is not a comment! -->\n" + "and <this is not an element>\n" + "]]>         \n"
				+ "This is text: &lt; and &amp;\n" + "<!-- comment 1 \"test\"... -->\n" + "<!-- ... comment2 -->\n"
				+ "%ISOLat2;        \n" + "<!-- \n" + "Type <key>less-than</key> (&#x3C;)\n" + "-->        \n"
				+ "</LinearLayout>",

		"<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" + "<!--\n" + "/**\n"
				+ " * Licensed under the Apache License, Version 2.0 (the \"License\");\n" + " */\n" + "-->\n"
				+ "<!DOCTYPE metadata [\n" + "<!ELEMENT metadata (category)*>\n" + "<!ENTITY % ISOLat2\n"
				+ "         SYSTEM \"http://www.xml.com/iso/isolat2-xml.entities\" >\n" + "]>\n"
				+ "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
				+ "    android:orientation=\"vertical\" >\n" + "<![CDATA[\n" + "This is character data!\n"
				+ "<!-- This is not a comment! -->\n" + "and <this is not an element>\n" + "]]>\n"
				+ "This is text: &lt; and &amp;\n" + "    <!-- comment 1 \"test\"... -->\n"
				+ "    <!-- ... comment2 -->\n" + "%ISOLat2;        \n"
				+ "<!-- Type <key>less-than</key> (&#x3C;) -->\n" + "\n" + "</LinearLayout>");
	}

	public void testWindowsDelimiters() throws Exception {
		checkFormat(EclipseXmlFormatPreferences.create(), "res/layout-xlarge/layout.xml",
				"<LinearLayout><Button foo=\"bar\"></Button></LinearLayout>",

				"<LinearLayout>\r\n" + "\r\n" + "    <Button foo=\"bar\" >\r\n" + "    </Button>\r\n" + "\r\n"
						+ "</LinearLayout>", "\r\n");
	}

	public void testRemoveBlanklines() throws Exception {
		EclipseXmlFormatPreferences prefs = EclipseXmlFormatPreferences.create();
		prefs.removeEmptyLines = true;
		checkFormat(prefs, "res/layout-xlarge/layout.xml",
				"<foo><bar><baz1></baz1><baz2></baz2></bar><bar2></bar2><bar3><baz12></baz12></bar3></foo>",

				"<foo>\n" + "    <bar>\n" + "        <baz1>\n" + "        </baz1>\n" + "        <baz2>\n"
						+ "        </baz2>\n" + "    </bar>\n" + "    <bar2>\n" + "    </bar2>\n" + "    <bar3>\n"
						+ "        <baz12>\n" + "        </baz12>\n" + "    </bar3>\n" + "</foo>");
	}

	public void testRange() throws Exception {
		checkFormat(EclipseXmlFormatPreferences.create(), "res/layout-xlarge/layout.xml",
				"<LinearLayout><Button foo=\"bar\"></Button><CheckBox/></LinearLayout>", "\n"
						+ "    <Button foo=\"bar\" >\n" + "    </Button>\n" + "\n" + "    <CheckBox />\n", "\n",
				"Button", false, "CheckBox");
	}

	public void testOpenTagOnly() throws Exception {
		checkFormat(EclipseXmlFormatPreferences.create(), "res/layout-xlarge/layout.xml",
				"<LinearLayout><Button foo=\"bar\"></Button><CheckBox/></LinearLayout>", "\n"
						+ "    <Button foo=\"bar\" >\n" + "    </Button>\n", "\n",

				"Button", true, "Button");
	}

	public void testRange2() throws Exception {
		EclipseXmlFormatPreferences prefs = EclipseXmlFormatPreferences.create();
		prefs.removeEmptyLines = true;
		checkFormat(prefs, "res/layout-xlarge/layout.xml",
				"<foo><bar><baz1></baz1><baz2></baz2></bar><bar2></bar2><bar3><baz12></baz12></bar3></foo>",

				"        <baz1>\n" + "        </baz1>\n" + "        <baz2>\n" + "        </baz2>\n" + "    </bar>\n"
						+ "    <bar2>\n" + "    </bar2>\n" + "    <bar3>\n" + "        <baz12>\n"
						+ "        </baz12>\n", "\n", "baz1", false, "baz12");
	}

	public void testEOLcomments() throws Exception {
		checkFormat("res/drawable-mdpi/states.xml",
				"<selector xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
						+ "    <item android:state_pressed=\"true\"\n"
						+ "          android:color=\"#ffff0000\"/> <!-- pressed -->\n"
						+ "    <item android:state_focused=\"true\"\n"
						+ "          android:color=\"#ff0000ff\"/> <!-- focused -->\n"
						+ "    <item android:color=\"#ff000000\"/> <!-- default -->\n" + "</selector>",
				"<selector xmlns:android=\"http://schemas.android.com/apk/res/android\">\n" + "\n"
						+ "    <item android:state_pressed=\"true\" android:color=\"#ffff0000\"/> <!-- pressed -->\n"
						+ "    <item android:state_focused=\"true\" android:color=\"#ff0000ff\"/> <!-- focused -->\n"
						+ "    <item android:color=\"#ff000000\"/> <!-- default -->\n" + "\n" + "</selector>");
	}

	public void testFormatColorList() throws Exception {
		checkFormat("res/drawable-hdpi/states.xml",
				"<selector xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
						+ "<item android:state_activated=\"true\" android:color=\"#FFFFFF\"/>\n"
						+ "<item android:color=\"#777777\" /> <!-- not selected -->\n" + "</selector>",
				"<selector xmlns:android=\"http://schemas.android.com/apk/res/android\">\n" + "\n"
						+ "    <item android:state_activated=\"true\" android:color=\"#FFFFFF\"/>\n"
						+ "    <item android:color=\"#777777\"/> <!-- not selected -->\n" + "\n" + "</selector>");
	}

	public void testPreserveNewlineAfterComment() throws Exception {
		checkFormat("res/values/dimen.xml", "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
				+ "<resources><dimen name=\"colorstrip_height\">6dip</dimen>\n"
				+ "    <!-- comment1 --><dimen name=\"title_height\">45dip</dimen>\n" + "\n"
				+ "    <!-- comment2: newline above --><dimen name=\"now_playing_height\">90dip</dimen>\n"
				+ "    <dimen name=\"text_size_small\">14sp</dimen>\n" + "\n" + "\n"
				+ "    <!-- comment3: newline above and below -->\n" + "\n" + "\n" + "\n"
				+ "    <dimen name=\"text_size_medium\">18sp</dimen><dimen name=\"text_size_large\">22sp</dimen>\n"
				+ "</resources>", "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" + "<resources>\n" + "\n"
				+ "    <dimen name=\"colorstrip_height\">6dip</dimen>\n" + "    <!-- comment1 -->\n"
				+ "    <dimen name=\"title_height\">45dip</dimen>\n" + "\n" + "    <!-- comment2: newline above -->\n"
				+ "    <dimen name=\"now_playing_height\">90dip</dimen>\n"
				+ "    <dimen name=\"text_size_small\">14sp</dimen>\n" + "\n"
				+ "    <!-- comment3: newline above and below -->\n" + "\n"
				+ "    <dimen name=\"text_size_medium\">18sp</dimen>\n"
				+ "    <dimen name=\"text_size_large\">22sp</dimen>\n" + "\n" + "</resources>");
	}

	public void testPlurals() throws Exception {
		checkFormat(
				"res/values-us/strings.xml",
				"<resources xmlns:xliff=\"urn:oasis:names:tc:xliff:document:1.2\">\n"
						+ "<string name=\"toast_sync_error\">Sync error: <xliff:g id=\"error\">%1$s</xliff:g></string>\n"
						+ "<string name=\"session_subtitle\"><xliff:g id=\"time\">%1$s</xliff:g> in <xliff:g id=\"room\">%2$s</xliff:g></string>\n"
						+ "<plurals name=\"now_playing_countdown\">\n"
						+ "<item quantity=\"zero\"><xliff:g id=\"remaining_time\">%2$s</xliff:g></item>\n"
						+ "<item quantity=\"one\"><xliff:g id=\"number_of_days\">%1$s</xliff:g> day, <xliff:g id=\"remaining_time\">%2$s</xliff:g></item>\n"
						+ "<item quantity=\"other\"><xliff:g id=\"number_of_days\">%1$s</xliff:g> days, <xliff:g id=\"remaining_time\">%2$s</xliff:g></item>\n"
						+ "</plurals>\n" + "</resources>",
				"<resources xmlns:xliff=\"urn:oasis:names:tc:xliff:document:1.2\">\n"
						+ "\n"
						+ "    <string name=\"toast_sync_error\">Sync error: <xliff:g id=\"error\">%1$s</xliff:g></string>\n"
						+ "    <string name=\"session_subtitle\"><xliff:g id=\"time\">%1$s</xliff:g> in <xliff:g id=\"room\">%2$s</xliff:g></string>\n"
						+ "\n"
						+ "    <plurals name=\"now_playing_countdown\">\n"
						+ "        <item quantity=\"zero\"><xliff:g id=\"remaining_time\">%2$s</xliff:g></item>\n"
						+ "        <item quantity=\"one\"><xliff:g id=\"number_of_days\">%1$s</xliff:g> day, <xliff:g id=\"remaining_time\">%2$s</xliff:g></item>\n"
						+ "        <item quantity=\"other\"><xliff:g id=\"number_of_days\">%1$s</xliff:g> days, <xliff:g id=\"remaining_time\">%2$s</xliff:g></item>\n"
						+ "    </plurals>\n" + "\n" + "</resources>");
	}

	public void testMultiAttributeResource() throws Exception {
		checkFormat(
				"res/values-us/strings.xml",
				"<resources><string name=\"debug_enable_debug_logging_label\" translatable=\"false\">Enable extra debug logging?</string></resources>",
				"<resources>\n"
						+ "\n"
						+ "    <string name=\"debug_enable_debug_logging_label\" translatable=\"false\">Enable extra debug logging?</string>\n"
						+ "\n" + "</resources>");
	}

	public void testMultilineCommentAlignment() throws Exception {
		checkFormat("res/values-us/strings.xml", "<resources>"
				+ "    <!-- Deprecated strings - Move the identifiers to this section, mark as DO NOT TRANSLATE,\n"
				+ "         and remove the actual text.  These will be removed in a bulk operation. -->\n"
				+ "    <!-- Do Not Translate.  Unused string. -->\n"
				+ "    <string name=\"meeting_invitation\"></string>\n" + "</resources>", "<resources>\n" + "\n"
				+ "    <!--\n"
				+ "         Deprecated strings - Move the identifiers to this section, mark as DO NOT TRANSLATE,\n"
				+ "         and remove the actual text.  These will be removed in a bulk operation.\n" + "    -->\n"
				+ "    <!-- Do Not Translate.  Unused string. -->\n"
				+ "    <string name=\"meeting_invitation\"></string>\n" + "\n" + "</resources>");
	}

	public void testLineCommentSpacing() throws Exception {
		checkFormat("res/values-us/strings.xml", "<resources>\n" + "\n"
				+ "    <dimen name=\"colorstrip_height\">6dip</dimen>\n" + "    <!-- comment1 -->\n"
				+ "    <dimen name=\"title_height\">45dip</dimen>\n" + "    <!-- comment2: no newlines -->\n"
				+ "    <dimen name=\"now_playing_height\">90dip</dimen>\n"
				+ "    <dimen name=\"text_size_small\">14sp</dimen>\n" + "\n"
				+ "    <!-- comment3: newline above and below -->\n" + "\n"
				+ "    <dimen name=\"text_size_medium\">18sp</dimen>\n"
				+ "    <dimen name=\"text_size_large\">22sp</dimen>\n" + "\n" + "</resources>",

		"<resources>\n" + "\n" + "    <dimen name=\"colorstrip_height\">6dip</dimen>\n" + "    <!-- comment1 -->\n"
				+ "    <dimen name=\"title_height\">45dip</dimen>\n" + "    <!-- comment2: no newlines -->\n"
				+ "    <dimen name=\"now_playing_height\">90dip</dimen>\n"
				+ "    <dimen name=\"text_size_small\">14sp</dimen>\n" + "\n"
				+ "    <!-- comment3: newline above and below -->\n" + "\n"
				+ "    <dimen name=\"text_size_medium\">18sp</dimen>\n"
				+ "    <dimen name=\"text_size_large\">22sp</dimen>\n" + "\n" + "</resources>");
	}

	public void testCommentHandling() throws Exception {
		checkFormat(EclipseXmlFormatPreferences.create(), "res/layout/layout1.xml", "<foo >\n" + "\n"
				+ "    <!-- abc\n" + "         def\n" + "         ghi -->\n" + "\n" + "    <!-- abc\n" + "    def\n"
				+ "    ghi -->\n" + "    \n" + "<!-- abc\n" + "def\n" + "ghi -->\n" + "\n" + "</foo>",

		"<foo>\n" + "\n" + "    <!--\n" + "         abc\n" + "         def\n" + "         ghi\n" + "    -->\n" + "\n"
				+ "\n" + "    <!--\n" + "    abc\n" + "    def\n" + "    ghi\n" + "    -->\n" + "\n" + "\n"
				+ "    <!--\n" + "abc\n" + "def\n" + "ghi\n" + "    -->\n" + "\n" + "</foo>");
	}

	public void testCommentHandling2() throws Exception {
		checkFormat(EclipseXmlFormatPreferences.create(), "res/layout-xlarge/layout.xml", "<foo >\n"
				+ "    <!-- multi -->\n" + "\n" + "    <bar />\n" + "</foo>",

		"<foo>\n" + "\n" + "    <!-- multi -->\n" + "\n" + "    <bar />\n" + "\n" + "</foo>");
	}

	public void testMenus1() throws Exception {
		checkFormat(EclipseXmlFormatPreferences.create(), "res/menu/menu1.xml",
		// http://code.google.com/p/android/issues/detail?id=21383
				"<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
						+ "<menu xmlns:android=\"http://schemas.android.com/apk/res/android\" >\n" + "\n"
						+ "    <item\n" + "        android:id=\"@+id/menu_debug\"\n"
						+ "        android:icon=\"@android:drawable/ic_menu_more\"\n"
						+ "        android:showAsAction=\"ifRoom|withText\"\n"
						+ "        android:title=\"@string/menu_debug\">\n" + "    \n" + "        <menu>\n"
						+ "                <item\n"
						+ "                    android:id=\"@+id/menu_debug_clearCache_memory\"\n"
						+ "                    android:icon=\"@android:drawable/ic_menu_delete\"\n"
						+ "                    android:showAsAction=\"ifRoom|withText\"\n"
						+ "                    android:title=\"@string/menu_debug_clearCache_memory\"/>\n" + "    \n"
						+ "                <item\n"
						+ "                    android:id=\"@+id/menu_debug_clearCache_file\"\n"
						+ "                    android:icon=\"@android:drawable/ic_menu_delete\"\n"
						+ "                    android:showAsAction=\"ifRoom|withText\"\n"
						+ "                    android:title=\"@string/menu_debug_clearCache_file\"/>\n"
						+ "        </menu>\n" + "    </item>\n" + "</menu>",

				"<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
						+ "<menu xmlns:android=\"http://schemas.android.com/apk/res/android\" >\n" + "\n"
						+ "    <item\n" + "        android:id=\"@+id/menu_debug\"\n"
						+ "        android:icon=\"@android:drawable/ic_menu_more\"\n"
						+ "        android:showAsAction=\"ifRoom|withText\"\n"
						+ "        android:title=\"@string/menu_debug\">\n" + "        <menu>\n"
						+ "            <item\n" + "                android:id=\"@+id/menu_debug_clearCache_memory\"\n"
						+ "                android:icon=\"@android:drawable/ic_menu_delete\"\n"
						+ "                android:showAsAction=\"ifRoom|withText\"\n"
						+ "                android:title=\"@string/menu_debug_clearCache_memory\"/>\n"
						+ "            <item\n" + "                android:id=\"@+id/menu_debug_clearCache_file\"\n"
						+ "                android:icon=\"@android:drawable/ic_menu_delete\"\n"
						+ "                android:showAsAction=\"ifRoom|withText\"\n"
						+ "                android:title=\"@string/menu_debug_clearCache_file\"/>\n"
						+ "        </menu>\n" + "    </item>\n" + "\n" + "</menu>");
	}

	public void testMenus2() throws Exception {
		EclipseXmlFormatPreferences prefs = EclipseXmlFormatPreferences.create();
		prefs.removeEmptyLines = true;
		checkFormat(prefs, "res/drawable-hdpi/layerlist.xml",
		// http://code.google.com/p/android/issues/detail?id=21346
				"<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
						+ "<layer-list xmlns:android=\"http://schemas.android.com/apk/res/android\">\n" + "  <item>\n"
						+ "    <shape android:shape=\"rectangle\">\n" + "      <stroke\n"
						+ "        android:width=\"1dip\"\n" + "        android:color=\"@color/line_separator\"/>\n"
						+ "      <solid android:color=\"@color/event_header_background\"/>\n" + "    </shape>\n"
						+ "  </item>\n" + "  <item\n" + "    android:bottom=\"1dip\"\n" + "    android:top=\"1dip\">\n"
						+ "    <shape android:shape=\"rectangle\">\n" + "      <stroke\n"
						+ "        android:width=\"1dip\"\n"
						+ "        android:color=\"@color/event_header_background\"/>\n"
						+ "      <solid android:color=\"@color/transparent\"/>\n" + "    </shape>\n" + "  </item>\n"
						+ "</layer-list>",

				"<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
						+ "<layer-list xmlns:android=\"http://schemas.android.com/apk/res/android\" >\n"
						+ "    <item>\n" + "        <shape android:shape=\"rectangle\" >\n" + "            <stroke\n"
						+ "                android:width=\"1dip\"\n"
						+ "                android:color=\"@color/line_separator\" />\n"
						+ "            <solid android:color=\"@color/event_header_background\" />\n"
						+ "        </shape>\n" + "    </item>\n" + "    <item\n" + "        android:bottom=\"1dip\"\n"
						+ "        android:top=\"1dip\">\n" + "        <shape android:shape=\"rectangle\" >\n"
						+ "            <stroke\n" + "                android:width=\"1dip\"\n"
						+ "                android:color=\"@color/event_header_background\" />\n"
						+ "            <solid android:color=\"@color/transparent\" />\n" + "        </shape>\n"
						+ "    </item>\n" + "</layer-list>");
	}

	public void testMenus3() throws Exception {
		checkFormat(EclipseXmlFormatPreferences.create(), "res/menu/menu1.xml",
		// http://code.google.com/p/android/issues/detail?id=21227
				"<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
						+ "<menu xmlns:android=\"http://schemas.android.com/apk/res/android\" >\n" + "\n"
						+ "    <item\n" + "        android:icon=\"@android:drawable/ic_menu_more\"\n"
						+ "        android:title=\"@string/account_list_menu_more\">\n" + "        <menu>\n"
						+ "            <item\n"
						+ "                android:id=\"@+id/account_list_menu_backup_restore\"\n"
						+ "                android:icon=\"@android:drawable/ic_menu_save\"\n"
						+ "                android:title=\"@string/account_list_menu_backup_restore\"/>\n"
						+ "        </menu>\n" + "    </item>\n" + "\n" + "</menu>",

				"<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
						+ "<menu xmlns:android=\"http://schemas.android.com/apk/res/android\" >\n" + "\n"
						+ "    <item\n" + "        android:icon=\"@android:drawable/ic_menu_more\"\n"
						+ "        android:title=\"@string/account_list_menu_more\">\n" + "        <menu>\n"
						+ "            <item\n"
						+ "                android:id=\"@+id/account_list_menu_backup_restore\"\n"
						+ "                android:icon=\"@android:drawable/ic_menu_save\"\n"
						+ "                android:title=\"@string/account_list_menu_backup_restore\"/>\n"
						+ "        </menu>\n" + "    </item>\n" + "\n" + "</menu>");

	}

	public void testColors1() throws Exception {
		checkFormat(EclipseXmlFormatPreferences.create(), "res/values/colors.xml", "<resources>\n"
				+ "  <color name=\"enrollment_error\">#99e21f14</color>\n" + "\n"
				+ "  <color name=\"service_starting_up\">#99000000</color>\n" + "</resources>",

		"<resources>\n" + "\n" + "    <color name=\"enrollment_error\">#99e21f14</color>\n"
				+ "    <color name=\"service_starting_up\">#99000000</color>\n" + "\n" + "</resources>");
	}

	public void testEclipseFormatStyle1() throws Exception {
		EclipseXmlFormatPreferences prefs = new EclipseXmlFormatPreferences() {
			@Override
			public String getOneIndentUnit() {
				return "\t";
			}

			@Override
			public int getTabWidth() {
				return 8;
			}
		};
		checkFormat(prefs, "res/values/colors.xml", "<resources>\n"
				+ "  <color name=\"enrollment_error\">#99e21f14</color>\n" + "\n"
				+ "  <color name=\"service_starting_up\">#99000000</color>\n" + "</resources>",

		"<resources>\n" + "\n" + "\t<color name=\"enrollment_error\">#99e21f14</color>\n"
				+ "\t<color name=\"service_starting_up\">#99000000</color>\n" + "\n" + "</resources>");
	}

	public void testEclipseFormatStyle2() throws Exception {
		EclipseXmlFormatPreferences prefs = new EclipseXmlFormatPreferences() {
			@Override
			public String getOneIndentUnit() {
				return "  ";
			}

			@Override
			public int getTabWidth() {
				return 2;
			}
		};
		prefs.useEclipseIndent = true;
		checkFormat(prefs, "res/values/colors.xml", "<resources>\n"
				+ "  <color name=\"enrollment_error\">#99e21f14</color>\n" + "\n"
				+ "  <color name=\"service_starting_up\">#99000000</color>\n" + "</resources>",

		"<resources>\n" + "\n" + "  <color name=\"enrollment_error\">#99e21f14</color>\n"
				+ "  <color name=\"service_starting_up\">#99000000</color>\n" + "\n" + "</resources>");
	}

	public void testNameSorting() throws Exception {
		checkFormat("res/values/dimen.xml", "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" + "<resources>\n"
				+ "    <attr format=\"integer\" name=\"no\" />\n" + "</resources>",

		"<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" + "<resources>\n" + "\n"
				+ "    <attr name=\"no\" format=\"integer\" />\n" + "\n" + "</resources>");
	}

	public void testStableText() throws Exception {
		checkFormat("res/layout/stable.xml", "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
				+ "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
				+ "    android:layout_width=\"match_parent\"\n" + "    android:layout_height=\"match_parent\"\n"
				+ "    android:orientation=\"vertical\">\n" + "    Hello World\n" + "\n" + "</LinearLayout>",

		"<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
				+ "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
				+ "    android:layout_width=\"match_parent\"\n" + "    android:layout_height=\"match_parent\"\n"
				+ "    android:orientation=\"vertical\" >\n" + "    Hello World\n" + "\n" + "</LinearLayout>");
	}

	public void testResources1() throws Exception {
		checkFormat("res/values/strings.xml", "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" + "<resources>\n" + "\n"
				+ "        <string name=\"test_string\">a\n" + "                </string>\n" + "\n" + "</resources>",

		"<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" + "<resources>\n" + "\n"
				+ "    <string name=\"test_string\">a</string>\n" + "\n" + "</resources>");
	}

	public void testMarkup() throws Exception {
		checkFormat("res/values/strings.xml", "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" + "<resources>\n" + "\n"
				+ "<string name=\"welcome\">Welcome to <b>Android</b>!</string>"
				+ "<string name=\"glob_settings_top_text\"><b>To install a 24 Clock Widget, "
				+ "please <i>long press</i> in Home Screen.</b> Configure the Global Settings " + "here.</string>" + ""
				+ "\n" + "</resources>",

		"<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" + "<resources>\n" + "\n"
				+ "    <string name=\"welcome\">Welcome to <b>Android</b>!</string>\n"
				+ "    <string name=\"glob_settings_top_text\"><b>To install a 24 Clock Widget, "
				+ "please <i>long press</i> in Home Screen.</b> Configure the Global Settings " + "here.</string>\n"
				+ "\n" + "</resources>");
	}

	public void testPreserveEntities() throws Exception {
		// Ensure that entities such as &gt; in the input string are preserved
		// in the output
		// format
		checkFormat("res/values/strings.xml", "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
				+ "<resources><string name=\"untitled\">&lt;untitled2></string>\n"
				+ "<string name=\"untitled2\">&lt;untitled2&gt;</string>\n"
				+ "<string name=\"untitled3\">&apos;untitled3&quot;</string></resources>\n",

		"<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" + "<resources>\n" + "\n"
				+ "    <string name=\"untitled\">&lt;untitled2></string>\n"
				+ "    <string name=\"untitled2\">&lt;untitled2&gt;</string>\n"
				+ "    <string name=\"untitled3\">&apos;untitled3&quot;</string>\n" + "\n" + "</resources>\n");
	}

	public void testCData1() throws Exception {
		checkFormat("res/values/strings.xml", "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" + "<resources>\n"
				+ "    <string name=\"foo\"><![CDATA[bar]]></string>\n" + "</resources>",

		"<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" + "<resources>\n" + "\n"
				+ "    <string name=\"foo\"><![CDATA[bar]]></string>\n" + "\n" + "</resources>");
	}

	public void testCData2() throws Exception {
		checkFormat("res/values/strings.xml", "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" + "<resources>\n"
				+ "    <string name=\"foo1\"><![CDATA[bar1\n" + "bar2\n" + "bar3]]></string>\n"
				+ "    <string name=\"foo2\"><![CDATA[bar]]></string>\n" + "</resources>",

		"<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" + "<resources>\n" + "\n" + "    <string name=\"foo1\">\n"
				+ "<![CDATA[bar1\n" + "bar2\n" + "bar3]]>\n" + "    </string>\n"
				+ "    <string name=\"foo2\"><![CDATA[bar]]></string>\n" + "\n" + "</resources>");
	}

	public void testComplexString() throws Exception {
		checkFormat("res/values/strings.xml", "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" + "<resources>\n"
				+ "<string name=\"progress_completed_export_all\">The database has "
				+ "<b>successfully</b> been exported into: <br /><br /><font size=\"14\">"
				+ "\\\"<i>%s</i>\\\"</font></string>" + "</resources>",

		"<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" + "<resources>\n" + "\n"
				+ "    <string name=\"progress_completed_export_all\">The database has "
				+ "<b>successfully</b> been exported into: <br /><br /><font size=\"14\">"
				+ "\\\"<i>%s</i>\\\"</font></string>\n" + "\n" + "</resources>");
	}

	public void test52887() throws Exception {
		// https://code.google.com/p/android/issues/detail?id=52887
		checkFormat("res/layout/relative.xml",

		"<!--Comment-->\n" + "<RelativeLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
				+ "  android:layout_width=\"match_parent\"\n" + "  android:layout_height=\"match_parent\"/>\n",

		"<!-- Comment -->\n" + "<RelativeLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
				+ "    android:layout_width=\"match_parent\"\n" + "    android:layout_height=\"match_parent\" />\n");
	}

	public void testPreserveLastNewline() throws Exception {
		checkFormat("res/values/strings.xml", "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" + "<resources>\n"
				+ "<string name=\"progress_completed_export_all\">The database has "
				+ "<b>successfully</b> been exported into: <br /><br /><font size=\"14\">"
				+ "\\\"<i>%s</i>\\\"</font></string>" + "</resources>\n",

		"<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" + "<resources>\n" + "\n"
				+ "    <string name=\"progress_completed_export_all\">The database has "
				+ "<b>successfully</b> been exported into: <br /><br /><font size=\"14\">"
				+ "\\\"<i>%s</i>\\\"</font></string>\n" + "\n" + "</resources>\n");
	}
}

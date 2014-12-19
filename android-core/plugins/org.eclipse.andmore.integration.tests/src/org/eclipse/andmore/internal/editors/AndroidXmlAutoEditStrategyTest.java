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
package org.eclipse.andmore.internal.editors;

import org.eclipse.andmore.internal.editors.AndroidXmlAutoEditStrategy;
import org.eclipse.andmore.internal.editors.AndroidXmlEditor;
import org.eclipse.andmore.internal.editors.layout.refactoring.AdtProjectTest;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

@SuppressWarnings("javadoc")
public class AndroidXmlAutoEditStrategyTest extends AdtProjectTest {

	public void checkInsertNewline(String before, String after) throws Exception {
		AndroidXmlAutoEditStrategy s = new AndroidXmlAutoEditStrategy();

		// All tests just operate on the "edithandling" document; the contents
		// are
		// ignored and replaced with the before-document passed in
		IFile file = getLayoutFile(getProject(), "edithandling.xml");

		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		assertNotNull(page);
		IEditorPart editor = IDE.openEditor(page, file);
		assertTrue(editor instanceof AndroidXmlEditor);
		AndroidXmlEditor layoutEditor = (AndroidXmlEditor) editor;
		ISourceViewer viewer = layoutEditor.getStructuredSourceViewer();

		String newDocumentContent = stripCaret(before);
		IDocument document = viewer.getDocument();
		document.replace(0, document.getLength(), newDocumentContent);

		// Determine the offset, and possibly make text range selections as well
		int offset = updateCaret(viewer, before);

		DocumentCommand c = new TestDocumentCommand();
		c.doit = true;
		c.offset = offset;
		c.caretOffset = -1;
		c.length = 0;
		c.text = "\n";

		s.customizeDocumentCommand(document, c);

		if (c.doit) {
			if (c.length == 0 && c.text == null) {
				return;
			}

			document.replace(c.offset, c.length, c.text);

			int caretOffset = c.offset + c.text.length();

			// The shiftsCaret flag doesn't behave the way it's documented to
			// if (c.shiftsCaret && c.caretOffset != -1) {
			if (c.caretOffset != -1) {
				caretOffset = c.caretOffset;
			}
			viewer.setSelectedRange(caretOffset, 0);
		}

		String text = document.get();
		Point selectedRange = viewer.getSelectedRange();
		assert selectedRange.y == 0;
		String textWithCaret = text;
		if (selectedRange.x >= 0) {
			textWithCaret = text.substring(0, selectedRange.x) + "^" + text.substring(selectedRange.x);
		}

		assertEquals(after, textWithCaret);
	}

	private static String stripCaret(String s) {
		int index = s.indexOf('^');
		assertTrue(index != -1);
		return s.substring(0, index) + s.substring(index + 1);
	}

	public void testCornerCase1() throws Exception {
		checkInsertNewline("^", "\n^");
	}

	public void testCornerCase2() throws Exception {
		checkInsertNewline("\n^",

		"\n\n^");
	}

	public void testCornerCase3() throws Exception {
		checkInsertNewline("    ^",

		"    \n" + "    ^");
	}

	public void testSimpleIndentation1() throws Exception {
		checkInsertNewline("   ^ ",

		"   \n" + "   ^ ");
	}

	public void testSimpleIndentation2() throws Exception {
		checkInsertNewline("\n" + "   foo^\n",

		"\n" + "   foo\n" + "   ^\n");
	}

	public void testSimpleIndentation3() throws Exception {
		checkInsertNewline("\n" + "    <newtag>^\n",

		"\n" + "    <newtag>\n" + "        ^\n");
	}

	public void testSimpleIndentation4() throws Exception {
		checkInsertNewline("\n" + "    <newtag/>^\n",

		"\n" + "    <newtag/>\n" + "    ^\n");
	}

	public void testSimpleIndentation5() throws Exception {
		checkInsertNewline("\n" + "    <newtag^\n", "\n" + "    <newtag\n" + "        ^\n");
	}

	public void testSplitAttribute() throws Exception {
		checkInsertNewline("\n" + "    <newtag ^attribute='value'/>\n",

		"\n" + "    <newtag \n" + "        ^attribute='value'/>\n");
	}

	public void testIndentationInComments1() throws Exception {
		// Make sure that inside a comment we ignore tags etc
		checkInsertNewline("<!--\n   foo^\n--->\n",

		"<!--\n   foo\n   ^\n--->\n");
	}

	public void testIndentationInComments2() throws Exception {
		// Make sure that inside a comment we ignore tags etc
		checkInsertNewline("\n" + "<!--\n" + "<foo><^\n" + "-->\n",

		"\n" + "<!--\n" + "<foo><\n" + "^\n" + "-->\n");
	}

	public void testSurroundCaret() throws Exception {
		checkInsertNewline("\n" + "    <item>^</item>\n",

		"\n" + "    <item>\n" + "        ^\n" + "    </item>\n");
	}

	public void testSurroundCaret2() throws Exception {
		// This test combines both surround with and continuing earlier lines
		// (where
		// it searches for a matching tag)
		checkInsertNewline("\n" + "    <foo\n" + "        name='value'>^</foo>\n",

		"\n" + "    <foo\n" + "        name='value'>\n" + "        ^\n" + "    </foo>\n");
	}

	public void testContinueEarlierLine1() throws Exception {
		// Here we need to indent to the exact location of an earlier line
		checkInsertNewline("\n" + "    <foo\n" + "        name='value'/>^\n",

		"\n" + "    <foo\n" + "        name='value'/>\n" + "    ^\n");
	}

	public void testContinueEarlierLine2() throws Exception {
		checkInsertNewline("\n" + "    <foo\n" + "        name='value'></foo>^\n",

		"\n" + "    <foo\n" + "        name='value'></foo>\n" + "    ^\n");
		// Note that
		// <foo
		// >
		// </foo>
		// does not require special handling, this only happens with the closing
		// tag is sharing
		// a line.
	}

	public void testContinueEarlierLine3() throws Exception {
		// Make sure the code to look up the corresponding opening tag works
		// properly
		checkInsertNewline("\n" + "    <foo\n" + "        name='value'><bar></bar><baz/></foo>^\n",

		"\n" + "    <foo\n" + "        name='value'><bar></bar><baz/></foo>\n" + "    ^\n");
	}

	public void testContinueEarlierLine4() throws Exception {
		checkInsertNewline("    <Button\n" + "        android:id=\"@+id/button1\"\n"
				+ "        android:layout_width=\"wrap_content\"\n"
				+ "        android:layout_height=\"wrap_content\"\n" + "        android:text=\"Button\" >^\n"
				+ "    </Button>\n",

		"    <Button\n" + "        android:id=\"@+id/button1\"\n" + "        android:layout_width=\"wrap_content\"\n"
				+ "        android:layout_height=\"wrap_content\"\n" + "        android:text=\"Button\" >\n"
				+ "        ^\n" + "    </Button>\n");
	}

	public void testIndent() throws Exception {
		checkInsertNewline("    <Button\n" + "        attr=\"value\"></Button>^\n",

		"    <Button\n" + "        attr=\"value\"></Button>\n" + "    ^\n" + "");
	}

	public void testLineBeginning1() throws Exception {
		// Test that if you insert on a blank line, we just add a newline and
		// indent
		checkInsertNewline("<foo>\n" + "^\n" + "</foo>",

		"<foo>\n" + "\n" + "    ^\n" + "</foo>");
	}

	public void testLineBeginning2() throws Exception {
		// Test that if you insert with the caret on the beginning of a line
		// that has
		// content, we insert an indent correctly
		checkInsertNewline("<foo>\n" + "^    <bar/>\n" + "</foo>",

		"<foo>\n" + "\n" + "    ^<bar/>\n" + "</foo>");
	}

	public void testLineBeginning3() throws Exception {
		checkInsertNewline("<foo>\n" + "    <bar>\n" + "^\n" + "        <baz/>\n" + "    </bar>\n" + "</foo>",

		"<foo>\n" + "    <bar>\n" + "\n" + "        ^\n" + "        <baz/>\n" + "    </bar>\n" + "</foo>");

	}

	public void testLineBeginning4() throws Exception {
		// Test that if you insert with the caret on the beginning of a line
		// that has
		// content, we insert an indent correctly
		checkInsertNewline("<foo>\n" + "    <bar>\n" + "\n" + "^        <baz/>\n" + "    </bar>\n" + "</foo>",

		"<foo>\n" + "    <bar>\n" + "\n" + "\n" + "        ^<baz/>\n" + "    </bar>\n" + "</foo>");
	}

	public void testLineBeginning5() throws Exception {
		// Test that if you insert with the caret on the beginning of a line
		// that has
		// content, we insert an indent correctly
		checkInsertNewline("<foo>\n" + "    <bar>\n" + "\n" + "    ^    <baz/>\n" + "    </bar>\n" + "</foo>",

		"<foo>\n" + "    <bar>\n" + "\n" + "    \n" + "        ^<baz/>\n" + "    </bar>\n" + "</foo>");
	}

	public void testLineBeginning6() throws Exception {

		checkInsertNewline("    <foo>\n" + "        <bar>\n" + "            \n" + "        \n" + "^        </bar>\n"
				+ "    </foo>\n",

		"    <foo>\n" + "        <bar>\n" + "            \n" + "        \n" + "\n" + "        ^</bar>\n"
				+ "    </foo>\n");
	}

	public void testBlankContinuation() throws Exception {

		checkInsertNewline("    <foo>\n" + "        <bar>\n" + "            ^\n" + "        </bar>\n" + "    </foo>\n"
				+ "",

		"    <foo>\n" + "        <bar>\n" + "            \n" + "            ^\n" + "        </bar>\n" + "    </foo>\n"
				+ "");
	}

	public void testIssue22332a() throws Exception {
		checkInsertNewline("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" + "<resources>\n" + "\n"
				+ "    <string name=\"hello\">Hello World, MainActivity!</string>^\n" + "\n" + "</resources>",

		"<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" + "<resources>\n" + "\n"
				+ "    <string name=\"hello\">Hello World, MainActivity!</string>\n" + "    ^\n" + "\n"
				+ "</resources>");
	}

	public void testIssue22332b() throws Exception {
		checkInsertNewline("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" + "<resources>\n" + "\n"
				+ "    <string name=\"hello\">Hello World, MainActivity!</string>\n" + "    ^\n" + "\n"
				+ "</resources>",

		"<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" + "<resources>\n" + "\n"
				+ "    <string name=\"hello\">Hello World, MainActivity!</string>\n" + "    \n" + "    ^\n" + "\n"
				+ "</resources>");
	}

	/**
	 * To test When you press / after < I should reindent the current line. For
	 * example, if you have <foo> <bar> </ the moment you've typed this we
	 * should dedent it back out When you press newline we need to reindent
	 */

	/** Subclassed for test usage since constructor is protected */
	private class TestDocumentCommand extends DocumentCommand {
		public TestDocumentCommand() {
		}
	}
}

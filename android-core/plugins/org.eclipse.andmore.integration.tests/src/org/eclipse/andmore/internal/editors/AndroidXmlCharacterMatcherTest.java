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

import static org.junit.Assert.*;

import org.eclipse.andmore.internal.editors.AndroidXmlCharacterMatcher;
import org.eclipse.andmore.internal.editors.AndroidXmlEditor;
import org.eclipse.andmore.internal.editors.layout.refactoring.AdtProjectTest;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.junit.Ignore;
import org.junit.Test;

@SuppressWarnings("restriction")
public class AndroidXmlCharacterMatcherTest extends AdtProjectTest {
	@Test
	public void testGotoMatchingFwd1() throws Exception {
		checkGotoMatching("<app^lication android:icon", "^</application>");
	}

	@Test
	public void testGotoMatchingFwd2() throws Exception {
		checkGotoMatching("^<application android:icon", "^</application>");
	}

	@Test
	public void testGotoMatchingFwd3() throws Exception {
		checkGotoMatching("<application^ android:icon", "^</application>");
	}

	@Test
	public void testGotoMatchingBwd1() throws Exception {
		checkGotoMatching("^</application>", "^<application android:icon");
	}

	@Test
	public void testGotoMatchingBwd2() throws Exception {
		checkGotoMatching("<^/application>", "^<application android:icon");
	}

	@Test
	public void testGotoMatchingBwd3() throws Exception {
		checkGotoMatching("</^application>", "^<application android:icon");
	}

	@Test
	public void testGotoMatchingBwd4() throws Exception {
		checkGotoMatching("</app^lication>", "^<application android:icon");
	}

	@Test
	public void testGotoMatchingBwd5() throws Exception {
		checkGotoMatching("</^application>", "^<application android:icon");
	}

	@Test
	public void testGotoMatchingBwd6() throws Exception {
		checkGotoMatching("</^application>", "^<application android:icon");
	}

	@Test
	public void testGotoMatchingFwd4() throws Exception {
		checkGotoMatching("<intent-filter^>", "^</intent-filter>");
	}

	@Test
	public void testGotoMatchingFwd5() throws Exception {
		checkGotoMatching("<intent-filter>^", "^</intent-filter>");
	}

	@Test
	public void testGotoMatchingFallback() throws Exception {
		// Character matching is done by the superclass; ensure that fallback to
		// the
		// other XML matchers is working
		checkGotoMatching("android:icon=^\"@drawable/icon\"", "android:icon=\"@drawable/icon^\"");
	}

	private void checkGotoMatching(IFile file, String caretBefore, String caretAfter) throws Exception {
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		assertNotNull(page);
		IEditorPart editor = IDE.openEditor(page, file);
		assertTrue(editor instanceof AndroidXmlEditor);
		AndroidXmlEditor layoutEditor = (AndroidXmlEditor) editor;
		ISourceViewer viewer = layoutEditor.getStructuredSourceViewer();

		int caretPosBefore = updateCaret(viewer, caretBefore);

		AndroidXmlCharacterMatcher matcher = new AndroidXmlCharacterMatcher();
		IStructuredDocument document = layoutEditor.getStructuredDocument();

		IRegion match = matcher.match(document, caretPosBefore);
		assertNotNull(match);

		String text = document.get();
		final String after = stripCaret(caretAfter);
		int index = text.indexOf(after);
		int caretPosAfter = match.getOffset() - index;
		String textWithCaret = after.substring(0, caretPosAfter) + "^" + after.substring(caretPosAfter);

		assertEquals(caretAfter, textWithCaret);
	}

	private static String stripCaret(String s) {
		int index = s.indexOf('^');
		assertTrue(index != -1);
		return s.substring(0, index) + s.substring(index + 1);
	}

	private void checkGotoMatching(String caretBefore, String caretAfter) throws Exception {
		checkGotoMatching(getTestDataFile(getProject(), "manifest.xml", "AndroidManifest.xml", true), caretBefore,
				caretAfter);
	}
}

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
package org.eclipse.andmore.internal.editors.layout.refactoring;

import static com.android.SdkConstants.FQCN_RADIO_BUTTON;

import org.eclipse.andmore.internal.editors.layout.refactoring.ChangeViewRefactoring;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.w3c.dom.Element;

import java.util.List;

public class ChangeViewRefactoringTest extends RefactoringTest {

	public void testChangeView1() throws Exception {
		checkRefactoring("sample1a.xml", FQCN_RADIO_BUTTON, "@+id/button1", "@+id/button6");
	}

	public void testChangeView2() throws Exception {
		// Tests (1) updating references to the renamed id of the changed
		// widgets
		// (e.g. button3 is renamed to imageButton1 and layout references to
		// button3
		// must be updated), and (2) removal of attributes not available in the
		// new type
		// (the text property is removed since it is not available on the new
		// widget
		// type ImageButton)
		checkRefactoring("sample2.xml", "android.widget.ImageButton", "@+id/button3", "@+id/button5");
	}

	private void checkRefactoring(String basename, String newType, String... ids) throws Exception {
		assertTrue(ids.length > 0);

		IFile file = getLayoutFile(getProject(), basename);
		TestContext info = setupTestContext(file, basename);
		TestLayoutEditorDelegate layoutEditor = info.mLayoutEditorDelegate;
		List<Element> selectedElements = getElements(info.mElement, ids);

		ChangeViewRefactoring refactoring = new ChangeViewRefactoring(selectedElements, layoutEditor);
		refactoring.setType(newType);

		List<Change> changes = refactoring.computeChanges(new NullProgressMonitor());
		checkEdits(basename, changes);
	}
}

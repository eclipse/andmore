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

import static org.junit.Assert.*;
import static com.android.SdkConstants.FQCN_GESTURE_OVERLAY_VIEW;
import static com.android.SdkConstants.FQCN_LINEAR_LAYOUT;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Element;

import java.util.List;

public class WrapInRefactoringTest extends RefactoringTest {

	@Test
	public void testWrapIn1() throws Exception {
		// Test wrapping view: should indent view
		checkRefactoring("sample3.xml", FQCN_LINEAR_LAYOUT, "@+id/button2");
	}

	@Test
	public void testWrapIn2() throws Exception {
		// Test wrapping the root: should move namespace
		checkRefactoring("sample3.xml", FQCN_GESTURE_OVERLAY_VIEW, "@+id/newlinear");
	}

	@Test
	public void testWrapIn3() throws Exception {
		// Test wrap multiple adjacent elements - should wrap all as a unit
		checkRefactoring("sample3.xml", FQCN_LINEAR_LAYOUT, "@+id/button2", "@+id/android_logo");
	}

	private void checkRefactoring(String basename, String fqcn, String... ids) throws Exception {
		assertTrue(ids.length > 0);

		IFile file = getLayoutFile(getProject(), basename);
		TestContext info = setupTestContext(file, basename);
		TestLayoutEditorDelegate layoutEditor = info.mLayoutEditorDelegate;
		List<Element> selectedElements = getElements(info.mElement, ids);

		WrapInRefactoring refactoring = new WrapInRefactoring(selectedElements, layoutEditor);
		refactoring.setType(fqcn);
		List<Change> changes = refactoring.computeChanges(new NullProgressMonitor());
		checkEdits(basename, changes);
	}
}

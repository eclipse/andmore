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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.w3c.dom.Element;

import java.util.List;

public class UnwrapRefactoringTest extends RefactoringTest {

	public void testUnwrap1() throws Exception {
		// Unwrap view with parent, no children - this will unwrap the parent
		// (frame layout)
		checkRefactoring("unwrap.xml", "@+id/button");
	}

	public void testUnwrap2() throws Exception {
		// Unwrap view with parent and children; this should unwrap the element
		// itself
		checkRefactoring("unwrap.xml", "@+id/frame");
	}

	public void testUnwrap3() throws Exception {
		// Unwrap root: should transfer namespace
		checkRefactoring("unwrap.xml", "@+id/linear");
	}

	private void checkRefactoring(String basename, String id) throws Exception {
		IFile file = getLayoutFile(getProject(), basename);
		TestContext info = setupTestContext(file, basename);
		TestLayoutEditorDelegate layoutEditor = info.mLayoutEditorDelegate;
		List<Element> selectedElements = getElements(info.mElement, id);
		assertEquals(1, selectedElements.size());

		UnwrapRefactoring refactoring = new UnwrapRefactoring(selectedElements, layoutEditor);

		RefactoringStatus status = refactoring.checkInitialConditions(new NullProgressMonitor());
		assertFalse(status.hasError());
		List<Change> changes = refactoring.computeChanges(new NullProgressMonitor());
		checkEdits(basename, changes);
	}
}

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
import static org.eclipse.andmore.internal.editors.layout.refactoring.UseCompoundDrawableRefactoring.combine;

import org.eclipse.andmore.AdtUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Element;

import java.util.List;

@SuppressWarnings("javadoc")
@Ignore
public class UseCompoundDrawableRefactoringTest extends RefactoringTest {
	@Test
	public void testCombine() throws Exception {
		assertNull(combine(null, null));
		assertNull(combine("@dimen/foo", "@dimen/bar"));
		assertNull(combine("@dimen/foo", "@dimen/bar"));
		assertNull(combine("1sp", "@dimen/bar"));
		assertNull(combine("1sp", "2dp"));
		assertNull(combine(null, ""));
		assertNull(combine("", null));

		assertEquals("@dimen/foo", combine(null, "@dimen/foo"));
		assertEquals("@dimen/foo", combine("@dimen/foo", null));
		assertEquals("5sp", combine("5sp", null));

		assertEquals("10sp", combine("8sp", "2sp"));
		assertEquals("50dp", combine("30dp", "20dp"));
	}

	@Test
	public void test1() throws Exception {
		// Test converting an image above a text view
		checkRefactoring("refactoring/usecompound/compound1.xml", "@+id/layout1");
	}

	@Test
	public void test2() throws Exception {
		// Test converting an image below a text view
		checkRefactoring("refactoring/usecompound/compound2.xml", "@+id/layout2");
	}

	@Test
	public void test3() throws Exception {
		// Test converting an image to the left of a text view
		checkRefactoring("refactoring/usecompound/compound3.xml", "@+id/layout3");
	}

	@Test
	public void test4() throws Exception {
		// Test converting an image to the right of a text view
		checkRefactoring("refactoring/usecompound/compound4.xml", "@+id/layout4");
	}

	@Test
	public void test5() throws Exception {
		// Test converting an image where the LinearLayout is referenced (in a
		// relative layout)
		// and the text view has an id
		checkRefactoring("refactoring/usecompound/compound_all.xml", "@+id/layout2");
	}

	@Test
	public void test6() throws Exception {
		// Test converting an image where the LinearLayout is referenced (in a
		// relative layout)
		// and the text view does not have an id
		checkRefactoring("refactoring/usecompound/compound_all.xml", "@+id/layout3");
	}

	@Test
	public void test7() throws Exception {
		// Test converting where a namespace needs to be migrated
		checkRefactoring("refactoring/usecompound/compound5.xml", "@+id/layout");
	}

	@Test
	public void test8() throws Exception {
		// Test padding handling
		checkRefactoring("refactoring/usecompound/compound6.xml", "@+id/layout1");
	}

	@Test
	public void test9() throws Exception {
		// Test margin combination
		checkRefactoring("refactoring/usecompound/compound7.xml", "@+id/layout1");
	}

	private void checkRefactoring(String basename, String id) throws Exception {
		IFile file = getLayoutFile(getProject(), basename);
		TestContext info = setupTestContext(file, basename);
		TestLayoutEditorDelegate layoutEditor = info.mLayoutEditorDelegate;
		List<Element> selectedElements = getElements(info.mElement, new String[] { id });

		UseCompoundDrawableRefactoring refactoring = new UseCompoundDrawableRefactoring(selectedElements, layoutEditor);
		List<Change> changes = refactoring.computeChanges(new NullProgressMonitor());

		CompositeChange cc = new CompositeChange("Combined from unit test", changes.toArray(new Change[changes.size()]));
		cc.markAsSynthetic();
		addCleanupDir(AdtUtils.getAbsolutePath(getProject()).toFile());

		checkEdits(basename, changes);
	}
}

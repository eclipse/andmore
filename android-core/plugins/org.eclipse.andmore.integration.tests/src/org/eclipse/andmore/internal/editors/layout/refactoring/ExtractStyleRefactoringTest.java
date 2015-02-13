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

import com.android.utils.Pair;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Ignore
public class ExtractStyleRefactoringTest extends RefactoringTest {
	@Override
	protected boolean testCaseNeedsUniqueProject() {
		return true;
	}

	@Test
	public void testExtract1() throws Exception {
		// Test extracting into a new style file
		checkRefactoring("extractstyle1.xml", "newstyles.xml", "newstyle", false /* removeExtracted */,
				false /* applyStyle */, null, 1, "@+id/button2");
	}

	@Test
	public void testExtract1b() throws Exception {
		// Extract and apply new style
		checkRefactoring("extractstyle1.xml", "newstyles2.xml", "newstyle", false /* removeExtracted */,
				true /* applyStyle */, null, 2, "@+id/button2");
	}

	@Test
	public void testExtract1c() throws Exception {
		// Extract and remove extracted
		checkRefactoring("extractstyle1.xml", "newstyles3.xml", "newstyle", true /* removeExtracted */,
				false /* applyStyle */, null, 2, "@+id/button2");
	}

	@Test
	public void testExtract1d() throws Exception {
		// Extract and apply style and remove extracted
		checkRefactoring("extractstyle1.xml", "newstyles4.xml", "newstyle", true /* removeExtracted */,
				true /* applyStyle */, null, 2, "@+id/button2");
	}

	@Test
	public void testExtract2() throws Exception {
		getTestDataFile(getProject(), "navigationstyles.xml", "res/values/navigationstyles.xml");

		// -Modify- the existing styles.xml file
		checkRefactoring("extractstyle1.xml", "navigationstyles.xml", "newstyle", true /* removeExtracted */,
				true /* applyStyle */, null, 2, "@+id/button2");
	}

	@Test
	public void testExtract3() throws Exception {
		// Select multiple elements - overlap in values.
		checkRefactoring("extractstyle1.xml", "newstyles4.xml", "newstyle", true /* removeExtracted */,
				true /* applyStyle */, null, 2, "@+id/button1", "@+id/button2");
	}

	// This test fails for some reason - not in the refactoring (checked
	// manually)
	// but the DOM model returns null when run in a test context.
	@Test
	public void testExtract4() throws Exception {
		// Test extracting on a single caret position over an attribute: Should
		// extract
		// just that one attribute
		checkRefactoringByOffset("extractstyle1.xml", "newstyles5.xml", "newstyle", true /* removeExtracted */,
				true /* applyStyle */, null, 2, "android:text^Color=\"#FF00FF\"", "android:text^Color=\"#FF00FF\"");
	}

	@Test
	public void testExtract5() throws Exception {
		// Test extracting on a range selection inside an element: should
		// extract just
		// the attributes that overlap the selection
		checkRefactoringByOffset("extractstyle1.xml", "newstyles6.xml", "newstyle", true /* removeExtracted */,
				true /* applyStyle */, null, 2, "android:^textSize=\"20pt",
				"android:id=\"@+id/button1\" android:layout_a^lignParentBottom");
	}

	@Test
	public void testExtract6() throws Exception {
		// Test extracting on a single caret position which is not over any
		// attributes:
		checkRefactoringByOffset("extractstyle1.xml", "newstyles7.xml", "newstyle", true /* removeExtracted */,
				true /* applyStyle */, null, 0, "<Bu^tton", "<Bu^tton");
	}

	@Test
	public void testExtract7() throws Exception {
		// Verify that even with a different namespace prefix we end up with
		// android:
		// in the extracted style
		checkRefactoring("extractstyle2.xml", "newstyles8.xml", "newstyle", true /* removeExtracted */,
				true /* applyStyle */, null, 2, "@+id/button1", "@+id/button2");
	}

	@Test
	public void testExtract8() throws Exception {
		// Test setting parent style
		checkRefactoring("extractstyle1.xml", "newstyles3.xml", "newstyle", true /* removeExtracted */,
				false /* applyStyle */, "android:Widget.Button", 2, "@+id/button2");
	}

	// Check extract style on a selection of elements
	private void checkRefactoring(String basename, String styleFileName, String newStyleName, boolean removeExtracted,
			boolean applyStyle, String parentStyle, int expectedModifiedFileCount, String... ids) throws Exception {
		assertTrue(ids.length > 0);

		IFile file = getLayoutFile(getProject(), basename);
		TestContext info = setupTestContext(file, basename);
		TestLayoutEditorDelegate layoutEditor = info.mLayoutEditorDelegate;
		List<Element> selectedElements = getElements(info.mElement, ids);

		// Open the file such that ModelManager.getExistingModelForRead() in
		// DomUtilities
		// will succeed
		IWorkbench workbench = PlatformUI.getWorkbench();
		IWorkbenchWindow activeWorkbenchWindow = workbench.getActiveWorkbenchWindow();
		IWorkbenchPage page = activeWorkbenchWindow.getActivePage();
		IDE.openEditor(page, file);

		ExtractStyleRefactoring refactoring = new ExtractStyleRefactoring(selectedElements, layoutEditor);
		checkRefactoring(basename, styleFileName, newStyleName, removeExtracted, applyStyle, parentStyle,
				expectedModifiedFileCount, file, refactoring);
	}

	// Check extract style against a set of editor text locations
	private void checkRefactoringByOffset(String basename, String styleFileName, String newStyleName,
			boolean removeExtracted, boolean applyStyle, String parentStyle, int expectedModifiedFileCount,
			String beginCaretLocation, String endCaretLocation) throws Exception {
		IFile file = getLayoutFile(getProject(), basename);
		int beginOffset = getCaretOffset(file, beginCaretLocation);
		int endOffset = getCaretOffset(file, endCaretLocation);

		TestContext info = setupTestContext(file, basename);
		TestLayoutEditorDelegate layoutEditor = info.mLayoutEditorDelegate;

		// Open the file such that ModelManager.getExistingModelForRead() in
		// DomUtilities
		// will succeed
		IWorkbench workbench = PlatformUI.getWorkbench();
		IWorkbenchWindow activeWorkbenchWindow = workbench.getActiveWorkbenchWindow();
		IWorkbenchPage page = activeWorkbenchWindow.getActivePage();
		IDE.openEditor(page, file);

		ITextSelection selection = new TextSelection(beginOffset, endOffset - beginOffset);
		ExtractStyleRefactoring refactoring = new ExtractStyleRefactoring(file, layoutEditor, selection, null);
		checkRefactoring(basename, styleFileName, newStyleName, removeExtracted, applyStyle, parentStyle,
				expectedModifiedFileCount, file, refactoring);
	}

	// Common test code used by the other two check methods
	private void checkRefactoring(String basename, String styleFileName, String newStyleName, boolean removeExtracted,
			boolean applyStyle, String parentStyle, int expectedModifiedFileCount, IFile file,
			ExtractStyleRefactoring refactoring) throws Exception {
		refactoring.setStyleName(newStyleName);
		refactoring.setApplyStyle(applyStyle);
		refactoring.setRemoveExtracted(removeExtracted);
		refactoring.setStyleFileName(styleFileName);
		refactoring.setParent(parentStyle);

		// Pick the attributes to extract -- for now everything (and where there
		// are
		// conflicting values, pick the first one)
		Pair<Map<String, List<Attr>>, Set<Attr>> result = refactoring.getAvailableAttributes();
		Map<String, List<Attr>> availableAttributes = result.getFirst();
		Set<Attr> selected = result.getSecond();
		List<Attr> chosenAttributes = new ArrayList<Attr>();
		for (List<Attr> list : availableAttributes.values()) {
			Collections.sort(list, new Comparator<Attr>() {
				@Override
				public int compare(Attr a1, Attr a2) {
					return a1.getValue().compareTo(a2.getValue());
				}
			});
			Attr attr = list.get(0);
			if (selected.contains(attr)) {
				chosenAttributes.add(attr);
			}
		}
		refactoring.setChosenAttributes(chosenAttributes);

		List<Change> changes = refactoring.computeChanges(new NullProgressMonitor());
		assertEquals(expectedModifiedFileCount, changes.size());

		Map<IPath, String> fileToGolden = new HashMap<IPath, String>();
		IPath sourcePath = file.getProjectRelativePath();
		fileToGolden.put(sourcePath, basename);
		IPath newPath = refactoring.getStyleFile(getProject()).getProjectRelativePath();
		fileToGolden.put(newPath, styleFileName);

		checkEdits(changes, fileToGolden, true);

		int modifiedFileCount = 0;
		for (Change change : changes) {
			if (change instanceof TextFileChange) {
				modifiedFileCount++;
			}
		}
		assertEquals(expectedModifiedFileCount, modifiedFileCount);
	}

}

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

import static com.android.SdkConstants.DOT_XML;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.editors.layout.refactoring.ExtractIncludeRefactoring;
import org.eclipse.andmore.internal.preferences.AdtPrefs;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.w3c.dom.Element;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExtractIncludeRefactoringTest extends RefactoringTest {

	@Override
	protected boolean autoFormat() {
		return false;
	}

	@Override
	protected boolean testCaseNeedsUniqueProject() {
		// Because some of these tests look at ALL layouts in the project
		// to identify matches
		return true;
	}

	public void testExtract1() throws Exception {
		// Basic: Extract a single button
		checkRefactoring("sample3.xml", "newlayout1", false, null, 2, false /* diffs */, "@+id/button2");
	}

	public void testExtract2() throws Exception {
		// Extract a couple of elements
		checkRefactoring("sample3.xml", "newlayout2", false, null, 2, false /* diffs */, "@+id/button2",
				"@+id/android_logo");
	}

	public void testExtract3() throws Exception {
		// Test to make sure layout attributes are updated
		checkRefactoring("sample2.xml", "newlayout3", false, null, 2, false /* diffs */, "@+id/button3");
	}

	public void testExtract4() throws Exception {
		// Tests extracting from -multiple- files (as well as with custom
		// android namespace
		// prefix)

		// Make sure the variation-files exist
		Map<IPath, String> extraFiles = new HashMap<IPath, String>();
		extraFiles.put(getTestDataFile(getProject(), "sample3-variation1.xml", "res/layout-land/sample3.xml")
				.getProjectRelativePath(), "sample3-variation1.xml");
		extraFiles.put(getTestDataFile(getProject(), "sample3-variation2.xml", "res/layout-xlarge-land/sample3.xml")
				.getProjectRelativePath(), "sample3-variation2.xml");

		checkRefactoring("sample3.xml", "newlayout3", true, extraFiles, 4, false /* diffs */, "@+id/android_logo");
	}

	public void testExtract5() throws Exception {
		// Tests extracting from multiple files with -contiguous regions-.

		// Make sure the variation-files exist
		Map<IPath, String> extraFiles = new HashMap<IPath, String>();
		extraFiles.put(getTestDataFile(getProject(), "sample3-variation1.xml", "res/layout-land/sample3.xml")
				.getProjectRelativePath(), "sample3-variation1.xml");
		extraFiles.put(getTestDataFile(getProject(), "sample3-variation2.xml", "res/layout-xlarge-land/sample3.xml")
				.getProjectRelativePath(), "sample3-variation2.xml");

		checkRefactoring("sample3.xml", "newlayout3", true, extraFiles, 4, false /* diffs */, "@+id/android_logo",
				"@+id/button1");
	}

	public void testExtract6() throws Exception {
		// Tests extracting from multiple files where the layouts are completely
		// different/unrelated files

		// Create the duplicate files
		Map<IPath, String> extraFiles = new HashMap<IPath, String>();
		extraFiles.put(getTestDataFile(getProject(), "sample1a.xml", "res/layout/sample1a.xml")
				.getProjectRelativePath(), "sample1a.xml");
		extraFiles.put(getTestDataFile(getProject(), "sample7.xml", "res/layout/sample7.xml").getProjectRelativePath(),
				"sample7.xml");
		extraFiles.put(getTestDataFile(getProject(), "sample8.xml", "res/layout/sample8.xml").getProjectRelativePath(),
				"sample8.xml");

		checkRefactoring("sample7.xml", "newlayout6", true, extraFiles, 4, true /* diffs */, "@+id/linearLayout4");
	}

	public void testExtract7() throws Exception {
		// Just like testExtract6, except we turn on auto-formatting
		IPreferenceStore store = AdtPlugin.getDefault().getPreferenceStore();
		AdtPrefs.init(store);
		AdtPrefs prefs = AdtPrefs.getPrefs();
		prefs.initializeStoreWithDefaults(store);
		store.setValue(AdtPrefs.PREFS_FORMAT_GUI_XML, true);
		prefs.loadValues(null);

		assertTrue(AdtPrefs.getPrefs().getFormatGuiXml());

		testExtract6();
	}

	private void checkRefactoring(String basename, String layoutName, boolean replaceOccurrences,
			Map<IPath, String> extraFiles, int expectedModifiedFileCount, boolean createDiffs, String... ids)
			throws Exception {
		assertTrue(ids.length > 0);

		IFile file = getLayoutFile(getProject(), basename);
		TestContext info = setupTestContext(file, basename);
		TestLayoutEditorDelegate layoutEditor = info.mLayoutEditorDelegate;
		List<Element> selectedElements = getElements(info.mElement, ids);

		ExtractIncludeRefactoring refactoring = new ExtractIncludeRefactoring(selectedElements, layoutEditor);
		refactoring.setLayoutName(layoutName);
		refactoring.setReplaceOccurrences(replaceOccurrences);
		List<Change> changes = refactoring.computeChanges(new NullProgressMonitor());

		assertTrue(changes.size() >= 3);

		Map<IPath, String> fileToGolden = new HashMap<IPath, String>();
		IPath sourcePath = file.getProjectRelativePath();
		fileToGolden.put(sourcePath, basename);
		IPath newPath = sourcePath.removeLastSegments(1).append(layoutName + DOT_XML);
		fileToGolden.put(newPath, layoutName + DOT_XML);
		if (extraFiles != null) {
			fileToGolden.putAll(extraFiles);
		}

		checkEdits(changes, fileToGolden, createDiffs);

		int modifiedFileCount = 0;
		for (Change change : changes) {
			if (change instanceof TextFileChange) {
				modifiedFileCount++;
			}
		}
		assertEquals(expectedModifiedFileCount, modifiedFileCount);
	}
}

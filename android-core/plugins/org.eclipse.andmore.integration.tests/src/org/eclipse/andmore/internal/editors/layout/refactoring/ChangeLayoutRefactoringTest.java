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

import static com.android.SdkConstants.FQCN_GRID_LAYOUT;
import static com.android.SdkConstants.FQCN_LINEAR_LAYOUT;
import static com.android.SdkConstants.FQCN_RELATIVE_LAYOUT;

import org.eclipse.andmore.internal.editors.layout.gle2.CanvasViewInfo;
import org.eclipse.andmore.internal.editors.layout.refactoring.ChangeLayoutRefactoring;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.w3c.dom.Element;

import java.util.Collections;
import java.util.List;

@SuppressWarnings("javadoc")
public class ChangeLayoutRefactoringTest extends RefactoringTest {

	public void testChangeLayout1a() throws Exception {
		// Test a basic layout which performs some nesting -- tests basic grid
		// layout conversion
		checkRefactoring("sample1a.xml", true);
	}

	public void testChangeLayout1b() throws Exception {
		// Same as 1a, but with different formatting to look for edit handling
		// to for example
		// remove a line that is made empty when its only attribute is removed
		checkRefactoring("sample1b.xml", true);
	}

	public void testChangeLayout2() throws Exception {
		// Test code which analyzes an embedded RelativeLayout
		checkRefactoring("sample2.xml", true);
	}

	public void testChangeLayout3() throws Exception {
		// Test handling of LinearLayout "weight" attributes on its children:
		// the child with
		// weight > 0 should fill and subsequent children attach on the
		// bottom/right
		checkRefactoring("sample3.xml", true);
	}

	public void testChangeLayout4() throws Exception {
		checkRefactoring("sample4.xml", true);
	}

	public void testChangeLayout5() throws Exception {
		// Test handling of LinearLayout "gravity" attributes on its children
		checkRefactoring("sample5.xml", true);
	}

	public void testChangeLayout6() throws Exception {
		// Check handling of the LinearLayout "baseline" attribute
		checkRefactoring("sample6.xml", true);
	}

	public void testGridLayout1() throws Exception {
		checkRefactoring(FQCN_GRID_LAYOUT, "sample1a.xml", true);
	}

	public void testGridLayout2() throws Exception {
		// Test code which analyzes an embedded RelativeLayout
		checkRefactoring(FQCN_GRID_LAYOUT, "sample2.xml", true);
	}

	public void testGridLayout5() throws Exception {
		// Test handling of LinearLayout "gravity" attributes on its children
		checkRefactoring(FQCN_GRID_LAYOUT, "sample5.xml", true);
	}

	public void testConvertToGrid() throws Exception {
		checkRefactoring(FQCN_GRID_LAYOUT, "sample9.xml", true);
	}

	public void testConvertFromGrid() throws Exception {
		checkRefactoring(FQCN_LINEAR_LAYOUT, "sample10.xml", true);
	}

	private void checkRefactoring(String basename, boolean flatten) throws Exception {
		checkRefactoring(FQCN_RELATIVE_LAYOUT, basename, flatten);
	}

	public void testInitialAttributes() throws Exception {
		checkRefactoring(FQCN_LINEAR_LAYOUT, "sample10.xml", true, "android:orientation=vertical");
	}

	public void testInsertSpacer() throws Exception {
		checkRefactoring(FQCN_GRID_LAYOUT, "sample11.xml", true);
	}

	private void checkRefactoring(String newLayoutType, String basename, boolean flatten) throws Exception {
		checkRefactoring(newLayoutType, basename, flatten, null);
	}

	@Override
	protected int getMinSdk() {
		return 14;
	}

	private void checkRefactoring(String newLayoutType, String basename, boolean flatten, String initialAttributes)
			throws Exception {
		IFile file = getLayoutFile(getProject(), basename);
		TestContext info = setupTestContext(file, basename);
		TestLayoutEditorDelegate layoutEditor = info.mLayoutEditorDelegate;
		CanvasViewInfo rootView = info.mRootView;
		Element element = info.mElement;

		List<Element> selectedElements = Collections.singletonList(element);
		ChangeLayoutRefactoring refactoring = new ChangeLayoutRefactoring(selectedElements, layoutEditor);
		refactoring.setFlatten(flatten);
		refactoring.setType(newLayoutType);
		if (initialAttributes != null) {
			refactoring.setInitializedAttributes(initialAttributes);
		}
		refactoring.setRootView(rootView);

		List<Change> changes = refactoring.computeChanges(new NullProgressMonitor());
		checkEdits(basename, changes);
	}
}

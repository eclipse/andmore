/*
 * Copyright (C) 2012 The Android Open Source Project
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
package org.eclipse.andmore.internal.refactorings.renamepackage;

import com.android.annotations.NonNull;

import org.eclipse.andmore.internal.editors.manifest.ManifestInfo;
import org.eclipse.andmore.internal.refactorings.core.RefactoringTestBase;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.Name;

@SuppressWarnings("javadoc")
public class ApplicationPackageNameRefactoringTest extends RefactoringTestBase {
	public void testRefactor1() throws Exception {
		renamePackage(TEST_PROJECT, "my.pkg.name",

		"CHANGES:\n" + "-------\n"
				+ "[x] MainActivity.java - /testRefactor1/src/com/example/refactoringtest/MainActivity.java\n"
				+ "  @@ -7 +7\n" + "  + import my.pkg.name.R;\n" + "\n" + "\n"
				+ "[x] Make Manifest edits - /testRefactor1/AndroidManifest.xml\n" + "  @@ -3 +3\n"
				+ "  -     package=\"com.example.refactoringtest\"\n" + "  +     package=\"my.pkg.name\"\n"
				+ "  @@ -25 +25\n" + "  -             android:name=\".MainActivity2\"\n"
				+ "  +             android:name=\"com.example.refactoringtest.MainActivity2\"");
	}

	public void testRefactor2() throws Exception {
		// Tests custom view handling
		renamePackage(TEST_PROJECT2, "my.pkg.name",

		"CHANGES:\n" + "-------\n"
				+ "[x] MainActivity.java - /testRefactor2/src/com/example/refactoringtest/MainActivity.java\n"
				+ "  @@ -7 +7\n" + "  + import my.pkg.name.R;\n" + "\n" + "\n"
				+ "[x] Make Manifest edits - /testRefactor2/AndroidManifest.xml\n" + "  @@ -3 +3\n"
				+ "  -     package=\"com.example.refactoringtest\"\n" + "  +     package=\"my.pkg.name\"\n"
				+ "  @@ -25 +25\n" + "  -             android:name=\".MainActivity2\"\n"
				+ "  +             android:name=\"com.example.refactoringtest.MainActivity2\"");
	}

	public void testRefactor3() throws Exception {
		// Tests BuildConfig imports and updates
		renamePackage(TEST_PROJECT3, "my.pkg.name",

		"CHANGES:\n" + "-------\n"
				+ "[x] MoreCode.java - /testRefactor3/src/com/example/refactoringtest/subpkg/MoreCode.java\n"
				+ "  @@ -7 +7\n" + "  - import com.example.refactoringtest.BuildConfig;\n"
				+ "  - import com.example.refactoringtest.Manifest;\n" + "  - import com.example.refactoringtest.R;\n"
				+ "  + import my.pkg.name.BuildConfig;\n" + "  + import my.pkg.name.Manifest;\n"
				+ "  + import my.pkg.name.R;\n" + "\n" + "\n"
				+ "[x] MainActivity.java - /testRefactor3/src/com/example/refactoringtest/MainActivity.java\n"
				+ "  @@ -7 +7\n" + "  + import my.pkg.name.R;\n" + "\n" + "\n"
				+ "[x] Make Manifest edits - /testRefactor3/AndroidManifest.xml\n" + "  @@ -3 +3\n"
				+ "  -     package=\"com.example.refactoringtest\"\n" + "  +     package=\"my.pkg.name\"\n"
				+ "  @@ -25 +25\n" + "  -             android:name=\".MainActivity2\"\n"
				+ "  +             android:name=\"com.example.refactoringtest.MainActivity2\"");
	}

	// ---- Test infrastructure ----

	protected void renamePackage(@NonNull Object[] testData, @NonNull String newName, @NonNull String expected)
			throws Exception {
		IProject project = createProject(testData);
		renamePackage(project, newName, expected);
	}

	protected void renamePackage(@NonNull IProject project, @NonNull String newName, @NonNull String expected)
			throws Exception {
		ManifestInfo info = ManifestInfo.get(project);
		String currentPackage = info.getPackage();
		assertNotNull(currentPackage);

		final AST astValidator = AST.newAST(AST.JLS3);
		Name oldPackageName = astValidator.newName(currentPackage);
		Name newPackageName = astValidator.newName(newName);
		ApplicationPackageNameRefactoring refactoring = new ApplicationPackageNameRefactoring(project, oldPackageName,
				newPackageName);
		checkRefactoring(refactoring, expected);
	}
}

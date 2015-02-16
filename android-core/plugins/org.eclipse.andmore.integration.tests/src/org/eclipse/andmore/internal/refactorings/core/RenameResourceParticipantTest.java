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
package org.eclipse.andmore.internal.refactorings.core;

import static org.junit.Assert.*;

import com.android.annotations.NonNull;
import com.android.ide.common.resources.ResourceUrl;

import org.eclipse.andmore.internal.project.BaseProjectHelper;

import com.android.resources.ResourceType;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameFieldProcessor;
import org.eclipse.ltk.core.refactoring.participants.RenameProcessor;
import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;
import org.junit.Ignore;
import org.junit.Test;

@SuppressWarnings({ "javadoc", "restriction" })
public class RenameResourceParticipantTest extends RefactoringTestBase {
	@Test
	public void testRefactor1() throws Exception {
		renameResource(TEST_PROJECT, "@string/app_name", true /* updateReferences */, "myname",

		"CHANGES:\n" + "-------\n" + "[x] strings.xml - /testRefactor1/res/values/strings.xml\n" + "  @@ -4 +4\n"
				+ "  -     <string name=\"app_name\">RefactoringTest</string>\n"
				+ "  +     <string name=\"myname\">RefactoringTest</string>\n" + "\n" + "\n"
				+ "[ ] R.java - /testRefactor1/gen/com/example/refactoringtest/R.java\n" + "  @@ -29 +29\n"
				+ "  -         public static final int app_name=0x7f040000;\n"
				+ "  +         public static final int myname=0x7f040000;\n" + "\n" + "\n"
				+ "[x] AndroidManifest.xml - /testRefactor1/AndroidManifest.xml\n" + "  @@ -13 +13\n"
				+ "  -         android:label=\"@string/app_name\"\n" + "  +         android:label=\"@string/myname\"\n"
				+ "  @@ -17 +17\n" + "  -             android:label=\"@string/app_name\" >\n"
				+ "  +             android:label=\"@string/myname\" >");
	}

	@Test
	public void testRefactor2() throws Exception {
		renameResource(TEST_PROJECT, "@+id/menu_settings", true /* updateReferences */, "new_id_for_the_action_bar",

		"CHANGES:\n" + "-------\n" + "[x] activity_main.xml - /testRefactor2/res/menu/activity_main.xml\n"
				+ "  @@ -4 +4\n" + "  -         android:id=\"@+id/menu_settings\"\n"
				+ "  +         android:id=\"@+id/new_id_for_the_action_bar\"\n" + "\n" + "\n"
				+ "[ ] R.java - /testRefactor2/gen/com/example/refactoringtest/R.java\n" + "  @@ -19 +19\n"
				+ "  -         public static final int menu_settings=0x7f070003;\n"
				+ "  +         public static final int new_id_for_the_action_bar=0x7f070003;");
	}

	@Test
	public void testRefactor3() throws Exception {
		renameResource(TEST_PROJECT, "@+id/textView1", true /* updateReferences */, "output",

		"CHANGES:\n" + "-------\n" + "[x] activity_main.xml - /testRefactor3/res/layout/activity_main.xml\n"
				+ "  @@ -8 +8\n" + "  -         android:id=\"@+id/textView1\"\n"
				+ "  +         android:id=\"@+id/output\"\n" + "  @@ -19 +19\n"
				+ "  -         android:layout_alignLeft=\"@+id/textView1\"\n"
				+ "  -         android:layout_below=\"@+id/textView1\"\n"
				+ "  +         android:layout_alignLeft=\"@+id/output\"\n"
				+ "  +         android:layout_below=\"@+id/output\"\n" + "\n" + "\n"
				+ "[x] MainActivity.java - /testRefactor3/src/com/example/refactoringtest/MainActivity.java\n"
				+ "  @@ -14 +14\n" + "  -         View view1 = findViewById(R.id.textView1);\n"
				+ "  +         View view1 = findViewById(R.id.output);\n" + "\n" + "\n"
				+ "[ ] R.java - /testRefactor3/gen/com/example/refactoringtest/R.java\n" + "  @@ -20 +20\n"
				+ "  -         public static final int textView1=0x7f070000;\n"
				+ "  +         public static final int output=0x7f070000;");
	}

	@Test
	public void testRefactor4() throws Exception {
		renameResource(TEST_PROJECT,
		// same as testRefactor3, but use @id rather than @+id even though @+id
		// is in file
				"@id/textView1", true /* updateReferences */, "output",

				"CHANGES:\n" + "-------\n" + "[x] activity_main.xml - /testRefactor4/res/layout/activity_main.xml\n"
						+ "  @@ -8 +8\n" + "  -         android:id=\"@+id/textView1\"\n"
						+ "  +         android:id=\"@+id/output\"\n" + "  @@ -19 +19\n"
						+ "  -         android:layout_alignLeft=\"@+id/textView1\"\n"
						+ "  -         android:layout_below=\"@+id/textView1\"\n"
						+ "  +         android:layout_alignLeft=\"@+id/output\"\n"
						+ "  +         android:layout_below=\"@+id/output\"\n" + "\n" + "\n"
						+ "[x] MainActivity.java - /testRefactor4/src/com/example/refactoringtest/MainActivity.java\n"
						+ "  @@ -14 +14\n" + "  -         View view1 = findViewById(R.id.textView1);\n"
						+ "  +         View view1 = findViewById(R.id.output);\n" + "\n" + "\n"
						+ "[ ] R.java - /testRefactor4/gen/com/example/refactoringtest/R.java\n" + "  @@ -20 +20\n"
						+ "  -         public static final int textView1=0x7f070000;\n"
						+ "  +         public static final int output=0x7f070000;");
	}

	@Test
	public void testRefactor5() throws Exception {
		renameResource(TEST_PROJECT, "@layout/activity_main", true /* updateReferences */, "newlayout",

		"CHANGES:\n" + "-------\n"
				+ "[x] MainActivity.java - /testRefactor5/src/com/example/refactoringtest/MainActivity.java\n"
				+ "  @@ -13 +13\n" + "  -         setContentView(R.layout.activity_main);\n"
				+ "  +         setContentView(R.layout.newlayout);\n" + "\n" + "\n"
				+ "[ ] R.java - /testRefactor5/gen/com/example/refactoringtest/R.java\n" + "  @@ -23 +23\n"
				+ "  -         public static final int activity_main=0x7f030000;\n"
				+ "  +         public static final int newlayout=0x7f030000;\n" + "\n" + "\n"
				+ "[x] Rename 'testRefactor5/res/layout/activity_main.xml' to 'newlayout.xml'\n" + "\n"
				+ "[x] Rename 'testRefactor5/res/layout-land/activity_main.xml' to 'newlayout.xml'");
	}

	@Test
	public void testRefactor6() throws Exception {
		renameResource(TEST_PROJECT, "@drawable/ic_launcher", true /* updateReferences */, "newlauncher",

		"CHANGES:\n" + "-------\n" + "[ ] R.java - /testRefactor6/gen/com/example/refactoringtest/R.java\n"
				+ "  @@ -14 +14\n" + "  -         public static final int ic_launcher=0x7f020000;\n"
				+ "  +         public static final int newlauncher=0x7f020000;\n" + "\n" + "\n"
				+ "[x] Rename 'testRefactor6/res/drawable-xhdpi/ic_launcher.png' to 'newlauncher.png'\n" + "\n"
				+ "[x] Rename 'testRefactor6/res/drawable-mdpi/ic_launcher.png' to 'newlauncher.png'\n" + "\n"
				+ "[x] Rename 'testRefactor6/res/drawable-ldpi/ic_launcher.png' to 'newlauncher.png'\n" + "\n"
				+ "[x] Rename 'testRefactor6/res/drawable-hdpi/ic_launcher.png' to 'newlauncher.png'\n" + "\n"
				+ "[x] AndroidManifest.xml - /testRefactor6/AndroidManifest.xml\n" + "  @@ -12 +12\n"
				+ "  -         android:icon=\"@drawable/ic_launcher\"\n"
				+ "  +         android:icon=\"@drawable/newlauncher\"");
	}

	@Test
	public void testRefactor7() throws Exception {
		// Test refactoring initiated on a file rename
		IProject project = createProject(TEST_PROJECT);
		IFile file = project.getFile("res/layout/activity_main.xml");
		renameResource(project, file, true /* updateReferences */, "newlayout",

		"CHANGES:\n" + "-------\n"
				+ "[x] MainActivity.java - /testRefactor7/src/com/example/refactoringtest/MainActivity.java\n"
				+ "  @@ -13 +13\n" + "  -         setContentView(R.layout.activity_main);\n"
				+ "  +         setContentView(R.layout.newlayout);\n" + "\n" + "\n"
				+ "[ ] R.java - /testRefactor7/gen/com/example/refactoringtest/R.java\n" + "  @@ -23 +23\n"
				+ "  -         public static final int activity_main=0x7f030000;\n"
				+ "  +         public static final int newlayout=0x7f030000;\n" + "\n" + "\n"
				+ "[x] Rename 'testRefactor7/res/layout-land/activity_main.xml' to 'newlayout.xml'\n" + "\n"
				+ "[x] Rename 'testRefactor7/res/layout/activity_main.xml' to 'newlayout.xml'", null);
	}

	@Test
	public void testRefactor8() throws Exception {
		// Test refactoring initiated on a Java field rename
		IProject project = createProject(TEST_PROJECT);
		IJavaProject javaProject = BaseProjectHelper.getJavaProject(project);
		assertNotNull(javaProject);
		IType type = javaProject.findType("com.example.refactoringtest.R.layout");
		if (type == null || !type.exists()) {
			type = javaProject.findType("com.example.refactoringtest.R$layout");
			System.out.println("Had to switch to $ notation");
		}
		assertNotNull(type);
		assertTrue(type.exists());
		IField field = type.getField("activity_main");
		assertNotNull(field);
		assertTrue(field.exists());

		renameResource(project, field, true /* updateReferences */, "newlauncher",

		"CHANGES:\n" + "-------\n" + "[x] Rename 'testRefactor8/res/layout/activity_main.xml' to 'newlauncher.xml'\n"
				+ "\n" + "[x] Rename 'testRefactor8/res/layout-land/activity_main.xml' to 'newlauncher.xml'\n" + "\n"
				+ "[x] MainActivity.java - /testRefactor8/src/com/example/refactoringtest/MainActivity.java\n"
				+ "  @@ -13 +13\n" + "  -         setContentView(R.layout.activity_main);\n"
				+ "  +         setContentView(R.layout.newlauncher);\n" + "\n" + "\n"
				+ "[ ] R.java - /testRefactor8/gen/com/example/refactoringtest/R.java\n" + "  @@ -23 +23\n"
				+ "  -         public static final int activity_main=0x7f030000;\n"
				+ "  +         public static final int newlauncher=0x7f030000;", null);
	}

	@Test
	public void testInvalidName() throws Exception {
		renameResource(TEST_PROJECT, "@drawable/ic_launcher", true /* updateReferences */, "Newlauncher",

		"", "<ERROR\n" + "\t\n" + "ERROR: File-based resource names must start with a lowercase letter.\n"
				+ "Context: <Unspecified context>\n" + "code: none\n" + "Data: null\n" + ">");
	}

	@Test
	public void testRefactor9() throws Exception {
		// same as testRefactor4, but not updating references
		renameResource(TEST_PROJECT, "@id/textView1", false /* updateReferences */, "output",

		"CHANGES:\n" + "-------\n" + "[x] activity_main.xml - /testRefactor9/res/layout/activity_main.xml\n"
				+ "  @@ -8 +8\n" + "  -         android:id=\"@+id/textView1\"\n"
				+ "  +         android:id=\"@+id/output\"\n" + "\n" + "\n"
				+ "[ ] R.java - /testRefactor9/gen/com/example/refactoringtest/R.java\n" + "  @@ -20 +20\n"
				+ "  -         public static final int textView1=0x7f070000;\n"
				+ "  +         public static final int output=0x7f070000;");
	}

	@Test
	public void testRefactor10() throws Exception {
		// Check updating tools: attributes
		renameResource(TEST_PROJECT, "@layout/preview", true /* updateReferences */, "newlayout",

		"CHANGES:\n" + "-------\n" + "[x] activity_main.xml - /testRefactor10/res/layout-land/activity_main.xml\n"
				+ "  @@ -10 +10\n" + "  -         tools:listitem=\"@layout/preview\" >\n"
				+ "  +         tools:listitem=\"@layout/newlayout\" >\n" + "  @@ -17 +17\n"
				+ "  -         tools:layout=\"@layout/preview\" />\n"
				+ "  +         tools:layout=\"@layout/newlayout\" />");
	}

	// ---- Test infrastructure ----

	protected void renameResource(@NonNull Object[] testData, @NonNull Object resource, boolean updateReferences,
			@NonNull String newName, @NonNull String expected) throws Exception {
		renameResource(testData, resource, updateReferences, newName, expected, null);
	}

	protected void renameResource(@NonNull Object[] testData, @NonNull Object resource, boolean updateReferences,
			@NonNull String newName, @NonNull String expected, @NonNull String expectedWarnings) throws Exception {
		IProject project = createProject(testData);
		renameResource(project, resource, updateReferences, newName, expected, expectedWarnings);
	}

	protected void renameResource(@NonNull IProject project, @NonNull Object resource, boolean updateReferences,
			@NonNull String newName, @NonNull String expected, @NonNull String expectedWarnings) throws Exception {
		RenameProcessor processor = null;
		if (resource instanceof String) {
			String url = (String) resource;
			assert url.startsWith("@") : resource;

			ResourceUrl parsedUrl = ResourceUrl.parse(url);
			assertNotNull(url, parsedUrl);
			assert parsedUrl != null; // null analysis doesn't recognize
										// assertNotNull
			ResourceType type = parsedUrl.type;
			String currentName = parsedUrl.name;
			RenameResourceProcessor p;
			p = new RenameResourceProcessor(project, type, currentName, newName);
			p.setUpdateReferences(updateReferences);
			processor = p;
		} else if (resource instanceof IResource) {
			IResource r = (IResource) resource;
			org.eclipse.ltk.internal.core.refactoring.resource.RenameResourceProcessor p;
			p = new org.eclipse.ltk.internal.core.refactoring.resource.RenameResourceProcessor(r);
			String fileName = r.getName();
			int dot = fileName.indexOf('.');
			String extension = (dot != -1) ? fileName.substring(dot) : "";
			p.setNewResourceName(newName + extension);
			p.setUpdateReferences(updateReferences);
			processor = p;
		} else if (resource instanceof IField) {
			RenameFieldProcessor p = new RenameFieldProcessor((IField) resource);
			p.setNewElementName(newName);
			p.setUpdateReferences(updateReferences);
			processor = p;
		} else {
			fail("Unsupported resource element in tests: " + resource);
		}

		assertNotNull(processor);

		RenameRefactoring refactoring = new RenameRefactoring(processor);
		checkRefactoring(refactoring, expected, expectedWarnings);
	}
}
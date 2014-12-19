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

import com.android.annotations.NonNull;

import org.eclipse.andmore.internal.project.BaseProjectHelper;
import org.eclipse.andmore.internal.refactorings.core.RefactoringTestBase;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameTypeProcessor;
import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;


@SuppressWarnings({"javadoc", "restriction"})
public class AndroidTypeRenameParticipantTest extends RefactoringTestBase {
    public void testRefactor1() throws Exception {
        renameType(
                TEST_PROJECT,
                "com.example.refactoringtest.MainActivity",
                true /*updateReferences*/,
                "NewActivityName",

                "CHANGES:\n" +
                "-------\n" +
                "[x] Rename compilation unit 'MainActivity.java' to 'NewActivityName.java'\n" +
                "\n" +
                "[x] activity_main.xml - /testRefactor1/res/layout/activity_main.xml\n" +
                "  @@ -5 +5\n" +
                "  -     tools:context=\".MainActivity\" >\n" +
                "  +     tools:context=\".NewActivityName\" >\n" +
                "\n" +
                "\n" +
                "[x] activity_main.xml - /testRefactor1/res/layout-land/activity_main.xml\n" +
                "  @@ -5 +5\n" +
                "  -     tools:context=\".MainActivity\" >\n" +
                "  +     tools:context=\".NewActivityName\" >\n" +
                "\n" +
                "\n" +
                "[x] AndroidManifest.xml - /testRefactor1/AndroidManifest.xml\n" +
                "  @@ -16 +16\n" +
                "  -             android:name=\"com.example.refactoringtest.MainActivity\"\n" +
                "  +             android:name=\"com.example.refactoringtest.NewActivityName\"");
    }

    public void testRefactor1b() throws Exception {
        renameType(
                TEST_PROJECT,
                "com.example.refactoringtest.MainActivity2",
                true /*updateReferences*/,
                "NewActivityName",

                "CHANGES:\n" +
                "-------\n" +
                "[x] Rename compilation unit 'MainActivity2.java' to 'NewActivityName.java'\n" +
                "\n" +
                "[x] AndroidManifest.xml - /testRefactor1b/AndroidManifest.xml\n" +
                "  @@ -25 +25\n" +
                "  -             android:name=\".MainActivity2\"\n" +
                "  +             android:name=\".NewActivityName\"");
    }

    public void testRefactor1_noreferences() throws Exception {
        renameType(
                TEST_PROJECT,
                "com.example.refactoringtest.MainActivity",
                false /*updateReferences*/,
                "NewActivityName",

                "CHANGES:\n" +
                "-------\n" +
                "[x] Rename compilation unit 'MainActivity.java' to 'NewActivityName.java'");
    }

    public void testRefactor2() throws Exception {
        renameType(
                TEST_PROJECT2,
                "com.example.refactoringtest.CustomView1",
                true /*updateReferences*/,
                "NewCustomViewName",

                "CHANGES:\n" +
                "-------\n" +
                "[x] Rename compilation unit 'CustomView1.java' to 'NewCustomViewName.java'\n" +
                "\n" +
                "[x] attrs_custom_view.xml - /testRefactor2/res/values/attrs_custom_view.xml\n" +
                "  @@ -4 +4\n" +
                "  -     <declare-styleable name=\"CustomView1\">\n" +
                "  +     <declare-styleable name=\"NewCustomViewName\">\n" +
                "\n" +
                "\n" +
                "[x] customviews.xml - /testRefactor2/res/layout/customviews.xml\n" +
                "  @@ -9 +9\n" +
                "  -     <com.example.refactoringtest.CustomView1\n" +
                "  +     <com.example.refactoringtest.NewCustomViewName\n" +
                "\n" +
                "\n" +
                "[x] customviews.xml - /testRefactor2/res/layout-land/customviews.xml\n" +
                "  @@ -9 +9\n" +
                "  -     <com.example.refactoringtest.CustomView1\n" +
                "  +     <com.example.refactoringtest.NewCustomViewName");
    }

    public void testRefactorFragment() throws Exception {
        renameType(
            TEST_PROJECT2,
            "com.example.refactoringtest.MyFragment",
            true /*updateReferences*/,
            "NewFragmentName",

            "CHANGES:\n" +
            "-------\n" +
            "[x] Rename compilation unit 'MyFragment.java' to 'NewFragmentName.java'\n" +
            "\n" +
            "[x] activity_main.xml - /testRefactorFragment/res/layout/activity_main.xml\n" +
            "  @@ -33 +33\n" +
            "  -     <fragment android:name=\"com.example.refactoringtest.MyFragment\"/>\n" +
            "  +     <fragment android:name=\"com.example.refactoringtest.NewFragmentName\"/>");
    }

    // ---- Test infrastructure ----

    protected void renameType(
            @NonNull Object[] testData,
            @NonNull String typeFqcn,
            boolean updateReferences,
            @NonNull String newName,
            @NonNull String expected) throws Exception {
        IProject project = createProject(testData);
        IJavaProject javaProject = BaseProjectHelper.getJavaProject(project);
        assertNotNull(javaProject);
        IType type = javaProject.findType(typeFqcn);
        assertNotNull(typeFqcn, type);
        assertTrue(typeFqcn, type.exists());
        RenameTypeProcessor processor = new RenameTypeProcessor(type);
        processor.setNewElementName(newName);
        processor.setUpdateQualifiedNames(true);
        processor.setUpdateSimilarDeclarations(false);
        //processor.setMatchStrategy(?);
        //processor.setFilePatterns(patterns);
        processor.setUpdateReferences(updateReferences);
        assertNotNull(processor);

        RenameRefactoring refactoring = new RenameRefactoring(processor);
        checkRefactoring(refactoring, expected);
    }
}

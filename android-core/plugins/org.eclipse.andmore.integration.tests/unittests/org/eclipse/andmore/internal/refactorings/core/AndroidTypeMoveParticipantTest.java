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

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.project.BaseProjectHelper;
import org.eclipse.andmore.internal.refactorings.core.RefactoringTestBase;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.internal.corext.refactoring.reorg.IReorgPolicy.IMovePolicy;
import org.eclipse.jdt.internal.corext.refactoring.reorg.JavaMoveProcessor;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgDestinationFactory;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgPolicyFactory;
import org.eclipse.jdt.internal.ui.refactoring.reorg.CreateTargetQueries;
import org.eclipse.jdt.internal.ui.refactoring.reorg.ReorgQueries;
import org.eclipse.ltk.core.refactoring.participants.MoveRefactoring;
import org.eclipse.swt.widgets.Shell;


@SuppressWarnings({"javadoc", "restriction"})
public class AndroidTypeMoveParticipantTest extends RefactoringTestBase {
    public void testRefactor1() throws Exception {
        moveType(
                TEST_PROJECT2,
                "com.example.refactoringtest.CustomView1",
                "src/com/example/refactoringtest/subpackage",
                true /*updateReferences*/,

                "CHANGES:\n" +
                "-------\n" +
                "[x] Move resource 'testRefactor1/src/com/example/refactoringtest/CustomView1.java' to 'subpackage'\n" +
                "\n" +
                "[x] Move resource 'testRefactor1/src/com/example/refactoringtest/CustomView1.java' to 'subpackage'\n" +
                "\n" +
                "[x] customviews.xml - /testRefactor1/res/layout/customviews.xml\n" +
                "  @@ -9 +9\n" +
                "  -     <com.example.refactoringtest.CustomView1\n" +
                "  +     <com.example.refactoringtest.subpackage.CustomView1\n" +
                "\n" +
                "\n" +
                "[x] customviews.xml - /testRefactor1/res/layout-land/customviews.xml\n" +
                "  @@ -9 +9\n" +
                "  -     <com.example.refactoringtest.CustomView1\n" +
                "  +     <com.example.refactoringtest.subpackage.CustomView1");
    }

    public void testRefactorFragment() throws Exception {
        moveType(
                TEST_PROJECT2,
                "com.example.refactoringtest.MyFragment",
                "src/com/example/refactoringtest/subpackage",
                true /*updateReferences*/,

                "CHANGES:\n" +
                "-------\n" +
                "[x] Move resource 'testRefactorFragment/src/com/example/refactoringtest/MyFragment.java' to 'subpackage'\n" +
                "\n" +
                "[x] Move resource 'testRefactorFragment/src/com/example/refactoringtest/MyFragment.java' to 'subpackage'\n" +
                "\n" +
                "[x] activity_main.xml - /testRefactorFragment/res/layout/activity_main.xml\n" +
                "  @@ -33 +33\n" +
                "  -     <fragment android:name=\"com.example.refactoringtest.MyFragment\"/>\n" +
                "  +     <fragment android:name=\"com.example.refactoringtest.subpackage.MyFragment\"/>");
    }

    public void testRefactor1_norefs() throws Exception {
        moveType(
                TEST_PROJECT2,
                "com.example.refactoringtest.CustomView1",
                "src/com/example/refactoringtest/subpackage",
                false /*updateReferences*/,

                "CHANGES:\n" +
                "-------\n" +
                "[x] Move resource 'testRefactor1_norefs/src/com/example/refactoringtest/CustomView1.java' to 'subpackage'\n" +
                "\n" +
                "[x] Move resource 'testRefactor1_norefs/src/com/example/refactoringtest/CustomView1.java' to 'subpackage'");
    }

    // ---- Test infrastructure ----

    protected void moveType(
            @NonNull Object[] testData,
            @NonNull String typeFqcn,
            @NonNull String destination,
            boolean updateReferences,
            @NonNull String expected) throws Exception {
        IProject project = createProject(testData);

        IFolder destinationFolder = project.getFolder(destination);

        IJavaProject javaProject = BaseProjectHelper.getJavaProject(project);
        assertNotNull(javaProject);
        IType type = javaProject.findType(typeFqcn);
        assertNotNull(typeFqcn, type);
        assertTrue(typeFqcn, type.exists());
        IResource resource = type.getResource();
        assertNotNull(typeFqcn, resource);
        assertTrue(typeFqcn, resource.exists());

        IResource[] resources = new IResource[] { resource };
        IJavaElement[] elements = new IJavaElement[] { type };
        IMovePolicy policy = ReorgPolicyFactory.createMovePolicy(resources, elements);
        JavaMoveProcessor processor = new JavaMoveProcessor(policy);
        processor.setUpdateReferences(updateReferences);
        processor.setUpdateQualifiedNames(true);
        assertTrue(policy.canEnable());
        processor.setDestination(ReorgDestinationFactory.createDestination(destinationFolder));
        Shell parent = AdtPlugin.getShell();
        assertNotNull(parent);
        processor.setCreateTargetQueries(new CreateTargetQueries(parent));
        processor.setReorgQueries(new ReorgQueries(parent));

        MoveRefactoring refactoring = new MoveRefactoring(processor);
        checkRefactoring(refactoring, expected);
    }
}

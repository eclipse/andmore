/*
 * Copyright (C) 2010 The Android Open Source Project
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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.search.TypeNameMatch;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.OrganizeImportsOperation;
import org.eclipse.jdt.internal.corext.codemanipulation.OrganizeImportsOperation.IChooseImportQuery;
import org.eclipse.jdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.ui.SharedASTProvider;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;

/**
 * The helper class which fixes the import errors after refactoring
 *
 */
@SuppressWarnings("restriction")
public class FixImportsJob extends WorkspaceJob {

    private IFile mAndroidManifest;

    private String mJavaPackage;

    /**
     * Creates a new <code>FixImportsJob</code>
     *
     * @param name the job name
     * @param androidManifest the android manifest file
     * @param javaPackage the android java package
     */
    public FixImportsJob(String name, IFile androidManifest, String javaPackage) {
        super(name);
        this.mAndroidManifest = androidManifest;
        this.mJavaPackage = javaPackage;
    }

    @Override
    public IStatus runInWorkspace(final IProgressMonitor monitor) throws CoreException {
        if (mJavaPackage == null || mAndroidManifest == null || !mAndroidManifest.exists()) {
            return Status.CANCEL_STATUS;
        }
        IProject project = mAndroidManifest.getProject();
        IJavaProject javaProject = JavaCore.create(project);
        if (javaProject == null || !javaProject.isOpen()) {
            return Status.CANCEL_STATUS;
        }

        project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);

        IMarker[] markers = project.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
        for (int i = 0; i < markers.length; i++) {
            IMarker marker = markers[i];
            IResource resource = marker.getResource();
            try {
                IJavaElement element = JavaCore.create(resource);
                if (element != null && (element instanceof ICompilationUnit)) {
                    final ICompilationUnit cu = (ICompilationUnit) element;
                    IPackageFragment packageFragment = (IPackageFragment) cu
                            .getAncestor(IJavaElement.PACKAGE_FRAGMENT);
                    if (packageFragment != null && packageFragment.exists()) {
                        String packageName = packageFragment.getElementName();
                        if (packageName != null && packageName.startsWith(mJavaPackage)) {
                            CompilationUnit astRoot = SharedASTProvider.getAST(cu,
                                    SharedASTProvider.WAIT_ACTIVE_ONLY, null);
                            CodeGenerationSettings settings = JavaPreferencesSettings
                                    .getCodeGenerationSettings(cu.getJavaProject());
                            final boolean hasAmbiguity[] = new boolean[] {
                                false
                            };
                            IChooseImportQuery query = new IChooseImportQuery() {
                                @Override
                                public TypeNameMatch[] chooseImports(TypeNameMatch[][] openChoices,
                                        ISourceRange[] ranges) {
                                    hasAmbiguity[0] = true;
                                    return new TypeNameMatch[0];
                                }
                            };
                            final OrganizeImportsOperation op = new OrganizeImportsOperation(cu,
                                    astRoot, settings.importIgnoreLowercase, !cu.isWorkingCopy(),
                                    true, query);
                            Display.getDefault().asyncExec(new Runnable() {

                                @Override
                                public void run() {
                                    try {
                                        IProgressService progressService = PlatformUI
                                                .getWorkbench().getProgressService();
                                        progressService.run(
                                                true,
                                                true,
                                                new WorkbenchRunnableAdapter(op, op
                                                        .getScheduleRule()));
                                        IEditorPart openEditor = EditorUtility.isOpenInEditor(cu);
                                        if (openEditor != null) {
                                            openEditor.doSave(monitor);
                                        }
                                    } catch (Throwable e) {
                                        RefactoringUtil.log(e);
                                    }
                                }
                            });

                        }
                    }
                }
            } catch (Throwable e) {
                RefactoringUtil.log(e);
            }
        }
        return Status.OK_STATUS;
    }
}
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

package org.eclipse.andmore.internal.refactorings.renamepackage;

import com.android.ide.common.xml.ManifestData;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.project.AndroidManifestHelper;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.ui.refactoring.RefactoringSaveHelper;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import java.util.Iterator;

/**
 * Refactoring steps:
 * <ol>
 * <li>Update the "package" attribute of the &lt;manifest&gt; tag with the new
 * name.</li>
 * <li>Replace all values for the "android:name" attribute in the
 * &lt;application&gt; and "component class" (&lt;activity&gt;, &lt;service&gt;,
 * &lt;receiver&gt;, and &lt;provider&gt;) tags with the non-shorthand version
 * of the class name</li>
 * <li>Replace package resource imports (*.R) in .java files</li>
 * <li>Update package name in the namespace declarations (e.g. "xmlns:app")
 * used for custom styleable attributes in layout resource files</li>
 * </ol>
 * Caveat: Sometimes it is necessary to perform a project-wide
 * "Organize Imports" afterwards. (CTRL+SHIFT+O when a project has active
 * selection)
 */
public class RenamePackageAction implements IObjectActionDelegate {

    private ISelection mSelection;
    @SuppressWarnings("unused") private IWorkbenchPart mTargetPart; // TODO cleanup

    /**
     * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
     */
    @Override
    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
        mTargetPart = targetPart;
    }

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        mSelection = selection;
    }

    /**
     * @see IWorkbenchWindowActionDelegate#init
     */
    public void init(IWorkbenchWindow window) {
        // pass
    }

    @Override
    public void run(IAction action) {

        // Prompt for refactoring on the selected project
        if (mSelection instanceof IStructuredSelection) {
            for (Iterator<?> it = ((IStructuredSelection) mSelection).iterator(); it.hasNext();) {
                Object element = it.next();
                IProject project = null;
                if (element instanceof IProject) {
                    project = (IProject) element;
                } else if (element instanceof IAdaptable) {
                    project = (IProject) ((IAdaptable) element).getAdapter(IProject.class);
                }
                if (project != null) {
                    // It is advisable that the user saves before proceeding,
                    // revealing any compilation errors. The following lines
                    // enforce a save as a convenience.
                    RefactoringSaveHelper save_helper = new RefactoringSaveHelper(
                            RefactoringSaveHelper.SAVE_ALL_ALWAYS_ASK);
                    if (save_helper.saveEditors(AdtPlugin.getShell())) {
                        promptNewName(project);
                    }
                }
            }
        }
    }

    /*
     * Validate the new package name and start the refactoring wizard
     */
    private void promptNewName(final IProject project) {

        ManifestData manifestData = AndroidManifestHelper.parseForData(project);
        if (manifestData == null) {
            return;
        }

        final String oldPackageNameString = manifestData.getPackage();

        final AST astValidator = AST.newAST(AST.JLS3);
        Name oldPackageName = astValidator.newName(oldPackageNameString);

        IInputValidator validator = new IInputValidator() {

            @Override
            public String isValid(String newText) {
                try {
                    astValidator.newName(newText);
                } catch (IllegalArgumentException e) {
                    return "Illegal package name.";
                }

                if (newText.equals(oldPackageNameString))
                    return "No change.";
                else
                    return null;
            }
        };

        InputDialog dialog = new InputDialog(AdtPlugin.getShell(),
                "Rename Application Package", "Enter new package name:", oldPackageNameString,
                validator);

        if (dialog.open() == Window.OK) {
            Name newPackageName = astValidator.newName(dialog.getValue());
            initiateAndroidPackageRefactoring(project, oldPackageName, newPackageName);
        }
    }


    private void initiateAndroidPackageRefactoring(
            final IProject project,
            Name oldPackageName,
            Name newPackageName) {

        Refactoring package_name_refactoring =
            new ApplicationPackageNameRefactoring(project, oldPackageName, newPackageName);

        ApplicationPackageNameRefactoringWizard wizard =
            new ApplicationPackageNameRefactoringWizard(package_name_refactoring);
        RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation(wizard);
        try {
            op.run(AdtPlugin.getShell(), package_name_refactoring.getName());
        } catch (InterruptedException e) {
            Status s = new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID, e.getMessage(), e);
            AdtPlugin.getDefault().getLog().log(s);
        }
    }
}

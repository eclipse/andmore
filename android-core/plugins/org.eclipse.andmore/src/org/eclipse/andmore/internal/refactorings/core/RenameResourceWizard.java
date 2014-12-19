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
import com.android.annotations.Nullable;
import com.android.resources.ResourceType;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.refactoring.reorg.RenameRefactoringWizard;
import org.eclipse.jdt.ui.refactoring.RefactoringSaveHelper;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.widgets.Shell;

/**
 * Rename refactoring wizard for Android resources such as {@code @id/foo}
 */
@SuppressWarnings("restriction") // JDT refactoring UI
public class RenameResourceWizard extends RenameRefactoringWizard {
	private ResourceType mType;
    private boolean mCanClear;

    /**
     * Constructs a new {@linkplain RenameResourceWizard}
     *
     * @param refactoring the refactoring
     * @param type the type of resource being renamed
     * @param canClear whether the user can clear the value
     */
    public RenameResourceWizard(
            @NonNull RenameRefactoring refactoring,
            @NonNull ResourceType type,
            boolean canClear) {
        super(refactoring,
                "Rename Resource",
                "Enter the new name for this resource",
                JavaPluginImages.DESC_WIZBAN_REFACTOR_FIELD,
                IJavaHelpContextIds.RENAME_FIELD_WIZARD_PAGE);
        mType = type;
		mCanClear = canClear;
	}

	@Override
	protected void addUserInputPages() {
	    RenameRefactoring refactoring = (RenameRefactoring) getRefactoring();
        RenameResourceProcessor processor = (RenameResourceProcessor) refactoring.getProcessor();
	    String name = processor.getNewName();
        addPage(new RenameResourcePage(mType, name, mCanClear));
	}

    /**
     * Initiates a renaming of a resource item
     *
     * @param shell the shell to parent the dialog to
     * @param project the project containing the resource references
     * @param type the type of resource
     * @param currentName the name of the resource
     * @param newName the new name, or null if not known
     * @param canClear whether the name is allowed to be cleared
     * @return false if initiating the rename failed
     */
    public static RenameResult renameResource(
            @NonNull Shell shell,
            @NonNull IProject project,
            @NonNull ResourceType type,
            @NonNull String currentName,
            @Nullable String newName,
            boolean canClear) {
        try {
            RenameResourceProcessor processor = new RenameResourceProcessor(project, type,
                    currentName, newName);
            RenameRefactoring refactoring = new RenameRefactoring(processor);
            if (!refactoring.isApplicable()) {
                return RenameResult.unavailable();
            }

            if (!show(refactoring, processor, shell, type, canClear)) {
                return RenameResult.canceled();
            }
            return RenameResult.name(processor.getNewName());
        } catch (CoreException e) {
            AdtPlugin.log(e, null);
        }

        return RenameResult.unavailable();
    }

    /**
     * Show a refactoring dialog for the given resource refactoring operation
     *
     * @param refactoring the rename refactoring
     * @param processor the field processor
     * @param parent the parent shell
     * @param type the resource type
     * @param canClear whether the user is allowed to clear/reset the name to
     *            nothing
     * @return true if the refactoring was performed, and false if it was
     *         canceled
     * @throws CoreException if an unexpected error occurs
     */
    private static boolean show(
            @NonNull RenameRefactoring refactoring,
            @NonNull RenameResourceProcessor processor,
            @NonNull Shell parent,
            @NonNull ResourceType type,
            boolean canClear) throws CoreException {
        RefactoringSaveHelper saveHelper = new RefactoringSaveHelper(
                RefactoringSaveHelper.SAVE_REFACTORING);
        if (!saveHelper.saveEditors(parent)) {
            return false;
        }

        try {
            RenameResourceWizard wizard = new RenameResourceWizard(refactoring, type, canClear);
            RefactoringWizardOpenOperation operation = new RefactoringWizardOpenOperation(wizard);
            String dialogTitle = wizard.getDefaultPageTitle();
            int result = operation.run(parent, dialogTitle == null ? "" : dialogTitle);
            RefactoringStatus status = operation.getInitialConditionCheckingStatus();
            if (status.hasFatalError()) {
                return false;
            }
            if (result == RefactoringWizardOpenOperation.INITIAL_CONDITION_CHECKING_FAILED
                    || result == IDialogConstants.CANCEL_ID) {
                saveHelper.triggerIncrementalBuild();
                return false;
            }

            // Save modified resources; need to trigger R file regeneration
            saveHelper.saveEditors(parent);

            return true;
        } catch (InterruptedException e) {
            return false; // Canceled
        }
    }
}

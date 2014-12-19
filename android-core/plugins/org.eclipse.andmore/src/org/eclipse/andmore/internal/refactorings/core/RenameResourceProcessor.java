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

import static com.android.SdkConstants.PREFIX_RESOURCE_REF;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.resources.ResourceType;

import org.eclipse.andmore.AdtConstants;
import org.eclipse.andmore.internal.resources.ResourceNameValidator;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.ParticipantManager;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.RenameArguments;
import org.eclipse.ltk.core.refactoring.participants.RenameProcessor;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;

/**
 * A rename processor for Android resources.
 */
public class RenameResourceProcessor extends RenameProcessor {
    private IProject mProject;
    private ResourceType mType;
    private String mCurrentName;
    private String mNewName;
    private boolean mUpdateReferences = true;
    private ResourceNameValidator mValidator;
    private RenameArguments mRenameArguments;

    /**
     * Creates a new rename resource processor.
     *
     * @param project the project containing the renamed resource
     * @param type the type of the resource
     * @param currentName the current name of the resource
     * @param newName the new name of the resource, or null if not known
     */
    public RenameResourceProcessor(
            @NonNull IProject project,
            @NonNull ResourceType type,
            @NonNull String currentName,
            @Nullable String newName) {
        mProject = project;
        mType = type;
        mCurrentName = currentName;
        mNewName = newName != null ? newName : currentName;
        mUpdateReferences= true;
        mValidator = ResourceNameValidator.create(false, mProject, mType);
    }

    /**
     * Returns the project containing the renamed resource
     *
     * @return the project containing the renamed resource
     */
    @NonNull
    public IProject getProject() {
        return mProject;
    }

    /**
     * Returns the new resource name
     *
     * @return the new resource name
     */
    @NonNull
    public String getNewName() {
        return mNewName;
    }

    /**
     * Returns the current name of the resource
     *
     * @return the current name of the resource
     */
    public String getCurrentName() {
        return mCurrentName;
    }

    /**
     * Returns the type of the resource
     *
     * @return the type of the resource
     */
    @NonNull
    public ResourceType getType() {
        return mType;
    }

    /**
     * Sets the new name
     *
     * @param newName the new name
     */
    public void setNewName(@NonNull String newName) {
        mNewName = newName;
    }

    /**
     * Returns {@code true} if the refactoring processor also updates references
     *
     * @return {@code true} if the refactoring processor also updates references
     */
    public boolean isUpdateReferences() {
        return mUpdateReferences;
    }

    /**
     * Specifies if the refactoring processor also updates references. The
     * default behavior is to update references.
     *
     * @param updateReferences {@code true} if the refactoring processor should
     *            also updates references
     */
    public void setUpdateReferences(boolean updateReferences) {
        mUpdateReferences = updateReferences;
    }

    /**
     * Checks the given new potential name and returns a {@link RefactoringStatus} indicating
     * whether the potential new name is valid
     *
     * @param name the name to check
     * @return a {@link RefactoringStatus} with the validation result
     */
    public RefactoringStatus checkNewName(String name) {
        String error = mValidator.isValid(name);
        if (error != null) {
            return RefactoringStatus.createFatalErrorStatus(error);
        }

        return new RefactoringStatus();
    }

    @Override
    public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
        return new RefactoringStatus();
    }

    @Override
    public RefactoringStatus checkFinalConditions(IProgressMonitor pm,
            CheckConditionsContext context) throws CoreException {
        pm.beginTask("", 1);
        try {
            mRenameArguments = new RenameArguments(getNewName(), isUpdateReferences());
            return new RefactoringStatus();
        } finally {
            pm.done();
        }
    }

    @Override
    public Change createChange(IProgressMonitor pm) throws CoreException {
        pm.beginTask("", 1);
        try {
            // Added by {@link RenameResourceParticipant}
            return null;
        } finally {
            pm.done();
        }
    }

    @Override
    public Object[] getElements() {
        return new Object[0];
    }

    @Override
    public String getIdentifier() {
        return "com.android.ide.renameResourceProcessor"; //$NON-NLS-1$
    }

    @Override
    public String getProcessorName() {
        return "Rename Android Resource";
    }

    @Override
    public boolean isApplicable() {
        return true;
    }

    @Override
    public RefactoringParticipant[] loadParticipants(RefactoringStatus status,
            SharableParticipants shared) throws CoreException {
        String[] affectedNatures = new String[] { AdtConstants.NATURE_DEFAULT };
        String url = PREFIX_RESOURCE_REF + mType.getName() + '/' + mCurrentName;
        return ParticipantManager.loadRenameParticipants(status, this, url, mRenameArguments,
                null, affectedNatures, shared);
    }
}
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
package org.eclipse.andmore.internal.resources;

import com.android.annotations.Nullable;

import org.eclipse.andmore.internal.editors.layout.gle2.IncludeFinder;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.IInputValidator;

import java.util.Collection;

/** A validator which checks for cyclic dependencies */
public class CyclicDependencyValidator implements IInputValidator {
    private final Collection<String> mInvalidIds;

    private CyclicDependencyValidator(Collection<String> invalid) {
        this.mInvalidIds = invalid;
    }

    @Override
    public String isValid(String newText) {
        if (mInvalidIds.contains(newText)) {
            return "Cyclic include, not valid";
        }
        return null;
    }

    /**
     * Creates a validator which ensures that the chosen id is not for a layout that is
     * directly or indirectly including the given layout. Used to avoid cyclic
     * dependencies when offering layouts to be included within a given file, etc.
     *
     * @param file the target file that candidate layouts should not directly or
     *            indirectly include
     * @return a validator which checks whether resource ids are valid or whether they
     *         could result in a cyclic dependency
     */
    @Nullable
    public static IInputValidator create(@Nullable IFile file) {
        if (file == null) {
            return null;
        }

        IProject project = file.getProject();
        IncludeFinder includeFinder = IncludeFinder.get(project);
        final Collection<String> invalid =
            includeFinder.getInvalidIncludes(file);

        return new CyclicDependencyValidator(invalid);
    }
}

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

import org.eclipse.ltk.core.refactoring.RefactoringContribution;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;

import java.util.Map;

public class ChangeViewContribution extends RefactoringContribution {

    @SuppressWarnings("unchecked")
    @Override
    public RefactoringDescriptor createDescriptor(String id, String project, String description,
            String comment, Map arguments, int flags) throws IllegalArgumentException {
        return new ChangeViewRefactoring.Descriptor(project, description, comment, arguments);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map retrieveArgumentMap(RefactoringDescriptor descriptor) {
        if (descriptor instanceof ChangeViewRefactoring.Descriptor) {
            return ((ChangeViewRefactoring.Descriptor) descriptor).getArguments();
        }
        return super.retrieveArgumentMap(descriptor);
    }
}

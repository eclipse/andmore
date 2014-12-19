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
package org.eclipse.andmore.internal.wizards.newproject;

import org.eclipse.andmore.internal.wizards.newproject.NewProjectWizardState.Mode;

/**
 * A "New Sample Android Project" Wizard.
 * <p/>
 * This displays the new project wizard pre-configured for samples only.
 */
public class NewSampleProjectWizard extends NewProjectWizard {
    /**
     * Creates a new wizard for creating a sample Android project
     */
    public NewSampleProjectWizard() {
        super(Mode.SAMPLE);
    }
}
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
package org.eclipse.andmore.internal.wizards.templates;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.internal.ui.refactoring.PreviewWizardPage;

import java.util.List;

@SuppressWarnings("restriction") // Refactoring UI
class TemplatePreviewPage extends PreviewWizardPage {
    private final NewTemplateWizardState mValues;

    TemplatePreviewPage(NewTemplateWizardState values) {
        super(true);
        mValues = values;
        setTitle("Preview");
        setDescription("Optionally review pending changes");
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            List<Change> changes = mValues.computeChanges();
            CompositeChange root = new CompositeChange("Create template",
                    changes.toArray(new Change[changes.size()]));
            setChange(root);
        }

        super.setVisible(visible);
    }
}

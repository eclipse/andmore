/*
 * Copyright (C) 2014 The Android Open Source Project
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
package org.eclipse.andmore.internal.wizards.exportgradle;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

class ImportInsteadPage extends WizardPage {
    public ImportInsteadPage() {
        super("importInstead");
        setTitle("Import Instead?");
        setDescription("Consider importing directly into Android Studio instead of exporting from Eclipse");
    }

    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NULL);
        setControl(container);
        container.setLayout(new GridLayout(1, false));

        CLabel label = new CLabel(container, SWT.NONE);
        label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false, 1, 1));
        label.setText(
                "Recent versions of Android Studio now support direct import of ADT projects.\n" +
                "\n" +
                "There are advantages to importing from Studio instead of exporting from Eclipse:\n" +
                "- It can replace jars and library projects with Gradle dependencies instead\n" +
                "- On import, it creates a new copy of the project and changes the project structure\n" +
                "  to the new Gradle directory layout which better supports multiple resource directories.\n" +
                "- It can merge instrumentation test projects into the same project\n" +
                "- Android Studio is released more frequently than the ADT plugin, so the import\n" +
                "  mechanism more closely tracks the rapidly evolving requirements of Studio Gradle\n" +
                "  projects.\n" +
                "\n" +
                "If you want to preserve your Eclipse directory structure, or if for some reason import\n" +
                "in Studio doesn't work (please let us know by filing a bug), continue to export from\n" +
                "Eclipse instead.");
    }
}

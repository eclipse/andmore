/*
 * Copyright (C) 2013 The Android Open Source Project
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

import com.google.common.collect.Multimap;

import org.eclipse.andmore.internal.wizards.exportgradle.ExportStatus.FileStatus;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import java.io.File;
import java.util.Collection;

/**
 * Final page to review the result of the export.
 */
public class FinalPage extends WizardPage {

    private final ProjectSetupBuilder mBuilder;
    private ExportStatus mStatus;

    private Text mText;

    public FinalPage(ProjectSetupBuilder builder) {
        super("FinalPage"); //$NON-NLS-1$
        mBuilder = builder;
        setPageComplete(true);
        setTitle(ExportMessages.PageTitle);
        setDescription(ExportMessages.PageDescription);
    }

    @Override
    public void createControl(Composite parent) {
        initializeDialogUnits(parent);

        mText = new Text(parent, SWT.MULTI | SWT.READ_ONLY);
        mText.setLayoutData(new GridData(GridData.FILL_BOTH
                | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));

        setControl(mText);
        Dialog.applyDialogFont(parent);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            mStatus = mBuilder.getStatus();
            mBuilder.setCanFinish(!mStatus.hasError());
            mBuilder.setCanGenerate(false);

            StringBuilder sb = new StringBuilder();
            if (mStatus.hasError()) {
                sb.append("There was an error!").append("\n\n");

                String errorMsg = mStatus.getErrorMessage();
                if (errorMsg != null) {
                    sb.append(errorMsg);
                }

                Multimap<FileStatus, File> fileStatusMap = mStatus.getFileStatus();
                Collection<File> files = fileStatusMap.values();
                if (files != null) {
                    sb.append("\n\n").append("Error on files:").append('\n');
                    for (File file : files) {
                        sb.append("\n").append(file.getAbsolutePath());
                    }
                }
            } else {
                sb.append("Export successful.\n\n");

                int count = mBuilder.getModuleCount();
                if (count > 1) {
                    sb.append(String.format("Exported %s modules", count)).append('\n');
                    sb.append(String.format(
                            "Root folder: %s", mBuilder.getCommonRoot().toOSString()));
                } else {
                    sb.append("Exported project: ").append(mBuilder.getCommonRoot().toOSString());
                }

                sb.append("\n\n").append("Choose 'import project' in Android Studio").append('\n');
                sb.append("and select the following file:").append("\n\t");

                File bGradle = new File(
                        mBuilder.getCommonRoot().toFile(), BuildFileCreator.BUILD_FILE);
                sb.append(bGradle.getAbsolutePath());

                sb.append("\n\n").append("Do NOT import the Eclipse project itself!");
            }

            mText.setText(sb.toString());
        }
    }
}
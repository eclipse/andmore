/*
 * Copyright (C) 2007 The Android Open Source Project
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

package org.eclipse.andmore.internal.editors.manifest.pages;

import org.eclipse.andmore.internal.editors.manifest.ManifestEditor;
import org.eclipse.andmore.internal.editors.ui.SectionHelper.ManifestSectionPart;
import org.eclipse.andmore.internal.project.ExportHelper;
import org.eclipse.andmore.internal.sdk.ProjectState;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.andmore.internal.wizards.export.ExportWizard;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.FileEditorInput;

/**
 * Export section part for overview page.
 */
final class OverviewExportPart extends ManifestSectionPart {

    private final OverviewPage mOverviewPage;

    public OverviewExportPart(OverviewPage overviewPage, final Composite body, FormToolkit toolkit,
            ManifestEditor editor) {
        super(body, toolkit, ExpandableComposite.TWISTIE | ExpandableComposite.EXPANDED, true /* description */);
        mOverviewPage = overviewPage;
        Section section = getSection();
        section.setText("Exporting");

        final IProject project = getProject();
        boolean isLibrary = false;
        if (project != null) {
            ProjectState state = Sdk.getProjectState(project);
            if (state != null) {
                isLibrary = state.isLibrary();
            }
        }

        if (isLibrary) {
            section.setDescription("Library project cannot be exported.");
            Composite table = createTableLayout(toolkit, 2 /* numColumns */);
            createFormText(table, toolkit, true, "<form></form>", false /* setupLayoutData */);
        } else {
            section.setDescription("To export the application for distribution, you have the following options:");

            Composite table = createTableLayout(toolkit, 2 /* numColumns */);

            StringBuffer buf = new StringBuffer();
            buf.append("<form><li><a href=\"wizard\">"); //$NON-NLS-1$
            buf.append("Use the Export Wizard");
            buf.append("</a>"); //$NON-NLS-1$
            buf.append(" to export and sign an APK");
            buf.append("</li>"); //$NON-NLS-1$
            buf.append("<li><a href=\"manual\">"); //$NON-NLS-1$
            buf.append("Export an unsigned APK");
            buf.append("</a>"); //$NON-NLS-1$
            buf.append(" and sign it manually");
            buf.append("</li></form>"); //$NON-NLS-1$

            FormText text = createFormText(table, toolkit, true, buf.toString(),
                    false /* setupLayoutData */);
            text.addHyperlinkListener(new HyperlinkAdapter() {
                @Override
                public void linkActivated(HyperlinkEvent e) {
                    if (project != null) {
                        if ("manual".equals(e.data)) { //$NON-NLS-1$
                            // now we can export an unsigned apk for the project.
                            ExportHelper.exportUnsignedReleaseApk(project);
                        } else {
                            // call the export wizard
                            StructuredSelection selection = new StructuredSelection(project);

                            ExportWizard wizard = new ExportWizard();
                            wizard.init(PlatformUI.getWorkbench(), selection);
                            WizardDialog dialog = new WizardDialog(body.getShell(), wizard);
                            dialog.open();
                        }
                    }
                }
            });
        }

        layoutChanged();
    }

    /**
     * Returns the project of the edited file.
     */
    private IProject getProject() {
        IEditorInput input = mOverviewPage.mEditor.getEditorInput();
        if (input instanceof FileEditorInput) {
            FileEditorInput fileInput = (FileEditorInput)input;
            IFile file = fileInput.getFile();
            return file.getProject();
        }

        return null;
    }
}

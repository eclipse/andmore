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

package org.eclipse.andmore.internal.editors.export;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.ColumnLayout;
import org.eclipse.ui.forms.widgets.ColumnLayoutData;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;


/**
 * Page for export properties, used by {@link ExportEditor}.
 * It displays a part to edit the properties and another part
 * to provide some links and actions.
 */
public final class ExportPropertiesPage extends FormPage {

    /** Page id used for switching tabs programmatically */
    final static String PAGE_ID = "export_prop_page"; //$NON-NLS-1$

    /** Container editor */
    ExportEditor mEditor;
    /** Export fields part */
    private ExportFieldsPart mFieldsPart;
    /** Export links part */
    private ExportLinksPart mLinksPart;

    public ExportPropertiesPage(ExportEditor editor) {
        super(editor, PAGE_ID, "Export Properties");  // tab's label, user visible, keep it short
        mEditor = editor;
    }

    /**
     * Creates the content in the form hosted in this page.
     *
     * @param managedForm the form hosted in this page.
     */
    @Override
    protected void createFormContent(IManagedForm managedForm) {
        super.createFormContent(managedForm);
        ScrolledForm form = managedForm.getForm();
        form.setText("Android Export Properties");
        form.setImage(AdtPlugin.getAndroidLogo());

        Composite body = form.getBody();
        FormToolkit toolkit = managedForm.getToolkit();

        body.setLayout(new ColumnLayout());

        mFieldsPart = new ExportFieldsPart(body, toolkit, mEditor);
        mFieldsPart.getSection().setLayoutData(new ColumnLayoutData());
        managedForm.addPart(mFieldsPart);

        mLinksPart = new ExportLinksPart(body, toolkit, mEditor);
        mLinksPart.getSection().setLayoutData(new ColumnLayoutData());
        managedForm.addPart(mLinksPart);

        mFieldsPart.onModelInit(mEditor);
        mLinksPart.onModelInit(mEditor);
    }

    /**
     * Called after all pages have been created, to let the parts initialize their
     * content based on the document's model.
     * <p/>
     * The model should be acceded via the {@link ExportEditor}.
     */
    public void onModelInit() {
        if (mFieldsPart != null) {
            mFieldsPart.onModelInit(mEditor);
        }

        if (mLinksPart != null) {
            mLinksPart.onModelInit(mEditor);
        }
    }

    /**
     * Called after the document model has been changed. The model should be acceded via
     * the {@link ExportEditor}.
     *
     * @param event Specification of changes applied to document.
     */
    public void onModelChanged(DocumentEvent event) {
        if (mFieldsPart != null) {
            mFieldsPart.onModelChanged(mEditor, event);
        }

        if (mLinksPart != null) {
            mLinksPart.onModelChanged(mEditor, event);
        }
    }
}

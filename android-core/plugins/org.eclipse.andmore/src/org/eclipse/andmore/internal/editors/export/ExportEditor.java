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

import org.eclipse.andmore.AdtConstants;
import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.editors.AndroidTextEditor;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.FileEditorInput;

/**
 * Multi-page form editor for export.properties in Export Projects.
 */
public class ExportEditor extends AndroidTextEditor {

    public static final String ID = AdtConstants.EDITORS_NAMESPACE + ".text.ExportEditor"; //$NON-NLS-1$

    private ExportPropertiesPage mExportPropsPage;

    /**
     * Creates the form editor for resources XML files.
     */
    public ExportEditor() {
        super();
    }

    // ---- Base Class Overrides ----

    /**
     * Returns whether the "save as" operation is supported by this editor.
     * <p/>
     * Save-As is a valid operation for the ManifestEditor since it acts on a
     * single source file.
     *
     * @see IEditorPart
     */
    @Override
    public boolean isSaveAsAllowed() {
        return true;
    }

    /**
     * Create the various form pages.
     */
    @Override
    protected void createFormPages() {
        try {
            mExportPropsPage = new ExportPropertiesPage(this);
            addPage(mExportPropsPage);
        } catch (PartInitException e) {
            AdtPlugin.log(e, "Error creating nested page"); //$NON-NLS-1$
        }

    }

    /* (non-java doc)
     * Change the tab/title name to include the project name.
     */
    @Override
    protected void setInput(IEditorInput input) {
        super.setInput(input);
        if (input instanceof FileEditorInput) {
            FileEditorInput fileInput = (FileEditorInput) input;
            IFile file = fileInput.getFile();
            setPartName(String.format("%1$s", file.getName()));
        }
    }

    @Override
    protected void postCreatePages() {
        super.postCreatePages();
        mExportPropsPage.onModelInit();
    }

    /**
     * Indicates changes were made to the document.
     *
     * @param event Specification of changes applied to document.
     */
    @Override
    protected void onDocumentChanged(DocumentEvent event) {
        super.onDocumentChanged(event);
        mExportPropsPage.onModelChanged(event);
    }

    // ---- Local Methods ----

}

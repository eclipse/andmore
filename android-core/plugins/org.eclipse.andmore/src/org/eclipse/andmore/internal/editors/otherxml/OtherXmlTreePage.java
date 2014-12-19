/*
 * Copyright (C) 2008 The Android Open Source Project
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

package org.eclipse.andmore.internal.editors.otherxml;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.editors.IPageImageProvider;
import org.eclipse.andmore.internal.editors.IconFactory;
import org.eclipse.andmore.internal.editors.common.CommonXmlEditor;
import org.eclipse.andmore.internal.editors.ui.tree.UiTreeBlock;
import org.eclipse.andmore.internal.editors.uimodel.UiElementNode;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.ScrolledForm;

/**
 * Page for the xml form editor.
 */
public final class OtherXmlTreePage extends FormPage implements IPageImageProvider {
    /** Page id used for switching tabs programmatically */
    public final static String PAGE_ID = "xml_tree_page"; //$NON-NLS-1$

    /** Container editor */
    CommonXmlEditor mEditor;

    public OtherXmlTreePage(CommonXmlEditor editor) {
        super(editor, PAGE_ID, "Structure");  // tab's label, keep it short
        mEditor = editor;
    }

    @Override
    public Image getPageImage() {
        return IconFactory.getInstance().getIcon("editor_page_design");  //$NON-NLS-1$
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
        form.setText("Android Xml");
        form.setImage(AdtPlugin.getAndroidLogo());

        UiElementNode rootNode = mEditor.getUiRootNode();
        UiTreeBlock block = new UiTreeBlock(mEditor, rootNode,
                true /* autoCreateRoot */,
                null /* no element filters */,
                "Xml Elements",
                "List of all xml elements in this XML file.");
        block.createContent(managedForm);
    }
}

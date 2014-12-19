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

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.editors.IPageImageProvider;
import org.eclipse.andmore.internal.editors.IconFactory;
import org.eclipse.andmore.internal.editors.descriptors.ElementDescriptor;
import org.eclipse.andmore.internal.editors.manifest.ManifestEditor;
import org.eclipse.andmore.internal.editors.manifest.descriptors.AndroidManifestDescriptors;
import org.eclipse.andmore.internal.editors.ui.tree.UiTreeBlock;
import org.eclipse.andmore.internal.editors.uimodel.UiElementNode;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;


/**
 * Page for "Application" settings, part of the AndroidManifest form editor.
 * <p/>
 * Useful reference:
 * <a href="http://www.eclipse.org/articles/Article-Forms/article.html">
 *   http://www.eclipse.org/articles/Article-Forms/article.html</a>
 */
public final class ApplicationPage extends FormPage implements IPageImageProvider {
    /** Page id used for switching tabs programmatically */
    public final static String PAGE_ID = "application_page"; //$NON-NLS-1$

    /** Container editor */
    ManifestEditor mEditor;
    /** The Application Toogle part */
    private ApplicationToggle mTooglePart;
    /** The Application Attributes part */
    private ApplicationAttributesPart mAttrPart;
    /** The tree view block */
    private UiTreeBlock mTreeBlock;

    public ApplicationPage(ManifestEditor editor) {
        super(editor, PAGE_ID, "Application"); // tab's label, keep it short
        mEditor = editor;
    }

    @Override
    public Image getPageImage() {
        return IconFactory.getInstance().getIcon(getTitle(),
                                                 IconFactory.COLOR_BLUE,
                                                 IconFactory.SHAPE_RECT);
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
        form.setText("Android Manifest Application");
        form.setImage(AdtPlugin.getAndroidLogo());

        UiElementNode appUiNode = getUiApplicationNode();

        Composite body = form.getBody();
        FormToolkit toolkit = managedForm.getToolkit();

        // We usually prefer to have a ColumnLayout here. However
        // MasterDetailsBlock.createContent() below will reset the body's layout to a grid layout.
        mTooglePart = new ApplicationToggle(body, toolkit, mEditor, appUiNode);
        mTooglePart.getSection().setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        managedForm.addPart(mTooglePart);
        mAttrPart = new ApplicationAttributesPart(body, toolkit, mEditor, appUiNode);
        mAttrPart.getSection().setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        managedForm.addPart(mAttrPart);

        mTreeBlock = new UiTreeBlock(mEditor, appUiNode,
                false /* autoCreateRoot */,
                null /* element filters */,
                "Application Nodes",
                "List of all elements in the application");
        mTreeBlock.createContent(managedForm);
    }

    /**
     * Retrieves the application UI node. Since this is a mandatory node, it *always*
     * exists, even if there is no matching XML node.
     */
    private UiElementNode getUiApplicationNode() {
        AndroidManifestDescriptors manifestDescriptor = mEditor.getManifestDescriptors();
        if (manifestDescriptor != null) {
            ElementDescriptor desc = manifestDescriptor.getApplicationElement();
            return mEditor.getUiRootNode().findUiChildNode(desc.getXmlName());
        } else {
            // return the ui root node, as a dummy application root node.
            return mEditor.getUiRootNode();
        }
    }

    /**
     * Changes and refreshes the Application UI node handled by the sub parts.
     */
    public void refreshUiApplicationNode() {
        UiElementNode appUiNode = getUiApplicationNode();
        if (mTooglePart != null) {
            mTooglePart.setUiElementNode(appUiNode);
        }
        if (mAttrPart != null) {
            mAttrPart.setUiElementNode(appUiNode);
        }
        if (mTreeBlock != null) {
            mTreeBlock.changeRootAndDescriptors(appUiNode,
                    null /* element filters */,
                    true /* refresh */);
        }
    }
}

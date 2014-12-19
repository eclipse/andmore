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
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.ScrolledForm;

/**
 * Page for permissions settings, part of the AndroidManifest form editor.
 * <p/>
 * Useful reference:
 * <a href="http://www.eclipse.org/articles/Article-Forms/article.html">
 *   http://www.eclipse.org/articles/Article-Forms/article.html</a>
 */
public final class PermissionPage extends FormPage implements IPageImageProvider {
    /** Page id used for switching tabs programmatically */
    public final static String PAGE_ID = "permission_page"; //$NON-NLS-1$

    /** Container editor */
    ManifestEditor mEditor;

    private UiTreeBlock mTreeBlock;

    public PermissionPage(ManifestEditor editor) {
        super(editor, PAGE_ID, "Permissions");  // tab label, keep it short
        mEditor = editor;
    }

    @Override
    public Image getPageImage() {
        return IconFactory.getInstance().getIcon(getTitle(),
                                                 IconFactory.COLOR_RED,
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
        form.setText("Android Manifest Permissions");
        form.setImage(AdtPlugin.getAndroidLogo());

        UiElementNode manifest = mEditor.getUiRootNode();
        AndroidManifestDescriptors manifestDescriptor = mEditor.getManifestDescriptors();

        ElementDescriptor[] descriptorFilters = null;
        if (manifestDescriptor != null) {
            descriptorFilters = new ElementDescriptor[] {
                    manifestDescriptor.getPermissionElement(),
                    manifestDescriptor.getUsesPermissionElement(),
                    manifestDescriptor.getPermissionGroupElement(),
                    manifestDescriptor.getPermissionTreeElement()
            };
        }
        mTreeBlock = new UiTreeBlock(mEditor, manifest,
                true /* autoCreateRoot */,
                descriptorFilters,
                "Permissions",
                "List of permissions defined and used by the manifest");
        mTreeBlock.createContent(managedForm);
    }

    /**
     * Changes and refreshes the Application UI node handled by the sub parts.
     */
    public void refreshUiNode() {
        if (mTreeBlock != null) {
            UiElementNode manifest = mEditor.getUiRootNode();
            AndroidManifestDescriptors manifestDescriptor = mEditor.getManifestDescriptors();

            mTreeBlock.changeRootAndDescriptors(manifest,
                    new ElementDescriptor[] {
                        manifestDescriptor.getPermissionElement(),
                        manifestDescriptor.getUsesPermissionElement(),
                        manifestDescriptor.getPermissionGroupElement(),
                        manifestDescriptor.getPermissionTreeElement()
                    },
                    true /* refresh */);
        }
    }
}

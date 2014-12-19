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

import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

import java.util.HashMap;

/**
 * Section part for editing the properties in an Export editor.
 */
final class ExportFieldsPart extends AbstractPropertiesFieldsPart {

    public ExportFieldsPart(Composite body, FormToolkit toolkit, ExportEditor editor) {
        super(body, toolkit, editor);
        Section section = getSection();

        section.setText("Export Properties");
        section.setDescription("Properties of export.properties:");

        Composite table = createTableLayout(toolkit, 2 /* numColumns */);

        createLabel(table, toolkit,
                "Available Properties", //label
                "List of properties you can edit in export.properties");  //tooltip

        Text packageField = createLabelAndText(table, toolkit,
                "Package", //label,
                "", //$NON-NLS-1$ value,
                "TODO tooltip for Package");  //tooltip

        Text projectsField = createLabelAndText(table, toolkit,
                "Projects", //label,
                "", //$NON-NLS-1$ value,
                "TODO tooltip for Projects");  //tooltip

        Text versionCodeField = createLabelAndText(table, toolkit,
                "Version Code", //label,
                "", //$NON-NLS-1$ value,
                "TODO tooltip for Version Code");  //tooltip

        Text keyStoreField = createLabelAndText(table, toolkit,
                "Key Store", //label,
                "", //$NON-NLS-1$ value,
                "TODO tooltip for Key Store");  //tooltip

        Text keyAliasField = createLabelAndText(table, toolkit,
                "Key Alias", //label,
                "", //$NON-NLS-1$ value,
                "TODO tooltip for Key Alias");  //tooltip

        // Associate each field with the keyword in the properties files.
        // TODO there's probably some constant to reuse here.
        HashMap<String, Control> map = getNameToField();
        map.put("package", packageField);              //$NON-NLS-1$
        map.put("projects", projectsField);            //$NON-NLS-1$
        map.put("versionCode", versionCodeField);      //$NON-NLS-1$
        map.put("_key.store", keyStoreField);          //$NON-NLS-1$
        map.put("_key.alias", keyAliasField);          //$NON-NLS-1$

        addModifyListenerToFields();
    }

    @Override
    protected void setFieldModifyListener(Control field, ModifyListener markDirtyListener) {
        super.setFieldModifyListener(field, markDirtyListener);
        // TODO override for custom controls
    }

    @Override
    protected String getFieldText(Control field) {
        // TODO override for custom controls
        return super.getFieldText(field);
    }

    @Override
    protected void setFieldText(Control field, String value) {
        // TODO override for custom controls
        super.setFieldText(field, value);
    }
}

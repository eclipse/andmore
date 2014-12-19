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

package org.eclipse.andmore.internal.preferences;

import static com.android.ide.common.xml.XmlAttributeSortOrder.ALPHABETICAL;
import static com.android.ide.common.xml.XmlAttributeSortOrder.LOGICAL;
import static com.android.ide.common.xml.XmlAttributeSortOrder.NO_SORTING;

import com.android.sdkuilib.internal.widgets.ResolutionChooserDialog;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.preference.StringButtonFieldEditor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Preference page for the editors.
 */
public class EditorsPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
    private BooleanFieldEditor mIndentEditor;
    private BooleanFieldEditor mRemoveEmptyEditor;
    private BooleanFieldEditor mOneAttrPerLineEditor;
    private BooleanFieldEditor mSpaceBeforeCloseEditor;
    private BooleanFieldEditor mFormatGuiXmlEditor;

    /**
     * Constructs a new Android editors preference page
     */
    public EditorsPage() {
        super(GRID);
        setPreferenceStore(AdtPlugin.getDefault().getPreferenceStore());
    }

    @Override
    public void init(IWorkbench workbench) {
        // pass
    }

    @Override
    protected void createFieldEditors() {
        Composite parent = getFieldEditorParent();

        addField(new DensityFieldEditor(AdtPrefs.PREFS_MONITOR_DENSITY,
                "Monitor Density", parent));

        final MyBooleanFieldEditor editor = new MyBooleanFieldEditor(
                AdtPrefs.PREFS_USE_CUSTOM_XML_FORMATTER,
                "Format XML files using the standard Android XML style rather than the \n" +
                "configured Eclipse XML style (additional options below)",
                parent);
        addField(editor);

        // Add a listener which fires whenever the checkbox for the custom formatter
        // is toggled -- this will be used to enable/disable the formatting related options
        // on the page. To do this we subclass the BooleanFieldEditor to make the protected
        // method getChangeControl public (so we can access it and add a listener on it).
        // This is pretty ugly but I found several posts in the Eclipse forums asking
        // how to do it and they were all unanswered. (No, calling setPropertyChangeListener
        // does not work.)
        Button checkbox = editor.getChangeControl(parent);
        checkbox.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateCustomFormattingOptions(editor.getBooleanValue());
            }
        });

        mIndentEditor = new BooleanFieldEditor(AdtPrefs.PREFS_USE_ECLIPSE_INDENT,
                "Use Eclipse setting for indentation width and space or tab character "
                + "indentation \n(Android default is 4 space characters)",
                parent);
        addField(mIndentEditor);

        mRemoveEmptyEditor = new BooleanFieldEditor(AdtPrefs.PREVS_REMOVE_EMPTY_LINES,
                "Always remove empty lines between elements",
                parent);
        addField(mRemoveEmptyEditor);

        mOneAttrPerLineEditor = new BooleanFieldEditor(AdtPrefs.PREFS_ONE_ATTR_PER_LINE,
                "Allow single attributes to appear on the same line as their elements",
                parent);
        addField(mOneAttrPerLineEditor);

        mSpaceBeforeCloseEditor = new BooleanFieldEditor(AdtPrefs.PREFS_SPACE_BEFORE_CLOSE,
                "Add a space before the > or /> in opening tags",
                parent);
        addField(mSpaceBeforeCloseEditor);

        addField(new RadioGroupFieldEditor(AdtPrefs.PREFS_ATTRIBUTE_SORT,
                "Sort Attributes", 1,
                new String[][] {
                    { "&Logical (id, style, layout attributes, remaining attributes alphabetically)",
                        LOGICAL.key },
                    { "&Alphabetical", ALPHABETICAL.key },
                    { "&None", NO_SORTING.key },
                },
                parent, true));

        mFormatGuiXmlEditor = new BooleanFieldEditor(AdtPrefs.PREFS_FORMAT_GUI_XML,
                "Automatically format the XML edited by the visual layout editor",
                parent);
        addField(mFormatGuiXmlEditor);

        addField(new BooleanFieldEditor(AdtPrefs.PREFS_FORMAT_ON_SAVE,
                "Format on Save",
                parent));

        addField(new BooleanFieldEditor(AdtPrefs.PREFS_SHARED_LAYOUT_EDITOR,
                "Use a single layout editor for all configuration variations of a layout",
                parent));

        boolean enabled = getPreferenceStore().getBoolean(AdtPrefs.PREFS_USE_CUSTOM_XML_FORMATTER);
        updateCustomFormattingOptions(enabled);
    }

    private void updateCustomFormattingOptions(boolean enabled) {
        Composite parent = getFieldEditorParent();
        mIndentEditor.setEnabled(enabled, parent);
        mRemoveEmptyEditor.setEnabled(enabled, parent);
        mOneAttrPerLineEditor.setEnabled(enabled, parent);
        mSpaceBeforeCloseEditor.setEnabled(enabled, parent);
        mFormatGuiXmlEditor.setEnabled(enabled, parent);
    }

    /**
     * Overridden solely so that I can get access to the checkbox button to listen to
     * state changes
     */
    private class MyBooleanFieldEditor extends BooleanFieldEditor {
        public MyBooleanFieldEditor(String name, String label, Composite parent) {
            super(name, label, parent);
        }
        @Override
        protected Button getChangeControl(Composite parent) {
            return super.getChangeControl(parent);
        }
    }

    /**
     * Custom {@link StringButtonFieldEditor} to call out to {@link ResolutionChooserDialog}
     * when the button is called.
     */
    private static class DensityFieldEditor extends StringButtonFieldEditor {

        public DensityFieldEditor(String name, String labelText, Composite parent) {
            super(name, labelText, parent);
            setChangeButtonText("Compute...");
        }

        @Override
        protected String changePressed() {
            ResolutionChooserDialog dialog = new ResolutionChooserDialog(getShell());
            if (dialog.open() == Window.OK) {
                return Integer.toString(dialog.getDensity());
            }

            return null;
        }
    }
}

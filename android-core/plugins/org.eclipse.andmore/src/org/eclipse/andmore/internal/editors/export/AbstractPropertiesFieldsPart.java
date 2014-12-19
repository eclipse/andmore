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

import com.android.SdkConstants;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.editors.ui.SectionHelper.ManifestSectionPart;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Section part for editing fields of a properties file in an Export editor.
 * <p/>
 * This base class is intended to be derived and customized.
 */
abstract class AbstractPropertiesFieldsPart extends ManifestSectionPart {

    private final HashMap<String, Control> mNameToField = new HashMap<String, Control>();

    private ExportEditor mEditor;

    private boolean mInternalTextUpdate = false;

    public AbstractPropertiesFieldsPart(Composite body, FormToolkit toolkit, ExportEditor editor) {
        super(body, toolkit, ExpandableComposite.TWISTIE | ExpandableComposite.EXPANDED, true /* description */);
        mEditor = editor;
    }

    protected HashMap<String, Control> getNameToField() {
        return mNameToField;
    }

    protected ExportEditor getEditor() {
        return mEditor;
    }

    protected void setInternalTextUpdate(boolean internalTextUpdate) {
        mInternalTextUpdate = internalTextUpdate;
    }

    protected boolean isInternalTextUpdate() {
        return mInternalTextUpdate;
    }

    /**
     * Adds a modify listener to every text field that will mark the part as dirty.
     *
     * CONTRACT: Derived classes MUST call this at the end of their constructor.
     *
     * @see #setFieldModifyListener(Control, ModifyListener)
     */
    protected void addModifyListenerToFields() {
        ModifyListener markDirtyListener = new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                // Mark the part as dirty if a field has been changed.
                // This will force a commit() operation to store the data in the model.
                if (!mInternalTextUpdate) {
                    markDirty();
                }
            }
        };

        for (Control field : mNameToField.values()) {
            setFieldModifyListener(field, markDirtyListener);
        }
    }

    /**
     * Sets a listener that will mark the part as dirty when the control is modified.
     * The base method only handles {@link Text} fields.
     *
     * CONTRACT: Derived classes CAN use this to add a listener to their own controls.
     * The listener must call {@link #markDirty()} when the control is modified by the user.
     *
     * @param field A control previously registered with {@link #getNameToField()}.
     * @param markDirtyListener A {@link ModifyListener} that invokes {@link #markDirty()}.
     *
     * @see #isInternalTextUpdate()
     */
    protected void setFieldModifyListener(Control field, ModifyListener markDirtyListener) {
        if (field instanceof Text) {
            ((Text) field).addModifyListener(markDirtyListener);
        }
    }

    /**
     * Updates the model based on the content of fields. This is invoked when a field
     * has marked the document as dirty.
     *
     * CONTRACT: Derived classes do not need to override this.
     */
    @Override
    public void commit(boolean onSave) {

        // We didn't store any information indicating which field was dirty (we could).
        // Since there are not many fields, just update all the document lines that
        // match our field keywords.

        if (isDirty()) {
            mEditor.wrapRewriteSession(new Runnable() {
                @Override
                public void run() {
                    saveFieldsToModel();
                }
            });
        }

        super.commit(onSave);
    }

    private void saveFieldsToModel() {
        // Get a list of all keywords to process. Go thru the document, replacing in-place
        // the ones we can find and remove them from this set. This will leave the list
        // of new keywords to add at the end of the document.
        HashSet<String> allKeywords = new HashSet<String>(mNameToField.keySet());

        IDocument doc = mEditor.getDocument();
        int numLines = doc.getNumberOfLines();

        String delim = null;
        try {
            delim = numLines > 0 ? doc.getLineDelimiter(0) : null;
        } catch (BadLocationException e1) {
            // ignore
        }
        if (delim == null || delim.length() == 0) {
            delim = SdkConstants.CURRENT_PLATFORM == SdkConstants.PLATFORM_WINDOWS ?
                    "\r\n" : "\n"; //$NON-NLS-1$ //$NON-NLS-2#
        }

        for (int i = 0; i < numLines; i++) {
            try {
                IRegion info = doc.getLineInformation(i);
                String line = doc.get(info.getOffset(), info.getLength());
                line = line.trim();
                if (line.startsWith("#")) {  //$NON-NLS-1$
                    continue;
                }

                int pos = line.indexOf('=');
                if (pos > 0 && pos < line.length() - 1) {
                    String key = line.substring(0, pos).trim();

                    Control field = mNameToField.get(key);
                    if (field != null) {

                        // This is the new line to inject
                        line = key + "=" + getFieldText(field);

                        try {
                            // replace old line by new one. This doesn't change the
                            // line delimiter.
                            mInternalTextUpdate = true;
                            doc.replace(info.getOffset(), info.getLength(), line);
                            allKeywords.remove(key);
                        } finally {
                            mInternalTextUpdate = false;
                        }
                    }
                }

            } catch (BadLocationException e) {
                // TODO log it
                AdtPlugin.log(e, "Failed to replace in export.properties");
            }
        }

        for (String key : allKeywords) {
            Control field = mNameToField.get(key);
            if (field != null) {
                // This is the new line to inject
                String line = key + "=" + getFieldText(field);

                try {
                    // replace old line by new one
                    mInternalTextUpdate = true;

                    numLines = doc.getNumberOfLines();

                    IRegion info = numLines > 0 ? doc.getLineInformation(numLines - 1) : null;
                    if (info != null && info.getLength() == 0) {
                        // last line is empty. Insert right before there.
                        doc.replace(info.getOffset(), info.getLength(), line);
                    } else {
                        if (numLines > 0) {
                            String eofDelim = doc.getLineDelimiter(numLines - 1);
                            if (eofDelim == null || eofDelim.length() == 0) {
                                // The document doesn't end with a line delimiter, so add
                                // one to the line to be written.
                                line = delim + line;
                            }
                        }

                        int len = doc.getLength();
                        doc.replace(len, 0, line);
                    }

                    allKeywords.remove(key);
                } catch (BadLocationException e) {
                    // TODO log it
                    AdtPlugin.log(e, "Failed to append to export.properties: %s", line);
                } finally {
                    mInternalTextUpdate = false;
                }
            }
        }
    }

    /**
     * Used when committing fields values to the model to retrieve the text
     * associated with a field.
     * <p/>
     * The base method only handles {@link Text} controls.
     *
     * CONTRACT: Derived classes CAN use this to support their own controls.
     *
     * @param field A control previously registered with {@link #getNameToField()}.
     * @return A non-null string to write to the properties files.
     */
    protected String getFieldText(Control field) {
        if (field instanceof Text) {
            return ((Text) field).getText();
        }
        return "";
    }

    /**
     * Called after all pages have been created, to let the parts initialize their
     * content based on the document's model.
     * <p/>
     * The model should be acceded via the {@link ExportEditor}.
     *
     * @param editor The {@link ExportEditor} instance.
     */
    public void onModelInit(ExportEditor editor) {

        // Start with a set of all the possible keywords and remove those we
        // found in the document as we read the lines.
        HashSet<String> allKeywords = new HashSet<String>(mNameToField.keySet());

        // Parse the lines in the document for patterns "keyword=value",
        // trimming all whitespace and discarding lines that start with # (comments)
        // then affect to the internal fields as appropriate.
        IDocument doc = editor.getDocument();
        int numLines = doc.getNumberOfLines();
        for (int i = 0; i < numLines; i++) {
            try {
                IRegion info = doc.getLineInformation(i);
                String line = doc.get(info.getOffset(), info.getLength());
                line = line.trim();
                if (line.startsWith("#")) {  //$NON-NLS-1$
                    continue;
                }

                int pos = line.indexOf('=');
                if (pos > 0 && pos < line.length() - 1) {
                    String key = line.substring(0, pos).trim();

                    Control field = mNameToField.get(key);
                    if (field != null) {
                        String value = line.substring(pos + 1).trim();
                        try {
                            mInternalTextUpdate = true;
                            setFieldText(field, value);
                            allKeywords.remove(key);
                        } finally {
                            mInternalTextUpdate = false;
                        }
                    }
                }

            } catch (BadLocationException e) {
                // TODO log it
                AdtPlugin.log(e, "Failed to set field to export.properties value");
            }
        }

        // Clear the text of any keyword we didn't find in the document
        Iterator<String> iterator = allKeywords.iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            Control field = mNameToField.get(key);
            if (field != null) {
                try {
                    mInternalTextUpdate = true;
                    setFieldText(field, "");
                    iterator.remove();
                } finally {
                    mInternalTextUpdate = false;
                }
            }
        }
    }

    /**
     * Used when reading the model to set the field values.
     * <p/>
     * The base method only handles {@link Text} controls.
     *
     * CONTRACT: Derived classes CAN use this to support their own controls.
     *
     * @param field A control previously registered with {@link #getNameToField()}.
     * @param value A non-null string to that was read from the properties files.
     *              The value is an empty string if the property line is missing.
     */
    protected void setFieldText(Control field, String value) {
        if (field instanceof Text) {
            ((Text) field).setText(value);
        }
    }

    /**
     * Called after the document model has been changed. The model should be acceded via
     * the {@link ExportEditor} (e.g. getDocument, wrapRewriteSession)
     *
     * @param editor The {@link ExportEditor} instance.
     * @param event Specification of changes applied to document.
     */
    public void onModelChanged(ExportEditor editor, DocumentEvent event) {
        // To simplify and since we don't have many fields, just reload all the values.
        // A better way would to be to look at DocumentEvent which gives us the offset/length
        // and text that has changed.
        if (!mInternalTextUpdate) {
            onModelInit(editor);
        }
    }
}

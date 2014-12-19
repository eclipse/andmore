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
import org.eclipse.andmore.internal.editors.AndroidTextEditor;
import org.eclipse.andmore.internal.editors.ui.SectionHelper.ManifestSectionPart;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * Links section part for export properties page.
 * Displays some help and some links/actions for the user to use.
 */
final class ExportLinksPart extends ManifestSectionPart {

    private FormText mFormText;

    public ExportLinksPart(Composite body, FormToolkit toolkit, ExportEditor editor) {
        super(body, toolkit, ExpandableComposite.TWISTIE | ExpandableComposite.EXPANDED, true /* description */);
        Section section = getSection();
        section.setText("Links");
        section.setDescription("TODO SOME TEXT HERE. You can also edit the XML directly.");

        final Composite table = createTableLayout(toolkit, 2 /* numColumns */);

        StringBuffer buf = new StringBuffer();
        buf.append("<form>"); //$NON-NLS-1$

        buf.append("<li style=\"image\" value=\"android_img\"><a href=\"action_dosomething\">");
        buf.append("TODO Custom Action");
        buf.append("</a>"); //$NON-NLS-1$
        buf.append(": blah blah do something (like build/export).");
        buf.append("</li>"); //$NON-NLS-1$

        buf.append(String.format("<li style=\"image\" value=\"android_img\"><a href=\"page:%1$s\">", //$NON-NLS-1$
                AndroidTextEditor.TEXT_EDITOR_ID));
        buf.append("XML Source");
        buf.append("</a>"); //$NON-NLS-1$
        buf.append(": Directly edit the AndroidManifest.xml file.");
        buf.append("</li>"); //$NON-NLS-1$

        buf.append("<li style=\"image\" value=\"android_img\">"); //$NON-NLS-1$
        buf.append("<a href=\"http://code.google.com/android/devel/bblocks-manifest.html\">Documentation</a>: Documentation from the Android SDK for AndroidManifest.xml."); //$NON-NLS-1$
        buf.append("</li>"); //$NON-NLS-1$
        buf.append("</form>"); //$NON-NLS-1$

        mFormText = createFormText(table, toolkit, true, buf.toString(),
                false /* setupLayoutData */);

        Image androidLogo = AdtPlugin.getAndroidLogo();
        mFormText.setImage("android_img", androidLogo); //$NON-NLS-1$

        // Listener for default actions (page change, URL web browser)
        mFormText.addHyperlinkListener(editor.createHyperlinkListener());

        mFormText.addHyperlinkListener(new IHyperlinkListener() {
            @Override
            public void linkExited(HyperlinkEvent e) {
                // pass
            }

            @Override
            public void linkEntered(HyperlinkEvent e) {
                // pass
            }

            @Override
            public void linkActivated(HyperlinkEvent e) {
                String link = e.data.toString();
                if ("action_dosomething".equals(link)) {
                    MessageBox mb = new MessageBox(table.getShell(), SWT.OK);
                    mb.setText("Custom Action Invoked");
                    mb.open();
                }
            }
        });
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
        // pass
    }

    /**
     * Called after the document model has been changed. The model should be acceded via
     * the {@link ExportEditor} (e.g. getDocument, wrapRewriteSession)
     *
     * @param editor The {@link ExportEditor} instance.
     * @param event Specification of changes applied to document.
     */
    public void onModelChanged(ExportEditor editor, DocumentEvent event) {
        // pass
    }
}

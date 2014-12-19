/*
 * Copyright (C) 2011 The Android Open Source Project
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
package org.eclipse.andmore.internal.editors.formatting;

import org.eclipse.andmore.internal.preferences.AdtPrefs;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TypedPosition;
import org.eclipse.jface.text.formatter.FormattingContext;
import org.eclipse.jface.text.formatter.FormattingContextProperties;
import org.eclipse.jface.text.formatter.IContentFormatter;
import org.eclipse.jface.text.formatter.IContentFormatterExtension;
import org.eclipse.jface.text.formatter.IFormattingContext;
import org.eclipse.jface.text.formatter.IFormattingStrategy;
import org.eclipse.wst.xml.core.text.IXMLPartitions;

/**
 * Formatter which replaces the Eclipse formatter for the Android XML editors, and
 * delegates to it if the user has chosen to use the Eclipse formatter instead by turning
 * off {@link AdtPrefs#getUseCustomXmlFormatter()}
 */
public class AndroidXmlFormatter implements IContentFormatter, IContentFormatterExtension {
    @Override
    public final void format(IDocument document, IRegion region) {
        /**
         * This method is probably not going to be called. It is part of the
         * {@link IContentFormatter} but since we also implement
         * {@link IContentFormatterExtension} Eclipse should /* be calling
         * {@link #format(IDocument,IFormattingContext)} instead. However, for
         * completeness (and because other implementations of {@link IContentFormatter}
         * also do this we might as well make the method behave correctly
         */
        FormattingContext context = new FormattingContext();
        context.setProperty(FormattingContextProperties.CONTEXT_DOCUMENT, Boolean.FALSE);
        context.setProperty(FormattingContextProperties.CONTEXT_REGION, region);

        format(document, context);
    }

    @Override
    public IFormattingStrategy getFormattingStrategy(String contentType) {
        return new AndroidXmlFormattingStrategy();
    }

    @Override
    public void format(IDocument document, IFormattingContext context) {
        context.setProperty(FormattingContextProperties.CONTEXT_MEDIUM, document);
        formatMaster(context, document, 0, document.getLength());
    }

    protected void formatMaster(IFormattingContext context, IDocument document, int offset,
            int length) {
        try {
            final int delta= offset - document.getLineInformationOfOffset(offset).getOffset();
            offset -= delta;
            length += delta;
        } catch (BadLocationException exception) {
            // Do nothing
        }

        AndroidXmlFormattingStrategy strategy = new AndroidXmlFormattingStrategy();
        context.setProperty(FormattingContextProperties.CONTEXT_PARTITION,
                new TypedPosition(offset, length, IXMLPartitions.XML_DEFAULT));
        strategy.formatterStarts(context);
        strategy.format();
        strategy.formatterStops();
    }
}

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

import static org.eclipse.jface.text.formatter.FormattingContextProperties.CONTEXT_MEDIUM;
import static org.eclipse.jface.text.formatter.FormattingContextProperties.CONTEXT_PARTITION;
import static org.eclipse.jface.text.formatter.FormattingContextProperties.CONTEXT_REGION;

import org.eclipse.andmore.internal.preferences.AdtPrefs;
import org.eclipse.jface.text.TypedPosition;
import org.eclipse.jface.text.formatter.FormattingContext;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.xml.core.internal.formatter.XMLFormatterFormatProcessor;
import org.eclipse.wst.xml.core.text.IXMLPartitions;

/**
 * Customized version of the builtin XML format processor which delegates to the
 * Android specific formatter such that applying format on IFiles work as
 * expected
 */
@SuppressWarnings("restriction")
public class XmlFormatProcessor extends XMLFormatterFormatProcessor {
    /** Constructs a new {@link XmlFormatProcessor} */
    public XmlFormatProcessor() {
    }

    @Override
    public void formatModel(IStructuredModel structuredModel, int start, int length) {
        if (!AdtPrefs.getPrefs().getUseCustomXmlFormatter()) {
            super.formatModel(structuredModel, start, length);
            return;
        }

        AndroidXmlFormatter formatter = new AndroidXmlFormatter();
        IStructuredDocument document = structuredModel.getStructuredDocument();
        FormattingContext context = new FormattingContext();
        context.setProperty(CONTEXT_MEDIUM, document);
        context.setProperty(CONTEXT_PARTITION, new TypedPosition(start, length,
                IXMLPartitions.XML_DEFAULT));
        context.setProperty(CONTEXT_REGION, new org.eclipse.jface.text.Region(start, length));
        formatter.formatMaster(context, document, start, length);
    }
}

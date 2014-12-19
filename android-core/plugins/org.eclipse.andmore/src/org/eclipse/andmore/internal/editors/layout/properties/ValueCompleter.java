/*
 * Copyright (C) 2012 The Android Open Source Project
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
package org.eclipse.andmore.internal.editors.layout.properties;

import static com.android.SdkConstants.ATTR_TEXT_SIZE;
import static com.android.SdkConstants.PREFIX_RESOURCE_REF;
import static com.android.SdkConstants.PREFIX_THEME_REF;
import static com.android.SdkConstants.UNIT_DP;
import static com.android.SdkConstants.UNIT_SP;
import static com.android.SdkConstants.VALUE_FALSE;
import static com.android.SdkConstants.VALUE_TRUE;
import static com.android.ide.common.api.IAttributeInfo.Format.BOOLEAN;
import static com.android.ide.common.api.IAttributeInfo.Format.DIMENSION;
import static com.android.ide.common.api.IAttributeInfo.Format.ENUM;
import static com.android.ide.common.api.IAttributeInfo.Format.FLAG;
import static com.android.ide.common.api.IAttributeInfo.Format.FLOAT;
import static com.android.ide.common.api.IAttributeInfo.Format.INTEGER;
import static com.android.ide.common.api.IAttributeInfo.Format.REFERENCE;
import static com.android.ide.common.api.IAttributeInfo.Format.STRING;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.api.IAttributeInfo;
import com.android.ide.common.api.IAttributeInfo.Format;
import com.android.utils.SdkUtils;

import org.eclipse.andmore.internal.editors.common.CommonXmlEditor;
import org.eclipse.andmore.internal.editors.descriptors.AttributeDescriptor;
import org.eclipse.jface.fieldassist.ContentProposal;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * An {@link IContentProposalProvider} which completes possible property values
 * for Android properties, completing resource strings, flag values, enum
 * values, as well as dimension units.
 */
abstract class ValueCompleter implements IContentProposalProvider {
    @Nullable
    protected abstract CommonXmlEditor getEditor();

    @NonNull
    protected abstract AttributeDescriptor getDescriptor();

    @Override
    public IContentProposal[] getProposals(String contents, int position) {
        AttributeDescriptor descriptor = getDescriptor();
        IAttributeInfo info = descriptor.getAttributeInfo();
        EnumSet<Format> formats = info.getFormats();

        List<IContentProposal> proposals = new ArrayList<IContentProposal>();

        String prefix = contents; // TODO: Go back to position inside the array?

        // TODO: If the user is typing in a number, or a number plus a prefix of a dimension unit,
        // then propose that number plus the completed dimension unit (using sp for text, dp
        // for other properties and maybe both if I'm not sure)
        if (formats.contains(STRING)
                && !contents.isEmpty()
                && (formats.size() > 1 && formats.contains(REFERENCE) ||
                        formats.size() > 2)
                && !contents.startsWith(PREFIX_RESOURCE_REF)
                && !contents.startsWith(PREFIX_THEME_REF)) {
            proposals.add(new ContentProposal(contents));
        }

        if (!contents.isEmpty() && Character.isDigit(contents.charAt(0))
                && (formats.contains(DIMENSION)
                        || formats.contains(INTEGER)
                        || formats.contains(FLOAT))) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, n = contents.length(); i < n; i++) {
                char c = contents.charAt(i);
                if (Character.isDigit(c)) {
                    sb.append(c);
                } else {
                    break;
                }
            }

            String number = sb.toString();
            if (formats.contains(Format.DIMENSION)) {
                if (descriptor.getXmlLocalName().equals(ATTR_TEXT_SIZE)) {
                    proposals.add(new ContentProposal(number + UNIT_SP));
                }
                proposals.add(new ContentProposal(number + UNIT_DP));
            } else if (formats.contains(Format.INTEGER)) {
                proposals.add(new ContentProposal(number));
            }
            // Perhaps offer other units too -- see AndroidContentAssist.sDimensionUnits
        }

        if (formats.contains(REFERENCE) || contents.startsWith(PREFIX_RESOURCE_REF)
                || contents.startsWith(PREFIX_THEME_REF)) {
            CommonXmlEditor editor = getEditor();
            if (editor != null) {
                String[] matches = ResourceValueCompleter.computeResourceStringMatches(
                        editor,
                        descriptor, contents.substring(0, position));
                for (String match : matches) {
                    proposals.add(new ContentProposal(match));
                }
            }
        }

        if (formats.contains(FLAG)) {
            String[] values = info.getFlagValues();
            if (values != null) {
                // Flag completion
                int flagStart = prefix.lastIndexOf('|');
                String prepend = null;
                if (flagStart != -1) {
                    prepend = prefix.substring(0, flagStart + 1);
                    prefix = prefix.substring(flagStart + 1).trim();
                }

                boolean exactMatch = false;
                for (String value : values) {
                    if (prefix.equals(value)) {
                        exactMatch = true;
                        proposals.add(new ContentProposal(contents));

                        break;
                    }
                }

                if (exactMatch) {
                    prepend = contents + '|';
                    prefix = "";
                }

                for (String value : values) {
                    if (SdkUtils.startsWithIgnoreCase(value, prefix)) {
                        if (prepend != null && prepend.contains(value)) {
                            continue;
                        }
                        String match;
                        if (prepend != null) {
                            match = prepend + value;
                        } else {
                            match = value;
                        }
                        proposals.add(new ContentProposal(match));
                    }
                }
            }
        } else if (formats.contains(ENUM)) {
            String[] values = info.getEnumValues();
            if (values != null) {
                for (String value : values) {
                    if (SdkUtils.startsWithIgnoreCase(value, prefix)) {
                        proposals.add(new ContentProposal(value));
                    }
                }

                for (String value : values) {
                    if (!SdkUtils.startsWithIgnoreCase(value, prefix)) {
                        proposals.add(new ContentProposal(value));
                    }
                }
            }
        } else if (formats.contains(BOOLEAN)) {
            proposals.add(new ContentProposal(VALUE_TRUE));
            proposals.add(new ContentProposal(VALUE_FALSE));
        }

        return proposals.toArray(new IContentProposal[proposals.size()]);
    }
}

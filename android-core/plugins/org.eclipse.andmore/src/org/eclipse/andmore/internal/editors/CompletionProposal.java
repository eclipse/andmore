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
package org.eclipse.andmore.internal.editors;

import static com.android.SdkConstants.XMLNS;

import com.android.ide.common.api.IAttributeInfo;
import com.android.utils.XmlUtils;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.editors.descriptors.AttributeDescriptor;
import org.eclipse.andmore.internal.editors.descriptors.DescriptorsUtils;
import org.eclipse.andmore.internal.editors.descriptors.ElementDescriptor;
import org.eclipse.andmore.internal.editors.descriptors.IDescriptorProvider;
import org.eclipse.andmore.internal.editors.descriptors.TextAttributeDescriptor;
import org.eclipse.andmore.internal.editors.layout.gle2.DomUtilities;
import org.eclipse.andmore.internal.sdk.AndroidTargetData;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Just like {@link org.eclipse.jface.text.contentassist.CompletionProposal},
 * but computes the documentation string lazily since they are typically only
 * displayed for a small subset (the currently focused item) of the available
 * proposals, and producing the strings requires some computation.
 * <p>
 * It also attempts to compute documentation for value strings like
 * ?android:attr/dividerHeight.
 * <p>
 * TODO: Enhance this to compute documentation for additional values, such as
 * the various enum values (which are available in the attrs.xml file, but not
 * in the AttributeInfo objects for each enum value). To do this, I should
 * basically keep around the maps computed by the attrs.xml parser.
 */
class CompletionProposal implements ICompletionProposal {
    private static final Pattern ATTRIBUTE_PATTERN =
            Pattern.compile("[@?]android:attr/(.*)"); //$NON-NLS-1$

    private final AndroidContentAssist mAssist;
    private final Object mChoice;
    private final int mCursorPosition;
    private int mReplacementOffset;
    private final int mReplacementLength;
    private final String mReplacementString;
    private final Image mImage;
    private final String mDisplayString;
    private final IContextInformation mContextInformation;
    private final String mNsPrefix;
    private final String mNsUri;
    private String mAdditionalProposalInfo;

    CompletionProposal(AndroidContentAssist assist,
            Object choice, String replacementString, int replacementOffset,
            int replacementLength, int cursorPosition, Image image, String displayString,
            IContextInformation contextInformation, String additionalProposalInfo,
            String nsPrefix, String nsUri) {
        assert replacementString != null;
        assert replacementOffset >= 0;
        assert replacementLength >= 0;
        assert cursorPosition >= 0;

        mAssist = assist;
        mChoice = choice;
        mCursorPosition = cursorPosition;
        mReplacementOffset = replacementOffset;
        mReplacementLength = replacementLength;
        mReplacementString = replacementString;
        mImage = image;
        mDisplayString = displayString;
        mContextInformation = contextInformation;
        mAdditionalProposalInfo = additionalProposalInfo;
        mNsPrefix = nsPrefix;
        mNsUri = nsUri;
    }

    @Override
    public Point getSelection(IDocument document) {
        return new Point(mReplacementOffset + mCursorPosition, 0);
    }

    @Override
    public IContextInformation getContextInformation() {
        return mContextInformation;
    }

    @Override
    public Image getImage() {
        return mImage;
    }

    @Override
    public String getDisplayString() {
        if (mDisplayString != null) {
            return mDisplayString;
        }
        return mReplacementString;
    }

    @Override
    public String getAdditionalProposalInfo() {
        if (mAdditionalProposalInfo == null) {
            if (mChoice instanceof ElementDescriptor) {
                String tooltip = ((ElementDescriptor)mChoice).getTooltip();
                mAdditionalProposalInfo = DescriptorsUtils.formatTooltip(tooltip);
            } else if (mChoice instanceof TextAttributeDescriptor) {
                mAdditionalProposalInfo = ((TextAttributeDescriptor) mChoice).getTooltip();
            } else if (mChoice instanceof String) {
                // Try to produce it lazily for strings like @android
                String value = (String) mChoice;
                Matcher matcher = ATTRIBUTE_PATTERN.matcher(value);
                if (matcher.matches()) {
                    String attrName = matcher.group(1);
                    AndroidTargetData data = mAssist.getEditor().getTargetData();
                    if (data != null) {
                        IDescriptorProvider descriptorProvider =
                            data.getDescriptorProvider(mAssist.getRootDescriptorId());
                        if (descriptorProvider != null) {
                            ElementDescriptor[] rootElementDescriptors =
                                descriptorProvider.getRootElementDescriptors();
                            for (ElementDescriptor elementDesc : rootElementDescriptors) {
                                for (AttributeDescriptor desc : elementDesc.getAttributes()) {
                                    String name = desc.getXmlLocalName();
                                    if (attrName.equals(name)) {
                                        IAttributeInfo attributeInfo = desc.getAttributeInfo();
                                        if (attributeInfo != null) {
                                            return attributeInfo.getJavaDoc();
                                        }
                                    }
                                }
                            }
                        }
                    }

                }
            } else if (mChoice instanceof IType) {
                IType type = (IType) mChoice;
                try {
                    ISourceRange javadocRange = type.getJavadocRange();
                    if (javadocRange != null && javadocRange.getLength() > 0) {
                        ISourceRange sourceRange = type.getSourceRange();
                        if (sourceRange != null) {
                            String source = type.getSource();
                            int start = javadocRange.getOffset() - sourceRange.getOffset();
                            int length = javadocRange.getLength();
                            String doc = source.substring(start, start + length);
                            return doc;
                        }
                    }
                    return type.getAttachedJavadoc(new NullProgressMonitor());
                } catch (JavaModelException e) {
                    AdtPlugin.log(e, null);
                }
            }
        }

        return mAdditionalProposalInfo;
    }

    @Override
    public void apply(IDocument document) {
        try {
            Position position = new Position(mReplacementOffset);
            document.addPosition(position);

            // Ensure that the namespace is defined in the document
            String prefix = mNsPrefix;
            if (mNsUri != null && prefix != null) {
                Document dom = DomUtilities.getDocument(mAssist.getEditor());
                if (dom != null) {
                    Element root = dom.getDocumentElement();
                    if (root != null) {
                        // Is the namespace already defined?
                        boolean found = false;
                        NamedNodeMap attributes = root.getAttributes();
                        for (int i = 0, n = attributes.getLength(); i < n; i++) {
                            Attr attribute = (Attr) attributes.item(i);
                            String name = attribute.getName();
                            if (name.startsWith(XMLNS) && mNsUri.equals(attribute.getValue())) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            if (prefix.endsWith(":")) { //$NON-NLS-1$
                                prefix = prefix.substring(0, prefix.length() - 1);
                            }
                            XmlUtils.lookupNamespacePrefix(root, mNsUri, prefix, true);
                        }
                    }
                }
            }

            mReplacementOffset = position.getOffset();
            document.removePosition(position);
            document.replace(mReplacementOffset, mReplacementLength, mReplacementString);
        } catch (BadLocationException x) {
            // ignore
        }
    }
}
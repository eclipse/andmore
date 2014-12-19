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

package org.eclipse.andmore.internal.editors.values;

import static com.android.SdkConstants.ANDROID_NS_NAME_PREFIX;
import static com.android.SdkConstants.ANDROID_PREFIX;
import static com.android.SdkConstants.ATTR_NAME;
import static com.android.SdkConstants.ATTR_TYPE;
import static com.android.SdkConstants.PREFIX_RESOURCE_REF;
import static com.android.SdkConstants.TAG_ITEM;
import static com.android.SdkConstants.TAG_STYLE;
import static org.eclipse.andmore.internal.editors.descriptors.AttributeDescriptor.ATTRIBUTE_ICON_FILENAME;
import static org.eclipse.andmore.internal.sdk.AndroidTargetData.DESCRIPTOR_LAYOUT;

import com.android.annotations.VisibleForTesting;

import org.eclipse.andmore.internal.editors.AndroidContentAssist;
import org.eclipse.andmore.internal.editors.IconFactory;
import org.eclipse.andmore.internal.editors.descriptors.AttributeDescriptor;
import org.eclipse.andmore.internal.editors.descriptors.DocumentDescriptor;
import org.eclipse.andmore.internal.editors.descriptors.ElementDescriptor;
import org.eclipse.andmore.internal.editors.descriptors.IDescriptorProvider;
import org.eclipse.andmore.internal.editors.descriptors.SeparatorAttributeDescriptor;
import org.eclipse.andmore.internal.editors.uimodel.UiAttributeNode;
import org.eclipse.andmore.internal.editors.uimodel.UiElementNode;
import org.eclipse.andmore.internal.editors.uimodel.UiResourceAttributeNode;
import org.eclipse.andmore.internal.sdk.AndroidTargetData;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Content Assist Processor for /res/values and /res/drawable XML files
 * <p>
 * Further enhancements:
 * <ul>
 *   <li> Complete prefixes in the style element itself for the name attribute
 *   <li> Complete parent names
 * </ul>
 */
@VisibleForTesting
public class ValuesContentAssist extends AndroidContentAssist {

    /**
     * Constructor for ResourcesContentAssist
     */
    public ValuesContentAssist() {
        super(AndroidTargetData.DESCRIPTOR_RESOURCES);
    }

    @Override
    protected boolean computeAttributeValues(List<ICompletionProposal> proposals, int offset,
            String parentTagName, String attributeName, Node node, String wordPrefix,
            boolean skipEndTag, int replaceLength) {
        super.computeAttributeValues(proposals, offset, parentTagName, attributeName, node,
                wordPrefix, skipEndTag, replaceLength);

        if (parentTagName.equals(TAG_ITEM) && ATTR_NAME.equals(attributeName)) {

            // Special case: the user is code completing inside
            //    <style><item name="^"/></style>
            // In this case, ALL attributes are valid so we need to synthesize
            // a choice list from all the layout descriptors

            // Add in android: as a completion item?
            if (startsWith(ANDROID_NS_NAME_PREFIX, wordPrefix)) {
                proposals.add(new CompletionProposal(ANDROID_NS_NAME_PREFIX,
                        offset - wordPrefix.length(), // replacementOffset
                        wordPrefix.length() + replaceLength, // replacementLength
                        ANDROID_NS_NAME_PREFIX.length(), // cursorPosition
                        IconFactory.getInstance().getIcon(ATTRIBUTE_ICON_FILENAME),
                        null, null, null));
            }


            String attributePrefix = wordPrefix;
            if (startsWith(attributePrefix, ANDROID_NS_NAME_PREFIX)) {
                attributePrefix = attributePrefix.substring(ANDROID_NS_NAME_PREFIX.length());
            }

            AndroidTargetData data = mEditor.getTargetData();
            if (data != null) {
                IDescriptorProvider descriptorProvider =
                    data.getDescriptorProvider(
                            AndroidTargetData.DESCRIPTOR_LAYOUT);
                if (descriptorProvider != null) {
                    ElementDescriptor[] rootElementDescriptors =
                        descriptorProvider.getRootElementDescriptors();
                    Map<String, AttributeDescriptor> matches =
                        new HashMap<String, AttributeDescriptor>(180);
                    for (ElementDescriptor elementDesc : rootElementDescriptors) {
                        for (AttributeDescriptor desc : elementDesc.getAttributes()) {
                            if (desc instanceof SeparatorAttributeDescriptor) {
                                continue;
                            }
                            String name = desc.getXmlLocalName();
                            if (startsWith(name, attributePrefix)) {
                                matches.put(name, desc);
                            }
                        }
                    }

                    List<AttributeDescriptor> sorted =
                        new ArrayList<AttributeDescriptor>(matches.size());
                    sorted.addAll(matches.values());
                    Collections.sort(sorted);
                    char needTag = 0;
                    addMatchingProposals(proposals, sorted.toArray(), offset, node, wordPrefix,
                            needTag, true /* isAttribute */, false /* isNew */,
                            skipEndTag /* skipEndTag */, replaceLength);
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    protected void computeTextValues(List<ICompletionProposal> proposals, int offset,
            Node parentNode, Node currentNode, UiElementNode uiParent,
            String prefix) {
        super.computeTextValues(proposals, offset, parentNode, currentNode, uiParent,
                prefix);

        if (parentNode.getNodeName().equals(TAG_ITEM) &&
            parentNode.getParentNode() != null &&
            TAG_STYLE.equals(parentNode.getParentNode().getNodeName())) {

            // Special case: the user is code completing inside
            //    <style><item name="android:foo"/>|</style>
            // In this case, we need to find the right AttributeDescriptor
            // for the given attribute and offer its values

            AndroidTargetData data = mEditor.getTargetData();
            if (data != null) {
                IDescriptorProvider descriptorProvider =
                    data.getDescriptorProvider(DESCRIPTOR_LAYOUT);
                if (descriptorProvider != null) {

                    Element element = (Element) parentNode;
                    String attrName = element.getAttribute(ATTR_NAME);
                    int pos = attrName.indexOf(':');
                    if (pos >= 0) {
                        attrName = attrName.substring(pos + 1);
                    }

                    // Search for an attribute match
                    ElementDescriptor[] rootElementDescriptors =
                        descriptorProvider.getRootElementDescriptors();
                    for (ElementDescriptor elementDesc : rootElementDescriptors) {
                        for (AttributeDescriptor desc : elementDesc.getAttributes()) {
                            if (desc.getXmlLocalName().equals(attrName)) {
                                // Make a ui parent node such that we can attach our
                                // newfound attribute node to something (the code we delegate
                                // to for looking up attribute completions will look at the
                                // parent node and ask for its editor etc.)
                                if (uiParent == null) {
                                    DocumentDescriptor documentDescriptor =
                                        data.getLayoutDescriptors().getDescriptor();
                                    uiParent = documentDescriptor.createUiNode();
                                    uiParent.setEditor(mEditor);
                                }

                                UiAttributeNode currAttrNode = desc.createUiNode(uiParent);
                                AttribInfo attrInfo = new AttribInfo();
                                Object[] values = getAttributeValueChoices(currAttrNode, attrInfo,
                                        prefix);
                                char needTag = attrInfo.needTag;
                                if (attrInfo.correctedPrefix != null) {
                                    prefix = attrInfo.correctedPrefix;
                                }
                                boolean isAttribute = true;
                                boolean isNew = false;
                                int replaceLength = computeTextReplaceLength(currentNode, offset);
                                addMatchingProposals(proposals, values, offset, currentNode,
                                        prefix, needTag, isAttribute, isNew,
                                        false /* skipEndTag */, replaceLength);
                                return;
                            }
                        }
                    }
                }
            }
        }

        if (parentNode.getNodeName().equals(TAG_ITEM)) {
            // Completing text content inside an <item> tag: offer @resource completion.
            if (prefix.startsWith(PREFIX_RESOURCE_REF) || prefix.trim().length() == 0) {
                String[] choices = UiResourceAttributeNode.computeResourceStringMatches(
                        mEditor, null /*attributeDescriptor*/, prefix);
                if (choices == null || choices.length == 0) {
                    return;
                }

                // If the parent item tag specifies a type, filter the results
                Node typeNode = parentNode.getAttributes().getNamedItem(ATTR_TYPE);
                if (typeNode != null) {
                    String value = typeNode.getNodeValue();
                    List<String> filtered = new ArrayList<String>();
                    for (String s : choices) {
                        if (s.startsWith(ANDROID_PREFIX) ||
                                s.startsWith(PREFIX_RESOURCE_REF+ value)) {
                            filtered.add(s);
                        }
                    }
                    if (filtered.size() > 0) {
                        choices = filtered.toArray(new String[filtered.size()]);
                    }
                }

                int replaceLength = computeTextReplaceLength(currentNode, offset);
                addMatchingProposals(proposals, choices, offset, currentNode,
                        prefix, (char) 0 /*needTag*/, true /* isAttribute */, false /*isNew*/,
                        false /* skipEndTag*/,
                        replaceLength);

            }
        }
    }
}

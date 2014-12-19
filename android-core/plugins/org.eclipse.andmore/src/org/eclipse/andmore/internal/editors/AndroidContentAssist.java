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

package org.eclipse.andmore.internal.editors;

import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_LAYOUT_RESOURCE_PREFIX;
import static com.android.SdkConstants.PREFIX_ANDROID;
import static com.android.SdkConstants.PREFIX_RESOURCE_REF;
import static com.android.SdkConstants.PREFIX_THEME_REF;
import static com.android.SdkConstants.UNIT_DP;
import static com.android.SdkConstants.UNIT_IN;
import static com.android.SdkConstants.UNIT_MM;
import static com.android.SdkConstants.UNIT_PT;
import static com.android.SdkConstants.UNIT_PX;
import static com.android.SdkConstants.UNIT_SP;
import static org.eclipse.andmore.internal.editors.descriptors.AttributeDescriptor.ATTRIBUTE_ICON_FILENAME;

import com.android.ide.common.api.IAttributeInfo;
import com.android.ide.common.api.IAttributeInfo.Format;
import com.android.utils.Pair;
import com.android.utils.XmlUtils;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.editors.descriptors.AttributeDescriptor;
import org.eclipse.andmore.internal.editors.descriptors.ElementDescriptor;
import org.eclipse.andmore.internal.editors.descriptors.IDescriptorProvider;
import org.eclipse.andmore.internal.editors.descriptors.SeparatorAttributeDescriptor;
import org.eclipse.andmore.internal.editors.descriptors.TextValueDescriptor;
import org.eclipse.andmore.internal.editors.layout.gle2.DomUtilities;
import org.eclipse.andmore.internal.editors.uimodel.UiAttributeNode;
import org.eclipse.andmore.internal.editors.uimodel.UiElementNode;
import org.eclipse.andmore.internal.editors.uimodel.UiFlagAttributeNode;
import org.eclipse.andmore.internal.editors.uimodel.UiResourceAttributeNode;
import org.eclipse.andmore.internal.sdk.AndroidTargetData;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Content Assist Processor for Android XML files
 * <p>
 * Remaining corner cases:
 * <ul>
 * <li>Completion does not work right if there is a space between the = and the opening
 *   quote.
 * <li>Replacement completion does not work right if the caret is to the left of the
 *   opening quote, where the opening quote is a single quote, and the replacement items use
 *   double quotes.
 * </ul>
 */
@SuppressWarnings("restriction") // XML model
public abstract class AndroidContentAssist implements IContentAssistProcessor {

    /** Regexp to detect a full attribute after an element tag.
     * <pre>Syntax:
     *    name = "..." quoted string with all but < and "
     * or:
     *    name = '...' quoted string with all but < and '
     * </pre>
     */
    private static Pattern sFirstAttribute = Pattern.compile(
            "^ *[a-zA-Z_:]+ *= *(?:\"[^<\"]*\"|'[^<']*')");  //$NON-NLS-1$

    /** Regexp to detect an element tag name */
    private static Pattern sFirstElementWord = Pattern.compile("^[a-zA-Z0-9_:.-]+"); //$NON-NLS-1$

    /** Regexp to detect whitespace */
    private static Pattern sWhitespace = Pattern.compile("\\s+"); //$NON-NLS-1$

    protected final static String ROOT_ELEMENT = "";

    /** Descriptor of the root of the XML hierarchy. This a "fake" ElementDescriptor which
     *  is used to list all the possible roots given by actual implementations.
     *  DO NOT USE DIRECTLY. Call {@link #getRootDescriptor()} instead. */
    private ElementDescriptor mRootDescriptor;

    private final int mDescriptorId;

    protected AndroidXmlEditor mEditor;

    /**
     * Constructor for AndroidContentAssist
     * @param descriptorId An id for {@link AndroidTargetData#getDescriptorProvider(int)}.
     *      The Id can be one of {@link AndroidTargetData#DESCRIPTOR_MANIFEST},
     *      {@link AndroidTargetData#DESCRIPTOR_LAYOUT},
     *      {@link AndroidTargetData#DESCRIPTOR_MENU},
     *      or {@link AndroidTargetData#DESCRIPTOR_OTHER_XML}.
     *      All other values will throw an {@link IllegalArgumentException} later at runtime.
     */
    public AndroidContentAssist(int descriptorId) {
        mDescriptorId = descriptorId;
    }

    /**
     * Returns a list of completion proposals based on the
     * specified location within the document that corresponds
     * to the current cursor position within the text viewer.
     *
     * @param viewer the viewer whose document is used to compute the proposals
     * @param offset an offset within the document for which completions should be computed
     * @return an array of completion proposals or <code>null</code> if no proposals are possible
     *
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeCompletionProposals(org.eclipse.jface.text.ITextViewer, int)
     */
    @Override
    public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
        String wordPrefix = extractElementPrefix(viewer, offset);

        if (mEditor == null) {
            mEditor = AndroidXmlEditor.fromTextViewer(viewer);
            if (mEditor == null) {
                // This should not happen. Duck and forget.
                AdtPlugin.log(IStatus.ERROR, "Editor not found during completion");
                return null;
            }
        }

        // List of proposals, in the order presented to the user.
        List<ICompletionProposal> proposals = new ArrayList<ICompletionProposal>(80);

        // Look up the caret context - where in an element, or between elements, or
        // within an element's children, is the given caret offset located?
        Pair<Node, Node> context = DomUtilities.getNodeContext(viewer.getDocument(), offset);
        if (context == null) {
            return null;
        }
        Node parentNode = context.getFirst();
        Node currentNode = context.getSecond();
        assert parentNode != null || currentNode != null;

        UiElementNode rootUiNode = mEditor.getUiRootNode();
        if (currentNode == null || currentNode.getNodeType() == Node.TEXT_NODE) {
             UiElementNode parentUiNode =
                 rootUiNode == null ? null : rootUiNode.findXmlNode(parentNode);
             computeTextValues(proposals, offset, parentNode, currentNode, parentUiNode,
                    wordPrefix);
        } else if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
            String parent = currentNode.getNodeName();
            AttribInfo info = parseAttributeInfo(viewer, offset, offset - wordPrefix.length());
            char nextChar = extractChar(viewer, offset);
            if (info != null) {
                // check to see if we can find a UiElementNode matching this XML node
                UiElementNode currentUiNode = rootUiNode == null
                    ? null : rootUiNode.findXmlNode(currentNode);
                computeAttributeProposals(proposals, viewer, offset, wordPrefix, currentUiNode,
                        parentNode, currentNode, parent, info, nextChar);
            } else {
                computeNonAttributeProposals(viewer, offset, wordPrefix, proposals, parentNode,
                        currentNode, parent, nextChar);
            }
        }

        return proposals.toArray(new ICompletionProposal[proposals.size()]);
    }

    private void computeNonAttributeProposals(ITextViewer viewer, int offset, String wordPrefix,
            List<ICompletionProposal> proposals, Node parentNode, Node currentNode, String parent,
            char nextChar) {
        if (startsWith(parent, wordPrefix)) {
            // We are still editing the element's tag name, not the attributes
            // (the element's tag name may not even be complete)

            Object[] choices = getChoicesForElement(parent, currentNode);
            if (choices == null || choices.length == 0) {
                return;
            }

            int replaceLength = parent.length() - wordPrefix.length();
            boolean isNew = replaceLength == 0 && nextNonspaceChar(viewer, offset) == '<';
            // Special case: if we are right before the beginning of a new
            // element, wipe out the replace length such that we insert before it,
            // we don't edit the current element.
            if (wordPrefix.length() == 0 && nextChar == '<') {
                replaceLength = 0;
                isNew = true;
            }

            // If we found some suggestions, do we need to add an opening "<" bracket
            // for the element? We don't if the cursor is right after "<" or "</".
            // Per XML Spec, there's no whitespace between "<" or "</" and the tag name.
            char needTag = computeElementNeedTag(viewer, offset, wordPrefix);

            addMatchingProposals(proposals, choices, offset,
                    parentNode != null ? parentNode : null, wordPrefix, needTag,
                    false /* isAttribute */, isNew, false /*isComplete*/,
                    replaceLength);
        }
    }

    private void computeAttributeProposals(List<ICompletionProposal> proposals, ITextViewer viewer,
            int offset, String wordPrefix, UiElementNode currentUiNode, Node parentNode,
            Node currentNode, String parent, AttribInfo info, char nextChar) {
        // We're editing attributes in an element node (either the attributes' names
        // or their values).

        if (info.isInValue) {
            if (computeAttributeValues(proposals, offset, parent, info.name, currentNode,
                    wordPrefix, info.skipEndTag, info.replaceLength)) {
                return;
            }
        }

        // Look up attribute proposals based on descriptors
        Object[] choices = getChoicesForAttribute(parent, currentNode, currentUiNode,
                info, wordPrefix);
        if (choices == null || choices.length == 0) {
            return;
        }

        int replaceLength = info.replaceLength;
        if (info.correctedPrefix != null) {
            wordPrefix = info.correctedPrefix;
        }
        char needTag = info.needTag;
        // Look to the right and see if we're followed by whitespace
        boolean isNew = replaceLength == 0
            && (Character.isWhitespace(nextChar) || nextChar == '>' || nextChar == '/');

        addMatchingProposals(proposals, choices, offset, parentNode != null ? parentNode : null,
                wordPrefix, needTag, true /* isAttribute */, isNew, info.skipEndTag,
                replaceLength);
    }

    private char computeElementNeedTag(ITextViewer viewer, int offset, String wordPrefix) {
        char needTag = 0;
        int offset2 = offset - wordPrefix.length() - 1;
        char c1 = extractChar(viewer, offset2);
        if (!((c1 == '<') || (c1 == '/' && extractChar(viewer, offset2 - 1) == '<'))) {
            needTag = '<';
        }
        return needTag;
    }

    protected int computeTextReplaceLength(Node currentNode, int offset) {
        if (currentNode == null) {
            return 0;
        }

        assert currentNode != null && currentNode.getNodeType() == Node.TEXT_NODE;

        String nodeValue = currentNode.getNodeValue();
        int relativeOffset = offset - ((IndexedRegion) currentNode).getStartOffset();
        int lineEnd = nodeValue.indexOf('\n', relativeOffset);
        if (lineEnd == -1) {
            lineEnd = nodeValue.length();
        }
        return lineEnd - relativeOffset;
    }

    /**
     * Gets the choices when the user is editing the name of an XML element.
     * <p/>
     * The user is editing the name of an element (the "parent").
     * Find the grand-parent and if one is found, return its children element list.
     * The name which is being edited should be one of those.
     * <p/>
     * Example: <manifest><applic*cursor* => returns the list of all elements that
     * can be found under <manifest>, of which <application> is one of the choices.
     *
     * @return an ElementDescriptor[] or null if no valid element was found.
     */
    protected Object[] getChoicesForElement(String parent, Node currentNode) {
        ElementDescriptor grandparent = null;
        if (currentNode.getParentNode().getNodeType() == Node.ELEMENT_NODE) {
            grandparent = getDescriptor(currentNode.getParentNode().getNodeName());
        } else if (currentNode.getParentNode().getNodeType() == Node.DOCUMENT_NODE) {
            grandparent = getRootDescriptor();
        }
        if (grandparent != null) {
            for (ElementDescriptor e : grandparent.getChildren()) {
                if (e.getXmlName().startsWith(parent)) {
                    return sort(grandparent.getChildren());
                }
            }
        }

        return null;
    }

    /** Non-destructively sort a list of ElementDescriptors and return the result */
    protected static ElementDescriptor[] sort(ElementDescriptor[] elements) {
        if (elements != null && elements.length > 1) {
            // Sort alphabetically. Must make copy to not destroy original.
            ElementDescriptor[] copy = new ElementDescriptor[elements.length];
            System.arraycopy(elements, 0, copy, 0, elements.length);

            Arrays.sort(copy, new Comparator<ElementDescriptor>() {
                @Override
                public int compare(ElementDescriptor e1, ElementDescriptor e2) {
                    return e1.getXmlLocalName().compareTo(e2.getXmlLocalName());
                }
            });

            return copy;
        }

        return elements;
    }

    /**
     * Gets the choices when the user is editing an XML attribute.
     * <p/>
     * In input, attrInfo contains details on the analyzed context, namely whether the
     * user is editing an attribute value (isInValue) or an attribute name.
     * <p/>
     * In output, attrInfo also contains two possible new values (this is a hack to circumvent
     * the lack of out-parameters in Java):
     * - AttribInfo.correctedPrefix if the user has been editing an attribute value and it has
     *   been detected that what the user typed is different from what extractElementPrefix()
     *   predicted. This happens because extractElementPrefix() stops when a character that
     *   cannot be an element name appears whereas parseAttributeInfo() uses a grammar more
     *   lenient as suitable for attribute values.
     * - AttribInfo.needTag will be non-zero if we find that the attribute completion proposal
     *   must be double-quoted.
     * @param currentUiNode
     *
     * @return an AttributeDescriptor[] if the user is editing an attribute name.
     *         a String[] if the user is editing an attribute value with some known values,
     *         or null if nothing is known about the context.
     */
    private Object[] getChoicesForAttribute(
            String parent, Node currentNode, UiElementNode currentUiNode, AttribInfo attrInfo,
            String wordPrefix) {
        Object[] choices = null;
        if (attrInfo.isInValue) {
            // Editing an attribute's value... Get the attribute name and then the
            // possible choices for the tuple(parent,attribute)
            String value = attrInfo.valuePrefix;
            if (value.startsWith("'") || value.startsWith("\"")) {   //$NON-NLS-1$   //$NON-NLS-2$
                value = value.substring(1);
                // The prefix that was found at the beginning only scan for characters
                // valid for tag name. We now know the real prefix for this attribute's
                // value, which is needed to generate the completion choices below.
                attrInfo.correctedPrefix = value;
            } else {
                attrInfo.needTag = '"';
            }

            if (currentUiNode != null) {
                // look for an UI attribute matching the current attribute name
                String attrName = attrInfo.name;
                // remove any namespace prefix from the attribute name
                int pos = attrName.indexOf(':');
                if (pos >= 0) {
                    attrName = attrName.substring(pos + 1);
                }

                UiAttributeNode currAttrNode = null;
                for (UiAttributeNode attrNode : currentUiNode.getAllUiAttributes()) {
                    if (attrNode.getDescriptor().getXmlLocalName().equals(attrName)) {
                        currAttrNode = attrNode;
                        break;
                    }
                }

                if (currAttrNode != null) {
                    choices = getAttributeValueChoices(currAttrNode, attrInfo, value);
                }
            }

            if (choices == null) {
                // fallback on the older descriptor-only based lookup.

                // in order to properly handle the special case of the name attribute in
                // the action tag, we need the grandparent of the action node, to know
                // what type of actions we need.
                // e.g. activity -> intent-filter -> action[@name]
                String greatGrandParentName = null;
                Node grandParent = currentNode.getParentNode();
                if (grandParent != null) {
                    Node greatGrandParent = grandParent.getParentNode();
                    if (greatGrandParent != null) {
                        greatGrandParentName = greatGrandParent.getLocalName();
                    }
                }

                AndroidTargetData data = mEditor.getTargetData();
                if (data != null) {
                    choices = data.getAttributeValues(parent, attrInfo.name, greatGrandParentName);
                }
            }
        } else {
            // Editing an attribute's name... Get attributes valid for the parent node.
            if (currentUiNode != null) {
                choices = currentUiNode.getAttributeDescriptors();
            } else {
                ElementDescriptor parentDesc = getDescriptor(parent);
                if (parentDesc != null) {
                    choices = parentDesc.getAttributes();
                }
            }
        }
        return choices;
    }

    protected Object[] getAttributeValueChoices(UiAttributeNode currAttrNode, AttribInfo attrInfo,
            String value) {
        Object[] choices;
        int pos;
        choices = currAttrNode.getPossibleValues(value);
        if (choices != null && currAttrNode instanceof UiResourceAttributeNode) {
            attrInfo.skipEndTag = false;
        }

        if (currAttrNode instanceof UiFlagAttributeNode) {
            // A "flag" can consist of several values separated by "or" (|).
            // If the correct prefix contains such a pipe character, we change
            // it so that only the currently edited value is completed.
            pos = value.lastIndexOf('|');
            if (pos >= 0) {
                attrInfo.correctedPrefix = value = value.substring(pos + 1);
                attrInfo.needTag = 0;
            }

            attrInfo.skipEndTag = false;
        }

        // Should we do suffix completion on dimension units etc?
        choices = completeSuffix(choices, value, currAttrNode);

        // Check to see if the user is attempting resource completion
        AttributeDescriptor attributeDescriptor = currAttrNode.getDescriptor();
        IAttributeInfo attributeInfo = attributeDescriptor.getAttributeInfo();
        if (value.startsWith(PREFIX_RESOURCE_REF)
                && !attributeInfo.getFormats().contains(Format.REFERENCE)) {
            // Special case: If the attribute value looks like a reference to a
            // resource, offer to complete it, since in many cases our metadata
            // does not correctly state whether a resource value is allowed. We don't
            // offer these for an empty completion context, but if the user has
            // actually typed "@", in that case list resource matches.
            // For example, for android:minHeight this makes completion on @dimen/
            // possible.
            choices = UiResourceAttributeNode.computeResourceStringMatches(
                    mEditor, attributeDescriptor, value);
            attrInfo.skipEndTag = false;
        } else if (value.startsWith(PREFIX_THEME_REF)
                && !attributeInfo.getFormats().contains(Format.REFERENCE)) {
            choices = UiResourceAttributeNode.computeResourceStringMatches(
                    mEditor, attributeDescriptor, value);
            attrInfo.skipEndTag = false;
        }

        return choices;
    }

    /**
     * Compute attribute values. Return true if the complete set of values was
     * added, so addition descriptor information should not be added.
     */
    protected boolean computeAttributeValues(List<ICompletionProposal> proposals, int offset,
            String parentTagName, String attributeName, Node node, String wordPrefix,
            boolean skipEndTag, int replaceLength) {
        return false;
    }

    protected void computeTextValues(List<ICompletionProposal> proposals, int offset,
            Node parentNode, Node currentNode, UiElementNode uiParent,
            String wordPrefix) {

       if (parentNode != null) {
           // Examine the parent of the text node.
           Object[] choices = getElementChoicesForTextNode(parentNode);
           if (choices != null && choices.length > 0) {
               ISourceViewer viewer = mEditor.getStructuredSourceViewer();
               char needTag = computeElementNeedTag(viewer, offset, wordPrefix);

               int replaceLength = 0;
               addMatchingProposals(proposals, choices,
                       offset, parentNode, wordPrefix, needTag,
                               false /* isAttribute */,
                               false /*isNew*/,
                               false /*isComplete*/,
                               replaceLength);
           }
       }
    }

    /**
     * Gets the choices when the user is editing an XML text node.
     * <p/>
     * This means the user is editing outside of any XML element or attribute.
     * Simply return the list of XML elements that can be present there, based on the
     * parent of the current node.
     *
     * @return An ElementDescriptor[] or null.
     */
    protected ElementDescriptor[] getElementChoicesForTextNode(Node parentNode) {
        ElementDescriptor[] choices = null;
        String parent;
        if (parentNode.getNodeType() == Node.ELEMENT_NODE) {
            // We're editing a text node which parent is an element node. Limit
            // content assist to elements valid for the parent.
            parent = parentNode.getNodeName();
            ElementDescriptor desc = getDescriptor(parent);
            if (desc == null && parent.indexOf('.') != -1) {
                // The parent is a custom view and we don't have metadata about its
                // allowable children, so just assume any normal layout tag is
                // legal
                desc = mRootDescriptor;
            }

            if (desc != null) {
                choices = sort(desc.getChildren());
            }
        } else if (parentNode.getNodeType() == Node.DOCUMENT_NODE) {
            // We're editing a text node at the first level (i.e. root node).
            // Limit content assist to the only valid root elements.
            choices = sort(getRootDescriptor().getChildren());
        }

        return choices;
    }

     /**
     * Given a list of choices, adds in any that match the current prefix into the
     * proposals list.
     * <p/>
     * Choices is an object array. Items of the array can be:
     * - ElementDescriptor: a possible element descriptor which XML name should be completed.
     * - AttributeDescriptor: a possible attribute descriptor which XML name should be completed.
     * - String: string values to display as-is to the user. Typically those are possible
     *           values for a given attribute.
     * - Pair of Strings: the first value is the keyword to insert, and the second value
     *           is the tooltip/help for the value to be displayed in the documentation popup.
     */
    protected void addMatchingProposals(List<ICompletionProposal> proposals, Object[] choices,
            int offset, Node currentNode, String wordPrefix, char needTag,
            boolean isAttribute, boolean isNew, boolean skipEndTag, int replaceLength) {
        if (choices == null) {
            return;
        }

        Map<String, String> nsUriMap = new HashMap<String, String>();
        boolean haveLayoutParams = false;

        for (Object choice : choices) {
            String keyword = null;
            String nsPrefix = null;
            String nsUri = null;
            Image icon = null;
            String tooltip = null;
            if (choice instanceof ElementDescriptor) {
                keyword = ((ElementDescriptor)choice).getXmlName();
                icon    = ((ElementDescriptor)choice).getGenericIcon();
                // Tooltip computed lazily in {@link CompletionProposal}
            } else if (choice instanceof TextValueDescriptor) {
                continue; // Value nodes are not part of the completion choices
            } else if (choice instanceof SeparatorAttributeDescriptor) {
                continue; // not real attribute descriptors
            } else if (choice instanceof AttributeDescriptor) {
                keyword = ((AttributeDescriptor)choice).getXmlLocalName();
                icon    = ((AttributeDescriptor)choice).getGenericIcon();
                // Tooltip computed lazily in {@link CompletionProposal}

                // Get the namespace URI for the attribute. Note that some attributes
                // do not have a namespace and thus return null here.
                nsUri = ((AttributeDescriptor)choice).getNamespaceUri();
                if (nsUri != null) {
                    nsPrefix = nsUriMap.get(nsUri);
                    if (nsPrefix == null) {
                        nsPrefix = XmlUtils.lookupNamespacePrefix(currentNode, nsUri, false);
                        nsUriMap.put(nsUri, nsPrefix);
                    }
                }
                if (nsPrefix != null) {
                    nsPrefix += ":"; //$NON-NLS-1$
                }

            } else if (choice instanceof String) {
                keyword = (String) choice;
                if (isAttribute) {
                    icon = IconFactory.getInstance().getIcon(ATTRIBUTE_ICON_FILENAME);
                }
            } else if (choice instanceof Pair<?, ?>) {
                @SuppressWarnings("unchecked")
                Pair<String, String> pair = (Pair<String, String>) choice;
                keyword = pair.getFirst();
                tooltip = pair.getSecond();
                if (isAttribute) {
                    icon = IconFactory.getInstance().getIcon(ATTRIBUTE_ICON_FILENAME);
                }
            } else if (choice instanceof IType) {
                IType type = (IType) choice;
                keyword = type.getFullyQualifiedName();
                icon = JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_CUNIT);
            } else {
                continue; // discard unknown choice
            }

            String nsKeyword = nsPrefix == null ? keyword : (nsPrefix + keyword);

            if (nameStartsWith(nsKeyword, wordPrefix, nsPrefix)) {
                keyword = nsKeyword;
                String endTag = ""; //$NON-NLS-1$
                if (needTag != 0) {
                    if (needTag == '"') {
                        keyword = needTag + keyword;
                        endTag = String.valueOf(needTag);
                    } else if (needTag == '<') {
                        if (elementCanHaveChildren(choice)) {
                            endTag = String.format("></%1$s>", keyword);  //$NON-NLS-1$
                        } else {
                            endTag = "/>";  //$NON-NLS-1$
                        }
                        keyword = needTag + keyword + ' ';
                    } else if (needTag == ' ') {
                        keyword = needTag + keyword;
                    }
                } else if (!isAttribute && isNew) {
                    if (elementCanHaveChildren(choice)) {
                        endTag = String.format("></%1$s>", keyword);  //$NON-NLS-1$
                    } else {
                        endTag = "/>";  //$NON-NLS-1$
                    }
                    keyword = keyword + ' ';
                }

                final String suffix;
                int cursorPosition;
                final String displayString;
                if (choice instanceof AttributeDescriptor && isNew) {
                    // Special case for attributes: insert ="" stuff and locate caret inside ""
                    suffix = "=\"\""; //$NON-NLS-1$
                    cursorPosition = keyword.length() + suffix.length() - 1;
                    displayString = keyword + endTag; // don't include suffix;
                } else {
                    suffix = endTag;
                    cursorPosition = keyword.length();
                    displayString = null;
                }

                if (skipEndTag) {
                    assert isAttribute;
                    cursorPosition++;
                }

                if (nsPrefix != null &&
                        keyword.startsWith(ATTR_LAYOUT_RESOURCE_PREFIX, nsPrefix.length())) {
                    haveLayoutParams = true;
                }

                // For attributes, automatically insert ns:attribute="" and place the cursor
                // inside the quotes.
                // Special case for attributes: insert ="" stuff and locate caret inside ""
                proposals.add(new CompletionProposal(
                    this,
                    choice,
                    keyword + suffix,                   // String replacementString
                    offset - wordPrefix.length(),       // int replacementOffset
                    wordPrefix.length() + replaceLength,// int replacementLength
                    cursorPosition,                     // cursorPosition
                    icon,                               // Image image
                    displayString,                      // displayString
                    null,                               // IContextInformation contextInformation
                    tooltip,                            // String additionalProposalInfo
                    nsPrefix,
                    nsUri
                ));
            }
        }

        if (wordPrefix.length() > 0 && haveLayoutParams
                && !wordPrefix.startsWith(ATTR_LAYOUT_RESOURCE_PREFIX)) {
            // Sort layout parameters to the front if we automatically inserted some
            // that you didn't request. For example, you typed "width" and we match both
            // "width" and "layout_width" - should match layout_width.
            String nsPrefix = nsUriMap.get(ANDROID_URI);
            if (nsPrefix == null) {
                nsPrefix = PREFIX_ANDROID;
            } else {
                nsPrefix += ':';
            }
            if (!(wordPrefix.startsWith(nsPrefix)
                    && wordPrefix.startsWith(ATTR_LAYOUT_RESOURCE_PREFIX, nsPrefix.length()))) {
                int nextLayoutIndex = 0;
                for (int i = 0, n = proposals.size(); i < n; i++) {
                    ICompletionProposal proposal = proposals.get(i);
                    String keyword = proposal.getDisplayString();
                    if (keyword.startsWith(nsPrefix) &&
                            keyword.startsWith(ATTR_LAYOUT_RESOURCE_PREFIX, nsPrefix.length())
                            && i != nextLayoutIndex) {
                        // Swap to front
                        ICompletionProposal temp = proposals.get(nextLayoutIndex);
                        proposals.set(nextLayoutIndex, proposal);
                        proposals.set(i, temp);
                        nextLayoutIndex++;
                    }
                }
            }
        }
    }

    /**
     * Returns true if the given word starts with the given prefix. The comparison is not
     * case sensitive.
     *
     * @param word the word to test
     * @param prefix the prefix the word should start with
     * @return true if the given word starts with the given prefix
     */
    public static boolean startsWith(String word, String prefix) {
        int prefixLength = prefix.length();
        int wordLength = word.length();
        if (wordLength < prefixLength) {
            return false;
        }

        for (int i = 0; i < prefixLength; i++) {
            if (Character.toLowerCase(prefix.charAt(i))
                    != Character.toLowerCase(word.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    /** @return the editor associated with this content assist */
    AndroidXmlEditor getEditor() {
        return mEditor;
    }

    /**
     * This method performs a prefix match for the given word and prefix, with a couple of
     * Android code completion specific twists:
     * <ol>
     * <li> The match is not case sensitive, so {word="fOo",prefix="FoO"} is a match.
     * <li>If the word to be matched has a namespace prefix, the typed prefix doesn't have
     * to match it. So {word="android:foo", prefix="foo"} is a match.
     * <li>If the attribute name part starts with "layout_" it can be omitted. So
     * {word="android:layout_marginTop",prefix="margin"} is a match, as is
     * {word="android:layout_marginTop",prefix="android:margin"}.
     * </ol>
     *
     * @param word the full word to be matched, including namespace if any
     * @param prefix the prefix to check
     * @param nsPrefix the namespace prefix (android: or local definition of android
     *            namespace prefix)
     * @return true if the prefix matches for code completion
     */
    public static boolean nameStartsWith(String word, String prefix, String nsPrefix) {
        if (nsPrefix == null) {
            nsPrefix = ""; //$NON-NLS-1$
        }

        int wordStart = nsPrefix.length();
        int prefixStart = 0;

        if (startsWith(prefix, nsPrefix)) {
            // Already matches up through the namespace prefix:
            prefixStart = wordStart;
        } else if (startsWith(nsPrefix, prefix)) {
            return true;
        }

        int prefixLength = prefix.length();
        int wordLength = word.length();

        if (wordLength - wordStart < prefixLength - prefixStart) {
            return false;
        }

        boolean matches = true;
        for (int i = prefixStart, j = wordStart; i < prefixLength; i++, j++) {
            char c1 = Character.toLowerCase(prefix.charAt(i));
            char c2 = Character.toLowerCase(word.charAt(j));
            if (c1 != c2) {
                matches = false;
                break;
            }
        }

        if (!matches && word.startsWith(ATTR_LAYOUT_RESOURCE_PREFIX, wordStart)
                && !prefix.startsWith(ATTR_LAYOUT_RESOURCE_PREFIX, prefixStart)) {
            wordStart += ATTR_LAYOUT_RESOURCE_PREFIX.length();

            if (wordLength - wordStart < prefixLength - prefixStart) {
                return false;
            }

            for (int i = prefixStart, j = wordStart; i < prefixLength; i++, j++) {
                char c1 = Character.toLowerCase(prefix.charAt(i));
                char c2 = Character.toLowerCase(word.charAt(j));
                if (c1 != c2) {
                    return false;
                }
            }

            return true;
        }

        return matches;
    }

    /**
     * Indicates whether this descriptor describes an element that can potentially
     * have children (either sub-elements or text value). If an element can have children,
     * we want to explicitly write an opening and a separate closing tag.
     * <p/>
     * Elements can have children if the descriptor has children element descriptors
     * or if one of the attributes is a TextValueDescriptor.
     *
     * @param descriptor An ElementDescriptor or an AttributeDescriptor
     * @return True if the descriptor is an ElementDescriptor that can have children or a text
     *         value
     */
    private boolean elementCanHaveChildren(Object descriptor) {
        if (descriptor instanceof ElementDescriptor) {
            ElementDescriptor desc = (ElementDescriptor) descriptor;
            if (desc.hasChildren()) {
                return true;
            }
            for (AttributeDescriptor attrDesc : desc.getAttributes()) {
                if (attrDesc instanceof TextValueDescriptor) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns the element descriptor matching a given XML node name or null if it can't be
     * found.
     * <p/>
     * This is simplistic; ideally we should consider the parent's chain to make sure we
     * can differentiate between different hierarchy trees. Right now the first match found
     * is returned.
     */
    private ElementDescriptor getDescriptor(String nodeName) {
        return getRootDescriptor().findChildrenDescriptor(nodeName, true /* recursive */);
    }

    @Override
    public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
        return null;
    }

    /**
     * Returns the characters which when entered by the user should
     * automatically trigger the presentation of possible completions.
     *
     * In our case, we auto-activate on opening tags and attributes namespace.
     *
     * @return the auto activation characters for completion proposal or <code>null</code>
     *      if no auto activation is desired
     */
    @Override
    public char[] getCompletionProposalAutoActivationCharacters() {
        return new char[]{ '<', ':', '=' };
    }

    @Override
    public char[] getContextInformationAutoActivationCharacters() {
        return null;
    }

    @Override
    public IContextInformationValidator getContextInformationValidator() {
        return null;
    }

    @Override
    public String getErrorMessage() {
        return null;
    }

    /**
     * Heuristically extracts the prefix used for determining template relevance
     * from the viewer's document. The default implementation returns the String from
     * offset backwards that forms a potential XML element name, attribute name or
     * attribute value.
     *
     * The part were we access the document was extracted from
     * org.eclipse.jface.text.templatesTemplateCompletionProcessor and adapted to our needs.
     *
     * @param viewer the viewer
     * @param offset offset into document
     * @return the prefix to consider
     */
    protected String extractElementPrefix(ITextViewer viewer, int offset) {
        int i = offset;
        IDocument document = viewer.getDocument();
        if (i > document.getLength()) return ""; //$NON-NLS-1$

        try {
            for (; i > 0; --i) {
                char ch = document.getChar(i - 1);

                // We want all characters that can form a valid:
                // - element name, e.g. anything that is a valid Java class/variable literal.
                // - attribute name, including : for the namespace
                // - attribute value.
                // Before we were inclusive and that made the code fragile. So now we're
                // going to be exclusive: take everything till we get one of:
                // - any form of whitespace
                // - any xml separator, e.g. < > ' " and =
                if (Character.isWhitespace(ch) ||
                        ch == '<' || ch == '>' || ch == '\'' || ch == '"' || ch == '=') {
                    break;
                }
            }

            return document.get(i, offset - i);
        } catch (BadLocationException e) {
            return ""; //$NON-NLS-1$
        }
    }

    /**
     * Extracts the character at the given offset.
     * Returns 0 if the offset is invalid.
     */
    protected char extractChar(ITextViewer viewer, int offset) {
        IDocument document = viewer.getDocument();
        if (offset > document.getLength()) return 0;

        try {
            return document.getChar(offset);
        } catch (BadLocationException e) {
            return 0;
        }
    }

    /**
     * Search forward and find the first non-space character and return it. Returns 0 if no
     * such character was found.
     */
    private char nextNonspaceChar(ITextViewer viewer, int offset) {
        IDocument document = viewer.getDocument();
        int length = document.getLength();
        for (; offset < length; offset++) {
            try {
                char c = document.getChar(offset);
                if (!Character.isWhitespace(c)) {
                    return c;
                }
            } catch (BadLocationException e) {
                return 0;
            }
        }

        return 0;
    }

    /**
     * Information about the current edit of an attribute as reported by parseAttributeInfo.
     */
    protected static class AttribInfo {
        public AttribInfo() {
        }

        /** True if the cursor is located in an attribute's value, false if in an attribute name */
        public boolean isInValue = false;
        /** The attribute name. Null when not set. */
        public String name = null;
        /** The attribute value top the left of the cursor. Null when not set. The value
         * *may* start with a quote (' or "), in which case we know we don't need to quote
         * the string for the user */
        public String valuePrefix = null;
        /** String typed by the user so far (i.e. right before requesting code completion),
         *  which will be corrected if we find a possible completion for an attribute value.
         *  See the long comment in getChoicesForAttribute(). */
        public String correctedPrefix = null;
        /** Non-zero if an attribute value need a start/end tag (i.e. quotes or brackets) */
        public char needTag = 0;
        /** Number of characters to replace after the prefix */
        public int replaceLength = 0;
        /** Should the cursor advance through the end tag when inserted? */
        public boolean skipEndTag = false;
    }

    /**
     * Try to guess if the cursor is editing an element's name or an attribute following an
     * element. If it's an attribute, try to find if an attribute name is being defined or
     * its value.
     * <br/>
     * This is currently *only* called when we know the cursor is after a complete element
     * tag name, so it should never return null.
     * <br/>
     * Reference for XML syntax: http://www.w3.org/TR/2006/REC-xml-20060816/#sec-starttags
     * <br/>
     * @return An AttribInfo describing which attribute is being edited or null if the cursor is
     *         not editing an attribute (in which case it must be an element's name).
     */
    private AttribInfo parseAttributeInfo(ITextViewer viewer, int offset, int prefixStartOffset) {
        AttribInfo info = new AttribInfo();
        int originalOffset = offset;

        IDocument document = viewer.getDocument();
        int n = document.getLength();
        if (offset <= n) {
            try {
                // Look to the right to make sure we aren't sitting on the boundary of the
                // beginning of a new element with whitespace before it
                if (offset < n && document.getChar(offset) == '<') {
                    return null;
                }

                n = offset;
                for (;offset > 0; --offset) {
                    char ch = document.getChar(offset - 1);
                    if (ch == '>') break;
                    if (ch == '<') break;
                }

                // text will contain the full string of the current element,
                // i.e. whatever is after the "<" to the current cursor
                String text = document.get(offset, n - offset);

                // Normalize whitespace to single spaces
                text = sWhitespace.matcher(text).replaceAll(" "); //$NON-NLS-1$

                // Remove the leading element name. By spec, it must be after the < without
                // any whitespace. If there's nothing left, no attribute has been defined yet.
                // Be sure to keep any whitespace after the initial word if any, as it matters.
                text = sFirstElementWord.matcher(text).replaceFirst("");  //$NON-NLS-1$

                // There MUST be space after the element name. If not, the cursor is still
                // defining the element name.
                if (!text.startsWith(" ")) { //$NON-NLS-1$
                    return null;
                }

                // Remove full attributes:
                // Syntax:
                //    name = "..." quoted string with all but < and "
                // or:
                //    name = '...' quoted string with all but < and '
                String temp;
                do {
                    temp = text;
                    text = sFirstAttribute.matcher(temp).replaceFirst("");  //$NON-NLS-1$
                } while(!temp.equals(text));

                IRegion lineInfo = document.getLineInformationOfOffset(originalOffset);
                int lineStart = lineInfo.getOffset();
                String line = document.get(lineStart, lineInfo.getLength());
                int cursorColumn = originalOffset - lineStart;
                int prefixLength = originalOffset - prefixStartOffset;

                // Now we're left with 3 cases:
                // - nothing: either there is no attribute definition or the cursor located after
                //   a completed attribute definition.
                // - a string with no =: the user is writing an attribute name. This case can be
                //   merged with the previous one.
                // - string with an = sign, optionally followed by a quote (' or "): the user is
                //   writing the value of the attribute.
                int posEqual = text.indexOf('=');
                if (posEqual == -1) {
                    info.isInValue = false;
                    info.name = text.trim();

                    // info.name is currently just the prefix of the attribute name.
                    // Look at the text buffer to find the complete name (since we need
                    // to know its bounds in order to replace it when a different attribute
                    // that matches this prefix is chosen)
                    int nameStart = cursorColumn;
                    for (int nameEnd = nameStart; nameEnd < line.length(); nameEnd++) {
                        char c = line.charAt(nameEnd);
                        if (!(Character.isLetter(c) || c == ':' || c == '_')) {
                            String nameSuffix = line.substring(nameStart, nameEnd);
                            info.name = text.trim() + nameSuffix;
                            break;
                        }
                    }

                    info.replaceLength = info.name.length() - prefixLength;

                    if (info.name.length() == 0 && originalOffset > 0) {
                        // Ensure that attribute names are properly separated
                        char prevChar = extractChar(viewer, originalOffset - 1);
                        if (prevChar == '"' || prevChar == '\'') {
                            // Ensure that the attribute is properly separated from the
                            // previous element
                            info.needTag = ' ';
                        }
                    }
                    info.skipEndTag = false;
                } else {
                    info.isInValue = true;
                    info.name = text.substring(0, posEqual).trim();
                    info.valuePrefix = text.substring(posEqual + 1);

                    char quoteChar = '"'; // Does " or ' surround the XML value?
                    for (int i = posEqual + 1; i < text.length(); i++) {
                        if (!Character.isWhitespace(text.charAt(i))) {
                            quoteChar = text.charAt(i);
                            break;
                        }
                    }

                    // Must compute the complete value
                    int valueStart = cursorColumn;
                    int valueEnd = valueStart;
                    for (; valueEnd < line.length(); valueEnd++) {
                        char c = line.charAt(valueEnd);
                        if (c == quoteChar) {
                            // Make sure this isn't the *opening* quote of the value,
                            // which is the case if we invoke code completion with the
                            // caret between the = and the opening quote; in that case
                            // we consider it value completion, and offer items including
                            // the quotes, but we shouldn't bail here thinking we have found
                            // the end of the value.
                            // Look backwards to make sure we find another " before
                            // we find a =
                            boolean isFirst = false;
                            for (int j = valueEnd - 1; j >= 0; j--) {
                                char pc = line.charAt(j);
                                if (pc == '=') {
                                    isFirst = true;
                                    break;
                                } else if (pc == quoteChar) {
                                    valueStart = j;
                                    break;
                                }
                            }
                            if (!isFirst) {
                                info.skipEndTag = true;
                                break;
                            }
                        }
                    }
                    int valueEndOffset = valueEnd + lineStart;
                    info.replaceLength = valueEndOffset - (prefixStartOffset + prefixLength);
                    // Is the caret to the left of the value quote? If so, include it in
                    // the replace length.
                    int valueStartOffset = valueStart + lineStart;
                    if (valueStartOffset == prefixStartOffset && valueEnd > valueStart) {
                        info.replaceLength++;
                    }
                }
                return info;
            } catch (BadLocationException e) {
                // pass
            }
        }

        return null;
    }

    /** Returns the root descriptor id to use */
    protected int getRootDescriptorId() {
        return mDescriptorId;
    }

    /**
     * Computes (if needed) and returns the root descriptor.
     */
    protected ElementDescriptor getRootDescriptor() {
        if (mRootDescriptor == null) {
            AndroidTargetData data = mEditor.getTargetData();
            if (data != null) {
                IDescriptorProvider descriptorProvider =
                    data.getDescriptorProvider(getRootDescriptorId());

                if (descriptorProvider != null) {
                    mRootDescriptor = new ElementDescriptor("",     //$NON-NLS-1$
                            descriptorProvider.getRootElementDescriptors());
                }
            }
        }

        return mRootDescriptor;
    }

    /**
     * Fixed list of dimension units, along with user documentation, for use by
     * {@link #completeSuffix}.
     */
    private static final String[] sDimensionUnits = new String[] {
        UNIT_DP,
        "<b>Density-independent Pixels</b> - an abstract unit that is based on the physical "
                + "density of the screen.",

        UNIT_SP,
        "<b>Scale-independent Pixels</b> - this is like the dp unit, but it is also scaled by "
                + "the user's font size preference.",

        UNIT_PT,
        "<b>Points</b> - 1/72 of an inch based on the physical size of the screen.",

        UNIT_MM,
        "<b>Millimeters</b> - based on the physical size of the screen.",

        UNIT_IN,
        "<b>Inches</b> - based on the physical size of the screen.",

        UNIT_PX,
        "<b>Pixels</b> - corresponds to actual pixels on the screen. Not recommended.",
    };

    /**
     * Fixed list of fractional units, along with user documentation, for use by
     * {@link #completeSuffix}
     */
    private static final String[] sFractionUnits = new String[] {
        "%",  //$NON-NLS-1$
        "<b>Fraction</b> - a percentage of the base size",

        "%p", //$NON-NLS-1$
        "<b>Fraction</b> - a percentage relative to parent container",
    };

    /**
     * Completes suffixes for applicable types (like dimensions and fractions) such that
     * after a dimension number you get completion on unit types like "px".
     */
    private Object[] completeSuffix(Object[] choices, String value, UiAttributeNode currAttrNode) {
        IAttributeInfo attributeInfo = currAttrNode.getDescriptor().getAttributeInfo();
        EnumSet<Format> formats = attributeInfo.getFormats();
        List<Object> suffixes = new ArrayList<Object>();

        if (value.length() > 0 && Character.isDigit(value.charAt(0))) {
            boolean hasDimension = formats.contains(Format.DIMENSION);
            boolean hasFraction = formats.contains(Format.FRACTION);

            if (hasDimension || hasFraction) {
                // Split up the value into a numeric part (the prefix) and the
                // unit part (the suffix)
                int suffixBegin = 0;
                for (; suffixBegin < value.length(); suffixBegin++) {
                    if (!Character.isDigit(value.charAt(suffixBegin))) {
                        break;
                    }
                }
                String number = value.substring(0, suffixBegin);
                String suffix = value.substring(suffixBegin);

                // Add in the matching dimension and/or fraction units, if any
                if (hasDimension) {
                    // Each item has two entries in the array of strings: the first odd numbered
                    // ones are the unit names and the second even numbered ones are the
                    // corresponding descriptions.
                    for (int i = 0; i < sDimensionUnits.length; i += 2) {
                        String unit = sDimensionUnits[i];
                        if (startsWith(unit, suffix)) {
                            String description = sDimensionUnits[i + 1];
                            suffixes.add(Pair.of(number + unit, description));
                        }
                    }

                    // Allow "dip" completion but don't offer it ("dp" is preferred)
                    if (startsWith(suffix, "di") || startsWith(suffix, "dip")) { //$NON-NLS-1$ //$NON-NLS-2$
                        suffixes.add(Pair.of(number + "dip", "Alternative name for \"dp\"")); //$NON-NLS-1$
                    }
                }
                if (hasFraction) {
                    for (int i = 0; i < sFractionUnits.length; i += 2) {
                        String unit = sFractionUnits[i];
                        if (startsWith(unit, suffix)) {
                            String description = sFractionUnits[i + 1];
                            suffixes.add(Pair.of(number + unit, description));
                        }
                    }
                }
            }
        }

        boolean hasFlag = formats.contains(Format.FLAG);
        if (hasFlag) {
            boolean isDone = false;
            String[] flagValues = attributeInfo.getFlagValues();
            for (String flagValue : flagValues) {
                if (flagValue.equals(value)) {
                    isDone = true;
                    break;
                }
            }
            if (isDone) {
                // Add in all the new values with a separator of |
                String currentValue = currAttrNode.getCurrentValue();
                for (String flagValue : flagValues) {
                    if (currentValue == null || !currentValue.contains(flagValue)) {
                        suffixes.add(value + '|' + flagValue);
                    }
                }
            }
        }

        if (suffixes.size() > 0) {
            // Merge previously added choices (from attribute enums etc) with the new matches
            List<Object> all = new ArrayList<Object>();
            if (choices != null) {
                for (Object s : choices) {
                    all.add(s);
                }
            }
            all.addAll(suffixes);
            choices = all.toArray();
        }

        return choices;
    }
}

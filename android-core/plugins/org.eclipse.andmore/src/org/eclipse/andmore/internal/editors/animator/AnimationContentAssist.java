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

package org.eclipse.andmore.internal.editors.animator;

import static com.android.SdkConstants.ANDROID_NS_NAME_PREFIX;
import static com.android.SdkConstants.ANDROID_PKG;

import com.android.annotations.VisibleForTesting;
import com.android.ide.common.api.IAttributeInfo.Format;
import com.android.ide.common.resources.ResourceItem;
import com.android.ide.common.resources.ResourceRepository;
import com.android.resources.ResourceFolderType;
import com.android.resources.ResourceType;
import com.android.utils.Pair;

import org.eclipse.andmore.AdtUtils;
import org.eclipse.andmore.internal.editors.AndroidContentAssist;
import org.eclipse.andmore.internal.editors.descriptors.AttributeDescriptor;
import org.eclipse.andmore.internal.editors.descriptors.ElementDescriptor;
import org.eclipse.andmore.internal.editors.descriptors.IDescriptorProvider;
import org.eclipse.andmore.internal.editors.descriptors.SeparatorAttributeDescriptor;
import org.eclipse.andmore.internal.sdk.AndroidTargetData;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Content Assist Processor for /res/drawable XML files
 */
@VisibleForTesting
public final class AnimationContentAssist extends AndroidContentAssist {
    private static final String OBJECT_ANIMATOR = "objectAnimator"; //$NON-NLS-1$
    private static final String PROPERTY_NAME = "propertyName";  //$NON-NLS-1$
    private static final String INTERPOLATOR_PROPERTY_NAME = "interpolator"; //$NON-NLS-1$
    private static final String INTERPOLATOR_NAME_SUFFIX = "_interpolator"; //$NON-NLS-1$

    public AnimationContentAssist() {
        super(AndroidTargetData.DESCRIPTOR_ANIMATOR);
    }

    @Override
    protected int getRootDescriptorId() {
        String folderName = AdtUtils.getParentFolderName(mEditor.getEditorInput());
        ResourceFolderType folderType = ResourceFolderType.getFolderType(folderName);
        if (folderType == ResourceFolderType.ANIM) {
            return AndroidTargetData.DESCRIPTOR_ANIM;
        } else {
            return AndroidTargetData.DESCRIPTOR_ANIMATOR;
        }
    }

    @Override
    protected boolean computeAttributeValues(List<ICompletionProposal> proposals, int offset,
            String parentTagName, String attributeName, Node node, String wordPrefix,
            boolean skipEndTag, int replaceLength) {

        // Add value completion for the interpolator and propertyName attributes

        if (attributeName.endsWith(INTERPOLATOR_PROPERTY_NAME)) {
            if (!wordPrefix.startsWith("@android:anim/")) { //$NON-NLS-1$
                // List all framework interpolators with full path first
                AndroidTargetData data = mEditor.getTargetData();
                ResourceRepository repository = data.getFrameworkResources();
                List<String> interpolators = new ArrayList<String>();
                String base = '@' + ANDROID_PKG + ':' + ResourceType.ANIM.getName() + '/';
                for (ResourceItem item : repository.getResourceItemsOfType(ResourceType.ANIM)) {
                    String name = item.getName();
                    if (name.endsWith(INTERPOLATOR_NAME_SUFFIX)) {
                        interpolators.add(base + item.getName());
                    }
                }
                addMatchingProposals(proposals, interpolators.toArray(), offset, node, wordPrefix,
                        (char) 0 /* needTag */, true /* isAttribute */, false /* isNew */,
                        skipEndTag /* skipEndTag */, replaceLength);
            }


            return super.computeAttributeValues(proposals, offset, parentTagName, attributeName,
                    node, wordPrefix, skipEndTag, replaceLength);
        } else if (parentTagName.equals(OBJECT_ANIMATOR)
                && attributeName.endsWith(PROPERTY_NAME)) {

            // Special case: the user is code completing inside
            //    <objectAnimator propertyName="^">
            // In this case, offer ALL attribute names that make sense for animation
            // (e.g. all numeric ones)

            String attributePrefix = wordPrefix;
            if (startsWith(attributePrefix, ANDROID_NS_NAME_PREFIX)) {
                attributePrefix = attributePrefix.substring(ANDROID_NS_NAME_PREFIX.length());
            }

            AndroidTargetData data = mEditor.getTargetData();
            if (data != null) {
                IDescriptorProvider descriptorProvider =
                    data.getDescriptorProvider(AndroidTargetData.DESCRIPTOR_LAYOUT);
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
                                EnumSet<Format> formats = desc.getAttributeInfo().getFormats();
                                if (formats.contains(Format.INTEGER)
                                        || formats.contains(Format.FLOAT)) {
                                    // TODO: Filter out some common properties
                                    // that the user probably isn't trying to
                                    // animate:
                                    // num*, min*, max*, *Index, *Threshold,
                                    // *Duration, *Id, *Limit
                                    matches.put(name, desc);
                                }
                            }
                        }
                    }

                    List<AttributeDescriptor> sorted =
                        new ArrayList<AttributeDescriptor>(matches.size());
                    sorted.addAll(matches.values());
                    Collections.sort(sorted);
                    // Extract just the name+description pairs, since we don't want to
                    // use the full attribute descriptor (which forces the namespace
                    // prefix to be included)
                    List<Pair<String, String>> pairs =
                        new ArrayList<Pair<String, String>>(sorted.size());
                    for (AttributeDescriptor d : sorted) {
                        pairs.add(Pair.of(d.getXmlLocalName(), d.getAttributeInfo().getJavaDoc()));
                    }

                    addMatchingProposals(proposals, pairs.toArray(), offset, node, wordPrefix,
                            (char) 0 /* needTag */, true /* isAttribute */, false /* isNew */,
                            skipEndTag /* skipEndTag */, replaceLength);
                }
            }

            return false;
        } else {
            return super.computeAttributeValues(proposals, offset, parentTagName, attributeName,
                    node, wordPrefix, skipEndTag, replaceLength);
        }
    }
}

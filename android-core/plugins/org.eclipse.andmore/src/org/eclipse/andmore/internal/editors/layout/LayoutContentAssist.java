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

package org.eclipse.andmore.internal.editors.layout;

import static com.android.SdkConstants.ANDROID_PKG_PREFIX;
import static com.android.SdkConstants.ATTR_CLASS;
import static com.android.SdkConstants.ATTR_CONTEXT;
import static com.android.SdkConstants.ATTR_NAME;
import static com.android.SdkConstants.CLASS_ACTIVITY;
import static com.android.SdkConstants.CLASS_FRAGMENT;
import static com.android.SdkConstants.CLASS_V4_FRAGMENT;
import static com.android.SdkConstants.CLASS_VIEW;
import static com.android.SdkConstants.VIEW_FRAGMENT;
import static com.android.SdkConstants.VIEW_TAG;

import com.android.annotations.Nullable;
import com.android.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.ObjectArrays;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.editors.AndroidContentAssist;
import org.eclipse.andmore.internal.editors.descriptors.ElementDescriptor;
import org.eclipse.andmore.internal.editors.layout.descriptors.CustomViewDescriptorService;
import org.eclipse.andmore.internal.editors.layout.descriptors.ViewElementDescriptor;
import org.eclipse.andmore.internal.editors.layout.gle2.CustomViewFinder;
import org.eclipse.andmore.internal.project.BaseProjectHelper;
import org.eclipse.andmore.internal.sdk.AndroidTargetData;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Content Assist Processor for /res/layout XML files
 */
@VisibleForTesting
public final class LayoutContentAssist extends AndroidContentAssist {

    /**
     * Constructor for LayoutContentAssist
     */
    public LayoutContentAssist() {
        super(AndroidTargetData.DESCRIPTOR_LAYOUT);
    }

    @Override
    protected Object[] getChoicesForElement(String parent, Node currentNode) {
        Object[] choices = super.getChoicesForElement(parent, currentNode);
        if (choices == null) {
            if (currentNode.getParentNode().getNodeType() == Node.ELEMENT_NODE) {
                String parentName = currentNode.getParentNode().getNodeName();
                if (parentName.indexOf('.') != -1) {
                    // Custom view with unknown children; just use the root descriptor
                    // to get all eligible views instead
                    ElementDescriptor[] children = getRootDescriptor().getChildren();
                    for (ElementDescriptor e : children) {
                        if (e.getXmlName().startsWith(parent)) {
                            return sort(children);
                        }
                    }
                }
            }
        }

        if (choices == null && parent.length() >= 1 && Character.isLowerCase(parent.charAt(0))) {
            // Custom view prefix?
            List<ElementDescriptor> descriptors = getCustomViews();
            if (descriptors != null && !descriptors.isEmpty()) {
                List<ElementDescriptor> matches = Lists.newArrayList();
                for (ElementDescriptor descriptor : descriptors) {
                    if (descriptor.getXmlLocalName().startsWith(parent)) {
                        matches.add(descriptor);
                    }
                }
                if (!matches.isEmpty()) {
                    return matches.toArray(new ElementDescriptor[matches.size()]);
                }
            }
        }

        return choices;
    }

    @Override
    protected ElementDescriptor[] getElementChoicesForTextNode(Node parentNode) {
        ElementDescriptor[] choices = super.getElementChoicesForTextNode(parentNode);

        // Add in custom views, if any
        List<ElementDescriptor> descriptors = getCustomViews();
        if (descriptors != null && !descriptors.isEmpty()) {
            ElementDescriptor[] array = descriptors.toArray(
                    new ElementDescriptor[descriptors.size()]);
            choices = ObjectArrays.concat(choices, array, ElementDescriptor.class);
            choices = sort(choices);
        }

        return choices;
    }

    @Nullable
    private List<ElementDescriptor> getCustomViews() {
        // Add in custom views, if any
        IProject project = mEditor.getProject();
        CustomViewFinder finder = CustomViewFinder.get(project);
        Collection<String> views = finder.getAllViews();
        if (views == null) {
            finder.refresh();
            views = finder.getAllViews();
        }
        if (views != null && !views.isEmpty()) {
            List<ElementDescriptor> descriptors = Lists.newArrayListWithExpectedSize(views.size());
            CustomViewDescriptorService customViews = CustomViewDescriptorService.getInstance();
            for (String fqcn : views) {
                ViewElementDescriptor descriptor = customViews.getDescriptor(project, fqcn);
                if (descriptor != null) {
                    descriptors.add(descriptor);
                }
            }

            return descriptors;
        }

        return null;
    }

    @Override
    protected boolean computeAttributeValues(List<ICompletionProposal> proposals, int offset,
            String parentTagName, String attributeName, Node node, String wordPrefix,
            boolean skipEndTag, int replaceLength) {
        super.computeAttributeValues(proposals, offset, parentTagName, attributeName, node,
                wordPrefix, skipEndTag, replaceLength);

        boolean projectOnly = false;
        List<String> superClasses = null;
        if (VIEW_FRAGMENT.equals(parentTagName) && (attributeName.endsWith(ATTR_NAME)
                || attributeName.equals(ATTR_CLASS))) {
            // Insert fragment class matches
            superClasses = Arrays.asList(CLASS_V4_FRAGMENT, CLASS_FRAGMENT);
        } else if (VIEW_TAG.equals(parentTagName) && attributeName.endsWith(ATTR_CLASS)) {
            // Insert custom view matches
            superClasses = Collections.singletonList(CLASS_VIEW);
            projectOnly = true;
        } else if (attributeName.endsWith(ATTR_CONTEXT)) {
            // Insert activity matches
            superClasses = Collections.singletonList(CLASS_ACTIVITY);
        }

        if (superClasses != null) {
            IProject project = mEditor.getProject();
            if (project == null) {
                return false;
            }
            try {
                IJavaProject javaProject = BaseProjectHelper.getJavaProject(project);
                IType type = javaProject.findType(superClasses.get(0));
                Set<IType> elements = new HashSet<IType>();
                if (type != null) {
                    ITypeHierarchy hierarchy = type.newTypeHierarchy(new NullProgressMonitor());
                    IType[] allSubtypes = hierarchy.getAllSubtypes(type);
                    for (IType subType : allSubtypes) {
                        if (!projectOnly || subType.getResource() != null) {
                            elements.add(subType);
                        }
                    }
                }
                assert superClasses.size() <= 2; // If more, need to do additional work below
                if (superClasses.size() == 2) {
                    type = javaProject.findType(superClasses.get(1));
                    if (type != null) {
                        ITypeHierarchy hierarchy = type.newTypeHierarchy(
                                new NullProgressMonitor());
                        IType[] allSubtypes = hierarchy.getAllSubtypes(type);
                        for (IType subType : allSubtypes) {
                            if (!projectOnly || subType.getResource() != null) {
                                elements.add(subType);
                            }
                        }
                    }
                }

                List<IType> sorted = new ArrayList<IType>(elements);
                Collections.sort(sorted, new Comparator<IType>() {
                    @Override
                    public int compare(IType type1, IType type2) {
                        String fqcn1 = type1.getFullyQualifiedName();
                        String fqcn2 = type2.getFullyQualifiedName();
                        int category1 = fqcn1.startsWith(ANDROID_PKG_PREFIX) ? 1 : -1;
                        int category2 = fqcn2.startsWith(ANDROID_PKG_PREFIX) ? 1 : -1;
                        if (category1 != category2) {
                            return category1 - category2;
                        }
                        return fqcn1.compareTo(fqcn2);
                    }
                });
                addMatchingProposals(proposals, sorted.toArray(), offset, node, wordPrefix,
                        (char) 0, false /* isAttribute */, false /* isNew */,
                        false /* skipEndTag */, replaceLength);
                return true;
            } catch (CoreException e) {
                AdtPlugin.log(e, null);
            }
        }

        return false;
    }
}

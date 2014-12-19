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

import static com.android.SdkConstants.ANDROID_PKG;
import static com.android.SdkConstants.ANDROID_PREFIX;
import static com.android.SdkConstants.ANDROID_THEME_PREFIX;
import static com.android.SdkConstants.NEW_ID_PREFIX;
import static com.android.SdkConstants.PREFIX_RESOURCE_REF;
import static com.android.SdkConstants.PREFIX_THEME_REF;

import com.android.ide.common.resources.ResourceItem;
import com.android.ide.common.resources.ResourceRepository;
import com.android.resources.ResourceType;
import com.android.utils.SdkUtils;

import org.eclipse.andmore.internal.editors.AndroidXmlEditor;
import org.eclipse.andmore.internal.editors.common.CommonXmlEditor;
import org.eclipse.andmore.internal.editors.descriptors.AttributeDescriptor;
import org.eclipse.andmore.internal.editors.uimodel.UiResourceAttributeNode;
import org.eclipse.andmore.internal.resources.manager.ResourceManager;
import org.eclipse.andmore.internal.sdk.AndroidTargetData;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.fieldassist.ContentProposal;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Resource value completion for the given property
 * <p>
 * TODO:
 * <ul>
 * <li>also offer other values seen in the app
 * <li>also offer previously set values for this property
 * <li>also complete on properties
 * </ul>
 */
class ResourceValueCompleter implements IContentProposalProvider {
    protected final XmlProperty xmlProperty;

    ResourceValueCompleter(XmlProperty xmlProperty) {
        this.xmlProperty = xmlProperty;
    }

    @Override
    public IContentProposal[] getProposals(String contents, int position) {
        if (contents.startsWith(PREFIX_RESOURCE_REF)) {
            CommonXmlEditor editor = this.xmlProperty.getXmlEditor();
            if (editor != null) {
                String[] matches = computeResourceStringMatches(
                        editor,
                        this.xmlProperty.mDescriptor, contents.substring(0, position));
                List<IContentProposal> proposals = null;
                if (matches != null && matches.length > 0) {
                    proposals = new ArrayList<IContentProposal>(matches.length);
                    for (String match : matches) {
                        proposals.add(new ContentProposal(match));
                    }
                    return proposals.toArray(new IContentProposal[proposals.size()]);
                }
            }
        }

        return new IContentProposal[0];
    }

    /**
     * Similar to {@link UiResourceAttributeNode#computeResourceStringMatches}
     * but computes complete results up front rather than dividing it up into
     * smaller chunks like @{code @android:}, {@code string/}, and {@code ok}.
     */
    static String[] computeResourceStringMatches(AndroidXmlEditor editor,
            AttributeDescriptor attributeDescriptor, String prefix) {
        List<String> results = new ArrayList<String>(200);

        // System matches: only do this if the value already matches at least @a,
        // and doesn't start with something that can't possibly be @android
        if (prefix.startsWith("@a") && //$NON-NLS-1$
                prefix.regionMatches(true /* ignoreCase */, 0, ANDROID_PREFIX, 0,
                        Math.min(prefix.length() - 1, ANDROID_PREFIX.length()))) {
            AndroidTargetData data = editor.getTargetData();
            if (data != null) {
                ResourceRepository repository = data.getFrameworkResources();
                addMatches(repository, prefix, true /* isSystem */, results);
            }
        } else if (prefix.startsWith("?") && //$NON-NLS-1$
                prefix.regionMatches(true /* ignoreCase */, 0, ANDROID_THEME_PREFIX, 0,
                        Math.min(prefix.length() - 1, ANDROID_THEME_PREFIX.length()))) {
            AndroidTargetData data = editor.getTargetData();
            if (data != null) {
                ResourceRepository repository = data.getFrameworkResources();
                addMatches(repository, prefix, true /* isSystem */, results);
            }
        }


        // When completing project resources skip framework resources unless
        // the prefix possibly completes both, such as "@an" which can match
        // both the project resource @animator as well as @android:string
        if (!prefix.startsWith("@and") && !prefix.startsWith("?and")) { //$NON-NLS-1$ //$NON-NLS-2$
            IProject project = editor.getProject();
            if (project != null) {
                // get the resource repository for this project and the system resources.
                ResourceManager manager = ResourceManager.getInstance();
                ResourceRepository repository = manager.getProjectResources(project);
                if (repository != null) {
                    // We have a style name and a repository. Find all resources that match this
                    // type and recreate suggestions out of them.
                    addMatches(repository, prefix, false /* isSystem */, results);
                }

            }
        }

        if (attributeDescriptor != null) {
            UiResourceAttributeNode.sortAttributeChoices(attributeDescriptor, results);
        } else {
            Collections.sort(results);
        }

        return results.toArray(new String[results.size()]);
    }

    private static void addMatches(ResourceRepository repository, String prefix, boolean isSystem,
            List<String> results) {
        int typeStart = isSystem
                ? ANDROID_PREFIX.length() : PREFIX_RESOURCE_REF.length();

        for (ResourceType type : repository.getAvailableResourceTypes()) {
            if (prefix.regionMatches(typeStart, type.getName(), 0,
                    Math.min(type.getName().length(), prefix.length() - typeStart))) {
                StringBuilder sb = new StringBuilder();
                if (prefix.length() == 0 || prefix.startsWith(PREFIX_RESOURCE_REF)) {
                    sb.append(PREFIX_RESOURCE_REF);
                } else {
                    if (type != ResourceType.ATTR) {
                        continue;
                    }
                    sb.append(PREFIX_THEME_REF);
                }

                if (type == ResourceType.ID && prefix.startsWith(NEW_ID_PREFIX)) {
                    sb.append('+');
                }

                if (isSystem) {
                    sb.append(ANDROID_PKG).append(':');
                }

                sb.append(type.getName()).append('/');
                String base = sb.toString();

                int nameStart = typeStart + type.getName().length() + 1; // +1: add "/" divider
                String namePrefix =
                        prefix.length() <= nameStart ? "" : prefix.substring(nameStart);
                for (ResourceItem item : repository.getResourceItemsOfType(type)) {
                    String name = item.getName();
                    if (SdkUtils.startsWithIgnoreCase(name, namePrefix)) {
                        results.add(base + name);
                    }
                }
            }
        }
    }
}

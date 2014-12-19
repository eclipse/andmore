/*
 * Copyright (C) 2009 The Android Open Source Project
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

package org.eclipse.andmore.internal.editors.manifest.descriptors;

import org.eclipse.andmore.internal.editors.descriptors.DescriptorsUtils;
import org.eclipse.andmore.internal.editors.descriptors.ITextAttributeCreator;
import org.eclipse.andmore.internal.editors.descriptors.TextAttributeDescriptor;
import org.eclipse.andmore.internal.editors.manifest.model.UiManifestPkgAttrNode;
import org.eclipse.andmore.internal.editors.uimodel.UiAttributeNode;
import org.eclipse.andmore.internal.editors.uimodel.UiElementNode;

import com.android.ide.common.api.IAttributeInfo;

/**
 * Describes a package XML attribute. It is displayed by a {@link UiManifestPkgAttrNode}.
 * <p/>
 * Used by the override for .../targetPackage in {@link AndroidManifestDescriptors}.
 */
public class ManifestPkgAttrDescriptor extends TextAttributeDescriptor {

    /**
     * Used by {@link DescriptorsUtils} to create instances of this descriptor.
     */
    public static final ITextAttributeCreator CREATOR = new ITextAttributeCreator() {
        @Override
        public TextAttributeDescriptor create(String xmlLocalName,
                String nsUri, IAttributeInfo attrInfo) {
            return new ManifestPkgAttrDescriptor(xmlLocalName, nsUri, attrInfo);
        }
    };

    public ManifestPkgAttrDescriptor(String xmlLocalName, String nsUri, IAttributeInfo attrInfo) {
        super(xmlLocalName, nsUri, attrInfo);
    }

    /**
     * @return A new {@link UiManifestPkgAttrNode} linked to this descriptor.
     */
    @Override
    public UiAttributeNode createUiNode(UiElementNode uiParent) {
        return new UiManifestPkgAttrNode(this, uiParent);
    }
}

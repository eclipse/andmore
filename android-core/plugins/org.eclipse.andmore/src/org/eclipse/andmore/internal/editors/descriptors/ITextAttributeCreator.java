/*
 * Copyright (C) 2010 The Android Open Source Project
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

package org.eclipse.andmore.internal.editors.descriptors;

import com.android.SdkConstants;
import com.android.ide.common.api.IAttributeInfo;


/**
 * The {@link ITextAttributeCreator} interface is used by the appendAttribute(...) in
 * {@link DescriptorsUtils} to allows callers to override the kind of
 * {@link TextAttributeDescriptor} created for a given XML attribute name.
 * <p/>
 * The <code>create()</code> method must take arguments that are similar to the
 * single constructor for {@link TextAttributeDescriptor}.
 */
public interface ITextAttributeCreator {

    /**
     * Creates a new {@link TextAttributeDescriptor} instance for the given XML name,
     * UI name and tooltip.
     *
     * @param xmlLocalName The XML name of the attribute (case sensitive)
     * @param nsUri The URI of the attribute. Can be null if attribute has no namespace.
     *              See {@link SdkConstants#NS_RESOURCES} for a common value.
     * @param attrInfo The {@link IAttributeInfo} of this attribute. Can't be null.
     * @return A new {@link TextAttributeDescriptor} (or derived) instance.
     */
    public TextAttributeDescriptor create(
            String xmlLocalName,
            String nsUri,
            IAttributeInfo attrInfo);
}

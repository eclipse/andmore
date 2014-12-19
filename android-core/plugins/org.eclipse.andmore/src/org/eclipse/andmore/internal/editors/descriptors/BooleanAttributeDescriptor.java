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

package org.eclipse.andmore.internal.editors.descriptors;

import org.eclipse.andmore.internal.editors.uimodel.UiListAttributeNode;

import com.android.ide.common.api.IAttributeInfo;

/**
 * Describes a text attribute that can only contain boolean values.
 * It is displayed by a {@link UiListAttributeNode}.
 */
public class BooleanAttributeDescriptor extends ListAttributeDescriptor {
    private static final String[] VALUES = new String[] { "true", "false" };  //$NON-NLS-1$ //$NON-NLS-2$

    public BooleanAttributeDescriptor(String xmlLocalName, String nsUri, IAttributeInfo attrInfo) {
        super(xmlLocalName, nsUri, attrInfo, VALUES);
    }
}


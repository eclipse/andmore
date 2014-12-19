/*
 * Copyright (C) 2011 The Android Open Source Project
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
package org.eclipse.andmore.common.layout;

import com.android.annotations.NonNull;
import com.android.ide.common.api.INode;
import com.android.ide.common.api.INodeHandler;

/**
 * A convenience implementation of {@link INodeHandler} for setting a given attribute to a
 * given value on a particular node.
 */
class PropertySettingNodeHandler implements INodeHandler {
    private final String mNamespaceUri;
    private final String mAttribute;
    private final String mValue;

    PropertySettingNodeHandler(String namespaceUri, String attribute, String value) {
        super();
        mNamespaceUri = namespaceUri;
        mAttribute = attribute;
        mValue = value;
    }

    @Override
    public void handle(@NonNull INode node) {
        node.setAttribute(mNamespaceUri, mAttribute, mValue);
    }
}
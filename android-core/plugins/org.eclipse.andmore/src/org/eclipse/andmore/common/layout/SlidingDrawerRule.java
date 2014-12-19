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

import static com.android.SdkConstants.ATTR_CONTENT;
import static com.android.SdkConstants.ATTR_HANDLE;
import static com.android.SdkConstants.ATTR_ID;
import static com.android.SdkConstants.ATTR_LAYOUT_HEIGHT;
import static com.android.SdkConstants.ATTR_LAYOUT_WIDTH;
import static com.android.SdkConstants.ATTR_TEXT;


import com.android.SdkConstants;
import static com.android.SdkConstants.ANDROID_URI;
import com.android.annotations.NonNull;
import com.android.ide.common.api.INode;
import com.android.ide.common.api.IViewRule;
import com.android.ide.common.api.InsertType;

/**
 * An {@link IViewRule} for android.widget.SlidingDrawerRule which initializes new sliding
 * drawers with their mandatory children and default sizing attributes
 */
public class SlidingDrawerRule extends BaseLayoutRule {

    @Override
    public void onCreate(@NonNull INode node, @NonNull INode parent,
            @NonNull InsertType insertType) {
        super.onCreate(node, parent, insertType);

        if (insertType.isCreate()) {
            String matchParent = getFillParentValueName();
            node.setAttribute(ANDROID_URI, ATTR_LAYOUT_WIDTH, matchParent);
            node.setAttribute(ANDROID_URI, ATTR_LAYOUT_HEIGHT, matchParent);

            // Create mandatory children and reference them from the handle and content
            // attributes of the sliding drawer
            String handleId = "@+id/handle";    //$NON-NLS-1$
            String contentId = "@+id/content";  //$NON-NLS-1$
            node.setAttribute(ANDROID_URI, ATTR_HANDLE, handleId);
            node.setAttribute(ANDROID_URI, ATTR_CONTENT, contentId);

            // Handle
            INode handle = node.appendChild(SdkConstants.FQCN_BUTTON);
            handle.setAttribute(ANDROID_URI, ATTR_TEXT, "Handle");
            handle.setAttribute(ANDROID_URI, ATTR_ID, handleId);

            // Content
            INode content = node.appendChild(SdkConstants.FQCN_LINEAR_LAYOUT);
            content.setAttribute(ANDROID_URI, ATTR_ID, contentId);
            content.setAttribute(ANDROID_URI, ATTR_LAYOUT_WIDTH, matchParent);
            content.setAttribute(ANDROID_URI, ATTR_LAYOUT_HEIGHT, matchParent);
        }
    }
}

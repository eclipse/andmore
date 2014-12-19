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

package org.eclipse.andmore.common.layout;

import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_ID;
import static com.android.SdkConstants.ATTR_LAYOUT_BELOW;
import static com.android.SdkConstants.ATTR_LAYOUT_WIDTH;
import static com.android.SdkConstants.ATTR_TEXT;
import static com.android.SdkConstants.FQCN_EDIT_TEXT;

import com.android.annotations.NonNull;
import com.android.ide.common.api.INode;
import com.android.ide.common.api.IViewRule;
import com.android.ide.common.api.InsertType;

/**
 * An {@link IViewRule} for android.widget.DialerFilterRule.
 */
public class DialerFilterRule extends BaseViewRule {

    @Override
    public void onCreate(@NonNull INode node, @NonNull INode parent,
            @NonNull InsertType insertType) {
        super.onCreate(node, parent, insertType);

        // A DialerFilter requires a couple of nested EditTexts with fixed ids:
        if (insertType.isCreate()) {
            String fillParent = getFillParentValueName();
            INode hint = node.appendChild(FQCN_EDIT_TEXT);
            hint.setAttribute(ANDROID_URI, ATTR_TEXT, "Hint");
            hint.setAttribute(ANDROID_URI, ATTR_ID, "@android:id/hint"); //$NON-NLS-1$
            hint.setAttribute(ANDROID_URI, ATTR_LAYOUT_WIDTH, fillParent);

            INode primary = node.appendChild(FQCN_EDIT_TEXT);
            primary.setAttribute(ANDROID_URI, ATTR_TEXT, "Primary");
            primary.setAttribute(ANDROID_URI, ATTR_ID, "@android:id/primary"); //$NON-NLS-1$
            primary.setAttribute(ANDROID_URI, ATTR_LAYOUT_BELOW,
                    "@android:id/hint"); //$NON-NLS-1$
            primary.setAttribute(ANDROID_URI, ATTR_LAYOUT_WIDTH, fillParent);


            // What do we initialize the icon to?
            //INode icon = node.appendChild("android.widget.ImageView"); //$NON-NLS-1$
            //icon.setAttribute(ANDROID_URI, ATTR_ID, "@android:id/icon"); //$NON-NLS-1$
        }
    }

}

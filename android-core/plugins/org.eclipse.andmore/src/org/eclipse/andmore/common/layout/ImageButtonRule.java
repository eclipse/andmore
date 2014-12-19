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
import static com.android.SdkConstants.ATTR_SRC;

import com.android.annotations.NonNull;
import com.android.ide.common.api.INode;
import com.android.ide.common.api.IViewRule;
import com.android.ide.common.api.InsertType;

/**
 * An {@link IViewRule} for android.widget.ImageButton.
 */
public class ImageButtonRule extends BaseViewRule {

    @Override
    public void onCreate(@NonNull INode node, @NonNull INode parent,
            @NonNull InsertType insertType) {
        super.onCreate(node, parent, insertType);

        // When dropping an include tag, ask the user which layout to include.
        if (insertType == InsertType.CREATE) { // NOT InsertType.CREATE_PREVIEW
            String src = mRulesEngine.displayResourceInput("drawable", ""); //$NON-NLS-1$ //$NON-NLS-2$
            if (src != null) {
                node.editXml("Set Image",
                    new PropertySettingNodeHandler(ANDROID_URI, ATTR_SRC,
                            src.length() > 0 ? src : null));
                return;
            }
        }

        // Fallback if dismissed or during previews etc
        if (insertType.isCreate()) {
            node.setAttribute(ANDROID_URI, ATTR_SRC, getSampleImageSrc());
        }
    }

}

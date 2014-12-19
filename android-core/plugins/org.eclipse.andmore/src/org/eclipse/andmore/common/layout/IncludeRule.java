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

import static com.android.SdkConstants.ATTR_LAYOUT;

import com.android.annotations.NonNull;
import com.android.ide.common.api.INode;
import com.android.ide.common.api.IViewRule;
import com.android.ide.common.api.InsertType;

/**
 * An {@link IViewRule} for the special XML {@code <include>} tag.
 */
public class IncludeRule extends BaseViewRule {
    @Override
    public void onCreate(@NonNull INode node, @NonNull INode parent,
            @NonNull InsertType insertType) {
        // When dropping an include tag, ask the user which layout to include.
        if (insertType == InsertType.CREATE) { // NOT InsertType.CREATE_PREVIEW
            String include = mRulesEngine.displayIncludeSourceInput();
            if (include != null) {
                node.editXml("Include Layout",
                    // Note -- the layout attribute is NOT in the Android namespace!
                    new PropertySettingNodeHandler(null, ATTR_LAYOUT,
                            include.length() > 0 ? include : null));
            } else {
                // Remove the view; the insertion was canceled
                parent.removeChild(node);
            }
        }
    }
}

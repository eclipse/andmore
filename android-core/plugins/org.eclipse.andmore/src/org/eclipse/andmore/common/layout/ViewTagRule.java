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
package org.eclipse.andmore.common.layout;

import static com.android.SdkConstants.ATTR_CLASS;

import org.eclipse.andmore.internal.editors.layout.descriptors.ViewElementDescriptor;

import com.android.annotations.NonNull;
import com.android.ide.common.api.INode;
import com.android.ide.common.api.IViewRule;
import com.android.ide.common.api.InsertType;

/**
 * An {@link IViewRule} for the special XML {@code <view>} tag.
 */
public class ViewTagRule extends BaseViewRule {
    @Override
    public void onCreate(@NonNull INode node, @NonNull INode parent,
            @NonNull InsertType insertType) {
        // When dropping a view tag, ask the user which custom view class to use
        if (insertType == InsertType.CREATE) { // NOT InsertType.CREATE_PREVIEW
            String fqcn = mRulesEngine.displayCustomViewClassInput();
            if (fqcn != null) {
                if (!ViewElementDescriptor.viewNeedsPackage(fqcn)) {
                    fqcn = fqcn.substring(fqcn.lastIndexOf('.') + 1);
                }
                node.editXml("Set Custom View Class",
                    new PropertySettingNodeHandler(null, ATTR_CLASS,
                            fqcn.length() > 0 ? fqcn : null));
            } else {
                // Remove the view; the insertion was canceled
                parent.removeChild(node);
            }
        }
    }
}

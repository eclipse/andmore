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
import static com.android.SdkConstants.ATTR_LAYOUT_WIDTH;
import static com.android.SdkConstants.ATTR_NUM_COLUMNS;

import com.android.annotations.NonNull;
import com.android.ide.common.api.INode;
import com.android.ide.common.api.IViewRule;
import com.android.ide.common.api.InsertType;

/**
 * An {@link IViewRule} for android.widget.GridView
 */
public class GridViewRule extends BaseViewRule {

    @Override
    public void onCreate(@NonNull INode node, @NonNull INode parent,
            @NonNull InsertType insertType) {
        super.onCreate(node, parent, insertType);

        node.setAttribute(ANDROID_URI, ATTR_LAYOUT_WIDTH, getFillParentValueName());
        node.setAttribute(ANDROID_URI, ATTR_NUM_COLUMNS, "3"); //$NON-NLS-1$
    }
}

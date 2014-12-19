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

import static com.android.SdkConstants.ATTR_CHECKED;
import static com.android.SdkConstants.ATTR_ID;
import static com.android.SdkConstants.VALUE_TRUE;


import com.android.SdkConstants;
import static com.android.SdkConstants.ANDROID_URI;
import com.android.annotations.NonNull;
import com.android.ide.common.api.INode;
import com.android.ide.common.api.IViewRule;
import com.android.ide.common.api.InsertType;

/**
 * An {@link IViewRule} for android.widget.RadioGroup which initializes the radio group
 * with some radio buttons
 */
public class RadioGroupRule extends LinearLayoutRule {
    @Override
    public void onCreate(@NonNull INode node, @NonNull INode parent,
            @NonNull InsertType insertType) {
        super.onCreate(node, parent, insertType);

        if (insertType.isCreate()) {
            for (int i = 0; i < 3; i++) {
                INode handle = node.appendChild(SdkConstants.FQCN_RADIO_BUTTON);
                handle.setAttribute(ANDROID_URI, ATTR_ID, String.format("@+id/radio%d", i));
                if (i == 0) {
                    handle.setAttribute(ANDROID_URI, ATTR_CHECKED, VALUE_TRUE);
                }
            }
        }
    }
}

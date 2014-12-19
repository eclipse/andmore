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
import com.android.ide.common.api.RuleAction;

import java.util.List;

/**
 * Drop handler for the {@code <merge>} tag
 */
public class MergeRule extends FrameLayoutRule {
    // The <merge> tag behaves a lot like the FrameLayout; all children are added
    // on top of each other at (0,0)

    @Override
    public void addContextMenuActions(@NonNull List<RuleAction> actions,
            final @NonNull INode selectedNode) {
        // Deliberately ignore super.getContextMenu(); we don't want to attempt to list
        // properties for the <merge> tag
    }
}

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

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.api.DropFeedback;
import com.android.ide.common.api.IDragElement;
import com.android.ide.common.api.INode;

/**
 * An ignored layout is a layout that should not be treated as a layout by the
 * visual editor (usually because the widget extends a layout class we recognize
 * and support, but where the widget is more restrictive in how it manages its
 * children so we don't want to expose the normal configuration options).
 * <p>
 * For example, the ZoomControls widget is not user-configurable as a
 * LinearLayout even though it extends it. Our ZoomControls rule is therefore a
 * subclass of this {@link IgnoredLayoutRule} class.
 */
public abstract class IgnoredLayoutRule extends BaseLayoutRule {
    @Override
    public DropFeedback onDropEnter(@NonNull INode targetNode, @Nullable Object targetView,
            @Nullable IDragElement[] elements) {
        // Do nothing; this layout rule corresponds to a layout that
        // should not be handled as a layout by the visual editor - usually
        // because some widget is extending a layout for implementation purposes
        // but does not want to expose configurability of the base layout in the
        // editor.
        return null;
    }
}

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
package org.eclipse.andmore.internal.editors.common;

import org.eclipse.andmore.internal.editors.layout.LayoutEditorDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.part.EditorActionBarContributor;

/**
 * Action contributor for the editors.
 * This delegates to editor-specific action contributors.
 */
public class CommonActionContributor extends EditorActionBarContributor {

    public CommonActionContributor() {
        super();
    }

    @Override
    public void setActiveEditor(IEditorPart part) {
        LayoutEditorDelegate delegate = LayoutEditorDelegate.fromEditor(part);
        if (delegate != null) {
            delegate.setActiveEditor(part, getActionBars());
        }
    }
}

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
package org.eclipse.andmore.internal.lint;

import com.android.annotations.NonNull;

import org.eclipse.andmore.internal.editors.AndroidXmlEditor;
import org.eclipse.andmore.internal.editors.layout.gle2.DelegatingAction;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.widgets.Event;

/**
 * Action intended to wrap an existing XML editor action, and then runs lint after
 * the edit.
 */
public class LintEditAction extends DelegatingAction {
    private final AndroidXmlEditor mEditor;

    /**
     * Creates a new {@link LintEditAction} associated with the given editor to
     * wrap the given action
     *
     * @param action the action to be wrapped
     * @param editor the editor associated with the action
     */
    public LintEditAction(@NonNull IAction action, @NonNull AndroidXmlEditor editor) {
        super(action);
        mEditor = editor;
    }

    @Override
    public void runWithEvent(Event event) {
        super.runWithEvent(event);
        mEditor.runEditHooks();
    }
}

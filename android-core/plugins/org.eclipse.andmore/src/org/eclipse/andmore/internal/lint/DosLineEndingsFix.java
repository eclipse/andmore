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

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

/** Quickfix for correcting line endings in the file */
class DosLineEndingsFix extends LintFix {

    protected DosLineEndingsFix(String id, IMarker marker) {
        super(id, marker);
    }

    @Override
    public boolean needsFocus() {
        return false;
    }

    @Override
    public boolean isCancelable() {
        return false;
    }

    @Override
    public String getDisplayString() {
        return "Fix line endings";
    }

    @Override
    public void apply(IDocument document) {
        char next = 0;
        for (int i = document.getLength() - 1; i >= 0; i--) {
            try {
                char c = document.getChar(i);
                if (c == '\r' && next != '\n') {
                    document.replace(i, 1, "\n"); //$NON-NLS-1$
                }
                next = c;
            } catch (BadLocationException e) {
                AdtPlugin.log(e, null);
                return;
            }
        }

        deleteMarker();
    }
}

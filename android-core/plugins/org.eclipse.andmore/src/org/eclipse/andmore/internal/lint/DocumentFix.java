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
import org.eclipse.andmore.internal.editors.layout.gle2.DomUtilities;
import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.IDocument;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.w3c.dom.Node;

@SuppressWarnings("restriction") // DOM model
abstract class DocumentFix extends LintFix {

    protected DocumentFix(String id, IMarker marker) {
        super(id, marker);
    }

    protected abstract void apply(IDocument document, IStructuredModel model, Node node,
            int start, int end);

    @Override
    public void apply(IDocument document) {
        if (!(document instanceof IStructuredDocument)) {
            AdtPlugin.log(null, "Unexpected document type: %1$s. Can't fix.",
                    document.getClass().getName());
            return;
        }
        int start = mMarker.getAttribute(IMarker.CHAR_START, -1);
        int end = mMarker.getAttribute(IMarker.CHAR_END, -1);
        if (start != -1 && end != -1) {
            IModelManager manager = StructuredModelManager.getModelManager();
            IStructuredModel model = manager.getModelForEdit((IStructuredDocument) document);
            Node node = DomUtilities.getNode(document, start);
            try {
                apply(document, model, node, start, end);
            } finally {
                model.releaseFromEdit();
            }

            if (!isCancelable()) {
                deleteMarker();
            }
        }
    }
}
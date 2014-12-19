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

package org.eclipse.andmore.internal.editors.otherxml;

import org.eclipse.andmore.internal.editors.common.CommonXmlDelegate;
import org.eclipse.andmore.internal.editors.common.CommonXmlEditor;
import org.w3c.dom.Document;

/**
 * Plain XML editor with no form for files that have no associated descriptor data
 */
public class PlainXmlEditorDelegate extends CommonXmlDelegate {

    /**
     * Creates the form editor for plain XML files.
     */
    public PlainXmlEditorDelegate(CommonXmlEditor editor) {
        super(editor, new OtherXmlContentAssist());
        editor.addDefaultTargetListener();
    }

    // ---- Base Class Overrides ----

    @Override
    public void delegateCreateFormPages() {
    }

    @Override
    public void delegateXmlModelChanged(Document xml_doc) {
    }

    @Override
    public void delegateInitUiRootNode(boolean force) {
    }
}

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

import org.eclipse.andmore.internal.editors.AndroidSourceViewerConfig;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.source.ISourceViewer;



/**
 * Source Viewer Configuration for the Common XML editor.
 * Everything is already generic and done in the base class.
 * The base class will use the delegate to find out the proper content assist to use.
 */
public final class CommonSourceViewerConfig extends AndroidSourceViewerConfig {

    private final IContentAssistProcessor mContentAssist;

    public CommonSourceViewerConfig() {
        super();
        mContentAssist = null;
    }

    public CommonSourceViewerConfig(IContentAssistProcessor contentAssist) {
        super();
        mContentAssist = contentAssist;
    }


    /**
     * @return The {@link IContentAssistProcessor} passed to the constructor or null.
     */
    @Override
    public IContentAssistProcessor getAndroidContentAssistProcessor(
            ISourceViewer sourceViewer,
            String partitionType) {
        // You may think you could use AndroidXmlEditor.fromTextViewer(sourceViewer)
        // to find the editor associated with the sourceViewer and then access the
        // delegate and query the content assist specific to a given delegate.
        // Unfortunately this is invoked whilst the editor part is being created
        // so we can't match an existing editor to the source view -- since there
        // is no such "existing" editor. It's just being created.
        //
        // As a workaround, CommonXmlEditor#addPages() will unconfigure the
        // default sourceViewerConfig and reconfigure it with one that really
        // knows which content assist it should be using.

        return mContentAssist;
    }
}

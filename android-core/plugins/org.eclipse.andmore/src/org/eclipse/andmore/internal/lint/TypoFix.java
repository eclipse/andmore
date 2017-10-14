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

import com.android.tools.lint.checks.TypoDetector;
import com.android.tools.lint.detector.api.TextFormat;

import org.eclipse.andmore.AndmoreAndroidPlugin;
import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.FindReplaceDocumentAdapter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;

/** Quickfix for fixing typos */
@SuppressWarnings("restriction") // DOM model
final class TypoFix extends DocumentFix {
    private String mTypo;
    private String mReplacement;

    private TypoFix(String id, IMarker marker) {
        super(id, marker);
    }

    @Override
    public String getDisplayString() {
        return String.format("Replace \"%1$s\" by \"%2$s\"", mTypo, mReplacement);
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
    protected void apply(IDocument document, IStructuredModel model, Node node,
            int start, int end) {
        String message = mMarker.getAttribute(IMarker.MESSAGE, "");
        String typo = TypoDetector.getTypo(message, TextFormat.TEXT);
        if (typo == null) {
            return;
        }
        List<String> replacements = TypoDetector.getSuggestions(message, TextFormat.TEXT).getReplacements();
        if (replacements == null || replacements.isEmpty()) {
            return;
        }

        try {
            String current = document.get(start, end-start);
            if (current.equals(typo)) {
                document.replace(start, end - start, replacements.get(0));
            } else {
                // The buffer has been edited; try to find the typo.
                FindReplaceDocumentAdapter finder = new FindReplaceDocumentAdapter(document);
                IRegion forward = finder.find(start, typo, true /*forward*/, true, true, false);
                IRegion backward = finder.find(start, typo, false /*forward*/, true, true, false);
                if (forward != null && backward != null) {
                    // Pick the closest one
                    int forwardDelta = forward.getOffset() - start;
                    int backwardDelta = start - backward.getOffset();
                    if (forwardDelta < backwardDelta) {
                        start = forward.getOffset();
                    } else {
                        start = backward.getOffset();
                    }
                } else if (forward != null) {
                    start = forward.getOffset();
                } else if (backward != null) {
                    start = backward.getOffset();
                } else {
                    return;
                }
                end = start + typo.length();
                document.replace(start, end - start, replacements.get(0));
            }
        } catch (BadLocationException e) {
            AndmoreAndroidPlugin.log(e, null);
        }
    }

    @Override
    protected List<LintFix> getAllFixes() {
        String message = mMarker.getAttribute(IMarker.MESSAGE, "");
        String typo = TypoDetector.getTypo(message, TextFormat.TEXT);
        List<String> replacements = TypoDetector.getSuggestions(message, TextFormat.TEXT).getReplacements();
        if (replacements != null && !replacements.isEmpty() && typo != null) {
            List<LintFix> allFixes = new ArrayList<LintFix>(replacements.size());
            for (String replacement : replacements) {
                TypoFix fix = new TypoFix(mId, mMarker);
                fix.mTypo = typo;
                fix.mReplacement = replacement;
                allFixes.add(fix);
            }

            return allFixes;
        }

        return null;
    }
}
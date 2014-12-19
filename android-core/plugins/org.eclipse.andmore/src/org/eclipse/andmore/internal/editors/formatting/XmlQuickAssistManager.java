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
package org.eclipse.andmore.internal.editors.formatting;

import org.eclipse.andmore.internal.build.AaptQuickFix;
import org.eclipse.andmore.internal.editors.layout.refactoring.RefactoringAssistant;
import org.eclipse.andmore.internal.lint.LintFixGenerator;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.source.Annotation;

import java.util.ArrayList;
import java.util.List;

/**
 * This class implements Quick Assists for XML files. It does not perform any
 * quick assistance on its own, but it coordinates the various separate quick
 * assists available for XML such that the order is logical. This is necessary
 * because without it, the order of suggestions (when more than one assistant
 * provides suggestions) is not always optimal. There doesn't seem to be a way
 * from non-Java languages to set the sorting order (see
 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=229983 ). So instead of
 * registering our multiple XML quick assistants via the plugin.xml file, we
 * register <b>just</b> this manager, which delegates to the various XML quick
 * assistants as appropriate.
 */
public class XmlQuickAssistManager implements IQuickAssistProcessor {
    private final IQuickAssistProcessor[] mProcessors;

    /** Constructs a new {@link XmlQuickAssistManager} which orders the quick fixes */
    public XmlQuickAssistManager() {
        mProcessors = new IQuickAssistProcessor[] {
                new AaptQuickFix(),
                new LintFixGenerator(),
                new RefactoringAssistant()
        };
    }

    @Override
    public String getErrorMessage() {
        return null;
    }

    @Override
    public boolean canFix(Annotation annotation) {
        for (IQuickAssistProcessor processor : mProcessors) {
            if (processor.canFix(annotation)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean canAssist(IQuickAssistInvocationContext invocationContext) {
        for (IQuickAssistProcessor processor : mProcessors) {
            if (processor.canAssist(invocationContext)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public ICompletionProposal[] computeQuickAssistProposals(
            IQuickAssistInvocationContext invocationContext) {
        List<ICompletionProposal> allProposals = null;
        for (IQuickAssistProcessor processor : mProcessors) {
            if (processor.canAssist(invocationContext)) {
                ICompletionProposal[] proposals =
                        processor.computeQuickAssistProposals(invocationContext);
                if (proposals != null && proposals.length > 0) {
                    if (allProposals == null) {
                        allProposals = new ArrayList<ICompletionProposal>();
                    }
                    for (ICompletionProposal proposal : proposals) {
                        allProposals.add(proposal);
                    }
                }
            }
        }

        if (allProposals != null) {
            return allProposals.toArray(new ICompletionProposal[allProposals.size()]);
        }

        return null;
    }
}

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

package org.eclipse.andmore.internal.editors.layout.refactoring;

import org.eclipse.andmore.internal.project.BaseProjectHelper;
import org.eclipse.andmore.internal.refactorings.extractstring.ExtractStringProposal;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

/**
 * Quick Assistant for Java files in Android projects
 */
public class JavaQuickAssistant implements org.eclipse.jdt.ui.text.java.IQuickAssistProcessor {
    public JavaQuickAssistant() {
    }

    @Override
    public boolean hasAssists(IInvocationContext context) throws CoreException {
        return true;
    }

    @Override
    public IJavaCompletionProposal[] getAssists(IInvocationContext context,
            IProblemLocation[] locations) throws CoreException {
        // We should only offer Android quick assists within Android projects.
        // This can be done by adding this logic to the extension registration:
        //
        //    <enablement>
        //        <with variable="projectNatures">
        //            <iterate operator="or">
        //                <equals value="org.eclipe.andmore.AndroidNature"/>
        //            </iterate>
        //        </with>
        //    </enablement>
        //
        // However, this causes some errors to be dumped to the log, so instead we filter
        // out non Android projects programmatically:

        IProject project = context.getCompilationUnit().getJavaProject().getProject();
        if (project == null || !BaseProjectHelper.isAndroidProject(project)) {
            return null;
        }

        ASTNode coveringNode = context.getCoveringNode();
        if (coveringNode != null && coveringNode.getNodeType() == ASTNode.STRING_LITERAL
                && coveringNode.getLength() > 2) { // don't extract empty strings (includes quotes)
            return new IJavaCompletionProposal[] {
                new ExtractStringProposal(context)
            };
        }

        return null;
    }
}

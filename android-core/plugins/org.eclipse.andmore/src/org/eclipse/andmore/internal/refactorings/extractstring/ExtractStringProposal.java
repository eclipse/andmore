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
package org.eclipse.andmore.internal.refactorings.extractstring;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.AdtUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * Proposal for extracting strings in Java files
 */
public class ExtractStringProposal implements IJavaCompletionProposal {
    private IInvocationContext mContext;

    public ExtractStringProposal(IInvocationContext context) {
        mContext = context;
    }

    @Override
    public void apply(IDocument document) {
        IEditorPart editor = AdtUtils.getActiveEditor();
        IFile file = AdtUtils.getActiveFile();
        if (editor == null || file == null) {
            return;
        }

        ASTNode coveringNode = mContext.getCoveringNode();
        int start = coveringNode.getStartPosition();
        int length = coveringNode.getLength();
        ITextSelection selection = new TextSelection(start, length);

        ExtractStringRefactoring refactoring = new ExtractStringRefactoring(file, editor,
                selection);

        RefactoringWizard wizard = new ExtractStringWizard(refactoring, file.getProject());
        RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation(wizard);
        try {
            IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
            op.run(window.getShell(), wizard.getDefaultPageTitle());
        } catch (InterruptedException e) {
        }
    }

    @Override
    public Point getSelection(IDocument document) {
        return null;
    }

    @Override
    public String getAdditionalProposalInfo() {
        try {
            ASTNode coveringNode = mContext.getCoveringNode();
            int start = coveringNode.getStartPosition();
            int length = coveringNode.getLength();
            IBuffer buffer = mContext.getCompilationUnit().getBuffer();
            StringBuilder sb = new StringBuilder();
            String string = buffer.getText(start, length);
            string = ExtractStringRefactoring.unquoteAttrValue(string);
            String token = ExtractStringInputPage.guessId(string);

            // Look up the beginning and the end of the line (outside of the extracted string)
            // such that we can show a preview of the diff, e.g. if you have
            // foo.setTitle("Hello"); we want to show foo.setTitle(R.string.hello);
            // so we need to extract "foo.setTitle(" and ");".

            // Look backwards to the beginning of the line (and strip whitespace)
            int i = start - 1;
            while (i > 0) {
                char c = buffer.getChar(i);
                if (c == '\r' || (c == '\n')) {
                    break;
                }
                i--;
            }
            String linePrefix = buffer.getText(i + 1, start - (i + 1)).trim();

            // Look forwards to the end of the line (and strip whitespace)
            i = start + length;
            while (i < buffer.getLength()) {
                char c = buffer.getChar(i);
                if (c == '\r' || (c == '\n')) {
                    break;
                }
                i++;
            }
            String lineSuffix = buffer.getText(start + length, i - (start + length));

            // Should we show the replacement as just R.string.foo or
            // context.getString(R.string.foo) ?
            boolean useContext = false;
            ASTNode parent = coveringNode.getParent();
            if (parent != null) {
                int type = parent.getNodeType();
                if (type == ASTNode.ASSIGNMENT
                        || type == ASTNode.VARIABLE_DECLARATION_STATEMENT
                        || type == ASTNode.VARIABLE_DECLARATION_FRAGMENT
                        || type == ASTNode.VARIABLE_DECLARATION_EXPRESSION) {
                    useContext = true;
                }
            }

            // Display .java change:
            sb.append("...<br>");                   //$NON-NLS-1$
            sb.append(linePrefix);
            sb.append("<b>");                       //$NON-NLS-1$
            if (useContext) {
                sb.append("context.getString(");    //$NON-NLS-1$
            }
            sb.append("R.string.");                 //$NON-NLS-1$
            sb.append(token);
            if (useContext) {
                sb.append(")");                     //$NON-NLS-1$
            }
            sb.append("</b>");                      //$NON-NLS-1$
            sb.append(lineSuffix);
            sb.append("<br>...<br>");               //$NON-NLS-1$

            // Display strings.xml change:
            sb.append("<br>");                      //$NON-NLS-1$
            sb.append("&lt;resources&gt;<br>");     //$NON-NLS-1$
            sb.append("    <b>&lt;string name=\""); //$NON-NLS-1$
            sb.append(token);
            sb.append("\"&gt;");                    //$NON-NLS-1$
            sb.append(string);
            sb.append("&lt;/string&gt;</b><br>");   //$NON-NLS-1$
            sb.append("&lt;/resources&gt;");        //$NON-NLS-1$

            return sb.toString();
        } catch (JavaModelException e) {
            AdtPlugin.log(e, null);
        }

        return "Initiates the Extract String refactoring operation";
    }

    @Override
    public String getDisplayString() {
        return "Extract String";
    }

    @Override
    public Image getImage() {
        return AdtPlugin.getAndroidLogo();
    }

    @Override
    public IContextInformation getContextInformation() {
        return null;
    }

    @Override
    public int getRelevance() {
        return 80;
    }
}
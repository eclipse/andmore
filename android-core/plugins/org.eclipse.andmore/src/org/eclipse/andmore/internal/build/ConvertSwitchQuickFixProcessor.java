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

package org.eclipse.andmore.internal.build;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.AdtUtils;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.IQuickFixProcessor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;
import org.eclipse.ui.texteditor.IDocumentProvider;

import java.util.List;

/**
 * A quickfix processor which looks for "case expressions must be constant
 * expressions" errors, and if they apply to fields in a class named R, it
 * assumes this is code related to library projects that are no longer final and
 * will need to be rewritten to use if-else chains instead.
 */
public class ConvertSwitchQuickFixProcessor implements IQuickFixProcessor {
    /** Constructs a new {@link ConvertSwitchQuickFixProcessor} */
    public ConvertSwitchQuickFixProcessor() {
    }

    @Override
    public boolean hasCorrections(ICompilationUnit cu, int problemId) {
        return problemId == IProblem.NonConstantExpression;
    }

    @Override
    public IJavaCompletionProposal[] getCorrections(IInvocationContext context,
            IProblemLocation[] location) throws CoreException {
        if (location == null || location.length == 0) {
            return null;
        }
        ASTNode coveringNode = context.getCoveringNode();
        if (coveringNode == null) {
            return null;
        }

        // Look up the fully qualified name of the non-constant expression, if any, and
        // make sure it's R-something.
        if (coveringNode.getNodeType() == ASTNode.SIMPLE_NAME) {
            coveringNode = coveringNode.getParent();
            if (coveringNode == null) {
                return null;
            }
        }
        if (coveringNode.getNodeType() != ASTNode.QUALIFIED_NAME) {
            return null;
        }
        QualifiedName name = (QualifiedName) coveringNode;
        if (!name.getFullyQualifiedName().startsWith("R.")) { //$NON-NLS-1$
            return null;
        }

        IProblemLocation error = location[0];
        int errorStart = error.getOffset();
        int errorLength = error.getLength();
        int caret = context.getSelectionOffset();

        // Even though the hasCorrections() method above will return false for everything
        // other than non-constant expression errors, it turns out this getCorrections()
        // method will ALSO be called on lines where there is no such error. In particular,
        // if you have an invalid cast expression like this:
        //     Button button = findViewById(R.id.textView);
        // then this method will be called, and the expression will pass all of the above
        // checks. However, we -don't- want to show a migrate code suggestion in that case!
        // Therefore, we'll need to check if we're *actually* on a line with the given
        // problem.
        //
        // Unfortunately, we don't get passed the problemId again, and there's no access
        // to it. So instead we'll need to look up the markers on the line, and see
        // if we actually have a constant expression warning. This is not pretty!!

        boolean foundError = false;
        ICompilationUnit compilationUnit = context.getCompilationUnit();
        IResource file = compilationUnit.getResource();
        if (file != null) {
            IDocumentProvider provider = new TextFileDocumentProvider();
            try {
                provider.connect(file);
                IDocument document = provider.getDocument(file);
                if (document != null) {
                    List<IMarker> markers = AdtUtils.findMarkersOnLine(IMarker.PROBLEM,
                            file, document, errorStart);
                    for (IMarker marker : markers) {
                        String message = marker.getAttribute(IMarker.MESSAGE, "");
                        // There are no other attributes in the marker we can use to identify
                        // the exact error, so we'll need to resort to the actual message
                        // text even though that would not work if the messages had been
                        // localized... This can also break if the error messages change. Yuck.
                        if (message.contains("constant expressions")) { //$NON-NLS-1$
                            foundError = true;
                        }
                    }
                }
            } catch (Exception e) {
                AdtPlugin.log(e, "Can't validate error message in %1$s", file.getName());
            } finally {
                provider.disconnect(file);
            }
        }
        if (!foundError) {
            // Not a constant-expression warning, so do nothing
            return null;
        }

        IBuffer buffer = compilationUnit.getBuffer();
        boolean sameLine = false;
        // See if the caret is on the same line as the error
        if (caret <= errorStart) {
            // Search backwards to beginning of line
            for (int i = errorStart; i >= 0; i--) {
                if (i <= caret) {
                    sameLine = true;
                    break;
                }
                char c = buffer.getChar(i);
                if (c == '\n') {
                    break;
                }
            }
        } else {
            // Search forwards to the end of the line
            for (int i = errorStart + errorLength, n = buffer.getLength(); i < n; i++) {
                if (i >= caret) {
                    sameLine = true;
                    break;
                }
                char c = buffer.getChar(i);
                if (c == '\n') {
                    break;
                }
            }
        }

        if (sameLine) {
            String expression = buffer.getText(errorStart, errorLength);
            return new IJavaCompletionProposal[] {
                new MigrateProposal(expression)
            };
        }

        return null;
    }

    /** Proposal for the quick fix which displays an explanation message to the user */
    private class MigrateProposal implements IJavaCompletionProposal {
        private String mExpression;

        private MigrateProposal(String expression) {
            mExpression = expression;
        }

        @Override
        public void apply(IDocument document) {
            Shell shell = AdtPlugin.getShell();
            ConvertSwitchDialog dialog = new ConvertSwitchDialog(shell, mExpression);
            dialog.open();
        }

        @Override
        public Point getSelection(IDocument document) {
            return null;
        }

        @Override
        public String getAdditionalProposalInfo() {
            return "As of ADT 14, resource fields cannot be used as switch cases. Invoke this " +
                    "fix to get more information.";
        }

        @Override
        public String getDisplayString() {
            return "Migrate Android Code";
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
            return 50;
        }
    }
}

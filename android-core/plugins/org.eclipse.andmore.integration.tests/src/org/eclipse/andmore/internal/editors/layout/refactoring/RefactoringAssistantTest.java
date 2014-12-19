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

import static com.android.SdkConstants.FD_RES;
import static com.android.SdkConstants.FD_RES_LAYOUT;

import org.eclipse.andmore.AdtPlugin;

import org.eclipse.andmore.internal.editors.AndroidXmlEditor;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

public class RefactoringAssistantTest extends AdtProjectTest {
	public void testAssistant1() throws Exception {
		// "Extract String"
		checkFixes("sample1a.xml", "<Button android:text=\"Fir^stButton\"");
	}

	public void testAssistant2() throws Exception {
		// Visual refactoring operations
		checkFixes("sample1a.xml", "<Bu^tton android:text");
	}

	public void testAssistant3() throws Exception {
		checkFixes("sample1a.xml", "<Button andr^oid:text=\"FirstButton\"");
	}

	public void testAssistant4() throws Exception {
		// Check for resource rename refactoring (and don't offer extract
		// string)
		checkFixes("sample1a.xml", "android:id=\"@+id/Linea^rLayout2\"");
	}

	private void checkFixes(String name, String caretLocation) throws Exception {
		IProject project = getProject();
		IFile file = getTestDataFile(project, name, FD_RES + "/" + FD_RES_LAYOUT + "/" + name);

		// Determine the offset
		String fileContent = AdtPlugin.readFile(file);
		int caretDelta = caretLocation.indexOf("^");
		assertTrue(caretLocation, caretDelta != -1);
		String caretContext = caretLocation.substring(0, caretDelta)
				+ caretLocation.substring(caretDelta + "^".length());
		int caretContextIndex = fileContent.indexOf(caretContext);
		assertTrue("Caret content " + caretContext + " not found in file", caretContextIndex != -1);
		final int offset = caretContextIndex + caretDelta;

		RefactoringAssistant refactoringAssistant = new RefactoringAssistant();

		// Open file
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		assertNotNull(page);
		IEditorPart editor = IDE.openEditor(page, file);
		assertTrue(editor instanceof AndroidXmlEditor);
		AndroidXmlEditor layoutEditor = (AndroidXmlEditor) editor;
		final ISourceViewer viewer = layoutEditor.getStructuredSourceViewer();

		IQuickAssistInvocationContext invocationContext = new IQuickAssistInvocationContext() {
			@Override
			public int getLength() {
				return 0;
			}

			@Override
			public int getOffset() {
				return offset;
			}

			@Override
			public ISourceViewer getSourceViewer() {
				return viewer;
			}
		};
		ICompletionProposal[] proposals = refactoringAssistant.computeQuickAssistProposals(invocationContext);

		if (proposals != null) {
			for (ICompletionProposal proposal : proposals) {
				assertNotNull(proposal.getAdditionalProposalInfo());
				assertNotNull(proposal.getImage());
			}
		}

		StringBuilder sb = new StringBuilder(1000);
		sb.append("Quick assistant in " + name + " for " + caretLocation + ":\n");
		if (proposals != null) {
			for (ICompletionProposal proposal : proposals) {
				sb.append(proposal.getDisplayString());
				String help = proposal.getAdditionalProposalInfo();
				if (help != null && help.trim().length() > 0) {
					sb.append(" : ");
					sb.append(help.replace('\n', ' '));
				}
				sb.append('\n');
			}
		} else {
			sb.append("None found.\n");
		}
		assertEqualsGolden(name, sb.toString(), "txt");

		// No "apply" test on these assists since they are interactive.
		// Refactoring
		// is tested elsewhere.
	}
}

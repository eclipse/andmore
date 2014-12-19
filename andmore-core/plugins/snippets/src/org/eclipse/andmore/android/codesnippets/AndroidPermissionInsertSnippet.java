/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.eclipse.andmore.android.codesnippets;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.andmore.android.codesnippets.i18n.AndroidSnippetsNLS;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.common.log.UsageDataConstants;
import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.andmore.android.manifest.AndroidProjectManifestFile;
import org.eclipse.andmore.android.model.manifest.AndroidManifestFile;
import org.eclipse.andmore.android.model.manifest.dom.IAndroidManifestProperties;
import org.eclipse.andmore.android.model.manifest.dom.ManifestNode;
import org.eclipse.andmore.android.model.manifest.dom.UsesPermissionNode;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.wst.common.snippets.core.ISnippetItem;
import org.eclipse.wst.common.snippets.core.ISnippetsEntry;
import org.eclipse.wst.common.snippets.internal.ui.SnippetsView;
import org.eclipse.wst.common.snippets.ui.DefaultSnippetInsertion;

public class AndroidPermissionInsertSnippet extends DefaultSnippetInsertion {

	@Override
	protected void doInsert(IEditorPart editorPart, ITextEditor textEditor, IDocument document,
			ITextSelection textSelection) throws BadLocationException {
		String replacement = getInsertString(editorPart.getEditorSite().getShell());
		boolean permissionsAdded = addPermissionToManifest(editorPart, replacement);

		// get the Snippets View
		SnippetsView snippetsView = (SnippetsView) EclipseUtils.getActiveView(AndroidSnippetsStartup.SNIPPETS_VIEW_ID);
		// get the selected snippet
		ISnippetsEntry snippetEntry = snippetsView.getSelectedEntry();
		if (snippetEntry != null) {
			String snippetLabel = snippetEntry.getLabel();

			if (CodeSnippetsPlugin.getDefault() != null) {
				StudioLogger.collectUsageData(UsageDataConstants.WHAT_CODESNIPPET, UsageDataConstants.KIND_CODESNIPPET,
						"Codesnippet '" + snippetLabel + "' used. Permission added: " + permissionsAdded, //$NON-NLS-1$
						AndroidSnippetsStartup.SNIPPETS_VIEW_ID, CodeSnippetsPlugin.getDefault().getBundle()
								.getVersion().toString());
			}
		}

		super.doInsert(editorPart, textEditor, document, textSelection);
	}

	/**
	 * If the snippetText contains comment to insert uses-permission, then it
	 * adds uses-permission to androidmanifest file
	 * 
	 * @param editorPart
	 *            editor
	 * @param snippetText
	 *            text to drop
	 */
	private boolean addPermissionToManifest(IEditorPart editorPart, String snippetText) {
		boolean needToAddPermission = snippetText.contains("AndroidManifest.xml must have the following permission:"); //$NON-NLS-1$
		boolean shouldAddToManifest = false;
		if (needToAddPermission) {

			List<String> neededPermissions = getNeededPermissions(snippetText);
			List<String> permissionsToBeAdded = new ArrayList<String>(neededPermissions.size());

			IEditorInput input = editorPart.getEditorInput();
			FileEditorInput fileEditorInput = null;
			ManifestNode manifestNode = null;
			if (input instanceof FileEditorInput) {
				fileEditorInput = (FileEditorInput) input;
				IProject project;
				IFile file = fileEditorInput.getFile();
				project = file.getProject();
				try {
					AndroidManifestFile androidManifestFile = AndroidProjectManifestFile.getFromProject(project);
					manifestNode = androidManifestFile.getManifestNode();
				} catch (Exception e) {
					// Do nothing, just ask for the permissions.
				}
			}

			if (manifestNode != null) {
				for (String neededPermission : neededPermissions) {
					if (!permAlreadyExists(manifestNode, neededPermission)) {
						permissionsToBeAdded.add(neededPermission);
					}
				}
			}

			if (!permissionsToBeAdded.isEmpty()) {

				StringBuilder permMsgBuilder = new StringBuilder();
				for (String neededPermission : permissionsToBeAdded) {
					permMsgBuilder.append(AndroidSnippetsNLS.AndroidPermissionInsertSnippet_PermissionPrefix);
					permMsgBuilder.append(neededPermission);
					permMsgBuilder.append(AndroidSnippetsNLS.AndroidPermissionInsertSnippet_PermissionSuffix);
				}

				// Ask user permission
				shouldAddToManifest = EclipseUtils.showQuestionDialog(
						AndroidSnippetsNLS.AndroidPermissionInsertSnippet_Msg_AddToManifest_Title, NLS.bind(
								AndroidSnippetsNLS.AndroidPermissionInsertSnippet_Msg_AddToManifest_Msg,
								permMsgBuilder.toString()));

				if (shouldAddToManifest) {
					AndroidManifestFile androidManifestFile = null;
					manifestNode = null;
					if (fileEditorInput != null) {
						addPermissionToManifest(permissionsToBeAdded, fileEditorInput, androidManifestFile,
								manifestNode);
					}

				}
			}
		}
		return shouldAddToManifest;
	}

	private List<String> getNeededPermissions(String snippetText) {
		// search each <uses-permission tag
		StringTokenizer lineToken = new StringTokenizer(snippetText, "\n\r"); //$NON-NLS-1$
		List<String> neededPermissions = new ArrayList<String>(lineToken.countTokens());
		while (lineToken.hasMoreTokens()) {
			String line = lineToken.nextToken();
			if (line.contains("<uses-permission")) //$NON-NLS-1$
			{
				String permNameToAdd = null;
				String androidNameStr = "android:name=\""; //$NON-NLS-1$
				int beginIndex = line.indexOf(androidNameStr);
				int endIndex = line.indexOf("\"/>"); //$NON-NLS-1$
				if ((beginIndex > 0) && (endIndex > 0)) {
					permNameToAdd = line.substring(beginIndex + androidNameStr.length(), endIndex);
					neededPermissions.add(permNameToAdd);
				} else {
					// log malformed permission statement
					StudioLogger.error(AndroidPermissionInsertSnippet.class,
							"Permission code snippet was not in the right format to enable insert of uses-permission on androidmanifest.xml" //$NON-NLS-1$
									+ snippetText);
				}
			}
		}

		return neededPermissions;
	}

	/**
	 * If the snippetText contains comment to insert uses-permission, then it
	 * asks user to add uses-permission to androidmanifest file
	 * 
	 * @param snippetText
	 *            text to drop
	 * @param input
	 *            editor
	 * @param androidManifestFile
	 *            file to update
	 * @param manifestNode
	 *            node to update
	 */
	private void addPermissionToManifest(List<String> neededPermissions, IEditorInput input,
			AndroidManifestFile androidManifestFile, ManifestNode manifestNode) {
		IProject project;
		IFile file = ((FileEditorInput) input).getFile();
		project = file.getProject();
		try {
			androidManifestFile = AndroidProjectManifestFile.getFromProject(project);
			manifestNode = androidManifestFile.getManifestNode();

			for (String neededPermission : neededPermissions) {
				if (!permAlreadyExists(manifestNode, neededPermission)) {
					// append permission node
					UsesPermissionNode usesPermissionNode = new UsesPermissionNode(neededPermission);
					manifestNode.addChild(usesPermissionNode);
				}
			}

			AndroidProjectManifestFile.saveToProject(project, androidManifestFile, true);
		} catch (Exception e) {
			StudioLogger.error(AndroidPermissionInsertSnippet.class,
					"Error adding snippet permissions to androidmanifest.xml.", e); //$NON-NLS-1$
		}
	}

	private boolean permAlreadyExists(ManifestNode manifestNode, String neededPermission) {
		// Check if permissions does not exist yet
		boolean permAlreadyExists = false;
		List<UsesPermissionNode> permissionsNode = manifestNode.getUsesPermissionNodes();

		if (permissionsNode != null) {
			for (UsesPermissionNode existentPermissionNode : permissionsNode) {
				String permName = existentPermissionNode.getNodeProperties().get(IAndroidManifestProperties.PROP_NAME);
				if ((permName != null) && permName.equals(neededPermission)) {
					permAlreadyExists = true;
					break;
				}
			}
		}
		return permAlreadyExists;
	}

	@Override
	public void dragSetData(DragSourceEvent event, ISnippetItem item) {
		IEditorPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		addPermissionToManifest(part, item.getContentString());
		super.dragSetData(event, item);
	}
}

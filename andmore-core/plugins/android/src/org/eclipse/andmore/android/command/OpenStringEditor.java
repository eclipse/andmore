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

package org.eclipse.andmore.android.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.andmore.android.AndroidPlugin;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.common.log.UsageDataConstants;
import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.andmore.android.i18n.AndroidNLS;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.sequoyah.localization.tools.datamodel.LocaleInfo;
import org.eclipse.sequoyah.localization.tools.extensions.classes.ILocalizationSchema;
import org.eclipse.sequoyah.localization.tools.managers.LocalizationManager;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ListDialog;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.ide.IDE.SharedImages;
import org.eclipse.ui.part.FileEditorInput;
import org.w3c.dom.Document;

/**
 * Open the Android Localization Files Editor
 */
public class OpenStringEditor extends AbstractHandler {
	public static String STRING_EDITOR_ID = "org.eclipse.sequoyah.localization.tools.extensions.implementation.android.localizationEditor";

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		final List<IProject> supportedProjects = LocalizationManager.getInstance().getSupportedProjects();

		if (supportedProjects.size() == 0) {
			EclipseUtils.showErrorDialog(AndroidNLS.ERR_Localization_NoProjects_Title,
					AndroidNLS.ERR_Localization_NoProjects_Description);
		} else {

			Shell shell = HandlerUtil.getActiveShell(event);

			final ListDialog dialog = new ListDialog(shell);

			dialog.setContentProvider(new IStructuredContentProvider() {

				@Override
				public void dispose() {
					// do nothing
				}

				@Override
				public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
					// do nothing
				}

				@Override
				public Object[] getElements(Object inputElement) {
					return supportedProjects.toArray(new Object[supportedProjects.size()]);
				}
			});

			dialog.setLabelProvider(new ILabelProvider() {

				@Override
				public void removeListener(ILabelProviderListener listener) {
					// do nothing
				}

				@Override
				public boolean isLabelProperty(Object element, String property) {
					return false;
				}

				@Override
				public void dispose() {
					// do nothing
				}

				@Override
				public void addListener(ILabelProviderListener listener) {
					// do nothing
				}

				@Override
				public String getText(Object element) {
					IProject project = (IProject) element;
					return project.getName();
				}

				@Override
				public Image getImage(Object element) {
					return PlatformUI.getWorkbench().getSharedImages().getImage(SharedImages.IMG_OBJ_PROJECT);
				}
			});

			dialog.setInput(supportedProjects);

			dialog.setTitle(AndroidNLS.UI_Project_Selection);
			dialog.create();
			dialog.getOkButton().setEnabled(false);
			dialog.getTableViewer().addSelectionChangedListener(new ISelectionChangedListener() {

				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					dialog.getOkButton().setEnabled(!event.getSelection().isEmpty());
				}
			});
			dialog.open();
			Object[] result = dialog.getResult();

			if ((result != null) && (result.length > 0)) {

				IProject project = (IProject) result[0];
				try {
					ILocalizationSchema localizationSchema = LocalizationManager.getInstance().getLocalizationSchema(
							project);

					if (localizationSchema != null) {
						Map<LocaleInfo, IFile> files = localizationSchema.getLocalizationFiles(project);
						if (files.size() > 0) {
							List<String> malformedXMLFiles = new ArrayList<String>();
							for (IFile file : files.values()) {
								try {
									// Before opening check if XML is valid
									DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
									DocumentBuilder builder = factory.newDocumentBuilder();
									Document document = builder.parse(file.getContents());
								} catch (Exception e) {
									malformedXMLFiles.add(file.getFullPath().toPortableString());
								}
							}
							if (malformedXMLFiles.isEmpty()) {
								// no malformed files - proceed opening editor
								IFile inputFile = new ArrayList<IFile>(files.values()).get(0);

								final FileEditorInput fileEditor = new FileEditorInput(inputFile);

								final IWorkbenchWindow wbWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
								final IWorkbenchPage wbPage = wbWindow.getActivePage();

								wbPage.openEditor(fileEditor, STRING_EDITOR_ID);

								// listen to project close event
								ResourcesPlugin.getWorkspace().addResourceChangeListener(
										new ProjectCloseListener(fileEditor, wbWindow, wbPage),
										IResourceChangeEvent.PRE_CLOSE);
							} else {
								StudioLogger.error("Cannot open Localization Files Editor - XML(s) Malformed: "
										+ malformedXMLFiles);
								EclipseUtils.showErrorDialog(AndroidNLS.ERR_Localization_XMLMalformed_Title, NLS.bind(
										AndroidNLS.ERR_Localization_XMLMalformed_Description,
										malformedXMLFiles.toString()));
							}
						} else {
							EclipseUtils.showErrorDialog(AndroidNLS.ERR_Localization_NoFiles_Title,
									AndroidNLS.ERR_Localization_NoFiles_Description);
						}

					}

					// UDC log
					StudioLogger.collectUsageData("Localization Editor openned", //$NON-NLS-1$
							"Localization Editor", UsageDataConstants.DESCRIPTION_DEFAULT, //$NON-NLS-1$
							AndroidPlugin.PLUGIN_ID, AndroidPlugin.getDefault().getBundle().getVersion().toString());
				} catch (PartInitException e) {
					StudioLogger.error("Cannot open Localization Files Editor");
				}
			}
		}

		return null;
	}

	/**
	 * This listener handles a project close event. It closes the location files
	 * editor associated with the project, in case it is still opened.
	 */
	private class ProjectCloseListener implements IResourceChangeListener {
		private FileEditorInput fileEditor;

		private IWorkbenchWindow wbWindow;

		private IWorkbenchPage wbPage;

		public ProjectCloseListener(FileEditorInput fileEditor, IWorkbenchWindow wbWindow, IWorkbenchPage wbPage) {
			this.fileEditor = fileEditor;
			this.wbWindow = wbWindow;
			this.wbPage = wbPage;
		}

		@Override
		public void resourceChanged(IResourceChangeEvent event) {
			IProject closedProject = (IProject) event.getResource();

			if (fileEditor.getFile().getProject().equals(closedProject)) {
				final IEditorPart part = wbPage.findEditor(fileEditor);

				// still opened
				if (part != null) {
					// editor must be closed by a UI thread
					wbWindow.getShell().getDisplay().syncExec(new Runnable() {
						@Override
						public void run() {
							// saves the editor
							wbPage.closeEditor(part, true);
						}
					});
				}
				// removes listener from workspace
				ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
			}
		}
	}
}

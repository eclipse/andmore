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

package org.eclipse.andmore.android.generatemenucode.ui;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;

import org.eclipse.andmore.android.codeutils.CodeUtilsActivator;
import org.eclipse.andmore.android.codeutils.i18n.CodeUtilsNLS;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.common.log.UsageDataConstants;
import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.andmore.android.generatecode.JDTUtils;
import org.eclipse.andmore.android.generatemenucode.model.codegenerators.JavaModifierBasedOnMenu;
import org.eclipse.andmore.android.generateviewbylayout.ui.AbstractCodeGeneratorHandler;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Command handler to start code generation based on menu
 */
public class GenerateMenuCodeHandler extends AbstractHandler implements IHandler {

	private static final String WIZARD_IMAGE_PATH = "icons/wizban/fill_activity_ban.png"; //$NON-NLS-1$

	/**
	 * Open {@link GenerateMenuCodeDialog} and use
	 * {@link JavaModifierBasedOnMenu} to insert code into Android source code
	 * (Activity/Fragment).
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		SelectionBean selectionBean = resolveSelection(event);

		if (selectionBean.isProject() || selectionBean.isAllowedClassInstance()) {

			final IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);

			GenerateMenuCodeDialog menuDialog = new GenerateMenuCodeDialog(window.getShell(),
					CodeUtilsNLS.GenerateMenuCodeDialog_DefaultMessage, CodeUtilsNLS.GenerateMenuCodeDialog_Title,
					CodeUtilsNLS.GenerateMenuCodeDialog_ShellTitle, CodeUtilsActivator.getImageDescriptor(
							WIZARD_IMAGE_PATH).createImage());

			final JavaModifierBasedOnMenu modifier = new JavaModifierBasedOnMenu();
			menuDialog.init(modifier, selectionBean.getJavaProject(), selectionBean.getJavaFile());

			int status = menuDialog.open();
			if (status == Window.OK) {
				ICompilationUnit compilationUnit = menuDialog.getJavaFile();
				IEditorPart editor = null;
				try {
					editor = JavaUI.openInEditor(compilationUnit);
				} catch (Exception e) {
					StudioLogger
							.warn(GenerateMenuCodeHandler.class,
									"Unable to open editor or bring it to front for Java file while trying to generate menu code based on xml file", //$NON-NLS-1$
									e);
				}
				final ProgressMonitorDialog dialog = new ProgressMonitorDialog(menuDialog.getShell());
				final IEditorPart editorPart = editor;

				PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {

					@Override
					public void run() {
						try {

							dialog.run(true, false, new IRunnableWithProgress() {

								@Override
								public void run(IProgressMonitor monitor) throws InvocationTargetException,
										InterruptedException {
									try {
										// collect usage data - UDC
										StudioLogger.collectUsageData(UsageDataConstants.WHAT_VIEW_BY_MENU_EXEC,
												UsageDataConstants.KIND_VIEW_BY_MENU_EXEC,
												"View by menu feature executed.", //$NON-NLS-1$
												CodeUtilsActivator.PLUGIN_ID, CodeUtilsActivator.getDefault()
														.getBundle().getVersion().toString());
										modifier.insertCode(monitor, editorPart);
									} catch (final JavaModelException e) {
										final MultiStatus errorStatus = new MultiStatus(CodeUtilsActivator.PLUGIN_ID,
												IStatus.ERROR,
												"Error inserting code on activity/fragment based on menu", //$NON-NLS-1$
												null);
										errorStatus.merge(e.getStatus());

										PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {

											@Override
											public void run() {
												IStatus mostSevere = EclipseUtils.findMostSevereError(errorStatus);
												ErrorDialog.openError(PlatformUI.getWorkbench().getDisplay()
														.getActiveShell(),
														"Error inserting code on activity/fragment based on menu", //$NON-NLS-1$
														e.getMessage(), mostSevere);
											}
										});
										StudioLogger.error(this.getClass(),
												"Error inserting code on activity/fragment based on menu" //$NON-NLS-1$
														+ ": " + e.getMessage()); //$NON-NLS-1$
									}
								}
							});
						} catch (Exception e) {
							StudioLogger.error(this.getClass(),
									"Error inserting code on activity/fragment based on menu" //$NON-NLS-1$
											+ ": " + e.getMessage()); //$NON-NLS-1$
						}
					}
				});
			}
		} else {
			EclipseUtils.showErrorDialog(CodeUtilsNLS.GenerateMenuCodeDialog_Title,
					CodeUtilsNLS.GenerateMenuCodeHandler_SelectedClassNeitherActivityFragment);// GenerateMenuCodeHandler_SelectedClassNeitherActivityFragment
		}

		return null;
	}

	protected static SelectionBean resolveSelection(ExecutionEvent event) throws ExecutionException {
		SelectionBean selectionBean = new SelectionBean();
		ITextEditor editor = null;
		IFileEditorInput fileEditorInput = null;

		ISelection selection = HandlerUtil.getCurrentSelection(event);

		// case where the selection comes from the Text Editor
		if (selection instanceof TextSelection) {
			editor = (ITextEditor) HandlerUtil.getActiveEditorChecked(event);
			if (editor.getEditorInput() instanceof IFileEditorInput) {
				fileEditorInput = (IFileEditorInput) editor.getEditorInput();
				selectionBean.setJavaFile(fileEditorInput.getFile());
			}
		} else if (selection instanceof IStructuredSelection) {
			Iterator<?> selectionIterator = ((IStructuredSelection) selection).iterator();
			Object selectedObject = selectionIterator.next();

			// case where the selection comes from the package explorer
			if (selectedObject instanceof IFile) {
				selectionBean.setJavaFile((IFile) selectedObject);
			}

			// again, case where the selection comes from the package explorer
			else if (selectedObject instanceof ICompilationUnit) {
				ICompilationUnit compilationUnit = (ICompilationUnit) selectedObject;
				selectionBean.setJavaFile((IFile) compilationUnit.getResource());
			}

			// case where the selection comes from a project
			else if (selectedObject instanceof IAdaptable) {
				try {
					IResource resource = (IResource) ((IAdaptable) selectedObject).getAdapter(IResource.class);
					selectionBean.setJavaProject(resource.getProject());
					selectionBean.setProject(true);
				} catch (Exception ex) {
					StudioLogger.error(AbstractCodeGeneratorHandler.class, "Error retrieving class information", ex); //$NON-NLS-1$
					throw new RuntimeException(
							CodeUtilsNLS.GenerateMenuCodeHandler_Error_CannotRetrieveClassInformation, ex);
				}
			}
		}

		// just check classes in case classes were selected, not project
		if (!selectionBean.isProject()) {
			try {
				// the selected class must be either an Activity or a Fragment
				selectionBean.setAllowedClassInstance(JDTUtils.isSubclass(selectionBean.getJavaFile(),
						"android.app.Activity") //$NON-NLS-1$
						|| JDTUtils.isFragmentSubclass(selectionBean.getJavaFile())
						|| JDTUtils.isCompatibilityFragmentSubclass(selectionBean.getJavaFile()));
			} catch (JavaModelException jme) {
				StudioLogger.error(AbstractCodeGeneratorHandler.class, "Error retrieving class information", jme); //$NON-NLS-1$
				throw new RuntimeException(CodeUtilsNLS.GenerateMenuCodeHandler_Error_CannotRetrieveClassInformation,
						jme);
			}
		}
		return selectionBean;
	}
}

class SelectionBean {
	private boolean isProject = false;

	private boolean isAllowedClassInstance = false;

	private IFile javaFile = null;

	private IProject javaProject = null;

	public boolean isProject() {
		return isProject;
	}

	public void setProject(boolean isProject) {
		this.isProject = isProject;
	}

	/**
	 * @return true if activity or fragment, false otherwise
	 */
	public boolean isAllowedClassInstance() {
		return isAllowedClassInstance;
	}

	public void setAllowedClassInstance(boolean isAllowedClassInstance) {
		this.isAllowedClassInstance = isAllowedClassInstance;
	}

	/**
	 * @return selected Android file (Activity or Fragment)
	 */
	public IFile getJavaFile() {
		return javaFile;
	}

	/**
	 * Sets the Android file selected (Activity or Fragment)
	 * 
	 * @param javaFile
	 */
	public void setJavaFile(IFile javaFile) {
		this.javaFile = javaFile;
		javaProject = javaFile.getProject();
	}

	/**
	 * @return the project where the Android file (Activity or Fragment) is
	 *         located
	 */
	public IProject getJavaProject() {
		return javaProject;
	}

	public void setJavaProject(IProject javaProject) {
		this.javaProject = javaProject;
	}
}

/*******************************************************************************
 * Copyright (c) 2010 Motorola, Inc. All rights reserved.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Initial contributors:
 * Paulo Renato de Faria (Eldorado)
 * 
 * Contributors:
 * Carlos Alberto Souto Junior (Eldorado) - [317327] Major UI bugfixes and improvements in Android Native support
 *******************************************************************************/

package org.eclipse.sequoyah.android.cdt.internal.build.ui;

import java.io.File;
import java.util.Iterator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.sequoyah.android.cdt.build.core.NDKUtils;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.progress.IProgressService;
import org.eclipse.ui.views.navigator.ResourceComparator;

/**
 * Handler to deal with JNI c and h generation from Java classes using javah  
 * @author Paulo Renato de Faria
 *
 */
public class NDKJavaToCppGeneratorHandler extends AbstractHandler implements IHandler
{
    /**
     * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
     */
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        ISelection selection =
                PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService()
                        .getSelection();

        IStructuredSelection structuredSelection = null;
        boolean hasChosenOutputFolder = false;
        String folderChosen = null;
        if (selection instanceof IStructuredSelection)
        {
            structuredSelection = (IStructuredSelection) selection;
            Iterator iter = structuredSelection.iterator();
            while (iter.hasNext())
            {
                Object obj = iter.next();
                if (obj instanceof ICompilationUnit)
                {
                    ICompilationUnit javaFile = (ICompilationUnit) obj;

                    if (!hasChosenOutputFolder)
                    {
                        folderChosen = openDirectoryChooser(javaFile.getJavaProject().getProject());
                    }

                    if (folderChosen != null)
                    {
                        //if user do not select a folder, use the default (the java source folder)
                        hasChosenOutputFolder = true;
                        generateJniSourceFiles(folderChosen, javaFile);
                    }
                }
            }
        }
        else if (selection instanceof TextSelection)
        {
            IEditorInput input =
                    PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
                            .getActiveEditor().getEditorInput();
            if (input instanceof IFileEditorInput)
            {
                IFile file = ((IFileEditorInput) input).getFile();
                ICompilationUnit javaFile = JavaCore.createCompilationUnitFrom(file);
                if (!hasChosenOutputFolder)
                {
                    folderChosen = openDirectoryChooser(javaFile.getJavaProject().getProject());
                }

                if (folderChosen != null)
                {
                    //if user do not select a folder, use the default (the java source folder)
                    hasChosenOutputFolder = true;
                    generateJniSourceFiles(folderChosen, javaFile);
                }
            }
        }
        return null;
    }

    /**
     * Opens dialog to choose directory
     * @return path to directory
     */
    private String openDirectoryChooser(IProject project)
    {
        String folderChosen = null;

        Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
        final ElementTreeSelectionDialog packageDialog =
                new ElementTreeSelectionDialog(shell, new WorkbenchLabelProvider(),
                        new WorkbenchContentProvider());

        packageDialog
                .setTitle(Messages.JNI_SOURCE_HEADER_CREATION_MONITOR_DIRECTORY_SELECTION_TITLE);
        packageDialog
                .setMessage(Messages.JNI_SOURCE_HEADER_CREATION_MONITOR_DIRECTORY_SELECTION_TITLE);

        packageDialog.setInput(ResourcesPlugin.getWorkspace().getRoot());
        packageDialog.setComparator(new ResourceComparator(ResourceComparator.NAME));


        if (project != null)
        {
            IResource jniFolder = project.findMember(NDKUtils.DEFAULT_JNI_FOLDER_NAME);
            if (jniFolder != null)
            {
                packageDialog.setInitialSelection(jniFolder);
            }
            else
            {
                packageDialog.setInitialSelection(project);
            }
        }

        //filter extensions
        packageDialog.addFilter(new ViewerFilter()
        {
            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element)
            {
                if (element instanceof IProject)
                {
                    return true;
                }
                if (element instanceof IFolder)
                {
                    return true;
                }
                return false;
            }
        });
        //user can select only one FILE
        packageDialog.setValidator(new ISelectionStatusValidator()
        {
            public IStatus validate(Object[] selection)
            {
                IStatus valid = new Status(IStatus.ERROR, UIPlugin.PLUGIN_ID, ""); //$NON-NLS-1$
                if (selection.length == 1)
                {
                    if (selection[0] instanceof IFolder)
                    {
                        valid = new Status(IStatus.OK, UIPlugin.PLUGIN_ID, ""); //$NON-NLS-1$
                    }
                }
                return valid;

            }
        });

        if (packageDialog.open() == IDialogConstants.OK_ID)
        {
            IResource resource = (IResource) packageDialog.getFirstResult();
            if (resource instanceof IFolder)
            {
                IFolder folder = (IFolder) resource;
                folderChosen = folder.getLocation().toOSString();
            }
        }

        return folderChosen;
    }

    /**
     * Creates source and header source files
     * @param folderChosen
     * @param javaFile
     */
    private void generateJniSourceFiles(final String folderChosen, final ICompilationUnit javaFile)
    {
        // Use the progess service to execute the runnable
        IProgressService service = PlatformUI.getWorkbench().getProgressService();
        try
        {
            service.run(true, false, new IRunnableWithProgress()
            {
                public void run(IProgressMonitor monitor)
                {
                    if (monitor == null)
                    {
                        monitor = new NullProgressMonitor();
                    }
                    monitor.beginTask(Messages.JNI_C_FILES_CREATION_MONITOR_TASK_NAME, 100);

                    if (NDKJavaToCppGenerator.checkJavaSdkExistence())
                    {
                        if (folderChosen != null)
                        {
                            IResource file;
                            try
                            {
                                file = javaFile.getCorrespondingResource();
                                IProject project = file.getProject();
                                //need to get selected java resource
                                int index = file.getName().indexOf(file.getFileExtension());
                                //remove .java 
                                String classname = file.getName().substring(0, index - 1);

                                //need to get selected java parent folder
                                String classPackage = "";
                                String fullPathToClass = file.getLocation().toOSString();
                                int indexLinuxMac = fullPathToClass.indexOf("src/");
                                if (indexLinuxMac > 0)
                                {
                                    classPackage =
                                            fullPathToClass.substring(indexLinuxMac + 4,
                                                    fullPathToClass.length());
                                }
                                int indexWin = file.getLocation().toOSString().indexOf("src\\");
                                if (indexWin > 0)
                                {
                                    classPackage =

                                            fullPathToClass.substring(indexWin + 4,
                                                    fullPathToClass.length());
                                }
                                classPackage = classPackage.replace(classname + ".java", "");
                                classPackage = classPackage.replace(File.separator, ".");
                                String outputDirectory =
                                        folderChosen != null ? folderChosen : file.getParent()

                                                .getLocation().toOSString();
                                monitor.worked(10);
                                NDKJavaToCppGenerator androidNDKJavaToCppGenerator =
                                        new NDKJavaToCppGenerator(project, classname, classPackage,
                                                outputDirectory);

                                androidNDKJavaToCppGenerator
                                        .generateCppSourceAndHeader(new SubProgressMonitor(monitor,
                                                90));
                            }
                            catch (Exception e)
                            {

                                String title =
                                        Messages.JNI_SOURCE_HEADER_CREATION_MONITOR_FILES_ERROR;
                                MessageUtils.showErrorDialog(title, e.getLocalizedMessage());

                                UIPlugin.log(title, e);
                            }
                        }
                    }
                    else
                    {
                        monitor.worked(100);
                        // Inform the user that he needs the Java SDK to perform this operation
                        MessageUtils.showErrorDialog(Messages.ERR_JNI_JDK_Not_Found_Dialog_Title,
                                Messages.ERR_JNI_JDK_Not_Found);
                    }
                    monitor.done();
                }
            });
        }
        catch (Exception e)
        {
            // do nothing
        }
    }
}

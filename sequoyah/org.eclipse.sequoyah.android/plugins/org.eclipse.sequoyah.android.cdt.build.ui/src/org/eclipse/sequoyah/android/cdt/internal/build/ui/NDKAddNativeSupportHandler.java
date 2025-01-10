/*******************************************************************************
 * Copyright (c) 2010 Motorola, Inc. All rights reserved.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Carlos Alberto Souto Junior - Initial Contribution
 * Marcelo Marzola Bossoni - [318712] Make run wizard an static method and make the add native support calls this method
 *******************************************************************************/
package org.eclipse.sequoyah.android.cdt.internal.build.ui;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

public class NDKAddNativeSupportHandler extends AbstractHandler implements
		IHandler {
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbench workbench = PlatformUI.getWorkbench();

		if ((workbench != null) && !workbench.isClosing()) {
			IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();

			if (window != null) {
				IWorkbenchPage activePage = window.getActivePage();
				if (activePage != null) {
					IWorkbenchPart part = activePage.getActivePart();
					ISelection selection = window.getSelectionService()
							.getSelection();
					IStructuredSelection structureSelection = null;
					if (selection instanceof IStructuredSelection) {
						structureSelection = (IStructuredSelection) selection;
					} else {
						structureSelection = new StructuredSelection();
					}

					Object selectionElement = structureSelection
							.getFirstElement();

					if (selectionElement == null) {
						AddNativeSupport.runWizard(null, part);
					} else {
						IResource resource = null;
						if (selectionElement instanceof IResource) {
							resource = (IResource) selectionElement;
						} else if (selectionElement instanceof IAdaptable) {
							try {
								resource = (IResource) ((IAdaptable) selectionElement)
										.getAdapter(IResource.class);
							} catch (Exception e) {

							}
						}

						if (resource != null) {
							AddNativeSupport.runWizard(resource.getProject(),
									part);
						}
					}

				}

			}
		}
		return workbench;
	}
}

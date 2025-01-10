/*******************************************************************************
 * Copyright (c) 2008-2010 MontaVista Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Yu-Fen Kuo (MontaVista) - initial API and implementation
 * Fabio Fantato (Instituto Eldorado) - [263188] - Create new examples to support tutorial presentation
 * Fabio Fantato (Instituto Eldorado) - [243494] Change the reference implementation to work on Galileo
 * Fabio Rigo (Eldorado) - Bug [288006] - Unify features of InstanceManager and InstanceRegistry
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 *******************************************************************************/
package org.eclipse.sequoyah.device.framework.ui.wizard;

import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.sequoyah.device.common.utilities.exception.ExceptionHandler;
import org.eclipse.sequoyah.device.common.utilities.exception.SequoyahException;
import org.eclipse.sequoyah.device.framework.manager.InstanceManager;
import org.eclipse.sequoyah.device.framework.model.IInstanceBuilder;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

/*
 * new device instance wizard.
 */
public class NewDeviceMenuWizard extends NewDeviceWizard implements INewWizard {

	public static String NEW_DEVICE_MENU_DEFAULT_PAGE_ID = "$_default_page_$"; //$NON-NLS-1$
	private String currentDeviceTypeId;
	private DeviceTypeCustomMenuWizardPageHandler customWizardPageHandler = new DeviceTypeCustomMenuWizardPageHandler(
			this);
	private DefaultDeviceTypeMenuWizardPage firstPage;
	private Properties properties;

	public void addPages() {
		firstPage = new DefaultDeviceTypeMenuWizardPage(NEW_DEVICE_MENU_DEFAULT_PAGE_ID, getCurrentDeviceTypeId());
		addPage(firstPage);
		setWindowTitle(DeviceWizardResources.SEQUOYAH_NewDeviceMenuWizard_Window_Title + firstPage.getDeviceType().getLabel());
		super.addPages();
	}

	public boolean performFinish() {
		updatePropertiesFromWizardPages();

		boolean ok = false;
		try {
			final IInstanceBuilder projectBuilder = getProjectBuilder();
			final IRunnableWithProgress runnable = new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException {
					try {
						InstanceManager.createProject(
								firstPage.getDeviceType(), projectBuilder, monitor);
					} catch (SequoyahException e) {
						ExceptionHandler.showException(e);
					}				
				}
			};
			getContainer().run(false, true, runnable);
			ok = true;
		} catch (InvocationTargetException x) {
			x.printStackTrace();
		} catch (InterruptedException x) {
			x.printStackTrace();
		}
		if (ok)
			return customWizardPageHandler.performFinish();
		return false;

	}

	public IInstanceBuilder getProjectBuilder() {
		return new InstanceBuilder(firstPage.getInstanceName(),
				getProperties());

	}

	public Properties getProperties() {
		return properties;
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
		//do nothing
	}

	public String getCurrentDeviceTypeId() {
		return currentDeviceTypeId;
	}

	public void setCurrentDeviceTypeId(String currentDeviceTypeId) {
		this.currentDeviceTypeId = currentDeviceTypeId;
	}

	public IWizardPage getNextPage(IWizardPage page) {
		IWizardPage nextPage = customWizardPageHandler.getNextPage(page);
		addCustomPage(nextPage);
		return nextPage;
	}

	public IWizardPage getPreviousPage(IWizardPage page) {
		return customWizardPageHandler.getPreviousPage(page);
	}

	private void addCustomPage(IWizardPage customPage) {
		if (customPage == null)
			return;
		IWizardPage[] allPages = this.getPages();
		boolean customPageFound = false;
		for (int i = 0; i < allPages.length && !customPageFound; i++) {
			if (allPages[i].equals(customPage))
				customPageFound = true;
		}
		if (!customPageFound) {
			addPage(customPage);
		}
	}

	private void updatePropertiesFromWizardPages() {
		properties = new Properties();
		// properties.putAll(firstPage.getProperties());
		for (IWizardPage page = firstPage; page != null; page = getNextPage(page)) {
			if (page instanceof IInstanceProperties) {
				properties.putAll(((IInstanceProperties) page).getProperties());
			}

		}

	}

	public boolean canFinish() {
		return (firstPage.isPageComplete() && customWizardPageHandler
				.canFinish());
	}

	/*
	@Override
	public Image getDefaultPageImage() {
		return DeviceUIPlugin.getDefault().getImageRegistry().get(
				DeviceUIPlugin.IMAGEKEY_NEW_DEVICE_WIZARD);
	}
	*/
}

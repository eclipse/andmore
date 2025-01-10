/*******************************************************************************
 * Copyright (c) 2008-2010 MontaVista Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Yu-Fen Kuo (MontaVista) - initial API and implementation
 * Yu-Fen Kuo (MontaVista)  - [236476] - provide a generic device type
 * Fabio Fantato (Instituto Eldorado) - [263188] - Create new examples to support tutorial presentation
 * Fabio Fantato (Instituto Eldorado) - [243494] Change the reference implementation to work on Galileo
 * Daniel Barboza Franco (Instituto Eldorado) - [268887] - Cannot access IWizardPage from IRunnableWithProgress
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 *******************************************************************************/
package org.eclipse.sequoyah.device.framework.ui.wizard;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.sequoyah.device.framework.factory.DeviceTypeRegistry;
import org.eclipse.sequoyah.device.framework.model.IDeviceType;
import org.eclipse.sequoyah.device.framework.ui.internal.model.DeviceTypeCustomWizardPage;
import org.eclipse.sequoyah.device.framework.ui.model.IDeviceTypeCustomWizardPage;

/*
 * handler used by the new device instance wizard. It tries to find the custom
 * pages defined from the new device wizard pages extension point and add them
 * dynamically when the specified device type is selected by user.
 */
public class DeviceTypeCustomMenuWizardPageHandler {
	private NewDeviceMenuWizard wizard;
	private static final String WIZARD_PAGES_EXTENSION_POINT_ID = "org.eclipse.sequoyah.device.framework.ui.newDeviceWizardPages"; //$NON-NLS-1$
	private static final String XML_ELEMENT_WIZARD_PAGE = "wizardPage"; //$NON-NLS-1$
	private static final String XML_ATTRIBUTE_ID = "id"; //$NON-NLS-1$
	private static final String XML_ATTRIBUTE_OPERATION_CLASS = "operationClass"; //$NON-NLS-1$
	private static final String XML_ATTRIBUTE_PAGE_CLASS = "pageClass"; //$NON-NLS-1$
	private static final String XML_ELEMENT_DEVICE_TYPE = "deviceType"; //$NON-NLS-1$
	private static final String XML_ATTRIBUTE_DEVICE_TYPE_ID = "deviceTypeId"; //$NON-NLS-1$

	private static Map<String, ArrayList<IDeviceTypeCustomWizardPage>> customPages = new HashMap<String, ArrayList<IDeviceTypeCustomWizardPage>>();

	public DeviceTypeCustomMenuWizardPageHandler(NewDeviceMenuWizard wizard) {
		this.wizard = wizard;
	}

	public IWizardPage getNextPage(IWizardPage currentPage) {
		loadExtensionsSynchronized();
		ArrayList<IDeviceTypeCustomWizardPage> pages = customPages.get(wizard
				.getCurrentDeviceTypeId());
		if (pages == null)
			return null;
		if (currentPage instanceof DefaultDeviceTypeMenuWizardPage) {
			return pages.get(0).getPageClass();
		} else {

			for (int i = 0; i < pages.size() - 1; i++) {
				IDeviceTypeCustomWizardPage page = pages.get(i);
				if (page.getPageClass().equals(currentPage))
					return pages.get(i + 1).getPageClass();

			}

		}
		return null;
	}

	public IWizardPage getPreviousPage(IWizardPage currentPage) {
		loadExtensionsSynchronized();
		if (currentPage instanceof DefaultDeviceTypeMenuWizardPage) {
			return null;
		}
		ArrayList<IDeviceTypeCustomWizardPage> pages = customPages.get(wizard
				.getCurrentDeviceTypeId());
		if (pages != null) {
			if (currentPage instanceof DefaultDeviceTypeMenuWizardPage) {
				return null;
			} else {

				for (int i = pages.size() - 1; i >= 0; i--) {
					IDeviceTypeCustomWizardPage page = pages.get(i);
					if (page.getPageClass().equals(currentPage)) {
						if (i == 0) {
							return wizard
									.getPage(NewDeviceMenuWizard.NEW_DEVICE_MENU_DEFAULT_PAGE_ID);
						} else {
							return pages.get(i - 1).getPageClass();
						}
					}
				}

			}
		}
		return null;
	}

	private static synchronized void loadExtensionsSynchronized() {
		if (!customPages.isEmpty())
			return;
		// Get the extensions
		IExtensionPoint extensionPoint = Platform.getExtensionRegistry()
				.getExtensionPoint(WIZARD_PAGES_EXTENSION_POINT_ID);
		if (extensionPoint != null) {
			IExtension[] extensions = extensionPoint.getExtensions();
			if (extensions != null) {

				for (int i = 0; i < extensions.length; ++i) {
					IExtension extension = extensions[i];
					IConfigurationElement[] elements = extension
							.getConfigurationElements();

					// process the top level elements for this extension
					for (int j = 0; j < elements.length; j++) {
						IConfigurationElement element = elements[j];

						if (element.getName().equals(XML_ELEMENT_WIZARD_PAGE)) {

							String id = element.getAttribute(XML_ATTRIBUTE_ID);
							DeviceWizardRunnable operationClass = null;

							IWizardPage pageClass;
							try {
								pageClass = (IWizardPage) element
										.createExecutableExtension(XML_ATTRIBUTE_PAGE_CLASS);
								// the operation is an optional element so it
								// might not be present
								if (element
										.getAttribute(XML_ATTRIBUTE_OPERATION_CLASS) != null) {
									operationClass = (DeviceWizardRunnable) element.createExecutableExtension(XML_ATTRIBUTE_OPERATION_CLASS);
								    operationClass.setPage(pageClass);
								}
								IDeviceTypeCustomWizardPage customPage = new DeviceTypeCustomWizardPage(
										id, pageClass, operationClass);
								
								IConfigurationElement[] children = element
										.getChildren();

								for (int k = 0; k < children.length; k++) {
									IConfigurationElement childElement = children[k];

									if (childElement.getName().equals(
											XML_ELEMENT_DEVICE_TYPE)) {
										String deviceTypeId = childElement
												.getAttribute(XML_ATTRIBUTE_DEVICE_TYPE_ID);
										if (customPages
												.containsKey(deviceTypeId)) {
											ArrayList<IDeviceTypeCustomWizardPage> list = customPages
													.get(deviceTypeId);
											list.add(customPage);
										} else {
											ArrayList<IDeviceTypeCustomWizardPage> list = new ArrayList<IDeviceTypeCustomWizardPage>();
											list.add(customPage);
											customPages.put(deviceTypeId, list);
										}

									}
								}
							} catch (CoreException e) {
								e.printStackTrace();
							}

						}

					}

				}

			}
			// put wizard pages defined for super class first
			for (String deviceTypeId : DeviceTypeRegistry.getInstance()
					.getDeviceTypeIds()) {
				ArrayList<IDeviceTypeCustomWizardPage> list = customPages
						.get(deviceTypeId);

				ArrayList<IDeviceTypeCustomWizardPage> superList = getSuperClassWizardPages(deviceTypeId);
				if (list != null) {
					if (superList != null)
						list.addAll(0, superList);
				} else {
					if (superList != null) {
						list = new ArrayList<IDeviceTypeCustomWizardPage>();
						list.addAll(superList);
						customPages.put(deviceTypeId, list);
					}

				}

			}
		}
	}

	private static ArrayList<IDeviceTypeCustomWizardPage> getSuperClassWizardPages(
			String deviceTypeId) {
		ArrayList<IDeviceTypeCustomWizardPage> pages = new ArrayList<IDeviceTypeCustomWizardPage>();
		if (customPages!=null) {
			ArrayList<IDeviceTypeCustomWizardPage> existPages = customPages.get(deviceTypeId);
			if (existPages!=null) {
				IDeviceType currentDeviceType = DeviceTypeRegistry.getInstance().getDeviceTypeById(deviceTypeId);
				if (currentDeviceType != null) {
					String superClassDeviceTypeId = currentDeviceType.getSuperClass();
					if (superClassDeviceTypeId != null) {
						pages.addAll(0,	getSuperClassWizardPages(superClassDeviceTypeId));
						ArrayList<IDeviceTypeCustomWizardPage> superPages = customPages.get(superClassDeviceTypeId);
						int startingIndex = 0;
						if (superPages!=null) {
							for (IDeviceTypeCustomWizardPage superCustomWizardPage : superPages) {
								if (!isPageExists(superCustomWizardPage.getId(), existPages)) {
									pages.add(startingIndex++, superCustomWizardPage);
								}
						
							}
						}
					}
				}
			}
		}
		return pages;
	}

	private static boolean isPageExists(String pageId,
			ArrayList<IDeviceTypeCustomWizardPage> list) {
		if (list != null) {
			for (IDeviceTypeCustomWizardPage deviceTypeCustomWizardPage : list) {
				if (deviceTypeCustomWizardPage.getId().equals(pageId))
					return true;
			}
		}
		return false;
	}

	public boolean canFinish() {
		loadExtensionsSynchronized();
		ArrayList<IDeviceTypeCustomWizardPage> pages = customPages.get(wizard
				.getCurrentDeviceTypeId());
		if (pages != null) {
			for (IDeviceTypeCustomWizardPage customWizardPageDefinition : pages) {
				if (!customWizardPageDefinition.getPageClass().isPageComplete())
					return false;
			}
		}
		return true;
	}

	public boolean performFinish() {
		loadExtensionsSynchronized();
		ArrayList<IDeviceTypeCustomWizardPage> pages = customPages.get(wizard
				.getCurrentDeviceTypeId());
		if (pages == null)
			return true;
		List<IRunnableWithProgress> operations = new ArrayList<IRunnableWithProgress>();
		for (IDeviceTypeCustomWizardPage page : pages) {
			IRunnableWithProgress operationClass = page.getOperationClass();
			if (operationClass != null)
				operations.add(operationClass);
		}
		for (IRunnableWithProgress runnableWithProgress : operations) {
			try {
				wizard.getContainer().run(false, true, runnableWithProgress);
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		return true;
	}
}

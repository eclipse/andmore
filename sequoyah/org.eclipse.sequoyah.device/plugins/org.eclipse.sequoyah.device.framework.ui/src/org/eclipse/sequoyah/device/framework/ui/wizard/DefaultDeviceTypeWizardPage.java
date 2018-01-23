/*******************************************************************************
 * Copyright (c) 2008-2010 MontaVista Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Yu-Fen Kuo (MontaVista) - initial API and implementation
 * Yu-Fen Kuo (MontaVista)  - [236476] - provide a generic device type
 * Daniel Barboza Franco (Eldorado Research Institute) - Bug [271682] - Default Wizard Page accepting invalid names
 * Daniel Barboza Franco (Eldorado Research Institute) - Bug [274502] - Change labels: Instance Management view and Services label
 * Fabio Rigo (Eldorado) - Bug [288006] - Unify features of InstanceManager and InstanceRegistry
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 *******************************************************************************/
package org.eclipse.sequoyah.device.framework.ui.wizard;

import java.util.ArrayList;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.sequoyah.device.framework.DevicePlugin;
import org.eclipse.sequoyah.device.framework.factory.DeviceTypeRegistry;
import org.eclipse.sequoyah.device.framework.factory.InstanceRegistry;
import org.eclipse.sequoyah.device.framework.internal.model.MobileDeviceType;
import org.eclipse.sequoyah.device.framework.model.AbstractMobileInstance;
import org.eclipse.sequoyah.device.framework.model.IDeviceType;
import org.eclipse.sequoyah.device.framework.ui.DeviceUIResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * default first page of the new device instance wizard. Allows user to specify
 * the device instance name and the device type
 * 
 */
public class DefaultDeviceTypeWizardPage extends WizardPage {

	private static final String PROPERTY_ICON = "icon"; //$NON-NLS-1$

	private String instanceName = DeviceUIResources.DefaultDeviceTypeWizardPage_0;
	private MobileDeviceType currentDeviceType;
	private TreeViewer deviceTypesTreeViewer;

	class AbstractDeviceTypeViewerFilter extends ViewerFilter {

		@Override
		public boolean select(Viewer viewer, Object parentElement,
				Object element) {
			if (element instanceof IDeviceType) {
				// do not display abstract device types
				if (((IDeviceType) element).isAbstract())
					return false;
			}
			return true;
		}

	}

	class DeviceTypesContentProvider implements ITreeContentProvider {
		public Object[] getChildren(Object parentElement) {
			ArrayList<Object> children = new ArrayList<Object>();
			if (parentElement instanceof DefaultDeviceTypeWizardPage) {
				return DeviceTypeRegistry
						.getInstance().getDeviceTypes().toArray();
			}
			return children.toArray();
		}

		public Object getParent(Object element) {
			return null;
		}

		public boolean hasChildren(Object element) {
			if (getElements(element).length > 0)
				return true;
			return false;
		}

		public Object[] getElements(Object inputElement) {
			return getChildren(inputElement);
		}

		public void dispose() {
			//do nothing
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			//do nothing
		}
	}

	class DeviceTypesLabelProvider extends LabelProvider {
		@Override
		public Image getImage(Object element) {
			if (element instanceof MobileDeviceType) {
				MobileDeviceType item = (MobileDeviceType) element;

				if (item.getProperties().containsKey(PROPERTY_ICON)) {
					String path = item.getProperties().getProperty(
							PROPERTY_ICON);
					Image image = DevicePlugin.getDefault()
							.getImageFromRegistry(item.getBundleName(), path);

					return image;
				}

			}
			return super.getImage(element);
		}

		@Override
		public String getText(Object element) {
			if (element instanceof MobileDeviceType) {
				MobileDeviceType item = (MobileDeviceType) element;

				return item.getLabel();

			}
			return super.getText(element);
		}

	}

	protected DefaultDeviceTypeWizardPage(String pageName, String title,
			ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
	}

	protected DefaultDeviceTypeWizardPage(String pageName) {
		super(pageName);
		setTitle(DeviceUIResources.SEQUOYAH_Default_Device_Type_Wizard_Page_title);

		setMessage(DeviceUIResources.SEQUOYAH_Default_Device_Type_Wizard_Page_message);
	}

	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(2, false));
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		container.setFont(parent.getFont());

		// new name label
		Label label = new Label(container, SWT.NONE);
		label.setText(DeviceUIResources.SEQUOYAH_Default_Device_Type_Wizard_Page_name);
		label.setFont(container.getFont());

		final Text nameText = new Text(container, SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		nameText.setLayoutData(gd);
		nameText.setFont(container.getFont());

		nameText.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent e) {
				InstanceRegistry registry = InstanceRegistry.getInstance();
				String name = nameText.getText();
				String errorMessage = null;

				instanceName = DeviceUIResources.DefaultDeviceTypeWizardPage_1;
				
				if (name != null) {
					//name = name.trim();
					if (!name.equals("")) { //$NON-NLS-1$
						if (! (registry.getInstancesByName(name).size() == 0)) {
							errorMessage = DeviceUIResources.SEQUOYAH_Emulator_Wizard_Project_Description_Duplicated_Error;
						}
						else if (!AbstractMobileInstance.validName(name)){
							errorMessage = DeviceUIResources.SEQUOYAH_Instance_Name_Invalid_Error;
						}
						else {
							instanceName = name;
						}
					}
				}

				setErrorMessage(errorMessage);
				getWizard().getContainer().updateButtons();

			}

		});

		createDeviceTypesArea(container);

		setControl(container);
	}

	public boolean isPageComplete() {
		if (getErrorMessage() != null)
			return false;
		if (!instanceName.trim().equals("") && currentDeviceType != null) { //$NON-NLS-1$
			return true;
		}

		return false;
	}

	private void createDeviceTypesArea(Composite parent) {

		Label label = new Label(parent, SWT.NONE);
		label.setText(DeviceUIResources.SEQUOYAH_Default_Device_Type_Wizard_Page_deviceTypes);
		label.setFont(parent.getFont());
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);
		deviceTypesTreeViewer = new TreeViewer(parent);
		deviceTypesTreeViewer
				.setContentProvider(new DeviceTypesContentProvider());
		deviceTypesTreeViewer.setLabelProvider(new DeviceTypesLabelProvider());
		deviceTypesTreeViewer.setInput(this);
		deviceTypesTreeViewer.addFilter(new AbstractDeviceTypeViewerFilter());
		deviceTypesTreeViewer.expandAll();
		gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 2;
		gd.verticalSpan = 2;
		deviceTypesTreeViewer.getControl().setLayoutData(gd);

		deviceTypesTreeViewer
				.addSelectionChangedListener(new ISelectionChangedListener() {

					public void selectionChanged(SelectionChangedEvent event) {
						ISelection selection = deviceTypesTreeViewer
								.getSelection();

						if (selection instanceof StructuredSelection) {
							Object firstElement = ((StructuredSelection) selection)
									.getFirstElement();
							if (firstElement instanceof MobileDeviceType) {
								currentDeviceType = (MobileDeviceType) firstElement;
								IWizard wizard = getWizard();
								if (wizard instanceof NewDeviceWizard) {
									((NewDeviceWizard) wizard)
											.setCurrentDeviceTypeId(currentDeviceType
													.getId());
								}

							}
						}
						getWizard().getContainer().updateButtons();

					}
				});
	}

	public String getInstanceName() {
		return instanceName.trim();
	}

	public MobileDeviceType getDeviceType() {
		return currentDeviceType;
	}

}

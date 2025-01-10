/*******************************************************************************
 * Copyright (c) 2008-2010 MontaVista Software, Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Yu-Fen Kuo (MontaVista) - initial API and implementation
 * Fabio Fantato (Instituto Eldorado) - [263188] - Create new examples to support tutorial presentation
 * Fabio Fantato (Instituto Eldorado) - [243494] Change the reference implementation to work on Galileo
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 *******************************************************************************/
package org.eclipse.sequoyah.device.framework.ui.wizard;

import java.util.Properties;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.sequoyah.device.common.utilities.IPropertyConstants;
import org.eclipse.sequoyah.device.framework.ui.DeviceUIResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/*
 * wizard page for the new QEmu device type device instance wizard. It allows
 * users to specify the host, port and display info.
 */
public class DefaultConnectionInfoWizardPage extends WizardPage implements
		IInstanceProperties {

	private Text hostText;
	private Text portText;
	private Text displayText;

	protected DefaultConnectionInfoWizardPage(String pageName) {
		super(pageName);
		
	}

	public DefaultConnectionInfoWizardPage() {
		super("$qemu_basic_info$"); //$NON-NLS-1$
		setTitle(DeviceUIResources.ConnectionInfoWizardPage_title);
		setMessage(DeviceUIResources.ConnectionInfoWizardPage_message);
	}

	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(2, false));
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		container.setFont(parent.getFont());

		Label label = new Label(container, SWT.NONE);
		label.setText(DeviceUIResources.ConnectionInfoWizardPage_Host);
		label.setFont(container.getFont());

		hostText = new Text(container, SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		hostText.setLayoutData(gd);
		hostText.setFont(container.getFont());

		hostText.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent e) {
				getWizard().getContainer().updateButtons();

			}

		});

		label = new Label(container, SWT.NONE);
		label.setText(DeviceUIResources.ConnectionInfoWizardPage_Port);
		label.setFont(container.getFont());

		portText = new Text(container, SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		portText.setLayoutData(gd);
		portText.setFont(container.getFont());

		portText.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent e) {
				getWizard().getContainer().updateButtons();

			}

		});

		label = new Label(container, SWT.NONE);
		label.setText(DeviceUIResources.ConnectionInfoWizardPage_Display);
		label.setFont(container.getFont());

		displayText = new Text(container, SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		displayText.setLayoutData(gd);
		displayText.setFont(container.getFont());

		displayText.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent e) {
				getWizard().getContainer().updateButtons();

			}

		});
		initializeDefaultValues();
		setControl(container);

	}

	private void initializeDefaultValues() {
		hostText.setText(IPropertyConstants.DEFAULT_HOST);
		portText.setText(IPropertyConstants.DEFAULT_PORT);
		displayText.setText(IPropertyConstants.DEFAULT_DISPLAY);
	}

	@Override
	public boolean isPageComplete() {
		if (hostText == null || hostText.isDisposed())
			return false;
		if (!hostText.getText().trim().equals("") //$NON-NLS-1$
				&& !displayText.getText().trim().equals("") //$NON-NLS-1$
				&& !portText.getText().trim().equals("")) //$NON-NLS-1$
			return true;
		return false;
	}

	public void dispose() {
		if (getControl() != null)
			setControl(null);
		super.dispose();
	}

	public Properties getProperties() {
		Properties properties = new Properties();
		properties.put(IPropertyConstants.HOST, hostText.getText());
		properties.put(IPropertyConstants.PORT, portText.getText());
		properties.put(IPropertyConstants.DISPLAY, displayText.getText());
		return properties;
	}
}

/*******************************************************************************
 * Copyright (c) 2008-2010 MontaVista Software, Inc and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Yu-Fen Kuo (MontaVista) - initial API and implementation
 * Fabio Fantato (Instituto Eldorado) - [243494] Change the reference implementation to work on Galileo
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 *******************************************************************************/
package org.eclipse.sequoyah.device.qemu.wizard.custom;

import java.util.Properties;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.sequoyah.device.framework.ui.wizard.IInstanceProperties;
import org.eclipse.sequoyah.device.qemu.QEmuPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/*
 * custom wizard page used by the new device instance wizard for generic QEmu
 * device type. In this page, user would specify the options for starting up the
 * QEmu emulator.
 */
public class QEmuBinarySettingsWizardPage extends WizardPage implements
		IInstanceProperties {
	private Text installedDirText;
	private Button browseInstalledDirButton;

	private Text emulatorBinaryText;
	private Button browseEmulatorBinaryButton;

	private Text kernelImageText;
	private Button browseKernelImageButton;

	private Text initrdText;
	private Button browseInitrdButton;

	private Text emulatedMachineText;
	private Text additionalOptionsText;
	private Button startVNCButton;

	private StyledText usage;

	protected QEmuBinarySettingsWizardPage(String pageName) {
		super(pageName);
	}

	public QEmuBinarySettingsWizardPage() {
		super("$qemu_binary_settings$"); //$NON-NLS-1$
		setTitle(QEmuPlugin
				.getResourceString("QEmuBinarySettingsWizardPage.title")); //$NON-NLS-1$
		setMessage(QEmuPlugin
				.getResourceString("QEmuBinarySettingsWizardPage.message")); //$NON-NLS-1$
	}

	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(3, false));
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		container.setFont(parent.getFont());

		Label label = new Label(container, SWT.NONE);
		label
				.setText(QEmuPlugin
						.getResourceString("QEmuBinarySettingsWizardPage.installedDir")); //$NON-NLS-1$
		label.setFont(container.getFont());

		installedDirText = new Text(container, SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		installedDirText.setLayoutData(gd);
		installedDirText.setFont(container.getFont());

		installedDirText.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent e) {
				updateUsage();
				getWizard().getContainer().updateButtons();

			}

		});

		browseInstalledDirButton = new Button(container, SWT.PUSH);
		browseInstalledDirButton.setText(QEmuPlugin
				.getResourceString("QEmuBinarySettingsWizardPage.browse")); //$NON-NLS-1$
		browseInstalledDirButton.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog fileDialog = new DirectoryDialog(getShell(),
						SWT.OPEN);
				if (installedDirText.getText() != null) {
					fileDialog.setFilterPath(installedDirText.getText());
				}
				String dir = fileDialog.open();
				if (dir != null) {
					dir = dir.trim();
					if (dir.length() > 0) {
						installedDirText.setText(dir);
					}
				}
			}

		});

		Group group = new Group(container, SWT.NONE);
		group
				.setText(QEmuPlugin
						.getResourceString("QEmuBinarySettingsWizardPage.emulatorStartupSettings")); //$NON-NLS-1$
		GridLayout layout = new GridLayout(3, false);
		group.setLayout(layout);
		gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 3;
		// gd.verticalSpan = 3;
		group.setLayoutData(gd);
		group.setFont(container.getFont());

		label = new Label(group, SWT.NONE);
		label
				.setText(QEmuPlugin
						.getResourceString("QEmuBinarySettingsWizardPage.emulatorBinary")); //$NON-NLS-1$
		label.setFont(container.getFont());

		emulatorBinaryText = new Text(group, SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		emulatorBinaryText.setLayoutData(gd);
		emulatorBinaryText.setFont(container.getFont());

		emulatorBinaryText.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent e) {
				updateUsage();
				getWizard().getContainer().updateButtons();

			}

		});

		browseEmulatorBinaryButton = new Button(group, SWT.PUSH);
		browseEmulatorBinaryButton.setText(QEmuPlugin
				.getResourceString("QEmuBinarySettingsWizardPage.browse")); //$NON-NLS-1$
		browseEmulatorBinaryButton.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				FileDialog fileDialog = new FileDialog(getShell(), SWT.OPEN);

				if (emulatorBinaryText.getText().trim() != "") { //$NON-NLS-1$
					fileDialog.setFilterPath(emulatorBinaryText.getText());
				} else if (installedDirText.getText().trim() != "") { //$NON-NLS-1$
					fileDialog.setFilterPath(installedDirText.getText());
				}
				String dir = fileDialog.open();
				if (dir != null) {
					dir = dir.trim();
					if (dir.length() > 0) {
						emulatorBinaryText.setText(getRelativePath(dir));
					}
				}
			}

		});

		label = new Label(group, SWT.NONE);
		label.setText(QEmuPlugin
				.getResourceString("QEmuBinarySettingsWizardPage.kernelImage")); //$NON-NLS-1$
		label.setFont(container.getFont());

		kernelImageText = new Text(group, SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		kernelImageText.setLayoutData(gd);
		kernelImageText.setFont(container.getFont());

		kernelImageText.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent e) {
				updateUsage();
				getWizard().getContainer().updateButtons();

			}

		});

		browseKernelImageButton = new Button(group, SWT.PUSH);
		browseKernelImageButton.setText(QEmuPlugin
				.getResourceString("QEmuBinarySettingsWizardPage.browse")); //$NON-NLS-1$
		browseKernelImageButton.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				FileDialog fileDialog = new FileDialog(getShell(), SWT.OPEN);
				if (kernelImageText.getText() != null) {
					fileDialog.setFilterPath(emulatorBinaryText.getText());
				} else if (installedDirText.getText() != null) {
					fileDialog.setFilterPath(installedDirText.getText());
				}
				String dir = fileDialog.open();
				if (dir != null) {
					dir = dir.trim();
					if (dir.length() > 0) {
						kernelImageText.setText(getRelativePath(dir));
					}
				}
			}

		});

		label = new Label(group, SWT.NONE);
		label.setText(QEmuPlugin
				.getResourceString("QEmuBinarySettingsWizardPage.initrd")); //$NON-NLS-1$
		label.setFont(container.getFont());

		initrdText = new Text(group, SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		initrdText.setLayoutData(gd);
		initrdText.setFont(container.getFont());

		initrdText.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent e) {
				updateUsage();
				getWizard().getContainer().updateButtons();

			}

		});

		browseInitrdButton = new Button(group, SWT.PUSH);
		browseInitrdButton.setText(QEmuPlugin
				.getResourceString("QEmuBinarySettingsWizardPage.browse")); //$NON-NLS-1$
		browseInitrdButton.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				FileDialog fileDialog = new FileDialog(getShell(), SWT.OPEN);
				if (initrdText.getText() != null) {
					fileDialog.setFilterPath(initrdText.getText());
				} else if (installedDirText.getText() != null) {
					fileDialog.setFilterPath(installedDirText.getText());
				}
				String dir = fileDialog.open();
				if (dir != null) {
					dir = dir.trim();
					if (dir.length() > 0) {
						initrdText.setText(getRelativePath(dir));
					}
				}
			}

		});
		group.pack();
		startVNCButton = new Button(container, SWT.CHECK);
		startVNCButton.setSelection(true);
		startVNCButton.setText(QEmuPlugin
				.getResourceString("QEmuBinarySettingsWizardPage.enableVNC")); //$NON-NLS-1$
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 3;
		startVNCButton.setLayoutData(gd);

		label = new Label(container, SWT.NONE);
		label
				.setText(QEmuPlugin
						.getResourceString("QEmuBinarySettingsWizardPage.emulatedMachine")); //$NON-NLS-1$
		label.setFont(container.getFont());

		emulatedMachineText = new Text(container, SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		emulatedMachineText.setLayoutData(gd);
		emulatedMachineText.setFont(container.getFont());

		emulatedMachineText.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent e) {
				updateUsage();
				getWizard().getContainer().updateButtons();

			}

		});

		label = new Label(container, SWT.NONE);
		label
				.setText(QEmuPlugin
						.getResourceString("QEmuBinarySettingsWizardPage.additionalOptions")); //$NON-NLS-1$
		label.setFont(container.getFont());

		additionalOptionsText = new Text(container, SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		additionalOptionsText.setLayoutData(gd);
		additionalOptionsText.setFont(container.getFont());

		additionalOptionsText.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent e) {
				updateUsage();
				getWizard().getContainer().updateButtons();

			}

		});

		label = new Label(container, SWT.NONE);
		label.setText(QEmuPlugin
				.getResourceString("QEmuBinarySettingsWizardPage.usage")); //$NON-NLS-1$
		label.setFont(container.getFont());
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 3;
		label.setLayoutData(gd);

		usage = new StyledText(container, SWT.BORDER | SWT.WRAP | SWT.MULTI
				| SWT.READ_ONLY);

		gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 3;
		gd.verticalSpan = 3;
		gd.minimumHeight = 100;
		usage.setLayoutData(gd);
		setControl(container);

	}

	@Override
	public boolean isPageComplete() {
		if (installedDirText == null || installedDirText.isDisposed())
			return false;
		if (installedDirText != null
				&& installedDirText.getText().trim() != "" //$NON-NLS-1$
				&& emulatorBinaryText != null
				&& emulatorBinaryText.getText().trim() != "")//$NON-NLS-1$
			return true;
		return false;
	}

	@Override
	public void dispose() {
		if (getControl() != null)
			setControl(null);
		super.dispose();
	}

	public Properties getProperties() {
		Properties properties = new Properties();
		properties.put(IPropertyConstants.QEMU_BINARY_INSTALLED_DIR,
				installedDirText.getText());
		properties.put(IPropertyConstants.QEMU_BINARY_NAME, emulatorBinaryText
				.getText());
		if (kernelImageText.getText().trim() != "") //$NON-NLS-1$
			properties.put(IPropertyConstants.KERNEL_IMAGE, kernelImageText
					.getText());
		if (initrdText.getText().trim() != "") //$NON-NLS-1$
			properties.put(IPropertyConstants.INITIAL_RAM_DISK, initrdText
					.getText());
		if (emulatedMachineText.getText().trim() != "") //$NON-NLS-1$
			properties.put(IPropertyConstants.EMULATED_MACHINE,
					emulatedMachineText.getText());
		if (startVNCButton.getSelection())
			properties.put(IPropertyConstants.ENABLE_VNC, Boolean.TRUE
					.toString());
		else
			properties.put(IPropertyConstants.ENABLE_VNC, Boolean.FALSE
					.toString());
		if (additionalOptionsText.getText().trim() != "") //$NON-NLS-1$
			properties.put(IPropertyConstants.ADDITIONAL_OPTIONS,
					additionalOptionsText.getText());
		return properties;
	}

	private String getRelativePath(String path) {
		if (path.startsWith(installedDirText.getText())) {
			return path.substring(installedDirText.getText().length() + 1);
		}
		return path;
	}

	private void updateUsage() {
		StringBuffer buffer = new StringBuffer();
		if (emulatorBinaryText.getText().trim() != "") { //$NON-NLS-1$
			buffer.append(emulatorBinaryText.getText().trim());
			//buffer.append(" -L . "); //$NON-NLS-1$
		}
		if (kernelImageText.getText().trim() != "") { //$NON-NLS-1$
			buffer.append(" -kernel "); //$NON-NLS-1$
			buffer.append(kernelImageText.getText().trim());
		}
		if (initrdText.getText().trim() != "") { //$NON-NLS-1$
			buffer.append(" -initrd "); //$NON-NLS-1$
			buffer.append(initrdText.getText().trim());
		}
		if (emulatedMachineText.getText().trim() != "") { //$NON-NLS-1$
			buffer.append(" -M "); //$NON-NLS-1$
			buffer.append(emulatedMachineText.getText().trim());
		}
		if (additionalOptionsText.getText().trim() != "") { //$NON-NLS-1$
			buffer.append(" "); //$NON-NLS-1$
			buffer.append(additionalOptionsText.getText().trim());
			buffer.append(" "); //$NON-NLS-1$
		}
		if (startVNCButton.getSelection()) {
			IWizardPage previousPage = getPreviousPage();
			if (previousPage instanceof IInstanceProperties) {
				Properties properties = ((IInstanceProperties) previousPage)
						.getProperties();
				String host = properties.getProperty(IPropertyConstants.HOST);
				String display = properties
						.getProperty(IPropertyConstants.DISPLAY);
				String vncConnectionString = null;
				if (host != null && display != null) {
					vncConnectionString = host + display;
				} else {
					vncConnectionString = "<host>:<display>"; //$NON-NLS-1$
				}
				buffer.append(" -vnc " + vncConnectionString); //$NON-NLS-1$
			}

		}
		usage.setText(buffer.toString());
	}

}

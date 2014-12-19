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
package org.eclipse.andmore.android.common.utilities.ui;

import org.eclipse.andmore.android.common.utilities.i18n.UtilitiesNLS;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

public class PasswordInputDialog extends Dialog {
	private static final int SMALL_TEXT_SIZE = 64;

	private String newPassword = null;

	private String oldPassword = null;

	private final boolean changePassword;

	private Text newPasswordConfirmText;

	private Text newPasswordText;

	private Text oldPasswordText;

	private Label message;

	private Label image;

	private final String description;

	private Button saveCheckBox;

	private boolean needToStorePassword = true;

	private int passwordMinimumSize = 1; // 1 is default, it is recommended to
											// change in the constructor

	/**
	 * Create a new Input dialog
	 * 
	 * @param parent
	 *            : the parent shell
	 * @param description
	 *            : the textual description of dialog
	 * @param changePassword
	 *            : true if you want this dialog be a change password dialog (it
	 *            will present old, new and confirm new fields). False to show
	 *            only enter password field
	 * @param oldPassword
	 *            : null if user must enter oldpassword. Anything to create
	 *            dialog with preentered password (only if changePassword is
	 *            true);
	 */
	public PasswordInputDialog(Shell parent, String description, boolean changePassword, String oldPassword,
			int passwordMinimumSize) {
		super(parent);
		this.changePassword = changePassword;
		this.description = description;
		if (changePassword) {
			this.oldPassword = oldPassword;
		}
		this.passwordMinimumSize = passwordMinimumSize;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite mainComposite = new Composite(parent, SWT.NONE);
		mainComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
		mainComposite.setLayout(new GridLayout(2, false));

		/**
		 * The Message Area (With an image and a text)
		 */
		Composite messageComposite = new Composite(mainComposite, SWT.NONE);
		GridData layoutData = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
		messageComposite.setLayoutData(layoutData);
		GridLayout layout = new GridLayout(2, false);
		layout.marginWidth = 0;
		messageComposite.setLayout(layout);

		image = new Label(messageComposite, SWT.NONE);
		image.setVisible(false);
		layoutData = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		image.setLayoutData(layoutData);

		message = new Label(messageComposite, SWT.NONE);
		message.setText(description);
		layoutData = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		message.setLayoutData(layoutData);

		Label oldPasswordLabel = new Label(mainComposite, SWT.NONE);
		layoutData = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		oldPasswordLabel.setLayoutData(layoutData);
		oldPasswordLabel.setText(UtilitiesNLS.Passwordinput_Enterpassword_Label);

		oldPasswordText = new Text(mainComposite, SWT.BORDER | SWT.SINGLE | SWT.PASSWORD);
		oldPasswordText.setTextLimit(SMALL_TEXT_SIZE);
		layoutData = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		layoutData.minimumWidth = 150;
		oldPasswordText.setLayoutData(layoutData);
		if (oldPassword != null) {
			oldPasswordText.setEnabled(false);
			oldPasswordText.setText(oldPassword);
			oldPasswordLabel.setEnabled(false);
		}
		oldPasswordText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				updateStatus();
			}
		});

		/**
		 * If the input is to change the passwords, create all other fields
		 */
		if (changePassword) {
			Label separator = new Label(mainComposite, SWT.HORIZONTAL | SWT.SEPARATOR);
			layoutData = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
			separator.setLayoutData(layoutData);

			/**
			 * The newPassword area
			 */
			Label passwordLabel = new Label(mainComposite, SWT.NONE);
			layoutData = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
			passwordLabel.setLayoutData(layoutData);
			passwordLabel.setText(UtilitiesNLS.Passwordinput_Enternewpassword_Label);

			newPasswordText = new Text(mainComposite, SWT.BORDER | SWT.SINGLE | SWT.PASSWORD);
			newPasswordText.setTextLimit(SMALL_TEXT_SIZE);
			layoutData = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
			layoutData.minimumWidth = 150;
			newPasswordText.setLayoutData(layoutData);

			Label passwordConfirmLabel = new Label(mainComposite, SWT.NONE);
			layoutData = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
			passwordConfirmLabel.setLayoutData(layoutData);
			passwordConfirmLabel.setText(UtilitiesNLS.Passwordinput_Reenterpassword_Label);

			newPasswordConfirmText = new Text(mainComposite, SWT.BORDER | SWT.SINGLE | SWT.PASSWORD);
			newPasswordConfirmText.setTextLimit(SMALL_TEXT_SIZE);
			layoutData = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
			layoutData.minimumWidth = 150;
			newPasswordConfirmText.setLayoutData(layoutData);

			newPasswordText.addModifyListener(new ModifyListener() {

				@Override
				public void modifyText(ModifyEvent e) {
					updateStatus();
				}
			});

			newPasswordConfirmText.addModifyListener(new ModifyListener() {

				@Override
				public void modifyText(ModifyEvent e) {
					updateStatus();
				}
			});
		}

		// Creates the save password checkbox
		saveCheckBox = new Button(mainComposite, SWT.CHECK);
		saveCheckBox.setText(UtilitiesNLS.PasswordProvider_SaveThisPassword);
		saveCheckBox.setSelection(false);
		GridData gridData = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
		saveCheckBox.setLayoutData(gridData);

		return mainComposite;
	}

	public void updateStatus() {
		String errorMessage = null;
		int severity = IMessageProvider.NONE;

		if (oldPasswordText.getText().length() > 0) {
			oldPassword = oldPasswordText.getText();
		} else {
			errorMessage = "";
			oldPassword = null;
		}

		if ((errorMessage == null) && changePassword) {
			if (newPasswordText.getText().length() < passwordMinimumSize) {
				errorMessage = NLS.bind(UtilitiesNLS.Passwordinput_Error_PasswordMinimumSize, passwordMinimumSize);
				severity = IMessageProvider.ERROR;
			} else {
				if (!newPasswordText.getText().equals(newPasswordConfirmText.getText())) {
					errorMessage = UtilitiesNLS.Passwordinput_Error_Passwordnotmatch;
					severity = IMessageProvider.ERROR;
				} else {
					newPassword = newPasswordText.getText();
				}
			}
		}

		setErrorMessage(errorMessage, severity);

	}

	private void setErrorMessage(String errorMsg, int severity) {
		if ((errorMsg == null) || (severity == IMessageProvider.NONE)) {
			image.setImage(null);
			image.setVisible(false);
			message.setText(description);
			getButton(OK).setEnabled(errorMsg == null);
		} else {
			message.setText(errorMsg);
			message.setVisible(true);
			switch (severity) {
			case IMessageProvider.ERROR:
				image.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_ERROR_TSK));
				break;

			case IMessageProvider.INFORMATION:
				image.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_INFO_TSK));
				break;

			case IMessageProvider.WARNING:
				image.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_WARN_TSK));
				break;

			default:
				image.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_ERROR_TSK));
				break;
			}
			image.setVisible(true);
			getButton(OK).setEnabled(false);
		}
		message.getParent().layout();
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		getButton(OK).setEnabled(false);
	}

	public String getNewPassword() {
		return newPassword;
	}

	public String getOldPassword() {
		return oldPassword;
	}

	public boolean needToStorePassword() {
		return needToStorePassword;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(UtilitiesNLS.Passwordinput_Title);
		newShell.setMinimumSize(500, 200);
		newShell.layout(true);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	@Override
	protected void okPressed() {
		needToStorePassword = saveCheckBox.getSelection();
		super.okPressed();
	}

}

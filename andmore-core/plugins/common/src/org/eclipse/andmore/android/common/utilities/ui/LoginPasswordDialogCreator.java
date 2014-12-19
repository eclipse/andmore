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
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

public class LoginPasswordDialogCreator {
	private LoginPasswordDialog dialog;

	private final String url;

	// dialog return values
	public static final int OK = LoginPasswordDialog.OK;

	public static final int CANCEL = LoginPasswordDialog.CANCEL;

	public LoginPasswordDialogCreator(String url) {

		this.url = url;

	}

	public int openLoginPasswordDialog() {
		final Integer[] dialogReturnValue = new Integer[1];
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {

			@Override
			public void run() {
				dialog = new LoginPasswordDialog(PlatformUI.getWorkbench().getDisplay().getActiveShell());
				dialogReturnValue[0] = dialog.open();
			}
		});

		return dialogReturnValue[0];
	}

	public String getTypedLogin() {
		String login = dialog != null ? dialog.getTypedLogin() : null;
		return login;
	}

	public String getTypedPassword() {
		String password = dialog != null ? dialog.getTypedPassword() : null;
		return password;
	}

	private class LoginPasswordDialog extends Dialog {
		private String login;

		private String password;

		/**
		 * @param parentShell
		 */
		protected LoginPasswordDialog(Shell parentShell) {

			super(parentShell);

			setShellStyle(getShellStyle() | SWT.DIALOG_TRIM);

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt
		 * .widgets.Composite)
		 */
		@Override
		protected Control createDialogArea(Composite parent) {

			parent.getShell().setText(UtilitiesNLS.LoginPasswordDialogCreator_DialogTItle0);

			Composite topComposite = new Composite(parent, SWT.FILL);
			GridLayout layout = new GridLayout(1, false);
			GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
			topComposite.setLayout(layout);
			topComposite.setLayoutData(layoutData);

			Label messageLabel = new Label(topComposite, SWT.NONE);
			layoutData = new GridData(SWT.FILL, SWT.CENTER, false, false);
			messageLabel.setLayoutData(layoutData);
			messageLabel
					.setText("   \n" + UtilitiesNLS.SDKLoginPasswordDialog_LoginInformationMessage + " " + url.substring(0, url.lastIndexOf("/")) + "   "); //$NON-NLS-1$ //$NON-NLS-3$ //$NON-NLS-4$

			Composite mainComposite = new Composite(topComposite, SWT.FILL);
			layout = new GridLayout(2, false);
			mainComposite.setLayout(layout);
			mainComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

			Label loginLabel = new Label(mainComposite, SWT.NONE);
			layoutData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
			loginLabel.setLayoutData(layoutData);
			loginLabel.setText(UtilitiesNLS.SDKLoginPasswordDialog_UsernameLabel);

			final Text loginText = new Text(mainComposite, SWT.SINGLE | SWT.BORDER);
			layoutData = new GridData(SWT.FILL, SWT.CENTER, true, false);
			loginText.setLayoutData(layoutData);
			loginText.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					login = loginText.getText();
				}
			});

			Label passwordLabel = new Label(mainComposite, SWT.NONE);
			layoutData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
			passwordLabel.setLayoutData(layoutData);
			passwordLabel.setText(UtilitiesNLS.SDKLoginPasswordDialog_PasswordLabel);

			final Text passwordText = new Text(mainComposite, SWT.SINGLE | SWT.BORDER | SWT.PASSWORD);
			layoutData = new GridData(SWT.FILL, SWT.CENTER, true, false);
			passwordText.setLayoutData(layoutData);
			passwordText.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					password = passwordText.getText();
				}
			});

			return topComposite;
		}

		public String getTypedLogin() {
			return login;
		}

		public String getTypedPassword() {
			return password;
		}

	}
}

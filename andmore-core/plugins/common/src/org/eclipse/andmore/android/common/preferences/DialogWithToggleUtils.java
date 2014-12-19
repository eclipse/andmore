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
package org.eclipse.andmore.android.common.preferences;

import org.eclipse.andmore.android.common.CommonPlugin;
import org.eclipse.andmore.android.common.utilities.i18n.UtilitiesNLS;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.service.prefs.BackingStoreException;

/**
 * DESCRIPTION: This class provides static methods to create toggle dialogs,
 * storing the user action into preference file. It allows resetting the
 * preference to their original status. <br>
 * RESPONSIBILITY: Provides the basic functionalities for questioning/informing
 * user and persisting the choice made <br>
 * COLABORATORS: none <br>
 * USAGE: - use showQuestion method to provide behavior for user choice between
 * "yes" and "no" option. - use showInformation method to provide behavior for
 * showing informational dialog to user - use resetAllDialogsConfiguration
 * method to reset to initial dialog configuration.
 */
public class DialogWithToggleUtils {

	// suffix of the preference key of the all toggle dialogs.
	private final static String TOGGLE_DIALOG = ".toggle.dialog";

	private static IEclipsePreferences getPreferences() {
		IScopeContext scope = InstanceScope.INSTANCE;
		return scope.getNode(CommonPlugin.PLUGIN_ID);
	}

	/**
	 * Shows a dialog with "Yes" and "No" buttons. The dialog is opened only if
	 * it is the first time that the dialog is shown or if the user did not
	 * check the option "Do not show this window again" when the dialog had been
	 * opened previously.
	 *
	 * @param preferenceKey
	 *            the key to use when persisting the user's preference.
	 * @param title
	 *            the dialog's title, or <code>null</code> if none.
	 * @param message
	 *            the dialog's message.
	 * @return if the dialog was opened: true, if the user pressed "Yes". if the
	 *         dialog was not opened: true, if the option was set to "Always".
	 */
	public static boolean showQuestion(final String preferenceKey, final String title, final String message) {
		final Boolean[] reply = new Boolean[1];

		final String prefKey = preferenceKey + TOGGLE_DIALOG;

		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				AbstractUIPlugin plugin = CommonPlugin.getDefault();
				IPreferenceStore store = plugin.getPreferenceStore();

				String preferenceValue = store.getString(prefKey);

				if (MessageDialogWithToggle.PROMPT.equals(preferenceValue) || (preferenceValue.length() == 0)) {

					IWorkbench workbench = PlatformUI.getWorkbench();
					IWorkbenchWindow ww = workbench.getActiveWorkbenchWindow();
					Shell shell = ww.getShell();

					MessageDialogWithToggle dialog = MessageDialogWithToggle.openYesNoQuestion(shell, title, message,
							UtilitiesNLS.UI_DoNotShowMEAgain, false, store, prefKey);
					reply[0] = (dialog.getReturnCode() == IDialogConstants.YES_ID);
				} else {
					reply[0] = preferenceValue.equals(MessageDialogWithToggle.ALWAYS);
				}
			}
		});

		return reply[0];
	}

	/**
	 * Shows an information dialog to user. The dialog is opened only if it is
	 * the first time that the dialog is shown or if the user did not checked
	 * the option "Do not show this window again" when the dialog had been
	 * opened previously.
	 *
	 * @param preferenceKey
	 *            the key to use when persisting the user's preference
	 * @param title
	 *            the dialog's title, or <code>null</code> if none
	 * @param message
	 *            the dialog�s message
	 * 
	 */
	public static void showInformation(final String preferenceKey, final String title, final String message) {
		final String prefKey = preferenceKey + TOGGLE_DIALOG;

		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				AbstractUIPlugin plugin = CommonPlugin.getDefault();
				IPreferenceStore store = plugin.getPreferenceStore();

				String preferenceValue = store.getString(prefKey);

				if (MessageDialogWithToggle.PROMPT.equals(preferenceValue) || (preferenceValue.length() == 0)) {
					IWorkbench workbench = PlatformUI.getWorkbench();
					IWorkbenchWindow ww = workbench.getActiveWorkbenchWindow();
					Shell shell = ww.getShell();

					MessageDialogWithToggle.openInformation(shell, title, message, UtilitiesNLS.UI_DoNotShowMEAgain,
							false, store, prefKey);
				}
			}
		});
	}

	/**
	 * Shown a warning dialog. The dialog is opened only if it is the first time
	 * that the dialog is shown or if the user did not checked the option
	 * "Do not show this window again" when the dialog had been opened
	 * previously.
	 *
	 * @param preferenceKey
	 *            the key to use when persisting the user's preference
	 * @param title
	 *            the dialog's title, or <code>null</code> if none
	 * @param message
	 *            the dialog�s message
	 * 
	 */
	public static void showWarning(final String preferenceKey, final String title, final String message) {
		final String prefKey = preferenceKey + TOGGLE_DIALOG;

		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				AbstractUIPlugin plugin = CommonPlugin.getDefault();
				IPreferenceStore store = plugin.getPreferenceStore();

				String preferenceValue = store.getString(prefKey);

				if (MessageDialogWithToggle.PROMPT.equals(preferenceValue) || (preferenceValue.length() == 0)) {
					IWorkbench workbench = PlatformUI.getWorkbench();
					IWorkbenchWindow ww = workbench.getActiveWorkbenchWindow();
					Shell shell = ww.getShell();

					MessageDialogWithToggle.openWarning(shell, title, message, UtilitiesNLS.UI_DoNotShowMEAgain, false,
							store, prefKey);
				}
			}
		});
	}

	/**
	 * Show a confirmation message.
	 *
	 * @param preferenceKey
	 *            the key to use when persisting the user's preference;
	 *            <code>null</code> if you don't want it persisted.
	 * @param title
	 *            the dialog's title, or <code>null</code> if none
	 * @param message
	 *            the dialog�s message
	 */
	public static boolean showConfirmation(final String preferenceKey, final String title, final String message) {
		final Boolean[] reply = new Boolean[1];

		final String prefKey = preferenceKey + TOGGLE_DIALOG;

		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				AbstractUIPlugin plugin = CommonPlugin.getDefault();
				IPreferenceStore store = plugin.getPreferenceStore();

				String preferenceValue = store.getString(prefKey);

				if (MessageDialogWithToggle.PROMPT.equals(preferenceValue) || (preferenceValue.length() == 0)) {

					IWorkbench workbench = PlatformUI.getWorkbench();
					IWorkbenchWindow ww = workbench.getActiveWorkbenchWindow();
					Shell shell = ww.getShell();

					MessageDialogWithToggle dialog = MessageDialogWithToggle.openOkCancelConfirm(shell, title, message,
							UtilitiesNLS.UI_AlwaysProceed, false, store, prefKey);
					reply[0] = (dialog.getReturnCode() == IDialogConstants.OK_ID);
				} else {
					reply[0] = preferenceValue.equals(MessageDialogWithToggle.ALWAYS);
				}
			}
		});

		return reply[0];
	}

	/**
	 * Shows an error dialog. The dialog is opened only if it is the first time
	 * that the dialog is shown or if the user did not checked the option
	 * "Do not show this window again" when the dialog had been opened
	 * previously.
	 *
	 * @param preferenceKey
	 *            the key to use when persisting the user's preference
	 * @param title
	 *            the dialog's title, or <code>null</code> if none
	 * @param message
	 *            the dialog�s message
	 * 
	 */
	public static void showError(final String preferenceKey, final String title, final String message) {
		final String prefKey = preferenceKey + TOGGLE_DIALOG;

		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				AbstractUIPlugin plugin = CommonPlugin.getDefault();
				IPreferenceStore store = plugin.getPreferenceStore();

				String preferenceValue = store.getString(prefKey);

				if (MessageDialogWithToggle.PROMPT.equals(preferenceValue) || (preferenceValue.length() == 0)) {
					IWorkbench workbench = PlatformUI.getWorkbench();
					IWorkbenchWindow ww = workbench.getActiveWorkbenchWindow();
					Shell shell = ww.getShell();

					MessageDialogWithToggle.openError(shell, title, message, UtilitiesNLS.UI_DoNotShowMEAgain, false,
							store, prefKey);
				}
			}
		});
	}

	/**
	 * Resets all dialog configuration clearing all 'Do not show me again'
	 * settings and showing all hidden dialogs again.
	 */
	public static void resetAllDialogsConfiguration() {
		IEclipsePreferences preferences = getPreferences();
		String[] propertyNames;
		try {
			propertyNames = preferences.keys();
		} catch (BackingStoreException e) {
			propertyNames = new String[0];
		}

		for (String propertyName : propertyNames) {
			if (propertyName.contains(TOGGLE_DIALOG)) {
				preferences.put(propertyName, MessageDialogWithToggle.PROMPT);
			}
		}
		try {
			preferences.flush();
		} catch (BackingStoreException e) {
			// do nothing
		}
	}

	/**
	 * Set a preference key to a certain value This key is used to toggle
	 * dialogs. Do not use it for general proposes
	 * 
	 * @param preferenceKey
	 * @param value
	 */
	public static void setToggleDialogPreferenceKey(final String preferenceKey, final String value) {
		final String prefKey = preferenceKey + TOGGLE_DIALOG;
		AbstractUIPlugin plugin = CommonPlugin.getDefault();
		IPreferenceStore store = plugin.getPreferenceStore();

		store.setValue(prefKey, value);
	}

	/**
	 * Get a preference toggle dialog preference key This key is used to toggle
	 * dialogs. Do not use it for general proposes
	 * 
	 * @param preferenceKey
	 * @return the key, or null if it is default (not set)
	 */
	public static String getToggleDialogPreferenceKey(final String preferenceKey) {
		final String prefKey = preferenceKey + TOGGLE_DIALOG;
		IPreferenceStore store = CommonPlugin.getDefault().getPreferenceStore();

		String prefValue = null;
		if (!store.isDefault(prefKey)) {
			prefValue = store.getString(prefKey);
		}
		return prefValue;
	}
}
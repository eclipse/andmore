package org.eclipse.andmore.integration.tests;


import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.junit.Assert;

public class DefaultDialogProcessor implements IDialogProcessor {

	@Override
	public void processDialog(Object dialog) {

		/**
		 * If this is a ProgressMonitorDialog, then ignore. The
		 * ProgressMonitorDialog does not block the UI from running.
		 */
		if (dialog instanceof ProgressMonitorDialog) {
			return;
		}

		System.out.println("Processing dialog: " + dialog.getClass().getName());

		// Handle jface dialog
		if (dialog instanceof org.eclipse.jface.dialogs.Dialog) {
			org.eclipse.jface.dialogs.Dialog jfaceDialog = (org.eclipse.jface.dialogs.Dialog) dialog;
			jfaceDialog.close();
			return;
		}

		// Handle swt dialog
		if (dialog instanceof org.eclipse.swt.widgets.Dialog) {
			Assert.fail("org.eclipse.swt.widgets.Dialog is currently not supported");
			return;
		}

		// Handle dialogPage. These are typically some sort of wizard
		if (dialog instanceof DialogPage) {
			Assert.fail("DialogPage is currently not supported");
			return;
		}

	}

}
package org.eclipse.andmore.integration.tests;

public interface IDialogProcessor {

	// This method is called to process the found dialog.
	// The dialog is passed in as an Object because eclipse does not have
	// one root dialog.
	public void processDialog(Object dialog);
}
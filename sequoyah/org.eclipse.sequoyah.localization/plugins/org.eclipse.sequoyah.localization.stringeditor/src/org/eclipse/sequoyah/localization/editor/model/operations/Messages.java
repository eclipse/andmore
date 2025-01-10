package org.eclipse.sequoyah.localization.editor.model.operations;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.sequoyah.localization.editor.model.operations.messages"; //$NON-NLS-1$
	public static String CloneOperation_0;
	public static String EditCellOperation_0;
	public static String TranslateOperation_0;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}

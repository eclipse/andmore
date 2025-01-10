/********************************************************************************
 * Copyright (c) 2007-2010 Motorola Inc. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Initial Contributor:
 * Fabio Fantato (Motorola)
 *
 * Contributors:
 * {Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/

package org.eclipse.sequoyah.device.framework.ui;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

/**
 * Perspective for Emulator framework
 * @author Fabio Fantato
 *
 */
public class DevicePerspectiveFactory implements IPerspectiveFactory {
	private static String NAVIGATOR = "navigator"; //$NON-NLS-1$
	private static String CONTROL   = "control"; //$NON-NLS-1$
	private static String CENTER   = "center"; //$NON-NLS-1$
	private static String VNC   = "vnc"; //$NON-NLS-1$
	/**
	 * Shortcut to New Folder Wizard
	 */
	public static final String SHORTCUT_FOLDER = "org.eclipse.ui.wizards.new.folder"; //$NON-NLS-1$

	/**
	 * Shortcut to New File Wizard
	 */
	public static final String SHORTCUT_FILE = "org.eclipse.ui.wizards.new.file"; //$NON-NLS-1$

	public static final String VIEW_INSTANCE_MGT = "org.eclipse.sequoyah.device.framework.ui.InstanceMgtView"; //$NON-NLS-1$
	public static final String VIEW_DEVICE_INSTANCE = "org.eclipse.sequoyah.device.framework.tree.ui.InstanceView"; //$NON-NLS-1$
	public static final String VIEW_DEVICE_PLUGIN = "org.eclipse.sequoyah.device.framework.tree.ui.DeviceView"; //$NON-NLS-1$
	/**
	 * Console view identifier (value <code>"org.eclipse.ui.console.ConsoleView"</code>).
	 */
	public static final String ID_CONSOLE_VIEW= "org.eclipse.ui.console.ConsoleView"; //$NON-NLS-1$
	public static final String VNC_VIEWER_VIEW="org.eclipse.sequoyah.vnc.vncviewer.vncviews.views.VNCViewerView"; //$NON-NLS-1$
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPerspectiveFactory#createInitialLayout(org.eclipse.ui.IPageLayout)
	 */
	public void createInitialLayout(IPageLayout layout) {
		 defineActions(layout);
		 defineLayout(layout);
	}

	/**
	 * Define actions 
	 * @param layout
	 */
	@SuppressWarnings("deprecation")
	private void defineActions(IPageLayout layout) {
        layout.addNewWizardShortcut(SHORTCUT_FOLDER);
        layout.addNewWizardShortcut(SHORTCUT_FILE);
        layout.addShowViewShortcut(IPageLayout.ID_RES_NAV);
        layout.addShowViewShortcut(VIEW_DEVICE_INSTANCE);
    }
	
	/**
	 * Define layout
	 * @param layout
	 */
	@SuppressWarnings("deprecation")
	private void defineLayout(IPageLayout layout) {
        String editorArea = layout.getEditorArea();
        layout.setEditorAreaVisible(false);
        IFolderLayout navigator  = layout.createFolder(NAVIGATOR, IPageLayout.LEFT, (float) 0.26, editorArea);
        IFolderLayout center     = layout.createFolder(CENTER, IPageLayout.RIGHT, (float) 0.60, editorArea);
        IFolderLayout bottom     = layout.createFolder(CONTROL, IPageLayout.BOTTOM, (float) 0.60, CENTER);
        IFolderLayout vnc        = layout.createFolder(VNC, IPageLayout.RIGHT, (float) 0.60, CENTER);
      
        navigator.addView(VIEW_DEVICE_INSTANCE);
        navigator.addView(IPageLayout.ID_RES_NAV);
        bottom.addView(VIEW_DEVICE_PLUGIN);
        bottom.addView(ID_CONSOLE_VIEW);
        center.addView(VIEW_INSTANCE_MGT);        
        vnc.addPlaceholder(VNC_VIEWER_VIEW);        
        vnc.addView(VNC_VIEWER_VIEW);
}


	
}

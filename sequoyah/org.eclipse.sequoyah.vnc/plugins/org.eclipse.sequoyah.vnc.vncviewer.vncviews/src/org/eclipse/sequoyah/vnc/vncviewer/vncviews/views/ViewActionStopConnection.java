/********************************************************************************
 * Copyright (c) 2007-2010 Motorola Inc and Others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Initial Contributor:
 * Daniel Barboza Franco (Eldorado Research Institute) -  [248037] - Action for stop connection on VNC Viewer
 *
 * Contributors:
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/
package org.eclipse.sequoyah.vnc.vncviewer.vncviews.views;

import java.io.IOException;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

public class ViewActionStopConnection implements IViewActionDelegate {

	
	private IViewPart targetPart;
	
	
	
	public void init(IViewPart view) {

		this.targetPart = view;

	}

	
	public void run(IAction action) {

		VNCViewerView.stop();
		try {
			VNCViewerView.stopProtocol();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	
	public void selectionChanged(IAction action, ISelection selection) {
	

	}

}

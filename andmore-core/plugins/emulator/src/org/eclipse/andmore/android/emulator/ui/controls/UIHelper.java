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
package org.eclipse.andmore.android.emulator.ui.controls;

import org.eclipse.andmore.android.emulator.core.model.IAndroidEmulatorInstance;
import org.eclipse.andmore.android.emulator.ui.controls.maindisplay.MainDisplayComposite;
import org.eclipse.sequoyah.vnc.vncviewer.graphics.IRemoteDisplay;
import org.eclipse.sequoyah.vnc.vncviewer.graphics.swt.SWTRemoteDisplay;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

/**
 * DESCRIPTION: This class provides helper methods for Android Composite UI
 *
 * RESPONSIBILITY: - Provide utility methods related to Android Composite UI
 *
 * COLABORATORS: None.
 *
 * USAGE: Use any of the public methods for getting help with skin UI
 */
public class UIHelper {
	/**
	 * Retrieves the instance related to a given control
	 * 
	 * @param control
	 *            The control associated to an instance, which is to be
	 *            retrieved
	 * 
	 * @return The instance associated with the control
	 */
	public static IAndroidEmulatorInstance getInstanceAssociatedToControl(Control control) {
		IAndroidEmulatorInstance result = null;
		Control composite = null;
		TabFolder folder = null;

		if (control instanceof ScrolledComposite) {
			composite = ((ScrolledComposite) control).getContent();
			folder = (TabFolder) composite.getParent();
		} else if (control instanceof MainDisplayComposite) {
			composite = control.getParent();
			folder = (TabFolder) control.getParent().getParent();
		} else if ((control instanceof Composite) && (control instanceof IAndroidComposite)) {
			composite = control;
			folder = (TabFolder) control.getParent();
		} else if ((control instanceof SWTRemoteDisplay) || (control instanceof RemoteCLIDisplay)) {
			composite = control.getParent();
			folder = (TabFolder) composite.getParent();
		}

		if (folder != null) {
			TabItem[] items = folder.getItems();
			for (TabItem item : items) {
				if (item.getControl() == composite) {
					result = (IAndroidEmulatorInstance) item.getData();
					break;
				}
			}
		}

		return result;
	}

	public static SWTRemoteDisplay getRemoteDisplayAssociatedToControl(Control control) {

		SWTRemoteDisplay remoteDisplay = null;

		if ((control instanceof Composite) && (control instanceof IAndroidComposite)) {
			for (Control childControl : ((Composite) control).getChildren()) {
				if (childControl instanceof SWTRemoteDisplay) {
					remoteDisplay = (SWTRemoteDisplay) childControl;
					break;
				}
			}
		}

		return remoteDisplay;
	}

	/**
	 * Ajusts the x,y coordinates of the mouse event according to the current
	 * zoom and rotation. Coordinates are originally set with the x,y
	 * coordinates of the UI element which may be resized according to zoom and
	 * rotation, but the x,y coordinates expected by the Android emulator is
	 * independent of the zoom and rotation.
	 * 
	 * @param e
	 *            the mouse event whose coordinates will be ajusted.
	 */
	public static void ajustCoordinates(MouseEvent e, IAndroidComposite composite) {
		int x;
		int y;

		SWTRemoteDisplay mainDisplay = UIHelper.getRemoteDisplayAssociatedToControl((Control) composite);
		IRemoteDisplay.Rotation rotation = mainDisplay.getRotation();
		double zoomFactor = composite.getZoomFactor();

		switch (rotation) {
		case ROTATION_90DEG_COUNTERCLOCKWISE:
			x = mainDisplay.getScreenWidth() - (int) (e.y / zoomFactor);
			y = (int) (e.x / zoomFactor);
			break;
		default:
			x = (int) (e.x / zoomFactor);
			y = (int) (e.y / zoomFactor);
		}

		e.x = x;
		e.y = y;
	}
}

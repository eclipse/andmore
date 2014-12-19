/*
 * Copyright (C) 2007 The Android Open Source Project
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

package org.eclipse.andmore.ddms.views;

import com.android.ddmlib.Client;
import com.android.ddmlib.IDevice;
import com.android.ddmuilib.ImageLoader;
import com.android.ddmuilib.explorer.DeviceExplorer;

import org.eclipse.andmore.ddms.CommonAction;
import org.eclipse.andmore.ddms.DdmsPlugin;
import org.eclipse.andmore.ddms.DdmsPlugin.ISelectionListener;
import org.eclipse.andmore.ddms.i18n.Messages;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

public class FileExplorerView extends ViewPart implements ISelectionListener {

	public static final String ID = "org.eclipse.andmore.ddms.views.FileExplorerView"; //$NON-NLS-1$

	private final static String COLUMN_NAME = DdmsPlugin.PLUGIN_ID + ".explorer.name"; //$NON-NLS-1S
	private final static String COLUMN_SIZE = DdmsPlugin.PLUGIN_ID + ".explorer.size"; //$NON-NLS-1S
	private final static String COLUMN_DATE = DdmsPlugin.PLUGIN_ID + ".explorer.data"; //$NON-NLS-1S
	private final static String COLUMN_TIME = DdmsPlugin.PLUGIN_ID + ".explorer.time"; //$NON-NLS-1S
	private final static String COLUMN_PERMISSIONS = DdmsPlugin.PLUGIN_ID + ".explorer.permissions"; //$NON-NLS-1S
	private final static String COLUMN_INFO = DdmsPlugin.PLUGIN_ID + ".explorer.info"; //$NON-NLS-1$

	private DeviceExplorer mExplorer;

	public FileExplorerView() {
	}

	@Override
	public void createPartControl(Composite parent) {
		ImageLoader loader = ImageLoader.getDdmUiLibLoader();

		DeviceExplorer.COLUMN_NAME = COLUMN_NAME;
		DeviceExplorer.COLUMN_SIZE = COLUMN_SIZE;
		DeviceExplorer.COLUMN_DATE = COLUMN_DATE;
		DeviceExplorer.COLUMN_TIME = COLUMN_TIME;
		DeviceExplorer.COLUMN_PERMISSIONS = COLUMN_PERMISSIONS;
		DeviceExplorer.COLUMN_INFO = COLUMN_INFO;

		// device explorer
		mExplorer = new DeviceExplorer();

		mExplorer.setCustomImages(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FILE),
				PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER), null /*
																										 * apk
																										 * image
																										 */, PlatformUI
						.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_ELEMENT));

		// creates the actions
		CommonAction pushAction = new CommonAction(Messages.FileExplorerView_Push_File) {
			@Override
			public void run() {
				mExplorer.pushIntoSelection();
			}
		};
		pushAction.setToolTipText(Messages.FileExplorerView_Push_File_Onto_Device);
		pushAction.setImageDescriptor(loader.loadDescriptor("push.png")); //$NON-NLS-1$
		pushAction.setEnabled(false);

		CommonAction pullAction = new CommonAction(Messages.FileExplorerView_Pull_File) {
			@Override
			public void run() {
				mExplorer.pullSelection();
			}
		};
		pullAction.setToolTipText(Messages.FileExplorerView_Pull_File_From_File);
		pullAction.setImageDescriptor(loader.loadDescriptor("pull.png")); //$NON-NLS-1$
		pullAction.setEnabled(false);

		CommonAction deleteAction = new CommonAction(Messages.FileExplorerView_Delete) {
			@Override
			public void run() {
				mExplorer.deleteSelection();
			}
		};
		deleteAction.setToolTipText(Messages.FileExplorerView_Delete_The_Selection);
		deleteAction.setImageDescriptor(loader.loadDescriptor("delete.png")); //$NON-NLS-1$
		deleteAction.setEnabled(false);

		CommonAction createNewFolderAction = new CommonAction("New Folder") {
			@Override
			public void run() {
				mExplorer.createNewFolderInSelection();
			}
		};
		createNewFolderAction.setToolTipText("New Folder");
		createNewFolderAction.setImageDescriptor(loader.loadDescriptor("add.png")); //$NON-NLS-1$
		createNewFolderAction.setEnabled(false);

		// set up the actions in the explorer
		mExplorer.setActions(pushAction, pullAction, deleteAction, createNewFolderAction);

		// and in the ui
		IActionBars actionBars = getViewSite().getActionBars();
		IMenuManager menuManager = actionBars.getMenuManager();
		IToolBarManager toolBarManager = actionBars.getToolBarManager();

		menuManager.add(pullAction);
		menuManager.add(pushAction);
		menuManager.add(new Separator());
		menuManager.add(deleteAction);
		menuManager.add(new Separator());
		menuManager.add(createNewFolderAction);

		toolBarManager.add(pullAction);
		toolBarManager.add(pushAction);
		toolBarManager.add(new Separator());
		toolBarManager.add(deleteAction);
		toolBarManager.add(new Separator());
		toolBarManager.add(createNewFolderAction);

		mExplorer.createPanel(parent);

		DdmsPlugin.getDefault().addSelectionListener(this);
	}

	@Override
	public void setFocus() {
		mExplorer.setFocus();
	}

	/**
	 * Sent when a new {@link Client} is selected.
	 *
	 * @param selectedClient
	 *            The selected client.
	 */
	@Override
	public void selectionChanged(Client selectedClient) {
		// pass
	}

	/**
	 * Sent when a new {@link Device} is selected.
	 *
	 * @param selectedDevice
	 *            the selected device.
	 */
	@Override
	public void selectionChanged(IDevice selectedDevice) {
		mExplorer.switchDevice(selectedDevice);
	}

	/**
	 * Sent when there is no current selection.
	 */
	public void selectionRemoved() {

	}

}

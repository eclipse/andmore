/*
 * Copyright (C) 2008 The Android Open Source Project
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

import com.android.ddmuilib.ImageLoader;
import com.android.ddmuilib.log.event.EventLogPanel;

import org.eclipse.andmore.ddms.CommonAction;
import org.eclipse.andmore.ddms.i18n.Messages;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;

public class EventLogView extends SelectionDependentViewPart {

	private EventLogPanel mLogPanel;

	@Override
	public void createPartControl(Composite parent) {
		ImageLoader loader = ImageLoader.getDdmUiLibLoader();

		// create the external actions
		CommonAction optionsAction = new CommonAction(Messages.EventLogView_Options);
		optionsAction.setToolTipText(Messages.EventLogView_Opens_Options_Panel);
		optionsAction.setImageDescriptor(loader.loadDescriptor("edit.png")); //$NON-NLS-1$

		CommonAction clearLogAction = new CommonAction(Messages.EventLogView_Clear_Log);
		clearLogAction.setToolTipText(Messages.EventLogView_Clears_Event_Log);
		clearLogAction.setImageDescriptor(loader.loadDescriptor("clear.png")); //$NON-NLS-1$

		CommonAction saveAction = new CommonAction(Messages.EventLogView_Save_Log);
		saveAction.setToolTipText(Messages.EventLogView_Saves_Event_Log);
		saveAction.setImageDescriptor(loader.loadDescriptor("save.png")); //$NON-NLS-1$

		CommonAction loadAction = new CommonAction(Messages.EventLogView_Load_Log);
		loadAction.setToolTipText(Messages.EventLogView_Loads_Event_Log);
		loadAction.setImageDescriptor(loader.loadDescriptor("load.png")); //$NON-NLS-1$

		CommonAction importBugAction = new CommonAction(Messages.EventLogView_Import_Bug_Report_Log);
		importBugAction.setToolTipText(Messages.EventLogView_Imports_Bug_Report);
		importBugAction.setImageDescriptor(loader.loadDescriptor("importBug.png")); //$NON-NLS-1$

		placeActions(optionsAction, clearLogAction, saveAction, loadAction, importBugAction);

		mLogPanel = new EventLogPanel();
		mLogPanel.setActions(optionsAction, clearLogAction, saveAction, loadAction, importBugAction);
		mLogPanel.createPanel(parent);
		setSelectionDependentPanel(mLogPanel);
	}

	@Override
	public void setFocus() {
		mLogPanel.setFocus();
	}

	@Override
	public void dispose() {
		if (mLogPanel != null) {
			mLogPanel.stopEventLog(true);
		}
	}

	/**
	 * Places the actions in the toolbar and in the menu.
	 * 
	 * @param importBugAction
	 */
	private void placeActions(IAction optionAction, IAction clearAction, IAction saveAction, IAction loadAction,
			CommonAction importBugAction) {
		IActionBars actionBars = getViewSite().getActionBars();

		// first in the menu
		IMenuManager menuManager = actionBars.getMenuManager();
		menuManager.add(clearAction);
		menuManager.add(new Separator());
		menuManager.add(saveAction);
		menuManager.add(loadAction);
		menuManager.add(importBugAction);
		menuManager.add(new Separator());
		menuManager.add(optionAction);

		// and then in the toolbar
		IToolBarManager toolBarManager = actionBars.getToolBarManager();
		toolBarManager.add(clearAction);
		toolBarManager.add(new Separator());
		toolBarManager.add(saveAction);
		toolBarManager.add(loadAction);
		toolBarManager.add(importBugAction);
		toolBarManager.add(new Separator());
		toolBarManager.add(optionAction);
	}

}

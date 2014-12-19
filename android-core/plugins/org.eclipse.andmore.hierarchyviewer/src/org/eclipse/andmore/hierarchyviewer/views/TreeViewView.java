/*
 * Copyright (C) 2010 The Android Open Source Project
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

package org.eclipse.andmore.hierarchyviewer.views;

import com.android.hierarchyviewerlib.actions.CapturePSDAction;
import com.android.hierarchyviewerlib.actions.DisplayViewAction;
import com.android.hierarchyviewerlib.actions.DumpDisplayListAction;
import com.android.hierarchyviewerlib.actions.InvalidateAction;
import com.android.hierarchyviewerlib.actions.ProfileNodesAction;
import com.android.hierarchyviewerlib.actions.RefreshViewAction;
import com.android.hierarchyviewerlib.actions.RequestLayoutAction;
import com.android.hierarchyviewerlib.actions.SaveTreeViewAction;
import com.android.hierarchyviewerlib.ui.TreeView;
import com.android.hierarchyviewerlib.ui.TreeViewControls;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.part.ViewPart;

// Awesome name.
public class TreeViewView extends ViewPart {

	public static final String ID = "org.eclipse.andmore.hierarchyviewer.views.TreeViewView"; //$NON-NLS-1$

	private TreeView mTreeView;

	@Override
	public void createPartControl(Composite parent) {
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		layout.horizontalSpacing = layout.verticalSpacing = 0;
		parent.setLayout(layout);

		Composite treeViewContainer = new Composite(parent, SWT.BORDER);
		treeViewContainer.setLayoutData(new GridData(GridData.FILL_BOTH));
		treeViewContainer.setLayout(new FillLayout());

		mTreeView = new TreeView(treeViewContainer);

		TreeViewControls treeViewControls = new TreeViewControls(parent);
		treeViewControls.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		placeActions();
	}

	public void placeActions() {
		IActionBars actionBars = getViewSite().getActionBars();

		IMenuManager mm = actionBars.getMenuManager();
		mm.removeAll();
		mm.add(SaveTreeViewAction.getAction(getSite().getShell()));
		mm.add(CapturePSDAction.getAction(getSite().getShell()));
		mm.add(new Separator());
		mm.add(RefreshViewAction.getAction());
		mm.add(DisplayViewAction.getAction(getSite().getShell()));
		mm.add(new Separator());
		mm.add(InvalidateAction.getAction());
		mm.add(RequestLayoutAction.getAction());
		mm.add(DumpDisplayListAction.getAction());
		mm.add(ProfileNodesAction.getAction());

		IToolBarManager tm = actionBars.getToolBarManager();
		tm.removeAll();
		tm.add(SaveTreeViewAction.getAction(getSite().getShell()));
		tm.add(CapturePSDAction.getAction(getSite().getShell()));
		tm.add(new Separator());
		tm.add(RefreshViewAction.getAction());
		tm.add(DisplayViewAction.getAction(getSite().getShell()));
		tm.add(new Separator());
		tm.add(InvalidateAction.getAction());
		tm.add(RequestLayoutAction.getAction());
		tm.add(DumpDisplayListAction.getAction());
		tm.add(ProfileNodesAction.getAction());
	}

	@Override
	public void setFocus() {
		mTreeView.setFocus();
	}

}

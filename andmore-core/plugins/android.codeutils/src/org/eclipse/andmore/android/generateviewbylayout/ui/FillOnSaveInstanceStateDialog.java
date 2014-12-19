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
package org.eclipse.andmore.android.generateviewbylayout.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.andmore.android.codeutils.i18n.CodeUtilsNLS;
import org.eclipse.andmore.android.generateviewbylayout.codegenerators.SaveStateCodeGenerator;
import org.eclipse.andmore.android.generateviewbylayout.model.LayoutNode;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;

/**
 * Dialog to Save UI state for:
 * <ul>
 * <li>a selected Activity/Fragment
 * <li>a selected layout xml
 * </ul>
 */
public class FillOnSaveInstanceStateDialog extends AbstractLayoutItemsDialog {

	public FillOnSaveInstanceStateDialog(Shell parentShell) {
		super(parentShell, CodeUtilsNLS.FillOnSaveInstanceStateDialog_DialogDescription,
				CodeUtilsNLS.FillOnSaveInstanceStateDialog_DialogTitle,
				CodeUtilsNLS.FillOnSaveInstanceStateDialog_ShellTitle, null);
	}

	@Override
	protected void createCustomContentArea(Composite mainComposite) {
		// default implementation does nothing
	}

	@Override
	protected boolean isResizable() {
		return true;
	}

	@Override
	protected List<LayoutNode> getGuiItemsList() {
		List<LayoutNode> alreadyDeclared = new ArrayList<LayoutNode>();
		if (getCodeGeneratorData() != null) {
			List<LayoutNode> allNodes = getCodeGeneratorData().getGUIItems(false);

			for (LayoutNode node : allNodes) {
				if (node.isAlreadyDeclaredInCode() && !node.isAlreadySaved() && canSaveState(node)) {
					alreadyDeclared.add(node);
				}
			}
		}
		return alreadyDeclared;
	}

	private boolean canSaveState(LayoutNode node) {
		int i = 0;
		boolean canSaveState = false;
		while (!canSaveState && (i < SaveStateCodeGenerator.saveStateNodeTypes.length)) {
			if (SaveStateCodeGenerator.saveStateNodeTypes[i].getNodeType().equals(node.getNodeType())) {
				canSaveState = true;
			}
			i++;
		}

		return canSaveState;
	}

	@Override
	protected void okPressed() {
		for (TableItem item : getViewer().getTable().getItems()) {
			if (item.getData() instanceof LayoutNode) {
				LayoutNode node = (LayoutNode) item.getData();
				node.setSaveState(item.getChecked());
			}
		}
		getModifier().setCodeGeneratorData(getCodeGeneratorData());
		super.okPressed();
	}

}

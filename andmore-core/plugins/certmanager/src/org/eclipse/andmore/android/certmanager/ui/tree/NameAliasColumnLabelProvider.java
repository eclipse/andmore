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
package org.eclipse.andmore.android.certmanager.ui.tree;

import org.eclipse.andmore.android.certmanager.ui.model.ITreeNode;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IDecoratorManager;
import org.eclipse.ui.PlatformUI;

public class NameAliasColumnLabelProvider extends ColumnLabelProvider {
	final IDecoratorManager decorator;

	public NameAliasColumnLabelProvider() {
		decorator = PlatformUI.getWorkbench().getDecoratorManager();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.ColumnLabelProvider#getImage(java.lang.Object)
	 */
	@Override
	public Image getImage(Object element) {
		Image result = null;
		if (element instanceof ITreeNode) {
			ITreeNode node = (ITreeNode) element;
			Image defaultImage = null;
			if (node.getIcon() != null) {
				defaultImage = node.getIcon().createImage();
				result = decorator.decorateImage(defaultImage, element);
			}
			if (result == null) {
				result = defaultImage;
			}
		}
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.ColumnLabelProvider#getText(java.lang.Object)
	 */
	@Override
	public String getText(Object element) {
		if (element instanceof ITreeNode) {
			ITreeNode node = (ITreeNode) element;
			return node.getName();
		}
		return ""; // other items do not need to show this column with data
	}

	@Override
	public void update(ViewerCell cell) {
		Object cellElement = cell.getElement();
		cell.setText(getText(cellElement));
		if (getImage(cellElement) != null) {
			cell.setImage(getImage(cellElement));
		}
	}

	@Override
	public String getToolTipText(Object element) {
		if (element instanceof ITreeNode) {
			ITreeNode treeNode = (ITreeNode) element;

			return treeNode.getTooltip();
		}
		return super.getToolTipText(element);
	}

	@Override
	public int getToolTipTimeDisplayed(Object object) {
		return 4000;
	}

	@Override
	public int getToolTipDisplayDelayTime(Object object) {
		return 500;
	}

	@Override
	public Point getToolTipShift(Object object) {
		return new Point(5, 5);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.BaseLabelProvider#addListener(org.eclipse.jface
	 * .viewers.ILabelProviderListener)
	 */
	@Override
	public void addListener(ILabelProviderListener listener) {
		decorator.addListener(listener);
		super.addListener(listener);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.BaseLabelProvider#removeListener(org.eclipse
	 * .jface.viewers.ILabelProviderListener)
	 */
	@Override
	public void removeListener(ILabelProviderListener listener) {
		decorator.removeListener(listener);
		super.removeListener(listener);
	}
}

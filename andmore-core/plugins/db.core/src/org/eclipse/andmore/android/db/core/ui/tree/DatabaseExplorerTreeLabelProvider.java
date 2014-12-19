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
package org.eclipse.andmore.android.db.core.ui.tree;

import org.eclipse.andmore.android.db.core.i18n.DbCoreNLS;
import org.eclipse.andmore.android.db.core.ui.AbstractTreeNode;
import org.eclipse.andmore.android.db.core.ui.ITreeNode;
import org.eclipse.andmore.android.db.core.ui.view.MOTODEVDatabaseExplorerView;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IDecoratorManager;
import org.eclipse.ui.PlatformUI;

/**
 * Provides label for the tree into {@link MOTODEVDatabaseExplorerView} by
 * encapsulating {@link AbstractTreeNode}
 */
public class DatabaseExplorerTreeLabelProvider extends CellLabelProvider {
	final IDecoratorManager decorator;

	public enum LabelProperty {
		TEXT, IMAGE;
	}

	public DatabaseExplorerTreeLabelProvider() {
		decorator = PlatformUI.getWorkbench().getDecoratorManager();
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

	public Image getImage(Object element) {
		Image result = null;
		if (element instanceof AbstractTreeNode) {
			ITreeNode treeNode = (ITreeNode) element;
			Image defaultImage = treeNode.getIcon().createImage();
			result = decorator.decorateImage(defaultImage, element);
			if (result == null) {
				result = defaultImage;
			}
		}

		return result;
	}

	public String getText(Object element) {
		if (element instanceof AbstractTreeNode) {
			ITreeNode treeNode = (ITreeNode) element;
			return treeNode.getName();
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.BaseLabelProvider#isLabelProperty(java.lang
	 * .Object, java.lang.String)
	 */
	@Override
	public boolean isLabelProperty(Object element, String property) {
		boolean isLabelProperty = false;
		LabelProperty labelProperty = LabelProperty.valueOf(property);
		if (labelProperty != null) {
			isLabelProperty = true;
		}

		return isLabelProperty;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.CellLabelProvider#getToolTipText(java.lang.
	 * Object)
	 */
	@Override
	public String getToolTipText(Object element) {
		if (element instanceof ITreeNode) {
			ITreeNode treeNode = (ITreeNode) element;
			String tooltipText = treeNode.getTooltip();
			IStatus nodeStatus = treeNode.getNodeStatus();
			if ((nodeStatus != null) && !nodeStatus.isOK()) {
				String statusMessage = nodeStatus.getMessage();
				if ((statusMessage != null) && !statusMessage.isEmpty()) {
					tooltipText = NLS.bind(DbCoreNLS.DatabaseExplorerTreeLabelProvider_Error_Tooltip_Prefix,
							statusMessage);
				}
			}
			return tooltipText;
		}
		return super.getToolTipText(element);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.CellLabelProvider#getToolTipTimeDisplayed(java
	 * .lang.Object)
	 */
	@Override
	public int getToolTipTimeDisplayed(Object object) {
		return 4000;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.CellLabelProvider#getToolTipDisplayDelayTime
	 * (java.lang.Object)
	 */
	@Override
	public int getToolTipDisplayDelayTime(Object object) {
		return 500;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.CellLabelProvider#getToolTipShift(java.lang
	 * .Object)
	 */
	@Override
	public Point getToolTipShift(Object object) {
		return new Point(5, 5);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.CellLabelProvider#update(org.eclipse.jface.
	 * viewers.ViewerCell)
	 */
	@Override
	public void update(ViewerCell cell) {
		Object cellElement = cell.getElement();
		cell.setText(getText(cellElement));
		cell.setImage(getImage(cellElement));
	}

}

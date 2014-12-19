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
package org.eclipse.andmore.android.emulator.core.emulationui;

import org.eclipse.andmore.android.emulator.EmulatorPlugin;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerRow;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * DESCRIPTION: This class contains the common logic for classes that retrieve
 * the labels that are presented at the emulation views
 *
 * RESPONSIBILITY: Provide basic support to emulation label providers. This
 * include - Maintaining the column index for determining which data is to be
 * retrieved from the beans - Maintaining which is the column at the view left
 * edge, to draw the tree correctly at that position - Updating the viewer cells
 * in a standardized way, giving support to coloring a line in alternative color
 *
 * COLABORATORS: None.
 *
 * USAGE: The class is used when a emulation view content provider is
 * instantiated
 */
public abstract class AbstractEmuLabelProvider extends ColumnLabelProvider {
	/**
	 * The column that is being updated by this label provider
	 */
	protected int columnIndex;

	/**
	 * The index of the column that is currently in the left edge of the viewer
	 */
	protected int firstColumnIndex;

	/**
	 * The host that contains the bean that should be painted in an alternative
	 * color or <code>null</code> if no bean will have alternative color
	 */
	private String alternativeColorHost;

	/**
	 * The id that identifies the bean that should be painted in an alternative
	 * color or <code>null</code> if no bean will have alternative color
	 */
	private long alternativeColorBeanId;

	/**
	 * The color that is used to paint highlighted nodes
	 */
	private final Color alternativeColor = new Color(PlatformUI.getWorkbench().getDisplay(), 255, 255, 0);

	/**
	 * Sets the index of the column that is currently at the left edge of the
	 * viewer
	 * 
	 * @param firstColumnIndex
	 *            The index of the column that is currently at the left edge of
	 *            the viewer
	 */
	public void setFirstColumnIndex(int firstColumnIndex) {
		this.firstColumnIndex = firstColumnIndex;
	}

	/**
	 * Retrieves the index of the column that is currently at the left edge of
	 * the viewer
	 * 
	 * @return firstColumnIndex The index of the column that is currently at the
	 *         left edge of the viewer
	 */
	public int getFirstColumnIndex() {
		return firstColumnIndex;
	}

	/**
	 * Sets the host that contains the bean that should be painted in an
	 * alternative color
	 * 
	 * @param host
	 *            The host that contains the bean to be colored in alternative
	 *            way, or <code>null</code> if no bean shall be colored in
	 *            alternative way
	 */
	public void setAlternativeColorHost(String host) {
		this.alternativeColorHost = host;
	}

	/**
	 * Sets the id that identifies the bean that should be painted in an
	 * alternative color
	 * 
	 * @param beanId
	 *            The id that identifies the bean that should be painted in an
	 *            alternative color or <code>null</code> if no bean will have
	 *            alternative color
	 */
	public void setAlternativeColorBeanId(long beanId) {
		this.alternativeColorBeanId = beanId;
	}

	/**
	 * @see org.eclipse.jface.viewers.CellLabelProvider#update(org.eclipse.jface.viewers.ViewerCell)
	 */
	@Override
	public void update(ViewerCell cell) {
		// The instance column index is set with the current cell column index,
		// as the logic
		// contained in this class depends on this information. Then after the
		// cell is
		// updated according to the standard procedure, the column index field
		// is reset so that
		// it does not interfere with subsequent updates.
		columnIndex = cell.getColumnIndex();
		super.update(cell);
		columnIndex = firstColumnIndex;

		// Checks if the cell needs to be highlighted. This will be true if the
		// values of
		// alternativeColorHost and alternativeColorBeanId are different from
		// null and -1
		if ((alternativeColorHost != null) && (alternativeColorBeanId != -1)) {

			Object element = cell.getElement();
			// Only leaf nodes can be highlighted
			if (element instanceof EmuViewerLeafNode) {
				// The next lines are used to check if the current element is
				// the one to be
				// highlighted. For that, the host and bean id needs to be
				// compared to the
				// alternativeColorHost and alternativeColorBeanId instance
				// field values
				EmuViewerLeafNode node = (EmuViewerLeafNode) element;
				long beanId = node.getBeanId();
				EmuViewerRootNode root = (EmuViewerRootNode) node.getParent().getParent();
				String host = root.getEmulatorIdentifier();

				if ((beanId == alternativeColorBeanId) && (host.equals(alternativeColorHost))) {
					// Highlighting the node

					cell.setBackground(alternativeColor);

					// Putting the node at the visible part of the tree

					ViewerRow highlightedRow = cell.getViewerRow();
					TreeItem highlightedItem = (TreeItem) highlightedRow.getItem();
					Tree tree = (Tree) cell.getControl();
					tree.showItem(highlightedItem);
				}
			}

		}
	}

	/**
	 * @see org.eclipse.jface.viewers.ILabelProvider#getImage(Object)
	 */
	@Override
	public Image getImage(Object element) {

		Image imageToReturn = null;

		// The image should appear near the label at the first cell in the row.
		// That is why
		// a test is being performed for the first column.
		if ((element instanceof EmuViewerNode) && (isProvidingForFirstColumn())) {
			if (element instanceof EmuViewerRootNode) {
				// Get an common icon for emulator nodes

				ImageDescriptor descriptor;
				descriptor = AbstractUIPlugin.imageDescriptorFromPlugin(EmulatorPlugin.PLUGIN_ID,
						IEmuIconPath.EMULATOR_ICON_PATH);
				if (descriptor != null) {
					imageToReturn = descriptor.createImage();
				}
			} else if (element instanceof EmuViewerLeafNode) {
				// Delegate the get method to the concrete class for the leaf
				// node icon

				imageToReturn = getLeafNodeIcon((EmuViewerLeafNode) element);
			} else {
				// Delegate the get method to the concrete class for the
				// intermediate node icon

				imageToReturn = getIntermediateNodeIcon((EmuViewerNode) element);
			}
		}

		return imageToReturn;
	}

	/**
	 * Tests if the resource being retrieved (text or image) is for a cell at
	 * the first column of the tree viewer
	 * 
	 * @return True if it is providing for the first column. False otherwise
	 */
	protected boolean isProvidingForFirstColumn() {
		return firstColumnIndex == columnIndex;
	}

	/**
	 * Retrieves the icon that shall be displayed next to the provided node
	 * element. The provided node is one of the intermediate nodes in the tree,
	 * and does not represent neither the emulator itself nor the leaf element
	 * 
	 * @param node
	 *            The tree node that will have the returned icon by its side
	 * 
	 * @return The icon that shall be displayed near the provided node
	 */
	/**
	 * @see AbstractEmuLabelProvider#getIntermediateNodeIcon(EmuViewerNode)
	 */
	protected Image getIntermediateNodeIcon(EmuViewerNode node) {

		Image imageToReturn = null;
		ImageDescriptor descriptor;

		if (node.getNodeId().equals(AbstractEmuCtProvider.SENT_TO_EMULATOR_ID)) {
			descriptor = AbstractUIPlugin.imageDescriptorFromPlugin(EmulatorPlugin.PLUGIN_ID,
					IEmuIconPath.SENT_TO_ICON_PATH);
		} else {
			descriptor = AbstractUIPlugin.imageDescriptorFromPlugin(EmulatorPlugin.PLUGIN_ID,
					IEmuIconPath.RECEIVE_FROM_ICON_PATH);
		}

		if (descriptor != null) {
			imageToReturn = descriptor.createImage();
		}

		return imageToReturn;
	}

	/**
	 * Retrieves the icon that shall be displayed next to the provided leaf node
	 * element
	 * 
	 * @param node
	 *            The tree node that will have the returned icon by its side
	 * 
	 * @return The icon that shall be displayed near the provided node
	 */
	protected abstract Image getLeafNodeIcon(EmuViewerLeafNode node);

	/**
	 * Gets the text referring to a particular leaf node, at the provided
	 * column.
	 * 
	 * @param element
	 *            The tree node that identifies the bean that needs to have
	 *            information retrieved from
	 * @param columnIndex
	 *            The id of the column that identifies which information to take
	 *            from the bean
	 * 
	 * @return The text from the bean at the specified column
	 */
	public abstract String getText(EmuViewerLeafNode node, int columnIndex);
}

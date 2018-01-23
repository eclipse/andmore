/********************************************************************************
 * Copyright (c) 2008-2010 Motorola Mobility, Inc. and Other. All rights reserved
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Julia Martinez Perdigueiro (Eldorado Research Institute) 
 * [244805] - Improvements on Instance view  
 *
 * Contributors:
 * Yu-Fen Kuo (MontaVista)  - [236476] - provide a generic device type
 * Daniel Barboza Franco (Eldorado Research Institute) - Bug [259243] - instance management view is showing device type ids instead of names
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 * Julia Martinez Perdigueiro (Eldorado) - [329548] Adding tooltip support for double click behavior
 ********************************************************************************/

package org.eclipse.sequoyah.device.framework.ui.view.provider;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.sequoyah.device.framework.model.IInstance;
import org.eclipse.sequoyah.device.framework.status.IStatus;
import org.eclipse.sequoyah.device.framework.status.StatusRegistry;
import org.eclipse.sequoyah.device.framework.ui.DeviceUIPlugin;
import org.eclipse.sequoyah.device.framework.ui.view.model.ViewerDeviceNode;
import org.eclipse.sequoyah.device.framework.ui.view.model.ViewerInstanceNode;
import org.eclipse.swt.graphics.Image;

public class InstanceMgtViewLabelProvider extends ColumnLabelProvider
{
    public static final int INSTANCE_COLUMN_NUMBER = 0;
    public static final int STATUS_COLUMN_NUMBER = 1;
    
    private static final String NO_INSTANCE_NAME = "<none>"; //$NON-NLS-1$
        
    /**
     * The column that is being updated by this label provider
     */
    protected int columnIndex;

    /**
     * The index of the column that is currently in the left edge of the viewer
     */
    protected int firstColumnIndex;
    

    private Map<ImageDescriptor, Image> imageCache = new HashMap<ImageDescriptor, Image>(20);

    /**
     * Sets the index of the column that is currently at the left edge of the viewer
     * 
     * @param firstColumnIndex The index of the column that is currently at the left 
     *                         edge of the viewer
     */
    public void setFirstColumnIndex(int firstColumnIndex)
    {
        this.firstColumnIndex = firstColumnIndex;
    }
    
    /**
     * Retrieves the index of the column that is currently at the left edge of the viewer
     * 
     * @return firstColumnIndex The index of the column that is currently at the left 
     *                          edge of the viewer
     */
    public int getFirstColumnIndex()
    {
        return firstColumnIndex;
    }
    
    /**
     * Tests if the resource being retrieved (text or image) is for a cell at the first column
     * of the tree viewer
     * 
     * @return True if it is providing for the first column. False otherwise
     */
    protected boolean isProvidingForFirstColumn()
    {
        return firstColumnIndex == columnIndex;
    }

    public String getText(Object element)
    {
        String label = ""; //$NON-NLS-1$
        
        if (element instanceof ViewerInstanceNode)
        {
            ViewerInstanceNode node = (ViewerInstanceNode) element;
            label = getText(node, columnIndex);
        }
        else if (element instanceof ViewerDeviceNode)
        {
            if (isProvidingForFirstColumn())
            {
                ViewerDeviceNode node = (ViewerDeviceNode) element;
                label = node.getDevice().getLabel();
            }
        }        
        
        return label;
    }
    
    public String getText(ViewerInstanceNode node, int columnIndex)
    {
        String label = ""; //$NON-NLS-1$
        if (node.containsInstance())
        {
        if (columnIndex == INSTANCE_COLUMN_NUMBER)
        {
            label = node.getInstanceName();
        }
        else if (columnIndex == STATUS_COLUMN_NUMBER)
        {
            String status = node.getInstanceStatus();
            
            label = StatusRegistry.getInstance().getStatus(status).getName();
        }
        }
        else
        {
        	if (isProvidingForFirstColumn())
        	{
        		label = NO_INSTANCE_NAME;
        	}
        }
        
        return label;
    }
    
    public Image getImage(Object element)
    {
        ImageDescriptor descriptor = null;
        Image image = null;
        
        if (element instanceof ViewerInstanceNode) {
            ViewerInstanceNode node = (ViewerInstanceNode) element;
            if (node.containsInstance())
            {
                if (columnIndex == INSTANCE_COLUMN_NUMBER)
                {
                    descriptor = DeviceUIPlugin.getDefault().getImageDescriptor(DeviceUIPlugin.ICON_DEVICE);
                }
                else if (columnIndex == STATUS_COLUMN_NUMBER)
                {
                    IStatus status = StatusRegistry.getInstance().getStatus(node.getInstanceStatus());
                    descriptor = status.getImage();
                }
            }
        } else if (element instanceof ViewerDeviceNode) {
            if (isProvidingForFirstColumn())
            {
                ViewerDeviceNode node = (ViewerDeviceNode) element;
                image = node.getDevice().getImage();
                return image;
            }
        }
        
        if (descriptor != null)
        {
            //obtain the cached image corresponding to the descriptor
            image = imageCache.get(descriptor);
            if (image == null) {
                image = descriptor.createImage();
                imageCache.put(descriptor, image);
            }
        }

        return image;
    }
    
    public void update(ViewerCell cell)
    {
        // The instance column index is set with the current cell column index, as the logic
        // contained in this class depends on this information. Then after the cell is 
        // updated according to the standard procedure, the column index field is reset so that
        // it does not interfere with subsequent updates. 
        columnIndex = cell.getColumnIndex();
        super.update(cell);
        columnIndex = firstColumnIndex;
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.CellLabelProvider#getToolTipText(java.lang.Object)
     */
    @Override
    public String getToolTipText(Object element) {
    	String tooltipText = null;
    	if (element != null && element instanceof ViewerInstanceNode) {
    		ViewerInstanceNode instanceNode = (ViewerInstanceNode) element;
    		IInstance instance = instanceNode.getInstance();
    		if (instance != null) {
    			String status = instance.getStatus();
    			if (status != null) {
    				tooltipText = StatusRegistry.getInstance().getTooltipTextForStatus(status);
    			}
    		}
    	}
    	return tooltipText;
    }
    
    @Override
    public int getToolTipDisplayDelayTime(Object object) {
      return 0;
    }

    @Override
    public int getToolTipTimeDisplayed(Object object) {
      return 5000;
    }
}

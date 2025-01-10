/********************************************************************************
 * Copyright (c) 2008-2010 Motorola Inc. and Other. All rights reserved
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Julia Martinez Perdigueiro (Eldorado Research Institute) 
 * [244805] - Improvements on Instance view  
 *
 * Contributors:
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/

package org.eclipse.sequoyah.device.framework.ui.view.model;

import java.util.Comparator;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.sequoyah.device.framework.ui.view.provider.InstanceMgtViewLabelProvider;

public class InstanceMgtViewComparator extends ViewerComparator
{
    /**
     * Index of the column that will be used at sorting
     */
    private int columnToSort = -1;

    /**
     * A flag that indicates if the sort is in ascending order or not 
     */
    private boolean isAscending = false;
    
    private InstanceMgtViewLabelProvider labelProvider;
    
    public InstanceMgtViewComparator(InstanceMgtViewLabelProvider labelProvider)
    {
        this.labelProvider = labelProvider;
    }
    /**
     * Sets the column that will be used at sorting
     * 
     * @param columnIndex The index that identifies the column that will be sorted
     */
    public void setColumnToSort(int columnIndex)
    {
        columnToSort = columnIndex;
    }

    /**
     * Changes the sort strategy
     * If it was previously ascending, turns to descending and vice-versa
     */
    public void toggleAscending()
    {
        isAscending = !isAscending;
    }

    /**
     * Tests if the sort strategy is currently ascending
     * 
     * @return True if ascending; false if descending
     */
    public boolean isAscending()
    {
        return isAscending;
    }

    /**
     * @see org.eclipse.jface.viewers.ViewerComparator#compare(Viewer, Object, Object)
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public int compare(Viewer treeViewer, Object e1, Object e2)
    {
        int compareResult;

        // Only the leaf node will have customized sorting strategy
        if ((e1 instanceof ViewerInstanceNode) && (e2 instanceof ViewerInstanceNode))
        {
            // If the columnToSort attribute is set with an invalid value, then use the
            // super sorting strategy
            int columnCount = ((TreeViewer) treeViewer).getTree().getColumnCount();
            if ((columnToSort < 0) || (columnToSort > columnCount))
            {
                compareResult = super.compare(treeViewer, e1, e2);
            }
            else
            {
                // The leaf nodes are sorted according to the label provided for the 
                // column set at columnToSort. The label is retrieved from the label
                // provider and provided to the default comparator.
                ViewerInstanceNode node1 = (ViewerInstanceNode) e1;
                ViewerInstanceNode node2 = (ViewerInstanceNode) e2;

                String label1 = labelProvider.getText(node1, columnToSort);
                String label2 = labelProvider.getText(node2, columnToSort);

                // Depending on the isAscending flag, the comparison is done in an
                // opposite way
                Comparator comparator = getComparator();
                if (isAscending)
                {
                    compareResult = comparator.compare(label1, label2);
                }
                else
                {
                    compareResult = comparator.compare(label2, label1);
                }
            }
        }
        else
        {
            // Non-leaf nodes and other elements will follow the super sorting strategy
            compareResult = super.compare(treeViewer, e1, e2);
        }

        return compareResult;
    }
}

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

import java.util.HashSet;
import java.util.Set;

public abstract class ViewerAbstractNode
{
    /*
     * The parent node of this node
     */
    private final ViewerAbstractNode parent;
    
    /*
     * A set containing all children of this node
     */
    private final Set<ViewerAbstractNode> children = new HashSet<ViewerAbstractNode>();

    public ViewerAbstractNode(ViewerAbstractNode parent)
    {
        this.parent = parent;
    }

    /**
     * Retrieves the node's parent
     * 
     * @return The parent node
     */
    public ViewerAbstractNode getParent()
    {
        return parent;
    }
    /**
     * Adds a new child to this node
     * 
     * @param child The child to be added to the node
     */
    public void addChild(ViewerAbstractNode child)
    {
        children.add(child);
    }

    /**
     * Retrieves all this node's children 
     * 
     * @return A set containing all children of this node
     */
    public Set<ViewerAbstractNode> getChildren()
    {
        return children;
    }
    
    public boolean hasChildren()
    {
        return children.size() > 0;
    }
}

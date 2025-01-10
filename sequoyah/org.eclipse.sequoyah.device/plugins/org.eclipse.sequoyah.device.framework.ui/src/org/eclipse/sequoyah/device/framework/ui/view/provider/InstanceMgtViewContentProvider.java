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
 * Yu-Fen Kuo (MontaVista)  - [236476] - provide a generic device type
 * Fabio Rigo (Eldorado) - Bug [288006] - Unify features of InstanceManager and InstanceRegistry
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/

package org.eclipse.sequoyah.device.framework.ui.view.provider;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.sequoyah.device.common.utilities.BasePlugin;
import org.eclipse.sequoyah.device.framework.factory.DeviceTypeRegistry;
import org.eclipse.sequoyah.device.framework.factory.InstanceRegistry;
import org.eclipse.sequoyah.device.framework.model.IDeviceType;
import org.eclipse.sequoyah.device.framework.model.IInstance;
import org.eclipse.sequoyah.device.framework.ui.view.model.ViewerAbstractNode;
import org.eclipse.sequoyah.device.framework.ui.view.model.ViewerDeviceNode;
import org.eclipse.sequoyah.device.framework.ui.view.model.ViewerInstanceNode;
import org.eclipse.ui.IViewSite;

public class InstanceMgtViewContentProvider implements ITreeContentProvider
{
    /*
     * The parent of the entire tree
     */
    private static IViewSite treeParent;

    public Object[] getChildren(Object parentElement)
    {
        Object[] children = null;
        if (parentElement instanceof ViewerAbstractNode)
        {
            ViewerAbstractNode parentNode = (ViewerAbstractNode) parentElement;
            children = parentNode.getChildren().toArray();
        }            
        
        return children;
    }

    public Object getParent(Object element)
    {
        Object parentElement = null;
        if (element instanceof ViewerAbstractNode)
        {   
            ViewerAbstractNode node = (ViewerAbstractNode) element;
            parentElement = node.getParent();
        }
        
        return parentElement;
    }

    public boolean hasChildren(Object element)
    {
        return (getChildren(element).length > 0);
    }

    public Object[] getElements(Object inputElement)
    {
        Object[] elements;
        
        if (inputElement instanceof IViewSite)
        {
            if (treeParent == null)
            {
                treeParent = (IViewSite) inputElement;
            }
            
            Map<String, ViewerDeviceNode> deviceNodeMap = new HashMap<String, ViewerDeviceNode>();
            
            for (IDeviceType device : DeviceTypeRegistry.getInstance().getDeviceTypes())
            {
            	if (!device.isAbstract()) {
            		ViewerDeviceNode deviceNode = new ViewerDeviceNode(device);
            		deviceNodeMap.put(device.getId(), deviceNode);
            	}
            }
            
            InstanceRegistry instanceRegistry = InstanceRegistry.getInstance();
            for (IInstance instance : instanceRegistry.getInstances())
            {
                String deviceId = instance.getDeviceTypeId();
                ViewerDeviceNode deviceNode = deviceNodeMap.get(deviceId);
                
                if (deviceNode != null)
                {
                	deviceNode.addChild(new ViewerInstanceNode(deviceNode, instance));
                }
                else
                {
                    BasePlugin.logError("deviceNode should not be null");
                }
            }
            
            for (ViewerDeviceNode deviceNode : deviceNodeMap.values())
            {
                if (!deviceNode.hasChildren())
                {
                    deviceNode.addChild(new ViewerInstanceNode(deviceNode, null));
                }
            }
            
            elements = deviceNodeMap.values().toArray();
        }
        else
        {
            elements = getChildren(inputElement);
        }
        
        return elements;
    }

    public void dispose()
    {
        //do nothing
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
    {
        //do nothing
    }

}

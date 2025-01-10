/********************************************************************************
 * Copyright (c) 2007-2010 Motorola Mobility, Inc.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Fabio Fantato (Motorola)
 * 
 * Contributors:
 * Fabio Rigo (Eldorado) - [245111] Disable the "Delete" option in popup if the instance is not prepared for deletion
 * Daniel Barboza Franco (Eldorado Research Institute) - Bug [246082] - Complement bug #245111 by allowing disable of "Properties" option as well
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 * Julia Martinez Perdigueiro (Eldorado) - [329548] Adding default service attribute for double click behavior on dev mgt view
 ********************************************************************************/

package org.eclipse.sequoyah.device.framework.status;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.sequoyah.device.framework.DevicePlugin;
import org.eclipse.sequoyah.device.framework.DeviceResources;

public class MobileStatus implements IStatus {
    private String id;
	private String name;
	private ImageDescriptor image;
	private Object parent;
	private boolean canDeleteInstance;
	private boolean canEditProperties;
	private String defaultServiceId;
	
	public MobileStatus(){
		this.id = DevicePlugin.SEQUOYAH_STATUS_UNAVAILABLE;
		this.name = DeviceResources.SEQUOYAH_STATUS_UNAVAILABLE;
	};
	
	public MobileStatus(String id,String name){
		this.id = id;
		this.name = name;
	};
	

	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}

	public ImageDescriptor getImage() {
		return image;
	}
	
	public void setImage(ImageDescriptor image) {
		this.image = image;
	}

	public String getName() {
		return name;
	}
	
	public void setName(String name){
		this.name = name;
	}
	

	public Object getParent() {
		return parent;
	}

	public void setParent(Object instance) {
		this.parent = instance;
	}
	
	public boolean canDeleteInstance()
	{
	    return canDeleteInstance;
	}

	public void setCanDeleteInstance(boolean canDeleteInstance)
	{
	    this.canDeleteInstance = canDeleteInstance;
	}
	
	public boolean canEditProperties()
	{
	    return canEditProperties;
	}

	public void setCanEditProperties(boolean canEditProperties)
	{
	    this.canEditProperties = canEditProperties;
	}
	
    public String getDefaultServiceId() 
    { 
    	return defaultServiceId; 
    }
    
    public void setDefaultServiceId(String defaultServiceId)
    {
    	this.defaultServiceId = defaultServiceId;
    }
	
	public Object clone(){
		MobileStatus clone = new MobileStatus(this.id,this.name);
		clone.setParent(this.parent);
		clone.setImage(this.image);
		return clone;
	}

	public String toString(){
		return "[Status: id="+(id!=null?id:"")+";name="+(name!=null?name:"")+"]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
	}	

}

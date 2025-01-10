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
 * Daniel Barboza Franco - Bug [239970] - Invisible Services
 * Yu-Fen Kuo (MontaVista) - Bug [236476 ]- provide a generic device type
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah 
 * Pablo Leite (Eldorado) - [329548] Allow multiple instances selection on Device Manager View 
 ********************************************************************************/

package org.eclipse.sequoyah.device.framework.model;

import java.util.Collection;
import java.util.List;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.sequoyah.device.framework.internal.model.DeviceServicesTransitions;
import org.eclipse.sequoyah.device.framework.model.handler.IServiceHandler;
import org.eclipse.sequoyah.device.framework.status.IStatusTransition;


public interface IService {
	String getId();

	void setId(String id);

    ImageDescriptor getImage();
	
	void setImage(ImageDescriptor image);
	
	String getName();

	void setName(String name);

	String getDescription();

	void setDescription(String description);

	String getProvider();

	void setProvider(String provider);

	String getVersion();

	void setVersion(String version);

	String getCopyright();

	void setCopyright(String copyright);

	IServiceHandler getHandler();

	void setHandler(IServiceHandler handler);
	
	Object clone();
	
	Collection<IStatusTransition> getStatusTransitions(String deviceTypeId);
	
	IStatusTransition getStatusTransitions(String deviceTypeId, String startId);
	
	void setVisible(boolean visible);
	
	boolean isVisible();

    void setDevicesTransitions(List<DeviceServicesTransitions> devicesTransitions);

    List<DeviceServicesTransitions> getDevicesTransitions();
}

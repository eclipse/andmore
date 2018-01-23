/********************************************************************************
 * Copyright (c) 2007-2010 Motorola Mobility, Inc. and others.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Fabio Fantato (Motorola)
 * 
 * Contributors:
 * Otavio Luiz Ferranti (Eldorado Research Institute) - bug#221733 - Adding data persistence
 * Yu-Fen Kuo (MontaVista) - bug#236476 - provide a generic device type
 * Daniel Barboza Franco (Eldorado Research Institute) - Bug [221739] - Improvements to State machine implementation
 * Mauren Brenner (Eldorado) - Bug [274503] - Added methods to get and set the name suffix
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 * Flavio Vantin (Eldorado) - Bug [315158] Added methods to get and set the separator between name and suffix.
 * Pablo Leite (Eldorado) - [329548] Allow multiple instances selection on Device Manager View 
 ********************************************************************************/

package org.eclipse.sequoyah.device.framework.model;

import java.util.Properties;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.sequoyah.device.framework.statemachine.StateMachineHandler;

/**
 * Interface which defines the requirements of the device instance classes 
 * @author Fabio Fantato
 */
public interface IInstance extends IAdaptable {
	public int getPID();
	public void setPID(int pid);
	public String getId();
	public void setId(String id);
	public String getName();
	public void setName(String name);
	public String getDeviceTypeId();
	public void setDeviceTypeId(String deviceTypeId);
	public String getStatus();
	public void setStatus(String status);
	public Properties getProperties();
	public void setProperties(Properties properties);
	public String getNameSuffix();
	public void setNameSuffix(String suffix);
	public String getSeparator();
	public void setSeparator(String separator);
	public void setStateMachineHandler(StateMachineHandler stateMachineHandler);
	public StateMachineHandler getStateMachineHandler();
}

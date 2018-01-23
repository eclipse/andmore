/********************************************************************************
 * Copyright (c) 2007-2010 Motorola Inc and others.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Fabio Fantato (Motorola)
 * 
 * Contributors:
 * Otavio Luiz Ferranti (Eldorado Research Institute) - bug#221733 - Adding data persistence
 * Daniel Barboza Franco (Eldorado Research Institute) - Bug [221739] - Improvements to State machine implementation
 * Daniel Barboza Franco (Eldorado Research Institute) - Bug [252261] - Internal class MobileInstance providing functionalities
 * Daniel Barboza Franco (Eldorado Research Institute) - Bug [271682] - Default Wizard Page accepting invalid names
 * Mauren Brenner (Eldorado) - Bug [274503] - Added name suffix along with getter and setter methods
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 * Flavio Vantin (Eldorado) - Bug [315851] Implemented methods to get and set the separator between name and suffix.
 * Marcelo Bossoni (Eldorado) - Bug [318990] Fix NPE on getStatus method
 ********************************************************************************/

package org.eclipse.sequoyah.device.framework.model;

import java.util.Properties;
import java.util.regex.Pattern;

import org.eclipse.sequoyah.device.framework.statemachine.StateMachineHandler;

public abstract class AbstractMobileInstance implements IInstance {

	private static final String VALID_INSTANCE_REGEXP = "([a-z]|[A-Z]|[0-9]|\\.|_|-)+";//$NON-NLS-1$
	protected String id;
	private String name;
	private String separator = "\\";
	private String nameSuffix;
	private String deviceId;
	private Properties properties;
	protected int pid;
	private StateMachineHandler stateMachineHandler;

	public AbstractMobileInstance() {
		
	}

	/**
	 * Retrieves the instance id.
	 * @return The instance id.
	 */
	public String getId() {
		return id;
	}

	/**
	 * Sets the instance id
	 * @param id - The instance id.
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * Retrieves the instance PID.
	 * @return The instance PID.
	 */
	public int getPID() {
		return pid;
	}

	/**
	 * Sets the instance PID.
	 * @return pid - The instance PID.
	 */
	public void setPID(int pid) {
		this.pid = pid;
	}

	/**
	 * Retrieves the instance name.
	 * @return The instance name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the instance name.
	 * @param name - The instance name.
	 */
	public void setName(String name) {		
		this.name = name;
	}
	
	/**
	 * Retrieves the name suffix.
	 * @return The name suffix.
	 */
	public String getNameSuffix() {
		return nameSuffix;
	}
	
	/**
	 * Sets the name suffix.
	 * @param suffix - The name suffix.
	 */
	public void setNameSuffix(String suffix) {
		nameSuffix = suffix;
	}
	
	/**
	 * Retrieves the text that separates the name and suffix.
	 */
    public String getSeparator()
    {
        return separator;
    }
    
    /**
     * Sets the text that separates the name and suffix.
     * @param suffix - The name suffix.
     */
    public void setSeparator(String separator)
    {
        this.separator = separator;
    }

	/**
	 * Retrieves the device id.
	 * @return The device id.
	 */
	public String getDeviceTypeId() {
		return deviceId;
	}

	/**
	 * Sets the device id.
	 * @param deviceId - The device id.
	 */
	public void setDeviceTypeId(String deviceTypeId) {
		this.deviceId = deviceTypeId;
		
	}
	

	/**
	 * Retrieves the instance status.
	 * @return The instance status.
	 */
	public String getStatus() {
		return getStateMachineHandler() != null ? getStateMachineHandler().getState() : "unknown";
	}

	/**
	 * Sets the instance status. 
	 * Warning: This method do not check if the transition is valid.
	 * It should be used only in cases that it is not possible to cover 
	 * the transition using the services mechanism. 
	 * @param status - The instance status.
	 */
	public void setStatus(String status) {
		stateMachineHandler.setState(status);
	}

	/**
	 * Retrieves the instance properties.
	 */
	public Properties getProperties() {
		return this.properties;
	}

	/**
	 * Sets the instance properties.
	 * @param properties - The instance properties.
	 */
	public void setProperties(Properties properties) {
		this.properties = properties;
	}

	public String toString() {
		String status = getStatus();
		return "[Device Instance: " + //$NON-NLS-1$
				"id=" + (id==null?"":id) + //$NON-NLS-1$ //$NON-NLS-2$
				",name=" + (name==null?"":name) + //$NON-NLS-1$ //$NON-NLS-2$
				",device=" + (deviceId==null?"":deviceId) + //$NON-NLS-1$ //$NON-NLS-2$
				",status=" + (status==null?"":status) + //$NON-NLS-1$ //$NON-NLS-2$
				"]"; //$NON-NLS-1$
	}

	/** Returns a equivalent object of a different class
	 * @param adapter - The requested object class
	 * @return The requested object if conversion is available
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	@SuppressWarnings("rawtypes")
	public Object getAdapter(Class adapter) {
		if (adapter.equals(this.getClass())) {
			return this;
		}
		return null;
	}

	public StateMachineHandler getStateMachineHandler() {
		return this.stateMachineHandler;
	}

	public void setStateMachineHandler(StateMachineHandler stateMachineHandler) {
		this.stateMachineHandler = stateMachineHandler;
	}

	/**
	 * Check if the argument is a valid name for an instance.
	 * @param name - the name to be validated.
	 * @return true if the name is valid.
	 */
	public static boolean validName(String name) {
		return Pattern.matches(VALID_INSTANCE_REGEXP, name);
	}

}

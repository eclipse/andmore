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
 * Otavio Luiz Ferranti (Eldorado Research Institute) - bug#221733 - Code cleanup
 * Fabio Rigo (Eldorado Research Institute) - bug 244052 - The dirtyChanged method is being called out of UI thread
 * Daniel Barboza Franco (Eldorado Research Institute) - Bug [271180] - Instance persistence mechanism can cause instance duplication.
 * Fabio Rigo (Eldorado) - Bug [288006] - Unify features of InstanceManager and InstanceRegistry
 * Daniel Barboza Franco - Bug [287875] - Save instances information on all updates
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 * Bruno Tomazela (Eldorado) - Bug [371126] - Device Management View shows incorrect devices (same device multiple times)
 ********************************************************************************/
package org.eclipse.sequoyah.device.framework.factory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.sequoyah.device.framework.DevicePlugin;
import org.eclipse.sequoyah.device.framework.events.IInstanceListener;
import org.eclipse.sequoyah.device.framework.events.InstanceAdapter;
import org.eclipse.sequoyah.device.framework.events.InstanceEvent;
import org.eclipse.sequoyah.device.framework.events.InstanceEvent.InstanceEventType;
import org.eclipse.sequoyah.device.framework.events.InstanceEventManager;
import org.eclipse.sequoyah.device.framework.manager.persistence.DeviceXmlReader;
import org.eclipse.sequoyah.device.framework.manager.persistence.DeviceXmlWriter;
import org.eclipse.sequoyah.device.framework.model.IInstance;
import org.eclipse.sequoyah.device.framework.model.IInstanceRegistry;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * Stores the device instances and provides basic query methods.
 * @author Fabio Fantato
 */
public class InstanceRegistry implements IInstanceRegistry {
	
    /*
     * The list of available device instances.
     * Use a Map instead of a List to ensure that duplicates are not allowed.
     * Use the id of the instance as the key, and the instance itself as the value of the mapping.
     */
	private Map<String, IInstance> instances; 
	private static InstanceRegistry _instance;

	/**
	* Constructor - Stores the device instances and provides basic query methods.
	*/
	private InstanceRegistry(){
		instances = new HashMap<String, IInstance>();
		
		reload();
		
		IWorkbench workbench = DevicePlugin.getDefault().getWorkbench();
	    workbench.addWindowListener(new WindowListener());
	    
	    IInstanceListener listener = new InstanceAdapter()	{
		    public void instanceUpdated(InstanceEvent e) {
				DeviceXmlWriter.saveInstances();
		    }
		};
	    InstanceEventManager.getInstance().addInstanceListener(listener);
	}
	
	public void reload() {
		clear();
		File path = DevicePlugin.getDeviceXmlLocation();
	    File file = new File(path, DevicePlugin.getDeviceXmlFileName());
		Collection<IInstance> loadedInstances = DeviceXmlReader.loadInstances(file);
		for (IInstance inst : loadedInstances)
		{
			addInstance(inst);
		}
	}

	private class WindowListener implements IWindowListener {

        public void windowClosed(IWorkbenchWindow window) {
            DeviceXmlWriter.saveInstances();
        }

        public void windowOpened(IWorkbenchWindow window) {
        	//do nothing
        }

        public void windowDeactivated(IWorkbenchWindow window) {
        	//do nothing
        }

        public void windowActivated(IWorkbenchWindow window) {
        	//do nothing
        }
    }
	
	/**
	 * Singleton method.
	 * @return An InstanceRegistry instance.
	 */
	public static InstanceRegistry getInstance(){
		if (_instance==null) {
			_instance = new InstanceRegistry();
		}
		return _instance;
	}

	public ImageDescriptor getImage(){
		return DevicePlugin.getDefault().getImageDescriptor(DevicePlugin.ICON_DEVICE);
	}

	/**
	 * Retrieves the list of registered instances.
	 * @return A list of instances.
	 */
	public List<IInstance> getInstances() {
		return new ArrayList<IInstance>(instances.values()); //returns a List of available instances
	}

	/* (non-Javadoc)
	 * @see org.eclipse.sequoyah.device.framework.model.IInstanceRegistry#setInstances(java.util.List)
	 */
	public void setInstances(List<IInstance> instances) {
	    this.instances.clear();
		for (IInstance instance: instances){
		    addInstance(instance);
		}
	}

	/**
	 * Add an instance to the registry.
	 * @param instance - The instance to be added.
	 */
	public void addInstance(IInstance instance){
		this.instances.put(instance.getId(), instance); //adds an instance to the mapping, replacing the value if it already exists on the mapping.
		InstanceEventManager.getInstance().notifyListeners(new InstanceEvent(InstanceEventType.INSTANCE_LOADED, instance));
	}

	/**
	 * Removes an instance from the registry.
	 * @param instance - The instance to be removed.
	 */
	public void removeInstance(IInstance instance){
		this.instances.remove(instance.getId()); //remove the mapping.
		InstanceEventManager.getInstance().notifyListeners(new InstanceEvent(InstanceEventType.INSTANCE_UNLOADED, instance));
	}

	/**
	 * Removes all instances from the registry.
	 */
	public void clear(){
		this.instances.clear();
	}
	
	/**
	 * Retrieves all instances with a specified matching name
	 * 
	 * @param name -
	 *            The instance name to be queried
	 * @return A list of IInstance objects of name matching instances
	 */
	public List<IInstance> getInstancesByName(String name) {
		InstanceRegistry registry = InstanceRegistry.getInstance();

		List<IInstance> instanceList = registry.getInstances();
		List<IInstance> returnValue = new ArrayList<IInstance>();

		Iterator<IInstance> it = instanceList.iterator();
		while (it.hasNext()) {
			IInstance inst = it.next();
			if (inst.getName().equals(name)) {
				returnValue.add(inst);
			}
		}
		return returnValue;
	}
}

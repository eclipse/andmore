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
package org.eclipse.andmore.android.devices;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.TreeSet;

import org.eclipse.andmore.android.DDMSFacade;
import org.eclipse.andmore.android.ISerialNumbered;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.sequoyah.device.common.utilities.exception.SequoyahException;
import org.eclipse.sequoyah.device.framework.DeviceUtils;
import org.eclipse.sequoyah.device.framework.factory.DeviceTypeRegistry;
import org.eclipse.sequoyah.device.framework.factory.InstanceRegistry;
import org.eclipse.sequoyah.device.framework.manager.InstanceManager;
import org.eclipse.sequoyah.device.framework.model.IDeviceType;
import org.eclipse.sequoyah.device.framework.model.IInstance;
import org.eclipse.sequoyah.device.framework.model.IInstanceBuilder;
import org.eclipse.sequoyah.device.framework.model.IService;
import org.eclipse.sequoyah.device.framework.model.handler.IServiceHandler;

/**
 * This manager provides information about device instances It shall work with
 * TmL to be able to provide these information
 */
public class DevicesManager {

	// instance
	private static DevicesManager instance;

	/**
	 * Singleton
	 * 
	 * @return a unique DevicesManager instance
	 */
	public static DevicesManager getInstance() {
		if (instance == null) {
			instance = new DevicesManager();
		}
		return instance;
	}

	/**
	 * Get all devices registered in TmL
	 * 
	 * @return all devices registered in TmL
	 */
	public Collection<ISerialNumbered> getAllDevices() {
		Collection<ISerialNumbered> devicesCollection = new LinkedHashSet<ISerialNumbered>();

		List<IInstance> tmlDevices = InstanceRegistry.getInstance().getInstances();

		for (IInstance tmlInstance : tmlDevices) {
			if (tmlInstance instanceof ISerialNumbered) {
				devicesCollection.add((ISerialNumbered) tmlInstance);
			}
		}
		return devicesCollection;
	}

	/**
	 * Get all devices registered in TmL, sorted using the comparator passed as
	 * a parameter
	 * 
	 * @param comparator
	 *            the comparator that will be used to sort the devices list
	 * @return all devices registered in TmL, sorted using the comparator passed
	 *         as a parameter
	 */
	public Collection<ISerialNumbered> getAllDevices(Comparator<ISerialNumbered> comparator) {
		Collection<ISerialNumbered> sortedDevices = new TreeSet<ISerialNumbered>(comparator);
		sortedDevices.addAll(getAllDevices());
		return sortedDevices;
	}

	/**
	 * Get all devices registered in TmL, sorted using the default comparator
	 * 
	 * @return all devices registered in TmL, sorted using the default
	 *         comparator
	 */
	public Collection<ISerialNumbered> getAllDevicesSorted() {
		return getAllDevices(getDefaultComparator());
	}

	/**
	 * Get the device, given its name
	 * 
	 * @param name
	 *            the device name
	 * @return a device instance
	 */
	public ISerialNumbered getDeviceByName(String name) {
		ISerialNumbered instanceToReturn = null;

		for (ISerialNumbered device : getAllDevices()) {
			if (device.getDeviceName().equals(name)) {
				instanceToReturn = device;
				break;
			}
		}
		return instanceToReturn;

	}

	/**
	 * Get the device, given its serial number
	 * 
	 * @param serial
	 *            the device serial name
	 * @return a device instance
	 */
	public ISerialNumbered getDeviceBySerialNumber(String serial) {
		ISerialNumbered instanceToReturn = null;

		String serialNumber = "";
		for (ISerialNumbered device : getAllDevices()) {
			if ((serialNumber = device.getSerialNumber()) != null) {
				if (serialNumber.equals(serial)) {
					instanceToReturn = device;
					break;
				}
			}
		}
		return instanceToReturn;

	}

	/**
	 * Get all devices of certain type
	 * 
	 * @param type
	 *            the device type
	 * @return all devices of the device passed as a parameter
	 */
	@SuppressWarnings("unchecked")
	public Collection<ISerialNumbered> getInstancesByType(Class type) {
		Collection<ISerialNumbered> instancesToReturn = new LinkedHashSet<ISerialNumbered>();

		for (ISerialNumbered device : getAllDevices()) {
			if (device.getClass().equals(type)) {
				instancesToReturn.add(device);
			}
		}
		return instancesToReturn;

	}

	/**
	 * Get all online devices of certain type
	 * 
	 * @param type
	 *            the device type
	 * @return all online devices of the device passed as a parameter
	 */
	@SuppressWarnings("unchecked")
	public Collection<ISerialNumbered> getOnlineDevicesByType(Class type) {
		Collection<ISerialNumbered> instancesToReturn = new HashSet<ISerialNumbered>();

		String serialNumber = null;
		for (ISerialNumbered device : getInstancesByType(type)) {
			if (((serialNumber = device.getSerialNumber()) != null) && (DDMSFacade.isDeviceOnline(serialNumber))) {
				instancesToReturn.add(device);
			}
		}
		return instancesToReturn;

	}

	/**
	 * The default comparator to be used to sort ISerialNumbered instances It
	 * considers if the devices are online and after that it compares their
	 * names Online devices shall be placed in the beginning of the list
	 * 
	 * @return a Comparator instance
	 */
	public static Comparator<ISerialNumbered> getDefaultComparator() {
		return new Comparator<ISerialNumbered>() {
			@Override
			public int compare(ISerialNumbered serial1, ISerialNumbered serial2) {
				int compareResult;

				String name1 = serial1.getDeviceName();
				String name2 = serial2.getDeviceName();
				boolean dev1online = serial1.getSerialNumber() != null;
				boolean dev2online = serial2.getSerialNumber() != null;

				if ((dev1online && dev2online) || (!dev1online && !dev2online)) {
					compareResult = name1.compareToIgnoreCase(name2);
				} else if (dev1online) {
					compareResult = -1;
				} else
				// dev2online
				{
					compareResult = 1;
				}

				return compareResult;
			}
		};
	}

	/**
	 * Creates a TmL instance
	 * 
	 * @param serialNumber
	 *            The serial number of the device to create a TmL instance for
	 * @throws TmLException
	 */
	public void createInstanceForDevice(String serialNumber, String deviceTypeID, IInstanceBuilder instanceBuilder,
			String initServiceID) throws SequoyahException {

		ISerialNumbered instance = getDeviceBySerialNumber(serialNumber);
		if (instance == null) {
			IDeviceType tmlDeviceType = DeviceTypeRegistry.getInstance().getDeviceTypeById(deviceTypeID);

			InstanceManager.createProject(tmlDeviceType, instanceBuilder, new NullProgressMonitor());

			IInstance newInstance = (IInstance) getDeviceBySerialNumber(serialNumber);
			IService service = DeviceUtils.getServiceById(tmlDeviceType, initServiceID);
			IServiceHandler handler = service.getHandler();
			handler.run(newInstance);
		}

	}

	/**
	 * Destroys the TmL instance
	 * 
	 * @param device
	 *            The device to delete the correspondent TmL instance
	 */
	public void deleteInstanceOfDevice(String serialNumber) {

		IInstance instanceToDelete = (IInstance) getDeviceBySerialNumber(serialNumber);
		if (instanceToDelete != null) {
			InstanceManager.deleteInstance(instanceToDelete);
		}

	}

}

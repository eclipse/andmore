/********************************************************************************
 * Copyright (c) 2010 Motorola Inc.
 * All rights reserved. This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Daniel Barboza Franco (Eldorado)
 * 
 * Contributors:
 * Contributor (Company) - Bug [NUMBER] - Bug Description
 * 
 * For more information and instructions of how to run this test, please refer to http://wiki.eclipse.org/Sequoyah/unit_test
 ********************************************************************************/

package org.eclipse.sequoyah.device.framework.junit;

import static org.junit.Assert.*;

import java.util.Properties;

import org.eclipse.sequoyah.device.common.utilities.exception.SequoyahException;
import org.eclipse.sequoyah.device.framework.manager.InstanceManager;
import org.eclipse.sequoyah.device.framework.model.IInstance;
import org.junit.Test;

public class TestDevice {

	@Test
	public void testCreateInstance() {
		
		String devName = "JUnit Device";
		String devId = "org.eclipse.sequoyah.device.qemuarm.qemuarmDevice";
		String status = "OFF";
		Properties props = new Properties();
		
		IInstance instance = null;
		
		try {
			instance = InstanceManager.createInstance(devName, devId, status, props);
		} catch (SequoyahException e) {
			fail(e.getMessage());
			e.printStackTrace();
		}
		
		assertNotNull(instance);
		assertEquals(devName, instance.getName());
		assertEquals(devId, instance.getDeviceTypeId());
		assertEquals(status, instance.getStatus());
		
	}

}

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
package org.eclipse.andmore.android.emulator.device.init;

import static org.eclipse.andmore.android.common.log.StudioLogger.info;

import java.util.List;

import org.eclipse.andmore.android.emulator.EmulatorPlugin;
import org.eclipse.andmore.android.emulator.logic.IAndroidLogicInstance;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.sequoyah.device.framework.events.InstanceEvent;
import org.eclipse.sequoyah.device.framework.events.InstanceEvent.InstanceEventType;
import org.eclipse.sequoyah.device.framework.events.InstanceEventManager;
import org.eclipse.sequoyah.device.framework.model.IInstance;
import org.eclipse.sequoyah.device.framework.model.IService;
import org.eclipse.sequoyah.device.framework.model.handler.IServiceHandler;

/**
 * DESCRIPTION: This class plugs the init procedure to a TmL service. This
 * service implements the interface directly, because it causes the instance to
 * have a particular behavior at the state machine.
 *
 * RESPONSIBILITY: Provide the initialization procedure to apply to every
 * instance that is loaded at TmL device framework
 *
 * COLABORATORS: None.
 *
 * USAGE: This class is intended to be used by Eclipse only
 */
public class InitServiceHandler implements IServiceHandler {
	/**
	 * The parent service handler
	 */
	private IServiceHandler parent;

	/**
	 * The service that launches the handler
	 */
	private IService service;

	/**
	 * @see IServiceHandler#run(IInstance)
	 */
	@Override
	public void run(IInstance instance) {
		if (instance instanceof IAndroidLogicInstance) {
			// The service definition defined (by convention) that
			// stopped-dirty is the success state, and not available
			// is the failure state. The exception is being thrown for
			// the framework to set the state correctly.
			instance.setStatus(EmulatorPlugin.STATUS_NOT_AVAILABLE);
			InstanceEventManager.getInstance().notifyListeners(
					new InstanceEvent(InstanceEventType.INSTANCE_TRANSITIONED, instance));
		}

	}

	/**
	 * @see IServiceHandler#newInstance()
	 */
	@Override
	public IServiceHandler newInstance() {
		return new InitServiceHandler();
	}

	/**
	 * @see IServiceHandler#setParent(IServiceHandler)
	 */
	@Override
	public void setParent(IServiceHandler handler) {
		this.parent = handler;
	}

	/**
	 * @see IServiceHandler#setService(IService)
	 */
	@Override
	public void setService(IService service) {
		this.service = service;
	}

	/**
	 * @see IServiceHandler#updatingService(IInstance)
	 */
	@Override
	public void updatingService(IInstance instance) {
		info("Updating init emulator service");
	}

	/**
	 * @see IServiceHandler#clone()
	 * @see Cloneable#clone()
	 */
	@Override
	public Object clone() {
		IServiceHandler newHandler = newInstance();
		newHandler.setParent(parent);
		newHandler.setService(service);
		return newHandler;
	}

	/**
	 * @see IServiceHandler#getParent()
	 */
	@Override
	public IServiceHandler getParent() {
		return parent;
	}

	/**
	 * @see IServiceHandler#getService()
	 */
	@Override
	public IService getService() {
		return service;
	}

	@Override
	public IStatus singleInit(List<IInstance> instances) {
		return Status.OK_STATUS;
	}
}

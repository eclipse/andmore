/*******************************************************************************
 * Copyright (c) 2008-2010 MontaVista Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Yu-Fen Kuo (MontaVista) - initial API and implementation
 *     Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 *******************************************************************************/
package org.eclipse.sequoyah.device.framework.model;

import java.util.Collection;

import org.eclipse.jface.resource.ImageDescriptor;

public interface IDeviceTypeRegistry {
	public Collection<IDeviceType> getDeviceTypes();
	public IDeviceType getDeviceTypeById(String id);
	public ImageDescriptor getImage();
}

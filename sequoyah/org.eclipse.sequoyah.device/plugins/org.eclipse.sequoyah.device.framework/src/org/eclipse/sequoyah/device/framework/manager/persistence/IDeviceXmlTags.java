/********************************************************************************
 * Copyright (c) 2008-2010 Motorola Inc and others. All rights reserved
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Fabio Rigo (Eldorado Research Institute) 
 * [245114] Enhance persistence policies
 * Yu-Fen Kuo (MontaVista)  - [236476] - provide a generic device type
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/

package org.eclipse.sequoyah.device.framework.manager.persistence;

interface IDeviceXmlTags
{
    String SEQUOYAH_XML_DEVICES = "devices"; //$NON-NLS-1$
    String SEQUOYAH_XML_DEVICE = "deviceType"; //$NON-NLS-1$
    String SEQUOYAH_XML_DEVICE_ID = "id"; //$NON-NLS-1$
    String SEQUOYAH_XML_DEVICE_PLUGIN = "plugin"; //$NON-NLS-1$
    String SEQUOYAH_XML_INSTANCES = "instances"; //$NON-NLS-1$
    String SEQUOYAH_XML_INSTANCE = "instance"; //$NON-NLS-1$
    String SEQUOYAH_XML_INSTANCE_NAME = "name"; //$NON-NLS-1$
    String SEQUOYAH_XML_INSTANCE_DEVICE_ID = "deviceType_id"; //$NON-NLS-1$
    String SEQUOYAH_XML_ROOT = "sequoyah"; //$NON-NLS-1$

}

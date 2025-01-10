/********************************************************************************
 * Copyright (c) 2008-2010 Motorola Inc and others. All rights reserved
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Fabio Rigo (Eldorado) - [245114] Enhance persistence policies
 * 
 * Other Contributors:
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/

package org.eclipse.sequoyah.device.framework.manager.persistence;

public class SequoyahDevice
{
    private String id;
    private String plugin;

    public SequoyahDevice(String id, String plugin) {
        this.id = id;
        this.plugin = plugin;
    }

    public String getId() {
        return id;
    }

    public String getPlugin() {
        return plugin;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setPlugin(String plugin) {
        this.plugin = plugin;
    }
}

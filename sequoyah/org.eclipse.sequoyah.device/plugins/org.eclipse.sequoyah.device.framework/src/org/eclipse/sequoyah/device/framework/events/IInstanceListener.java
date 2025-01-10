/********************************************************************************
 * Copyright (c) 2008-2010 Motorola Inc. and others.  All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Initial Contributor:
 * Fabio Rigo (Eldorado) - [244951] Implement listener/event mechanism at device framework
 *
 * Contributors:
 * Fabio Rigo (Eldorado) - [287995] Provide an instance is about to transition event
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/

package org.eclipse.sequoyah.device.framework.events;

public interface IInstanceListener
{
    void instanceCreated(InstanceEvent e);
    void instanceDeleted(InstanceEvent e);
    void instanceUpdated(InstanceEvent e);
    void instanceLoaded(InstanceEvent e);
    void instanceUnloaded(InstanceEvent e);
    void instanceTransitioned(InstanceEvent e);
    void instanceAboutToTransition(InstanceEvent e);
}

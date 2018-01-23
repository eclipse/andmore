/********************************************************************************
 * Copyright (c) 2007-2010 Motorola Inc.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Daniel Franco (Motorola)
 * 
 * Contributors:
 * Daniel Barboza Franco (Motorola) - Bug [233775] - Does not have a way to enter the session password for the vnc connection
 * Fabio Rigo (Eldorado) - Bug [244066] - The services are being run at one of the UI threads
 * Fabio Rigo (Eldorado Research Institute) - [246212] - Enhance encapsulation of protocol implementer 
 * Daniel Barboza Franco (Eldorado Research Institute) - Bug [233121] - There is no support for proxies when connecting the protocol
 * Daniel Barboza Franco (Eldorado Research Institute) - Bug [246585] - VncViewerService is not working anymore after changes made in ProtocolHandle
 * Yu-Fen Kuo (MontaVista)  - [236476] - provide a generic device type
 * Daniel Barboza Franco (Eldorado Research Institute) - [221740] - Sample implementation for Linux host
 * Fabio Rigo (Eldorado Research Institute) - Bug [262632] - Avoid providing raw streams to the user in the protocol framework
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/

package org.eclipse.sequoyah.device.service.vncviewer.handler;

import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.sequoyah.device.common.utilities.IPropertyConstants;
import org.eclipse.sequoyah.device.framework.model.IInstance;
import org.eclipse.sequoyah.device.framework.model.handler.IServiceHandler;
import org.eclipse.sequoyah.device.framework.model.handler.ServiceHandler;
import org.eclipse.sequoyah.vnc.protocol.PluginProtocolActionDelegate;
import org.eclipse.sequoyah.vnc.protocol.lib.ProtocolHandle;
import org.eclipse.sequoyah.vnc.protocol.lib.ProtocolMessage;

import org.eclipse.sequoyah.vnc.vncviewer.vncviews.views.VNCViewerView;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

public class VNCViewerServiceHandler extends ServiceHandler
{
    @Override
    public IStatus runService(IInstance instance, Map<Object, Object> arguments,
            IProgressMonitor monitor)
    {

        final String host = instance.getProperties().getProperty(IPropertyConstants.HOST);
        final int port = Integer.parseInt(instance.getProperties().getProperty(IPropertyConstants.PORT));
        
        final String protoVersion;
        if (Platform.getOS().equals(Platform.OS_WIN32)) {
        	protoVersion = "VNC 3.3"; //$NON-NLS-1$	
        }
        else protoVersion = "VNC 3.8"; //$NON-NLS-1$
        
        final String password = instance.getProperties().getProperty(IPropertyConstants.PASSWORD);

        try
        {

            if (instance.getStatus().equals("IDLE-VNC")) //$NON-NLS-1$
            {
                VNCViewerView.stop();
                ProtocolHandle handle = VNCViewerView.protocolHandle;
                PluginProtocolActionDelegate.requestStopProtocol(handle);
            }

            PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable()
            {

                public void run()
                {
                    try
                    {
                        PlatformUI.getWorkbench().getWorkbenchWindows()[0].getActivePage()
                                .showView("org.eclipse.sequoyah.vnc.vncviewer.vncviews.views.VNCViewerView"); //$NON-NLS-1$
                    }
                    catch (PartInitException e)
                    {
                        e.printStackTrace();
                    }
                }

            });

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        if (VNCViewerView.protocolHandle != null)
        {
            VNCViewerView.stop();
            PluginProtocolActionDelegate.requestStopProtocol(VNCViewerView.protocolHandle);
        }

        PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
            public void run() {
                  VNCViewerView.start(host, port, protoVersion, password, false);
            }
        });  


        /*
         *  The code below is a fix for QEMU-ARM which expect 
         *  the sequence ctrl+alt+3 to show it's display. 
         *****/
        if (instance.getDeviceTypeId().equals("org.eclipse.sequoyah.device.qemuarm.qemuarmDevice")) //$NON-NLS-1$
        {

            try
            {

                ProtocolHandle handle = VNCViewerView.protocolHandle;
                ProtocolMessage qemumsg = new ProtocolMessage(0x04);
                qemumsg.setFieldValue("downFlag", 1); //$NON-NLS-1$
                qemumsg.setFieldValue("padding", 0); //$NON-NLS-1$

                qemumsg.setFieldValue("key", 0xFFE3); //$NON-NLS-1$
                PluginProtocolActionDelegate.sendMessageToServer(handle, qemumsg);
                qemumsg.setFieldValue("key", 0xFFE9); //$NON-NLS-1$
                PluginProtocolActionDelegate.sendMessageToServer(handle, qemumsg);
                qemumsg.setFieldValue("key", 0x033); //$NON-NLS-1$
                PluginProtocolActionDelegate.sendMessageToServer(handle, qemumsg);

                qemumsg.setFieldValue("downFlag", 0); //$NON-NLS-1$
                qemumsg.setFieldValue("key", 0xFFE3); //$NON-NLS-1$
                PluginProtocolActionDelegate.sendMessageToServer(handle, qemumsg);
                qemumsg.setFieldValue("key", 0xFFE9); //$NON-NLS-1$
                PluginProtocolActionDelegate.sendMessageToServer(handle, qemumsg);
                qemumsg.setFieldValue("key", 0x033); //$NON-NLS-1$
                PluginProtocolActionDelegate.sendMessageToServer(handle, qemumsg);

            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        return Status.OK_STATUS;
    }

    @Override
    public IStatus updatingService(IInstance instance, IProgressMonitor monitor)
    {
        return Status.OK_STATUS;
    }

    @Override
    public IServiceHandler newInstance()
    {
        return new VNCViewerServiceHandler();
    }

}

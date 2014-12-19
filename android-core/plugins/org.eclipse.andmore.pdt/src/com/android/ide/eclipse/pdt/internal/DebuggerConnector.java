/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.ide.eclipse.pdt.internal;

import com.android.ide.eclipse.ddms.IDebuggerConnector;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of the com.android.ide.ddms.debuggerConnector extension point.
 */
public class DebuggerConnector extends DevTreeProjectProvider implements IDebuggerConnector {

    private final static String ATTR_CONNECT_MAP_PORT = "port"; //$NON-NLS-1$
    private final static String ATTR_CONNECT_MAP_HOSTNAME = "hostname"; //$NON-NLS-1$

    @Override
    public boolean isWorkspaceApp(String appName) {
        return getProject() != null;
    }

    @Override
    public boolean connectDebugger(String appName, int appPort, int selectedPort) {
        IProject project = getProject();

        if (project != null) {
            // get the launch manager
            ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();

            // get the config for the remote launch config.
            ILaunchConfigurationType configType = manager.getLaunchConfigurationType(
                            IJavaLaunchConfigurationConstants.ID_REMOTE_JAVA_APPLICATION);

            String projectName = project.getName();

            // look for an existing launch config
            ILaunchConfiguration config = findConfig(manager, configType, projectName,
                    selectedPort);

            if (config == null) {
                // Didn't find a matching config, so we make one.
                // It'll be made in the "working copy" object first.
                ILaunchConfigurationWorkingCopy wc = null;

                try {
                    // make the working copy object with a unique name
                    wc = configType.newInstance(null,
                            manager.generateUniqueLaunchConfigurationNameFrom(projectName));

                    // set the project name
                    wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME,
                            projectName);

                    // set the connect map info
                    Map<String, String> connectMap = new HashMap<String, String>();
                    connectMap.put(ATTR_CONNECT_MAP_PORT, Integer.toString(selectedPort));
                    connectMap.put(ATTR_CONNECT_MAP_HOSTNAME, "localhost"); //$NON-NLS-1$
                    wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_CONNECT_MAP, connectMap);

                    // set the VM connector ID
                    wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_CONNECTOR,
                            IJavaLaunchConfigurationConstants.ID_SOCKET_ATTACH_VM_CONNECTOR);

                    // save the working copy to get the launch config object which we return.
                    config = wc.doSave();

                } catch (CoreException e) {

                }

            }

            if (config != null) {
                DebugUITools.launch(config, ILaunchManager.DEBUG_MODE);
                return true;
            }
        }

        return false;
    }

    /**
     * Looks for and returns an existing {@link ILaunchConfiguration} object for a
     * specified project and connection port.
     * @param manager The {@link ILaunchManager}.
     * @param type The {@link ILaunchConfigurationType}.
     * @param projectName The name of the project
     * @param connectionPort the remote connection port.
     * @return an existing <code>ILaunchConfiguration</code> object matching the project, or
     *      <code>null</code>.
     */
    private static ILaunchConfiguration findConfig(ILaunchManager manager,
            ILaunchConfigurationType type,
            String projectName, int connectionPort) {
        try {
            ILaunchConfiguration[] configs = manager.getLaunchConfigurations(type);

            // look for one set up for the project with a debug equal to the selected debug port.
            for (ILaunchConfiguration config : configs) {

                Map<?, ?> attributes = config.getAttributes();

                String name = (String) attributes.get(
                        IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME);

                if (name == null || name.equals(projectName) == false) {
                    continue;
                }

                Map<?, ?> connectMap = (Map<?, ?>) attributes.get(
                        IJavaLaunchConfigurationConstants.ATTR_CONNECT_MAP);

                if (connectMap != null) {
                    String portStr = (String) connectMap.get(ATTR_CONNECT_MAP_PORT);
                    if (portStr != null) {
                        Integer port = Integer.valueOf(portStr);
                        if (connectionPort == port) {
                            return config;
                        }
                    }
                }

            }
        } catch (CoreException e) {
        }

        // didn't find anything that matches. Return null
        return null;
    }
}

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

package org.eclipse.andmore.ddms;

/**
 * Classes which implement this interface provides a way to connect a debugger
 * to a VM running on a connected device.
 */
public interface IDebuggerConnector {
	/**
	 * Is this application from a project present in the workspace?
	 * 
	 * @param appName
	 *            name of the application. This is typically the application's
	 *            package, but can be different if the component was setup to
	 *            run in its own process.
	 * @return true if there is a project in the workspace containing the given
	 *         app.
	 */
	boolean isWorkspaceApp(String appName);

	/**
	 * Connects a debugger to a VM identified by its appName.
	 * <p/>
	 * The given port is tied to the application and should be used if possible.
	 * However the "selected" port can also be used if needed.
	 * 
	 * @param appName
	 *            the name of the application. Usually the application's package
	 *            but this can be different if the component was setup to run in
	 *            it's own process.
	 * @param appPort
	 *            the preferred connection port.
	 * @param selectedPort
	 *            the port value for the selected application
	 * @return true if success.
	 */
	boolean connectDebugger(String appName, int appPort, int selectedPort);

}

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

package org.eclipse.andmore.android.common.proxy;

import java.net.Authenticator;

import org.eclipse.core.internal.net.ProxyManager;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.ui.IStartup;

/**
 * Startup class that prepares the proxy settings so the user is not bothered by
 * login/password dialogs while using Studio.
 */
@SuppressWarnings("restriction")
public class NetworkProxySettingStartup implements IStartup {
	@Override
	public void earlyStartup() {
		// Try to retrieve proxy configuration to use if necessary
		IProxyService proxyService = ProxyManager.getProxyManager();
		if (proxyService.isProxiesEnabled() || proxyService.isSystemProxiesEnabled()) {
			Authenticator.setDefault(new ProxyAuthenticator());
		}
	}

}

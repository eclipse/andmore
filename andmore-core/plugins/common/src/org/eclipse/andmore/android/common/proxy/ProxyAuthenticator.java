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

import java.net.PasswordAuthentication;
import java.net.URL;

import org.eclipse.core.internal.net.ProxyManager;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.ui.internal.net.auth.NetAuthenticator;

/**
 * This class uses the Eclipse proxy settings for the JVM to be able to access
 * network without bothering the user with login/password dialogs. If
 * retrieving/setting Eclipse proxy settings to the JVM, the default behavior
 * will kick in (which is prompting the user).
 */
@SuppressWarnings("restriction")
public class ProxyAuthenticator extends NetAuthenticator {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.internal.net.auth.NetAuthenticator#getPasswordAuthentication
	 * ()
	 */
	@Override
	protected PasswordAuthentication getPasswordAuthentication() {
		// return value
		PasswordAuthentication authentication = null;

		// retrieve Eclipse proxy settings
		IProxyService proxyService = ProxyManager.getProxyManager();

		IProxyData proxyData;

		// verify if the connection is https or http
		URL url = getRequestingURL();
		String urlStr = (url != null ? url.toString() : null);
		if ((urlStr != null) && (urlStr.length() > 0) && urlStr.startsWith("https")) {
			proxyData = proxyService.getProxyData(IProxyData.HTTPS_PROXY_TYPE);
		} else {
			proxyData = proxyService.getProxyData(IProxyData.HTTP_PROXY_TYPE);
		}

		// proxy data retrieved; return values
		if (proxyData != null) {
			String userId = proxyData.getUserId();
			if ((userId != null) && (userId.trim().length() > 0)) {
				String password = proxyData.getPassword();
				authentication = new PasswordAuthentication(userId, (password == null ? "" : password).toCharArray());
			}
		}

		// if setting Eclipse proxy fails, let the superclass open the
		// user/password prompt dialog
		if (authentication == null) {
			authentication = super.getPasswordAuthentication();
		}

		return authentication;
	}
}

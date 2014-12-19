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

package org.eclipse.andmore.android.common.utilities;

import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.auth.AuthState;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.common.utilities.i18n.UtilitiesNLS;
import org.eclipse.andmore.android.common.utilities.ui.LoginPasswordDialogCreator;
import org.eclipse.core.internal.net.ProxyManager;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.ui.internal.net.auth.NetAuthenticator;

/**
 * Class for opening an input stream with the given URL.
 */
@SuppressWarnings("restriction")
public class HttpUtils {
	/**
	 * 1 second if the unit is milliseconds.
	 */
	private static final int ONE_SECOND = 1000;

	// map of credentials authentication so the user is not repeatedly asked for
	// them
	private static final Map<String, Credentials> authenticationRealmCache = new HashMap<String, Credentials>();

	private GetMethod getMethod;

	/**
	 * Retrieves an open InputStream with the contents of the file pointed by
	 * the given url.
	 * 
	 * @param url
	 *            The address from where to retrieve the InputStream
	 * @param monitor
	 *            The monitor to progress while accessing the file
	 * 
	 * @return The open InputStream object, or <code>null</code> if no file was
	 *         found
	 * 
	 * @throws IOException
	 *             if some error occurs with the network communication
	 */
	public InputStream getInputStreamForUrl(String url, IProgressMonitor monitor) throws IOException {
		return getInputStreamForUrl(url, monitor, true);
	}

	private InputStream getInputStreamForUrl(String url, IProgressMonitor monitor, boolean returnStream)
			throws IOException {
		SubMonitor subMonitor = SubMonitor.convert(monitor);

		subMonitor.beginTask(UtilitiesNLS.HttpUtils_MonitorTask_PreparingConnection, 300);

		StudioLogger.debug(HttpUtils.class, "Verifying proxy usage for opening http connection"); //$NON-NLS-1$

		// Try to retrieve proxy configuration to use if necessary
		IProxyService proxyService = ProxyManager.getProxyManager();
		IProxyData proxyData = null;
		if (proxyService.isProxiesEnabled() || proxyService.isSystemProxiesEnabled()) {
			Authenticator.setDefault(new NetAuthenticator());
			if (url.startsWith("https")) {
				proxyData = proxyService.getProxyData(IProxyData.HTTPS_PROXY_TYPE);
				StudioLogger.debug(HttpUtils.class, "Using https proxy"); //$NON-NLS-1$
			} else if (url.startsWith("http")) {
				proxyData = proxyService.getProxyData(IProxyData.HTTP_PROXY_TYPE);
				StudioLogger.debug(HttpUtils.class, "Using http proxy"); //$NON-NLS-1$
			} else {
				StudioLogger.debug(HttpUtils.class, "Not using any proxy"); //$NON-NLS-1$
			}
		}

		// Creates the http client and the method to be executed
		HttpClient client = null;
		client = new HttpClient();

		// If there is proxy data, work with it
		if (proxyData != null) {
			if (proxyData.getHost() != null) {
				// Sets proxy host and port, if any
				client.getHostConfiguration().setProxy(proxyData.getHost(), proxyData.getPort());
			}

			if ((proxyData.getUserId() != null) && (proxyData.getUserId().trim().length() > 0)) {
				// Sets proxy user and password, if any
				Credentials cred = new UsernamePasswordCredentials(proxyData.getUserId(),
						proxyData.getPassword() == null ? "" : proxyData.getPassword()); //$NON-NLS-1$
				client.getState().setProxyCredentials(AuthScope.ANY, cred);
			}
		}

		InputStream streamForUrl = null;
		getMethod = new GetMethod(url);
		getMethod.setFollowRedirects(true);

		// set a 30 seconds timeout
		HttpMethodParams params = getMethod.getParams();
		params.setSoTimeout(15 * ONE_SECOND);
		getMethod.setParams(params);

		boolean trying = true;
		Credentials credentials = null;
		subMonitor.worked(100);
		subMonitor.setTaskName(UtilitiesNLS.HttpUtils_MonitorTask_ContactingSite);
		do {
			StudioLogger.info(HttpUtils.class, "Attempting to make a connection"); //$NON-NLS-1$

			// retry to connect to the site once, also set the timeout for 5
			// seconds
			HttpClientParams clientParams = client.getParams();
			clientParams.setIntParameter(HttpClientParams.MAX_REDIRECTS, 1);
			clientParams.setSoTimeout(5 * ONE_SECOND);
			client.setParams(clientParams);

			client.executeMethod(getMethod);
			if (subMonitor.isCanceled()) {
				break;
			} else {
				AuthState authorizationState = getMethod.getHostAuthState();
				String authenticationRealm = authorizationState.getRealm();

				if (getMethod.getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
					StudioLogger.debug(HttpUtils.class, "Client requested authentication; retrieving credentials"); //$NON-NLS-1$

					credentials = authenticationRealmCache.get(authenticationRealm);

					if (credentials == null) {
						StudioLogger.debug(HttpUtils.class, "Credentials not found; prompting user for login/password"); //$NON-NLS-1$

						subMonitor.setTaskName(UtilitiesNLS.HttpUtils_MonitorTask_WaitingAuthentication);

						LoginPasswordDialogCreator dialogCreator = new LoginPasswordDialogCreator(url);
						if (dialogCreator.openLoginPasswordDialog() == LoginPasswordDialogCreator.OK) {

							credentials = new UsernamePasswordCredentials(dialogCreator.getTypedLogin(),
									dialogCreator.getTypedPassword());
						} else {
							// cancel pressed; stop trying
							trying = false;

							// set the monitor canceled to be able to stop
							// process
							subMonitor.setCanceled(true);
						}

					}

					if (credentials != null) {
						AuthScope scope = new AuthScope(null, -1, authenticationRealm);
						client.getState().setCredentials(scope, credentials);
					}

					subMonitor.worked(100);
				} else if (getMethod.getStatusCode() == HttpStatus.SC_OK) {
					StudioLogger.debug(HttpUtils.class, "Http connection suceeded"); //$NON-NLS-1$

					subMonitor.setTaskName(UtilitiesNLS.HttpUtils_MonitorTask_RetrievingSiteContent);
					if ((authenticationRealm != null) && (credentials != null)) {
						authenticationRealmCache.put(authenticationRealm, credentials);
					} else {
						// no authentication was necessary, just work the
						// monitor
						subMonitor.worked(100);
					}

					StudioLogger.info(HttpUtils.class, "Retrieving site content"); //$NON-NLS-1$

					// if the stream should not be returned (ex: only testing
					// the connection is
					// possible), then null will be returned
					if (returnStream) {
						streamForUrl = getMethod.getResponseBodyAsStream();
					}

					// succeeded; stop trying
					trying = false;

					subMonitor.worked(100);
				} else {
					// unhandled return status code
					trying = false;

					subMonitor.worked(200);
				}
			}
		} while (trying);

		subMonitor.done();

		return streamForUrl;
	}

	/**
	 * Check if a connection with the given URL can be established.
	 * 
	 * @param url
	 *            The URL to test the connection.
	 * 
	 * @return <code>true</code> if the connection can be established;
	 *         <code>false</code> otherwise
	 */
	public boolean isConnectionOk(String url) {
		try {
			getInputStreamForUrl(url, null, false);
			// no need to release connection since the stream has not been
			// retrieved
			// if the code above does not throw any exception, the connection is
			// fine
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Release the http connection after users finished reading the InputStream
	 * provided by the {@link #getInputStreamForUrl(String, IProgressMonitor)}
	 * method.
	 */
	public void releaseConnection() {
		if (getMethod != null) {
			Thread t = new Thread() {
				/*
				 * (non-Javadoc)
				 * 
				 * @see java.lang.Thread#run()
				 */
				@Override
				public void run() {
					getMethod.releaseConnection();
				}
			};
			t.start();

		}
	}
}

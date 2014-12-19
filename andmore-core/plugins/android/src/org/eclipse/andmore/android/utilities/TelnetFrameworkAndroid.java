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
package org.eclipse.andmore.android.utilities;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;

import org.apache.commons.net.telnet.TelnetClient;

/**
 * DESCRIPTION: This class defines the framework for telnet services to
 * comunicate with phoe or emulator devices. <BR>
 * RESPONSIBILITY: Provide telnet connection <BR>
 * COLABORATORS: none <BR>
 * USAGE: This class should be instantiated whenever a telnet connection to a
 * phone or emulator device is needed. <BR>
 */
public class TelnetFrameworkAndroid {

	// private InputStreamReader responseReader;

	// private PrintWriter commandWriter;

	private TelnetClient telnetClient;

	private long timeout = 5000L;

	/**
	 * Connect to a device using telnet.
	 * 
	 * @param telnetHost
	 *            the telnet host IP address
	 * @param telnetPort
	 *            the telnet port
	 * 
	 * @throws MotodevException
	 *             when the connection cannot be established
	 */
	public synchronized void connect(String telnetHost, int telnetPort) throws IOException {
		if ((telnetClient == null) || ((telnetClient != null) && (!telnetClient.isConnected()))) {
			telnetClient = new TelnetClient(telnetHost);
			telnetClient.connect(telnetHost, telnetPort);
		}
	}

	/**
	 * Disconnect a telnet connection to a device
	 * 
	 * @throws MotodevException
	 *             when the disconnection cannot be executed
	 */
	public synchronized void disconnect() throws IOException {
		if ((telnetClient != null) && (telnetClient.isConnected())) {
			telnetClient.disconnect();
		}
	}

	/**
	 * @return
	 *
	 */
	public synchronized String write(String telnetInputText, String[] waitFor) throws IOException {
		PrintWriter commandWriter = null;
		try {
			commandWriter = new PrintWriter(telnetClient.getOutputStream());
			commandWriter.println(telnetInputText);
			commandWriter.flush();
			if (waitFor != null) {
				return waitFor(waitFor);
			}
		} finally {
			if (commandWriter != null) {
				commandWriter.close();
			}
		}

		return null;
	}

	/**
	 * Tests if the telnet client instance is connected
	 * 
	 * @return true if it is connected; false otherwise
	 */
	public boolean isConnected() {
		boolean connected = false;
		if (telnetClient != null) {
			connected = telnetClient.isConnected();
		}
		return connected;
	}

	/**
     *
     */
	public String waitFor(String[] waitForArray) throws IOException {
		InputStreamReader responseReader = null;
		StringBuffer answerFromRemoteHost = new StringBuffer();

		try {
			responseReader = new InputStreamReader(telnetClient.getInputStream());

			boolean found = false;

			do {
				char readChar = 0;
				long currentTime = System.currentTimeMillis();
				long timeoutTime = currentTime + timeout;

				while (readChar == 0) {
					if (responseReader == null) {
						// responseReader can only be set to null if method
						// releaseTelnetInputStreamReader()
						// has been called, which should happen if host becomes
						// unavailable.
						throw new IOException("Telnet host is unavailable; stopped waiting for answer.");
					}

					if (responseReader.ready()) {
						readChar = (char) responseReader.read();
					} else {
						try {
							Thread.sleep(50);
						} catch (InterruptedException e) {
							// Do nothing
						}
					}

					currentTime = System.currentTimeMillis();

					if ((!responseReader.ready()) && (currentTime > timeoutTime)) {
						throw new IOException("A timeout has occured when trying to read the telnet stream");
					}
				}

				answerFromRemoteHost.append(readChar);

				for (String aWaitFor : waitForArray) {
					found = answerFromRemoteHost.toString().contains(aWaitFor);
				}

			} while (!found);
		} finally {
			if (responseReader != null) {
				responseReader.close();
			}
		}

		return answerFromRemoteHost.toString();
	}

	/**
	 * Retrieves the input stream associated to this telnet connection
	 */
	public InputStream getInputStream() {
		InputStream s = null;
		if (telnetClient != null) {
			s = telnetClient.getInputStream();
		}
		return s;
	}

	/**
	 * Retrieves the output stream associated to this telnet connection
	 */
	public OutputStream getOutputStream() {
		OutputStream s = null;
		if (telnetClient != null) {
			s = telnetClient.getOutputStream();
		}
		return s;
	}
}

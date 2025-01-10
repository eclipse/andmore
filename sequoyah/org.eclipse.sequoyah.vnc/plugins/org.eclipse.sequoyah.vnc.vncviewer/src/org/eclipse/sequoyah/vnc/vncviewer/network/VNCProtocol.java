/********************************************************************************
 * Copyright (c) 2007-2010 Motorola Inc. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Initial Contributor:
 * Daniel Franco (Motorola)
 *
 * Contributors:
 * Fabio Rigo - Bug [221741] - Support to VNC Protocol Extension
 * Eugene Melekhov (Montavista) - Bug [227793] - Implementation of the several encodings, performance enhancement etc
 * Daniel Barboza Franco - Bug [233775] - Does not have a way to enter the session password for the vnc connection
 * Daniel Barboza Franco - Bug [233062] - Protocol connection port is static.
 * Fabio Rigo - Bug [238191] - Enhance exception handling
 * Fabio Rigo (Eldorado Research Institute) - [246212] - Enhance encapsulation of protocol implementer 
 * Fabio Rigo (Eldorado Research Institute) - [260559] - Enhance protocol framework and VNC viewer robustness
 * Fabio Rigo (Eldorado Research Institute) - Bug [262632] - Avoid providing raw streams to the user in the protocol framework
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/

package org.eclipse.sequoyah.vnc.vncviewer.network;

import java.io.DataInput;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import org.eclipse.sequoyah.vnc.protocol.lib.IProtocolHandshake;
import org.eclipse.sequoyah.vnc.protocol.lib.ProtocolHandle;
import org.eclipse.sequoyah.vnc.protocol.lib.exceptions.ProtocolHandshakeException;
import org.eclipse.sequoyah.vnc.vncviewer.exceptions.ProtoClientException;
import org.eclipse.sequoyah.vnc.vncviewer.registry.VNCProtocolRegistry;

import static org.eclipse.sequoyah.vnc.utilities.logger.Logger.log;

/**
 * Abstract class that defines the main behavior of the VNC Protocol.
 */
abstract public class VNCProtocol implements IProtocolHandshake,
		IRFBConstants {

	private static final int PROTOCOL_VERSION_MESSAGE_SIZE = 12;

	private static final int VNC_AUTHENTICATION_CHALLENGE_MESSAGE_SIZE = 16;

	/**
	 * Returns the protocol version string
	 * 
	 * @return the protocol version string
	 */
	protected abstract String getVersion();

	abstract protected void handShake(DataInput in, OutputStream out)
			throws Exception;

	protected int[] getSupportedEncodings() {
		// return painter.getSupportedEncodings();
		return AbstractVNCPainter.getSupportedEncodingsStatic();
	}

	/**
	 * Implements the init phase of the RFB Protocol.
	 */
	private void initPhase(ProtocolHandle handle, DataInput in,
			OutputStream out, String password) throws Exception {

		VNCProtocolData previousData = VNCProtocolRegistry.getInstance().get(handle);
		VNCProtocolData data = new VNCProtocolData();

		if (previousData != null) {
			data.setVncPainter(previousData.getVncPainter());
		}
		
		data.setPassword(password);

		/* ClientInit */
		out.write(SHARED_FLAG); // SharedFlag

		/* ServerInit */
		data.setFbWidth(in.readUnsignedShort());
		data.setFbHeight(in.readUnsignedShort());

		PixelFormat pixelFormat = new PixelFormat();
		pixelFormat.getPixelFormat(in);
		data.setPixelFormat(pixelFormat);

		int nameLen = in.readInt();

		byte[] serverName = new byte[nameLen];
		in.readFully(serverName, 0, nameLen);

		String serverNameStr = ""; //$NON-NLS-1$
		for (int i = 0; i < serverName.length; i++) {
			char c = (char) serverName[i];
			serverNameStr += c;
		}
		data.setServerName(serverNameStr);

		VNCProtocolRegistry.getInstance().register(handle, data);

		sendEncodingsPreferences(getSupportedEncodings(), out);

	}

	void sendEncodingsPreferences(int[] encs, OutputStream out)
			throws IOException {
		int length = encs.length;
		byte[] b = new byte[4 + 4 * encs.length];

		b[0] = (byte) SET_ENCODINGS;
		b[1] = (byte) 0;
		b[2] = (byte) ((length >> 8) & 0xff);
		b[3] = (byte) (length & 0xff);

		for (int i = 0; i < length; i++) {
			b[4 + 4 * i] = (byte) ((encs[i] >> 24) & 0xff);
			b[5 + 4 * i] = (byte) ((encs[i] >> 16) & 0xff);
			b[6 + 4 * i] = (byte) ((encs[i] >> 8) & 0xff);
			b[7 + 4 * i] = (byte) (encs[i] & 0xff);
		}
		out.write(b);
	}

	/**
	 * This method compares each byte of the RFB Protocol client version using
	 * the String sent by the Server.
	 */
	protected void compareVersion(byte[] b) throws Exception {
		String clientVersion = getVersion();
		String serverVersion = new String(b);
		boolean versionOk = false;

		if (serverVersion.equals(clientVersion)) {
			versionOk = true;
		} else if ((serverVersion.length() == clientVersion.length())
				&& serverVersion.substring(0, 10).equals(
						clientVersion.substring(0, 10))) {
			// the last number of the version String
			if (serverVersion.charAt(10) > clientVersion.charAt(10)) {
				versionOk = true;
			}
		}
		if (!versionOk) {
			throw new ProtoClientException("Wrong protocol version."); //$NON-NLS-1$
		}
	}

	protected void negotiateProtocol(DataInput in, OutputStream out)
			throws Exception {
		byte[] b = new byte[PROTOCOL_VERSION_MESSAGE_SIZE];
		in.readFully(b, 0, PROTOCOL_VERSION_MESSAGE_SIZE);
		compareVersion(b);
		out.write(getVersion().getBytes());
	}

	protected int negotiateSecurity(DataInput in, OutputStream out)
			throws Exception {
		int[] securityTypes = readSecurityTypes(in);
		int securityType = chooseSecurityType(securityTypes);
		if (securityType != SECURITY_TYPE_INVALID) {
			sendSecurityType(out, securityType);
		}
		return securityType;
	}

	protected int chooseSecurityType(int[] securityTypes) throws Exception {
		for (int i = 0; i < securityTypes.length; i++) {
			if (securityTypeSupported(securityTypes[i])) {
				return securityTypes[i];
			}
		}
		return SECURITY_TYPE_INVALID;
	}

	protected int[] readSecurityTypes(DataInput in) throws Exception {
		int[] result = null;
		int secTypesNumber = in.readByte();
		if (secTypesNumber > 0) {
			result = new int[secTypesNumber];
			for (int i = 0; i < secTypesNumber; i++) {
				result[i] = in.readByte();
			}
		} else {
			handshakeFail(in);
		}
		return result;
	}

	protected void sendSecurityType(OutputStream out, int securityType)
			throws Exception {
		out.write((byte) securityType);
	}

	protected void handshakeFail(DataInput in) throws Exception {
		int failReasonLength;
		StringBuffer reason = new StringBuffer();
		failReasonLength = in.readInt();
		for (int j = 0; j < failReasonLength; j++) {
			reason.append((char) (in.readByte()));
		}
		throw new Exception("Handshake failed: " + reason.toString()); //$NON-NLS-1$
	}

	protected boolean securityTypeSupported(int type) {
		switch (type) {
		case SECURITY_TYPE_NONE: // None
		case SECURITY_TYPE_VNC: // VNCAuthentication
			return true;
		default:
			return false;
		}
	}

	protected void authenticate(DataInput in, OutputStream out,
			String password, int securityType) throws Exception {
		switch (securityType) {
		case SECURITY_TYPE_NONE:
			// No op. We do not need to do anything else
			break;
		case SECURITY_TYPE_VNC:
			authenticateVNC(in, out, password);
			break;
		default:
			throw new Exception("Handshake failed: unsupported security type " //$NON-NLS-1$
					+ securityType);
		}
	}

	protected void authenticateVNC(DataInput in, OutputStream out,
			String password) throws Exception {
		byte[] challenge = new byte[VNC_AUTHENTICATION_CHALLENGE_MESSAGE_SIZE];
		in.readFully(challenge, 0, VNC_AUTHENTICATION_CHALLENGE_MESSAGE_SIZE);

		byte[] pwd = { 0, 0, 0, 0, 0, 0, 0, 0 };
		byte[] pwdOrg = (password != null) ? password.getBytes() : new byte[0];
		for (int i = 0; i < 8 && i < pwdOrg.length; i++) {
			pwd[i] = pwdOrg[i];
		}

		DesEncoder des = new DesEncoder(pwd);
		des.encode(challenge, challenge);
		out.write(challenge);
	}

	protected void readAuthenticationResult(DataInput in)
			throws Exception {
		int securityResult = in.readInt();
		if (securityResult != 0) {
			handshakeFail(in);
		}
	}

	public void clientHandshake(ProtocolHandle handle, DataInput in,
			OutputStream out, Map<?,?> parameters) throws ProtocolHandshakeException {

		String password = (String) parameters.get("password"); //$NON-NLS-1$

		try {
			negotiateProtocol(in, out);
		} catch (Exception e) {
			log(VNCProtocol.class).error(
					"VNC protocol negotiation error: " + e.getMessage()); //$NON-NLS-1$
			throw new ProtocolHandshakeException("VNC protocol negotiation error."); //$NON-NLS-1$
		}

		int securityType;
		try {
			securityType = negotiateSecurity(in, out);
		} catch (Exception e) {
			log(VNCProtocol.class).error(
					"VNC security negotiation error: " + e.getMessage()); //$NON-NLS-1$
			throw new ProtocolHandshakeException("VNC security negotiation error."); //$NON-NLS-1$
		}

		try {
			authenticate(in, out, password, securityType);
			readAuthenticationResult(in);
		} catch (Exception e) {
			log(VNCProtocol.class).error(
					"VNC authenticate error: " + e.getMessage()); //$NON-NLS-1$
			throw new ProtocolHandshakeException("VNC authenticate error."); //$NON-NLS-1$
		}

		try {
			initPhase(handle, in, out, password);
		} catch (Exception e) {
			log(VNCProtocol.class).error(
					"VNC Init Phase error: " + e.getMessage()); //$NON-NLS-1$
			throw new ProtocolHandshakeException("VNC Init Phase error."); //$NON-NLS-1$
		}

	}

	public void serverHandshake(ProtocolHandle handle, DataInput in,
			OutputStream out, Map<?,?> parameters) throws ProtocolHandshakeException {

	}
}

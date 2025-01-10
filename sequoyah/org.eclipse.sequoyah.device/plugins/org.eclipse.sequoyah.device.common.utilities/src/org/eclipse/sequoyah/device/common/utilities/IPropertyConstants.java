/********************************************************************************
 * Copyright (c) 2007-2010 Motorola Inc.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Fabio Fantato (Motorola)
 * 
 * Contributors:
 * Fabio Fantato (Motorola) - bug#221733 - Package was changed to make able to any
 * 							  other plugin access these constants values.
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/
package org.eclipse.sequoyah.device.common.utilities;

public interface IPropertyConstants {
	public static final String HOST = "host"; //$NON-NLS-1$
	public static final String PORT = "port"; //$NON-NLS-1$
	public static final String DISPLAY = "display"; //$NON-NLS-1$
	public static final String PASSWORD = "password"; //$NON-NLS-1$

	public static final String DEFAULT_HOST = "127.0.0.1"; //$NON-NLS-1$
	public static final String DEFAULT_PORT = "5900"; //$NON-NLS-1$
	public static final String DEFAULT_DISPLAY = ":0.0"; //$NON-NLS-1$
	public static final String DEFAULT_PASSWORD = null;
}


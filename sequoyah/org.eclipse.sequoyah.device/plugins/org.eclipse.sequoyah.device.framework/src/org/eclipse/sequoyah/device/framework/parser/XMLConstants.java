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
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/
package org.eclipse.sequoyah.device.framework.parser;

public interface XMLConstants {
	public static final String TAG_CONFIG = "config"; //$NON-NLS-1$
	public static final String TAG_PROPERTY = "property"; //$NON-NLS-1$
	public static final String TAG_ID          = "id"; //$NON-NLS-1$
	public static final String TAG_NAME        = "name"; //$NON-NLS-1$
	public static final String TAG_DESCRIPTION = "description"; //$NON-NLS-1$
	public static final String TAG_SCOPE = "scope"; //$NON-NLS-1$
	public static final String TAG_RULE = "rule"; //$NON-NLS-1$
	
	
	public static final String SCOPE_DEVICE   = "DEVICE"; //$NON-NLS-1$
	public static final String SCOPE_SERVICE  = "SERVICE"; //$NON-NLS-1$
	public static final String SCOPE_INSTANCE = "INSTANCE"; //$NON-NLS-1$
	
	public static final String RULE_INVISIBLE = "INVISIBLE"; //$NON-NLS-1$
	public static final String RULE_READ_ONLY = "READ-ONLY"; //$NON-NLS-1$
	public static final String RULE_WRITABLE  = "WRITABLE"; //$NON-NLS-1$
	
	
}

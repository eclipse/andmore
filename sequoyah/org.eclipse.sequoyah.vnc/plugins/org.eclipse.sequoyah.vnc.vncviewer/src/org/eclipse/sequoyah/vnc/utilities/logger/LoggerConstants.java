/********************************************************************************
 * Copyright (c) 2007-2010 Motorola Inc. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Initial Contributor:
 * Fabio Fantato (Motorola)
 *
 * Contributors:
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/

package org.eclipse.sequoyah.vnc.utilities.logger;


/**
 * DESCRIPTION: This class define constants to be used in Log process
 *
 *
 * RESPONSIBILITY: Log framework
 *
 * COLABORATORS: none
 *
 *
 */
public class LoggerConstants
{
    /**
     * Status type severity indicating this status represents an debug.
     */
    public static final String DEBUG = "DEBUG"; //$NON-NLS-1$

    /**
     * Status type severity indicating this status is informational only.
     */
    public static final String INFO = "INFO"; //$NON-NLS-1$

    /**
     * Status type severity indicating this status represents a warning.
     */
    public static final String WARNING = "WARN"; //$NON-NLS-1$

    /**
     * Status type severity indicating this status represents an error.
     */
    public static final String ERROR = "ERROR"; //$NON-NLS-1$

    /**
     * Status type severity indicating this status represents an fatal error.
     */
    public static final String FATAL = "FATAL"; //$NON-NLS-1$

    /**
     * Status type severity indicating turn on all logging.
     */
    public static final String LOG_ON = "ALL"; //$NON-NLS-1$

    /**
     * Status type severity indicating to turn off logging.
     */
    public static final String LOG_OFF = "OFF"; //$NON-NLS-1$

    /**
     * References for logger simple
     */
    public static final int LOG_SIMPLE = -1;
    
    /**
     * References for logger org.apache.log4j
     */
    public static final int LOG4J = 0;

    /**
     * References for logger showed in the console
     */
    public static final int LOG_CONSOLE = 1;

    /**
     * References for logger showed in a file
     */
    public static final int LOG_FILE = 2;

    /**
     * References for logger showed in a html file
     */
    public static final int LOG_HTML = 3;
    
    
    /**
     * Status type severity indicating this status represents an debug.
     */
    public static final String TXT_DEBUG = "[DEBUG]"; //$NON-NLS-1$

    /**
     * Status type severity indicating this status is informational only.
     */
    public static final String TXT_INFO = "[INFO]"; //$NON-NLS-1$

    /**
     * Status type severity indicating this status represents a warning.
     */
    public static final String TXT_WARNING = "[WARN]"; //$NON-NLS-1$

    /**
     * Status type severity indicating this status represents an error.
     */
    public static final String TXT_ERROR = "[ERROR]"; //$NON-NLS-1$

    /**
     * Status type severity indicating this status represents an fatal error.
     */
    public static final String TXT_FATAL = "[FATAL]"; //$NON-NLS-1$

    /**
     * Status type severity indicating turn on all logging.
     */
    public static final String TXT_ALL = "[LOG]"; //$NON-NLS-1$
    
}

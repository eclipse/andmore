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

package org.eclipse.sequoyah.device.common.utilities.logger;


/**
 * This class define wich logger should be used by the system.
 * If Log4J was found in classpath, the Log4JLogger will be instanced. 
 * If was not possible to show log4J, all messages will be 
 * redirect to console using the SimpleLogger.
 */
public abstract class LoggerFactory
{   
    private static final String DEFAULT_LOG_FILE_NAME = "default.log"; //$NON-NLS-1$

	/**
     * Get logger chosen for application
     *
     * @param log
     *           logger used in application
     * @return instance of object log appropriate
     */
    public static final ILogger getLogger(int log)
    {
    	ILogger logger = null;
        switch (log)
        {
            case LoggerConstants.LOG_SIMPLE:
            	logger = SimpleLogger.getInstance();
            	break;
        }        
        return logger;        
    }

    /**
     * get logger chosen for application and configure this log in a class
     *
     * @param log log id
     * @param _class Class used to get log
     *
     * @return Ilogger instance
     */
	@SuppressWarnings("rawtypes")
	public static final ILogger getLogger(int log, Class _class)
    {
        getLogger(log).configureLogger(_class);

        return getLogger(log);
    }

    /**
     * get logger chosen for application and configure this log in a class
     * write the log in output defined by logOutput id 
     *
     * @param log log id
     * @param _class class used to get log
     * @param logOutput id used to define the log Output 
     * @param logMinLevel Set the level of this log
     * @param logFileName Set the filename that the log is writed
     *
     * @return ILogger instance 
     */
	@SuppressWarnings("rawtypes")
	public static final ILogger getLogger(int log, Class _class, int logOutput, String logMinLevel,
                                          String logFileName)
    {
        if (logFileName == null)
        {
            logFileName = DEFAULT_LOG_FILE_NAME;
        }

        switch (logOutput)
        {
            case LoggerConstants.LOG_CONSOLE:
                getLogger(log).setLogToConsole();

                break;

            case LoggerConstants.LOG_FILE:
                getLogger(log).setLogToFile(logFileName);

                break;

            case LoggerConstants.LOG_HTML:
                getLogger(log).setLogToHTMLFile(logFileName);

                break;

            default:
                getLogger(log).setLogToConsole();

                break;
        }

        getLogger(log).configureLogger(_class);

        if (logMinLevel != null)
        {
            getLogger(log).setLevel(logMinLevel);
        }

        return getLogger(log);
    }
}

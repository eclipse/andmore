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
 * This class define wich logger should be used by the system.
 * If Log4J was foudn Log4JLogger will be instanced their
 * framework will be available.
 * If was not possible to show log4J, all messages will be 
 * redirect to console using the GenericLogger.
 */
public abstract class LoggerFactory
{   
    /**
     * Get logger chosen for application
     *
     * @param log
     *           logger used in application
     * @return instance of object log appropriate
     */
    public static final ILogger getFactory(int log)
    {
    	ILogger logger =  SimpleLogger.getInstance();
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
    @SuppressWarnings("unchecked")
	public static final ILogger getLogger(int log, Class _class)
    {
        getFactory(log).configureLogger(_class);

        return getFactory(log);
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
    @SuppressWarnings("unchecked")
	public static final ILogger getLogger(int log, Class _class, int logOutput, String logMinLevel,
                                          String logFileName)
    {
        if (logFileName == null)
        {
            logFileName = "default.log"; //$NON-NLS-1$
        }

        switch (logOutput)
        {
            case LoggerConstants.LOG_CONSOLE:
                getFactory(log).setLogToConsole();

                break;

            case LoggerConstants.LOG_FILE:
                getFactory(log).setLogToFile(logFileName);

                break;

            case LoggerConstants.LOG_HTML:
                getFactory(log).setLogToHTMLFile(logFileName);

                break;

            default:
                getFactory(log).setLogToConsole();

                break;
        }

        getFactory(log).configureLogger(_class);

        if (logMinLevel != null)
        {
            getFactory(log).setLevel(logMinLevel);
        }

        return getFactory(log);
    }
}

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
 * DESCRIPTION:  A Logger object is used to log messages for CDE Components. COLABORATORS: none USAGE:  Used to register messages in some outputs like files or console.
 */
public class Logger
{
    private static ILogger log;
    private static int logModel;

    static
    {
    	logModel= LoggerConstants.LOG_SIMPLE;
    }

    /**
     * @param model
     */
    public static void logModel(int model){
    	logModel=model;
    }
    
    /**
     * Configure a log to a specific class
     *
     * @param _class Class used to get log
     *
     * @return Ilogger instance
     */
	@SuppressWarnings("rawtypes")
	public static ILogger log(Class _class)
    {
    	if (log==null) {
    		log = LoggerFactory.getLogger(logModel, Logger.class);    		
    	}    	
        log.configureLogger(_class);

        return log;
    }

    /**
     * Configure log settings level and specific output, defined by logOutput id
     *
     * @param logOutput ID of logOutput
     * @param logMinLevel Level of Log
     * @param logFileName File name to write the log
     */
    public static void logConfig(int logOutput, String logMinLevel, String logFileName)
    {    	
        log = LoggerFactory.getLogger(logModel, Logger.class, logOutput, logMinLevel,logFileName);
    }
}

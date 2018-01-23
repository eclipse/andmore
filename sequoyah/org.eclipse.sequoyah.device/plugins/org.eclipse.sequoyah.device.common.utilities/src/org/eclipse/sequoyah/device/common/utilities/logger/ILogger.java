/********************************************************************************
 * Copyright (c) 2007-2010 Motorola Inc. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Initial Contributor:
 * Fabio Fantato (Instituto Eldorado)
 *
 * Contributors:
 * Fabio Fantato (Instituto Eldorado) - [243494] Change the reference implementation to work on Galileo
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/


package org.eclipse.sequoyah.device.common.utilities.logger;

/**
 * DESCRIPTION: This class is an interface to log
 *
 *
 * RESPONSIBILITY: Log framework
 *
 * COLABORATORS: none
 *
 * USAGE: Define the methods available to log
 *
 */
public interface ILogger
{
    /**
     * @param configure
     */
    public void configureLogger(Object configure);

    /**
     * @param message
     */
    public void debug(Object message);

    /**
     * @param message
     */
    public void error(Object message);

    /**
     *
     * @param message
     * @param throwable
     */
    public void error(Object message, Object throwable);

    /**
     * @param message
     */
    public void fatal(Object message);

    /**
     * Return the current level of the object
     *
     * @return the object!
     */
    public Object getCurrentLevel();

    /**
     * @param message
     */
    public void info(Object message);

    /**
     * @param priority
     * @param message
     * @param throwable
     * @see LoggerConstants
     */
    public void log(Object priority, Object message, Object throwable);

    /**
     * @param priority
     * @param message
     */
    public void log(Object priority, Object message);

    /**
     * @param throwable
     */
    public void log(Object throwable);

    /**
     * @param level 
     * @see LoggerConstants
     */
    public void setLevel(Object level);

    /**
     * Show the log in Console
     */
    public void setLogToConsole();

    /**
     * Show the log in Default Console
     */
    public void setLogToDefault();
    
    /**
     * Write the log into a file
     *
     * @param filename the file name 
     */
    public void setLogToFile(String filename);

    /**
     * Write the log into a file
     *
     * @param filename the file name
     * @param threshold level of message
     */
    public void setLogToFile(String filename, String threshold);

    /**
     * Write the log into a HTML file
     *
     * @param htmlFilename the HTML file name
     */
    public void setLogToHTMLFile(String htmlFilename);

    /**
     * @param message
     */
    public void warn(Object message);
}

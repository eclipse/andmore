/********************************************************************************
 * Copyright (c) 2007-2010 Motorola Inc. All rights reserved.
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

package org.eclipse.sequoyah.vnc.utilities.logger;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;


public final class SimpleLogger implements ILogger
{
    private static SimpleLogger logLogger;
    private static final Map<String, String> levelsMap;
    
    static {
    	levelsMap = new HashMap<String, String>();
        levelsMap.put(LoggerConstants.DEBUG, LoggerConstants.TXT_DEBUG);
        levelsMap.put(LoggerConstants.INFO, LoggerConstants.TXT_INFO);
        levelsMap.put(LoggerConstants.WARNING, LoggerConstants.TXT_WARNING);
        levelsMap.put(LoggerConstants.ERROR, LoggerConstants.TXT_ERROR);
        levelsMap.put(LoggerConstants.FATAL, LoggerConstants.TXT_FATAL);
        levelsMap.put(LoggerConstants.LOG_ON, LoggerConstants.TXT_ALL);
    }
    
    
    private SimpleLogger()    
    {
    	MessageConsole console = new MessageConsole("System Output", null); //$NON-NLS-1$
    	ConsolePlugin.getDefault().getConsoleManager().addConsoles(new IConsole[] { console });
    	ConsolePlugin.getDefault().getConsoleManager().showConsoleView(console);
    	MessageConsoleStream stream = console.newMessageStream();

    	System.setOut(new PrintStream(stream));
    	System.setErr(new PrintStream(stream));

    }

    /**
     * Configure the Log view
     *
     * @param logView Log view ID
     */
    private void configureLogView(int logView)
    {
        switch (logView)
        {
            case LoggerConstants.LOG_CONSOLE:
                setAppenderConsole();
                break;
            default:
                String errMsg = Integer.toString(logView);
                throw new IllegalArgumentException(errMsg);
        }
    }

    /**
     * Configure log in a specific class
     * @param classType The class object
     * @see com.motorola.motodev.cde.utilities.log.ILogger#configureLogger(java.lang.Object)
     */
    public void configureLogger(Object classType)
    {
    	configureLogView(LoggerConstants.LOG_CONSOLE);   	
    }

    /**
     * Debug a message into logger
     * @param message a message object
     * @see com.motorola.motodev.cde.log.ILogger#info(java.lang.Object)
     */
    public void debug(Object message)
    {
        System.out.println(LoggerConstants.TXT_DEBUG+message);
    }

    /**
     * Register an error message into logger
     * @param message a message object
     * @see com.motorola.motodev.cde.log.ILogger#error(java.lang.Object)
     */
    public void error(Object message)
    {
    	System.out.println(LoggerConstants.TXT_ERROR+message);
    }

    /**
     * Register an error message into logger includding the throwable
     * @param message a message object
     * @param throwable a throwable object
     * @see com.motorola.motodev.cde.log.ILogger#error(java.lang.Object,
     *      java.lang.Object)
     */
    public void error(Object message, Object throwable)
    {
    	System.out.println(LoggerConstants.TXT_ERROR+message+"-"+((Throwable)throwable).getMessage()); //$NON-NLS-1$
    }

    /**
     * (non-Javadoc)
     *
     * @see com.motorola.motodev.cde.log.ILogger#fatal(java.lang.Object)
     */
    public void fatal(Object message)
    {
    	System.out.println(LoggerConstants.TXT_FATAL+message);
    }

    /**
     * Get the current log level
     *
     * @return the current log level
     */
    public Object getCurrentLevel()
    {
        return LoggerConstants.TXT_ALL;
    }

    /**
     * Get instance of logger
     *
     * @return logger
     */
    public static SimpleLogger getInstance()
    {
        if (logLogger == null)
        {
            logLogger = new SimpleLogger();
        }

        return logLogger;
    }

    /**
     * get Message info
     * @param message a message object
     * @see com.motorola.motodev.cde.log.ILogger#info(java.lang.Object)
     */
    public void info(Object message)
    {
    	System.out.println(LoggerConstants.TXT_INFO+message);
    }

    /**
     * Set a message log with priority and throwable
     * @param priority priority object defined in levelsMap
     * @param message a message object
     * @param throwable a throwable object
     * @see com.motorola.motodev.cde.log.ILogger#log(java.lang.Object,
     *      java.lang.Object, java.lang.Object)
     */
    public void log(Object priority, Object message, Object throwable)
    {
    	System.out.println(levelsMap.get(priority)+message+"-"+((Throwable)throwable).getMessage()); //$NON-NLS-1$
    }

    /**
     * @param throwable
     * @see com.motorola.motodev.cde.log.ILogger#log(java.lang.Object)
     */
    public void log(Object throwable)
    {
    	System.out.println(LoggerConstants.TXT_ALL+((Throwable)throwable).getMessage());
    }

    /**
     * Set a message with priority
     * @param priority priority object defined in levelsMap
     * @param message a message object 
     * @see com.motorola.motodev.cde.log.ILogger#log(java.lang.Object,
     *      java.lang.Object)
     */
    public void log(Object priority, Object message)
    {
    	System.out.println(levelsMap.get(priority)+message);
    }

    /**
     * Sets a Appender Console
     */
    private void setAppenderConsole()
    {
    }

    /**
     * Sets a Appender File
     */

	private void setAppenderFile()
    {
    }

    /**
     * Sets a Appender HTML File
     */
	private void setAppenderHTML()
    {
    }

    /**
     * Sets the level of log
     * @param level Level object
     * @see com.motorola.motodev.cde.log.ILogger#setLevel(java.lang.Object)
     */
    public void setLevel(Object level)
    {
    }

    /**
     * Sets the log view in console
     */
    public void setLogToConsole()
    {
    }

    /**
     * Sets the log in a file
     *
     * @param filename the file name
     */
    public void setLogToFile(String filename)
    {
    }

    /**
     * Write the log into a file
     *
     * @param filename the file name
     * @param threshold level of message
     */
    public void setLogToFile(String filename, String threshold)
    {
       
    }

    /**
     * Sets the log in a HTML file
     *
     * @param filename the HTML file name
     */
    public void setLogToHTMLFile(String htmlFilename)
    {
    }

    /**
     * Sets a message in log with warn level
     * @param message message object
     * @see com.motorola.motodev.cde.log.ILogger#warn(java.lang.Object)
     */
    public void warn(Object message)
    {
    	System.out.println(LoggerConstants.TXT_WARNING+message);
    }
}

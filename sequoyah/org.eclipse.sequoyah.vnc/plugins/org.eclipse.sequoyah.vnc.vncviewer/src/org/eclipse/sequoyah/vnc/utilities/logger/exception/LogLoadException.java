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


package org.eclipse.sequoyah.vnc.utilities.logger.exception;


@SuppressWarnings("serial")
public class LogLoadException extends Exception
{
    /**
     * Constructor with no arguments
     */
    public LogLoadException()
    {
        super();
    }

    /**
     * Constructor with a string argument: the message to be added to the
     * exception
     *
     * @param arg0
     */
    public LogLoadException(String arg0)
    {
        super(arg0);
    }

    /**
     * Constructor with a throwable argument: a throwable object to be added to
     * the exception
     *
     * @param arg0
     */
    public LogLoadException(Throwable arg0)
    {
        super(arg0);
    }

    /**
     * Constructor with a string argument and a throwable argument: the string
     * refers to the message to be added to the exception and the throwable
     * object is the object to be added to the exception
     *
     * @param arg0
     * @param arg1
     */
    public LogLoadException(String arg0, Throwable arg1)
    {
        super(arg0, arg1);
    }
}

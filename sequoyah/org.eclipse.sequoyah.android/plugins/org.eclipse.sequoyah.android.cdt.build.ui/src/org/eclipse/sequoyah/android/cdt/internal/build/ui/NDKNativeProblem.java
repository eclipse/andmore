/*******************************************************************************
 * Copyright (c) 2010 Motorola, Inc. All rights reserved.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Initial contributors:
 * Carlos Alberto Souto Junior (Eldorado)
 *******************************************************************************/

package org.eclipse.sequoyah.android.cdt.internal.build.ui;

import org.eclipse.jdt.core.compiler.CategorizedProblem;

public class NDKNativeProblem extends CategorizedProblem
{

    private int id;

    private int startPosition, endPosition, line, column;

    private int severity;

    private String[] arguments;

    private String message;

    public NDKNativeProblem(

    String message, int id, String[] stringArguments, int severity, int startPosition,
            int endPosition, int line, int column)
    {

        this.message = message;
        this.id = id;
        this.arguments = stringArguments;
        this.severity = severity;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.line = line;
        this.column = column;

    }

    public String[] getArguments()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public int getID()
    {
        // TODO Auto-generated method stub
        return 0;

    }

    public String getMessage()
    {
        // TODO Auto-generated method stub
        return this.message;
    }

    public char[] getOriginatingFileName()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public int getSourceEnd()
    {
        // TODO Auto-generated method stub
        return endPosition;
    }

    public int getSourceLineNumber()
    {
        // TODO Auto-generated method stub
        return this.line;
    }

    public int getSourceStart()
    {
        // TODO Auto-generated method stub
        return startPosition;
    }

    public int getSourceColumnNumber()
    {
        return this.column;
    }

    public boolean isError()
    {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isWarning()
    {
        // TODO Auto-generated method stub
        return true;
    }

    public void setSourceEnd(int sourceEnd)
    {
        // TODO Auto-generated method stub

    }

    public void setSourceLineNumber(int lineNumber)
    {
        // TODO Auto-generated method stub

    }

    public void setSourceStart(int sourceStart)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public int getCategoryID()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getMarkerType()
    {
        // TODO Auto-generated method stub
        return "org.eclipse.sequoyah.android.cdt.build.ui.managedMarker";
    }

}
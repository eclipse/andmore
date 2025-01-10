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

import java.util.ArrayList;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodDeclaration;

public class NDKNativeMethodDetectionVisitor extends ASTVisitor
{

    //private int[] nativeLine;
    private boolean nativePresence = false;

    private int startPosition, endPosition;

    private int numberNatives = 0;

    private ArrayList<NDKNativeProblem> problems;

    public int getNumberNatives()
    {
        return numberNatives;
    }

    public void setNumberNatives(int numberNatives)
    {
        this.numberNatives = numberNatives;
    }

    public int getStartPosition()
    {
        return startPosition;
    }

    public void setStartPosition(int startPosition)
    {
        this.startPosition = startPosition;
    }

    public int getEndPosition()
    {
        return endPosition;
    }

    public void setEndPosition(int endPosition)
    {
        this.endPosition = endPosition;
    }

    public void setNativePresence(boolean nativePresence)
    {
        this.nativePresence = nativePresence;
    }

    public boolean isNativePresence()
    {
        return nativePresence;
    }

    @Override
    public boolean visit(MethodDeclaration node)
    {

        nativePresence = false;
        Object[] modifiersList = node.modifiers().toArray();
        for (Object o : modifiersList)
        {
            if (o.toString().equals("native")) //$NON-NLS-1$
            {
                nativePresence = true;
                startPosition = node.getStartPosition();
                endPosition = node.getLength() + startPosition;
                problems.add(new NDKNativeProblem(
                        Messages.NDKNativeMethodDetectionVisitor_ProblemName0, 0, null, 0,
                        startPosition, endPosition, 14, 1));

            }
        }

        return true;

    }

    public NDKNativeProblem[] getProblems()
    {
        NDKNativeProblem[] newProblems = new NDKNativeProblem[problems.size()];
        for (int i = 0; i < problems.size(); i++)
        {
            newProblems[i] = problems.get(i);

        }
        return newProblems;
    }

    public void resetVisitor()
    {
        problems = new ArrayList<NDKNativeProblem>();
    }

}

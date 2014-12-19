/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.andmore.internal.wizards.newproject;

import org.eclipse.jdt.internal.ui.packageview.PackageExplorerPart;
import org.eclipse.jdt.internal.ui.workingsets.IWorkingSetIDs;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkingSet;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This class contains a helper method to deal with working sets.
 * <p/>
 * Copied from org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageOne
 */
@SuppressWarnings("restriction")
public final class WorkingSetHelper {

    private static final IWorkingSet[] EMPTY_WORKING_SET_ARRAY = new IWorkingSet[0];

    /** This class is never instantiated. */
    private WorkingSetHelper() {
    }

    public static IWorkingSet[] getSelectedWorkingSet(IStructuredSelection selection,
            IWorkbenchPart activePart) {
        IWorkingSet[] selected= getSelectedWorkingSet(selection);
        if (selected != null && selected.length > 0) {
            for (int i= 0; i < selected.length; i++) {
                if (!isValidWorkingSet(selected[i]))
                    return EMPTY_WORKING_SET_ARRAY;
            }
            return selected;
        }

        if (!(activePart instanceof PackageExplorerPart))
            return EMPTY_WORKING_SET_ARRAY;

        PackageExplorerPart explorerPart= (PackageExplorerPart) activePart;
        if (explorerPart.getRootMode() == PackageExplorerPart.PROJECTS_AS_ROOTS) {
            //Get active filter
            IWorkingSet filterWorkingSet= explorerPart.getFilterWorkingSet();
            if (filterWorkingSet == null)
                return EMPTY_WORKING_SET_ARRAY;

            if (!isValidWorkingSet(filterWorkingSet))
                return EMPTY_WORKING_SET_ARRAY;

            return new IWorkingSet[] {filterWorkingSet};
        } else {
            //If we have been gone into a working set return the working set
            Object input= explorerPart.getViewPartInput();
            if (!(input instanceof IWorkingSet))
                return EMPTY_WORKING_SET_ARRAY;

            IWorkingSet workingSet= (IWorkingSet)input;
            if (!isValidWorkingSet(workingSet))
                return EMPTY_WORKING_SET_ARRAY;

            return new IWorkingSet[] {workingSet};
        }
    }

    private static IWorkingSet[] getSelectedWorkingSet(IStructuredSelection selection) {
        if (!(selection instanceof ITreeSelection))
            return EMPTY_WORKING_SET_ARRAY;

        ITreeSelection treeSelection= (ITreeSelection) selection;
        if (treeSelection.isEmpty())
            return EMPTY_WORKING_SET_ARRAY;

        List<?> elements = treeSelection.toList();
        if (elements.size() == 1) {
            Object element= elements.get(0);
            TreePath[] paths= treeSelection.getPathsFor(element);
            if (paths.length != 1)
                return EMPTY_WORKING_SET_ARRAY;

            TreePath path= paths[0];
            if (path.getSegmentCount() == 0)
                return EMPTY_WORKING_SET_ARRAY;

            Object candidate= path.getSegment(0);
            if (!(candidate instanceof IWorkingSet))
                return EMPTY_WORKING_SET_ARRAY;

            IWorkingSet workingSetCandidate= (IWorkingSet) candidate;
            if (isValidWorkingSet(workingSetCandidate))
                return new IWorkingSet[] { workingSetCandidate };

            return EMPTY_WORKING_SET_ARRAY;
        }

        ArrayList<Object> result = new ArrayList<Object>();
        for (Iterator<?> iterator = elements.iterator(); iterator.hasNext();) {
            Object element= iterator.next();
            if (element instanceof IWorkingSet && isValidWorkingSet((IWorkingSet) element)) {
                result.add(element);
            }
        }
        return result.toArray(new IWorkingSet[result.size()]);
    }


    private static boolean isValidWorkingSet(IWorkingSet workingSet) {
        String id= workingSet.getId();
        if (!IWorkingSetIDs.JAVA.equals(id) && !IWorkingSetIDs.RESOURCE.equals(id))
            return false;

        if (workingSet.isAggregateWorkingSet())
            return false;

        return true;
    }
}

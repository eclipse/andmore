/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.eclipse.andmore;

import static com.android.SdkConstants.CLASS_CONSTRUCTOR;
import static com.android.SdkConstants.CONSTRUCTOR_NAME;

import com.google.common.base.Predicate;

import org.eclipse.andmore.ddms.ISourceRevealer;
import org.eclipse.andmore.internal.project.BaseProjectHelper;
import org.eclipse.andmore.internal.project.ProjectHelper;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IPerspectiveRegistry;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.dialogs.ListDialog;
import org.eclipse.ui.ide.IDE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of the com.android.ide.ddms.sourceRevealer extension point.
 * Note that this code is duplicated in the PDT plugin's SourceRevealer as well.
 */
public class SourceRevealer implements ISourceRevealer {
    @Override
    public boolean reveal(String applicationName, String className, int line) {
        IProject project = ProjectHelper.findAndroidProjectByAppName(applicationName);
        if (project != null) {
            return BaseProjectHelper.revealSource(project, className, line);
        }

        return false;
    }

    /**
     * Reveal the source for given fully qualified method name.<br>
     *
     * The method should take care of the following scenarios:<ol>
     * <li> A search, either by filename/line number, or for fqmn might provide only 1 result.
     *    In such a case, just open that result. Give preference to the file name/line # search
     *    since that is the most accurate (gets to the line number). </li>
     * <li> The search might not provide any results. e.g, the method name may be of the form
     *    "com.x.y$1.methodName". Searches for methods within anonymous classes will fail. In
     *    such a case, if the fileName:lineNumber argument is available, a search for that
     *    should be made instead. </li>
     * <li> The search might provide multiple results. In such a case, the fileName/lineNumber
     *    values should be utilized to narrow down the results.</li>
     * </ol>
     *
     * @param fqmn fully qualified method name
     * @param fileName file name in which the method is present, null if not known
     * @param lineNumber line number in the file which should be given focus, -1 if not known.
     *        Line numbers begin at 1, not 0.
     * @param perspective perspective to switch to before the source is revealed, null to not
     *        switch perspectives
     */
    @Override
    public boolean revealMethod(String fqmn, String fileName, int lineNumber, String perspective) {
        // Search by filename:linenumber. If there is just one result for it, that would
        // be the correct match that is accurate to the line
        List<SearchMatch> fileMatches = Collections.emptyList();
        if (fileName != null && lineNumber >= 0) {
            fileMatches = searchForFile(fileName);
            if (fileMatches.size() == 1) {
                return revealLineMatch(fileMatches, fileName, lineNumber, perspective);
            }
        }

        List<SearchMatch> methodMatches = searchForMethod(fqmn);

        // if there is a unique method name match:
        //    1. if there are > 1 file name matches, try to see if they can be narrowed down
        //    2. if not, display the method match
        if (methodMatches.size() == 1) {
            if (fileMatches.size() > 0) {
                List<SearchMatch> filteredMatches = filterMatchByResource(fileMatches,
                                                        methodMatches.get(0).getResource());
                if (filteredMatches.size() == 1) {
                    return revealLineMatch(filteredMatches, fileName, lineNumber, perspective);
                }
            } else if (fileName != null && lineNumber > 0) {
                // Couldn't find file match, but we have a filename and line number: attempt
                // to use this to pinpoint the location within the method
                IMethod method = (IMethod) methodMatches.get(0).getElement();
                IJavaElement element = method;
                while (element != null) {
                    if (element instanceof ICompilationUnit) {
                        ICompilationUnit unit = ((ICompilationUnit) element).getPrimary();
                        IResource resource = unit.getResource();
                        if (resource instanceof IFile) {
                            IFile file = (IFile) resource;

                            try {
                                // See if the line number looks like it's inside the given method
                                ISourceRange sourceRange = method.getSourceRange();
                                IRegion region = AdtUtils.getRegionOfLine(file, lineNumber - 1);
                                // When fields are initialized with code, this logically belongs
                                // to the constructor, but the line numbers are outside of the
                                // constructor. In this case we'll trust the line number rather
                                // than the method range.
                                boolean isConstructor = fqmn.endsWith(CONSTRUCTOR_NAME);
                                if (isConstructor
                                        || region != null
                                            && region.getOffset() >= sourceRange.getOffset()
                                            && region.getOffset() < sourceRange.getOffset()
                                            + sourceRange.getLength()) {
                                    // Yes: use the line number instead
                                    if (perspective != null) {
                                        SourceRevealer.switchToPerspective(perspective);
                                    }
                                    return displayFile(file, lineNumber);
                                }

                            } catch (JavaModelException e) {
                                AdtPlugin.log(e, null);
                            }
                        }
                    }
                    element = element.getParent();
                }

            }

            return displayMethod((IMethod) methodMatches.get(0).getElement(), perspective);
        }

        // no matches for search by method, so search by filename
        if (methodMatches.size() == 0) {
            if (fileMatches.size() > 0) {
                return revealLineMatch(fileMatches, fileName, lineNumber, perspective);
            } else {
                // Last ditch effort: attempt to look up the class corresponding to the fqn
                // and jump to the line there
                if (fileMatches.isEmpty() && fqmn.indexOf('.') != -1) {
                    String className = fqmn.substring(0, fqmn.lastIndexOf('.'));
                    for (IJavaProject project : BaseProjectHelper.getAndroidProjects(null)) {
                        IType type;
                        try {
                            type = project.findType(className);
                            if (type != null && type.exists()) {
                                IResource resource = type.getResource();
                                if (resource instanceof IFile) {
                                    if (perspective != null) {
                                        SourceRevealer.switchToPerspective(perspective);
                                    }
                                    return displayFile((IFile) resource, lineNumber);
                                }
                            }
                        } catch (JavaModelException e) {
                            AdtPlugin.log(e, null);
                        }
                    }
                }

                return false;
            }
        }

        // multiple matches for search by method, narrow down by filename
        if (fileName != null) {
            return revealLineMatch(
                    filterMatchByFileName(methodMatches, fileName),
                    fileName, lineNumber, perspective);
        }

        // prompt the user
        SearchMatch match = getMatchToDisplay(methodMatches, fqmn);
        if (match == null) {
            return false;
        } else {
            return displayMethod((IMethod) match.getElement(), perspective);
        }
    }

    private boolean revealLineMatch(List<SearchMatch> matches, String fileName, int lineNumber,
            String perspective) {
        SearchMatch match = getMatchToDisplay(matches,
                String.format("%s:%d", fileName, lineNumber));
        if (match == null) {
            return false;
        }

        if (perspective != null) {
            SourceRevealer.switchToPerspective(perspective);
        }

        return displayFile((IFile) match.getResource(), lineNumber);
    }

    private boolean displayFile(IFile file, int lineNumber) {
        try {
            IMarker marker = file.createMarker(IMarker.TEXT);
            marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
            IDE.openEditor(
                    PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(),
                    marker);
            marker.delete();
            return true;
        } catch (CoreException e) {
            AdtPlugin.printErrorToConsole(e.getMessage());
            return false;
        }
    }

    private boolean displayMethod(IMethod method, String perspective) {
        if (perspective != null) {
            SourceRevealer.switchToPerspective(perspective);
        }

        try {
            JavaUI.openInEditor(method);
            return true;
        } catch (Exception e) {
            AdtPlugin.printErrorToConsole(e.getMessage());
            return false;
        }
    }

    private List<SearchMatch> filterMatchByFileName(List<SearchMatch> matches, String fileName) {
        if (fileName == null) {
            return matches;
        }

        // Use a map to collapse multiple matches in a single file into just one match since
        // we know the line number in the file.
        Map<IResource, SearchMatch> matchesPerFile =
                new HashMap<IResource, SearchMatch>(matches.size());

        for (SearchMatch m: matches) {
            if (m.getResource() instanceof IFile
                    && m.getResource().getName().startsWith(fileName)) {
                matchesPerFile.put(m.getResource(), m);
            }
        }

        List<SearchMatch> filteredMatches = new ArrayList<SearchMatch>(matchesPerFile.values());

        // sort results, first by project name, then by file name
        Collections.sort(filteredMatches, new Comparator<SearchMatch>() {
            @Override
            public int compare(SearchMatch m1, SearchMatch m2) {
                String p1 = m1.getResource().getProject().getName();
                String p2 = m2.getResource().getProject().getName();

                if (!p1.equals(p2)) {
                    return p1.compareTo(p2);
                }

                String r1 = m1.getResource().getName();
                String r2 = m2.getResource().getName();
                return r1.compareTo(r2);
            }
        });
        return filteredMatches;
    }

    private List<SearchMatch> filterMatchByResource(List<SearchMatch> matches,
            IResource resource) {
        List<SearchMatch> filteredMatches = new ArrayList<SearchMatch>(matches.size());

        for (SearchMatch m: matches) {
            if (m.getResource().equals(resource)) {
                filteredMatches.add(m);
            }
        }

        return filteredMatches;
    }

    private SearchMatch getMatchToDisplay(List<SearchMatch> matches, String searchTerm) {
        // no matches for given search
        if (matches.size() == 0) {
            return null;
        }

        // there is only 1 match, so we return that
        if (matches.size() == 1) {
            return matches.get(0);
        }

        // multiple matches, prompt the user to select
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window == null) {
            return null;
        }

        ListDialog dlg = new ListDialog(window.getShell());
        dlg.setMessage("Multiple files match search: " + searchTerm);
        dlg.setTitle("Select file to open");
        dlg.setInput(matches);
        dlg.setContentProvider(new IStructuredContentProvider() {
            @Override
            public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            }

            @Override
            public void dispose() {
            }

            @Override
            public Object[] getElements(Object inputElement) {
                return ((List<?>) inputElement).toArray();
            }
        });
        dlg.setLabelProvider(new LabelProvider() {
           @Override
           public String getText(Object element) {
               SearchMatch m = (SearchMatch) element;
               return String.format("/%s/%s",    //$NON-NLS-1$
                       m.getResource().getProject().getName(),
                       m.getResource().getProjectRelativePath().toString());
           }
        });
        dlg.setInitialSelections(new Object[] { matches.get(0) });
        dlg.setHelpAvailable(false);

        if (dlg.open() == Window.OK) {
            Object[] selectedMatches = dlg.getResult();
            if (selectedMatches.length > 0) {
                return (SearchMatch) selectedMatches[0];
            }
        }

        return null;
    }

    private List<SearchMatch> searchForFile(String fileName) {
        return searchForPattern(fileName, IJavaSearchConstants.CLASS, MATCH_IS_FILE_PREDICATE);
    }

    private List<SearchMatch> searchForMethod(String fqmn) {
        if (fqmn.endsWith(CONSTRUCTOR_NAME)) {
            fqmn = fqmn.substring(0, fqmn.length() - CONSTRUCTOR_NAME.length() - 1); // -1: dot
            return searchForPattern(fqmn, IJavaSearchConstants.CONSTRUCTOR,
                    MATCH_IS_METHOD_PREDICATE);
        }
        if (fqmn.endsWith(CLASS_CONSTRUCTOR)) {
            // Don't try to search for class init methods: Eclipse will throw NPEs if you do
            return Collections.emptyList();
        }

        return searchForPattern(fqmn, IJavaSearchConstants.METHOD, MATCH_IS_METHOD_PREDICATE);
    }

    private List<SearchMatch> searchForPattern(String pattern, int searchFor,
            Predicate<SearchMatch> filterPredicate) {
        SearchEngine se = new SearchEngine();
        SearchPattern searchPattern = SearchPattern.createPattern(
                pattern,
                searchFor,
                IJavaSearchConstants.DECLARATIONS,
                SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE);
        SearchResultAccumulator requestor = new SearchResultAccumulator(filterPredicate);
        try {
            se.search(searchPattern,
                    new SearchParticipant[] {SearchEngine.getDefaultSearchParticipant()},
                    SearchEngine.createWorkspaceScope(),
                    requestor,
                    new NullProgressMonitor());
        } catch (CoreException e) {
            AdtPlugin.printErrorToConsole(e.getMessage());
            return Collections.emptyList();
        }

        return requestor.getMatches();
    }

    private static final Predicate<SearchMatch> MATCH_IS_FILE_PREDICATE =
            new Predicate<SearchMatch>() {
                @Override
                public boolean apply(SearchMatch match) {
                    return match.getResource() instanceof IFile;
                }
            };

    private static final Predicate<SearchMatch> MATCH_IS_METHOD_PREDICATE =
            new Predicate<SearchMatch>() {
                @Override
                public boolean apply(SearchMatch match) {
                    return match.getResource() instanceof IFile;
                }
            };

    private static class SearchResultAccumulator extends SearchRequestor {
        private final List<SearchMatch> mSearchMatches = new ArrayList<SearchMatch>();
        private final Predicate<SearchMatch> mPredicate;

        public SearchResultAccumulator(Predicate<SearchMatch> filterPredicate) {
            mPredicate = filterPredicate;
        }

        public List<SearchMatch> getMatches() {
            return mSearchMatches;
        }

        @Override
        public void acceptSearchMatch(SearchMatch match) throws CoreException {
            if (mPredicate.apply(match)) {
                mSearchMatches.add(match);
            }
        }
    }

    private static void switchToPerspective(String perspectiveId) {
        IWorkbench workbench = PlatformUI.getWorkbench();
        IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
        IPerspectiveRegistry perspectiveRegistry = workbench.getPerspectiveRegistry();
        if (perspectiveId != null
                && perspectiveId.length() > 0
                && perspectiveRegistry.findPerspectiveWithId(perspectiveId) != null) {
            try {
                workbench.showPerspective(perspectiveId, window);
            } catch (WorkbenchException e) {
                AdtPlugin.printErrorToConsole(e.getMessage());
            }
        }
    }
}

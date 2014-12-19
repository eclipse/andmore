/*
 * Copyright (C) 2011 The Android Open Source Project
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
package org.eclipse.andmore.internal.editors.layout.gle2;

import static com.android.SdkConstants.CLASS_VIEW;
import static com.android.SdkConstants.CLASS_VIEWGROUP;
import static com.android.SdkConstants.FN_FRAMEWORK_LIBRARY;

import com.android.utils.Pair;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.project.BaseProjectHelper;
import org.eclipse.andmore.internal.sdk.ProjectState;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.jdt.internal.core.ResolvedBinaryType;
import org.eclipse.jdt.internal.core.ResolvedSourceType;
import org.eclipse.swt.widgets.Display;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The {@link CustomViewFinder} can look up the custom views and third party views
 * available for a given project.
 */
@SuppressWarnings("restriction") // JDT model access for custom-view class lookup
public class CustomViewFinder {
    /**
     * Qualified name for the per-project non-persistent property storing the
     * {@link CustomViewFinder} for this project
     */
    private final static QualifiedName CUSTOM_VIEW_FINDER = new QualifiedName(AdtPlugin.PLUGIN_ID,
            "viewfinder"); //$NON-NLS-1$

    /** Project that this view finder locates views for */
    private final IProject mProject;

    private final List<Listener> mListeners = new ArrayList<Listener>();

    private List<String> mCustomViews;
    private List<String> mThirdPartyViews;
    private boolean mRefreshing;

    /**
     * Constructs an {@link CustomViewFinder} for the given project. Don't use this method;
     * use the {@link #get} factory method instead.
     *
     * @param project project to create an {@link CustomViewFinder} for
     */
    private CustomViewFinder(IProject project) {
        mProject = project;
    }

    /**
     * Returns the {@link CustomViewFinder} for the given project
     *
     * @param project the project the finder is associated with
     * @return a {@CustomViewFinder} for the given project, never null
     */
    public static CustomViewFinder get(IProject project) {
        CustomViewFinder finder = null;
        try {
            finder = (CustomViewFinder) project.getSessionProperty(CUSTOM_VIEW_FINDER);
        } catch (CoreException e) {
            // Not a problem; we will just create a new one
        }

        if (finder == null) {
            finder = new CustomViewFinder(project);
            try {
                project.setSessionProperty(CUSTOM_VIEW_FINDER, finder);
            } catch (CoreException e) {
                AdtPlugin.log(e, "Can't store CustomViewFinder");
            }
        }

        return finder;
    }

    public void refresh() {
        refresh(null /*listener*/, true /* sync */);
    }

    public void refresh(final Listener listener) {
        refresh(listener, false /* sync */);
    }

    private void refresh(final Listener listener, boolean sync) {
        // Add this listener to the list of listeners which should be notified when the
        // search is done. (There could be more than one since multiple requests could
        // arrive for a slow search since the search is run in a different thread).
        if (listener != null) {
            synchronized (this) {
                mListeners.add(listener);
            }
        }
        synchronized (this) {
            if (listener != null) {
                mListeners.add(listener);
            }
            if (mRefreshing) {
                return;
            }
            mRefreshing = true;
        }

        FindViewsJob job = new FindViewsJob();
        job.schedule();
        if (sync) {
            try {
                job.join();
            } catch (InterruptedException e) {
                AdtPlugin.log(e, null);
            }
        }
    }

    public Collection<String> getCustomViews() {
        return mCustomViews == null ? null : Collections.unmodifiableCollection(mCustomViews);
    }

    public Collection<String> getThirdPartyViews() {
        return mThirdPartyViews == null
            ? null : Collections.unmodifiableCollection(mThirdPartyViews);
    }

    public Collection<String> getAllViews() {
        // Not yet initialized: return null
        if (mCustomViews == null) {
            return null;
        }
        List<String> all = new ArrayList<String>(mCustomViews.size() + mThirdPartyViews.size());
        all.addAll(mCustomViews);
        all.addAll(mThirdPartyViews);
        return all;
    }

    /**
     * Returns a pair of view lists - the custom views and the 3rd-party views.
     * This method performs no caching; it is the same as asking the custom view finder
     * to refresh itself and then waiting for the answer and returning it.
     *
     * @param project the Android project
     * @param layoutsOnly if true, only search for layouts
     * @return a pair of lists, the first containing custom views and the second
     *         containing 3rd party views
     */
    public static Pair<List<String>,List<String>> findViews(
            final IProject project, boolean layoutsOnly) {
        CustomViewFinder finder = get(project);

        return finder.findViews(layoutsOnly);
    }

    private Pair<List<String>,List<String>> findViews(final boolean layoutsOnly) {
        final Set<String> customViews = new HashSet<String>();
        final Set<String> thirdPartyViews = new HashSet<String>();

        ProjectState state = Sdk.getProjectState(mProject);
        final List<IProject> libraries = state != null
            ? state.getFullLibraryProjects() : Collections.<IProject>emptyList();

        SearchRequestor requestor = new SearchRequestor() {
            @Override
            public void acceptSearchMatch(SearchMatch match) throws CoreException {
                // Ignore matches in comments
                if (match.isInsideDocComment()) {
                    return;
                }

                Object element = match.getElement();
                if (element instanceof ResolvedBinaryType) {
                    // Third party view
                    ResolvedBinaryType type = (ResolvedBinaryType) element;
                    IPackageFragment fragment = type.getPackageFragment();
                    IPath path = fragment.getPath();
                    String last = path.lastSegment();
                    // Filter out android.jar stuff
                    if (last.equals(FN_FRAMEWORK_LIBRARY)) {
                        return;
                    }
                    if (!isValidView(type, layoutsOnly)) {
                        return;
                    }

                    IProject matchProject = match.getResource().getProject();
                    if (mProject == matchProject || libraries.contains(matchProject)) {
                        String fqn = type.getFullyQualifiedName();
                        thirdPartyViews.add(fqn);
                    }
                } else if (element instanceof ResolvedSourceType) {
                    // User custom view
                    IProject matchProject = match.getResource().getProject();
                    if (mProject == matchProject || libraries.contains(matchProject)) {
                        ResolvedSourceType type = (ResolvedSourceType) element;
                        if (!isValidView(type, layoutsOnly)) {
                            return;
                        }
                        String fqn = type.getFullyQualifiedName();
                        fqn = fqn.replace('$', '.');
                        customViews.add(fqn);
                    }
                }
            }
        };
        try {
            IJavaProject javaProject = BaseProjectHelper.getJavaProject(mProject);
            if (javaProject != null) {
                String className = layoutsOnly ? CLASS_VIEWGROUP : CLASS_VIEW;
                IType viewType = javaProject.findType(className);
                if (viewType != null) {
                    IJavaSearchScope scope = SearchEngine.createHierarchyScope(viewType);
                    SearchParticipant[] participants = new SearchParticipant[] {
                        SearchEngine.getDefaultSearchParticipant()
                    };
                    int matchRule = SearchPattern.R_PATTERN_MATCH | SearchPattern.R_CASE_SENSITIVE;

                    SearchPattern pattern = SearchPattern.createPattern("*",
                            IJavaSearchConstants.CLASS, IJavaSearchConstants.IMPLEMENTORS,
                            matchRule);
                    SearchEngine engine = new SearchEngine();
                    engine.search(pattern, participants, scope, requestor,
                            new NullProgressMonitor());
                }
            }
        } catch (CoreException e) {
            AdtPlugin.log(e, null);
        }


        List<String> custom = new ArrayList<String>(customViews);
        List<String> thirdParty = new ArrayList<String>(thirdPartyViews);

        if (!layoutsOnly) {
            // Update our cached answers (unless we were filtered on only layouts)
            mCustomViews = custom;
            mThirdPartyViews = thirdParty;
        }

        return Pair.of(custom, thirdParty);
    }

    /**
     * Determines whether the given member is a valid android.view.View to be added to the
     * list of custom views or third party views. It checks that the view is public and
     * not abstract for example.
     */
    private static boolean isValidView(IType type, boolean layoutsOnly)
            throws JavaModelException {
        // Skip anonymous classes
        if (type.isAnonymous()) {
            return false;
        }
        int flags = type.getFlags();
        if (Flags.isAbstract(flags) || !Flags.isPublic(flags)) {
            return false;
        }

        // TODO: if (layoutsOnly) perhaps try to filter out AdapterViews and other ViewGroups
        // not willing to accept children via XML

        // See if the class has one of the acceptable constructors
        // needed for XML instantiation:
        //    View(Context context)
        //    View(Context context, AttributeSet attrs)
        //    View(Context context, AttributeSet attrs, int defStyle)
        // We don't simply do three direct checks via type.getMethod() because the types
        // are not resolved, so we don't know for each parameter if we will get the
        // fully qualified or the unqualified class names.
        // Instead, iterate over the methods and look for a match.
        String typeName = type.getElementName();
        for (IMethod method : type.getMethods()) {
            // Only care about constructors
            if (!method.getElementName().equals(typeName)) {
                continue;
            }

            String[] parameterTypes = method.getParameterTypes();
            if (parameterTypes == null || parameterTypes.length < 1 || parameterTypes.length > 3) {
                continue;
            }

            String first = parameterTypes[0];
            // Look for the parameter type signatures -- produced by
            // JDT's Signature.createTypeSignature("Context", false /*isResolved*/);.
            // This is not a typo; they were copy/pasted from the actual parameter names
            // observed in the debugger examining these data structures.
            if (first.equals("QContext;")                                   //$NON-NLS-1$
                    || first.equals("Qandroid.content.Context;")) {         //$NON-NLS-1$
                if (parameterTypes.length == 1) {
                    return true;
                }
                String second = parameterTypes[1];
                if (second.equals("QAttributeSet;")                         //$NON-NLS-1$
                        || second.equals("Qandroid.util.AttributeSet;")) {  //$NON-NLS-1$
                    if (parameterTypes.length == 2) {
                        return true;
                    }
                    String third = parameterTypes[2];
                    if (third.equals("I")) {                                //$NON-NLS-1$
                        if (parameterTypes.length == 3) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * Interface implemented by clients of the {@link CustomViewFinder} to be notified
     * when a custom view search has completed. Will always be called on the SWT event
     * dispatch thread.
     */
    public interface Listener {
        void viewsUpdated(Collection<String> customViews, Collection<String> thirdPartyViews);
    }

    /**
     * Job for performing class search off the UI thread. This is marked as a system job
     * so that it won't show up in the progress monitor etc.
     */
    private class FindViewsJob extends Job {
        FindViewsJob() {
            super("Find Custom Views");
            setSystem(true);
        }
        @Override
        protected IStatus run(IProgressMonitor monitor) {
            Pair<List<String>, List<String>> views = findViews(false);
            mCustomViews = views.getFirst();
            mThirdPartyViews = views.getSecond();

            // Notify listeners on SWT's UI thread
            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    Collection<String> customViews =
                        Collections.unmodifiableCollection(mCustomViews);
                    Collection<String> thirdPartyViews =
                        Collections.unmodifiableCollection(mThirdPartyViews);
                    synchronized (this) {
                        for (Listener l : mListeners) {
                            l.viewsUpdated(customViews, thirdPartyViews);
                        }
                        mListeners.clear();
                        mRefreshing = false;
                    }
                }
            });
            return Status.OK_STATUS;
        }
    }
}

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
package org.eclipse.andmore.internal.lint;

import static com.android.SdkConstants.DOT_JAVA;
import static com.android.SdkConstants.DOT_XML;

import com.android.tools.lint.detector.api.LintUtils;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.editors.IconFactory;
import org.eclipse.andmore.internal.preferences.LintPreferencePage;
import org.eclipse.andmore.internal.project.BaseProjectHelper;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.preference.IPreferenceNode;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.jface.preference.PreferenceNode;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.IDocumentProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Eclipse View which shows lint warnings for the current project
 */
public class LintViewPart extends ViewPart implements SelectionListener, IJobChangeListener {
    /** The view id for this view part */
    public static final String ID = "org.eclipse.andmore.internal.lint.LintViewPart"; //$NON-NLS-1$
    private static final String QUICKFIX_DISABLED_ICON = "quickfix-disabled";         //$NON-NLS-1$
    private static final String QUICKFIX_ICON = "quickfix";                           //$NON-NLS-1$
    private static final String REFRESH_ICON = "refresh";                             //$NON-NLS-1$
    private static final String EXPAND_DISABLED_ICON = "expandall-disabled";          //$NON-NLS-1$
    private static final String EXPAND_ICON = "expandall";                            //$NON-NLS-1$
    private static final String COLUMNS_ICON = "columns";                             //$NON-NLS-1$
    private static final String OPTIONS_ICON = "options";                             //$NON-NLS-1$
    private static final String IGNORE_THIS_ICON = "ignore-this";                     //$NON-NLS-1$
    private static final String IGNORE_THIS_DISABLED_ICON = "ignore-this-disabled";   //$NON-NLS-1$
    private static final String IGNORE_FILE_ICON = "ignore-file";                     //$NON-NLS-1$
    private static final String IGNORE_FILE_DISABLED_ICON = "ignore-file-disabled";   //$NON-NLS-1$
    private static final String IGNORE_PRJ_ICON = "ignore-project";                   //$NON-NLS-1$
    private static final String IGNORE_PRJ_DISABLED_ICON = "ignore-project-disabled"; //$NON-NLS-1$
    private static final String IGNORE_ALL_ICON = "ignore-all";                   //$NON-NLS-1$
    private static final String IGNORE_ALL_DISABLED_ICON = "ignore-all-disabled"; //$NON-NLS-1$
    private IMemento mMemento;
    private LintList mLintView;
    private Text mDetailsText;
    private Label mErrorLabel;
    private SashForm mSashForm;
    private Action mFixAction;
    private Action mRemoveAction;
    private Action mIgnoreAction;
    private Action mAlwaysIgnoreAction;
    private Action mIgnoreFileAction;
    private Action mIgnoreProjectAction;
    private Action mRemoveAllAction;
    private Action mRefreshAction;
    private Action mExpandAll;
    private Action mCollapseAll;
    private Action mConfigureColumns;
    private Action mOptions;

    /**
     * Initial projects to show: this field is only briefly not null during the
     * construction initiated by {@link #show(List)}
     */
    private static List<? extends IResource> sInitialResources;

    /**
     * Constructs a new {@link LintViewPart}
     */
    public LintViewPart() {
    }

    @Override
    public void init(IViewSite site, IMemento memento) throws PartInitException {
        super.init(site, memento);
        mMemento = memento;
    }

    @Override
    public void saveState(IMemento memento) {
        super.saveState(memento);

        mLintView.saveState(memento);
    }

    @Override
    public void dispose() {
        if (mLintView != null) {
            mLintView.dispose();
            mLintView = null;
        }
        super.dispose();
    }

    @Override
    public void createPartControl(Composite parent) {
        GridLayout gridLayout = new GridLayout(1, false);
        gridLayout.verticalSpacing = 0;
        gridLayout.marginWidth = 0;
        gridLayout.marginHeight = 0;
        parent.setLayout(gridLayout);

        mErrorLabel = new Label(parent, SWT.NONE);
        mErrorLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));

        mSashForm = new SashForm(parent, SWT.NONE);
        mSashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        mLintView = new LintList(getSite(), mSashForm, mMemento, false /*singleFile*/);

        mDetailsText = new Text(mSashForm,
                SWT.BORDER | SWT.READ_ONLY | SWT.WRAP | SWT.V_SCROLL | SWT.MULTI);
        Display display = parent.getDisplay();
        mDetailsText.setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
        mDetailsText.setForeground(display.getSystemColor(SWT.COLOR_INFO_FOREGROUND));

        mLintView.addSelectionListener(this);
        mSashForm.setWeights(new int[] {8, 2});

        createActions();
        initializeToolBar();

        // If there are currently running jobs, listen for them such that we can update the
        // button state
        refreshStopIcon();

        if (sInitialResources != null) {
            mLintView.setResources(sInitialResources);
            sInitialResources = null;
        } else {
            // No supplied context: show lint warnings for all projects
            IJavaProject[] androidProjects = BaseProjectHelper.getAndroidProjects(null);
            if (androidProjects.length > 0) {
                List<IResource> projects = new ArrayList<IResource>();
                for (IJavaProject project : androidProjects) {
                    projects.add(project.getProject());
                }
                mLintView.setResources(projects);
            }
        }

        updateIssueCount();
    }

    /**
     * Create the actions.
     */
    private void createActions() {
        ISharedImages sharedImages = PlatformUI.getWorkbench().getSharedImages();
        IconFactory iconFactory = IconFactory.getInstance();
        mFixAction = new LintViewAction("Fix", ACTION_FIX,
                iconFactory.getImageDescriptor(QUICKFIX_ICON),
                iconFactory.getImageDescriptor(QUICKFIX_DISABLED_ICON));

        mIgnoreAction = new LintViewAction("Suppress this error with an annotation/attribute",
                ACTION_IGNORE_THIS,
                iconFactory.getImageDescriptor(IGNORE_THIS_ICON),
                iconFactory.getImageDescriptor(IGNORE_THIS_DISABLED_ICON));
        mIgnoreFileAction = new LintViewAction("Ignore in this file", ACTION_IGNORE_FILE,
                iconFactory.getImageDescriptor(IGNORE_FILE_ICON),
                iconFactory.getImageDescriptor(IGNORE_FILE_DISABLED_ICON));
        mIgnoreProjectAction = new LintViewAction("Ignore in this project", ACTION_IGNORE_TYPE,
                iconFactory.getImageDescriptor(IGNORE_PRJ_ICON),
                iconFactory.getImageDescriptor(IGNORE_PRJ_DISABLED_ICON));
        mAlwaysIgnoreAction = new LintViewAction("Always Ignore", ACTION_IGNORE_ALL,
                iconFactory.getImageDescriptor(IGNORE_ALL_ICON),
                iconFactory.getImageDescriptor(IGNORE_ALL_DISABLED_ICON));

        mRemoveAction = new LintViewAction("Remove", ACTION_REMOVE,
                sharedImages.getImageDescriptor(ISharedImages.IMG_ELCL_REMOVE),
                sharedImages.getImageDescriptor(ISharedImages.IMG_ELCL_REMOVE_DISABLED));
        mRemoveAllAction = new LintViewAction("Remove All", ACTION_REMOVE_ALL,
                sharedImages.getImageDescriptor(ISharedImages.IMG_ELCL_REMOVEALL),
                sharedImages.getImageDescriptor(ISharedImages.IMG_ELCL_REMOVEALL_DISABLED));
        mRefreshAction = new LintViewAction("Refresh (& Save Files)", ACTION_REFRESH,
                iconFactory.getImageDescriptor(REFRESH_ICON), null);
        mRemoveAllAction.setEnabled(true);
        mCollapseAll = new LintViewAction("Collapse All", ACTION_COLLAPSE,
                sharedImages.getImageDescriptor(ISharedImages.IMG_ELCL_COLLAPSEALL),
                sharedImages.getImageDescriptor(ISharedImages.IMG_ELCL_COLLAPSEALL_DISABLED));
        mCollapseAll.setEnabled(true);
        mExpandAll = new LintViewAction("Expand All", ACTION_EXPAND,
                iconFactory.getImageDescriptor(EXPAND_ICON),
                iconFactory.getImageDescriptor(EXPAND_DISABLED_ICON));
        mExpandAll.setEnabled(true);

        mConfigureColumns = new LintViewAction("Configure Columns...", ACTION_COLUMNS,
                iconFactory.getImageDescriptor(COLUMNS_ICON),
                null);

        mOptions = new LintViewAction("Options...", ACTION_OPTIONS,
                iconFactory.getImageDescriptor(OPTIONS_ICON),
                null);

        enableActions(Collections.<IMarker>emptyList(), false /*updateWidgets*/);
    }

    /**
     * Initialize the toolbar.
     */
    private void initializeToolBar() {
        IToolBarManager toolbarManager = getViewSite().getActionBars().getToolBarManager();
        toolbarManager.add(mRefreshAction);
        toolbarManager.add(mFixAction);
        toolbarManager.add(mIgnoreAction);
        toolbarManager.add(mIgnoreFileAction);
        toolbarManager.add(mIgnoreProjectAction);
        toolbarManager.add(mAlwaysIgnoreAction);
        toolbarManager.add(new Separator());
        toolbarManager.add(mRemoveAction);
        toolbarManager.add(mRemoveAllAction);
        toolbarManager.add(new Separator());
        toolbarManager.add(mExpandAll);
        toolbarManager.add(mCollapseAll);
        toolbarManager.add(mConfigureColumns);
        toolbarManager.add(mOptions);
    }

    @Override
    public void setFocus() {
        mLintView.setFocus();
    }

    /**
     * Sets the resource associated with the lint view
     *
     * @param resources the associated resources
     */
    public void setResources(List<? extends IResource> resources) {
        mLintView.setResources(resources);

        // Refresh the stop/refresh icon status
        refreshStopIcon();
    }

    private void refreshStopIcon() {
        Job[] currentJobs = LintJob.getCurrentJobs();
        if (currentJobs.length > 0) {
            ISharedImages sharedImages = PlatformUI.getWorkbench().getSharedImages();
            mRefreshAction.setImageDescriptor(sharedImages.getImageDescriptor(
                    ISharedImages.IMG_ELCL_STOP));
            for (Job job : currentJobs) {
                job.addJobChangeListener(this);
            }
        } else {
            mRefreshAction.setImageDescriptor(
                    IconFactory.getInstance().getImageDescriptor(REFRESH_ICON));

        }
    }

    // ---- Implements SelectionListener ----

    @Override
    public void widgetSelected(SelectionEvent e) {
        List<IMarker> markers = mLintView.getSelectedMarkers();
        if (markers.size() != 1) {
            mDetailsText.setText(""); //$NON-NLS-1$
        } else {
            mDetailsText.setText(EclipseLintClient.describe(markers.get(0)));
        }

        IStatusLineManager status = getViewSite().getActionBars().getStatusLineManager();
        status.setMessage(mDetailsText.getText());

        updateIssueCount();

        enableActions(markers, true /* updateWidgets */);
    }

    private void enableActions(List<IMarker> markers, boolean updateWidgets) {
        // Update enabled state of actions
        boolean hasSelection = markers.size() > 0;
        boolean canFix = hasSelection;
        for (IMarker marker : markers) {
            if (!LintFix.hasFix(EclipseLintClient.getId(marker))) {
                canFix = false;
                break;
            }

            // Some fixes cannot be run in bulk
            if (markers.size() > 1) {
                List<LintFix> fixes = LintFix.getFixes(EclipseLintClient.getId(marker), marker);
                if (fixes == null || !fixes.get(0).isBulkCapable()) {
                    canFix = false;
                    break;
                }
            }
        }

        boolean haveFile = false;
        boolean isJavaOrXml = true;
        for (IMarker marker : markers) {
            IResource resource = marker.getResource();
            if (resource instanceof IFile || resource instanceof IFolder) {
                haveFile = true;
                String name = resource.getName();
                if (!LintUtils.endsWith(name, DOT_XML) && !LintUtils.endsWith(name, DOT_JAVA)) {
                    isJavaOrXml = false;
                }
                break;
            }
        }

        mFixAction.setEnabled(canFix);
        mIgnoreAction.setEnabled(hasSelection && haveFile && isJavaOrXml);
        mIgnoreFileAction.setEnabled(hasSelection && haveFile);
        mIgnoreProjectAction.setEnabled(hasSelection);
        mAlwaysIgnoreAction.setEnabled(hasSelection);
        mRemoveAction.setEnabled(hasSelection);

        if (updateWidgets) {
            getViewSite().getActionBars().getToolBarManager().update(false);
        }
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent e) {
        Object source = e.getSource();
        if (source == mLintView.getTreeViewer().getControl()) {
            // Jump to editor
            List<IMarker> selection = mLintView.getSelectedMarkers();
            if (selection.size() > 0) {
                EclipseLintClient.showMarker(selection.get(0));
            }
        }
    }

    // --- Implements IJobChangeListener ----

    @Override
    public void done(IJobChangeEvent event) {
        mRefreshAction.setImageDescriptor(
                IconFactory.getInstance().getImageDescriptor(REFRESH_ICON));

        if (!mLintView.isDisposed()) {
            mLintView.getDisplay().asyncExec(new Runnable()  {
                @Override
                public void run() {
                    if (!mLintView.isDisposed()) {
                        updateIssueCount();
                    }
                }
            });
        }
    }

    private void updateIssueCount() {
        int errors = mLintView.getErrorCount();
        int warnings = mLintView.getWarningCount();
        mErrorLabel.setText(String.format("%1$d errors, %2$d warnings", errors, warnings));
    }

    @Override
    public void aboutToRun(IJobChangeEvent event) {
    }

    @Override
    public void awake(IJobChangeEvent event) {
    }

    @Override
    public void running(IJobChangeEvent event) {
    }

    @Override
    public void scheduled(IJobChangeEvent event) {
    }

    @Override
    public void sleeping(IJobChangeEvent event) {
    }

    // ---- Actions ----

    private static final int ACTION_REFRESH = 1;
    private static final int ACTION_FIX = 2;
    private static final int ACTION_IGNORE_THIS = 3;
    private static final int ACTION_IGNORE_FILE = 4;
    private static final int ACTION_IGNORE_TYPE = 5;
    private static final int ACTION_IGNORE_ALL = 6;
    private static final int ACTION_REMOVE = 7;
    private static final int ACTION_REMOVE_ALL = 8;
    private static final int ACTION_COLLAPSE = 9;
    private static final int ACTION_EXPAND = 10;
    private static final int ACTION_COLUMNS = 11;
    private static final int ACTION_OPTIONS = 12;

    private class LintViewAction extends Action {

        private final int mAction;

        private LintViewAction(String label, int action,
                ImageDescriptor imageDesc, ImageDescriptor disabledImageDesc) {
            super(label);
            mAction = action;
            setImageDescriptor(imageDesc);
            if (disabledImageDesc != null) {
                setDisabledImageDescriptor(disabledImageDesc);
            }
        }

        @Override
        public void run() {
            switch (mAction) {
                case ACTION_REFRESH: {
                    IWorkbench workbench = PlatformUI.getWorkbench();
                    if (workbench != null) {
                        workbench.saveAllEditors(false /*confirm*/);
                    }

                    Job[] jobs = LintJob.getCurrentJobs();
                    if (jobs.length > 0) {
                        EclipseLintRunner.cancelCurrentJobs(false);
                    } else {
                        List<? extends IResource> resources = mLintView.getResources();
                        if (resources == null) {
                            return;
                        }
                        Job job = EclipseLintRunner.startLint(resources, null, null,
                                false /*fatalOnly*/, false /*show*/);
                        if (job != null && workbench != null) {
                            job.addJobChangeListener(LintViewPart.this);
                            ISharedImages sharedImages = workbench.getSharedImages();
                            setImageDescriptor(sharedImages.getImageDescriptor(
                                    ISharedImages.IMG_ELCL_STOP));
                        }
                    }
                    break;
                }
                case ACTION_FIX: {
                    List<IMarker> markers = mLintView.getSelectedMarkers();
                    for (IMarker marker : markers) {
                        List<LintFix> fixes = LintFix.getFixes(EclipseLintClient.getId(marker),
                                marker);
                        if (fixes == null) {
                            continue;
                        }
                        LintFix fix = fixes.get(0);
                        IResource resource = marker.getResource();
                        if (fix.needsFocus() && resource instanceof IFile) {
                            IRegion region = null;
                            try {
                                int start = marker.getAttribute(IMarker.CHAR_START, -1);
                                int end = marker.getAttribute(IMarker.CHAR_END, -1);
                                if (start != -1) {
                                    region = new Region(start, end - start);
                                }
                                AdtPlugin.openFile((IFile) resource, region);
                            } catch (PartInitException e) {
                                AdtPlugin.log(e, "Can't open file %1$s", resource);
                            }
                        }
                        IDocumentProvider provider = new TextFileDocumentProvider();
                        try {
                            provider.connect(resource);
                            IDocument document = provider.getDocument(resource);
                            if (document != null) {
                                fix.apply(document);
                                if (!fix.needsFocus()) {
                                    provider.saveDocument(new NullProgressMonitor(), resource,
                                            document,  true /*overwrite*/);
                                }
                            }
                        } catch (Exception e) {
                            AdtPlugin.log(e, "Did not find associated editor to apply fix: %1$s",
                                    resource.getName());
                        } finally {
                            provider.disconnect(resource);
                        }
                    }
                    break;
                }
                case ACTION_REMOVE: {
                    for (IMarker marker : mLintView.getSelectedMarkers()) {
                        try {
                            marker.delete();
                        } catch (CoreException e) {
                            AdtPlugin.log(e, null);
                        }
                    }
                    break;
                }
                case ACTION_REMOVE_ALL: {
                    List<? extends IResource> resources = mLintView.getResources();
                    if (resources != null) {
                        for (IResource resource : resources) {
                            EclipseLintClient.clearMarkers(resource);
                        }
                    }
                    break;
                }
                case ACTION_IGNORE_ALL:
                    assert false;
                    break;
                case ACTION_IGNORE_TYPE:
                case ACTION_IGNORE_FILE: {
                    boolean ignoreInFile = mAction == ACTION_IGNORE_FILE;
                    for (IMarker marker : mLintView.getSelectedMarkers()) {
                        String id = EclipseLintClient.getId(marker);
                        if (id != null) {
                            IResource resource = marker.getResource();
                            LintFixGenerator.suppressDetector(id, true,
                                    ignoreInFile ? resource : resource.getProject(),
                                    ignoreInFile);
                        }
                    }
                    break;
                }
                case ACTION_IGNORE_THIS: {
                    for (IMarker marker : mLintView.getSelectedMarkers()) {
                        LintFixGenerator.addSuppressAnnotation(marker);
                    }
                    break;
                }
                case ACTION_COLLAPSE: {
                    mLintView.collapseAll();
                    break;
                }
                case ACTION_EXPAND: {
                    mLintView.expandAll();
                    break;
                }
                case ACTION_COLUMNS: {
                    mLintView.configureColumns();
                    break;
                }
                case ACTION_OPTIONS: {
                    PreferenceManager manager = new PreferenceManager();

                    LintPreferencePage page = new LintPreferencePage();
                    String title = "Default/Global Settings";
                    page.setTitle(title);
                    IPreferenceNode node = new PreferenceNode(title, page);
                    manager.addToRoot(node);


                    List<? extends IResource> resources = mLintView.getResources();
                    if (resources != null) {
                        Set<IProject> projects = new HashSet<IProject>();
                        for (IResource resource : resources) {
                            projects.add(resource.getProject());
                        }
                        if (projects.size() > 0) {
                            for (IProject project : projects) {
                                page = new LintPreferencePage();
                                page.setTitle(String.format("Settings for %1$s",
                                        project.getName()));
                                page.setElement(project);
                                node = new PreferenceNode(project.getName(), page);
                                manager.addToRoot(node);
                            }
                        }
                    }

                    Shell shell = LintViewPart.this.getSite().getShell();
                    PreferenceDialog dialog = new PreferenceDialog(shell, manager);
                    dialog.create();
                    dialog.setSelectedNode(title);
                    dialog.open();
                    break;
                }
                default:
                    assert false : mAction;
            }
            updateIssueCount();
        }
    }

    /**
     * Shows or reconfigures the LintView to show the lint warnings for the
     * given project
     *
     * @param projects the projects to show lint warnings for
     */
    public static void show(List<? extends IResource> projects) {
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window != null) {
            IWorkbenchPage page = window.getActivePage();
            if (page != null) {
                try {
                    // Pass initial project context via static field read by constructor
                    sInitialResources = projects;
                    IViewPart view = page.showView(LintViewPart.ID, null,
                            IWorkbenchPage.VIEW_ACTIVATE);
                    if (sInitialResources != null && view instanceof LintViewPart) {
                        // The view must be showing already since the constructor was not
                        // run, so reconfigure the view instead
                        LintViewPart lintView = (LintViewPart) view;
                        lintView.setResources(projects);
                    }
                } catch (PartInitException e) {
                    AdtPlugin.log(e, "Cannot open Lint View");
                } finally {
                    sInitialResources = null;
                }
            }
        }
    }
}

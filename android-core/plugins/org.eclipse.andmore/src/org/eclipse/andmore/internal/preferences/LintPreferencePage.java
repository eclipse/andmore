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
package org.eclipse.andmore.internal.preferences;

import static com.android.tools.lint.detector.api.Issue.OutputFormat.RAW;
import static com.android.tools.lint.detector.api.Issue.OutputFormat.TEXT;

import com.android.annotations.NonNull;
import com.android.tools.lint.client.api.Configuration;
import com.android.tools.lint.client.api.IssueRegistry;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.Project;
import com.android.tools.lint.detector.api.Severity;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.AdtUtils;
import org.eclipse.andmore.internal.lint.EclipseLintClient;
import org.eclipse.andmore.internal.lint.EclipseLintRunner;
import org.eclipse.andmore.internal.project.BaseProjectHelper;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TreeNodeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.dialogs.PropertyPage;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Preference page for configuring Lint preferences */
public class LintPreferencePage extends PropertyPage implements IWorkbenchPreferencePage,
        SelectionListener, ControlListener, ModifyListener {
    private static final String ID =
            "org.eclipse.andmore.common.preferences.LintPreferencePage"; //$NON-NLS-1$
    private static final int ID_COLUMN_WIDTH = 150;

    private EclipseLintClient mClient;
    private IssueRegistry mRegistry;
    private Configuration mConfiguration;
    private IProject mProject;
    private Map<Issue, Severity> mSeverities = new HashMap<Issue, Severity>();
    private Map<Issue, Severity> mInitialSeverities = Collections.<Issue, Severity>emptyMap();
    private boolean mIgnoreEvent;

    private Tree mTree;
    private TreeViewer mTreeViewer;
    private Text mDetailsText;
    private Button mCheckFileCheckbox;
    private Button mCheckExportCheckbox;
    private Link mWorkspaceLink;
    private TreeColumn mNameColumn;
    private TreeColumn mIdColumn;
    private Combo mSeverityCombo;
    private Button mIncludeAll;
    private Button mIgnoreAll;
    private Text mSearch;

    /**
     * Create the preference page.
     */
    public LintPreferencePage() {
        setPreferenceStore(AdtPlugin.getDefault().getPreferenceStore());
    }

    @Override
    public Control createContents(Composite parent) {
        IAdaptable resource = getElement();
        if (resource != null) {
            mProject = (IProject) resource.getAdapter(IProject.class);
        }

        Composite container = new Composite(parent, SWT.NULL);
        container.setLayout(new GridLayout(2, false));

        if (mProject != null) {
            Label projectLabel = new Label(container, SWT.CHECK);
            projectLabel.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, false, false, 1,
                    1));
            projectLabel.setText("Project-specific configuration:");

            mWorkspaceLink = new Link(container, SWT.NONE);
            mWorkspaceLink.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
            mWorkspaceLink.setText("<a>Configure Workspace Settings...</a>");
            mWorkspaceLink.addSelectionListener(this);
        } else {
            mCheckFileCheckbox = new Button(container, SWT.CHECK);
            mCheckFileCheckbox.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false,
                    2, 1));
            mCheckFileCheckbox.setSelection(true);
            mCheckFileCheckbox.setText("When saving files, check for errors");

            mCheckExportCheckbox = new Button(container, SWT.CHECK);
            mCheckExportCheckbox.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false,
                    2, 1));
            mCheckExportCheckbox.setSelection(true);
            mCheckExportCheckbox.setText("Run full error check when exporting app and abort if fatal errors are found");

            Label separator = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
            separator.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));

            Label checkListLabel = new Label(container, SWT.NONE);
            checkListLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
            checkListLabel.setText("Issues:");
        }

        mRegistry = EclipseLintClient.getRegistry();
        mClient = new EclipseLintClient(mRegistry,
                mProject != null ? Collections.singletonList(mProject) : null, null, false);
        Project project = null;
        if (mProject != null) {
            File dir = AdtUtils.getAbsolutePath(mProject).toFile();
            project = mClient.getProject(dir, dir);
        }
        mConfiguration = mClient.getConfigurationFor(project);

        mSearch = new Text(container, SWT.SEARCH | SWT.ICON_CANCEL | SWT.ICON_SEARCH);
        mSearch.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        mSearch.addSelectionListener(this);
        mSearch.addModifyListener(this);
        // Grab the Enter key such that pressing return in the search box filters (instead
        // of closing the options dialog)
        mSearch.setMessage("type filter text (use ~ to filter by severity, e.g. ~ignore)");
        mSearch.addTraverseListener(new TraverseListener() {
            @Override
            public void keyTraversed(TraverseEvent e) {
                if (e.keyCode == SWT.CR) {
                    updateFilter();
                    e.doit = false;
                } else if (e.keyCode == SWT.ARROW_DOWN) {
                    // Allow moving from the search into the table
                    if (mTree.getItemCount() > 0) {
                        TreeItem firstCategory = mTree.getItem(0);
                        if (firstCategory.getItemCount() > 0) {
                            TreeItem first = firstCategory.getItem(0);
                            mTree.setFocus();
                            mTree.select(first);
                        }
                    }
                }
            }
        });

        mTreeViewer = new TreeViewer(container, SWT.BORDER | SWT.FULL_SELECTION);
        mTree = mTreeViewer.getTree();
        GridData gdTable = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
        gdTable.widthHint = 500;
        gdTable.heightHint = 160;
        mTree.setLayoutData(gdTable);
        mTree.setLinesVisible(true);
        mTree.setHeaderVisible(true);

        TreeViewerColumn column1 = new TreeViewerColumn(mTreeViewer, SWT.NONE);
        mIdColumn = column1.getColumn();
        mIdColumn.setWidth(100);
        mIdColumn.setText("Id");

        TreeViewerColumn column2 = new TreeViewerColumn(mTreeViewer, SWT.FILL);
        mNameColumn = column2.getColumn();
        mNameColumn.setWidth(100);
        mNameColumn.setText("Name");

        mTreeViewer.setContentProvider(new ContentProvider());
        mTreeViewer.setLabelProvider(new LabelProvider());

        mDetailsText = new Text(container, SWT.BORDER | SWT.READ_ONLY | SWT.WRAP |SWT.V_SCROLL
                | SWT.MULTI);
        GridData gdText = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 2);
        gdText.heightHint = 80;
        mDetailsText.setLayoutData(gdText);

        Label severityLabel = new Label(container, SWT.NONE);
        severityLabel.setText("Severity:");

        mSeverityCombo = new Combo(container, SWT.READ_ONLY);
        mSeverityCombo.setItems(new String[] {
                "(Default)", "Fatal", "Error", "Warning", "Information", "Ignore"
        });
        GridData gdSeverityCombo = new GridData(SWT.FILL, SWT.TOP, false, false, 1, 1);
        gdSeverityCombo.widthHint = 90;
        mSeverityCombo.setLayoutData(gdSeverityCombo);
        mSeverityCombo.setText("");
        mSeverityCombo.addSelectionListener(this);

        List<Issue> issues = mRegistry.getIssues();
        for (Issue issue : issues) {
            Severity severity = mConfiguration.getSeverity(issue);
            mSeverities.put(issue, severity);
        }
        mInitialSeverities = new HashMap<Issue, Severity>(mSeverities);

        mTreeViewer.setInput(mRegistry);

        mTree.addSelectionListener(this);
        // Add a listener to resize the column to the full width of the table
        mTree.addControlListener(this);

        loadSettings(false);

        mTreeViewer.expandAll();

        return container;
    }

    /**
     * Initialize the preference page.
     */
    @Override
    public void init(IWorkbench workbench) {
        // Initialize the preference page
    }

    @Override
    protected void contributeButtons(Composite parent) {
        super.contributeButtons(parent);

        // Add "Include All" button for quickly enabling all the detectors, including
        // those disabled by default
        mIncludeAll = new Button(parent, SWT.PUSH);
        mIncludeAll.setText("Include All");
        mIncludeAll.addSelectionListener(this);

        // Add "Ignore All" button for quickly disabling all the detectors
        mIgnoreAll = new Button(parent, SWT.PUSH);
        mIgnoreAll.setText("Ignore All");
        mIgnoreAll.addSelectionListener(this);

        // As per the contributeButton javadoc: increase parent's column count for each
        // added button
        ((GridLayout) parent.getLayout()).numColumns += 2;
    }

    @Override
    public void dispose() {
        super.dispose();
        cancelPendingSearch();
    }

    @Override
    protected void performDefaults() {
        super.performDefaults();

        mConfiguration.startBulkEditing();

        List<Issue> issues = mRegistry.getIssues();
        for (Issue issue : issues) {
            mConfiguration.setSeverity(issue, null);
        }

        mConfiguration.finishBulkEditing();

        loadSettings(true);
    }

    @Override
    public boolean performOk() {
        storeSettings();
        return true;
    }

    private void loadSettings(boolean refresh) {
        if (mCheckExportCheckbox != null) {
            AdtPrefs prefs = AdtPrefs.getPrefs();
            mCheckFileCheckbox.setSelection(prefs.isLintOnSave());
            mCheckExportCheckbox.setSelection(prefs.isLintOnExport());
        }

        mSeverities.clear();
        List<Issue> issues = mRegistry.getIssues();
        for (Issue issue : issues) {
            Severity severity = mConfiguration.getSeverity(issue);
            mSeverities.put(issue, severity);
        }

        if (refresh) {
            mTreeViewer.refresh();
        }
    }

    private void storeSettings() {
        // Lint on Save, Lint on Export
        if (mCheckExportCheckbox != null) {
            AdtPrefs prefs = AdtPrefs.getPrefs();
            prefs.setLintOnExport(mCheckExportCheckbox.getSelection());
            prefs.setLintOnSave(mCheckFileCheckbox.getSelection());
        }

        if (mConfiguration == null) {
            return;
        }

        mConfiguration.startBulkEditing();
        try {
            // Severities
            for (Map.Entry<Issue, Severity> entry : mSeverities.entrySet()) {
                Issue issue = entry.getKey();
                Severity severity = entry.getValue();
                if (mConfiguration.getSeverity(issue) != severity) {
                    if ((severity == issue.getDefaultSeverity()) && issue.isEnabledByDefault()) {
                        severity = null;
                    }
                    mConfiguration.setSeverity(issue, severity);
                }
            }
        } finally {
            mConfiguration.finishBulkEditing();
        }

        if (!mInitialSeverities.equals(mSeverities)) {
            // Ask user whether we should re-run the rules.
            MessageDialog dialog = new MessageDialog(
                    null, "Lint Settings Have Changed", null,
                    "The list of enabled checks has changed. Would you like to run lint now " +
                            "to update the results?",
                    MessageDialog.QUESTION,
                    new String[] {
                            "Yes", "No"
                    },
                    0); // yes is the default
            int result = dialog.open();
            if (result == 0) {
                // Run lint on all the open Android projects
                IWorkspace workspace = ResourcesPlugin.getWorkspace();
                IProject[] projects = workspace.getRoot().getProjects();
                List<IProject> androidProjects = new ArrayList<IProject>(projects.length);
                for (IProject project : projects) {
                    if (project.isOpen() && BaseProjectHelper.isAndroidProject(project)) {
                        androidProjects.add(project);
                    }
                }

                EclipseLintRunner.startLint(androidProjects, null,  null, false /*fatalOnly*/,
                        true /*show*/);
            }
        }
    }

    private void updateFilter() {
        cancelPendingSearch();
        if (!mSearch.isDisposed()) {
            // Clear selection before refiltering since otherwise it might be showing
            // items no longer available in the list.
            mTree.setSelection(new TreeItem[0]);
            mDetailsText.setText("");
            try {
                mIgnoreEvent = true;
                mSeverityCombo.setText("");
                mSeverityCombo.setEnabled(false);
            } finally {
                mIgnoreEvent = false;
            }

            mTreeViewer.getContentProvider().inputChanged(mTreeViewer, null, mRegistry);
            mTreeViewer.refresh();
            mTreeViewer.expandAll();
        }
    }

    private void cancelPendingSearch() {
        if (mPendingUpdate != null) {
            Shell shell = getShell();
            if (!shell.isDisposed()) {
                getShell().getDisplay().timerExec(-1, mPendingUpdate);
            }
            mPendingUpdate = null;
        }
    }

    private Runnable mPendingUpdate;

    private void scheduleSearch() {
        if (mPendingUpdate == null) {
            mPendingUpdate = new Runnable() {
                @Override
                public void run() {
                    mPendingUpdate = null;
                    updateFilter();
                }
            };
        }
        getShell().getDisplay().timerExec(250 /*ms*/, mPendingUpdate);
    }

    // ---- Implements SelectionListener ----

    @Override
    public void widgetSelected(SelectionEvent e) {
        if (mIgnoreEvent) {
            return;
        }

        Object source = e.getSource();
        if (source == mTree) {
            TreeItem item = (TreeItem) e.item;
            Object data = item != null ? item.getData() : null;
            if (data instanceof Issue) {
                Issue issue = (Issue) data;
                String summary = issue.getDescription(Issue.OutputFormat.TEXT);
                String explanation = issue.getExplanation(Issue.OutputFormat.TEXT);

                StringBuilder sb = new StringBuilder(summary.length() + explanation.length() + 20);
                sb.append(summary);
                sb.append('\n').append('\n');
                sb.append(explanation);
                mDetailsText.setText(sb.toString());
                try {
                    mIgnoreEvent = true;
                    Severity severity = getSeverity(issue);
                    mSeverityCombo.select(severity.ordinal() + 1); // Skip the default option
                    mSeverityCombo.setEnabled(true);
                } finally {
                    mIgnoreEvent = false;
                }
            } else {
                mDetailsText.setText("");
                try {
                    mIgnoreEvent = true;
                    mSeverityCombo.setText("");
                    mSeverityCombo.setEnabled(false);
                } finally {
                    mIgnoreEvent = false;
                }
            }
        } else if (source == mWorkspaceLink) {
            int result = PreferencesUtil.createPreferenceDialogOn(getShell(), ID,
                    new String[] { ID }, null).open();
            if (result == Window.OK) {
                loadSettings(true);
            }
        } else if (source == mSeverityCombo) {
            int index = mSeverityCombo.getSelectionIndex();
            Issue issue = (Issue) mTree.getSelection()[0].getData();
            Severity severity;
            if (index == -1 || index == 0) {
                // "(Default)"
                severity = issue.getDefaultSeverity();
            } else {
                // -1: Skip the "(Default)"
                severity = Severity.values()[index - 1];
            }
            mSeverities.put(issue, severity);
            mTreeViewer.refresh();
        } else if (source == mIncludeAll) {
            List<Issue> issues = mRegistry.getIssues();
            for (Issue issue : issues) {
                // The default severity is never ignore; for disabled-by-default
                // issues the {@link Issue#isEnabledByDefault()} method is false instead
                mSeverities.put(issue, issue.getDefaultSeverity());
            }
            mTreeViewer.refresh();
        } else if (source == mIgnoreAll) {
            List<Issue> issues = mRegistry.getIssues();
            for (Issue issue : issues) {
                mSeverities.put(issue, Severity.IGNORE);
            }
            mTreeViewer.refresh();
        } else if (source == mSearch) {
            updateFilter();
        }
    }

    private Severity getSeverity(Issue issue) {
        Severity severity = mSeverities.get(issue);
        if (severity != null) {
            return severity;
        }

        return mConfiguration.getSeverity(issue);
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent e) {
        Object source = e.getSource();
        if (source == mTree) {
            widgetSelected(e);
        } else if (source == mSearch) {
            if (e.detail == SWT.CANCEL) {
                // Cancel the search
                mSearch.setText("");
            }
            updateFilter();
        }
    }

    // ---- Implements ModifyListener ----
    @Override
    public void modifyText(ModifyEvent e) {
        if (e.getSource() == mSearch) {
            scheduleSearch();
        }
    }

    // ---- Implements ControlListener ----

    @Override
    public void controlMoved(ControlEvent e) {
    }

    @Override
    public void controlResized(ControlEvent e) {
        Rectangle r = mTree.getClientArea();
        int availableWidth = r.width;

        mIdColumn.setWidth(ID_COLUMN_WIDTH);
        availableWidth -= ID_COLUMN_WIDTH;

        // Name absorbs everything else
        mNameColumn.setWidth(availableWidth);
    }

    private boolean filterMatches(@NonNull String filter, @NonNull Issue issue) {
        return (filter.startsWith("~") //$NON-NLS-1$
                        && mConfiguration.getSeverity(issue).getDescription()
                            .toLowerCase(Locale.US).startsWith(filter.substring(1)))
                || issue.getCategory().getName().toLowerCase(Locale.US).startsWith(filter)
                || issue.getCategory().getFullName().toLowerCase(Locale.US).startsWith(filter)
                || issue.getId().toLowerCase(Locale.US).contains(filter)
                || issue.getDescription(RAW).toLowerCase(Locale.US).contains(filter);
    }

    private class ContentProvider extends TreeNodeContentProvider {
        private Map<Category, List<Issue>> mCategoryToIssues;
        private Object[] mElements;

        @Override
        public Object[] getElements(Object inputElement) {
            return mElements;
        }

        @Override
        public boolean hasChildren(Object element) {
            return element instanceof Category;
        }

        @Override
        public Object[] getChildren(Object parentElement) {
            assert mCategoryToIssues != null;
            List<Issue> list = mCategoryToIssues.get(parentElement);
            if (list == null) {
                return new Object[0];
            }  else {
                return list.toArray();
            }
        }

        @Override
        public Object getParent(Object element) {
            return null;
        }

        @Override
        public void inputChanged(final Viewer viewer, final Object oldInput,
                final Object newInput) {
            mCategoryToIssues = null;

            String filter = mSearch.isDisposed() ? "" : mSearch.getText().trim();
            if (filter.length() == 0) {
                filter = null;
            } else {
                filter = filter.toLowerCase(Locale.US);
            }

            mCategoryToIssues = new HashMap<Category, List<Issue>>();
            List<Issue> issues = mRegistry.getIssues();
            for (Issue issue : issues) {
                if (filter == null || filterMatches(filter, issue)) {
                    List<Issue> list = mCategoryToIssues.get(issue.getCategory());
                    if (list == null) {
                        list = new ArrayList<Issue>();
                        mCategoryToIssues.put(issue.getCategory(), list);
                    }
                    list.add(issue);
                }
            }

            if (filter == null) {
                // Not filtering: show all categories
                mElements = mRegistry.getCategories().toArray();
            } else {
                // Filtering: only include categories that contain matches
                if (mCategoryToIssues == null) {
                    getChildren(null);
                }

                // Preserve the current category order, so instead of
                // just creating a list of the mCategoryToIssues keyset, add them
                // in the order they appear in in the registry
                List<Category> categories = new ArrayList<Category>(mCategoryToIssues.size());
                for (Category category : mRegistry.getCategories()) {
                    if (mCategoryToIssues.containsKey(category)) {
                        categories.add(category);
                    }
                }
                mElements = categories.toArray();
            }
        }
    }

    private class LabelProvider implements ITableLabelProvider, IColorProvider {

        @Override
        public void addListener(ILabelProviderListener listener) {
        }

        @Override
        public void dispose() {
        }

        @Override
        public boolean isLabelProperty(Object element, String property) {
            return true;
        }

        @Override
        public void removeListener(ILabelProviderListener listener) {
        }

        @Override
        public Image getColumnImage(Object element, int columnIndex) {
            if (element instanceof Category) {
                return null;
            }

            if (columnIndex == 1) {
                Issue issue = (Issue) element;
                Severity severity = mSeverities.get(issue);
                if (severity == null) {
                    return null;
                }

                ISharedImages sharedImages = PlatformUI.getWorkbench().getSharedImages();
                switch (severity) {
                    case FATAL:
                    case ERROR:
                        return sharedImages.getImage(ISharedImages.IMG_OBJS_ERROR_TSK);
                    case WARNING:
                        return sharedImages.getImage(ISharedImages.IMG_OBJS_WARN_TSK);
                    case INFORMATIONAL:
                        return sharedImages.getImage(ISharedImages.IMG_OBJS_INFO_TSK);
                    case IGNORE:
                        return sharedImages.getImage(ISharedImages.IMG_ELCL_REMOVE_DISABLED);
                }
            }
            return null;
        }

        @Override
        public String getColumnText(Object element, int columnIndex) {
            if (element instanceof Category) {
                if (columnIndex == 0) {
                    return ((Category) element).getFullName();
                } else {
                    return null;
                }
            }

            Issue issue = (Issue) element;
            switch (columnIndex) {
                case 0:
                    return issue.getId();
                case 1:
                    return issue.getDescription(TEXT);
            }

            return null;
        }

        // ---- IColorProvider ----

        @Override
        public Color getForeground(Object element) {
            if (element instanceof Category) {
                return mTree.getDisplay().getSystemColor(SWT.COLOR_INFO_FOREGROUND);
            }

            if (element instanceof Issue) {
                Issue issue = (Issue) element;
                Severity severity = mSeverities.get(issue);
                if (severity == Severity.IGNORE) {
                    return mTree.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY);
                }
            }

            return null;
        }

        @Override
        public Color getBackground(Object element) {
            if (element instanceof Category) {
                return mTree.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND);
            }
            return null;
        }
    }
}

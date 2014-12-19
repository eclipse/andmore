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

import com.android.tools.lint.client.api.Configuration;
import com.android.tools.lint.client.api.IssueRegistry;
import com.android.tools.lint.client.api.LintClient;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.Severity;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import org.eclipse.andmore.AdtConstants;
import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.AdtUtils;
import org.eclipse.andmore.internal.editors.layout.LayoutEditorDelegate;
import org.eclipse.andmore.internal.editors.layout.gle2.GraphicalEditorPart;
import org.eclipse.andmore.internal.editors.layout.gle2.LayoutActionBar;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TreeNodeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TreeEvent;
import org.eclipse.swt.events.TreeListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;
import org.eclipse.ui.progress.WorkbenchJob;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A tree-table widget which shows a list of lint warnings for an underlying
 * {@link IResource} such as a file, a project, or a list of projects.
 */
class LintList extends Composite implements IResourceChangeListener, ControlListener {
    private static final Object UPDATE_MARKERS_FAMILY = new Object();

    // For persistence:
    private static final String KEY_WIDTHS = "lintColWidth"; //$NON-NLS-1$
    private static final String KEY_VISIBLE = "lintColVisible"; //$NON-NLS-1$
    // Mapping SWT TreeColumns to LintColumns
    private static final String KEY_COLUMN = "lintColumn"; //$NON-NLS-1$

    private final IWorkbenchPartSite mSite;
    private final TreeViewer mTreeViewer;
    private final Tree mTree;
    private Set<String> mExpandedIds;
    private ContentProvider mContentProvider;
    private String mSelectedId;
    private List<? extends IResource> mResources;
    private Configuration mConfiguration;
    private final boolean mSingleFile;
    private int mErrorCount;
    private int mWarningCount;
    private final UpdateMarkersJob mUpdateMarkersJob = new UpdateMarkersJob();
    private final IssueRegistry mRegistry;
    private final IMemento mMemento;
    private final LintColumn mMessageColumn = new LintColumn.MessageColumn(this);
    private final LintColumn mLineColumn = new LintColumn.LineColumn(this);
    private final LintColumn[] mColumns = new LintColumn[] {
            mMessageColumn,
            new LintColumn.PriorityColumn(this),
            new LintColumn.CategoryColumn(this),
            new LintColumn.LocationColumn(this),
            new LintColumn.FileColumn(this),
            new LintColumn.PathColumn(this),
            mLineColumn
    };
    private LintColumn[] mVisibleColumns;

    LintList(IWorkbenchPartSite site, Composite parent, IMemento memento, boolean singleFile) {
        super(parent, SWT.NONE);
        mSingleFile = singleFile;
        mMemento = memento;
        mSite = site;
        mRegistry = EclipseLintClient.getRegistry();

        GridLayout gridLayout = new GridLayout(1, false);
        gridLayout.marginWidth = 0;
        gridLayout.marginHeight = 0;
        setLayout(gridLayout);

        mTreeViewer = new TreeViewer(this, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
        mTree = mTreeViewer.getTree();
        mTree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

        createColumns();
        mTreeViewer.setComparator(new TableComparator());
        setSortIndicators();

        mContentProvider = new ContentProvider();
        mTreeViewer.setContentProvider(mContentProvider);

        mTree.setLinesVisible(true);
        mTree.setHeaderVisible(true);
        mTree.addControlListener(this);

        ResourcesPlugin.getWorkspace().addResourceChangeListener(
                this,
                IResourceChangeEvent.POST_CHANGE
                        | IResourceChangeEvent.PRE_BUILD
                        | IResourceChangeEvent.POST_BUILD);

        // Workaround for https://bugs.eclipse.org/341865
        mTree.addPaintListener(new PaintListener() {
            @Override
            public void paintControl(PaintEvent e) {
                mTreePainted = true;
                mTreeViewer.getTree().removePaintListener(this);
            }
        });

        // Remember the most recently selected id category such that we can
        // attempt to reselect it after a refresh
        mTree.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                List<IMarker> markers = getSelectedMarkers();
                if (markers.size() > 0) {
                    mSelectedId = EclipseLintClient.getId(markers.get(0));
                }
            }
        });
        mTree.addTreeListener(new TreeListener() {
            @Override
            public void treeExpanded(TreeEvent e) {
                Object data = e.item.getData();
                if (data instanceof IMarker) {
                    String id = EclipseLintClient.getId((IMarker) data);
                    if (id != null) {
                        if (mExpandedIds == null) {
                            mExpandedIds = new HashSet<String>();
                        }
                        mExpandedIds.add(id);
                    }
                }
            }

            @Override
            public void treeCollapsed(TreeEvent e) {
                if (mExpandedIds != null) {
                    Object data = e.item.getData();
                    if (data instanceof IMarker) {
                        String id = EclipseLintClient.getId((IMarker) data);
                        if (id != null) {
                            mExpandedIds.remove(id);
                        }
                    }
                }
            }
        });
    }

    private boolean mTreePainted;

    private void updateColumnWidths() {
        Rectangle r = mTree.getClientArea();
        int availableWidth = r.width;
        // Add all available size to the first column
        for (int i = 1; i < mTree.getColumnCount(); i++) {
            TreeColumn column = mTree.getColumn(i);
            availableWidth -= column.getWidth();
        }
        if (availableWidth > 100) {
            mTree.getColumn(0).setWidth(availableWidth);
        }
    }

    public void setResources(List<? extends IResource> resources) {
        mResources = resources;

        mConfiguration = null;
        for (IResource resource : mResources) {
            IProject project = resource.getProject();
            if (project != null) {
                // For logging only
                LintClient client = new EclipseLintClient(null, null, null, false);
                mConfiguration = ProjectLintConfiguration.get(client, project, false);
                break;
            }
        }
        if (mConfiguration == null) {
            mConfiguration = GlobalLintConfiguration.get();
        }

        List<IMarker> markerList = getMarkers();
        mTreeViewer.setInput(markerList);
        if (mSingleFile) {
            expandAll();
        }

        // Selecting the first item isn't a good idea since it may not be the first
        // item shown in the table (since it does its own sorting), and furthermore we
        // may not have all the data yet; this is called when scanning begins, not when
        // it's done:
        //if (mTree.getItemCount() > 0) {
        //    mTree.select(mTree.getItem(0));
        //}

        updateColumnWidths(); // in case mSingleFile changed
    }

    /** Select the first item */
    public void selectFirst() {
        if (mTree.getItemCount() > 0) {
            mTree.select(mTree.getItem(0));
        }
    }

    private List<IMarker> getMarkers() {
        mErrorCount = mWarningCount = 0;
        List<IMarker> markerList = new ArrayList<IMarker>();
        if (mResources != null) {
            for (IResource resource : mResources) {
                IMarker[] markers = EclipseLintClient.getMarkers(resource);
                for (IMarker marker : markers) {
                    markerList.add(marker);
                    int severity = marker.getAttribute(IMarker.SEVERITY, 0);
                    if (severity == IMarker.SEVERITY_ERROR) {
                        mErrorCount++;
                    } else if (severity == IMarker.SEVERITY_WARNING) {
                        mWarningCount++;
                    }
                }
            }

            // No need to sort the marker list here; it will be sorted by the tree table model
        }
        return markerList;
    }

    public int getErrorCount() {
        return mErrorCount;
    }

    public int getWarningCount() {
        return mWarningCount;
    }

    @Override
    protected void checkSubclass() {
        // Disable the check that prevents subclassing of SWT components
    }

    public void addSelectionListener(SelectionListener listener) {
        mTree.addSelectionListener(listener);
    }

    public void refresh() {
        mTreeViewer.refresh();
    }

    public List<IMarker> getSelectedMarkers() {
        TreeItem[] selection = mTree.getSelection();
        List<IMarker> markers = new ArrayList<IMarker>(selection.length);
        for (TreeItem item : selection) {
            Object data = item.getData();
            if (data instanceof IMarker) {
                markers.add((IMarker) data);
            }
        }

        return markers;
    }

    @Override
    public void dispose() {
        cancelJobs();
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
        super.dispose();
    }

    private class ContentProvider extends TreeNodeContentProvider {
        private Map<Object, Object[]> mChildren;
        private Map<IMarker, Integer> mTypeCount;
        private IMarker[] mTopLevels;

        @Override
        public Object[] getElements(Object inputElement) {
            if (inputElement == null) {
                mTypeCount = null;
                return new IMarker[0];
            }

            @SuppressWarnings("unchecked")
            List<IMarker> list = (List<IMarker>) inputElement;

            // Partition the children such that at the top level we have one
            // marker of each type, and below we have all the duplicates of
            // each one of those errors. And for errors with multiple locations,
            // there is a third level.
            Multimap<String, IMarker> types = ArrayListMultimap.<String, IMarker>create(100, 20);
            for (IMarker marker : list) {
                String id = EclipseLintClient.getId(marker);
                types.put(id, marker);
            }

            Set<String> ids = types.keySet();

            mChildren = new HashMap<Object, Object[]>(ids.size());
            mTypeCount = new HashMap<IMarker, Integer>(ids.size());

            List<IMarker> topLevel = new ArrayList<IMarker>(ids.size());
            for (String id : ids) {
                Collection<IMarker> markers = types.get(id);
                int childCount = markers.size();

                // Must sort the list items in order to have a stable first item
                // (otherwise preserving expanded paths etc won't work)
                TableComparator sorter = getTableSorter();
                IMarker[] array = markers.toArray(new IMarker[markers.size()]);
                sorter.sort(mTreeViewer, array);

                IMarker topMarker = array[0];
                mTypeCount.put(topMarker, childCount);
                topLevel.add(topMarker);

                IMarker[] children = Arrays.copyOfRange(array, 1, array.length);
                mChildren.put(topMarker, children);
            }

            mTopLevels = topLevel.toArray(new IMarker[topLevel.size()]);
            return mTopLevels;
        }

        @Override
        public boolean hasChildren(Object element) {
            Object[] children = mChildren != null ? mChildren.get(element) : null;
            return children != null && children.length > 0;
        }

        @Override
        public Object[] getChildren(Object parentElement) {
            Object[] children = mChildren.get(parentElement);
            if (children != null) {
                return children;
            }

            return new Object[0];
        }

        @Override
        public Object getParent(Object element) {
            return null;
        }

        public int getCount(IMarker marker) {
            if (mTypeCount != null) {
                Integer count = mTypeCount.get(marker);
                if (count != null) {
                    return count.intValue();
                }
            }

            return -1;
        }

        IMarker[] getTopMarkers() {
            return mTopLevels;
        }
    }

    private class LintColumnLabelProvider extends StyledCellLabelProvider {
        private LintColumn mColumn;

        LintColumnLabelProvider(LintColumn column) {
            mColumn = column;
        }

        @Override
        public void update(ViewerCell cell) {
            Object element = cell.getElement();
            cell.setImage(mColumn.getImage((IMarker) element));
            StyledString styledString = mColumn.getStyledValue((IMarker) element);
            if (styledString == null) {
                cell.setText(mColumn.getValue((IMarker) element));
                cell.setStyleRanges(null);
            } else {
                cell.setText(styledString.toString());
                cell.setStyleRanges(styledString.getStyleRanges());
            }
            super.update(cell);
        }
    }

    TreeViewer getTreeViewer() {
        return mTreeViewer;
    }

    Tree getTree() {
        return mTree;
    }

    // ---- Implements IResourceChangeListener ----

    @Override
    public void resourceChanged(IResourceChangeEvent event) {
        if (mResources == null) {
            return;
        }
        IMarkerDelta[] deltas = event.findMarkerDeltas(AdtConstants.MARKER_LINT, true);
        if (deltas.length > 0) {
            // Update immediately for POST_BUILD events, otherwise do an unconditional
            // update after 30 seconds. This matches the logic in Eclipse's ProblemView
            // (see the MarkerView class).
            if (event.getType() == IResourceChangeEvent.POST_BUILD) {
                cancelJobs();
                getProgressService().schedule(mUpdateMarkersJob, 100);
            } else {
                IWorkbenchSiteProgressService progressService = getProgressService();
                if (progressService == null) {
                    mUpdateMarkersJob.schedule(30000);
                } else {
                    getProgressService().schedule(mUpdateMarkersJob, 30000);
                }
            }
        }
    }

    // ---- Implements ControlListener ----

    @Override
    public void controlMoved(ControlEvent e) {
    }

    @Override
    public void controlResized(ControlEvent e) {
        updateColumnWidths();
    }

    // ---- Updating Markers ----

    private void cancelJobs() {
        mUpdateMarkersJob.cancel();
    }

    protected IWorkbenchSiteProgressService getProgressService() {
        if (mSite != null) {
            Object siteService = mSite.getAdapter(IWorkbenchSiteProgressService.class);
            if (siteService != null) {
                return (IWorkbenchSiteProgressService) siteService;
            }
        }
        return null;
    }

    private class UpdateMarkersJob extends WorkbenchJob {
        UpdateMarkersJob() {
            super("Updating Lint Markers");
            setSystem(true);
        }

        @Override
        public IStatus runInUIThread(IProgressMonitor monitor) {
            if (mTree.isDisposed()) {
                return Status.CANCEL_STATUS;
            }

            mTreeViewer.setInput(null);
            List<IMarker> markerList = getMarkers();
            if (markerList.size() == 0) {
                LayoutEditorDelegate delegate =
                    LayoutEditorDelegate.fromEditor(AdtUtils.getActiveEditor());
                if (delegate != null) {
                    GraphicalEditorPart g = delegate.getGraphicalEditor();
                    assert g != null;
                    LayoutActionBar bar = g == null ? null : g.getLayoutActionBar();
                    assert bar != null;
                    if (bar != null) {
                        bar.updateErrorIndicator();
                    }
                }
            }
            // Trigger selection update
            Event updateEvent = new Event();
            updateEvent.widget = mTree;
            mTree.notifyListeners(SWT.Selection, updateEvent);
            mTreeViewer.setInput(markerList);
            mTreeViewer.refresh();

            if (mExpandedIds != null) {
                List<IMarker> expanded = new ArrayList<IMarker>(mExpandedIds.size());
                IMarker[] topMarkers = mContentProvider.getTopMarkers();
                if (topMarkers != null) {
                    for (IMarker marker : topMarkers) {
                        String id = EclipseLintClient.getId(marker);
                        if (id != null && mExpandedIds.contains(id)) {
                            expanded.add(marker);
                        }
                    }
                }
                if (!expanded.isEmpty()) {
                    mTreeViewer.setExpandedElements(expanded.toArray());
                }
            }

            if (mSelectedId != null) {
                IMarker[] topMarkers = mContentProvider.getTopMarkers();
                for (IMarker marker : topMarkers) {
                    if (mSelectedId.equals(EclipseLintClient.getId(marker))) {
                        mTreeViewer.setSelection(new StructuredSelection(marker), true /*reveal*/);
                        break;
                    }
                }
            }

            return Status.OK_STATUS;
        }

        @Override
        public boolean shouldRun() {
            // Do not run if the change came in before there is a viewer
            return PlatformUI.isWorkbenchRunning();
        }

        @Override
        public boolean belongsTo(Object family) {
            return UPDATE_MARKERS_FAMILY == family;
        }
    }

    /**
     * Returns the list of resources being shown in the list
     *
     * @return the list of resources being shown in this composite
     */
    public List<? extends IResource> getResources() {
        return mResources;
    }

    /** Expands all nodes */
    public void expandAll() {
        mTreeViewer.expandAll();

        if (mExpandedIds == null) {
            mExpandedIds = new HashSet<String>();
        }
        IMarker[] topMarkers = mContentProvider.getTopMarkers();
        if (topMarkers != null) {
            for (IMarker marker : topMarkers) {
                String id = EclipseLintClient.getId(marker);
                if (id != null) {
                    mExpandedIds.add(id);
                }
            }
        }
    }

    /** Collapses all nodes */
    public void collapseAll() {
        mTreeViewer.collapseAll();
        mExpandedIds = null;
    }

    // ---- Column Persistence ----

    public void saveState(IMemento memento) {
        if (mSingleFile) {
            // Don't use persistence for single-file lists: this is a special mode of the
            // window where we show a hardcoded set of columns for a single file, deliberately
            // omitting the location column etc
            return;
        }

        IMemento columnEntry = memento.createChild(KEY_WIDTHS);
        LintColumn[] columns = new LintColumn[mTree.getColumnCount()];
        int[] positions = mTree.getColumnOrder();
        for (int i = 0; i < columns.length; i++) {
            TreeColumn treeColumn = mTree.getColumn(i);
            LintColumn column = (LintColumn) treeColumn.getData(KEY_COLUMN);
            // Workaround for TeeColumn.getWidth() returning 0 in some cases,
            // see https://bugs.eclipse.org/341865 for details.
            int width = getColumnWidth(column, mTreePainted);
            columnEntry.putInteger(getKey(treeColumn), width);
            columns[positions[i]] = column;
        }

        if (getVisibleColumns() != null) {
            IMemento visibleEntry = memento.createChild(KEY_VISIBLE);
            for (LintColumn column : getVisibleColumns()) {
                visibleEntry.putBoolean(getKey(column), true);
            }
        }
    }

    private void createColumns() {
        LintColumn[] columns = getVisibleColumns();
        TableLayout layout = new TableLayout();

        for (int i = 0; i < columns.length; i++) {
            LintColumn column = columns[i];
            TreeViewerColumn viewerColumn = null;
            TreeColumn treeColumn;
            viewerColumn = new TreeViewerColumn(mTreeViewer, SWT.NONE);
            treeColumn = viewerColumn.getColumn();
            treeColumn.setData(KEY_COLUMN, column);
            treeColumn.setResizable(true);
            treeColumn.addSelectionListener(getHeaderListener());
            if (!column.isLeftAligned()) {
                treeColumn.setAlignment(SWT.RIGHT);
            }
            viewerColumn.setLabelProvider(new LintColumnLabelProvider(column));
            treeColumn.setText(column.getColumnHeaderText());
            treeColumn.setImage(column.getColumnHeaderImage());
            IMemento columnWidths = null;
            if (mMemento != null && !mSingleFile) {
                columnWidths = mMemento.getChild(KEY_WIDTHS);
            }
            int columnWidth = getColumnWidth(column, false);
            if (columnWidths != null) {
                columnWidths.putInteger(getKey(column), columnWidth);
            }
            if (i == 0) {
                // The first column should use layout -weights- to get all the
                // remaining room
                layout.addColumnData(new ColumnWeightData(1, true));
            } else if (columnWidth < 0) {
                int defaultColumnWidth = column.getPreferredWidth();
                layout.addColumnData(new ColumnPixelData(defaultColumnWidth, true, true));
            } else {
                layout.addColumnData(new ColumnPixelData(columnWidth, true));
            }
        }
        mTreeViewer.getTree().setLayout(layout);
        mTree.layout(true);
    }

    private int getColumnWidth(LintColumn column, boolean getFromUi) {
        Tree tree = mTreeViewer.getTree();
        if (getFromUi) {
            TreeColumn[] columns = tree.getColumns();
            for (int i = 0; i < columns.length; i++) {
                if (column.equals(columns[i].getData(KEY_COLUMN))) {
                    return columns[i].getWidth();
                }
            }
        }
        int preferredWidth = -1;
        if (mMemento != null && !mSingleFile) {
            IMemento columnWidths = mMemento.getChild(KEY_WIDTHS);
            if (columnWidths != null) {
                Integer value = columnWidths.getInteger(getKey(column));
                // Make sure we get a useful value
                if (value != null && value.intValue() >= 0)
                    preferredWidth = value.intValue();
            }
        }
        if (preferredWidth <= 0) {
            preferredWidth = Math.max(column.getPreferredWidth(), 30);
        }
        return preferredWidth;
    }

    private static String getKey(TreeColumn treeColumn) {
        return getKey((LintColumn) treeColumn.getData(KEY_COLUMN));
    }

    private static String getKey(LintColumn column) {
        return column.getClass().getSimpleName();
    }

    private LintColumn[] getVisibleColumns() {
        if (mVisibleColumns == null) {
            if (mSingleFile) {
                // Special mode where we show just lint warnings for a single file:
                // use a hardcoded list of columns, not including path/location etc but
                // including line numbers (which are normally not shown by default).
                mVisibleColumns = new LintColumn[] {
                        mMessageColumn, mLineColumn
                };
            } else {
                // Generate visible columns based on (a) previously saved window state,
                // and (b) default window visible states provided by the columns themselves
                List<LintColumn> list = new ArrayList<LintColumn>();
                IMemento visibleColumns = null;
                if (mMemento != null) {
                    visibleColumns = mMemento.getChild(KEY_VISIBLE);
                }
                for (LintColumn column : mColumns) {
                    if (visibleColumns != null) {
                        Boolean b = visibleColumns.getBoolean(getKey(column));
                        if (b != null && b.booleanValue()) {
                            list.add(column);
                        }
                    } else if (column.visibleByDefault()) {
                        list.add(column);
                    }
                }
                if (!list.contains(mMessageColumn)) {
                    list.add(0, mMessageColumn);
                }
                mVisibleColumns = list.toArray(new LintColumn[list.size()]);
            }
        }

        return mVisibleColumns;
    }

    int getCount(IMarker marker) {
        return mContentProvider.getCount(marker);
    }

    Issue getIssue(String id) {
        return mRegistry.getIssue(id);
    }

    Issue getIssue(IMarker marker) {
        String id = EclipseLintClient.getId(marker);
        return mRegistry.getIssue(id);
    }

    Severity getSeverity(Issue issue) {
        return mConfiguration.getSeverity(issue);
    }

    // ---- Choosing visible columns ----

    public void configureColumns() {
        ColumnDialog dialog = new ColumnDialog(getShell(), mColumns, getVisibleColumns());
        if (dialog.open() == Window.OK) {
            mVisibleColumns = dialog.getSelectedColumns();
            // Clear out columns: Must recreate to set the right label provider etc
            for (TreeColumn column : mTree.getColumns()) {
                column.dispose();
            }
            createColumns();
            mTreeViewer.setComparator(new TableComparator());
            setSortIndicators();
            mTreeViewer.refresh();
        }
    }

    // ---- Table Sorting ----

    private SelectionListener getHeaderListener() {
        return new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                final TreeColumn treeColumn = (TreeColumn) e.widget;
                final LintColumn column = (LintColumn) treeColumn.getData(KEY_COLUMN);

                try {
                    IWorkbenchSiteProgressService progressService = getProgressService();
                    if (progressService == null) {
                        BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
                            @Override
                            public void run() {
                                resortTable(treeColumn, column,
                                        new NullProgressMonitor());
                            }
                        });
                    } else {
                        getProgressService().busyCursorWhile(new IRunnableWithProgress() {
                            @Override
                            public void run(IProgressMonitor monitor) {
                                resortTable(treeColumn, column, monitor);
                            }
                        });
                    }
                } catch (InvocationTargetException e1) {
                    AdtPlugin.log(e1, null);
                } catch (InterruptedException e1) {
                    return;
                }
            }

            private void resortTable(final TreeColumn treeColumn, LintColumn column,
                    IProgressMonitor monitor) {
                TableComparator sorter = getTableSorter();
                monitor.beginTask("Sorting", 100);
                monitor.worked(10);
                if (column.equals(sorter.getTopColumn())) {
                    sorter.reverseTopPriority();
                } else {
                    sorter.setTopPriority(column);
                }
                monitor.worked(15);
                PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        mTreeViewer.refresh();
                        updateDirectionIndicator(treeColumn);
                    }
                });
                monitor.done();
            }
        };
    }

    private void setSortIndicators() {
        LintColumn top = getTableSorter().getTopColumn();
        TreeColumn[] columns = mTreeViewer.getTree().getColumns();
        for (int i = 0; i < columns.length; i++) {
            TreeColumn column = columns[i];
            if (column.getData(KEY_COLUMN).equals(top)) {
                updateDirectionIndicator(column);
                return;
            }
        }
    }

    private void updateDirectionIndicator(TreeColumn column) {
        Tree tree = mTreeViewer.getTree();
        tree.setSortColumn(column);
        if (getTableSorter().isAscending()) {
            tree.setSortDirection(SWT.UP);
        } else {
            tree.setSortDirection(SWT.DOWN);
        }
    }

    private TableComparator getTableSorter() {
        return (TableComparator) mTreeViewer.getComparator();
    }

    /** Comparator used to sort the {@link LintList} tree.
     * <p>
     * This code is simplified from similar code in
     *    org.eclipse.ui.views.markers.internal.TableComparator
     */
    private class TableComparator extends ViewerComparator {
        private int[] mPriorities;
        private boolean[] mDirections;
        private int[] mDefaultPriorities;
        private boolean[] mDefaultDirections;

        private TableComparator() {
            int[] defaultPriorities = new int[mColumns.length];
            for (int i = 0; i < defaultPriorities.length; i++) {
                defaultPriorities[i] = i;
            }
            mPriorities = defaultPriorities;

            boolean[] directions = new boolean[mColumns.length];
            for (int i = 0; i < directions.length; i++) {
                directions[i] = mColumns[i].isAscending();
            }
            mDirections = directions;

            mDefaultPriorities = new int[defaultPriorities.length];
            System.arraycopy(defaultPriorities, 0, this.mDefaultPriorities, 0,
                    defaultPriorities.length);
            mDefaultDirections = new boolean[directions.length];
            System.arraycopy(directions, 0, this.mDefaultDirections, 0, directions.length);
        }

        private void resetState() {
            System.arraycopy(mDefaultPriorities, 0, mPriorities, 0, mPriorities.length);
            System.arraycopy(mDefaultDirections, 0, mDirections, 0, mDirections.length);
        }

        private void reverseTopPriority() {
            mDirections[mPriorities[0]] = !mDirections[mPriorities[0]];
        }

        private void setTopPriority(LintColumn property) {
            for (int i = 0; i < mColumns.length; i++) {
                if (mColumns[i].equals(property)) {
                    setTopPriority(i);
                    return;
                }
            }
        }

        private void setTopPriority(int priority) {
            if (priority < 0 || priority >= mPriorities.length) {
                return;
            }
            int index = -1;
            for (int i = 0; i < mPriorities.length; i++) {
                if (mPriorities[i] == priority) {
                    index = i;
                }
            }
            if (index == -1) {
                resetState();
                return;
            }
            // shift the array
            for (int i = index; i > 0; i--) {
                mPriorities[i] = mPriorities[i - 1];
            }
            mPriorities[0] = priority;
            mDirections[priority] = mDefaultDirections[priority];
        }

        private boolean isAscending() {
            return mDirections[mPriorities[0]];
        }

        private int getTopPriority() {
            return mPriorities[0];
        }

        private LintColumn getTopColumn() {
            return mColumns[getTopPriority()];
        }

        @Override
        public int compare(Viewer viewer, Object e1, Object e2) {
            return compare((IMarker) e1, (IMarker) e2, 0, true);
        }

        private int compare(IMarker marker1, IMarker marker2, int depth,
                boolean continueSearching) {
            if (depth >= mPriorities.length) {
                return 0;
            }
            int column = mPriorities[depth];
            LintColumn property = mColumns[column];
            int result = property.compare(marker1, marker2);
            if (result == 0 && continueSearching) {
                return compare(marker1, marker2, depth + 1, continueSearching);
            }
            return result * (mDirections[column] ? 1 : -1);
        }
    }
}

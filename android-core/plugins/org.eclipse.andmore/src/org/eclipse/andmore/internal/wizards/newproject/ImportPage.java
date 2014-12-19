/*
 * Copyright (C) 2012 The Android Open Source Project
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
package org.eclipse.andmore.internal.wizards.newproject;

import com.android.tools.lint.detector.api.LintUtils;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkingSet;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** WizardPage for importing Android projects */
class ImportPage extends WizardPage implements SelectionListener, IStructuredContentProvider,
        ICheckStateListener, KeyListener, TraverseListener, ControlListener {
    private static final int DIR_COLUMN = 0;
    private static final int NAME_COLUMN = 1;

    private final NewProjectWizardState mValues;
    private List<ImportedProject> mProjectPaths;
    private final IProject[] mExistingProjects;

    private Text mDir;
    private Button mBrowseButton;
    private Button mCopyCheckBox;
    private Button mRefreshButton;
    private Button mDeselectAllButton;
    private Button mSelectAllButton;
    private Table mTable;
    private CheckboxTableViewer mCheckboxTableViewer;
    private WorkingSetGroup mWorkingSetGroup;

    ImportPage(NewProjectWizardState values) {
        super("importPage"); //$NON-NLS-1$
        mValues = values;
        setTitle("Import Projects");
        setDescription("Select a directory to search for existing Android projects");
        mWorkingSetGroup = new WorkingSetGroup();
        setWorkingSets(new IWorkingSet[0]);

        // Record all projects such that we can ensure that the project names are unique
        IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        mExistingProjects = workspaceRoot.getProjects();
    }

    public void init(IStructuredSelection selection, IWorkbenchPart activePart) {
        setWorkingSets(WorkingSetHelper.getSelectedWorkingSet(selection, activePart));
    }

    @SuppressWarnings("unused") // SWT constructors have side effects and aren't unused
    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NULL);
        setControl(container);
        container.setLayout(new GridLayout(3, false));

        Label directoryLabel = new Label(container, SWT.NONE);
        directoryLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        directoryLabel.setText("Root Directory:");

        mDir = new Text(container, SWT.BORDER);
        mDir.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        mDir.addKeyListener(this);
        mDir.addTraverseListener(this);

        mBrowseButton = new Button(container, SWT.NONE);
        mBrowseButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        mBrowseButton.setText("Browse...");
        mBrowseButton.addSelectionListener(this);

        Label projectsLabel = new Label(container, SWT.NONE);
        projectsLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
        projectsLabel.setText("Projects:");

        mTable = new Table(container, SWT.CHECK);
        mTable.setHeaderVisible(true);
        mCheckboxTableViewer = new CheckboxTableViewer(mTable);

        TableViewerColumn dirViewerColumn = new TableViewerColumn(mCheckboxTableViewer, SWT.NONE);
        TableColumn dirColumn = dirViewerColumn.getColumn();
        dirColumn.setWidth(200);
        dirColumn.setText("Project to Import");
        TableViewerColumn nameViewerColumn = new TableViewerColumn(mCheckboxTableViewer, SWT.NONE);
        TableColumn nameColumn = nameViewerColumn.getColumn();
        nameColumn.setWidth(200);
        nameColumn.setText("New Project Name");
        nameViewerColumn.setEditingSupport(new ProjectNameEditingSupport(mCheckboxTableViewer));

        mTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 4));
        mTable.setLinesVisible(true);
        mTable.setHeaderVisible(true);
        mTable.addSelectionListener(this);
        mTable.addControlListener(this);
        mCheckboxTableViewer.setContentProvider(this);
        mCheckboxTableViewer.setInput(this);
        mCheckboxTableViewer.addCheckStateListener(this);
        mCheckboxTableViewer.setLabelProvider(new ProjectCellLabelProvider());

        mSelectAllButton = new Button(container, SWT.NONE);
        mSelectAllButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        mSelectAllButton.setText("Select All");
        mSelectAllButton.addSelectionListener(this);

        mDeselectAllButton = new Button(container, SWT.NONE);
        mDeselectAllButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        mDeselectAllButton.setText("Deselect All");
        mDeselectAllButton.addSelectionListener(this);

        mRefreshButton = new Button(container, SWT.NONE);
        mRefreshButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        mRefreshButton.setText("Refresh");
        mRefreshButton.addSelectionListener(this);
        new Label(container, SWT.NONE);

        mCopyCheckBox = new Button(container, SWT.CHECK);
        mCopyCheckBox.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
        mCopyCheckBox.setText("Copy projects into workspace");
        mCopyCheckBox.addSelectionListener(this);

        Composite group = mWorkingSetGroup.createControl(container);
        group.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1));

        updateColumnWidths();
    }

    private void updateColumnWidths() {
        Rectangle r = mTable.getClientArea();
        int availableWidth = r.width;
        // Add all available size to the first column
        for (int i = 1; i < mTable.getColumnCount(); i++) {
            TableColumn column = mTable.getColumn(i);
            availableWidth -= column.getWidth();
        }
        if (availableWidth > 100) {
            mTable.getColumn(0).setWidth(availableWidth);
        }
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        validatePage();
    }

    private void refresh() {
        File root = new File(mDir.getText().trim());
        mProjectPaths = searchForProjects(root);
        mCheckboxTableViewer.refresh();
        mCheckboxTableViewer.setAllChecked(true);

        updateValidity();
        validatePage();
    }

    private void updateValidity(){
        List<ImportedProject> selected = new ArrayList<ImportedProject>();
        List<ImportedProject> disabled = new ArrayList<ImportedProject>();
        for (ImportedProject project : mProjectPaths) {
            String projectName = project.getProjectName();
            boolean invalid = false;
            for (IProject existingProject : mExistingProjects) {
                if (projectName.equals(existingProject.getName())) {
                    invalid = true;
                    break;
                }
            }
            if (invalid) {
                disabled.add(project);
            } else {
                selected.add(project);
            }
        }

        mValues.importProjects = selected;

        mCheckboxTableViewer.setGrayedElements(disabled.toArray());
        mCheckboxTableViewer.setCheckedElements(selected.toArray());
        mCheckboxTableViewer.refresh();
        mCheckboxTableViewer.getTable().setFocus();
    }

    private List<ImportedProject> searchForProjects(File dir) {
        List<ImportedProject> projects = new ArrayList<ImportedProject>();
        addProjects(dir, projects, dir.getPath().length() + 1);
        return projects;
    }

    /** Finds all project directories under the given directory */
    private void addProjects(File dir, List<ImportedProject> projects, int prefixLength) {
        if (dir.isDirectory()) {
            if (LintUtils.isManifestFolder(dir)) {
                String relative = dir.getPath();
                if (relative.length() > prefixLength) {
                    relative = relative.substring(prefixLength);
                }
                projects.add(new ImportedProject(dir, relative));
            }

            File[] children = dir.listFiles();
            if (children != null) {
                for (File child : children) {
                    addProjects(child, projects, prefixLength);
                }
            }
        }
    }

    private void validatePage() {
        IStatus status = null;

        // Validate project name -- unless we're creating a sample, in which case
        // the user will get a chance to pick the name on the Sample page
        if (mProjectPaths == null || mProjectPaths.isEmpty()) {
            status = new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID,
                    "Select a directory to search for existing Android projects");
        } else if (mValues.importProjects == null || mValues.importProjects.isEmpty()) {
            status = new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID,
                    "Select at least one project");
        } else {
            for (ImportedProject project : mValues.importProjects) {
                if (mCheckboxTableViewer.getGrayed(project)) {
                    status = new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID,
                            String.format("Cannot import %1$s because the project name is in use",
                                    project.getProjectName()));
                    break;
                } else {
                    status = ProjectNamePage.validateProjectName(project.getProjectName());
                    if (status != null && !status.isOK()) {
                        // Need to insert project name to make it clear which project name
                        // is in violation
                        if (mValues.importProjects.size() > 1) {
                            String message = String.format("%1$s: %2$s",
                                    project.getProjectName(), status.getMessage());
                            status = new Status(status.getSeverity(), AdtPlugin.PLUGIN_ID,
                                    message);
                        }
                        break;
                    } else {
                        status = null; // Don't leave non null status with isOK() == true
                    }
                }
            }
        }

        // -- update UI & enable finish if there's no error
        setPageComplete(status == null || status.getSeverity() != IStatus.ERROR);
        if (status != null) {
            setMessage(status.getMessage(),
                    status.getSeverity() == IStatus.ERROR
                        ? IMessageProvider.ERROR : IMessageProvider.WARNING);
        } else {
            setErrorMessage(null);
            setMessage(null);
        }
    }

    /**
     * Returns the working sets to which the new project should be added.
     *
     * @return the selected working sets to which the new project should be added
     */
    private IWorkingSet[] getWorkingSets() {
        return mWorkingSetGroup.getSelectedWorkingSets();
    }

    /**
     * Sets the working sets to which the new project should be added.
     *
     * @param workingSets the initial selected working sets
     */
    private void setWorkingSets(IWorkingSet[] workingSets) {
        assert workingSets != null;
        mWorkingSetGroup.setWorkingSets(workingSets);
    }

    @Override
    public IWizardPage getNextPage() {
        // Sync working set data to the value object, since the WorkingSetGroup
        // doesn't let us add listeners to do this lazily
        mValues.workingSets = getWorkingSets();

        return super.getNextPage();
    }

    // ---- Implements SelectionListener ----

    @Override
    public void widgetSelected(SelectionEvent e) {
        Object source = e.getSource();
        if (source == mBrowseButton) {
            // Choose directory
            DirectoryDialog dialog = new DirectoryDialog(getShell(), SWT.OPEN);
            String path = mDir.getText().trim();
            if (path.length() > 0) {
                dialog.setFilterPath(path);
            }
            String file = dialog.open();
            if (file != null) {
                mDir.setText(file);
                refresh();
            }
        } else if (source == mSelectAllButton) {
            mCheckboxTableViewer.setAllChecked(true);
            mValues.importProjects = mProjectPaths;
        } else if (source == mDeselectAllButton) {
            mCheckboxTableViewer.setAllChecked(false);
            mValues.importProjects = Collections.emptyList();
        } else if (source == mRefreshButton || source == mDir) {
            refresh();
        } else if (source == mCopyCheckBox) {
            mValues.copyIntoWorkspace = mCopyCheckBox.getSelection();
        }

        validatePage();
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent e) {
    }

    // ---- KeyListener ----

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getSource() == mDir) {
            if (e.keyCode == SWT.CR) {
                refresh();
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    // ---- TraverseListener ----

    @Override
    public void keyTraversed(TraverseEvent e) {
        // Prevent Return from running through the wizard; return is handled by
        // key listener to refresh project list instead
        if (SWT.TRAVERSE_RETURN == e.detail) {
            e.doit = false;
        }
    }

    // ---- Implements IStructuredContentProvider ----

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }

    @Override
    public Object[] getElements(Object inputElement) {
        return mProjectPaths != null ? mProjectPaths.toArray() : new Object[0];
    }

    // ---- Implements ICheckStateListener ----

    @Override
    public void checkStateChanged(CheckStateChangedEvent event) {
        // Try to disable other elements that conflict with this
        Object[] checked = mCheckboxTableViewer.getCheckedElements();
        List<ImportedProject> selected = new ArrayList<ImportedProject>(checked.length);
        for (Object o : checked) {
            if (!mCheckboxTableViewer.getGrayed(o)) {
                selected.add((ImportedProject) o);
            }
        }
        mValues.importProjects = selected;
        validatePage();

        mCheckboxTableViewer.update(event.getElement(), null);
    }

    // ---- Implements ControlListener ----

    @Override
    public void controlMoved(ControlEvent e) {
    }

    @Override
    public void controlResized(ControlEvent e) {
        updateColumnWidths();
    }

    private final class ProjectCellLabelProvider extends CellLabelProvider {
        @Override
        public void update(ViewerCell cell) {
            Object element = cell.getElement();
            int index = cell.getColumnIndex();
            ImportedProject project = (ImportedProject) element;

            Display display = mTable.getDisplay();
            Color fg;
            if (mCheckboxTableViewer.getGrayed(element)) {
                fg = display.getSystemColor(SWT.COLOR_DARK_GRAY);
            } else {
                fg = display.getSystemColor(SWT.COLOR_LIST_FOREGROUND);
            }
            cell.setForeground(fg);
            cell.setBackground(display.getSystemColor(SWT.COLOR_LIST_BACKGROUND));

            switch (index) {
                case DIR_COLUMN: {
                    // Directory name
                    cell.setText(project.getRelativePath());
                    return;
                }

                case NAME_COLUMN: {
                    // New name
                    cell.setText(project.getProjectName());
                    return;
                }
                default:
                    assert false : index;
            }
            cell.setText("");
        }
    }

    /** Editing support for the project name column */
    private class ProjectNameEditingSupport extends EditingSupport {
        private ProjectNameEditingSupport(ColumnViewer viewer) {
            super(viewer);
        }

        @Override
        protected void setValue(Object element, Object value) {
            ImportedProject project = (ImportedProject) element;
            project.setProjectName(value.toString());
            mCheckboxTableViewer.update(element, null);
            updateValidity();
            validatePage();
        }

        @Override
        protected Object getValue(Object element) {
            ImportedProject project = (ImportedProject) element;
            return project.getProjectName();
        }

        @Override
        protected CellEditor getCellEditor(Object element) {
            return new TextCellEditor(mTable);
        }

        @Override
        protected boolean canEdit(Object element) {
            return true;
        }
    }
}

/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.sdkuilib.internal.repository.ui;

import com.android.sdklib.internal.repository.sources.SdkAddonSource;
import com.android.sdklib.internal.repository.sources.SdkSource;
import com.android.sdklib.internal.repository.sources.SdkSourceCategory;
import com.android.sdklib.internal.repository.sources.SdkSourceProperties;
import com.android.sdklib.internal.repository.sources.SdkSources;
import com.android.sdklib.internal.repository.sources.SdkSysImgSource;
import com.android.sdklib.repository.SdkSysImgConstants;
import com.android.sdkuilib.internal.repository.UpdaterBaseDialog;
import com.android.sdkuilib.internal.repository.SwtUpdaterData;
import com.android.sdkuilib.ui.GridDataBuilder;
import com.android.sdkuilib.ui.GridLayoutBuilder;

import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Dialog that displays 2 tabs: <br/>
 * - one tab with the list of extra add-ons sites defined by the user. <br/>
 * - one tab with the list of 3rd-party add-ons currently available, which the user can
 *   deactivate to prevent from loading them.
 */
public class AddonSitesDialog extends UpdaterBaseDialog {

    private final SdkSources mSources;
    private Table mUserTable;
    private TableViewer mUserTableViewer;
    private CheckboxTableViewer mSitesTableViewer;
    private Button mUserButtonNew;
    private Button mUserButtonDelete;
    private Button mUserButtonEdit;
    private Runnable mSourcesChangeListener;

    /**
     * Create the dialog.
     *
     * @param parent The parent's shell
     * @wbp.parser.entryPoint
     */
    public AddonSitesDialog(Shell parent, SwtUpdaterData updaterData) {
        super(parent, updaterData, "Add-on Sites");
        mSources = updaterData.getSources();
        assert mSources != null;
    }

    /**
     * Create contents of the dialog.
     * @wbp.parser.entryPoint
     */
    @Override
    protected void createContents() {
        super.createContents();
        Shell shell = getShell();
        shell.setMinimumSize(new Point(300, 300));
        shell.setSize(600, 400);

        TabFolder tabFolder = new TabFolder(shell, SWT.NONE);
        GridDataBuilder.create(tabFolder).fill().grab().hSpan(2);

        TabItem sitesTabItem = new TabItem(tabFolder, SWT.NONE);
        sitesTabItem.setText("Official Add-on Sites");
        createTabOfficialSites(tabFolder, sitesTabItem);

        TabItem userTabItem = new TabItem(tabFolder, SWT.NONE);
        userTabItem.setText("User Defined Sites");
        createTabUserSites(tabFolder, userTabItem);

        // placeholder for aligning close button
        Label label = new Label(shell, SWT.NONE);
        GridDataBuilder.create(label).hFill().hGrab();

        createCloseButton();
    }

    void createTabOfficialSites(TabFolder tabFolder, TabItem sitesTabItem) {
        Composite root = new Composite(tabFolder, SWT.NONE);
        sitesTabItem.setControl(root);
        GridLayoutBuilder.create(root).columns(3);

        Label label = new Label(root, SWT.NONE);
        GridDataBuilder.create(label).hGrab().vCenter().hSpan(3);
        label.setText(
            "This lets select which official 3rd-party sites you want to load.\n" +
            "\n" +
            "These sites are managed by non-Android vendors to provide add-ons and extra packages.\n" +
            "They are by default all enabled. When you disable one, the SDK Manager will not check the site for new packages."
        );

        mSitesTableViewer = CheckboxTableViewer.newCheckList(root, SWT.BORDER | SWT.FULL_SELECTION);
        mSitesTableViewer.setContentProvider(new SourcesContentProvider());

        Table sitesTable = mSitesTableViewer.getTable();
        sitesTable.setToolTipText("Enable 3rd-Party Site");
        sitesTable.setLinesVisible(true);
        sitesTable.setHeaderVisible(true);
        GridDataBuilder.create(sitesTable).fill().grab().hSpan(3);

        TableViewerColumn columnViewer = new TableViewerColumn(mSitesTableViewer, SWT.NONE);
        TableColumn column = columnViewer.getColumn();
        column.setResizable(true);
        column.setWidth(150);
        column.setText("Name");
        columnViewer.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof SdkSource) {
                    String name = ((SdkSource) element).getUiName();
                    if (name != null) {
                        return name;
                    }
                    return ((SdkSource) element).getShortDescription();
                }
                return super.getText(element);
            }
        });

        columnViewer = new TableViewerColumn(mSitesTableViewer, SWT.NONE);
        column = columnViewer.getColumn();
        column.setResizable(true);
        column.setWidth(400);
        column.setText("URL");
        columnViewer.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof SdkSource) {
                    return ((SdkSource) element).getUrl();
                }
                return super.getText(element);
            }
        });

        mSitesTableViewer.addCheckStateListener(new ICheckStateListener() {
            @Override
            public void checkStateChanged(CheckStateChangedEvent event) {
                on_SitesTableViewer_checkStateChanged(event);
            }
        });

        // "enable all" and "disable all" buttons under the table
        Button selectAll = new Button(root, SWT.NONE);
        selectAll.setText("Enable All");
        GridDataBuilder.create(selectAll).hLeft();
        selectAll.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                on_SitesTableViewer_selectAll();
            }
        });

        // placeholder between both buttons
        label = new Label(root, SWT.NONE);
        GridDataBuilder.create(label).hFill().hGrab();

        Button deselectAll = new Button(root, SWT.NONE);
        deselectAll.setText("Disable All");
        GridDataBuilder.create(deselectAll).hRight();
        deselectAll.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                on_SitesTableViewer_deselectAll();
            }
        });
    }

    void createTabUserSites(TabFolder tabFolder, TabItem userTabItem) {
        Composite root = new Composite(tabFolder, SWT.NONE);
        userTabItem.setControl(root);
        GridLayoutBuilder.create(root).columns(2);

        Label label = new Label(root, SWT.NONE);
        GridDataBuilder.create(label).hLeft().vCenter().hSpan(2);
        label.setText(
            "This lets you manage a list of user-contributed external add-on sites URLs.\n" +
            "\n" +
            "Add-on sites can provide new add-ons and extra packages.\n" +
            "They cannot provide standard Android platforms, system images or docs.\n" +
            "Adding a URL here will not allow you to clone an official Android repository."
        );

        mUserTableViewer = new TableViewer(root, SWT.BORDER | SWT.FULL_SELECTION);
        mUserTableViewer.setContentProvider(new SourcesContentProvider());

        mUserTableViewer.addPostSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                on_UserTableViewer_selectionChanged(event);
            }
        });
        mUserTable = mUserTableViewer.getTable();
        mUserTable.setLinesVisible(true);
        mUserTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseUp(MouseEvent event) {
                on_UserTable_mouseUp(event);
            }
        });
        GridDataBuilder.create(mUserTable).fill().grab().vSpan(5);

        TableViewerColumn tableViewerColumn = new TableViewerColumn(mUserTableViewer, SWT.NONE);
        TableColumn userColumnUrl = tableViewerColumn.getColumn();
        userColumnUrl.setWidth(100);

        // Implementation detail: set the label provider on the table viewer *after* associating
        // a column. This will set the label provider on the column for us.
        mUserTableViewer.setLabelProvider(new LabelProvider());


        mUserButtonNew = new Button(root, SWT.NONE);
        mUserButtonNew.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                userNewOrEdit(false /*isEdit*/);
            }
        });
        GridDataBuilder.create(mUserButtonNew).hFill().vCenter();
        mUserButtonNew.setText("New...");

        mUserButtonEdit = new Button(root, SWT.NONE);
        mUserButtonEdit.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                userNewOrEdit(true /*isEdit*/);
            }
        });
        GridDataBuilder.create(mUserButtonEdit).hFill().vCenter();
        mUserButtonEdit.setText("Edit...");

        mUserButtonDelete = new Button(root, SWT.NONE);
        mUserButtonDelete.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                on_UserButtonDelete_widgetSelected(e);
            }
        });
        GridDataBuilder.create(mUserButtonDelete).hFill().vCenter();
        mUserButtonDelete.setText("Delete...");

        adjustColumnsWidth(mUserTable, userColumnUrl);
    }

    @Override
    protected void close() {
        if (mSources != null && mSourcesChangeListener != null) {
            mSources.removeChangeListener(mSourcesChangeListener);
        }
        SdkSourceProperties p = new SdkSourceProperties();
        p.save();
        super.close();
    }

    /**
     * Adds a listener to adjust the column width when the parent is resized.
     */
    private void adjustColumnsWidth(final Table table, final TableColumn column0) {
        // Add a listener to resize the column to the full width of the table
        table.addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {
                Rectangle r = table.getClientArea();
                column0.setWidth(r.width * 100 / 100); // 100%
            }
        });
    }

    private void userNewOrEdit(final boolean isEdit) {
        final SdkSource[] knownSources = mSources.getAllSources();
        String title = isEdit ? "Edit Add-on Site URL" : "Add Add-on Site URL";
        String msg = "Please enter the URL of the addon.xml:";
        IStructuredSelection sel = (IStructuredSelection) mUserTableViewer.getSelection();
        final String initialValue = !isEdit || sel.isEmpty() ? null :
                                                               sel.getFirstElement().toString();

        if (isEdit && initialValue == null) {
            // Edit with no actual value is not supposed to happen. Ignore this case.
            return;
        }

        InputDialog dlg = new InputDialog(
                getShell(),
                title,
                msg,
                initialValue,
                new IInputValidator() {
            @Override
            public String isValid(String newText) {

                newText = newText == null ? null : newText.trim();

                if (newText == null || newText.length() == 0) {
                    return "Error: URL field is empty. Please enter a URL.";
                }

                // A URL should have one of the following prefixes
                if (!newText.startsWith("file://") &&                 //$NON-NLS-1$
                        !newText.startsWith("ftp://") &&              //$NON-NLS-1$
                        !newText.startsWith("http://") &&             //$NON-NLS-1$
                        !newText.startsWith("https://")) {            //$NON-NLS-1$
                    return "Error: The URL must start by one of file://, ftp://, http:// or https://";
                }

                if (isEdit && newText.equals(initialValue)) {
                    // Edited value hasn't changed. This isn't an error.
                    return null;
                }

                // Reject URLs that are already in the source list.
                // URLs are generally case-insensitive (except for file:// where it all depends
                // on the current OS so we'll ignore this case.)
                for (SdkSource s : knownSources) {
                    if (newText.equalsIgnoreCase(s.getUrl())) {
                        return "Error: This site is already listed.";
                    }
                }

                return null;
            }
        });

        if (dlg.open() == Window.OK) {
            String url = dlg.getValue().trim();

            if (!url.equals(initialValue)) {
                if (isEdit && initialValue != null) {
                    // Remove the old value before we add the new one, which is we just
                    // asserted will be different.
                    for (SdkSource source : mSources.getSources(SdkSourceCategory.USER_ADDONS)) {
                        if (initialValue.equals(source.getUrl())) {
                            mSources.remove(source);
                            break;
                        }
                    }

                }

                // create the source, store it and update the list
                SdkSource newSource;
                // use url suffix to decide whether this is a SysImg or Addon;
                // see SdkSources.loadUserAddons() for another check like this
                if (url.endsWith(SdkSysImgConstants.URL_DEFAULT_FILENAME)) {
                     newSource = new SdkSysImgSource(url, null/*uiName*/);
                } else {
                     newSource = new SdkAddonSource(url, null/*uiName*/);
                }
                mSources.add(SdkSourceCategory.USER_ADDONS, newSource);
                setReturnValue(true);
                // notify sources change listeners. This will invoke our own loadUserUrlsList().
                mSources.notifyChangeListeners();

                // select the new source
                IStructuredSelection newSel = new StructuredSelection(newSource);
                mUserTableViewer.setSelection(newSel, true /*reveal*/);
            }
        }
    }

    private void on_UserButtonDelete_widgetSelected(SelectionEvent e) {
        IStructuredSelection sel = (IStructuredSelection) mUserTableViewer.getSelection();
        String selectedUrl = sel.isEmpty() ? null : sel.getFirstElement().toString();

        if (selectedUrl == null) {
            return;
        }

        MessageBox mb = new MessageBox(getShell(),
                SWT.YES | SWT.NO | SWT.ICON_QUESTION | SWT.APPLICATION_MODAL);
        mb.setText("Delete add-on site");
        mb.setMessage(String.format("Do you want to delete the URL %1$s?", selectedUrl));
        if (mb.open() == SWT.YES) {
            for (SdkSource source : mSources.getSources(SdkSourceCategory.USER_ADDONS)) {
                if (selectedUrl.equals(source.getUrl())) {
                    mSources.remove(source);
                    setReturnValue(true);
                    mSources.notifyChangeListeners();
                    break;
                }
            }
        }
    }

    private void on_UserTable_mouseUp(MouseEvent event) {
        Point p = new Point(event.x, event.y);
        if (mUserTable.getItem(p) == null) {
            mUserTable.deselectAll();
            on_UserTableViewer_selectionChanged(null /*event*/);
        }
    }

    private void on_UserTableViewer_selectionChanged(SelectionChangedEvent event) {
        ISelection sel = mUserTableViewer.getSelection();
        mUserButtonDelete.setEnabled(!sel.isEmpty());
        mUserButtonEdit.setEnabled(!sel.isEmpty());
    }

    private void on_SitesTableViewer_checkStateChanged(CheckStateChangedEvent event) {
        Object element = event.getElement();
        if (element instanceof SdkSource) {
            SdkSource source = (SdkSource) element;
            boolean isChecked = event.getChecked();
            if (source.isEnabled() != isChecked) {
                setReturnValue(true);
                source.setEnabled(isChecked);
                mSources.notifyChangeListeners();
            }
        }
    }

    private void on_SitesTableViewer_selectAll() {
        for (Object item : (Object[]) mSitesTableViewer.getInput()) {
            if (!mSitesTableViewer.getChecked(item)) {
                mSitesTableViewer.setChecked(item, true);
                on_SitesTableViewer_checkStateChanged(
                        new CheckStateChangedEvent(mSitesTableViewer, item, true));
            }
        }
    }

    private void on_SitesTableViewer_deselectAll() {
        for (Object item : (Object[]) mSitesTableViewer.getInput()) {
            if (mSitesTableViewer.getChecked(item)) {
                mSitesTableViewer.setChecked(item, false);
                on_SitesTableViewer_checkStateChanged(
                        new CheckStateChangedEvent(mSitesTableViewer, item, false));
            }
        }
    }


    @Override
    protected void postCreate() {
        // A runnable to initially load and then update the user urls & sites lists.
        final Runnable updateInUiThread = new Runnable() {
            @Override
            public void run() {
                loadUserUrlsList();
                loadSiteUrlsList();
            }
        };

        // A listener that runs when the sources have changed.
        // This is most likely called on a worker thread.
        mSourcesChangeListener = new Runnable() {
            @Override
            public void run() {
                Shell shell = getShell();
                if (shell != null) {
                    Display display = shell.getDisplay();
                    if (display != null) {
                        display.syncExec(updateInUiThread);
                    }
                }
            }
        };

        mSources.addChangeListener(mSourcesChangeListener);

        // initialize the list
        updateInUiThread.run();
    }

    private void loadUserUrlsList() {
        SdkSource[] knownSources = mSources.getSources(SdkSourceCategory.USER_ADDONS);
        Arrays.sort(knownSources);

        ISelection oldSelection = mUserTableViewer.getSelection();

        mUserTableViewer.setInput(knownSources);
        mUserTableViewer.refresh();
        // initialize buttons' state that depend on the list
        on_UserTableViewer_selectionChanged(null /*event*/);

        if (oldSelection != null && !oldSelection.isEmpty()) {
            mUserTableViewer.setSelection(oldSelection, true /*reveal*/);
        }
    }

    private void loadSiteUrlsList() {
        SdkSource[] knownSources = mSources.getSources(SdkSourceCategory.ADDONS_3RD_PARTY);
        Arrays.sort(knownSources);

        ISelection oldSelection = mSitesTableViewer.getSelection();

        mSitesTableViewer.setInput(knownSources);
        mSitesTableViewer.refresh();

        if (oldSelection != null && !oldSelection.isEmpty()) {
            mSitesTableViewer.setSelection(oldSelection, true /*reveal*/);
        }

        // Check the sources which are currently enabled.
        ArrayList<SdkSource> disabled = new ArrayList<SdkSource>(knownSources.length);
        for (SdkSource source : knownSources) {
            if (source.isEnabled()) {
                disabled.add(source);
            }
        }
        mSitesTableViewer.setCheckedElements(disabled.toArray());
    }


    private static class SourcesContentProvider implements IStructuredContentProvider {
        @Override
        public void dispose() {
            // pass
        }

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            // pass
        }

        @Override
        public Object[] getElements(Object inputElement) {
            if (inputElement instanceof SdkSource[]) {
                return (Object[]) inputElement;
            } else {
                return new Object[0];
            }
        }
    }
}

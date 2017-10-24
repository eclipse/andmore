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

import com.android.sdklib.internal.repository.ITask;
import com.android.sdklib.internal.repository.ITaskMonitor;
import com.android.sdklib.internal.repository.archives.Archive;
import com.android.sdklib.internal.repository.archives.ArchiveInstaller;
import com.android.sdklib.internal.repository.packages.Package;
import com.android.sdklib.internal.repository.updater.PkgItem;
import com.android.sdklib.internal.repository.updater.PkgItem.PkgState;
import com.android.sdklib.repository.ISdkChangeListener;
import com.android.sdkuilib.internal.repository.SwtUpdaterData;
import com.android.sdkuilib.internal.repository.core.PkgCategory;
import com.android.sdkuilib.internal.repository.core.PkgCategoryApi;
import com.android.sdkuilib.internal.repository.core.PkgContentProvider;
import com.android.sdkuilib.internal.repository.icons.ImageFactory;
import com.android.sdkuilib.repository.SdkUpdaterWindow.SdkInvocationContext;
import com.android.sdkuilib.ui.GridDataBuilder;
import com.android.sdkuilib.ui.GridLayoutBuilder;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Page that displays both locally installed packages as well as all known
 * remote available packages. This gives an overview of what is installed
 * vs what is available and allows the user to update or install packages.
 */
public final class PackagesPage extends Composite implements ISdkChangeListener {

    enum MenuAction {
        RELOAD                      (SWT.NONE,  "Reload"),
        SHOW_ADDON_SITES            (SWT.NONE,  "Manage Add-on Sites..."),
        TOGGLE_SHOW_ARCHIVES        (SWT.CHECK, "Show Archives Details"),
        TOGGLE_SHOW_INSTALLED_PKG   (SWT.CHECK, "Show Installed Packages"),
        TOGGLE_SHOW_OBSOLETE_PKG    (SWT.CHECK, "Show Obsolete Packages"),
        TOGGLE_SHOW_UPDATE_NEW_PKG  (SWT.CHECK, "Show Updates/New Packages")
        ;

        private final int mMenuStyle;
        private final String mMenuTitle;

        MenuAction(int menuStyle, String menuTitle) {
            mMenuStyle = menuStyle;
            mMenuTitle = menuTitle;
        }

        public int getMenuStyle() {
            return mMenuStyle;
        }

        public String getMenuTitle() {
            return mMenuTitle;
        }
    };

    private final Map<MenuAction, MenuItem> mMenuActions = new HashMap<MenuAction, MenuItem>();

    private final PackagesPageImpl mImpl;
    private final SdkInvocationContext mContext;

    private boolean mDisplayArchives = false;
    private boolean mOperationPending;

    private Composite mGroupPackages;
    private Text mTextSdkOsPath;
    private Button mCheckFilterObsolete;
    private Button mCheckFilterInstalled;
    private Button mCheckFilterNew;
    private Composite mGroupOptions;
    private Composite mGroupSdk;
    private Button mButtonDelete;
    private Button mButtonInstall;
    private Font mTreeFontItalic;
    private TreeColumn mTreeColumnName;
    private CheckboxTreeViewer mTreeViewer;

    public PackagesPage(
            Composite parent,
            int swtStyle,
            SwtUpdaterData swtUpdaterData,
            SdkInvocationContext context) {
        super(parent, swtStyle);
        mImpl = new PackagesPageImpl(swtUpdaterData) {
            @Override
            protected boolean isUiDisposed() {
                return mGroupPackages == null || mGroupPackages.isDisposed();
            };
            @Override
            protected void syncExec(Runnable runnable) {
                if (!isUiDisposed()) {
                    mGroupPackages.getDisplay().syncExec(runnable);
                }
            };

            @Override
            protected void syncViewerSelection() {
                PackagesPage.this.syncViewerSelection();
            }

            @Override
            protected void refreshViewerInput() {
                PackagesPage.this.refreshViewerInput();
            }

            @Override
            protected Font getTreeFontItalic() {
                return mTreeFontItalic;
            }

            @Override
            protected void loadPackages(boolean useLocalCache, boolean overrideExisting) {
                PackagesPage.this.loadPackages(useLocalCache, overrideExisting);
            }
        };
        mContext = context;

        createContents(this);
        postCreate();  //$hide$
    }

    public void performFirstLoad() {
        mImpl.performFirstLoad();
    }

    @SuppressWarnings("unused")
    private void createContents(Composite parent) {
        GridLayoutBuilder.create(parent).noMargins().columns(2);

        mGroupSdk = new Composite(parent, SWT.NONE);
        GridDataBuilder.create(mGroupSdk).hFill().vCenter().hGrab().hSpan(2);
        GridLayoutBuilder.create(mGroupSdk).columns(2);

        Label label1 = new Label(mGroupSdk, SWT.NONE);
        label1.setText("SDK Path:");

        mTextSdkOsPath = new Text(mGroupSdk, SWT.NONE);
        GridDataBuilder.create(mTextSdkOsPath).hFill().vCenter().hGrab();
        mTextSdkOsPath.setEnabled(false);

        Group groupPackages = new Group(parent, SWT.NONE);
        mGroupPackages = groupPackages;
        GridDataBuilder.create(mGroupPackages).fill().grab().hSpan(2);
        groupPackages.setText("Packages");
        GridLayoutBuilder.create(groupPackages).columns(1);

        mTreeViewer = new CheckboxTreeViewer(groupPackages, SWT.BORDER);
        mImpl.setITreeViewer(new PackagesPageImpl.ICheckboxTreeViewer() {
            @Override
            public Object getInput() {
                return mTreeViewer.getInput();
            }

            @Override
            public void setInput(List<PkgCategory> cats) {
                mTreeViewer.setInput(cats);
            }

            @Override
            public void setContentProvider(PkgContentProvider pkgContentProvider) {
                mTreeViewer.setContentProvider(pkgContentProvider);
            }

            @Override
            public void refresh() {
                mTreeViewer.refresh();
            }

            @Override
            public Object[] getCheckedElements() {
                return mTreeViewer.getCheckedElements();
            }
        });
        mTreeViewer.addFilter(new ViewerFilter() {
            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element) {
                return filterViewerItem(element);
            }
        });

        mTreeViewer.addCheckStateListener(new ICheckStateListener() {
            @Override
            public void checkStateChanged(CheckStateChangedEvent event) {
                onTreeCheckStateChanged(event); //$hide$
            }
        });

        mTreeViewer.addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(DoubleClickEvent event) {
                onTreeDoubleClick(event); //$hide$
            }
        });

        Tree tree = mTreeViewer.getTree();
        tree.setLinesVisible(true);
        tree.setHeaderVisible(true);
        GridDataBuilder.create(tree).fill().grab();

        // column name icon is set when loading depending on the current filter type
        // (e.g. API level or source)
        TreeViewerColumn columnName = new TreeViewerColumn(mTreeViewer, SWT.NONE);
        mTreeColumnName = columnName.getColumn();
        mTreeColumnName.setText("Name");
        mTreeColumnName.setWidth(340);

        TreeViewerColumn columnApi = new TreeViewerColumn(mTreeViewer, SWT.NONE);
        TreeColumn treeColumn2 = columnApi.getColumn();
        treeColumn2.setText("API");
        treeColumn2.setAlignment(SWT.CENTER);
        treeColumn2.setWidth(50);

        TreeViewerColumn columnRevision = new TreeViewerColumn(mTreeViewer, SWT.NONE);
        TreeColumn treeColumn3 = columnRevision.getColumn();
        treeColumn3.setText("Rev.");
        treeColumn3.setToolTipText("Revision currently installed");
        treeColumn3.setAlignment(SWT.CENTER);
        treeColumn3.setWidth(50);


        TreeViewerColumn columnStatus = new TreeViewerColumn(mTreeViewer, SWT.NONE);
        TreeColumn treeColumn4 = columnStatus.getColumn();
        treeColumn4.setText("Status");
        treeColumn4.setAlignment(SWT.LEAD);
        treeColumn4.setWidth(190);

        mImpl.setIColumns(
                wrapColumn(columnName),
                wrapColumn(columnApi),
                wrapColumn(columnRevision),
                wrapColumn(columnStatus));

        mGroupOptions = new Composite(groupPackages, SWT.NONE);
        GridDataBuilder.create(mGroupOptions).hFill().vCenter().hGrab();
        GridLayoutBuilder.create(mGroupOptions).columns(7).noMargins();

        // Options line 1, 7 columns

        Label label3 = new Label(mGroupOptions, SWT.NONE);
        label3.setText("Show:");

        mCheckFilterNew = new Button(mGroupOptions, SWT.CHECK);
        mCheckFilterNew.setText("Updates/New");
        mCheckFilterNew.setToolTipText("Show Updates and New");
        mCheckFilterNew.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                refreshViewerInput();
            }
        });
        mCheckFilterNew.setSelection(true);

        mCheckFilterInstalled = new Button(mGroupOptions, SWT.CHECK);
        mCheckFilterInstalled.setToolTipText("Show Installed");
        mCheckFilterInstalled.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                refreshViewerInput();
            }
        });
        mCheckFilterInstalled.setSelection(true);
        mCheckFilterInstalled.setText("Installed");

        new Label(mGroupOptions, SWT.NONE);

        Link linkSelectNew = new Link(mGroupOptions, SWT.NONE);
        // Note for i18n: we need to identify which link is used, and this is done by using the
        // text itself so for translation purposes we want to keep the <a> link strings separate.
        final String strLinkNew = "New";
        final String strLinkUpdates = "Updates";
        linkSelectNew.setText(
                String.format("Select <a>%1$s</a> or <a>%2$s</a>", strLinkNew, strLinkUpdates));
        linkSelectNew.setToolTipText("Selects all items that are either new or updates.");
        GridDataBuilder.create(linkSelectNew).hFill();
        linkSelectNew.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                super.widgetSelected(e);
                boolean selectNew = e.text == null || e.text.equals(strLinkNew);
                onSelectNewUpdates(selectNew, !selectNew, false/*selectTop*/);
            }
        });

        // placeholder between "select all" and "install"
        Label placeholder = new Label(mGroupOptions, SWT.NONE);
        GridDataBuilder.create(placeholder).hFill().hGrab();

        mButtonInstall = new Button(mGroupOptions, SWT.NONE);
        mButtonInstall.setText("");  //$NON-NLS-1$  placeholder, filled in updateButtonsState()
        mButtonInstall.setToolTipText("Install one or more packages");
        GridDataBuilder.create(mButtonInstall).vCenter().wHint(150);
        mButtonInstall.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onButtonInstall();  //$hide$
            }
        });

        // Options line 2, 7 columns

        new Label(mGroupOptions, SWT.NONE);

        mCheckFilterObsolete = new Button(mGroupOptions, SWT.CHECK);
        mCheckFilterObsolete.setText("Obsolete");
        mCheckFilterObsolete.setToolTipText("Also show obsolete packages");
        mCheckFilterObsolete.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                refreshViewerInput();
            }
        });
        mCheckFilterObsolete.setSelection(false);

        // placeholder before "deselect"
        new Label(mGroupOptions, SWT.NONE);
        new Label(mGroupOptions, SWT.NONE);

        Link linkDeselect = new Link(mGroupOptions, SWT.NONE);
        linkDeselect.setText("<a>Deselect All</a>");
        linkDeselect.setToolTipText("Deselects all the currently selected items");
        GridDataBuilder.create(linkDeselect).hFill();
        linkDeselect.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                super.widgetSelected(e);
                onDeselectAll();
            }
        });

        // placeholder between "deselect" and "delete"
        placeholder = new Label(mGroupOptions, SWT.NONE);
        GridDataBuilder.create(placeholder).hFill().hGrab();

        mButtonDelete = new Button(mGroupOptions, SWT.NONE);
        mButtonDelete.setText("");  //$NON-NLS-1$  placeholder, filled in updateButtonsState()
        mButtonDelete.setToolTipText("Delete one ore more installed packages");
        GridDataBuilder.create(mButtonDelete).vCenter().wHint(150);
        mButtonDelete.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onButtonDelete();  //$hide$
            }
        });
    }

    private PackagesPageImpl.ITreeViewerColumn wrapColumn(final TreeViewerColumn column) {
        return new PackagesPageImpl.ITreeViewerColumn() {
            @Override
            public void setLabelProvider(ColumnLabelProvider labelProvider) {
                column.setLabelProvider(labelProvider);
            }
        };
    }

    private Image getImage(String filename) {
        if (mImpl.mSwtUpdaterData != null) {
            ImageFactory imgFactory = mImpl.mSwtUpdaterData.getImageFactory();
            if (imgFactory != null) {
                return imgFactory.getImageByName(filename);
            }
        }
        return null;
    }


    // -- Start of internal part ----------
    // Hide everything down-below from SWT designer
    //$hide>>$


    // --- menu interactions ---

    public void registerMenuAction(final MenuAction action, MenuItem item) {
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                Button button = null;

                switch (action) {
                case RELOAD:
                    mImpl.fullReload();
                    break;
                case SHOW_ADDON_SITES:
                    AddonSitesDialog d = new AddonSitesDialog(getShell(), mImpl.mSwtUpdaterData);
                    if (d.open()) {
                        mImpl.loadPackages();
                    }
                    break;
                case TOGGLE_SHOW_ARCHIVES:
                    mDisplayArchives = !mDisplayArchives;
                    // Force the viewer to be refreshed
                    ((PkgContentProvider) mTreeViewer.getContentProvider()).
                        setDisplayArchives(mDisplayArchives);
                    mTreeViewer.setInput(null);
                    refreshViewerInput();
                    syncViewerSelection();
                    break;
                case TOGGLE_SHOW_INSTALLED_PKG:
                    button = mCheckFilterInstalled;
                    break;
                case TOGGLE_SHOW_OBSOLETE_PKG:
                    button = mCheckFilterObsolete;
                    break;
                case TOGGLE_SHOW_UPDATE_NEW_PKG:
                    button = mCheckFilterNew;
                    break;
                }

                if (button != null && !button.isDisposed()) {
                    // Toggle this button (radio or checkbox)

                    boolean value = button.getSelection();

                    // SWT doesn't automatically switch radio buttons when using the
                    // Widget#setSelection method, so we'll do it here manually.
                    if (!value && (button.getStyle() & SWT.RADIO) != 0) {
                        // we'll be selecting this radio button, so deselect all ther other ones
                        // in the parent group.
                        for (Control child : button.getParent().getChildren()) {
                            if (child instanceof Button &&
                                    child != button &&
                                    (child.getStyle() & SWT.RADIO) != 0) {
                                ((Button) child).setSelection(value);
                            }
                        }
                    }

                    button.setSelection(!value);

                    // SWT doesn't actually invoke the listeners when using Widget#setSelection
                    // so let's run the actual action.
                    button.notifyListeners(SWT.Selection, new Event());
                }

                updateMenuCheckmarks();
            }
        });

        mMenuActions.put(action, item);
    }

    // --- internal methods ---

    private void updateMenuCheckmarks() {

        for (Entry<MenuAction, MenuItem> entry : mMenuActions.entrySet()) {
            MenuAction action = entry.getKey();
            MenuItem item = entry.getValue();

            if (action.getMenuStyle() == SWT.NONE) {
                continue;
            }

            boolean value = false;
            Button button = null;

            switch (action) {
            case TOGGLE_SHOW_ARCHIVES:
                value = mDisplayArchives;
                break;
            case TOGGLE_SHOW_INSTALLED_PKG:
                button = mCheckFilterInstalled;
                break;
            case TOGGLE_SHOW_OBSOLETE_PKG:
                button = mCheckFilterObsolete;
                break;
            case TOGGLE_SHOW_UPDATE_NEW_PKG:
                button = mCheckFilterNew;
                break;
            case RELOAD:
            case SHOW_ADDON_SITES:
                // No checkmark to update
                break;
            }

            if (button != null && !button.isDisposed()) {
                value = button.getSelection();
            }

            if (!item.isDisposed()) {
                item.setSelection(value);
            }
        }
    }

    private void postCreate() {
        mImpl.postCreate();

        if (mImpl.mSwtUpdaterData != null) {
            mTextSdkOsPath.setText(mImpl.mSwtUpdaterData.getOsSdkRoot());
        }

        ((PkgContentProvider) mTreeViewer.getContentProvider()).setDisplayArchives(
                mDisplayArchives);

        ColumnViewerToolTipSupport.enableFor(mTreeViewer, ToolTip.NO_RECREATE);

        Tree tree = mTreeViewer.getTree();
        FontData fontData = tree.getFont().getFontData()[0];
        fontData.setStyle(SWT.ITALIC);
        mTreeFontItalic = new Font(tree.getDisplay(), fontData);

        tree.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                mTreeFontItalic.dispose();
                mTreeFontItalic = null;
            }
        });
    }

    private void loadPackages(boolean useLocalCache, boolean overrideExisting) {
        if (mImpl.mSwtUpdaterData == null) {
            return;
        }

        // LoadPackage is synchronous but does not block the UI.
        // Consequently it's entirely possible for the user
        // to request the app to close whilst the packages are loading. Any
        // action done after loadPackages must check the UI hasn't been
        // disposed yet. Otherwise hilarity ensues.

        if (mTreeColumnName.isDisposed()) {
            // If the UI got disposed, don't try to load anything since we won't be
            // able to display it anyway.
            return;
        }

        mTreeColumnName.setImage(getImage(PackagesPageIcons.ICON_SORT_BY_API));

        mImpl.loadPackagesImpl(useLocalCache, overrideExisting);
    }

    private void refreshViewerInput() {
        // Dynamically update the table while we load after each source.
        // Since the official Android source gets loaded first, it makes the
        // window look non-empty a lot sooner.
        if (!mGroupPackages.isDisposed()) {
            try {
                mImpl.setViewerInput();
            } catch (Exception ignore) {}

            // set the initial expanded state
            expandInitial(mTreeViewer.getInput());

            updateButtonsState();
            updateMenuCheckmarks();
        }
    }

    /**
     * Decide whether to keep an item in the current tree based on user-chosen filter options.
     */
    private boolean filterViewerItem(Object treeElement) {
        if (treeElement instanceof PkgCategory) {
            PkgCategory cat = (PkgCategory) treeElement;

            if (!cat.getItems().isEmpty()) {
                // A category is hidden if all of its content is hidden.
                // However empty categories are always visible.
                for (PkgItem item : cat.getItems()) {
                    if (filterViewerItem(item)) {
                        // We found at least one element that is visible.
                        return true;
                    }
                }
                return false;
            }
        }

        if (treeElement instanceof PkgItem) {
            PkgItem item = (PkgItem) treeElement;

            if (!mCheckFilterObsolete.getSelection()) {
                if (item.isObsolete()) {
                    return false;
                }
            }

            if (!mCheckFilterInstalled.getSelection()) {
                if (item.getState() == PkgState.INSTALLED) {
                    return false;
                }
            }

            if (!mCheckFilterNew.getSelection()) {
                if (item.getState() == PkgState.NEW || item.hasUpdatePkg()) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Performs the initial expansion of the tree. This expands categories that contain
     * at least one installed item and collapses the ones with nothing installed.
     *
     * TODO: change this to only change the expanded state on categories that have not
     * been touched by the user yet. Once we do that, call this every time a new source
     * is added or the list is reloaded.
     */
    private void expandInitial(Object elem) {
        if (elem == null) {
            return;
        }
        if (mTreeViewer != null && !mTreeViewer.getTree().isDisposed()) {

            boolean enablePreviews =
                mImpl.mSwtUpdaterData.getSettingsController().getSettings().getEnablePreviews();

            mTreeViewer.setExpandedState(elem, true);
            nextCategory: for (Object pkg :
                    ((ITreeContentProvider) mTreeViewer.getContentProvider()).
                        getChildren(elem)) {
                if (pkg instanceof PkgCategory) {
                    PkgCategory cat = (PkgCategory) pkg;

                    // Always expand the Tools category (and the preview one, if enabled)
                    if (cat.getKey().equals(PkgCategoryApi.KEY_TOOLS) ||
                            (enablePreviews &&
                                    cat.getKey().equals(PkgCategoryApi.KEY_TOOLS_PREVIEW))) {
                        expandInitial(pkg);
                        continue nextCategory;
                    }


                    for (PkgItem item : cat.getItems()) {
                        if (item.getState() == PkgState.INSTALLED) {
                            expandInitial(pkg);
                            continue nextCategory;
                        }
                    }
                }
            }
        }
    }

    /**
     * Handle checking and unchecking of the tree items.
     *
     * When unchecking, all sub-tree items checkboxes are cleared too.
     * When checking a source, all of its packages are checked too.
     * When checking a package, only its compatible archives are checked.
     */
    private void onTreeCheckStateChanged(CheckStateChangedEvent event) {
        boolean checked = event.getChecked();
        Object elem = event.getElement();

        assert event.getSource() == mTreeViewer;

        // When selecting, we want to only select compatible archives and expand the super nodes.
        checkAndExpandItem(elem, checked, true/*fixChildren*/, true/*fixParent*/);
        updateButtonsState();
    }

    private void onTreeDoubleClick(DoubleClickEvent event) {
        assert event.getSource() == mTreeViewer;
        ISelection sel = event.getSelection();
        if (sel.isEmpty() || !(sel instanceof ITreeSelection)) {
            return;
        }
        ITreeSelection tsel = (ITreeSelection) sel;
        Object elem = tsel.getFirstElement();
        if (elem == null) {
            return;
        }

        ITreeContentProvider provider =
            (ITreeContentProvider) mTreeViewer.getContentProvider();
        Object[] children = provider.getElements(elem);
        if (children == null) {
            return;
        }

        if (children.length > 0) {
            // If the element has children, expand/collapse it.
            if (mTreeViewer.getExpandedState(elem)) {
                mTreeViewer.collapseToLevel(elem, 1);
            } else {
                mTreeViewer.expandToLevel(elem, 1);
            }
        } else {
            // If the element is a terminal one, select/deselect it.
            checkAndExpandItem(
                    elem,
                    !mTreeViewer.getChecked(elem),
                    false /*fixChildren*/,
                    true /*fixParent*/);
            updateButtonsState();
        }
    }

    private void checkAndExpandItem(
            Object elem,
            boolean checked,
            boolean fixChildren,
            boolean fixParent) {
        ITreeContentProvider provider =
            (ITreeContentProvider) mTreeViewer.getContentProvider();

        // fix the item itself
        if (checked != mTreeViewer.getChecked(elem)) {
            mTreeViewer.setChecked(elem, checked);
        }
        if (elem instanceof PkgItem) {
            // update the PkgItem to reflect the selection
            ((PkgItem) elem).setChecked(checked);
        }

        if (!checked) {
            if (fixChildren) {
                // when de-selecting, we deselect all children too
                mTreeViewer.setSubtreeChecked(elem, checked);
                for (Object child : provider.getChildren(elem)) {
                    checkAndExpandItem(child, checked, fixChildren, false/*fixParent*/);
                }
            }

            // fix the parent when deselecting
            if (fixParent) {
                Object parent = provider.getParent(elem);
                if (parent != null && mTreeViewer.getChecked(parent)) {
                    mTreeViewer.setChecked(parent, false);
                }
            }
            return;
        }

        // When selecting, we also select sub-items (for a category)
        if (fixChildren) {
            if (elem instanceof PkgCategory || elem instanceof PkgItem) {
                Object[] children = provider.getChildren(elem);
                for (Object child : children) {
                    checkAndExpandItem(child, true, fixChildren, false/*fixParent*/);
                }
                // only fix the parent once the last sub-item is set
                if (elem instanceof PkgCategory) {
                    if (children.length > 0) {
                        checkAndExpandItem(
                                children[0], true, false/*fixChildren*/, true/*fixParent*/);
                    } else {
                        mTreeViewer.setChecked(elem, false);
                    }
                }
            } else if (elem instanceof Package) {
                // in details mode, we auto-select compatible packages
                selectCompatibleArchives(elem, provider);
            }
        }

        if (fixParent && checked && elem instanceof PkgItem) {
            Object parent = provider.getParent(elem);
            if (!mTreeViewer.getChecked(parent)) {
                Object[] children = provider.getChildren(parent);
                boolean allChecked = children.length > 0;
                for (Object e : children) {
                    if (!mTreeViewer.getChecked(e)) {
                        allChecked = false;
                        break;
                    }
                }
                if (allChecked) {
                    mTreeViewer.setChecked(parent, true);
                }
            }
        }
    }

    private void selectCompatibleArchives(Object pkg, ITreeContentProvider provider) {
        for (Object archive : provider.getChildren(pkg)) {
            if (archive instanceof Archive) {
                mTreeViewer.setChecked(archive, ((Archive) archive).isCompatible());
            }
        }
    }

    /**
     * Checks all PkgItems that are either new or have updates or select top platform
     * for initial run.
     */
    private void onSelectNewUpdates(boolean selectNew, boolean selectUpdates, boolean selectTop) {
        // This will update the tree's "selected" state and then invoke syncViewerSelection()
        // which will in turn update tree.
        mImpl.onSelectNewUpdates(selectNew, selectUpdates, selectTop);
    }

    /**
     * Deselect all checked PkgItems.
     */
    private void onDeselectAll() {
        // This does not update the tree itself, syncViewerSelection does it below.
        mImpl.onDeselectAll();
        syncViewerSelection();
    }

    /**
     * Synchronize the 'checked' state of PkgItems in the tree with their internal isChecked state.
     */
    private void syncViewerSelection() {
        ITreeContentProvider provider = (ITreeContentProvider) mTreeViewer.getContentProvider();

        Object input = mTreeViewer.getInput();
        if (input != null) {
            for (Object cat : provider.getElements(input)) {
                Object[] children = provider.getElements(cat);
                boolean allChecked = children.length > 0;
                for (Object child : children) {
                    if (child instanceof PkgItem) {
                        PkgItem item = (PkgItem) child;
                        boolean checked = item.isChecked();
                        allChecked &= checked;

                        if (checked != mTreeViewer.getChecked(item)) {
                            if (checked) {
                                if (!mTreeViewer.getExpandedState(cat)) {
                                    mTreeViewer.setExpandedState(cat, true);
                                }
                            }
                            checkAndExpandItem(
                                    item,
                                    checked,
                                    true/*fixChildren*/,
                                    false/*fixParent*/);
                        }
                    }
                }

                if (allChecked != mTreeViewer.getChecked(cat)) {
                    mTreeViewer.setChecked(cat, allChecked);
                }
            }
        }

        updateButtonsState();
    }

    /**
     * Indicate an install/delete operation is pending.
     * This disables the install/delete buttons.
     * Use {@link #endOperationPending()} to revert, typically in a {@code try..finally} block.
     */
    private void beginOperationPending() {
        mOperationPending = true;
        updateButtonsState();
    }

    private void endOperationPending() {
        mOperationPending = false;
        updateButtonsState();
    }

    /**
     * Updates the Install and Delete Package buttons.
     */
    private void updateButtonsState() {
        if (!mButtonInstall.isDisposed()) {
            int numPackages = getArchivesForInstall(null /*archives*/);

            mButtonInstall.setEnabled((numPackages > 0) && !mOperationPending);
            mButtonInstall.setText(
                    numPackages == 0 ? "Install packages..." :          // disabled button case
                        numPackages == 1 ? "Install 1 package..." :
                            String.format("Install %d packages...", numPackages));
        }

        if (!mButtonDelete.isDisposed()) {
            // We can only delete local archives
            int numPackages = getArchivesToDelete(null /*outMsg*/, null /*outArchives*/);

            mButtonDelete.setEnabled((numPackages > 0) && !mOperationPending);
            mButtonDelete.setText(
                    numPackages == 0 ? "Delete packages..." :           // disabled button case
                        numPackages == 1 ? "Delete 1 package..." :
                            String.format("Delete %d packages...", numPackages));
        }
    }

    /**
     * Called when the Install Package button is selected.
     * Collects the packages to be installed and shows the installation window.
     */
    private void onButtonInstall() {
        ArrayList<Archive> archives = new ArrayList<Archive>();
        getArchivesForInstall(archives);

        if (mImpl.mSwtUpdaterData != null) {
            boolean needsRefresh = false;
            try {
                beginOperationPending();

                List<Archive> installed = mImpl.mSwtUpdaterData.updateOrInstallAll_WithGUI(
                    archives,
                    mCheckFilterObsolete.getSelection() /* includeObsoletes */,
                    mContext == SdkInvocationContext.IDE ?
                            SwtUpdaterData.TOOLS_MSG_UPDATED_FROM_ADT :
                                SwtUpdaterData.TOOLS_MSG_UPDATED_FROM_SDKMAN);
                needsRefresh = installed != null && !installed.isEmpty();
            } finally {
                endOperationPending();

                if (needsRefresh) {
                    // The local package list has changed, make sure to refresh it
                    mImpl.localReload();
                }
            }
        }
    }

    /**
     * Selects the archives that can be installed.
     * This can be used with a null {@code outArchives} just to count the number of
     * installable archives.
     *
     * @param outArchives An archive list where to add the archives that can be installed.
     *   This can be null.
     * @return The number of archives that can be installed.
     */
    private int getArchivesForInstall(List<Archive> outArchives) {
        if (mTreeViewer == null ||
                mTreeViewer.getTree() == null ||
                mTreeViewer.getTree().isDisposed()) {
            return 0;
        }
        Object[] checked = mTreeViewer.getCheckedElements();
        if (checked == null) {
            return 0;
        }

        int count = 0;

        // Give us a way to force install of incompatible archives.
        boolean checkIsCompatible =
            System.getenv(ArchiveInstaller.ENV_VAR_IGNORE_COMPAT) == null;

        if (mDisplayArchives) {
            // In detail mode, we display archives so we can install only the
            // archives that are actually selected.

            for (Object c : checked) {
                if (c instanceof Archive) {
                    Archive a = (Archive) c;
                    if (a != null) {
                        if (checkIsCompatible && !a.isCompatible()) {
                            continue;
                        }
                        count++;
                        if (outArchives != null) {
                            outArchives.add((Archive) c);
                        }
                    }
                }
            }
        } else {
            // In non-detail mode, we install all the compatible archives
            // found in the selected pkg items. We also automatically
            // select update packages rather than the root package if any.

            for (Object c : checked) {
                Package p = null;
                if (c instanceof Package) {
                    // This is an update package
                    p = (Package) c;
                } else if (c instanceof PkgItem) {
                    p = ((PkgItem) c).getMainPackage();

                    PkgItem pi = (PkgItem) c;
                    if (pi.getState() == PkgState.INSTALLED) {
                        // We don't allow installing items that are already installed
                        // unless they have a pending update.
                        p = pi.getUpdatePkg();

                    } else if (pi.getState() == PkgState.NEW) {
                        p = pi.getMainPackage();
                    }
                }
                if (p != null) {
                    for (Archive a : p.getArchives()) {
                        if (a != null) {
                            if (checkIsCompatible && !a.isCompatible()) {
                                continue;
                            }
                            count++;
                            if (outArchives != null) {
                                outArchives.add(a);
                            }
                        }
                    }
                }
            }
        }

        return count;
    }

    /**
     * Called when the Delete Package button is selected.
     * Collects the packages to be deleted, prompt the user for confirmation
     * and actually performs the deletion.
     */
    private void onButtonDelete() {
        final String title = "Delete SDK Package";
        StringBuilder msg = new StringBuilder("Are you sure you want to delete:");

        // A list of archives to delete
        final ArrayList<Archive> archives = new ArrayList<Archive>();

        getArchivesToDelete(msg, archives);

        if (!archives.isEmpty()) {
            msg.append("\n").append("This cannot be undone.");  //$NON-NLS-1$
            if (MessageDialog.openQuestion(getShell(), title, msg.toString())) {
                try {
                    beginOperationPending();

                    mImpl.mSwtUpdaterData.getTaskFactory().start("Delete Package", new ITask() {
                        @Override
                        public void run(ITaskMonitor monitor) {
                            monitor.setProgressMax(archives.size() + 1);
                            for (Archive a : archives) {
                                monitor.setDescription("Deleting '%1$s' (%2$s)",
                                        a.getParentPackage().getShortDescription(),
                                        a.getLocalOsPath());

                                // Delete the actual package
                                a.deleteLocal();

                                monitor.incProgress(1);
                                if (monitor.isCancelRequested()) {
                                    break;
                                }
                            }

                            monitor.incProgress(1);
                            monitor.setDescription("Done");
                        }
                    });
                } finally {
                    endOperationPending();

                    // The local package list has changed, make sure to refresh it
                    mImpl.localReload();
                }
            }
        }
    }

    /**
     * Selects the archives that can be deleted and collect their names.
     * This can be used with a null {@code outArchives} and a null {@code outMsg}
     * just to count the number of archives to be deleted.
     *
     * @param outMsg A StringBuilder where the names of the packages to be deleted is
     *   accumulated. This is used to confirm deletion with the user.
     * @param outArchives An archive list where to add the archives that can be installed.
     *   This can be null.
     * @return The number of archives that can be deleted.
     */
    private int getArchivesToDelete(StringBuilder outMsg, List<Archive> outArchives) {
        if (mTreeViewer == null ||
                mTreeViewer.getTree() == null ||
                mTreeViewer.getTree().isDisposed()) {
            return 0;
        }
        Object[] checked = mTreeViewer.getCheckedElements();
        if (checked == null) {
            // This should not happen since the button should be disabled
            return 0;
        }

        int count = 0;

        if (mDisplayArchives) {
            // In detail mode, select archives that can be deleted

            for (Object c : checked) {
                if (c instanceof Archive) {
                    Archive a = (Archive) c;
                    if (a != null && a.isLocal()) {
                        count++;
                        if (outMsg != null) {
                            String osPath = a.getLocalOsPath();
                            File dir = new File(osPath);
                            Package p = a.getParentPackage();
                            if (p != null && dir.isDirectory()) {
                                outMsg.append("\n - ")    //$NON-NLS-1$
                                      .append(p.getShortDescription());
                            }
                        }
                        if (outArchives != null) {
                            outArchives.add(a);
                        }
                    }
                }
            }
        } else {
            // In non-detail mode, select archives of selected packages that can be deleted.

            for (Object c : checked) {
                if (c instanceof PkgItem) {
                    PkgItem pi = (PkgItem) c;
                    PkgState state = pi.getState();
                    if (state == PkgState.INSTALLED) {
                        Package p = pi.getMainPackage();

                        for (Archive a : p.getArchives()) {
                            if (a != null && a.isLocal()) {
                                count++;
                                if (outMsg != null) {
                                    String osPath = a.getLocalOsPath();
                                    File dir = new File(osPath);
                                    if (dir.isDirectory()) {
                                        outMsg.append("\n - ")    //$NON-NLS-1$
                                              .append(p.getShortDescription());
                                    }
                                }
                                if (outArchives != null) {
                                    outArchives.add(a);
                                }
                            }
                        }
                    }
                }
            }
        }

        return count;
    }

    // ----------------------


    // --- Implementation of ISdkChangeListener ---

    @Override
    public void onSdkLoaded() {
        onSdkReload();
    }

    @Override
    public void onSdkReload() {
        // The sdkmanager finished reloading its data. We must not call localReload() from here
        // since we don't want to alter the sdkmanager's data that just finished loading.
        mImpl.loadPackages();
    }

    @Override
    public void preInstallHook() {
        // nothing to be done for now.
    }

    @Override
    public void postInstallHook() {
        // nothing to be done for now.
    }


    // --- End of hiding from SWT Designer ---
    //$hide<<$
}

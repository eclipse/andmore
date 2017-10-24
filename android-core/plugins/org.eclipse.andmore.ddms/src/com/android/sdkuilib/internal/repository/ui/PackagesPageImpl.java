/*
 * Copyright (C) 2012 The Android Open Source Project
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

import com.android.SdkConstants;
import com.android.sdklib.AndroidVersion;
import com.android.sdklib.internal.repository.DownloadCache;
import com.android.sdklib.internal.repository.DownloadCache.Strategy;
import com.android.sdklib.internal.repository.archives.Archive;
import com.android.sdklib.internal.repository.packages.Package;
import com.android.sdklib.internal.repository.sources.SdkSource;
import com.android.sdklib.internal.repository.updater.PackageLoader;
import com.android.sdklib.internal.repository.updater.PackageLoader.ISourceLoadedCallback;
import com.android.sdklib.internal.repository.updater.PkgItem;
import com.android.sdklib.internal.repository.updater.PkgItem.PkgState;
import com.android.sdklib.repository.IDescription;
import com.android.sdkuilib.internal.repository.SwtUpdaterData;
import com.android.sdkuilib.internal.repository.core.PackagesDiffLogic;
import com.android.sdkuilib.internal.repository.core.PkgCategory;
import com.android.sdkuilib.internal.repository.core.PkgCategoryApi;
import com.android.sdkuilib.internal.repository.core.PkgContentProvider;
import com.android.sdkuilib.internal.repository.icons.ImageFactory;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IInputProvider;
import org.eclipse.jface.viewers.ITableFontProvider;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * Base class for {@link PackagesPage} that holds most of the logic to display
 * the tree/list of packages. This class holds most of the logic and {@link PackagesPage}
 * holds most of the UI (creating the UI, dealing with menus and buttons and tree
 * selection.) This makes it easier to test the functionality by mocking only a
 * subset of the UI.
 */
abstract class PackagesPageImpl {

    final SwtUpdaterData mSwtUpdaterData;
    final PackagesDiffLogic mDiffLogic;

    private ICheckboxTreeViewer mITreeViewer;
    private ITreeViewerColumn   mIColumnName;
    private ITreeViewerColumn   mIColumnApi;
    private ITreeViewerColumn   mIColumnRevision;
    private ITreeViewerColumn   mIColumnStatus;

    PackagesPageImpl(SwtUpdaterData swtUpdaterData) {
        mSwtUpdaterData = swtUpdaterData;
        mDiffLogic = new PackagesDiffLogic(swtUpdaterData);
    }

    /**
     * Utility method that derived classes can override to check whether the UI is disposed.
     * When the UI is disposed, most operations that affect the UI will be bypassed.
     * @return True if UI is not available and should not be touched.
     */
    abstract protected boolean isUiDisposed();

    /**
     * Utility method to execute a runnable on the main UI thread.
     * Will do nothing if {@link #isUiDisposed()} returns false.
     * @param runnable The runnable to execute on the main UI thread.
     */
    abstract protected void syncExec(Runnable runnable);

    /**
     * Synchronizes the 'checked' state of PkgItems in the tree with their internal isChecked state.
     */
    abstract protected void syncViewerSelection();

    void performFirstLoad() {
        // First a package loader is created that only checks
        // the local cache xml files. It populates the package
        // list based on what the client got last, essentially.
        loadPackages(true /*useLocalCache*/, false /*overrideExisting*/);

        // Next a regular package loader is created that will
        // respect the expiration and refresh parameters of the
        // download cache.
        loadPackages(false /*useLocalCache*/, true /*overrideExisting*/);
    }

    public void setITreeViewer(ICheckboxTreeViewer iTreeViewer) {
        mITreeViewer = iTreeViewer;
    }

    public void setIColumns(
            ITreeViewerColumn columnName,
            ITreeViewerColumn columnApi,
            ITreeViewerColumn columnRevision,
            ITreeViewerColumn columnStatus) {
        mIColumnName = columnName;
        mIColumnApi = columnApi;
        mIColumnRevision = columnRevision;
        mIColumnStatus = columnStatus;
    }

    void postCreate() {
        // Caller needs to call setITreeViewer before this.
        assert mITreeViewer     != null;
        // Caller needs to call setIColumns before this.
        assert mIColumnApi      != null;
        assert mIColumnName     != null;
        assert mIColumnStatus   != null;
        assert mIColumnRevision != null;

        mITreeViewer.setContentProvider(new PkgContentProvider(mITreeViewer));

        mIColumnApi.setLabelProvider(
                new PkgTreeColumnViewerLabelProvider(new PkgCellLabelProvider(mIColumnApi)));
        mIColumnName.setLabelProvider(
                new PkgTreeColumnViewerLabelProvider(new PkgCellLabelProvider(mIColumnName)));
        mIColumnStatus.setLabelProvider(
                new PkgTreeColumnViewerLabelProvider(new PkgCellLabelProvider(mIColumnStatus)));
        mIColumnRevision.setLabelProvider(
                new PkgTreeColumnViewerLabelProvider(new PkgCellLabelProvider(mIColumnRevision)));
    }

    /**
     * Performs a full reload by removing all cached packages data, including the platforms
     * and addons from the sdkmanager instance. This will perform a full local parsing
     * as well as a full reload of the remote data (by fetching all sources again.)
     */
    void fullReload() {
        // Clear all source information, forcing them to be refreshed.
        mSwtUpdaterData.getSources().clearAllPackages();
        // Clear and reload all local data too.
        localReload();
    }

    /**
     * Performs a full reload of all the local package information, including the platforms
     * and addons from the sdkmanager instance. This will perform a full local parsing.
     * <p/>
     * This method does NOT force a new fetch of the remote sources.
     *
     * @see #fullReload()
     */
    void localReload() {
        // Clear all source caches, otherwise loading will use the cached data
        mSwtUpdaterData.getLocalSdkParser().clearPackages();
        mSwtUpdaterData.getSdkManager().reloadSdk(mSwtUpdaterData.getSdkLog());
        loadPackages();
    }

    /**
     * Performs a "normal" reload of the package information, use the default download
     * cache and refreshing strategy as needed.
     */
    void loadPackages() {
        loadPackages(false /*useLocalCache*/, false /*overrideExisting*/);
    }

    /**
     * Performs a reload of the package information.
     *
     * @param useLocalCache When true, the {@link PackageLoader} is switched to use
     *  a specific {@link DownloadCache} using the {@link Strategy#ONLY_CACHE}, meaning
     *  it will only use data from the local cache. It will not try to fetch or refresh
     *  manifests. This is used once the very first time the sdk manager window opens
     *  and is typically followed by a regular load with refresh.
     */
    abstract protected void loadPackages(boolean useLocalCache, boolean overrideExisting);

    /**
     * Actual implementation of {@link #loadPackages(boolean, boolean)}.
     * Derived implementations must call this to do the actual work after setting up the UI.
     */
    void loadPackagesImpl(final boolean useLocalCache, final boolean overrideExisting) {
        if (mSwtUpdaterData == null) {
            return;
        }

        PackageLoader packageLoader = getPackageLoader(useLocalCache);
        assert packageLoader != null;

        mDiffLogic.updateStart();
        packageLoader.loadPackages(overrideExisting, new ISourceLoadedCallback() {
            @Override
            public boolean onUpdateSource(SdkSource source, Package[] newPackages) {
                // This runs in a thread and must not access UI directly.
                final boolean changed = mDiffLogic.updateSourcePackages(source, newPackages);

                syncExec(new Runnable() {
                    @Override
                    public void run() {
                        if (changed ||
                            mITreeViewer.getInput() != mDiffLogic.getCategories()) {
                                refreshViewerInput();
                        }
                    }
                });

                // Return true to tell the loader to continue with the next source.
                // Return false to stop the loader if any UI has been disposed, which can
                // happen if the user is trying to close the window during the load operation.
                return !isUiDisposed();
            }

            @Override
            public void onLoadCompleted() {
                // This runs in a thread and must not access UI directly.
                final boolean changed = mDiffLogic.updateEnd();

                syncExec(new Runnable() {
                    @Override
                    public void run() {
                        if (changed ||
                            mITreeViewer.getInput() != mDiffLogic.getCategories()) {
                            try {
                                refreshViewerInput();
                            } catch (Exception ignore) {}
                        }

                        if (!useLocalCache &&
                                mDiffLogic.isFirstLoadComplete() &&
                                !isUiDisposed()) {
                            // At the end of the first load, if nothing is selected then
                            // automatically select all new and update packages.
                            Object[] checked = mITreeViewer.getCheckedElements();
                            if (checked == null || checked.length == 0) {
                                onSelectNewUpdates(
                                        false, //selectNew
                                        true,  //selectUpdates,
                                        true); //selectTop
                            }
                        }
                    }
                });
            }
        });
    }

    /**
     * Used by {@link #loadPackagesImpl(boolean, boolean)} to get the package
     * loader for the first or second pass update. When starting the manager
     * starts with a first pass that reads only from the local cache, with no
     * extra network access. That's {@code useLocalCache} being true.
     * <p/>
     * Leter it does a second pass with {@code useLocalCache} set to false
     * and actually uses the download cache specified in {@link SwtUpdaterData}.
     *
     * This is extracted so that we can control this cache via unit tests.
     */
    protected PackageLoader getPackageLoader(boolean useLocalCache) {
        if (useLocalCache) {
            return new PackageLoader(mSwtUpdaterData, new DownloadCache(Strategy.ONLY_CACHE));
        } else {
            return mSwtUpdaterData.getPackageLoader();
        }
    }

    /**
     * Overridden by the UI to respond to a request to refresh the tree viewer
     * when the input has changed.
     * The implementation must call {@link #setViewerInput()} somehow and will
     * also need to adjust the expand state of the tree items and/or update
     * some buttons or other state.
     */
    abstract protected void refreshViewerInput();

    /**
     * Invoked from {@link #refreshViewerInput()} to actually either set the
     * input of the tree viewer or refresh it if it's the <em>same</em> input
     * object.
     */
    protected void setViewerInput() {
        List<PkgCategory> cats = mDiffLogic.getCategories();
        if (mITreeViewer.getInput() != cats) {
            // set initial input
            mITreeViewer.setInput(cats);
        } else {
            // refresh existing, which preserves the expanded state, the selection
            // and the checked state.
            mITreeViewer.refresh();
        }
    }

    /**
     * Checks all PkgItems that are either new or have updates or select top platform
     * for initial run.
     */
    void onSelectNewUpdates(boolean selectNew, boolean selectUpdates, boolean selectTop) {
        // This does not update the tree itself, syncViewerSelection does it in the caller.
        mDiffLogic.checkNewUpdateItems(
                selectNew,
                selectUpdates,
                selectTop,
                SdkConstants.CURRENT_PLATFORM);
        syncViewerSelection();
    }

    /**
     * Deselect all checked PkgItems.
     */
    void onDeselectAll() {
        // This does not update the tree itself, syncViewerSelection does it in the caller.
        mDiffLogic.uncheckAllItems();
    }

    // ----------------------

    abstract protected Font getTreeFontItalic();

    class PkgCellLabelProvider extends ColumnLabelProvider implements ITableFontProvider {

        private final ITreeViewerColumn mColumn;

        public PkgCellLabelProvider(ITreeViewerColumn column) {
            super();
            mColumn = column;
        }

        @Override
        public String getText(Object element) {

            if (mColumn == mIColumnName) {
                if (element instanceof PkgCategory) {
                    return ((PkgCategory) element).getLabel();
                } else if (element instanceof PkgItem) {
                    return getPkgItemName((PkgItem) element);
                } else if (element instanceof IDescription) {
                    return ((IDescription) element).getShortDescription();
                }

            } else if (mColumn == mIColumnApi) {
                AndroidVersion version = null;
                if (element instanceof PkgItem) {
                    version = ((PkgItem) element).getAndroidVersion();
                }
                if (version != null) {
                    return version.getApiString();
                }

            } else if (mColumn == mIColumnRevision) {
                if (element instanceof PkgItem) {
                    PkgItem pkg = (PkgItem) element;
                    return pkg.getRevision().toShortString();
                }

            } else if (mColumn == mIColumnStatus) {
                if (element instanceof PkgItem) {
                    PkgItem pkg = (PkgItem) element;

                    switch(pkg.getState()) {
                    case INSTALLED:
                        Package update = pkg.getUpdatePkg();
                        if (update != null) {
                            return String.format(
                                    "Update available: rev. %1$s",
                                    update.getRevision().toShortString());
                        }
                        return "Installed";

                    case NEW:
                        Package p = pkg.getMainPackage();
                        if (p != null && p.hasCompatibleArchive()) {
                            return "Not installed";
                        } else {
                            return String.format("Not compatible with %1$s",
                                    SdkConstants.currentPlatformName());
                        }
                    }
                    return pkg.getState().toString();

                } else if (element instanceof Package) {
                    // This is an update package.
                    return "New revision " + ((Package) element).getRevision().toShortString();
                }
            }

            return ""; //$NON-NLS-1$
        }

        private String getPkgItemName(PkgItem item) {
            String name = item.getName().trim();

            // When sorting by API, the package name might contains the API number
            // or the platform name at the end. If we find it, cut it out since it's
            // redundant.

            PkgCategoryApi cat = (PkgCategoryApi) findCategoryForItem(item);
            String apiLabel = cat.getApiLabel();
            String platLabel = cat.getPlatformName();

            if (platLabel != null && name.endsWith(platLabel)) {
                return name.substring(0, name.length() - platLabel.length());

            } else if (apiLabel != null && name.endsWith(apiLabel)) {
                return name.substring(0, name.length() - apiLabel.length());

            } else if (platLabel != null && item.isObsolete() && name.indexOf(platLabel) > 0) {
                // For obsolete items, the format is "<base name> <platform name> (Obsolete)"
                // so in this case only accept removing a platform name that is not at
                // the end.
                name = name.replace(platLabel, ""); //$NON-NLS-1$
            }

            // Collapse potential duplicated spacing
            name = name.replaceAll(" +", " "); //$NON-NLS-1$ //$NON-NLS-2$

            return name;
        }

        private PkgCategory findCategoryForItem(PkgItem item) {
            List<PkgCategory> cats = mDiffLogic.getCategories();
            for (PkgCategory cat : cats) {
                for (PkgItem i : cat.getItems()) {
                    if (i == item) {
                        return cat;
                    }
                }
            }

            return null;
        }

        @Override
        public Image getImage(Object element) {
            ImageFactory imgFactory = mSwtUpdaterData.getImageFactory();

            if (imgFactory != null) {
                if (mColumn == mIColumnName) {
                    if (element instanceof PkgCategory) {
                        return imgFactory.getImageForObject(((PkgCategory) element).getIconRef());
                    } else if (element instanceof PkgItem) {
                        return imgFactory.getImageForObject(((PkgItem) element).getMainPackage());
                    }
                    return imgFactory.getImageForObject(element);

                } else if (mColumn == mIColumnStatus && element instanceof PkgItem) {
                    PkgItem pi = (PkgItem) element;
                    switch(pi.getState()) {
                    case INSTALLED:
                        if (pi.hasUpdatePkg()) {
                            return imgFactory.getImageByName(PackagesPageIcons.ICON_PKG_UPDATE);
                        } else {
                            return imgFactory.getImageByName(PackagesPageIcons.ICON_PKG_INSTALLED);
                        }
                    case NEW:
                        Package p = pi.getMainPackage();
                        if (p != null && p.hasCompatibleArchive()) {
                            return imgFactory.getImageByName(PackagesPageIcons.ICON_PKG_NEW);
                        } else {
                            return imgFactory.getImageByName(PackagesPageIcons.ICON_PKG_INCOMPAT);
                        }
                    }
                }
            }
            return super.getImage(element);
        }

        // -- ITableFontProvider

        @Override
        public Font getFont(Object element, int columnIndex) {
            if (element instanceof PkgItem) {
                if (((PkgItem) element).getState() == PkgState.NEW) {
                    return getTreeFontItalic();
                }
            } else if (element instanceof Package) {
                // update package
                return getTreeFontItalic();
            }
            return super.getFont(element);
        }

        // -- Tooltip support

        @Override
        public String getToolTipText(Object element) {
            PkgItem pi = element instanceof PkgItem ? (PkgItem) element : null;
            if (pi != null) {
                element = pi.getMainPackage();
            }
            if (element instanceof IDescription) {
                String s = getTooltipDescription((IDescription) element);

                if (pi != null && pi.hasUpdatePkg()) {
                    s += "\n-----------------" +        //$NON-NLS-1$
                         "\nUpdate Available:\n" +      //$NON-NLS-1$
                         getTooltipDescription(pi.getUpdatePkg());
                }

                return s;
            }
            return super.getToolTipText(element);
        }

        private String getTooltipDescription(IDescription element) {
            String s = element.getLongDescription();
            if (element instanceof Package) {
                Package p = (Package) element;

                if (!p.isLocal()) {
                    // For non-installed item, try to find a download size
                    for (Archive a : p.getArchives()) {
                        if (!a.isLocal() && a.isCompatible()) {
                            s += '\n' + a.getSizeDescription();
                            break;
                        }
                    }
                }

                // Display info about where this package comes/came from
                SdkSource src = p.getParentSource();
                if (src != null) {
                    try {
                        URL url = new URL(src.getUrl());
                        String host = url.getHost();
                        if (p.isLocal()) {
                            s += String.format("\nInstalled from %1$s", host);
                        } else {
                            s += String.format("\nProvided by %1$s", host);
                        }
                    } catch (MalformedURLException ignore) {
                    }
                }
            }
            return s;
        }

        @Override
        public Point getToolTipShift(Object object) {
            return new Point(15, 5);
        }

        @Override
        public int getToolTipDisplayDelayTime(Object object) {
            return 500;
        }
    }

    interface ICheckboxTreeViewer extends IInputProvider {
        void setContentProvider(PkgContentProvider pkgContentProvider);
        void refresh();
        void setInput(List<PkgCategory> cats);
        Object[] getCheckedElements();
    }

    interface ITreeViewerColumn {
        void setLabelProvider(ColumnLabelProvider labelProvider);
    }
}

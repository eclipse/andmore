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


import com.android.sdklib.AndroidVersion;
import com.android.sdklib.internal.repository.packages.ExtraPackage;
import com.android.sdklib.internal.repository.packages.Package;
import com.android.sdklib.internal.repository.packages.PlatformPackage;
import com.android.sdklib.internal.repository.packages.PlatformToolPackage;
import com.android.sdklib.internal.repository.packages.ToolPackage;
import com.android.sdklib.internal.repository.sources.SdkSource;
import com.android.sdklib.internal.repository.updater.PackageLoader;
import com.android.sdklib.internal.repository.updater.SettingsController;
import com.android.sdklib.internal.repository.updater.PackageLoader.IAutoInstallTask;
import com.android.sdkuilib.internal.repository.SwtUpdaterData;
import com.android.sdkuilib.internal.repository.core.SdkLogAdapter;
import com.android.sdkuilib.internal.tasks.ProgressView;
import com.android.sdkuilib.internal.tasks.ProgressViewFactory;
import com.android.sdkuilib.ui.GridDataBuilder;
import com.android.sdkuilib.ui.GridLayoutBuilder;
import com.android.sdkuilib.ui.SwtBaseDialog;
import com.android.utils.ILogger;
import com.android.utils.Pair;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;

import java.io.File;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * This is a private implementation of UpdateWindow for ADT,
 * designed to install a very specific package.
 * <p/>
 * Example of usage:
 * <pre>
 * AdtUpdateDialog dialog = new AdtUpdateDialog(
 *     AdtPlugin.getDisplay().getActiveShell(),
 *     new AdtConsoleSdkLog(),
 *     sdk.getSdkLocation());
 *
 * Pair<Boolean, File> result = dialog.installExtraPackage(
 *     "android", "compatibility");  //$NON-NLS-1$ //$NON-NLS-2$
 * or
 * Pair<Boolean, File> result = dialog.installPlatformPackage(11);
 * </pre>
 */
public class AdtUpdateDialog extends SwtBaseDialog {

    public static final int USE_MAX_REMOTE_API_LEVEL = 0;

    private static final String APP_NAME = "Android SDK Manager";
    private final SwtUpdaterData mUpdaterData;

    private Boolean mResultCode = Boolean.FALSE;
    private Map<Package, File> mResultPaths = null;
    private SettingsController mSettingsController;
    private PackageFilter mPackageFilter;
    private PackageLoader mPackageLoader;

    private ProgressBar mProgressBar;
    private Label mStatusText;

    /**
     * Creates a new {@link AdtUpdateDialog}.
     * Callers will want to call {@link #installExtraPackage} or
     * {@link #installPlatformPackage} after this.
     *
     * @param parentShell The existing parent shell. Must not be null.
     * @param sdkLog An SDK logger. Must not be null.
     * @param osSdkRoot The current SDK root OS path. Must not be null or empty.
     */
    public AdtUpdateDialog(
            Shell parentShell,
            ILogger sdkLog,
            String osSdkRoot) {
        super(parentShell, SWT.NONE, APP_NAME);
        mUpdaterData = new SwtUpdaterData(osSdkRoot, sdkLog);
    }

    /**
     * Displays the update dialog and triggers installation of the requested {@code extra}
     * package with the specified vendor and path attributes.
     * <p/>
     * Callers must not try to reuse this dialog after this call.
     *
     * @param vendor The extra package vendor string to match.
     * @param path   The extra package path   string to match.
     * @return A boolean indicating whether the installation was successful (meaning the package
     *   was either already present, or got installed or updated properly) and a {@link File}
     *   with the path to the root folder of the package. The file is null when the boolean
     *   is false, otherwise it should point to an existing valid folder.
     * @wbp.parser.entryPoint
     */
    public Pair<Boolean, File> installExtraPackage(String vendor, String path) {
        mPackageFilter = createExtraFilter(vendor, path);
        open();

        File installPath = null;
        if (mResultPaths != null) {
            for (Entry<Package, File> entry : mResultPaths.entrySet()) {
                if (entry.getKey() instanceof ExtraPackage) {
                    installPath = entry.getValue();
                    break;
                }
            }
        }

        return Pair.of(mResultCode, installPath);
    }

    /**
     * Displays the update dialog and triggers installation of platform-tools package.
     * <p/>
     * Callers must not try to reuse this dialog after this call.
     *
     * @return A boolean indicating whether the installation was successful (meaning the package
     *   was either already present, or got installed or updated properly) and a {@link File}
     *   with the path to the root folder of the package. The file is null when the boolean
     *   is false, otherwise it should point to an existing valid folder.
     * @wbp.parser.entryPoint
     */
    public Pair<Boolean, File> installPlatformTools() {
        mPackageFilter = createPlatformToolsFilter();
        open();

        File installPath = null;
        if (mResultPaths != null) {
            for (Entry<Package, File> entry : mResultPaths.entrySet()) {
                if (entry.getKey() instanceof ExtraPackage) {
                    installPath = entry.getValue();
                    break;
                }
            }
        }

        return Pair.of(mResultCode, installPath);
    }

    /**
     * Displays the update dialog and triggers installation of the requested platform
     * package with the specified API  level.
     * <p/>
     * Callers must not try to reuse this dialog after this call.
     *
     * @param apiLevel The platform API level to match.
     *  The special value {@link #USE_MAX_REMOTE_API_LEVEL} means to use
     *  the highest API level available on the remote repository.
     * @return A boolean indicating whether the installation was successful (meaning the package
     *   was either already present, or got installed or updated properly) and a {@link File}
     *   with the path to the root folder of the package. The file is null when the boolean
     *   is false, otherwise it should point to an existing valid folder.
     */
    public Pair<Boolean, File> installPlatformPackage(int apiLevel) {
        mPackageFilter = createPlatformFilter(apiLevel);
        open();

        File installPath = null;
        if (mResultPaths != null) {
            for (Entry<Package, File> entry : mResultPaths.entrySet()) {
                if (entry.getKey() instanceof PlatformPackage) {
                    installPath = entry.getValue();
                    break;
                }
            }
        }

        return Pair.of(mResultCode, installPath);
    }

    /**
     * Displays the update dialog and triggers installation of a new SDK. This works by
     * requesting a remote platform package with the specified API levels as well as
     * the first tools or platform-tools packages available.
     * <p/>
     * Callers must not try to reuse this dialog after this call.
     *
     * @param apiLevels A set of platform API levels to match.
     *  The special value {@link #USE_MAX_REMOTE_API_LEVEL} means to use
     *  the highest API level available in the repository.
     * @return A boolean indicating whether the installation was successful (meaning the packages
     *   were either already present, or got installed or updated properly).
     */
    public boolean installNewSdk(Set<Integer> apiLevels) {
        mPackageFilter = createNewSdkFilter(apiLevels);
        open();
        return mResultCode.booleanValue();
    }

    @Override
    protected void createContents() {
        Shell shell = getShell();
        shell.setMinimumSize(new Point(450, 100));
        shell.setSize(450, 100);

        mUpdaterData.setWindowShell(shell);

        GridLayoutBuilder.create(shell).columns(1);

        Composite composite1 = new Composite(shell, SWT.NONE);
        composite1.setLayout(new GridLayout(1, false));
        GridDataBuilder.create(composite1).fill().grab();

        mProgressBar = new ProgressBar(composite1, SWT.NONE);
        GridDataBuilder.create(mProgressBar).hFill().hGrab();

        mStatusText = new Label(composite1, SWT.NONE);
        mStatusText.setText("Status Placeholder");  //$NON-NLS-1$ placeholder
        GridDataBuilder.create(mStatusText).hFill().hGrab();
    }

    @Override
    protected void postCreate() {
        ProgressViewFactory factory = new ProgressViewFactory();
        factory.setProgressView(new ProgressView(
                mStatusText,
                mProgressBar,
                null /*buttonStop*/,
                new SdkLogAdapter(mUpdaterData.getSdkLog())));
        mUpdaterData.setTaskFactory(factory);

        setupSources();
        initializeSettings();

        if (mUpdaterData.checkIfInitFailed()) {
            close();
            return;
        }

        mUpdaterData.broadcastOnSdkLoaded();

        mPackageLoader = new PackageLoader(mUpdaterData);
    }

    @Override
    protected void eventLoop() {
        mPackageLoader.loadPackagesWithInstallTask(
                mPackageFilter.installFlags(),
                new IAutoInstallTask() {
            @Override
            public Package[] filterLoadedSource(SdkSource source, Package[] packages) {
                for (Package pkg : packages) {
                    mPackageFilter.visit(pkg);
                }
                return packages;
            }

            @Override
            public boolean acceptPackage(Package pkg) {
                // Is this the package we want to install?
                return mPackageFilter.accept(pkg);
            }

            @Override
            public void setResult(boolean success, Map<Package, File> installPaths) {
                // Capture the result from the installation.
                mResultCode = Boolean.valueOf(success);
                mResultPaths = installPaths;
            }

            @Override
            public void taskCompleted() {
                // We can close that window now.
                close();
            }
        });

        super.eventLoop();
    }

    // -- Start of internal part ----------
    // Hide everything down-below from SWT designer
    //$hide>>$

    // --- Public API -----------


    // --- Internals & UI Callbacks -----------

    /**
     * Used to initialize the sources.
     */
    private void setupSources() {
        mUpdaterData.setupDefaultSources();
    }

    /**
     * Initializes settings.
     */
    private void initializeSettings() {
        mSettingsController = mUpdaterData.getSettingsController();
        mSettingsController.loadSettings();
        mSettingsController.applySettings();
    }

    // ----

    private static abstract class PackageFilter {
        /** Returns the installer flags for the corresponding mode. */
        abstract int installFlags();

        /** Visit a new package definition, in case we need to adjust the filter dynamically. */
        abstract void visit(Package pkg);

        /** Checks whether this is the package we've been looking for. */
        abstract boolean accept(Package pkg);
    }

    public static PackageFilter createExtraFilter(
            final String vendor,
            final String path) {
        return new PackageFilter() {
            String mVendor = vendor;
            String mPath = path;

            @Override
            boolean accept(Package pkg) {
                if (pkg instanceof ExtraPackage) {
                    ExtraPackage ep = (ExtraPackage) pkg;
                    if (ep.getVendorId().equals(mVendor)) {
                        // Check actual extra <path> field first
                        if (ep.getPath().equals(mPath)) {
                            return true;
                        }
                        // If not, check whether this is one of the <old-paths> values.
                        for (String oldPath : ep.getOldPaths()) {
                            if (oldPath.equals(mPath)) {
                                return true;
                            }
                        }
                    }
                }
                return false;
            }

            @Override
            void visit(Package pkg) {
                // nop
            }

            @Override
            int installFlags() {
                return SwtUpdaterData.TOOLS_MSG_UPDATED_FROM_ADT;
            }
        };
    }

    private PackageFilter createPlatformToolsFilter() {
        return new PackageFilter() {
            @Override
            boolean accept(Package pkg) {
                return pkg instanceof PlatformToolPackage;
            }

            @Override
            void visit(Package pkg) {
                // nop
            }

            @Override
            int installFlags() {
                return SwtUpdaterData.TOOLS_MSG_UPDATED_FROM_ADT;
            }
        };
    }

    public static PackageFilter createPlatformFilter(final int apiLevel) {
        return new PackageFilter() {
            int mApiLevel = apiLevel;
            boolean mFindMaxApi = apiLevel == USE_MAX_REMOTE_API_LEVEL;

            @Override
            boolean accept(Package pkg) {
                if (pkg instanceof PlatformPackage) {
                    PlatformPackage pp = (PlatformPackage) pkg;
                    AndroidVersion v = pp.getAndroidVersion();
                    return !v.isPreview() && v.getApiLevel() == mApiLevel;
                }
                return false;
            }

            @Override
            void visit(Package pkg) {
                // Try to find the max API in all remote packages
                if (mFindMaxApi &&
                        pkg instanceof PlatformPackage &&
                        !pkg.isLocal()) {
                    PlatformPackage pp = (PlatformPackage) pkg;
                    AndroidVersion v = pp.getAndroidVersion();
                    if (!v.isPreview()) {
                        int api = v.getApiLevel();
                        if (api > mApiLevel) {
                            mApiLevel = api;
                        }
                    }
                }
            }

            @Override
            int installFlags() {
                return SwtUpdaterData.TOOLS_MSG_UPDATED_FROM_ADT;
            }
        };
    }

    public static PackageFilter createNewSdkFilter(final Set<Integer> apiLevels) {
        return new PackageFilter() {
            int mMaxApiLevel;
            boolean mFindMaxApi = apiLevels.contains(USE_MAX_REMOTE_API_LEVEL);
            boolean mNeedTools = true;
            boolean mNeedPlatformTools = true;

            @Override
            boolean accept(Package pkg) {
                if (!pkg.isLocal()) {
                    if (pkg instanceof PlatformPackage) {
                        PlatformPackage pp = (PlatformPackage) pkg;
                        AndroidVersion v = pp.getAndroidVersion();
                        if (!v.isPreview()) {
                            int level = v.getApiLevel();
                            if ((mFindMaxApi && level == mMaxApiLevel) ||
                                    (level > 0 && apiLevels.contains(level))) {
                                return true;
                            }
                        }
                    } else if (mNeedTools && pkg instanceof ToolPackage) {
                        // We want a tool package. There should be only one,
                        // but in case of error just take the first one.
                        mNeedTools = false;
                        return true;
                    } else if (mNeedPlatformTools && pkg instanceof PlatformToolPackage) {
                        // We want a platform-tool package. There should be only one,
                        // but in case of error just take the first one.
                        mNeedPlatformTools = false;
                        return true;
                    }
                }
                return false;
            }

            @Override
            void visit(Package pkg) {
                // Try to find the max API in all remote packages
                if (mFindMaxApi &&
                        pkg instanceof PlatformPackage &&
                        !pkg.isLocal()) {
                    PlatformPackage pp = (PlatformPackage) pkg;
                    AndroidVersion v = pp.getAndroidVersion();
                    if (!v.isPreview()) {
                        int api = v.getApiLevel();
                        if (api > mMaxApiLevel) {
                            mMaxApiLevel = api;
                        }
                    }
                }
            }

            @Override
            int installFlags() {
                return SwtUpdaterData.NO_TOOLS_MSG;
            }
        };
    }



    // End of hiding from SWT Designer
    //$hide<<$

    // -----

}

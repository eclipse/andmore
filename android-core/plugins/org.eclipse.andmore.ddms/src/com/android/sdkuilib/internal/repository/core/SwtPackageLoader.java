/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.sdkuilib.internal.repository.core;

import com.android.annotations.NonNull;
import com.android.sdklib.internal.repository.DownloadCache;
import com.android.sdklib.internal.repository.updater.PackageLoader;
import com.android.sdkuilib.internal.repository.SwtUpdaterData;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * Loads packages fetched from the remote SDK Repository and keeps track
 * of their state compared with the current local SDK installation.
 */
public class SwtPackageLoader extends PackageLoader {

    /**
     * Creates a new PackageManager associated with the given {@link SwtUpdaterData}
     * and using the {@link SwtUpdaterData}'s default {@link DownloadCache}.
     *
     * @param swtUpdaterData The {@link SwtUpdaterData}. Must not be null.
     */
    public SwtPackageLoader(SwtUpdaterData swtUpdaterData) {
        super(swtUpdaterData);
    }

    /**
     * Creates a new PackageManager associated with the given {@link SwtUpdaterData}
     * but using the specified {@link DownloadCache} instead of the one from
     * {@link SwtUpdaterData}.
     *
     * @param swtUpdaterData The {@link SwtUpdaterData}. Must not be null.
     * @param cache The {@link DownloadCache} to use instead of the one from {@link SwtUpdaterData}.
     */
    public SwtPackageLoader(SwtUpdaterData swtUpdaterData, DownloadCache cache) {
        super(swtUpdaterData, cache);
    }

    /**
     * Runs the runnable on the UI thread using {@link Display#syncExec(Runnable)}.
     *
     * @param r Non-null runnable.
     */
    @Override
    protected void runOnUiThread(@NonNull Runnable r) {
        SwtUpdaterData swtUpdaterData = (SwtUpdaterData) getUpdaterData();
        Shell shell = swtUpdaterData.getWindowShell();

        if (shell != null && !shell.isDisposed()) {
            shell.getDisplay().syncExec(r);
        }
    }
}

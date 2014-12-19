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

package com.android.ide.eclipse.monitor;

import com.android.SdkConstants;
import com.android.ide.eclipse.ddms.IToolsLocator;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * {@link SdkToolsLocator} has two functions: <ul>
 *  <li> It implements all the methods of {@link IToolsLocator} interface, and
 *       so can be used as such. </li>
 *  <li> It provides the {@link #isValidInstallation()} method to check the validity
 *       of an installation. </li>
 *  The only reason this class does not explicitly implement the {@link IToolsLocator} interface
 *  is that it is used very early during the startup to check the validity of the installation.
 *  Actually implementing that interface causes other bundles to be activated before the
 *  Eclipse Platform is fully initialized, resulting in startup error.
 */
public class SdkToolsLocator {
    public static final String PLATFORM_EXECUTABLE_EXTENSION =
            (SdkConstants.CURRENT_PLATFORM == SdkConstants.PLATFORM_WINDOWS) ?
                    ".exe" : ""; //$NON-NLS-1$

    public static final String PLATFORM_SCRIPT_EXTENSION =
            (SdkConstants.CURRENT_PLATFORM == SdkConstants.PLATFORM_WINDOWS) ?
                    ".bat" : ""; //$NON-NLS-1$

    public static final String FN_HPROF_CONV = "hprof-conv" + PLATFORM_EXECUTABLE_EXTENSION; //$NON-NLS-1$
    public static final String FN_TRACEVIEW = "traceview" + PLATFORM_SCRIPT_EXTENSION; //$NON-NLS-1$

    private final File mSdkFolder;

    public SdkToolsLocator(File sdkFolder) {
        mSdkFolder = sdkFolder;
    }

    public String getAdbLocation() {
        return new File(getSdkPlatformToolsFolder(), SdkConstants.FN_ADB).getAbsolutePath();
    }

    public String getTraceViewLocation() {
        return new File(getSdkToolsFolder(), FN_TRACEVIEW).getAbsolutePath();
    }

    public String getHprofConvLocation() {
        return new File(getSdkPlatformToolsFolder(), FN_HPROF_CONV).getAbsolutePath();
    }

    private String getSdkToolsFolder() {
        return new File(mSdkFolder, SdkConstants.FD_TOOLS).getAbsolutePath();
    }

    private String getSdkPlatformToolsFolder() {
        return new File(mSdkFolder, SdkConstants.FD_PLATFORM_TOOLS).getAbsolutePath();
    }

    public SdkInstallStatus isValidInstallation() {
        List<String> executables = Arrays.asList(
                getTraceViewLocation(),
                getHprofConvLocation());

        for (String exe : executables) {
            File f = new File(exe);
            if (!f.exists()) {
                return SdkInstallStatus.invalidInstallation(exe + " not present.");
            }
            if (!f.canExecute()) {
                return SdkInstallStatus.invalidInstallation(exe + " is not executable.");
            }
        }

        return SdkInstallStatus.VALID;
    }

    public static class SdkInstallStatus {
        private boolean mValid;
        private String mCause;

        private SdkInstallStatus(boolean valid, String errorMessage) {
            mValid = valid;
            mCause = errorMessage;
        }

        public boolean isValid() {
            return mValid;
        }

        public String getErrorMessage() {
            return mCause;
        }

        public static final SdkInstallStatus VALID = new SdkInstallStatus(true, "");

        public static SdkInstallStatus invalidInstallation(String errorMessage) {
            return new SdkInstallStatus(false, errorMessage);
        }
    }
}

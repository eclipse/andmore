/*
 * Copyright (C) 2010 The Android Open Source Project
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

package org.eclipse.andmore.internal.build;

import com.android.sdklib.build.ApkBuilder.JarStatus;

/**
 * Exception throw when native libraries are detected in jar file.
 *
 */
public final class NativeLibInJarException extends Exception {
    private static final long serialVersionUID = 1L;

    private final JarStatus mStatus;
    private final String mLibName;
    private final String[] mConsoleMsgs;

    NativeLibInJarException(JarStatus status, String message, String libName,
            String[] consoleMsgs) {
        super(message);
        mStatus = status;
        mLibName = libName;
        mConsoleMsgs = consoleMsgs;
    }

    /**
     * Returns the {@link JarStatus} object containing the information about the libraries that
     * were found.
     */
    public JarStatus getStatus() {
        return mStatus;
    }

    /**
     * Returns the name of the jar file containing the native libraries.
     */
    public String getJarName() {
        return mLibName;
    }

    /**
     * Returns additional information that should be shown to the user.
     */
    public String[] getAdditionalInfo() {
        return mConsoleMsgs;
    }
}

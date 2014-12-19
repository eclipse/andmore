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

package org.eclipse.andmore.internal.sdk;

import org.eclipse.andmore.AdtPlugin;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.utils.ILogger;

/**
 * An {@link ILogger} logger that outputs to the ADT console.
 */
public class AdtConsoleSdkLog implements ILogger {

    private static final String TAG = "SDK Manager"; //$NON-NLS-1$

    @Override
    public void error(@Nullable Throwable t, @Nullable String errorFormat, Object... args) {
        if (t != null) {
            AdtPlugin.logAndPrintError(t, TAG, "Error: " + errorFormat, args);
        } else {
            AdtPlugin.printErrorToConsole(TAG, String.format(errorFormat, args));
        }
    }

    @Override
    public void info(@NonNull String msgFormat, Object... args) {
        String msg = String.format(msgFormat, args);
        for (String s : msg.split("\n")) {
            if (s.trim().length() > 0) {
                AdtPlugin.printToConsole(TAG, s);
            }
        }
    }

    @Override
    public void verbose(@NonNull String msgFormat, Object... args) {
        info(msgFormat, args);
    }

    @Override
    public void warning(@NonNull String warningFormat, Object... args) {
        AdtPlugin.printToConsole(TAG, String.format("Warning: " + warningFormat, args));
    }
}

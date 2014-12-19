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

package org.eclipse.andmore.internal.build.builders;

import com.android.annotations.NonNull;

import org.apache.tools.ant.types.selectors.SelectorUtils;
import org.eclipse.core.runtime.IPath;

/**
 * Collection of file path or path patterns to be checked for changes.
 *
 * All paths should be relative to the project they belong to.
 * Patterns can use Ant-type glob patterns.
 *
 * This is an immutable class that does not store any info beyond the list of paths. This is to
 * be used in conjunction with {@link PatternBasedDeltaVisitor}.
 */
class ChangedFileSet {

    private final String mLogName;

    private final String[] mInputs;
    private String mOutput;

    ChangedFileSet(String logName, String... inputs) {
        mLogName = logName;
        mInputs = inputs;
    }

    public void setOutput(@NonNull String output) {
        mOutput = output;
    }

    public boolean isInput(@NonNull String path, @NonNull IPath iPath) {
        for (String i : mInputs) {
            if (SelectorUtils.matchPath(i, path)) {
                return true;
            }
        }

        return false;
    }

    public boolean isOutput(@NonNull String path, @NonNull IPath iPath) {
        if (mOutput != null) {
            return SelectorUtils.matchPath(mOutput, path);
        }

        return false;
    }

    public String getLogName() {
        return mLogName;
    }
}

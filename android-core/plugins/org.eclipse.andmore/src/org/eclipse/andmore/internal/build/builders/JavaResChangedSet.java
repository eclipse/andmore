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
import com.android.sdklib.build.ApkBuilder;

import org.eclipse.core.runtime.IPath;

/**
 * Custom {@link ChangedFileSet} for java resources.
 *
 * This builds the set of inputs to be all the source folders of the given project,
 * and excludes files that won't be packaged.
 * This exclusion can't be easily described as a glob-pattern so it's overriding the default
 * behavior instead.
 *
 */
class JavaResChangedSet extends ChangedFileSet {

    JavaResChangedSet(String logName, String... inputs) {
        super(logName, inputs);
    }

    @Override
    public boolean isInput(@NonNull String path, @NonNull IPath iPath) {
        if (!ApkBuilder.checkFileForPackaging(iPath.lastSegment(), iPath.getFileExtension())) {
            return false;
        }
        return super.isInput(path, iPath);
    }
}

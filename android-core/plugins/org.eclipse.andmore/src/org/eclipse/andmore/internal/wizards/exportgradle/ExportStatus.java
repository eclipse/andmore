/*
 * Copyright (C) 2013 The Android Open Source Project
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

package org.eclipse.andmore.internal.wizards.exportgradle;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import java.io.File;

public class ExportStatus {

    public static enum FileStatus { OK, VCS_FAILURE, IO_FAILURE; }

    private String mMainError = null;
    private final Multimap<FileStatus, File> mFileStatus = ArrayListMultimap.create();

    void addFileStatus(@NonNull FileStatus status, @NonNull File file) {
        mFileStatus.put(status, file);
    }

    boolean hasError() {
        return mMainError != null ||
                !mFileStatus.get(FileStatus.VCS_FAILURE).isEmpty() ||
                !mFileStatus.get(FileStatus.IO_FAILURE).isEmpty();
    }

    public void setErrorMessage(String error) {
        mMainError = error;
    }

    @Nullable
    public String getErrorMessage() {
        return mMainError;
    }

    @NonNull
    public Multimap<FileStatus, File> getFileStatus() {
        return mFileStatus;
    }
}

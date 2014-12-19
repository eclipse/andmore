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

package org.eclipse.andmore.internal.build;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.sdklib.build.RenderScriptChecker;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.core.resources.IFile;

import java.io.File;
import java.io.IOException;
import java.util.Set;

public class RsSourceChangeHandler implements SourceChangeHandler {

    private final RenderScriptChecker mChecker;
    private boolean mIsCheckerLoaded = false;

    private boolean mMustCompile = false;

    public RsSourceChangeHandler(@NonNull RenderScriptChecker checker) {
        mChecker = checker;
    }

    @Override
    public boolean handleGeneratedFile(IFile file, int kind) {
        if (mMustCompile) {
            return false;
        }

        if (!mIsCheckerLoaded) {
            try {
                mChecker.loadDependencies();
            } catch (IOException e) {
                // failed to load the dependency files, force a compilation, log the error.
                AdtPlugin.log(e, "Failed to read dependency files");
                mMustCompile = true;
                return false;
            }
        }

        Set<File> oldOutputs = mChecker.getOldOutputs();
        // mustCompile is always false here.
        mMustCompile = oldOutputs.contains(file.getLocation().toFile());
        return mMustCompile;
    }

    @Override
    public void handleSourceFile(IFile file, int kind) {
        if (mMustCompile) {
            return;
        }

        String ext = file.getFileExtension();
        if (SdkConstants.EXT_RS.equals(ext) ||
                SdkConstants.EXT_FS.equals(ext) ||
                SdkConstants.EXT_RSH.equals(ext)) {
            mMustCompile = true;
        }
    }

    public boolean mustCompile() {
        return mMustCompile;
    }

    @NonNull
    public RenderScriptChecker getChecker() {
        return mChecker;
    }

    public void prepareFullBuild() {
        mMustCompile = true;
    }
}

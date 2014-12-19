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
import com.google.common.collect.Lists;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;

import java.util.List;

/**
 * A configured Gradle module for export. This includes gradle path, dependency, type, etc...
 */
public class GradleModule {

    @NonNull
    private final IJavaProject mJavaProject;

    private String mPath;
    private Type mType;

    private final List<GradleModule> mDependencies = Lists.newArrayList();

    public static enum Type { ANDROID, JAVA };

    GradleModule(@NonNull IJavaProject javaProject) {
        mJavaProject = javaProject;
    }

    @NonNull
    public IJavaProject getJavaProject() {
        return mJavaProject;
    }

    @NonNull
    public IProject getProject() {
        return mJavaProject.getProject();
    }

    boolean isConfigured() {
        return mType != null;
    }

    public void setType(Type type) {
        mType = type;
    }

    public Type getType() {
        return mType;
    }

    public void addDependency(GradleModule module) {
        mDependencies.add(module);
    }

    public List<GradleModule> getDependencies() {
        return mDependencies;
    }

    public void setPath(String path) {
        mPath = path;
    }

    public String getPath() {
        return mPath;
    }

    @Override
    public String toString() {
        return "GradleModule [mJavaProject=" + mJavaProject + ", mPath=" + mPath + ", mType="
                + mType + ", mDependencies=" + mDependencies + "]";
    }
}


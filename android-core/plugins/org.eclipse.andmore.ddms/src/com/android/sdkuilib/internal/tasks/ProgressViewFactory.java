/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.sdkuilib.internal.tasks;

import com.android.sdklib.internal.repository.ITask;
import com.android.sdklib.internal.repository.ITaskFactory;
import com.android.sdklib.internal.repository.ITaskMonitor;

/**
 * An {@link ITaskFactory} that creates a new {@link ProgressTask} dialog
 * for each new task.
 */
public final class ProgressViewFactory implements ITaskFactory {

    private ProgressView mProgressView;

    public ProgressViewFactory() {
    }

    public void setProgressView(ProgressView progressView) {
        mProgressView = progressView;
    }

    @Override
    public void start(String title, ITask task) {
        start(title, null /*monitor*/, task);
    }

    @Override
    public void start(String title, ITaskMonitor parentMonitor, ITask task) {
        assert mProgressView != null;
        mProgressView.startTask(title, parentMonitor, task);
    }
}

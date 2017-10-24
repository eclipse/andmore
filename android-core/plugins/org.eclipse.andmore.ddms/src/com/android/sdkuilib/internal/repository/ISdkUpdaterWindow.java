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

package com.android.sdkuilib.internal.repository;

import com.android.sdklib.repository.ISdkChangeListener;

/**
 * Interface for the actual implementation of the Update Window.
 */
public interface ISdkUpdaterWindow {

    /**
     * Adds a new listener to be notified when a change is made to the content of the SDK.
     */
    public abstract void addListener(ISdkChangeListener listener);

    /**
     * Removes a new listener to be notified anymore when a change is made to the content of
     * the SDK.
     */
    public abstract void removeListener(ISdkChangeListener listener);

    /**
     * Opens the window.
     */
    public abstract void open();

}

/*
 * Copyright (C) 2018 The Android Open Source Project
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
package org.eclipse.andmore.service;

import org.eclipse.core.runtime.jobs.Job;

/**
 * Operates service to queue Jobs and run them when the application is in running state 
 */
public interface StartupService {
    /**
     * Inserts the specified Job into the service queue, waiting if necessary
     * for space to become available. The queue can be accessed before the service starts.
     *
     * @param job The Job to add
     * @throws InterruptedException if interrupted while waiting
     * @throws ClassCastException if the class of the specified element
     *         prevents it from being added to this queue
     * @throws NullPointerException if the specified element is null
     * @throws IllegalArgumentException if some property of the specified
     *         element prevents it from being added to this queue
     */
    public void put(Job job) throws InterruptedException;

    /**
     * Start service
     * @param jobCount Number of jobs to be run before stopping
     */
    public void start(int jobCount);
    
    /**
     * Stop service
     */
    public void stop();

}

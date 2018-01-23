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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.eclipse.core.runtime.jobs.Job;

public class AdtStartupService implements StartupService {
    public static int MAX_QUEUE_LENGTH = 16;
    
    private BlockingQueue<Job> jobQueue;
    private Thread consumeThread;
    private static AdtStartupService singleton;

    /**
     * Private constructor for singleton implementation
     */
    private AdtStartupService() {
        jobQueue = new LinkedBlockingQueue<Job>(MAX_QUEUE_LENGTH);
    }

    // ---- Implements StartupService ----

	@Override
	public void put(Job job) throws InterruptedException {
        jobQueue.put(job);
	}

	@Override
	public void start(int jobCount) {
		if ((consumeThread != null) && consumeThread.isAlive())
			return;
        Runnable comsumeTask = new Runnable()
        {
            @Override
            public void run() 
            {
            	int count = jobCount;
                while (true)
                {
                    try 
                    {
                        jobQueue.take().schedule();
                        if ((count > 0) && (--count == 0))
                        	break;
                    } 
                    catch (InterruptedException e) 
                    {
                        break;
                    }
                }
            }
        };
        consumeThread = new Thread(comsumeTask, "Startup Service");
        consumeThread.start();
	}

	@Override
	public void stop() {
		if (consumeThread != null)
			consumeThread.interrupt();
	}
	
	public static AdtStartupService instance() {
		if (singleton == null) {
			synchronized(AdtStartupService.class) {
				if (singleton == null) {
					singleton = new AdtStartupService();
				}
			}
		}
		return singleton;
	}
}

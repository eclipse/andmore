/*
 * Copyright (C) 2012 The Android Open Source Project
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
package org.eclipse.andmore.android.db.core.ui;

import org.eclipse.core.runtime.jobs.Job;

/**
 * This class represents a job that must be used in order to load the model in
 * background. Caller must implement the run method with all logic to load the
 * node children. There's no need to take care of loading node and/or setting
 * the node loading flag. There's also no need to add a jobChange listener.
 */
public abstract class AbstractLoadingNodeJob extends Job {
	protected ITreeNode node = null;

	public AbstractLoadingNodeJob(String name, ITreeNode node) {
		super(name);
		this.node = node;
		addJobChangeListener(new LoadingJobListener());
	}

	/**
	 * @return the node
	 */
	public ITreeNode getNode() {
		return node;
	}

}
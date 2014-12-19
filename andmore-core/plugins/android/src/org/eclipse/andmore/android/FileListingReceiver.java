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
package org.eclipse.andmore.android;

import java.util.ArrayList;
import java.util.List;

import com.android.ddmlib.FileListingService;
import com.android.ddmlib.FileListingService.FileEntry;
import com.android.ddmlib.FileListingService.IListingReceiver;

/**
 * Receive ddms notifications about file listing and notify the listener
 */
class FileListingReceiver implements IListingReceiver {
	private final List<String> availableSegments;

	private final FileListingService service;

	private final IDatabaseListingListener listener;

	private final List<String> databases;

	public FileListingReceiver(List<String> segments, FileListingService fileListingService,
			IDatabaseListingListener listener) {
		this.availableSegments = segments;
		this.service = fileListingService;
		this.listener = listener;
		this.databases = new ArrayList<String>();
	}

	@Override
	public void refreshEntry(FileEntry fileentry) {
		// do nothing
	}

	@Override
	public void setChildren(FileEntry fileentry, FileEntry[] afileentry) {
		if (availableSegments.size() > 0) {
			String theSegment = availableSegments.remove(0);
			FileEntry entry = fileentry.findChild(theSegment);
			if (entry != null) {
				service.getChildren(entry, false, new FileListingReceiver(availableSegments, service, listener));
			} else {
				notifyListeners();
			}
		} else {
			for (FileEntry entry : afileentry) {
				databases.add(entry.getName());
			}
			notifyListeners();
		}
	}

	private void notifyListeners() {
		this.listener.databasesFound(databases);
	}

}
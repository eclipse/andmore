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
package org.eclipse.andmore.android.videos.model;

import java.util.List;

/**
 * Class that represents a single channel / playlist
 */
public class VideoChannel {

	/*
	 * Attributes
	 */
	private String name = null;

	private String displayName = null;

	private boolean ordered = false;

	private boolean defaultChannel = false;

	private List<Video> videos = null;

	private int visibleVideos = 0;

	private boolean active = true;

	/*
	 * Methods
	 */
	public int getVisibleVideos() {
		return visibleVideos;
	}

	public void setVisibleVideos(int visibleVideos) {
		this.visibleVideos = visibleVideos;
	}

	public VideoChannel(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<Video> getVideos() {
		return videos;
	}

	public void setVideos(List<Video> videos) {
		this.videos = videos;
	}

	public boolean isOrdered() {
		return ordered;
	}

	public void setOrdered(boolean ordered) {
		this.ordered = ordered;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public boolean isDefaultChannel() {
		return defaultChannel;
	}

	public void setDefaultChannel(boolean defaultChannel) {
		this.defaultChannel = defaultChannel;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

}

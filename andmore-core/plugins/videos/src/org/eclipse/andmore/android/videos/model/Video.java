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

import java.net.URL;
import java.util.Date;
import java.util.List;

/**
 * Class that represents a single Video
 */
public class Video {

	/*
	 * Attributes
	 */
	private String id = null;

	private String title = null;

	private String description = null;

	private Date date = null;

	private float rating = 0;

	private long views = 0;

	private URL snapshot = null;

	private String embeddedLink = null;

	private String externalLink = null;

	private boolean visible = true;

	private VideoChannel channel = null;

	private Object data = null;

	private List<String> keywords = null;

	/*
	 * Methods
	 */
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public float getRating() {
		return rating;
	}

	public void setRating(float rating) {
		this.rating = rating;
	}

	public long getViews() {
		return views;
	}

	public void setViews(long views) {
		this.views = views;
	}

	public URL getSnapshot() {
		return snapshot;
	}

	public void setSnapshot(URL snapshot) {
		this.snapshot = snapshot;
	}

	public String getEmbeddedLink() {
		return embeddedLink;
	}

	public void setEmbeddedLink(String embeddedLink) {
		this.embeddedLink = embeddedLink;
	}

	public String getExternalLink() {
		return externalLink;
	}

	public void setExternalLink(String externalLink) {
		this.externalLink = externalLink;
	}

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	public VideoChannel getChannel() {
		return channel;
	}

	public void setChannel(VideoChannel channel) {
		this.channel = channel;
	}

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}

	public List<String> getKeywords() {
		return keywords;
	}

	public void setKeywords(List<String> keywords) {
		this.keywords = keywords;
	}

}

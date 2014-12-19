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
package org.eclipse.andmore.android.videos.model.extension;

import java.util.List;

import org.eclipse.andmore.android.videos.model.Video;
import org.eclipse.andmore.android.videos.model.VideoChannel;

/**
 * Define an interface that can be used to connect video service providers. This
 * way, the videos displayed in the view can come from different service
 * providers
 */
public interface VideoServiceProvider {

	/**
	 * Load the videos of the given channel / playlist
	 * 
	 * @param channel
	 *            the channel / playlist of the videos that must be retrieved
	 * @return all videos in the channel / playlist
	 * @throws Exception
	 */
	public List<Video> loadVideos(VideoChannel channel) throws Exception;

	/**
	 * Rate a video
	 * 
	 * @param video
	 *            the video to be rated
	 * @param rate
	 *            rate from 1 to 5
	 */
	public void rate(Video video, int rate) throws Exception;

}

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
package org.eclipse.andmore.android.videos.implementation.youtube;

import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.common.proxy.ProxyAuthenticator;
import org.eclipse.andmore.android.videos.model.Video;
import org.eclipse.andmore.android.videos.model.VideoChannel;
import org.eclipse.andmore.android.videos.model.extension.VideoServiceProvider;
import org.eclipse.core.internal.net.ProxyManager;
import org.eclipse.core.net.proxy.IProxyService;

import com.google.gdata.client.youtube.YouTubeService;
import com.google.gdata.data.extensions.Rating;
import com.google.gdata.data.youtube.PlaylistEntry;
import com.google.gdata.data.youtube.PlaylistFeed;
import com.google.gdata.data.youtube.PlaylistLinkEntry;
import com.google.gdata.data.youtube.PlaylistLinkFeed;
import com.google.gdata.data.youtube.VideoEntry;

/**
 * Implements the methods to retrieve videos from YouTube
 */
public class YoutubeVideoServiceProvider implements VideoServiceProvider {

	private String youtubeUser = null;

	/*
	 * YouTube Service
	 */
	private YouTubeService youtubeService = null;

	private final String YOUTUBE_SERVICE_APP_ID = "motodev-studio-for-android";

	/*
	 * YouTube Service URIs
	 */
	public static final String YOUTUBE_GDATA_SERVER = "http://gdata.youtube.com";

	// change <user> by the real YouTube username
	private final String ALL_PLAYLISTS_FEED = YOUTUBE_GDATA_SERVER + "/feeds/api/users/<user>/playlists";

	/*
	 * Map "playlist name" -> object from Youtube API that represent the
	 * playlist
	 */
	private final Map<String, PlaylistLinkEntry> allPlaylistsMap = new HashMap<String, PlaylistLinkEntry>();

	/**
	 * Initialize the YouTube Service
	 */
	public YoutubeVideoServiceProvider(String user) throws Exception {
		// Try to retrieve proxy configuration to use if necessary
		IProxyService proxyService = ProxyManager.getProxyManager();
		if (proxyService.isProxiesEnabled() || proxyService.isSystemProxiesEnabled()) {
			Authenticator.setDefault(new ProxyAuthenticator());
		}

		youtubeUser = user;
		youtubeService = new YouTubeService(YOUTUBE_SERVICE_APP_ID);
		PlaylistLinkFeed feeds = youtubeService.getFeed(new URL(ALL_PLAYLISTS_FEED.replace("<user>", youtubeUser)),
				PlaylistLinkFeed.class);
		for (PlaylistLinkEntry feedEntry : feeds.getEntries()) {
			allPlaylistsMap.put(feedEntry.getTitle().getPlainText(), feedEntry);
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.motorola.studio.android.videos.model.extension.VideoServiceProvider
	 * #loadVideos(java.util.List)
	 */
	@Override
	public List<Video> loadVideos(VideoChannel channel) throws Exception {

		List<Video> videos = null;

		PlaylistLinkEntry entry = allPlaylistsMap.get(channel.getName());

		if (entry != null) {
			// Get videos in the playlist
			String playlistUrl = entry.getFeedUrl();
			PlaylistFeed playlistFeed = youtubeService.getFeed(new URL(playlistUrl), PlaylistFeed.class);

			videos = new ArrayList<Video>();
			for (PlaylistEntry playlistEntry : playlistFeed.getEntries()) {
				videos.add(getVideoInstance(playlistEntry));
			}
		}

		return videos;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.motorola.studio.android.videos.model.VideoManager#rate(com.motorola
	 * .studio.android.videos.model.Video, int)
	 */
	@Override
	public void rate(Video video, int rate) throws Exception {

		VideoEntry youtubeVideo = (VideoEntry) video.getData();
		String ratingUrl = youtubeVideo.getRatingLink().getHref();
		Rating myRating = new Rating();
		myRating.setValue(rate);
		myRating.setMax(5);
		myRating.setMin(1);
		youtubeVideo.setRating(myRating);

		youtubeService.insert(new URL(ratingUrl), youtubeVideo);

	}

	/**
	 * Create and populate an object of Video type, with the information from
	 * the VideoEntry passed as argument
	 * 
	 * @param videoEntry
	 *            the VideoEntry object to be converted
	 * @return the corresponding Video instance
	 */
	private Video getVideoInstance(VideoEntry videoEntry) {
		Video video = new Video();

		video.setId(videoEntry.getId());
		video.setTitle(videoEntry.getTitle().getPlainText());
		video.setDescription(videoEntry.getMediaGroup().getDescription() != null ? videoEntry.getMediaGroup()
				.getDescription().getPlainTextContent() : "");
		video.setEmbeddedLink(videoEntry.getMediaGroup().getPlayer().getUrl().replace("watch?v=", "embed/")
				.replace("&feature=youtube_gdata_player", ""));
		video.setExternalLink(videoEntry.getMediaGroup().getPlayer().getUrl());
		video.setRating(videoEntry.getRating() != null ? videoEntry.getRating().getAverage() : 0);
		video.setDate(new Date(videoEntry.getUpdated().getValue()));
		video.setViews(videoEntry.getStatistics() != null ? videoEntry.getStatistics().getViewCount() : 0);
		video.setData(videoEntry);
		video.setKeywords(videoEntry.getMediaGroup().getKeywords() != null ? videoEntry.getMediaGroup().getKeywords()
				.getKeywords() : null);

		try {
			video.setSnapshot(new URL(videoEntry.getMediaGroup().getThumbnails().get(0).getUrl()));
		} catch (MalformedURLException e) {
			StudioLogger.error(this.getClass(), "Error while retrieving video snapshot", e);
		}

		return video;

	}
}

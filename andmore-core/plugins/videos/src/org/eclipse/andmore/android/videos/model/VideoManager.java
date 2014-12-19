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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.common.utilities.HttpUtils;
import org.eclipse.andmore.android.videos.Activator;
import org.eclipse.andmore.android.videos.implementation.youtube.YoutubeVideoServiceProvider;
import org.eclipse.andmore.android.videos.model.extension.VideoServiceProvider;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Manage the actions related to videos, including loading them and providing
 * any related information to other classes.
 */
public class VideoManager {

	/*
	 * Sort options
	 */
	public static final int SORT_MOST_RECENT = 0;

	public static final int SORT_MOST_VIEWED = 1;

	public static final int SORT_TOP_RATED = 2;

	/*
	 * Video channels and source definitions (XML)
	 */
	public static final String VIDEOS_DEFINITIONS = "resources/motodev_videos.xml";

	public static final String TAG_USER = "user";

	public static final String TAG_USER_ATT_NAME = "name";

	public static final String TAG_CHANNEL = "channel";

	public static final String TAG_CHANNEL_ATT_NAME = "name";

	public static final String TAG_CHANNEL_ATT_DISPLAY_NAME = "display_name";

	// "fixed" or "variable"
	public static final String TAG_CHANNEL_ATT_ORDER = "order";

	public static final String TAG_CHANNEL_ATT_ORDER_FIXED = "fixed";

	// "true" or "false"
	public static final String TAG_CHANNEL_ATT_DEFAULT = "default";

	/*
	 * Singleton
	 */
	private static VideoManager instance;

	/*
	 * The selected video service provider
	 */
	private VideoServiceProvider videoServiceProvider = null;

	/*
	 * Credentials information to be passed to the video service provider
	 */
	private String userName = null;

	/*
	 * The video channel objects (the videos themselves are attributes of these
	 * objects)
	 */
	private String defaultVideoChannel = null;

	// Map "channel name" -> object that represent the video channel
	private final Map<String, VideoChannel> channelsMap = new HashMap<String, VideoChannel>();

	private final List<VideoChannel> channelsList = new ArrayList<VideoChannel>();

	/**
	 * Singleton
	 * 
	 * @return a unique VideoManager instance
	 * @throws Exception
	 */
	public static synchronized VideoManager getInstance() throws Exception {
		if (instance == null) {
			instance = new VideoManager();
		}
		return instance;
	}

	/**
	 * Load all channels and videos
	 */
	public void load() throws Exception {
		channelsMap.clear();
		channelsList.clear();

		File file = new File(FileLocator.toFileURL(Activator.getDefault().getBundle().getEntry(VIDEOS_DEFINITIONS))
				.getPath());

		DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document doc = documentBuilder.parse(file);

		/*
		 * Get service username
		 */
		NodeList user = doc.getElementsByTagName(TAG_USER);
		Node userNameNode = user.item(0).getAttributes().getNamedItem(TAG_USER_ATT_NAME);
		userName = userNameNode.getNodeValue();

		/*
		 * Initialize video service provider
		 */
		videoServiceProvider = new YoutubeVideoServiceProvider(userName);

		/*
		 * Get all channels
		 */
		NodeList channels = doc.getElementsByTagName(TAG_CHANNEL);

		for (int i = 0; i < channels.getLength(); i++) {

			Node channel = channels.item(i);
			String displayName = channel.getAttributes().getNamedItem(TAG_CHANNEL_ATT_DISPLAY_NAME).getTextContent();
			String name = channel.getAttributes().getNamedItem(TAG_CHANNEL_ATT_NAME).getTextContent();
			boolean order = false;
			boolean defaultChannel = false;
			if (channel.getAttributes().getNamedItem(TAG_CHANNEL_ATT_ORDER) != null) {
				order = channel.getAttributes().getNamedItem(TAG_CHANNEL_ATT_ORDER).getTextContent()
						.equals(TAG_CHANNEL_ATT_ORDER_FIXED) ? true : false;
			}
			if (channel.getAttributes().getNamedItem(TAG_CHANNEL_ATT_DEFAULT) != null) {
				defaultChannel = channel.getAttributes().getNamedItem(TAG_CHANNEL_ATT_DEFAULT).getTextContent()
						.equals(new Boolean(true).toString()) ? true : false;
			}

			/*
			 * Create and populate the channels
			 */
			VideoChannel videoChannel = new VideoChannel(name);
			videoChannel.setDisplayName(displayName);
			videoChannel.setOrdered(order);
			videoChannel.setDefaultChannel(defaultChannel);
			if (defaultChannel) {
				defaultVideoChannel = name;
			}
			List<Video> videos = videoServiceProvider.loadVideos(videoChannel);
			if (videos != null) {
				videoChannel.setVideos(videos);
			} else {
				videoChannel.setActive(false);
			}
			channelsMap.put(name, videoChannel);
			channelsList.add(videoChannel);

		}

	}

	/**
	 * Sort all videos from all channels
	 * 
	 * @param criteria
	 */
	public void sort(final int criteria) {
		for (VideoChannel channel : channelsList) {
			if (!channel.isOrdered()) {
				List<Video> videos = channel.getVideos();
				Collections.sort(videos, new Comparator<Video>() {

					@Override
					public int compare(Video arg0, Video arg1) {
						int result = 0;

						switch (criteria) {
						case VideoManager.SORT_MOST_RECENT:
							result = arg0.getDate().compareTo(arg1.getDate());
							break;
						case VideoManager.SORT_MOST_VIEWED:
							result = arg0.getViews() >= arg1.getViews() ? 1 : -1;
							break;
						case VideoManager.SORT_TOP_RATED:
							result = arg0.getRating() >= arg1.getRating() ? 1 : -1;
							break;

						}
						return result;
					}
				});
			}
		}
	}

	/**
	 * Get videos from a channel
	 * 
	 * @param channel
	 *            channel to retrieve the videos
	 */
	public List<Video> getVideos(VideoChannel channel) {
		List<Video> videos = null;
		if (channelsMap.get(channel.getName()) != null) {
			videos = channelsMap.get(channel.getName()).getVideos();
		}
		return videos;
	}

	/**
	 * Get all channels
	 * 
	 * @return all channels
	 */
	public List<VideoChannel> getChannels() {
		return channelsList;
	}

	/**
	 * Get only the active channels
	 * 
	 * @return the active channels, which were found by the video service
	 *         provider
	 */
	public List<VideoChannel> getActiveChannels() {
		List<VideoChannel> activeVideoChannels = new ArrayList<VideoChannel>();
		for (VideoChannel channel : channelsList) {
			if (channel.isActive()) {
				activeVideoChannels.add(channel);
			}
		}
		return activeVideoChannels;
	}

	/**
	 * Get the default channel, to be selected in the UI by default
	 * 
	 * @return the default channel
	 */
	public VideoChannel getDefaultChannel() {
		return channelsMap.get(defaultVideoChannel);
	}

	/**
	 * Rate a video
	 * 
	 * @param video
	 *            video to be rated
	 * @param rate
	 *            video rate
	 */
	public void rate(Video video, int rate) throws Exception {
		// delegate to the video service provider
		videoServiceProvider.rate(video, rate);
	}

	/**
	 * Search all videos from all channels
	 * 
	 * @param keyword
	 *            search keyword
	 * @return only the videos that match the keyword
	 */
	public List<Video> search(String keyword) {

		List<Video> selectedVideos = new ArrayList<Video>();

		// split the keyword into tokens
		String[] tokens = keyword.split("\\s");
		int numberOfTokens = tokens.length;

		/*
		 * For each channel, search the videos and try to find all tokens on
		 * each video
		 */
		int visibleVideosInChannel;
		for (VideoChannel channel : channelsList) {
			visibleVideosInChannel = 0;
			for (Video video : channel.getVideos()) {

				int tokensFound = 0;

				if (numberOfTokens > 0) {
					StringBuffer sb = new StringBuffer();
					sb.append(video.getTitle());
					sb.append(" ");
					sb.append(video.getDescription());
					sb.append(" ");
					sb.append(video.getKeywords());

					for (int i = 0; i < tokens.length; i++) {
						String token = tokens[i];
						if (sb.toString().toUpperCase().contains(token.toUpperCase())) {
							tokensFound++;
						}
					}
				}

				// if all tokens were found, mark the video as visible ...
				if (tokensFound == numberOfTokens) {
					video.setVisible(true);
					visibleVideosInChannel++;
					selectedVideos.add(video);
				}
				// ... otherwise, mark the video as not visible
				else {
					video.setVisible(false);
				}
			}
			channel.setVisibleVideos(visibleVideosInChannel);
		}

		return selectedVideos;

	}

	/**
	 * Get the video thumbnail from the internet, given its URL
	 * 
	 * @param url
	 *            video URL
	 * @return the file representing the downloaded thumbnail
	 */
	public File getThumbnail(URL url) {

		File cacheIconFile = null;
		HttpUtils httpUtils = new HttpUtils();
		InputStream inStream = null;
		FileOutputStream outStream = null;
		try {
			inStream = httpUtils.getInputStreamForUrl(url.toString(), new NullProgressMonitor());

			File remoteFile = new File(url.toString());
			String[] remoteFileName = remoteFile.getName().split("\\.");
			cacheIconFile = File.createTempFile(remoteFileName[0], "." + remoteFileName[1]);
			cacheIconFile.deleteOnExit();

			outStream = new FileOutputStream(cacheIconFile);

			byte[] buf = new byte[1024];
			int len;
			while ((len = inStream.read(buf)) > 0) {
				outStream.write(buf, 0, len);
			}

		} catch (Exception e) {
			StudioLogger.error(this.getClass(), "Error while retrieving video thumbnail", e);
		} finally {
			try {
				inStream.close();
				outStream.close();
			} catch (Exception e) {
				StudioLogger.error(e.getMessage());
			}
		}

		return cacheIconFile;
	}

}

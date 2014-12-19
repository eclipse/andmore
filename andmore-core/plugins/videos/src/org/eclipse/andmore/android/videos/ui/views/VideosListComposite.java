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
package org.eclipse.andmore.android.videos.ui.views;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.andmore.android.videos.i18n.VideosNLS;
import org.eclipse.andmore.android.videos.model.Video;
import org.eclipse.andmore.android.videos.model.VideoChannel;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * A specialized Composite that shows all videos in a list. The videos are
 * organized in tabs, given that each tab represent a different channel /
 * playlist
 */
public class VideosListComposite extends Composite {

	/*
	 * The channels that are being represented
	 */
	private List<VideoChannel> videoChannels = null;

	/*
	 * The selected video in the list
	 */
	private String selectedVideo = null;

	/*
	 * The default channel
	 */
	private String defaultVideoChannelName = null;

	/*
	 * Widgets in the composite
	 */
	private final List<Composite> tabCompositeList = new ArrayList<Composite>(); // List
																					// of
																					// the
																					// parent
																					// composites
																					// for
																					// each
																					// tab

	private final Map<String, VideoComposite> videoCompositeMap = new HashMap<String, VideoComposite>(); // Map
																											// "video id"
																											// ->
																											// composite
																											// that
																											// represents
																											// that
																											// video

	private final Map<String, Composite> noVideosCompositeMap = new HashMap<String, Composite>(); // Map
																									// "channel name"
																									// ->
																									// "no videos"
																									// composite

	/**
	 * Constructor responsible for creating the entire component
	 * 
	 * @param parent
	 *            the parent composite
	 * @param style
	 *            SWT style
	 * @param videoChannels
	 *            all video channels with their respective videos already
	 *            populated
	 * @param defaultVideoChannelName
	 *            the name of the default video channel (to be selected in the
	 *            UI)
	 */
	public VideosListComposite(Composite parent, int style, List<VideoChannel> videoChannels,
			String defaultVideoChannelName) {
		super(parent, style);
		this.videoChannels = videoChannels;
		this.defaultVideoChannelName = defaultVideoChannelName;
		createControls();
	}

	/**
	 * Change the style for the video being played
	 * 
	 * @param video
	 *            the video to be selected
	 */
	public void setSelectedVideo(Video video) {

		VideoComposite composite;

		if (video != null) {

			// deselect the current selected video
			if (selectedVideo != null) {
				composite = videoCompositeMap.get(selectedVideo);
				if (composite != null) {
					composite.deselect();
				}
			}

			// update the selection
			selectedVideo = video.getId();
			composite = videoCompositeMap.get(video.getId());
			if (composite != null) {
				composite.select();
			}

		} else {
			selectedVideo = null;
		}

	}

	/**
	 * Highlight the given keyword in the video composites being displayed at
	 * this moment
	 * 
	 * @param keyword
	 *            the keyword to be highlighted
	 */
	public void highlightKeywords(String keyword) {
		for (Map.Entry<String, VideoComposite> entry : videoCompositeMap.entrySet()) {
			VideoComposite videoComposite = entry.getValue();
			if (videoComposite.getVideo().isVisible()) {
				videoComposite.highlightKeyword(keyword);
			}
		}
	}

	/**
	 * Create all widgets of this composite
	 */
	private void createControls() {

		// set the composite layout
		setLayout(new GridLayout(1, false));

		CTabFolder tabFolder = new CTabFolder(this, SWT.NONE);
		tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tabFolder.setBorderVisible(true);
		tabFolder.setFont(new Font(getShell().getDisplay(), getShell().getDisplay().getSystemFont().getFontData()[0]
				.getName(), 10, SWT.BOLD));

		tabCompositeList.clear();
		videoCompositeMap.clear();

		for (VideoChannel channel : videoChannels) {
			CTabItem item = new CTabItem(tabFolder, SWT.NONE);
			item.setText(channel.getDisplayName());
			item.setData(channel);
			Composite itemComposite = new Composite(tabFolder, SWT.NONE);
			itemComposite.setLayout(new GridLayout(1, false));
			itemComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			Composite videosScrolledComposite = populateVideos(itemComposite, channel);
			// store tab item
			videosScrolledComposite.setData(item);
			tabCompositeList.add(videosScrolledComposite);
			item.setControl(itemComposite);
			if (channel.getName().equals(defaultVideoChannelName)) {
				tabFolder.setSelection(item);
			}
		}
	}

	/**
	 * Populate one tab (given the parent composite) with its videos
	 * 
	 * @param parent
	 * @param videos
	 */
	private Composite populateVideos(Composite parent, VideoChannel channel) {

		/*
		 * Create the scrolled composite
		 */
		final ScrolledComposite videosScrolledComposite = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL);
		videosScrolledComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		videosScrolledComposite.setLayout(new GridLayout(1, false));

		/*
		 * Create the composite which will be the parent of each video
		 * representation composite
		 */
		final Composite videosComposite = new Composite(videosScrolledComposite, SWT.NONE);
		videosComposite.setLayout(new GridLayout(1, false));
		videosComposite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));

		createNoVideosComposite(videosComposite, channel.getName());

		/*
		 * For each video, create its composite
		 */
		for (final Video video : channel.getVideos()) {
			VideoComposite videoComposite = new VideoComposite(this, videosComposite, videosScrolledComposite,
					SWT.NONE, video);
			videoComposite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
			videoCompositeMap.put(video.getId(), videoComposite);
		}

		videosScrolledComposite.setContent(videosComposite);
		videosScrolledComposite.setExpandVertical(true);
		videosScrolledComposite.setExpandHorizontal(true);
		videosScrolledComposite.setAlwaysShowScrollBars(true);
		videosScrolledComposite.getVerticalBar().setIncrement(20);
		videosScrolledComposite.getVerticalBar().setPageIncrement(100);
		videosScrolledComposite.getHorizontalBar().setIncrement(20);
		videosScrolledComposite.getHorizontalBar().setPageIncrement(100);

		videosScrolledComposite.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				videosScrolledComposite.setMinSize(videosComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			}
		});

		return videosScrolledComposite;

	}

	/**
	 * Implement this
	 */
	public void sort() {
		/*
		 * // change all parents setParent(getSite().getShell()); // add in the
		 * order setParent(mainComposite);
		 */
	}

	/**
	 * Filter the videos being displayed and show only the ones passed as
	 * parameter. Also identify if all videos are in the list, so that the
	 * appropriate actions are executed.
	 * 
	 * @param videos
	 *            the list of videos to remain in the screen
	 * @param all
	 *            whether all videos videos are in the list or not
	 */
	public void filter(List<Video> videos, boolean all) {
		/*
		 * Hide all items
		 */
		for (Map.Entry<String, VideoComposite> composite : videoCompositeMap.entrySet()) {
			GridData layoutData = (GridData) composite.getValue().getLayoutData();
			if (layoutData != null) {
				layoutData.exclude = true;
			}
			composite.getValue().setVisible(false);
		}

		/*
		 * Show only items that match
		 */
		for (Video video : videos) {
			((GridData) videoCompositeMap.get(video.getId()).getLayoutData()).exclude = false;
			Composite composite = videoCompositeMap.get(video.getId());
			composite.setVisible(true);
		}

		/*
		 * Update tab name accordingly, and also display or hide the "no videos"
		 * message
		 */
		for (Composite composite : tabCompositeList) {

			CTabItem tabItem = (CTabItem) composite.getData();
			VideoChannel videoChannel = (VideoChannel) tabItem.getData();

			if (!all) {
				tabItem.setText(videoChannel.getDisplayName() + " (" + videoChannel.getVisibleVideos() + ")");
			} else {
				tabItem.setText(videoChannel.getDisplayName());
			}

			/*
			 * Display or hide the "No Videos" composite
			 */
			if (videoChannel.getVisibleVideos() == 0) {
				displayNoVideosComposite(videoChannel.getName());
			} else {
				hideNoVideosComposite(videoChannel.getName());
			}

			// update scroll bar size
			((ScrolledComposite) composite)
					.setMinSize(composite.getChildren()[0].computeSize(SWT.DEFAULT, SWT.DEFAULT));

		}

		// force the parent layout, as well as the layout of all children
		getParent().layout(true, true);

	}

	/**
	 * Create a composite that will be displayed when no videos are found due to
	 * a search
	 * 
	 * @param parent
	 *            the parent composite
	 * @param channelName
	 *            the channel name
	 */
	private void createNoVideosComposite(Composite parent, String channelName) {

		Composite noVideosComposite = new Composite(parent, SWT.NONE);
		GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, false);
		layoutData.exclude = true;
		noVideosComposite.setLayoutData(layoutData);
		noVideosComposite.setLayout(new FillLayout());
		noVideosComposite.setVisible(false);

		Label noVideosLabel = new Label(noVideosComposite, SWT.NONE);
		noVideosLabel.setText(VideosNLS.UI_No_Videos_Search);

		noVideosCompositeMap.put(channelName, noVideosComposite);

	}

	/**
	 * Display the composite that states that no videos are found due to a
	 * search
	 * 
	 * @param channelName
	 *            the channel name
	 */
	private void displayNoVideosComposite(String channelName) {
		Composite noVideosComposite = noVideosCompositeMap.get(channelName);
		if (noVideosComposite != null) {
			((GridData) noVideosComposite.getLayoutData()).exclude = false;
			noVideosComposite.setVisible(true);
		}

	}

	/**
	 * Hide the composite that states that no videos are found due to a search
	 * 
	 * @param channelName
	 *            the channel name
	 */
	private void hideNoVideosComposite(String channelName) {

		Composite noVideosComposite = noVideosCompositeMap.get(channelName);
		if (noVideosComposite != null) {
			((GridData) noVideosComposite.getLayoutData()).exclude = true;
			noVideosComposite.setVisible(false);
		}

	}
}

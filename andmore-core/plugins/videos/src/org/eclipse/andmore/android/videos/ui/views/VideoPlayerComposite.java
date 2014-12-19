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

import org.eclipse.andmore.android.videos.model.Video;
import org.eclipse.core.internal.net.ProxyManager;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.AuthenticationEvent;
import org.eclipse.swt.browser.AuthenticationListener;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * This composite represents the player area on the MOTODEV videos view
 * 
 */
@SuppressWarnings("restriction")
public class VideoPlayerComposite extends Composite {
	private final int PLAYER_MAXIMUM_WIDTH = 560;

	private final double PLAYER_HEIGHT_RATE = 0.7;

	private static final int BROWSER_MIN_WIDTH = 250;

	private static final int BROWSER_COMPOSITE_MIN_WIDTH = BROWSER_MIN_WIDTH + 50;

	private boolean sashResized = false;

	private ScrolledComposite scrolledComposite;

	private Composite mainComposite;

	private Label globalVideoTitle;

	public VideoPlayerComposite(Composite parent) {
		super(parent, SWT.NONE);
	}

	/*
	 * Media Player
	 */
	private Browser browser;

	public Browser getBrowser() {
		return browser;
	}

	public Label getGlobalVideoTitle() {
		return globalVideoTitle;
	}

	public void setVideoTitleFont(Font font) {
		globalVideoTitle.setFont(font);
	}

	public void createVideoPlayerArea(Font fontForLabel) {
		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 1;
		layout.marginHeight = 1;
		this.setLayout(layout);
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		this.setVisible(false);

		/*
		 * Setup the scrolled composite
		 */
		scrolledComposite = new ScrolledComposite(this, SWT.H_SCROLL | SWT.V_SCROLL);
		scrolledComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		scrolledComposite.setLayout(new GridLayout(1, false));

		mainComposite = new Composite(scrolledComposite, SWT.NONE);
		mainComposite.setLayout(new GridLayout(1, false));
		mainComposite.setVisible(false);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		data.widthHint = BROWSER_COMPOSITE_MIN_WIDTH;
		data.minimumWidth = BROWSER_COMPOSITE_MIN_WIDTH;
		data.exclude = true;
		mainComposite.setLayoutData(data);

		globalVideoTitle = new Label(mainComposite, SWT.WRAP);
		globalVideoTitle.setFont(fontForLabel);
		globalVideoTitle.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

		browser = new Browser(mainComposite, SWT.NO_SCROLL);

		browser.setVisible(false);
		browser.setSize(new Point(0, 0));
		browser.setJavascriptEnabled(true);

		data = new GridData(SWT.NONE, SWT.NONE, false, false);
		data.widthHint = BROWSER_MIN_WIDTH;
		data.minimumWidth = BROWSER_MIN_WIDTH;
		data.exclude = true;
		browser.setLayoutData(data);

		// set proxy user and password, if needed
		browser.addAuthenticationListener(new AuthenticationListener() {
			@Override
			public void authenticate(AuthenticationEvent event) {
				IProxyService proxyService = ProxyManager.getProxyManager();
				if (proxyService.isProxiesEnabled() || proxyService.isSystemProxiesEnabled()) {
					IProxyData proxyData = proxyService.getProxyData(IProxyData.HTTP_PROXY_TYPE);
					event.user = proxyData.getUserId();
					event.password = proxyData.getPassword();
				}
			}
		});

		scrolledComposite.setContent(mainComposite);
		scrolledComposite.setExpandVertical(true);
		scrolledComposite.setExpandHorizontal(true);
		scrolledComposite.setAlwaysShowScrollBars(false);
		scrolledComposite.getVerticalBar().setIncrement(20);

		scrolledComposite.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				scrolledComposite.setMinSize(mainComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			}
		});
	}

	/**
	 * Adjust the media player size, according to the view's size
	 */
	public void adjustMediaPlayerSize(SashForm sash) {

		Point browserSize = new Point(0, 0);

		Point viewSize = sash.getSize();
		int sashWidth = viewSize.x - 40;

		((GridData) globalVideoTitle.getLayoutData()).widthHint = sashWidth;

		globalVideoTitle.getParent().layout();

		int width = sashWidth > PLAYER_MAXIMUM_WIDTH ? PLAYER_MAXIMUM_WIDTH : sashWidth;
		if (width < BROWSER_MIN_WIDTH) {
			width = BROWSER_MIN_WIDTH;
		}
		int height = (int) (width * PLAYER_HEIGHT_RATE);

		browserSize.x = width;
		browserSize.y = height;

		int titleLineSize = globalVideoTitle.getSize().y;

		if (sashResized) {
			// enter here due to a vertical resize (from top to bottom or
			// vice-versa)
			Point browserOriginalSize = new Point(0, 0);
			browserOriginalSize.y = browserSize.y;
			browserOriginalSize.x = browserSize.x;

			// calculate necessary height to get the width
			height = sash.getChildren()[0].getSize().y - titleLineSize - 20 < (int) (browserSize.x * PLAYER_HEIGHT_RATE) ? sash
					.getChildren()[0].getSize().y - titleLineSize - 20
					: (int) (browserSize.x * PLAYER_HEIGHT_RATE);
			width = (int) (browserSize.y / PLAYER_HEIGHT_RATE);

			// check if the width is smaller than the minimum, if so, set it to
			// the minimum
			if (width < BROWSER_MIN_WIDTH) {
				width = BROWSER_MIN_WIDTH;
			}

			// adjust height according to width (it may have changed)
			height = (int) (width * PLAYER_HEIGHT_RATE);

			browserSize.x = width;
			browserSize.y = height;

			// sashResized indicates that the sash division was moved
			if (browserOriginalSize.x != browserSize.x && browserOriginalSize.y != browserSize.y) {
				sashResized = true;
			}
			if (browserOriginalSize.x != browserSize.x && browserOriginalSize.y == browserSize.y) {
				sashResized = false;
			}

		}

		((GridData) browser.getLayoutData()).widthHint = browserSize.x;
		((GridData) browser.getLayoutData()).heightHint = browserSize.y;
		browser.setSize(browserSize);
		browser.execute("adjustSize(" + browserSize.x + ", " + browserSize.y + ");");

		globalVideoTitle.getParent().layout();

		sash.setWeights(sash.getWeights());
	}

	public void adjustVideoPlayerInitialSize() {
		scrolledComposite.setMinSize(mainComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
	}

	public void openVideo(Video video) {
		// adjust parent composites to visible
		this.setVisible(true);
		this.getParent().setVisible(true);
		mainComposite.setVisible(true);

		/*
		 * Set video title in the player area
		 */
		globalVideoTitle.getParent().setVisible(true);
		globalVideoTitle.setText(video.getTitle());
		((GridData) globalVideoTitle.getLayoutData()).exclude = false;

		/*
		 * Adjust and update the browser to play the video
		 */
		browser.setVisible(true);
		((GridData) browser.getLayoutData()).exclude = false;
		browser.execute("setVideo('" + video.getEmbeddedLink() + "');");
	}
}

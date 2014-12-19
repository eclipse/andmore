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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.common.log.UsageDataConstants;
import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.andmore.android.videos.Activator;
import org.eclipse.andmore.android.videos.i18n.VideosNLS;
import org.eclipse.andmore.android.videos.model.Video;
import org.eclipse.andmore.android.videos.model.VideoManager;
import org.eclipse.andmore.android.videos.ui.utils.UiUtilities;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.browser.ProgressAdapter;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.part.ViewPart;
import org.osgi.framework.Bundle;

/**
 * The view that contains all MOTODEV Video Tutorials
 */
public class MOTODEVVideosView extends ViewPart {

	/*
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "org.eclipse.andmore.android.videos.views.MOTODEVVideosView";

	/*
	 * View state
	 */
	private final String OPEN_EXTERNAL_BROWSER_PREFERENCE = "open.external.browser";

	/*
	 * Video Manager
	 */
	private VideoManager videoManager;

	/*
	 * Resource constants
	 */
	private final String VIDEO_WATCH_EMBED_HTML = "resources/watch_video.html";

	private final String VIDEO_WATCH_EMBED_HTML_JS_LIBRARY = "resources/swfobject.js";

	private final String VIEW_LOADING_ICON = "icons/loading_icon.gif";

	private final String VIEW_ERROR_ICON = "icons/error_icon.png";

	/*
	 * Containers
	 */
	private Composite parentComposite = null;

	private Composite container = null;

	private VideosListComposite videosListComposite;

	private SashForm sash;

	private VideoPlayerComposite videoPlayerArea;

	private String videoWatchURL;

	private String videoWatchJSLibraryURL;

	private Boolean flashPlayerSupport = null;

	private Boolean isBrowserLoaded = null;

	private static Boolean openExternalBrowser = null;

	public static int PLAY_LISTENER = 43214321;

	private Color backgroundColor = null;

	private Image viewErrorImg = null;

	private String loadingImgFullPath = null;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.ViewPart#init(org.eclipse.ui.IViewSite,
	 * org.eclipse.ui.IMemento)
	 */
	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {

		super.init(site, memento);

		try {

			Device device = getDefaultImage().getDevice();
			Bundle bundle = Activator.getDefault().getBundle();
			Display display = getSite().getShell().getDisplay();

			/*
			 * Retrieve view state
			 */
			if (openExternalBrowser == null && memento != null) {
				openExternalBrowser = memento.getBoolean(OPEN_EXTERNAL_BROWSER_PREFERENCE);
			}

			/*
			 * Colors
			 */
			backgroundColor = display.getSystemColor(SWT.COLOR_LIST_BACKGROUND);

			/*
			 * HTML to be embedded in the browser
			 */
			videoWatchURL = FileLocator.toFileURL(bundle.getEntry(VIDEO_WATCH_EMBED_HTML)).getFile();

			// using the method getAbsolutePath is necessary for this to work on
			// all systems
			videoWatchJSLibraryURL = new Path(FileLocator.toFileURL(bundle.getEntry(VIDEO_WATCH_EMBED_HTML_JS_LIBRARY))
					.getFile()).toPortableString();

			/*
			 * Images to be used
			 */
			viewErrorImg = new Image(device, FileLocator.toFileURL(bundle.getEntry(VIEW_ERROR_ICON)).getPath());

			loadingImgFullPath = FileLocator.toFileURL(Activator.getDefault().getBundle().getEntry(VIEW_LOADING_ICON))
					.getPath();

		} catch (IOException e) {
			StudioLogger.error(this.getClass(), "Error while initializing Videos View", e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets
	 * .Composite)
	 */
	@Override
	public void createPartControl(Composite parent) {

		// just point to the parent so that it can be accessed by the Job that
		// will be created
		parentComposite = parent;

		clearContent();

		/*
		 * Display a loading screen while the content is not loaded
		 */
		displayLoadingScreen(parent);

		/*
		 * Create a Job to load the view content
		 */
		Job refreshViews = new Job(VideosNLS.UI_Job_Refresh_View) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {

				final Boolean[] showErrorComposite = new Boolean[1];
				showErrorComposite[0] = false;

				final String[] errorMessage = new String[1];
				errorMessage[0] = null;

				/*
				 * Try to load the videos information. If an exception is
				 * thrown, log the error and prepare to show the error screen
				 */
				try {
					videoManager = VideoManager.getInstance();
					videoManager.load();
					videoManager.sort(VideoManager.SORT_MOST_RECENT);
				} catch (Exception e) {
					showErrorComposite[0] = true;
					errorMessage[0] = VideosNLS.UI_ErrorMsg + " " + e.getLocalizedMessage();
					StudioLogger.error(this.getClass(), "Error while initializing Videos View", e);
				}

				if (!monitor.isCanceled()) {

					/*
					 * Run the following code in the UI thread, whether it's the
					 * error screen or the videos screen itself. Also, disable
					 * the loading composite that is being displayed
					 */
					Display.getDefault().asyncExec(new Runnable() {
						@Override
						public void run() {

							// dispose the loading composite
							clearContent();

							if (showErrorComposite[0]) {
								// if an error occurred, display the error
								// screen
								displayErrorScreen(parentComposite, errorMessage[0]);
							} else {
								// ... otherwise, continue with the normal
								// workflow
								displayVideosScreen(parentComposite);
							}

						}
					});

				}

				return Status.OK_STATUS;
			}

			@Override
			public boolean belongsTo(Object family) {
				// relate the Job to this view
				return getTitle().equals(family);
			}

		};

		// Schedule the job to run
		refreshViews.schedule();

	}

	/**
	 * Display the videos screen
	 * 
	 * @param parent
	 *            the parent composite
	 */
	private void displayVideosScreen(Composite parent) {

		// Create the main composite container
		container = new Composite(parent, SWT.NONE);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		container.setLayout(new GridLayout(1, false));

		sash = new SashForm(container, SWT.VERTICAL | SWT.SMOOTH | SWT.BORDER);

		sash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// Create a global media player area, with the media player and info
		// related to the video
		createMediaPlayerAndToolbarArea(sash);

		// Create videos list area
		createVideosListArea(sash);

		// Adjust the media player size then the view changes its size
		sash.getChildren()[0].addControlListener(new ControlListener() {
			@Override
			public void controlResized(ControlEvent e) {
				videoPlayerArea.adjustMediaPlayerSize(sash);
			}

			@Override
			public void controlMoved(ControlEvent e) {
				// do nothing
			}
		});

		sash.setWeights(new int[] { 1, 2 });
		parentComposite.layout();

	}

	/**
	 * Display the loading screen
	 * 
	 * @param parent
	 *            the parent composite
	 * @return the loading composite
	 */
	private void displayLoadingScreen(Composite parent) {
		container = displayInfoScreen(parent, loadingImgFullPath, null, VideosNLS.UI_Loading, null, false);

		parentComposite.layout();
	}

	/**
	 * Display the error screen
	 * 
	 * @param parent
	 *            the parent composite
	 */
	private void displayErrorScreen(Composite parent, String error) {

		container = displayInfoScreen(parent, null, viewErrorImg, error, new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				EclipseUtils.openNetworkPreferences(getSite().getShell());
			}
		}, true);

		parentComposite.layout();

	}

	/**
	 * Generic method that display a information message (loading, error,
	 * warning, etc) in the screen, being composed of an image, a text and
	 * possibly a reload button
	 * 
	 * @param parent
	 *            the parent composite
	 * @param imagePath
	 *            path to the image that must be displayed
	 * @param image
	 *            alternatively, if no path is provided (null), an Image object
	 *            can be used
	 * @param message
	 *            the message to be displayed (a Link will be used to display
	 *            the message)
	 * @param selectionListener
	 *            handler to be called when the user clicks on the message
	 * @param reloadButton
	 *            whether a "reload" button should be displayed or not
	 * @return the composite created to construct the view
	 */
	private Composite displayInfoScreen(Composite parent, String imagePath, Image image, String message,
			SelectionListener selectionListener, boolean reloadButton) {

		Composite mainComposite = new Composite(parent, SWT.NONE);
		mainComposite.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
		mainComposite.setLayout(new GridLayout(1, false));
		mainComposite.setBackground(backgroundColor);

		Composite centeredComposite = new Composite(mainComposite, SWT.NONE);
		centeredComposite.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
		centeredComposite.setLayout(new GridLayout(1, false));
		centeredComposite.setBackground(backgroundColor);

		/*
		 * Status image
		 */
		final Label infoImg = new Label(centeredComposite, SWT.NONE);
		infoImg.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));
		infoImg.setBackground(backgroundColor);

		// Checking the mime type doesn't work on Linux
		// MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(new
		// File(imagePath)).equals("image/gif"))
		// Instead of "image/gif", it returns "application/octet-stream"
		if (imagePath != null && imagePath.endsWith(".gif")) {
			UiUtilities.displayAnimatedGIF(getDefaultImage().getDevice(), imagePath, infoImg);
		} else if (image != null) {
			infoImg.setImage(image);
		}

		/*
		 * Status text
		 */
		Link infoText = new Link(centeredComposite, SWT.NONE);
		GridData loadingTextGridData = new GridData(SWT.CENTER, SWT.NONE, true, false);
		loadingTextGridData.verticalIndent = 10;
		infoText.setLayoutData(loadingTextGridData);
		infoText.setBackground(backgroundColor);
		infoText.setText(message);
		if (selectionListener != null) {
			infoText.addSelectionListener(selectionListener);
		}

		/*
		 * Reload button, if requested
		 */
		if (reloadButton) {
			Button button = new Button(centeredComposite, SWT.NONE);
			GridData buttonGridData = new GridData(SWT.CENTER, SWT.NONE, false, false);
			buttonGridData.verticalIndent = 10;
			button.setLayoutData(buttonGridData);
			button.setText(VideosNLS.UI_Reload);
			button.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					reloadView();
				}
			});
		}

		return mainComposite;
	}

	/**
	 * Select the initial video to be displayed It's the first video of the
	 * first channel
	 */
	private void selectInitialVideo() {

		if (openExternalBrowser != null && !openExternalBrowser) {
			Video firstVideo = null;
			try {
				firstVideo = videoManager.getActiveChannels().get(0).getVideos().get(0);
			} catch (Exception e) {
				StudioLogger.error(this.getClass(), "No initial video to set", e);
			}

			if (firstVideo != null) {
				playVideo(firstVideo);
			}

			videoPlayerArea.adjustVideoPlayerInitialSize();
		}
	}

	/**
	 * Clear view content, i.e. remove everything being displayed
	 */
	private void clearContent() {
		if (container != null && !container.isDisposed()) {
			container.dispose();
		}

	}

	/**
	 * Try to reload the view
	 */
	private void reloadView() {
		createPartControl(parentComposite);
	}

	/**
	 * Create the view toolbar, with search field and a checkbox to define if
	 * the video must be displayed inside the view or in the external browser
	 */
	private void createToolBar() {
		ToolBarManager tbManager = (ToolBarManager) getViewSite().getActionBars().getToolBarManager();
		tbManager.add(new ToolbarContribution("MOTODEV Video Tutorials Toolbar"));
		tbManager.update(true);
		getViewSite().getActionBars().updateActionBars();
	}

	/**
	 * Create the area that contains the media player (browser embedded) as well
	 * as the information about the video being watched.
	 * 
	 * Additionally, the toolbar are is also created after the browser is
	 * loaded, since it depends on information acquired during browser loading
	 * (such as browser compatibility and Flash Player availability)
	 * 
	 * @param parent
	 *            the parent SashForm composite
	 */
	private void createMediaPlayerAndToolbarArea(SashForm parent) {
		Composite newPage = new Composite(parent, SWT.BORDER);
		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 1;
		layout.marginHeight = 1;
		newPage.setLayout(layout);
		newPage.setVisible(false);

		videoPlayerArea = new VideoPlayerComposite(newPage);
		videoPlayerArea.createVideoPlayerArea(new Font(getDefaultImage().getDevice(), getSite().getShell().getDisplay()
				.getSystemFont().getFontData()[0].getName(), 12, SWT.BOLD));
		Browser browser = videoPlayerArea.getBrowser();
		new FlashPlayerDetection(browser, "hasFlashPlayer");
		// identify when the page is loaded
		browser.addProgressListener(new ProgressAdapter() {

			@Override
			public void completed(ProgressEvent event) {
				// only the first loading matters, so that we remove the
				// listener,
				// otherwise it would be called each time a new video is
				// selected to
				// be played
				isBrowserLoaded = true;
				videoPlayerArea.getBrowser().removeProgressListener(this);

				/*
				 * The toolbar is only created when the embedded page is loaded
				 */
				createToolBar();
				selectInitialVideo();
			}
		});

		// Set the JavaScript library source (with the real path) and open the
		// URL
		File file = getVideoWatchHTML(videoWatchURL, videoWatchJSLibraryURL);
		browser.setUrl(file.toString());
	}

	/**
	 * Get the HTML to be used in runtime, with the JavasScript library real
	 * path set. This is necessary because relative paths do not work, and
	 * loading the lib programmatically using JavaScript doesn't work on Linux
	 * and MacOS since this is a async process and the objects defined in the
	 * lib might not be loaded when you try to use them.
	 * 
	 * @param fileURL
	 *            the original HTML file
	 * @param javascriptSrc
	 *            the JavaScript full path
	 * @return a file with the original HTML file, but with the JavasScript full
	 *         path set
	 */
	private File getVideoWatchHTML(String fileURL, String javascriptSrc) {

		File tempFile = null;

		BufferedReader bReader = null;
		BufferedWriter bWriter = null;
		FileReader fReader = null;
		FileWriter fWriter = null;
		try {
			// the original file
			File originalFile = new File(fileURL);

			// the temp file, with the same name as the original
			String[] tempFileName = originalFile.getName().split("\\.");
			tempFile = File.createTempFile(tempFileName[0], "." + tempFileName[1]);
			tempFile.deleteOnExit();

			/*
			 * Read the original file and copy it's content to the temp file, at
			 * the same time that the placeholders for the javascript library
			 * path is replaced by the real path(s)
			 */
			fReader = new FileReader(originalFile);
			fWriter = new FileWriter(tempFile);
			bReader = new BufferedReader(fReader);
			bWriter = new BufferedWriter(fWriter);
			String line;
			StringBuffer stringBuffer = new StringBuffer();
			while ((line = bReader.readLine()) != null) {
				stringBuffer.append(line);
			}
			bWriter.write(stringBuffer.toString().replace("@@JAVASCRIPT_SRC@@", javascriptSrc));

		} catch (IOException e) {
			StudioLogger.error(this.getClass(), "Could not create the HTML to be diplayed in the Browser widget", e);
		} finally {
			// close both reader/writer
			try {
				bWriter.close();
				bReader.close();
				fWriter.close();
				fReader.close();
			} catch (IOException e) {
				StudioLogger.error(e.getMessage());
			}

		}

		return tempFile;
	}

	/**
	 * Crate the tabs, which represent the playlists
	 * 
	 * @param parent
	 *            the parent composite
	 */
	private void createVideosListArea(Composite parent) {
		Composite newPage = new Composite(parent, SWT.NONE);
		newPage.setLayout(new GridLayout(1, false));

		videosListComposite = new VideosListComposite(newPage, SWT.NONE, videoManager.getActiveChannels(), videoManager
				.getDefaultChannel().getName());
		videosListComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		videosListComposite.addListener(MOTODEVVideosView.PLAY_LISTENER, new Listener() {
			@Override
			public void handleEvent(Event event) {
				playVideo((Video) event.data);
			}
		});
	}

	/**
	 * Play the video, whether using the internal or external browser
	 * 
	 * @param video
	 *            the video to be played
	 */
	private void playVideo(Video video) {

		if (openExternalBrowser == null || openExternalBrowser) {
			openVideoInExternalBrowser(video);
		} else {
			openVideoInEmbeddedBrowser(video);
		}

		// Log video playback
		try {

			StudioLogger.collectUsageData(UsageDataConstants.WHAT_VIDEOS_PLAY, UsageDataConstants.KIND_VIDEOS,
					"Video Played: " + video.getId() + "|" + video.getTitle() + "|"
							+ (openExternalBrowser != null ? openExternalBrowser.toString() : "null"),
					Activator.PLUGIN_ID, Activator.getDefault().getBundle().getVersion().toString());
		} catch (Exception e) {
			// do nothing, just do that to be safe and to not break the
			// functionality
			// due to errors when logging usage information
		}

	}

	/**
	 * Play the video using the embedded browser
	 * 
	 * @param video
	 *            the video to be played
	 */
	private void openVideoInEmbeddedBrowser(Video video) {
		videoPlayerArea.openVideo(video);
		/*
		 * Select video in the list
		 */
		videosListComposite.setSelectedVideo(video);

		parentComposite.layout();

		/*
		 * Adjust size
		 */
		videoPlayerArea.adjustMediaPlayerSize(sash);
	}

	/**
	 * Open the external browser
	 * 
	 * @param video
	 *            the video to be opened
	 */
	private void openVideoInExternalBrowser(Video video) {

		IWorkbenchBrowserSupport browserSupport = PlatformUI.getWorkbench().getBrowserSupport();
		IWebBrowser browser;
		try {

			browser = browserSupport.createBrowser(IWorkbenchBrowserSupport.LOCATION_BAR
					| IWorkbenchBrowserSupport.NAVIGATION_BAR | IWorkbenchBrowserSupport.AS_EXTERNAL,
					VideosNLS.UI_MOTODEV_Video + video.getTitle(), null, null);
			browser.openURL(new URL(video.getExternalLink()));

		} catch (Exception e) {
			StudioLogger.error(this.getClass(), "Error opening video on external browser", e);
		}
	}

	/**
	 * Filter videos in the UI based on the search results
	 * 
	 * @param keyword
	 *            search keyword
	 */
	private void searchAndFilter(String keyword) {
		// execute trim and remove duplicated blank spaces
		keyword = removeDuplicatedBlankSpaces(keyword.trim());

		// search and filter
		videosListComposite.filter(videoManager.search(keyword), keyword.equals(""));
		// highlight keywords
		videosListComposite.highlightKeywords(keyword);

	}

	/**
	 * Remove all duplicated spaces in a given string
	 * 
	 * @param text
	 *            the string to have duplicated spaces removed
	 * @return the string passed as parameter with no duplicated spaces
	 */
	private String removeDuplicatedBlankSpaces(String text) {
		Pattern pattern = Pattern.compile("\\s+");
		Matcher matcher = pattern.matcher(text);
		matcher.find();
		return matcher.replaceAll(" ");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.WorkbenchPart#dispose()
	 */
	@Override
	public void dispose() {
		super.dispose();
		if (container != null && !container.isDisposed()) {
			container.dispose();
		}
		// cancel all jobs related to this view
		Job.getJobManager().cancel(getTitle());
	}

	/*
	 * Save the view state when the workbench is closed
	 * 
	 * @see org.eclipse.ui.part.ViewPart#saveState(org.eclipse.ui.IMemento)
	 */
	@Override
	public void saveState(IMemento memento) {
		if (canPlayEmbeddedVideo() && openExternalBrowser != null) {
			memento.putBoolean(OPEN_EXTERNAL_BROWSER_PREFERENCE, openExternalBrowser);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
	 */
	@Override
	public void setFocus() {
		// nothing
	}

	/**
	 * Check if the browser can load web pages. Due to several problems of the
	 * Browser widget on Linux, we are disabling this feature on this OS for
	 * now.
	 * 
	 * Some of the problems on Linux are: - Browser doesn't work with some
	 * proxies - Intermitent crashes of SWT - The Browser widget blinks on the
	 * screen
	 * 
	 * For MacOS X, it's enabled only from Snow Leopard on (> 10.6), since the
	 * Browser widget might not work correctly in Leopard with Flash Player 64
	 * bits, since the kernel modules themselves run in 32 bits, what causes a
	 * crash (Studio closes).
	 * 
	 * @return "true" if the browser can load pages, "false" otherwise
	 */
	private boolean canPlayEmbeddedVideo() {

		Boolean result = false;

		// for now, the correct version will only be analyzed on MacOSX
		boolean correctVersion = true;

		if (Platform.getOS().equals(Platform.OS_MACOSX)) {
			String versionText = System.getProperty("os.version");

			// Get the version as a number
			float versionNumber = 0;
			try {

				if (!versionText.contains(".")) {
					versionNumber = Integer.parseInt(versionText);
				} else {
					String[] versionPieces = versionText.split("\\.");
					versionNumber = Float.parseFloat(versionPieces[0] + "."
							+ (!versionPieces[1].equals("") ? versionPieces[1] : "0"));

				}
			} catch (NumberFormatException e) {
				StudioLogger.error(this.getClass(), "Cannot get the OS version", e);
			}

			// Leopard = 10.5 = not supported
			if (versionNumber <= 10.5) {
				correctVersion = false;
			}

		}

		// If we are not on Linux and the MacOS X has the correct version,
		// check if the browser could load the web page and, consequently,
		// if the flash player is supported or not
		if (!Platform.getOS().equals(Platform.OS_LINUX) && correctVersion) {
			if (isBrowserLoaded != null && isBrowserLoaded && flashPlayerSupport != null) {
				result = true;
			}
		}

		// Log player support
		try {

			StudioLogger.collectUsageData(UsageDataConstants.WHAT_VIDEOS_PLAYER_SUPPORT,
					UsageDataConstants.KIND_VIDEOS, result.toString() + "|" + Platform.getOS() + "|" + correctVersion,
					Activator.PLUGIN_ID, Activator.getDefault().getBundle().getVersion().toString());
		} catch (Exception e) {
			// do nothing, just do that to be safe and to not break the
			// functionality
			// due to errors when logging usage information
		}

		return result;
	}

	/**
	 * Check if an appropriate version of Flash Player was found, which means
	 * that the videos can be displayed in the embedded browser
	 * 
	 * @return "true" if an appropriate version of Flash Player was found,
	 *         "false" otherwise
	 */
	private boolean hasFlashPlayer() {
		Boolean result = false;

		if (isBrowserLoaded != null && isBrowserLoaded && flashPlayerSupport != null) {
			result = flashPlayerSupport;
		}

		// Log flash player support
		try {

			StudioLogger.collectUsageData(UsageDataConstants.WHAT_VIDEOS_PLAYER_SUPPORT,
					UsageDataConstants.KIND_VIDEOS, result.toString(), Activator.PLUGIN_ID, Activator.getDefault()
							.getBundle().getVersion().toString());
		} catch (Exception e) {
			// do nothing, just do that to be safe and to not break the
			// functionality
			// due to errors when logging usage information
		}

		return result;
	}

	/**
	 * Get the message that will be displayed to the user to inform that the
	 * appropriate version of Flash Player was not found
	 * 
	 * @return the "no Flash Player" message
	 */
	private String getNoFlashMessage() {
		String message = VideosNLS.UI_No_Flash_Player + " ";

		if (Platform.getOSArch().equals(Platform.ARCH_X86)) {
			// 32 bits
			message += NLS.bind(VideosNLS.UI_No_Flash_Player_32bits_Extension, VideosNLS.UI_Flash_Player_Link_32bits);
		} else {
			// 64 bits
			message += NLS.bind(VideosNLS.UI_No_Flash_Player_64bits_Extension, VideosNLS.UI_Flash_Player_Link_64bits);
		}

		return message;
	}

	/**
	 * Create the toolbar, with a search field and a checkbox to define if the
	 * video must be displayed inside the view or in the external browser
	 */
	private class ToolbarContribution extends ControlContribution {

		protected ToolbarContribution(String id) {
			super(id);
		}

		@Override
		protected Control createControl(Composite parent) {

			Composite toolbarComposite = new Composite(parent, SWT.NONE);
			toolbarComposite.setLayout(new RowLayout(SWT.HORIZONTAL));

			final Text searchText = new Text(toolbarComposite, SWT.SEARCH);
			searchText.setLayoutData(new RowData(150, SWT.DEFAULT));
			searchText.setMessage(VideosNLS.UI_Search);

			searchText.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					searchAndFilter(searchText.getText());
				}
			});

			if (canPlayEmbeddedVideo()) {

				// recover the state, if there is flash player
				// if there is NO flash player, the checkbox is always selected
				boolean enablement = hasFlashPlayer();
				boolean selection = !enablement ? true : openExternalBrowser != null ? openExternalBrowser : false;
				final Button openExternal = new Button(toolbarComposite, SWT.CHECK);
				openExternal.setText(VideosNLS.UI_Open_External_Browser);
				openExternal.setToolTipText(VideosNLS.UI_Open_External_Browser);
				openExternal.setEnabled(enablement);
				openExternal.setSelection(selection);
				openExternalBrowser = selection;

				if (!enablement) {
					Label warningImg = new Label(toolbarComposite, SWT.NONE);
					warningImg.setImage(PlatformUI.getWorkbench().getSharedImages()
							.getImage(ISharedImages.IMG_OBJS_WARN_TSK));
					warningImg.setToolTipText(getNoFlashMessage());
				}

				openExternal.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						openExternalBrowser = openExternal.getSelection();
					}
				});

			}

			return toolbarComposite;

		}
	}

	/**
	 * Function called from JavaScript to inform if the browser could find Adobe
	 * Flash Player or not
	 */
	private class FlashPlayerDetection extends BrowserFunction {

		@Override
		public Object function(Object[] arguments) {
			flashPlayerSupport = new Boolean(arguments[0].toString());
			return null;
		}

		public FlashPlayerDetection(Browser browser, String name) {
			super(browser, name);
		}

	}

}
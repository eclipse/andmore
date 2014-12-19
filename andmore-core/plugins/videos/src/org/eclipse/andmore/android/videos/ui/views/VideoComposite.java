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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.videos.Activator;
import org.eclipse.andmore.android.videos.i18n.VideosNLS;
import org.eclipse.andmore.android.videos.model.Video;
import org.eclipse.andmore.android.videos.model.VideoManager;
import org.eclipse.andmore.android.videos.ui.utils.UiUtilities;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * A specialized Composite to be used to represent a single Video
 * 
 */
public class VideoComposite extends Composite {

	/*
	 * Video Manager
	 */
	private VideoManager videoManager;

	/*
	 * The video that is being represented
	 */
	private Video video = null;

	/*
	 * Resource constants
	 */
	private final String PLAY_ICON = "icons/play_icon.png";

	private final String THUMBNAIL_LOADING_ICON = "icons/thumbnail_loading.png";

	private final String THUMBNAIL_NOT_AVAILABLE_ICON = "icons/preview_not_available.png";

	/*
	 * Other global elements and resources
	 */
	private Image playImg = null;

	private Image thumbLoadingImg = null;

	private Font videoTitleFont = null;

	private Font videoTitleSelectedFont = null;

	private Color highlightTextColor = null;

	private Color backgroundColor = null;

	private boolean showAllDescription = false;

	/*
	 * Some widgets in the composite
	 */
	private StyledText videoTitle = null;

	private final List<StyledText> styledTexts = new ArrayList<StyledText>();

	/*
	 * Parent class
	 */
	private VideosListComposite parentClass = null;

	private final ScrolledComposite scrollParent;

	/*
	 * Layout details
	 */
	private final int MINIMUM_DESCRIPTION_COLUMN_WIDTH = 200;

	/**
	 * Constructor responsible for creating the entire component
	 * 
	 * @param parent
	 *            the parent composite
	 * @param style
	 *            SWT style
	 * @param video
	 *            the video to be represented
	 */
	public VideoComposite(VideosListComposite parentClass, Composite parent, ScrolledComposite scrollParent, int style,
			Video video) {
		super(parent, style);
		this.video = video;
		this.parentClass = parentClass;
		this.scrollParent = scrollParent;
		init();
		createControls();
	}

	/**
	 * Initialize variables and resources
	 */
	private void init() {

		try {

			// Video Manager instance
			videoManager = VideoManager.getInstance();

			/*
			 * Images to be used
			 */
			playImg = new Image(getShell().getDisplay(), FileLocator.toFileURL(
					Activator.getDefault().getBundle().getEntry(PLAY_ICON)).getPath());

			thumbLoadingImg = new Image(getShell().getDisplay(), FileLocator.toFileURL(
					Activator.getDefault().getBundle().getEntry(THUMBNAIL_LOADING_ICON)).getPath());

			/*
			 * Fonts
			 */
			videoTitleFont = new Font(getShell().getDisplay(),
					getShell().getDisplay().getSystemFont().getFontData()[0].getName(), 10, SWT.BOLD);

			videoTitleSelectedFont = new Font(getShell().getDisplay(), getShell().getDisplay().getSystemFont()
					.getFontData()[0].getName(), 10, SWT.BOLD | SWT.ITALIC);

			/*
			 * Colors
			 */
			backgroundColor = getShell().getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND);

			highlightTextColor = getShell().getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION);

		} catch (Exception e) {
			StudioLogger.error(this.getClass(), "Error while initializing Video Composite", e);
		}

	}

	/**
	 * Create all widgets of this composite
	 */
	private void createControls() {

		// set the composite layout
		setLayout(new GridLayout(3, false));
		setBackground(backgroundColor);

		// Set loading thumbnail image
		final Label videoImage = new Label(this, SWT.NONE);
		videoImage.setImage(thumbLoadingImg);

		// Get the real thumbnail
		Thread updateVideoImage = new Thread(new Runnable() {

			@Override
			public void run() {
				// this operation is time consuming (require internet access)
				// that's why we are using a new thread and just after we have
				// the
				// file download we use the UI thread
				final File thumbnail = videoManager.getThumbnail(video.getSnapshot());

				Display.getDefault().syncExec(new Runnable() {
					@Override
					public void run() {
						Image videoImg = thumbnail != null ? new Image(getShell().getDisplay(), thumbnail.toString())
								: null;

						// make sure the element still exists, the view can be
						// closed
						if (!videoImage.isDisposed()) {
							if (videoImg != null) {
								videoImage.setImage(videoImg);
							} else {
								ImageData notAvailable = AbstractUIPlugin.imageDescriptorFromPlugin(
										Activator.PLUGIN_ID, THUMBNAIL_NOT_AVAILABLE_ICON).getImageData();
								Point size = videoImage.getSize();
								if (videoImage.getImage() != null && !videoImage.getImage().isDisposed()) {
									size = new Point(videoImage.getImage().getBounds().width, videoImage.getImage()
											.getBounds().height);
									videoImage.getImage().dispose();
								}
								videoImage.setImage(new Image(getShell().getDisplay(), notAvailable.scaledTo(size.x,
										size.y)));
							}
						}

					}
				});

			}
		});
		updateVideoImage.start();

		videoImage.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
		videoImage.setBackground(backgroundColor);

		final Composite titleAndDescriptionComposite = new Composite(this, SWT.WRAP | SWT.BACKGROUND);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		data.widthHint = MINIMUM_DESCRIPTION_COLUMN_WIDTH;
		data.minimumWidth = MINIMUM_DESCRIPTION_COLUMN_WIDTH;
		data.heightHint = videoImage.getSize().y;
		titleAndDescriptionComposite.setLayoutData(data);
		titleAndDescriptionComposite.setLayout(new GridLayout(1, true));
		titleAndDescriptionComposite.setBackground(backgroundColor);

		videoTitle = new StyledText(titleAndDescriptionComposite, SWT.WRAP | SWT.BACKGROUND);
		videoTitle.setEnabled(false);
		videoTitle.setBackground(backgroundColor);
		videoTitle.setFont(videoTitleFont);
		data = new GridData(SWT.FILL, SWT.FILL, true, false);

		videoTitle.setLayoutData(data);
		styledTexts.add(videoTitle);

		final StyledText videoDescription = new StyledText(titleAndDescriptionComposite, SWT.WRAP | SWT.BACKGROUND);
		videoDescription.setEnabled(false);
		videoDescription.setBackground(backgroundColor);
		data = new GridData(SWT.FILL, SWT.FILL, true, true);
		videoDescription.setLayoutData(data);
		styledTexts.add(videoDescription);

		Composite playButtonArea = new Composite(this, SWT.NONE);
		GridLayout layout = new GridLayout(1, true);
		playButtonArea.setLayoutData(new GridData(SWT.NONE, SWT.NONE, false, false));
		playButtonArea.setLayout(layout);
		playButtonArea.setBackground(backgroundColor);

		Label playImage = new Label(playButtonArea, SWT.NONE);
		playImage.setImage(playImg);
		playImage.setBackground(backgroundColor);
		playImage.setToolTipText(VideosNLS.UI_Play_Video);
		playImage.setCursor(new Cursor(getShell().getDisplay(), SWT.CURSOR_HAND));
		playImage.setLayoutData(new GridData(SWT.NONE, SWT.NONE, false, false));

		videoTitle.setText(video.getTitle());
		videoDescription.setText(video.getDescription());

		final Label moreText = new Label(playButtonArea, SWT.WRAP | SWT.BACKGROUND);
		moreText.addMouseListener(new MouseListener() {

			@Override
			public void mouseUp(MouseEvent e) {
				if (!showAllDescription) {
					showAllDescription = true;
					GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
					data.widthHint = MINIMUM_DESCRIPTION_COLUMN_WIDTH;
					data.minimumWidth = MINIMUM_DESCRIPTION_COLUMN_WIDTH;
					titleAndDescriptionComposite.setLayoutData(data);
				} else {
					showAllDescription = false;
					GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
					data.widthHint = MINIMUM_DESCRIPTION_COLUMN_WIDTH;
					data.minimumWidth = MINIMUM_DESCRIPTION_COLUMN_WIDTH;
					data.heightHint = videoImage.getSize().y;
					titleAndDescriptionComposite.setLayoutData(data);
				}

				// redraw everything
				parentClass.layout();
				((Composite) scrollParent.getContent()).layout(true);
				scrollParent.setMinSize(scrollParent.getContent().computeSize(SWT.DEFAULT, SWT.DEFAULT));
			}

			@Override
			public void mouseDown(MouseEvent e) {
				// do nothing
			}

			@Override
			public void mouseDoubleClick(MouseEvent e) {
				// do nothing
			}
		});

		titleAndDescriptionComposite.addControlListener(new ControlListener() {

			@Override
			public void controlResized(ControlEvent e) {
				Point descriptionPreferedSize = videoDescription.computeSize(titleAndDescriptionComposite.getSize().x,
						SWT.DEFAULT);
				Point titlePreferedSize = videoTitle.computeSize(titleAndDescriptionComposite.getSize().x, SWT.DEFAULT);

				int preferedHeight = descriptionPreferedSize.y + titlePreferedSize.y;

				if (preferedHeight + 20 > videoImage.getSize().y) {
					moreText.setCursor(new Cursor(getShell().getDisplay(), SWT.CURSOR_HAND));
					moreText.setBackground(backgroundColor);
					GridData data = new GridData(SWT.CENTER, SWT.TOP, false, false);
					moreText.setLayoutData(data);

					moreText.setVisible(true);
					if (!showAllDescription) {
						moreText.setText("(+)");
						moreText.setToolTipText(VideosNLS.UI_More);
					} else {
						moreText.setText("(-)");
						moreText.setToolTipText(VideosNLS.UI_Less);
					}
				} else {
					moreText.setVisible(false);
					moreText.setCursor(new Cursor(getShell().getDisplay(), SWT.CURSOR_ARROW));
				}
			}

			@Override
			public void controlMoved(ControlEvent e) {
				// do nothing
			}
		});

		playImage.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				Event event = new Event();
				event.data = video;
				parentClass.notifyListeners(MOTODEVVideosView.PLAY_LISTENER, event);
			}
		});

		// create and set the pop-up menu for the composite and all widgets in
		// the composite
		setMenuForAllWidgets(this, createVideoPopupMenu());
	}

	/**
	 * Add a menu item in the video composite right-click, with options to copy
	 * the video URL
	 * 
	 * @param videoComposite
	 *            the video composite
	 * @param video
	 *            the video being represented by the composite
	 * @return the menu
	 */
	private Menu createVideoPopupMenu() {

		Menu popupMenu = new Menu(getShell(), SWT.POP_UP);

		MenuItem videoUrl = new MenuItem(popupMenu, SWT.NONE);
		videoUrl.setText(VideosNLS.UI_Copy_URL);
		MenuItem videoEmbeddedUrl = new MenuItem(popupMenu, SWT.NONE);
		videoEmbeddedUrl.setText(VideosNLS.UI_Copy_Embedded_URL);

		videoUrl.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				copyTextToClipboard(video.getExternalLink());
			}
		});

		videoEmbeddedUrl.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				copyTextToClipboard(video.getEmbeddedLink());
			}
		});

		return popupMenu;
	}

	/**
	 * Recursively add the menu for all widgets
	 * 
	 * @param mainComposite
	 *            the main composite
	 * @param popupMenu
	 *            the menu to be added to the main composite all all children
	 *            widgets
	 */
	private void setMenuForAllWidgets(Composite mainComposite, Menu popupMenu) {

		mainComposite.setMenu(popupMenu);

		for (Control control : mainComposite.getChildren()) {
			control.setMenu(popupMenu);
			if (control instanceof Composite && ((Composite) control).getChildren().length > 0) {
				setMenuForAllWidgets((Composite) control, popupMenu);
			}
		}
	}

	/**
	 * Copy the selected text to clipboard
	 * 
	 * @param text
	 *            the text to be copied to the clipboard
	 */
	private void copyTextToClipboard(String text) {
		Clipboard clipboard = new Clipboard(getShell().getDisplay());
		clipboard.setContents(new Object[] { text }, new Transfer[] { TextTransfer.getInstance() });
	}

	/**
	 * Change the element appearance to mark it as selected
	 */
	public void select() {
		videoTitle.setFont(videoTitleSelectedFont);
	}

	/**
	 * Change the element appearance to the default one
	 */
	public void deselect() {
		videoTitle.setFont(videoTitleFont);
	}

	/**
	 * Highlight the given keyword in the composite
	 * 
	 * @param keyword
	 *            the keyword to be highlighted
	 */
	public void highlightKeyword(String keyword) {
		for (StyledText styledText : styledTexts) {
			UiUtilities.highlightKeywords(styledText, keyword, highlightTextColor);
		}
	}

	/**
	 * Retrieve the video being represented
	 * 
	 * @return the video that is being represented
	 */
	public Video getVideo() {
		return video;
	}

}

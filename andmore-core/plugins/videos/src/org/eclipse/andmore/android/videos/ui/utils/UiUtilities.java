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
package org.eclipse.andmore.android.videos.ui.utils;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

/**
 * General UI Utilities used by the MOTODEV Studio Videos View
 */
public class UiUtilities {

	/**
	 * Highlight the search keyword in the styled text provided
	 * 
	 * @param styledText
	 *            the styled text to be decorated
	 * @param keyword
	 *            the keyword to be highlighted in the styled text
	 * @param highlightTextColor
	 *            the background color to be used to highlight the keyword
	 */
	public static void highlightKeywords(StyledText styledText, String keyword, Color highlightTextColor) {

		// split into multiple keywords
		String[] keywordPieces = keyword.split(" ");

		/*
		 * Define which characters must be highlighted by creating a boolean
		 * array with the text size plus 1 (the last position will remain with
		 * the default value - false, and that's important to define the last
		 * segment to be highlighted)
		 */
		List<Integer> startOffsets = null;
		boolean[] highlightedChars = null;
		highlightedChars = new boolean[styledText.getText().length() + 1];

		// for each keyword, define which characters must be highlighted in the
		// string / StyledText
		for (String keywordPiece : keywordPieces) {
			// find all occurrences of that keyword
			startOffsets = findAllIndexOf(styledText.getText().toUpperCase(), keywordPiece.toUpperCase());

			if (startOffsets.size() > 0) {
				// for each occurrence, mark the characters that must be
				// highlighted
				for (int startOffset : startOffsets) {
					for (int i = startOffset; i < startOffset + keywordPiece.length(); i++) {
						highlightedChars[i] = true;
					}
				}

			}
		}

		// finally, create and set the style ranges based on the boolean array
		// with the
		// information about what characters that must be highlighted
		styledText.setStyleRanges(createStyleRanges(highlightedChars, highlightTextColor));
	}

	/**
	 * Create and return an array of StyleRanges based on a boolean array
	 * defining which characters must be highlighted in the StyledText
	 * 
	 * @param highlightedChars
	 *            a boolean array defining which characters must be highlighted
	 *            in the StyledText
	 * @param highlightTextColor
	 *            the background color to be used to highlight the keyword
	 * @return an array of the StyleRanges that must be applied to the
	 *         StyledText
	 */
	private static StyleRange[] createStyleRanges(boolean[] highlightedChars, Color highlightTextColor) {

		List<StyleRange> styleRanges = new ArrayList<StyleRange>();

		// the start variable marks the start of a new segment
		Integer start = null;
		for (int j = 0; j < highlightedChars.length; j++) {
			if (highlightedChars[j] == true) {
				// this is the beginning of a new segment. If start is not
				// null, then this is just an adjacent character in the current
				// segment
				if (start == null) {
					start = j;
				}
			} else {
				// end of the current segment, register it and continue
				// searching for the next segment
				if (start != null) {
					styleRanges.add(getHighlightStyle(start, j - start, highlightTextColor));
					start = null;
				}
			}
		}

		return styleRanges.toArray(new StyleRange[styleRanges.size()]);

	}

	/**
	 * Find all "indexOf" of a keyword in a string, and not only the first one,
	 * as the method available in the String type implementation
	 * 
	 * @param text
	 *            the base string
	 * @param keyword
	 *            the keyword to be find in the base string
	 * @return a list with all indexes
	 */
	private static List<Integer> findAllIndexOf(String text, String keyword) {
		List<Integer> allIndexes = new ArrayList<Integer>();

		if (!keyword.equals("")) {
			int index = text.indexOf(keyword);
			while (index >= 0) {
				allIndexes.add(index);
				index = text.indexOf(keyword, index + keyword.length());
			}
		}

		return allIndexes;
	}

	/**
	 * Create the StyleRange that will be used to highlight the keyword in a
	 * given location
	 * 
	 * @param startOffset
	 *            where the keyword starts in a string
	 * @param length
	 *            the size of the keyword
	 * @param highlightTextColor
	 *            the background color to be used to highlight the keyword
	 * @return the corresponding StyleRange
	 */
	private static StyleRange getHighlightStyle(int startOffset, int length, Color highlightTextColor) {
		StyleRange styleRange = new StyleRange();
		styleRange.start = startOffset;
		styleRange.length = length;
		styleRange.background = highlightTextColor;
		return styleRange;
	}

	/**
	 * Display an animated GIF in a Label
	 * 
	 * @param device
	 *            the device object to be used to create the UI components
	 * @param path
	 *            the GIF path
	 * @param container
	 *            the Label where the GIF will be displayed
	 */
	public static void displayAnimatedGIF(final Device device, final String path, final Label container) {

		// create the image loader
		final ImageLoader imageLoader = new ImageLoader();
		imageLoader.load(path);

		// create GC and set the first image
		final Image firstImage = new Image(device, imageLoader.data[0]);
		final GC gc = new GC(firstImage);
		container.setImage(firstImage);

		// the paint listener is important because just calling redraw on the
		// container doesn't update the content (actually, that works on
		// Windows,
		// but on Linux and MacOS it doesn't)
		container.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				e.gc.drawImage(firstImage, 0, 0);
			}
		});

		/*
		 * Create a thread that flip among the other images in the GIF
		 */
		final Thread thread = new Thread() {
			@Override
			public void run() {

				try {
					final Integer[] imageNumber = new Integer[1];
					imageNumber[0] = 0;

					/*
					 * Do it until the thread is interrupted
					 */
					while (true) {

						// wait the appropriate time
						int delayTime = imageLoader.data[imageNumber[0]].delayTime;
						Thread.sleep(delayTime * 10);

						// draw the next image
						// it's a sync call so that the executions are not
						// delayed and grouped in the
						// feature. Ex: after some time, 10 calls are executed
						// one right after another,
						// what will flip among 10 images in the GIF almost at
						// the same time, which
						// has no value to the user. Using syncExec we prevent
						// that.
						Display.getDefault().syncExec(new Runnable() {
							@Override
							public void run() {
								// cyclic counter
								imageNumber[0] = imageNumber[0].intValue() == imageLoader.data.length - 1 ? 0
										: imageNumber[0] + 1;

								ImageData nextImageData = imageLoader.data[imageNumber[0]];
								Image nextImage = new Image(device, nextImageData);

								gc.drawImage(nextImage, nextImageData.x, nextImageData.y);
								nextImage.dispose();

								// redraw
								if (!container.isDisposed()) {
									container.redraw();
								}
							}
						});

					}
				} catch (InterruptedException e) {
					StudioLogger.info(this.getClass(), "Thread displaying animated GIF was interrupted: " + path);
				}
			}
		};

		/*
		 * When the container is disposed, the thread that flips among the
		 * images in the GIF is also stopped
		 */
		container.addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(DisposeEvent e) {
				thread.interrupt();
			}
		});

		// start the thread
		thread.start();

	}

}

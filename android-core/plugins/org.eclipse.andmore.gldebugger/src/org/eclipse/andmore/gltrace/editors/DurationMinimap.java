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

package org.eclipse.andmore.gltrace.editors;

import org.eclipse.andmore.gltrace.GLProtoBuf.GLMessage.Function;
import org.eclipse.andmore.gltrace.model.GLCall;
import org.eclipse.andmore.gltrace.model.GLTrace;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import java.util.ArrayList;
import java.util.List;

public class DurationMinimap extends Canvas {
	/** Default alpha value. */
	private static final int DEFAULT_ALPHA = 255;

	/** Alpha value for highlighting visible calls. */
	private static final int VISIBLE_CALLS_HIGHLIGHT_ALPHA = 50;

	/** Clamp call durations at this value. */
	private static final long CALL_DURATION_CLAMP = 20000;

	private static final String FONT_KEY = "default.font"; //$NON-NLS-1$

	/**
	 * Scale font size by this amount to get the max display length of call
	 * duration.
	 */
	private static final int MAX_DURATION_LENGTH_SCALE = 6;

	/** List of GL Calls in the trace. */
	private List<GLCall> mCalls;

	/** Number of GL contexts in the trace. */
	private int mContextCount;

	/** Starting call index of currently displayed frame. */
	private int mStartCallIndex;

	/** Ending call index of currently displayed frame. */
	private int mEndCallIndex;

	/** The top index that is currently visible in the table. */
	private int mVisibleCallTopIndex;

	/** The bottom index that is currently visible in the table. */
	private int mVisibleCallBottomIndex;

	private Color mBackgroundColor;
	private Color mDurationLineColor;
	private Color mGlDrawColor;
	private Color mGlErrorColor;
	private Color mContextHeaderColor;
	private Color mVisibleCallsHighlightColor;
	private Color mMouseMarkerColor;

	private FontRegistry mFontRegistry;
	private int mFontWidth;
	private int mFontHeight;

	// back buffers used for double buffering
	private Image mBackBufferImage;
	private GC mBackBufferGC;

	// mouse state
	private boolean mMouseInSelf;
	private int mMouseY;

	// helper object used to position various items on screen
	private final PositionHelper mPositionHelper;

	public DurationMinimap(Composite parent, GLTrace trace) {
		super(parent, SWT.NO_BACKGROUND);

		setInput(trace);

		initializeColors();
		initializeFonts();

		mPositionHelper = new PositionHelper(mFontHeight, mContextCount, mFontWidth * MAX_DURATION_LENGTH_SCALE, /*
																												 * max
																												 * display
																												 * length
																												 * for
																												 * call
																												 * .
																												 */
		CALL_DURATION_CLAMP /* max duration */);

		addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				draw(e.display, e.gc);
			}
		});

		addListener(SWT.Resize, new Listener() {
			@Override
			public void handleEvent(Event event) {
				controlResized();
			}
		});

		addMouseMoveListener(new MouseMoveListener() {
			@Override
			public void mouseMove(MouseEvent e) {
				mouseMoved(e);
			}
		});

		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent e) {
				mouseClicked(e);
			}
		});

		addMouseTrackListener(new MouseTrackListener() {
			@Override
			public void mouseHover(MouseEvent e) {
			}

			@Override
			public void mouseExit(MouseEvent e) {
				mMouseInSelf = false;
				redraw();
			}

			@Override
			public void mouseEnter(MouseEvent e) {
				mMouseInSelf = true;
				redraw();
			}
		});
	}

	public void setInput(GLTrace trace) {
		if (trace != null) {
			mCalls = trace.getGLCalls();
			mContextCount = trace.getContexts().size();
		} else {
			mCalls = null;
			mContextCount = 1;
		}
	}

	@Override
	public void dispose() {
		disposeColors();
		disposeBackBuffer();
		super.dispose();
	}

	private void initializeColors() {
		mBackgroundColor = new Color(getDisplay(), 0x33, 0x33, 0x33);
		mDurationLineColor = new Color(getDisplay(), 0x08, 0x51, 0x9c);
		mGlDrawColor = new Color(getDisplay(), 0x6b, 0xae, 0xd6);
		mContextHeaderColor = new Color(getDisplay(), 0xd1, 0xe5, 0xf0);
		mVisibleCallsHighlightColor = new Color(getDisplay(), 0xcc, 0xcc, 0xcc);
		mMouseMarkerColor = new Color(getDisplay(), 0xaa, 0xaa, 0xaa);

		mGlErrorColor = getDisplay().getSystemColor(SWT.COLOR_RED);
	}

	private void disposeColors() {
		mBackgroundColor.dispose();
		mDurationLineColor.dispose();
		mGlDrawColor.dispose();
		mContextHeaderColor.dispose();
		mVisibleCallsHighlightColor.dispose();
		mMouseMarkerColor.dispose();
	}

	private void initializeFonts() {
		mFontRegistry = new FontRegistry(getDisplay());
		mFontRegistry.put(FONT_KEY, new FontData[] { new FontData("Arial", 8, SWT.NORMAL) }); //$NON-NLS-1$

		GC gc = new GC(getDisplay());
		gc.setFont(mFontRegistry.get(FONT_KEY));
		mFontWidth = gc.getFontMetrics().getAverageCharWidth();
		mFontHeight = gc.getFontMetrics().getHeight();
		gc.dispose();
	}

	private void initializeBackBuffer() {
		Rectangle clientArea = getClientArea();

		if (clientArea.width == 0 || clientArea.height == 0) {
			mBackBufferImage = null;
			mBackBufferGC = null;
			return;
		}

		mBackBufferImage = new Image(getDisplay(), clientArea.width, clientArea.height);
		mBackBufferGC = new GC(mBackBufferImage);
	}

	private void disposeBackBuffer() {
		if (mBackBufferImage != null) {
			mBackBufferImage.dispose();
			mBackBufferImage = null;
		}

		if (mBackBufferGC != null) {
			mBackBufferGC.dispose();
			mBackBufferGC = null;
		}
	}

	private void mouseMoved(MouseEvent e) {
		mMouseY = e.y;
		redraw();
	}

	private void mouseClicked(MouseEvent e) {
		if (mMouseInSelf) {
			int selIndex = mPositionHelper.getCallAt(mMouseY);
			sendCallSelectedEvent(selIndex);
			redraw();
		}
	}

	private void draw(Display display, GC gc) {
		if (mBackBufferImage == null) {
			initializeBackBuffer();
		}

		if (mBackBufferImage == null) {
			return;
		}

		// draw contents onto the back buffer
		drawBackground(mBackBufferGC, mBackBufferImage.getBounds());
		drawContextHeaders(mBackBufferGC);
		drawCallDurations(mBackBufferGC);
		drawVisibleCallHighlights(mBackBufferGC);
		drawMouseMarkers(mBackBufferGC);

		// finally copy over the rendered back buffer onto screen
		int width = getClientArea().width;
		int height = getClientArea().height;
		gc.drawImage(mBackBufferImage, 0, 0, width, height, 0, 0, width, height);
	}

	private void drawBackground(GC gc, Rectangle bounds) {
		gc.setBackground(mBackgroundColor);
		gc.fillRectangle(bounds);
	}

	private void drawContextHeaders(GC gc) {
		if (mContextCount <= 1) {
			return;
		}

		gc.setForeground(mContextHeaderColor);
		gc.setFont(mFontRegistry.get(FONT_KEY));
		for (int i = 0; i < mContextCount; i++) {
			Point p = mPositionHelper.getHeaderLocation(i);
			gc.drawText("CTX" + Integer.toString(i), p.x, p.y);
		}
	}

	/**
	 * Draw the call durations as a sequence of lines.
	 *
	 * Calls are arranged on the y-axis based on the sequence in which they were
	 * originally called by the application. If the display height is lesser
	 * than the number of calls, then not every call is shown - the calls are
	 * underscanned based the height of the display.
	 *
	 * The x-axis shows two pieces of information: the duration of the call, and
	 * the context in which the call was made. The duration controls how long
	 * the displayed line is, and the context controls the starting offset of
	 * the line.
	 */
	private void drawCallDurations(GC gc) {
		if (mCalls == null || mCalls.size() < mEndCallIndex) {
			return;
		}

		gc.setBackground(mDurationLineColor);

		int callUnderScan = mPositionHelper.getCallUnderScanValue();
		for (int i = mStartCallIndex; i < mEndCallIndex; i += callUnderScan) {
			boolean resetColor = false;
			GLCall c = mCalls.get(i);

			long duration = c.getWallDuration();

			if (c.hasErrors()) {
				gc.setBackground(mGlErrorColor);
				resetColor = true;

				// If the call has any errors, we want it to be visible in the
				// minimap
				// regardless of how long it took.
				duration = mPositionHelper.getMaxDuration();
			} else if (c.getFunction() == Function.glDrawArrays || c.getFunction() == Function.glDrawElements
					|| c.getFunction() == Function.eglSwapBuffers) {
				gc.setBackground(mGlDrawColor);
				resetColor = true;

				// render all draw calls & swap buffer at max length
				duration = mPositionHelper.getMaxDuration();
			}

			Rectangle bounds = mPositionHelper.getDurationBounds(i - mStartCallIndex, c.getContextId(), duration);
			gc.fillRectangle(bounds);

			if (resetColor) {
				gc.setBackground(mDurationLineColor);
			}
		}
	}

	/**
	 * Draw a bounding box that highlights the currently visible range of calls
	 * in the {@link GLFunctionTraceViewer} table.
	 */
	private void drawVisibleCallHighlights(GC gc) {
		gc.setAlpha(VISIBLE_CALLS_HIGHLIGHT_ALPHA);
		gc.setBackground(mVisibleCallsHighlightColor);
		gc.fillRectangle(mPositionHelper.getBoundsFramingCalls(mVisibleCallTopIndex - mStartCallIndex,
				mVisibleCallBottomIndex - mStartCallIndex));
		gc.setAlpha(DEFAULT_ALPHA);
	}

	private void drawMouseMarkers(GC gc) {
		if (!mMouseInSelf) {
			return;
		}

		if (mPositionHelper.getCallAt(mMouseY) < 0) {
			return;
		}

		gc.setForeground(mMouseMarkerColor);
		gc.drawLine(0, mMouseY, getClientArea().width, mMouseY);
	}

	private void controlResized() {
		// regenerate back buffer on size changes
		disposeBackBuffer();
		initializeBackBuffer();

		redraw();
	}

	public int getMinimumWidth() {
		return mPositionHelper.getMinimumWidth();
	}

	/** Set the GL Call start and end indices for currently displayed frame. */
	public void setCallRangeForCurrentFrame(int startCallIndex, int endCallIndex) {
		mStartCallIndex = startCallIndex;
		mEndCallIndex = endCallIndex;
		mPositionHelper.updateCallDensity(mEndCallIndex - mStartCallIndex, getClientArea().height);
		redraw();
	}

	/**
	 * Set the call range that is currently visible in the
	 * {@link GLFunctionTraceViewer} table.
	 * 
	 * @param visibleTopIndex
	 *            index of call currently visible at the top of the table.
	 * @param visibleBottomIndex
	 *            index of call currently visible at the bottom of the table.
	 */
	public void setVisibleCallRange(int visibleTopIndex, int visibleBottomIndex) {
		mVisibleCallTopIndex = visibleTopIndex;
		mVisibleCallBottomIndex = visibleBottomIndex;
		redraw();
	}

	public interface ICallSelectionListener {
		void callSelected(int selectedCallIndex);
	}

	private List<ICallSelectionListener> mListeners = new ArrayList<ICallSelectionListener>();

	public void addCallSelectionListener(ICallSelectionListener l) {
		mListeners.add(l);
	}

	private void sendCallSelectedEvent(int selectedCall) {
		for (ICallSelectionListener l : mListeners) {
			l.callSelected(selectedCall);
		}
	}

	/**
	 * Utility class to help with the positioning and sizes of elements in the
	 * canvas.
	 */
	private static class PositionHelper {
		/** Left Margin after which duration lines are drawn. */
		private static final int LEFT_MARGIN = 5;

		/** Top margin after which header is drawn. */
		private static final int TOP_MARGIN = 5;

		/**
		 * # of pixels of padding between duration markers for different
		 * contexts.
		 */
		private static final int CONTEXT_PADDING = 10;

		private final int mHeaderMargin;
		private final int mContextCount;
		private final int mMaxDurationLength;
		private final long mMaxDuration;
		private final double mScale;

		private int mCallCount;
		private int mNumCallsPerPixel = 1;

		public PositionHelper(int fontHeight, int contextCount, int maxDurationLength, long maxDuration) {
			mContextCount = contextCount;
			mMaxDurationLength = maxDurationLength;
			mMaxDuration = maxDuration;
			mScale = (double) maxDurationLength / maxDuration;

			// header region is present only there are multiple contexts
			if (mContextCount > 1) {
				mHeaderMargin = fontHeight * 3;
			} else {
				mHeaderMargin = 0;
			}
		}

		/** Get the minimum width of the canvas. */
		public int getMinimumWidth() {
			return LEFT_MARGIN + (mMaxDurationLength + CONTEXT_PADDING) * mContextCount;
		}

		/** Get the bounds for a call duration line. */
		public Rectangle getDurationBounds(int callIndex, int context, long duration) {
			if (duration <= 0) {
				duration = 1;
			} else if (duration > mMaxDuration) {
				duration = mMaxDuration;
			}

			int x = LEFT_MARGIN + ((mMaxDurationLength + CONTEXT_PADDING) * context);
			int y = (callIndex / mNumCallsPerPixel) + TOP_MARGIN + mHeaderMargin;
			int w = (int) (duration * mScale);
			int h = 1;

			return new Rectangle(x, y, w, h);
		}

		public long getMaxDuration() {
			return mMaxDuration;
		}

		/** Get the bounds for calls spanning given range. */
		public Rectangle getBoundsFramingCalls(int startCallIndex, int endCallIndex) {
			if (startCallIndex >= 0 && endCallIndex >= startCallIndex && endCallIndex <= mCallCount) {
				int x = LEFT_MARGIN;
				int y = (startCallIndex / mNumCallsPerPixel) + TOP_MARGIN + mHeaderMargin;
				int w = ((mMaxDurationLength + CONTEXT_PADDING) * mContextCount);
				int h = (endCallIndex - startCallIndex) / mNumCallsPerPixel;
				return new Rectangle(x, y, w, h);
			} else {
				return new Rectangle(0, 0, 0, 0);
			}
		}

		public Point getHeaderLocation(int context) {
			int x = LEFT_MARGIN + ((mMaxDurationLength + CONTEXT_PADDING) * context);
			return new Point(x, TOP_MARGIN);
		}

		/**
		 * Update the call density based on the number of calls to be displayed
		 * and the available height to display them in.
		 */
		public void updateCallDensity(int callCount, int displayHeight) {
			mCallCount = callCount;

			if (displayHeight <= 0) {
				displayHeight = callCount + 1;
			}

			mNumCallsPerPixel = (callCount / displayHeight) + 1;
		}

		/**
		 * Get the underscan value. In cases where there are more calls to be
		 * displayed than there are availble pixels, we only display 1 out of
		 * every underscan calls.
		 */
		public int getCallUnderScanValue() {
			return mNumCallsPerPixel;
		}

		/** Get the index of the call at given y offset. */
		public int getCallAt(int y) {
			if (!isWithinBounds(y)) {
				return -1;
			}

			Rectangle displayBounds = getBoundsFramingCalls(0, mCallCount);
			return (y - displayBounds.y) * mNumCallsPerPixel;
		}

		/** Does the provided y offset map to a valid call? */
		private boolean isWithinBounds(int y) {
			Rectangle displayBounds = getBoundsFramingCalls(0, mCallCount);
			if (y < displayBounds.y) {
				return false;
			}

			if (y > (displayBounds.y + displayBounds.height)) {
				return false;
			}

			return true;
		}
	}
}

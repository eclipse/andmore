/*
 * Copyright (C) 2013 The Android Open Source Project
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

package org.eclipse.andmore.internal.editors.draw9patch.ui;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.editors.draw9patch.graphics.NinePatchedImage;
import org.eclipse.andmore.internal.editors.draw9patch.graphics.NinePatchedImage.Chunk;
import org.eclipse.andmore.internal.editors.draw9patch.graphics.NinePatchedImage.Tick;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ScrollBar;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * View and edit Draw 9-patch image.
 */
public class ImageViewer extends Canvas implements PaintListener, KeyListener, MouseListener,
        MouseMoveListener {
    private static final boolean DEBUG = false;

    public static final String HELP_MESSAGE_KEY_TIPS = "Press Shift to erase pixels."
            + " Press Control to draw layout bounds.";

    public static final String HELP_MESSAGE_KEY_TIPS2 = "Release Shift to draw pixels.";

    private static final Color BLACK_COLOR = AdtPlugin.getDisplay().getSystemColor(SWT.COLOR_BLACK);
    private static final Color RED_COLOR = AdtPlugin.getDisplay().getSystemColor(SWT.COLOR_RED);

    private static final Color BACK_COLOR
            = new Color(AdtPlugin.getDisplay(), new RGB(0x00, 0xFF, 0x00));
    private static final Color LOCK_COLOR
            = new Color(AdtPlugin.getDisplay(), new RGB(0xFF, 0x00, 0x00));
    private static final Color PATCH_COLOR
            = new Color(AdtPlugin.getDisplay(), new RGB(0xFF, 0xFF, 0x00));
    private static final Color PATCH_ONEWAY_COLOR
            = new Color(AdtPlugin.getDisplay(), new RGB(0x00, 0x00, 0xFF));
    private static final Color CORRUPTED_COLOR
            = new Color(AdtPlugin.getDisplay(), new RGB(0xFF, 0x00, 0x00));

    private static final int NONE_ALPHA = 0xFF;
    private static final int LOCK_ALPHA = 50;
    private static final int PATCH_ALPHA = 50;
    private static final int GUIDE_ALPHA = 60;

    private static final int MODE_NONE = 0x00;
    private static final int MODE_BLACK_TICK = 0x01;
    private static final int MODE_RED_TICK = 0x02;
    private static final int MODE_ERASE = 0xFF;

    private int mDrawMode = MODE_NONE;

    private static final int MARGIN = 5;
    private static final String CHECKER_PNG_PATH = "/icons/checker.png";

    private Image mBackgroundLayer = null;

    private NinePatchedImage mNinePatchedImage = null;

    private Chunk[][] mChunks = null;
    private Chunk[][] mBadChunks = null;

    private boolean mIsLockShown = true;
    private boolean mIsPatchesShown = false;
    private boolean mIsBadPatchesShown = false;

    private ScrollBar mHorizontalBar;
    private ScrollBar mVerticalBar;

    private int mZoom = 500;

    private int mHorizontalScroll = 0;
    private int mVerticalScroll = 0;

    private final Rectangle mPadding = new Rectangle(0, 0, 0, 0);

    // one pixel size that considered zoom
    private int mZoomedPixelSize = 1;

    private Image mBufferImage = null;

    private boolean isCtrlPressed = false;
    private boolean isShiftPressed = false;

    private final List<UpdateListener> mUpdateListenerList
            = new ArrayList<UpdateListener>();

    private final Point mCursorPoint = new Point(0, 0);

    private static final int DEFAULT_UPDATE_QUEUE_SIZE = 10;

    private final ArrayBlockingQueue<NinePatchedImage> mUpdateQueue
            = new ArrayBlockingQueue<NinePatchedImage>(DEFAULT_UPDATE_QUEUE_SIZE);

    private final Runnable mUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            if (isDisposed()) {
                return;
            }

            redraw();

            fireUpdateEvent();
        }
    };

    private final Thread mUpdateThread = new Thread() {
        @Override
        public void run() {
            while (!isDisposed()) {
                try {
                    mUpdateQueue.take();
                    mNinePatchedImage.findPatches();
                    mNinePatchedImage.findContentsArea();

                    mChunks = mNinePatchedImage.getChunks(mChunks);
                    mBadChunks = mNinePatchedImage.getCorruptedChunks(mBadChunks);

                    AdtPlugin.getDisplay().asyncExec(mUpdateRunnable);

                } catch (InterruptedException e) {
                }
            }
        }
    };

    private StatusChangedListener mStatusChangedListener = null;

    public void addUpdateListener(UpdateListener l) {
        mUpdateListenerList.add(l);
    }

    public void removeUpdateListener(UpdateListener l) {
        mUpdateListenerList.remove(l);
    }

    private void fireUpdateEvent() {
        int len = mUpdateListenerList.size();
        for(int i=0; i < len; i++) {
            mUpdateListenerList.get(i).update(mNinePatchedImage);
        }
    }

    public void setStatusChangedListener(StatusChangedListener l) {
        mStatusChangedListener = l;
        if (mStatusChangedListener != null) {
            mStatusChangedListener.helpTextChanged(HELP_MESSAGE_KEY_TIPS);
        }
    }

    void setShowLock(boolean show) {
        mIsLockShown = show;
        redraw();
    }

    void setShowPatchesArea(boolean show) {
        mIsPatchesShown = show;
        redraw();
    }

    void setShowBadPatchesArea(boolean show) {
        mIsBadPatchesShown = show;
        redraw();
    }

    void setZoom(int zoom) {
        mZoom = zoom;
        mZoomedPixelSize = getZoomedPixelSize(1);
        redraw();
    }

    public ImageViewer(Composite parent, int style) {
        super(parent, style);

        mUpdateThread.start();

        mBackgroundLayer = AdtPlugin.getImageDescriptor(CHECKER_PNG_PATH).createImage();

        addMouseListener(this);
        addMouseMoveListener(this);
        addPaintListener(this);

        mHorizontalBar = getHorizontalBar();
        mHorizontalBar.setThumb(1);
        mHorizontalBar.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                super.widgetSelected(event);
                ScrollBar bar = (ScrollBar) event.widget;
                if (mHorizontalBar.isVisible()
                        && mHorizontalScroll != bar.getSelection()) {
                    mHorizontalScroll = bar.getSelection();
                    redraw();
                }
            }
        });

        mVerticalBar = getVerticalBar();
        mVerticalBar.setThumb(1);
        mVerticalBar.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                super.widgetSelected(event);
                ScrollBar bar = (ScrollBar) event.widget;
                if (mVerticalBar.isVisible()
                        && mVerticalScroll != bar.getSelection()) {
                    mVerticalScroll = bar.getSelection();
                    redraw();
                }
            }
        });
    }

    /**
     * Load the image file.
     *
     * @param fileName must be absolute path
     */
    public NinePatchedImage loadFile(String fileName) {
        mNinePatchedImage = new NinePatchedImage(fileName);

        return mNinePatchedImage;
    }

    /**
     * Start displaying the image.
     */
    public void startDisplay() {
        mZoomedPixelSize = getZoomedPixelSize(1);

        scheduleUpdate();
    }

    private void draw(int x, int y, int drawMode) {
        if (drawMode == MODE_ERASE) {
            erase(x, y);
        } else {
            int color = (drawMode == MODE_RED_TICK) ? NinePatchedImage.RED_TICK
                    : NinePatchedImage.BLACK_TICK;
            mNinePatchedImage.setPatch(x, y, color);
            redraw();

            scheduleUpdate();
        }
    }

    private void erase(int x, int y) {
        mNinePatchedImage.erase(x, y);
        redraw();

        scheduleUpdate();
    }

    private void scheduleUpdate() {
        try {
            mUpdateQueue.put(mNinePatchedImage);
        } catch (InterruptedException e) {
        }
    }

    @Override
    public void mouseDown(MouseEvent event) {
        if (event.button == 1 && !isShiftPressed) {
            mDrawMode = !isCtrlPressed ? MODE_BLACK_TICK : MODE_RED_TICK;
            draw(mCursorPoint.x, mCursorPoint.y, mDrawMode);
        } else if (event.button == 3 || isShiftPressed) {
            mDrawMode = MODE_ERASE;
            erase(mCursorPoint.x, mCursorPoint.y);
        }
    }

    @Override
    public void mouseUp(MouseEvent event) {
        mDrawMode = MODE_NONE;
    }

    @Override
    public void mouseDoubleClick(MouseEvent event) {
    }

    private int getLogicalPositionX(int x) {
        return Math.round((x - mPadding.x + mHorizontalScroll) / ((float) mZoom / 100));
    }

    private int getLogicalPositionY(int y) {
        return Math.round((y - mPadding.y + mVerticalScroll) / ((float) mZoom / 100));
    }

    @Override
    public void mouseMove(MouseEvent event) {
        int posX = getLogicalPositionX(event.x);
        int posY = getLogicalPositionY(event.y);

        int width = mNinePatchedImage.getWidth();
        int height = mNinePatchedImage.getHeight();

        if (posX < 0) {
            posX = 0;
        }
        if (posX >= width) {
            posX = width - 1;
        }
        if (posY < 0) {
            posY = 0;
        }
        if (posY >= height) {
            posY = height - 1;
        }

        if (mDrawMode != MODE_NONE) {
            int drawMode = mDrawMode;
            if (isShiftPressed) {
                drawMode = MODE_ERASE;
            } else if (mDrawMode != MODE_ERASE) {
                drawMode = !isCtrlPressed ? MODE_BLACK_TICK : MODE_RED_TICK;
            }

            /*
             * Consider the previous cursor position because mouseMove events are
             * scatter.
             */
            int x = mCursorPoint.x;
            int y = mCursorPoint.y;
            for (; y != posY; y += (y > posY) ? -1 : 1) {
                draw(x, y, drawMode);
            }

            x = mCursorPoint.x;
            y = mCursorPoint.y;
            for (; x != posX; x += (x > posX) ? -1 : 1) {
                draw(x, y, drawMode);
            }
        }
        mCursorPoint.x = posX;
        mCursorPoint.y = posY;

        redraw();

        if (mStatusChangedListener != null) {
            // Update position on status panel if position is in logical size.
            if (posX >= 0 && posY >= 0
                    && posX <= mNinePatchedImage.getWidth()
                    && posY <= mNinePatchedImage.getHeight()) {
                mStatusChangedListener.cursorPositionChanged(posX + 1, posY + 1);
            }
        }
    }

    private synchronized void calcPaddings(int width, int height) {
        Point canvasSize = getSize();

        mPadding.width = getZoomedPixelSize(width);
        mPadding.height = getZoomedPixelSize(height);

        int margin = getZoomedPixelSize(MARGIN);

        if (mPadding.width < canvasSize.x) {
            mPadding.x = (canvasSize.x - mPadding.width) / 2;
        } else {
            mPadding.x = margin;
        }

        if (mPadding.height < canvasSize.y) {
            mPadding.y = (canvasSize.y - mPadding.height) / 2;
        } else {
            mPadding.y = margin;
        }
    }

    private void calcScrollBarSettings() {
        Point size = getSize();
        int screenWidth = size.x;
        int screenHeight = size.y;

        int imageWidth = getZoomedPixelSize(mNinePatchedImage.getWidth() + 1);
        int imageHeight = getZoomedPixelSize(mNinePatchedImage.getHeight() + 1);

        // consider the scroll bar sizes
        int verticalBarSize = mVerticalBar.getSize().x;
        int horizontalBarSize = mHorizontalBar.getSize().y;

        int horizontalScroll = imageWidth - (screenWidth - verticalBarSize);
        int verticalScroll = imageHeight - (screenHeight - horizontalBarSize);

        int margin = getZoomedPixelSize(MARGIN) * 2;

        if (horizontalScroll > 0) {
            mHorizontalBar.setVisible(true);

            // horizontal maximum
            int max = horizontalScroll + verticalBarSize + margin;
            mHorizontalBar.setMaximum(max);

            // set corrected scroll size
            int value = mHorizontalBar.getSelection();
            value = max < value ? max : value;

            mHorizontalBar.setSelection(value);
            mHorizontalScroll = value;

        } else {
            mHorizontalBar.setSelection(0);
            mHorizontalBar.setMaximum(0);
            mHorizontalBar.setVisible(false);
        }

        if (verticalScroll > 0) {
            mVerticalBar.setVisible(true);

            // vertical maximum
            int max = verticalScroll + horizontalBarSize + margin;
            mVerticalBar.setMaximum(max);

            // set corrected scroll size
            int value = mVerticalBar.getSelection();
            value = max < value ? max : value;

            mVerticalBar.setSelection(value);
            mVerticalScroll = value;

        } else {
            mVerticalBar.setSelection(0);
            mVerticalBar.setMaximum(0);
            mVerticalBar.setVisible(false);
        }
    }

    private int getZoomedPixelSize(int val) {
        return Math.round(val * (float) mZoom / 100);
    }

    @Override
    public void paintControl(PaintEvent pe) {
        if (mNinePatchedImage == null) {
            return;
        }

        // Use buffer
        GC bufferGc = null;
        if (mBufferImage == null) {
            mBufferImage = new Image(AdtPlugin.getDisplay(), pe.width, pe.height);
        } else {
            int width = mBufferImage.getBounds().width;
            int height = mBufferImage.getBounds().height;
            if (width != pe.width || height != pe.height) {
                mBufferImage = new Image(AdtPlugin.getDisplay(), pe.width, pe.height);
            }
        }

        // Draw previous image once for prevent flicking
        pe.gc.drawImage(mBufferImage, 0, 0);

        bufferGc = new GC(mBufferImage);
        bufferGc.setAdvanced(true);

        // Make interpolation disable
        bufferGc.setInterpolation(SWT.NONE);

        // clear buffer
        bufferGc.fillRectangle(0, 0, pe.width, pe.height);

        calcScrollBarSettings();

        // padding from current zoom
        int width = mNinePatchedImage.getWidth();
        int height = mNinePatchedImage.getHeight();
        calcPaddings(width, height);

        int baseX = mPadding.x - mHorizontalScroll;
        int baseY = mPadding.y - mVerticalScroll;

        // draw checker image
        bufferGc.drawImage(mBackgroundLayer,
                0, 0, mBackgroundLayer.getImageData().width,
                mBackgroundLayer.getImageData().height,
                baseX, baseY, mPadding.width, mPadding.height);

        if (DEBUG) {
            System.out.println(String.format("%d,%d %d,%d %d,%d",
                    width, height, baseX, baseY, mPadding.width, mPadding.height));
        }

        // draw image
        /* TODO: Do not draw invisible area, for better performance. */
        bufferGc.drawImage(mNinePatchedImage.getImage(), 0, 0, width, height, baseX, baseY,
                mPadding.width, mPadding.height);

        bufferGc.setBackground(BLACK_COLOR);

        // draw patch ticks
        drawHorizontalPatches(bufferGc, baseX, baseY);
        drawVerticalPatches(bufferGc, baseX, baseY);

        // draw content ticks
        drawHorizontalContentArea(bufferGc, baseX, baseY);
        drawVerticalContentArea(bufferGc, baseX, baseY);

        if (mNinePatchedImage.isValid(mCursorPoint.x, mCursorPoint.y)) {
            bufferGc.setForeground(BLACK_COLOR);
        } else if (mIsLockShown) {
            drawLockArea(bufferGc, baseX, baseY);
        }

        // Patches
        if (mIsPatchesShown) {
            drawPatchAreas(bufferGc, baseX, baseY);
        }

        // Bad patches
        if (mIsBadPatchesShown) {
            drawBadPatchAreas(bufferGc, baseX, baseY);
        }

        if (mNinePatchedImage.isValid(mCursorPoint.x, mCursorPoint.y)) {
            bufferGc.setForeground(BLACK_COLOR);
        } else {
            bufferGc.setForeground(RED_COLOR);
        }

        drawGuideLine(bufferGc, baseX, baseY);

        bufferGc.dispose();

        pe.gc.drawImage(mBufferImage, 0, 0);
    }

    private static final Color getColor(int color) {
        switch (color) {
            case NinePatchedImage.RED_TICK:
                return RED_COLOR;
            default:
                return BLACK_COLOR;
        }
    }

    private void drawVerticalPatches(GC gc, int baseX, int baseY) {
        List<Tick> verticalPatches = mNinePatchedImage.getVerticalPatches();
        for (Tick t : verticalPatches) {
            if (t.color != NinePatchedImage.TRANSPARENT_TICK) {
                gc.setBackground(getColor(t.color));
                gc.fillRectangle(
                        baseX,
                        baseY + getZoomedPixelSize(t.start),
                        mZoomedPixelSize,
                        getZoomedPixelSize(t.getLength()));
            }
        }
    }

    private void drawHorizontalPatches(GC gc, int baseX, int baseY) {
        List<Tick> horizontalPatches = mNinePatchedImage.getHorizontalPatches();
        for (Tick t : horizontalPatches) {
            if (t.color != NinePatchedImage.TRANSPARENT_TICK) {
                gc.setBackground(getColor(t.color));
                gc.fillRectangle(
                        baseX + getZoomedPixelSize(t.start),
                        baseY,
                        getZoomedPixelSize(t.getLength()),
                        mZoomedPixelSize);
            }
        }
    }

    private void drawHorizontalContentArea(GC gc, int baseX, int baseY) {
        List<Tick> horizontalContentArea = mNinePatchedImage.getHorizontalContents();
        for (Tick t : horizontalContentArea) {
            if (t.color != NinePatchedImage.TRANSPARENT_TICK) {
                gc.setBackground(getColor(t.color));
                gc.fillRectangle(
                        baseX + getZoomedPixelSize(t.start),
                        baseY + getZoomedPixelSize(mNinePatchedImage.getHeight() - 1),
                        getZoomedPixelSize(t.getLength()),
                        mZoomedPixelSize);
            }
        }

    }

    private void drawVerticalContentArea(GC gc, int baseX, int baseY) {
        List<Tick> verticalContentArea = mNinePatchedImage.getVerticalContents();
        for (Tick t : verticalContentArea) {
            if (t.color != NinePatchedImage.TRANSPARENT_TICK) {
                gc.setBackground(getColor(t.color));
                gc.fillRectangle(
                        baseX + getZoomedPixelSize(mNinePatchedImage.getWidth() - 1),
                        baseY + getZoomedPixelSize(t.start),
                        mZoomedPixelSize,
                        getZoomedPixelSize(t.getLength()));
            }
        }
    }

    private void drawLockArea(GC gc, int baseX, int baseY) {
        gc.setAlpha(LOCK_ALPHA);
        gc.setForeground(LOCK_COLOR);
        gc.setBackground(LOCK_COLOR);

        gc.fillRectangle(
                baseX + mZoomedPixelSize,
                baseY + mZoomedPixelSize,
                getZoomedPixelSize(mNinePatchedImage.getWidth() - 2),
                getZoomedPixelSize(mNinePatchedImage.getHeight() - 2));
        gc.setAlpha(NONE_ALPHA);

    }

    private void drawPatchAreas(GC gc, int baseX, int baseY) {
        if (mChunks != null) {
            int yLen = mChunks.length;
            int xLen = mChunks[0].length;

            gc.setAlpha(PATCH_ALPHA);

            for (int yPos = 0; yPos < yLen; yPos++) {
                for (int xPos = 0; xPos < xLen; xPos++) {
                    Chunk c = mChunks[yPos][xPos];

                    if (c.type == Chunk.TYPE_FIXED) {
                        gc.setBackground(BACK_COLOR);
                    } else if (c.type == Chunk.TYPE_HORIZONTAL) {
                        gc.setBackground(PATCH_ONEWAY_COLOR);
                    } else if (c.type == Chunk.TYPE_VERTICAL) {
                        gc.setBackground(PATCH_ONEWAY_COLOR);
                    } else if (c.type == Chunk.TYPE_HORIZONTAL + Chunk.TYPE_VERTICAL) {
                        gc.setBackground(PATCH_COLOR);
                    }
                    Rectangle r = c.rect;
                    gc.fillRectangle(
                            baseX + getZoomedPixelSize(r.x),
                            baseY + getZoomedPixelSize(r.y),
                            getZoomedPixelSize(r.width),
                            getZoomedPixelSize(r.height));
                }
            }
        }
        gc.setAlpha(NONE_ALPHA);
    }

    private void drawBadPatchAreas(GC gc, int baseX, int baseY) {
        if (mBadChunks != null) {
            int yLen = mBadChunks.length;
            int xLen = mBadChunks[0].length;

            gc.setAlpha(NONE_ALPHA);
            gc.setForeground(CORRUPTED_COLOR);
            gc.setBackground(CORRUPTED_COLOR);

            for (int yPos = 0; yPos < yLen; yPos++) {
                for (int xPos = 0; xPos < xLen; xPos++) {
                    Chunk c = mBadChunks[yPos][xPos];
                    if ((c.type & Chunk.TYPE_CORRUPT) != 0) {
                        Rectangle r = c.rect;
                        gc.drawRectangle(
                                baseX + getZoomedPixelSize(r.x),
                                baseY + getZoomedPixelSize(r.y),
                                getZoomedPixelSize(r.width),
                                getZoomedPixelSize(r.height));
                    }
                }
            }
        }
    }

    private void drawGuideLine(GC gc, int baseX, int baseY) {
        gc.setAntialias(SWT.ON);
        gc.setInterpolation(SWT.HIGH);

        int x = Math.round((mCursorPoint.x * ((float) mZoom / 100) + baseX)
                + ((float) mZoom / 100 / 2));
        int y = Math.round((mCursorPoint.y * ((float) mZoom / 100) + baseY)
                + ((float) mZoom / 100 / 2));
        gc.setAlpha(GUIDE_ALPHA);

        Point size = getSize();
        gc.drawLine(x, 0, x, size.y);
        gc.drawLine(0, y, size.x, y);

        gc.setAlpha(NONE_ALPHA);
    }

    @Override
    public void keyPressed(KeyEvent event) {
        int keycode = event.keyCode;
        if (keycode == SWT.CTRL) {
            isCtrlPressed = true;
        }
        if (keycode == SWT.SHIFT) {
            isShiftPressed = true;
            if (mStatusChangedListener != null) {
                mStatusChangedListener.helpTextChanged(HELP_MESSAGE_KEY_TIPS2);
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent event) {
        int keycode = event.keyCode;
        if (keycode == SWT.CTRL) {
            isCtrlPressed = false;
        }
        if (keycode == SWT.SHIFT) {
            isShiftPressed = false;
            if (mStatusChangedListener != null) {
                mStatusChangedListener.helpTextChanged(HELP_MESSAGE_KEY_TIPS);
            }
        }
    }

    @Override
    public void dispose() {
        mBackgroundLayer.dispose();
        super.dispose();
    }

    /**
     * Listen image updated event.
     */
    public interface UpdateListener {
        /**
         * 9-patched image has been updated.
         */
        public void update(NinePatchedImage image);
    }

    /**
     * Listen status changed event.
     */
    public interface StatusChangedListener {
        /**
         * Mouse cursor position has been changed.
         */
        public void cursorPositionChanged(int x, int y);

        /**
         * Help text has been changed.
         */
        public void helpTextChanged(String text);
    }
}

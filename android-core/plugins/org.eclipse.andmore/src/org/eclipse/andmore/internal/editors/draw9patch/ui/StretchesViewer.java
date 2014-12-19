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
import org.eclipse.andmore.internal.editors.draw9patch.graphics.GraphicsUtilities;
import org.eclipse.andmore.internal.editors.draw9patch.graphics.NinePatchedImage;
import org.eclipse.andmore.internal.editors.draw9patch.graphics.NinePatchedImage.Projection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

/**
 * Preview 9-patched image pane.
 */
public class StretchesViewer extends Composite {
    private static final boolean DEBUG = false;

    private static final int PADDING_COLOR = 0x0000CC;

    private static final int PADDING_COLOR_ALPHA = 100;

    private static final PaletteData PADDING_PALLET
            = new PaletteData(new RGB[] {new RGB(0x00, 0x00, 0xCC)});

    private static final String CHECKER_PNG_PATH = "/icons/checker.png";

    private Image mBackgroundLayer = null;

    private final StretchView mHorizontal;
    private final StretchView mVertical;

    private final StretchView mBoth;

    private NinePatchedImage mNinePatchedImage = null;

    private ImageData mContentAreaImageData = null;

    private boolean mIsContentAreaShown = false;

    private Image mContentAreaImage = null;

    private int mScale = 2;

    public StretchesViewer(Composite parent, int style) {
        super(parent, style);

        mBackgroundLayer = AdtPlugin.getImageDescriptor(CHECKER_PNG_PATH).createImage();

        setLayout(new FillLayout(SWT.VERTICAL));

        mHorizontal = new StretchView(this, SWT.NULL);
        mVertical = new StretchView(this, SWT.NULL);
        mBoth = new StretchView(this, SWT.NULL);
    }

    /**
     * Set show/not show content area.
     * @param If show, true
     */
    public void setShowContentArea(boolean show) {
        mIsContentAreaShown = show;
        redraw();
    }

    private static final boolean equalSize(ImageData image1, ImageData image2) {
        return (image1.width == image2.width && image1.height == image2.height);
    }

    /**
     * Update preview image.
     */
    public void updatePreview(NinePatchedImage image) {
        mNinePatchedImage = image;
        ImageData base = mNinePatchedImage.getImageData();

        if (mContentAreaImageData == null
                || (mContentAreaImageData != null && !equalSize(base, mContentAreaImageData))) {
            mContentAreaImageData = new ImageData(
                    base.width,
                    base.height,
                    1,
                    PADDING_PALLET);
        } else {
            GraphicsUtilities.clearImageData(mContentAreaImageData);
        }

        mHorizontal.setImage(mNinePatchedImage);
        mVertical.setImage(mNinePatchedImage);
        mBoth.setImage(mNinePatchedImage);

        mContentAreaImage = buildContentAreaPreview();

        setScale(mScale);
    }

    private Image buildContentAreaPreview() {
        if (mContentAreaImage != null) {
            mContentAreaImage.dispose();
        }

        Rectangle rect = mNinePatchedImage.getContentArea();

        int yLen = rect.y + rect.height;
        for (int y = rect.y; y < yLen; y++) {
            int xLen = rect.x + rect.width;
            for (int x = rect.x; x < xLen; x++) {
                mContentAreaImageData.setPixel(x, y, PADDING_COLOR);
                mContentAreaImageData.setAlpha(x, y, PADDING_COLOR_ALPHA);
            }
        }
        return new Image(AdtPlugin.getDisplay(), mContentAreaImageData);
    }

    public void setScale(int scale) {
        if (DEBUG) {
            System.out.println("scale = " + scale);
        }

        mScale = scale;
        int imageWidth = mNinePatchedImage.getWidth();
        int imageHeight = mNinePatchedImage.getHeight();

        mHorizontal.setSize(imageWidth * scale, imageHeight);
        mVertical.setSize(imageWidth, imageHeight * scale);
        mBoth.setSize(imageWidth * scale, imageHeight * scale);

        redraw();
    }

    @Override
    public void dispose() {
        mBackgroundLayer.dispose();
        super.dispose();
    }

    private class StretchView extends Canvas implements PaintListener {

        private final Point mSize = new Point(0, 0);
        private final Rectangle mPadding = new Rectangle(0, 0, 0, 0);
        private Projection[][] mProjection = null;

        public StretchView(Composite parent, int style) {
            super(parent, style);
            addPaintListener(this);
        }

        private void setImage(NinePatchedImage image) {
            setSize(image.getWidth(), image.getHeight());
        }

        @Override
        public void setSize(int width, int heigh) {
            mSize.x = width;
            mSize.y = heigh;
            mProjection = mNinePatchedImage.getProjections(mSize.x, mSize.y, mProjection);
        }

        private synchronized void calcPaddings(int width, int height) {
            Point canvasSize = getSize();

            mPadding.x = (canvasSize.x - width) / 2;
            mPadding.y = (canvasSize.y - height) / 2;

            mPadding.width = width;
            mPadding.height = height;
        }

        @Override
        public void paintControl(PaintEvent pe) {
            if (mNinePatchedImage == null || mProjection == null) {
                return;
            }

            Point size = getSize();

            // relative scaling
            float ratio = 1.0f;
            float wRatio = ((float) size.x / mSize.x);
            ratio = Math.min(wRatio, ratio);
            float hRatio = ((float) size.y / mSize.y);
            ratio = Math.min(hRatio, ratio);

            int width = Math.round(mSize.x * ratio);
            int height = Math.round(mSize.y * ratio);

            calcPaddings(width, height);

            Rectangle dest = new Rectangle(0, 0, 0, 0);

            GC gc = pe.gc;

            int backgroundLayerWidth = mBackgroundLayer.getImageData().width;
            int backgroundLayerHeight = mBackgroundLayer.getImageData().height;

            int yCount = size.y / backgroundLayerHeight
                    + ((size.y % backgroundLayerHeight) > 0 ? 1 : 0);
            int xCount = size.x / backgroundLayerWidth
                    + ((size.x % backgroundLayerWidth) > 0 ? 1 : 0);

            // draw background layer
            for (int y = 0; y < yCount; y++) {
                for (int x = 0; x < xCount; x++) {
                    gc.drawImage(mBackgroundLayer,
                            x * backgroundLayerWidth,
                            y * backgroundLayerHeight);
                }
            }

            // draw the border line
            gc.setAlpha(0x88);
            gc.drawRectangle(0, 0, size.x, size.y);
            gc.setAlpha(0xFF);

            int yLen = mProjection.length;
            int xLen = mProjection[0].length;
            for (int yPos = 0; yPos < yLen; yPos++) {
                for (int xPos = 0; xPos < xLen; xPos++) {
                    Projection p = mProjection[yPos][xPos];

                    // consider the scale
                    dest.x = (int) Math.ceil(p.dest.x * ratio);
                    dest.y = (int) Math.ceil(p.dest.y * ratio);
                    dest.width = (int) Math.ceil(p.dest.width * ratio);
                    dest.height = (int) Math.ceil(p.dest.height * ratio);

                    gc.drawImage(mNinePatchedImage.getImage(), p.src.x, p.src.y,
                            p.src.width, p.src.height,
                            (mPadding.x + dest.x), (mPadding.y + dest.y),
                            dest.width, dest.height);

                    if (mIsContentAreaShown) {
                        gc.drawImage(mContentAreaImage, p.src.x, p.src.y,
                                p.src.width, p.src.height,
                                (mPadding.x + dest.x), (mPadding.y + dest.y),
                                dest.width, dest.height);
                    }
                }
            }
        }
    }
}

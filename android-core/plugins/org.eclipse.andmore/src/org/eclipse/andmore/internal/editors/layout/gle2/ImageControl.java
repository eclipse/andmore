/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.eclipse.andmore.internal.editors.layout.gle2;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

/**
 * An ImageControl which simply renders an image, with optional margins and tooltips. This
 * is useful since a {@link CLabel}, even without text, will hide the image when there is
 * not enough room to fully fit it.
 * <p>
 * The image is always rendered left and top aligned.
 */
public class ImageControl extends Canvas implements MouseTrackListener {
    private Image mImage;
    private int mLeftMargin;
    private int mTopMargin;
    private int mRightMargin;
    private int mBottomMargin;
    private boolean mDisposeImage = true;
    private boolean mMouseIn;
    private Color mHoverColor;
    private float mScale = 1.0f;

    /**
     * Creates an ImageControl rendering the given image, which will be disposed when this
     * control is disposed (unless the {@link #setDisposeImage} method is called to turn
     * off auto dispose).
     *
     * @param parent the parent to add the image control to
     * @param style the SWT style to use
     * @param image the image to be rendered, which must not be null and should be unique
     *            for this image control since it will be disposed by this control when
     *            the control is disposed (unless the {@link #setDisposeImage} method is
     *            called to turn off auto dispose)
     */
    public ImageControl(@NonNull Composite parent, int style, @Nullable Image image) {
        super(parent, style | SWT.NO_FOCUS | SWT.DOUBLE_BUFFERED);
        mImage = image;

        addPaintListener(new PaintListener() {
            @Override
            public void paintControl(PaintEvent event) {
                onPaint(event);
            }
        });
    }

    @Nullable
    public Image getImage() {
        return mImage;
    }

    public void setImage(@Nullable Image image) {
        if (mDisposeImage && mImage != null) {
            mImage.dispose();
        }
        mImage = image;
        redraw();
    }

    public void fitToWidth(int width) {
        if (mImage == null) {
            return;
        }
        Rectangle imageRect = mImage.getBounds();
        int imageWidth = imageRect.width;
        if (imageWidth <= width) {
            mScale = 1.0f;
            return;
        }

        mScale = width / (float) imageWidth;
        redraw();
    }

    public void setScale(float scale) {
        mScale = scale;
    }

    public float getScale() {
        return mScale;
    }

    public void setHoverColor(@Nullable Color hoverColor) {
        if (mHoverColor != null) {
            removeMouseTrackListener(this);
        }
        mHoverColor = hoverColor;
        if (hoverColor != null) {
            addMouseTrackListener(this);
        }
    }

    @Nullable
    public Color getHoverColor() {
        return mHoverColor;
    }

    @Override
    public void dispose() {
        super.dispose();

        if (mDisposeImage && mImage != null && !mImage.isDisposed()) {
            mImage.dispose();
        }
        mImage = null;
    }

    public void setDisposeImage(boolean disposeImage) {
        mDisposeImage = disposeImage;
    }

    public boolean getDisposeImage() {
        return mDisposeImage;
    }

    @Override
    public Point computeSize(int wHint, int hHint, boolean changed) {
        checkWidget();
        Point e = new Point(0, 0);
        if (mImage != null) {
            Rectangle r = mImage.getBounds();
            if (mScale != 1.0f) {
                e.x += mScale * r.width;
                e.y += mScale * r.height;
            } else {
                e.x += r.width;
                e.y += r.height;
            }
        }
        if (wHint == SWT.DEFAULT) {
            e.x += mLeftMargin + mRightMargin;
        } else {
            e.x = wHint;
        }
        if (hHint == SWT.DEFAULT) {
            e.y += mTopMargin + mBottomMargin;
        } else {
            e.y = hHint;
        }

        return e;
    }

    private void onPaint(PaintEvent event) {
        Rectangle rect = getClientArea();
        if (mImage == null || rect.width == 0 || rect.height == 0) {
            return;
        }

        GC gc = event.gc;
        Rectangle imageRect = mImage.getBounds();
        int imageHeight = imageRect.height;
        int imageWidth = imageRect.width;
        int destWidth = imageWidth;
        int destHeight = imageHeight;

        int oldGcAlias = gc.getAntialias();
        int oldGcInterpolation = gc.getInterpolation();
        if (mScale != 1.0f) {
            destWidth = (int) (mScale * destWidth);
            destHeight = (int) (mScale * destHeight);
            gc.setAntialias(SWT.ON);
            gc.setInterpolation(SWT.HIGH);
        }

        gc.drawImage(mImage, 0, 0, imageWidth, imageHeight, rect.x + mLeftMargin, rect.y
                + mTopMargin, destWidth, destHeight);

        gc.setAntialias(oldGcAlias);
        gc.setInterpolation(oldGcInterpolation);

        if (mHoverColor != null && mMouseIn) {
            gc.setAlpha(60);
            gc.setBackground(mHoverColor);
            gc.setLineWidth(1);
            gc.fillRectangle(0, 0, destWidth, destHeight);
        }
    }

    public void setMargins(int leftMargin, int topMargin, int rightMargin, int bottomMargin) {
        checkWidget();
        mLeftMargin = Math.max(0, leftMargin);
        mTopMargin = Math.max(0, topMargin);
        mRightMargin = Math.max(0, rightMargin);
        mBottomMargin = Math.max(0, bottomMargin);
        redraw();
    }

    // ---- Implements MouseTrackListener ----

    @Override
    public void mouseEnter(MouseEvent e) {
        mMouseIn = true;
        if (mHoverColor != null) {
            redraw();
        }
    }

    @Override
    public void mouseExit(MouseEvent e) {
        mMouseIn = false;
        if (mHoverColor != null) {
            redraw();
        }
    }

    @Override
    public void mouseHover(MouseEvent e) {
    }
}

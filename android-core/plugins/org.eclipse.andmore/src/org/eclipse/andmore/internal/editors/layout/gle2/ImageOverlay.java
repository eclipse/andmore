/*
 * Copyright (C) 2010 The Android Open Source Project
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

import static org.eclipse.andmore.internal.editors.layout.gle2.ImageUtils.SHADOW_SIZE;

import com.android.SdkConstants;
import com.android.annotations.Nullable;
import com.android.ide.common.api.Rect;
import com.android.ide.common.rendering.api.IImageFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.WritableRaster;
import java.lang.ref.SoftReference;

/**
 * The {@link ImageOverlay} class renders an image as an overlay.
 */
public class ImageOverlay extends Overlay implements IImageFactory {
    /**
     * Whether the image should be pre-scaled (scaled to the zoom level) once
     * instead of dynamically during each paint; this is necessary on some
     * platforms (see issue #19447)
     */
    private static final boolean PRESCALE =
            // Currently this is necessary on Linux because the "Cairo" library
            // seems to be a bottleneck
            SdkConstants.CURRENT_PLATFORM == SdkConstants.PLATFORM_LINUX
                    && !(Boolean.getBoolean("adt.noprescale")); //$NON-NLS-1$

    /** Current background image. Null when there's no image. */
    private Image mImage;

    /** A pre-scaled version of the image */
    private Image mPreScaledImage;

    /** Whether the rendered image should have a drop shadow */
    private boolean mShowDropShadow;

    /** Current background AWT image. This is created by {@link #getImage()}, which is called
     * by the LayoutLib. */
    private SoftReference<BufferedImage> mAwtImage = new SoftReference<BufferedImage>(null);

    /**
     * Strong reference to the image in the above soft reference, to prevent
     * garbage collection when {@link PRESCALE} is set, until the scaled image
     * is created (lazily as part of the next paint call, where this strong
     * reference is nulled out and the above soft reference becomes eligible to
     * be reclaimed when memory is low.)
     */
    @SuppressWarnings("unused") // Used by the garbage collector to keep mAwtImage non-soft
    private BufferedImage mAwtImageStrongRef;

    /** The associated {@link LayoutCanvas}. */
    private LayoutCanvas mCanvas;

    /** Vertical scaling & scrollbar information. */
    private CanvasTransform mVScale;

    /** Horizontal scaling & scrollbar information. */
    private CanvasTransform mHScale;

    /**
     * Constructs an {@link ImageOverlay} tied to the given canvas.
     *
     * @param canvas The {@link LayoutCanvas} to paint the overlay over.
     * @param hScale The horizontal scale information.
     * @param vScale The vertical scale information.
     */
    public ImageOverlay(LayoutCanvas canvas, CanvasTransform hScale, CanvasTransform vScale) {
        mCanvas = canvas;
        mHScale = hScale;
        mVScale = vScale;
    }

    @Override
    public void create(Device device) {
        super.create(device);
    }

    @Override
    public void dispose() {
        if (mImage != null) {
            mImage.dispose();
            mImage = null;
        }
        if (mPreScaledImage != null) {
            mPreScaledImage.dispose();
            mPreScaledImage = null;
        }
    }

    /**
     * Sets the image to be drawn as an overlay from the passed in AWT
     * {@link BufferedImage} (which will be converted to an SWT image).
     * <p/>
     * The image <b>can</b> be null, which is the case when we are dealing with
     * an empty document.
     *
     * @param awtImage The AWT image to be rendered as an SWT image.
     * @param isAlphaChannelImage whether the alpha channel of the image is relevant
     * @return The corresponding SWT image, or null.
     */
    public synchronized Image setImage(BufferedImage awtImage, boolean isAlphaChannelImage) {
        mShowDropShadow = !isAlphaChannelImage;

        BufferedImage oldAwtImage = mAwtImage.get();
        if (awtImage != oldAwtImage || awtImage == null) {
            mAwtImage.clear();
            mAwtImageStrongRef = null;

            if (mImage != null) {
                mImage.dispose();
            }

            if (awtImage == null) {
                mImage = null;
            } else {
                mImage = SwtUtils.convertToSwt(mCanvas.getDisplay(), awtImage,
                        isAlphaChannelImage, -1);
            }
        } else {
            assert awtImage instanceof SwtReadyBufferedImage;

            if (isAlphaChannelImage) {
                if (mImage != null) {
                    mImage.dispose();
                }

                mImage = SwtUtils.convertToSwt(mCanvas.getDisplay(), awtImage, true, -1);
            } else {
                Image prev = mImage;
                mImage = ((SwtReadyBufferedImage)awtImage).getSwtImage();
                if (prev != mImage && prev != null) {
                    prev.dispose();
                }
            }
        }

        if (mPreScaledImage != null) {
            // Force refresh on next paint
            mPreScaledImage.dispose();
            mPreScaledImage = null;
        }

        return mImage;
    }

    /**
     * Returns the currently painted image, or null if none has been set
     *
     * @return the currently painted image or null
     */
    public Image getImage() {
        return mImage;
    }

    /**
     * Returns the currently rendered image, or null if none has been set
     *
     * @return the currently rendered image or null
     */
    @Nullable
    BufferedImage getAwtImage() {
        BufferedImage awtImage = mAwtImage.get();
        if (awtImage == null && mImage != null) {
            awtImage = SwtUtils.convertToAwt(mImage);
        }

        return awtImage;
    }

    /**
     * Returns whether this image overlay should be painted with a drop shadow.
     * This is usually the case, but not for transparent themes like the dialog
     * theme (Theme.*Dialog), which already provides its own shadow.
     *
     * @return true if the image overlay should be shown with a drop shadow.
     */
    public boolean getShowDropShadow() {
        return mShowDropShadow;
    }

    @Override
    public synchronized void paint(GC gc) {
        if (mImage != null) {
            boolean valid = mCanvas.getViewHierarchy().isValid();
            mCanvas.ensureZoomed();
            if (!valid) {
                gc_setAlpha(gc, 128); // half-transparent
            }

            CanvasTransform hi = mHScale;
            CanvasTransform vi = mVScale;

            // On some platforms, dynamic image scaling is very slow (see issue #19447) so
            // compute a pre-scaled version of the image once and render that instead.
            // This is done lazily in paint rather than when the image changes because
            // the image must be rescaled each time the zoom level changes, which varies
            // independently from when the image changes.
            BufferedImage awtImage = mAwtImage.get();
            if (PRESCALE && awtImage != null) {
                int imageWidth = (mPreScaledImage == null) ? 0
                        : mPreScaledImage.getImageData().width
                            - (mShowDropShadow ? SHADOW_SIZE : 0);
                if (mPreScaledImage == null || imageWidth != hi.getScaledImgSize()) {
                    double xScale = hi.getScaledImgSize() / (double) awtImage.getWidth();
                    double yScale = vi.getScaledImgSize() / (double) awtImage.getHeight();
                    BufferedImage scaledAwtImage;

                    // NOTE: == comparison on floating point numbers is okay
                    // here because we normalize the scaling factor
                    // to an exact 1.0 in the zooming code when the value gets
                    // near 1.0 to make painting more efficient in the presence
                    // of rounding errors.
                    if (xScale == 1.0 && yScale == 1.0) {
                        // Scaling to 100% is easy!
                        scaledAwtImage = awtImage;

                        if (mShowDropShadow) {
                            // Just need to draw drop shadows
                            scaledAwtImage = ImageUtils.createRectangularDropShadow(awtImage);
                        }
                    } else {
                        if (mShowDropShadow) {
                            scaledAwtImage = ImageUtils.scale(awtImage, xScale, yScale,
                                    SHADOW_SIZE, SHADOW_SIZE);
                            ImageUtils.drawRectangleShadow(scaledAwtImage, 0, 0,
                                    scaledAwtImage.getWidth() - SHADOW_SIZE,
                                    scaledAwtImage.getHeight() - SHADOW_SIZE);
                        } else {
                            scaledAwtImage = ImageUtils.scale(awtImage, xScale, yScale);
                        }
                    }

                    if (mPreScaledImage != null && !mPreScaledImage.isDisposed()) {
                        mPreScaledImage.dispose();
                    }
                    mPreScaledImage = SwtUtils.convertToSwt(mCanvas.getDisplay(), scaledAwtImage,
                            true /*transferAlpha*/, -1);
                    // We can't just clear the mAwtImageStrongRef here, because if the
                    // zooming factor changes, we may need to use it again
                }

                if (mPreScaledImage != null) {
                    gc.drawImage(mPreScaledImage, hi.translate(0), vi.translate(0));
                }
                return;
            }

            // we only anti-alias when reducing the image size.
            int oldAlias = -2;
            if (hi.getScale() < 1.0) {
                oldAlias = gc_setAntialias(gc, SWT.ON);
            }

            int srcX = 0;
            int srcY = 0;
            int srcWidth = hi.getImgSize();
            int srcHeight = vi.getImgSize();
            int destX = hi.translate(0);
            int destY = vi.translate(0);
            int destWidth = hi.getScaledImgSize();
            int destHeight = vi.getScaledImgSize();

            gc.drawImage(mImage,
                    srcX, srcY, srcWidth, srcHeight,
                    destX, destY, destWidth, destHeight);

            if (mShowDropShadow) {
                SwtUtils.drawRectangleShadow(gc, destX, destY, destWidth, destHeight);
            }

            if (oldAlias != -2) {
                gc_setAntialias(gc, oldAlias);
            }

            if (!valid) {
                gc_setAlpha(gc, 255); // opaque
            }
        }
    }

    /**
     * Sets the alpha for the given GC.
     * <p/>
     * Alpha may not work on all platforms and may fail with an exception, which
     * is hidden here (false is returned in that case).
     *
     * @param gc the GC to change
     * @param alpha the new alpha, 0 for transparent, 255 for opaque.
     * @return True if the operation worked, false if it failed with an
     *         exception.
     * @see GC#setAlpha(int)
     */
    private boolean gc_setAlpha(GC gc, int alpha) {
        try {
            gc.setAlpha(alpha);
            return true;
        } catch (SWTException e) {
            return false;
        }
    }

    /**
     * Sets the non-text antialias flag for the given GC.
     * <p/>
     * Antialias may not work on all platforms and may fail with an exception,
     * which is hidden here (-2 is returned in that case).
     *
     * @param gc the GC to change
     * @param alias One of {@link SWT#DEFAULT}, {@link SWT#ON}, {@link SWT#OFF}.
     * @return The previous aliasing mode if the operation worked, or -2 if it
     *         failed with an exception.
     * @see GC#setAntialias(int)
     */
    private int gc_setAntialias(GC gc, int alias) {
        try {
            int old = gc.getAntialias();
            gc.setAntialias(alias);
            return old;
        } catch (SWTException e) {
            return -2;
        }
    }

    /**
     * Custom {@link BufferedImage} class able to convert itself into an SWT {@link Image}
     * efficiently.
     *
     * The BufferedImage also contains an instance of {@link ImageData} that's kept around
     * and used to create new SWT {@link Image} objects in {@link #getSwtImage()}.
     *
     */
    private static final class SwtReadyBufferedImage extends BufferedImage {

        private final ImageData mImageData;
        private final Device mDevice;

        /**
         * Creates the image with a given model, raster and SWT {@link ImageData}
         * @param model the color model
         * @param raster the image raster
         * @param imageData the SWT image data.
         * @param device the {@link Device} in which the SWT image will be painted.
         */
        private SwtReadyBufferedImage(int width, int height, ImageData imageData, Device device) {
            super(width, height, BufferedImage.TYPE_INT_ARGB);
            mImageData = imageData;
            mDevice = device;
        }

        /**
         * Returns a new {@link Image} object initialized with the content of the BufferedImage.
         * @return the image object.
         */
        private Image getSwtImage() {
            // transfer the content of the bufferedImage into the image data.
            WritableRaster raster = getRaster();
            int[] imageDataBuffer = ((DataBufferInt) raster.getDataBuffer()).getData();

            mImageData.setPixels(0, 0, imageDataBuffer.length, imageDataBuffer, 0);

            return new Image(mDevice, mImageData);
        }

        /**
         * Creates a new {@link SwtReadyBufferedImage}.
         * @param w the width of the image
         * @param h the height of the image
         * @param device the device in which the SWT image will be painted
         * @return a new {@link SwtReadyBufferedImage} object
         */
        private static SwtReadyBufferedImage createImage(int w, int h, Device device) {
            // NOTE: We can't make this image bigger to accommodate the drop shadow directly
            // (such that we could paint one into the image after a layoutlib render)
            // since this image is in the full resolution of the device, and gets scaled
            // to fit in the layout editor. This would have the net effect of causing
            // the drop shadow to get zoomed/scaled along with the scene, making a tiny
            // drop shadow for tablet layouts, a huge drop shadow for tiny QVGA screens, etc.

            ImageData imageData = new ImageData(w, h, 32,
                    new PaletteData(0x00FF0000, 0x0000FF00, 0x000000FF));

            SwtReadyBufferedImage swtReadyImage = new SwtReadyBufferedImage(w, h,
                    imageData, device);

            return swtReadyImage;
        }
    }

    /**
     * Implementation of {@link IImageFactory#getImage(int, int)}.
     */
    @Override
    public BufferedImage getImage(int w, int h) {
        BufferedImage awtImage = mAwtImage.get();
        if (awtImage == null ||
                awtImage.getWidth() != w ||
                awtImage.getHeight() != h) {
            mAwtImage.clear();
            awtImage = SwtReadyBufferedImage.createImage(w, h, getDevice());
            mAwtImage = new SoftReference<BufferedImage>(awtImage);
            if (PRESCALE) {
                mAwtImageStrongRef = awtImage;
            }
        }

        return awtImage;
    }

    /**
     * Returns the bounds of the current image, or null
     *
     * @return the bounds of the current image, or null
     */
    public Rect getImageBounds() {
        if (mImage == null) {
            return null;
        }

        return new Rect(0, 0, mImage.getImageData().width, mImage.getImageData().height);
    }
}

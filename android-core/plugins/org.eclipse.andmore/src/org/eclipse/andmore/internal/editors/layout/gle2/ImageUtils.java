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

import static com.android.SdkConstants.DOT_9PNG;
import static com.android.SdkConstants.DOT_BMP;
import static com.android.SdkConstants.DOT_GIF;
import static com.android.SdkConstants.DOT_JPG;
import static com.android.SdkConstants.DOT_PNG;
import static com.android.utils.SdkUtils.endsWithIgnoreCase;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.KEY_INTERPOLATION;
import static java.awt.RenderingHints.KEY_RENDERING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR;
import static java.awt.RenderingHints.VALUE_RENDER_QUALITY;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.api.Rect;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;

/**
 * Utilities related to image processing.
 */
public class ImageUtils {
    /**
     * Returns true if the given image has no dark pixels
     *
     * @param image the image to be checked for dark pixels
     * @return true if no dark pixels were found
     */
    public static boolean containsDarkPixels(BufferedImage image) {
        for (int y = 0, height = image.getHeight(); y < height; y++) {
            for (int x = 0, width = image.getWidth(); x < width; x++) {
                int pixel = image.getRGB(x, y);
                if ((pixel & 0xFF000000) != 0) {
                    int r = (pixel & 0xFF0000) >> 16;
                    int g = (pixel & 0x00FF00) >> 8;
                    int b = (pixel & 0x0000FF);

                    // One perceived luminance formula is (0.299*red + 0.587*green + 0.114*blue)
                    // In order to keep this fast since we don't need a very accurate
                    // measure, I'll just estimate this with integer math:
                    long brightness = (299L*r + 587*g + 114*b) / 1000;
                    if (brightness < 128) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Returns the perceived brightness of the given RGB integer on a scale from 0 to 255
     *
     * @param rgb the RGB triplet, 8 bits each
     * @return the perceived brightness, with 0 maximally dark and 255 maximally bright
     */
    public static int getBrightness(int rgb) {
        if ((rgb & 0xFFFFFF) != 0) {
            int r = (rgb & 0xFF0000) >> 16;
            int g = (rgb & 0x00FF00) >> 8;
            int b = (rgb & 0x0000FF);
            // See the containsDarkPixels implementation for details
            return (int) ((299L*r + 587*g + 114*b) / 1000);
        }

        return 0;
    }

    /**
     * Converts an alpha-red-green-blue integer color into an {@link RGB} color.
     * <p>
     * <b>NOTE</b> - this will drop the alpha value since {@link RGB} objects do not
     * contain transparency information.
     *
     * @param rgb the RGB integer to convert to a color description
     * @return the color description corresponding to the integer
     */
    public static RGB intToRgb(int rgb) {
        return new RGB((rgb & 0xFF0000) >>> 16, (rgb & 0xFF00) >>> 8, rgb & 0xFF);
    }

    /**
     * Converts an {@link RGB} color into a alpha-red-green-blue integer
     *
     * @param rgb the RGB color descriptor to convert
     * @param alpha the amount of alpha to add into the color integer (since the
     *            {@link RGB} objects do not contain an alpha channel)
     * @return an integer corresponding to the {@link RGB} color
     */
    public static int rgbToInt(RGB rgb, int alpha) {
        return alpha << 24 | (rgb.red << 16) | (rgb.green << 8) | rgb.blue;
    }

    /**
     * Crops blank pixels from the edges of the image and returns the cropped result. We
     * crop off pixels that are blank (meaning they have an alpha value = 0). Note that
     * this is not the same as pixels that aren't opaque (an alpha value other than 255).
     *
     * @param image the image to be cropped
     * @param initialCrop If not null, specifies a rectangle which contains an initial
     *            crop to continue. This can be used to crop an image where you already
     *            know about margins in the image
     * @return a cropped version of the source image, or null if the whole image was blank
     *         and cropping completely removed everything
     */
    @Nullable
    public static BufferedImage cropBlank(
            @NonNull BufferedImage image,
            @Nullable Rect initialCrop) {
        return cropBlank(image, initialCrop, image.getType());
    }

    /**
     * Crops blank pixels from the edges of the image and returns the cropped result. We
     * crop off pixels that are blank (meaning they have an alpha value = 0). Note that
     * this is not the same as pixels that aren't opaque (an alpha value other than 255).
     *
     * @param image the image to be cropped
     * @param initialCrop If not null, specifies a rectangle which contains an initial
     *            crop to continue. This can be used to crop an image where you already
     *            know about margins in the image
     * @param imageType the type of {@link BufferedImage} to create
     * @return a cropped version of the source image, or null if the whole image was blank
     *         and cropping completely removed everything
     */
    public static BufferedImage cropBlank(BufferedImage image, Rect initialCrop, int imageType) {
        CropFilter filter = new CropFilter() {
            @Override
            public boolean crop(BufferedImage bufferedImage, int x, int y) {
                int rgb = bufferedImage.getRGB(x, y);
                return (rgb & 0xFF000000) == 0x00000000;
                // TODO: Do a threshold of 80 instead of just 0? Might give better
                // visual results -- e.g. check <= 0x80000000
            }
        };
        return crop(image, filter, initialCrop, imageType);
    }

    /**
     * Crops pixels of a given color from the edges of the image and returns the cropped
     * result.
     *
     * @param image the image to be cropped
     * @param blankArgb the color considered to be blank, as a 32 pixel integer with 8
     *            bits of alpha, red, green and blue
     * @param initialCrop If not null, specifies a rectangle which contains an initial
     *            crop to continue. This can be used to crop an image where you already
     *            know about margins in the image
     * @return a cropped version of the source image, or null if the whole image was blank
     *         and cropping completely removed everything
     */
    @Nullable
    public static BufferedImage cropColor(
            @NonNull BufferedImage image,
            final int blankArgb,
            @Nullable Rect initialCrop) {
        return cropColor(image, blankArgb, initialCrop, image.getType());
    }

    /**
     * Crops pixels of a given color from the edges of the image and returns the cropped
     * result.
     *
     * @param image the image to be cropped
     * @param blankArgb the color considered to be blank, as a 32 pixel integer with 8
     *            bits of alpha, red, green and blue
     * @param initialCrop If not null, specifies a rectangle which contains an initial
     *            crop to continue. This can be used to crop an image where you already
     *            know about margins in the image
     * @param imageType the type of {@link BufferedImage} to create
     * @return a cropped version of the source image, or null if the whole image was blank
     *         and cropping completely removed everything
     */
    public static BufferedImage cropColor(BufferedImage image,
            final int blankArgb, Rect initialCrop, int imageType) {
        CropFilter filter = new CropFilter() {
            @Override
            public boolean crop(BufferedImage bufferedImage, int x, int y) {
                return blankArgb == bufferedImage.getRGB(x, y);
            }
        };
        return crop(image, filter, initialCrop, imageType);
    }

    /**
     * Interface implemented by cropping functions that determine whether
     * a pixel should be cropped or not.
     */
    private static interface CropFilter {
        /**
         * Returns true if the pixel is should be cropped.
         *
         * @param image the image containing the pixel in question
         * @param x the x position of the pixel
         * @param y the y position of the pixel
         * @return true if the pixel should be cropped (for example, is blank)
         */
        boolean crop(BufferedImage image, int x, int y);
    }

    private static BufferedImage crop(BufferedImage image, CropFilter filter, Rect initialCrop,
            int imageType) {
        if (image == null) {
            return null;
        }

        // First, determine the dimensions of the real image within the image
        int x1, y1, x2, y2;
        if (initialCrop != null) {
            x1 = initialCrop.x;
            y1 = initialCrop.y;
            x2 = initialCrop.x + initialCrop.w;
            y2 = initialCrop.y + initialCrop.h;
        } else {
            x1 = 0;
            y1 = 0;
            x2 = image.getWidth();
            y2 = image.getHeight();
        }

        // Nothing left to crop
        if (x1 == x2 || y1 == y2) {
            return null;
        }

        // This algorithm is a bit dumb -- it just scans along the edges looking for
        // a pixel that shouldn't be cropped. I could maybe try to make it smarter by
        // for example doing a binary search to quickly eliminate large empty areas to
        // the right and bottom -- but this is slightly tricky with components like the
        // AnalogClock where I could accidentally end up finding a blank horizontal or
        // vertical line somewhere in the middle of the rendering of the clock, so for now
        // we do the dumb thing -- not a big deal since we tend to crop reasonably
        // small images.

        // First determine top edge
        topEdge: for (; y1 < y2; y1++) {
            for (int x = x1; x < x2; x++) {
                if (!filter.crop(image, x, y1)) {
                    break topEdge;
                }
            }
        }

        if (y1 == image.getHeight()) {
            // The image is blank
            return null;
        }

        // Next determine left edge
        leftEdge: for (; x1 < x2; x1++) {
            for (int y = y1; y < y2; y++) {
                if (!filter.crop(image, x1, y)) {
                    break leftEdge;
                }
            }
        }

        // Next determine right edge
        rightEdge: for (; x2 > x1; x2--) {
            for (int y = y1; y < y2; y++) {
                if (!filter.crop(image, x2 - 1, y)) {
                    break rightEdge;
                }
            }
        }

        // Finally determine bottom edge
        bottomEdge: for (; y2 > y1; y2--) {
            for (int x = x1; x < x2; x++) {
                if (!filter.crop(image, x, y2 - 1)) {
                    break bottomEdge;
                }
            }
        }

        // No need to crop?
        if (x1 == 0 && y1 == 0 && x2 == image.getWidth() && y2 == image.getHeight()) {
            return image;
        }

        if (x1 == x2 || y1 == y2) {
            // Nothing left after crop -- blank image
            return null;
        }

        int width = x2 - x1;
        int height = y2 - y1;

        // Now extract the sub-image
        if (imageType == -1) {
            imageType = image.getType();
        }
        if (imageType == BufferedImage.TYPE_CUSTOM) {
            imageType = BufferedImage.TYPE_INT_ARGB;
        }
        BufferedImage cropped = new BufferedImage(width, height, imageType);
        Graphics g = cropped.getGraphics();
        g.drawImage(image, 0, 0, width, height, x1, y1, x2, y2, null);

        g.dispose();

        return cropped;
    }

    /**
     * Creates a drop shadow of a given image and returns a new image which shows the
     * input image on top of its drop shadow.
     * <p>
     * <b>NOTE: If the shape is rectangular and opaque, consider using
     * {@link #drawRectangleShadow(Graphics, int, int, int, int)} instead.</b>
     *
     * @param source the source image to be shadowed
     * @param shadowSize the size of the shadow in pixels
     * @param shadowOpacity the opacity of the shadow, with 0=transparent and 1=opaque
     * @param shadowRgb the RGB int to use for the shadow color
     * @return a new image with the source image on top of its shadow
     */
    public static BufferedImage createDropShadow(BufferedImage source, int shadowSize,
            float shadowOpacity, int shadowRgb) {

        // This code is based on
        //      http://www.jroller.com/gfx/entry/non_rectangular_shadow

        BufferedImage image = new BufferedImage(source.getWidth() + shadowSize * 2,
                source.getHeight() + shadowSize * 2,
                BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2 = image.createGraphics();
        g2.drawImage(source, null, shadowSize, shadowSize);

        int dstWidth = image.getWidth();
        int dstHeight = image.getHeight();

        int left = (shadowSize - 1) >> 1;
        int right = shadowSize - left;
        int xStart = left;
        int xStop = dstWidth - right;
        int yStart = left;
        int yStop = dstHeight - right;

        shadowRgb = shadowRgb & 0x00FFFFFF;

        int[] aHistory = new int[shadowSize];
        int historyIdx = 0;

        int aSum;

        int[] dataBuffer = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        int lastPixelOffset = right * dstWidth;
        float sumDivider = shadowOpacity / shadowSize;

        // horizontal pass
        for (int y = 0, bufferOffset = 0; y < dstHeight; y++, bufferOffset = y * dstWidth) {
            aSum = 0;
            historyIdx = 0;
            for (int x = 0; x < shadowSize; x++, bufferOffset++) {
                int a = dataBuffer[bufferOffset] >>> 24;
                aHistory[x] = a;
                aSum += a;
            }

            bufferOffset -= right;

            for (int x = xStart; x < xStop; x++, bufferOffset++) {
                int a = (int) (aSum * sumDivider);
                dataBuffer[bufferOffset] = a << 24 | shadowRgb;

                // subtract the oldest pixel from the sum
                aSum -= aHistory[historyIdx];

                // get the latest pixel
                a = dataBuffer[bufferOffset + right] >>> 24;
                aHistory[historyIdx] = a;
                aSum += a;

                if (++historyIdx >= shadowSize) {
                    historyIdx -= shadowSize;
                }
            }
        }
        // vertical pass
        for (int x = 0, bufferOffset = 0; x < dstWidth; x++, bufferOffset = x) {
            aSum = 0;
            historyIdx = 0;
            for (int y = 0; y < shadowSize; y++, bufferOffset += dstWidth) {
                int a = dataBuffer[bufferOffset] >>> 24;
                aHistory[y] = a;
                aSum += a;
            }

            bufferOffset -= lastPixelOffset;

            for (int y = yStart; y < yStop; y++, bufferOffset += dstWidth) {
                int a = (int) (aSum * sumDivider);
                dataBuffer[bufferOffset] = a << 24 | shadowRgb;

                // subtract the oldest pixel from the sum
                aSum -= aHistory[historyIdx];

                // get the latest pixel
                a = dataBuffer[bufferOffset + lastPixelOffset] >>> 24;
                aHistory[historyIdx] = a;
                aSum += a;

                if (++historyIdx >= shadowSize) {
                    historyIdx -= shadowSize;
                }
            }
        }

        g2.drawImage(source, null, 0, 0);
        g2.dispose();

        return image;
    }

    /**
     * Draws a rectangular drop shadow (of size {@link #SHADOW_SIZE} by
     * {@link #SHADOW_SIZE} around the given source and returns a new image with
     * both combined
     *
     * @param source the source image
     * @return the source image with a drop shadow on the bottom and right
     */
    public static BufferedImage createRectangularDropShadow(BufferedImage source) {
        int type = source.getType();
        if (type == BufferedImage.TYPE_CUSTOM) {
            type = BufferedImage.TYPE_INT_ARGB;
        }

        int width = source.getWidth();
        int height = source.getHeight();
        BufferedImage image = new BufferedImage(width + SHADOW_SIZE, height + SHADOW_SIZE, type);
        Graphics g = image.getGraphics();
        g.drawImage(source, 0, 0, width, height, null);
        ImageUtils.drawRectangleShadow(image, 0, 0, width, height);
        g.dispose();

        return image;
    }

    /**
     * Draws a drop shadow for the given rectangle into the given context. It
     * will not draw anything if the rectangle is smaller than a minimum
     * determined by the assets used to draw the shadow graphics.
     * The size of the shadow is {@link #SHADOW_SIZE}.
     *
     * @param image the image to draw the shadow into
     * @param x the left coordinate of the left hand side of the rectangle
     * @param y the top coordinate of the top of the rectangle
     * @param width the width of the rectangle
     * @param height the height of the rectangle
     */
    public static final void drawRectangleShadow(BufferedImage image,
            int x, int y, int width, int height) {
        Graphics gc = image.getGraphics();
        try {
            drawRectangleShadow(gc, x, y, width, height);
        } finally {
            gc.dispose();
        }
    }

    /**
     * Draws a small drop shadow for the given rectangle into the given context. It
     * will not draw anything if the rectangle is smaller than a minimum
     * determined by the assets used to draw the shadow graphics.
     * The size of the shadow is {@link #SMALL_SHADOW_SIZE}.
     *
     * @param image the image to draw the shadow into
     * @param x the left coordinate of the left hand side of the rectangle
     * @param y the top coordinate of the top of the rectangle
     * @param width the width of the rectangle
     * @param height the height of the rectangle
     */
    public static final void drawSmallRectangleShadow(BufferedImage image,
            int x, int y, int width, int height) {
        Graphics gc = image.getGraphics();
        try {
            drawSmallRectangleShadow(gc, x, y, width, height);
        } finally {
            gc.dispose();
        }
    }

    /**
     * The width and height of the drop shadow painted by
     * {@link #drawRectangleShadow(Graphics, int, int, int, int)}
     */
    public static final int SHADOW_SIZE = 20; // DO NOT EDIT. This corresponds to bitmap graphics

    /**
     * The width and height of the drop shadow painted by
     * {@link #drawSmallRectangleShadow(Graphics, int, int, int, int)}
     */
    public static final int SMALL_SHADOW_SIZE = 10; // DO NOT EDIT. Corresponds to bitmap graphics

    /**
     * Draws a drop shadow for the given rectangle into the given context. It
     * will not draw anything if the rectangle is smaller than a minimum
     * determined by the assets used to draw the shadow graphics.
     * <p>
     * This corresponds to
     * {@link SwtUtils#drawRectangleShadow(org.eclipse.swt.graphics.GC, int, int, int, int)},
     * but applied to an AWT graphics object instead, such that no image
     * conversion has to be performed.
     * <p>
     * Make sure to keep changes in the visual appearance here in sync with the
     * AWT version in
     * {@link SwtUtils#drawRectangleShadow(org.eclipse.swt.graphics.GC, int, int, int, int)}.
     *
     * @param gc the graphics context to draw into
     * @param x the left coordinate of the left hand side of the rectangle
     * @param y the top coordinate of the top of the rectangle
     * @param width the width of the rectangle
     * @param height the height of the rectangle
     */
    public static final void drawRectangleShadow(Graphics gc,
            int x, int y, int width, int height) {
        if (sShadowBottomLeft == null) {
            // Shadow graphics. This was generated by creating a drop shadow in
            // Gimp, using the parameters x offset=10, y offset=10, blur radius=10,
            // color=black, and opacity=51. These values attempt to make a shadow
            // that is legible both for dark and light themes, on top of the
            // canvas background (rgb(150,150,150). Darker shadows would tend to
            // blend into the foreground for a dark holo screen, and lighter shadows
            // would be hard to spot on the canvas background. If you make adjustments,
            // make sure to check the shadow with both dark and light themes.
            //
            // After making the graphics, I cut out the top right, bottom left
            // and bottom right corners as 20x20 images, and these are reproduced by
            // painting them in the corresponding places in the target graphics context.
            // I then grabbed a single horizontal gradient line from the middle of the
            // right edge,and a single vertical gradient line from the bottom. These
            // are then painted scaled/stretched in the target to fill the gaps between
            // the three corner images.
            //
            // Filenames: bl=bottom left, b=bottom, br=bottom right, r=right, tr=top right
            sShadowBottomLeft  = readImage("shadow-bl.png"); //$NON-NLS-1$
            sShadowBottom      = readImage("shadow-b.png");  //$NON-NLS-1$
            sShadowBottomRight = readImage("shadow-br.png"); //$NON-NLS-1$
            sShadowRight       = readImage("shadow-r.png");  //$NON-NLS-1$
            sShadowTopRight    = readImage("shadow-tr.png"); //$NON-NLS-1$
            assert sShadowBottomLeft != null;
            assert sShadowBottomRight.getWidth() == SHADOW_SIZE;
            assert sShadowBottomRight.getHeight() == SHADOW_SIZE;
        }

        int blWidth = sShadowBottomLeft.getWidth();
        int trHeight = sShadowTopRight.getHeight();
        if (width < blWidth) {
            return;
        }
        if (height < trHeight) {
            return;
        }

        gc.drawImage(sShadowBottomLeft, x, y + height, null);
        gc.drawImage(sShadowBottomRight, x + width, y + height, null);
        gc.drawImage(sShadowTopRight, x + width, y, null);
        gc.drawImage(sShadowBottom,
                x + sShadowBottomLeft.getWidth(), y + height,
                x + width, y + height + sShadowBottom.getHeight(),
                0, 0, sShadowBottom.getWidth(), sShadowBottom.getHeight(),
                null);
        gc.drawImage(sShadowRight,
                x + width, y + sShadowTopRight.getHeight(),
                x + width + sShadowRight.getWidth(), y + height,
                0, 0, sShadowRight.getWidth(), sShadowRight.getHeight(),
                null);
    }

    /**
     * Draws a small drop shadow for the given rectangle into the given context. It
     * will not draw anything if the rectangle is smaller than a minimum
     * determined by the assets used to draw the shadow graphics.
     * <p>
     *
     * @param gc the graphics context to draw into
     * @param x the left coordinate of the left hand side of the rectangle
     * @param y the top coordinate of the top of the rectangle
     * @param width the width of the rectangle
     * @param height the height of the rectangle
     */
    public static final void drawSmallRectangleShadow(Graphics gc,
            int x, int y, int width, int height) {
        if (sShadow2BottomLeft == null) {
            // Shadow graphics. This was generated by creating a drop shadow in
            // Gimp, using the parameters x offset=5, y offset=%, blur radius=5,
            // color=black, and opacity=51. These values attempt to make a shadow
            // that is legible both for dark and light themes, on top of the
            // canvas background (rgb(150,150,150). Darker shadows would tend to
            // blend into the foreground for a dark holo screen, and lighter shadows
            // would be hard to spot on the canvas background. If you make adjustments,
            // make sure to check the shadow with both dark and light themes.
            //
            // After making the graphics, I cut out the top right, bottom left
            // and bottom right corners as 20x20 images, and these are reproduced by
            // painting them in the corresponding places in the target graphics context.
            // I then grabbed a single horizontal gradient line from the middle of the
            // right edge,and a single vertical gradient line from the bottom. These
            // are then painted scaled/stretched in the target to fill the gaps between
            // the three corner images.
            //
            // Filenames: bl=bottom left, b=bottom, br=bottom right, r=right, tr=top right
            sShadow2BottomLeft  = readImage("shadow2-bl.png"); //$NON-NLS-1$
            sShadow2Bottom      = readImage("shadow2-b.png");  //$NON-NLS-1$
            sShadow2BottomRight = readImage("shadow2-br.png"); //$NON-NLS-1$
            sShadow2Right       = readImage("shadow2-r.png");  //$NON-NLS-1$
            sShadow2TopRight    = readImage("shadow2-tr.png"); //$NON-NLS-1$
            assert sShadow2BottomLeft != null;
            assert sShadow2TopRight != null;
            assert sShadow2BottomRight.getWidth() == SMALL_SHADOW_SIZE;
            assert sShadow2BottomRight.getHeight() == SMALL_SHADOW_SIZE;
        }

        int blWidth = sShadow2BottomLeft.getWidth();
        int trHeight = sShadow2TopRight.getHeight();
        if (width < blWidth) {
            return;
        }
        if (height < trHeight) {
            return;
        }

        gc.drawImage(sShadow2BottomLeft, x, y + height, null);
        gc.drawImage(sShadow2BottomRight, x + width, y + height, null);
        gc.drawImage(sShadow2TopRight, x + width, y, null);
        gc.drawImage(sShadow2Bottom,
                x + sShadow2BottomLeft.getWidth(), y + height,
                x + width, y + height + sShadow2Bottom.getHeight(),
                0, 0, sShadow2Bottom.getWidth(), sShadow2Bottom.getHeight(),
                null);
        gc.drawImage(sShadow2Right,
                x + width, y + sShadow2TopRight.getHeight(),
                x + width + sShadow2Right.getWidth(), y + height,
                0, 0, sShadow2Right.getWidth(), sShadow2Right.getHeight(),
                null);
    }

    /**
     * Reads the given image from the plugin folder
     *
     * @param name the name of the image (including file extension)
     * @return the corresponding image, or null if something goes wrong
     */
    @Nullable
    public static BufferedImage readImage(@NonNull String name) {
        InputStream stream = ImageUtils.class.getResourceAsStream("/icons/" + name); //$NON-NLS-1$
        if (stream != null) {
            try {
                return ImageIO.read(stream);
            } catch (IOException e) {
                AdtPlugin.log(e, "Could not read %1$s", name);
            } finally {
                try {
                    stream.close();
                } catch (IOException e) {
                    // Dumb API
                }
            }
        }

        return null;
    }

    // Normal drop shadow
    private static BufferedImage sShadowBottomLeft;
    private static BufferedImage sShadowBottom;
    private static BufferedImage sShadowBottomRight;
    private static BufferedImage sShadowRight;
    private static BufferedImage sShadowTopRight;

    // Small drop shadow
    private static BufferedImage sShadow2BottomLeft;
    private static BufferedImage sShadow2Bottom;
    private static BufferedImage sShadow2BottomRight;
    private static BufferedImage sShadow2Right;
    private static BufferedImage sShadow2TopRight;

    /**
     * Returns a bounding rectangle for the given list of rectangles. If the list is
     * empty, the bounding rectangle is null.
     *
     * @param items the list of rectangles to compute a bounding rectangle for (may not be
     *            null)
     * @return a bounding rectangle of the passed in rectangles, or null if the list is
     *         empty
     */
    public static Rectangle getBoundingRectangle(List<Rectangle> items) {
        Iterator<Rectangle> iterator = items.iterator();
        if (!iterator.hasNext()) {
            return null;
        }

        Rectangle bounds = iterator.next();
        Rectangle union = new Rectangle(bounds.x, bounds.y, bounds.width, bounds.height);
        while (iterator.hasNext()) {
            union.add(iterator.next());
        }

        return union;
    }

    /**
     * Returns a new image which contains of the sub image given by the rectangle (x1,y1)
     * to (x2,y2)
     *
     * @param source the source image
     * @param x1 top left X coordinate
     * @param y1 top left Y coordinate
     * @param x2 bottom right X coordinate
     * @param y2 bottom right Y coordinate
     * @return a new image containing the pixels in the given range
     */
    public static BufferedImage subImage(BufferedImage source, int x1, int y1, int x2, int y2) {
        int width = x2 - x1;
        int height = y2 - y1;
        int imageType = source.getType();
        if (imageType == BufferedImage.TYPE_CUSTOM) {
            imageType = BufferedImage.TYPE_INT_ARGB;
        }
        BufferedImage sub = new BufferedImage(width, height, imageType);
        Graphics g = sub.getGraphics();
        g.drawImage(source, 0, 0, width, height, x1, y1, x2, y2, null);
        g.dispose();

        return sub;
    }

    /**
     * Returns the color value represented by the given string value
     * @param value the color value
     * @return the color as an int
     * @throw NumberFormatException if the conversion failed.
     */
    public static int getColor(String value) {
        // Copied from ResourceHelper in layoutlib
        if (value != null) {
            if (value.startsWith("#") == false) { //$NON-NLS-1$
                throw new NumberFormatException(
                        String.format("Color value '%s' must start with #", value));
            }

            value = value.substring(1);

            // make sure it's not longer than 32bit
            if (value.length() > 8) {
                throw new NumberFormatException(String.format(
                        "Color value '%s' is too long. Format is either" +
                        "#AARRGGBB, #RRGGBB, #RGB, or #ARGB",
                        value));
            }

            if (value.length() == 3) { // RGB format
                char[] color = new char[8];
                color[0] = color[1] = 'F';
                color[2] = color[3] = value.charAt(0);
                color[4] = color[5] = value.charAt(1);
                color[6] = color[7] = value.charAt(2);
                value = new String(color);
            } else if (value.length() == 4) { // ARGB format
                char[] color = new char[8];
                color[0] = color[1] = value.charAt(0);
                color[2] = color[3] = value.charAt(1);
                color[4] = color[5] = value.charAt(2);
                color[6] = color[7] = value.charAt(3);
                value = new String(color);
            } else if (value.length() == 6) {
                value = "FF" + value; //$NON-NLS-1$
            }

            // this is a RRGGBB or AARRGGBB value

            // Integer.parseInt will fail to parse strings like "ff191919", so we use
            // a Long, but cast the result back into an int, since we know that we're only
            // dealing with 32 bit values.
            return (int)Long.parseLong(value, 16);
        }

        throw new NumberFormatException();
    }

    /**
     * Resize the given image
     *
     * @param source the image to be scaled
     * @param xScale x scale
     * @param yScale y scale
     * @return the scaled image
     */
    public static BufferedImage scale(BufferedImage source, double xScale, double yScale) {
       return scale(source, xScale, yScale, 0, 0);
    }

    /**
     * Resize the given image
     *
     * @param source the image to be scaled
     * @param xScale x scale
     * @param yScale y scale
     * @param rightMargin extra margin to add on the right
     * @param bottomMargin extra margin to add on the bottom
     * @return the scaled image
     */
    public static BufferedImage scale(BufferedImage source, double xScale, double yScale,
            int rightMargin, int bottomMargin) {
        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();
        int destWidth = Math.max(1, (int) (xScale * sourceWidth));
        int destHeight = Math.max(1, (int) (yScale * sourceHeight));
        int imageType = source.getType();
        if (imageType == BufferedImage.TYPE_CUSTOM) {
            imageType = BufferedImage.TYPE_INT_ARGB;
        }
        if (xScale > 0.5 && yScale > 0.5) {
            BufferedImage scaled =
                    new BufferedImage(destWidth + rightMargin, destHeight + bottomMargin, imageType);
            Graphics2D g2 = scaled.createGraphics();
            g2.setComposite(AlphaComposite.Src);
            g2.setColor(new Color(0, true));
            g2.fillRect(0, 0, destWidth + rightMargin, destHeight + bottomMargin);
            g2.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(KEY_RENDERING, VALUE_RENDER_QUALITY);
            g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
            g2.drawImage(source, 0, 0, destWidth, destHeight, 0, 0, sourceWidth, sourceHeight,
                    null);
            g2.dispose();
            return scaled;
        } else {
            // When creating a thumbnail, using the above code doesn't work very well;
            // you get some visible artifacts, especially for text. Instead use the
            // technique of repeatedly scaling the image into half; this will cause
            // proper averaging of neighboring pixels, and will typically (for the kinds
            // of screen sizes used by this utility method in the layout editor) take
            // about 3-4 iterations to get the result since we are logarithmically reducing
            // the size. Besides, each successive pass in operating on much fewer pixels
            // (a reduction of 4 in each pass).
            //
            // However, we may not be resizing to a size that can be reached exactly by
            // successively diving in half. Therefore, once we're within a factor of 2 of
            // the final size, we can do a resize to the exact target size.
            // However, we can get even better results if we perform this final resize
            // up front. Let's say we're going from width 1000 to a destination width of 85.
            // The first approach would cause a resize from 1000 to 500 to 250 to 125, and
            // then a resize from 125 to 85. That last resize can distort/blur a lot.
            // Instead, we can start with the destination width, 85, and double it
            // successfully until we're close to the initial size: 85, then 170,
            // then 340, and finally 680. (The next one, 1360, is larger than 1000).
            // So, now we *start* the thumbnail operation by resizing from width 1000 to
            // width 680, which will preserve a lot of visual details such as text.
            // Then we can successively resize the image in half, 680 to 340 to 170 to 85.
            // We end up with the expected final size, but we've been doing an exact
            // divide-in-half resizing operation at the end so there is less distortion.


            int iterations = 0; // Number of halving operations to perform after the initial resize
            int nearestWidth = destWidth; // Width closest to source width that = 2^x, x is integer
            int nearestHeight = destHeight;
            while (nearestWidth < sourceWidth / 2) {
                nearestWidth *= 2;
                nearestHeight *= 2;
                iterations++;
            }

            // If we're supposed to add in margins, we need to do it in the initial resizing
            // operation if we don't have any subsequent resizing operations.
            if (iterations == 0) {
                nearestWidth += rightMargin;
                nearestHeight += bottomMargin;
            }

            BufferedImage scaled = new BufferedImage(nearestWidth, nearestHeight, imageType);
            Graphics2D g2 = scaled.createGraphics();
            g2.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(KEY_RENDERING, VALUE_RENDER_QUALITY);
            g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
            g2.drawImage(source, 0, 0, nearestWidth, nearestHeight,
                    0, 0, sourceWidth, sourceHeight, null);
            g2.dispose();

            sourceWidth = nearestWidth;
            sourceHeight = nearestHeight;
            source = scaled;

            for (int iteration = iterations - 1; iteration >= 0; iteration--) {
                int halfWidth = sourceWidth / 2;
                int halfHeight = sourceHeight / 2;
                if (iteration == 0) { // Last iteration: Add margins in final image
                    scaled = new BufferedImage(halfWidth + rightMargin, halfHeight + bottomMargin,
                            imageType);
                } else {
                    scaled = new BufferedImage(halfWidth, halfHeight, imageType);
                }
                g2 = scaled.createGraphics();
                g2.setRenderingHint(KEY_INTERPOLATION,VALUE_INTERPOLATION_BILINEAR);
                g2.setRenderingHint(KEY_RENDERING, VALUE_RENDER_QUALITY);
                g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
                g2.drawImage(source, 0, 0,
                        halfWidth, halfHeight, 0, 0,
                        sourceWidth, sourceHeight,
                        null);
                g2.dispose();

                sourceWidth = halfWidth;
                sourceHeight = halfHeight;
                source = scaled;
                iterations--;
            }
            return scaled;
        }
    }

    /**
     * Returns true if the given file path points to an image file recognized by
     * Android. See http://developer.android.com/guide/appendix/media-formats.html
     * for details.
     *
     * @param path the filename to be tested
     * @return true if the file represents an image file
     */
    public static boolean hasImageExtension(String path) {
        return endsWithIgnoreCase(path, DOT_PNG)
            || endsWithIgnoreCase(path, DOT_9PNG)
            || endsWithIgnoreCase(path, DOT_GIF)
            || endsWithIgnoreCase(path, DOT_JPG)
            || endsWithIgnoreCase(path, DOT_BMP);
    }

    /**
     * Creates a new image of the given size filled with the given color
     *
     * @param width the width of the image
     * @param height the height of the image
     * @param color the color of the image
     * @return a new image of the given size filled with the given color
     */
    public static BufferedImage createColoredImage(int width, int height, RGB color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics g = image.getGraphics();
        g.setColor(new Color(color.red, color.green, color.blue));
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.dispose();
        return image;
    }
}

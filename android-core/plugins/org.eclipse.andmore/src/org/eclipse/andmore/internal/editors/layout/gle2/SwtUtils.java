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

import com.android.ide.common.api.Rect;

import org.eclipse.andmore.internal.editors.IconFactory;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.WritableRaster;
import java.util.List;

/**
 * Various generic SWT utilities such as image conversion.
 */
public class SwtUtils {

    private SwtUtils() {
    }

    /**
     * Returns the {@link PaletteData} describing the ARGB ordering expected from integers
     * representing pixels for AWT {@link BufferedImage}.
     *
     * @param imageType the {@link BufferedImage#getType()} type
     * @return A new {@link PaletteData} suitable for AWT images.
     */
    public static PaletteData getAwtPaletteData(int imageType) {
        switch (imageType) {
            case BufferedImage.TYPE_INT_RGB:
            case BufferedImage.TYPE_INT_ARGB:
            case BufferedImage.TYPE_INT_ARGB_PRE:
                return new PaletteData(0x00FF0000, 0x0000FF00, 0x000000FF);

            case BufferedImage.TYPE_3BYTE_BGR:
            case BufferedImage.TYPE_4BYTE_ABGR:
            case BufferedImage.TYPE_4BYTE_ABGR_PRE:
                return new PaletteData(0x000000FF, 0x0000FF00, 0x00FF0000);

            default:
                throw new UnsupportedOperationException("RGB type not supported yet.");
        }
    }

    /**
     * Returns true if the given type of {@link BufferedImage} is supported for
     * conversion. For unsupported formats, use
     * {@link #convertToCompatibleFormat(BufferedImage)} first.
     *
     * @param imageType the {@link BufferedImage#getType()}
     * @return true if we can convert the given buffered image format
     */
    private static boolean isSupportedPaletteType(int imageType) {
        switch (imageType) {
            case BufferedImage.TYPE_INT_RGB:
            case BufferedImage.TYPE_INT_ARGB:
            case BufferedImage.TYPE_INT_ARGB_PRE:
            case BufferedImage.TYPE_3BYTE_BGR:
            case BufferedImage.TYPE_4BYTE_ABGR:
            case BufferedImage.TYPE_4BYTE_ABGR_PRE:
                return true;
            default:
                return false;
        }
    }

    /** Converts the given arbitrary {@link BufferedImage} to another {@link BufferedImage}
     * in a format that is supported (see {@link #isSupportedPaletteType(int)})
     *
     * @param image the image to be converted
     * @return a new image that is in a guaranteed compatible format
     */
    private static BufferedImage convertToCompatibleFormat(BufferedImage image) {
        BufferedImage converted = new BufferedImage(image.getWidth(), image.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        Graphics graphics = converted.getGraphics();
        graphics.drawImage(image, 0, 0, null);
        graphics.dispose();

        return converted;
    }

    /**
     * Converts an AWT {@link BufferedImage} into an equivalent SWT {@link Image}. Whether
     * the transparency data is transferred is optional, and this method can also apply an
     * alpha adjustment during the conversion.
     * <p/>
     * Implementation details: on Windows, the returned {@link Image} will have an ordering
     * matching the Windows DIB (e.g. RGBA, not ARGB). Callers must make sure to use
     * <code>Image.getImageData().paletteData</code> to get the right pixels out of the image.
     *
     * @param display The display where the SWT image will be shown
     * @param awtImage The AWT {@link BufferedImage}
     * @param transferAlpha If true, copy alpha data out of the source image
     * @param globalAlpha If -1, do nothing, otherwise adjust the alpha of the final image
     *            by the given amount in the range [0,255]
     * @return A new SWT {@link Image} with the same contents as the source
     *         {@link BufferedImage}
     */
    public static Image convertToSwt(Device display, BufferedImage awtImage,
            boolean transferAlpha, int globalAlpha) {
        if (!isSupportedPaletteType(awtImage.getType())) {
            awtImage = convertToCompatibleFormat(awtImage);
        }

        int width = awtImage.getWidth();
        int height = awtImage.getHeight();

        WritableRaster raster = awtImage.getRaster();
        DataBuffer dataBuffer = raster.getDataBuffer();
        ImageData imageData =
            new ImageData(width, height, 32, getAwtPaletteData(awtImage.getType()));

        if (dataBuffer instanceof DataBufferInt) {
            int[] imageDataBuffer = ((DataBufferInt) dataBuffer).getData();
            imageData.setPixels(0, 0, imageDataBuffer.length, imageDataBuffer, 0);
        } else if (dataBuffer instanceof DataBufferByte) {
            byte[] imageDataBuffer = ((DataBufferByte) dataBuffer).getData();
            try {
                imageData.setPixels(0, 0, imageDataBuffer.length, imageDataBuffer, 0);
            } catch (SWTException se) {
                // Unsupported depth
                return convertToSwt(display, convertToCompatibleFormat(awtImage),
                        transferAlpha, globalAlpha);
            }
        }

        if (transferAlpha) {
            byte[] alphaData = new byte[height * width];
            for (int y = 0; y < height; y++) {
                byte[] alphaRow = new byte[width];
                for (int x = 0; x < width; x++) {
                    int alpha = awtImage.getRGB(x, y) >>> 24;

                    // We have to multiply in the alpha now since if we
                    // set ImageData.alpha, it will ignore the alphaData.
                    if (globalAlpha != -1) {
                        alpha = alpha * globalAlpha >> 8;
                    }

                    alphaRow[x] = (byte) alpha;
                }
                System.arraycopy(alphaRow, 0, alphaData, y * width, width);
            }

            imageData.alphaData = alphaData;
        } else if (globalAlpha != -1) {
            imageData.alpha = globalAlpha;
        }

        return new Image(display, imageData);
    }

    /**
     * Converts a direct-color model SWT image to an equivalent AWT image. If the image
     * does not have a supported color model, returns null. This method does <b>NOT</b>
     * preserve alpha in the source image.
     *
     * @param swtImage the SWT image to be converted to AWT
     * @return an AWT image representing the source SWT image
     */
    public static BufferedImage convertToAwt(Image swtImage) {
        ImageData swtData = swtImage.getImageData();
        BufferedImage awtImage =
            new BufferedImage(swtData.width, swtData.height, BufferedImage.TYPE_INT_ARGB);
        PaletteData swtPalette = swtData.palette;
        if (swtPalette.isDirect) {
            PaletteData awtPalette = getAwtPaletteData(awtImage.getType());

            if (swtPalette.equals(awtPalette)) {
                // No color conversion needed.
                for (int y = 0; y < swtData.height; y++) {
                    for (int x = 0; x < swtData.width; x++) {
                      int pixel = swtData.getPixel(x, y);
                      awtImage.setRGB(x, y, 0xFF000000 | pixel);
                    }
                }
            } else {
                // We need to remap the colors
                int sr = -awtPalette.redShift   + swtPalette.redShift;
                int sg = -awtPalette.greenShift + swtPalette.greenShift;
                int sb = -awtPalette.blueShift  + swtPalette.blueShift;

                for (int y = 0; y < swtData.height; y++) {
                    for (int x = 0; x < swtData.width; x++) {
                      int pixel = swtData.getPixel(x, y);

                      int r = pixel & swtPalette.redMask;
                      int g = pixel & swtPalette.greenMask;
                      int b = pixel & swtPalette.blueMask;
                      r = (sr < 0) ? r >>> -sr : r << sr;
                      g = (sg < 0) ? g >>> -sg : g << sg;
                      b = (sb < 0) ? b >>> -sb : b << sb;

                      pixel = 0xFF000000 | r | g | b;
                      awtImage.setRGB(x, y, pixel);
                    }
                }
            }
        } else {
            return null;
        }

        return awtImage;
    }

    /**
     * Creates a new image from a source image where the contents from a given set of
     * bounding boxes are copied into the new image and the rest is left transparent. A
     * scale can be applied to make the resulting image larger or smaller than the source
     * image. Note that the alpha channel in the original image is ignored, and the alpha
     * values for the painted rectangles will be set to a specific value passed into this
     * function.
     *
     * @param image the source image
     * @param rectangles the set of rectangles (bounding boxes) to copy from the source
     *            image
     * @param boundingBox the bounding rectangle of the rectangle list, which can be
     *            computed by {@link ImageUtils#getBoundingRectangle}
     * @param scale a scale factor to apply to the result, e.g. 0.5 to shrink the
     *            destination down 50%, 1.0 to leave it alone and 2.0 to zoom in by
     *            doubling the image size
     * @param alpha the alpha (in the range 0-255) that painted bits should be set to
     * @return a pair of the rendered cropped image, and the location within the source
     *         image that the crop begins (multiplied by the scale). May return null if
     *         there are no selected items.
     */
    public static Image drawRectangles(Image image,
            List<Rectangle> rectangles, Rectangle boundingBox, double scale, byte alpha) {

        if (rectangles.size() == 0 || boundingBox == null || boundingBox.isEmpty()) {
            return null;
        }

        ImageData srcData = image.getImageData();
        int destWidth = (int) (scale * boundingBox.width);
        int destHeight = (int) (scale * boundingBox.height);

        ImageData destData = new ImageData(destWidth, destHeight, srcData.depth, srcData.palette);
        byte[] alphaData = new byte[destHeight * destWidth];
        destData.alphaData = alphaData;

        for (Rectangle bounds : rectangles) {
            int dx1 = bounds.x - boundingBox.x;
            int dy1 = bounds.y - boundingBox.y;
            int dx2 = dx1 + bounds.width;
            int dy2 = dy1 + bounds.height;

            dx1 *= scale;
            dy1 *= scale;
            dx2 *= scale;
            dy2 *= scale;

            int sx1 = bounds.x;
            int sy1 = bounds.y;
            int sx2 = sx1 + bounds.width;
            int sy2 = sy1 + bounds.height;

            if (scale == 1.0d) {
                for (int dy = dy1, sy = sy1; dy < dy2; dy++, sy++) {
                    for (int dx = dx1, sx = sx1; dx < dx2; dx++, sx++) {
                        destData.setPixel(dx, dy, srcData.getPixel(sx, sy));
                        alphaData[dy * destWidth + dx] = alpha;
                    }
                }
            } else {
                // Scaled copy.
                int sxDelta = sx2 - sx1;
                int dxDelta = dx2 - dx1;
                int syDelta = sy2 - sy1;
                int dyDelta = dy2 - dy1;
                for (int dy = dy1, sy = sy1; dy < dy2; dy++, sy = (dy - dy1) * syDelta / dyDelta
                        + sy1) {
                    for (int dx = dx1, sx = sx1; dx < dx2; dx++, sx = (dx - dx1) * sxDelta
                            / dxDelta + sx1) {
                        assert sx < sx2 && sy < sy2;
                        destData.setPixel(dx, dy, srcData.getPixel(sx, sy));
                        alphaData[dy * destWidth + dx] = alpha;
                    }
                }
            }
        }

        return new Image(image.getDevice(), destData);
    }

    /**
     * Creates a new empty/blank image of the given size
     *
     * @param display the display to associate the image with
     * @param width the width of the image
     * @param height the height of the image
     * @return a new blank image of the given size
     */
    public static Image createEmptyImage(Display display, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        return SwtUtils.convertToSwt(display, image, false, 0);
    }

    /**
     * Converts the given SWT {@link Rectangle} into an ADT {@link Rect}
     *
     * @param swtRect the SWT {@link Rectangle}
     * @return an equivalent {@link Rect}
     */
    public static Rect toRect(Rectangle swtRect) {
        return new Rect(swtRect.x, swtRect.y, swtRect.width, swtRect.height);
    }

    /**
     * Sets the values of the given ADT {@link Rect} to the values of the given SWT
     * {@link Rectangle}
     *
     * @param target the ADT {@link Rect} to modify
     * @param source the SWT {@link Rectangle} to read values from
     */
    public static void set(Rect target, Rectangle source) {
        target.set(source.x, source.y, source.width, source.height);
    }

    /**
     * Compares an ADT {@link Rect} with an SWT {@link Rectangle} and returns true if they
     * are equivalent
     *
     * @param r1 the ADT {@link Rect}
     * @param r2 the SWT {@link Rectangle}
     * @return true if the two rectangles are equivalent
     */
    public static boolean equals(Rect r1, Rectangle r2) {
        return r1.x == r2.x && r1.y == r2.y && r1.w == r2.width && r1.h == r2.height;

    }

    /**
     * Get the average width of the font used by the given control
     *
     * @param display the display associated with the font usage
     * @param font the font to look up the average character width for
     * @return the average width, in pixels, of the given font
     */
    public static final int getAverageCharWidth(Display display, Font font) {
        GC gc = new GC(display);
        gc.setFont(font);
        FontMetrics fontMetrics = gc.getFontMetrics();
        int width = fontMetrics.getAverageCharWidth();
        gc.dispose();
        return width;
    }

    /**
     * Get the average width of the given font
     *
     * @param control the control to look up the default font for
     * @return the average width, in pixels, of the current font in the control
     */
    public static final int getAverageCharWidth(Control control) {
        GC gc = new GC(control.getDisplay());
        int width = gc.getFontMetrics().getAverageCharWidth();
        gc.dispose();
        return width;
    }

    /**
     * Draws a drop shadow for the given rectangle into the given context. It
     * will not draw anything if the rectangle is smaller than a minimum
     * determined by the assets used to draw the shadow graphics.
     * <p>
     * This corresponds to {@link ImageUtils#drawRectangleShadow(Graphics, int, int, int, int)},
     * but applied directly to an SWT graphics context instead, such that no image conversion
     * has to be performed.
     * <p>
     * Make sure to keep changes in the visual appearance here in sync with the
     * AWT version in {@link ImageUtils#drawRectangleShadow(Graphics, int, int, int, int)}.
     *
     * @param gc the graphics context to draw into
     * @param x the left coordinate of the left hand side of the rectangle
     * @param y the top coordinate of the top of the rectangle
     * @param width the width of the rectangle
     * @param height the height of the rectangle
     */
    public static final void drawRectangleShadow(GC gc, int x, int y, int width, int height) {
        if (sShadowBottomLeft == null) {
            IconFactory icons = IconFactory.getInstance();
            // See ImageUtils.drawRectangleShadow for an explanation of the assets.
            sShadowBottomLeft  = icons.getIcon("shadow-bl"); //$NON-NLS-1$
            sShadowBottom      = icons.getIcon("shadow-b");  //$NON-NLS-1$
            sShadowBottomRight = icons.getIcon("shadow-br"); //$NON-NLS-1$
            sShadowRight       = icons.getIcon("shadow-r");  //$NON-NLS-1$
            sShadowTopRight    = icons.getIcon("shadow-tr"); //$NON-NLS-1$
            assert sShadowBottomRight.getImageData().width == SHADOW_SIZE;
            assert sShadowBottomRight.getImageData().height == SHADOW_SIZE;
        }

        ImageData bottomLeftData = sShadowBottomLeft.getImageData();
        ImageData topRightData = sShadowTopRight.getImageData();
        ImageData bottomData = sShadowBottom.getImageData();
        ImageData rightData = sShadowRight.getImageData();
        int blWidth = bottomLeftData.width;
        int trHeight = topRightData.height;
        if (width < blWidth) {
            return;
        }
        if (height < trHeight) {
            return;
        }

        gc.drawImage(sShadowBottomLeft, x, y + height);
        gc.drawImage(sShadowBottomRight, x + width, y + height);
        gc.drawImage(sShadowTopRight, x + width, y);
        gc.drawImage(sShadowBottom,
                0, 0,
                bottomData.width, bottomData.height,
                x + bottomLeftData.width, y + height,
                width - bottomLeftData.width, bottomData.height);
        gc.drawImage(sShadowRight,
                0, 0,
                rightData.width, rightData.height,
                x + width, y + topRightData.height,
                rightData.width, height - topRightData.height);
    }

    private static Image sShadowBottomLeft;
    private static Image sShadowBottom;
    private static Image sShadowBottomRight;
    private static Image sShadowRight;
    private static Image sShadowTopRight;
}

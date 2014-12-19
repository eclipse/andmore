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

import com.android.ide.common.api.Rect;

import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

@SuppressWarnings("javadoc")
public class ImageUtilsTest extends TestCase {
    public void testCropBlank() throws Exception {
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics g = image.getGraphics();
        g.setColor(new Color(0, true));
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.dispose();

        BufferedImage crop = ImageUtils.cropBlank(image, null);
        assertNull(crop);
    }

    public void testCropBlankPre() throws Exception {
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics g = image.getGraphics();
        g.setColor(new Color(0, true));
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.dispose();

        BufferedImage crop = ImageUtils.cropBlank(image, new Rect(5, 5, 80, 80));
        assertNull(crop);
    }

    public void testCropNonblank() throws Exception {
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics g = image.getGraphics();
        g.setColor(new Color(0, false));
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.dispose();

        BufferedImage crop = ImageUtils.cropBlank(image, null);
        assertNotNull(crop);
        assertEquals(image.getWidth(), crop.getWidth());
        assertEquals(image.getHeight(), crop.getHeight());
    }

    public void testCropSomething() throws Exception {
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics g = image.getGraphics();
        g.setColor(new Color(0, true));
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.setColor(new Color(0xFF00FF00, true));
        g.fillRect(25, 25, 50, 50);
        g.dispose();

        BufferedImage crop = ImageUtils.cropBlank(image, null);
        assertNotNull(crop);
        assertEquals(50, crop.getWidth());
        assertEquals(50, crop.getHeight());
        assertEquals(0xFF00FF00, crop.getRGB(0, 0));
        assertEquals(0xFF00FF00, crop.getRGB(49, 49));
    }

    public void testCropSomethingPre() throws Exception {
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics g = image.getGraphics();
        g.setColor(new Color(0, true));
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.setColor(new Color(0xFF00FF00, true));
        g.fillRect(25, 25, 50, 50);
        g.dispose();

        BufferedImage crop = ImageUtils.cropBlank(image, new Rect(0, 0, 100, 100));
        assertNotNull(crop);
        assertEquals(50, crop.getWidth());
        assertEquals(50, crop.getHeight());
        assertEquals(0xFF00FF00, crop.getRGB(0, 0));
        assertEquals(0xFF00FF00, crop.getRGB(49, 49));
    }

    public void testCropSomethingPre2() throws Exception {
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics g = image.getGraphics();
        g.setColor(new Color(0, true));
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.setColor(new Color(0xFF00FF00, true));
        g.fillRect(25, 25, 50, 50);
        g.dispose();

        BufferedImage crop = ImageUtils.cropBlank(image, new Rect(5, 5, 80, 80));
        assertNotNull(crop);
        assertEquals(50, crop.getWidth());
        assertEquals(50, crop.getHeight());
        assertEquals(0xFF00FF00, crop.getRGB(0, 0));
        assertEquals(0xFF00FF00, crop.getRGB(49, 49));
    }

    public void testCropColor() throws Exception {
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics g = image.getGraphics();
        g.setColor(new Color(0xFF00FF00, true));
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.dispose();

        BufferedImage crop = ImageUtils.cropColor(image, 0xFF00FF00, null);
        assertNull(crop);
    }

    public void testCropNonColor() throws Exception {
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics g = image.getGraphics();
        g.setColor(new Color(0xFF00FF00, true));
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.dispose();

        BufferedImage crop = ImageUtils.cropColor(image, 0xFFFF0000, null);
        assertNotNull(crop);
        assertEquals(image.getWidth(), crop.getWidth());
        assertEquals(image.getHeight(), crop.getHeight());
    }

    public void testCropColorSomething() throws Exception {
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics g = image.getGraphics();
        g.setColor(new Color(0xFF00FF00, true));
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.setColor(new Color(0xFFFF0000, true));
        g.fillRect(25, 25, 50, 50);
        g.dispose();

        BufferedImage crop = ImageUtils.cropColor(image, 0xFF00FF00, null);
        assertEquals(50, crop.getWidth());
        assertEquals(50, crop.getHeight());
        assertEquals(0xFFFF0000, crop.getRGB(0, 0));
        assertEquals(0xFFFF0000, crop.getRGB(49, 49));
    }

    public void testNothingTodo() throws Exception {
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics g = image.getGraphics();
        g.setColor(new Color(0xFF00FF00, true));
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.dispose();

        BufferedImage crop = ImageUtils.cropColor(image, 0xFFFF0000, new Rect(40, 40, 0, 0));
        assertNull(crop);
    }

    public void testContainsDarkPixels() throws Exception {
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics g = image.getGraphics();
        g.setColor(new Color(0, true));
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.dispose();

        assertFalse(ImageUtils.containsDarkPixels(image));

        image.setRGB(50, 50, 0xFFFFFFFF);
        assertFalse(ImageUtils.containsDarkPixels(image));
        image.setRGB(50, 50, 0xFFAAAAAA);
        assertFalse(ImageUtils.containsDarkPixels(image));
        image.setRGB(50, 50, 0xFF00FF00);
        assertFalse(ImageUtils.containsDarkPixels(image));
        image.setRGB(50, 50, 0xFFFF8800);
        assertFalse(ImageUtils.containsDarkPixels(image));
        image.setRGB(50, 50, 0xFF333333);
        assertTrue(ImageUtils.containsDarkPixels(image));

    }

    public void testGetBoundingRectangle() {
        assertEquals(null, ImageUtils.getBoundingRectangle(Collections.<Rectangle> emptyList()));

        assertEquals(new Rectangle(1, 2, 3, 4), ImageUtils.getBoundingRectangle(Arrays
                .asList(new Rectangle(1, 2, 3, 4))));
        assertEquals(new Rectangle(1, 2, 3, 4), ImageUtils.getBoundingRectangle(Arrays
                .asList(new Rectangle(1, 2, 3, 4), new Rectangle(1, 2, 1, 1))));
        assertEquals(new Rectangle(5, 5, 25, 25), ImageUtils.getBoundingRectangle(Arrays.asList(
                new Rectangle(10, 10, 20, 20), new Rectangle(5, 5, 1, 1))));
    }

    /**
     * Paints a set of {@link Rectangle} object out of a rendered {@link BufferedImage}
     * such that the resulting image is transparent except for a minimum bounding
     * rectangle of the selected elements.
     *
     * @param image the source image
     * @param rectangles the set of rectangles to copy
     * @param boundingBox the bounding rectangle of the set of rectangles to copy, can be
     *            computed by {@link ImageUtils#getBoundingRectangle}
     * @param scale a scale factor to apply to the result, e.g. 0.5 to shrink the
     *            destination down 50%, 1.0 to leave it alone and 2.0 to zoom in by
     *            doubling the image size
     * @return a rendered image, or null
     */
    public static BufferedImage drawRectangles(BufferedImage image,
            List<Rectangle> rectangles, Rectangle boundingBox, double scale) {

        // This code is not a test. When I implemented image cropping, I first implemented
        // it for BufferedImages (since it's easier; easy image painting, easy scaling,
        // easy transparency handling, etc). However, this meant that we would need to
        // convert the SWT images from the ImageOverlay to BufferedImages, crop and convert
        // back; not ideal, so I rewrote it in SWT (see SwtUtils). However, I
        // don't want to throw away the code in case we start keeping BufferedImages rather
        // than SWT images or need it for other purposes, but rather than place it in the
        // production codebase I'm leaving this utility here in the associated ImageUtils
        // test class. It was used like this:
        // @formatter:off
        //
        //    BufferedImage wholeImage = SwtUtils.convertToAwt(image);
        //    BufferedImage result = ImageUtils.cropSelection(wholeImage,
        //        rectangles, boundingBox, scale);
        //    e.image = SwtUtils.convertToSwt(image.getDevice(), result, true,
        //        DRAG_TRANSPARENCY);
        //
        // @formatter:on

        if (boundingBox == null) {
            return null;
        }

        int destWidth = (int) (scale * boundingBox.width);
        int destHeight = (int) (scale * boundingBox.height);
        BufferedImage dest = new BufferedImage(destWidth, destHeight, image.getType());

        Graphics2D g = dest.createGraphics();

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

            g.drawImage(image, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, null);
        }

        g.dispose();

        return dest;
    }

    public void testSubImage() throws Exception {
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics g = image.getGraphics();
        g.setColor(new Color(0xFF00FF00, true));
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.setColor(new Color(0xFFFF0000, true));
        g.fillRect(25, 25, 50, 50);
        g.dispose();

        BufferedImage sub = ImageUtils.subImage(image, 25, 25, 35, 45);
        assertEquals(10, sub.getWidth());
        assertEquals(20, sub.getHeight());
        assertEquals(0xFFFF0000, sub.getRGB(0, 0));
        assertEquals(0xFFFF0000, sub.getRGB(9, 9));

        sub = ImageUtils.subImage(image, 23, 23, 23 + 5, 23 + 5);
        assertEquals(5, sub.getWidth());
        assertEquals(5, sub.getHeight());
        assertEquals(0xFF00FF00, sub.getRGB(0, 0));
        assertEquals(0xFFFF0000, sub.getRGB(4, 4));
    }

    public void testGetColor() throws Exception {
        assertEquals(0xFF000000, ImageUtils.getColor("#000"));
        assertEquals(0xFF000000, ImageUtils.getColor("#000000"));
        assertEquals(0xABCDEF91, ImageUtils.getColor("#ABCDEF91"));
    }

    public void testGetBrightness() throws Exception {
        assertEquals(96, ImageUtils.getBrightness(0x456789));
        assertEquals(198, ImageUtils.getBrightness(0xABCDEF));

        assertEquals(0, ImageUtils.getBrightness(0x0));
        assertEquals(255, ImageUtils.getBrightness(0xFFFFFF));
        assertEquals(299*255/1000, ImageUtils.getBrightness(0xFF0000));
        assertEquals(587*255/1000, ImageUtils.getBrightness(0x00FF00));
        assertEquals(114*255/1000, ImageUtils.getBrightness(0x0000FF));
    }

    public void testColorConversion() throws Exception {
        assertEquals(0, ImageUtils.rgbToInt(ImageUtils.intToRgb(0), 0));
        assertEquals(0xFFFFFFFF, ImageUtils.rgbToInt(ImageUtils.intToRgb(0xFFFFFF), 0xFF));
        assertEquals(0x12345678, ImageUtils.rgbToInt(ImageUtils.intToRgb(0x345678), 0x12));
    }

    public void testScaleImage() throws Exception {
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics g = image.getGraphics();
        g.setColor(new Color(0xFF00FF00, true));
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.setColor(new Color(0xFFFF0000, true));
        g.fillRect(25, 25, 50, 50);
        g.dispose();

        BufferedImage scaled = ImageUtils.scale(image, 0.5, 0.5);
        assertEquals(50, scaled.getWidth());
        assertEquals(50, scaled.getHeight());
        assertEquals(0xFF00FF00, scaled.getRGB(0, 0));
        assertEquals(0xFF00FF00, scaled.getRGB(49, 49));
        assertEquals(0xFFFF0000, scaled.getRGB(25, 25));

        scaled = ImageUtils.scale(image, 2.0, 2.0);
        assertEquals(200, scaled.getWidth());
        assertEquals(200, scaled.getHeight());
        assertEquals(0xFF00FF00, scaled.getRGB(0, 0));
        assertEquals(0xFF00FF00, scaled.getRGB(48, 48));
        assertEquals(0xFFFF0000, scaled.getRGB(100, 100));
        assertEquals(0xFF00FF00, scaled.getRGB(199, 199));

        scaled = ImageUtils.scale(image, 0.25, 0.25);
        assertEquals(25, scaled.getWidth());
        assertEquals(25, scaled.getHeight());
        assertEquals(0xFF00FF00, scaled.getRGB(0, 0));
        assertEquals(0xFF00FF00, scaled.getRGB(24, 24));
        assertEquals(0xFFFF0000, scaled.getRGB(13, 13));

        scaled = ImageUtils.scale(image, 0.25, 0.25, 75, 95);
        assertEquals(100, scaled.getWidth());
        assertEquals(120, scaled.getHeight());
        assertEquals(0xFF00FF00, scaled.getRGB(0, 0));
        assertEquals(0xFF00FF00, scaled.getRGB(24, 24));
        assertEquals(0xFFFF0000, scaled.getRGB(13, 13));

    }

    public void testCreateColoredImage() throws Exception {
        BufferedImage image = ImageUtils.createColoredImage(120, 110, new RGB(0xFE, 0xFD, 0xFC));
        assertEquals(120, image.getWidth());
        assertEquals(110, image.getHeight());
        assertEquals(0xFFFEFDFC, image.getRGB(0, 0));
        assertEquals(0xFFFEFDFC, image.getRGB(50, 50));
        assertEquals(0xFFFEFDFC, image.getRGB(119, 109));
    }
}

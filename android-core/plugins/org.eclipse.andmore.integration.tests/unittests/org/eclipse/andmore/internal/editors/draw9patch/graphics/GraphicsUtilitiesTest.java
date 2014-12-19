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

package org.eclipse.andmore.internal.editors.draw9patch.graphics;

import java.util.Arrays;

import junit.framework.TestCase;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;

public class GraphicsUtilitiesTest extends TestCase {
    private static final int MASK_ALPHA = 0xFF000000;

    private static final String DIR = "/com/android/ide/eclipse/testdata/draw9patch/";

    public void testConvertToNinePatchNull() throws Exception {
        ImageData result = GraphicsUtilities.convertToNinePatch(null);
        assertNull(result);
    }

    public void testConvertToNinePatch() throws Exception {
        String fileName = DIR + "no-patched.png";
        Image image = new Image(Display.getDefault(),
                getClass().getResourceAsStream(fileName));
        ImageData baseData = image.getImageData();

        ImageData result = GraphicsUtilities.convertToNinePatch(baseData);

        assertEquals(baseData.width + 2, result.width);
        assertEquals(baseData.height + 2, result.height);

        // horizontal
        for (int x = 0; x < result.width; x++) {

            // top row
            assertEquals(0x0, result.getPixel(x, 0) & MASK_ALPHA);

            // bottom row
            assertEquals(0x0, result.getPixel(x, result.height - 1) & MASK_ALPHA);
        }

        // vertical
        for (int y = 0; y < result.height; y++) {

            // left column
            assertEquals(0x0, result.getPixel(0, y) & MASK_ALPHA);

            // right column
            assertEquals(0x0, result.getPixel(result.width - 1, y) & MASK_ALPHA);
        }
    }

    public void testClearImageDataNull() throws Exception {
        try {
            GraphicsUtilities.clearImageData(null);
            fail();
        } catch (IllegalArgumentException e) {
        }
    }

    public void testClearImageData() throws Exception {
        String fileName = DIR + "no-patched.png";
        Image image = new Image(Display.getDefault(),
                getClass().getResourceAsStream(fileName));

        ImageData baseData = image.getImageData();
        GraphicsUtilities.clearImageData(baseData);
        for (int y = 0; y < baseData.height; y++) {
            for (int x = 0; x < baseData.width; x++) {
                assertEquals(0x000000, baseData.getPixel(x, y));
                assertEquals(0x00, baseData.getAlpha(x, y));
            }
        }

    }

    public void testCopyNull() throws Exception {
        ImageData result = GraphicsUtilities.copy(null);
        assertNull(result);
    }

    public void testCopy() throws Exception {
        String fileName = DIR + "no-patched.png";
        Image image = new Image(Display.getDefault(),
                getClass().getResourceAsStream(fileName));

        ImageData baseData = image.getImageData();
        ImageData copiedData = GraphicsUtilities.copy(baseData);

        assertEquals(baseData.width, copiedData.width);
        assertEquals(baseData.height, copiedData.height);
        assertEquals(baseData.depth, copiedData.depth);
        assertEquals(baseData.transparentPixel, copiedData.transparentPixel);
        assertEquals(baseData.alpha, copiedData.alpha);
        assertTrue(baseData.palette.equals(copiedData.palette));

        final int[] baseColors = new int[baseData.width];
        final byte[] baseAlpha = new byte[baseData.width];

        final int[] copiedColors = new int[copiedData.width];
        final byte[] copiedAlpha = new byte[copiedData.width];

        for (int y = 0; y < baseData.height; y++) {

            baseData.getPixels(0, y, baseData.width, baseColors, 0);
            baseData.getPixels(0, y, baseData.width, copiedColors, 0);
            assertTrue(Arrays.equals(baseColors, copiedColors));

            baseData.getAlphas(0, y, baseData.width, baseAlpha, 0);
            baseData.getAlphas(0, y, baseData.width, copiedAlpha, 0);
            assertTrue(Arrays.equals(baseAlpha, copiedAlpha));

        }
    }

    public void testGetVerticalPixelsIllegalArgument() throws Exception {
        String fileName = DIR + "no-patched.png";
        Image image = new Image(Display.getDefault(),
                getClass().getResourceAsStream(fileName));

        ImageData baseData = image.getImageData();
        int[] temp = new int[baseData.width];

        // data must not be null
        try {
            GraphicsUtilities.getVerticalPixels(null, 0, 0, 1, temp);
            fail();
        } catch (IllegalArgumentException e) {
        }

        // out must not be null
        try {
            GraphicsUtilities.getVerticalPixels(baseData, 0, 0, 1, null);
            fail();
        } catch (IllegalArgumentException e) {
        }

        // out length must be > height
        try {
            GraphicsUtilities.getVerticalPixels(baseData, 0, 0, 1, new int[0]);
            fail();
        } catch (IllegalArgumentException e) {
        }

        // x must be > 0
        try {
            GraphicsUtilities.getVerticalPixels(baseData, -1, 0, 1, temp);
            fail();
        } catch (IllegalArgumentException e) {
        }

        // y must be > 0
        try {
            GraphicsUtilities.getVerticalPixels(baseData, 0, -1, 1, temp);
            fail();
        } catch (IllegalArgumentException e) {
        }

        // height must be >= 0
        try {
            GraphicsUtilities.getVerticalPixels(baseData, 0, 0, 0, temp);
            fail();
        } catch (IllegalArgumentException e) {
        }

        // argument x must be < data.width
        try {
            GraphicsUtilities.getVerticalPixels(baseData, baseData.width, 0, baseData.height, temp);
            fail();
        } catch (IllegalArgumentException e) {
        }

        // argument y must be < data.height
        try {
            GraphicsUtilities
                    .getVerticalPixels(baseData, 0, baseData.height, baseData.height, temp);
            fail();
        } catch (IllegalArgumentException e) {
        }

        // argument height must be > (y + data.height)
        try {
            GraphicsUtilities.getVerticalPixels(baseData, 0, 1, baseData.height, temp);
            fail();
        } catch (IllegalArgumentException e) {
        }

    }

    public void testGetVerticalPixels() throws Exception {
        String fileName = DIR + "no-patched.png";
        Image image = new Image(Display.getDefault(),
                getClass().getResourceAsStream(fileName));

        ImageData baseData = image.getImageData();
        int[] temp = new int[baseData.width];

        GraphicsUtilities.getVerticalPixels(baseData, 0, 0, baseData.height, temp);

        int height = baseData.height;
        for (int y = 0; y < height; y++) {
            assertEquals(baseData.getPixel(0, y), temp[y]);
        }
    }

    public void testGetHorizontalPixelsIllegalArgument() throws Exception {
        String fileName = DIR + "no-patched.png";
        Image image = new Image(Display.getDefault(),
                getClass().getResourceAsStream(fileName));

        ImageData baseData = image.getImageData();
        int[] temp = new int[baseData.width];

        // data must not be null
        try {
            GraphicsUtilities.getHorizontalPixels(null, 0, 0, 1, temp);
            fail();
        } catch (IllegalArgumentException e) {
        }

        // out must not be null
        try {
            GraphicsUtilities.getHorizontalPixels(baseData, 0, 0, 1, null);
            fail();
        } catch (IllegalArgumentException e) {
        }

        // out length must be > width
        try {
            GraphicsUtilities.getHorizontalPixels(baseData, 0, 0, 1, new int[0]);
            fail();
        } catch (IllegalArgumentException e) {
        }

        // x must be > 0
        try {
            GraphicsUtilities.getHorizontalPixels(baseData, -1, 0, 1, temp);
            fail();
        } catch (IllegalArgumentException e) {
        }

        // y must be > 0
        try {
            GraphicsUtilities.getHorizontalPixels(baseData, 0, -1, 1, temp);
            fail();
        } catch (IllegalArgumentException e) {
        }

        // width must be >= 0
        try {
            GraphicsUtilities.getHorizontalPixels(baseData, 0, 0, 0, temp);
            fail();
        } catch (IllegalArgumentException e) {
        }

        // argument x must be < data.width
        try {
            GraphicsUtilities
                    .getHorizontalPixels(baseData, baseData.width, 0, baseData.width, temp);
            fail();
        } catch (IllegalArgumentException e) {
        }

        // argument y must be < data.height
        try {
            GraphicsUtilities
                    .getHorizontalPixels(baseData, 0, baseData.height, baseData.width, temp);
            fail();
        } catch (IllegalArgumentException e) {
        }

        // argument width must be > (x + data.width)
        try {
            GraphicsUtilities.getHorizontalPixels(baseData, 1, 0, baseData.width, temp);
            fail();
        } catch (IllegalArgumentException e) {
        }

    }

    public void testGetHorizontalPixels() throws Exception {
        String fileName = DIR + "no-patched.png";
        Image image = new Image(Display.getDefault(),
                getClass().getResourceAsStream(fileName));

        ImageData baseData = image.getImageData();
        int[] temp = new int[baseData.width];

        GraphicsUtilities.getHorizontalPixels(baseData, 0, 0, baseData.width, temp);

        int width = baseData.width;
        for (int x = 0; x < width; x++) {
            assertEquals(baseData.getPixel(x, 0), temp[x]);
        }
    }

}

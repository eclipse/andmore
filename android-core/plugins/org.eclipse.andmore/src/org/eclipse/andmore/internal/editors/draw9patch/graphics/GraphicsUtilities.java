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

import org.eclipse.swt.graphics.ImageData;

/**
 * The utility class for SWT Image and ImageData manipulation.
 */
public class GraphicsUtilities {

    /**
     * Convert normal image to 9-patched.
     * @return Returns 9-patched ImageData object. If image is null, returns null.
     */
    public static ImageData convertToNinePatch(ImageData image) {
        if (image == null) {
            return null;
        }
        ImageData result = new ImageData(image.width + 2, image.height + 2, image.depth,
                image.palette);

        final int[] colors = new int[image.width];
        final byte[] alpha = new byte[image.width];

        for (int y = 0; y < image.height; y++) {

            // Copy pixels
            image.getPixels(0, y, image.width, colors, 0);
            result.setPixels(1, y + 1, image.width, colors, 0);

            // Copy alpha
            image.getAlphas(0, y, image.width, alpha, 0);
            result.setAlphas(1, y + 1, image.width, alpha, 0);
        }

        return result;
    }

    /**
     * Wipe all color and alpha pixels.
     */
    public static void clearImageData(ImageData imageData) {
        if (imageData == null) {
            throw new IllegalArgumentException("image data must not be null");
        }
        int width = imageData.width;
        int height = imageData.height;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                imageData.setPixel(x, y, 0x00000000);
                imageData.setAlpha(x, y, 0x00);
            }
        }
    }

    /**
     * Duplicate the image data.
     * @return If image is null, return null.
     */
    public static ImageData copy(ImageData image) {
        if (image == null) {
            return null;
        }
        ImageData result = new ImageData(image.width, image.height, image.depth,
                image.palette);

        final int[] colors = new int[image.width];
        final byte[] alpha = new byte[image.width];

        for (int y = 0; y < image.height; y++) {

            // Copy pixels
            image.getPixels(0, y, image.width, colors, 0);
            result.setPixels(0, y, image.width, colors, 0);

            // Copy alpha
            image.getAlphas(0, y, image.width, alpha, 0);
            result.setAlphas(0, y, image.width, alpha, 0);
        }

        return result;
    }

    /**
     * Get column pixels.
     * @return length of obtained pixels.
     */
    public static int getVerticalPixels(ImageData data, int x, int y, int height, int[] out) {
        if (data == null) {
            throw new IllegalArgumentException("data must not be null");
        }
        if (out == null) {
            throw new IllegalArgumentException("out array must not be null");
        }
        if (height > out.length) {
            throw new IllegalArgumentException("out array length must be > height");
        }
        if (data.height < (y + height)) {
            throw new IllegalArgumentException("image height must be > (y + height)");
        }
        if (x < 0 || y < 0) {
            throw new IllegalArgumentException("argument x, y must be >= 0");
        }
        if (x >= data.width) {
            throw new IllegalArgumentException("argument x must be < data.width");
        }
        if (y >= data.height) {
            throw new IllegalArgumentException("argument y must be < data.height");
        }
        if (height <= 0) {
            throw new IllegalArgumentException("argument height must be > 0");
        }

        int idx = 0;
        while (idx < height) {
            data.getPixels(x, (y + idx), 1, out, idx);
            idx++;
        }
        return idx;
    }

    /**
     * Get row pixels.
     */
    public static void getHorizontalPixels(ImageData data, int x, int y, int width, int[] out) {
        if (data == null) {
            throw new IllegalArgumentException("data must not be null");
        }
        if (out == null) {
            throw new IllegalArgumentException("out array must not be null");
        }
        if (width > out.length) {
            throw new IllegalArgumentException("out array length must be > width");
        }
        if (data.width < (x + width)) {
            throw new IllegalArgumentException("image height must be > (x + width)");
        }
        if (x < 0 || y < 0) {
            throw new IllegalArgumentException("argument x, y must be >= 0");
        }
        if (x >= data.width) {
            throw new IllegalArgumentException("argument x must be < data.width");
        }
        if (y >= data.height) {
            throw new IllegalArgumentException("argument y must be < data.height");
        }
        if (width <= 0) {
            throw new IllegalArgumentException("argument width must be > 0");
        }

        data.getPixels(x, y, width, out, 0);
    }

}

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

import static com.android.SdkConstants.DOT_9PNG;
import static com.android.SdkConstants.DOT_PNG;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The model of 9-patched image.
 */
public class NinePatchedImage {
    private static final boolean DEBUG = false;

    /**
     * Get 9-patched filename as like image.9.png .
     */
    public static String getNinePatchedFileName(String fileName) {
        if (fileName.endsWith(DOT_9PNG)) {
            return fileName;
        }
        return fileName.substring(0, fileName.lastIndexOf(DOT_PNG)) + DOT_9PNG;
    }

    // For stretch regions and padding
    public static final int BLACK_TICK = 0xFF000000;
    // For Layout Bounds
    public static final int RED_TICK = 0xFFFF0000;
    // Blank
    public static final int TRANSPARENT_TICK = 0x00000000;

    private ImageData mBaseImageData;

    private Image mBaseImage = null;

    private boolean mHasNinePatchExtension = false;

    private boolean mDirtyFlag = false;

    private int[] mHorizontalPatchPixels = null;
    private int[] mVerticalPatchPixels = null;

    private int[] mHorizontalContentPixels = null;
    private int[] mVerticalContentPixels = null;

    // for Prevent unexpected stretch in StretchsView
    private boolean mRedTickOnlyInHorizontalFlag = false;
    private boolean mRedTickOnlyInVerticalFlag = false;

    private final List<Tick> mHorizontalPatches = new ArrayList<Tick>();
    private final List<Tick> mVerticalPatches = new ArrayList<Tick>();

    private final List<Tick> mHorizontalContents = new ArrayList<Tick>();
    private final List<Tick> mVerticalContents = new ArrayList<Tick>();


    private static final int CHUNK_BIN_SIZE = 100;
    private final List<Chunk> mChunkBin = new ArrayList<Chunk>(CHUNK_BIN_SIZE);

    private int mHorizontalFixedPatchSum = 0;
    private int mVerticalFixedPatchSum = 0;

    private static final int PROJECTION_BIN_SIZE = 100;
    private final List<Projection> mProjectionBin = new ArrayList<Projection>(PROJECTION_BIN_SIZE);

    private Chunk[][] mPatchChunks = null;

    public ImageData getImageData() {
        return mBaseImageData;
    }

    public int getWidth() {
        return mBaseImageData.width;
    }

    public int getHeight() {
        return mBaseImageData.height;
    }

    public Image getImage() {
        if (mBaseImage == null) {
            mBaseImage = new Image(AdtPlugin.getDisplay(), mBaseImageData);
        }
        return mBaseImage;
    }

    public boolean hasNinePatchExtension() {
        return mHasNinePatchExtension;
    }

    /**
     * Get the image has/hasn't been edited flag.
     * @return If has been edited, return true
     */
    public boolean isDirty() {
        return mDirtyFlag;
    }

    /**
     * Clear dirty(edited) flag.
     */
    public void clearDirtyFlag() {
        mDirtyFlag = false;
    }

    public NinePatchedImage(String fileName) {
        boolean hasNinePatchExtension = fileName.endsWith(DOT_9PNG);
        ImageData data = new ImageData(fileName);

        initNinePatchedImage(data, hasNinePatchExtension);
    }

    public NinePatchedImage(InputStream inputStream, String fileName) {
        boolean hasNinePatchExtension = fileName.endsWith(DOT_9PNG);
        ImageData data = new ImageData(inputStream);

        initNinePatchedImage(data, hasNinePatchExtension);
    }

    private Chunk getChunk() {
        if (mChunkBin.size() > 0) {
            Chunk chunk = mChunkBin.remove(0);
            chunk.init();
            return chunk;
        }
        return new Chunk();
    }

    private static final void recycleChunks(Chunk[][] patchChunks, List<Chunk> bin) {
        int yLen = patchChunks.length;
        int xLen = patchChunks[0].length;

        for (int y = 0; y < yLen; y++) {
            for (int x = 0; x < xLen; x++) {
                if (bin.size() < CHUNK_BIN_SIZE) {
                    bin.add(patchChunks[y][x]);
                }
                patchChunks[y][x] = null;
            }
        }
    }

    private Projection getProjection() {
        if (mProjectionBin.size() > 0) {
            Projection projection = mProjectionBin.remove(0);
            return projection;
        }
        return new Projection();
    }

    private static final void recycleProjections(Projection[][] projections, List<Projection> bin) {
        int yLen = projections.length;
        int xLen = 0;
        if (yLen > 0) {
            xLen = projections[0].length;
        }

        for (int y = 0; y < yLen; y++) {
            for (int x = 0; x < xLen; x++) {
                if (bin.size() < CHUNK_BIN_SIZE) {
                    bin.add(projections[y][x]);
                }
                projections[y][x] = null;
            }
        }
    }

    private static final int[] initArray(int[] array) {
        int len = array.length;
        for (int i = 0; i < len; i++) {
            array[i] = TRANSPARENT_TICK;
        }
        return array;
    }

    /**
     * Get one pixel with alpha from the image.
     * @return packed integer value as ARGB8888
     */
    private static final int getPixel(ImageData image, int x, int y) {
        return (image.getAlpha(x, y) << 24) + image.getPixel(x, y);
    }

    private static final boolean isTransparentPixel(ImageData image, int x, int y) {
        return image.getAlpha(x, y) == 0x0;
    }

    private static final boolean isValidTickColor(int pixel) {
        return (pixel == BLACK_TICK || pixel == RED_TICK);
    }

    private void initNinePatchedImage(ImageData imageData, boolean hasNinePatchExtension) {
        mBaseImageData = imageData;
        mHasNinePatchExtension = hasNinePatchExtension;
    }

    private boolean ensurePixel(int x, int y, int[] pixels, int index) {
        boolean isValid = true;
        int pixel = getPixel(mBaseImageData, x, y);
        if (!isTransparentPixel(mBaseImageData, x, y)) {
            if (index == 0 || index == pixels.length - 1) {
                isValid = false;
            }
            if (isValidTickColor(pixel)) {
                pixels[index] = pixel;
            } else {
                isValid = false;
            }
            // clear pixel
            mBaseImageData.setAlpha(x, y, 0x0);
        }
        return isValid;
    }

    private boolean ensureHorizontalPixel(int x, int y, int[] pixels) {
        return ensurePixel(x, y, pixels, x);
    }

    private boolean ensureVerticalPixel(int x, int y, int[] pixels) {
        return ensurePixel(x, y, pixels, y);
    }

    /**
     * Ensure that image data is 9-patch.
     */
    public boolean ensure9Patch() {
        boolean isValid = true;

        int width = mBaseImageData.width;
        int height = mBaseImageData.height;

        createPatchArray();
        createContentArray();

        // horizontal
        for (int x = 0; x < width; x++) {
            // top row
            if (!ensureHorizontalPixel(x, 0, mHorizontalPatchPixels)) {
                isValid = false;
            }
            // bottom row
            if (!ensureHorizontalPixel(x, height - 1, mHorizontalContentPixels)) {
                isValid = false;
            }
        }
        // vertical
        for (int y = 0; y < height; y++) {
            // left column
            if (!ensureVerticalPixel(0, y, mVerticalPatchPixels)) {
                isValid = false;
            }
            // right column
            if (!ensureVerticalPixel(width -1, y, mVerticalContentPixels)) {
                isValid = false;
            }
        }
        findPatches();
        findContentsArea();

        return isValid;
    }

    private void createPatchArray() {
        mHorizontalPatchPixels = initArray(new int[mBaseImageData.width]);
        mVerticalPatchPixels = initArray(new int[mBaseImageData.height]);
    }

    private void createContentArray() {
        mHorizontalContentPixels = initArray(new int[mBaseImageData.width]);
        mVerticalContentPixels = initArray(new int[mBaseImageData.height]);
    }

    /**
     * Convert to 9-patch image.
     * <p>
     * This method doesn't consider that target image is already 9-patched or
     * not.
     * </p>
     */
    public void convertToNinePatch() {
        mBaseImageData = GraphicsUtilities.convertToNinePatch(mBaseImageData);
        mHasNinePatchExtension = true;

        createPatchArray();
        createContentArray();

        findPatches();
        findContentsArea();
    }

    public boolean isValid(int x, int y) {
        return (x == 0) ^ (y == 0)
                ^ (x == mBaseImageData.width - 1) ^ (y == mBaseImageData.height - 1);
    }

    /**
     * Set patch or content.
     */
    public void setPatch(int x, int y, int color) {
        if (isValid(x, y)) {
            if (x == 0) {
                mVerticalPatchPixels[y] = color;
            } else if (y == 0) {
                mHorizontalPatchPixels[x] = color;
            } else if (x == mBaseImageData.width - 1) {
                mVerticalContentPixels[y] = color;
            } else if (y == mBaseImageData.height - 1) {
                mHorizontalContentPixels[x] = color;
            }

            // Mark as dirty
            mDirtyFlag = true;
        }
    }

    /**
     * Erase the pixel.
     */
    public void erase(int x, int y) {
        if (isValid(x, y)) {
            int color = TRANSPARENT_TICK;
            if (x == 0) {
                mVerticalPatchPixels[y] = color;
            } else if (y == 0) {
                mHorizontalPatchPixels[x] = color;
            } else if (x == mBaseImageData.width - 1) {
                mVerticalContentPixels[y] = color;
            } else if (y == mBaseImageData.height - 1) {
                mHorizontalContentPixels[x] = color;
            }

            // Mark as dirty
            mDirtyFlag = true;
        }
    }

    public List<Tick> getHorizontalPatches() {
        return mHorizontalPatches;
    }

    public List<Tick> getVerticalPatches() {
        return mVerticalPatches;
    }

    /**
     * Find patches from pixels array.
     * @param pixels Target of seeking ticks.
     * @param out Add the found ticks.
     * @return If BlackTick is not found but only RedTick is found, returns true
     */
    private static boolean findPatches(int[] pixels, List<Tick> out) {
        boolean redTickOnly = true;
        Tick patch = null;
        int len = 0;

        // find patches
        out.clear();
        len = pixels.length - 1;
        for (int i = 1; i < len; i++) {
            int pixel = pixels[i];

            if (redTickOnly && pixel != TRANSPARENT_TICK && pixel != RED_TICK) {
                redTickOnly = false;
            }

            if (patch != null) {
                if (patch.color != pixel) {
                    patch.end = i;
                    out.add(patch);
                    patch = null;
                }
            }
            if (patch == null) {
                patch = new Tick(pixel);
                patch.start = i;
            }
        }

        if (patch != null) {
            patch.end = len;
            out.add(patch);
        }
        return redTickOnly;
    }

    public void findPatches() {

        // find horizontal patches
        mRedTickOnlyInHorizontalFlag = findPatches(mHorizontalPatchPixels, mHorizontalPatches);

        // find vertical patches
        mRedTickOnlyInVerticalFlag = findPatches(mVerticalPatchPixels, mVerticalPatches);
    }

    public Rectangle getContentArea() {
        Tick horizontal = getContentArea(mHorizontalContents);
        Tick vertical = getContentArea(mVerticalContents);

        Rectangle rect = new Rectangle(0, 0, 0, 0);
        rect.x = 1;
        rect.width = mBaseImageData.width - 1;
        rect.y = 1;
        rect.height = mBaseImageData.height - 1;

        if (horizontal != null) {
            rect.x = horizontal.start;
            rect.width = horizontal.getLength();
        }
        if (vertical != null) {
            rect.y = vertical.start;
            rect.height = vertical.getLength();
        }

        return rect;
    }

    private Tick getContentArea(List<Tick> list) {
        int size = list.size();
        if (size == 0) {
            return null;
        }
        if (size == 1) {
            return list.get(0);
        }

        Tick start = null;
        Tick end = null;

        for (int i = 0; i < size; i++) {
            Tick t = list.get(i);
            if (t.color == BLACK_TICK) {
                if (start == null) {
                    start = t;
                    end = t;
                } else {
                    end = t;
                }
            }
        }

        // red tick only
        if (start == null) {
            return null;
        }

        Tick result = new Tick(start.color);
        result.start = start.start;
        result.end = end.end;

        return result;
    }

    public List<Tick> getHorizontalContents() {
        return mHorizontalContents;
    }

    public List<Tick> getVerticalContents() {
        return mVerticalContents;
    }

    private static void findContentArea(int[] pixels, List<Tick> out) {
        Tick contents = null;
        int len = 0;

        // find horizontal contents area
        out.clear();
        len = pixels.length - 1;
        for (int x = 1; x < len; x++) {
            if (contents != null) {
                if (contents.color != pixels[x]) {
                    contents.end = x;
                    out.add(contents);
                    contents = null;
                }
            }
            if (contents == null) {
                contents = new Tick(pixels[x]);
                contents.start = x;
            }
        }

        if (contents != null) {
            contents.end = len;
            out.add(contents);
        }
    }

    public void findContentsArea() {

        // find horizontal contents area
        findContentArea(mHorizontalContentPixels, mHorizontalContents);

        // find vertical contents area
        findContentArea(mVerticalContentPixels, mVerticalContents);
    }

    /**
     * Get raw image data.
     * <p>
     * The raw image data is applicable for save.
     * </p>
     */
    public ImageData getRawImageData() {
        ImageData image = GraphicsUtilities.copy(mBaseImageData);

        final int width = image.width;
        final int height = image.height;
        int len = 0;

        len = mHorizontalPatchPixels.length;
        for (int x = 0; x < len; x++) {
            int pixel = mHorizontalPatchPixels[x];
            if (pixel != TRANSPARENT_TICK) {
                image.setAlpha(x, 0, 0xFF);
                image.setPixel(x, 0, pixel);
            }
        }

        len = mVerticalPatchPixels.length;
        for (int y = 0; y < len; y++) {
            int pixel = mVerticalPatchPixels[y];
            if (pixel != TRANSPARENT_TICK) {
                image.setAlpha(0, y, 0xFF);
                image.setPixel(0, y, pixel);
            }
        }

        len = mHorizontalContentPixels.length;
        for (int x = 0; x < len; x++) {
            int pixel = mHorizontalContentPixels[x];
            if (pixel != TRANSPARENT_TICK) {
                image.setAlpha(x, height - 1, 0xFF);
                image.setPixel(x, height - 1, pixel);
            }
        }

        len = mVerticalContentPixels.length;
        for (int y = 0; y < len; y++) {
            int pixel = mVerticalContentPixels[y];
            if (pixel != TRANSPARENT_TICK) {
                image.setAlpha(width - 1, y, 0xFF);
                image.setPixel(width - 1, y, pixel);
            }
        }

        return image;
    }

    public Chunk[][] getChunks(Chunk[][] chunks) {
        int lenY = mVerticalPatches.size();
        int lenX = mHorizontalPatches.size();

        if (lenY == 0 || lenX == 0) {
            return null;
        }

        if (chunks == null) {
            chunks = new Chunk[lenY][lenX];
        } else {
            int y = chunks.length;
            int x = chunks[0].length;
            if (lenY != y || lenX != x) {
                recycleChunks(chunks, mChunkBin);
                chunks = new Chunk[lenY][lenX];
            }
        }

        // for calculate weights
        float horizontalPatchSum = 0;
        float verticalPatchSum = 0;

        mVerticalFixedPatchSum = 0;
        mHorizontalFixedPatchSum = 0;

        for (int y = 0; y < lenY; y++) {
            Tick yTick = mVerticalPatches.get(y);

            for (int x = 0; x < lenX; x++) {
                Tick xTick = mHorizontalPatches.get(x);
                Chunk t = getChunk();
                chunks[y][x] = t;

                t.rect.x = xTick.start;
                t.rect.width = xTick.getLength();
                t.rect.y = yTick.start;
                t.rect.height = yTick.getLength();

                if (mRedTickOnlyInHorizontalFlag
                        || xTick.color == BLACK_TICK || lenX == 1) {
                    t.type += Chunk.TYPE_HORIZONTAL;
                    if (y == 0) {
                        horizontalPatchSum += t.rect.width;
                    }
                }
                if (mRedTickOnlyInVerticalFlag
                        || yTick.color == BLACK_TICK || lenY == 1) {
                    t.type += Chunk.TYPE_VERTICAL;
                    if (x == 0) {
                        verticalPatchSum += t.rect.height;
                    }
                }

                if ((t.type & Chunk.TYPE_HORIZONTAL) == 0 && lenX > 1 && y == 0) {
                    mHorizontalFixedPatchSum += t.rect.width;
                }
                if ((t.type & Chunk.TYPE_VERTICAL) == 0 && lenY > 1 && x == 0) {
                    mVerticalFixedPatchSum += t.rect.height;
                }

            }
        }

        // calc weights
        for (int y = 0; y < lenY; y++) {
            for (int x = 0; x < lenX; x++) {
                Chunk chunk = chunks[y][x];
                if ((chunk.type & Chunk.TYPE_HORIZONTAL) != 0) {
                    chunk.horizontalWeight = chunk.rect.width / horizontalPatchSum;
                }
                if ((chunk.type & Chunk.TYPE_VERTICAL) != 0) {
                    chunk.verticalWeight = chunk.rect.height / verticalPatchSum;

                }
            }
        }

        return chunks;
    }

    public Chunk[][] getCorruptedChunks(Chunk[][] chunks) {
        chunks = getChunks(chunks);

        if (chunks != null) {
            int yLen = chunks.length;
            int xLen = chunks[0].length;

            for (int yPos = 0; yPos < yLen; yPos++) {
                for (int xPos = 0; xPos < xLen; xPos++) {
                    Chunk c = chunks[yPos][xPos];
                    Rectangle r = c.rect;
                    if ((c.type & Chunk.TYPE_HORIZONTAL) != 0
                            && isHorizontalCorrupted(mBaseImageData, r)) {
                        c.type |= Chunk.TYPE_CORRUPT;
                    }
                    if ((c.type & Chunk.TYPE_VERTICAL) != 0
                            && isVerticalCorrupted(mBaseImageData, r)) {
                        c.type |= Chunk.TYPE_CORRUPT;
                    }
                }
            }
        }
        return chunks;
    }

    private static boolean isVerticalCorrupted(ImageData data, Rectangle r) {
        int[] column = new int[r.width];
        int[] sample = new int[r.width];

        GraphicsUtilities.getHorizontalPixels(data, r.x, r.y, r.width, column);

        int lenY = r.y + r.height;
        for (int y = r.y; y < lenY; y++) {
            GraphicsUtilities.getHorizontalPixels(data, r.x, y, r.width, sample);
            if (!Arrays.equals(column, sample)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isHorizontalCorrupted(ImageData data, Rectangle r) {
        int[] column = new int[r.height];
        int[] sample = new int[r.height];
        GraphicsUtilities.getVerticalPixels(data, r.x, r.y, r.height, column);

        int lenX = r.x + r.width;
        for (int x = r.x; x < lenX; x++) {
            GraphicsUtilities.getVerticalPixels(data, x, r.y, r.height, sample);
            if (!Arrays.equals(column, sample)) {
                return true;
            }
        }
        return false;
    }

    public Projection[][] getProjections(int width, int height, Projection[][] projections) {
        mPatchChunks = getChunks(mPatchChunks);
        if (mPatchChunks == null) {
            return null;
        }

        if (DEBUG) {
            System.out.println(String.format("width:%d, height:%d", width, height));
        }

        int lenY = mPatchChunks.length;
        int lenX = mPatchChunks[0].length;

        if (projections == null) {
            projections = new Projection[lenY][lenX];
        } else {
            int y = projections.length;
            int x = projections[0].length;
            if (lenY != y || lenX != x) {
                recycleProjections(projections, mProjectionBin);
                projections = new Projection[lenY][lenX];
            }
        }

        float xZoom = ((float) width / mBaseImageData.width);
        float yZoom = ((float) height / mBaseImageData.height);

        if (DEBUG) {
            System.out.println(String.format("xZoom:%f, yZoom:%f", xZoom, yZoom));
        }

        int destX = 0;
        int destY = 0;
        int streatchableWidth = width - mHorizontalFixedPatchSum;
        streatchableWidth = streatchableWidth > 0 ? streatchableWidth : 1;

        int streatchableHeight = height - mVerticalFixedPatchSum;
        streatchableHeight = streatchableHeight > 0 ? streatchableHeight : 1;

        if (DEBUG) {
            System.out.println(String.format("streatchable %d %d", streatchableWidth,
                    streatchableHeight));
        }

        for (int yPos = 0; yPos < lenY; yPos++) {
            destX = 0;
            Projection p = null;
            for (int xPos = 0; xPos < lenX; xPos++) {
                Chunk chunk = mPatchChunks[yPos][xPos];

                if (DEBUG) {
                    System.out.println(String.format("Tile[%d, %d] = %s",
                            yPos, xPos, chunk.toString()));
                }

                p = getProjection();
                projections[yPos][xPos] = p;

                p.chunk = chunk;
                p.src = chunk.rect;
                p.dest.x = destX;
                p.dest.y = destY;

                // fixed size
                p.dest.width = chunk.rect.width;
                p.dest.height = chunk.rect.height;

                // horizontal stretch
                if ((chunk.type & Chunk.TYPE_HORIZONTAL) != 0) {
                    p.dest.width = Math.round(streatchableWidth * chunk.horizontalWeight);
                }
                // vertical stretch
                if ((chunk.type & Chunk.TYPE_VERTICAL) != 0) {
                    p.dest.height = Math.round(streatchableHeight * chunk.verticalWeight);
                }

                destX += p.dest.width;
            }
            destY += p.dest.height;
        }
        return projections;
    }

    /**
     * Projection class for make relation between chunked image and resized image.
     */
    public static class Projection {
        public Chunk chunk = null;
        public Rectangle src = null;
        public final Rectangle dest = new Rectangle(0, 0, 0, 0);

        @Override
        public String toString() {
            return String.format("src[%d, %d, %d, %d] => dest[%d, %d, %d, %d]",
                    src.x, src.y, src.width, src.height,
                    dest.x, dest.y, dest.width, dest.height);
        }
    }

    public static class Chunk {
        public static final int TYPE_FIXED = 0x0;
        public static final int TYPE_HORIZONTAL = 0x1;
        public static final int TYPE_VERTICAL = 0x2;
        public static final int TYPE_CORRUPT = 0x80000000;

        public int type = TYPE_FIXED;

        public Rectangle rect = new Rectangle(0, 0, 0, 0);

        public float horizontalWeight = 0.0f;
        public float verticalWeight = 0.0f;

        void init() {
            type = Chunk.TYPE_FIXED;
            horizontalWeight = 0.0f;
            verticalWeight = 0.0f;
            rect.x = 0;
            rect.y = 0;
            rect.width = 0;
            rect.height = 0;
        }

        private String typeToString() {
            switch (type) {
                case TYPE_FIXED:
                    return "FIXED";
                case TYPE_HORIZONTAL:
                    return "HORIZONTAL";
                case TYPE_VERTICAL:
                    return "VERTICAL";
                case TYPE_HORIZONTAL + TYPE_VERTICAL:
                    return "BOTH";
                default:
                    return "UNKNOWN";
            }
        }

        @Override
        public String toString() {
            return String.format("%s %f/%f %s", typeToString(), horizontalWeight, verticalWeight,
                    rect.toString());
        }
    }

    public static class Tick {
        public int start;
        public int end;
        public int color;

        /**
         * Get the tick length.
         */
        public int getLength() {
            return end - start;
        }

        public Tick(int tickColor) {
            color = tickColor;
        }

        @Override
        public String toString() {
            return String.format("%d tick: %d to %d", color, start, end);
        }
    }
}

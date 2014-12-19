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

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.andmore.internal.editors.draw9patch.graphics.NinePatchedImage.Chunk;
import org.eclipse.andmore.internal.editors.draw9patch.graphics.NinePatchedImage.Projection;
import org.eclipse.andmore.internal.editors.draw9patch.graphics.NinePatchedImage.Tick;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

public class NinePatchedImageTest extends TestCase {

    private static final String DIR = "/com/android/ide/eclipse/testdata/draw9patch/";

    public void testReadNoPatchedImage() throws Exception {
        String fileName = DIR + "no-patched.png";
        NinePatchedImage image = new NinePatchedImage(getClass()
                .getResourceAsStream(fileName), fileName);
        assertNotNull(image);

        ImageData data = image.getImageData();
        int width = data.width;
        int height = data.height;

        assertEquals(72, width);
        assertEquals(50, height);

        assertFalse(image.hasNinePatchExtension());
    }

    public void testReadNoPatchedInteraceImage() throws Exception {
        String fileName = DIR + "no-patched-interlace.png";
        NinePatchedImage image = new NinePatchedImage(getClass()
                .getResourceAsStream(fileName), fileName);
        assertNotNull(image);

        ImageData data = image.getImageData();
        int width = data.width;
        int height = data.height;

        assertEquals(72, width);
        assertEquals(50, height);

        assertFalse(image.hasNinePatchExtension());
    }

    public void testConvert9PatchedImage() throws Exception {
        String fileName = DIR + "no-patched.png";
        NinePatchedImage image = new NinePatchedImage(getClass()
                .getResourceAsStream(fileName), fileName);
        assertNotNull(image);

        ImageData data = image.getImageData();
        int width = data.width;
        int height = data.height;

        assertEquals(72, width);
        assertEquals(50, height);

        assertFalse(image.hasNinePatchExtension());

        image.convertToNinePatch();

        data = image.getImageData();
        width = data.width;
        height = data.height;

        // increased patch size
        assertEquals(72 + 2, width);
        assertEquals(50 + 2, height);

        assertTrue(image.hasNinePatchExtension());
        assertFalse(image.isDirty());

        // initialized patches
        List<Tick> horizontalPatches = image.getHorizontalPatches();
        List<Tick> verticalPatches = image.getVerticalPatches();
        assertEquals(1, horizontalPatches.size());
        assertEquals(1, verticalPatches.size());

        // initialized contents area
        List<Tick> horizontalContentsArea = image.getHorizontalContents();
        List<Tick> verticalContentsArea = image.getVerticalContents();
        assertEquals(1, horizontalContentsArea.size());
        assertEquals(1, verticalContentsArea.size());

        // content area rectangle
        Rectangle contentsArea = image.getContentArea();
        assertEquals(new Rectangle(1, 1, width - 2, height - 2), contentsArea);
    }

    public void testReadInvalidPatchedImageCorners() throws Exception {

        // top-left
        String fileName = DIR + "invalid-patched1.9.png";
        NinePatchedImage image = new NinePatchedImage(getClass()
                .getResourceAsStream(fileName), fileName);
        assertNotNull(image);
        assertTrue(image.hasNinePatchExtension());
        assertFalse(image.ensure9Patch());

        // top-right
        fileName = DIR + "invalid-patched2.9.png";
        image = new NinePatchedImage(getClass()
                .getResourceAsStream(fileName), fileName);
        assertNotNull(image);
        assertTrue(image.hasNinePatchExtension());
        assertFalse(image.ensure9Patch());

        // bottom-left
        fileName = DIR + "invalid-patched3.9.png";
        image = new NinePatchedImage(getClass()
                .getResourceAsStream(fileName), fileName);
        assertNotNull(image);
        assertTrue(image.hasNinePatchExtension());
        assertFalse(image.ensure9Patch());

        // bottom-right
        fileName = DIR + "invalid-patched4.9.png";
        image = new NinePatchedImage(getClass()
                .getResourceAsStream(fileName), fileName);
        assertNotNull(image);
        assertTrue(image.hasNinePatchExtension());
        assertFalse(image.ensure9Patch());
    }

    public void testReadInvalidPatchedImageLine() throws Exception {

        // top
        String fileName = DIR + "invalid-patched5.9.png";
        NinePatchedImage image = new NinePatchedImage(getClass()
                .getResourceAsStream(fileName), fileName);
        assertNotNull(image);
        assertTrue(image.hasNinePatchExtension());
        assertFalse(image.ensure9Patch());

        // right
        fileName = DIR + "invalid-patched6.9.png";
        image = new NinePatchedImage(getClass()
                .getResourceAsStream(fileName), fileName);
        assertNotNull(image);
        assertTrue(image.hasNinePatchExtension());
        assertFalse(image.ensure9Patch());

        // bottom
        fileName = DIR + "invalid-patched7.9.png";
        image = new NinePatchedImage(getClass()
                .getResourceAsStream(fileName), fileName);
        assertNotNull(image);
        assertTrue(image.hasNinePatchExtension());
        assertFalse(image.ensure9Patch());

        // left
        fileName = DIR + "invalid-patched8.9.png";
        image = new NinePatchedImage(getClass()
                .getResourceAsStream(fileName), fileName);
        assertNotNull(image);
        assertTrue(image.hasNinePatchExtension());
        assertFalse(image.ensure9Patch());
    }

    public void testEnsure9PatchIgnoreInvalidPixels() throws Exception {
        // top
        String fileName = DIR + "invalid-patched5.9.png";
        NinePatchedImage image = new NinePatchedImage(getClass()
                .getResourceAsStream(fileName), fileName);
        assertNotNull(image);

        // invalid pixel
        int invalidPixel = image.getImageData().getPixel(33, 0);
        assertTrue(0x0 != invalidPixel);

        assertTrue(image.hasNinePatchExtension());
        assertFalse(image.ensure9Patch());

        // ensure9path() ignored invalid pixels
        int invalidPixelAlpha = image.getImageData().getAlpha(33, 0);
        assertEquals(0x00, invalidPixelAlpha);
    }

    public void test9Patch1() throws Exception {
        String fileName = DIR + "patched1.9.png";
        NinePatchedImage image = new NinePatchedImage(getClass()
                .getResourceAsStream(fileName), fileName);
        assertNotNull(image);

        assertTrue(image.hasNinePatchExtension());
        assertTrue(image.ensure9Patch());

        // patches
        List<Tick> horizontalPatches = image.getHorizontalPatches();
        List<Tick> verticalPatches = image.getVerticalPatches();
        assertEquals(3, horizontalPatches.size());
        assertEquals(3, verticalPatches.size());

        Chunk[][] chunks = null;
        chunks = image.getChunks(chunks);

        // vertical chunk size
        assertEquals(3, chunks.length);

        // horizontal chunk size
        for (int i = 0; i < chunks.length; i++) {
            assertEquals(3, chunks[i].length);
        }

        Chunk c = null;
        Rectangle rect = null;

        // Row 1
        c = chunks[0][0];
        rect = c.rect;
        assertEquals(Chunk.TYPE_HORIZONTAL | Chunk.TYPE_VERTICAL, c.type);
        assertEquals(1, rect.x);
        assertEquals(1, rect.y);
        assertEquals(1, rect.width);
        assertEquals(1, rect.height);

        c = chunks[0][1];
        rect = c.rect;
        assertEquals(Chunk.TYPE_VERTICAL, c.type);
        assertEquals(2, rect.x);
        assertEquals(1, rect.y);
        assertEquals(70, rect.width);
        assertEquals(1, rect.height);

        c = chunks[0][2];
        rect = c.rect;
        assertEquals(Chunk.TYPE_HORIZONTAL | Chunk.TYPE_VERTICAL, c.type);
        assertEquals(72, rect.x);
        assertEquals(1, rect.y);
        assertEquals(1, rect.width);
        assertEquals(1, rect.height);

        // Row 2
        c = chunks[1][0];
        rect = c.rect;
        assertEquals(Chunk.TYPE_HORIZONTAL, c.type);
        assertEquals(1, rect.x);
        assertEquals(2, rect.y);
        assertEquals(1, rect.width);
        assertEquals(48, rect.height);

        c = chunks[1][1];
        rect = c.rect;
        assertEquals(Chunk.TYPE_FIXED, c.type);
        assertEquals(2, rect.x);
        assertEquals(2, rect.y);
        assertEquals(70, rect.width);
        assertEquals(48, rect.height);

        c = chunks[1][2];
        rect = c.rect;
        assertEquals(Chunk.TYPE_HORIZONTAL, c.type);
        assertEquals(72, rect.x);
        assertEquals(2, rect.y);
        assertEquals(1, rect.width);
        assertEquals(48, rect.height);

        // Row 3
        c = chunks[2][0];
        rect = c.rect;
        assertEquals(Chunk.TYPE_HORIZONTAL | Chunk.TYPE_VERTICAL, c.type);
        assertEquals(1, rect.x);
        assertEquals(50, rect.y);
        assertEquals(1, rect.width);
        assertEquals(1, rect.height);

        c = chunks[2][1];
        rect = c.rect;
        assertEquals(Chunk.TYPE_VERTICAL, c.type);
        assertEquals(2, rect.x);
        assertEquals(50, rect.y);
        assertEquals(70, rect.width);
        assertEquals(1, rect.height);

        c = chunks[2][2];
        rect = c.rect;
        assertEquals(Chunk.TYPE_HORIZONTAL | Chunk.TYPE_VERTICAL, c.type);
        assertEquals(72, rect.x);
        assertEquals(50, rect.y);
        assertEquals(1, rect.width);
        assertEquals(1, rect.height);
    }

    public void test9Patch2() throws Exception {
        String fileName = DIR + "patched2.9.png";
        NinePatchedImage image = new NinePatchedImage(getClass()
                .getResourceAsStream(fileName), fileName);
        assertNotNull(image);

        assertTrue(image.hasNinePatchExtension());
        assertTrue(image.ensure9Patch());

        // patches
        List<Tick> horizontalPatches = image.getHorizontalPatches();
        List<Tick> verticalPatches = image.getVerticalPatches();
        assertEquals(5, horizontalPatches.size());
        assertEquals(7, verticalPatches.size());

        NinePatchedImage.Chunk[][] chunks = null;
        chunks = image.getChunks(chunks);

        // vertical chunk size
        assertEquals(7, chunks.length);

        // horizontal chunk size
        for (int i = 0; i < chunks.length; i++) {
            assertEquals(5, chunks[i].length);
        }

        NinePatchedImage.Chunk c = null;
        Rectangle rect = null;

        // Row 1
        c = chunks[0][0];
        rect = c.rect;
        assertEquals(Chunk.TYPE_HORIZONTAL | Chunk.TYPE_VERTICAL, c.type);
        assertEquals(1, rect.x);
        assertEquals(1, rect.y);
        assertEquals(1, rect.width);
        assertEquals(1, rect.height);

        c = chunks[0][1];
        rect = c.rect;
        assertEquals(Chunk.TYPE_VERTICAL, c.type);
        assertEquals(2, rect.x);
        assertEquals(1, rect.y);
        assertEquals(34, rect.width);
        assertEquals(1, rect.height);

        c = chunks[0][2];
        rect = c.rect;
        assertEquals(Chunk.TYPE_HORIZONTAL | Chunk.TYPE_VERTICAL, c.type);
        assertEquals(36, rect.x);
        assertEquals(1, rect.y);
        assertEquals(1, rect.width);
        assertEquals(1, rect.height);

        c = chunks[0][3];
        rect = c.rect;
        assertEquals(Chunk.TYPE_VERTICAL, c.type);
        assertEquals(37, rect.x);
        assertEquals(1, rect.y);
        assertEquals(35, rect.width);
        assertEquals(1, rect.height);

        c = chunks[0][4];
        rect = c.rect;
        assertEquals(Chunk.TYPE_HORIZONTAL | Chunk.TYPE_VERTICAL, c.type);
        assertEquals(72, rect.x);
        assertEquals(1, rect.y);
        assertEquals(1, rect.width);
        assertEquals(1, rect.height);

        // Row 2
        c = chunks[1][0];
        rect = c.rect;
        assertEquals(Chunk.TYPE_HORIZONTAL, c.type);
        assertEquals(1, rect.x);
        assertEquals(2, rect.y);
        assertEquals(1, rect.width);
        assertEquals(7, rect.height);

        c = chunks[1][1];
        rect = c.rect;
        assertEquals(Chunk.TYPE_FIXED, c.type);
        assertEquals(2, rect.x);
        assertEquals(2, rect.y);
        assertEquals(34, rect.width);
        assertEquals(7, rect.height);

        c = chunks[1][2];
        rect = c.rect;
        assertEquals(Chunk.TYPE_HORIZONTAL, c.type);
        assertEquals(36, rect.x);
        assertEquals(2, rect.y);
        assertEquals(1, rect.width);
        assertEquals(7, rect.height);

        c = chunks[1][3];
        rect = c.rect;
        assertEquals(Chunk.TYPE_FIXED, c.type);
        assertEquals(37, rect.x);
        assertEquals(2, rect.y);
        assertEquals(35, rect.width);
        assertEquals(7, rect.height);

        c = chunks[1][4];
        rect = c.rect;
        assertEquals(Chunk.TYPE_HORIZONTAL, c.type);
        assertEquals(72, rect.x);
        assertEquals(2, rect.y);
        assertEquals(1, rect.width);
        assertEquals(7, rect.height);

        // Row 3
        c = chunks[2][0];
        rect = c.rect;
        assertEquals(Chunk.TYPE_HORIZONTAL | Chunk.TYPE_VERTICAL, c.type);
        assertEquals(1, rect.x);
        assertEquals(9, rect.y);
        assertEquals(1, rect.width);
        assertEquals(4, rect.height);

        c = chunks[2][1];
        rect = c.rect;
        assertEquals(Chunk.TYPE_VERTICAL, c.type);
        assertEquals(2, rect.x);
        assertEquals(9, rect.y);
        assertEquals(34, rect.width);
        assertEquals(4, rect.height);

        c = chunks[2][2];
        rect = c.rect;
        assertEquals(Chunk.TYPE_HORIZONTAL | Chunk.TYPE_VERTICAL, c.type);
        assertEquals(36, rect.x);
        assertEquals(9, rect.y);
        assertEquals(1, rect.width);
        assertEquals(4, rect.height);

        c = chunks[2][3];
        rect = c.rect;
        assertEquals(Chunk.TYPE_VERTICAL, c.type);
        assertEquals(37, rect.x);
        assertEquals(9, rect.y);
        assertEquals(35, rect.width);
        assertEquals(4, rect.height);

        c = chunks[2][4];
        rect = c.rect;
        assertEquals(Chunk.TYPE_HORIZONTAL | Chunk.TYPE_VERTICAL, c.type);
        assertEquals(72, rect.x);
        assertEquals(9, rect.y);
        assertEquals(1, rect.width);
        assertEquals(4, rect.height);

        // Row 4
        c = chunks[3][0];
        rect = c.rect;
        assertEquals(Chunk.TYPE_HORIZONTAL, c.type);
        assertEquals(1, rect.x);
        assertEquals(13, rect.y);
        assertEquals(1, rect.width);
        assertEquals(13, rect.height);

        c = chunks[3][1];
        rect = c.rect;
        assertEquals(Chunk.TYPE_FIXED, c.type);
        assertEquals(2, rect.x);
        assertEquals(13, rect.y);
        assertEquals(34, rect.width);
        assertEquals(13, rect.height);

        c = chunks[3][2];
        rect = c.rect;
        assertEquals(Chunk.TYPE_HORIZONTAL, c.type);
        assertEquals(36, rect.x);
        assertEquals(13, rect.y);
        assertEquals(1, rect.width);
        assertEquals(13, rect.height);

        c = chunks[3][3];
        rect = c.rect;
        assertEquals(Chunk.TYPE_FIXED, c.type);
        assertEquals(37, rect.x);
        assertEquals(13, rect.y);
        assertEquals(35, rect.width);
        assertEquals(13, rect.height);

        c = chunks[3][4];
        rect = c.rect;
        assertEquals(Chunk.TYPE_HORIZONTAL, c.type);
        assertEquals(72, rect.x);
        assertEquals(13, rect.y);
        assertEquals(1, rect.width);
        assertEquals(13, rect.height);

        // Row 5
        c = chunks[4][0];
        rect = c.rect;
        assertEquals(Chunk.TYPE_HORIZONTAL | Chunk.TYPE_VERTICAL, c.type);
        assertEquals(1, rect.x);
        assertEquals(26, rect.y);
        assertEquals(1, rect.width);
        assertEquals(12, rect.height);

        c = chunks[4][1];
        rect = c.rect;
        assertEquals(Chunk.TYPE_VERTICAL, c.type);
        assertEquals(2, rect.x);
        assertEquals(26, rect.y);
        assertEquals(34, rect.width);
        assertEquals(12, rect.height);

        c = chunks[4][2];
        rect = c.rect;
        assertEquals(Chunk.TYPE_HORIZONTAL | Chunk.TYPE_VERTICAL, c.type);
        assertEquals(36, rect.x);
        assertEquals(26, rect.y);
        assertEquals(1, rect.width);
        assertEquals(12, rect.height);

        c = chunks[4][3];
        rect = c.rect;
        assertEquals(Chunk.TYPE_VERTICAL, c.type);
        assertEquals(37, rect.x);
        assertEquals(26, rect.y);
        assertEquals(35, rect.width);
        assertEquals(12, rect.height);

        c = chunks[4][4];
        rect = c.rect;
        assertEquals(Chunk.TYPE_HORIZONTAL | Chunk.TYPE_VERTICAL, c.type);
        assertEquals(72, rect.x);
        assertEquals(26, rect.y);
        assertEquals(1, rect.width);
        assertEquals(12, rect.height);

        // Row 6
        c = chunks[5][0];
        rect = c.rect;
        assertEquals(Chunk.TYPE_HORIZONTAL, c.type);
        assertEquals(1, rect.x);
        assertEquals(38, rect.y);
        assertEquals(1, rect.width);
        assertEquals(12, rect.height);

        c = chunks[5][1];
        rect = c.rect;
        assertEquals(Chunk.TYPE_FIXED, c.type);
        assertEquals(2, rect.x);
        assertEquals(38, rect.y);
        assertEquals(34, rect.width);
        assertEquals(12, rect.height);

        c = chunks[5][2];
        rect = c.rect;
        assertEquals(Chunk.TYPE_HORIZONTAL, c.type);
        assertEquals(36, rect.x);
        assertEquals(38, rect.y);
        assertEquals(1, rect.width);
        assertEquals(12, rect.height);

        c = chunks[5][3];
        rect = c.rect;
        assertEquals(Chunk.TYPE_FIXED, c.type);
        assertEquals(37, rect.x);
        assertEquals(38, rect.y);
        assertEquals(35, rect.width);
        assertEquals(12, rect.height);

        c = chunks[5][4];
        rect = c.rect;
        assertEquals(Chunk.TYPE_HORIZONTAL, c.type);
        assertEquals(72, rect.x);
        assertEquals(38, rect.y);
        assertEquals(1, rect.width);
        assertEquals(12, rect.height);

        // Row 7
        c = chunks[6][0];
        rect = c.rect;
        assertEquals(Chunk.TYPE_HORIZONTAL | Chunk.TYPE_VERTICAL, c.type);
        assertEquals(1, rect.x);
        assertEquals(50, rect.y);
        assertEquals(1, rect.width);
        assertEquals(1, rect.height);

        c = chunks[6][1];
        rect = c.rect;
        assertEquals(Chunk.TYPE_VERTICAL, c.type);
        assertEquals(2, rect.x);
        assertEquals(50, rect.y);
        assertEquals(34, rect.width);
        assertEquals(1, rect.height);

        c = chunks[6][2];
        rect = c.rect;
        assertEquals(Chunk.TYPE_HORIZONTAL | Chunk.TYPE_VERTICAL, c.type);
        assertEquals(36, rect.x);
        assertEquals(50, rect.y);
        assertEquals(1, rect.width);
        assertEquals(1, rect.height);

        c = chunks[6][3];
        rect = c.rect;
        assertEquals(Chunk.TYPE_VERTICAL, c.type);
        assertEquals(37, rect.x);
        assertEquals(50, rect.y);
        assertEquals(35, rect.width);
        assertEquals(1, rect.height);

        c = chunks[6][4];
        rect = c.rect;
        assertEquals(Chunk.TYPE_HORIZONTAL | Chunk.TYPE_VERTICAL, c.type);
        assertEquals(72, rect.x);
        assertEquals(50, rect.y);
        assertEquals(1, rect.width);
        assertEquals(1, rect.height);
    }

    public void testContentArea() throws Exception {
        String fileName = DIR + "content-area.9.png";
        NinePatchedImage image = new NinePatchedImage(getClass()
                .getResourceAsStream(fileName), fileName);
        assertNotNull(image);

        assertTrue(image.hasNinePatchExtension());
        assertTrue(image.ensure9Patch());

        // contents area
        List<Tick> horizontalContentsArea = image.getHorizontalContents();
        List<Tick> verticalContentsArea = image.getVerticalContents();
        assertEquals(3, horizontalContentsArea.size());
        assertEquals(3, verticalContentsArea.size());

        // content area rectangle
        Rectangle contentsArea = image.getContentArea();
        assertEquals(new Rectangle(19, 13, 35, 25), contentsArea);
    }

    public void testContentAreaOneDot() throws Exception {
        String fileName = DIR + "content-area-one-dot.9.png";
        NinePatchedImage image = new NinePatchedImage(getClass()
                .getResourceAsStream(fileName), fileName);
        assertNotNull(image);

        assertTrue(image.hasNinePatchExtension());
        assertTrue(image.ensure9Patch());

        // contents area
        List<Tick> horizontalContentsArea = image.getHorizontalContents();
        List<Tick> verticalContentsArea = image.getVerticalContents();
        assertEquals(3, horizontalContentsArea.size());
        assertEquals(3, verticalContentsArea.size());

        // content area rectangle
        Rectangle contentsArea = image.getContentArea();
        assertEquals(new Rectangle(19, 13, 1, 1), contentsArea);
    }

    public void testContentAreaTwoDots() throws Exception {
        String fileName = DIR + "content-area-two-dots.9.png";
        NinePatchedImage image = new NinePatchedImage(getClass()
                .getResourceAsStream(fileName), fileName);
        assertNotNull(image);

        assertTrue(image.hasNinePatchExtension());
        assertTrue(image.ensure9Patch());

        // contents area
        List<Tick> horizontalContentsArea = image.getHorizontalContents();
        List<Tick> verticalContentsArea = image.getVerticalContents();
        assertEquals(5, horizontalContentsArea.size());
        assertEquals(5, verticalContentsArea.size());

        // content area rectangle
        Rectangle contentsArea = image.getContentArea();
        assertEquals(new Rectangle(19, 13, 35, 25), contentsArea);

        String fileName2 = DIR + "content-area.9.png";
        NinePatchedImage image2 = new NinePatchedImage(getClass()
                .getResourceAsStream(fileName2), fileName2);
        assertNotNull(image2);

        assertTrue(image2.hasNinePatchExtension());
        assertTrue(image2.ensure9Patch());

        // content area rectangle
        Rectangle contentsArea2 = image2.getContentArea();
        assertEquals(contentsArea2, contentsArea);
    }

    public void testBadPatches() throws Exception {
        String fileName = DIR + "patched-with-badpatches.9.png";
        NinePatchedImage image = new NinePatchedImage(getClass()
                .getResourceAsStream(fileName), fileName);
        assertNotNull(image);

        assertTrue(image.hasNinePatchExtension());
        assertTrue(image.ensure9Patch());

        Chunk[][] chunks = null;
        chunks = image.getChunks(chunks);

        // vertical chunk size
        assertEquals(5, chunks.length);

        // horizontal chunk size
        for (int i = 0; i < chunks.length; i++) {
            assertEquals(7, chunks[i].length);
        }

        chunks = image.getCorruptedChunks(chunks);

        Chunk c = null;

        // collect bad patches
        List<Point> badPatches = new ArrayList<Point>(5 * 7);
        for (int y = 0; y < chunks.length; y++) {
            for (int x = 0; x < chunks[0].length; x++) {
                c = chunks[y][x];
                if ((c.type & Chunk.TYPE_CORRUPT) != 0x0) {
                    badPatches.add(new Point(y, x));
                }
            }
        }

        assertEquals(15, badPatches.size());

        assertTrue(badPatches.contains(new Point(0, 3)));

        assertTrue(badPatches.contains(new Point(1, 1)));
        assertTrue(badPatches.contains(new Point(1, 2)));
        assertTrue(badPatches.contains(new Point(1, 3)));
        assertTrue(badPatches.contains(new Point(1, 4)));
        assertTrue(badPatches.contains(new Point(1, 5)));

        assertTrue(badPatches.contains(new Point(2, 1)));
        assertTrue(badPatches.contains(new Point(2, 5)));

        assertTrue(badPatches.contains(new Point(3, 0)));
        assertTrue(badPatches.contains(new Point(3, 1)));
        assertTrue(badPatches.contains(new Point(3, 5)));
        assertTrue(badPatches.contains(new Point(3, 6)));

        assertTrue(badPatches.contains(new Point(4, 1)));
        assertTrue(badPatches.contains(new Point(4, 3)));
        assertTrue(badPatches.contains(new Point(4, 5)));
    }

    public void testProjection() throws Exception {
        // top
        String fileName = DIR + "patched3.9.png";
        NinePatchedImage image = new NinePatchedImage(getClass()
                .getResourceAsStream(fileName), fileName);
        assertNotNull(image);

        assertTrue(image.hasNinePatchExtension());
        assertTrue(image.ensure9Patch());

        ImageData data = image.getImageData();
        assertEquals(72 + 2, data.width);
        assertEquals(50 + 2, data.height);

        int width = 72 * 2;
        int height = 50 * 2;

        Chunk[][] chunks = null;
        chunks = image.getChunks(chunks);

        Projection[][] projections = null;
        projections = image.getProjections(width, height, projections);

        assertEquals(chunks.length, projections.length);
        for (int i = 0; i < chunks.length; i++) {
            assertEquals(chunks[i].length, projections[i].length);
        }

        for (int y = 0; y < projections.length; y++) {
            for (int x = 0; x < projections[y].length; x++) {
                assertEquals(projections[y][x].src, chunks[y][x].rect);

                // If chunk type is FIXED. Same projection size as original
                // chunk.
                if (projections[y][x].chunk.type == Chunk.TYPE_FIXED) {
                    assertEquals(projections[y][x].dest.width, chunks[y][x].rect.width);
                    assertEquals(projections[y][x].dest.height, chunks[y][x].rect.height);
                }
            }
        }

        Projection p = null;
        Rectangle rect = null;

        // Check start position
        p = projections[0][0];

        // src position start from 1, 9-patch row and column included.
        assertEquals(1, p.src.x);
        assertEquals(1, p.src.y);

        // dest position start from 0, 9-patch row and column ignored.
        assertEquals(0, p.dest.x);
        assertEquals(0, p.dest.y);

        // row 1
        p = projections[0][0];
        rect = p.dest;
        assertEquals(0, rect.x);
        assertEquals(0, rect.y);
        assertEquals(74, rect.width);
        assertEquals(5, rect.height);

        p = projections[0][1];
        rect = p.dest;
        assertEquals(74, rect.x);
        assertEquals(0, rect.y);
        assertEquals(62, rect.width);
        assertEquals(5, rect.height);

        p = projections[0][2];
        rect = p.dest;
        assertEquals(136, rect.x);
        assertEquals(0, rect.y);
        assertEquals(8, rect.width);
        assertEquals(5, rect.height);

        // row 2
        p = projections[1][0];
        rect = p.dest;
        assertEquals(0, rect.x);
        assertEquals(5, rect.y);
        assertEquals(74, rect.width);
        assertEquals(24, rect.height);

        p = projections[1][1];
        rect = p.dest;
        assertEquals(74, rect.x);
        assertEquals(5, rect.y);
        assertEquals(62, rect.width);
        assertEquals(24, rect.height);

        p = projections[1][2];
        rect = p.dest;
        assertEquals(136, rect.x);
        assertEquals(5, rect.y);
        assertEquals(8, rect.width);
        assertEquals(24, rect.height);

        // row 3
        p = projections[2][0];
        rect = p.dest;
        assertEquals(0, rect.x);
        assertEquals(29, rect.y);
        assertEquals(74, rect.width);
        assertEquals(58, rect.height);

        p = projections[2][1];
        rect = p.dest;
        assertEquals(74, rect.x);
        assertEquals(29, rect.y);
        assertEquals(62, rect.width);
        assertEquals(58, rect.height);

        p = projections[2][2];
        rect = p.dest;
        assertEquals(136, rect.x);
        assertEquals(29, rect.y);
        assertEquals(8, rect.width);
        assertEquals(58, rect.height);

        // row 4
        p = projections[3][0];
        rect = p.dest;
        assertEquals(0, rect.x);
        assertEquals(87, rect.y);
        assertEquals(74, rect.width);
        assertEquals(13, rect.height);

        p = projections[3][1];
        rect = p.dest;
        assertEquals(74, rect.x);
        assertEquals(87, rect.y);
        assertEquals(62, rect.width);
        assertEquals(13, rect.height);

        p = projections[3][2];
        rect = p.dest;
        assertEquals(136, rect.x);
        assertEquals(87, rect.y);
        assertEquals(8, rect.width);
        assertEquals(13, rect.height);
    }

    public void testReadLayoutBoundsOnlyImage() throws Exception {
        String fileName = DIR + "layout-bounds-only.9.png";
        NinePatchedImage image = new NinePatchedImage(getClass()
                .getResourceAsStream(fileName), fileName);
        assertNotNull(image);

        ImageData data = image.getImageData();
        int width = data.width;
        int height = data.height;

        assertEquals(74, width);
        assertEquals(52, height);

        assertTrue(image.hasNinePatchExtension());
        assertTrue(image.ensure9Patch());
    }
}

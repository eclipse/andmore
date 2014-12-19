/*
 * Copyright (C) 2012 The Android Open Source Project
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

import com.android.annotations.Nullable;
import com.android.ide.common.api.Rect;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

/**
 * This class implements 2D bin packing: packing rectangles into a given area as
 * tightly as "possible" (bin packing in general is NP hard, so this class uses
 * heuristics).
 * <p>
 * The algorithm implemented is to keep a set of (possibly overlapping)
 * available areas for placement. For each newly inserted rectangle, we first
 * pick which available space to occupy, and we then subdivide the
 * current rectangle into all the possible remaining unoccupied sub-rectangles.
 * We also remove any other space rectangles which are no longer eligible if
 * they are intersecting the newly placed rectangle.
 * <p>
 * This algorithm is not very fast, so should not be used for a large number of
 * rectangles.
 */
class BinPacker {
    /**
     * When enabled, the successive passes are dumped as PNG images showing the
     * various available and occupied rectangles)
     */
    private static final boolean DEBUG = false;

    private final List<Rect> mSpace = new ArrayList<Rect>();
    private final int mMinHeight;
    private final int mMinWidth;

    /**
     * Creates a new {@linkplain BinPacker}. To use it, first add one or more
     * initial available space rectangles with {@link #addSpace(Rect)}, and then
     * place the rectangles with {@link #occupy(int, int)}. The returned
     * {@link Rect} from {@link #occupy(int, int)} gives the coordinates of the
     * positioned rectangle.
     *
     * @param minWidth the smallest width of any rectangle placed into this bin
     * @param minHeight the smallest height of any rectangle placed into this bin
     */
    BinPacker(int minWidth, int minHeight) {
        mMinWidth = minWidth;
        mMinHeight = minHeight;

        if (DEBUG) {
            mAllocated = new ArrayList<Rect>();
            sLayoutId++;
            sRectId = 1;
        }
    }

    /** Adds more available space */
    void addSpace(Rect rect) {
        if (rect.w >= mMinWidth && rect.h >= mMinHeight) {
            mSpace.add(rect);
        }
    }

    /** Attempts to place a rectangle of the given dimensions, if possible */
    @Nullable
    Rect occupy(int width, int height) {
        int index = findBest(width, height);
        if (index == -1) {
            return null;
        }

        return split(index, width, height);
    }

    /**
     * Finds the best available space rectangle to position a new rectangle of
     * the given size in.
     */
    private int findBest(int width, int height) {
        if (mSpace.isEmpty()) {
            return -1;
        }

        // Try to pack as far up as possible first
        int bestIndex = -1;
        boolean multipleAtSameY = false;
        int minY = Integer.MAX_VALUE;
        for (int i = 0, n = mSpace.size(); i < n; i++) {
            Rect rect = mSpace.get(i);
            if (rect.y <= minY) {
                if (rect.w >= width && rect.h >= height) {
                    if (rect.y < minY) {
                        minY = rect.y;
                        multipleAtSameY = false;
                        bestIndex = i;
                    } else if (minY == rect.y) {
                        multipleAtSameY = true;
                    }
                }
            }
        }

        if (!multipleAtSameY) {
            return bestIndex;
        }

        bestIndex = -1;

        // Pick a rectangle. This currently tries to find the rectangle whose shortest
        // side most closely matches the placed rectangle's size.
        // Attempt to find the best short side fit
        int bestShortDistance = Integer.MAX_VALUE;
        int bestLongDistance = Integer.MAX_VALUE;

        for (int i = 0, n = mSpace.size(); i < n; i++) {
            Rect rect = mSpace.get(i);
            if (rect.y != minY) { // Only comparing elements at same y
                continue;
            }
            if (rect.w >= width && rect.h >= height) {
                if (width < height) {
                    int distance = rect.w - width;
                    if (distance < bestShortDistance ||
                            distance == bestShortDistance &&
                            (rect.h - height) < bestLongDistance) {
                        bestShortDistance = distance;
                        bestLongDistance = rect.h - height;
                        bestIndex = i;
                    }
                } else {
                    int distance = rect.w - width;
                    if (distance < bestShortDistance ||
                            distance == bestShortDistance &&
                            (rect.h - height) < bestLongDistance) {
                        bestShortDistance = distance;
                        bestLongDistance = rect.h - height;
                        bestIndex = i;
                    }
                }
            }
        }

        return bestIndex;
    }

    /**
     * Removes the rectangle at the given index. Since the rectangles are in an
     * {@link ArrayList}, removing a rectangle in the normal way is slow (it
     * would involve shifting all elements), but since we don't care about
     * order, this always swaps the to-be-deleted element to the last position
     * in the array first, <b>then</b> it deletes it (which should be
     * immediate).
     *
     * @param index the index in the {@link #mSpace} list to remove a rectangle
     *            from
     */
    private void removeRect(int index) {
        assert !mSpace.isEmpty();
        int lastIndex = mSpace.size() - 1;
        if (index != lastIndex) {
            // Swap before remove to make deletion faster since we don't
            // care about order
            Rect temp = mSpace.get(index);
            mSpace.set(index, mSpace.get(lastIndex));
            mSpace.set(lastIndex, temp);
        }

        mSpace.remove(lastIndex);
    }

    /**
     * Splits the rectangle at the given rectangle index such that it can contain
     * a rectangle of the given width and height. */
    private Rect split(int index, int width, int height) {
        Rect rect = mSpace.get(index);
        assert rect.w >= width && rect.h >= height : rect;

        Rect r = new Rect(rect);
        r.w = width;
        r.h = height;

        // Remove all rectangles that intersect my rectangle
        for (int i = 0; i < mSpace.size(); i++) {
            Rect other = mSpace.get(i);
            if (other.intersects(r)) {
                removeRect(i);
                i--;
            }
        }


        // Split along vertical line x = rect.x + width:
        // (rect.x,rect.y)
        //     +-------------+-------------------------+
        //     |             |                         |
        //     |             |                         |
        //     |             | height                  |
        //     |             |                         |
        //     |             |                         |
        //     +-------------+           B             | rect.h
        //     |   width                               |
        //     |             |                         |
        //     |      A                                |
        //     |             |                         |
        //     |                                       |
        //     +---------------------------------------+
        //                    rect.w
        int remainingHeight = rect.h - height;
        int remainingWidth = rect.w - width;
        if (remainingHeight >= mMinHeight) {
            mSpace.add(new Rect(rect.x, rect.y + height, width, remainingHeight));
        }
        if (remainingWidth >= mMinWidth) {
            mSpace.add(new Rect(rect.x + width, rect.y, remainingWidth, rect.h));
        }

        // Split along horizontal line y = rect.y + height:
        //     +-------------+-------------------------+
        //     |             |                         |
        //     |             | height                  |
        //     |             |          A              |
        //     |             |                         |
        //     |             |                         | rect.h
        //     +-------------+ - - - - - - - - - - - - |
        //     |  width                                |
        //     |                                       |
        //     |                B                      |
        //     |                                       |
        //     |                                       |
        //     +---------------------------------------+
        //                   rect.w
        if (remainingHeight >= mMinHeight) {
            mSpace.add(new Rect(rect.x, rect.y + height, rect.w, remainingHeight));
        }
        if (remainingWidth >= mMinWidth) {
            mSpace.add(new Rect(rect.x + width, rect.y, remainingWidth, height));
        }

        // Remove redundant rectangles. This is not very efficient.
        for (int i = 0; i < mSpace.size() - 1; i++) {
            for (int j = i + 1; j < mSpace.size(); j++) {
                Rect iRect = mSpace.get(i);
                Rect jRect = mSpace.get(j);
                if (jRect.contains(iRect)) {
                    removeRect(i);
                    i--;
                    break;
                }
                if (iRect.contains(jRect)) {
                    removeRect(j);
                    j--;
                }
            }
        }

        if (DEBUG) {
            mAllocated.add(r);
            dumpImage();
        }

        return r;
    }

    // DEBUGGING CODE: Enable with DEBUG

    private List<Rect> mAllocated;
    private static int sLayoutId;
    private static int sRectId;
    private void dumpImage() {
        if (DEBUG) {
            int width = 100;
            int height = 100;
            for (Rect rect : mSpace) {
                width = Math.max(width, rect.w);
                height = Math.max(height, rect.h);
            }
            width += 10;
            height += 10;

            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, image.getWidth(), image.getHeight());

            Color[] colors = new Color[] {
                    Color.blue, Color.cyan,
                    Color.green, Color.magenta, Color.orange,
                    Color.pink, Color.red, Color.white, Color.yellow, Color.darkGray,
                    Color.lightGray, Color.gray,
            };

            char allocated = 'A';
            for (Rect rect : mAllocated) {
                Color color = new Color(0x9FFFFFFF, true);
                g.setColor(color);
                g.setBackground(color);
                g.fillRect(rect.x, rect.y, rect.w, rect.h);
                g.setColor(Color.WHITE);
                g.drawRect(rect.x, rect.y, rect.w, rect.h);
                g.drawString("" + (allocated++),
                        rect.x + rect.w / 2, rect.y + rect.h / 2);
            }

            int colorIndex = 0;
            for (Rect rect : mSpace) {
                Color color = colors[colorIndex];
                colorIndex = (colorIndex + 1) % colors.length;

                color = new Color(color.getRed(), color.getGreen(), color.getBlue(), 128);
                g.setColor(color);

                g.fillRect(rect.x, rect.y, rect.w, rect.h);
                g.setColor(Color.WHITE);
                g.drawString(Integer.toString(colorIndex),
                        rect.x + rect.w / 2, rect.y + rect.h / 2);
            }


            g.dispose();

            File file = new File("/tmp/layout" + sLayoutId + "_pass" + sRectId + ".png");
            try {
                ImageIO.write(image, "PNG", file);
                System.out.println("Wrote diagnostics image " + file);
            } catch (IOException e) {
                e.printStackTrace();
            }
            sRectId++;
        }
    }
}
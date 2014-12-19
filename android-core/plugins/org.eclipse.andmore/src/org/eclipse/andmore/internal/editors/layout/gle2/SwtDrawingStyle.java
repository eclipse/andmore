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

import com.android.ide.common.api.DrawingStyle;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;

/**
 * Description of the drawing styles with specific color, line style and alpha
 * definitions. This class corresponds to the more generic {@link DrawingStyle}
 * class which defines the drawing styles but does not introduce any specific
 * SWT values to the API clients.
 * <p>
 * TODO: This class should eventually be replaced by a scheme where the color
 * constants are instead coming from the theme.
 */
public enum SwtDrawingStyle {
    /**
     * The style definition corresponding to {@link DrawingStyle#SELECTION}
     */
    SELECTION(new RGB(0x00, 0x99, 0xFF), 192, new RGB(0x00, 0x99, 0xFF), 192, 1, SWT.LINE_SOLID),

    /**
     * The style definition corresponding to {@link DrawingStyle#GUIDELINE}
     */
    GUIDELINE(new RGB(0x00, 0xAA, 0x00), 192, SWT.LINE_SOLID),

    /**
     * The style definition corresponding to {@link DrawingStyle#GUIDELINE}
     */
    GUIDELINE_SHADOW(new RGB(0x00, 0xAA, 0x00), 192, SWT.LINE_SOLID),

    /**
     * The style definition corresponding to {@link DrawingStyle#GUIDELINE_DASHED}
     */
    GUIDELINE_DASHED(new RGB(0x00, 0xAA, 0x00), 192, SWT.LINE_CUSTOM),

    /**
     * The style definition corresponding to {@link DrawingStyle#DISTANCE}
     */
    DISTANCE(new RGB(0xFF, 0x00, 0x00), 192 - 32, SWT.LINE_SOLID),

    /**
     * The style definition corresponding to {@link DrawingStyle#GRID}
     */
    GRID(new RGB(0xAA, 0xAA, 0xAA), 128, SWT.LINE_SOLID),

    /**
     * The style definition corresponding to {@link DrawingStyle#HOVER}
     */
    HOVER(null, 0, new RGB(0xFF, 0xFF, 0xFF), 40, 1, SWT.LINE_DOT),

    /**
     * The style definition corresponding to {@link DrawingStyle#HOVER}
     */
    HOVER_SELECTION(null, 0, new RGB(0xFF, 0xFF, 0xFF), 10, 1, SWT.LINE_DOT),

    /**
     * The style definition corresponding to {@link DrawingStyle#ANCHOR}
     */
    ANCHOR(new RGB(0x00, 0x99, 0xFF), 96, SWT.LINE_SOLID),

    /**
     * The style definition corresponding to {@link DrawingStyle#OUTLINE}
     */
    OUTLINE(new RGB(0x88, 0xFF, 0x88), 160, SWT.LINE_SOLID),

    /**
     * The style definition corresponding to {@link DrawingStyle#DROP_RECIPIENT}
     */
    DROP_RECIPIENT(new RGB(0xFF, 0x99, 0x00), 255, new RGB(0xFF, 0x99, 0x00), 160, 2,
            SWT.LINE_SOLID),

    /**
     * The style definition corresponding to {@link DrawingStyle#DROP_ZONE}
     */
    DROP_ZONE(new RGB(0x00, 0xAA, 0x00), 220, new RGB(0x55, 0xAA, 0x00), 200, 1, SWT.LINE_SOLID),

    /**
     * The style definition corresponding to
     * {@link DrawingStyle#DROP_ZONE_ACTIVE}
     */
    DROP_ZONE_ACTIVE(new RGB(0x00, 0xAA, 0x00), 220, new RGB(0x00, 0xAA, 0x00), 128, 2,
            SWT.LINE_SOLID),

    /**
     * The style definition corresponding to {@link DrawingStyle#DROP_PREVIEW}
     */
    DROP_PREVIEW(new RGB(0xFF, 0x99, 0x00), 255, null, 0, 2, SWT.LINE_CUSTOM),

    /**
     * The style definition corresponding to {@link DrawingStyle#RESIZE_PREVIEW}
     */
    RESIZE_PREVIEW(new RGB(0xFF, 0x99, 0x00), 255, null, 0, 2, SWT.LINE_SOLID),

    /**
     * The style used to show a proposed resize bound which is being rejected (for example,
     * because there is no near edge to attach to in a RelativeLayout).
     */
    RESIZE_FAIL(new RGB(0xFF, 0x99, 0x00), 255, null, 0, 2, SWT.LINE_CUSTOM),

    /**
     * The style definition corresponding to {@link DrawingStyle#HELP}
     */
    HELP(new RGB(0xFF, 0xFF, 0xFF), 255, new RGB(0x00, 0x00, 0x00), 128, 1, SWT.LINE_SOLID),

    /**
     * The style definition corresponding to {@link DrawingStyle#INVALID}
     */
    INVALID(new RGB(0xFF, 0xFF, 0xFF), 255, new RGB(0xFF, 0x00, 0x00), 64, 2, SWT.LINE_SOLID),

    /**
     * The style definition corresponding to {@link DrawingStyle#DEPENDENCY}
     */
    DEPENDENCY(new RGB(0xFF, 0xFF, 0xFF), 255, new RGB(0xFF, 0xFF, 0x00), 24, 2, SWT.LINE_SOLID),

    /**
     * The style definition corresponding to {@link DrawingStyle#CYCLE}
     */
    CYCLE(new RGB(0xFF, 0x00, 0x00), 192, null, 0, 1, SWT.LINE_SOLID),

    /**
     * The style definition corresponding to {@link DrawingStyle#DRAGGED}
     */
    DRAGGED(new RGB(0xFF, 0xFF, 0xFF), 255, new RGB(0x00, 0xFF, 0x00), 16, 2, SWT.LINE_SOLID),

    /**
     * The style definition corresponding to {@link DrawingStyle#EMPTY}
     */
    EMPTY(new RGB(0xFF, 0xFF, 0x55), 255, new RGB(0xFF, 0xFF, 0x55), 255, 1, SWT.LINE_DASH),

    /**
     * The style definition corresponding to {@link DrawingStyle#CUSTOM1}
     */
    CUSTOM1(new RGB(0xFF, 0x00, 0xFF), 255, null, 0, 1, SWT.LINE_SOLID),

    /**
     * The style definition corresponding to {@link DrawingStyle#CUSTOM2}
     */
    CUSTOM2(new RGB(0x00, 0xFF, 0xFF), 255, null, 0, 1, SWT.LINE_DOT);

    /**
     * Construct a new style value with the given foreground, background, width,
     * linestyle and transparency.
     *
     * @param stroke A color descriptor for the foreground color, or null if no
     *            foreground color should be set
     * @param fill A color descriptor for the background color, or null if no
     *            foreground color should be set
     * @param lineWidth The line width, in pixels, or 0 if no line width should
     *            be set
     * @param lineStyle The SWT line style - such as {@link SWT#LINE_SOLID}.
     * @param strokeAlpha The alpha value of the stroke, an integer in the range 0 to 255
     *            where 0 is fully transparent and 255 is fully opaque.
     * @param fillAlpha The alpha value of the fill, an integer in the range 0 to 255
     *            where 0 is fully transparent and 255 is fully opaque.
     */
    private SwtDrawingStyle(RGB stroke, int strokeAlpha, RGB fill, int fillAlpha, int lineWidth,
            int lineStyle) {
        mStroke = stroke;
        mFill = fill;
        mLineWidth = lineWidth;
        mLineStyle = lineStyle;
        mStrokeAlpha = strokeAlpha;
        mFillAlpha = fillAlpha;
    }

    /**
     * Convenience constructor for typical drawing styles, which do not specify
     * a fill and use a standard thickness line
     *
     * @param stroke Stroke color to be used (e.g. for the border/foreground)
     * @param strokeAlpha Transparency to use for the stroke; 0 is transparent
     *            and 255 is fully opaque.
     * @param lineStyle The SWT line style - such as {@link SWT#LINE_SOLID}.
     */
    private SwtDrawingStyle(RGB stroke, int strokeAlpha, int lineStyle) {
        this(stroke, strokeAlpha, null, 255, 1, lineStyle);
    }

    /**
     * Return the stroke/foreground/border RGB color description to be used for
     * this style, or null if none
     */
    public RGB getStrokeColor() {
        return mStroke;
    }

    /**
     * Return the fill/background/interior RGB color description to be used for
     * this style, or null if none
     */
    public RGB getFillColor() {
        return mFill;
    }

    /** Return the line width to be used for this style */
    public int getLineWidth() {
        return mLineWidth;
    }

    /** Return the SWT line style to be used for this style */
    public int getLineStyle() {
        return mLineStyle;
    }

    /**
     * Return the stroke alpha value (in the range 0,255) to be used for this
     * style
     */
    public int getStrokeAlpha() {
        return mStrokeAlpha;
    }

    /**
     * Return the fill alpha value (in the range 0,255) to be used for this
     * style
     */
    public int getFillAlpha() {
        return mFillAlpha;
    }

    /**
     * Return the corresponding SwtDrawingStyle for the given
     * {@link DrawingStyle}
     * @param style The style to convert from a {@link DrawingStyle} to a {@link SwtDrawingStyle}.
     * @return A corresponding {@link SwtDrawingStyle}.
     */
    public static SwtDrawingStyle of(DrawingStyle style) {
        switch (style) {
            case SELECTION:
                return SELECTION;
            case GUIDELINE:
                return GUIDELINE;
            case GUIDELINE_SHADOW:
                return GUIDELINE_SHADOW;
            case GUIDELINE_DASHED:
                return GUIDELINE_DASHED;
            case DISTANCE:
                return DISTANCE;
            case GRID:
                return GRID;
            case HOVER:
                return HOVER;
            case HOVER_SELECTION:
                return HOVER_SELECTION;
            case ANCHOR:
                return ANCHOR;
            case OUTLINE:
                return OUTLINE;
            case DROP_ZONE:
                return DROP_ZONE;
            case DROP_ZONE_ACTIVE:
                return DROP_ZONE_ACTIVE;
            case DROP_RECIPIENT:
                return DROP_RECIPIENT;
            case DROP_PREVIEW:
                return DROP_PREVIEW;
            case RESIZE_PREVIEW:
                return RESIZE_PREVIEW;
            case RESIZE_FAIL:
                return RESIZE_FAIL;
            case HELP:
                return HELP;
            case INVALID:
                return INVALID;
            case DEPENDENCY:
                return DEPENDENCY;
            case CYCLE:
                return CYCLE;
            case DRAGGED:
                return DRAGGED;
            case EMPTY:
                return EMPTY;
            case CUSTOM1:
                return CUSTOM1;
            case CUSTOM2:
                return CUSTOM2;

                // Internal error
            default:
                throw new IllegalArgumentException("Unknown style " + style);
        }
    }

    /** RGB description of the stroke/foreground/border color */
    private final RGB mStroke;

    /** RGB description of the fill/foreground/interior color */
    private final RGB mFill;

    /** Pixel thickness of the stroke/border */
    private final int mLineWidth;

    /** SWT line style of the border/stroke */
    private final int mLineStyle;

    /** Alpha (in the range 0-255) of the stroke/border */
    private final int mStrokeAlpha;

    /** Alpha (in the range 0-255) of the fill/interior */
    private final int mFillAlpha;
}

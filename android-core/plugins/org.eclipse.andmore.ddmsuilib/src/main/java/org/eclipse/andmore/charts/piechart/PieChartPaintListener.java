/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - Renato Stoffalette Joao <rsjoao@br.ibm.com>
 *******************************************************************************/package org.eclipse.andmore.charts.piechart;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.swtchart.IBarSeries;
import org.swtchart.ISeries;
import org.swtchart.ITitle;
import org.swtchart.Range;

public class PieChartPaintListener implements PaintListener {

    private PieChart chart;
    private Control plotArea;
    private double[][] seriesValues;
    private String[] seriesNames;
    private static final int X_GAP = 10;

    private static final Color WHITE = Display.getDefault().getSystemColor(SWT.COLOR_WHITE);
    private static final Color BLACK = Display.getDefault().getSystemColor(SWT.COLOR_BLACK);
    private static final String FONT = "Arial"; //$NON-NLS-1$

    private Point[] pieCenters;
    private int[][] pieSliceAngles;
    private int pieWidth;

    private String origTitleText;

    /**
     * Handles drawing and updating of a PieChart, with titles given to its legend and
     * to each of its pies. Pies will be drawn in the given chart's plot area.
     * @param chart The PieChart to draw and update.
     * @since 2.0
     */
    public PieChartPaintListener(PieChart chart) {
        this.chart = chart;
        this.plotArea = chart.getPlotArea();
    }

    @Override
    public void paintControl(PaintEvent e) {
        GC gc = e.gc;
        Rectangle bounds;
        this.getPieSeriesArray();
        pieCenters = new Point[seriesValues.length];
        pieSliceAngles = new int[seriesValues.length][];
        if (seriesValues.length == 0) {
            bounds = gc.getClipping();
            Font oldFont = gc.getFont();
            Font font = new Font(Display.getDefault(), FONT, 15, SWT.BOLD);
            gc.setForeground(BLACK);
            gc.setFont(font);
            String text = "No data"; //$NON-NLS-1$
            Point textSize = e.gc.textExtent(text);
            gc.drawText(text, (bounds.width - textSize.x) / 2, (bounds.height - textSize.y) / 2);
            gc.setFont(oldFont);
            font.dispose();
            return;
        }
        bounds = plotArea.getBounds();
        // Adjust the title so it centers in the plot area
        if (origTitleText == null) {
            origTitleText = chart.getTitle().getText();
        }

        // We want to center the title in the plot area rather than the entire view which includes
        // the legend.  To force this, we have two algorithms depending on what level of the
        // underlying SWT Chart software we are using.  If the title is an SWT Control, we simply
        // set the title's location manually.  If the title is just a PaintListener, we center it
        // by adding a number of trailing spaces which we calculate.
        if (chart.getTitle() instanceof Control) {
            setTitleBounds(bounds);
        } else {
            adjustTitle(e);
        }
        int width = bounds.width / seriesValues.length;
        int x = bounds.x;

        if (chart.getLegend().isVisible()) {
            Rectangle legendBounds = ((Control) chart.getLegend()).getBounds();
            Font oldFont = gc.getFont();
            Font font = new Font(Display.getDefault(), FONT, 10, SWT.BOLD);
            gc.setForeground(BLACK);
            gc.setFont(font);
            String text = chart.getAxisSet().getXAxis(0).getTitle().getText();
            Point textSize = e.gc.textExtent(text);
            gc.drawText(text, legendBounds.x + (legendBounds.width - textSize.x) / 2, legendBounds.y - textSize.y);
            gc.setFont(oldFont);
            font.dispose();
        }

        pieWidth = Math.min(width - X_GAP, bounds.height);
        for (int i = 0; i < seriesValues.length; i++) {
            drawPieChart(e, i, new Rectangle(x, bounds.y, width, bounds.height));
            x += width;
        }
    }

    // For a title which is a Control, position it appropriately to center in the plot area.
    private void setTitleBounds(Rectangle bounds) {
        Control title = (Control) chart.getTitle();
        Rectangle titleBounds = title.getBounds();
        title.setLocation(new Point(bounds.x + (bounds.width - titleBounds.width) / 2, title.getLocation().y));
    }

    // Adjust the title with trailing blanks so it centers in the plot area
    // rather than for the entire chart view which looks odd when not
    // centered above the pie-charts themselves.
    private void adjustTitle(PaintEvent pe) {
        ITitle title = chart.getTitle();
        Font font = title.getFont();
        Font oldFont = pe.gc.getFont();
        pe.gc.setFont(font);
        Control legend = (Control)chart.getLegend();
        Rectangle legendBounds = legend.getBounds();
        int adjustment = legendBounds.width - 15;
        Point blankSize = pe.gc.textExtent(" "); //$NON-NLS-1$
        int numBlanks = ((adjustment / blankSize.x) >> 1) << 1;
        String text = origTitleText;
        for (int i = 0; i < numBlanks; ++i)
            text += " "; //$NON-NLS-1$
        pe.gc.setFont(oldFont);
        title.setText(text);
    }

    private void drawPieChart(PaintEvent e, int chartnum, Rectangle bounds) {
        double series[] = seriesValues[chartnum];
        int nelemSeries = series.length;
        double sumTotal = 0;

        pieSliceAngles[chartnum] = new int[nelemSeries - 1]; // Don't need first angle; it's always 0
        for (int i = 0; i < nelemSeries; i++) {
            sumTotal += series[i];
        }

        GC gc = e.gc;
        Font oldFont = gc.getFont();
        gc.setLineWidth(1);

        int pieX = bounds.x + (bounds.width - pieWidth) / 2;
        int pieY = bounds.y + (bounds.height - pieWidth) / 2;
        pieCenters[chartnum] = new Point(pieX + pieWidth / 2, pieY + pieWidth / 2);
        if (sumTotal == 0) {
            gc.drawOval(pieX, pieY, pieWidth, pieWidth);
        } else {
            double factor = 100 / sumTotal;
            int sweepAngle = 0;
            int incrementAngle = 0;
            int initialAngle = 90;
            for (int i = 0; i < nelemSeries; i++) {
                // Stored angles increase in clockwise direction from 0 degrees at 12:00
                if (i > 0) {
                    pieSliceAngles[chartnum][i - 1] = 90 - initialAngle;
                }

                gc.setBackground(((IBarSeries) chart.getSeriesSet().getSeries()[i]).getBarColor());

                if (i == (nelemSeries - 1)) {
                    sweepAngle = 360 - incrementAngle;
                } else {
                    double angle = series[i] * factor * 3.6;
                    sweepAngle = (int) Math.round(angle);
                }
                gc.fillArc(pieX, pieY, pieWidth, pieWidth, initialAngle, (-sweepAngle));
                gc.drawArc(pieX, pieY, pieWidth, pieWidth, initialAngle, (-sweepAngle));
                incrementAngle += sweepAngle;
                initialAngle += (-sweepAngle);
            }
            gc.drawLine(pieCenters[chartnum].x, pieCenters[chartnum].y, pieCenters[chartnum].x, pieCenters[chartnum].y - pieWidth / 2);
        }

        Font font = new Font(Display.getDefault(), FONT, 12, SWT.BOLD);
        gc.setForeground(BLACK);
        gc.setBackground(WHITE);
        gc.setFont(font);
        String text = seriesNames[chartnum];
        Point textSize = e.gc.textExtent(text);
        gc.drawText(text, pieX + (pieWidth - textSize.x) / 2, pieY + pieWidth + textSize.y);
        gc.setFont(oldFont);
        font.dispose();
    }

    private void getPieSeriesArray() {
        ISeries series[] = this.chart.getSeriesSet().getSeries();
        if (series == null || series.length == 0) {
            seriesValues = new double[0][0];
            seriesNames = new String[0];
            return;
        }
        String names[] = this.chart.getAxisSet().getXAxis(0).getCategorySeries();
        Range range = chart.getAxisSet().getXAxis(0).getRange();
        int itemRange = (int) range.upper - (int) range.lower + 1;
        int itemOffset = (int) range.lower;
        seriesValues = new double[itemRange][series.length];
        seriesNames = new String[itemRange];

        for (int i = 0; i < seriesValues.length; i++) {
            seriesNames[i] = names[i + itemOffset];
            for (int j = 0; j < seriesValues[i].length; j++) {
                double d[] = series[j].getXSeries();
                if (d != null && d.length > 0) {
                    seriesValues[i][j] = d[i + itemOffset];
                } else {
                    seriesValues[i][j] = 0;
                }
            }
        }

        return;
    }

    /**
     * Given a set of 2D pixel coordinates (typically those of a mouse cursor), return the
     * index of the given pie's slice that those coordinates reside in.
     * @param chartnum The id of the chart.
     * @param x The x-coordinate to test.
     * @param y The y-coordinate to test.
     * @return The slice that contains the point with coordinates (x,y).
     * @since 2.0
     */
    public int getSliceIndexFromPosition(int chartnum, int x, int y) {
        Range range = chart.getAxisSet().getXAxis(0).getRange();
        chartnum -= (int) range.lower;
        if (chartnum >= pieCenters.length || chartnum < 0) {
            return -1;
        }
        // Only continue if the point is inside the pie circle
        double rad = Math.sqrt(Math.pow(pieCenters[chartnum].x - x, 2) + Math.pow(pieCenters[chartnum].y - y, 2));
        if (2 * rad > pieWidth) {
            return -1;
        }
        // Angle is relative to 12:00 position, increases clockwise
        double angle = Math.acos((pieCenters[chartnum].y - y) / rad) / Math.PI * 180.0;
        if (x - pieCenters[chartnum].x < 0) {
            angle = 360 - angle;
        }
        if (pieSliceAngles[chartnum].length == 0 || angle < pieSliceAngles[chartnum][0]) {
            return 0;
        }
        for (int s = 0; s < pieSliceAngles[chartnum].length - 1; s++) {
            if (pieSliceAngles[chartnum][s] <= angle && angle < pieSliceAngles[chartnum][s+1]) {
                return s + 1;
            }
        }
        return pieSliceAngles[chartnum].length;
    }
}
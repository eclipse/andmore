/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - Renato Stoffalette Joao <rsjoao@br.ibm.com>
 *******************************************************************************/
package org.eclipse.andmore.charts.piechart;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.swtchart.Chart;
import org.swtchart.IAxis;
import org.swtchart.IBarSeries;
import org.swtchart.ISeries;
import org.swtchart.ITitle;

public class PieChart extends Chart {

    protected List<RGB> colorList = new ArrayList<RGB>();
    private Color[] customColors = null;
    private PieChartPaintListener pieChartPaintListener;

    public PieChart(Composite parent, int style) {
        super(parent, style);
        // Hide all original axes and plot area
        for (IAxis axis : getAxisSet().getAxes()) {
            axis.getTitle().setVisible(false);
        }
        getPlotArea().setVisible(false);
        // Make the title draw after the pie-chart itself so we can modify the title
        // to center over the pie-chart area
        ITitle title = getTitle();
        // Underlying SWT Chart implementation changes from the title being a Control to just
        // a PaintListener.  In the Control class case, we can move it's location to
        // center over a PieChart, but in the latter case, we need to alter the title
        // with blanks in the PieChartPaintListener and have the title paint after it
        // once the title has been altered.
        if (title instanceof Control) {
            addPaintListener(pieChartPaintListener = new PieChartPaintListener(this));
        } else {
            removePaintListener((PaintListener)title);
            addPaintListener(pieChartPaintListener = new PieChartPaintListener(this));
            addPaintListener((PaintListener)title);
        }
        IAxis xAxis = getAxisSet().getXAxis(0);
        xAxis.enableCategory(true);
        xAxis.setCategorySeries(new String[]{""}); //$NON-NLS-1$
    }

    @Override
    public void addPaintListener(PaintListener listener) {
        if (!listener.getClass().getName().startsWith("org.swtchart.internal.axis")) { //$NON-NLS-1$
            super.addPaintListener(listener);
        }
    }

    /**
     * Sets the custom colors to use.
     * @param customColors The custom colors to use.
     * @since 2.0
     */
    public void setCustomColors(Color[] customColors) {
        this.customColors = customColors.clone();
    }

    /**
     * Add data to this Pie Chart. We'll build one pie chart for each value in the array provided. The val matrix must
     * have an array of an array of values. Ex. labels = {'a', 'b'} val = {{1,2,3}, {4,5,6}} This will create 3 pie
     * charts. For the first one, 'a' will be 1 and 'b' will be 4. For the second chart 'a' will be 2 and 'b' will be 5.
     * For the third 'a' will be 3 and 'b' will be 6.
     * @param labels The titles of each series. (These are not the same as titles given to pies.)
     * @param val New values.
     */
    public void addPieChartSeries(String labels[], double val[][]) {
        setSeriesNames(val[0].length);
        for (ISeries s : this.getSeriesSet().getSeries()) {
            this.getSeriesSet().deleteSeries(s.getId());
        }

        int size = Math.min(labels.length, val.length);
        for (int i = 0; i < size; i++) {
            IBarSeries s = (IBarSeries) this.getSeriesSet().createSeries(ISeries.SeriesType.BAR, labels[i]);
            double d[] = new double[val[i].length];
            for (int j = 0; j < val[i].length; j++) {
                d[j] = val[i][j];
            }
            s.setXSeries(d);
            if (customColors != null) {
                s.setBarColor(customColors[i % customColors.length]);
            } else {
                s.setBarColor(new Color(this.getDisplay(), sliceColor(i)));
            }
        }
    }

    /**
     * Sets this chart's category names such that the number of names
     * is equal to the number of pies. This method will only make changes
     * to category series if they are not already properly set.
     * @param numExpected The number of pies / the expected number of category names.
     */
    private void setSeriesNames(int numExpected) {
        IAxis xAxis = getAxisSet().getXAxis(0);
        if (xAxis.getCategorySeries().length != numExpected) {
            String[] seriesNames = new String[numExpected];
            for (int i = 0, n = Math.min(xAxis.getCategorySeries().length, numExpected); i < n; i++) {
                seriesNames[i] = xAxis.getCategorySeries()[i];
            }
            for (int i = xAxis.getCategorySeries().length; i < numExpected; i++) {
                seriesNames[i] = ""; //$NON-NLS-1$
            }
            xAxis.setCategorySeries(seriesNames);
        }
    }

    protected RGB sliceColor(int i) {
        if (colorList.size() > i) {
            return colorList.get(i);
        }

        RGB next = IColorsConstants.COLORS[i % IColorsConstants.COLORS.length];
        colorList.add(next);
        return next;
    }

    /**
     * Given a set of 2D pixel coordinates (typically those of a mouse cursor), return the
     * index of the given pie's slice that those coordinates reside in.
     * @param pieIndex The index of the pie to get the slice of.
     * @param x The x-coordinate to test.
     * @param y The y-coordinate to test.
     * @return The slice that contains the point with coordinates (x,y).
     * @since 2.0
     */
    public int getSliceIndexFromPosition(int pieIndex, int x, int y) {
        return pieChartPaintListener.getSliceIndexFromPosition(pieIndex, x, y);
    }

    /**
     * Given a pie and one of its slices, returns the size of the slice as a percentage of the pie.
     * @param pieIndex The index of the pie to check.
     * @param sliceIndex The slice of the pie to get the percentage of.
     * @return The percentage of the entire pie taken up by the slice.
     * @since 2.0
     */
    public double getSlicePercent(int pieIndex, int sliceIndex) {
        double max = 0;
        ISeries series[] = getSeriesSet().getSeries();
        for (int i = 0; i < series.length; i++) {
            max += series[i].getXSeries()[pieIndex];
        }
        return series[sliceIndex].getXSeries()[pieIndex] / max * 100;
    }
}
/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.sdkuilib.internal.repository.ui;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TreeColumnViewerLabelProvider;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

/**
 * A custom version of {@link TreeColumnViewerLabelProvider} which
 * handles {@link TreePath}s and delegates content to the given
 * {@link ColumnLabelProvider} for a given {@link TreeViewerColumn}.
 * <p/>
 * The implementation handles a variety of providers (table label, table
 * color, table font) but does not implement a tooltip provider, so we
 * delegate the calls here to the appropriate {@link ColumnLabelProvider}.
 * <p/>
 * Only {@link #getToolTipText(Object)} is really useful for us but we
 * delegate all the tooltip calls for completeness and avoid surprises later
 * if we ever decide to override more things in the label provider.
 */
class PkgTreeColumnViewerLabelProvider extends TreeColumnViewerLabelProvider {

    private CellLabelProvider mTooltipProvider;

    public PkgTreeColumnViewerLabelProvider(ColumnLabelProvider columnLabelProvider) {
        super(columnLabelProvider);
    }

    @Override
    public void setProviders(Object provider) {
        super.setProviders(provider);
        if (provider instanceof CellLabelProvider) {
            mTooltipProvider = (CellLabelProvider) provider;
        }
    }

    @Override
    public Image getToolTipImage(Object object) {
        if (mTooltipProvider != null) {
            return mTooltipProvider.getToolTipImage(object);
        }
        return super.getToolTipImage(object);
    }

    @Override
    public String getToolTipText(Object element) {
        if (mTooltipProvider != null) {
            return mTooltipProvider.getToolTipText(element);
        }
        return super.getToolTipText(element);
    }

    @Override
    public Color getToolTipBackgroundColor(Object object) {
        if (mTooltipProvider != null) {
            return mTooltipProvider.getToolTipBackgroundColor(object);
        }
        return super.getToolTipBackgroundColor(object);
    }

    @Override
    public Color getToolTipForegroundColor(Object object) {
        if (mTooltipProvider != null) {
            return mTooltipProvider.getToolTipForegroundColor(object);
        }
        return super.getToolTipForegroundColor(object);
    }

    @Override
    public Font getToolTipFont(Object object) {
        if (mTooltipProvider != null) {
            return mTooltipProvider.getToolTipFont(object);
        }
        return super.getToolTipFont(object);
    }

    @Override
    public Point getToolTipShift(Object object) {
        if (mTooltipProvider != null) {
            return mTooltipProvider.getToolTipShift(object);
        }
        return super.getToolTipShift(object);
    }

    @Override
    public boolean useNativeToolTip(Object object) {
        if (mTooltipProvider != null) {
            return mTooltipProvider.useNativeToolTip(object);
        }
        return super.useNativeToolTip(object);
    }

    @Override
    public int getToolTipTimeDisplayed(Object object) {
        if (mTooltipProvider != null) {
            return mTooltipProvider.getToolTipTimeDisplayed(object);
        }
        return super.getToolTipTimeDisplayed(object);
    }

    @Override
    public int getToolTipDisplayDelayTime(Object object) {
        if (mTooltipProvider != null) {
            return mTooltipProvider.getToolTipDisplayDelayTime(object);
        }
        return super.getToolTipDisplayDelayTime(object);
    }

    @Override
    public int getToolTipStyle(Object object) {
        if (mTooltipProvider != null) {
            return mTooltipProvider.getToolTipStyle(object);
        }
        return super.getToolTipStyle(object);
    }
}

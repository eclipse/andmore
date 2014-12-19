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

import static org.eclipse.andmore.internal.editors.layout.configuration.Configuration.CFG_DEVICE;
import static org.eclipse.andmore.internal.editors.layout.configuration.Configuration.CFG_DEVICE_STATE;
import static org.eclipse.andmore.internal.editors.layout.configuration.Configuration.MASK_ALL;
import static org.eclipse.andmore.internal.editors.layout.gle2.ImageUtils.SHADOW_SIZE;
import static org.eclipse.andmore.internal.editors.layout.gle2.ImageUtils.SMALL_SHADOW_SIZE;
import static org.eclipse.andmore.internal.editors.layout.gle2.RenderPreview.LARGE_SHADOWS;
import static org.eclipse.andmore.internal.editors.layout.gle2.RenderPreviewMode.CUSTOM;
import static org.eclipse.andmore.internal.editors.layout.gle2.RenderPreviewMode.NONE;
import static org.eclipse.andmore.internal.editors.layout.gle2.RenderPreviewMode.SCREENS;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.api.Rect;
import com.android.ide.common.rendering.api.Capability;
import com.android.ide.common.resources.configuration.DensityQualifier;
import com.android.ide.common.resources.configuration.DeviceConfigHelper;
import com.android.ide.common.resources.configuration.FolderConfiguration;
import com.android.ide.common.resources.configuration.LanguageQualifier;
import com.android.ide.common.resources.configuration.ScreenSizeQualifier;
import com.android.resources.Density;
import com.android.resources.ScreenSize;
import com.android.sdklib.devices.Device;
import com.android.sdklib.devices.Screen;
import com.android.sdklib.devices.State;
import com.google.common.collect.Lists;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.AdtUtils;
import org.eclipse.andmore.internal.editors.IconFactory;
import org.eclipse.andmore.internal.editors.common.CommonXmlEditor;
import org.eclipse.andmore.internal.editors.layout.configuration.Configuration;
import org.eclipse.andmore.internal.editors.layout.configuration.ConfigurationChooser;
import org.eclipse.andmore.internal.editors.layout.configuration.ConfigurationClient;
import org.eclipse.andmore.internal.editors.layout.configuration.ConfigurationDescription;
import org.eclipse.andmore.internal.editors.layout.configuration.Locale;
import org.eclipse.andmore.internal.editors.layout.configuration.NestedConfiguration;
import org.eclipse.andmore.internal.editors.layout.configuration.VaryingConfiguration;
import org.eclipse.andmore.internal.editors.layout.gle2.IncludeFinder.Reference;
import org.eclipse.andmore.internal.preferences.AdtPrefs;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Manager for the configuration previews, which handles layout computations,
 * managing the image buffer cache, etc
 */
public class RenderPreviewManager {
    private static double sScale = 1.0;
    private static final int RENDER_DELAY = 150;
    private static final int PREVIEW_VGAP = 18;
    private static final int PREVIEW_HGAP = 12;
    private static final int MAX_WIDTH = 200;
    private static final int MAX_HEIGHT = MAX_WIDTH;
    private static final int ZOOM_ICON_WIDTH = 16;
    private static final int ZOOM_ICON_HEIGHT = 16;
    private @Nullable List<RenderPreview> mPreviews;
    private @Nullable RenderPreviewList mManualList;
    private final @NonNull LayoutCanvas mCanvas;
    private final @NonNull CanvasTransform mVScale;
    private final @NonNull CanvasTransform mHScale;
    private int mPrevCanvasWidth;
    private int mPrevCanvasHeight;
    private int mPrevImageWidth;
    private int mPrevImageHeight;
    private @NonNull RenderPreviewMode mMode = NONE;
    private @Nullable RenderPreview mActivePreview;
    private @Nullable ScrollBarListener mListener;
    private int mLayoutHeight;
    /** Last seen state revision in this {@link RenderPreviewManager}. If less
     * than {@link #sRevision}, the previews need to be updated on next exposure */
    private static int mRevision;
    /** Current global revision count */
    private static int sRevision;
    private boolean mNeedLayout;
    private boolean mNeedRender;
    private boolean mNeedZoom;
    private SwapAnimation mAnimation;

    /**
     * Creates a {@link RenderPreviewManager} associated with the given canvas
     *
     * @param canvas the canvas to manage previews for
     */
    public RenderPreviewManager(@NonNull LayoutCanvas canvas) {
        mCanvas = canvas;
        mHScale = canvas.getHorizontalTransform();
        mVScale = canvas.getVerticalTransform();
    }

    /**
     * Revise the global state revision counter. This will cause all layout
     * preview managers to refresh themselves to the latest revision when they
     * are next exposed.
     */
    public static void bumpRevision() {
        sRevision++;
    }

    /**
     * Returns the associated chooser
     *
     * @return the associated chooser
     */
    @NonNull
    ConfigurationChooser getChooser() {
        GraphicalEditorPart editor = mCanvas.getEditorDelegate().getGraphicalEditor();
        return editor.getConfigurationChooser();
    }

    /**
     * Returns the associated canvas
     *
     * @return the canvas
     */
    @NonNull
    public LayoutCanvas getCanvas() {
        return mCanvas;
    }

    /** Zooms in (grows all previews) */
    public void zoomIn() {
        sScale = sScale * (1 / 0.9);
        if (Math.abs(sScale-1.0) < 0.0001) {
            sScale = 1.0;
        }

        updatedZoom();
    }

    /** Zooms out (shrinks all previews) */
    public void zoomOut() {
        sScale = sScale * (0.9 / 1);
        if (Math.abs(sScale-1.0) < 0.0001) {
            sScale = 1.0;
        }
        updatedZoom();
    }

    /** Zooms to 100 (resets zoom) */
    public void zoomReset() {
        sScale = 1.0;
        updatedZoom();
        mNeedZoom = mNeedLayout = true;
        mCanvas.redraw();
    }

    private void updatedZoom() {
        if (hasPreviews()) {
            for (RenderPreview preview : mPreviews) {
                preview.disposeThumbnail();
            }
            RenderPreview preview = mCanvas.getPreview();
            if (preview != null) {
                preview.disposeThumbnail();
            }
        }

        mNeedLayout = mNeedRender = true;
        mCanvas.redraw();
    }

    static int getMaxWidth() {
        return (int) (sScale * MAX_WIDTH);
    }

    static int getMaxHeight() {
        return (int) (sScale * MAX_HEIGHT);
    }

    static double getScale() {
        return sScale;
    }

    /**
     * Returns whether there are any manual preview items (provided the current
     * mode is manual previews
     *
     * @return true if there are items in the manual preview list
     */
    public boolean hasManualPreviews() {
        assert mMode == CUSTOM;
        return mManualList != null && !mManualList.isEmpty();
    }

    /** Delete all the previews */
    public void deleteManualPreviews() {
        disposePreviews();
        selectMode(NONE);
        mCanvas.setFitScale(true /* onlyZoomOut */, true /*allowZoomIn*/);

        if (mManualList != null) {
            mManualList.delete();
        }
    }

    /** Dispose all the previews */
    public void disposePreviews() {
        if (mPreviews != null) {
            List<RenderPreview> old = mPreviews;
            mPreviews = null;
            for (RenderPreview preview : old) {
                preview.dispose();
            }
        }
    }

    /**
     * Deletes the given preview
     *
     * @param preview the preview to be deleted
     */
    public void deletePreview(RenderPreview preview) {
        mPreviews.remove(preview);
        preview.dispose();
        layout(true);
        mCanvas.redraw();

        if (mManualList != null) {
            mManualList.remove(preview);
            saveList();
        }
    }

    /**
     * Compute the total width required for the previews, including internal padding
     *
     * @return total width in pixels
     */
    public int computePreviewWidth() {
        int maxPreviewWidth = 0;
        if (hasPreviews()) {
            for (RenderPreview preview : mPreviews) {
                maxPreviewWidth = Math.max(maxPreviewWidth, preview.getWidth());
            }

            if (maxPreviewWidth > 0) {
                maxPreviewWidth += 2 * PREVIEW_HGAP; // 2x for left and right side
                maxPreviewWidth += LARGE_SHADOWS ? SHADOW_SIZE : SMALL_SHADOW_SIZE;
            }

            return maxPreviewWidth;
        }

        return 0;
    }

    /**
     * Layout Algorithm. This sets the {@link RenderPreview#getX()} and
     * {@link RenderPreview#getY()} coordinates of all the previews. It also
     * marks previews as visible or invisible via
     * {@link RenderPreview#setVisible(boolean)} according to their position and
     * the current visible view port in the layout canvas. Finally, it also sets
     * the {@code mLayoutHeight} field, such that the scrollbars can compute the
     * right scrolled area, and that scrolling can cause render refreshes on
     * views that are made visible.
     * <p>
     * This is not a traditional bin packing problem, because the objects to be
     * packaged do not have a fixed size; we can scale them up and down in order
     * to provide an "optimal" size.
     * <p>
     * See http://en.wikipedia.org/wiki/Packing_problem See
     * http://en.wikipedia.org/wiki/Bin_packing_problem
     */
    void layout(boolean refresh) {
        mNeedLayout = false;

        if (mPreviews == null || mPreviews.isEmpty()) {
            return;
        }

        int scaledImageWidth = mHScale.getScaledImgSize();
        int scaledImageHeight = mVScale.getScaledImgSize();
        Rectangle clientArea = mCanvas.getClientArea();

        if (!refresh &&
                (scaledImageWidth == mPrevImageWidth
                && scaledImageHeight == mPrevImageHeight
                && clientArea.width == mPrevCanvasWidth
                && clientArea.height == mPrevCanvasHeight)) {
            // No change
            return;
        }

        mPrevImageWidth = scaledImageWidth;
        mPrevImageHeight = scaledImageHeight;
        mPrevCanvasWidth = clientArea.width;
        mPrevCanvasHeight = clientArea.height;

        if (mListener == null) {
            mListener = new ScrollBarListener();
            mCanvas.getVerticalBar().addSelectionListener(mListener);
        }

        beginRenderScheduling();

        mLayoutHeight = 0;

        if (previewsHaveIdenticalSize() || fixedOrder()) {
            // If all the preview boxes are of identical sizes, or if the order is predetermined,
            // just lay them out in rows.
            rowLayout();
        } else if (previewsFit()) {
            layoutFullFit();
        } else {
            rowLayout();
        }

        mCanvas.updateScrollBars();
    }

    /**
     * Performs a simple layout where the views are laid out in a row, wrapping
     * around the top left canvas image.
     */
    private void rowLayout() {
        // TODO: Separate layout heuristics for portrait and landscape orientations (though
        // it also depends on the dimensions of the canvas window, which determines the
        // shape of the leftover space)

        int scaledImageWidth = mHScale.getScaledImgSize();
        int scaledImageHeight = mVScale.getScaledImgSize();
        Rectangle clientArea = mCanvas.getClientArea();

        int availableWidth = clientArea.x + clientArea.width - getX();
        int availableHeight = clientArea.y + clientArea.height - getY();
        int maxVisibleY = clientArea.y + clientArea.height;

        int bottomBorder = scaledImageHeight;
        int rightHandSide = scaledImageWidth + PREVIEW_HGAP;
        int nextY = 0;

        // First lay out images across the top right hand side
        int x = rightHandSide;
        int y = 0;
        boolean wrapped = false;

        int vgap = PREVIEW_VGAP;
        for (RenderPreview preview : mPreviews) {
            // If we have forked previews, double the vgap to allow space for two labels
            if (preview.isForked()) {
                vgap *= 2;
                break;
            }
        }

        List<RenderPreview> aspectOrder;
        if (!fixedOrder()) {
            aspectOrder = new ArrayList<RenderPreview>(mPreviews);
            Collections.sort(aspectOrder, RenderPreview.INCREASING_ASPECT_RATIO);
        } else {
            aspectOrder = mPreviews;
        }

        for (RenderPreview preview : aspectOrder) {
            if (x > 0 && x + preview.getWidth() > availableWidth) {
                x = rightHandSide;
                int prevY = y;
                y = nextY;
                if ((prevY <= bottomBorder ||
                        y <= bottomBorder)
                            && Math.max(nextY, y + preview.getHeight()) > bottomBorder) {
                    // If there's really no visible room below, don't bother
                    // Similarly, don't wrap individually scaled views
                    if (bottomBorder < availableHeight - 40 && preview.getScale() < 1.2) {
                        // If it's closer to the top row than the bottom, just
                        // mark the next row for left justify instead
                        if (bottomBorder - y > y + preview.getHeight() - bottomBorder) {
                            rightHandSide = 0;
                            wrapped = true;
                        } else if (!wrapped) {
                            y = nextY = Math.max(nextY, bottomBorder + vgap);
                            x = rightHandSide = 0;
                            wrapped = true;
                        }
                    }
                }
            }
            if (x > 0 && y <= bottomBorder
                    && Math.max(nextY, y + preview.getHeight()) > bottomBorder) {
                if (clientArea.height - bottomBorder < preview.getHeight()) {
                    // No room below the device on the left; just continue on the
                    // bottom row
                } else if (preview.getScale() < 1.2) {
                    if (bottomBorder - y > y + preview.getHeight() - bottomBorder) {
                        rightHandSide = 0;
                        wrapped = true;
                    } else {
                        y = nextY = Math.max(nextY, bottomBorder + vgap);
                        x = rightHandSide = 0;
                        wrapped = true;
                    }
                }
            }

            preview.setPosition(x, y);

            if (y > maxVisibleY && maxVisibleY > 0) {
                preview.setVisible(false);
            } else if (!preview.isVisible()) {
                preview.setVisible(true);
            }

            x += preview.getWidth();
            x += PREVIEW_HGAP;
            nextY = Math.max(nextY, y + preview.getHeight() + vgap);
        }

        mLayoutHeight = nextY;
    }

    private boolean fixedOrder() {
        return mMode == SCREENS;
    }

    /** Returns true if all the previews have the same identical size */
    private boolean previewsHaveIdenticalSize() {
        if (!hasPreviews()) {
            return true;
        }

        Iterator<RenderPreview> iterator = mPreviews.iterator();
        RenderPreview first = iterator.next();
        int width = first.getWidth();
        int height = first.getHeight();

        while (iterator.hasNext()) {
            RenderPreview preview = iterator.next();
            if (width != preview.getWidth() || height != preview.getHeight()) {
                return false;
            }
        }

        return true;
    }

    /** Returns true if all the previews can fully fit in the available space */
    private boolean previewsFit() {
        int scaledImageWidth = mHScale.getScaledImgSize();
        int scaledImageHeight = mVScale.getScaledImgSize();
        Rectangle clientArea = mCanvas.getClientArea();
        int availableWidth = clientArea.x + clientArea.width - getX();
        int availableHeight = clientArea.y + clientArea.height - getY();
        int bottomBorder = scaledImageHeight;
        int rightHandSide = scaledImageWidth + PREVIEW_HGAP;

        // First see if we can fit everything; if so, we can try to make the layouts
        // larger such that they fill up all the available space
        long availableArea = rightHandSide * bottomBorder +
                availableWidth * (Math.max(0, availableHeight - bottomBorder));

        long requiredArea = 0;
        for (RenderPreview preview : mPreviews) {
            // Note: This does not include individual preview scale; the layout
            // algorithm itself may be tweaking the scales to fit elements within
            // the layout
            requiredArea += preview.getArea();
        }

        return requiredArea * sScale < availableArea;
    }

    private void layoutFullFit() {
        int scaledImageWidth = mHScale.getScaledImgSize();
        int scaledImageHeight = mVScale.getScaledImgSize();
        Rectangle clientArea = mCanvas.getClientArea();
        int availableWidth = clientArea.x + clientArea.width - getX();
        int availableHeight = clientArea.y + clientArea.height - getY();
        int maxVisibleY = clientArea.y + clientArea.height;
        int bottomBorder = scaledImageHeight;
        int rightHandSide = scaledImageWidth + PREVIEW_HGAP;

        int minWidth = Integer.MAX_VALUE;
        int minHeight = Integer.MAX_VALUE;
        for (RenderPreview preview : mPreviews) {
            minWidth = Math.min(minWidth, preview.getWidth());
            minHeight = Math.min(minHeight, preview.getHeight());
        }

        BinPacker packer = new BinPacker(minWidth, minHeight);

        // TODO: Instead of this, just start with client area and occupy scaled image size!

        // Add in gap on right and bottom since we'll add that requirement on the width and
        // height rectangles too (for spacing)
        packer.addSpace(new Rect(rightHandSide, 0,
                availableWidth - rightHandSide + PREVIEW_HGAP,
                availableHeight + PREVIEW_VGAP));
        if (maxVisibleY > bottomBorder) {
            packer.addSpace(new Rect(0, bottomBorder + PREVIEW_VGAP,
                    availableWidth + PREVIEW_HGAP, maxVisibleY - bottomBorder + PREVIEW_VGAP));
        }

        // TODO: Sort previews first before attempting to position them?

        ArrayList<RenderPreview> aspectOrder = new ArrayList<RenderPreview>(mPreviews);
        Collections.sort(aspectOrder, RenderPreview.INCREASING_ASPECT_RATIO);

        for (RenderPreview preview : aspectOrder) {
            int previewWidth = preview.getWidth();
            int previewHeight = preview.getHeight();
            previewHeight += PREVIEW_VGAP;
            if (preview.isForked()) {
                previewHeight += PREVIEW_VGAP;
            }
            previewWidth += PREVIEW_HGAP;
            // title height? how do I account for that?
            Rect position = packer.occupy(previewWidth, previewHeight);
            if (position != null) {
                preview.setPosition(position.x, position.y);
                preview.setVisible(true);
            } else {
                // Can't fit: give up and do plain row layout
                rowLayout();
                return;
            }
        }

        mLayoutHeight = availableHeight;
    }
    /**
     * Paints the configuration previews
     *
     * @param gc the graphics context to paint into
     */
    void paint(GC gc) {
        if (hasPreviews()) {
            // Ensure up to date at all times; consider moving if it's too expensive
            layout(mNeedLayout);
            if (mNeedRender) {
                renderPreviews();
            }
            if (mNeedZoom) {
                boolean allowZoomIn = true /*mMode == NONE*/;
                mCanvas.setFitScale(false /*onlyZoomOut*/, allowZoomIn);
                mNeedZoom = false;
            }
            int rootX = getX();
            int rootY = getY();

            for (RenderPreview preview : mPreviews) {
                if (preview.isVisible()) {
                    int x = rootX + preview.getX();
                    int y = rootY + preview.getY();
                    preview.paint(gc, x, y);
                }
            }

            RenderPreview preview = mCanvas.getPreview();
            if (preview != null) {
                String displayName = null;
                Configuration configuration = preview.getConfiguration();
                if (configuration instanceof VaryingConfiguration) {
                    // Use override flags from stashed preview, but configuration
                    // data from live (not varying) configured configuration
                    VaryingConfiguration cfg = (VaryingConfiguration) configuration;
                    int flags = cfg.getAlternateFlags() | cfg.getOverrideFlags();
                    displayName = NestedConfiguration.computeDisplayName(flags,
                            getChooser().getConfiguration());
                } else if (configuration instanceof NestedConfiguration) {
                    int flags = ((NestedConfiguration) configuration).getOverrideFlags();
                    displayName = NestedConfiguration.computeDisplayName(flags,
                            getChooser().getConfiguration());
                } else {
                    displayName = configuration.getDisplayName();
                }
                if (displayName != null) {
                    CanvasTransform hi = mHScale;
                    CanvasTransform vi = mVScale;

                    int destX = hi.translate(0);
                    int destY = vi.translate(0);
                    int destWidth = hi.getScaledImgSize();
                    int destHeight = vi.getScaledImgSize();

                    int x = destX + destWidth / 2 - preview.getWidth() / 2;
                    int y = destY + destHeight;

                    preview.paintTitle(gc, x, y, false /*showFile*/, displayName);
                }
            }

            // Zoom overlay
            int x = getZoomX();
            if (x > 0) {
                int y = getZoomY();
                int oldAlpha = gc.getAlpha();

                // Paint background oval rectangle behind the zoom and close icons
                gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_GRAY));
                gc.setAlpha(128);
                int padding = 3;
                int arc = 5;
                gc.fillRoundRectangle(x - padding, y - padding,
                        ZOOM_ICON_WIDTH + 2 * padding,
                        4 * ZOOM_ICON_HEIGHT + 2 * padding, arc, arc);

                gc.setAlpha(255);
                IconFactory iconFactory = IconFactory.getInstance();
                Image zoomOut = iconFactory.getIcon("zoomminus"); //$NON-NLS-1$);
                Image zoomIn = iconFactory.getIcon("zoomplus");   //$NON-NLS-1$);
                Image zoom100 = iconFactory.getIcon("zoom100");   //$NON-NLS-1$);
                Image close = iconFactory.getIcon("close");       //$NON-NLS-1$);

                gc.drawImage(zoomIn, x, y);
                y += ZOOM_ICON_HEIGHT;
                gc.drawImage(zoomOut, x, y);
                y += ZOOM_ICON_HEIGHT;
                gc.drawImage(zoom100, x, y);
                y += ZOOM_ICON_HEIGHT;
                gc.drawImage(close, x, y);
                y += ZOOM_ICON_HEIGHT;
                gc.setAlpha(oldAlpha);
            }
        } else if (mMode == CUSTOM) {
            int rootX = getX();
            rootX += mHScale.getScaledImgSize();
            rootX += 2 * PREVIEW_HGAP;
            int rootY = getY();
            rootY += 20;
            gc.setFont(mCanvas.getFont());
            gc.setForeground(mCanvas.getDisplay().getSystemColor(SWT.COLOR_BLACK));
            gc.drawText("Add previews with \"Add as Thumbnail\"\nin the configuration menu",
                    rootX, rootY, true);
        }

        if (mAnimation != null) {
            mAnimation.tick(gc);
        }
    }

    private void addPreview(@NonNull RenderPreview preview) {
        if (mPreviews == null) {
            mPreviews = Lists.newArrayList();
        }
        mPreviews.add(preview);
    }

    /** Adds the current configuration as a new configuration preview */
    public void addAsThumbnail() {
        ConfigurationChooser chooser = getChooser();
        String name = chooser.getConfiguration().getDisplayName();
        if (name == null || name.isEmpty()) {
            name = getUniqueName();
        }
        InputDialog d = new InputDialog(
                AdtPlugin.getShell(),
                "Add as Thumbnail Preview",  // title
                "Name of thumbnail:",
                name,
                null);
        if (d.open() == Window.OK) {
            selectMode(CUSTOM);

            String newName = d.getValue();
            // Create a new configuration from the current settings in the composite
            Configuration configuration = Configuration.copy(chooser.getConfiguration());
            configuration.setDisplayName(newName);

            RenderPreview preview = RenderPreview.create(this, configuration);
            addPreview(preview);

            layout(true);
            beginRenderScheduling();
            scheduleRender(preview);
            mCanvas.setFitScale(true /* onlyZoomOut */, false /*allowZoomIn*/);

            if (mManualList == null) {
                loadList();
            }
            if (mManualList != null) {
                mManualList.add(preview);
                saveList();
            }
        }
    }

    /**
     * Computes a unique new name for a configuration preview that represents
     * the current, default configuration
     *
     * @return a unique name
     */
    private String getUniqueName() {
        if (mPreviews == null || mPreviews.isEmpty()) {
            // NO, not for the first preview!
            return "Config1";
        }

        Set<String> names = new HashSet<String>(mPreviews.size());
        for (RenderPreview preview : mPreviews) {
            names.add(preview.getDisplayName());
        }

        int index = 2;
        while (true) {
            String name = String.format("Config%1$d", index);
            if (!names.contains(name)) {
                return name;
            }
            index++;
        }
    }

    /** Generates a bunch of default configuration preview thumbnails */
    public void addDefaultPreviews() {
        ConfigurationChooser chooser = getChooser();
        Configuration parent = chooser.getConfiguration();
        if (parent instanceof NestedConfiguration) {
            parent = ((NestedConfiguration) parent).getParent();
        }
        if (mCanvas.getImageOverlay().getImage() != null) {
            // Create Language variation
            createLocaleVariation(chooser, parent);

            // Vary screen size
            // TODO: Be smarter here: Pick a screen that is both as differently as possible
            // from the current screen as well as also supported. So consider
            // things like supported screens, targetSdk etc.
            createScreenVariations(parent);

            // Vary orientation
            createStateVariation(chooser, parent);

            // Vary render target
            createRenderTargetVariation(chooser, parent);
        }

        // Also add in include-context previews, if any
        addIncludedInPreviews();

        // Make a placeholder preview for the current screen, in case we switch from it
        RenderPreview preview = RenderPreview.create(this, parent);
        mCanvas.setPreview(preview);

        sortPreviewsByOrientation();
    }

    private void createRenderTargetVariation(ConfigurationChooser chooser, Configuration parent) {
        /* This is disabled for now: need to load multiple versions of layoutlib.
        When I did this, there seemed to be some drug interactions between
        them, and I would end up with NPEs in layoutlib code which normally works.
        VaryingConfiguration configuration =
                VaryingConfiguration.create(chooser, parent);
        configuration.setAlternatingTarget(true);
        configuration.syncFolderConfig();
        addPreview(RenderPreview.create(this, configuration));
        */
    }

    private void createStateVariation(ConfigurationChooser chooser, Configuration parent) {
        State currentState = parent.getDeviceState();
        State nextState = parent.getNextDeviceState(currentState);
        if (nextState != currentState) {
            VaryingConfiguration configuration =
                    VaryingConfiguration.create(chooser, parent);
            configuration.setAlternateDeviceState(true);
            configuration.syncFolderConfig();
            addPreview(RenderPreview.create(this, configuration));
        }
    }

    private void createLocaleVariation(ConfigurationChooser chooser, Configuration parent) {
        LanguageQualifier currentLanguage = parent.getLocale().language;
        for (Locale locale : chooser.getLocaleList()) {
            LanguageQualifier language = locale.language;
            if (!language.equals(currentLanguage)) {
                VaryingConfiguration configuration =
                        VaryingConfiguration.create(chooser, parent);
                configuration.setAlternateLocale(true);
                configuration.syncFolderConfig();
                addPreview(RenderPreview.create(this, configuration));
                break;
            }
        }
    }

    private void createScreenVariations(Configuration parent) {
        ConfigurationChooser chooser = getChooser();
        VaryingConfiguration configuration;

        configuration = VaryingConfiguration.create(chooser, parent);
        configuration.setVariation(0);
        configuration.setAlternateDevice(true);
        configuration.syncFolderConfig();
        addPreview(RenderPreview.create(this, configuration));

        configuration = VaryingConfiguration.create(chooser, parent);
        configuration.setVariation(1);
        configuration.setAlternateDevice(true);
        configuration.syncFolderConfig();
        addPreview(RenderPreview.create(this, configuration));
    }

    /**
     * Returns the current mode as seen by this {@link RenderPreviewManager}.
     * Note that it may not yet have been synced with the global mode kept in
     * {@link AdtPrefs#getRenderPreviewMode()}.
     *
     * @return the current preview mode
     */
    @NonNull
    public RenderPreviewMode getMode() {
        return mMode;
    }

    /**
     * Update the set of previews for the current mode
     *
     * @param force force a refresh even if the preview type has not changed
     * @return true if the views were recomputed, false if the previews were
     *         already showing and the mode not changed
     */
    public boolean recomputePreviews(boolean force) {
        RenderPreviewMode newMode = AdtPrefs.getPrefs().getRenderPreviewMode();
        if (newMode == mMode && !force
                && (mRevision == sRevision
                    || mMode == NONE
                    || mMode == CUSTOM)) {
            return false;
        }

        RenderPreviewMode oldMode = mMode;
        mMode = newMode;
        mRevision = sRevision;

        sScale = 1.0;
        disposePreviews();

        switch (mMode) {
            case DEFAULT:
                addDefaultPreviews();
                break;
            case INCLUDES:
                addIncludedInPreviews();
                break;
            case LOCALES:
                addLocalePreviews();
                break;
            case SCREENS:
                addScreenSizePreviews();
                break;
            case VARIATIONS:
                addVariationPreviews();
                break;
            case CUSTOM:
                addManualPreviews();
                break;
            case NONE:
                // Can't just set mNeedZoom because with no previews, the paint
                // method does nothing
                mCanvas.setFitScale(false /*onlyZoomOut*/, true /*allowZoomIn*/);
                break;
            default:
                assert false : mMode;
        }

        // We schedule layout for the next redraw rather than process it here immediately;
        // not only does this let us avoid doing work for windows where the tab is in the
        // background, but when a file is opened we may not know the size of the canvas
        // yet, and the layout methods need it in order to do a good job. By the time
        // the canvas is painted, we have accurate bounds.
        mNeedLayout = mNeedRender = true;
        mCanvas.redraw();

        if (oldMode != mMode && (oldMode == NONE || mMode == NONE)) {
            // If entering or exiting preview mode: updating padding which is compressed
            // only in preview mode.
            mCanvas.getHorizontalTransform().refresh();
            mCanvas.getVerticalTransform().refresh();
        }

        return true;
    }

    /**
     * Sets the new render preview mode to use
     *
     * @param mode the new mode
     */
    public void selectMode(@NonNull RenderPreviewMode mode) {
        if (mode != mMode) {
            AdtPrefs.getPrefs().setPreviewMode(mode);
            recomputePreviews(false);
        }
    }

    /** Similar to {@link #addDefaultPreviews()} but for locales */
    public void addLocalePreviews() {

        ConfigurationChooser chooser = getChooser();
        List<Locale> locales = chooser.getLocaleList();
        Configuration parent = chooser.getConfiguration();

        for (Locale locale : locales) {
            if (!locale.hasLanguage() && !locale.hasRegion()) {
                continue;
            }
            NestedConfiguration configuration = NestedConfiguration.create(chooser, parent);
            configuration.setOverrideLocale(true);
            configuration.setLocale(locale, false);

            String displayName = ConfigurationChooser.getLocaleLabel(chooser, locale, false);
            assert displayName != null; // it's never non null when locale is non null
            configuration.setDisplayName(displayName);

            addPreview(RenderPreview.create(this, configuration));
        }

        // Make a placeholder preview for the current screen, in case we switch from it
        Configuration configuration = parent;
        Locale locale = configuration.getLocale();
        String label = ConfigurationChooser.getLocaleLabel(chooser, locale, false);
        if (label == null) {
            label = "default";
        }
        configuration.setDisplayName(label);
        RenderPreview preview = RenderPreview.create(this, parent);
        if (preview != null) {
            mCanvas.setPreview(preview);
        }

        // No need to sort: they should all be identical
    }

    /** Similar to {@link #addDefaultPreviews()} but for screen sizes */
    public void addScreenSizePreviews() {
        ConfigurationChooser chooser = getChooser();
        Collection<Device> devices = chooser.getDevices();
        Configuration configuration = chooser.getConfiguration();
        boolean canScaleNinePatch = configuration.supports(Capability.FIXED_SCALABLE_NINE_PATCH);

        // Rearrange the devices a bit such that the most interesting devices bubble
        // to the front
        // 10" tablet, 7" tablet, reference phones, tiny phone, and in general the first
        // version of each seen screen size
        List<Device> sorted = new ArrayList<Device>(devices);
        Set<ScreenSize> seenSizes = new HashSet<ScreenSize>();
        State currentState = configuration.getDeviceState();
        String currentStateName = currentState != null ? currentState.getName() : "";

        for (int i = 0, n = sorted.size(); i < n; i++) {
            Device device = sorted.get(i);
            boolean interesting = false;

            State state = device.getState(currentStateName);
            if (state == null) {
                state = device.getAllStates().get(0);
            }

            if (device.getName().startsWith("Nexus ")         //$NON-NLS-1$
                    || device.getName().endsWith(" Nexus")) { //$NON-NLS-1$
                // Not String#contains("Nexus") because that would also pick up all the generic
                // entries ("3.7in WVGA (Nexus One)") so we'd have them duplicated
                interesting = true;
            }

            FolderConfiguration c = DeviceConfigHelper.getFolderConfig(state);
            if (c != null) {
                ScreenSizeQualifier sizeQualifier = c.getScreenSizeQualifier();
                if (sizeQualifier != null) {
                    ScreenSize size = sizeQualifier.getValue();
                    if (!seenSizes.contains(size)) {
                        seenSizes.add(size);
                        interesting = true;
                    }
                }

                // Omit LDPI, not really used anymore
                DensityQualifier density = c.getDensityQualifier();
                if (density != null) {
                    Density d = density.getValue();
                    if (d == Density.LOW) {
                        interesting = false;
                    }

                    if (!canScaleNinePatch && d == Density.TV) {
                        interesting = false;
                    }
                }
            }

            if (interesting) {
                NestedConfiguration screenConfig = NestedConfiguration.create(chooser,
                        configuration);
                screenConfig.setOverrideDevice(true);
                screenConfig.setDevice(device, true);
                screenConfig.syncFolderConfig();
                screenConfig.setDisplayName(ConfigurationChooser.getDeviceLabel(device, true));
                addPreview(RenderPreview.create(this, screenConfig));
            }
        }

        // Sorted by screen size, in decreasing order
        sortPreviewsByScreenSize();
    }

    /**
     * Previews this layout as included in other layouts
     */
    public void addIncludedInPreviews() {
        ConfigurationChooser chooser = getChooser();
        IProject project = chooser.getProject();
        if (project == null) {
            return;
        }
        IncludeFinder finder = IncludeFinder.get(project);

        final List<Reference> includedBy = finder.getIncludedBy(chooser.getEditedFile());

        if (includedBy == null || includedBy.isEmpty()) {
            // TODO: Generate some useful defaults, such as including it in a ListView
            // as the list item layout?
            return;
        }

        for (final Reference reference : includedBy) {
            String title = reference.getDisplayName();
            Configuration config = Configuration.create(chooser.getConfiguration(),
                    reference.getFile());
            RenderPreview preview = RenderPreview.create(this, config);
            preview.setDisplayName(title);
            preview.setIncludedWithin(reference);

            addPreview(preview);
        }

        sortPreviewsByOrientation();
    }

    /**
     * Previews this layout as included in other layouts
     */
    public void addVariationPreviews() {
        ConfigurationChooser chooser = getChooser();

        IFile file = chooser.getEditedFile();
        List<IFile> variations = AdtUtils.getResourceVariations(file, false /*includeSelf*/);

        // Sort by parent folder
        Collections.sort(variations, new Comparator<IFile>() {
            @Override
            public int compare(IFile file1, IFile file2) {
                return file1.getParent().getName().compareTo(file2.getParent().getName());
            }
        });

        Configuration currentConfig = chooser.getConfiguration();

        for (IFile variation : variations) {
            String title = variation.getParent().getName();
            Configuration config = Configuration.create(chooser.getConfiguration(), variation);
            config.setTheme(currentConfig.getTheme());
            config.setActivity(currentConfig.getActivity());
            RenderPreview preview = RenderPreview.create(this, config);
            preview.setDisplayName(title);
            preview.setAlternateInput(variation);

            addPreview(preview);
        }

        sortPreviewsByOrientation();
    }

    /**
     * Previews this layout using a custom configured set of layouts
     */
    public void addManualPreviews() {
        if (mManualList == null) {
            loadList();
        } else {
            mPreviews = mManualList.createPreviews(mCanvas);
        }
    }

    private void loadList() {
        IProject project = getChooser().getProject();
        if (project == null) {
            return;
        }

        if (mManualList == null) {
            mManualList = RenderPreviewList.get(project);
        }

        try {
            mManualList.load(getChooser().getDevices());
            mPreviews = mManualList.createPreviews(mCanvas);
        } catch (IOException e) {
            AdtPlugin.log(e, null);
        }
    }

    private void saveList() {
        if (mManualList != null) {
            try {
                mManualList.save();
            } catch (IOException e) {
                AdtPlugin.log(e, null);
            }
        }
    }

    void rename(ConfigurationDescription description, String newName) {
        IProject project = getChooser().getProject();
        if (project == null) {
            return;
        }

        if (mManualList == null) {
            mManualList = RenderPreviewList.get(project);
        }
        description.displayName = newName;
        saveList();
    }


    /**
     * Notifies that the main configuration has changed.
     *
     * @param flags the change flags, a bitmask corresponding to the
     *            {@code CHANGE_} constants in {@link ConfigurationClient}
     */
    public void configurationChanged(int flags) {
        // Similar to renderPreviews, but only acts on incomplete previews
        if (hasPreviews()) {
            // Do zoomed images first
            beginRenderScheduling();
            for (RenderPreview preview : mPreviews) {
                if (preview.getScale() > 1.2) {
                    preview.configurationChanged(flags);
                }
            }
            for (RenderPreview preview : mPreviews) {
                if (preview.getScale() <= 1.2) {
                    preview.configurationChanged(flags);
                }
            }
            RenderPreview preview = mCanvas.getPreview();
            if (preview != null) {
                preview.configurationChanged(flags);
                preview.dispose();
            }
            mNeedLayout = true;
            mCanvas.redraw();
        }
    }

    /** Updates the configuration preview thumbnails */
    public void renderPreviews() {
        if (hasPreviews()) {
            beginRenderScheduling();

            // Process in visual order
            ArrayList<RenderPreview> visualOrder = new ArrayList<RenderPreview>(mPreviews);
            Collections.sort(visualOrder, RenderPreview.VISUAL_ORDER);

            // Do zoomed images first
            for (RenderPreview preview : visualOrder) {
                if (preview.getScale() > 1.2 && preview.isVisible()) {
                    scheduleRender(preview);
                }
            }
            // Non-zoomed images
            for (RenderPreview preview : visualOrder) {
                if (preview.getScale() <= 1.2 && preview.isVisible()) {
                    scheduleRender(preview);
                }
            }
        }

        mNeedRender = false;
    }

    private int mPendingRenderCount;

    /**
     * Reset rendering scheduling. The next render request will be scheduled
     * after a single delay unit.
     */
    public void beginRenderScheduling() {
        mPendingRenderCount = 0;
    }

    /**
     * Schedule rendering the given preview. Each successive call will add an additional
     * delay unit to the schedule from the previous {@link #scheduleRender(RenderPreview)}
     * call, until {@link #beginRenderScheduling()} is called again.
     *
     * @param preview the preview to render
     */
    public void scheduleRender(@NonNull RenderPreview preview) {
        mPendingRenderCount++;
        preview.render(mPendingRenderCount * RENDER_DELAY);
    }

    /**
     * Switch to the given configuration preview
     *
     * @param preview the preview to switch to
     */
    public void switchTo(@NonNull RenderPreview preview) {
        IFile input = preview.getAlternateInput();
        if (input != null) {
            IWorkbenchPartSite site = mCanvas.getEditorDelegate().getEditor().getSite();
            try {
                // This switches to the given file, but the file might not have
                // an identical configuration to what was shown in the preview.
                // For example, while viewing a 10" layout-xlarge file, it might
                // show a preview for a 5" version tied to the default layout. If
                // you click on it, it will open the default layout file, but it might
                // be using a different screen size; any of those that match the
                // default layout, say a 3.8".
                //
                // Thus, we need to also perform a screen size sync first
                Configuration configuration = preview.getConfiguration();
                boolean setSize = false;
                if (configuration instanceof NestedConfiguration) {
                    NestedConfiguration nestedConfig = (NestedConfiguration) configuration;
                    setSize = nestedConfig.isOverridingDevice();
                    if (configuration instanceof VaryingConfiguration) {
                        VaryingConfiguration c = (VaryingConfiguration) configuration;
                        setSize |= c.isAlternatingDevice();
                    }

                    if (setSize) {
                        ConfigurationChooser chooser = getChooser();
                        IFile editedFile = chooser.getEditedFile();
                        if (editedFile != null) {
                            chooser.syncToVariations(CFG_DEVICE|CFG_DEVICE_STATE,
                                    editedFile, configuration, false, false);
                        }
                    }
                }

                IDE.openEditor(site.getWorkbenchWindow().getActivePage(), input,
                        CommonXmlEditor.ID);
            } catch (PartInitException e) {
                AdtPlugin.log(e, null);
            }
            return;
        }

        GraphicalEditorPart editor = mCanvas.getEditorDelegate().getGraphicalEditor();
        ConfigurationChooser chooser = editor.getConfigurationChooser();

        Configuration originalConfiguration = chooser.getConfiguration();

        // The new configuration is the configuration which will become the configuration
        // in the layout editor's chooser
        Configuration previewConfiguration = preview.getConfiguration();
        Configuration newConfiguration = previewConfiguration;
        if (newConfiguration instanceof NestedConfiguration) {
            // Should never use a complementing configuration for the main
            // rendering's configuration; instead, create a new configuration
            // with a snapshot of the configuration's current values
            newConfiguration = Configuration.copy(previewConfiguration);

            // Remap all the previews to be parented to this new copy instead
            // of the old one (which is no longer controlled by the chooser)
            for (RenderPreview p : mPreviews) {
                Configuration configuration = p.getConfiguration();
                if (configuration instanceof NestedConfiguration) {
                    NestedConfiguration nested = (NestedConfiguration) configuration;
                    nested.setParent(newConfiguration);
                }
            }
        }

        // Make a preview for the configuration which *was* showing in the
        // chooser up until this point:
        RenderPreview newPreview = mCanvas.getPreview();
        if (newPreview == null) {
            newPreview = RenderPreview.create(this, originalConfiguration);
        }

        // Update its configuration such that it is complementing or inheriting
        // from the new chosen configuration
        if (previewConfiguration instanceof VaryingConfiguration) {
            VaryingConfiguration varying = VaryingConfiguration.create(
                    (VaryingConfiguration) previewConfiguration,
                    newConfiguration);
            varying.updateDisplayName();
            originalConfiguration = varying;
            newPreview.setConfiguration(originalConfiguration);
        } else if (previewConfiguration instanceof NestedConfiguration) {
            NestedConfiguration nested = NestedConfiguration.create(
                    (NestedConfiguration) previewConfiguration,
                    originalConfiguration,
                    newConfiguration);
            nested.setDisplayName(nested.computeDisplayName());
            originalConfiguration = nested;
            newPreview.setConfiguration(originalConfiguration);
        }

        // Replace clicked preview with preview of the formerly edited main configuration
        // This doesn't work yet because the image overlay has had its image
        // replaced by the configuration previews! I should make a list of them
        //newPreview.setFullImage(mImageOverlay.getAwtImage());
        for (int i = 0, n = mPreviews.size(); i < n; i++) {
            if (preview == mPreviews.get(i)) {
                mPreviews.set(i, newPreview);
                break;
            }
        }

        // Stash the corresponding preview (not active) on the canvas so we can
        // retrieve it if clicking to some other preview later
        mCanvas.setPreview(preview);
        preview.setVisible(false);

        // Switch to the configuration from the clicked preview (though it's
        // most likely a copy, see above)
        chooser.setConfiguration(newConfiguration);
        editor.changed(MASK_ALL);

        // Scroll to the top again, if necessary
        mCanvas.getVerticalBar().setSelection(mCanvas.getVerticalBar().getMinimum());

        mNeedLayout = mNeedZoom = true;
        mCanvas.redraw();
        mAnimation = new SwapAnimation(preview, newPreview);
    }

    /**
     * Gets the preview at the given location, or null if none. This is
     * currently deeply tied to where things are painted in onPaint().
     */
    RenderPreview getPreview(ControlPoint mousePos) {
        if (hasPreviews()) {
            int rootX = getX();
            if (mousePos.x < rootX) {
                return null;
            }
            int rootY = getY();

            for (RenderPreview preview : mPreviews) {
                int x = rootX + preview.getX();
                int y = rootY + preview.getY();
                if (mousePos.x >= x && mousePos.x <= x + preview.getWidth()) {
                    if (mousePos.y >= y && mousePos.y <= y + preview.getHeight()) {
                        return preview;
                    }
                }
            }
        }

        return null;
    }

    private int getX() {
        return mHScale.translate(0);
    }

    private int getY() {
        return mVScale.translate(0);
    }

    private int getZoomX() {
        Rectangle clientArea = mCanvas.getClientArea();
        int x = clientArea.x + clientArea.width - ZOOM_ICON_WIDTH;
        if (x < mHScale.getScaledImgSize() + PREVIEW_HGAP) {
            // No visible previews because the main image is zoomed too far
            return -1;
        }

        return x - 6;
    }

    private int getZoomY() {
        Rectangle clientArea = mCanvas.getClientArea();
        return clientArea.y + 5;
    }

    /**
     * Returns the height of the layout
     *
     * @return the height
     */
    public int getHeight() {
        return mLayoutHeight;
    }

    /**
     * Notifies that preview manager that the mouse cursor has moved to the
     * given control position within the layout canvas
     *
     * @param mousePos the mouse position, relative to the layout canvas
     */
    public void moved(ControlPoint mousePos) {
        RenderPreview hovered = getPreview(mousePos);
        if (hovered != mActivePreview) {
            if (mActivePreview != null) {
                mActivePreview.setActive(false);
            }
            mActivePreview = hovered;
            if (mActivePreview != null) {
                mActivePreview.setActive(true);
            }
            mCanvas.redraw();
        }
    }

    /**
     * Notifies that preview manager that the mouse cursor has entered the layout canvas
     *
     * @param mousePos the mouse position, relative to the layout canvas
     */
    public void enter(ControlPoint mousePos) {
        moved(mousePos);
    }

    /**
     * Notifies that preview manager that the mouse cursor has exited the layout canvas
     *
     * @param mousePos the mouse position, relative to the layout canvas
     */
    public void exit(ControlPoint mousePos) {
        if (mActivePreview != null) {
            mActivePreview.setActive(false);
        }
        mActivePreview = null;
        mCanvas.redraw();
    }

    /**
     * Process a mouse click, and return true if it was handled by this manager
     * (e.g. the click was on a preview)
     *
     * @param mousePos the mouse position where the click occurred
     * @return true if the click occurred over a preview and was handled, false otherwise
     */
    public boolean click(ControlPoint mousePos) {
        // Clicked zoom?
        int x = getZoomX();
        if (x > 0) {
            if (mousePos.x >= x && mousePos.x <= x + ZOOM_ICON_WIDTH) {
                int y = getZoomY();
                if (mousePos.y >= y && mousePos.y <= y + 4 * ZOOM_ICON_HEIGHT) {
                    if (mousePos.y < y + ZOOM_ICON_HEIGHT) {
                        zoomIn();
                    } else if (mousePos.y < y + 2 * ZOOM_ICON_HEIGHT) {
                        zoomOut();
                    } else if (mousePos.y < y + 3 * ZOOM_ICON_HEIGHT) {
                        zoomReset();
                    } else {
                        selectMode(NONE);
                    }
                    return true;
                }
            }
        }

        RenderPreview preview = getPreview(mousePos);
        if (preview != null) {
            boolean handled = preview.click(mousePos.x - getX() - preview.getX(),
                    mousePos.y - getY() - preview.getY());
            if (handled) {
                // In case layout was performed, there could be a new preview
                // under this coordinate now, so make sure it's hover etc
                // shows up
                moved(mousePos);
                return true;
            }
        }

        return false;
    }

    /**
     * Returns true if there are thumbnail previews
     *
     * @return true if thumbnails are being shown
     */
    public boolean hasPreviews() {
        return mPreviews != null && !mPreviews.isEmpty();
    }


    private void sortPreviewsByScreenSize() {
        if (mPreviews != null) {
            Collections.sort(mPreviews, new Comparator<RenderPreview>() {
                @Override
                public int compare(RenderPreview preview1, RenderPreview preview2) {
                    Configuration config1 = preview1.getConfiguration();
                    Configuration config2 = preview2.getConfiguration();
                    Device device1 = config1.getDevice();
                    Device device2 = config1.getDevice();
                    if (device1 != null && device2 != null) {
                        Screen screen1 = device1.getDefaultHardware().getScreen();
                        Screen screen2 = device2.getDefaultHardware().getScreen();
                        if (screen1 != null && screen2 != null) {
                            double delta = screen1.getDiagonalLength()
                                    - screen2.getDiagonalLength();
                            if (delta != 0.0) {
                                return (int) Math.signum(delta);
                            } else {
                                if (screen1.getPixelDensity() != screen2.getPixelDensity()) {
                                    return screen1.getPixelDensity().compareTo(
                                            screen2.getPixelDensity());
                                }
                            }
                        }

                    }
                    State state1 = config1.getDeviceState();
                    State state2 = config2.getDeviceState();
                    if (state1 != state2 && state1 != null && state2 != null) {
                        return state1.getName().compareTo(state2.getName());
                    }

                    return preview1.getDisplayName().compareTo(preview2.getDisplayName());
                }
            });
        }
    }

    private void sortPreviewsByOrientation() {
        if (mPreviews != null) {
            Collections.sort(mPreviews, new Comparator<RenderPreview>() {
                @Override
                public int compare(RenderPreview preview1, RenderPreview preview2) {
                    Configuration config1 = preview1.getConfiguration();
                    Configuration config2 = preview2.getConfiguration();
                    State state1 = config1.getDeviceState();
                    State state2 = config2.getDeviceState();
                    if (state1 != state2 && state1 != null && state2 != null) {
                        return state1.getName().compareTo(state2.getName());
                    }

                    return preview1.getDisplayName().compareTo(preview2.getDisplayName());
                }
            });
        }
    }

    /**
     * Vertical scrollbar listener which updates render previews which are not
     * visible and triggers a redraw
     */
    private class ScrollBarListener implements SelectionListener {
        @Override
        public void widgetSelected(SelectionEvent e) {
            if (mPreviews == null) {
                return;
            }

            ScrollBar bar = mCanvas.getVerticalBar();
            int selection = bar.getSelection();
            int thumb = bar.getThumb();
            int maxY = selection + thumb;
            beginRenderScheduling();
            for (RenderPreview preview : mPreviews) {
                if (!preview.isVisible() && preview.getY() <= maxY) {
                    preview.setVisible(true);
                }
            }
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent e) {
        }
    }

    /** Animation overlay shown briefly after swapping two previews */
    private class SwapAnimation implements Runnable {
        private long begin;
        private long end;
        private static final long DURATION = 400; // ms
        private Rect initialRect1;
        private Rect targetRect1;
        private Rect initialRect2;
        private Rect targetRect2;
        private RenderPreview preview;

        SwapAnimation(RenderPreview preview1, RenderPreview preview2) {
            begin = System.currentTimeMillis();
            end = begin + DURATION;

            initialRect1 = new Rect(preview1.getX(), preview1.getY(),
                    preview1.getWidth(), preview1.getHeight());

            CanvasTransform hi = mCanvas.getHorizontalTransform();
            CanvasTransform vi = mCanvas.getVerticalTransform();
            initialRect2 = new Rect(hi.translate(0), vi.translate(0),
                    hi.getScaledImgSize(), vi.getScaledImgSize());
            preview = preview2;
        }

        void tick(GC gc) {
            long now = System.currentTimeMillis();
            if (now > end || mCanvas.isDisposed()) {
                mAnimation = null;
                return;
            }

            CanvasTransform hi = mCanvas.getHorizontalTransform();
            CanvasTransform vi = mCanvas.getVerticalTransform();
            if (targetRect1 == null) {
                targetRect1 = new Rect(hi.translate(0), vi.translate(0),
                    hi.getScaledImgSize(), vi.getScaledImgSize());
            }
            double portion = (now - begin) / (double) DURATION;
            Rect rect1 = new Rect(
                    (int) (portion * (targetRect1.x - initialRect1.x) + initialRect1.x),
                    (int) (portion * (targetRect1.y - initialRect1.y) + initialRect1.y),
                    (int) (portion * (targetRect1.w - initialRect1.w) + initialRect1.w),
                    (int) (portion * (targetRect1.h - initialRect1.h) + initialRect1.h));

            if (targetRect2 == null) {
                targetRect2 = new Rect(preview.getX(), preview.getY(),
                        preview.getWidth(), preview.getHeight());
            }
            portion = (now - begin) / (double) DURATION;
            Rect rect2 = new Rect(
                (int) (portion * (targetRect2.x - initialRect2.x) + initialRect2.x),
                (int) (portion * (targetRect2.y - initialRect2.y) + initialRect2.y),
                (int) (portion * (targetRect2.w - initialRect2.w) + initialRect2.w),
                (int) (portion * (targetRect2.h - initialRect2.h) + initialRect2.h));

            gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_GRAY));
            gc.drawRectangle(rect1.x, rect1.y, rect1.w, rect1.h);
            gc.drawRectangle(rect2.x, rect2.y, rect2.w, rect2.h);

            mCanvas.getDisplay().timerExec(5, this);
        }

        @Override
        public void run() {
            mCanvas.redraw();
        }
    }

    /**
     * Notifies the {@linkplain RenderPreviewManager} that the configuration used
     * in the main chooser has been changed. This may require updating parent references
     * in the preview configurations inheriting from it.
     *
     * @param oldConfiguration the previous configuration
     * @param newConfiguration the new configuration in the chooser
     */
    public void updateChooserConfig(
            @NonNull Configuration oldConfiguration,
            @NonNull Configuration newConfiguration) {
        if (hasPreviews()) {
            for (RenderPreview preview : mPreviews) {
                Configuration configuration = preview.getConfiguration();
                if (configuration instanceof NestedConfiguration) {
                    NestedConfiguration nestedConfig = (NestedConfiguration) configuration;
                    if (nestedConfig.getParent() == oldConfiguration) {
                        nestedConfig.setParent(newConfiguration);
                    }
                }
            }
        }
    }
}

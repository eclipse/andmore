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

import static com.android.SdkConstants.ANDROID_STYLE_RESOURCE_PREFIX;
import static com.android.SdkConstants.PREFIX_RESOURCE_REF;
import static com.android.SdkConstants.STYLE_RESOURCE_PREFIX;
import static org.eclipse.andmore.internal.editors.layout.configuration.Configuration.MASK_RENDERING;
import static org.eclipse.andmore.internal.editors.layout.gle2.ImageUtils.SHADOW_SIZE;
import static org.eclipse.andmore.internal.editors.layout.gle2.ImageUtils.SMALL_SHADOW_SIZE;
import static org.eclipse.andmore.internal.editors.layout.gle2.RenderPreviewMode.DEFAULT;
import static org.eclipse.andmore.internal.editors.layout.gle2.RenderPreviewMode.INCLUDES;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.rendering.api.RenderSession;
import com.android.ide.common.rendering.api.ResourceValue;
import com.android.ide.common.rendering.api.Result;
import com.android.ide.common.rendering.api.Result.Status;
import com.android.ide.common.resources.ResourceFile;
import com.android.ide.common.resources.ResourceRepository;
import com.android.ide.common.resources.ResourceResolver;
import com.android.ide.common.resources.configuration.FolderConfiguration;
import com.android.ide.common.resources.configuration.ScreenOrientationQualifier;
import com.android.io.IAbstractFile;
import com.android.resources.Density;
import com.android.resources.ResourceType;
import com.android.resources.ScreenOrientation;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.devices.Device;
import com.android.sdklib.devices.Screen;
import com.android.sdklib.devices.State;
import com.android.utils.SdkUtils;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.AdtUtils;
import org.eclipse.andmore.internal.editors.IconFactory;
import org.eclipse.andmore.internal.editors.descriptors.DocumentDescriptor;
import org.eclipse.andmore.internal.editors.layout.configuration.Configuration;
import org.eclipse.andmore.internal.editors.layout.configuration.ConfigurationChooser;
import org.eclipse.andmore.internal.editors.layout.configuration.ConfigurationClient;
import org.eclipse.andmore.internal.editors.layout.configuration.ConfigurationDescription;
import org.eclipse.andmore.internal.editors.layout.configuration.Locale;
import org.eclipse.andmore.internal.editors.layout.configuration.NestedConfiguration;
import org.eclipse.andmore.internal.editors.layout.configuration.VaryingConfiguration;
import org.eclipse.andmore.internal.editors.layout.gle2.IncludeFinder.Reference;
import org.eclipse.andmore.internal.editors.uimodel.UiDocumentNode;
import org.eclipse.andmore.internal.resources.ResourceHelper;
import org.eclipse.andmore.internal.resources.manager.ProjectResources;
import org.eclipse.andmore.internal.resources.manager.ResourceManager;
import org.eclipse.andmore.internal.sdk.AndroidTargetData;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.andmore.io.IFileWrapper;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Region;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.UIJob;
import org.w3c.dom.Document;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.ref.SoftReference;
import java.util.Comparator;
import java.util.Map;

/**
 * Represents a preview rendering of a given configuration
 */
public class RenderPreview implements IJobChangeListener {
    /** Whether previews should use large shadows */
    static final boolean LARGE_SHADOWS = false;

    /**
     * Still doesn't work; get exceptions from layoutlib:
     * java.lang.IllegalStateException: After scene creation, #init() must be called
     *   at com.android.layoutlib.bridge.impl.RenderAction.acquire(RenderAction.java:151)
     * <p>
     * TODO: Investigate.
     */
    private static final boolean RENDER_ASYNC = false;

    /**
     * Height of the toolbar shown over a preview during hover. Needs to be
     * large enough to accommodate icons below.
     */
    private static final int HEADER_HEIGHT = 20;

    /** Whether to dump out rendering failures of the previews to the log */
    private static final boolean DUMP_RENDER_DIAGNOSTICS = false;

    /** Extra error checking in debug mode */
    private static final boolean DEBUG = false;

    private static final Image EDIT_ICON;
    private static final Image ZOOM_IN_ICON;
    private static final Image ZOOM_OUT_ICON;
    private static final Image CLOSE_ICON;
    private static final int EDIT_ICON_WIDTH;
    private static final int ZOOM_IN_ICON_WIDTH;
    private static final int ZOOM_OUT_ICON_WIDTH;
    private static final int CLOSE_ICON_WIDTH;
    static {
        ISharedImages sharedImages = PlatformUI.getWorkbench().getSharedImages();
        IconFactory icons = IconFactory.getInstance();
        CLOSE_ICON = sharedImages.getImage(ISharedImages.IMG_ETOOL_DELETE);
        EDIT_ICON = icons.getIcon("editPreview");   //$NON-NLS-1$
        ZOOM_IN_ICON = icons.getIcon("zoomplus");   //$NON-NLS-1$
        ZOOM_OUT_ICON = icons.getIcon("zoomminus"); //$NON-NLS-1$
        CLOSE_ICON_WIDTH = CLOSE_ICON.getImageData().width;
        EDIT_ICON_WIDTH = EDIT_ICON.getImageData().width;
        ZOOM_IN_ICON_WIDTH = ZOOM_IN_ICON.getImageData().width;
        ZOOM_OUT_ICON_WIDTH = ZOOM_OUT_ICON.getImageData().width;
    }

    /** The configuration being previewed */
    private @NonNull Configuration mConfiguration;

    /** Configuration to use if we have an alternate input to be rendered */
    private @NonNull Configuration mAlternateConfiguration;

    /** The associated manager */
    private final @NonNull RenderPreviewManager mManager;
    private final @NonNull LayoutCanvas mCanvas;

    private @NonNull SoftReference<ResourceResolver> mResourceResolver =
            new SoftReference<ResourceResolver>(null);
    private @Nullable Job mJob;
    private @Nullable Image mThumbnail;
    private @Nullable String mDisplayName;
    private int mWidth;
    private int mHeight;
    private int mX;
    private int mY;
    private int mTitleHeight;
    private double mScale = 1.0;
    private double mAspectRatio;

    /** If non null, points to a separate file containing the source */
    private @Nullable IFile mAlternateInput;

    /** If included within another layout, the name of that outer layout */
    private @Nullable Reference mIncludedWithin;

    /** Whether the mouse is actively hovering over this preview */
    private boolean mActive;

    /**
     * Whether this preview cannot be rendered because of a model error - such
     * as an invalid configuration, a missing resource, an error in the XML
     * markup, etc. If non null, contains the error message (or a blank string
     * if not known), and null if the render was successful.
     */
    private String mError;

    /** Whether in the current layout, this preview is visible */
    private boolean mVisible;

    /** Whether the configuration has changed and needs to be refreshed the next time
     * this preview made visible. This corresponds to the change flags in
     * {@link ConfigurationClient}. */
    private int mDirty;

    /**
     * Creates a new {@linkplain RenderPreview}
     *
     * @param manager the manager
     * @param canvas canvas where preview is painted
     * @param configuration the associated configuration
     * @param width the initial width to use for the preview
     * @param height the initial height to use for the preview
     */
    private RenderPreview(
            @NonNull RenderPreviewManager manager,
            @NonNull LayoutCanvas canvas,
            @NonNull Configuration configuration) {
        mManager = manager;
        mCanvas = canvas;
        mConfiguration = configuration;
        updateSize();

        // Should only attempt to create configurations for fully configured devices
        assert mConfiguration.getDevice() != null
                && mConfiguration.getDeviceState() != null
                && mConfiguration.getLocale() != null
                && mConfiguration.getTarget() != null
                && mConfiguration.getTheme() != null
                && mConfiguration.getFullConfig() != null
                && mConfiguration.getFullConfig().getScreenSizeQualifier() != null :
                    mConfiguration;
    }

    /**
     * Sets the configuration to use for this preview
     *
     * @param configuration the new configuration
     */
    public void setConfiguration(@NonNull Configuration configuration) {
        mConfiguration = configuration;
    }

    /**
     * Gets the scale being applied to the thumbnail
     *
     * @return the scale being applied to the thumbnail
     */
    public double getScale() {
        return mScale;
    }

    /**
     * Sets the scale to apply to the thumbnail
     *
     * @param scale the factor to scale the thumbnail picture by
     */
    public void setScale(double scale) {
        disposeThumbnail();
        mScale = scale;
    }

    /**
     * Returns the aspect ratio of this render preview
     *
     * @return the aspect ratio
     */
    public double getAspectRatio() {
        return mAspectRatio;
    }

    /**
     * Returns whether the preview is actively hovered
     *
     * @return whether the mouse is hovering over the preview
     */
    public boolean isActive() {
        return mActive;
    }

    /**
     * Sets whether the preview is actively hovered
     *
     * @param active if the mouse is hovering over the preview
     */
    public void setActive(boolean active) {
        mActive = active;
    }

    /**
     * Returns whether the preview is visible. Previews that are off
     * screen are typically marked invisible during layout, which means we don't
     * have to expend effort computing preview thumbnails etc
     *
     * @return true if the preview is visible
     */
    public boolean isVisible() {
        return mVisible;
    }

    /**
     * Returns whether this preview represents a forked layout
     *
     * @return true if this preview represents a separate file
     */
    public boolean isForked() {
        return mAlternateInput != null || mIncludedWithin != null;
    }

    /**
     * Returns the file to be used for this preview, or null if this is not a
     * forked layout meaning that the file is the one used in the chooser
     *
     * @return the file or null for non-forked layouts
     */
    @Nullable
    public IFile getAlternateInput() {
        if (mAlternateInput != null) {
            return mAlternateInput;
        } else if (mIncludedWithin != null) {
            return mIncludedWithin.getFile();
        }

        return null;
    }

    /**
     * Returns the area of this render preview, PRIOR to scaling
     *
     * @return the area (width times height without scaling)
     */
    int getArea() {
        return mWidth * mHeight;
    }

    /**
     * Sets whether the preview is visible. Previews that are off
     * screen are typically marked invisible during layout, which means we don't
     * have to expend effort computing preview thumbnails etc
     *
     * @param visible whether this preview is visible
     */
    public void setVisible(boolean visible) {
        if (visible != mVisible) {
            mVisible = visible;
            if (mVisible) {
                if (mDirty != 0) {
                    // Just made the render preview visible:
                    configurationChanged(mDirty); // schedules render
                } else {
                    updateForkStatus();
                    mManager.scheduleRender(this);
                }
            } else {
                dispose();
            }
        }
    }

    /**
     * Sets the layout position relative to the top left corner of the preview
     * area, in control coordinates
     */
    void setPosition(int x, int y) {
        mX = x;
        mY = y;
    }

    /**
     * Gets the layout X position relative to the top left corner of the preview
     * area, in control coordinates
     */
    int getX() {
        return mX;
    }

    /**
     * Gets the layout Y position relative to the top left corner of the preview
     * area, in control coordinates
     */
    int getY() {
        return mY;
    }

    /** Determine whether this configuration has a better match in a different layout file */
    private void updateForkStatus() {
        ConfigurationChooser chooser = mManager.getChooser();
        FolderConfiguration config = mConfiguration.getFullConfig();
        if (mAlternateInput != null && chooser.isBestMatchFor(mAlternateInput, config)) {
            return;
        }

        mAlternateInput = null;
        IFile editedFile = chooser.getEditedFile();
        if (editedFile != null) {
            if (!chooser.isBestMatchFor(editedFile, config)) {
                ProjectResources resources = chooser.getResources();
                if (resources != null) {
                    ResourceFile best = resources.getMatchingFile(editedFile.getName(),
                            ResourceType.LAYOUT, config);
                    if (best != null) {
                        IAbstractFile file = best.getFile();
                        if (file instanceof IFileWrapper) {
                            mAlternateInput = ((IFileWrapper) file).getIFile();
                        } else if (file instanceof File) {
                            mAlternateInput = AdtUtils.fileToIFile(((File) file));
                        }
                    }
                }
                if (mAlternateInput != null) {
                    mAlternateConfiguration = Configuration.create(mConfiguration,
                            mAlternateInput);
                }
            }
        }
    }

    /**
     * Creates a new {@linkplain RenderPreview}
     *
     * @param manager the manager
     * @param configuration the associated configuration
     * @return a new configuration
     */
    @NonNull
    public static RenderPreview create(
            @NonNull RenderPreviewManager manager,
            @NonNull Configuration configuration) {
        LayoutCanvas canvas = manager.getCanvas();
        return new RenderPreview(manager, canvas, configuration);
    }

    /**
     * Throws away this preview: cancels any pending rendering jobs and disposes
     * of image resources etc
     */
    public void dispose() {
        disposeThumbnail();

        if (mJob != null) {
            mJob.cancel();
            mJob = null;
        }
    }

    /** Disposes the thumbnail rendering. */
    void disposeThumbnail() {
        if (mThumbnail != null) {
            mThumbnail.dispose();
            mThumbnail = null;
        }
    }

    /**
     * Returns the display name of this preview
     *
     * @return the name of the preview
     */
    @NonNull
    public String getDisplayName() {
        if (mDisplayName == null) {
            String displayName = getConfiguration().getDisplayName();
            if (displayName == null) {
                // No display name: this must be the configuration used by default
                // for the view which is originally displayed (before adding thumbnails),
                // and you've switched away to something else; now we need to display a name
                // for this original configuration. For now, just call it "Original"
                return "Original";
            }

            return displayName;
        }

        return mDisplayName;
    }

    /**
     * Sets the display name of this preview. By default, the display name is
     * the display name of the configuration, but it can be overridden by calling
     * this setter (which only sets the preview name, without editing the configuration.)
     *
     * @param displayName the new display name
     */
    public void setDisplayName(@NonNull String displayName) {
        mDisplayName = displayName;
    }

    /**
     * Sets an inclusion context to use for this layout, if any. This will render
     * the configuration preview as the outer layout with the current layout
     * embedded within.
     *
     * @param includedWithin a reference to a layout which includes this one
     */
    public void setIncludedWithin(Reference includedWithin) {
        mIncludedWithin = includedWithin;
    }

    /**
     * Request a new render after the given delay
     *
     * @param delay the delay to wait before starting the render job
     */
    public void render(long delay) {
        Job job = mJob;
        if (job != null) {
            job.cancel();
        }
        if (RENDER_ASYNC) {
            job = new AsyncRenderJob();
        } else {
            job = new RenderJob();
        }
        job.schedule(delay);
        job.addJobChangeListener(this);
        mJob = job;
    }

    /** Render immediately */
    private void renderSync() {
        GraphicalEditorPart editor = mCanvas.getEditorDelegate().getGraphicalEditor();
        if (editor.getReadyLayoutLib(false /*displayError*/) == null) {
            // Don't attempt to render when there is no ready layout library: most likely
            // the targets are loading/reloading.
            return;
        }

        disposeThumbnail();

        Configuration configuration =
                mAlternateInput != null && mAlternateConfiguration != null
                ? mAlternateConfiguration : mConfiguration;
        ResourceResolver resolver = getResourceResolver(configuration);
        RenderService renderService = RenderService.create(editor, configuration, resolver);

        if (mIncludedWithin != null) {
            renderService.setIncludedWithin(mIncludedWithin);
        }

        if (mAlternateInput != null) {
            IAndroidTarget target = editor.getRenderingTarget();
            AndroidTargetData data = null;
            if (target != null) {
                Sdk sdk = Sdk.getCurrent();
                if (sdk != null) {
                    data = sdk.getTargetData(target);
                }
            }

            // Construct UI model from XML
            DocumentDescriptor documentDescriptor;
            if (data == null) {
                documentDescriptor = new DocumentDescriptor("temp", null);//$NON-NLS-1$
            } else {
                documentDescriptor = data.getLayoutDescriptors().getDescriptor();
            }
            UiDocumentNode model = (UiDocumentNode) documentDescriptor.createUiNode();
            model.setEditor(mCanvas.getEditorDelegate().getEditor());
            model.setUnknownDescriptorProvider(editor.getModel().getUnknownDescriptorProvider());

            Document document = DomUtilities.getDocument(mAlternateInput);
            if (document == null) {
                mError = "No document";
                createErrorThumbnail();
                return;
            }
            model.loadFromXmlNode(document);
            renderService.setModel(model);
        } else {
            renderService.setModel(editor.getModel());
        }
        RenderLogger log = editor.createRenderLogger(getDisplayName());
        renderService.setLog(log);
        RenderSession session = renderService.createRenderSession();
        Result render = session.render(1000);

        if (DUMP_RENDER_DIAGNOSTICS) {
            if (log.hasProblems() || !render.isSuccess()) {
                AdtPlugin.log(IStatus.ERROR, "Found problems rendering preview "
                        + getDisplayName() + ": "
                        + render.getErrorMessage() + " : "
                        + log.getProblems(false));
                Throwable exception = render.getException();
                if (exception != null) {
                    AdtPlugin.log(exception, "Failure rendering preview " + getDisplayName());
                }
            }
        }

        if (render.isSuccess()) {
            mError = null;
        } else {
            mError = render.getErrorMessage();
            if (mError == null) {
                mError = "";
            }
        }

        if (render.getStatus() == Status.ERROR_TIMEOUT) {
            // TODO: Special handling? schedule update again later
            return;
        }
        if (render.isSuccess()) {
            BufferedImage image = session.getImage();
            if (image != null) {
                createThumbnail(image);
            }
        }

        if (mError != null) {
            createErrorThumbnail();
        }
    }

    private ResourceResolver getResourceResolver(Configuration configuration) {
        ResourceResolver resourceResolver = mResourceResolver.get();
        if (resourceResolver != null) {
            return resourceResolver;
        }

        GraphicalEditorPart graphicalEditor = mCanvas.getEditorDelegate().getGraphicalEditor();
        String theme = configuration.getTheme();
        if (theme == null) {
            return null;
        }

        Map<ResourceType, Map<String, ResourceValue>> configuredFrameworkRes = null;
        Map<ResourceType, Map<String, ResourceValue>> configuredProjectRes = null;

        FolderConfiguration config = configuration.getFullConfig();
        IAndroidTarget target = graphicalEditor.getRenderingTarget();
        ResourceRepository frameworkRes = null;
        if (target != null) {
            Sdk sdk = Sdk.getCurrent();
            if (sdk == null) {
                return null;
            }
            AndroidTargetData data = sdk.getTargetData(target);

            if (data != null) {
                // TODO: SHARE if possible
                frameworkRes = data.getFrameworkResources();
                configuredFrameworkRes = frameworkRes.getConfiguredResources(config);
            } else {
                return null;
            }
        } else {
            return null;
        }
        assert configuredFrameworkRes != null;


        // get the resources of the file's project.
        ProjectResources projectRes = ResourceManager.getInstance().getProjectResources(
                graphicalEditor.getProject());
        configuredProjectRes = projectRes.getConfiguredResources(config);

        if (!theme.startsWith(PREFIX_RESOURCE_REF)) {
            if (frameworkRes.hasResourceItem(ANDROID_STYLE_RESOURCE_PREFIX + theme)) {
                theme = ANDROID_STYLE_RESOURCE_PREFIX + theme;
            } else {
                theme = STYLE_RESOURCE_PREFIX + theme;
            }
        }

        resourceResolver = ResourceResolver.create(
                configuredProjectRes, configuredFrameworkRes,
                ResourceHelper.styleToTheme(theme),
                ResourceHelper.isProjectStyle(theme));
        mResourceResolver = new SoftReference<ResourceResolver>(resourceResolver);
        return resourceResolver;
    }

    /**
     * Sets the new image of the preview and generates a thumbnail
     *
     * @param image the full size image
     */
    void createThumbnail(BufferedImage image) {
        if (image == null) {
            mThumbnail = null;
            return;
        }

        ImageOverlay imageOverlay = mCanvas.getImageOverlay();
        boolean drawShadows = imageOverlay == null || imageOverlay.getShowDropShadow();
        double scale = getWidth() / (double) image.getWidth();
        int shadowSize;
        if (LARGE_SHADOWS) {
            shadowSize = drawShadows ? SHADOW_SIZE : 0;
        } else {
            shadowSize = drawShadows ? SMALL_SHADOW_SIZE : 0;
        }
        if (scale < 1.0) {
            if (LARGE_SHADOWS) {
                image = ImageUtils.scale(image, scale, scale,
                        shadowSize, shadowSize);
                if (drawShadows) {
                    ImageUtils.drawRectangleShadow(image, 0, 0,
                            image.getWidth() - shadowSize,
                            image.getHeight() - shadowSize);
                }
            } else {
                image = ImageUtils.scale(image, scale, scale,
                        shadowSize, shadowSize);
                if (drawShadows) {
                    ImageUtils.drawSmallRectangleShadow(image, 0, 0,
                            image.getWidth() - shadowSize,
                            image.getHeight() - shadowSize);
                }
            }
        }

        mThumbnail = SwtUtils.convertToSwt(mCanvas.getDisplay(), image,
                true /* transferAlpha */, -1);
    }

    void createErrorThumbnail() {
        int shadowSize = LARGE_SHADOWS ? SHADOW_SIZE : SMALL_SHADOW_SIZE;
        int width = getWidth();
        int height = getHeight();
        BufferedImage image = new BufferedImage(width + shadowSize, height + shadowSize,
                BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = image.createGraphics();
        g.setColor(new java.awt.Color(0xfffbfcc6));
        g.fillRect(0, 0, width, height);

        g.dispose();

        ImageOverlay imageOverlay = mCanvas.getImageOverlay();
        boolean drawShadows = imageOverlay == null || imageOverlay.getShowDropShadow();
        if (drawShadows) {
            if (LARGE_SHADOWS) {
                ImageUtils.drawRectangleShadow(image, 0, 0,
                        image.getWidth() - SHADOW_SIZE,
                        image.getHeight() - SHADOW_SIZE);
            } else {
                ImageUtils.drawSmallRectangleShadow(image, 0, 0,
                        image.getWidth() - SMALL_SHADOW_SIZE,
                        image.getHeight() - SMALL_SHADOW_SIZE);
            }
        }

        mThumbnail = SwtUtils.convertToSwt(mCanvas.getDisplay(), image,
                true /* transferAlpha */, -1);
    }

    private static double getScale(int width, int height) {
        int maxWidth = RenderPreviewManager.getMaxWidth();
        int maxHeight = RenderPreviewManager.getMaxHeight();
        if (width > 0 && height > 0
                && (width > maxWidth || height > maxHeight)) {
            if (width >= height) { // landscape
                return maxWidth / (double) width;
            } else { // portrait
                return maxHeight / (double) height;
            }
        }

        return 1.0;
    }

    /**
     * Returns the width of the preview, in pixels
     *
     * @return the width in pixels
     */
    public int getWidth() {
        return (int) (mWidth * mScale * RenderPreviewManager.getScale());
    }

    /**
     * Returns the height of the preview, in pixels
     *
     * @return the height in pixels
     */
    public int getHeight() {
        return (int) (mHeight * mScale * RenderPreviewManager.getScale());
    }

    /**
     * Handles clicks within the preview (x and y are positions relative within the
     * preview
     *
     * @param x the x coordinate within the preview where the click occurred
     * @param y the y coordinate within the preview where the click occurred
     * @return true if this preview handled (and therefore consumed) the click
     */
    public boolean click(int x, int y) {
        if (y >= mTitleHeight && y < mTitleHeight + HEADER_HEIGHT) {
            int left = 0;
            left += CLOSE_ICON_WIDTH;
            if (x <= left) {
                // Delete
                mManager.deletePreview(this);
                return true;
            }
            left += ZOOM_IN_ICON_WIDTH;
            if (x <= left) {
                // Zoom in
                mScale = mScale * (1 / 0.5);
                if (Math.abs(mScale-1.0) < 0.0001) {
                    mScale = 1.0;
                }

                render(0);
                mManager.layout(true);
                mCanvas.redraw();
                return true;
            }
            left += ZOOM_OUT_ICON_WIDTH;
            if (x <= left) {
                // Zoom out
                mScale = mScale * (0.5 / 1);
                if (Math.abs(mScale-1.0) < 0.0001) {
                    mScale = 1.0;
                }
                render(0);

                mManager.layout(true);
                mCanvas.redraw();
                return true;
            }
            left += EDIT_ICON_WIDTH;
            if (x <= left) {
                // Edit. For now, just rename
                InputDialog d = new InputDialog(
                        AdtPlugin.getShell(),
                        "Rename Preview",  // title
                        "Name:",
                        getDisplayName(),
                        null);
                if (d.open() == Window.OK) {
                    String newName = d.getValue();
                    mConfiguration.setDisplayName(newName);
                    if (mDescription != null) {
                        mManager.rename(mDescription, newName);
                    }
                    mCanvas.redraw();
                }

                return true;
            }

            // Clicked anywhere else on header
            // Perhaps open Edit dialog here?
        }

        mManager.switchTo(this);
        return true;
    }

    /**
     * Paints the preview at the given x/y position
     *
     * @param gc the graphics context to paint it into
     * @param x the x coordinate to paint the preview at
     * @param y the y coordinate to paint the preview at
     */
    void paint(GC gc, int x, int y) {
        mTitleHeight = paintTitle(gc, x, y, true /*showFile*/);
        y += mTitleHeight;
        y += 2;

        int width = getWidth();
        int height = getHeight();
        if (mThumbnail != null && mError == null) {
            gc.drawImage(mThumbnail, x, y);

            if (mActive) {
                int oldWidth = gc.getLineWidth();
                gc.setLineWidth(3);
                gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_LIST_SELECTION));
                gc.drawRectangle(x - 1, y - 1, width + 2, height + 2);
                gc.setLineWidth(oldWidth);
            }
        } else if (mError != null) {
            if (mThumbnail != null) {
                gc.drawImage(mThumbnail, x, y);
            } else {
                gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_WIDGET_BORDER));
                gc.drawRectangle(x, y, width, height);
            }

            gc.setClipping(x, y, width, height);
            Image icon = IconFactory.getInstance().getIcon("renderError"); //$NON-NLS-1$
            ImageData data = icon.getImageData();
            int prevAlpha = gc.getAlpha();
            int alpha = 96;
            if (mThumbnail != null) {
                alpha -= 32;
            }
            gc.setAlpha(alpha);
            gc.drawImage(icon, x + (width - data.width) / 2, y + (height - data.height) / 2);

            String msg = mError;
            Density density = mConfiguration.getDensity();
            if (density == Density.TV || density == Density.LOW) {
                msg = "Broken rendering library; unsupported DPI. Try using the SDK manager " +
                        "to get updated layout libraries.";
            }
            int charWidth = gc.getFontMetrics().getAverageCharWidth();
            int charsPerLine = (width - 10) / charWidth;
            msg = SdkUtils.wrap(msg, charsPerLine, null);
            gc.setAlpha(255);
            gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_BLACK));
            gc.drawText(msg, x + 5, y + HEADER_HEIGHT, true);
            gc.setAlpha(prevAlpha);
            gc.setClipping((Region) null);
        } else {
            gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_WIDGET_BORDER));
            gc.drawRectangle(x, y, width, height);

            Image icon = IconFactory.getInstance().getIcon("refreshPreview"); //$NON-NLS-1$
            ImageData data = icon.getImageData();
            int prevAlpha = gc.getAlpha();
            gc.setAlpha(96);
            gc.drawImage(icon, x + (width - data.width) / 2,
                    y + (height - data.height) / 2);
            gc.setAlpha(prevAlpha);
        }

        if (mActive) {
            int left = x ;
            int prevAlpha = gc.getAlpha();
            gc.setAlpha(208);
            Color bg = mCanvas.getDisplay().getSystemColor(SWT.COLOR_WHITE);
            gc.setBackground(bg);
            gc.fillRectangle(left, y, x + width - left, HEADER_HEIGHT);
            gc.setAlpha(prevAlpha);

            y += 2;

            // Paint icons
            gc.drawImage(CLOSE_ICON, left, y);
            left += CLOSE_ICON_WIDTH;

            gc.drawImage(ZOOM_IN_ICON, left, y);
            left += ZOOM_IN_ICON_WIDTH;

            gc.drawImage(ZOOM_OUT_ICON, left, y);
            left += ZOOM_OUT_ICON_WIDTH;

            gc.drawImage(EDIT_ICON, left, y);
            left += EDIT_ICON_WIDTH;
        }
    }

    /**
     * Paints the preview title at the given position (and returns the required
     * height)
     *
     * @param gc the graphics context to paint into
     * @param x the left edge of the preview rectangle
     * @param y the top edge of the preview rectangle
     */
    private int paintTitle(GC gc, int x, int y, boolean showFile) {
        String displayName = getDisplayName();
        return paintTitle(gc, x, y, showFile, displayName);
    }

    /**
     * Paints the preview title at the given position (and returns the required
     * height)
     *
     * @param gc the graphics context to paint into
     * @param x the left edge of the preview rectangle
     * @param y the top edge of the preview rectangle
     * @param displayName the title string to be used
     */
    int paintTitle(GC gc, int x, int y, boolean showFile, String displayName) {
        int titleHeight = 0;

        if (showFile && mIncludedWithin != null) {
            if (mManager.getMode() != INCLUDES) {
                displayName = "<include>";
            } else {
                // Skip: just paint footer instead
                displayName = null;
            }
        }

        int width = getWidth();
        int labelTop = y + 1;
        gc.setClipping(x, labelTop, width, 100);

        // Use font height rather than extent height since we want two adjacent
        // previews (which may have different display names and therefore end
        // up with slightly different extent heights) to have identical title
        // heights such that they are aligned identically
        int fontHeight = gc.getFontMetrics().getHeight();

        if (displayName != null && displayName.length() > 0) {
            gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_WHITE));
            Point extent = gc.textExtent(displayName);
            int labelLeft = Math.max(x, x + (width - extent.x) / 2);
            Image icon = null;
            Locale locale = mConfiguration.getLocale();
            if (locale != null && (locale.hasLanguage() || locale.hasRegion())
                    && (!(mConfiguration instanceof NestedConfiguration)
                            || ((NestedConfiguration) mConfiguration).isOverridingLocale())) {
                icon = locale.getFlagImage();
            }

            if (icon != null) {
                int flagWidth = icon.getImageData().width;
                int flagHeight = icon.getImageData().height;
                labelLeft = Math.max(x + flagWidth / 2, labelLeft);
                gc.drawImage(icon, labelLeft - flagWidth / 2 - 1, labelTop);
                labelLeft += flagWidth / 2 + 1;
                gc.drawText(displayName, labelLeft,
                        labelTop - (extent.y - flagHeight) / 2, true);
            } else {
                gc.drawText(displayName, labelLeft, labelTop, true);
            }

            labelTop += extent.y;
            titleHeight += fontHeight;
        }

        if (showFile && (mAlternateInput != null || mIncludedWithin != null)) {
            // Draw file flag, and parent folder name
            IFile file = mAlternateInput != null
                    ? mAlternateInput : mIncludedWithin.getFile();
            String fileName = file.getParent().getName() + File.separator
                    + file.getName();
            Point extent = gc.textExtent(fileName);
            Image icon = IconFactory.getInstance().getIcon("android_file"); //$NON-NLS-1$
            int flagWidth = icon.getImageData().width;
            int flagHeight = icon.getImageData().height;

            int labelLeft = Math.max(x, x + (width - extent.x - flagWidth - 1) / 2);

            gc.drawImage(icon, labelLeft, labelTop);

            gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_GRAY));
            labelLeft += flagWidth + 1;
            labelTop -= (extent.y - flagHeight) / 2;
            gc.drawText(fileName, labelLeft, labelTop, true);

            titleHeight += Math.max(titleHeight, icon.getImageData().height);
        }

        gc.setClipping((Region) null);

        return titleHeight;
    }

    /**
     * Notifies that the preview's configuration has changed.
     *
     * @param flags the change flags, a bitmask corresponding to the
     *            {@code CHANGE_} constants in {@link ConfigurationClient}
     */
    public void configurationChanged(int flags) {
        if (!mVisible) {
            mDirty |= flags;
            return;
        }

        if ((flags & MASK_RENDERING) != 0) {
            mResourceResolver.clear();
            // Handle inheritance
            mConfiguration.syncFolderConfig();
            updateForkStatus();
            updateSize();
        }

        // Sanity check to make sure things are working correctly
        if (DEBUG) {
            RenderPreviewMode mode = mManager.getMode();
            if (mode == DEFAULT) {
                assert mConfiguration instanceof VaryingConfiguration;
                VaryingConfiguration config = (VaryingConfiguration) mConfiguration;
                int alternateFlags = config.getAlternateFlags();
                switch (alternateFlags) {
                    case Configuration.CFG_DEVICE_STATE: {
                        State configState = config.getDeviceState();
                        State chooserState = mManager.getChooser().getConfiguration()
                                .getDeviceState();
                        assert configState != null && chooserState != null;
                        assert !configState.getName().equals(chooserState.getName())
                                : configState.toString() + ':' + chooserState;

                        Device configDevice = config.getDevice();
                        Device chooserDevice = mManager.getChooser().getConfiguration()
                                .getDevice();
                        assert configDevice != null && chooserDevice != null;
                        assert configDevice == chooserDevice
                                : configDevice.toString() + ':' + chooserDevice;

                        break;
                    }
                    case Configuration.CFG_DEVICE: {
                        Device configDevice = config.getDevice();
                        Device chooserDevice = mManager.getChooser().getConfiguration()
                                .getDevice();
                        assert configDevice != null && chooserDevice != null;
                        assert configDevice != chooserDevice
                                : configDevice.toString() + ':' + chooserDevice;

                        State configState = config.getDeviceState();
                        State chooserState = mManager.getChooser().getConfiguration()
                                .getDeviceState();
                        assert configState != null && chooserState != null;
                        assert configState.getName().equals(chooserState.getName())
                                : configState.toString() + ':' + chooserState;

                        break;
                    }
                    case Configuration.CFG_LOCALE: {
                        Locale configLocale = config.getLocale();
                        Locale chooserLocale = mManager.getChooser().getConfiguration()
                                .getLocale();
                        assert configLocale != null && chooserLocale != null;
                        assert configLocale != chooserLocale
                                : configLocale.toString() + ':' + chooserLocale;
                        break;
                    }
                    default: {
                        // Some other type of override I didn't anticipate
                        assert false : alternateFlags;
                    }
                }
            }
        }

        mDirty = 0;
        mManager.scheduleRender(this);
    }

    private void updateSize() {
        Device device = mConfiguration.getDevice();
        if (device == null) {
            return;
        }
        Screen screen = device.getDefaultHardware().getScreen();
        if (screen == null) {
            return;
        }

        FolderConfiguration folderConfig = mConfiguration.getFullConfig();
        ScreenOrientationQualifier qualifier = folderConfig.getScreenOrientationQualifier();
        ScreenOrientation orientation = qualifier == null
                ? ScreenOrientation.PORTRAIT : qualifier.getValue();

        // compute width and height to take orientation into account.
        int x = screen.getXDimension();
        int y = screen.getYDimension();
        int screenWidth, screenHeight;

        if (x > y) {
            if (orientation == ScreenOrientation.LANDSCAPE) {
                screenWidth = x;
                screenHeight = y;
            } else {
                screenWidth = y;
                screenHeight = x;
            }
        } else {
            if (orientation == ScreenOrientation.LANDSCAPE) {
                screenWidth = y;
                screenHeight = x;
            } else {
                screenWidth = x;
                screenHeight = y;
            }
        }

        int width = RenderPreviewManager.getMaxWidth();
        int height = RenderPreviewManager.getMaxHeight();
        if (screenWidth > 0) {
            double scale = getScale(screenWidth, screenHeight);
            width = (int) (screenWidth * scale);
            height = (int) (screenHeight * scale);
        }

        if (width != mWidth || height != mHeight) {
            mWidth = width;
            mHeight = height;

            Image thumbnail = mThumbnail;
            mThumbnail = null;
            if (thumbnail != null) {
                thumbnail.dispose();
            }
            if (mHeight != 0) {
                mAspectRatio = mWidth / (double) mHeight;
            }
        }
    }

    /**
     * Returns the configuration associated with this preview
     *
     * @return the configuration
     */
    @NonNull
    public Configuration getConfiguration() {
        return mConfiguration;
    }

    // ---- Implements IJobChangeListener ----

    @Override
    public void aboutToRun(IJobChangeEvent event) {
    }

    @Override
    public void awake(IJobChangeEvent event) {
    }

    @Override
    public void done(IJobChangeEvent event) {
        mJob = null;
    }

    @Override
    public void running(IJobChangeEvent event) {
    }

    @Override
    public void scheduled(IJobChangeEvent event) {
    }

    @Override
    public void sleeping(IJobChangeEvent event) {
    }

    // ---- Delayed Rendering ----

    private final class RenderJob extends UIJob {
        public RenderJob() {
            super("RenderPreview");
            setSystem(true);
            setUser(false);
        }

        @Override
        public IStatus runInUIThread(IProgressMonitor monitor) {
            mJob = null;
            if (!mCanvas.isDisposed()) {
                renderSync();
                mCanvas.redraw();
                return org.eclipse.core.runtime.Status.OK_STATUS;
            }

            return org.eclipse.core.runtime.Status.CANCEL_STATUS;
        }

        @Override
        public Display getDisplay() {
            if (mCanvas.isDisposed()) {
                return null;
            }
            return mCanvas.getDisplay();
        }
    }

    private final class AsyncRenderJob extends Job {
        public AsyncRenderJob() {
            super("RenderPreview");
            setSystem(true);
            setUser(false);
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            mJob = null;

            if (mCanvas.isDisposed()) {
                return org.eclipse.core.runtime.Status.CANCEL_STATUS;
            }

            renderSync();

            // Update display
            mCanvas.getDisplay().asyncExec(new Runnable() {
                @Override
                public void run() {
                    mCanvas.redraw();
                }
            });

            return org.eclipse.core.runtime.Status.OK_STATUS;
        }
    }

    /**
     * Sets the input file to use for rendering. If not set, this will just be
     * the same file as the configuration chooser. This is used to render other
     * layouts, such as variations of the currently edited layout, which are
     * not kept in sync with the main layout.
     *
     * @param file the file to set as input
     */
    public void setAlternateInput(@Nullable IFile file) {
        mAlternateInput = file;
    }

    /** Corresponding description for this preview if it is a manually added preview */
    private @Nullable ConfigurationDescription mDescription;

    /**
     * Sets the description of this preview, if this preview is a manually added preview
     *
     * @param description the description of this preview
     */
    public void setDescription(@Nullable ConfigurationDescription description) {
        mDescription = description;
    }

    /**
     * Returns the description of this preview, if this preview is a manually added preview
     *
     * @return the description
     */
    @Nullable
    public ConfigurationDescription getDescription() {
        return mDescription;
    }

    @Override
    public String toString() {
        return getDisplayName() + ':' + mConfiguration;
    }

    /** Sorts render previews into increasing aspect ratio order */
    static Comparator<RenderPreview> INCREASING_ASPECT_RATIO = new Comparator<RenderPreview>() {
        @Override
        public int compare(RenderPreview preview1, RenderPreview preview2) {
            return (int) Math.signum(preview1.mAspectRatio - preview2.mAspectRatio);
        }
    };
    /** Sorts render previews into visual order: row by row, column by column */
    static Comparator<RenderPreview> VISUAL_ORDER = new Comparator<RenderPreview>() {
        @Override
        public int compare(RenderPreview preview1, RenderPreview preview2) {
            int delta = preview1.mY - preview2.mY;
            if (delta == 0) {
                delta = preview1.mX - preview2.mX;
            }
            return delta;
        }
    };
}

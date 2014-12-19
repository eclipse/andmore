/*
 * Copyright (C) 2009 The Android Open Source Project
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

import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_LAYOUT_HEIGHT;
import static com.android.SdkConstants.ATTR_LAYOUT_WIDTH;
import static com.android.SdkConstants.ATTR_TEXT;
import static com.android.SdkConstants.VALUE_WRAP_CONTENT;
import static com.android.SdkConstants.XMLNS_ANDROID;
import static com.android.SdkConstants.XMLNS_URI;

import com.android.ide.common.api.InsertType;
import com.android.ide.common.api.Rect;
import com.android.ide.common.api.RuleAction.Toggle;
import com.android.ide.common.rendering.LayoutLibrary;
import com.android.ide.common.rendering.api.Capability;
import com.android.ide.common.rendering.api.LayoutLog;
import com.android.ide.common.rendering.api.RenderSession;
import com.android.ide.common.rendering.api.ViewInfo;
import com.android.sdklib.IAndroidTarget;
import com.android.utils.Pair;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.editors.IconFactory;
import org.eclipse.andmore.internal.editors.descriptors.DescriptorsUtils;
import org.eclipse.andmore.internal.editors.descriptors.DocumentDescriptor;
import org.eclipse.andmore.internal.editors.descriptors.ElementDescriptor;
import org.eclipse.andmore.internal.editors.layout.LayoutEditorDelegate;
import org.eclipse.andmore.internal.editors.layout.configuration.ConfigurationChooser;
import org.eclipse.andmore.internal.editors.layout.descriptors.CustomViewDescriptorService;
import org.eclipse.andmore.internal.editors.layout.descriptors.ViewElementDescriptor;
import org.eclipse.andmore.internal.editors.layout.gre.NodeFactory;
import org.eclipse.andmore.internal.editors.layout.gre.NodeProxy;
import org.eclipse.andmore.internal.editors.layout.gre.PaletteMetadataDescriptor;
import org.eclipse.andmore.internal.editors.layout.gre.ViewMetadataRepository;
import org.eclipse.andmore.internal.editors.layout.gre.ViewMetadataRepository.RenderMode;
import org.eclipse.andmore.internal.editors.layout.uimodel.UiViewElementNode;
import org.eclipse.andmore.internal.editors.uimodel.UiDocumentNode;
import org.eclipse.andmore.internal.editors.uimodel.UiElementNode;
import org.eclipse.andmore.internal.preferences.AdtPrefs;
import org.eclipse.andmore.internal.sdk.AndroidTargetData;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.wb.internal.core.editor.structure.IPage;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A palette control for the {@link GraphicalEditorPart}.
 * <p/>
 * The palette contains several groups, each with a UI name (e.g. layouts and views) and each
 * with a list of element descriptors.
 * <p/>
 *
 * TODO list:
 *   - The available items should depend on the actual GLE2 Canvas selection. Selected android
 *     views should force filtering on what they accept can be dropped on them (e.g. TabHost,
 *     TableLayout). Should enable/disable them, not hide them, to avoid shuffling around.
 *   - Optional: a text filter
 *   - Optional: have context-sensitive tools items, e.g. selection arrow tool,
 *     group selection tool, alignment, etc.
 */
public class PaletteControl extends Composite {

    /**
     * Wrapper to create a {@link PaletteControl}
     */
    static class PalettePage implements IPage {
        private final GraphicalEditorPart mEditorPart;
        private PaletteControl mControl;

        PalettePage(GraphicalEditorPart editor) {
            mEditorPart = editor;
        }

        @Override
        public void createControl(Composite parent) {
            mControl = new PaletteControl(parent, mEditorPart);
        }

        @Override
        public Control getControl() {
            return mControl;
        }

        @Override
        public void dispose() {
            mControl.dispose();
        }

        @Override
        public void setToolBar(IToolBarManager toolBarManager) {
        }

        /**
         * Add tool bar items to the given toolbar
         *
         * @param toolbar the toolbar to add items into
         */
        void createToolbarItems(final ToolBar toolbar) {
            final ToolItem popupMenuItem = new ToolItem(toolbar, SWT.PUSH);
            popupMenuItem.setToolTipText("View Menu");
            popupMenuItem.setImage(IconFactory.getInstance().getIcon("view_menu"));
            popupMenuItem.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    Rectangle bounds = popupMenuItem.getBounds();
                    // Align menu horizontally with the toolbar button and
                    // vertically with the bottom of the toolbar
                    Point point = toolbar.toDisplay(bounds.x, bounds.y + bounds.height);
                    mControl.showMenu(point.x, point.y);
                }
            });
        }

        @Override
        public void setFocus() {
            mControl.setFocus();
        }
    }

    /**
     * The parent grid layout that contains all the {@link Toggle} and
     * {@link IconTextItem} widgets.
     */
    private GraphicalEditorPart mEditor;
    private Color mBackground;
    private Color mForeground;

    /** The palette modes control various ways to visualize and lay out the views */
    private static enum PaletteMode {
        /** Show rendered previews of the views */
        PREVIEW("Show Previews", true),
        /** Show rendered previews of the views, scaled down to 75% */
        SMALL_PREVIEW("Show Small Previews", true),
        /** Show rendered previews of the views, scaled down to 50% */
        TINY_PREVIEW("Show Tiny Previews", true),
        /** Show an icon + text label */
        ICON_TEXT("Show Icon and Text", false),
        /** Show only icons, packed multiple per row */
        ICON_ONLY("Show Only Icons", true);

        PaletteMode(String actionLabel, boolean wrap) {
            mActionLabel = actionLabel;
            mWrap = wrap;
        }

        public String getActionLabel() {
            return mActionLabel;
        }

        public boolean getWrap() {
            return mWrap;
        }

        public boolean isPreview() {
            return this == PREVIEW || this == SMALL_PREVIEW || this == TINY_PREVIEW;
        }

        public boolean isScaledPreview() {
            return this == SMALL_PREVIEW || this == TINY_PREVIEW;
        }

        private final String mActionLabel;
        private final boolean mWrap;
    };

    /** Token used in preference string to record alphabetical sorting */
    private static final String VALUE_ALPHABETICAL = "alpha";   //$NON-NLS-1$
    /** Token used in preference string to record categories being turned off */
    private static final String VALUE_NO_CATEGORIES = "nocat"; //$NON-NLS-1$
    /** Token used in preference string to record auto close being turned off */
    private static final String VALUE_NO_AUTOCLOSE = "noauto";      //$NON-NLS-1$

    private final PreviewIconFactory mPreviewIconFactory = new PreviewIconFactory(this);
    private PaletteMode mPaletteMode = null;
    /** Use alphabetical sorting instead of natural order? */
    private boolean mAlphabetical;
    /** Use categories instead of a single large list of views? */
    private boolean mCategories = true;
    /** Auto-close the previous category when new categories are opened */
    private boolean mAutoClose = true;
    private AccordionControl mAccordion;
    private String mCurrentTheme;
    private String mCurrentDevice;
    private IAndroidTarget mCurrentTarget;
    private AndroidTargetData mCurrentTargetData;

    /**
     * Create the composite.
     * @param parent The parent composite.
     * @param editor An editor associated with this palette.
     */
    public PaletteControl(Composite parent, GraphicalEditorPart editor) {
        super(parent, SWT.NONE);

        mEditor = editor;
    }

    /** Reads UI mode from persistent store to preserve palette mode across IDE sessions */
    private void loadPaletteMode() {
        String paletteModes = AdtPrefs.getPrefs().getPaletteModes();
        if (paletteModes.length() > 0) {
            String[] tokens = paletteModes.split(","); //$NON-NLS-1$
            try {
                mPaletteMode = PaletteMode.valueOf(tokens[0]);
            } catch (Throwable t) {
                mPaletteMode = PaletteMode.values()[0];
            }
            mAlphabetical = paletteModes.contains(VALUE_ALPHABETICAL);
            mCategories = !paletteModes.contains(VALUE_NO_CATEGORIES);
            mAutoClose = !paletteModes.contains(VALUE_NO_AUTOCLOSE);
        } else {
            mPaletteMode = PaletteMode.SMALL_PREVIEW;
        }
    }

    /**
     * Returns the most recently stored version of auto-close-mode; this is the last
     * user-initiated setting of the auto-close mode (we programmatically switch modes when
     * you enter icons-only mode, and set it back to this when going to any other mode)
     */
    private boolean getSavedAutoCloseMode() {
        return !AdtPrefs.getPrefs().getPaletteModes().contains(VALUE_NO_AUTOCLOSE);
    }

    /** Saves UI mode to persistent store to preserve palette mode across IDE sessions */
    private void savePaletteMode() {
        StringBuilder sb = new StringBuilder();
        sb.append(mPaletteMode);
        if (mAlphabetical) {
            sb.append(',').append(VALUE_ALPHABETICAL);
        }
        if (!mCategories) {
            sb.append(',').append(VALUE_NO_CATEGORIES);
        }
        if (!mAutoClose) {
            sb.append(',').append(VALUE_NO_AUTOCLOSE);
        }
        AdtPrefs.getPrefs().setPaletteModes(sb.toString());
    }

    private void refreshPalette() {
        IAndroidTarget oldTarget = mCurrentTarget;
        mCurrentTarget = null;
        mCurrentTargetData = null;
        mCurrentTheme = null;
        mCurrentDevice = null;
        reloadPalette(oldTarget);
    }

    @Override
    protected void checkSubclass() {
        // Disable the check that prevents subclassing of SWT components
    }

    @Override
    public void dispose() {
        if (mBackground != null) {
            mBackground.dispose();
            mBackground = null;
        }
        if (mForeground != null) {
            mForeground.dispose();
            mForeground = null;
        }

        super.dispose();
    }

    /**
     * Returns the currently displayed target
     *
     * @return the current target, or null
     */
    public IAndroidTarget getCurrentTarget() {
        return mCurrentTarget;
    }

    /**
     * Returns the currently displayed theme (in palette modes that support previewing)
     *
     * @return the current theme, or null
     */
    public String getCurrentTheme() {
        return mCurrentTheme;
    }

    /**
     * Returns the currently displayed device (in palette modes that support previewing)
     *
     * @return the current device, or null
     */
    public String getCurrentDevice() {
        return mCurrentDevice;
    }

    /** Returns true if previews in the palette should be made available */
    private boolean previewsAvailable() {
        // Not layoutlib 5 -- we require custom background support to do
        // a decent job with previews
        LayoutLibrary layoutLibrary = mEditor.getLayoutLibrary();
        return layoutLibrary != null && layoutLibrary.supports(Capability.CUSTOM_BACKGROUND_COLOR);
    }

    /**
     * Loads or reloads the palette elements by using the layout and view descriptors from the
     * given target data.
     *
     * @param target The target that has just been loaded
     */
    public void reloadPalette(IAndroidTarget target) {
        ConfigurationChooser configChooser = mEditor.getConfigurationChooser();
        String theme = configChooser.getThemeName();
        String device = configChooser.getDeviceName();
        if (device == null) {
            return;
        }
        AndroidTargetData targetData =
            target != null ? Sdk.getCurrent().getTargetData(target) : null;
        if (target == mCurrentTarget && targetData == mCurrentTargetData
                && mCurrentTheme != null && mCurrentTheme.equals(theme)
                && mCurrentDevice != null && mCurrentDevice.equals(device)) {
            return;
        }
        mCurrentTheme = theme;
        mCurrentTarget = target;
        mCurrentTargetData = targetData;
        mCurrentDevice = device;
        mPreviewIconFactory.reset();

        if (targetData == null) {
            return;
        }

        Set<String> expandedCategories = null;
        if (mAccordion != null) {
            expandedCategories = mAccordion.getExpandedCategories();
            // We auto-expand all categories when showing icons-only. When returning to some
            // other mode we don't want to retain all categories open.
            if (expandedCategories.size() > 3) {
                expandedCategories = null;
            }
        }

        // Erase old content and recreate new
        for (Control c : getChildren()) {
            c.dispose();
        }

        if (mPaletteMode == null) {
            loadPaletteMode();
            assert mPaletteMode != null;
        }

        // Ensure that the palette mode is supported on this version of the layout library
        if (!previewsAvailable()) {
            if (mPaletteMode.isPreview()) {
                mPaletteMode = PaletteMode.ICON_TEXT;
            }
        }

        if (mPaletteMode.isPreview()) {
            if (mForeground != null) {
                mForeground.dispose();
                mForeground = null;
            }
            if (mBackground != null) {
                mBackground.dispose();
                mBackground = null;
            }
            RGB background = mPreviewIconFactory.getBackgroundColor();
            if (background != null) {
                mBackground = new Color(getDisplay(), background);
            }
            RGB foreground = mPreviewIconFactory.getForegroundColor();
            if (foreground != null) {
                mForeground = new Color(getDisplay(), foreground);
            }
        }

        List<String> headers = Collections.emptyList();
        final Map<String, List<ViewElementDescriptor>> categoryToItems;
        categoryToItems = new HashMap<String, List<ViewElementDescriptor>>();
        headers = new ArrayList<String>();
        List<Pair<String,List<ViewElementDescriptor>>> paletteEntries =
            ViewMetadataRepository.get().getPaletteEntries(targetData,
                    mAlphabetical, mCategories);
        for (Pair<String,List<ViewElementDescriptor>> pair : paletteEntries) {
            String category = pair.getFirst();
            List<ViewElementDescriptor> categoryItems = pair.getSecond();
            headers.add(category);
            categoryToItems.put(category, categoryItems);
        }

        headers.add("Custom & Library Views");

        // Set the categories to expand the first item if
        //   (1) we don't have a previously selected category, or
        //   (2) there's just one category anyway, or
        //   (3) the set of categories have changed so our previously selected category
        //       doesn't exist anymore (can happen when you toggle "Show Categories")
        if ((expandedCategories == null && headers.size() > 0) || headers.size() == 1 ||
                (expandedCategories != null && expandedCategories.size() >= 1
                        && !headers.contains(
                                expandedCategories.iterator().next().replace("&&", "&")))) { //$NON-NLS-1$ //$NON-NLS-2$
            // Expand the first category if we don't have a previous selection (e.g. refresh)
            expandedCategories = Collections.singleton(headers.get(0));
        }

        boolean wrap = mPaletteMode.getWrap();

        // Pack icon-only view vertically; others stretch to fill palette region
        boolean fillVertical = mPaletteMode != PaletteMode.ICON_ONLY;

        mAccordion = new AccordionControl(this, SWT.NONE, headers, fillVertical, wrap,
                expandedCategories) {
            @Override
            protected Composite createChildContainer(Composite parent, Object header, int style) {
                assert categoryToItems != null;
                List<ViewElementDescriptor> list = categoryToItems.get(header);
                final Composite composite;
                if (list == null) {
                    assert header.equals("Custom & Library Views");

                    Composite wrapper = new Composite(parent, SWT.NONE);
                    GridLayout gridLayout = new GridLayout(1, false);
                    gridLayout.marginWidth = gridLayout.marginHeight = 0;
                    gridLayout.horizontalSpacing = gridLayout.verticalSpacing = 0;
                    gridLayout.marginBottom = 3;
                    wrapper.setLayout(gridLayout);
                    if (mPaletteMode.isPreview() && mBackground != null) {
                        wrapper.setBackground(mBackground);
                    }
                    composite = super.createChildContainer(wrapper, header, style);
                    if (mPaletteMode.isPreview() && mBackground != null) {
                        composite.setBackground(mBackground);
                    }
                    composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

                    Button refreshButton = new Button(wrapper, SWT.PUSH | SWT.FLAT);
                    refreshButton.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER,
                            false, false, 1, 1));
                    refreshButton.setText("Refresh");
                    refreshButton.setImage(IconFactory.getInstance().getIcon("refresh")); //$NON-NLS-1$
                    refreshButton.addSelectionListener(new SelectionAdapter() {
                        @Override
                        public void widgetSelected(SelectionEvent e) {
                            CustomViewFinder finder = CustomViewFinder.get(mEditor.getProject());
                            finder.refresh(new ViewFinderListener(composite));
                        }
                    });

                    wrapper.layout(true);
                } else {
                    composite = super.createChildContainer(parent, header, style);
                    if (mPaletteMode.isPreview() && mBackground != null) {
                        composite.setBackground(mBackground);
                    }
                }
                addMenu(composite);
                return composite;
            }
            @Override
            protected void createChildren(Composite parent, Object header) {
                assert categoryToItems != null;
                List<ViewElementDescriptor> list = categoryToItems.get(header);
                if (list == null) {
                    assert header.equals("Custom & Library Views");
                    addCustomItems(parent);
                    return;
                } else {
                    for (ViewElementDescriptor desc : list) {
                        createItem(parent, desc);
                    }
                }
            }
        };
        addMenu(mAccordion);
        for (CLabel headerLabel : mAccordion.getHeaderLabels()) {
            addMenu(headerLabel);
        }
        setLayout(new FillLayout());

        // Expand All for icon-only mode, but don't store it as the persistent auto-close mode;
        // when we enter other modes it will read back whatever persistent mode.
        if (mPaletteMode == PaletteMode.ICON_ONLY) {
            mAccordion.expandAll(true);
            mAccordion.setAutoClose(false);
        } else {
            mAccordion.setAutoClose(getSavedAutoCloseMode());
        }

        layout(true);
    }

    protected void addCustomItems(final Composite parent) {
        final CustomViewFinder finder = CustomViewFinder.get(mEditor.getProject());
        Collection<String> allViews = finder.getAllViews();
        if (allViews == null) { // Not yet initialized: trigger an async refresh
            finder.refresh(new ViewFinderListener(parent));
            return;
        }

        // Remove previous content
        for (Control c : parent.getChildren()) {
            c.dispose();
        }

        // Add new views
        for (final String fqcn : allViews) {
            CustomViewDescriptorService service = CustomViewDescriptorService.getInstance();
            ViewElementDescriptor desc = service.getDescriptor(mEditor.getProject(), fqcn);
            if (desc == null) {
                // The descriptor lookup performs validation steps of the class, and may
                // in some cases determine that this is not a view and will return null;
                // guard against that.
                continue;
            }

            Control item = createItem(parent, desc);

            // Add control-click listener on custom view items to you can warp to
            // (and double click listener too -- the more discoverable, the better.)
            if (item instanceof IconTextItem) {
                IconTextItem it = (IconTextItem) item;
                it.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseDoubleClick(MouseEvent e) {
                        AdtPlugin.openJavaClass(mEditor.getProject(), fqcn);
                    }

                    @Override
                    public void mouseDown(MouseEvent e) {
                        if ((e.stateMask & SWT.MOD1) != 0) {
                            AdtPlugin.openJavaClass(mEditor.getProject(), fqcn);
                        }
                    }
                });
            }
        }
    }

    /* package */ GraphicalEditorPart getEditor() {
        return mEditor;
    }

    private Control createItem(Composite parent, ViewElementDescriptor desc) {
        Control item = null;
        switch (mPaletteMode) {
            case SMALL_PREVIEW:
            case TINY_PREVIEW:
            case PREVIEW: {
                ImageDescriptor descriptor = mPreviewIconFactory.getImageDescriptor(desc);
                if (descriptor != null) {
                    Image image = descriptor.createImage();
                    ImageControl imageControl = new ImageControl(parent, SWT.None, image);
                    if (mPaletteMode.isScaledPreview()) {
                        // Try to preserve the overall size since rendering sizes typically
                        // vary with the dpi - so while the scaling factor for a 160 dpi
                        // rendering the scaling factor should be 0.5, for a 320 dpi one the
                        // scaling factor should be half that, 0.25.
                        float scale = 1.0f;
                        if (mPaletteMode == PaletteMode.SMALL_PREVIEW) {
                            scale = 0.75f;
                        } else if (mPaletteMode == PaletteMode.TINY_PREVIEW) {
                            scale = 0.5f;
                        }
                        ConfigurationChooser chooser = mEditor.getConfigurationChooser();
                        int dpi = chooser.getConfiguration().getDensity().getDpiValue();
                        while (dpi > 160) {
                            scale = scale / 2;
                            dpi = dpi / 2;
                        }
                        imageControl.setScale(scale);
                    }
                    imageControl.setHoverColor(getDisplay().getSystemColor(SWT.COLOR_WHITE));
                    if (mBackground != null) {
                        imageControl.setBackground(mBackground);
                    }
                    String toolTip = desc.getUiName();
                    // It appears pretty much none of the descriptors have tooltips
                    //String descToolTip = desc.getTooltip();
                    //if (descToolTip != null && descToolTip.length() > 0) {
                    //    toolTip = toolTip + "\n" + descToolTip;
                    //}
                    imageControl.setToolTipText(toolTip);

                    item = imageControl;
                } else {
                    // Just use an Icon+Text item for these for now
                    item = new IconTextItem(parent, desc);
                    if (mForeground != null) {
                        item.setForeground(mForeground);
                        item.setBackground(mBackground);
                    }
                }
                break;
            }
            case ICON_TEXT: {
                item = new IconTextItem(parent, desc);
                break;
            }
            case ICON_ONLY: {
                item = new ImageControl(parent, SWT.None, desc.getGenericIcon());
                item.setToolTipText(desc.getUiName());
                break;
            }
            default:
                throw new IllegalArgumentException("Not yet implemented");
        }

        final DragSource source = new DragSource(item, DND.DROP_COPY);
        source.setTransfer(new Transfer[] { SimpleXmlTransfer.getInstance() });
        source.addDragListener(new DescDragSourceListener(desc));
        item.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                source.dispose();
            }
        });
        addMenu(item);

        return item;
    }

    /**
     * An Item widget represents one {@link ElementDescriptor} that can be dropped on the
     * GLE2 canvas using drag'n'drop.
     */
    private static class IconTextItem extends CLabel implements MouseTrackListener {

        private boolean mMouseIn;

        public IconTextItem(Composite parent, ViewElementDescriptor desc) {
            super(parent, SWT.NONE);
            mMouseIn = false;

            setText(desc.getUiName());
            setImage(desc.getGenericIcon());
            setToolTipText(desc.getTooltip());
            addMouseTrackListener(this);
        }

        @Override
        public int getStyle() {
            int style = super.getStyle();
            if (mMouseIn) {
                style |= SWT.SHADOW_IN;
            }
            return style;
        }

        @Override
        public void mouseEnter(MouseEvent e) {
            if (!mMouseIn) {
                mMouseIn = true;
                redraw();
            }
        }

        @Override
        public void mouseExit(MouseEvent e) {
            if (mMouseIn) {
                mMouseIn = false;
                redraw();
            }
        }

        @Override
        public void mouseHover(MouseEvent e) {
            // pass
        }
    }

    /**
     * A {@link DragSourceListener} that deals with drag'n'drop of
     * {@link ElementDescriptor}s.
     */
    private class DescDragSourceListener implements DragSourceListener {
        private final ViewElementDescriptor mDesc;
        private SimpleElement[] mElements;

        public DescDragSourceListener(ViewElementDescriptor desc) {
            mDesc = desc;
        }

        @Override
        public void dragStart(DragSourceEvent e) {
            // See if we can find out the bounds of this element from a preview image.
            // Preview images are created before the drag source listener is notified
            // of the started drag.
            Rect bounds = null;
            Rect dragBounds = null;

            createDragImage(e);
            if (mImage != null && !mIsPlaceholder) {
                int width = mImageLayoutBounds.width;
                int height = mImageLayoutBounds.height;
                assert mImageLayoutBounds.x == 0;
                assert mImageLayoutBounds.y == 0;
                bounds = new Rect(0, 0, width, height);
                double scale = mEditor.getCanvasControl().getScale();
                int scaledWidth = (int) (scale * width);
                int scaledHeight = (int) (scale * height);
                int x = -scaledWidth / 2;
                int y = -scaledHeight / 2;
                dragBounds = new Rect(x, y, scaledWidth, scaledHeight);
            }

            SimpleElement se = new SimpleElement(
                    SimpleXmlTransfer.getFqcn(mDesc),
                    null   /* parentFqcn */,
                    bounds /* bounds */,
                    null   /* parentBounds */);
            if (mDesc instanceof PaletteMetadataDescriptor) {
                PaletteMetadataDescriptor pm = (PaletteMetadataDescriptor) mDesc;
                pm.initializeNew(se);
            }
            mElements = new SimpleElement[] { se };

            // Register this as the current dragged data
            GlobalCanvasDragInfo dragInfo = GlobalCanvasDragInfo.getInstance();
            dragInfo.startDrag(
                    mElements,
                    null /* selection */,
                    null /* canvas */,
                    null /* removeSource */);
            dragInfo.setDragBounds(dragBounds);
            dragInfo.setDragBaseline(mBaseline);


            e.doit = true;
        }

        @Override
        public void dragSetData(DragSourceEvent e) {
            // Provide the data for the drop when requested by the other side.
            if (SimpleXmlTransfer.getInstance().isSupportedType(e.dataType)) {
                e.data = mElements;
            }
        }

        @Override
        public void dragFinished(DragSourceEvent e) {
            // Unregister the dragged data.
            GlobalCanvasDragInfo.getInstance().stopDrag();
            mElements = null;
            if (mImage != null) {
                mImage.dispose();
                mImage = null;
            }
        }

        // TODO: Figure out the right dimensions to use for rendering.
        // We WILL crop this after rendering, but for performance reasons it would be good
        // not to make it much larger than necessary since to crop this we rely on
        // actually scanning pixels.

        /**
         * Width of the rendered preview image (before it is cropped), although the actual
         * width may be smaller (since we also take the device screen's size into account)
         */
        private static final int MAX_RENDER_HEIGHT = 400;

        /**
         * Height of the rendered preview image (before it is cropped), although the
         * actual width may be smaller (since we also take the device screen's size into
         * account)
         */
        private static final int MAX_RENDER_WIDTH = 500;

        /** Amount of alpha to multiply into the image (divided by 256) */
        private static final int IMG_ALPHA = 128;

        /** The image shown during the drag */
        private Image mImage;
        /** The non-effect bounds of the drag image */
        private Rectangle mImageLayoutBounds;
        private int mBaseline = -1;

        /**
         * If true, the image is a preview of the view, and if not it is a "fallback"
         * image of some sort, such as a rendering of the palette item itself
         */
        private boolean mIsPlaceholder;

        private void createDragImage(DragSourceEvent event) {
            mBaseline = -1;
            Pair<Image, Rectangle> preview = renderPreview();
            if (preview != null) {
                mImage = preview.getFirst();
                mImageLayoutBounds = preview.getSecond();
            } else {
                mImage = null;
                mImageLayoutBounds = null;
            }

            mIsPlaceholder = mImage == null;
            if (mIsPlaceholder) {
                // Couldn't render preview (or the preview is a blank image, such as for
                // example the preview of an empty layout), so instead create a placeholder
                // image
                // Render the palette item itself as an image
                Control control = ((DragSource) event.widget).getControl();
                GC gc = new GC(control);
                Point size = control.getSize();
                Display display = getDisplay();
                final Image image = new Image(display, size.x, size.y);
                gc.copyArea(image, 0, 0);
                gc.dispose();

                BufferedImage awtImage = SwtUtils.convertToAwt(image);
                if (awtImage != null) {
                    awtImage = ImageUtils.createDropShadow(awtImage, 3 /* shadowSize */,
                            0.7f /* shadowAlpha */, 0x000000 /* shadowRgb */);
                    mImage = SwtUtils.convertToSwt(display, awtImage, true, IMG_ALPHA);
                } else {
                    ImageData data = image.getImageData();
                    data.alpha = IMG_ALPHA;

                    // Changing the ImageData -after- constructing an image on it
                    // has no effect, so we have to construct a new image. Luckily these
                    // are tiny images.
                    mImage = new Image(display, data);
                }
                image.dispose();
            }

            event.image = mImage;

            if (!mIsPlaceholder) {
                // Shift the drag feedback image up such that it's centered under the
                // mouse pointer
                double scale = mEditor.getCanvasControl().getScale();
                event.offsetX = (int) (scale * mImageLayoutBounds.width / 2);
                event.offsetY = (int) (scale * mImageLayoutBounds.height / 2);
            }
        }

        /**
         * Performs the actual rendering of the descriptor into an image and returns the
         * image as well as the layout bounds of the image (not including drop shadow etc)
         */
        private Pair<Image, Rectangle> renderPreview() {
            ViewMetadataRepository repository = ViewMetadataRepository.get();
            RenderMode renderMode = repository.getRenderMode(mDesc.getFullClassName());
            if (renderMode == RenderMode.SKIP) {
                return null;
            }

            // Create blank XML document
            Document document = DomUtilities.createEmptyDocument();

            // Insert our target view's XML into it as a node
            GraphicalEditorPart editor = getEditor();
            LayoutEditorDelegate layoutEditorDelegate = editor.getEditorDelegate();

            String viewName = mDesc.getXmlLocalName();
            Element element = document.createElement(viewName);

            // Set up a proper name space
            Attr attr = document.createAttributeNS(XMLNS_URI, XMLNS_ANDROID);
            attr.setValue(ANDROID_URI);
            element.getAttributes().setNamedItemNS(attr);

            element.setAttributeNS(ANDROID_URI, ATTR_LAYOUT_WIDTH, VALUE_WRAP_CONTENT);
            element.setAttributeNS(ANDROID_URI, ATTR_LAYOUT_HEIGHT, VALUE_WRAP_CONTENT);

            // This doesn't apply to all, but doesn't seem to cause harm and makes for a
            // better experience with text-oriented views like buttons and texts
            element.setAttributeNS(ANDROID_URI, ATTR_TEXT,
                    DescriptorsUtils.getBasename(mDesc.getUiName()));

            // Is this a palette variation?
            if (mDesc instanceof PaletteMetadataDescriptor) {
                PaletteMetadataDescriptor pm = (PaletteMetadataDescriptor) mDesc;
                pm.initializeNew(element);
            }

            document.appendChild(element);

            // Construct UI model from XML
            AndroidTargetData data = layoutEditorDelegate.getEditor().getTargetData();
            DocumentDescriptor documentDescriptor;
            if (data == null) {
                documentDescriptor = new DocumentDescriptor("temp", null/*children*/);//$NON-NLS-1$
            } else {
                documentDescriptor = data.getLayoutDescriptors().getDescriptor();
            }
            UiDocumentNode model = (UiDocumentNode) documentDescriptor.createUiNode();
            model.setEditor(layoutEditorDelegate.getEditor());
            model.setUnknownDescriptorProvider(editor.getModel().getUnknownDescriptorProvider());
            model.loadFromXmlNode(document);

            // Call the create-hooks such that we for example insert mandatory
            // children into views like the DialerFilter, apply image source attributes
            // to ImageButtons, etc.
            LayoutCanvas canvas = editor.getCanvasControl();
            NodeFactory nodeFactory = canvas.getNodeFactory();
            UiElementNode parent = model.getUiRoot();
            UiElementNode child = parent.getUiChildren().get(0);
            if (child instanceof UiViewElementNode) {
                UiViewElementNode childUiNode = (UiViewElementNode) child;
                NodeProxy childNode = nodeFactory.create(childUiNode);

                // Applying create hooks as part of palette render should
                // not trigger model updates
                layoutEditorDelegate.getEditor().setIgnoreXmlUpdate(true);
                try {
                    canvas.getRulesEngine().callCreateHooks(layoutEditorDelegate.getEditor(),
                            null, childNode, InsertType.CREATE_PREVIEW);
                    childNode.applyPendingChanges();
                } catch (Throwable t) {
                    AdtPlugin.log(t, "Failed calling creation hooks for widget %1$s", viewName);
                } finally {
                    layoutEditorDelegate.getEditor().setIgnoreXmlUpdate(false);
                }
            }

            Integer overrideBgColor = null;
            boolean hasTransparency = false;
            LayoutLibrary layoutLibrary = editor.getLayoutLibrary();
            if (layoutLibrary != null &&
                    layoutLibrary.supports(Capability.CUSTOM_BACKGROUND_COLOR)) {
                // It doesn't matter what the background color is as long as the alpha
                // is 0 (fully transparent). We're using red to make it more obvious if
                // for some reason the background is painted when it shouldn't be.
                overrideBgColor = new Integer(0x00FF0000);
            }

            RenderSession session = null;
            try {
                // Use at most the size of the screen for the preview render.
                // This is important since when we fill the size of certain views (like
                // a SeekBar), we want it to at most be the width of the screen, and for small
                // screens the RENDER_WIDTH was wider.
                LayoutLog silentLogger = new LayoutLog();

                session = RenderService.create(editor)
                    .setModel(model)
                    .setMaxRenderSize(MAX_RENDER_WIDTH, MAX_RENDER_HEIGHT)
                    .setLog(silentLogger)
                    .setOverrideBgColor(overrideBgColor)
                    .setDecorations(false)
                    .createRenderSession();
            } catch (Throwable t) {
                // Previews can fail for a variety of reasons -- let's not bug
                // the user with it
                return null;
            }

            if (session != null) {
                if (session.getResult().isSuccess()) {
                    BufferedImage image = session.getImage();
                    if (image != null) {
                        BufferedImage cropped;
                        Rect initialCrop = null;
                        ViewInfo viewInfo = null;

                        List<ViewInfo> viewInfoList = session.getRootViews();

                        if (viewInfoList != null && viewInfoList.size() > 0) {
                            viewInfo = viewInfoList.get(0);
                            mBaseline = viewInfo.getBaseLine();
                        }

                        if (viewInfo != null) {
                            int x1 = viewInfo.getLeft();
                            int x2 = viewInfo.getRight();
                            int y2 = viewInfo.getBottom();
                            int y1 = viewInfo.getTop();
                            initialCrop = new Rect(x1, y1, x2 - x1, y2 - y1);
                        }

                        if (hasTransparency) {
                            cropped = ImageUtils.cropBlank(image, initialCrop);
                        } else {
                            // Find out what the "background" color is such that we can properly
                            // crop it out of the image. To do this we pick out a pixel in the
                            // bottom right unpainted area. Rather than pick the one in the far
                            // bottom corner, we pick one as close to the bounds of the view as
                            // possible (but still outside of the bounds), such that we can
                            // deal with themes like the dialog theme.
                            int edgeX = image.getWidth() -1;
                            int edgeY = image.getHeight() -1;
                            if (viewInfo != null) {
                                if (viewInfo.getRight() < image.getWidth()-1) {
                                    edgeX = viewInfo.getRight()+1;
                                }
                                if (viewInfo.getBottom() < image.getHeight()-1) {
                                    edgeY = viewInfo.getBottom()+1;
                                }
                            }
                            int edgeColor = image.getRGB(edgeX, edgeY);
                            cropped = ImageUtils.cropColor(image, edgeColor, initialCrop);
                        }

                        if (cropped != null) {
                            int width = initialCrop != null ? initialCrop.w : cropped.getWidth();
                            int height = initialCrop != null ? initialCrop.h : cropped.getHeight();
                            boolean needsContrast = hasTransparency
                                    && !ImageUtils.containsDarkPixels(cropped);
                            cropped = ImageUtils.createDropShadow(cropped,
                                    hasTransparency ? 3 : 5 /* shadowSize */,
                                    !hasTransparency ? 0.6f : needsContrast ? 0.8f : 0.7f/*alpha*/,
                                    0x000000 /* shadowRgb */);

                            double scale = canvas.getScale();
                            if (scale != 1L) {
                                cropped = ImageUtils.scale(cropped, scale, scale);
                            }

                            Display display = getDisplay();
                            int alpha = (!hasTransparency || !needsContrast) ? IMG_ALPHA : -1;
                            Image swtImage = SwtUtils.convertToSwt(display, cropped, true, alpha);
                            Rectangle imageBounds = new Rectangle(0, 0, width, height);
                            return Pair.of(swtImage, imageBounds);
                        }
                    }
                }

                session.dispose();
            }

            return null;
        }

        /**
         * Utility method to print out the contents of the given XML document. This is
         * really useful when working on the preview code above. I'm including all the
         * code inside a constant false, which means the compiler will omit all the code,
         * but I'd like to leave it in the code base and by doing it this way rather than
         * as commented out code the code won't be accidentally broken.
         */
        @SuppressWarnings("all")
        private void dumpDocument(Document document) {
            // Diagnostics: print out the XML that we're about to render
            if (false) { // Will be omitted by the compiler
                org.apache.xml.serialize.OutputFormat outputFormat =
                    new org.apache.xml.serialize.OutputFormat(
                            "XML", "ISO-8859-1", true); //$NON-NLS-1$ //$NON-NLS-2$
                outputFormat.setIndent(2);
                outputFormat.setLineWidth(100);
                outputFormat.setIndenting(true);
                outputFormat.setOmitXMLDeclaration(true);
                outputFormat.setOmitDocumentType(true);
                StringWriter stringWriter = new StringWriter();
                // Using FQN here to avoid having an import above, which will result
                // in a deprecation warning, and there isn't a way to annotate a single
                // import element with a SuppressWarnings.
                org.apache.xml.serialize.XMLSerializer serializer =
                    new org.apache.xml.serialize.XMLSerializer(stringWriter, outputFormat);
                serializer.setNamespaces(true);
                try {
                    serializer.serialize(document.getDocumentElement());
                    System.out.println(stringWriter.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /** Action for switching view modes via radio buttons */
    private class PaletteModeAction extends Action {
        private final PaletteMode mMode;

        PaletteModeAction(PaletteMode mode) {
            super(mode.getActionLabel(), IAction.AS_RADIO_BUTTON);
            mMode = mode;
            boolean selected = mMode == mPaletteMode;
            setChecked(selected);
            setEnabled(!selected);
        }

        @Override
        public void run() {
            if (isEnabled()) {
                mPaletteMode = mMode;
                refreshPalette();
                savePaletteMode();
            }
        }
    }

    /** Action for toggling various checkbox view modes - categories, sorting, etc */
    private class ToggleViewOptionAction extends Action {
        private final int mAction;
        final static int TOGGLE_CATEGORY = 1;
        final static int TOGGLE_ALPHABETICAL = 2;
        final static int TOGGLE_AUTO_CLOSE = 3;
        final static int REFRESH = 4;
        final static int RESET = 5;

        ToggleViewOptionAction(String title, int action, boolean checked) {
            super(title, (action == REFRESH || action == RESET) ? IAction.AS_PUSH_BUTTON
                    : IAction.AS_CHECK_BOX);
            mAction = action;
            if (checked) {
                setChecked(checked);
            }
        }

        @Override
        public void run() {
            switch (mAction) {
                case TOGGLE_CATEGORY:
                    mCategories = !mCategories;
                    refreshPalette();
                    break;
                case TOGGLE_ALPHABETICAL:
                    mAlphabetical = !mAlphabetical;
                    refreshPalette();
                    break;
                case TOGGLE_AUTO_CLOSE:
                    mAutoClose = !mAutoClose;
                    mAccordion.setAutoClose(mAutoClose);
                    break;
                case REFRESH:
                    mPreviewIconFactory.refresh();
                    refreshPalette();
                    break;
                case RESET:
                    mAlphabetical = false;
                    mCategories = true;
                    mAutoClose = true;
                    mPaletteMode = PaletteMode.SMALL_PREVIEW;
                    refreshPalette();
                    break;
            }
            savePaletteMode();
        }
    }

    private void addMenu(Control control) {
        control.addMenuDetectListener(new MenuDetectListener() {
            @Override
            public void menuDetected(MenuDetectEvent e) {
                showMenu(e.x, e.y);
            }
        });
    }

    private void showMenu(int x, int y) {
        MenuManager manager = new MenuManager() {
            @Override
            public boolean isDynamic() {
                return true;
            }
        };
        boolean previews = previewsAvailable();
        for (PaletteMode mode : PaletteMode.values()) {
            if (mode.isPreview() && !previews) {
                continue;
            }
            manager.add(new PaletteModeAction(mode));
        }
        if (mPaletteMode.isPreview()) {
            manager.add(new Separator());
            manager.add(new ToggleViewOptionAction("Refresh Previews",
                    ToggleViewOptionAction.REFRESH,
                    false));
        }
        manager.add(new Separator());
        manager.add(new ToggleViewOptionAction("Show Categories",
                ToggleViewOptionAction.TOGGLE_CATEGORY,
                mCategories));
        manager.add(new ToggleViewOptionAction("Sort Alphabetically",
                ToggleViewOptionAction.TOGGLE_ALPHABETICAL,
                mAlphabetical));
        manager.add(new Separator());
        manager.add(new ToggleViewOptionAction("Auto Close Previous",
                ToggleViewOptionAction.TOGGLE_AUTO_CLOSE,
                mAutoClose));
        manager.add(new Separator());
        manager.add(new ToggleViewOptionAction("Reset",
                ToggleViewOptionAction.RESET,
                false));

        Menu menu = manager.createContextMenu(PaletteControl.this);
        menu.setLocation(x, y);
        menu.setVisible(true);
    }

    private final class ViewFinderListener implements CustomViewFinder.Listener {
        private final Composite mParent;

        private ViewFinderListener(Composite parent) {
            mParent = parent;
        }

        @Override
        public void viewsUpdated(Collection<String> customViews,
                Collection<String> thirdPartyViews) {
            addCustomItems(mParent);
            mParent.layout(true);
        }
    }
}

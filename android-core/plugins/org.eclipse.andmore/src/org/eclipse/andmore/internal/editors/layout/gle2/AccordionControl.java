/*
 * Copyright (C) 2011 The Android Open Source Project
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

import org.eclipse.andmore.internal.editors.IconFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ScrollBar;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The accordion control allows a series of labels with associated content that can be
 * shown. For more details on accordions, see http://en.wikipedia.org/wiki/Accordion_(GUI)
 * <p>
 * This control allows the children to be created lazily. You can also customize the
 * composite which is created to hold the children items, to for example allow multiple
 * columns of items rather than just the default vertical stack.
 * <p>
 * The visual appearance of the headers is built in; it uses a mild gradient, with a
 * heavier gradient during mouse-overs. It also uses a bold label along with the eclipse
 * folder icons.
 * <p>
 * The control can be configured to enforce a single category open at any time (the
 * default), or allowing multiple categories open (where they share the available space).
 * The control can also be configured to fill the available vertical space for the open
 * category/categories.
 */
public abstract class AccordionControl extends Composite {
    /** Pixel spacing between header items */
    private static final int HEADER_SPACING = 0;

    /** Pixel spacing between items in the content area */
    private static final int ITEM_SPACING = 0;

    private static final String KEY_CONTENT = "content"; //$NON-NLS-1$
    private static final String KEY_HEADER = "header"; //$NON-NLS-1$

    private Image mClosed;
    private Image mOpen;
    private boolean mSingle = true;
    private boolean mWrap;

    /**
     * Creates the container which will hold the items in a category; this can be
     * overridden to lay out the children with a different layout than the default
     * vertical RowLayout
     */
    protected Composite createChildContainer(Composite parent, Object header, int style) {
        Composite composite = new Composite(parent, style);
        if (mWrap) {
            RowLayout layout = new RowLayout(SWT.HORIZONTAL);
            layout.center = true;
            composite.setLayout(layout);
        } else {
            RowLayout layout = new RowLayout(SWT.VERTICAL);
            layout.spacing = ITEM_SPACING;
            layout.marginHeight = 0;
            layout.marginWidth = 0;
            layout.marginLeft = 0;
            layout.marginTop = 0;
            layout.marginRight = 0;
            layout.marginBottom = 0;
            composite.setLayout(layout);
        }

        // TODO - maybe do multi-column arrangement for simple nodes
        return composite;
    }

    /**
     * Creates the children under a particular header
     *
     * @param parent the parent composite to add the SWT items to
     * @param header the header object that is being opened for the first time
     */
    protected abstract void createChildren(Composite parent, Object header);

    /**
     * Set whether a single category should be enforced or not (default=true)
     *
     * @param single if true, enforce a single category open at a time
     */
    public void setAutoClose(boolean single) {
        mSingle = single;
    }

    /**
     * Returns whether a single category should be enforced or not (default=true)
     *
     * @return true if only a single category can be open at a time
     */
    public boolean isAutoClose() {
        return mSingle;
    }

    /**
     * Returns the labels used as header categories
     *
     * @return list of header labels
     */
    public List<CLabel> getHeaderLabels() {
        List<CLabel> headers = new ArrayList<CLabel>();
        for (Control c : getChildren()) {
            if (c instanceof CLabel) {
                headers.add((CLabel) c);
            }
        }

        return headers;
    }

    /**
     * Show all categories
     *
     * @param performLayout if true, call {@link #layout} and {@link #pack} when done
     */
    public void expandAll(boolean performLayout) {
        for (Control c : getChildren()) {
            if (c instanceof CLabel) {
                if (!isOpen(c)) {
                    toggle((CLabel) c, false, false);
                }
            }
        }
        if (performLayout) {
            pack();
            layout();
        }
    }

    /**
     * Hide all categories
     *
     * @param performLayout if true, call {@link #layout} and {@link #pack} when done
     */
    public void collapseAll(boolean performLayout) {
        for (Control c : getChildren()) {
            if (c instanceof CLabel) {
                if (isOpen(c)) {
                    toggle((CLabel) c, false, false);
                }
            }
        }
        if (performLayout) {
            layout();
        }
    }

    /**
     * Create the composite.
     *
     * @param parent the parent widget to add the accordion to
     * @param style the SWT style mask to use
     * @param headers a list of headers, whose {@link Object#toString} method should
     *            produce the heading label
     * @param greedy if true, grow vertically as much as possible
     * @param wrapChildren if true, configure the child area to be horizontally laid out
     *            with wrapping
     * @param expand Set of headers to expand initially
     */
    public AccordionControl(Composite parent, int style, List<?> headers,
            boolean greedy, boolean wrapChildren, Set<String> expand) {
        super(parent, style);
        mWrap = wrapChildren;

        GridLayout gridLayout = new GridLayout(1, false);
        gridLayout.verticalSpacing = HEADER_SPACING;
        gridLayout.horizontalSpacing = 0;
        gridLayout.marginWidth = 0;
        gridLayout.marginHeight = 0;
        setLayout(gridLayout);

        Font labelFont = null;

        mOpen = IconFactory.getInstance().getIcon("open-folder");     //$NON-NLS-1$
        mClosed = IconFactory.getInstance().getIcon("closed-folder"); //$NON-NLS-1$
        List<CLabel> expandLabels = new ArrayList<CLabel>();

        for (Object header : headers) {
            final CLabel label = new CLabel(this, SWT.SHADOW_OUT);
            label.setText(header.toString().replace("&", "&&")); //$NON-NLS-1$ //$NON-NLS-2$
            updateBackground(label, false);
            if (labelFont == null) {
                labelFont = JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT);
            }
            label.setFont(labelFont);
            label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
            setHeader(header, label);
            label.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseUp(MouseEvent e) {
                    if (e.button == 1 && (e.stateMask & SWT.MODIFIER_MASK) == 0) {
                        toggle(label, true, mSingle);
                    }
                }
            });
            label.addMouseTrackListener(new MouseTrackListener() {
                @Override
                public void mouseEnter(MouseEvent e) {
                    updateBackground(label, true);
                }

                @Override
                public void mouseExit(MouseEvent e) {
                    updateBackground(label, false);
                }

                @Override
                public void mouseHover(MouseEvent e) {
                }
            });

            // Turn off border?
            final ScrolledComposite scrolledComposite = new ScrolledComposite(this, SWT.V_SCROLL);
            ScrollBar verticalBar = scrolledComposite.getVerticalBar();
            verticalBar.setIncrement(20);
            verticalBar.setPageIncrement(100);

            // Do we need the scrolled composite or can we just look at the next
            // wizard in the hierarchy?

            setContentArea(label, scrolledComposite);
            scrolledComposite.setExpandHorizontal(true);
            scrolledComposite.setExpandVertical(true);
            GridData scrollGridData = new GridData(SWT.FILL,
                    greedy ? SWT.FILL : SWT.TOP, false, greedy, 1, 1);
            scrollGridData.exclude = true;
            scrollGridData.grabExcessHorizontalSpace = wrapChildren;
            scrolledComposite.setLayoutData(scrollGridData);

            if (wrapChildren) {
                scrolledComposite.addControlListener(new ControlAdapter() {
                    @Override
                    public void controlResized(ControlEvent e) {
                        Rectangle r = scrolledComposite.getClientArea();
                        Control content = scrolledComposite.getContent();
                        if (content != null && r != null) {
                            Point minSize = content.computeSize(r.width, SWT.DEFAULT);
                            scrolledComposite.setMinSize(minSize);
                            ScrollBar vBar = scrolledComposite.getVerticalBar();
                            vBar.setPageIncrement(r.height);
                        }
                    }
                  });
            }

            updateIcon(label);
            if (expand != null && expand.contains(label.getText())) {
                // Comparing "label.getText()" rather than "header" because we make some
                // tweaks to the label (replacing & with && etc) and in the getExpandedCategories
                // method we return the label texts
                expandLabels.add(label);
            }
        }

        // Expand the requested categories
        for (CLabel label : expandLabels) {
            toggle(label, false, false);
        }
    }

    /** Updates the background gradient of the given header label */
    private void updateBackground(CLabel label, boolean mouseOver) {
        Display display = label.getDisplay();
        label.setBackground(new Color[] {
                display.getSystemColor(SWT.COLOR_WIDGET_HIGHLIGHT_SHADOW),
                display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND),
                display.getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW)
        }, new int[] {
                mouseOver ? 60 : 40, 100
        }, true);
    }

    /**
     * Updates the icon for a header label to be open/close based on the {@link #isOpen}
     * state
     */
    private void updateIcon(CLabel label) {
        label.setImage(isOpen(label) ? mOpen : mClosed);
    }

    /** Returns true if the content area for the given label is open/showing */
    private boolean isOpen(Control label) {
        return !((GridData) getContentArea(label).getLayoutData()).exclude;
    }

    /** Toggles the visibility of the children of the given label */
    private void toggle(CLabel label, boolean performLayout, boolean autoClose) {
        if (autoClose) {
            collapseAll(true);
        }
        ScrolledComposite scrolledComposite = getContentArea(label);

        GridData scrollGridData = (GridData) scrolledComposite.getLayoutData();
        boolean close = !scrollGridData.exclude;
        scrollGridData.exclude = close;
        scrolledComposite.setVisible(!close);
        updateIcon(label);

        if (!scrollGridData.exclude && scrolledComposite.getContent() == null) {
            Object header = getHeader(label);
            Composite composite = createChildContainer(scrolledComposite, header, SWT.NONE);
            createChildren(composite, header);
            while (composite.getParent() != scrolledComposite) {
                composite = composite.getParent();
            }
            scrolledComposite.setContent(composite);
            scrolledComposite.setMinSize(composite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
        }

        if (performLayout) {
            layout(true);
        }
    }

    /** Returns the header object for the given header label */
    private Object getHeader(Control label) {
        return label.getData(KEY_HEADER);
    }

    /** Sets the header object for the given header label */
    private void setHeader(Object header, final CLabel label) {
        label.setData(KEY_HEADER, header);
    }

    /** Returns the content area for the given header label */
    private ScrolledComposite getContentArea(Control label) {
        return (ScrolledComposite) label.getData(KEY_CONTENT);
    }

    /** Sets the content area for the given header label */
    private void setContentArea(final CLabel label, ScrolledComposite scrolledComposite) {
        label.setData(KEY_CONTENT, scrolledComposite);
    }

    @Override
    protected void checkSubclass() {
        // Disable the check that prevents subclassing of SWT components
    }

    /**
     * Returns the set of expanded categories in the palette. Note: Header labels will have
     * escaped ampersand characters with double ampersands.
     *
     * @return the set of expanded categories in the palette - never null
     */
    public Set<String> getExpandedCategories() {
        Set<String> expanded = new HashSet<String>();
        for (Control c : getChildren()) {
            if (c instanceof CLabel) {
                if (isOpen(c)) {
                    expanded.add(((CLabel) c).getText());
                }
            }
        }

        return expanded;
    }
}

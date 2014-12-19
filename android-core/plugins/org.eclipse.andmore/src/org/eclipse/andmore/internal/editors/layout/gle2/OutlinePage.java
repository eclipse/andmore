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

import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_COLUMN_COUNT;
import static com.android.SdkConstants.ATTR_LAYOUT_COLUMN;
import static com.android.SdkConstants.ATTR_LAYOUT_COLUMN_SPAN;
import static com.android.SdkConstants.ATTR_LAYOUT_GRAVITY;
import static com.android.SdkConstants.ATTR_LAYOUT_ROW;
import static com.android.SdkConstants.ATTR_LAYOUT_ROW_SPAN;
import static com.android.SdkConstants.ATTR_ROW_COUNT;
import static com.android.SdkConstants.ATTR_SRC;
import static com.android.SdkConstants.ATTR_TEXT;
import static com.android.SdkConstants.AUTO_URI;
import static com.android.SdkConstants.DRAWABLE_PREFIX;
import static com.android.SdkConstants.GRID_LAYOUT;
import static com.android.SdkConstants.LAYOUT_RESOURCE_PREFIX;
import static com.android.SdkConstants.URI_PREFIX;
import static org.eclipse.jface.viewers.StyledString.COUNTER_STYLER;
import static org.eclipse.jface.viewers.StyledString.QUALIFIER_STYLER;

import com.android.SdkConstants;
import com.android.annotations.VisibleForTesting;
import com.android.ide.common.api.INode;
import com.android.ide.common.api.InsertType;
import com.android.utils.Pair;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.AdtUtils;
import org.eclipse.andmore.common.layout.BaseLayoutRule;
import org.eclipse.andmore.common.layout.GridLayoutRule;
import org.eclipse.andmore.internal.editors.IconFactory;
import org.eclipse.andmore.internal.editors.descriptors.DescriptorsUtils;
import org.eclipse.andmore.internal.editors.layout.LayoutEditorDelegate;
import org.eclipse.andmore.internal.editors.layout.gle2.IncludeFinder.Reference;
import org.eclipse.andmore.internal.editors.layout.gre.NodeProxy;
import org.eclipse.andmore.internal.editors.layout.properties.PropertySheetPage;
import org.eclipse.andmore.internal.editors.layout.uimodel.UiViewElementNode;
import org.eclipse.andmore.internal.editors.manifest.ManifestInfo;
import org.eclipse.andmore.internal.editors.uimodel.UiElementNode;
import org.eclipse.andmore.internal.sdk.ProjectState;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.INullSelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;
import org.eclipse.wb.core.controls.SelfOrientingSashForm;
import org.eclipse.wb.internal.core.editor.structure.IPage;
import org.eclipse.wb.internal.core.editor.structure.PageSiteComposite;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * An outline page for the layout canvas view.
 * <p/>
 * The page is created by {@link LayoutEditorDelegate#delegateGetAdapter(Class)}. This means
 * we have *one* instance of the outline page per open canvas editor.
 * <p/>
 * It sets itself as a listener on the site's selection service in order to be
 * notified of the canvas' selection changes.
 * The underlying page is also a selection provider (via IContentOutlinePage)
 * and as such it will broadcast selection changes to the site's selection service
 * (on which both the layout editor part and the property sheet page listen.)
 */
public class OutlinePage extends ContentOutlinePage
    implements INullSelectionListener, IPage {

    /** Label which separates outline text from additional attributes like text prefix or url */
    private static final String LABEL_SEPARATOR = " - ";

    /** Max character count in labels, used for truncation */
    private static final int LABEL_MAX_WIDTH = 50;

    /**
     * The graphical editor that created this outline.
     */
    private final GraphicalEditorPart mGraphicalEditorPart;

    /**
     * RootWrapper is a workaround: we can't set the input of the TreeView to its root
     * element, so we introduce a fake parent.
     */
    private final RootWrapper mRootWrapper = new RootWrapper();

    /**
     * Menu manager for the context menu actions.
     * The actions delegate to the current GraphicalEditorPart.
     */
    private MenuManager mMenuManager;

    private Composite mControl;
    private PropertySheetPage mPropertySheet;
    private PageSiteComposite mPropertySheetComposite;
    private boolean mShowPropertySheet;
    private boolean mShowHeader;
    private boolean mIgnoreSelection;
    private boolean mActive = true;

    /** Action to Select All in the tree */
    private final Action mTreeSelectAllAction = new Action() {
        @Override
        public void run() {
            getTreeViewer().getTree().selectAll();
            OutlinePage.this.fireSelectionChanged(getSelection());
        }

        @Override
        public String getId() {
            return ActionFactory.SELECT_ALL.getId();
        }
    };

    /** Action for moving items up in the tree */
    private Action mMoveUpAction = new Action("Move Up\t-",
            IconFactory.getInstance().getImageDescriptor("up")) { //$NON-NLS-1$

        @Override
        public String getId() {
            return "adt.outline.moveup"; //$NON-NLS-1$
        }

        @Override
        public boolean isEnabled() {
            return canMove(false);
        }

        @Override
        public void run() {
            move(false);
        }
    };

    /** Action for moving items down in the tree */
    private Action mMoveDownAction = new Action("Move Down\t+",
            IconFactory.getInstance().getImageDescriptor("down")) { //$NON-NLS-1$

        @Override
        public String getId() {
            return "adt.outline.movedown"; //$NON-NLS-1$
        }

        @Override
        public boolean isEnabled() {
            return canMove(true);
        }

        @Override
        public void run() {
            move(true);
        }
    };

    /**
     * Creates a new {@link OutlinePage} associated with the given editor
     *
     * @param graphicalEditorPart the editor associated with this outline
     */
    public OutlinePage(GraphicalEditorPart graphicalEditorPart) {
        super();
        mGraphicalEditorPart = graphicalEditorPart;
    }

    @Override
    public Control getControl() {
        // We've injected some controls between the root of the outline page
        // and the tree control, so return the actual root (a sash form) rather
        // than the superclass' implementation which returns the tree. If we don't
        // do this, various checks in the outline page which checks that getControl().getParent()
        // is the outline window itself will ignore this page.
        return mControl;
    }

    void setActive(boolean active) {
        if (active != mActive) {
            mActive = active;

            // Outlines are by default active when they are created; this is intended
            // for deactivating a hidden outline and later reactivating it
            assert mControl != null;
            if (active) {
                getSite().getPage().addSelectionListener(this);
                setModel(mGraphicalEditorPart.getCanvasControl().getViewHierarchy().getRoot());
            } else {
                getSite().getPage().removeSelectionListener(this);
                mRootWrapper.setRoot(null);
                if (mPropertySheet != null) {
                    mPropertySheet.selectionChanged(null, TreeSelection.EMPTY);
                }
            }
        }
    }

    /** Refresh all the icon state */
    public void refreshIcons() {
        TreeViewer treeViewer = getTreeViewer();
        if (treeViewer != null) {
            Tree tree = treeViewer.getTree();
            if (tree != null && !tree.isDisposed()) {
                treeViewer.refresh();
            }
        }
    }

    /**
     * Set whether the outline should be shown in the header
     *
     * @param show whether a header should be shown
     */
    public void setShowHeader(boolean show) {
        mShowHeader = show;
    }

    /**
     * Set whether the property sheet should be shown within this outline
     *
     * @param show whether the property sheet should show
     */
    public void setShowPropertySheet(boolean show) {
        if (show != mShowPropertySheet) {
            mShowPropertySheet = show;
            if (mControl == null) {
                return;
            }

            if (show && mPropertySheet == null) {
                createPropertySheet();
            } else if (!show) {
                mPropertySheetComposite.dispose();
                mPropertySheetComposite = null;
                mPropertySheet.dispose();
                mPropertySheet = null;
            }

            mControl.layout();
        }
    }

    @Override
    public void createControl(Composite parent) {
        mControl = new SelfOrientingSashForm(parent, SWT.VERTICAL);

        if (mShowHeader) {
            PageSiteComposite mOutlineComposite = new PageSiteComposite(mControl, SWT.BORDER);
            mOutlineComposite.setTitleText("Outline");
            mOutlineComposite.setTitleImage(IconFactory.getInstance().getIcon("components_view"));
            mOutlineComposite.setPage(new IPage() {
                @Override
                public void createControl(Composite outlineParent) {
                    createOutline(outlineParent);
                }

                @Override
                public void dispose() {
                }

                @Override
                public Control getControl() {
                    return getTreeViewer().getTree();
                }

                @Override
                public void setToolBar(IToolBarManager toolBarManager) {
                    makeContributions(null, toolBarManager, null);
                    toolBarManager.update(false);
                }

                @Override
                public void setFocus() {
                    getControl().setFocus();
                }
            });
        } else {
            createOutline(mControl);
        }

        if (mShowPropertySheet) {
            createPropertySheet();
        }
    }

    private void createOutline(Composite parent) {
        if (AdtUtils.isEclipse4()) {
            // This is a workaround for the focus behavior in Eclipse 4 where
            // the framework ends up calling setFocus() on the first widget in the outline
            // AFTER a mouse click has been received. Specifically, if the user clicks in
            // the embedded property sheet to for example give a Text property editor focus,
            // then after the mouse click, the Outline window activation event is processed,
            // and this event causes setFocus() to be called first on the PageBookView (which
            // ends up calling setFocus on the first control, normally the TreeViewer), and
            // then on the Page itself. We're dealing with the page setFocus() in the override
            // of that method in the class, such that it does nothing.
            // However, we have to also disable the setFocus on the first control in the
            // outline page. To deal with that, we create our *own* first control in the
            // outline, and make its setFocus() a no-op. We also make it invisible, since we
            // don't actually want anything but the tree viewer showing in the outline.
            Text text = new Text(parent, SWT.NONE) {
                @Override
                public boolean setFocus() {
                    // Focus no-op
                    return true;
                }

                @Override
                protected void checkSubclass() {
                    // Disable the check that prevents subclassing of SWT components
                }
            };
            text.setVisible(false);
        }

        super.createControl(parent);

        TreeViewer tv = getTreeViewer();
        tv.setAutoExpandLevel(2);
        tv.setContentProvider(new ContentProvider());
        tv.setLabelProvider(new LabelProvider());
        tv.setInput(mRootWrapper);
        tv.expandToLevel(mRootWrapper.getRoot(), 2);

        int supportedOperations = DND.DROP_COPY | DND.DROP_MOVE;
        Transfer[] transfers = new Transfer[] {
            SimpleXmlTransfer.getInstance()
        };

        tv.addDropSupport(supportedOperations, transfers, new OutlineDropListener(this, tv));
        tv.addDragSupport(supportedOperations, transfers, new OutlineDragListener(this, tv));

        // The tree viewer will hold CanvasViewInfo instances, however these
        // change each time the canvas is reloaded. OTOH layoutlib gives us
        // constant UiView keys which we can use to perform tree item comparisons.
        tv.setComparer(new IElementComparer() {
            @Override
            public int hashCode(Object element) {
                if (element instanceof CanvasViewInfo) {
                    UiViewElementNode key = ((CanvasViewInfo) element).getUiViewNode();
                    if (key != null) {
                        return key.hashCode();
                    }
                }
                if (element != null) {
                    return element.hashCode();
                }
                return 0;
            }

            @Override
            public boolean equals(Object a, Object b) {
                if (a instanceof CanvasViewInfo && b instanceof CanvasViewInfo) {
                    UiViewElementNode keyA = ((CanvasViewInfo) a).getUiViewNode();
                    UiViewElementNode keyB = ((CanvasViewInfo) b).getUiViewNode();
                    if (keyA != null) {
                        return keyA.equals(keyB);
                    }
                }
                if (a != null) {
                    return a.equals(b);
                }
                return false;
            }
        });
        tv.addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(DoubleClickEvent event) {
                // This used to open the property view, but now that properties are docked
                // let's use it for something else -- such as showing the editor source
                /*
                // Front properties panel; its selection is already linked
                IWorkbenchPage page = getSite().getPage();
                try {
                    page.showView(IPageLayout.ID_PROP_SHEET, null, IWorkbenchPage.VIEW_ACTIVATE);
                } catch (PartInitException e) {
                    AdtPlugin.log(e, "Could not activate property sheet");
                }
                */

                TreeItem[] selection = getTreeViewer().getTree().getSelection();
                if (selection.length > 0) {
                    CanvasViewInfo vi = getViewInfo(selection[0].getData());
                    if (vi != null) {
                        LayoutCanvas canvas = mGraphicalEditorPart.getCanvasControl();
                        canvas.show(vi);
                    }
                }
            }
        });

        setupContextMenu();

        // Listen to selection changes from the layout editor
        getSite().getPage().addSelectionListener(this);
        getControl().addDisposeListener(new DisposeListener() {

            @Override
            public void widgetDisposed(DisposeEvent e) {
                dispose();
            }
        });

        Tree tree = tv.getTree();
        tree.addKeyListener(new KeyListener() {

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.character == '-') {
                    if (mMoveUpAction.isEnabled()) {
                        mMoveUpAction.run();
                    }
                } else if (e.character == '+') {
                    if (mMoveDownAction.isEnabled()) {
                        mMoveDownAction.run();
                    }
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
            }
        });

        setupTooltip();
    }

    /**
     * This flag is true when the mouse button is being pressed somewhere inside
     * the property sheet
     */
    private boolean mPressInPropSheet;

    private void createPropertySheet() {
        mPropertySheetComposite = new PageSiteComposite(mControl, SWT.BORDER);
        mPropertySheetComposite.setTitleText("Properties");
        mPropertySheetComposite.setTitleImage(IconFactory.getInstance().getIcon("properties_view"));
        mPropertySheet = new PropertySheetPage(mGraphicalEditorPart);
        mPropertySheetComposite.setPage(mPropertySheet);
        if (AdtUtils.isEclipse4()) {
            mPropertySheet.getControl().addMouseListener(new MouseListener() {
                @Override
                public void mouseDown(MouseEvent e) {
                    mPressInPropSheet = true;
                }

                @Override
                public void mouseUp(MouseEvent e) {
                    mPressInPropSheet = false;
                }

                @Override
                public void mouseDoubleClick(MouseEvent e) {
                }
            });
        }
    }

    @Override
    public void setFocus() {
        // Only call setFocus on the tree viewer if the mouse click isn't in the property
        // sheet area
        if (!mPressInPropSheet) {
            super.setFocus();
        }
    }

    @Override
    public void dispose() {
        mRootWrapper.setRoot(null);

        getSite().getPage().removeSelectionListener(this);
        super.dispose();
        if (mPropertySheet != null) {
            mPropertySheet.dispose();
            mPropertySheet = null;
        }
    }

    /**
     * Invoked by {@link LayoutCanvas} to set the model (a.k.a. the root view info).
     *
     * @param rootViewInfo The root of the view info hierarchy. Can be null.
     */
    public void setModel(CanvasViewInfo rootViewInfo) {
        if (!mActive) {
            return;
        }

        mRootWrapper.setRoot(rootViewInfo);

        TreeViewer tv = getTreeViewer();
        if (tv != null && !tv.getTree().isDisposed()) {
            Object[] expanded = tv.getExpandedElements();
            tv.refresh();
            tv.setExpandedElements(expanded);
            // Ensure that the root is expanded
            tv.expandToLevel(rootViewInfo, 2);
        }
    }

    /**
     * Returns the current tree viewer selection. Shouldn't be null,
     * although it can be {@link TreeSelection#EMPTY}.
     */
    @Override
    public ISelection getSelection() {
        return super.getSelection();
    }

    /**
     * Sets the outline selection.
     *
     * @param selection Only {@link ITreeSelection} will be used, otherwise the
     *   selection will be cleared (including a null selection).
     */
    @Override
    public void setSelection(ISelection selection) {
        // TreeViewer should be able to deal with a null selection, but let's make it safe
        if (selection == null) {
            selection = TreeSelection.EMPTY;
        }
        if (selection.equals(TreeSelection.EMPTY)) {
            return;
        }

        super.setSelection(selection);

        TreeViewer tv = getTreeViewer();
        if (tv == null || !(selection instanceof ITreeSelection) || selection.isEmpty()) {
            return;
        }

        // auto-reveal the selection
        ITreeSelection treeSel = (ITreeSelection) selection;
        for (TreePath p : treeSel.getPaths()) {
            tv.expandToLevel(p, 1);
        }
    }

    @Override
    protected void fireSelectionChanged(ISelection selection) {
        super.fireSelectionChanged(selection);
        if (mPropertySheet != null && !mIgnoreSelection) {
            mPropertySheet.selectionChanged(null, selection);
        }
    }

    /**
     * Listens to a workbench selection.
     * Only listen on selection coming from {@link LayoutEditorDelegate}, which avoid
     * picking up our own selections.
     */
    @Override
    public void selectionChanged(IWorkbenchPart part, ISelection selection) {
        if (mIgnoreSelection) {
            return;
        }

        if (part instanceof IEditorPart) {
            LayoutEditorDelegate delegate = LayoutEditorDelegate.fromEditor((IEditorPart) part);
            if (delegate != null) {
                try {
                    mIgnoreSelection = true;
                    setSelection(selection);

                    if (mPropertySheet != null) {
                        mPropertySheet.selectionChanged(part, selection);
                    }
                } finally {
                    mIgnoreSelection = false;
                }
            }
        }
    }

    @Override
    public void selectionChanged(SelectionChangedEvent event) {
        if (!mIgnoreSelection) {
            super.selectionChanged(event);
        }
    }

    // ----

    /**
     * In theory, the root of the model should be the input of the {@link TreeViewer},
     * which would be the root {@link CanvasViewInfo}.
     * That means in theory {@link ContentProvider#getElements(Object)} should return
     * its own input as the single root node.
     * <p/>
     * However as described in JFace Bug 9262, this case is not properly handled by
     * a {@link TreeViewer} and leads to an infinite recursion in the tree viewer.
     * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=9262
     * <p/>
     * The solution is to wrap the tree viewer input in a dummy root node that acts
     * as a parent. This class does just that.
     */
    private static class RootWrapper {
        private CanvasViewInfo mRoot;

        public void setRoot(CanvasViewInfo root) {
            mRoot = root;
        }

        public CanvasViewInfo getRoot() {
            return mRoot;
        }
    }

    /** Return the {@link CanvasViewInfo} associated with the given TreeItem's data field */
    /* package */ static CanvasViewInfo getViewInfo(Object viewData) {
        if (viewData instanceof RootWrapper) {
            return ((RootWrapper) viewData).getRoot();
        }
        if (viewData instanceof CanvasViewInfo) {
            return (CanvasViewInfo) viewData;
        }
        return null;
    }

    // --- Content and Label Providers ---

    /**
     * Content provider for the Outline model.
     * Objects are going to be {@link CanvasViewInfo}.
     */
    private static class ContentProvider implements ITreeContentProvider {

        @Override
        public Object[] getChildren(Object element) {
            if (element instanceof RootWrapper) {
                CanvasViewInfo root = ((RootWrapper)element).getRoot();
                if (root != null) {
                    return new Object[] { root };
                }
            }
            if (element instanceof CanvasViewInfo) {
                List<CanvasViewInfo> children = ((CanvasViewInfo) element).getUniqueChildren();
                if (children != null) {
                    return children.toArray();
                }
            }
            return new Object[0];
        }

        @Override
        public Object getParent(Object element) {
            if (element instanceof CanvasViewInfo) {
                return ((CanvasViewInfo) element).getParent();
            }
            return null;
        }

        @Override
        public boolean hasChildren(Object element) {
            if (element instanceof CanvasViewInfo) {
                List<CanvasViewInfo> children = ((CanvasViewInfo) element).getChildren();
                if (children != null) {
                    return children.size() > 0;
                }
            }
            return false;
        }

        /**
         * Returns the root element.
         * Semantically, the root element is the single top-level XML element of the XML layout.
         */
        @Override
        public Object[] getElements(Object inputElement) {
            return getChildren(inputElement);
        }

        @Override
        public void dispose() {
            // pass
        }

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            // pass
        }
    }

    /**
     * Label provider for the Outline model.
     * Objects are going to be {@link CanvasViewInfo}.
     */
    private class LabelProvider extends StyledCellLabelProvider {
        /**
         * Returns the element's logo with a fallback on the android logo.
         *
         * @param element the tree element
         * @return the image to be used as a logo
         */
        public Image getImage(Object element) {
            if (element instanceof CanvasViewInfo) {
                element = ((CanvasViewInfo) element).getUiViewNode();
            }

            if (element instanceof UiViewElementNode) {
                UiViewElementNode v = (UiViewElementNode) element;
                return v.getIcon();
            }

            return AdtPlugin.getAndroidLogo();
        }

        /**
         * Uses {@link UiElementNode#getStyledDescription} for the label for this tree item.
         */
        @Override
        public void update(ViewerCell cell) {
            Object element = cell.getElement();
            StyledString styledString = null;

            CanvasViewInfo vi = null;
            if (element instanceof CanvasViewInfo) {
                vi = (CanvasViewInfo) element;
                element = vi.getUiViewNode();
            }

            Image image = getImage(element);

            if (element instanceof UiElementNode) {
                UiElementNode node = (UiElementNode) element;
                styledString = node.getStyledDescription();
                Node xmlNode = node.getXmlNode();
                if (xmlNode instanceof Element) {
                    Element e = (Element) xmlNode;

                    // Temporary diagnostics code when developing GridLayout
                    if (GridLayoutRule.sDebugGridLayout) {

                        String namespace;
                        if (e.getNodeName().equals(GRID_LAYOUT) ||
                                e.getParentNode() != null
                                && e.getParentNode().getNodeName().equals(GRID_LAYOUT)) {
                            namespace = ANDROID_URI;
                        } else {
                            // Else: probably a v7 gridlayout
                            IProject project = mGraphicalEditorPart.getProject();
                            ProjectState projectState = Sdk.getProjectState(project);
                            if (projectState != null && projectState.isLibrary()) {
                                namespace = AUTO_URI;
                            } else {
                                ManifestInfo info = ManifestInfo.get(project);
                                namespace = URI_PREFIX + info.getPackage();
                            }
                        }

                        if (e.getNodeName() != null && e.getNodeName().endsWith(GRID_LAYOUT)) {
                            // Attach rowCount/columnCount info
                            String rowCount = e.getAttributeNS(namespace, ATTR_ROW_COUNT);
                            if (rowCount.length() == 0) {
                                rowCount = "?";
                            }
                            String columnCount = e.getAttributeNS(namespace, ATTR_COLUMN_COUNT);
                            if (columnCount.length() == 0) {
                                columnCount = "?";
                            }

                            styledString.append(" - columnCount=", QUALIFIER_STYLER);
                            styledString.append(columnCount, QUALIFIER_STYLER);
                            styledString.append(", rowCount=", QUALIFIER_STYLER);
                            styledString.append(rowCount, QUALIFIER_STYLER);
                        } else if (e.getParentNode() != null
                            && e.getParentNode().getNodeName() != null
                            && e.getParentNode().getNodeName().endsWith(GRID_LAYOUT)) {
                            // Attach row/column info
                            String row = e.getAttributeNS(namespace, ATTR_LAYOUT_ROW);
                            if (row.length() == 0) {
                                row = "?";
                            }
                            Styler colStyle = QUALIFIER_STYLER;
                            String column = e.getAttributeNS(namespace, ATTR_LAYOUT_COLUMN);
                            if (column.length() == 0) {
                                column = "?";
                            } else {
                                String colCount = ((Element) e.getParentNode()).getAttributeNS(
                                        namespace, ATTR_COLUMN_COUNT);
                                if (colCount.length() > 0 && Integer.parseInt(colCount) <=
                                        Integer.parseInt(column)) {
                                    colStyle = StyledString.createColorRegistryStyler(
                                        JFacePreferences.ERROR_COLOR, null);
                                }
                            }
                            String rowSpan = e.getAttributeNS(namespace, ATTR_LAYOUT_ROW_SPAN);
                            String columnSpan = e.getAttributeNS(namespace,
                                    ATTR_LAYOUT_COLUMN_SPAN);
                            if (rowSpan.length() == 0) {
                                rowSpan = "1";
                            }
                            if (columnSpan.length() == 0) {
                                columnSpan = "1";
                            }

                            styledString.append(" - cell (row=", QUALIFIER_STYLER);
                            styledString.append(row, QUALIFIER_STYLER);
                            styledString.append(',', QUALIFIER_STYLER);
                            styledString.append("col=", colStyle);
                            styledString.append(column, colStyle);
                            styledString.append(')', colStyle);
                            styledString.append(", span=(", QUALIFIER_STYLER);
                            styledString.append(columnSpan, QUALIFIER_STYLER);
                            styledString.append(',', QUALIFIER_STYLER);
                            styledString.append(rowSpan, QUALIFIER_STYLER);
                            styledString.append(')', QUALIFIER_STYLER);

                            String gravity = e.getAttributeNS(namespace, ATTR_LAYOUT_GRAVITY);
                            if (gravity != null && gravity.length() > 0) {
                                styledString.append(" : ", COUNTER_STYLER);
                                styledString.append(gravity, COUNTER_STYLER);
                            }

                        }
                    }

                    if (e.hasAttributeNS(ANDROID_URI, ATTR_TEXT)) {
                        // Show the text attribute
                        String text = e.getAttributeNS(ANDROID_URI, ATTR_TEXT);
                        if (text != null && text.length() > 0
                                && !text.contains(node.getDescriptor().getUiName())) {
                            if (text.charAt(0) == '@') {
                                String resolved = mGraphicalEditorPart.findString(text);
                                if (resolved != null) {
                                    text = resolved;
                                }
                            }
                            if (styledString.length() < LABEL_MAX_WIDTH - LABEL_SEPARATOR.length()
                                    - 2) {
                                styledString.append(LABEL_SEPARATOR, QUALIFIER_STYLER);

                                styledString.append('"', QUALIFIER_STYLER);
                                styledString.append(truncate(text, styledString), QUALIFIER_STYLER);
                                styledString.append('"', QUALIFIER_STYLER);
                            }
                        }
                    } else if (e.hasAttributeNS(ANDROID_URI, ATTR_SRC)) {
                        // Show ImageView source attributes etc
                        String src = e.getAttributeNS(ANDROID_URI, ATTR_SRC);
                        if (src != null && src.length() > 0) {
                            if (src.startsWith(DRAWABLE_PREFIX)) {
                                src = src.substring(DRAWABLE_PREFIX.length());
                            }
                            styledString.append(LABEL_SEPARATOR, QUALIFIER_STYLER);
                            styledString.append(truncate(src, styledString), QUALIFIER_STYLER);
                        }
                    } else if (e.getTagName().equals(SdkConstants.VIEW_INCLUDE)) {
                        // Show the include reference.

                        // Note: the layout attribute is NOT in the Android namespace
                        String src = e.getAttribute(SdkConstants.ATTR_LAYOUT);
                        if (src != null && src.length() > 0) {
                            if (src.startsWith(LAYOUT_RESOURCE_PREFIX)) {
                                src = src.substring(LAYOUT_RESOURCE_PREFIX.length());
                            }
                            styledString.append(LABEL_SEPARATOR, QUALIFIER_STYLER);
                            styledString.append(truncate(src, styledString), QUALIFIER_STYLER);
                        }
                    }
                }
            } else if (element == null && vi != null) {
                // It's an inclusion-context: display it
                Reference includedWithin = mGraphicalEditorPart.getIncludedWithin();
                if (includedWithin != null) {
                    styledString = new StyledString();
                    styledString.append(includedWithin.getDisplayName(), QUALIFIER_STYLER);
                    image = IconFactory.getInstance().getIcon(SdkConstants.VIEW_INCLUDE);
                }
            }

            if (styledString == null) {
                styledString = new StyledString();
                styledString.append(element == null ? "(null)" : element.toString());
            }

           cell.setText(styledString.toString());
           cell.setStyleRanges(styledString.getStyleRanges());
           cell.setImage(image);
           super.update(cell);
       }

        @Override
        public boolean isLabelProperty(Object element, String property) {
            return super.isLabelProperty(element, property);
        }
    }

    // --- Context Menu ---

    /**
     * This viewer uses its own actions that delegate to the ones given
     * by the {@link LayoutCanvas}. All the processing is actually handled
     * directly by the canvas and this viewer only gets refreshed as a
     * consequence of the canvas changing the XML model.
     */
    private void setupContextMenu() {

        mMenuManager = new MenuManager();
        mMenuManager.removeAll();

        mMenuManager.add(mMoveUpAction);
        mMenuManager.add(mMoveDownAction);
        mMenuManager.add(new Separator());

        mMenuManager.add(new SelectionManager.SelectionMenu(mGraphicalEditorPart));
        mMenuManager.add(new Separator());
        final String prefix = LayoutCanvas.PREFIX_CANVAS_ACTION;
        mMenuManager.add(new DelegateAction(prefix + ActionFactory.CUT.getId()));
        mMenuManager.add(new DelegateAction(prefix + ActionFactory.COPY.getId()));
        mMenuManager.add(new DelegateAction(prefix + ActionFactory.PASTE.getId()));

        mMenuManager.add(new Separator());

        mMenuManager.add(new DelegateAction(prefix + ActionFactory.DELETE.getId()));

        mMenuManager.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(IMenuManager manager) {
                // Update all actions to match their LayoutCanvas counterparts
                for (IContributionItem contrib : manager.getItems()) {
                    if (contrib instanceof ActionContributionItem) {
                        IAction action = ((ActionContributionItem) contrib).getAction();
                        if (action instanceof DelegateAction) {
                            ((DelegateAction) action).updateFromEditorPart(mGraphicalEditorPart);
                        }
                    }
                }
            }
        });

        new DynamicContextMenu(
                mGraphicalEditorPart.getEditorDelegate(),
                mGraphicalEditorPart.getCanvasControl(),
                mMenuManager);

        getTreeViewer().getTree().setMenu(mMenuManager.createContextMenu(getControl()));

        // Update Move Up/Move Down state only when the menu is opened
        getTreeViewer().getTree().addMenuDetectListener(new MenuDetectListener() {
            @Override
            public void menuDetected(MenuDetectEvent e) {
                mMenuManager.update(IAction.ENABLED);
            }
        });
    }

    /**
     * An action that delegates its properties and behavior to a target action.
     * The target action can be null or it can change overtime, typically as the
     * layout canvas' editor part is activated or closed.
     */
    private static class DelegateAction extends Action {
        private IAction mTargetAction;
        private final String mCanvasActionId;

        public DelegateAction(String canvasActionId) {
            super(canvasActionId);
            setId(canvasActionId);
            mCanvasActionId = canvasActionId;
        }

        // --- Methods form IAction ---

        /** Returns the target action's {@link #isEnabled()} if defined, or false. */
        @Override
        public boolean isEnabled() {
            return mTargetAction == null ? false : mTargetAction.isEnabled();
        }

        /** Returns the target action's {@link #isChecked()} if defined, or false. */
        @Override
        public boolean isChecked() {
            return mTargetAction == null ? false : mTargetAction.isChecked();
        }

        /** Returns the target action's {@link #isHandled()} if defined, or false. */
        @Override
        public boolean isHandled() {
            return mTargetAction == null ? false : mTargetAction.isHandled();
        }

        /** Runs the target action if defined. */
        @Override
        public void run() {
            if (mTargetAction != null) {
                mTargetAction.run();
            }
            super.run();
        }

        /**
         * Updates this action to delegate to its counterpart in the given editor part
         *
         * @param editorPart The editor being updated
         */
        public void updateFromEditorPart(GraphicalEditorPart editorPart) {
            LayoutCanvas canvas = editorPart == null ? null : editorPart.getCanvasControl();
            if (canvas == null) {
                mTargetAction = null;
            } else {
                mTargetAction = canvas.getAction(mCanvasActionId);
            }

            if (mTargetAction != null) {
                setText(mTargetAction.getText());
                setId(mTargetAction.getId());
                setDescription(mTargetAction.getDescription());
                setImageDescriptor(mTargetAction.getImageDescriptor());
                setHoverImageDescriptor(mTargetAction.getHoverImageDescriptor());
                setDisabledImageDescriptor(mTargetAction.getDisabledImageDescriptor());
                setToolTipText(mTargetAction.getToolTipText());
                setActionDefinitionId(mTargetAction.getActionDefinitionId());
                setHelpListener(mTargetAction.getHelpListener());
                setAccelerator(mTargetAction.getAccelerator());
                setChecked(mTargetAction.isChecked());
                setEnabled(mTargetAction.isEnabled());
            } else {
                setEnabled(false);
            }
        }
    }

    /** Returns the associated editor with this outline */
    /* package */GraphicalEditorPart getEditor() {
        return mGraphicalEditorPart;
    }

    @Override
    public void setActionBars(IActionBars actionBars) {
        super.setActionBars(actionBars);

        // Map Outline actions to canvas actions such that they share Undo context etc
        LayoutCanvas canvas = mGraphicalEditorPart.getCanvasControl();
        canvas.updateGlobalActions(actionBars);

        // Special handling for Select All since it's different than the canvas (will
        // include selecting the root etc)
        actionBars.setGlobalActionHandler(mTreeSelectAllAction.getId(), mTreeSelectAllAction);
        actionBars.updateActionBars();
    }

    // ---- Move Up/Down Support ----

    /** Returns true if the current selected item can be moved */
    private boolean canMove(boolean forward) {
        CanvasViewInfo viewInfo = getSingleSelectedItem();
        if (viewInfo != null) {
            UiViewElementNode node = viewInfo.getUiViewNode();
            if (forward) {
                return findNext(node) != null;
            } else {
                return findPrevious(node) != null;
            }
        }

        return false;
    }

    /** Moves the current selected item down (forward) or up (not forward) */
    private void move(boolean forward) {
        CanvasViewInfo viewInfo = getSingleSelectedItem();
        if (viewInfo != null) {
            final Pair<UiViewElementNode, Integer> target;
            UiViewElementNode selected = viewInfo.getUiViewNode();
            if (forward) {
                target = findNext(selected);
            } else {
                target = findPrevious(selected);
            }
            if (target != null) {
                final LayoutCanvas canvas = mGraphicalEditorPart.getCanvasControl();
                final SelectionManager selectionManager = canvas.getSelectionManager();
                final ArrayList<SelectionItem> dragSelection = new ArrayList<SelectionItem>();
                dragSelection.add(selectionManager.createSelection(viewInfo));
                SelectionManager.sanitize(dragSelection);

                if (!dragSelection.isEmpty()) {
                    final SimpleElement[] elements = SelectionItem.getAsElements(dragSelection);
                    UiViewElementNode parentNode = target.getFirst();
                    final NodeProxy targetNode = canvas.getNodeFactory().create(parentNode);

                    // Record children of the target right before the drop (such that we
                    // can find out after the drop which exact children were inserted)
                    Set<INode> children = new HashSet<INode>();
                    for (INode node : targetNode.getChildren()) {
                        children.add(node);
                    }

                    String label = MoveGesture.computeUndoLabel(targetNode,
                            elements, DND.DROP_MOVE);
                    canvas.getEditorDelegate().getEditor().wrapUndoEditXmlModel(label, new Runnable() {
                        @Override
                        public void run() {
                            InsertType insertType = InsertType.MOVE_INTO;
                            if (dragSelection.get(0).getNode().getParent() == targetNode) {
                                insertType = InsertType.MOVE_WITHIN;
                            }
                            canvas.getRulesEngine().setInsertType(insertType);
                            int index = target.getSecond();
                            BaseLayoutRule.insertAt(targetNode, elements, false, index);
                            targetNode.applyPendingChanges();
                            canvas.getClipboardSupport().deleteSelection("Remove", dragSelection);
                        }
                    });

                    // Now find out which nodes were added, and look up their
                    // corresponding CanvasViewInfos
                    final List<INode> added = new ArrayList<INode>();
                    for (INode node : targetNode.getChildren()) {
                        if (!children.contains(node)) {
                            added.add(node);
                        }
                    }

                    selectionManager.setOutlineSelection(added);
                }
            }
        }
    }

    /**
     * Returns the {@link CanvasViewInfo} for the currently selected item, or null if
     * there are no or multiple selected items
     *
     * @return the current selected item if there is exactly one item selected
     */
    private CanvasViewInfo getSingleSelectedItem() {
        TreeItem[] selection = getTreeViewer().getTree().getSelection();
        if (selection.length == 1) {
            return getViewInfo(selection[0].getData());
        }

        return null;
    }


    /** Returns the pair [parent,index] of the next node (when iterating forward) */
    @VisibleForTesting
    /* package */ static Pair<UiViewElementNode, Integer> findNext(UiViewElementNode node) {
        UiElementNode parent = node.getUiParent();
        if (parent == null) {
            return null;
        }

        UiElementNode next = node.getUiNextSibling();
        if (next != null) {
            if (DescriptorsUtils.canInsertChildren(next.getDescriptor(), null)) {
                return getFirstPosition(next);
            } else {
                return getPositionAfter(next);
            }
        }

        next = parent.getUiNextSibling();
        if (next != null) {
            return getPositionBefore(next);
        } else {
            UiElementNode grandParent = parent.getUiParent();
            if (grandParent != null) {
                return getLastPosition(grandParent);
            }
        }

        return null;
    }

    /** Returns the pair [parent,index] of the previous node (when iterating backward) */
    @VisibleForTesting
    /* package */ static Pair<UiViewElementNode, Integer> findPrevious(UiViewElementNode node) {
        UiElementNode prev = node.getUiPreviousSibling();
        if (prev != null) {
            UiElementNode curr = prev;
            while (true) {
                List<UiElementNode> children = curr.getUiChildren();
                if (children.size() > 0) {
                    curr = children.get(children.size() - 1);
                    continue;
                }
                if (DescriptorsUtils.canInsertChildren(curr.getDescriptor(), null)) {
                    return getFirstPosition(curr);
                } else {
                    if (curr == prev) {
                        return getPositionBefore(curr);
                    } else {
                        return getPositionAfter(curr);
                    }
                }
            }
        }

        return getPositionBefore(node.getUiParent());
    }

    /** Returns the pair [parent,index] of the position immediately before the given node  */
    private static Pair<UiViewElementNode, Integer> getPositionBefore(UiElementNode node) {
        if (node != null) {
            UiElementNode parent = node.getUiParent();
            if (parent != null && parent instanceof UiViewElementNode) {
                return Pair.of((UiViewElementNode) parent, node.getUiSiblingIndex());
            }
        }

        return null;
    }

    /** Returns the pair [parent,index] of the position immediately following the given node  */
    private static Pair<UiViewElementNode, Integer> getPositionAfter(UiElementNode node) {
        if (node != null) {
            UiElementNode parent = node.getUiParent();
            if (parent != null && parent instanceof UiViewElementNode) {
                return Pair.of((UiViewElementNode) parent, node.getUiSiblingIndex() + 1);
            }
        }

        return null;
    }

    /** Returns the pair [parent,index] of the first position inside the given parent */
    private static Pair<UiViewElementNode, Integer> getFirstPosition(UiElementNode parent) {
        if (parent != null && parent instanceof UiViewElementNode) {
            return Pair.of((UiViewElementNode) parent, 0);
        }

        return null;
    }

    /**
     * Returns the pair [parent,index] of the last position after the given node's
     * children
     */
    private static Pair<UiViewElementNode, Integer> getLastPosition(UiElementNode parent) {
        if (parent != null && parent instanceof UiViewElementNode) {
            return Pair.of((UiViewElementNode) parent, parent.getUiChildren().size());
        }

        return null;
    }

    /**
     * Truncates the given text such that it will fit into the given {@link StyledString}
     * up to a maximum length of {@link #LABEL_MAX_WIDTH}.
     *
     * @param text the text to truncate
     * @param string the existing string to be appended to
     * @return the truncated string
     */
    private static String truncate(String text, StyledString string) {
        int existingLength = string.length();

        if (text.length() + existingLength > LABEL_MAX_WIDTH) {
            int truncatedLength = LABEL_MAX_WIDTH - existingLength - 3;
            if (truncatedLength > 0) {
                return String.format("%1$s...", text.substring(0, truncatedLength));
            } else {
                return ""; //$NON-NLS-1$
            }
        }

        return text;
    }

    @Override
    public void setToolBar(IToolBarManager toolBarManager) {
        makeContributions(null, toolBarManager, null);
        toolBarManager.update(false);
    }

    /**
     * Sets up a custom tooltip when hovering over tree items. It currently displays the error
     * message for the lint warning associated with each node, if any (and only if the hover
     * is over the icon portion).
     */
    private void setupTooltip() {
        final Tree tree = getTreeViewer().getTree();

        // This is based on SWT Snippet 125
        final Listener listener = new Listener() {
            Shell mTip = null;
            Label mLabel  = null;

            @Override
            public void handleEvent(Event event) {
                switch(event.type) {
                case SWT.Dispose:
                case SWT.KeyDown:
                case SWT.MouseExit:
                case SWT.MouseDown:
                case SWT.MouseMove:
                    if (mTip != null) {
                        mTip.dispose();
                        mTip = null;
                        mLabel = null;
                    }
                    break;
                case SWT.MouseHover:
                    if (mTip != null) {
                        mTip.dispose();
                        mTip = null;
                        mLabel = null;
                    }

                    String tooltip = null;

                    TreeItem item = tree.getItem(new Point(event.x, event.y));
                    if (item != null) {
                        Rectangle rect = item.getBounds(0);
                        if (event.x - rect.x > 16) { // 16: Standard width of our outline icons
                            return;
                        }

                        Object data = item.getData();
                        if (data != null && data instanceof CanvasViewInfo) {
                            LayoutEditorDelegate editor = mGraphicalEditorPart.getEditorDelegate();
                            CanvasViewInfo vi = (CanvasViewInfo) data;
                            IMarker marker = editor.getIssueForNode(vi.getUiViewNode());
                            if (marker != null) {
                                tooltip = marker.getAttribute(IMarker.MESSAGE, null);
                            }
                        }

                        if (tooltip != null) {
                            Shell shell = tree.getShell();
                            Display display = tree.getDisplay();

                            Color fg = display.getSystemColor(SWT.COLOR_INFO_FOREGROUND);
                            Color bg = display.getSystemColor(SWT.COLOR_INFO_BACKGROUND);
                            mTip = new Shell(shell, SWT.ON_TOP | SWT.NO_FOCUS | SWT.TOOL);
                            mTip.setBackground(bg);
                            FillLayout layout = new FillLayout();
                            layout.marginWidth = 1;
                            layout.marginHeight = 1;
                            mTip.setLayout(layout);
                            mLabel = new Label(mTip, SWT.WRAP);
                            mLabel.setForeground(fg);
                            mLabel.setBackground(bg);
                            mLabel.setText(tooltip);
                            mLabel.addListener(SWT.MouseExit, this);
                            mLabel.addListener(SWT.MouseDown, this);

                            Point pt = tree.toDisplay(rect.x, rect.y + rect.height);
                            Rectangle displayBounds = display.getBounds();
                            // -10: Don't extend -all- the way to the edge of the screen
                            // which would make it look like it has been cropped
                            int availableWidth = displayBounds.x + displayBounds.width - pt.x - 10;
                            if (availableWidth < 80) {
                                availableWidth = 80;
                            }
                            Point size = mTip.computeSize(SWT.DEFAULT, SWT.DEFAULT);
                            if (size.x > availableWidth) {
                                size = mTip.computeSize(availableWidth, SWT.DEFAULT);
                            }
                            mTip.setBounds(pt.x, pt.y, size.x, size.y);

                            mTip.setVisible(true);
                        }
                    }
                }
            }
        };

        tree.addListener(SWT.Dispose, listener);
        tree.addListener(SWT.KeyDown, listener);
        tree.addListener(SWT.MouseMove, listener);
        tree.addListener(SWT.MouseHover, listener);
    }
}

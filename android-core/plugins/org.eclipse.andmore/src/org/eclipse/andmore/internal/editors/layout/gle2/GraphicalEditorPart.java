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

import static com.android.SdkConstants.ANDROID_PKG;
import static com.android.SdkConstants.ANDROID_STRING_PREFIX;
import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_CONTEXT;
import static com.android.SdkConstants.ATTR_ID;
import static com.android.SdkConstants.ATTR_LAYOUT_HEIGHT;
import static com.android.SdkConstants.ATTR_LAYOUT_WIDTH;
import static com.android.SdkConstants.FD_GEN_SOURCES;
import static com.android.SdkConstants.GRID_LAYOUT;
import static com.android.SdkConstants.SCROLL_VIEW;
import static com.android.SdkConstants.STRING_PREFIX;
import static com.android.SdkConstants.VALUE_FALSE;
import static com.android.SdkConstants.VALUE_FILL_PARENT;
import static com.android.SdkConstants.VALUE_MATCH_PARENT;
import static com.android.SdkConstants.VALUE_WRAP_CONTENT;
import static org.eclipse.andmore.internal.editors.layout.RenderSecurityManager.ENABLED_PROPERTY;
import static org.eclipse.andmore.internal.editors.layout.configuration.Configuration.CFG_DEVICE;
import static org.eclipse.andmore.internal.editors.layout.configuration.Configuration.CFG_DEVICE_STATE;
import static org.eclipse.andmore.internal.editors.layout.configuration.Configuration.CFG_FOLDER;
import static org.eclipse.andmore.internal.editors.layout.configuration.Configuration.CFG_TARGET;
import static org.eclipse.andmore.internal.editors.layout.descriptors.ViewElementDescriptor.viewNeedsPackage;
import static org.eclipse.wb.core.controls.flyout.IFlyoutPreferences.DOCK_EAST;
import static org.eclipse.wb.core.controls.flyout.IFlyoutPreferences.DOCK_WEST;
import static org.eclipse.wb.core.controls.flyout.IFlyoutPreferences.STATE_COLLAPSED;
import static org.eclipse.wb.core.controls.flyout.IFlyoutPreferences.STATE_OPEN;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.rendering.LayoutLibrary;
import com.android.ide.common.rendering.RenderSecurityException;
import com.android.ide.common.rendering.StaticRenderSession;
import com.android.ide.common.rendering.api.Capability;
import com.android.ide.common.rendering.api.LayoutLog;
import com.android.ide.common.rendering.api.RenderSession;
import com.android.ide.common.rendering.api.ResourceValue;
import com.android.ide.common.rendering.api.Result;
import com.android.ide.common.rendering.api.SessionParams.RenderingMode;
import com.android.ide.common.resources.ResourceRepository;
import com.android.ide.common.resources.ResourceResolver;
import com.android.ide.common.resources.configuration.FolderConfiguration;
import com.android.ide.common.sdk.LoadStatus;
import com.android.resources.Density;
import com.android.resources.ResourceFolderType;
import com.android.resources.ResourceType;
import com.android.sdklib.IAndroidTarget;
import com.android.tools.lint.detector.api.LintUtils;
import com.android.utils.Pair;

import org.eclipse.andmore.AndmoreAndroidConstants;
import org.eclipse.andmore.AndmoreAndroidPlugin;
import org.eclipse.andmore.AdtUtils;
import org.eclipse.andmore.common.layout.BaseLayoutRule;
import org.eclipse.andmore.common.layout.BaseViewRule;
import org.eclipse.andmore.internal.editors.AndroidXmlEditor;
import org.eclipse.andmore.internal.editors.IPageImageProvider;
import org.eclipse.andmore.internal.editors.IconFactory;
import org.eclipse.andmore.internal.editors.common.CommonXmlDelegate;
import org.eclipse.andmore.internal.editors.common.CommonXmlEditor;
import org.eclipse.andmore.internal.editors.layout.LayoutEditorDelegate;
import org.eclipse.andmore.internal.editors.layout.LayoutReloadMonitor;
import org.eclipse.andmore.internal.editors.layout.ProjectCallback;
import org.eclipse.andmore.internal.editors.layout.RenderSecurityManager;
import org.eclipse.andmore.internal.editors.layout.LayoutReloadMonitor.ChangeFlags;
import org.eclipse.andmore.internal.editors.layout.LayoutReloadMonitor.ILayoutReloadListener;
import org.eclipse.andmore.internal.editors.layout.configuration.Configuration;
import org.eclipse.andmore.internal.editors.layout.configuration.ConfigurationChooser;
import org.eclipse.andmore.internal.editors.layout.configuration.ConfigurationClient;
import org.eclipse.andmore.internal.editors.layout.configuration.ConfigurationDescription;
import org.eclipse.andmore.internal.editors.layout.configuration.ConfigurationMatcher;
import org.eclipse.andmore.internal.editors.layout.configuration.LayoutCreatorDialog;
import org.eclipse.andmore.internal.editors.layout.descriptors.LayoutDescriptors;
import org.eclipse.andmore.internal.editors.layout.gle2.IncludeFinder.Reference;
import org.eclipse.andmore.internal.editors.layout.gle2.PaletteControl.PalettePage;
import org.eclipse.andmore.internal.editors.layout.gre.RulesEngine;
import org.eclipse.andmore.internal.editors.layout.properties.PropertyFactory;
import org.eclipse.andmore.internal.editors.manifest.ManifestInfo;
import org.eclipse.andmore.internal.editors.uimodel.UiDocumentNode;
import org.eclipse.andmore.internal.editors.uimodel.UiElementNode;
import org.eclipse.andmore.internal.preferences.AdtPrefs;
import org.eclipse.andmore.internal.resources.ResourceHelper;
import org.eclipse.andmore.internal.resources.manager.ProjectResources;
import org.eclipse.andmore.internal.resources.manager.ResourceManager;
import org.eclipse.andmore.internal.sdk.AndroidTargetData;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.andmore.internal.sdk.Sdk.ITargetChangeListener;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.preferences.BuildPathsPropertyPage;
import org.eclipse.jdt.ui.actions.OpenNewClassWizardAction;
import org.eclipse.jdt.ui.wizards.NewClassWizardPage;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.INullSelectionListener;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.part.PageBookView;
import org.eclipse.wb.core.controls.flyout.FlyoutControlComposite;
import org.eclipse.wb.core.controls.flyout.IFlyoutListener;
import org.eclipse.wb.core.controls.flyout.PluginFlyoutPreferences;
import org.eclipse.wb.internal.core.editor.structure.PageSiteComposite;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Graphical layout editor part, version 2.
 * <p/>
 * The main component of the editor part is the {@link LayoutCanvasViewer}, which
 * actually delegates its work to the {@link LayoutCanvas} control.
 * <p/>
 * The {@link LayoutCanvasViewer} is set as the site's {@link ISelectionProvider}:
 * when the selection changes in the canvas, it is thus broadcasted to anyone listening
 * on the site's selection service.
 * <p/>
 * This part is also an {@link ISelectionListener}. It listens to the site's selection
 * service and thus receives selection changes from itself as well as the associated
 * outline and property sheet (these are registered by {@link LayoutEditorDelegate#delegateGetAdapter(Class)}).
 *
 * @since GLE2
 */
public class GraphicalEditorPart extends EditorPart
    implements IPageImageProvider, INullSelectionListener, IFlyoutListener,
            ConfigurationClient {

    /*
     * Useful notes:
     * To understand Drag & drop:
     *   http://www.eclipse.org/articles/Article-Workbench-DND/drag_drop.html
     *
     * To understand the site's selection listener, selection provider, and the
     * confusion of different-yet-similarly-named interfaces, consult this:
     *   http://www.eclipse.org/articles/Article-WorkbenchSelections/article.html
     *
     * To summarize the selection mechanism:
     * - The workbench site selection service can be seen as "centralized"
     *   service that registers selection providers and selection listeners.
     * - The editor part and the outline are selection providers.
     * - The editor part, the outline and the property sheet are listeners
     *   which all listen to each others indirectly.
     */

    /** Property key for the window preferences for the structure flyout */
    private static final String PREF_STRUCTURE = "design.structure";     //$NON-NLS-1$

    /** Property key for the window preferences for the palette flyout */
    private static final String PREF_PALETTE = "design.palette";         //$NON-NLS-1$

    /**
     * Session-property on files which specifies the initial config state to be used on
     * this file
     */
    public final static QualifiedName NAME_INITIAL_STATE =
        new QualifiedName(AndmoreAndroidPlugin.PLUGIN_ID, "initialstate");//$NON-NLS-1$

    /**
     * Session-property on files which specifies the inclusion-context (reference to another layout
     * which should be "including" this layout) when the file is opened
     */
    public final static QualifiedName NAME_INCLUDE =
        new QualifiedName(AndmoreAndroidPlugin.PLUGIN_ID, "includer");//$NON-NLS-1$

    /** Reference to the layout editor */
    private final LayoutEditorDelegate mEditorDelegate;

    /** Reference to the file being edited. Can also be used to access the {@link IProject}. */
    private IFile mEditedFile;

    /** The configuration chooser at the top of the layout editor. */
    private ConfigurationChooser mConfigChooser;

    /** The sash that splits the palette from the error view.
     * The error view is shown only when needed. */
    private SashForm mSashError;

    /** The palette displayed on the left of the sash. */
    private PaletteControl mPalette;

    /** The layout canvas displayed to the right of the sash. */
    private LayoutCanvasViewer mCanvasViewer;

    /** The Rules Engine associated with this editor. It is project-specific. */
    private RulesEngine mRulesEngine;

    /** Styled text displaying the most recent error in the error view. */
    private StyledText mErrorLabel;

    /**
     * The resource reference to a file that should surround this file (e.g. include this file
     * visually), or null if not applicable
     */
    private Reference mIncludedWithin;

    private Map<ResourceType, Map<String, ResourceValue>> mConfiguredFrameworkRes;
    private Map<ResourceType, Map<String, ResourceValue>> mConfiguredProjectRes;
    private ProjectCallback mProjectCallback;
    private boolean mNeedsRecompute = false;
    private TargetListener mTargetListener;
    private ResourceResolver mResourceResolver;
    private ReloadListener mReloadListener;
    private int mMinSdkVersion;
    private int mTargetSdkVersion;
    private LayoutActionBar mActionBar;
    private OutlinePage mOutlinePage;
    private FlyoutControlComposite mStructureFlyout;
    private FlyoutControlComposite mPaletteComposite;
    private PropertyFactory mPropertyFactory;
    private boolean mRenderedOnce;
    private final Object mCredential = new Object();

    /**
     * Flags which tracks whether this editor is currently active which is set whenever
     * {@link #activated()} is called and clear whenever {@link #deactivated()} is called.
     * This is used to suppress repeated calls to {@link #activate()} to avoid doing
     * unnecessary work.
     */
    private boolean mActive;

    /**
     * Constructs a new {@link GraphicalEditorPart}
     *
     * @param editorDelegate the associated XML editor delegate
     */
    public GraphicalEditorPart(@NonNull LayoutEditorDelegate editorDelegate) {
        mEditorDelegate = editorDelegate;
        setPartName("Graphical Layout");
    }

    // ------------------------------------
    // Methods overridden from base classes
    //------------------------------------

    /**
     * Initializes the editor part with a site and input.
     * {@inheritDoc}
     */
    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        setSite(site);
        useNewEditorInput(input);

        if (mTargetListener == null) {
            mTargetListener = new TargetListener();
            AndmoreAndroidPlugin.getDefault().addTargetListener(mTargetListener);

            // Trigger a check to see if the SDK needs to be reloaded (which will
            // invoke onSdkLoaded asynchronously as needed).
            AndmoreAndroidPlugin.getDefault().refreshSdk();
        }
    }

    private void useNewEditorInput(IEditorInput input) throws PartInitException {
        // The contract of init() mentions we need to fail if we can't understand the input.
        if (!(input instanceof FileEditorInput)) {
            throw new PartInitException("Input is not of type FileEditorInput: " +  //$NON-NLS-1$
                    input == null ? "null" : input.toString());                     //$NON-NLS-1$
        }
    }

    @Override
    public Image getPageImage() {
        return IconFactory.getInstance().getIcon("editor_page_design");  //$NON-NLS-1$
    }

    @Override
    public void createPartControl(Composite parent) {

        Display d = parent.getDisplay();

        GridLayout gl = new GridLayout(1, false);
        parent.setLayout(gl);
        gl.marginHeight = gl.marginWidth = 0;

        // Check whether somebody has requested an initial state for the newly opened file.
        // The initial state is a serialized version of the state compatible with
        // {@link ConfigurationComposite#CONFIG_STATE}.
        String initialState = null;
        IFile file = mEditedFile;
        if (file == null) {
            IEditorInput input = mEditorDelegate.getEditor().getEditorInput();
            if (input instanceof FileEditorInput) {
                file = ((FileEditorInput) input).getFile();
            }
        }

        if (file != null) {
            try {
                initialState = (String) file.getSessionProperty(NAME_INITIAL_STATE);
                if (initialState != null) {
                    // Only use once
                    file.setSessionProperty(NAME_INITIAL_STATE, null);
                }
            } catch (CoreException e) {
                AndmoreAndroidPlugin.log(e, "Can't read session property %1$s", NAME_INITIAL_STATE);
            }
        }

        IPreferenceStore preferenceStore = AndmoreAndroidPlugin.getDefault().getPreferenceStore();
        PluginFlyoutPreferences preferences;
        preferences = new PluginFlyoutPreferences(preferenceStore, PREF_PALETTE);
        preferences.initializeDefaults(DOCK_WEST, STATE_OPEN, 200);
        mPaletteComposite = new FlyoutControlComposite(parent, SWT.NONE, preferences);
        mPaletteComposite.setTitleText("Palette");
        mPaletteComposite.setMinWidth(100);
        Composite paletteParent = mPaletteComposite.getFlyoutParent();
        Composite editorParent = mPaletteComposite.getClientParent();
        mPaletteComposite.setListener(this);

        mPaletteComposite.setLayoutData(new GridData(GridData.FILL_BOTH));

        PageSiteComposite paletteComposite = new PageSiteComposite(paletteParent, SWT.BORDER);
        paletteComposite.setTitleText("Palette");
        paletteComposite.setTitleImage(IconFactory.getInstance().getIcon("palette"));
        PalettePage decor = new PalettePage(this);
        paletteComposite.setPage(decor);
        mPalette = (PaletteControl) decor.getControl();
        decor.createToolbarItems(paletteComposite.getToolBar());

        // Create the shared structure+editor area
        preferences = new PluginFlyoutPreferences(preferenceStore, PREF_STRUCTURE);
        preferences.initializeDefaults(DOCK_EAST, STATE_OPEN, 300);
        mStructureFlyout = new FlyoutControlComposite(editorParent, SWT.NONE, preferences);
        mStructureFlyout.setTitleText("Structure");
        mStructureFlyout.setMinWidth(150);
        mStructureFlyout.setListener(this);

        Composite layoutBarAndCanvas = new Composite(mStructureFlyout.getClientParent(), SWT.NONE);
        GridLayout gridLayout = new GridLayout(1, false);
        gridLayout.horizontalSpacing = 0;
        gridLayout.verticalSpacing = 0;
        gridLayout.marginWidth = 0;
        gridLayout.marginHeight = 0;
        layoutBarAndCanvas.setLayout(gridLayout);

        mConfigChooser = new ConfigurationChooser(this, layoutBarAndCanvas, initialState);
        mConfigChooser.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        mActionBar = new LayoutActionBar(layoutBarAndCanvas, SWT.NONE, this);
        GridData detailsData = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
        mActionBar.setLayoutData(detailsData);
        if (file != null) {
            mActionBar.updateErrorIndicator(file);
        }

        mSashError = new SashForm(layoutBarAndCanvas, SWT.VERTICAL | SWT.BORDER);
        mSashError.setLayoutData(new GridData(GridData.FILL_BOTH));

        mCanvasViewer = new LayoutCanvasViewer(mEditorDelegate, mRulesEngine, mSashError, SWT.NONE);
        mSashError.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

        mErrorLabel = new StyledText(mSashError, SWT.READ_ONLY | SWT.WRAP | SWT.V_SCROLL);
        mErrorLabel.setEditable(false);
        mErrorLabel.setBackground(d.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
        mErrorLabel.setForeground(d.getSystemColor(SWT.COLOR_INFO_FOREGROUND));
        mErrorLabel.addMouseListener(new ErrorLabelListener());

        mSashError.setWeights(new int[] { 80, 20 });
        mSashError.setMaximizedControl(mCanvasViewer.getControl());

        // Create the structure views. We really should do this *lazily*, but that
        // seems to cause a bug: property sheet won't update. Track this down later.
        createStructureViews(mStructureFlyout.getFlyoutParent(), false);
        showStructureViews(false, false, false);

        // Initialize the state
        reloadPalette();

        IWorkbenchPartSite site = getSite();
        site.setSelectionProvider(mCanvasViewer);
        site.getPage().addSelectionListener(this);
    }

    private void createStructureViews(Composite parent, boolean createPropertySheet) {
        mOutlinePage = new OutlinePage(this);
        mOutlinePage.setShowPropertySheet(createPropertySheet);
        mOutlinePage.setShowHeader(true);

        IPageSite pageSite = new IPageSite() {

            @Override
            public IWorkbenchPage getPage() {
                return getSite().getPage();
            }

            @Override
            public ISelectionProvider getSelectionProvider() {
                return getSite().getSelectionProvider();
            }

            @Override
            public Shell getShell() {
                return getSite().getShell();
            }

            @Override
            public IWorkbenchWindow getWorkbenchWindow() {
                return getSite().getWorkbenchWindow();
            }

            @Override
            public void setSelectionProvider(ISelectionProvider provider) {
                getSite().setSelectionProvider(provider);
            }

            @Override
            public Object getAdapter(Class adapter) {
                return getSite().getAdapter(adapter);
            }

            @Override
            public Object getService(Class api) {
                return getSite().getService(api);
            }

            @Override
            public boolean hasService(Class api) {
                return getSite().hasService(api);
            }

            @Override
            public void registerContextMenu(String menuId, MenuManager menuManager,
                    ISelectionProvider selectionProvider) {
            }

            @Override
            public IActionBars getActionBars() {
                return null;
            }
        };
        mOutlinePage.init(pageSite);
        mOutlinePage.createControl(parent);
        mOutlinePage.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                getCanvasControl().getSelectionManager().setSelection(event.getSelection());
            }
        });
    }

    /** Shows the embedded (within the layout editor) outline and or properties */
    void showStructureViews(final boolean showOutline, final boolean showProperties,
            final boolean updateLayout) {
        Display display = mConfigChooser.getDisplay();
        if (display.getThread() != Thread.currentThread()) {
            display.asyncExec(new Runnable() {
                @Override
                public void run() {
                    if (!mConfigChooser.isDisposed()) {
                        showStructureViews(showOutline, showProperties, updateLayout);
                    }
                }

            });
            return;
        }

        boolean show = showOutline || showProperties;

        Control[] children = mStructureFlyout.getFlyoutParent().getChildren();
        if (children.length == 0) {
            if (show) {
                createStructureViews(mStructureFlyout.getFlyoutParent(), showProperties);
            }
            return;
        }

        mOutlinePage.setShowPropertySheet(showProperties);

        Control control = children[0];
        if (show != control.getVisible()) {
            control.setVisible(show);
            mOutlinePage.setActive(show); // disable/re-enable listeners etc
            if (show) {
                ISelection selection = getCanvasControl().getSelectionManager().getSelection();
                mOutlinePage.selectionChanged(getEditorDelegate().getEditor(), selection);
            }
            if (updateLayout) {
                mStructureFlyout.layout();
            }
            // TODO: *dispose* the non-showing widgets to save memory?
        }
    }

    /**
     * Returns the property factory associated with this editor
     *
     * @return the factory
     */
    @NonNull
    public PropertyFactory getPropertyFactory() {
        if (mPropertyFactory == null) {
            mPropertyFactory = new PropertyFactory(this);
        }

        return mPropertyFactory;
    }

    /**
     * Invoked by {@link LayoutCanvas} to set the model (a.k.a. the root view info).
     *
     * @param rootViewInfo The root of the view info hierarchy. Can be null.
     */
    public void setModel(CanvasViewInfo rootViewInfo) {
        if (mOutlinePage != null) {
            mOutlinePage.setModel(rootViewInfo);
        }
    }

    /**
     * Listens to workbench selections that does NOT come from {@link LayoutEditorDelegate}
     * (those are generated by ourselves).
     * <p/>
     * Selection can be null, as indicated by this class implementing
     * {@link INullSelectionListener}.
     */
    @Override
    public void selectionChanged(IWorkbenchPart part, ISelection selection) {
        Object delegate = part instanceof IEditorPart ?
                LayoutEditorDelegate.fromEditor((IEditorPart) part) : null;
        if (delegate == null) {
            if (part instanceof PageBookView) {
                PageBookView pbv = (PageBookView) part;
                 org.eclipse.ui.part.IPage currentPage = pbv.getCurrentPage();
                if (currentPage instanceof OutlinePage) {
                    LayoutCanvas canvas = getCanvasControl();
                    if (canvas != null && canvas.getOutlinePage() != currentPage) {
                        // The notification is not for this view; ignore
                        // (can happen when there are multiple pages simultaneously
                        // visible)
                        return;
                    }
                }
            }
            mCanvasViewer.setSelection(selection);
        }
    }

    @Override
    public void dispose() {
        getSite().getPage().removeSelectionListener(this);
        getSite().setSelectionProvider(null);

        if (mTargetListener != null) {
            AndmoreAndroidPlugin.getDefault().removeTargetListener(mTargetListener);
            mTargetListener = null;
        }

        if (mReloadListener != null) {
            LayoutReloadMonitor.getMonitor().removeListener(mReloadListener);
            mReloadListener = null;
        }

        if (mCanvasViewer != null) {
            mCanvasViewer.dispose();
            mCanvasViewer = null;
        }
        super.dispose();
    }

    /**
     * Select the visual element corresponding to the given XML node
     * @param xmlNode The Node whose element we want to select
     */
    public void select(Node xmlNode) {
        mCanvasViewer.getCanvas().getSelectionManager().select(xmlNode);
    }

    // ---- Implements ConfigurationClient ----
    @Override
    public void aboutToChange(int flags) {
        if ((flags & CFG_TARGET) != 0) {
            IAndroidTarget oldTarget = mConfigChooser.getConfiguration().getTarget();
            preRenderingTargetChangeCleanUp(oldTarget);
        }
    }

    @Override
    public boolean changed(int flags) {
        mConfiguredFrameworkRes = mConfiguredProjectRes = null;
        mResourceResolver = null;

        if (mEditedFile == null) {
            return true;
        }

        // Before doing the normal process, test for the following case.
        // - the editor is being opened (or reset for a new input)
        // - the file being opened is not the best match for any possible configuration
        // - another random compatible config was chosen in the config composite.
        // The result is that 'match' will not be the file being edited, but because this is not
        // due to a config change, we should not trigger opening the actual best match (also,
        // because the editor is still opening the MatchingStrategy woudln't answer true
        // and the best match file would open in a different editor).
        // So the solution is that if the editor is being created, we just call recomputeLayout
        // without looking for a better matching layout file.
        if (mEditorDelegate.getEditor().isCreatingPages()) {
            recomputeLayout();
        } else {
            boolean affectsFileSelection = (flags & Configuration.MASK_FILE_ATTRS) != 0;
            IFile best = null;
            // get the resources of the file's project.
            if (affectsFileSelection) {
                best = ConfigurationMatcher.getBestFileMatch(mConfigChooser);
            }
            if (best != null) {
                if (!best.equals(mEditedFile)) {
                    try {
                        // tell the editor that the next replacement file is due to a config
                        // change.
                        mEditorDelegate.setNewFileOnConfigChange(true);

                        boolean reuseEditor = AdtPrefs.getPrefs().isSharedLayoutEditor();
                        if (!reuseEditor) {
                            String data = ConfigurationDescription.getDescription(best);
                            if (data == null) {
                                // Not previously opened: duplicate the current state as
                                // much as possible
                                data = mConfigChooser.getConfiguration().toPersistentString();
                                ConfigurationDescription.setDescription(best, data);
                            }
                        }

                        // ask the IDE to open the replacement file.
                        IDE.openEditor(getSite().getWorkbenchWindow().getActivePage(), best,
                                CommonXmlEditor.ID);

                        // we're done!
                        return reuseEditor;
                    } catch (PartInitException e) {
                        // FIXME: do something!
                    }
                }

                // at this point, we have not opened a new file.

                // Store the state in the current file
                mConfigChooser.saveConstraints();

                // Even though the layout doesn't change, the config changed, and referenced
                // resources need to be updated.
                recomputeLayout();
            } else if (affectsFileSelection) {
                // display the error.
                Configuration configuration = mConfigChooser.getConfiguration();
                FolderConfiguration currentConfig = configuration.getFullConfig();
                displayError(
                        "No resources match the configuration\n" +
                        " \n" +
                        "\t%1$s\n" +
                        " \n" +
                        "Change the configuration or create:\n" +
                        " \n" +
                        "\tres/%2$s/%3$s\n" +
                        " \n" +
                        "You can also click the 'Create New...' item in the configuration " +
                        "dropdown menu above.",
                        currentConfig.toDisplayString(),
                        currentConfig.getFolderName(ResourceFolderType.LAYOUT),
                        mEditedFile.getName());
            } else {
                // Something else changed, such as the theme - just recompute existing
                // layout
                mConfigChooser.saveConstraints();
                recomputeLayout();
            }
        }

        if ((flags & CFG_TARGET) != 0) {
            Configuration configuration = mConfigChooser.getConfiguration();
            IAndroidTarget target = configuration.getTarget();
            Sdk current = Sdk.getCurrent();
            if (current != null) {
                AndroidTargetData targetData = current.getTargetData(target);
                updateCapabilities(targetData);
            }
        }

        if ((flags & (CFG_DEVICE | CFG_DEVICE_STATE)) != 0) {
            // When the device changes, zoom the view to fit, but only up to 100% (e.g. zoom
            // out to fit the content, or zoom back in if we were zoomed out more from the
            // previous view, but only up to 100% such that we never blow up pixels
            if (mActionBar.isZoomingAllowed()) {
                getCanvasControl().setFitScale(true,  true /*allowZoomIn*/);
            }
        }

        reloadPalette();

        getCanvasControl().getPreviewManager().configurationChanged(flags);

        return true;
    }

    @Override
    public void setActivity(@NonNull String activity) {
        ManifestInfo manifest = ManifestInfo.get(mEditedFile.getProject());
        String pkg = manifest.getPackage();
        if (activity.startsWith(pkg) && activity.length() > pkg.length()
                && activity.charAt(pkg.length()) == '.') {
            activity = activity.substring(pkg.length());
        }
        CommonXmlEditor editor = getEditorDelegate().getEditor();
        Element element = editor.getUiRootNode().getXmlDocument().getDocumentElement();
        AdtUtils.setToolsAttribute(editor,
                element, "Choose Activity", ATTR_CONTEXT,
                activity, false /*reveal*/, false /*append*/);
    }

    /**
     * Returns a {@link ProjectResources} for the framework resources based on the current
     * configuration selection.
     * @return the framework resources or null if not found.
     */
    @Override
    @Nullable
    public ResourceRepository getFrameworkResources() {
        return getFrameworkResources(getRenderingTarget());
    }

    /**
     * Returns a {@link ProjectResources} for the framework resources of a given
     * target.
     * @param target the target for which to return the framework resources.
     * @return the framework resources or null if not found.
     */
    @Override
    @Nullable
    public ResourceRepository getFrameworkResources(@Nullable IAndroidTarget target) {
        if (target != null) {
            AndroidTargetData data = Sdk.getCurrent().getTargetData(target);

            if (data != null) {
                return data.getFrameworkResources();
            }
        }

        return null;
    }

    @Override
    @Nullable
    public ProjectResources getProjectResources() {
        if (mEditedFile != null) {
            ResourceManager manager = ResourceManager.getInstance();
            return manager.getProjectResources(mEditedFile.getProject());
        }

        return null;
    }


    @Override
    @NonNull
    public Map<ResourceType, Map<String, ResourceValue>> getConfiguredFrameworkResources() {
        if (mConfiguredFrameworkRes == null && mConfigChooser != null) {
            ResourceRepository frameworkRes = getFrameworkResources();

            if (frameworkRes == null) {
                AndmoreAndroidPlugin.log(IStatus.ERROR, "Failed to get ProjectResource for the framework");
            } else {
                // get the framework resource values based on the current config
                mConfiguredFrameworkRes = frameworkRes.getConfiguredResources(
                        mConfigChooser.getConfiguration().getFullConfig());
            }
        }

        return mConfiguredFrameworkRes;
    }

    @Override
    @NonNull
    public Map<ResourceType, Map<String, ResourceValue>> getConfiguredProjectResources() {
        if (mConfiguredProjectRes == null && mConfigChooser != null) {
            ProjectResources project = getProjectResources();

            // get the project resource values based on the current config
            mConfiguredProjectRes = project.getConfiguredResources(
                    mConfigChooser.getConfiguration().getFullConfig());
        }

        return mConfiguredProjectRes;
    }

    @Override
    public void createConfigFile() {
        LayoutCreatorDialog dialog = new LayoutCreatorDialog(mConfigChooser.getShell(),
                mEditedFile.getName(), mConfigChooser.getConfiguration().getFullConfig());
        if (dialog.open() != Window.OK) {
            return;
        }

        FolderConfiguration config = new FolderConfiguration();
        dialog.getConfiguration(config);

        // Creates a new layout file from the specified {@link FolderConfiguration}.
        CreateNewConfigJob job = new CreateNewConfigJob(this, mEditedFile, config);
        job.schedule();
    }

    /**
     * Returns the resource name of the file that is including this current layout, if any
     * (may be null)
     *
     * @return the resource name of an including layout, or null
     */
    @Override
    public Reference getIncludedWithin() {
        return mIncludedWithin;
    }

    @Override
    @Nullable
    public LayoutCanvas getCanvas() {
        return getCanvasControl();
    }

    /**
     * Listens to target changed in the current project, to trigger a new layout rendering.
     */
    private class TargetListener implements ITargetChangeListener {

        @Override
        public void onProjectTargetChange(IProject changedProject) {
            if (changedProject != null && changedProject.equals(getProject())) {
                updateEditor();
            }
        }

        @Override
        public void onTargetLoaded(IAndroidTarget loadedTarget) {
            IAndroidTarget target = getRenderingTarget();
            if (target != null && target.equals(loadedTarget)) {
                updateEditor();
            }
        }

        @Override
        public void onSdkLoaded() {
            // get the current rendering target to unload it
            IAndroidTarget oldTarget = getRenderingTarget();
            preRenderingTargetChangeCleanUp(oldTarget);

            computeSdkVersion();

            // get the project target
            Sdk currentSdk = Sdk.getCurrent();
            if (currentSdk != null) {
                IAndroidTarget target = currentSdk.getTarget(mEditedFile.getProject());
                if (target != null) {
                    mConfigChooser.onSdkLoaded(target);
                    changed(CFG_FOLDER | CFG_TARGET);
                }
            }
        }

        private void updateEditor() {
            mEditorDelegate.getEditor().commitPages(false /* onSave */);

            // because the target changed we must reset the configured resources.
            mConfiguredFrameworkRes = mConfiguredProjectRes = null;
            mResourceResolver = null;

            // make sure we remove the custom view loader, since its parent class loader is the
            // bridge class loader.
            mProjectCallback = null;

            // recreate the ui root node always, this will also call onTargetChange
            // on the config composite
            mEditorDelegate.delegateInitUiRootNode(true /*force*/);
        }

        private IProject getProject() {
            return getEditorDelegate().getEditor().getProject();
        }
    }

    /** Refresh the configured project resources associated with this editor */
    public void refreshProjectResources() {
        mConfiguredProjectRes = null;
        mResourceResolver = null;
    }

    /**
     * Returns the currently edited file
     *
     * @return the currently edited file, or null
     */
    public IFile getEditedFile() {
        return mEditedFile;
    }

    /**
     * Returns the project for the currently edited file, or null
     *
     * @return the project containing the edited file, or null
     */
    public IProject getProject() {
        if (mEditedFile != null) {
            return mEditedFile.getProject();
        } else {
            return null;
        }
    }

    // ----------------

    /**
     * Save operation in the Graphical Editor Part.
     * <p/>
     * In our workflow, the model is owned by the Structured XML Editor.
     * The graphical layout editor just displays it -- thus we don't really
     * save anything here.
     * <p/>
     * This must NOT call the parent editor part. At the contrary, the parent editor
     * part will call this *after* having done the actual save operation.
     * <p/>
     * The only action this editor must do is mark the undo command stack as
     * being no longer dirty.
     */
    @Override
    public void doSave(IProgressMonitor monitor) {
        // TODO implement a command stack
//        getCommandStack().markSaveLocation();
//        firePropertyChange(PROP_DIRTY);
    }

    /**
     * Save operation in the Graphical Editor Part.
     * <p/>
     * In our workflow, the model is owned by the Structured XML Editor.
     * The graphical layout editor just displays it -- thus we don't really
     * save anything here.
     */
    @Override
    public void doSaveAs() {
        // pass
    }

    /**
     * In our workflow, the model is owned by the Structured XML Editor.
     * The graphical layout editor just displays it -- thus we don't really
     * save anything here.
     */
    @Override
    public boolean isDirty() {
        return false;
    }

    /**
     * In our workflow, the model is owned by the Structured XML Editor.
     * The graphical layout editor just displays it -- thus we don't really
     * save anything here.
     */
    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    @Override
    public void setFocus() {
        // TODO Auto-generated method stub

    }

    /**
     * Responds to a page change that made the Graphical editor page the activated page.
     */
    public void activated() {
        if (!mActive) {
            mActive = true;

            syncDockingState();
            mActionBar.updateErrorIndicator();

            boolean changed = mConfigChooser.syncRenderState();
            if (changed) {
                // Will also force recomputeLayout()
                return;
            }

            if (mNeedsRecompute) {
                recomputeLayout();
            }

            mCanvasViewer.getCanvas().syncPreviewMode();
        }
    }

    /**
     * The global docking state version. This number is incremented each time
     * the user customizes the window layout in any layout.
     */
    private static int sDockingStateVersion;

    /**
     * The window docking state version that this window is currently showing;
     * when a different window is reconfigured, the global version number is
     * incremented, and when this window is shown, and the current version is
     * less than the global version, the window layout will be synced.
     */
    private int mDockingStateVersion;

    /**
     * Syncs the window docking state.
     * <p>
     * The layout editor lets you change the docking state -- e.g. you can minimize the
     * palette, and drag the structure view to the bottom, and so on. When you restart
     * the IDE, the window comes back up with your customized state.
     * <p>
     * <b>However</b>, when you have multiple editor files open, if you minimize the palette
     * in one editor and then switch to another, the other editor will have the old window
     * state. That's because each editor has its own set of windows.
     * <p>
     * This method fixes this. Whenever a window is shown, this method is called, and the
     * docking state is synced such that the editor will match the current persistent docking
     * state.
     */
    private void syncDockingState() {
        if (mDockingStateVersion == sDockingStateVersion) {
            // No changes to apply
            return;
        }
        mDockingStateVersion = sDockingStateVersion;

        IPreferenceStore preferenceStore = AndmoreAndroidPlugin.getDefault().getPreferenceStore();
        PluginFlyoutPreferences preferences;
        preferences = new PluginFlyoutPreferences(preferenceStore, PREF_PALETTE);
        mPaletteComposite.apply(preferences);
        preferences = new PluginFlyoutPreferences(preferenceStore, PREF_STRUCTURE);
        mStructureFlyout.apply(preferences);
        mPaletteComposite.layout();
        mStructureFlyout.layout();
        mPaletteComposite.redraw(); // the structure view is nested within the palette
    }

    /**
     * Responds to a page change that made the Graphical editor page the deactivated page
     */
    public void deactivated() {
        mActive = false;

        LayoutCanvas canvas = getCanvasControl();
        if (canvas != null) {
            canvas.deactivated();
        }
    }

    /**
     * Opens and initialize the editor with a new file.
     * @param file the file being edited.
     */
    public void openFile(IFile file) {
        mEditedFile = file;
        mConfigChooser.setFile(mEditedFile);

        if (mReloadListener == null) {
            mReloadListener = new ReloadListener();
            LayoutReloadMonitor.getMonitor().addListener(mEditedFile.getProject(), mReloadListener);
        }

        if (mRulesEngine == null) {
            mRulesEngine = new RulesEngine(this, mEditedFile.getProject());
            if (mCanvasViewer != null) {
                mCanvasViewer.getCanvas().setRulesEngine(mRulesEngine);
            }
        }

        // Pick up hand-off data: somebody requesting this file to be opened may have
        // requested that it should be opened as included within another file
        if (mEditedFile != null) {
            try {
                mIncludedWithin = (Reference) mEditedFile.getSessionProperty(NAME_INCLUDE);
                if (mIncludedWithin != null) {
                    // Only use once
                    mEditedFile.setSessionProperty(NAME_INCLUDE, null);
                }
            } catch (CoreException e) {
                AndmoreAndroidPlugin.log(e, "Can't access session property %1$s", NAME_INCLUDE);
            }
        }

        computeSdkVersion();
    }

    /**
     * Resets the editor with a replacement file.
     * @param file the replacement file.
     */
    public void replaceFile(IFile file) {
        mEditedFile = file;
        mConfigChooser.replaceFile(mEditedFile);
        computeSdkVersion();
    }

    /**
     * Resets the editor with a replacement file coming from a config change in the config
     * selector.
     * @param file the replacement file.
     */
    public void changeFileOnNewConfig(IFile file) {
        mEditedFile = file;
        mConfigChooser.changeFileOnNewConfig(mEditedFile);
    }

    /**
     * Responds to a target change for the project of the edited file
     */
    public void onTargetChange() {
        AndroidTargetData targetData = mConfigChooser.onXmlModelLoaded();
        updateCapabilities(targetData);

        changed(CFG_FOLDER | CFG_TARGET);
    }

    /** Updates the capabilities for the given target data (which may be null) */
    private void updateCapabilities(AndroidTargetData targetData) {
        if (targetData != null) {
            LayoutLibrary layoutLib = targetData.getLayoutLibrary();
            if (mIncludedWithin != null && !layoutLib.supports(Capability.EMBEDDED_LAYOUT)) {
                showIn(null);
            }
        }
    }

    /**
     * Returns the {@link CommonXmlDelegate} for this editor
     *
     * @return the {@link CommonXmlDelegate} for this editor
     */
    @NonNull
    public LayoutEditorDelegate getEditorDelegate() {
        return mEditorDelegate;
    }

    /**
     * Returns the {@link RulesEngine} associated with this editor
     *
     * @return the {@link RulesEngine} associated with this editor, never null
     */
    public RulesEngine getRulesEngine() {
        return mRulesEngine;
    }

    /**
     * Return the {@link LayoutCanvas} associated with this editor
     *
     * @return the associated {@link LayoutCanvas}
     */
    public LayoutCanvas getCanvasControl() {
        if (mCanvasViewer != null) {
            return mCanvasViewer.getCanvas();
        }
        return null;
    }

    /**
     * Returns the {@link UiDocumentNode} for the XML model edited by this editor
     *
     * @return the associated model
     */
    public UiDocumentNode getModel() {
        return mEditorDelegate.getUiRootNode();
    }

    /**
     * Callback for XML model changed. Only update/recompute the layout if the editor is visible
     */
    public void onXmlModelChanged() {
        // To optimize the rendering when the user is editing in the XML pane, we don't
        // refresh the editor if it's not the active part.
        //
        // This behavior is acceptable when the editor is the single "full screen" part
        // (as in this case active means visible.)
        // Unfortunately this breaks in 2 cases:
        // - when performing a drag'n'drop from one editor to another, the target is not
        //   properly refreshed before it becomes active.
        // - when duplicating the editor window and placing both editors side by side (xml in one
        //   and canvas in the other one), the canvas may not be refreshed when the XML is edited.
        //
        // TODO find a way to really query whether the pane is visible, not just active.

        if (mEditorDelegate.isGraphicalEditorActive()) {
            recomputeLayout();
        } else {
            // Remember we want to recompute as soon as the editor becomes active.
            mNeedsRecompute = true;
        }
    }

    /**
     * Recomputes the layout
     */
    public void recomputeLayout() {
        try {
            if (!ensureFileValid()) {
                return;
            }

            UiDocumentNode model = getModel();
            LayoutCanvas canvas = mCanvasViewer.getCanvas();
            if (!ensureModelValid(model)) {
                // Although we display an error, we still treat an empty document as a
                // successful layout result so that we can drop new elements in it.
                //
                // For that purpose, create a special LayoutScene that has no image,
                // no root view yet indicates success and then update the canvas with it.

                canvas.setSession(
                        new StaticRenderSession(
                                Result.Status.SUCCESS.createResult(),
                                null /*rootViewInfo*/, null /*image*/),
                        null /*explodeNodes*/, true /* layoutlib5 */);
                return;
            }

            LayoutLibrary layoutLib = getReadyLayoutLib(true /*displayError*/);

            if (layoutLib != null) {
                // if drawing in real size, (re)set the scaling factor.
                if (mActionBar.isZoomingRealSize()) {
                    mActionBar.computeAndSetRealScale(false /* redraw */);
                }

                IProject project = mEditedFile.getProject();
                renderWithBridge(project, model, layoutLib);

                canvas.getPreviewManager().renderPreviews();
            }
        } finally {
            // no matter the result, we are done doing the recompute based on the latest
            // resource/code change.
            mNeedsRecompute = false;
        }
    }

    /**
     * Reloads the palette
     */
    public void reloadPalette() {
        if (mPalette != null) {
            IAndroidTarget renderingTarget = getRenderingTarget();
            if (renderingTarget != null) {
                mPalette.reloadPalette(renderingTarget);
            }
        }
    }

    /**
     * Returns the {@link LayoutLibrary} associated with this editor, if it has
     * been initialized already. May return null if it has not been initialized (or has
     * not finished initializing).
     *
     * @return The {@link LayoutLibrary}, or null
     */
    public LayoutLibrary getLayoutLibrary() {
        return getReadyLayoutLib(false /*displayError*/);
    }

    /**
     * Returns the scale to multiply pixels in the layout coordinate space with to obtain
     * the corresponding dip (device independent pixel)
     *
     * @return the scale to multiple layout coordinates with to obtain the dip position
     */
    public float getDipScale() {
        float dpi = mConfigChooser.getConfiguration().getDensity().getDpiValue();
        return Density.DEFAULT_DENSITY / dpi;
    }

    // --- private methods ---

    /**
     * Ensure that the file associated with this editor is valid (exists and is
     * synchronized). Any reasons why it is not are displayed in the editor's error area.
     *
     * @return True if the editor is valid, false otherwise.
     */
    private boolean ensureFileValid() {
        // check that the resource exists. If the file is opened but the project is closed
        // or deleted for some reason (changed from outside of eclipse), then this will
        // return false;
        if (mEditedFile.exists() == false) {
            displayError("Resource '%1$s' does not exist.",
                         mEditedFile.getFullPath().toString());
            return false;
        }

        if (mEditedFile.isSynchronized(IResource.DEPTH_ZERO) == false) {
            String message = String.format("%1$s is out of sync. Please refresh.",
                    mEditedFile.getName());

            displayError(message);

            // also print it in the error console.
            IProject iProject = mEditedFile.getProject();
            AndmoreAndroidPlugin.printErrorToConsole(iProject.getName(), message);
            return false;
        }

        return true;
    }

    /**
     * Returns a {@link LayoutLibrary} that is ready for rendering, or null if the bridge
     * is not available or not ready yet (due to SDK loading still being in progress etc).
     * If enabled, any reasons preventing the bridge from being returned are displayed to the
     * editor's error area.
     *
     * @param displayError whether to display the loading error or not.
     *
     * @return LayoutBridge the layout bridge for rendering this editor's scene
     */
    LayoutLibrary getReadyLayoutLib(boolean displayError) {
        Sdk currentSdk = Sdk.getCurrent();
        if (currentSdk != null) {
            IAndroidTarget target = getRenderingTarget();

            if (target != null) {
                AndroidTargetData data = currentSdk.getTargetData(target);
                if (data != null) {
                    LayoutLibrary layoutLib = data.getLayoutLibrary();

                    if (layoutLib.getStatus() == LoadStatus.LOADED) {
                        return layoutLib;
                    } else if (displayError) { // getBridge() == null
                        // SDK is loaded but not the layout library!

                        // check whether the bridge managed to load, or not
                        if (layoutLib.getStatus() == LoadStatus.LOADING) {
                            displayError("Eclipse is loading framework information and the layout library from the SDK folder.\n%1$s will refresh automatically once the process is finished.",
                                         mEditedFile.getName());
                        } else {
                            String message = layoutLib.getLoadMessage();
                            displayError("Eclipse failed to load the framework information and the layout library!" +
                                    message != null ? "\n" + message : "");
                        }
                    }
                } else { // data == null
                    // It can happen that the workspace refreshes while the SDK is loading its
                    // data, which could trigger a redraw of the opened layout if some resources
                    // changed while Eclipse is closed.
                    // In this case data could be null, but this is not an error.
                    // We can just silently return, as all the opened editors are automatically
                    // refreshed once the SDK finishes loading.
                    LoadStatus targetLoadStatus = currentSdk.checkAndLoadTargetData(target, null);

                    // display error is asked.
                    if (displayError) {
                        String targetName = target.getName();
                        switch (targetLoadStatus) {
                            case LOADING:
                                String s;
                                if (currentSdk.getTarget(getProject()) == target) {
                                    s = String.format(
                                            "The project target (%1$s) is still loading.",
                                            targetName);
                                } else {
                                    s = String.format(
                                            "The rendering target (%1$s) is still loading.",
                                            targetName);
                                }
                                s += "\nThe layout will refresh automatically once the process is finished.";
                                displayError(s);

                                break;
                            case FAILED: // known failure
                            case LOADED: // success but data isn't loaded?!?!
                                displayError("The project target (%s) was not properly loaded.",
                                        targetName);
                                break;
                        }
                    }
                }

            } else if (displayError) { // target == null
                displayError("The project target is not set. Right click project, choose Properties | Android.");
            }
        } else if (displayError) { // currentSdk == null
            displayError("Eclipse is loading the SDK.\n%1$s will refresh automatically once the process is finished.",
                         mEditedFile.getName());
        }

        return null;
    }

    /**
     * Returns the {@link IAndroidTarget} used for the rendering.
     * <p/>
     * This first looks for the rendering target setup in the config UI, and if nothing has
     * been setup yet, returns the target of the project.
     *
     * @return an IAndroidTarget object or null if no target is setup and the project has no
     * target set.
     *
     */
    public IAndroidTarget getRenderingTarget() {
        // if the SDK is null no targets are loaded.
        Sdk currentSdk = Sdk.getCurrent();
        if (currentSdk == null) {
            return null;
        }

        // attempt to get a target from the configuration selector.
        IAndroidTarget renderingTarget = mConfigChooser.getConfiguration().getTarget();
        if (renderingTarget != null) {
            return renderingTarget;
        }

        // fall back to the project target
        if (mEditedFile != null) {
            return currentSdk.getTarget(mEditedFile.getProject());
        }

        return null;
    }

    /**
     * Returns whether the current rendering target supports the given capability
     *
     * @param capability the capability to be looked up
     * @return true if the current rendering target supports the given capability
     */
    public boolean renderingSupports(Capability capability) {
        IAndroidTarget target = getRenderingTarget();
        if (target != null) {
            AndroidTargetData targetData = Sdk.getCurrent().getTargetData(target);
            LayoutLibrary layoutLib = targetData.getLayoutLibrary();
            return layoutLib.supports(capability);
        }

        return false;
    }

    private boolean ensureModelValid(UiDocumentNode model) {
        // check there is actually a model (maybe the file is empty).
        if (model.getUiChildren().size() == 0) {
            if (mEditorDelegate.getEditor().isCreatingPages()) {
                displayError("Loading editor");
                return false;
            }
            displayError(
                    "No XML content. Please add a root view or layout to your document.");
            return false;
        }

        return true;
    }

    /**
     * Creates a {@link RenderService} associated with this editor
     * @return the render service
     */
    @NonNull
    public RenderService createRenderService() {
        return RenderService.create(this, mCredential);
    }

    /**
     * Creates a {@link RenderLogger} associated with this editor
     * @param name the name of the logger
     * @return the new logger
     */
    @NonNull
    public RenderLogger createRenderLogger(String name) {
        return new RenderLogger(name, mCredential);
    }

    /**
     * Creates a {@link RenderService} associated with this editor
     *
     * @param configuration the configuration to use (and fallback to editor for the rest)
     * @param resolver a resource resolver to use to look up resources
     * @return the render service
     */
    @NonNull
    public RenderService createRenderService(Configuration configuration,
            ResourceResolver resolver) {
        return RenderService.create(this, configuration, resolver, mCredential);
    }

    private void renderWithBridge(IProject iProject, UiDocumentNode model,
            LayoutLibrary layoutLib) {
        LayoutCanvas canvas = getCanvasControl();
        Set<UiElementNode> explodeNodes = canvas.getNodesToExplode();
        RenderLogger logger = createRenderLogger(mEditedFile.getName());
        RenderingMode renderingMode = RenderingMode.NORMAL;
        // FIXME set the rendering mode using ViewRule or something.
        List<UiElementNode> children = model.getUiChildren();
        if (children.size() > 0 &&
                children.get(0).getDescriptor().getXmlLocalName().equals(SCROLL_VIEW)) {
            renderingMode = RenderingMode.V_SCROLL;
        }

        RenderSession session = RenderService.create(this, mCredential)
            .setModel(model)
            .setLog(logger)
            .setRenderingMode(renderingMode)
            .setIncludedWithin(mIncludedWithin)
            .setNodesToExpand(explodeNodes)
            .createRenderSession();

        boolean layoutlib5 = layoutLib.supports(Capability.EMBEDDED_LAYOUT);
        canvas.setSession(session, explodeNodes, layoutlib5);

        // update the UiElementNode with the layout info.
        if (session != null && session.getResult().isSuccess() == false) {
            // An error was generated. Print it (and any other accumulated warnings)
            String errorMessage = session.getResult().getErrorMessage();
            Throwable exception = session.getResult().getException();
            if (exception != null && errorMessage == null) {
                errorMessage = exception.toString();
            }
            if (exception != null || (errorMessage != null && errorMessage.length() > 0)) {
                logger.error(null, errorMessage, exception, null /*data*/);
            } else if (!logger.hasProblems()) {
                logger.error(null, "Unexpected error in rendering, no details given",
                        null /*data*/);
            }
            // These errors will be included in the log warnings which are
            // displayed regardless of render success status below
        }

        // We might have detected some missing classes and swapped them by a mock view,
        // or run into fidelity warnings or missing resources, so emit all these
        // warnings
        Set<String> missingClasses = mProjectCallback.getMissingClasses();
        Set<String> brokenClasses = mProjectCallback.getUninstantiatableClasses();
        if (logger.hasProblems()) {
            displayLoggerProblems(iProject, logger);
            displayFailingClasses(missingClasses, brokenClasses, true);
            displayUserStackTrace(logger, true);
        } else if (missingClasses.size() > 0 || brokenClasses.size() > 0) {
            displayFailingClasses(missingClasses, brokenClasses, false);
            displayUserStackTrace(logger, true);
        } else if (session != null) {
            // Nope, no missing or broken classes. Clear success, congrats!
            hideError();

            // First time this layout is opened, run lint on the file (after a delay)
            if (!mRenderedOnce) {
                mRenderedOnce = true;
                Job job = new Job("Run Lint") {
                    @Override
                    protected IStatus run(IProgressMonitor monitor) {
                        getEditorDelegate().delegateRunLint();
                        return Status.OK_STATUS;
                    }

                };
                job.setSystem(true);
                job.schedule(3000); // 3 seconds
            }

            mConfigChooser.ensureInitialized();
        }

        model.refreshUi();
    }

    /**
     * Returns the {@link ResourceResolver} for this editor
     *
     * @return the resolver used to resolve resources for the current configuration of
     *         this editor, or null
     */
    public ResourceResolver getResourceResolver() {
        if (mResourceResolver == null) {
            String theme = mConfigChooser.getThemeName();
            if (theme == null) {
                displayError("Missing theme.");
                return null;
            }
            boolean isProjectTheme = mConfigChooser.getConfiguration().isProjectTheme();

            Map<ResourceType, Map<String, ResourceValue>> configuredProjectRes =
                getConfiguredProjectResources();

            // Get the framework resources
            Map<ResourceType, Map<String, ResourceValue>> frameworkResources =
                getConfiguredFrameworkResources();

            if (configuredProjectRes == null) {
                displayError("Missing project resources for current configuration.");
                return null;
            }

            if (frameworkResources == null) {
                displayError("Missing framework resources.");
                return null;
            }

            mResourceResolver = ResourceResolver.create(
                    configuredProjectRes, frameworkResources,
                    theme, isProjectTheme);
        }

        return mResourceResolver;
    }

    /** Returns a project callback, and optionally resets it */
    ProjectCallback getProjectCallback(boolean reset, LayoutLibrary layoutLibrary) {
        // Lazily create the project callback the first time we need it
        if (mProjectCallback == null) {
            ResourceManager resManager = ResourceManager.getInstance();
            IProject project = getProject();
            ProjectResources projectRes = resManager.getProjectResources(project);
            mProjectCallback = new ProjectCallback(layoutLibrary, projectRes, project,
                    mCredential, this);
        } else if (reset) {
            // Also clears the set of missing/broken classes prior to rendering
            mProjectCallback.getMissingClasses().clear();
            mProjectCallback.getUninstantiatableClasses().clear();
        }

        return mProjectCallback;
    }

    /**
     * Returns the resource name of this layout, NOT including the @layout/ prefix
     *
     * @return the resource name of this layout, NOT including the @layout/ prefix
     */
    public String getLayoutResourceName() {
        return ResourceHelper.getLayoutName(mEditedFile);
    }

    /**
     * Cleans up when the rendering target is about to change
     * @param oldTarget the old rendering target.
     */
    private void preRenderingTargetChangeCleanUp(IAndroidTarget oldTarget) {
        // first clear the caches related to this file in the old target
        Sdk currentSdk = Sdk.getCurrent();
        if (currentSdk != null) {
            AndroidTargetData data = currentSdk.getTargetData(oldTarget);
            if (data != null) {
                LayoutLibrary layoutLib = data.getLayoutLibrary();

                // layoutLib can never be null.
                layoutLib.clearCaches(mEditedFile.getProject());
            }
        }

        // Also remove the ProjectCallback as it caches custom views which must be reloaded
        // with the classloader of the new LayoutLib. We also have to clear it out
        // because it stores a reference to the layout library which could have changed.
        mProjectCallback = null;

        // FIXME: get rid of the current LayoutScene if any.
    }

    private class ReloadListener implements ILayoutReloadListener {
        /**
         * Called when the file changes triggered a redraw of the layout
         */
        @Override
        public void reloadLayout(final ChangeFlags flags, final boolean libraryChanged) {
            if (mConfigChooser.isDisposed()) {
                return;
            }
            Display display = mConfigChooser.getDisplay();
            display.asyncExec(new Runnable() {
                @Override
                public void run() {
                    reloadLayoutSwt(flags, libraryChanged);
                }
            });
        }

        /** Reload layout. <b>Must be called on the SWT thread</b> */
        private void reloadLayoutSwt(ChangeFlags flags, boolean libraryChanged) {
            if (mConfigChooser.isDisposed()) {
                return;
            }
            assert mConfigChooser.getDisplay().getThread() == Thread.currentThread();

            boolean recompute = false;
            // we only care about the r class of the main project.
            if (flags.rClass && libraryChanged == false) {
                recompute = true;
                if (mEditedFile != null) {
                    ResourceManager manager = ResourceManager.getInstance();
                    ProjectResources projectRes = manager.getProjectResources(
                            mEditedFile.getProject());

                    if (projectRes != null) {
                        projectRes.resetDynamicIds();
                    }
                }
            }

            if (flags.localeList) {
                // the locale list *potentially* changed so we update the locale in the
                // config composite.
                // However there's no recompute, as it could not be needed
                // (for instance a new layout)
                // If a resource that's not a layout changed this will trigger a recompute anyway.
                mConfigChooser.updateLocales();
            }

            // if a resources was modified.
            if (flags.resources) {
                recompute = true;

                // TODO: differentiate between single and multi resource file changed, and whether
                // the resource change affects the cache.

                // force a reparse in case a value XML file changed.
                mConfiguredProjectRes = null;
                mResourceResolver = null;

                // clear the cache in the bridge in case a bitmap/9-patch changed.
                LayoutLibrary layoutLib = getReadyLayoutLib(true /*displayError*/);
                if (layoutLib != null) {
                    layoutLib.clearCaches(mEditedFile.getProject());
                }
            }

            if (flags.code) {
                // only recompute if the custom view loader was used to load some code.
                if (mProjectCallback != null && mProjectCallback.isUsed()) {
                    mProjectCallback = null;
                    recompute = true;
                }
            }

            if (flags.manifest) {
                recompute |= computeSdkVersion();
            }

            if (recompute) {
                if (mEditorDelegate.isGraphicalEditorActive()) {
                    recomputeLayout();
                } else {
                    mNeedsRecompute = true;
                }
            }
        }
    }

    // ---- Error handling ----

    /**
     * Switches the sash to display the error label.
     *
     * @param errorFormat The new error to display if not null.
     * @param parameters String.format parameters for the error format.
     */
    private void displayError(String errorFormat, Object...parameters) {
        if (errorFormat != null) {
            mErrorLabel.setText(String.format(errorFormat, parameters));
        } else {
            mErrorLabel.setText("");
        }
        mSashError.setMaximizedControl(null);
    }

    /** Displays the canvas and hides the error label. */
    private void hideError() {
        mErrorLabel.setText("");
        mSashError.setMaximizedControl(mCanvasViewer.getControl());
    }

    /** Display the problem list encountered during a render */
    private void displayUserStackTrace(RenderLogger logger, boolean append) {
        List<Throwable> throwables = logger.getFirstTrace();
        if (throwables == null || throwables.isEmpty()) {
            return;
        }

        Throwable throwable = throwables.get(0);

        if (throwable instanceof RenderSecurityException) {
            addActionLink(mErrorLabel, ActionLinkStyleRange.LINK_DISABLE_SANDBOX,
                    "\nTurn off custom view rendering sandbox\n");

            StringBuilder builder = new StringBuilder(200);
            String lastFailedPath = RenderSecurityManager.getLastFailedPath();
            if (lastFailedPath != null) {
                builder.append("Diagnostic info for ADT bug report:\n");
                builder.append("Failed path: ").append(lastFailedPath).append('\n');
                String tempDir = System.getProperty("java.io.tmpdir");
                builder.append("Normal temp dir: ").append(tempDir).append('\n');
                File normalized = new File(tempDir);
                builder.append("Normalized temp dir: ").append(normalized.getPath()).append('\n');
                try {
                    builder.append("Canonical temp dir: ").append(normalized.getCanonicalPath())
                    .append('\n');
                } catch (IOException e) {
                    // ignore
                }
                builder.append("os.name: ").append(System.getProperty("os.name")).append('\n');
                builder.append("os.version: ").append(System.getProperty("os.version"));
                builder.append('\n');
                builder.append("java.runtime.version: ");
                builder.append(System.getProperty("java.runtime.version"));
            }
            if (throwable.getMessage().equals("Unable to create temporary file")) {
                String javaVersion = System.getProperty("java.version");
                if (javaVersion.startsWith("1.7.0_")) {
                    int version = Integer
                            .parseInt(javaVersion.substring(javaVersion.indexOf('_') + 1));
                    if (version > 0 && version < 45) {
                        builder.append('\n');
                        builder.append("Tip: This may be caused by using an older version " +
                                "of JDK 1.7.0; try using at least 1.7.0_45 (you are using " +
                                javaVersion + ")");
                    }
                }
            }
            if (builder.length() > 0) {
                addText(mErrorLabel, builder.toString());
            }
        }

        StackTraceElement[] frames = throwable.getStackTrace();
        int end = -1;
        boolean haveInterestingFrame = false;
        for (int i = 0; i < frames.length; i++) {
            StackTraceElement frame = frames[i];
            if (isInterestingFrame(frame)) {
                haveInterestingFrame = true;
            }
            String className = frame.getClassName();
            if (className.equals(
                    "com.android.layoutlib.bridge.impl.RenderSessionImpl")) { //$NON-NLS-1$
                end = i;
                break;
            }
        }

        if (end == -1 || !haveInterestingFrame) {
            // Not a recognized stack trace range: just skip it
            return;
        }

        if (!append) {
            mErrorLabel.setText("\n");    //$NON-NLS-1$
        } else {
            addText(mErrorLabel, "\n\n"); //$NON-NLS-1$
        }

        addText(mErrorLabel, throwable.toString() + '\n');
        for (int i = 0; i < end; i++) {
            StackTraceElement frame = frames[i];
            String className = frame.getClassName();
            String methodName = frame.getMethodName();
            addText(mErrorLabel, "    at " + className + '.' + methodName + '(');
            String fileName = frame.getFileName();
            if (fileName != null && !fileName.isEmpty()) {
                int lineNumber = frame.getLineNumber();
                String location = fileName + ':' + lineNumber;
                if (isInterestingFrame(frame)) {
                    addActionLink(mErrorLabel, ActionLinkStyleRange.LINK_OPEN_LINE,
                            location, className, methodName, fileName, lineNumber);
                } else {
                    addText(mErrorLabel, location);
                }
                addText(mErrorLabel, ")\n"); //$NON-NLS-1$
            }
        }
    }

    private static boolean isInterestingFrame(StackTraceElement frame) {
        String className = frame.getClassName();
        return !(className.startsWith("android.")         //$NON-NLS-1$
                || className.startsWith("com.android.")   //$NON-NLS-1$
                || className.startsWith("java.")          //$NON-NLS-1$
                || className.startsWith("javax.")         //$NON-NLS-1$
                || className.startsWith("sun."));         //$NON-NLS-1$
    }

    /**
     * Switches the sash to display the error label to show a list of
     * missing classes and give options to create them.
     */
    private void displayFailingClasses(Set<String> missingClasses, Set<String> brokenClasses,
            boolean append) {
        if (missingClasses.size() == 0 && brokenClasses.size() == 0) {
            return;
        }

        if (!append) {
            mErrorLabel.setText("");    //$NON-NLS-1$
        } else {
            addText(mErrorLabel, "\n"); //$NON-NLS-1$
        }

        if (missingClasses.size() > 0) {
            addText(mErrorLabel, "The following classes could not be found:\n");
            for (String clazz : missingClasses) {
                addText(mErrorLabel, "- ");
                addText(mErrorLabel, clazz);
                addText(mErrorLabel, " (");

                IProject project = getProject();
                Collection<String> customViews = getCustomViewClassNames(project);
                addTypoSuggestions(clazz, customViews, false);
                addTypoSuggestions(clazz, customViews, true);
                addTypoSuggestions(clazz, getAndroidViewClassNames(project), false);

                addActionLink(mErrorLabel,
                        ActionLinkStyleRange.LINK_FIX_BUILD_PATH, "Fix Build Path", clazz);
                addText(mErrorLabel, ", ");
                addActionLink(mErrorLabel,
                        ActionLinkStyleRange.LINK_EDIT_XML, "Edit XML", clazz);
                if (clazz.indexOf('.') != -1) {
                    // Add "Create Class" link, but only for custom views
                    addText(mErrorLabel, ", ");
                    addActionLink(mErrorLabel,
                            ActionLinkStyleRange.LINK_CREATE_CLASS, "Create Class", clazz);
                }
                addText(mErrorLabel, ")\n");
            }
        }
        if (brokenClasses.size() > 0) {
            addText(mErrorLabel, "The following classes could not be instantiated:\n");

            // Do we have a custom class (not an Android or add-ons class)
            boolean haveCustomClass = false;

            for (String clazz : brokenClasses) {
                addText(mErrorLabel, "- ");
                addText(mErrorLabel, clazz);
                addText(mErrorLabel, " (");
                addActionLink(mErrorLabel,
                        ActionLinkStyleRange.LINK_OPEN_CLASS, "Open Class", clazz);
                addText(mErrorLabel, ", ");
                addActionLink(mErrorLabel,
                        ActionLinkStyleRange.LINK_SHOW_LOG, "Show Error Log", clazz);
                addText(mErrorLabel, ")\n");

                if (!(clazz.startsWith("android.") || //$NON-NLS-1$
                        clazz.startsWith("com.google."))) { //$NON-NLS-1$
                    haveCustomClass = true;
                }
            }

            addText(mErrorLabel, "See the Error Log (Window > Show View) for more details.\n");

            if (haveCustomClass) {
                addBoldText(mErrorLabel, "Tip: Use View.isInEditMode() in your custom views "
                        + "to skip code when shown in Eclipse");
            }
        }

        mSashError.setMaximizedControl(null);
    }

    private void addTypoSuggestions(String actual, Collection<String> views,
            boolean compareWithPackage) {
        if (views.size() == 0) {
            return;
        }

        // Look for typos and try to match with custom views and android views
        String actualBase = actual.substring(actual.lastIndexOf('.') + 1);
        int maxDistance = actualBase.length() >= 4 ? 2 : 1;

        if (views.size() > 0) {
            for (String suggested : views) {
                String suggestedBase = suggested.substring(suggested.lastIndexOf('.') + 1);

                String matchWith = compareWithPackage ? suggested : suggestedBase;
                if (Math.abs(actualBase.length() - matchWith.length()) > maxDistance) {
                    // The string lengths differ more than the allowed edit distance;
                    // no point in even attempting to compute the edit distance (requires
                    // O(n*m) storage and O(n*m) speed, where n and m are the string lengths)
                    continue;
                }
                if (LintUtils.editDistance(actualBase, matchWith) <= maxDistance) {
                    // Suggest this class as a typo for the given class
                    String labelClass = (suggestedBase.equals(actual) || actual.indexOf('.') != -1)
                        ? suggested : suggestedBase;
                    addActionLink(mErrorLabel,
                            ActionLinkStyleRange.LINK_CHANGE_CLASS_TO,
                            String.format("Change to %1$s",
                                    // Only show full package name if class name
                                    // is the same
                                    labelClass),
                            actual,
                            viewNeedsPackage(suggested) ? suggested : suggestedBase);
                    addText(mErrorLabel, ", ");
                }
            }
        }
    }

    private static Collection<String> getCustomViewClassNames(IProject project) {
        CustomViewFinder finder = CustomViewFinder.get(project);
        Collection<String> views = finder.getAllViews();
        if (views == null) {
            finder.refresh();
            views = finder.getAllViews();
        }

        return views;
    }

    private static Collection<String> getAndroidViewClassNames(IProject project) {
        Sdk currentSdk = Sdk.getCurrent();
        IAndroidTarget target = currentSdk.getTarget(project);
        if (target != null) {
            AndroidTargetData targetData = currentSdk.getTargetData(target);
            if (targetData != null) {
                LayoutDescriptors layoutDescriptors = targetData.getLayoutDescriptors();
                return layoutDescriptors.getAllViewClassNames();
            }
        }

        return Collections.emptyList();
    }

    /** Add a normal line of text to the styled text widget. */
    private void addText(StyledText styledText, String...string) {
        for (String s : string) {
            styledText.append(s);
        }
    }

    /** Display the problem list encountered during a render */
    private void displayLoggerProblems(IProject project, RenderLogger logger) {
        if (logger.hasProblems()) {
            mErrorLabel.setText("");
            // A common source of problems is attempting to open a layout when there are
            // compilation errors. In this case, may not have run (or may not be up to date)
            // so resources cannot be looked up etc. Explain this situation to the user.

            boolean hasAaptErrors = false;
            boolean hasJavaErrors = false;
            try {
                IMarker[] markers;
                markers = project.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
                if (markers.length > 0) {
                    for (IMarker marker : markers) {
                        String markerType = marker.getType();
                        if (markerType.equals(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER)) {
                            int severity = marker.getAttribute(IMarker.SEVERITY, -1);
                            if (severity == IMarker.SEVERITY_ERROR) {
                                hasJavaErrors = true;
                            }
                        } else if (markerType.equals(AndmoreAndroidConstants.MARKER_AAPT_COMPILE)) {
                            int severity = marker.getAttribute(IMarker.SEVERITY, -1);
                            if (severity == IMarker.SEVERITY_ERROR) {
                                hasAaptErrors = true;
                            }
                        }
                    }
                }
            } catch (CoreException e) {
                AndmoreAndroidPlugin.log(e, null);
            }

            if (logger.seenTagPrefix(LayoutLog.TAG_RESOURCES_RESOLVE_THEME_ATTR)) {
                addBoldText(mErrorLabel,
                        "Missing styles. Is the correct theme chosen for this layout?\n");
                addText(mErrorLabel,
                        "Use the Theme combo box above the layout to choose a different layout, " +
                        "or fix the theme style references.\n\n");
            }

            List<Throwable> trace = logger.getFirstTrace();
            if (trace != null
                    && trace.toString().contains(
                            "java.lang.IndexOutOfBoundsException: Index: 2, Size: 2") //$NON-NLS-1$
                    && mConfigChooser.getConfiguration().getDensity() == Density.TV) {
                addBoldText(mErrorLabel,
                        "It looks like you are using a render target where the layout library " +
                        "does not support the tvdpi density.\n\n");
                addText(mErrorLabel, "Please try either updating to " +
                        "the latest available version (using the SDK manager), or if no updated " +
                        "version is available for this specific version of Android, try using " +
                        "a more recent render target version.\n\n");

            }

            if (hasAaptErrors && logger.seenTagPrefix(LayoutLog.TAG_RESOURCES_PREFIX)) {
                // Text will automatically be wrapped by the error widget so no reason
                // to insert linebreaks in this error message:
                String message =
                    "NOTE: This project contains resource errors, so aapt did not succeed, "
                     + "which can cause rendering failures. "
                     + "Fix resource problems first.\n\n";
                 addBoldText(mErrorLabel, message);
            } else if (hasJavaErrors && mProjectCallback != null && mProjectCallback.isUsed()) {
                // Text will automatically be wrapped by the error widget so no reason
                // to insert linebreaks in this error message:
                String message =
                   "NOTE: This project contains Java compilation errors, "
                    + "which can cause rendering failures for custom views. "
                    + "Fix compilation problems first.\n\n";
                addBoldText(mErrorLabel, message);
            }

            if (logger.seenTag(RenderLogger.TAG_MISSING_DIMENSION)) {
                List<UiElementNode> elements = UiDocumentNode.getAllElements(getModel());
                for (UiElementNode element : elements) {
                    String width = element.getAttributeValue(ATTR_LAYOUT_WIDTH);
                    if (width == null || width.length() == 0) {
                        addSetAttributeLink(element, ATTR_LAYOUT_WIDTH);
                    }

                    String height = element.getAttributeValue(ATTR_LAYOUT_HEIGHT);
                    if (height == null || height.length() == 0) {
                        addSetAttributeLink(element, ATTR_LAYOUT_HEIGHT);
                    }
                }
            }

            String problems = logger.getProblems(false /*includeFidelityWarnings*/);
            addText(mErrorLabel, problems);

            List<String> fidelityWarnings = logger.getFidelityWarnings();
            if (fidelityWarnings != null && fidelityWarnings.size() > 0) {
                addText(mErrorLabel,
                        "The graphics preview in the layout editor may not be accurate:\n");
                for (String warning : fidelityWarnings) {
                    addText(mErrorLabel, warning + ' ');
                    addActionLink(mErrorLabel,
                            ActionLinkStyleRange.IGNORE_FIDELITY_WARNING,
                            "(Ignore for this session)\n", warning);
                }
            }

            mSashError.setMaximizedControl(null);
        } else {
            mSashError.setMaximizedControl(mCanvasViewer.getControl());
        }
    }

    /** Appends an action link to set the given attribute on the given value */
    private void addSetAttributeLink(UiElementNode element, String attribute) {
        if (element.getXmlNode().getNodeName().equals(GRID_LAYOUT)) {
            // GridLayout does not require a layout_width or layout_height to be defined
            return;
        }

        String fill = VALUE_FILL_PARENT;
        // See whether we should offer match_parent instead of fill_parent
        Sdk currentSdk = Sdk.getCurrent();
        if (currentSdk != null) {
            IAndroidTarget target = currentSdk.getTarget(getProject());
            if (target.getVersion().getApiLevel() >= 8) {
                fill = VALUE_MATCH_PARENT;
            }
        }

        String id = element.getAttributeValue(ATTR_ID);
        if (id == null || id.length() == 0) {
            id = '<' + element.getXmlNode().getNodeName() + '>';
        } else {
            id = BaseViewRule.stripIdPrefix(id);
        }

        addText(mErrorLabel, String.format("\"%1$s\" does not set the required %2$s attribute:\n",
                id, attribute));
        addText(mErrorLabel, " (1) ");
        addActionLink(mErrorLabel,
                ActionLinkStyleRange.SET_ATTRIBUTE,
                String.format("Set to \"%1$s\"", VALUE_WRAP_CONTENT),
                element, attribute, VALUE_WRAP_CONTENT);
        addText(mErrorLabel, "\n (2) ");
        addActionLink(mErrorLabel,
                ActionLinkStyleRange.SET_ATTRIBUTE,
                String.format("Set to \"%1$s\"\n", fill),
                element, attribute, fill);
    }

    /** Appends the given text as a bold string in the given text widget */
    private void addBoldText(StyledText styledText, String text) {
        String s = styledText.getText();
        int start = (s == null ? 0 : s.length());

        styledText.append(text);
        StyleRange sr = new StyleRange();
        sr.start = start;
        sr.length = text.length();
        sr.fontStyle = SWT.BOLD;
        styledText.setStyleRange(sr);
    }

    /**
     * Add a URL-looking link to the styled text widget.
     * <p/>
     * A mouse-click listener is setup and it interprets the link based on the
     * action, corresponding to the value fields in {@link ActionLinkStyleRange}.
     */
    private void addActionLink(StyledText styledText, int action, String label,
            Object... data) {
        String s = styledText.getText();
        int start = (s == null ? 0 : s.length());
        styledText.append(label);

        StyleRange sr = new ActionLinkStyleRange(action, data);
        sr.start = start;
        sr.length = label.length();
        sr.fontStyle = SWT.NORMAL;
        sr.underlineStyle = SWT.UNDERLINE_LINK;
        sr.underline = true;
        styledText.setStyleRange(sr);
    }

    /**
     * Looks up the resource file corresponding to the given type
     *
     * @param type The type of resource to look up, such as {@link ResourceType#LAYOUT}
     * @param name The name of the resource (not including ".xml")
     * @param isFrameworkResource if true, the resource is a framework resource, otherwise
     *            it's a project resource
     * @return the resource file defining the named resource, or null if not found
     */
    public IPath findResourceFile(ResourceType type, String name, boolean isFrameworkResource) {
        // FIXME: This code does not handle theme value resolution.
        // There is code to handle this, but it's in layoutlib; we should
        // expose that and use it here.

        Map<ResourceType, Map<String, ResourceValue>> map;
        map = isFrameworkResource ? mConfiguredFrameworkRes : mConfiguredProjectRes;
        if (map == null) {
            // Not yet configured
            return null;
        }

        Map<String, ResourceValue> layoutMap = map.get(type);
        if (layoutMap != null) {
            ResourceValue value = layoutMap.get(name);
            if (value != null) {
                String valueStr = value.getValue();
                if (valueStr.startsWith("?")) { //$NON-NLS-1$
                    // FIXME: It's a reference. We should resolve this properly.
                    return null;
                }
                return new Path(valueStr);
            }
        }

        return null;
    }

    /**
     * Looks up the path to the file corresponding to the given attribute value, such as
     * @layout/foo, which will return the foo.xml file in res/layout/. (The general format
     * of the resource url is {@literal @[<package_name>:]<resource_type>/<resource_name>}.
     *
     * @param url the attribute url
     * @return the path to the file defining this attribute, or null if not found
     */
    public IPath findResourceFile(String url) {
        if (!url.startsWith("@")) { //$NON-NLS-1$
            return null;
        }
        int typeEnd = url.indexOf('/', 1);
        if (typeEnd == -1) {
            return null;
        }
        int nameBegin = typeEnd + 1;
        int typeBegin = 1;
        int colon = url.lastIndexOf(':', typeEnd);
        boolean isFrameworkResource = false;
        if (colon != -1) {
            // The URL contains a package name.
            // While the url format technically allows other package names,
            // the platform apparently only supports @android for now (or if it does,
            // there are no usages in the current code base so this is not common).
            String packageName = url.substring(typeBegin, colon);
            if (ANDROID_PKG.equals(packageName)) {
                isFrameworkResource = true;
            }

            typeBegin = colon + 1;
        }

        String typeName = url.substring(typeBegin, typeEnd);
        ResourceType type = ResourceType.getEnum(typeName);
        if (type == null) {
            return null;
        }

        String name = url.substring(nameBegin);
        return findResourceFile(type, name, isFrameworkResource);
    }

    /**
     * Resolve the given @string reference into a literal String using the current project
     * configuration
     *
     * @param text the text resource reference to resolve
     * @return the resolved string, or null
     */
    public String findString(String text) {
        if (text.startsWith(STRING_PREFIX)) {
            return findString(text.substring(STRING_PREFIX.length()), false);
        } else if (text.startsWith(ANDROID_STRING_PREFIX)) {
            return findString(text.substring(ANDROID_STRING_PREFIX.length()), true);
        } else {
            return text;
        }
    }

    private String findString(String name, boolean isFrameworkResource) {
        Map<ResourceType, Map<String, ResourceValue>> map;
        map = isFrameworkResource ? mConfiguredFrameworkRes : mConfiguredProjectRes;
        if (map == null) {
            // Not yet configured
            return null;
        }

        Map<String, ResourceValue> layoutMap = map.get(ResourceType.STRING);
        if (layoutMap != null) {
            ResourceValue value = layoutMap.get(name);
            if (value != null) {
                // FIXME: This code does not handle theme value resolution.
                // There is code to handle this, but it's in layoutlib; we should
                // expose that and use it here.
                return value.getValue();
            }
        }

        return null;
    }

    /**
     * This StyleRange represents a clickable link in the render output, where various
     * actions can be taken such as creating a class, opening the project chooser to
     * adjust the build path, etc.
     */
    private class ActionLinkStyleRange extends StyleRange {
        /** Create a view class */
        private static final int LINK_CREATE_CLASS = 1;
        /** Edit the build path for the current project */
        private static final int LINK_FIX_BUILD_PATH = 2;
        /** Show the XML tab */
        private static final int LINK_EDIT_XML = 3;
        /** Open the given class */
        private static final int LINK_OPEN_CLASS = 4;
        /** Show the error log */
        private static final int LINK_SHOW_LOG = 5;
        /** Change the class reference to the given fully qualified name */
        private static final int LINK_CHANGE_CLASS_TO = 6;
        /** Ignore the given fidelity warning */
        private static final int IGNORE_FIDELITY_WARNING = 7;
        /** Set an attribute on the given XML element to a given value  */
        private static final int SET_ATTRIBUTE = 8;
        /** Open the given file and line number */
        private static final int LINK_OPEN_LINE = 9;
        /** Disable sandbox */
        private static final int LINK_DISABLE_SANDBOX = 10;

        /** Client data: the contents depend on the specific action */
        private final Object[] mData;
        /** The action to be taken when the link is clicked */
        private final int mAction;

        private ActionLinkStyleRange(int action, Object... data) {
            super();
            mAction = action;
            mData = data;
        }

        /** Performs the click action */
        public void onClick() {
            switch (mAction) {
                case LINK_CREATE_CLASS:
                    createNewClass((String) mData[0]);
                    break;
                case LINK_EDIT_XML:
                    mEditorDelegate.getEditor().setActivePage(AndroidXmlEditor.TEXT_EDITOR_ID);
                    break;
                case LINK_FIX_BUILD_PATH:
                    @SuppressWarnings("restriction")
                    String id = BuildPathsPropertyPage.PROP_ID;
                    PreferencesUtil.createPropertyDialogOn(
                            AndmoreAndroidPlugin.getShell(),
                            getProject(), id, null, null).open();
                    break;
                case LINK_OPEN_CLASS:
                    AndmoreAndroidPlugin.openJavaClass(getProject(), (String) mData[0]);
                    break;
                case LINK_OPEN_LINE:
                    boolean success = AndmoreAndroidPlugin.openStackTraceLine(
                            (String) mData[0],   // class
                            (String) mData[1],   // method
                            (String) mData[2],   // file
                            (Integer) mData[3]); // line
                    if (!success) {
                        MessageDialog.openError(mErrorLabel.getShell(), "Not Found",
                                String.format("Could not find %1$s.%2$s", mData[0], mData[1]));
                    }
                    break;
                case LINK_SHOW_LOG:
                    IWorkbench workbench = PlatformUI.getWorkbench();
                    IWorkbenchWindow workbenchWindow = workbench.getActiveWorkbenchWindow();
                    try {
                        IWorkbenchPage page = workbenchWindow.getActivePage();
                        page.showView("org.eclipse.pde.runtime.LogView"); //$NON-NLS-1$
                    } catch (PartInitException e) {
                        AndmoreAndroidPlugin.log(e, null);
                    }
                    break;
                case LINK_CHANGE_CLASS_TO:
                    // Change class reference of mData[0] to mData[1]
                    // TODO: run under undo lock
                    MultiTextEdit edits = new MultiTextEdit();
                    ISourceViewer textViewer =
                        mEditorDelegate.getEditor().getStructuredSourceViewer();
                    IDocument document = textViewer.getDocument();
                    String xml = document.get();
                    int index = 0;
                    // Replace <old with <new and </old with </new
                    String prefix = "<"; //$NON-NLS-1$
                    String find = prefix + mData[0];
                    String replaceWith = prefix + mData[1];
                    while (true) {
                        index = xml.indexOf(find, index);
                        if (index == -1) {
                            break;
                        }
                        edits.addChild(new ReplaceEdit(index, find.length(), replaceWith));
                        index += find.length();
                    }
                    index = 0;
                    prefix = "</"; //$NON-NLS-1$
                    find = prefix + mData[0];
                    replaceWith = prefix + mData[1];
                    while (true) {
                        index = xml.indexOf(find, index);
                        if (index == -1) {
                            break;
                        }
                        edits.addChild(new ReplaceEdit(index, find.length(), replaceWith));
                        index += find.length();
                    }
                    // Handle <view class="old">
                    index = 0;
                    prefix = "\""; //$NON-NLS-1$
                    String suffix = "\""; //$NON-NLS-1$
                    find = prefix + mData[0] + suffix;
                    replaceWith = prefix + mData[1] + suffix;
                    while (true) {
                        index = xml.indexOf(find, index);
                        if (index == -1) {
                            break;
                        }
                        edits.addChild(new ReplaceEdit(index, find.length(), replaceWith));
                        index += find.length();
                    }
                    try {
                        edits.apply(document);
                    } catch (MalformedTreeException e) {
                        AndmoreAndroidPlugin.log(e, null);
                    } catch (BadLocationException e) {
                        AndmoreAndroidPlugin.log(e, null);
                    }
                    break;
                case IGNORE_FIDELITY_WARNING:
                    RenderLogger.ignoreFidelityWarning((String) mData[0]);
                    recomputeLayout();
                    break;
                case SET_ATTRIBUTE: {
                    final UiElementNode element = (UiElementNode) mData[0];
                    final String attribute = (String) mData[1];
                    final String value = (String) mData[2];
                    mEditorDelegate.getEditor().wrapUndoEditXmlModel(
                            String.format("Set \"%1$s\" to \"%2$s\"", attribute, value),
                            new Runnable() {
                        @Override
                        public void run() {
                            element.setAttributeValue(attribute, ANDROID_URI, value, true);
                            element.commitDirtyAttributesToXml();
                        }
                    });
                    break;
                }
                case LINK_DISABLE_SANDBOX: {
                    RenderSecurityManager.sEnabled = false;
                    recomputeLayout();

                    MessageDialog.openInformation(AndmoreAndroidPlugin.getShell(),
                        "Disabled Rendering Sandbox",
                        "The custom view rendering sandbox was disabled for this session.\n\n" +
                        "You can turn it off permanently by adding\n" +
                        "-D" + ENABLED_PROPERTY + "=" + VALUE_FALSE + "\n" +
                        "as a new line in eclipse.ini.");

                    break;
                }
                default:
                    assert false : mAction;
                    break;
            }
        }

        @Override
        public boolean similarTo(StyleRange style) {
            // Prevent adjacent link ranges from getting merged
            return false;
        }
    }

    /**
     * Returns the error label for the graphical editor (which may not be visible
     * or showing errors)
     *
     * @return the error label, never null
     */
    StyledText getErrorLabel() {
        return mErrorLabel;
    }

    /**
     * Monitor clicks on the error label.
     * If the click happens on a style range created by
     * {@link GraphicalEditorPart#addClassLink(StyledText, String)}, we assume it's about
     * a missing class and we then proceed to display the standard Eclipse class creator wizard.
     */
    private class ErrorLabelListener extends MouseAdapter {

        @Override
        public void mouseUp(MouseEvent event) {
            super.mouseUp(event);

            if (event.widget != mErrorLabel) {
                return;
            }

            int offset = mErrorLabel.getCaretOffset();

            StyleRange r = null;
            StyleRange[] ranges = mErrorLabel.getStyleRanges();
            if (ranges != null && ranges.length > 0) {
                for (StyleRange sr : ranges) {
                    if (sr.start <= offset && sr.start + sr.length > offset) {
                        r = sr;
                        break;
                    }
                }
            }

            if (r instanceof ActionLinkStyleRange) {
                ActionLinkStyleRange range = (ActionLinkStyleRange) r;
                range.onClick();
            }

            LayoutCanvas canvas = getCanvasControl();
            canvas.updateMenuActionState();
        }
    }

    private void createNewClass(String fqcn) {

        int pos = fqcn.lastIndexOf('.');
        String packageName = pos < 0 ? "" : fqcn.substring(0, pos);  //$NON-NLS-1$
        String className = pos <= 0 || pos >= fqcn.length() ? "" : fqcn.substring(pos + 1); //$NON-NLS-1$

        // create the wizard page for the class creation, and configure it
        NewClassWizardPage page = new NewClassWizardPage();

        // set the parent class
        page.setSuperClass(SdkConstants.CLASS_VIEW, true /* canBeModified */);

        // get the source folders as java elements.
        IPackageFragmentRoot[] roots = getPackageFragmentRoots(
                mEditorDelegate.getEditor().getProject(),
                false /*includeContainers*/, true /*skipGenFolder*/);

        IPackageFragmentRoot currentRoot = null;
        IPackageFragment currentFragment = null;
        int packageMatchCount = -1;

        for (IPackageFragmentRoot root : roots) {
            // Get the java element for the package.
            // This method is said to always return a IPackageFragment even if the
            // underlying folder doesn't exist...
            IPackageFragment fragment = root.getPackageFragment(packageName);
            if (fragment != null && fragment.exists()) {
                // we have a perfect match! we use it.
                currentRoot = root;
                currentFragment = fragment;
                packageMatchCount = -1;
                break;
            } else {
                // we don't have a match. we look for the fragment with the best match
                // (ie the closest parent package we can find)
                try {
                    IJavaElement[] children;
                    children = root.getChildren();
                    for (IJavaElement child : children) {
                        if (child instanceof IPackageFragment) {
                            fragment = (IPackageFragment)child;
                            if (packageName.startsWith(fragment.getElementName())) {
                                // its a match. get the number of segments
                                String[] segments = fragment.getElementName().split("\\."); //$NON-NLS-1$
                                if (segments.length > packageMatchCount) {
                                    packageMatchCount = segments.length;
                                    currentFragment = fragment;
                                    currentRoot = root;
                                }
                            }
                        }
                    }
                } catch (JavaModelException e) {
                    // Couldn't get the children: we just ignore this package root.
                }
            }
        }

        ArrayList<IPackageFragment> createdFragments = null;

        if (currentRoot != null) {
            // if we have a perfect match, we set it and we're done.
            if (packageMatchCount == -1) {
                page.setPackageFragmentRoot(currentRoot, true /* canBeModified*/);
                page.setPackageFragment(currentFragment, true /* canBeModified */);
            } else {
                // we have a partial match.
                // create the package. We have to start with the first segment so that we
                // know what to delete in case of a cancel.
                try {
                    createdFragments = new ArrayList<IPackageFragment>();

                    int totalCount = packageName.split("\\.").length; //$NON-NLS-1$
                    int count = 0;
                    int index = -1;
                    // skip the matching packages
                    while (count < packageMatchCount) {
                        index = packageName.indexOf('.', index+1);
                        count++;
                    }

                    // create the rest of the segments, except for the last one as indexOf will
                    // return -1;
                    while (count < totalCount - 1) {
                        index = packageName.indexOf('.', index+1);
                        count++;
                        createdFragments.add(currentRoot.createPackageFragment(
                                packageName.substring(0, index),
                                true /* force*/, new NullProgressMonitor()));
                    }

                    // create the last package
                    createdFragments.add(currentRoot.createPackageFragment(
                            packageName, true /* force*/, new NullProgressMonitor()));

                    // set the root and fragment in the Wizard page
                    page.setPackageFragmentRoot(currentRoot, true /* canBeModified*/);
                    page.setPackageFragment(createdFragments.get(createdFragments.size()-1),
                            true /* canBeModified */);
                } catch (JavaModelException e) {
                    // If we can't create the packages, there's a problem.
                    // We revert to the default package
                    for (IPackageFragmentRoot root : roots) {
                        // Get the java element for the package.
                        // This method is said to always return a IPackageFragment even if the
                        // underlying folder doesn't exist...
                        IPackageFragment fragment = root.getPackageFragment(packageName);
                        if (fragment != null && fragment.exists()) {
                            page.setPackageFragmentRoot(root, true /* canBeModified*/);
                            page.setPackageFragment(fragment, true /* canBeModified */);
                            break;
                        }
                    }
                }
            }
        } else if (roots.length > 0) {
            // if we haven't found a valid fragment, we set the root to the first source folder.
            page.setPackageFragmentRoot(roots[0], true /* canBeModified*/);
        }

        // if we have a starting class name we use it
        if (className != null) {
            page.setTypeName(className, true /* canBeModified*/);
        }

        // create the action that will open it the wizard.
        OpenNewClassWizardAction action = new OpenNewClassWizardAction();
        action.setConfiguredWizardPage(page);
        action.run();
        IJavaElement element = action.getCreatedElement();

        if (element == null) {
            // lets delete the packages we created just for this.
            // we need to start with the leaf and go up
            if (createdFragments != null) {
                try {
                    for (int i = createdFragments.size() - 1 ; i >= 0 ; i--) {
                        createdFragments.get(i).delete(true /* force*/,
                                                       new NullProgressMonitor());
                    }
                } catch (JavaModelException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Computes and return the {@link IPackageFragmentRoot}s corresponding to the source
     * folders of the specified project.
     *
     * @param project the project
     * @param includeContainers True to include containers
     * @param skipGenFolder True to skip the "gen" folder
     * @return an array of IPackageFragmentRoot.
     */
    private IPackageFragmentRoot[] getPackageFragmentRoots(IProject project,
            boolean includeContainers, boolean skipGenFolder) {
        ArrayList<IPackageFragmentRoot> result = new ArrayList<IPackageFragmentRoot>();
        try {
            IJavaProject javaProject = JavaCore.create(project);
            IPackageFragmentRoot[] roots = javaProject.getPackageFragmentRoots();
            for (int i = 0; i < roots.length; i++) {
                if (skipGenFolder) {
                    IResource resource = roots[i].getResource();
                    if (resource != null && resource.getName().equals(FD_GEN_SOURCES)) {
                        continue;
                    }
                }
                IClasspathEntry entry = roots[i].getRawClasspathEntry();
                if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE ||
                        (includeContainers &&
                                entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER)) {
                    result.add(roots[i]);
                }
            }
        } catch (JavaModelException e) {
        }

        return result.toArray(new IPackageFragmentRoot[result.size()]);
    }

    /**
     * Reopens this file as included within the given file (this assumes that the given
     * file has an include tag referencing this view, and the set of views that have this
     * property can be found using the {@link IncludeFinder}.
     *
     * @param includeWithin reference to a file to include as a surrounding context,
     *   or null to show the file standalone
     */
    public void showIn(Reference includeWithin) {
        mIncludedWithin = includeWithin;

        if (includeWithin != null) {
            IFile file = includeWithin.getFile();

            // Update configuration
            if (file != null) {
                mConfigChooser.resetConfigFor(file);
            }
        }
        recomputeLayout();
    }

    /**
     * Return all resource names of a given type, either in the project or in the
     * framework.
     *
     * @param framework if true, return all the framework resource names, otherwise return
     *            all the project resource names
     * @param type the type of resource to look up
     * @return a collection of resource names, never null but possibly empty
     */
    public Collection<String> getResourceNames(boolean framework, ResourceType type) {
        Map<ResourceType, Map<String, ResourceValue>> map =
            framework ? mConfiguredFrameworkRes : mConfiguredProjectRes;
        Map<String, ResourceValue> animations = map.get(type);
        if (animations != null) {
            return animations.keySet();
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Return this editor's current configuration
     *
     * @return the current configuration
     */
    public FolderConfiguration getConfiguration() {
        return mConfigChooser.getConfiguration().getFullConfig();
    }

    /**
     * Figures out the project's minSdkVersion and targetSdkVersion and return whether the values
     * have changed.
     */
    private boolean computeSdkVersion() {
        int oldMinSdkVersion = mMinSdkVersion;
        int oldTargetSdkVersion = mTargetSdkVersion;

        Pair<Integer, Integer> v = ManifestInfo.computeSdkVersions(mEditedFile.getProject());
        mMinSdkVersion = v.getFirst();
        mTargetSdkVersion = v.getSecond();

        return oldMinSdkVersion != mMinSdkVersion || oldTargetSdkVersion != mTargetSdkVersion;
    }

    /**
     * Returns the associated configuration chooser
     *
     * @return the configuration chooser
     */
    @NonNull
    public ConfigurationChooser getConfigurationChooser() {
        return mConfigChooser;
    }

    /**
     * Returns the associated layout actions bar
     *
     * @return the layout actions bar
     */
    @NonNull
    public LayoutActionBar getLayoutActionBar() {
        return mActionBar;
    }

    /**
     * Returns the target SDK version
     *
     * @return the target SDK version
     */
    public int getTargetSdkVersion() {
        return mTargetSdkVersion;
    }

    /**
     * Returns the minimum SDK version
     *
     * @return the minimum SDK version
     */
    public int getMinSdkVersion() {
        return mMinSdkVersion;
    }

    /** If the flyout hover is showing, dismiss it */
    public void dismissHoverPalette() {
        mPaletteComposite.dismissHover();
    }

    // ---- Implements IFlyoutListener ----

    @Override
    public void stateChanged(int oldState, int newState) {
        // Auto zoom the surface if you open or close flyout windows such as the palette
        // or the property/outline views
        if (newState == STATE_OPEN || newState == STATE_COLLAPSED && oldState == STATE_OPEN) {
            getCanvasControl().setFitScale(true /*onlyZoomOut*/, true /*allowZoomIn*/);
        }

        sDockingStateVersion++;
        mDockingStateVersion = sDockingStateVersion;
    }
}

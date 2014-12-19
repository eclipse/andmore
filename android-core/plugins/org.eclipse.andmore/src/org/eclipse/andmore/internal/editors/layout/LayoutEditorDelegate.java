/*
 * Copyright (C) 2007 The Android Open Source Project
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

package org.eclipse.andmore.internal.editors.layout;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.annotations.VisibleForTesting;
import com.android.annotations.VisibleForTesting.Visibility;
import com.android.resources.ResourceFolderType;
import com.android.sdklib.IAndroidTarget;
import com.android.tools.lint.client.api.IssueRegistry;

import org.eclipse.andmore.AdtConstants;
import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.editors.AndroidXmlEditor;
import org.eclipse.andmore.internal.editors.XmlEditorMultiOutline;
import org.eclipse.andmore.internal.editors.common.CommonXmlDelegate;
import org.eclipse.andmore.internal.editors.common.CommonXmlEditor;
import org.eclipse.andmore.internal.editors.descriptors.DocumentDescriptor;
import org.eclipse.andmore.internal.editors.descriptors.ElementDescriptor;
import org.eclipse.andmore.internal.editors.descriptors.IUnknownDescriptorProvider;
import org.eclipse.andmore.internal.editors.layout.descriptors.CustomViewDescriptorService;
import org.eclipse.andmore.internal.editors.layout.descriptors.LayoutDescriptors;
import org.eclipse.andmore.internal.editors.layout.descriptors.ViewElementDescriptor;
import org.eclipse.andmore.internal.editors.layout.gle2.DomUtilities;
import org.eclipse.andmore.internal.editors.layout.gle2.GraphicalEditorPart;
import org.eclipse.andmore.internal.editors.layout.gle2.LayoutActionBar;
import org.eclipse.andmore.internal.editors.layout.gle2.LayoutCanvas;
import org.eclipse.andmore.internal.editors.layout.gle2.OutlinePage;
import org.eclipse.andmore.internal.editors.layout.gle2.SelectionManager;
import org.eclipse.andmore.internal.editors.layout.gre.RulesEngine;
import org.eclipse.andmore.internal.editors.layout.properties.PropertySheetPage;
import org.eclipse.andmore.internal.editors.layout.uimodel.UiViewElementNode;
import org.eclipse.andmore.internal.editors.uimodel.UiDocumentNode;
import org.eclipse.andmore.internal.lint.EclipseLintClient;
import org.eclipse.andmore.internal.lint.EclipseLintRunner;
import org.eclipse.andmore.internal.preferences.AdtPrefs;
import org.eclipse.andmore.internal.sdk.AndroidTargetData;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IShowEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.wst.sse.ui.StructuredTextEditor;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Multi-page form editor for /res/layout XML files.
 */
public class LayoutEditorDelegate extends CommonXmlDelegate
         implements IShowEditorInput, CommonXmlDelegate.IActionContributorDelegate {

    /** The prefix for layout folders that are not the default layout folder */
    private static final String LAYOUT_FOLDER_PREFIX = "layout-"; //$NON-NLS-1$

    public static class Creator implements IDelegateCreator {
        @Override
        @SuppressWarnings("unchecked")
        public LayoutEditorDelegate createForFile(
                @NonNull CommonXmlEditor delegator,
                @Nullable ResourceFolderType type) {
            if (ResourceFolderType.LAYOUT == type) {
                return new LayoutEditorDelegate(delegator);
            }

            return null;
        }
    }

    /**
     * Old standalone-editor ID.
     * Use {@link CommonXmlEditor#ID} instead.
     */
    public static final String LEGACY_EDITOR_ID =
        AdtConstants.EDITORS_NAMESPACE + ".layout.LayoutEditor"; //$NON-NLS-1$

    /** Root node of the UI element hierarchy */
    private UiDocumentNode mUiDocRootNode;

    private GraphicalEditorPart mGraphicalEditor;
    private int mGraphicalEditorIndex;

    /** Implementation of the {@link IContentOutlinePage} for this editor */
    private OutlinePage mLayoutOutline;

    /** The XML editor outline */
    private IContentOutlinePage mEditorOutline;

    /** Multiplexing outline, used for multi-page editors that have their own outline */
    private XmlEditorMultiOutline mMultiOutline;

    /**
     * Temporary flag set by the editor caret listener which is used to cause
     * the next getAdapter(IContentOutlinePage.class) call to return the editor
     * outline rather than the multi-outline. See the {@link #delegateGetAdapter}
     * method for details.
     */
    private boolean mCheckOutlineAdapter;

    /** Custom implementation of {@link IPropertySheetPage} for this editor */
    private IPropertySheetPage mPropertyPage;

    private final HashMap<String, ElementDescriptor> mUnknownDescriptorMap =
        new HashMap<String, ElementDescriptor>();

    private EclipseLintClient mClient;

    /**
     * Flag indicating if the replacement file is due to a config change.
     * If false, it means the new file is due to an "open action" from the user.
     */
    private boolean mNewFileOnConfigChange = false;

    /**
     * Checks whether an editor part is an instance of {@link CommonXmlEditor}
     * with an associated {@link LayoutEditorDelegate} delegate.
     *
     * @param editorPart An editor part. Can be null.
     * @return The {@link LayoutEditorDelegate} delegate associated with the editor or null.
     */
    public static @Nullable LayoutEditorDelegate fromEditor(@Nullable IEditorPart editorPart) {
        if (editorPart instanceof CommonXmlEditor) {
            CommonXmlDelegate delegate = ((CommonXmlEditor) editorPart).getDelegate();
            if (delegate instanceof LayoutEditorDelegate) {
                return ((LayoutEditorDelegate) delegate);
            }
        } else if (editorPart instanceof GraphicalEditorPart) {
            GraphicalEditorPart part = (GraphicalEditorPart) editorPart;
            return part.getEditorDelegate();
        }
        return null;
    }

    /**
     * Creates the form editor for resources XML files.
     */
    @VisibleForTesting(visibility=Visibility.PRIVATE)
    protected LayoutEditorDelegate(CommonXmlEditor editor) {
        super(editor, new LayoutContentAssist());
        // Note that LayoutEditor has its own listeners and does not
        // need to call editor.addDefaultTargetListener().
    }

    /**
     * Returns the {@link RulesEngine} associated with this editor
     *
     * @return the {@link RulesEngine} associated with this editor.
     */
    public RulesEngine getRulesEngine() {
        return mGraphicalEditor.getRulesEngine();
    }

    /**
     * Returns the {@link GraphicalEditorPart} associated with this editor
     *
     * @return the {@link GraphicalEditorPart} associated with this editor
     */
    public GraphicalEditorPart getGraphicalEditor() {
        return mGraphicalEditor;
    }

    /**
     * @return The root node of the UI element hierarchy
     */
    @Override
    public UiDocumentNode getUiRootNode() {
        return mUiDocRootNode;
    }

    public void setNewFileOnConfigChange(boolean state) {
        mNewFileOnConfigChange = state;
    }

    // ---- Base Class Overrides ----

    @Override
    public void dispose() {
        super.dispose();
        if (mGraphicalEditor != null) {
            mGraphicalEditor.dispose();
            mGraphicalEditor = null;
        }
    }

    /**
     * Save the XML.
     * <p/>
     * Clients must NOT call this directly. Instead they should always
     * call {@link CommonXmlEditor#doSave(IProgressMonitor)} so that th
     * editor super class can commit the data properly.
     * <p/>
     * Here we just need to tell the graphical editor that the model has
     * been saved.
     */
    @Override
    public void delegateDoSave(IProgressMonitor monitor) {
        super.delegateDoSave(monitor);
        if (mGraphicalEditor != null) {
            mGraphicalEditor.doSave(monitor);
        }
    }

    /**
     * Create the various form pages.
     */
    @Override
    public void delegateCreateFormPages() {
        try {
            // get the file being edited so that it can be passed to the layout editor.
            IFile editedFile = null;
            IEditorInput input = getEditor().getEditorInput();
            if (input instanceof FileEditorInput) {
                FileEditorInput fileInput = (FileEditorInput)input;
                editedFile = fileInput.getFile();
                if (!editedFile.isAccessible()) {
                    return;
                }
            } else {
                AdtPlugin.log(IStatus.ERROR,
                        "Input is not of type FileEditorInput: %1$s",  //$NON-NLS-1$
                        input.toString());
            }

            // It is possible that the Layout Editor already exits if a different version
            // of the same layout is being opened (either through "open" action from
            // the user, or through a configuration change in the configuration selector.)
            if (mGraphicalEditor == null) {

                // Instantiate GLE v2
                mGraphicalEditor = new GraphicalEditorPart(this);

                mGraphicalEditorIndex = getEditor().addPage(mGraphicalEditor,
                                                            getEditor().getEditorInput());
                getEditor().setPageText(mGraphicalEditorIndex, mGraphicalEditor.getTitle());

                mGraphicalEditor.openFile(editedFile);
            } else {
                if (mNewFileOnConfigChange) {
                    mGraphicalEditor.changeFileOnNewConfig(editedFile);
                    mNewFileOnConfigChange = false;
                } else {
                    mGraphicalEditor.replaceFile(editedFile);
                }
            }
        } catch (PartInitException e) {
            AdtPlugin.log(e, "Error creating nested page"); //$NON-NLS-1$
        }
    }

    @Override
    public void delegatePostCreatePages() {
        // Optional: set the default page. Eventually a default page might be
        // restored by selectDefaultPage() later based on the last page used by the user.
        // For example, to make the last page the default one (rather than the first page),
        // uncomment this line:
        //   setActivePage(getPageCount() - 1);
    }

    /* (non-java doc)
     * Change the tab/title name to include the name of the layout.
     */
    @Override
    public void delegateSetInput(IEditorInput input) {
        handleNewInput(input);
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.part.EditorPart#setInputWithNotify(org.eclipse.ui.IEditorInput)
     */
    public void delegateSetInputWithNotify(IEditorInput input) {
        handleNewInput(input);
    }

    /**
     * Called to replace the current {@link IEditorInput} with another one.
     * <p/>
     * This is used when {@link LayoutEditorMatchingStrategy} returned
     * <code>true</code> which means we're opening a different configuration of
     * the same layout.
     */
    @Override
    public void showEditorInput(IEditorInput editorInput) {
        if (getEditor().getEditorInput().equals(editorInput)) {
            return;
        }

        // Save the current editor input. This must be called on the editor itself
        // since it's the base editor that commits pending changes.
        getEditor().doSave(new NullProgressMonitor());

        // Get the current page
        int currentPage = getEditor().getActivePage();

        // Remove the pages, except for the graphical editor, which will be dynamically adapted
        // to the new model.
        // page after the graphical editor:
        int count = getEditor().getPageCount();
        for (int i = count - 1 ; i > mGraphicalEditorIndex ; i--) {
            getEditor().removePage(i);
        }
        // Pages before the graphical editor
        for (int i = mGraphicalEditorIndex - 1 ; i >= 0 ; i--) {
            getEditor().removePage(i);
        }

        // Set the current input. We're in the delegate, the input must
        // be set into the actual editor instance.
        getEditor().setInputWithNotify(editorInput);

        // Re-create or reload the pages with the default page shown as the previous active page.
        getEditor().createAndroidPages();
        getEditor().selectDefaultPage(Integer.toString(currentPage));

        // When changing an input file of an the editor, the titlebar is not refreshed to
        // show the new path/to/file being edited. So we force a refresh
        getEditor().firePropertyChange(IWorkbenchPart.PROP_TITLE);
    }

    /** Performs a complete refresh of the XML model */
    public void refreshXmlModel() {
        Document xmlDoc = mUiDocRootNode.getXmlDocument();

        delegateInitUiRootNode(true /*force*/);
        mUiDocRootNode.loadFromXmlNode(xmlDoc);

        // Update the model first, since it is used by the viewers.
        // No need to call AndroidXmlEditor.xmlModelChanged(xmlDoc) since it's
        // a no-op. Instead call onXmlModelChanged on the graphical editor.

        if (mGraphicalEditor != null) {
            mGraphicalEditor.onXmlModelChanged();
        }
    }

    /**
     * Processes the new XML Model, which XML root node is given.
     *
     * @param xml_doc The XML document, if available, or null if none exists.
     */
    @Override
    public void delegateXmlModelChanged(Document xml_doc) {
        // init the ui root on demand
        delegateInitUiRootNode(false /*force*/);

        mUiDocRootNode.loadFromXmlNode(xml_doc);

        // Update the model first, since it is used by the viewers.
        // No need to call AndroidXmlEditor.xmlModelChanged(xmlDoc) since it's
        // a no-op. Instead call onXmlModelChanged on the graphical editor.

        if (mGraphicalEditor != null) {
            mGraphicalEditor.onXmlModelChanged();
        }
    }

    /**
     * Tells the graphical editor to recompute its layout.
     */
    public void recomputeLayout() {
        mGraphicalEditor.recomputeLayout();
    }

    /**
     * Does this editor participate in the "format GUI editor changes" option?
     *
     * @return true since this editor supports automatically formatting XML
     *         affected by GUI changes
     */
    @Override
    public boolean delegateSupportsFormatOnGuiEdit() {
        return true;
    }

    /**
     * Returns one of the issues for the given node (there could be more than one)
     *
     * @param node the node to look up lint issues for
     * @return the marker for one of the issues found for the given node
     */
    @Nullable
    public IMarker getIssueForNode(@Nullable UiViewElementNode node) {
        if (node == null) {
            return null;
        }

        if (mClient != null) {
            return mClient.getIssueForNode(node);
        }

        return null;
    }

    /**
     * Returns a collection of nodes that have one or more lint warnings
     * associated with them (retrievable via
     * {@link #getIssueForNode(UiViewElementNode)})
     *
     * @return a collection of nodes, which should <b>not</b> be modified by the
     *         caller
     */
    @Nullable
    public Collection<Node> getLintNodes() {
        if (mClient != null) {
            return mClient.getIssueNodes();
        }

        return null;
    }

    @Override
    public Job delegateRunLint() {
        // We want to customize the {@link EclipseLintClient} created to run this
        // single file lint, in particular such that we can set the mode which collects
        // nodes on that lint job, such that we can quickly look up error nodes
        //Job job = super.delegateRunLint();

        Job job = null;
        IFile file = getEditor().getInputFile();
        if (file != null) {
            IssueRegistry registry = EclipseLintClient.getRegistry();
            List<IFile> resources = Collections.singletonList(file);
            mClient = new EclipseLintClient(registry,
                    resources, getEditor().getStructuredDocument(), false /*fatal*/);

            mClient.setCollectNodes(true);

            job = EclipseLintRunner.startLint(mClient, resources, file,
                    false /*show*/);
        }

        if (job != null) {
            GraphicalEditorPart graphicalEditor = getGraphicalEditor();
            if (graphicalEditor != null) {
                job.addJobChangeListener(new LintJobListener(graphicalEditor));
            }
        }
        return job;
    }

    private class LintJobListener extends JobChangeAdapter implements Runnable {
        private final GraphicalEditorPart mEditor;
        private final LayoutCanvas mCanvas;

        LintJobListener(GraphicalEditorPart editor) {
            mEditor = editor;
            mCanvas = editor.getCanvasControl();
        }

        @Override
        public void done(IJobChangeEvent event) {
            LayoutActionBar bar = mEditor.getLayoutActionBar();
            if (!bar.isDisposed()) {
                bar.updateErrorIndicator();
            }

            // Redraw
            if (!mCanvas.isDisposed()) {
                mCanvas.getDisplay().asyncExec(this);
            }
        }

        @Override
        public void run() {
            if (!mCanvas.isDisposed()) {
                mCanvas.redraw();

                OutlinePage outlinePage = mCanvas.getOutlinePage();
                if (outlinePage != null) {
                    outlinePage.refreshIcons();
                }
            }
        }
    }

    /**
     * Returns the custom IContentOutlinePage or IPropertySheetPage when asked for it.
     */
    @Override
    public Object delegateGetAdapter(Class<?> adapter) {
        if (adapter == IContentOutlinePage.class) {
            // Somebody has requested the outline. Eclipse can only have a single outline page,
            // even for a multi-part editor:
            //       https://bugs.eclipse.org/bugs/show_bug.cgi?id=1917
            // To work around this we use PDE's workaround of having a single multiplexing
            // outline which switches its contents between the outline pages we register
            // for it, and then on page switch we notify it to update itself.

            // There is one complication: The XML editor outline listens for the editor
            // selection and uses this to automatically expand its tree children and show
            // the current node containing the caret as selected. Unfortunately, this
            // listener code contains this:
            //
            //     /* Bug 136310, unless this page is that part's
            //      * IContentOutlinePage, ignore the selection change */
            //     if (part.getAdapter(IContentOutlinePage.class) == this) {
            //
            // This means that when we return the multiplexing outline from this getAdapter
            // method, the outline no longer updates to track the selection.
            // To work around this, we use the following hack^H^H^H^H technique:
            // - Add a selection listener *before* requesting the editor outline, such
            //   that the selection listener is told about the impending selection event
            //   right before the editor outline hears about it. Set the flag
            //   mCheckOutlineAdapter to true. (We also only set it if the editor view
            //   itself is active.)
            // - In this getAdapter method, when somebody requests the IContentOutline.class,
            //   see if mCheckOutlineAdapter to see if this request is *likely* coming
            //   from the XML editor outline. If so, make sure it is by actually looking
            //   at the signature of the caller. If it's the editor outline, then return
            //   the editor outline instance itself rather than the multiplexing outline.
            if (mCheckOutlineAdapter && mEditorOutline != null) {
                mCheckOutlineAdapter = false;
                // Make *sure* this is really the editor outline calling in case
                // future versions of Eclipse changes the sequencing or dispatch of selection
                // events:
                StackTraceElement[] frames = new Throwable().fillInStackTrace().getStackTrace();
                if (frames.length > 2) {
                    StackTraceElement frame = frames[2];
                    if (frame.getClassName().equals(
                            "org.eclipse.wst.sse.ui.internal.contentoutline." + //$NON-NLS-1$
                            "ConfigurableContentOutlinePage$PostSelectionServiceListener")) { //$NON-NLS-1$
                        return mEditorOutline;
                    }
                }
            }

            // Use a multiplexing outline: workaround for
            // https://bugs.eclipse.org/bugs/show_bug.cgi?id=1917
            if (mMultiOutline == null || mMultiOutline.isDisposed()) {
                mMultiOutline = new XmlEditorMultiOutline();
                mMultiOutline.addSelectionChangedListener(new ISelectionChangedListener() {
                    @Override
                    public void selectionChanged(SelectionChangedEvent event) {
                        ISelection selection = event.getSelection();
                        getEditor().getSite().getSelectionProvider().setSelection(selection);
                        if (getEditor().getIgnoreXmlUpdate()) {
                            return;
                        }
                        SelectionManager manager =
                                mGraphicalEditor.getCanvasControl().getSelectionManager();
                        manager.setSelection(selection);
                    }
                });
                updateOutline(getEditor().getActivePageInstance());
            }

            return mMultiOutline;
        }

        if (IPropertySheetPage.class == adapter && mGraphicalEditor != null) {
            if (mPropertyPage == null) {
                mPropertyPage = new PropertySheetPage(mGraphicalEditor);
            }

            return mPropertyPage;
        }

        // return default
        return super.delegateGetAdapter(adapter);
    }

    /**
     * Update the contents of the outline to show either the XML editor outline
     * or the layout editor graphical outline depending on which tab is visible
     */
    private void updateOutline(IFormPage page) {
        if (mMultiOutline == null) {
            return;
        }

        IContentOutlinePage outline;
        CommonXmlEditor editor = getEditor();
        if (!editor.isEditorPageActive()) {
            outline = getGraphicalOutline();
        } else {
            // Use plain XML editor outline instead
            if (mEditorOutline == null) {
                StructuredTextEditor structuredTextEditor = editor.getStructuredTextEditor();
                if (structuredTextEditor != null) {
                    IWorkbenchWindow window = editor.getSite().getWorkbenchWindow();
                    ISelectionService service = window.getSelectionService();
                    service.addPostSelectionListener(new ISelectionListener() {
                        @Override
                        public void selectionChanged(IWorkbenchPart part, ISelection selection) {
                            if (getEditor().isEditorPageActive()) {
                                mCheckOutlineAdapter = true;
                            }
                        }
                    });

                    mEditorOutline = (IContentOutlinePage) structuredTextEditor.getAdapter(
                            IContentOutlinePage.class);
                }
            }

            outline = mEditorOutline;
        }

        mMultiOutline.setPageActive(outline);
    }

    /**
     * Returns the graphical outline associated with the layout editor
     *
     * @return the outline page, never null
     */
    @NonNull
    public OutlinePage getGraphicalOutline() {
        if (mLayoutOutline == null) {
            mLayoutOutline = new OutlinePage(mGraphicalEditor);
        }

        return mLayoutOutline;
    }

    @Override
    public void delegatePageChange(int newPageIndex) {
        if (getEditor().getCurrentPage() == getEditor().getTextPageIndex() &&
                newPageIndex == mGraphicalEditorIndex) {
            // You're switching from the XML editor to the WYSIWYG editor;
            // look at the caret position and figure out which node it corresponds to
            // (if any) and if found, select the corresponding visual element.
            ISourceViewer textViewer = getEditor().getStructuredSourceViewer();
            int caretOffset = textViewer.getTextWidget().getCaretOffset();
            if (caretOffset >= 0) {
                Node node = DomUtilities.getNode(textViewer.getDocument(), caretOffset);
                if (node != null && mGraphicalEditor != null) {
                    mGraphicalEditor.select(node);
                }
            }
        }

        super.delegatePageChange(newPageIndex);

        if (mGraphicalEditor != null) {
            if (newPageIndex == mGraphicalEditorIndex) {
                mGraphicalEditor.activated();
            } else {
                mGraphicalEditor.deactivated();
            }
        }
    }

    @Override
    public int delegateGetPersistenceCategory() {
        return AndroidXmlEditor.CATEGORY_LAYOUT;
    }

    @Override
    public void delegatePostPageChange(int newPageIndex) {
        super.delegatePostPageChange(newPageIndex);

        if (mGraphicalEditor != null) {
            LayoutCanvas canvas = mGraphicalEditor.getCanvasControl();
            if (canvas != null) {
                IActionBars bars = getEditor().getEditorSite().getActionBars();
                if (bars != null) {
                    canvas.updateGlobalActions(bars);
                }
            }
        }

        IFormPage page = getEditor().getActivePageInstance();
        updateOutline(page);
    }

    @Override
    public IFormPage delegatePostSetActivePage(IFormPage superReturned, String pageIndex) {
        IFormPage page = superReturned;
        if (page != null) {
            updateOutline(page);
        }

        return page;
    }

    // ----- IActionContributorDelegate methods ----

    @Override
    public void setActiveEditor(IEditorPart part, IActionBars bars) {
        if (mGraphicalEditor != null) {
            LayoutCanvas canvas = mGraphicalEditor.getCanvasControl();
            if (canvas != null) {
                canvas.updateGlobalActions(bars);
            }
        }
    }


    @Override
    public void delegateActivated() {
        if (mGraphicalEditor != null) {
            if (getEditor().getActivePage() == mGraphicalEditorIndex) {
                mGraphicalEditor.activated();
            } else {
                mGraphicalEditor.deactivated();
            }
        }
    }

    @Override
    public void delegateDeactivated() {
        if (mGraphicalEditor != null && getEditor().getActivePage() == mGraphicalEditorIndex) {
            mGraphicalEditor.deactivated();
        }
    }

    @Override
    public String delegateGetPartName() {
        IEditorInput editorInput = getEditor().getEditorInput();
        if (!AdtPrefs.getPrefs().isSharedLayoutEditor()
              && editorInput instanceof IFileEditorInput) {
            IFileEditorInput fileInput = (IFileEditorInput) editorInput;
            IFile file = fileInput.getFile();
            IContainer parent = file.getParent();
            if (parent != null) {
                String parentName = parent.getName();
                if  (parentName.startsWith(LAYOUT_FOLDER_PREFIX)) {
                    parentName = parentName.substring(LAYOUT_FOLDER_PREFIX.length());
                    return parentName + File.separatorChar + file.getName();
                }
            }
        }

        return super.delegateGetPartName();
    }

    // ---- Local Methods ----

    /**
     * Returns true if the Graphics editor page is visible. This <b>must</b> be
     * called from the UI thread.
     */
    public boolean isGraphicalEditorActive() {
        IWorkbenchPartSite workbenchSite = getEditor().getSite();
        IWorkbenchPage workbenchPage = workbenchSite.getPage();

        // check if the editor is visible in the workbench page
        if (workbenchPage.isPartVisible(getEditor())
                && workbenchPage.getActiveEditor() == getEditor()) {
            // and then if the page of the editor is visible (not to be confused with
            // the workbench page)
            return mGraphicalEditorIndex == getEditor().getActivePage();
        }

        return false;
    }

    @Override
    public void delegateInitUiRootNode(boolean force) {
        // The root UI node is always created, even if there's no corresponding XML node.
        if (mUiDocRootNode == null || force) {
            // get the target data from the opened file (and its project)
            AndroidTargetData data = getEditor().getTargetData();

            Document doc = null;
            if (mUiDocRootNode != null) {
                doc = mUiDocRootNode.getXmlDocument();
            }

            DocumentDescriptor desc;
            if (data == null) {
                desc = new DocumentDescriptor("temp", null /*children*/);
            } else {
                desc = data.getLayoutDescriptors().getDescriptor();
            }

            // get the descriptors from the data.
            mUiDocRootNode = (UiDocumentNode) desc.createUiNode();
            super.setUiRootNode(mUiDocRootNode);
            mUiDocRootNode.setEditor(getEditor());

            mUiDocRootNode.setUnknownDescriptorProvider(new IUnknownDescriptorProvider() {
                @Override
                public ElementDescriptor getDescriptor(String xmlLocalName) {
                    ElementDescriptor unknown = mUnknownDescriptorMap.get(xmlLocalName);
                    if (unknown == null) {
                        unknown = createUnknownDescriptor(xmlLocalName);
                        mUnknownDescriptorMap.put(xmlLocalName, unknown);
                    }

                    return unknown;
                }
            });

            onDescriptorsChanged(doc);
        }
    }

    /**
     * Creates a new {@link ViewElementDescriptor} for an unknown XML local name
     * (i.e. one that was not mapped by the current descriptors).
     * <p/>
     * Since we deal with layouts, we returns either a descriptor for a custom view
     * or one for the base View.
     *
     * @param xmlLocalName The XML local name to match.
     * @return A non-null {@link ViewElementDescriptor}.
     */
    private ViewElementDescriptor createUnknownDescriptor(String xmlLocalName) {
        ViewElementDescriptor desc = null;
        IEditorInput editorInput = getEditor().getEditorInput();
        if (editorInput instanceof IFileEditorInput) {
            IFileEditorInput fileInput = (IFileEditorInput)editorInput;
            IProject project = fileInput.getFile().getProject();

            // Check if we can find a custom view specific to this project.
            // This only works if there's an actual matching custom class in the project.
            if (xmlLocalName.indexOf('.') != -1) {
                desc = CustomViewDescriptorService.getInstance().getDescriptor(project,
                        xmlLocalName);
            }

            if (desc == null) {
                // If we didn't find a custom view, create a synthetic one using the
                // the base View descriptor as a model.
                // This is a layout after all, so every XML node should represent
                // a view.

                Sdk currentSdk = Sdk.getCurrent();
                if (currentSdk != null) {
                    IAndroidTarget target = currentSdk.getTarget(project);
                    if (target != null) {
                        AndroidTargetData data = currentSdk.getTargetData(target);
                        if (data != null) {
                            // data can be null when the target is still loading
                            ViewElementDescriptor viewDesc =
                                data.getLayoutDescriptors().getBaseViewDescriptor();

                            desc = new ViewElementDescriptor(
                                    xmlLocalName, // xml local name
                                    xmlLocalName, // ui_name
                                    xmlLocalName, // canonical class name
                                    null, // tooltip
                                    null, // sdk_url
                                    viewDesc.getAttributes(),
                                    viewDesc.getLayoutAttributes(),
                                    null, // children
                                    false /* mandatory */);
                            desc.setSuperClass(viewDesc);
                        }
                    }
                }
            }
        }

        if (desc == null) {
            // We can only arrive here if the SDK's android target has not finished
            // loading. Just create a dummy descriptor with no attributes to be able
            // to continue.
            desc = new ViewElementDescriptor(xmlLocalName, xmlLocalName);
        }
        return desc;
    }

    private void onDescriptorsChanged(Document document) {

        mUnknownDescriptorMap.clear();

        if (document != null) {
            mUiDocRootNode.loadFromXmlNode(document);
        } else {
            mUiDocRootNode.reloadFromXmlNode(mUiDocRootNode.getXmlDocument());
        }

        if (mGraphicalEditor != null) {
            mGraphicalEditor.onTargetChange();
            mGraphicalEditor.reloadPalette();
            mGraphicalEditor.getCanvasControl().syncPreviewMode();
        }
    }

    /**
     * Handles a new input, and update the part name.
     * @param input the new input.
     */
    private void handleNewInput(IEditorInput input) {
        if (input instanceof FileEditorInput) {
            FileEditorInput fileInput = (FileEditorInput) input;
            IFile file = fileInput.getFile();
            getEditor().setPartName(String.format("%1$s", file.getName()));
        }
    }

    /**
     * Helper method that returns a {@link ViewElementDescriptor} for the requested FQCN.
     * Will return null if we can't find that FQCN or we lack the editor/data/descriptors info.
     */
    public ViewElementDescriptor getFqcnViewDescriptor(String fqcn) {
        ViewElementDescriptor desc = null;

        AndroidTargetData data = getEditor().getTargetData();
        if (data != null) {
            LayoutDescriptors layoutDesc = data.getLayoutDescriptors();
            if (layoutDesc != null) {
                DocumentDescriptor docDesc = layoutDesc.getDescriptor();
                if (docDesc != null) {
                    desc = internalFindFqcnViewDescriptor(fqcn, docDesc.getChildren(), null);
                }
            }
        }

        if (desc == null) {
            // We failed to find a descriptor for the given FQCN.
            // Let's consider custom classes and create one as needed.
            desc = createUnknownDescriptor(fqcn);
        }

        return desc;
    }

    /**
     * Internal helper to recursively search for a {@link ViewElementDescriptor} that matches
     * the requested FQCN.
     *
     * @param fqcn The target View FQCN to find.
     * @param descriptors A list of children descriptors to iterate through.
     * @param visited A set we use to remember which descriptors have already been visited,
     *  necessary since the view descriptor hierarchy is cyclic.
     * @return Either a matching {@link ViewElementDescriptor} or null.
     */
    private ViewElementDescriptor internalFindFqcnViewDescriptor(String fqcn,
            ElementDescriptor[] descriptors,
            Set<ElementDescriptor> visited) {
        if (visited == null) {
            visited = new HashSet<ElementDescriptor>();
        }

        if (descriptors != null) {
            for (ElementDescriptor desc : descriptors) {
                if (visited.add(desc)) {
                    // Set.add() returns true if this a new element that was added to the set.
                    // That means we haven't visited this descriptor yet.
                    // We want a ViewElementDescriptor with a matching FQCN.
                    if (desc instanceof ViewElementDescriptor &&
                            fqcn.equals(((ViewElementDescriptor) desc).getFullClassName())) {
                        return (ViewElementDescriptor) desc;
                    }

                    // Visit its children
                    ViewElementDescriptor vd =
                        internalFindFqcnViewDescriptor(fqcn, desc.getChildren(), visited);
                    if (vd != null) {
                        return vd;
                    }
                }
            }
        }

        return null;
    }
}

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

package org.eclipse.andmore.internal.editors;

import static org.eclipse.wst.sse.ui.internal.actions.StructuredTextEditorActionConstants.ACTION_NAME_FORMAT_DOCUMENT;

import com.android.annotations.Nullable;
import com.android.sdklib.IAndroidTarget;

import org.eclipse.andmore.AndmoreAndroidConstants;
import org.eclipse.andmore.AndmoreAndroidPlugin;
import org.eclipse.andmore.AdtUtils;
import org.eclipse.andmore.internal.editors.uimodel.UiElementNode;
import org.eclipse.andmore.internal.lint.EclipseLintRunner;
import org.eclipse.andmore.internal.preferences.AdtPrefs;
import org.eclipse.andmore.internal.refactorings.core.RenameResourceXmlTextAction;
import org.eclipse.andmore.internal.sdk.AndroidTargetData;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.andmore.internal.sdk.Sdk.ITargetChangeListener;
import org.eclipse.andmore.internal.sdk.Sdk.TargetChangeListener;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.ide.IDEActionFactory;
import org.eclipse.ui.ide.IGotoMarker;
import org.eclipse.ui.internal.browser.WorkbenchBrowserSupport;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.part.WorkbenchPart;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IModelStateListener;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.ui.StructuredTextEditor;
import org.eclipse.wst.sse.ui.internal.StructuredTextViewer;
import org.eclipse.wst.xml.core.internal.document.NodeContainer;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;

/**
 * Multi-page form editor for Android XML files.
 * <p/>
 * It is designed to work with a {@link StructuredTextEditor} that will display an XML file.
 * <br/>
 * Derived classes must implement createFormPages to create the forms before the
 * source editor. This can be a no-op if desired.
 */
@SuppressWarnings("restriction") // Uses XML model, which has no non-restricted replacement yet
public abstract class AndroidXmlEditor extends FormEditor {

    /** Icon used for the XML source page. */
    public static final String ICON_XML_PAGE = "editor_page_source"; //$NON-NLS-1$

    /** Preference name for the current page of this file */
    private static final String PREF_CURRENT_PAGE = "_current_page"; //$NON-NLS-1$

    /** Id string used to create the Android SDK browser */
    private static String BROWSER_ID = "android"; //$NON-NLS-1$

    /** Page id of the XML source editor, used for switching tabs programmatically */
    public final static String TEXT_EDITOR_ID = "editor_part"; //$NON-NLS-1$

    /** Width hint for text fields. Helps the grid layout resize properly on smaller screens */
    public static final int TEXT_WIDTH_HINT = 50;

    /** Page index of the text editor (always the last page) */
    protected int mTextPageIndex;
    /** The text editor */
    private StructuredTextEditor mTextEditor;
    /** Listener for the XML model from the StructuredEditor */
    private XmlModelStateListener mXmlModelStateListener;
    /** Listener to update the root node if the target of the file is changed because of a
     * SDK location change or a project target change */
    private TargetChangeListener mTargetListener = null;

    /** flag set during page creation */
    private boolean mIsCreatingPage = false;

    /**
     * Flag used to ignore XML model updates. For example, the flag is set during
     * formatting. A format operation should completely preserve the semantics of the XML
     * so the document listeners can use this flag to skip updating the model when edits
     * are observed during a formatting operation
     */
    private boolean mIgnoreXmlUpdate;

    /**
     * Flag indicating we're inside {@link #wrapEditXmlModel(Runnable)}.
     * This is a counter, which allows us to nest the edit XML calls.
     * There is no pending operation when the counter is at zero.
     */
    private int mIsEditXmlModelPending;

    /**
     * Usually null, but during an editing operation, represents the highest
     * node which should be formatted when the editing operation is complete.
     */
    private UiElementNode mFormatNode;

    /**
     * Whether {@link #mFormatNode} should be formatted recursively, or just
     * the node itself (its arguments)
     */
    private boolean mFormatChildren;

    /**
     * Creates a form editor.
     * <p/>
     * Some derived classes will want to use {@link #addDefaultTargetListener()}
     * to setup the default listener to monitor SDK target changes. This
     * is no longer the default.
     */
    public AndroidXmlEditor() {
        super();
    }

    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        super.init(site, input);
        // Trigger a check to see if the SDK needs to be reloaded (which will
        // invoke onSdkLoaded or ITargetChangeListener asynchronously as needed).
        AndmoreAndroidPlugin.getDefault().refreshSdk();
    }

    /**
     * Setups a default {@link ITargetChangeListener} that will call
     * {@link #initUiRootNode(boolean)} when the SDK or the target changes.
     */
    public void addDefaultTargetListener() {
        if (mTargetListener == null) {
            mTargetListener = new TargetChangeListener() {
                @Override
                public IProject getProject() {
                    return AndroidXmlEditor.this.getProject();
                }

                @Override
                public void reload() {
                    commitPages(false /* onSave */);

                    // recreate the ui root node always
                    initUiRootNode(true /*force*/);
                }
            };
            AndmoreAndroidPlugin.getDefault().addTargetListener(mTargetListener);
        }
    }

    // ---- Abstract Methods ----

    /**
     * Returns the root node of the UI element hierarchy manipulated by the current
     * UI node editor.
     */
    abstract public UiElementNode getUiRootNode();

    /**
     * Creates the various form pages.
     * <p/>
     * Derived classes must implement this to add their own specific tabs.
     */
    abstract protected void createFormPages();

    /**
     * Called by the base class {@link AndroidXmlEditor} once all pages (custom form pages
     * as well as text editor page) have been created. This give a chance to deriving
     * classes to adjust behavior once the text page has been created.
     */
    protected void postCreatePages() {
        // Nothing in the base class.
    }

    /**
     * Creates the initial UI Root Node, including the known mandatory elements.
     * @param force if true, a new UiManifestNode is recreated even if it already exists.
     */
    abstract protected void initUiRootNode(boolean force);

    /**
     * Subclasses should override this method to process the new XML Model, which XML
     * root node is given.
     *
     * The base implementation is empty.
     *
     * @param xml_doc The XML document, if available, or null if none exists.
     */
    abstract protected void xmlModelChanged(Document xml_doc);

    /**
     * Controls whether XML models are ignored or not.
     *
     * @param ignore when true, ignore all subsequent XML model updates, when false start
     *            processing XML model updates again
     */
    public void setIgnoreXmlUpdate(boolean ignore) {
        mIgnoreXmlUpdate = ignore;
    }

    /**
     * Returns whether XML model events are ignored or not. This is the case
     * when we are deliberately modifying the document in a way which does not
     * change the semantics (such as formatting), or when we have already
     * directly updated the model ourselves.
     *
     * @return true if XML events should be ignored
     */
    public boolean getIgnoreXmlUpdate() {
        return mIgnoreXmlUpdate;
    }

    // ---- Base Class Overrides, Interfaces Implemented ----

    @Override
    public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
        Object result = super.getAdapter(adapter);

        if (result != null && adapter.equals(IGotoMarker.class) ) {
            final IGotoMarker gotoMarker = (IGotoMarker) result;
            return new IGotoMarker() {
                @Override
                public void gotoMarker(IMarker marker) {
                    gotoMarker.gotoMarker(marker);
                    try {
                        // Lint markers should always jump to XML text
                        if (marker.getType().equals(AndmoreAndroidConstants.MARKER_LINT)) {
                            IEditorPart editor = AdtUtils.getActiveEditor();
                            if (editor instanceof AndroidXmlEditor) {
                                AndroidXmlEditor xmlEditor = (AndroidXmlEditor) editor;
                                xmlEditor.setActivePage(AndroidXmlEditor.TEXT_EDITOR_ID);
                            }
                        }
                    } catch (CoreException e) {
                        AndmoreAndroidPlugin.log(e, null);
                    }
                }
            };
        }

        if (result == null && adapter == IContentOutlinePage.class) {
            return getStructuredTextEditor().getAdapter(adapter);
        }

        return result;
    }

    /**
     * Creates the pages of the multi-page editor.
     */
    @Override
    protected void addPages() {
        createAndroidPages();
        selectDefaultPage(null /* defaultPageId */);
    }

    /**
     * Creates the page for the Android Editors
     */
    public void createAndroidPages() {
        mIsCreatingPage = true;
        createFormPages();
        createTextEditor();
        updateActionBindings();
        postCreatePages();
        mIsCreatingPage = false;
    }

    /**
     * Returns whether the editor is currently creating its pages.
     */
    public boolean isCreatingPages() {
        return mIsCreatingPage;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * If the page is an instance of {@link IPageImageProvider}, the image returned by
     * by {@link IPageImageProvider#getPageImage()} will be set on the page's tab.
     */
    @Override
    public int addPage(IFormPage page) throws PartInitException {
        int index = super.addPage(page);
        if (page instanceof IPageImageProvider) {
            setPageImage(index, ((IPageImageProvider) page).getPageImage());
        }
        return index;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * If the editor is an instance of {@link IPageImageProvider}, the image returned by
     * by {@link IPageImageProvider#getPageImage()} will be set on the page's tab.
     */
    @Override
    public int addPage(IEditorPart editor, IEditorInput input) throws PartInitException {
        int index = super.addPage(editor, input);
        if (editor instanceof IPageImageProvider) {
            setPageImage(index, ((IPageImageProvider) editor).getPageImage());
        }
        return index;
    }

    /**
     * Creates undo redo (etc) actions for the editor site (so that it works for any page of this
     * multi-page editor) by re-using the actions defined by the {@link StructuredTextEditor}
     * (aka the XML text editor.)
     */
    protected void updateActionBindings() {
        IActionBars bars = getEditorSite().getActionBars();
        if (bars != null) {
            IAction action = mTextEditor.getAction(ActionFactory.UNDO.getId());
            bars.setGlobalActionHandler(ActionFactory.UNDO.getId(), action);

            action = mTextEditor.getAction(ActionFactory.REDO.getId());
            bars.setGlobalActionHandler(ActionFactory.REDO.getId(), action);

            bars.setGlobalActionHandler(ActionFactory.DELETE.getId(),
                    mTextEditor.getAction(ActionFactory.DELETE.getId()));
            bars.setGlobalActionHandler(ActionFactory.CUT.getId(),
                    mTextEditor.getAction(ActionFactory.CUT.getId()));
            bars.setGlobalActionHandler(ActionFactory.COPY.getId(),
                    mTextEditor.getAction(ActionFactory.COPY.getId()));
            bars.setGlobalActionHandler(ActionFactory.PASTE.getId(),
                    mTextEditor.getAction(ActionFactory.PASTE.getId()));
            bars.setGlobalActionHandler(ActionFactory.SELECT_ALL.getId(),
                    mTextEditor.getAction(ActionFactory.SELECT_ALL.getId()));
            bars.setGlobalActionHandler(ActionFactory.FIND.getId(),
                    mTextEditor.getAction(ActionFactory.FIND.getId()));
            bars.setGlobalActionHandler(IDEActionFactory.BOOKMARK.getId(),
                    mTextEditor.getAction(IDEActionFactory.BOOKMARK.getId()));

            bars.updateActionBars();
        }
    }

    /**
     * Clears the action bindings for the editor site.
     */
    protected void clearActionBindings(boolean includeUndoRedo) {
        IActionBars bars = getEditorSite().getActionBars();
        if (bars != null) {
            // For some reason, undo/redo doesn't seem to work in the form editor.
            // This appears to be the case for pure Eclipse form editors too, e.g. see
            //      https://bugs.eclipse.org/bugs/show_bug.cgi?id=68423
            // However, as a workaround we can use the *text* editor's underlying undo
            // to revert operations being done in the UI, and the form automatically updates.
            // Therefore, to work around this, we simply leave the text editor bindings
            // in place if {@code includeUndoRedo} is not set
            if (includeUndoRedo) {
                bars.setGlobalActionHandler(ActionFactory.UNDO.getId(), null);
                bars.setGlobalActionHandler(ActionFactory.REDO.getId(), null);
            }
            bars.setGlobalActionHandler(ActionFactory.DELETE.getId(), null);
            bars.setGlobalActionHandler(ActionFactory.CUT.getId(), null);
            bars.setGlobalActionHandler(ActionFactory.COPY.getId(), null);
            bars.setGlobalActionHandler(ActionFactory.PASTE.getId(), null);
            bars.setGlobalActionHandler(ActionFactory.SELECT_ALL.getId(), null);
            bars.setGlobalActionHandler(ActionFactory.FIND.getId(), null);
            bars.setGlobalActionHandler(IDEActionFactory.BOOKMARK.getId(), null);

            bars.updateActionBars();
        }
    }

    /**
     * Selects the default active page.
     * @param defaultPageId the id of the page to show. If <code>null</code> the editor attempts to
     * find the default page in the properties of the {@link IResource} object being edited.
     */
    public void selectDefaultPage(String defaultPageId) {
        if (defaultPageId == null) {
            IFile file = getInputFile();
            if (file != null) {
                QualifiedName qname = new QualifiedName(AndmoreAndroidPlugin.PLUGIN_ID,
                        getClass().getSimpleName() + PREF_CURRENT_PAGE);
                String pageId;
                try {
                    pageId = file.getPersistentProperty(qname);
                    if (pageId != null) {
                        defaultPageId = pageId;
                    }
                } catch (CoreException e) {
                    // ignored
                }
            }
        }

        if (defaultPageId != null) {
            try {
                setActivePage(Integer.parseInt(defaultPageId));
            } catch (Exception e) {
                // We can get NumberFormatException from parseInt but also
                // AssertionError from setActivePage when the index is out of bounds.
                // Generally speaking we just want to ignore any exception and fall back on the
                // first page rather than crash the editor load. Logging the error is enough.
                AndmoreAndroidPlugin.log(e, "Selecting page '%s' in AndroidXmlEditor failed", defaultPageId);
            }
        } else if (AdtPrefs.getPrefs().isXmlEditorPreferred(getPersistenceCategory())) {
            setActivePage(mTextPageIndex);
        }
    }

    /** The layout editor */
    public static final int CATEGORY_LAYOUT   = 1 << 0;
    /** The manifest editor */
    public static final int CATEGORY_MANIFEST = 1 << 1;
    /** Any other XML editor */
    public static final int CATEGORY_OTHER    = 1 << 2;

    /**
     * Returns the persistence category to use for this editor; this should be
     * one of the {@code CATEGORY_} constants such as {@link #CATEGORY_MANIFEST},
     * {@link #CATEGORY_LAYOUT}, {@link #CATEGORY_OTHER}, ...
     * <p>
     * The persistence category is used to group editors together when it comes
     * to certain types of persistence metadata. For example, whether this type
     * of file was most recently edited graphically or with an XML text editor.
     * We'll open new files in the same text or graphical mode as the last time
     * the user edited a file of the same persistence category.
     * <p>
     * Before we added the persistence category, we had a single boolean flag
     * recording whether the XML files were most recently edited graphically or
     * not. However, this meant that users can't for example prefer to edit
     * Manifest files graphically and string files via XML. By splitting the
     * editors up into categories, we can track the mode at a finer granularity,
     * and still allow similar editors such as those used for animations and
     * colors to be treated the same way.
     *
     * @return the persistence category constant
     */
    protected int getPersistenceCategory() {
        return CATEGORY_OTHER;
    }

    /**
     * Removes all the pages from the editor.
     */
    protected void removePages() {
        int count = getPageCount();
        for (int i = count - 1 ; i >= 0 ; i--) {
            removePage(i);
        }
    }

    /**
     * Overrides the parent's setActivePage to be able to switch to the xml editor.
     *
     * If the special pageId TEXT_EDITOR_ID is given, switches to the mTextPageIndex page.
     * This is needed because the editor doesn't actually derive from IFormPage and thus
     * doesn't have the get-by-page-id method. In this case, the method returns null since
     * IEditorPart does not implement IFormPage.
     */
    @Override
    public IFormPage setActivePage(String pageId) {
        if (pageId.equals(TEXT_EDITOR_ID)) {
            super.setActivePage(mTextPageIndex);
            return null;
        } else {
            return super.setActivePage(pageId);
        }
    }

    /**
     * Notifies this multi-page editor that the page with the given id has been
     * activated. This method is called when the user selects a different tab.
     *
     * @see MultiPageEditorPart#pageChange(int)
     */
    @Override
    protected void pageChange(int newPageIndex) {
        super.pageChange(newPageIndex);

        // Do not record page changes during creation of pages
        if (mIsCreatingPage) {
            return;
        }

        IFile file = getInputFile();
        if (file != null) {
            QualifiedName qname = new QualifiedName(AndmoreAndroidPlugin.PLUGIN_ID,
                    getClass().getSimpleName() + PREF_CURRENT_PAGE);
            try {
                file.setPersistentProperty(qname, Integer.toString(newPageIndex));
            } catch (CoreException e) {
                // ignore
            }
        }

        boolean isTextPage = newPageIndex == mTextPageIndex;
        AdtPrefs.getPrefs().setXmlEditorPreferred(getPersistenceCategory(), isTextPage);
    }

    /**
     * Returns true if the active page is the editor page
     *
     * @return true if the active page is the editor page
     */
    public boolean isEditorPageActive() {
        return getActivePage() == mTextPageIndex;
    }

    /**
     * Returns the {@link IFile} matching the editor's input or null.
     */
    @Nullable
    public IFile getInputFile() {
        IEditorInput input = getEditorInput();
        if (input instanceof IFileEditorInput) {
            return ((IFileEditorInput) input).getFile();
        }
        return null;
    }

    /**
     * Removes attached listeners.
     *
     * @see WorkbenchPart
     */
    @Override
    public void dispose() {
        IStructuredModel xml_model = getModelForRead();
        if (xml_model != null) {
            try {
                if (mXmlModelStateListener != null) {
                    xml_model.removeModelStateListener(mXmlModelStateListener);
                }

            } finally {
                xml_model.releaseFromRead();
            }
        }

        if (mTargetListener != null) {
            AndmoreAndroidPlugin.getDefault().removeTargetListener(mTargetListener);
            mTargetListener = null;
        }

        super.dispose();
    }

    /**
     * Commit all dirty pages then saves the contents of the text editor.
     * <p/>
     * This works by committing all data to the XML model and then
     * asking the Structured XML Editor to save the XML.
     *
     * @see IEditorPart
     */
    @Override
    public void doSave(IProgressMonitor monitor) {
        commitPages(true /* onSave */);

        if (AdtPrefs.getPrefs().isFormatOnSave()) {
            IAction action = mTextEditor.getAction(ACTION_NAME_FORMAT_DOCUMENT);
            if (action != null) {
                try {
                    mIgnoreXmlUpdate = true;
                    action.run();
                } finally {
                    mIgnoreXmlUpdate = false;
                }
            }
        }

        // The actual "save" operation is done by the Structured XML Editor
        getEditor(mTextPageIndex).doSave(monitor);

        // Check for errors on save, if enabled
        if (AdtPrefs.getPrefs().isLintOnSave()) {
            runLint();
        }
    }

    /**
     * Tells the editor to start a Lint check.
     * It's up to the caller to check whether this should be done depending on preferences.
     * <p/>
     * The default implementation is to call {@link #startLintJob()}.
     *
     * @return The Job started by {@link EclipseLintRunner} or null if no job was started.
     */
    protected Job runLint() {
        return startLintJob();
    }

    /**
     * Utility method that creates a Job to run Lint on the current document.
     * Does not wait for the job to finish - just returns immediately.
     *
     * @return a new job, or null
     * @see EclipseLintRunner#startLint(java.util.List, IResource, IDocument,
     *      boolean, boolean)
     */
    @Nullable
    public Job startLintJob() {
        IFile file = getInputFile();
        if (file != null) {
            return EclipseLintRunner.startLint(Collections.singletonList(file), file,
                    getStructuredDocument(), false /*fatalOnly*/, false /*show*/);
        }

        return null;
    }

    /* (non-Javadoc)
     * Saves the contents of this editor to another object.
     * <p>
     * Subclasses must override this method to implement the open-save-close lifecycle
     * for an editor.  For greater details, see <code>IEditorPart</code>
     * </p>
     *
     * @see IEditorPart
     */
    @Override
    public void doSaveAs() {
        commitPages(true /* onSave */);

        IEditorPart editor = getEditor(mTextPageIndex);
        editor.doSaveAs();
        setPageText(mTextPageIndex, editor.getTitle());
        setInput(editor.getEditorInput());
    }

    /**
     * Commits all dirty pages in the editor. This method should
     * be called as a first step of a 'save' operation.
     * <p/>
     * This is the same implementation as in {@link FormEditor}
     * except it fixes two bugs: a cast to IFormPage is done
     * from page.get(i) <em>before</em> being tested with instanceof.
     * Another bug is that the last page might be a null pointer.
     * <p/>
     * The incorrect casting makes the original implementation crash due
     * to our {@link StructuredTextEditor} not being an {@link IFormPage}
     * so we have to override and duplicate to fix it.
     *
     * @param onSave <code>true</code> if commit is performed as part
     * of the 'save' operation, <code>false</code> otherwise.
     * @since 3.3
     */
    @Override
    public void commitPages(boolean onSave) {
        if (pages != null) {
            for (int i = 0; i < pages.size(); i++) {
                Object page = pages.get(i);
                if (page != null && page instanceof IFormPage) {
                    IFormPage form_page = (IFormPage) page;
                    IManagedForm managed_form = form_page.getManagedForm();
                    if (managed_form != null && managed_form.isDirty()) {
                        managed_form.commit(onSave);
                    }
                }
            }
        }
    }

    /* (non-Javadoc)
     * Returns whether the "save as" operation is supported by this editor.
     * <p>
     * Subclasses must override this method to implement the open-save-close lifecycle
     * for an editor.  For greater details, see <code>IEditorPart</code>
     * </p>
     *
     * @see IEditorPart
     */
    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    /**
     * Returns the page index of the text editor (always the last page)

     * @return the page index of the text editor (always the last page)
     */
    public int getTextPageIndex() {
        return mTextPageIndex;
    }

    // ---- Local methods ----


    /**
     * Helper method that creates a new hyper-link Listener.
     * Used by derived classes which need active links in {@link FormText}.
     * <p/>
     * This link listener handles two kinds of URLs:
     * <ul>
     * <li> Links starting with "http" are simply sent to a local browser.
     * <li> Links starting with "file:/" are simply sent to a local browser.
     * <li> Links starting with "page:" are expected to be an editor page id to switch to.
     * <li> Other links are ignored.
     * </ul>
     *
     * @return A new hyper-link listener for FormText to use.
     */
    public final IHyperlinkListener createHyperlinkListener() {
        return new HyperlinkAdapter() {
            /**
             * Switch to the page corresponding to the link that has just been clicked.
             * For this purpose, the HREF of the &lt;a&gt; tags above is the page ID to switch to.
             */
            @Override
            public void linkActivated(HyperlinkEvent e) {
                super.linkActivated(e);
                String link = e.data.toString();
                if (link.startsWith("http") ||          //$NON-NLS-1$
                        link.startsWith("file:/")) {    //$NON-NLS-1$
                    openLinkInBrowser(link);
                } else if (link.startsWith("page:")) {  //$NON-NLS-1$
                    // Switch to an internal page
                    setActivePage(link.substring(5 /* strlen("page:") */));
                }
            }
        };
    }

    /**
     * Open the http link into a browser
     *
     * @param link The URL to open in a browser
     */
    private void openLinkInBrowser(String link) {
        try {
            IWorkbenchBrowserSupport wbs = WorkbenchBrowserSupport.getInstance();
            wbs.createBrowser(BROWSER_ID).openURL(new URL(link));
        } catch (PartInitException e1) {
            // pass
        } catch (MalformedURLException e1) {
            // pass
        }
    }

    /**
     * Creates the XML source editor.
     * <p/>
     * Memorizes the index page of the source editor (it's always the last page, but the number
     * of pages before can change.)
     * <br/>
     * Retrieves the underlying XML model from the StructuredEditor and attaches a listener to it.
     * Finally triggers modelChanged() on the model listener -- derived classes can use this
     * to initialize the model the first time.
     * <p/>
     * Called only once <em>after</em> createFormPages.
     */
    private void createTextEditor() {
        try {
            mTextEditor = new StructuredTextEditor() {
                @Override
                protected void createActions() {
                    super.createActions();

                    Action action = new RenameResourceXmlTextAction(mTextEditor);
                    action.setActionDefinitionId(IJavaEditorActionDefinitionIds.RENAME_ELEMENT);
                    setAction(IJavaEditorActionDefinitionIds.RENAME_ELEMENT, action);
                }
            };
            int index = addPage(mTextEditor, getEditorInput());
            mTextPageIndex = index;
            setPageText(index, mTextEditor.getTitle());
            setPageImage(index,
                    IconFactory.getInstance().getIcon(ICON_XML_PAGE));

            if (!(mTextEditor.getTextViewer().getDocument() instanceof IStructuredDocument)) {
                Status status = new Status(IStatus.ERROR, AndmoreAndroidPlugin.PLUGIN_ID,
                        "Error opening the Android XML editor. Is the document an XML file?");
                throw new RuntimeException("Android XML Editor Error", new CoreException(status));
            }

            IStructuredModel xml_model = getModelForRead();
            if (xml_model != null) {
                try {
                    mXmlModelStateListener = new XmlModelStateListener();
                    xml_model.addModelStateListener(mXmlModelStateListener);
                    mXmlModelStateListener.modelChanged(xml_model);
                } catch (Exception e) {
                    AndmoreAndroidPlugin.log(e, "Error while loading editor"); //$NON-NLS-1$
                } finally {
                    xml_model.releaseFromRead();
                }
            }
        } catch (PartInitException e) {
            ErrorDialog.openError(getSite().getShell(),
                    "Android XML Editor Error", null, e.getStatus());
        }
    }

    /**
     * Returns the ISourceViewer associated with the Structured Text editor.
     */
    public final ISourceViewer getStructuredSourceViewer() {
        if (mTextEditor != null) {
            // We can't access mDelegate.getSourceViewer() because it is protected,
            // however getTextViewer simply returns the SourceViewer casted, so we
            // can use it instead.
            return mTextEditor.getTextViewer();
        }
        return null;
    }

    /**
     * Return the {@link StructuredTextEditor} associated with this XML editor
     *
     * @return the associated {@link StructuredTextEditor}
     */
    public StructuredTextEditor getStructuredTextEditor() {
        return mTextEditor;
    }

    /**
     * Returns the {@link IStructuredDocument} used by the StructuredTextEditor (aka Source
     * Editor) or null if not available.
     */
    public IStructuredDocument getStructuredDocument() {
        if (mTextEditor != null && mTextEditor.getTextViewer() != null) {
            return (IStructuredDocument) mTextEditor.getTextViewer().getDocument();
        }
        return null;
    }

    /**
     * Returns a version of the model that has been shared for read.
     * <p/>
     * Callers <em>must</em> call model.releaseFromRead() when done, typically
     * in a try..finally clause.
     *
     * Portability note: this uses getModelManager which is part of wst.sse.core; however
     * the interface returned is part of wst.sse.core.internal.provisional so we can
     * expect it to change in a distant future if they start cleaning their codebase,
     * however unlikely that is.
     *
     * @return The model for the XML document or null if cannot be obtained from the editor
     */
    public IStructuredModel getModelForRead() {
        IStructuredDocument document = getStructuredDocument();
        if (document != null) {
            IModelManager mm = StructuredModelManager.getModelManager();
            if (mm != null) {
                // TODO simplify this by not using the internal IStructuredDocument.
                // Instead we can now use mm.getModelForRead(getFile()).
                // However we must first check that SSE for Eclipse 3.3 or 3.4 has this
                // method. IIRC 3.3 didn't have it.

                return mm.getModelForRead(document);
            }
        }
        return null;
    }

    /**
     * Returns a version of the model that has been shared for edit.
     * <p/>
     * Callers <em>must</em> call model.releaseFromEdit() when done, typically
     * in a try..finally clause.
     * <p/>
     * Because of this, it is mandatory to use the wrapper
     * {@link #wrapEditXmlModel(Runnable)} which executes a runnable into a
     * properly configured model and then performs whatever cleanup is necessary.
     *
     * @return The model for the XML document or null if cannot be obtained from the editor
     */
    private IStructuredModel getModelForEdit() {
        IStructuredDocument document = getStructuredDocument();
        if (document != null) {
            IModelManager mm = StructuredModelManager.getModelManager();
            if (mm != null) {
                // TODO simplify this by not using the internal IStructuredDocument.
                // Instead we can now use mm.getModelForRead(getFile()).
                // However we must first check that SSE for Eclipse 3.3 or 3.4 has this
                // method. IIRC 3.3 didn't have it.

                return mm.getModelForEdit(document);
            }
        }
        return null;
    }

    /**
     * Helper class to perform edits on the XML model whilst making sure the
     * model has been prepared to be changed.
     * <p/>
     * It first gets a model for edition using {@link #getModelForEdit()},
     * then calls {@link IStructuredModel#aboutToChangeModel()},
     * then performs the requested action
     * and finally calls {@link IStructuredModel#changedModel()}
     * and {@link IStructuredModel#releaseFromEdit()}.
     * <p/>
     * The method is synchronous. As soon as the {@link IStructuredModel#changedModel()} method
     * is called, XML model listeners will be triggered.
     * <p/>
     * Calls can be nested: only the first outer call will actually start and close the edit
     * session.
     * <p/>
     * This method is <em>not synchronized</em> and is not thread safe.
     * Callers must be using it from the the main UI thread.
     *
     * @param editAction Something that will change the XML.
     */
    public final void wrapEditXmlModel(Runnable editAction) {
        wrapEditXmlModel(editAction, null);
    }

    /**
     * Perform any editor-specific hooks after applying an edit. When edits are
     * nested, the hooks will only run after the final top level edit has been
     * performed.
     * <p>
     * Note that the edit hooks are performed outside of the edit lock so
     * the hooks should not perform edits on the model without acquiring
     * a lock first.
     */
    public void runEditHooks() {
        if (!mIgnoreXmlUpdate) {
            // Check for errors, if enabled
            if (AdtPrefs.getPrefs().isLintOnSave()) {
                runLint();
            }
        }
    }

    /**
     * Executor which performs the given action under an edit lock (and optionally as a
     * single undo event).
     *
     * @param editAction the action to be executed
     * @param undoLabel if non null, the edit action will be run as a single undo event
     *            and the label used as the name of the undoable action
     */
    private final void wrapEditXmlModel(final Runnable editAction, final String undoLabel) {
        Display display = mTextEditor.getSite().getShell().getDisplay();
        if (display.getThread() != Thread.currentThread()) {
            display.syncExec(new Runnable() {
                @Override
                public void run() {
                    if (!mTextEditor.getTextViewer().getControl().isDisposed()) {
                        wrapEditXmlModel(editAction, undoLabel);
                    }
                }
            });
            return;
        }

        IStructuredModel model = null;
        int undoReverseCount = 0;
        try {

            if (mIsEditXmlModelPending == 0) {
                try {
                    model = getModelForEdit();
                    if (undoLabel != null) {
                        // Run this action as an undoable unit.
                        // We have to do it more than once, because in some scenarios
                        // Eclipse WTP decides to cancel the current undo command on its
                        // own -- see http://code.google.com/p/android/issues/detail?id=15901
                        // for one such call chain. By nesting these calls several times
                        // we've incrementing the command count such that a couple of
                        // cancellations are ignored. Interfering with this mechanism may
                        // sound dangerous, but it appears that this undo-termination is
                        // done for UI reasons to anticipate what the user wants, and we know
                        // that in *our* scenarios we want the entire unit run as a single
                        // unit. Here's what the documentation for
                        // IStructuredTextUndoManager#forceEndOfPendingCommand says
                        //   "Normally, the undo manager can figure out the best
                        //    times when to end a pending command and begin a new
                        //    one ... to the structure of a structured
                        //    document. There are times, however, when clients may
                        //    wish to override those algorithms and end one earlier
                        //    than normal. The one known case is for multi-page
                        //    editors. If a user is on one page, and type '123' as
                        //    attribute value, then click around to other parts of
                        //    page, or different pages, then return to '123|' and
                        //    type 456, then "undo" they typically expect the undo
                        //    to just undo what they just typed, the 456, not the
                        //    whole attribute value."
                        for (int i = 0; i < 4; i++) {
                            model.beginRecording(this, undoLabel);
                            undoReverseCount++;
                        }
                    }
                    model.aboutToChangeModel();
                } catch (Throwable t) {
                    // This is never supposed to happen unless we suddenly don't have a model.
                    // If it does, we don't want to even try to modify anyway.
                    AndmoreAndroidPlugin.log(t, "XML Editor failed to get model to edit");  //$NON-NLS-1$
                    return;
                }
            }
            mIsEditXmlModelPending++;
            editAction.run();
        } finally {
            mIsEditXmlModelPending--;
            if (model != null) {
                try {
                    boolean oldIgnore = mIgnoreXmlUpdate;
                    try {
                        mIgnoreXmlUpdate = true;

                        if (AdtPrefs.getPrefs().getFormatGuiXml() && mFormatNode != null) {
                            if (mFormatNode == getUiRootNode()) {
                                reformatDocument();
                            } else {
                                Node node = mFormatNode.getXmlNode();
                                if (node instanceof IndexedRegion) {
                                    IndexedRegion region = (IndexedRegion) node;
                                    int begin = region.getStartOffset();
                                    int end = region.getEndOffset();

                                    if (!mFormatChildren) {
                                        // This will format just the attribute list
                                        end = begin + 1;
                                    }

                                    if (mFormatChildren
                                         && node == node.getOwnerDocument().getDocumentElement()) {
                                        reformatDocument();
                                    } else {
                                        reformatRegion(begin, end);
                                    }
                                }
                            }
                            mFormatNode = null;
                            mFormatChildren = false;
                        }

                        // Notify the model we're done modifying it. This must *always* be executed.
                        model.changedModel();

                        // Clean up the undo unit. This is done more than once as explained
                        // above for beginRecording.
                        for (int i = 0; i < undoReverseCount; i++) {
                            model.endRecording(this);
                        }
                    } finally {
                        mIgnoreXmlUpdate = oldIgnore;
                    }
                } catch (Exception e) {
                    AndmoreAndroidPlugin.log(e, "Failed to clean up undo unit");
                }
                model.releaseFromEdit();

                if (mIsEditXmlModelPending < 0) {
                    AndmoreAndroidPlugin.log(IStatus.ERROR,
                            "wrapEditXmlModel finished with invalid nested counter==%1$d", //$NON-NLS-1$
                            mIsEditXmlModelPending);
                    mIsEditXmlModelPending = 0;
                }

                runEditHooks();

                // Notify listeners
                IStructuredModel readModel = getModelForRead();
                if (readModel != null) {
                    try {
                        mXmlModelStateListener.modelChanged(readModel);
                    } catch (Exception e) {
                        AndmoreAndroidPlugin.log(e, "Error while notifying changes"); //$NON-NLS-1$
                    } finally {
                        readModel.releaseFromRead();
                    }
                }
            }
        }
    }

    /**
     * Does this editor participate in the "format GUI editor changes" option?
     *
     * @return true if this editor supports automatically formatting XML
     *         affected by GUI changes
     */
    public boolean supportsFormatOnGuiEdit() {
        return false;
    }

    /**
     * Mark the given node as needing to be formatted when the current edits are
     * done, provided the user has turned that option on (see
     * {@link AdtPrefs#getFormatGuiXml()}).
     *
     * @param node the node to be scheduled for formatting
     * @param attributesOnly if true, only update the attributes list of the
     *            node, otherwise update the node recursively (e.g. all children
     *            too)
     */
    public void scheduleNodeReformat(UiElementNode node, boolean attributesOnly) {
        if (!supportsFormatOnGuiEdit()) {
            return;
        }

        if (node == mFormatNode) {
            if (!attributesOnly) {
                mFormatChildren = true;
            }
        } else if (mFormatNode == null) {
            mFormatNode = node;
            mFormatChildren = !attributesOnly;
        } else {
            if (mFormatNode.isAncestorOf(node)) {
                mFormatChildren = true;
            } else if (node.isAncestorOf(mFormatNode)) {
                mFormatNode = node;
                mFormatChildren = true;
            } else {
                // Two independent nodes; format their closest common ancestor.
                // Later we could consider having a small number of independent nodes
                // and formatting those, and only switching to formatting the common ancestor
                // when the number of individual nodes gets large.
                mFormatChildren = true;
                mFormatNode = UiElementNode.getCommonAncestor(mFormatNode, node);
            }
        }
    }

    /**
     * Creates an "undo recording" session by calling the undoableAction runnable
     * under an undo session.
     * <p/>
     * This also automatically starts an edit XML session, as if
     * {@link #wrapEditXmlModel(Runnable)} had been called.
     * <p>
     * You can nest several calls to {@link #wrapUndoEditXmlModel(String, Runnable)}, only one
     * recording session will be created.
     *
     * @param label The label for the undo operation. Can be null. Ideally we should really try
     *              to put something meaningful if possible.
     * @param undoableAction the action to be run as a single undoable unit
     */
    public void wrapUndoEditXmlModel(String label, Runnable undoableAction) {
        assert label != null : "All undoable actions should have a label";
        wrapEditXmlModel(undoableAction, label == null ? "" : label); //$NON-NLS-1$
    }

    /**
     * Returns true when the runnable of {@link #wrapEditXmlModel(Runnable)} is currently
     * being executed. This means it is safe to actually edit the XML model.
     *
     * @return true if the XML model is already locked for edits
     */
    public boolean isEditXmlModelPending() {
        return mIsEditXmlModelPending > 0;
    }

    /**
     * Returns the XML {@link Document} or null if we can't get it
     */
    public final Document getXmlDocument(IStructuredModel model) {
        if (model == null) {
            AndmoreAndroidPlugin.log(IStatus.WARNING, "Android Editor: No XML model for root node."); //$NON-NLS-1$
            return null;
        }

        if (model instanceof IDOMModel) {
            IDOMModel dom_model = (IDOMModel) model;
            return dom_model.getDocument();
        }
        return null;
    }

    /**
     * Returns the {@link IProject} for the edited file.
     */
    @Nullable
    public IProject getProject() {
        IFile file = getInputFile();
        if (file != null) {
            return file.getProject();
        }

        return null;
    }

    /**
     * Returns the {@link AndroidTargetData} for the edited file.
     */
    @Nullable
    public AndroidTargetData getTargetData() {
        IProject project = getProject();
        if (project != null) {
            Sdk currentSdk = Sdk.getCurrent();
            if (currentSdk != null) {
                IAndroidTarget target = currentSdk.getTarget(project);

                if (target != null) {
                    return currentSdk.getTargetData(target);
                }
            }
        }

        IEditorInput input = getEditorInput();
        if (input instanceof IURIEditorInput) {
            IURIEditorInput urlInput = (IURIEditorInput) input;
            Sdk currentSdk = Sdk.getCurrent();
            if (currentSdk != null) {
                try {
                    String path = AdtUtils.getFile(urlInput.getURI().toURL()).getPath();
                    for (IAndroidTarget target : currentSdk.getTargets()) {
                        if (path.startsWith(target.getLocation())) {
                            return currentSdk.getTargetData(target);
                        }
                    }
                } catch (MalformedURLException e) {
                    // File might be in some other weird random location we can't
                    // handle: Just ignore these
                }
            }
        }

        return null;
    }

    /**
     * Shows the editor range corresponding to the given XML node. This will
     * front the editor and select the text range.
     *
     * @param xmlNode The DOM node to be shown. The DOM node should be an XML
     *            node from the existing XML model used by the structured XML
     *            editor; it will not do attribute matching to find a
     *            "corresponding" element in the document from some foreign DOM
     *            tree.
     * @return True if the node was shown.
     */
    public boolean show(Node xmlNode) {
        if (xmlNode instanceof IndexedRegion) {
            IndexedRegion region = (IndexedRegion)xmlNode;

            IEditorPart textPage = getEditor(mTextPageIndex);
            if (textPage instanceof StructuredTextEditor) {
                StructuredTextEditor editor = (StructuredTextEditor) textPage;

                setActivePage(AndroidXmlEditor.TEXT_EDITOR_ID);

                // Note - we cannot use region.getLength() because that seems to
                // always return 0.
                int regionLength = region.getEndOffset() - region.getStartOffset();
                editor.selectAndReveal(region.getStartOffset(), regionLength);
                return true;
            }
        }

        return false;
    }

    /**
     * Selects and reveals the given range in the text editor
     *
     * @param start the beginning offset
     * @param length the length of the region to show
     * @param frontTab if true, front the tab, otherwise just make the selection but don't
     *     change the active tab
     */
    public void show(int start, int length, boolean frontTab) {
        IEditorPart textPage = getEditor(mTextPageIndex);
        if (textPage instanceof StructuredTextEditor) {
            StructuredTextEditor editor = (StructuredTextEditor) textPage;
            if (frontTab) {
                setActivePage(AndroidXmlEditor.TEXT_EDITOR_ID);
            }
            editor.selectAndReveal(start, length);
            if (frontTab) {
                editor.setFocus();
            }
        }
    }

    /**
     * Returns true if this editor has more than one page (usually a graphical view and an
     * editor)
     *
     * @return true if this editor has multiple pages
     */
    public boolean hasMultiplePages() {
        return getPageCount() > 1;
    }

    /**
     * Get the XML text directly from the editor.
     *
     * @param xmlNode The node whose XML text we want to obtain.
     * @return The XML representation of the {@link Node}, or null if there was an error.
     */
    public String getXmlText(Node xmlNode) {
        String data = null;
        IStructuredModel model = getModelForRead();
        try {
            IStructuredDocument document = getStructuredDocument();
            if (xmlNode instanceof NodeContainer) {
                // The easy way to get the source of an SSE XML node.
                data = ((NodeContainer) xmlNode).getSource();
            } else  if (xmlNode instanceof IndexedRegion && document != null) {
                // Try harder.
                IndexedRegion region = (IndexedRegion) xmlNode;
                int start = region.getStartOffset();
                int end = region.getEndOffset();

                if (end > start) {
                    data = document.get(start, end - start);
                }
            }
        } catch (BadLocationException e) {
            // the region offset was invalid. ignore.
        } finally {
            model.releaseFromRead();
        }
        return data;
    }

    /**
     * Formats the text around the given caret range, using the current Eclipse
     * XML formatter settings.
     *
     * @param begin The starting offset of the range to be reformatted.
     * @param end The ending offset of the range to be reformatted.
     */
    public void reformatRegion(int begin, int end) {
        ISourceViewer textViewer = getStructuredSourceViewer();

        // Clamp text range to valid offsets.
        IDocument document = textViewer.getDocument();
        int documentLength = document.getLength();
        end = Math.min(end, documentLength);
        begin = Math.min(begin, end);

        if (!AdtPrefs.getPrefs().getUseCustomXmlFormatter()) {
            // Workarounds which only apply to the builtin Eclipse formatter:
            //
            // It turns out the XML formatter does *NOT* format things correctly if you
            // select just a region of text. You *MUST* also include the leading whitespace
            // on the line, or it will dedent all the content to column 0. Therefore,
            // we must figure out the offset of the start of the line that contains the
            // beginning of the tag.
            try {
                IRegion lineInformation = document.getLineInformationOfOffset(begin);
                if (lineInformation != null) {
                    int lineBegin = lineInformation.getOffset();
                    if (lineBegin != begin) {
                        begin = lineBegin;
                    } else if (begin > 0) {
                        // Trick #2: It turns out that, if an XML element starts in column 0,
                        // then the XML formatter will NOT indent it (even if its parent is
                        // indented). If you on the other hand include the end of the previous
                        // line (the newline), THEN the formatter also correctly inserts the
                        // element. Therefore, we adjust the beginning range to include the
                        // previous line (if we are not already in column 0 of the first line)
                        // in the case where the element starts the line.
                        begin--;
                    }
                }
            } catch (BadLocationException e) {
                // This cannot happen because we already clamped the offsets
                AndmoreAndroidPlugin.log(e, e.toString());
            }
        }

        if (textViewer instanceof StructuredTextViewer) {
            StructuredTextViewer structuredTextViewer = (StructuredTextViewer) textViewer;
            int operation = ISourceViewer.FORMAT;
            boolean canFormat = structuredTextViewer.canDoOperation(operation);
            if (canFormat) {
                StyledText textWidget = textViewer.getTextWidget();
                textWidget.setSelection(begin, end);

                boolean oldIgnore = mIgnoreXmlUpdate;
                try {
                    // Formatting does not affect the XML model so ignore notifications
                    // about model edits from this
                    mIgnoreXmlUpdate = true;
                    structuredTextViewer.doOperation(operation);
                } finally {
                    mIgnoreXmlUpdate = oldIgnore;
                }

                textWidget.setSelection(0, 0);
            }
        }
    }

    /**
     * Invokes content assist in this editor at the given offset
     *
     * @param offset the offset to invoke content assist at, or -1 to leave
     *            caret alone
     */
    public void invokeContentAssist(int offset) {
        ISourceViewer textViewer = getStructuredSourceViewer();
        if (textViewer instanceof StructuredTextViewer) {
            StructuredTextViewer structuredTextViewer = (StructuredTextViewer) textViewer;
            int operation = ISourceViewer.CONTENTASSIST_PROPOSALS;
            boolean allowed = structuredTextViewer.canDoOperation(operation);
            if (allowed) {
                if (offset != -1) {
                    StyledText textWidget = textViewer.getTextWidget();
                    // Clamp text range to valid offsets.
                    IDocument document = textViewer.getDocument();
                    int documentLength = document.getLength();
                    offset = Math.max(0, Math.min(offset, documentLength));
                    textWidget.setSelection(offset, offset);
                }
                structuredTextViewer.doOperation(operation);
            }
        }
    }

    /**
     * Formats the XML region corresponding to the given node.
     *
     * @param node The node to be formatted.
     */
    public void reformatNode(Node node) {
        if (mIsCreatingPage) {
            return;
        }

        if (node instanceof IndexedRegion) {
            IndexedRegion region = (IndexedRegion) node;
            int begin = region.getStartOffset();
            int end = region.getEndOffset();
            reformatRegion(begin, end);
        }
    }

    /**
     * Formats the XML document according to the user's XML formatting settings.
     */
    public void reformatDocument() {
        ISourceViewer textViewer = getStructuredSourceViewer();
        if (textViewer instanceof StructuredTextViewer) {
            StructuredTextViewer structuredTextViewer = (StructuredTextViewer) textViewer;
            int operation = StructuredTextViewer.FORMAT_DOCUMENT;
            boolean canFormat = structuredTextViewer.canDoOperation(operation);
            if (canFormat) {
                boolean oldIgnore = mIgnoreXmlUpdate;
                try {
                    // Formatting does not affect the XML model so ignore notifications
                    // about model edits from this
                    mIgnoreXmlUpdate = true;
                    structuredTextViewer.doOperation(operation);
                } finally {
                    mIgnoreXmlUpdate = oldIgnore;
                }
            }
        }
    }

    /**
     * Returns the indentation String of the given node.
     *
     * @param xmlNode The node whose indentation we want.
     * @return The indent-string of the given node, or "" if the indentation for some reason could
     *         not be computed.
     */
    public String getIndent(Node xmlNode) {
        return getIndent(getStructuredDocument(), xmlNode);
    }

    /**
     * Returns the indentation String of the given node.
     *
     * @param document The Eclipse document containing the XML
     * @param xmlNode The node whose indentation we want.
     * @return The indent-string of the given node, or "" if the indentation for some reason could
     *         not be computed.
     */
    public static String getIndent(IDocument document, Node xmlNode) {
        if (xmlNode instanceof IndexedRegion) {
            IndexedRegion region = (IndexedRegion)xmlNode;
            int startOffset = region.getStartOffset();
            return getIndentAtOffset(document, startOffset);
        }

        return ""; //$NON-NLS-1$
    }

    /**
     * Returns the indentation String at the line containing the given offset
     *
     * @param document the document containing the offset
     * @param offset The offset of a character on a line whose indentation we seek
     * @return The indent-string of the given node, or "" if the indentation for some
     *         reason could not be computed.
     */
    public static String getIndentAtOffset(IDocument document, int offset) {
        try {
            IRegion lineInformation = document.getLineInformationOfOffset(offset);
            if (lineInformation != null) {
                int lineBegin = lineInformation.getOffset();
                if (lineBegin != offset) {
                    String prefix = document.get(lineBegin, offset - lineBegin);

                    // It's possible that the tag whose indentation we seek is not
                    // at the beginning of the line. In that case we'll just return
                    // the indentation of the line itself.
                    for (int i = 0; i < prefix.length(); i++) {
                        if (!Character.isWhitespace(prefix.charAt(i))) {
                            return prefix.substring(0, i);
                        }
                    }

                    return prefix;
                }
            }
        } catch (BadLocationException e) {
            AndmoreAndroidPlugin.log(e, "Could not obtain indentation"); //$NON-NLS-1$
        }

        return ""; //$NON-NLS-1$
    }

    /**
     * Returns the active {@link AndroidXmlEditor}, provided it matches the given source
     * viewer
     *
     * @param viewer the source viewer to ensure the active editor is associated with
     * @return the active editor provided it matches the given source viewer or null.
     */
    public static AndroidXmlEditor fromTextViewer(ITextViewer viewer) {
        IWorkbenchWindow wwin = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (wwin != null) {
            // Try the active editor first.
            IWorkbenchPage page = wwin.getActivePage();
            if (page != null) {
                IEditorPart editor = page.getActiveEditor();
                if (editor instanceof AndroidXmlEditor) {
                    ISourceViewer ssviewer =
                        ((AndroidXmlEditor) editor).getStructuredSourceViewer();
                    if (ssviewer == viewer) {
                        return (AndroidXmlEditor) editor;
                    }
                }
            }

            // If that didn't work, try all the editors
            for (IWorkbenchPage page2 : wwin.getPages()) {
                if (page2 != null) {
                    for (IEditorReference editorRef : page2.getEditorReferences()) {
                        IEditorPart editor = editorRef.getEditor(false /*restore*/);
                        if (editor instanceof AndroidXmlEditor) {
                            ISourceViewer ssviewer =
                                ((AndroidXmlEditor) editor).getStructuredSourceViewer();
                            if (ssviewer == viewer) {
                                return (AndroidXmlEditor) editor;
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    /** Called when this editor is activated */
    public void activated() {
        if (getActivePage() == mTextPageIndex) {
            updateActionBindings();
        }
    }

    /** Called when this editor is deactivated */
    public void deactivated() {
    }

    /**
     * Listen to changes in the underlying XML model in the structured editor.
     */
    private class XmlModelStateListener implements IModelStateListener {

        /**
         * A model is about to be changed. This typically is initiated by one
         * client of the model, to signal a large change and/or a change to the
         * model's ID or base Location. A typical use might be if a client might
         * want to suspend processing until all changes have been made.
         * <p/>
         * This AndroidXmlEditor implementation of IModelChangedListener is empty.
         */
        @Override
        public void modelAboutToBeChanged(IStructuredModel model) {
            // pass
        }

        /**
         * Signals that the changes foretold by modelAboutToBeChanged have been
         * made. A typical use might be to refresh, or to resume processing that
         * was suspended as a result of modelAboutToBeChanged.
         * <p/>
         * This AndroidXmlEditor implementation calls the xmlModelChanged callback.
         */
        @Override
        public void modelChanged(IStructuredModel model) {
            if (mIgnoreXmlUpdate) {
                return;
            }
            xmlModelChanged(getXmlDocument(model));
        }

        /**
         * Notifies that a model's dirty state has changed, and passes that state
         * in isDirty. A model becomes dirty when any change is made, and becomes
         * not-dirty when the model is saved.
         * <p/>
         * This AndroidXmlEditor implementation of IModelChangedListener is empty.
         */
        @Override
        public void modelDirtyStateChanged(IStructuredModel model, boolean isDirty) {
            // pass
        }

        /**
         * A modelDeleted means the underlying resource has been deleted. The
         * model itself is not removed from model management until all have
         * released it. Note: baseLocation is not (necessarily) changed in this
         * event, but may not be accurate.
         * <p/>
         * This AndroidXmlEditor implementation of IModelChangedListener is empty.
         */
        @Override
        public void modelResourceDeleted(IStructuredModel model) {
            // pass
        }

        /**
         * A model has been renamed or copied (as in saveAs..). In the renamed
         * case, the two parameters are the same instance, and only contain the
         * new info for id and base location.
         * <p/>
         * This AndroidXmlEditor implementation of IModelChangedListener is empty.
         */
        @Override
        public void modelResourceMoved(IStructuredModel oldModel, IStructuredModel newModel) {
            // pass
        }

        /**
         * This AndroidXmlEditor implementation of IModelChangedListener is empty.
         */
        @Override
        public void modelAboutToBeReinitialized(IStructuredModel structuredModel) {
            // pass
        }

        /**
         * This AndroidXmlEditor implementation of IModelChangedListener is empty.
         */
        @Override
        public void modelReinitialized(IStructuredModel structuredModel) {
            // pass
        }
    }
}

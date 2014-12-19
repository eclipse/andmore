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

package org.eclipse.andmore.internal.editors;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.core.internal.filebuffers.SynchronizableDocument;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.DocumentRewriteSession;
import org.eclipse.jface.text.DocumentRewriteSessionType;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension4;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.internal.browser.WorkbenchBrowserSupport;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.part.WorkbenchPart;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.wst.sse.ui.StructuredTextEditor;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Multi-page form editor for Android text files.
 * <p/>
 * It is designed to work with a {@link TextEditor} that will display a text file.
 * <br/>
 * Derived classes must implement createFormPages to create the forms before the
 * source editor. This can be a no-op if desired.
 */
@SuppressWarnings("restriction")
public abstract class AndroidTextEditor extends FormEditor implements IResourceChangeListener {

    /** Preference name for the current page of this file */
    private static final String PREF_CURRENT_PAGE = "_current_page";

    /** Id string used to create the Android SDK browser */
    private static String BROWSER_ID = "android"; //$NON-NLS-1$

    /** Page id of the XML source editor, used for switching tabs programmatically */
    public final static String TEXT_EDITOR_ID = "editor_part"; //$NON-NLS-1$

    /** Width hint for text fields. Helps the grid layout resize properly on smaller screens */
    public static final int TEXT_WIDTH_HINT = 50;

    /** Page index of the text editor (always the last page) */
    private int mTextPageIndex;

    /** The text editor */
    private TextEditor mTextEditor;

    /** flag set during page creation */
    private boolean mIsCreatingPage = false;

    private IDocument mDocument;

    /**
     * Creates a form editor.
     */
    public AndroidTextEditor() {
        super();
    }

    // ---- Abstract Methods ----

    /**
     * Creates the various form pages.
     * <p/>
     * Derived classes must implement this to add their own specific tabs.
     */
    abstract protected void createFormPages();

    /**
     * Called by the base class {@link AndroidTextEditor} once all pages (custom form pages
     * as well as text editor page) have been created. This give a chance to deriving
     * classes to adjust behavior once the text page has been created.
     */
    protected void postCreatePages() {
        // Nothing in the base class.
    }

    /**
     * Subclasses should override this method to process the new text model.
     * This is called after the document has been edited.
     *
     * The base implementation is empty.
     *
     * @param event Specification of changes applied to document.
     */
    protected void onDocumentChanged(DocumentEvent event) {
        // pass
    }

    // ---- Base Class Overrides, Interfaces Implemented ----

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
    protected void createAndroidPages() {
        mIsCreatingPage = true;
        createFormPages();
        createTextEditor();
        createUndoRedoActions();
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
     * Creates undo redo actions for the editor site (so that it works for any page of this
     * multi-page editor) by re-using the actions defined by the {@link TextEditor}
     * (aka the XML text editor.)
     */
    private void createUndoRedoActions() {
        IActionBars bars = getEditorSite().getActionBars();
        if (bars != null) {
            IAction action = mTextEditor.getAction(ActionFactory.UNDO.getId());
            bars.setGlobalActionHandler(ActionFactory.UNDO.getId(), action);

            action = mTextEditor.getAction(ActionFactory.REDO.getId());
            bars.setGlobalActionHandler(ActionFactory.REDO.getId(), action);

            bars.updateActionBars();
        }
    }

    /**
     * Selects the default active page.
     * @param defaultPageId the id of the page to show. If <code>null</code> the editor attempts to
     * find the default page in the properties of the {@link IResource} object being edited.
     */
    protected void selectDefaultPage(String defaultPageId) {
        if (defaultPageId == null) {
            if (getEditorInput() instanceof IFileEditorInput) {
                IFile file = ((IFileEditorInput) getEditorInput()).getFile();

                QualifiedName qname = new QualifiedName(AdtPlugin.PLUGIN_ID,
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
                AdtPlugin.log(e, "Selecting page '%s' in AndroidXmlEditor failed", defaultPageId);
            }
        }
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

        if (getEditorInput() instanceof IFileEditorInput) {
            IFile file = ((IFileEditorInput) getEditorInput()).getFile();

            QualifiedName qname = new QualifiedName(AdtPlugin.PLUGIN_ID,
                    getClass().getSimpleName() + PREF_CURRENT_PAGE);
            try {
                file.setPersistentProperty(qname, Integer.toString(newPageIndex));
            } catch (CoreException e) {
                // ignore
            }
        }
    }

    /**
     * Notifies this listener that some resource changes
     * are happening, or have already happened.
     *
     * Closes all project files on project close.
     * @see IResourceChangeListener
     */
    @Override
    public void resourceChanged(final IResourceChangeEvent event) {
        if (event.getType() == IResourceChangeEvent.PRE_CLOSE) {
            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    @SuppressWarnings("hiding")
                    IWorkbenchPage[] pages = getSite().getWorkbenchWindow().getPages();
                    for (int i = 0; i < pages.length; i++) {
                        if (((FileEditorInput)mTextEditor.getEditorInput())
                                .getFile().getProject().equals(
                                        event.getResource())) {
                            IEditorPart editorPart = pages[i].findEditor(mTextEditor
                                    .getEditorInput());
                            pages[i].closeEditor(editorPart, true);
                        }
                    }
                }
            });
        }
    }

    /**
     * Initializes the editor part with a site and input.
     * <p/>
     * Checks that the input is an instance of {@link IFileEditorInput}.
     *
     * @see FormEditor
     */
    @Override
    public void init(IEditorSite site, IEditorInput editorInput) throws PartInitException {
        if (!(editorInput instanceof IFileEditorInput))
            throw new PartInitException("Invalid Input: Must be IFileEditorInput");
        super.init(site, editorInput);
    }

    /**
     * Returns the {@link IFile} matching the editor's input or null.
     * <p/>
     * By construction, the editor input has to be an {@link IFileEditorInput} so it must
     * have an associated {@link IFile}. Null can only be returned if this editor has no
     * input somehow.
     */
    public IFile getFile() {
        if (getEditorInput() instanceof IFileEditorInput) {
            return ((IFileEditorInput) getEditorInput()).getFile();
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
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);

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

        // The actual "save" operation is done by the Structured XML Editor
        getEditor(mTextPageIndex).doSave(monitor);
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
            mTextEditor = new TextEditor();
            int index = addPage(mTextEditor, getEditorInput());
            mTextPageIndex = index;
            setPageText(index, mTextEditor.getTitle());

            IDocumentProvider provider = mTextEditor.getDocumentProvider();
            mDocument = provider.getDocument(getEditorInput());

            mDocument.addDocumentListener(new IDocumentListener() {
                @Override
                public void documentChanged(DocumentEvent event) {
                    onDocumentChanged(event);
                }

                @Override
                public void documentAboutToBeChanged(DocumentEvent event) {
                    // ignore
                }
            });


        } catch (PartInitException e) {
            ErrorDialog.openError(getSite().getShell(),
                    "Android Text Editor Error", null, e.getStatus());
        }
    }

    /**
     * Gives access to the {@link IDocument} from the {@link TextEditor}, corresponding to
     * the current file input.
     * <p/>
     * All edits should be wrapped in a {@link #wrapRewriteSession(Runnable)}.
     * The actual document instance is a {@link SynchronizableDocument}, which creates a lock
     * around read/set operations. The base API provided by {@link IDocument} provides ways to
     * manipulate the document line per line or as a bulk.
     */
    public IDocument getDocument() {
        return mDocument;
    }

    /**
     * Returns the {@link IProject} for the edited file.
     */
    public IProject getProject() {
        if (mTextEditor != null) {
            IEditorInput input = mTextEditor.getEditorInput();
            if (input instanceof FileEditorInput) {
                FileEditorInput fileInput = (FileEditorInput)input;
                IFile inputFile = fileInput.getFile();

                if (inputFile != null) {
                    return inputFile.getProject();
                }
            }
        }

        return null;
    }

    /**
     * Runs the given operation in the context of a document RewriteSession.
     * Takes care of properly starting and stopping the operation.
     * <p/>
     * The operation itself should just access {@link #getDocument()} and use the
     * normal document's API to manipulate it.
     *
     * @see #getDocument()
     */
    public void wrapRewriteSession(Runnable operation) {
        if (mDocument instanceof IDocumentExtension4) {
            IDocumentExtension4 doc4 = (IDocumentExtension4) mDocument;

            DocumentRewriteSession session = null;
            try {
                session = doc4.startRewriteSession(DocumentRewriteSessionType.UNRESTRICTED_SMALL);

                operation.run();
            } catch(IllegalStateException e) {
                AdtPlugin.log(e, "wrapRewriteSession failed");
                e.printStackTrace();
            } finally {
                if (session != null) {
                    doc4.stopRewriteSession(session);
                }
            }

        } else {
            // Not an IDocumentExtension4? Unlikely. Try the operation anyway.
            operation.run();
        }
    }

}

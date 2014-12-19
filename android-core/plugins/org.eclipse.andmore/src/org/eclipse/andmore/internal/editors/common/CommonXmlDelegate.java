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

package org.eclipse.andmore.internal.editors.common;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.resources.ResourceFolderType;

import org.eclipse.andmore.internal.editors.AndroidXmlEditor;
import org.eclipse.andmore.internal.editors.uimodel.UiElementNode;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.part.EditorActionBarContributor;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.w3c.dom.Document;

/**
 * Implementation of form editor for /res XML files.
 * <p/>
 * All delegates must have one {@link IDelegateCreator} instance
 * registered in the {@code DELEGATES[]} array of {@link CommonXmlEditor}.
 */
public abstract class CommonXmlDelegate {

    /** The editor that created the delegate. Never null. */
    private final CommonXmlEditor mEditor;

    /** Root node of the UI element hierarchy. Can be null. */
    private UiElementNode mUiRootNode;

    private IContentAssistProcessor mContentAssist;

    /**
     * Static creator for {@link CommonXmlDelegate}s. Delegates implement a method
     * that will decide whether this delegate can be created for the given file input.
     */
    public interface IDelegateCreator {
        /**
         * Determines whether this delegate can handle the given file, typically
         * based on its resource path (e.g. ResourceManager#getResourceFolder).
         *
         * @param delegator The non-null instance of {@link CommonXmlEditor}.
         * @param type The {@link ResourceFolderType} of the folder containing the file,
         *   if it can be determined. Null otherwise.
         * @return A new delegate that can handle that file or null.
         */
        public @Nullable <T extends CommonXmlDelegate> T createForFile(
                            @NonNull CommonXmlEditor delegator,
                            @Nullable ResourceFolderType type);
    }

    /** Implemented by delegates that need to support {@link EditorActionBarContributor} */
    public interface IActionContributorDelegate {
        /** Called from {@link EditorActionBarContributor#setActiveEditor(IEditorPart)}. */
        public void setActiveEditor(IEditorPart part, IActionBars bars);
    }

    protected CommonXmlDelegate(
            CommonXmlEditor editor,
            IContentAssistProcessor contentAssist) {
        mEditor = editor;
        mContentAssist = contentAssist;
    }

    public void dispose() {
    }

    /**
     * Returns the editor that created this delegate.
     *
     * @return the editor that created this delegate. Never null.
     */
    public @NonNull CommonXmlEditor getEditor() {
        return mEditor;
    }

    /**
     * @return The root node of the UI element hierarchy
     */
    public UiElementNode getUiRootNode() {
        return mUiRootNode;
    }

    protected void setUiRootNode(UiElementNode uiRootNode) {
        mUiRootNode = uiRootNode;
    }

    /** Called to compute the initial {@code UiRootNode}. */
    public abstract void delegateInitUiRootNode(boolean force);

    /**
     * Returns true, indicating the "save as" operation is supported by this editor.
     */
    public boolean isSaveAsAllowed() {
        return true;
    }

    /**
     * Create the various form pages.
     */
    public abstract void delegateCreateFormPages();

    public void delegatePostCreatePages() {
        // pass
    }

    /**
     * Changes the tab/title name to include the project name.
     */
    public void delegateSetInput(IEditorInput input) {
        if (input instanceof FileEditorInput) {
            FileEditorInput fileInput = (FileEditorInput) input;
            IFile file = fileInput.getFile();
            getEditor().setPartName(file.getName());
        } else if (input instanceof IURIEditorInput) {
            IURIEditorInput uriInput = (IURIEditorInput) input;
            String name = uriInput.getName();
            getEditor().setPartName(name);
        }
    }

    /**
     * Processes the new XML Model, which XML root node is given.
     *
     * @param xml_doc The XML document, if available, or null if none exists.
     */
    public abstract void delegateXmlModelChanged(Document xml_doc);

    public void delegatePageChange(int newPageIndex) {
        // pass
    }

    public void delegatePostPageChange(int newPageIndex) {
        // pass
    }
    /**
     * Save the XML.
     * <p/>
     * The actual save operation is done in the super class by committing
     * all data to the XML model and then having the Structured XML Editor
     * save the XML.
     * <p/>
     * Here we just need to tell the graphical editor that the model has
     * been saved.
     */
    public void delegateDoSave(IProgressMonitor monitor) {
        // pass
    }

    /**
     * Tells the editor to start a Lint check.
     * It's up to the caller to check whether this should be done depending on preferences.
     */
    public Job delegateRunLint() {
        return getEditor().startLintJob();
    }


    /**
     * Returns the custom IContentOutlinePage or IPropertySheetPage when asked for it.
     */
    public Object delegateGetAdapter(Class<?> adapter) {
        return null;
    }

    /**
     * Returns the {@link IContentAssistProcessor} associated with this editor.
     * Most implementations should lazily allocate one processor and always return the
     * same instance.
     * Must return null if there's no specific content assist processor for this editor.
     */
    public IContentAssistProcessor getAndroidContentAssistProcessor() {
        return mContentAssist;
    }

    /**
     * Does this editor participate in the "format GUI editor changes" option?
     *
     * @return false since editors do not support automatically formatting XML
     *         affected by GUI changes unless they explicitly opt in to it.
     */
    public boolean delegateSupportsFormatOnGuiEdit() {
        return false;
    }

    /**
     * Called after the editor's active page has been set.
     *
     * @param superReturned the return value from
     *            {@link MultiPageEditorPart#setActivePage(int)}
     * @param pageIndex the index of the page to be activated; the index must be
     *            valid
     * @return the page, or null
     * @see MultiPageEditorPart#setActivePage(int)
     */
    public IFormPage delegatePostSetActivePage(IFormPage superReturned, String pageIndex) {
        return superReturned;
    }

    /** Called after an editor has been activated */
    public void delegateActivated() {
    }

    /** Called after an editor has been deactivated */
    public void delegateDeactivated() {
    }

    /**
     * Returns the name of the editor to be shown in the editor tab etc. Return
     * null to keep the default.
     *
     * @return the part name, or null to use the default
     */
    public String delegateGetPartName() {
        return null;
    }

    /**
     * Returns the persistence category, as described in
     * {@link AndroidXmlEditor#getPersistenceCategory}.
     *
     * @return the persistence category to use for this editor
     */
    public int delegateGetPersistenceCategory() {
        return AndroidXmlEditor.CATEGORY_OTHER;
    }
}

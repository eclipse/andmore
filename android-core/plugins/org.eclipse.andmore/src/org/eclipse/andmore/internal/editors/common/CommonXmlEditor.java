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

import com.android.ide.common.resources.ResourceFolder;
import com.android.resources.ResourceFolderType;

import org.eclipse.andmore.AdtConstants;
import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.AdtUtils;
import org.eclipse.andmore.internal.editors.AndroidXmlEditor;
import org.eclipse.andmore.internal.editors.animator.AnimationEditorDelegate;
import org.eclipse.andmore.internal.editors.color.ColorEditorDelegate;
import org.eclipse.andmore.internal.editors.common.CommonXmlDelegate.IDelegateCreator;
import org.eclipse.andmore.internal.editors.drawable.DrawableEditorDelegate;
import org.eclipse.andmore.internal.editors.layout.LayoutEditorDelegate;
import org.eclipse.andmore.internal.editors.menu.MenuEditorDelegate;
import org.eclipse.andmore.internal.editors.otherxml.OtherXmlEditorDelegate;
import org.eclipse.andmore.internal.editors.otherxml.PlainXmlEditorDelegate;
import org.eclipse.andmore.internal.editors.uimodel.UiElementNode;
import org.eclipse.andmore.internal.editors.values.ValuesEditorDelegate;
import org.eclipse.andmore.internal.resources.manager.ResourceManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.ISourceViewerExtension2;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IShowEditorInput;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.ide.IDE;
import org.w3c.dom.Document;

/**
 * Multi-page form editor for ALL /res XML files.
 * <p/>
 * This editor doesn't actually do anything. Instead, it defers actual implementation
 * to {@link CommonXmlDelegate} instances.
 */
public class CommonXmlEditor extends AndroidXmlEditor implements IShowEditorInput {

    public static final String ID = AdtConstants.EDITORS_NAMESPACE + ".CommonXmlEditor"; //$NON-NLS-1$

    /**
     * Registered {@link CommonXmlDelegate}s.
     * All delegates must have a {@code Creator} class which is instantiated
     * once here statically. All the creators are invoked in the order they
     * are defined and the first one to return a non-null delegate is used.
     */
    private static final IDelegateCreator[] DELEGATES = {
            new LayoutEditorDelegate.Creator(),
            new ValuesEditorDelegate.Creator(),
            new AnimationEditorDelegate.Creator(),
            new ColorEditorDelegate.Creator(),
            new DrawableEditorDelegate.Creator(),
            new MenuEditorDelegate.Creator(),
            new OtherXmlEditorDelegate.Creator(),
    };

    /**
     * IDs of legacy editors replaced by the {@link CommonXmlEditor}.
     */
    public static final String[] LEGACY_EDITOR_IDS = {
        LayoutEditorDelegate.LEGACY_EDITOR_ID,
        ValuesEditorDelegate.LEGACY_EDITOR_ID,
        AnimationEditorDelegate.LEGACY_EDITOR_ID,
        ColorEditorDelegate.LEGACY_EDITOR_ID,
        DrawableEditorDelegate.LEGACY_EDITOR_ID,
        MenuEditorDelegate.LEGACY_EDITOR_ID,
        OtherXmlEditorDelegate.LEGACY_EDITOR_ID,
    };

    private CommonXmlDelegate mDelegate = null;

    /**
     * Creates the form editor for resources XML files.
     */
    public CommonXmlEditor() {
        super();
    }

    @Override
    public void init(IEditorSite site, final IEditorInput editorInput)
            throws PartInitException {
        if (editorInput instanceof IFileEditorInput) {

            IFileEditorInput fileInput = (IFileEditorInput) editorInput;
            IFile file = fileInput.getFile();

            // Adjust the default file editor ID

            IEditorDescriptor file_desc = IDE.getDefaultEditor(file);
            String id = file_desc == null ? null : file_desc.getId();
            boolean mustChange = id != null &&
                                 !id.equals(ID) &&
                                 id.startsWith(AdtConstants.EDITORS_NAMESPACE);
            if (!mustChange) {
                // Maybe this was opened by a manual Open With with a legacy ID?
                id = site.getId();
                mustChange = id != null &&
                             !id.equals(ID) &&
                             id.startsWith(AdtConstants.EDITORS_NAMESPACE);
            }

            if (mustChange) {
                // It starts by our editor namespace but it's not the right ID.
                // This is an old Android XML ID. Change it to our new ID.
                IDE.setDefaultEditor(file, ID);
                AdtPlugin.log(IStatus.INFO,
                        "Changed legacy editor ID %s for %s",   //$NON-NLS-1$
                        id,
                        file.getFullPath());
            }

            // Now find the delegate for the file.

            ResourceFolder resFolder = ResourceManager.getInstance().getResourceFolder(file);
            ResourceFolderType type = resFolder == null ? null : resFolder.getType();

            if (type == null) {
                // We lack any real resource information about that file.
                // Let's take a guess using the actual path.
                String folderName = AdtUtils.getParentFolderName(editorInput);
                type = ResourceFolderType.getFolderType(folderName);
            }

            if (type != null) {
                for (IDelegateCreator creator : DELEGATES) {
                    mDelegate = creator.createForFile(this, type);
                    if (mDelegate != null) {
                        break;
                    }
                }
            }

            if (mDelegate == null) {
                // We didn't find any editor.
                // We'll use the PlainXmlEditorDelegate as a catch-all editor.
                AdtPlugin.log(IStatus.INFO,
                        "No valid Android XML Editor Delegate found for file %1$s [Res %2$s, type %3$s]",
                        file.getFullPath(),
                        resFolder,
                        type);
                mDelegate = new PlainXmlEditorDelegate(this);
            }
        } else if (editorInput instanceof IURIEditorInput) {
            String folderName = AdtUtils.getParentFolderName(editorInput);
            ResourceFolderType type = ResourceFolderType.getFolderType(folderName);
            if (type == ResourceFolderType.LAYOUT) {
                // The layout editor has a lot of hardcoded requirements for real IFiles
                // and IProjects so for now just use a plain XML editor for project-less layout
                // files
                mDelegate = new OtherXmlEditorDelegate(this);
            } else if (type != null) {
                for (IDelegateCreator creator : DELEGATES) {
                    mDelegate = creator.createForFile(this, type);
                    if (mDelegate != null) {
                        break;
                    }
                }
            }

            if (mDelegate == null) {
                // We didn't find any editor.
                // We'll use the PlainXmlEditorDelegate as a catch-all editor.
                AdtPlugin.log(IStatus.INFO,
                        "No valid Android XML Editor Delegate found for file %1$s [Res %2$s, type %3$s]",
                        ((IURIEditorInput) editorInput).getURI().toString(),
                        folderName,
                        type);
                mDelegate = new PlainXmlEditorDelegate(this);
            }
        }

        if (mDelegate == null) {
            // We can't do anything if we don't have a valid file.
            AdtPlugin.log(IStatus.INFO,
                    "Android XML Editor cannot process non-file input %1$s",   //$NON-NLS-1$
                    (editorInput == null ? "null" : editorInput.toString()));   //$NON-NLS-1$
            throw new PartInitException("Android XML Editor cannot process this input.");
        } else {
            // Invoke the editor's init after setting up the delegate. This will call setInput().
            super.init(site, editorInput);
        }
    }

    /**
     * @return The root node of the UI element hierarchy
     */
    @Override
    public UiElementNode getUiRootNode() {
        return mDelegate == null ? null : mDelegate.getUiRootNode();
    }

    public CommonXmlDelegate getDelegate() {
        return mDelegate;
    }

    // ---- Base Class Overrides ----

    @Override
    public void dispose() {
        if (mDelegate != null) {
            mDelegate.dispose();
        }

        super.dispose();
    }

    /**
     * Save the XML.
     * <p/>
     * The actual save operation is done in the super class by committing
     * all data to the XML model and then having the Structured XML Editor
     * save the XML.
     * <p/>
     * Here we just need to tell the delegate that the model has
     * been saved.
     */
    @Override
    public void doSave(IProgressMonitor monitor) {
        super.doSave(monitor);
        if (mDelegate != null) {
            mDelegate.delegateDoSave(monitor);
        }
    }

    /**
     * Returns whether the "save as" operation is supported by this editor.
     * <p/>
     * Save-As is a valid operation for the ManifestEditor since it acts on a
     * single source file.
     *
     * @see IEditorPart
     */
    @Override
    public boolean isSaveAsAllowed() {
        return mDelegate == null ? false : mDelegate.isSaveAsAllowed();
    }

    /**
     * Create the various form pages.
     */
    @Override
    protected void createFormPages() {
        if (mDelegate != null) {
            mDelegate.delegateCreateFormPages();
        }
    }

    @Override
    protected void postCreatePages() {
        super.postCreatePages();

        if (mDelegate != null) {
            mDelegate.delegatePostCreatePages();
        }
    }

    @Override
    protected void addPages() {
        // Create the editor pages.
        // This will also create the EditorPart.
        super.addPages();

        // When the EditorPart is being created, it configures the SourceViewer
        // and will try to use our CommonSourceViewerConfig. Our config needs to
        // know which ContentAssist processor to use (since we have one per resource
        // folder type) but it doesn't have the necessary info to do so.
        // Consequently, once the part is created, we can now unconfigure the source
        // viewer and reconfigure it with the right settings.
        ISourceViewer ssv = getStructuredSourceViewer();
        if (mDelegate != null && ssv instanceof ISourceViewerExtension2) {
            ((ISourceViewerExtension2) ssv).unconfigure();
            ssv.configure(new CommonSourceViewerConfig(
                    mDelegate.getAndroidContentAssistProcessor()));
        }
    }

    /* (non-java doc)
     * Change the tab/title name to include the name of the layout.
     */
    @Override
    protected void setInput(IEditorInput input) {
        super.setInput(input);
        assert mDelegate != null;
        if (mDelegate != null) {
            mDelegate.delegateSetInput(input);
        }
    }

    @Override
    public void setInputWithNotify(IEditorInput input) {
        super.setInputWithNotify(input);
        if (mDelegate instanceof LayoutEditorDelegate) {
            ((LayoutEditorDelegate) mDelegate).delegateSetInputWithNotify(input);
        }
    }

    /**
     * Processes the new XML Model, which XML root node is given.
     *
     * @param xml_doc The XML document, if available, or null if none exists.
     */
    @Override
    protected void xmlModelChanged(Document xml_doc) {
        if (mDelegate != null) {
            mDelegate.delegateXmlModelChanged(xml_doc);
        }
    }

    @Override
    protected Job runLint() {
        if (mDelegate != null && getEditorInput() instanceof IFileEditorInput) {
            return mDelegate.delegateRunLint();
        }
        return null;
    }

    /**
     * Returns the custom IContentOutlinePage or IPropertySheetPage when asked for it.
     */
    @Override
    public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
        if (mDelegate != null) {
            Object value = mDelegate.delegateGetAdapter(adapter);
            if (value != null) {
                return value;
            }
        }

        // return default
        return super.getAdapter(adapter);
    }

    @Override
    protected void pageChange(int newPageIndex) {
        if (mDelegate != null) {
            mDelegate.delegatePageChange(newPageIndex);
        }

        super.pageChange(newPageIndex);

        if (mDelegate != null) {
            mDelegate.delegatePostPageChange(newPageIndex);
        }
    }

    @Override
    protected int getPersistenceCategory() {
        if (mDelegate != null) {
            return mDelegate.delegateGetPersistenceCategory();
        }
        return CATEGORY_OTHER;
    }

    @Override
    public void initUiRootNode(boolean force) {
        if (mDelegate != null) {
            mDelegate.delegateInitUiRootNode(force);
        }
    }

    @Override
    public IFormPage setActivePage(String pageId) {
        IFormPage page = super.setActivePage(pageId);

        if (mDelegate != null) {
            return mDelegate.delegatePostSetActivePage(page, pageId);
        }

        return page;
    }

    /* Implements showEditorInput(...) in IShowEditorInput */
    @Override
    public void showEditorInput(IEditorInput editorInput) {
        if (mDelegate instanceof LayoutEditorDelegate) {
            ((LayoutEditorDelegate) mDelegate).showEditorInput(editorInput);
        }
    }

    @Override
    public boolean supportsFormatOnGuiEdit() {
        if (mDelegate != null) {
            return mDelegate.delegateSupportsFormatOnGuiEdit();
        }
        return super.supportsFormatOnGuiEdit();
    }

    @Override
    public void activated() {
        super.activated();
        if (mDelegate != null) {
            mDelegate.delegateActivated();
        }
    }

    @Override
    public void deactivated() {
        super.deactivated();
        if (mDelegate != null) {
            mDelegate.delegateDeactivated();
        }
    }

    @Override
    public String getPartName() {
        if (mDelegate != null) {
            String name = mDelegate.delegateGetPartName();
            if (name != null) {
                return name;
            }
        }

        return super.getPartName();
    }

    // --------------------
    // Base methods exposed so that XmlEditorDelegate can access them

    @Override
    public void setPartName(String partName) {
        super.setPartName(partName);
    }

    @Override
    public void setPageText(int pageIndex, String text) {
        super.setPageText(pageIndex, text);
    }

    @Override
    public void firePropertyChange(int propertyId) {
        super.firePropertyChange(propertyId);
    }

    @Override
    public int getPageCount() {
        return super.getPageCount();
    }

    @Override
    public int getCurrentPage() {
        return super.getCurrentPage();
    }
}

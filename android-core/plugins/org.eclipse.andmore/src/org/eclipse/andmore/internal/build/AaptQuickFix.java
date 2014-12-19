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

package org.eclipse.andmore.internal.build;

import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.XMLNS_ANDROID;
import static com.android.SdkConstants.XMLNS_URI;

import com.android.ide.common.resources.ResourceUrl;
import com.android.resources.ResourceType;
import com.android.utils.Pair;

import org.eclipse.andmore.AdtConstants;
import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.AdtUtils;
import org.eclipse.andmore.internal.editors.AndroidXmlEditor;
import org.eclipse.andmore.internal.resources.ResourceHelper;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolution2;
import org.eclipse.ui.IMarkerResolutionGenerator2;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.List;

/**
 * Shared handler for both quick assist processors (Control key handler) and quick fix
 * marker resolution (Problem view handling), since there is a lot of overlap between
 * these two UI handlers.
 */
@SuppressWarnings("restriction") // XML model
public class AaptQuickFix implements IMarkerResolutionGenerator2, IQuickAssistProcessor {

    public AaptQuickFix() {
    }

    /** Returns the error message from aapt that signals missing resources */
    private static String getTargetMarkerErrorMessage() {
        return "No resource found that matches the given name";
    }

    /** Returns the error message from aapt that signals a missing namespace declaration */
    private static String getUnboundErrorMessage() {
        return "Error parsing XML: unbound prefix";
    }

    // ---- Implements IMarkerResolution2 ----

    @Override
    public boolean hasResolutions(IMarker marker) {
        String message = null;
        try {
            message = (String) marker.getAttribute(IMarker.MESSAGE);
        } catch (CoreException e) {
            AdtPlugin.log(e, null);
        }

        return message != null
                && (message.contains(getTargetMarkerErrorMessage())
                        || message.contains(getUnboundErrorMessage()));
    }

    @Override
    public IMarkerResolution[] getResolutions(IMarker marker) {
        IResource markerResource = marker.getResource();
        IProject project = markerResource.getProject();
        try {
            String message = (String) marker.getAttribute(IMarker.MESSAGE);
            if (message.contains(getUnboundErrorMessage()) && markerResource instanceof IFile) {
                return new IMarkerResolution[] {
                        new CreateNamespaceFix((IFile) markerResource)
                    };
            }
        } catch (CoreException e1) {
            AdtPlugin.log(e1, null);
        }

        int start = marker.getAttribute(IMarker.CHAR_START, 0);
        int end = marker.getAttribute(IMarker.CHAR_END, 0);
        if (end > start) {
            int length = end - start;
            IDocumentProvider provider = new TextFileDocumentProvider();
            try {
                provider.connect(markerResource);
                IDocument document = provider.getDocument(markerResource);
                String resource = document.get(start, length);
                if (ResourceHelper.canCreateResource(resource)) {
                    return new IMarkerResolution[] {
                        new CreateResourceProposal(project, resource)
                    };
                }
            } catch (Exception e) {
                AdtPlugin.log(e, "Can't find range information for %1$s", markerResource);
            } finally {
                provider.disconnect(markerResource);
            }
        }

        return null;
    }

    // ---- Implements IQuickAssistProcessor ----

    @Override
    public boolean canAssist(IQuickAssistInvocationContext invocationContext) {
        return true;
    }

    @Override
    public boolean canFix(Annotation annotation) {
        return true;
    }

    @Override
    public ICompletionProposal[] computeQuickAssistProposals(
            IQuickAssistInvocationContext invocationContext) {

        // We have to find the corresponding project/file (so we can look up the aapt
        // error markers). Unfortunately, an IQuickAssistProcessor only gets
        // access to an ISourceViewer which has no hooks back to the surrounding
        // editor.
        //
        // However, the IQuickAssistProcessor will only be used interactively by a file
        // being edited, so we can cheat like the hyperlink detector and simply
        // look up the currently active file in the IDE. To be on the safe side,
        // we'll make sure that that editor has the same sourceViewer such that
        // we are indeed looking at the right file:
        ISourceViewer sourceViewer = invocationContext.getSourceViewer();
        AndroidXmlEditor editor = AndroidXmlEditor.fromTextViewer(sourceViewer);
        if (editor != null) {
            IFile file = editor.getInputFile();
            if (file == null) {
                return null;
            }
            IDocument document = sourceViewer.getDocument();
            List<IMarker> markers = AdtUtils.findMarkersOnLine(AdtConstants.MARKER_AAPT_COMPILE,
                    file, document, invocationContext.getOffset());
            try {
                for (IMarker marker : markers) {
                    String message = marker.getAttribute(IMarker.MESSAGE, ""); //$NON-NLS-1$
                    if (message.contains(getTargetMarkerErrorMessage())) {
                        int start = marker.getAttribute(IMarker.CHAR_START, 0);
                        int end = marker.getAttribute(IMarker.CHAR_END, 0);
                        int length = end - start;
                        String resource = document.get(start, length);
                        // Can only offer create value for non-framework value
                        // resources
                        if (ResourceHelper.canCreateResource(resource)) {
                            IProject project = editor.getProject();
                            return new ICompletionProposal[] {
                                new CreateResourceProposal(project, resource)
                            };
                        }
                    } else if (message.contains(getUnboundErrorMessage())) {
                        return new ICompletionProposal[] {
                            new CreateNamespaceFix(null)
                        };
                    }
                }
            } catch (BadLocationException e) {
                AdtPlugin.log(e, null);
            }
        }

        return null;
    }

    @Override
    public String getErrorMessage() {
        return null;
    }

    /** Quick fix to insert namespace binding when missing */
    private final static class CreateNamespaceFix
            implements ICompletionProposal, IMarkerResolution2 {
        private IFile mFile;

        public CreateNamespaceFix(IFile file) {
            mFile = file;
        }

        private IndexedRegion perform(IDocument doc) {
            IModelManager manager = StructuredModelManager.getModelManager();
            IStructuredModel model = manager.getExistingModelForEdit(doc);
            if (model != null) {
                try {
                    perform(model);
                } finally {
                    model.releaseFromEdit();
                }
            }

            return null;
        }

        private IndexedRegion perform(IFile file) {
            IModelManager manager = StructuredModelManager.getModelManager();
            IStructuredModel model;
            try {
                model = manager.getModelForEdit(file);
                if (model != null) {
                    try {
                        perform(model);
                    } finally {
                        model.releaseFromEdit();
                    }
                }
            } catch (Exception e) {
                AdtPlugin.log(e, "Can't look up XML model");
            }

            return null;
        }

        private IndexedRegion perform(IStructuredModel model) {
            if (model instanceof IDOMModel) {
                IDOMModel domModel = (IDOMModel) model;
                Document document = domModel.getDocument();
                Element element = document.getDocumentElement();
                Attr attr = document.createAttributeNS(XMLNS_URI, XMLNS_ANDROID);
                attr.setValue(ANDROID_URI);
                element.getAttributes().setNamedItemNS(attr);
                return (IndexedRegion) attr;
            }

            return null;
        }

        // ---- Implements ICompletionProposal ----

        @Override
        public void apply(IDocument document) {
            perform(document);
        }

        @Override
        public String getAdditionalProposalInfo() {
            return "Adds an Android namespace declaratiopn to the root element.";
        }

        @Override
        public IContextInformation getContextInformation() {
            return null;
        }

        @Override
        public String getDisplayString() {
            return "Insert namespace binding";
        }

        @Override
        public Image getImage() {
            return AdtPlugin.getAndroidLogo();
        }

        @Override
        public Point getSelection(IDocument doc) {
            return null;
        }


        // ---- Implements MarkerResolution2 ----

        @Override
        public String getLabel() {
            return getDisplayString();
        }

        @Override
        public void run(IMarker marker) {
            try {
                AdtPlugin.openFile(mFile, null);
            } catch (PartInitException e) {
                AdtPlugin.log(e, "Can't open file %1$s", mFile.getName());
            }

            IndexedRegion indexedRegion = perform(mFile);
            if (indexedRegion != null) {
                try {
                    IRegion region =
                        new Region(indexedRegion.getStartOffset(), indexedRegion.getLength());
                    AdtPlugin.openFile(mFile, region);
                } catch (PartInitException e) {
                    AdtPlugin.log(e, "Can't open file %1$s", mFile.getName());
                }
            }
        }

        @Override
        public String getDescription() {
            return getAdditionalProposalInfo();
        }
    }

    private static class CreateResourceProposal
            implements ICompletionProposal, IMarkerResolution2 {
        private final IProject mProject;
        private final String mResource;

        CreateResourceProposal(IProject project, String resource) {
            super();
            mProject = project;
            mResource = resource;
        }

        private void perform() {
            ResourceUrl resource = ResourceUrl.parse(mResource);
            if (resource == null) {
                return;
            }
            ResourceType type = resource.type;
            String name = resource.name;
            assert !resource.framework;
            String value = ""; //$NON-NLS-1$

            // Try to pick a reasonable first guess. The new value will be highlighted and
            // selected for editing, but if we have an initial value then the new file
            // won't show an error.
            switch (type) {
                case STRING: value = "TODO"; break; //$NON-NLS-1$
                case DIMEN: value = "1dp"; break; //$NON-NLS-1$
                case BOOL: value = "true"; break; //$NON-NLS-1$
                case COLOR: value = "#000000"; break; //$NON-NLS-1$
                case INTEGER: value = "1"; break; //$NON-NLS-1$
                case ARRAY: value = "<item>1</item>"; break; //$NON-NLS-1$
            }

            Pair<IFile, IRegion> location =
                ResourceHelper.createResource(mProject, type, name, value);
            if (location != null) {
                IFile file = location.getFirst();
                IRegion region = location.getSecond();
                try {
                    AdtPlugin.openFile(file, region);
                } catch (PartInitException e) {
                    AdtPlugin.log(e, "Can't open file %1$s", file.getName());
                }
            }
        }

        // ---- Implements ICompletionProposal ----

        @Override
        public void apply(IDocument document) {
            perform();
        }

        @Override
        public String getAdditionalProposalInfo() {
            return "Creates an XML file entry for the given missing resource "
                    + "and opens it in the editor.";
        }

        @Override
        public IContextInformation getContextInformation() {
            return null;
        }

        @Override
        public String getDisplayString() {
            return String.format("Create resource %1$s", mResource);
        }

        @Override
        public Image getImage() {
            return AdtPlugin.getAndroidLogo();
        }

        @Override
        public Point getSelection(IDocument document) {
            return null;
        }

        // ---- Implements MarkerResolution2 ----

        @Override
        public String getLabel() {
            return getDisplayString();
        }

        @Override
        public void run(IMarker marker) {
            perform();
        }

        @Override
        public String getDescription() {
            return getAdditionalProposalInfo();
        }
    }
}

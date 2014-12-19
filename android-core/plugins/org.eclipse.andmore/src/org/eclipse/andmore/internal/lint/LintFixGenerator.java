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
package org.eclipse.andmore.internal.lint;

import static com.android.SdkConstants.DOT_JAVA;
import static com.android.SdkConstants.DOT_XML;

import com.android.tools.lint.client.api.Configuration;
import com.android.tools.lint.client.api.DefaultConfiguration;
import com.android.tools.lint.client.api.IssueRegistry;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.Issue.OutputFormat;
import com.android.tools.lint.detector.api.Project;
import com.android.tools.lint.detector.api.Severity;
import com.android.utils.SdkUtils;

import org.eclipse.andmore.AdtConstants;
import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.AdtUtils;
import org.eclipse.andmore.internal.editors.AndroidXmlEditor;
import org.eclipse.andmore.internal.preferences.AdtPrefs;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
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
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolution2;
import org.eclipse.ui.IMarkerResolutionGenerator2;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A quickfix and marker resolution for disabling lint checks, and any
 * IDE specific implementations for fixing the warnings.
 * <p>
 * I would really like for this quickfix to show up as a light bulb on top of the error
 * icon in the editor, and I've spent a whole day trying to make it work. I did not
 * succeed, but here are the steps I tried in case I want to pick up the work again
 * later:
 * <ul>
 * <li>
 *     The WST has some support for quick fixes, and I came across some forum posts
 *     referencing the ability to show light bulbs. However, it turns out that the
 *     quickfix support for annotations in WST is hardcoded to source validation
 *     errors *only*.
 * <li>
 *     I tried defining my own editor annotations, and customizing the icon directly
 *     by either setting an icon or using the image provider. This works fine
 *     if I make my marker be a new independent marker type. However, whenever I
 *     switch the marker type back to extend the "Problem" type, then the icon reverts
 *     back to the standard error icon and it ignores my custom settings.
 *     And if I switch away from the Problems marker type, then the errors no longer
 *     show up in the Problems view. (I also tried extending the JDT marker but that
 *     still didn't work.)
 * <li>
 *     It looks like only JDT handles quickfix icons. It has a bunch of custom code
 *     to handle this, along with its own Annotation subclass used by the editor.
 *     I tried duplicating some of this by subclassing StructuredTextEditor, but
 *     it was evident that I'd have to pull in a *huge* amount of duplicated code to
 *     make this work, which seems risky given that all this is internal code that
 *     can change from one Eclipse version to the next.
 * </ul>
 * It looks like our best bet would be to reconsider whether these should show up
 * in the Problems view; perhaps we should use a custom view for these. That would also
 * make marker management more obvious.
 */
@SuppressWarnings("restriction") // DOM model
public class LintFixGenerator implements IMarkerResolutionGenerator2, IQuickAssistProcessor {
    /** Constructs a new {@link LintFixGenerator} */
    public LintFixGenerator() {
    }

    // ---- Implements IMarkerResolutionGenerator2 ----

    @Override
    public boolean hasResolutions(IMarker marker) {
        try {
            assert marker.getType().equals(AdtConstants.MARKER_LINT);
        } catch (CoreException e) {
        }

        return true;
    }

    @Override
    public IMarkerResolution[] getResolutions(IMarker marker) {
        String id = marker.getAttribute(EclipseLintRunner.MARKER_CHECKID_PROPERTY,
                ""); //$NON-NLS-1$
        IResource resource = marker.getResource();

        List<IMarkerResolution> resolutions = new ArrayList<IMarkerResolution>();

        if (resource.getName().endsWith(DOT_JAVA)) {
            AddSuppressAnnotation.createFixes(marker, id, resolutions);
        }

        resolutions.add(new MoreInfoProposal(id, marker.getAttribute(IMarker.MESSAGE, null)));
        resolutions.add(new SuppressProposal(resource, id, false));
        resolutions.add(new SuppressProposal(resource.getProject(), id, true /* all */));
        resolutions.add(new SuppressProposal(resource, id, true /* all */));
        resolutions.add(new ClearMarkersProposal(resource, true /* all */));

        if (resolutions.size() > 0) {
            return resolutions.toArray(new IMarkerResolution[resolutions.size()]);
        }

        return null;
    }

    // ---- Implements IQuickAssistProcessor ----

    @Override
    public String getErrorMessage() {
        return "Disable Lint Error";
    }

    @Override
    public boolean canFix(Annotation annotation) {
        return true;
    }

    @Override
    public boolean canAssist(IQuickAssistInvocationContext invocationContext) {
        return true;
    }

    @Override
    public ICompletionProposal[] computeQuickAssistProposals(
            IQuickAssistInvocationContext invocationContext) {
        ISourceViewer sourceViewer = invocationContext.getSourceViewer();
        AndroidXmlEditor editor = AndroidXmlEditor.fromTextViewer(sourceViewer);
        if (editor != null) {
            IFile file = editor.getInputFile();
            if (file == null) {
                return null;
            }
            IDocument document = sourceViewer.getDocument();
            List<IMarker> markers = AdtUtils.findMarkersOnLine(AdtConstants.MARKER_LINT,
                    file, document, invocationContext.getOffset());
            List<ICompletionProposal> proposals = new ArrayList<ICompletionProposal>();
            if (markers.size() > 0) {
                for (IMarker marker : markers) {
                    String id = marker.getAttribute(EclipseLintRunner.MARKER_CHECKID_PROPERTY,
                            ""); //$NON-NLS-1$

                    // TODO: Allow for more than one fix?
                    List<LintFix> fixes = LintFix.getFixes(id, marker);
                    if (fixes != null) {
                        for (LintFix fix : fixes) {
                            proposals.add(fix);
                        }
                    }

                    String message = marker.getAttribute(IMarker.MESSAGE, null);
                    proposals.add(new MoreInfoProposal(id, message));

                    proposals.addAll(AddSuppressAttribute.createFixes(editor, marker, id));
                    proposals.add(new SuppressProposal(file, id, false));
                    proposals.add(new SuppressProposal(file.getProject(), id, true /* all */));
                    proposals.add(new SuppressProposal(file, id, true /* all */));

                    proposals.add(new ClearMarkersProposal(file, true /* all */));
                }
            }
            if (proposals.size() > 0) {
                return proposals.toArray(new ICompletionProposal[proposals.size()]);
            }
        }

        return null;
    }

    /**
     * Suppress the given detector, and rerun the checks on the file
     *
     * @param id the id of the detector to be suppressed, or null
     * @param updateMarkers if true, update all markers
     * @param resource the resource associated with the markers
     * @param thisFileOnly if true, only suppress this issue in this file
     */
    public static void suppressDetector(String id, boolean updateMarkers, IResource resource,
            boolean thisFileOnly) {
        IssueRegistry registry = EclipseLintClient.getRegistry();
        Issue issue = registry.getIssue(id);
        if (issue != null) {
            EclipseLintClient mClient = new EclipseLintClient(registry,
                    Collections.singletonList(resource), null, false);
            Project project = null;
            IProject eclipseProject = resource.getProject();
            if (eclipseProject != null) {
                File dir = AdtUtils.getAbsolutePath(eclipseProject).toFile();
                project = mClient.getProject(dir, dir);
            }
            Configuration configuration = mClient.getConfigurationFor(project);
            if (thisFileOnly && configuration instanceof DefaultConfiguration) {
                File file = AdtUtils.getAbsolutePath(resource).toFile();
                ((DefaultConfiguration) configuration).ignore(issue, file);
            } else {
                configuration.setSeverity(issue, Severity.IGNORE);
            }
        }

        if (updateMarkers) {
            EclipseLintClient.removeMarkers(resource, id);
        }
    }

    /**
     * Adds a suppress lint annotation or attribute depending on whether the
     * error is in a Java or XML file.
     *
     * @param marker the marker pointing to the error to be suppressed
     */
    public static void addSuppressAnnotation(IMarker marker) {
        String id = EclipseLintClient.getId(marker);
        if (id != null) {
            IResource resource = marker.getResource();
            if (!(resource instanceof IFile)) {
                return;
            }
            IFile file = (IFile) resource;
            boolean isJava = file.getName().endsWith(DOT_JAVA);
            boolean isXml = SdkUtils.endsWith(file.getName(), DOT_XML);
            if (!isJava && !isXml) {
                return;
            }

            try {
                // See if the current active file is the one containing this marker;
                // if so we can take some shortcuts
                IEditorPart activeEditor = AdtUtils.getActiveEditor();
                IEditorPart part = null;
                if (activeEditor != null) {
                    IEditorInput input = activeEditor.getEditorInput();
                    if (input instanceof FileEditorInput
                            && ((FileEditorInput)input).getFile().equals(file)) {
                        part = activeEditor;
                    }
                }
                if (part == null) {
                    IRegion region = null;
                    int start = marker.getAttribute(IMarker.CHAR_START, -1);
                    int end = marker.getAttribute(IMarker.CHAR_END, -1);
                    if (start != -1 && end != -1) {
                        region = new Region(start, end - start);
                    }
                    part = AdtPlugin.openFile(file, region, true /* showEditor */);
                }

                if (isJava) {
                    List<IMarkerResolution> resolutions = new ArrayList<IMarkerResolution>();
                    AddSuppressAnnotation.createFixes(marker, id, resolutions);
                    if (resolutions.size() > 0) {
                        resolutions.get(0).run(marker);
                    }
                } else {
                    assert isXml;
                    if (part instanceof AndroidXmlEditor) {
                        AndroidXmlEditor editor = (AndroidXmlEditor) part;
                        List<AddSuppressAttribute> fixes = AddSuppressAttribute.createFixes(editor,
                                marker, id);
                        if (fixes.size() > 0) {
                            IStructuredDocument document = editor.getStructuredDocument();
                            fixes.get(0).apply(document);
                        }
                    }
                }
            } catch (PartInitException pie) {
                AdtPlugin.log(pie, null);
            }
        }
    }

    private static class SuppressProposal implements ICompletionProposal, IMarkerResolution2 {
        private final String mId;
        private final boolean mGlobal;
        private final IResource mResource;

        private SuppressProposal(IResource resource, String check, boolean global) {
            mResource = resource;
            mId = check;
            mGlobal = global;
        }

        private void perform() {
            suppressDetector(mId, true, mResource, !mGlobal);
        }

        @Override
        public String getDisplayString() {
            if (mResource instanceof IProject) {
                return "Disable Check in This Project";
            } else if (mGlobal) {
                return "Disable Check";
            } else {
                return "Disable Check in This File Only";
            }
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

        // ---- Implements ICompletionProposal ----

        @Override
        public void apply(IDocument document) {
            perform();
        }

        @Override
        public Point getSelection(IDocument document) {
            return null;
        }

        @Override
        public String getAdditionalProposalInfo() {
            StringBuilder sb = new StringBuilder(200);
            if (mResource instanceof IProject) {
                sb.append("Suppresses this type of lint warning in the current project only.");
            } else if (mGlobal) {
                sb.append("Suppresses this type of lint warning in all files.");
            } else {
                sb.append("Suppresses this type of lint warning in the current file only.");
            }
            sb.append("<br><br>"); //$NON-NLS-1$
            sb.append("You can re-enable checks from the \"Android > Lint Error Checking\" preference page.");

            return sb.toString();
        }

        @Override
        public Image getImage() {
            ISharedImages sharedImages = PlatformUI.getWorkbench().getSharedImages();
            return sharedImages.getImage(ISharedImages.IMG_OBJS_WARN_TSK);
        }

        @Override
        public IContextInformation getContextInformation() {
            return null;
        }
    }

    private static class ClearMarkersProposal implements ICompletionProposal, IMarkerResolution2 {
        private final boolean mGlobal;
        private final IResource mResource;

        public ClearMarkersProposal(IResource resource, boolean global) {
            mResource = resource;
            mGlobal = global;
        }

        private void perform() {
            IResource resource = mGlobal ? mResource.getProject() : mResource;
            EclipseLintClient.clearMarkers(resource);
        }

        @Override
        public String getDisplayString() {
            return mGlobal ? "Clear All Lint Markers" : "Clear Markers in This File Only";
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

        // ---- Implements ICompletionProposal ----

        @Override
        public void apply(IDocument document) {
            perform();
        }

        @Override
        public Point getSelection(IDocument document) {
            return null;
        }

        @Override
        public String getAdditionalProposalInfo() {
            StringBuilder sb = new StringBuilder(200);
            if (mGlobal) {
                sb.append("Clears all lint warning markers from the project.");
            } else {
                sb.append("Clears all lint warnings from this file.");
            }
            sb.append("<br><br>"); //$NON-NLS-1$
            sb.append("This temporarily hides the problem, but does not suppress it. " +
                    "Running Lint again can bring the error back.");
            if (AdtPrefs.getPrefs().isLintOnSave()) {
                sb.append(' ');
                sb.append("This will happen the next time the file is saved since lint-on-save " +
                        "is enabled. You can turn this off in the \"Lint Error Checking\" " +
                        "preference page.");
            }

            return sb.toString();
        }

        @Override
        public Image getImage() {
            ISharedImages sharedImages = PlatformUI.getWorkbench().getSharedImages();
            return sharedImages.getImage(ISharedImages.IMG_ELCL_REMOVE);
        }

        @Override
        public IContextInformation getContextInformation() {
            return null;
        }
    }

    private static class MoreInfoProposal implements ICompletionProposal, IMarkerResolution2 {
        private final String mId;
        private final String mMessage;

        public MoreInfoProposal(String id, String message) {
            mId = id;
            mMessage = message;
        }

        private void perform() {
            Issue issue = EclipseLintClient.getRegistry().getIssue(mId);
            assert issue != null : mId;

            StringBuilder sb = new StringBuilder(300);
            sb.append(mMessage);
            sb.append('\n').append('\n');
            sb.append("Issue Explanation:");
            sb.append('\n');
            String explanation = issue.getExplanation(Issue.OutputFormat.TEXT);
            if (explanation != null && !explanation.isEmpty()) {
                sb.append('\n');
                sb.append(explanation);
            } else {
                sb.append(issue.getDescription(Issue.OutputFormat.TEXT));
            }

            if (issue.getMoreInfo() != null) {
                sb.append('\n').append('\n');
                sb.append("More Information: ");
                sb.append(issue.getMoreInfo());
            }

            MessageDialog.openInformation(AdtPlugin.getShell(), "More Info",
                    sb.toString());
        }

        @Override
        public String getDisplayString() {
            return String.format("Explain Issue (%1$s)", mId);
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

        // ---- Implements ICompletionProposal ----

        @Override
        public void apply(IDocument document) {
            perform();
        }

        @Override
        public Point getSelection(IDocument document) {
            return null;
        }

        @Override
        public String getAdditionalProposalInfo() {
            return "Provides more information about this issue."
                    + "<br><br>" //$NON-NLS-1$
                    + EclipseLintClient.getRegistry().getIssue(mId).getExplanation(
                            OutputFormat.HTML);
        }

        @Override
        public Image getImage() {
            ISharedImages sharedImages = PlatformUI.getWorkbench().getSharedImages();
            return sharedImages.getImage(ISharedImages.IMG_OBJS_INFO_TSK);
        }

        @Override
        public IContextInformation getContextInformation() {
            return null;
        }
    }
}

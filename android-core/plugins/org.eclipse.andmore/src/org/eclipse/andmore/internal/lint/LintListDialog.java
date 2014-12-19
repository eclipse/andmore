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

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.AdtUtils;
import org.eclipse.andmore.internal.editors.AndroidXmlEditor;
import org.eclipse.andmore.internal.editors.IconFactory;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SuppressWarnings("restriction") // WST DOM access
class LintListDialog extends TitleAreaDialog implements SelectionListener {
    private static final String PROJECT_LOGO_LARGE = "android-64"; //$NON-NLS-1$
    private final IFile mFile;
    private final IEditorPart mEditor;
    private Button mFixButton;
    private Button mIgnoreButton;
    private Button mIgnoreAllButton;
    private Button mShowButton;
    private Text mDetailsText;
    private Button mIgnoreTypeButton;
    private LintList mList;

    LintListDialog(
            @NonNull Shell parentShell,
            @NonNull IFile file,
            @Nullable IEditorPart editor) {
        super(parentShell);
        mFile = file;
        mEditor = editor;
        setHelpAvailable(false);
    }

    @Override
    protected void setShellStyle(int newShellStyle) {
        // Allow resize
        super.setShellStyle(newShellStyle | SWT.TITLE | SWT.MODELESS | SWT.RESIZE);
    }

    @Override
    public boolean close() {
        mList.dispose();
        return super.close();
    }

    @Override
    protected Control createContents(Composite parent) {
      Control contents = super.createContents(parent);
      setTitle("Lint Warnings in Layout");
      setMessage("Lint Errors found for the current layout:");
      setTitleImage(IconFactory.getInstance().getIcon(PROJECT_LOGO_LARGE));

      return contents;
    }

    @SuppressWarnings("unused") // SWT constructors have side effects, they are not unused
    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);
        Composite container = new Composite(area, SWT.NONE);
        container.setLayoutData(new GridData(GridData.FILL_BOTH));

        container.setLayout(new GridLayout(2, false));
        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        IWorkbenchPartSite site = null;
        if (page.getActivePart() != null) {
            site = page.getActivePart().getSite();
        }

        mList = new LintList(site, container, null /*memento*/, true /*singleFile*/);
        mList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 6));

        mShowButton = new Button(container, SWT.NONE);
        mShowButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        mShowButton.setText("Show");
        mShowButton.setToolTipText("Opens the editor to reveal the XML with the issue");
        mShowButton.addSelectionListener(this);

        mFixButton = new Button(container, SWT.NONE);
        mFixButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        mFixButton.setText("Fix");
        mFixButton.setToolTipText("Automatically corrects the problem, if possible");
        mFixButton.setEnabled(false);
        mFixButton.addSelectionListener(this);

        mIgnoreButton = new Button(container, SWT.NONE);
        mIgnoreButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        mIgnoreButton.setText("Suppress Issue");
        mIgnoreButton.setToolTipText("Adds a special attribute in the layout to suppress this specific warning");
        mIgnoreButton.addSelectionListener(this);

        mIgnoreAllButton = new Button(container, SWT.NONE);
        mIgnoreAllButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        mIgnoreAllButton.setText("Suppress in Layout");
        mIgnoreAllButton.setEnabled(mEditor instanceof AndroidXmlEditor);
        mIgnoreAllButton.setToolTipText("Adds an attribute on the root element to suppress all issues of this type in this layout");
        mIgnoreAllButton.addSelectionListener(this);

        mIgnoreTypeButton = new Button(container, SWT.NONE);
        mIgnoreTypeButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        mIgnoreTypeButton.setText("Disable Issue Type");
        mIgnoreTypeButton.setToolTipText("Turns off checking for this type of error everywhere");
        mIgnoreTypeButton.addSelectionListener(this);

        new Label(container, SWT.NONE);

        mDetailsText = new Text(container, SWT.BORDER | SWT.READ_ONLY | SWT.WRAP
                | SWT.V_SCROLL | SWT.MULTI);
        Display display = parent.getDisplay();
        mDetailsText.setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
        mDetailsText.setForeground(display.getSystemColor(SWT.COLOR_INFO_FOREGROUND));
        GridData gdText = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
        gdText.heightHint = 80;
        mDetailsText.setLayoutData(gdText);

        new Label(container, SWT.NONE);

        mList.addSelectionListener(this);

        mList.setResources(Collections.<IResource>singletonList(mFile));
        mList.selectFirst();
        if (mList.getSelectedMarkers().size() > 0) {
            updateSelectionState();
        }

        return area;
    }

    /**
     * Create contents of the button bar.
     */
    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
    }

    /**
     * Return the initial size of the dialog.
     */
    @Override
    protected Point getInitialSize() {
        return new Point(600, 400);
    }

    private void selectMarker(IMarker marker) {
        if (marker == null) {
            mDetailsText.setText(""); //$NON-NLS-1$
            return;
        }

        mDetailsText.setText(EclipseLintClient.describe(marker));
    }

    // ---- Implements SelectionListener ----

    @Override
    public void widgetSelected(SelectionEvent e) {
        Object source = e.getSource();
        if (source == mList.getTreeViewer().getControl()) {
            // Enable/disable buttons
            updateSelectionState();
        } else if (source == mShowButton) {
            List<IMarker> selection = mList.getSelectedMarkers();
            if (selection.size() > 0) {
                EclipseLintClient.showMarker(selection.get(0));
            }
        } else if (source == mFixButton) {
            List<IMarker> selection = mList.getSelectedMarkers();
            for (IMarker marker : selection) {
                List<LintFix> fixes = LintFix.getFixes(EclipseLintClient.getId(marker), marker);
                if (fixes == null) {
                    continue;
                }
                LintFix fix = fixes.get(0);
                IEditorPart editor = AdtUtils.getActiveEditor();
                if (editor instanceof AndroidXmlEditor) {
                    IStructuredDocument doc = ((AndroidXmlEditor) editor).getStructuredDocument();
                    fix.apply(doc);
                    if (fix.needsFocus()) {
                        close();
                    }
                } else {
                    AdtPlugin.log(IStatus.ERROR, "Did not find associated editor to apply fix");
                }
            }
        } else if (source == mIgnoreTypeButton) {
            for (IMarker marker : mList.getSelectedMarkers()) {
                String id = EclipseLintClient.getId(marker);
                if (id != null) {
                    LintFixGenerator.suppressDetector(id, true, mFile, true /*all*/);
                }
            }
        } else if (source == mIgnoreButton) {
            for (IMarker marker : mList.getSelectedMarkers()) {
                LintFixGenerator.addSuppressAnnotation(marker);
            }
        } else if (source == mIgnoreAllButton) {
            Set<String> ids = new HashSet<String>();
            for (IMarker marker : mList.getSelectedMarkers()) {
                String id = EclipseLintClient.getId(marker);
                if (id != null && !ids.contains(id)) {
                    ids.add(id);
                    if (mEditor instanceof AndroidXmlEditor) {
                        AndroidXmlEditor editor = (AndroidXmlEditor) mEditor;
                        AddSuppressAttribute fix = AddSuppressAttribute.createFixForAll(editor,
                                marker, id);
                        if (fix != null) {
                            IStructuredDocument document = editor.getStructuredDocument();
                            fix.apply(document);
                        }
                    }
                }
            }
            mList.refresh();
        }
    }

    private void updateSelectionState() {
        List<IMarker> selection = mList.getSelectedMarkers();

        if (selection.size() == 1) {
            selectMarker(selection.get(0));
        } else {
            selectMarker(null);
        }

        boolean canFix = selection.size() > 0;
        for (IMarker marker : selection) {
            if (!LintFix.hasFix(EclipseLintClient.getId(marker))) {
                canFix = false;
                break;
            }

            // Some fixes cannot be run in bulk
            if (selection.size() > 1) {
                List<LintFix> fixes = LintFix.getFixes(EclipseLintClient.getId(marker), marker);
                if (fixes == null || !fixes.get(0).isBulkCapable()) {
                    canFix = false;
                    break;
                }
            }
        }

        mFixButton.setEnabled(canFix);
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent e) {
        Object source = e.getSource();
        if (source == mList.getTreeViewer().getControl()) {
            // Jump to editor
            List<IMarker> selection = mList.getSelectedMarkers();
            if (selection.size() > 0) {
                EclipseLintClient.showMarker(selection.get(0));
                close();
            }
        }
    }
}

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
package org.eclipse.andmore.internal.wizards.templates;

import static org.eclipse.andmore.internal.wizards.templates.NewProjectWizard.CATEGORY_ACTIVITIES;
import static org.eclipse.andmore.internal.wizards.templates.NewProjectWizard.CATEGORY_OTHER;
import static org.eclipse.andmore.internal.wizards.templates.NewProjectWizard.IS_LAUNCHER;
import static org.eclipse.andmore.internal.wizards.templates.TemplateHandler.PREVIEW_PADDING;
import static org.eclipse.andmore.internal.wizards.templates.TemplateHandler.PREVIEW_WIDTH;

import com.google.common.collect.Lists;
import com.google.common.io.Files;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.editors.layout.gle2.ImageControl;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

class ActivityPage extends WizardPage implements SelectionListener {
    private final NewProjectWizardState mValues;
    private List mList;
    private Button mCreateToggle;
    private java.util.List<File> mTemplates;

    private boolean mIgnore;
    private boolean mShown;
    private ImageControl mPreview;
    private Image mPreviewImage;
    private boolean mDisposePreviewImage;
    private Label mHeading;
    private Label mDescription;
    private boolean mOnlyActivities;
    private boolean mAskCreate;
    private boolean mLauncherActivitiesOnly;

    /**
     * Create the wizard.
     */
    ActivityPage(NewProjectWizardState values, boolean onlyActivities, boolean askCreate) {
        super("activityPage"); //$NON-NLS-1$
        mValues = values;
        mOnlyActivities = onlyActivities;
        mAskCreate = askCreate;

        if (onlyActivities) {
            setTitle("Create Activity");
        } else {
            setTitle("Create Android Object");
        }
        if (onlyActivities && askCreate) {
            setDescription(
                    "Select whether to create an activity, and if so, what kind of activity.");
        } else {
            setDescription("Select which template to use");
        }
    }

    /** Sets whether the activity page should only offer launcher activities */
    void setLauncherActivitiesOnly(boolean launcherActivitiesOnly) {
        mLauncherActivitiesOnly = launcherActivitiesOnly;
    }

    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NULL);
        setControl(container);
    }

    @SuppressWarnings("unused") // SWT constructors have side effects and aren't unused
    private void onEnter() {
        Composite container = (Composite) getControl();
        container.setLayout(new GridLayout(3, false));

        if (mAskCreate) {
            mCreateToggle = new Button(container, SWT.CHECK);
            mCreateToggle.setSelection(true);
            mCreateToggle.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
            mCreateToggle.setText("Create Activity");
            mCreateToggle.addSelectionListener(this);
        }

        mList = new List(container, SWT.BORDER | SWT.V_SCROLL);
        mList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));


        TemplateManager manager = mValues.template.getManager();
        java.util.List<File> templates = manager.getTemplates(CATEGORY_ACTIVITIES);

        if (!mOnlyActivities) {
            templates.addAll(manager.getTemplates(CATEGORY_OTHER));
        }
        java.util.List<String> names = new ArrayList<String>(templates.size());
        File current = mValues.activityValues.getTemplateLocation();
        mTemplates = Lists.newArrayListWithExpectedSize(templates.size());
        int index = -1;
        for (int i = 0, n = templates.size(); i < n; i++) {
            File template = templates.get(i);
            TemplateMetadata metadata = manager.getTemplate(template);
            if (metadata == null) {
                continue;
            }
            if (mLauncherActivitiesOnly) {
                Parameter parameter = metadata.getParameter(IS_LAUNCHER);
                if (parameter == null) {
                    continue;
                }
            }
            mTemplates.add(template);
            names.add(metadata.getTitle());
            if (template.equals(current)) {
                index = names.size();
            }
        }
        String[] items = names.toArray(new String[names.size()]);
        mList.setItems(items);
        if (index == -1 && !mTemplates.isEmpty()) {
            mValues.activityValues.setTemplateLocation(mTemplates.get(0));
            index = 0;
        }
        if (index >= 0) {
            mList.setSelection(index);
            mList.addSelectionListener(this);
        }

        // Preview
        mPreview = new ImageControl(container, SWT.NONE, null);
        mPreview.setDisposeImage(false); // Handled manually in this class
        GridData gd_mImage = new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1);
        gd_mImage.widthHint = PREVIEW_WIDTH + 2 * PREVIEW_PADDING;
        mPreview.setLayoutData(gd_mImage);
        new Label(container, SWT.NONE);

        mHeading = new Label(container, SWT.NONE);
        mHeading.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
        new Label(container, SWT.NONE);

        mDescription = new Label(container, SWT.WRAP);
        mDescription.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1));

        Font font = JFaceResources.getFontRegistry().getBold(JFaceResources.BANNER_FONT);
        if (font != null) {
            mHeading.setFont(font);
        }

        updatePreview();
    }

    private void updatePreview() {
        Image oldImage = mPreviewImage;
        boolean dispose = mDisposePreviewImage;
        mPreviewImage = null;

        String title = "";
        String description = "";
        TemplateHandler handler = mValues.activityValues.getTemplateHandler();
        TemplateMetadata template = handler.getTemplate();
        if (template != null) {
            String thumb = template.getThumbnailPath();
            if (thumb != null && !thumb.isEmpty()) {
                File file = new File(mValues.activityValues.getTemplateLocation(),
                        thumb.replace('/', File.separatorChar));
                if (file != null) {
                    try {
                        byte[] bytes = Files.toByteArray(file);
                        ByteArrayInputStream input = new ByteArrayInputStream(bytes);
                        mPreviewImage = new Image(getControl().getDisplay(), input);
                        mDisposePreviewImage = true;
                        input.close();
                    } catch (IOException e) {
                        AdtPlugin.log(e, null);
                    }
                }
            } else {
                // Fallback icon
                mDisposePreviewImage = false;
                mPreviewImage = TemplateMetadata.getDefaultTemplateIcon();
            }
            title = template.getTitle();
            description = template.getDescription();
        }

        mHeading.setText(title);
        mDescription.setText(description);
        mPreview.setImage(mPreviewImage);
        mPreview.fitToWidth(PREVIEW_WIDTH);

        if (oldImage != null && dispose) {
            oldImage.dispose();
        }

        Composite parent = (Composite) getControl();
        parent.layout(true, true);
        parent.redraw();
    }

    @Override
    public void dispose() {
        super.dispose();

        if (mPreviewImage != null && mDisposePreviewImage) {
            mDisposePreviewImage = false;
            mPreviewImage.dispose();
            mPreviewImage = null;
        }
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible && !mShown) {
            onEnter();
        }

        super.setVisible(visible);

        if (visible) {
            mShown = true;
            if (mAskCreate) {
                try {
                    mIgnore = true;
                    mCreateToggle.setSelection(mValues.createActivity);
                } finally {
                    mIgnore = false;
                }
            }
        }

        validatePage();
    }


    private void validatePage() {
        IStatus status = null;

        if (mValues.createActivity) {
            if (mList.getSelectionCount() < 1) {
                status = new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID,
                        "Select an activity type");
            } else {
                TemplateHandler templateHandler = mValues.activityValues.getTemplateHandler();
                status = templateHandler.validateTemplate(mValues.minSdkLevel,
                        mValues.getBuildApi());
            }
        }

        setPageComplete(status == null || status.getSeverity() != IStatus.ERROR);
        if (status != null) {
            setMessage(status.getMessage(),
                    status.getSeverity() == IStatus.ERROR
                        ? IMessageProvider.ERROR : IMessageProvider.WARNING);
        } else {
            setErrorMessage(null);
            setMessage(null);
        }
    }

    @Override
    public boolean isPageComplete() {
        // Ensure that the Finish button isn't enabled until
        // the user has reached and completed this page
        if (!mShown && mValues.createActivity) {
            return false;
        }

        return super.isPageComplete();
    }

    // ---- Implements SelectionListener ----

    @Override
    public void widgetSelected(SelectionEvent e) {
        if (mIgnore) {
            return;
        }

        Object source = e.getSource();
        if (source == mCreateToggle) {
            mValues.createActivity = mCreateToggle.getSelection();
            mList.setEnabled(mValues.createActivity);
        } else if (source == mList) {
            int index = mList.getSelectionIndex();
            if (index >= 0 && index < mTemplates.size()) {
                File template = mTemplates.get(index);
                mValues.activityValues.setTemplateLocation(template);
                updatePreview();
            }
        }

        validatePage();
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent e) {
    }
}

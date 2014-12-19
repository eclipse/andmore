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

package org.eclipse.andmore.internal.assetstudio;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static org.eclipse.andmore.internal.wizards.templates.NewProjectWizard.DEFAULT_LAUNCHER_ICON;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.assetstudiolib.ActionBarIconGenerator;
import com.android.assetstudiolib.GraphicGenerator;
import com.android.assetstudiolib.GraphicGenerator.Shape;
import com.android.assetstudiolib.LauncherIconGenerator;
import com.android.assetstudiolib.MenuIconGenerator;
import com.android.assetstudiolib.NotificationIconGenerator;
import com.android.assetstudiolib.TabIconGenerator;
import com.android.assetstudiolib.TextRenderUtil;
import com.android.assetstudiolib.Util;
import com.android.utils.Pair;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.AdtUtils;
import org.eclipse.andmore.internal.assetstudio.CreateAssetSetWizardState.SourceType;
import org.eclipse.andmore.internal.editors.layout.gle2.ImageControl;
import org.eclipse.andmore.internal.editors.layout.gle2.ImageUtils;
import org.eclipse.andmore.internal.editors.layout.gle2.SwtUtils;
import org.eclipse.andmore.internal.editors.manifest.ManifestInfo;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.FontDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Slider;
import org.eclipse.swt.widgets.Text;

import java.awt.Paint;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.imageio.ImageIO;

/**
 * This is normally page 2 of a Create New Asset Set wizard, unless we can offer actions
 * to create a specific asset type, in which case we skip page 1. On this page the user
 * gets to configure the parameters of the asset, and see a preview.
 */
public class ConfigureAssetSetPage extends WizardPage implements SelectionListener,
        ModifyListener {
    private final CreateAssetSetWizardState mValues;

    private static final int PREVIEW_AREA_WIDTH = 144;

    private boolean mShown;

    private Composite mConfigurationArea;
    private Button mImageRadio;
    private Button mClipartRadio;
    private Button mTextRadio;
    private Button mPickImageButton;
    private Button mTrimCheckBox;
    private Slider mPaddingSlider;
    private Label mPercentLabel;
    private Button mCropRadio;
    private Button mCenterRadio;
    private Button mNoShapeRadio;
    private Button mSquareRadio;
    private Button mCircleButton;
    private Button mBgButton;
    private Button mFgButton;
    private Composite mPreviewArea;
    private Button mFontButton;
    private Composite mForegroundArea;
    private Composite mImageForm;
    private Composite mClipartForm;
    private Composite mTextForm;
    private Text mImagePathText;

    private boolean mTimerPending;
    private RGB mBgColor;
    private RGB mFgColor;
    private Text mText;

    /** Most recently set image path: preserved across wizard sessions */
    private static String sImagePath;
    private Button mChooseClipart;
    private Composite mClipartPreviewPanel;
    private Label mThemeLabel;
    private Composite mThemeComposite;
    private Button mHoloLightRadio;
    private Button mHoloDarkRadio;
    private Label mScalingLabel;
    private Composite mScalingComposite;
    private Label mShapeLabel;
    private Composite mShapeComposite;
    private Label mBgColorLabel;
    private Label mFgColorLabel;

    private boolean mIgnore;
    private SourceType mShowingType;

    /**
     * Create the wizard.
     *
     * @param values the wizard state
     */
    public ConfigureAssetSetPage(CreateAssetSetWizardState values) {
        super("configureAssetPage");
        mValues = values;

        setTitle("Configure Icon Set");
        setDescription("Configure the attributes of the icon set");
    }

    /**
     * Create contents of the wizard.
     *
     * @param parent the parent widget
     */
    @Override
    @SuppressWarnings("unused") // Don't warn about unassigned "new Label(.)": has side-effect
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NULL);

        setControl(container);
        GridLayout glContainer = new GridLayout(2, false);
        glContainer.marginWidth = 0;
        glContainer.horizontalSpacing = 0;
        glContainer.marginHeight = 0;
        glContainer.verticalSpacing = 0;
        container.setLayout(glContainer);

        ScrolledComposite configurationScrollArea = new ScrolledComposite(container, SWT.V_SCROLL);
        configurationScrollArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 2));
        configurationScrollArea.setExpandHorizontal(true);
        configurationScrollArea.setExpandVertical(true);

        mConfigurationArea = new Composite(configurationScrollArea, SWT.NONE);
        GridLayout glConfigurationArea = new GridLayout(3, false);
        glConfigurationArea.horizontalSpacing = 0;
        glConfigurationArea.marginRight = 15;
        glConfigurationArea.marginWidth = 0;
        glConfigurationArea.marginHeight = 0;
        mConfigurationArea.setLayout(glConfigurationArea);

        Label foregroundLabel = new Label(mConfigurationArea, SWT.NONE);
        foregroundLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        foregroundLabel.setText("Foreground:");

        Composite foregroundComposite = new Composite(mConfigurationArea, SWT.NONE);
        foregroundComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
        GridLayout glForegroundComposite = new GridLayout(5, false);
        glForegroundComposite.horizontalSpacing = 0;
        foregroundComposite.setLayout(glForegroundComposite);

        mImageRadio = new Button(foregroundComposite, SWT.FLAT | SWT.TOGGLE);
        mImageRadio.setSelection(false);
        mImageRadio.addSelectionListener(this);
        mImageRadio.setText("Image");

        mClipartRadio = new Button(foregroundComposite, SWT.FLAT | SWT.TOGGLE);
        mClipartRadio.setText("Clipart");
        mClipartRadio.addSelectionListener(this);

        mTextRadio = new Button(foregroundComposite, SWT.FLAT | SWT.TOGGLE);
        mTextRadio.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
        mTextRadio.setText("Text");
        mTextRadio.addSelectionListener(this);
        new Label(mConfigurationArea, SWT.NONE);

        mForegroundArea = new Composite(mConfigurationArea, SWT.NONE);
        mForegroundArea.setLayout(new StackLayout());
        mForegroundArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));

        mImageForm = new Composite(mForegroundArea, SWT.NONE);
        mImageForm.setLayout(new GridLayout(3, false));

        Label fileLabel = new Label(mImageForm, SWT.NONE);
        fileLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        fileLabel.setText("Image File:");

        mImagePathText = new Text(mImageForm, SWT.BORDER);
        GridData pathLayoutData = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
        pathLayoutData.widthHint = 200;
        mImagePathText.setLayoutData(pathLayoutData);
        mImagePathText.addSelectionListener(this);
        mImagePathText.addModifyListener(this);

        mPickImageButton = new Button(mImageForm, SWT.FLAT);
        mPickImageButton.setText("Browse...");
        mPickImageButton.addSelectionListener(this);

        mClipartForm = new Composite(mForegroundArea, SWT.NONE);
        mClipartForm.setLayout(new GridLayout(2, false));

        mChooseClipart = new Button(mClipartForm, SWT.FLAT);
        mChooseClipart.setText("Choose...");
        mChooseClipart.addSelectionListener(this);

        mClipartPreviewPanel = new Composite(mClipartForm, SWT.NONE);
        RowLayout rlClipartPreviewPanel = new RowLayout(SWT.HORIZONTAL);
        rlClipartPreviewPanel.marginBottom = 0;
        rlClipartPreviewPanel.marginTop = 0;
        rlClipartPreviewPanel.center = true;
        mClipartPreviewPanel.setLayout(rlClipartPreviewPanel);
        mClipartPreviewPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

        mTextForm = new Composite(mForegroundArea, SWT.NONE);
        mTextForm.setLayout(new GridLayout(2, false));

        Label textLabel = new Label(mTextForm, SWT.NONE);
        textLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        textLabel.setText("Text:");

        mText = new Text(mTextForm, SWT.BORDER);
        mText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        mText.addModifyListener(this);

        Label fontLabel = new Label(mTextForm, SWT.NONE);
        fontLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        fontLabel.setText("Font:");

        mFontButton = new Button(mTextForm, SWT.FLAT);
        mFontButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
        mFontButton.addSelectionListener(this);
        mFontButton.setText("Choose Font...");
        new Label(mConfigurationArea, SWT.NONE);

        mTrimCheckBox = new Button(mConfigurationArea, SWT.CHECK);
        mTrimCheckBox.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
        mTrimCheckBox.setSelection(false);
        mTrimCheckBox.setText("Trim Surrounding Blank Space");
        mTrimCheckBox.addSelectionListener(this);
        new Label(mConfigurationArea, SWT.NONE);

        Label paddingLabel = new Label(mConfigurationArea, SWT.NONE);
        paddingLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
        paddingLabel.setText("Additional Padding:");
        new Label(mConfigurationArea, SWT.NONE);

        mPaddingSlider = new Slider(mConfigurationArea, SWT.NONE);
        mPaddingSlider.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        // This doesn't work right -- not sure why. For now just use a plain slider
        // and subtract 10 from it to get the real range.
        //mPaddingSlider.setValues(0, -10, 50, 0, 1, 10);
        //mPaddingSlider.setSelection(10 + 15);
        mPaddingSlider.addSelectionListener(this);

        mPercentLabel = new Label(mConfigurationArea, SWT.NONE);
        mPercentLabel.setText("  15%"); // Enough available space for -10%
        mScalingLabel = new Label(mConfigurationArea, SWT.NONE);
        mScalingLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        mScalingLabel.setText("Foreground Scaling:");

        mScalingComposite = new Composite(mConfigurationArea, SWT.NONE);
        mScalingComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
        GridLayout gl_mScalingComposite = new GridLayout(5, false);
        gl_mScalingComposite.horizontalSpacing = 0;
        mScalingComposite.setLayout(gl_mScalingComposite);

        mCropRadio = new Button(mScalingComposite, SWT.FLAT | SWT.TOGGLE);
        mCropRadio.setSelection(true);
        mCropRadio.setText("Crop");
        mCropRadio.addSelectionListener(this);

        mCenterRadio = new Button(mScalingComposite, SWT.FLAT | SWT.TOGGLE);
        mCenterRadio.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 4, 1));
        mCenterRadio.setText("Center");
        mCenterRadio.addSelectionListener(this);

        mShapeLabel = new Label(mConfigurationArea, SWT.NONE);
        mShapeLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        mShapeLabel.setText("Shape");

        mShapeComposite = new Composite(mConfigurationArea, SWT.NONE);
        mShapeComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
        GridLayout gl_mShapeComposite = new GridLayout(6, false);
        gl_mShapeComposite.horizontalSpacing = 0;
        mShapeComposite.setLayout(gl_mShapeComposite);

        mNoShapeRadio = new Button(mShapeComposite, SWT.FLAT | SWT.TOGGLE);
        mNoShapeRadio.setText("None");
        mNoShapeRadio.addSelectionListener(this);

        mSquareRadio = new Button(mShapeComposite, SWT.FLAT | SWT.TOGGLE);
        mSquareRadio.setSelection(true);
        mSquareRadio.setText("Square");
        mSquareRadio.addSelectionListener(this);

        mCircleButton = new Button(mShapeComposite, SWT.FLAT | SWT.TOGGLE);
        mCircleButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 4, 1));
        mCircleButton.setText("Circle");
        mCircleButton.addSelectionListener(this);

        mThemeLabel = new Label(mConfigurationArea, SWT.NONE);
        mThemeLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        mThemeLabel.setText("Theme");

        mThemeComposite = new Composite(mConfigurationArea, SWT.NONE);
        mThemeComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        GridLayout gl_mThemeComposite = new GridLayout(2, false);
        gl_mThemeComposite.horizontalSpacing = 0;
        mThemeComposite.setLayout(gl_mThemeComposite);

        mHoloLightRadio = new Button(mThemeComposite, SWT.FLAT | SWT.TOGGLE);
        mHoloLightRadio.setText("Holo Light");
        mHoloLightRadio.setSelection(true);
        mHoloLightRadio.addSelectionListener(this);

        mHoloDarkRadio = new Button(mThemeComposite, SWT.FLAT | SWT.TOGGLE);
        mHoloDarkRadio.setText("Holo Dark");
        mHoloDarkRadio.addSelectionListener(this);

        mBgColorLabel = new Label(mConfigurationArea, SWT.NONE);
        mBgColorLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        mBgColorLabel.setText("Background Color:");

        mBgButton = new Button(mConfigurationArea, SWT.FLAT);
        mBgButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
        mBgButton.addSelectionListener(this);
        mBgButton.setAlignment(SWT.CENTER);

        mFgColorLabel = new Label(mConfigurationArea, SWT.NONE);
        mFgColorLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        mFgColorLabel.setText("Foreground Color:");

        mFgButton = new Button(mConfigurationArea, SWT.FLAT);
        mFgButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
        mFgButton.setAlignment(SWT.CENTER);
        mFgButton.addSelectionListener(this);

        configurationScrollArea.setContent(mConfigurationArea);
        configurationScrollArea.setMinSize(mConfigurationArea.computeSize(SWT.DEFAULT,
                SWT.DEFAULT));

        Label previewLabel = new Label(container, SWT.NONE);
        previewLabel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1));
        previewLabel.setText("Preview:");

        mPreviewArea = new Composite(container, SWT.BORDER);

        RowLayout rlPreviewAreaPreviewArea = new RowLayout(SWT.HORIZONTAL);
        rlPreviewAreaPreviewArea.wrap = true;
        rlPreviewAreaPreviewArea.pack = true;
        rlPreviewAreaPreviewArea.center = true;
        rlPreviewAreaPreviewArea.spacing = 0;
        rlPreviewAreaPreviewArea.marginBottom = 0;
        rlPreviewAreaPreviewArea.marginTop = 0;
        rlPreviewAreaPreviewArea.marginRight = 0;
        rlPreviewAreaPreviewArea.marginLeft = 0;
        mPreviewArea.setLayout(rlPreviewAreaPreviewArea);
        GridData gdMPreviewArea = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
        gdMPreviewArea.widthHint = PREVIEW_AREA_WIDTH;
        mPreviewArea.setLayoutData(gdMPreviewArea);

        // Initial color
        Display display = parent.getDisplay();
        updateColor(display, mValues.background, true /*background*/);
        updateColor(display, mValues.foreground, false /*background*/);

        setSourceType(mValues.sourceType);

        new Label(mConfigurationArea, SWT.NONE);
        new Label(mConfigurationArea, SWT.NONE);
        new Label(mConfigurationArea, SWT.NONE);

        validatePage();
    }

    void configureAssetType(AssetType type) {
        if (mValues.sourceType != mShowingType) {
            mShowingType = mValues.sourceType;
            showGroup(type.needsForegroundScaling(), mScalingLabel, mScalingComposite);
            showGroup(type.needsShape(), mShapeLabel, mShapeComposite);
            showGroup(type.needsTheme(), mThemeLabel, mThemeComposite);
            showGroup(type.needsColors(), mBgColorLabel, mBgButton);
            showGroup(type.needsColors() && mValues.sourceType != SourceType.IMAGE,
                    mFgColorLabel, mFgButton);

            Composite parent = mScalingLabel.getParent();
            parent.pack();
            parent.layout();
        }
    }

    private static void showGroup(boolean show, Control control1, Control control2) {
        showControl(show, control1);
        showControl(show, control2);
    }

    private static void showControl(boolean show, Control control) {
        Object data = control.getLayoutData();
        if (data instanceof GridData) {
            GridData gridData = (GridData) data;
            gridData.exclude = !show;
        }
        control.setVisible(show);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);

        // We update the image selection here rather than in {@link #createControl} because
        // that method is called when the wizard is created, and we want to wait until the
        // user has chosen a project before attempting to look up the right default image to use
        if (visible) {
            mShown = true;

            // Clear out old previews - important if the user goes back to page one, changes
            // asset type and steps into page 2 - at that point we arrive here and we might
            // display the old previews for a brief period until the preview delay timer expires.
            for (Control c : mPreviewArea.getChildren()) {
                c.dispose();
            }
            mPreviewArea.layout(true);

            // Update asset type configuration: will show/hide parameter controls depending
            // on which asset type is chosen
            AssetType type = mValues.type;
            assert type != null;
            configureAssetType(type);

            // Initial image - use the most recently used image, or the default launcher
            // icon created in our default projects, if there
            if (mValues.imagePath != null) {
                sImagePath = mValues.imagePath.getPath();
            }
            if (sImagePath == null) {
                IProject project = mValues.project;
                if (project != null) {
                    IResource icon = project.findMember("res/drawable-hdpi/icon.png"); //$NON-NLS-1$
                    if (icon != null) {
                        IWorkspaceRoot workspace = ResourcesPlugin.getWorkspace().getRoot();
                        IPath workspacePath = workspace.getLocation();
                        sImagePath = workspacePath.append(icon.getFullPath()).toOSString();
                    }
                }
            }
            if (sImagePath != null) {
                mValues.imagePath = new File(sImagePath);
                mImagePathText.setText(sImagePath);
            }

            try {
                mIgnore = true;

                mTrimCheckBox.setSelection(mValues.trim);

                // This doesn't work right -- not sure why. For now just use a plain slider
                // and subtract 10 from it to get the real range.
                //mPaddingSlider.setValues(0, -10, 50, 0, 1, 10);
                //mPaddingSlider.setSelection(10 + 15);
                mPaddingSlider.setSelection(mValues.padding + 10);
                mPercentLabel.setText(Integer.toString(mValues.padding) + '%');

                if (mValues.imagePath != null) {
                    mImagePathText.setText(mValues.imagePath.getPath());
                }

                if (mValues.text != null) {
                    mText.setText(mValues.text);
                }

                setSourceType(mValues.sourceType);

                // Shape=NONE does not apply for notification icons; it's needed for API < 9
                if (mValues.shape == Shape.NONE && mValues.type == AssetType.NOTIFICATION) {
                    mValues.shape = Shape.SQUARE;
                }

                setShape(mValues.shape);
                mNoShapeRadio.setEnabled(mValues.type != AssetType.NOTIFICATION);

                if (mValues.sourceType == SourceType.CLIPART
                        && mValues.clipartName != null) {
                    updateClipartPreview();
                }

                // Initial color
                Display display = mPreviewArea.getDisplay();
                //updateColor(display, new RGB(0xa4, 0xc6, 0x39), true /*background*/);
                updateColor(display, mValues.background, true /*background*/);
                updateColor(display, mValues.foreground, false /*background*/);

                updateTrimOptions();
            } finally {
                mIgnore = false;
            }

            validatePage();

            requestUpdatePreview(true /*quickly*/);

            if (mTextRadio.getSelection()) {
                mText.setFocus();
            }
        }
    }

    private void setSourceType(CreateAssetSetWizardState.SourceType sourceType) {
        if (sourceType == CreateAssetSetWizardState.SourceType.IMAGE) {
            chooseForegroundTab(mImageRadio, mImageForm);
        } else if (sourceType == CreateAssetSetWizardState.SourceType.CLIPART) {
            chooseForegroundTab(mClipartRadio, mClipartForm);
            mChooseClipart.setFocus();
        } else if (sourceType == CreateAssetSetWizardState.SourceType.TEXT) {
            updateFontLabel();
            chooseForegroundTab(mTextRadio, mTextForm);
            mText.setFocus();
        }
    }

    private void updateTrimOptions() {
        // Trimming and padding is not available for clipart images; padding etc is
        // predefined to work well with action bar icons
        if (mValues.sourceType == SourceType.CLIPART
                && mValues.type == AssetType.ACTIONBAR) {
            mTrimCheckBox.setEnabled(false);
            mPaddingSlider.setEnabled(false);
            mValues.trim = false;
        } else if (!mTrimCheckBox.isEnabled()) {
            mTrimCheckBox.setEnabled(true);
            mPaddingSlider.setEnabled(true);
        }
    }

    private boolean validatePage() {
        String error = null;
        //String warning = null;

        if (mImageRadio.getSelection()) {
            String path = mValues.imagePath != null ? mValues.imagePath.getPath() : null;
            if (path == null || path.length() == 0) {
                error = "Select an image";
            } else if (path.equals(DEFAULT_LAUNCHER_ICON)) {
                // Silent
            } else if (!(new File(path).exists())) {
                error = String.format("%1$s does not exist", path);
            } else {
                // Preserve across wizard sessions
                sImagePath = path;
            }
        } else if (mTextRadio.getSelection()) {
            if (mValues.text.length() == 0) {
                error = "Enter text";
            }
        } else {
            assert mClipartRadio.getSelection();
            if (mValues.clipartName == null) {
                error = "Select clip art";
            }
        }

        setPageComplete(error == null);
        if (error != null) {
            setMessage(error, IMessageProvider.ERROR);
        //} else if (warning != null) {
        //    setMessage(warning, IMessageProvider.WARNING);
        } else {
            setErrorMessage(null);
            setMessage(null);
        }

        return error == null;
    }

    @Override
    public boolean isPageComplete() {
        // Force user to reach second page before hitting Finish
        return mShown;
    }

    // ---- Implements ModifyListener ----

    @Override
    public void modifyText(ModifyEvent e) {
        if (mIgnore) {
            return;
        }

        if (e.getSource() == mImagePathText) {
            mValues.imagePath = new File(mImagePathText.getText().trim());
            requestUpdatePreview(false);
        } else if (e.getSource() == mText) {
            mValues.text = mText.getText().trim();
            requestUpdatePreview(false);
        }

        validatePage();
    }

    // ---- Implements SelectionListener ----

    @Override
    public void widgetDefaultSelected(SelectionEvent e) {
        // Nothing to do
    }

    @Override
    public void widgetSelected(SelectionEvent e) {
        if (mIgnore) {
            return;
        }

        Object source = e.getSource();
        boolean updateQuickly = true;

        // Tabs
        if (source == mImageRadio) {
            mValues.sourceType = CreateAssetSetWizardState.SourceType.IMAGE;
            chooseForegroundTab((Button) source, mImageForm);
            configureAssetType(mValues.type);
            updateTrimOptions();
        } else if (source == mClipartRadio) {
            mValues.sourceType = CreateAssetSetWizardState.SourceType.CLIPART;
            chooseForegroundTab((Button) source, mClipartForm);
            configureAssetType(mValues.type);
            updateTrimOptions();
        } else if (source == mTextRadio) {
            mValues.sourceType = CreateAssetSetWizardState.SourceType.TEXT;
            updateFontLabel();
            chooseForegroundTab((Button) source, mTextForm);
            configureAssetType(mValues.type);
            mText.setFocus();
            updateTrimOptions();
        }

        // Choose image file
        if (source == mPickImageButton) {
            FileDialog dialog = new FileDialog(mPickImageButton.getShell(), SWT.OPEN);

            String curLocation = mImagePathText.getText().trim();
            if (!curLocation.isEmpty()) {
                dialog.setFilterPath(curLocation);
            }

            String file = dialog.open();
            if (file != null) {
                mValues.imagePath = new File(file);
                mImagePathText.setText(file);
            }
        }

        // Enforce Radio Groups
        if (source == mCropRadio) {
            mCropRadio.setSelection(true); // Ensure that you can't toggle it off
            mCenterRadio.setSelection(false);
            mValues.crop = true;
        } else if (source == mCenterRadio) {
            mCenterRadio.setSelection(true);
            mCropRadio.setSelection(false);
            mValues.crop = false;
        }
        if (source == mSquareRadio) {
            mValues.shape = GraphicGenerator.Shape.SQUARE;
            setShape(mValues.shape);
        } else if (source == mCircleButton) {
            mValues.shape = GraphicGenerator.Shape.CIRCLE;
            setShape(mValues.shape);
        } else if (source == mNoShapeRadio) {
            mValues.shape = GraphicGenerator.Shape.NONE;
            setShape(mValues.shape);
        }

        if (source == mTrimCheckBox) {
            mValues.trim = mTrimCheckBox.getSelection();
        }

        if (source == mHoloDarkRadio) {
            mHoloDarkRadio.setSelection(true);
            mHoloLightRadio.setSelection(false);
            mValues.holoDark = true;
        } else if (source == mHoloLightRadio) {
            mHoloLightRadio.setSelection(true);
            mHoloDarkRadio.setSelection(false);
            mValues.holoDark = false;
        }

        if (source == mChooseClipart) {
            MessageDialog dialog = new MessageDialog(mChooseClipart.getShell(),
                    "Choose Clip Art",
                    null, "Choose Clip Art Image:", MessageDialog.NONE,
                    new String[] { "Close" }, 0) {
                @Override
                protected Control createCustomArea(Composite parent) {
                    // Outer form which just establishes a width for the inner form which
                    // wraps in a RowLayout
                    Composite outer = new Composite(parent, SWT.NONE);
                    GridLayout gridLayout = new GridLayout();
                    outer.setLayout(gridLayout);

                    Composite chooserForm = new Composite(outer, SWT.NONE);
                    GridData gd = new GridData();
                    gd.grabExcessVerticalSpace = true;
                    gd.widthHint = 450;
                    chooserForm.setLayoutData(gd);
                    RowLayout clipartFormLayout = new RowLayout(SWT.HORIZONTAL);
                    clipartFormLayout.center = true;
                    clipartFormLayout.wrap = true;
                    chooserForm.setLayout(clipartFormLayout);

                    MouseAdapter clickListener = new MouseAdapter() {
                        @Override
                        public void mouseDown(MouseEvent event) {
                            // Clicked on some of the sample art
                            if (event.widget instanceof ImageControl) {
                                ImageControl image = (ImageControl) event.widget;
                                mValues.clipartName = (String) image.getData();
                                close();

                                updateClipartPreview();
                                updatePreview();
                            }
                        }
                    };
                    Display display = chooserForm.getDisplay();
                    Color hoverColor = display.getSystemColor(SWT.COLOR_RED);
                    Iterator<String> clipartImages = GraphicGenerator.getClipartNames();
                    while (clipartImages.hasNext()) {
                        String name = clipartImages.next();
                        try {
                            BufferedImage icon = GraphicGenerator.getClipartIcon(name);
                            if (icon != null) {
                                Image swtImage = SwtUtils.convertToSwt(display, icon, true, -1);
                                ImageControl img = new ImageControl(chooserForm,
                                        SWT.NONE, swtImage);
                                img.setData(name);
                                img.setHoverColor(hoverColor);
                                img.addMouseListener(clickListener);
                            }
                        } catch (IOException e1) {
                            AdtPlugin.log(e1, null);
                        }
                    }
                    outer.pack();
                    outer.layout();
                    return outer;
                }
            };
            dialog.open();
        }

        if (source == mBgButton) {
            ColorDialog dlg = new ColorDialog(mBgButton.getShell());
            dlg.setRGB(mBgColor);
            dlg.setText("Choose a new Background Color");
            RGB rgb = dlg.open();
            if (rgb != null) {
                // Dispose the old color, create the
                // new one, and set into the label
                mValues.background = rgb;
                updateColor(mBgButton.getDisplay(), rgb, true /*background*/);
            }
        } else if (source == mFgButton) {
            ColorDialog dlg = new ColorDialog(mFgButton.getShell());
            dlg.setRGB(mFgColor);
            dlg.setText("Choose a new Foreground Color");
            RGB rgb = dlg.open();
            if (rgb != null) {
                // Dispose the old color, create the
                // new one, and set into the label
                mValues.foreground = rgb;
                updateColor(mFgButton.getDisplay(), rgb, false /*background*/);
            }
        }

        if (source == mFontButton) {
            FontDialog dialog = new FontDialog(mFontButton.getShell());
            FontData[] fontList;
            if (mFontButton.getData() == null) {
                fontList = mFontButton.getDisplay().getFontList(
                        mValues.getTextFont().getFontName(), true /*scalable*/);
            } else {
                fontList = mFontButton.getFont().getFontData();
            }
            dialog.setFontList(fontList);
            FontData data = dialog.open();
            if (data != null) {
                Font font = new Font(mFontButton.getDisplay(), dialog.getFontList());
                mFontButton.setFont(font);
                mFontButton.setData(font);

                // Always use a large font for the rendering, even though user is typically
                // picking small font sizes in the font chooser
                //int dpi = mFontButton.getDisplay().getDPI().y;
                //int height = (int) Math.round(fontData.getHeight() * dpi / 72.0);
                int fontHeight = new TextRenderUtil.Options().fontSize;
                FontData fontData = font.getFontData()[0];
                int awtStyle = java.awt.Font.PLAIN;
                int swtStyle = fontData.getStyle();
                if ((swtStyle & SWT.ITALIC) != 0) {
                    awtStyle |= java.awt.Font.ITALIC;
                }
                if ((swtStyle & SWT.BOLD) != 0) {
                    awtStyle = java.awt.Font.BOLD;
                }
                mValues.setTextFont(new java.awt.Font(fontData.getName(), awtStyle, fontHeight));

                updateFontLabel();
                mFontButton.getParent().pack();
            }
        }

        if (source == mPaddingSlider) {
            mValues.padding = getPadding();
            mPercentLabel.setText(Integer.toString(getPadding()) + '%');

            // When dragging the slider, only do periodic updates
            updateQuickly = false;
        }

        requestUpdatePreview(updateQuickly);
    }

    @SuppressWarnings("unused") // SWT constructors have side effects and are not unused
    private void updateClipartPreview() {
        for (Control c : mClipartPreviewPanel.getChildren()) {
            c.dispose();
        }
        if (mClipartPreviewPanel.getChildren().length == 0) {
            try {
                BufferedImage icon = GraphicGenerator.getClipartIcon(
                        mValues.clipartName);
                if (icon != null) {
                    Display display = mClipartForm.getDisplay();
                    Image swtImage = SwtUtils.convertToSwt(display, icon,
                            true, -1);
                    new ImageControl(mClipartPreviewPanel,
                            SWT.NONE, swtImage);
                }
            } catch (IOException e1) {
                AdtPlugin.log(e1, null);
            }
            mClipartPreviewPanel.pack();
            mClipartPreviewPanel.layout();
        }
    }

    private void setShape(GraphicGenerator.Shape shape) {
        if (shape == GraphicGenerator.Shape.SQUARE) {
            mSquareRadio.setSelection(true);
            mCircleButton.setSelection(false);
            mNoShapeRadio.setSelection(false);
        } else if (shape == GraphicGenerator.Shape.CIRCLE) {
            mCircleButton.setSelection(true);
            mSquareRadio.setSelection(false);
            mNoShapeRadio.setSelection(false);
        } else if (shape == GraphicGenerator.Shape.NONE) {
            mNoShapeRadio.setSelection(true);
            mCircleButton.setSelection(false);
            mSquareRadio.setSelection(false);
        } else {
            assert false : shape;
        }
    }

    private void updateFontLabel() {
        mFontButton.setText(mValues.getTextFont().getFontName());
    }

    private int getPadding() {
        // Shifted - see comment for mPaddingSlider construction for an explanation
        return mPaddingSlider.getSelection() - 10;
    }

    private void chooseForegroundTab(Button newButton, Composite newArea) {
        if (newButton.getSelection()) {
            mImageRadio.setSelection(false);
            mClipartRadio.setSelection(false);
            mTextRadio.setSelection(false);
            newButton.setSelection(true);
            StackLayout stackLayout = (StackLayout) mForegroundArea.getLayout();
            stackLayout.topControl = newArea;
            mForegroundArea.layout();
        } else {
            // Treat it as a radio button: you can't click to turn it off, you have to
            // click on one of the other buttons
            newButton.setSelection(true);
        }
    }

    /**
     * Delay updates of the preview, to ensure that the SWT UI acts immediately (to handle
     * radio group selections etc).
     *
     * @param quickly if true, update the previews soon, otherwise schedule one a bit later
     */
    private void requestUpdatePreview(boolean quickly) {
        if (mTimerPending) {
            return;
        }
        mTimerPending = true;

        final Runnable timer = new Runnable() {
            @Override
            public void run() {
                mTimerPending = false;
                updatePreview();
            }
        };

        mPreviewArea.getDisplay().timerExec(quickly ? 10 : 250, timer);
    }

    private void updatePreview() {
        Display display = mPreviewArea.getDisplay();

        for (Control c : mPreviewArea.getChildren()) {
            c.dispose();
        }

        if (!validatePage()) {
            return;
        }

        Map<String, Map<String, BufferedImage>> map = generateImages(mValues,
                true /*previewOnly*/, this);
        for (Entry<String, Map<String, BufferedImage>> categoryEntry : map.entrySet()) {
            String category = categoryEntry.getKey();
            if (category.length() > 0) {
                Label nameLabel = new Label(mPreviewArea, SWT.NONE);
                nameLabel.setText(String.format("%1$s:", category));
                RowData rowData = new RowData();
                nameLabel.setLayoutData(rowData);
                // Ensure these get their own rows
                rowData.width = PREVIEW_AREA_WIDTH;
            }

            Map<String, BufferedImage> images = categoryEntry.getValue();
            for (Entry<String, BufferedImage> entry :  images.entrySet()) {
                BufferedImage image = entry.getValue();
                Image swtImage = SwtUtils.convertToSwt(display, image, true, -1);
                if (swtImage != null) {
                    @SuppressWarnings("unused") // Has side effect
                    ImageControl imageControl = new ImageControl(mPreviewArea, SWT.NONE, swtImage);
                }
            }
        }

        mPreviewArea.layout(true);
    }

    /**
     * Generate images using the given wizard state
     *
     * @param mValues the state to use
     * @param previewOnly whether we are only generating previews
     * @param page if non null, a wizard page to write error messages to
     * @return a map of image objects
     */
    public static Map<String, Map<String, BufferedImage>> generateImages(
            @NonNull CreateAssetSetWizardState mValues,
            boolean previewOnly,
            @Nullable WizardPage page) {
        // Map of ids to images: Preserve insertion order (the densities)
        Map<String, Map<String, BufferedImage>> categoryMap =
                new LinkedHashMap<String, Map<String, BufferedImage>>();

        AssetType type = mValues.type;
        boolean trim = mValues.trim;

        BufferedImage sourceImage = null;
        switch (mValues.sourceType) {
            case IMAGE: {
                // Load the image
                // TODO: Only do this when the source image type is image
                String path = mValues.imagePath != null ? mValues.imagePath.getPath() : "";
                if (path.length() == 0) {
                    if (page != null) {
                        page.setErrorMessage("Enter a filename");
                    }
                    return Collections.emptyMap();
                }
                if (!path.equals(DEFAULT_LAUNCHER_ICON)) {
                    File file = new File(path);
                    if (!file.isFile()) {
                        if (page != null) {
                            page.setErrorMessage(String.format("%1$s does not exist", file.getPath()));
                        }
                        return Collections.emptyMap();
                    }
                }

                if (page != null) {
                    page.setErrorMessage(null);
                }
                try {
                    sourceImage = mValues.getCachedImage(path, false);
                    if (sourceImage != null) {
                        if (trim) {
                            sourceImage = ImageUtils.cropBlank(sourceImage, null, TYPE_INT_ARGB);
                        }
                        if (mValues.padding != 0) {
                            sourceImage = Util.paddedImage(sourceImage, mValues.padding);
                        }
                    }
                } catch (IOException ioe) {
                    if (page != null) {
                        page.setErrorMessage(ioe.getLocalizedMessage());
                    }
                }
                break;
            }
            case CLIPART: {
                try {
                    sourceImage = GraphicGenerator.getClipartImage(mValues.clipartName);

                    boolean isActionBar = mValues.type == AssetType.ACTIONBAR;
                    if (trim && !isActionBar) {
                        sourceImage = ImageUtils.cropBlank(sourceImage, null, TYPE_INT_ARGB);
                    }

                    if (type.needsColors()) {
                        RGB fg = mValues.foreground;
                        int color = 0xFF000000 | (fg.red << 16) | (fg.green << 8) | fg.blue;
                        Paint paint = new java.awt.Color(color);
                        sourceImage = Util.filledImage(sourceImage, paint);
                    }

                    int padding = mValues.padding;
                    if (padding != 0 && !isActionBar) {
                        sourceImage = Util.paddedImage(sourceImage, padding);
                    }
                } catch (IOException e) {
                    AdtPlugin.log(e, null);
                    return categoryMap;
                }
                break;
            }
            case TEXT: {
                String text = mValues.text;
                TextRenderUtil.Options options = new TextRenderUtil.Options();
                options.font = mValues.getTextFont();
                int color;
                if (type.needsColors()) {
                    RGB fg = mValues.foreground;
                    color = 0xFF000000 | (fg.red << 16) | (fg.green << 8) | fg.blue;
                } else {
                    color = 0xFFFFFFFF;
                }
                options.foregroundColor = color;
                sourceImage = TextRenderUtil.renderTextImage(text, mValues.padding, options);

                if (trim) {
                    sourceImage = ImageUtils.cropBlank(sourceImage, null, TYPE_INT_ARGB);
                }

                int padding = mValues.padding;
                if (padding != 0) {
                    sourceImage = Util.paddedImage(sourceImage, padding);
                }
                break;
            }
        }

        GraphicGenerator generator = null;
        GraphicGenerator.Options options = null;
        switch (type) {
            case LAUNCHER: {
                generator = new LauncherIconGenerator();
                LauncherIconGenerator.LauncherOptions launcherOptions =
                        new LauncherIconGenerator.LauncherOptions();
                launcherOptions.shape = mValues.shape;
                launcherOptions.crop = mValues.crop;
                launcherOptions.style = GraphicGenerator.Style.SIMPLE;

                RGB bg = mValues.background;
                int color = (bg.red << 16) | (bg.green << 8) | bg.blue;
                launcherOptions.backgroundColor = color;
                // Flag which tells the generator iterator to include a web graphic
                launcherOptions.isWebGraphic = !previewOnly;
                options = launcherOptions;

                break;
            }
            case MENU:
                generator = new MenuIconGenerator();
                options = new GraphicGenerator.Options();
                break;
            case ACTIONBAR: {
                generator = new ActionBarIconGenerator();
                ActionBarIconGenerator.ActionBarOptions actionBarOptions =
                        new ActionBarIconGenerator.ActionBarOptions();
                actionBarOptions.theme = mValues.holoDark
                        ? ActionBarIconGenerator.Theme.HOLO_DARK
                                : ActionBarIconGenerator.Theme.HOLO_LIGHT;
                actionBarOptions.sourceIsClipart = (mValues.sourceType == SourceType.CLIPART);

                options = actionBarOptions;
                break;
            }
            case NOTIFICATION: {
                generator = new NotificationIconGenerator();
                options = new NotificationIconGenerator.NotificationOptions();
                break;
            }
            case TAB:
                generator = new TabIconGenerator();
                options = new TabIconGenerator.TabOptions();
                break;
            default:
                AdtPlugin.log(IStatus.ERROR, "Unsupported asset type: %1$s", type);
                return categoryMap;
        }

        options.sourceImage = sourceImage;

        IProject project = mValues.project;
        if (mValues.minSdk != -1) {
            options.minSdk = mValues.minSdk;
        } else {
            Pair<Integer, Integer> v = ManifestInfo.computeSdkVersions(project);
            options.minSdk = v.getFirst();
        }

        String baseName = mValues.outputName;
        generator.generate(null, categoryMap, mValues, options, baseName);

        return categoryMap;
    }

    /**
     * Generate custom icons into the project based on the asset studio wizard
     * state
     *
     * @param newProject the project to write into
     * @param values the wizard state to read configuration settings from
     * @param previewOnly whether we are only generating a preview. For example,
     *            the launcher icons won't generate a huge 512x512 web graphic
     *            in preview mode
     * @param page a wizard page to write error messages to, or null
     */
    public static void generateIcons(final IProject newProject,
            @NonNull CreateAssetSetWizardState values,
            boolean previewOnly,
            @Nullable WizardPage page) {
        // Generate the custom icons
        Map<String, Map<String, BufferedImage>> categories = generateImages(values,
                false /*previewOnly*/, page);
        for (Map<String, BufferedImage> previews : categories.values()) {
            for (Map.Entry<String, BufferedImage> entry : previews.entrySet()) {
                String relativePath = entry.getKey();
                IPath dest = new Path(relativePath);
                IFile file = newProject.getFile(dest);

                // In case template already created icons (should remove that)
                // remove them first
                if (file.exists()) {
                    try {
                        file.delete(true, new NullProgressMonitor());
                    } catch (CoreException e) {
                        AdtPlugin.log(e, null);
                    }
                }
                AdtUtils.createWsParentDirectory(file.getParent());
                BufferedImage image = entry.getValue();

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                try {
                    ImageIO.write(image, "PNG", stream); //$NON-NLS-1$
                    byte[] bytes = stream.toByteArray();
                    InputStream is = new ByteArrayInputStream(bytes);
                    file.create(is, true /*force*/, null /*progress*/);
                } catch (IOException e) {
                    AdtPlugin.log(e, null);
                } catch (CoreException e) {
                    AdtPlugin.log(e, null);
                }

                try {
                    file.getParent().refreshLocal(1, new NullProgressMonitor());
                } catch (CoreException e) {
                    AdtPlugin.log(e, null);
                }
            }
        }
    }

    private void updateColor(Display display, RGB color, boolean isBackground) {
        // Button.setBackgroundColor does not work (at least not on OSX) so
        // we instead have to use Button.setImage with an image of the given
        // color
        BufferedImage coloredImage = ImageUtils.createColoredImage(60, 20, color);
        Image image = SwtUtils.convertToSwt(display, coloredImage, false, -1);

        if (isBackground) {
            mBgColor = color;
            mBgButton.setImage(image);
        } else {
            mFgColor = color;
            mFgButton.setImage(image);
        }
    }
}

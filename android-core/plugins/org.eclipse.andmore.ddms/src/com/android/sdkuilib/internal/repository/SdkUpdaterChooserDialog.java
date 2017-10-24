/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.sdkuilib.internal.repository;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.sdklib.AndroidVersion;
import com.android.sdklib.internal.repository.archives.Archive;
import com.android.sdklib.internal.repository.packages.IAndroidVersionProvider;
import com.android.sdklib.internal.repository.packages.Package;
import com.android.sdklib.internal.repository.sources.SdkSource;
import com.android.sdklib.internal.repository.updater.ArchiveInfo;
import com.android.sdklib.internal.repository.updater.SdkUpdaterLogic;
import com.android.sdklib.repository.FullRevision;
import com.android.sdklib.repository.License;
import com.android.sdkuilib.internal.repository.icons.ImageFactory;
import com.android.sdkuilib.ui.GridDialog;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;


/**
 * Implements an {@link SdkUpdaterChooserDialog}.
 */
final class SdkUpdaterChooserDialog extends GridDialog {

    /** Last dialog size for this session. */
    private static Point sLastSize;
    /** Precomputed flag indicating whether the "accept license" radio is checked. */
    private boolean mAcceptSameAllLicense;
    private boolean mInternalLicenseRadioUpdate;

    // UI fields
    private SashForm mSashForm;
    private Composite mPackageRootComposite;
    private TreeViewer mTreeViewPackage;
    private Tree mTreePackage;
    private TreeColumn mTreeColum;
    private StyledText mPackageText;
    private Button mLicenseRadioAccept;
    private Button mLicenseRadioReject;
    private Button mLicenseRadioAcceptLicense;
    private Group mPackageTextGroup;
    private final SwtUpdaterData mSwtUpdaterData;
    private Group mTableGroup;
    private Label mErrorLabel;

    /**
     * List of all archives to be installed with dependency information.
     * <p/>
     * Note: in a lot of cases, we need to find the archive info for a given archive. This
     * is currently done using a simple linear search, which is fine since we only have a very
     * limited number of archives to deal with (e.g. < 10 now). We might want to revisit
     * this later if it becomes an issue. Right now just do the simple thing.
     * <p/>
     * Typically we could add a map Archive=>ArchiveInfo later.
     */
    private final Collection<ArchiveInfo> mArchives;



    /**
     * Create the dialog.
     *
     * @param parentShell The shell to use, typically updaterData.getWindowShell()
     * @param swtUpdaterData The updater data
     * @param archives The archives to be installed
     */
    public SdkUpdaterChooserDialog(Shell parentShell,
            SwtUpdaterData swtUpdaterData,
            Collection<ArchiveInfo> archives) {
        super(parentShell, 3, false/*makeColumnsEqual*/);
        mSwtUpdaterData = swtUpdaterData;
        mArchives = archives;
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    /**
     * Returns the results, i.e. the list of selected new archives to install.
     * This is similar to the {@link ArchiveInfo} list instance given to the constructor
     * except only accepted archives are present.
     * <p/>
     * An empty list is returned if cancel was chosen.
     */
    public ArrayList<ArchiveInfo> getResult() {
        ArrayList<ArchiveInfo> ais = new ArrayList<ArchiveInfo>();

        if (getReturnCode() == Window.OK) {
            for (ArchiveInfo ai : mArchives) {
                if (ai.isAccepted()) {
                    ais.add(ai);
                }
            }
        }

        return ais;
    }

    /**
     * Create the main content of the dialog.
     * See also {@link #createButtonBar(Composite)} below.
     */
    @Override
    public void createDialogContent(Composite parent) {
        // Sash form
        mSashForm = new SashForm(parent, SWT.NONE);
        mSashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));


        // Left part of Sash Form

        mTableGroup = new Group(mSashForm, SWT.NONE);
        mTableGroup.setText("Packages");
        mTableGroup.setLayout(new GridLayout(1, false/*makeColumnsEqual*/));

        mTreeViewPackage = new TreeViewer(mTableGroup, SWT.BORDER | SWT.V_SCROLL | SWT.SINGLE);
        mTreePackage = mTreeViewPackage.getTree();
        mTreePackage.setHeaderVisible(false);
        mTreePackage.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

        mTreePackage.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onPackageSelected();  //$hide$
            }
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                onPackageDoubleClick();
            }
        });

        mTreeColum = new TreeColumn(mTreePackage, SWT.NONE);
        mTreeColum.setWidth(100);
        mTreeColum.setText("Packages");

        // Right part of Sash form

        mPackageRootComposite = new Composite(mSashForm, SWT.NONE);
        mPackageRootComposite.setLayout(new GridLayout(4, false/*makeColumnsEqual*/));
        mPackageRootComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        mPackageTextGroup = new Group(mPackageRootComposite, SWT.NONE);
        mPackageTextGroup.setText("Package Description && License");
        mPackageTextGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1));
        mPackageTextGroup.setLayout(new GridLayout(1, false/*makeColumnsEqual*/));

        mPackageText = new StyledText(mPackageTextGroup,
                        SWT.MULTI | SWT.READ_ONLY | SWT.WRAP | SWT.V_SCROLL);
        mPackageText.setBackground(
                getParentShell().getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
        mPackageText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

        mLicenseRadioAccept = new Button(mPackageRootComposite, SWT.RADIO);
        mLicenseRadioAccept.setText("Accept");
        mLicenseRadioAccept.setToolTipText("Accept this package.");
        mLicenseRadioAccept.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onLicenseRadioSelected();
            }
        });

        mLicenseRadioReject = new Button(mPackageRootComposite, SWT.RADIO);
        mLicenseRadioReject.setText("Reject");
        mLicenseRadioReject.setToolTipText("Reject this package.");
        mLicenseRadioReject.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onLicenseRadioSelected();
            }
        });

        Link link = new Link(mPackageRootComposite, SWT.NONE);
        link.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false, 1, 1));
        final String printAction = "Print"; // extracted for NLS, to compare with below.
        link.setText(String.format("<a>Copy to clipboard</a> | <a>%1$s</a>", printAction));
        link.setToolTipText("Copies all text and license to clipboard | Print using system defaults.");
        link.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                super.widgetSelected(e);
                if (printAction.equals(e.text)) {
                    mPackageText.print();
                } else {
                    Point p = mPackageText.getSelection();
                    mPackageText.selectAll();
                    mPackageText.copy();
                    mPackageText.setSelection(p);
                }
            }
        });


        mLicenseRadioAcceptLicense = new Button(mPackageRootComposite, SWT.RADIO);
        mLicenseRadioAcceptLicense.setText("Accept License");
        mLicenseRadioAcceptLicense.setToolTipText("Accept all packages that use the same license.");
        mLicenseRadioAcceptLicense.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onLicenseRadioSelected();
            }
        });

        mSashForm.setWeights(new int[] {200, 300});
    }

    /**
     * Creates and returns the contents of this dialog's button bar.
     * <p/>
     * This reimplements most of the code from the base class with a few exceptions:
     * <ul>
     * <li>Enforces 3 columns.
     * <li>Inserts a full-width error label.
     * <li>Inserts a help label on the left of the first button.
     * <li>Renames the OK button into "Install"
     * </ul>
     */
    @Override
    protected Control createButtonBar(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 0; // this is incremented by createButton
        layout.makeColumnsEqualWidth = false;
        layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
        layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
        layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
        layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
        composite.setLayout(layout);
        GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1);
        composite.setLayoutData(data);
        composite.setFont(parent.getFont());

        // Error message area
        mErrorLabel = new Label(composite, SWT.NONE);
        mErrorLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));

        // Label at the left of the install/cancel buttons
        Label label = new Label(composite, SWT.NONE);
        label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        label.setText("[*] Something depends on this package");
        label.setEnabled(false);
        layout.numColumns++;

        // Add the ok/cancel to the button bar.
        createButtonsForButtonBar(composite);

        // the ok button should be an "install" button
        Button button = getButton(IDialogConstants.OK_ID);
        button.setText("Install");

        return composite;
    }

    // -- End of UI, Start of internal logic ----------
    // Hide everything down-below from SWT designer
    //$hide>>$

    @Override
    public void create() {
        super.create();

        // set window title
        getShell().setText("Choose Packages to Install");

        setWindowImage();

        // Automatically accept those with an empty license or no license
        for (ArchiveInfo ai : mArchives) {
            Archive a = ai.getNewArchive();
            if (a != null) {
                License license = a.getParentPackage().getLicense();
                boolean hasLicense = license != null &&
                                     license.getLicense() != null &&
                                     license.getLicense().length() > 0;
                ai.setAccepted(!hasLicense);
            }
        }

        // Fill the list with the replacement packages
        mTreeViewPackage.setLabelProvider(new NewArchivesLabelProvider());
        mTreeViewPackage.setContentProvider(new NewArchivesContentProvider());
        mTreeViewPackage.setInput(createTreeInput(mArchives));
        mTreeViewPackage.expandAll();

        adjustColumnsWidth();

        // select first item
        onPackageSelected();
    }

    /**
     * Creates the icon of the window shell.
     */
    private void setWindowImage() {
        String imageName = "android_icon_16.png"; //$NON-NLS-1$
        if (SdkConstants.currentPlatform() == SdkConstants.PLATFORM_DARWIN) {
            imageName = "android_icon_128.png";   //$NON-NLS-1$
        }

        if (mSwtUpdaterData != null) {
            ImageFactory imgFactory = mSwtUpdaterData.getImageFactory();
            if (imgFactory != null) {
                getShell().setImage(imgFactory.getImageByName(imageName));
            }
        }
    }

    /**
     * Adds a listener to adjust the columns width when the parent is resized.
     * <p/>
     * If we need something more fancy, we might want to use this:
     * http://dev.eclipse.org/viewcvs/index.cgi/org.eclipse.swt.snippets/src/org/eclipse/swt/snippets/Snippet77.java?view=co
     */
    private void adjustColumnsWidth() {
        // Add a listener to resize the column to the full width of the table
        ControlAdapter resizer = new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {
                Rectangle r = mTreePackage.getClientArea();
                mTreeColum.setWidth(r.width);
            }
        };
        mTreePackage.addControlListener(resizer);
        resizer.controlResized(null);
    }

    /**
     * Captures the window size before closing this.
     * @see #getInitialSize()
     */
    @Override
    public boolean close() {
        sLastSize = getShell().getSize();
        return super.close();
    }

    /**
     * Tries to reuse the last window size during this session.
     * <p/>
     * Note: the alternative would be to implement {@link #getDialogBoundsSettings()}
     * since the default {@link #getDialogBoundsStrategy()} is to persist both location
     * and size.
     */
    @Override
    protected Point getInitialSize() {
        if (sLastSize != null) {
            return sLastSize;
        } else {
            // Arbitrary values that look good on my screen and fit on 800x600
            return new Point(740, 470);
        }
    }

    /**
     * Callback invoked when a package item is selected in the list.
     */
    private void onPackageSelected() {
        Object item = getSelectedItem();

        // Update mAcceptSameAllLicense : true if all items under the same license are accepted.
        ArchiveInfo ai = null;
        List<ArchiveInfo> list = null;
        if (item instanceof ArchiveInfo) {
            ai = (ArchiveInfo) item;

            Object p =
                ((NewArchivesContentProvider) mTreeViewPackage.getContentProvider()).getParent(ai);
            if (p instanceof LicenseEntry) {
                list = ((LicenseEntry) p).getArchives();
            }
            displayPackageInformation(ai);

        } else if (item instanceof LicenseEntry) {
            LicenseEntry entry = (LicenseEntry) item;
            list = entry.getArchives();
            displayLicenseInformation(entry);

        } else {
            // Fallback, should not happen.
            displayEmptyInformation();
        }

        // the "Accept License" radio is selected if there's a license with >= 0 items
        // and they are all in "accepted" state.
        mAcceptSameAllLicense = list != null && list.size() > 0;
        if (mAcceptSameAllLicense) {
            assert list != null;
            License lic0 = getLicense(list.get(0));
            for (ArchiveInfo ai2 : list) {
                License lic2 = getLicense(ai2);
                if (ai2.isAccepted() && (lic0 == lic2 || lic0.equals(lic2))) {
                    continue;
                } else {
                    mAcceptSameAllLicense = false;
                    break;
                }
            }
        }

        displayMissingDependency(ai);
        updateLicenceRadios(ai);
    }

    /** Returns the currently selected tree item.
     * @return Either {@link ArchiveInfo} or {@link LicenseEntry} or null. */
    private Object getSelectedItem() {
        ISelection sel = mTreeViewPackage.getSelection();
        if (sel instanceof IStructuredSelection) {
            Object elem = ((IStructuredSelection) sel).getFirstElement();
            if (elem instanceof ArchiveInfo || elem instanceof LicenseEntry) {
                return elem;
            }
        }
        return null;
    }

    /**
     * Information displayed when nothing valid is selected.
     */
    private void displayEmptyInformation() {
        mPackageText.setText("Please select a package or a license.");
    }

    /**
     * Updates the package description and license text depending on the selected package.
     * <p/>
     * Note that right now there is no logic to support more than one level of dependencies
     * (e.g. A <- B <- C and A is disabled so C should be disabled; currently C's state depends
     * solely on B's state). We currently don't need this. It would be straightforward to add
     * if we had a need for it, though. This would require changes to {@link ArchiveInfo} and
     * {@link SdkUpdaterLogic}.
     */
    private void displayPackageInformation(ArchiveInfo ai) {
        Archive aNew = ai   == null ? null : ai.getNewArchive();
        Package pNew = aNew == null ? null : aNew.getParentPackage();

        if (pNew == null) {
            displayEmptyInformation();
            return;
        }
        assert ai   != null;                        // make Eclipse null detector happy
        assert aNew != null;

        mPackageText.setText("");                   //$NON-NLS-1$

        addSectionTitle("Package Description\n");
        addText(pNew.getLongDescription(), "\n\n"); //$NON-NLS-1$

        Archive aOld = ai.getReplaced();
        if (aOld != null) {
            Package pOld = aOld.getParentPackage();

            FullRevision rOld = pOld.getRevision();
            FullRevision rNew = pNew.getRevision();

            boolean showRev = true;

            if (pNew instanceof IAndroidVersionProvider &&
                    pOld instanceof IAndroidVersionProvider) {
                AndroidVersion vOld = ((IAndroidVersionProvider) pOld).getAndroidVersion();
                AndroidVersion vNew = ((IAndroidVersionProvider) pNew).getAndroidVersion();

                if (!vOld.equals(vNew)) {
                    // Versions are different, so indicate more than just the revision.
                    addText(String.format("This update will replace API %1$s revision %2$s with API %3$s revision %4$s.\n\n",
                            vOld.getApiString(), rOld.toShortString(),
                            vNew.getApiString(), rNew.toShortString()));
                    showRev = false;
                }
            }

            if (showRev) {
                addText(String.format("This update will replace revision %1$s with revision %2$s.\n\n",
                        rOld.toShortString(),
                        rNew.toShortString()));
            }
        }

        ArchiveInfo[] aDeps = ai.getDependsOn();
        if ((aDeps != null && aDeps.length > 0) || ai.isDependencyFor()) {
            addSectionTitle("Dependencies\n");

            if (aDeps != null && aDeps.length > 0) {
                addText("Installing this package also requires installing:");
                for (ArchiveInfo aDep : aDeps) {
                    addText(String.format("\n- %1$s",
                            aDep.getShortDescription()));
                }
                addText("\n\n");
            }

            if (ai.isDependencyFor()) {
                addText("This package is a dependency for:");
                for (ArchiveInfo ai2 : ai.getDependenciesFor()) {
                    addText(String.format("\n- %1$s",
                            ai2.getShortDescription()));
                }
                addText("\n\n");
            }
        }

        addSectionTitle("Archive Description\n");
        addText(aNew.getLongDescription(), "\n\n");                             //$NON-NLS-1$

        License license = pNew.getLicense();
        if (license != null) {
            String text = license.getLicense();
            if (text != null) {
                addSectionTitle("License\n");
                addText(text.trim(), "\n\n");                                   //$NON-NLS-1$
            }
        }

        addSectionTitle("Site\n");
        SdkSource source = pNew.getParentSource();
        if (source != null) {
            addText(source.getShortDescription());
        }
    }

    /**
     * Updates the description for a license entry.
     */
    private void displayLicenseInformation(LicenseEntry entry) {
        List<ArchiveInfo> archives = entry == null ? null : entry.getArchives();
        if (archives == null) {
            // There should not be a license entry without any package in it.
            displayEmptyInformation();
            return;
        }
        assert entry != null;

        mPackageText.setText("");                   //$NON-NLS-1$

        License license = null;
        addSectionTitle("Packages\n");
        for (ArchiveInfo ai : entry.getArchives()) {
            Archive aNew = ai.getNewArchive();
            if (aNew != null) {
                Package pNew = aNew.getParentPackage();
                if (pNew != null) {
                    if (license == null) {
                        license = pNew.getLicense();
                    } else {
                        assert license.equals(pNew.getLicense()); // all items have the same license
                    }
                    addText("- ", pNew.getShortDescription(), "\n"); //$NON-NLS-1$ //$NON-NLS-2$
                }
            }
        }

        if (license != null) {
            String text = license.getLicense();
            if (text != null) {
                addSectionTitle("\nLicense\n");
                addText(text.trim(), "\n\n");                                   //$NON-NLS-1$
            }
        }
    }

    /**
     * Computes and displays missing dependencies.
     *
     * If there's a selected package, check the dependency for that one.
     * Otherwise display the first missing dependency of any other package.
     */
    private void displayMissingDependency(ArchiveInfo ai) {
        String error = null;

        try {
            if (ai != null) {
                if (ai.isAccepted()) {
                    // Case where this package is accepted but blocked by another non-accepted one
                    ArchiveInfo[] adeps = ai.getDependsOn();
                    if (adeps != null) {
                        for (ArchiveInfo adep : adeps) {
                            if (!adep.isAccepted()) {
                                error = String.format("This package depends on '%1$s'.",
                                        adep.getShortDescription());
                                return;
                            }
                        }
                    }
                } else {
                    // Case where this package blocks another one when not accepted
                    for (ArchiveInfo adep : ai.getDependenciesFor()) {
                        // It only matters if the blocked one is accepted
                        if (adep.isAccepted()) {
                            error = String.format("Package '%1$s' depends on this one.",
                                    adep.getShortDescription());
                            return;
                        }
                    }
                }
            }

            // If there is no missing dependency on the current selection,
            // just find the first missing dependency of any other package.
            for (ArchiveInfo ai2 : mArchives) {
                if (ai2 == ai) {
                    // We already processed that one above.
                    continue;
                }
                if (ai2.isAccepted()) {
                    // The user requested to install this package.
                    // Check if all its dependencies are met.
                    ArchiveInfo[] adeps = ai2.getDependsOn();
                    if (adeps != null) {
                        for (ArchiveInfo adep : adeps) {
                            if (!adep.isAccepted()) {
                                error = String.format("Package '%1$s' depends on '%2$s'",
                                        ai2.getShortDescription(),
                                        adep.getShortDescription());
                                return;
                            }
                        }
                    }
                } else {
                    // The user did not request to install this package.
                    // Check whether this package blocks another one when not accepted.
                    for (ArchiveInfo adep : ai2.getDependenciesFor()) {
                        // It only matters if the blocked one is accepted
                        // or if it's a local archive that is already installed (these
                        // are marked as implicitly accepted, so it's the same test.)
                        if (adep.isAccepted()) {
                            error = String.format("Package '%1$s' depends on '%2$s'",
                                    adep.getShortDescription(),
                                    ai2.getShortDescription());
                            return;
                        }
                    }
                }
            }
        } finally {
            mErrorLabel.setText(error == null ? "" : error);        //$NON-NLS-1$
        }
    }

    private void addText(String...string) {
        for (String s : string) {
            mPackageText.append(s);
        }
    }

    private void addSectionTitle(String string) {
        String s = mPackageText.getText();
        int start = (s == null ? 0 : s.length());
        mPackageText.append(string);

        StyleRange sr = new StyleRange();
        sr.start = start;
        sr.length = string.length();
        sr.fontStyle = SWT.BOLD;
        sr.underline = true;
        mPackageText.setStyleRange(sr);
    }

    private void updateLicenceRadios(ArchiveInfo ai) {
        if (mInternalLicenseRadioUpdate) {
            return;
        }
        mInternalLicenseRadioUpdate = true;

        boolean oneAccepted = false;

        mLicenseRadioAcceptLicense.setSelection(mAcceptSameAllLicense);
        oneAccepted = ai != null && ai.isAccepted();
        mLicenseRadioAccept.setEnabled(ai != null);
        mLicenseRadioReject.setEnabled(ai != null);
        mLicenseRadioAccept.setSelection(oneAccepted);
        mLicenseRadioReject.setSelection(ai != null && ai.isRejected());

        // The install button is enabled if there's at least one package accepted.
        // If the current one isn't, look for another one.
        boolean missing = mErrorLabel.getText() != null && mErrorLabel.getText().length() > 0;
        if (!missing && !oneAccepted) {
            for(ArchiveInfo ai2 : mArchives) {
                if (ai2.isAccepted()) {
                    oneAccepted = true;
                    break;
                }
            }
        }

        getButton(IDialogConstants.OK_ID).setEnabled(!missing && oneAccepted);

        mInternalLicenseRadioUpdate = false;
    }

    /**
     * Callback invoked when one of the radio license buttons is selected.
     *
     * - accept/refuse: toggle, update item checkbox
     * - accept all: set accept-all, check all items with the *same* license
     */
    private void onLicenseRadioSelected() {
        if (mInternalLicenseRadioUpdate) {
            return;
        }
        mInternalLicenseRadioUpdate = true;

        Object item = getSelectedItem();
        ArchiveInfo ai = (item instanceof ArchiveInfo) ? (ArchiveInfo) item : null;
        boolean needUpdate = true;

        if (!mAcceptSameAllLicense && mLicenseRadioAcceptLicense.getSelection()) {
            // Accept all has been switched on. Mark all packages as accepted

            List<ArchiveInfo> list = null;
            if (item instanceof LicenseEntry) {
                list = ((LicenseEntry) item).getArchives();
            } else if (ai != null) {
                Object p = ((NewArchivesContentProvider) mTreeViewPackage.getContentProvider())
                                                                         .getParent(ai);
                if (p instanceof LicenseEntry) {
                    list = ((LicenseEntry) p).getArchives();
                }
            }

            if (list != null && list.size() > 0) {
                mAcceptSameAllLicense = true;
                for(ArchiveInfo ai2 : list) {
                    ai2.setAccepted(true);
                    ai2.setRejected(false);
                }
            }

        } else if (ai != null && mLicenseRadioAccept.getSelection()) {
            // Accept only this one
            mAcceptSameAllLicense = false;
            ai.setAccepted(true);
            ai.setRejected(false);

        } else if (ai != null && mLicenseRadioReject.getSelection()) {
            // Reject only this one
            mAcceptSameAllLicense = false;
            ai.setAccepted(false);
            ai.setRejected(true);

        } else {
            needUpdate = false;
        }

        mInternalLicenseRadioUpdate = false;

        if (needUpdate) {
            if (mAcceptSameAllLicense) {
                mTreeViewPackage.refresh();
            } else {
               mTreeViewPackage.refresh(ai);
               mTreeViewPackage.refresh(
                       ((NewArchivesContentProvider) mTreeViewPackage.getContentProvider()).
                       getParent(ai));
            }
            displayMissingDependency(ai);
            updateLicenceRadios(ai);
        }
    }

    /**
     * Callback invoked when a package item is double-clicked in the list.
     */
    private void onPackageDoubleClick() {
        Object item = getSelectedItem();

        if (item instanceof ArchiveInfo) {
            ArchiveInfo ai = (ArchiveInfo) item;
            boolean wasAccepted = ai.isAccepted();
            ai.setAccepted(!wasAccepted);
            ai.setRejected(wasAccepted);

            // update state
            mAcceptSameAllLicense = false;
            mTreeViewPackage.refresh(ai);
            // refresh parent since its icon might have changed.
            mTreeViewPackage.refresh(
                    ((NewArchivesContentProvider) mTreeViewPackage.getContentProvider()).
                    getParent(ai));

            displayMissingDependency(ai);
            updateLicenceRadios(ai);

        } else if (item instanceof LicenseEntry) {
            mTreeViewPackage.setExpandedState(item, !mTreeViewPackage.getExpandedState(item));
        }
    }

    /**
     * Provides the labels for the tree view.
     * Root branches are {@link LicenseEntry} elements.
     * Leave nodes are {@link ArchiveInfo} which all have the same license.
     */
    private class NewArchivesLabelProvider extends LabelProvider {
        @Override
        public Image getImage(Object element) {
            if (element instanceof ArchiveInfo) {
                // Archive icon: accepted (green), rejected (red), not set yet (question mark)
                ArchiveInfo ai = (ArchiveInfo) element;

                ImageFactory imgFactory = mSwtUpdaterData.getImageFactory();
                if (imgFactory != null) {
                    if (ai.isAccepted()) {
                        return imgFactory.getImageByName("accept_icon16.png");
                    } else if (ai.isRejected()) {
                        return imgFactory.getImageByName("reject_icon16.png");
                    }
                    return imgFactory.getImageByName("unknown_icon16.png");
                }
                return super.getImage(element);

            } else if (element instanceof LicenseEntry) {
                // License icon: green if all below are accepted, red if all rejected, otherwise
                // no icon.
                ImageFactory imgFactory = mSwtUpdaterData.getImageFactory();
                if (imgFactory != null) {
                    boolean allAccepted = true;
                    boolean allRejected = true;
                    for (ArchiveInfo ai : ((LicenseEntry) element).getArchives()) {
                        allAccepted = allAccepted && ai.isAccepted();
                        allRejected = allRejected && ai.isRejected();
                    }
                    if (allAccepted && !allRejected) {
                        return imgFactory.getImageByName("accept_icon16.png");
                    } else if (!allAccepted && allRejected) {
                        return imgFactory.getImageByName("reject_icon16.png");
                    }
                }
            }

            return null;
        }

        @Override
        public String getText(Object element) {
            if (element instanceof LicenseEntry) {
                return ((LicenseEntry) element).getLicenseRef();

            } else if (element instanceof ArchiveInfo) {
                ArchiveInfo ai = (ArchiveInfo) element;

                String desc = ai.getShortDescription();

                if (ai.isDependencyFor()) {
                    desc += " [*]";
                }

                return desc;

            }

            assert element instanceof String || element instanceof ArchiveInfo;
            return null;
        }
    }

    /**
     * Provides the content for the tree view.
     * Root branches are {@link LicenseEntry} elements.
     * Leave nodes are {@link ArchiveInfo} which all have the same license.
     */
    private class NewArchivesContentProvider implements ITreeContentProvider {
        private List<LicenseEntry> mInput;

        @Override
        public void dispose() {
            // pass
        }

        @SuppressWarnings("unchecked")
        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            // Input should be the result from createTreeInput.
            if (newInput instanceof List<?> &&
                    ((List<?>) newInput).size() > 0 &&
                    ((List<?>) newInput).get(0) instanceof LicenseEntry) {
                mInput = (List<LicenseEntry>) newInput;
            } else {
                mInput = null;
            }
        }

        @Override
        public boolean hasChildren(Object parent) {
            if (parent instanceof List<?>) {
                // This is the root of the tree.
                return true;

            } else if (parent instanceof LicenseEntry) {
                return ((LicenseEntry) parent).getArchives().size() > 0;
            }

            return false;
        }

        @Override
        public Object[] getElements(Object parent) {
            return getChildren(parent);
        }

        @Override
        public Object[] getChildren(Object parent) {
            if (parent instanceof List<?>) {
                return ((List<?>) parent).toArray();

            } else if (parent instanceof LicenseEntry) {
                return ((LicenseEntry) parent).getArchives().toArray();
            }

            return new Object[0];
        }

        @Override
        public Object getParent(Object child) {
            if (child instanceof LicenseEntry) {
                return ((LicenseEntry) child).getRoot();

            } else if (child instanceof ArchiveInfo && mInput != null) {
                for (LicenseEntry entry : mInput) {
                    if (entry.getArchives().contains(child)) {
                        return entry;
                    }
                }
            }

            return null;
        }
    }

    /**
     * Represents a branch in the view tree: an entry where all the sub-archive info
     * share the same license. Contains a link to the share root list for convenience.
     */
    private static class LicenseEntry {
        private final List<LicenseEntry> mRoot;
        private final String mLicenseRef;
        private final List<ArchiveInfo> mArchives;

        public LicenseEntry(
                @NonNull List<LicenseEntry> root,
                @NonNull String licenseRef,
                @NonNull List<ArchiveInfo> archives) {
            mRoot = root;
            mLicenseRef = licenseRef;
            mArchives = archives;
        }

        @NonNull
        public List<LicenseEntry> getRoot() {
            return mRoot;
        }

        @NonNull
        public String getLicenseRef() {
            return mLicenseRef;
        }

        @NonNull
        public List<ArchiveInfo> getArchives() {
            return mArchives;
        }
    }

    /**
     * Creates the tree structure based on the given archives.
     * The current structure is to have a branch per license type,
     * with all the archives sharing the same license under it.
     * Elements with no license are left at the root.
     *
     * @param archives The non-null collection of archive info to display. Ideally non-empty.
     * @return A list of {@link LicenseEntry}, each containing a list of {@link ArchiveInfo}.
     */
    @NonNull
    private List<LicenseEntry> createTreeInput(@NonNull Collection<ArchiveInfo> archives) {
        // Build an ordered map with all the licenses, ordered by license ref name.
        final String noLicense = "No license";      //NLS

        Comparator<String> comp = new Comparator<String>() {
            @Override
            public int compare(String s1, String s2) {
                boolean first1 = noLicense.equals(s1);
                boolean first2 = noLicense.equals(s2);
                if (first1 && first2) {
                    return 0;
                } else if (first1) {
                    return -1;
                } else if (first2) {
                    return 1;
                }
                return s1.compareTo(s2);
            }
        };

        Map<String, List<ArchiveInfo>> map = new TreeMap<String, List<ArchiveInfo>>(comp);

        for (ArchiveInfo info : archives) {
            String ref = noLicense;
            License license = getLicense(info);
            if (license != null && license.getLicenseRef() != null) {
                ref = prettyLicenseRef(license.getLicenseRef());
            }

            List<ArchiveInfo> list = map.get(ref);
            if (list == null) {
                map.put(ref, list = new ArrayList<ArchiveInfo>());
            }
            list.add(info);
        }

        // Transform result into a list
        List<LicenseEntry> licensesList = new ArrayList<LicenseEntry>();
        for (Map.Entry<String, List<ArchiveInfo>> entry : map.entrySet()) {
            licensesList.add(new LicenseEntry(licensesList, entry.getKey(), entry.getValue()));
        }

        return licensesList;
    }

    /**
     * Helper method to retrieve the {@link License} for a given {@link ArchiveInfo}.
     *
     * @param ai The archive info. Can be null.
     * @return The license for the package owning the archive. Can be null.
     */
    @Nullable
    private License getLicense(@Nullable ArchiveInfo ai) {
        if (ai != null) {
            Archive aNew = ai.getNewArchive();
            if (aNew != null) {
                Package pNew = aNew.getParentPackage();
                if (pNew != null) {
                    return pNew.getLicense();
                }
            }
        }
        return null;
    }

    /**
     * Reformats the licenseRef to be more human-readable.
     * It's an XML ref and in practice it looks like [oem-]android-[type]-license.
     * If it's not a format we can deal with, leave it alone.
     */
    private String prettyLicenseRef(String ref) {
        // capitalize every word
        StringBuilder sb = new StringBuilder();
        boolean capitalize = true;
        for (char c : ref.toCharArray()) {
            if (c >= 'a' && c <= 'z') {
                if (capitalize) {
                    c = (char) (c + 'A' - 'a');
                    capitalize = false;
                }
            } else {
                if (c == '-') {
                    c = ' ';
                }
                capitalize = true;
            }
            sb.append(c);
        }

        ref = sb.toString();

        // A few acronyms should stay upper-case
        for (String w : new String[] { "Sdk", "Mips", "Arm" }) {
            ref = ref.replaceAll(w, w.toUpperCase(Locale.US));
        }

        return ref;
    }

    // End of hiding from SWT Designer
    //$hide<<$
}

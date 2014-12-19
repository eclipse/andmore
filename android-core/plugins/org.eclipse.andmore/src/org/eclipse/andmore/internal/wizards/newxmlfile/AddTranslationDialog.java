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
package org.eclipse.andmore.internal.wizards.newxmlfile;

import static com.android.SdkConstants.FD_RES;
import static com.android.SdkConstants.FD_RES_VALUES;
import static com.android.SdkConstants.RES_QUALIFIER_SEP;

import com.android.ide.common.rendering.api.ResourceValue;
import com.android.ide.common.res2.ValueXmlHelper;
import com.android.ide.common.resources.LocaleManager;
import com.android.ide.common.resources.ResourceItem;
import com.android.ide.common.resources.configuration.FolderConfiguration;
import com.android.resources.ResourceType;
import com.google.common.base.Charsets;
import com.google.common.collect.Maps;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.AdtUtils;
import org.eclipse.andmore.internal.editors.layout.configuration.FlagManager;
import org.eclipse.andmore.internal.editors.layout.gle2.ImageControl;
import org.eclipse.andmore.internal.editors.layout.gle2.RenderPreviewManager;
import org.eclipse.andmore.internal.resources.manager.ProjectResources;
import org.eclipse.andmore.internal.resources.manager.ResourceManager;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

/**
 * Dialog which adds a new translation to the project
 */
public class AddTranslationDialog extends Dialog implements ControlListener, SelectionListener,
        TraverseListener {
    private static final int KEY_COLUMN = 0;
    private static final int DEFAULT_TRANSLATION_COLUMN = 1;
    private static final int NEW_TRANSLATION_COLUMN = 2;
    private final FolderConfiguration mConfiguration = new FolderConfiguration();
    private final IProject mProject;
    private String mTarget;
    private boolean mIgnore;
    private Map<String, String> mTranslations;
    private Set<String> mExistingLanguages;
    private String mSelectedLanguage;
    private String mSelectedRegion;

    private Table mTable;
    private Combo mLanguageCombo;
    private Combo mRegionCombo;
    private ImageControl mFlag;
    private Label mFile;
    private Button mOkButton;
    private Composite mErrorPanel;
    private Label mErrorLabel;
    private MyTableViewer mTableViewer;

    /**
     * Creates the dialog.
     * @param parentShell the parent shell
     * @param project the project to add translations into
     */
    public AddTranslationDialog(Shell parentShell, IProject project) {
        super(parentShell);
        setShellStyle(SWT.CLOSE | SWT.RESIZE | SWT.TITLE);
        mProject = project;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite container = (Composite) super.createDialogArea(parent);
        GridLayout gl_container = new GridLayout(6, false);
        gl_container.horizontalSpacing = 0;
        container.setLayout(gl_container);

        Label languageLabel = new Label(container, SWT.NONE);
        languageLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        languageLabel.setText("Language:");
        mLanguageCombo = new Combo(container, SWT.READ_ONLY);
        GridData gd_mLanguageCombo = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
        gd_mLanguageCombo.widthHint = 150;
        mLanguageCombo.setLayoutData(gd_mLanguageCombo);

        Label regionLabel = new Label(container, SWT.NONE);
        GridData gd_regionLabel = new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1);
        gd_regionLabel.horizontalIndent = 10;
        regionLabel.setLayoutData(gd_regionLabel);
        regionLabel.setText("Region:");
        mRegionCombo = new Combo(container, SWT.READ_ONLY);
        GridData gd_mRegionCombo = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_mRegionCombo.widthHint = 150;
        mRegionCombo.setLayoutData(gd_mRegionCombo);
        mRegionCombo.setEnabled(false);

        mFlag = new ImageControl(container, SWT.NONE, null);
        mFlag.setDisposeImage(false);
        GridData gd_mFlag = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_mFlag.exclude = true;
        gd_mFlag.widthHint = 32;
        gd_mFlag.horizontalIndent = 3;
        mFlag.setLayoutData(gd_mFlag);

        mFile = new Label(container, SWT.NONE);
        mFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        mTableViewer = new MyTableViewer(container, SWT.BORDER | SWT.FULL_SELECTION);
        mTable = mTableViewer.getTable();
        mTable.setEnabled(false);
        mTable.setLinesVisible(true);
        mTable.setHeaderVisible(true);
        mTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 6, 2));
        mTable.addControlListener(this);
        mTable.addTraverseListener(this);
        // If you have difficulty opening up this form in WindowBuilder and it complains about
        // the next line, change the type of the mTableViewer field and the above
        // constructor call from MyTableViewer to TableViewer
        TableViewerColumn keyViewerColumn = new TableViewerColumn(mTableViewer, SWT.NONE);
        TableColumn keyColumn = keyViewerColumn.getColumn();
        keyColumn.setWidth(100);
        keyColumn.setText("Key");
        TableViewerColumn defaultViewerColumn = new TableViewerColumn(mTableViewer, SWT.NONE);
        TableColumn defaultColumn = defaultViewerColumn.getColumn();
        defaultColumn.setWidth(200);
        defaultColumn.setText("Default");
        TableViewerColumn translationViewerColumn = new TableViewerColumn(mTableViewer, SWT.NONE);
        TableColumn translationColumn = translationViewerColumn.getColumn();
        translationColumn.setWidth(200);
        translationColumn.setText("New Translation");

        mErrorPanel = new Composite(container, SWT.NONE);
        GridData gd_mErrorLabel = new GridData(SWT.FILL, SWT.CENTER, false, false, 6, 1);
        gd_mErrorLabel.exclude = true;
        mErrorPanel.setLayoutData(gd_mErrorLabel);

        translationViewerColumn.setEditingSupport(new TranslationEditingSupport(mTableViewer));

        fillLanguages();
        fillRegions();
        fillStrings();
        updateColumnWidths();
        validatePage();

        mLanguageCombo.addSelectionListener(this);
        mRegionCombo.addSelectionListener(this);

        return container;
    }

    /** Populates the table with keys and default strings */
    private void fillStrings() {
        ResourceManager manager = ResourceManager.getInstance();
        ProjectResources resources = manager.getProjectResources(mProject);
        mExistingLanguages = resources.getLanguages();

        Collection<ResourceItem> items = resources.getResourceItemsOfType(ResourceType.STRING);

        ResourceItem[] array = items.toArray(new ResourceItem[items.size()]);
        Arrays.sort(array);

        // TODO: Read in the actual XML files providing the default keys here
        // (they can be obtained via ResourceItem.getSourceFileList())
        // such that we can read all the attributes associated with each
        // item, and if it defines translatable=false, or the filename is
        // donottranslate.xml, we can ignore it, and in other cases just
        // duplicate all the attributes (such as "formatted=true", or other
        // local conventions such as "product=tablet", or "msgid="123123123",
        // etc.)

        mTranslations = Maps.newHashMapWithExpectedSize(items.size());
        IBaseLabelProvider labelProvider = new CellLabelProvider() {
            @Override
            public void update(ViewerCell cell) {
                Object element = cell.getElement();
                int index = cell.getColumnIndex();
                ResourceItem item = (ResourceItem) element;
                switch (index) {
                    case KEY_COLUMN: {
                        // Key
                        cell.setText(item.getName());
                        return;
                    }
                    case DEFAULT_TRANSLATION_COLUMN: {
                        // Default translation
                        ResourceValue value = item.getResourceValue(ResourceType.STRING,
                                mConfiguration, false);

                        if (value != null) {
                            cell.setText(value.getValue());
                            return;
                        }
                        break;
                    }
                    case NEW_TRANSLATION_COLUMN: {
                        // New translation
                        String translation = mTranslations.get(item.getName());
                        if (translation != null) {
                            cell.setText(translation);
                            return;
                        }
                        break;
                    }
                    default:
                        assert false : index;
                }
                cell.setText("");
            }
        };

        mTableViewer.setLabelProvider(labelProvider);
        mTableViewer.setContentProvider(new ArrayContentProvider());
        mTableViewer.setInput(array);
    }

    /** Populate the languages dropdown */
    private void fillLanguages() {
        Set<String> languageCodes = LocaleManager.getLanguageCodes();
        List<String> labels = new ArrayList<String>();
        for (String code : languageCodes) {
            labels.add(code + ": " + LocaleManager.getLanguageName(code)); //$NON-NLS-1$
        }
        Collections.sort(labels);
        labels.add(0, "(Select)");
        mLanguageCombo.setItems(labels.toArray(new String[labels.size()]));
        mLanguageCombo.select(0);
    }

    /** Populate the regions dropdown */
    private void fillRegions() {
        // TODO: When you switch languages, offer some "default" usable options. For example,
        // when you choose English, offer the countries that use English, and so on. Unfortunately
        // we don't have good data about this, we'd just need to hardcode a few common cases.
        Set<String> regionCodes = LocaleManager.getRegionCodes();
        List<String> labels = new ArrayList<String>();
        for (String code : regionCodes) {
            labels.add(code + ": " + LocaleManager.getRegionName(code)); //$NON-NLS-1$
        }
        Collections.sort(labels);
        labels.add(0, "Any");
        mRegionCombo.setItems(labels.toArray(new String[labels.size()]));
        mRegionCombo.select(0);
    }

    /** React to resizing by distributing the space evenly between the last two columns */
    private void updateColumnWidths() {
        Rectangle r = mTable.getClientArea();
        int availableWidth = r.width;
        // Distribute all available space to the last two columns
        int columnCount = mTable.getColumnCount();
        for (int i = 0; i < columnCount; i++) {
            TableColumn column = mTable.getColumn(i);
            availableWidth -= column.getWidth();
        }
        if (availableWidth != 0) {
            TableColumn column = mTable.getColumn(DEFAULT_TRANSLATION_COLUMN);
            column.setWidth(column.getWidth() + availableWidth / 2);
            column = mTable.getColumn(NEW_TRANSLATION_COLUMN);
            column.setWidth(column.getWidth() + availableWidth / 2 + availableWidth % 2);
        }
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        mOkButton = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
                // Don't make the OK button default as in most dialogs, since when you press
                // Return thinking you might edit a value it dismisses the dialog instead
                false);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
        mOkButton.setEnabled(false);

        validatePage();
    }

    /**
     * Return the initial size of the dialog.
     */
    @Override
    protected Point getInitialSize() {
        return new Point(800, 600);
    }

    private void updateTarget() {
        if (mSelectedLanguage == null) {
            mTarget = null;
            mFile.setText("");
        } else {
            String folder = FD_RES + '/' + FD_RES_VALUES + RES_QUALIFIER_SEP + mSelectedLanguage;
            if (mSelectedRegion != null) {
                folder = folder + RES_QUALIFIER_SEP + 'r' + mSelectedRegion;
            }
            mTarget = folder + "/strings.xml"; //$NON-NLS-1$
            mFile.setText(String.format("Creating %1$s", mTarget));
        }
    }

    private void updateFlag() {
        if (mSelectedLanguage == null) {
            // Nothing selected
            ((GridData) mFlag.getLayoutData()).exclude = true;
        } else {
            FlagManager manager = FlagManager.get();
            Image flag = manager.getFlag(mSelectedLanguage, mSelectedRegion);
            if (flag != null) {
                ((GridData) mFlag.getLayoutData()).exclude = false;
                mFlag.setImage(flag);
            }
        }

        mFlag.getParent().layout(true);
        mFlag.getParent().redraw();
    }

    /** Actually create the new translation file and write it to disk */
    private void createTranslation() {
        List<String> keys = new ArrayList<String>(mTranslations.keySet());
        Collections.sort(keys);

        StringBuilder sb = new StringBuilder(keys.size() * 120);
        sb.append("<resources>\n\n");          //$NON-NLS-1$
        for (String key : keys) {
            String value = mTranslations.get(key);
            if (value == null || value.trim().isEmpty()) {
                continue;
            }
            sb.append("    <string name=\"");  //$NON-NLS-1$
            sb.append(key);
            sb.append("\">");                  //$NON-NLS-1$
            sb.append(ValueXmlHelper.escapeResourceString(value));
            sb.append("</string>\n");          //$NON-NLS-1$
        }
        sb.append("\n</resources>");           //$NON-NLS-1$

        IFile file = mProject.getFile(mTarget);

        try {
            IContainer parent = file.getParent();
            AdtUtils.ensureExists(parent);
            InputStream source = new ByteArrayInputStream(sb.toString().getBytes(Charsets.UTF_8));
            file.create(source, true, new NullProgressMonitor());
            AdtPlugin.openFile(file, null, true /*showEditorTab*/);

            // Ensure that the project resources updates itself to notice the new language.
            // In theory, this shouldn't be necessary.
            ResourceManager manager = ResourceManager.getInstance();
            IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
            IFolder folder = root.getFolder(parent.getFullPath());
            manager.getResourceFolder(folder);
            RenderPreviewManager.bumpRevision();
        } catch (CoreException e) {
            AdtPlugin.log(e, null);
        }
    }

    private void validatePage() {
        if (mOkButton == null) { // Early initialization
            return;
        }

        String message = null;

        if (mSelectedLanguage == null) {
            message = "Select a language";
        } else if (mExistingLanguages.contains(mSelectedLanguage)) {
            if (mSelectedRegion == null) {
                message = String.format("%1$s is already translated in this project",
                        LocaleManager.getLanguageName(mSelectedLanguage));
            } else {
                ResourceManager manager = ResourceManager.getInstance();
                ProjectResources resources = manager.getProjectResources(mProject);
                SortedSet<String> regions = resources.getRegions(mSelectedLanguage);
                if (regions.contains(mSelectedRegion)) {
                    message = String.format("%1$s (%2$s) is already translated in this project",
                            LocaleManager.getLanguageName(mSelectedLanguage),
                            LocaleManager.getRegionName(mSelectedRegion));
                }
            }
        } else {
            // Require all strings to be translated? No, some of these may not
            // be translatable (e.g. translatable=false, defined in donottranslate.xml, etc.)
            //int missing = mTable.getItemCount() - mTranslations.values().size();
            //if (missing > 0) {
            //    message = String.format("Missing %1$d translations", missing);
            //}
        }

        boolean valid = message == null;
        mTable.setEnabled(message == null);
        mOkButton.setEnabled(valid);
        showError(message);
    }

    private void showError(String error) {
        GridData data = (GridData) mErrorPanel.getLayoutData();

        boolean show = error != null;
        if (show == data.exclude) {
            if (show) {
                if (mErrorLabel == null) {
                    mErrorPanel.setLayout(new GridLayout(2, false));
                    IWorkbench workbench = PlatformUI.getWorkbench();
                    ISharedImages sharedImages = workbench.getSharedImages();
                    String iconName = ISharedImages.IMG_OBJS_ERROR_TSK;
                    Image image = sharedImages.getImage(iconName);
                    @SuppressWarnings("unused")
                    ImageControl icon = new ImageControl(mErrorPanel, SWT.NONE, image);

                    mErrorLabel = new Label(mErrorPanel, SWT.NONE);
                    mErrorLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false,
                            1, 1));
                }
                mErrorLabel.setText(error);
            }
            data.exclude = !show;
            mErrorPanel.getParent().layout(true);
        }
    }

    @Override
    protected void okPressed() {
        mTableViewer.applyEditorValue();

        super.okPressed();
        createTranslation();
    }

    // ---- Implements ControlListener ----

    @Override
    public void controlMoved(ControlEvent e) {
    }

    @Override
    public void controlResized(ControlEvent e) {
        if (mIgnore) {
            return;
        }

        updateColumnWidths();
    }

    // ---- Implements SelectionListener ----

    @Override
    public void widgetSelected(SelectionEvent e) {
        if (mIgnore) {
            return;
        }

        Object source = e.getSource();
        if (source == mLanguageCombo) {
            try {
                mIgnore = true;
                mRegionCombo.select(0);
                mSelectedRegion = null;
            } finally {
                mIgnore = false;
            }

            int languageIndex = mLanguageCombo.getSelectionIndex();
            if (languageIndex == 0) {
                mSelectedLanguage = null;
                mRegionCombo.setEnabled(false);
            } else {
                // This depends on the label format
                mSelectedLanguage = mLanguageCombo.getItem(languageIndex).substring(0, 2);
                mRegionCombo.setEnabled(true);
            }

            updateTarget();
            updateFlag();
        } else if (source == mRegionCombo) {
            int regionIndex = mRegionCombo.getSelectionIndex();
            if (regionIndex == 0) {
                mSelectedRegion = null;
            } else {
                mSelectedRegion = mRegionCombo.getItem(regionIndex).substring(0, 2);
            }

            updateTarget();
            updateFlag();
        }

        try {
            mIgnore = true;
            validatePage();
        } finally {
            mIgnore = false;
        }
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent e) {
    }

    // ---- TraverseListener ----

    @Override
    public void keyTraversed(TraverseEvent e) {
        // If you press Return and we're not cell editing, start editing the current row
        if (e.detail == SWT.TRAVERSE_RETURN && !mTableViewer.isCellEditorActive()) {
            int index = mTable.getSelectionIndex();
            if (index != -1) {
                Object next = mTable.getItem(index).getData();
                mTableViewer.editElement(next, NEW_TRANSLATION_COLUMN);
            }
        }
    }

    /** Editing support for the translation column */
    private class TranslationEditingSupport extends EditingSupport {
        /**
         * When true, setValue is being called as part of a default action
         * (e.g. Return), not due to focus loss
         */
        private boolean mDefaultAction;

        private TranslationEditingSupport(ColumnViewer viewer) {
            super(viewer);
        }

        @Override
        protected void setValue(Object element, Object value) {
            ResourceItem item = (ResourceItem) element;
            mTranslations.put(item.getName(), value.toString());
            mTableViewer.update(element, null);
            validatePage();

            // If the user is pressing Return to finish editing a value (which is
            // not the only way this method can get called - for example, if you click
            // outside the cell while editing, the focus loss will also result in
            // this method getting called), then mDefaultAction is true, and we automatically
            // start editing the next row.
            if (mDefaultAction) {
                mTable.getDisplay().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        if (!mTable.isDisposed() && !mTableViewer.isCellEditorActive()) {
                            int index = mTable.getSelectionIndex();
                            if (index != -1 && index < mTable.getItemCount() - 1) {
                                Object next = mTable.getItem(index + 1).getData();
                                mTableViewer.editElement(next, NEW_TRANSLATION_COLUMN);
                            }
                        }
                    }
                });
            }
        }

        @Override
        protected Object getValue(Object element) {
            ResourceItem item = (ResourceItem) element;
            String value = mTranslations.get(item.getName());
            if (value == null) {
                return "";
            }
            return value;
        }

        @Override
        protected CellEditor getCellEditor(Object element) {
            return new TextCellEditor(mTable) {
                @Override
                protected void handleDefaultSelection(SelectionEvent event) {
                    try {
                        mDefaultAction = true;
                        super.handleDefaultSelection(event);
                    } finally {
                        mDefaultAction = false;
                    }
                }
            };
        }

        @Override
        protected boolean canEdit(Object element) {
            return true;
        }
    }

    private class MyTableViewer extends TableViewer {
        public MyTableViewer(Composite parent, int style) {
            super(parent, style);
        }

        // Make this public so we can call it to ensure values are applied before the dialog
        // is dismissed in {@link #okPressed}
        @Override
        public void applyEditorValue() {
            super.applyEditorValue();
        }
    }
}

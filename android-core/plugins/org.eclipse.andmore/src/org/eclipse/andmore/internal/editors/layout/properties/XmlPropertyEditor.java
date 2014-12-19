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

package org.eclipse.andmore.internal.editors.layout.properties;

import static com.android.SdkConstants.ANDROID_PREFIX;
import static com.android.SdkConstants.ANDROID_THEME_PREFIX;
import static com.android.SdkConstants.ATTR_ID;
import static com.android.SdkConstants.DOT_PNG;
import static com.android.SdkConstants.DOT_XML;
import static com.android.SdkConstants.NEW_ID_PREFIX;
import static com.android.SdkConstants.PREFIX_RESOURCE_REF;
import static com.android.SdkConstants.PREFIX_THEME_REF;
import static org.eclipse.andmore.common.layout.BaseViewRule.stripIdPrefix;

import com.android.annotations.NonNull;
import com.android.ide.common.api.IAttributeInfo;
import com.android.ide.common.api.IAttributeInfo.Format;
import com.android.ide.common.rendering.api.ResourceValue;
import com.android.ide.common.resources.ResourceRepository;
import com.android.ide.common.resources.ResourceResolver;
import com.android.resources.ResourceType;
import com.google.common.collect.Maps;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.common.layout.BaseViewRule;
import org.eclipse.andmore.internal.editors.common.CommonXmlEditor;
import org.eclipse.andmore.internal.editors.layout.LayoutEditorDelegate;
import org.eclipse.andmore.internal.editors.layout.gle2.GraphicalEditorPart;
import org.eclipse.andmore.internal.editors.layout.gle2.ImageUtils;
import org.eclipse.andmore.internal.editors.layout.gle2.LayoutCanvas;
import org.eclipse.andmore.internal.editors.layout.gle2.RenderService;
import org.eclipse.andmore.internal.editors.layout.gle2.SelectionManager;
import org.eclipse.andmore.internal.editors.layout.gle2.SwtUtils;
import org.eclipse.andmore.internal.editors.layout.gre.NodeProxy;
import org.eclipse.andmore.internal.preferences.AdtPrefs;
import org.eclipse.andmore.internal.refactorings.core.RenameResourceWizard;
import org.eclipse.andmore.internal.refactorings.core.RenameResult;
import org.eclipse.andmore.internal.resources.ResourceHelper;
import org.eclipse.andmore.internal.resources.manager.ResourceManager;
import org.eclipse.andmore.internal.ui.ReferenceChooserDialog;
import org.eclipse.andmore.internal.ui.ResourceChooser;
import org.eclipse.andmore.internal.ui.ResourcePreviewHelper;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wb.draw2d.IColorConstants;
import org.eclipse.wb.internal.core.model.property.Property;
import org.eclipse.wb.internal.core.model.property.editor.AbstractTextPropertyEditor;
import org.eclipse.wb.internal.core.model.property.editor.presentation.ButtonPropertyEditorPresentation;
import org.eclipse.wb.internal.core.model.property.editor.presentation.PropertyEditorPresentation;
import org.eclipse.wb.internal.core.model.property.table.PropertyTable;
import org.eclipse.wb.internal.core.utils.ui.DrawUtils;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

/**
 * Special property editor used for the {@link XmlProperty} instances which handles
 * editing the XML properties, rendering defaults by looking up the actual colors and images,
 */
class XmlPropertyEditor extends AbstractTextPropertyEditor {
    public static final XmlPropertyEditor INSTANCE = new XmlPropertyEditor();
    private static final int SAMPLE_SIZE = 10;
    private static final int SAMPLE_MARGIN = 3;

    protected XmlPropertyEditor() {
    }

    private final PropertyEditorPresentation mPresentation =
            new ButtonPropertyEditorPresentation() {
        @Override
        protected void onClick(PropertyTable propertyTable, Property property) throws Exception {
            openDialog(propertyTable, property);
        }
    };

    @Override
    public PropertyEditorPresentation getPresentation() {
        return mPresentation;
    }

    @Override
    public String getText(Property property) throws Exception {
        Object value = property.getValue();
        if (value instanceof String) {
            return (String) value;
        }
        return null;
    }

    @Override
    protected String getEditorText(Property property) throws Exception {
        return getText(property);
    }

    @Override
    public void paint(Property property, GC gc, int x, int y, int width, int height)
            throws Exception {
        String text = getText(property);
        if (text != null) {
            ResourceValue resValue = null;
            String resolvedText = null;

            // TODO: Use the constants for @, ?, @android: etc
            if (text.startsWith("@") || text.startsWith("?")) { //$NON-NLS-1$ //$NON-NLS-2$
                // Yes, try to resolve it in order to show better info
                XmlProperty xmlProperty = (XmlProperty) property;
                GraphicalEditorPart graphicalEditor = xmlProperty.getGraphicalEditor();
                if (graphicalEditor != null) {
                    ResourceResolver resolver = graphicalEditor.getResourceResolver();
                    boolean isFramework = text.startsWith(ANDROID_PREFIX)
                            || text.startsWith(ANDROID_THEME_PREFIX);
                    resValue = resolver.findResValue(text, isFramework);
                    while (resValue != null && resValue.getValue() != null) {
                        String value = resValue.getValue();
                        if (value.startsWith(PREFIX_RESOURCE_REF)
                                || value.startsWith(PREFIX_THEME_REF)) {
                            // TODO: do I have to strip off the @ too?
                            isFramework = isFramework
                                    || value.startsWith(ANDROID_PREFIX)
                                    || value.startsWith(ANDROID_THEME_PREFIX);
                            ResourceValue v = resolver.findResValue(text, isFramework);
                            if (v != null && !value.equals(v.getValue())) {
                                resValue = v;
                            } else {
                                break;
                            }
                        } else {
                            break;
                        }
                    }
                }
            } else if (text.startsWith("#") && text.matches("#\\p{XDigit}+")) { //$NON-NLS-1$
                resValue = new ResourceValue(ResourceType.COLOR, property.getName(), text, false);
            }

            if (resValue != null && resValue.getValue() != null) {
                String value = resValue.getValue();
                // Decide whether it's a color, an image, a nine patch etc
                // and decide how to render it
                if (value.startsWith("#") || value.endsWith(DOT_XML) //$NON-NLS-1$
                        && value.contains("res/color")) { //$NON-NLS-1$ // TBD: File.separator?
                    XmlProperty xmlProperty = (XmlProperty) property;
                    GraphicalEditorPart graphicalEditor = xmlProperty.getGraphicalEditor();
                    if (graphicalEditor != null) {
                        ResourceResolver resolver = graphicalEditor.getResourceResolver();
                        RGB rgb = ResourceHelper.resolveColor(resolver, resValue);
                        if (rgb != null) {
                            Color color = new Color(gc.getDevice(), rgb);
                            // draw color sample
                            Color oldBackground = gc.getBackground();
                            Color oldForeground = gc.getForeground();
                            try {
                                int width_c = SAMPLE_SIZE;
                                int height_c = SAMPLE_SIZE;
                                int x_c = x;
                                int y_c = y + (height - height_c) / 2;
                                // update rest bounds
                                int delta = SAMPLE_SIZE + SAMPLE_MARGIN;
                                x += delta;
                                width -= delta;
                                // fill
                                gc.setBackground(color);
                                gc.fillRectangle(x_c, y_c, width_c, height_c);
                                // draw line
                                gc.setForeground(IColorConstants.gray);
                                gc.drawRectangle(x_c, y_c, width_c, height_c);
                            } finally {
                                gc.setBackground(oldBackground);
                                gc.setForeground(oldForeground);
                            }
                            color.dispose();
                        }
                    }
                } else {
                    Image swtImage = null;
                    if (value.endsWith(DOT_XML) && value.contains("res/drawable")) { // TBD: Filesep?
                        Map<String, Image> cache = getImageCache(property);
                        swtImage = cache.get(value);
                        if (swtImage == null) {
                            XmlProperty xmlProperty = (XmlProperty) property;
                            GraphicalEditorPart graphicalEditor = xmlProperty.getGraphicalEditor();
                            RenderService service = RenderService.create(graphicalEditor);
                            service.setOverrideRenderSize(SAMPLE_SIZE, SAMPLE_SIZE);
                            BufferedImage drawable = service.renderDrawable(resValue);
                            if (drawable != null) {
                                swtImage = SwtUtils.convertToSwt(gc.getDevice(), drawable,
                                        true /*transferAlpha*/, -1);
                                cache.put(value, swtImage);
                            }
                        }
                    } else if (value.endsWith(DOT_PNG)) {
                        // TODO: 9-patch handling?
                        //if (text.endsWith(DOT_9PNG)) {
                        //    // 9-patch image: How do we paint this?
                        //    URL url = new File(text).toURI().toURL();
                        //    NinePatch ninepatch = NinePatch.load(url, false /* ?? */);
                        //    BufferedImage image = ninepatch.getImage();
                        //}
                        Map<String, Image> cache = getImageCache(property);
                        swtImage = cache.get(value);
                        if (swtImage == null) {
                            File file = new File(value);
                            if (file.exists()) {
                                try {
                                    BufferedImage awtImage = ImageIO.read(file);
                                    if (awtImage != null && awtImage.getWidth() > 0
                                            && awtImage.getHeight() > 0) {
                                        awtImage = ImageUtils.cropBlank(awtImage, null);
                                        if (awtImage != null) {
                                            // Scale image
                                            int imageWidth = awtImage.getWidth();
                                            int imageHeight = awtImage.getHeight();
                                            int maxWidth = 3 * height;

                                            if (imageWidth > maxWidth || imageHeight > height) {
                                                double scale = height / (double) imageHeight;
                                                int scaledWidth = (int) (imageWidth * scale);
                                                if (scaledWidth > maxWidth) {
                                                    scale = maxWidth / (double) imageWidth;
                                                }
                                                awtImage = ImageUtils.scale(awtImage, scale,
                                                        scale);
                                            }
                                            swtImage = SwtUtils.convertToSwt(gc.getDevice(),
                                                    awtImage, true /*transferAlpha*/, -1);
                                        }
                                    }
                                } catch (IOException e) {
                                    AdtPlugin.log(e, value);
                                }
                            }
                            cache.put(value, swtImage);
                        }

                    } else if (value != null) {
                        // It's a normal string: if different from the text, paint
                        // it in parentheses, e.g.
                        //   @string/foo: Foo Bar (probably cropped)
                        if (!value.equals(text) && !value.equals("@null")) { //$NON-NLS-1$
                            resolvedText = value;
                        }
                    }

                    if (swtImage != null) {
                        // Make a square the size of the height
                        ImageData imageData = swtImage.getImageData();
                        int imageWidth = imageData.width;
                        int imageHeight = imageData.height;
                        if (imageWidth > 0 && imageHeight > 0) {
                            gc.drawImage(swtImage, x, y + (height - imageHeight) / 2);
                            int delta = imageWidth + SAMPLE_MARGIN;
                            x += delta;
                            width -= delta;
                        }
                    }
                }
            }

            DrawUtils.drawStringCV(gc, text, x, y, width, height);

            if (resolvedText != null && resolvedText.length() > 0) {
                Point size = gc.stringExtent(text);
                x += size.x;
                width -= size.x;

                x += SAMPLE_MARGIN;
                width -= SAMPLE_MARGIN;

                if (width > 0) {
                    Color oldForeground = gc.getForeground();
                    try {
                        gc.setForeground(PropertyTable.COLOR_PROPERTY_FG_DEFAULT);
                        DrawUtils.drawStringCV(gc, '(' + resolvedText + ')', x, y, width, height);
                    } finally {
                        gc.setForeground(oldForeground);
                    }
                }
            }
        }
    }

    @Override
    protected boolean setEditorText(Property property, String text) throws Exception {
        Object oldValue = property.getValue();
        String old = oldValue != null ? oldValue.toString() : null;

        // If users enters a new id without specifying the @id/@+id prefix, insert it
        boolean isId = isIdProperty(property);
        if (isId && !text.startsWith(PREFIX_RESOURCE_REF)) {
            text = NEW_ID_PREFIX + text;
        }

        // Handle id refactoring: if you change an id, may want to update references too.
        // Ask user.
        if (isId && property instanceof XmlProperty
                && old != null && !old.isEmpty()
                && text != null && !text.isEmpty()
                && !text.equals(old)) {
            XmlProperty xmlProperty = (XmlProperty) property;
            IPreferenceStore store = AdtPlugin.getDefault().getPreferenceStore();
            String refactorPref = store.getString(AdtPrefs.PREFS_REFACTOR_IDS);
            boolean performRefactor = false;
            Shell shell = AdtPlugin.getShell();
            if (refactorPref == null
                    || refactorPref.isEmpty()
                    || refactorPref.equals(MessageDialogWithToggle.PROMPT)) {
                MessageDialogWithToggle dialog =
                        MessageDialogWithToggle.openYesNoCancelQuestion(
                    shell,
                    "Update References?",
                    "Update all references as well? " +
                    "This will update all XML references and Java R field references.",
                    "Do not show again",
                    false,
                    store,
                    AdtPrefs.PREFS_REFACTOR_IDS);
                switch (dialog.getReturnCode()) {
                    case IDialogConstants.CANCEL_ID:
                        return false;
                    case IDialogConstants.YES_ID:
                        performRefactor = true;
                        break;
                    case IDialogConstants.NO_ID:
                        performRefactor = false;
                        break;
                }
            } else {
                performRefactor = refactorPref.equals(MessageDialogWithToggle.ALWAYS);
            }
            if (performRefactor) {
                CommonXmlEditor xmlEditor = xmlProperty.getXmlEditor();
                if (xmlEditor != null) {
                    IProject project = xmlEditor.getProject();
                    if (project != null && shell != null) {
                        RenameResourceWizard.renameResource(shell, project,
                                ResourceType.ID, stripIdPrefix(old), stripIdPrefix(text), false);
                    }
                }
            }
        }

        property.setValue(text);

        return true;
    }

    private static boolean isIdProperty(Property property) {
        XmlProperty xmlProperty = (XmlProperty) property;
        return xmlProperty.getDescriptor().getXmlLocalName().equals(ATTR_ID);
    }

    private void openDialog(PropertyTable propertyTable, Property property) throws Exception {
        XmlProperty xmlProperty = (XmlProperty) property;
        IAttributeInfo attributeInfo = xmlProperty.getDescriptor().getAttributeInfo();

        if (isIdProperty(property)) {
            Object value = xmlProperty.getValue();
            if (value != null && !value.toString().isEmpty()) {
                GraphicalEditorPart editor = xmlProperty.getGraphicalEditor();
                if (editor != null) {
                    LayoutCanvas canvas = editor.getCanvasControl();
                    SelectionManager manager = canvas.getSelectionManager();

                    NodeProxy primary = canvas.getNodeFactory().create(xmlProperty.getNode());
                    if (primary != null) {
                        RenameResult result = manager.performRename(primary, null);
                        if (result.isCanceled()) {
                            return;
                        } else if (!result.isUnavailable()) {
                            String name = result.getName();
                            String id = NEW_ID_PREFIX + BaseViewRule.stripIdPrefix(name);
                            xmlProperty.setValue(id);
                            return;
                        }
                    }
                }
            }

            // When editing the id attribute, don't offer a resource chooser: usually
            // you want to enter a *new* id here
            attributeInfo = null;
        }

        boolean referenceAllowed = false;
        if (attributeInfo != null) {
            EnumSet<Format> formats = attributeInfo.getFormats();
            ResourceType type = null;
            List<ResourceType> types = null;
            if (formats.contains(Format.FLAG)) {
                String[] flagValues = attributeInfo.getFlagValues();
                if (flagValues != null) {
                    FlagXmlPropertyDialog dialog =
                        new FlagXmlPropertyDialog(propertyTable.getShell(),
                                "Select Flag Values", false /* radio */,
                                flagValues, xmlProperty);

                    dialog.open();
                    return;
                }
            } else if (formats.contains(Format.ENUM)) {
                String[] enumValues = attributeInfo.getEnumValues();
                if (enumValues != null) {
                    FlagXmlPropertyDialog dialog =
                        new FlagXmlPropertyDialog(propertyTable.getShell(),
                                "Select Enum Value", true /* radio */,
                                enumValues, xmlProperty);
                    dialog.open();
                    return;
                }
            } else {
                for (Format format : formats) {
                    ResourceType t = format.getResourceType();
                    if (t != null) {
                        if (type != null) {
                            if (types == null) {
                                types = new ArrayList<ResourceType>();
                                types.add(type);
                            }
                            types.add(t);
                        }
                        type = t;
                    } else if (format == Format.REFERENCE) {
                        referenceAllowed = true;
                    }
                }
            }
            if (types != null || referenceAllowed) {
                // Multiple resource types (such as string *and* boolean):
                // just use a reference chooser
                GraphicalEditorPart graphicalEditor = xmlProperty.getGraphicalEditor();
                if (graphicalEditor != null) {
                    LayoutEditorDelegate delegate = graphicalEditor.getEditorDelegate();
                    IProject project = delegate.getEditor().getProject();
                    if (project != null) {
                        // get the resource repository for this project and the system resources.
                        ResourceRepository projectRepository =
                            ResourceManager.getInstance().getProjectResources(project);
                        Shell shell = AdtPlugin.getShell();
                        ReferenceChooserDialog dlg = new ReferenceChooserDialog(
                                project,
                                projectRepository,
                                shell);
                        dlg.setPreviewHelper(new ResourcePreviewHelper(dlg, graphicalEditor));

                        String currentValue = (String) property.getValue();
                        dlg.setCurrentResource(currentValue);

                        if (dlg.open() == Window.OK) {
                            String resource = dlg.getCurrentResource();
                            if (resource != null) {
                                // Returns null for cancel, "" for clear and otherwise a new value
                                if (resource.length() > 0) {
                                    property.setValue(resource);
                                } else {
                                    property.setValue(null);
                                }
                            }
                        }

                        return;
                    }
                }
            } else if (type != null) {
                // Single resource type: use a resource chooser
                GraphicalEditorPart graphicalEditor = xmlProperty.getGraphicalEditor();
                if (graphicalEditor != null) {
                    String currentValue = (String) property.getValue();
                    // TODO: Add validator factory?
                    String resource = ResourceChooser.chooseResource(graphicalEditor,
                            type, currentValue, null /* validator */);
                    // Returns null for cancel, "" for clear and otherwise a new value
                    if (resource != null) {
                        if (resource.length() > 0) {
                            property.setValue(resource);
                        } else {
                            property.setValue(null);
                        }
                    }
                }

                return;
            }
        }

        // Fallback: Just use a plain string editor
        StringXmlPropertyDialog dialog =
                new StringXmlPropertyDialog(propertyTable.getShell(), property);
        if (dialog.open() == Window.OK) {
            // TODO: Do I need to activate?
        }
    }

    /** Qualified name for the per-project persistent property include-map */
    private final static QualifiedName CACHE_NAME = new QualifiedName(AdtPlugin.PLUGIN_ID,
            "property-images");//$NON-NLS-1$

    @NonNull
    private static Map<String, Image> getImageCache(@NonNull Property property) {
        XmlProperty xmlProperty = (XmlProperty) property;
        GraphicalEditorPart graphicalEditor = xmlProperty.getGraphicalEditor();
        IProject project = graphicalEditor.getProject();
        try {
            Map<String, Image> cache = (Map<String, Image>) project.getSessionProperty(CACHE_NAME);
            if (cache == null) {
                cache = Maps.newHashMap();
                project.setSessionProperty(CACHE_NAME, cache);
            }

            return cache;
        } catch (CoreException e) {
            AdtPlugin.log(e, null);
            return Maps.newHashMap();
        }
    }
}

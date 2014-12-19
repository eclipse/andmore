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

package org.eclipse.andmore.internal.editors.layout.gle2;

import static com.android.SdkConstants.DOT_PNG;
import static com.android.SdkConstants.FQCN_DATE_PICKER;
import static com.android.SdkConstants.FQCN_EXPANDABLE_LIST_VIEW;
import static com.android.SdkConstants.FQCN_LIST_VIEW;
import static com.android.SdkConstants.FQCN_TIME_PICKER;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.rendering.LayoutLibrary;
import com.android.ide.common.rendering.api.Capability;
import com.android.ide.common.rendering.api.RenderSession;
import com.android.ide.common.rendering.api.ResourceValue;
import com.android.ide.common.rendering.api.SessionParams.RenderingMode;
import com.android.ide.common.rendering.api.StyleResourceValue;
import com.android.ide.common.rendering.api.ViewInfo;
import com.android.ide.common.resources.ResourceResolver;
import com.android.sdklib.IAndroidTarget;
import com.android.utils.Pair;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.editors.descriptors.DocumentDescriptor;
import org.eclipse.andmore.internal.editors.descriptors.ElementDescriptor;
import org.eclipse.andmore.internal.editors.layout.LayoutEditorDelegate;
import org.eclipse.andmore.internal.editors.layout.gre.PaletteMetadataDescriptor;
import org.eclipse.andmore.internal.editors.layout.gre.ViewMetadataRepository;
import org.eclipse.andmore.internal.editors.layout.gre.ViewMetadataRepository.RenderMode;
import org.eclipse.andmore.internal.editors.uimodel.UiDocumentNode;
import org.eclipse.andmore.internal.editors.uimodel.UiElementNode;
import org.eclipse.andmore.internal.resources.ResourceHelper;
import org.eclipse.andmore.internal.sdk.AndroidTargetData;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.RGB;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.imageio.ImageIO;

/**
 * Factory which can provide preview icons for android views of a particular SDK and
 * editor's configuration chooser
 */
public class PreviewIconFactory {
    private PaletteControl mPalette;
    private RGB mBackground;
    private RGB mForeground;
    private File mImageDir;

    private static final String PREVIEW_INFO_FILE = "preview.properties"; //$NON-NLS-1$

    public PreviewIconFactory(PaletteControl palette) {
        mPalette = palette;
    }

    /**
     * Resets the state in the preview icon factory such that it will re-fetch information
     * like the theme and SDK (the icons themselves are cached in a directory across IDE
     * session though)
     */
    public void reset() {
        mImageDir = null;
        mBackground = null;
        mForeground = null;
    }

    /**
     * Deletes all the persistent state for the current settings such that it will be regenerated
     */
    public void refresh() {
        File imageDir = getImageDir(false);
        if (imageDir != null && imageDir.exists()) {
            File[] files = imageDir.listFiles();
            for (File file : files) {
                file.delete();
            }
            imageDir.delete();
            reset();
        }
    }

    /**
     * Returns an image descriptor for the given element descriptor, or null if no image
     * could be computed. The rendering parameters (SDK, theme etc) correspond to those
     * stored in the associated palette.
     *
     * @param desc the element descriptor to get an image for
     * @return an image descriptor, or null if no image could be rendered
     */
    public ImageDescriptor getImageDescriptor(ElementDescriptor desc) {
        File imageDir = getImageDir(false);
        if (!imageDir.exists()) {
            render();
        }
        File file = new File(imageDir, getFileName(desc));
        if (file.exists()) {
            try {
                return ImageDescriptor.createFromURL(file.toURI().toURL());
            } catch (MalformedURLException e) {
                AdtPlugin.log(e, "Could not create image descriptor for %s", file);
            }
        }

        return null;
    }

    /**
     * Partition the elements in the document according to their rendering preferences;
     * elements that should be skipped are removed, elements that should be rendered alone
     * are placed in their own list, etc
     *
     * @param document the document containing render fragments for the various elements
     * @return
     */
    private List<List<Element>> partitionRenderElements(Document document) {
        List<List<Element>> elements = new ArrayList<List<Element>>();

        List<Element> shared = new ArrayList<Element>();
        Element root = document.getDocumentElement();
        elements.add(shared);

        ViewMetadataRepository repository = ViewMetadataRepository.get();

        NodeList children = root.getChildNodes();
        for (int i = 0, n = children.getLength(); i < n; i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                String fqn = repository.getFullClassName(element);
                assert fqn.length() > 0 : element.getNodeName();
                RenderMode renderMode = repository.getRenderMode(fqn);

                // Temporary special cases
                if (fqn.equals(FQCN_LIST_VIEW) || fqn.equals(FQCN_EXPANDABLE_LIST_VIEW)) {
                    if (!mPalette.getEditor().renderingSupports(Capability.ADAPTER_BINDING)) {
                        renderMode = RenderMode.SKIP;
                    }
                } else if (fqn.equals(FQCN_DATE_PICKER) || fqn.equals(FQCN_TIME_PICKER)) {
                    IAndroidTarget renderingTarget = mPalette.getEditor().getRenderingTarget();
                    // In Honeycomb, these widgets only render properly in the Holo themes.
                    int apiLevel = renderingTarget.getVersion().getApiLevel();
                    if (apiLevel == 11) {
                        String themeName = mPalette.getCurrentTheme();
                        if (themeName == null || !themeName.startsWith("Theme.Holo")) { //$NON-NLS-1$
                            // Note - it's possible that the the theme is some other theme
                            // such as a user theme which inherits from Theme.Holo and that
                            // the render -would- have worked, but it's harder to detect that
                            // scenario, so we err on the side of caution and just show an
                            // icon + name for the time widgets.
                            renderMode = RenderMode.SKIP;
                        }
                    } else if (apiLevel >= 12) {
                        // Currently broken, even for Holo.
                        renderMode = RenderMode.SKIP;
                    } // apiLevel <= 10 is fine
                }

                if (renderMode == RenderMode.ALONE) {
                    elements.add(Collections.singletonList(element));
                } else if (renderMode == RenderMode.NORMAL) {
                    shared.add(element);
                } else {
                    assert renderMode == RenderMode.SKIP;
                }
            }
        }

        return elements;
    }

    /**
     * Renders ALL the widgets and then extracts image data for each view and saves it on
     * disk
     */
    private boolean render() {
        File imageDir = getImageDir(true);

        GraphicalEditorPart editor = mPalette.getEditor();
        LayoutEditorDelegate layoutEditorDelegate = editor.getEditorDelegate();
        LayoutLibrary layoutLibrary = editor.getLayoutLibrary();
        Integer overrideBgColor = null;
        if (layoutLibrary != null) {
            if (layoutLibrary.supports(Capability.CUSTOM_BACKGROUND_COLOR)) {
                Pair<RGB, RGB> themeColors = getColorsFromTheme();
                RGB bg = themeColors.getFirst();
                RGB fg = themeColors.getSecond();
                if (bg != null) {
                    storeBackground(imageDir, bg, fg);
                    overrideBgColor = Integer.valueOf(ImageUtils.rgbToInt(bg, 0xFF));
                }
            }
        }

        ViewMetadataRepository repository = ViewMetadataRepository.get();
        Document document = repository.getRenderingConfigDoc();

        if (document == null) {
            return false;
        }

        // Construct UI model from XML
        AndroidTargetData data = layoutEditorDelegate.getEditor().getTargetData();
        DocumentDescriptor documentDescriptor;
        if (data == null) {
            documentDescriptor = new DocumentDescriptor("temp", null/*children*/);//$NON-NLS-1$
        } else {
            documentDescriptor = data.getLayoutDescriptors().getDescriptor();
        }
        UiDocumentNode model = (UiDocumentNode) documentDescriptor.createUiNode();
        model.setEditor(layoutEditorDelegate.getEditor());
        model.setUnknownDescriptorProvider(editor.getModel().getUnknownDescriptorProvider());

        Element documentElement = document.getDocumentElement();
        List<List<Element>> elements = partitionRenderElements(document);
        for (List<Element> elementGroup : elements) {
            // Replace the document elements with the current element group
            while (documentElement.getFirstChild() != null) {
                documentElement.removeChild(documentElement.getFirstChild());
            }
            for (Element element : elementGroup) {
                documentElement.appendChild(element);
            }

            model.loadFromXmlNode(document);

            RenderSession session = null;
            NodeList childNodes = documentElement.getChildNodes();
            try {
                // Important to get these sizes large enough for clients that don't support
                // RenderMode.FULL_EXPAND such as 1.6
                int width = 200;
                int height = childNodes.getLength() == 1 ? 400 : 1600;

                session = RenderService.create(editor)
                    .setModel(model)
                    .setOverrideRenderSize(width, height)
                    .setRenderingMode(RenderingMode.FULL_EXPAND)
                    .setLog(editor.createRenderLogger("palette"))
                    .setOverrideBgColor(overrideBgColor)
                    .setDecorations(false)
                    .createRenderSession();
            } catch (Throwable t) {
                // If there are internal errors previewing the components just revert to plain
                // icons and labels
                continue;
            }

            if (session != null) {
                if (session.getResult().isSuccess()) {
                    BufferedImage image = session.getImage();
                    if (image != null && image.getWidth() > 0 && image.getHeight() > 0) {

                        // Fallback for older platforms where we couldn't do background rendering
                        // at the beginning of this method
                        if (mBackground == null) {
                            Pair<RGB, RGB> themeColors = getColorsFromTheme();
                            RGB bg = themeColors.getFirst();
                            RGB fg = themeColors.getSecond();

                            if (bg == null) {
                                // Just use a pixel from the rendering instead.
                                int p = image.getRGB(image.getWidth() - 1, image.getHeight() - 1);
                                // However, in this case we don't trust the foreground color
                                // even if one was found in the themes; pick one that is guaranteed
                                // to contrast with the background
                                bg = ImageUtils.intToRgb(p);
                                if (ImageUtils.getBrightness(ImageUtils.rgbToInt(bg, 255)) < 128) {
                                    fg = new RGB(255, 255, 255);
                                } else {
                                    fg = new RGB(0, 0, 0);
                                }
                            }
                            storeBackground(imageDir, bg, fg);
                            assert mBackground != null;
                        }

                        List<ViewInfo> viewInfoList = session.getRootViews();
                        if (viewInfoList != null && viewInfoList.size() > 0) {
                            // We don't render previews under a <merge> so there should
                            // only be one root.
                            ViewInfo firstRoot = viewInfoList.get(0);
                            int parentX = firstRoot.getLeft();
                            int parentY = firstRoot.getTop();
                            List<ViewInfo> infos = firstRoot.getChildren();
                            for (ViewInfo info : infos) {
                                Object cookie = info.getCookie();
                                if (!(cookie instanceof UiElementNode)) {
                                    continue;
                                }
                                UiElementNode node = (UiElementNode) cookie;
                                String fileName = getFileName(node);
                                File file = new File(imageDir, fileName);
                                if (file.exists()) {
                                    // On Windows, perhaps we need to rename instead?
                                    file.delete();
                                }
                                int x1 = parentX + info.getLeft();
                                int y1 = parentY + info.getTop();
                                int x2 = parentX + info.getRight();
                                int y2 = parentY + info.getBottom();
                                if (x1 != x2 && y1 != y2) {
                                    savePreview(file, image, x1, y1, x2, y2);
                                }
                            }
                        }
                    }
                } else {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0, n = childNodes.getLength(); i < n; i++) {
                        Node node = childNodes.item(i);
                        if (node instanceof Element) {
                            Element e = (Element) node;
                            String fqn = repository.getFullClassName(e);
                            fqn = fqn.substring(fqn.lastIndexOf('.') + 1);
                            if (sb.length() > 0) {
                                sb.append(", "); //$NON-NLS-1$
                            }
                            sb.append(fqn);
                        }
                    }
                    AdtPlugin.log(IStatus.WARNING, "Failed to render set of icons for %1$s",
                            sb.toString());

                    if (session.getResult().getException() != null) {
                        AdtPlugin.log(session.getResult().getException(),
                                session.getResult().getErrorMessage());
                    } else if (session.getResult().getErrorMessage() != null) {
                        AdtPlugin.log(IStatus.WARNING, session.getResult().getErrorMessage());
                    }
                }

                session.dispose();
            }
        }

        mPalette.getEditor().recomputeLayout();

        return true;
    }

    /**
     * Look up the background and foreground colors from the theme. May not find either
     * the background or foreground or both, but will always return a pair of possibly
     * null colors.
     *
     * @return a pair of possibly null color descriptions
     */
    @NonNull
    private Pair<RGB, RGB> getColorsFromTheme() {
        RGB background = null;
        RGB foreground = null;

        ResourceResolver resources = mPalette.getEditor().getResourceResolver();
        if (resources == null) {
            return Pair.of(background, foreground);
        }
        StyleResourceValue theme = resources.getCurrentTheme();
        if (theme != null) {
            background = resolveThemeColor(resources, "windowBackground"); //$NON-NLS-1$
            if (background == null) {
                background = renderDrawableResource("windowBackground"); //$NON-NLS-1$
                // This causes some harm with some themes: We'll find a color, say black,
                // that isn't actually rendered in the theme. Better to use null here,
                // which will cause the caller to pick a pixel from the observed background
                // instead.
                //if (background == null) {
                //    background = resolveThemeColor(resources, "colorBackground"); //$NON-NLS-1$
                //}
            }
            foreground = resolveThemeColor(resources, "textColorPrimary"); //$NON-NLS-1$
        }

        // Ensure that the foreground color is suitably distinct from the background color
        if (background != null) {
            int bgRgb = ImageUtils.rgbToInt(background, 0xFF);
            int backgroundBrightness = ImageUtils.getBrightness(bgRgb);
            if (foreground == null) {
                if (backgroundBrightness < 128) {
                    foreground = new RGB(255, 255, 255);
                } else {
                    foreground = new RGB(0, 0, 0);
                }
            } else {
                int fgRgb = ImageUtils.rgbToInt(foreground, 0xFF);
                int foregroundBrightness = ImageUtils.getBrightness(fgRgb);
                if (Math.abs(backgroundBrightness - foregroundBrightness) < 64) {
                    if (backgroundBrightness < 128) {
                        foreground = new RGB(255, 255, 255);
                    } else {
                        foreground = new RGB(0, 0, 0);
                    }
                }
            }
        }

        return Pair.of(background, foreground);
    }

    /**
     * Renders the given resource which should refer to a drawable and returns a
     * representative color value for the drawable (such as the color in the center)
     *
     * @param themeItemName the item in the theme to be looked up and rendered
     * @return a color representing a typical color in the drawable
     */
    private RGB renderDrawableResource(String themeItemName) {
        GraphicalEditorPart editor = mPalette.getEditor();
        ResourceResolver resources = editor.getResourceResolver();
        ResourceValue resourceValue = resources.findItemInTheme(themeItemName);
        BufferedImage image = RenderService.create(editor)
            .setOverrideRenderSize(100, 100)
            .renderDrawable(resourceValue);
        if (image != null) {
            // Use the middle pixel as the color since that works better for gradients;
            // solid colors work too.
            int rgb = image.getRGB(image.getWidth() / 2, image.getHeight() / 2);
            return ImageUtils.intToRgb(rgb);
        }

        return null;
    }

    private static RGB resolveThemeColor(ResourceResolver resources, String resourceName) {
        ResourceValue textColor = resources.findItemInTheme(resourceName);
        return ResourceHelper.resolveColor(resources, textColor);
    }

    private String getFileName(ElementDescriptor descriptor) {
        if (descriptor instanceof PaletteMetadataDescriptor) {
            PaletteMetadataDescriptor pmd = (PaletteMetadataDescriptor) descriptor;
            StringBuilder sb = new StringBuilder();
            String name = pmd.getUiName();
            // Strip out whitespace, parentheses, etc.
            for (int i = 0, n = name.length(); i < n; i++) {
                char c = name.charAt(i);
                if (Character.isLetter(c)) {
                    sb.append(c);
                }
            }
            return sb.toString() + DOT_PNG;
        }
        return descriptor.getUiName() + DOT_PNG;
    }

    private String getFileName(UiElementNode node) {
        ViewMetadataRepository repository = ViewMetadataRepository.get();
        String fqn = repository.getFullClassName((Element) node.getXmlNode());
        return fqn.substring(fqn.lastIndexOf('.') + 1) + DOT_PNG;
    }

    /**
     * Cleans up a name by removing punctuation and whitespace etc to make
     * it a better filename
     * @param name the name to clean
     * @return a cleaned up name
     */
    @NonNull
    private static String cleanup(@Nullable String name) {
        if (name == null) {
            return "";
        }

        // Extract just the characters (no whitespace, parentheses, punctuation etc)
        // to ensure that the filename is pretty portable
        StringBuilder sb = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isJavaIdentifierPart(c)) {
                sb.append(Character.toLowerCase(c));
            }
        }

        return sb.toString();
    }

    /** Returns the location of a directory containing image previews (which may not exist) */
    private File getImageDir(boolean create) {
        if (mImageDir == null) {
            // Location for plugin-related state data
            IPath pluginState = AdtPlugin.getDefault().getStateLocation();

            // We have multiple directories - one for each combination of SDK, theme and device
            // (and later, possibly other qualifiers).
            // These are created -lazily-.
            String targetName = mPalette.getCurrentTarget().hashString();
            String androidTargetNamePrefix = "android-";
            String themeNamePrefix = "Theme.";
            if (targetName.startsWith(androidTargetNamePrefix)) {
                targetName = targetName.substring(androidTargetNamePrefix.length());
            }
            String themeName = mPalette.getCurrentTheme();
            if (themeName == null) {
                themeName = "Theme"; //$NON-NLS-1$
            }
            if (themeName.startsWith(themeNamePrefix)) {
                themeName = themeName.substring(themeNamePrefix.length());
            }
            targetName = cleanup(targetName);
            themeName = cleanup(themeName);
            String deviceName = cleanup(mPalette.getCurrentDevice());
            String dirName = String.format("palette-preview-r16b-%s-%s-%s", targetName,
                    themeName, deviceName);
            IPath dirPath = pluginState.append(dirName);

            mImageDir = new File(dirPath.toOSString());
        }

        if (create && !mImageDir.exists()) {
            mImageDir.mkdirs();
        }

        return mImageDir;
    }

    private void savePreview(File output, BufferedImage image,
            int left, int top, int right, int bottom) {
        try {
            BufferedImage im = ImageUtils.subImage(image, left, top, right, bottom);
            ImageIO.write(im, "PNG", output); //$NON-NLS-1$
        } catch (IOException e) {
            AdtPlugin.log(e, "Failed writing palette file");
        }
    }

    private void storeBackground(File imageDir, RGB bg, RGB fg) {
        mBackground = bg;
        mForeground = fg;
        File file = new File(imageDir, PREVIEW_INFO_FILE);
        String colors = String.format(
                "background=#%02x%02x%02x\nforeground=#%02x%02x%02x\n", //$NON-NLS-1$
                bg.red, bg.green, bg.blue,
                fg.red, fg.green, fg.blue);
        AdtPlugin.writeFile(file, colors);
    }

    public RGB getBackgroundColor() {
        if (mBackground == null) {
            initColors();
        }

        return mBackground;
    }

    public RGB getForegroundColor() {
        if (mForeground == null) {
            initColors();
        }

        return mForeground;
    }

    public void initColors() {
        try {
            // Already initialized? Foreground can be null which would call
            // initColors again and again, but background is never null after
            // initialization so we use it as the have-initialized flag.
            if (mBackground != null) {
                return;
            }

            File imageDir = getImageDir(false);
            if (!imageDir.exists()) {
                render();

                // Initialized as part of the render
                if (mBackground != null) {
                    return;
                }
            }

            File file = new File(imageDir, PREVIEW_INFO_FILE);
            if (file.exists()) {
                Properties properties = new Properties();
                InputStream is = null;
                try {
                    is = new BufferedInputStream(new FileInputStream(file));
                    properties.load(is);
                } catch (IOException e) {
                    AdtPlugin.log(e, "Can't read preview properties");
                } finally {
                    if (is != null) {
                        try {
                            is.close();
                        } catch (IOException e) {
                            // Nothing useful can be done.
                        }
                    }
                }

                String colorString = (String) properties.get("background"); //$NON-NLS-1$
                if (colorString != null) {
                    int rgb = ImageUtils.getColor(colorString.trim());
                    mBackground = ImageUtils.intToRgb(rgb);
                }
                colorString = (String) properties.get("foreground"); //$NON-NLS-1$
                if (colorString != null) {
                    int rgb = ImageUtils.getColor(colorString.trim());
                    mForeground = ImageUtils.intToRgb(rgb);
                }
            }

            if (mBackground == null) {
                mBackground = new RGB(0, 0, 0);
            }
            // mForeground is allowed to be null.
        } catch (Throwable t) {
            AdtPlugin.log(t, "Cannot initialize preview color settings");
        }
    }
}

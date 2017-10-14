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

import static com.android.SdkConstants.LAYOUT_RESOURCE_PREFIX;

import com.android.annotations.NonNull;
import com.android.ide.common.api.IClientRulesEngine;
import com.android.ide.common.api.INode;
import com.android.ide.common.api.Rect;
import com.android.ide.common.rendering.HardwareConfigHelper;
import com.android.ide.common.rendering.LayoutLibrary;
import org.eclipse.andmore.internal.editors.layout.RenderSecurityManager;
import com.android.ide.common.rendering.api.Capability;
import com.android.ide.common.rendering.api.DrawableParams;
import com.android.ide.common.rendering.api.HardwareConfig;
import com.android.ide.common.rendering.api.IImageFactory;
import com.android.ide.common.rendering.api.ILayoutPullParser;
import com.android.ide.common.rendering.api.LayoutLog;
import com.android.ide.common.rendering.api.RenderSession;
import com.android.ide.common.rendering.api.ResourceValue;
import com.android.ide.common.rendering.api.Result;
import com.android.ide.common.rendering.api.SessionParams;
import com.android.ide.common.rendering.api.SessionParams.RenderingMode;
import com.android.ide.common.rendering.api.ViewInfo;
import com.android.ide.common.resources.ResourceResolver;
import com.android.ide.common.resources.configuration.FolderConfiguration;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.devices.Device;
import com.google.common.base.Charsets;
import com.google.common.io.Files;

import org.eclipse.andmore.AndmoreAndroidPlugin;
import org.eclipse.andmore.AdtUtils;
import org.eclipse.andmore.internal.editors.layout.ContextPullParser;
import org.eclipse.andmore.internal.editors.layout.ProjectCallback;
import org.eclipse.andmore.internal.editors.layout.UiElementPullParser;
import org.eclipse.andmore.internal.editors.layout.configuration.Configuration;
import org.eclipse.andmore.internal.editors.layout.configuration.ConfigurationChooser;
import org.eclipse.andmore.internal.editors.layout.configuration.Locale;
import org.eclipse.andmore.internal.editors.layout.gle2.IncludeFinder.Reference;
import org.eclipse.andmore.internal.editors.layout.gre.NodeFactory;
import org.eclipse.andmore.internal.editors.layout.gre.NodeProxy;
import org.eclipse.andmore.internal.editors.layout.uimodel.UiViewElementNode;
import org.eclipse.andmore.internal.editors.manifest.ManifestInfo;
import org.eclipse.andmore.internal.editors.manifest.ManifestInfo.ActivityAttributes;
import org.eclipse.andmore.internal.editors.uimodel.UiDocumentNode;
import org.eclipse.andmore.internal.editors.uimodel.UiElementNode;
import org.eclipse.andmore.internal.sdk.AndroidTargetData;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.core.resources.IProject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The {@link RenderService} provides rendering and layout information for
 * Android layouts. This is a wrapper around the layout library.
 */
public class RenderService {
    private static final Object RENDERING_LOCK = new Object();

    /** Reference to the file being edited. Can also be used to access the {@link IProject}. */
    private final GraphicalEditorPart mEditor;

    // The following fields are inferred from the editor and not customizable by the
    // client of the render service:

    private final IProject mProject;
    private final ProjectCallback mProjectCallback;
    private final ResourceResolver mResourceResolver;
    private final int mMinSdkVersion;
    private final int mTargetSdkVersion;
    private final LayoutLibrary mLayoutLib;
    private final IImageFactory mImageFactory;
    private final HardwareConfigHelper mHardwareConfigHelper;
    private final Locale mLocale;

    // The following fields are optional or configurable using the various chained
    // setters:

    private UiDocumentNode mModel;
    private Reference mIncludedWithin;
    private RenderingMode mRenderingMode = RenderingMode.NORMAL;
    private LayoutLog mLogger;
    private Integer mOverrideBgColor;
    private boolean mShowDecorations = true;
    private Set<UiElementNode> mExpandNodes = Collections.<UiElementNode>emptySet();
    private final Object mCredential;

    /** Use the {@link #create} factory instead */
    private RenderService(GraphicalEditorPart editor, Object credential) {
        mEditor = editor;
        mCredential = credential;

        mProject = editor.getProject();
        LayoutCanvas canvas = editor.getCanvasControl();
        mImageFactory = canvas.getImageOverlay();
        ConfigurationChooser chooser = editor.getConfigurationChooser();
        Configuration config = chooser.getConfiguration();
        FolderConfiguration folderConfig = config.getFullConfig();

        Device device = config.getDevice();
        assert device != null; // Should only attempt render with configuration that has device
        mHardwareConfigHelper = new HardwareConfigHelper(device);
        mHardwareConfigHelper.setOrientation(
                folderConfig.getScreenOrientationQualifier().getValue());

        mLayoutLib = editor.getReadyLayoutLib(true /*displayError*/);
        mResourceResolver = editor.getResourceResolver();
        mProjectCallback = editor.getProjectCallback(true /*reset*/, mLayoutLib);
        mMinSdkVersion = editor.getMinSdkVersion();
        mTargetSdkVersion = editor.getTargetSdkVersion();
        mLocale = config.getLocale();
    }

    private RenderService(GraphicalEditorPart editor,
            Configuration configuration, ResourceResolver resourceResolver,
            Object credential) {
        mEditor = editor;
        mCredential = credential;

        mProject = editor.getProject();
        LayoutCanvas canvas = editor.getCanvasControl();
        mImageFactory = canvas.getImageOverlay();
        FolderConfiguration folderConfig = configuration.getFullConfig();

        Device device = configuration.getDevice();
        assert device != null;
        mHardwareConfigHelper = new HardwareConfigHelper(device);
        mHardwareConfigHelper.setOrientation(
                folderConfig.getScreenOrientationQualifier().getValue());

        mLayoutLib = editor.getReadyLayoutLib(true /*displayError*/);
        mResourceResolver =  resourceResolver != null ? resourceResolver : editor.getResourceResolver();
        mProjectCallback = editor.getProjectCallback(true /*reset*/, mLayoutLib);
        mMinSdkVersion = editor.getMinSdkVersion();
        mTargetSdkVersion = editor.getTargetSdkVersion();
        mLocale = configuration.getLocale();
    }

    private RenderSecurityManager createSecurityManager() {
        String projectPath = null;
        String sdkPath = null;
        if (RenderSecurityManager.RESTRICT_READS) {
            projectPath = AdtUtils.getAbsolutePath(mProject).toFile().getPath();
            Sdk sdk = Sdk.getCurrent();
            sdkPath = sdk != null ? sdk.getSdkOsLocation() : null;
        }
        RenderSecurityManager securityManager = new RenderSecurityManager(sdkPath, projectPath);
        securityManager.setLogger(AndmoreAndroidPlugin.getDefault());

        // Make sure this is initialized before we attempt to use it from layoutlib
        Toolkit.getDefaultToolkit();

        return securityManager;
      }

    /**
     * Returns true if this configuration supports the given rendering
     * capability
     *
     * @param target the target to look up the layout library for
     * @param capability the capability to check
     * @return true if the capability is supported
     */
    public static boolean supports(
            @NonNull IAndroidTarget target,
            @NonNull Capability capability) {
        Sdk sdk = Sdk.getCurrent();
        if (sdk != null) {
            AndroidTargetData targetData = sdk.getTargetData(target);
            if (targetData != null) {
                LayoutLibrary layoutLib = targetData.getLayoutLibrary();
                if (layoutLib != null) {
                    return layoutLib.supports(capability);
                }
            }
        }

        return false;
    }

    /**
     * Creates a new {@link RenderService} associated with the given editor.
     *
     * @param editor the editor to provide configuration data such as the render target
     * @return a {@link RenderService} which can perform rendering services
     */
    public static RenderService create(GraphicalEditorPart editor) {
        // Delegate to editor such that it can pass its credential to the service
        return editor.createRenderService();
    }

    /**
     * Creates a new {@link RenderService} associated with the given editor.
     *
     * @param editor the editor to provide configuration data such as the render target
     * @param credential the sandbox credential
     * @return a {@link RenderService} which can perform rendering services
     */
    @NonNull
    public static RenderService create(GraphicalEditorPart editor, Object credential) {
        return new RenderService(editor, credential);
    }

    /**
     * Creates a new {@link RenderService} associated with the given editor.
     *
     * @param editor the editor to provide configuration data such as the render target
     * @param configuration the configuration to use (and fallback to editor for the rest)
     * @param resolver a resource resolver to use to look up resources
     * @return a {@link RenderService} which can perform rendering services
     */
    public static RenderService create(GraphicalEditorPart editor,
            Configuration configuration, ResourceResolver resolver) {
        // Delegate to editor such that it can pass its credential to the service
        return editor.createRenderService(configuration, resolver);
    }

    /**
     * Creates a new {@link RenderService} associated with the given editor.
     *
     * @param editor the editor to provide configuration data such as the render target
     * @param configuration the configuration to use (and fallback to editor for the rest)
     * @param resolver a resource resolver to use to look up resources
     * @param credential the sandbox credential
     * @return a {@link RenderService} which can perform rendering services
     */
    public static RenderService create(GraphicalEditorPart editor,
            Configuration configuration, ResourceResolver resolver, Object credential) {
        return new RenderService(editor, configuration, resolver, credential);
    }

    /**
     * Renders the given model, using this editor's theme and screen settings, and returns
     * the result as a {@link RenderSession}.
     *
     * @param model the model to be rendered, which can be different than the editor's own
     *            {@link #getModel()}.
     * @param width the width to use for the layout, or -1 to use the width of the screen
     *            associated with this editor
     * @param height the height to use for the layout, or -1 to use the height of the screen
     *            associated with this editor
     * @param explodeNodes a set of nodes to explode, or null for none
     * @param overrideBgColor If non-null, use the given color as a background to render over
     *        rather than the normal background requested by the theme
     * @param noDecor If true, don't draw window decorations like the system bar
     * @param logger a logger where rendering errors are reported
     * @param renderingMode the {@link RenderingMode} to use for rendering
     * @return the resulting rendered image wrapped in an {@link RenderSession}
     */

    /**
     * Sets the {@link LayoutLog} to be used during rendering. If none is specified, a
     * silent logger will be used.
     *
     * @param logger the log to be used
     * @return this (such that chains of setters can be stringed together)
     */
    public RenderService setLog(LayoutLog logger) {
        mLogger = logger;
        return this;
    }

    /**
     * Sets the model to be rendered, which can be different than the editor's own
     * {@link GraphicalEditorPart#getModel()}.
     *
     * @param model the model to be rendered
     * @return this (such that chains of setters can be stringed together)
     */
    public RenderService setModel(UiDocumentNode model) {
        mModel = model;
        return this;
    }

    /**
     * Overrides the width and height to be used during rendering (which might be adjusted if
     * the {@link #setRenderingMode(RenderingMode)} is {@link RenderingMode#FULL_EXPAND}.
     *
     * A value of -1 will make the rendering use the normal width and height coming from the
     * {@link Configuration#getDevice()} object.
     *
     * @param overrideRenderWidth the width in pixels of the layout to be rendered
     * @param overrideRenderHeight the height in pixels of the layout to be rendered
     * @return this (such that chains of setters can be stringed together)
     */
    public RenderService setOverrideRenderSize(int overrideRenderWidth, int overrideRenderHeight) {
        mHardwareConfigHelper.setOverrideRenderSize(overrideRenderWidth, overrideRenderHeight);
        return this;
    }

    /**
     * Sets the max width and height to be used during rendering (which might be adjusted if
     * the {@link #setRenderingMode(RenderingMode)} is {@link RenderingMode#FULL_EXPAND}.
     *
     * A value of -1 will make the rendering use the normal width and height coming from the
     * {@link Configuration#getDevice()} object.
     *
     * @param maxRenderWidth the max width in pixels of the layout to be rendered
     * @param maxRenderHeight the max height in pixels of the layout to be rendered
     * @return this (such that chains of setters can be stringed together)
     */
    public RenderService setMaxRenderSize(int maxRenderWidth, int maxRenderHeight) {
        mHardwareConfigHelper.setMaxRenderSize(maxRenderWidth, maxRenderHeight);
        return this;
    }

    /**
     * Sets the {@link RenderingMode} to be used during rendering. If none is specified,
     * the default is {@link RenderingMode#NORMAL}.
     *
     * @param renderingMode the rendering mode to be used
     * @return this (such that chains of setters can be stringed together)
     */
    public RenderService setRenderingMode(RenderingMode renderingMode) {
        mRenderingMode = renderingMode;
        return this;
    }

    /**
     * Sets the overriding background color to be used, if any. The color should be a
     * bitmask of AARRGGBB. The default is null.
     *
     * @param overrideBgColor the overriding background color to be used in the rendering,
     *            in the form of a AARRGGBB bitmask, or null to use no custom background.
     * @return this (such that chains of setters can be stringed together)
     */
    public RenderService setOverrideBgColor(Integer overrideBgColor) {
        mOverrideBgColor = overrideBgColor;
        return this;
    }

    /**
     * Sets whether the rendering should include decorations such as a system bar, an
     * application bar etc depending on the SDK target and theme. The default is true.
     *
     * @param showDecorations true if the rendering should include system bars etc.
     * @return this (such that chains of setters can be stringed together)
     */
    public RenderService setDecorations(boolean showDecorations) {
        mShowDecorations = showDecorations;
        return this;
    }

    /**
     * Sets the nodes to expand during rendering. These will be padded with approximately
     * 20 pixels and also highlighted by the {@link EmptyViewsOverlay}. The default is an
     * empty collection.
     *
     * @param nodesToExpand the nodes to be expanded
     * @return this (such that chains of setters can be stringed together)
     */
    public RenderService setNodesToExpand(Set<UiElementNode> nodesToExpand) {
        mExpandNodes = nodesToExpand;
        return this;
    }

    /**
     * Sets the {@link Reference} to an outer layout that this layout should be rendered
     * within. The outer layout <b>must</b> contain an include tag which points to this
     * layout. The default is null.
     *
     * @param includedWithin a reference to an outer layout to render this layout within
     * @return this (such that chains of setters can be stringed together)
     */
    public RenderService setIncludedWithin(Reference includedWithin) {
        mIncludedWithin = includedWithin;
        return this;
    }

    /** Initializes any remaining optional fields after all setters have been called */
    private void finishConfiguration() {
        if (mLogger == null) {
            // Silent logging
            mLogger = new LayoutLog();
        }
    }

    /**
     * Renders the model and returns the result as a {@link RenderSession}.
     * @return the {@link RenderSession} resulting from rendering the current model
     */
    public RenderSession createRenderSession() {
        assert mModel != null : "Incomplete service config";
        finishConfiguration();

        if (mResourceResolver == null) {
            // Abort the rendering if the resources are not found.
            return null;
        }

        HardwareConfig hardwareConfig = mHardwareConfigHelper.getConfig();

        UiElementPullParser modelParser = new UiElementPullParser(mModel,
                false, mExpandNodes, hardwareConfig.getDensity(), mProject);
        ILayoutPullParser topParser = modelParser;

        // Code to support editing included layout
        // first reset the layout parser just in case.
        mProjectCallback.setLayoutParser(null, null);

        if (mIncludedWithin != null) {
            // Outer layout name:
            String contextLayoutName = mIncludedWithin.getName();

            // Find the layout file.
            ResourceValue contextLayout = mResourceResolver.findResValue(
                    LAYOUT_RESOURCE_PREFIX + contextLayoutName, false  /* forceFrameworkOnly*/);
            if (contextLayout != null) {
                File layoutFile = new File(contextLayout.getValue());
                if (layoutFile.isFile()) {
                    try {
                        // Get the name of the layout actually being edited, without the extension
                        // as it's what IXmlPullParser.getParser(String) will receive.
                        String queryLayoutName = mEditor.getLayoutResourceName();
                        mProjectCallback.setLayoutParser(queryLayoutName, modelParser);
                        topParser = new ContextPullParser(mProjectCallback, layoutFile);
                        topParser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
                        String xmlText = Files.toString(layoutFile, Charsets.UTF_8);
                        topParser.setInput(new StringReader(xmlText));
                    } catch (IOException e) {
                        AndmoreAndroidPlugin.log(e, null);
                    } catch (XmlPullParserException e) {
                        AndmoreAndroidPlugin.log(e, null);
                    }
                }
            }
        }

        SessionParams params = new SessionParams(
                topParser,
                mRenderingMode,
                mProject /* projectKey */,
                hardwareConfig,
                mResourceResolver,
                mProjectCallback,
                mMinSdkVersion,
                mTargetSdkVersion,
                mLogger);

        // Request margin and baseline information.
        // TODO: Be smarter about setting this; start without it, and on the first request
        // for an extended view info, re-render in the same session, and then set a flag
        // which will cause this to create extended view info each time from then on in the
        // same session
        params.setExtendedViewInfoMode(true);

        params.setLocale(mLocale.toLocaleId());

        ManifestInfo manifestInfo = ManifestInfo.get(mProject);
        try {
            params.setRtlSupport(manifestInfo.isRtlSupported());
        } catch (Exception e) {
            // ignore.
        }
        if (!mShowDecorations) {
            params.setForceNoDecor();
        } else {
            try {
                params.setAppLabel(manifestInfo.getApplicationLabel());
                params.setAppIcon(manifestInfo.getApplicationIcon());
                String activity = mEditor.getConfigurationChooser().getConfiguration().getActivity();
                if (activity != null) {
                    ActivityAttributes info = manifestInfo.getActivityAttributes(activity);
                    if (info != null) {
                        if (info.getLabel() != null) {
                            params.setAppLabel(info.getLabel());
                        }
                        if (info.getIcon() != null) {
                            params.setAppIcon(info.getIcon());
                        }
                    }
                }
            } catch (Exception e) {
                // ignore.
            }
        }

        if (mOverrideBgColor != null) {
            params.setOverrideBgColor(mOverrideBgColor.intValue());
        }

        // set the Image Overlay as the image factory.
        params.setImageFactory(mImageFactory);

        mProjectCallback.setLogger(mLogger);
        mProjectCallback.setResourceResolver(mResourceResolver);
        RenderSecurityManager securityManager = createSecurityManager();
        try {
            securityManager.setActive(true, mCredential);
            synchronized (RENDERING_LOCK) {
                return mLayoutLib.createSession(params);
            }
        } catch (RuntimeException t) {
            // Exceptions from the bridge
            mLogger.error(null, t.getLocalizedMessage(), t, null);
            throw t;
        } finally {
            securityManager.dispose(mCredential);
            mProjectCallback.setLogger(null);
            mProjectCallback.setResourceResolver(null);
        }
    }

    /**
     * Renders the given resource value (which should refer to a drawable) and returns it
     * as an image
     *
     * @param drawableResourceValue the drawable resource value to be rendered, or null
     * @return the image, or null if something went wrong
     */
    public BufferedImage renderDrawable(ResourceValue drawableResourceValue) {
        if (drawableResourceValue == null) {
            return null;
        }

        finishConfiguration();

        HardwareConfig hardwareConfig = mHardwareConfigHelper.getConfig();

        DrawableParams params = new DrawableParams(drawableResourceValue, mProject, hardwareConfig,
                mResourceResolver, mProjectCallback, mMinSdkVersion,
                mTargetSdkVersion, mLogger);
        params.setForceNoDecor();
        Result result = mLayoutLib.renderDrawable(params);
        if (result != null && result.isSuccess()) {
            Object data = result.getData();
            if (data instanceof BufferedImage) {
                return (BufferedImage) data;
            }
        }

        return null;
    }

    /**
     * Measure the children of the given parent node, applying the given filter to the
     * pull parser's attribute values.
     *
     * @param parent the parent node to measure children for
     * @param filter the filter to apply to the attribute values
     * @return a map from node children of the parent to new bounds of the nodes
     */
    public Map<INode, Rect> measureChildren(INode parent,
            final IClientRulesEngine.AttributeFilter filter) {
        finishConfiguration();
        HardwareConfig hardwareConfig = mHardwareConfigHelper.getConfig();

        final NodeFactory mNodeFactory = mEditor.getCanvasControl().getNodeFactory();
        UiElementNode parentNode = ((NodeProxy) parent).getNode();
        UiElementPullParser topParser = new UiElementPullParser(parentNode,
                false, Collections.<UiElementNode>emptySet(), hardwareConfig.getDensity(),
                mProject) {
            @Override
            public String getAttributeValue(String namespace, String localName) {
                if (filter != null) {
                    Object cookie = getViewCookie();
                    if (cookie instanceof UiViewElementNode) {
                        NodeProxy node = mNodeFactory.create((UiViewElementNode) cookie);
                        if (node != null) {
                            String value = filter.getAttribute(node, namespace, localName);
                            if (value != null) {
                                return value;
                            }
                            // null means no preference, not "unset".
                        }
                    }
                }

                return super.getAttributeValue(namespace, localName);
            }

            /**
             * The parser usually assumes that the top level node is a document node that
             * should be skipped, and that's not the case when we render in the middle of
             * the tree, so override {@link UiElementPullParser#onNextFromStartDocument}
             * to change this behavior
             */
            @Override
            public void onNextFromStartDocument() {
                mParsingState = START_TAG;
            }
        };

        SessionParams params = new SessionParams(
                topParser,
                RenderingMode.FULL_EXPAND,
                mProject /* projectKey */,
                hardwareConfig,
                mResourceResolver,
                mProjectCallback,
                mMinSdkVersion,
                mTargetSdkVersion,
                mLogger);
        params.setLayoutOnly();
        params.setForceNoDecor();

        RenderSession session = null;
        mProjectCallback.setLogger(mLogger);
        mProjectCallback.setResourceResolver(mResourceResolver);
        RenderSecurityManager securityManager = createSecurityManager();
        try {
            securityManager.setActive(true, mCredential);
            synchronized (RENDERING_LOCK) {
                session = mLayoutLib.createSession(params);
            }
            if (session.getResult().isSuccess()) {
                assert session.getRootViews().size() == 1;
                ViewInfo root = session.getRootViews().get(0);
                List<ViewInfo> children = root.getChildren();
                Map<INode, Rect> map = new HashMap<INode, Rect>(children.size());
                for (ViewInfo info : children) {
                    if (info.getCookie() instanceof UiViewElementNode) {
                        UiViewElementNode uiNode = (UiViewElementNode) info.getCookie();
                        NodeProxy node = mNodeFactory.create(uiNode);
                        map.put(node, new Rect(info.getLeft(), info.getTop(),
                                info.getRight() - info.getLeft(),
                                info.getBottom() - info.getTop()));
                    }
                }

                return map;
            }
        } catch (RuntimeException t) {
            // Exceptions from the bridge
            mLogger.error(null, t.getLocalizedMessage(), t, null);
            throw t;
        } finally {
            securityManager.dispose(mCredential);
            mProjectCallback.setLogger(null);
            mProjectCallback.setResourceResolver(null);
            if (session != null) {
                session.dispose();
            }
        }

        return null;
    }
}

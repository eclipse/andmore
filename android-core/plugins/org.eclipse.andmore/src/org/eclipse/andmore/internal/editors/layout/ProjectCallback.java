/*
 * Copyright (C) 2008 The Android Open Source Project
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

package org.eclipse.andmore.internal.editors.layout;

import static com.android.SdkConstants.ANDROID_PKG_PREFIX;
import static com.android.SdkConstants.CALENDAR_VIEW;
import static com.android.SdkConstants.CLASS_VIEW;
import static com.android.SdkConstants.EXPANDABLE_LIST_VIEW;
import static com.android.SdkConstants.FQCN_GRID_VIEW;
import static com.android.SdkConstants.FQCN_SPINNER;
import static com.android.SdkConstants.GRID_VIEW;
import static com.android.SdkConstants.LIST_VIEW;
import static com.android.SdkConstants.SPINNER;
import static com.android.SdkConstants.VIEW_FRAGMENT;
import static com.android.SdkConstants.VIEW_INCLUDE;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.andmore.AndmoreAndroidConstants;
import org.eclipse.andmore.AndmoreAndroidPlugin;
import org.eclipse.andmore.internal.editors.layout.gle2.GraphicalEditorPart;
import org.eclipse.andmore.internal.editors.layout.gle2.LayoutMetadata;
import org.eclipse.andmore.internal.editors.layout.gle2.RenderLogger;
import org.eclipse.andmore.internal.editors.layout.uimodel.UiViewElementNode;
import org.eclipse.andmore.internal.project.AndroidManifestHelper;
import org.eclipse.andmore.internal.resources.manager.ProjectClassLoader;
import org.eclipse.andmore.internal.resources.manager.ProjectResources;
import org.eclipse.core.resources.IProject;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.rendering.LayoutLibrary;
import com.android.ide.common.rendering.api.ActionBarCallback;
import com.android.ide.common.rendering.api.AdapterBinding;
import com.android.ide.common.rendering.api.DataBindingItem;
import com.android.ide.common.rendering.api.Features;
import com.android.ide.common.rendering.api.ILayoutPullParser;
import com.android.ide.common.rendering.api.LayoutLog;
import com.android.ide.common.rendering.api.LayoutlibCallback;
import com.android.ide.common.rendering.api.ParserFactory;
import com.android.ide.common.rendering.api.ResourceReference;
import com.android.ide.common.rendering.api.ResourceValue;
import com.android.ide.common.rendering.api.Result;
import com.android.ide.common.resources.ResourceResolver;
import com.android.ide.common.xml.ManifestData;
import com.android.resources.ResourceType;
import com.android.util.Pair;
import com.google.common.base.Charsets;
import com.google.common.io.Files;

/**
 * Loader for Android Project class in order to use them in the layout editor.
 * <p/>This implements {@link IProjectCallback} for the old and new API through
 * {@link LegacyCallback}
 */
public final class ProjectCallback extends LayoutlibCallback {
    private final HashMap<String, Class<?>> mLoadedClasses = new HashMap<String, Class<?>>();
    private final Set<String> mMissingClasses = new TreeSet<String>();
    private final Set<String> mBrokenClasses = new TreeSet<String>();
    private final IProject mProject;
    private final ClassLoader mParentClassLoader;
    private final ProjectResources mProjectRes;
    private final Object mCredential;
    private boolean mUsed = false;
    private String mNamespace;
    private ProjectClassLoader mLoader = null;
    private LayoutLog mLogger;
    private LayoutLibrary mLayoutLib;
    private String mLayoutName;
    private ILayoutPullParser mLayoutEmbeddedParser;
    private ResourceResolver mResourceResolver;
    private GraphicalEditorPart mEditor;
    
    private ParserFactory parserFactory;

    /**
     * Creates a new {@link ProjectCallback} to be used with the layout lib.
     *
     * @param layoutLib The layout library this callback is going to be invoked from
     * @param projectRes the {@link ProjectResources} for the project.
     * @param project the project.
     * @param credential the sandbox credential
     */
    public ProjectCallback(LayoutLibrary layoutLib,
            ProjectResources projectRes, IProject project, Object credential,
            GraphicalEditorPart editor) {
        mLayoutLib = layoutLib;
        mParentClassLoader = layoutLib.getClassLoader();
        mProjectRes = projectRes;
        mProject = project;
        mCredential = credential;
        mEditor = editor;
		parserFactory = new ParserFactory() {
			@Override
			@NonNull
			public XmlPullParser createParser(@Nullable String debugName) throws XmlPullParserException {
				return new KXmlParser();
			}
		};
    }

    public Set<String> getMissingClasses() {
        return mMissingClasses;
    }

    public Set<String> getUninstantiatableClasses() {
        return mBrokenClasses;
    }

    /**
     * Sets the {@link LayoutLog} logger to use for error messages during problems
     *
     * @param logger the new logger to use, or null to clear it out
     */
    public void setLogger(LayoutLog logger) {
        mLogger = logger;
    }

    /**
     * Returns the {@link LayoutLog} logger used for error messages, or null
     *
     * @return the logger being used, or null if no logger is in use
     */
    public LayoutLog getLogger() {
        return mLogger;
    }

    /**
     * {@inheritDoc}
     *
     * This implementation goes through the output directory of the Eclipse project and loads the
     * <code>.class</code> file directly.
     */
    @Override
    @SuppressWarnings("unchecked")
    public Object loadView(String className, Class[] constructorSignature,
            Object[] constructorParameters)
            throws ClassNotFoundException, Exception {
        mUsed = true;

        if (className == null) {
            // Just make a plain <View> if you specify <view> without a class= attribute.
            className = CLASS_VIEW;
        }

        // look for a cached version
        Class<?> clazz = mLoadedClasses.get(className);
        if (clazz != null) {
            return instantiateClass(clazz, constructorSignature, constructorParameters);
        }

        // load the class.

        try {
            if (mLoader == null) {
                // Allow creating class loaders during rendering; may be prevented by the
                // RenderSecurityManager
                boolean token = RenderSecurityManager.enterSafeRegion(mCredential);
                try {
                  mLoader = new ProjectClassLoader(mParentClassLoader, mProject);
                } finally {
                    RenderSecurityManager.exitSafeRegion(token);
                }
            }
            clazz = mLoader.loadClass(className);
        } catch (Exception e) {
            // Add the missing class to the list so that the renderer can print them later.
            // no need to log this.
            if (!className.equals(VIEW_FRAGMENT) && !className.equals(VIEW_INCLUDE)) {
                mMissingClasses.add(className);
            }
        }

        try {
            if (clazz != null) {
                // first try to instantiate it because adding it the list of loaded class so that
                // we don't add broken classes.
                Object view = instantiateClass(clazz, constructorSignature, constructorParameters);
                mLoadedClasses.put(className, clazz);

                return view;
            }
        } catch (Throwable e) {
            // Find root cause to log it.
            while (e.getCause() != null) {
                e = e.getCause();
            }

            appendToIdeLog(e, "%1$s failed to instantiate.", className); //$NON-NLS-1$

            // Add the missing class to the list so that the renderer can print them later.
            if (mLogger instanceof RenderLogger) {
                RenderLogger renderLogger = (RenderLogger) mLogger;
                renderLogger.recordThrowable(e);

            }
            mBrokenClasses.add(className);
        }

        // Create a mock view instead. We don't cache it in the mLoadedClasses map.
        // If any exception is thrown, we'll return a CFN with the original class name instead.
        try {
            clazz = mLoader.loadClass(SdkConstants.CLASS_MOCK_VIEW);
            Object view = instantiateClass(clazz, constructorSignature, constructorParameters);

            // Set the text of the mock view to the simplified name of the custom class
            Method m = view.getClass().getMethod("setText",
                                                 new Class<?>[] { CharSequence.class });
            String label = getShortClassName(className);
            if (label.equals(VIEW_FRAGMENT)) {
                label = "<fragment>\n"
                        + "Pick preview layout from the \"Fragment Layout\" context menu";
            } else if (label.equals(VIEW_INCLUDE)) {
                label = "Text";
            }

            m.invoke(view, label);

            // Call MockView.setGravity(Gravity.CENTER) to get the text centered in
            // MockViews.
            // TODO: Do this in layoutlib's MockView class instead.
            try {
                // Look up android.view.Gravity#CENTER - or can we just hard-code
                // the value (17) here?
                Class<?> gravity =
                    Class.forName("android.view.Gravity", //$NON-NLS-1$
                            true, view.getClass().getClassLoader());
                Field centerField = gravity.getField("CENTER"); //$NON-NLS-1$
                int center = centerField.getInt(null);
                m = view.getClass().getMethod("setGravity",
                        new Class<?>[] { Integer.TYPE });
                // Center
                //int center = (0x0001 << 4) | (0x0001 << 0);
                m.invoke(view, Integer.valueOf(center));
            } catch (Exception e) {
                // Not important to center views
            }

            return view;
        } catch (Exception e) {
            // We failed to create and return a mock view.
            // Just throw back a CNF with the original class name.
            throw new ClassNotFoundException(className, e);
        }
    }

    private String getShortClassName(String fqcn) {
        // The name is typically a fully-qualified class name. Let's make it a tad shorter.

        if (fqcn.startsWith("android.")) {                                      //$NON-NLS-1$
            // For android classes, convert android.foo.Name to android...Name
            int first = fqcn.indexOf('.');
            int last = fqcn.lastIndexOf('.');
            if (last > first) {
                return fqcn.substring(0, first) + ".." + fqcn.substring(last);   //$NON-NLS-1$
            }
        } else {
            // For custom non-android classes, it's best to keep the 2 first segments of
            // the namespace, e.g. we want to get something like com.example...MyClass
            int first = fqcn.indexOf('.');
            first = fqcn.indexOf('.', first + 1);
            int last = fqcn.lastIndexOf('.');
            if (last > first) {
                return fqcn.substring(0, first) + ".." + fqcn.substring(last);   //$NON-NLS-1$
            }
        }

        return fqcn;
    }

    /**
     * Returns the namespace for the project. The namespace contains a standard part + the
     * application package.
     *
     * @return The package namespace of the project or null in case of error.
     */
    @Override
    public String getNamespace() {
        if (mNamespace == null) {
            boolean token = RenderSecurityManager.enterSafeRegion(mCredential);
            try {
                ManifestData manifestData = AndroidManifestHelper.parseForData(mProject);
                if (manifestData != null) {
                    String javaPackage = manifestData.getPackage();
                    mNamespace = String.format(AndmoreAndroidConstants.NS_CUSTOM_RESOURCES, javaPackage);
                }
            } finally {
                RenderSecurityManager.exitSafeRegion(token);
            }
        }

        return mNamespace;
    }

    @Override
    public Pair<ResourceType, String> resolveResourceId(int id) {
        if (mProjectRes != null) {
            return mProjectRes.resolveResourceId(id);
        }

        return null;
    }

    @Override
    public String resolveResourceId(int[] id) {
        if (mProjectRes != null) {
            return mProjectRes.resolveStyleable(id);
        }

        return null;
    }

    @Override
    public Integer getResourceId(ResourceType type, String name) {
        if (mProjectRes != null) {
            return mProjectRes.getResourceId(type, name);
        }

        return null;
    }

    /**
     * Returns whether the loader has received requests to load custom views. Note that
     * the custom view loading may not actually have succeeded; this flag only records
     * whether it was <b>requested</b>.
     * <p/>
     * This allows to efficiently only recreate when needed upon code change in the
     * project.
     *
     * @return true if the loader has been asked to load custom views
     */
    public boolean isUsed() {
        return mUsed;
    }

    /**
     * Instantiate a class object, using a specific constructor and parameters.
     * @param clazz the class to instantiate
     * @param constructorSignature the signature of the constructor to use
     * @param constructorParameters the parameters to use in the constructor.
     * @return A new class object, created using a specific constructor and parameters.
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    private Object instantiateClass(Class<?> clazz,
            Class[] constructorSignature,
            Object[] constructorParameters) throws Exception {
        Constructor<?> constructor = null;

        try {
            constructor = clazz.getConstructor(constructorSignature);

        } catch (NoSuchMethodException e) {
            // Custom views can either implement a 3-parameter, 2-parameter or a
            // 1-parameter. Let's synthetically build and try all the alternatives.
            // That's kind of like switching to the other box.
            //
            // The 3-parameter constructor takes the following arguments:
            // ...(Context context, AttributeSet attrs, int defStyle)

            int n = constructorSignature.length;
            if (n == 0) {
                // There is no parameter-less constructor. Nobody should ask for one.
                throw e;
            }

            for (int i = 3; i >= 1; i--) {
                if (i == n) {
                    // Let's skip the one we know already fails
                    continue;
                }
                Class[] sig = new Class[i];
                Object[] params = new Object[i];

                int k = i;
                if (n < k) {
                    k = n;
                }
                System.arraycopy(constructorSignature, 0, sig, 0, k);
                System.arraycopy(constructorParameters, 0, params, 0, k);

                for (k++; k <= i; k++) {
                    if (k == 2) {
                        // Parameter 2 is the AttributeSet
                        sig[k-1] = clazz.getClassLoader().loadClass("android.util.AttributeSet");
                        params[k-1] = null;

                    } else if (k == 3) {
                        // Parameter 3 is the int defstyle
                        sig[k-1] = int.class;
                        params[k-1] = 0;
                    }
                }

                constructorSignature = sig;
                constructorParameters = params;

                try {
                    // Try again...
                    constructor = clazz.getConstructor(constructorSignature);
                    if (constructor != null) {
                        // Found a suitable constructor, now let's use it.
                        // (But let's warn the user if the simple View constructor was found
                        // since Unexpected Things may happen if the attribute set constructors
                        // are not found)
                        if (constructorSignature.length < 2 && mLogger != null) {
                            mLogger.warning("wrongconstructor", //$NON-NLS-1$
                                String.format("Custom view %1$s is not using the 2- or 3-argument "
                                    + "View constructors; XML attributes will not work",
                                    clazz.getSimpleName()), null /*data*/);
                        }
                        break;
                    }
                } catch (NoSuchMethodException e1) {
                    // pass
                }
            }

            // If all the alternatives failed, throw the initial exception.
            if (constructor == null) {
                throw e;
            }
        }

        constructor.setAccessible(true);
        return constructor.newInstance(constructorParameters);
    }

    public void setLayoutParser(String layoutName, ILayoutPullParser layoutParser) {
        mLayoutName = layoutName;
        mLayoutEmbeddedParser = layoutParser;
    }

    @Override
    public ILayoutPullParser getParser(String layoutName) {
        boolean token = RenderSecurityManager.enterSafeRegion(mCredential);
        try {
            // Try to compute the ResourceValue for this layout since layoutlib
            // must be an older version which doesn't pass the value:
            if (mResourceResolver != null) {
                ResourceValue value = mResourceResolver.getProjectResource(ResourceType.LAYOUT,
                        layoutName);
                if (value != null) {
                    return getParser(value);
                }
            }

            return getParser(layoutName, null);
        } finally {
            RenderSecurityManager.exitSafeRegion(token);
        }
    }

    @Override
    public ILayoutPullParser getParser(ResourceValue layoutResource) {
        boolean token = RenderSecurityManager.enterSafeRegion(mCredential);
        try {
            return getParser(layoutResource.getName(),
                    new File(layoutResource.getValue()));
        } finally {
            RenderSecurityManager.exitSafeRegion(token);
        }
    }

    private ILayoutPullParser getParser(String layoutName, File xml) {
        if (layoutName.equals(mLayoutName)) {
            ILayoutPullParser parser = mLayoutEmbeddedParser;
            // The parser should only be used once!! If it is included more than once,
            // subsequent includes should just use a plain pull parser that is not tied
            // to the XML model
            mLayoutEmbeddedParser = null;
            return parser;
        }

        // For included layouts, create a ContextPullParser such that we get the
        // layout editor behavior in included layouts as well - which for example
        // replaces <fragment> tags with <include>.
        if (xml != null && xml.isFile()) {
            ContextPullParser parser = new ContextPullParser(this, xml);
            try {
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
                String xmlText = Files.toString(xml, Charsets.UTF_8);
                parser.setInput(new StringReader(xmlText));
                return parser;
            } catch (XmlPullParserException e) {
                appendToIdeLog(e, null);
            } catch (FileNotFoundException e) {
                // Shouldn't happen since we check isFile() above
            } catch (IOException e) {
                appendToIdeLog(e, null);
            }
        }

        return null;
    }

    @Override
    public Object getAdapterItemValue(ResourceReference adapterView, Object adapterCookie,
            ResourceReference itemRef,
            int fullPosition, int typePosition, int fullChildPosition, int typeChildPosition,
            ResourceReference viewRef, ViewAttribute viewAttribute, Object defaultValue) {

        // Special case for the palette preview
        if (viewAttribute == ViewAttribute.TEXT
                && adapterView.getName().startsWith("android_widget_")) { //$NON-NLS-1$
            String name = adapterView.getName();
            if (viewRef.getName().equals("text2")) { //$NON-NLS-1$
                return "Sub Item";
            }
            if (fullPosition == 0) {
                String viewName = name.substring("android_widget_".length());
                if (viewName.equals(EXPANDABLE_LIST_VIEW)) {
                    return "ExpandableList"; // ExpandableListView is too wide, character-wraps
                }
                return viewName;
            } else {
                return "Next Item";
            }
        }

        if (itemRef.isFramework()) {
            // Special case for list_view_item_2 and friends
            if (viewRef.getName().equals("text2")) { //$NON-NLS-1$
                return "Sub Item " + (fullPosition + 1);
            }
        }

        if (viewAttribute == ViewAttribute.TEXT && ((String) defaultValue).length() == 0) {
            return "Item " + (fullPosition + 1);
        }

        return null;
    }

    /**
     * For the given class, finds and returns the nearest super class which is a ListView
     * or an ExpandableListView or a GridView (which uses a list adapter), or returns null.
     *
     * @param clz the class of the view object
     * @return the fully qualified class name of the list ancestor, or null if there
     *         is no list view ancestor
     */
    public static String getListAdapterViewFqcn(Class<?> clz) {
        String fqcn = clz.getName();
        if (fqcn.endsWith(LIST_VIEW)) { // including EXPANDABLE_LIST_VIEW
            return fqcn;
        } else if (fqcn.equals(FQCN_GRID_VIEW)) {
            return fqcn;
        } else if (fqcn.equals(FQCN_SPINNER)) {
            return fqcn;
        } else if (fqcn.startsWith(ANDROID_PKG_PREFIX)) {
            return null;
        }
        Class<?> superClass = clz.getSuperclass();
        if (superClass != null) {
            return getListAdapterViewFqcn(superClass);
        } else {
            // Should not happen; we would have encountered android.view.View first,
            // and it should have been covered by the ANDROID_PKG_PREFIX case above.
            return null;
        }
    }

    /**
     * Looks at the parent-chain of the view and if it finds a custom view, or a
     * CalendarView, within the given distance then it returns true. A ListView within a
     * CalendarView should not be assigned a custom list view type because it sets its own
     * and then attempts to cast the layout to its own type which would fail if the normal
     * default list item binding is used.
     */
    private boolean isWithinIllegalParent(Object viewObject, int depth) {
        String fqcn = viewObject.getClass().getName();
        if (fqcn.endsWith(CALENDAR_VIEW) || !fqcn.startsWith(ANDROID_PKG_PREFIX)) {
            return true;
        }

        if (depth > 0) {
            Result result = mLayoutLib.getViewParent(viewObject);
            if (result.isSuccess()) {
                Object parent = result.getData();
                if (parent != null) {
                    return isWithinIllegalParent(parent, depth -1);
                }
            }
        }

        return false;
    }

    @Override
    public AdapterBinding getAdapterBinding(final ResourceReference adapterView,
            final Object adapterCookie, final Object viewObject) {
        // Look for user-recorded preference for layout to be used for previews
        if (adapterCookie instanceof UiViewElementNode) {
            UiViewElementNode uiNode = (UiViewElementNode) adapterCookie;
            AdapterBinding binding = LayoutMetadata.getNodeBinding(viewObject, uiNode);
            if (binding != null) {
                return binding;
            }
        } else if (adapterCookie instanceof Map<?,?>) {
            @SuppressWarnings("unchecked")
            Map<String, String> map = (Map<String, String>) adapterCookie;
            AdapterBinding binding = LayoutMetadata.getNodeBinding(viewObject, map);
            if (binding != null) {
                return binding;
            }
        }

        if (viewObject == null) {
            return null;
        }

        // Is this a ListView or ExpandableListView? If so, return its fully qualified
        // class name, otherwise return null. This is used to filter out other types
        // of AdapterViews (such as Spinners) where we don't want to use the list item
        // binding.
        String listFqcn = getListAdapterViewFqcn(viewObject.getClass());
        if (listFqcn == null) {
            return null;
        }

        // Is this ListView nested within an "illegal" container, such as a CalendarView?
        // If so, don't change the bindings below. Some views, such as CalendarView, and
        // potentially some custom views, might be doing specific things with the ListView
        // that could break if we add our own list binding, so for these leave the list
        // alone.
        if (isWithinIllegalParent(viewObject, 2)) {
            return null;
        }

        int count = listFqcn.endsWith(GRID_VIEW) ? 24 : 12;
        AdapterBinding binding = new AdapterBinding(count);
        if (listFqcn.endsWith(EXPANDABLE_LIST_VIEW)) {
            binding.addItem(new DataBindingItem(LayoutMetadata.DEFAULT_EXPANDABLE_LIST_ITEM,
                    true /* isFramework */, 1));
        } else if (listFqcn.equals(SPINNER)) {
            binding.addItem(new DataBindingItem(LayoutMetadata.DEFAULT_SPINNER_ITEM,
                    true /* isFramework */, 1));
        } else {
            binding.addItem(new DataBindingItem(LayoutMetadata.DEFAULT_LIST_ITEM,
                    true /* isFramework */, 1));
        }

        return binding;
    }

    /**
     * Sets the {@link ResourceResolver} to be used when looking up resources
     *
     * @param resolver the resolver to use
     */
    public void setResourceResolver(ResourceResolver resolver) {
        mResourceResolver = resolver;
    }

    // Append the given message to the ADT log. Bypass the sandbox if necessary
    // such that we can write to the log file.
    private void appendToIdeLog(Throwable exception, String format, Object ... args) {
        boolean token = RenderSecurityManager.enterSafeRegion(mCredential);
        try {
            AndmoreAndroidPlugin.log(exception, format, args);
        } finally {
            RenderSecurityManager.exitSafeRegion(token);
        }
    }

    @Override
    public ActionBarCallback getActionBarCallback() {
        return new ActionBarHandler(mEditor);
    }
    
    @Override
    public ParserFactory getParserFactory() {
    	return parserFactory;
    }

    @Override
    public boolean supports(int ideFeature) {
        switch(ideFeature) {
            case Features.ACTION_BAR:
            case Features.ADAPTER_BINDING:
            case Features.ANIMATED_VIEW_MANIPULATION:
            // Duplicate ID - case Features.FULL_ANIMATED_VIEW_MANIPULATION:
            case Features.CUSTOM_BACKGROUND_COLOR:
            case Features.EMBEDDED_LAYOUT:
            case Features.EXTENDED_VIEWINFO:
            case Features.FIXED_SCALABLE_NINE_PATCH:
            case Features.LAYOUT_ONLY:
            case Features.PLAY_ANIMATION:
            case Features.PREFERENCES_RENDERING:
            case Features.RECYCLER_VIEW_ADAPTER:
            case Features.RENDER:
            case Features.RENDER_ALL_DRAWABLE_STATES:
            case Features.RTL:
            case Features.SIMULATE_PLATFORM:
            case Features.UNBOUND_RENDERING:
            case Features.VIEW_MANIPULATION:
            default:
                return false;
        }
    }
}

/*
 * Copyright (C) 2007 The Android Open Source Project
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

package org.eclipse.andmore.internal.sdk;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.rendering.LayoutLibrary;
import com.android.ide.common.rendering.api.LayoutLog;
import com.android.ide.common.resources.ResourceRepository;
import com.android.ide.common.sdk.LoadStatus;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.IAndroidTarget.IOptionalLibrary;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.common.resources.platform.AttributeInfo;
import org.eclipse.andmore.internal.editors.animator.AnimDescriptors;
import org.eclipse.andmore.internal.editors.animator.AnimatorDescriptors;
import org.eclipse.andmore.internal.editors.color.ColorDescriptors;
import org.eclipse.andmore.internal.editors.descriptors.IDescriptorProvider;
import org.eclipse.andmore.internal.editors.drawable.DrawableDescriptors;
import org.eclipse.andmore.internal.editors.layout.descriptors.LayoutDescriptors;
import org.eclipse.andmore.internal.editors.manifest.descriptors.AndroidManifestDescriptors;
import org.eclipse.andmore.internal.editors.menu.descriptors.MenuDescriptors;
import org.eclipse.andmore.internal.editors.otherxml.descriptors.OtherXmlDescriptors;
import org.eclipse.andmore.internal.editors.values.descriptors.ValuesDescriptors;
import org.eclipse.andmore.internal.resources.manager.ProjectResources;
import org.eclipse.core.runtime.IStatus;

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

/**
 * This class contains the data of an Android Target as loaded from the SDK.
 */
public class AndroidTargetData {

    public final static int DESCRIPTOR_MANIFEST = 1;
    public final static int DESCRIPTOR_LAYOUT = 2;
    public final static int DESCRIPTOR_MENU = 3;
    public final static int DESCRIPTOR_OTHER_XML = 4;
    public final static int DESCRIPTOR_RESOURCES = 5;
    public final static int DESCRIPTOR_SEARCHABLE = 6;
    public final static int DESCRIPTOR_PREFERENCES = 7;
    public final static int DESCRIPTOR_APPWIDGET_PROVIDER = 8;
    public final static int DESCRIPTOR_DRAWABLE = 9;
    public final static int DESCRIPTOR_ANIMATOR = 10;
    public final static int DESCRIPTOR_ANIM = 11;
    public final static int DESCRIPTOR_COLOR = 12;

    private final IAndroidTarget mTarget;

    /**
     * mAttributeValues is a map { key => list [ values ] }.
     * The key for the map is "(element-xml-name,attribute-namespace:attribute-xml-local-name)".
     * The attribute namespace prefix must be:
     * - "android" for SdkConstants.NS_RESOURCES
     * - "xmlns" for the XMLNS URI.
     *
     * This is used for attributes that do not have a unique name, but still need to be populated
     * with values in the UI. Uniquely named attributes have their values in {@link #mEnumValueMap}.
     */
    private Hashtable<String, String[]> mAttributeValues = new Hashtable<String, String[]>();

    private AndroidManifestDescriptors mManifestDescriptors;
    private DrawableDescriptors mDrawableDescriptors;
    private AnimatorDescriptors mAnimatorDescriptors;
    private AnimDescriptors mAnimDescriptors;
    private ColorDescriptors mColorDescriptors;
    private LayoutDescriptors mLayoutDescriptors;
    private MenuDescriptors mMenuDescriptors;
    private OtherXmlDescriptors mOtherXmlDescriptors;

    private Map<String, Map<String, Integer>> mEnumValueMap;

    private ResourceRepository mFrameworkResources;
    private LayoutLibrary mLayoutLibrary;
    private Map<String, AttributeInfo> mAttributeMap;

    private boolean mLayoutBridgeInit = false;

    AndroidTargetData(IAndroidTarget androidTarget) {
        mTarget = androidTarget;
    }

    /**
     * Sets the associated map from string attribute name to
     * {@link AttributeInfo}
     *
     * @param attributeMap the map
     */
    public void setAttributeMap(@NonNull Map<String, AttributeInfo> attributeMap) {
        mAttributeMap = attributeMap;
    }

    /**
     * Returns the associated map from string attribute name to
     * {@link AttributeInfo}
     *
     * @return the map
     */
    @Nullable
    public Map<String, AttributeInfo> getAttributeMap() {
        return mAttributeMap;
    }

    /**
     * Creates an AndroidTargetData object.
     */
    void setExtraData(
            AndroidManifestDescriptors manifestDescriptors,
            LayoutDescriptors layoutDescriptors,
            MenuDescriptors menuDescriptors,
            OtherXmlDescriptors otherXmlDescriptors,
            DrawableDescriptors drawableDescriptors,
            AnimatorDescriptors animatorDescriptors,
            AnimDescriptors animDescriptors,
            ColorDescriptors colorDescriptors,
            Map<String, Map<String, Integer>> enumValueMap,
            String[] permissionValues,
            String[] activityIntentActionValues,
            String[] broadcastIntentActionValues,
            String[] serviceIntentActionValues,
            String[] intentCategoryValues,
            String[] platformLibraries,
            IOptionalLibrary[] optionalLibraries,
            ResourceRepository frameworkResources,
            LayoutLibrary layoutLibrary) {

        mManifestDescriptors = manifestDescriptors;
        mDrawableDescriptors = drawableDescriptors;
        mAnimatorDescriptors = animatorDescriptors;
        mAnimDescriptors = animDescriptors;
        mColorDescriptors = colorDescriptors;
        mLayoutDescriptors = layoutDescriptors;
        mMenuDescriptors = menuDescriptors;
        mOtherXmlDescriptors = otherXmlDescriptors;
        mEnumValueMap = enumValueMap;
        mFrameworkResources = frameworkResources;
        mLayoutLibrary = layoutLibrary;

        setPermissions(permissionValues);
        setIntentFilterActionsAndCategories(activityIntentActionValues, broadcastIntentActionValues,
                serviceIntentActionValues, intentCategoryValues);
        setOptionalLibraries(platformLibraries, optionalLibraries);
    }

    /**
     * Returns an {@link IDescriptorProvider} from a given Id.
     * The Id can be one of {@link #DESCRIPTOR_MANIFEST}, {@link #DESCRIPTOR_LAYOUT},
     * {@link #DESCRIPTOR_MENU}, or {@link #DESCRIPTOR_OTHER_XML}.
     * All other values will throw an {@link IllegalArgumentException}.
     */
    public IDescriptorProvider getDescriptorProvider(int descriptorId) {
        switch (descriptorId) {
            case DESCRIPTOR_MANIFEST:
                return mManifestDescriptors;
            case DESCRIPTOR_LAYOUT:
                return mLayoutDescriptors;
            case DESCRIPTOR_MENU:
                return mMenuDescriptors;
            case DESCRIPTOR_OTHER_XML:
                return mOtherXmlDescriptors;
            case DESCRIPTOR_RESOURCES:
                // FIXME: since it's hard-coded the Resources Descriptors are not platform dependent.
                return ValuesDescriptors.getInstance();
            case DESCRIPTOR_PREFERENCES:
                return mOtherXmlDescriptors.getPreferencesProvider();
            case DESCRIPTOR_APPWIDGET_PROVIDER:
                return mOtherXmlDescriptors.getAppWidgetProvider();
            case DESCRIPTOR_SEARCHABLE:
                return mOtherXmlDescriptors.getSearchableProvider();
            case DESCRIPTOR_DRAWABLE:
                return mDrawableDescriptors;
            case DESCRIPTOR_ANIMATOR:
                return mAnimatorDescriptors;
            case DESCRIPTOR_ANIM:
                return mAnimDescriptors;
            case DESCRIPTOR_COLOR:
                return mColorDescriptors;
            default :
                 throw new IllegalArgumentException();
        }
    }

    /**
     * Returns the manifest descriptors.
     */
    public AndroidManifestDescriptors getManifestDescriptors() {
        return mManifestDescriptors;
    }

    /**
     * Returns the drawable descriptors
     */
    public DrawableDescriptors getDrawableDescriptors() {
        return mDrawableDescriptors;
    }

    /**
     * Returns the animation descriptors
     */
    public AnimDescriptors getAnimDescriptors() {
        return mAnimDescriptors;
    }

    /**
     * Returns the color descriptors
     */
    public ColorDescriptors getColorDescriptors() {
        return mColorDescriptors;
    }

    /**
     * Returns the animator descriptors
     */
    public AnimatorDescriptors getAnimatorDescriptors() {
        return mAnimatorDescriptors;
    }

    /**
     * Returns the layout Descriptors.
     */
    public LayoutDescriptors getLayoutDescriptors() {
        return mLayoutDescriptors;
    }

    /**
     * Returns the menu descriptors.
     */
    public MenuDescriptors getMenuDescriptors() {
        return mMenuDescriptors;
    }

    /**
     * Returns the XML descriptors
     */
    public OtherXmlDescriptors getXmlDescriptors() {
        return mOtherXmlDescriptors;
    }

    /**
     * Returns this list of possible values for an XML attribute.
     * <p/>This should only be called for attributes for which possible values depend on the
     * parent element node.
     * <p/>For attributes that have the same values no matter the parent node, use
     * {@link #getEnumValueMap()}.
     * @param elementName the name of the element containing the attribute.
     * @param attributeName the name of the attribute
     * @return an array of String with the possible values, or <code>null</code> if no values were
     * found.
     */
    public String[] getAttributeValues(String elementName, String attributeName) {
        String key = String.format("(%1$s,%2$s)", elementName, attributeName); //$NON-NLS-1$
        return mAttributeValues.get(key);
    }

    /**
     * Returns this list of possible values for an XML attribute.
     * <p/>This should only be called for attributes for which possible values depend on the
     * parent and great-grand-parent element node.
     * <p/>The typical example of this is for the 'name' attribute under
     * activity/intent-filter/action
     * <p/>For attributes that have the same values no matter the parent node, use
     * {@link #getEnumValueMap()}.
     * @param elementName the name of the element containing the attribute.
     * @param attributeName the name of the attribute
     * @param greatGrandParentElementName the great-grand-parent node.
     * @return an array of String with the possible values, or <code>null</code> if no values were
     * found.
     */
    public String[] getAttributeValues(String elementName, String attributeName,
            String greatGrandParentElementName) {
        if (greatGrandParentElementName != null) {
            String key = String.format("(%1$s,%2$s,%3$s)", //$NON-NLS-1$
                    greatGrandParentElementName, elementName, attributeName);
            String[] values = mAttributeValues.get(key);
            if (values != null) {
                return values;
            }
        }

        return getAttributeValues(elementName, attributeName);
    }

    /**
     * Returns the enum values map.
     * <p/>The map defines the possible values for XML attributes. The key is the attribute name
     * and the value is a map of (string, integer) in which the key (string) is the name of
     * the value, and the Integer is the numerical value in the compiled binary XML files.
     */
    public Map<String, Map<String, Integer>> getEnumValueMap() {
        return mEnumValueMap;
    }

    /**
     * Returns the {@link ProjectResources} containing the Framework Resources.
     */
    public ResourceRepository getFrameworkResources() {
        return mFrameworkResources;
    }

    /**
     * Returns a {@link LayoutLibrary} object possibly containing a {@link LayoutBridge} object.
     * <p/>If {@link LayoutLibrary#getBridge()} is <code>null</code>,
     * {@link LayoutBridge#getStatus()} will contain the reason (either {@link LoadStatus#LOADING}
     * or {@link LoadStatus#FAILED}).
     * <p/>Valid {@link LayoutBridge} objects are always initialized before being returned.
     */
    public synchronized LayoutLibrary getLayoutLibrary() {
        if (mLayoutBridgeInit == false && mLayoutLibrary.getStatus() == LoadStatus.LOADED) {
            boolean ok = mLayoutLibrary.init(
                    mTarget.getProperties(),
                    new File(mTarget.getPath(IAndroidTarget.FONTS)),
                    getEnumValueMap(),
                    new LayoutLog() {

                        @Override
                        public void error(String tag, String message, Throwable throwable,
                                Object data) {
                            AdtPlugin.log(throwable, message);
                        }

                        @Override
                        public void error(String tag, String message, Object data) {
                            AdtPlugin.log(IStatus.ERROR, message);
                        }

                        @Override
                        public void warning(String tag, String message, Object data) {
                            AdtPlugin.log(IStatus.WARNING, message);
                        }
                    });
            if (!ok) {
                AdtPlugin.log(IStatus.ERROR,
                        "LayoutLibrary initialization failed");
            }
            mLayoutBridgeInit = true;
        }

        return mLayoutLibrary;
    }

    /**
     * Sets the permission values
     * @param permissionValues the list of permissions
     */
    private void setPermissions(String[] permissionValues) {
        setValues("(uses-permission,android:name)", permissionValues);   //$NON-NLS-1$
        setValues("(application,android:permission)", permissionValues); //$NON-NLS-1$
        setValues("(activity,android:permission)", permissionValues);    //$NON-NLS-1$
        setValues("(receiver,android:permission)", permissionValues);    //$NON-NLS-1$
        setValues("(service,android:permission)", permissionValues);     //$NON-NLS-1$
        setValues("(provider,android:permission)", permissionValues);    //$NON-NLS-1$
    }

    private void setIntentFilterActionsAndCategories(String[] activityIntentActions,
            String[] broadcastIntentActions, String[] serviceIntentActions,
            String[] intentCategoryValues) {
        setValues("(activity,action,android:name)", activityIntentActions);  //$NON-NLS-1$
        setValues("(receiver,action,android:name)", broadcastIntentActions); //$NON-NLS-1$
        setValues("(service,action,android:name)", serviceIntentActions);    //$NON-NLS-1$
        setValues("(category,android:name)", intentCategoryValues);          //$NON-NLS-1$
    }

    private void setOptionalLibraries(String[] platformLibraries,
            IOptionalLibrary[] optionalLibraries) {

        ArrayList<String> libs = new ArrayList<String>();

        if (platformLibraries != null) {
            for (String name : platformLibraries) {
                libs.add(name);
            }
        }

        if (optionalLibraries != null) {
            for (int i = 0; i < optionalLibraries.length; i++) {
                libs.add(optionalLibraries[i].getName());
            }
        }
        setValues("(uses-library,android:name)",  libs.toArray(new String[libs.size()]));
    }

    /**
     * Sets a (name, values) pair in the hash map.
     * <p/>
     * If the name is already present in the map, it is first removed.
     * @param name the name associated with the values.
     * @param values The values to add.
     */
    private void setValues(String name, String[] values) {
        mAttributeValues.remove(name);
        mAttributeValues.put(name, values);
    }

    public void dispose() {
        if (mLayoutLibrary != null) {
            mLayoutLibrary.dispose();
        }
    }
}

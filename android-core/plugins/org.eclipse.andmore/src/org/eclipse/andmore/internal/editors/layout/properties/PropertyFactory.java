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

import static com.android.SdkConstants.ATTR_ID;
import static com.android.SdkConstants.ATTR_LAYOUT_MARGIN;
import static com.android.SdkConstants.ATTR_LAYOUT_RESOURCE_PREFIX;

import com.android.annotations.Nullable;
import com.android.ide.common.api.IAttributeInfo;
import com.android.ide.common.api.IAttributeInfo.Format;
import com.android.tools.lint.detector.api.LintUtils;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import org.eclipse.andmore.internal.editors.descriptors.AttributeDescriptor;
import org.eclipse.andmore.internal.editors.descriptors.DescriptorsUtils;
import org.eclipse.andmore.internal.editors.descriptors.SeparatorAttributeDescriptor;
import org.eclipse.andmore.internal.editors.descriptors.XmlnsAttributeDescriptor;
import org.eclipse.andmore.internal.editors.layout.descriptors.ViewElementDescriptor;
import org.eclipse.andmore.internal.editors.layout.gle2.CanvasViewInfo;
import org.eclipse.andmore.internal.editors.layout.gle2.GraphicalEditorPart;
import org.eclipse.andmore.internal.editors.layout.gre.ViewMetadataRepository;
import org.eclipse.andmore.internal.editors.layout.uimodel.UiViewElementNode;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.wb.internal.core.editor.structure.property.PropertyListIntersector;
import org.eclipse.wb.internal.core.model.property.ComplexProperty;
import org.eclipse.wb.internal.core.model.property.Property;
import org.eclipse.wb.internal.core.model.property.category.PropertyCategory;
import org.eclipse.wb.internal.core.model.property.editor.PropertyEditor;
import org.eclipse.wb.internal.core.model.property.editor.presentation.ButtonPropertyEditorPresentation;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * The {@link PropertyFactory} creates (and caches) the set of {@link Property}
 * instances applicable to a given node. It's also responsible for ordering
 * these, and sometimes combining them into {@link ComplexProperty} category
 * nodes.
 * <p>
 * TODO: For any properties that are *set* in XML, they should NOT be labeled as
 * advanced (which would make them disappear)
 */
public class PropertyFactory {
    /** Disable cache during development only */
    @SuppressWarnings("unused")
    private static final boolean CACHE_ENABLED = true || !LintUtils.assertionsEnabled();
    static {
        if (!CACHE_ENABLED) {
            System.err.println("WARNING: The property cache is disabled");
        }
    }

    private static final Property[] NO_PROPERTIES = new Property[0];

    private static final int PRIO_FIRST = -100000;
    private static final int PRIO_SECOND = PRIO_FIRST + 10;
    private static final int PRIO_LAST = 100000;

    private final GraphicalEditorPart mGraphicalEditorPart;
    private Map<UiViewElementNode, Property[]> mCache =
            new WeakHashMap<UiViewElementNode, Property[]>();
    private UiViewElementNode mCurrentViewCookie;

    /** Sorting orders for the properties */
    public enum SortingMode {
        NATURAL,
        BY_ORIGIN,
        ALPHABETICAL;
    }

    /** The default sorting mode */
    public static final SortingMode DEFAULT_MODE = SortingMode.BY_ORIGIN;

    private SortingMode mSortMode = DEFAULT_MODE;
    private SortingMode mCacheSortMode;

    public PropertyFactory(GraphicalEditorPart graphicalEditorPart) {
        mGraphicalEditorPart = graphicalEditorPart;
    }

    /**
     * Get the properties for the given list of selection items.
     *
     * @param items the {@link CanvasViewInfo} instances to get an intersected
     *            property list for
     * @return the properties for the given items
     */
    public Property[] getProperties(List<CanvasViewInfo> items) {
        mCurrentViewCookie = null;

        if (items == null || items.size() == 0) {
            return NO_PROPERTIES;
        } else if (items.size() == 1) {
            CanvasViewInfo item = items.get(0);
            mCurrentViewCookie = item.getUiViewNode();

            return getProperties(item);
        } else {
            // intersect properties
            PropertyListIntersector intersector = new PropertyListIntersector();
            for (CanvasViewInfo node : items) {
                intersector.intersect(getProperties(node));
            }

            return intersector.getProperties();
        }
    }

    private Property[] getProperties(CanvasViewInfo item) {
        UiViewElementNode node = item.getUiViewNode();
        if (node == null) {
            return NO_PROPERTIES;
        }

        if (mCacheSortMode != mSortMode) {
            mCacheSortMode = mSortMode;
            mCache.clear();
        }

        Property[] properties = mCache.get(node);
        if (!CACHE_ENABLED) {
            properties = null;
        }
        if (properties == null) {
            Collection<? extends Property> propertyList = getProperties(node);
            if (propertyList == null) {
                properties = new Property[0];
            } else {
                properties = propertyList.toArray(new Property[propertyList.size()]);
            }
            mCache.put(node, properties);
        }
        return properties;
    }


    protected Collection<? extends Property> getProperties(UiViewElementNode node) {
        ViewMetadataRepository repository = ViewMetadataRepository.get();
        ViewElementDescriptor viewDescriptor = (ViewElementDescriptor) node.getDescriptor();
        String fqcn = viewDescriptor.getFullClassName();
        Set<String> top = new HashSet<String>(repository.getTopAttributes(fqcn));
        AttributeDescriptor[] attributeDescriptors = node.getAttributeDescriptors();

        List<XmlProperty> properties = new ArrayList<XmlProperty>(attributeDescriptors.length);
        int priority = 0;
        for (final AttributeDescriptor descriptor : attributeDescriptors) {
            // TODO: Filter out non-public properties!!
            // (They shouldn't be in the descriptors at all)

            assert !(descriptor instanceof SeparatorAttributeDescriptor); // No longer inserted
            if (descriptor instanceof XmlnsAttributeDescriptor) {
                continue;
            }

            PropertyEditor editor = XmlPropertyEditor.INSTANCE;
            IAttributeInfo info = descriptor.getAttributeInfo();
            if (info != null) {
                EnumSet<Format> formats = info.getFormats();
                if (formats.contains(Format.BOOLEAN)) {
                    editor = BooleanXmlPropertyEditor.INSTANCE;
                } else if (formats.contains(Format.ENUM)) {
                    // We deliberately don't use EnumXmlPropertyEditor.INSTANCE here,
                    // since some attributes (such as layout_width) can have not just one
                    // of the enum values but custom values such as "42dp" as well. And
                    // furthermore, we don't even bother limiting this to formats.size()==1,
                    // since the editing experience with the enum property editor is
                    // more limited than the text editor plus enum completer anyway
                    // (for example, you can't type to filter the values, and clearing
                    // the value is harder.)
                }
            }

            XmlProperty property = new XmlProperty(editor, this, node, descriptor);
            // Assign ids sequentially. This ensures that the properties will mostly keep their
            // relative order (such as placing width before height), even though we will regroup
            // some (such as properties in the same category, and the layout params etc)
            priority += 10;

            PropertyCategory category = PropertyCategory.NORMAL;
            String name = descriptor.getXmlLocalName();
            if (top.contains(name) || PropertyMetadata.isPreferred(name)) {
                category = PropertyCategory.PREFERRED;
                property.setPriority(PRIO_FIRST + priority);
            } else {
                property.setPriority(priority);

                // Prefer attributes defined on the specific type of this
                // widget
                // NOTE: This doesn't work very well for TextViews
               /* IAttributeInfo attributeInfo = descriptor.getAttributeInfo();
                if (attributeInfo != null && fqcn.equals(attributeInfo.getDefinedBy())) {
                    category = PropertyCategory.PREFERRED;
                } else*/ if (PropertyMetadata.isAdvanced(name)) {
                    category = PropertyCategory.ADVANCED;
                }
            }
            if (category != null) {
                property.setCategory(category);
            }
            properties.add(property);
        }

        switch (mSortMode) {
            case BY_ORIGIN:
                return sortByOrigin(node, properties);

            case ALPHABETICAL:
                return sortAlphabetically(node, properties);

            default:
            case NATURAL:
                return sortNatural(node, properties);
        }
    }

    protected Collection<? extends Property> sortAlphabetically(
            UiViewElementNode node,
            List<XmlProperty> properties) {
        Collections.sort(properties, Property.ALPHABETICAL);
        return properties;
    }

    protected Collection<? extends Property> sortByOrigin(
            UiViewElementNode node,
            List<XmlProperty> properties) {
        List<Property> collapsed = new ArrayList<Property>(properties.size());
        List<Property> layoutProperties = Lists.newArrayListWithExpectedSize(20);
        List<Property> marginProperties = null;
        List<Property> deprecatedProperties = null;
        Map<String, ComplexProperty> categoryToProperty = new HashMap<String, ComplexProperty>();
        Multimap<String, Property> categoryToProperties = ArrayListMultimap.create();

        if (properties.isEmpty()) {
            return properties;
        }

        ViewElementDescriptor parent = (ViewElementDescriptor) properties.get(0).getDescriptor()
                .getParent();
        Map<String, Integer> categoryPriorities = Maps.newHashMap();
        int nextCategoryPriority = 100;
        while (parent != null) {
            categoryPriorities.put(parent.getFullClassName(), nextCategoryPriority += 100);
            parent = parent.getSuperClassDesc();
        }

        for (int i = 0, max = properties.size(); i < max; i++) {
            XmlProperty property = properties.get(i);

            AttributeDescriptor descriptor = property.getDescriptor();
            if (descriptor.isDeprecated()) {
                if (deprecatedProperties == null) {
                    deprecatedProperties = Lists.newArrayListWithExpectedSize(10);
                }
                deprecatedProperties.add(property);
                continue;
            }

            String firstName = descriptor.getXmlLocalName();
            if (firstName.startsWith(ATTR_LAYOUT_RESOURCE_PREFIX)) {
                if (firstName.startsWith(ATTR_LAYOUT_MARGIN)) {
                    if (marginProperties == null) {
                        marginProperties = Lists.newArrayListWithExpectedSize(5);
                    }
                    marginProperties.add(property);
                } else {
                    layoutProperties.add(property);
                }
                continue;
            }

            if (firstName.equals(ATTR_ID)) {
                // Add id to the front (though the layout parameters will be added to
                // the front of this at the end)
                property.setPriority(PRIO_FIRST);
                collapsed.add(property);
                continue;
            }

            if (property.getCategory() == PropertyCategory.PREFERRED) {
                collapsed.add(property);
                // Fall through: these are *duplicated* inside their defining categories!
                // However, create a new instance of the property, such that the propertysheet
                // doesn't see the same property instance twice (when selected, it will highlight
                // both, etc.) Also, set the category to Normal such that we don't draw attention
                // to it again. We want it to appear in both places such that somebody looking
                // within a category will always find it there, even if for this specific
                // view type it's a common attribute and replicated up at the top.
                XmlProperty oldProperty = property;
                property = new XmlProperty(oldProperty.getEditor(), this, node,
                        oldProperty.getDescriptor());
                property.setPriority(oldProperty.getPriority());
            }

            IAttributeInfo attributeInfo = descriptor.getAttributeInfo();
            if (attributeInfo != null && attributeInfo.getDefinedBy() != null) {
                String category = attributeInfo.getDefinedBy();
                ComplexProperty complex = categoryToProperty.get(category);
                if (complex == null) {
                    complex = new ComplexProperty(
                            category.substring(category.lastIndexOf('.') + 1),
                            "[]",
                            null /* properties */);
                    categoryToProperty.put(category, complex);
                    Integer categoryPriority = categoryPriorities.get(category);
                    if (categoryPriority != null) {
                        complex.setPriority(categoryPriority);
                    } else {
                        // Descriptor for an attribute whose definedBy does *not*
                        // correspond to one of the known superclasses of this widget.
                        // This sometimes happens; for example, a RatingBar will pull in
                        // an ImageView's minWidth attribute. Probably an error in the
                        // metadata, but deal with it gracefully here.
                        categoryPriorities.put(category, nextCategoryPriority += 100);
                        complex.setPriority(nextCategoryPriority);
                    }
                }
                categoryToProperties.put(category, property);
                continue;
            } else {
                collapsed.add(property);
            }
        }

        // Update the complex properties
        for (String category : categoryToProperties.keySet()) {
            Collection<Property> subProperties = categoryToProperties.get(category);
            if (subProperties.size() > 1) {
                ComplexProperty complex = categoryToProperty.get(category);
                assert complex != null : category;
                Property[] subArray = new Property[subProperties.size()];
                complex.setProperties(subProperties.toArray(subArray));
                //complex.setPriority(subArray[0].getPriority());

                collapsed.add(complex);

                boolean allAdvanced = true;
                boolean isPreferred = false;
                for (Property p : subProperties) {
                    PropertyCategory c = p.getCategory();
                    if (c != PropertyCategory.ADVANCED) {
                        allAdvanced = false;
                    }
                    if (c == PropertyCategory.PREFERRED) {
                        isPreferred = true;
                    }
                }
                if (isPreferred) {
                    complex.setCategory(PropertyCategory.PREFERRED);
                } else if (allAdvanced) {
                    complex.setCategory(PropertyCategory.ADVANCED);
                }
            } else if (subProperties.size() == 1) {
                collapsed.add(subProperties.iterator().next());
            }
        }

        if (layoutProperties.size() > 0 || marginProperties != null) {
            if (marginProperties != null) {
                XmlProperty[] m =
                        marginProperties.toArray(new XmlProperty[marginProperties.size()]);
                Property marginProperty = new ComplexProperty(
                        "Margins",
                        "[]",
                        m);
                layoutProperties.add(marginProperty);
                marginProperty.setPriority(PRIO_LAST);

                for (XmlProperty p : m) {
                    p.setParent(marginProperty);
                }
            }
            Property[] l = layoutProperties.toArray(new Property[layoutProperties.size()]);
            Arrays.sort(l, Property.PRIORITY);
            Property property = new ComplexProperty(
                    "Layout Parameters",
                    "[]",
                    l);
            for (Property p : l) {
                if (p instanceof XmlProperty) {
                    ((XmlProperty) p).setParent(property);
                }
            }
            property.setCategory(PropertyCategory.PREFERRED);
            collapsed.add(property);
            property.setPriority(PRIO_SECOND);
        }

        if (deprecatedProperties != null && deprecatedProperties.size() > 0) {
            Property property = new ComplexProperty(
                    "Deprecated",
                    "(Deprecated Properties)",
                    deprecatedProperties.toArray(new Property[deprecatedProperties.size()]));
            property.setPriority(PRIO_LAST);
            collapsed.add(property);
        }

        Collections.sort(collapsed, Property.PRIORITY);

        return collapsed;
    }

    protected Collection<? extends Property> sortNatural(
            UiViewElementNode node,
            List<XmlProperty> properties) {
        Collections.sort(properties, Property.ALPHABETICAL);
        List<Property> collapsed = new ArrayList<Property>(properties.size());
        List<Property> layoutProperties = Lists.newArrayListWithExpectedSize(20);
        List<Property> marginProperties = null;
        List<Property> deprecatedProperties = null;
        Map<String, ComplexProperty> categoryToProperty = new HashMap<String, ComplexProperty>();
        Multimap<String, Property> categoryToProperties = ArrayListMultimap.create();

        for (int i = 0, max = properties.size(); i < max; i++) {
            XmlProperty property = properties.get(i);

            AttributeDescriptor descriptor = property.getDescriptor();
            if (descriptor.isDeprecated()) {
                if (deprecatedProperties == null) {
                    deprecatedProperties = Lists.newArrayListWithExpectedSize(10);
                }
                deprecatedProperties.add(property);
                continue;
            }

            String firstName = descriptor.getXmlLocalName();
            if (firstName.startsWith(ATTR_LAYOUT_RESOURCE_PREFIX)) {
                if (firstName.startsWith(ATTR_LAYOUT_MARGIN)) {
                    if (marginProperties == null) {
                        marginProperties = Lists.newArrayListWithExpectedSize(5);
                    }
                    marginProperties.add(property);
                } else {
                    layoutProperties.add(property);
                }
                continue;
            }

            if (firstName.equals(ATTR_ID)) {
                // Add id to the front (though the layout parameters will be added to
                // the front of this at the end)
                property.setPriority(PRIO_FIRST);
                collapsed.add(property);
                continue;
            }

            String category = PropertyMetadata.getCategory(firstName);
            if (category != null) {
                ComplexProperty complex = categoryToProperty.get(category);
                if (complex == null) {
                    complex = new ComplexProperty(
                            category,
                            "[]",
                            null /* properties */);
                    categoryToProperty.put(category, complex);
                    complex.setPriority(property.getPriority());
                }
                categoryToProperties.put(category, property);
                continue;
            }

            // Index of second word in the first name, so in fooBar it's 3 (index of 'B')
            int firstNameIndex = firstName.length();
            for (int k = 0, kn = firstName.length(); k < kn; k++) {
                if (Character.isUpperCase(firstName.charAt(k))) {
                    firstNameIndex = k;
                    break;
                }
            }

            // Scout forwards and see how many properties we can combine
            int j = i + 1;
            if (property.getCategory() != PropertyCategory.PREFERRED
                    && !property.getDescriptor().isDeprecated()) {
                for (; j < max; j++) {
                    XmlProperty next = properties.get(j);
                    String nextName = next.getName();
                    if (nextName.regionMatches(0, firstName, 0, firstNameIndex)
                            // Also make sure we begin the second word at the next
                            // character; if not, we could have something like
                            // scrollBar
                            // scrollingBehavior
                            && nextName.length() > firstNameIndex
                            && Character.isUpperCase(nextName.charAt(firstNameIndex))) {

                        // Deprecated attributes, and preferred attributes, should not
                        // be pushed into normal clusters (preferred stay top-level
                        // and sort to the top, deprecated are all put in the same cluster at
                        // the end)

                        if (next.getCategory() == PropertyCategory.PREFERRED) {
                            break;
                        }
                        if (next.getDescriptor().isDeprecated()) {
                            break;
                        }

                        // This property should be combined with the previous
                        // property
                    } else {
                        break;
                    }
                }
            }
            if (j - i > 1) {
                // Combining multiple properties: all the properties from i
                // through j inclusive
                XmlProperty[] subprops = new XmlProperty[j - i];
                for (int k = i, index = 0; k < j; k++, index++) {
                    subprops[index] = properties.get(k);
                }
                Arrays.sort(subprops, Property.PRIORITY);

                // See if we can compute a LONGER base than just the first word.
                // For example, if we have "lineSpacingExtra" and "lineSpacingMultiplier"
                // we'd like the base to be "lineSpacing", not "line".
                int common = firstNameIndex;
                for (int k = firstNameIndex + 1, n = firstName.length(); k < n; k++) {
                    if (Character.isUpperCase(firstName.charAt(k))) {
                        common = k;
                        break;
                    }
                }
                if (common > firstNameIndex) {
                    for (int k = 0, n = subprops.length; k < n; k++) {
                        String nextName = subprops[k].getName();
                        if (nextName.regionMatches(0, firstName, 0, common)
                                // Also make sure we begin the second word at the next
                                // character; if not, we could have something like
                                // scrollBar
                                // scrollingBehavior
                                && nextName.length() > common
                                && Character.isUpperCase(nextName.charAt(common))) {
                            // New prefix is okay
                        } else {
                            common = firstNameIndex;
                            break;
                        }
                    }
                    firstNameIndex = common;
                }

                String base = firstName.substring(0, firstNameIndex);
                base = DescriptorsUtils.capitalize(base);
                Property complexProperty = new ComplexProperty(
                        base,
                        "[]",
                        subprops);
                complexProperty.setPriority(subprops[0].getPriority());
                //complexProperty.setCategory(PropertyCategory.PREFERRED);
                collapsed.add(complexProperty);
                boolean allAdvanced = true;
                boolean isPreferred = false;
                for (XmlProperty p : subprops) {
                    p.setParent(complexProperty);
                    PropertyCategory c = p.getCategory();
                    if (c != PropertyCategory.ADVANCED) {
                        allAdvanced = false;
                    }
                    if (c == PropertyCategory.PREFERRED) {
                        isPreferred = true;
                    }
                }
                if (isPreferred) {
                    complexProperty.setCategory(PropertyCategory.PREFERRED);
                } else if (allAdvanced) {
                    complexProperty.setCategory(PropertyCategory.PREFERRED);
                }
            } else {
                // Add the individual properties (usually 1, sometimes 2
                for (int k = i; k < j; k++) {
                    collapsed.add(properties.get(k));
                }
            }

            i = j - 1; // -1: compensate in advance for the for-loop adding 1
        }

        // Update the complex properties
        for (String category : categoryToProperties.keySet()) {
            Collection<Property> subProperties = categoryToProperties.get(category);
            if (subProperties.size() > 1) {
                ComplexProperty complex = categoryToProperty.get(category);
                assert complex != null : category;
                Property[] subArray = new Property[subProperties.size()];
                complex.setProperties(subProperties.toArray(subArray));
                complex.setPriority(subArray[0].getPriority());
                collapsed.add(complex);

                boolean allAdvanced = true;
                boolean isPreferred = false;
                for (Property p : subProperties) {
                    PropertyCategory c = p.getCategory();
                    if (c != PropertyCategory.ADVANCED) {
                        allAdvanced = false;
                    }
                    if (c == PropertyCategory.PREFERRED) {
                        isPreferred = true;
                    }
                }
                if (isPreferred) {
                    complex.setCategory(PropertyCategory.PREFERRED);
                } else if (allAdvanced) {
                    complex.setCategory(PropertyCategory.ADVANCED);
                }
            } else if (subProperties.size() == 1) {
                collapsed.add(subProperties.iterator().next());
            }
        }

        if (layoutProperties.size() > 0 || marginProperties != null) {
            if (marginProperties != null) {
                XmlProperty[] m =
                        marginProperties.toArray(new XmlProperty[marginProperties.size()]);
                Property marginProperty = new ComplexProperty(
                        "Margins",
                        "[]",
                        m);
                layoutProperties.add(marginProperty);
                marginProperty.setPriority(PRIO_LAST);

                for (XmlProperty p : m) {
                    p.setParent(marginProperty);
                }
            }
            Property[] l = layoutProperties.toArray(new Property[layoutProperties.size()]);
            Arrays.sort(l, Property.PRIORITY);
            Property property = new ComplexProperty(
                    "Layout Parameters",
                    "[]",
                    l);
            for (Property p : l) {
                if (p instanceof XmlProperty) {
                    ((XmlProperty) p).setParent(property);
                }
            }
            property.setCategory(PropertyCategory.PREFERRED);
            collapsed.add(property);
            property.setPriority(PRIO_SECOND);
        }

        if (deprecatedProperties != null && deprecatedProperties.size() > 0) {
            Property property = new ComplexProperty(
                    "Deprecated",
                    "(Deprecated Properties)",
                    deprecatedProperties.toArray(new Property[deprecatedProperties.size()]));
            property.setPriority(PRIO_LAST);
            collapsed.add(property);
        }

        Collections.sort(collapsed, Property.PRIORITY);

        return collapsed;
    }

    @Nullable
    GraphicalEditorPart getGraphicalEditor() {
        return mGraphicalEditorPart;
    }

    // HACK: This should be passed into each property instead
    public Object getCurrentViewObject() {
        return mCurrentViewCookie;
    }

    public void setSortingMode(SortingMode sortingMode) {
        mSortMode = sortingMode;
    }

    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=388574
    public static Composite addWorkaround(Composite parent) {
        if (ButtonPropertyEditorPresentation.isInWorkaround) {
            Composite top = new Composite(parent, SWT.NONE);
            top.setLayout(new GridLayout(1, false));
            Label label = new Label(top, SWT.WRAP);
            label.setText(
                    "This dialog is shown instead of an inline text editor as a\n" +
                    "workaround for an Eclipse bug specific to OSX Mountain Lion.\n" +
                    "It should be fixed in Eclipse 4.3.");
            label.setForeground(top.getDisplay().getSystemColor(SWT.COLOR_RED));
            GridData data = new GridData();
            data.grabExcessVerticalSpace = false;
            data.grabExcessHorizontalSpace = false;
            data.horizontalAlignment = GridData.FILL;
            data.verticalAlignment = GridData.BEGINNING;
            label.setLayoutData(data);

            Link link = new Link(top, SWT.NO_FOCUS);
            link.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));
            link.setText("<a>https://bugs.eclipse.org/bugs/show_bug.cgi?id=388574</a>");
            link.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent event) {
                    try {
                        IWorkbench workbench = PlatformUI.getWorkbench();
                        IWebBrowser browser = workbench.getBrowserSupport().getExternalBrowser();
                        browser.openURL(new URL(event.text));
                    } catch (Exception e) {
                        String message = String.format(
                                "Could not open browser. Vist\n%1$s\ninstead.",
                                event.text);
                        MessageDialog.openError(((Link)event.getSource()).getShell(),
                                "Browser Error", message);
                    }
                }
            });

            return top;
        }

        return null;
    }
}

/*
 * Copyright (C) 2010 The Android Open Source Project
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

package org.eclipse.andmore.common.layout;

import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_CLASS;
import static com.android.SdkConstants.ATTR_HINT;
import static com.android.SdkConstants.ATTR_ID;
import static com.android.SdkConstants.ATTR_LAYOUT_HEIGHT;
import static com.android.SdkConstants.ATTR_LAYOUT_RESOURCE_PREFIX;
import static com.android.SdkConstants.ATTR_LAYOUT_WIDTH;
import static com.android.SdkConstants.ATTR_STYLE;
import static com.android.SdkConstants.ATTR_TEXT;
import static com.android.SdkConstants.DOT_LAYOUT_PARAMS;
import static com.android.SdkConstants.ID_PREFIX;
import static com.android.SdkConstants.NEW_ID_PREFIX;
import static com.android.SdkConstants.VALUE_FALSE;
import static com.android.SdkConstants.VALUE_FILL_PARENT;
import static com.android.SdkConstants.VALUE_MATCH_PARENT;
import static com.android.SdkConstants.VALUE_TRUE;
import static com.android.SdkConstants.VALUE_WRAP_CONTENT;
import static com.android.SdkConstants.VIEW_FRAGMENT;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.api.AbstractViewRule;
import com.android.ide.common.api.IAttributeInfo;
import com.android.ide.common.api.IAttributeInfo.Format;
import com.android.ide.common.api.IClientRulesEngine;
import com.android.ide.common.api.IDragElement;
import com.android.ide.common.api.IMenuCallback;
import com.android.ide.common.api.INode;
import com.android.ide.common.api.IViewMetadata;
import com.android.ide.common.api.IViewRule;
import com.android.ide.common.api.RuleAction;
import com.android.ide.common.api.RuleAction.ActionProvider;
import com.android.ide.common.api.RuleAction.ChoiceProvider;
import com.android.resources.ResourceType;
import com.android.utils.Pair;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Common IViewRule processing to all view and layout classes.
 */
public class BaseViewRule extends AbstractViewRule {
    /** List of recently edited properties */
    private static List<String> sRecent = new LinkedList<String>();

    /** Maximum number of recent properties to track and list */
    private final static int MAX_RECENT_COUNT = 12;

    // Strings used as internal ids, group ids and prefixes for actions
    private static final String FALSE_ID = "false"; //$NON-NLS-1$
    private static final String TRUE_ID = "true"; //$NON-NLS-1$
    private static final String PROP_PREFIX = "@prop@"; //$NON-NLS-1$
    private static final String CLEAR_ID = "clear"; //$NON-NLS-1$
    private static final String ZCUSTOM = "zcustom"; //$NON-NLS-1$

    protected IClientRulesEngine mRulesEngine;

    // Cache of attributes. Key is FQCN of a node mixed with its view hierarchy
    // parent. Values are a custom map as needed by getContextMenu.
    private Map<String, Map<String, Prop>> mAttributesMap =
        new HashMap<String, Map<String, Prop>>();

    @Override
    public boolean onInitialize(@NonNull String fqcn, @NonNull IClientRulesEngine engine) {
        mRulesEngine = engine;

        // This base rule can handle any class so we don't need to filter on
        // FQCN. Derived classes should do so if they can handle some
        // subclasses.

        // If onInitialize returns false, it means it can't handle the given
        // FQCN and will be unloaded.

        return true;
    }

    /**
     * Returns the {@link IClientRulesEngine} associated with this {@link IViewRule}
     *
     * @return the {@link IClientRulesEngine} associated with this {@link IViewRule}
     */
    public IClientRulesEngine getRulesEngine() {
        return mRulesEngine;
    }

    // === Context Menu ===

    /**
     * Generate custom actions for the context menu: <br/>
     * - Explicit layout_width and layout_height attributes.
     * - List of all other simple toggle attributes.
     */
    @Override
    public void addContextMenuActions(@NonNull List<RuleAction> actions,
            final @NonNull INode selectedNode) {
        String width = null;
        String currentWidth = selectedNode.getStringAttr(ANDROID_URI, ATTR_LAYOUT_WIDTH);

        String fillParent = getFillParentValueName();
        boolean canMatchParent = supportsMatchParent();
        if (canMatchParent && VALUE_FILL_PARENT.equals(currentWidth)) {
            currentWidth = VALUE_MATCH_PARENT;
        } else if (!canMatchParent && VALUE_MATCH_PARENT.equals(currentWidth)) {
            currentWidth = VALUE_FILL_PARENT;
        } else if (!VALUE_WRAP_CONTENT.equals(currentWidth) && !fillParent.equals(currentWidth)) {
            width = currentWidth;
        }

        String height = null;
        String currentHeight = selectedNode.getStringAttr(ANDROID_URI, ATTR_LAYOUT_HEIGHT);

        if (canMatchParent && VALUE_FILL_PARENT.equals(currentHeight)) {
            currentHeight = VALUE_MATCH_PARENT;
        } else if (!canMatchParent && VALUE_MATCH_PARENT.equals(currentHeight)) {
            currentHeight = VALUE_FILL_PARENT;
        } else if (!VALUE_WRAP_CONTENT.equals(currentHeight)
                && !fillParent.equals(currentHeight)) {
            height = currentHeight;
        }
        final String newWidth = width;
        final String newHeight = height;

        final IMenuCallback onChange = new IMenuCallback() {
            @Override
            public void action(
                    final @NonNull RuleAction action,
                    final @NonNull List<? extends INode> selectedNodes,
                    final @Nullable String valueId, final @Nullable Boolean newValue) {
                String fullActionId = action.getId();
                boolean isProp = fullActionId.startsWith(PROP_PREFIX);
                final String actionId = isProp ?
                        fullActionId.substring(PROP_PREFIX.length()) : fullActionId;

                if (fullActionId.equals(ATTR_LAYOUT_WIDTH)) {
                    final String newAttrValue = getValue(valueId, newWidth);
                    if (newAttrValue != null) {
                        for (INode node : selectedNodes) {
                            node.editXml("Change Attribute " + ATTR_LAYOUT_WIDTH,
                                    new PropertySettingNodeHandler(ANDROID_URI,
                                            ATTR_LAYOUT_WIDTH, newAttrValue));
                        }
                        editedProperty(ATTR_LAYOUT_WIDTH);
                    }
                    return;
                } else if (fullActionId.equals(ATTR_LAYOUT_HEIGHT)) {
                    // Ask the user
                    final String newAttrValue = getValue(valueId, newHeight);
                    if (newAttrValue != null) {
                        for (INode node : selectedNodes) {
                            node.editXml("Change Attribute " + ATTR_LAYOUT_HEIGHT,
                                    new PropertySettingNodeHandler(ANDROID_URI,
                                            ATTR_LAYOUT_HEIGHT, newAttrValue));
                        }
                        editedProperty(ATTR_LAYOUT_HEIGHT);
                    }
                    return;
                } else if (fullActionId.equals(ATTR_ID)) {
                    // Ids must be set individually so open the id dialog for each
                    // selected node (though allow cancel to break the loop)
                    for (INode node : selectedNodes) {
                        if (!mRulesEngine.rename(node)) {
                            break;
                        }
                    }
                    editedProperty(ATTR_ID);
                    return;
                } else if (isProp) {
                    INode firstNode = selectedNodes.get(0);
                    String key = getPropertyMapKey(selectedNode);
                    Map<String, Prop> props = mAttributesMap.get(key);
                    final Prop prop = (props != null) ? props.get(actionId) : null;

                    if (prop != null) {
                        editedProperty(actionId);

                        // For custom values (requiring an input dialog) input the
                        // value outside the undo-block.
                        // Input the value as a text, unless we know it's the "text" or
                        // "style" attributes (where we know we want to ask for specific
                        // resource types).
                        String uri = ANDROID_URI;
                        String v = null;
                        if (prop.isStringEdit()) {
                            boolean isStyle = actionId.equals(ATTR_STYLE);
                            boolean isText = actionId.equals(ATTR_TEXT);
                            boolean isHint = actionId.equals(ATTR_HINT);
                            if (isStyle || isText || isHint) {
                                String resourceTypeName = isStyle
                                        ? ResourceType.STYLE.getName()
                                        : ResourceType.STRING.getName();
                                String oldValue = selectedNodes.size() == 1
                                    ? (isStyle ? firstNode.getStringAttr(ATTR_STYLE, actionId)
                                            : firstNode.getStringAttr(ANDROID_URI, actionId))
                                    : ""; //$NON-NLS-1$
                                oldValue = ensureValidString(oldValue);
                                v = mRulesEngine.displayResourceInput(resourceTypeName, oldValue);
                                if (isStyle) {
                                    uri = null;
                                }
                            } else if (actionId.equals(ATTR_CLASS) && selectedNodes.size() >= 1 &&
                                    VIEW_FRAGMENT.equals(selectedNodes.get(0).getFqcn())) {
                                v = mRulesEngine.displayFragmentSourceInput();
                                uri = null;
                            } else {
                                v = inputAttributeValue(firstNode, actionId);
                            }
                        }
                        final String customValue = v;

                        for (INode n : selectedNodes) {
                            if (prop.isToggle()) {
                                // case of toggle
                                String value = "";                  //$NON-NLS-1$
                                if (valueId.equals(TRUE_ID)) {
                                    value = newValue ? "true" : ""; //$NON-NLS-1$ //$NON-NLS-2$
                                } else if (valueId.equals(FALSE_ID)) {
                                    value = newValue ? "false" : "";//$NON-NLS-1$ //$NON-NLS-2$
                                }
                                n.setAttribute(uri, actionId, value);
                            } else if (prop.isFlag()) {
                                // case of a flag
                                String values = "";                 //$NON-NLS-1$
                                if (!valueId.equals(CLEAR_ID)) {
                                    values = n.getStringAttr(ANDROID_URI, actionId);
                                    Set<String> newValues = new HashSet<String>();
                                    if (values != null) {
                                        newValues.addAll(Arrays.asList(
                                                values.split("\\|"))); //$NON-NLS-1$
                                    }
                                    if (newValue) {
                                        newValues.add(valueId);
                                    } else {
                                        newValues.remove(valueId);
                                    }

                                    List<String> sorted = new ArrayList<String>(newValues);
                                    Collections.sort(sorted);
                                    values = join('|', sorted);

                                    // Special case
                                    if (valueId.equals("normal")) { //$NON-NLS-1$
                                        // For textStyle for example, if you have "bold|italic"
                                        // and you select the "normal" property, this should
                                        // not behave in the normal flag way and "or" itself in;
                                        // it should replace the other two.
                                        // This also applies to imeOptions.
                                        values = valueId;
                                    }
                                }
                                n.setAttribute(uri, actionId, values);
                            } else if (prop.isEnum()) {
                                // case of an enum
                                String value = "";                   //$NON-NLS-1$
                                if (!valueId.equals(CLEAR_ID)) {
                                    value = newValue ? valueId : ""; //$NON-NLS-1$
                                }
                                n.setAttribute(uri, actionId, value);
                            } else {
                                assert prop.isStringEdit();
                                // We've already received the value outside the undo block
                                if (customValue != null) {
                                    n.setAttribute(uri, actionId, customValue);
                                }
                            }
                        }
                    }
                }
            }

            /**
             * Input the custom value for the given attribute. This will use the Reference
             * Chooser if it is a reference value, otherwise a plain text editor.
             */
            private String inputAttributeValue(final INode node, final String attribute) {
                String oldValue = node.getStringAttr(ANDROID_URI, attribute);
                oldValue = ensureValidString(oldValue);
                IAttributeInfo attributeInfo = node.getAttributeInfo(ANDROID_URI, attribute);
                if (attributeInfo != null
                        && attributeInfo.getFormats().contains(Format.REFERENCE)) {
                    return mRulesEngine.displayReferenceInput(oldValue);
                } else {
                    // A single resource type? If so use a resource chooser initialized
                    // to this specific type
                    /* This does not work well, because the metadata is a bit misleading:
                     * for example a Button's "text" property and a Button's "onClick" property
                     * both claim to be of type [string], but @string/ is NOT valid for
                     * onClick..
                    if (attributeInfo != null && attributeInfo.getFormats().length == 1) {
                        // Resource chooser
                        Format format = attributeInfo.getFormats()[0];
                        return mRulesEngine.displayResourceInput(format.name(), oldValue);
                    }
                    */

                    // Fallback: just edit the raw XML string
                    String message = String.format("New %1$s Value:", attribute);
                    return mRulesEngine.displayInput(message, oldValue, null);
                }
            }

            /**
             * Returns the value (which will ask the user if the value is the special
             * {@link #ZCUSTOM} marker
             */
            private String getValue(String valueId, String defaultValue) {
                if (valueId.equals(ZCUSTOM)) {
                    if (defaultValue == null) {
                        defaultValue = "";
                    }
                    String value = mRulesEngine.displayInput(
                            "Set custom layout attribute value (example: 50dp)",
                            defaultValue, null);
                    if (value != null && value.trim().length() > 0) {
                        return value.trim();
                    } else {
                        return null;
                    }
                }

                return valueId;
            }
        };

        IAttributeInfo textAttribute = selectedNode.getAttributeInfo(ANDROID_URI, ATTR_TEXT);
        if (textAttribute != null) {
            actions.add(RuleAction.createAction(PROP_PREFIX + ATTR_TEXT, "Edit Text...", onChange,
                    null, 10, true));
        }

        String editIdLabel = selectedNode.getStringAttr(ANDROID_URI, ATTR_ID) != null ?
                "Edit ID..." : "Assign ID...";
        actions.add(RuleAction.createAction(ATTR_ID, editIdLabel, onChange, null, 20, true));

        addCommonPropertyActions(actions, selectedNode, onChange, 21);

        // Create width choice submenu
        actions.add(RuleAction.createSeparator(32));
        List<Pair<String, String>> widthChoices = new ArrayList<Pair<String,String>>(4);
        widthChoices.add(Pair.of(VALUE_WRAP_CONTENT, "Wrap Content"));
        if (canMatchParent) {
            widthChoices.add(Pair.of(VALUE_MATCH_PARENT, "Match Parent"));
        } else {
            widthChoices.add(Pair.of(VALUE_FILL_PARENT, "Fill Parent"));
        }
        if (width != null) {
            widthChoices.add(Pair.of(width, width));
        }
        widthChoices.add(Pair.of(ZCUSTOM, "Other..."));
        actions.add(RuleAction.createChoices(
                ATTR_LAYOUT_WIDTH, "Layout Width",
                onChange,
                null /* iconUrls */,
                currentWidth,
                null, 35,
                true, // supportsMultipleNodes
                widthChoices));

        // Create height choice submenu
        List<Pair<String, String>> heightChoices = new ArrayList<Pair<String,String>>(4);
        heightChoices.add(Pair.of(VALUE_WRAP_CONTENT, "Wrap Content"));
        if (canMatchParent) {
            heightChoices.add(Pair.of(VALUE_MATCH_PARENT, "Match Parent"));
        } else {
            heightChoices.add(Pair.of(VALUE_FILL_PARENT, "Fill Parent"));
        }
        if (height != null) {
            heightChoices.add(Pair.of(height, height));
        }
        heightChoices.add(Pair.of(ZCUSTOM, "Other..."));
        actions.add(RuleAction.createChoices(
                ATTR_LAYOUT_HEIGHT, "Layout Height",
                onChange,
                null /* iconUrls */,
                currentHeight,
                null, 40,
                true,
                heightChoices));

        actions.add(RuleAction.createSeparator(45));
        RuleAction properties = RuleAction.createChoices("properties", "Other Properties", //$NON-NLS-1$
                onChange /*callback*/, null /*icon*/, 50,
                true /*supportsMultipleNodes*/, new ActionProvider() {
            @Override
            public @NonNull List<RuleAction> getNestedActions(@NonNull INode node) {
                List<RuleAction> propertyActionTypes = new ArrayList<RuleAction>();
                propertyActionTypes.add(RuleAction.createChoices(
                        "recent", "Recent", //$NON-NLS-1$
                        onChange /*callback*/, null /*icon*/, 10,
                        true /*supportsMultipleNodes*/, new ActionProvider() {
                            @Override
                            public @NonNull List<RuleAction> getNestedActions(@NonNull INode n) {
                                List<RuleAction> propertyActions = new ArrayList<RuleAction>();
                                addRecentPropertyActions(propertyActions, n, onChange);
                                return propertyActions;
                            }
                }));

                propertyActionTypes.add(RuleAction.createSeparator(20));

                addInheritedProperties(propertyActionTypes, node, onChange, 30);

                propertyActionTypes.add(RuleAction.createSeparator(50));
                propertyActionTypes.add(RuleAction.createChoices(
                        "layoutparams", "Layout Parameters", //$NON-NLS-1$
                        onChange /*callback*/, null /*icon*/, 60,
                        true /*supportsMultipleNodes*/, new ActionProvider() {
                            @Override
                            public @NonNull List<RuleAction> getNestedActions(@NonNull INode n) {
                                List<RuleAction> propertyActions = new ArrayList<RuleAction>();
                                addPropertyActions(propertyActions, n, onChange, null, true);
                                return propertyActions;
                            }
                }));

                propertyActionTypes.add(RuleAction.createSeparator(70));

                propertyActionTypes.add(RuleAction.createChoices(
                        "allprops", "All By Name", //$NON-NLS-1$
                        onChange /*callback*/, null /*icon*/, 80,
                        true /*supportsMultipleNodes*/, new ActionProvider() {
                            @Override
                            public @NonNull List<RuleAction> getNestedActions(@NonNull INode n) {
                                List<RuleAction> propertyActions = new ArrayList<RuleAction>();
                                addPropertyActions(propertyActions, n, onChange, null, false);
                                return propertyActions;
                            }
                }));

                return propertyActionTypes;
            }
        });

        actions.add(properties);
    }

    @Override
    @Nullable
    public String getDefaultActionId(@NonNull final INode selectedNode) {
        IAttributeInfo textAttribute = selectedNode.getAttributeInfo(ANDROID_URI, ATTR_TEXT);
        if (textAttribute != null) {
            return PROP_PREFIX + ATTR_TEXT;
        }

        return null;
    }

    private static String getPropertyMapKey(INode node) {
        // Compute the key for mAttributesMap. This depends on the type of this
        // node and its parent in the view hierarchy.
        StringBuilder sb = new StringBuilder();
        sb.append(node.getFqcn());
        sb.append('_');
        INode parent = node.getParent();
        if (parent != null) {
            sb.append(parent.getFqcn());
        }
        return sb.toString();
    }

    /**
     * Adds menu items for the inherited attributes, one pull-right menu for each super class
     * that defines attributes.
     *
     * @param propertyActionTypes the actions list to add into
     * @param node the node to apply the attributes to
     * @param onChange the callback to use for setting attributes
     * @param sortPriority the initial sort attribute for the first menu item
     */
    private void addInheritedProperties(List<RuleAction> propertyActionTypes, INode node,
            final IMenuCallback onChange, int sortPriority) {
        List<String> attributeSources = node.getAttributeSources();
        for (final String definedBy : attributeSources) {
            String sourceClass = definedBy;

            // Strip package prefixes when necessary
            int index = sourceClass.length();
            if (sourceClass.endsWith(DOT_LAYOUT_PARAMS)) {
                index = sourceClass.length() - DOT_LAYOUT_PARAMS.length() - 1;
            }
            int lastDot = sourceClass.lastIndexOf('.', index);
            if (lastDot != -1) {
                sourceClass = sourceClass.substring(lastDot + 1);
            }

            String label;
            if (definedBy.equals(node.getFqcn())) {
                label = String.format("Defined by %1$s", sourceClass);
            } else {
                label = String.format("Inherited from %1$s", sourceClass);
            }

            propertyActionTypes.add(RuleAction.createChoices("def_" + definedBy,
                    label,
                    onChange /*callback*/, null /*icon*/, sortPriority++,
                    true /*supportsMultipleNodes*/, new ActionProvider() {
                        @Override
                        public @NonNull List<RuleAction> getNestedActions(@NonNull INode n) {
                            List<RuleAction> propertyActions = new ArrayList<RuleAction>();
                            addPropertyActions(propertyActions, n, onChange, definedBy, false);
                            return propertyActions;
                        }
           }));
        }
    }

    /**
     * Creates a list of properties that are commonly edited for views of the
     * selected node's type
     */
    private void addCommonPropertyActions(List<RuleAction> actions, INode selectedNode,
            IMenuCallback onChange, int sortPriority) {
        Map<String, Prop> properties = getPropertyMetadata(selectedNode);
        IViewMetadata metadata = mRulesEngine.getMetadata(selectedNode.getFqcn());
        if (metadata != null) {
            List<String> attributes = metadata.getTopAttributes();
            if (attributes.size() > 0) {
                for (String attribute : attributes) {
                    // Text and ID are handled manually in the menu construction code because
                    // we want to place them consistently and customize the action label
                    if (ATTR_TEXT.equals(attribute) || ATTR_ID.equals(attribute)) {
                        continue;
                    }

                    Prop property = properties.get(attribute);
                    if (property != null) {
                        String title = property.getTitle();
                        if (title.endsWith("...")) {
                            title = String.format("Edit %1$s", property.getTitle());
                        }
                        actions.add(createPropertyAction(property, attribute, title,
                                selectedNode, onChange, sortPriority));
                        sortPriority++;
                    }
                }
            }
        }
    }

    /**
     * Record that the given property was just edited; adds it to the front of
     * the recently edited property list
     *
     * @param property the name of the property
     */
    static void editedProperty(String property) {
        if (sRecent.contains(property)) {
            sRecent.remove(property);
        } else if (sRecent.size() > MAX_RECENT_COUNT) {
            sRecent.remove(sRecent.size() - 1);
        }
        sRecent.add(0, property);
    }

    /**
     * Creates a list of recently modified properties that apply to the given selected node
     */
    private void addRecentPropertyActions(List<RuleAction> actions, INode selectedNode,
            IMenuCallback onChange) {
        int sortPriority = 10;
        Map<String, Prop> properties = getPropertyMetadata(selectedNode);
        for (String attribute : sRecent) {
            Prop property = properties.get(attribute);
            if (property != null) {
                actions.add(createPropertyAction(property, attribute, property.getTitle(),
                        selectedNode, onChange, sortPriority));
                sortPriority += 10;
            }
        }
    }

    /**
     * Creates a list of nested actions representing the property-setting
     * actions for the given selected node
     */
    private void addPropertyActions(List<RuleAction> actions, INode selectedNode,
            IMenuCallback onChange, String definedBy, boolean layoutParamsOnly) {

        Map<String, Prop> properties = getPropertyMetadata(selectedNode);

        int sortPriority = 10;
        for (Map.Entry<String, Prop> entry : properties.entrySet()) {
            String id = entry.getKey();
            Prop property = entry.getValue();
            if (layoutParamsOnly) {
                // If we have definedBy information, that is most accurate; all layout
                // params will be defined by a class whose name ends with
                // .LayoutParams:
                if (definedBy != null) {
                    if (!definedBy.endsWith(DOT_LAYOUT_PARAMS)) {
                        continue;
                    }
                } else if (!id.startsWith(ATTR_LAYOUT_RESOURCE_PREFIX)) {
                    continue;
                }
            }
            if (definedBy != null && !definedBy.equals(property.getDefinedBy())) {
                continue;
            }
            actions.add(createPropertyAction(property, id, property.getTitle(),
                    selectedNode, onChange, sortPriority));
            sortPriority += 10;
        }

        // The properties are coming out of map key order which isn't right, so sort
        // alphabetically instead
        Collections.sort(actions, new Comparator<RuleAction>() {
            @Override
            public int compare(RuleAction action1, RuleAction action2) {
                return action1.getTitle().compareTo(action2.getTitle());
            }
        });
    }

    private RuleAction createPropertyAction(Prop p, String id, String title, INode selectedNode,
            IMenuCallback onChange, int sortPriority) {
        if (p.isToggle()) {
            // Toggles are handled as a multiple-choice between true, false
            // and nothing (clear)
            String value = selectedNode.getStringAttr(ANDROID_URI, id);
            if (value != null) {
                value = value.toLowerCase(Locale.US);
            }
            if (VALUE_TRUE.equals(value)) {
                value = TRUE_ID;
            } else if (VALUE_FALSE.equals(value)) {
                value = FALSE_ID;
            } else {
                value = CLEAR_ID;
            }
            return RuleAction.createChoices(PROP_PREFIX + id, title,
                    onChange, BOOLEAN_CHOICE_PROVIDER,
                    value,
                    null, sortPriority,
                    true);
        } else if (p.getChoices() != null) {
            // Enum or flags. Their possible values are the multiple-choice
            // items, with an extra "clear" option to remove everything.
            String current = selectedNode.getStringAttr(ANDROID_URI, id);
            if (current == null || current.length() == 0) {
                current = CLEAR_ID;
            }
            return RuleAction.createChoices(PROP_PREFIX + id, title,
                    onChange, new EnumPropertyChoiceProvider(p),
                    current,
                    null, sortPriority,
                    true);
        } else {
            return RuleAction.createAction(
                    PROP_PREFIX + id,
                    title,
                    onChange,
                    null, sortPriority,
                    true);
        }
    }

    private Map<String, Prop> getPropertyMetadata(final INode selectedNode) {
        String key = getPropertyMapKey(selectedNode);
        Map<String, Prop> props = mAttributesMap.get(key);
        if (props == null) {
            // Prepare the property map
            props = new HashMap<String, Prop>();
            for (IAttributeInfo attrInfo : selectedNode.getDeclaredAttributes()) {
                String id = attrInfo != null ? attrInfo.getName() : null;
                if (id == null || id.equals(ATTR_LAYOUT_WIDTH) || id.equals(ATTR_LAYOUT_HEIGHT)) {
                    // Layout width/height are already handled at the root level
                    continue;
                }
                if (attrInfo == null) {
                    continue;
                }
                EnumSet<Format> formats = attrInfo.getFormats();

                String title = getAttributeDisplayName(id);

                String definedBy = attrInfo != null ? attrInfo.getDefinedBy() : null;
                if (formats.contains(IAttributeInfo.Format.BOOLEAN)) {
                    props.put(id, new Prop(title, true, definedBy));
                } else if (formats.contains(IAttributeInfo.Format.ENUM)) {
                    // Convert each enum into a map id=>title
                    Map<String, String> values = new HashMap<String, String>();
                    if (attrInfo != null) {
                        for (String e : attrInfo.getEnumValues()) {
                            values.put(e, getAttributeDisplayName(e));
                        }
                    }

                    props.put(id, new Prop(title, false, false, values, definedBy));
                } else if (formats.contains(IAttributeInfo.Format.FLAG)) {
                    // Convert each flag into a map id=>title
                    Map<String, String> values = new HashMap<String, String>();
                    if (attrInfo != null) {
                        for (String e : attrInfo.getFlagValues()) {
                            values.put(e, getAttributeDisplayName(e));
                        }
                    }

                    props.put(id, new Prop(title, false, true, values, definedBy));
                } else {
                    props.put(id, new Prop(title + "...", false, definedBy));
                }
            }
            mAttributesMap.put(key, props);
        }
        return props;
    }

    /**
     * A {@link ChoiceProvder} which provides alternatives suitable for choosing
     * values for a boolean property: true, false, or "default".
     */
    private static ChoiceProvider BOOLEAN_CHOICE_PROVIDER = new ChoiceProvider() {
        @Override
        public void addChoices(@NonNull List<String> titles, @NonNull List<URL> iconUrls,
                @NonNull List<String> ids) {
            titles.add("True");
            ids.add(TRUE_ID);

            titles.add("False");
            ids.add(FALSE_ID);

            titles.add(RuleAction.SEPARATOR);
            ids.add(RuleAction.SEPARATOR);

            titles.add("Default");
            ids.add(CLEAR_ID);
        }
    };

    /**
     * A {@link ChoiceProvider} which provides the various available
     * attribute values available for a given {@link Prop} property descriptor.
     */
    private static class EnumPropertyChoiceProvider implements ChoiceProvider {
        private Prop mProperty;

        public EnumPropertyChoiceProvider(Prop property) {
            super();
            mProperty = property;
        }

        @Override
        public void addChoices(@NonNull List<String> titles, @NonNull List<URL> iconUrls,
                @NonNull List<String> ids) {
            for (Entry<String, String> entry : mProperty.getChoices().entrySet()) {
                ids.add(entry.getKey());
                titles.add(entry.getValue());
            }

            titles.add(RuleAction.SEPARATOR);
            ids.add(RuleAction.SEPARATOR);

            titles.add("Default");
            ids.add(CLEAR_ID);
        }
    }

    /**
     * Returns true if the given node is "filled" (e.g. has layout width set to match
     * parent or fill parent
     */
    protected final boolean isFilled(INode node, String attribute) {
        String value = node.getStringAttr(ANDROID_URI, attribute);
        return VALUE_MATCH_PARENT.equals(value) || VALUE_FILL_PARENT.equals(value);
    }

    /**
     * Returns fill_parent or match_parent, depending on whether the minimum supported
     * platform supports match_parent or not
     *
     * @return match_parent or fill_parent depending on which is supported by the project
     */
    protected final String getFillParentValueName() {
        return supportsMatchParent() ? VALUE_MATCH_PARENT : VALUE_FILL_PARENT;
    }

    /**
     * Returns true if the project supports match_parent instead of just fill_parent
     *
     * @return true if the project supports match_parent instead of just fill_parent
     */
    protected final boolean supportsMatchParent() {
        // fill_parent was renamed match_parent in API level 8
        return mRulesEngine.getMinApiLevel() >= 8;
    }

    /** Join strings into a single string with the given delimiter */
    static String join(char delimiter, Collection<String> strings) {
        StringBuilder sb = new StringBuilder(100);
        for (String s : strings) {
            if (sb.length() > 0) {
                sb.append(delimiter);
            }
            sb.append(s);
        }
        return sb.toString();
    }

    static Map<String, String> concatenate(Map<String, String> pre, Map<String, String> post) {
        Map<String, String> result = new HashMap<String, String>(pre.size() + post.size());
        result.putAll(pre);
        result.putAll(post);
        return result;
    }

    // Quick utility for building up maps declaratively to minimize the diffs
    static Map<String, String> mapify(String... values) {
        Map<String, String> map = new HashMap<String, String>(values.length / 2);
        for (int i = 0; i < values.length; i += 2) {
            String key = values[i];
            if (key == null) {
                continue;
            }
            String value = values[i + 1];
            map.put(key, value);
        }

        return map;
    }

    /**
     * Produces a display name for an attribute, usually capitalizing the attribute name
     * and splitting up underscores into new words
     *
     * @param name the attribute name to convert
     * @return a display name for the attribute name
     */
    public static String getAttributeDisplayName(String name) {
        if (name != null && name.length() > 0) {
            StringBuilder sb = new StringBuilder();
            boolean capitalizeNext = true;
            for (int i = 0, n = name.length(); i < n; i++) {
                char c = name.charAt(i);
                if (capitalizeNext) {
                    c = Character.toUpperCase(c);
                }
                capitalizeNext = false;
                if (c == '_') {
                    c = ' ';
                    capitalizeNext = true;
                }
                sb.append(c);
            }

            return sb.toString();
        }

        return name;
    }


    // ==== Paste support ====

    /**
     * Most views can't accept children so there's nothing to paste on them. In
     * this case, defer the call to the parent layout and use the target node as
     * an indication of where to paste.
     */
    @Override
    public void onPaste(@NonNull INode targetNode, @Nullable Object targetView,
            @NonNull IDragElement[] elements) {
        //
        INode parent = targetNode.getParent();
        if (parent != null) {
            String parentFqcn = parent.getFqcn();
            IViewRule parentRule = mRulesEngine.loadRule(parentFqcn);

            if (parentRule instanceof BaseLayoutRule) {
                ((BaseLayoutRule) parentRule).onPasteBeforeChild(parent, targetView, targetNode,
                        elements);
            }
        }
    }

    /**
     * Support class for the context menu code. Stores state about properties in
     * the context menu.
     */
    private static class Prop {
        private final boolean mToggle;
        private final boolean mFlag;
        private final String mTitle;
        private final Map<String, String> mChoices;
        private String mDefinedBy;

        public Prop(String title, boolean isToggle, boolean isFlag, Map<String, String> choices,
                String definedBy) {
            mTitle = title;
            mToggle = isToggle;
            mFlag = isFlag;
            mChoices = choices;
            mDefinedBy = definedBy;
        }

        public String getDefinedBy() {
            return mDefinedBy;
        }

        public Prop(String title, boolean isToggle, String definedBy) {
            this(title, isToggle, false, null, definedBy);
        }

        private boolean isToggle() {
            return mToggle;
        }

        private boolean isFlag() {
            return mFlag && mChoices != null;
        }

        private boolean isEnum() {
            return !mFlag && mChoices != null;
        }

        private String getTitle() {
            return mTitle;
        }

        private Map<String, String> getChoices() {
            return mChoices;
        }

        private boolean isStringEdit() {
            return mChoices == null && !mToggle;
        }
    }

    /**
     * Returns a source attribute value which points to a sample image. This is typically
     * used to provide an initial image shown on ImageButtons, etc. There is no guarantee
     * that the source pointed to by this method actually exists.
     *
     * @return a source attribute to use for sample images, never null
     */
    protected final String getSampleImageSrc() {
        // Builtin graphics available since v1:
        return "@android:drawable/btn_star"; //$NON-NLS-1$
    }

    /**
     * Strips the {@code @+id} or {@code @id} prefix off of the given id
     *
     * @param id attribute to be stripped
     * @return the id name without the {@code @+id} or {@code @id} prefix
     */
    @NonNull
    public static String stripIdPrefix(@Nullable String id) {
        if (id == null) {
            return ""; //$NON-NLS-1$
        } else if (id.startsWith(NEW_ID_PREFIX)) {
            return id.substring(NEW_ID_PREFIX.length());
        } else if (id.startsWith(ID_PREFIX)) {
            return id.substring(ID_PREFIX.length());
        }
        return id;
    }

    private static String ensureValidString(String value) {
        if (value == null) {
            value = ""; //$NON-NLS-1$
        }
        return value;
    }
 }

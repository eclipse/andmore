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

import static com.android.SdkConstants.ATTR_CONTENT_DESCRIPTION;
import static com.android.SdkConstants.ATTR_HINT;
import static com.android.SdkConstants.ATTR_TEXT;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/** Extra metadata about properties not available from the descriptors (yet) */
class PropertyMetadata {
    static boolean isAdvanced(@NonNull String name) {
        return sAdvanced.contains(name);
    }

    static boolean isPreferred(@NonNull String name) {
        return sPreferred.contains(name);
    }

    @Nullable
    static String getCategory(@NonNull String name) {
        //return sCategories.get(name);
        assert false : "Disabled to save memory since this method is not currently used.";
        return null;
    }

    private static final int ADVANCED_MAP_SIZE = 134;
    private static final Set<String> sAdvanced = new HashSet<String>(ADVANCED_MAP_SIZE);
    static {
        // This metadata about which attributes are "advanced" was generated as follows:
        // First, I ran the sdk/attribute_stats project with the --list argument to dump out
        // *all* referenced XML attributes found in layouts, run against a bunch of
        // sample Android code (development/samples, packages/apps, vendor, etc.
        //
        // Then I iterated over the LayoutDescriptors' ViewElementDescriptors'
        // AttributeDescriptors, and basically diffed the two: any attribute descriptor name
        // which was *not* found in any of the representative layouts is added here
        // as an advanced property.
        //
        // Then I manually edited in some attributes that were referenced in the sample
        // layouts but which I still consider to be advanced:
        // -- nothing right now

        // I also manually *removed* some entries from the below list:
        //     drawableBottom   (the others, drawableTop, drawableLeft and drawableRight were all
        //                       NOT on the list so keep bottom off for symmetry)
        //     rating     (useful when you deal with a RatingsBar component)


        // Automatically generated, see above:
        sAdvanced.add("alwaysDrawnWithCache");
        sAdvanced.add("animationCache");
        sAdvanced.add("animationDuration");
        sAdvanced.add("animationResolution");
        sAdvanced.add("baseline");
        sAdvanced.add("bufferType");
        sAdvanced.add("calendarViewShown");
        sAdvanced.add("completionHint");
        sAdvanced.add("completionHintView");
        sAdvanced.add("completionThreshold");
        sAdvanced.add("cursorVisible");
        sAdvanced.add("dateTextAppearance");
        sAdvanced.add("dial");
        sAdvanced.add("digits");
        sAdvanced.add("disableChildrenWhenDisabled");
        sAdvanced.add("disabledAlpha");
        sAdvanced.add("drawableAlpha");
        sAdvanced.add("drawableEnd");
        sAdvanced.add("drawableStart");
        sAdvanced.add("drawingCacheQuality");
        sAdvanced.add("dropDownAnchor");
        sAdvanced.add("dropDownHeight");
        sAdvanced.add("dropDownHorizontalOffset");
        sAdvanced.add("dropDownSelector");
        sAdvanced.add("dropDownVerticalOffset");
        sAdvanced.add("dropDownWidth");
        sAdvanced.add("editorExtras");
        sAdvanced.add("ems");
        sAdvanced.add("endYear");
        sAdvanced.add("eventsInterceptionEnabled");
        sAdvanced.add("fadeDuration");
        sAdvanced.add("fadeEnabled");
        sAdvanced.add("fadeOffset");
        sAdvanced.add("fadeScrollbars");
        sAdvanced.add("filterTouchesWhenObscured");
        sAdvanced.add("firstDayOfWeek");
        sAdvanced.add("flingable");
        sAdvanced.add("focusedMonthDateColor");
        sAdvanced.add("foregroundInsidePadding");
        sAdvanced.add("format");
        sAdvanced.add("gestureColor");
        sAdvanced.add("gestureStrokeAngleThreshold");
        sAdvanced.add("gestureStrokeLengthThreshold");
        sAdvanced.add("gestureStrokeSquarenessThreshold");
        sAdvanced.add("gestureStrokeType");
        sAdvanced.add("gestureStrokeWidth");
        sAdvanced.add("hand_hour");
        sAdvanced.add("hand_minute");
        sAdvanced.add("hapticFeedbackEnabled");
        sAdvanced.add("id");
        sAdvanced.add("imeActionId");
        sAdvanced.add("imeActionLabel");
        sAdvanced.add("indeterminateDrawable");
        sAdvanced.add("indeterminateDuration");
        sAdvanced.add("inputMethod");
        sAdvanced.add("interpolator");
        sAdvanced.add("isScrollContainer");
        sAdvanced.add("keepScreenOn");
        sAdvanced.add("layerType");
        sAdvanced.add("layoutDirection");
        sAdvanced.add("maxDate");
        sAdvanced.add("minDate");
        sAdvanced.add("mode");
        sAdvanced.add("numeric");
        sAdvanced.add("paddingEnd");
        sAdvanced.add("paddingStart");
        sAdvanced.add("persistentDrawingCache");
        sAdvanced.add("phoneNumber");
        sAdvanced.add("popupBackground");
        sAdvanced.add("popupPromptView");
        sAdvanced.add("privateImeOptions");
        sAdvanced.add("quickContactWindowSize");
        //sAdvanced.add("rating");
        sAdvanced.add("requiresFadingEdge");
        sAdvanced.add("rotation");
        sAdvanced.add("rotationX");
        sAdvanced.add("rotationY");
        sAdvanced.add("saveEnabled");
        sAdvanced.add("scaleX");
        sAdvanced.add("scaleY");
        sAdvanced.add("scrollX");
        sAdvanced.add("scrollY");
        sAdvanced.add("scrollbarAlwaysDrawHorizontalTrack");
        sAdvanced.add("scrollbarDefaultDelayBeforeFade");
        sAdvanced.add("scrollbarFadeDuration");
        sAdvanced.add("scrollbarSize");
        sAdvanced.add("scrollbarThumbHorizontal");
        sAdvanced.add("scrollbarThumbVertical");
        sAdvanced.add("scrollbarTrackHorizontal");
        sAdvanced.add("scrollbarTrackVertical");
        sAdvanced.add("secondaryProgress");
        sAdvanced.add("selectedDateVerticalBar");
        sAdvanced.add("selectedWeekBackgroundColor");
        sAdvanced.add("selectionDivider");
        sAdvanced.add("selectionDividerHeight");
        sAdvanced.add("showWeekNumber");
        sAdvanced.add("shownWeekCount");
        sAdvanced.add("solidColor");
        sAdvanced.add("soundEffectsEnabled");
        sAdvanced.add("spinnerMode");
        sAdvanced.add("spinnersShown");
        sAdvanced.add("startYear");
        sAdvanced.add("switchMinWidth");
        sAdvanced.add("switchPadding");
        sAdvanced.add("switchTextAppearance");
        sAdvanced.add("textColorHighlight");
        sAdvanced.add("textCursorDrawable");
        sAdvanced.add("textDirection");
        sAdvanced.add("textEditNoPasteWindowLayout");
        sAdvanced.add("textEditPasteWindowLayout");
        sAdvanced.add("textEditSideNoPasteWindowLayout");
        sAdvanced.add("textEditSidePasteWindowLayout");
        sAdvanced.add("textEditSuggestionItemLayout");
        sAdvanced.add("textIsSelectable");
        sAdvanced.add("textOff");
        sAdvanced.add("textOn");
        sAdvanced.add("textScaleX");
        sAdvanced.add("textSelectHandle");
        sAdvanced.add("textSelectHandleLeft");
        sAdvanced.add("textSelectHandleRight");
        sAdvanced.add("thumbOffset");
        sAdvanced.add("thumbTextPadding");
        sAdvanced.add("tint");
        sAdvanced.add("track");
        sAdvanced.add("transformPivotX");
        sAdvanced.add("transformPivotY");
        sAdvanced.add("translationX");
        sAdvanced.add("translationY");
        sAdvanced.add("uncertainGestureColor");
        sAdvanced.add("unfocusedMonthDateColor");
        sAdvanced.add("unselectedAlpha");
        sAdvanced.add("verticalScrollbarPosition");
        sAdvanced.add("weekDayTextAppearance");
        sAdvanced.add("weekNumberColor");
        sAdvanced.add("weekSeparatorLineColor");

        assert sAdvanced.size() == ADVANCED_MAP_SIZE : sAdvanced.size();

    }

    private static final int PREFERRED_MAP_SIZE = 7;
    private static final Set<String> sPreferred = new HashSet<String>(PREFERRED_MAP_SIZE);
    static {
        // Manual registrations of attributes that should be treated as preferred if
        // they are available on a widget even if they don't show up in the top 10% of
        // usages (which the view metadata provides)
        sPreferred.add(ATTR_TEXT);
        sPreferred.add(ATTR_CONTENT_DESCRIPTION);
        sPreferred.add(ATTR_HINT);
        sPreferred.add("indeterminate");
        sPreferred.add("progress");
        sPreferred.add("rating");
        sPreferred.add("max");
        assert sPreferred.size() == PREFERRED_MAP_SIZE : sPreferred.size();
    }

    /*
    private static final int CATEGORY_MAP_SIZE = 62;
    private static final Map<String, String> sCategories =
            new HashMap<String, String>(CATEGORY_MAP_SIZE);
    static {
        sCategories.put("requiresFadingEdge", "Scrolling");
        sCategories.put("fadingEdgeLength", "Scrolling");
        sCategories.put("scrollbarSize", "Scrolling");
        sCategories.put("scrollbarThumbVertical", "Scrolling");
        sCategories.put("scrollbarThumbHorizontal", "Scrolling");
        sCategories.put("scrollbarTrackHorizontal", "Scrolling");
        sCategories.put("scrollbarTrackVertical", "Scrolling");
        sCategories.put("scrollbarAlwaysDrawHorizontalTrack", "Scrolling");
        sCategories.put("scrollbarAlwaysDrawVerticalTrack", "Scrolling");
        sCategories.put("scrollViewStyle", "Scrolling");
        sCategories.put("scrollbars", "Scrolling");
        sCategories.put("scrollingCache", "Scrolling");
        sCategories.put("scrollHorizontally", "Scrolling");
        sCategories.put("scrollbarFadeDuration", "Scrolling");
        sCategories.put("scrollbarDefaultDelayBeforeFade", "Scrolling");
        sCategories.put("fastScrollEnabled", "Scrolling");
        sCategories.put("smoothScrollbar", "Scrolling");
        sCategories.put("isScrollContainer", "Scrolling");
        sCategories.put("fadeScrollbars", "Scrolling");
        sCategories.put("overScrollMode", "Scrolling");
        sCategories.put("overScrollHeader", "Scrolling");
        sCategories.put("overScrollFooter", "Scrolling");
        sCategories.put("verticalScrollbarPosition", "Scrolling");
        sCategories.put("fastScrollAlwaysVisible", "Scrolling");
        sCategories.put("fastScrollThumbDrawable", "Scrolling");
        sCategories.put("fastScrollPreviewBackgroundLeft", "Scrolling");
        sCategories.put("fastScrollPreviewBackgroundRight", "Scrolling");
        sCategories.put("fastScrollTrackDrawable", "Scrolling");
        sCategories.put("fastScrollOverlayPosition", "Scrolling");
        sCategories.put("horizontalScrollViewStyle", "Scrolling");
        sCategories.put("fastScrollTextColor", "Scrolling");
        sCategories.put("scrollbarSize", "Scrolling");
        sCategories.put("scrollbarSize", "Scrolling");
        sCategories.put("scrollbarSize", "Scrolling");
        sCategories.put("scrollbarSize", "Scrolling");
        sCategories.put("scrollbarSize", "Scrolling");

        // TODO: All the styles: radioButtonStyle, ratingBarStyle, progressBarStyle, ...

        sCategories.put("focusable", "Focus");
        sCategories.put("focusableInTouchMode", "Focus");
        sCategories.put("nextFocusLeft", "Focus");
        sCategories.put("nextFocusRight", "Focus");
        sCategories.put("nextFocusUp", "Focus");
        sCategories.put("nextFocusDown", "Focus");
        sCategories.put("descendantFocusability", "Focus");
        sCategories.put("selectAllOnFocus", "Focus");
        sCategories.put("nextFocusForward", "Focus");
        sCategories.put("colorFocusedHighlight", "Focus");

        sCategories.put("rotation", "Transforms");
        sCategories.put("scrollX", "Transforms");
        sCategories.put("scrollY", "Transforms");
        sCategories.put("rotationX", "Transforms");
        sCategories.put("rotationY", "Transforms");
        sCategories.put("transformPivotX", "Transforms");
        sCategories.put("transformPivotY", "Transforms");
        sCategories.put("translationX", "Transforms");
        sCategories.put("translationY", "Transforms");
        sCategories.put("scaleX", "Transforms");
        sCategories.put("scaleY", "Transforms");

        sCategories.put("width", "Size");
        sCategories.put("height", "Size");
        sCategories.put("minWidth", "Size");
        sCategories.put("minHeight", "Size");

        sCategories.put("longClickable", "Clicks");
        sCategories.put("onClick", "Clicks");
        sCategories.put("clickable", "Clicks");
        sCategories.put("hapticFeedbackEnabled", "Clicks");

        sCategories.put("duplicateParentState", "State");
        sCategories.put("addStatesFromChildren", "State");

        assert sCategories.size() == CATEGORY_MAP_SIZE : sCategories.size();
    }
    */

//    private static final int PRIO_CLZ_LAYOUT = 1000;
//    private static final int PRIO_CLZ_TEXT = 2000;
//    private static final int PRIO_CLZ_DRAWABLE = 3000;
//    private static final int PRIO_CLZ_ANIMATION = 4000;
//    private static final int PRIO_CLZ_FOCUS = 5000;
//
//    private static final int PRIORITY_MAP_SIZE = 100;
//    private static final Map<String, Integer> sPriorities =
//            new HashMap<String, Integer>(PRIORITY_MAP_SIZE);
//    static {
//        // TODO: I should put all the properties roughly based on their original order: this
//        // will correspond to the rough order they came in with
//        // TODO: How can I make similar complex properties show up adjacent; e.g. min and max
//        sPriorities.put("min", PRIO_CLZ_LAYOUT);
//        sPriorities.put("max", PRIO_CLZ_LAYOUT);
//
//        assert sPriorities.size() == PRIORITY_MAP_SIZE : sPriorities.size();
//    }

    // TODO: Emit metadata into a file
}

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
package org.eclipse.andmore.internal.lint;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.detector.api.Issue;

import org.eclipse.andmore.internal.editors.IconFactory;
import org.eclipse.andmore.internal.editors.layout.gle2.SwtUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import java.io.File;
import java.util.Comparator;

/** A column shown in the {@link LintList} */
abstract class LintColumn implements Comparator<IMarker> {
    protected final LintList mList;

    protected LintColumn(@NonNull LintList list) {
        mList = list;
    }

    /** @return true if this column should be shown by default */
    public boolean isVisibleByDefault() {
        return true;
    }

    /** @return true if this column's text should be left aligned */
    public boolean isLeftAligned() {
        return true;
    }

    /**
     * @return the number of pixels that this column should show by default
     */
    public int getPreferredWidth() {
        return getPreferredCharWidth() * SwtUtils.getAverageCharWidth(mList.getDisplay(),
                mList.getTree().getFont());
    }

    /**
     * @return the number of characters that this column should show by default
     */
    public int getPreferredCharWidth() {
        return 15;
    }

    /**
     * @return the title of the column
     */
    @NonNull
    public abstract String getColumnHeaderText();

    /**
     * @return the image of the column, or null
     */
    public Image getColumnHeaderImage() {
        return null;
    }

    /**
     * @param marker the {@link IMarker} to get the value for
     * @return the value of this column for the given marker
     */
    public abstract String getValue(@NonNull IMarker marker);

    /**
     * @param marker the {@link IMarker} to get the value for
     * @return the styled value of this column for the given marker
     */
    public StyledString getStyledValue(@NonNull IMarker marker) {
        return null;
    }

    /**
     * @param marker the {@link IMarker} to get the image for
     * @return The image for this particular column, or null
     */
    @Nullable
    public Image getImage(@NonNull IMarker marker) {
        return null;
    }

    /**
     * @param marker the {@link IMarker} to get the font for
     * @return The font for this particular column, or null
     */
    @Nullable
    public Font getFont(@NonNull IMarker marker) {
        return null;
    }

    /**
     * @return true if the sort should be in ascending order. If false, sort in descending order.
     */
    public boolean isAscending() {
        return true;
    }

    /**
     * @return true if this column should be visible by default
     */
    public boolean visibleByDefault() {
        return true;
    }

    @Override
    public int compare(IMarker o1, IMarker o2) {
        return getValue(o1).compareTo(getValue(o2));
    }

    // Used for default LabelProvider
    @Override
    public String toString() {
        return getColumnHeaderText();
    }

    static class MessageColumn extends LintColumn {

        public MessageColumn(LintList list) {
            super(list);
        }

        @Override
        public @NonNull String getColumnHeaderText() {
            return "Description";
        }

        @Override
        public int getPreferredCharWidth() {
            return 80;
        }

        @Override
        public String getValue(@NonNull IMarker marker) {
            return getStyledValue(marker).toString();
        }

        @Override
        public StyledString getStyledValue(@NonNull IMarker marker) {
            StyledString styledString = new StyledString();

            String message = marker.getAttribute(IMarker.MESSAGE, "");
            styledString.append(message);

            int count = mList.getCount(marker);
            if (count > 1) {
                styledString.append(String.format(" (%2$d items)", message, count),
                        StyledString.COUNTER_STYLER);
            }

            return styledString;
        }

        @Override
        public Image getImage(@NonNull IMarker marker) {
            int severity = marker.getAttribute(IMarker.SEVERITY, 0);
            ISharedImages sharedImages = PlatformUI.getWorkbench().getSharedImages();
            switch (severity) {
                case IMarker.SEVERITY_ERROR:
                    if (LintFix.hasFix(EclipseLintClient.getId(marker))) {
                        return IconFactory.getInstance().getIcon("quickfix_error");   //$NON-NLS-1$
                    }
                    return sharedImages.getImage(ISharedImages.IMG_OBJS_ERROR_TSK);
                case IMarker.SEVERITY_WARNING:
                    if (LintFix.hasFix(EclipseLintClient.getId(marker))) {
                        return IconFactory.getInstance().getIcon("quickfix_warning"); //$NON-NLS-1$
                    }
                    return sharedImages.getImage(ISharedImages.IMG_OBJS_WARN_TSK);
                case IMarker.SEVERITY_INFO:
                    return sharedImages.getImage(ISharedImages.IMG_OBJS_INFO_TSK);
                default:
                    return null;
            }
        }

        @Override
        public Font getFont(@NonNull IMarker marker) {
            int severity = marker.getAttribute(IMarker.SEVERITY, 0);
            if (severity == IMarker.SEVERITY_ERROR) {
                return JFaceResources.getFontRegistry().getBold(
                        JFaceResources.DEFAULT_FONT);
            }

            return null;
        }

        @Override
        public boolean isAscending() {
            return false;
        }

        @Override
        public int compare(IMarker marker2, IMarker marker1) {
            // Reversing order marker1/marker2 here since we want to use a "descending" column
            // sorting marker to indicate priority. (Note that we return from isAscending too.)

            String id1 = EclipseLintClient.getId(marker1);
            String id2 = EclipseLintClient.getId(marker2);
            if (id1 == null || id2 == null) {
                return marker1.getResource().getName().compareTo(
                        marker2.getResource().getName());
            }
            Issue issue1 = mList.getIssue(id1);
            Issue issue2 = mList.getIssue(id2);
            if (issue1 == null || issue2 == null) {
                // Unknown issue? Can happen if you have used a third party detector
                // which is no longer available but which left a persistent marker behind
                return id1.compareTo(id2);
            }
            int delta = mList.getSeverity(issue1).ordinal() -
                    mList.getSeverity(issue2).ordinal();
            if (delta != 0) {
                return delta;
            }
            delta = issue2.getPriority() - issue1.getPriority();
            if (delta != 0) {
                return delta;
            }
            delta = issue1.getCategory().compareTo(issue2.getCategory());
            if (delta != 0) {
                return delta;
            }
            delta = id1.compareTo(id2);
            if (delta != 0) {
                return delta;
            }

            IResource resource1 = marker1.getResource();
            IResource resource2 = marker2.getResource();

            IProject project1 = resource1.getProject();
            IProject project2 = resource2.getProject();
            delta = project1.getName().compareTo(project2.getName());
            if (delta != 0) {
                return delta;
            }

            delta = resource1.getName().compareTo(resource2.getName());
            if (delta != 0) {
                return delta;
            }

            return marker1.getAttribute(IMarker.LINE_NUMBER, 0)
                    - marker2.getAttribute(IMarker.LINE_NUMBER, 0);
        }
    }

    static class CategoryColumn extends LintColumn {

        public CategoryColumn(LintList list) {
            super(list);
        }

        @Override
        public @NonNull String getColumnHeaderText() {
            return "Category";
        }

        @Override
        public int getPreferredCharWidth() {
            return 20;
        }

        @Override
        public String getValue(@NonNull IMarker marker) {
            Issue issue = mList.getIssue(marker);
            if (issue != null) {
                return issue.getCategory().getFullName();
            } else {
                return "";
            }
        }
    }

    static class LocationColumn extends LintColumn {
        public LocationColumn(LintList list) {
            super(list);
        }

        @Override
        public @NonNull String getColumnHeaderText() {
            return "Location";
        }

        @Override
        public int getPreferredCharWidth() {
            return 35;
        }

        @Override
        public String getValue(@NonNull IMarker marker) {
            return getStyledValue(marker).toString();
        }

        @Override
        public StyledString getStyledValue(@NonNull IMarker marker) {
            StyledString styledString = new StyledString();

            // Combined location
            IResource resource = marker.getResource();
            if (resource instanceof IProject) {
                styledString.append(resource.getName());
            } else {
                // Show location as Parent/File:Line in Project
                styledString.append(resource.getName());
                if (resource instanceof IFile) {
                    int line = marker.getAttribute(IMarker.LINE_NUMBER, -1);
                    if (line > 1) {
                        styledString.append(':').append(Integer.toString(line));
                    }
                } else if (resource instanceof IFolder) {
                    styledString.append(File.separatorChar);
                }

                if (!(resource.getParent() instanceof IProject)) {
                    styledString.append(" in ");
                    styledString.append(resource.getParent().getName(),
                            StyledString.DECORATIONS_STYLER);
                }

                styledString.append(String.format(" (%1$s)", resource.getProject().getName()),
                        StyledString.QUALIFIER_STYLER);
            }

            return styledString;
        }

        @Override
        public int compare(IMarker marker1, IMarker marker2) {
            IResource resource1 = marker1.getResource();
            IResource resource2 = marker2.getResource();

            IProject project1 = resource1.getProject();
            IProject project2 = resource2.getProject();
            int delta = project1.getName().compareTo(project2.getName());
            if (delta != 0) {
                return delta;
            }

            delta = resource1.getName().compareTo(resource2.getName());
            if (delta != 0) {
                return delta;
            }

            return marker1.getAttribute(IMarker.LINE_NUMBER, 0)
                    - marker2.getAttribute(IMarker.LINE_NUMBER, 0);
        }
    }

    static class FileColumn extends LintColumn {
        public FileColumn(LintList list) {
            super(list);
        }

        @Override
        public @NonNull String getColumnHeaderText() {
            return "File";
        }

        @Override
        public boolean visibleByDefault() {
            return false;
        }

        @Override
        public int getPreferredCharWidth() {
            return 12;
        }

        @Override
        public String getValue(@NonNull IMarker marker) {
            if (marker.getResource() instanceof IFile) {
                return marker.getResource().getName();
            } else {
                return "";
            }
        }
    }

    static class PathColumn extends LintColumn {
        public PathColumn(LintList list) {
            super(list);
        }

        @Override
        public @NonNull String getColumnHeaderText() {
            return "Path";
        }

        @Override
        public boolean visibleByDefault() {
            return false;
        }

        @Override
        public int getPreferredCharWidth() {
            return 25;
        }

        @Override
        public String getValue(@NonNull IMarker marker) {
            return marker.getResource().getFullPath().toOSString();
        }
    }

    static class LineColumn extends LintColumn {
        public LineColumn(LintList list) {
            super(list);
        }

        @Override
        public @NonNull String getColumnHeaderText() {
            return "Line";
        }

        @Override
        public boolean visibleByDefault() {
            return false;
        }

        @Override
        public boolean isLeftAligned() {
            return false;
        }

        @Override
        public int getPreferredCharWidth() {
            return 4;
        }

        @Override
        public String getValue(@NonNull IMarker marker) {
            int line = getLine(marker);
            if (line >= 1) {
                return Integer.toString(line);
            } else {
                return "";
            }
        }

        private int getLine(IMarker marker) {
            if (marker.getResource() instanceof IFile) {
                int line = marker.getAttribute(IMarker.LINE_NUMBER, -1);
                return line;
            }

            return -1;
        }
        @Override
        public int compare(IMarker marker1, IMarker marker2) {
            return getLine(marker1) - getLine(marker2);
        }
    }

    static class PriorityColumn extends LintColumn {
        public PriorityColumn(LintList list) {
            super(list);
        }

        @Override
        public @NonNull String getColumnHeaderText() {
            return "Priority";
        }

        @Override
        public boolean visibleByDefault() {
            return false;
        }

        @Override
        public boolean isLeftAligned() {
            return false;
        }

        @Override
        public int getPreferredCharWidth() {
            return 2;
        }

        @Override
        public String getValue(@NonNull IMarker marker) {
            int priority = getPriority(marker);
            if (priority > 0) {
                return Integer.toString(priority);
            }
            return "";
        }

        private int getPriority(IMarker marker) {
            Issue issue = mList.getIssue(marker);
            if (issue != null) {
                return issue.getPriority();
            }
            return 0;
        }

        @Override
        public int compare(IMarker marker1, IMarker marker2) {
            return getPriority(marker1) - getPriority(marker2);
        }

        @Override
        public boolean isAscending() {
            return false;
        }
    }
}
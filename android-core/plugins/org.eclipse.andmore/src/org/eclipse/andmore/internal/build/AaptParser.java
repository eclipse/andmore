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

package org.eclipse.andmore.internal.build;

import org.eclipse.andmore.AdtConstants;
import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.project.BaseProjectHelper;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.FindReplaceDocumentAdapter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;
import org.eclipse.ui.texteditor.IDocumentProvider;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AaptParser {

    // TODO: rename the pattern to something that makes sense + javadoc comments.
    /**
     * Single line aapt warning for skipping files.<br>
     * "  (skipping hidden file '&lt;file path&gt;'"
     */
    private final static Pattern sPattern0Line1 = Pattern.compile(
            "^\\s+\\(skipping hidden file\\s'(.*)'\\)$"); //$NON-NLS-1$

    /**
     * First line of dual line aapt error.<br>
     * "ERROR at line &lt;line&gt;: &lt;error&gt;"<br>
     * " (Occurred while parsing &lt;path&gt;)"
     */
    private final static Pattern sPattern1Line1 = Pattern.compile(
            "^ERROR\\s+at\\s+line\\s+(\\d+):\\s+(.*)$"); //$NON-NLS-1$
    /**
     * Second line of dual line aapt error.<br>
     * "ERROR at line &lt;line&gt;: &lt;error&gt;"<br>
     * " (Occurred while parsing &lt;path&gt;)"<br>
     * @see #sPattern1Line1
     */
    private final static Pattern sPattern1Line2 = Pattern.compile(
            "^\\s+\\(Occurred while parsing\\s+(.*)\\)$");  //$NON-NLS-1$
    /**
     * First line of dual line aapt error.<br>
     * "ERROR: &lt;error&gt;"<br>
     * "Defined at file &lt;path&gt; line &lt;line&gt;"
     */
    private final static Pattern sPattern2Line1 = Pattern.compile(
            "^ERROR:\\s+(.+)$"); //$NON-NLS-1$
    /**
     * Second line of dual line aapt error.<br>
     * "ERROR: &lt;error&gt;"<br>
     * "Defined at file &lt;path&gt; line &lt;line&gt;"<br>
     * @see #sPattern2Line1
     */
    private final static Pattern sPattern2Line2 = Pattern.compile(
            "Defined\\s+at\\s+file\\s+(.+)\\s+line\\s+(\\d+)"); //$NON-NLS-1$
    /**
     * Single line aapt error<br>
     * "&lt;path&gt; line &lt;line&gt;: &lt;error&gt;"
     */
    private final static Pattern sPattern3Line1 = Pattern.compile(
            "^(.+)\\sline\\s(\\d+):\\s(.+)$"); //$NON-NLS-1$
    /**
     * First line of dual line aapt error.<br>
     * "ERROR parsing XML file &lt;path&gt;"<br>
     * "&lt;error&gt; at line &lt;line&gt;"
     */
    private final static Pattern sPattern4Line1 = Pattern.compile(
            "^Error\\s+parsing\\s+XML\\s+file\\s(.+)$"); //$NON-NLS-1$
    /**
     * Second line of dual line aapt error.<br>
     * "ERROR parsing XML file &lt;path&gt;"<br>
     * "&lt;error&gt; at line &lt;line&gt;"<br>
     * @see #sPattern4Line1
     */
    private final static Pattern sPattern4Line2 = Pattern.compile(
            "^(.+)\\s+at\\s+line\\s+(\\d+)$"); //$NON-NLS-1$

    /**
     * Single line aapt warning<br>
     * "&lt;path&gt;:&lt;line&gt;: &lt;error&gt;"
     */
    private final static Pattern sPattern5Line1 = Pattern.compile(
            "^(.+?):(\\d+):\\s+WARNING:(.+)$"); //$NON-NLS-1$

    /**
     * Single line aapt error<br>
     * "&lt;path&gt;:&lt;line&gt;: &lt;error&gt;"
     */
    private final static Pattern sPattern6Line1 = Pattern.compile(
            "^(.+?):(\\d+):\\s+(.+)$"); //$NON-NLS-1$

    /**
     * 4 line aapt error<br>
     * "ERROR: 9-path image &lt;path&gt; malformed"<br>
     * Line 2 and 3 are taken as-is while line 4 is ignored (it repeats with<br>
     * 'ERROR: failure processing &lt;path&gt;)
     */
    private final static Pattern sPattern7Line1 = Pattern.compile(
            "^ERROR:\\s+9-patch\\s+image\\s+(.+)\\s+malformed\\.$"); //$NON-NLS-1$

    private final static Pattern sPattern8Line1 = Pattern.compile(
            "^(invalid resource directory name): (.*)$"); //$NON-NLS-1$

    /**
     * Portion of the error message which states the context in which the error occurred,
     * such as which property was being processed and what the string value was that
     * caused the error.
     * <p>
     * Example:
     * error: No resource found that matches the given name (at 'text' with value '@string/foo')
     */
    private static final Pattern sValueRangePattern =
        Pattern.compile("\\(at '(.+)' with value '(.*)'\\)"); //$NON-NLS-1$


    /**
     * Portion of error message which points to the second occurrence of a repeated resource
     * definition.
     * <p>
     * Example:
     * error: Resource entry repeatedStyle1 already has bag item android:gravity.
     */
    private static final Pattern sRepeatedRangePattern =
        Pattern.compile("Resource entry (.+) already has bag item (.+)\\."); //$NON-NLS-1$

    /**
     * Error message emitted when aapt skips a file because for example it's name is
     * invalid, such as a layout file name which starts with _.
     * <p>
     * This error message is used by AAPT in Tools 19 and earlier.
     */
    private static final Pattern sSkippingPattern =
        Pattern.compile("    \\(skipping (.+) .+ '(.*)'\\)"); //$NON-NLS-1$

    /**
     * Error message emitted when aapt skips a file because for example it's name is
     * invalid, such as a layout file name which starts with _.
     * <p>
     * This error message is used by AAPT in Tools 20 and later.
     */
    private static final Pattern sNewSkippingPattern =
        Pattern.compile("    \\(skipping .+ '(.+)' due to ANDROID_AAPT_IGNORE pattern '.+'\\)"); //$NON-NLS-1$

    /**
     * Suffix of error message which points to the first occurrence of a repeated resource
     * definition.
     * Example:
     * Originally defined here.
     */
    private static final String ORIGINALLY_DEFINED_MSG = "Originally defined here."; //$NON-NLS-1$

    /**
     * Portion of error message which points to the second occurrence of a repeated resource
     * definition.
     * <p>
     * Example:
     * error: Resource entry repeatedStyle1 already has bag item android:gravity.
     */
    private static final Pattern sNoResourcePattern =
        Pattern.compile("No resource found that matches the given name: attr '(.+)'\\."); //$NON-NLS-1$

    /**
     * Portion of error message which points to a missing required attribute in a
     * resource definition.
     * <p>
     * Example:
     * error: error: A 'name' attribute is required for <style>
     */
    private static final Pattern sRequiredPattern =
        Pattern.compile("A '(.+)' attribute is required for <(.+)>"); //$NON-NLS-1$

    /**
     * 2 line aapt error<br>
     * "ERROR: Invalid configuration: foo"<br>
     * "                              ^^^"<br>
     * There's no need to parse the 2nd line.
     */
    private final static Pattern sPattern9Line1 = Pattern.compile(
            "^Invalid configuration: (.+)$"); //$NON-NLS-1$

    private final static Pattern sXmlBlockPattern = Pattern.compile(
            "W/ResourceType\\(.*\\): Bad XML block: no root element node found"); //$NON-NLS-1$

    /**
     * Parse the output of aapt and mark the incorrect file with error markers
     *
     * @param results the output of aapt
     * @param project the project containing the file to mark
     * @return true if the parsing failed, false if success.
     */
    public static boolean parseOutput(List<String> results, IProject project) {
        int size = results.size();
        if (size > 0) {
            return parseOutput(results.toArray(new String[size]), project);
        }

        return false;
    }

    /**
     * Parse the output of aapt and mark the incorrect file with error markers
     *
     * @param results the output of aapt
     * @param project the project containing the file to mark
     * @return true if the parsing failed, false if success.
     */
    public static boolean parseOutput(String[] results, IProject project) {
        // nothing to parse? just return false;
        if (results.length == 0) {
            return false;
        }

        // get the root of the project so that we can make IFile from full
        // file path
        String osRoot = project.getLocation().toOSString();

        Matcher m;

        for (int i = 0; i < results.length ; i++) {
            String p = results[i];

            m = sPattern0Line1.matcher(p);
            if (m.matches()) {
                // we ignore those (as this is an ignore message from aapt)
                continue;
            }

            m = sPattern1Line1.matcher(p);
            if (m.matches()) {
                String lineStr = m.group(1);
                String msg = m.group(2);

                // get the matcher for the next line.
                m = getNextLineMatcher(results, ++i, sPattern1Line2);
                if (m == null) {
                    return true;
                }

                String location = m.group(1);

                // check the values and attempt to mark the file.
                if (checkAndMark(location, lineStr, msg, osRoot, project,
                        AdtConstants.MARKER_AAPT_COMPILE, IMarker.SEVERITY_ERROR) == false) {
                    return true;
                }
                continue;
            }

            // this needs to be tested before Pattern2 since they both start with 'ERROR:'
            m = sPattern7Line1.matcher(p);
            if (m.matches()) {
                String location = m.group(1);
                String msg = p; // default msg is the line in case we don't find anything else

                if (++i < results.length) {
                    msg = results[i].trim();
                    if (++i < results.length) {
                        msg = msg + " - " + results[i].trim(); //$NON-NLS-1$

                        // skip the next line
                        i++;
                    }
                }

                // display the error
                if (checkAndMark(location, null, msg, osRoot, project,
                        AdtConstants.MARKER_AAPT_COMPILE, IMarker.SEVERITY_ERROR) == false) {
                    return true;
                }

                // success, go to the next line
                continue;
            }

            m =  sPattern2Line1.matcher(p);
            if (m.matches()) {
                // get the msg
                String msg = m.group(1);

                // get the matcher for the next line.
                m = getNextLineMatcher(results, ++i, sPattern2Line2);
                if (m == null) {
                    return true;
                }

                String location = m.group(1);
                String lineStr = m.group(2);

                // check the values and attempt to mark the file.
                if (checkAndMark(location, lineStr, msg, osRoot, project,
                        AdtConstants.MARKER_AAPT_COMPILE, IMarker.SEVERITY_ERROR) == false) {
                    return true;
                }
                continue;
            }

            m = sPattern3Line1.matcher(p);
            if (m.matches()) {
                String location = m.group(1);
                String lineStr = m.group(2);
                String msg = m.group(3);

                // check the values and attempt to mark the file.
                if (checkAndMark(location, lineStr, msg, osRoot, project,
                        AdtConstants.MARKER_AAPT_COMPILE, IMarker.SEVERITY_ERROR) == false) {
                    return true;
                }

                // success, go to the next line
                continue;
            }

            m = sPattern4Line1.matcher(p);
            if (m.matches()) {
                // get the filename.
                String location = m.group(1);

                // get the matcher for the next line.
                m = getNextLineMatcher(results, ++i, sPattern4Line2);
                if (m == null) {
                    return true;
                }

                String msg = m.group(1);
                String lineStr = m.group(2);

                // check the values and attempt to mark the file.
                if (checkAndMark(location, lineStr, msg, osRoot, project,
                        AdtConstants.MARKER_AAPT_COMPILE, IMarker.SEVERITY_ERROR) == false) {
                    return true;
                }

                // success, go to the next line
                continue;
            }

            m = sPattern5Line1.matcher(p);
            if (m.matches()) {
                String location = m.group(1);
                String lineStr = m.group(2);
                String msg = m.group(3);

                // check the values and attempt to mark the file.
                if (checkAndMark(location, lineStr, msg, osRoot, project,
                        AdtConstants.MARKER_AAPT_COMPILE, IMarker.SEVERITY_WARNING) == false) {
                    return true;
                }

                // success, go to the next line
                continue;
            }

            m = sPattern6Line1.matcher(p);
            if (m.matches()) {
                String location = m.group(1);
                String lineStr = m.group(2);
                String msg = m.group(3);

                // check the values and attempt to mark the file.
                if (checkAndMark(location, lineStr, msg, osRoot, project,
                        AdtConstants.MARKER_AAPT_COMPILE, IMarker.SEVERITY_ERROR) == false) {
                    return true;
                }

                // success, go to the next line
                continue;
            }

            m = sPattern8Line1.matcher(p);
            if (m.matches()) {
                String location = m.group(2);
                String msg = m.group(1);

                // check the values and attempt to mark the file.
                if (checkAndMark(location, null, msg, osRoot, project,
                        AdtConstants.MARKER_AAPT_COMPILE, IMarker.SEVERITY_ERROR) == false) {
                    return true;
                }

                // success, go to the next line
                continue;
            }

            m = sPattern9Line1.matcher(p);
            if (m.matches()) {
                String badConfig = m.group(1);
                String msg = String.format("APK Configuration filter '%1$s' is invalid", badConfig);

                // skip the next line
                i++;

                // check the values and attempt to mark the file.
                if (checkAndMark(null /*location*/, null, msg, osRoot, project,
                        AdtConstants.MARKER_AAPT_PACKAGE, IMarker.SEVERITY_ERROR) == false) {
                    return true;
                }

                // success, go to the next line
                continue;
            }

            m = sNewSkippingPattern.matcher(p);
            if (m.matches()) {
                String location = m.group(1);

                if (location.startsWith(".")         //$NON-NLS-1$
                        || location.endsWith("~")) { //$NON-NLS-1$
                    continue;
                }

                // check the values and attempt to mark the file.
                if (checkAndMark(location, null, p.trim(), osRoot, project,
                        AdtConstants.MARKER_AAPT_COMPILE, IMarker.SEVERITY_WARNING) == false) {
                    return true;
                }

                // success, go to the next line
                continue;
            }

            m = sSkippingPattern.matcher(p);
            if (m.matches()) {
                String location = m.group(2);

                // Certain files can safely be skipped without marking the project
                // as having errors. See isHidden() in AaptAssets.cpp:
                String type = m.group(1);
                if (type.equals("backup")          //$NON-NLS-1$   // main.xml~, etc
                        || type.equals("hidden")   //$NON-NLS-1$   // .gitignore, etc
                        || type.equals("index")) { //$NON-NLS-1$   // thumbs.db, etc
                    continue;
                }

                // check the values and attempt to mark the file.
                if (checkAndMark(location, null, p.trim(), osRoot, project,
                        AdtConstants.MARKER_AAPT_COMPILE, IMarker.SEVERITY_WARNING) == false) {
                    return true;
                }

                // success, go to the next line
                continue;
            }

            m = sXmlBlockPattern.matcher(p);
            if (m.matches()) {
                // W/ResourceType(12345): Bad XML block: no root element node found
                // Sadly there's NO filename reference; this error typically describes the
                // error *after* this line.
                if (results.length == 1) {
                    // This is the only error message: dump to console and quit
                    return true;
                }
                // Continue: the real culprit is displayed next and should get a marker
                continue;
            }

            return true;
        }

        return false;
    }

    /**
     * Check if the parameters gotten from the error output are valid, and mark
     * the file with an AAPT marker.
     * @param location the full OS path of the error file. If null, the project is marked
     * @param lineStr
     * @param message
     * @param root The root directory of the project, in OS specific format.
     * @param project
     * @param markerId The marker id to put.
     * @param severity The severity of the marker to put (IMarker.SEVERITY_*)
     * @return true if the parameters were valid and the file was marked successfully.
     *
     * @see IMarker
     */
    private static final  boolean checkAndMark(String location, String lineStr,
            String message, String root, IProject project, String markerId, int severity) {
        // check this is in fact a file
        if (location != null) {
            File f = new File(location);
            if (f.exists() == false) {
                return false;
            }
        }

        // get the line number
        int line = -1; // default value for error with no line.

        if (lineStr != null) {
            try {
                line = Integer.parseInt(lineStr);
            } catch (NumberFormatException e) {
                // looks like the string we extracted wasn't a valid
                // file number. Parsing failed and we return true
                return false;
            }
        }

        // add the marker
        IResource f2 = project;
        if (location != null) {
            f2 = getResourceFromFullPath(location, root, project);
            if (f2 == null) {
                return false;
            }
        }

        // Attempt to determine the exact range of characters affected by this error.
        // This will look up the actual text of the file, go to the particular error line
        // and scan for the specific string mentioned in the error.
        int startOffset = -1;
        int endOffset = -1;
        if (f2 instanceof IFile) {
            IRegion region = findRange((IFile) f2, line, message);
            if (region != null) {
                startOffset = region.getOffset();
                endOffset = startOffset + region.getLength();
            }
        }

        // check if there's a similar marker already, since aapt is launched twice
        boolean markerAlreadyExists = false;
        try {
            IMarker[] markers = f2.findMarkers(markerId, true, IResource.DEPTH_ZERO);

            for (IMarker marker : markers) {
                if (startOffset != -1) {
                    int tmpBegin = marker.getAttribute(IMarker.CHAR_START, -1);
                    if (tmpBegin != startOffset) {
                        break;
                    }
                    int tmpEnd = marker.getAttribute(IMarker.CHAR_END, -1);
                    if (tmpEnd != startOffset) {
                        break;
                    }
                }

                int tmpLine = marker.getAttribute(IMarker.LINE_NUMBER, -1);
                if (tmpLine != line) {
                    break;
                }

                int tmpSeverity = marker.getAttribute(IMarker.SEVERITY, -1);
                if (tmpSeverity != severity) {
                    break;
                }

                String tmpMsg = marker.getAttribute(IMarker.MESSAGE, null);
                if (tmpMsg == null || tmpMsg.equals(message) == false) {
                    break;
                }

                // if we're here, all the marker attributes are equals, we found it
                // and exit
                markerAlreadyExists = true;
                break;
            }

        } catch (CoreException e) {
            // if we couldn't get the markers, then we just mark the file again
            // (since markerAlreadyExists is initialized to false, we do nothing)
        }

        if (markerAlreadyExists == false) {
            BaseProjectHelper.markResource(f2, markerId, message, line,
                    startOffset, endOffset, severity);
        }

        return true;
    }

    /**
     * Given an aapt error message in a given file and a given (initial) line number,
     * return the corresponding offset range for the error, or null.
     */
    private static IRegion findRange(IFile file, int line, String message) {
        Matcher matcher = sValueRangePattern.matcher(message);
        if (matcher.find()) {
            String property = matcher.group(1);
            String value = matcher.group(2);

            // First find the property. We can't just immediately look for the
            // value, because there could be other attributes in this element
            // earlier than the one in error, and we might accidentally pick
            // up on a different occurrence of the value in a context where
            // it is valid.
            if (value.length() > 0) {
                return findRange(file, line, property, value);
            } else {
                // Find first occurrence of property followed by '' or ""
                IRegion region1 = findRange(file, line, property, "\"\""); //$NON-NLS-1$
                IRegion region2 = findRange(file, line, property, "''");   //$NON-NLS-1$
                if (region1 == null) {
                    if (region2 == null) {
                        // Highlight the property instead
                        return findRange(file, line, property, null);
                    }
                    return region2;
                } else if (region2 == null) {
                    return region1;
                } else if (region1.getOffset() < region2.getOffset()) {
                    return region1;
                } else {
                    return region2;
                }
            }
        }

        matcher = sRepeatedRangePattern.matcher(message);
        if (matcher.find()) {
            String property = matcher.group(2);
            return findRange(file, line, property, null);
        }

        matcher = sNoResourcePattern.matcher(message);
        if (matcher.find()) {
            String property = matcher.group(1);
            return findRange(file, line, property, null);
        }

        matcher = sRequiredPattern.matcher(message);
        if (matcher.find()) {
            String elementName = matcher.group(2);
            IRegion region = findRange(file, line, '<' + elementName, null);
            if (region != null && region.getLength() > 1) {
                // Skip the opening <
                region = new Region(region.getOffset() + 1, region.getLength() - 1);
            }
            return region;
        }

        if (message.endsWith(ORIGINALLY_DEFINED_MSG)) {
            return findLineTextRange(file, line);
        }

        return null;
    }

    /**
     * Given a file and line number, return the range of the first match starting on the
     * given line. If second is non null, also search for the second string starting at he
     * location of the first string.
     */
    private static IRegion findRange(IFile file, int line, String first,
            String second) {
        IRegion region = null;
        IDocumentProvider provider = new TextFileDocumentProvider();
        try {
            provider.connect(file);
            IDocument document = provider.getDocument(file);
            if (document != null) {
                IRegion lineInfo = document.getLineInformation(line - 1);
                int lineStartOffset = lineInfo.getOffset();
                // The aapt errors will be anchored on the line where the
                // element starts - which means that with formatting where
                // attributes end up on subsequent lines we don't find it on
                // the error line indicated by aapt.
                // Therefore, search forwards in the document.
                FindReplaceDocumentAdapter adapter =
                    new FindReplaceDocumentAdapter(document);

                region = adapter.find(lineStartOffset, first,
                        true /*forwardSearch*/, true /*caseSensitive*/,
                        false /*wholeWord*/, false /*regExSearch*/);
                if (region != null && second != null) {
                    region = adapter.find(region.getOffset() + first.length(), second,
                            true /*forwardSearch*/, true /*caseSensitive*/,
                            false /*wholeWord*/, false /*regExSearch*/);
                }
            }
        } catch (Exception e) {
            AdtPlugin.log(e, "Can't find range information for %1$s", file.getName());
        } finally {
            provider.disconnect(file);
        }
        return region;
    }

    /** Returns the non-whitespace line range at the given line number. */
    private static IRegion findLineTextRange(IFile file, int line) {
        IDocumentProvider provider = new TextFileDocumentProvider();
        try {
            provider.connect(file);
            IDocument document = provider.getDocument(file);
            if (document != null) {
                IRegion lineInfo = document.getLineInformation(line - 1);
                String lineContents = document.get(lineInfo.getOffset(), lineInfo.getLength());
                int lineBegin = 0;
                int lineEnd = lineContents.length()-1;

                for (; lineEnd >= 0; lineEnd--) {
                    char c = lineContents.charAt(lineEnd);
                    if (!Character.isWhitespace(c)) {
                        break;
                    }
                }
                lineEnd++;
                for (; lineBegin < lineEnd; lineBegin++) {
                    char c = lineContents.charAt(lineBegin);
                    if (!Character.isWhitespace(c)) {
                        break;
                    }
                }
                if (lineBegin < lineEnd) {
                    return new Region(lineInfo.getOffset() + lineBegin, lineEnd - lineBegin);
                }
            }
        } catch (Exception e) {
            AdtPlugin.log(e, "Can't find range information for %1$s", file.getName());
        } finally {
            provider.disconnect(file);
        }

        return null;
    }

    /**
     * Returns a matching matcher for the next line
     * @param lines The array of lines
     * @param nextIndex The index of the next line
     * @param pattern The pattern to match
     * @return null if error or no match, the matcher otherwise.
     */
    private static final Matcher getNextLineMatcher(String[] lines,
            int nextIndex, Pattern pattern) {
        // unless we can't, because we reached the last line
        if (nextIndex == lines.length) {
            // we expected a 2nd line, so we flag as error
            // and we bail
            return null;
        }

        Matcher m = pattern.matcher(lines[nextIndex]);
        if (m.matches()) {
           return m;
        }

        return null;
    }

    private static IResource getResourceFromFullPath(String filename, String root,
            IProject project) {
        if (filename.startsWith(root)) {
            String file = filename.substring(root.length());

            // get the resource
            IResource r = project.findMember(file);

            // if the resource is valid, we add the marker
            if (r != null && r.exists()) {
                return r;
            }
        }

        return null;
    }

}

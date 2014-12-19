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

package org.eclipse.andmore.internal.project;

import com.android.ide.common.xml.AndroidManifestParser;
import com.android.ide.common.xml.ManifestData;
import com.android.io.FileWrapper;
import com.android.io.IAbstractFile;
import com.android.io.StreamException;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.project.XmlErrorHandler.XmlErrorListener;
import org.eclipse.andmore.io.IFileWrapper;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.xml.sax.SAXException;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

public class AndroidManifestHelper {

    /**
     * Parses the Android Manifest, and returns an object containing the result of the parsing.
     * <p/>
     * This method can also gather XML error during the parsing. This is done by using an
     * {@link XmlErrorHandler} to mark the files in case of error, as well as a given
     * {@link XmlErrorListener}. To use a different error handler, consider using
     * {@link AndroidManifestParser#parse(IAbstractFile, boolean, com.android.sdklib.xml.AndroidManifestParser.ManifestErrorHandler)}
     * directly.
     *
     * @param manifestFile the {@link IFile} representing the manifest file.
     * @param gatherData indicates whether the parsing will extract data from the manifest. If null,
     * the method will always return null.
     * @param errorListener an optional error listener. If non null, then the parser will also
     * look for XML errors.
     * @return an {@link ManifestData} or null if the parsing failed.
     * @throws ParserConfigurationException
     * @throws StreamException
     * @throws IOException
     * @throws SAXException
     */
    public static ManifestData parseUnchecked(
            IAbstractFile manifestFile,
            boolean gatherData,
            XmlErrorListener errorListener) throws SAXException, IOException,
            StreamException, ParserConfigurationException {
        if (manifestFile != null) {
            IFile eclipseFile = null;
            if (manifestFile instanceof IFileWrapper) {
                eclipseFile = ((IFileWrapper)manifestFile).getIFile();
            }
            XmlErrorHandler errorHandler = null;
            if (errorListener != null) {
                errorHandler = new XmlErrorHandler(eclipseFile, errorListener);
            }

            return AndroidManifestParser.parse(manifestFile, gatherData, errorHandler);
        }

        return null;
    }

    /**
     * Parses the Android Manifest, and returns an object containing the result of the parsing.
     * <p/>
     * This method can also gather XML error during the parsing. This is done by using an
     * {@link XmlErrorHandler} to mark the files in case of error, as well as a given
     * {@link XmlErrorListener}. To use a different error handler, consider using
     * {@link AndroidManifestParser#parse(IAbstractFile, boolean, com.android.sdklib.xml.AndroidManifestParser.ManifestErrorHandler)}
     * directly.
     *
     * @param manifestFile the {@link IFile} representing the manifest file.
     * @param gatherData indicates whether the parsing will extract data from the manifest. If null,
     * the method will always return null.
     * @param errorListener an optional error listener. If non null, then the parser will also
     * look for XML errors.
     * @return an {@link ManifestData} or null if the parsing failed.
     */
    public static ManifestData parse(
            IAbstractFile manifestFile,
            boolean gatherData,
            XmlErrorListener errorListener) {
        try {
            return parseUnchecked(manifestFile, gatherData, errorListener);
        } catch (ParserConfigurationException e) {
            AdtPlugin.logAndPrintError(e, AndroidManifestHelper.class.getCanonicalName(),
                    "Bad parser configuration for %s: %s",
                    manifestFile.getOsLocation(),
                    e.getMessage());
        } catch (SAXException e) {
            AdtPlugin.logAndPrintError(e, AndroidManifestHelper.class.getCanonicalName(),
                    "Parser exception for %s: %s",
                    manifestFile.getOsLocation(),
                    e.getMessage());
        } catch (IOException e) {
            // Don't log a console error when failing to read a non-existing file
            if (!(e instanceof FileNotFoundException)) {
                AdtPlugin.logAndPrintError(e, AndroidManifestHelper.class.getCanonicalName(),
                        "I/O error for %s: %s",
                        manifestFile.getOsLocation(),
                        e.getMessage());
            }
        } catch (StreamException e) {
            AdtPlugin.logAndPrintError(e, AndroidManifestHelper.class.getCanonicalName(),
                    "Unable to read %s: %s",
                    manifestFile.getOsLocation(),
                    e.getMessage());
        }

        return null;
    }

    /**
     * Parses the Android Manifest for a given project, and returns an object containing
     * the result of the parsing.
     * <p/>
     * This method can also gather XML error during the parsing. This is done by using an
     * {@link XmlErrorHandler} to mark the files in case of error, as well as a given
     * {@link XmlErrorListener}. To use a different error handler, consider using
     * {@link AndroidManifestParser#parse(IAbstractFile, boolean, com.android.sdklib.xml.AndroidManifestParser.ManifestErrorHandler)}
     * directly.
     *
     * @param javaProject the project containing the manifest to parse.
     * @param gatherData indicates whether the parsing will extract data from the manifest. If null,
     * the method will always return null.
     * @param errorListener an optional error listener. If non null, then the parser will also
     * look for XML errors.
     * @return an {@link ManifestData} or null if the parsing failed.
     */
    public static ManifestData parse(
            IJavaProject javaProject,
            boolean gatherData,
            XmlErrorListener errorListener) {

        IFile manifestFile = ProjectHelper.getManifest(javaProject.getProject());
        if (manifestFile != null) {
            return parse(new IFileWrapper(manifestFile), gatherData, errorListener);
        }

        return null;
    }

    /**
     * Parses the manifest file only for error check.
     * @param manifestFile The manifest file to parse.
     * @param errorListener the {@link XmlErrorListener} object being notified of the presence
     * of errors.
     */
    public static void parseForError(IFile manifestFile, XmlErrorListener errorListener) {
        parse(new IFileWrapper(manifestFile), false, errorListener);
    }

    /**
     * Parses the manifest file, and collects data.
     * @param manifestFile The manifest file to parse.
     * @return an {@link ManifestData} or null if the parsing failed.
     */
    public static ManifestData parseForData(IFile manifestFile) {
        return parse(new IFileWrapper(manifestFile), true, null);
    }

    /**
     * Parses the manifest file, and collects data.
     * @param project the project containing the manifest.
     * @return an {@link AndroidManifestHelper} or null if the parsing failed.
     */
    public static ManifestData parseForData(IProject project) {
        IFile manifestFile = ProjectHelper.getManifest(project);
        if (manifestFile != null) {
            return parse(new IFileWrapper(manifestFile), true, null);
        }

        return null;
    }

    /**
     * Parses the manifest file, and collects data.
     *
     * @param osManifestFilePath The OS path of the manifest file to parse.
     * @return an {@link AndroidManifestHelper} or null if the parsing failed.
     */
    public static ManifestData parseForData(String osManifestFilePath) {
        return parse(new FileWrapper(osManifestFilePath), true, null);
    }
}

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

import com.android.ide.common.xml.AndroidManifestParser.ManifestErrorHandler;

import org.eclipse.andmore.AdtConstants;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * XML error handler used by the parser to report errors/warnings.
 */
public class XmlErrorHandler extends DefaultHandler implements ManifestErrorHandler {

    private final IJavaProject mJavaProject;
    /** file being parsed */
    private final IFile mFile;
    /** link to the delta visitor, to set the xml error flag */
    private final XmlErrorListener mErrorListener;

    /**
     * Classes which implement this interface provide a method that deals
     * with XML errors.
     */
    public interface XmlErrorListener {
        /**
         * Sent when an XML error is detected.
         */
        public void errorFound();
    }

    public static class BasicXmlErrorListener implements XmlErrorListener {
        public boolean mHasXmlError = false;

        @Override
        public void errorFound() {
            mHasXmlError = true;
        }
    }

    public XmlErrorHandler(IJavaProject javaProject, IFile file, XmlErrorListener errorListener) {
        mJavaProject = javaProject;
        mFile = file;
        mErrorListener = errorListener;
    }

    public XmlErrorHandler(IFile file, XmlErrorListener errorListener) {
        this(null, file, errorListener);
    }

    /**
     * Xml Error call back
     * @param exception the parsing exception
     * @throws SAXException
     */
    @Override
    public void error(SAXParseException exception) throws SAXException {
        handleError(exception, exception.getLineNumber());
    }

    /**
     * Xml Fatal Error call back
     * @param exception the parsing exception
     * @throws SAXException
     */
    @Override
    public void fatalError(SAXParseException exception) throws SAXException {
        handleError(exception, exception.getLineNumber());
    }

    /**
     * Xml Warning call back
     * @param exception the parsing exception
     * @throws SAXException
     */
    @Override
    public void warning(SAXParseException exception) throws SAXException {
        if (mFile != null) {
            BaseProjectHelper.markResource(mFile,
                    AdtConstants.MARKER_XML,
                    exception.getMessage(),
                    exception.getLineNumber(),
                    IMarker.SEVERITY_WARNING);
        }
    }

    protected final IFile getFile() {
        return mFile;
    }

    /**
     * Handles a parsing error and an optional line number.
     * @param exception
     * @param lineNumber
     */
    @Override
    public void handleError(Exception exception, int lineNumber) {
        if (mErrorListener != null) {
            mErrorListener.errorFound();
        }

        String message = exception.getMessage();
        if (message == null) {
            message = "Unknown error " + exception.getClass().getCanonicalName();
        }

        if (mFile != null) {
            BaseProjectHelper.markResource(mFile,
                    AdtConstants.MARKER_XML,
                    message,
                    lineNumber,
                    IMarker.SEVERITY_ERROR);
        }
    }

    /**
     * Checks that a class is valid and can be used in the Android Manifest.
     * <p/>
     * Errors are put as {@link IMarker} on the manifest file.
     * @param locator
     * @param className the fully qualified name of the class to test.
     * @param superClassName the fully qualified name of the class it is supposed to extend.
     * @param testVisibility if <code>true</code>, the method will check the visibility of
     * the class or of its constructors.
     */
    @Override
    public void checkClass(Locator locator, String className, String superClassName,
            boolean testVisibility) {
        if (mJavaProject == null) {
            return;
        }
        // we need to check the validity of the activity.
        String result = BaseProjectHelper.testClassForManifest(mJavaProject,
                className, superClassName, testVisibility);
        if (result != BaseProjectHelper.TEST_CLASS_OK) {
            // get the line number
            int line = locator.getLineNumber();

            // mark the file
            IMarker marker = BaseProjectHelper.markResource(getFile(),
                    AdtConstants.MARKER_ANDROID, result, line, IMarker.SEVERITY_ERROR);

            // add custom attributes to be used by the manifest editor.
            if (marker != null) {
                try {
                    marker.setAttribute(AdtConstants.MARKER_ATTR_TYPE,
                            AdtConstants.MARKER_ATTR_TYPE_ACTIVITY);
                    marker.setAttribute(AdtConstants.MARKER_ATTR_CLASS, className);
                } catch (CoreException e) {
                }
            }
        }
    }
}

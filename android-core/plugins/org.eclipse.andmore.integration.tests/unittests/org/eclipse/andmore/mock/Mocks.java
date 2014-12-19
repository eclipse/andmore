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

package org.eclipse.andmore.mock;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;

import com.android.io.IAbstractFolder;
import com.android.io.IAbstractResource;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import java.util.Map;

public class Mocks {
    public static IJavaProject createProject(IClasspathEntry[] entries, IPath outputLocation)
            throws Exception {
        IJavaProject javaProject = createMock(IJavaProject.class);
        final Capture<IClasspathEntry[]> capturedEntries = new Capture<IClasspathEntry[]>();
        Capture<IPath> capturedOutput = new Capture<IPath>();
        capturedEntries.setValue(entries);
        capturedOutput.setValue(outputLocation);

        IProject project = createProject();
        expect(javaProject.getProject()).andReturn(project).anyTimes();
        expect(javaProject.getOutputLocation()).andReturn(capturedOutput.getValue()).anyTimes();

        expect(javaProject.getRawClasspath()).andAnswer(new IAnswer<IClasspathEntry[]>() {
            @Override
            public IClasspathEntry[] answer() throws Throwable {
                return capturedEntries.getValue();
            }
        }).anyTimes();

        javaProject.setRawClasspath(capture(capturedEntries), isA(IProgressMonitor.class));
        expectLastCall().anyTimes();

        javaProject.setRawClasspath(capture(capturedEntries), capture(capturedOutput),
                isA(IProgressMonitor.class));
        expectLastCall().anyTimes();

        final Capture<String> capturedCompliance = new Capture<String>();
        capturedCompliance.setValue("1.4");
        final Capture<String> capturedSource = new Capture<String>();
        capturedSource.setValue("1.4");
        final Capture<String> capturedTarget = new Capture<String>();
        capturedTarget.setValue("1.4");

        expect(javaProject.getOption(JavaCore.COMPILER_COMPLIANCE, true)).andAnswer(
                new IAnswer<String>() {
                    @Override
                    public String answer() throws Throwable {
                        return capturedCompliance.getValue();
                    }
                });
        expect(javaProject.getOption(JavaCore.COMPILER_SOURCE, true)).andAnswer(
                new IAnswer<String>() {
                    @Override
                    public String answer() throws Throwable {
                        return capturedSource.getValue();
                    }
                });
        expect(javaProject.getOption(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, true)).andAnswer(
                new IAnswer<String>() {
                    @Override
                    public String answer() throws Throwable {
                        return capturedTarget.getValue();
                    }
                });

        javaProject.setOption(eq(JavaCore.COMPILER_COMPLIANCE), capture(capturedCompliance));
        expectLastCall().anyTimes();
        javaProject.setOption(eq(JavaCore.COMPILER_SOURCE), capture(capturedSource));
        expectLastCall().anyTimes();
        javaProject.setOption(eq(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM),
                capture(capturedTarget));
        expectLastCall().anyTimes();

        replay(javaProject);

        return javaProject;
    }

    /**
     * Creates a mock implementation of {@link IFile}.
     * <p/>
     * Supported methods:
     * <ul>
     * <li>IFile#getName()</li>
     * <li>IFile#getLocation()</li>
     * </ul>
     */
    public static IFile createFile(String fileName) {
        IFile file = createNiceMock(IFile.class);
        expect(file.getName()).andReturn(fileName).anyTimes();
        expect(file.getLocation()).andReturn(new Path(fileName)).anyTimes();
        replay(file);
        return file;
    }

    /**
     * Creates a mock implementation of {@link IFolder}.
     * <p/>
     * Supported methods:
     * <ul>
     * <li>{@link IFolder#getName()}</li>
     * <li>{@link IFolder#members()}</li>
     * </ul>
     */
    public static IFolder createFolder(String name, IResource[] members) throws Exception {
        IFolder file = createNiceMock(IFolder.class);
        expect(file.getName()).andReturn(name).anyTimes();
        // expect(file.getLocation()).andReturn(new Path(name)).anyTimes();
        expect(file.members()).andReturn(members).anyTimes();
        replay(file);
        return file;
    }

    public static IAbstractFolder createAbstractFolder(String name, IAbstractResource[] members) {
        IAbstractFolder folder = createNiceMock(IAbstractFolder.class);
        expect(folder.getName()).andReturn(name).anyTimes();
        // expect(file.getLocation()).andReturn(new Path(name)).anyTimes();
        expect(folder.listMembers()).andReturn(members).anyTimes();
        replay(folder);

        return folder;
    }

    /**
     * Mock implementation of {@link IProject}.
     * <p/>
     * Supported methods:
     * <ul>
     * <li>{@link IProject#build(int kind, IProgressMonitor monitor)}</li>
     * <li>
     * {@link IProject#build(int kind, String builderName, Map args, IProgressMonitor monitor)}
     * </li>
     * </ul>
     */
    public static IProject createProject() {
        IProject project = EasyMock.createNiceMock(IProject.class);
        replay(project);
        return project;
    }

    /**
     * Creates a mock implementation of an {@link IClasspathEntry}, which supports
     * {@link IClasspathEntry#getEntryKind} and {@link IClasspathEntry#getPath}.
     */
    public static IClasspathEntry createClasspathEntry(IPath path, int kind) {
        IClasspathEntry entry = createNiceMock(IClasspathEntry.class);
        expect(entry.getEntryKind()).andReturn(kind).anyTimes();
        expect(entry.getPath()).andReturn(path).anyTimes();
        replay(entry);
        return entry;
    }
}

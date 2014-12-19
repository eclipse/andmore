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

package org.eclipse.andmore.internal.resources.manager;

import com.android.SdkConstants;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.build.BuildHelper;
import org.eclipse.andmore.internal.sdk.ProjectState;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * ClassLoader able to load class from output of an Eclipse project.
 */
public final class ProjectClassLoader extends ClassLoader {

    private final IJavaProject mJavaProject;
    private URLClassLoader mJarClassLoader;
    private boolean mInsideJarClassLoader = false;

    public ProjectClassLoader(ClassLoader parentClassLoader, IProject project) {
        super(parentClassLoader);
        mJavaProject = JavaCore.create(project);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        // if we are here through a child classloader, throw an exception.
        if (mInsideJarClassLoader) {
            throw new ClassNotFoundException(name);
        }

        // attempt to load the class from the main project
        Class<?> clazz = loadFromProject(mJavaProject, name);

        if (clazz != null) {
            return clazz;
        }

        // attempt to load the class from the jar dependencies
        clazz = loadClassFromJar(name);
        if (clazz != null) {
            return clazz;
        }

        // attempt to load the class from the libraries
        try {
            // get the project info
            ProjectState projectState = Sdk.getProjectState(mJavaProject.getProject());

            // this can happen if the project has no project.properties.
            if (projectState != null) {

                List<IProject> libProjects = projectState.getFullLibraryProjects();
                List<IJavaProject> referencedJavaProjects = BuildHelper.getJavaProjects(
                        libProjects);

                for (IJavaProject javaProject : referencedJavaProjects) {
                    clazz = loadFromProject(javaProject, name);

                    if (clazz != null) {
                        return clazz;
                    }
                }
            }
        } catch (CoreException e) {
            // log exception?
        }

        throw new ClassNotFoundException(name);
    }

    /**
     * Attempts to load a class from a project output folder.
     * @param project the project to load the class from
     * @param name the name of the class
     * @return a class object if found, null otherwise.
     */
    private Class<?> loadFromProject(IJavaProject project, String name) {
        try {
            // get the project output folder.
            IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
            IPath outputLocation = project.getOutputLocation();
            IResource outRes = root.findMember(outputLocation);
            if (outRes == null) {
                return null;
            }

            File outFolder = new File(outRes.getLocation().toOSString());

            // get the class name segments
            String[] segments = name.split("\\."); //$NON-NLS-1$

            // try to load the class from the bin folder of the project.
            File classFile = getFile(outFolder, segments, 0);
            if (classFile == null) {
                return null;
            }

            // load the content of the file and create the class.
            FileInputStream fis = new FileInputStream(classFile);
            byte[] data = new byte[(int)classFile.length()];
            int read = 0;
            try {
                read = fis.read(data);
            } catch (IOException e) {
                data = null;
            }
            fis.close();

            if (data != null) {
                try {
                    Class<?> clazz = defineClass(null, data, 0, read);
                    if (clazz != null) {
                        return clazz;
                    }
                } catch (UnsupportedClassVersionError e) {
                    // Attempt to reload on lower version
                    int maxVersion = 50; // JDK 1.6
                    try {
                        byte[] rewritten = rewriteClass(data, maxVersion, 0);
                        return defineClass(null, rewritten, 0, rewritten.length);
                    } catch (UnsupportedClassVersionError e2) {
                        throw e; // throw *original* exception, not attempt to rewrite
                    }
                }
            }
        } catch (Exception e) {
            // log the exception?
        }

        return null;
    }

    /**
     * Rewrites the given class to the given target class file version.
     */
    public static byte[] rewriteClass(byte[] classData, final int maxVersion, final int minVersion) {
        assert maxVersion >= minVersion;
        ClassWriter classWriter = new ClassWriter(0);
        ClassVisitor classVisitor = new ClassVisitor(Opcodes.ASM4, classWriter) {
            @Override
            public void visit(int version, int access, String name, String signature,
                    String superName, String[] interfaces) {
                if (version > maxVersion) {
                    version = maxVersion;
                }
                if (version < minVersion) {
                    version = minVersion;
                }
                super.visit(version, access, name, signature, superName, interfaces);
            }
        };
        ClassReader reader = new ClassReader(classData);
        reader.accept(classVisitor, 0);
        return classWriter.toByteArray();
    }

    /**
     * Returns the File matching the a certain path from a root {@link File}.
     * <p/>The methods checks that the file ends in .class even though the last segment
     * does not.
     * @param parent the root of the file.
     * @param segments the segments containing the path of the file
     * @param index the offset at which to start looking into segments.
     * @throws FileNotFoundException
     */
    private File getFile(File parent, String[] segments, int index)
            throws FileNotFoundException {
        // reached the end with no match?
        if (index == segments.length) {
            throw new FileNotFoundException();
        }

        String toMatch = segments[index];
        File[] files = parent.listFiles();

        // we're at the last segments. we look for a matching <file>.class
        if (index == segments.length - 1) {
            toMatch = toMatch + ".class";

            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().equals(toMatch)) {
                        return file;
                    }
                }
            }

            // no match? abort.
            throw new FileNotFoundException();
        }

        String innerClassName = null;

        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    if (toMatch.equals(file.getName())) {
                        return getFile(file, segments, index+1);
                    }
                } else if (file.getName().startsWith(toMatch)) {
                    if (innerClassName == null) {
                        StringBuilder sb = new StringBuilder(segments[index]);
                        for (int i = index + 1 ; i < segments.length ; i++) {
                            sb.append('$');
                            sb.append(segments[i]);
                        }
                        sb.append(".class");

                        innerClassName = sb.toString();
                    }

                    if (file.getName().equals(innerClassName)) {
                        return file;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Loads a class from the 3rd party jar present in the project
     *
     * @return the class loader or null if not found.
     */
    private Class<?> loadClassFromJar(String name) {
        if (mJarClassLoader == null) {
            // get the OS path to all the external jars
            URL[] jars = getExternalJars();

            mJarClassLoader = new URLClassLoader(jars, this /* parent */);
        }

        try {
            // because a class loader always look in its parent loader first, we need to know
            // that we are querying the jar classloader. This will let us know to not query
            // it again for classes we don't find, or this would create an infinite loop.
            mInsideJarClassLoader = true;
            return mJarClassLoader.loadClass(name);
        } catch (ClassNotFoundException e) {
            // not found? return null.
            return null;
        } finally {
            mInsideJarClassLoader = false;
        }
    }

    /**
     * Returns an array of external jar files used by the project.
     * @return an array of OS-specific absolute file paths
     */
    private final URL[] getExternalJars() {
        // get a java project from it
        IJavaProject javaProject = JavaCore.create(mJavaProject.getProject());

        ArrayList<URL> oslibraryList = new ArrayList<URL>();
        IClasspathEntry[] classpaths = javaProject.readRawClasspath();
        if (classpaths != null) {
            for (IClasspathEntry e : classpaths) {
                if (e.getEntryKind() == IClasspathEntry.CPE_LIBRARY ||
                        e.getEntryKind() == IClasspathEntry.CPE_VARIABLE) {
                    // if this is a classpath variable reference, we resolve it.
                    if (e.getEntryKind() == IClasspathEntry.CPE_VARIABLE) {
                        e = JavaCore.getResolvedClasspathEntry(e);
                    }

                    handleClassPathEntry(e, oslibraryList);
                } else if (e.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
                    // get the container.
                    try {
                        IClasspathContainer container = JavaCore.getClasspathContainer(
                                e.getPath(), javaProject);
                        // ignore the system and default_system types as they represent
                        // libraries that are part of the runtime.
                        if (container != null &&
                                container.getKind() == IClasspathContainer.K_APPLICATION) {
                            IClasspathEntry[] entries = container.getClasspathEntries();
                            for (IClasspathEntry entry : entries) {
                                // TODO: Xav -- is this necessary?
                                if (entry.getEntryKind() == IClasspathEntry.CPE_VARIABLE) {
                                    entry = JavaCore.getResolvedClasspathEntry(entry);
                                }

                                handleClassPathEntry(entry, oslibraryList);
                            }
                        }
                    } catch (JavaModelException jme) {
                        // can't resolve the container? ignore it.
                        AdtPlugin.log(jme, "Failed to resolve ClasspathContainer: %s",
                                e.getPath());
                    }
                }
            }
        }

        return oslibraryList.toArray(new URL[oslibraryList.size()]);
    }

    private void handleClassPathEntry(IClasspathEntry e, ArrayList<URL> oslibraryList) {
        // get the IPath
        IPath path = e.getPath();

        // check the name ends with .jar
        if (SdkConstants.EXT_JAR.equalsIgnoreCase(path.getFileExtension())) {
            boolean local = false;
            IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
            if (resource != null && resource.exists() &&
                    resource.getType() == IResource.FILE) {
                local = true;
                try {
                    oslibraryList.add(new File(resource.getLocation().toOSString())
                            .toURI().toURL());
                } catch (MalformedURLException mue) {
                    // pass
                }
            }

            if (local == false) {
                // if the jar path doesn't match a workspace resource,
                // then we get an OSString and check if this links to a valid file.
                String osFullPath = path.toOSString();

                File f = new File(osFullPath);
                if (f.exists()) {
                    try {
                        oslibraryList.add(f.toURI().toURL());
                    } catch (MalformedURLException mue) {
                        // pass
                    }
                }
            }
        }
    }
}

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

package org.eclipse.andmore.internal.build;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import org.eclipse.andmore.internal.editors.layout.refactoring.AdtProjectTest;
import org.eclipse.andmore.internal.sdk.ProjectState;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.junit.Test;
import org.junit.Ignore;

import com.android.sdklib.BuildToolInfo;

/**
 * Test case to see if the DexWrapper can still be executed.
 */

public class DexWrapperTest extends AdtProjectTest {

    @Ignore // DexWrapper only works with build tools version 21 - 25
	@Test
	public void testMainDexClassExists() {
		loadMainDexClass();
	}
	
    @Ignore // DexWrapper only works with build tools version 21 - 25
	@Test
	public void testMainClassFieldsAvailable() {		
		Class<?> mainDexClass = loadMainDexClass();
		
		try {
			mainDexClass.getDeclaredField(DexWrapper.DEX_MAIN_FIELD_OUTPUT_ARRAYS);
			mainDexClass.getDeclaredField(DexWrapper.DEX_MAIN_FIELD_CLASSES_IN_MAIN_DEX);
			mainDexClass.getDeclaredField(DexWrapper.DEX_MAIN_FIELD_OUTPUT_RESOURCES);
		} catch (SecurityException e) {
			fail(e.getLocalizedMessage());
	    } catch (NoSuchFieldException e) {
	    	fail(e.getLocalizedMessage());
	    }
	}
	
    @Ignore // DexWrapper only works with build tools version 21 - 25
	@Test
	public void testDexArgumentsAvailable() {
		Class<?> dexArgumentsClass = loadDexArgumentsClass();
		
		try {
			dexArgumentsClass.getDeclaredField(DexWrapper.DEX_ARGUMENTS_FIELD_OUT_NAME);
			dexArgumentsClass.getDeclaredField(DexWrapper.DEX_ARGUMENTS_FIELD_JAR_OUTPUT);
			dexArgumentsClass.getDeclaredField(DexWrapper.DEX_ARGUMENTS_FIELD_FILES_NAMES);
			dexArgumentsClass.getDeclaredField(DexWrapper.DEX_ARGUMENTS_FIELD_VERBOSE);
			dexArgumentsClass.getDeclaredField(DexWrapper.DEX_ARGUMENTS_FIELD_FORCE_JUMBO);
			dexArgumentsClass.getDeclaredField(DexWrapper.DEX_ARGUMENTS_FIELD_MULTI_DEX);
			dexArgumentsClass.getDeclaredField(DexWrapper.DEX_ARGUMENTS_FIELD_MAIN_DEX_LIST_FILE);
			dexArgumentsClass.getDeclaredField(DexWrapper.DEX_ARGUMENTS_FIELD_MINIMAL_MAIN_DEX);
		} catch (SecurityException e) {
			fail(e.getLocalizedMessage());
	    } catch (NoSuchFieldException e) {
	    	fail(e.getLocalizedMessage());
	    }
	}
	
	private Class<?> loadDexArgumentsClass() {
		URLClassLoader loader = getSdkClassLoader();
		try {
			return loader.loadClass(DexWrapper.DEX_ARGS);
		} catch (SecurityException e) {
			fail(e.getLocalizedMessage());
		} catch(ClassNotFoundException e) {
			fail(e.getLocalizedMessage());
		}
		
		return null;
	}
	
	private Class<?> loadMainDexClass() {
		URLClassLoader loader = getSdkClassLoader();
		try {
			return loader.loadClass(DexWrapper.DEX_MAIN);
		} catch (SecurityException e) {
			fail(e.getLocalizedMessage());
		} catch(ClassNotFoundException e) {
			fail(e.getLocalizedMessage());
		}
		
		return null;
	}
	
	public URLClassLoader getSdkClassLoader() {
		ProjectState projectState = Sdk.getProjectState(getProject());
		assertNotNull(projectState);
		
		BuildToolInfo buildToolInfo = projectState.getBuildToolInfo();
        if (buildToolInfo == null) {
            buildToolInfo = getSdk().getLatestBuildTool();
        }
        assertNotNull(buildToolInfo);
        
        if(buildToolInfo.getRevision().getMajor() < 21) {
        	fail("DexWrapper only works with build tools version 21 and higher. "
        			+ "Currently trying to use " + buildToolInfo.getRevision().getMajor());
        }
        
		String dxLocation = buildToolInfo.getPath(BuildToolInfo.PathId.DX_JAR);
		
		File f = new File(dxLocation);	
		URL url = null;
		try {
			url = f.toURI().toURL();
		} catch(MalformedURLException malformedURLException) {
			fail(malformedURLException.getLocalizedMessage());
		}
		
		return new URLClassLoader(new URL[] { url }, DexWrapper.class.getClassLoader());
	}
}

/*
 * Copyright (C) 2009 The Android Open Source Project
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

package org.eclipse.andmore.internal.launch.junit.runtime;

import org.eclipse.jdt.internal.junit.runner.IVisitsTestTrees;

import java.util.ArrayList;
import java.util.List;

/**
 * Reference for an Android test suite aka class.
 */
@SuppressWarnings("restriction")
class TestSuiteReference extends AndroidTestReference {

    private final String mClassName;
    private List<AndroidTestReference> mTests;

    /**
     * Creates a TestSuiteReference
     *
     * @param className the fully qualified name of the test class
     */
    TestSuiteReference(String className) {
         mClassName = className;
         mTests = new ArrayList<AndroidTestReference>();
    }

    /**
     * Returns a count of the number of test cases included in this suite.
     */
    @Override
    public int countTestCases() {
        return mTests.size();
    }

    /**
     * Sends test identifier and test count information for this test class, and all its included
     * test methods.
     *
     * @param notified the {@link IVisitsTestTrees} to send test info too
     */
    @Override
    public void sendTree(IVisitsTestTrees notified) {
        notified.visitTreeEntry(getIdentifier(), true, countTestCases());
        for (AndroidTestReference ref: mTests) {
            ref.sendTree(notified);
        }
    }

    /**
     * Return the name of this test class.
     */
    @Override
    public String getName() {
        return mClassName;
    }

    /**
     * Adds a test method to this suite.
     *
     * @param testRef the {@link TestCaseReference} to add
     */
    void addTest(AndroidTestReference testRef) {
        mTests.add(testRef);
    }

    /** Returns the test suite of given name, null if no such test suite exists */
    public TestSuiteReference getTestSuite(String name) {
        for (AndroidTestReference ref: mTests) {
            if (ref instanceof TestSuiteReference && ref.getName().equals(name)) {
                return (TestSuiteReference) ref;
            }
        }

        return null;
    }
}

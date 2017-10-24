/**
 * Copyright (C) 2015 David Carver and others
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

package org.eclipse.andmore.integration.tests;

import org.eclipse.andmore.integration.tests.functests.sampleProjects.SampleProjectTest;
import org.eclipse.andmore.internal.build.AaptParserTest;
import org.eclipse.andmore.internal.build.AaptQuickFixTest;
//import org.eclipse.andmore.internal.build.DexWrapperTest;
import org.eclipse.andmore.internal.editors.AndroidContentAssistTest;
import org.eclipse.andmore.internal.editors.AndroidXmlAutoEditStrategyTest;
import org.eclipse.andmore.internal.editors.AndroidXmlCharacterMatcherTest;
import org.eclipse.andmore.internal.editors.HyperlinksTest;
import org.eclipse.andmore.internal.editors.formatting.EclipseXmlPrettyPrinterTest;
import org.eclipse.andmore.internal.editors.layout.gle2.LayoutMetadataTest;
import org.eclipse.andmore.internal.editors.manifest.ManifestInfoTest;
import org.eclipse.andmore.internal.launch.JUnitLaunchConfigDelegateTest;
import org.eclipse.andmore.internal.lint.ProjectLintConfigurationTest;
import org.eclipse.andmore.internal.refactorings.core.AndroidPackageRenameParticipantTest;
import org.eclipse.andmore.internal.refactorings.core.RenameResourceParticipantTest;
import org.eclipse.andmore.internal.refactorings.renamepackage.ApplicationPackageNameRefactoringTest;
import org.eclipse.andmore.internal.settings.MultiDexTest;
import org.eclipse.andmore.internal.wizards.exportgradle.ExportGradleTest;
import org.eclipse.andmore.internal.wizards.templates.TemplateHandlerTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({SampleProjectTest.class,
	AaptParserTest.class,
	AaptQuickFixTest.class,
    // DexWrapper only works with build tools version 21 - 25
	//DexWrapperTest.class,
	MultiDexTest.class,
	EclipseXmlPrettyPrinterTest.class,
	AndroidContentAssistTest.class,
	AndroidXmlAutoEditStrategyTest.class,
	AndroidXmlCharacterMatcherTest.class,
	HyperlinksTest.class,
	LayoutMetadataTest.class,
	ManifestInfoTest.class,
	JUnitLaunchConfigDelegateTest.class,
	ProjectLintConfigurationTest.class,
	AndroidPackageRenameParticipantTest.class,
	RenameResourceParticipantTest.class,
	ApplicationPackageNameRefactoringTest.class,
	ExportGradleTest.class,
	TemplateHandlerTest.class})
public class IntegrationTestSuite {

}

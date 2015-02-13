package org.eclipse.andmore.integration.tests;

import org.eclipse.andmore.integration.tests.functests.sampleProjects.SampleProjectTest;
import org.eclipse.andmore.internal.build.AaptParserTest;
import org.eclipse.andmore.internal.build.AaptQuickFixTest;
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
import org.eclipse.andmore.internal.wizards.exportgradle.ExportGradleTest;
import org.eclipse.andmore.internal.wizards.templates.TemplateHandlerTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({SampleProjectTest.class,
	AaptParserTest.class,
	AaptQuickFixTest.class,
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

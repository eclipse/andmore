/********************************************************************************
 * Copyright (c) 2007-2010 Motorola Inc. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Initial Contributor:
 * Fabio Fantato (Motorola)
 *
 * Contributors:
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/
package org.eclipse.sequoyah.device.framework.wizard.ui;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.sequoyah.device.framework.wizard.model.IWizardCustomizer;

/**
 * The Wizard Factory defines the common methods to support customized wizards for SEQUOYAH Wizard Instances.
 * If the user decides to customize on wizard it is need to implement a concrete class for it and 
 * reference this class using the extension point {@link org.eclipse.sequoyah.emulator.core.wizard}
 * @author Fabio Fantato
 */
public abstract class AbstractWizardCustomizer implements IWizardCustomizer {
	private boolean hasCustomizedProjectPage;
	private boolean hasCustomizedPropertyPage;
	private boolean hasCustomizedOtherPage;
	
	
	public AbstractWizardCustomizer() {
		this.hasCustomizedProjectPage = false;
		this.hasCustomizedPropertyPage = false;
		this.hasCustomizedOtherPage = false;	
	}
	
	
	/**
	 * To create a new WizardFactory its necessary to define which pages will
	 * be customized. Only the pages set up with true should return a valid WizardPage
	 * instance to replace the original implementation.
	 * @param hasCustomizedProjectPage
	 * @param hasCustomizedPropertyPage
	 * @param hasCustomizedOtherPage
	 */
	public AbstractWizardCustomizer(boolean hasCustomizedProjectPage,boolean hasCustomizedPropertyPage,boolean hasCustomizedOtherPage){
		this.hasCustomizedProjectPage = hasCustomizedProjectPage;
		this.hasCustomizedPropertyPage = hasCustomizedPropertyPage;
		this.hasCustomizedOtherPage = hasCustomizedOtherPage;		
	}
	
	
	/**
	 * Redefine the page for project info to be show in wizard. 
	 * To use the default implementation use:
	 * return new WizardNewProjectCreationPage(EmulatorWizardConstants.PAGE_PROJECT);
	 *
	 * It is important to use as pageId to any WizardPage created the constant EmulatorWizardConstants.PAGE_PROJECT
	 * @return
	 */
	public WizardPage getCustomizedProjectPage() {
		//EmulatorWizardConstants.PAGE_PROPERTY
		return null;
	}
	
	
	/**
	 * Redefine the page for properties info to be show in wizard. 
	 *
	 * It is important to use as pageId to any WizardPage created the constant EmulatorWizardConstants.PAGE_PROPERTIES
	 * @return
	 */
	public WizardPage getCustomizedPropertyPage() {
		//EmulatorWizardConstants.PAGE_PROPERTY
		return null;
	}
	
	
	/**
	 * Redefine the page for other info to be show in wizard. 
	 *
	 * It is important to use as pageId to any WizardPage created the constant EmulatorWizardConstants.PAGE_OTHER
	 * @return
	 */
	public WizardPage getCustomizedOtherPage() {
		//EmulatorWizardConstants.PAGE_OTHER
		return null;
	}
	
	
	public boolean hasCustomizedPropertyPage() {
		return this.hasCustomizedPropertyPage;
	}
	
	public boolean hasCustomizedProjectPage() {
		return this.hasCustomizedProjectPage;
	}
	

	public boolean hasCustomizedOtherPage() {
		return this.hasCustomizedOtherPage;
	}

	
}

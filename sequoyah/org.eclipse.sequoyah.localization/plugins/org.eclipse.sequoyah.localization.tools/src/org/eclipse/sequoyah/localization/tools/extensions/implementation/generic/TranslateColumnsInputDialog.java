/********************************************************************************
 * Copyright (c) 2009-2010 Motorola Mobility, Inc.
 * All rights reserved. This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Matheus Tait Lima (Eldorado)
 * Vinicius Rigoni (Eldorado)
 * Marcel Gorri (Eldorado)
 * 
 * Contributors:
 * Marcelo Marzola Bossoni (Eldorado) - Bug [326793] - Change from Table to Tree (display arrays as tree)
 * Marcelo Marzola Bossoni (Instituto de Pesquisas Eldorado) - Bug [352375] - Let translators contribute with translate dialog
 ********************************************************************************/
package org.eclipse.sequoyah.localization.tools.extensions.implementation.generic;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.preference.IPreferenceNode;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.osgi.util.NLS;
import org.eclipse.sequoyah.localization.tools.extensions.classes.ILocalizationSchema;
import org.eclipse.sequoyah.localization.tools.extensions.classes.ITranslator;
import org.eclipse.sequoyah.localization.tools.i18n.Messages;
import org.eclipse.sequoyah.localization.tools.managers.LocalizationManager;
import org.eclipse.sequoyah.localization.tools.managers.PreferencesManager;
import org.eclipse.sequoyah.localization.tools.managers.TranslatorManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.dialogs.WorkbenchPreferenceDialog;

/**
 * Asks for: - source language, and - target languages
 * 
 * These data will be used for translation purposes
 */
@SuppressWarnings("restriction")
public class TranslateColumnsInputDialog extends TitleAreaDialog implements ITranslateDialog {

	private IProject project = null;

	private String dialogTitle = null;

	// Translators
	private Combo translatorsCombo = null;

	private String translator;

	private Label translatorBrandingImage = null;

	// "From" information
	private String selectedColumn = null;

	private String[] selectedCells = null;

	private Combo fromCombo = null;

	private String fromLanguage;
	
	private Composite customArea = null;

	// "To" information
	private List<DestinationColumn> destinationColumns = null;

	private TreeColumn[] columns = null;

	private PreferenceDialog networkPreferencesDialog;

	private PreferenceManager prefMan;

	private static final String PROXY_PREFERENCE_PAGE_ID = "org.eclipse.ui.net.NetPreferences"; //$NON-NLS-1$

	private Composite mainComposite;

	/**
	 * The constructor
	 */
	public TranslateColumnsInputDialog(Shell parentShell, IProject project,
			String selectedColumn, String[] selectedCells,
			TreeColumn[] columns, String dialogTitle) {
		super(parentShell);
		this.project = project;
		this.selectedColumn = selectedColumn;
		this.selectedCells = selectedCells;
		this.columns = columns;
		this.dialogTitle = dialogTitle;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets
	 * .Shell)
	 */
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(this.dialogTitle);
	}

	/**
	 * Get the information about destination columns and languages
	 * 
	 * @return information about destination columns and languages
	 */
	public List<DestinationColumn> getDestinationColumns() {
		List<DestinationColumn> destinationColumns = new ArrayList<DestinationColumn>();
		for (DestinationColumn destinationColumn : this.destinationColumns) {
			if (destinationColumn.isSelected()) {
				destinationColumns.add(destinationColumn);
			}
		}
		return destinationColumns;
	}

	/**
	 * Centralizes a window on the screen
	 * 
	 * @param shell
	 *            the window
	 */
	private static void centralizeShell(Shell shell) {
		int width = shell.getSize().x;
		int height = shell.getSize().y;
		int x = (shell.getDisplay().getClientArea().width - width) / 2;
		int y = (shell.getDisplay().getClientArea().height - height) / 2;
		shell.setLocation(x, y);
	}

	/**
	 * Opens the network preferences page
	 * 
	 * @return true if the user has confirmed the network configurations or
	 *         false otherwise
	 */
	private boolean openNetworkPreferencesPage() {
		// Creates the dialog every time, because it is disposed when it is
		// closed.
		networkPreferencesDialog = new WorkbenchPreferenceDialog(
				this.getShell(), prefMan);
		networkPreferencesDialog.create();
		centralizeShell(networkPreferencesDialog.getShell());
		networkPreferencesDialog.open();
		return networkPreferencesDialog.getReturnCode() == WorkbenchPreferenceDialog.OK;
	}

	/**
	 * Get the original language
	 * 
	 * @return the original language of the word(s) being translated
	 */
	public String getFromLanguage() {
		return LanguagesUtil.getLanguageID(fromLanguage);
	}

	/**
	 * Get the selected translator
	 * 
	 * @return the selected translator name
	 */
	public String getTranslator() {
		return translator;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets
	 * .Composite)
	 */
	@Override
	protected Control createDialogArea(Composite parent) {

		ILocalizationSchema schema = LocalizationManager.getInstance()
				.getLocalizationSchema(project);

		ComboListener comboListener = new ComboListener();

		mainComposite = new Composite(parent, SWT.NONE);
		mainComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
				true));
		mainComposite.setLayout(new GridLayout(1, true));

		/*
		 * Translators Area
		 */
		createTranslatorArea(mainComposite, comboListener);

		/*
		 * From Area
		 */
		createFromArea(mainComposite, comboListener, schema);

		/*
		 * To Area
		 */
		createToArea(mainComposite, schema);

		/*
		 * Network Area
		 */
		createNetworkGroup(mainComposite);

		/*
		 * Branding Area
		 */
		createTranslatorBrandingArea(mainComposite);
		
		// Set initial values
		setInitialValues();
		
		createCustomArea(false);
		
		setTitle(dialogTitle);

		return parent;
	}
	
	private void createCustomArea(boolean recomputeSize) {
		if (customArea != null) {
			customArea.dispose();
		}
		
		if (translator != null) {
			ITranslator aTranslator = TranslatorManager.getInstance().getTranslatorByName(translator);
			if (aTranslator != null) {
				customArea = aTranslator.createCustomArea(mainComposite, this);
				if (customArea != null) {
					if (recomputeSize) {
						mainComposite.getShell().setSize(mainComposite.getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT));
					}
					mainComposite.getShell().layout(true, true); 
				}
			}
		}
		
	}

	/**
	 * Create translators group and combo
	 * 
	 * @param parent
	 *            parent composite
	 * @param comboListener
	 *            translator combo listener
	 */
	private void createTranslatorArea(Composite parent,
			ComboListener comboListener) {

		Group translatorGroup = new Group(parent, SWT.SHADOW_ETCHED_OUT);
		translatorGroup.setText(Messages.Translator_Text);
		translatorGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
				false));
		translatorGroup.setLayout(new GridLayout(2, false));

		Label translatorText = new Label(translatorGroup, SWT.NONE);
		translatorText.setText(Messages.Service_Text);

		translatorsCombo = LanguagesUtil
				.createTranslatorsCombo((Composite) translatorGroup);
		translatorsCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true,
				false));
		translatorsCombo.addSelectionListener(comboListener);

	}

	/**
	 * Create origin area with information about selection and original language
	 * 
	 * @param parent
	 *            parent composite
	 * @param comboListener
	 *            translator combo listener
	 * @param schema
	 *            android localization schema
	 */
	private void createFromArea(Composite parent, ComboListener comboListener,
			ILocalizationSchema schema) {
		Group languageFromGroup = new Group(parent, SWT.SHADOW_ETCHED_OUT);
		languageFromGroup.setText(Messages.TranslationDialog_From);
		languageFromGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
				false));
		languageFromGroup.setLayout(new GridLayout(3, false));

		/*
		 * Selected cells / text
		 */
		// TODO: decide whether to show or now the selected text
		/*
		 * Label labelFrom = new Label(languageFromGroup, SWT.NONE);
		 * labelFrom.setText(Messages.TranslationDialog_SelectedText);
		 * 
		 * Label labelFromText = new Label(languageFromGroup, SWT.NONE);
		 * labelFromText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
		 * false, 2, 1)); String selectedText = ""; for (String text :
		 * this.selectedCells) { selectedText += text + "\n"; }
		 * labelFromText.setText(selectedText);
		 */

		/*
		 * Language selection
		 */
		Label fromText = new Label(languageFromGroup, SWT.NONE);
		fromText.setText(Messages.TranslationDialog_FromLanguage);

		String selectedLanguage = schema
				.getISO639LangFromID(this.selectedColumn);
		// check if ISO639 exists
		selectedLanguage = ((LanguagesUtil.getLanguageName(selectedLanguage) != null) ? selectedLanguage
				: null);
		String defaultLanguage = PreferencesManager.getInstance()
				.getProjectPreferencesManager(this.project)
				.getDefaultLanguageForColumn(this.selectedColumn);
		fromCombo = LanguagesUtil.createLanguagesCombo(
				(Composite) languageFromGroup, selectedLanguage,
				defaultLanguage, schema);
		fromCombo
				.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		fromCombo.addSelectionListener(comboListener);

		LanguagesUtil.createImageStatus(languageFromGroup, selectedLanguage);

	}

	/**
	 * Create destination area with information about destination columns and
	 * languages
	 * 
	 * @param parent
	 *            parent composite
	 * @param schema
	 *            android localization schema
	 */
	private void createToArea(Composite parent, ILocalizationSchema schema) {
		Group languageToGroup = new Group(parent, SWT.SHADOW_ETCHED_OUT);
		languageToGroup.setText(Messages.TranslationDialog_To);
		languageToGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
				false));
		languageToGroup.setLayout(new GridLayout(4, false));

		if (this.columns.length <= 2) {
			Label noColumns = new Label(languageToGroup, SWT.NONE);
			noColumns.setText(Messages.TranslationDialog_NoColumns);
		}

		/*
		 * Destination Columns
		 */
		this.destinationColumns = new ArrayList<DestinationColumn>();

		int i = 0;
		for (TreeColumn column : this.columns) {
			if ((i > 0) && (!column.getText().equals(this.selectedColumn))) {

				// checkbox
				Button checkbox = new Button(languageToGroup, SWT.CHECK);
				checkbox.setSelection(true);

				// label
				Label label = new Label(languageToGroup, SWT.NONE);
				label.setText(column.getText());

				// languages combo
				String selectedLanguage = schema.getISO639LangFromID(column
						.getText());
				// check if ISO639 exists
				selectedLanguage = ((LanguagesUtil
						.getLanguageName(selectedLanguage) != null) ? selectedLanguage
						: null);
				String defaultLanguage = PreferencesManager.getInstance()
						.getProjectPreferencesManager(this.project)
						.getDefaultLanguageForColumn(column.getText());

				DestinationColumn destColumn = new DestinationColumn(checkbox,
						true, column.getText(), selectedLanguage);

				checkbox.setData(destColumn);
				checkbox.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						((DestinationColumn) ((Button) e.getSource()).getData())
								.setSelected(((Button) e.getSource())
										.getSelection());
						validate();
					}

				});
				Combo languagesCombo = LanguagesUtil.createLanguagesCombo(
						languageToGroup, selectedLanguage, defaultLanguage,
						schema);
				destColumn.setLang(LanguagesUtil.getLanguageID(languagesCombo
						.getText()));
				languagesCombo.setLayoutData(new GridData(SWT.FILL, SWT.FILL,
						true, false));
				languagesCombo.setData(destColumn);
				languagesCombo.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						String selectedText = ((Combo) e.getSource()).getText();
						if (!selectedText.equals(LanguagesUtil
								.getComboSeparator())) {
							((DestinationColumn) ((Combo) e.getSource())
									.getData()).setLang(LanguagesUtil
									.getLanguageID(selectedText));
							// update default language
							PreferencesManager
									.getInstance()
									.getProjectPreferencesManager(project)
									.setDefaultLanguageForColumn(
											((DestinationColumn) ((Combo) e
													.getSource()).getData())
													.getText(),
											LanguagesUtil
													.getLanguageID(selectedText));
						} else {
							((DestinationColumn) ((Combo) e.getSource())
									.getData()).setLang(null);
						}
						validate();
					}

				});

				// status image
				LanguagesUtil.createImageStatus(languageToGroup,
						selectedLanguage);

				destinationColumns.add(destColumn);
			}
			i++;
		}

		/*
		 * Select and Deselect All buttons
		 */
		Composite buttonsArea = new Composite(languageToGroup, SWT.NONE);
		buttonsArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true,
				4, 1));
		buttonsArea.setLayout(new GridLayout(2, true));

		Button selectAllButton = new Button(buttonsArea, SWT.PUSH);
		selectAllButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
				false));
		selectAllButton.setText(Messages.TranslationDialog_SelectAll);
		selectAllButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				for (DestinationColumn destColumn : destinationColumns) {
					destColumn.setSelected(true);
				}
				validate();
			}
		});

		Button deselectAllButton = new Button(buttonsArea, SWT.PUSH);
		deselectAllButton.setText(Messages.TranslationDialog_DeselectAll);
		deselectAllButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
				false));
		deselectAllButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				for (DestinationColumn destColumn : destinationColumns) {
					destColumn.setSelected(false);
				}
				validate();
			}
		});

	}

	/**
	 * Create translation branding area, which will display a branding image, if
	 * any, for the translator service selected
	 * 
	 * @param parent
	 *            parent composite
	 */
	private void createTranslatorBrandingArea(Composite parent) {
		Composite brandingComposite = new Composite(parent, SWT.RIGHT);
		brandingComposite.setLayout(new GridLayout(1, false));
		brandingComposite.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false,
				false));

		translatorBrandingImage = new Label(brandingComposite, SWT.NONE);
		translatorBrandingImage.setLayoutData(new GridData(SWT.FILL, SWT.TOP,
				true, true));
	}

	/**
	 * Populate attributes with their initial values
	 */
	private void setInitialValues() {
		translator = translatorsCombo.getText();
		fromLanguage = fromCombo.getText();
		TranslatorManager.getInstance().setTranslatorBranding(translator,
				translatorBrandingImage);
	}

	/**
	 * Selection Listener that will be responsible for monitoring changes in
	 * combo selections (from language and translator) The combos for
	 * "to language" are differently managed
	 */
	class ComboListener implements SelectionListener {
		public void widgetDefaultSelected(SelectionEvent e) {
			widgetSelected(e);
		}

		public void widgetSelected(SelectionEvent e) {
			if (e.getSource() == fromCombo) {
				if (!fromCombo.getText().equals(
						LanguagesUtil.getComboSeparator())) {
					fromLanguage = fromCombo.getText();
					if (!fromLanguage.equals(LanguagesUtil.getComboSeparator())) {
						// update default language
						PreferencesManager
								.getInstance()
								.getProjectPreferencesManager(project)
								.setDefaultLanguageForColumn(
										selectedColumn,
										LanguagesUtil
												.getLanguageID(fromLanguage));
					}
				} else {
					fromLanguage = null;
				}
			} else if (e.getSource() == translatorsCombo) {
				translator = translatorsCombo.getText();
				TranslatorManager.getInstance().setTranslatorBranding(
						translator, translatorBrandingImage);
			}
			createCustomArea(true);
			validate();
		}
	}

	/**
	 * This class represents a destination column, with its language and name
	 */
	public class DestinationColumn {
		private Button checkbox;

		private boolean isSelected = false;

		private String text;

		private String lang;

		public DestinationColumn(Button checkbox, boolean isSelected,
				String text, String lang) {
			super();
			this.checkbox = checkbox;
			this.text = text;
			this.isSelected = isSelected;
			this.lang = lang;
		}

		public Button getCheckbox() {
			return checkbox;
		}

		public void setCheckbox(Button checkbox) {
			this.checkbox = checkbox;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}

		public String getLang() {
			return lang;
		}

		public void setLang(String lang) {
			this.lang = lang;
		}

		public boolean isSelected() {
			return isSelected;
		}

		public void setSelected(boolean isSelected) {
			this.isSelected = isSelected;
			this.getCheckbox().setSelection(isSelected);
		}
	}

	@Override
	protected void initializeBounds() {
		super.initializeBounds();
		// call validate after creating dialog
		validate();
	}

	/**
	 * Validate the current selection and enable/disable OK button
	 */
	public void validate() {

		String errorMessage = null;

		if (fromLanguage == null) {
			errorMessage = Messages.TranslateColumnInputDialog_Error_ToOrFromNotSet;
		}

		if (errorMessage == null && getDestinationColumns().size() == 0) {
			errorMessage = Messages.TranslateColumnsInputDialog_Error_NoDestinationColumnSelected;
		}

		if (errorMessage == null) {
			for (DestinationColumn destColumn : getDestinationColumns()) {
				if (destColumn.getLang() == null) {
					errorMessage = NLS.bind(Messages.TranslateColumnsInputDialog_Error_NoLanguageSet, destColumn.getText());
					break;
				}
			}
		}

		if (errorMessage == null && (translator == null || translator.length() == 0)) {
			errorMessage = Messages.TranslateColumnsInputDialog_Error_NoTranslatorsAvailable;
		}
		
		if (errorMessage == null) {
			errorMessage = TranslatorManager.getInstance().getTranslatorByName(translator).canTranslate(fromLanguage, getDestinationColumnsNames());
		}

		setErrorMessage(errorMessage);
		if (getButton(IDialogConstants.OK_ID) != null) {
			getButton(IDialogConstants.OK_ID).setEnabled(errorMessage == null);
		}
		
	}
	
	@Override
	public void create() {
		super.create();
		validate();
	}

	private String[] getDestinationColumnsNames() {
		List<String> selected = new ArrayList<String>();
		for (DestinationColumn column: destinationColumns) {
			if (column.isSelected()) {
				selected.add(column.getText());
			}
		}
		return selected.toArray(new String[0]);
	}

	/**
	 * Create Network group
	 * 
	 * @param parent
	 *            parent composite
	 */
	@SuppressWarnings("unchecked")
	private void createNetworkGroup(Composite parent) {

		// Makes the network preferences dialog manager
		PreferenceManager manager = PlatformUI.getWorkbench()
				.getPreferenceManager();

		IPreferenceNode networkNode = null;

		for (IPreferenceNode node : (List<IPreferenceNode>) manager
				.getElements(PreferenceManager.PRE_ORDER)) {
			if (node.getId().equals(PROXY_PREFERENCE_PAGE_ID)) {
				networkNode = node;
				break;
			}
		}
		prefMan = new PreferenceManager();
		if (networkNode != null) {
			prefMan.addToRoot(networkNode);
		}

		Link downloadText = new Link(parent, SWT.WRAP);
		downloadText.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				// Do nothing
			}

			public void widgetSelected(SelectionEvent e) {
				openNetworkPreferencesPage();
			}
		});

		String linkText = Messages.bind(Messages.NetworkLinkText,
				Messages.NetworkLinkText, Messages.NetworkLinkLink);

		downloadText.setText(linkText);
		downloadText.update();
		GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, false, 1,
				1);
		downloadText.setLayoutData(gridData);
	}
}

/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eclipse.andmore.android.wizards.monkey;

import java.util.Collection;
import java.util.EventListener;
import java.util.EventObject;
import java.util.LinkedHashSet;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * DESCRIPTION: <br>
 * This class is an abstract implementation of a Composite extension that
 * specific composites for making Composites listeners. <br>
 * It provides common functionalities to those subclasses which assist content
 * validation, layout, etc. <br>
 * RESPONSIBILITY: <br>
 * - Provide common functionalities to classes implementing Composites <br>
 * COLABORATORS: <br>
 * Composite: extends this class <br>
 * MagxPropertyCompositeChangeListener: declares this interface for other
 * classes to be able to listen to change events on the content <br>
 * MagxPropertyCompositeChangeEvent: declares the class and uses this kind of
 * event for notifying content change to listeners <br>
 * USAGE: <br>
 * This class should be extended by UI classes implementing Composites (property
 * pages, wizards, etc).
 */
public abstract class AbstractPropertiesComposite extends Composite {
	/**
	 * 
	 * This class represents events for notifying content change on
	 * AbstractPropertiesComposite extending classes to registered listeners.
	 * 
	 */
	@SuppressWarnings("serial")
	public class PropertyCompositeChangeEvent extends EventObject {
		/**
		 * Creates a new MagxPropertyCompositeChangeEvent object with the
		 * composite that changed as its data.
		 * 
		 * @param composite
		 *            the composite that changed
		 */
		PropertyCompositeChangeEvent(AbstractPropertiesComposite composite) {
			super(composite);
		}
	}

	/**
	 * 
	 * This interface must be implemented by classes that wish to listen to
	 * changes on AbstractPropertiesComposite extending classes.
	 *
	 */
	public interface PropertyCompositeChangeListener extends EventListener {
		/**
		 * Notifies the event of change on AbstractPropertiesComposite extending
		 * classes.
		 * 
		 * @param e
		 *            the change event
		 */
		public void compositeChanged(PropertyCompositeChangeEvent e);
	}

	// The default value to use for margins, both vertical and horizontal
	private static final int DEFAULT_MARGIN_SIZE = 20;

	// collection of registered listeners to this composite
	private static final Collection<PropertyCompositeChangeListener> listeners = new LinkedHashSet<PropertyCompositeChangeListener>();

	public AbstractPropertiesComposite(Composite parent) {
		super(parent, SWT.NONE);
	}

	/**
	 * Adds the given listener to the list of registered listeners of this
	 * composite's changes.
	 * 
	 * @param listener
	 *            the listener to be registered
	 */
	public static void addCompositeChangeListener(PropertyCompositeChangeListener listener) {
		listeners.add(listener);
	}

	/**
	 * Removes the given listener from the list of registered listeners of this
	 * composite's changes.
	 * 
	 * @param listener
	 *            the listener to be unregistered
	 */
	public static void removeCompositeChangeListener(PropertyCompositeChangeListener listener) {
		listeners.remove(listener);
	}

	/**
	 * Notifies an event of change on the composite to all registered listeners.
	 * 
	 * This method must be called by extending classes whenever a change is made
	 * to them that registered listeners should be aware of (examples: typed
	 * text, button press, etc)
	 */
	protected void notifyCompositeChangeListeners() {
		PropertyCompositeChangeEvent event = new PropertyCompositeChangeEvent(this);

		for (PropertyCompositeChangeListener listener : listeners) {
			listener.compositeChanged(event);
		}
	}

	/**
	 * Given a collection of controls, turn all their enabled status to the one
	 * provided
	 * 
	 * @param enabled
	 *            True to enable all controls in the collection
	 * @param controlsToUpdate
	 *            A collection of all controls to apply the state provided by
	 *            enabled parameter
	 */
	protected void updateWidgetEnableStatus(boolean enabled, Collection<Control> controlsToUpdate) {
		for (Control control : controlsToUpdate) {
			if (!control.isDisposed()) {
				control.setEnabled(enabled);
			}
		}
	}

	/**
	 * Sets the main layout to this composite as a GridLayout with the given
	 * number of columns and with columns not the same width.
	 * 
	 * @param numColumns
	 *            the number of columns for the GridLayout
	 */
	protected final void setMainLayout(int numColumns) {
		GridLayout mainLayout = new GridLayout(numColumns, false);
		mainLayout.marginWidth = DEFAULT_MARGIN_SIZE;
		mainLayout.marginHeight = DEFAULT_MARGIN_SIZE;
		this.setLayout(mainLayout);
	}

	/**
	 * Declaration of the abstract getErrorMessage() method. Retrieves the error
	 * message associated with the current extending composite state. If no
	 * error is found with the current state, <code>null</code> <b>must</b> be
	 * returned by the extending composite.
	 * 
	 * @return the error message, or <code>null</code> if there are no errors
	 */
	public abstract String getErrorMessage();

}

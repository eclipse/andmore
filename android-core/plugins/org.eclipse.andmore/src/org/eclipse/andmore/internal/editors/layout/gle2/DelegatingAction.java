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
package org.eclipse.andmore.internal.editors.layout.gle2;

import com.android.annotations.NonNull;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.widgets.Event;

/**
 * Implementation of {@link IAction} which delegates to a different
 * {@link IAction} which allows a subclass to wrap and customize some of the
 * behavior of a different action
 */
public class DelegatingAction implements IAction {
    private final IAction mAction;

    /**
     * Construct a new delegate of the given action
     *
     * @param action the action to be delegated
     */
    public DelegatingAction(@NonNull IAction action) {
        mAction = action;
    }

    @Override
    public void addPropertyChangeListener(IPropertyChangeListener listener) {
        mAction.addPropertyChangeListener(listener);
    }

    @Override
    public int getAccelerator() {
        return mAction.getAccelerator();
    }

    @Override
    public String getActionDefinitionId() {
        return mAction.getActionDefinitionId();
    }

    @Override
    public String getDescription() {
        return mAction.getDescription();
    }

    @Override
    public ImageDescriptor getDisabledImageDescriptor() {
        return mAction.getDisabledImageDescriptor();
    }

    @Override
    public HelpListener getHelpListener() {
        return mAction.getHelpListener();
    }

    @Override
    public ImageDescriptor getHoverImageDescriptor() {
        return mAction.getHoverImageDescriptor();
    }

    @Override
    public String getId() {
        return mAction.getId();
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        return mAction.getImageDescriptor();
    }

    @Override
    public IMenuCreator getMenuCreator() {
        return mAction.getMenuCreator();
    }

    @Override
    public int getStyle() {
        return mAction.getStyle();
    }

    @Override
    public String getText() {
        return mAction.getText();
    }

    @Override
    public String getToolTipText() {
        return mAction.getToolTipText();
    }

    @Override
    public boolean isChecked() {
        return mAction.isChecked();
    }

    @Override
    public boolean isEnabled() {
        return mAction.isEnabled();
    }

    @Override
    public boolean isHandled() {
        return mAction.isHandled();
    }

    @Override
    public void removePropertyChangeListener(IPropertyChangeListener listener) {
        mAction.removePropertyChangeListener(listener);
    }

    @Override
    public void run() {
        mAction.run();
    }

    @Override
    public void runWithEvent(Event event) {
        mAction.runWithEvent(event);
    }

    @Override
    public void setActionDefinitionId(String id) {
        mAction.setActionDefinitionId(id);
    }

    @Override
    public void setChecked(boolean checked) {
        mAction.setChecked(checked);
    }

    @Override
    public void setDescription(String text) {
        mAction.setDescription(text);
    }

    @Override
    public void setDisabledImageDescriptor(ImageDescriptor newImage) {
        mAction.setDisabledImageDescriptor(newImage);
    }

    @Override
    public void setEnabled(boolean enabled) {
        mAction.setEnabled(enabled);
    }

    @Override
    public void setHelpListener(HelpListener listener) {
        mAction.setHelpListener(listener);
    }

    @Override
    public void setHoverImageDescriptor(ImageDescriptor newImage) {
        mAction.setHoverImageDescriptor(newImage);
    }

    @Override
    public void setId(String id) {
        mAction.setId(id);
    }

    @Override
    public void setImageDescriptor(ImageDescriptor newImage) {
        mAction.setImageDescriptor(newImage);
    }

    @Override
    public void setMenuCreator(IMenuCreator creator) {
        mAction.setMenuCreator(creator);
    }

    @Override
    public void setText(String text) {
        mAction.setText(text);
    }

    @Override
    public void setToolTipText(String text) {
        mAction.setToolTipText(text);
    }

    @Override
    public void setAccelerator(int keycode) {
        mAction.setAccelerator(keycode);
    }
}

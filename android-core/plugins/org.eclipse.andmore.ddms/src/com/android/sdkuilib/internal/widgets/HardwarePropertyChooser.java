/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.sdkuilib.internal.widgets;

import com.android.sdklib.internal.avd.HardwareProperties.HardwareProperty;
import com.android.sdklib.internal.avd.HardwareProperties.HardwarePropertyType;
import com.android.sdkuilib.ui.GridDialog;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

/**
 * Dialog to choose a hardware property
 */
class HardwarePropertyChooser extends GridDialog {

    private final Map<String, HardwareProperty> mProperties;
    private final Collection<String> mExceptProperties;
    private HardwareProperty mChosenProperty;
    private Label mTypeLabel;
    private Label mDescriptionLabel;

    HardwarePropertyChooser(Shell parentShell,
            Map<String, HardwareProperty> properties,
            Collection<String> exceptProperties) {
        super(parentShell, 2, false);
        mProperties = properties;
        mExceptProperties = exceptProperties;
    }

    public HardwareProperty getProperty() {
        return mChosenProperty;
    }

    @Override
    public void createDialogContent(Composite parent) {
        Label l = new Label(parent, SWT.NONE);
        l.setText("Property:");

        final Combo c = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
        // simple list for index->name resolution.
        final ArrayList<String> indexToName = new ArrayList<String>();

        // Sort the combo entries by display name if available, otherwise by hardware name.
        Set<Entry<String, HardwareProperty>> entries =
            new TreeSet<Map.Entry<String,HardwareProperty>>(
                    new Comparator<Map.Entry<String,HardwareProperty>>() {
                @Override
                public int compare(Entry<String, HardwareProperty> entry0,
                                   Entry<String, HardwareProperty> entry1) {
                    String s0 = entry0.getValue().getAbstract();
                    String s1 = entry1.getValue().getAbstract();
                    if (s0 != null && s1 != null) {
                        return s0.compareTo(s1);
                    }
                    return entry0.getKey().compareTo(entry1.getKey());
                }
            });
        entries.addAll(mProperties.entrySet());

        for (Entry<String, HardwareProperty> entry : entries) {
            if (entry.getValue().isValidForUi() &&
                    mExceptProperties.contains(entry.getKey()) == false) {
                c.add(entry.getValue().getAbstract());
                indexToName.add(entry.getKey());
            }
        }
        boolean hasValues = true;
        if (indexToName.size() == 0) {
            hasValues = false;
            c.add("No properties");
            c.select(0);
            c.setEnabled(false);
        }

        c.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                int index = c.getSelectionIndex();
                String name = indexToName.get(index);
                processSelection(name, true /* pack */);
            }
        });

        l = new Label(parent, SWT.NONE);
        l.setText("Type:");

        mTypeLabel = new Label(parent, SWT.NONE);

        l = new Label(parent, SWT.NONE);
        l.setText("Description:");

        mDescriptionLabel = new Label(parent, SWT.NONE);

        if (hasValues) {
            c.select(0);
            processSelection(indexToName.get(0), false /* pack */);
        }
    }

    private void processSelection(String name, boolean pack) {
        mChosenProperty = name == null ? null : mProperties.get(name);

        String type = "Unknown";
        String desc = "Unknown";

        if (mChosenProperty != null) {
            desc = mChosenProperty.getDescription();
            HardwarePropertyType vt = mChosenProperty.getType();
            if (vt != null) {
                type = vt.getName();
            }
        }

        mTypeLabel.setText(type);
        mDescriptionLabel.setText(desc);

        if (pack) {
            getShell().pack();
        }
    }

}

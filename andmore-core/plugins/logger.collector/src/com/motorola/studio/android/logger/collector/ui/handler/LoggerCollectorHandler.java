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
package com.motorola.studio.android.logger.collector.ui.handler;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.motorola.studio.android.logger.collector.ui.wizard.LoggerCollectorWizard;
import com.motorola.studio.android.logger.collector.util.WidgetsUtil;

/**
 * This class is responsible to handling action in menu collect log file item.
 */
public class LoggerCollectorHandler extends AbstractHandler {

	/**
	 * This method is responsible to execute handler action
	 * 
	 * @param event
	 *            execute event
	 * @return handler action
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		return WidgetsUtil.runWizard(new LoggerCollectorWizard());
	}
}

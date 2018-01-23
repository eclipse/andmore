/********************************************************************************
 * Copyright (c) 2007-2010 Motorola Mobility, Inc. and others.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Fabio Fantato (Motorola)
 * 
 * Contributors:
 * Yu-Fen Kuo (MontaVista) - bug#236476 - provide a generic device type
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 * Julia Martinez Perdigueiro (Eldorado) - [329548] Adding attribute/method to retrieve the tooltip on double click support for dev mgt view
 ********************************************************************************/
package org.eclipse.sequoyah.device.framework.status;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class StatusRegistry {
	private Map<String,IStatus> status;
	private Map<String, String> statusUITooltip;
		private static StatusRegistry _instance;

		private StatusRegistry(){		
			status = new HashMap<String,IStatus>();
			statusUITooltip = new HashMap<String, String>();
		}
		
		public static StatusRegistry getInstance(){
			if (_instance==null) {				
				_instance = new StatusRegistry();
				StatusManager.getInstance().loadStatus();
			}
			return _instance;
		}
						
		public IStatus getStatus(String statusId) {
			return status.get(statusId);
		}

		public Collection<IStatus> getStatus() {
			return status.values();
		}
		
		public void setStatus(Map<String,IStatus> status) {
			this.status = status;
		}
		
		public void addStatus(IStatus status){
			this.status.put(status.getId(),status);
		}

		public void clear(){
			this.status.clear();
			this.statusUITooltip.clear();
		}
		
		public void addStatusTooltip(String statusId, String tooltipText){
			this.statusUITooltip.put(statusId, tooltipText);
		}
		
		public String getTooltipTextForStatus(String statusId){
			return this.statusUITooltip.get(statusId);
		}
}
/********************************************************************************
 * Copyright (c) 2007-2010 Motorola Inc and others.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Daniel Barboza Franco (Eldorado Research Institute) - bug 221739
 * 
 * Contributors:
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/

package org.eclipse.sequoyah.device.framework.statemachine;

import java.util.Set;
import java.util.Vector;

import org.eclipse.sequoyah.device.framework.status.IStatusTransition;

public class StateMachine {

	boolean machine[][];
	@SuppressWarnings("rawtypes")
	private Vector statesVector;
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public StateMachine(Set<IStatusTransition> transitions) {
		
		int size;
		
		statesVector = new Vector();
		
		for (IStatusTransition transition:transitions) {
			if (!statesVector.contains(transition.getStartId())) {
				statesVector.add(transition.getStartId());
			}
			
			if (!statesVector.contains(transition.getEndId())) {
				statesVector.add(transition.getEndId());
			}
			
			if (!statesVector.contains(transition.getHaltId())) {
				statesVector.add(transition.getHaltId());
			}
		}

		size = statesVector.size();
		machine = new boolean[size][size];
		
		for (int i=0; i<size;i++) {
			for (int j=0; j<size; j++) {
				machine[i][j] = false;
			}
		}
			
		for (IStatusTransition transition:transitions) {
			int halt, start, end;
			start = statesVector.indexOf(transition.getStartId());
			end =  statesVector.indexOf(transition.getEndId());
			halt = statesVector.indexOf(transition.getHaltId());
			
			machine[start][end] = true;
			machine[start][halt] = true;
			
		}
	}

	/**
	 * Returns true if the transition is valid, false otherwise.
	 * @param src The source state.
	 * @param dest The destination state.
	 * @return true if the transition is valid, false otherwise.
	 * 
	 **/
	public boolean isTransitionValid(String src, String dest){
		
		int s,d;
		s = statesVector.indexOf(src);
		d = statesVector.indexOf(dest);
		
		return machine[s][d];
	}
	
}

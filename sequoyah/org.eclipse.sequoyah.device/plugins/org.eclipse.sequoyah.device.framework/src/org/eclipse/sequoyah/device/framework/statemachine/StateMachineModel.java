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
 * Daniel Barboza Franco (Eldorado Research Institute) - Bug [250644] - Instance view keeps enabled buttons while performing a service.
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/

package org.eclipse.sequoyah.device.framework.statemachine;


public class StateMachineModel {

	String currentState;
	
	StateMachine stateMachine;
	
	public StateMachineModel(StateMachine stm) {
		
		this.stateMachine = stm;
	}
	
	
	/**
	 * This method verify if the transition from the current state to dest is valid.
	 * If so, the current state is updated to dest. The up-to-date current state is returned.
	 * @param src The source state.
	 * @param dest The destination state.
	 * @return The curent state.
	 */
	public synchronized String transitionState(String dest) {
		if (stateMachine.isTransitionValid(currentState, dest)) {
			currentState = dest;
		}
		
		return currentState;
	}
	
	
	/**
	 * This method sets the current state to dest without concerning if the	transition is valid.
	 * 
	 * Warning: Should only be called to set the first state, or in some cases on that this state machine
	 * do not represent the actual value for the artifact that it is modeling.
	 * @param dest The destination state.
	 */
	public synchronized void setState(String dest) {
		currentState = dest;
	}
	
	public String getState(){
		return currentState;
	}
	
}

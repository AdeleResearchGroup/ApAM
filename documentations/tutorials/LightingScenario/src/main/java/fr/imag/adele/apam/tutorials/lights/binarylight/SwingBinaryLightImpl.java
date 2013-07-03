/**
 * Copyright 2011-2013 Universite Joseph Fourier, LIG, ADELE team
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 * SwingBinaryLightImpl.java - 2 juil. 2013
 */
package fr.imag.adele.apam.tutorials.lights.binarylight;

import java.awt.Color;
import java.util.Iterator;

import fr.imag.adele.apam.tutorials.lights.devices.BinaryLight;
import fr.imag.adele.apam.tutorials.lights.devices.ButtonPressed;

/**
 * @author thibaud
 *
 */
public class SwingBinaryLightImpl extends BinaryLightImpl
							implements BinaryLight {

	/**
	 * @param currentStatus
	 */
	public SwingBinaryLightImpl() {
		super(false);
	}
	
    private javax.swing.JFrame         frame;
    private javax.swing.JLabel        light;
    private String name ="APAM Simple Binary Light";
    private String myLocation;


    public void started() {
    	System.out.println("A light have been started in the "+myLocation);
        frame = new javax.swing.JFrame(name);
        initComponents();
        frame.setVisible(true);
    }

    public void stopped() {
    	System.out.println("A light have been stopped in the "+myLocation);
        if (frame != null) {
            frame.dispose();
            frame = null;
        }
    }

    private void initComponents() {
        frame.setSize(100, 80);
        light= new javax.swing.JLabel();
        light.setOpaque(true);
        light.setText(myLocation);
        setLightOff();
        frame.setLayout(new java.awt.BorderLayout());
        frame.add(light, java.awt.BorderLayout.CENTER);
    }
    
    private void setLightOn() {
        light.setBackground(Color.YELLOW);
        frame.repaint();    	
    }
    
    private void setLightOff() {
        light.setBackground(Color.GRAY);
        frame.repaint();    	
    }
    

	/* (non-Javadoc)
	 * @see fr.imag.adele.apam.tutorials.lights.devices.BinaryLight#setLightStatus(boolean)
	 */
	public void setLightStatus(boolean newStatus) {
		currentStatus=newStatus;
		if(currentStatus)
			setLightOn();
		else setLightOff();
	}

	/* (non-Javadoc)
	 * @see fr.imag.adele.apam.tutorials.lights.devices.BinaryLight#switchLightStatus()
	 */
	public void switchLightStatus() {
		if(currentStatus)
			currentStatus=false;
		else currentStatus=true;
		
		this.setLightStatus(currentStatus);
	}

	
	
	/*TODO: Remove all this when composite work
	public void aButtonHasBeenPressed(ButtonPressed event) {
		System.out.println("SwingBinaryLightImpl.aButtonHasBeenPressed(ButtonPressed event)");
		// Change the lights status
		switchLightStatus();
	}*/

    
}

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
 * SwingButtonImpl.java - 2 juil. 2013
 */
package fr.imag.adele.apam.tutorials.lights.button;

import fr.imag.adele.apam.tutorials.lights.devices.SimpleButton;
import fr.imag.adele.apam.tutorials.lights.devices.messages.ButtonPressed;

import java.awt.event.ActionListener;

/**
 * @author thibaud
 *
 */
public class ButtonImpl implements SimpleButton {
	
    private javax.swing.JFrame         frame;
    private javax.swing.JButton        btn;
    private String name ="APAM Simple Button";
    private String myLocation;
    private String myName;

    public void started() {
    	System.out.println("A button named "+myName+" have been started in the "+myLocation);

    }

    public void stopped() {
    	System.out.println("A button have been stopped in the "+myLocation);
    }


	/* (non-Javadoc)
	 * @see fr.imag.adele.apam.tutorials.lights.devices.SimpleButton#pressButton()
	 */
	public ButtonPressed pressButton() {
        System.out.println("I am button : "+myName+" in "+myLocation+", and I have been pressed !");
		return new ButtonPressed();
	}


    public String getName() {
        return myName;
    }


    public String getLocation() {
        return myLocation;
    }
}

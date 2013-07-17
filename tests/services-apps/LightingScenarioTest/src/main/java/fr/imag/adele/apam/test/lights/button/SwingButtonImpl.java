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
package fr.imag.adele.apam.test.lights.button;

import java.awt.event.ActionListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.test.lights.devices.SimpleButton;
import fr.imag.adele.apam.test.lights.devices.messages.ButtonPressed;

/**
 * @author thibaud
 *
 */
public class SwingButtonImpl implements ActionListener, SimpleButton {
    private static Logger logger = LoggerFactory.getLogger(SwingButtonImpl.class);
	
    private javax.swing.JFrame         frame;
    private javax.swing.JButton        btn;
    private String name ="APAM Simple Button";
    private String myLocation;
    private String myName;

    public void started() {
    	logger.debug(myName+".started(), location : "+myLocation);
    	
        frame = new javax.swing.JFrame(name);
        initComponents();
        //frame.setVisible(true);
    }

    public void stopped() {
	logger.debug(myName+".stopped(), location : "+myLocation);
        if (frame != null) {
            frame.dispose();
            frame = null;
        }
    }

    private void initComponents() {
        frame.setSize(200, 80);
        btn = new javax.swing.JButton("Button "+myLocation);
        btn.addActionListener(this);
        frame.setLayout(new java.awt.BorderLayout());
        frame.add(btn, java.awt.BorderLayout.CENTER);
    }
    
    public void actionPerformed(java.awt.event.ActionEvent e) {
    	pressButton();
    }

	/* (non-Javadoc)
	 * @see fr.imag.adele.apam.test.lights.devices.SimpleButton#pressButton()
	 */
	public ButtonPressed pressButton() {
	    logger.debug(myName+".pressButton(), location : "+myLocation);
	    
		return new ButtonPressed();
	}


    public String getName() {
        return myName;
    }


    public String getLocation() {
        return myLocation;
    }
}

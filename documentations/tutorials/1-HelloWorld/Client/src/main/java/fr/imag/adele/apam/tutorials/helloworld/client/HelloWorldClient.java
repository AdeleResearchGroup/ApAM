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
 * HelloWorldApAM.java - 24 juil. 2013
 */
package fr.imag.adele.apam.tutorials.helloworld.client;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextField;

import fr.imag.adele.apam.tutorials.helloworld.service.HelloWorld;


/**
 * GUI Client for the Hello World Service
 *
 */
public class HelloWorldClient implements ActionListener {
    
    /*
     * This one will be dynamically injected by ApAM,
     * please notice there are no annotation or whatever
     */
    private HelloWorld helloService;
    
    JTextField nameField;
    JFrame myMainFrame;
    
    /**
     * This one will be called when bundle starts using an ApAM callback
     */
    public void start() {
	myMainFrame = new JFrame("ApAM Hello Service Client");
	myMainFrame.setLayout(new BoxLayout(myMainFrame.getContentPane(), BoxLayout.Y_AXIS));
	nameField = new JTextField("Please enter your name...");
	myMainFrame.add(nameField);
	JButton button = new JButton("Call Hello Service !");
	myMainFrame.add(button);
	button.addActionListener(this);
	myMainFrame.pack();
	myMainFrame.setVisible(true);
    }
    
    /**
     * This one will be called when bundle stops using an ApAM callback
     */
    public void stop() {
	myMainFrame.dispose();
    }

    /* (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {
	if(helloService != null)
	    helloService.sayHello(nameField.getText());	
    }
}

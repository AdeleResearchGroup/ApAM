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
 * AdeleTechHeatingApplication.java - 9 juil. 2013
 */
package fr.imag.adele.apam.tutorials.temperature.adeletech.services;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import fr.imag.adele.apam.tutorials.temperature.devices.Heater;
import fr.imag.adele.apam.tutorials.temperature.devices.TemperatureSensor;

/**
 * @author thibaud
 *
 */
public class AdeleTechHeatingApplication  implements ChangeListener, ActionListener{
	
	JFrame mainWindow;
	String myName;
	JLabel labelTemperature;
	JLabel labelHeater;
	JSlider userSetting;
	JTextField labelSetting;
	
	TemperatureSensor mySensor;
	Heater myHeater;
	
	
	public void start() {
		System.out.println("GenericHeatingApplication : "
				+myName+".start()");
		mainWindow=new JFrame(myName);
		labelTemperature =new JLabel();
		labelHeater = new JLabel();
		labelSetting=new JTextField();
		
		initComponents();
		mainWindow.pack();
		mainWindow.setVisible(true);
	}
	
	public void stop() {
		System.out.println("GenericHeatingApplication : "
				+myName+".stop()");
		if(mainWindow != null)
				mainWindow.dispose();
		mainWindow = null;
		
	}
	
	void initComponents() {

		mainWindow.setLayout(new BoxLayout(mainWindow.getContentPane(), BoxLayout.Y_AXIS));
		
		JPanel panelSensor = new JPanel();
		panelSensor.setBorder(BorderFactory.createTitledBorder("Temperature Sensor"));
		labelTemperature.setPreferredSize(new Dimension(200,30));
		updateTemperature();
		panelSensor.add(labelTemperature);
		
		
		JPanel panelUserSetting = new JPanel();
		panelUserSetting.setBorder(BorderFactory.createTitledBorder("User Temperature Setting"));
		panelUserSetting.setLayout(new BoxLayout(panelUserSetting, BoxLayout.Y_AXIS));
		userSetting = new JSlider(10,40,21);
		userSetting.addChangeListener(this);
		labelSetting.setEnabled(false);
		labelSetting.setText("21°C");
		labelSetting.setHorizontalAlignment(JTextField.CENTER);
		panelUserSetting.add(labelSetting);
		panelUserSetting.add(userSetting);
	
		
		
		JPanel panelHeater = new JPanel();
		panelHeater.setBorder(BorderFactory.createTitledBorder("Heater Activity"));
		labelHeater.setPreferredSize(new Dimension(200,30));
		updateHeater();
		panelHeater.add(labelHeater);

		JPanel panelTemp = new JPanel();
		JButton refreshStatus = new JButton("Refresh Status");
		refreshStatus.setPreferredSize(new Dimension(200,30));
		refreshStatus.addActionListener(this);
		panelTemp.add(refreshStatus);
		
		mainWindow.add(panelSensor);
		mainWindow.add(panelUserSetting);
		mainWindow.add(panelHeater);
		mainWindow.add(panelTemp);
	}
	
	void updateTemperature() {
		if (mySensor!=null)
			labelTemperature.setText(mySensor.getCurrentTemperature()+"°C");
		else labelTemperature.setText("No Temperature Sensor");
	}
	
	void updateHeater() {
		if(myHeater!= null) {
			if (mySensor!=null) {
				if (mySensor.getCurrentTemperature()>=userSetting.getValue()) {
					System.out.println("Current temperature is too high, turning off the Heater");
					myHeater.turnOff();				
				} else {
					System.out.println("Current temperature is too low, turning on the Heater");
					myHeater.turnOn();				
				}
			}
			if(myHeater.getStatus())
				labelHeater.setText("Heater is on");
			else labelHeater.setText("Heater is off");
		}
		else labelHeater.setText("No Heater");
	}

	/* (non-Javadoc)
	 * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
	 */
	public void stateChanged(ChangeEvent e) {
		labelSetting.setText(userSetting.getValue()+"°C");
		updateTemperature();
		updateHeater();
	}

	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		updateTemperature();
		updateHeater();
		
	}
	

}

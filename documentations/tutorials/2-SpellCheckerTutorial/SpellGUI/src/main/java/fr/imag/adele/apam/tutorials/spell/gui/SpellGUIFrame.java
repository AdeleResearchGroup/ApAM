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
 * SpellGUI.java - 26 juin 2013
 */
package fr.imag.adele.apam.tutorials.spell.gui;

import java.awt.Font;
import java.awt.event.ActionListener;

import fr.imag.adele.apam.tutorials.spell.checker.SpellChecker;

/**
 * 
 * 
 * @author thibaud
 *
 */
public class SpellGUIFrame implements ActionListener{
	
    private javax.swing.JFrame         frame;
    private javax.swing.JTextArea     textAreaEnter;
    private javax.swing.JLabel     textAreaReturn;    
    private javax.swing.JButton        checkBtn;
    private String name ="APAM Spell Checker Tutorial";
    
    private SpellChecker spellCheckerServiceProvider;
    private String currentLanguage;

    public void start() {
    	System.out.println("SpellGUIFrame Service Started");
        frame = new javax.swing.JFrame(name);
        initComponents();
        frame.setVisible(true);
    }

    public void stop() {
    	System.out.println("SpellGUIFrame Service Stopped");

        if (frame != null) {
            frame.dispose();
            frame = null;
        }
    }

    private void initComponents() {
        frame.setSize(600, 200);
        textAreaEnter = new javax.swing.JTextArea("Enter your passage here...",5,100);
        textAreaEnter.setFont(new Font("Serif", Font.ITALIC, 16));
        textAreaReturn = new javax.swing.JLabel(" | No results for the moment !");
        
        checkBtn = new javax.swing.JButton("Check Text !");
        
        checkBtn.addActionListener(this);
        frame.setLayout(new java.awt.BorderLayout());
        frame.add(textAreaEnter, java.awt.BorderLayout.NORTH);
        frame.add(checkBtn, java.awt.BorderLayout.CENTER);
        frame.add(textAreaReturn, java.awt.BorderLayout.SOUTH);
    }
    
    public void actionPerformed(java.awt.event.ActionEvent e) {
        String passage = textAreaEnter.getText();
        String[] wrongs = spellCheckerServiceProvider.check(passage);
        String results= "Current Language : "+currentLanguage+" | Wrong words : ";
        for(int i=0;i<wrongs.length;i++)
        	results+=" - "+wrongs[i];
        textAreaReturn.setText(results);
    }	

}

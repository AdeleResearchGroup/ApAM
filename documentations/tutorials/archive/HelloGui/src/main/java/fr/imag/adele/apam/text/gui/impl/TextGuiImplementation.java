/**
 * Copyright 2011-2012 Universite Joseph Fourier, LIG, ADELE team
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
 */
package fr.imag.adele.apam.text.gui.impl;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextField;

import fr.imag.adele.apam.hello.world.spec.HelloService;

public class TextGuiImplementation {

    private JFrame         frame;
    private JTextField     textField;
    private JButton        callBtn;
    private String name ="APAM Say Hello";
    
    private HelloService hello;

    public void started() {
        frame = new JFrame(name);
        initComponents();
        frame.setVisible(true);
    }

    public void stopped() {
        if (frame != null) {
            frame.dispose();
            frame = null;
        }
    }

    private void initComponents() {
        frame.setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.setPreferredSize(new Dimension(400, 70));
        frame.setSize(400, 70);
        frame.setLocationRelativeTo(null);

        textField = new JTextField();

        callBtn = new JButton("Call");
        callBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                String msg = textField.getText();
                if (msg != null & msg.trim().length() > 0) {
                    String lang = hello.getLang();
                    if (lang!=null){
                        frame.setTitle(name + " in " + lang );
                        frame.repaint();
                        hello.sayHello(msg);
                    }

                    textField.setText("");
                }
            }
        });

        frame.setLayout(new BorderLayout());

        frame.add(textField, BorderLayout.CENTER);
        frame.add(callBtn, BorderLayout.EAST);

        frame.getRootPane().setDefaultButton(callBtn);
    }
}

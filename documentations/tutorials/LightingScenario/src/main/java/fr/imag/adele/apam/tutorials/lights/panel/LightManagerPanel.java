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
 * DeviceManagerPanel.java - 3 juil. 2013
 */
package fr.imag.adele.apam.tutorials.lights.panel;

import fr.imag.adele.apam.tutorials.lights.devices.BinaryLight;
import fr.imag.adele.apam.tutorials.lights.devices.SimpleButton;
import fr.imag.adele.apam.tutorials.lights.devices.messages.LightStatusChanged;


import javax.swing.*;
import javax.swing.plaf.DimensionUIResource;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.Set;

/**
 * @author thibaud
 *
 */
public class LightManagerPanel {
	
    private javax.swing.JFrame         frame;
    private javax.swing.JButton        btn;
    private String name ="APAM Light Manager Panel";
    Set<BinaryLight> theLights;
    Set<SimpleButton> theButtons;

    private JPanel buttonsColumn;
    private JPanel lightsColumn;


    public void started() {
    	System.out.println("LightManagerPanel.started()");
    	
        frame = new javax.swing.JFrame(name);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        buttonsColumn = new JPanel();
        buttonsColumn.setLayout(new BoxLayout(buttonsColumn, BoxLayout.Y_AXIS));
        buttonsColumn.setBorder(BorderFactory.createTitledBorder("Buttons"));

        lightsColumn = new JPanel();
        lightsColumn.setLayout(new BoxLayout(lightsColumn, BoxLayout.Y_AXIS));
        lightsColumn .setBorder(BorderFactory.createTitledBorder("Lights"));

        frame.setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.X_AXIS));

        rebuildLightsColumn();
        rebuildButtonsColumn();
        repackFrame();

    }

    private void repackFrame() {
        //frame.setVisible(false);
        //frame.removeAll();

        frame.add(buttonsColumn);
        frame.add(lightsColumn);
//        frame.add(new JLabel("toto"));


        frame.pack();
        frame.setVisible(true);
    }

    public void stopped() {
    	System.out.println("LightManagerPanel.stopped()");
    	
        if (frame != null) {
            frame.dispose();
            frame = null;
        }
    }


    public void aLightStatusHasChanged(LightStatusChanged event) {
        rebuildLightsColumn();
    }

    /**
     * CallBack method on dependency resolution
     * @param button
     */

    public void newButton() {
        rebuildButtonsColumn();
    }


    /**
     * CallBack method on dependency resolution
     */
    public void removeButton() {
        rebuildButtonsColumn();
    }

    /**
     * CallBack method on dependency resolution
     * @param light
     */
    public void newLight() {
        rebuildLightsColumn();
    }

    /**
     * CallBack method on dependency resolution
     */
    public void removeLight() {
        rebuildLightsColumn();
    }

    private void rebuildButtonsColumn() {
        System.out.println("LightManagerPanel.rebuildButtonsColumn(), found "
                +theButtons.size()+" buttons to add to the list");

        buttonsColumn.removeAll();
        if (theButtons != null && theButtons.size()>0) {
            Iterator<SimpleButton> it=theButtons.iterator();
            while(it.hasNext()) {
                final SimpleButton btn= it.next();
                JPanel panel=new JPanel();
                panel.setBorder(BorderFactory.createTitledBorder(
                        btn.getName()+" in "+btn.getLocation()));
                JButton btnGUI = new JButton("Press !");
                btnGUI.setPreferredSize(new DimensionUIResource(180, 40));

                btnGUI.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        System.out.println("A button has been clicked : "+btn.getName());
                        btn.pressButton();
                    }
                });

                panel.add(btnGUI);
                buttonsColumn.add(panel);
            }

        }

        repackFrame();

    }

    private void rebuildLightsColumn() {
        System.out.println("LightManagerPanel.rebuildLightsColumn(), found "
                +theLights.size()+" lights to add to the list");

        lightsColumn.removeAll();
        if (theLights != null && theLights.size()>0) {
            Iterator<BinaryLight> it=theLights.iterator();
            while(it.hasNext()) {
                BinaryLight light = it.next();
                JPanel panel=new JPanel();
                panel.setBorder(BorderFactory.createTitledBorder(
                        light.getName() + " in " + light.getLocation()));

                JLabel lightGUI= new javax.swing.JLabel();
                lightGUI.setOpaque(true);
                lightGUI.setPreferredSize(new DimensionUIResource(180, 40));

                if(light.isLightOn())
                    lightGUI.setBackground(Color.YELLOW);
                else lightGUI.setBackground(Color.GRAY);
                panel.add(lightGUI);
                lightsColumn.add(panel);
            }
        }



        repackFrame();
    }

}

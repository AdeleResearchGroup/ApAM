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
    private String name;
    
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
                    hello.sayHello(msg);
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

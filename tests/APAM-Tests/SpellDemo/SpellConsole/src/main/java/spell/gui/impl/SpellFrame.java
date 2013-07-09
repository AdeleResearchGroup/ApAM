package spell.gui.impl;


import java.awt.Font;
import java.awt.event.ActionListener;
import java.util.Set;

import spell.check.SpellCheck ;


public class SpellFrame implements ActionListener{
	private javax.swing.JFrame         frame;
	private javax.swing.JTextArea     textAreaEnter;
	private javax.swing.JLabel     textAreaReturn;    
	private javax.swing.JButton        checkBtn;
	private String name ="APAM Spell Checker Tutorial";

	private SpellCheck spellCheckerServiceProvider;
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
		Set<String> wrongs = spellCheckerServiceProvider.check(passage);
		String results= "Current Language : "+currentLanguage+" | Wrong words : ";
		for(String wrong : wrongs)
			results+=" - "+ wrong;
		textAreaReturn.setText(results);
	}	


}

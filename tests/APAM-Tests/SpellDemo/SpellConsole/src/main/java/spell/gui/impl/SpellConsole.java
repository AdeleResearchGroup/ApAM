
package spell.gui.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Set;

import spell.check.SpellCheck ;
import fr.imag.adele.apam.ApamComponent;
import fr.imag.adele.apam.Instance;

public class SpellConsole implements Runnable, ApamComponent {

	SpellCheck spell ;

	public String getLine() {
		System.out.println("Enter something here : ");
		try{
			BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
			return bufferRead.readLine();
		}
		catch(IOException e) {e.printStackTrace();}
		return "no line" ;
	}

	public void run() {
		System.out.println("=== executing  SpellConsole ");
//		String line = getLine () ;
//		System.out.println("read : " + line);
		Set<String> errors = spell.check ("une ligne a voir") ;
		System.out.println(errors);
	}

	public void apamInit(Instance inst) {
		new Thread(this, "Spell Demo").start();
	}

	public void apamRemove() {
	}

}

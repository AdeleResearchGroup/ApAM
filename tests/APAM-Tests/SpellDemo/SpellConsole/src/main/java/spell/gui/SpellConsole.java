
package spell.gui;

import java.util.Set;

import spell.check.SpellCheck ;
import fr.imag.adele.apam.ApamComponent;
import fr.imag.adele.apam.Instance;

public class SpellConsole implements Runnable, ApamComponent {

	SpellCheck spell ;

	public void run() {
		System.out.println("=== executing  SpellConsole ");
		Set<String> errors = spell.check ("essai pour voir") ;
		System.out.println(errors);
	}

	public void apamInit(Instance inst) {
		new Thread(this, "Spell Demo").start();
	}

	public void apamRemove() {
	}

}

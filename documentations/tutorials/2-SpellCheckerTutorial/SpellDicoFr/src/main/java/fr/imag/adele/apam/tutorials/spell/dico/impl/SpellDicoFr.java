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
 * SpellDicoFr.java - 26 juin 2013
 */
package fr.imag.adele.apam.tutorials.spell.dico.impl;

import java.util.HashSet;
import java.util.Set;
import fr.imag.adele.apam.tutorials.spell.dico.spec.SpellDico;

/**
 * @author thibaud
 *
 */
public class SpellDicoFr implements SpellDico{
	
	private Set<String> FrenchDictionary;
	
	
	/**
	 * Fill the local dictionary with a set of word inside a file (separated with '\n' characters)
	 * @param dicoFilename
	 */
	private void fillDicoFr(String dicoFilename) {
		// TODO: Parse a txt file of French words to add to the Set		
	}
	
	public SpellDicoFr() {
		FrenchDictionary = new HashSet<String>();
		// Adding a few words for testing purpose
		FrenchDictionary.add("le");
		FrenchDictionary.add("petit");
		FrenchDictionary.add("chat");
		FrenchDictionary.add("est");
		FrenchDictionary.add("mort");

		fillDicoFr("dico.fr");	
	}
	
	/**
	 * @param word is supposed to be a word in french
	 * @return true if word is defined in the dictionary
	 */
	public boolean check(String word) {
		return FrenchDictionary.contains(word);
	}
	
    //Called by APAM when an instance of this implementation is created
    public void start(){
        System.out.println("SpellDicoFr Service Started");
    }
    
    // Called by APAM when an instance of this implementation is removed
    public void stop(){
        System.out.println("SpellDicoFr Service Stopped");
    }	

	

}

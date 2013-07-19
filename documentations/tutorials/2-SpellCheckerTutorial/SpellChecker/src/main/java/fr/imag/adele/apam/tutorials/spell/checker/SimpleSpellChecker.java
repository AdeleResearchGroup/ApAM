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
 * SimpleSpellChecker.java - 26 juin 2013
 */
package fr.imag.adele.apam.tutorials.spell.checker;

import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import fr.imag.adele.apam.tutorials.spell.dico.spec.SpellDico;


/**
 * Very simple implementation of SpellChecker
 * @author thibaud
 *
 */
public class SimpleSpellChecker implements SpellChecker {
	
	private SpellDico dictionnaryProvider ;


	/* (non-Javadoc)
	 * @see fr.imag.adele.apam.tutorials.spell.checker.SpellChecker#check(java.lang.String)
	 */
	public String[] check(String passage) {
		if ((passage == null) || (passage.length() == 0))
			return null;
		Set<String> errorList = new HashSet<String>(); 
	     
		StringTokenizer st = new StringTokenizer(passage, " ,.!?;:"); 
	    while (st.hasMoreTokens()) {
	    	String word = st.nextToken();
	    	if (!dictionnaryProvider.check(word))
	    		errorList.add(word);
	    }
	    
	    return errorList.toArray(new String[0]);
	}
	
    //Called by APAM when an instance of this implementation is created
    public void start(){
        System.out.println("SimpleSpellChecker Service Started");
    }
    
    // Called by APAM when an instance of this implementation is removed
    public void stop(){
        System.out.println("SimpleSpellChecker Service Stopped");
    }		
	
}

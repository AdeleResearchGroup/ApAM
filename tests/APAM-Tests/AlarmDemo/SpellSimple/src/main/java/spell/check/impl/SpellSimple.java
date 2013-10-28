package spell.check.impl; 
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import Alarm.DisplayAlarm;

import spell.dico.SpellDico;

public class SpellSimple implements DisplayAlarm { 
	private SpellDico dictionary ;   
	
	public Set<String> check(String passage) { 
		if ((passage == null) || (passage.length() == 0)) { 
			return null; 
			} 
		
		Set<String> errorList = new HashSet<String> () ; 
		StringTokenizer st = new StringTokenizer(passage, " ,.!?;:"); 
		while (st.hasMoreTokens()) { 
			String word = st.nextToken(); 
			if (! dictionary.check(word)) {   
				errorList.add(word); } 
		}
		return errorList ;
	} 
}

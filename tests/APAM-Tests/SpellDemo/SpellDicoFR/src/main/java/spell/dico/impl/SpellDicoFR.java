package spell.dico.impl; 

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import spell.dico.SpellDico ;
 
public class SpellDicoFR implements SpellDico { 
      Set<String> dictionary = new HashSet <String> (Arrays.asList("un", "mot", "utile")) ;

      public boolean check (String word) {
            return dictionary.contains(word) ;
      } 
}

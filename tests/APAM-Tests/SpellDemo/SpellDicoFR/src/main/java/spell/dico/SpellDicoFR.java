package spell.dico; 

import java.util.HashSet;
import java.util.Set;
import spell.dico.SpellDico ;
  //import spell.dico.SpellDico; 

public class SpellDicoFR implements SpellDico { 
      Set<String> dictionary = new HashSet <String> () ;
      public boolean check (String word) {
            return dictionary.contains(word) ;
      } 
}

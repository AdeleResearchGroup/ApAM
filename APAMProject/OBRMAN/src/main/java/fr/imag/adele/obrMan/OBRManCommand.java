package fr.imag.adele.obrMan;

import java.io.IOException;
import java.net.URL;


public interface OBRManCommand {

    public String printCompositeRepositories(String compositeTypeName);
    
    public void setInitialConfig(URL modellocation) throws IOException;

}

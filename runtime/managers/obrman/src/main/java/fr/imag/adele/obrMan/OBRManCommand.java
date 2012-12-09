package fr.imag.adele.obrMan;

import java.io.IOException;
import java.net.URL;
import java.util.Set;


public interface OBRManCommand {

    public Set<String> getCompositeRepositories(String compositeTypeName);
    
    public void setInitialConfig(URL modellocation) throws IOException;
    
    public boolean updateRepos(String compositeName);

}

package fr.imag.adele.obrMan.internal;

/**
 * Copyright Universite Joseph Fourier (www.ujf-grenoble.fr)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.PrintWriter;
import java.util.Set;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceProperty;

import fr.imag.adele.obrMan.OBRManCommand;

/**
 * 
 * 
 * @author Mehdi
 */
@Instantiate
@Component(publicFactory = false, immediate = true, name = "obrman.shell")
@Provides(specifications = OBRManShell.class)
public class OBRManShell {

    @ServiceProperty(name="org.knowhowlab.osgi.shell.group.id",value ="obrman" )
    String universalShell_groupID;

    @ServiceProperty(name="org.knowhowlab.osgi.shell.group.name",value ="OBR Manager Commands" )
    String universalShell_groupName;

    @ServiceProperty(name="org.knowhowlab.osgi.shell.commands", value="{}")
    String[] universalShell_groupCommands = new String[] {
            "cr#cr - list repositories of a composite ",
            "ur#ur - update resources from repositories"
    };



    // ipojo injected
    @Requires
    OBRManCommand obrmanCommand;

    /**
     * compositeRepositories
     * list repositories of a compositeType
     */

    public void cr(PrintWriter out, String... args) {
        String compositeTypeName = args[0];
        String result = "";
        Set<String> repositories = obrmanCommand.getCompositeRepositories(compositeTypeName);
        result += (compositeTypeName + " (" + repositories.size() + ") : \n");
        for (String repository : repositories) {
            result += ("    >> " + repository + "\n");
        }
        out.println(result);
    }
    
    public void ur(PrintWriter out, String... args) {
       String compositeTypeName = args[0];
       boolean state = obrmanCommand.updateRepos(compositeTypeName);
       if (state){
           out.println("Update " + compositeTypeName + " repositories performed");
       }else {
           out.println("Update " + compositeTypeName + " repositories failed");
       }

    }
    
}

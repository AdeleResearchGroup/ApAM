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

    /**
     * Defines the command scope (obrman).
     */
    @ServiceProperty(name = "osgi.command.scope", value = "obrman")
    String        m_scope;

    /**
     * Defines the functions (commands).
     */
    @ServiceProperty(name = "osgi.command.function", value = "{}")
    String[]      m_function = new String[] { "cr" };

    // ipojo injected
    @Requires
    OBRManCommand obrmanCommand;

    /**
     * compositeRepositories
     */
//    @Descriptor("list repositories of a compositeType")
//    @Descriptor("the name of the compositeType ") 
    public void cr(String compositeTypeName) {
        String result = "";
        Set<String> repositories = obrmanCommand.getCompositeRepositories(compositeTypeName);
        result += (compositeTypeName + " (" + repositories.size() + ") : \n");
        for (String repository : repositories) {
            result += ("    >> " + repository + "\n");
        }
        System.out.println(result);
    }
}

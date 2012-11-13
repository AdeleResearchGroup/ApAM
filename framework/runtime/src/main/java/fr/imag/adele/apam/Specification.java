package fr.imag.adele.apam;

import java.util.Set;
import fr.imag.adele.apam.apform.ApformSpecification;

public interface Specification extends Component {

    /**
     * return the apform specification associated with this specification.
     * 
     * @return
     */
    public ApformSpecification getApformSpec();

    /**
     * remove from ASM but does not try to delete in SAM. It deletes all its
     * Implementations. No change of state. May be selected again later.
     */
    //    public void remove();

    /**
     * Return the implementation that implement that specification and has the provided name.
     * 
     * @param implemName the name
     * @return the implementation
     */
    public Implementation getImpl(String implemName);

    /**
     * Return all the implementation of that specification. If no services implementation are found,
     * returns null.
     * 
     * @return the implementations
     * @throws ConnectionException the connection exception
     */
    public Set<Implementation> getImpls();

    /**
     * Return the list of currently required specification.
     * WARNING : does not include required interfaces and messages.
     * 
     * @return the list of currently required specification. Null if none
     */
    public Set<Specification> getRequires();

    /**
     * Return the list of specification that currently require that spec.
     * 
     * @return the list of specifications using that spec. Null if none
     * @throws ConnectionException the connection exception
     */
    public Set<Specification> getInvRequires();

}

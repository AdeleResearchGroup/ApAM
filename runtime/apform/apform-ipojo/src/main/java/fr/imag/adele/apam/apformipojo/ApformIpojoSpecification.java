package fr.imag.adele.apam.apformipojo;

import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.HandlerManager;
import org.apache.felix.ipojo.IPojoContext;
import org.apache.felix.ipojo.metadata.Element;
import org.osgi.framework.BundleContext;

import fr.imag.adele.apam.Apam;
import fr.imag.adele.apam.apform.Apform2Apam;
import fr.imag.adele.apam.apform.ApformSpecification;
import fr.imag.adele.apam.declarations.SpecificationDeclaration;
import fr.imag.adele.apam.impl.ComponentBrokerImpl;

public class ApformIpojoSpecification extends ApformIpojoComponent implements ApformSpecification {

    /**
     * Build a new factory with the specified metadata
     *
     * @param context
     * @param metadata
     * @throws ConfigurationException
     */
    public ApformIpojoSpecification(BundleContext context, Element metadata) throws ConfigurationException {
        super(context, metadata);

    }

    @Override
    public SpecificationDeclaration getDeclaration() {
        return (SpecificationDeclaration) super.getDeclaration();
    }

    @Override
    public boolean hasInstrumentedCode() {
        return false;
    }

    /**
     * Gets the class name.
     *
     * @return the class name.
     * @see org.apache.felix.ipojo.IPojoFactory#getClassName()
     */
    @Override
    public String getClassName() {
        return this.getDeclaration().getName();
    }

    @Override
    public boolean isInstantiable() {
        return false;
    }

    @Override
    public ApformIpojoInstance createApamInstance(IPojoContext context, HandlerManager[] handlers) {
        throw new UnsupportedOperationException("APAM specification is not instantiable");
    }


    /**
     * Register this implementation with APAM
     */
    @Override
    protected void bindToApam(Apam apam) {
        Apform2Apam.newSpecification(this);
    }

    /**
     * Unregister this implementation from APAM
     *
     * @param apam
     */
    @Override
    protected void unbindFromApam(Apam apam) {
        // Apform2Apam.vanishSpecification(getName());
        ComponentBrokerImpl.disappearedComponent(getName()) ;

    }

    @Override
    public void setProperty(String attr, String value) {
        // TODO Auto-generated method stub

    }


}
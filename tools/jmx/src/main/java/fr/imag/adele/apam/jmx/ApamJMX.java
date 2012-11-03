package fr.imag.adele.apam.jmx;


public interface ApamJMX {

    public String test();

    /**
     * ASMSpec.
     * 
     * @param specificationName
     *            the specification name
     */

    public String up(String componentName);

    /**
     * Resolver Commands.
     */

    public String put(String componentName);

    public String put(String componentName, String compositeTarget);

    /**
     * Specifications.
     * @return 
     */

    public String specs();

    /**
     * Implementations.
     */
    public String implems();

    /**
     * Instances.
     */
    public String insts();

    /**
     * ASMSpec.
     * 
     * @param specificationName
     *            the specification name
     */
    public String spec(String specificationName);

    /**
     * ASMImpl.
     * 
     * @param implementationName
     *            the implementation name
     */

    public String implem(String implementationName);

    public String l(String componentName);

    public String launch(String componentName, String compositeTarget);

    /**
     * ASMInst.
     * 
     * @param implementationName
     *            the implementation name
     */

    public String inst(String instanceName);

    public String applis();

    public String appli(String appliName);

    public String dump();

    public String pending();

    public String compoTypes();

    public String compoType(String name);

    public String compos();

    public String compo(String compoName);

    public String wire(String instName);

}
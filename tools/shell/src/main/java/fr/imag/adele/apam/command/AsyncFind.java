package fr.imag.adele.apam.command;

import fr.imag.adele.apam.*;

/**
 * Created with IntelliJ IDEA.
 * User: Mehdi
 * Date: 05/12/12
 * Time: 17:44
 * To change this template use File | Settings | File Templates.
 */
public class AsyncFind implements Runnable {

    private String componentName;

    private Component component;

    private Composite target;

    private boolean instantiate;
    public AsyncFind(Component component, Composite target, String componentName, boolean b) {
       this.component = component;
        this.target = target;
        this.componentName = componentName;
        this.instantiate = b;
    }



    @Override
    public void run() {

        component= CST.apamResolver.findComponentByName(target, componentName);
        if (component!=null){
            System.out.println(">> " + component.getName() + " deployed!");
            if (instantiate){
                if (component instanceof Implementation)
                    ((Implementation)component).createInstance(target,null);
                if (component instanceof Specification) {
                    Implementation impl = CST.apamResolver.resolveSpecByName(target, componentName, null, null) ;
                    if (impl != null)
                        impl.createInstance(null, null);
                }
            }
        }

        else
            System.out.println(">> Deployment failed for " + componentName);


    }
}

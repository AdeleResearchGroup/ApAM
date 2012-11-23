package fr.imag.adele.obrMan.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.bundlerepository.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StackLoading {
    
    

//  if (selected == null)
//      return null;
//
//  String name = selected.getComponentName();
//  fr.imag.adele.apam.Component c = CST.componentBroker.getComponent(name);
//  boolean deployed = false;
//  // Check if already deployed
//  if (c == null) {
//     
////      if (!stack.isOnloadingComponent(name)) {
////          stack.loadUnloadComponent(name,true);
////      }
//      logger.debug("Start verifying resource : " + selected.resource.getSymbolicName() + " for : " + name);
//      if (stack.isOnloadingResource(selected.resource)) { // check if the resource still
//                                                          // loading
//          logger.debug("The " + selected.getResource().getSymbolicName() + " which contains " + name
//                  + " is on loading!");
//          stack.loadAndLoaded(selected.resource,name,true);
//          deployed = true;
//      } else if (alreadyDeployed(selected)) {// check if the resource is already deployed
//          logger.debug("The component " + name + " is on an already installed resource : "
//                  + selected.getResource().getSymbolicName());
//          stack.loadAndLoaded(selected.resource,name,false);
//          return CST.componentBroker.getComponent(name);
//      } else {
//          // deploy selected resource
//          stack.loadAndLoaded(selected.resource,name,true);
//          deployed = selected.obrManager.deployInstall(selected);
//      }
//      if (!deployed) {
//          logger.error("could not install resource ");
//          stack.loadAndLoaded(selected.resource, name,false);
//          ObrUtil.printRes(selected.resource);
//          return null;
//      }
//
//      // wait loading of all components of the resource installed
//      componentLoading(name, selected.resource);
//      List<String> loadingCompos = new ArrayList<String>();
//      loadingCompos.addAll(stack.getOnLoadingComponents(selected.resource));
//      for (String componentName : loadingCompos) {
//          if (!stack.isSyncCompo(componentName)) {                    
//              synchronized (componentName) {
//                  stack.setSyncComponent(componentName, true);
//                  logger.debug("Waiting on " + componentName + " for " + name);
//                  CST.componentBroker.getWaitComponent(componentName);
//                  logger.debug("End Waiting on " + componentName);
////                  stack.loadUnloadComponent(componentName,false);
//              }
//              stack.setSyncComponent(componentName, false);
//          }
//      }
//      // waiting for the component to be ready in Apam.
//      c = CST.componentBroker.getWaitComponent(name);
//      stack.loadAndLoaded(selected.resource, name,false);
//
//  } else { // do not install twice.
//      // It is a logical deployment. The already existing component is not visible !
//      // System.err.println("Logical deployment of : " + name + " found by OBRMAN but allready deployed.");
////      stack.loadUnloadComponent(name,false);
//  }
//  logger.debug("Finish Loading Resource : " + selected.resource.getSymbolicName() + " for : " + name);
//  return c;
    

    Logger                            logger                       = LoggerFactory.getLogger(getClass());

    // private List<String> onLoadingResource = new ArrayList<String>();
    private Map<String, List<String>> onLoadingComponentByResource = new HashMap<String, List<String>>();
//    private List<String>         onLoadingComponent        = new ArrayList<String>();
    private Map<String, Boolean>      onSynchronizedComponent      = new HashMap<String, Boolean>();

    private void loading(Resource resource, String component) {
        List<String> onLoadingComponent = onLoadingComponentByResource.get(resource.getSymbolicName());
        if (onLoadingComponent == null) {
            logger.debug("[First Time Loading Resource]" + resource.getSymbolicName());
            onLoadingComponent = new ArrayList<String>();
            if (component != null) {
                onLoadingComponentByResource.put(resource.getSymbolicName(), onLoadingComponent);
            }
        }

        if (!onLoadingComponent.contains(component)) {
            onLoadingComponent.add(component);
        }

//        for (String res : onLoadingResource) {
//            logger.debug("[Loading Resource]" + res + " [" + onLoadingResourceRequests.get(res) + "]");
//        }
    }

//    private void loadingComponent(String component) {
//        onLoadingComponent.add(component);
//        logger.debug("[Components On Loading] : " + onLoadingComponent);
//    }

    private void loaded(Resource resource, String component) {
        boolean removed = false;
        List<String> onLoadingComponent = onLoadingComponentByResource.get(resource.getSymbolicName());
        if (onLoadingComponent != null) {
            if (onLoadingComponent.contains(component)) {
                onLoadingComponent.remove(component);
            }

            if (onLoadingComponent.size() == 0) {
                onLoadingComponentByResource.keySet().remove(resource.getSymbolicName());
            }
        }
//        if (onLoadingResourceRequests.get(resource.getSymbolicName()) != null) {
//            onLoadingResourceRequests.put(resource.getSymbolicName(), onLoadingResourceRequests.get(resource
//                    .getSymbolicName()) - 1);
//            if (onLoadingResourceRequests.get(resource.getSymbolicName()) == 0) {
//                onLoadingResource.remove(resource.getSymbolicName());
//                removed = true;
//            }
//        }
//        logger.debug("Loaded Resource]" + resource.getSymbolicName() + " ["
//                + onLoadingResourceRequests.get(resource.getSymbolicName()) + "]");
//        for (String res : onLoadingResource) {
//            logger.debug("[Loading Resource]" + res + " [" + onLoadingResourceRequests.get(res) + "]");
//        }
//        return removed;
    }

//
//    private void componentLoaded(String component) {
//        onLoadingComponent.remove(component);
//        logger.debug("[Loaded Component]" + component);
//        logger.debug("Component On Loading : " + onLoadingComponent);
//    }

    public boolean isOnloadingResource(Resource resource) {
        return onLoadingComponentByResource.keySet().contains(resource.getSymbolicName());

    }

    public void loadAndLoaded(Resource r, String component, boolean state) {
        synchronized (component) {
            if (state) {
                loading(r, component);
            }
            else {
                loaded(r, component);
            }
        }
//        loadUnloadResource(r, component, false);
    }

//    public boolean isOnloadingComponent(Resource resource, String component) {
//        
//        boolean result = onLoadingComponent.contains(component);
//        logger.debug("[" + result + "] is " + component + " in " + onLoadingComponent);
//
//        return result;
//    }

    public void setSyncComponent(String name, Boolean value) {
        synchronized (name) {
            onSynchronizedComponent.put(name, value);
        }
        
    }

    public Boolean isSyncCompo(String name) {
        if (onSynchronizedComponent.get(name) != null && onSynchronizedComponent.get(name)) {
            return onSynchronizedComponent.get(name);
        }
        return false;
    }

    public List<String> getOnLoadingComponents(Resource resource) {
        return onLoadingComponentByResource.get(resource.getSymbolicName());
    }
//
//    public synchronized void loadUnloadComponent(String c, boolean state) {
//        if (state) {
//            loadingComponent(c);
//        }
//        else {
//            componentLoaded(c);
//        }
//    }
//
//    public synchronized void loadUnloadResource(Resource r, String component, boolean state) {
//        if (state) {
//            loadingResource(r);
//        }
//        else {
//            if (resourceLoaded(r)) {
//                if (component != null)
//                    loadUnloadComponent(component, false);
//            }
//        }
//    }

}

package fr.imag.adele.apam.ASMImpl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import fr.imag.adele.am.MachineID;
import fr.imag.adele.am.eventing.AMEvent;
import fr.imag.adele.am.eventing.AMEventingHandler;
import fr.imag.adele.am.exception.ConnectionException;
import fr.imag.adele.am.query.Query;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.apamAPI.ASMInst;
import fr.imag.adele.apam.apamAPI.ApamDependencyHandler;
import fr.imag.adele.apam.apamAPI.DynamicManager;
import fr.imag.adele.sam.Implementation;
import fr.imag.adele.sam.InstPID;
import fr.imag.adele.sam.Instance;
import fr.imag.adele.sam.Specification;
import fr.imag.adele.sam.event.EventProperty;

public class SamInstEventHandler implements AMEventingHandler {

    // The managers are waiting for the apparition of an instance of the ASMImpl or implementing the interface
    // In both case, no ASMInst is created.
    static Map<String, Set<DynamicManager>> expectedImpls      = new HashMap<String, Set<DynamicManager>>();
    static Map<String, Set<DynamicManager>> expectedInterfaces = new HashMap<String, Set<DynamicManager>>();

    // registers the managers that are interested in services that disappear.
    static Set<DynamicManager>              listenLost         = new HashSet<DynamicManager>();

    // contains the apam instance that registered to APAM but not yet created in ASM
    static Map<String, NewApamInstance>     newApamInstance    = new HashMap<String, NewApamInstance>();
    // contains the Sam instance that has been notified before the Apam handler
    static Map<String, NewSamInstance>      newSamInstance     = new HashMap<String, NewSamInstance>();

    static public SamInstEventHandler       theInstHandler;

    public SamInstEventHandler() {
        SamInstEventHandler.theInstHandler = this;
    }

    public static class NewApamInstance {
        ApamDependencyHandler handler;
        String                implName = null;
        String                specName = null;

        public NewApamInstance(ApamDependencyHandler handler, String implName, String specName) {
            this.handler = handler;
            this.implName = implName;
            this.specName = specName;
        }
    }

    private class NewSamInstance {
        Instance samInst;
        long     eventTime;

        public NewSamInstance(Instance samInst, long eventTime) {
            this.samInst = samInst;
            this.eventTime = eventTime;
            long currentTime = System.currentTimeMillis();
            // garbage old events (not related to an Apam instance)
            Set<String> instances = new HashSet<String>(SamInstEventHandler.newSamInstance.keySet());
            for (String inst : instances) {
                // at least 30 seconds (because of the debugger) !
                if (SamInstEventHandler.newSamInstance.get(inst).eventTime < (currentTime - 30000))
                    SamInstEventHandler.newSamInstance.remove(inst);
            }
        }
    }

    public static NewApamInstance getHandlerInstance(String samName) {
        return SamInstEventHandler.newApamInstance.get(samName);
        // NewApamInstance samInst = SamInstEventHandler.newApamInstance.get(samName);
        // if (samInst == null)
        // return null;
        // implName = samInst.implName;
        // specName = samInst.specName;
        // return SamInstEventHandler.newApamInstance.get(samName).handler;
    }

    public static synchronized void addNewApamInstance(String samName, ApamDependencyHandler handler, String implName,
            String specName) {
        if ((samName == null) || (handler == null))
            return;
        try {
            if (SamInstEventHandler.newSamInstance.get(samName) != null) { // the event arrived first
                Instance samInst = SamInstEventHandler.newSamInstance.get(samName).samInst;
                samInst.setProperty(CST.A_DEPHANDLER, handler);
                if (specName != null)
                    samInst.setProperty(CST.A_APAMSPECNAME, specName);
                if (implName != null)
                    samInst.setProperty(CST.A_APAMIMPLNAME, implName);
                SamInstEventHandler.newSamInstance.remove(samName);
            } else {
                SamInstEventHandler.newApamInstance.put(samName, new NewApamInstance(handler, implName, specName));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static synchronized void addExpectedImpl(String samImplName, DynamicManager manager) {
        if ((samImplName == null) || (manager == null))
            return;
        Set<DynamicManager> mans = SamInstEventHandler.expectedImpls.get(samImplName);
        if (mans == null) {
            mans = new HashSet<DynamicManager>();
            mans.add(manager);
            SamInstEventHandler.expectedImpls.put(samImplName, mans);
        } else {
            mans.add(manager);
        }
    }

    public static synchronized void removeExpectedImpl(String samImplName, DynamicManager manager) {
        if ((samImplName == null) || (manager == null))
            return;

        Set<DynamicManager> mans = SamInstEventHandler.expectedImpls.get(samImplName);
        if (mans != null) {
            mans.remove(manager);
        }
    }

    public static synchronized void addExpectedInterf(String interf, DynamicManager manager) {
        if ((interf == null) || (manager == null))
            return;

        Set<DynamicManager> mans = SamInstEventHandler.expectedInterfaces.get(interf);
        if (mans == null) {
            mans = new HashSet<DynamicManager>();
            mans.add(manager);
            SamInstEventHandler.expectedInterfaces.put(interf, mans);
        } else {
            mans.add(manager);
        }
    }

    public static synchronized void removeExpectedInterf(String interf, DynamicManager manager) {
        if ((interf == null) || (manager == null))
            return;
        Set<DynamicManager> mans = SamInstEventHandler.expectedInterfaces.get(interf);
        if (mans != null) {
            mans.remove(manager);
        }
    }

    static public synchronized void addLost(DynamicManager manager) {
        if (manager == null)
            return;
        SamInstEventHandler.listenLost.add(manager);
    }

    static public synchronized void removeLost(DynamicManager manager) {
        if (manager == null)
            return;
        SamInstEventHandler.listenLost.remove(manager);
    }

    @Override
    public Query getQuery() throws ConnectionException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public MachineID getTargetedMachineID() throws ConnectionException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public MachineID getMachineID() {
        // TODO Auto-generated method stub
        return null;
    }

    // executed when a new implementation is detected by SAM. Check if it is an expected impl.
    @Override
    public synchronized void receive(AMEvent amEvent) throws ConnectionException {
        InstPID instPid = (InstPID) amEvent.getProperty(EventProperty.INSTANCE_PID);
        if (instPid == null)
            return;
        Instance samInst = CST.SAMInstBroker.getInstance(instPid); // null if departure
        String samName = instPid.getId(); // available even if it disappeared

        if (amEvent.getProperty(EventProperty.TYPE).equals(EventProperty.TYPE_ARRIVAL)) {
            if (SamInstEventHandler.newApamInstance.containsKey(samName)) { // It is an APAM instance either under
                                                                            // creation, or auto appear
                NewApamInstance inst = SamInstEventHandler.newApamInstance.get(samName);
                samInst.setProperty(CST.A_DEPHANDLER, inst.handler);
                if (inst.specName != null)
                    samInst.setProperty(CST.A_APAMSPECNAME, inst.specName);
                if (inst.implName != null)
                    samInst.setProperty(CST.A_APAMIMPLNAME, inst.implName);
                SamInstEventHandler.newApamInstance.remove(samName);
            } else { // record the event in the case it arrived before the handler registration
                SamInstEventHandler.newSamInstance
                        .put(samName, new NewSamInstance(samInst, System.currentTimeMillis()));
            }

            Implementation samImpl = samInst.getImplementation();
            // if (samImpl == null) {
            // System.out.println("samImpl est null pour " + samName);
            // }
            String samImplName = samImpl.getName();
            // ASMImpl impl = CST.ASMImplBroker.getImpl(samImpl);
            if (SamInstEventHandler.expectedImpls.keySet().contains(samImplName)) {
                for (DynamicManager manager : SamInstEventHandler.expectedImpls.get(samImplName)) {
                    manager.appeared(samInst);
                }
                SamInstEventHandler.expectedImpls.remove(samImplName);
                return;
            }
            Specification samSpec = samInst.getSpecification();
            String[] interfs = samSpec.getInterfaceNames();
            for (String interf : interfs) {
                if (SamInstEventHandler.expectedInterfaces.get(interf) != null) {
                    for (DynamicManager manager : SamInstEventHandler.expectedInterfaces.get(interf)) {
                        manager.appeared(samInst);
                    }
                    SamInstEventHandler.expectedInterfaces.remove(interf);
                }
            }
            return;
        }

        // a service disappears
        if (amEvent.getProperty(EventProperty.TYPE).equals(EventProperty.TYPE_DEPARTURE)) {
            ASMInst inst = CST.ASMInstBroker.getInst(samName);
            if (inst == null)
                return;
            // notifies interested managers
            if (SamInstEventHandler.listenLost.contains(inst)) {
                for (DynamicManager manager : SamInstEventHandler.listenLost) {
                    manager.lostInst(inst);
                }
            }
            // deletes that instance, which deletes the wires and so on.
            inst.remove();
            return;
        }

        // A property has been changed
        if (amEvent.getProperty(EventProperty.TYPE).equals(EventProperty.TYPE_MODIFIED)) {
            ASMInst inst = CST.ASMInstBroker.getInst(samInst);
            if (inst == null)
                return;
            inst.setSamProperties(samInst.getProperties());
        }
    }

}

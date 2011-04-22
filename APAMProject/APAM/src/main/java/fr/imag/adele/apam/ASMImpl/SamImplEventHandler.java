package fr.imag.adele.apam.ASMImpl;

import java.util.HashSet;
import java.util.Set;

import fr.imag.adele.am.MachineID;
import fr.imag.adele.am.eventing.AMEvent;
import fr.imag.adele.am.eventing.AMEventingHandler;
import fr.imag.adele.am.exception.ConnectionException;
import fr.imag.adele.am.query.Query;
import fr.imag.adele.apam.CST;
import fr.imag.adele.sam.Implementation;
import fr.imag.adele.sam.PID;
import fr.imag.adele.sam.event.EventProperty;

public class SamImplEventHandler implements AMEventingHandler {

    Set<String> expectedImpls = new HashSet<String>();

    @Override
    public Query getQuery() throws ConnectionException {
        // TODO Auto-generated method stub
        return null;
    }

    public Implementation getImplementation(String expected) throws ConnectionException {
        if (expected == null)
            return null;
        // if allready here
        Implementation implementation = CST.SAMImplBroker.getImplementation(expected);
        if (implementation != null)
            return implementation;

        // not yet here. Wait for it.
        synchronized (this) {
            
            try {
                while (expectedImpls.contains(expected)) {
                    this.wait();
                }
                // The expected impl arrived.
                implementation = CST.SAMImplBroker.getImplementation(expected);
                if (implementation == null) // should never occur
                    System.out.println("wake up but imlementation is not present " + expected);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return implementation;
    }

    @Override
    public MachineID getTargetedMachineID() throws ConnectionException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * 
     * @param expected
     */
    public synchronized void addExpected(String expected) {
    	expectedImpls.add(expected);
    }
    
    // executed when a new implementation is detected by SAM. Check if it is an expected impl.
    @Override
    public synchronized void receive(AMEvent amEvent) throws ConnectionException {
        if (amEvent.getProperty(EventProperty.TYPE).equals(EventProperty.TYPE_ARRIVAL)) {
            PID implPid = (PID) amEvent.getProperty(EventProperty.IMPLEMENTATION_PID);
            if (implPid == null)
                return;
            String name = implPid.getId();
            if (expectedImpls.contains(name)) { // it is expected
                expectedImpls.remove(name);
                notifyAll(); // wake up the thread waiting in getImplementation
            }
        }

    }

    @Override
    public MachineID getMachineID() {
        // TODO Auto-generated method stub
        return null;
    }

}

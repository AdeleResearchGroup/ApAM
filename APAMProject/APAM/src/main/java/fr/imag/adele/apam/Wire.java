package fr.imag.adele.apam;

import java.util.HashSet;
import java.util.Set;

import org.osgi.framework.Filter;

import fr.imag.adele.apam.apamAPI.ASMInst;
import fr.imag.adele.apam.util.Attributes;

public class Wire {
    ASMInst     source;
    ASMInst     destination;
    String      depName;
    Set<Filter> constraints = new HashSet<Filter>();

    public Wire(ASMInst from, ASMInst to, String depName, Set<Filter> constraints) {
        if (Wire.checkNewWire(from, to)) {
            source = from;
            destination = to;
            this.depName = depName;
            this.constraints = constraints;
        }
    }

    public Wire(ASMInst from, ASMInst to, String depName, Filter filter) {
        if (Wire.checkNewWire(from, to)) {
            constraints.add(filter);
            source = from;
            destination = to;
            this.depName = depName;
        }
    }

    /**
     * Check if this new wire is consistent or not. Check the shared property, Check if composite are correctly set.
     * Checks if the wire is already existing.
     * 
     * @param from
     * @param to
     * @return
     */
    public static boolean checkNewWire(ASMInst from, ASMInst to) {
        if ((from.getComposite() == null) || (to.getComposite() == null)) {
            System.out.println("erreur : Composite not present in instance.");
            return false;
        }
        String shared = (String) to.getProperty(Attributes.SHARED);
        if (shared == null)
            return true; // sharable by default
        try {
            if (shared.equals(Attributes.PRIVATE)) {
                if (to.getWires() == null)
                    return true;
            } else if (shared.equals(Attributes.LOCAL)) {
                if (from.getComposite() == to.getComposite())
                    return true;
            } else if (shared.equals(Attributes.APPLI)) {
                if (from.getComposite() == to.getComposite())
                    return true;
                if (from.getComposite().dependsOn(to.getComposite()))
                    return true;
            } else
                return true;

            System.out.println("prohibited wire between " + from + " and " + to);
            return false;
        } catch (Exception e) {
        }
        return false;
    }

    public ASMInst getSource() {
        return source;
    }

    public ASMInst getDestination() {
        return destination;
    }

    public String getDepName() {
        return depName;
    }

    public Set<Filter> getConstraints() {
        return new HashSet<Filter>(constraints);
    }

    public void setConstraints(Set<Filter> constraints) {
        this.constraints = constraints;
    }

    public void addFilter(Filter filter) {
        constraints.add(filter);
    }

    public void removeFilter(Filter filter) {
        constraints.remove(filter);
    }

}

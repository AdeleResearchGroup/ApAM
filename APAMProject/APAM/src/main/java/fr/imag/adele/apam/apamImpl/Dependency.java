package fr.imag.adele.apam.apamImpl;

import java.util.Set;

/**
 * The information known by the handler about the potential dependencies of its managed instance.
 * 
 * Describes a single dependency (atomic or composite) [] == optional, {} == multiple
 * A specification dependency is atomic of the form
 * ----<targetKind fieldType>
 * A composite type dependency is atomic of the form
 * ----<targetKind fieldType, [multiple] [{source}]>
 * An implementation dependency is of the form
 * ----Atomic : <targetKind fieldName [fieldType] [multiple]> (targetKind != specification)
 * ----Composite <specificationName [multiple] {Atomic} >
 */

public class Dependency {
    public String                specification;     // for a composite dependency only: the common specification.
    public String[]              source;            // for composites only, the list of source specifications.
    public boolean               isMultiple = false; // cardinality multiple (atomic of composite). Optional=false
    public Set<AtomicDependency> dependencies;      // List of dependencies, even if atomic.

    public enum TargetKind {
        INTERFACE, PUSH_MESSAGE, PULL_MESSAGE, SPECIFICATION
    }

    public class AtomicDependency {
        public TargetKind targetKind; // INTERFACE, PUSH_MESSAGE, PULL_MESSAGE
        public String     fieldName; // Variable name for interface, messageField for Pull msg, method for push msg.
        public String     fieldType; // Type of the field: interface name; dataType for Pull and Push msg. Optional

        @Override
        public String toString() {
            String val = "Dependency: \n         target: " + targetKind + "  " + fieldType;
            if (fieldName != null)
                val = val + "; field: " + fieldName;
            val = val + "; multiple = " + isMultiple;
            return val + "\n";
        }
    }

    @Override
    public String toString() {
        if (!isComposite())
            return getAtomicDependency().toString();

        String val = "Composite Dependency; Specification : " + specification;
        if ((source != null) && (source.length > 0)) {
            val = val + "\n         source specification: ";
            for (String sc : source) {
                val = val + "  " + sc;
            }
        }
        for (AtomicDependency dep : dependencies) {
            val = val + dep.toString();
        }
        return val + "\n";
    }

    public boolean isComposite() {
        return specification != null;
    }

    public AtomicDependency getAtomicDependency() {
        if (isComposite()) {
            System.err.println(" xxx ");
            return null;
        }
        return (AtomicDependency) dependencies.toArray()[0];
    }
}

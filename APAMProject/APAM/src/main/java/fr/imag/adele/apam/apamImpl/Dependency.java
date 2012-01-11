package fr.imag.adele.apam.apamImpl;

import java.util.List;
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
 * ----Complex: <specificationName [multiple] {Atomic} >
 */

public abstract class Dependency {
    public DependencyKind kind;
    public boolean        isMultiple = false;       // cardinality multiple. Optional=false

    public List<String>   implementationConstraints;
    public List<String>   instanceConstraints;
    public List<String>   implementationPreferences;
    public List<String>   instancePreferences;

    public enum DependencyKind {
        SPECIFICATION, IMPLEMENTATION, COMPLEX, COMPOSITE
    }

    public enum TargetKind {
        INTERFACE, PUSH_MESSAGE, PULL_MESSAGE, SPECIFICATION
    }

    public static class SpecificationDependency extends Dependency {
        public TargetKind targetKind; // INTERFACE, PUSH_MESSAGE, PULL_MESSAGE
        public String     fieldType; // Type of the field: interface name; dataType for Pull and Push msg. Optional

        public SpecificationDependency(TargetKind targetKind, String fieldType) {
            this.targetKind = targetKind;
            this.fieldType = fieldType;
            kind = DependencyKind.SPECIFICATION;
        }

        @Override
        public String toString() {
            if (isMultiple)
                return targetKind + "  " + fieldType + "  multiple = true";
            return targetKind + "  " + fieldType;
        }
    }

    public static class ImplementationDependency extends Dependency {
        public String                specification; // for a complex dependency only: the common specification.
        public Set<AtomicDependency> dependencies; // List of dependencies.

        public ImplementationDependency(String specification, Set<AtomicDependency> dependencies, boolean multiple) {
            this.specification = specification;
            this.dependencies = dependencies;
            isMultiple = multiple;
            if (specification == null)
                kind = DependencyKind.IMPLEMENTATION;
            else
                kind = DependencyKind.COMPLEX;
        }

        @Override
        public String toString() {
            String ret = "   ";
            if (kind == DependencyKind.COMPLEX)
                ret = "specification  " + specification + "  ";
            for (AtomicDependency dep : dependencies) {
                ret = ret + dep;
            }
            if (isMultiple)
                ret = ret + "   multiple = true \n";
            return ret;
        }
    }

    public static class CompositeDependency extends Dependency {
        public String[]   source;    // the list of source specifications.
        public TargetKind targetKind; // INTERFACE, PUSH_MESSAGE, PULL_MESSAGE, SPECIFICATION
        public String     fieldType; // Type of the field: interface name; spec name, dataType for Pull and Push msg.

        public CompositeDependency(String[] source, TargetKind targetKind, String fieldType,
                boolean multiple) {
            this.source = source;
            this.targetKind = targetKind;
            this.fieldType = fieldType;
            isMultiple = multiple;
            kind = DependencyKind.COMPOSITE;
        }

        @Override
        public String toString() {
            String ret;
            if (isMultiple)
                ret = targetKind + "  " + fieldType + ",   multiple = true";
            ret = targetKind + "  " + fieldType;
            if (source != null) {
                ret = ret + "\n   sources = ";
                for (String sc : source) {
                    ret = ret + "  " + sc;
                }
            }
            return ret;
        }
    }

    public static class AtomicDependency {
        public TargetKind targetKind; // INTERFACE, PUSH_MESSAGE, PULL_MESSAGE
        public String     fieldName; // Variable name for interface, messageField for Pull msg, method for push msg.
        public String     fieldType; // Type of the field: interface name; dataType for Pull and Push msg. Optional

        public AtomicDependency(TargetKind targetKind, String fieldName, String fieldType) {
            this.targetKind = targetKind;
            this.fieldName = fieldName;
            this.fieldType = fieldType;
        }

        @Override
        public String toString() {
            return "  target: " + targetKind + "  field:" + fieldName + "  type:" + fieldType + "\n";
        }
    }
}

package fr.imag.adele.apam.declarations.instrumentation;

import fr.imag.adele.apam.declarations.AtomicImplementationDeclaration;
import fr.imag.adele.apam.declarations.references.components.ComponentReference;

/**
 * The base class of all declarations that require instrumenting (injecting, intercepting, invoking, ...)
 * the code of the implementation
 * 
 * @author vega
 * 
 */
public abstract class Instrumentation {

	/**
	 * A small utility to handle lazy evaluation of values that are calculated from the reflection information,
	 * and that remain constant afterwards.
	 */
	protected abstract class Lazy<T> {

		private boolean isEvaluated;
		private T value;

		public Lazy() {
			value = null;
			isEvaluated = false;
		}

		protected abstract T evaluate(InstrumentedClass instrumentedClass);

		public T get() {

			if (isEvaluated) {
				return value;
			}

			value = evaluate(instrumentedClass);
			isEvaluated = true;
			return value;
		}
	}

	/**
	 * The implementation associated with this component
	 */
	protected final ComponentReference<AtomicImplementationDeclaration> implementation;
	
	/**
	 * The instrumented class associated with the implementation
	 */
	protected final InstrumentedClass instrumentedClass;

	protected Instrumentation(ComponentReference<AtomicImplementationDeclaration> implementation, InstrumentedClass instrumentedClass) {
		this.implementation 	= implementation;
		this.instrumentedClass	= instrumentedClass;
	}

	/**
	 * The component declaring this instrumentation
	 */
	public ComponentReference<AtomicImplementationDeclaration> getImplementation() {
		return implementation;
	}

	/**
	 * An unique identifier for this injection, within the scope of the declaring implementation
	 */
	public abstract String getName();
	
	/**
	 * Whether the instrumentation declaration is valid in the instrumented code
	 */
	public abstract boolean isValidInstrumentation();

}

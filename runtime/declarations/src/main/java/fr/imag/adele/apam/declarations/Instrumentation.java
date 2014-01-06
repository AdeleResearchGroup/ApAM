package fr.imag.adele.apam.declarations;

import fr.imag.adele.apam.declarations.AtomicImplementationDeclaration.CodeReflection;

/**
 * The base class of all declarations that require instrumenting (injecting,
 * intercepting, invoking, ...) the code of the implementation
 * 
 * @author vega
 * 
 */
public abstract class Instrumentation {

	/**
	 * A small utility to handle lazy evaluation of values that are calculated
	 * from the reflection information, and that remain constant afterwards.
	 */
	protected abstract class Lazy<T> {

		private boolean isEvaluated;
		private T value;

		public Lazy() {
			value = null;
			isEvaluated = false;
		}

		protected abstract T evaluate(CodeReflection reflection);

		public T get() {

			if (isEvaluated) {
				return value;
			}

			value = evaluate(implementation.getReflection());
			isEvaluated = true;
			return value;
		}
	}

	/**
	 * The implementation associated to this instrumentation
	 */
	protected final AtomicImplementationDeclaration implementation;

	protected Instrumentation(AtomicImplementationDeclaration implementation) {
		this.implementation = implementation;
	}

	/**
	 * The component declaring this instrumentation
	 */
	public AtomicImplementationDeclaration getImplementation() {
		return implementation;
	}

	/**
	 * Whether the instrumentation declaration is valid in the instrumented code
	 */
	public abstract boolean isValidInstrumentation();

}

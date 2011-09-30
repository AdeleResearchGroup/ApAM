package fr.imag.adele.dynamic.application.manager;

import fr.imag.adele.apam.apamAPI.ASMInst;
import fr.imag.adele.sam.Instance;

/**
 * This class represents one services equivalence class.
 * 
 * Membership to the equivalence class is represented by a boolean predicate that must be
 * satisfied by all members. 
 * 
 * NOTE The equivalence relation defining the equivalence classes is not explicitly modeled
 * because we are seldom interested in comparing two services, but rather knowing if a service
 * belongs to a given equivalence class.
 * 
 *   
 * @author vega
 *
 */
public abstract class ServiceClassifier {

	/**
	 * Determines if the service is a member of this equivalence class
	 */
	public abstract boolean contains(ASMInst instance);
	
	/**
	 * Determines if the service is a member of this equivalence class
	 */
	public abstract boolean contains(Instance instance);

	/**
	 * Determines if this service class is included in another service class
	 * 
	 * NOTE Notice that in general service classifier are not implemented by extension, but 
	 * rather as a predicate. This means that determining class inclusion is akin to prove
	 * implication of predicates, which is not generally feasible. This method is then just
	 * a hint that can work in very specific cases, and should be programmed conservatively
	 * so that if inclusion can not be easily determined it should return false.
	 */
	public boolean includedIn(ServiceClassifier classifier) {
		
		/*
		 * this is always included in self
		 */
		if (classifier.equals(this))
			return true;
		
		/*
		 * this is always included in universe
		 */
		if (classifier.equals(ANY))
			return true;
		
		return false;

	}

	/**
	 * Determines if a service class is contained in another service class
	 * 
	 * NOTE this method is final as it is derived from the includedIn method.
	 */
	public final boolean contains(ServiceClassifier classifier) {
		return classifier.includedIn(this);
	}
	
	
	/**
	 * A class to represents a logical operation on the predicates defining two service classifiers.
	 *
	 */
	public abstract static class BinaryOperator extends ServiceClassifier {
	
		private final ServiceClassifier operand1;
		private final ServiceClassifier operand2;
		
		private BinaryOperator(ServiceClassifier operand1, ServiceClassifier operand2) {
			
			assert operand1 != null && operand2 != null;
			
			this.operand1 	= operand1;
			this.operand2	= operand2;
		}
		
		/**
		 * Get first operand
		 */
		public ServiceClassifier firstOperand() {
			return operand1;
		}
		
		/**
		 * Get first operand
		 */
		public ServiceClassifier secondOperand() {
			return operand2;
		}
		
		/**
		 * Redefine equality. Notice that we implement syntactical equality of service class expressions, we
		 * do not try to validate if two predicates are equivalent.
		 */
		public boolean equals(Object object) {
			
			if (object == this)
				return true;
			
			if (object == null)
				return false;
			
			if (!(object instanceof BinaryOperator))
				return false;
			
			BinaryOperator that = (BinaryOperator) object;
			
			return this.operand1.equals(that.operand1) && this.operand2.equals(that.operand2);
		}
		
		/**
		 * redefine hashcode to match equality definition
		 */
		public int hashCode() {
			return operand1.hashCode()+operand2.hashCode();
		}

	}
	
	
	/**
	 * A class that represents the intersection of two equivalence classes
	 * 
	 * NOTE Notice that the two equivalence classes may be unrelated, in the sense that they 
	 * may not belong to the same quotient set. The resulting equivalence class belongs to 
	 * the quotient set of the intersection of the two base relations.
	 * 
	 */
	private static class AndOperator extends BinaryOperator {
		
		public AndOperator(ServiceClassifier operand1, ServiceClassifier operand2) {
			super(operand1,operand2);
		}
		
			
		public @Override boolean contains(ASMInst instance) {
			return firstOperand().contains(instance) && secondOperand().contains(instance);
		}

		public @Override boolean contains(Instance instance) {
			return firstOperand().contains(instance) && secondOperand().contains(instance);
		}
		
		public @Override boolean includedIn(ServiceClassifier classifier) {


			/*
			 * if any of the operands is included in the specified classifier, intersection
			 * will also be included
			 * 
			 */
			return 	( firstOperand().includedIn(classifier) || secondOperand().includedIn(classifier) ) ||
					super.includedIn(classifier);

		}
	}
	
	/**
	 * Returns a new classifier that represents the intersection of this equivalence classes
	 * with another equivalence class
	 *
	 */
	public final ServiceClassifier and(ServiceClassifier that) {
		return new AndOperator(this,that);
	}
		
	/**
	 * Returns a new classifier that represents the union of two equivalence classes
	 * 
	 * NOTE Notice that the two equivalence classes may be unrelated, in the sense that they 
	 * may not belong to the same quotient set. The resulting equivalence class belongs to 
	 * the quotient set of the union of the two base relations.
	 */
	private static class OrOperator extends BinaryOperator {
		
		public OrOperator(ServiceClassifier operand1, ServiceClassifier operand2) {
			super(operand1,operand2);
		}
		
			
		public @Override boolean contains(ASMInst instance) {
			return firstOperand().contains(instance) || secondOperand().contains(instance);
		}

		
		public @Override boolean contains(Instance instance) {
			return firstOperand().contains(instance) || secondOperand().contains(instance);
		}

		
		public @Override boolean includedIn(ServiceClassifier classifier) {
			/*
			 * if both operands are included in the specified classifier, union
			 * will also be included
			 * 
			 */
			return 	(firstOperand().includedIn(classifier) && secondOperand().includedIn(classifier)) ||
					super.includedIn(classifier);

		}
	}

	/**
	 * Returns a new classifier that represents the union of this equivalence classes
	 * with another equivalence class
	 *
	 */
	public final ServiceClassifier or(ServiceClassifier that) {
		return new OrOperator(this,that);
	}

	/**
	 * The universal class containing all services
	 */
	public static ServiceClassifier ANY = new ServiceClassifier() {
		
		public @Override boolean contains(ASMInst instance) {
			return true;
		}

		
		public @Override boolean contains(Instance instance) {
			return true;
		}


		/**
		 * Universe is only included in itself
		 */
		public @Override boolean includedIn(ServiceClassifier classifier) {
			return classifier.equals(this);
		}
		
		/**
		 * Redefines equality to insure unicity of this instance
		 */
		public boolean equals(Object object) {
			return object == this;
		}
		
		/**
		 * redefines hashcode to match equality definition
		 */
		public int hashCode() {
			return this.getClass().hashCode();
		};
		
	};
}

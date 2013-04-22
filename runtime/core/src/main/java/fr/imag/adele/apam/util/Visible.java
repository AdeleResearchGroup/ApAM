package fr.imag.adele.apam.util;

import java.util.HashSet;
import java.util.Set;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.declarations.CompositeDeclaration;

public class Visible {

	/*
	 * =============== Visibility control ================= //
	 */

	/**
	 * returns true if Component "from" can establish a wire or link towards component target. 
	 * @param from
	 * @param to
	 */
	public static boolean isVisible (Specification source, Component target) {
		return true;
	}
	public static boolean isVisible (Component source, Specification target) {
		return true ;
	}

	public static boolean isVisible (Instance source, Component target) {
		if (target instanceof Specification)
			return true ;
		if (target instanceof Implementation)
			return checkImplVisible (source.getComposite().getCompType(), (Implementation)target) ;
		return checkInstVisible(source.getComposite(), (Instance)target);
	}

	/**
	 * Return true if implementation source can create a link towards implementation target
	 * @param source
	 * @param target
	 * @return
	 */
	public static boolean isVisible (Implementation source, Implementation target) {
		//They have a composite type in common
		for (CompositeType cSource : source.getInCompositeType()) {
			if (target.getInCompositeType().contains(cSource))
				return true ;
		}

		//at least one CT source must imports target
		boolean imported = false ;
		for (CompositeType cSource : source.getInCompositeType()) {
			if (checkVisibilityExpression (source, 
					((CompositeDeclaration) cSource.getDeclaration()).getVisibility().getImportImplementations(), 
					target)) {
				imported = true ;
				break ;
			}
		}
		if (!imported) return false ;

		//needs that target is exported by one of its composite types					
		String exports ;
		for (CompositeType cTarget : target.getInCompositeType()) {
			exports = ((CompositeDeclaration) cTarget.getDeclaration()).getVisibility().getExportImplementations(); 
			if (checkVisibilityExpression (target, exports, target)) {
				return true ;
			}
		}
		return false ;
	}

	public static boolean isVisible (Implementation source, Instance target) {
		//If target in same CT than source
		if (((Implementation)source).getInCompositeType().contains(target.getComposite().getCompType()))
			return true ;

		//target must be exported
		String exports = ((CompositeDeclaration)target.getComposite().getCompType().getDeclaration()).getVisibility().getExportInstances() ;
		if (!checkVisibilityExpression (target, exports, target)) {
			return false ;
		}

		//and at least a CT of source must import target
		for (CompositeType cSource : source.getInCompositeType()) {
			if (checkVisibilityExpression (source, 
					((CompositeDeclaration) cSource.getDeclaration()).getVisibility().getImportInstances(), 
					target)) {
				return true ;
			}
		}
		return false ;
	}


	/**
	 * return true if expression is null, "true" or if the component matches the expression.
	 * Substitution, if any, is with respect to from.
	 * 
	 * @param from
	 * @param expre
	 * @param comp
	 * @return
	 */
	private static boolean checkVisibilityExpression(Component from, String expre, Component comp) {
		if ((expre == null) || expre.isEmpty() || expre.equals(CST.V_TRUE)) {
			return true;
		}
		if (expre.equals(CST.V_FALSE)) {
			return false;
		}

		//from is used for the substitution, if any.
		ApamFilter f = ApamFilter.newInstanceApam(expre, from);
		if (f == null) {
			//Bad filter
			return false;
		}
		return comp.match(f);
	}


	/**
	 * Return the subset of impls that is visible from client.
	 * 
	 * @param client
	 * @param impls
	 * @return
	 */
	public static Set<Implementation> getVisibleImpls (Instance client, Set<Implementation> impls) {
		if (impls == null) {return null ; }

		Set<Implementation> ret = new HashSet <Implementation> () ;
//		CompositeType compo = client.getComposite().getCompType() ;
		for (Implementation impl : impls) {
			if (isVisible(client, impl)) {
				ret.add(impl) ;
			}
		}
		return ret ;
	}

//	/**
//	 * Return the subset of insts that is visible from client.
//	 * @param client
//	 * @param insts
//	 * @return
//	 */
//	public static Set<Instance> getVisibleInsts (Instance client, Set<Instance> insts) {
//		if (insts == null) {return null ;}
//
//		if(client==null) return insts;
//
//		Set<Instance> ret = new HashSet <Instance> () ;
//		Composite compo = client.getComposite() ;
//		for (Instance inst : insts) {
//			if (checkInstVisible(compo, inst)) {
//				ret.add(inst) ;
//			}
//		}
//		return ret ;
//	}

	/**
	 * Implementation toImpl is exported if it matches the export clause in at least one of it composite types.
	 * compoFrom can see toImpl if toImpl is visible or if it is in the same composite type. 
	 *
	 * @param compoFrom
	 * @param toImpl
	 * @return
	 */
	private static boolean checkImplVisible(CompositeType compoFrom, Implementation toImpl) {
		if (toImpl.getInCompositeType().isEmpty() || toImpl.getInCompositeType().contains(compoFrom)) {
			return true;
		}

		// First check if toImpl can be imported (borrowed) in compoFrom
		String imports = ((CompositeDeclaration) compoFrom.getDeclaration()).getVisibility().getImportImplementations(); 
		if (checkVisibilityExpression(compoFrom, imports, toImpl) == false) {
			return false;
		}

		// true if at least one composite type that owns toImpl exports it.
		for (CompositeType compoTo : toImpl.getInCompositeType()) {
			if (checkImplVisibleInCompo(compoFrom, toImpl, compoTo)) {
				return true;
			}
		}
		return false;
	}

	private static boolean
	checkImplVisibleInCompo(CompositeType compoFrom, Implementation toImpl, CompositeType compoTo) {
		if (compoFrom == compoTo) {
			return true;
		}
		String exports = ((CompositeDeclaration) compoTo.getDeclaration()).getVisibility().getExportImplementations();
		return checkVisibilityExpression(compoFrom, exports, toImpl) ;
	}


	/**
	 * Instance toInst is visible from composite compoFrom if :
	 * 		toInst is inside compoFrom or 
	 * 		compoFrom imports toInst AND
	 * 		toInst is in same appli than compoFrom or
	 * 		toInst is global.
	 *
	 * @param fromCompo
	 * @param toInst
	 * @return
	 */
	public static boolean checkInstVisible(Composite fromCompo, Instance toInst) {
		Composite toCompo = toInst.getComposite();
		CompositeType fromCompoType = fromCompo.getCompType();
		CompositeType toCompoType   = toInst.getComposite().getCompType();

		if (fromCompo == toCompo) {
			return true;
		}

		// First check if toInst can be imported by fromCompo
		String imports = ((CompositeDeclaration) fromCompoType.getDeclaration()).getVisibility().getImportInstances();
		if (!checkVisibilityExpression(fromCompo, imports, toInst)) {
			return false;
		}

		//exported ?
		String exports = ((CompositeDeclaration) toCompoType.getDeclaration()).getVisibility().getExportInstances();
		if (checkVisibilityExpression(fromCompo, exports, toInst)) {
			return true;
		}

		//exportApp ? Only if source is an instance.
		if (fromCompo.getAppliComposite() == toCompo.getAppliComposite()) {
			String appli = ((CompositeDeclaration) toCompoType.getDeclaration()).getVisibility()
					.getApplicationInstances();
			if ((appli != null) && checkVisibilityExpression(fromCompo, appli, toInst)) {
				return true;
			}
		}
		return false;
	}


}

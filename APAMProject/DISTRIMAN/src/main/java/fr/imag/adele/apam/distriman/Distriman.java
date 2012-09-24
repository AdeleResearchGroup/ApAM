package fr.imag.adele.apam.distriman;

import java.util.List;
import java.util.Set;
import java.util.logging.Filter;

import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.DependencyManager;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.ManagerModel;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.core.ResolvableReference;

public class Distriman implements DependencyManager{

	
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void getSelectionPathSpec(CompositeType compTypeFrom,
			String specName, List<DependencyManager> selPath) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void getSelectionPathImpl(CompositeType compTypeFrom,
			String implName, List<DependencyManager> selPath) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void getSelectionPathInst(Composite compoFrom, Implementation impl,
			Set<Filter> constraints, List<Filter> preferences,
			List<DependencyManager> selPath) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getPriority() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void newComposite(ManagerModel model, CompositeType composite) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Implementation resolveSpecByResource(CompositeType compoType,
			ResolvableReference ressource, Set<Filter> constraints,
			List<Filter> preferences) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Implementation findImplByName(CompositeType compoType,
			String implName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Specification findSpecByName(CompositeType compoType, String specName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Instance resolveImpl(Composite compo, Implementation impl,
			Set<Filter> constraints, List<Filter> preferences) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Instance> resolveImpls(Composite compo, Implementation impl,
			Set<Filter> constraints) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void notifySelection(Instance client, ResolvableReference resName,
			String depName, Implementation impl, Instance inst,
			Set<Instance> insts) {
		// TODO Auto-generated method stub
		
	}

}

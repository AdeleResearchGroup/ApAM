package fr.imag.adele.obrMan.internal;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Resolver;
import org.apache.felix.bundlerepository.Resource;

public class CompositeRepostiory {

	Repository[] repositories;
	
	Resource[] resources;
	
	Resolver resolver;
	
	public CompositeRepostiory(RepositoryAdmin repoAdmin) {
	    
		List<Resource> resourcesTemp = new ArrayList<Resource>();
		repositories = repoAdmin.listRepositories();
		this.resolver =  repoAdmin.resolver();
		for (Repository repository : repositories) {
			resourcesTemp.addAll(Arrays.asList(repository.getResources()));
			repoAdmin.removeRepository(repository.getURI());
		}
		resources = resourcesTemp.toArray(new Resource[0]);
		
	}


	public Resource[] getResources() {
		return resources;
	}
	
	public Resolver getResolver(){
		return resolver;
	}
	
	public Repository[] getRepositories(){
		return repositories;
	}
	
}

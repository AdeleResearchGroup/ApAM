/**
 * Copyright 2011-2012 Universite Joseph Fourier, LIG, ADELE team
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package fr.imag.adele.apam.test.obrman;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.ops4j.pax.exam.util.PathUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.ow2.chameleon.testing.helpers.IPOJOHelper;
import org.ow2.chameleon.testing.helpers.OSGiHelper;

import fr.imag.adele.apam.Apam;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.ManagerModel;
import fr.imag.adele.obrMan.OBRManCommand;

public class OBRMANHelper {

	private final BundleContext context;

	private final OSGiHelper osgi;

	private final IPOJOHelper ipojo;
	
	public static final String OBRMAN_BUNDLE_NAME = "obrman";
	
	private Version version;

	public OBRMANHelper(BundleContext pContext) {
		context = pContext;
		osgi = new OSGiHelper(context);
		ipojo = new IPOJOHelper(context);
		version=getBundleVersion();
	}

	public void dispose() {
		osgi.dispose();
		ipojo.dispose();
	}

	public void setObrManInitialConfig(String modelPrefix, String[] repos,
			int expectedSize) throws IOException {
		// URL obrModelAppUrl = context.getBundle().getResource(modelPrefix +
		// ".OBRMAN.cfg");
		URL obrModelAppUrl = new URL("file:" + PathUtils.getBaseDir()
				+ "/target/classes/" + modelPrefix + ".OBRMAN.cfg");

		System.out.println(modelPrefix + " >>> " + obrModelAppUrl);

		OBRManCommand obrman = getAService(OBRManCommand.class);

		obrman.setInitialConfig(obrModelAppUrl);

		Set<String> rootRepos = getCompositeRepos(CST.ROOT_COMPOSITE_TYPE);
		for (String repo : repos) {
			assertTrue(rootRepos.contains(repo));
		}

		assertEquals(expectedSize, rootRepos.size());

	}

	public CompositeType createCompositeType(String name, String main,
			String mainSpec) throws IOException {
		waitForIt(100);

		// URL obrModelAppUrl = context.getBundle().getResource(name +
		// ".OBRMAN.cfg");
		URL obrModelAppUrl = new URL("file:" + PathUtils.getBaseDir()
				+ "/target/classes/" + name + ".OBRMAN.cfg");

		System.out.println(name + " >>> " + obrModelAppUrl);

		ManagerModel model = new ManagerModel("OBRMAN", obrModelAppUrl);

		Set<ManagerModel> models = new HashSet<ManagerModel>();

		models.add(model);

		Apam apam = getAService(Apam.class);

		CompositeType app = apam.createCompositeType(null, name, mainSpec,
				main, models, null);

		assertNotNull(app);

		assertNotNull(app.getMainImpl().getApformImpl());

		return app;
	}

	public <T> T createInstance(CompositeType CompoType, Class<T> class1) {

		Composite instanceApp = (Composite) CompoType
				.createInstance(null, null);

		assertNotNull(instanceApp);

		T appSpec = class1.cast(instanceApp.getServiceObject());

		return appSpec;

	}

	/**
	 * This method allows to verify the state of the bundle to make sure that we
	 * can perform tasks on it
	 * 
	 * @param time
	 */
	public void waitForIt(int time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			assert false;
		}

		if (context != null)
			while (
			// context.getBundle().getState() != Bundle.STARTING &&
			context.getBundle().getState() != Bundle.ACTIVE // &&
			// context.getBundle().getState() != Bundle.STOPPING
			) {
				try {
					Thread.sleep(time);
				} catch (InterruptedException e) {
					System.err.println("waitForIt failed.");
				}
			}
	}

	public <S> S getAService(Class<S> clazz) {
		Object o = osgi.getServiceObject(clazz.getName(), null);
		S s = clazz.cast(o);
		assertNotNull(s);
		return s;
	}

	public Set<String> getCompositeRepos(String compositeName) {
		OBRManCommand obrman = getAService(OBRManCommand.class);
		return obrman.getCompositeRepositories(compositeName);
	}

	public OSGiHelper getOSGiHelper() {
		return osgi;
	}

	public IPOJOHelper getIpojoHelper() {
		return ipojo;
	}

	public void printBundleList() {
		System.out.println("---------List of installed bundle--------");
		for (Bundle bundle : context.getBundles()) {
			System.out.println("- " + bundle.getLocation() + " ["
					+ bundle.getState() + "]");
		}
		System.out.println("---------End of List of installed bundle--------");
	}
	
	private Version getBundleVersion() {
	    for (Bundle bundle : context.getBundles())
		if(bundle.getSymbolicName().equals(OBRMAN_BUNDLE_NAME))
			return bundle.getVersion();
	    return null;
	}
	
	public String getMavenVersion() {
	    if (version==null)
		return null;
	    else {
		    String result=version.getMajor()+"."+version.getMinor()+"."+version.getMicro();
		    if(version.getQualifier()!= null && version.getQualifier().length()>0)
			result+="-"+version.getQualifier();
		    return result;
	    }

	}

}

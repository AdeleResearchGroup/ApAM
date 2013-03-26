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
package fr.imag.adele.apam.test.testcases;

//import static org.ops4j.pax.exam.CoreOptions.bundle;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.pax.distriman.test.iface.P2Spec;
import fr.imag.adele.apam.test.support.distriman.DistrimanUtil;
import fr.imag.adele.apam.tests.helpers.ExtensionAbstract;

@RunWith(JUnit4TestRunner.class)
public class DistriManTest extends ExtensionAbstract {

	@Override
	public List<Option> config() {
		List<Option> addon = super.config();
		addon.add(packApamDistriMan());
		addon.add(mavenBundle().groupId("fr.imag.adele.apam.tests.services")
						.artifactId("apam-pax-distriman-iface").versionAsInProject());
		
		return addon;
	}

	@Test
	public void ProviderSingleInterface_tc086() throws MalformedURLException, IOException {
		
		Implementation p2Impl = CST.apamResolver.findImplByName(null,
				"P2-singleinterface");

		Instance p2Inst = p2Impl.createInstance(null, null);
		
		String url="http://127.0.0.1:8080/apam/machine";
		
		final String jsonPayload = DistrimanUtil.httpRequestDependency("p2", "fr.imag.adele.apam.pax.distriman.test.iface.P2Spec", "P1", false, url);
		
		Map<String, String> parameters=new HashMap<String, String>(){{put("content", jsonPayload);}};
		
		String buffer=DistrimanUtil.curl(parameters, url);
		
		String response=URLDecoder.decode(buffer.toString(),"UTF-8");
		
		System.err.println(response);
		
		ObjectMapper om=new ObjectMapper();
		
		JsonNode node=om.readValue(response, JsonNode.class);
		
		Map<String,String> endpoints=om.convertValue(node.get("endpoint_entry"), new TypeReference<Map<String, String>>() {});
		
		System.out.println("Class\tURL");
		for(Map.Entry<String, String> entry:endpoints.entrySet()){
			System.out.println(String.format("%s\t%s", entry.getKey(),entry.getValue()));
		}
		
		Assert.assertTrue("distriman(provider host) did not create an endpoint after requested",endpoints.size()==1);
		
		try{
		
			ClientProxyFactoryBean factory = new ClientProxyFactoryBean();
			factory.setServiceClass(P2Spec.class);
			factory.setAddress(endpoints.get("fr.imag.adele.apam.pax.distriman.test.iface.P2Spec"));
			P2Spec proxy = (P2Spec)factory.create();
			System.err.println(proxy.getName());
			
		}catch(Exception e){
			Assert.fail(String.format("distriman(provider host) created an endpoint but was not possible to connect to it, failed with the message %s", e.getMessage()));
		}
		
	}

}

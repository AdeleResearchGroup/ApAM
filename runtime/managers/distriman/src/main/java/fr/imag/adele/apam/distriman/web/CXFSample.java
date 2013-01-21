package fr.imag.adele.apam.distriman.web;

import java.rmi.registry.Registry;
import java.util.HashMap;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.osgi.service.http.HttpService;

@Component
@Instantiate
@Provides
public class CXFSample {

	@Requires(optional = false)
	private HttpService http;

	Registry registry;

	Server server;

	public void start() {
		// Create our service implementation
		SampleImpl helloWorldImpl = new SampleImpl();

		// switch to the cxg minimal bundle class loader
		Thread.currentThread().setContextClassLoader(
				CXFNonSpringServlet.class.getClassLoader());

		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		Bus cxfbus = null;
		try {
			CXFNonSpringServlet servlet = new CXFNonSpringServlet();
			// Register a CXF Servlet dispatcher
			http.registerServlet("/ws2", servlet, null, null);
			// get the bus
			cxfbus = servlet.getBus();
		} catch (Exception e) {
			// TODO log
			throw new RuntimeException(e);
		} finally {
			Thread.currentThread().setContextClassLoader(loader);
		}

		// Create our Server
		// Thread.currentThread().setContextClassLoader(JaxWsServerFactoryBean.class.getClassLoader());
		Thread.currentThread().setContextClassLoader(
				ServerFactoryBean.class.getClassLoader());

		// JaxWsServerFactoryBean svrFactory = new JaxWsServerFactoryBean();
		ServerFactoryBean svrFactory = new ServerFactoryBean();
		svrFactory.setBus(cxfbus);
		
//		HashMap props = new HashMap();
//		props.put("jaxb.additionalContextClasses", new Class[]
//		{ SampleComplex.class,SampleImpl.class  }
//		);
//		svrFactory.setProperties(props);
		
		svrFactory.setServiceClass(SampleIface.class);
		// svrFactory.setAddress("http://localhost:8081/ws/SampleIface");
		svrFactory.setServiceBean(helloWorldImpl);
		server = svrFactory.create();

	}

	public void connectCall() {
		Thread.currentThread().setContextClassLoader(
				ServerFactoryBean.class.getClassLoader());
		ClientProxyFactoryBean factory = new ClientProxyFactoryBean();
		factory.setServiceClass(SampleIface.class);
		factory.setAddress(server.getEndpoint().getEndpointInfo().getAddress());
		// factory.setAddress("http://localhost:8581/ws/SampleIface");
		// factory.setAddress("http://localhost:49862/SampleIface");
		//System.out.println("Classes:"+factory.getProperties().get("jaxb.additionalContextClasses"));
		SampleIface client = (SampleIface) factory.create();

		System.out.println("Client side:" + client.hello("jander"));
		//System.out.println("Client side complex:"+ client.getComplex().getValue());

	}

	public void stop() {
		server.stop();
	}

}

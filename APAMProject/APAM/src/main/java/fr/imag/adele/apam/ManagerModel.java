package fr.imag.adele.apam;

import java.net.URL;

public class ManagerModel {
	private String name ;
	private String managerName ;
	private URL url ;
	private int type ;
	
	public ManagerModel (String name, String managerName, URL url, int type) {
		this.name = name ;
		this.managerName = managerName ;
		this.url= url ;
		this.type = type ;
	}
	
	public URL getURL () {
		return url ;
	}
	/**
	 * this model name.
	 * @return
	 */
	public String getName () {
		return name ;
	}

	/**
	 * The name of the manager this interprets this model
	 * @return
	 */
	public String getManagerName () {
		return name ;
	}

	/**
	 * Type of packaging of the URL
	 * @return
	 */
	public int getType () {
		return type ;
	}
}

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
package fr.imag.adele.apam;

import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ManagerModel {
	private final String managerName;
	private final URL url;
	private static Logger logger = LoggerFactory.getLogger(ManagerModel.class);

	public ManagerModel(String managerName, URL url) {
		if ((managerName == null) || (url == null)) {
			logger.error("ERROR : missing parameters for ManagerModel constructor");
		}
		this.managerName = managerName;
		this.url = url;
	}

	/**
	 * The name of the manager this interprets this model
	 * 
	 * @return
	 */
	public String getManagerName() {
		return managerName;
	}

	public URL getURL() {
		return url;
	}

	@Override
	public String toString() {
		return managerName + " -> " + url;
	}

}

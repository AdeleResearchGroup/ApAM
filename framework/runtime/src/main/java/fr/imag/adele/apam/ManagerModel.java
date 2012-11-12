package fr.imag.adele.apam;

import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ManagerModel {
    private final String managerName;
    private final URL    url;
    private static Logger logger = LoggerFactory.getLogger(ManagerModel.class);

    public ManagerModel(String managerName, URL url) {
        if ( (managerName == null) || (url == null)) {
            logger.error("ERROR : missing parameters for ManagerModel constructor");
        }
        this.managerName = managerName;
        this.url = url;
    }

    public URL getURL() {
        return url;
    }


    @Override
    public String toString() {
        return managerName+" -> "+url;
    }

    /**
     * The name of the manager this interprets this model
     * 
     * @return
     */
    public String getManagerName() {
        return managerName;
    }

}

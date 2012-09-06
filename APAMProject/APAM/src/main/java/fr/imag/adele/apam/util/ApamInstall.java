package fr.imag.adele.apam.util;

import java.net.URL;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.impl.APAMImpl;

public class ApamInstall {
	
	private static Logger logger = LoggerFactory.getLogger(ApamInstall.class);
	
    public static Implementation intallImplemFromURL(URL url, String compoName) {
        if (!ApamInstall.deployBundle(url, compoName))
            return null;
        return CST.ImplBroker.getImpl(compoName,true);
    }

    public static Specification intallSpecFromURL(URL url, String compoName) {
        if (!ApamInstall.deployBundle(url, compoName))
            return null;
        return CST.SpecBroker.getSpec(compoName,true);
    }

    private static boolean deployBundle(URL url, String compoName) {
        Bundle bundle = null;
        try {
            bundle = APAMImpl.context.installBundle(url.toString());
            if (!ApamInstall.getAllComponentNames(bundle).contains(compoName)) {
                logger.error("Bundle " + url.toString() + " does not contain " + compoName +
                        " but contains " + ApamInstall.getAllComponentNames(bundle));
                return false;
            }
            bundle.start();
            return true;
        } catch (BundleException e) {
            logger.error(e.getMessage());
            return false;
        }
    }

    private static void getcompositeNames(String metadata, String balise, Set<String> names) {
        if ((metadata == null) || metadata.isEmpty())
            return;
        int compoIndex = metadata.indexOf(balise);
        if (compoIndex == -1)
            return;
        String components = metadata.substring(compoIndex + 5);
        String name = ApamInstall.getNamecomponent(components);
        if (name != null) {
            // System.out.println(balise + " name = " + name);
            names.add(name);
        }
        ApamInstall.getcompositeNames(components, balise, names);
    }

    private static String getNamecomponent(String string) {
        String[] tokens = string.split("\\s");
        // System.out.println(tokens);
        String name;
        for (String token : tokens) {
            // System.out.print(" " + token);
            if (token.startsWith("$name=")) {
                name = token.substring(7, token.length() - 1);
                // System.out.println("name = " + name);
                return name;
            }
        }
        return null;
    }

    public static Set<String> getAllComponentNames(Bundle bundle) {
        Set<String> componentNames = new HashSet<String>();
        
        @SuppressWarnings("unchecked")
		Dictionary<String,String> headers = bundle.getHeaders();
        String iPOJO_components = (String) headers.get("iPOJO-Components");

        //        Enumeration en = headers.keys();
        //        while (en.hasMoreElements()) {
        //            String s = (String) en.nextElement();
        //            System.out.println(s + "  " + headers.get(s));
        //        }

        ApamInstall.getcompositeNames(iPOJO_components, "fr.imag.adele.apam:specification", componentNames);
        //        System.out.println("\nfr.imag.adele.apam:specification : " + componentNames);
        ApamInstall.getcompositeNames(iPOJO_components, "fr.imag.adele.apam:composite", componentNames);
        //        System.out.println("\nfr.imag.adele.apam:composite : " + componentNames);
        ApamInstall.getcompositeNames(iPOJO_components, "fr.imag.adele.apam:implementation", componentNames);
        //        System.out.println("\nfr.imag.adele.apam:implementation : " + componentNames);
        ApamInstall.getcompositeNames(iPOJO_components, "ipojo:component", componentNames);
        //        System.out.println("\nipojo:component : " + componentNames);
        ApamInstall.getcompositeNames(iPOJO_components, "component", componentNames);
        //        System.out.println("\ncomponent : " + componentNames);

        return componentNames;
    }

}

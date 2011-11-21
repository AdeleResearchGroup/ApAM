package fr.imag.adele.apam.util;

import java.net.URL;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Set;

import org.osgi.framework.Bundle;
//import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

import fr.imag.adele.apam.apamImpl.APAMImpl;

public class ApamInstall {

    public static boolean intallFromURL(URL url, String compoName) throws BundleException {
        Bundle bundle = APAMImpl.context.installBundle(url.toString());
        if (!ApamInstall.getAllComponentNames(bundle).contains(compoName)) {
            System.err.println("Bubdle " + url.toString() + " does not contain " + compoName);
            return false;
        }
        return true;
        // return APAMImpl.context.installBundle(url.toString());
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
            System.out.println(balise + " name = " + name);
            names.add(name);
        }
        ApamInstall.getcompositeNames(components, balise, names);
    }

    private static String getNamecomponent(String string) {
        String[] tokens = string.split("\\s");
        System.out.println(tokens);
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
        Dictionary headers = bundle.getHeaders();
        String iPOJO_components = (String) headers.get("iPOJO-Components");
        ApamInstall.getcompositeNames(iPOJO_components, "fr.imag.adele.apam:composite", componentNames);
        System.out.println("\nfr.imag.adele.apam:composite : " + componentNames);
        ApamInstall.getcompositeNames(iPOJO_components, "fr.imag.adele.apam:implementations", componentNames);
        System.out.println("\nfr.imag.adele.apam:implementations : " + componentNames);
        ApamInstall.getcompositeNames(iPOJO_components, "ipojo:component", componentNames);
        System.out.println("\nipojo:component : " + componentNames);
        ApamInstall.getcompositeNames(iPOJO_components, "component", componentNames);
        System.out.println("\ncomponent : " + componentNames);
        return componentNames;
    }

}

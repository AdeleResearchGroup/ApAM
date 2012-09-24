package fr.imag.adele.obrMan.internal;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class SaxHandler extends DefaultHandler {
	boolean localRepo = false;
	
	String localRepoPath;
	
	public void startElement(String uri, String localName,String qName, Attributes attributes) throws SAXException {
		if (qName.equalsIgnoreCase("localRepository")) {
			localRepo=true;
		}
	}

	public void endElement(String uri, String localName,
		String qName) throws SAXException {		
		if (localRepo) localRepo=false;
	}

	public void characters(char ch[], int start, int length) throws SAXException {
   		if (localRepo) {
   			localRepoPath =  new String(ch, start, length);
   		}
	}
	
	public URL getRepo() throws MalformedURLException{		
		if (localRepoPath == null ) return null;
		File file = new File( localRepoPath +File.separator +"repository.xml" );
		if (!file.exists()) return null;
		return file.toURI().toURL();
	}
}

package fr.imag.adele.obrMan.internal;

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
	public String getRepo(){
		String path;
		path = "file:///" + localRepoPath + "/repository.xml";
		return path;
	}
}

package fr.imag.adele.obrMan.internal;

import java.io.File;

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
		if (localRepoPath == null ) return null;
		path = "file:///" + localRepoPath +File.separator +"repository.xml";
		return path;
	}
}

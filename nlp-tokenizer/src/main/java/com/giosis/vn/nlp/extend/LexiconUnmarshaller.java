/**
 * (C) Le Hong Phuong, phuonglh@gmail.com
 */
package com.giosis.vn.nlp.extend;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import vn.hus.nlp.lexicon.IConstants;
import vn.hus.nlp.lexicon.jaxb.Corpus;
import vn.hus.nlp.lexicon.jaxb.ObjectFactory;

/**
 * @author Le Hong Phuong, phuonglh@gmail.com
 * <p>
 * A unmarshaller for lexicon.
 */
public class LexiconUnmarshaller {

	
	JAXBContext jaxbContext; 
	
	Unmarshaller unmarshaller;
	
	/**
	 * Default constructor.
	 */
	public LexiconUnmarshaller() {
		// create JAXB context
		//
		createContext();
	}
	
	private void createContext() {
		jaxbContext = null;
		try {
			ClassLoader cl = ObjectFactory.class.getClassLoader();
			jaxbContext = JAXBContext.newInstance(IConstants.PACKAGE_NAME, cl);
		} catch (JAXBException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Get the marshaller object.
	 * @return the marshaller object
	 */
	protected Unmarshaller getUnmarshaller() {
		if (unmarshaller == null) {
			try {
				// create the unmarshaller
				unmarshaller = jaxbContext.createUnmarshaller();
			} catch (JAXBException e) {
				e.printStackTrace();
			}
		}
		return unmarshaller;
	}

	/**
	 * Unmarshal a lexicon.
	 * @param filename a lexicon file
	 * @return a Corpus object.
	 */
	public Corpus unmarshal(String filename) {
		try {
			InputStream stream = new FileInputStream(new File(filename));
			
			if (stream!=null)
			{
				Object object = getUnmarshaller().unmarshal(stream);
				if (object instanceof Corpus) {
					Corpus corpus = (Corpus) object;
					return corpus;
				}
			}
		} catch (JAXBException | FileNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}
	
}

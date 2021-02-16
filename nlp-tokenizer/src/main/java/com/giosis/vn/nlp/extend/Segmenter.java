/**
 * (C) Le Hong Phuong, phuonglh@gmail.com
 *  Vietnam National University, Hanoi, Vietnam.
 */
package com.giosis.vn.nlp.extend;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import vn.hus.nlp.graph.AdjacencyListWeightedGraph;
import vn.hus.nlp.graph.Edge;
import vn.hus.nlp.graph.IGraph;
import vn.hus.nlp.graph.IWeightedGraph;
import vn.hus.nlp.graph.Node;
import vn.hus.nlp.graph.io.GraphIO;
import vn.hus.nlp.graph.search.ShortestPathFinder;
import vn.hus.nlp.graph.util.GraphConnectivity;
import vn.hus.nlp.tokenizer.segmenter.AbstractLexiconRecognizer;
import vn.hus.nlp.tokenizer.segmenter.AbstractResolver;
import vn.hus.nlp.tokenizer.segmenter.ExternalLexiconRecognizer;
import vn.hus.nlp.tokenizer.segmenter.IConstants;
import vn.hus.nlp.tokenizer.segmenter.StringNormalizer;
import vn.hus.nlp.utils.CaseConverter;

/**
 * @author Le Hong Phuong, phuonglh@gmail.com
 *         <p>
 *         vn.hus.nlp.segmenter
 *         <p>
 *         Nov 12, 2007, 8:11:26 PM
 *         <p>
 *         Segmenter of Vietnamese. It splits a chain of Vietnamese syllables
 *         (so called a phrase) into words. Before performing the segmentation,
 *         it does some necessary preprocessing:
 *         <ul>
 *         <li>If the first character of the phrase is an uppercase, it is
 *         changed to lower case.</li>
 *         <li>Normalize the phrase so that the accents of syllables are in
 *         their right places, for example, the syllable <tt>hòa</tt> is
 *         converted to <tt>hoà</tt> </li>.
 *         </ul>
 */
public class Segmenter {

	private static StringNormalizer normalizer;
	
	private Logger logger;
	
	/**
	 * The DFA representing Vietnamese lexicon (the internal lexicon).
	 */
	private static AbstractLexiconRecognizer lexiconRecognizer;
	
	/**
	 * The external lexicon recognizer.
	 */
	private static AbstractLexiconRecognizer externalLexiconRecognizer;
	
	/**
	 * Result of the segmentation. A segmentation can have several results. 
	 * Each result is represented by an array of words.
	 */
	private final List<String[]> result;

	/**
	 * An ambiguity resolver.
	 */
	private AbstractResolver resolver = null;

	private static double MAX_EDGE_WEIGHT = 100;
	
	private static boolean DEBUG = false;
	
	/**
	 * Default constructor.
	 */
	public Segmenter() {
		result = new ArrayList<String[]>();
		createLogger();
		// create DFA lexicon recognizer
		getDFALexiconRecognizer();
		// create external lexicon recognizer
		getExternalLexiconRecognizer();
		// create a string normalizer
		normalizer = StringNormalizer.getInstance();
	}

	/**
	 * Build a segmenter with an ambiguity resolver.
	 * @param resolver
	 */
	public Segmenter(AbstractResolver resolver) {
		this();
		this.resolver = resolver;
	}
	
	/**
	 * Build a segmenter with a properties object and an ambiguity resolver.  
	 * @param properties
	 * @param resolver
	 */
	public Segmenter(Properties properties, AbstractResolver resolver) {
		result = new ArrayList<String[]>();
		createLogger();
		// create DFA lexicon recognizer
		getDFALexiconRecognizer(properties);
		// create external lexicon recognizer
//		getExternalLexiconRecognizer(properties); // 사용자 사전 구조가 너무 단순하여 사용 안함
		// create a string normalizer
//		normalizer = StringNormalizer.getInstance(properties); //소문자로 들어오기 때문에 노말라이저도 사용 안함
		this.resolver = resolver;
	}
	
	private void createLogger() {
		if (logger == null) {
			logger = Logger.getLogger(Segmenter.class.getName());
			// use a console handler to trace the log
			logger.addHandler(new ConsoleHandler());
			logger.setLevel(Level.FINEST);
		}
	}

	/**
	 * @return The result list. Each element of the list is a possible segmentation.
	 * The list is normally contains less than 4 results.
	 */
	public List<String[]> getResult() {
		return result;
	}
	/**
	 * A pre-processing of segmentation. If the first character of the phrase is
	 * an uppercase, then it is converted to the corresponding lowercase; all 
	 * syllables are assured to have correct accents. This method is called before 
	 * method {@link #segment(String)}
	 * 
	 * @param phrase
	 *            a phrase to segment
	 * @return a phrase after pre-process
	 */
	private static String normalize(String phrase) {
		// 1. change the case of the first character.
		//
		StringBuffer s = new StringBuffer(phrase);
		char firstChar = s.charAt(0);
		char lowerChar = firstChar;
		// convert first character
		if ('A' <= firstChar && firstChar <= 'Z') {
			lowerChar = Character.toLowerCase(firstChar);
		} else if (CaseConverter.isValidUpper(firstChar))
			lowerChar = CaseConverter.toLower(firstChar);
		s.setCharAt(0, lowerChar);
		// 2. normalize the accents of the phrase
		return normalizer.normalize(s.toString());
	}
	

	
	
	/**
	 * Creates an internal lexicon recognizer.
	 * @return the DFA lexicon recognizer in use
	 */
	private AbstractLexiconRecognizer getDFALexiconRecognizer() {
		if (lexiconRecognizer == null) {
			// use the DFA lexicon recognizer
			// user can use any lexicon recognizer here.
			lexiconRecognizer = DFALexiconRecognizer.getInstance(IConstants.LEXICON_DFA);
		}
		return lexiconRecognizer;
	}
	
	/**
	 * Creates an internal lexicon recognizer.
	 * @return the DFA lexicon recognizer in use
	 */
	private AbstractLexiconRecognizer getDFALexiconRecognizer(Properties properties) {
		if (lexiconRecognizer == null) {
			// use the DFA lexicon recognizer
			// user can use any lexicon recognizer here.
			lexiconRecognizer = DFALexiconRecognizer.getInstance(properties.getProperty("lexiconDFA"));
		}
		return lexiconRecognizer;
	}	
	
	/**
	 * Creates an external lexicon recognizer.
	 * @return the external lexicon recognizer 
	 */
	private AbstractLexiconRecognizer getExternalLexiconRecognizer() {
		if (externalLexiconRecognizer == null) {
			externalLexiconRecognizer = new ExternalLexiconRecognizer();
		}
		return externalLexiconRecognizer;
	}

	/**
	 * Creates an external lexicon recognizer.
	 * @param properties
	 * @return the external lexicon recognizer 
	 */
	private AbstractLexiconRecognizer getExternalLexiconRecognizer(Properties properties) {
		if (externalLexiconRecognizer == null) {
			externalLexiconRecognizer = new ExternalLexiconRecognizer(properties);
		}
		return externalLexiconRecognizer;
	}

	
	/**
	 * Try to connect an unconnected graph. If a graph is unconnected, we 
	 * find all of its isolated vertices and add a "fake" transition to them. 
	 * A vertex is called isolated if it has not any intransition.  
	 * @param graph a graph 
	 */
	private void connect(IGraph graph) {
		// no need to connect the graph if it's connected.
		if (GraphConnectivity.countComponents(graph) == 1) 
			return;
		
		// get all isolated vertices - vertices that do not have any intransitions. 
		int[] isolatedVertices = GraphConnectivity.getIsolatedVertices(graph);
		// info for debug
		if (DEBUG) {
			System.err.println("The graph for the phrase is: ");
			GraphIO.print(graph);
			System.out.println("Isolated vertices: ");
			for (int i : isolatedVertices) {
				System.out.println(i);
			}
		}
		
		// There is a trick here: vertex 0 is always isolated in our linear graph since 
		// it is the initial vertex and does not have any intransition.
		// We need to check whether it has an outtransition or not (its degree is not zero),
		// if no, we connect it to the nearest vertex - vertex 1 - to get an edge with weight 1.0;
		// if yes, we do nothing. Note that since the graph represents an array of non-null syllables,
		// so the number of vertices of the graph is at least 2 and it does contain vertex 1.
		boolean zeroVertex = false;
		for (int i = 0; i < isolatedVertices.length; i++) {
			int u = isolatedVertices[i];
			if (u == 0) {
				zeroVertex = true;
				/*
				GraphDegree graphDegree = new GraphDegree(graph);
				if (graphDegree.degree(0) == 0) {
					graph.insert(new Edge(0,1,MAX_EDGE_WEIGHT));
				}
				*/
				// we always add a new edge (0,1) regardless of vertex 0 is 
				// of degree 0 or higher.
				graph.insert(new Edge(0,1,MAX_EDGE_WEIGHT));
			} else {
				if (u != 1) {
					// u is an internal isolated vertex, u > 0. We simply add an edge (u-1,u) 
					// also with the maximum weight 1.0
					graph.insert(new Edge(u-1,u,MAX_EDGE_WEIGHT));
				} else { // u == 1
					if (!zeroVertex) { // insert edge (0,1) only when there does not this edge
						graph.insert(new Edge(u-1,u,MAX_EDGE_WEIGHT));
					}
				}
			}
		}
		// make sure that the graph is now connected:
		if (GraphConnectivity.countComponents(graph) != 1) {
			logger.log(Level.INFO, "Hmm, fail to connect the graph!");
		}
	}
	
	/**
	 * Prepare to segment a phrase. 
	 * @param phrase a phrase to be segmented.
	 * @see #segment(String)
	 * @return an array of syllables of the phrase
	 */
	private String[] prepare(String phrase) {
		// clear the last result
		result.clear();
		// normalize the phrase
		phrase = Segmenter.normalize(phrase);
		// get syllables of the phrase
		String[] syllables = phrase.split("\\s+");
		return syllables;
	}
	
	/**
	 * Segment a phrase.
	 * @see #normalize(String)
	 * @param phrase
	 * @return a list of possible segmentations.
	 */
	public String[] segment(String phrase) {
		
		String strRet[] = null; 
		DFALexiconRecognizer recognizer = (DFALexiconRecognizer) getDFALexiconRecognizer(); 
		if(recognizer.accept(phrase.toLowerCase())) {
			strRet = recognizer.getWordList();
		}
		
		return strRet;
	}
	
	/**
	 * @param segmentations a list of possible segmentations.
	 * @return the most probable segmentation
	 */
	public String[] resolveAmbiguity(List<String[]> segmentations) {
		return resolver.resolve(segmentations);
	}
	


	/**
	 * Print the result of the segmentation.
	 */
	public void printResult() {
		for (Iterator<String[]> it = result.iterator(); it.hasNext(); ) {
			String[] segmentation = it.next();
			for (int i = 0; i < segmentation.length; i++) {
				System.out.print("[" + segmentation[i]+"] ");
			}
			System.out.println();
		}
	}
	/**
	 * Dispose the segmenter to save space. 
	 */
	public void dispose() {
		result.clear();
		lexiconRecognizer.dispose();
		externalLexiconRecognizer.dispose();
	}
	
}
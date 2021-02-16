/**
 * (C) Le Hong Phuong, phuonglh@gmail.com
 *  Vietnam National University, Hanoi, Vietnam.
 */
package com.giosis.vn.nlp.extend;

import vn.hus.nlp.fsm.IConstants;
import vn.hus.nlp.tokenizer.segmenter.AbstractLexiconRecognizer;

/**
 * @author Le Hong Phuong, phuonglh@gmail.com
 * <p>
 * vn.hus.nlp.segmenter
 * <p>
 * Nov 12, 2007, 8:44:14 PM
 * <p>
 * A recognizer for Vietnamese lexicon that uses an internal DFA representation.
 */
public final class DFALexiconRecognizer extends AbstractLexiconRecognizer {
	
	private static DFA lexiconDFA = null;
	
	private static DFASimulator simulator = null;
	
	private static DFALexiconRecognizer recognizer = null;
	
	private String[] wordList = null;
	
	/**
	 * Private constructor.
	 * @param dfaLexiconFilename
	 */
	private DFALexiconRecognizer(String dfaLexiconFilename) {
		if (lexiconDFA == null) {
			// build the lexicon DFA
//			System.out.print("Load the lexicon automaton... ");
			lexiconDFA = (DFA) new FSMUnmarshaller().unmarshal(dfaLexiconFilename, IConstants.FSM_DFA);
//			System.out.println("OK.");
		}
	}
	
	/**
	 * @param dfaLexiconFilename the DFA lexicon filen
	 * @return The singleton instance of the lexicon DFA. 
	 */
	public static DFALexiconRecognizer getInstance(String dfaLexiconFilename) {
		if (recognizer == null) {
			recognizer = new DFALexiconRecognizer(dfaLexiconFilename);
		}
		return recognizer;
	}

	/**
	 * @return the DFA simulator
	 */
	private DFASimulator getDFASimulator() {
		if (simulator == null) {
			simulator = (DFASimulator)lexiconDFA.getSimulator();
		}
		return simulator;
	}
	
	/* (non-Javadoc)
	 * @see vn.hus.segmenter.AbstractLexiconRecognizer#accept(java.lang.String)
	 */
	@Override
	public boolean accept(String token) {
//		long s = System.nanoTime();
		boolean f = getDFASimulator().accept(token);
		wordList = getDFASimulator().getWordList();
//		System.out.println("DFALexiconRecognizer "+token+" elpased : " + (System.nanoTime() - s)/1000000.0);
		return f;
	}

	/* (non-Javadoc)
	 * @see vn.hus.segmenter.AbstractLexiconRecognizer#dispose()
	 */
	@Override
	public void dispose() {
		lexiconDFA.dispose();
	}
	
	public String[] getWordList() {
		return wordList;
	}
}

/**
 * (C) Le Hong Phuong, phuonglh@gmail.com
 *  Vietnam National University, Hanoi, Vietnam.
 */
package com.giosis.vn.nlp.extend;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import vn.hus.nlp.fsm.ConfigurationEvent;
import vn.hus.nlp.fsm.ISimulatorListener;
import vn.hus.nlp.fsm.Simulator;
import vn.hus.nlp.fsm.State;
import vn.hus.nlp.fsm.fsa.DFAConfiguration;

/**
 * @author Le Hong Phuong, phuonglh@gmail.com
 *         <p>
 *         Nov 7, 2007, 9:57:44 PM
 *         <p>
 *         An implementation of deterministic simulators for DFA. A
 *         deterministic simulator is at one and only one state or configuration
 *         at a time.
 */
public class DFASimulator extends Simulator {

	/**
	 * The dfa that the simulator operates on.
	 */
	protected DFA dfa;
	/**
	 * The configuration the machine could possibly be in at a given moment in
	 * the simulation.
	 */
	protected DFAConfiguration configuration = null;
	
	/**
	 * A simple logger for the simulator.
	 */
	protected SimulatorLogger logger = null;

	/**
	 * Print out the trace of the simulator or not (DEBUG mode).
	 */
	private final boolean DEBUG = false;
	
	protected String[] wordList;
	public String[] getWordList() {
		return wordList;
	}

	/**
	 * @author Le Hong Phuong, phuonglh@gmail.com
	 * <p>
	 * vn.hus.fsm
	 * <p>
	 * Nov 9, 2007, 3:57:28 PM
	 * <p>
	 * A simple logger for the {@link DFASimulator} to log its processing.
	 */
	class SimulatorLogger implements ISimulatorListener {

		private final Logger logger;
		
		public SimulatorLogger() {
			logger = Logger.getLogger(DFASimulator.class.getName());
			// use a console handler to trace the log
			logger.addHandler(new ConsoleHandler());
			logger.setLevel(Level.FINEST);
		}
		
		
		public void update(ConfigurationEvent configurationEvent) {
			// log the configuration event
			logger.log(Level.INFO, configurationEvent.toString());
		}
		
	}
	
	/**
	 * Default constructor.
	 * 
	 * @param dfa an dfa.
	 */
	public DFASimulator(DFA dfa) {
		// invoke the parent's constructor to
		// init listeners
		super();
		this.dfa = dfa;
		if (DEBUG) {
			logger = new SimulatorLogger();
			addSimulatorListener(logger);
		}
	}

	/**
	 * Find the next configuration of the DFA.
	 * 
	 * @param configuration
	 * @return The next configuration of current configuration or null if the
	 *         simulator cannot go further.
	 */
	protected DFAConfiguration next(DFAConfiguration configuration) {
		DFAConfiguration nextConfiguration = null;
		// get information of current configuration
		State currentState = configuration.getCurrentState();
		String unprocessedInput = configuration.getUnprocessedInput();
		int len = unprocessedInput.length();
		if (len > 0) {
			// get all inputs of outtransitions of the current state
			char[] outTransitionInputs = currentState.getOutTransitionInputs();
			// get the first character of the unprocessed input
			char nextInput = unprocessedInput.charAt(0);
			// find the next configuration
			for (int i = 0; i < outTransitionInputs.length; i++) {
				if (outTransitionInputs[i] == nextInput) {
					// get the next state (possible null)
					State nextState = dfa.getNextState(currentState, nextInput);
					if (nextState != null) {
						// create the next configuration
						if (unprocessedInput.length() > 0) {
							unprocessedInput = unprocessedInput.substring(1);
						}
						nextConfiguration = new DFAConfiguration(nextState, configuration, 
								configuration.getTotalInput(), unprocessedInput);
						// create a configuration event and notify all registered listeners
						if (DEBUG) {
							notify(new ConfigurationEvent(configuration, nextConfiguration, nextInput)); // DEBUG
						}
					}
				}
			}
		}
		return nextConfiguration;
	}

	/**
	 * Track an input on the DFA.
	 * 
	 * @param input
	 *            an input
	 * @return the configuration at which the machine cannot go further on the
	 *         input.
	 *         
	 * 설	명  : 단어를 한글자씩 보며 사전과 비교해 상태를 결정한다.
	 * configuration.getUnprocessedInput() : 아직 분석하지 않은 남은 문자들 
	 * 			
	 */
	
	public DFAConfiguration track(String input) {
		String phrase = input; //분석할 남은 단어들
		StringBuffer terms = new StringBuffer(); //리턴할 Term을 모아두는 변수
		
		//최초 상태 초기화
		configuration = new DFAConfiguration(dfa.getInitialState(), null, input, input);
		
		while (configuration != null) {
			DFAConfiguration nextConfiguration = next(configuration);
			if (nextConfiguration == null) {
				
				//분석할 단어가 남아있다면
				if(configuration.getUnprocessedInput().length() > 1) {
					DFAConfiguration parent = getParent(configuration);
					String tmpTerm =  ""; //분석한 단어
				
					//직전 상태가 공백으로 시작한다면
					if(configuration.getUnprocessedInput().startsWith(" ") || parent.getUnprocessedInput().startsWith(" ")) {
						//남은 처리할 단어 중에  UnprocessedInput이 시작되는 부분까지 잘라냄 (term 추출)
						//연속해서 같은 단어가 반복될 경우, 첫번째 단어만 뽑아냄
						if(phrase.indexOf(configuration.getUnprocessedInput()) == 0) {
							tmpTerm = phrase.substring(0, phrase.indexOf(configuration.getUnprocessedInput(), configuration.getUnprocessedInput().length())).trim() + ",";
						}
						else
							tmpTerm = phrase.substring(0, phrase.indexOf(configuration.getUnprocessedInput())).trim() + ",";
					}
					//문자 중간에 끊겼을 경우(ex:nike가 아닌 nikb 라고 검색이 들어오면 nik에서 끊긴다) 끊긴 곳 앞에 존재하는 공백을 찾는다.
					else {
						DFAConfiguration gParent = getParent(parent);
						while(gParent!=null && !gParent.getUnprocessedInput().startsWith(" ")){
							gParent = getParent(gParent);
						}
						
						//공백을 찾은 경우, 그 공백의 index 까지를 잘라냄
						if(gParent != null) {
							tmpTerm = phrase.substring(0, phrase.indexOf(gParent.getUnprocessedInput())).trim()+ ",";
						}
						//분석할 남은 단어가 마지막 단어인 경우
						else if(phrase.indexOf(" ") < 0) {
							tmpTerm = phrase + ",";
						} else { 
							tmpTerm = phrase.substring(0, phrase.indexOf(" ")).trim()+ ",";
						}
					}
					
					//최종 Terms에 추가
					terms.append(tmpTerm);
					
					//분석된 텀은 남은 작업대상 단어에서 뺀다.
					phrase = phrase.substring(tmpTerm.length()-1).trim();
					
					nextConfiguration = new DFAConfiguration(dfa.getInitialState(), null, input, phrase);
				}
				//마지막 단어라면
				else {
					terms.append(phrase);
					
					//리턴할 String[] 만들기.
					wordList = terms.toString().split(",");
					return configuration;
				}
			}
			configuration = nextConfiguration;
		}


		wordList = terms.toString().split(",");
		return configuration;
	}
	
	

	//실행 결과 리턴.
	@Override
	public boolean accept(String input) {
		boolean ret = true;
		try {
			track(input);
		}catch(Exception e) {
			ret = false;
		}
		
		return ret;
	}
	
	//부모 상태를 리턴
	public DFAConfiguration getParent(DFAConfiguration config) {
		return (DFAConfiguration) config.getParent();
	}
	

	/**
	 * A run of the dfa on an input does not return any 
	 * result.
	 * 
	 * @see vn.hus.nlp.fsm.Simulator#run(java.lang.String)
	 */
	@Override
	public String run(String input) {
		return null;
	}

}

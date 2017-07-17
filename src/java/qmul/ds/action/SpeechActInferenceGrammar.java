/*******************************************************************************
 * Copyright (c) 2001, 2010 Matthew Purver
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "LICENSE" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *******************************************************************************/
package qmul.ds.action;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import qmul.ds.action.atomic.Effect;

/**
 * A set of {@link ComputationalAction}s that define speech act inference rules,
 * e.g.
 * 
 * IF  		ty(t)
 * 	   		sem << [x==that:e|p==right(x):t] 
 * THEN 	put(sa:accept) 
 * ELSE 	abort
 * 
 * @author arash
 */
public class SpeechActInferenceGrammar extends HashMap<String, ComputationalAction> implements Serializable {

	private static Logger logger = Logger.getLogger(SpeechActInferenceGrammar.class);

	private static final long serialVersionUID = 1L;

	public static final String FILE_NAME = "speech-act-inference-grammar.txt";

	private static final String ALWAYS_GOOD_PREFIX = "*";

	private static final String BACKTRACK_ON_SUCCESS_PREFIX = "+";

	/**
	 * Read a set of {@link ComputationalAction}s from file
	 * 
	 * @param dir
	 *            containing the computational-actions.txt file
	 */
	public SpeechActInferenceGrammar(File dir) {
		super();
		logger.info("Reading Speech Act Grammar from:"+dir);
		System.out.println("Reading Speech Act Grammar from:"+dir);
		File file = new File(dir, FILE_NAME);
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			initActions(reader);
		} catch (FileNotFoundException e) {
			
			logger.warn("No speech act inference file " + file.getAbsolutePath());
			
		}
	}

	/**
	 * Read a set of {@link ComputationalAction}s from file
	 * 
	 * @param dirNameOrURL
	 *            containing the computational-actions.txt file
	 */
	public SpeechActInferenceGrammar(String dirNameOrURL) {
		BufferedReader reader;
		try {
			if (dirNameOrURL.matches("(https?|file):.*")) {
				reader = new BufferedReader(
						new InputStreamReader(new URL(dirNameOrURL.replaceAll("/?$", "/") + FILE_NAME).openStream()));
			} else {
				File file = new File(dirNameOrURL, FILE_NAME);
				reader = new BufferedReader(new FileReader(file));
			}

			initActions(reader);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e);
			logger.error("Error reading speech act grammar file " + dirNameOrURL);
		}
	}
	
	/**
	 * Read a set of {@link ComputationalAction}s from file
	 * 
	 * @param dirNameOrURL
	 *            containing the computational-actions.txt file
	 */
	public SpeechActInferenceGrammar(String dirNameOrURL, String filename) {
		BufferedReader reader;
		try {
			if (dirNameOrURL.matches("(https?|file):.*")) {
				reader = new BufferedReader(
						new InputStreamReader(new URL(dirNameOrURL.replaceAll("/?$", "/") + filename).openStream()));
			} else {
				File file = new File(dirNameOrURL, filename);
				reader = new BufferedReader(new FileReader(file));
			}

			initActions(reader);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e);
			logger.error("Error reading speech act grammar file " + dirNameOrURL);
		}
	}

	public SpeechActInferenceGrammar() {
		super();
	}

	public SpeechActInferenceGrammar(SpeechActInferenceGrammar nonoptionalGrammar) {
		super(nonoptionalGrammar);
	}

	/**
	 * Read a set of {@link ComputationalAction}s from file
	 * 
	 * @param reader
	 *            containing the computational-actions.txt file data
	 */
	private void initActions(BufferedReader reader) {
		try {
			String line;
			String name = null;
			boolean alwaysGood = false;
			boolean backtrackOnSuccess = false;
			List<String> lines = new ArrayList<String>();
			while ((line = reader.readLine()) != null) {
				line = Lexicon.comment(line.trim());
				if ((line == null) || (line.isEmpty() && lines.isEmpty())) {
					continue;
				}
				if (line.isEmpty() && !lines.isEmpty()) {
					ComputationalAction action = new ComputationalAction(name, lines);
					action.setAlwaysGood(alwaysGood);
					action.setBacktrackOnSuccess(backtrackOnSuccess);
					put(name, action);
					logger.debug("Added as: " + action);
					lines.clear();
					name = null;
					alwaysGood = false;
				} else if (name == null) {
					name = line;
					alwaysGood = name.startsWith(ALWAYS_GOOD_PREFIX);
					if (alwaysGood) {
						name = name.substring(ALWAYS_GOOD_PREFIX.length());
					}
					backtrackOnSuccess = name.startsWith(BACKTRACK_ON_SUCCESS_PREFIX);
					if (backtrackOnSuccess) {
						name = name.substring(BACKTRACK_ON_SUCCESS_PREFIX.length());
					}
					logger.debug(
							"New speech act inference action: " + name + " (" + alwaysGood + "," + backtrackOnSuccess + ")");
				} else {
					lines.add(line);
				}
			}
			if (!lines.isEmpty()) {
				ComputationalAction action = new ComputationalAction(name, lines);
				put(name, action);
				action.setAlwaysGood(alwaysGood);
				action.setBacktrackOnSuccess(backtrackOnSuccess);
				logger.debug("Added as: " + action + "(always good:" + action.isAlwaysGood() + ", exhaustive:"
						+ action.backtrackOnSuccess() + ")");
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
			logger.error(e);
			logger.error("Error reading speech act grammar from " + reader);
		}
		logger.info("Loaded speech act grammar with " + size() + " speech act inference action entries.");
		logger.trace(this);
	}
	
//	public void addNewComputationalAction(String name, Effect effect){
//		String key = this.getNewKey(name);
//		logger.debug("name: " + name +"; key= " + key); 
//		
//		this.put(key, new ComputationalAction(key, effect));
//	}
//	
//	private String getNewKey(String name){
//		int index = 0;
//		
//		Iterator<Entry<String, ComputationalAction>> iterator = this.entrySet().iterator();
//		while(iterator.hasNext()){
//			Entry<String, ComputationalAction> entry = iterator.next();
//			String key = entry.getKey();
//
//			if(key.contains(name)){
//				String sub = key.replace(name+"-", "");
//				
//				if(this.isInteger(sub.trim())){
//					int exisit_index = Integer.valueOf(sub.trim());
//					if(exisit_index >= index)
//						index = exisit_index;
//				}
//			}
//		}
//		
//		return name + "-" + String.valueOf(index+1);
//	}
//
//	private boolean isInteger(String s) {
//	    try { 
//	        Integer.parseInt(s); 
//	    } catch(NumberFormatException e) { 
//	        return false; 
//	    } catch(NullPointerException e) {
//	        return false;
//	    }
//	    // only got here if we didn't return false
//	    return true;
//	}
}

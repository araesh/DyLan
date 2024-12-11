package qmul.ds.learn;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import qmul.ds.formula.TTRRecordType;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.util.Pair;

/**
 * Objects of this front-end class learn unknown words from a corpus of parsed sentences. The class provides methods for
 * parsing and loading corpora.
 * It makes use of a {@link Hypothesiser} to hypothesise whole action sequences that lead from the axiom tree to the
 * target tree. These are then split via {@code CandidateSequence.split()} into their comprising parts. These
 * different split possibilities are then handed over to the {@link WordHypothesisBase} for generalisation and
 * probability estimation/update. This happens incrementally, i.e. one training example at a time as they are
 * encountered in the corpus.
 * 
 * @author arash
 * 
 */

public class TTRWordLearner extends WordLearner<TTRRecordType>{
	
	public static final Logger logger=Logger.getLogger(TTRWordLearner.class);
	public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI_GREEN = "\u001B[32m";
	public static final String ANSI_YELLOW = "\u001B[33m";
	public static final String ANSI_BLUE = "\u001B[34m";
	public static final String ANSI_PURPLE = "\u001B[35m";
	public static final String ANSI_CYAN = "\u001B[36m";
	public static final String ANSI_RED = "\u001B[31m";


	public TTRWordLearner(String seedResourceDir, RecordTypeCorpus c) {
		hypothesiser = new TTRHypothesiser(seedResourceDir);
		corpus = c;
		this.corpusIterator = corpus.iterator();
	}
	
	public TTRWordLearner(String resourceDir, String corpusFileName) throws IOException, ClassNotFoundException {
		this(resourceDir);
		this.loadCorpus(new File(corpusFileName));
	}

	public TTRWordLearner(String seedResourceDir) {
		this.seedResourceDir= seedResourceDir; //"resource" + File.separator + "2013-english-ttr-induction-seed";
		hypothesiser = new TTRHypothesiser(seedResourceDir);
		corpus = null;
	}

	/**
	 * Generated by Copilot:
	 * Default constructor for the TTRWordLearner class.
	 * Initializes the seedResourceDir with a predefined path to the seed resource directory.
	 * Creates a new TTRHypothesiser with the seed resource directory.
	 * `seed` here refers to a beginning point for the lexicon and grammar (to not start from zero!): Ash
	 */
	public TTRWordLearner() {
		seedResourceDir="resource" + File.separator + "2013-english-ttr-induction-seed";
		hypothesiser = new TTRHypothesiser(seedResourceDir);
		corpus = null;
	}


	@Override
	public boolean learnOnce() {
		if (corpusIterator == null) {
			logger.info("No corpus loaded.");
			return false;
		}
		if (!corpusIterator.hasNext()) {
			logger.info("No more examples in the corpus.");
			return false;
		}

		Pair<Sentence<Word>, TTRRecordType> entry = corpusIterator.next();
		logger.info("Hypothesising sequences for utterance: " + entry.first());
		// logger.info("Hypothesising from training example: "+
		// sentence+"->"+target);
		long time = System.currentTimeMillis();
		Collection<CandidateSequence> hyps = null;
		try {
			((TTRHypothesiser)hypothesiser).loadTrainingExample(entry.first(), entry.second());
			hyps = hypothesiser.hypothesise();
			logger.info("\n");
			if (hyps.size() == 0) {
				logger.warn(ANSI_YELLOW + "NO SEQUENCES RECEIVED from hypothesiser! skipping... " + ANSI_RESET);
//				System.out.println("no sequences returned, skipping this");
				skipped.add(entry);
				return true;
			}
		} catch (Exception e) {
			logger.error("problem hypothesising. Sentence:" + entry);
			e.printStackTrace();
			logger.error("Skipping...");
			skipped.add(entry);
			return true;
		}
		logger.info(ANSI_GREEN +  "Got " + hyps.size() + " sequences from Hypothesiser for "+ entry.second() + ANSI_RESET);
		logger.info(ANSI_GREEN + "Now splitting the sequences..." + ANSI_RESET);
		// DAGHypothesiser.printHypMap(hyps);
		hb.forgetCurrentDist();
		int totalSplit = 0;  // AA: Better be called `totalSplits`!
		int i = 0;
		try {
			for (CandidateSequence cs: hyps) {
				i++;
				logger.debug("Splitting: " + cs.toShortString());
				Set<List<CandidateSequence>> splitSequences = cs.split();
				for (List<CandidateSequence> seq: splitSequences)
					logger.trace("Result: " + seq);
				totalSplit += splitSequences.size();
				logger.trace(i + ":" + splitSequences.size()+ " ");
				logger.debug("Adding split sequences to hypothesis base...");
				hb.addSequenceTuples(splitSequences);
			}
			logger.info("\n");
			this.hb.updateDistsEndOfExample(entry.first());
			logger.info("Processing took:"+ (System.currentTimeMillis()-time)/1000 + " seconds");  // AA Not working correctly!
		} catch (Exception e) {
			logger.fatal("problem while updating distributions on sentence:" + entry);
			logger.fatal("this is fatal :(");
			e.printStackTrace();
			System.exit(1);
		}
		// System.out.println("All Done. Prior after " + sentence);
		// System.out.println(hb.getPrior());
		return true;
	}


	@Override
	public void loadCorpus(File corpusFile) throws IOException, ClassNotFoundException {
		RecordTypeCorpus c=new RecordTypeCorpus();
		c.loadCorpus(corpusFile);
		this.corpus=c;
		this.corpusIterator=this.corpus.iterator();
	}

	
	public static void main(String[] args) {
//		TTRWordLearner learner = new TTRWordLearner();  // Commented out by Arash A.
		String babyDSPath = "resource\\2023-babyds-induction-output\\".replace("\\", File.separator);  // fix later
		String corpusPath = babyDSPath + "babyds_train_86.txt";//"CHILDES400.txt";//"dataset.txt";//"AAtrain-3-testInduction.txt";
		String lexiconPath = babyDSPath + "lexicon.lex";
		TTRWordLearner learner = new TTRWordLearner(babyDSPath);
//		logger.info(ANSI_YELLOW + "learner initialized with seed resource dir: " + babyDSPath + ANSI_RESET);
		try {
//			learner.loadCorpus(new File(args[2]));  // Commented out by Arash A.
//			learner.learn();
//			learner.getHypothesisBase().saveLearnedLexicon("resource/2013-ttr-learner-output/lexicon.lex", 2);
//			learner.getHypothesisBase().saveLearnedLexicon("resource/2013-ttr-learner-output/lexicon.lex", 3);
//			learner.getHypothesisBase().saveLearnedLexicon("resource/2013-ttr-learner-output/lexicon.lex", 4);
//			learner.getHypothesisBase().saveLearnedLexicon("resource/2013-ttr-learner-output/lexicon.lex", 5);

			File corpusFile = new File(corpusPath);
			learner.loadCorpus(corpusFile);
			learner.learn();

			learner.getHypothesisBase().saveLearnedLexicon(lexiconPath, 1);  // Testing if top-1 can be a thing here:
			learner.getHypothesisBase().saveLearnedLexicon(lexiconPath, 2);
			learner.getHypothesisBase().saveLearnedLexicon(lexiconPath, 3);
			learner.getHypothesisBase().saveLearnedLexicon(lexiconPath, 4);
			learner.getHypothesisBase().saveLearnedLexicon(lexiconPath, 5);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}

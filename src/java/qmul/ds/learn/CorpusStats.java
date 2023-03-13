/**
 * A class that computes some statistics about a TTRCorpus by receiving samples in the dataset/corpus as a Sentence when they are being processed,
 * and updating stats as they are received. And finally returning these stats as a string.
 * Used in GeneratorTester in seperateParseableData.
 * There is another class called CorpusStatistics but was not working correctly and debugging it was taking a lot of time; So I wrote this to help me add
 * dataset stats at the end of txt files.
 *
 * @author Arash Ashrafzadeh
 */

package qmul.ds.learn;

import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.Word;

import java.util.Collections;
import java.util.HashMap;

public class CorpusStats {

    HashMap<String, Integer> wordsCountMap = new HashMap<>(); // A map from words in the dataset to their number of occurrence.
    Integer totalWordsCount = 0;
    HashMap<Integer, Integer> sentLenCountMap = new HashMap<>(); // A map from sentence length to the count of sentences with that length in the dataset.
    Integer corpusSize = 0;

    /**
     * Receives a sentence and:
     * - increments corpusSize by 1.
     * - calls statUpdate on the sentence to update corpus stats appropriately.
     *
     * @param sentence
     */
    public void addSentence(Sentence<Word> sentence) {
        corpusSize++;
        statUpdater(sentence);
    }

    /**
     * Updates word-level and sentence-level information about the corpus based on the sentence it receives.
     * Called from `addSentence`.
     *
     * @param sentence
     */
    public void statUpdater(Sentence<Word> sentence) {

        // ------Update word stats------
        for (Word w : sentence) {
            String ws = w.word();
            Integer count = wordsCountMap.getOrDefault(ws, 0) + 1;
            wordsCountMap.put(ws, count);
            totalWordsCount += 1;
        }
        // ------Update sentence stats------
        Integer len = sentence.size();
        sentLenCountMap.put(len, sentLenCountMap.getOrDefault(len, 0) + 1);
    }

    /**
     * Returns a string containing the stats gathered from the corpus.
     *
     * @return finalStats
     */
    public String statReporter() {

        // ------Make sentence stats------
        String sentStats = "\n\n---- Sentence-Level Stats ----\n";
        Integer totalSentLen = 0; // Used to calculate average sentence length.
        for (Integer len : sentLenCountMap.keySet()) {
            Integer count = sentLenCountMap.get(len);
            sentStats += String.format("Count of sentences with length %d is %d.\n", len, count);
            totalSentLen += len*count;
        }
        sentStats += String.format("\nCorpus size: %d", corpusSize);
        Integer maxLen = Collections.max(sentLenCountMap.keySet()); // AA: reference: https://stackoverflow.com/questions/14831045/find-the-biggest-number-in-hashset-hashmap-java
        sentStats += String.format("\nMaximum length: %d", maxLen);
        Integer minLen = Collections.min(sentLenCountMap.keySet());
        sentStats += String.format("\nMinimum length: %d", minLen);
        Double avgLen = Double.valueOf(totalSentLen) / corpusSize;
        sentStats += String.format("\nAverage length: %f\n", avgLen);

        // ------Make word stats------
        String wordStats = "\n---- Word-Level Stats ----";
        wordStats += String.format("\nCount of all words: %d", totalWordsCount);
        Integer tokensCount = wordsCountMap.keySet().size();
        wordStats += String.format("\nCount of unique tokens: %d", tokensCount);
        Double ratio = Double.valueOf(totalWordsCount) / tokensCount;
        wordStats += String.format("\nWords/Tokens count ratio: %f", ratio);

        String finalStats = "\n\n\n====== DATASET STATS ======";
        finalStats += sentStats;
        finalStats += wordStats;
        return finalStats;
    }
}

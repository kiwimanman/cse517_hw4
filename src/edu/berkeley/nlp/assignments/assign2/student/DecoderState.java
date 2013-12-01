package edu.berkeley.nlp.assignments.assign2.student;

import edu.berkeley.nlp.langmodel.EnglishWordIndexer;
import edu.berkeley.nlp.langmodel.NgramLanguageModel;
import edu.berkeley.nlp.mt.decoder.DistortionModel;
import edu.berkeley.nlp.mt.phrasetable.ScoredPhrasePairForSentence;
import edu.berkeley.nlp.util.StringIndexer;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Keith Stone
 */
public abstract class DecoderState {
    /**
     * @param ngram
     * @param lexIndexer
     * @return
     */
    protected static int[] toArray(final List<String> ngram, StringIndexer lexIndexer) {
        int[] ngramArray = new int[ngram.size()];
        for (int w = 0; w < ngramArray.length; ++w) {
            ngramArray[w] = lexIndexer.addAndGetIndex(ngram.get(w));
        }
        return ngramArray;
    }

    protected static int[] toArray(final List<String> ngram) {
        return toArray(ngram, EnglishWordIndexer.getIndexer());
    }

    DecoderState backPointer;
    ScoredPhrasePairForSentence score;
    List<String> priorNgram;

    public DecoderState() {

    }

    public abstract double getPriority(NgramLanguageModel languageModel, DistortionModel distortionModel);
    public abstract int    getDecodedLength();

    public List<String> getPriorNgram(NgramLanguageModel languageModel) {
        if (priorNgram != null)
            return priorNgram;

        int totalWords = languageModel.getOrder() - 1;
        if (score == null) {
            priorNgram = new ArrayList<String>();
            for (int i = 0; i < totalWords; i++) {
                priorNgram.add(NgramLanguageModel.START);
            }
            return priorNgram;
        }

        List<String> english = score.getEnglish();
        if (english.size() < totalWords) {
            List<String> partialList;
            List<String> previous = backPointer.getPriorNgram(languageModel);
            List<String> missingWords = previous.subList(previous.size() - (totalWords - english.size()), previous.size());
            partialList = new ArrayList<String>(missingWords);
            partialList.addAll(english);
            priorNgram = partialList;
        } else {
            int from = english.size() - totalWords;
            int to = english.size();
            priorNgram = english.subList(from, to);

        }
        return priorNgram;
    }

    public List<ScoredPhrasePairForSentence> decode() {
        if (backPointer == null) {
            return new ArrayList<ScoredPhrasePairForSentence>();
        } else {
            List<ScoredPhrasePairForSentence> previousScores = backPointer.decode();
            previousScores.add(score);
            return previousScores;
        }
    }

    public DecoderState getBackPointer() {
        return backPointer;
    }

    public ScoredPhrasePairForSentence getScore() {
        return score;
    }
}

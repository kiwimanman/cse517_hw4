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
        return toArray(ngram, indexer);
    }

    static StringIndexer indexer = EnglishWordIndexer.getIndexer();
    static int START_TOKEN_INDEX = DecoderState.indexer.indexOf(NgramLanguageModel.START);

    DecoderState backPointer;
    ScoredPhrasePairForSentence score;
    int[] priorNgram = { START_TOKEN_INDEX, START_TOKEN_INDEX };
    Double priority;
    int decodedLength = 0;
    int translatedLength = 0;
    Integer hashcode;
    NgramLanguageModel languageModel;
    DistortionModel distortionModel;

    public DecoderState(DecoderState backPointer, ScoredPhrasePairForSentence score, NgramLanguageModel languageModel, DistortionModel distortionModel) {
        this.languageModel = languageModel;
        this.distortionModel = distortionModel;

        this.score = score;
        this.backPointer = backPointer;

        int foreignLength = score == null ? 0 : score.getForeignLength();
        int decodedLength = backPointer == null ? 0 : backPointer.getDecodedLength();
        this.decodedLength = foreignLength + decodedLength;

        int englishLength = score == null ? 0 : score.getEnglish().size();
        int translatedLength = backPointer == null ? 0 : backPointer.getTranslatedLength();
        this.translatedLength = englishLength + translatedLength;
    }

    public double getPriority() {
        if (priority == null) {
            this.priority  = (score       == null ? 0.0 : score.score);
            this.priority += (backPointer == null ? 0.0 : backPointer.getPriority());
            this.priority += scoreWithLanguageModel();
            this.priority += scoreWithDistortionModel();
        }
        return priority;
    }

    public int getDecodedLength() {
        return decodedLength;
    }

    public int getTranslatedLength() {
        return translatedLength;
    }

    protected void buildPriorNgram() {
        if (score != null) {
            int[] english = toArray(score.getEnglish());
            int translationSize = english.length;

            if (translationSize == 0) {
                int[] previousNgram = backPointer.getPriorNgram();
                priorNgram[0] = previousNgram[0];
                priorNgram[1] = previousNgram[1];
            } else if (translationSize == 1) {
                priorNgram[0] = backPointer.getPriorNgram()[1];
                priorNgram[1] = english[0];
            } else {
                priorNgram[0] = english[translationSize - 2];
                priorNgram[1] = english[translationSize - 1];
            }
        }
    }

    public int[] getPriorNgram() {
        return priorNgram;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DecoderState that = (DecoderState) o;

        if (decodedLength    != that.decodedLength)    return false;
        if (translatedLength != that.translatedLength) return false;
        if (priorNgram[0]    != that.priorNgram[0])    return false;
        if (priorNgram[1]    != that.priorNgram[1])    return false;

        return true;
    }

    @Override
    public int hashCode() {
        if (hashcode != null) return hashcode;

        int result = priorNgram.hashCode();
        result = 31 * result + 13 * translatedLength + decodedLength;
        hashcode = result;
        return hashcode;
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

    abstract protected double scoreWithLanguageModel();

    abstract protected double scoreWithDistortionModel();

    public DecoderState getBackPointer() {
        return backPointer;
    }

    public ScoredPhrasePairForSentence getScore() {
        return score;
    }
}

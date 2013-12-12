package edu.berkeley.nlp.assignments.assign2.student;

import edu.berkeley.nlp.langmodel.EnglishWordIndexer;
import edu.berkeley.nlp.langmodel.NgramLanguageModel;
import edu.berkeley.nlp.mt.decoder.DistortionModel;
import edu.berkeley.nlp.mt.phrasetable.PhraseTable;
import edu.berkeley.nlp.mt.phrasetable.ScoredPhrasePairForSentence;
import edu.berkeley.nlp.util.Pair;
import edu.berkeley.nlp.util.StringIndexer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * @author Keith Stone
 */
public abstract class DecoderState implements Iterable<Pair<Integer, Integer>> {
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

    Double previousScore;
    Double translationScore;
    Double languageModelScore;
    Double distortionScore;

    boolean[] decodedMask;
    int decodedLength = 0;
    int translatedLength = 0;
    Integer hashcode;
    NgramLanguageModel languageModel;
    DistortionModel distortionModel;
    Integer unbrokenTranslationLength;
    PhraseTable phraseTable;

    public DecoderState(DecoderState backPointer, ScoredPhrasePairForSentence score, int i, int j, NgramLanguageModel languageModel, DistortionModel distortionModel, PhraseTable phraseTable) {
        this.languageModel = languageModel;
        this.distortionModel = distortionModel;
        this.phraseTable = phraseTable;

        this.score = score;
        this.backPointer = backPointer;

        int foreignLength = score == null ? 0 : score.getForeignLength();
        int englishLength = score == null ? 0 : score.getEnglish().size();

        if (backPointer != null) {
            int decodedLength  = backPointer.getDecodedLength();
            this.decodedLength = foreignLength + decodedLength;

            int translatedLength  = backPointer.getTranslatedLength();
            this.translatedLength = englishLength + translatedLength;

            decodedMask = java.util.Arrays.copyOfRange(backPointer.decodedMask, 0, backPointer.decodedMask.length);
            for (int k = i; k < j; k++) {
                decodedMask[k] = true;
            }
        }
    }

    public double getPriority() {
        if (priority == null) {
            this.translationScore   = (score       == null ? 0.0 : score.score);
            this.previousScore      = (backPointer == null ? 0.0 : backPointer.getPriority());
            this.languageModelScore = scoreWithLanguageModel();
            this.distortionScore    = scoreWithDistortionModel();
            this.priority = previousScore + translationScore + languageModelScore + distortionScore;
        }
        return priority;
    }

    public int getDecodedLength() {
        return decodedLength;
    }

    public int getTranslatedLength() {
        return translatedLength;
    }

    public boolean isLegalPhrase(int start, int end) {
        if (end <= start) return false;
        if (end > decodedMask.length)  return false;
        for (int i = start; i < end; i++) {
            if (decodedMask[i]) return false;
        }
        return true;
    }

    public int getForeignSideEndIndex() {
        return score == null ? 0 : score.getEnd();
    }

    public int getUnbrokenTranslationLength() {
        if (unbrokenTranslationLength != null) return unbrokenTranslationLength;
        for (unbrokenTranslationLength = 0; unbrokenTranslationLength < decodedMask.length; unbrokenTranslationLength++) {
            if (!decodedMask[unbrokenTranslationLength]) return unbrokenTranslationLength;
        }
        return unbrokenTranslationLength;
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

    public Iterator<Pair<Integer, Integer>> iterator() {
        return new MonotonicIterator(this);
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
        if (getForeignSideEndIndex() != that.getForeignSideEndIndex()) return false;
        if (priorNgram[0]    != that.priorNgram[0])    return false;
        if (priorNgram[1]    != that.priorNgram[1])    return false;
        for (int i = 0; i < decodedMask.length; i++)
            if (decodedMask[i] != that.decodedMask[i]) return false;

        return true;
    }

    @Override
    public int hashCode() {
        if (hashcode != null) return hashcode;

        int result = priorNgram.hashCode();
        int maskResult = decodedMask.hashCode();
        result = 31 * result + 17 * maskResult + 13 * translatedLength + decodedLength;
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

    public PhraseTable getPhraseTable() {
        return phraseTable;
    }

    public DistortionModel getDistortionModel() {
        return distortionModel;
    }
}

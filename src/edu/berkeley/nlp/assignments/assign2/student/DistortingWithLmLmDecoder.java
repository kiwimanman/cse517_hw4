package edu.berkeley.nlp.assignments.assign2.student;

import edu.berkeley.nlp.langmodel.NgramLanguageModel;
import edu.berkeley.nlp.mt.decoder.DistortionModel;
import edu.berkeley.nlp.mt.phrasetable.PhraseTable;
import edu.berkeley.nlp.mt.phrasetable.PhraseTableForSentence;
import edu.berkeley.nlp.mt.phrasetable.ScoredPhrasePairForSentence;
import edu.berkeley.nlp.util.Pair;
import edu.berkeley.nlp.util.PriorityQueue;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Keith Stone
 */
public class DistortingWithLmLmDecoder extends MonotonicWithLmDecoder {
    public class DistortingStateWithLm extends StateWithLm {
        public DistortingStateWithLm(List<String> sentence, PhraseTable phraseTable, DistortionModel distortionModel) {
            super(sentence, phraseTable);
            this.distortionModel = distortionModel;
        }

        public DistortingStateWithLm(DecoderState backPointer, ScoredPhrasePairForSentence score, int i, int j, NgramLanguageModel languageModel, DistortionModel distortionModel, PhraseTable phraseTable) {
            super(backPointer, score, i, j, languageModel, distortionModel, phraseTable);
            buildPriorNgram();
        }

        protected double scoreWithDistortionModel() {
            if (backPointer == null) return 0;
            int end = backPointer.getForeignSideEndIndex();
            int start = score.getStart();
            double priority = distortionModel.getDistortionScore(end, start);
            return priority;
        }

        public Iterator<Pair<Integer, Integer>> iterator() {
            return new DistortingIterator(this);
        }
    }

    public DistortingWithLmLmDecoder(PhraseTable pt, NgramLanguageModel lm, DistortionModel dm) {
        super(pt, lm, dm);
    }

    protected DecoderState buildState(DecoderState state, ScoredPhrasePairForSentence score, int i, int j) {
        return new DistortingStateWithLm(state, score, i, j, lm, dm, phraseTable);
    }

    protected DecoderState buildStartState(List<String> sentence) {
        return new DistortingStateWithLm(sentence, phraseTable, dm);
    }
}

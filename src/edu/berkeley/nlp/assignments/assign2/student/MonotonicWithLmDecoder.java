package edu.berkeley.nlp.assignments.assign2.student;

import edu.berkeley.nlp.langmodel.EnglishWordIndexer;
import edu.berkeley.nlp.langmodel.NgramLanguageModel;
import edu.berkeley.nlp.mt.decoder.DistortionModel;
import edu.berkeley.nlp.mt.phrasetable.PhraseTable;
import edu.berkeley.nlp.mt.phrasetable.ScoredPhrasePairForSentence;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Keith Stone
 */
public class MonotonicWithLmDecoder extends MonotonicNoLmDecoder {
    public class StateWithLm extends DecoderState {
        public StateWithLm(DecoderState backPointer, ScoredPhrasePairForSentence score, NgramLanguageModel languageModel, DistortionModel distortionModel) {
            super(backPointer, score, languageModel, distortionModel);
            buildPriorNgram();
        }

        protected double scoreWithLanguageModel() {
            double sum = 0.0;

            int[] mungedPartialSentence;

            int[] priorNgram = backPointer.getPriorNgram();
            int[] partialSentence = toArray(score.getEnglish());
            mungedPartialSentence = new int[partialSentence.length + 2];
            System.arraycopy(priorNgram, 0, mungedPartialSentence, 0, 2);
            System.arraycopy(partialSentence, 0, mungedPartialSentence, 2, partialSentence.length);

            for (int i = 0; i < mungedPartialSentence.length - 2; i++) {
                double lmScore = languageModel.getNgramLogProbability(mungedPartialSentence, i, i + 2);
                sum += lmScore;
            }
            return sum;
        }

        protected double scoreWithDistortionModel() {
            return 0.0;
        }
    }

    public MonotonicWithLmDecoder(PhraseTable pt, NgramLanguageModel lm, DistortionModel dm) {
        super(pt, lm, dm);
    }

    protected DecoderState buildState(DecoderState state, ScoredPhrasePairForSentence score) {
        if (state == null && score == null) {
            return new StateNoLm(null, null, lm, dm);
        } else {
            return new StateWithLm(state, score, lm, dm);
        }
    }
}

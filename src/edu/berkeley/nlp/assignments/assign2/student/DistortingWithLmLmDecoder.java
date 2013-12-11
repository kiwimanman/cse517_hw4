package edu.berkeley.nlp.assignments.assign2.student;

import edu.berkeley.nlp.langmodel.NgramLanguageModel;
import edu.berkeley.nlp.mt.decoder.DistortionModel;
import edu.berkeley.nlp.mt.phrasetable.PhraseTable;
import edu.berkeley.nlp.mt.phrasetable.PhraseTableForSentence;
import edu.berkeley.nlp.mt.phrasetable.ScoredPhrasePairForSentence;
import edu.berkeley.nlp.util.PriorityQueue;

import java.util.List;
import java.util.Map;

/**
 * @author Keith Stone
 */
public class DistortingWithLmLmDecoder extends MonotonicWithLmDecoder {
    public class DistortingStateWithLm extends StateWithLm {
        public DistortingStateWithLm(DecoderState backPointer, ScoredPhrasePairForSentence score, int i, int j, NgramLanguageModel languageModel, DistortionModel distortionModel) {
            super(backPointer, score, i, j, languageModel, distortionModel);
            buildPriorNgram();
        }

        protected double scoreWithDistortionModel() {
            int end = backPointer.getUnbrokenTranslationLength();
            int start = score.getStart();
            double priority = distortionModel.getDistortionScore(end, start);
            return priority;
        }
    }

    public DistortingWithLmLmDecoder(PhraseTable pt, NgramLanguageModel lm, DistortionModel dm) {
        super(pt, lm, dm);
    }

    protected void extrapolateState(DecoderState state, Map<Integer, PriorityQueue<DecoderState>> beamMap, PhraseTableForSentence tmState) {
        int i = state.getUnbrokenTranslationLength();
        int maxDistortion = i + dm.getDistortionLimit();
        int phraseSize = phraseTable.getMaxPhraseSize();
        for (; i < maxDistortion; i++) {
            for (int j = 0; j < phraseSize; j++) {
                int end = i + j + 1;
                if (state.isLegalPhrase(i, end)) {
                    List<ScoredPhrasePairForSentence> scores = tmState.getScoreSortedTranslationsForSpan(i, end);
                    if (scores != null) {
                        for (ScoredPhrasePairForSentence score : scores) {
                            DecoderState newState = buildState(state, score, i, end);
                            int beamIndex = newState.getDecodedLength();
                            PriorityQueue<DecoderState> targetBeam =  beamMap.get(beamIndex);
                            double priority = newState.getPriority();
                            targetBeam.setPriority(newState, priority);
                        }
                    }
                }
            }
        }
    }

    protected DecoderState buildState(DecoderState state, ScoredPhrasePairForSentence score, int i, int j) {
        return new DistortingStateWithLm(state, score, i, j, lm, dm);
    }
}

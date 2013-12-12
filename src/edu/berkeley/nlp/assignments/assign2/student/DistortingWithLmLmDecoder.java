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
        public DistortingStateWithLm(DecoderState backPointer, ScoredPhrasePairForSentence score, int i, int j, NgramLanguageModel languageModel, DistortionModel distortionModel, PhraseTable phraseTable) {
            super(backPointer, score, i, j, languageModel, distortionModel, phraseTable);
            buildPriorNgram();
        }

        protected double scoreWithDistortionModel() {
            int end = backPointer.getForeignSideEndIndex();
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
        int maxPhraseEnd = Math.min(maxDistortion, phraseSize + i) + 1;
        for (; i < maxDistortion; i++) {
            for (int j = i + 1; j < maxPhraseEnd; j++) {
                if (state.isLegalPhrase(i, j)) {
                    List<ScoredPhrasePairForSentence> scores = tmState.getScoreSortedTranslationsForSpan(i, j);
                    if (scores != null) {
                        for (ScoredPhrasePairForSentence score : scores) {
                            DecoderState newState = buildState(state, score, i, j);
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
        return new DistortingStateWithLm(state, score, i, j, lm, dm, phraseTable);
    }
}

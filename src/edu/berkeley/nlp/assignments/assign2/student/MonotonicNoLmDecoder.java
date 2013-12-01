package edu.berkeley.nlp.assignments.assign2.student;

import edu.berkeley.nlp.langmodel.NgramLanguageModel;
import edu.berkeley.nlp.mt.decoder.Decoder;
import edu.berkeley.nlp.mt.decoder.DistortionModel;
import edu.berkeley.nlp.mt.phrasetable.PhraseTable;
import edu.berkeley.nlp.mt.phrasetable.PhraseTableForSentence;
import edu.berkeley.nlp.mt.phrasetable.ScoredPhrasePairForSentence;
import edu.berkeley.nlp.util.FastPriorityQueue;
import edu.berkeley.nlp.util.PriorityQueue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Keith Stone
 */

public class MonotonicNoLmDecoder implements Decoder {
    public class StateNoLm extends DecoderState {
        int decodedLength = 0;
        double priority;

        public StateNoLm() {
        }

        public StateNoLm(DecoderState backPointer, ScoredPhrasePairForSentence score, NgramLanguageModel languageModel, DistortionModel distortionModel) {
            this.score = score;
            this.decodedLength = score.getForeignLength() + backPointer.getDecodedLength();
            this.backPointer = backPointer;
            this.priority = backPointer.getPriority(languageModel, distortionModel) + score.score;
        }

        public double getPriority(NgramLanguageModel languageModel, DistortionModel distortionModel) {
            // return Decoder.StaticMethods.scoreHypothesis(decode(), languageModel, distortionModel);
            return priority;
        }

        public int getDecodedLength() {
            return decodedLength;
        }
    }

    PhraseTable phraseTable;
    NgramLanguageModel lm;
    DistortionModel dm;
    int maxBeamSize = 2000;

    public MonotonicNoLmDecoder(PhraseTable pt, NgramLanguageModel lm, DistortionModel dm) {
        super();
        this.phraseTable = pt;
        this.lm = lm;
        this.dm = dm;
    }

    public List<ScoredPhrasePairForSentence> decode(List<String> sentence) {
        Map<Integer, PriorityQueue<DecoderState>> beamMap = buildBeamMap(sentence.size());
        PhraseTableForSentence tmState = phraseTable.initialize(sentence);
        beamMap.get(0).setPriority(new StateNoLm(), 0.0);

        for (int i = 0; i < sentence.size(); i++) {
            PriorityQueue<DecoderState> beam = beamMap.get(i);
            extrapolateBeam(beam, beamMap, tmState);
            cullBeams(beamMap);
        }

        return beamMap.get(sentence.size()).getFirst().decode();
    }

    private Map<Integer, PriorityQueue<DecoderState>> buildBeamMap(int size) {
        Map<Integer, PriorityQueue<DecoderState>> beamMap = new HashMap<Integer, PriorityQueue<DecoderState>>();
        for (int i = 0; i <= size; i++) {
            beamMap.put(i, new FastPriorityQueue<DecoderState>(maxBeamSize));
        }
        return beamMap;
    }

    private void extrapolateState(DecoderState state, Map<Integer, PriorityQueue<DecoderState>> beamMap, PhraseTableForSentence tmState) {
        int i = state.getDecodedLength();
        int phraseSize = phraseTable.getMaxPhraseSize();
        for (int j = 1; j <= phraseSize; j++) {
            List<ScoredPhrasePairForSentence> scores = tmState.getScoreSortedTranslationsForSpan(i, i + j);
            if (scores != null) {
                for (ScoredPhrasePairForSentence score : scores) {
                    DecoderState newState = buildState(state, score);
                    int beamIndex = newState.getDecodedLength();
                    beamMap.get(beamIndex).setPriority(newState, newState.getPriority(lm, dm));
                }
            }
        }
    }

    protected DecoderState buildState(DecoderState state, ScoredPhrasePairForSentence score) {
        return new StateNoLm(state, score, lm, dm);
    }

    private void extrapolateBeam(PriorityQueue<DecoderState> beam, Map<Integer, PriorityQueue<DecoderState>> beamMap, PhraseTableForSentence tmState) {
        while (beam.hasNext()) {
            DecoderState state = beam.removeFirst();
            extrapolateState(state, beamMap, tmState);
        }
    }

    private void cullBeams(Map<Integer, PriorityQueue<DecoderState>> beamMap) {
        for (Integer i : beamMap.keySet()) {
            PriorityQueue<DecoderState> oldBeam = beamMap.get(i);
            if (oldBeam.size() > maxBeamSize) {
                PriorityQueue<DecoderState> newBeam = new FastPriorityQueue<DecoderState>(maxBeamSize);
                for (int k = 0; k < maxBeamSize; k++) {
                    double priority = oldBeam.getPriority();
                    newBeam.setPriority(oldBeam.removeFirst(), priority);
                }
                beamMap.put(i, newBeam);
            }
        }
    }
}


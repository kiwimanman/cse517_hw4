package edu.berkeley.nlp.assignments.assign2.student;

import edu.berkeley.nlp.langmodel.NgramLanguageModel;
import edu.berkeley.nlp.mt.decoder.Decoder;
import edu.berkeley.nlp.mt.decoder.DistortionModel;
import edu.berkeley.nlp.mt.phrasetable.PhraseTable;
import edu.berkeley.nlp.mt.phrasetable.PhraseTableForSentence;
import edu.berkeley.nlp.mt.phrasetable.ScoredPhrasePairForSentence;
import edu.berkeley.nlp.util.FastPriorityQueue;
import edu.berkeley.nlp.util.PriorityQueue;

import java.util.*;

/**
 * @author Keith Stone
 */

public class MonotonicNoLmDecoder implements Decoder {
    public class StateNoLm extends DecoderState {
        public StateNoLm(List<String> sentence) {
            super(null, null, 0, 0, null, null);
            decodedMask = new boolean[sentence.size()];
        }

        public StateNoLm(DecoderState backPointer, ScoredPhrasePairForSentence score, int i, int j, NgramLanguageModel languageModel, DistortionModel distortionModel) {
            super(backPointer, score, i, j, languageModel, distortionModel);
        }

        protected double scoreWithLanguageModel() {
            return 0.0;
        }

        protected double scoreWithDistortionModel() {
            return 0.0;
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
        beamMap.get(0).setPriority(buildStartState(sentence), 0.0);

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

    protected void extrapolateState(DecoderState state, Map<Integer, PriorityQueue<DecoderState>> beamMap, PhraseTableForSentence tmState) {
        int i = state.getDecodedLength();
        int phraseSize = phraseTable.getMaxPhraseSize();
        for (int j = 0; j < phraseSize; j++) {
            int end = i + j + 1;
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

    protected DecoderState buildState(DecoderState state, ScoredPhrasePairForSentence score, int i, int j) {
        return new StateNoLm(state, score, i, j, lm, dm);
    }

    protected DecoderState buildStartState(List<String> sentence) {
        return new StateNoLm(sentence);
    }

    private void extrapolateBeam(PriorityQueue<DecoderState> beam, Map<Integer, PriorityQueue<DecoderState>> beamMap, PhraseTableForSentence tmState) {
        while (beam.hasNext()) {
            DecoderState state = beam.removeFirst();
            extrapolateState(state, beamMap, tmState);
        }
    }

    private void cullBeams(Map<Integer, PriorityQueue<DecoderState>> beamMap) {
        Set<DecoderState> stateSet = new HashSet<DecoderState>(2000);
        for (Integer i : beamMap.keySet()) {
            PriorityQueue<DecoderState> oldBeam = beamMap.get(i);
            PriorityQueue<DecoderState> newBeam = new FastPriorityQueue<DecoderState>(maxBeamSize);
            stateSet.clear();
            while (!oldBeam.isEmpty() && stateSet.size() < maxBeamSize) {
                double priority = oldBeam.getPriority();
                DecoderState ds = oldBeam.removeFirst();
                if (!stateSet.contains(ds)) {
                    stateSet.add(ds);
                    newBeam.setPriority(ds, priority);
                }
            }
            beamMap.put(i, newBeam);
        }
    }
}


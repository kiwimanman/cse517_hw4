package edu.berkeley.nlp.assignments.assign2.student;

import edu.berkeley.nlp.langmodel.NgramLanguageModel;
import edu.berkeley.nlp.mt.decoder.Decoder;
import edu.berkeley.nlp.mt.decoder.DecoderFactory;
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

public class MonotonicNoLmDecoderFactory implements DecoderFactory {
    public class State {
        ScoredPhrasePairForSentence score;
        int decodedLength;
        State backPointer;
        double priority;

        public State() {
            decodedLength = 0;
        }

        public State(State backPointer, ScoredPhrasePairForSentence score, NgramLanguageModel languageModel, DistortionModel distortionModel) {
            this.score = score;
            this.decodedLength = score.getForeignLength() + backPointer.getDecodedLength();
            this.backPointer = backPointer;
            this.priority = backPointer.getPriority(languageModel, distortionModel) + score.score;
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

        public double getPriority(NgramLanguageModel languageModel, DistortionModel distortionModel) {
            // return Decoder.StaticMethods.scoreHypothesis(decode(), languageModel, distortionModel);
            return priority;
        }

        public int getDecodedLength() {
            return decodedLength;
        }
    }

    public class MonotonicNoLmDecoder implements Decoder {
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
            Map<Integer, PriorityQueue<State>> beamMap = buildBeamMap(sentence.size());
            PhraseTableForSentence tmState = phraseTable.initialize(sentence);
            beamMap.get(0).setPriority(new State(), 0.0);

            for (int i = 0; i < sentence.size(); i++) {
                PriorityQueue<State> beam = beamMap.get(i);
                extrapolateBeam(beam, beamMap, tmState);
                cullBeams(beamMap);
            }

            return beamMap.get(sentence.size()).getFirst().decode();
        }

        private Map<Integer, PriorityQueue<State>> buildBeamMap(int size) {
            Map<Integer, PriorityQueue<State>> beamMap = new HashMap<Integer, PriorityQueue<State>>();
            for (int i = 0; i <= size; i++) {
                beamMap.put(i, new FastPriorityQueue<State>(maxBeamSize));
            }
            return beamMap;
        }

        private void extrapolateState(State state, Map<Integer, PriorityQueue<State>> beamMap, PhraseTableForSentence tmState) {
            int i = state.getDecodedLength();
            int phraseSize = phraseTable.getMaxPhraseSize();
            for (int j = 1; j <= phraseSize; j++) {
                List<ScoredPhrasePairForSentence> scores = tmState.getScoreSortedTranslationsForSpan(i, i + j);
                if (scores != null) {
                    for (ScoredPhrasePairForSentence score : scores) {
                        State newState = new State(state, score, lm, dm);
                        int beamIndex = newState.getDecodedLength();
                        beamMap.get(beamIndex).setPriority(newState, newState.getPriority(lm, dm));
                    }
                }
            }
        }

        private void extrapolateBeam(PriorityQueue<State> beam, Map<Integer, PriorityQueue<State>> beamMap, PhraseTableForSentence tmState) {
            while (beam.hasNext()) {
                State state = beam.removeFirst();
                extrapolateState(state, beamMap, tmState);
            }
        }

        private void cullBeams(Map<Integer, PriorityQueue<State>> beamMap) {
            for (Integer i : beamMap.keySet()) {
                PriorityQueue<State> oldBeam = beamMap.get(i);
                if (oldBeam.size() > maxBeamSize) {
                    PriorityQueue<State> newBeam = new FastPriorityQueue<State>(maxBeamSize);
                    for (int k = 0; k < maxBeamSize; k++) {
                        double priority = oldBeam.getPriority();
                        newBeam.setPriority(oldBeam.removeFirst(), priority);
                    }
                    beamMap.put(i, newBeam);
                }
            }
        }
    }

    public Decoder newDecoder(PhraseTable tm, NgramLanguageModel lm, DistortionModel dm) {
        return new MonotonicNoLmDecoder(tm, lm, dm);
    }
}

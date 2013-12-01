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
        int decodedLength = 0;
        Double priority;

        public StateWithLm(DecoderState backPointer, ScoredPhrasePairForSentence score, NgramLanguageModel languageModel, DistortionModel distortionModel) {
            this.score = score;
            this.decodedLength = score.getForeignLength() + backPointer.getDecodedLength();
            this.backPointer = backPointer;
        }

        public double getPriority(NgramLanguageModel languageModel, DistortionModel distortionModel) {
            if (priority == null) {
                this.priority = score.score + backPointer.getPriority(languageModel, distortionModel);
                List<String> priorNgram = new ArrayList<String>(backPointer.getPriorNgram(languageModel));
                priorNgram.addAll(score.getEnglish());
                for (int i = 0; i < priorNgram.size() - languageModel.getOrder() + 1; i++) {
                    List<String> ngram = priorNgram.subList(i, i + languageModel.getOrder());
                    int[] ngramArray = toArray(ngram);
                    double lmScore = languageModel.getNgramLogProbability(ngramArray, 0, ngramArray.length);
                    priority += lmScore;
                }
            }
            return priority;
        }

        public int getDecodedLength() {
            return decodedLength;
        }


    }

    public MonotonicWithLmDecoder(PhraseTable pt, NgramLanguageModel lm, DistortionModel dm) {
        super(pt, lm, dm);
    }

    protected DecoderState buildState(DecoderState state, ScoredPhrasePairForSentence score) {
        return new StateWithLm(state, score, lm, dm);
    }
}

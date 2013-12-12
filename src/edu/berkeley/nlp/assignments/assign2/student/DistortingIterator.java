package edu.berkeley.nlp.assignments.assign2.student;

import edu.berkeley.nlp.util.Pair;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * @author Keith Stone
 */
public class DistortingIterator implements Iterator<Pair<Integer, Integer>> {
    DecoderState state;
    int maxI;
    int maxJFirstTime;
    int maxJ;
    Pair<Integer, Integer> pair;
    Boolean hasNext;

    public DistortingIterator(DecoderState state) {
        this.state = state;

        int translatedLength = state.getUnbrokenTranslationLength();
        int decodingEndPoint = state.getForeignSideEndIndex();

        int distortionPoint = Math.min(decodingEndPoint, translatedLength);

        int phraseSize = state.getPhraseTable().getMaxPhraseSize();
        int distortionLimit = state.getDistortionModel().getDistortionLimit();
        maxJ = distortionPoint + Math.min(phraseSize, distortionLimit);
        maxI = maxJ - 1;
        maxJFirstTime = translatedLength + phraseSize;

        pair = new Pair<Integer, Integer>(distortionPoint, distortionPoint);
    }

    public boolean hasNext() {
        if (hasNext == null) {
            // Hella not thread safe no more...
            hasNext = true;
            // Ensure a legal phrase before marking as complete by leaving the function
            try {
                do  {
                    findNextState();
                } while (!state.isLegalPhrase(pair.getFirst(), pair.getSecond()));
            } catch (RuntimeException e) {
                hasNext = false;
                pair = null;
            }
        }
        return hasNext;
    }

    public Pair next() {
        if (pair == null) throw new NoSuchElementException();
        hasNext = null;
        return pair;
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

    private void findNextState() {
        // Special logic for expanding from the stem.
        if (pair.getFirst() == state.getDecodedLength()) {
            if (pair.getSecond() >= maxJFirstTime) {
                incrementI();
            } else {
                incrementJ();
            }
        } else {
            if (pair.getSecond() >= maxJ) {
                incrementI();
            } else {
                incrementJ();
            }
        }

        // Stop condition
        if (pair.getFirst() > maxI) {
            // Marks the done searching condition
            pair = null;
            throw new RuntimeException("Jump to exit logic, god I feel dirty...");
        }
    }

    private void incrementI() {
        pair.setFirst(pair.getFirst() + 1);
        pair.setSecond(pair.getFirst() + 1);
    }

    private void incrementJ() {
        pair.setSecond(pair.getSecond() + 1);
    }
}

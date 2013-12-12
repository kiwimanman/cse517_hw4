package edu.berkeley.nlp.assignments.assign2.student;

import edu.berkeley.nlp.util.Pair;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * @author Keith Stone
 */
public class MonotonicIterator implements Iterator<Pair<Integer, Integer>> {
    int i;
    int j;
    int length;
    Pair<Integer, Integer> pair;

    public MonotonicIterator(int i, int length) {
        this.i = i;
        this.j = 0;
        this.length = length;
        pair = new Pair<Integer, Integer>(i, i);
    }

    public boolean hasNext() {
        return (j < length);
    }

    public Pair next() {
        if (!hasNext()) throw new NoSuchElementException();
        j++;
        pair.setSecond(pair.getSecond() + 1);
        return pair;
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }
}

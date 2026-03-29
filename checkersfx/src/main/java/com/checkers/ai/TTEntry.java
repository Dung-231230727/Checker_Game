package com.checkers.ai;

/**
 * Entry for the Transposition Table.
 */
public class TTEntry {
    public static final int EXACT = 0;
    public static final int LOWER_BOUND = 1;
    public static final int UPPER_BOUND = 2;

    public long key;
    public int score;
    public int depth;
    public int type;

    public TTEntry(long key, int score, int depth, int type) {
        this.key = key;
        this.score = score;
        this.depth = depth;
        this.type = type;
    }
}

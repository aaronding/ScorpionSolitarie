package com.family.solitaire.solver;

import static com.family.solitaire.model.CardConstants.CARDS_IN_SUIT;
import static com.family.solitaire.model.CardConstants.NCARDS;
import static com.family.solitaire.model.CardConstants.NCOLS;
import static com.family.solitaire.model.CardConstants.RESERVE_SIZE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.family.solitaire.model.Board;
import com.family.solitaire.model.Card;
import com.family.solitaire.model.Column;
import com.family.solitaire.model.Move;
import com.family.solitaire.model.Rule;

/**
 * Finds a way to win from a position, knowing where every card is, face down
 * or not.
 *
 * <p>A best-first search: it always continues from the most promising position
 * seen so far (fewest cards face down, most cards already on their same-suit
 * neighbour), never visits a position twice, and gives up after a set number
 * of positions. Columns the rules treat alike count as the same position.
 *
 * <p>The only move it never tries is taking a card off its same-suit
 * neighbour (say 5&#9829; off 6&#9829;). That move is never needed: whatever
 * card would use the uncovered 6&#9829; can go where the 5&#9829; would have
 * gone instead. So a search that ends before the limit proves the position
 * cannot be won.
 *
 * @author Aaron Ding
 */
public final class Solver {

    /** Stands for dealing the reserve in a solution. */
    public static final Move DEAL_RESERVE = new Move(-1, -1, -1);

    public enum Outcome { SOLVED, UNSOLVABLE, GAVE_UP }

    public static final class Result {
        Result(Outcome outcome, List<Move> solution, int positions) {
            this.outcome = outcome;
            this.solution = solution;
            this.positions = positions;
        }
        public final Outcome outcome;
        /** The moves to win, in order; empty unless solved. */
        public final List<Move> solution;
        /** Positions examined. */
        public final int positions;
    }

    public Solver(Rule rule, int positionLimit) {
        this.limit = positionLimit;
        for (int card = 0; card < NCARDS; card++) {
            List<Integer> list = new ArrayList<Integer>();
            for (int other = 0; other < NCARDS; other++) {
                if (rule.canStack(Card.valueOf(other), Card.valueOf(card)))
                    list.add(other);
            }
            followers[card] = new int[list.size()];
            for (int i = 0; i < list.size(); i++)
                followers[card][i] = list.get(i);
        }
    }

    /** Stops a search running on another thread; it then reports GAVE_UP. */
    public void cancel() {
        cancelled = true;
    }

    public Result solve(Board board) {
        load(board);
        states = new byte[1024][];
        parent = new int[1024];
        via = new int[1024];
        depth = new int[1024];
        nodes = 0;
        heapSize = 0;
        visited = new long[Integer.highestOneBit(Math.max(1024, limit) * 4)];

        if (isWon())
            return new Result(Outcome.SOLVED, Collections.<Move>emptyList(), 0);
        markVisited(hash());
        push(-1, 0);

        while (heapSize > 0) {
            if (nodes >= limit || cancelled)
                return new Result(Outcome.GAVE_UP, Collections.<Move>emptyList(), nodes);
            int node = pop();
            decode(states[node]);
            int n = generate();
            for (int k = 0; k < n; k++) {
                int m = moves[k];
                int carried = 0;
                boolean flipped = false;
                if (m == DEAL) {
                    dealReserve();
                } else {
                    carried = len[from(m)] - row(m);
                    flipped = apply(from(m), row(m), to(m));
                }
                if (isWon())
                    return new Result(Outcome.SOLVED, pathTo(node, m), nodes);
                if (markVisited(hash()))
                    push(node, m);
                if (m == DEAL)
                    undealReserve();
                else
                    unapply(from(m), to(m), carried, flipped);
            }
        }
        return new Result(cancelled ? Outcome.GAVE_UP : Outcome.UNSOLVABLE,
                          Collections.<Move>emptyList(), nodes);
    }

    private List<Move> pathTo(int node, int last) {
        List<Move> path = new ArrayList<Move>();
        path.add(toMove(last));
        for (int i = node; parent[i] >= 0; i = parent[i])
            path.add(toMove(via[i]));
        Collections.reverse(path);
        return path;
    }

    // ---- moves -------------------------------------------------------------

    private static final int DEAL = -1;

    private static int move(int from, int row, int to) { return (from << 12) | (row << 6) | to; }
    private static int from(int m) { return m >> 12; }
    private static int row(int m) { return (m >> 6) & 63; }
    private static int to(int m) { return m & 7; }

    private static Move toMove(int m) {
        return m == DEAL ? DEAL_RESERVE : new Move(from(m), row(m), to(m));
    }

    private final int[] moves = new int[NCOLS * 8 + 1];

    /** Every legal move from the current position; returns how many. */
    private int generate() {
        int n = 0;
        boolean sawSymmetricEmpty = false;
        for (int to = 0; to < NCOLS; to++) {
            if (len[to] == 0) {
                // Empty columns the rules treat alike are one target.
                if (reserveUsed || to >= RESERVE_SIZE) {
                    if (sawSymmetricEmpty)
                        continue;
                    sawSymmetricEmpty = true;
                }
                for (int suit = 0; suit < 4; suit++) {
                    int loc = where[suit * CARDS_IN_SUIT + CARDS_IN_SUIT - 1];
                    int from = loc >> 6, row = loc & 63;
                    if (loc < 0 || from == to)
                        continue;
                    // A King already at the top gains nothing by moving, unless
                    // the move changes which columns the reserve will land on.
                    if (row == 0 && (reserveUsed || (from >= RESERVE_SIZE && to >= RESERVE_SIZE)))
                        continue;
                    moves[n++] = move(from, row, to);
                }
            } else {
                for (int card : followers[cols[to][len[to] - 1]]) {
                    int loc = where[card];
                    if (loc < 0 || loc >> 6 == to)
                        continue;
                    int from = loc >> 6, row = loc & 63;
                    if (row > 0 && cols[from][row - 1] - card == 1
                            && card % CARDS_IN_SUIT != CARDS_IN_SUIT - 1)
                        continue;   // already on its same-suit neighbour; see class comment
                    moves[n++] = move(from, row, to);
                }
            }
        }
        if (!reserveUsed)
            moves[n++] = DEAL;
        return n;
    }

    // ---- position ----------------------------------------------------------

    private static final int DOWN = 64, CARD = 63;

    private final byte[][] cols = new byte[NCOLS][NCARDS + 1];
    private final int[] len = new int[NCOLS];
    private final int[] where = new int[NCARDS];   // column*64 + row of face-up cards, else -1
    private final int[] reserve = new int[RESERVE_SIZE];
    private boolean reserveUsed;

    private void load(Board board) {
        for (int c = 0; c < NCOLS; c++) {
            Column column = board.getColumn(c);
            len[c] = column.getSize();
            for (int r = 0; r < len[c]; r++) {
                Card card = column.getCard(r);
                cols[c][r] = (byte)(card.value() | (card.isFacedDown() ? DOWN : 0));
            }
        }
        Card[] res = board.getReserve().getCards();
        reserveUsed = board.getReserve().used();
        for (int i = 0; i < RESERVE_SIZE; i++)
            reserve[i] = res[i] == null ? -1 : res[i].value();
        indexCards();
    }

    private void indexCards() {
        Arrays.fill(where, -1);
        for (int c = 0; c < NCOLS; c++)
            for (int r = 0; r < len[c]; r++)
                if ((cols[c][r] & DOWN) == 0)
                    where[cols[c][r]] = c * 64 + r;
    }

    private boolean apply(int from, int row, int to) {
        for (int r = row; r < len[from]; r++) {
            int card = cols[from][r];
            cols[to][len[to]] = (byte)card;
            where[card] = to * 64 + len[to];
            len[to]++;
        }
        len[from] = row;
        if (row > 0 && (cols[from][row - 1] & DOWN) != 0) {
            int card = cols[from][row - 1] & CARD;
            cols[from][row - 1] = (byte)card;
            where[card] = from * 64 + row - 1;
            return true;
        }
        return false;
    }

    private void unapply(int from, int to, int count, boolean flipped) {
        if (flipped) {
            int card = cols[from][len[from] - 1];
            cols[from][len[from] - 1] = (byte)(card | DOWN);
            where[card] = -1;
        }
        for (int k = len[to] - count; k < len[to]; k++) {
            int card = cols[to][k];
            cols[from][len[from]] = (byte)card;
            where[card] = from * 64 + len[from];
            len[from]++;
        }
        len[to] -= count;
    }

    private void dealReserve() {
        for (int i = 0; i < RESERVE_SIZE; i++) {
            cols[i][len[i]] = (byte)reserve[i];
            where[reserve[i]] = i * 64 + len[i];
            len[i]++;
        }
        reserveUsed = true;
    }

    private void undealReserve() {
        for (int i = 0; i < RESERVE_SIZE; i++) {
            len[i]--;
            where[reserve[i]] = -1;
        }
        reserveUsed = false;
    }

    private boolean isWon() {
        if (!reserveUsed)
            return false;
        for (int c = 0; c < NCOLS; c++) {
            if (len[c] == 0)
                continue;
            if (len[c] != CARDS_IN_SUIT || cols[c][0] != topOfSuit(cols[c][0]))
                return false;
            for (int r = 1; r < CARDS_IN_SUIT; r++)
                if (cols[c][r - 1] - cols[c][r] != 1)
                    return false;
        }
        return true;
    }

    private static int topOfSuit(int card) {
        return (card / CARDS_IN_SUIT) * CARDS_IN_SUIT + CARDS_IN_SUIT - 1;
    }

    /**
     * How far from won, lower is better: face-down cards and the reserve count
     * most, then every card not yet resting on its same-suit neighbour.
     */
    private int distance() {
        int d = reserveUsed ? 0 : 3 * 4;
        for (int c = 0; c < NCOLS; c++) {
            for (int r = 0; r < len[c]; r++) {
                int card = cols[c][r];
                if ((card & DOWN) != 0) {
                    d += 4;
                } else if (r == 0) {
                    if (card != topOfSuit(card))
                        d += 1;
                } else if (cols[c][r - 1] - card != 1 || card % CARDS_IN_SUIT == CARDS_IN_SUIT - 1) {
                    d += 1;
                }
            }
        }
        return d;
    }

    // ---- positions: storage, queue and seen set -----------------------------

    private byte[][] states;
    private int[] parent, via, depth;
    private int nodes;

    private void push(int from, int m) {
        if (nodes == states.length) {
            int n = nodes * 2;
            states = Arrays.copyOf(states, n);
            parent = Arrays.copyOf(parent, n);
            via = Arrays.copyOf(via, n);
            depth = Arrays.copyOf(depth, n);
        }
        int node = nodes++;
        states[node] = encode();
        parent[node] = from;
        via[node] = m;
        depth[node] = from < 0 ? 0 : depth[from] + 1;
        // Mostly greedy; the small weight on depth keeps solutions short.
        long priority = distance() * 8L + depth[node];
        heapAdd((priority << 32) | node);
    }

    private byte[] encode() {
        int size = 1 + NCOLS;
        for (int c = 0; c < NCOLS; c++)
            size += len[c];
        byte[] s = new byte[size];
        int i = 0;
        s[i++] = (byte)(reserveUsed ? 1 : 0);
        for (int c = 0; c < NCOLS; c++) {
            s[i++] = (byte)len[c];
            System.arraycopy(cols[c], 0, s, i, len[c]);
            i += len[c];
        }
        return s;
    }

    private void decode(byte[] s) {
        int i = 0;
        reserveUsed = s[i++] == 1;
        for (int c = 0; c < NCOLS; c++) {
            len[c] = s[i++];
            System.arraycopy(s, i, cols[c], 0, len[c]);
            i += len[c];
        }
        indexCards();
    }

    private long[] heap = new long[1024];
    private int heapSize;

    private void heapAdd(long v) {
        if (heapSize == heap.length)
            heap = Arrays.copyOf(heap, heapSize * 2);
        int i = heapSize++;
        while (i > 0) {
            int p = (i - 1) / 2;
            if (heap[p] <= v)
                break;
            heap[i] = heap[p];
            i = p;
        }
        heap[i] = v;
    }

    private int pop() {
        long top = heap[0];
        long last = heap[--heapSize];
        int i = 0;
        while (true) {
            int child = 2 * i + 1;
            if (child >= heapSize)
                break;
            if (child + 1 < heapSize && heap[child + 1] < heap[child])
                child++;
            if (heap[child] >= last)
                break;
            heap[i] = heap[child];
            i = child;
        }
        heap[i] = last;
        return (int)top;
    }

    private static final long[][] ZOBRIST = new long[NCARDS + 1][128];
    static {
        Random random = new Random(0x5c0e910L);
        for (long[] row : ZOBRIST)
            for (int i = 0; i < row.length; i++)
                row[i] = random.nextLong();
    }

    private final long[] columnHashes = new long[NCOLS];
    private long[] visited;

    private long hash() {
        for (int c = 0; c < NCOLS; c++) {
            long h = 0x9E3779B97F4A7C15L * (len[c] + 1);
            for (int r = 0; r < len[c]; r++)
                h ^= ZOBRIST[r][cols[c][r] & 127];
            columnHashes[c] = h;
        }
        // Once the reserve is gone the columns are interchangeable; before
        // that only the last four are, since the reserve lands on the first three.
        Arrays.sort(columnHashes, reserveUsed ? 0 : RESERVE_SIZE, NCOLS);
        long h = reserveUsed ? 1 : 2;
        for (long ch : columnHashes)
            h = mix(h * 31 + ch);
        return h == 0 ? 1 : h;
    }

    private static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** Adds h to the seen set; false if it was already there. */
    private boolean markVisited(long h) {
        int mask = visited.length - 1;
        int i = (int)h & mask;
        while (visited[i] != 0) {
            if (visited[i] == h)
                return false;
            i = (i + 1) & mask;
        }
        visited[i] = h;
        return true;
    }

    private final int limit;
    private final int[][] followers = new int[NCARDS][];
    private volatile boolean cancelled;
}

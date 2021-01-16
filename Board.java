/* Skeleton Copyright (C) 2015, 2020 Paul N. Hilfinger and the Regents of the
 * University of California.  All rights reserved. */
package loa;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;

import java.util.regex.Pattern;

import static loa.Piece.*;
import static loa.Square.*;

/** Represents the state of a game of Lines of Action.
 *  @author Nazli Urenli
 */
class Board {

    /**
     * Default number of moves for each side that results in a draw.
     */
    static final int DEFAULT_MOVE_LIMIT = 60;

    /**
     * Pattern describing a valid square designator (cr).
     */
    static final Pattern ROW_COL = Pattern.compile("^[a-h][1-8]$");
    /**
     * The standard initial configuration for Lines of Action (bottom row
     * first).
     */
    static final Piece[][] INITIAL_PIECES = {
            {EMP, BP, BP, BP, BP, BP, BP, EMP},
            {WP, EMP, EMP, EMP, EMP, EMP, EMP, WP},
            {WP, EMP, EMP, EMP, EMP, EMP, EMP, WP},
            {WP, EMP, EMP, EMP, EMP, EMP, EMP, WP},
            {WP, EMP, EMP, EMP, EMP, EMP, EMP, WP},
            {WP, EMP, EMP, EMP, EMP, EMP, EMP, WP},
            {WP, EMP, EMP, EMP, EMP, EMP, EMP, WP},
            {EMP, BP, BP, BP, BP, BP, BP, EMP}
    };
    /**
     * Current contents of the board.  Square S is at _board[S.index()].
     */
    private final Piece[] _board = new Piece[BOARD_SIZE * BOARD_SIZE];
    /**
     * List of all unretracted moves on this board, in order.
     */
    private final ArrayList<Move> _moves = new ArrayList<>();
    /**
     * List of the sizes of continguous clusters of pieces, by color.
     */
    private final ArrayList<Integer>
            _whiteRegionSizes = new ArrayList<>(),
            _blackRegionSizes = new ArrayList<>();
    /**
     * Current side on move.
     */
    private Piece _turn;
    /**
     * Limit on number of moves before tie is declared.
     */
    private int _moveLimit;
    /**
     * True iff the value of _winner is known to be valid.
     */
    private boolean _winnerKnown;
    /**
     * Cached value of the winner (BP, WP, EMP (for tie), or null (game still
     * in progress).  Use only if _winnerKnown.
     */
    private Piece _winner;
    /**
     * True iff subsets computation is up-to-date.
     */
    private boolean _subsetsInitialized;

    /** An array list that stores previous pieces. */
    private final ArrayList<Piece> _prevPieces = new ArrayList<>();
    /**
     * A Board whose initial contents are taken from INITIALCONTENTS
     * and in which the player playing TURN is to move. The resulting
     * Board has
     * get(col, row) == INITIALCONTENTS[row][col]
     * Assumes that PLAYER is not null and INITIALCONTENTS is 8x8.
     * <p>
     * CAUTION: The natural written notation for arrays initializers puts
     * the BOTTOM row of INITIALCONTENTS at the top.
     */
    Board(Piece[][] initialContents, Piece turn) {
        initialize(initialContents, turn);
    }

    /**
     * A new board in the standard initial position.
     */
    Board() {
        this(INITIAL_PIECES, BP);
    }

    /**
     * A Board whose initial contents and state are copied from
     * BOARD.
     */
    Board(Board board) {
        this();
        copyFrom(board);
    }

    /**
     * Set my state to CONTENTS with SIDE to move.
     */
    void initialize(Piece[][] contents, Piece side) {
        _moves.clear();
        for (int myRows = 0; myRows < contents.length; myRows += 1) {
            for (int myColumns = 0;
                 myColumns < contents[myRows].length; myColumns += 1) {
                _board[sq(myColumns, myRows).index()]
                        = contents[myRows][myColumns];
            }
        }
        _turn = side;
        _moveLimit = DEFAULT_MOVE_LIMIT;
    }

    /**
     * Set me to the initial configuration.
     */
    void clear() {
        initialize(INITIAL_PIECES, BP);
    }

    /**
     * Set my state to a copy of BOARD.
     */
    void copyFrom(Board board) {
        if (board == this) {
            return;
        }

        for (int i = 0; i < board._board.length; i++) {
            this._board[i] = board._board[i];
        }
        this._moves.clear();
        this._moves.addAll(board._moves);

        this._prevPieces.clear();
        this._prevPieces.addAll(board._prevPieces);

        this._whiteRegionSizes.clear();
        this._whiteRegionSizes.addAll(board._whiteRegionSizes);

        this._blackRegionSizes.clear();
        this._blackRegionSizes.addAll(board._blackRegionSizes);

        this._turn = board._turn;

        this._moveLimit = board._moveLimit;

        this._winnerKnown = board._winnerKnown;

        this._winner = board._winner;
    }

    /**
     * Return the contents of the square at SQ.
     */
    Piece get(Square sq) {
        return _board[sq.index()];
    }

    /**
     * Set the square at SQ to V and set the side that is to move next
     * to NEXT, if NEXT is not null.
     */
    void set(Square sq, Piece v, Piece next) {
        _board[sq.index()] = v;
        if (next != null) {
            _turn = next;
        }
    }

    /**
     * Set the square at SQ to V, without modifying the side that
     * moves next.
     */
    void set(Square sq, Piece v) {
        set(sq, v, null);
    }

    /**
     * Set limit on number of moves by each side that results in a tie to
     * LIMIT, where 2 * LIMIT > movesMade().
     */
    void setMoveLimit(int limit) {
        if (2 * limit <= movesMade()) {
            throw new IllegalArgumentException("move limit too small");
        }
        _moveLimit = 2 * limit;
    }

    /**
     * Assuming isLegal(MOVE), make MOVE. Assumes MOVE.isCapture()
     * is false.
     */
    void makeMove(Move move) {
        assert isLegal(move);
        assert !move.isCapture();

        _moves.add(move);
        _prevPieces.add(_board[move.getTo().index()]);

        _board[move.getFrom().index()] = EMP;
        _board[move.getTo().index()] = _turn;
        _turn = _turn.opposite();
        _subsetsInitialized = false;
    }

    /**
     * Retract (unmake) one move, returning to the state immediately before
     * that move.  Requires that movesMade () > 0.
     */
    void retract() {
        int size = _moves.size();
        if (size == 0) {
            return;
        }
        Move lastMove = _moves.remove(size - 1);
        Piece capturedPiece = _prevPieces.remove(size - 1);

        _board[lastMove.getTo().index()] = capturedPiece;
        _board[lastMove.getFrom().index()] = _turn.opposite();
        _turn = _turn.opposite();
        _winner  = null;
        _winnerKnown = false;
    }

    /**
     * Return the Piece representing who is next to move.
     */
    Piece turn() {
        return _turn;
    }

    /**
     * Return true iff FROM - TO is a legal move for the player currently on
     * move.
     */
    boolean isLegal(Square from, Square to) {
        if (!from.isValidMove(to)) {
            return false;
        }
        if (blocked(from, to)) {
            return false;
        }
        int numPiece = findStepCountTo(from, to);
        int distance = from.distance(to);
        if (numPiece != distance) {
            return false;
        }
        return true;
    }
    /** Returns the steps from FROM, to TO. */
    protected int findStepCountTo(Square from, Square to) {
        int dir = from.direction(to);
        int n1 = findStepCountDirection(from, dir);
        dir = to.direction(from);
        int n2 = findStepCountDirection(from, dir);
        return n1 + n2 + 1;
    }
    /** Returns the steps from FROM, using the direction DIR. */
    protected int findStepCountDirection(Square from, int dir) {
        int numPiece = 0;

        Square next = from;
        while (true) {
            next = next.moveDest(dir, 1);
            if (next == null) {
                break;
            }
            if (_board[next.index()] != EMP) {
                numPiece++;
            }
        }
        return numPiece;
    }

    /**
     * Return true iff MOVE is legal for the player currently on move.
     * The isCapture() property is ignored.
     */
    boolean isLegal(Move move) {
        return isLegal(move.getFrom(), move.getTo());
    }

    /**
     * Return a sequence of all legal moves from this position.
     */
    List<Move> legalMoves() {
        Piece movTurn = _turn;
        ArrayList<Move> legalMoves = new ArrayList<>();
        for (int col = 0; col < BOARD_SIZE; col += 1) {
            for (int row = 0; row < BOARD_SIZE; row += 1) {
                Square fromSq = sq(col, row);
                if (get(fromSq) != movTurn) {
                    continue;
                }
                for (int check1 = 0; check1 < BOARD_SIZE; check1 += 1) {
                    for (int check2 = 0; check2 < BOARD_SIZE; check2 += 1) {
                        Square toSq = sq(check2, check1);
                        if (isLegal(fromSq, toSq)) {
                            Move theseMoves = Move.mv(fromSq, toSq);
                            legalMoves.add(theseMoves);
                        }
                    }
                }
            }
        }
        return legalMoves;
    }

    /**
     * Return true iff the game is over (either player has all his
     * pieces continguous or there is a tie).
     */
    boolean gameOver() {
        return winner() != null;
    }

    /**
     * Return true iff SIDE's pieces are continguous.
     */
    boolean piecesContiguous(Piece side) {
        return getRegionSizes(side).size() == 1;
    }

    /**
     * Return the winning side, if any.  If the game is not over, result is
     * null.  If the game has ended in a tie, returns EMP.
     */
    Piece winner() {
        if (!_winnerKnown) {
            boolean bpCont = piecesContiguous(BP);
            boolean wpCont = piecesContiguous(WP);
            if (bpCont && wpCont) {
                _winner = _turn.opposite();
            } else {
                if (bpCont) {
                    _winner = BP;
                } else if (wpCont) {
                    _winner = WP;
                }
            }
            if (_winner == null &&  movesMade() == _moveLimit) {
                _winner = EMP;
            }

            _winnerKnown = (_winner != null);
        }
        return _winner;
    }

    /**
     * Return the total number of moves that have been made (and not
     * retracted).  Each valid call to makeMove with a normal move increases
     * this number by 1.
     */
    int movesMade() {
        return _moves.size();
    }

    @Override
    public boolean equals(Object obj) {
        Board b = (Board) obj;
        return Arrays.deepEquals(_board, b._board) && _turn == b._turn;
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(_board) * 2 + _turn.hashCode();
    }

    @Override
    public String toString() {
        Formatter out = new Formatter();
        out.format("===%n");
        for (int r = BOARD_SIZE - 1; r >= 0; r -= 1) {
            out.format("    ");
            for (int c = 0; c < BOARD_SIZE; c += 1) {
                out.format("%s ", get(sq(c, r)).abbrev());
            }
            out.format("%n");
        }
        out.format("Next move: %s%n===", turn().fullName());
        return out.toString();
    }

    /**
     * Return true if a move from FROM to TO is blocked by an opposing
     * piece or by a friendly piece on the target square.
     */
    private boolean blocked(Square from, Square to) {
        Move move = Move.mv(from, to);
        int distance = from.distance(to);
        int dir = from.direction(to);

        Square next = from;
        for (int i = 0; i < distance - 1; i++) {
            next = next.moveDest(dir, 1);
            if (_board[next.index()] == EMP) {
                continue;
            }
            if (_board[next.index()] != _turn) {
                return true;
            }
        }

        next = from.moveDest(dir, distance);
        if (next != null
                && _board[next.index()] == _turn) {
            return true;
        }

        return false;
    }

    /**
     * Return the size of the as-yet unvisited cluster of squares
     * containing P at and adjacent to SQ.  VISITED indicates squares that
     * have already been processed or are in different clusters.  Update
     * VISITED to reflect squares counted.
     */
    private int numContig(Square sq, boolean[][] visited, Piece p) {
        if (visited[sq.row()] [sq.col()]) {
            return 0;
        }
        visited[sq.row()] [sq.col()] = true;
        if (_board[sq.index()] == EMP) {
            return 0;
        }
        if (_board[sq.index()] != p) {
            return 0;
        }
        int num = 0;
        Square[] adjArr = sq.adjacent();
        for (Square adj : adjArr) {
            num += numContig(adj, visited, p);
        }
        return num + 1;
    }


    /**
     * Set the values of _whiteRegionSizes and _blackRegionSizes.
     */
    private void computeRegions() {
        if (_subsetsInitialized) {
            return;
        }
        _whiteRegionSizes.clear();
        _blackRegionSizes.clear();
        fillRegions(_whiteRegionSizes, WP);
        fillRegions(_blackRegionSizes, BP);

        Collections.sort(_whiteRegionSizes, Collections.reverseOrder());
        Collections.sort(_blackRegionSizes, Collections.reverseOrder());
        _subsetsInitialized = true;
    }

    /**
     * Return the sizes of all the regions in the current union-find
     * structure for side S.
     */
    List<Integer> getRegionSizes(Piece s) {
        computeRegions();
        if (s == WP) {
            return _whiteRegionSizes;
        } else {
            return _blackRegionSizes;
        }
    }
    /** Returns an Arraylist of LIST and PIECE P,
     * that determine the visited squares. */
    private void fillRegions(ArrayList<Integer> list, Piece p) {
        boolean[][] visited = new boolean[BOARD_SIZE][BOARD_SIZE];
        boolean[][] visitedFound = new boolean[BOARD_SIZE][BOARD_SIZE];
        for (int col = 0; col < BOARD_SIZE; col++) {
            for (int row = 0; row < BOARD_SIZE; row++) {
                Square sq = sq(col, row);
                int num = numContig(sq, visited,  p);
                if (num != 0) {
                    visitedFound[col][row] = true;
                    list.add(num);
                }
                System.arraycopy(visitedFound, 0, visited,
                        0, visitedFound.length);
            }
        }
    }
}











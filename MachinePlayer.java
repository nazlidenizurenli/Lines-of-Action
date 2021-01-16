/* Skeleton Copyright (C) 2015, 2020 Paul N. Hilfinger and the Regents of the
 * University of California.  All rights reserved. */
package loa;

import static loa.Piece.*;

/** An automated Player.
 *  @author Nazli Urenli
 */
class MachinePlayer extends Player {

    /** Set a maximum score. */
    private static final int MAX_SCORE = 10000;

    /** Set a minimum score. */
    private static final int MIN_SCORE = -10000;

    /**
     * A position-score magnitude indicating a win (for white if positive,
     * black if negative).
     */
    private static final int WINNING_VALUE = Integer.MAX_VALUE - 20;
    /**
     * A magnitude greater than a normal value.
     */
    private static final int INFTY = Integer.MAX_VALUE;
    /**
     * Used to convey moves discovered by findMove.
     */
    private Move _foundMove;

    /**
     * A new MachinePlayer with no piece or controller (intended to produce
     * a template).
     */
    MachinePlayer() {
        this(null, null);
    }

    /**
     * A MachinePlayer that plays the SIDE pieces in GAME.
     */
    MachinePlayer(Piece side, Game game) {
        super(side, game);
    }

    @Override
    String getMove() {
        Move choice;

        assert side() == getGame().getBoard().turn();
        int depth;
        choice = searchForMove();
        getGame().reportMove(choice);
        return choice.toString();
    }

    @Override
    Player create(Piece piece, Game game) {
        return new MachinePlayer(piece, game);
    }

    @Override
    boolean isManual() {
        return false;
    }

    /**
     * Return a move after searching the game tree to DEPTH>0 moves
     * from the current position. Assumes the game is not over.
     */
    private Move searchForMove() {
        Board work = new Board(getBoard());
        int value;
        assert side() == work.turn();
        _foundMove = null;
        if (side() == WP) {
            value = findMove(work, chooseDepth(),
                    true, 1, -INFTY, INFTY);
        } else {
            value = findMove(work, chooseDepth(),
                    true, -1, -INFTY, INFTY);
        }
        return _foundMove;
    }

    /**
     * Find a move from position BOARD and return its value, recording
     * the move found in _foundMove iff SAVEMOVE. The move
     * should have maximal value or have value > BETA if SENSE==1,
     * and minimal value or value < ALPHA if SENSE==-1. Searches up to
     * DEPTH levels.  Searching at level 0 simply returns a static estimate
     * of the board value and does not set _foundMove. If the game is over
     * on BOARD, does not set _foundMove.
     */
    private int findMove(Board board, int depth, boolean saveMove,
                         int sense, int alpha, int beta) {
        Piece side = side();
        Move move = null;
        move = findBestMove(board, depth, saveMove, sense, alpha, beta);

        if (saveMove) {
            _foundMove = move;
        }
        return 0;
    }

    /** Return the bestMove.
     *
     * @param board in BOARD
     * @param depth I select
     * @param saveMove true or false
     * @param sense my integer
     * @param alpha value1
     * @param beta value2
     * @return bestMove
     */
    protected Move findBestMove(Board board, int depth, boolean saveMove,
                                int sense, int alpha, int beta) {
        final int boardSize = 8;
        int bestVal = Integer.MIN_VALUE;
        Piece turn = board.turn();
        Move bestMove = null;
        for (int col = 0; col < 8; col++) {
            for (int row = 0; row < 8; row++) {
                Square sq = Square.sq(col, row);
                if (board.get(sq) != turn) {
                    continue;
                }
                for (int dir = 0; dir < 8; dir++) {
                    Move move = findMove(board, sq, dir);
                    if (move == null) {
                        continue;
                    }
                    board.makeMove(move);
                    int moveVal = minimax(board, depth,
                            sense == 1 ? 0 : 1, alpha, beta);
                    board.retract();
                    if (moveVal > bestVal) {
                        bestMove = move;
                        bestVal = moveVal;
                    }
                }
            }
        }
        return bestMove;
    }

    /** Return the move in BOARD from FROM with direction DIR. */
    protected Move findMove(Board board, Square from, int dir) {
        Square adj = from.moveDest(dir, 1);
        if (adj == null) {
            return null;
        }
        int stepCount = board.findStepCountTo(from, adj);
        Square dest = from.moveDest(dir, stepCount);
        if (dest == null) {
            return null;
        }
        if (!board.isLegal(from, dest)) {
            return null;
        }
        boolean capture = board.get(dest) != null;
        capture = false;
        Move move = Move.mv(from, dest, capture);
        return move;
    }
    /**
     * Find a move from position BOARD and return its value, recording
     * the move found in _foundMove iff SAVEMOVE. The move
     * should have maximal value or have value > BETA if SENSE==1,
     * and minimal value or value < ALPHA if SENSE==-1. Searches up to
     * DEPTH levels.  Searching at level 0 simply returns a static estimate
     * of the board value and does not set _foundMove. If the game is over
     * on BOARD, does not set _foundMove.
     */
    protected int minimax(Board board, int depth,
                          int sense, int alpha, int beta) {
        if (depth == 0 || board.gameOver()) {
            int score = evaluate(board, depth);
            return score;
        }
        Piece turn = board.turn();
        int maxOrMinEval = sense == 1
                ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        for (int col = 0; col < 8; col++) {
            for (int row = 0; row < 8; row++) {
                Square sq = Square.sq(col, row);
                if (board.get(sq) != turn) {
                    continue;
                }
                for (int dir = 0; dir < 8; dir++) {
                    Move move = findMove(board, sq, dir);
                    if (move == null) {
                        continue;
                    }
                    board.makeMove(move);
                    try {
                        if (sense == 1) {
                            int eval = minimax(board, depth - 1,
                                    0, alpha, beta);
                            maxOrMinEval = Math.max(eval, maxOrMinEval);
                            alpha = Math.max(eval, alpha);
                            if (beta <= alpha) {
                                break;
                            }
                        } else {
                            int eval = minimax(board, depth - 1,
                                    1, alpha, beta);
                            maxOrMinEval = Math.min(eval, maxOrMinEval);
                            beta = Math.min(eval, beta);
                            if (beta <= alpha) {
                                break;
                            }
                        }
                    } finally {
                        board.retract();
                    }

                }
            }
        }
        return maxOrMinEval;
    }

    /** Return the evaluation of BOARD with DEPTH. */
    protected int evaluate(Board board, int depth) {
        int score = 0;
        if (board.gameOver()) {
            Piece winner = board.winner();
            if (winner == Piece.EMP) {
                return (MAX_SCORE * (depth + 1)) - 1;
            }
            return MAX_SCORE * (depth + 1);
        }
        Piece turn = board.turn();
        int turnSize = board.getRegionSizes(turn).size();
        int oppositeSize = board.getRegionSizes(turn.opposite()).size();
        score +=  turnSize * 2;
        score += oppositeSize * -2;
        return score;
    }
    /**
     * Return a search depth for the current position.
     */
    private int chooseDepth() {
        return 2;
    }
}













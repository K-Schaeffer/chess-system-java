package chess;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import boardgame.Board;
import boardgame.Piece;
import boardgame.Position;
import chess.pieces.King;
import chess.pieces.Rook;

public class ChessMatch {

	private int turn;
	private Color currentPlayer;
	private Board board;
	private boolean check;
	private boolean checkMate;

	private List<Piece> piecesOnTheBoard = new ArrayList<>();
	private List<Piece> capturedPieces = new ArrayList<>();

	public ChessMatch() {
		board = new Board(8, 8);
		turn = 1;
		currentPlayer = Color.WHITE;
		initialSetup();
	}

	public int getTurn() {
		return turn;
	}

	public Color getCurrentPlayer() {
		return currentPlayer;
	}

	public boolean getCheck() {
		return check;
	}

	public boolean getCheckMate() {
		return checkMate;
	}

	public ChessPiece[][] getPieces() {
		ChessPiece[][] mat = new ChessPiece[board.getRows()][board.getColumns()];
		for (int i = 0; i < board.getRows(); i++) {
			for (int j = 0; j < board.getColumns(); j++) {
				mat[i][j] = (ChessPiece) board.piece(i, j);
			}
		}
		return mat;
	}

	public boolean[][] possibleMoves(ChessPosition sourcePosition) {
		Position position = sourcePosition.toPosition();
		validateSourcePosition(position);
		return board.piece(position).possibleMoves();
	}

	public ChessPiece performChessMove(ChessPosition sourcePosition, ChessPosition targetPosition) {
		Position source = sourcePosition.toPosition();
		Position target = targetPosition.toPosition();
		validateSourcePosition(source);
		validateTargetPosition(source, target);
		Piece capturedPiece = makeMove(source, target);

		if (testCheck(currentPlayer)) { // If the current player get a true value in the testCheck...
			undoMove(source, target, capturedPiece);
			throw new ChessException("You can't put yourself in check!");
		}

		check = (testCheck(opponent(currentPlayer))) ? true : false;

		if (testCheckMate(opponent(currentPlayer))) {
			checkMate = true;
		} else {
			nextTurn();
		}
		return (ChessPiece) capturedPiece;
	}

	private Piece makeMove(Position source, Position target) {
		ChessPiece p = (ChessPiece) board.removePiece(source); // Making a downcasting so I can use the
																// "increaseMoveCount"
		p.increaseMoveCount();
		Piece capturedPiece = board.removePiece(target);
		board.placePiece(p, target); // A natural upcasting to the placePiece P.

		if (capturedPiece != null) {
			piecesOnTheBoard.remove(capturedPiece);
			capturedPieces.add(capturedPiece);
		}

		return capturedPiece;
	}

	private void undoMove(Position source, Position target, Piece capturedPiece) {
		ChessPiece p = (ChessPiece) board.removePiece(target);
		p.decreaseMoveCount();
		board.placePiece(p, source);

		if (capturedPiece != null) {
			board.placePiece(capturedPiece, source);
			capturedPieces.remove(capturedPiece);
			piecesOnTheBoard.add(capturedPiece);
		}
	}

	private void validateSourcePosition(Position position) {
		if (!board.thereIsAPiece(position)) {
			throw new ChessException("There is no piece on source position");
		}
		if (currentPlayer != ((ChessPiece) board.piece(position)).getColor()) {
			throw new ChessException("The chosen piece is not yours");
		}
		if (!board.piece(position).isThereAnyPossibleMove()) {
			throw new ChessException("There is no possible moves for the chosen piece");
		}
	}

	private void validateTargetPosition(Position source, Position target) {
		if (!board.piece(source).possibleMove(target)) {
			throw new ChessException("The chosen piece can't move to target position");
		}
	}

	private void nextTurn() {
		turn++;
		currentPlayer = (currentPlayer == Color.WHITE) ? Color.BLACK : Color.WHITE;
	}

	private Color opponent(Color color) {
		return (color == Color.WHITE) ? Color.BLACK : Color.WHITE;
	}

	private ChessPiece king(Color color) {
		// That is a filter of Pieces. I'm taking the piecesOnTheBoard with the stream,
		// then i'm filtering using the lambda expression
		// which makes a downcasting of the Piece to have the ChessPiece attributes. If
		// the king x has the same color that the argument brought,
		// Then it will be part of the filter, which is made by the collect.
		List<Piece> list = piecesOnTheBoard.stream().filter(x -> ((ChessPiece) x).getColor() == color)
				.collect(Collectors.toList());
		for (Piece p : list) { // That for was made to look for the King in the filtered list.
			if (p instanceof King) { // if the piece is an instance of king, so It's a king.
				return (ChessPiece) p;
			}
		}
		throw new IllegalStateException("There is no " + color + " king on the board"); // This exception CAN'T happen.
	}

	private boolean testCheck(Color color) {
		Position kingPosition = king(color).getChessPosition().toPosition();
		List<Piece> opponentPieces = piecesOnTheBoard.stream() // The same filter, but using the method to take the
																// opponent color.
				.filter(x -> ((ChessPiece) x).getColor() == opponent(color)).collect(Collectors.toList());
		for (Piece p : opponentPieces) {
			boolean[][] mat = p.possibleMoves(); // The possible moves matrix of the Piece "p"
			if (mat[kingPosition.getRow()][kingPosition.getColumn()]) {
				return true; // It's returning true if the matrix of any opponentPiece finds the same
								// position of the kingPosition.
			}
		}
		return false;
	}

	private boolean testCheckMate(Color color) {
		if (!testCheck(color)) {
			return false;
		}
		List<Piece> list = piecesOnTheBoard.stream().filter(x -> ((ChessPiece) x).getColor() == color)
				.collect(Collectors.toList());
		for (Piece p : list) {
			boolean[][] mat = p.possibleMoves();
			for (int i = 0; i < board.getRows(); i++) { // Both for's to go through the matrix
				for (int j = 0; j < board.getColumns(); j++) {
					if (mat[i][j]) { // If there's a possible move...
						Position source = ((ChessPiece) p).getChessPosition().toPosition();
						Position target = new Position(i, j);
						Piece capturedPiece = makeMove(source, target); // Those three lines make the piece move to the
																		// possible position (to test)
						boolean testCheck = testCheck(color); // That will make sure if the piece will continue in check
						undoMove(source, target, capturedPiece); // Undo the test move
						if (!testCheck) { // If isnt in Check, so it's not a checkMate, we can get away of it
							return false;
						}
					}
				}
			}
		}
		return true; // If there's not any way.. It's a checkmate.
	}

	private void placeNewPiece(char column, int row, ChessPiece piece) {
		board.placePiece(piece, new ChessPosition(column, row).toPosition());
		piecesOnTheBoard.add(piece);
	}

	private void initialSetup() {
		/*
		 * placeNewPiece('c', 1, new Rook(board, Color.WHITE)); placeNewPiece('c', 2,
		 * new Rook(board, Color.WHITE)); placeNewPiece('d', 2, new Rook(board,
		 * Color.WHITE)); placeNewPiece('e', 2, new Rook(board, Color.WHITE));
		 * placeNewPiece('e', 1, new Rook(board, Color.WHITE)); placeNewPiece('d', 1,
		 * new King(board, Color.WHITE));
		 * 
		 * placeNewPiece('c', 7, new Rook(board, Color.BLACK)); placeNewPiece('c', 8,
		 * new Rook(board, Color.BLACK)); placeNewPiece('d', 7, new Rook(board,
		 * Color.BLACK)); placeNewPiece('e', 7, new Rook(board, Color.BLACK));
		 * placeNewPiece('e', 8, new Rook(board, Color.BLACK)); placeNewPiece('d', 8,
		 * new King(board, Color.BLACK));
		 */

		// An easy checkmate
		placeNewPiece('h', 7, new Rook(board, Color.WHITE));
		placeNewPiece('d', 1, new Rook(board, Color.WHITE));
		placeNewPiece('e', 1, new King(board, Color.WHITE));

		placeNewPiece('b', 8, new Rook(board, Color.BLACK));
		placeNewPiece('a', 8, new King(board, Color.BLACK));

	}

}

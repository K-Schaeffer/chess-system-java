package chess;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import boardgame.Board;
import boardgame.Piece;
import boardgame.Position;
import chess.pieces.Bishop;
import chess.pieces.King;
import chess.pieces.Knight;
import chess.pieces.Pawn;
import chess.pieces.Queen;
import chess.pieces.Rook;

public class ChessMatch {

	private int turn;
	private Color currentPlayer;
	private Board board;
	private boolean check;
	private boolean checkMate;
	private ChessPiece enPassantVulnerable;
	private ChessPiece promoted;
	
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
	
	public ChessPiece getEnPassantVulnerable() {
		return enPassantVulnerable;
	}

	public ChessPiece getPromoted() {
		return promoted;
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
		
		ChessPiece movedPiece = (ChessPiece) board.piece(target); 

		check = (testCheck(opponent(currentPlayer))) ? true : false;

		// #specialmove promotion
		promoted = null;
		if (movedPiece instanceof Pawn) {
			if((movedPiece.getColor() == Color.WHITE && target.getRow() == 0) || (movedPiece.getColor() == Color.BLACK && target.getRow() == 7)) {
				promoted = (ChessPiece) board.piece(target); // Setting the pawn so I can make a defensive programming
				promoted = replacePromotedPiece("Q"); // By default, I start with the Queen to make the process easier
			}
		}
		
		
		if (testCheckMate(opponent(currentPlayer))) {
			checkMate = true;
		} else {
			nextTurn();
		}
		
		// #specialmove en passant
		if(movedPiece instanceof Pawn && (target.getRow() == source.getRow() - 2 || target.getRow() == source.getRow() + 2)){
			// if it's a pawn and the line difference its more 2 or less 2...
			enPassantVulnerable = movedPiece;			
		}else {
			enPassantVulnerable = null;
		}
		
		return (ChessPiece) capturedPiece;
	}

	public ChessPiece replacePromotedPiece(String type) {
		if(promoted == null) {
			throw new IllegalStateException("There is no piece to be promoted");
		}
		if(!type.equals("B") && !type.equals("N") && !type.equals("R") && !type.equals("Q")){
			throw new InvalidParameterException("Invalid type for promotion");
		}
		
		// Getting the promoted piece position and removing it from the board
		Position pos = promoted.getChessPosition().toPosition();		
		Piece p = board.removePiece(pos);
		piecesOnTheBoard.remove(p);
		
		//Instantiating the new piece and placing it on the board
		ChessPiece newPiece = newPiece(type, promoted.getColor()); 
		board.placePiece(newPiece, pos);
		piecesOnTheBoard.add(newPiece);
		
		return newPiece;
	}
	
	private ChessPiece newPiece(String type, Color color) { //Method to help in the promotion
		if(type.equals("B")) return new Bishop(board, color);
		if(type.equals("N")) return new Knight(board, color);
		if(type.equals("Q")) return new Queen(board, color);
		return new Rook(board, color);
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

		// #specialmove castling kingside rook (minor castling)

		if (p instanceof King && target.getColumn() == source.getColumn() + 2) { // If the piece is a king and its
																					// moving twice to the right side...
			Position sourceT = new Position(source.getRow(), source.getColumn() + 3);
			Position targetT = new Position(source.getRow(), source.getColumn() + 1);
			ChessPiece rook = (ChessPiece) board.removePiece(sourceT);
			board.placePiece(rook, targetT);
			rook.increaseMoveCount();
			// With that I move manually the Rook (T)
		}

		// #specialmove castling queenside rook (major castling)

		if (p instanceof King && target.getColumn() == source.getColumn() - 2) {
			Position sourceT = new Position(source.getRow(), source.getColumn() - 4);
			Position targetT = new Position(source.getRow(), source.getColumn() - 1);
			ChessPiece rook = (ChessPiece) board.removePiece(sourceT);
			board.placePiece(rook, targetT);
			rook.increaseMoveCount();
		}

		// #specialmove en passant
		if(p instanceof Pawn) {
			if (source.getColumn() != target.getColumn() && capturedPiece == null) { // If the Pawn walked through diagonal without capturing...
				Position pawnPosition;
				if(p.getColor() == Color.WHITE) { // Condition to know the positions of the captured pawns
					pawnPosition = new Position(target.getRow() + 1, target.getColumn()); // The white en passant will capture the piece below
				}else {
					pawnPosition = new Position(target.getRow() - 1, target.getColumn()); // The black en passant will capture the piece above
				}
				capturedPiece = board.removePiece(pawnPosition); //Removing manually the pawn
				capturedPieces.add(capturedPiece); // Adding manually the captured pawn in the list
				piecesOnTheBoard.remove(capturedPiece);  // Removing it from piecesOnTheBoard
			}
		}
		
		return capturedPiece;
	}

	private void undoMove(Position source, Position target, Piece capturedPiece) {
		ChessPiece p = (ChessPiece) board.removePiece(target);
		p.decreaseMoveCount();
		board.placePiece(p, source);

		if (capturedPiece != null) {
			board.placePiece(capturedPiece, target);
			capturedPieces.remove(capturedPiece);
			piecesOnTheBoard.add(capturedPiece);
		}
		

		// #specialmove castling kingside rook (minor castling)

		if (p instanceof King && target.getColumn() == source.getColumn() + 2) {
			Position sourceT = new Position(source.getRow(), source.getColumn() + 3);
			Position targetT = new Position(source.getRow(), source.getColumn() + 1);
			ChessPiece rook = (ChessPiece) board.removePiece(targetT);
			board.placePiece(rook, sourceT);
			rook.decreaseMoveCount();
			// With that I remove manually the Rook (T) - I Undo the castling
		}

		// #specialmove castling queenside rook (major castling)

		if (p instanceof King && target.getColumn() == source.getColumn() - 2) {
			Position sourceT = new Position(source.getRow(), source.getColumn() - 4);
			Position targetT = new Position(source.getRow(), source.getColumn() - 1);
			ChessPiece rook = (ChessPiece) board.removePiece(targetT);
			board.placePiece(rook, sourceT);
			rook.decreaseMoveCount();
		}
		
		// #specialmove en passant
		if (p instanceof Pawn) { // Condition to undo the en passant
			if (source.getColumn() != target.getColumn() && capturedPiece == enPassantVulnerable) {
				ChessPiece pawn = (ChessPiece)board.removePiece(target); // Removing
				Position pawnPosition;
				if (p.getColor() == Color.WHITE) { // Testing the right position to place the captured pawn
					pawnPosition = new Position(3, target.getColumn()); 
																							
				} else {
					pawnPosition = new Position(4, target.getColumn()); 
				}
				board.placePiece(pawn, pawnPosition); 
			}
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
		placeNewPiece('a', 1, new Rook(board, Color.WHITE));
		placeNewPiece('b', 1, new Knight(board, Color.WHITE));
		placeNewPiece('c', 1, new Bishop(board, Color.WHITE));
		placeNewPiece('d', 1, new Queen(board, Color.WHITE));
		placeNewPiece('e', 1, new King(board, Color.WHITE, this)); // "this" is the self-reference to THIS chessmatch
		placeNewPiece('f', 1, new Bishop(board, Color.WHITE));
		placeNewPiece('g', 1, new Knight(board, Color.WHITE));
		placeNewPiece('h', 1, new Rook(board, Color.WHITE));
		placeNewPiece('a', 2, new Pawn(board, Color.WHITE, this));
		placeNewPiece('b', 2, new Pawn(board, Color.WHITE, this));
		placeNewPiece('c', 2, new Pawn(board, Color.WHITE, this));
		placeNewPiece('d', 2, new Pawn(board, Color.WHITE, this));
		placeNewPiece('e', 2, new Pawn(board, Color.WHITE, this));
		placeNewPiece('f', 2, new Pawn(board, Color.WHITE, this));
		placeNewPiece('g', 2, new Pawn(board, Color.WHITE, this));
		placeNewPiece('h', 2, new Pawn(board, Color.WHITE, this));

		placeNewPiece('a', 8, new Rook(board, Color.BLACK));
		placeNewPiece('b', 8, new Knight(board, Color.BLACK));
		placeNewPiece('c', 8, new Bishop(board, Color.BLACK));
		placeNewPiece('d', 8, new Queen(board, Color.BLACK));
		placeNewPiece('e', 8, new King(board, Color.BLACK, this));
		placeNewPiece('f', 8, new Bishop(board, Color.BLACK));
		placeNewPiece('g', 8, new Knight(board, Color.BLACK));
		placeNewPiece('h', 8, new Rook(board, Color.BLACK));
		placeNewPiece('a', 7, new Pawn(board, Color.BLACK, this));
		placeNewPiece('b', 7, new Pawn(board, Color.BLACK, this));
		placeNewPiece('c', 7, new Pawn(board, Color.BLACK, this));
		placeNewPiece('d', 7, new Pawn(board, Color.BLACK, this));
		placeNewPiece('e', 7, new Pawn(board, Color.BLACK, this));
		placeNewPiece('f', 7, new Pawn(board, Color.BLACK, this));
		placeNewPiece('g', 7, new Pawn(board, Color.BLACK, this));
		placeNewPiece('h', 7, new Pawn(board, Color.BLACK, this));

	}

}

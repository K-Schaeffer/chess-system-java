package chess.pieces;

import boardgame.Board;
import boardgame.Position;
import chess.ChessPiece;
import chess.Color;

public class Pawn extends ChessPiece {

	public Pawn(Board board, Color color) {
		super(board, color);
	}

	@Override
	public boolean[][] possibleMoves() {
		boolean[][] mat = new boolean[getBoard().getRows()][getBoard().getColumns()];

		Position p = new Position(0, 0);

		// The white pawn movement logic

		if (getColor() == Color.WHITE) {
			p.setValues(position.getRow() - 1, position.getColumn()); // It will move with -1 Row, because it will go UP
			if (getBoard().positionExists(p) && !getBoard().thereIsAPiece(p)) { // If the position above him exists and
																				// it's
																				// empty it will move
				mat[p.getRow()][p.getColumn()] = true;
			}
			p.setValues(position.getRow() - 2, position.getColumn()); // The White pawn first movement logic (It can
																		// move 2 positions)
			Position p2 = new Position(position.getRow() - 1, position.getColumn()); // The aux Position p2 in the
																						// first position
			if (getBoard().positionExists(p) && !getBoard().thereIsAPiece(p) && getBoard().positionExists(p2)
					&& !getBoard().thereIsAPiece(p2) && getMoveCount() == 0) { // Condition testing the first/second
																				// position and if is the first move

				mat[p.getRow()][p.getColumn()] = true;
			}

			p.setValues(position.getRow() - 1, position.getColumn() - 1); // The left diagonal
			if (getBoard().positionExists(p) && isThereOpponentPiece(p)) { // If the position exists and there's a
																			// opponent...
				mat[p.getRow()][p.getColumn()] = true;

			}

			p.setValues(position.getRow() - 1, position.getColumn() + 1); // The right diagonal
			if (getBoard().positionExists(p) && isThereOpponentPiece(p)) {
				mat[p.getRow()][p.getColumn()] = true;

			}
		} else {
			// The black pawn movement logic
			p.setValues(position.getRow() + 1, position.getColumn()); // It will move with +1 Row, because it will go
																		// DOWN
			if (getBoard().positionExists(p) && !getBoard().thereIsAPiece(p)) {
				mat[p.getRow()][p.getColumn()] = true;
			}
			p.setValues(position.getRow() + 2, position.getColumn());

			Position p2 = new Position(position.getRow() + 1, position.getColumn());

			if (getBoard().positionExists(p) && !getBoard().thereIsAPiece(p) && getBoard().positionExists(p2)
					&& !getBoard().thereIsAPiece(p2) && getMoveCount() == 0) {

				mat[p.getRow()][p.getColumn()] = true;
			}

			p.setValues(position.getRow() + 1, position.getColumn() - 1);
			if (getBoard().positionExists(p) && isThereOpponentPiece(p)) {
				mat[p.getRow()][p.getColumn()] = true;

			}

			p.setValues(position.getRow() + 1, position.getColumn() + 1);
			if (getBoard().positionExists(p) && isThereOpponentPiece(p)) {
				mat[p.getRow()][p.getColumn()] = true;

			}

		}

		return mat;
	}

	@Override
	public String toString() {
		return "P";
	}

}

package chess;

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
	
	private List<Piece> piecesOnTheBoard;
	private List<Piece> capturedPieces;

	public ChessMatch() {   
		board = new Board(8,8);
		currentPlayer = Color.WHITE;
		turn = 1;
		check = false;
		checkMate = false;
		enPassantVulnerable = null;
		promoted = null;
		piecesOnTheBoard = new ArrayList<>();
		capturedPieces = new ArrayList<>();
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
	
	//returns matrix of chess pieces corresponding to this game
	public ChessPiece[][] getPieces() {
		ChessPiece[][] mat = new ChessPiece[board.getRows()][board.getColumns()];
		for(int i=0; i<board.getRows(); i++) {
			for(int j=0; j<board.getColumns(); j++) {
				mat[i][j] = (ChessPiece)board.piece(i, j);
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
		Position target= targetPosition.toPosition();
		// validate if the origin position exists
		validateSourcePosition(source);
		// validate if the target position exists
		validateTargetPosition(source, target);
		Piece capturePiece = makeMove(source, target); 
		

		// if true, then the player made a move that put himself in check 
		//  or a move that kept him in check
		if(testCheck(currentPlayer)) {
			undoMove(source, target, capturePiece);
			throw new ChessException("You can't put yourself in check");
		}
		ChessPiece movedPiece = (ChessPiece)board.piece(target);
		
		// special move Promotion
		// test Promotion before checking 
		// because after Promotion the new piece may leave the opponent in check
		promoted = null; //to ensure you are running a new test
		if(movedPiece instanceof Pawn) { //if the moved piece was a pawn
			//if the moved piece is of <color> and has reached the end of the board
			if(movedPiece.getColor() == Color.WHITE && target.getRow() == 0 || 
					movedPiece.getColor() == Color.BLACK && target.getRow() == 7){
				// piece that arrived at the end
				 promoted = (ChessPiece)board.piece(target);
				// by default it changes to Queen
				// but it is allowed to exchange for another piece
				 promoted = replacePromotedPiece("Q"); 
			}
		}
		
		check = (testCheck(opponent(currentPlayer))) ? true : false;

		//if the currentPlayer made a move that put the opponent in Check
		// then check=true, otherwise check=false
		if(testCheckMate(opponent(currentPlayer))) {
			checkMate = true;
		}
		else {
			nextTurn();
		}
		// special move En Passant
		// if the piece that was moved was a pawn and it moved two squares 
		// to White Pieces or Black Pieces
		if(movedPiece instanceof Pawn && target.getRow() == source.getRow() + 2 ||
				target.getRow() == source.getRow() - 2) {
			// then the vulnerable piece is the one that was moved
			enPassantVulnerable = movedPiece;
		}else {
			// otherwise the piece is not vulnerable
			enPassantVulnerable = null;
		}
		return (ChessPiece)capturePiece;
	}
	
	private void validateSourcePosition(Position position) { 
		if(!board.thereIsAPiece(position)) {
			throw new ChessException("There is no piece on source position");
		}
		// the colors of the player and that player's pieces must be the same
		if(currentPlayer != ((ChessPiece)board.piece(position)).getColor()){
			throw new  ChessException("The chosen piece is not yours");
		}
		if(!board.piece(position).isThereAnyPossibleMove()) {
			throw new ChessException("There is no possible move for the chosen piece");
		}
	}
	
	public ChessPiece replacePromotedPiece(String type) {//type is the type of the piece
		if(promoted == null) {
			throw new IllegalStateException("There is no piece to be promoted");
		}
		if(!type.equals("B") && !type.equals("N") && !type.equals("R") && !type.equals("Q")) {
			return promoted;
		}
		Position pos = promoted.getChessPosition().toPosition();
		// remove the piece that was promoted, because we are going to replace it
		Piece p = board.removePiece(pos);
		// remove the list of pieces from the board
		piecesOnTheBoard.remove(p);
		ChessPiece newPiece = newPiece(type, promoted.getColor());
		// places the new piece in the position of the promoted piece
		board.placePiece(newPiece, pos);
		// add the new piece that was promoted to the list of pieces on the board
		piecesOnTheBoard.add(newPiece); 
		return newPiece;
 	}
	
	private ChessPiece newPiece(String type, Color color) {
		if(type.equals("Q")) return new Queen(board, color);
		if(type.equals("N")) return new Knight(board, color);
		if(type.equals("R")) return new Rook(board, color);
		return new Bishop(board, color);
	}
	
	private void validateTargetPosition(Position source, Position target) {
		if (!board.piece(source).possibleMove(target)) {
			 throw new ChessException("The chosen piece can't move to target position");
		}
	}
	
	private void nextTurn() {
		turn++;
		//if the current player is white, then he changes to black, otherwise he is white
		currentPlayer = (currentPlayer == Color.WHITE) ? Color.BLACK : Color.WHITE;
	}
	
	
	private Piece makeMove(Position source, Position target) {
		// removes the piece that was in the original position
		ChessPiece p = (ChessPiece)board.removePiece(source);
		p.increaseMoveCount();
		// remove the possible piece that is in the destination (captured)
		Piece capturedPiece = board.removePiece(target);
		// puts position p in the destination position
		board.placePiece(p, target);
		
		if (capturedPiece != null) {
			piecesOnTheBoard.remove(capturedPiece);
			capturedPieces.add(capturedPiece); 
		}
		
		// special move Castling Kingside Rook
		// if p is a King and Moves 2 spaces to the Right, then it was a Small Rook -> move the Rook
		if(p instanceof King && target.getColumn() == source.getColumn() + 2) {
			// origin position of the King's right tower
			Position sourceT =  new Position(source.getRow(), source.getColumn() + 3);
			// destination position of the King's right tower
			Position targetT =  new Position(source.getRow(), source.getColumn() + 1);
			// remove the tower
			ChessPiece rook = (ChessPiece)board.removePiece(sourceT);
			// place the tower in the target position (targetT)
			board.placePiece(rook, targetT);
			rook.increaseMoveCount();
		}
		
		// special move Castling Queenside Rook
		// if p is a King and Moves 2 spaces to the Left, then it was a Big Roque -> move the Rook
		if(p instanceof King && target.getColumn() == source.getColumn() - 2) {
			// origin position of the King's right tower
			Position sourceT =  new Position(source.getRow(), source.getColumn() - 4);
			// destination position of the King's right tower
			Position targetT =  new Position(source.getRow(), source.getColumn() - 1);
			// remove the tower
			ChessPiece rook = (ChessPiece)board.removePiece(sourceT);
			// place the tower in the target position (targetT)
			board.placePiece(rook, targetT);
			rook.increaseMoveCount();
		}
		
		// special move En Passant
		if(p instanceof Pawn) {
			//if the pawn went diagonal there are two cases: he ate someone or he is an en passant
			if(source.getColumn() != target.getColumn() && capturedPiece == null) {
				Position pawnPosition;
				if(p.getColor() == Color.WHITE) {
					// then the piece to be captured is below my white piece, one line down
					pawnPosition = new Position(target.getRow()+1, target.getColumn());
				}else {
					pawnPosition = new Position(target.getRow()-1, target.getColumn());
				}
				capturedPiece = board.removePiece(pawnPosition);
				capturedPieces.add(capturedPiece);
				piecesOnTheBoard.remove(capturedPiece);  
			}
		}
		return capturedPiece;
 	}
	
	private void undoMove(Position source, Position target, Piece capturedPiece) {
		ChessPiece p = (ChessPiece)board.removePiece(target);
		p.decreaseMoveCount();
		// place the piece where it was, at the origin
		board.placePiece(p, source);
		if (capturedPiece != null) {
			board.placePiece(capturedPiece, target);
			piecesOnTheBoard.add(capturedPiece);   
			capturedPieces.remove(capturedPiece);
		}

		// special move Castling Kingside Rook
		// if p is a King and moved 2 squares to the right, then it was a Small Castling -> move the Rook
		if(p instanceof King && target.getColumn() == source.getColumn() + 2) {
			// origin position of the King's right tower
			Position sourceT =  new Position(source.getRow(), source.getColumn() + 3);
			// destination position of the King's right tower
			Position targetT =  new Position(source.getRow(), source.getColumn() + 1);
			ChessPiece rook = (ChessPiece)board.removePiece(targetT); 
			board.placePiece(rook, sourceT); 
			rook.decreaseMoveCount();   
		}
		// special move Castling Queenside Rook
		// if p is a King and Moves 2 spaces to the Left, then it was a Big Roque -> move the Rook
		if(p instanceof King && target.getColumn() == source.getColumn() - 2) {
			Position sourceT =  new Position(source.getRow(), source.getColumn() - 4);
			Position targetT =  new Position(source.getRow(), source.getColumn() - 1);
			ChessPiece rook = (ChessPiece)board.removePiece(targetT); 
			board.placePiece(rook, sourceT); 
			rook.decreaseMoveCount();   
		}
		
		// special move En Passant
		if(p instanceof Pawn) {
			//if the captured piece is an en passant
			if(source.getColumn() != target.getColumn() && capturedPiece == enPassantVulnerable){
				Position pawnPosition;
				// remove piece that was in the wrong place
				ChessPiece pawn = (ChessPiece)board.removePiece(target);
				if(p.getColor() == Color.WHITE) {
					//then the piece to be captured is below my white piece, one line down
					pawnPosition = new Position(3, target.getColumn());
				}else {
					pawnPosition = new Position(4, target.getColumn());
				}
				board.placePiece(pawn, pawnPosition);
			}
		}
	}
	
	private Color opponent(Color color) {
		return (color == Color.WHITE) ? Color.BLACK : Color.WHITE; 
	}
	
	//look for the king of that color in the list of pieces in play
	private ChessPiece king(Color color) {
		List<Piece> list = piecesOnTheBoard.stream().filter(x -> 
		((ChessPiece)x).getColor() == color).collect(Collectors.toList()); 
		for (Piece p : list) {
			if(p instanceof King) {
				return ((ChessPiece)p);
			}
		}
		throw new IllegalStateException("There is no " + color + "king on the board"); 
	}
	
	private boolean testCheck(Color color) {
		Position kingPosition = king(color).getChessPosition().toPosition();
		// list of pieces on the board filtered with the color of that king's opponent
		List<Piece> opponentPieces = piecesOnTheBoard.stream().filter(x -> 
		((ChessPiece)x).getColor() == opponent(color)).collect(Collectors.toList()) ;
		for(Piece p : opponentPieces) {
			boolean[][] mat = p.possibleMoves();
			if(mat[kingPosition.getRow()][kingPosition.getColumn()] == true) {
				return true;
			}
		}
		return false;
	}
	
	private boolean testCheckMate(Color color) {
		//if it is not in check, it is also not in checkMate
		if(!testCheck(color)) { 
			return false;
		}
		
		List<Piece> list = piecesOnTheBoard.stream().filter(x -> 
		((ChessPiece)x).getColor() == color).collect(Collectors.toList());
		// if there is a piece "p" that has a movement that removes the check, return false
		for(Piece p : list) {
			boolean[][] mat = p.possibleMoves();
			for(int i=0; i<board.getRows(); i++) {
				for(int j=0; j<board.getColumns(); j++) {
					//if it is a possible movement
					if(mat[i][j]) { 
						Position source = ((ChessPiece)p).getChessPosition().toPosition();
						Position target = new Position(i,j);
						Piece capturedPiece = makeMove(source, target);
						//tests if the king of my color is still in check
						boolean testCheck = testCheck(color); 
						undoMove(source, target, capturedPiece);
						if(!testCheck) {
							return false;
						}
					}
				}
			}
			
		}
		return true;
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
        placeNewPiece('e', 1, new King(board, Color.WHITE, this));
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

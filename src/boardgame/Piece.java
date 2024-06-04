package boardgame;

public abstract class Piece {
	protected Position position;
	private Board board;

	public Piece(Board board) { 
		this.board = board;
		//the position of a newly created piece will be null, not yet placed on the board
		position = null;
	}

	protected Board getBoard() {
		return board;
	}
	
	//matrix of false/true 
	public abstract boolean[][] possibleMoves(); // true = allowed to move
	
	//if possible move to certain position
	public boolean possibleMove(Position position) {
		return possibleMoves()[position.getRow()][position.getColumn()];
	} 
	
	//to find out if the piece is locked, if there is at least 1 possible movement
	public boolean isThereAnyPossibleMove() {
		boolean[][] mat = possibleMoves();
		for(int i=0; i<mat.length; i++) {
			for(int j=0; j<mat.length; j++) {
				if(mat[i][j]) {
					return true;
				}
			}
		}
		return false;
	}
}

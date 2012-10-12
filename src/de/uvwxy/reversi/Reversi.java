package de.uvwxy.reversi;

import java.nio.ByteBuffer;

import android.util.Log;

public class Reversi {
	// vars to keep board dimensions:
	private int vBoardWidth = 8;
	private int vBoardHeight = 8;

	// Byte: It has a minimum value of -128 and a maximum value of 127 (inclusive)
	// byte positions:
	private int bBoardWidth = 0; // +128 to remove sign
	private int bBoardHeight = 1; // +128 to remove sign
	private int bBoardBegin = bBoardHeight + 1;
	private int bBoardEnd = bBoardBegin + (vBoardWidth * vBoardHeight);
	private int bNumPlayingPlayers = bBoardEnd;

	// all 8*2bytes are present all the time!
	// two bytes for score each (-> (127+128)*(127+128) = 65025 = 0b1111 11110 0000 0001
	private int bScore0 = bNumPlayingPlayers + 1;
	private int bScore1 = bScore0 + 2;
	private int bScore2 = bScore1 + 2;
	private int bScore3 = bScore2 + 2;
	private int bScore4 = bScore3 + 2;
	private int bScore5 = bScore4 + 2;
	private int bScore6 = bScore5 + 2;
	private int bScore7 = bScore6 + 2;
	private int bPlayerMove = bScore7 + 1;

	// byte IDs for board data
	private final byte iEmpty = 0;
	private final byte iPlayer0 = 1;
	private final byte iPlayer1 = 2;
	private final byte iPlayer2 = 3;
	private final byte iPlayer3 = 4;
	private final byte iPlayer4 = 5;
	private final byte iPlayer5 = 6;
	private final byte iPlayer6 = 7;
	private final byte iPlayer7 = 8;
	private final byte iNone = 9;

	/**
	 * Initially: Board size: [boardWidth,boardHeight,...]
	 * 
	 * Second: The board data: [0/0, 0/1, 0/2, 0/3, 0/4, 0/5, 0/6, 0/7,..., 1/0, 1/1, 1/2,...]
	 * 
	 * Third: Number of playing players [2 - 8]
	 * 
	 * Fourth: Score for all possible 8 players (2bytes each): [...,player0score,player1score,player2score,...]
	 * 
	 * Fifth: Which player has to move: [...,player0|player1,...]
	 * 
	 */
	byte[] boardData;

	public Reversi() {
		newStandard2PBoard();
	}
	
	public Reversi(int w, int h){
		newStandard2PBoard(w, h);
	}

	public Reversi(byte[] boardData) {
		this.boardData = boardData;
	}

	private void setupBytePositions() {
		// byte positions:
		bBoardWidth = 0; // +128 to remove sign
		bBoardHeight = 1; // +128 to remove sign
		bBoardBegin = bBoardHeight + 1;
		// [boardBegin ... boardEnd]
		bBoardEnd = bBoardBegin + (vBoardWidth * vBoardHeight);
		bNumPlayingPlayers = bBoardEnd + 1;

		// two bytes for score each (-> (127+128)*(127+128) = 65025 = 0b1111 11110 0000 0001
		bScore0 = bNumPlayingPlayers + 1;
		bScore1 = bScore0 + 2;
		bScore2 = bScore1 + 2;
		bScore3 = bScore2 + 2;
		bScore4 = bScore3 + 2;
		bScore5 = bScore4 + 2;
		bScore6 = bScore5 + 2;
		bScore7 = bScore6 + 2;
		bPlayerMove = bScore7 + 1;

		boardData = new byte[bPlayerMove + 1];
	}

	private void setWidth(int w) {
		boardData[bBoardWidth] = (byte) (w - 128);
	}

	public int getWidth() {
		return boardData[bBoardWidth] + 128;
	}

	private void setHeight(int h) {
		boardData[bBoardHeight] = (byte) (h - 128);
	}

	public int getHeight() {
		return boardData[bBoardHeight] + 128;
	}

	private void noneBoard() {
		for (int i = bBoardBegin; i < bBoardEnd; i++) {
			boardData[i] = iNone;
		}
	}

	private void emptyBoard() {
		for (int i = bBoardBegin; i < bBoardEnd; i++) {
			boardData[i] = iEmpty;
		}
	}

	private void setNumPlayers(int n) {
		boardData[bNumPlayingPlayers] = (byte) (n);
	}

	public int getNumPlayers() {
		return boardData[bNumPlayingPlayers];
	}

	private void setStartBlock(int x, int y, byte playerA, byte playerB) {
		// 3/3 3/4 -> 1 2
		// 4/3 4/4 -> 2 1

		boardData[bBoardBegin + (vBoardWidth * (y - 1)) + (x - 1)] = playerA;
		boardData[bBoardBegin + (vBoardWidth * (y - 1)) + x] = playerB;
		boardData[bBoardBegin + (vBoardWidth * y) + (x - 1)] = playerB;
		boardData[bBoardBegin + (vBoardWidth * y) + (x)] = playerA;

	}

	/**
	 * 
	 * @param player
	 *            [1..8]
	 * @param points
	 *            [15bits]
	 */
	public void setPlayerPoints(int player, int points) {
		ByteBuffer b = ByteBuffer.allocate(4);
		b.putInt(points);

		// [0] [1] [2] [3]
		// [0..0][0..0][left][right]
		byte left = b.get(2);
		byte right = b.get(3);

		boardData[bScore0 + (2 * player) - 2] = left;
		boardData[bScore0 + (2 * player) - 1] = right;

	}

	/**
	 * 
	 * @param player
	 *            [1..8]
	 * @return [15bits]
	 */
	public int getPlayerPoints(int player) {
		ByteBuffer b = ByteBuffer.allocate(4);
		b.put((byte) 0);
		b.put((byte) 0);
		// player is 1..8 -> add 0 for bScore0, etc.
		b.put(boardData[bScore0 + (2 * player) - 2]);
		b.put(boardData[bScore0 + (2 * player) - 1]);
		b.position(0);
		return b.getInt();
	}

	public void resetAllPlayerPoints() {
		setPlayerPoints(iPlayer0, 0);
		setPlayerPoints(iPlayer1, 0);
		setPlayerPoints(iPlayer2, 0);
		setPlayerPoints(iPlayer3, 0);
		setPlayerPoints(iPlayer4, 0);
		setPlayerPoints(iPlayer5, 0);
		setPlayerPoints(iPlayer6, 0);
		setPlayerPoints(iPlayer7, 0);
	}

	public void recalculateAllPlayerPoints() {
		int[] ppoints = new int[8];

		for (int i = bBoardBegin; i <= bBoardEnd; i++) {
			if (boardData[i] >= 1 && boardData[i] <= 8) {
				ppoints[boardData[i] - 1]++;
			}
		}

		for (int i = 0; i < 8; i++) {
			setPlayerPoints((i + 1), ppoints[i]);
		}
	}

	public void selectRandomPlayerForMove() {
		boardData[bPlayerMove] = (byte) (Math.random() * getNumPlayers() + 1);
	}

	public byte getPlayerWhichHasToMove() {
		return boardData[bPlayerMove];
	}

	public void setPlayerWhichHasToMove(int player) {
		boardData[bPlayerMove] = (byte) player;
	}

	void newStandard2PBoard() {
		newStandard2PBoard(8, 8);
	}

	void newStandard2PBoard(int w, int h) {
		vBoardWidth = w;
		vBoardHeight = h;

		setupBytePositions();

		setWidth(w);
		setHeight(h);

		emptyBoard();

		setNumPlayers(2);

		// this tries to center the starting position
		int bW2 = (vBoardWidth / 2);
		int bH2 = (vBoardHeight / 2);

		setStartBlock(bW2, bH2, iPlayer0, iPlayer1);

	}

	public byte get(int x, int y) {
		return boardData[bBoardBegin + (x) + (y * vBoardWidth)];
	}

	public void set(int x, int y, byte b) {
		boardData[bBoardBegin + (x) + (y * vBoardWidth)] = b;
	}

	public void printBoard() {
		String buf = "";
		for (int i = bBoardBegin; i < bBoardEnd; i++) {
			if ((i - bBoardBegin) % getWidth() == 0) {
				buf += "\n";
			}
			buf += "" + boardData[i];
		}
		Log.i("REV", "Board:\n" + buf);
	}
	
	
	public boolean moveIsPossible(int x, int y, int player){
		
		return false;
	}
	
	public void doMove(int x, int y, int player){
		// TODO!
	}
}
package de.uvwxy.reversi;

import java.nio.ByteBuffer;

import android.util.Log;

public class Reversi {

	// byte IDs for board data
	public static final byte iEmpty = 0;
	public static final byte iPlayer0 = 1;
	public static final byte iPlayer1 = 2;
	public static final byte iPlayer2 = 3;
	public static final byte iPlayer3 = 4;
	public static final byte iPlayer4 = 5;
	public static final byte iPlayer5 = 6;
	public static final byte iPlayer6 = 7;
	public static final byte iPlayer7 = 8;
	public static final byte iNone = 9;

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
	private int bPlayerMove = bScore7 + 2;

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
	private byte[] boardData;

	public Reversi() {
		newStandard2PBoard();
	}

	public Reversi(int w, int h) {
		newStandard2PBoard(w, h);
	}

	public Reversi(byte[] boardData) {
		this.boardData = boardData;
		vBoardWidth = getWidth();
		vBoardHeight = getHeight();
		setupBytePositions();
	}

	public byte[] getObjectData() {
		return boardData;
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
		bPlayerMove = bScore7 + 2;
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
	 * Get the number of points of this player on this board
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
		boardData = new byte[bPlayerMove + 1];

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

	public boolean moveIsPossible(int x, int y, byte player) {
		// check four directions
		// 012
		// 7 3
		// 654
		// Log.i("REV", "Checking: " + x + "/" + y);
		// Log.i("REV", "Testing 0");
		if (testMove(x, y, -1, -1, player))
			return true;
		// Log.i("REV", "Testing 1");
		if (testMove(x, y, 0, -1, player))
			return true;
		// Log.i("REV", "Testing 2");
		if (testMove(x, y, 1, -1, player))
			return true;
		// Log.i("REV", "Testing 3");
		if (testMove(x, y, 1, 0, player))
			return true;
		// Log.i("REV", "Testing 4");
		if (testMove(x, y, 1, 1, player))
			return true;
		// Log.i("REV", "Testing 5");
		if (testMove(x, y, 0, 1, player))
			return true;
		// Log.i("REV", "Testing 6");
		if (testMove(x, y, -1, 1, player))
			return true;
		// Log.i("REV", "Testing 7");
		if (testMove(x, y, -1, 0, player))
			return true;

		return false;
	}

	/**
	 * Returns true if the player has a move on this board.
	 * 
	 * @param player
	 * @return
	 */
	public boolean playerHasMove(byte player) {
		for (int x = 0; x < vBoardWidth; x++) {
			for (int y = 0; y < vBoardHeight; y++) {
				if (moveIsPossible(x, y, player))
					return true;
			}
		}
		return false;
	}

	private boolean testMove(int x, int y, int left, int up, byte player) {
		// non empty fields are not valid to place a stone
		if (get(x, y) != iEmpty)
			return false;

		boolean foundOtherStone = false;
		int xi = x;
		int yi = y;
		yi += left;
		xi += up;
		// >= 0 otherwise border is never checked
		while (xi < vBoardWidth && xi >= 0 && yi < vBoardHeight && yi >= 0) {
			byte vi = get(xi, yi);

			// fail if found empty
			if (vi == iEmpty)
				return false;
			// return true if finally player stone and found other stone before
			if (vi == player)
				return true && foundOtherStone;
			if (vi != player && vi <= iPlayer7 && vi >= iPlayer0)
				foundOtherStone = true;
			yi += left;
			xi += up;
		}

		return false;
	}

	public void doMove(int xm, int ym, byte player) {
		boolean[][] flipMatrix = new boolean[vBoardWidth][vBoardHeight];

		checkAndFillMatrix(xm, ym, -1, -1, player, flipMatrix);
		checkAndFillMatrix(xm, ym, 0, -1, player, flipMatrix);
		checkAndFillMatrix(xm, ym, 1, -1, player, flipMatrix);

		checkAndFillMatrix(xm, ym, 1, 0, player, flipMatrix);

		checkAndFillMatrix(xm, ym, 1, 1, player, flipMatrix);
		checkAndFillMatrix(xm, ym, 0, 1, player, flipMatrix);
		checkAndFillMatrix(xm, ym, -1, 1, player, flipMatrix);

		checkAndFillMatrix(xm, ym, -1, 0, player, flipMatrix);

		for (int x = 0; x < vBoardWidth; x++) {
			for (int y = 0; y < vBoardHeight; y++) {
				if (flipMatrix[x][y])
					set(x, y, (byte) player);
			}
		}

		recalculateAllPlayerPoints();
	}

	private int checkAndFillMatrix(int xm, int ym, int left, int up, byte player, boolean[][] flipMatrix) {
		if (testMove(xm, ym, left, up, player))
			return collectMove(xm, ym, left, up, player, flipMatrix);
		return 0;
	}

	private int collectMove(int x, int y, int left, int up, byte player, boolean[][] flipMatrix) {
		// non empty fields are not valid to place a stone
		int xi = x;
		int yi = y;
		int sum = 0;
		flipMatrix[xi][yi] = true;
		yi += left;
		xi += up;
		while (xi < vBoardWidth && xi >= 0 && yi < vBoardHeight && y >= 0) {
			// halt at first own stone
			if (get(xi, yi) == player)
				break;
			flipMatrix[xi][yi] = true;
			sum++;
			yi += left;
			xi += up;
		}

		return sum;
	}

	/**
	 * Flag the next player in this object.
	 * 
	 * @return true if there is a next player, false if the game is over
	 */
	public boolean setNextPlayer() {
		byte nextPlayer = getPlayerWhichHasToMove();
		byte origPlayer = nextPlayer;
		Log.i("REV", "Choosing next player (from " + nextPlayer + ")");
		do {
			nextPlayer += 1;

			if (nextPlayer > getNumPlayers())
				nextPlayer = 1;
			if (origPlayer == nextPlayer && !playerHasMove(nextPlayer))
				return false;
		} while (!playerHasMove(nextPlayer));
		Log.i("REV", "Chose " + nextPlayer);
		setPlayerWhichHasToMove(nextPlayer);
		return true;
	}

	/**
	 * Determine the winner of this match
	 * 
	 * @return the winner, -1 when something went wrong
	 */
	public byte getWinner() {
		int points = Integer.MIN_VALUE;
		byte player = -1;
		for (byte i = 1; i <= getNumPlayers(); i++) {
			if (points < getPlayerPoints(i))
				player = i;
		}
		return player;
	}

	/**
	 * Create the points string
	 * 
	 * @return the points string, format x:y:z:...
	 */
	public String getPointsString() {
		String buf = "" + getPlayerPoints(1);
		for (byte i = 2; i <= getNumPlayers(); i++)
			buf += ":" + getPlayerPoints(i);
		return buf;
	}
}

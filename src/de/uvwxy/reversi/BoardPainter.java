package de.uvwxy.reversi;

import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import de.uvwxy.clickmap.ClickMapFactory;
import de.uvwxy.paintbox.PaintBox;

public class BoardPainter extends PaintBox {
	private boolean initialize = true;

	private Context context;

	int offsetx, offsety;

	Bitmap empty = null;
	Bitmap red = null;
	Bitmap blue = null;

	Paint pRed = null;
	Paint pBlue = null;
	Paint pBlack = null;

	int[][][] rectLookUpTable = new int[8][8][4];
	// depends on size of the screen
	double rectScale = 0;

	Reversi rev = null;
	SendGameReply sgr = null;

	public BoardPainter(Context context) {
		super(context);
		this.context = context;
		this.setOnTouchListener(touchClickListener);
	}

	public BoardPainter(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
		this.setOnTouchListener(touchClickListener);
	}

	public BoardPainter(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		this.context = context;
		this.setOnTouchListener(touchClickListener);
	}

	private Bitmap getScaledBitmap(Bitmap b, int newWidth, int newHeight) {
		int width = b.getWidth();
		int height = b.getHeight();

		float scaleWidth = ((float) newWidth) / width;
		float scaleHeight = ((float) newHeight) / height;

		float ratio = ((float) width) / newWidth;

		Matrix matrix = new Matrix();
		matrix.postScale(scaleWidth, scaleHeight);

		return Bitmap.createBitmap(b, 0, 0, width, height, matrix, true);
	}

	Rect rctBGdst = null, rctBGsrc = null;

	Rect[][] src = new Rect[8][8];
	Rect[][] dst = new Rect[8][8];

	@Override
	protected void onDraw(Canvas canvas) {
		if (canvas == null)
			return;

		if (initialize) {
			init();

		}

		Paint p = new Paint();
		p.setColor(Color.WHITE);
		p.setStyle(Style.FILL);
		p.setAntiAlias(true);

		canvas.drawRect(0, 0, getWidth(), getHeight(), p);

		canvas.drawBitmap(empty, rctBGsrc, rctBGdst, p);

		for (int x = 0; x < 8; x++) {
			for (int y = 0; y < 8; y++) {
				if (rev.get(x, y) == rev.iPlayer0)
					canvas.drawBitmap(red, src[x][y], dst[x][y], p);
				if (rev.get(x, y) == rev.iPlayer1)
					canvas.drawBitmap(blue, src[x][y], dst[x][y], p);
			}
		}

		// canvas.drawBitmap(blue, src[3][3], dst[3][3], p);
		// canvas.drawBitmap(blue, src[4][4], dst[4][4], p);
		// canvas.drawBitmap(red, src[3][4], dst[3][4], p);
		// canvas.drawBitmap(red, src[4][3], dst[4][3], p);

		p.setColor(Color.BLACK);
		if (myTurn) {
			canvas.drawRect(0, 0, 16, 16, pBlack);
			switch (sgr.getID()) {
			case Reversi.iPlayer0:
				canvas.drawRect(1, 1, 15, 15, pRed);
				break;
			case Reversi.iPlayer1:
				canvas.drawRect(1, 1, 15, 15, pBlue);
				break;
			default:
			}

			canvas.drawText("It's your turn!", 20, 10, p);
		}
		canvas.drawText("width: " + getWidth() + " height: " + getHeight(), getWidth() / 2 - 50, getHeight() / 2 - 10,
				p);
	}

	/**
	 * 
	 */
	private void init() {
		initialize = false;
		boolean widerThanHigh = getWidth() > getHeight();
		int w = widerThanHigh ? getHeight() : getWidth();
		if (widerThanHigh) {
			offsetx = (getWidth() - w) / 2;
		} else {
			offsety = (getHeight() - w) / 2;
		}

		Options o = new BitmapFactory.Options();
		o.inScaled = false;

		Bitmap bEmpty = BitmapFactory.decodeResource(context.getResources(), R.drawable.empty, o);
		Bitmap bBlue = BitmapFactory.decodeResource(context.getResources(), R.drawable.blue, o);
		Bitmap bRed = BitmapFactory.decodeResource(context.getResources(), R.drawable.red, o);
		Bitmap bMask = BitmapFactory.decodeResource(context.getResources(), R.drawable.mask, o);

		empty = getScaledBitmap(bEmpty, w, w);
		red = getScaledBitmap(bRed, w, w);
		blue = getScaledBitmap(bBlue, w, w);

		pRed = new Paint();
		pBlue = new Paint();
		pBlack = new Paint();

		pRed.setColor(Color.RED);
		pBlue.setColor(Color.BLUE);
		pBlack.setColor(Color.BLACK);

		pRed.setStyle(Style.FILL);
		pBlue.setStyle(Style.FILL);
		pBlack.setStyle(Style.FILL);

		rctBGsrc = new Rect(0, 0, w, w);
		rctBGdst = new Rect(offsetx, offsety, offsetx + w, offsety + w);

		Log.i("REV", "Empty dim: " + bEmpty.getWidth() + "/" + bEmpty.getHeight());
		Log.i("REV", "Red dim: " + bRed.getWidth() + "/" + bRed.getHeight());
		Log.i("REV", "Blue dim: " + bBlue.getWidth() + "/" + bBlue.getHeight());

		Log.i("REV", "x Empty dim: " + empty.getWidth() + "/" + empty.getHeight());
		Log.i("REV", "x Red dim: " + red.getWidth() + "/" + red.getHeight());
		Log.i("REV", "x Blue dim: " + blue.getWidth() + "/" + blue.getHeight());

		rectScale = ((double) w) / ((double) bEmpty.getHeight());
		Log.i("REV", "rectScale = " + ((double) w) + " / " + ((double) bEmpty.getHeight()));

		int[][][] clickMask = null;

		try {
			clickMask = ClickMapFactory.createClickMapOnAndroid(bMask);
		} catch (NotFoundException e) {
			e.printStackTrace();
		}

		if (clickMask != null) {
			// mask is in steps of 10. divide by 10.
			for (int x = 0; x < 8; x++) {
				for (int y = 0; y < 8; y++) {
					// TODO: something fancy
					rectLookUpTable[x][y] = clickMask[x * 10][y * 10];
					rectLookUpTable[x][y][0] *= rectScale;
					rectLookUpTable[x][y][1] *= rectScale;
					rectLookUpTable[x][y][2] *= rectScale;
					rectLookUpTable[x][y][3] *= rectScale;

					src[x][y] = new Rect((int) (rectLookUpTable[x][y][0]), (int) (rectLookUpTable[x][y][1]),
							(int) (rectLookUpTable[x][y][2]), (int) (rectLookUpTable[x][y][3]));
					dst[x][y] = new Rect((int) (rectLookUpTable[x][y][0]) + offsetx, (int) (rectLookUpTable[x][y][1])
							+ offsety, (int) (rectLookUpTable[x][y][2]) + offsetx, (int) (rectLookUpTable[x][y][3])
							+ offsety);
				}
			}
		} else {
			Log.i("REV", "clickMask was null");
		}

		Log.i("REV", "rectScale " + rectScale);
		rev = new Reversi();
	}

	private OnTouchListener touchClickListener = new OnTouchListener() {

		public boolean onTouch(View v, MotionEvent event) {
			float x = event.getX();
			float y = event.getY();

			int action = event.getAction();

			switch (action) {
			case MotionEvent.ACTION_DOWN: // Finger 0
				setFingersDown(0);
				consumeFingerDown(0, event.getX(0), event.getY(0));
				break;
			case MotionEvent.ACTION_POINTER_1_DOWN + 256: // Finger 1
				setFingersDown(1);
				consumeFingerDown(1, event.getX(1), event.getY(1));
				break;
			case MotionEvent.ACTION_POINTER_1_DOWN + 512: // Finger 2
				setFingersDown(2);
				consumeFingerDown(2, event.getX(2), event.getY(2));
				break;
			case MotionEvent.ACTION_POINTER_1_DOWN + 768: // Finger 3
				setFingersDown(3);
				consumeFingerDown(3, event.getX(3), event.getY(3));
				break;
			case MotionEvent.ACTION_POINTER_1_DOWN + 1024: // Finger 4
				setFingersDown(4);
				consumeFingerDown(4, event.getX(4), event.getY(4));
				break;
			case MotionEvent.ACTION_POINTER_1_DOWN + 1024 + 256: // Finger 5
				setFingersDown(5);
				consumeFingerDown(5, event.getX(5), event.getY(5));
				break;
			case MotionEvent.ACTION_POINTER_1_DOWN + 1024 + 512: // Finger 6
				setFingersDown(6);
				consumeFingerDown(6, event.getX(6), event.getY(6));
				break;
			case MotionEvent.ACTION_POINTER_1_DOWN + 1024 + 768: // Finger 7
				setFingersDown(7);
				consumeFingerDown(7, event.getX(7), event.getY(7));
				break;
			case MotionEvent.ACTION_POINTER_1_DOWN + 1024 + 1024: // Finger 8
				setFingersDown(8);
				consumeFingerDown(8, event.getX(8), event.getY(8));
				break;
			case MotionEvent.ACTION_POINTER_1_DOWN + 1024 + 1024 + 256: // 9
				setFingersDown(9);
				consumeFingerDown(9, event.getX(9), event.getY(9));
				break;
			case MotionEvent.ACTION_UP:
				Log.i("REMIND", "Fingers cleared");
				// all fingers gone, reset mask
				for (int i = 0; i < fingerDown.length; i++) {
					fingerDown[i] = false;
					fingerUsed[i] = false;
				}
				break;
			}

			return true;
		}
	};

	boolean[] fingerDown = new boolean[10];
	boolean[] fingerUsed = new boolean[10];

	public void setReversiGame(Reversi r, boolean myTurn) {
		rev = r;
		this.myTurn = myTurn;
	}

	public void setSendGameReply(SendGameReply sgr) {
		this.sgr = sgr;
	}

	private void setFingersDown(int f) {
		Log.i("REMIND", "Fingers down: " + f);
		for (int i = 0; i < f; i++) {
			fingerDown[i] = true;
		}
	}

	private void consumeFingerDown(int index, float x, float y) {
		if (!fingerUsed[index]) {
			fingerUsed[index] = true;

			// DO THE MAGIC
			x -= offsetx;
			y -= offsety;

			int stonex = -1;
			int stoney = -1;

			for (int ix = 0; ix < 8; ix++) {
				for (int iy = 0; iy < 8; iy++) {
					if (rectLookUpTable[ix][iy][0] <= x && rectLookUpTable[ix][iy][1] <= y
							&& rectLookUpTable[ix][iy][2] >= x && rectLookUpTable[ix][iy][3] >= y) {
						actionOnStone(ix, iy);
						return;
					}
				}
			}

		}
	}

	private boolean myTurn = false;

	private void actionOnStone(int x, int y) {
		Log.i("REV", "Game: " + x + "/" + y);

		if (myTurn && rev.playerHasMove(sgr.getID())) {
			if (rev.get(x, y) == rev.iEmpty) {
				if (rev.moveIsPossible(x, y, sgr.getID())) {
					Log.i("REV", "Game: " + x + "/" + y + " is possible!");
					rev.doMove(x, y, sgr.getID());
					Log.i("REV", "Client says board ID is " + rev.getPlayerWhichHasToMove());
					myTurn = false;
					sgr.sendGameReply(rev);
				} else {
					Log.i("REV", "Game: " + x + "/" + y + " move not possible");
				}
			}

		} else {
			Log.i("REV", "not your turn!");
		}
	}
}

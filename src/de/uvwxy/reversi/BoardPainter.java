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
import de.uvwxy.clickmap.ClickMapFactory;
import de.uvwxy.paintbox.PaintBox;

public class BoardPainter extends PaintBox {
	private boolean initialize = true;

	private Context context;

	int offsetx, offsety;

	Bitmap empty = null;
	Bitmap red = null;
	Bitmap blue = null;

	int[][][] rectLookUpTable = new int[8][8][4];
	// depends on size of the screen
	double rectScale = 0;

	public BoardPainter(Context context) {
		super(context);
		this.context = context;
	}

	public BoardPainter(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
	}

	public BoardPainter(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		this.context = context;
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

						Log.i("REV", "" + x + "/" + y + " " + rectLookUpTable[x][y][0] + " " + rectLookUpTable[x][y][1]
								+ " " + rectLookUpTable[x][y][2] + " " + rectLookUpTable[x][y][3]);
						src[x][y] = new Rect((int) (rectLookUpTable[x][y][0] * rectScale),
								(int) (rectLookUpTable[x][y][1] * rectScale),
								(int) (rectLookUpTable[x][y][2] * rectScale),
								(int) (rectLookUpTable[x][y][3] * rectScale));
						dst[x][y] = new Rect((int) (rectLookUpTable[x][y][0] * rectScale) + offsetx,
								(int) (rectLookUpTable[x][y][1] * rectScale) + offsety,
								(int) (rectLookUpTable[x][y][2] * rectScale) + offsetx,
								(int) (rectLookUpTable[x][y][3] * rectScale) + offsety);
					}
				}
			} else {
				Log.i("REV", "clickMask was null");
			}

			Log.i("REV", "rectScale " + rectScale);

		}

		Paint p = new Paint();
		p.setColor(Color.WHITE);
		p.setStyle(Style.FILL);
		p.setAntiAlias(true);

		canvas.drawRect(0, 0, getWidth(), getHeight(), p);

		canvas.drawBitmap(empty, rctBGsrc, rctBGdst, p);

		// for (int x = 0; x < 8; x++) {
		// for (int y = 0; y < 8; y++) {
		//
		// }
		// }

		canvas.drawBitmap(blue, src[3][3], dst[3][3], p);
		canvas.drawBitmap(blue, src[4][4], dst[4][4], p);
		canvas.drawBitmap(red, src[3][4], dst[3][4], p);
		canvas.drawBitmap(red, src[4][3], dst[4][3], p);

		p.setColor(Color.BLACK);
		canvas.drawText("width: " + getWidth() + " height: " + getHeight(), getWidth() / 2 - 50, getHeight() / 2 - 10,
				p);
	}
}

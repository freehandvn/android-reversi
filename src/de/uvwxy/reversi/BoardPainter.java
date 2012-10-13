package de.uvwxy.reversi;

import java.io.File;
import java.io.IOException;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources.NotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
				rectScale = ((double) w) / ((double) getHeight());
			} else {
				offsety = (getHeight() - w) / 2;
				rectScale = ((double) w) / ((double) getWidth());
			}

			empty = getScaledBitmap(BitmapFactory.decodeResource(context.getResources(), R.drawable.empty), w, w);
			red = getScaledBitmap(BitmapFactory.decodeResource(context.getResources(), R.drawable.red), w, w);
			blue = getScaledBitmap(BitmapFactory.decodeResource(context.getResources(), R.drawable.blue), w, w);

			int[][][] clickMask = null;

			try {
				clickMask = ClickMapFactory.createClickMapOnAndroid(context, R.drawable.mask);
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
					}
				}
			} else {
				Log.i("REV", "clickMask was null");
			}

			Log.i("REV", "scale " + rectScale);

		}

		Paint p = new Paint();
		p.setColor(Color.WHITE);
		p.setStyle(Style.FILL);
		p.setAntiAlias(true);

		canvas.drawRect(0, 0, getWidth(), getHeight(), p);

		canvas.drawBitmap(empty, offsetx, offsety, p);
		int x = 3, y = 4;
		Rect src = new Rect(rectLookUpTable[x][y][0], rectLookUpTable[x][y][1], rectLookUpTable[x][y][2],
				rectLookUpTable[x][y][3]);
		Rect dst = new Rect((int) (rectLookUpTable[x][y][0] * rectScale), (int) (rectLookUpTable[x][y][1] * rectScale),
				(int) (rectLookUpTable[x][y][2] * rectScale), (int) (rectLookUpTable[x][y][3] * rectScale));
		canvas.drawBitmap(blue, src, dst, p);

		p.setColor(Color.BLACK);
		canvas.drawText("width: " + getWidth() + " height: " + getHeight(), getWidth() / 2 - 50, getHeight() / 2 - 10,
				p);
	}
}

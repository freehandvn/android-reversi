package de.uvwxy.reversi;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import de.uvwxy.paintbox.PaintBox;

public class BoardPainter extends PaintBox {
	private boolean initialize = true;

	private Context context;

	int offsetx, offsety;
	
	Bitmap empty = null;
	Bitmap red = null;
	Bitmap blue = null;

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
			if (widerThanHigh)
				offsetx = (getWidth() - w) / 2;
			else
				offsety = (getHeight() - w) / 2;
			
			empty = getScaledBitmap(BitmapFactory.decodeResource(context.getResources(), R.drawable.blue), w, w);

		}

		Paint p = new Paint();
		p.setColor(Color.WHITE);
		p.setStyle(Style.FILL);
		p.setAntiAlias(true);

		canvas.drawRect(0, 0, getWidth(), getHeight(), p);

		canvas.drawBitmap(empty, offsetx, offsety, p);

		p.setColor(Color.BLACK);
		canvas.drawText("width: " + getWidth() + " height: " + getHeight(), getWidth() / 2 - 50, getHeight() / 2 - 10,
				p);
	}

}

package com.afollestad.overhear;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

public class SquareImageView extends ImageView {

	public SquareImageView(Context context) {
		super(context);
	}

	@Override 
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		/**
		 * This method insures the image view keeps it's aspect ratio when the view is stretched 
		 * (like in a RelativeLayout where fill_parent or wrap_content are used).
		 */
		Drawable d = getDrawable();
		if(d != null) {
			int width = MeasureSpec.getSize(widthMeasureSpec);
			int height = (int) Math.ceil((float) width * (float) d.getIntrinsicHeight() / (float) d.getIntrinsicWidth());
			setMeasuredDimension(width, height);
		} else {
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		}
	}
}

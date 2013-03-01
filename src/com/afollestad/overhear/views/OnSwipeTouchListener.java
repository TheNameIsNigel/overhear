package com.afollestad.overhear.views;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

/**
 * Used in the now playing viewer for gestures on the album art view.
 * 
 * @author Aidan Follestad
 */
public abstract class OnSwipeTouchListener implements View.OnTouchListener {

    public OnSwipeTouchListener(Context context) {
        gestureDetector = new GestureDetector(context, new GestureListener());
        final float scale = context.getResources().getDisplayMetrics().density;
        SWIPE_THRESHOLD = (int)(GESTURE_THRESHOLD_DP * scale + 0.5f);
    }

    private final GestureDetector gestureDetector;
    private final static float GESTURE_THRESHOLD_DP = 60.0f;
    private static int SWIPE_THRESHOLD;

    public boolean onTouch(final View v, final MotionEvent event) {
        onBasicTouch(v, event);
        return gestureDetector.onTouchEvent(event);
    }

    private final class GestureListener extends GestureDetector.SimpleOnGestureListener {

        private static final int SWIPE_VELOCITY_THRESHOLD = 100;
        private long LAST_DOWN = -1;

        @Override
        public boolean onDown(MotionEvent e) {
        	long now = System.currentTimeMillis();
        	if((now - LAST_DOWN) <= 300) {
        		OnSwipeTouchListener.this.onDoubleTap();
        	}
        	LAST_DOWN = now;
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            boolean result = false;
            try {
                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            onSwipeRight();
                        } else {
                            onSwipeLeft();
                        }
                    }
                } else {
                    if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffY > 0) {
                            onSwipeBottom();
                        } else {
                            onSwipeTop();
                        }
                    }
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
            return result;
        }
    }

    public abstract void onBasicTouch(View v, MotionEvent event);

    public abstract void onSwipeRight();

    public abstract void onSwipeLeft();

    public abstract void onSwipeTop();

    public abstract void onSwipeBottom();
    
    public abstract void onDoubleTap();
}
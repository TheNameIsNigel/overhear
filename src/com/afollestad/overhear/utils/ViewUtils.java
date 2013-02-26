package com.afollestad.overhear.utils;

import android.view.View;

public class ViewUtils {

	public static void relativeMargins(View view, int top, int bottom) {
		view.setPadding(0, top, 0, bottom);
	}
}
